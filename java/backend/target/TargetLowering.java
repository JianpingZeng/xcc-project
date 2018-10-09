package backend.target;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

import backend.codegen.*;
import backend.codegen.dagisel.*;
import backend.codegen.dagisel.SDNode.*;
import backend.intrinsic.Intrinsic;
import backend.support.CallingConv;
import backend.type.PointerType;
import backend.type.Type;
import backend.value.Function;
import backend.value.GlobalValue;
import backend.value.Instruction;
import backend.value.Value;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.*;

import java.util.ArrayList;
import java.util.Arrays;

import static backend.codegen.RTLIB.*;
import static backend.codegen.dagisel.CondCode.*;
import static backend.codegen.dagisel.FunctionLoweringInfo.computeValueVTs;
import static backend.codegen.dagisel.RegsForValue.getCopyFromParts;
import static backend.codegen.dagisel.RegsForValue.getCopyToParts;
import static backend.target.TargetLowering.LegalizeAction.*;
import static backend.target.TargetOptions.EnablePerformTailCallOpt;

/**
 * This class defines information used to lower LLVM code to
 * legal SelectionDAG operators that the target instruction selector can accept
 * natively.
 * <p>
 * This class also defines callbacks that targets must implement to lower
 * target-specific constructs to SelectionDAG operators.
 *
 * @author Jianping Zeng
 * @version 0.1
 */
public abstract class TargetLowering {
  /**
   * This enum indicates whether operations are valid for a
   * target, and if not, what action should be used to make them valid.
   */
  public enum LegalizeAction {
    /**
     * The target natively supports this operation.
     */
    Legal,

    /**
     * This operation should be executed in a larger type.
     */
    Promote,

    /**
     * Try to expand this to other ops, otherwise use a libcall.
     */
    Expand,

    /**
     * Use the LowerOperation hook to implement custom lowering.
     */
    Custom
  }

  /**
   * How the target represents true/false values.
   */
  public enum BooleanContent {
    UndefinedBooleanContent,    // Only bit 0 counts, the rest can hold garbage.
    ZeroOrOneBooleanContent,        // All bits zero except for bit 0.
    ZeroOrNegativeOneBooleanContent // All bits equal to bit 0.
  }

  public static class TargetLoweringOpt {
    public SelectionDAG dag;
    public SDValue oldVal;
    public SDValue newVal;

    public TargetLoweringOpt(SelectionDAG inDAG) {
      dag = inDAG;
    }

    public boolean combineTo(SDValue o, SDValue n) {
      oldVal = o;
      newVal = n;
      return true;
    }

    public boolean shrinkDemandedConstant(SDValue op, APInt demanded) {
      switch (op.getOpcode()) {
        default:
          break;
        case ISD.XOR:
        case ISD.AND:
        case ISD.OR: {
          ConstantSDNode c = op.getOperand(1).getNode() instanceof ConstantSDNode ?
              (ConstantSDNode) op.getOperand(1).getNode() : null;
          if (c == null) return false;

          if (op.getOpcode() == ISD.XOR &&
              (c.getAPIntValue().or(demanded.negative())).isAllOnesValue())
            return false;

          if (c.getAPIntValue().intersects(demanded.negative())) {
            EVT vt = op.getValueType();
            SDValue newVal = dag.getNode(op.getOpcode(), vt, op.getOperand(0),
                dag.getConstant(demanded.and(c.getAPIntValue()), vt, false));
            return combineTo(op, newVal);
          }
          break;
        }
      }
      return false;
    }

    public boolean shrinkDemandedOp(SDValue op, int bitwidth, APInt demanded) {
      Util.assertion(op.getNumOperands() == 2);
      Util.assertion(op.getNode().getNumValues() == 1);

      if (!op.getNode().hasOneUse())
        return false;

      TargetLowering tli = dag.getTargetLoweringInfo();
      long smallVTBits = bitwidth - demanded.countLeadingZeros();
      if (!Util.isPowerOf2(smallVTBits))
        smallVTBits = Util.nextPowerOf2(smallVTBits);
      for (; smallVTBits < bitwidth; smallVTBits = Util.nextPowerOf2(smallVTBits)) {
        EVT smallVT = EVT.getIntegerVT((int) smallVTBits);
        if (tli.isTruncateFree(op.getValueType(), smallVT) &&
            tli.isZExtFree(smallVT, op.getValueType())) {
          SDValue x = dag.getNode(op.getOpcode(), smallVT,
              dag.getNode(ISD.TRUNCATE, smallVT,
                  op.getNode().getOperand(0)),
              dag.getNode(ISD.TRUNCATE, smallVT,
                  op.getNode().getOperand(1)));
          SDValue z = dag.getNode(ISD.ZERO_EXTEND, op.getValueType(), x);
          return combineTo(op, z);
        }
      }
      return false;
    }
  }

  /**
   * This indicates the default register class to use for
   * each ValueType the target supports natively.
   */
  protected TargetRegisterClass[] registerClassForVT;
  protected EVT[] transformToType;
  protected int[] numRegistersForVT;
  protected EVT[] registerTypeForVT;

  protected ValueTypeAction valueTypeAction;

  protected TargetMachine tm;
  protected TargetData td;

  private ArrayList<Pair<EVT, TargetRegisterClass>> availableRegClasses;
  private boolean isLittleEndian;
  private MVT pointerTy;
  /**
   * True if this target uses GOT for PIC code.
   */
  private boolean usesGlobalOffsetTable;
  /**
   * Tells the code generator not to expand operations into sequences
   * that use the selection operation is possible.
   */
  private boolean selectIsExpensive;
  /**
   * Tells the target not to expand integer divide by constants into a
   * sequence of muls, adds, and shifts. This is a hack until a real
   * cost mode is in place. If we ever optimize for size, this will be
   * set to true unconditionally.
   */
  private boolean intDivIsCheap;
  /**
   * Tells the target whether it shouldn't generate srl/add/sra for a signed
   * divide by power of two, and let the target handle it.
   */
  private boolean pow2DivIsCheap;
  /**
   * This target to use _setjmp to implement llvm.setjmp. Default to false.
   */
  private boolean useUnderscoreSetJmp;
  /**
   * This target to use _longjmp to implement llvm.longjmp. Default to false.
   */
  private boolean useUnderscoreLongJmp;
  /**
   * This type used for shift amounts. Usually i8 or whatever pointer is.
   */
  private MVT shiftAmountTy;
  /**
   * The information about the contents of the high-bits in boolean values
   * held in a type wider than i1. See {@linkplain #getBooleanContents()}.
   */
  private BooleanContent booleanContents;

  private int exceptionPointerRegister;

  private int exceptionSelectorRegister;

  private long[][] opActions;
  private long[] loadExtActions;
  private long[] truncStoreActions;
  private long[][][] indexedModeActions;
  private long[] convertActions;
  private long[] condCodeActions;
  private byte[] targetDAGCombineArray;
  private TObjectIntHashMap<Pair<Integer, Integer>> promoteType;

  private String[] libCallRoutineNames;
  private CondCode[] cmpLibCallCCs;
  private CallingConv[] libCallCallingConv;

  private int stackPointerRegisterToSaveRestore;

  protected int maxStoresPerMemset;
  protected int maxStoresPerMemcpy;
  protected int maxStoresPerMemmove;
  protected int setPrefLoopAlignment;
  protected boolean benefitFromCodePlacementOpt;

  protected ArrayList<APFloat> legalFPImmediates;

  public TargetLowering(TargetMachine tm) {
    this.tm = tm;
    td = tm.getTargetData();
    valueTypeAction = new ValueTypeAction();
    availableRegClasses = new ArrayList<>();
    registerClassForVT = new TargetRegisterClass[MVT.LAST_VALUETYPE];
    transformToType = new EVT[MVT.LAST_VALUETYPE];
    numRegistersForVT = new int[MVT.LAST_VALUETYPE];
    registerTypeForVT = new EVT[MVT.LAST_VALUETYPE];
    libCallRoutineNames = new String[RTLIB.UNKNOWN_LIBCALL.ordinal()];
    cmpLibCallCCs = new CondCode[RTLIB.UNKNOWN_LIBCALL.ordinal()];
    libCallCallingConv = new CallingConv[RTLIB.UNKNOWN_LIBCALL.ordinal()];
    opActions = new long[MVT.LAST_VALUETYPE / 8 * 4][ISD.BUILTIN_OP_END];
    loadExtActions = new long[LoadExtType.LAST_LOADEXT_TYPE.ordinal()];
    truncStoreActions = new long[MVT.LAST_VALUETYPE];
    indexedModeActions = new long[MVT.LAST_VALUETYPE][2][MemIndexedMode.values().length];
    convertActions = new long[MVT.LAST_VALUETYPE];
    condCodeActions = new long[SETCC_INVALID.ordinal()];
    targetDAGCombineArray = new byte[(ISD.BUILTIN_OP_END + 7) / 8];
    promoteType = new TObjectIntHashMap<Pair<Integer, Integer>>();
    legalFPImmediates = new ArrayList<>();

    stackPointerRegisterToSaveRestore = 0;

    for (int vt = 0; vt < MVT.LAST_VALUETYPE; vt++) {
      for (MemIndexedMode im : MemIndexedMode.values()) {
        if (im != MemIndexedMode.UNINDEXED) {
          setIndexedLoadAction(im, vt, Expand);
          setIndexedStoreAction(im, vt, Expand);
        }
      }
      setOperationAction(ISD.FGETSIGN, vt, Expand);
    }
    setOperationAction(ISD.PREFETCH, MVT.Other, Expand);
    setOperationAction(ISD.ConstantFP, MVT.f32, Expand);
    setOperationAction(ISD.ConstantFP, MVT.f64, Expand);
    setOperationAction(ISD.ConstantFP, MVT.f80, Expand);

    // These library functions default to expand.
    setOperationAction(ISD.FLOG, MVT.f64, Expand);
    setOperationAction(ISD.FLOG2, MVT.f64, Expand);
    setOperationAction(ISD.FLOG10, MVT.f64, Expand);
    setOperationAction(ISD.FEXP, MVT.f64, Expand);
    setOperationAction(ISD.FEXP2, MVT.f64, Expand);
    setOperationAction(ISD.FLOG, MVT.f32, Expand);
    setOperationAction(ISD.FLOG2, MVT.f32, Expand);
    setOperationAction(ISD.FLOG10, MVT.f32, Expand);
    setOperationAction(ISD.FEXP, MVT.f32, Expand);
    setOperationAction(ISD.FEXP2, MVT.f32, Expand);

    // Default ISD.TRAP to expand (which turns it into abort).
    setOperationAction(ISD.TRAP, MVT.Other, Expand);

    isLittleEndian = td.isLittleEndian();
    usesGlobalOffsetTable = false;
    shiftAmountTy = pointerTy = MVT.getIntegerVT(8 * td.getPointerSize());
    maxStoresPerMemset = maxStoresPerMemcpy = maxStoresPerMemmove = 8;
    benefitFromCodePlacementOpt = false;
    useUnderscoreLongJmp = false;
    useUnderscoreSetJmp = false;
    selectIsExpensive = false;
    intDivIsCheap = false;
    pow2DivIsCheap = false;
    stackPointerRegisterToSaveRestore = 0;
    booleanContents = BooleanContent.UndefinedBooleanContent;

    initLibcallNames();
    initCmpLibcallCCs();
    initLibcallCallingConvs();

    TargetAsmInfo asmInfo = tm.getTargetAsmInfo();
    if (asmInfo == null || !asmInfo.hasDotLocAndDotFile())
      setOperationAction(ISD.DEBUG_LOC, MVT.Other, Expand);
  }

  public boolean isIntDivCheap() {
    return intDivIsCheap;
  }

  private void initLibcallNames() {
    libCallRoutineNames[RTLIB.SHL_I16.ordinal()] = "__ashlhi3";
    libCallRoutineNames[RTLIB.SHL_I32.ordinal()] = "__ashlsi3";
    libCallRoutineNames[RTLIB.SHL_I64.ordinal()] = "__ashldi3";
    libCallRoutineNames[RTLIB.SHL_I128.ordinal()] = "__ashlti3";
    libCallRoutineNames[RTLIB.SRL_I16.ordinal()] = "__lshrhi3";
    libCallRoutineNames[RTLIB.SRL_I32.ordinal()] = "__lshrsi3";
    libCallRoutineNames[RTLIB.SRL_I64.ordinal()] = "__lshrdi3";
    libCallRoutineNames[RTLIB.SRL_I128.ordinal()] = "__lshrti3";
    libCallRoutineNames[RTLIB.SRA_I16.ordinal()] = "__ashrhi3";
    libCallRoutineNames[RTLIB.SRA_I32.ordinal()] = "__ashrsi3";
    libCallRoutineNames[RTLIB.SRA_I64.ordinal()] = "__ashrdi3";
    libCallRoutineNames[RTLIB.SRA_I128.ordinal()] = "__ashrti3";
    libCallRoutineNames[RTLIB.MUL_I16.ordinal()] = "__mulhi3";
    libCallRoutineNames[RTLIB.MUL_I32.ordinal()] = "__mulsi3";
    libCallRoutineNames[RTLIB.MUL_I64.ordinal()] = "__muldi3";
    libCallRoutineNames[RTLIB.MUL_I128.ordinal()] = "__multi3";
    libCallRoutineNames[RTLIB.SDIV_I16.ordinal()] = "__divhi3";
    libCallRoutineNames[RTLIB.SDIV_I32.ordinal()] = "__divsi3";
    libCallRoutineNames[RTLIB.SDIV_I64.ordinal()] = "__divdi3";
    libCallRoutineNames[RTLIB.SDIV_I128.ordinal()] = "__divti3";
    libCallRoutineNames[RTLIB.UDIV_I16.ordinal()] = "__udivhi3";
    libCallRoutineNames[RTLIB.UDIV_I32.ordinal()] = "__udivsi3";
    libCallRoutineNames[RTLIB.UDIV_I64.ordinal()] = "__udivdi3";
    libCallRoutineNames[RTLIB.UDIV_I128.ordinal()] = "__udivti3";
    libCallRoutineNames[RTLIB.SREM_I16.ordinal()] = "__modhi3";
    libCallRoutineNames[RTLIB.SREM_I32.ordinal()] = "__modsi3";
    libCallRoutineNames[RTLIB.SREM_I64.ordinal()] = "__moddi3";
    libCallRoutineNames[RTLIB.SREM_I128.ordinal()] = "__modti3";
    libCallRoutineNames[RTLIB.UREM_I16.ordinal()] = "__umodhi3";
    libCallRoutineNames[RTLIB.UREM_I32.ordinal()] = "__umodsi3";
    libCallRoutineNames[RTLIB.UREM_I64.ordinal()] = "__umoddi3";
    libCallRoutineNames[RTLIB.UREM_I128.ordinal()] = "__umodti3";
    libCallRoutineNames[RTLIB.NEG_I32.ordinal()] = "__negsi2";
    libCallRoutineNames[RTLIB.NEG_I64.ordinal()] = "__negdi2";
    libCallRoutineNames[RTLIB.ADD_F32.ordinal()] = "__addsf3";
    libCallRoutineNames[RTLIB.ADD_F64.ordinal()] = "__adddf3";
    libCallRoutineNames[RTLIB.ADD_F80.ordinal()] = "__addxf3";
    libCallRoutineNames[RTLIB.ADD_PPCF128.ordinal()] = "__gcc_qadd";
    libCallRoutineNames[RTLIB.SUB_F32.ordinal()] = "__subsf3";
    libCallRoutineNames[RTLIB.SUB_F64.ordinal()] = "__subdf3";
    libCallRoutineNames[RTLIB.SUB_F80.ordinal()] = "__subxf3";
    libCallRoutineNames[RTLIB.SUB_PPCF128.ordinal()] = "__gcc_qsub";
    libCallRoutineNames[RTLIB.MUL_F32.ordinal()] = "__mulsf3";
    libCallRoutineNames[RTLIB.MUL_F64.ordinal()] = "__muldf3";
    libCallRoutineNames[RTLIB.MUL_F80.ordinal()] = "__mulxf3";
    libCallRoutineNames[RTLIB.MUL_PPCF128.ordinal()] = "__gcc_qmul";
    libCallRoutineNames[RTLIB.DIV_F32.ordinal()] = "__divsf3";
    libCallRoutineNames[RTLIB.DIV_F64.ordinal()] = "__divdf3";
    libCallRoutineNames[RTLIB.DIV_F80.ordinal()] = "__divxf3";
    libCallRoutineNames[RTLIB.DIV_PPCF128.ordinal()] = "__gcc_qdiv";
    libCallRoutineNames[RTLIB.REM_F32.ordinal()] = "fmodf";
    libCallRoutineNames[RTLIB.REM_F64.ordinal()] = "fmod";
    libCallRoutineNames[RTLIB.REM_F80.ordinal()] = "fmodl";
    libCallRoutineNames[RTLIB.REM_PPCF128.ordinal()] = "fmodl";
    libCallRoutineNames[RTLIB.POWI_F32.ordinal()] = "__powisf2";
    libCallRoutineNames[RTLIB.POWI_F64.ordinal()] = "__powidf2";
    libCallRoutineNames[RTLIB.POWI_F80.ordinal()] = "__powixf2";
    libCallRoutineNames[RTLIB.POWI_PPCF128.ordinal()] = "__powitf2";
    libCallRoutineNames[RTLIB.SQRT_F32.ordinal()] = "sqrtf";
    libCallRoutineNames[RTLIB.SQRT_F64.ordinal()] = "sqrt";
    libCallRoutineNames[RTLIB.SQRT_F80.ordinal()] = "sqrtl";
    libCallRoutineNames[RTLIB.SQRT_PPCF128.ordinal()] = "sqrtl";
    libCallRoutineNames[RTLIB.LOG_F32.ordinal()] = "logf";
    libCallRoutineNames[RTLIB.LOG_F64.ordinal()] = "log";
    libCallRoutineNames[RTLIB.LOG_F80.ordinal()] = "logl";
    libCallRoutineNames[RTLIB.LOG_PPCF128.ordinal()] = "logl";
    libCallRoutineNames[RTLIB.LOG2_F32.ordinal()] = "log2f";
    libCallRoutineNames[RTLIB.LOG2_F64.ordinal()] = "log2";
    libCallRoutineNames[RTLIB.LOG2_F80.ordinal()] = "log2l";
    libCallRoutineNames[RTLIB.LOG2_PPCF128.ordinal()] = "log2l";
    libCallRoutineNames[RTLIB.LOG10_F32.ordinal()] = "log10f";
    libCallRoutineNames[RTLIB.LOG10_F64.ordinal()] = "log10";
    libCallRoutineNames[RTLIB.LOG10_F80.ordinal()] = "log10l";
    libCallRoutineNames[RTLIB.LOG10_PPCF128.ordinal()] = "log10l";
    libCallRoutineNames[RTLIB.EXP_F32.ordinal()] = "expf";
    libCallRoutineNames[RTLIB.EXP_F64.ordinal()] = "exp";
    libCallRoutineNames[RTLIB.EXP_F80.ordinal()] = "expl";
    libCallRoutineNames[RTLIB.EXP_PPCF128.ordinal()] = "expl";
    libCallRoutineNames[RTLIB.EXP2_F32.ordinal()] = "exp2f";
    libCallRoutineNames[RTLIB.EXP2_F64.ordinal()] = "exp2";
    libCallRoutineNames[RTLIB.EXP2_F80.ordinal()] = "exp2l";
    libCallRoutineNames[RTLIB.EXP2_PPCF128.ordinal()] = "exp2l";
    libCallRoutineNames[RTLIB.SIN_F32.ordinal()] = "sinf";
    libCallRoutineNames[RTLIB.SIN_F64.ordinal()] = "sin";
    libCallRoutineNames[RTLIB.SIN_F80.ordinal()] = "sinl";
    libCallRoutineNames[RTLIB.SIN_PPCF128.ordinal()] = "sinl";
    libCallRoutineNames[RTLIB.COS_F32.ordinal()] = "cosf";
    libCallRoutineNames[RTLIB.COS_F64.ordinal()] = "cos";
    libCallRoutineNames[RTLIB.COS_F80.ordinal()] = "cosl";
    libCallRoutineNames[RTLIB.COS_PPCF128.ordinal()] = "cosl";
    libCallRoutineNames[RTLIB.POW_F32.ordinal()] = "powf";
    libCallRoutineNames[RTLIB.POW_F64.ordinal()] = "pow";
    libCallRoutineNames[RTLIB.POW_F80.ordinal()] = "powl";
    libCallRoutineNames[RTLIB.POW_PPCF128.ordinal()] = "powl";
    libCallRoutineNames[RTLIB.CEIL_F32.ordinal()] = "ceilf";
    libCallRoutineNames[RTLIB.CEIL_F64.ordinal()] = "ceil";
    libCallRoutineNames[RTLIB.CEIL_F80.ordinal()] = "ceill";
    libCallRoutineNames[RTLIB.CEIL_PPCF128.ordinal()] = "ceill";
    libCallRoutineNames[RTLIB.TRUNC_F32.ordinal()] = "truncf";
    libCallRoutineNames[RTLIB.TRUNC_F64.ordinal()] = "trunc";
    libCallRoutineNames[RTLIB.TRUNC_F80.ordinal()] = "truncl";
    libCallRoutineNames[RTLIB.TRUNC_PPCF128.ordinal()] = "truncl";
    libCallRoutineNames[RTLIB.RINT_F32.ordinal()] = "rintf";
    libCallRoutineNames[RTLIB.RINT_F64.ordinal()] = "rint";
    libCallRoutineNames[RTLIB.RINT_F80.ordinal()] = "rintl";
    libCallRoutineNames[RTLIB.RINT_PPCF128.ordinal()] = "rintl";
    libCallRoutineNames[RTLIB.NEARBYINT_F32.ordinal()] = "nearbyintf";
    libCallRoutineNames[RTLIB.NEARBYINT_F64.ordinal()] = "nearbyint";
    libCallRoutineNames[RTLIB.NEARBYINT_F80.ordinal()] = "nearbyintl";
    libCallRoutineNames[RTLIB.NEARBYINT_PPCF128.ordinal()] = "nearbyintl";
    libCallRoutineNames[RTLIB.FLOOR_F32.ordinal()] = "floorf";
    libCallRoutineNames[RTLIB.FLOOR_F64.ordinal()] = "floor";
    libCallRoutineNames[RTLIB.FLOOR_F80.ordinal()] = "floorl";
    libCallRoutineNames[RTLIB.FLOOR_PPCF128.ordinal()] = "floorl";
    libCallRoutineNames[RTLIB.FPEXT_F32_F64.ordinal()] = "__extendsfdf2";
    libCallRoutineNames[RTLIB.FPROUND_F64_F32.ordinal()] = "__truncdfsf2";
    libCallRoutineNames[RTLIB.FPROUND_F80_F32.ordinal()] = "__truncxfsf2";
    libCallRoutineNames[RTLIB.FPROUND_PPCF128_F32.ordinal()] = "__trunctfsf2";
    libCallRoutineNames[RTLIB.FPROUND_F80_F64.ordinal()] = "__truncxfdf2";
    libCallRoutineNames[RTLIB.FPROUND_PPCF128_F64.ordinal()] = "__trunctfdf2";
    libCallRoutineNames[RTLIB.FPTOSINT_F32_I8.ordinal()] = "__fixsfi8";
    libCallRoutineNames[RTLIB.FPTOSINT_F32_I16.ordinal()] = "__fixsfi16";
    libCallRoutineNames[RTLIB.FPTOSINT_F32_I32.ordinal()] = "__fixsfsi";
    libCallRoutineNames[RTLIB.FPTOSINT_F32_I64.ordinal()] = "__fixsfdi";
    libCallRoutineNames[RTLIB.FPTOSINT_F32_I128.ordinal()] = "__fixsfti";
    libCallRoutineNames[RTLIB.FPTOSINT_F64_I32.ordinal()] = "__fixdfsi";
    libCallRoutineNames[RTLIB.FPTOSINT_F64_I64.ordinal()] = "__fixdfdi";
    libCallRoutineNames[RTLIB.FPTOSINT_F64_I128.ordinal()] = "__fixdfti";
    libCallRoutineNames[RTLIB.FPTOSINT_F80_I32.ordinal()] = "__fixxfsi";
    libCallRoutineNames[RTLIB.FPTOSINT_F80_I64.ordinal()] = "__fixxfdi";
    libCallRoutineNames[RTLIB.FPTOSINT_F80_I128.ordinal()] = "__fixxfti";
    libCallRoutineNames[RTLIB.FPTOSINT_PPCF128_I32.ordinal()] = "__fixtfsi";
    libCallRoutineNames[RTLIB.FPTOSINT_PPCF128_I64.ordinal()] = "__fixtfdi";
    libCallRoutineNames[RTLIB.FPTOSINT_PPCF128_I128.ordinal()] = "__fixtfti";
    libCallRoutineNames[RTLIB.FPTOUINT_F32_I8.ordinal()] = "__fixunssfi8";
    libCallRoutineNames[RTLIB.FPTOUINT_F32_I16.ordinal()] = "__fixunssfi16";
    libCallRoutineNames[RTLIB.FPTOUINT_F32_I32.ordinal()] = "__fixunssfsi";
    libCallRoutineNames[RTLIB.FPTOUINT_F32_I64.ordinal()] = "__fixunssfdi";
    libCallRoutineNames[RTLIB.FPTOUINT_F32_I128.ordinal()] = "__fixunssfti";
    libCallRoutineNames[RTLIB.FPTOUINT_F64_I32.ordinal()] = "__fixunsdfsi";
    libCallRoutineNames[RTLIB.FPTOUINT_F64_I64.ordinal()] = "__fixunsdfdi";
    libCallRoutineNames[RTLIB.FPTOUINT_F64_I128.ordinal()] = "__fixunsdfti";
    libCallRoutineNames[RTLIB.FPTOUINT_F80_I32.ordinal()] = "__fixunsxfsi";
    libCallRoutineNames[RTLIB.FPTOUINT_F80_I64.ordinal()] = "__fixunsxfdi";
    libCallRoutineNames[RTLIB.FPTOUINT_F80_I128.ordinal()] = "__fixunsxfti";
    libCallRoutineNames[RTLIB.FPTOUINT_PPCF128_I32.ordinal()] = "__fixunstfsi";
    libCallRoutineNames[RTLIB.FPTOUINT_PPCF128_I64.ordinal()] = "__fixunstfdi";
    libCallRoutineNames[RTLIB.FPTOUINT_PPCF128_I128.ordinal()] = "__fixunstfti";
    libCallRoutineNames[RTLIB.SINTTOFP_I32_F32.ordinal()] = "__floatsisf";
    libCallRoutineNames[RTLIB.SINTTOFP_I32_F64.ordinal()] = "__floatsidf";
    libCallRoutineNames[RTLIB.SINTTOFP_I32_F80.ordinal()] = "__floatsixf";
    libCallRoutineNames[RTLIB.SINTTOFP_I32_PPCF128.ordinal()] = "__floatsitf";
    libCallRoutineNames[RTLIB.SINTTOFP_I64_F32.ordinal()] = "__floatdisf";
    libCallRoutineNames[RTLIB.SINTTOFP_I64_F64.ordinal()] = "__floatdidf";
    libCallRoutineNames[RTLIB.SINTTOFP_I64_F80.ordinal()] = "__floatdixf";
    libCallRoutineNames[RTLIB.SINTTOFP_I64_PPCF128.ordinal()] = "__floatditf";
    libCallRoutineNames[RTLIB.SINTTOFP_I128_F32.ordinal()] = "__floattisf";
    libCallRoutineNames[RTLIB.SINTTOFP_I128_F64.ordinal()] = "__floattidf";
    libCallRoutineNames[RTLIB.SINTTOFP_I128_F80.ordinal()] = "__floattixf";
    libCallRoutineNames[RTLIB.SINTTOFP_I128_PPCF128.ordinal()] = "__floattitf";
    libCallRoutineNames[RTLIB.UINTTOFP_I32_F32.ordinal()] = "__floatunsisf";
    libCallRoutineNames[RTLIB.UINTTOFP_I32_F64.ordinal()] = "__floatunsidf";
    libCallRoutineNames[RTLIB.UINTTOFP_I32_F80.ordinal()] = "__floatunsixf";
    libCallRoutineNames[RTLIB.UINTTOFP_I32_PPCF128.ordinal()] = "__floatunsitf";
    libCallRoutineNames[RTLIB.UINTTOFP_I64_F32.ordinal()] = "__floatundisf";
    libCallRoutineNames[RTLIB.UINTTOFP_I64_F64.ordinal()] = "__floatundidf";
    libCallRoutineNames[RTLIB.UINTTOFP_I64_F80.ordinal()] = "__floatundixf";
    libCallRoutineNames[RTLIB.UINTTOFP_I64_PPCF128.ordinal()] = "__floatunditf";
    libCallRoutineNames[RTLIB.UINTTOFP_I128_F32.ordinal()] = "__floatuntisf";
    libCallRoutineNames[RTLIB.UINTTOFP_I128_F64.ordinal()] = "__floatuntidf";
    libCallRoutineNames[RTLIB.UINTTOFP_I128_F80.ordinal()] = "__floatuntixf";
    libCallRoutineNames[RTLIB.UINTTOFP_I128_PPCF128.ordinal()] = "__floatuntitf";
    libCallRoutineNames[RTLIB.OEQ_F32.ordinal()] = "__eqsf2";
    libCallRoutineNames[RTLIB.OEQ_F64.ordinal()] = "__eqdf2";
    libCallRoutineNames[RTLIB.UNE_F32.ordinal()] = "__nesf2";
    libCallRoutineNames[RTLIB.UNE_F64.ordinal()] = "__nedf2";
    libCallRoutineNames[RTLIB.OGE_F32.ordinal()] = "__gesf2";
    libCallRoutineNames[RTLIB.OGE_F64.ordinal()] = "__gedf2";
    libCallRoutineNames[RTLIB.OLT_F32.ordinal()] = "__ltsf2";
    libCallRoutineNames[RTLIB.OLT_F64.ordinal()] = "__ltdf2";
    libCallRoutineNames[RTLIB.OLE_F32.ordinal()] = "__lesf2";
    libCallRoutineNames[RTLIB.OLE_F64.ordinal()] = "__ledf2";
    libCallRoutineNames[RTLIB.OGT_F32.ordinal()] = "__gtsf2";
    libCallRoutineNames[RTLIB.OGT_F64.ordinal()] = "__gtdf2";
    libCallRoutineNames[RTLIB.UO_F32.ordinal()] = "__unordsf2";
    libCallRoutineNames[RTLIB.UO_F64.ordinal()] = "__unorddf2";
    libCallRoutineNames[RTLIB.O_F32.ordinal()] = "__unordsf2";
    libCallRoutineNames[RTLIB.O_F64.ordinal()] = "__unorddf2";
    libCallRoutineNames[RTLIB.MEMCPY.ordinal()] = "memcpy";
    libCallRoutineNames[RTLIB.MEMMOVE.ordinal()] = "memmove";
    libCallRoutineNames[RTLIB.MEMSET.ordinal()] = "memset";
    libCallRoutineNames[RTLIB.UNWIND_RESUME.ordinal()] = "_Unwind_Resume";
  }

  private void initCmpLibcallCCs() {
    Arrays.fill(cmpLibCallCCs, SETCC_INVALID);
    cmpLibCallCCs[OEQ_F32.ordinal()] = SETEQ;
    cmpLibCallCCs[OEQ_F32.ordinal()] = SETEQ;
    cmpLibCallCCs[OEQ_F64.ordinal()] = SETEQ;
    cmpLibCallCCs[UNE_F32.ordinal()] = SETNE;
    cmpLibCallCCs[UNE_F64.ordinal()] = SETNE;
    cmpLibCallCCs[OGE_F32.ordinal()] = SETGE;
    cmpLibCallCCs[OGE_F64.ordinal()] = SETGE;
    cmpLibCallCCs[OLT_F32.ordinal()] = SETLT;
    cmpLibCallCCs[OLT_F64.ordinal()] = SETLT;
    cmpLibCallCCs[OLE_F32.ordinal()] = SETLE;
    cmpLibCallCCs[OLE_F64.ordinal()] = SETLE;
    cmpLibCallCCs[OGT_F32.ordinal()] = SETGT;
    cmpLibCallCCs[OGT_F64.ordinal()] = SETGT;
    cmpLibCallCCs[UO_F32.ordinal()] = SETNE;
    cmpLibCallCCs[UO_F64.ordinal()] = SETNE;
    cmpLibCallCCs[O_F32.ordinal()] = SETEQ;
    cmpLibCallCCs[O_F64.ordinal()] = SETEQ;

  }

  private void initLibcallCallingConvs() {
    for (int i = 0; i < RTLIB.UNKNOWN_LIBCALL.ordinal(); i++)
      libCallCallingConv[i] = CallingConv.C;
  }

  public void setOperationAction(int opc, int vt, LegalizeAction action) {
    int i = vt;
    int j = i & 31;
    i = i >> 5;
    opActions[i][opc] &= ~(3L << (j * 2));
    opActions[i][opc] |= (long) action.ordinal() << (j * 2);
  }

  public void setLoadExtActions(MemIndexedMode im, int vt, LegalizeAction action) {
    Util.assertion(vt < 64 * 4 && im.ordinal() < loadExtActions.length, "Table isn't big enough!");
    loadExtActions[im.ordinal()] &= ~(3L << (vt * 2));
    loadExtActions[im.ordinal()] |= action.ordinal() << (vt * 2);
  }

  public void setIndexedLoadAction(MemIndexedMode im, int vt, LegalizeAction action) {
    Util.assertion(vt < MVT.LAST_VALUETYPE && im.ordinal() < indexedModeActions[0][0].length, "Table isn't big enough!");

    indexedModeActions[vt][0][im.ordinal()] = action.ordinal();
  }

  public void setIndexedStoreAction(MemIndexedMode im, int vt, LegalizeAction action) {
    Util.assertion(vt < MVT.LAST_VALUETYPE && im.ordinal() < indexedModeActions[0][1].length, "Table isn't big enough!");

    indexedModeActions[vt][1][im.ordinal()] = action.ordinal();
  }

  public ValueTypeAction getValueTypeActions() {
    return valueTypeAction;
  }

  public BooleanContent getBooleanContents() {
    return booleanContents;
  }

  public void setBooleanContents(BooleanContent cnt) {
    this.booleanContents = cnt;
  }

  public TargetData getTargetData() {
    return td;
  }

  public TargetMachine getTargetMachine() {
    return tm;
  }

  public boolean isBigEndian() {
    return !isLittleEndian;
  }

  public boolean isLittleEndian() {
    return isLittleEndian;
  }

  public int getStackPointerRegisterToSaveRestore() {
    return stackPointerRegisterToSaveRestore;
  }

  public void setStackPointerRegisterToSaveRestore(int spreg) {
    stackPointerRegisterToSaveRestore = spreg;
  }

  public void setExceptionPointerRegister(int reg) {
    this.exceptionPointerRegister = reg;
  }

  public int getExceptionPointerRegister() {
    return exceptionPointerRegister;
  }

  public void setExceptionSelectorRegister(int reg) {
    this.exceptionSelectorRegister = reg;
  }

  public int getExceptionSelectorRegister() {
    return exceptionSelectorRegister;
  }

  public void setUseUnderscoreSetJmp(boolean val) {
    this.useUnderscoreSetJmp = val;
  }

  public void setUseUnderscoreLongJmp(boolean val) {
    this.useUnderscoreLongJmp = val;
  }

  public void setLoadExtAction(LoadExtType extType, MVT vt, LegalizeAction action) {
    Util.assertion(vt.simpleVT < 64 * 4 && extType.ordinal() < loadExtActions.length, "Table isn't big enough!");

    loadExtActions[extType.ordinal()] &= ~(3L << vt.simpleVT * 2);
    loadExtActions[extType.ordinal()] |= ((long) action.ordinal() << vt.simpleVT * 2);
  }

  /**
   * Create a detail info about machine function.
   *
   * @param mf
   * @return
   */
  public abstract MachineFunctionInfo createMachineFunctionInfo(
      MachineFunction mf);

  public EVT getValueType(Type type) {
    return getValueType(type, false);
  }

  /**
   * eturn the EVT corresponding to this LLVM type.
   * This is fixed by the LLVM operations except for the pointer size.  If
   * AllowUnknown is true, this will return MVT.Other for types with no EVT
   * counterpart (e.g. structs), otherwise it will Util.assertion(.     *
   *
   * @param type
   * @param allowUnknown
   * @return
   */
  public EVT getValueType(Type type, boolean allowUnknown) {
    EVT vt = EVT.getEVT(type, allowUnknown);
    return vt.equals(new EVT(new MVT(MVT.iPTR))) ? new EVT(pointerTy) : vt;
  }

    /*
    public abstract FastISel createFastISel(MachineFunction mf,
            MachineModuleInfo mmi,
            TObjectIntHashMap vm,
            HashMap<BasicBlock, MachineBasicBlock> bm,
            TObjectIntHashMap<Instruction.AllocaInst> am);
    */

  /**
   * Return true if the target has native support for the
   * specified value type.  This means that it has a register that directly
   * holds it without promotions or expansions.
   *
   * @param vt
   * @return
   */
  public boolean isTypeLegal(EVT vt) {
    Util.assertion(!vt.isSimple() || vt.getSimpleVT().simpleVT < registerClassForVT.length);
    return vt.isSimple() && registerClassForVT[vt.getSimpleVT().simpleVT] != null;
  }

  public EVT getTypeToTransformTo(EVT vt) {
    if (vt.isSimple()) {
      Util.assertion(vt.getSimpleVT().simpleVT < transformToType.length);
      EVT nvt = transformToType[vt.getSimpleVT().simpleVT];
      Util.assertion(getTypeAction(nvt) != Promote, "Promote may not follow expand or promote");

      return nvt;
    }

    if (vt.isVector()) {
      EVT nvt = vt.getPow2VectorType();
      if (nvt.equals(vt)) {
        // Vector length is a power of 2, split to half the size.
        int numElts = vt.getVectorNumElements();
        EVT eltVT = vt.getVectorElementType();
        return numElts == 1 ? eltVT : EVT.getVectorVT(eltVT, numElts / 2);
      }

      // Promote to a power of two size, avoiding multi-step promotions.
      return getTypeAction(nvt) == Promote ?
          getTypeToTransformTo(nvt) :
          nvt;
    } else if (vt.isInteger()) {
      EVT nvt = vt.getRoundIntegerType();
      if (nvt.equals(vt)) {
        // Size is a power of two, expand to half the size.
        return EVT.getIntegerVT(vt.getSizeInBits() / 2);
      } else {
        // Promote to a power of two size, avoiding multi-step promotion.
        return getTypeAction(nvt) == Promote ?
            getTypeToTransformTo(nvt) :
            nvt;
      }
    }
    Util.assertion(false, "Unsupported extended type!");
    return new EVT(new MVT(MVT.Other));
  }

  public EVT getTypeToExpandTo(EVT vt) {
    Util.assertion(!vt.isVector());
    while (true) {
      switch (getTypeAction(vt)) {
        case Legal:
          return vt;
        case Expand:
          vt = getTypeToTransformTo(vt);
          break;
        default:
          Util.assertion(false, "Type is illegal or to be expanded!");
          return vt;
      }
    }
  }

  /**
   * Return how we should legalize values of this type, either
   * it is already legal (return 'Legal') or we need to promote it to a larger
   * × type (return 'Promote'), or we need to expand it into multiple registers
   * × of smaller integer type (return 'Expand').  'Custom' is not an option.
   *
   * @param nvt
   * @return
   */
  private LegalizeAction getTypeAction(EVT nvt) {
    return valueTypeAction.getTypeAction(nvt);
  }

  public TargetRegisterClass getRegClassFor(EVT vt) {
    Util.assertion(vt.isSimple(), "getRegClassFor called on illegal type!");
    TargetRegisterClass rc = registerClassForVT[vt.getSimpleVT().simpleVT];
    Util.assertion(rc != null, "This value type is not natively supported!");
    return rc;
  }

  public MVT getPointerTy() {
    return pointerTy;
  }

  /**
   * Add the specified register class as an available
   * /// regclass for the specified value type.  This indicates the selector can
   * /// handle values of that class natively.
   *
   * @param vt
   */
  public void addRegisterClass(int vt, TargetRegisterClass regClass) {
    Util.assertion(vt < registerClassForVT.length);
    availableRegClasses.add(Pair.get(new EVT(vt), regClass));
    registerClassForVT[vt] = regClass;
  }

  public int getVectorTypeBreakdown(EVT vt,
                                    OutRef<EVT> intermediateVT,
                                    OutRef<Integer> numIntermediates,
                                    OutRef<EVT> registerVT) {
    int numElts = vt.getVectorNumElements();
    EVT eltTy = vt.getVectorElementType();

    int numVectorRegs = 1;
    if (!Util.isPowerOf2(numElts)) {
      numVectorRegs = numElts;
      numElts = 1;
    }

    while (numElts > 1 && !isTypeLegal(
        new EVT(MVT.getVectorVT(eltTy.getSimpleVT(), numElts)))) {
      numElts >>>= 1;
      numVectorRegs <<= 1;
    }

    numIntermediates.set(numVectorRegs);
    EVT newVT = new EVT(MVT.getVectorVT(eltTy.getSimpleVT(), numElts));
    if (!isTypeLegal(newVT)) {
      newVT = eltTy;
    }
    intermediateVT.set(newVT);

    EVT destVT = getRegisterType(newVT);
    registerVT.set(destVT);
    if (destVT.bitsLT(newVT)) {
      return numVectorRegs * (newVT.getSizeInBits() / destVT.getSizeInBits());
    } else {
      // Otherwise, promotion or legal types use the same number of registers as
      // the vector decimated to the appropriate level.
      return numVectorRegs;
    }
  }

  public EVT getRegisterType(EVT valueVT) {
    if (valueVT.isSimple()) {
      Util.assertion(valueVT.getSimpleVT().simpleVT < registerTypeForVT.length);
      return registerTypeForVT[valueVT.getSimpleVT().simpleVT];
    }
    if (valueVT.isVector()) {
      OutRef<EVT> registerVT = new OutRef<>();
      getVectorTypeBreakdown(valueVT, new OutRef<>(),
          new OutRef<>(), registerVT);
      return registerVT.get();
    }
    if (valueVT.isInteger()) {
      return getRegisterType(getTypeToTransformTo(valueVT));
    }
    Util.assertion(false, "Unsupported extended type!");
    return new EVT(MVT.Other);
  }

  public int getNumRegisters(EVT valueVT) {
    Util.assertion(valueVT.getSimpleVT().simpleVT < numRegistersForVT.length);
    return numRegistersForVT[valueVT.getSimpleVT().simpleVT];
  }

  private static int getVectorTypeBreakdownMVT(MVT vt,
                                               OutRef<MVT> intermediateVT,
                                               OutRef<Integer> numIntermediates,
                                               OutRef<EVT> registerVT, TargetLowering tli) {
    int numElts = vt.getVectorNumElements();
    MVT eltTy = vt.getVectorElementType();

    int numVectorRegs = 1;
    if (!Util.isPowerOf2(numElts)) {
      numVectorRegs = numElts;
      numElts = 1;
    }

    while (numElts > 1 && !tli
        .isTypeLegal(new EVT(MVT.getVectorVT(eltTy, numElts)))) {
      numElts >>>= 1;
      numVectorRegs <<= 1;
    }

    numIntermediates.set(numVectorRegs);
    MVT newVT = MVT.getVectorVT(eltTy, numElts);
    if (!tli.isTypeLegal(new EVT(newVT))) {
      newVT = eltTy;
    }
    intermediateVT.set(newVT);

    EVT destVT = tli.getRegisterType(new EVT(newVT));
    registerVT.set(destVT);
    if (destVT.bitsLT(new EVT(newVT))) {
      return numVectorRegs * (newVT.getSizeInBits() / destVT.getSizeInBits());
    } else {
      // Otherwise, promotion or legal types use the same number of registers as
      // the vector decimated to the appropriate level.
      return numVectorRegs;
    }
  }

  /**
   * Once all of the register classes are added. this allow use to compute
   * derived properties we expose.
   */
  public void computeRegisterProperties() {
    // Everything defaults to needing one register.
    for (int i = 0; i < MVT.LAST_VALUETYPE; ++i) {
      numRegistersForVT[i] = 1;
      registerTypeForVT[i] = transformToType[i] = new EVT(new MVT(i));
    }

    // Except for isVoid, which doesn't need any registers.
    numRegistersForVT[MVT.isVoid] = 0;

    // Find the largest integer register class.
    int largetestIntReg = MVT.LAST_INTEGER_VALUETYPE;
    for (; registerClassForVT[largetestIntReg] == null; --largetestIntReg)
      Util.assertion(largetestIntReg != MVT.i1, "No integer register defined");

    // Every integer value type larger than this largest register takes twice as
    // many registers to represent as the previous ValueType.
    for (int expandedReg = largetestIntReg + 1; ; ++expandedReg) {
      EVT vt = new EVT(new MVT(expandedReg));
      if (!vt.isInteger())
        break;

      numRegistersForVT[expandedReg] =
          2 * numRegistersForVT[expandedReg - 1];
      registerTypeForVT[expandedReg] = new EVT(new MVT(largetestIntReg));
      transformToType[expandedReg] = new EVT(new MVT(expandedReg - 1));
      valueTypeAction.setTypeAction(vt, Expand);
    }

    // Inspect all of the ValueType's smaller than the largest integer
    // register to see which ones need promotion.
    int legalIntReg = largetestIntReg;
    for (int intReg = largetestIntReg - 1; intReg >= MVT.i1; --intReg) {
      EVT ivt = new EVT(new MVT(intReg));
      if (isTypeLegal(ivt)) {
        legalIntReg = intReg;
      } else {
        registerTypeForVT[intReg] = transformToType[intReg] = new EVT(
            new MVT(legalIntReg));
        valueTypeAction.setTypeAction(ivt, Promote);
      }
    }

    // ppcf128 type is really two f64's.
    if (!isTypeLegal(new EVT(new MVT(MVT.ppcf128)))) {
      numRegistersForVT[MVT.ppcf128] = 2 * numRegistersForVT[MVT.f64];
      registerTypeForVT[MVT.ppcf128] = transformToType[MVT.ppcf128] = new EVT(
          new MVT(MVT.f64));
      valueTypeAction.setTypeAction(new EVT(MVT.ppcf128), Expand);
    }

    // Decide how to handle f64. If the target does not have native f64 support,
    // expand it to i64 and we will be generating soft float library calls.
    if (!isTypeLegal(new EVT(new MVT(MVT.f64)))) {
      numRegistersForVT[MVT.i64] = numRegistersForVT[MVT.i64];
      registerTypeForVT[MVT.i64] = transformToType[MVT.i64] = new EVT(new MVT(MVT.i64));
      valueTypeAction.setTypeAction(new EVT(MVT.f64), Expand);
    }

    // Decide how to handle f32. If the target does not have native support for
    // f32, promote it to f64 if it is legal. Otherwise, expand it to i32.
    if (!isTypeLegal(new EVT(new MVT(MVT.f32)))) {
      if (isTypeLegal(new EVT(new MVT(MVT.f64)))) {
        numRegistersForVT[MVT.f32] = numRegistersForVT[MVT.f64];
        registerTypeForVT[MVT.f32] = registerTypeForVT[MVT.f64];
        transformToType[MVT.f32] = new EVT(new MVT(MVT.f64));
        valueTypeAction.setTypeAction(new EVT(MVT.f32), Promote);
      } else {
        numRegistersForVT[MVT.f32] = numRegistersForVT[MVT.i32];
        registerTypeForVT[MVT.f32] = registerTypeForVT[MVT.i32];
        transformToType[MVT.f32] = new EVT(new MVT(MVT.i32));
        valueTypeAction.setTypeAction(new EVT(MVT.f32), Expand);
      }
    }

    // Loop over all of the vector value types to see which need transformations.
    for (int i = MVT.FIRST_INTEGER_VECTOR_VALUETYPE; i <= MVT.LAST_INTEGER_VECTOR_VALUETYPE; i++) {
      MVT vt = new MVT(i);
      if (!isTypeLegal(new EVT(vt))) {
        OutRef<MVT> intermediateVT = new OutRef<>(new MVT());
        OutRef<EVT> registerVT = new OutRef<>(new EVT());
        OutRef<Integer> numIntermediates = new OutRef<>(
            0);

        numRegistersForVT[i] = getVectorTypeBreakdownMVT(vt,
            intermediateVT, numIntermediates, registerVT, this);
        registerTypeForVT[i] = registerVT.get();

        boolean isLegalWiderType = false;
        EVT elTvt = new EVT(vt.getVectorElementType());
        int numElts = vt.getVectorNumElements();
        for (int nvt = i + 1; nvt <= MVT.LAST_INTEGER_VECTOR_VALUETYPE; ++nvt) {
          EVT svt = new EVT(new MVT(nvt));
          if (isTypeLegal(svt) && svt.getVectorElementType().equals(elTvt)
              && svt.getVectorNumElements() > numElts) {
            transformToType[i] = svt;
            valueTypeAction.setTypeAction(new EVT(vt), Promote);
            isLegalWiderType = true;
            break;
          }
        }
        if (!isLegalWiderType) {
          EVT nvt = new EVT(vt.getPower2VectorType());
          if (nvt.equals(new EVT(vt))) {
            // Type is already a power of 2.  The default action is to split.
            transformToType[i] = new EVT(new MVT(MVT.Other));
            valueTypeAction.setTypeAction(new EVT(vt), Expand);
          } else {
            transformToType[i] = nvt;
            valueTypeAction.setTypeAction(new EVT(vt), Promote);
          }
        }
      }
    }
  }

  /**
   * Computing the assembly alignment for specified function with target-specific
   * method.
   *
   * @param fn
   * @return
   */
  public abstract int getFunctionAlignment(Function fn);

  public MVT getShiftAmountTy() {
    return shiftAmountTy;
  }

  public void setShiftAmountTy(MVT vt) {
    shiftAmountTy = vt;
  }

  public String getTargetNodeName(int opcode) {
    Util.shouldNotReachHere("This method should be implemented by Target!");
    return null;
  }

  public abstract SDValue lowerFormalArguments(SDValue root,
                                               CallingConv callingConv,
                                               boolean varArg,
                                               ArrayList<InputArg> ins,
                                               SelectionDAG dag,
                                               ArrayList<SDValue> inVals);

  public abstract SDValue lowerMemArgument(SDValue chain,
                                           CallingConv cc,
                                           ArrayList<InputArg> argInfo,
                                           SelectionDAG dag,
                                           CCValAssign va,
                                           MachineFrameInfo mfi,
                                           int i);

  public void computeMaskedBitsForTargetNode(SDValue op,
                                             APInt mask,
                                             APInt[] knownVals,
                                             SelectionDAG selectionDAG,
                                             int depth) {
    int opc = op.getOpcode();
    Util.assertion(opc >= ISD.BUILTIN_OP_END ||
        opc == ISD.INTRINSIC_VOID ||
        opc == ISD.INTRINSIC_W_CHAIN ||
        opc == ISD.INTRINSIC_WO_CHAIN);

    knownVals[0] = knownVals[1] = new APInt(mask.getBitWidth(), 0);
  }

  public abstract SDValue lowerReturn(SDValue chain,
                                      CallingConv cc,
                                      boolean isVarArg,
                                      ArrayList<OutputArg> outs,
                                      SelectionDAG dag);

  public Pair<SDValue, SDValue> lowerCallTo(SDValue chain,
                                            Type retTy,
                                            boolean retSExt,
                                            boolean retZExt,
                                            boolean varArg,
                                            boolean isInReg,
                                            int numFixedArgs,
                                            CallingConv cc,
                                            boolean isTailCall,
                                            boolean isReturnValueUsed,
                                            SDValue callee,
                                            ArrayList<ArgListEntry> args,
                                            SelectionDAG dag) {
    Util.assertion(!isTailCall || EnablePerformTailCallOpt.value);
    ArrayList<OutputArg> outs = new ArrayList<>(32);
    for (int i = 0, e = args.size(); i < e; i++) {
      ArrayList<EVT> valueVTs = new ArrayList<>();
      computeValueVTs(this, args.get(i).ty, valueVTs);
      for (int j = 0, sz = valueVTs.size(); j < sz; j++) {
        EVT vt = valueVTs.get(j);
        Type argTy = vt.getTypeForEVT();
        SDValue op = new SDValue(args.get(i).node.getNode(),
            args.get(i).node.getResNo() + j);

        ArgFlagsTy flags = new ArgFlagsTy();
        int originalAlignment = getTargetData().getABITypeAlignment(argTy);

        if (args.get(i).isZExt)
          flags.setZExt();
        if (args.get(i).isSRet)
          flags.setSExt();
        if (args.get(i).isInReg)
          flags.setInReg();
        if (args.get(i).isByVal) {
          flags.setByVal();
          PointerType pty = (PointerType) args.get(i).ty;
          Type eltTy = pty.getElementType();
          int frameAlign = getByValTypeAlignment(eltTy);
          long frameSize = getTargetData().getTypeAllocSize(eltTy);

          if (args.get(i).alignment != 0)
            frameAlign = args.get(i).alignment;
          flags.setByValAlign(frameAlign);
          flags.setByValSize((int) frameSize);
        }
        if (args.get(i).isSRet)
          flags.setSRet();

        flags.setOrigAlign(originalAlignment);

        EVT partVT = getRegisterType(vt);
        int numParts = getNumRegisters(vt);
        SDValue[] parts = new SDValue[numParts];
        int extendKind = ISD.ANY_EXTEND;

        if (args.get(i).isSExt)
          extendKind = ISD.SIGN_EXTEND;
        else if (args.get(i).isZExt)
          extendKind = ISD.ZERO_EXTEND;

        getCopyToParts(dag, op, parts, partVT, extendKind);

        for (int k = 0; k < numParts; k++) {
          OutputArg outFlags = new OutputArg(flags, parts[k], i < numFixedArgs);
          if (numParts > 1 && k == 0)
            outFlags.flags.setSplit();
          else if (k != 0)
            outFlags.flags.setOrigAlign(1);

          outs.add(outFlags);
        }
      }
    }

    ArrayList<InputArg> ins = new ArrayList<>(32);
    ArrayList<EVT> retTys = new ArrayList<>(4);

    computeValueVTs(this, retTy, retTys);
    for (EVT vt : retTys) {
      EVT registerVT = getRegisterType(vt);
      int numRegs = getNumRegisters(vt);
      for (int i = 0; i < numRegs; i++) {
        InputArg input = new InputArg();
        input.vt = registerVT;
        input.used = isReturnValueUsed;
        if (retSExt)
          input.flags.setSExt();
        if (retZExt)
          input.flags.setZExt();
        if (isInReg)
          input.flags.setInReg();
        ins.add(input);
      }
    }

    if (isTailCall && !isEligibleTailCallOptimization(callee, cc, varArg,
        ins, dag))
      isTailCall = false;

    ArrayList<SDValue> inVals = new ArrayList<>();
    chain = lowerCall(chain, callee, cc, varArg, isTailCall, outs, ins, dag,
        inVals);

    if (isTailCall) {
      dag.setRoot(chain);
      return Pair.get(new SDValue(), new SDValue());
    }

    int assertOp = ISD.DELETED_NODE;
    if (retSExt)
      assertOp = ISD.AssertSext;
    else if (retZExt)
      assertOp = ISD.AssertZext;

    ArrayList<SDValue> returnValues = new ArrayList<>();
    int curReg = 0;
    for (EVT vt : retTys) {
      EVT registerVT = getRegisterType(vt);
      int numRegs = getNumRegisters(vt);
      SDValue[] temp = new SDValue[numRegs];
      for (int i = 0; i < numRegs; i++)
        temp[i] = inVals.get(curReg + i);

      SDValue returnValue = getCopyFromParts(dag, temp, registerVT, vt,
          assertOp);
      for (int i = 0; i < numRegs; i++)
        inVals.set(i + curReg, temp[i]);
      returnValues.add(returnValue);
      curReg += numRegs;
    }

    if (returnValues.isEmpty())
      return Pair.get(new SDValue(), chain);

    SDValue res = dag.getNode(ISD.MERGE_VALUES, dag.getVTList(retTys), returnValues);
    return Pair.get(res, chain);
  }

  private int getByValTypeAlignment(Type ty) {
    return td.getCallFrameTypeAlignment(ty);
  }

  public boolean isEligibleTailCallOptimization(SDValue calle,
                                                CallingConv calleeCC, boolean isVarArg, ArrayList<InputArg> ins,
                                                SelectionDAG dag) {
    return false;
  }

  public abstract SDValue lowerCall(SDValue chain, SDValue callee,
                                    CallingConv cc, boolean isVarArg, boolean isTailCall,
                                    ArrayList<OutputArg> outs, ArrayList<InputArg> ins,
                                    SelectionDAG dag, ArrayList<SDValue> inVals);

  public boolean isTruncateFree(Type ty1, Type ty2) {
    return false;
  }

  public boolean isTruncateFree(EVT vt1, EVT vt2) {
    return false;
  }

  public boolean isZExtFree(EVT vt1, EVT vt2) {
    return false;
  }

  public boolean isZExtFree(Type ty1, Type ty2) {
    return false;
  }

  public MachineBasicBlock emitInstrWithCustomInserter(MachineInstr mi,
                                                       MachineBasicBlock mbb) {
    Util.shouldNotReachHere("Target must implements this method!");
    return null;
  }

  public boolean isOperationLegal(int opc, EVT vt) {
    return (vt.equals(new EVT(MVT.Other)) || isTypeLegal(vt))
        && getOperationAction(opc, vt) == Legal;
  }

  /**
   * It is similar to {@linkplain #isShuffleMaskLegal(TIntArrayList, EVT)}.
   * This is used by Targets to indicate if there is a suitbable
   * VECTOR_SHUFLE that can be used to replace a VAND with a constant
   * pool entry.
   *
   * @return
   */
  public boolean isVectorClearMaskLegal(TIntArrayList indices, EVT vt) {
    return false;
  }

  public LegalizeAction getOperationAction(int opc, EVT vt) {
    if (vt.isExtended()) return Expand;
    Util.assertion(opc < opActions[0].length && vt.getSimpleVT().simpleVT < 64 * 8, "Table isn't big enough!");

    int i = vt.getSimpleVT().simpleVT;
    int j = i & 31;
    i = i >> 5;
    return LegalizeAction.values()[(int) ((opActions[i][opc] >> (j * 2)) & 3)];
  }

  public boolean isOperationLegalOrCustom(int opc, EVT vt) {
    return (vt.getSimpleVT().simpleVT == MVT.Other || isTypeLegal(vt))
        && (getOperationAction(opc, vt) == Legal ||
        getOperationAction(opc, vt) == Custom);
  }

  public LegalizeAction getLoadExtAction(LoadExtType lType, EVT vt) {
    Util.assertion(lType.ordinal() < loadExtActions.length && vt.getSimpleVT().simpleVT < 32, "Table isn't big enough!");

    return LegalizeAction.values()[(int) (loadExtActions[lType.ordinal()]
        >> ((2 * vt.getSimpleVT().simpleVT) & 3))];
  }

  public boolean isLoadExtLegal(LoadExtType lType, EVT vt) {
    return vt.isSimple() && (getLoadExtAction(lType, vt) == Legal ||
        getLoadExtAction(lType, vt) == Custom);
  }

  public LegalizeAction getTruncStoreAction(EVT valVT, EVT memVT) {
    Util.assertion(valVT.getSimpleVT().simpleVT < truncStoreActions.length && memVT.getSimpleVT().simpleVT < 32, "Table isn't big enough!");

    return LegalizeAction.values()[(int) (truncStoreActions[valVT.getSimpleVT().simpleVT] >>
        ((2 * memVT.getSimpleVT().simpleVT) & 3))];
  }

  public void setTruncStoreAction(int valVT, int memVT, LegalizeAction action) {
    Util.assertion(valVT < truncStoreActions.length && memVT < 64 * 4, "Table isn't big enough!");

    truncStoreActions[valVT] &= ~(3L << memVT * 2);
    truncStoreActions[valVT] |= (long) action.ordinal() << memVT * 2;
  }

  public boolean isTruncStoreLegal(EVT valVT, EVT memVT) {
    return isTypeLegal(valVT) && memVT.isSimple() &&
        (getTruncStoreAction(valVT, memVT) == Legal ||
            getTruncStoreAction(valVT, memVT) == Custom);
  }

  public LegalizeAction getIndexedLoadAction(int idxMode, EVT vt) {
    Util.assertion(idxMode < indexedModeActions[0][0].length && vt.getSimpleVT().simpleVT < MVT.LAST_VALUETYPE,
        "Table isn't big enough!");

    return LegalizeAction.values()[(int) indexedModeActions[vt.getSimpleVT().simpleVT][0][idxMode]];
  }

  public void setIndexedModeAction(MemIndexedMode idxMode, MVT vt, LegalizeAction action) {
    Util.assertion(vt.simpleVT < MVT.LAST_VALUETYPE && idxMode.ordinal() < indexedModeActions[0][0].length,
        "Table isn't big enough!");

    indexedModeActions[vt.simpleVT][0][idxMode.ordinal()] = action.ordinal();
  }

  public boolean isIndexedLoadLegal(MemIndexedMode idxMode, EVT vt) {
    return vt.isSimple() && (getIndexedLoadAction(idxMode.ordinal(), vt) == Legal ||
        getIndexedLoadAction(idxMode.ordinal(), vt) == Custom);
  }

  public LegalizeAction getIndexedStoreAction(int idxMode, EVT vt) {
    Util.assertion(idxMode < indexedModeActions[0][1].length && vt.getSimpleVT().simpleVT < MVT.LAST_VALUETYPE,
        "Table is't big enough!");

    return LegalizeAction.values()[(int) indexedModeActions[vt.getSimpleVT().simpleVT][1][idxMode]];
  }

  public void setIndexedStoreAction(MemIndexedMode idxMode, MVT vt, LegalizeAction action) {
    Util.assertion(vt.simpleVT < MVT.LAST_VALUETYPE && idxMode.ordinal() < indexedModeActions[0][1].length,
        "Table isn't big enough!");

    indexedModeActions[vt.simpleVT][1][idxMode.ordinal()] = action.ordinal();
  }

  public boolean isIndexedStoreLegal(MemIndexedMode idxMode, EVT vt) {
    return vt.isSimple() && (getIndexedStoreAction(idxMode.ordinal(), vt) == Legal
        || getIndexedStoreAction(idxMode.ordinal(), vt) == Custom);
  }

  public LegalizeAction getConvertAction(EVT fromVT, EVT toVT) {
    Util.assertion(fromVT.getSimpleVT().simpleVT < condCodeActions.length && toVT.getSimpleVT().simpleVT < 32, "Table isn't big enough!");

    return LegalizeAction.values()[(int) condCodeActions[fromVT.getSimpleVT().simpleVT >>
        (2 * toVT.getSimpleVT().simpleVT) & 3]];
  }

  public LegalizeAction getCondCodeAction(CondCode cc, EVT vt) {
    Util.assertion(cc.ordinal() < condCodeActions.length && vt.getSimpleVT().simpleVT < 32, "Table isn't big enough!");

    LegalizeAction action = LegalizeAction.values()[(int) (condCodeActions[cc.ordinal()] >>
        (2 * vt.getSimpleVT().simpleVT) & 3)];
    Util.assertion(action != Promote, "Can't promote condition code!");
    return action;
  }

  public void setCondCodeAction(CondCode cc, int vt, LegalizeAction action) {
    Util.assertion(vt < 64 * 4 && cc.ordinal() < condCodeActions.length, "Table isn't big enough!");
    condCodeActions[cc.ordinal()] &= ~(3L << vt * 2);
    condCodeActions[cc.ordinal()] |= ((long) action.ordinal() << vt * 2);
  }

  public boolean isCondCodeLegal(CondCode cc, EVT vt) {
    return getCondCodeAction(cc, vt) == Legal ||
        getCondCodeAction(cc, vt) == Custom;
  }

  public void addLegalFPImmediate(APFloat imm) {
    legalFPImmediates.add(imm);
  }

  public int getNumLegalFPImmediate() {
    return legalFPImmediates.size();
  }

  public APFloat getLegalImmediate(int idx) {
    Util.assertion(idx >= 0 && idx < getNumLegalFPImmediate());
    return legalFPImmediates.get(idx);
  }

  public void setTargetDAGCombine(int opc) {
    Util.assertion((opc >> 3) < targetDAGCombineArray.length);
    targetDAGCombineArray[opc >> 3] |= 1 << (opc & 3);
  }

  public void addPromotedToType(int opc, int origVT, int destVT) {
    promoteType.put(Pair.get(opc, origVT), destVT);
  }

  public EVT getTypeToPromoteType(int opc, EVT vt) {
    Util.assertion(getOperationAction(opc, vt) == Promote, "This operation isn't promoted!");

    Pair<Integer, Integer> key = Pair.get(opc, vt.getSimpleVT().simpleVT);
    if (promoteType.containsKey(key))
      return new EVT(promoteType.get(key));

    Util.assertion(vt.isInteger() || vt.isFloatingPoint());
    EVT nvt = vt;
    do {
      nvt = new EVT(nvt.getSimpleVT().simpleVT + 1);
      Util.assertion(nvt.isInteger() == vt.isInteger() && !nvt.equals(new EVT(MVT.isVoid)), "Didn't find type to promote to!");

    } while (!isTypeLegal(nvt) || getOperationAction(opc, nvt) == Promote);
    return nvt;
  }

  public int getSetCCResultType(EVT vt) {
    return pointerTy.simpleVT;
  }

  public void replaceNodeResults(SDNode n, ArrayList<SDValue> results,
                                 SelectionDAG dag) {
    Util.assertion(false, "this method should be implemented for this target!");
  }

  public void lowerOperationWrapper(
      SDNode n,
      ArrayList<SDValue> results,
      SelectionDAG dag) {
    SDValue res = lowerOperation(new SDValue(n, 0), dag);
    if (res.getNode() != null)
      results.add(res);
  }

  public RTLIB getFPEXT(EVT opVT, EVT retVT) {
    if (opVT.getSimpleVT().simpleVT == MVT.f32)
      if (retVT.getSimpleVT().simpleVT == MVT.f64)
        return FPEXT_F32_F64;
    return UNKNOWN_LIBCALL;
  }

  public RTLIB getFPROUND(EVT opVT, EVT retVT) {
    int opSimpleVT = opVT.getSimpleVT().simpleVT;
    int retSimpleVT = retVT.getSimpleVT().simpleVT;
    if (retSimpleVT == MVT.f32) {
      if (opSimpleVT == MVT.f64)
        return FPROUND_F64_F32;
      if (opSimpleVT == MVT.f80)
        return FPROUND_F80_F32;
      if (opSimpleVT == MVT.ppcf128)
        return FPROUND_PPCF128_F32;
    } else if (retSimpleVT == MVT.f64) {
      if (opSimpleVT == MVT.f80)
        return FPROUND_F80_F64;
      if (opSimpleVT == MVT.ppcf128)
        return FPROUND_PPCF128_F64;
    }
    return UNKNOWN_LIBCALL;
  }

  public RTLIB getFPTOSINT(EVT opVT, EVT retVT) {
    int opSimpleVT = opVT.getSimpleVT().simpleVT;
    int retSimpleVT = retVT.getSimpleVT().simpleVT;
    if (opSimpleVT == MVT.f32) {
      if (retSimpleVT == MVT.i8)
        return FPTOSINT_F32_I8;
      if (retSimpleVT == MVT.i16)
        return FPTOSINT_F32_I16;
      if (retSimpleVT == MVT.i32)
        return FPTOSINT_F32_I32;
      if (retSimpleVT == MVT.i64)
        return FPTOSINT_F32_I64;
      if (retSimpleVT == MVT.i128)
        return FPTOSINT_F32_I128;
    } else if (opSimpleVT == MVT.f64) {
      if (retSimpleVT == MVT.i32)
        return FPTOSINT_F64_I32;
      if (retSimpleVT == MVT.i64)
        return FPTOSINT_F64_I64;
      if (retSimpleVT == MVT.i128)
        return FPTOSINT_F64_I128;
    } else if (opSimpleVT == MVT.f80) {
      if (retSimpleVT == MVT.i32)
        return FPTOSINT_F80_I32;
      if (retSimpleVT == MVT.i64)
        return FPTOSINT_F80_I64;
      if (retSimpleVT == MVT.i128)
        return FPTOSINT_F80_I128;
    } else if (opSimpleVT == MVT.ppcf128) {
      if (retSimpleVT == MVT.i32)
        return FPTOSINT_PPCF128_I32;
      if (retSimpleVT == MVT.i64)
        return FPTOSINT_PPCF128_I64;
      if (retSimpleVT == MVT.i128)
        return FPTOSINT_PPCF128_I128;
    }
    return UNKNOWN_LIBCALL;
  }

  public RTLIB getFPTOUINT(EVT opVT, EVT retVT) {
    int opSimpleVT = opVT.getSimpleVT().simpleVT;
    int retSimpleVT = retVT.getSimpleVT().simpleVT;

    if (opSimpleVT == MVT.f32) {
      if (retSimpleVT == MVT.i8)
        return FPTOUINT_F32_I8;
      if (retSimpleVT == MVT.i16)
        return FPTOUINT_F32_I16;
      if (retSimpleVT == MVT.i32)
        return FPTOUINT_F32_I32;
      if (retSimpleVT == MVT.i64)
        return FPTOUINT_F32_I64;
      if (retSimpleVT == MVT.i128)
        return FPTOUINT_F32_I128;
    } else if (opSimpleVT == MVT.f64) {
      if (retSimpleVT == MVT.i32)
        return FPTOUINT_F64_I32;
      if (retSimpleVT == MVT.i64)
        return FPTOUINT_F64_I64;
      if (retSimpleVT == MVT.i128)
        return FPTOUINT_F64_I128;
    } else if (opSimpleVT == MVT.f80) {
      if (retSimpleVT == MVT.i32)
        return FPTOUINT_F80_I32;
      if (retSimpleVT == MVT.i64)
        return FPTOUINT_F80_I64;
      if (retSimpleVT == MVT.i128)
        return FPTOUINT_F80_I128;
    } else if (opSimpleVT == MVT.ppcf128) {
      if (retSimpleVT == MVT.i32)
        return FPTOUINT_PPCF128_I32;
      if (retSimpleVT == MVT.i64)
        return FPTOUINT_PPCF128_I64;
      if (retSimpleVT == MVT.i128)
        return FPTOUINT_PPCF128_I128;
    }
    return UNKNOWN_LIBCALL;
  }

  public RTLIB getSINTTOFP(EVT opVT, EVT retVT) {
    int opSimpleVT = opVT.getSimpleVT().simpleVT;
    int retSimpleVT = retVT.getSimpleVT().simpleVT;

    if (opSimpleVT == MVT.i32) {
      if (retSimpleVT == MVT.f32)
        return SINTTOFP_I32_F32;
      else if (retSimpleVT == MVT.f64)
        return SINTTOFP_I32_F64;
      else if (retSimpleVT == MVT.f80)
        return SINTTOFP_I32_F80;
      else if (retSimpleVT == MVT.ppcf128)
        return SINTTOFP_I32_PPCF128;
    } else if (opSimpleVT == MVT.i64) {
      if (retSimpleVT == MVT.f32)
        return SINTTOFP_I64_F32;
      else if (retSimpleVT == MVT.f64)
        return SINTTOFP_I64_F64;
      else if (retSimpleVT == MVT.f80)
        return SINTTOFP_I64_F80;
      else if (retSimpleVT == MVT.ppcf128)
        return SINTTOFP_I64_PPCF128;
    } else if (opSimpleVT == MVT.i128) {
      if (retSimpleVT == MVT.f32)
        return SINTTOFP_I128_F32;
      else if (retSimpleVT == MVT.f64)
        return SINTTOFP_I128_F64;
      else if (retSimpleVT == MVT.f80)
        return SINTTOFP_I128_F80;
      else if (retSimpleVT == MVT.ppcf128)
        return SINTTOFP_I128_PPCF128;
    }
    return UNKNOWN_LIBCALL;
  }

  public RTLIB getUINTTOFP(EVT opVT, EVT retVT) {
    int opSimpleVT = opVT.getSimpleVT().simpleVT;
    int retSimpleVT = retVT.getSimpleVT().simpleVT;

    if (opSimpleVT == MVT.i32) {
      if (retSimpleVT == MVT.f32)
        return UINTTOFP_I32_F32;
      else if (retSimpleVT == MVT.f64)
        return UINTTOFP_I32_F64;
      else if (retSimpleVT == MVT.f80)
        return UINTTOFP_I32_F80;
      else if (retSimpleVT == MVT.ppcf128)
        return UINTTOFP_I32_PPCF128;
    } else if (opSimpleVT == MVT.i64) {
      if (retSimpleVT == MVT.f32)
        return UINTTOFP_I64_F32;
      else if (retSimpleVT == MVT.f64)
        return UINTTOFP_I64_F64;
      else if (retSimpleVT == MVT.f80)
        return UINTTOFP_I64_F80;
      else if (retSimpleVT == MVT.ppcf128)
        return UINTTOFP_I64_PPCF128;
    } else if (opSimpleVT == MVT.i128) {
      if (retSimpleVT == MVT.f32)
        return UINTTOFP_I128_F32;
      else if (retSimpleVT == MVT.f64)
        return UINTTOFP_I128_F64;
      else if (retSimpleVT == MVT.f80)
        return UINTTOFP_I128_F80;
      else if (retSimpleVT == MVT.ppcf128)
        return UINTTOFP_I128_PPCF128;
    }
    return UNKNOWN_LIBCALL;
  }

  public CondCode getCmpLibCallCC(RTLIB lc) {
    return cmpLibCallCCs[lc.ordinal()];
  }

  public String getLibCallName(RTLIB lc) {
    return libCallRoutineNames[lc.ordinal()];
  }

  public void setLibCallName(RTLIB lc, String name) {
    libCallRoutineNames[lc.ordinal()] = name;
  }

  public CallingConv getLibCallCallingConv(RTLIB lc) {
    return libCallCallingConv[lc.ordinal()];
  }

  /**
   * Try to simplify a setcc built with the specified operands and cc.
   * If it is unable to simplify it, return a null value.
   *
   * @param vt
   * @param lhs
   * @param rhs
   * @param cc
   * @param foldBooleans
   * @param dagCBI
   * @return
   */
  public SDValue simplifySetCC(EVT vt,
                               SDValue lhs,
                               SDValue rhs,
                               CondCode cc,
                               boolean foldBooleans,
                               DAGCombinerInfo dagCBI) {
    SelectionDAG dag = dagCBI.dag;
    switch (cc) {
      case SETFALSE:
      case SETFALSE2:
        return dag.getConstant(0, vt, false);
      case SETTRUE:
      case SETTRUE2:
        return dag.getConstant(0, vt, false);
      default:
        break;
    }

    if (lhs.getNode() instanceof ConstantSDNode) {
      return dag.getSetCC(vt, rhs, lhs, ISD.getSetCCSwappedOperands(cc));
    }

    if (rhs.getNode() instanceof ConstantSDNode) {
      ConstantSDNode rhsCNT = (ConstantSDNode) rhs.getNode();
      APInt c = rhsCNT.getAPIntValue();
      if (lhs.getOpcode() == ISD.SRL && (c.eq(0) || c.eq(1)) &&
          lhs.getOperand(0).getOpcode() == ISD.CTLZ &&
          lhs.getOperand(1).getOpcode() == ISD.Constant) {
        // If the LHS is '(srl (ctlz x), 5)', the RHS is 0/1, and this is an
        // equality comparison, then we're just comparing whether X itself is
        // zero.
        long shAmt = ((ConstantSDNode) lhs.getOperand(1).getNode()).getZExtValue();
        if ((cc == SETEQ || cc == SETNE) && shAmt == Util.log2(lhs.getValueType().getSizeInBits())) {
          if (c.eq(0) && cc == SETEQ) {
            cc = SETNE;
          } else {
            cc = SETEQ;
          }
          SDValue zero = dag.getConstant(0, lhs.getValueType(), false);
          return dag.getSetCC(vt, lhs.getOperand(0).getOperand(0), zero, cc);
        }
      }

      // If the LHS is '(and load, const)', the RHS is 0,
      // the test is for equality or unsigned, and all 1 bits of the const are
      // in the same partial word, see if we can shorten the load.
      if (dagCBI.isBeforeLegalize() && lhs.getOpcode() == ISD.AND &&
          c.eq(0) && lhs.getNode().hasOneUse() &&
          lhs.getOperand(0).getNode() instanceof LoadSDNode &&
          lhs.getOperand(0).getNode().hasOneUse() &&
          lhs.getOperand(1).getNode() instanceof ConstantSDNode) {
        LoadSDNode ld = (LoadSDNode) lhs.getOperand(0).getNode();
        long bestMark = 0;
        int bestWidth = 0, bestOffset = 0;
        if (!ld.isVolatile() && ld.isUnindexed() &&
            lhs.getValueType().getSizeInBits() <= 64) {
          int origWidth = lhs.getValueType().getSizeInBits();
          if (ld.getExtensionType() == LoadExtType.NON_EXTLOAD) {
            origWidth = ld.getMemoryVT().getSizeInBits();
          }

          long mask = ((ConstantSDNode) lhs.getOperand(1).getNode()).getZExtValue();
          for (int width = origWidth / 2; width >= 8; width /= 2) {
            long newMask = (1L << width) - 1;
            for (int offset = 0; offset < origWidth / width; offset++) {
              if ((newMask & mask) == mask) {
                if (!td.isLittleEndian())
                  bestOffset = (origWidth / width - offset - 1) * (width / 8);
                else
                  bestOffset = offset * width / 8;
                bestMark = mask >> (offset * (width / 8) * 8);
                bestWidth = width;
                break;
              }
              newMask = newMask << width;
            }
          }
        }
        if (bestWidth != 0) {
          EVT newVT = EVT.getIntegerVT(bestWidth);
          if (newVT.isRound()) {
            EVT ptrType = ld.getOperand(1).getValueType();
            SDValue ptr = ld.getBasePtr();
            if (bestOffset == 0)
              ptr = dag.getNode(ISD.ADD, ptrType, ptr,
                  dag.getConstant(bestOffset, ptrType, false));
            int newAlign = Util.minAlign(ld.getAlignment(), bestOffset);
            SDValue newLoad = dag.getLoad(newVT, ld.getChain(), ptr,
                ld.getSrcValue(), ld.getSrcValueOffset() + bestOffset,
                false, newAlign);
            return dag.getSetCC(vt, dag.getNode(ISD.AND, newVT, newLoad,
                dag.getConstant(bestMark, newVT, false)),
                dag.getConstant(0, newVT, false),
                cc);
          }
        }
      }

      if (lhs.getOpcode() == ISD.ZERO_EXTEND) {
        int inSize = lhs.getOperand(0).getValueType().getSizeInBits();
        if (c.intersects(APInt.getHighBitsSet(c.getBitWidth(),
            c.getBitWidth() - inSize))) {
          switch (cc) {
            case SETUGT:
            case SETUGE:
            case SETEQ:
              return dag.getConstant(0, vt, false);
            case SETULT:
            case SETULE:
            case SETNE:
              return dag.getConstant(1, vt, false);
            case SETGT:
            case SETGE:
              return dag.getConstant(c.isNegative() ? 1 : 0, vt, false);
            case SETLT:
            case SETLE:
              return dag.getConstant(c.isNonNegative() ? 1 : 0, vt, false);
            default:
              break;
          }
        }

        switch (cc) {
          case SETEQ:
          case SETNE:
          case SETUGT:
          case SETUGE:
          case SETULT:
          case SETULE: {
            EVT newVT = lhs.getOperand(0).getValueType();
            if (dagCBI.isBeforeLegalizeOps() ||
                (isOperationLegal(ISD.SETCC, newVT)) &&
                    getCondCodeAction(cc, newVT) == Legal) {
              return dag.getSetCC(vt, lhs.getOperand(0),
                  dag.getConstant(new APInt(c).trunc(inSize),
                      newVT, false), cc);
            }
            break;
          }
          default:
            break;
        }
      } else if (lhs.getOpcode() == ISD.SIGN_EXTEND_INREG &&
          (cc == SETEQ || cc == SETNE)) {
        EVT extSrcTy = ((VTSDNode) lhs.getOperand(1).getNode()).getVT();
        int extSrcTyBits = extSrcTy.getSizeInBits();
        EVT extDstTy = lhs.getValueType();
        int extDstTyBits = extDstTy.getSizeInBits();

        APInt extBits = APInt.getHighBitsSet(extDstTyBits, extDstTyBits - extSrcTyBits);
        if (c.and(extBits).ne(0) && c.and(extBits).ne(extBits)) {
          return dag.getConstant(cc == SETNE ? 1 : 0, vt, false);
        }

        SDValue zextOp = new SDValue();
        EVT op0Ty = lhs.getOperand(0).getValueType();
        if (op0Ty.equals(extSrcTy)) {
          zextOp = lhs.getOperand(0);
        } else {
          APInt imm = APInt.getLowBitsSet(extDstTyBits, extSrcTyBits);
          zextOp = dag.getNode(ISD.AND, op0Ty, lhs.getOperand(0),
              dag.getConstant(imm, op0Ty, false));
        }
        if (!dagCBI.isCalledByLegalizer())
          dagCBI.addToWorkList(zextOp.getNode());
        return dag.getSetCC(vt, zextOp, dag.getConstant(
            c.and(APInt.getLowBitsSet(extDstTyBits, extSrcTyBits)),
            extDstTy, false), cc);
      } else if ((rhsCNT.isNullValue() || rhsCNT.getAPIntValue().eq(1)) &&
          (cc == SETEQ || cc == SETNE)) {
        if (lhs.getOpcode() == ISD.SETCC) {
          boolean trueWhenTrue = cc == SETEQ ^ (rhsCNT.getZExtValue() != 1);
          if (trueWhenTrue)
            return lhs;

          CondCode ccT = ((CondCodeSDNode) lhs.getOperand(2).getNode()).getCondition();
          ccT = ISD.getSetCCInverse(ccT, lhs.getOperand(0).getValueType().isInteger());
          return dag.getSetCC(vt, lhs.getOperand(0), lhs.getOperand(1), ccT);
        }
        if ((lhs.getOpcode() == ISD.XOR || lhs.getOpcode() == ISD.AND) &&
            lhs.getOperand(0).getOpcode() == ISD.XOR &
                lhs.getOperand(1).equals(lhs.getOperand(0).getOperand(1)) &&
            lhs.getOperand(1).getNode() instanceof ConstantSDNode &&
            ((ConstantSDNode) lhs.getOperand(1).getNode()).getAPIntValue().eq(1)) {
          int bitwidth = lhs.getValueSizeInBits();
          if (dag.maskedValueIsZero(lhs, APInt.getHighBitsSet(bitwidth, bitwidth - 1))) {
            SDValue val;
            if (lhs.getOpcode() == ISD.XOR)
              val = lhs.getOperand(0);
            else {
              Util.assertion(lhs.getOpcode() == ISD.AND && lhs.getOperand(0).getOpcode() == ISD.XOR);

              val = dag.getNode(ISD.AND, lhs.getValueType(),
                  lhs.getOperand(0).getOperand(0),
                  lhs.getOperand(1));
            }
            return dag.getSetCC(vt, val, rhs, cc == SETEQ ? SETNE : SETEQ);
          }
        }
      }

      APInt minVal, maxVal;
      int operandBitSize = rhsCNT.getValueType(0).getSizeInBits();
      if (cc.isSignedIntSetCC()) {
        minVal = APInt.getSignedMinValue(operandBitSize);
        maxVal = APInt.getSignedMaxValue(operandBitSize);
      } else {
        minVal = APInt.getMinValue(operandBitSize);
        maxVal = APInt.getMaxValue(operandBitSize);
      }

      if (cc == SETGE || cc == SETUGE) {
        if (c.eq(minVal)) return dag.getConstant(1, vt, false);
        return dag.getSetCC(vt, lhs, dag.getConstant(c.decrease(),
            rhs.getValueType(), false), cc == SETGE ? SETGT : SETUGT);
      }

      if (cc == SETLE || cc == SETULE) {
        if (c.eq(maxVal)) return dag.getConstant(1, vt, false);
        return dag.getSetCC(vt, lhs, dag.getConstant(c.increase(),
            rhs.getValueType(), false), cc == SETLE ? SETLT : SETULT);
      }

      if ((cc == SETLT || cc == SETULT) && c.eq(minVal))
        return dag.getConstant(0, vt, false);
      if ((cc == SETGE || cc == SETUGE) && c.eq(minVal))
        return dag.getConstant(1, vt, false);
      if ((cc == SETGT || cc == SETUGT) && c.eq(maxVal))
        return dag.getConstant(0, vt, false);
      if ((cc == SETLE || cc == SETULE) && c.eq(maxVal))
        return dag.getConstant(1, vt, false);

      if ((cc == SETGT || cc == SETUGT) && c.eq(minVal))
        return dag.getSetCC(vt, lhs, rhs, SETNE);
      if ((cc == SETLT || cc == SETULT) && c.eq(maxVal))
        return dag.getSetCC(vt, lhs, rhs, SETNE);

      if ((cc == SETLT || cc == SETULT) && c.eq(minVal.add(1)))
        return dag.getSetCC(vt, lhs, dag.getConstant(minVal, lhs.getValueType(), false),
            SETEQ);
      else if ((cc == SETGT || cc == SETUGT) && c.eq(maxVal.sub(1)))
        return dag.getSetCC(vt, lhs, dag.getConstant(maxVal, lhs.getValueType(), false),
            SETEQ);

      if (cc == SETUGT && c.eq(APInt.getSignedMaxValue(operandBitSize))) {
        return dag.getSetCC(vt, lhs, dag.getConstant(0, rhs.getValueType(), false),
            SETLT);
      }

      if (cc == SETULT && c.eq(APInt.getSignedMinValue(operandBitSize))) {
        SDValue constMinusOne = dag.getConstant(APInt.getAllOnesValue(operandBitSize),
            rhs.getValueType(), false);
        return dag.getSetCC(vt, lhs, constMinusOne, SETGT);
      }

      if ((cc == SETEQ || cc == SETNE) && vt.equals(lhs.getValueType()) &&
          lhs.getOpcode() == ISD.AND) {
        if (lhs.getOperand(1).getNode() instanceof ConstantSDNode) {
          ConstantSDNode csn = (ConstantSDNode) lhs.getOperand(1).getNode();
          EVT shiftTy = dagCBI.isBeforeLegalize() ?
              new EVT(getPointerTy()) : new EVT(getShiftAmountTy());
          if (cc == SETNE && c.eq(0)) {
            if (Util.isPowerOf2(csn.getZExtValue())) {
              return dag.getNode(ISD.SRL, vt, lhs,
                  dag.getConstant(Util.log2(csn.getZExtValue()), shiftTy, false));
            }
          } else if (cc == SETEQ && c.eq(csn.getZExtValue())) {
            if (c.isPowerOf2())
              return dag.getNode(ISD.SRL, vt, lhs,
                  dag.getConstant(c.logBase2(), shiftTy, false));
          }
        }
      }
    }

    if (lhs.getNode() instanceof ConstantFPSDNode) {
      SDValue o = dag.foldSetCC(vt, lhs, rhs, cc);
      if (o.getNode() != null)
        return o;
    } else if (rhs.getNode() instanceof ConstantFPSDNode) {
      ConstantFPSDNode cfp = (ConstantFPSDNode) rhs.getNode();
      if (cfp.getValueAPF().isNaN()) {
        switch (cc.getUnorderedFlavor()) {
          default:
            Util.shouldNotReachHere("Unknown flavor!");
            break;
          case 0:
            return dag.getConstant(0, vt, false);
          case 1:
            return dag.getConstant(1, vt, false);
          case 2:
            return dag.getUNDEF(vt);
        }
      }

      if (cc == SETO || cc == SETUO)
        return dag.getSetCC(vt, lhs, lhs, cc);
    }

    if (lhs.equals(rhs)) {
      if (lhs.getValueType().isInteger()) {
        return dag.getConstant(cc.isTrueWhenEqual() ? 1 : 0, vt, false);
      }
      int uof = cc.getUnorderedFlavor();
      if (uof == 2)
        return dag.getConstant(cc.isTrueWhenEqual() ? 1 : 0, vt, false);
      if (uof == (cc.isTrueWhenEqual() ? 1 : 0))
        return dag.getConstant(uof, vt, false);

      CondCode newCC = uof == 0 ? SETO : SETUO;
      if (newCC != cc)
        return dag.getSetCC(vt, lhs, rhs, newCC);
    }

    if ((cc == SETEQ || cc == SETNE) && lhs.getValueType().isInteger()) {
      int lhsOpc = lhs.getOpcode();
      if (lhsOpc == ISD.ADD || lhsOpc == ISD.SUB || lhsOpc == ISD.XOR) {
        if (lhsOpc == rhs.getOpcode()) {
          if (lhs.getOperand(0).equals(rhs.getOperand(0)))
            return dag.getSetCC(vt, lhs.getOperand(1),
                rhs.getOperand(1), cc);
          if (lhs.getOperand(1).equals(rhs.getOperand(1)))
            return dag.getSetCC(vt, lhs.getOperand(0),
                rhs.getOperand(0), cc);
          if (SelectionDAG.isCommutativeBinOp(lhsOpc)) {
            if (lhs.getOperand(0).equals(rhs.getOperand(1)))
              return dag.getSetCC(vt, lhs.getOperand(1),
                  rhs.getOperand(0), cc);
            if (lhs.getOperand(1).equals(rhs.getOperand(0)))
              return dag.getSetCC(vt, lhs.getOperand(0),
                  rhs.getOperand(0), cc);
          }
        }

        if (rhs.getNode() instanceof ConstantSDNode) {
          ConstantSDNode rhsC = (ConstantSDNode) rhs.getNode();
          if (lhs.getOperand(1).getNode() instanceof ConstantSDNode) {
            ConstantSDNode lhsOp1C = (ConstantSDNode) lhs.getOperand(1).getNode();

            if (lhs.getOpcode() == ISD.ADD && lhs.getNode().hasOneUse()) {
              return dag.getSetCC(vt, lhs.getOperand(0),
                  dag.getConstant(rhsC.getAPIntValue().
                          sub(lhsOp1C.getAPIntValue()),
                      lhs.getValueType(), false),
                  cc);
            }

            if (lhs.getOpcode() == ISD.XOR) {
              if (dag.maskedValueIsZero(lhs.getOperand(0), lhsOp1C.getAPIntValue().not()))
                return dag.getSetCC(vt, lhs.getOperand(0),
                    dag.getConstant(lhsOp1C.getAPIntValue().
                            xor(rhsC.getAPIntValue()),
                        lhs.getValueType(), false),
                    cc);
            }

            if (lhs.getOperand(0).getNode() instanceof ConstantSDNode) {
              ConstantSDNode lhsOp0C = (ConstantSDNode) lhs.getOperand(0).getNode();
              if (lhsOpc == ISD.SUB && lhs.getNode().hasOneUse()) {
                return dag.getSetCC(vt, lhs.getOperand(1),
                    dag.getConstant(lhsOp0C.getAPIntValue().
                            sub(rhsC.getAPIntValue()),
                        lhs.getValueType(), false),
                    cc);
              }
            }
          }
        }

        if (lhs.getOperand(0).equals(rhs))
          return dag.getSetCC(vt, lhs.getOperand(1),
              dag.getConstant(0, lhs.getValueType(), false), cc);
        if (lhs.getOperand(1).equals(rhs)) {
          if (SelectionDAG.isCommutativeBinOp(lhsOpc)) {
            return dag.getSetCC(vt, lhs.getOperand(0),
                dag.getConstant(0, lhs.getValueType(), false), cc);
          } else if (lhs.getNode().hasOneUse()) {
            SDValue sh = dag.getNode(ISD.SHL, rhs.getValueType(),
                rhs, dag.getConstant(1, new EVT(getShiftAmountTy()), false));
            if (!dagCBI.isCalledByLegalizer())
              dagCBI.addToWorkList(sh.getNode());
            return dag.getSetCC(vt, lhs.getOperand(0), sh, cc);
          }
        }
      }

      int rhsOpc = rhs.getOpcode();
      if (rhsOpc == ISD.ADD || rhsOpc == ISD.SUB || rhsOpc == ISD.XOR) {
        if (rhs.getOperand(0).equals(lhs))
          return dag.getSetCC(vt, rhs.getOperand(1),
              dag.getConstant(0, rhs.getValueType(), false), cc);
        else if (rhs.getOperand(1).equals(rhs)) {
          if (SelectionDAG.isCommutativeBinOp(rhsOpc)) {
            return dag.getSetCC(vt, rhs.getOperand(0),
                dag.getConstant(0, rhs.getValueType(), false), cc);
          } else if (rhs.getNode().hasOneUse()) {
            Util.assertion(rhsOpc == ISD.SUB, "Unexpected operation!");
            SDValue sh = dag.getNode(ISD.SHL, rhs.getValueType(),
                lhs, dag.getConstant(1, new EVT(getShiftAmountTy()), false));
            if (!dagCBI.isCalledByLegalizer())
              dagCBI.addToWorkList(sh.getNode());
            return dag.getSetCC(vt, sh, rhs.getOperand(0), cc);
          }
        }
      }

      if (lhsOpc == ISD.AND) {
        if (lhs.getOperand(0).equals(rhs) || lhs.getOperand(1).equals(rhs)) {
          if (valueHasExactlyOneBitSet(rhs, dag)) {
            cc = ISD.getSetCCInverse(cc, true);
            SDValue zero = dag.getConstant(0, rhs.getValueType(), false);
            return dag.getSetCC(vt, lhs, zero, cc);
          }
        }
      }
      if (rhsOpc == ISD.AND) {
        if (rhs.getOperand(0).equals(lhs) || rhs.getOperand(1).equals(lhs)) {
          if (valueHasExactlyOneBitSet(lhs, dag)) {
            cc = ISD.getSetCCInverse(cc, true);
            SDValue zero = dag.getConstant(0, lhs.getValueType(), false);
            return dag.getSetCC(vt, rhs, zero, cc);
          }
        }
      }
    }

    SDValue temp;
    if (lhs.getValueType().getSimpleVT().simpleVT == MVT.i1 && foldBooleans) {
      switch (cc) {
        default:
          Util.shouldNotReachHere("Unknown integer setcc!");
          break;
        case SETEQ:
          temp = dag.getNode(ISD.XOR, new EVT(MVT.i1), lhs, rhs);
          lhs = dag.getNOT(temp, new EVT(MVT.i1));
          if (!dagCBI.isCalledByLegalizer())
            dagCBI.addToWorkList(temp.getNode());
          break;
        case SETNE:
          lhs = dag.getNode(ISD.XOR, new EVT(MVT.i1), lhs, rhs);
          break;
        case SETGT:
        case SETULT:
          temp = dag.getNOT(lhs, new EVT(MVT.i1));
          lhs = dag.getNode(ISD.ADD, new EVT(MVT.i1), rhs, temp);
          if (!dagCBI.isCalledByLegalizer())
            dagCBI.addToWorkList(temp.getNode());
          break;
        case SETLT:
        case SETUGT:
          temp = dag.getNOT(rhs, new EVT(MVT.i1));
          lhs = dag.getNode(ISD.AND, new EVT(MVT.i1), lhs, temp);
          if (!dagCBI.isCalledByLegalizer())
            dagCBI.addToWorkList(temp.getNode());
          break;
        case SETULE:
        case SETGE:
          temp = dag.getNOT(lhs, new EVT(MVT.i1));
          lhs = dag.getNode(ISD.OR, new EVT(MVT.i1), rhs, temp);
          if (!dagCBI.isCalledByLegalizer())
            dagCBI.addToWorkList(temp.getNode());
          break;
        case SETUGE:
        case SETLE:
          temp = dag.getNOT(rhs, new EVT(MVT.i1));
          lhs = dag.getNode(ISD.OR, new EVT(MVT.i1), lhs, temp);
          break;
      }
      if (vt.getSimpleVT().simpleVT != MVT.i1) {
        if (!dagCBI.isCalledByLegalizer())
          dagCBI.addToWorkList(lhs.getNode());
        lhs = dag.getNode(ISD.ZERO_EXTEND, vt, lhs);
      }
      return lhs;
    }
    return new SDValue();
  }

  private static boolean valueHasExactlyOneBitSet(SDValue val, SelectionDAG dag) {
    if (val.getNode().getOperand(0).getNode() instanceof ConstantSDNode) {
      ConstantSDNode c = (ConstantSDNode) val.getNode().getOperand(0).getNode();
      if (c.getAPIntValue().eq(1) && val.getOpcode() == ISD.SHL)
        return true;
      if (c.getAPIntValue().isSignBit() && val.getOpcode() == ISD.SRL)
        return true;
    }
    EVT opVT = val.getValueType();
    int bitWidth = opVT.getStoreSizeInBits();
    APInt mask = APInt.getAllOnesValue(bitWidth);
    APInt[] res = new APInt[2];
    dag.computeMaskedBits(val, mask, res, 0);
    return res[0].countPopulation() == bitWidth - 1 &&
        res[1].countPopulation() == 1;
  }

  public SDValue lowerOperation(SDValue op, SelectionDAG dag) {
    Util.shouldNotReachHere("lowerOperation not implemented for this target!");
    return new SDValue();
  }

  public boolean allowsUnalignedMemoryAccesses(EVT memVT) {
    return false;
  }

  public boolean isShuffleMaskLegal(TIntArrayList mask, EVT vt) {
    return true;
  }

  public boolean hasTargetDAGCombine(int opc) {
    Util.assertion((opc >> 3) < targetDAGCombineArray.length);
    return (targetDAGCombineArray[opc >> 3] & (1 << (opc & 7))) != 0;
  }

  public int getMaxStoresPerMemset() {
    return maxStoresPerMemset;
  }

  public int getMaxStoresPerMemcpy() {
    return maxStoresPerMemcpy;
  }

  public int getMaxStoresPerMemmove() {
    return maxStoresPerMemmove;
  }

  public SDValue performDAGCombine(SDNode n, DAGCombinerInfo combineInfo) {
    return new SDValue();
  }

  /**
   * At this point, we know that only the
   * DemandedMask bits of the result of Op are ever used downstream.  If we can
   * use this information to simplify Op, create a new simplified DAG node and
   * return true, returning the original and new nodes in Old and New. Otherwise,
   * analyze the expression and return a mask of KnownOne and KnownZero bits for
   * the expression (used to simplify the caller).  The KnownZero/One bits may
   * only be accurate for those bits in the DemandedMask.
   *
   * @param op
   * @param demandedMask
   * @param tlo
   * @param res
   * @param depth
   * @return
   */
  public boolean simplifyDemandedBits(SDValue op,
                                      APInt demandedMask,
                                      TargetLoweringOpt tlo,
                                      APInt[] res,
                                      int depth) {
    // res[0] -- knownZero
    // res[1] -- knownOne
    Util.assertion(res != null && res.length == 2, "Invalid res passed through!");
    Util.assertion(depth >= 0);
    int bitwidth = demandedMask.getBitWidth();
    Util.assertion(op.getValueSizeInBits() == bitwidth, "Mask size mismatches value type size!");
    APInt newMask = demandedMask;
    res[0] = new APInt(bitwidth, 0);
    res[1] = new APInt(bitwidth, 0);
    if (!op.getNode().hasOneUse()) {
      if (depth != 0) {
        tlo.dag.computeMaskedBits(op, demandedMask, res, depth);
        return false;
      }

      newMask = APInt.getAllOnesValue(bitwidth);
    } else if (demandedMask.eq(0)) {
      if (op.getOpcode() != ISD.UNDEF)
        return tlo.combineTo(op, tlo.dag.getUNDEF(op.getValueType()));
      return false;
    } else if (depth == 6)
      return false;

    switch (op.getOpcode()) {
      case ISD.Constant:
        res[1] = ((ConstantSDNode) op.getNode()).getAPIntValue().and(newMask);
        res[0] = res[1].not().and(newMask);
        return false;
      case ISD.AND: {
        // If the RHS is a constant, check to see if the LHS would be zero without
        // using the bits from the RHS.  Below, we use knowledge about the RHS to
        // simplify the LHS, here we're using information from the LHS to simplify
        // the RHS.
        if (op.getOperand(1).getNode() instanceof ConstantSDNode) {
          ConstantSDNode rhsC = (ConstantSDNode) op.getOperand(1).getNode();
          APInt[] lhsRes = new APInt[2];
          tlo.dag.computeMaskedBits(op.getOperand(0), newMask, lhsRes,
              depth + 1);
          if (lhsRes[0].and(newMask).eq(rhsC.getAPIntValue().not().and(newMask))) {
            return tlo.combineTo(op, op.getOperand(0));
          }
          if (tlo.shrinkDemandedConstant(op, lhsRes[0].not().and(newMask)))
            return true;
        }

        if (simplifyDemandedBits(op.getOperand(1), newMask, tlo, res,
            depth + 1))
          return true;
        Util.assertion(res[0].and(res[1]).eq(0));
        APInt[] lhsRes = new APInt[2];
        if (simplifyDemandedBits(op.getOperand(0), res[0].not().and(newMask),
            tlo, lhsRes, depth + 1))
          return true;
        Util.assertion(lhsRes[0].and(lhsRes[1]).eq(0));
        if (newMask.and(lhsRes[0].not()).and(res[1]).eq(lhsRes[0].not().and(newMask))) {
          return tlo.combineTo(op, op.getOperand(0));
        }
        if (newMask.and(res[0].not()).and(lhsRes[1]).eq(res[0].not().and(newMask)))
          return tlo.combineTo(op, op.getOperand(1));
        if (newMask.and(res[0].or(lhsRes[0])).eq(newMask))
          return tlo.combineTo(op,
              tlo.dag.getConstant(0, op.getValueType(), false));
        if (tlo.shrinkDemandedConstant(op, lhsRes[0].not().and(newMask)))
          return true;
        if (tlo.shrinkDemandedOp(op, bitwidth, newMask))
          return true;

        res[1].andAssign(lhsRes[1]);
        res[1].orAssign(lhsRes[0]);
        break;
      }
      case ISD.SIGN_EXTEND_INREG: {
        EVT vt = ((VTSDNode) op.getOperand(1).getNode()).getVT();
        APInt newBits = APInt.getHighBitsSet(bitwidth, bitwidth -
            vt.getSizeInBits()).and(newMask);
        if (newBits.eq(0))
          return tlo.combineTo(op, op.getOperand(0));

        APInt inSignBit = APInt.getSignBit(vt.getSizeInBits());
        inSignBit.zext(bitwidth);
        APInt inputDemandedBits = APInt.getLowBitsSet(bitwidth,
            vt.getSizeInBits()).and(newMask);
        inputDemandedBits.orAssign(inSignBit);
        if (simplifyDemandedBits(op.getOperand(0), inputDemandedBits,
            tlo, res, depth + 1))
          return true;

        Util.assertion(res[0].and(res[1]).eq(0), "Bits known to be one AND zero?");
        if (res[0].intersects(inSignBit)) {
          return tlo.combineTo(op, tlo.dag.getZeroExtendInReg(
              op.getOperand(0), vt));
        }
        if (res[1].intersects(inSignBit)) {
          res[1].orAssign(newBits);
          res[0].andAssign(newBits.not());
        } else {
          res[0].and(newBits.not());
          res[1].and(newBits.not());
        }
        break;
      }
      case ISD.ZERO_EXTEND: {
        int operandBitWidth = op.getOperand(0).getValueSizeInBits();
        APInt inMask = new APInt(newMask);
        inMask.trunc(operandBitWidth);

        APInt newBits = APInt.getHighBitsSet(bitwidth, bitwidth - operandBitWidth).
            and(newMask);
        if (!newBits.intersects(newMask)) {
          return tlo.combineTo(op, tlo.dag.getNode(ISD.ANY_EXTEND,
              op.getValueType(), op.getOperand(0)));
        }
        if (simplifyDemandedBits(op.getOperand(0), inMask, tlo, res, depth + 1)) {
          return true;
        }
        Util.assertion(res[0].add(res[1]).eq(0), "Bits known to be one AND zero?");
        res[0].zext(bitwidth);
        res[1].zext(bitwidth);
        res[0].orAssign(newBits);
        break;
      }
      case ISD.SIGN_EXTEND: {
        EVT inVT = op.getOperand(0).getValueType();
        int inBits = inVT.getSizeInBits();
        APInt inMask = APInt.getLowBitsSet(bitwidth, inBits);
        APInt inSignBit = APInt.getBitsSet(bitwidth, inBits - 1, inBits);
        APInt newBits = inMask.not().and(newMask);

        if (newBits.eq(0))
          return tlo.combineTo(op, tlo.dag.getNode(ISD.ANY_EXTEND,
              op.getValueType(), op.getOperand(0)));

        APInt inDemandedBits = inMask.and(newMask);
        inDemandedBits.orAssign(inSignBit);
        inDemandedBits.trunc(inBits);
        if (simplifyDemandedBits(op.getOperand(0), inDemandedBits,
            tlo, res, depth + 1))
          return true;
        res[0].zext(bitwidth);
        res[1].zext(bitwidth);

        if (res[0].intersects(inSignBit)) {
          return tlo.combineTo(op, tlo.dag.getNode(ISD.ZERO_EXTEND,
              op.getValueType(), op.getOperand(0)));
        }
        if (res[1].intersects(inSignBit)) {
          res[1].orAssign(newBits);
          res[0].andAssign(newBits.not());
        } else {
          res[1].andAssign(newBits.not());
          res[0].andAssign(newBits.not());
        }
        break;
      }
      case ISD.ANY_EXTEND: {
        int operandBitWidth = op.getOperand(0).getValueSizeInBits();
        APInt inMask = newMask;
        inMask.trunc(operandBitWidth);
        if (simplifyDemandedBits(op.getOperand(0), inMask, tlo, res, depth + 1))
          return true;
        Util.assertion(res[0].and(res[1]).eq(0), "Bits known to be one AND zero?");
        res[0].zext(bitwidth);
        res[1].zext(bitwidth);
        break;
      }
      case ISD.TRUNCATE: {
        APInt truncMask = newMask;
        truncMask.zext(op.getOperand(0).getValueSizeInBits());
        if (simplifyDemandedBits(op.getOperand(0), truncMask, tlo, res, depth + 1))
          return true;
        res[0].trunc(bitwidth);
        res[1].trunc(bitwidth);

        if (op.getOperand(0).getNode().hasOneUse()) {
          SDValue in = op.getOperand(0);
          int inBitwidth = in.getValueSizeInBits();
          switch (in.getOpcode()) {
            default:
              break;
            case ISD.SRL:
              ConstantSDNode shAmt = in.getOperand(1).getNode() instanceof
                  ConstantSDNode ? (ConstantSDNode) in.getOperand(1).getNode() : null;
              if (shAmt != null) {
                APInt highBits = APInt.getHighBitsSet(inBitwidth,
                    inBitwidth - bitwidth);
                highBits = highBits.lshr(shAmt.getZExtValue());
                highBits.trunc(bitwidth);

                if (shAmt.getZExtValue() < bitwidth &&
                    highBits.and(newMask).eq(0)) {
                  SDValue newTrunc = tlo.dag.getNode(ISD.TRUNCATE,
                      op.getValueType(), in.getOperand(0));
                  return tlo.combineTo(op, tlo.dag.getNode(ISD.SRL,
                      op.getValueType(), newTrunc,
                      in.getOperand(1)));
                }
              }
              break;
          }
        }
        Util.assertion(res[0].and(res[1]).eq(0), "Bits known to be one AND zero?");
        break;
      }
      case ISD.AssertSext: {
        EVT vt = ((VTSDNode) op.getOperand(1).getNode()).getVT();
        APInt inMask = APInt.getLowBitsSet(bitwidth, vt.getSizeInBits());
        if (simplifyDemandedBits(op.getOperand(0), inMask.and(newMask),
            tlo, res, depth + 1))
          return true;
        Util.assertion(res[0].andAssign(res[1]).eq(0), "Bits known to be one AND zero?");
        res[0].orAssign(inMask.not().and(newMask));
        break;
      }
      case ISD.ADD:
      case ISD.MUL:
      case ISD.SUB: {
        APInt loMask = APInt.getLowBitsSet(bitwidth,
            bitwidth - newMask.countLeadingZeros());
        APInt[] lhsRes = new APInt[2];
        if (simplifyDemandedBits(op.getOperand(0), loMask, tlo, lhsRes, depth + 1))
          return true;
        if (simplifyDemandedBits(op.getOperand(1), loMask, tlo, lhsRes, depth + 1))
          return true;
        if (tlo.shrinkDemandedOp(op, bitwidth, newMask))
          return true;
      }
      default:
        tlo.dag.computeMaskedBits(op, newMask, res, depth);
        break;
    }

    if (newMask.and(res[0].or(res[1])).eq(newMask))
      return tlo.combineTo(op, tlo.dag.getConstant(res[1], op.getValueType(), false));
    return false;
  }

  public boolean getTargetMemIntrinsic(IntrinsicInfo info, Instruction.CallInst ci, Intrinsic.ID iid) {
    return false;
  }

  public EVT getOptimalMemOpType(long size, int align,
                                 boolean isSrcConst,
                                 boolean isSrcStr,
                                 SelectionDAG dag) {
    return new EVT(MVT.iAny);
  }

  public SDValue emitTargetCodeForMemcpy(SelectionDAG dag,
                                         SDValue chain,
                                         SDValue dst,
                                         SDValue src,
                                         SDValue size,
                                         int align,
                                         boolean alwaysInline,
                                         Value dstVal,
                                         long dstOff,
                                         Value srcVal,
                                         long srcOff) {
    return new SDValue();
  }

  public boolean isOffsetFoldingLegal(GlobalAddressSDNode ga) {
    // assume that everything is safe in static mode.
    if (tm.getRelocationModel() == TargetMachine.RelocModel.Static)
      return true;
    // in dynamic-no-pic mode, assume that known defined values are safe.
    if (tm.getRelocationModel() == TargetMachine.RelocModel.DynamicNoPIC &&
        ga != null && !ga.getGlobalValue().isDeclaration() &&
        !ga.getGlobalValue().isWeakForLinker())
      return true;
    // Otherwise assume nothing is safe.
    return false;
  }

  public SDValue buildSDIV(SDNode n, SelectionDAG dag, ArrayList<SDNode> created) {
    EVT vt = n.getValueType(0);
    if (!isTypeLegal(vt))
      return new SDValue();

    APInt d = ((ConstantSDNode) n.getOperand(1).getNode()).getAPIntValue();
    APInt.MU magics = d.magic();

    SDValue q;
    if (isOperationLegalOrCustom(ISD.MULHS, vt))
      q = dag.getNode(ISD.MULHS, vt, n.getOperand(0),
          dag.getConstant(magics.m, vt, false));
    else if (isOperationLegalOrCustom(ISD.SMUL_LOHI, vt))
      q = new SDValue(dag.getNode(ISD.SMUL_LOHI,
          dag.getVTList(vt, vt),
          n.getOperand(0),
          dag.getConstant(magics.m, vt, false)).getNode(), 1);
    else
      q = new SDValue();
    if (d.isStrictlyPositive() && magics.m.isNegative()) {
      q = dag.getNode(ISD.ADD, vt, q, n.getOperand(0));
      if (created != null)
        created.add(q.getNode());
    }
    if (d.isNegative() && magics.m.isStrictlyPositive()) {
      q = dag.getNode(ISD.SUB, vt, q, n.getOperand(0));
      if (created != null)
        created.add(q.getNode());
    }
    if (magics.s > 0) {
      q = dag.getNode(ISD.SRA, vt, q, dag.getConstant(magics.s,
          new EVT(getShiftAmountTy()), false));
      if (created != null)
        created.add(q.getNode());
    }
    SDValue t = dag.getNode(ISD.SRL, vt, q,
        dag.getConstant(vt.getSizeInBits() - 1, new EVT(getShiftAmountTy()), false));
    if (created != null)
      created.add(t.getNode());

    return dag.getNode(ISD.ADD, vt, q, t);
  }

  public SDValue buildUDIV(SDNode n, SelectionDAG dag, ArrayList<SDNode> created) {
    EVT vt = n.getValueType(0);
    if (!isTypeLegal(vt))
      return new SDValue();

    ConstantSDNode n1C = n.getOperand(1).getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n.getOperand(1).getNode() : null;
    Util.assertion(n1C != null);

    APInt.MU magics = n1C.getAPIntValue().magicu();
    SDValue q;
    if (isOperationLegalOrCustom(ISD.MULHU, vt))
      q = dag.getNode(ISD.MULHU, vt, n.getOperand(0),
          dag.getConstant(magics.m, vt, false));
    else if (isOperationLegalOrCustom(ISD.UMUL_LOHI, vt))
      q = new SDValue(dag.getNode(ISD.UMUL_LOHI,
          dag.getVTList(vt, vt),
          n.getOperand(0),
          dag.getConstant(magics.m, vt, false)).getNode(), 1);
    else
      q = new SDValue();
    if (created != null)
      created.add(q.getNode());

    if (magics.a) {
      Util.assertion(magics.s < n1C.getAPIntValue().getBitWidth());
      return dag.getNode(ISD.SRL, vt, q,
          dag.getConstant(magics.s, new EVT(getShiftAmountTy()), false));
    } else {
      SDValue npq = dag.getNode(ISD.SUB, vt, n.getOperand(0), q);
      if (created != null)
        created.add(npq.getNode());
      npq = dag.getNode(ISD.SRL, vt, npq,
          dag.getConstant(1, new EVT(getShiftAmountTy()), false));
      if (created != null)
        created.add(npq.getNode());
      npq = dag.getNode(ISD.ADD, vt, npq, q);
      if (created != null)
        created.add(npq.getNode());
      return dag.getNode(ISD.SRL, vt, npq,
          dag.getConstant(magics.s - 1, new EVT(getShiftAmountTy()), false));
    }
  }

  public boolean isConsecutiveLoad(LoadSDNode ld, LoadSDNode base, int bytes, int dist, MachineFrameInfo mfi) {
    if (!ld.getChain().equals(base.getChain()))
      return false;
    EVT vt = ld.getValueType(0);
    if (vt.getSizeInBits() / 8 != bytes)
      return false;
    SDValue loc = ld.getOperand(1);
    SDValue baseLoc = base.getOperand(1);
    if (loc.getOpcode() == ISD.FrameIndex) {
      if (baseLoc.getOpcode() != ISD.FrameIndex)
        return false;
      int fi = ((FrameIndexSDNode) loc.getNode()).getFrameIndex();
      int bfi = ((FrameIndexSDNode) baseLoc.getNode()).getFrameIndex();
      int fs = (int) mfi.getObjectSize(fi);
      int bfs = (int) mfi.getObjectSize(bfi);
      if (fs != bfs || fs != bytes)
        return false;
      return mfi.getObjectOffset(fi) == mfi.getObjectOffset(bfi) + dist * bytes;
    }
    if (loc.getOpcode() == ISD.ADD &&
        loc.getOperand(0).equals(baseLoc)) {
      if (loc.getOperand(1).getNode() instanceof ConstantSDNode) {
        ConstantSDNode csd = (ConstantSDNode) loc.getOperand(1).getNode();
        if (csd.getSExtValue() == dist * bytes)
          return true;
      }
    }
    OutRef<GlobalValue> gv1 = new OutRef<>();
    OutRef<GlobalValue> gv2 = new OutRef<>();
    OutRef<Long> offset1 = new OutRef<Long>(0L);
    OutRef<Long> offset2 = new OutRef<Long>(0L);

    boolean isGA1 = isGAPlusOffset(loc.getNode(), gv1, offset1);
    boolean isGA2 = isGAPlusOffset(baseLoc.getNode(), gv2, offset2);
    ;
    if (isGA1 && isGA2 && gv1.get().equals(gv2.get()))
      return offset1.get() == offset2.get() + dist * bytes;
    return false;
  }

  public boolean isGAPlusOffset(SDNode n,
                                OutRef<GlobalValue> gv,
                                OutRef<Long> offset) {
    if (n instanceof GlobalAddressSDNode) {
      GlobalAddressSDNode ga = (GlobalAddressSDNode) n;
      gv.set(ga.getGlobalValue());
      offset.set(offset.get() + ga.getOffset());
      return true;
    }
    if (n.getOpcode() == ISD.ADD) {
      SDValue n1 = n.getOperand(0);
      SDValue n2 = n.getOperand(1);
      if (isGAPlusOffset(n1.getNode(), gv, offset)) {
        if (n2.getNode() instanceof ConstantSDNode) {
          ConstantSDNode v = (ConstantSDNode) n2.getNode();
          offset.set(offset.get() + v.getSExtValue());
          return true;
        }
      } else if (isGAPlusOffset(n2.getNode(), gv, offset)) {
        if (n1.getNode() instanceof ConstantSDNode) {
          ConstantSDNode v = (ConstantSDNode) n1.getNode();
          offset.set(offset.get() + v.getSExtValue());
          return true;
        }
      }
    }
    return false;
  }

  public boolean isPreIndexedAddressPart(SDNode n,
                                         OutRef<SDValue> basePtr,
                                         OutRef<SDValue> offset,
                                         OutRef<MemIndexedMode> am,
                                         SelectionDAG dag) {
    return false;
  }

  public boolean shouldShrinkFPConstant(EVT vt) {
    return true;
  }

  /**
   * Checks if it is profitable to narrow the Value from vt1 to vt2.
   * e.g. it is useful to narrow from i32 to i8 in X86 target.
   *
   * @param vt1
   * @param vt2
   * @return
   */
  public boolean isNarrowingProfitable(EVT vt1, EVT vt2) {
    return false;
  }
}