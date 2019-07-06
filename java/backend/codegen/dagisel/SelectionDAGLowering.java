/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package backend.codegen.dagisel;

import backend.analysis.aa.AliasAnalysis;
import backend.analysis.aa.AliasResult;
import backend.codegen.*;
import backend.codegen.dagisel.SDNode.RegisterSDNode;
import backend.debug.DIVariable;
import backend.debug.DebugLoc;
import backend.intrinsic.Intrinsic;
import backend.ir.AllocationInst;
import backend.support.*;
import backend.target.*;
import backend.type.*;
import backend.utils.InstVisitor;
import backend.value.*;
import backend.value.Instruction.*;
import backend.value.Instruction.CmpInst.Predicate;
import backend.value.IntrinsicInst.DbgDeclareInst;
import backend.value.IntrinsicInst.DbgValueInst;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.*;

import java.util.*;

import static backend.codegen.dagisel.FunctionLoweringInfo.computeValueVTs;
import static backend.codegen.dagisel.RegsForValue.getCopyToParts;
import static backend.intrinsic.Intrinsic.ID.not_intrinsic;
import static backend.target.TargetOptions.DisableJumpTables;
import static backend.target.TargetOptions.EnablePerformTailCallOpt;
import static backend.value.Operator.And;
import static backend.value.Operator.Or;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class SelectionDAGLowering implements InstVisitor<Void> {
  MachineBasicBlock curMBB;
  HashMap<Value, SDValue> nodeMap;
  ArrayList<SDValue> pendingLoads;
  ArrayList<SDValue> pendingExports;
  private int sdNodeOrder;
  private DebugLoc curDebugLoc;
  /**
   * Maps argument value for unused arguments. This is used
   * to preserve debug information for incoming arguments.
   */
  private HashMap<Value, SDValue> unusedArgNodeMap;
  private HashMap<Value, DanglingDebugInfo> danglingDebugInfoMap;

  public boolean hasTailCall() {
    return hasTailCall;
  }

  public void copyToExportRegsIfNeeds(Value val) {
    if (!val.isUseEmpty()) {
      if (funcInfo.valueMap.containsKey(val)) {
        int reg = funcInfo.valueMap.get(val);
        copyValueToVirtualRegister(val, reg);
      }
    }
  }

  private void addPendingLoad(SDValue ld) {
    if (ld == null || pendingLoads.contains(ld))
      return;
    pendingLoads.add(ld);
  }

  public void setCurrentBasicBlock(MachineBasicBlock mbb) {
    curMBB = mbb;
  }

  public MachineBasicBlock getCurrentBasicBlock() {
    return curMBB;
  }

  public DebugLoc getCurDebugLoc() {
    return curDebugLoc;
  }

  static class Case implements Comparable<Case> {
    ConstantInt low;
    ConstantInt high;
    MachineBasicBlock mbb;

    Case() {
    }

    Case(ConstantInt lowVal, ConstantInt highVal, MachineBasicBlock mbb) {
      this.low = lowVal;
      this.high = highVal;
      this.mbb = mbb;
    }

    public long size() {
      return high.getSExtValue() - low.getSExtValue() + 1;
    }

    @Override
    public int compareTo(Case o) {
      return high.getValue().slt(o.low.getValue()) ? 1 : -1;
    }
  }

  static class CaseBits implements Comparable<CaseBits> {
    long mask;
    MachineBasicBlock mbb;
    int bits;

    CaseBits(long mask, MachineBasicBlock mbb, int bits) {
      this.mask = mask;
      this.mbb = mbb;
      this.bits = bits;
    }

    @Override
    public int compareTo(CaseBits o) {
      return Integer.compare(bits, o.bits);
    }
  }

  static class CaseRec {
    CaseRec(MachineBasicBlock mbb, ConstantInt lt, ConstantInt ge,
            ArrayList<Case> caseRanges) {
      this.mbb = mbb;
      this.low = lt;
      this.high = ge;
      this.caseRanges = caseRanges;
    }

    MachineBasicBlock mbb;
    ConstantInt low;
    ConstantInt high;
    ArrayList<Case> caseRanges;
  }

  static class CaseBlock {
    CondCode cc;
    Value cmpLHS, cmpMHS, cmpRHS;
    MachineBasicBlock trueMBB, falseMBB;
    MachineBasicBlock thisMBB;

    public CaseBlock(CondCode cc, Value lhs, Value rhs, Value middle,
                     MachineBasicBlock tbb, MachineBasicBlock fbb,
                     MachineBasicBlock curbb) {
      this.cc = cc;
      this.cmpLHS = lhs;
      this.cmpMHS = middle;
      this.cmpRHS = rhs;
      this.trueMBB = tbb;
      this.falseMBB = fbb;
      this.thisMBB = curbb;
    }
  }

  static class JumpTable {
    int reg;
    int jti;
    MachineBasicBlock mbb;
    MachineBasicBlock defaultMBB;

    public JumpTable(int reg, int jti, MachineBasicBlock m,
                     MachineBasicBlock d) {
      this.reg = reg;
      this.jti = jti;
      mbb = m;
      defaultMBB = d;
    }
  }

  static class JumpTableHeader {
    APInt first;
    APInt last;
    Value val;
    MachineBasicBlock headerBB;
    boolean emitted;

    public JumpTableHeader(APInt f, APInt l, Value v, MachineBasicBlock h,
                           boolean e) {
      first = f;
      last = l;
      val = v;
      headerBB = h;
      emitted = e;
    }

    public JumpTableHeader(APInt f, APInt l, Value v, MachineBasicBlock h) {
      this(f, l, v, h, false);
    }
  }

  static class BitTestCase {
    long mask;
    MachineBasicBlock thisMBB;
    MachineBasicBlock targetMBB;

    public BitTestCase(long m, MachineBasicBlock t, MachineBasicBlock tr) {
      mask = m;
      thisMBB = t;
      targetMBB = tr;
    }
  }

  static class BitTestBlock {
    APInt first;
    APInt range;
    Value val;
    int reg;
    boolean emitted;
    MachineBasicBlock parentMBB;
    MachineBasicBlock defaultMBB;
    ArrayList<BitTestCase> cases;

    public BitTestBlock(APInt f, APInt r, Value v, int reg, boolean e,
                        MachineBasicBlock p, MachineBasicBlock d,
                        ArrayList<BitTestCase> cs) {
      first = f;
      range = r;
      val = v;
      this.reg = reg;
      emitted = e;
      parentMBB = p;
      defaultMBB = d;
      cases = new ArrayList<>();
      cases.addAll(cs);
    }
  }

  public TargetLowering tli;
  public SelectionDAG dag;
  public TargetData td;
  public AliasAnalysis aa;

  public ArrayList<Pair<MachineInstr, Integer>> phiNodesToUpdate;

  public TObjectIntHashMap<Constant> constantsOut;

  public FunctionLoweringInfo funcInfo;

  public TargetMachine.CodeGenOpt optLevel;
  boolean hasTailCall;

  public ArrayList<CaseBlock> switchCases;

  public ArrayList<Pair<JumpTableHeader, JumpTable>> jtiCases;

  public ArrayList<BitTestBlock> bitTestCases;

  public SelectionDAGLowering(SelectionDAG dag, TargetLowering tli,
                              FunctionLoweringInfo funcInfo, TargetMachine.CodeGenOpt level) {
    this.dag = dag;
    this.tli = tli;
    this.funcInfo = funcInfo;
    this.optLevel = level;

    nodeMap = new HashMap<>();
    pendingLoads = new ArrayList<>();
    pendingExports = new ArrayList<>();
    phiNodesToUpdate = new ArrayList<>();
    constantsOut = new TObjectIntHashMap<>();
    switchCases = new ArrayList<>();
    jtiCases = new ArrayList<>();
    bitTestCases = new ArrayList<>();
    unusedArgNodeMap = new HashMap<>();
    danglingDebugInfoMap = new HashMap<>();
  }

  public void init(AliasAnalysis aa) {
    this.aa = aa;
    td = dag.getTarget().getTargetData();
  }

  public void clear() {
    nodeMap.clear();
    pendingExports.clear();
    pendingLoads.clear();
    dag.clear();
    hasTailCall = false;
    curDebugLoc = new DebugLoc();
    unusedArgNodeMap.clear();
    danglingDebugInfoMap.clear();
  }

  public SDValue getRoot() {
    if (pendingLoads.isEmpty())
      return dag.getRoot();

    if (pendingLoads.size() == 1) {
      SDValue root = pendingLoads.get(0);
      dag.setRoot(root);
      pendingLoads.clear();
      return root;
    }
    SDValue[] vals = new SDValue[pendingLoads.size()];
    pendingLoads.toArray(vals);
    SDValue root = dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), vals);
    pendingLoads.clear();
    dag.setRoot(root);
    return root;
  }

  public SDValue getControlRoot() {
    SDValue root = dag.getRoot();
    if (pendingExports.isEmpty())
      return root;

    if (root.getOpcode() != ISD.EntryToken) {
      int i = 0, e = pendingExports.size();
      while (i < e) {
        Util.assertion(pendingExports.get(i).getNode().getNumOperands() > 1);
        if (pendingExports.get(i).getNode().getOperand(0).equals(root))
          break;

        i++;
      }
      if (i == e)
        pendingExports.add(root);
    }

    SDValue[] vals = new SDValue[pendingExports.size()];
    pendingExports.toArray(vals);
    root = dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), vals);
    pendingExports.clear();
    dag.setRoot(root);
    return root;
  }

  public void copyValueToVirtualRegister(Value val, int reg) {
    SDValue op = getValue(val);
    Util.assertion(op.getOpcode() != ISD.CopyFromReg ||
        ((RegisterSDNode) op.getOperand(1).getNode()).getReg() != reg, "Copy from a arg to the same reg");

    Util.assertion(!TargetRegisterInfo.isPhysicalRegister(reg), "Is a physical reg?");

    RegsForValue rfv = new RegsForValue(tli, reg, val.getType());
    SDValue chain = dag.getEntryNode();
    OutRef<SDValue> x = new OutRef<>(chain);
    rfv.getCopyToRegs(op, dag, x, null);
    chain = x.get();
    pendingExports.add(chain);
  }

  public SDValue getValue(Value val) {
    if (nodeMap.containsKey(val))
      return nodeMap.get(val);

    if (val instanceof Constant) {
      Constant cnt = (Constant) val;
      EVT vt = tli.getValueType(cnt.getType(), true);
      if (cnt instanceof ConstantInt) {
        SDValue n = dag.getConstant((ConstantInt) cnt, vt, false);
        nodeMap.put(val, n);
        return n;
      }
      if (cnt instanceof GlobalValue) {
        SDValue n = dag
            .getGlobalAddress((GlobalValue) cnt, vt, 0, false, 0);
        nodeMap.put(val, n);
        return n;
      }
      if (cnt instanceof ConstantPointerNull) {
        SDValue n = dag.getConstant(0, vt, false);
        nodeMap.put(val, n);
        return n;
      }
      if (cnt instanceof ConstantFP) {
        SDValue n = dag.getConstantFP((ConstantFP) cnt, vt, false);
        nodeMap.put(val, n);
        return n;
      }
      if (cnt instanceof Value.UndefValue) {
        SDValue n = dag.getUNDEF(vt);
        nodeMap.put(val, n);
        return n;
      }
      if (cnt instanceof ConstantExpr) {
        ConstantExpr ce = (ConstantExpr) cnt;
        visit(ce.getOpcode(), ce);
        SDValue n1 = nodeMap.get(val);
        Util.assertion(n1.getNode() != null);
        return n1;
      }

      if (cnt instanceof ConstantStruct || cnt instanceof ConstantArray) {
        ArrayList<SDValue> constants = new ArrayList<>();
        for (int i = 0, e = cnt.getNumOfOperands(); i < e; i++) {
          SDNode elt = getValue(cnt.operand(i)).getNode();
          for (int j = 0, ee = elt.getNumValues(); j < ee; j++)
            constants.add(new SDValue(elt, j));
        }
        return dag.getMergeValues(constants);
      }

      if (cnt.getType() instanceof StructType || cnt.getType() instanceof ArrayType) {
        Util.assertion(cnt instanceof ConstantAggregateZero || cnt instanceof Value.UndefValue, "Unknown struct or array constant!");


        ArrayList<EVT> valueVTs = new ArrayList<>();
        computeValueVTs(tli, cnt.getType(), valueVTs);
        int numElts = valueVTs.size();
        if (numElts == 0)
          return new SDValue();

        ArrayList<SDValue> constants = new ArrayList<>();
        for (int i = 0; i < numElts; i++) {
          EVT eltVT = valueVTs.get(i);
          if (cnt instanceof Value.UndefValue)
            constants.add(dag.getUNDEF(eltVT));
          else if (eltVT.isFloatingPoint())
            constants.add(dag.getConstantFP(0, eltVT, false));
          else
            constants.add(dag.getConstant(0, eltVT, false));
        }
        return dag.getMergeValues(constants);
      }

      Util.shouldNotReachHere("Vector type not supported!");
      return null;
    }
    if (val instanceof AllocaInst) {
      if (funcInfo.staticAllocaMap.containsKey(val))
        return dag.getFrameIndex(funcInfo.staticAllocaMap.get(val),
            new EVT(tli.getPointerTy()), false);
    }
    Util.assertion(funcInfo.valueMap.containsKey(val), "Value not in map!");
    int inReg = funcInfo.valueMap.get(val);
    RegsForValue rfv = new RegsForValue(tli, inReg, val.getType());
    SDValue chain = dag.getEntryNode();
    OutRef<SDValue> x = new OutRef<>(chain);
    SDValue res = rfv.getCopyFromRegs(dag, x, null);
    chain = x.get();
    return res;
  }

  public void setValue(Value val, SDValue sdVal) {
    Util.assertion(!nodeMap.containsKey(val), "Already set a value for this node!");
    nodeMap.put(val, sdVal);
  }

  public void lowerCallTo(CallSite cs, SDValue callee, boolean isTailCall) {
    lowerCallTo(cs, callee, isTailCall, null);
  }

  public void lowerCallTo(CallSite cs, SDValue callee, boolean isTailCall,
                          MachineBasicBlock landingPad) {
    PointerType pt = (PointerType) (cs.getCalledValue().getType());
    FunctionType fty = (FunctionType) pt.getElementType();
    MachineModuleInfo mmi = dag.getMachineModuleInfo();
    int beginLabel = 0, endLabel = 0;

    ArrayList<ArgListEntry> args = new ArrayList<>(cs.getNumOfArguments());

    for (int i = 0, e = cs.getNumOfArguments(); i < e; i++) {
      ArgListEntry entry = new ArgListEntry();
      SDValue arg = getValue(cs.getArgument(i));
      entry.node = arg;
      entry.ty = cs.getArgument(i).getType();

      int attrInd = i + 1;
      entry.isSExt = cs.paramHasAttr(attrInd, Attribute.SExt);
      entry.isZExt = cs.paramHasAttr(attrInd, Attribute.ZExt);
      entry.isInReg = cs.paramHasAttr(attrInd, Attribute.InReg);
      entry.isSRet = cs.paramHasAttr(attrInd, Attribute.StructRet);
      entry.isNest = cs.paramHasAttr(attrInd, Attribute.Nest);
      entry.isByVal = cs.paramHasAttr(attrInd, Attribute.ByVal);
      entry.alignment = cs.paramAlignment(attrInd);
      args.add(entry);
    }

    if (landingPad != null && mmi != null) {
      beginLabel = mmi.nextLabelID();
      getRoot();
      dag.setRoot(dag.getLabel(ISD.EH_LABEL, getControlRoot(), beginLabel));
    }

    if (isTailCall && !isInTailCallPosition(cs.getInstruction(),
        cs.getAttributes().getRetAttribute(), tli))
      isTailCall = false;

    Pair<SDValue, SDValue> result = tli.lowerCallTo(dag.getContext(), getRoot(), cs.getType(),
        cs.paramHasAttr(0, Attribute.SExt),
        cs.paramHasAttr(0, Attribute.ZExt), fty.isVarArg(),
        cs.paramHasAttr(0, Attribute.InReg), fty.getNumParams(),
        cs.getCallingConv(), isTailCall,
        !cs.getInstruction().isUseEmpty(), callee, args, dag);
    Util.assertion(!isTailCall || result.second.getNode() != null);
    Util.assertion(result.second.getNode() != null || result.first.getNode() == null);
    if (result.first.getNode() != null)
      setValue(cs.getInstruction(), result.first);

    if (result.second.getNode() != null)
      dag.setRoot(result.second);
    else
      hasTailCall = true;

    if (landingPad != null && mmi != null) {
      endLabel = mmi.getNextLabelID();
      dag.setRoot(dag.getLabel(ISD.EH_LABEL, getRoot(), endLabel));
      mmi.addInvoke(landingPad, beginLabel, endLabel);
    }
  }

  static boolean isInTailCallPosition(Instruction inst, int retAttr,
                                      TargetLowering tli) {
    BasicBlock exitBB = inst.getParent();
    TerminatorInst ti = exitBB.getTerminator();
    ReturnInst ret = ti instanceof ReturnInst ? (ReturnInst) ti : null;
    Function f = exitBB.getParent();

    if (ret == null && !(ti instanceof UnreachableInst))
      return false;

    if (inst.mayHasSideEffects() || inst.mayReadMemory() || !inst.isSafeToSpecutativelyExecute()) {
      for (int i = exitBB.size() - 2; ; --i) {
        if (exitBB.getInstAt(i).equals(inst))
          break;
        if (exitBB.getInstAt(i).mayHasSideEffects() || exitBB.getInstAt(i).mayReadMemory()
            || !exitBB.getInstAt(i).isSafeToSpecutativelyExecute())
          return false;
      }
    }

    if (ret == null || ret.getNumOfOperands() == 0)
      return true;

    if (f.getAttributes().getRetAttribute() != retAttr)
      return false;

    Instruction u = ret.operand(0) instanceof Instruction ?
        (Instruction) ret.operand(0) : null;
    while (true) {
      if (u == null)
        return false;

      if (!u.hasOneUses())
        return false;

      if (u.equals(inst))
        break;
      if (u instanceof TruncInst && tli
          .isTruncateFree(u.operand(0).getType(), u.getType())) {
        u = u.operand(0) instanceof Instruction ?
            (Instruction) u.operand(0) : null;
        continue;
      }
      return false;
    }
    return true;
  }

  @Override
  public Void visit(Instruction inst) {
    curDebugLoc = inst.getDebugLoc();
    visit(inst.getOpcode(), inst);
    curDebugLoc = new DebugLoc();
    return null;
  }

  public void visit(Operator opc, User u) {
    switch (opc) {
      case Ret:
        visitRet(u);
        break;
      case Br:
        visitBr(u);
        break;
      case Switch:
        visitSwitch(u);
        break;
      case Unreachable:
        // ignore unreachable instr.
        break;
      // add
      case Add:
        visitAdd(u);
        break;
      case FAdd:
        visitFAdd(u);
        break;
      // subtractive
      case Sub:
        visitSub(u);
        break;
      case FSub:
        visitFSub(u);
        break;
      // multiple
      case Mul:
        visitMul(u);
        break;
      case FMul:
        visitFMul(u);
        break;
      // division
      case UDiv:
        visitUDiv(u);
        break;
      case SDiv:
        visitSDiv(u);
        break;
      case FDiv:
        visitFDiv(u);
        break;
      // mod operation
      case URem:
        visitURem(u);
        break;
      case SRem:
        visitSRem(u);
        break;
      case FRem:
        visitFRem(u);
        break;
      // bit-operation
      case And:
        visitAnd(u);
        break;
      case Or:
        visitOr(u);
        break;
      case Xor:
        visitXor(u);
        break;
      // comparison operation
      case ICmp:
        visitICmp(u);
        break;
      case FCmp:
        visitFCmp(u);
        break;
      // shift operation
      case Shl:
        visitShl(u);
        break;
      case LShr:
        visitLShr(u);
        break;
      case AShr:
        visitAShr(u);
        break;
      // converts operation
      //truncate integers.
      case Trunc:
        visitTrunc(u);
        break;
      // zero extend integers.
      case ZExt:
        visitZExt(u);
        break;
      // Sign extend integers.
      case SExt:
        visitSExt(u);
        break;
      // floatint-pint to unsigned integer.
      case FPToUI:
        visitFPToUI(u);
        break;
      // floating point to signed integer.
      case FPToSI:
        visitFPToSI(u);
        break;
      // unsigned integer to floating-point.
      case UIToFP:
        visitUIToFP(u);
        break;
      // signed integer to floating-point.
      case SIToFP:
        visitSIToFP(u);
        break;
      // floating point truncate.
      case FPTrunc:
        visitFPTrunc(u);
        break;
      // float point extend.
      case FPExt:
        visitFPExt(u);
        break;
      // pointer to integer.
      case PtrToInt:
        visitPtrToInt(u);
        break;
      // Integer to pointer.
      case IntToPtr:
        visitIntToPtr(u);
        break;
      // type cast.
      case BitCast:
        visitBitCast(u);
        break;
      // memory operation
      case Alloca:
        visitAlloca(u);
        break;
      case Free:
        visitFree(u);
        break;
      case Malloc:
        visitMalloc(u);
        break;
      case Store:
        visitStore(u);
        break;
      case Load:
        visitLoad(u);
        break;
      // other operation
      case Phi:
        visitPhiNode(u);
        break;
      case Call:
        visitCall(u);
        break;
      case GetElementPtr:
        visitGetElementPtr(u);
        break;
      // Select instruction acts as ?: operator in C language.
      case Select:
        visitSelect(u);
        break;
      case ExtractElement:
        visitExtractElementInst(u);
        break;
      case InsertElement:
        visitInsertElementInst(u);
        break;
      case ShuffleVector:
        visitShuffleVectorInst(u);
        break;
      case ExtractValue:
        visitExtractVectorInst(u);
        break;
      case InsertValue:
        visitInsertValueInst(u);
        break;
      case Invoke:
      default:
        // TODO 1/16/2019
        Util.shouldNotReachHere("Unknown operator!");
    }
  }

  @Override
  public Void visitRet(User inst) {
    SDValue chain = getControlRoot();
    ReturnInst ret = (ReturnInst) inst;
    ArrayList<OutputArg> outs = new ArrayList<>(8);
    for (int i = 0, e = inst.getNumOfOperands(); i < e; i++) {
      ArrayList<EVT> valueVTs = new ArrayList<>();
      computeValueVTs(tli, ret.operand(i).getType(), valueVTs);
      int numValues = valueVTs.size();
      if (numValues <= 0)
        continue;
      SDValue retOp = getValue(ret.operand(i));
      for (int j = 0; j < numValues; j++) {
        EVT vt = valueVTs.get(j);

        int extendKind = ISD.ANY_EXTEND;
        Function f = ret.getParent().getParent();
        if (f.paramHasAttr(0, Attribute.SExt))
          extendKind = ISD.SIGN_EXTEND;
        else if (f.paramHasAttr(0, Attribute.ZExt))
          extendKind = ISD.ZERO_EXTEND;

        if (extendKind != ISD.ANY_EXTEND && vt.isInteger()) {
          EVT minVT = tli.getRegisterType(dag.getContext(), new EVT(MVT.i32));
          if (vt.bitsLT(minVT))
            vt = minVT;
        }

        int numParts = tli.getNumRegisters(vt);
        EVT partVT = tli.getRegisterType(dag.getContext(), vt);
        SDValue[] parts = new SDValue[numParts];
        getCopyToParts(dag,
            new SDValue(retOp.getNode(), retOp.getResNo() + j),
            parts, partVT, extendKind);

        ArgFlagsTy flags = new ArgFlagsTy();
        if (f.paramHasAttr(0, Attribute.InReg))
          flags.setInReg();

        if (f.paramHasAttr(0, Attribute.SExt))
          flags.setSExt();
        else if (f.paramHasAttr(0, Attribute.ZExt))
          flags.setZExt();
        for (int k = 0; k < numParts; k++)
          outs.add(new OutputArg(flags, parts[i], true));
      }
    }

    boolean isVarArg = dag.getMachineFunction().getFunction().isVarArg();
    CallingConv cc = dag.getMachineFunction().getFunction().getCallingConv();
    chain = tli.lowerReturn(chain, cc, isVarArg, outs, dag);
    Util.assertion(chain.getNode() != null && chain.getValueType().getSimpleVT().simpleVT == MVT.Other);

    dag.setRoot(chain);
    return null;
  }

  @Override
  public Void visitBr(User inst) {
    BranchInst bi = (BranchInst) inst;
    MachineBasicBlock succ0MBB = funcInfo.mbbmap.get(bi.getSuccessor(0));

    MachineBasicBlock nextMBB = null;
    int itr = funcInfo.mf.getIndexOfMBB(curMBB);
    if (++itr < funcInfo.mf.getNumBlocks())
      nextMBB = funcInfo.mf.getMBBAt(itr);

    if (bi.isUnconditional()) {
      curMBB.addSuccessor(succ0MBB);
      if (!Objects.equals(succ0MBB, nextMBB)) {
        dag.setRoot(dag.getNode(ISD.BR, new EVT(MVT.Other),
            getControlRoot(), dag.getBasicBlock(succ0MBB)));
      }
      return null;
    }

    Value condVal = bi.getCondition();
    MachineBasicBlock succ1MBB = funcInfo.mbbmap.get(bi.getSuccessor(1));

    if (condVal instanceof BinaryOperator) {
      BinaryOperator bo = (BinaryOperator) condVal;
      if (bo.hasOneUses() && (bo.getOpcode() == And || bo.getOpcode() == Or)) {
        findMergedConditions(bo, succ0MBB, succ1MBB, curMBB, bo.getOpcode());

        Util.assertion(switchCases.isEmpty() || switchCases.get(0).thisMBB.equals(curMBB),
            "Unexpected lowering!");

        if (shouldEmitAsBranches(switchCases)) {
          switchCases.forEach(cb -> {
            exportFromCurrentBlock(cb.cmpLHS);
            exportFromCurrentBlock(cb.cmpRHS);
          });
          visitSwitchCase(switchCases.get(0));
          switchCases.remove(0);
          return null;
        }

        for (int i = 1, e = switchCases.size(); i < e; i++) {
          funcInfo.mf.erase(switchCases.get(i).thisMBB);
        }
        switchCases.clear();
      }
    }
    CaseBlock cb = new CaseBlock(CondCode.SETEQ, condVal, ConstantInt.getTrue(dag.getContext()),
        null, succ0MBB, succ1MBB, curMBB);
    visitSwitchCase(cb);
    return null;
  }

  private boolean shouldEmitAsBranches(ArrayList<CaseBlock> cases) {
    if (cases.size() != 2)
      return true;

    CaseBlock cb0 = cases.get(0), cb1 = cases.get(1);
    if (cb0.cmpLHS.equals(cb1.cmpLHS) && cb0.cmpRHS.equals(cb1.cmpRHS) || (
        cb0.cmpRHS.equals(cb1.cmpLHS) && cb0.cmpLHS.equals(cb1.cmpRHS)))
      return false;

    return true;
  }

  private void exportFromCurrentBlock(Value val) {
    if (!(val instanceof Instruction) & !(val instanceof Argument))
      return;

    if (funcInfo.isExportedInst(val))
      return;

    int reg = funcInfo.initializeRegForValue(val);
    copyValueToVirtualRegister(val, reg);
  }

  private void findMergedConditions(Value cond,
                                    MachineBasicBlock tbb,
                                    MachineBasicBlock fbb,
                                    MachineBasicBlock curBB,
                                    Operator opc) {
    Instruction inst = cond instanceof Instruction ? (Instruction)cond : null;
    if (inst == null || !(inst instanceof BinaryOperator ||
        inst instanceof CmpInst) ||
        inst.getOpcode() != opc || !inst.hasOneUses() ||
        inst.getParent() != curBB.getBasicBlock() ||
        !inBlock(inst.operand(0), curBB.getBasicBlock()) ||
        !inBlock(inst.operand(1), curBB.getBasicBlock())) {
      emitBranchForMergedCondition(cond, tbb, fbb, curBB);
      return;
    }

    MachineFunction mf = dag.getMachineFunction();
    int itr = mf.getIndexOfMBB(curBB);
    MachineBasicBlock tempBB = mf.createMachineBasicBlock(curBB.getBasicBlock());
    curBB.getParent().insert(++itr, tempBB);

    if (opc == Or) {
      // Codegen X | Y as:
      //   jmp_if_X TBB
      //   jmp TmpBB
      // TmpBB:
      //   jmp_if_Y TBB
      //   jmp FBB
      findMergedConditions(inst.operand(0), tbb, tempBB, curBB, opc);
      findMergedConditions(inst.operand(1), tbb, fbb, tempBB, opc);
    } else {
      Util.assertion(opc == And);
      // Codegen X & Y as:
      //   jmp_if_X TmpBB
      //   jmp FBB
      // TmpBB:
      //   jmp_if_Y TBB
      //   jmp FBB
      //
      //  This requires creation of TmpBB after CurBB.
      findMergedConditions(inst.operand(0), tempBB, fbb, curBB, opc);
      findMergedConditions(inst.operand(1), tbb, fbb, tempBB, opc);
    }
  }

  private static boolean inBlock(Value val, BasicBlock bb) {
    if (val instanceof Instruction) {
      return ((Instruction) val).getParent().equals(bb);
    }
    return false;
  }

  private void emitBranchForMergedCondition(Value cond, MachineBasicBlock tbb,
                                            MachineBasicBlock fbb, MachineBasicBlock curBB) {
    BasicBlock bb = curBB.getBasicBlock();

    if (cond instanceof CmpInst) {
      CmpInst ci = (CmpInst) cond;
      if (Objects.equals(curBB, curMBB) || (
          isExportableFromCurrentBlock(ci.operand(0), bb)
              && isExportableFromCurrentBlock(ci.operand(1), bb))) {
        CondCode c;
        if (cond instanceof ICmpInst) {
          c = getICmpCondCode(ci.getPredicate());
        } else if (cond instanceof FCmpInst) {
          c = getFCmpCondCode(ci.getPredicate());
        } else {
          c = CondCode.SETEQ;
          Util.shouldNotReachHere("Unknown compare instruction!");
        }
        CaseBlock cb = new CaseBlock(c, ci.operand(0), ci.operand(1),
            null, tbb, fbb, curBB);
        switchCases.add(cb);
      }
    }

    CaseBlock cb = new CaseBlock(CondCode.SETEQ, cond, ConstantInt.getTrue(dag.getContext()),
        null, tbb, fbb, curBB);
    switchCases.add(cb);
  }

  private boolean isExportableFromCurrentBlock(Value val, BasicBlock fromBB) {
    if (val instanceof Instruction) {
      Instruction vi = (Instruction) val;
      return vi.getParent().equals(fromBB) || funcInfo.isExportedInst(val);
    }

    if (val instanceof Argument) {
      return fromBB.equals(fromBB.getParent().getEntryBlock()) || funcInfo
          .isExportedInst(val);
    }
    return true;
  }

  public void visitSwitchCase(CaseBlock cb) {
    SDValue cond;
    SDValue condLHS = getValue(cb.cmpLHS);
    if (cb.cmpMHS == null) {
      if (cb.cmpRHS.equals(ConstantInt.getTrue(dag.getContext())) && cb.cc == CondCode.SETEQ) {
        cond = condLHS;
      }
      else if (cb.cmpRHS.equals(ConstantInt.getFalse(dag.getContext())) & cb.cc == CondCode.SETEQ) {
        SDValue one = dag.getConstant(1, condLHS.getValueType(), false);
        cond = dag.getNode(ISD.XOR, condLHS.getValueType(), condLHS, one);
      } else {
        cond = dag.getSetCC(new EVT(MVT.i1), condLHS, getValue(cb.cmpRHS),
            cb.cc);
      }
    } else {
      Util.assertion(cb.cc == CondCode.SETLE);
      APInt low = ((ConstantInt) cb.cmpLHS).getValue();
      APInt high = ((ConstantInt) cb.cmpRHS).getValue();

      SDValue cmpOp = getValue(cb.cmpRHS);
      EVT vt = cmpOp.getValueType();

      if (((ConstantInt) cb.cmpLHS).isMinValue(true)) {
        cond = dag.getSetCC(new EVT(MVT.i1), cmpOp,
            dag.getConstant(high, vt, false), CondCode.SETLE);
      } else {
        SDValue sub = dag.getNode(ISD.SUB, vt, cmpOp,
            dag.getConstant(low, vt, false));
        cond = dag.getSetCC(new EVT(MVT.i1), sub,
            dag.getConstant(high.sub(low), vt, false),
            CondCode.SETULE);
      }
    }

    curMBB.addSuccessor(cb.trueMBB);
    curMBB.addSuccessor(cb.falseMBB);

    MachineBasicBlock nextBlock = null;
    int idx = curMBB.getParent().getIndexOfMBB(curMBB);
    if (++idx < curMBB.getParent().getNumBlocks())
      nextBlock = curMBB.getParent().getMBBAt(idx);

    if (cb.trueMBB.equals(nextBlock)) {
      MachineBasicBlock temp = cb.trueMBB;
      cb.trueMBB = cb.falseMBB;
      cb.falseMBB = temp;
      SDValue one = dag.getConstant(1, condLHS.getValueType(), false);
      cond = dag.getNode(ISD.XOR, cond.getValueType(), cond, one);
    }

    SDValue brCond = dag.getNode(ISD.BRCOND, new EVT(MVT.Other), getControlRoot(),
        cond, dag.getBasicBlock(cb.trueMBB));

    // Insert the false branch. Do this even if it's a fall through branch,
    // this makes it easier to do DAG optimizations which require inverting
    // the branch condition.
      dag.setRoot(dag.getNode(ISD.BR, new EVT(MVT.Other),
          brCond, dag.getBasicBlock(cb.falseMBB)));
  }

  @Override
  public Void visitSwitch(User inst) {
    SwitchInst si = (SwitchInst) inst;
    MachineBasicBlock nextBB = null;
    int idx = curMBB.getParent().getIndexOfMBB(curMBB);
    if (++idx < curMBB.getParent().getNumBlocks())
      nextBB = curMBB.getParent().getMBBAt(idx);

    MachineBasicBlock defaultBB = funcInfo.mbbmap.get(si.getDefaultBlock());
    if (si.getNumOfOperands() == 2) {
      // equivalent to unconditional branch.
      curMBB.addSuccessor(defaultBB);
      if (!defaultBB.equals(nextBB)) {
        dag.setRoot(dag.getNode(ISD.BR, new EVT(MVT.Other),
            getControlRoot(), dag.getBasicBlock(defaultBB)));
        return null;
      }
    }

    ArrayList<Case> cases = new ArrayList<>();
    clusterify(cases, si);

    Value condVal = si.operand(0);
    Stack<CaseRec> worklist = new Stack<>();
    worklist.add(new CaseRec(curMBB, null, null, cases));
    while (!worklist.isEmpty()) {
      CaseRec cr = worklist.pop();

      if (handleBitTestsSwitchCase(cr, condVal, defaultBB))
        continue;

      if (handleSmallSwitchRange(cr, condVal, defaultBB))
        continue;

      if (handleJTSwitchCase(cr, condVal, defaultBB))
        continue;

      handleBTSplitSwitchCase(cr, worklist, condVal, defaultBB);
    }
    return null;
  }

  private int clusterify(ArrayList<Case> cases, SwitchInst si) {
    int numCmps = 0;

    for (int i = 1, e = si.getNumOfSuccessors(); i < e; i++) {
      MachineBasicBlock mbb = funcInfo.mbbmap.get(si.getSuccessor(i));
      cases.add(new Case(si.getSuccessorValue(i), si.getSuccessorValue(i),
          mbb));
    }
    // sort the case in order of low value of lhs less than high value of rhs.
    cases.sort((c1, c2) -> {
      long res = c1.low.getValue().sub(c2.high.getValue()).getSExtValue();
      return res == 0 ? 0 : res < 0 ? -1 : 1;
    });

    // merge case into cluster.
    if (cases.size() >= 2) {
      for (int i = 0, j = i + 1; j < cases.size(); ) {
        APInt nextVal = cases.get(j).low.getValue();
        APInt currentVal = cases.get(i).high.getValue();

        MachineBasicBlock nextBB = cases.get(j).mbb;
        MachineBasicBlock curBB = cases.get(i).mbb;

        if (nextVal.sub(currentVal).eq(1) && curBB.equals(nextBB)) {
          cases.get(i).high = cases.get(j).high;
          cases.remove(j);
        } else {
          i = j++;
        }
      }

      for (int i = 0, e = cases.size(); i < e; i++, ++numCmps) {
        if (!cases.get(i).low.getValue().eq(cases.get(i).high.getValue()))
          ++numCmps;
      }
    }
    return numCmps;
  }

  private boolean handleBitTestsSwitchCase(CaseRec cr, Value condVal,
                                           MachineBasicBlock defaultMBB) {
    EVT pty = new EVT(tli.getPointerTy());
    int intPtrBits = pty.getSizeInBits();

    int frontCaseIdx = 0;
    int backCaseIdx = cr.caseRanges.size();

    MachineFunction mf = funcInfo.mf;

    if (!tli.isOperationLegal(ISD.SHL, new EVT(tli.getPointerTy())))
      return false;

    int numCmps = 0;
    for (int i = frontCaseIdx; i < backCaseIdx; i++) {
      Case c = cr.caseRanges.get(i);
      numCmps += c.low.getValue().eq(c.high.getValue()) ? 1 : 2;
    }

    HashSet<MachineBasicBlock> dests = new HashSet<>();
    for (Case c : cr.caseRanges) {
      dests.add(c.mbb);
      if (dests.size() > 3)
        return false;
    }

    APInt minVal = cr.caseRanges.get(frontCaseIdx).low.getValue();
    APInt maxVal = cr.caseRanges.get(backCaseIdx-1).high.getValue();

    APInt cmpRange = maxVal.sub(minVal);

    if (cmpRange.uge(new APInt(cmpRange.getBitWidth(), intPtrBits)) || (
        !(dests.size() == 1 && numCmps >= 3) && !(dests.size() == 2
            && numCmps >= 5) && !(dests.size() >= 3 && numCmps >= 6)))
      return false;

    APInt lowBound = APInt.getNullValue(cmpRange.getBitWidth());

    if (minVal.isNonNegative() && maxVal
        .slt(new APInt(maxVal.getBitWidth(), intPtrBits))) {
      cmpRange = maxVal;
    } else {
      lowBound = minVal;
    }

    ArrayList<CaseBits> caseBits = new ArrayList<>();
    int i = 0, count = 0;

    for (Case c : cr.caseRanges) {
      MachineBasicBlock dest = c.mbb;
      for (i = 0; i < count; i++)
        if (dest.equals(caseBits.get(i).mbb))
          break;

      if (i == count) {
        Util.assertion(count < 3);
        caseBits.add(new CaseBits(0, dest, 0));
        count++;
      }

      APInt lowValue = c.low.getValue();
      APInt highValue = c.high.getValue();

      long lo = lowValue.sub(lowBound).getZExtValue();
      long hi = highValue.sub(lowBound).getZExtValue();

      for (long j = lo; j <= hi; ++j) {
        caseBits.get(i).mask |= 1L << j;
        caseBits.get(i).bits++;
      }
    }

    caseBits.sort((c1, c2) -> {
      return c1.bits - c2.bits;
    });

    ArrayList<BitTestCase> btc = new ArrayList<>();
    int itr = 1;    // iterator for cr.mbb
    BasicBlock llvmBB = cr.mbb.getBasicBlock();

    for (int j = 0, e = caseBits.size(); j < e; j++) {
      MachineBasicBlock caseBB = mf.createMachineBasicBlock(llvmBB);
      mf.insert(itr, caseBB);
      btc.add(new BitTestCase(caseBits.get(j).mask, caseBB, caseBits.get(j).mbb));

      exportFromCurrentBlock(condVal);
    }

    BitTestBlock btb = new BitTestBlock(lowBound, cmpRange, condVal, -1, cr.mbb.equals(curMBB), cr.mbb, defaultMBB,
        btc);
    if (cr.mbb.equals(curMBB))
      visitBitTestHeader(btb);

    bitTestCases.add(btb);
    return true;
  }

  private boolean handleSmallSwitchRange(CaseRec cr, Value val,
                                         MachineBasicBlock defaultMBB) {
    Case backCase = cr.caseRanges.get(cr.caseRanges.size() - 1);

    int size = cr.caseRanges.size();
    if (size > 3)
      return false;

    MachineFunction mf = funcInfo.mf;
    MachineBasicBlock nextBlock = null;
    int itr = mf.getIndexOfMBB(cr.mbb);

    if (++itr < mf.size())
      nextBlock = mf.getMBBAt(itr);

    if (nextBlock != null && !defaultMBB.equals(nextBlock) &&
        !backCase.mbb.equals(nextBlock)) {
      for (int i = 0, e = cr.caseRanges.size() - 1; i < e; i++) {
        if (cr.caseRanges.get(i).mbb.equals(nextBlock)) {
          Case t = cr.caseRanges.get(i);
          cr.caseRanges.set(i, backCase);
          cr.caseRanges.set(cr.caseRanges.size() - 1, t);
          backCase = t;
          break;
        }
      }
    }

    MachineBasicBlock curBlock = cr.mbb;
    for (int i = 0, e = cr.caseRanges.size(); i < e; i++) {
      MachineBasicBlock fallThrough = null;
      if (!cr.caseRanges.get(i).equals(cr.caseRanges.get(e - 1))) {
        fallThrough = mf.createMachineBasicBlock(curBlock.getBasicBlock());
        mf.insert(itr++, fallThrough);

        exportFromCurrentBlock(val);
      } else {
        fallThrough = defaultMBB;
      }

      Value rhs, lhs, mhs;
      CondCode cc;
      if (cr.caseRanges.get(i).high.equals(cr.caseRanges.get(i).low)) {
        cc = CondCode.SETEQ;
        lhs = val;
        rhs = cr.caseRanges.get(i).high;
        mhs = null;
      } else {
        cc = CondCode.SETLE;
        lhs = cr.caseRanges.get(i).low;
        mhs = val;
        rhs = cr.caseRanges.get(i).high;
      }
      CaseBlock cb = new CaseBlock(cc, lhs, rhs, mhs, cr.caseRanges.get(i).mbb, fallThrough, curBlock);

      if (curBlock.equals(curMBB))
        visitSwitchCase(cb);
      else
        switchCases.add(cb);

      curBlock = fallThrough;
    }
    return true;
  }

  private boolean handleJTSwitchCase(CaseRec cr, Value val,
                                     MachineBasicBlock defaultMBB) {
    int frontCaseIdx = 0, backCaseIdx = cr.caseRanges.size();

    APInt first = cr.caseRanges.get(0).low.getValue();
    APInt last = cr.caseRanges.get(backCaseIdx-1).high.getValue();

    int tsize = 0;
    for (Case c : cr.caseRanges)
      tsize += c.size();

    if (!areJTsAllowed(tli) || tsize <= 3)
      return false;

    APInt range = computeRange(first, last);
    double density = tsize / range.roundToDouble();
    if (density < 0.4)
      return false;

    MachineFunction mf = funcInfo.mf;
    MachineBasicBlock nextBB = null;
    int itr = mf.getIndexOfMBB(cr.mbb);

    if (++itr < mf.size())
      nextBB = mf.getMBBAt(itr);

    BasicBlock llvmBB = cr.mbb.getBasicBlock();

    MachineBasicBlock jumpTableBB = mf.createMachineBasicBlock(llvmBB);
    mf.insert(itr, jumpTableBB);
    cr.mbb.addSuccessor(defaultMBB);
    cr.mbb.addSuccessor(jumpTableBB);

    ArrayList<MachineBasicBlock> destMBBs = new ArrayList<>();
    APInt tei = new APInt(first);
    for (int i = 0, e = cr.caseRanges.size(); i < e; tei.increase()) {
      Case c = cr.caseRanges.get(i);
      APInt low = c.low.getValue();
      APInt high = c.high.getValue();
      if (low.sle(tei) && tei.sle(high)) {
        destMBBs.add(c.mbb);
        if (tei.eq(high))
          i++;
      } else {
        destMBBs.add(defaultMBB);
      }
    }

    BitMap succHandled = new BitMap(cr.mbb.getParent().getNumBlocks());
    for (MachineBasicBlock mbb : destMBBs) {
      if (!succHandled.get(mbb.getNumber())) {
        succHandled.set(mbb.getNumber(), true);
        jumpTableBB.addSuccessor(mbb);
      }
    }

    MachineJumpTableInfo.JTEntryKind encoding = tli.getJumpTableEncoding();
    int jti = mf.getOrCreateJumpTableInfo(encoding).getJumpTableIndex(destMBBs);

    JumpTable jt = new JumpTable(-1, jti, jumpTableBB, defaultMBB);
    JumpTableHeader jht = new JumpTableHeader(first, last, val, cr.mbb,
        cr.mbb.equals(curMBB));
    if (cr.mbb.equals(curMBB))
      visitJumpTableHeader(jt, jht);

    jtiCases.add(Pair.get(jht, jt));
    return true;
  }

  public void visitJumpTableHeader(JumpTable jt, JumpTableHeader jht) {
    SDValue switchOp = getValue(jht.val);
    EVT vt = switchOp.getValueType();
    SDValue sub = dag.getNode(ISD.SUB, vt, switchOp, dag.getConstant(jht.first, vt, false));


    if (vt.bitsGT(new EVT(tli.getPointerTy())))
      switchOp = dag.getNode(ISD.TRUNCATE, new EVT(tli.getPointerTy()), sub);
    else
      switchOp = dag.getNode(ISD.ZERO_EXTEND, new EVT(tli.getPointerTy()), sub);

    int jumpTableReg = funcInfo.makeReg(new EVT(tli.getPointerTy()));
    SDValue copyTo = dag.getCopyToReg(getControlRoot(), jumpTableReg, switchOp);

    jt.reg = jumpTableReg;

    SDValue cmp = dag.getSetCC(
        new EVT(tli.getSetCCResultType(sub.getValueType())),
        sub,
        dag.getConstant(jht.last.sub(jht.first), vt, false),
        CondCode.SETUGT);

    MachineBasicBlock nextBlock = null;
    int itr = funcInfo.mf.getIndexOfMBB(curMBB);
    if (++itr < funcInfo.mf.size())
      nextBlock = funcInfo.mf.getMBBAt(itr);

    SDValue brCond = dag.getNode(ISD.BRCOND, new EVT(MVT.Other),
        copyTo, cmp, dag.getBasicBlock(jt.defaultMBB));

    if (jt.mbb.equals(nextBlock))
      dag.setRoot(brCond);
    else
      dag.setRoot(dag.getNode(ISD.BR, new EVT(MVT.Other), brCond,
          dag.getBasicBlock(jt.mbb)));
  }

  public void visitJumpTable(JumpTable jt) {
    Util.assertion(jt.reg != -1, "Should lower JT header first!");
    EVT pty = new EVT(tli.getPointerTy());
    SDValue index = dag.getCopyFromReg(getControlRoot(), jt.reg, pty);
    SDValue table = dag.getJumpTable(jt.jti, pty, false, 0);
    dag.setRoot(dag.getNode(ISD.BR_JT, new EVT(MVT.Other),
        index.getValue(1), table, index));
  }

  private boolean handleBTSplitSwitchCase(CaseRec cr, Stack<CaseRec> worklist,
                                          Value val, MachineBasicBlock defaultMBB) {
    MachineFunction mf = funcInfo.mf;

    MachineBasicBlock nextBB = null;
    int itr = mf.getIndexOfMBB(cr.mbb);
    if (++itr < mf.size())
      nextBB = mf.getMBBAt(itr);

    int frontCaseIdx = 0;
    int backCaseIdx = cr.caseRanges.size();
    BasicBlock llvmBB = cr.mbb.getBasicBlock();

    int size = cr.caseRanges.size();

    APInt first = cr.caseRanges.get(frontCaseIdx).low.getValue();
    APInt last = cr.caseRanges.get(backCaseIdx - 1).high.getValue();
    double fmetric = 0.0;
    int pivot = size / 2;

    int tsize = 0;
    for (Case c : cr.caseRanges)
      tsize += c.size();

    int lsize = (int) cr.caseRanges.get(frontCaseIdx).size();
    int rsize = tsize - lsize;
    for (int i = 0, j = i + 1, e = cr.caseRanges.size(); j < e; ++i, j++) {
      APInt lend = cr.caseRanges.get(i).high.getValue();
      APInt rbegin = cr.caseRanges.get(j).low.getValue();
      APInt range = computeRange(lend, rbegin);
      Util.assertion(range.sub(2).isNonNegative());

      double ldensity = lsize / (lend.sub(first).add(1)).roundToDouble();
      double rdensity = rsize / (last.sub(rbegin).add(1)).roundToDouble();
      double metric = range.logBase2() * (ldensity + rdensity);
      if (fmetric < metric) {
        pivot = j;
        fmetric = metric;
      }

      lsize += cr.caseRanges.get(j).size();
      rsize -= cr.caseRanges.get(j).size();
    }

    if (areJTsAllowed(tli)) {
      Util.assertion(fmetric > 0);
    } else {
      pivot = size / 2;
    }

    ArrayList<Case> lhsr = new ArrayList<>(cr.caseRanges.subList(0, pivot));
    ArrayList<Case> rhsr = new ArrayList<>(cr.caseRanges.subList(pivot, cr.caseRanges.size()));

    ConstantInt c = cr.caseRanges.get(pivot).low;
    MachineBasicBlock falseBB = null, trueBB = null;

    if (lhsr.size() == 1 && Objects.equals(lhsr.get(0).high, cr.high)
        && c.getValue().eq(cr.high.getValue().add(1))) {
      trueBB = lhsr.get(0).mbb;
    } else {
      trueBB = mf.createMachineBasicBlock(llvmBB);
      mf.insert(itr, trueBB);
      worklist.add(new CaseRec(trueBB, c, cr.high, lhsr));

      exportFromCurrentBlock(val);
    }

    if (rhsr.size() == 1 && cr.low != null && rhsr.get(0).low.getValue()
        .eq(cr.low.getValue().sub(1))) {
      falseBB = rhsr.get(0).mbb;
    } else {
      falseBB = mf.createMachineBasicBlock(llvmBB);
      mf.insert(itr, falseBB);
      worklist.add(new CaseRec(falseBB, cr.low, c, rhsr));

      exportFromCurrentBlock(val);
    }

    CaseBlock cb = new CaseBlock(CondCode.SETLT, val, c, null, trueBB,
        falseBB, cr.mbb);
    if (cr.mbb.equals(curMBB))
      visitSwitchCase(cb);
    else
      switchCases.add(cb);

    return true;
  }

  private static boolean areJTsAllowed(TargetLowering tli) {
    return !DisableJumpTables.value && (tli.isOperationLegalOrCustom(ISD.BR_JT, new EVT(MVT.Other))
        || tli.isOperationLegalOrCustom(ISD.BRIND, new EVT(MVT.Other)));
  }

  private static APInt computeRange(APInt first, APInt last) {
    APInt lastExt = new APInt(last), firstExt = new APInt(first);
    int bitWidth = Math.max(last.getBitWidth(), first.getBitWidth()) + 1;
    lastExt.sext(bitWidth);
    firstExt.sext(bitWidth);
    return lastExt.sub(firstExt).add(1);
  }

  void visitBitTestHeader(BitTestBlock btb) {
    SDValue switchOp = getValue(btb.val);
    EVT vt = switchOp.getValueType();
    SDValue sub = dag.getNode(ISD.SUB, vt, switchOp, dag.getConstant(btb.first, vt, false));

    SDValue rangeCmp = dag.getSetCC(
        new EVT(tli.getSetCCResultType(sub.getValueType())),
        sub, dag.getConstant(btb.range, vt, false),
        CondCode.SETUGT);
    SDValue shiftOp;
    if (vt.bitsGT(new EVT(tli.getPointerTy())))
      shiftOp = dag.getNode(ISD.TRUNCATE, new EVT(tli.getPointerTy()),
          sub);
    else
      shiftOp = dag.getNode(ISD.ZERO_EXTEND, new EVT(tli.getPointerTy()),
          sub);

    btb.reg = funcInfo.makeReg(new EVT(tli.getPointerTy()));
    SDValue copyTo = dag.getCopyToReg(getControlRoot(), btb.reg,
        shiftOp);

    MachineBasicBlock nextBlock = null;
    int itr = funcInfo.mf.getIndexOfMBB(curMBB);
    if (++itr < funcInfo.mf.size())
      nextBlock = funcInfo.mf.getMBBAt(itr);

    MachineBasicBlock mbb = btb.cases.get(0).thisMBB;

    curMBB.addSuccessor(btb.defaultMBB);
    curMBB.addSuccessor(mbb);

    SDValue brRange = dag.getNode(ISD.BRCOND, new EVT(MVT.Other),
        copyTo, rangeCmp, dag.getBasicBlock(btb.defaultMBB));
    if (mbb.equals(nextBlock))
      dag.setRoot(brRange);
    else
      dag.setRoot(dag.getNode(ISD.BR, new EVT(MVT.Other),
          copyTo, dag.getBasicBlock(mbb)));
  }

  public void visitBitTestCase(MachineBasicBlock nextMBB, int reg, BitTestCase btc) {
    SDValue shiftOp = dag.getCopyFromReg(getControlRoot(),
        reg, new EVT(tli.getPointerTy()));
    SDValue switchVal = dag.getNode(ISD.SHL, new EVT(tli.getPointerTy()),
        dag.getConstant(1, new EVT(tli.getPointerTy()), false),
        shiftOp);
    SDValue andOp = dag.getNode(ISD.AND, new EVT(tli.getPointerTy()),
        switchVal, dag.getConstant(btc.mask, new EVT(tli.getPointerTy()), false));
    SDValue andCMp = dag.getSetCC(
        new EVT(tli.getSetCCResultType(andOp.getValueType())),
        andOp, dag.getConstant(0, new EVT(tli.getPointerTy()), false),
        CondCode.SETNE);
    curMBB.addSuccessor(btc.targetMBB);
    curMBB.addSuccessor(nextMBB);

    SDValue brAnd = dag.getNode(ISD.BRCOND, new EVT(MVT.Other),
        getControlRoot(), andCMp, dag.getBasicBlock(btc.targetMBB));

    MachineBasicBlock nextBlock = curMBB;
    int itr = funcInfo.mf.getIndexOfMBB(curMBB);
    if (++itr < funcInfo.mf.size())
      nextBlock = funcInfo.mf.getMBBAt(itr);

    if (nextBlock.equals(nextMBB))
      dag.setRoot(brAnd);
    else
      dag.setRoot(dag.getNode(ISD.BR, new EVT(MVT.Other), brAnd,
          dag.getBasicBlock(nextMBB)));
  }

  private int getSDOpc(Operator opc) {
    switch (opc) {
      case Add:
        return ISD.ADD;
      case FAdd:
        return ISD.FADD;
      case Sub:
        return ISD.SUB;
      case FSub:
        return ISD.FSUB;
      case Mul:
        return ISD.MUL;
      case FMul:
        return ISD.FMUL;
      case URem:
        return ISD.UREM;
      case SRem:
        return ISD.SREM;
      case FRem:
        return ISD.FREM;
      case SDiv:
        return ISD.SDIV;
      case UDiv:
        return ISD.UDIV;
      case FDiv:
        return ISD.FDIV;
      case And:
        return ISD.AND;
      case Or:
        return ISD.OR;
      case Xor:
        return ISD.XOR;
      case Shl:
        return ISD.SHL;
      case AShr:
        return ISD.SRL;
      case LShr:
        return ISD.SRA;
      default:
        Util.assertion("Unknown binary operator!");
        return -1;
    }
  }

  @Override
  public Void visitBinaryOp(User inst) {
    Operator op = inst instanceof Instruction ?
        ((Instruction) inst).getOpcode() : ((ConstantExpr) inst).getOpcode();
    SDValue op0 = getValue(inst.operand(0));
    SDValue op1 = getValue(inst.operand(1));
    int opc = getSDOpc(op);
    Util.assertion(opc >= 0);
    if (op.isShift()) {
      if (!op1.getValueType().getSimpleVT().equals(tli.getShiftAmountTy())) {
        EVT pty = new EVT(tli.getPointerTy());
        EVT sty = new EVT(tli.getShiftAmountTy());
        if (sty.bitsGT(op1.getValueType()))
          op1 = dag.getNode(ISD.ANY_EXTEND, sty, op1);
        else if (sty.getSizeInBits() >= Util.log2Ceil(op1.getValueType().getSizeInBits())) {
          op1 = dag.getNode(ISD.TRUNCATE, sty, op1);
        } else if (pty.bitsLT(op1.getValueType()))
          op1 = dag.getNode(ISD.TRUNCATE, pty, op1);
        else if (pty.bitsGT(op1.getValueType()))
          op1 = dag.getNode(ISD.ANY_EXTEND, pty, op1);
      }
    }
    setValue(inst, dag.getNode(opc, op0.getValueType(), op0, op1));
    return null;
  }

  private CondCode getICmpCondCode(Predicate pred) {
    switch (pred) {
      case ICMP_EQ:
        return CondCode.SETEQ;
      case ICMP_NE:
        return CondCode.SETNE;
      case ICMP_SLE:
        return CondCode.SETLE;
      case ICMP_ULE:
        return CondCode.SETULE;
      case ICMP_SGE:
        return CondCode.SETGE;
      case ICMP_UGE:
        return CondCode.SETUGE;
      case ICMP_SLT:
        return CondCode.SETLT;
      case ICMP_ULT:
        return CondCode.SETULT;
      case ICMP_SGT:
        return CondCode.SETGT;
      case ICMP_UGT:
        return CondCode.SETUGT;
      default:
        Util.shouldNotReachHere("Invalid ICmp predicate opcode!");
        ;
        return CondCode.SETNE;
    }
  }

  private CondCode getFCmpCondCode(Predicate pred) {
    CondCode fpc, foc;
    switch (pred) {
      case FCMP_FALSE:
        fpc = foc = CondCode.SETFALSE;
        break;
      case FCMP_OEQ:
        foc = CondCode.SETEQ;
        fpc = CondCode.SETOEQ;
        break;
      case FCMP_OGT:
        foc = CondCode.SETGT;
        fpc = CondCode.SETOGT;
        break;
      case FCMP_OGE:
        foc = CondCode.SETGE;
        fpc = CondCode.SETOGE;
        break;
      case FCMP_OLT:
        foc = CondCode.SETLT;
        fpc = CondCode.SETOLT;
        break;
      case FCMP_OLE:
        foc = CondCode.SETLE;
        fpc = CondCode.SETOLE;
        break;
      case FCMP_ONE:
        foc = CondCode.SETNE;
        fpc = CondCode.SETONE;
        break;
      case FCMP_ORD:
        foc = fpc = CondCode.SETO;
        break;
      case FCMP_UNO:
        foc = fpc = CondCode.SETUO;
        break;
      case FCMP_UEQ:
        foc = CondCode.SETEQ;
        fpc = CondCode.SETUEQ;
        break;
      case FCMP_UGT:
        foc = CondCode.SETGT;
        fpc = CondCode.SETUGT;
        break;
      case FCMP_UGE:
        foc = CondCode.SETGE;
        fpc = CondCode.SETUGE;
        break;
      case FCMP_ULT:
        foc = CondCode.SETLT;
        fpc = CondCode.SETULT;
        break;
      case FCMP_ULE:
        foc = CondCode.SETLE;
        fpc = CondCode.SETULE;
        break;
      case FCMP_UNE:
        foc = CondCode.SETNE;
        fpc = CondCode.SETUNE;
        break;
      case FCMP_TRUE:
        foc = fpc = CondCode.SETTRUE;
        break;
      default:
        Util.shouldNotReachHere("Invalid predicate for FCmp instruction!");
        foc = fpc = CondCode.SETFALSE;
        break;
    }
    return BackendCmdOptions.finiteOnlyFPMath() ? foc : fpc;
  }

  @Override
  public Void visitICmp(User inst) {
    Predicate pred = inst instanceof CmpInst ? ((CmpInst) inst).getPredicate() :
        ((ConstantExpr) inst).getPredicate();
    SDValue op1 = getValue(inst.operand(0));
    SDValue op2 = getValue(inst.operand(1));
    CondCode opc = getICmpCondCode(pred);
    EVT destVT = tli.getValueType(inst.getType());
    setValue(inst, dag.getSetCC(destVT, op1, op2, opc));
    return null;
  }

  @Override
  public Void visitFCmp(User inst) {
    Predicate pred = inst instanceof CmpInst ?
        ((CmpInst) inst).getPredicate() :
        ((ConstantExpr) inst).getPredicate();
    SDValue op1 = getValue(inst.operand(0));
    SDValue op2 = getValue(inst.operand(1));
    CondCode opc = getFCmpCondCode(pred);
    EVT destVT = tli.getValueType(inst.getType());
    setValue(inst, dag.getSetCC(destVT, op1, op2, opc));
    return null;
  }

  @Override
  public Void visitTrunc(User inst) {
    EVT destVT = tli.getValueType(inst.getType());
    SDValue op1 = getValue(inst.operand(0));
    setValue(inst, dag.getNode(ISD.TRUNCATE, destVT, op1));
    return null;
  }

  @Override
  public Void visitZExt(User inst) {
    EVT destVT = tli.getValueType(inst.getType());
    SDValue op1 = getValue(inst.operand(0));
    setValue(inst, dag.getNode(ISD.ZERO_EXTEND, destVT, op1));
    return null;
  }

  @Override
  public Void visitSExt(User inst) {
    EVT destVT = tli.getValueType(inst.getType());
    SDValue op1 = getValue(inst.operand(0));
    setValue(inst, dag.getNode(ISD.SIGN_EXTEND, destVT, op1));
    return null;
  }

  @Override
  public Void visitFPToUI(User inst) {
    EVT destVT = tli.getValueType(inst.getType());
    SDValue op1 = getValue(inst.operand(0));
    setValue(inst, dag.getNode(ISD.FP_TO_UINT, destVT, op1));
    return null;
  }

  @Override
  public Void visitFPToSI(User inst) {
    EVT destVT = tli.getValueType(inst.getType());
    SDValue op1 = getValue(inst.operand(0));
    setValue(inst, dag.getNode(ISD.FP_TO_SINT, destVT, op1));
    return null;
  }

  @Override
  public Void visitUIToFP(User inst) {
    EVT destVT = tli.getValueType(inst.getType());
    SDValue op1 = getValue(inst.operand(0));
    setValue(inst, dag.getNode(ISD.UINT_TO_FP, destVT, op1));
    return null;
  }

  @Override
  public Void visitSIToFP(User inst) {
    EVT destVT = tli.getValueType(inst.getType());
    SDValue op1 = getValue(inst.operand(0));
    setValue(inst, dag.getNode(ISD.SINT_TO_FP, destVT, op1));
    return null;
  }

  @Override
  public Void visitFPTrunc(User inst) {
    EVT destVT = tli.getValueType(inst.getType());
    SDValue op1 = getValue(inst.operand(0));
    setValue(inst, dag.getNode(ISD.FP_ROUND, destVT, op1));
    return null;
  }

  @Override
  public Void visitFPExt(User inst) {
    EVT destVT = tli.getValueType(inst.getType());
    SDValue op1 = getValue(inst.operand(0));
    setValue(inst, dag.getNode(ISD.FP_EXTEND, destVT, op1));
    return null;
  }

  @Override
  public Void visitPtrToInt(User inst) {
    EVT destVT = tli.getValueType(inst.getType());
    EVT srcVT = tli.getValueType(inst.operand(0).getType());
    int opc;
    if (destVT.getSizeInBits() < srcVT.getSizeInBits())
      opc = ISD.TRUNCATE;
    else
      opc = ISD.ZERO_EXTEND;
    SDValue op1 = getValue(inst.operand(0));
    setValue(inst, dag.getNode(opc, destVT, op1));
    return null;
  }

  @Override
  public Void visitIntToPtr(User inst) {
    EVT destVT = tli.getValueType(inst.getType());
    EVT srcVT = tli.getValueType(inst.operand(0).getType());
    int opc;
    if (destVT.getSizeInBits() < srcVT.getSizeInBits())
      opc = ISD.TRUNCATE;
    else
      opc = ISD.ZERO_EXTEND;
    SDValue op1 = getValue(inst.operand(0));
    setValue(inst, dag.getNode(opc, destVT, op1));
    return null;
  }

  @Override
  public Void visitBitCast(User inst) {
    EVT destVT = tli.getValueType(inst.getType());
    SDValue op1 = getValue(inst.operand(0));
    EVT srcVT = tli.getValueType(inst.operand(0).getType());
    if (!destVT.equals(srcVT))
      setValue(inst, dag.getNode(ISD.BIT_CONVERT, destVT, op1));
    else
      setValue(inst, op1);
    return null;
  }

  @Override
  public Void visitAlloca(User inst) {
    AllocaInst ai = (AllocaInst) inst;

    if (funcInfo.staticAllocaMap.containsKey(ai))
      return null;
    Type ty = ai.getAllocatedType();
    long size = tli.getTargetData().getTypeAllocSize(ty);
    int align = Math.max(tli.getTargetData().getPrefTypeAlignment(ty), ai.getAlignment());
    SDValue allocaSize = getValue(ai.getArraySize());
    allocaSize = dag.getNode(ISD.MUL, allocaSize.getValueType(), allocaSize,
        dag.getConstant(size, allocaSize.getValueType(), false));

    EVT intPtr = new EVT(tli.getPointerTy());
    if (intPtr.bitsLT(allocaSize.getValueType()))
      allocaSize = dag.getNode(ISD.TRUNCATE, intPtr, allocaSize);
    else if (intPtr.bitsGT(allocaSize.getValueType()))
      allocaSize = dag.getNode(ISD.ZERO_EXTEND, intPtr, allocaSize);

    int stackAlign = tli.getTargetMachine().getFrameLowering().getStackAlignment();
    if (align <= stackAlign)
      align = 0;

    allocaSize = dag.getNode(ISD.ADD, allocaSize.getValueType(), allocaSize,
        dag.getIntPtrConstant(stackAlign - 1));
    allocaSize = dag.getNode(ISD.AND, allocaSize.getValueType(), allocaSize,
        dag.getIntPtrConstant(~(stackAlign - 1)));
    SDValue[] ops = {getRoot(), allocaSize, dag.getIntPtrConstant(align)};
    SDNode.SDVTList vts = dag.getVTList(allocaSize.getValueType(), new EVT(MVT.Other));
    SDValue dsa = dag.getNode(ISD.DYNAMIC_STACKALLOC, vts, ops);
    setValue(inst, dsa);
    dag.setRoot(dsa.getValue(1));

    // Inform the Frame Information that we have just allocated a variable-sized
    // object.
    funcInfo.mf.getFrameInfo().createVariableSizedObject();
    return null;
  }

  @Override
  public Void visitMalloc(User inst) {
    Util.shouldNotReachHere("Not implemented currently!");
    return null;
  }

  @Override
  public Void visitLoad(User inst) {
    LoadInst li = (LoadInst) inst;
    Value sv = li.operand(0);
    SDValue ptr = getValue(sv);

    Type ty = li.getType();
    boolean isVolatile = li.isVolatile();
    int alignment = li.getAlignment();

    ArrayList<EVT> valueVTs = new ArrayList<>();
    TLongArrayList offsets = new TLongArrayList();
    computeValueVTs(tli, ty, valueVTs, offsets);

    if (valueVTs.isEmpty())
      return null;

    SDValue root = new SDValue();
    boolean constantMemory = false;
    if (li.isVolatile()) {
      root = getRoot();
    } else if (aa != null && aa.pointsToConstantMemory(sv)) {
      root = dag.getEntryNode();
      constantMemory = true;
    } else {
      root = dag.getRoot();
    }

    ArrayList<SDValue> values = new ArrayList<>(valueVTs.size());
    ArrayList<SDValue> chains = new ArrayList<>(valueVTs.size());
    EVT ptrVT = ptr.getValueType();
    for (int i = 0; i < valueVTs.size(); i++) {
      SDValue l = dag.getLoad(valueVTs.get(i), root,
          dag.getNode(ISD.ADD, ptrVT, ptr, dag.getConstant(offsets.get(i), ptrVT, false)),
          sv, (int) offsets.get(i), isVolatile, alignment);
      values.add(l);
      chains.add(l.getValue(1));
    }
    if (!constantMemory) {
      SDValue chain = dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), chains);
      if (isVolatile)
        dag.setRoot(chain);
      else
        addPendingLoad(chain);
    }
    setValue(li, dag.getNode(ISD.MERGE_VALUES, dag.getVTList(valueVTs), values));
    return null;
  }

  @Override
  public Void visitStore(User inst) {
    StoreInst si = (StoreInst) inst;
    Value srcVal = si.operand(0);
    Value ptrVal = si.operand(1);

    ArrayList<EVT> valueVTs = new ArrayList<>();
    TLongArrayList offsets = new TLongArrayList();
    computeValueVTs(tli, srcVal.getType(), valueVTs, offsets);
    int numValues = valueVTs.size();
    if (numValues == 0)
      return null;

    SDValue src = getValue(srcVal);
    SDValue ptr = getValue(ptrVal);

    SDValue root = getRoot();
    ArrayList<SDValue> chains = new ArrayList<>(numValues);
    EVT ptrVT = ptr.getValueType();
    boolean isVolatile = si.isVolatile();
    int align = si.getAlignment();
    for (int i = 0; i < numValues; i++) {
      chains.add(dag.getStore(root, new SDValue(src.getNode(), src.getResNo() + i),
          dag.getNode(ISD.ADD, ptrVT, ptr, dag.getConstant(offsets.get(i), ptrVT, false)),
          ptrVal, (int) offsets.get(i), isVolatile, align));
    }
    dag.setRoot(dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), chains));
    return null;
  }

  @Override
  public Void visitCall(User inst) {
    String renameFn = null;
    CallInst ci = (CallInst) inst;
    Function f = ci.getCalledFunction();
    if (f != null) {
      if (f.isDeclaration()) {
        TargetIntrinsicInfo ii = tli.getTargetMachine().getIntrinsinsicInfo();
        if (ii != null) {
          Intrinsic.ID iid = ii.getIntrinsicID(f);
          if (iid != not_intrinsic) {
            renameFn = visitIntrinsicCall(ci, iid);
            if (renameFn == null)
              return null;
          }
        }
        Intrinsic.ID iid = f.getIntrinsicID();
        if (iid != not_intrinsic) {
          renameFn = visitIntrinsicCall(ci, iid);
          if (renameFn == null)
            return null;
        }
      }
      if (!f.hasLocalLinkage() && f.hasName()) {
        String name = f.getName();
        switch (name) {
          case "copysign":
          case "copysignf": {
            if (ci.getNumOfOperands() == 3 &&
                ci.operand(1).getType().isFloatingPointType() &&
                ci.getType().equals(ci.operand(1).getType()) &&
                ci.getType().equals(ci.operand(2).getType())) {
              SDValue lhs = getValue(ci.operand(1));
              SDValue rhs = getValue(ci.operand(2));
              setValue(ci, dag.getNode(ISD.FCOPYSIGN, lhs.getValueType(),
                  lhs, rhs));
              return null;
            }
            break;
          }
          case "fabs":
          case "fabsf": {
            if (ci.getNumOfOperands() == 2 &&
                ci.operand(1).getType().isFloatingPointType() &&
                ci.getType().equals(ci.operand(1).getType())) {
              SDValue lhs = getValue(ci.operand(1));
              setValue(ci, dag.getNode(ISD.FABS, lhs.getValueType(), lhs));
              return null;
            }
            break;
          }
          case "sin":
          case "sinf": {
            if (ci.getNumOfOperands() == 2 &&
                ci.operand(1).getType().isFloatingPointType() &&
                ci.getType().equals(ci.operand(1).getType())) {
              SDValue lhs = getValue(ci.operand(1));
              setValue(ci, dag.getNode(ISD.FSIN, lhs.getValueType(), lhs));
              return null;
            }
            break;
          }
          case "cos":
          case "cosf":
          case "cosl": {
            if (ci.getNumOfOperands() == 2 &&
                ci.operand(1).getType().isFloatingPointType() &&
                ci.getType().equals(ci.operand(1).getType())) {
              SDValue lhs = getValue(ci.operand(1));
              setValue(ci, dag.getNode(ISD.FCOS, lhs.getValueType(), lhs));
              return null;
            }
            break;
          }
        }
      }
    }
    SDValue callee;
    if (renameFn == null)
      callee = getValue(inst.operand(0));
    else
      callee = dag.getExternalSymbol(renameFn, new EVT(tli.getPointerTy()));

    boolean isTailCall = EnablePerformTailCallOpt.value && ci.isTailCall();
    lowerCallTo(new CallSite(ci), callee, isTailCall);
    return null;
  }

  /**
   * Lower the call to the specified intrinsic function. If we want to emit
   * this as a call to a named external function, return the name. Otherwise
   * it and return null.
   *
   * @param ci
   * @param iid
   * @return
   */
  private String visitIntrinsicCall(CallInst ci, Intrinsic.ID iid) {
    DebugLoc dl = getCurDebugLoc();

    switch (iid) {
      default:
        // turns on call to intrinsic to target specified handler.
        visitTargetIntrinsic(ci, iid);
        return null;
      case vastart:
        visitVAStart(ci);
        return null;
      case vaend:
        visitVAEnd(ci);
        return null;
      case vacopy:
        visitVACopy(ci);
        return null;
      case returnaddress:
        setValue(ci, dag.getNode(ISD.RETURNADDR, new EVT(tli.getPointerTy()),
            getValue(ci.operand(1))));
        return null;
      case frameaddress:
        setValue(ci, dag.getNode(ISD.FRAMEADDR, new EVT(tli.getPointerTy()),
            getValue(ci.operand(1))));
        return null;
      case setjmp:
        return "_setjmp" + (tli.isUseUnderscoreSetJmp() ? "0" : "1");
      case longjmp:
        return "_longjmp" + (tli.isUseUnderscoreLongJmp() ? "0" : "1");
      case memset: {
        // Assert for address < 256 since we support only user defined address
        // spaces.
        Util.assertion(((PointerType) ci.getArgOperand(0).getType()).getAddressSpace() < 256, "Unknown address space");

        SDValue op1 = getValue(ci.getArgOperand(0));
        SDValue op2 = getValue(ci.getArgOperand(1));
        SDValue op3 = getValue(ci.getArgOperand(2));
        int align = (int) ((ConstantInt) ci.getArgOperand(3)).getZExtValue();
        dag.setRoot(dag.getMemset(getRoot(), op1, op2, op3,
            align, ci.getArgOperand(0), 0));
        return null;
      }
      case memcpy: {
        // Assert for address < 256 since we support only user defined address
        // spaces.
        Util.assertion(((PointerType) ci.getArgOperand(0).getType()).getAddressSpace() < 256 &&
            ((PointerType) ci.getArgOperand(1).getType()).getAddressSpace() < 256, "Unknown address space");

        SDValue op1 = getValue(ci.getArgOperand(0));
        SDValue op2 = getValue(ci.getArgOperand(1));
        SDValue op3 = getValue(ci.getArgOperand(2));
        int align = (int) ((ConstantInt) ci.getArgOperand(3)).getZExtValue();
        boolean isVolatile = ((ConstantInt) ci.getArgOperand(4)).getZExtValue() == 1;
        dag.setRoot(dag.getMemcpy(getRoot(), op1, op2, op3,
            align, false, ci.getArgOperand(0),
            0, ci.getArgOperand(1), 0));

        return null;
      }
      case memmove: {
        // Assert for address < 256 since we support only user defined address
        // spaces.
        Util.assertion(((PointerType) ci.getArgOperand(0).getType()).getAddressSpace() < 256 &&
            ((PointerType) ci.getArgOperand(1).getType()).getAddressSpace() < 256, "Unknown address space");

        SDValue op1 = getValue(ci.getArgOperand(0));
        SDValue op2 = getValue(ci.getArgOperand(1));
        SDValue op3 = getValue(ci.getArgOperand(2));
        int align = (int) ((ConstantInt) ci.getArgOperand(3)).getZExtValue();

        // If the source and destination are known to not be aliases, we can
        // lower memmove as memcpy.
        long size = -1;
        if (op3.getNode() instanceof SDNode.ConstantSDNode) {
          size = ((SDNode.ConstantSDNode) op3.getNode()).getZExtValue();
        }
        if (aa != null && aa.alias(ci.getArgOperand(0), (int) size,
            ci.getArgOperand(1), (int) size) == AliasResult.NoAlias) {
          dag.setRoot(dag.getMemcpy(getRoot(), op1, op2, op3, align, false, ci.getArgOperand(0),
              0, ci.getArgOperand(1), 0));
          return null;
        }

        dag.setRoot(dag.getMemmove(getRoot(), op1, op2, op3,
            align, ci.getArgOperand(0), 0, ci.getArgOperand(1), 0));
        return null;
      }
      case sqrt:
        setValue(ci, dag.getNode(ISD.FSQRT, getValue(ci.operand(1)).getValueType(),
            getValue(ci.operand(1))));
        return null;
      case sin:
        setValue(ci, dag.getNode(ISD.FSIN, getValue(ci.operand(1)).getValueType(),
            getValue(ci.operand(1))));
      case cos:
        setValue(ci, dag.getNode(ISD.FCOS, getValue(ci.operand(1)).getValueType(),
            getValue(ci.operand(1))));
      case log:
      case log2:
      case log10:
      case exp:
      case exp2:
      case pow:
        Util.shouldNotReachHere("unimplemented math function!");
        return null;
      case pcmarker:
        SDValue tmp = getValue(ci.operand(1));
        dag.setRoot(dag.getNode(ISD.PCMARKER, new EVT(MVT.Other),
            getRoot(), tmp));
        return null;
      case bswap:
        setValue(ci, dag.getNode(ISD.BSWAP,
            getValue(ci.operand(1)).getValueType(),
            getValue(ci.operand(1))));
        return null;
      case cttz: {
        SDValue arg = getValue(ci.operand(1));
        EVT vt = arg.getValueType();
        setValue(ci, dag.getNode(ISD.CTTZ, vt, arg));
        return null;
      }
      case ctlz: {
        SDValue arg = getValue(ci.operand(1));
        EVT vt = arg.getValueType();
        setValue(ci, dag.getNode(ISD.CTLZ, vt, arg));
        return null;
      }
      case ctpop: {
        SDValue arg = getValue(ci.operand(1));
        EVT vt = arg.getValueType();
        setValue(ci, dag.getNode(ISD.CTPOP, vt, arg));
        return null;
      }
      case stacksave: {
        SDValue op = getValue(ci.operand(1));
        SDValue res = dag.getNode(ISD.STACKSAVE,
            dag.getVTList(new EVT(tli.getPointerTy()), new EVT(MVT.Other)), op);
        setValue(ci, res);
        dag.setRoot(res.getValue(1));
        return null;
      }
      case stackrestore: {
        SDValue res = getValue(ci.operand(1));
        dag.setRoot(dag.getNode(ISD.STACKRESTORE, new EVT(MVT.Other), getRoot(),
            res));
        return null;
      }
      case uadd_with_overflow:
        return implVisitAluOverflow(ci, ISD.UADDO);
      case sadd_with_overflow:
        return implVisitAluOverflow(ci, ISD.SADDO);
      case usub_with_overflow:
        return implVisitAluOverflow(ci, ISD.USUBO);
      case ssub_with_overflow:
        return implVisitAluOverflow(ci, ISD.SSUBO);
      case umul_with_overflow:
        return implVisitAluOverflow(ci, ISD.UMULO);
      case smul_with_overflow:
        return implVisitAluOverflow(ci, ISD.SMULO);
      case invariant_start:
      case lifetime_start:
        // discard it
        setValue(ci, dag.getUNDEF(new EVT(tli.getPointerTy())));
        return null;
      case invariant_end:
      case lifetime_end:
        return null;
      case dbg_declare: {
        DbgDeclareInst di = (DbgDeclareInst) ci;
        MDNode variable = di.getVariable();
        Value address = di.getAddress();
        if (address == null || !new DIVariable(di.getVariable()).verify())
          return null;

        // Build an entry in DbgOrdering.  Debug info input nodes get an SDNodeOrder
        // but do not always have a corresponding SDNode built.  The SDNodeOrder
        // absolute, but not relative, values are different depending on whether
        // debug info exists.
        ++sdNodeOrder;

        if (address instanceof Value.UndefValue ||
            (address.isUseEmpty() && !(address instanceof Argument))) {
          // dbg declare for an undef value, just return a null.
          SDDbgValue sdv = dag.getDbgValue(variable, Value.UndefValue.get(address.getType()),
              0, dl, sdNodeOrder);
          dag.addDbgValue(sdv, null, false);
          return null;
        }

        if ((!nodeMap.containsKey(address) || nodeMap.get(address).getNode() == null) &&
            address instanceof Argument)
          nodeMap.put(address, unusedArgNodeMap.containsKey(address) ?
              unusedArgNodeMap.get(address) : new SDValue());
        SDDbgValue sdv;
        SDValue n = nodeMap.get(address);
        if (n.getNode() != null) {
          // Parameters are handled specially.
          boolean isParameter = new DIVariable(variable).getTag() == Dwarf.DW_TAG_arg_variable;
          if (address instanceof BitCastInst)
            address = ((BitCastInst) address).operand(0);

          if (isParameter && !(address instanceof AllocationInst)) {
            if (n.getNode() instanceof SDNode.FrameIndexSDNode) {
              SDNode.FrameIndexSDNode fiNode = (SDNode.FrameIndexSDNode) n.getNode();
              sdv = dag.getDbgValue(variable, fiNode.getFrameIndex(), 0, dl, sdNodeOrder);
            } else {
              // Can't do anything with other non-AI cases yet.  This might be a
              // parameter of a callee function that got inlined, for example.
              return null;
            }
          } else if (address instanceof AllocationInst) {
            sdv = dag.getDbgValue(variable, n.getNode(), n.getResNo(), 0, dl, sdNodeOrder);
          } else
            return null;
          dag.addDbgValue(sdv, n.getNode(), isParameter);
        } else {
          // If Address is an arugment then try to emits its dbg value using
          // virtual register info from the FuncInfo.ValueMap. Otherwise add undef
          // to help track missing debug info.
          if (!emitFuncArgumentDbgValue(address, variable, 0, n)) {
            sdv = dag.getDbgValue(variable, Value.UndefValue.get(address.getType()),
                0, dl, sdNodeOrder);
            dag.addDbgValue(sdv, null, false);
          }
        }
        return null;
      }
      case dbg_value: {
        DbgValueInst di = (DbgValueInst)ci;
        if (!new DIVariable(di.getVariable()).verify())
          return null;

        MDNode variable = di.getVariable();
        long offset = di.getOffset();
        Value v = di.getValue();
        if (v == null) return null;

        // Build an entry in DbgOrdering.  Debug info input nodes get an SDNodeOrder
        // but do not always have a corresponding SDNode built.  The SDNodeOrder
        // absolute, but not relative, values are different depending on whether
        // debug info exists.
        ++sdNodeOrder;
        SDDbgValue sdv;
        if (v instanceof ConstantInt || v instanceof ConstantFP) {
          sdv = dag.getDbgValue(variable, v, offset, dl, sdNodeOrder);
          dag.addDbgValue(sdv, null, false);
        }
        else {
          // Do not use getValue() in here; we don't want to generate code at
          // this point if it hasn't been done yet.
          SDValue n = nodeMap.get(v);
          if (n.getNode() == null && v instanceof Argument) {
            n = unusedArgNodeMap.get(v);
          }
          if (n.getNode() != null) {
            if (!emitFuncArgumentDbgValue(v, variable, offset, n)) {
              sdv = dag.getDbgValue(variable, n.getNode(), n.getResNo(),
                  offset, dl, sdNodeOrder);
              dag.addDbgValue(sdv, n.getNode(), false);
            }
          }
          else if (v instanceof PhiNode && !v.isUseEmpty()) {
            // Do not call getValue(V) yet, as we don't want to generate code.
            // Remember it for later.
            DanglingDebugInfo ddi = new DanglingDebugInfo(di, dl, sdNodeOrder);
            danglingDebugInfoMap.put(v, ddi);
          }
          else {
            // We may expand this to cover more cases.  One case where we have no
            // data available is an unreferenced parameter; we need this fallback.
            sdv = dag.getDbgValue(variable, Value.UndefValue.get(v.getType()),
                offset, dl, sdNodeOrder);
            dag.addDbgValue(sdv, null, false);
          }
        }

        // build a debug info table entry.
        if (v instanceof BitCastInst)
          v = ((BitCastInst)v).operand(0);
        // don't handle byval struct arguments or VLAs.
        if (!(v instanceof AllocaInst))
          return null;

        AllocaInst ai = (AllocaInst) v;
        if (!funcInfo.staticAllocaMap.containsKey(ai))
          return null;

        int fi = funcInfo.staticAllocaMap.get(ai);
        MachineModuleInfo mmi = dag.getMachineModuleInfo();
        if (!di.getDebugLoc().isUnknown() && mmi.hasDebugInfo())
          mmi.setVariableDgbInfo(variable, fi, di.getDebugLoc());
        return null;
      }
    }
  }

  /**
   * If the DbgValueInst is a dbg_value of a function
   * argument, create the corresponding DBG_VALUE machine instruction for it now.
   * At the end of instruction selection, they will be inserted to the entry BB.
   * @param value
   * @param variable
   * @param offset
   * @param n
   * @return
   */
  private boolean emitFuncArgumentDbgValue(Value value, MDNode variable,
                                           long offset, SDValue n) {
    if (!(value instanceof Argument))
      return false;
    Argument arg = (Argument) value;
    MachineFunction mf = dag.getMachineFunction();
    DIVariable dv = new DIVariable(variable);
    if (!dv.isInlinedFnArgument(mf.getFunction()))
      return false;
    MachineBasicBlock mbb = funcInfo.mbb;
    if (mbb.equals(mf.getEntryBlock()))
      return false;

    int reg = 0;
    if (arg.hasByValAttr()) {
      // Byval arguments' frame index is recorded during argument lowering.
      // Use this info directly.
      TargetRegisterInfo tri = dag.getTarget().getRegisterInfo();
      reg = tri.getFrameRegister(mf);
      offset = funcInfo.getByValArgumentFrameIndex(arg);
    }

    if (n.getNode() != null && n.getOpcode() == ISD.CopyFromReg) {
      reg = ((RegisterSDNode)n.getOperand(1).getNode()).getReg();
      if (reg != 0 && TargetRegisterInfo.isVirtualRegister(reg)) {
        MachineRegisterInfo mri = mf.getMachineRegisterInfo();
        int pr = mri.getliveInPhysReg(reg);
        if (pr != 0) reg = pr;
      }
    }

    if (reg == 0) {
      if (!funcInfo.valueMap.containsKey(value))
        return false;
      reg = funcInfo.valueMap.get(value);
    }

    TargetInstrInfo tii = dag.getTarget().getInstrInfo();
    MachineInstrBuilder mib = MachineInstrBuilder.buildMI(tii.get(TargetOpcodes.DBG_VALUE),
        getCurDebugLoc()).addReg(reg, MachineOperand.RegState.Debug)
        .addImm(offset).addMetadata(variable);
    funcInfo.argDbgValues.add(mib.getMInstr());
    return false;
  }

  private void visitVACopy(CallInst ci) {
    dag.setRoot(dag.getNode(ISD.VACOPY, new EVT(MVT.Other), getRoot(),
        getValue(ci.operand(0)), getValue(ci.operand(2)),
        dag.getSrcValue(ci.operand(1)),
        dag.getSrcValue(ci.operand(2))));
  }

  private void visitVAEnd(CallInst ci) {
    dag.setRoot(dag.getNode(ISD.VAEND, new EVT(MVT.Other), getRoot(),
        getValue(ci.operand(0)), dag.getSrcValue(ci.operand(1))));
  }

  private void visitVAStart(CallInst ci) {
    dag.setRoot(dag.getNode(ISD.VASTART, new EVT(MVT.Other), getRoot(),
        getValue(ci.operand(1)), dag.getSrcValue(ci.operand(1))));
  }

  private void visitTargetIntrinsic(CallInst ci, Intrinsic.ID iid) {
    boolean hasChain = !ci.doesNotAccessMemory();
    boolean onlyLoad = hasChain && ci.onlyReadsMemory();

    ArrayList<SDValue> ops = new ArrayList<>();
    if (hasChain) {
      if (onlyLoad)
        ops.add(dag.getRoot());
      else
        ops.add(getRoot());
    }

    IntrinsicInfo info = new IntrinsicInfo();
    boolean isTargetIntrinsic = tli.getTargetMemIntrinsic(info, ci, iid);
    if (!isTargetIntrinsic)
      ops.add(dag.getConstant(iid.ordinal(), new EVT(tli.getPointerTy()), false));

    for (int i = 1, e = ci.getNumOfOperands(); i < e; i++) {
      SDValue op = getValue(ci.operand(i));
      Util.assertion(tli.isTypeLegal(op.getValueType()), "Intrinsic uses a non-legal type?");

      ops.add(op);
    }
    ArrayList<EVT> valueVTs = new ArrayList<>();
    computeValueVTs(tli, ci.getType(), valueVTs);
    if (hasChain)
      valueVTs.add(new EVT(MVT.Other));

    SDNode.SDVTList vts = dag.getVTList(valueVTs);
    SDValue result;
    if (isTargetIntrinsic) {
      result = dag.getMemIntrinsicNode(info.opc, vts, ops, info.memVT, info.ptrVal,
          info.offset, info.align, info.vol, info.readMem, info.writeMem);
    } else if (!hasChain)
      result = dag.getNode(ISD.INTRINSIC_WO_CHAIN, vts, ops);
    else if (!ci.getType().isVoidType())
      result = dag.getNode(ISD.INTRINSIC_W_CHAIN, vts, ops);
    else
      result = dag.getNode(ISD.INTRINSIC_VOID, vts, ops);

    if (hasChain) {
      SDValue chain = result.getValue(result.getNode().getNumValues() - 1);
      if (onlyLoad)
        addPendingLoad(chain);
      else
        dag.setRoot(chain);
    }
    if (!ci.getType().isVoidType()) {
      setValue(ci, result);
    }
  }

  @Override
  public Void visitGetElementPtr(User inst) {
    SDValue node = getValue(inst.operand(0));
    Type ty = inst.operand(0).getType();
    for (int i = 1, e = inst.getNumOfOperands(); i < e; i++) {
      Value idx = inst.operand(i);
      if (ty instanceof StructType) {
        long field = ((ConstantInt) idx).getZExtValue();
        StructType sty = (StructType) ty;
        if (field != 0) {
          // N = N + offset;
          long offset = td.getStructLayout(sty).getElementOffset(field);
          node = dag.getNode(ISD.ADD, node.getValueType(), node,
              dag.getIntPtrConstant(offset));
        }
        ty = sty.getElementType((int) field);
      } else {
        ty = ((SequentialType) ty).getElementType();
        if (idx instanceof ConstantInt) {
          ConstantInt ci = (ConstantInt) idx;
          long val = ci.getZExtValue();
          if (val == 0)
            continue;
          long offset = td.getTypeAllocSize(ty) * ci.getSExtValue();
          SDValue offsVal;
          EVT pty = new EVT(tli.getPointerTy());
          int ptrBits = pty.getSizeInBits();
          if (ptrBits < 64) {
            offsVal = dag.getNode(ISD.TRUNCATE, pty, dag.getConstant(offset,
                new EVT(MVT.i64), false));
          } else {
            offsVal = dag.getIntPtrConstant(offset);
          }
          node = dag.getNode(ISD.ADD, node.getValueType(), node, offsVal);
          continue;
        }

        // node = node + idx * eltSize;
        long eltSize = td.getTypeAllocSize(ty);
        SDValue idxN = getValue(idx);

        if (idxN.getValueType().bitsLT(node.getValueType())) {
          idxN = dag.getNode(ISD.SIGN_EXTEND, node.getValueType(), idxN);
        } else if (idxN.getValueType().bitsGT(node.getValueType())) {
          idxN = dag.getNode(ISD.TRUNCATE, node.getValueType(), idxN);
        }

        if (eltSize != 1) {
          if (Util.isPowerOf2(eltSize)) {
            int amt = Util.log2(eltSize);
            idxN = dag.getNode(ISD.SHL, node.getValueType(),
                idxN, dag.getConstant(amt, new EVT(tli.getPointerTy()), false));
          } else {
            SDValue scale = dag.getIntPtrConstant(eltSize);
            idxN = dag.getNode(ISD.MUL, node.getValueType(), idxN, scale);
          }
        }
        node = dag.getNode(ISD.ADD, node.getValueType(), node, idxN);
      }
    }
    setValue(inst, node);
    return null;
  }

  @Override
  public Void visitPhiNode(User inst) {
    //PHI handled specially!;
    return null;
  }

  public Void visitSelect(User u) {
    ArrayList<EVT> valueVTs = new ArrayList<>();
    computeValueVTs(tli, u.getType(), valueVTs);
    if (!valueVTs.isEmpty()) {
      SDValue[] values = new SDValue[valueVTs.size()];
      SDValue cond = getValue(u.operand(0));
      SDValue trueVal = getValue(u.operand(1));
      SDValue falseVal = getValue(u.operand(2));
      for (int i = 0; i < valueVTs.size(); i++) {
        values[i] = dag.getNode(ISD.SELECT,
            trueVal.getValueType(), cond,
            new SDValue(trueVal.getNode(), trueVal.getResNo() + i),
            new SDValue(falseVal.getNode(), falseVal.getResNo() + i));
      }
      setValue(u, dag.getNode(ISD.MERGE_VALUES, dag.getVTList(valueVTs),
          values));
    }
    return null;
  }

  @Override
  public Void visitFree(User inst) {
    Util.shouldNotReachHere("Not implemented currently!");
    return null;
  }

  @Override
  public Void visitExtractElementInst(User inst) {
    SDValue inVec = getValue(inst.operand(0));
    SDValue inIdx = dag.getNode(ISD.ZERO_EXTEND, new EVT(tli.getPointerTy()),
        getValue(inst.operand(1)));
    setValue(inst, dag.getNode(ISD.EXTRACT_VECTOR_ELT, tli.getValueType(inst.getType()),
        inVec, inIdx));
    return null;
  }

  @Override
  public Void visitInsertElementInst(User inst) {
    SDValue inVec = getValue(inst.operand(0));
    SDValue inVal = getValue(inst.operand(1));
    SDValue inIdx = dag.getNode(ISD.ZERO_EXTEND, new EVT(tli.getPointerTy()),
        getValue(inst.operand(2)));
    setValue(inst, dag.getNode(ISD.INSERT_VECTOR_ELT, tli.getValueType(inst.getType()),
        inVec, inVal, inIdx));
    return null;
  }

  @Override
  public Void visitShuffleVectorInst(User inst) {
    SDValue src1 = getValue(inst.operand(0));
    SDValue src2 = getValue(inst.operand(1));

    TIntArrayList mask = new TIntArrayList();
    ShuffleVectorInst.getShuffleMask((Constant)inst.operand(2), mask);

    EVT vt = tli.getValueType(inst.getType());
    EVT srcVT = src1.getValueType();
    int srcNumElts = srcVT.getVectorNumElements();
    int maskNumElts = mask.size();
    if (srcNumElts == maskNumElts) {
      setValue(inst, dag.getVectorShuffle(vt, src1, src2, mask.toArray()));
      return null;
    }
    // TODO
    return null;
  }

  private static int computeLinearIndex(Type ty,
                                        int[] indices) {
    return computeLinearIndex(ty, indices, 0);
  }

  /**
   * Given an LLVM IR aggregate type and a sequence of insertvalue or extractvalue
   * indices that identify a member, return the linearized index of the start of
   * the member.
   * @param ty
   * @param indices
   * @param curIndex
   * @return
   */
  private static int computeLinearIndex(Type ty,
                                        int[] indices,
                                        int curIndex) {
    return computeLinearIndex(ty, indices, 0, indices.length, curIndex);
  }

  private static int computeLinearIndex(Type ty,
                                        int[] indices,
                                        int indexBegin,
                                        int indexEnd,
                                        int curIndex) {
    // base case, we are done.
    if (indices != null && indexBegin == indexEnd)
      return curIndex;

    if (ty instanceof StructType) {
      StructType sty = (StructType) ty;
      for (int i = 0, e = sty.getNumOfElements(); i < e; i++) {
        if (indices != null && indices[indexBegin] == e - i)
          return computeLinearIndex(sty.getElementType(i), indices, indexBegin+1, indexEnd, curIndex);
        curIndex = computeLinearIndex(sty.getElementType(i), null, 0, 0, curIndex);
      }
      return curIndex;
    }

    // given array type, recursively tranverse the element.
    else if (ty instanceof ArrayType) {
      ArrayType aty = (ArrayType) ty;
      Type eltTy = aty.getElementType();
      for (int i = 0, e = (int) aty.getNumElements(); i < e; i++) {
        if (indices != null && indexBegin < indexEnd && indices[indexBegin] == i)
          return computeLinearIndex(eltTy, indices, indexBegin+1, indexEnd, curIndex);
        curIndex = computeLinearIndex(eltTy, null, 0, 0, curIndex);
      }
      return curIndex;
    }
    return curIndex + 1;
  }

  @Override
  public Void visitExtractVectorInst(User inst) {
    Value op0 = inst.operand(0);
    Type aggTy = op0.getType();
    Type valTy = inst.getType();
    boolean outOfUndef = op0 instanceof Value.UndefValue;

    int linearIndex = computeLinearIndex(aggTy, ((ExtractValueInst)inst).getIndices());
    ArrayList<EVT> valValueVTs = new ArrayList<>();
    computeValueVTs(tli, valTy, valValueVTs);

    int numValValues = valValueVTs.size();
    if (numValValues == 0) {
      setValue(inst, dag.getUNDEF(new EVT(MVT.Other)));
      return null;
    }

    SDValue[] values = new SDValue[numValValues];
    SDValue agg = getValue(op0);
    // Copy out the selected value(s).
    for (int i = linearIndex; i < linearIndex + numValValues; i++) {
      values[i - linearIndex] = outOfUndef ? dag.getUNDEF(agg.getNode().getValueType(agg.getResNo()+i)) :
          new SDValue(agg.getNode(), agg.getResNo() + i);
    }

    setValue(inst, dag.getNode(ISD.MERGE_VALUES, dag.getVTList(valValueVTs), values));
    return null;
  }

  @Override
  public Void visitInsertValueInst(User inst) {
    InsertValueInst ivInst = (InsertValueInst) inst;
    Value op0 = inst.operand(0);
    Value op1 = inst.operand(1);
    Type aggTy = inst.getType();
    Type valTy = op1.getType();

    boolean intoUndef = op0 instanceof Value.UndefValue;
    boolean fromUndef = op1 instanceof Value.UndefValue;

    int linearIndex = computeLinearIndex(aggTy, ivInst.getIndices());
    ArrayList<EVT> aggValueVTs = new ArrayList<>();
    computeValueVTs(tli, aggTy, aggValueVTs);

    ArrayList<EVT> valValueVTs = new ArrayList<>();
    computeValueVTs(tli, valTy, valValueVTs);

    int numAggValues = aggValueVTs.size();
    int numValValues = valValueVTs.size();
    SDValue[] values = new SDValue[numAggValues];
    SDValue agg = getValue(op0);

    int i = 0;
    // Copy the beginning value(s) from the original aggregate.
    for (; i < linearIndex; i++) {
      values[i] = intoUndef ? dag.getUNDEF(aggValueVTs.get(i)) :
          new SDValue(agg.getNode(), agg.getResNo() + i);
    }

    // Copy values from the inserted value(s).
    if (numValValues != 0) {
      SDValue val = getValue(op1);
      for (; i < linearIndex + numValValues; ++i)
        values[i] = fromUndef ? dag.getUNDEF(aggValueVTs.get(i)) :
            new SDValue(val.getNode(), val.getResNo() + i - linearIndex);
    }

    // Copy remaining value(s) from the original aggregate.
    for (; i < numAggValues; ++i) {
      values[i] = intoUndef ? dag.getUNDEF(aggValueVTs.get(i)) :
          new SDValue(agg.getNode(), agg.getResNo() + i);
    }

    setValue(inst, dag.getNode(ISD.MERGE_VALUES, dag.getVTList(aggValueVTs), values));
    return null;
  }

  /**
   * Lower the arithmetic instrinsics into the normal call.
   * @param inst
   * @param opc
   * @return
   */
  private String implVisitAluOverflow(CallInst inst, int opc) {
    SDValue op1 = getValue(inst.getArgOperand(0));
    SDValue op2 = getValue(inst.getArgOperand(1));

    SDNode.SDVTList vts = dag.getVTList(op1.getValueType(), new EVT(MVT.i1));
    setValue(inst, dag.getNode(opc, vts, op1, op2));
    return null;
  }
}
