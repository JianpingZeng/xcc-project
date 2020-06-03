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

import backend.codegen.*;
import backend.codegen.dagisel.SDNode.*;
import backend.debug.DebugLoc;
import backend.mc.MCInstrDesc;
import backend.mc.MCRegisterClass;
import backend.target.TargetInstrInfo;
import backend.target.TargetOpcode;
import backend.target.TargetRegisterInfo;
import backend.target.TargetSubtarget;
import backend.type.Type;
import backend.value.ConstantFP;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.Util;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Stack;

import static backend.codegen.MachineInstrBuilder.buildMI;
import static backend.codegen.dagisel.SDep.Kind.Data;
import static backend.codegen.dagisel.SDep.Kind.Order;
import static backend.mc.MCOperandInfo.OperandConstraint.TIED_TO;
import static backend.target.TargetRegisterInfo.isPhysicalRegister;
import static backend.target.TargetRegisterInfo.isVirtualRegister;

/**
 * A ScheduleDAG for scheduling SDNode-based DAGs.
 * <p>
 * Edges between SUnits are initially based on edges in the SelectionDAG,
 * and additional edges can be added by the schedulers as heuristics.
 * SDNodes such as Constants, Registers, and a few others that are not
 * interesting to schedulers are not allocated SUnits.
 * <p>
 * SDNodes with MVT::Flag operands are grouped along with the flagged
 * nodes into a single SUnit so that they are scheduled together.
 * <p>
 * SDNode-based scheduling graphs do not use SDep::Anti or SDep::Output
 * edges.  Physical register dependence information is not carried in
 * the DAG and must be handled explicitly by schedulers.
 */
public abstract class ScheduleDAGSDNodes extends ScheduleDAG {
  public SelectionDAG dag;

  public ScheduleDAGSDNodes(MachineFunction mf) {
    super(mf);
  }

  public void run(SelectionDAG dag, MachineBasicBlock mbb, int insertPos) {
    this.dag = dag;
    super.run(dag, mbb, insertPos);
  }

  private static boolean isPassiveNode(SDNode node) {
    if (node == null) return false;
    switch (node.getOpcode()) {
      case ISD.Constant:
      case ISD.TargetConstant:
      case ISD.ConstantFP:
      case ISD.TargetConstantFP:
      case ISD.GlobalAddress:
      case ISD.TargetGlobalAddress:
      case ISD.TargetGlobalTLSAddress:
      case ISD.GlobalTLSAddress:
      case ISD.Register:
      case ISD.BasicBlock:
      case ISD.FrameIndex:
      case ISD.TargetFrameIndex:
      case ISD.ConstantPool:
      case ISD.TargetConstantPool:
      case ISD.JumpTable:
      case ISD.TargetJumpTable:
      case ISD.ExternalSymbol:
      case ISD.BlockAddress:
      case ISD.TargetExternalSymbol:
      case ISD.TargetBlockAddress:
      case ISD.MEMOPERAND:
      case ISD.EntryToken:
        return true;
      default:
        return false;
    }
  }

  public SUnit newSUnit(SDNode n) {
    SUnit su = new SUnit(n, sunits.size());
    su.originNode = su;
    sunits.add(su);
    return su;
  }

  public SUnit clone(SUnit u) {
    SUnit su = new SUnit(u.getNode(), -1);
    su.originNode = u.originNode;
    su.latency = u.latency;
    su.isTwoAddress = u.isTwoAddress;
    su.isCommutable = u.isCommutable;
    su.hasPhysRegDefs = u.hasPhysRegDefs;
    su.isCloned = true;
    return su;
  }

  public void buildSchedGraph() {
    buildSchedUnits();
    addSchedEdges();
  }

  public void computeLatency(SUnit su) {
    Util.shouldNotReachHere("Should not reach here!");
  }

  public static int countResults(SDNode node) {
    int n = node.getNumValues();
    while (n != 0 && node.getValueType(n - 1).getSimpleVT().simpleVT == MVT.Glue)
      --n;
    if (n > 0 && node.getValueType(n - 1).getSimpleVT().simpleVT == MVT.Other)
      --n;
    return n;
  }

  public static int countOperands(SDNode node) {
    int n = node.getNumOperands();
    while (n > 0 && node.getOperand(n - 1).getValueType().getSimpleVT().simpleVT == MVT.Glue)
      --n;
    if (n > 0 && node.getOperand(n - 1).getValueType().getSimpleVT().simpleVT == MVT.Other)
      --n;
    while (n > 0 && node.getOperand(n - 1).getNode() instanceof MemOperandSDNode)
      --n;
    return n;
  }

  public void emitNode(SDNode node,
                       boolean isClone,
                       boolean isCloned,
                       TObjectIntHashMap<SDValue> vrBaseMap) {
    // if this is a machine instruction.
    if (node.isMachineOpecode()) {
      int opc = node.getMachineOpcode();
      if (opc == TargetOpcode.EXTRACT_SUBREG ||
          opc == TargetOpcode.INSERT_SUBREG ||
          opc == TargetOpcode.SUBREG_TO_REG) {
        emitSubregNode(node, vrBaseMap);
        return;
      }

      if (opc == TargetOpcode.COPY_TO_REGCLASS) {
        emitCopyToRegClassNode(node, vrBaseMap);
        return;
      }

      if (opc == TargetOpcode.IMPLICIT_DEF) {
        // We just want a unique VR for each IMPLICIT_DEF use.
        return;
      }

      MCInstrDesc tid = tii.get(opc);
      int numResults = countResults(node);
      int nodeOperands = countOperands(node);
      boolean hasPhysRegOuts = numResults > tid.getNumDefs() &&
          tid.getImplicitDefs() != null &&
          tid.getImplicitDefs().length > 0;
      // create a machine instruction.
      MachineInstr mi = buildMI(tid).getMInstr();

      if (numResults > 0) {
        createVirtualRegisters(node, mi, tid, isClone, isCloned, vrBaseMap);
      }

      boolean hasOptPRefs = tid.getNumDefs() > numResults;
      Util.assertion(!hasOptPRefs || !hasPhysRegOuts);
      int numSkip = hasOptPRefs ? tid.getNumDefs() - numResults : 0;
      // insert new operand into this machine instruction.
      for (int i = numSkip; i < nodeOperands; i++) {
        addOperand(mi, node.getOperand(i), i - numSkip + tid.getNumDefs(), tid,
            vrBaseMap);
      }

      mbb.insert(insertPos++, mi);
      if (hasPhysRegOuts) {
        for (int i = tid.getNumDefs(); i < numResults; i++) {
          int reg = tid.getImplicitDefs()[i - tid.getNumDefs()];
          if (node.hasAnyUseOfValue(i))
            emitCopyFromReg(node, i, isClone, isCloned, reg, vrBaseMap);
        }
      }
      return;
    }

    switch (node.getOpcode()) {
      default:
        Util.shouldNotReachHere(
            "This is target-independent node should have been selected");
        break;
      case ISD.EntryToken:
        Util.shouldNotReachHere("EntryToken should have been excluded from the schedule!");
        break;
      case ISD.MERGE_VALUES:
      case ISD.TokenFactor:
        break;
      case ISD.CopyToReg: {
        SDValue srcVal = node.getOperand(2);
        int srcReg = 0;
        if (srcVal.getNode() instanceof RegisterSDNode)
          srcReg = ((RegisterSDNode) srcVal.getNode()).getReg();
        else
          srcReg = getVR(srcVal, vrBaseMap);

        int destReg = ((RegisterSDNode) node.getOperand(1).getNode()).getReg();
        if (srcReg == destReg) break;

        MCRegisterClass srcRC = null, destRC = null;
        if (isVirtualRegister(srcReg))
          srcRC = mri.getRegClass(srcReg);
        else
          srcRC = tri.getPhysicalRegisterRegClass(srcReg);

        if (isVirtualRegister(destReg))
          destRC = mri.getRegClass(destReg);
        else
          destRC = tri.getPhysicalRegisterRegClass(destReg, node.getOperand(1).getValueType());

        buildMI(mbb, insertPos++, node.getDebugLoc(), tii.get(TargetOpcode.COPY), destReg).addReg(srcReg);
        break;
      }
      case ISD.CopyFromReg:
        int srcReg = ((RegisterSDNode) node.getOperand(1).getNode()).getReg();
        emitCopyFromReg(node, 0, isClone, isCloned, srcReg, vrBaseMap);
        break;
      case ISD.INLINEASM:
        Util.shouldNotReachHere("InlineAsm not supported currently!");
        break;
    }
  }

  public MachineBasicBlock emitSchedule() {
    TObjectIntHashMap<SDValue> vrBaseMap = new TObjectIntHashMap<>();
    TObjectIntHashMap<SUnit> copyVRBaseMap = new TObjectIntHashMap<>();
    for (SUnit su : sequence) {
      if (su == null) {
        emitNoop();
        continue;
      }

      if (su.getNode() == null) {
        emitPhysRegCopy(su, copyVRBaseMap);
        continue;
      }

      LinkedList<SDNode> flaggedNodes = new LinkedList<>();
      for (SDNode flag = su.getNode().getFlaggedNode(); flag != null;
           flag = flag.getFlaggedNode())
        flaggedNodes.add(flag);

      while (!flaggedNodes.isEmpty()) {
        SDNode sn = flaggedNodes.removeLast();
        emitNode(sn, !Objects.equals(su.originNode, su), su.isCloned, vrBaseMap);
      }
      emitNode(su.getNode(), !Objects.equals(su.originNode, su), su.isCloned, vrBaseMap);
    }
    return mbb;
  }

  @Override
  public abstract void schedule();

  public void dumpNode(SUnit su) {
    if (su.getNode() == null) {
      System.err.println("PHYS REG COPY!");
      return;
    }

    su.getNode().dump(dag);
    System.err.println();
    LinkedList<SDNode> flaggedNodes = new LinkedList<>();
    for (SDNode n = su.getNode().getFlaggedNode(); n != null; n = n.getFlaggedNode()) {
      flaggedNodes.add(n);
    }
    while (!flaggedNodes.isEmpty()) {
      System.err.print("    ");
      SDNode n = flaggedNodes.removeLast();
      n.dump(dag);
      System.err.println();
    }
  }

  public String getGraphNodeLabel(SUnit su) {
    StringBuilder sb = new StringBuilder();
    sb.append("SU(").append(su.nodeNum).append("): ");
    if (su.getNode() != null) {
      LinkedList<SDNode> flaggedNodes = new LinkedList<>();
      for (SDNode n = su.getNode(); n != null; n = n.getFlaggedNode())
        flaggedNodes.add(n);

      while (!flaggedNodes.isEmpty()) {
        sb.append(SelectionDAGDotGraphTraits.getNodeLabel(flaggedNodes.getLast(), dag, false));
        flaggedNodes.removeLast();
        if (!flaggedNodes.isEmpty())
          sb.append("\n    ");
      }
    } else {
      sb.append("CROSS RC COPY");
    }
    return sb.toString();
  }

  /**
   * Generates machine instruction for subreg SDNode.
   *
   * @param node
   * @param vrBaseMap
   */
  private void emitSubregNode(SDNode node,
                              TObjectIntHashMap<SDValue> vrBaseMap) {
    int vrBase = 0;
    int opc = node.getMachineOpcode();

    for (SDUse u : node.getUseList()) {
      SDNode user = u.getUser();
      if (user.getOpcode() == ISD.CopyToReg &&
          user.getOperand(2).getNode().equals(node)) {
        int destReg = ((RegisterSDNode) user.getOperand(1).getNode()).getReg();
        if (isVirtualRegister(destReg)) {
          vrBase = destReg;
          break;
        }
      }
    }

    if (opc == TargetOpcode.EXTRACT_SUBREG) {
      long subIdx = ((ConstantSDNode) node.getOperand(1).getNode()).getZExtValue();
      MachineInstr mi = buildMI(tii.get(TargetOpcode.EXTRACT_SUBREG)).getMInstr();

      int vreg = getVR(node.getOperand(0), vrBaseMap);
      MCRegisterClass destRC = mri.getRegClass(vreg);
      MCRegisterClass srcRC = destRC.getSubRegisterRegClass(subIdx);
      Util.assertion(srcRC != null, "Invalid subregister index in EXTRACT_SUBREG");

      if (vrBase == 0 || srcRC != mri.getRegClass(vrBase)) {
        vrBase = mri.createVirtualRegister(srcRC);
      }

      mi.addOperand(MachineOperand.createReg(vrBase, true, false));
      addOperand(mi, node.getOperand(0), 0, null, vrBaseMap);
      mi.addOperand(MachineOperand.createImm(subIdx));
      mbb.insert(insertPos++, mi);
    } else if (opc == TargetOpcode.INSERT_SUBREG ||
        opc == TargetOpcode.SUBREG_TO_REG) {
      SDValue n0 = node.getOperand(0);
      SDValue n1 = node.getOperand(1);
      SDValue n2 = node.getOperand(2);
      int subReg = getVR(n1, vrBaseMap);
      int subIdx = (int) ((ConstantSDNode) n2.getNode()).getZExtValue();
      MCRegisterClass destRC = mri.getRegClass(subReg);
      MCRegisterClass srcRC = destRC
          .getSuperRegisterRegClass(tri, destRC, subIdx, node.getValueType(0));

      if (vrBase == 0 || !srcRC.equals(mri.getRegClass(vrBase))) {
        vrBase = mri.createVirtualRegister(srcRC);
      }

      MachineInstr mi = buildMI(tii.get(opc)).getMInstr();
      mi.addOperand(MachineOperand.createReg(vrBase, true, false));

      if (opc == TargetOpcode.SUBREG_TO_REG) {
        ConstantSDNode sdn = (ConstantSDNode) n0.getNode();
        mi.addOperand(MachineOperand.createImm(sdn.getZExtValue()));
      } else
        addOperand(mi, n0, 0, null, vrBaseMap);

      addOperand(mi, n1, 0, null, vrBaseMap);
      mi.addOperand(MachineOperand.createImm(subIdx));
      mbb.insert(insertPos++, mi);
    } else
      Util.shouldNotReachHere("Node is not insert_subreg, extract_subreg, or subreg_to_reg");

    SDValue op = new SDValue(node, 0);
    Util.assertion(!vrBaseMap.containsKey(op), "Node emitted out of order!");
    vrBaseMap.put(op, vrBase);
  }

  private void emitCopyToRegClassNode(SDNode node,
                                      TObjectIntHashMap<SDValue> vrBaseMap) {
    int vreg = getVR(node.getOperand(0), vrBaseMap);
    MCRegisterClass srcRC = mri.getRegClass(vreg);

    int destRCIdx = (int) ((ConstantSDNode) node.getOperand(1).getNode()).getZExtValue();
    MCRegisterClass destRC = tri.getRegClass(destRCIdx);

    int newVReg = mri.createVirtualRegister(destRC);
    buildMI(mbb, insertPos++, new DebugLoc(), tii.get(TargetOpcode.COPY), newVReg).addReg(vreg);
    SDValue op = new SDValue(node, 0);
    Util.assertion(!vrBaseMap.containsKey(op));
    vrBaseMap.put(op, newVReg);
  }

  /**
   * Return the virtual register corresponding to the
   * specified SDValue.
   */
  private int getVR(SDValue op, TObjectIntHashMap<SDValue> vrBaseMap) {
    if (op.isMachineOpcode() && op.getMachineOpcode() == TargetOpcode.IMPLICIT_DEF) {
      int vreg = getDstOfOnlyCopyToRegUse(op.getNode(), op.getResNo());
      if (vreg == 0) {
        MCRegisterClass rc = tli.getRegClassFor(op.getValueType());
        vreg = mri.createVirtualRegister(rc);
      }
      buildMI(mbb, insertPos++, op.getDebugLoc(), tii.get(TargetOpcode.IMPLICIT_DEF), vreg);
      return vreg;
    }

    Util.assertion(vrBaseMap.containsKey(op));
    return vrBaseMap.get(op);
  }

  private int getDstOfOnlyCopyToRegUse(SDNode node, int resNo) {
    if (!node.hasOneUse())
      return 0;

    SDNode user = node.getUseList().get(0).getUser();
    if (user.getOpcode() == ISD.CopyToReg &&
        user.getOperand(2).getNode().equals(node) &&
        user.getOperand(2).getResNo() == resNo) {
      int reg = ((RegisterSDNode) user.getOperand(1).getNode()).getReg();
      if (isVirtualRegister(reg))
        return reg;
    }
    return 0;
  }

  private void addOperand(MachineInstr mi,
                          SDValue op,
                          int iiOpNum,
                          MCInstrDesc tid,
                          TObjectIntHashMap<SDValue> vrBaseMap) {
    if (op.isMachineOpcode())
      addRegisterOperand(mi, op, iiOpNum, tid, vrBaseMap);
    else if (op.getNode() instanceof ConstantSDNode) {
      long imm = ((ConstantSDNode) op.getNode()).getSExtValue();
      mi.addOperand(MachineOperand.createImm(imm));
    } else if (op.getNode() instanceof ConstantFPSDNode) {
      ConstantFP imm = ((ConstantFPSDNode) op.getNode()).getConstantFPValue();
      mi.addOperand(MachineOperand.createFPImm(imm));
    } else if (op.getNode() instanceof RegisterSDNode) {
      int reg = ((RegisterSDNode) op.getNode()).getReg();
      boolean isImp = tid != null && iiOpNum >= tid.getNumOperands() && !tid.isVariadic();
      mi.addOperand(MachineOperand.createReg(reg, false, isImp));
    } else if (op.getNode() instanceof GlobalAddressSDNode) {
      GlobalAddressSDNode gas = (GlobalAddressSDNode) op.getNode();
      mi.addOperand(MachineOperand.createGlobalAddress(gas.getGlobalValue(),
          gas.getOffset(), gas.getTargetFlags()));
    } else if (op.getNode() instanceof BasicBlockSDNode) {
      BasicBlockSDNode bb = (BasicBlockSDNode) op.getNode();
      mi.addOperand(MachineOperand.createMBB(bb.getBasicBlock(), 0));
    } else if (op.getNode() instanceof FrameIndexSDNode) {
      FrameIndexSDNode fi = (FrameIndexSDNode) op.getNode();
      mi.addOperand(MachineOperand.createFrameIndex(fi.getFrameIndex()));
    } else if (op.getNode() instanceof JumpTableSDNode) {
      JumpTableSDNode jti = (JumpTableSDNode) op.getNode();
      mi.addOperand(MachineOperand.createJumpTableIndex(jti.getJumpTableIndex(),
          jti.getTargetFlags()));
    } else if (op.getNode() instanceof ConstantPoolSDNode) {
      ConstantPoolSDNode cp = (ConstantPoolSDNode) op.getNode();
      int offset = cp.getOffset();
      int align = cp.getAlignment();
      Type ty = cp.getType();
      if (align == 0) {
        align = tm.getTargetData().getPrefTypeAlignment(ty);
        if (align == 0) {
          align = (int) tm.getTargetData().getTypeAllocSize(ty);
        }
      }
      int idx;
      if (cp.isMachineConstantPoolValue())
        idx = mcpl.getConstantPoolIndex(cp.getMachineConstantPoolValue(), align);
      else
        idx = mcpl.getConstantPoolIndex(cp.getConstantValue(), align);
      mi.addOperand(MachineOperand.createConstantPoolIndex(idx, offset,
          cp.getTargetFlags()));
    } else if (op.getNode() instanceof ExternalSymbolSDNode) {
      ExternalSymbolSDNode es = (ExternalSymbolSDNode) op.getNode();
      mi.addOperand(MachineOperand.createExternalSymbol(es.getExtSymol(),
          0, es.getTargetFlags()));
    }
    else if (op.getNode() instanceof BlockAddressSDNode) {
      BlockAddressSDNode ba = (BlockAddressSDNode) op.getNode();
      mi.addOperand(MachineOperand.createBlockAddress(ba.getBlockAddress(), ba.getTargetFlags()));
    }
    else {
      Util.assertion(!op.getValueType().equals(new EVT(MVT.Other)) &&
          !op.getValueType().equals(new EVT(MVT.Glue)));
      addRegisterOperand(mi, op, iiOpNum, tid, vrBaseMap);
    }
  }

  private static final int MinRCSize = 4;

  private void addRegisterOperand(MachineInstr mi,
                                  SDValue op,
                                  int iiOpNum,
                                  MCInstrDesc tid,
                                  TObjectIntHashMap<SDValue> vrBaseMap) {
    Util.assertion(!op.getValueType().equals(new EVT(MVT.Other)) &&
        !op.getValueType().equals(new EVT(MVT.Glue)));

    int vreg = getVR(op, vrBaseMap);
    Util.assertion(isVirtualRegister(vreg));

    MCInstrDesc ii = mi.getDesc();
    boolean isOptDef = iiOpNum < ii.getNumOperands() && ii.opInfo[iiOpNum].isOptionalDef();

    if (tid != null) {
      MCRegisterClass srcRC = mri.getRegClass(vreg);
      MCRegisterClass destRC = null;
      if (iiOpNum < tid.getNumOperands())
        destRC = tid.opInfo[iiOpNum].getRegisterClass(tri);
      Util.assertion(destRC != null || (ii.isVariadic() &&
          iiOpNum >= ii.getNumOperands()), "Don't have operand info for this instruction!");

      // It is normal to emit a copy between two different register classes, such as GPR to GPRnopc
      // but those two register classes have a common sub register class, GPRnopc, we use emit a
      // virtual register with the GPRnopc RC.
      if (destRC != null && mri.constraintRegClass(vreg, destRC, MinRCSize) == null) {
        int newVReg = mri.createVirtualRegister(destRC);
        buildMI(mbb, insertPos++, new DebugLoc(), tii.get(TargetOpcode.COPY), newVReg).addReg(vreg);
        vreg = newVReg;
      }
    }

    mi.addOperand(MachineOperand.createReg(vreg, isOptDef, false));
  }

  private void emitCopyFromReg(SDNode node,
                               int resNo,
                               boolean isClone,
                               boolean isCloned,
                               int srcReg,
                               TObjectIntHashMap<SDValue> vrBaseMap) {
    int vrbase = 0;
    if (isVirtualRegister(srcReg)) {
      SDValue op = new SDValue(node, resNo);
      if (isClone)
        vrBaseMap.remove(op);

      Util.assertion(!vrBaseMap.containsKey(op));
      vrBaseMap.put(op, srcReg);
      return;
    }

    boolean matchReg = true;
    MCRegisterClass useRC = null;

    EVT vt = node.getValueType(resNo);
    if (tli.isTypeLegal(vt))
      useRC = tli.getRegClassFor(vt);

    if (!isClone && !isCloned) {
      for (SDUse u : node.useList) {
        SDNode user = u.getUser();
        boolean match = true;
        if (user.getOpcode() == ISD.CopyToReg &&
            user.getOperand(2).getNode().equals(node) &&
            user.getOperand(2).getResNo() == resNo) {
          int destReg = ((RegisterSDNode) user.getOperand(1).getNode()).getReg();
          if (isVirtualRegister(destReg)) {
            vrbase = destReg;
            match = false;
          } else if (destReg != srcReg) {
            match = false;
          }
        } else {
          for (int i = 0, e = user.getNumOperands(); i < e; i++) {
            SDValue op = user.getOperand(i);
            if (!op.getNode().equals(node) || op.getResNo() != resNo)
              continue;

            if (vt.getSimpleVT().simpleVT == MVT.Other ||
                vt.getSimpleVT().simpleVT == MVT.Glue)
              continue;

            match = false;
            if (user.isMachineOpecode()) {
              MCInstrDesc ii = tii.get(user.getMachineOpcode());
              MCRegisterClass rc = null;
              if (i + ii.getNumDefs() < ii.getNumOperands())
                rc = ii.opInfo[i + ii.getNumDefs()].getRegisterClass(tri);
              if (useRC == null)
                useRC = rc;
              else if (rc != null) {
                MCRegisterClass comRC = tri.getCommonSubClass(useRC, rc);
                if (comRC != null)
                  useRC = comRC;
              }
            }
          }
        }
        matchReg &= match;
        if (vrbase != 0)
          break;
      }

      MCRegisterClass srcRC, destRC;
      srcRC = tri.getPhysicalRegisterRegClass(srcReg, vt);

      if (vrbase != 0)
        destRC = mri.getRegClass(vrbase);
      else if (useRC != null)
        destRC = useRC;
      else
        destRC = tli.getRegClassFor(vt);

      if (matchReg && srcRC.getCopyCost() < 0)
        vrbase = srcReg;
      else {
        vrbase = mri.createVirtualRegister(destRC);
        buildMI(mbb, insertPos++, new DebugLoc(), tii.get(TargetOpcode.COPY), vrbase).addReg(srcReg);
      }

      SDValue op = new SDValue(node, resNo);
      if (isClone)
        vrBaseMap.remove(op);
      Util.assertion(!vrBaseMap.containsKey(op), "Node emitted out of order!");
      vrBaseMap.put(op, vrbase);
    }
  }

  private void createVirtualRegisters(SDNode node,
                                      MachineInstr mi,
                                      MCInstrDesc tid,
                                      boolean isClone,
                                      boolean isCloned,
                                      TObjectIntHashMap<SDValue> vrBaseMap) {
    Util.assertion(node.getMachineOpcode() != TargetOpcode.IMPLICIT_DEF);
    for (int i = 0; i < tid.getNumDefs(); i++) {
      int vrbase = 0;
      MCRegisterClass rc = tid.opInfo[i].getRegisterClass(tri);
      if (tid.opInfo[i].isOptionalDef()) {
        int numResult = countResults(node);
        vrbase = ((RegisterSDNode) node.getOperand(i - numResult).getNode()).getReg();
        Util.assertion(isPhysicalRegister(vrbase));
        mi.addOperand(MachineOperand.createReg(vrbase, true, false));
      }

      if (vrbase == 0 && !isClone && !isCloned) {
        for (SDUse u : node.getUseList()) {
          SDNode user = u.getUser();
          if (user.getOpcode() == ISD.CopyToReg &&
              user.getOperand(2).getNode().equals(node) &&
              user.getOperand(2).getResNo() == i) {
            int reg = ((RegisterSDNode) user.getOperand(1).getNode()).getReg();
            if (isVirtualRegister(reg)) {
              MCRegisterClass regRC = mri.getRegClass(reg);
              if (regRC.equals(rc)) {
                vrbase = reg;
                mi.addOperand(MachineOperand.createReg(reg, true, false));
                break;
              }
            }
          }
        }

        if (vrbase == 0) {
          Util.assertion(rc != null);
          vrbase = mri.createVirtualRegister(rc);
          mi.addOperand(MachineOperand.createReg(vrbase, true, false));
        }

        SDValue op = new SDValue(node, i);
        if (isClone)
          vrBaseMap.remove(op);
        Util.assertion(!vrBaseMap.containsKey(op));
        vrBaseMap.put(op, vrbase);
      }
    }
  }

  private void buildSchedUnits() {
    for (SDNode node : dag.allNodes)
      node.setNodeID(-1);

    boolean unitLatencies = forceUnitLatencies();
    // Walk through the DAG in the order of depth-first, so that we can avoid traverse
    // those "dead" nodes (pruned from DAG by instruction selector).
    Stack<SDNode> worklist = new Stack<>();
    worklist.push(dag.getRoot().getNode());
    HashSet<SDNode> visited = new HashSet<>();
    visited.add(dag.getRoot().getNode());

    while (!worklist.isEmpty()) {
      SDNode curNode = worklist.pop();
      // Add all operands to the worklist unless they've already been added.
      for (int i = 0, e = curNode.getNumOperands(); i < e; i++) {
        SDNode child = curNode.getOperand(i).getNode();
        if (!visited.contains(child)) {
          visited.add(child);
          worklist.push(child);
        }
      }

      if (isPassiveNode(curNode) || curNode.getNodeID() != -1)
        continue;

      SUnit nodeSUnit = newSUnit(curNode);
      SDNode n = curNode;
      while (n.getNumOperands() != 0 &&
          n.getOperand(n.getNumOperands() - 1).getValueType().getSimpleVT()
              .simpleVT == MVT.Glue) {
        n = n.getOperand(n.getNumOperands() - 1).getNode();
        Util.assertion(n.getNodeID() == -1);
        n.setNodeID(nodeSUnit.nodeNum);
      }

      n = curNode;
      while (n.getValueType(n.getNumValues() - 1).getSimpleVT().simpleVT == MVT.Glue) {
        SDValue flagVal = new SDValue(n, n.getNumValues() - 1);
        boolean hasFlagUse = false;
        for (SDUse u : n.useList) {
          if (flagVal.isOperandOf(u.getUser())) {
            hasFlagUse = true;
            Util.assertion(n.getNodeID() == -1);
            n.setNodeID(nodeSUnit.nodeNum);
            n = u.getUser();
            break;
          }
        }
        if (!hasFlagUse) break;
      }
      nodeSUnit.setNode(n);
      Util.assertion(n.getNodeID() == -1);
      n.setNodeID(nodeSUnit.nodeNum);

      if (unitLatencies)
        nodeSUnit.latency = 1;
      else
        computeLatency(nodeSUnit);
    }
  }

  private void addSchedEdges() {
    TargetSubtarget ts = tm.getSubtarget();
    boolean unitLatencies = forceUnitLatencies();

    for (int i = 0, e = sunits.size(); i < e; i++) {
      SUnit su = sunits.get(i);
      SDNode node = su.getNode();

      if (node.isMachineOpecode()) {
        int opc = node.getMachineOpcode();
        MCInstrDesc tid = tii.get(opc);
        for (int j = 0; j < tid.getNumOperands(); j++) {
          if (tid.getOperandConstraint(j, TIED_TO) != -1) {
            su.isTwoAddress = true;
            break;
          }
        }
        if (tid.isCommutable())
          su.isCommutable = true;
      }
      for (SDNode n = su.getNode(); n != null; n = n.getFlaggedNode()) {
        if (n.isMachineOpecode() &&
            tii.get(n.getMachineOpcode()).getImplicitDefs() != null) {
          su.hasPhysRegClobbers = true;
          int numUsed = countResults(n);
          while (numUsed != 0 && !n.hasAnyUseOfValue(numUsed - 1))
            --numUsed;
          if (numUsed > tii.get(n.getMachineOpcode()).getNumDefs())
            su.hasPhysRegDefs = true;
        }

        for (int j = 0, sz = n.getNumOperands(); j < sz; j++) {
          SDNode opN = n.getOperand(j).getNode();
          if (isPassiveNode(opN)) continue;
          SUnit opSU = sunits.get(opN.getNodeID());
          Util.assertion(opSU != null);
          if (Objects.equals(opSU, su)) continue;
          if (n.getOperand(j).getValueType().equals(new EVT(MVT.Glue)))
            continue;

          EVT opVT = n.getOperand(j).getValueType();
          Util.assertion(!opVT.equals(new EVT(MVT.Glue)));
          boolean isChain = opVT.equals(new EVT(MVT.Other));

          int[] res = checkForPhysRegDependency(opN, n, i, tri, tii);
          int physReg = res[0];
          int cost = res[1];

          Util.assertion(physReg == 0 || !isChain);
          if (cost >= 0)
            physReg = 0;

          SDep dep = new SDep(opSU, isChain ? Order : Data,
              opSU.latency, physReg, false, false, false);
          if (!isChain && !unitLatencies) {
            computeOperandLatency(opSU, su, dep);
            ts.adjustSchedDependency(opSU, su, dep);
          }

          su.addPred(dep);
        }
      }
    }
  }

  private static int[] checkForPhysRegDependency(SDNode def,
                                                 SDNode user,
                                                 int op,
                                                 TargetRegisterInfo tri,
                                                 TargetInstrInfo tii) {
    // returned array layouts as follows.
    // 0 -- phyReg
    // 1 -- cost.
    int[] res = {0, 1};
    if (op != 2 || user.getOpcode() != ISD.CopyToReg)
      return res;
    int reg = ((RegisterSDNode) user.getOperand(1).getNode()).getReg();
    if (isVirtualRegister(reg))
      return res;

    int resNo = user.getOperand(2).getResNo();
    if (def.isMachineOpecode()) {
      MCInstrDesc tid = tii.get(def.getMachineOpcode());
      if (resNo >= tid.getNumDefs() && tid.implicitDefs != null &&
          tid.implicitDefs[resNo - tid.getNumDefs()] == reg) {
        res[0] = reg;
        MCRegisterClass rc = tri.getPhysicalRegisterRegClass(reg, def.getValueType(resNo));
        res[1] = rc.getCopyCost();
      }
    }
    return res;
  }

  @Override
  public void addCustomGraphFeatures(ScheduleDAGDotTraits graphWriter) {
    if (dag != null) {
      graphWriter.emitSimpleNode(null, "plaintext=circle", "GraphRoot");
      SDNode n = dag.getRoot().getNode();
      if (n != null && n.getNodeID() != -1) {
        graphWriter.emitEdge(null, -1, sunits.get(n.getNodeID()), -1,
            "color=blue,style=dashed");
      }
    }
  }
}
