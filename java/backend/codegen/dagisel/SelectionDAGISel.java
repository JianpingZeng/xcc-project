/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
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
import backend.codegen.*;
import backend.codegen.dagisel.SDNode.*;
import backend.debug.DebugLoc;
import backend.intrinsic.Intrinsic;
import backend.mc.MCInstrDesc;
import backend.mc.MCRegisterClass;
import backend.mc.MCSymbol;
import backend.pass.AnalysisUsage;
import backend.support.Attribute;
import backend.support.DepthFirstOrder;
import backend.support.MachineFunctionPass;
import backend.target.*;
import backend.target.TargetMachine.CodeGenOpt;
import backend.type.PointerType;
import backend.type.Type;
import backend.value.*;
import backend.value.Instruction.*;
import tools.APInt;
import tools.OutRef;
import tools.Pair;
import tools.Util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.*;

import static backend.codegen.MachineInstrBuilder.buildMI;
import static backend.codegen.dagisel.FunctionLoweringInfo.computeValueVTs;
import static backend.codegen.dagisel.RegsForValue.getCopyFromParts;
import static backend.codegen.dagisel.SelectionDAGISel.ChainResult.CR_InducesCycle;
import static backend.support.BackendCmdOptions.createScheduler;
import static backend.support.ErrorHandling.reportFatalError;
import static backend.target.TargetOptions.*;

/**
 * This class defined here attempts to implement the conversion from LLVM IR -> DAG -> target-specific DAG
 * -> Machine Instr, scoping on basic block. Conversely, the original implementation of LLVM would perform
 * Selection Scheduling based on local DAG before generating machine instr to facilitate pipeline provided
 * by target platform. This work might be added in the future if possible.
 * <p>
 * The working flow of this Instruction Selection based on DAG covering as follows.
 * <pre>
 *     LLVM IR (scoping in basic block instead of global function)
 *        |  done by class SelectionDAGLowering
 *        v
 *  target-independent DAG
 *        | done by X86SelectionDAGISel or other ISel specified by concrete target machine, e.g. ARM, C-SKY etc.
 *        v
 *  target-specific DAG
 *        | Instruction Emitter
 *        v
 *  Machine instruction
 * </pre>
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public abstract class SelectionDAGISel extends MachineFunctionPass implements BuiltinOpcodes, NodeFlag {
  protected SelectionDAG curDAG;
  protected TargetLowering tli;
  protected FunctionLoweringInfo funcInfo;
  protected SelectionDAGLowering sdl;
  protected int dagSize;
  protected CodeGenOpt optLevel;
  protected MachineFunction mf;
  protected TargetMachine tm;
  protected AliasAnalysis aa;
  protected int iselPosition;
  protected boolean optForSize;

  /**
   * A table encoded as a state machine for recgonzing the
   * input target-undependent DAG and transforming it into
   * target-specific DAG.
   * <p>
   * This variable should be statically initialized in class [*]GenDAGISel,
   * like {@code RISCVGenDAGISel}.
   */
  protected int[] matcherTable;

  public SelectionDAGISel(TargetMachine tm, CodeGenOpt optLevel) {
    this.tm = tm;
    tli = tm.getSubtarget().getTargetLowering();
    funcInfo = new FunctionLoweringInfo(tli);
    curDAG = new SelectionDAG(tli, funcInfo);
    sdl = new SelectionDAGLowering(curDAG, tli, funcInfo, optLevel);
    dagSize = 0;
    this.optLevel = optLevel;
  }

  public TargetMachine getTargetMachine() {
    return tm;
  }

  @Override
  public void getAnalysisUsage(AnalysisUsage au) {
    //au.addRequired(AliasAnalysis.class);
    //au.addPreserved(AliasAnalysis.class);
    au.addRequired(MachineModuleInfo.class);
    au.addPreserved(MachineModuleInfo.class);
    super.getAnalysisUsage(au);
  }

  @Override
  public boolean runOnMachineFunction(MachineFunction mf) {
    this.mf = mf;
    Function f = mf.getFunction();
    //AliasAnalysis aa = (AliasAnalysis) getAnalysisToUpDate(AliasAnalysis.class);
    MachineModuleInfo mmi = (MachineModuleInfo) getAnalysisToUpDate(MachineModuleInfo.class);
    mf.setMMI(mmi);
    curDAG.init(mf, mmi);
    funcInfo.set(f, mf);
    sdl.init(aa);

    TargetInstrInfo tii = mf.getSubtarget().getInstrInfo();
    MachineRegisterInfo regInfo = mf.getMachineRegisterInfo();

    selectAllBasicBlocks(f);
    emitLiveInCopies(mf, tii, regInfo);

    for (Pair<Integer, Integer> reg : regInfo.getLiveIns()) {
      mf.getEntryBlock().addLiveIn(reg.first);
    }
    funcInfo.clear();
    sdl.clear();
    curDAG.clear();
    dagSize = 0;
    return true;
  }

  private void emitLiveInCopies(MachineFunction mf, TargetInstrInfo tii, MachineRegisterInfo mri) {
    MachineBasicBlock entryBB = mf.getEntryBlock();
    for (Pair<Integer, Integer> regs : mri.getLiveIns()) {
      if (regs.second != 0) {
        MCRegisterClass rc = mri.getRegClass(regs.second);
        buildMI(entryBB, 0, new DebugLoc(), tii.get(TargetOpcode.COPY), regs.second).addReg(regs.first);
      }
    }
  }

  private void selectAllBasicBlocks(Function fn) {
    // Iterate over all basic blocks in the function in the post traversal order
    ArrayList<BasicBlock> blocks = DepthFirstOrder.reversePostOrder(fn.getEntryBlock());
    for (BasicBlock llvmBB : blocks) {
      funcInfo.mbb = funcInfo.mbbmap.get(llvmBB);
      boolean allPredsVisited = true;
      for (int i = 0, e = llvmBB.getNumPredecessors(); i < e; i++) {
        BasicBlock predBB = llvmBB.predAt(i);
        if (!funcInfo.visitedBBs.contains(predBB)) {
          allPredsVisited = false;
          break;
        }
      }

      if (allPredsVisited) {
        for (int i = 0, e = llvmBB.size(); i < e && llvmBB.getInstAt(i) instanceof PhiNode; i++) {
          funcInfo.computePHILiveOutRegInfo((PhiNode)llvmBB.getInstAt(i));
        }
      }
      else {
        for (int i = 0, e = llvmBB.size(); i < e && llvmBB.getInstAt(i) instanceof PhiNode; i++) {
          funcInfo.invalidatePHILiveOutRegInfo((PhiNode)llvmBB.getInstAt(i));
        }
      }
      funcInfo.visitedBBs.add(llvmBB);
      funcInfo.insertPtr = funcInfo.mbb.getFirstNonPHI();

      int bi = 0, end = llvmBB.size();

      // set up an EH landing-pad block.
      if (funcInfo.mbb.isLandingPad())
        prepareEHLandingPad();

      // Lower any arguments needed in this block if this is entry block.
      if (llvmBB.equals(fn.getEntryBlock())) {
        boolean fail = lowerArguments(llvmBB);
        if (fail) Util.shouldNotReachHere("Fail to lower argument!");
      }

      // Do FastISel on as many instructions as possible.
      if (Util.DEBUG) {
        System.err.println("========Instructions============");
      }

      sdl.setCurrentBasicBlock(funcInfo.mbb);
      for (; bi != end && !sdl.hasTailCall(); ++bi) {
        Instruction inst = llvmBB.getInstAt(bi);
        if (Util.DEBUG) System.err.println(inst.toString());

        if (!(inst instanceof TerminatorInst))
          sdl.visit(inst);
      }
      if (!sdl.hasTailCall()) {
        /*for (bi = 0; bi != end; ++bi) {
          Instruction inst = llvmBB.getInstAt(bi);
          if (!(inst instanceof PhiNode))
            sdl.copyToExportRegsIfNeeds(inst);
        }*/
        handlePhiNodeInSuccessorBlocks(llvmBB);
        sdl.visit(llvmBB.getTerminator());
      }
      curDAG.setRoot(sdl.getControlRoot());
      codeGenAndEmitInst();
      sdl.clear();

      // add operand for machine phinode
      finishBasicBlock();
    }
  }

  private void handlePhiNodeInSuccessorBlocks(BasicBlock llvmBB) {
    HashSet<MachineBasicBlock> handled = new HashSet<>();
    for (int i = 0, e = llvmBB.getNumSuccessors(); i < e; i++) {
      BasicBlock succBB = llvmBB.suxAt(i);
      MachineBasicBlock succMBB = funcInfo.mbbmap.get(succBB);
      Util.assertion(succBB != null && succMBB != null);

      if (!handled.add(succMBB)) continue;

      Instruction inst;
      int instItr = 0;
      for (int j = 0, sz = succBB.getNumOfInsts();
           j < sz && ((inst = succBB.getInstAt(j)) instanceof PhiNode); ++j) {
        if (inst.isUseEmpty()) continue;
        PhiNode pn = (PhiNode) inst;
        Value incomingOp = pn.getIncomingValueForBlock(llvmBB);
        int reg;
        if (incomingOp instanceof Constant) {
          if (sdl.constantsOut.containsKey(incomingOp))
            reg = sdl.constantsOut.get(incomingOp);
          else {
            reg = funcInfo.createRegForValue(incomingOp);
            sdl.copyValueToVirtualRegister(incomingOp, reg);
            sdl.constantsOut.put((Constant) incomingOp, reg);
          }
        } else {
          if (funcInfo.valueMap.containsKey(incomingOp)) {
            reg = funcInfo.valueMap.get(incomingOp);
          } else {
            Util.assertion(incomingOp instanceof AllocaInst && funcInfo.staticAllocaMap.containsKey(incomingOp));

            reg = funcInfo.createRegForValue(incomingOp);
            sdl.copyValueToVirtualRegister(incomingOp, reg);
            funcInfo.valueMap.put(incomingOp, reg);
          }
        }

        ArrayList<EVT> vts = new ArrayList<>();
        computeValueVTs(tli, pn.getType(), vts);
        for (EVT vt : vts) {
          int numberRegs = tli.getNumRegisters(curDAG.getContext(), vt);
          for (int k = 0; k < numberRegs; k++) {
            sdl.phiNodesToUpdate.add(Pair.get(succMBB.getInstAt(instItr++), reg + k));
          }
          reg += numberRegs;
        }
      }
    }
    sdl.constantsOut.clear();
  }

  private void codeGenAndEmitInst() {
    if (Util.DEBUG)
      System.err.println("Instruction Selection");

    String blockName = mf.getFunction().getName() + ":" +
        funcInfo.mbb.getBasicBlock().getName();

    if (ViewDAGBeforeCodeGen.value) {
      curDAG.viewGraph("dag-input-combine-first for " + blockName);
    }

    // The first combination before the first legalization phase.
    curDAG.combine(CombineLevel.Unrestricted, aa, optLevel);
    if (Util.DEBUG) {
      for (SDNode n : curDAG.allNodes) {
        Util.assertion(!n.isDeleted());
      }
    }

    if (false) {
      curDAG.viewGraph("dag-before-legalize for " + blockName);
    }

    // #1 The first legalization phase.
    boolean changed = curDAG.legalizeTypes();
    if (changed && Util.DEBUG) {
      for (SDNode n : curDAG.allNodes) {
        Util.assertion(!n.isDeleted());
      }
    }
    if (false) {
      curDAG.viewGraph("dag-after-first-legalize-types for " + blockName);
    }

    if (changed) {
      curDAG.combine(CombineLevel.NoIllegalTypes, aa, optLevel);
      if (Util.DEBUG) {
        for (SDNode n : curDAG.allNodes) {
          Util.assertion(!n.isDeleted());
        }
      }
      if (false) {
        curDAG.viewGraph("dag-after-second-combines for " + blockName);
      }
    }

    // #2 The second legalization steps for vector type.
    changed = curDAG.legalizeVectors();
    if (changed) {
      changed = curDAG.legalizeTypes();
      if (changed && Util.DEBUG) {
        for (SDNode n : curDAG.allNodes) {
          Util.assertion(!n.isDeleted());
        }
      }
    }
    if (changed) {
      curDAG.combine(CombineLevel.NoIllegalOperations, aa, optLevel);
      if (Util.DEBUG) {
        for (SDNode n : curDAG.allNodes) {
          Util.assertion(!n.isDeleted());
        }
      }
    }

    if (false) {
      curDAG.viewGraph("dag-after-combine2 for " + blockName);
    }

    // #3 The third legalization phase.
    curDAG.legalize(false, optLevel);
    if (Util.DEBUG) {
      for (SDNode n : curDAG.allNodes) {
        Util.assertion(!n.isDeleted());
      }
    }
    if (false) {
      curDAG.viewGraph("dag-after-legalizations for " + blockName);
    }
    curDAG.combine(CombineLevel.NoIllegalOperations, aa, optLevel);
    if (Util.DEBUG) {
      for (SDNode n : curDAG.allNodes) {
        Util.assertion(!n.isDeleted());
      }
    }

    // For virtual register info.
    if (optLevel != CodeGenOpt.None)
      computeLiveOutVRegInfo();

    if (ViewDAGBeforeISel.value) {
      curDAG.viewGraph("dag-before-isel for " + blockName);
    }

    instructionSelect();

    if (ViewDAGBeforeSched.value) {
      curDAG.viewGraph("dag-before-sched for " + blockName);
    }

    ScheduleDAG scheduler = createScheduler(this, optLevel);
    scheduler.run(curDAG, funcInfo.mbb, funcInfo.mbb.size());
    if (ViewDAGAfterSched.value) {
      scheduler.viewGraph("dag-after-sched for " + blockName);
    }
    funcInfo.mbb = scheduler.emitSchedule();
  }

  private void instructionSelect() {
    Util.assertion(matcherTable != null, "MatchTable should be initialized before calling instructionSelect");

    // call the target hook to do some preparation work.
    preprocessISelDAG();
    boolean changed;
    do {
      changed = false;
      dagSize = curDAG.assignTopologicalOrder();
      SDNode.HandleSDNode dummy = new SDNode.HandleSDNode(curDAG.getRoot());
      iselPosition = curDAG.allNodes.indexOf(curDAG.getRoot().getNode());
      Util.assertion(iselPosition != -1, "Specified root node not exists in allNodes!");
      iselPosition++;
      while (iselPosition != 0) {
        SDNode node = curDAG.allNodes.get(--iselPosition);
        if (node.isUseEmpty() || node.isDeleted() || node.isMachineOpecode())
          continue;

        SDNode resNode = select(node);
        if (Objects.equals(resNode, node))
          continue;

        if (resNode != null) {
          changed = true;
          replaceUses(node, resNode);
        }
        if (node.isUseEmpty())
          // we just mark node and all SDNode which can be reached from node as
          // DELETED_NODE instead of erasing it from allNodes.
          curDAG.collectDeadNode(node);
      }
      curDAG.setRoot(dummy.getValue());
      dummy.dropOperands();
      curDAG.removeDeadNodes();
    } while (changed);

    postprocessISelDAG();
  }

  private long getVBR(long val, OutRef<Integer> idx) {
    Util.assertion(val >= 128, "Not a VBR");
    val &= 127;
    int shift = 7;
    long nextBits;
    do {
      nextBits = matcherTable[idx.get()];
      idx.set(idx.get() + 1);
      val |= (nextBits & 127) << shift;
      shift += 7;
    } while ((nextBits & 128) != 0);
    return val;
  }

  private boolean checkSame(int[] matcherTable,
                            OutRef<Integer> matcherIndex,
                            SDValue n,
                            ArrayList<Pair<SDValue, SDNode>> recordedNodes) {
    int recNo = matcherTable[matcherIndex.get()];
    matcherIndex.set(matcherIndex.get() + 1);
    Util.assertion(recNo < recordedNodes.size(), "invalid checkSame");
    return n.equals(recordedNodes.get(recNo).first);
  }

  private boolean checkOpcode(int[] matcherTable,
                              OutRef<Integer> matcherIndex,
                              SDNode n) {
    int opc = matcherTable[matcherIndex.get()];
    matcherIndex.set(matcherIndex.get() + 1);
    return n.getOpcode() == opc;
  }

  private boolean checkType(int[] matcherTable,
                            OutRef<Integer> matcherIndex,
                            SDValue n) {
    int vt = matcherTable[matcherIndex.get()];
    matcherIndex.set(matcherIndex.get() + 1);
    if (n.getValueType().getSimpleVT().simpleVT == vt)
      return true;

    return vt == MVT.iPTR && n.getValueType().equals(new EVT(tli.getPointerTy()));
  }

  private boolean checkChildType(int[] matcherTable,
                                 OutRef<Integer> matcherIndex,
                                 SDValue n,
                                 int childNo) {
    if (childNo >= n.getNumOperands())
      return false;
    return checkType(matcherTable, matcherIndex, n.getOperand(childNo));
  }

  private boolean checkCondCode(int[] matcherTable,
                                OutRef<Integer> matcherIndex,
                                SDValue n) {
    int cc = matcherTable[matcherIndex.get()];
    matcherIndex.set(matcherIndex.get() + 1);
    return ((CondCodeSDNode) n.getNode()).getCondition() ==
        CondCode.values()[cc];
  }

  private boolean checkValueType(int[] matcherTable,
                                 OutRef<Integer> matcherIndex,
                                 SDValue n) {
    int vt = matcherTable[matcherIndex.get()];
    matcherIndex.set(matcherIndex.get() + 1);
    EVT actualVT = ((SDNode.VTSDNode) n.getNode()).getVT();
    if (actualVT.equals(new EVT(vt)))
      return true;

    return vt == MVT.iPTR && actualVT.equals(new EVT(tli.getPointerTy()));
  }

  private boolean checkInteger(int[] matcherTable,
                               OutRef<Integer> matcherIndex,
                               SDValue n) {
    long val = matcherTable[matcherIndex.get()];
    matcherIndex.set(matcherIndex.get() + 1);
    if ((val & 128) != 0)
      val = getVBR(val, matcherIndex);

    if (!(n.getNode() instanceof ConstantSDNode))
      return false;
    ConstantSDNode c = (ConstantSDNode) n.getNode();
    return c.getSExtValue() == val;
  }

  private boolean checkAndImm(int[] matcherTable,
                              OutRef<Integer> matcherIndex,
                              SDValue n) {
    if (n.getOpcode() != ISD.AND) return false;

    long val = matcherTable[matcherIndex.get()];
    matcherIndex.set(matcherIndex.get() + 1);
    if ((val & 128) != 0)
      val = getVBR(val, matcherIndex);

    if (!(n.getOperand(1).getNode() instanceof ConstantSDNode))
      return false;
    ConstantSDNode c = (ConstantSDNode) n.getOperand(1).getNode();
    return checkAndMask(n.getOperand(0), c, val);
  }

  private boolean checkOrImm(int[] matcherTable,
                             OutRef<Integer> matcherIndex,
                             SDValue n) {
    if (n.getOpcode() != ISD.OR) return false;

    long val = matcherTable[matcherIndex.get()];
    matcherIndex.set(matcherIndex.get() + 1);
    if ((val & 128) != 0)
      val = getVBR(val, matcherIndex);

    if (!(n.getOperand(1).getNode() instanceof ConstantSDNode))
      return false;
    ConstantSDNode c = (ConstantSDNode) n.getOperand(1).getNode();
    return checkAndMask(n.getOperand(0), c, val);
  }


  public boolean checkPatternPredicate(int[] matcherTable,
                                       OutRef<Integer> matcherIndex) {
    boolean res = checkPatternPredicate(matcherTable[matcherIndex.get()]);
    matcherIndex.set(matcherIndex.get()+1);
    return res;
  }

  public boolean checkPatternPredicate(int predNo) {
    Util.shouldNotReachHere("This method should be overrided by generation of tblgen");
    return false;
  }

  public boolean checkNodePredicate(int[] matcherTable,
                                    OutRef<Integer> matcherIndex,
                                    SDNode n) {
    boolean res = checkNodePredicate(n, matcherTable[matcherIndex.get()]);
    matcherIndex.set(matcherIndex.get() + 1);
    return res;
  }

  public boolean checkNodePredicate(SDNode node, int predNo) {
    Util.shouldNotReachHere("This method should be overrided by generation of tblgen");
    return false;
  }

  public boolean checkComplexPattern(SDNode root,
                                     SDNode parent,
                                     SDValue n,
                                     int patternNo,
                                     ArrayList<tools.Pair<SDValue, SDNode>> result) {
    Util.shouldNotReachHere("This method should be overrided by generation of tblgen");
    return false;
  }

  public SDValue runSDNodeXForm(SDValue v, int xformNo) {
    Util.shouldNotReachHere("This method should be overrided by generation of tblgen");
    return null;
  }

  private int isPredicateKnownToFail(int index, SDValue n,
                                     OutRef<Boolean> result,
                                     ArrayList<Pair<SDValue, SDNode>> recordedNodes) {
    OutRef<Integer> tmpIdx = new OutRef<>(0);
    switch (matcherTable[index++]) {
      default:
        result.set(false);
        return index - 1;
      case OPC_CheckSame:
        tmpIdx.set(index);
        result.set(!checkSame(matcherTable, tmpIdx, n, recordedNodes));
        return tmpIdx.get();
      case OPC_CheckPatternPredicate:
        tmpIdx.set(index);
        result.set(!checkPatternPredicate(matcherTable, tmpIdx));
        return tmpIdx.get();
      case OPC_CheckPredicate:
        tmpIdx.set(index);
        result.set(!checkNodePredicate(matcherTable, tmpIdx, n.getNode()));
        return tmpIdx.get();
      case OPC_CheckType:
        tmpIdx.set(index);
        result.set(!checkType(matcherTable, tmpIdx, n));
        return tmpIdx.get();
      case OPC_CheckChild0Type:
      case OPC_CheckChild1Type:
      case OPC_CheckChild2Type:
      case OPC_CheckChild3Type:
      case OPC_CheckChild4Type:
      case OPC_CheckChild5Type:
      case OPC_CheckChild6Type:
      case OPC_CheckChild7Type:
        tmpIdx.set(index);
        result.set(!checkChildType(matcherTable, tmpIdx, n,
            matcherTable[index - 1] - OPC_CheckChild0Type));
        return tmpIdx.get();
      case OPC_CheckCondCode:
        tmpIdx.set(index);
        result.set(!checkCondCode(matcherTable, tmpIdx, n));
        return tmpIdx.get();
      case OPC_CheckValueType:
        tmpIdx.set(index);
        result.set(!checkType(matcherTable, tmpIdx, n));
        return tmpIdx.get();
      case OPC_CheckInteger:
        tmpIdx.set(index);
        result.set(!checkType(matcherTable, tmpIdx, n));
        return tmpIdx.get();
      case OPC_CheckAndImm:
        tmpIdx.set(index);
        result.set(!checkAndImm(matcherTable, tmpIdx, n));
        return tmpIdx.get();
      case OPC_CheckOrImm:
        tmpIdx.set(index);
        result.set(!checkOrImm(matcherTable, tmpIdx, n));
        return tmpIdx.get();
    }
  }

  static class MatchScope {
    /// failIndex - If this match fails, this is the index to continue with.
    int failIndex;

    /// nodeStack - The node stack when the scope was formed.
    Stack<SDValue> nodeStack;

    /**
     * The number of recorded nodes when the scope was formed.
     */
    int numRecordedNodes;

    /// The number of matched memref entries.
    int numMatchedMemRefs;

    /// The current chain/flag
    SDValue inputChain, inputFlag;

    /// HasChainNodesMatched - True if the ChainNodesMatched list is non-empty.
    boolean hasChainNodesMatched, hasFlagResultNodesMatched;

    MatchScope() {
      failIndex = -1;
      nodeStack = new Stack<>();
      numRecordedNodes = -1;
      numMatchedMemRefs = -1;
      inputChain = new SDValue();
      inputFlag = new SDValue();
      hasChainNodesMatched = false;
      hasFlagResultNodesMatched = false;
    }
  }

  public enum ChainResult {
    CR_Simple,
    CR_InducesCycle,
    CR_LeadsToInteriorNode
  }

  ChainResult walkChainUsers(SDNode chainNode,
                             ArrayList<SDNode> chainedNodesInPattern,
                             ArrayList<SDNode> interiorChainedNodes) {
    ChainResult result = ChainResult.CR_Simple;

    for (SDUse u : chainNode.useList) {
      // Make sure the use is of the chain, not some other value we produce.
      if (u.getValueType().getSimpleVT().simpleVT != MVT.Other)
        continue;

      SDNode user = u.getUser();
      // If we see an already-selected machine node, then we've gone beyond the
      // pattern that we're selecting down into the already selected chunk of the
      // DAG.
      int userOpc = user.getOpcode();
      if (user.isMachineOpecode() || userOpc == ISD.HANDLENODE)
        // Root of the graph
        continue;

      if (userOpc == ISD.CopyToReg ||
          userOpc == ISD.CopyFromReg ||
          userOpc == ISD.INLINEASM ||
          userOpc == ISD.EH_LABEL) {
        // If their node ID got reset to -1 then they've already been selected.
        // Treat them like a MachineOpcode.
        if (user.getNodeID() == -1)
          continue;
      }

      // If we have a TokenFactor, we handle it specially.
      if (userOpc != ISD.TokenFactor) {
        // If the node isn't a token factor and isn't part of our pattern, then it
        // must be a random chained node in between two nodes we're selecting.
        // This happens when we have something like:
        //   x = load ptr
        //   call
        //   y = x+4
        //   store y -> ptr
        // Because we structurally match the load/store as a read/modify/write,
        // but the call is chained between them.  We cannot fold in this case
        // because it would induce a cycle in the graph.
        if (!chainedNodesInPattern.contains(user))
          return CR_InducesCycle;

        // Otherwise we found a node that is part of our pattern.  For example in:
        //   x = load ptr
        //   y = x+4
        //   store y -> ptr
        // This would happen when we're scanning down from the load and see the
        // store as a user.  Record that there is a use of ChainedNode that is
        // part of the pattern and keep scanning uses.
        result = ChainResult.CR_LeadsToInteriorNode;
        interiorChainedNodes.add(user);
        continue;
      }

      // If we found a TokenFactor, there are two cases to consider: first if the
      // TokenFactor is just hanging "below" the pattern we're matching (i.e. no
      // uses of the TF are in our pattern) we just want to ignore it.  Second,
      // the TokenFactor can be sandwiched in between two chained nodes, like so:
      //     [Load chain]
      //         ^
      //         |
      //       [Load]
      //       ^    ^
      //       |    \                    DAG's like cheese
      //      /       \                       do you?
      //     /         |
      // [TokenFactor] [Op]
      //     ^          ^
      //     |          |
      //      \        /
      //       \      /
      //       [Store]
      //
      // In this case, the TokenFactor becomes part of our match and we rewrite it
      // as a new TokenFactor.
      //
      // To distinguish these two cases, do a recursive walk down the uses.
      switch (walkChainUsers(user, chainedNodesInPattern, interiorChainedNodes)) {
        case CR_Simple:
          // If the uses of the TokenFactor are just already-selected nodes, ignore
          // it, it is "below" our pattern.
          continue;
        case CR_InducesCycle:
          // If the uses of the TokenFactor lead to nodes that are not part of our
          // pattern that are not selected, folding would turn this into a cycle,
          // bail out now.
          return CR_InducesCycle;
        case CR_LeadsToInteriorNode:
          break; // Otherwise, keep going
      }

      // Okay, we know we're in the interesting interior case.  The TokenFactor
      // is now going to be considered part of the pattern so that we rewrite its
      // uses (it may have uses that are not part of the pattern) with the
      // ultimate chain result of the generated code.  We will also add its chain
      // inputs as inputs to the ultimate TokenFactor we create.
      result = ChainResult.CR_LeadsToInteriorNode;
      chainedNodesInPattern.add(user);
      interiorChainedNodes.add(user);
    }
    return result;
  }

  /**
   * This implements the OPC_EmitMergeInputChains operation for it when
   * the pattern at least one node with a chains. The input list contains
   * a list of all of the chained nodes that we match. We must determine
   * if this is a valid thing to cover (i.e. matching it won't induce cycles
   * in in the DAG) and if so, creating a TokenFactor node. that will be used
   * as the input node chain for the generated ndoes.
   *
   * @param chainNodesMatched
   * @param curDAG
   * @return
   */
  private SDValue handleMergeInputChains(ArrayList<SDNode> chainNodesMatched,
                                         SelectionDAG curDAG) {
    ArrayList<SDNode> interiorChainedNodes = new ArrayList<>();
    for (SDNode n : chainNodesMatched) {
      if (walkChainUsers(n, chainNodesMatched, interiorChainedNodes) == CR_InducesCycle)
        // will introduce a cycle
        return new SDValue();
    }

    // Okay, we have walked all the matched nodes and collected TokenFactor nodes
    // that we are interested in.  Form our input TokenFactor node.
    ArrayList<SDValue> inputChains = new ArrayList<>();
    for (int i = 0, e = chainNodesMatched.size(); i < e; i++) {
      SDNode n = chainNodesMatched.get(i);
      // Add the input chain of this node to the InputChains list (which will be
      // the operands of the generated TokenFactor) if it's not an interior node.
      if (n.getOpcode() != ISD.TokenFactor) {
        if (interiorChainedNodes.contains(n))
          continue;

        // add the chain.
        SDValue inChain = chainNodesMatched.get(i).getOperand(0);
        Util.assertion(inChain.getValueType().equals(new EVT(MVT.Other)), "not a chain");
        inputChains.add(inChain);
        continue;
      }

      // If we have a token factor, we want to add all inputs of the token factor
      // that are not part of the pattern we're matching.
      for (int op = 0, size = n.getNumOperands(); op < size; op++) {
        if (!chainNodesMatched.contains(n.getOperand(op).getNode()))
          inputChains.add(n.getOperand(op));
      }
    }
    if (inputChains.size() == 1)
      return inputChains.get(0);
    return curDAG.getNode(ISD.TokenFactor, new EVT(MVT.Other), inputChains);
  }

  /**
   * Transform an EmitNode flags word into the number of fixed arty values
   * that shold be skipped when copying from the root.
   *
   * @param flags
   * @return
   */
  private static int getNumFixedFromVariadicInfo(int flags) {
    return ((flags & OPFL_VariadicInfo) >> 4) - 1;
  }

  /**
   * When a match is complete, this method updates uses of interior flag
   * and chain results to use the new flag and chain results.
   *
   * @param nodeToMatch
   * @param inputChain
   * @param chainNodesMatched
   * @param inputFlag
   * @param flagResultNodesMatched
   * @param isMorphNodeTo
   */
  private void updateChainsAndFlags(SDNode nodeToMatch,
                                    SDValue inputChain,
                                    ArrayList<SDNode> chainNodesMatched,
                                    SDValue inputFlag,
                                    ArrayList<SDNode> flagResultNodesMatched,
                                    boolean isMorphNodeTo) {
    LinkedList<SDNode> deadNodes = new LinkedList<>();

    // Now that all the normal results are replaced, we replace the chain and
    // flag results if present.
    if (!chainNodesMatched.isEmpty()) {
      Util.assertion(inputChain.getNode() != null,
          "Matched input chain but didn't produce a chain!");

      for (SDNode chainNode : chainNodesMatched) {
        // If this node was already deleted, don't look at it.
        if (chainNode.getOpcode() == ISD.DELETED_NODE)
          continue;

        // Don't replace the results of the root node if we're doing a
        // MorphNodeTo.
        if (chainNode.equals(nodeToMatch) && isMorphNodeTo)
          continue;

        SDValue chainVal = new SDValue(chainNode, chainNode.getNumValues() - 1);
        if (chainVal.getValueType().equals(new EVT(MVT.Glue)))
          chainVal = chainVal.getValue(chainVal.getNode().getNumValues() - 2);
        Util.assertion(chainVal.getValueType().equals(new EVT(MVT.Other)), "not a chain!");
        curDAG.replaceAllUsesOfValueWith(chainVal, inputChain, null);

        // If the node became dead, delete it.
        if (chainNode.isUseEmpty())
          deadNodes.add(chainNode);
      }
    }

    // If the result produces a flag, update any flag results in the matched
    // pattern with the flag result.
    if (inputFlag.getNode() != null) {
      for (SDNode flagNode : flagResultNodesMatched) {
        if (flagNode.getOpcode() == ISD.DELETED_NODE)
          continue;

        Util.assertion(flagNode.getValueType(flagNode.getNumValues() - 1).equals(new EVT(MVT.Glue)),
            "Doesn't have a flag result");
        curDAG.replaceAllUsesOfValueWith(new SDValue(flagNode, flagNode.getNumValues() - 1),
            inputFlag, null);

        // if this flagNode becomes dead, add it into deadNodes list.
        if (flagNode.isUseEmpty())
          deadNodes.add(flagNode);
      }
    }

    if (!deadNodes.isEmpty())
      curDAG.collectDeadNodes(deadNodes);
  }

  /**
   * This is a cache used to dispatch efficiently into isel state machine
   * that start with a OPC_SwitchOpcode node.
   */
  private int[] opcodeOffest = new int[256];

  /**
   * The entry to morphy the given SDNode and return a target-specific SDNode.
   * If match failure, return null.
   * @param node
   * @return
   */
  public abstract SDNode select(SDNode node);

  /**
   * The entry to common instruction selection among different targets.
   * @param nodeToMatch
   * @return
   */
  public SDNode selectCommonCode(SDNode nodeToMatch) {
    switch (nodeToMatch.getOpcode()) {
      default:
        break;
      case ISD.EntryToken:
      case ISD.BasicBlock:
      case ISD.Register:
      case ISD.HANDLENODE:
      case ISD.TargetConstant:
      case ISD.TargetConstantFP:
      case ISD.TargetConstantPool:
      case ISD.TargetFrameIndex:
      case ISD.TargetExternalSymbol:
      case ISD.TargetBlockAddress:
      case ISD.TargetJumpTable:
      case ISD.TargetGlobalAddress:
      case ISD.TargetGlobalTLSAddress:
      case ISD.TokenFactor:
      case ISD.CopyFromReg:
      case ISD.CopyToReg:
      case ISD.EH_LABEL:
        // mark it as selected.
        nodeToMatch.setNodeID(-1);
        return null;
      case ISD.AssertSext:
      case ISD.AssertZext:
        curDAG.replaceAllUsesOfValueWith(new SDValue(nodeToMatch, 0),
            nodeToMatch.getOperand(0));
        return null;
      case ISD.INLINEASM:
        return select_INLINEASM(new SDValue(nodeToMatch, 0));
      case ISD.UNDEF:
        return select_UNDEF(new SDValue(nodeToMatch, 0));
    }
    Util.assertion(!nodeToMatch.isMachineOpecode(), "Node already selected");

    // Set up the node stack with nodeToMatch as the only node on the stack.
    Stack<SDValue> nodeStack = new Stack<>();
    SDValue n = new SDValue(nodeToMatch, 0);
    nodeStack.push(n);

    // Scopes used when matching, if a match failure happens, this indicates
    // where to continue checking.
    LinkedList<MatchScope> matchScopes = new LinkedList<>();

    // This is the set of nodes that have been recorded by the state machine.
    ArrayList<Pair<SDValue, SDNode>> recordedNodes = new ArrayList<>();

    // This is the set of MemRef's we're seen in the input patterns.
    ArrayList<MachineMemOperand> matchedMemRefs = new ArrayList<>();

    // These are the current input chain and flag for use when
    // generating nodes. Various emit operations change these.
    // For example, emitting a copytoreg uses and update these.
    SDValue inputChain = new SDValue(), inputFlag = new SDValue();

    // If a patterns nodes that have input/output chains, the
    // OPC_EmitMergeInputChains operation is emitted witch indicates
    // which ones they are. The result is captured into this list
    // so that we can update the chain results when the pattern is
    // complete.
    ArrayList<SDNode> chainNodesMatched = new ArrayList<>();
    ArrayList<SDNode> flagResultNodesMatched = new ArrayList<>();
    OutRef<Integer> idx = new OutRef<>(0);

    int matcherIndex = 0;
    if (n.getOpcode() < opcodeOffest.length) {
      matcherIndex = opcodeOffest[n.getOpcode()];
    } else if (matcherTable[0] == OPC_SwitchOpcode) {
      // Otherwise, the table isn't computed, but the state machine does start
      // with an OPC_SwitchOpcode instruction.  Populate the table now, since this
      // is the first time we're selecting an instruction.
      idx.set(1);
      while (true) {
        long caseSize = matcherTable[idx.get()];
        idx.set(idx.get() + 1);
        if ((caseSize & 128) != 0)
          caseSize = getVBR(caseSize, idx);
        if (caseSize == 0) break;
        int opc = matcherTable[idx.get()];
        idx.set(idx.get() + 1);
        if (opc >= opcodeOffest.length) {
          int[] temp = new int[(opc + 1) * 2];
          System.arraycopy(opcodeOffest, 0, temp, 0, opcodeOffest.length);
          opcodeOffest = temp;
        }
        opcodeOffest[opc] = idx.get();
        idx.set((int) (idx.get() + caseSize));
      }

      // Okay, do the lookup for the first opcode.
      if (n.getOpcode() < opcodeOffest.length)
        matcherIndex = opcodeOffest[n.getOpcode()];
    }

    while (true) {
      Util.assertion(matcherIndex < matcherTable.length, "Invalid index!");
      int opcode = matcherTable[matcherIndex++];
      switch (opcode) {
        case OPC_Scope: {
          // Okay, the semantics of this operation are that we should push a scope
          // then evaluate the first child.  However, pushing a scope only to have
          // the first check fail (which then pops it) is inefficient.  If we can
          // determine immediately that the first check (or first several) will
          // immediately fail, don't even bother pushing a scope for them.
          int failIndex = 0;
          while (true) {
            long numToSkip = matcherTable[matcherIndex++];
            if ((numToSkip & 128) != 0) {
              idx.set(matcherIndex);
              numToSkip = getVBR(numToSkip, idx);
              matcherIndex = idx.get();
            }
            if (numToSkip == 0) {
              failIndex = 0;
              break;
            }

            failIndex = (int) (matcherIndex + numToSkip);
            // If we can't evaluate this predicate without pushing a scope (e.g. if
            // it is a 'MoveParentMatcher') or if the predicate succeeds on this node, we
            // push the scope and evaluate the full predicate chain.
            OutRef<Boolean> result = new OutRef<>(false);
            matcherIndex = isPredicateKnownToFail(matcherIndex,
                n, result, recordedNodes);
            if (!result.get())
              break;

            if (Util.DEBUG) {
              System.err.printf("  Skipped scope entry at index %d continuing at %d\n",
                  matcherIndex, failIndex);
            }

            // Otherwise, we know that this case of the Scope is guaranteed to fail,
            // move to the next case.
            matcherIndex = failIndex;
          }

          // If the whole scope failed to match, bail.
          if (failIndex == 0) break;

          // Push a MatchScope which indicates where to go if the first child fails
          // to match.
          MatchScope newEntry = new MatchScope();
          newEntry.failIndex = failIndex;
          newEntry.nodeStack.addAll(nodeStack);
          newEntry.numRecordedNodes = recordedNodes.size();
          newEntry.numMatchedMemRefs = matchedMemRefs.size();
          newEntry.inputFlag = inputFlag;
          newEntry.inputChain = inputChain;
          newEntry.hasFlagResultNodesMatched = !flagResultNodesMatched.isEmpty();
          newEntry.hasChainNodesMatched = !chainNodesMatched.isEmpty();
          matchScopes.add(newEntry);
          continue;
        }
        case OPC_RecordNode:
          // remember this node, it may end up being on an operand in the pattern.
          SDNode parent = null;
          if (nodeStack.size() > 1)
            parent = nodeStack.get(nodeStack.size() - 2).getNode();
          recordedNodes.add(Pair.get(n, parent));
          continue;

        case OPC_RecordChild0:
        case OPC_RecordChild1:
        case OPC_RecordChild2:
        case OPC_RecordChild3:
        case OPC_RecordChild4:
        case OPC_RecordChild5:
        case OPC_RecordChild6:
        case OPC_RecordChild7: {
          int childNo = opcode - OPC_RecordChild0;
          if (childNo >= n.getNumOperands())
            break;  // match fails if out of range child number
          recordedNodes.add(Pair.get(n.getOperand(childNo), n.getNode()));
          continue;
        }
        case OPC_RecordMemRef:
          Util.assertion(n.getNode() instanceof SDNode.MemSDNode,
              String.format("The node must be a memory access operation, rather than '%s'", n.getNode().getOperationName(curDAG)));
          matchedMemRefs.add(((SDNode.MemSDNode) n.getNode()).getMemOperand());
          continue;
        case OPC_CaptureFlagInput:
          if (n.getNumOperands() > 0 &&
              n.getOperand(n.getNumOperands() - 1).getValueType().equals(new EVT(MVT.Glue)))
            inputFlag = n.getOperand(n.getNumOperands() - 1);
          continue;
        case OPC_MoveChild: {
          int childNo = matcherTable[matcherIndex++];
          if (childNo >= n.getNumOperands())
            break;  // match fails if out of range child number
          n = n.getOperand(childNo);
          nodeStack.push(n);
          continue;
        }

        case OPC_MoveParent:
          nodeStack.pop();
          Util.assertion(!nodeStack.isEmpty(), "Node stack imbalance");
          n = nodeStack.peek();
          continue;
        case OPC_CheckSame: {
          idx.set(matcherIndex);
          boolean res = !checkSame(matcherTable, idx, n, recordedNodes);
          matcherIndex = idx.get();
          if (res) break;
          continue;
        }
        case OPC_CheckPatternPredicate: {
          idx.set(matcherIndex);
          boolean res = !checkPatternPredicate(matcherTable, idx);
          matcherIndex = idx.get();
          if (res) break;
          continue;
        }
        case OPC_CheckPredicate: {
          idx.set(matcherIndex);
          boolean res = !checkNodePredicate(matcherTable, idx, n.getNode());
          matcherIndex = idx.get();
          if (res) break;
          continue;
        }
        case OPC_CheckComplexPat: {
          int cpNum = matcherTable[matcherIndex++];
          int recNo = matcherTable[matcherIndex++];
          if (!checkComplexPattern(nodeToMatch, recordedNodes.get(recNo).second,
              recordedNodes.get(recNo).first, cpNum, recordedNodes)) break;
          continue;
        }
        case OPC_CheckOpcode: {
          idx.set(matcherIndex);
          boolean res = !checkOpcode(matcherTable, idx, n.getNode());
          matcherIndex = idx.get();
          if (res) break;
          continue;
        }
        case OPC_CheckType: {
          idx.set(matcherIndex);
          boolean res = !checkType(matcherTable, idx, n);
          matcherIndex = idx.get();
          if (res) break;
          continue;
        }
        case OPC_SwitchOpcode: {
          int curNodeOpcode = n.getOpcode();
          //int switchStart = matcherIndex-1;
          long caseSize;
          while (true) {
            caseSize = matcherTable[matcherIndex++];
            if ((caseSize & 128) != 0) {
              idx.set(matcherIndex);
              caseSize = getVBR(caseSize, idx);
              matcherIndex = idx.get();
            }
            if (caseSize == 0) break;
            // If the opcode matches, then we will execute this case.
            if (curNodeOpcode == matcherTable[matcherIndex++])
              break;

            // Otherwise, skip over this case.
            matcherIndex += caseSize;
          }

          // if no cases matched, bail out.
          if (caseSize == 0) break;
          continue;
        }
        case OPC_SwitchType: {
          int curNodeType = n.getValueType().getSimpleVT().simpleVT;
          long caseSize;
          while (true) {
            caseSize = matcherTable[matcherIndex++];
            if ((caseSize & 128) != 0) {
              idx.set(matcherIndex);
              caseSize = getVBR(caseSize, idx);
              matcherIndex = idx.get();
            }
            if (caseSize == 0) break;

            int caseVT = matcherTable[matcherIndex++];
            if (caseVT == MVT.iPTR)
              caseVT = tli.getPointerTy().simpleVT;

            // If the VT matches, then we will execute this case.
            if (curNodeType == caseVT)
              break;

            // Otherwise, skip over this case.
            matcherIndex += caseSize;
          }
        }
        case OPC_CheckChild0Type:
        case OPC_CheckChild1Type:
        case OPC_CheckChild2Type:
        case OPC_CheckChild3Type:
        case OPC_CheckChild4Type:
        case OPC_CheckChild5Type:
        case OPC_CheckChild6Type:
        case OPC_CheckChild7Type: {
          int childNo = opcode - OPC_CheckChild0Type;
          idx.set(matcherIndex);
          boolean res = !checkChildType(matcherTable, idx, n, childNo);
          matcherIndex = idx.get();
          if (res) break;
          continue;
        }
        case OPC_CheckCondCode: {
          idx.set(matcherIndex);
          boolean res = !checkCondCode(matcherTable, idx, n);
          matcherIndex = idx.get();
          if (res) break;
          continue;
        }
        case OPC_CheckValueType: {
          idx.set(matcherIndex);
          boolean res = !checkValueType(matcherTable, idx, n);
          matcherIndex = idx.get();
          if (res) break;
          continue;
        }
        case OPC_CheckInteger: {
          idx.set(matcherIndex);
          boolean res = !checkInteger(matcherTable, idx, n);
          matcherIndex = idx.get();
          if (res) break;
          continue;
        }
        case OPC_CheckAndImm: {
          idx.set(matcherIndex);
          boolean res = !checkAndImm(matcherTable, idx, n);
          matcherIndex = idx.get();
          if (res) break;
          continue;
        }
        case OPC_CheckOrImm: {
          idx.set(matcherIndex);
          boolean res = !checkOrImm(matcherTable, idx, n);
          matcherIndex = idx.get();
          if (res) break;
          continue;
        }
        case OPC_CheckFoldableChainNode: {
          Util.assertion(nodeStack.size() != 1, "No parent node");
          // Verify that all intermediate nodes between the root and this one have
          // a single use.
          boolean hasMultipleUses = false;
          for (int i = 1, e = nodeStack.size() - 1; i < e; i++)
            if (!nodeStack.get(i).hasOneUse()) {
              hasMultipleUses = true;
              break;
            }
          if (hasMultipleUses) break;

          // Check to see that the target thinks this is profitable to fold and that
          // we can fold it without inducing cycles in the graph.
          if (!isLegalAndProfitableToFold(n, nodeStack.get(nodeStack.size() - 1).getNode(), nodeToMatch))
            break;
          continue;
        }
        case OPC_EmitInteger: {
          int vt = matcherTable[matcherIndex++];
          long val = matcherTable[matcherIndex++];
          if ((val & 128) != 0) {
            idx.set(matcherIndex);
            val = getVBR(val, idx);
            matcherIndex = idx.get();
          }
          recordedNodes.add(Pair.get(curDAG.getTargetConstant(val, new EVT(vt)), null));
          continue;
        }
        case OPC_EmitRegister: {
          int vt = matcherTable[matcherIndex++];
          int regNo = matcherTable[matcherIndex++];
          recordedNodes.add(Pair.get(curDAG.getRegister(regNo, new EVT(vt)), null));
          continue;
        }
        case OPC_EmitConvertToTarget: {
          // Convert from IMM/FPIMM to target version.
          int recNo = matcherTable[matcherIndex++];
          Util.assertion(recNo < recordedNodes.size(), "invalid checkSame");
          SDValue imm = recordedNodes.get(recNo).first;

          if (imm.getOpcode() == ISD.Constant) {
            long val = ((ConstantSDNode) imm.getNode()).getZExtValue();
            imm = curDAG.getTargetConstant(val, imm.getValueType());
          } else if (imm.getOpcode() == ISD.ConstantFP) {
            ConstantFP fp = ((SDNode.ConstantFPSDNode) imm.getNode()).getConstantFPValue();
            imm = curDAG.getTargetConstantFP(fp, imm.getValueType());
          }
          recordedNodes.add(Pair.get(imm, recordedNodes.get(recNo).second));
          continue;
        }
        case OPC_EmitMergeInputChains: {
          Util.assertion(inputChain.getNode() == null,
              "emitMergeInputChains should be the first chain producing ndoe");

          int numChains = matcherTable[matcherIndex++];
          Util.assertion(numChains != 0, "Can't FP zero chains");
          Util.assertion(chainNodesMatched.isEmpty(), "Should only have one emitMergeInputChains per match");

          // Read all of the chained nodes.
          for (int i = 0; i < numChains; i++) {
            int recNo = matcherTable[matcherIndex++];
            Util.assertion(recNo < recordedNodes.size(), "invalid checkSame");
            SDNode tmpNode = recordedNodes.get(recNo).first.getNode();
            if (!tmpNode.equals(nodeToMatch) &&
                !recordedNodes.get(recNo).first.hasOneUse()) {
              chainNodesMatched.clear();
              break;
            }
            chainNodesMatched.add(tmpNode);
          }

          // If the inner loop broke out, the match fails.
          if (chainNodesMatched.isEmpty())
            break;

          // Merge the input chains if they are not intra-pattern references.
          inputChain = handleMergeInputChains(chainNodesMatched, curDAG);
          if (inputChain.getNode() == null)
            break;
          continue;
        }

        case OPC_EmitCopyToReg: {
          int recNo = matcherTable[matcherIndex++];
          Util.assertion(recNo < recordedNodes.size(), "invalid checkSame");
          int dstPhysReg = matcherTable[matcherIndex++];
          if (inputChain.getNode() == null)
            inputChain = curDAG.getEntryNode();

          inputChain = curDAG.getCopyToReg(inputChain, dstPhysReg,
              recordedNodes.get(recNo).first, inputFlag);
          inputFlag = inputChain.getValue(1);
          continue;
        }
        case OPC_EmitNodeXForm: {
          int xFormNo = matcherTable[matcherIndex++];
          int recNo = matcherTable[matcherIndex++];
          Util.assertion(recNo < recordedNodes.size(), "invalid checkSame");
          recordedNodes.add(Pair.get(runSDNodeXForm(recordedNodes.get(recNo).first, xFormNo), null));
          continue;
        }
        case OPC_EmitNode:
        case OPC_MorphNodeTo: {
          int targetOpc = Integer.parseUnsignedInt(Integer.toUnsignedString(matcherTable[matcherIndex++]&0xff));
          targetOpc |= (Integer.parseUnsignedInt(Integer.toUnsignedString(matcherTable[matcherIndex++]&0x0ff))) << 8;
          int emitNodeInfo = matcherTable[matcherIndex++];

          // get the result vt list.
          int numVTs = matcherTable[matcherIndex++];
          ArrayList<EVT> vts = new ArrayList<>();
          for (int i = 0; i < numVTs; i++) {
            int vt = matcherTable[matcherIndex++];
            if (vt == MVT.iPTR) vt = tli.getPointerTy().simpleVT;
            vts.add(new EVT(vt));
          }

          if ((emitNodeInfo & OPFL_Chain) != 0)
            vts.add(new EVT(MVT.Other));
          if ((emitNodeInfo & OPFL_FlagOutput) != 0)
            vts.add(new EVT(MVT.Glue));

          SDNode.SDVTList vtlist = curDAG.getVTList(vts);

          int numOps = matcherTable[matcherIndex++];
          ArrayList<SDValue> ops = new ArrayList<>();
          for (int i = 0; i < numOps; i++) {
            long recNo = matcherTable[matcherIndex++];
            if ((recNo & 128) != 0) {
              idx.set(matcherIndex);
              recNo = getVBR(recNo, idx);
              matcherIndex = idx.get();
            }

            Util.assertion(recNo < Integer.MAX_VALUE, "too large index in Java");
            Util.assertion(recNo < recordedNodes.size(), "invalid checkSame");
            ops.add(recordedNodes.get((int) recNo).first);
          }

          // If there are variadic operands to add, handle them now.
          if ((emitNodeInfo & OPFL_VariadicInfo) != 0) {
            // Determine the start index to copy from.
            int firstToCopy = getNumFixedFromVariadicInfo(emitNodeInfo);
            firstToCopy += (emitNodeInfo & OPFL_Chain) != 0 ? 1 : 0;
            Util.assertion(nodeToMatch.getNumOperands() >= firstToCopy,
                "invalid variadic node");
            for (int i = firstToCopy, e = nodeToMatch.getNumOperands(); i < e; i++) {
              SDValue v = nodeToMatch.getOperand(i);
              if (v.getValueType().equals(new EVT(MVT.Glue))) break;
              ops.add(v);
            }
          }

          // If this has chain/flag inputs, add them.
          if ((emitNodeInfo & OPFL_Chain) != 0)
            ops.add(inputChain);
          if ((emitNodeInfo & OPFL_FlagInput) != 0 && inputFlag.getNode() != null)
            ops.add(inputFlag);

          MachineSDNode res;
          SDValue[] tempOps = new SDValue[ops.size()];
          ops.toArray(tempOps);
          if (opcode != OPC_MorphNodeTo) {
            // If this is a normal EmitNode command, just create the new node and
            // add the results to the RecordedNodes list.

            res = curDAG.getMachineNode(targetOpc, vtlist, tempOps);
            // Add all the non-flag/non-chain results to the RecordedNodes list.
            for (int i = 0, e = vts.size(); i < e; i++) {
              if (vts.get(i).equals(new EVT(MVT.Other)) ||
                  vts.get(i).equals(new EVT(MVT.Glue)))
                break;
              recordedNodes.add(Pair.get(new SDValue(res, i), null));
            }
          } else {
            res = morphNode(nodeToMatch, targetOpc, vtlist, emitNodeInfo, tempOps);
          }

          if ((emitNodeInfo & OPFL_FlagOutput) != 0) {
            inputFlag = new SDValue(res, vts.size() - 1);
            if ((emitNodeInfo & OPFL_Chain) != 0)
              inputChain = new SDValue(res, vts.size() - 2);
          } else if ((emitNodeInfo & OPFL_Chain) != 0)
            inputChain = new SDValue(res, vts.size() - 1);

          // If the OPFL_MemRefs flag is set on this node, slap all of the
          // accumulated memrefs onto it.
          if ((emitNodeInfo & OPFL_MemRefs) != 0) {
            MachineMemOperand[] refs = new MachineMemOperand[matchedMemRefs.size()];
            matchedMemRefs.toArray(refs);
            res.setMemRefs(refs);
          }

          // If this was a MorphNodeTo then we're completely done!
          if (opcode == OPC_MorphNodeTo) {
            // Update chain and flag uses.
            updateChainsAndFlags(nodeToMatch, inputChain, chainNodesMatched,
                inputFlag, flagResultNodesMatched, true);
            return res;
          }
          continue;
        }
        case OPC_MarkFlagResults: {
          int numNodes = matcherTable[matcherIndex++];
          // Read and remember all the flag-result nodes.
          for (int i = 0; i < numNodes; i++) {
            long recNo = matcherTable[matcherIndex++];
            if ((recNo & 128) != 0) {
              idx.set(matcherIndex);
              recNo = getVBR(recNo, idx);
              matcherIndex = idx.get();
            }
            Util.assertion(recNo < Integer.MAX_VALUE, "integer too large");
            Util.assertion(recNo < recordedNodes.size(), "invalid checkSame");
            flagResultNodesMatched.add(recordedNodes.get((int) recNo).first.getNode());
          }
          continue;
        }
        case OPC_CompleteMatch: {
          // The match has been completed, and any new nodes (if any) have been
          // created.  Patch up references to the matched dag to use the newly
          // created nodes.
          int numResults = matcherTable[matcherIndex++];
          for (int i = 0; i < numResults; i++) {
            long resSlot = matcherTable[matcherIndex++];
            if ((resSlot & 128) != 0) {
              idx.set(matcherIndex);
              resSlot = getVBR(resSlot, idx);
              matcherIndex = idx.get();
            }

            Util.assertion(resSlot < Integer.MAX_VALUE, "integer too large");
            Util.assertion(resSlot < recordedNodes.size(), "invalid checkSame");

            SDValue res = recordedNodes.get((int) resSlot).first;

            // FIXME2: Eliminate this horrible hack by fixing the 'Gen' program
            // after (parallel) on input patterns are removed.  This would also
            // allow us to stop encoding #results in OPC_CompleteMatch's table
            // entry.
            if (nodeToMatch.getNumValues() <= i ||
                nodeToMatch.getValueType(i).equals(new EVT(MVT.Other)) ||
                nodeToMatch.getValueType(i).equals(new EVT(MVT.Glue)))
              break;
            Util.assertion(nodeToMatch.getValueType(i).equals(res.getValueType()) ||
                nodeToMatch.getValueType(i).equals(new EVT(MVT.iPTR)) ||
                res.getValueType().equals(new EVT(MVT.iPTR)) ||
                nodeToMatch.getValueType(i).getSizeInBits() ==
                    res.getValueType().getSizeInBits(), "invalid replacement, mismatch result type");
            curDAG.replaceAllUsesOfValueWith(new SDValue(nodeToMatch, i), res);
          }

          // If the root node defines a flag, add it to the flag nodes to update
          // list.
          if (nodeToMatch.getValueType(nodeToMatch.getNumValues() - 1).getSimpleVT().simpleVT == MVT.Glue)
            flagResultNodesMatched.add(nodeToMatch);

          // Update chain and flag uses.
          updateChainsAndFlags(nodeToMatch, inputChain, chainNodesMatched,
              inputFlag, flagResultNodesMatched, false);
          Util.assertion(nodeToMatch.isUseEmpty(), "Didn't replace all uses of the node?");
          return null;
        }
      }

      // If the code reached this point, then the match failed.  See if there is
      // another child to try in the current 'Scope', otherwise pop it until we
      // find a case to check.
      while (true) {
        if (matchScopes.isEmpty()) {
          cannotYetSelect(nodeToMatch);
          return null;
        }

        // Restore the interpreter state back to the point where the scope was
        // formed.
        MatchScope lastScope = matchScopes.getLast();
        for (int i = recordedNodes.size() - 1, j = recordedNodes.size() - lastScope.numRecordedNodes; j > 0; --i, --j)
          recordedNodes.remove(i);

        recordedNodes.ensureCapacity(lastScope.numRecordedNodes);
        nodeStack.clear();
        nodeStack.addAll(lastScope.nodeStack);
        n = nodeStack.peek();

        if (lastScope.numMatchedMemRefs != matchedMemRefs.size()) {
          Util.assertion(matchedMemRefs.size() > lastScope.numMatchedMemRefs);
          while (matchedMemRefs.size() != lastScope.numMatchedMemRefs)
            matchedMemRefs.remove(matchedMemRefs.size() - 1);
        }
        matcherIndex = lastScope.failIndex;

        inputChain = lastScope.inputChain;
        inputFlag = lastScope.inputFlag;
        if (!lastScope.hasChainNodesMatched)
          chainNodesMatched.clear();
        if (!lastScope.hasFlagResultNodesMatched)
          flagResultNodesMatched.clear();

        // Check to see what the offset is at the new MatcherIndex.  If it is zero
        // we have reached the end of this scope, otherwise we have another child
        // in the current scope to try.
        long numToSkips = matcherTable[matcherIndex++];
        if ((numToSkips & 128) != 0) {
          idx.set(matcherIndex);
          numToSkips = getVBR(numToSkips, idx);
          matcherIndex = idx.get();
        }

        if (numToSkips != 0) {
          lastScope.failIndex = (int) (matcherIndex + numToSkips);
          break;
        }

        // End of this scope, pop it and try the next child in the containing
        // scope.
        matchScopes.pop();
      }
    }
  }

  /**
   * Handle morphing a node in place for the selector (Acutally, we don't morph it in place due to type cast in Java).
   * @param node
   * @param targetOpc
   * @param vts
   * @param emitNodeInfo
   * @param ops
   * @return
   */
  private MachineSDNode morphNode(SDNode node,
                           int targetOpc,
                           SDNode.SDVTList vts,
                           int emitNodeInfo,
                           SDValue... ops) {
    int oldGlueResultNo = -1, oldChainResultNo = -1;
    int numResults = node.getNumValues();
    if (node.getValueType(numResults-1).equals(new EVT(MVT.Glue))) {
      oldGlueResultNo = numResults - 1;
      if (numResults != 1 && node.getValueType(numResults-2).equals(new EVT(MVT.Other)))
        oldChainResultNo = numResults - 2;
    }
    else if (node.getValueType(numResults - 1).equals(new EVT(MVT.Other)))
      oldChainResultNo = numResults - 1;

    MachineSDNode res = curDAG.getMachineNode(targetOpc, vts, ops);
    int resNumResults = res.getNumValues();
    // move the glue if needed.
    if ((emitNodeInfo & OPFL_FlagOutput) != 0 && oldGlueResultNo != -1 &&
        oldGlueResultNo != resNumResults - 1) {
      curDAG.replaceAllUsesOfValueWith(new SDValue(node, oldGlueResultNo), new SDValue(res, resNumResults - 1));
    }

    if ((emitNodeInfo & OPFL_FlagOutput) != 0)
      --resNumResults;

    // Move the chain reference if needed.
    if ((emitNodeInfo & OPFL_Chain) != 0 && oldChainResultNo != -1 &&
        oldChainResultNo != resNumResults - 1) {
      curDAG.replaceAllUsesOfValueWith(new SDValue(node, oldChainResultNo), new SDValue(res, resNumResults-1));
    }

    replaceUses(node, res);
    return res;
  }

  /**
   * This hook allows targets to hack on the graph before
   * instruction selection starts.
   */
  public void preprocessISelDAG() { }

  /**
   * This hook allows the target to hack on the graph
   * right after selection.
   */
  public void postprocessISelDAG() { }

  /**
   * Replace the old node with new one.
   *
   * @param oldNode
   * @param newNode
   */
  public void replaceUses(SDNode oldNode, SDNode newNode) {
    ISelUpdater isu = new ISelUpdater(iselPosition, curDAG.allNodes);
    curDAG.replaceAllUsesWith(oldNode, newNode, isu);
    iselPosition = isu.getISelPos();
  }

  public void replaceUses(SDValue oldVal, SDValue newVal) {
    ISelUpdater isu = new ISelUpdater(iselPosition, curDAG.allNodes);
    curDAG.replaceAllUsesOfValueWith(oldVal, newVal, isu);
    iselPosition = isu.getISelPos();
  }

  public void replaceUses(SDValue[] f, SDValue[] t) {
    ISelUpdater isu = new ISelUpdater(iselPosition, curDAG.allNodes);
    curDAG.replaceAllUsesOfValuesWith(f, t, isu);
  }

  private boolean lowerArguments(BasicBlock llvmBB) {
    Function fn = llvmBB.getParent();
    SelectionDAG dag = sdl.dag;
    TargetData td = tli.getTargetData();

    ArrayList<InputArg> ins = new ArrayList<>();
    int idx = 1;
    for (Argument arg : fn.getArgumentList()) {
      boolean isArgUseEmpty = arg.isUseEmpty();
      ArrayList<EVT> vts = new ArrayList<>();
      computeValueVTs(tli, arg.getType(), vts);
      for (EVT vt : vts) {
        Type argTy = vt.getTypeForEVT(dag.getContext());
        ArgFlagsTy flags = new ArgFlagsTy();
        int originalAlign = td.getABITypeAlignment(argTy);

        if (fn.paramHasAttr(idx, Attribute.ZExt))
          flags.setZExt();
        if (fn.paramHasAttr(idx, Attribute.SExt))
          flags.setSExt();
        if (fn.paramHasAttr(idx, Attribute.InReg))
          flags.setInReg();
        if (fn.paramHasAttr(idx, Attribute.StructRet))
          flags.setSRet();
        if (fn.paramHasAttr(idx, Attribute.ByVal)) {
          flags.setByVal();
          PointerType ty = (PointerType) arg.getType();
          Type elemTy = ty.getElementType();
          int frameAlign = td.getTypeAlign(elemTy);
          long frameSize = td.getTypeSize(elemTy);

          if (fn.getParamAlignment(idx) != 0)
            frameAlign = fn.getParamAlignment(idx);
          flags.setByValAlign(frameAlign);
          flags.setByValSize((int) frameSize);
        }
        if (fn.paramHasAttr(idx, Attribute.Nest))
          flags.setNest();
        flags.setOrigAlign(originalAlign);

        EVT registerVT = tli.getRegisterType(dag.getContext(), vt);
        int numRegs = tli.getNumRegisters(curDAG.getContext(), vt);
        for (int i = 0; i < numRegs; i++) {
          InputArg myArgs = new InputArg(flags, registerVT, isArgUseEmpty);
          if (numRegs > 1 && i == 0) {
            myArgs.flags.setSplit();
          } else if (i > 0)
            myArgs.flags.setOrigAlign(1);
          ins.add(myArgs);
        }
        ++idx;
      }
    }

    ArrayList<SDValue> inVals = new ArrayList<>();
    SDValue newRoot = tli.lowerFormalArguments(dag.getRoot(),
        fn.getCallingConv(), fn.isVarArg(), ins, dag, inVals);

    Util.assertion(newRoot.getNode() != null && newRoot.getValueType().equals(new EVT(MVT.Other)),
        "lowerFormalArguments din't return a valid chain!");
    Util.assertion(inVals.size() == ins.size(),
        "lowerFormalArguments didn't emit the correct number of values");

    dag.setRoot(newRoot);
    // set up the incoming arguments.
    int i = 0;
    idx = 1;
    for (int ii = 0, sz = fn.getNumOfArgs(); ii != sz; ++ii, ++idx) {
      Argument arg = fn.getArgumentList().get(ii);
      ArrayList<SDValue> argValues = new ArrayList<>();
      ArrayList<EVT> vts = new ArrayList<>();
      computeValueVTs(tli, arg.getType(), vts);
      for (EVT vt : vts) {
        EVT partVT = tli.getRegisterType(dag.getContext(), vt);
        int numParts = tli.getNumRegisters(curDAG.getContext(), vt);
        if (!arg.isUseEmpty()) {
          int op = ISD.DELETED_NODE;
          if (fn.paramHasAttr(idx, Attribute.SExt))
            op = ISD.AssertSext;
          else if (fn.paramHasAttr(idx, Attribute.ZExt))
            op = ISD.AssertZext;

          SDValue[] ops = new SDValue[numParts];
          for (int j = 0; j < numParts; j++)
            ops[j] = inVals.get(j + i);
          argValues.add(getCopyFromParts(dag, ops, partVT, vt, op));
        }
        i += numParts;
      }
      if (argValues.isEmpty())
        continue;
      if (argValues.get(0).getNode() instanceof FrameIndexSDNode) {
        FrameIndexSDNode fi = (FrameIndexSDNode) argValues.get(0).getNode();
        funcInfo.setArgumentFrameIndex(arg, fi.getFrameIndex());
      }

      SDValue res = dag.getMergeValues(argValues);
      sdl.setValue(arg, res);
      if (res.getOpcode() == ISD.BUILD_PAIR) {
        if (res.getOperand(0).getNode() instanceof LoadSDNode) {
          LoadSDNode ld = (LoadSDNode) res.getOperand(0).getNode();
          if (ld.getBasePtr().getNode() instanceof FrameIndexSDNode)
            funcInfo.setArgumentFrameIndex(arg, ((FrameIndexSDNode)ld.getBasePtr().getNode()).getFrameIndex());
        }
      }

      if (res.getOpcode() == ISD.CopyFromReg) {
        int reg = ((RegisterSDNode)res.getOperand(1).getNode()).getReg();
        if (TargetRegisterInfo.isVirtualRegister(reg)) {
          funcInfo.valueMap.put(arg, reg);
          continue;
        }
      }
      if (!FunctionLoweringInfo.isOnlyUsedInEntryBlock(arg)) {
        funcInfo.initializeRegForValue(arg);
        sdl.copyToExportRegsIfNeeds(arg);
      }
    }

    Util.assertion(i == inVals.size());

    emitFunctionEntryCode(fn, mf);
    return false;
  }

  /**
   * For special target machine to implement special purpose.
   *
   * @param fn
   * @param mf
   */
  public void emitFunctionEntryCode(Function fn, MachineFunction mf) {
  }

  private void finishBasicBlock() {
    if (Util.DEBUG) {
      System.err.println("Target-post-processed machine code:");
      funcInfo.mbb.dump();

      System.err.printf("Total mount of phi nodes to update: %d%n",
          sdl.phiNodesToUpdate.size());
      int i = 0;
      for (Pair<MachineInstr, Integer> itr : sdl.phiNodesToUpdate) {
        System.err.printf("Node %d : (", i++);
        itr.first.dump();
        System.err.printf(", %d)%n", itr.second);
      }
    }
    if (sdl.switchCases.isEmpty() && sdl.jtiCases.isEmpty() &&
        sdl.bitTestCases.isEmpty()) {
      for (int i = 0, e = sdl.phiNodesToUpdate.size(); i < e; i++) {
        MachineInstr phi = sdl.phiNodesToUpdate.get(i).first;
        Util.assertion(phi.getOpcode() == TargetOpcode.PHI, "This is not a phi node!");
        phi.addOperand(MachineOperand.createReg(sdl.phiNodesToUpdate.get(i).second,
            false, false));
        phi.addOperand(MachineOperand.createMBB(funcInfo.mbb, 0));
      }
      sdl.phiNodesToUpdate.clear();
      return;
    }
    for (int i = 0, e = sdl.bitTestCases.size(); i < e; i++) {
      if (!sdl.bitTestCases.get(i).emitted) {
        funcInfo.mbb = sdl.bitTestCases.get(i).parentMBB;
        sdl.setCurrentBasicBlock(funcInfo.mbb);
        sdl.visitBitTestHeader(sdl.bitTestCases.get(i));
        curDAG.setRoot(sdl.getRoot());
        codeGenAndEmitInst();
        sdl.clear();
      }

      for (int j = 0, sz = sdl.bitTestCases.get(i).cases.size(); j < sz; j++) {
        funcInfo.mbb = (sdl.bitTestCases.get(i).cases.get(j)).thisMBB;
        sdl.setCurrentBasicBlock(funcInfo.mbb);

        if (j + 1 < sz) {
          sdl.visitBitTestCase(sdl.bitTestCases.get(i).cases.get(j + 1).thisMBB,
              sdl.bitTestCases.get(i).reg, sdl.bitTestCases.get(i).cases.get(j));
        } else {
          sdl.visitBitTestCase(sdl.bitTestCases.get(i).defaultMBB,
              sdl.bitTestCases.get(i).reg, sdl.bitTestCases.get(i).cases.get(j));
        }

        curDAG.setRoot(sdl.getRoot());
        codeGenAndEmitInst();
        sdl.clear();
      }

      for (int pi = 0, pe = sdl.phiNodesToUpdate.size(); pi < pe; pi++) {
        MachineInstr phi = sdl.phiNodesToUpdate.get(pi).first;
        MachineBasicBlock mbb = phi.getParent();
        Util.assertion(phi.getOpcode() == TargetOpcode.PHI, "This is not a phi node!");

        if (mbb.equals(sdl.bitTestCases.get(i).defaultMBB)) {
          phi.addOperand(MachineOperand.createReg(sdl.phiNodesToUpdate.get(pi).second,
              false, false));
          phi.addOperand(MachineOperand.createMBB(sdl.bitTestCases.get(i).parentMBB, 0));
          phi.addOperand(MachineOperand.createReg(sdl.phiNodesToUpdate.get(pi).second,
              false, false));
          int sz = sdl.bitTestCases.get(i).cases.size();
          phi.addOperand(MachineOperand.createMBB(sdl.bitTestCases.get(i).cases.get(sz - 1).thisMBB, 0));
        }

        for (int j = 0, ej = sdl.bitTestCases.get(i).cases.size(); j < ej; j++) {
          MachineBasicBlock bb = sdl.bitTestCases.get(i).cases.get(j).thisMBB;
          if (!mbb.getSuccessors().contains(mbb)) {
            phi.addOperand(MachineOperand.createReg(sdl.phiNodesToUpdate.get(pi).second,
                false, false));
            phi.addOperand(MachineOperand.createMBB(bb, 0));
          }
        }
      }
    }
    sdl.bitTestCases.clear();

    for (int i = 0, e = sdl.jtiCases.size(); i < e; i++) {
      if (!sdl.jtiCases.get(i).first.emitted) {
        funcInfo.mbb = sdl.jtiCases.get(i).first.headerBB;
        sdl.setCurrentBasicBlock(funcInfo.mbb);
        sdl.visitJumpTableHeader(sdl.jtiCases.get(i).second,
            sdl.jtiCases.get(i).first);
        curDAG.setRoot(sdl.getRoot());
        codeGenAndEmitInst();
        sdl.clear();
      }

      funcInfo.mbb = sdl.jtiCases.get(i).second.mbb;
      sdl.setCurrentBasicBlock(funcInfo.mbb);

      sdl.visitJumpTable(sdl.jtiCases.get(i).second);
      curDAG.setRoot(sdl.getRoot());
      codeGenAndEmitInst();
      sdl.clear();

      for (int pi = 0, pe = sdl.phiNodesToUpdate.size(); pi < pe; pi++) {
        MachineInstr phi = sdl.phiNodesToUpdate.get(pi).first;
        MachineBasicBlock phiMBB = phi.getParent();
        Util.assertion(phi.getOpcode() == TargetOpcode.PHI, "This is not a PHI node!");
        if (phiMBB.equals(sdl.jtiCases.get(i).second.defaultMBB)) {
          phi.addOperand(MachineOperand.createReg(sdl.phiNodesToUpdate.get(pi).second,
              false, false));
          phi.addOperand(MachineOperand.createMBB(sdl.jtiCases.get(i).first.headerBB, 0));
        }
        if (funcInfo.mbb.getSuccessors().contains(phiMBB)) {
          phi.addOperand(MachineOperand.createReg(sdl.phiNodesToUpdate.get(pi).second,
              false, false));
          phi.addOperand(MachineOperand.createMBB(funcInfo.mbb, 0));
        }
      }
    }
    sdl.jtiCases.clear();

    for (int i = 0, e = sdl.phiNodesToUpdate.size(); i < e; i++) {
      MachineInstr phi = sdl.phiNodesToUpdate.get(i).first;
      Util.assertion(phi.getOpcode() == TargetOpcode.PHI);
      if (funcInfo.mbb.isSuccessor(phi.getParent())) {
        phi.addOperand(MachineOperand.createReg(sdl.phiNodesToUpdate.get(i).second,
            false, false));
        phi.addOperand(MachineOperand.createMBB(funcInfo.mbb, 0));
      }
    }

    for (int i = 0, e = sdl.switchCases.size(); i < e; i++) {
      funcInfo.mbb = sdl.switchCases.get(i).thisMBB;
      sdl.setCurrentBasicBlock(funcInfo.mbb);

      sdl.visitSwitchCase(sdl.switchCases.get(i));
      curDAG.setRoot(sdl.getRoot());
      codeGenAndEmitInst();
      sdl.clear();

      while ((funcInfo.mbb = sdl.switchCases.get(i).trueMBB) != null) {
        for (int pi = 0, sz = funcInfo.mbb.size(); pi < sz &&
            funcInfo.mbb.getInstAt(pi).getOpcode() == TargetOpcode.PHI; ++pi) {
          MachineInstr phi = funcInfo.mbb.getInstAt(pi);
          for (int pn = 0; ; ++pn) {
            Util.assertion(pn != sdl.phiNodesToUpdate.size(), "Didn't find PHI entry!");
            if (sdl.phiNodesToUpdate.get(pn).first.equals(phi)) {
              phi.addOperand(MachineOperand.createReg(sdl.phiNodesToUpdate.get(pn).second,
                  false, false));
              phi.addOperand(MachineOperand.createMBB(sdl.switchCases.get(i).thisMBB, 0));
              break;
            }
          }
        }

        if (funcInfo.mbb.equals(sdl.switchCases.get(i).falseMBB))
          sdl.switchCases.get(i).falseMBB = null;

        sdl.switchCases.get(i).trueMBB = sdl.switchCases.get(i).falseMBB;
        sdl.switchCases.get(i).falseMBB = null;
      }
      Util.assertion(sdl.switchCases.get(i).trueMBB == null && sdl.switchCases.get(i).falseMBB == null);
    }

    sdl.switchCases.clear();
    sdl.phiNodesToUpdate.clear();
  }

  public boolean isLegalAndProfitableToFold(SDValue node, SDNode use, SDNode root) {
    if (optLevel == CodeGenOpt.None || !node.hasOneUse()) return false;

    EVT vt = root.getValueType(root.getNumValues() - 1);
    while (vt.equals(new EVT(MVT.Glue))) {
      SDNode fu = findFlagUse(root);
      if (fu == null)
        break;
      root = fu;
      vt = root.getValueType(root.getNumValues() - 1);
    }
    return !isNonImmUse(root, node.getNode(), use);
  }

  private static SDNode findFlagUse(SDNode node) {
    int flagResNo = node.getNumValues() - 1;
    for (SDUse u : node.useList) {
      if (u.getResNo() == flagResNo)
        return u.getUser();
    }
    return null;
  }

  private static boolean isNonImmUse(SDNode root, SDNode def, SDNode immedUse) {
    HashSet<SDNode> visited = new HashSet<>();
    return findNonImmUse(root, def, immedUse, root, visited);
  }

  private static boolean findNonImmUse(SDNode use, SDNode def, SDNode immedUse, SDNode root,
                               HashSet<SDNode> visited) {
    if (use.getNodeID() < def.getNodeID() || !visited.add(use))
      return false;

    for (int i = 0, e = use.getNumOperands(); i < e; i++) {
      SDNode n = use.getOperand(i).getNode();
      if (n.equals(def)) {
        if (use.equals(immedUse) || use.equals(root))
          continue;

        Util.assertion(!Objects.equals(n, root));
        return true;
      }

      if (findNonImmUse(n, def, immedUse, root, visited))
        return true;
    }
    return false;
  }

  @Override
  public String getPassName() {
    return "Instruction Selector based on DAG covering";
  }

  protected boolean checkAndMask(SDValue lhs, ConstantSDNode rhs,
                                 long desiredMaskS) {
    APInt actualMask = rhs.getAPIntValue();
    APInt desiredMask = new APInt(lhs.getValueSizeInBits(), desiredMaskS);
    if (actualMask.eq(desiredMask))
      return true;

    if (actualMask.intersects(desiredMask.negative()))
      return false;

    APInt neededMask = desiredMask.and(actualMask.negative());
    if (curDAG.maskedValueIsZero(lhs, neededMask, 0))
      return true;

    return false;
  }

  protected boolean checkOrMask(SDValue lhs, ConstantSDNode rhs,
                                long desiredMaskS) {
    APInt actualMask = rhs.getAPIntValue();
    APInt desiredMask = new APInt(lhs.getValueSizeInBits(), desiredMaskS);
    if (actualMask.eq(desiredMask))
      return true;

    if (actualMask.intersects(desiredMask.negative()))
      return false;

    APInt neededMask = desiredMask.and(actualMask.negative());
    APInt[] res = new APInt[2];
    curDAG.computeMaskedBits(lhs, neededMask, res, 0);
    APInt knownZero = res[0], knownOne = res[1];
    if (neededMask.and(knownOne).eq(neededMask))
      return true;

    return false;
  }

  public static boolean isChainCompatible(SDNode chain, SDNode op) {
    if (chain.getOpcode() == ISD.EntryToken)
      return true;
    if (chain.getOpcode() == ISD.TokenFactor)
      return false;
    if (chain.getNumOperands() > 0) {
      SDValue c0 = chain.getOperand(0);
      if (c0.getValueType().equals(new EVT(MVT.Other)))
        return !c0.getNode().equals(op) && isChainCompatible(c0.getNode(), op);
    }
    return true;
  }

  /**
   * Select the specified address as a target
   * addressing mode, according to the specified constraint code.  If this does
   * not match or is not implemented, return true.  The resultant operands
   * (which will appear in the machine instruction) should be added to the
   * outOps vector.
   * @param outOps
   * @return
   */
  public boolean selectInlineAsmMemoryOperands(SDValue op,
                                               char constraintCode,
                                               ArrayList<SDValue> outOps) {
    return true;
  }

  public void selectInlineAsmMemoryOperand(ArrayList<SDValue> ops) {
    Util.shouldNotReachHere("InlineAsm not supported!");
  }

  public SDNode select_INLINEASM(SDValue n) {
    ArrayList<SDValue> ops = new ArrayList<>();
    for (int i = 0, e = n.getNumOperands(); i < e; i++)
      ops.add(n.getOperand(i));
    selectInlineAsmMemoryOperand(ops);

    ArrayList<EVT> vts = new ArrayList<>();
    vts.add(new EVT(MVT.Other));
    vts.add(new EVT(MVT.Glue));
    SDValue newNode = curDAG.getNode(ISD.INLINEASM, vts);
    return newNode.getNode();
  }

  public SDNode select_UNDEF(SDValue n) {
    return curDAG.selectNodeTo(n.getNode(), TargetOpcode.IMPLICIT_DEF,
        n.getValueType());
  }

  public SDNode select_PROLOG_LABEL(SDValue n) {
    SDValue chain = n.getOperand(0);
    int c = ((LabelSDNode) n.getNode()).getLabelID();
    SDValue tmp = curDAG.getTargetConstant(c, new EVT(MVT.i32));
    return curDAG.selectNodeTo(n.getNode(), TargetOpcode.PROLOG_LABEL,
        new EVT(MVT.Other), tmp, chain);
  }

  public SDNode select_DECLARE(SDValue n) {
    Util.shouldNotReachHere("This method should be overrided by concrete target!");
    return null;
  }

  public void cannotYetSelect(SDNode n) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream os = new PrintStream(baos);
    os.print("Cannot yet selectCommonCode: ");
    n.print(os, curDAG);
    os.close();
    Util.shouldNotReachHere(baos.toString());
  }

  public void cannotYetSelectIntrinsic(SDNode n) {
    reportFatalError("Cannot yet selectCommonCode intrinsic function.");
  }

  private void computeLiveOutVRegInfo() {
    HashSet<SDNode> visitedNodes = new HashSet<>();
    LinkedList<SDNode> worklist = new LinkedList<>();

    worklist.add(curDAG.getRoot().getNode());

    APInt mask;
    APInt[] knownVals = new APInt[2];

    do {
      SDNode n = worklist.pop();
      if (!visitedNodes.add(n))
        continue;

      // Otherwise, add all chain operands to the worklist.
      for (int i = 0, e = n.getNumOperands(); i < e; ++i) {
        if (n.getOperand(i).getValueType().getSimpleVT().simpleVT == MVT.Other)
          worklist.push(n.getOperand(i).getNode());
      }

      // if this is a CopyToReg with a vreg dest, process it.
      if (n.getOpcode() != ISD.CopyToReg)
        continue;

      int destReg = ((SDNode.RegisterSDNode)n.getOperand(1).getNode()).getReg();
      if (!TargetRegisterInfo.isVirtualRegister(destReg))
        continue;

      // Ignore non-scalar or non-integer values.
      SDValue src = n.getOperand(2);
      EVT srcVT = src.getValueType();
      if (!srcVT.isInteger() || srcVT.isVector())
        continue;

      int numSignBits = curDAG.computeNumSignBits(src);
      mask = APInt.getAllOnesValue(srcVT.getSizeInBits());
      curDAG.computeMaskedBits(src, mask, knownVals, 0);
      funcInfo.addLiveOutRegInfo(destReg, numSignBits, knownVals[0], knownVals[1]);
    }while (!worklist.isEmpty());
  }

  private void prepareEHLandingPad() {
    MachineBasicBlock mbb = funcInfo.mbb;
    MCSymbol label = mf.getMMI().addLandingPad(mbb);
    mf.getMMI().setCallSiteLandingPad(label, sdl.lpadToCallSiteMap.get(mbb));

    MCInstrDesc mcid = tm.getSubtarget().getInstrInfo().get(TargetOpcode.EH_LABEL);
    buildMI(mbb, funcInfo.insertPtr, sdl.getCurDebugLoc(), mcid).addMCSym(label);

    int reg = tli.getExceptionPointerRegister();
    if (reg != 0) mbb.addLiveIn(reg);

    reg = tli.getExceptionSelectorRegister();
    if (reg != 0) mbb.addLiveIn(reg);

    BasicBlock llvmBB = mbb.getBasicBlock();
    BranchInst br = llvmBB.getTerminator() instanceof BranchInst ?
            (BranchInst)llvmBB.getTerminator() : null;
    if (br != null && br.isUnconditional()) {
      int i = 0, e = llvmBB.size() - 1;
      for (; i != e; ++i) {
        if (llvmBB.getInstAt(i) instanceof CallInst) {
          CallInst ci = (CallInst) llvmBB.getInstAt(i);
          if (ci.getCalledFunction().getIntrinsicID() == Intrinsic.ID.eh_selector)
            break;
        }
      }
      if (i == e)
        copyCatchInfo(br.getSuccessor(0), llvmBB, mf.getMMI(), funcInfo);
    }
  }

  private void copyCatchInfo(BasicBlock succBB, BasicBlock lpad,
                             MachineModuleInfo mmi, FunctionLoweringInfo funcInfo) {
    HashSet<BasicBlock> visited = new HashSet<>();

    // The 'eh.selector' call may not be in the direct successor of a basic block,
    // but could be several successors deeper. If we don't find it, try going one
    // level further.
    while (visited.add(succBB)) {
      for (Instruction inst : succBB) {
        if (inst instanceof CallInst && ((CallInst)inst).getCalledFunction()
                .getIntrinsicID() == Intrinsic.ID.eh_selector) {
          sdl.addCatchInfo((CallInst) inst, mmi, funcInfo.mbbmap.get(lpad));
          return;
        }

        BranchInst br = succBB.getTerminator() instanceof BranchInst ?
                (BranchInst) succBB.getTerminator() : null;
        if (br != null && br.isUnconditional())
          succBB = br.getSuccessor(0);
        else
          break;
      }
    }
  }
}
