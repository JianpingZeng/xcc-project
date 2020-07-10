package backend.bitcode.reader;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.

 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import backend.debug.DebugLoc;
import backend.io.ByteSequence;
import backend.ir.IndirectBrInst;
import backend.ir.SelectInst;
import backend.support.*;
import backend.type.*;
import backend.value.*;
import backend.value.Instruction.CallInst;
import backend.value.Instruction.GetElementPtrInst;
import backend.value.Instruction.LandingPadInst;
import tools.*;

import java.util.*;

import static backend.bitcode.BitcodeUtil.*;
import static backend.bitcode.reader.BitcodeReader.AttributeCodes.PARAMATTR_CODE_ENTRY;
import static backend.bitcode.reader.BitcodeReader.BinaryOpcodes.*;
import static backend.bitcode.reader.BitcodeReader.BlockIDs.*;
import static backend.bitcode.reader.BitcodeReader.BlockInfoCodes.*;
import static backend.bitcode.reader.BitcodeReader.CastOpcodes.*;
import static backend.bitcode.reader.BitcodeReader.ConstantsCodes.*;
import static backend.bitcode.reader.BitcodeReader.Encoding.*;
import static backend.bitcode.reader.BitcodeReader.FixedAbbrevIDs.*;
import static backend.bitcode.reader.BitcodeReader.ModuleCodes.*;
import static backend.bitcode.reader.BitcodeReader.StandardBlockIDs.*;
import static backend.bitcode.reader.BitcodeReader.StandardWidths.*;
import static backend.bitcode.reader.BitcodeReader.TypeCodes.*;
import static backend.bitcode.reader.BitcodeReader.TypeSymtabCodes.TST_CODE_ENTRY;
import static backend.bitcode.reader.BitcodeReader.ValueSymtabCodes.VST_CODE_BBENTRY;
import static backend.bitcode.reader.BitcodeReader.ValueSymtabCodes.VST_CODE_ENTRY;
import static backend.support.AutoUpgrade.*;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class BitcodeReader implements GVMaterializer {
  private Module theModule;
  private LLVMContext context;

  @Override
  public boolean isMaterializable(GlobalValue gv) {
    if (gv instanceof Function) {
      Function f = (Function) gv;
      return f.isDeclaration() && deferredFunctionInfo.containsKey(f);
    }
    return false;
  }

  @Override
  public boolean isDematerializable(GlobalValue gv) { return false; }

  @Override
  public boolean materialize(GlobalValue gv, OutRef<String> errInfo) {
    Function f = (Function) gv;
    if (f == null || !isMaterializable(f)) return false;

    Util.assertion(deferredFunctionInfo.containsKey(f), "Deferred function not found");
    jumpToBit(deferredFunctionInfo.get(f));

    if (parseFunctionBody(f)) {
      if (errInfo != null)
        errInfo.set(errorString);
      return true;
    }
    // upgrade any old intrinsic calls in the function.
    for (Pair<Function, Function> pair : upgradedIntrinsics) {
      if (pair.first != pair.second) {
        for (Use u : pair.first.usesList) {
          if (u.getUser() instanceof CallInst)
            upgradeIntrinsicCall((CallInst)u.getUser(), pair.second);
        }
        if (!pair.first.isUseEmpty())
          pair.first.replaceAllUsesWith(pair.second);
        pair.first.eraseFromParent();
      }
    }
    return false;
  }

  @Override
  public void dematerialize(GlobalValue gv) { }

  @Override
  public boolean materializeModule(Module m, OutRef<String> errInfo) {
    Util.assertion(theModule == m, "Can only materialize the module which this BitcodeReader is being attached to");
    for (Function f : theModule) {
      if (isMaterializable(f) && materialize(f, errInfo))
        return true;
    }

    for (Pair<Function, Function> pair : upgradedIntrinsics) {
      if (pair.first != pair.second) {
        for (Use u : pair.first.usesList) {
          if (u.getUser() instanceof CallInst)
            upgradeIntrinsicCall((CallInst)u.getUser(), pair.second);
        }
        if (!pair.first.isUseEmpty())
          pair.first.replaceAllUsesWith(pair.second);
        pair.first.eraseFromParent();
      }
    }
    upgradedIntrinsics.clear();
    upgradeExceptionHandling(m);
    checkDebugInfoIntrinsics(m);
    return false;
  }

  public interface StandardWidths {
    int BlockIDWidth = 8,  // We use VBR-8 for block IDs.
        CodeLenWidth = 4,  // Codelen are VBR-4.
        BlockSizeWidth = 32;  // BlockSize up to 2^32 32-bit words = 16GB per block.
  }

  // The standard abbrev namespace always has a way to exit a block, enter a
  // nested block, define abbrevs, and define an unabbreviated record.
  public interface FixedAbbrevIDs {
    int END_BLOCK = 0,  // Must be zero to guarantee termination for broken bitcode.

    ENTER_SUBBLOCK = 1,

    /// DEFINE_ABBREV - Defines an abbrev for the current block.  It consists
    /// of a vbr5 for # operand infos.  Each operand info is emitted with a
    /// single bit to indicate if it is a literal encoding.  If so, the value is
    /// emitted with a vbr8.  If not, the encoding is emitted as 3 bits followed
    /// by the info value as a vbr5 if needed.
    DEFINE_ABBREV = 2,

    // UNABBREV_RECORDs are emitted with a vbr6 for the record code, followed by
    // a vbr6 for the # operands, followed by vbr6's for each operand.
    UNABBREV_RECORD = 3,

    // This is not a code, this is a marker for the first abbrev assignment.
    FIRST_APPLICATION_ABBREV = 4;
  }

  /// StandardBlockIDs - All bitcode files can optionally include a BLOCKINFO
  /// block, which contains metadata about other blocks in the file.
  public interface StandardBlockIDs {
    /// BLOCKINFO_BLOCK is used to define metadata about blocks, for example,
    /// standard abbrevs that should be available to all blocks of a specified
    /// ID.
    int BLOCKINFO_BLOCK_ID = 0,

    // Block IDs 1-7 are reserved for future expansion.
    FIRST_APPLICATION_BLOCKID = 8;
  }

  /// BlockInfoCodes - The blockinfo block contains metadata about user-defined
  /// blocks.
  public interface BlockInfoCodes {
    // DEFINE_ABBREV has magic semantics here, applying to the current SETBID'd
    // block, instead of the BlockInfo block.
    int BLOCKINFO_CODE_SETBID = 1,       // SETBID: [blockid#]
    BLOCKINFO_CODE_BLOCKNAME = 2,    // BLOCKNAME: [name]
    BLOCKINFO_CODE_SETRECORDNAME = 3; // BLOCKINFO_CODE_SETRECORDNAME: [id, name]
  }

  // The only top-level block type defined is for a module.
  public interface BlockIDs {
    // Blocks
    int MODULE_BLOCK_ID = FIRST_APPLICATION_BLOCKID,

    // Module sub-block id's.
    PARAMATTR_BLOCK_ID = MODULE_BLOCK_ID + 1,
    TYPE_BLOCK_ID_OLD = PARAMATTR_BLOCK_ID + 1,
    CONSTANTS_BLOCK_ID = TYPE_BLOCK_ID_OLD + 1,
    FUNCTION_BLOCK_ID = CONSTANTS_BLOCK_ID + 1,
    TYPE_SYMTAB_BLOCK_ID_OLD = FUNCTION_BLOCK_ID + 1,
    VALUE_SYMTAB_BLOCK_ID = TYPE_SYMTAB_BLOCK_ID_OLD + 1,
    METADATA_BLOCK_ID = VALUE_SYMTAB_BLOCK_ID + 1,
    METADATA_ATTACHMENT_ID = METADATA_BLOCK_ID + 1,
    TYPE_BLOCK_ID_NEW = METADATA_ATTACHMENT_ID + 1;
  }

  /// MODULE blocks have a number of optional fields and subblocks.
  public interface ModuleCodes {
    int MODULE_CODE_VERSION = 1,    // VERSION:     [version#]
        MODULE_CODE_TRIPLE = 2,    // TRIPLE:      [strchr x N]
        MODULE_CODE_DATALAYOUT = 3,    // DATALAYOUT:  [strchr x N]
        MODULE_CODE_ASM = 4,    // ASM:         [strchr x N]
        MODULE_CODE_SECTIONNAME = 5,    // SECTIONNAME: [strchr x N]
        MODULE_CODE_DEPLIB = 6,    // DEPLIB:      [strchr x N]

    // GLOBALVAR: [pointer type, isconst, initid,
    //             linkage, alignment, section, visibility, threadlocal]
    MODULE_CODE_GLOBALVAR = 7,

    // FUNCTION:  [type, callingconv, isproto, linkage, paramattrs, alignment,
    //             section, visibility]
    MODULE_CODE_FUNCTION = 8,

    // ALIAS: [alias type, aliasee val#, linkage]
    MODULE_CODE_ALIAS = 9,

    /// MODULE_CODE_PURGEVALS: [numvals]
    MODULE_CODE_PURGEVALS = 10,

    MODULE_CODE_GCNAME = 11;   // GCNAME: [strchr x N]
  }

  /// PARAMATTR blocks have code for defining a parameter attribute set.
  public interface AttributeCodes {
    int PARAMATTR_CODE_ENTRY = 1;   // ENTRY: [paramidx0, attr0, paramidx1, attr1...]
  }

  /// TYPE blocks have codes for each type primitive they use.
  public interface TypeCodes {
    int TYPE_CODE_NUMENTRY =  1,    // NUMENTRY: [numentries]

    // Type Codes
    TYPE_CODE_VOID     =  2,    // VOID
    TYPE_CODE_FLOAT    =  3,    // FLOAT
    TYPE_CODE_DOUBLE   =  4,    // DOUBLE
    TYPE_CODE_LABEL    =  5,    // LABEL
    TYPE_CODE_OPAQUE   =  6,    // OPAQUE
    TYPE_CODE_INTEGER  =  7,    // INTEGER: [width]
    TYPE_CODE_POINTER  =  8,    // POINTER: [pointee type]
    TYPE_CODE_FUNCTION =  9,    // FUNCTION: [vararg, retty, paramty x N]

    // FIXME: This is the encoding used for structs in LLVM 2.9 and earlier.
    // REMOVE this in LLVM 3.1
    TYPE_CODE_STRUCT_OLD = 10,  // STRUCT: [ispacked, eltty x N]
    TYPE_CODE_ARRAY    = 11,    // ARRAY: [numelts, eltty]
    TYPE_CODE_VECTOR   = 12,    // VECTOR: [numelts, eltty]

    // These are not with the other floating point types because they're
    // a late addition, and putting them in the right place breaks
    // binary compatibility.
    TYPE_CODE_X86_FP80 = 13,    // X86 LONG DOUBLE
    TYPE_CODE_FP128    = 14,    // LONG DOUBLE (112 bit mantissa)
    TYPE_CODE_PPC_FP128= 15,    // PPC LONG DOUBLE (2 doubles)

    TYPE_CODE_METADATA = 16,    // METADATA

    TYPE_CODE_X86_MMX = 17,     // X86 MMX

    TYPE_CODE_STRUCT_ANON = 18, // STRUCT_ANON: [ispacked, eltty x N]
    TYPE_CODE_STRUCT_NAME = 19, // STRUCT_NAME: [strchr x N]
    TYPE_CODE_STRUCT_NAMED = 20;// STRUCT_NAMED: [ispacked, eltty x N]
  }

  // The type symbol table only has one code (TST_ENTRY_CODE).
  interface TypeSymtabCodes {
    int TST_CODE_ENTRY = 1;     // TST_ENTRY: [typeid, namechar x N]
  }

  // The value symbol table only has one code (VST_ENTRY_CODE).
  public interface ValueSymtabCodes {
    int VST_CODE_ENTRY = 1,  // VST_ENTRY: [valid, namechar x N]
        VST_CODE_BBENTRY = 2;   // VST_BBENTRY: [bbid, namechar x N]
  }

  public interface MetadataCodes {
    int METADATA_STRING = 1,   // MDSTRING:      [values]
        // 2 and 3 are not used
        METADATA_NAME = 4,   // STRING:        [values]
        // 5 is not used.
        METADATA_KIND = 6,   // [n x [id, name]]
        // 7 is unused
        METADATA_NODE = 8,   // NODE2:         [n x (type num, value num)]
        METADATA_FN_NODE = 9,   // FN_NODE2:      [n x (type num, value num)]
        METADATA_NAMED_NODE = 10,  // NAMED_NODE2:   [n x mdnodes]
        METADATA_ATTACHMENT = 11;   // [m x [value, [n x [id, mdnode]]]
  }

  // The constants block (CONSTANTS_BLOCK_ID) describes emission for each
  // constant and maintains an implicit current type value.
  public interface ConstantsCodes {
    int CST_CODE_SETTYPE = 1,  // SETTYPE:       [typeid]
        CST_CODE_NULL = 2,  // NULL
        CST_CODE_UNDEF = 3,  // UNDEF
        CST_CODE_INTEGER = 4,  // INTEGER:       [intval]
        CST_CODE_WIDE_INTEGER = 5,  // WIDE_INTEGER:  [n x intval]
        CST_CODE_FLOAT = 6,  // FLOAT:         [fpval]
        CST_CODE_AGGREGATE = 7,  // AGGREGATE:     [n x value number]
        CST_CODE_STRING = 8,  // STRING:        [values]
        CST_CODE_CSTRING = 9,  // CSTRING:       [values]
        CST_CODE_CE_BINOP = 10,  // CE_BINOP:      [opcode, opval, opval]
        CST_CODE_CE_CAST = 11,  // CE_CAST:       [opcode, opty, opval]
        CST_CODE_CE_GEP = 12,  // CE_GEP:        [n x operands]
        CST_CODE_CE_SELECT = 13,  // CE_SELECT:     [opval, opval, opval]
        CST_CODE_CE_EXTRACTELT = 14,  // CE_EXTRACTELT: [opty, opval, opval]
        CST_CODE_CE_INSERTELT = 15,  // CE_INSERTELT:  [opval, opval, opval]
        CST_CODE_CE_SHUFFLEVEC = 16,  // CE_SHUFFLEVEC: [opval, opval, opval]
        CST_CODE_CE_CMP = 17,  // CE_CMP:        [opty, opval, opval, pred]
        CST_CODE_INLINEASM = 18,  // INLINEASM:     [sideeffect,asmstr,conststr]
        CST_CODE_CE_SHUFVEC_EX = 19,  // SHUFVEC_EX:    [opty, opval, opval, opval]
        CST_CODE_CE_INBOUNDS_GEP = 20,// INBOUNDS_GEP:  [n x operands]
        CST_CODE_BLOCKADDRESS = 21;   // CST_CODE_BLOCKADDRESS [fnty, fnval, bb#]
  }

  /// CastOpcodes - These are values used in the bitcode files to encode which
  /// cast a CST_CODE_CE_CAST or a XXX refers to.  The values of these enums
  /// have no fixed relation to the LLVM IR enum values.  Changing these will
  /// break compatibility with old files.
  public interface CastOpcodes {
    int CAST_TRUNC = 0,
        CAST_ZEXT = 1,
        CAST_SEXT = 2,
        CAST_FPTOUI = 3,
        CAST_FPTOSI = 4,
        CAST_UITOFP = 5,
        CAST_SITOFP = 6,
        CAST_FPTRUNC = 7,
        CAST_FPEXT = 8,
        CAST_PTRTOINT = 9,
        CAST_INTTOPTR = 10,
        CAST_BITCAST = 11;
  }

  /// BinaryOpcodes - These are values used in the bitcode files to encode which
  /// binop a CST_CODE_CE_BINOP or a XXX refers to.  The values of these enums
  /// have no fixed relation to the LLVM IR enum values.  Changing these will
  /// break compatibility with old files.
  public interface BinaryOpcodes {
    int BINOP_ADD = 0,
        BINOP_SUB = 1,
        BINOP_MUL = 2,
        BINOP_UDIV = 3,
        BINOP_SDIV = 4,    // overloaded for FP
        BINOP_UREM = 5,
        BINOP_SREM = 6,    // overloaded for FP
        BINOP_SHL = 7,
        BINOP_LSHR = 8,
        BINOP_ASHR = 9,
        BINOP_AND = 10,
        BINOP_OR = 11,
        BINOP_XOR = 12;
  }

  /// OverflowingBinaryOperatorOptionalFlags - Flags for serializing
  /// OverflowingBinaryOperator's SubclassOptionalData contents.
  public interface OverflowingBinaryOperatorOptionalFlags {
    int OBO_NO_UNSIGNED_WRAP = 0,
        OBO_NO_SIGNED_WRAP = 1;
  }

  /**
   * Flags for serializing PossiblyExactOperator's SubclassOptionalData contents.
   */
  public interface PossiblyExactOperatorOptionalFlags {
    int PEO_EXACT = 0;
  }

  /// SDivOperatorOptionalFlags - Flags for serializing SDivOperator's
  /// SubclassOptionalData contents.
  interface SDivOperatorOptionalFlags {
    int SDIV_EXACT = 0;
  }

  // The function body block (FUNCTION_BLOCK_ID) describes function bodies.  It
  // can contain a constant block (CONSTANTS_BLOCK_ID).
  public interface FunctionCodes {
    int FUNC_CODE_DECLAREBLOCKS = 1, // DECLAREBLOCKS: [n]

    FUNC_CODE_INST_BINOP = 2, // BINOP:      [opcode, ty, opval, opval]
    FUNC_CODE_INST_CAST = 3, // CAST:       [opcode, ty, opty, opval]
    FUNC_CODE_INST_GEP = 4, // GEP:        [n x operands]
    FUNC_CODE_INST_SELECT = 5, // SELECT:     [ty, opval, opval, opval]
    FUNC_CODE_INST_EXTRACTELT = 6, // EXTRACTELT: [opty, opval, opval]
    FUNC_CODE_INST_INSERTELT = 7, // INSERTELT:  [ty, opval, opval, opval]
    FUNC_CODE_INST_SHUFFLEVEC = 8, // SHUFFLEVEC: [ty, opval, opval, opval]
    FUNC_CODE_INST_CMP = 9, // CMP:        [opty, opval, opval, pred]

    FUNC_CODE_INST_RET = 10, // RET:        [opty,opval<both optional>]
    FUNC_CODE_INST_BR = 11, // BR:         [bb#, bb#, cond] or [bb#]
    FUNC_CODE_INST_SWITCH = 12, // SWITCH:     [opty, op0, op1, ...]
    FUNC_CODE_INST_INVOKE = 13, // INVOKE:     [attr, fnty, op0,op1, ...]
    FUNC_CODE_INST_UNWIND = 14, // UNWIND
    FUNC_CODE_INST_UNREACHABLE = 15, // UNREACHABLE

    FUNC_CODE_INST_PHI = 16, // PHI:        [ty, val0,bb0, ...]
    // 17 and 18 are not used.
    FUNC_CODE_INST_ALLOCA = 19, // ALLOCA:     [instty, op, align]
    FUNC_CODE_INST_LOAD = 20, // LOAD:       [opty, op, align, vol]
    // 21 and 22 are not used
    FUNC_CODE_INST_VAARG = 23, // VAARG:      [valistty, valist, instty]
    // This store code encodes the pointer type, rather than the value type
    // this is so information only available in the pointer type (e.g. address
    // spaces) is retained.
    FUNC_CODE_INST_STORE = 24, // STORE:      [ptrty,ptr,val, align, vol]
    // 25 are not used
    FUNC_CODE_INST_EXTRACTVAL = 26, // EXTRACTVAL: [n x operands]
    FUNC_CODE_INST_INSERTVAL = 27, // INSERTVAL:  [n x operands]
    // fcmp/icmp returning Int1TY or vector of Int1Ty. Same as CMP, exists to
    // support legacy vicmp/vfcmp instructions.
    FUNC_CODE_INST_CMP2 = 28, // CMP2:       [opty, opval, opval, pred]
    // new select on i1 or [N x i1]
    FUNC_CODE_INST_VSELECT = 29, // VSELECT:    [ty,opval,opval,predty,pred]
    FUNC_CODE_INST_INBOUNDS_GEP = 30, // INBOUNDS_GEP: [n x operands]
    FUNC_CODE_INST_INDIRECTBR = 31, // INDIRECTBR: [opty, op0, op1, ...]
    // 32 is not used.
    FUNC_CODE_DEBUG_LOC_AGAIN = 33, // DEBUG_LOC_AGAIN

    FUNC_CODE_INST_CALL = 34, // CALL2:      [attr, fnty, fnid, args...]

    FUNC_CODE_DEBUG_LOC = 35, // DEBUG_LOC with potentially invalid metadata
    FUNC_CODE_INST_FENCE = 36, // FENCE: [ordering, synchscope]
    FUNC_CODE_INST_CMPXCHG     = 37, // CMPXCHG: [ptrty,ptr,cmp,new, align, vol,
                                     //           ordering, synchscope]
    FUNC_CODE_INST_ATOMICRMW   = 38, // ATOMICRMW: [ptrty,ptr,val, operation,
                                     //             align, vol,
                                     //             ordering, synchscope]
    FUNC_CODE_INST_RESUME      = 39, // RESUME:     [opval]
    FUNC_CODE_INST_LANDINGPAD  = 40, // LANDINGPAD: [ty,val,val,num,id0,val0...]
    FUNC_CODE_INST_LOADATOMIC  = 41, // LOAD: [opty, op, align, vol,
                                     //        ordering, synchscope]
    FUNC_CODE_INST_STOREATOMIC = 42; // STORE: [ptrty,ptr,val, align, vol
                                     //         ordering, synchscope]
  }

  public interface Encoding {
    int Fixed = 1,  // A fixed width field, val specifies number of bits.
        VBR = 2,  // A VBR field where val specifies the width of each chunk.
        Array = 3,  // A sequence of fields, next field species elt encoding.
        Char6 = 4,  // A 6-bit fixed field which maps to [a-zA-Z0-9._].
        Blob = 5;   // 32-bit aligned array of 8-bit characters.
  }

  /// BitCodeAbbrevOp - This describes one or more operands in an abbreviation.
  /// This is actually a union of two different things:
  ///   1. It could be a literal integer value ("the operand is always 17").
  ///   2. It could be an encoding specification ("this operand encoded like so").
  public static class BitCodeAbbrevOp {
    long val;           // A literal value or data for an encoding.
    boolean isLiteral;     // Indicate whether this is a literal value or not.
    int enc;     // The encoding to use.

    public BitCodeAbbrevOp(long v) {
      val = v;
      isLiteral = true;
    }

    public BitCodeAbbrevOp(int e, long data) {
      val = data;
      isLiteral = false;
      enc = e;
    }

    public boolean isLiteral() {
      return isLiteral;
    }

    boolean isEncoding() {
      return !isLiteral;
    }

    // Accessors for literals.
    public long getLiteralValue() {
      assert (isLiteral());
      return val;
    }

    // Accessors for encoding info.
    public int getEncoding() {
      assert (isEncoding());
      return enc;
    }

    public long getEncodingData() {
      assert (isEncoding() && hasEncodingData());
      return val;
    }

    public boolean hasEncodingData() {
      return hasEncodingData(getEncoding());
    }

    boolean hasEncodingData(int e) {
      switch (e) {
        default:
          Util.assertion("Unknown encoding");
        case Fixed:
        case VBR:
          return true;
        case Array:
        case Char6:
        case Blob:
          return false;
      }
    }

    /// isChar6 - Return true if this character is legal in the Char6 encoding.
    public static boolean isChar6(char c) {
      if (c >= 'a' && c <= 'z') return true;
      if (c >= 'A' && c <= 'Z') return true;
      if (c >= '0' && c <= '9') return true;
      if (c == '.' || c == '_') return true;
      return false;
    }

    public static int encodeChar6(byte c) {
      if (c >= 'a' && c <= 'z') return c - 'a';
      if (c >= 'A' && c <= 'Z') return c - 'A' + 26;
      if (c >= '0' && c <= '9') return c - '0' + 26 + 26;
      if (c == '.') return 62;
      if (c == '_') return 63;
      Util.assertion("Not a value Char6 character!");
      return 0;
    }

    public static int decodeChar6(int v) {
      Util.assertion((v & ~63) == 0, "Not a Char6 encoded character!");
      if (v < 26) return (char) (v + 'a');
      if (v < 26 + 26) return (char) (v - 26 + 'A');
      if (v < 26 + 26 + 10) return (char) (v - 26 - 26 + '0');
      if (v == 62) return '.';
      if (v == 63) return '_';
      Util.assertion("Not a value Char6 character!");
      return ' ';
    }
  }

  /// BitCodeAbbrev - This class represents an abbreviation record.  An
  /// abbreviation allows a complex record that has redundancy to be stored in a
  /// specialized format instead of the fully-general, fully-vbr, format.
  public static class BitCodeAbbrev {
    ArrayList<BitCodeAbbrevOp> operandList;
    public BitCodeAbbrev() { operandList = new ArrayList<>(); }
    public int getNumOperandInfos() { return operandList.size(); }
    public BitCodeAbbrevOp getOperandInfo(int n) { return operandList.get(n); }
    public void add(BitCodeAbbrevOp opInfo) { operandList.add(opInfo); }
  }

  /// BlockInfo - This contains information emitted to BLOCKINFO_BLOCK blocks.
  /// These describe abbreviations that all blocks of the specified ID inherit.
  static class BlockInfo {
    int blockID;
    ArrayList<BitCodeAbbrev> abbrevs;
    String name;

    ArrayList<Pair<Integer, String>> recordNames;

    BlockInfo() {
      blockID = 0;
      abbrevs = new ArrayList<>();
      name = "";
      recordNames = new ArrayList<>();
    }
  }

  private static class Block {
    int prevCodeSize;
    LinkedList<BitCodeAbbrev> prevAbbrevs;
    Block(int pcs) {
      this.prevCodeSize = pcs;
      prevAbbrevs = new LinkedList<>();
    }
  }

  private String errorString;
  private ByteSequence buffer;
  /**
   * The current pointer to the buffer from which the current bit is read.
   * Note that, this variable refers to the bit position instead of byte position.
   */
  private int curOffset;
  private int bitsInCurWord;
  private int curWord;
  /**
   * Specify how many bytes would be read when calling {@linkplain #readCode()}.
   * This value will changes when entering difference block.
   */
  private int curCodeSize;

  private ArrayList<Pair<GlobalVariable, Integer>> globalInits;
  private ArrayList<Pair<GlobalAlias, Integer>> aliasInits;

  /**
   * The set of attributes by index. Index zero in the file is for null,
   * And is thus not represented here. As such all indices are off by one.
   */
  private ArrayList<AttrList> mattributes;

  /**
   * While parsing a function body, this is a list of the basic
   * blocks for the function.
   */
  private ArrayList<BasicBlock> functionBBs;

  /**
   * When reading the module header, this list is populated with functions
   * that have bodies in the file.
   */
  private LinkedList<Function> functionsWithBodies;

  /**
   * When intrinsic functions are encountered which requires upgrading they
   * are stored here with their replacement function.
   */
  private ArrayList<Pair<Function, Function>> upgradedIntrinsics;

  /**
   * Map the bitcode's custom MDKind ID to the Module's MDKind ID.
   */
  private TreeMap<Integer, Integer> mdKindMap;

  private BitcodeReaderMDValueList mdValueList;

  /**
   * After the module headers have been read, the field {@linkplain #functionsWithBodies}
   * list is reversed. This keeps track of whether we've done this yet.
   */
  private boolean hasReversedFunctionsWithBodies;

  /**
   * When function bodies are initially scanned. This map contains info
   * about where to find deferred function body in the stream.
   */
  private HashMap<Function, Long> deferredFunctionInfo;

  /**
   * These are blockaddr references to basic blocks. These are resolved
   * lazily when functions are loaded.
   */
  private HashMap<Function, ArrayList<Pair<Long, GlobalVariable>>> blockAddrFwdRefs;

  private LinkedList<Block> blockScope;
  private LinkedList<BitCodeAbbrev> curAbbrevs;

  private LinkedList<BlockInfo> blockInfoRecords;

  /// IgnoreBlockInfoNames - This is set to true if we don't care about the
  /// block/record name information in the BlockInfo block. Only llvm-bcanalyzer
  /// uses this.
  private boolean ignoreBlockInfoNames;

  private BitcodeReaderValueList valueList;
  private ArrayList<Type> typeList;
  private ArrayList<Instruction> instructionList;

  private BitcodeReader(MemoryBuffer buffer, LLVMContext ctx) {
    context = ctx;
    errorString = null;
    theModule = null;
    this.buffer = ByteSequence.create(buffer);
    curOffset = 0;
    curCodeSize = 2;
    globalInits = new ArrayList<>();
    aliasInits = new ArrayList<>();
    mattributes = new ArrayList<>();
    functionBBs = new ArrayList<>();
    functionsWithBodies = new LinkedList<>();
    upgradedIntrinsics = new ArrayList<>();
    mdKindMap = new TreeMap<>();
    mdValueList = new BitcodeReaderMDValueList(context);
    hasReversedFunctionsWithBodies = false;
    deferredFunctionInfo = new HashMap<>();
    blockAddrFwdRefs = new HashMap<>();
    blockScope = new LinkedList<>();
    curAbbrevs = new LinkedList<>();
    blockInfoRecords = new LinkedList<>();
    ignoreBlockInfoNames = false;
    valueList = new BitcodeReaderValueList();
    typeList = new ArrayList<>();
    instructionList = new ArrayList<>();
  }

  private String getErrorString() {
    return errorString;
  }

  private boolean error(String msg) {
    errorString = msg;
    return true;
  }

  private long read(int numBits) {
    Util.assertion(numBits <= 32, "Can't return more than 32 bits");
    if (bitsInCurWord >= numBits) {
      // remained bits is enough.
      int res = curWord & ((1 << numBits) - 1);
      curWord >>>= numBits;
      bitsInCurWord -= numBits;
      return res;
    }

    // if we ran out data stream, stop at end of the stream.
    if (atEndOfStream()) {
      curWord = 0;
      bitsInCurWord = 0;
      return 0;
    }

    // remained bits is not enough, but we keep the left bits.
    int res = curWord;
    // construct a 32 bit integer with 4 bytes layout in little endian.
    int b1 = buffer.byteAt(curOffset) & 0xff;
    int b2 = buffer.byteAt(curOffset+1) & 0xff;
    int b3 = buffer.byteAt(curOffset+2) & 0xff;
    int b4 = buffer.byteAt(curOffset+3) & 0xff;

    curWord = b1 | (b2 << 8) | (b3 << 16) | (b4 << 24);
    curOffset += 4;

    // extract numBits - bitsInCurWord bits from curWord.
    int bitsLeft = numBits - bitsInCurWord;
    res |= (curWord & (~0 >>> (32 - bitsLeft))) << bitsInCurWord;
    if (bitsLeft != 32)
      curWord >>>= bitsLeft;
    else
      curWord = 0;
    bitsInCurWord = 32 - bitsLeft;
    return res;
  }

  private boolean atEndOfStream() {
    return curOffset >= buffer.length();
  }

  private long readCode() {
    return read(curCodeSize);
  }

  private int readSubBlockID() {
    return (int) readVBR(BlockIDWidth);
  }

  private boolean parseBitcodeInfo(Module m) {
    context = m.getContext();
    theModule = null;
    if ((buffer.length() & 3) != 0) {
      if (!isRawBitcode(buffer) && !isBitcodeWrapper(buffer))
        return error("Invalid bitcode signature");
      else
        return error("Bitcode stream should be a multiple of 4 bytes in length");
    }

    if (isBitcodeWrapper(buffer)) {
      buffer = skipBitcodeWrapperHeader(buffer);
      if (buffer == null)
        return error("Invalid bitcode wrapper header");
    }

    // sniff for the signature.
    if (read(8) != 'B' || read(8) != 'C' ||
        read(4) != 0x0 || read(4) != 0xC ||
        read(4) != 0xE || read(4) != 0xD) {
      return error("Invalid bitcode signature");
    }

    // we expect a number of well-defined blocks, though we don't necessarily
    // need to understand them all.
    while (!atEndOfStream()) {
      long code = readCode();
      if (code != ENTER_SUBBLOCK)
        return error("Invalid record at top-level");

      int blockID = readSubBlockID();
      // We only know the MODULE subblock ID.
      switch (blockID) {
        case BLOCKINFO_BLOCK_ID:
          if (readBlockInfoBlock())
            return error("Malformed BlockInfoBlock");
          break;
        case MODULE_BLOCK_ID:
          // Reject multiple MODULE_BLOCK's in a single bitstream.
          if (theModule != null)
            return error("Multiple MODULE_BLOCKS in same stream");

          theModule = m;
          if (parseModule())
            return true;
          break;
        default:
          if (skipBlock())
            return error("Malformed block record");
          break;
      }
    }
    return false;
  }

  private boolean readBlockInfoBlock() {
    // If this is the second stream to get to the block info block, skip it.
    if (hasBlockInfoRecords())
      return skipBlock();

    if (enterSubBlock(BLOCKINFO_BLOCK_ID)) 
      return true;

    ArrayList<Long> record = new ArrayList<>();
    BlockInfo curBlockInfo = null;

    // read all the records for this module.
    while (true) {
      int code = (int) readCode();
      if (code == END_BLOCK)
        return readBlockEnd();
      if (code == ENTER_SUBBLOCK) {
        readSubBlockID();
        if (skipBlock())
          return true;
        continue;
      }

      // read abbrev records, associate them with curBID.
      if (code == DEFINE_ABBREV) {
        if (curBlockInfo == null) return true;

        readAbbrevRecord();
        BitCodeAbbrev abbv = curAbbrevs.getLast();
        curAbbrevs.removeLast();
        curBlockInfo.abbrevs.add(abbv);
        continue;
      }

      // read a record.
      record.clear();
      switch (readRecord(code, record)) {
        default:break;
        case BLOCKINFO_CODE_SETBID:
          if (record.isEmpty()) return true;
          curBlockInfo = getOrCreateBlcokInfo(record.get(0).intValue());
          break;
        case BLOCKINFO_CODE_BLOCKNAME: {
          if (curBlockInfo == null) return true;

          if (ignoreBlockInfoNames) break;
          StringBuilder name = new StringBuilder();
          for (Long i : record)
            name.append((char) i.intValue());
          curBlockInfo.name = name.toString();
          break;
        }
        case BLOCKINFO_CODE_SETRECORDNAME: {
          if (curBlockInfo == null) return true;
          if (ignoreBlockInfoNames) break;
          StringBuilder name = new StringBuilder();
          for (int i = 1, e = record.size(); i < e; i++)
            name.append((char)record.get(i).intValue());
          curBlockInfo.recordNames.add(Pair.get(record.get(0).intValue(), name.toString()));
          break;
        }
      }
    }
  }

  private BlockInfo getOrCreateBlcokInfo(int blockID) {
    BlockInfo bi = getBlockInfo(blockID);
    if (bi != null) return bi;
    // otherwise, create a new block info and insert it into
    // the hash map.
    bi = new BlockInfo();
    blockInfoRecords.add(bi);
    bi.blockID = blockID;
    return bi;
  }

  private BitCodeAbbrev getAbbrev(int abbrevID) {
    int abbrevNo = abbrevID - FIRST_APPLICATION_ABBREV;
    Util.assertion(abbrevNo < curAbbrevs.size(), String.format("Invalid abbrev #'%d'!", abbrevNo));
    return curAbbrevs.get(abbrevNo);
  }

  private int readRecord(long abbrevID, ArrayList<Long> vals) {
    if (abbrevID == UNABBREV_RECORD) {
      int code = (int) readVBR(6);
      int numElts = (int) readVBR(6);
      for (int i = 0; i < numElts; ++i)
        vals.add(readVBR(6));
      return code;
    }

    BitCodeAbbrev abbv = getAbbrev((int) abbrevID);
    for (int i = 0, e = abbv.getNumOperandInfos(); i < e; ++i) {
      BitCodeAbbrevOp op = abbv.getOperandInfo(i);
      if (op.isLiteral())
        readAbbreviatedLiteral(op, vals);
      else if (op.getEncoding() == Array) {
        // for array, we have the number of elements and array element type.
        int numElts = (int) readVBR(6);
        Util.assertion(i+2 == e, "array op not second last?");
        BitCodeAbbrevOp eltEnc = abbv.getOperandInfo(++i);
        // read all elements
        for (; numElts != 0; --numElts)
          readAbbreviatedField(eltEnc, vals);
      } else if (op.getEncoding() == Blob) {
        // Blob case.  Read the number of bytes as a vbr6.
        int numElts = (int) readVBR(6);
        // 32 bits alignment
        skipToWord();

        int newEnd = curOffset + ((numElts+3)&~3);
        if (newEnd >= buffer.length()) {
          // input bitstream is not long enough, fill the vals with numElts zero.
          vals.addAll(Collections.nCopies(numElts, 0L));
          curOffset = newEnd;
          break;
        }

        // Otherwise, read the number of bytes.
        for (; numElts != 0; --numElts)
          vals.add(read(32));
        // skip trailing padding.
        curOffset = newEnd;
      } else {
        readAbbreviatedField(op, vals);
      }
    }
    long code = vals.get(0);
    vals.remove(0);
    return (int) code;
  }

  private void readAbbreviatedField(BitCodeAbbrevOp op, ArrayList<Long> vals) {
    Util.assertion(!op.isLiteral(), "Use readAbbreviatedField for literal value?");
    switch (op.getEncoding()) {
      case Fixed:
        vals.add(read((int) op.getEncodingData()));
        break;
      case VBR:
        vals.add(readVBR((int) op.getEncodingData()));
        break;
      case Char6:
        vals.add((long)BitCodeAbbrevOp.decodeChar6((int) read(6)));
        break;
      default:
        Util.shouldNotReachHere("Unknown encoding");
    }
  }

  private void readAbbreviatedLiteral(BitCodeAbbrevOp op, ArrayList<Long> vals) {
    Util.assertion(op.isLiteral(), "Not a literal");
    vals.add(op.getLiteralValue());
  }

  private boolean hasEncodingData(int e) {
    switch (e) {
      default:
        Util.assertion(String.format("Unknown encoding '%d'", e));
      case Fixed:
      case VBR:
        return true;
      case Array:
      case Char6:
      case Blob:
        return false;
    }
  }

  private void readAbbrevRecord() {
    BitCodeAbbrev abbv = new BitCodeAbbrev();
    long numOpInfo = readVBR(5);
    for (int i = 0; i < numOpInfo; i++) {
      boolean isLiteral = read(1) != 0;
      if (isLiteral) {
        abbv.add(new BitCodeAbbrevOp(readVBR(8)));
        continue;
      }

      int encoding = (int) read(3);
      if (hasEncodingData(encoding))
        abbv.add(new BitCodeAbbrevOp(encoding, readVBR(5)));
      else
        abbv.add(new BitCodeAbbrevOp(encoding, 0));
    }
    curAbbrevs.add(abbv);
  }

  private void popBlockScope() {
    curCodeSize = blockScope.getLast().prevCodeSize;
    // delete abbrevs from popped scope.
    for (int i = 0, e = curAbbrevs.size(); i < e; i++) {
      curAbbrevs.set(i, null);
    }

    LinkedList<BitCodeAbbrev> temp = curAbbrevs;
    curAbbrevs = blockScope.getLast().prevAbbrevs;
    blockScope.getLast().prevAbbrevs = temp;
    blockScope.removeLast();
  }

  private boolean readBlockEnd() {
    if (blockScope.isEmpty()) return true;

    // Block tail:
    //   [END_BLOCK, <align4bytes>]
    skipToWord();
    popBlockScope();
    return false;
  }

  private boolean enterSubBlock(int blockID) {
    return enterSubBlock(blockID, null);
  }

  private boolean enterSubBlock(int blockID, OutRef<Integer> numWordsP) {
    blockScope.addLast(new Block(curCodeSize));
    LinkedList<BitCodeAbbrev> temp = curAbbrevs;
    curAbbrevs = blockScope.getLast().prevAbbrevs;
    blockScope.getLast().prevAbbrevs = temp;

    BlockInfo info = getBlockInfo(blockID);
    if (info != null) {
      for (int i = 0, e = info.abbrevs.size(); i < e; i++) {
        curAbbrevs.add(info.abbrevs.get(i));
      }
    }

    // get the code size of this block.
    curCodeSize = (int) readVBR(CodeLenWidth);
    skipToWord();
    int numWords = (int) read(BlockSizeWidth);
    if (numWordsP != null)
      numWordsP.set(numWords);

    if (curCodeSize == 0 | atEndOfStream() ||
        curOffset + numWords*4 > buffer.length())
      return true;
    return false;
  }

  private BlockInfo getBlockInfo(int blockID) {
    if (!blockInfoRecords.isEmpty() && blockInfoRecords.getLast().blockID == blockID)
      return blockInfoRecords.getLast();

    for (BlockInfo info : blockInfoRecords) {
      if (info.blockID == blockID)
        return info;
    }
    return null;
  }

  /**
   * Return true if we've already read and processed the
   * lock info block for this Bitstream.  We only process it for the first
   * ursor that walks over it.
   * @return
   */
  private boolean hasBlockInfoRecords() {
    return !blockInfoRecords.isEmpty();
  }

  private boolean parseModule() {
    if (enterSubBlock(MODULE_BLOCK_ID))
      return error("Malformed block record");

    ArrayList<Long> record = new ArrayList<>();
    ArrayList<String> sectionTable = new ArrayList<>();
    ArrayList<String> gcTable = new ArrayList<>();

    // Read all the records for this module.
    while (!atEndOfStream()) {
      long code = readCode();
      if (code == END_BLOCK) {
        if (readBlockEnd())
          return error("Error at end of module block");

        // Patch the initializers for globals and alias up.
        resolveGlobalAndAliasInits();
        if (!globalInits.isEmpty() || !aliasInits.isEmpty())
          return error("Malformed global initializer set");
        if (!functionsWithBodies.isEmpty())
          return error("Too few function bodies found");

        // look for intrinsic functions which need to be upgraded at some point.
        for (Function f : theModule.getFunctionList()) {
          OutRef<Function> newFn = new OutRef<>();
          if (upgradeIntrinsicFunction(f, newFn))
            upgradedIntrinsics.add(Pair.get(f, newFn.get()));
        }

        // look for global variable which need to be renamed.
        for (GlobalVariable gv : theModule.getGlobalVariableList()) {
          upgradeGlobalVariable(gv);
        }

        globalInits.clear();
        aliasInits.clear();
        functionsWithBodies.clear();
        return false;
      }

      if (code == ENTER_SUBBLOCK) {
        switch (readSubBlockID()) {
          default:
            if (skipBlock())
              return error("Malformed block record");
            break;
          case BLOCKINFO_BLOCK_ID:
            if (readBlockInfoBlock())
              return error("Malformed BlockInfoBlock");
            break;
          case PARAMATTR_BLOCK_ID:
            if (parseAttributeBlock())
              return true;
            break;
          case TYPE_BLOCK_ID_NEW:
            if (parseTypeTable())
              return true;
            break;
          case TYPE_BLOCK_ID_OLD:
            // FIXME: Remove in LLVM 3.1
            if (parseOldTypeTable())
              return true;
            break;
          case TYPE_SYMTAB_BLOCK_ID_OLD:
            if (parseOldTypeSymbolTable())
              return true;
            break;
          case VALUE_SYMTAB_BLOCK_ID:
            if (parseValueSymbolTable())
              return true;
            break;
          case CONSTANTS_BLOCK_ID:
            if (parseConstants() || resolveGlobalAndAliasInits())
              return true;
            break;
          case METADATA_BLOCK_ID:
            if (parseMetadata())
              return true;
            break;
          case FUNCTION_BLOCK_ID:
            // if this is the first function body we're seen, reverse the functionWithBodies list.
            if (!hasReversedFunctionsWithBodies) {
              Collections.reverse(functionsWithBodies);
              hasReversedFunctionsWithBodies = true;
            }
            if (rememberAndSkipFunctionBody())
              return true;
            break;
        }
        continue;
      }

      if (code == DEFINE_ABBREV) {
        readAbbrevRecord();
        continue;
      }

      // Read a record.
      switch (readRecord(code, record)) {
        default: break;
        case MODULE_CODE_VERSION:
          // VERSION: [version#]
          if (record.size() < 1)
            return error("Malformed MODULE_CODE_VERSION");
          // Only version#0 is supported as yet.
          if (record.get(0) != 0)
            return error("Unknown bitstream version!");
          break;
        case MODULE_CODE_TRIPLE: {
          StringBuilder sb = new StringBuilder();
          if (convertToString(record, 0, sb))
            return error("Invalid MODULE_CODE_TRIPLE record");
          theModule.setTargetTriple(sb.toString());
          break;
        }
        case MODULE_CODE_DATALAYOUT: {
          // DATALAYOUT: [strchr x N]
          StringBuilder res = new StringBuilder();
          if (convertToString(record, 0, res))
            return error("Invalid MODULE_CODE_DATALAYOUT record");
          theModule.setDataLayout(res.toString());
          break;
        }
        case MODULE_CODE_ASM: {
          // ASM: [strchr x N]
          StringBuilder sb = new StringBuilder();
          if (convertToString(record, 0, sb))
            return error("Invalid MODULE_CODE_ASM record");
          theModule.setModuleInlineAsm(sb.toString());
          break;
        }
        case MODULE_CODE_DEPLIB: {
          // DEPLIB: [strchr x N]
          StringBuilder sb = new StringBuilder();
          if (convertToString(record, 0, sb))
            return error("Invalid MODULE_CODE_DEPLIB record");
          theModule.addLibrary(sb.toString());
          break;
        }
        case MODULE_CODE_SECTIONNAME: {
          // DEPLIB: [strchr x N]
          StringBuilder sb = new StringBuilder();
          if (convertToString(record, 0, sb))
            return error("Invalid MODULE_CODE_SECTIONNAME record");
          sectionTable.add(sb.toString());
          break;
        }
        case MODULE_CODE_GCNAME: {
          // DEPLIB: [strchr x N]
          StringBuilder sb = new StringBuilder();
          if (convertToString(record, 0, sb))
            return error("Invalid MODULE_CODE_SECTIONNAME record");
          gcTable.add(sb.toString());
          break;
        }
        case MODULE_CODE_GLOBALVAR: {
          // GLOBALVAR: [pointer type, isconst, initid,
          //             linkage, alignment, section, visibility, threadlocal]
          if (record.size() < 6)
            return error("Invalid MODULE_CODE_GLOBALVAR record");
          Type ty = getTypeByID(record.get(0).intValue());
          if (ty == null || !ty.isPointerType())
            return error("Invalid MODULE_CODE_GLOBALVAR record");
          PointerType pty = (PointerType) ty;
          int addrSpace = pty.getAddressSpace();
          ty = pty.getElementType();
          boolean isConstant = record.get(1) != 0;
          GlobalValue.LinkageType linkage = getDecodedLinkage(record.get(3).intValue());
          int alignment = (1 << record.get(4)) >>> 1;
          String section = "";
          if (record.get(5) != 0) {
            if (record.get(5) - 1 >= sectionTable.size())
              return error("Invalid section ID");
            section = sectionTable.get(record.get(5).intValue() - 1);
          }

          GlobalValue.VisibilityTypes visibility = GlobalValue.VisibilityTypes.DefaultVisibility;
          if (record.size() > 6)
            visibility = getDecodedVisibility(record.get(6).intValue());
          boolean isThreadLocal = false;
          if (record.size() > 7)
            isThreadLocal = record.get(7) != 0;

          GlobalVariable newGV = new GlobalVariable(theModule, ty, isConstant,
              linkage, null, "", null, addrSpace);
          newGV.setAlignment(alignment);
          newGV.setThreadLocal(isThreadLocal);
          newGV.setVisibility(visibility);
          if (!section.isEmpty())
            newGV.setSection(section);

          valueList.add(newGV);
          // remember which value to use for the global initializer.
          int initId = record.get(2).intValue();
          if (initId != 0)
            globalInits.add(Pair.get(newGV, initId - 1));
          break;
        }
        case MODULE_CODE_FUNCTION: {
          // FUNCTION:  [type, callingconv, isproto, linkage, paramattr,
          //             alignment, section, visibility, gc]
          if (record.size() < 8)
            return error("Invalid MODULE_CODE_FUNCTION record");
          Type ty = getTypeByID(record.get(0).intValue());
          if (ty == null || !ty.isPointerType())
            return error("Function not a pointer type");
          Type eltTy = ((PointerType)ty).getElementType();
          if (!eltTy.isFunctionType())
            return error("Function not a pointer to function type!");
          FunctionType fty = (FunctionType)eltTy;
          Function func = new Function(fty, GlobalValue.LinkageType.ExternalLinkage,
              "", theModule);
          func.setCallingConv(getDecodedCallingConv(record.get(1).intValue()));
          boolean isProto = record.get(2) != 0;
          func.setLinkage(getDecodedLinkage(record.get(3).intValue()));
          func.setAttributes(getAttributes(record.get(4).intValue()));
          func.setAlignment((1 << record.get(5)) >>> 1);
          if (record.get(6) != 0) {
            if (record.get(6) - 1 >= sectionTable.size())
              return error("Invalid section ID");
            func.setSection(sectionTable.get(record.get(6).intValue() - 1));
          }
          func.setVisibility(getDecodedVisibility(record.get(7).intValue()));
          if (record.size() > 8 && record.get(8) != 0) {
            if (record.get(8) - 1 > gcTable.size())
              return error("Invalid GC ID");
            // Don't set GC, because we don't need it.
          }
          valueList.add(func);
          // If this is a function with a body, remember the prototype we are
          // creating now, so that we can match up the body with them later.
          if (!isProto)
            functionsWithBodies.add(func);
          break;
        }
        case MODULE_CODE_ALIAS: {
          // ALIAS: [alias type, aliasee val#, linkage]
          // ALIAS: [alias type, aliasee val#, linkage, visibility]
          if (record.size() < 3)
            return error("Invalid MODULE_CODE_ALIAS");
          Type ty = getTypeByID(record.get(0).intValue());
          if (ty == null || !ty.isPointerType())
            return error("Alias not a pointer type");
          GlobalAlias ga = new GlobalAlias(ty, getDecodedLinkage(record.get(2).intValue()),
              "", null, theModule);
          // old bitcode files didn't have visibility field.
          if (record.size() > 3)
            ga.setVisibility(getDecodedVisibility(record.get(3).intValue()));
          valueList.add(ga);
          aliasInits.add(Pair.get(ga, record.get(1).intValue()));
          break;
        }
        case MODULE_CODE_PURGEVALS: {
          /// MODULE_CODE_PURGEVALS: [numvals]
          // trim down the value list to the specified size.
          if (record.size() < 1 || record.get(0) > valueList.size())
            return error("Invalid MODULE_CODE_PURGEVALS record");
          valueList.shrinkTo(record.get(0).intValue());
          break;
        }
      }
      record.clear();
    }

    return error("Premature end of stream");
  }

  private static CallingConv getDecodedCallingConv(int val) {
    return CallingConv.getCallingConv(val);
  }

  private AttrList getAttributes(int val) {
    if (val - 1 < mattributes.size() && val - 1 >= 0)
      return mattributes.get(val - 1);
    return null;
  }

  private BasicBlock getBasicBlock(int id) {
    if (id >= functionBBs.size()) return null;
    return functionBBs.get(id);
  }

  private static GlobalValue.LinkageType getDecodedLinkage(int val) {
    switch (val) {
      default:
      case 0: return GlobalValue.LinkageType.ExternalLinkage;
      case 1:  return GlobalValue.LinkageType.WeakAnyLinkage;
      case 2:  return GlobalValue.LinkageType.AppendingLinkage;
      case 3:  return GlobalValue.LinkageType.InternalLinkage;
      case 4:  return GlobalValue.LinkageType.LinkOnceAnyLinkage;
      case 5:  return GlobalValue.LinkageType.DLLImportLinkage;
      case 6:  return GlobalValue.LinkageType.DLLExportLinkage;
      case 7:  return GlobalValue.LinkageType.ExternalWeakLinkage;
      case 8:  return GlobalValue.LinkageType.CommonLinkage;
      case 9:  return GlobalValue.LinkageType.PrivateLinkage;
      case 10: return GlobalValue.LinkageType.WeakODRLinkage;
      case 11: return GlobalValue.LinkageType.LinkOnceODRLinkage;
      case 12: return GlobalValue.LinkageType.AvailableExternallyLinkage;
      case 13: return GlobalValue.LinkageType.LinkerPrivateLinkage;
      case 14: return GlobalValue.LinkageType.LinkerPrivateWeakLinkage;
      case 15: return GlobalValue.LinkageType.LinkerPrivateWeakDefAutoLinkage;
    }
  }

  private static GlobalValue.VisibilityTypes getDecodedVisibility(int val) {
    switch (val) {
      default:
      case 0: return GlobalValue.VisibilityTypes.DefaultVisibility;
      case 1: return GlobalValue.VisibilityTypes.HiddenVisibility;
      case 2: return GlobalValue.VisibilityTypes.ProtectedVisibility;
    }
  }

  private static Operator getDecodedCastOpcode(int val) {
    switch (val) {
      default: return Operator.None;
      case CAST_TRUNC   : return Operator.Trunc;
      case CAST_ZEXT    : return Operator.ZExt;
      case CAST_SEXT    : return Operator.SExt;
      case CAST_FPTOUI  : return Operator.FPToUI;
      case CAST_FPTOSI  : return Operator.FPToSI;
      case CAST_UITOFP  : return Operator.UIToFP;
      case CAST_SITOFP  : return Operator.SIToFP;
      case CAST_FPTRUNC : return Operator.FPTrunc;
      case CAST_FPEXT   : return Operator.FPExt;
      case CAST_PTRTOINT: return Operator.PtrToInt;
      case CAST_INTTOPTR: return Operator.IntToPtr;
      case CAST_BITCAST : return Operator.BitCast;
    }
  }

  private static Operator getDecodedBinaryOpcode(int val, Type ty) {
    switch (val) {
      default: return Operator.None;
      case BINOP_ADD:
        return ty.isFPOrFPVectorTy() ? Operator.FAdd : Operator.Add;
      case BINOP_SUB:
        return ty.isFPOrFPVectorTy() ? Operator.FSub : Operator.Sub;
      case BINOP_MUL:
        return ty.isFPOrFPVectorTy() ? Operator.FMul : Operator.Mul;
      case BINOP_UDIV: return Operator.UDiv;
      case BINOP_SDIV:
        return ty.isFPOrFPVectorTy() ? Operator.FDiv : Operator.SDiv;
      case BINOP_UREM:  return Operator.URem;
      case BINOP_SREM:
        return ty.isFPOrFPVectorTy() ? Operator.FRem : Operator.SRem;
      case BINOP_SHL: return Operator.Shl;
      case BINOP_LSHR: return Operator.LShr;
      case BINOP_ASHR: return Operator.AShr;
      case BINOP_AND: return Operator.And;
      case BINOP_OR: return Operator.Or;
      case BINOP_XOR: return Operator.Xor;
    }
  }

  /**
   * FIXME: Only used for {@linkplain #parseOldTypeTable()} which will be removed in LLVM 3.1
   * @param id
   * @return
   */
  private Type getTypeByIDOrNull(int id) {
    if (id >= typeList.size()) {
      typeList.addAll(Collections.nCopies(id - typeList.size(), null));
    }
    return typeList.get(id);
  }

  private Type getTypeByID(int id) {
    if (id >= typeList.size()) return null;
    // if the type id is in the range, return it.
    Type res = typeList.get(id);
    if (res != null) return res;
    // we encounter a forward reference, just create placeholder here.
    res = StructType.create(context);
    typeList.set(id, res);
    return res;
  }

  /**
   * Convert a string from a record into a String object, return true on failure.
   * @param record
   * @param index
   * @param result
   * @return
   */
  private static boolean convertToString(ArrayList<Long> record,
                                         int index, StringBuilder result) {
    if (index > record.size())
      return true;

    for (int i = index,e  = record.size(); i < e; i++)
      result.append((char)record.get(i).intValue());
    return false;
  }

  /**
   * When we see the block for a function body,
   * remember where it is and then skip it.  This lets us lazily deserialize the
   * functions.
   * @return
   */
  private boolean rememberAndSkipFunctionBody() {
    // get the function we are entering
    if (functionsWithBodies.isEmpty())
      return error("Insufficient function protos");
    Function fn = functionsWithBodies.getLast();
    functionsWithBodies.removeLast();

    // save the current stream state.
    long curBit = getCurrentBitNo();
    deferredFunctionInfo.put(fn, curBit);
    // skip over the function block for now.
    if (skipBlock())
      return error("Malformed block record");
    return false;
  }

  /**
   * Return the number of bits we've read.
   * @return
   */
  private long getCurrentBitNo() {
    return curOffset * 8 - bitsInCurWord;
  }

  private boolean parseMetadata() {
    if (enterSubBlock(METADATA_BLOCK_ID))
      return error("Malformed metadata block record");

    int nextMDValueNo = mdValueList.size();
    ArrayList<Long> record = new ArrayList<>();
    while (true) {
      long code = readCode();
      if (code == END_BLOCK) {
        if (readBlockEnd())
          return error("Error at end of PARAMETER block");
        return false;
      }

      if (code == ENTER_SUBBLOCK) {
        // no known subblocks, skip them.
        readSubBlockID();
        if (skipBlock())
          return error("Malformed block record");
        continue;
      }

      if (code == DEFINE_ABBREV) {
        readAbbrevRecord();
        continue;
      }

      boolean isFunctionLocal = false;
      record.clear();
      code = readRecord(code, record);
      switch ((int) code) {
        default: break;
        case MetadataCodes.METADATA_NAME: {
          // Read named of the named metadata.
          StringBuilder name = new StringBuilder();
          convertToString(record, 0, name);
          record.clear();
          code = readCode();
          // METADATA_NAME is alwasy followed by METADATA_NAMED_NODE
          int nextBitCode = readRecord(code, record);
          Util.assertion(nextBitCode == MetadataCodes.METADATA_NAMED_NODE);
          // read named metadata elements.
          int size = record.size();
          NamedMDNode nmd = theModule.getOrCreateNamedMetadata(name.toString());
          for (int i = 0; i < size; ++i) {
            MDNode md = (MDNode) mdValueList.getValueFwdRef(Math.toIntExact(record.get(i)));
            if (md == null)
              return error("Malformed metadata record");
            nmd.addOperand(md);
          }
          break;
        }
        case MetadataCodes.METADATA_FN_NODE:
          isFunctionLocal = true;
          // fall through.
        case MetadataCodes.METADATA_NODE: {
          if ((record.size() % 2) == 1)
            return error("Invalid METADATA_NODE record");
          int size = record.size();
          ArrayList<Value> elts = new ArrayList<>();
          for (int i = 0; i < size; i+=2) {
            Type ty = getTypeByID(Math.toIntExact(record.get(i)));
            if (ty == null)
              return error("Invalid METADATA_NODE record");
            if (ty.isMetadataTy())
              elts.add(mdValueList.getValueFwdRef(Math.toIntExact(record.get(i + 1))));
            else if (!ty.isVoidType())
              elts.add(valueList.getValueFwdRef(Math.toIntExact(record.get(i + 1)), ty));
            else
              elts.add(null);
          }
          Value v = MDNode.get(context, elts, isFunctionLocal);
          isFunctionLocal = false;
          mdValueList.assignValue(v, nextMDValueNo++);
          break;
        }
        case MetadataCodes.METADATA_STRING: {
          StringBuilder name = new StringBuilder();
          convertToString(record, 0, name);
          Value v = MDString.get(context, name.toString());
          mdValueList.assignValue(v, nextMDValueNo++);
          break;
        }
        case MetadataCodes.METADATA_KIND: {
          int recordLength = record.size();
          if (recordLength < 2)
            return error("Invalid METADATA_KIND record");
          StringBuilder name = new StringBuilder();
          convertToString(record, 1, name);
          int kind = Math.toIntExact(record.get(0));
          int newKind = theModule.getMDKindID(name.toString());
          if (mdKindMap.containsKey(kind))
            return error("Conflicting METADATA_KIND record");
          mdKindMap.put(kind, newKind);
          break;
        }
      }
    }
  }

  private boolean parseMetadataAttachment() {
    if (enterSubBlock(METADATA_ATTACHMENT_ID))
      return error("Malformed block record");

    ArrayList<Long> record = new ArrayList<>();
    while (true) {
      long code = readCode();
      if (code == END_BLOCK) {
        if (readBlockEnd())
          return error("Error at end of PARAMETER_BLOCK");
        break;
      }
      if (code == DEFINE_ABBREV) {
        readAbbrevRecord();
        continue;
      }

      record.clear();
      switch (readRecord(code, record)) {
        default: break;
        case MetadataCodes.METADATA_ATTACHMENT: {
          int recordLength = record.size();
          if (record.isEmpty() || ((recordLength - 1) %2) == 1)
            return error("Invalid METADATA_ATTACHMENT reader!");
          Instruction inst = instructionList.get(record.get(0).intValue());
          for (int i = 1; i < recordLength; i+=2) {
            int kind = record.get(i).intValue();
            if (!mdKindMap.containsKey(kind))
              return error("Invalid metadata kind id");
            Value node = mdValueList.getValueFwdRef(record.get(i+1).intValue());
            inst.setMetadata(kind, (MDNode) node);
          }
          break;
        }
      }
    }
    return false;
  }

  private boolean parseConstants() {
    if (enterSubBlock(CONSTANTS_BLOCK_ID))
      return error("Malformed block record");

    ArrayList<Long> record = new ArrayList<>();
    // read all the records for this value table.
    Type curTy = Type.getInt32Ty(context);
    int nextCstNo = valueList.size();
    while (true) {
      long code = readCode();
      if (code == END_BLOCK)
        break;

      if (code == ENTER_SUBBLOCK) {
        // No known subblocks, always skip them.
        readSubBlockID();
        if (skipBlock())
          return error("Malformed block record");
        continue;
      }

      if (code == DEFINE_ABBREV) {
        readAbbrevRecord();
        continue;
      }

      // read a record.
      record.clear();
      Value v = null;
      int bitcode = readRecord(code, record);
      switch (bitcode) {
        default:
        case CST_CODE_UNDEF:
          // UNDEF
          v = Value.UndefValue.get(curTy);
          break;
        case CST_CODE_SETTYPE:
          // SETTYPE: [typeid]
          if (record.isEmpty())
            return error("Malformed CST_CODE_SETTYPE record");
          if (record.get(0).intValue() >= typeList.size())
            return error("Invalid Type ID in CST_CODE_SETTYPE record");
          curTy = typeList.get(record.get(0).intValue());
          continue;
        case CST_CODE_NULL:
          // INTEGER: [NULL]
          v = Constant.getNullValue(curTy);
          break;
        case CST_CODE_INTEGER:
          // INTEGER: [intval]
          if (!curTy.isIntegerTy() || record.isEmpty())
            return error("Invalid CST_CODE_INTEGER record");
          v = ConstantInt.get(curTy, decodeSignRotatedValue(record.get(0)));
          break;
        case CST_CODE_WIDE_INTEGER: {
          // WIDE_INTEGER: [n x intval]
          if (!curTy.isIntegerTy() || record.isEmpty())
            return error("Invalid WIDE_CODE_INTEGER record");

          int numWords = record.size();
          ArrayList<Long> words = new ArrayList<>();
          for (int i = 0; i < numWords; i++)
            words.add(decodeSignRotatedValue(record.get(i)));
          v = ConstantInt.get(context, new APInt(((IntegerType)curTy).getBitWidth(), words));
          break;
        }
        case CST_CODE_FLOAT: {
          // FLOAT: [fpval]
          if (record.isEmpty())
            return error("Invalid CST_CODE_FLOAT");
          if (curTy.isFloatTy())
            v = ConstantFP.get(context, new APFloat(new APInt(32, record.get(0).intValue())));
          else if (curTy.isDoubleTy())
            v = ConstantFP.get(context, new APFloat(new APInt(64, record.get(0))));
          else if (curTy.isX86_FP80Ty()) {
            // bits are not stored the same way as a normal i80 APInt, compensate.
            long[] rearrange = new long[] {
              (record.get(1) & 0xFFFFL) | (record.get(0) << 16),
              record.get(0) >>> 48
            };
            v = ConstantFP.get(context, new APFloat(new APInt(80, rearrange)));
          }
          else if (curTy.isFP128Ty()) {
            long[] rearrange = new long[] {record.get(0), record.get(1)};
            v = ConstantFP.get(context, new APFloat(new APInt(128, rearrange), true));
          }
          else if (curTy.isPPC_FP128Ty()) {
            long[] rearrange = new long[] {record.get(0), record.get(1)};
            v = ConstantFP.get(context, new APFloat(new APInt(128, rearrange)));
          }
          else
            v = Value.UndefValue.get(curTy);
          break;
        }
        case CST_CODE_AGGREGATE: {
          // AGGREGATE: [n x value number]
          if (record.isEmpty())
            return error("Invalid CST_AGGREGATE record");

          int size = record.size();
          ArrayList<Constant> elts = new ArrayList<>();
          if (curTy instanceof StructType) {
            StructType sty = (StructType) curTy;
            for (int i = 0; i < size; i++)
              elts.add(valueList.getConstantFwdRef(record.get(0).intValue(), sty.getElementType(i)));
            v = ConstantStruct.get(sty, elts);
          }
          else if (curTy instanceof ArrayType) {
            ArrayType aty = (ArrayType) curTy;
            Type eltTy = aty.getElementType();
            for (int i = 0; i < size; i++)
              elts.add(valueList.getConstantFwdRef(record.get(i).intValue(), eltTy));
            v = ConstantArray.get(aty, elts);
          }
          else if (curTy instanceof VectorType) {
            VectorType vecTy = (VectorType) curTy;
            Type eltTy = vecTy.getElementType();
            for (int i = 0; i < size; i++)
              elts.add(valueList.getConstantFwdRef(record.get(i).intValue(), eltTy));
            v = ConstantVector.get(elts);
          }
          else
            v = Value.UndefValue.get(curTy);
          break;
        }

        case CST_CODE_STRING: {
          // STRING: [values]
          if (record.isEmpty())
            return error("Invlaid CST_CODE_STRING");

          ArrayType aty = (ArrayType) curTy;
          Type eltTy = aty.getElementType();

          int size = record.size();
          ArrayList<Constant> elts = new ArrayList<>();
          for (int i = 0; i < size; i++)
            elts.add(ConstantInt.get(eltTy, record.get(i).intValue()));
          v = ConstantArray.get(aty, elts);
          break;
        }
        case CST_CODE_CSTRING: {
          // CSTRING: [values]
          if (record.isEmpty())
            return error("Invalid CST_CODE_CSTRING record");
          ArrayType aty = (ArrayType) curTy;
          Type eltTy = aty.getElementType();
          int size = record.size();
          ArrayList<Constant> elts = new ArrayList<>();
          for (int i = 0; i < size; i++)
            elts.add(ConstantInt.get(eltTy, record.get(i).intValue()));
          elts.add(Constant.getNullValue(eltTy));
          v = ConstantArray.get(aty, elts);
          break;
        }
        case CST_CODE_CE_BINOP: {
          // CE_BINOP: [opcode, opval, opval]
          if (record.size() < 3)
            return error("Invalid CSE_CODE_CE_BINOP record");
          Operator opc = getDecodedBinaryOpcode(record.get(0).intValue(), curTy);
          if (opc == Operator.None)
            v = Value.UndefValue.get(curTy);
          else {
            Constant lhs = valueList.getConstantFwdRef(record.get(1).intValue(), curTy);
            Constant rhs = valueList.getConstantFwdRef(record.get(2).intValue(), curTy);
            int flags = 0;
            if (record.size() >= 4) {
              // Ignores all flags
              // TODO
            }
            v = ConstantExpr.get(opc, lhs, rhs);
          }
          break;
        }
        case CST_CODE_CE_CAST: {
          // CE_CAST: [opcode, opty, opval]
          if (record.size() < 3)
            return error("Invalid CST_CODE_CE_CAST record");
          Operator opc = getDecodedCastOpcode(record.get(0).intValue());
          if (opc == Operator.None)
            v = Value.UndefValue.get(curTy);
          else {
            Type opTy = getTypeByID(record.get(1).intValue());
            if (opTy == null)
              return error("Invalid CE_CAST record");
            Constant op = valueList.getConstantFwdRef(record.get(2).intValue(), opTy);
            v = ConstantExpr.getCast(opc, op, curTy);
          }
          break;
        }
        case CST_CODE_CE_INBOUNDS_GEP:
        case CST_CODE_CE_GEP: {
          // CE_GEP:        [n x operands]
          if ((record.size() & 1) != 0)
            return error("Invalid CE_GEP record");
          ArrayList<Constant> elts = new ArrayList<>();
          for (int i = 0, e = record.size(); i < e; i+=2) {
            Type eltTy = getTypeByID(record.get(i).intValue());
            if (eltTy == null) return error("Invalid CE_GEP record");
            elts.add(valueList.getConstantFwdRef(record.get(i+1).intValue(), eltTy));
          }
          if (bitcode == CST_CODE_CE_INBOUNDS_GEP)
            v = ConstantExpr.getInBoundsGetElementPtr(elts.get(0), elts.subList(1, elts.size()));
          else
            v = ConstantExpr.getGetElementPtr(elts.get(0), elts.subList(1, elts.size()));
          break;
        }
        case CST_CODE_CE_SELECT: {
          // CE_SELECT: [opval#, opval#, opval#]
          if (record.size() < 3)
            return error("Invalid CSE_CODE_CE_SELECT record");
          v = ConstantExpr.getSelect(
              valueList.getConstantFwdRef(record.get(0).intValue(), Type.getInt1Ty(context)),
              valueList.getConstantFwdRef(record.get(1).intValue(), curTy),
              valueList.getConstantFwdRef(record.get(2).intValue(), curTy));
          break;
        }
        case CST_CODE_CE_EXTRACTELT: {
          // CE_EXTRACTELT: [opty, opval, opval]
          if (record.size() < 3) return error("Invalid CE_EXTRACTELT record");
          Type resTy = getTypeByID(record.get(0).intValue());
          VectorType opTy = resTy instanceof VectorType ? (VectorType)resTy : null;
          if (opTy == null)
            return error("Invalid CE_EXTRACTELEMENT record");
          Constant op0 = valueList.getConstantFwdRef(record.get(1).intValue(), opTy);
          Constant op1 = valueList.getConstantFwdRef(record.get(2).intValue(), opTy);
          v = ConstantExpr.getExtractElement(op0, op1);
          break;
        }
        case CST_CODE_CE_INSERTELT: {
          // CE_INSERTELT: [opval, opval, opval]
          if (record.size() < 3) return error("Invalid CE_INSERTELT record");
          VectorType opTy = (VectorType) curTy;
          Constant op0 = valueList.getConstantFwdRef(record.get(0).intValue(), opTy);
          Constant op1 = valueList.getConstantFwdRef(record.get(1).intValue(), opTy.getElementType());
          Constant op2 = valueList.getConstantFwdRef(record.get(2).intValue(), Type.getInt32Ty(context));
          v = ConstantExpr.getInsertElement(op0, op1, op2);
          break;
        }
        case CST_CODE_CE_SHUFFLEVEC: {
          // CE_SHUFFLEVEC: [opval, opval, opval]
          if (record.size() < 3) return error("Invalid CE_SHUFFLEVEC record");
          VectorType opTy = (VectorType) curTy;
          Constant op0 = valueList.getConstantFwdRef(record.get(0).intValue(), opTy);
          Constant op1 = valueList.getConstantFwdRef(record.get(1).intValue(), opTy);
          Type shufTy = VectorType.get(Type.getInt32Ty(context), opTy.getNumElements());
          Constant op2 = valueList.getConstantFwdRef(record.get(2).intValue(), shufTy);
          v = ConstantExpr.getShuffleVector(op0, op1, op2);
          break;
        }
        case CST_CODE_CE_SHUFVEC_EX: {
          // [opty, opval, opval, opval]
          if (record.size() < 4) return error("Invalid CE_SHUFFLEVEC_EX record");
          if (!(curTy instanceof VectorType) || !(getTypeByID(record.get(0).intValue()) instanceof VectorType))
            return error("Invalid CE_SHUFFLEVEC_EX record");

          VectorType rty = (VectorType) curTy;
          VectorType opTy = (VectorType)getTypeByID(record.get(0).intValue());

          Constant op0 = valueList.getConstantFwdRef(record.get(1).intValue(), opTy);
          Constant op1 = valueList.getConstantFwdRef(record.get(2).intValue(), opTy);
          Type shufTy = VectorType.get(Type.getInt32Ty(context), rty.getNumElements());
          Constant op2 = valueList.getConstantFwdRef(record.get(2).intValue(), shufTy);
          v = ConstantExpr.getShuffleVector(op0, op1, op2);
          break;
        }
        case CST_CODE_CE_CMP: {
          // CE_CMP: [opty, opval, opval, pred]
          if (record.size() < 4)
            return error("Invalid CST_CODE_CE_CMP record");
          Type opTy = getTypeByID(record.get(0).intValue());

          Constant op0 = valueList.getConstantFwdRef(record.get(1).intValue(), opTy);
          Constant op1 = valueList.getConstantFwdRef(record.get(2).intValue(), opTy);
          if (opTy.isFPOrFPVectorTy())
            v = ConstantExpr.getFCmp(getDecodedPredicate(record.get(3)), op0, op1);
          else
            v = ConstantExpr.getICmp(getDecodedPredicate(record.get(3)), op0, op1);
          break;
        }
        case CST_CODE_INLINEASM: {
          if (record.size() < 2)
            return error("Invalid CST_INLINE Code record");

          StringBuilder asmStr = new StringBuilder();
          // constraint string.
          StringBuilder constrStr = new StringBuilder();
          boolean hasSideEffects = (record.get(0) & 1) != 0;
          boolean isAlignStack = (record.get(0) >> 1) != 0;
          int asmStrSize = record.get(1).intValue();
          if (2 + asmStrSize >= record.size())
            return error("Invalid CST_INLINEASM record");
          int constrStrSize = record.get(2+asmStrSize).intValue();
          if (3 + asmStrSize + constrStrSize > record.size())
            return error("Invlaid CST_INLINE record");

          for (int i = 0; i < asmStrSize; i++)
            asmStr.append((char)record.get(2+i).intValue());
          for (int i = 0; i < constrStrSize; i++)
            constrStr.append((char)record.get(3+asmStrSize+i).intValue());

          PointerType pty = (PointerType)curTy;
          v = InlineAsm.get((FunctionType)pty.getElementType(),
              asmStr.toString(), constrStr.toString(),
              hasSideEffects, isAlignStack);
          break;
        }
        case CST_CODE_BLOCKADDRESS: {
          if (record.size() < 3)
            return error("Invalid CE_BLOCKADDRESS record");
          Type fnTy = getTypeByID(record.get(0).intValue());
          if (fnTy == null)
            return error("Invalid CE_BLOCKADDRESS record");
          Value val = valueList.getConstantFwdRef(record.get(1).intValue(), fnTy);
          if (!(val instanceof Function))
            return error("Invalid CE_BLOCKADDRESS record");
          Function fn = (Function) val;
          GlobalVariable fwdRef = new GlobalVariable(fn.getParent(),
              Type.getInt8Ty(context), false,
              GlobalValue.LinkageType.InternalLinkage,
              null, "", null, 0);

          ArrayList<Pair<Long, GlobalVariable>> list;
          if (blockAddrFwdRefs.containsKey(fn))
            list = blockAddrFwdRefs.get(fn);
          else {
            list = new ArrayList<>();
            blockAddrFwdRefs.put(fn, list);
          }

          list.add(Pair.get(record.get(2), fwdRef));
          v = fwdRef;
          break;
        }
      }
      valueList.assignValue(v, nextCstNo);
      ++nextCstNo;
    }

    if (nextCstNo != valueList.size())
      return error("Invalid constant reference!");

    if (readBlockEnd())
      return error("Error at end of constants block");

    // Once all constants have been read, go through and resolve forward references.
    valueList.resolveConstantForwardRefs();
    return false;
  }

  private Instruction.CmpInst.Predicate getDecodedPredicate(long val) {
    return Instruction.CmpInst.Predicate.getPred((int) val);
  }

  /**
   * Decode a signed value stored with the sign bit in the LSB for dense VBR encoding.
   * @param val
   * @return
   */
  private long decodeSignRotatedValue(long val) {
    if ((val & 1) == 0)
      return val >>> 1;
    if (val != 1)
      return -(val >>> 1);
    // there is no such thing as -0 with integers. "0" really means MININT.
    return 1L << 63;
  }

  private boolean parseValueSymbolTable() {
    if (enterSubBlock(VALUE_SYMTAB_BLOCK_ID))
      return error("Malformed block record");

    ArrayList<Long> record = new ArrayList<>();
    while (true) {
      long code = readCode();
      if (code == END_BLOCK) {
        if (readBlockEnd())
          return error("Error at end of value symbol table block");
        return false;
      }

      if (code == ENTER_SUBBLOCK) {
        readSubBlockID();
        if (skipBlock())
          return error("Malformed block record");
        continue;
      }

      if (code == DEFINE_ABBREV) {
        readAbbrevRecord();
        continue;
      }
      // read a record.
      record.clear();
      StringBuilder valueName = new StringBuilder();
      switch (readRecord(code, record)) {
        default:
          break;
        case VST_CODE_ENTRY: {
          // VST_ENTRY: [valueid, namechar x N]
          if (convertToString(record, 1, valueName))
            return error("Invalid VST_ENTRY record");
          int valueID = record.get(0).intValue();
          if (valueID >= valueList.size())
            return error("Invlaid Value ID in VST_ENTRY record");
          Value v = valueList.get(valueID);

          v.setName(valueName.toString());
          break;
        }
        case VST_CODE_BBENTRY: {
          if (convertToString(record, 1, valueName))
            return error("Invalid VST_BBENTRY record");
          BasicBlock bb = getBasicBlock(record.get(0).intValue());
          if (bb == null)
            return error("Invlaid BB ID in VST_BBENTRY record");

          bb.setName(valueName.toString());
          break;
        }
      }
    }
  }

  private boolean parseOldTypeSymbolTable() {
    if (enterSubBlock(TYPE_SYMTAB_BLOCK_ID_OLD))
      return error("Malformed block record");

    ArrayList<Long> record = new ArrayList<>();

    while (true) {
      long code = readCode();
      if (code == END_BLOCK) {
        if (readBlockEnd())
          return error("Error at the end of type symbol table block");
        return false;
      }

      if (code == ENTER_SUBBLOCK) {
        readSubBlockID();
        if (skipBlock())
          return error("Malformed block record");
        continue;
      }
      if (code == DEFINE_ABBREV) {
        readAbbrevRecord();
        continue;
      }

      // read a record.
      record.clear();
      StringBuilder typeName = new StringBuilder();
      switch (readRecord(code, record)) {
        default:
          // Uknown type.
          break;
        case TST_CODE_ENTRY:
          // TST_ENTRY: [type, namechar x N]
          if (convertToString(record, 1, typeName))
            return error("Invlaid TST_ENTRY record");
          int typeID = record.get(0).intValue();
          if (typeID >= typeList.size())
            return error("Invalid Type ID in TST_ENTRY record");

          theModule.addTypeName(typeName.toString(), typeList.get(typeID));
          break;
      }
    }
  }

  private boolean parseTypeTable() {
    if (enterSubBlock(TYPE_BLOCK_ID_NEW))
      return error("malformed block record");
    return parseTypeTableBody();
  }

  private boolean parseTypeTableBody() {
    Util.assertion(typeList.isEmpty(), "Multiple TYPE_BLOCKs found!");

    ArrayList<Long> record = new ArrayList<>();
    int numRecords = 0;
    StringBuilder typeName = new StringBuilder();
    // read all records for this type table.
    while (true) {
      long code = readCode();
      if (code == END_BLOCK) {
        if (numRecords != typeList.size())
          return error("Invalid type forward reference in TYPE_BLOCK");
        if (readBlockEnd())
          return error("Error at end of type table block");
        return false;
      }

      if (code == ENTER_SUBBLOCK) {
        // unknown subblock.
        readSubBlockID();
        if (skipBlock())
          return error("Malformed block record");
        continue;
      }

      if (code == DEFINE_ABBREV) {
        readAbbrevRecord();
        continue;
      }

      // read a record.
      record.clear();
      Type resultTy = null;
      switch (readRecord(code, record)) {
        default: return error("Unknown type in type table!");
        case TYPE_CODE_NUMENTRY:
          if (record.isEmpty()) return error("Invalid TYPE_CODE_NUMENTRY record");
          int len = Math.toIntExact(record.get(0));
          if (typeList.size() < len)
            typeList.addAll(Collections.nCopies(len - typeList.size(), null));
          else {
            while (typeList.size() > len)
              typeList.remove(typeList.size() - 1);
          }
          continue;
        case TYPE_CODE_VOID:
          resultTy = Type.getVoidTy(context);
          break;
        case TYPE_CODE_FLOAT:
          resultTy = Type.getFloatTy(context);
          break;
        case TYPE_CODE_DOUBLE:
          resultTy = Type.getDoubleTy(context);
          break;
        case TYPE_CODE_X86_FP80:
          resultTy = Type.getX86_FP80Ty(context);
          break;
        case TYPE_CODE_FP128:
          resultTy = Type.getFP128Ty(context);
          break;
        case TYPE_CODE_PPC_FP128:
          resultTy = Type.getPPC_FP128Ty(context);
          break;
        case TYPE_CODE_LABEL:
          resultTy = Type.getLabelTy(context);
          break;
        case TYPE_CODE_METADATA:
          resultTy = Type.getMetadataTy(context);
          break;
        case TYPE_CODE_INTEGER:
          // INTEGER: [width]
          if (record.isEmpty()) return error("Invalid integer type record");
          resultTy = IntegerType.get(context, Math.toIntExact(record.get(0)));
          break;
        case TYPE_CODE_POINTER: {
          // POINTER: [pointee type] or [pointee type, addres space]
          if (record.isEmpty()) return error("Invalid pointer type record");
          int addressSpace = 0;
          if (record.size() == 2)
            addressSpace = Math.toIntExact(record.get(1));
          resultTy = getTypeByID(Math.toIntExact(record.get(0)));
          if (resultTy == null) return error("Invalid element type in pointer type");
          resultTy = PointerType.get(resultTy, addressSpace);
          break;
        }
        case TYPE_CODE_FUNCTION: {
          // FUNCTION: [vararg, attrid, retty, paramty x N]
          if (record.size() < 3)
            return error("Invalid FUNCTION type record");
          ArrayList<Type> argTys = new ArrayList<>();
          for (int i = 3, e = record.size(); i < e; ++i) {
            Type t = getTypeByID(Math.toIntExact(record.get(i)));
            if (t != null)
              argTys.add(t);
            else
              break;
          }
          resultTy = getTypeByID(Math.toIntExact(record.get(2)));
          if (resultTy == null || argTys.size() < record.size() - 3)
            return error("Invalid argument type in function type");
          resultTy = FunctionType.get(resultTy, argTys, record.get(0) != 0);
          break;
        }
        case TYPE_CODE_STRUCT_ANON: {
          // STRUCT: [ispacked, elety x N]
          if (record.isEmpty())
            return error("Invalid STRUCT type record");
          ArrayList<Type> eltTys = new ArrayList<>();
          for (int i = 1, e = record.size(); i < e; ++i) {
            Type t = getTypeByID(Math.toIntExact(record.get(i)));
            if (t != null)
              eltTys.add(t);
            else
              break;
          }
          if (eltTys.size() != record.size() - 1)
            return error("Invalid member type in struct type");
          resultTy = StructType.get(context, eltTys, record.get(0) != 0);
          break;
        }
        case TYPE_CODE_STRUCT_NAME: {
          // STRUCT_NAME: [strchr x N]
          if (convertToString(record, 0, typeName))
            return error("Invalid STRUCT_NAME record");
          continue;
        }
        case TYPE_CODE_STRUCT_NAMED: {
          // STRUCT: [ispacked, eltty x N]
          if (record.isEmpty()) return error("Invalid STRUCT type record");
          if (numRecords >= typeList.size())
            return error("Invalid TYPE table");

          Type tmp = typeList.get(numRecords);
          StructType res = tmp instanceof StructType ? (StructType) tmp : null;
          if (res != null) {
            res.setName(typeName.toString());
            typeList.set(numRecords, null);
          } else {
            res = StructType.create(context, typeName.toString());
          }
          typeName.delete(0, typeName.length());
          ArrayList<Type> eleTys = new ArrayList<>();
          for (int i = 1, e = record.size(); i < e; ++i) {
            Type t = getTypeByID(Math.toIntExact(record.get(i)));
            if (t != null)
              eleTys.add(t);
            else
              break;
          }
          if (eleTys.size() != record.size() - 1)
            return error("Invalid member type in STRUCT type record");
          res.setBody(eleTys, record.get(0) != 0);
          resultTy = res;
          break;
        }
        case TYPE_CODE_OPAQUE: {
          // OPAQUE: []
          if (record.isEmpty())
            return error("Invalid OPAQUE type record");
          if (numRecords >= typeList.size())
            return error("invalid TYPE table");
          Type tmp = typeList.get(numRecords);
          StructType res = tmp instanceof StructType ? (StructType) tmp : null;
          if (res != null) {
            res.setName(typeName.toString());
            typeList.set(numRecords, null);
          } else {
            res = StructType.create(context, typeName.toString());
          }
          typeName.delete(0, typeName.length());
          resultTy = res;
          break;
        }
        case TYPE_CODE_ARRAY: {
          // ARRAY: [numelets, elety]
          if (record.size() < 2) return error("Invalid ARRAY type record");
          resultTy = getTypeByID(Math.toIntExact(record.get(1)));
          if (resultTy != null)
            resultTy = ArrayType.get(resultTy, record.get(0));
          else
            return error("Invalid ARRAY type record");
          break;
        }
        case TYPE_CODE_VECTOR: {
          // VECTOR: [numelets, elety]
          if (record.size() < 2) return error("Invalid VECTOR type record");
          resultTy = getTypeByID(Math.toIntExact(record.get(1)));
          if (resultTy != null)
            resultTy = VectorType.get(resultTy, record.get(0));
          else
            return error("Invalid VECTOR type record");
          break;
        }
      }
      if (numRecords >= typeList.size())
        return error("invalid TYPE table");
      Util.assertion(resultTy != null, "Didn't read a type?");
      Util.assertion(typeList.get(numRecords) == null, "already read type?");
      typeList.set(numRecords++, resultTy);
    }
  }

  /**
   * Parse the type table.
   * FIXME: remove in LLVM 3.1
   * @return
   */
  private boolean parseOldTypeTable() {
    if (enterSubBlock(TYPE_BLOCK_ID_OLD))
      return error("Malformed block record");
    if (!typeList.isEmpty())
      return error("Multiple TYPE_BLOCKs found!");

    ArrayList<Long> record = new ArrayList<>();
    int numRecords = 0;

    // read all the records for this type table.
    while (true) {
      long code = readCode();
      if (code == END_BLOCK) {
        if (numRecords != typeList.size())
          return error("Invalid type forward reference in TYPE_BLOCK");
        if (readBlockEnd())
          return error("Error at end of type table block");
        return false;
      }

      if (code == ENTER_SUBBLOCK) {
        // no known sub-blocks, always skip them.
        readSubBlockID();
        if (skipBlock())
          return error("Malformed block record");
        continue;
      }

      if (code == DEFINE_ABBREV) {
        readAbbrevRecord();
        continue;
      }

      // read a record.
      record.clear();
      Type resultTy = null;
      switch (readRecord(code, record)) {
        default:
          // Unknown type.
          break;
        case TYPE_CODE_NUMENTRY:
          // TYPE_CODE_NUMENTRY: [numentries]
          // TYPE_CODE_NUMENTRY contains a count of the number of types in the
          // type list.  This allows us to reserve space.
          if (record.isEmpty())
            return error("Invalid TYPE_CODE_NUMENTRY record");
          typeList.ensureCapacity(Math.toIntExact(record.get(0)));
          continue;
        case TYPE_CODE_VOID:
          resultTy = Type.getVoidTy(context);
          break;
        case TYPE_CODE_FLOAT:
          resultTy = Type.getFloatTy(context);
          break;
        case TYPE_CODE_DOUBLE:
          resultTy = Type.getDoubleTy(context);
          break;
        case TYPE_CODE_X86_FP80:
          resultTy = Type.getX86_FP80Ty(context);
          break;
        case TYPE_CODE_FP128:
          resultTy = Type.getFP128Ty(context);
          break;
        case TYPE_CODE_PPC_FP128:
          resultTy = Type.getPPC_FP128Ty(context);
          break;
        case TYPE_CODE_LABEL:
          resultTy = Type.getLabelTy(context);
          break;
        case TYPE_CODE_OPAQUE:
          resultTy = null;
          break;
        case TYPE_CODE_METADATA:
          resultTy = Type.getMetadataTy(context);
          break;
        case TYPE_CODE_INTEGER:
          if (record.isEmpty())
            return error("Invalid Integer type record");
          resultTy = IntegerType.get(context, record.get(0).intValue());
          break;
        case TYPE_CODE_POINTER: {
          // POINTER: [pointee type] or
          //          [pointee type, address space]
          if (record.isEmpty())
            return error("Invalid Pointer Type record");
          int addressSpace = 0;
          if (record.size() == 2)
            addressSpace = record.get(1).intValue();
          resultTy = PointerType.get(getTypeByIDOrNull(record.get(0).intValue()),
              addressSpace);
          break;
        }
        case TYPE_CODE_FUNCTION: {
          // FIXME: attrid is dead, remove it in LLVM 3.0
          // FUNCTION: [vararg, attrid, retty, paramty x N]
          if (record.size() < 3)
            return error("Invalid FUNCTION type record");
          ArrayList<Type> argTy = new ArrayList<>();
          for (int i = 3, e = record.size(); i < e; i++)
            argTy.add(getTypeByIDOrNull(record.get(i).intValue()));;

          resultTy = FunctionType.get(getTypeByIDOrNull(record.get(2).intValue()),
              argTy, record.get(0) != 0);
          break;
        }
        case TYPE_CODE_STRUCT_ANON: {
          // STRUCT: [ispacked, eltty x N]
          if (record.isEmpty())
            return error("Invalid STRUCT type record");

          ArrayList<Type> eltTys = new ArrayList<>();
          for (int i = 1, e = record.size(); i < e; i++)
            eltTys.add(getTypeByIDOrNull(record.get(i).intValue()));
          resultTy = StructType.get(context, eltTys, record.get(0) != 0);
          break;
        }
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_VECTOR: {
          // ARRAY: [numelts, eltty]
          // or VECTOR: [numelts, elety]
          boolean isArray = code == TYPE_CODE_ARRAY;
          if (record.size() != 2)
            return error(String.format("Invalid %s type record", isArray ? "ARRAY" : "VECTOR"));

          if (isArray)
            resultTy = ArrayType.get(getTypeByIDOrNull(record.get(1).intValue()),
                record.get(0));
          else
            resultTy = VectorType.get(getTypeByIDOrNull(record.get(1).intValue()),
                record.get(0));
          break;
        }
      }

      if (numRecords == typeList.size()) {
        // if this is a new type slot, just append it to the
        // tail of typeList.
        typeList.add(resultTy != null ? resultTy : OpaqueType.get(context));;
        ++numRecords;
      }
      else if (resultTy == null) {
        // Otherwise, this was forward referenced, so an opaque type was created,
        // but the result type is actually just an opaque.  Leave the one we
        // created previously.
        ++numRecords;
      }
      else {
        // Otherwise, this was forward referenced, so an opaque type was created.
        // Resolve the opaque type to the real type now.
        Util.assertion(numRecords < typeList.size(),
            "typeList imbalance");
        OpaqueType oldTy = (OpaqueType) typeList.get(numRecords++);
        // Don't directly push the new type on the Tab. Instead we want to replace
        // the opaque type we previously inserted with the new concrete value. The
        // refinement from the abstract (opaque) type to the new type causes all
        // uses of the abstract type to use the concrete type (NewTy). This will
        // also cause the opaque type to be deleted.
        oldTy.refineAbstractTypeTo(resultTy);
        Util.assertion(typeList.get(numRecords-1) != oldTy,
            "refineAbstractTypeTo didn't work!");
      }
    }
  }

  /**
   * A method used to parse attribute block.
   * @return
   */
  private boolean parseAttributeBlock() {
    if (enterSubBlock(PARAMATTR_BLOCK_ID))
      return error("Malformed block record");
    if (!mattributes.isEmpty())
      return error("Multiple PARAMATTR blocks found!");

    ArrayList<Long> record = new ArrayList<>();
    ArrayList<AttributeWithIndex> attrs = new ArrayList<>();

    // read all the records.
    while (true) {
      long code = readCode();
      if (code == END_BLOCK) {
        if (readBlockEnd())
          return error("Error at end of PARAMATTR block");
        return false;
      }
      if (code == ENTER_SUBBLOCK) {
        // if we found there is no sub-blocks, just skips thems.
        readSubBlockID();
        if (skipBlock())
          return error("Malformed block record");
        continue;
      }

      if (code == DEFINE_ABBREV) {
        readAbbrevRecord();
        continue;
      }

      // read a record.
      record.clear();
      switch (readRecord(code, record)) {
        default:
          // ignore it.
          break;
        case PARAMATTR_CODE_ENTRY: {
          // ENTRY: [paramidx0, attr0, ...]
          if ((record.size() & 0x1) != 0)
            return error("Invlaid ENTRY record");

          int retAttribute = Attribute.None;
          int fnAttribute = Attribute.None;
          for (int i = 0, e = record.size(); i < e; i+=2) {
            int alignment = (int) ((record.get(i+1) & (0xffffL << 16)) >> 16);
            if (alignment != 0 && Util.isPowerOf2(alignment)) {
              return error("Alignment is not a power of two!");
            }

            long reconstituedAttr = record.get(i+1) & 0xffff;
            if (alignment != 0)
              reconstituedAttr |= Attribute.constructAlignmentFromInt(alignment);
            reconstituedAttr |= (record.get(i+1) & (0xffffL << 32)) >> 11;
            record.set(i+1, reconstituedAttr);

            if (record.get(i) == 0)
              retAttribute |= record.get(i+1);
            else if (record.get(i) == ~0L)
              fnAttribute |= record.get(i+1);
          }

          int oldRetAttribute = Attribute.NoUnwind |
                                Attribute.NoReturn |
                                Attribute.ReadOnly |
                                Attribute.ReadNone;
          if (fnAttribute == Attribute.None &&
              retAttribute != Attribute.None &&
              (retAttribute & oldRetAttribute) != 0) {
            record.add(~0L);
            record.add(0L);

            fnAttribute |= retAttribute & oldRetAttribute;
            retAttribute &= ~oldRetAttribute;
          }

          for (int i = 0, e = record.size(); i < e; i += 2) {
            if (record.get(i) == 0) {
              if (retAttribute != Attribute.None)
                attrs.add(AttributeWithIndex.get(0, retAttribute));
            }
            else if (record.get(i) == ~0) {
              if (fnAttribute != Attribute.None)
                attrs.add(AttributeWithIndex.get(~0, fnAttribute));
            }
            else if (record.get(i+1) != Attribute.None) {
              attrs.add(AttributeWithIndex.get(record.get(i).intValue(), record.get(i+1).intValue()));;
            }

            mattributes.add(new AttrList(attrs));
            attrs.clear();
            break;
          }
        }
      }
    }
  }

  /**
   * Resolve all of the initialiers for global valeus and alias that we can.
   * @return
   */
  private boolean resolveGlobalAndAliasInits() {
    LinkedList<Pair<GlobalVariable, Integer>> globalInitWorklist = new LinkedList<>(globalInits);
    globalInits.clear();

    LinkedList<Pair<GlobalAlias, Integer>> aliasInitWorklist = new LinkedList<>(aliasInits);
    aliasInits.clear();

    while (!globalInitWorklist.isEmpty()) {
      int valID = globalInitWorklist.getLast().second;
      if (valID >= valueList.size()) {
        // not ready to resolve this yet, it requires something later in the file.
        globalInits.add(globalInitWorklist.getLast());
      }
      else {
        if (valueList.get(valID) instanceof Constant) {
          Constant c = (Constant) valueList.get(valID);
          globalInitWorklist.getLast().first.setInitializer(c);
        }
        else
          return error("Global variable initializer is not a constant!");
      }
      globalInitWorklist.removeLast();
    }

    while (!aliasInitWorklist.isEmpty()) {
      int valID = aliasInitWorklist.getLast().second;
      if (valID >= valueList.size())
        aliasInits.add(aliasInitWorklist.getLast());
      else {
        if (valueList.get(valID) instanceof Constant) {
          Constant c = (Constant) valueList.get(valID);
          aliasInitWorklist.getLast().first.setAliasee(c);
        }
        else
          return error("Alias initializer is not constant!");
      }
      aliasInitWorklist.removeLast();
    }
    return false;
  }

  /**
   * Having read the ENTER_SUBBLOCK abbrevid and a BlockID, skip
   * over the body of this block.  If the block record is malformed, return
   * true.
   * @return
   */
  private boolean skipBlock() {
    readVBR(CodeLenWidth);
    skipToWord();
    int numWords = (int) read(BlockSizeWidth);
    int skipTo = curOffset + numWords*4;
    if (atEndOfStream() || skipTo > buffer.length())
      return true;

    curOffset = skipTo;
    return false;
  }

  private void skipToWord() {
    curWord = 0;
    bitsInCurWord = 0;
  }

  private long readVBR(int numBits) {
    long piece = read(numBits);
    if ((piece & (1<<(numBits - 1))) == 0)
      return piece;

    long result = 0;
    int nextBit = 0;
    while (true) {
      result |= (piece & ((1 << (numBits - 1)) - 1)) << nextBit;
      if ((piece & (1<<(numBits - 1))) == 0)
        return result;

      nextBit += numBits - 1;
      piece = read(numBits);
    }
  }

  private void jumpToBit(long bitNo) {
    long byteNo = (bitNo/8) & ~3;
    long wordBitNo = bitNo & 31;
    Util.assertion(byteNo <= buffer.length(), "Invalid location");
    curOffset = (int) byteNo;
    bitsInCurWord = 0;
    curWord = 0;
    if (wordBitNo != 0)
      read((int) wordBitNo);
  }

  /**
   * Parse the function body block of the specified function.
   * @param f
   * @return
   */
  private boolean parseFunctionBody(Function f) {
    if (enterSubBlock(FUNCTION_BLOCK_ID))
      return error("Malformed block record");

    instructionList.clear();
    int moduleValueListSize = valueList.size();
    int moduleMDValueListSize = mdValueList.size();
    // add all function arguments to value table.
    for (Argument arg : f.getArgumentList())
      valueList.add(arg);

    int nextValueNo = valueList.size();
    BasicBlock curBB = null;
    int curBBNo = 0;
    DebugLoc lastLoc = new DebugLoc();

    // read all records.
    ArrayList<Long> record = new ArrayList<>();
    while (true) {
      long code = readCode();
      if (code == END_BLOCK) {
        if (readBlockEnd())
          return error("Error at end of function block");
        break;
      }
      if (code == ENTER_SUBBLOCK) {
        switch (readSubBlockID()) {
          default:
            if (skipBlock())
              return error("Malformed block record");
            break;
          case CONSTANTS_BLOCK_ID:
            if (parseConstants()) return true;
            nextValueNo = valueList.size();
            break;
          case VALUE_SYMTAB_BLOCK_ID:
            if (parseValueSymbolTable()) return true;
            break;
          case METADATA_ATTACHMENT_ID:
            if (parseMetadataAttachment()) return true;
            break;
          case METADATA_BLOCK_ID:
            if (parseMetadata()) return true;
            break;
        }
        continue;
      }

      if (code == DEFINE_ABBREV) {
        readAbbrevRecord();
        continue;
      }

      record.clear();
      Instruction inst = null;
      int bitcode = readRecord(code, record);
      switch (bitcode) {
        default:
          return error("Unknown instruction");
        case FunctionCodes.FUNC_CODE_DECLAREBLOCKS:
          // DECLAREBLOCKS: [nblocks]
          if (record.isEmpty() || record.get(0) == 0)
            return error("Malformed DECLAREBLOCKS record");
          if (functionBBs.size() < record.get(0))
            functionBBs.addAll(Collections.nCopies((int) (record.get(0) - functionBBs.size()), null));
          else {
            while (functionBBs.size() > record.get(0))
              functionBBs.remove(functionBBs.size() - 1);
          }
          for (int i = 0, e = functionBBs.size(); i < e; ++i)
            functionBBs.set(i, BasicBlock.createBasicBlock(context, "", f));
          curBB = functionBBs.get(0);
          continue;
        case FunctionCodes.FUNC_CODE_DEBUG_LOC_AGAIN:
          // This record indicates that the last instruction is at the same
          // location as the previous instruction with a location.
          inst = null;
          if (curBB != null && !curBB.isEmpty())
            inst = curBB.getLastInst();
          else if (curBBNo != 0 && functionBBs.get(curBBNo-1) != null &&
                  !functionBBs.get(curBBNo-1).isEmpty())
            inst = functionBBs.get(curBBNo-1).getLastInst();

          if (inst == null)
            return error("Invalid DEBUG_LOC_AGAIN record");
          inst.setDebugLoc(lastLoc);
          inst = null;
          continue;
          case FunctionCodes.FUNC_CODE_DEBUG_LOC: {
            // DEBUG_LOC: [line, col, scope, ia]
            inst = null;
            if (curBB != null && !curBB.isEmpty())
              inst = curBB.getLastInst();
            else if (curBBNo != 0 && functionBBs.get(curBBNo-1) != null &&
                    !functionBBs.get(curBBNo-1).isEmpty())
              inst = functionBBs.get(curBBNo-1).getLastInst();

            if (inst == null || record.size() < 4)
              return error("Invalid DEBUG_LOC record");
            int line = Math.toIntExact(record.get(0)), col = Math.toIntExact(record.get(1));
            int scopeID = Math.toIntExact(record.get(2)), iaid = Math.toIntExact(record.get(3));
            MDNode scope = null, ia = null;
            if (scopeID != 0) scope = (MDNode) mdValueList.getValueFwdRef(scopeID-1);
            if (iaid != 0) ia = (MDNode) mdValueList.getValueFwdRef(iaid - 1);
            lastLoc = DebugLoc.get(line, col, scope, ia);
            inst.setDebugLoc(lastLoc);
            inst = null;
            continue;
          }
        case FunctionCodes.FUNC_CODE_INST_BINOP: {
          // BINOP: [opval, ty, opval, opcode]
          OutRef<Integer> opNum = new OutRef<>(0);
          Value lhs = getValueTypePair(record, opNum, nextValueNo);
          Value rhs = null;
          if (lhs == null || (rhs = getValue(record, opNum, lhs.getType())) == null ||
                  opNum.get() + 1 > record.size()) {
            return error("Invalid BINOP record");
          }

          Operator opc = getDecodedBinaryOpcode(record.get(opNum.get()).intValue(), lhs.getType());
          opNum.set(opNum.get()+1);
          if (opc == Operator.None) return error("Invalid BINOP record");
          inst = Instruction.BinaryOperator.create(opc, lhs, rhs, "");
          instructionList.add(inst);
          if (opNum.get() < record.size()) {
            switch (opc) {
              case Add:
              case Sub:
              case Mul:
              case Shl: {
                int value = Math.toIntExact(record.get(opNum.get()));
                if ((value & (1 << OverflowingBinaryOperatorOptionalFlags.OBO_NO_SIGNED_WRAP)) != 0)
                  ((OverflowingBinaryOperator)inst).setHasNoSignedWrap(true);
                if ((value & (1 << OverflowingBinaryOperatorOptionalFlags.OBO_NO_UNSIGNED_WRAP)) != 0)
                  ((OverflowingBinaryOperator)inst).setHasNoUnsignedWrap(true);
                break;
              }
              case SDiv:
              case UDiv:
              case LShr:
              case AShr:
                if ((record.get(opNum.get()) & (1 << PossiblyExactOperatorOptionalFlags.PEO_EXACT)) != 0)
                  ((ExactBinaryOperator)inst).setIsExact(true);
                break;
            }
          }
          break;
        }
        case FunctionCodes.FUNC_CODE_INST_CAST: {
          // CAST: [opval, opty, destty, castopc]
          OutRef<Integer> opNum = new OutRef<>(0);
          Value op;
          if ((op = getValueTypePair(record, opNum, nextValueNo)) == null ||
                  opNum.get() + 2 != record.size())
            return error("Invalid CAST record");
          Type resTy = getTypeByID(Math.toIntExact(record.get(opNum.get())));
          Operator opc = getDecodedCastOpcode(Math.toIntExact(record.get(opNum.get() + 1)));
          if (opc == Operator.None || resTy == null)
            return error("Invalid CAST record");
          inst = Instruction.CastInst.create(opc, op, resTy, "", (Instruction) null);
          instructionList.add(inst);
          break;
        }
        case FunctionCodes.FUNC_CODE_INST_INBOUNDS_GEP:
        case FunctionCodes.FUNC_CODE_INST_GEP: {
          // GEP: [n x operands]
          Value basePtr;
          OutRef<Integer> opNum = new OutRef<>(0);
          if ((basePtr = getValueTypePair(record, opNum, nextValueNo)) == null)
            return error("Invalid GEP record");

          ArrayList<Value> gepIdx = new ArrayList<>();
          while (opNum.get() < record.size()) {
            Value op = null;
            if ((op = getValueTypePair(record, opNum, nextValueNo)) == null)
              return error("Invalid GEP record");
            gepIdx.add(op);
          }
          inst = new GetElementPtrInst(basePtr, gepIdx, "");
          instructionList.add(inst);
          if (bitcode == FunctionCodes.FUNC_CODE_INST_INBOUNDS_GEP)
            ((GetElementPtrInst)inst).setIsInBounds(true);
          break;
        }
        case FunctionCodes.FUNC_CODE_INST_EXTRACTVAL: {
          // EXTRACTVAL: [opty, opval, n x indices]
          OutRef<Integer> opNum = new OutRef<>(0);
          Value agg;
          if ((agg = getValueTypePair(record, opNum, nextValueNo)) == null)
            return error("Invalid EXTRACTVAL record");
          int[] idx = new int[record.size() - opNum.get()];
          for (int i = opNum.get(), recSize = record.size(); i < recSize; ++i) {
            long index = record.get(i);
            if (index != (int)index)
              return error("Invalid EXTRACTVAL index");
            idx[i - opNum.get()] = (int) index;
          }
          inst = new Instruction.ExtractValueInst(agg, idx);
          instructionList.add(inst);
          break;
        }
        case FunctionCodes.FUNC_CODE_INST_INSERTVAL: {
          // INSERTVAL: [opty, opval, opty, opval, n x indices]
          OutRef<Integer> opNum = new OutRef<>(0);
          Value agg, val;
          if ((agg = getValueTypePair(record, opNum, nextValueNo)) == null ||
                  (val = getValueTypePair(record, opNum, nextValueNo)) == null)
            return error("Invalid INSERTVAL record");
          int[] idx = new int[record.size() - opNum.get()];
          for (int i = opNum.get(), recSize = record.size(); i < recSize; ++i) {
            long index = record.get(i);
            if (index != (int)index)
              return error("Invalid INSERTVAL index");
            idx[i - opNum.get()] = (int) index;
          }
          inst = new Instruction.InsertValueInst(agg, val, idx);
          instructionList.add(inst);
          break;
        }
        case FunctionCodes.FUNC_CODE_INST_SELECT: {
          // SELECT: [opval, ty, opval, opval]
          OutRef<Integer> opNum = new OutRef<>(0);
          Value cond, trueVal, falseVal;
          if ((trueVal = getValueTypePair(record, opNum, nextValueNo)) == null ||
                  (falseVal = getValue(record, opNum, trueVal.getType())) == null ||
                  (cond = getValue(record, opNum, Type.getInt1Ty(context))) == null)
            return error("Invalid SELECT record");

          inst = new SelectInst(cond, trueVal, falseVal, "");
          instructionList.add(inst);
          break;
        }
        case FunctionCodes.FUNC_CODE_INST_VSELECT: {
          OutRef<Integer> opNum = new OutRef<>(0);
          Value cond, trueVal, falseVal;
          if ((trueVal = getValueTypePair(record, opNum, nextValueNo)) == null ||
                  (falseVal = getValue(record, opNum, trueVal.getType())) == null ||
                  (cond = getValueTypePair(record, opNum, nextValueNo)) == null)
            return error("Invalid SELECT record");

          if (cond.getType() instanceof VectorType) {
            VectorType vty = (VectorType) cond.getType();
            if (!vty.getElementType().isIntegerTy(1))
              return error("Invalid SELECT condition type");
          } else {
            // expect i1
            if (!cond.getType().isIntegerTy(1))
              return error("Invalid SELECT condition type");
          }

          inst = new SelectInst(cond, trueVal, falseVal, "");
          instructionList.add(inst);
          break;
        }
        case FunctionCodes.FUNC_CODE_INST_EXTRACTELT: {
          // EXTRACTELT: [opty, opval, opval]
          OutRef<Integer> opNum = new OutRef<>(0);
          Value agg, val;
          if ((agg = getValueTypePair(record, opNum, nextValueNo)) == null ||
               (val = getValue(record, opNum, Type.getInt32Ty(context))) == null)
            return error("Invalid EXTRACTELT record");
          inst = new Instruction.ExtractElementInst(agg, val);
          instructionList.add(inst);
          break;
        }
        case FunctionCodes.FUNC_CODE_INST_INSERTELT: {
          // INSERTELT: [ty, opval,opval,opval]
          OutRef<Integer> opNum = new OutRef<>(0);
          Value vec, elt, idx;
          if ((vec = getValueTypePair(record, opNum, nextValueNo)) == null ||
               (elt = getValue(record, opNum, ((VectorType)vec.getType()).getElementType())) == null ||
               (idx = getValue(record, opNum, Type.getInt32Ty(context))) == null)
            return error("Invalid INSERTELT record");
          inst = new Instruction.InsertElementInst(vec, elt, idx);
          instructionList.add(inst);
          break;
        }
        case FunctionCodes.FUNC_CODE_INST_SHUFFLEVEC: {
          OutRef<Integer> opNum = new OutRef<>(0);
          Value vec1, vec2, mask;
          if ((vec1 = getValueTypePair(record, opNum, nextValueNo)) == null ||
              (vec2 = getValue(record, opNum, vec1.getType())) == null ||
              (mask = getValueTypePair(record, opNum, nextValueNo)) == null)
            return error("Invalid SHUFFLEVEC record");
          inst = new Instruction.ShuffleVectorInst(vec1, vec2, mask);
          instructionList.add(inst);
          break;
        }
        case FunctionCodes.FUNC_CODE_INST_CMP:
        case FunctionCodes.FUNC_CODE_INST_CMP2: {
          OutRef<Integer> opNum = new OutRef<>(0);
          Value lhs, rhs;
          if ((lhs = getValueTypePair(record, opNum, nextValueNo)) == null ||
                  (rhs = getValue(record, opNum, lhs.getType())) == null)
            return error("Invalid CMP record");
          int pred = record.get(opNum.get()).intValue();
          if (lhs.getType().isFPOrFPVectorTy())
            inst = new Instruction.FCmpInst(Instruction.CmpInst.Predicate.getPred(pred), lhs, rhs);
          else
            inst = new Instruction.ICmpInst(Instruction.CmpInst.Predicate.getPred(pred), lhs, rhs);
          instructionList.add(inst);
          break;
        }

        case FunctionCodes.FUNC_CODE_INST_RET: {
          // RET: [opty, opval<optional>]
          int size = record.size();
          if (size == 0) {
            inst = new Instruction.ReturnInst(context);
            instructionList.add(inst);
            break;
          }

          OutRef<Integer> opNum = new OutRef<>(0);
          Value op = null;
          if ((op = getValueTypePair(record, opNum, nextValueNo)) == null ||
                  opNum.get() != record.size())
            return error("Invalid RET record");
          inst = new Instruction.ReturnInst(context, op);
          instructionList.add(inst);
          break;
        }
        case FunctionCodes.FUNC_CODE_INST_BR: {
          // BR: [bb#, bb#, opval] or [bb#]
          if (record.size() != 1 && record.size() != 3)
            return error("Invalid BR record");
          BasicBlock trueDest = getBasicBlock(record.get(0).intValue());
          if (trueDest == null) return error("Invalid true destination of BR record");
          if (record.size() == 1) {
            inst = new Instruction.BranchInst(trueDest);
            instructionList.add(inst);
          } else {
            BasicBlock falseDest = getBasicBlock(record.get(1).intValue());
            if (falseDest == null) return error("Invalid false destination of BR record");
            Value cond = getFnValueByID(record.get(2).intValue(), Type.getInt1Ty(context));
            if (cond == null)
              return error("Invalid condition of BR record");
            inst = new Instruction.BranchInst(trueDest, falseDest, cond);
            instructionList.add(inst);
          }
          break;
        }
        case FunctionCodes.FUNC_CODE_INST_SWITCH: {
          // SWITCH: [opty, op0, op1, ...]
          if (record.size() < 3 || (record.size() & 1) == 0)
            return error("Invalid SWITCH record");
          Type opTy = getTypeByID(record.get(0).intValue());
          Value cond = getFnValueByID(record.get(1).intValue(), opTy);
          BasicBlock defaultBB = getBasicBlock(record.get(2).intValue());
          if (opTy == null || cond == null || defaultBB == null)
            return error("Invalid SWITCH record");
          int numCases = (record.size() - 3) /2;
          Instruction.SwitchInst si = new Instruction.SwitchInst(cond, defaultBB, numCases, "");
          instructionList.add(si);
          for (int i = 0; i < numCases; ++i) {
            Value v = getFnValueByID(record.get(i*2+3).intValue(), opTy);
            ConstantInt caseVal = v instanceof ConstantInt ? (ConstantInt) v : null;
            BasicBlock destBB = getBasicBlock(record.get(1+3+i*2).hashCode());
            if (caseVal == null || destBB == null)
              return error("Invalid SWITCH record");
            si.addCase(caseVal, destBB);
          }
          inst = si;
          break;
        }
        case FunctionCodes.FUNC_CODE_INST_INDIRECTBR: {
          // INDIRECTBR: [opty, op0, op1, ...]
          if (record.size() < 2)
            return error("Invalid INDIRECTBR record");
          Type opTy = getTypeByID(record.get(0).intValue());
          Value address = getFnValueByID(record.get(1).intValue(), opTy);
          if (opTy == null || address == null)
            return error("Invalid INDIRECTBR record");

          int numDests = record.size() - 2;
          IndirectBrInst ir = IndirectBrInst.create(address, numDests, (Instruction) null);
          instructionList.add(ir);
          for (int i = 0; i < numDests; ++i) {
            BasicBlock destBB = getBasicBlock(record.get(2+i).intValue());
            if (destBB == null)
              return error("Invalid INDIRECTBR record");
            ir.addDestination(destBB);
          }
          break;
        }
        case FunctionCodes.FUNC_CODE_INST_INVOKE: {
          // INVOKE: [attrs, cc, normBB, unwindBB, fnty, op0,op1,op2, ...]
          if (record.size() < 4)
            return error("Invalid INVOKE record");

          AttrList attr = getAttributes(record.get(0).intValue());
          int cc = record.get(1).intValue();
          BasicBlock normalBB = getBasicBlock(record.get(2).intValue());
          BasicBlock unwindBB = getBasicBlock(record.get(3).intValue());
          OutRef<Integer> opNum = new OutRef<>(4);
          Value callee;
          if ((callee = getValueTypePair(record, opNum, nextValueNo)) == null)
            return error("Invalid INVOKE record");

          PointerType calleeTy = callee.getType() instanceof PointerType ? (PointerType)callee.getType() : null;
          FunctionType fty = calleeTy == null ? null : (FunctionType) calleeTy.getElementType();
          if (fty == null || normalBB == null || unwindBB == null)
            return error("Invalid INVOKE record");
          ArrayList<Value> ops = new ArrayList<>();
          int opIdx = opNum.get();
          for (int i = 0, e = fty.getNumParams(); i < e; ++i, ++opIdx) {
            Value arg = getFnValueByID(record.get(opIdx).intValue(), fty.getParamType(i));
            if (arg == null)
              return error("Invalid INVOKE record");
            ops.add(arg);
          }
          if (!fty.isVarArg()) {
            if (record.size() != opIdx)
              return error("Invalid INVOKE record");
          } else {
            while (opIdx != record.size()) {
              Value op;
              opNum.set(opIdx);
              if ((op = getValueTypePair(record, opNum, nextValueNo)) == null)
                return error("Invalid INVOKE record");
              opIdx = opNum.get();
              ops.add(op);
            }
          }
          inst = Instruction.InvokeInst.create(callee, normalBB, unwindBB, ops);
          instructionList.add(inst);
          ((Instruction.InvokeInst)inst).setCallingConv(CallingConv.values()[cc]);
          ((Instruction.InvokeInst)inst).setAttributes(attr);
          break;
        }
        case FunctionCodes.FUNC_CODE_INST_RESUME: {
          // RESUME: [opval]
          OutRef<Integer> idx = new OutRef<>(0);
          Value val;
          if ((val = getValueTypePair(record, idx, nextValueNo)) == null)
            return error("Invalid RESUME record");
          inst = Instruction.ResumeInst.create(val);
          instructionList.add(inst);
          break;
        }
        case FunctionCodes.FUNC_CODE_INST_UNWIND:
          // UNWIND:
          inst = new Instruction.UnWindInst(context);
          instructionList.add(inst);
          break;
        case FunctionCodes.FUNC_CODE_INST_UNREACHABLE:
          inst = new UnreachableInst(context);
          instructionList.add(inst);
          break;
        case FunctionCodes.FUNC_CODE_INST_PHI: {
          // PHI: [ty, val0, bb0, ...]
          if (record.isEmpty() || ((record.size() - 1)&1) != 0)
            return error("Invalid PHI record");
          Type ty = getTypeByID(record.get(0).intValue());
          if (ty == null) return error("Invalid PHI record");
          Instruction.PhiNode pn = new Instruction.PhiNode(ty, (record.size()-1)/2, "");
          instructionList.add(pn);
          for (int i = 1, e = record.size(); i < e; i += 2) {
            Value v = getFnValueByID(record.get(i).intValue(), ty);
            BasicBlock bb = getBasicBlock(record.get(i+1).intValue());
            if (v == null || bb == null)
              return error("Invalid PHI record");
            pn.addIncoming(v, bb);
          }
          inst = pn;
          break;
        }
        case FunctionCodes.FUNC_CODE_INST_LANDINGPAD: {
          // LANDINGPAD: [ty, val, val, num, (id0,val0 ...)?]
          if (record.size() < 4)
            return error("Invalid LANDINGPAD record");
          Type ty = getTypeByID(record.get(0).intValue());
          if (ty == null) return error("Invalid LANDINGPAD record");
          Value persFn = null;
          OutRef<Integer> idx = new OutRef<>(0);
          if ((persFn = getValueTypePair(record, idx, nextValueNo)) == null)
            return error("Invalid LANDINGPAD record");
          boolean isCleanup = record.get(idx.get()) == 0;
          idx.set(idx.get()+1);
          int numClauses = record.get(idx.get()).intValue();
          idx.set(idx.get()+1);
          LandingPadInst lp = LandingPadInst.create(ty, persFn, numClauses);
          lp.setCleanup(isCleanup);
          for (int j = 0; j < numClauses; ++j) {
            LandingPadInst.ClauseType ct = LandingPadInst.ClauseType.values()[record.get(idx.get()).intValue()];;
            Value val;
            if ((val = getValueTypePair(record, idx, nextValueNo)) == null)
              return error("Invalid LANDINGPAD record");
            Util.assertion(ct != LandingPadInst.ClauseType.Catch ||
                    !(val.getType() instanceof ArrayType), "Catch clause has an invalid type");
            Util.assertion(ct != LandingPadInst.ClauseType.Filter ||
                    val.getType() instanceof ArrayType, "Filter clause has an invalid type");
            lp.addClause(val);
          }
          instructionList.add(lp);
          break;
        }
        case FunctionCodes.FUNC_CODE_INST_ALLOCA: {
          // ALLOCA: [instty, opty, op, align]
          if (record.size() < 4)
            return error("Invalid ALLOCA record");

          Type ty = getTypeByID(Math.toIntExact(record.get(0)));
          PointerType pty = ty instanceof PointerType ? (PointerType)ty : null;
          Type opTy = getTypeByID(Math.toIntExact(record.get(1)));
          Value size = getFnValueByID(record.get(2).intValue(), opTy);
          if (pty == null || size == null) return error("Invalid ALLOCA record");
          int align = record.get(3).intValue();
          inst = new Instruction.AllocaInst(pty.getElementType(), size, (1<<align) >>> 1);
          instructionList.add(inst);
          break;
        }
        case FunctionCodes.FUNC_CODE_INST_LOAD: {
          // LOAD: [opty, op, align, volatile]
          OutRef<Integer> opNum = new OutRef<>(0);
          Value op;
          if ((op = getValueTypePair(record, opNum, nextValueNo)) == null ||
                  opNum.get() + 2 != record.size())
            return error("Invalid LOAD record");

          inst = new Instruction.LoadInst(op, "", record.get(opNum.get()+1).intValue() != 0,
                  (1<<record.get(opNum.get()).intValue()) >>> 1);
          instructionList.add(inst);
          break;
        }
        case FunctionCodes.FUNC_CODE_INST_LOADATOMIC: {
          Util.shouldNotReachHere("loadatomic is not supported yet!");
        }
        case FunctionCodes.FUNC_CODE_INST_STORE: {
          // STORE2:[ptrty, ptr, val, align, volatile]
          OutRef<Integer> opNum = new OutRef<>(0);
          Value val, ptr;
          if ((ptr = getValueTypePair(record, opNum, nextValueNo)) == null ||
                  (val = getValue(record, opNum, ((PointerType)ptr.getType()).getElementType())) == null ||
                  opNum.get() + 2 != record.size())
            return error("Invalid STORE record");

          int align = record.get(opNum.get()).intValue();
          boolean isVol = record.get(opNum.get()+1) != 0;
          inst = new Instruction.StoreInst(val, ptr, isVol , (1<<align) >>> 1);
          instructionList.add(inst);
          break;
        }
        case FunctionCodes.FUNC_CODE_INST_STOREATOMIC:
        case FunctionCodes.FUNC_CODE_INST_CMPXCHG:
        case FunctionCodes.FUNC_CODE_INST_ATOMICRMW:
        case FunctionCodes.FUNC_CODE_INST_FENCE:
          Util.shouldNotReachHere("atomic instructions are not supported yet!");

        case FunctionCodes.FUNC_CODE_INST_CALL: {
          // CALL: [paramattrs, cc, fnty, fnid, arg0, arg1...]
          if (record.size() < 3)
            return error("Invalid CALL record");

          AttrList attr = getAttributes(record.get(0).intValue());
          int cc = record.get(1).intValue();
          OutRef<Integer> opNum = new OutRef<>(2);
          Value callee;
          if ((callee = getValueTypePair(record, opNum, nextValueNo)) == null)
            return error("Invalid CALL record");

          PointerType calleeTy = callee.getType() instanceof PointerType ? (PointerType)callee.getType() : null;
          FunctionType fty = calleeTy == null ? null : (FunctionType) calleeTy.getElementType();
          if (fty == null || record.size() < fty.getNumParams() + opNum.get())
            return error("Invalid CALL record");

          ArrayList<Value> ops = new ArrayList<>();
          int opIdx = opNum.get();
          for (int i = 0, e = fty.getNumParams(); i < e; ++i, ++opIdx) {
            Value arg;
            if (fty.getParamType(i).isLabelTy())
              arg = getBasicBlock(record.get(opIdx).intValue());
            else
              arg = getFnValueByID(record.get(opIdx).intValue(), fty.getParamType(i));

            if (arg == null)
              return error("Invalid CALL record");
            ops.add(arg);
          }
          if (!fty.isVarArg()) {
            if (record.size() != opIdx)
              return error("Invalid CALL record");
          } else {
            while (opIdx != record.size()) {
              Value op;
              opNum.set(opIdx);
              if ((op = getValueTypePair(record, opNum, nextValueNo)) == null)
                return error("Invalid CALL record");
              opIdx = opNum.get();
              ops.add(op);
            }
          }
          inst = CallInst.create(callee, ops, "", (Instruction) null);
          instructionList.add(inst);
          ((CallInst)inst).setCallingConv(CallingConv.getCallingConv(cc>>>1));
          ((CallInst)inst).setAttributes(attr);
          ((CallInst)inst).setTailCall((cc&1) != 0);
          break;
        }
        case FunctionCodes.FUNC_CODE_INST_VAARG: {
          // VAARG: [valistty, valist, instty]
          if (record.size() < 3)
            return error("Invalid VAARG record");
          Type opTy = getTypeByID(record.get(0).intValue());
          Value op = getFnValueByID(record.get(1).intValue(), opTy);
          Type resTy = getTypeByID(record.get(2).intValue());
          if (opTy == null || op == null || resTy == null)
            return error("Invalid VAARG record");
          inst = new Instruction.VAArgInst(op, resTy);
          instructionList.add(inst);
          break;
        }
      }

      // add instruction to the end of current block.
      if (curBB == null) {
        return error("Invalid instruction with no BB");
      }

      curBB.appendInst(inst);
      if (inst instanceof Instruction.TerminatorInst) {
        ++curBBNo;
        curBB = curBBNo < functionBBs.size() ? functionBBs.get(curBBNo) : null;
      }

      if (inst != null && !inst.getType().isVoidType())
        valueList.assignValue(inst, nextValueNo++);
    }

    Value val = valueList.get(valueList.size() - 1);
    Argument arg = val instanceof Argument ? (Argument)val : null;
    if (arg != null && arg.getParent() == null) {
      for (int i = moduleValueListSize, e = valueList.size(); i < e; ++i) {
        arg = (Argument) valueList.get(i);
        if (arg != null && arg.getParent() == null)
          arg.replaceAllUsesWith(Value.UndefValue.get(arg.getType()));
      }
    }

    // See if there are anything take the address of blocks in this function. If so,
    // resolve them now.
    if (blockAddrFwdRefs.containsKey(f)) {
      ArrayList<Pair<Long, GlobalVariable>> refList = blockAddrFwdRefs.get(f);
      for (Pair<Long, GlobalVariable> pair : refList) {
        int blockIdx = pair.first.intValue();
        if (blockIdx >= functionBBs.size())
          return error(String.format("Invalid blockaddress blcok #%d", blockIdx));
        GlobalVariable fwdRef = pair.second;
        fwdRef.replaceAllUsesWith(BlockAddress.get(f, functionBBs.get(blockIdx)));
        fwdRef.eraseFromParent();
      }
      blockAddrFwdRefs.remove(f);
    }

    // trim the value list down to the size it was before we parsed this function.
    valueList.shrinkTo(moduleValueListSize);
    mdValueList.shrinkTo(moduleMDValueListSize);
    functionBBs.clear();
    return false;
  }

  private Value getValueTypePair(ArrayList<Long> record, OutRef<Integer> slot, int instNum) {
    if (slot.get() >= record.size()) return null;
    int valNo = Math.toIntExact(record.get(slot.get()));
    slot.set(slot.get()+1);
    if (valNo < instNum) {
      return getFnValueByID(valNo, null);
    }
    else if (slot.get() >= record.size())
      return null;

    int typeNo = Math.toIntExact(record.get(slot.get()));
    slot.set(slot.get()+1);
    return getFnValueByID(valNo, getTypeByID(typeNo));
  }

  private Value getValue(ArrayList<Long> record, OutRef<Integer> slot, Type ty) {
    if (slot.get() >= record.size()) return null;
    int valNo = Math.toIntExact(record.get(slot.get()));
    slot.set(slot.get() + 1);
    return getFnValueByID(valNo, ty);
  }

  private Value getFnValueByID(int id, Type ty) {
    if (ty != null && ty.isMetadataTy())
      return mdValueList.getValueFwdRef(id);
    return valueList.getValueFwdRef(id, ty);
  }

  /**
   * This is entry point for lazyly reading a bitcode from at a time.
   *
   * @param buffer
   * @param errMsg
   * @return
   */
  public static Module getLazyBitcodeModule(MemoryBuffer buffer,
                                            OutRef<String> errMsg,
                                            LLVMContext ctx) {
    Module m = new Module(buffer.getBufferIdentifier(), ctx);
    BitcodeReader reader = new BitcodeReader(buffer, ctx);
    if (reader.parseBitcodeInfo(m)) {
      if (errMsg != null)
        errMsg.set(reader.getErrorString());

      return null;
    }
    if (reader.materializeModule(m, errMsg))
      return null;
    return m;
  }

  /**
   * Read the specified bitcode file, returning the module. If an error occurs, just
   * return null and fill the {@code errorMsg} if it is not a null.
   *
   * @param buffer
   * @param errorMsg
   * @return
   */
  public static Module parseBitcodeFile(MemoryBuffer buffer,
                                        OutRef<String> errorMsg,
                                        LLVMContext ctx) {
    return getLazyBitcodeModule(buffer, errorMsg, ctx);
  }
}
