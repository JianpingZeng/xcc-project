/*
 * Extremely C language Compiler
 *   Copyright (c) 2015-2019, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.bitcode.writer;

import backend.bitcode.reader.BitcodeReader;
import backend.bitcode.reader.BitcodeReader.BitCodeAbbrev;
import backend.bitcode.reader.BitcodeReader.BitCodeAbbrevOp;
import backend.debug.DebugLoc;
import backend.support.*;
import backend.type.*;
import backend.value.*;
import backend.value.Instruction.*;
import gnu.trove.list.array.TLongArrayList;
import tools.APInt;
import tools.OutRef;
import tools.Pair;
import tools.Util;

import java.io.*;
import java.util.*;

import static backend.bitcode.reader.BitcodeReader.AttributeCodes.PARAMATTR_CODE_ENTRY;
import static backend.bitcode.reader.BitcodeReader.BinaryOpcodes.*;
import static backend.bitcode.reader.BitcodeReader.BitCodeAbbrevOp.isChar6;
import static backend.bitcode.reader.BitcodeReader.BlockIDs.*;
import static backend.bitcode.reader.BitcodeReader.CastOpcodes.*;
import static backend.bitcode.reader.BitcodeReader.ConstantsCodes.*;
import static backend.bitcode.reader.BitcodeReader.FixedAbbrevIDs.FIRST_APPLICATION_ABBREV;
import static backend.bitcode.reader.BitcodeReader.FunctionCodes.*;
import static backend.bitcode.reader.BitcodeReader.MetadataCodes.METADATA_FN_NODE2;
import static backend.bitcode.reader.BitcodeReader.MetadataCodes.METADATA_NODE2;
import static backend.bitcode.reader.BitcodeReader.MetadataCodes.METADATA_STRING;
import static backend.bitcode.reader.BitcodeReader.ModuleCodes.*;
import static backend.bitcode.reader.BitcodeReader.TypeCodes.*;
import static backend.bitcode.reader.BitcodeReader.ValueSymtabCodes.VST_CODE_BBENTRY;
import static backend.bitcode.reader.BitcodeReader.ValueSymtabCodes.VST_CODE_ENTRY;
import static backend.type.LLVMTypeID.*;

/**
 * This class is designed to accommodate several important interface functions
 * which are going to be used to print the LLVM IR in the format of in-memory to
 * external bitcode file.
 */
public class BitcodeWriter {
  /**
   * These are manifest constants used by the bitcode writer. They do not need to
   * be kept in sync with the reader, but need to be consistent within this file.
   */
  public static final int
    CurVersion = 0,
    // VALUE_SYMTAB_BLOCK abbrev id's.
    VST_ENTRY_8_ABBREV = FIRST_APPLICATION_ABBREV,
    VST_ENTRY_7_ABBREV = VST_ENTRY_8_ABBREV + 1,
    VST_ENTRY_6_ABBREV = VST_ENTRY_7_ABBREV + 1,
    VST_BBENTRY_6_ABBREV = VST_ENTRY_6_ABBREV + 1,

    // CONSTANTS_BLOCK abbrev id's.
    CONSTANTS_SETTYPE_ABBREV = FIRST_APPLICATION_ABBREV,
    CONSTANTS_INTEGER_ABBREV = CONSTANTS_SETTYPE_ABBREV + 1,
    CONSTANTS_CE_CAST_Abbrev = CONSTANTS_INTEGER_ABBREV + 1,
    CONSTANTS_NULL_Abbrev = CONSTANTS_CE_CAST_Abbrev + 1,

    // FUNCTION_BLOCK abbrev id's.
    FUNCTION_INST_LOAD_ABBREV = FIRST_APPLICATION_ABBREV,
    FUNCTION_INST_BINOP_ABBREV = FUNCTION_INST_LOAD_ABBREV + 1,
    FUNCTION_INST_BINOP_FLAGS_ABBREV = FUNCTION_INST_BINOP_ABBREV + 1,
    FUNCTION_INST_CAST_ABBREV = FUNCTION_INST_BINOP_FLAGS_ABBREV + 1,
    FUNCTION_INST_RET_VOID_ABBREV = FUNCTION_INST_CAST_ABBREV + 1,
    FUNCTION_INST_RET_VAL_ABBREV = FUNCTION_INST_RET_VOID_ABBREV + 1,
    FUNCTION_INST_UNREACHABLE_ABBREV = FUNCTION_INST_RET_VAL_ABBREV + 1;

  public static void writeBitcodeToFile(Module m, String file) {
    try {
      BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file));
      writeBitcodeToFile(m, os);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void writeBitcodeToFile(Module m, OutputStream os) throws IOException {
    BitstreamWriter stream = new BitstreamWriter();
    byte[] buffer = stream.getBuffer();
    writeBitcodeToStream(m, stream);
    os.write(buffer);
  }

  private static void writeBitcodeToStream(Module m, BitstreamWriter stream) {
    // if this is darwin platform, emit a file header and trailer as desired.
    boolean isDarwin = m.getTargetTriple().contains("-darwin");
    if (isDarwin)
      emitDarwinBCHeader(stream, m.getTargetTriple());
    // emit file header.
    stream.emit((byte)'B', 8);
    stream.emit((byte)'C', 8);
    stream.emit(0x0, 4);
    stream.emit(0xC, 4);
    stream.emit(0xE, 4);
    stream.emit(0xD, 4);

    // emit the module.
    writeModule(m, stream);
    if (isDarwin)
      emitDarwinBCTrailer(stream, stream.getBuffer().length);
  }

  private static void emitDarwinBCTrailer(BitstreamWriter stream, int length) {
    stream.backpatchWord(DarwinBCSizeFieldOffset, length - DarwinBCHeaderSize);;
    while ((length & 15) != 0) {
      stream.emit(0, 8);
      ++length;
    }
  }

  private static void writeModule(Module m, BitstreamWriter stream) {
    stream.enterSubBlock(MODULE_BLOCK_ID, 3);
    // emit the version number if it is non-zero.
    if (CurVersion != 0) {
      TLongArrayList vals = new TLongArrayList();
      vals.add(CurVersion);
      stream.emitRecord(MODULE_CODE_VERSION, vals);
    }

    // Analyze the module, enumerating globals, functions, etc.
    ValueEnumerator ve = new ValueEnumerator(m);

    // Emit BlockInfo, which defines the standard abbreviation etc.
    writeBlockInfo(ve, stream);

    // Emit information about parameter attributes.
    writeAttributeTable(ve, stream);

    // Emit information describing all of the types in the module.
    writeTypeTable(ve, stream);

    // Emit top-level description of module, including target
    // triple, inline asm, description for global variables, and
    // function prototype etc
    writeModuleInfo(m, ve, stream);

    // Emit constants.
    writeModuleConstants(ve, stream);

    // Emit metadata
    writeModuleMetadata(m, ve, stream);

    // Emit function bodies.
    for (Function fn : m.getFunctionList())
      if (!fn.isDeclaration())
        writeFunction(fn, ve, stream);

    // Emit metadata
    writeModuleMetadataStore(m, stream);

    // Emit the type symbol table information.
    writeTypeSymbolTable(m.getTypeSymbolTable(), ve, stream);

    // Emit names for globals/functions etc.
    writeValueSymbolTable(m.getValueSymbolTable(), ve, stream);
    stream.exitBlock();
  }

  private static void writeTypeSymbolTable(TreeMap<String, Type> typeSymbolTable,
                                           ValueEnumerator ve,
                                           BitstreamWriter stream) {
    // TODO: 7/2/19
  }

  private static void writeModuleMetadataStore(Module m,
                                               BitstreamWriter stream) {
    // TODO: 7/2/19
  }

  /**
   * Emit a function body to the module stream.
   * @param fn
   * @param ve
   * @param stream
   */
  private static void writeFunction(Function fn,
                                    ValueEnumerator ve,
                                    BitstreamWriter stream) {
    stream.enterSubBlock(FUNCTION_BLOCK_ID, 4);
    ve.incorporateFunction(fn);

    TLongArrayList vals = new TLongArrayList();
    // Emit the number of basic blocks, so the reader can create them ahead of time.
    vals.add(ve.getBasicBlocks().size());
    stream.emitRecord(FUNC_CODE_DECLAREBLOCKS, vals);
    vals.clear();

    // If there are function-local constants, emit them now.
    int cstStart, cstEnd;
    Pair<Integer, Integer> res = ve.getFunctionConstantRange();
    cstStart = res.first;
    cstEnd = res.second;
    writeConstants(cstStart, cstEnd, ve, stream, false);

    // If there is function-local metadata, emit it.
    writeFunctionLocalMetadata(fn, ve, stream);

    int instID = cstEnd;
    boolean needsMetadataAttachment = false;

    // An unknown location.
    DebugLoc lastDL = new DebugLoc();

    // Finally, emit all the instructions, in order.
    for (BasicBlock bb : fn) {
      for (Instruction inst : bb) {
        writeInstruction(inst, instID, ve, stream, vals);

        if (!inst.getType().isVoidType())
          ++instID;

        // if the instruction has metadata, write a metadata attachment later.
        needsMetadataAttachment |= inst.hasMetadataOtherThanDebugLoc();
        // if the instruction has a debug location, emit it.
        DebugLoc dl = inst.getDebugLoc();
        if (dl.isUnknown()) {
          // nothing todo.
        }
        else if (dl.equals(lastDL)) {
          stream.emitRecord(FUNC_CODE_DEBUG_LOC_AGAIN, vals);
        }
        else {
          OutRef<MDNode> scope = new OutRef<>(), ia = new OutRef<>();
          dl.getScopeAndInlinedAt(scope, ia, inst.getContext());

          vals.add(dl.getLine());
          vals.add(dl.getCol());
          vals.add(scope.get() != null ? ve.getValueID(scope.get())+1 : 0);
          vals.add(ia.get() != null ? ve.getValueID(ia.get())+1 : 0);
          stream.emitRecord(FUNC_CODE_DEBUG_LOC2, vals);
          vals.clear();

          lastDL = dl;
        }
      }
    }

    // emit names for all instructions etc.
    writeValueSymbolTable(fn.getValueSymbolTable(), ve, stream);

    if (needsMetadataAttachment) {
      writeMetadataAttachment(fn, ve, stream);
    }
    ve.purgeFunction();
    stream.exitBlock();
  }

  private static int getEncodedCastOpcode(Operator opc) {
    switch (opc) {
      case Trunc:    return CAST_TRUNC;
      case ZExt:     return CAST_ZEXT;
      case SExt:     return CAST_SEXT;
      case FPToSI:   return CAST_FPTOSI;
      case FPToUI:   return CAST_FPTOUI;
      case UIToFP:   return CAST_UITOFP;
      case SIToFP:   return CAST_SITOFP;
      case FPTrunc:  return CAST_FPTRUNC;
      case FPExt:    return CAST_FPEXT;
      case PtrToInt: return CAST_PTRTOINT;
      case IntToPtr: return CAST_INTTOPTR;
      case BitCast:  return CAST_BITCAST;
      default:
        Util.shouldNotReachHere("Unknown cast instruction!");
        return -1;
    }
  }

  private static int getEncodedBinaryOpcode(Operator opc) {
    switch (opc) {
      default:
        Util.shouldNotReachHere("Unknown binary instruction!");
        return -1;
      case Add:
      case FAdd: return BINOP_ADD;
      case Sub:
      case FSub: return BINOP_SUB;
      case Mul:
      case FMul: return BINOP_MUL;
      case UDiv: return BINOP_UDIV;
      case FDiv:
      case SDiv: return BINOP_SDIV;
      case URem: return BINOP_UREM;
      case FRem:
      case SRem: return BINOP_SREM;
      case Shl:  return BINOP_SHL;
      case LShr: return BINOP_LSHR;
      case AShr: return BINOP_ASHR;
      case And:  return BINOP_AND;
      case Or:   return BINOP_OR;
      case Xor:  return BINOP_XOR;
    }
  }

  /**
   * Emit an instruction to the specified stream.
   * @param inst
   * @param instID
   * @param ve
   * @param stream
   * @param vals
   */
  private static void writeInstruction(Instruction inst,
                                       int instID,
                                       ValueEnumerator ve,
                                       BitstreamWriter stream,
                                       TLongArrayList vals) {
    int code = 0;
    int abbrevToUse = 0;
    ve.setInstructionID(inst);
    switch (inst.getOpcode()) {
      default:
        if (inst.isCast()) {
          code = FUNC_CODE_INST_CAST;
          if (pushValueAndType(inst.operand(0), instID, vals, ve))
            abbrevToUse = FUNCTION_INST_CAST_ABBREV;
          vals.add(ve.getTypeID(inst.getType()));
          vals.add(getEncodedCastOpcode(inst.getOpcode()));
        }
        else {
          Util.assertion(inst instanceof BinaryOperator, "Unknown instruction!");
          code = FUNC_CODE_INST_BINOP;
          if (!pushValueAndType(inst.operand(0), instID, vals, ve))
            abbrevToUse = FUNCTION_INST_BINOP_ABBREV;
          vals.add(ve.getValueID(inst.operand(1)));
          vals.add(getEncodedBinaryOpcode(inst.getOpcode()));
          // Flags. TODO implements the FLAGS
          vals.add(0);
        }
        break;
      case GetElementPtr:
        code = FUNC_CODE_INST_GEP;
        if (((GEPOperator)inst).isInBounds())
          code = FUNC_CODE_INST_INBOUNDS_GEP;
        for (int i = 0, e = inst.getNumOfOperands(); i < e; i++)
          pushValueAndType(inst.operand(i), instID, vals, ve);
        break;
      case ExtractValue:
        code = FUNC_CODE_INST_EXTRACTVAL;
        pushValueAndType(inst.operand(0), instID, vals, ve);
        ExtractValueInst evi = (ExtractValueInst) inst;
        for (int idx : evi.getIndices()) {
          vals.add(idx);
        }
        break;
      case InsertValue:
        code = FUNC_CODE_INST_INSERTVAL;
        pushValueAndType(inst.operand(0), instID, vals, ve);
        pushValueAndType(inst.operand(1), instID, vals, ve);
        InsertValueInst ivi = (InsertValueInst) inst;
        for (int idx : ivi.getIndices())
          vals.add(idx);
        break;
      case ExtractElement:
        code = FUNC_CODE_INST_EXTRACTELT;
        pushValueAndType(inst.operand(0), instID, vals, ve);
        vals.add(ve.getValueID(inst.operand(1)));
        break;
      case InsertElement:
        code = FUNC_CODE_INST_INSERTELT;
        pushValueAndType(inst.operand(0), instID, vals, ve);
        vals.add(ve.getValueID(inst.operand(1)));
        vals.add(ve.getValueID(inst.operand(2)));
        break;
      case ShuffleVector:
        code = FUNC_CODE_INST_SHUFFLEVEC;
        pushValueAndType(inst.operand(0), instID, vals, ve);
        vals.add(ve.getValueID(inst.operand(1)));
        vals.add(ve.getValueID(inst.operand(2)));
        break;
      case ICmp:
      case FCmp:
        // compare returning Int1Ty or vector of Int1Ty
        code = FUNC_CODE_INST_CMP2;
        pushValueAndType(inst.operand(0), instID, vals, ve);
        vals.add(ve.getValueID(inst.operand(1)));
        vals.add(((Instruction.CmpInst)inst).getPredicate().ordinal());
        break;
      case Ret: {
        code = FUNC_CODE_INST_RET;
        int numOperand = inst.getNumOfOperands();
        if (numOperand == 0)
          abbrevToUse = FUNCTION_INST_RET_VOID_ABBREV;
        else if (numOperand == 1) {
          if (!pushValueAndType(inst.operand(0), instID, vals, ve))
            abbrevToUse = FUNCTION_INST_RET_VAL_ABBREV;
        }
        else {
          for (int i = 0; i < numOperand; i++)
            pushValueAndType(inst.operand(i), instID, vals, ve);
        }
        break;
      }
      case Br: {
        code = FUNC_CODE_INST_BR;
        BranchInst br = (BranchInst) inst;
        vals.add(ve.getValueID(br.getSuccessor(0)));
        if (br.isConditional()) {
          vals.add(ve.getValueID(br.getSuccessor(1)));
          vals.add(ve.getValueID(br.getCondition()));
        }
        break;
      }
      case Switch:
        code = FUNC_CODE_INST_SWITCH;
        vals.add(ve.getTypeID(inst.operand(0).getType()));
        for (int i = 0, e = inst.getNumOfOperands(); i < e; i++)
          vals.add(ve.getValueID(inst.operand(i)));
        break;
      case Unwind:
        code = FUNC_CODE_INST_UNWIND;
        break;
      case Unreachable:
        code = FUNC_CODE_INST_UNREACHABLE;
        abbrevToUse = FUNCTION_INST_UNREACHABLE_ABBREV;
        break;
      case Phi:
        code = FUNC_CODE_INST_PHI;
        vals.add(ve.getTypeID(inst.getType()));
        for (int i = 0, e = inst.getNumOfOperands(); i < e; i++)
          vals.add(ve.getValueID(inst.operand(i)));
        break;
      case Alloca:
        code = FUNC_CODE_INST_ALLOCA;
        vals.add(ve.getTypeID(inst.getType()));
        vals.add(ve.getTypeID(inst.operand(0).getType()));
        vals.add(ve.getValueID(inst.operand(0)));
        vals.add(Util.log2(((Instruction.AllocaInst)inst).getAlignment() ) + 1);
        break;
      case Load:
        code = FUNC_CODE_INST_LOAD;
        if (!pushValueAndType(inst.operand(0), instID, vals, ve))
          abbrevToUse = FUNCTION_INST_LOAD_ABBREV;
        vals.add(Util.log2(((LoadInst)inst).getAlignment()) + 1);
        vals.add(((LoadInst)inst).isVolatile()?1:0);
        break;
      case Store:
        code = FUNC_CODE_INST_STORE2;
        pushValueAndType(inst.operand(1), instID, vals, ve);
        vals.add(ve.getValueID(inst.operand(0)));
        vals.add(Util.log2(((StoreInst)inst).getAlignment()) + 1);
        vals.add(((StoreInst)inst).isVolatile() ? 1:0);
        break;
      case Call: {
        CallInst ci = (CallInst) inst;
        PointerType pty = (PointerType) ci.getCalledValue().getType();
        FunctionType fty = (FunctionType) pty.getElementType();
        code = FUNC_CODE_INST_CALL2;

        vals.add(ve.getAttributeID(ci.getAttributes()));
        vals.add(ci.getCallingConv().ordinal() << 1);
        pushValueAndType(ci.getCalledValue(), instID, vals, ve);

        // Emit the value for the fixed parameter.
        for (int i = 0, e = fty.getNumParams(); i < e; i++) {
          vals.add(ve.getValueID(ci.getArgOperand(i)));
        }

        // Emit the type/value pairs for vaargs parameters.
        if (fty.isVarArg()) {
          for (int i = fty.getNumParams(), e = ci.getNumOfOperands(); i < e; i++)
            pushValueAndType(ci.getArgOperand(i), instID, vals, ve);
        }
        break;
      }
      case VAArg:
        code = FUNC_CODE_INST_VAARG;
        vals.add(ve.getTypeID(inst.operand(0).getType())); // valist.
        vals.add(ve.getValueID(inst.operand(0))); // valist
        vals.add(ve.getTypeID(inst.getType())); // restype
        break;
    }
    stream.emitRecord(code, vals, abbrevToUse);
    vals.clear();
  }

  private static boolean pushValueAndType(Value v,
                                       int instID,
                                       TLongArrayList vals,
                                       ValueEnumerator ve) {
    int valID = ve.getValueID(v);
    vals.add(valID);
    if (valID >= instID) {
      vals.add(ve.getTypeID(v.getType()));
      return true;
    }
    return false;
  }

  private static void writeFunctionLocalMetadata(Function fn,
                                          ValueEnumerator ve,
                                          BitstreamWriter stream) {
    boolean startedMetadataBlock = false;
    TLongArrayList record = new TLongArrayList();
    ArrayList<MDNode> vals = ve.getFunctionLocalMDValues();
    for (MDNode node : vals) {
      if (node != null) {
        if (node.isFunctionLocal() && node.getFunction().equals(fn)) {
          if (!startedMetadataBlock) {
            stream.enterSubBlock(METADATA_BLOCK_ID, 3);
            startedMetadataBlock = true;
          }
          writeMDNode(node, ve, stream, record);
        }
      }
    }
    if (startedMetadataBlock)
      stream.exitBlock();
  }

  private static void writeMDNode(MDNode node,
                                  ValueEnumerator ve,
                                  BitstreamWriter stream,
                                  TLongArrayList records) {
    for (int i = 0, e = node.getNumOperands(); i < e; i++) {
      if (node.getOperand(i) != null) {
        records.add(ve.getTypeID(node.getOperand(i).getType()));
        records.add(ve.getValueID(node.getOperand(i)));
      }
      else {
        records.add(ve.getTypeID(Type.getVoidTy(node.getContext())));
        records.add(0);
      }
    }
    int mdCode = node.isFunctionLocal() ? METADATA_FN_NODE2 :
        METADATA_NODE2;
    stream.emitRecord(mdCode, records, 0);
    records.clear();
  }

  private static void writeMetadataAttachment(Function fn,
                                              ValueEnumerator ve,
                                              BitstreamWriter stream) {
    stream.enterSubBlock(METADATA_ATTACHMENT_ID, 3);
    TLongArrayList record = new TLongArrayList();
    // Write metadata attachments
    // METADATA_ATTACHMENT2 - [m x [value, [n x [id, mdnode]]]
    ArrayList<Pair<Integer, MDNode>> mds = new ArrayList<>();

    for (BasicBlock bb : fn) {
      for (Instruction inst : bb) {
        mds.clear();
        inst.getAllMetadataOtherThanDebugLoc(mds);

        if (mds.isEmpty())
          continue;

        record.add(ve.getInstructionID(inst));
        for (Pair<Integer, MDNode> entry : mds) {
          record.add(entry.first);
          record.add(ve.getValueID(entry.second));
        }
        stream.emitRecord(METADATA_ATTACHMENT_ID, record, 0);
        record.clear();
      }
    }
    stream.exitBlock();
  }

  private static void writeConstants(int firstVal,
                                     int lastVal,
                                     ValueEnumerator ve,
                                     BitstreamWriter stream,
                                     boolean isGlobal) {
    if (firstVal == lastVal) return;
    stream.enterSubBlock(CONSTANTS_BLOCK_ID, 4);
    int aggregateAbbrev = 0;
    int string8Abbrev = 0;
    int cstring7Abbrev = 0;
    int cstring6Abbrev = 0;
    // if this is a constant pool for the module, emit module-specific abbrevs.
    if (isGlobal) {
      // Abbrev for CST_CODE_AGGREGATE.
      BitCodeAbbrev abbv = new BitCodeAbbrev();
      abbv.add(new BitCodeAbbrevOp(CST_CODE_AGGREGATE));
      abbv.add(new BitCodeAbbrevOp(BitcodeReader.Encoding.Array));
      abbv.add(new BitCodeAbbrevOp(BitcodeReader.Encoding.Fixed, Util.log2Ceil(lastVal+1)));;
      aggregateAbbrev = stream.emitAbbrev(abbv);

      // Abbrev for CST_CODE_STRING.
      abbv = new BitCodeAbbrev();
      abbv.add(new BitCodeAbbrevOp(CST_CODE_STRING));
      abbv.add(new BitCodeAbbrevOp(BitcodeReader.Encoding.Array));
      abbv.add(new BitCodeAbbrevOp(BitcodeReader.Encoding.Fixed, 8));
      string8Abbrev = stream.emitAbbrev(abbv);

      // Abbrev for CST_CODE_CSTRING.
      abbv = new BitCodeAbbrev();
      abbv.add(new BitCodeAbbrevOp(CST_CODE_CSTRING));
      abbv.add(new BitCodeAbbrevOp(BitcodeReader.Encoding.Array));
      abbv.add(new BitCodeAbbrevOp(BitcodeReader.Encoding.Fixed, 7));
      cstring7Abbrev = stream.emitAbbrev(abbv);

      // Abbrev for CST_CODE_CSTRING.
      abbv = new BitCodeAbbrev();
      abbv.add(new BitCodeAbbrevOp(CST_CODE_CSTRING));
      abbv.add(new BitCodeAbbrevOp(BitcodeReader.Encoding.Array));
      abbv.add(new BitCodeAbbrevOp(BitcodeReader.Encoding.Char6));
      cstring6Abbrev = stream.emitAbbrev(abbv);
    }

    TLongArrayList record = new TLongArrayList();
    ArrayList<Pair<Value, Integer>> vals = ve.getValues();
    Type lastTy = null;
    for (int i = firstVal; i < lastVal; i++) {
      Value val = vals.get(i).first;
      // If we need to switch types, do so now.
      if (!val.getType().equals(lastTy)) {
        lastTy = val.getType();
        record.add(ve.getTypeID(lastTy));
        stream.emitRecord(CST_CODE_SETTYPE, record,
            CONSTANTS_SETTYPE_ABBREV);
        record.clear();
      }

      if (val instanceof InlineAsm) {
        InlineAsm ia = (InlineAsm) val;
        int sd = ia.hasSideEffects() ? 1 : 0;
        int alignStack = ia.isAlignStack() ? 1 : 0;
        record.add(sd | (alignStack << 1));

        // Add the asm string.
        String asmStr = ia.getAsmString();
        record.add(asmStr.length());
        for (int j = 0, sz = asmStr.length(); j < sz; j++)
          record.add((byte)asmStr.charAt(j));

        // Add the constraint string.
        String constraintStr = ia.getConstraintString();
        record.add(constraintStr.length());
        for (int j = 0, sz = constraintStr.length(); j < sz; j++)
          record.add((byte)constraintStr.charAt(j));

        stream.emitRecord(CST_CODE_INLINEASM, record);
        record.clear();
        continue;
      }

      Constant c = (Constant) val;
      int code = -1;
      int abbrevToUse = 0;
      if (c.isNullValue())
        code = CST_CODE_NULL;
      else if (c instanceof Value.UndefValue)
        code = CST_CODE_UNDEF;
      else if (c instanceof ConstantInt) {
        ConstantInt ci = (ConstantInt) c;
        if (ci.getBitsWidth() <= 64) {
          long v = ci.getSExtValue();
          if (v >= 0)
            record.add(v << 1);
          else
            record.add((-1 << 1) | 1);
          code = CST_CODE_INTEGER;
          abbrevToUse = CONSTANTS_INTEGER_ABBREV;
        }
        else {
          // Wide integers, > 64 bits in size.
          // We have an arbitrary precision integer value to write whose
          // bit width is > 64. However, in canonical int integer
          // format it is likely that the high bits are going to be zero.
          // So, we only write the number of active words.
          int nwords = ci.getValue().getActiveWords();
          long[] rawWords = ci.getValue().getRawData();
          for (int j = 0; j < nwords; j++) {
            long v = rawWords[j];
            if (v >= 0)
              record.add(v << 1);
            else
              record.add((-v << 1) | 1);
          }
          code = CST_CODE_WIDE_INTEGER;
        }
      }
      else if (c instanceof ConstantFP) {
        ConstantFP cfp = (ConstantFP) c;
        code = CST_CODE_FLOAT;
        Type ty = cfp.getType();
        if (ty.isFloatTy() || ty.isDoubleTy()) {
          record.add(cfp.getValueAPF().bitcastToAPInt().getZExtValue());
        }
        else if (ty.isX86_FP80Ty()) {
          // api needed to prevent premature destruction
          // bits are not in the same order as a normal i80 APInt, compensate.
          APInt api = cfp.getValueAPF().bitcastToAPInt();
          long[] rawData = api.getRawData();
          record.add((rawData[1] << 48) | (rawData[0] >> 16));
          record.add(rawData[0] & 0xffffL);
        }
        else if (ty.isFP128Ty() || ty.isPPC_FP128Ty()) {
          APInt api = cfp.getValueAPF().bitcastToAPInt();
          long[] p = api.getRawData();
          record.add(p[0]);
          record.add(p[1]);
        }
        else
          Util.assertion("Unknown FP type!");
      }
      else if (c instanceof ConstantArray && ((ConstantArray)c).isString()) {
        ConstantArray ca = (ConstantArray) c;
        // Emit constant strings specially.
        int numOps = ca.getNumOfOperands();
        // If this is a null-terminated string, use the denser CSTRING encoding.
        if (ca.operand(numOps-1).isNullValue()) {
          code = CST_CODE_CSTRING;
          --numOps;
        }
        else {
          code = CST_CODE_STRING;
          abbrevToUse = string8Abbrev;
        }
        boolean isCStr7 = code == CST_CODE_CSTRING;
        boolean isCStrChar6 = code == CST_CODE_CSTRING;
        for (int j = 0; j < numOps; j++) {
          long v = ((ConstantInt)ca.operand(i)).getZExtValue();
          record.add(v);
          isCStr7 &= (v & 128) == 0;
          if (isCStrChar6)
            isCStrChar6 = isChar6((char)v);
        }
        if (isCStrChar6)
          abbrevToUse = cstring6Abbrev;
        else if (isCStr7)
          abbrevToUse = cstring7Abbrev;
      }
      else if (c instanceof ConstantArray ||
          c instanceof ConstantStruct ||
          c instanceof ConstantVector) {
        code = CST_CODE_AGGREGATE;
        for (int j = 0, sz = c.getNumOfOperands(); j < sz; j++)
          record.add(ve.getValueID(c.operand(j)));
        abbrevToUse = aggregateAbbrev;
      }
      else if (c  instanceof ConstantExpr) {
        ConstantExpr ce = (ConstantExpr) c;
        switch (ce.getOpcode()) {
          default:
            if (ce.getOpcode().isCastOps()) {
              code = CST_CODE_CE_CAST;
              record.add(getEncodedCastOpcode(ce.getOpcode()));
              record.add(ve.getTypeID(ce.getType()));
              record.add(ve.getValueID(ce.operand(0)));
              abbrevToUse = CONSTANTS_CE_CAST_Abbrev;
            }
            else {
              Util.assertion(ce.getNumOfOperands() == 2,
                  "Unknown constant expr!");
              code = CST_CODE_CE_BINOP;
              record.add(getEncodedBinaryOpcode(ce.getOpcode()));
              record.add(ve.getValueID(ce.operand(0)));
              record.add(ve.getValueID(ce.operand(1)));
              // Flags as 0 currently, TODO 6/24/2019.
            }
            break;
          case GetElementPtr:
            code = CST_CODE_CE_GEP;
            GEPOperator gep = (GEPOperator) c;
            if (gep.isInBounds())
              code = CST_CODE_CE_INBOUNDS_GEP;
            for (int j = 0, e = ce.getNumOfOperands(); j < e; j++) {
              record.add(ve.getTypeID(c.operand(j).getType()));
              record.add(ve.getValueID(ce.operand(j)));
            }
            break;
          case Select:
            code = CST_CODE_CE_SELECT;
            record.add(ve.getValueID(c.operand(0)));
            record.add(ve.getValueID(c.operand(1)));
            record.add(ve.getValueID(c.operand(2)));
            break;
          case ExtractElement:
            code = CST_CODE_CE_EXTRACTELT;
            record.add(ve.getTypeID(ce.operand(0).getType()));
            record.add(ve.getValueID(c.operand(0)));
            record.add(ve.getValueID(c.operand(1)));
            break;
          case InsertElement:
            code = CST_CODE_CE_INSERTELT;
            record.add(ve.getValueID(c.operand(0)));
            record.add(ve.getValueID(c.operand(1)));
            record.add(ve.getValueID(c.operand(2)));
            break;
          case ShuffleVector:
            // If the return type and argument types are the same, this is a
            // standard shufflevector instruction.  If the types are different,
            // then the shuffle is widening or truncating the input vectors, and
            // the argument type must also be encoded.
            if (ce.getType().equals(ce.operand(0).getType()))
              code = CST_CODE_CE_SHUFFLEVEC;
            else {
              code = CST_CODE_CE_SHUFVEC_EX;
              record.add(ve.getTypeID(ce.operand(0).getType()));
            }
            record.add(ve.getValueID(c.operand(0)));
            record.add(ve.getValueID(c.operand(1)));
            record.add(ve.getValueID(c.operand(2)));
            break;
          case ICmp:
          case FCmp:
            code = CST_CODE_CE_CMP;
            record.add(ve.getTypeID(ce.operand(0).getType()));
            record.add(ve.getValueID(ce.operand(0)));
            record.add(ve.getValueID(ce.operand(1)));
            record.add(ce.getPredicate().ordinal());
            break;
        }
      }
      else if (c instanceof BlockAddress) {
        BlockAddress ba = (BlockAddress) c;
        Util.assertion(ba.getFunction().equals(ba.getBasicBlock().getParent()),
            "Malformed blockaddress");
        code = CST_CODE_BLOCKADDRESS;
        record.add(ve.getTypeID(ba.getFunction().getType()));
        record.add(ve.getValueID(ba.getFunction()));
        record.add(ve.getGlobalBasicBlockID(ba.getBasicBlock()));
     }
     else {
        Util.shouldNotReachHere("Unknown constant!");
      }
      stream.emitRecord(code, record, abbrevToUse);
      record.clear();
    }
    stream.exitBlock();
  }

  private static void writeModuleMetadata(Module m,
                                          ValueEnumerator ve,
                                          BitstreamWriter stream) {
    ArrayList<Pair<Value, Integer>> vals = ve.getMdValues();
    boolean startedMetadataBlock = false;
    int mdsAbbrev = 0;
    TLongArrayList record = new TLongArrayList();
    for (int i = 0, e = vals.size(); i < e; i++) {
      if (vals.get(i).first instanceof MDNode) {
        MDNode n = (MDNode) vals.get(i).first;
        if (!n.isFunctionLocal() || n.getFunction() == null) {
          if (!startedMetadataBlock) {
            stream.enterSubBlock(METADATA_BLOCK_ID, 3);;
            startedMetadataBlock = true;
          }
          writeMDNode(n, ve, stream, record);
        }
      }
      else if (vals.get(i).first instanceof MDString) {
        MDString mds = (MDString) vals.get(i).first;
        if (!startedMetadataBlock) {
          stream.enterSubBlock(METADATA_BLOCK_ID, 3);;
          BitCodeAbbrev abbv = new BitCodeAbbrev();
          abbv.add(new BitCodeAbbrevOp(METADATA_STRING));;
          abbv.add(new BitCodeAbbrevOp(BitcodeReader.Encoding.Array));
          abbv.add(new BitCodeAbbrevOp(BitcodeReader.Encoding.Fixed, 8));
          mdsAbbrev = stream.emitAbbrev(abbv);
          startedMetadataBlock = true;
        }

        // Code: [strchar x N]
        for (char ch : mds.getName().toCharArray())
          record.add((byte)ch);

        // // Emit the finished record.
        stream.emitRecord(METADATA_STRING, record, mdsAbbrev);
        record.clear();
      }
    }

    // Write named metadata.
    // TODO 6/24/2019, write named metadata in module
    if (startedMetadataBlock)
      stream.exitBlock();
  }

  private static void writeModuleConstants(ValueEnumerator ve,
                                           BitstreamWriter stream) {
    ArrayList<Pair<Value, Integer>> vals = ve.getValues();

    // Find the first constant to emit, which is the first non-globalvalue value.
    // We know globalvalues have been emitted by WriteModuleInfo.
    for (int i = 0, e = vals.size(); i < e; i++) {
      if (!(vals.get(i).first instanceof GlobalValue)) {
        writeConstants(i, e, ve, stream, true);
        return;
      }
    }
  }

  /**
   * Emit top-level description of module, including target triple, inline asm,
   * descriptors for global variables, and function prototype info.
   * @param m
   * @param ve
   * @param stream
   */
  private static void writeModuleInfo(Module m,
                                      ValueEnumerator ve,
                                      BitstreamWriter stream) {
    // Emit the list of dependent libraries for the Module.
    for (String lib : m.getLibraryList()) {
      writeStringRecord(MODULE_CODE_DEPLIB, lib, 0, stream);
    }
    // Emit various pieces of data attached to a module.
    if (!m.getTargetTriple().isEmpty())
      writeStringRecord(MODULE_CODE_TRIPLE,
          m.getTargetTriple(), 0, stream);
    if (!m.getDataLayout().isEmpty())
      writeStringRecord(MODULE_CODE_DATALAYOUT,
          m.getDataLayout(), 0, stream);
    if (!m.getModuleInlineAsm().isEmpty())
      writeStringRecord(MODULE_CODE_ASM,
          m.getModuleInlineAsm(), 0, stream);

    // Emit information about sections and GC, computing how many there are. Also
    // compute the maximum alignment value.
    // We don't use GC at all!!!
    TreeMap<String, Integer> sectionMap = new TreeMap<>();
    int maxAlignment = 0;
    int maxGlobalType = 0;
    for (GlobalVariable gv : m.getGlobalVariableList()) {
      maxAlignment = Math.max(maxAlignment, gv.getAlignment());
      maxGlobalType = Math.max(maxGlobalType, ve.getTypeID(gv.getType()));

      if (!gv.hasSection()) continue;
      // Give section names unique ID's.
      if (sectionMap.containsKey(gv.getSection()))
        continue;
      writeStringRecord(MODULE_CODE_SECTIONNAME, gv.getSection(),
          0, stream);
      sectionMap.put(gv.getSection(), sectionMap.size());
    }

    for (Function fn : m.getFunctionList()) {
      maxAlignment = Math.max(maxAlignment, fn.getAlignment());
      if (fn.hasSection()) {
        // Give section names unique ID's.
        if (!sectionMap.containsKey(fn.getSection())) {
          writeStringRecord(MODULE_CODE_SECTIONNAME,
              fn.getSection(), 0, stream);
          sectionMap.put(fn.getSection(), sectionMap.size());
        }
      }
    }

    // Emit abbrev for globals, now that we know # sections and max alignment.
    int simpleGVarAbbrev = 0;
    if (!m.getGlobalVariableList().isEmpty()) {
      // Add an abbrev for common globals with no visibility or thread localness.
      BitCodeAbbrev abbv = new BitCodeAbbrev();
      abbv.add(new BitCodeAbbrevOp(MODULE_CODE_GLOBALVAR));
      abbv.add(new BitCodeAbbrevOp(BitcodeReader.Encoding.Fixed,
          Util.log2Ceil(maxGlobalType+1)));
      // Constant.
      abbv.add(new BitCodeAbbrevOp(BitcodeReader.Encoding.Fixed, 1));
      // Initializer.
      abbv.add(new BitCodeAbbrevOp(BitcodeReader.Encoding.VBR, 6));
      // linkage and alignment
      abbv.add(new BitCodeAbbrevOp(BitcodeReader.Encoding.Fixed, 4));
      if (maxAlignment == 0)
        abbv.add(new BitCodeAbbrevOp(0));
      else {
        int maxEncAlignment = Util.log2(maxAlignment) + 1;
        abbv.add(new BitCodeAbbrevOp(BitcodeReader.Encoding.Fixed,
            Util.log2Ceil(maxEncAlignment+1)));
      }
      if (sectionMap.isEmpty())
        abbv.add(new BitCodeAbbrevOp(0));
      else
        abbv.add(new BitCodeAbbrevOp(BitcodeReader.Encoding.Fixed,
            Util.log2Ceil(sectionMap.size()+1)));

      // Don't bother emitting vis + thread local.
      simpleGVarAbbrev = stream.emitAbbrev(abbv);
    }

    // Emit the global variable information.
    TLongArrayList vals = new TLongArrayList();
    for (GlobalVariable gv : m.getGlobalVariableList()) {
      int abbrevToUse = 0;
      // GLOBALVAR: [type, isconst, initid,
      //             linkage, alignment, section, visibility, threadlocal]
      vals.add(ve.getTypeID(gv.getType()));
      vals.add(gv.isConstant() ? 1 : 0);
      vals.add(gv.isDeclaration() ? 0 :
          (ve.getValueID(gv.getInitializer()) + 1));
      vals.add(getEncodingLinkage(gv));
      vals.add(Util.log2(gv.getAlignment()) + 1);
      vals.add(gv.hasSection() ? sectionMap.get(gv.getSection()) : 0);
      if (gv.isThreadLocal() ||
          gv.getVisibility() != GlobalValue.VisibilityTypes.DefaultVisibility) {
        vals.add(getEncodingVisibility(gv));
        vals.add(gv.isThreadLocal() ? 1 : 0);
      }
      else
        abbrevToUse = simpleGVarAbbrev;

      stream.emitRecord(MODULE_CODE_GLOBALVAR, vals, abbrevToUse);
      vals.clear();
    }

    // Emit the function proto information.
    for (Function fn : m.getFunctionList()) {
      // FUNCTION:  [type, callingconv, isproto, paramattr,
      //             linkage, alignment, section, visibility, gc]
      vals.add(ve.getTypeID(fn.getType()));
      vals.add(fn.getCallingConv().ordinal());
      vals.add(fn.isDeclaration() ? 1 : 0);
      vals.add(getEncodingLinkage(fn));
      vals.add(ve.getAttributeID(fn.getAttributes()));
      vals.add(Util.log2(fn.getAlignment()) + 1);
      vals.add(fn.hasSection() ? sectionMap.get(fn.getSection()) : 0);
      int abbrevToUse = 0;
      stream.emitRecord(MODULE_CODE_FUNCTION, vals, abbrevToUse);
      vals.clear();
    }

    // Emit the alias information.
    for (GlobalAlias ga : m.getAliasList()) {
      vals.add(ve.getTypeID(ga.getType()));
      vals.add(ve.getValueID(ga.getAliasee()));
      vals.add(getEncodingLinkage(ga));
      vals.add(getEncodingVisibility(ga));
      int abbrevToUse = 0;
      stream.emitRecord(MODULE_CODE_ALIAS, vals, abbrevToUse);
      vals.clear();
    }
  }

  private static int getEncodingVisibility(GlobalValue gv) {
    switch (gv.getVisibility()) {
      case DefaultVisibility:   return 0;
      case HiddenVisibility:    return 1;
      case ProtectedVisibility: return 2;
      default:
        Util.shouldNotReachHere("Invalid visibility type!");
        return -1;
    }
  }

  private static int getEncodingLinkage(GlobalValue gv) {
    switch (gv.getLinkage()) {
      case ExternalLinkage:                 return 0;
      case WeakAnyLinkage:                  return 1;
      case AppendingLinkage:                return 2;
      case InternalLinkage:                 return 3;
      case LinkOnceAnyLinkage:              return 4;
      case DLLImportLinkage:                return 5;
      case DLLExportLinkage:                return 6;
      case ExternalWeakLinkage:             return 7;
      case CommonLinkage:                   return 8;
      case PrivateLinkage:                  return 9;
      case WeakODRLinkage:                  return 10;
      case LinkOnceODRLinkage:              return 11;
      case AvailableExternallyLinkage:      return 12;
      case LinkerPrivateLinkage:            return 13;
      case LinkerPrivateWeakLinkage:        return 14;
      case LinkerPrivateWeakDefAutoLinkage: return 15;
      default:
        Util.shouldNotReachHere("Invalid Linkage!");
        return -1;
    }
  }


  private static void writeStringRecord(int code,
                                        String str,
                                        int abbrevToUse,
                                        BitstreamWriter stream) {
    TLongArrayList vals = new TLongArrayList();
    // Code: [strchar x N]
    for (char ch : str.toCharArray())
      vals.add((byte)ch);
    // Emit the finished record.
    stream.emitRecord(code, vals, abbrevToUse);
  }

  private static void writeTypeTable(ValueEnumerator ve,
                                     BitstreamWriter stream) {
    ArrayList<Pair<Type, Integer>> typeList = ve.getTypes();

    stream.enterSubBlock(TYPE_BLOCK_ID, 4 /*count from # abbrevs */);
    TLongArrayList typeVals = new TLongArrayList();

    // Abbrev for TYPE_CODE_POINTER.
    BitCodeAbbrev abbv = new BitCodeAbbrev();
    abbv.add(new BitCodeAbbrevOp(TYPE_CODE_POINTER));
    abbv.add(new BitCodeAbbrevOp(BitcodeReader.Encoding.Fixed,
        Util.log2Ceil(ve.getTypes().size()+1)));
    abbv.add(new BitCodeAbbrevOp(0));  // Addrspace = 0
    int PtrAbbrev = stream.emitAbbrev(abbv);

    // Abbrev for TYPE_CODE_FUNCTION.
    abbv = new BitCodeAbbrev();
    abbv.add(new BitCodeAbbrevOp(TYPE_CODE_FUNCTION));
    abbv.add(new BitCodeAbbrevOp(BitcodeReader.Encoding.Fixed, 1));  // isvararg
    abbv.add(new BitCodeAbbrevOp(0));  // FIXME: DEAD value, remove in LLVM 3.0
    abbv.add(new BitCodeAbbrevOp(BitcodeReader.Encoding.Array));
    abbv.add(new BitCodeAbbrevOp(BitcodeReader.Encoding.Fixed,
        Util.log2Ceil(ve.getTypes().size()+1)));
    int FunctionAbbrev = stream.emitAbbrev(abbv);

    // Abbrev for TYPE_CODE_STRUCT.
    abbv = new BitCodeAbbrev();
    abbv.add(new BitCodeAbbrevOp(TYPE_CODE_STRUCT));
    abbv.add(new BitCodeAbbrevOp(BitcodeReader.Encoding.Fixed, 1));  // ispacked
    abbv.add(new BitCodeAbbrevOp(BitcodeReader.Encoding.Array));
    abbv.add(new BitCodeAbbrevOp(BitcodeReader.Encoding.Fixed,
        Util.log2Ceil(ve.getTypes().size()+1)));
    int StructAbbrev = stream.emitAbbrev(abbv);

    // Abbrev for TYPE_CODE_ARRAY.
    abbv = new BitCodeAbbrev();
    abbv.add(new BitCodeAbbrevOp(TYPE_CODE_ARRAY));
    abbv.add(new BitCodeAbbrevOp(BitcodeReader.Encoding.VBR, 8));   // size
    abbv.add(new BitCodeAbbrevOp(BitcodeReader.Encoding.Fixed,
        Util.log2Ceil(ve.getTypes().size()+1)));
    int ArrayAbbrev = stream.emitAbbrev(abbv);

    // Emit an entry count so the reader can reserve space.
    typeVals.add(typeList.size());
    stream.emitRecord(TYPE_CODE_NUMENTRY, typeVals);
    typeVals.clear();

    // Loop over all of the types, emitting each in turn.
    for (int i = 0, e = typeList.size(); i != e; ++i) {
     Type T = typeList.get(i).first;
      int AbbrevToUse = 0;
      int Code = 0;

      switch (T.getTypeID()) {
        default: Util.shouldNotReachHere("Unknown type!");
        case VoidTyID:   Code = TYPE_CODE_VOID;   break;
        case FloatTyID:  Code = TYPE_CODE_FLOAT;  break;
        case DoubleTyID: Code = TYPE_CODE_DOUBLE; break;
        case X86_FP80TyID: Code = TYPE_CODE_X86_FP80; break;
        case FP128TyID: Code = TYPE_CODE_FP128; break;
        case PPC_FP128TyID: Code = TYPE_CODE_PPC_FP128; break;
        case LabelTyID:  Code = TYPE_CODE_LABEL;  break;
        case OpaqueTyID: Code = TYPE_CODE_OPAQUE; break;
        case MetadataTyID: Code = TYPE_CODE_METADATA; break;
        case IntegerTyID:
          // INTEGER: [width]
          Code = TYPE_CODE_INTEGER;
          typeVals.add(((IntegerType)(T)).getBitWidth());
          break;
        case PointerTyID: {
       PointerType PTy = (PointerType) T; 
          // POINTER: [pointee type, address space]
          Code = TYPE_CODE_POINTER;
          typeVals.add(ve.getTypeID(PTy.getElementType()));
          int AddressSpace = PTy.getAddressSpace();
          typeVals.add(AddressSpace);
          if (AddressSpace == 0) AbbrevToUse = PtrAbbrev;
          break;
        }
        case FunctionTyID: {
       FunctionType FT = (FunctionType) T;
          // FUNCTION: [isvararg, attrid, retty, paramty x N]
          Code = TYPE_CODE_FUNCTION;
          typeVals.add(FT.isVarArg() ? 1 : 0);
          typeVals.add(0);  // FIXME: DEAD: remove in llvm 3.0
          typeVals.add(ve.getTypeID(FT.getReturnType()));
          for (int j = 0, sz = FT.getNumParams(); j < sz; ++j)
            typeVals.add(ve.getTypeID(FT.getParamType(j)));
          AbbrevToUse = FunctionAbbrev;
          break;
        }
        case StructTyID: {
       StructType ST = (StructType) T;
          // STRUCT: [ispacked, eltty x N]
          Code = TYPE_CODE_STRUCT;
          typeVals.add(ST.isPacked() ? 1 : 0);
          // Output all of the element types.
          for (int j = 0, sz = ST.getNumOfElements(); j < sz; j++)
            typeVals.add(ve.getTypeID(ST.getElementType(j)));

          AbbrevToUse = StructAbbrev;
          break;
        }
        case ArrayTyID: {
          ArrayType AT = (ArrayType) T;
          // ARRAY: [numelts, eltty]
          Code = TYPE_CODE_ARRAY;
          typeVals.add(AT.getNumElements());
          typeVals.add(ve.getTypeID(AT.getElementType()));
          AbbrevToUse = ArrayAbbrev;
          break;
        }
        case VectorTyID: {
          VectorType VT = (VectorType) T;
          // VECTOR [numelts, eltty]
          Code = TYPE_CODE_VECTOR;
          typeVals.add(VT.getNumElements());
          typeVals.add(ve.getTypeID(VT.getElementType()));
          break;
        }
      }

      // Emit the finished record.
      stream.emitRecord(Code, typeVals, AbbrevToUse);
      typeVals.clear();
    }

    stream.exitBlock();
  }

  private static void writeAttributeTable(ValueEnumerator ve,
                                          BitstreamWriter stream) {
    ArrayList<AttrList> attrs = ve.getAttributes();
    if (attrs.isEmpty()) return;

    stream.enterSubBlock(PARAMATTR_BLOCK_ID, 3);
    TLongArrayList record = new TLongArrayList();
    for (AttrList attr : attrs) {
      for (int i = 0, e = attr.size(); i < e; i++) {
        AttributeWithIndex pawi = attr.getSlot(i);
        record.add(pawi.index);

        long fauxAttr = pawi.attrs & 0xffff;
        if ((pawi.attrs & Attribute.Alignment) != 0)
          fauxAttr |= (1L << 16) << (((pawi.attrs & Attribute.Alignment) - 1) >>> 16);
        fauxAttr |= (pawi.attrs & (0x3FFL << 21)) << 11;
        record.add(fauxAttr);
      }

      stream.emitRecord(PARAMATTR_CODE_ENTRY, record);
      record.clear();
    }
    stream.exitBlock();
  }

  private static void writeBlockInfo(ValueEnumerator ve,
                                     BitstreamWriter stream) {
    // We only want to emit block info records for blocks that have multiple
    // instances: CONSTANTS_BLOCK, FUNCTION_BLOCK and VALUE_SYMTAB_BLOCK.  Other
    // blocks can defined their abbrevs inline.
    stream.enterBlockInfoBlock(2);

    {
      // 8-bit fixed-width VST_ENTRY/VST_BBENTRY strings.
      BitCodeAbbrev abbv = new BitCodeAbbrev();
      abbv.add(new BitCodeAbbrevOp(BitcodeReader.Encoding.Fixed, 3));
    }
  }

  /**
   * Emit names for globals/functions etc.
   * @param vst
   * @param ve
   * @param stream
   */
  private static void writeValueSymbolTable(ValueSymbolTable vst,
                                            ValueEnumerator ve,
                                            BitstreamWriter stream) {
    if (vst.isEmpty()) return;

    stream.enterSubBlock(VALUE_SYMTAB_BLOCK_ID, 4);
    TLongArrayList nameVals = new TLongArrayList();
    for (Map.Entry<String, Value> entry : vst.getMap().entrySet()) {
      String name = entry.getKey();
      boolean is7Bit = true;
      boolean isChar6 = true;
      for (char ch : name.toCharArray()) {
        if (isChar6)
          isChar6 = isChar6(ch);
        if ((ch & 128) != 0) {
          is7Bit = false;
          break;
        }
      }

      int abbrevToUse = VST_ENTRY_8_ABBREV;
      // VST_ENTRY:   [valueid, namechar x N]
      // VST_BBENTRY: [bbid, namechar x N]
      int code;
      if (entry.getValue() instanceof BasicBlock) {
        code = VST_CODE_BBENTRY;
        if (isChar6)
          abbrevToUse = VST_BBENTRY_6_ABBREV;
      }
      else {
        code = VST_CODE_ENTRY;
        if (isChar6)
          abbrevToUse = VST_ENTRY_6_ABBREV;
        else if (is7Bit)
          abbrevToUse = VST_ENTRY_7_ABBREV;
      }
      nameVals.add(ve.getValueID(entry.getValue()));
      for (char ch : name.toCharArray())
        nameVals.add((byte)ch);

      // Emit the finished record.
      stream.emitRecord(code, nameVals, abbrevToUse);
      nameVals.clear();
    }
    stream.exitBlock();
  }

  /** If generating a bc file on darwin, we have to emit a
   * header and trailer to make it compatible with the system archiver.  To do
   * this we emit the following header, and then emit a trailer that pads the
   * file out to be a multiple of 16 bytes.
   *
   * struct bc_header {
   *   uint32_t Magic;         // 0x0B17C0DE
   *   uint32_t Version;       // Version, currently always 0.
   *   uint32_t BitcodeOffset; // Offset to traditional bitcode file.
   *   uint32_t BitcodeSize;   // Size of traditional bitcode file.
   *   uint32_t CPUType;       // CPU specifier.
   *   ... potentially more later ...
   * };
   */
  private static final int DarwinBCSizeFieldOffset = 3*4; // Offset to bitcode_size.
  private static final int DarwinBCHeaderSize = 5*4;

  private static void emitDarwinBCHeader(BitstreamWriter stream, String tt) {
    int cpuType = ~0;
    // Match x86_64-*, i[3-9]86-*, powerpc-*, powerpc64-*, arm-*, thumb-*,
    // armv[0-9]-*, thumbv[0-9]-*, armv5te-*, or armv6t2-*. The CPUType is a magic
    // number from /usr/include/mach/machine.h.  It is ok to reproduce the
    // specific constants here because they are implicitly part of the Darwin ABI.
    int DARWIN_CPU_ARCH_ABI64      = 0x01000000,
        DARWIN_CPU_TYPE_X86        = 7,
        DARWIN_CPU_TYPE_ARM        = 12,
        DARWIN_CPU_TYPE_POWERPC    = 18;

    if (tt.contains("x86_64-"))
      cpuType = DARWIN_CPU_TYPE_X86 | DARWIN_CPU_ARCH_ABI64;
    else if (tt.length() >= 5 && tt.charAt(0) == 'i' &&
        tt.charAt(2) == '8' && tt.charAt(3) == '6' &&
        tt.charAt(4) == '-' && tt.charAt(1) - '3' < 6)
      cpuType = DARWIN_CPU_TYPE_X86;
    else if (tt.contains("powerpc-"))
      cpuType = DARWIN_CPU_TYPE_POWERPC;
    else if (tt.contains("powerpc640"))
      cpuType = DARWIN_CPU_TYPE_POWERPC | DARWIN_CPU_ARCH_ABI64;
    else if (isARMTriplet(tt))
      cpuType = DARWIN_CPU_TYPE_ARM;

    // Traditional bitcode starts after header.
    int bcOffset = DarwinBCHeaderSize;
    stream.emit(0x0B17C0DE, 32);
    stream.emit(0         , 32);
    stream.emit(bcOffset      , 32);
    stream.emit(0         , 32);
    stream.emit(cpuType       , 32);
  }

  /**
   * Return true if the specified target triple string is used to
   * describe the variant of ARM architecture.
   * @param tt
   * @return
   */
  private static boolean isARMTriplet(String tt) {
    int i = 0, size = tt.length();
    if (size >= 6 && tt.charAt(0) == 't' && tt.charAt(1) == 'h' &&
        tt.charAt(2) == 'u' && tt.charAt(3) == 'm' && tt.charAt(4) == 'b' )
      i = 5;
    else if (size >= 4 && tt.charAt(0) == 'a' && tt.charAt(1) == 'r' &&
        tt.charAt(2) == 'm')
      i = 3;
    else
      return false;
    if (tt.charAt(i) == '-')
      return true;
    else if (tt.charAt(i) == 'v') {
      if (size >= i + 4 &&
          tt.charAt(i + 1) == '6' && tt.charAt(i+2) == 't' &&
          tt.charAt(i+3) == '2')
        return true;
      else if (size >= i + 4 &&
          tt.charAt(i + 1) == '5' && tt.charAt(i+2) == 't' &&
          tt.charAt(i+3) == 'e')
        return true;
      else
        return false;
    }
    while (++i < size && tt.charAt(i) != '-') {
      if (!Character.isDigit(tt.charAt(i)))
        return false;
    }
    return true;
  }
}
