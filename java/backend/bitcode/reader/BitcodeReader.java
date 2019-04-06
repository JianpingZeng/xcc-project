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
import backend.support.GVMaterializer;
import backend.value.GlobalValue;
import backend.value.Module;
import cfe.support.MemoryBuffer;
import tools.OutRef;
import tools.Pair;
import tools.Util;

import java.util.ArrayList;
import java.util.LinkedList;

import static backend.bitcode.BitcodeUtil.*;
import static backend.bitcode.reader.BitcodeReader.BlockIDs.MODULE_BLOCK_ID;
import static backend.bitcode.reader.BitcodeReader.BlockInfoCodes.BLOCKINFO_CODE_BLOCKNAME;
import static backend.bitcode.reader.BitcodeReader.BlockInfoCodes.BLOCKINFO_CODE_SETRECORDNAME;
import static backend.bitcode.reader.BitcodeReader.Encoding.*;
import static backend.bitcode.reader.BitcodeReader.FixedAbbrevIDs.*;
import static backend.bitcode.reader.BitcodeReader.StandardBlockIDs.BLOCKINFO_BLOCK_ID;
import static backend.bitcode.reader.BitcodeReader.StandardBlockIDs.FIRST_APPLICATION_BLOCKID;
import static backend.bitcode.reader.BitcodeReader.StandardWidths.*;

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

  private BitcodeReader(MemoryBuffer buffer) {
    errorString = null;
    theModule = null;
    this.buffer = ByteSequence.create(buffer);
    curOffset = 0;
    curCodeSize = 2;
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

    ArrayList<Integer> record = new ArrayList<>();
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
          curBlockInfo = getOrCreateBlcokInfo(record.get(0));
          break;
        case BLOCKINFO_CODE_BLOCKNAME: {
          if (curBlockInfo == null) return true;

          if (ignoreBlockInfoNames) break;
          StringBuilder name = new StringBuilder();
          for (int i : record)
            name.append((char) i);
          curBlockInfo.name = name.toString();
          break;
        }
        case BLOCKINFO_CODE_SETRECORDNAME: {
          if (curBlockInfo == null) return true;
          if (ignoreBlockInfoNames) break;
          StringBuilder name = new StringBuilder();
          for (int i = 1, e = record.size(); i < e; i++)
            name.append((char)record.get(i).intValue());
          curBlockInfo.recordNames.add(Pair.get(record.get(0), name.toString()));
          break;
        }
      }
    }
  }

  private BlockInfo getOrCreateBlcokInfo(int id) {
    return null;
  }

  private int readRecord(int code, ArrayList<Integer> record) {
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

  private static class Block {
    int prevCodeSize;
    LinkedList<BitCodeAbbrev> prevAbbrevs;
    Block(int pcs) {
      this.prevCodeSize = pcs;
      prevAbbrevs = new LinkedList<>();
    }
  }

  private LinkedList<Block> blockScope;
  private LinkedList<BitCodeAbbrev> curAbbrevs;

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

  LinkedList<BlockInfo> blockInfoRecords;

  /// IgnoreBlockInfoNames - This is set to true if we don't care about the
  /// block/record name information in the BlockInfo block. Only llvm-bcanalyzer
  /// uses this.
  boolean ignoreBlockInfoNames;

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
    return true;
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
