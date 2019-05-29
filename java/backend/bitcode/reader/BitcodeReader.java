package backend.bitcode.reader;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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

import backend.io.BitStream;
import backend.io.ByteSequence;
import backend.support.AttrList;
import backend.support.CallingConv;
import backend.support.GVMaterializer;
import backend.support.LLVMContext;
import backend.type.*;
import backend.value.*;
import cfe.support.MemoryBuffer;
import tools.*;

import java.util.*;

import static backend.bitcode.BitcodeUtil.*;
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
import static backend.support.AutoUpgrade.upgradeGlobalVariable;
import static backend.support.AutoUpgrade.upgradeIntrinsicFunction;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class BitcodeReader implements GVMaterializer {
  private Module theModule;

  @Override
  public boolean isMaterializable(GlobalValue gv) {
    return false;
  }

  @Override
  public boolean isDematerializable(GlobalValue gv) {
    return false;
  }

  @Override
  public boolean materialize(GlobalValue gv, OutRef<String> errInfo) {
    return false;
  }

  @Override
  public void dematerialize(GlobalValue gv) {

  }

  @Override
  public boolean materializeModule(Module m, OutRef<String> errInfo) {
    return false;
  }

  interface StandardWidths {
    int BlockIDWidth = 8,  // We use VBR-8 for block IDs.
        CodeLenWidth = 4,  // Codelen are VBR-4.
        BlockSizeWidth = 32;  // BlockSize up to 2^32 32-bit words = 16GB per block.
  }

  // The standard abbrev namespace always has a way to exit a block, enter a
  // nested block, define abbrevs, and define an unabbreviated record.
  interface FixedAbbrevIDs {
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
  interface StandardBlockIDs {
    /// BLOCKINFO_BLOCK is used to define metadata about blocks, for example,
    /// standard abbrevs that should be available to all blocks of a specified
    /// ID.
    int BLOCKINFO_BLOCK_ID = 0,

    // Block IDs 1-7 are reserved for future expansion.
    FIRST_APPLICATION_BLOCKID = 8;
  }

  /// BlockInfoCodes - The blockinfo block contains metadata about user-defined
  /// blocks.
  interface BlockInfoCodes {
    // DEFINE_ABBREV has magic semantics here, applying to the current SETBID'd
    // block, instead of the BlockInfo block.
    int BLOCKINFO_CODE_SETBID = 1,       // SETBID: [blockid#]
    BLOCKINFO_CODE_BLOCKNAME = 2,    // BLOCKNAME: [name]
    BLOCKINFO_CODE_SETRECORDNAME = 3; // BLOCKINFO_CODE_SETRECORDNAME: [id, name]
  }

  // The only top-level block type defined is for a module.
  interface BlockIDs {
    // Blocks
    int MODULE_BLOCK_ID = FIRST_APPLICATION_BLOCKID,

    // Module sub-block id's.
    PARAMATTR_BLOCK_ID = MODULE_BLOCK_ID + 1,
        TYPE_BLOCK_ID = PARAMATTR_BLOCK_ID + 1,
        CONSTANTS_BLOCK_ID = TYPE_BLOCK_ID + 1,
        FUNCTION_BLOCK_ID = CONSTANTS_BLOCK_ID + 1,
        TYPE_SYMTAB_BLOCK_ID = FUNCTION_BLOCK_ID + 1,
        VALUE_SYMTAB_BLOCK_ID = TYPE_SYMTAB_BLOCK_ID + 1,
        METADATA_BLOCK_ID = VALUE_SYMTAB_BLOCK_ID + 1,
        METADATA_ATTACHMENT_ID = METADATA_BLOCK_ID + 1;
  }


  /// MODULE blocks have a number of optional fields and subblocks.
  interface ModuleCodes {
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
  interface AttributeCodes {
    int PARAMATTR_CODE_ENTRY = 1;   // ENTRY: [paramidx0, attr0, paramidx1, attr1...]
  }

  /// TYPE blocks have codes for each type primitive they use.
  interface TypeCodes {
    int TYPE_CODE_NUMENTRY = 1,   // NUMENTRY: [numentries]

    // Type Codes
    TYPE_CODE_VOID = 2,   // VOID
        TYPE_CODE_FLOAT = 3,   // FLOAT
        TYPE_CODE_DOUBLE = 4,   // DOUBLE
        TYPE_CODE_LABEL = 5,   // LABEL
        TYPE_CODE_OPAQUE = 6,   // OPAQUE
        TYPE_CODE_INTEGER = 7,   // INTEGER: [width]
        TYPE_CODE_POINTER = 8,   // POINTER: [pointee type]
        TYPE_CODE_FUNCTION = 9,   // FUNCTION: [vararg, retty, paramty x N]
        TYPE_CODE_STRUCT = 10,   // STRUCT: [ispacked, eltty x N]
        TYPE_CODE_ARRAY = 11,   // ARRAY: [numelts, eltty]
        TYPE_CODE_VECTOR = 12,   // VECTOR: [numelts, eltty]

    // These are not with the other floating point types because they're
    // a late addition, and putting them in the right place breaks
    // binary compatibility.
    TYPE_CODE_X86_FP80 = 13,   // X86 LONG DOUBLE
        TYPE_CODE_FP128 = 14,   // LONG DOUBLE (112 bit mantissa)
        TYPE_CODE_PPC_FP128 = 15,   // PPC LONG DOUBLE (2 doubles)

    TYPE_CODE_METADATA = 16;    // METADATA
  }

  // The type symbol table only has one code (TST_ENTRY_CODE).
  interface TypeSymtabCodes {
    int TST_CODE_ENTRY = 1;     // TST_ENTRY: [typeid, namechar x N]
  }

  // The value symbol table only has one code (VST_ENTRY_CODE).
  interface ValueSymtabCodes {
    int VST_CODE_ENTRY = 1,  // VST_ENTRY: [valid, namechar x N]
        VST_CODE_BBENTRY = 2;   // VST_BBENTRY: [bbid, namechar x N]
  }

  interface MetadataCodes {
    int METADATA_STRING = 1,   // MDSTRING:      [values]
    // FIXME: Remove NODE in favor of NODE2 in LLVM 3.0
    METADATA_NODE = 2,   // NODE with potentially invalid metadata
    // FIXME: Remove FN_NODE in favor of FN_NODE2 in LLVM 3.0
    METADATA_FN_NODE = 3,   // FN_NODE with potentially invalid metadata
        METADATA_NAME = 4,   // STRING:        [values]
    // FIXME: Remove NAMED_NODE in favor of NAMED_NODE2 in LLVM 3.0
    METADATA_NAMED_NODE = 5,   // NAMED_NODE with potentially invalid metadata
        METADATA_KIND = 6,   // [n x [id, name]]
    // FIXME: Remove ATTACHMENT in favor of ATTACHMENT2 in LLVM 3.0
    METADATA_ATTACHMENT = 7,   // ATTACHMENT with potentially invalid metadata
        METADATA_NODE2 = 8,   // NODE2:         [n x (type num, value num)]
        METADATA_FN_NODE2 = 9,   // FN_NODE2:      [n x (type num, value num)]
        METADATA_NAMED_NODE2 = 10,  // NAMED_NODE2:   [n x mdnodes]
        METADATA_ATTACHMENT2 = 11;   // [m x [value, [n x [id, mdnode]]]
  }

  // The constants block (CONSTANTS_BLOCK_ID) describes emission for each
  // constant and maintains an implicit current type value.
  interface ConstantsCodes {
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
  interface CastOpcodes {
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
  interface BinaryOpcodes {
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
  interface OverflowingBinaryOperatorOptionalFlags {
    int OBO_NO_UNSIGNED_WRAP = 0,
        OBO_NO_SIGNED_WRAP = 1;
  }

  /// SDivOperatorOptionalFlags - Flags for serializing SDivOperator's
  /// SubclassOptionalData contents.
  interface SDivOperatorOptionalFlags {
    int SDIV_EXACT = 0;
  }

  // The function body block (FUNCTION_BLOCK_ID) describes function bodies.  It
  // can contain a constant block (CONSTANTS_BLOCK_ID).
  interface FunctionCodes {
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
        FUNC_CODE_INST_MALLOC = 17, // MALLOC:     [instty, op, align]
        FUNC_CODE_INST_FREE = 18, // FREE:       [opty, op]
        FUNC_CODE_INST_ALLOCA = 19, // ALLOCA:     [instty, op, align]
        FUNC_CODE_INST_LOAD = 20, // LOAD:       [opty, op, align, vol]
    // FIXME: Remove STORE in favor of STORE2 in LLVM 3.0
    FUNC_CODE_INST_STORE = 21, // STORE:      [valty,val,ptr, align, vol]
    // FIXME: Remove CALL in favor of CALL2 in LLVM 3.0
    FUNC_CODE_INST_CALL = 22, // CALL with potentially invalid metadata
        FUNC_CODE_INST_VAARG = 23, // VAARG:      [valistty, valist, instty]
    // This store code encodes the pointer type, rather than the value type
    // this is so information only available in the pointer type (e.g. address
    // spaces) is retained.
    FUNC_CODE_INST_STORE2 = 24, // STORE:      [ptrty,ptr,val, align, vol]
    // FIXME: Remove GETRESULT in favor of EXTRACTVAL in LLVM 3.0
    FUNC_CODE_INST_GETRESULT = 25, // GETRESULT:  [ty, opval, n]
        FUNC_CODE_INST_EXTRACTVAL = 26, // EXTRACTVAL: [n x operands]
        FUNC_CODE_INST_INSERTVAL = 27, // INSERTVAL:  [n x operands]
    // fcmp/icmp returning Int1TY or vector of Int1Ty. Same as CMP, exists to
    // support legacy vicmp/vfcmp instructions.
    FUNC_CODE_INST_CMP2 = 28, // CMP2:       [opty, opval, opval, pred]
    // new select on i1 or [N x i1]
    FUNC_CODE_INST_VSELECT = 29, // VSELECT:    [ty,opval,opval,predty,pred]
        FUNC_CODE_INST_INBOUNDS_GEP = 30, // INBOUNDS_GEP: [n x operands]
        FUNC_CODE_INST_INDIRECTBR = 31, // INDIRECTBR: [opty, op0, op1, ...]

    // FIXME: Remove DEBUG_LOC in favor of DEBUG_LOC2 in LLVM 3.0
    FUNC_CODE_DEBUG_LOC = 32, // DEBUG_LOC with potentially invalid metadata
        FUNC_CODE_DEBUG_LOC_AGAIN = 33, // DEBUG_LOC_AGAIN

    FUNC_CODE_INST_CALL2 = 34, // CALL2:      [attr, fnty, fnid, args...]

    FUNC_CODE_DEBUG_LOC2 = 35;  // DEBUG_LOC2: [Line,Col,ScopeVal, IAVal]
  }

  interface Encoding {
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
  class BitCodeAbbrevOp {
    long val;           // A literal value or data for an encoding.
    boolean isLiteral;     // Indicate whether this is a literal value or not.
    int enc;     // The encoding to use.

    BitCodeAbbrevOp(long v) {
      val = v;
      isLiteral = true;
    }

    BitCodeAbbrevOp(int e, long data) {
      val = data;
      isLiteral = false;
      enc = e;
    }

    boolean isLiteral() {
      return isLiteral;
    }

    boolean isEncoding() {
      return !isLiteral;
    }

    // Accessors for literals.
    long getLiteralValue() {
      assert (isLiteral());
      return val;
    }

    // Accessors for encoding info.
    int getEncoding() {
      assert (isEncoding());
      return enc;
    }

    long getEncodingData() {
      assert (isEncoding() && hasEncodingData());
      return val;
    }

    boolean hasEncodingData() {
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
    boolean isChar6(char c) {
      if (c >= 'a' && c <= 'z') return true;
      if (c >= 'A' && c <= 'Z') return true;
      if (c >= '0' && c <= '9') return true;
      if (c == '.' || c == '_') return true;
      return false;
    }

    int encodeChar6(char c) {
      if (c >= 'a' && c <= 'z') return c - 'a';
      if (c >= 'A' && c <= 'Z') return c - 'A' + 26;
      if (c >= '0' && c <= '9') return c - '0' + 26 + 26;
      if (c == '.') return 62;
      if (c == '_') return 63;
      Util.assertion("Not a value Char6 character!");
      return 0;
    }

    char decodeChar6(int v) {
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
  static class BitCodeAbbrev {
    ArrayList<BitCodeAbbrevOp> operandList;

    int getNumOperandInfos() {
      return operandList.size();
    }

    BitCodeAbbrevOp getOperandInfo(int n) {
      return operandList.get(n);
    }

    void Add(BitCodeAbbrevOp opInfo) {
      operandList.add(opInfo);
    }
  }

  /// BlockInfo - This contains information emitted to BLOCKINFO_BLOCK blocks.
  /// These describe abbreviations that all blocks of the specified ID inherit.
  static class BlockInfo {
    int blockID;
    ArrayList<BitCodeAbbrev> abbrevs;
    String name;

    ArrayList<Pair<Integer, String>> recordNames;
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
   * The current pointer to the buffer from which the current byte is read.
   */
  private int curOffset;
  private BitStream bitStream;
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

  private BitcodeReader(MemoryBuffer buffer) {
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
    hasReversedFunctionsWithBodies = false;
    deferredFunctionInfo = new HashMap<>();
    blockAddrFwdRefs = new HashMap<>();
    blockScope = new LinkedList<>();
    curAbbrevs = new LinkedList<>();
    blockInfoRecords = new LinkedList<>();
    ignoreBlockInfoNames = false;
    valueList = new BitcodeReaderValueList();
    typeList = new ArrayList<>();
  }

  private String getErrorString() {
    return errorString;
  }

  private boolean error(String msg) {
    errorString = msg;
    return true;
  }

  private long read(int size) {
    long res = bitStream.read(curOffset, size);
    curOffset += size;
    return res;
  }

  private boolean atEndOfStream() {
    return curOffset < bitStream.size();
  }

  private long readCode() {
    return read(curCodeSize);
  }

  private int readSubBlockID() {
    int res = (int) bitStream.readVBR(curOffset, BlockIDWidth);
    curOffset += BlockIDWidth;
    return res;
  }

  private boolean parseBitcodeInfo(Module m) {
    theModule = null;
    bitStream = null;
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

    bitStream = BitStream.create(buffer);
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
    return true;
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
        case BLOCKINFO_BLOCK_ID:
          if (record.size() < 1) return true;
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

  private BlockInfo getOrCreateBlcokInfo(int id) {
    return null;
  }

  private int readRecord(long code, ArrayList<Long> record) {
    return 0;
  }

  private void readAbbrevRecord() {

  }

  private boolean readBlockEnd() {
    return false;
  }

  private boolean enterSubBlock(int blockID) {
    return enterSubBlock(blockID, null);
  }

  private boolean enterSubBlock(int blockID, OutRef<Integer> numWordsP) {
    blockScope.push(new Block(curCodeSize));
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
        curOffset + numWords*4 > bitStream.size())
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
          case TYPE_BLOCK_ID:
            if (parseTypeTable())
              return true;
            break;
          case TYPE_SYMTAB_BLOCK_ID:
            if (parseTypeSymbolTable())
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
          if (!ty.isPointerType())
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
          if (!ty.isPointerType())
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
          if (!ty.isPointerType())
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
    switch (val) {
      default:
      case 0: return CallingConv.C;
      case 8: return CallingConv.Fast;
      case 9: return CallingConv.Cold;
      case 64: return CallingConv.X86_StdCall;
      case 65: return CallingConv.X86_FastCall;
    }
  }

  private AttrList getAttributes(int val) {
    if (val - 1 < mattributes.size())
      return mattributes.get(val - 1);
    return null;
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

  private Type getTypeByID(int id, boolean isTypeTable) {
    // if the type id is in the range, return it.
    if (id >= 0 && id < typeList.size())
      return typeList.get(id);
    if (!isTypeTable) return null;

    while (typeList.size() <= id)
      typeList.add(OpaqueType.get());
    return typeList.get(typeList.size()-1);

  }

  private Type getTypeByID(int id) {
    return getTypeByID(id, false);
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

  private long getCurrentBitNo() {
    // TODO
    return 0;
  }

  private boolean parseMetadata() {
    Util.shouldNotReachHere("Metadata is unimplemented as yet!");
    return false;
  }

  private boolean parseConstants() {
    if (enterSubBlock(CONSTANTS_BLOCK_ID))
      return error("Malformed block record");

    ArrayList<Long> record = new ArrayList<>();
    // read all the records for this value table.
    Type curTy = LLVMContext.Int32Ty;
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
          v = ConstantInt.get(new APInt(((IntegerType)curTy).getBitWidth(), words));
          break;
        }
        case CST_CODE_FLOAT: {
          // FLOAT: [fpval]
          if (record.isEmpty())
            return error("Invalid CST_CODE_FLOAT");
          if (curTy.isFloatTy())
            v = ConstantFP.get(new APFloat(new APInt(32, record.get(0).intValue())));
          else if (curTy.isDoubleTy())
            v = ConstantFP.get(new APFloat(new APInt(64, record.get(0))));
          else if (curTy.isX86_FP80Ty()) {
            // bits are not stored the same way as a normal i80 APInt, compensate.
            long[] rearrange = new long[] {
              (record.get(1) & 0xFFFFL) | (record.get(0) << 16),
              record.get(0) >>> 48
            };
            v = ConstantFP.get(new APFloat(new APInt(80, rearrange)));
          }
          else if (curTy.isFP128Ty()) {
            long[] rearrange = new long[] {record.get(0), record.get(1)};
            v = ConstantFP.get(new APFloat(new APInt(128, rearrange), true));
          }
          else if (curTy.isPPC_FP128Ty()) {
            long[] rearrange = new long[] {record.get(0), record.get(1)};
            v = ConstantFP.get(new APFloat(new APInt(128, rearrange)));
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
              elts.add(valueList.getConstantFwdRefs(record.get(0).intValue(), sty.getElementType(i)));
            v = ConstantStruct.get(sty, elts);
          }
          else if (curTy instanceof ArrayType) {
            ArrayType aty = (ArrayType) curTy;
            Type eltTy = aty.getElementType();
            for (int i = 0; i < size; i++)
              elts.add(valueList.getConstantFwdRefs(record.get(i).intValue(), eltTy));
            v = ConstantArray.get(aty, elts);
          }
          else if (curTy instanceof VectorType) {
            VectorType vecTy = (VectorType) curTy;
            Type eltTy = vecTy.getElementType();
            for (int i = 0; i < size; i++)
              elts.add(valueList.getConstantFwdRefs(record.get(i).intValue(), eltTy));
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
            Constant lhs = valueList.getConstantFwdRefs(record.get(1).intValue(), curTy);
            Constant rhs = valueList.getConstantFwdRefs(record.get(2).intValue(), curTy);
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
            Constant op = valueList.getConstantFwdRefs(record.get(2).intValue(), opTy);
            v = ConstantExpr.getCast(opc, op, opTy);
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
            elts.add(valueList.getConstantFwdRefs(record.get(i+1).intValue(), eltTy));
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
              valueList.getConstantFwdRefs(record.get(0).intValue(), LLVMContext.Int1Ty),
              valueList.getConstantFwdRefs(record.get(1).intValue(), curTy),
              valueList.getConstantFwdRefs(record.get(2).intValue(), curTy));
          break;
        }
        case CST_CODE_CE_EXTRACTELT: {
          // CE_EXTRACTELT: [opty, opval, opval]
          if (record.size() < 3) return error("Invalid CE_EXTRACTELT record");
          Type resTy = getTypeByID(record.get(0).intValue());
          VectorType opTy = resTy instanceof VectorType ? (VectorType)resTy : null;
          if (opTy == null)
            return error("Invalid CE_EXTRACTELEMENT record");
          Constant op0 = valueList.getConstantFwdRefs(record.get(1).intValue(), opTy);
          Constant op1 = valueList.getConstantFwdRefs(record.get(2).intValue(), opTy);
          v = ConstantExpr.getExtractElement(op0, op1);
          break;
        }
        case CST_CODE_CE_INSERTELT: {
          // CE_INSERTELT: [opval, opval, opval]
          if (record.size() < 3) return error("Invalid CE_INSERTELT record");
          VectorType opTy = (VectorType) curTy;
          Constant op0 = valueList.getConstantFwdRefs(record.get(0).intValue(), opTy);
          Constant op1 = valueList.getConstantFwdRefs(record.get(1).intValue(), opTy.getElementType());
          Constant op2 = valueList.getConstantFwdRefs(record.get(2).intValue(), LLVMContext.Int32Ty);
          v = ConstantExpr.getInsertElement(op0, op1, op2);
          break;
        }
        case CST_CODE_CE_SHUFFLEVEC: {
          // CE_SHUFFLEVEC: [opval, opval, opval]
          if (record.size() < 3) return error("Invalid CE_SHUFFLEVEC record");
          VectorType opTy = (VectorType) curTy;
          Constant op0 = valueList.getConstantFwdRefs(record.get(0).intValue(), opTy);
          Constant op1 = valueList.getConstantFwdRefs(record.get(1).intValue(), opTy);
          Type shufTy = VectorType.get(LLVMContext.Int32Ty, opTy.getNumElements());
          Constant op2 = valueList.getConstantFwdRefs(record.get(2).intValue(), shufTy);
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

          Constant op0 = valueList.getConstantFwdRefs(record.get(1).intValue(), opTy);
          Constant op1 = valueList.getConstantFwdRefs(record.get(2).intValue(), opTy);
          Type shufTy = VectorType.get(LLVMContext.Int32Ty, rty.getNumElements());
          Constant op2 = valueList.getConstantFwdRefs(record.get(2).intValue(), shufTy);
          v = ConstantExpr.getShuffleVector(op0, op1, op2);
          break;
        }
        case CST_CODE_CE_CMP: {
          // CE_CMP: [opty, opval, opval, pred]
          if (record.size() < 4)
            return error("Invalid CST_CODE_CE_CMP record");
          Type opTy = getTypeByID(record.get(0).intValue());

          Constant op0 = valueList.getConstantFwdRefs(record.get(1).intValue(), opTy);
          Constant op1 = valueList.getConstantFwdRefs(record.get(2).intValue(), opTy);
          if (opTy.isFPOrFPVectorTy())
            v = ConstantExpr.getFCmp(getDecodedPredicate(record.get(3)), op0, op1);
          else
            v = ConstantExpr.getICmp(getDecodedPredicate(record.get(3)), op0, op1);
          break;
        }
        case CST_CODE_INLINEASM: {
          Util.shouldNotReachHere("inline asm is unimplemented!");
          break;
        }
        case CST_CODE_BLOCKADDRESS: {
          if (record.size() < 3)
            return error("Invalid CE_BLOCKADDRESS record");
          Type fnTy = getTypeByID(record.get(0).intValue());
          if (fnTy == null)
            return error("Invalid CE_BLOCKADDRESS record");
          Value val = valueList.getConstantFwdRefs(record.get(1).intValue(), fnTy);
          if (!(val instanceof Function))
            return error("Invalid CE_BLOCKADDRESS record");
          Function fn = (Function) val;
          GlobalVariable fwdRef = new GlobalVariable(fn.getParent(),
              LLVMContext.Int8Ty, false,
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
    if (val < 0 || val > Instruction.CmpInst.Predicate.values().length - 1)
      return null;
    return Instruction.CmpInst.Predicate.values()[(int) val];
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
    return false;
  }

  private boolean parseTypeSymbolTable() {
    return false;
  }

  private boolean parseTypeTable() {
    return false;
  }

  private boolean parseAttributeBlock() {
    return false;
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
    if (atEndOfStream() || curOffset + numWords*4 > bitStream.size())
      return true;

    curOffset += numWords*4;
    return false;
  }

  private void skipToWord() {
    // TODO
    Util.shouldNotReachHere("TODO");
  }

  private long readVBR(int width) {
    final long value = bitStream.readVBR(curOffset, width);
    curOffset += BitStream.widthVBR(value, width);
    return value;
  }

  /**
   * This is entry point for lazyly reading a bitcode from at a time.
   *
   * @param buffer
   * @param errMsg
   * @return
   */
  public static Module getLazyBitcodeModule(MemoryBuffer buffer,
                                            OutRef<String> errMsg) {
    Module m = new Module(buffer.getBufferIdentifier());
    BitcodeReader reader = new BitcodeReader(buffer);
    if (reader.parseBitcodeInfo(m)) {
      if (errMsg != null)
        errMsg.set(reader.getErrorString());

      return null;
    }
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
                                        OutRef<String> errorMsg) {
    return getLazyBitcodeModule(buffer, errorMsg);
  }
}
