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

package backend.target.x86;

import backend.codegen.*;
import backend.codegen.dagisel.*;
import backend.codegen.dagisel.SDNode.*;
import backend.debug.DebugLoc;
import backend.mc.MCRegisterClass;
import backend.pass.Pass;
import backend.pass.RegisterPass;
import backend.support.Attribute;
import backend.target.TargetInstrInfo;
import backend.target.TargetMachine;
import backend.target.TargetMachine.CodeModel;
import backend.target.TargetOpcodes;
import backend.value.Function;
import backend.value.GlobalValue;
import tools.OutRef;
import tools.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import static backend.codegen.MachineInstrBuilder.buildMI;
import static backend.codegen.dagisel.LoadExtType.EXTLOAD;
import static backend.codegen.dagisel.LoadExtType.NON_EXTLOAD;
import static backend.codegen.dagisel.MemIndexedMode.UNINDEXED;
import static backend.target.x86.X86ISelAddressMode.BaseType.FrameIndexBase;
import static backend.target.x86.X86ISelAddressMode.BaseType.RegBase;
import static backend.target.x86.X86RegisterInfo.*;

public abstract class X86DAGISel extends SelectionDAGISel {
  protected X86TargetLowering tli;
  protected X86Subtarget subtarget;

  public X86DAGISel(X86TargetMachine tm, TargetMachine.CodeGenOpt optLevel) {
    super(tm, optLevel);
    try {
      new RegisterPass("Instruction Selector based on DAG covering", "dag-isel",
          Class.forName("backend.target.x86.X86GenDAGISel").asSubclass(Pass.class));
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }

    subtarget = tm.getSubtarget();
    tli = tm.getTargetLowering();
  }

  /**
   * This hook allows targets to hack on the graph before
   * instruction selection starts.
   */
  @Override
  public void preprocessISelDAG() {
    Function f = mf.getFunction();
    optForSize = f.hasFnAttr(Attribute.OptimizeForSize);
    if (optLevel != TargetMachine.CodeGenOpt.None)
      preprocessForRMW();

    preprocessForFPConvert();
  }

  public static X86DAGISel createX86DAGISel(X86TargetMachine tm, TargetMachine.CodeGenOpt level) {
    return new X86GenDAGISel(tm, level);
  }

  @Override
  public String getPassName() {
    return "X86 DAG To DAG Instruction Selector";
  }

  @Override
  public void emitFunctionEntryCode(Function fn, MachineFunction mf) {
    // If this is a main function, emit special code for it.
    MachineBasicBlock mbb = mf.getEntryBlock();
    if (fn.hasExternalLinkage() && fn.getName().equals("main")) {
      emitSpecialCodeForMain(mbb, mf.getFrameInfo());
    }
  }

  @Override
  public boolean isLegalAndProfitableToFold(SDValue node, SDNode use, SDNode root) {
    if (optLevel == TargetMachine.CodeGenOpt.None) return false;

    if (Objects.equals(use, root)) {
      switch (use.getOpcode()) {
        default:
          break;
        case ISD.ADD:
        case ISD.ADDC:
        case ISD.ADDE:
        case ISD.AND:
        case ISD.XOR:
        case ISD.OR: {
          SDValue op1 = use.getOperand(1);

          // If the other operand is a 8-bit immediate we should fold the immediate
          // instead. This reduces code size.
          // e.g.
          // movl 4(%esp), %eax
          // addl $4, %eax
          // vs.
          // movl $4, %eax
          // addl 4(%esp), %eax
          // The former is 2 bytes shorter. In case where the increment is 1, then
          // the saving can be 4 bytes (by using incl %eax).
          if (op1.getNode() instanceof ConstantSDNode) {
            ConstantSDNode imm = (ConstantSDNode) op1.getNode();
            if (imm.getAPIntValue().isSignedIntN(8))
              return false;
          }

          // If the other operand is a TLS address, we should fold it instead.
          // This produces
          // movl    %gs:0, %eax
          // leal    i@NTPOFF(%eax), %eax
          // instead of
          // movl    $i@NTPOFF, %eax
          // addl    %gs:0, %eax
          // if the block also has an access to a second TLS address this will save
          // a load.
          if (op1.getOpcode() == X86ISD.Wrapper) {
            SDValue val = op1.getOperand(0);
            if (val.getOpcode() == ISD.TargetGlobalTLSAddress)
              return false;
          }
        }
      }
    }
    // Call the super's method to cope with the generic situation.
    return super.isLegalAndProfitableToFold(node, use, root);
  }

  private static boolean hasNoSignedComparisonUses(SDNode n) {
    // Examine each user of the node.
    for (SDUse su : n.getUseList()) {
      SDNode u = su.getUser();
      if (u.getOpcode() != ISD.CopyToReg)
        return false;

      // Only examine CopyToReg uses that copy to EFLAGS.
      if (((RegisterSDNode) u.getOperand(1).getNode()).getReg() != X86GenRegisterNames.EFLAGS)
        return false;

      // Examine each user of the CopyToReg use.
      for (SDUse flagUI : u.getUseList()) {
        // Only examine the Flag result.
        if (flagUI.getResNo() != 1) continue;

        if (!flagUI.getUser().isMachineOpecode()) return false;
        switch (flagUI.getUser().getMachineOpcode()) {
          // These comparisons don't treat the most significant bit specially.
          case X86GenInstrNames.SETAr:
          case X86GenInstrNames.SETAEr:
          case X86GenInstrNames.SETBr:
          case X86GenInstrNames.SETBEr:
          case X86GenInstrNames.SETEr:
          case X86GenInstrNames.SETNEr:
          case X86GenInstrNames.SETPr:
          case X86GenInstrNames.SETNPr:
          case X86GenInstrNames.SETAm:
          case X86GenInstrNames.SETAEm:
          case X86GenInstrNames.SETBm:
          case X86GenInstrNames.SETBEm:
          case X86GenInstrNames.SETEm:
          case X86GenInstrNames.SETNEm:
          case X86GenInstrNames.SETPm:
          case X86GenInstrNames.SETNPm:
          /*case X86GenInstrNames.JA_4:
          case X86GenInstrNames.JAE_4:
          case X86GenInstrNames.JB_4:
          case X86GenInstrNames.JBE_4:
          case X86GenInstrNames.JE_4:
          case X86GenInstrNames.JNE_4:
          case X86GenInstrNames.JP_4:
          case X86GenInstrNames.JNP_4:*/
          case X86GenInstrNames.CMOVA16rr:
          case X86GenInstrNames.CMOVA16rm:
          case X86GenInstrNames.CMOVA32rr:
          case X86GenInstrNames.CMOVA32rm:
          case X86GenInstrNames.CMOVA64rr:
          case X86GenInstrNames.CMOVA64rm:
          case X86GenInstrNames.CMOVAE16rr:
          case X86GenInstrNames.CMOVAE16rm:
          case X86GenInstrNames.CMOVAE32rr:
          case X86GenInstrNames.CMOVAE32rm:
          case X86GenInstrNames.CMOVAE64rr:
          case X86GenInstrNames.CMOVAE64rm:
          case X86GenInstrNames.CMOVB16rr:
          case X86GenInstrNames.CMOVB16rm:
          case X86GenInstrNames.CMOVB32rr:
          case X86GenInstrNames.CMOVB32rm:
          case X86GenInstrNames.CMOVB64rr:
          case X86GenInstrNames.CMOVB64rm:
          case X86GenInstrNames.CMOVBE16rr:
          case X86GenInstrNames.CMOVBE16rm:
          case X86GenInstrNames.CMOVBE32rr:
          case X86GenInstrNames.CMOVBE32rm:
          case X86GenInstrNames.CMOVBE64rr:
          case X86GenInstrNames.CMOVBE64rm:
          case X86GenInstrNames.CMOVE16rr:
          case X86GenInstrNames.CMOVE16rm:
          case X86GenInstrNames.CMOVE32rr:
          case X86GenInstrNames.CMOVE32rm:
          case X86GenInstrNames.CMOVE64rr:
          case X86GenInstrNames.CMOVE64rm:
          case X86GenInstrNames.CMOVNE16rr:
          case X86GenInstrNames.CMOVNE16rm:
          case X86GenInstrNames.CMOVNE32rr:
          case X86GenInstrNames.CMOVNE32rm:
          case X86GenInstrNames.CMOVNE64rr:
          case X86GenInstrNames.CMOVNE64rm:
          case X86GenInstrNames.CMOVNP16rr:
          case X86GenInstrNames.CMOVNP16rm:
          case X86GenInstrNames.CMOVNP32rr:
          case X86GenInstrNames.CMOVNP32rm:
          case X86GenInstrNames.CMOVNP64rr:
          case X86GenInstrNames.CMOVNP64rm:
          case X86GenInstrNames.CMOVP16rr:
          case X86GenInstrNames.CMOVP16rm:
          case X86GenInstrNames.CMOVP32rr:
          case X86GenInstrNames.CMOVP32rm:
          case X86GenInstrNames.CMOVP64rr:
          case X86GenInstrNames.CMOVP64rm:
            continue;
            // Anything else: assume conservatively.
          default:
            return false;
        }
      }
    }
    return true;
  }

  @Override
  public SDNode select(SDNode node) {
    EVT nvt = node.getValueType(0);
    int opc, mopc;
    int opcode = node.getOpcode();

    if (Util.DEBUG) {
      System.err.print("Selecting: ");
      node.dump(curDAG);
      System.err.println();
    }
    if (node.isMachineOpecode()) {
      // Already been selected.
      return null;
    }

    switch (opcode) {
      default:
        break;
      case X86ISD.GlobalBaseReg:
        return getGlobalBaseReg();
      case X86ISD.ATOMOR64_DAG:
        return selectAtomic64(node, X86GenInstrNames.ATOMOR6432);
      case X86ISD.ATOMXOR64_DAG:
        return selectAtomic64(node, X86GenInstrNames.ATOMXOR6432);
      case X86ISD.ATOMADD64_DAG:
        return selectAtomic64(node, X86GenInstrNames.ATOMADD6432);
      case X86ISD.ATOMSUB64_DAG:
        return selectAtomic64(node, X86GenInstrNames.ATOMSUB6432);
      case X86ISD.ATOMAND64_DAG:
        return selectAtomic64(node, X86GenInstrNames.ATOMAND6432);
      case X86ISD.ATOMNAND64_DAG:
        return selectAtomic64(node, X86GenInstrNames.ATOMNAND6432);
      case X86ISD.ATOMSWAP64_DAG:
        return selectAtomic64(node, X86GenInstrNames.ATOMSWAP6432);

      case ISD.ATOMIC_LOAD_ADD: {
        SDNode retVal = selectAtomicLoadAdd(node, nvt);
        if (retVal != null)
          return retVal;
        break;
      }

      case ISD.SMUL_LOHI:
      case ISD.UMUL_LOHI: {
        SDValue n0 = node.getOperand(0);
        SDValue n1 = node.getOperand(1);

        boolean isSigned = opcode == ISD.SMUL_LOHI;
        int[][] OpcTable = {
            {X86GenInstrNames.MUL8r, X86GenInstrNames.MUL16r, X86GenInstrNames.MUL32r, X86GenInstrNames.MUL64r},
            {X86GenInstrNames.IMUL8r, X86GenInstrNames.IMUL16r, X86GenInstrNames.IMUL32r, X86GenInstrNames.IMUL64r}
        };
        int[][] MOpcTable = {
            {X86GenInstrNames.MUL8m, X86GenInstrNames.MUL16m, X86GenInstrNames.MUL32m, X86GenInstrNames.MUL64m},
            {X86GenInstrNames.IMUL8m, X86GenInstrNames.IMUL16m, X86GenInstrNames.IMUL32m, X86GenInstrNames.IMUL64m}
        };
        int vt = nvt.getSimpleVT().simpleVT;
        Util.assertion(vt >= MVT.i8 && vt <= MVT.i64, "Unsupported VT!");
        opc = OpcTable[isSigned ? 1 : 0][vt - MVT.i8];
        mopc = MOpcTable[isSigned ? 1 : 0][vt - MVT.i8];

        int[][] Regs = {{X86GenRegisterNames.AL, X86GenRegisterNames.AH},
            {X86GenRegisterNames.AX, X86GenRegisterNames.DX},
            {X86GenRegisterNames.EAX, X86GenRegisterNames.EDX},
            {X86GenRegisterNames.RAX, X86GenRegisterNames.RDX},
        };
        int loReg = Regs[vt - MVT.i8][0];
        int hiReg = Regs[vt - MVT.i8][1];

        SDValue[] tmps = new SDValue[5];
        boolean foldedLoad = tryFoldLoad(node, n1, tmps);
        // multiply is commutative
        if (!foldedLoad) {
          foldedLoad = tryFoldLoad(node, n0, tmps);
          if (foldedLoad) {
            SDValue t = n0;
            n0 = n1;
            n1 = t;
          }
        }

        SDValue inFlag = curDAG.getCopyToReg(curDAG.getEntryNode(),
            loReg, n0, new SDValue()).getValue(1);
        if (foldedLoad) {
          SDValue[] ops = {tmps[0], tmps[1], tmps[2], tmps[3], tmps[4], n1.getOperand(0),
              inFlag};
          SDNode chainedNode = curDAG.getMachineNode(mopc, new EVT(MVT.Other),
              new EVT(MVT.Glue), ops);
          inFlag = new SDValue(chainedNode, 1);
          replaceUses(n1.getValue(1), new SDValue(chainedNode, 0));
        } else {
          inFlag = new SDValue(curDAG.getMachineNode(opc, new EVT(MVT.Glue),
              n1, inFlag), 0);
        }

        // Copy the low half of the result, if it is needed.
        if (!new SDValue(node, 0).isUseEmpty()) {
          SDValue result = curDAG.getCopyFromReg(curDAG.getEntryNode(), loReg, nvt,
              inFlag);
          inFlag = result.getValue(2);
          replaceUses(new SDValue(node, 0), result);
          if (Util.DEBUG) {
            System.err.print("=> ");
            result.getNode().dump(curDAG);
            System.err.println();
          }
        }

        // Copy the high half of the result, if it is needed.
        if (!new SDValue(node, 1).isUseEmpty()) {
          SDValue result;
          if (hiReg == X86GenRegisterNames.AH & subtarget.is64Bit()) {
            // Prevent use of AH in a REX instruction by referencing AX instead.
            // Shift it down 8 bits.
            result = curDAG.getCopyFromReg(curDAG.getEntryNode(),
                X86GenRegisterNames.AX, new EVT(MVT.i16), inFlag);
            inFlag = result.getValue(2);
            result = new SDValue(curDAG.getMachineNode(X86GenInstrNames.SHR16r1,
                new EVT(MVT.i16), result, curDAG.getTargetConstant(8,
                    new EVT(MVT.i8))), 0);
            // Then truncate it down to i8.
            result = curDAG.getTargetExtractSubreg(SUBREG_8BIT,
                new EVT(MVT.i8), result);
          } else {
            result = curDAG.getCopyFromReg(curDAG.getEntryNode(), hiReg, nvt, inFlag);
            inFlag = result.getValue(2);
          }
          replaceUses(new SDValue(node, 1), result);
          if (Util.DEBUG) {
            System.err.print("=> ");
            result.getNode().dump(curDAG);
            System.err.println();
          }
        }
        return null;
      }
      case ISD.SDIVREM:
      case ISD.UDIVREM: {
        SDValue n0 = node.getOperand(0), n1 = node.getOperand(1);
        boolean isSigned = opcode == ISD.SDIVREM;
        int[][][] OpcodeTable = {
            {
                {X86GenInstrNames.DIV8r, X86GenInstrNames.DIV8m},
                {X86GenInstrNames.DIV16r, X86GenInstrNames.DIV16m},
                {X86GenInstrNames.DIV32r, X86GenInstrNames.DIV32m},
                {X86GenInstrNames.DIV64r, X86GenInstrNames.DIV64m}
            },
            {
                {X86GenInstrNames.IDIV8r, X86GenInstrNames.IDIV8m},
                {X86GenInstrNames.IDIV16r, X86GenInstrNames.IDIV16m},
                {X86GenInstrNames.IDIV32r, X86GenInstrNames.IDIV32m},
                {X86GenInstrNames.IDIV64r, X86GenInstrNames.IDIV64m}
            }
        };
        int vt = nvt.getSimpleVT().simpleVT;
        Util.assertion(vt >= MVT.i8 && vt <= MVT.i64, "Unsupported VT!");
        opc = OpcodeTable[isSigned ? 1 : 0][vt - MVT.i8][0];
        mopc = OpcodeTable[isSigned ? 1 : 0][vt - MVT.i8][1];
        int[][] RegAndExtOpc = {
            {
                X86GenRegisterNames.AL,
                X86GenRegisterNames.AH,
                X86GenRegisterNames.AH,
                0,
                X86GenInstrNames.CBW
            },
            {
                X86GenRegisterNames.AX,
                X86GenRegisterNames.DX,
                X86GenInstrNames.MOV16r0,
                X86GenRegisterNames.DX,
                X86GenInstrNames.CWD},
            {
                X86GenRegisterNames.EAX,
                X86GenRegisterNames.EDX,
                X86GenRegisterNames.EDX,
                X86GenInstrNames.MOV32r0,
                X86GenInstrNames.CDQ
            },
            {
                X86GenRegisterNames.RAX,
                X86GenRegisterNames.RDX,
                X86GenRegisterNames.RDX,
                X86GenInstrNames.MOV64r0,
                X86GenInstrNames.CQO
            }
        };
        int loReg, hiReg, clrReg, clrOpcode, sextOpcode;
        loReg = RegAndExtOpc[vt - MVT.i8][0];
        hiReg = RegAndExtOpc[vt - MVT.i8][1];
        clrReg = RegAndExtOpc[vt - MVT.i8][2];
        clrOpcode = RegAndExtOpc[vt - MVT.i8][3];
        sextOpcode = RegAndExtOpc[vt - MVT.i8][4];

        SDValue[] tmps = new SDValue[5];
        boolean foldedLoad = tryFoldLoad(node, n1, tmps);
        boolean signBitIsZero = curDAG.signBitIsZero(n0, 0);

        SDValue inFlag;
        if (vt == MVT.i8 && (!isSigned || signBitIsZero)) {
          // Special case for div8, just use a move with zero extension to AX to
          // clear the upper 8 bits (AH).
          SDValue[] temp = new SDValue[5];
          SDValue move, chain;
          if (tryFoldLoad(node, n0, temp)) {
            SDValue[] ops = {temp[0], temp[1], temp[2], temp[3], temp[4], n0.getOperand(0)};
            move = new SDValue(curDAG.getMachineNode(X86GenInstrNames.MOVZX16rm8,
                new EVT(MVT.i16),
                new EVT(MVT.Other),
                ops), 0);
            chain = move.getValue(1);
            replaceUses(n0.getValue(1), chain);
          } else {
            move = new SDValue(curDAG.getMachineNode(X86GenInstrNames.MOVZX16rr8,
                new EVT(MVT.i16), n0), 0);
            chain = curDAG.getEntryNode();
          }
          chain = curDAG.getCopyToReg(chain, X86GenRegisterNames.AX,
              move, new SDValue());
          inFlag = chain.getValue(1);
        } else {
          inFlag = curDAG.getCopyToReg(curDAG.getEntryNode(), loReg, n0, new SDValue()).getValue(1);
          if (isSigned && !signBitIsZero) {
            inFlag = new SDValue(curDAG.getMachineNode(sextOpcode, new EVT(MVT.Glue), inFlag), 0);
          } else {
            SDValue clrNode = new SDValue(curDAG.getMachineNode(clrOpcode, nvt), 0);
            inFlag = curDAG.getCopyToReg(curDAG.getEntryNode(), clrReg, clrNode,
                inFlag).getValue(1);
          }
        }

        if (foldedLoad) {
          SDValue[] ops = {tmps[0], tmps[1], tmps[2], tmps[3], tmps[4], n1.getOperand(0),
              inFlag};
          SDNode chainedNode = curDAG.getMachineNode(mopc, new EVT(MVT.Other),
              new EVT(MVT.Glue), ops);
          inFlag = new SDValue(chainedNode, 1);
          replaceUses(n1.getValue(1), new SDValue(chainedNode, 0));
        } else {
          inFlag = new SDValue(curDAG.getMachineNode(opc, new EVT(MVT.Glue),
              n1, inFlag), 0);
        }

        // Copy the division (low) result, if it is needed.
        if (!new SDValue(node, 0).isUseEmpty()) {
          SDValue result = curDAG.getCopyFromReg(curDAG.getEntryNode(),
              loReg, nvt, inFlag);
          inFlag = result.getValue(2);
          replaceUses(new SDValue(node, 0), result);
          if (Util.DEBUG) {
            System.err.print("=> ");
            result.getNode().dump(curDAG);
            System.err.println();
          }
        }
        // Copy the remainder(high) result if it is needed.
        if (!new SDValue(node, 1).isUseEmpty()) {
          SDValue result;
          if (hiReg == X86GenRegisterNames.AH && subtarget.is64Bit()) {
            // Prevent use of AH in a REX instruction by referencing AX instead.
            // Shift it down 8 bits.
            result = curDAG.getCopyFromReg(curDAG.getEntryNode(),
                X86GenRegisterNames.AX, new EVT(MVT.i16), inFlag);
            inFlag = result.getValue(2);
            result = new SDValue(curDAG.getMachineNode(X86GenInstrNames.SHR16ri,
                new EVT(MVT.i16), result, curDAG.getTargetConstant(8, new EVT(MVT.i8))), 0);
            // then truncate it down to i8
            result = curDAG.getTargetExtractSubreg(SUBREG_8BIT,
                new EVT(MVT.i8), result);
          } else {
            result = curDAG.getCopyFromReg(curDAG.getEntryNode(), hiReg, nvt, inFlag);
            inFlag = result.getValue(2);
          }
          replaceUses(new SDValue(node, 1), result);
          if (Util.DEBUG) {
            System.err.print("=> ");
            result.getNode().dump(curDAG);
            System.err.println();
          }
        }
        return null;
      }

      case X86ISD.CMP: {
        SDValue n0 = node.getOperand(0), n1 = node.getOperand(1);

        // Look for (X86cmp (and $op, $imm), 0) and see if we can convert it to
        // use a smaller encoding.
        if (n0.getNode().getOpcode() == ISD.AND && n0.getNode().hasOneUse() &&
            !n0.getValueType().equals(new EVT(MVT.i8))) {
          if (!(n0.getOperand(1).getNode() instanceof ConstantSDNode))
            break;

          ConstantSDNode c = (ConstantSDNode) n0.getOperand(1).getNode();
          // For example, convert "testl %eax, $8" to "testb %al, $8"
          if ((c.getZExtValue() & ~0xffL) == 0 &&
              ((c.getZExtValue() & 0x80) == 0 || hasNoSignedComparisonUses(node))) {
            SDValue imm = curDAG.getTargetConstant(c.getZExtValue(), new EVT(MVT.i8));
            SDValue reg = n0.getNode().getOperand(0);

            // On x86-32, only the ABCD registers have 8-bit subregisters.
            if (!subtarget.is64Bit()) {
              MCRegisterClass trc = null;
              switch (n0.getValueType().getSimpleVT().simpleVT) {
                case MVT.i32:
                  trc = X86GenRegisterInfo.GR32_ABCDClass.getInstance();
                  break;
                case MVT.i16:
                  trc = X86GenRegisterInfo.GR16_ABCDClass.getInstance();
                  break;
                default:
                  Util.shouldNotReachHere("Unsupported TEST operand type!");
              }
              SDValue rc = curDAG.getTargetConstant(trc.getID(), new EVT(MVT.i32));
              reg = new SDValue(curDAG.getMachineNode(TargetOpcodes.COPY_TO_REGCLASS,
                  reg.getValueType(), reg, rc), 0);
            }

            // Extract the l-register
            SDValue subreg = curDAG.getTargetExtractSubreg(SUBREG_8BIT, new EVT(MVT.i8), reg);
            // emit a testb
            return curDAG.getMachineNode(X86GenInstrNames.TEST8ri, new EVT(MVT.i32), subreg, imm);
          }

          // For example, "testl %eax, $2048" to "testb %ah, $8".
          if ((c.getZExtValue() & ~0xff00L) == 0 &&
              ((c.getZExtValue() & 0x8000) == 0 ||
                  hasNoSignedComparisonUses(node))) {
            // Shift the immediate right by 8 bits.
            SDValue shiftedImm = curDAG.getTargetConstant(c.getZExtValue() >> 8, new EVT(MVT.i8));

            SDValue reg = n0.getNode().getOperand(0);
            MCRegisterClass trc = null;
            switch (n0.getValueType().getSimpleVT().simpleVT) {
              case MVT.i16:
                trc = X86GenRegisterInfo.GR16_ABCDClass.getInstance();
                break;
              case MVT.i32:
                trc = X86GenRegisterInfo.GR32_ABCDClass.getInstance();
                break;
              case MVT.i64:
                trc = X86GenRegisterInfo.GR64_ABCDClass.getInstance();
                break;
              default:
                Util.shouldNotReachHere("Unsupported TEST operand type!");
            }
            SDValue rc = curDAG.getTargetConstant(trc.getID(), new EVT(MVT.i32));
            reg = new SDValue(curDAG.getMachineNode(TargetOpcodes.COPY_TO_REGCLASS,
                reg.getValueType(), reg, rc), 0);

            // Extract the h-register.
            SDValue subreg = curDAG.getTargetExtractSubreg(SUBREG_8BIT_HI,
                new EVT(MVT.i8), reg);

            // Emit a testb. No special NOREX tricks are needed since there's
            // only one GPR operand!
            return curDAG.getMachineNode(X86GenInstrNames.TEST8ri,
                new EVT(MVT.i32), subreg, shiftedImm);
          }

          // For example, "testl %eax, $32776" to "testw %ax, $32776".
          if ((c.getZExtValue() & ~0xffffL) == 0 &&
              !n0.getValueType().equals(new EVT(MVT.i16)) &&
              ((c.getZExtValue() & 0x8000) == 0 || hasNoSignedComparisonUses(node))) {
            SDValue imm = curDAG.getTargetConstant(c.getZExtValue(), new EVT(MVT.i16));
            SDValue reg = n0.getNode().getOperand(0);

            // Extract the 16-bit subregister.
            SDValue subreg = curDAG.getTargetExtractSubreg(SUBREG_16BIT,
                new EVT(MVT.i16), reg);

            // Emit a testw.
            return curDAG.getMachineNode(X86GenInstrNames.TEST16ri, new EVT(MVT.i32), subreg, imm);
          }

          // For example, "testq %rax, $268468232" to "testl %eax, $268468232".
          if ((c.getZExtValue() & ~0xffffffffL) == 0 &&
              n0.getValueType().equals(new EVT(MVT.i64)) &&
              ((c.getZExtValue() & 0x80000000) == 0 ||
                  hasNoSignedComparisonUses(node))) {
            SDValue imm = curDAG.getTargetConstant(c.getZExtValue(), new EVT(MVT.i32));
            SDValue reg = n0.getNode().getOperand(0);

            // Extract the 32-bit subregister.
            SDValue subreg = curDAG.getTargetExtractSubreg(SUBREG_32BIT,
                new EVT(MVT.i32), reg);

            // Emit a testl
            return curDAG.getMachineNode(X86GenInstrNames.TEST32ri, new EVT(MVT.i32), subreg, imm);
          }
        }
        break;
      }
    }

    SDNode resNode = selectCommonCode(node);
    if (Util.DEBUG) {
      System.err.print("=> ");
      if (resNode == null || resNode.equals(node))
        node.dump(curDAG);
      else
        resNode.dump(curDAG);
      System.err.println();
    }
    return resNode;
  }

  private SDNode selectAtomic64(SDNode node, int opc) {
    SDValue chain = node.getOperand(0);
    SDValue in1 = node.getOperand(1);
    SDValue in2 = node.getOperand(2);
    SDValue in3 = node.getOperand(3);
    SDValue[] temp = new SDValue[5];
    if (!selectAddr(in1.getNode(), in2, temp))
      return null;

    SDValue lsi = node.getOperand(4);
    SDValue[] ops = new SDValue[9];
    System.arraycopy(temp, 0, ops, 0, 5);
    ops[5] = in2;
    ops[6] = in3;
    ops[7] = lsi;
    ops[8] = chain;
    EVT[] vts = {new EVT(MVT.i32), new EVT(MVT.i32), new EVT(MVT.Other)};
    return curDAG.getTargetNode(opc, vts, ops);
  }

  private SDNode selectAtomicLoadAdd(SDNode node, EVT vt) {
    if (node.hasAnyUseOfValue(0))
      return null;

    SDValue chain = node.getOperand(0);
    SDValue ptr = node.getOperand(1);
    SDValue val = node.getOperand(2);
    SDValue[] temp = new SDValue[5];
    if (!selectAddr(ptr.getNode(), ptr, temp))
      return null;

    boolean isInc = false, isDec = false, isSub = false, isCN = false;
    ConstantSDNode cn = val.getNode() instanceof ConstantSDNode ? ((ConstantSDNode) val.getNode()) : null;
    if (cn != null) {
      isCN = true;
      long cnVal = cn.getSExtValue();
      if (cnVal == 1)
        isInc = true;
      else if (cnVal == -1)
        isDec = true;
      else if (cnVal >= 0)
        val = curDAG.getTargetConstant(cnVal, vt);
      else {
        isSub = true;
        val = curDAG.getTargetConstant(-cnVal, vt);
      }
    } else if (val.hasOneUse() && val.getOpcode() == ISD.SUB &&
        X86.isZeroMode(val.getOperand(0))) {
      isSub = true;
      val = val.getOperand(1);
    }

    int opc = 0;
    switch (vt.getSimpleVT().simpleVT) {
      default:
        return null;
      case MVT.i8:
        if (isInc)
          opc = X86GenInstrNames.LOCK_INC8m;
        else if (isDec)
          opc = X86GenInstrNames.LOCK_DEC8m;
        else if (isSub) {
          if (isCN)
            opc = X86GenInstrNames.LOCK_SUB8mi;
          else
            opc = X86GenInstrNames.LOCK_SUB8mr;
        } else {
          if (isCN)
            opc = X86GenInstrNames.LOCK_ADD8mi;
          else
            opc = X86GenInstrNames.LOCK_ADD8mr;
        }
        break;
      case MVT.i16:
        if (isInc)
          opc = X86GenInstrNames.LOCK_INC16m;
        else if (isDec)
          opc = X86GenInstrNames.LOCK_DEC16m;
        else if (isSub) {
          if (isCN) {
            if (predicate_i16immSExt8(val.getNode()))
              opc = X86GenInstrNames.LOCK_SUB16mi8;
            else
              opc = X86GenInstrNames.LOCK_SUB16mi;
          } else
            opc = X86GenInstrNames.LOCK_SUB16mr;
        } else {
          if (isCN) {
            if (predicate_i16immSExt8(val.getNode()))
              opc = X86GenInstrNames.LOCK_ADD16mi8;
            else
              opc = X86GenInstrNames.LOCK_ADD16mi;
          } else
            opc = X86GenInstrNames.LOCK_ADD16mr;
        }
        break;
      case MVT.i32:
        if (isInc)
          opc = X86GenInstrNames.LOCK_INC32m;
        else if (isDec)
          opc = X86GenInstrNames.LOCK_DEC32m;
        else if (isSub) {
          if (isCN) {
            if (predicate_i32immSExt8(val.getNode()))
              opc = X86GenInstrNames.LOCK_SUB32mi8;
            else
              opc = X86GenInstrNames.LOCK_SUB32mi;
          } else
            opc = X86GenInstrNames.LOCK_SUB32mr;
        } else {
          if (isCN) {
            if (predicate_i32immSExt8(val.getNode()))
              opc = X86GenInstrNames.LOCK_ADD32mi8;
            else
              opc = X86GenInstrNames.LOCK_ADD32mi;
          } else
            opc = X86GenInstrNames.LOCK_ADD32mr;
        }
        break;
      case MVT.i64:
        if (isInc)
          opc = X86GenInstrNames.LOCK_INC64m;
        else if (isDec)
          opc = X86GenInstrNames.LOCK_DEC64m;
        else if (isSub) {
          opc = X86GenInstrNames.LOCK_SUB64mr;
          if (isCN) {
            if (predicate_i64immSExt8(val.getNode()))
              opc = X86GenInstrNames.LOCK_SUB64mi8;
            else if (predicate_i64immSExt32(val.getNode()))
              opc = X86GenInstrNames.LOCK_SUB64mi32;
          }
        } else {
          opc = X86GenInstrNames.LOCK_ADD64mr;
          if (isCN) {
            if (predicate_i64immSExt8(val.getNode()))
              opc = X86GenInstrNames.LOCK_ADD64mi8;
            else if (predicate_i64immSExt32(val.getNode()))
              opc = X86GenInstrNames.LOCK_ADD64mi32;
          }
        }
    }
    SDValue undef = new SDValue(curDAG.getTargetNode(TargetOpcodes.IMPLICIT_DEF, vt), 0);
    SDValue memOp = curDAG.getMemOperand(((MemSDNode) node).getMemOperand());
    if (isInc || isDec) {
      SDValue[] ops = {temp[0], temp[1], temp[2], temp[3], temp[4], memOp, chain};
      SDValue ret = new SDValue(curDAG.getTargetNode(opc, new EVT(MVT.Other), ops), 0);
      SDValue[] retVals = {undef, ret};
      return curDAG.getMergeValues(retVals).getNode();
    } else {
      SDValue[] ops = {temp[0], temp[1], temp[2], temp[3], temp[4], val, memOp, chain};
      SDValue ret = new SDValue(curDAG.getTargetNode(opc, new EVT(MVT.Other), ops), 0);
      SDValue[] retVals = {undef, ret};
      return curDAG.getMergeValues(retVals).getNode();
    }
  }

  public boolean predicate_i16immSExt8(SDNode n) {
    return false;
  }

  public boolean predicate_i32immSExt8(SDNode n) {
    return false;
  }

  public boolean predicate_i64immSExt8(SDNode n) {
    return false;
  }

  public boolean predicate_i64immSExt32(SDNode n) {
    return false;
  }

  private boolean matchLoadInAddress(LoadSDNode n, X86ISelAddressMode am) {
    SDValue address = n.getOperand(1);
    // load gs:0 -> GS segment register.
    // load fs:0 -> FS segment register.
    //
    // This optimization is valid because the GNU TLS model defines that
    // gs:0 (or fs:0 on X86-64) contains its own address.
    // For more information see http://people.redhat.com/drepper/tls.pdf
    if (address.getNode() instanceof ConstantSDNode) {
      ConstantSDNode c = (ConstantSDNode) address.getNode();
      if (c.getSExtValue() == 0 && am.segment.getNode() == null &&
          subtarget.isTargetELF()) {
        Util.shouldNotReachHere("Don't support address space!");
        return false;
      }
    }

    return true;
  }

  private boolean matchWrapper(SDValue val, X86ISelAddressMode am) {
    if (am.hasSymbolicDisplacement())
      return true;
    SDValue n0 = val.getOperand(0);
    CodeModel model = tm.getCodeModel();
    if (subtarget.is64Bit() &&
        (model == CodeModel.Small || model == CodeModel.Kernel) &&
        !am.hasBaseOrIndexReg() && val.getOpcode() == X86ISD.WrapperRIP) {
      if (n0.getNode() instanceof GlobalAddressSDNode) {
        GlobalAddressSDNode gv = (GlobalAddressSDNode) n0.getNode();
        long offset = am.disp + gv.getOffset();
        if (!X86.isOffsetSuitableForCodeModel(offset, model)) return true;
        am.gv = gv.getGlobalValue();
        am.disp = (int) offset;
        am.symbolFlags = gv.getTargetFlags();
      } else if (n0.getNode() instanceof ConstantPoolSDNode) {
        ConstantPoolSDNode cp = (ConstantPoolSDNode) n0.getNode();
        long offset = am.disp + cp.getOffset();
        if (!X86.isOffsetSuitableForCodeModel(offset, model)) return true;
        am.cp = cp.getConstantValue();
        am.align = cp.getAlignment();
        am.disp = (int) offset;
        am.symbolFlags = cp.getTargetFlags();
      } else if (n0.getNode() instanceof ExternalSymbolSDNode) {
        ExternalSymbolSDNode es = (ExternalSymbolSDNode) n0.getNode();
        am.externalSym = es.getExtSymol();
        am.symbolFlags = es.getTargetFlags();
      } else {
        JumpTableSDNode j = (JumpTableSDNode) n0.getNode();
        am.jti = j.getJumpTableIndex();
        am.symbolFlags = j.getJumpTableIndex();
      }

      if (val.getOpcode() == X86ISD.WrapperRIP)
        am.setBaseReg(curDAG.getRegister(X86GenRegisterNames.RIP, new EVT(MVT.i64)));
      return false;
    }
    if (!subtarget.is64Bit() || ((model == CodeModel.Small || model == CodeModel.Kernel)
        && tm.getRelocationModel() == TargetMachine.RelocModel.Static)) {
      if (n0.getNode() instanceof GlobalAddressSDNode) {
        GlobalAddressSDNode gv = ((GlobalAddressSDNode) n0.getNode());
        am.gv = gv.getGlobalValue();
        am.disp += gv.getOffset();
        am.symbolFlags = gv.getTargetFlags();
      } else if (n0.getNode() instanceof ConstantPoolSDNode) {
        ConstantPoolSDNode cp = (ConstantPoolSDNode) n0.getNode();
        am.cp = cp.getConstantValue();
        am.align = cp.getAlignment();
        am.disp += cp.getOffset();
        am.symbolFlags = cp.getTargetFlags();
      } else if (n0.getNode() instanceof ExternalSymbolSDNode) {
        ExternalSymbolSDNode es = (ExternalSymbolSDNode) n0.getNode();
        am.externalSym = es.getExtSymol();
        am.symbolFlags = es.getTargetFlags();
      } else {
        JumpTableSDNode j = (JumpTableSDNode) n0.getNode();
        am.jti = j.getJumpTableIndex();
        am.symbolFlags = j.getTargetFlags();
      }
      return false;
    }
    return true;
  }

  private boolean matchAddress(SDValue val, X86ISelAddressMode am) {
    OutRef<X86ISelAddressMode> amRef = new OutRef<>(am);
    boolean res = matchAddressRecursively(val, amRef, 0);
    am.setValuesFrom(amRef.get());
    if (res) return true;

    if (am.scale == 2 && am.baseType == X86ISelAddressMode.BaseType.RegBase &&
        am.base.reg.getNode() == null) {
      am.base.reg = am.indexReg;
      am.scale = 1;
    }

    if (tm.getCodeModel() == CodeModel.Small &&
        subtarget.is64Bit() &&
        am.scale == 1 &&
        am.baseType == X86ISelAddressMode.BaseType.RegBase &&
        am.base.reg.getNode() == null &&
        am.indexReg.getNode() == null &&
        am.symbolFlags == 0 &&
        am.hasSymbolicDisplacement())
      am.base.reg = curDAG.getRegister(X86GenRegisterNames.RIP, new EVT(MVT.i64));

    return false;
  }

  private static boolean isDispSafeFroFrameIndex(long val) {
    // On 64-bit platforms, we can run into an issue where a frame index
    // includes a displacement that, when added to the explicit displacement,
    // will overflow the displacement field. Assuming that the frame index
    // displacement fits into a 31-bit integer  (which is only slightly more
    // aggressive than the current fundamental assumption that it fits into
    // a 32-bit integer), a 31-bit disp should always be safe.
    return Util.isInt32(val);
  }

  private boolean foldOffsetIntoAddress(long offset,
                                        X86ISelAddressMode am) {
    long val = am.disp + offset;
    CodeModel cm = tm.getCodeModel();
    if (subtarget.is64Bit()) {
      if (!X86.isOffsetSuitableForCodeModel(val, cm, am.hasSymbolicDisplacement()))
        return true;

      // In addition to the checks required for a register base, check that
      // we do not try to use an unsafe Disp with a frame index.
      if (am.baseType == FrameIndexBase && !isDispSafeFroFrameIndex(val))
        return true;
    }
    am.disp = (int) val;
    return false;
  }

  private boolean matchAddressRecursively(SDValue n,
                                          OutRef<X86ISelAddressMode> am,
                                          int depth) {
    boolean is64Bit = subtarget.is64Bit();
    if (depth > 5)
      return matchAddressBase(n, am.get());

    CodeModel m = tm.getCodeModel();
    if (am.get().isRIPRelative()) {
      if (am.get().externalSym == null && am.get().jti != -1)
        return true;

      if (n.getNode() instanceof ConstantSDNode) {
        ConstantSDNode cn = (ConstantSDNode) n.getNode();
        long val = am.get().disp + cn.getSExtValue();
        if (X86.isOffsetSuitableForCodeModel(val, m, am.get().hasSymbolicDisplacement())) {
          am.get().disp = (int) val;
          return false;
        }
      }
      return true;
    }

    switch (n.getOpcode()) {
      default:
        break;
      case ISD.Constant: {
        long val = ((ConstantSDNode) n.getNode()).getSExtValue();
        if (!foldOffsetIntoAddress(val, am.get()))
          return false;
        break;
      }
      case X86ISD.Wrapper:
      case X86ISD.WrapperRIP: {
        if (!matchWrapper(n, am.get()))
          return false;
        break;
      }
      case ISD.LOAD:
        if (!matchLoadInAddress((LoadSDNode) n.getNode(), am.get()))
          return false;
        break;
      case ISD.FrameIndex:
        if (am.get().baseType == RegBase && am.get().base.reg.getNode() == null) {
          am.get().baseType = FrameIndexBase;
          am.get().base.frameIndex = ((FrameIndexSDNode) n.getNode()).getFrameIndex();
          return false;
        }
        break;
      case ISD.SHL:
        if (am.get().indexReg.getNode() != null || am.get().scale == 1)
          break;
        if (n.getOperand(1).getNode() instanceof ConstantSDNode) {
          ConstantSDNode csd = (ConstantSDNode) n.getOperand(1).getNode();
          long val = csd.getZExtValue();
          if (val == 1 || val == 2 || val == 3) {
            am.get().scale = 1 << val;
            SDValue shVal = n.getOperand(0);
            if (shVal.getOpcode() == ISD.ADD && shVal.hasOneUse() &&
                shVal.getOperand(1).getNode() instanceof ConstantSDNode) {
              am.get().indexReg = shVal.getOperand(0);
              ConstantSDNode addVal = (ConstantSDNode) shVal.getOperand(1).getNode();
              long disp = am.get().disp + (addVal.getSExtValue() << val);
              if (!is64Bit || X86.isOffsetSuitableForCodeModel(disp,
                  m, am.get().hasSymbolicDisplacement()))
                am.get().disp = Math.toIntExact(disp);
              else {
                am.get().indexReg = shVal;
              }
            } else {
              am.get().indexReg = shVal;
            }
            return false;
          }
          break;
        }
        // Fall through
      case ISD.SMUL_LOHI:
      case ISD.UMUL_LOHI:
        if (n.getResNo() != 0) break;
        // Fall through
      case ISD.MUL:
      case X86ISD.MUL_IMM:
        // X*[3,5,9] --> X+X*[2,4,8]
        if (am.get().baseType == RegBase &&
            am.get().base.reg.getNode() == null &&
            am.get().indexReg.getNode() == null) {
          if (n.getOperand(1).getNode() instanceof ConstantSDNode) {
            ConstantSDNode csd = (ConstantSDNode) n.getOperand(1).getNode();
            long val = csd.getZExtValue();
            if (val == 3 || val == 5 || val == 9) {
              am.get().scale = (int) (val - 1);
              SDValue mulVal = n.getOperand(0);
              SDValue reg;
              if (mulVal.getOpcode() == ISD.ADD && mulVal.hasOneUse() &&
                  mulVal.getOperand(1).getNode() instanceof ConstantSDNode) {
                reg = mulVal.getOperand(0);
                ConstantSDNode addVal = (ConstantSDNode) mulVal.getOperand(1).getNode();
                long disp = am.get().disp + addVal.getSExtValue() * val;

                if (!is64Bit || X86.isOffsetSuitableForCodeModel(disp,
                    m, am.get().hasSymbolicDisplacement()))
                  am.get().disp = Math.toIntExact(disp);
                else
                  reg = n.getOperand(0);
              } else {
                reg = n.getOperand(0);
              }

              am.get().indexReg = am.get().base.reg = reg;
              return false;
            }
          }
        }
        break;
      case ISD.SUB: {
        // Given A-B, if A can be completely folded into the address and
        // the index field with the index field unused, use -B as the index.
        // This is a win if a has multiple parts that can be folded into
        // the address. Also, this saves a mov if the base register has
        // other uses, since it avoids a two-address sub instruction, however
        // it costs an additional mov if the index register has other uses.

        // Test if the LHS of the sub can be folded.
        X86ISelAddressMode backup = am.get().clone();
        if (matchAddressRecursively(n.getOperand(0), am, depth + 1)) {
          am.set(backup);
          break;
        }
        if (am.get().indexReg.getNode() != null || am.get().isRIPRelative()) {
          am.set(backup);
          break;
        }
        int cost = 0;
        SDValue rhs = n.getOperand(1);
        int rhsOpc = rhs.getOpcode();
        if (!rhs.hasOneUse() ||
            rhsOpc == ISD.CopyFromReg ||
            rhsOpc == ISD.TRUNCATE ||
            rhsOpc == ISD.ANY_EXTEND ||
            (rhsOpc == ISD.ZERO_EXTEND &&
                rhs.getOperand(0).getValueType().equals(new EVT(MVT.i32))))
          ++cost;

        if ((am.get().baseType == RegBase && am.get().base.reg.getNode() != null &&
            !am.get().base.reg.getNode().hasOneUse()) ||
            am.get().baseType == FrameIndexBase) {
          --cost;
        }
        boolean b1 = (am.get().hasSymbolicDisplacement() && !backup.hasSymbolicDisplacement());
        boolean b2 = am.get().disp != 0 && backup.disp == 0;
        boolean b3 = am.get().segment.getNode() != null && backup.segment.getNode() == null;
        if ((b1 ? 1 : 0) + (b2 ? 1 : 0) + (b3 ? 1 : 0) >= 2) {
          --cost;
        }

        if (cost >= 0) {
          am.set(backup);
          break;
        }

        SDValue zero = curDAG.getConstant(0, n.getValueType(), false);
        SDValue neg = curDAG.getNode(ISD.SUB, n.getValueType(), zero, rhs);
        am.get().indexReg = neg;
        am.get().scale = 1;

        // FIXME, we have to comment the following code for two reasons
        // 1. We can't reposition any node of a DAG because it will invalidate the iterating index on selecting instruction.
        // 2. it is unnecessary to keep the topological order.
        // 8/22/2019.
        /*
        if (zero.getNode().getNodeID() == -1 ||
            zero.getNode().getNodeID() > n.getNode().getNodeID()) {
          curDAG.repositionNode(n.getNode(), zero.getNode());
          zero.getNode().setNodeID(n.getNode().getNodeID());
        }
        if (neg.getNode().getNodeID() == -1 ||
            neg.getNode().getNodeID() > n.getNode().getNodeID()) {
          curDAG.repositionNode(n.getNode(), neg.getNode());
          neg.getNode().setNodeID(n.getNode().getNodeID());
        }*/
        return false;
      }
      case ISD.ADD: {
        X86ISelAddressMode backup = am.get().clone();
        if (!matchAddressRecursively(n.getOperand(0), am, depth + 1) &&
            !matchAddressRecursively(n.getOperand(1), am, depth + 1)) {
          return false;
        }

        am.set(backup.clone());
        if (!matchAddressRecursively(n.getOperand(1), am, depth + 1) &&
            !matchAddressRecursively(n.getOperand(0), am, depth + 1))
          return false;

        am.set(backup.clone());

        if (am.get().baseType == RegBase &&
            am.get().base.reg.getNode() == null &&
            am.get().indexReg.getNode() == null) {
          am.get().base.reg = n.getOperand(0);
          am.get().indexReg = n.getOperand(1);
          am.get().scale = 1;
          return false;
        }
        break;
      }
      case ISD.OR: {
        if (n.getOperand(1).getNode() instanceof ConstantSDNode) {
          ConstantSDNode csd = (ConstantSDNode) n.getOperand(1).getNode();
          X86ISelAddressMode backup = am.get().clone();
          long offset = csd.getSExtValue();
          if (!matchAddressRecursively(n.getOperand(0), am, depth + 1) &&
              am.get().gv == null &&
              (!is64Bit || X86.isOffsetSuitableForCodeModel(
                  am.get().disp + offset, m, am.get().hasSymbolicDisplacement())) &&
              curDAG.maskedValueIsZero(n.getOperand(0), csd.getAPIntValue())) {
            am.get().disp += offset;
            return false;
          }
          am.set(backup);
        }
        break;
      }
      case ISD.AND: {
        SDValue shift = n.getOperand(0);
        if (shift.getNumOperands() != 2) break;

        if (am.get().indexReg.getNode() != null || am.get().scale != 1) break;
        SDValue x = shift.getOperand(0);
        if (!(n.getOperand(1).getNode() instanceof ConstantSDNode) ||
            !(shift.getOperand(1).getNode() instanceof ConstantSDNode))
          break;

        ConstantSDNode c2 = (ConstantSDNode) n.getOperand(1).getNode();
        ConstantSDNode c1 = (ConstantSDNode) shift.getOperand(1).getNode();
        // Handle "(X >> (8-C1)) & C2" as "(X >> 8) & 0xff)" if safe. This
        // allows us to convert the shift and and into an h-register extract and
        // a scaled index.
        if (shift.getOpcode() == ISD.SRL && shift.hasOneUse()) {
          long scaleLog = 8 - c1.getZExtValue();
          if (scaleLog > 0 && scaleLog < 4 && c2.getZExtValue() ==
              (0xffL << scaleLog)) {
            SDValue eight = curDAG.getConstant(8, new EVT(MVT.i8), false);
            SDValue mask = curDAG.getConstant(0xff, n.getValueType(), false);
            SDValue srl = curDAG.getNode(ISD.SRL, n.getValueType(), x, eight);
            SDValue and = curDAG.getNode(ISD.AND, n.getValueType(), srl, mask);
            SDValue shlCount = curDAG.getConstant(scaleLog, new EVT(MVT.i8), false);
            SDValue shl = curDAG.getNode(ISD.SHL, n.getValueType(), and, shlCount);

            // FIXME, we have to comment the following code for two reasons
            // 1. We can't reposition any node of a DAG because it will invalidate the iterating index on selecting instruction.
            // 2. it is unnecessary to keep the topological order.
            // 8/22/2019.
            /*if (eight.getNode().getNodeID() == -1 ||
                eight.getNode().getNodeID() > x.getNode().getNodeID()) {
              curDAG.repositionNode(x.getNode(), eight.getNode());
              eight.getNode().setNodeID(x.getNode().getNodeID());
            }
            if (mask.getNode().getNodeID() == -1 ||
                mask.getNode().getNodeID() > x.getNode().getNodeID()) {
              curDAG.repositionNode(x.getNode(), mask.getNode());
              mask.getNode().setNodeID(x.getNode().getNodeID());
            }
            if (srl.getNode().getNodeID() == -1 ||
                srl.getNode().getNodeID() > shift.getNode().getNodeID()) {
              curDAG.repositionNode(shift.getNode(), srl.getNode());
              srl.getNode().setNodeID(shift.getNode().getNodeID());
            }
            if (and.getNode().getNodeID() == -1 ||
                and.getNode().getNodeID() > n.getNode().getNodeID()) {
              curDAG.repositionNode(n.getNode(), and.getNode());
              and.getNode().setNodeID(n.getNode().getNodeID());
            }
            if (shlCount.getNode().getNodeID() == -1 ||
                shlCount.getNode().getNodeID() > x.getNode().getNodeID()) {
              curDAG.repositionNode(x.getNode(), shlCount.getNode());
              shlCount.getNode().setNodeID(x.getNode().getNodeID());
            }
            if (shl.getNode().getNodeID() == -1 ||
                shl.getNode().getNodeID() > n.getNode().getNodeID()) {
              curDAG.repositionNode(n.getNode(), shl.getNode());
              shl.getNode().setNodeID(n.getNode().getNodeID());
            }*/
            curDAG.replaceAllUsesWith(n, shl, null);
            am.get().indexReg = and;
            am.get().scale = 1 << scaleLog;
            return false;
          }
        }

        if (shift.getOpcode() != ISD.SHL)
          break;
        if (!n.hasOneUse() || !shift.hasOneUse())
          break;
        long shiftCst = c1.getZExtValue();
        if (shiftCst != 1 && shiftCst != 2 && shiftCst != 3)
          break;

        SDValue newAndMask = curDAG.getNode(ISD.SRL, n.getValueType(),
            new SDValue(c2, 0), new SDValue(c1, 0));
        SDValue newAnd = curDAG.getNode(ISD.AND, n.getValueType(), x,
            newAndMask);
        SDValue newShift = curDAG.getNode(ISD.SHL, n.getValueType(),
            newAnd, new SDValue(c1, 0));

        // FIXME, we have to comment the following code for two reasons
        // 1. We can't reposition any node of a DAG because it will invalidate the iterating index on selecting instruction.
        // 2. it is unnecessary to keep the topological order.
        // 8/22/2019.
        /*if (c1.getNodeID() > x.getNode().getNodeID()) {
          curDAG.repositionNode(x.getNode(), c1);
          c1.setNodeID(x.getNode().getNodeID());
        }
        if (newAndMask.getNode().getNodeID() == -1 ||
            newAndMask.getNode().getNodeID() > x.getNode().getNodeID()) {
          curDAG.repositionNode(x.getNode(), newAndMask.getNode());
          newAndMask.getNode().setNodeID(x.getNode().getNodeID());
        }
        if (newAnd.getNode().getNodeID() == -1 ||
            newAnd.getNode().getNodeID() > shift.getNode().getNodeID()) {
          curDAG.repositionNode(shift.getNode(), newAnd.getNode());
          newAnd.getNode().setNodeID(shift.getNode().getNodeID());
        }
        if (newShift.getNode().getNodeID() == -1 ||
            newShift.getNode().getNodeID() > n.getNode().getNodeID()) {
          curDAG.repositionNode(n.getNode(), newShift.getNode());
          newShift.getNode().setNodeID(n.getNode().getNodeID());
        }*/
        curDAG.replaceAllUsesWith(n, newShift, null);
        am.get().scale = 1 << shiftCst;
        am.get().indexReg = newAnd;
        return false;
      }
    }
    return matchAddressBase(n, am.get());
  }

  protected boolean matchAddressBase(SDValue n, X86ISelAddressMode am) {
    if (am.baseType != X86ISelAddressMode.BaseType.RegBase ||
        am.base.reg.getNode() != null) {
      if (am.indexReg.getNode() == null) {
        am.indexReg = n;
        am.scale = 1;
        return false;
      }

      return true;
    }

    am.baseType = X86ISelAddressMode.BaseType.RegBase;
    am.base.reg = n;
    return false;
  }

  /**
   * comp indicates the base, scale, index, disp and segment for X86 Address mode.
   *
   * @param op
   * @param n
   * @param comp
   * @return
   */
  protected boolean selectAddr(SDNode op, SDValue n, SDValue[] comp) {
    X86ISelAddressMode am = new X86ISelAddressMode();
    boolean done = false;
    if (!n.hasOneUse()) {
      int opc = n.getOpcode();
      if (opc != ISD.Constant && opc != ISD.FrameIndex &&
          opc != X86ISD.Wrapper && opc != X86ISD.WrapperRIP) {
        for (SDUse u : n.getNode().getUseList()) {
          if (u.getNode().getOpcode() == ISD.CopyToReg) {
            matchAddressBase(n, am);
            done = true;
            break;
          }
        }
      }
    }

    if (!done && matchAddress(n, am))
      return false;

    EVT vt = n.getValueType();
    if (am.baseType == X86ISelAddressMode.BaseType.RegBase) {
      if (am.base.reg.getNode() == null)
        am.base.reg = curDAG.getRegister(0, vt);
    }
    if (am.indexReg.getNode() == null)
      am.indexReg = curDAG.getRegister(0, vt);
    getAddressOperands(am, comp);
    return true;
  }

  protected boolean selectLEAAddr(SDNode root, SDValue n, SDValue[] comp) {
    Util.assertion(comp.length == 4);
    X86ISelAddressMode am = new X86ISelAddressMode();
    SDValue backup = am.segment.clone();
    SDValue t = curDAG.getRegister(0, new EVT(MVT.i32));
    am.segment = t;
    if (matchAddress(n, am))
      return false;
    Util.assertion(t.equals(am.segment));
    am.segment = backup;

    EVT vt = n.getValueType();
    int complexity = 0;
    if (am.baseType == X86ISelAddressMode.BaseType.RegBase) {
      if (am.base.reg.getNode() != null)
        complexity = 1;
      else
        am.base.reg = curDAG.getRegister(0, vt);
    } else if (am.baseType == FrameIndexBase)
      complexity = 4;

    if (am.indexReg.getNode() != null)
      ++complexity;
    else
      am.indexReg = curDAG.getRegister(0, vt);

    if (am.scale > 1)
      ++complexity;
    if (am.hasSymbolicDisplacement()) {
      if (subtarget.is64Bit())
        complexity = 4;
      else
        complexity += 2;
    }

    if (am.disp != 0 && (am.base.reg.getNode() != null || am.indexReg.getNode() != null))
      ++complexity;

    if (complexity <= 2)
      return false;

    SDValue[] temp = new SDValue[5];
    temp[4] = new SDValue();    // segment.
    getAddressOperands(am, temp);
    System.arraycopy(temp, 0, comp, 0, 4);
    return true;
  }

  protected boolean selectTLSADDRAddr(SDNode op, SDValue val, SDValue[] comp) {
    Util.assertion(comp.length == 4);
    Util.assertion(val.getOpcode() == ISD.TargetGlobalTLSAddress);
    GlobalAddressSDNode ga = (GlobalAddressSDNode) val.getNode();
    X86ISelAddressMode am = new X86ISelAddressMode();
    am.gv = ga.getGlobalValue();
    am.disp += ga.getOffset();
    am.base.reg = curDAG.getRegister(0, val.getValueType());
    am.symbolFlags = ga.getTargetFlags();
    if (val.getValueType().getSimpleVT().simpleVT == MVT.i32) {
      am.scale = 1;
      am.indexReg = curDAG.getRegister(X86GenRegisterNames.EBX, new EVT(MVT.i32));
    } else {
      am.indexReg = curDAG.getRegister(0, new EVT(MVT.i64));
    }
    SDValue[] temp = new SDValue[5];
    System.arraycopy(comp, 0, temp, 0, 4);
    temp[4] = new SDValue();    // segment.
    getAddressOperands(am, temp);
    return true;
  }

  protected boolean selectScalarSSELoad(SDNode root, SDValue n, SDValue[] comp) {
    // comp[0]  -- base
    // comp[1]  -- scale
    // comp[2]  -- index
    // comp[3]  -- disp
    // comp[4]  -- segment
    // comp[5]  -- inChain
    // comp[6]  -- outChain

    Util.assertion(comp.length == 7);
    if (n.getOpcode() == ISD.SCALAR_TO_VECTOR) {
      comp[5] = n.getOperand(0).getValue(1);
      if (comp[5].getNode().isNONExtLoad() &&
          comp[5].getValue(0).hasOneUse() &&
          n.hasOneUse() &&
          isLegalAndProfitableToFold(n.getOperand(0), n.getNode(), root)) {
        LoadSDNode ld = (LoadSDNode) comp[5].getNode();
        if (!selectAddr(root, ld.getBasePtr(), comp))
          return false;
        comp[6] = ld.getChain();
        return true;
      }
    }

    if (n.getOpcode() == X86ISD.VZEXT_MOVL && n.getNode().hasOneUse() &&
        n.getOperand(0).getOpcode() == ISD.SCALAR_TO_VECTOR &&
        n.getOperand(0).getNode().hasOneUse() &&
        n.getOperand(0).getOperand(0).getNode().isNONExtLoad() &&
        n.getOperand(0).getOperand(0).hasOneUse()) {
      LoadSDNode ld = (LoadSDNode) n.getOperand(0).getOperand(0).getNode();
      if (!selectAddr(root, ld.getBasePtr(), comp))
        return false;
      comp[6] = ld.getChain();
      comp[5] = new SDValue(ld, 1);
      return true;
    }
    return false;
  }

  protected boolean tryFoldLoad(SDNode pred, SDValue node, SDValue[] comp) {
    Util.assertion(comp.length == 5);
    if (node.getNode().isNONExtLoad() &&
        node.getNode().hasOneUse() &&
        isLegalAndProfitableToFold(node, pred, pred))
      return selectAddr(pred, node.getOperand(1), comp);

    return false;
  }

  private void preprocessForRMW() {
    OutRef<SDValue> x = new OutRef<>();
    for (SDNode node : curDAG.allNodes) {
      if (node.getOpcode() == X86ISD.CALL) {
        /// Also try moving call address load from outside callseq_start to just
        /// before the call to allow it to be folded.
        ///
        ///     [Load chain]
        ///         ^
        ///         |
        ///       [Load]
        ///       ^    ^
        ///       |    |
        ///      /      \--
        ///     /          |
        ///[CALLSEQ_START] |
        ///     ^          |
        ///     |          |
        /// [LOAD/C2Reg]   |
        ///     |          |
        ///      \        /
        ///       \      /
        ///       [CALL]
        SDValue chain = node.getOperand(0);
        SDValue load = node.getOperand(1);
        x.set(chain);
        boolean res = !isCalleeLoad(load, x);
        chain = x.get();
        if (res)
          continue;
        moveBelowCallSeqStart(curDAG, load, new SDValue(node, 0), chain);
        continue;
      }

      if (!node.isNONTRUNCStore())
        continue;
      SDValue chain = node.getOperand(0);

      if (chain.getNode().getOpcode() != ISD.TokenFactor)
        continue;

      SDValue n1 = node.getOperand(1);
      SDValue n2 = node.getOperand(2);
      if ((n1.getValueType().isFloatingPoint() &&
          !n1.getValueType().isVector()) ||
          !n1.hasOneUse())
        continue;

      boolean rmodW = false;
      SDValue load = new SDValue();
      int opc = n1.getNode().getOpcode();
      switch (opc) {
        case ISD.ADD:
        case ISD.MUL:
        case ISD.AND:
        case ISD.OR:
        case ISD.XOR:
        case ISD.ADDC:
        case ISD.ADDE:
        case ISD.VECTOR_SHUFFLE: {
          SDValue n10 = n1.getOperand(0);
          SDValue n11 = n1.getOperand(1);
          x.set(load);
          rmodW = isRMWLoad(n10, chain, n2, x);
          load = x.get();
          if (!rmodW) {
            x.set(load);
            rmodW = isRMWLoad(n11, chain, n2, x);
            load = x.get();
          }
          break;
        }
        case ISD.SUB:
        case ISD.SHL:
        case ISD.SRA:
        case ISD.SRL:
        case ISD.ROTL:
        case ISD.ROTR:
        case ISD.SUBC:
        case ISD.SUBE:
        case X86ISD.SHLD:
        case X86ISD.SHRD: {
          SDValue n10 = n1.getOperand(0);
          x.set(load);
          rmodW = isRMWLoad(n10, chain, n2, x);
          load = x.get();
          break;
        }
      }
      if (rmodW) {
        moveBelowTokenFactor(curDAG, load, new SDValue(node, 0), chain);
      }
    }
  }

  static boolean isRMWLoad(SDValue n, SDValue chain, SDValue address,
                           OutRef<SDValue> load) {
    if (n.getOpcode() == ISD.BIT_CONVERT)
      n = n.getOperand(0);

    LoadSDNode ld = n.getNode() instanceof LoadSDNode ? (LoadSDNode) n.getNode() : null;
    if (ld == null || ld.isVolatile())
      return false;
    if (ld.getAddressingMode() != UNINDEXED)
      return false;

    LoadExtType extType = ld.getExtensionType();
    if (extType != NON_EXTLOAD && extType != EXTLOAD)
      return false;

    if (n.hasOneUse() &&
        n.getOperand(1).equals(address) &&
        n.getNode().isOperandOf(chain.getNode())) {
      load.set(n);
      return true;
    }
    return false;
  }

  static void moveBelowTokenFactor(SelectionDAG curDAG, SDValue load,
                                   SDValue store, SDValue tf) {
    ArrayList<SDValue> ops = new ArrayList<>();
    for (int i = 0, e = tf.getNode().getNumOperands(); i < e; i++) {
      if (load.getNode().equals(tf.getOperand(i).getNode()))
        ops.add(load.getOperand(0));
      else
        ops.add(tf.getOperand(i));
    }
    SDValue newTF = curDAG.updateNodeOperands(tf, ops);
    SDValue newLoad = curDAG.updateNodeOperands(load, newTF, load.getOperand(1),
        load.getOperand(2));
    curDAG.updateNodeOperands(store, newLoad.getValue(1), store.getOperand(1),
        store.getOperand(2), store.getOperand(3));
  }

  static boolean isCalleeLoad(SDValue callee, OutRef<SDValue> chain) {
    if (callee.getNode().equals(chain.get().getNode()) || !callee.hasOneUse())
      return false;
    LoadSDNode ld = callee.getNode() instanceof LoadSDNode ? (LoadSDNode) callee.getNode() : null;
    if (ld == null || ld.isVolatile() || ld.getAddressingMode() != UNINDEXED
        || ld.getExtensionType() != NON_EXTLOAD)
      return false;

    while (chain.get().getOpcode() != ISD.CALLSEQ_START) {
      if (chain.get().hasOneUse())
        return false;

      chain.set(chain.get().getOperand(0));
    }
    if (chain.get().getOperand(0).getNode().equals(callee.getNode()))
      return true;
    if (chain.get().getOperand(0).getOpcode() == ISD.TokenFactor &&
        callee.getValue(1).isOperandOf(chain.get().getOperand(0).getNode()) &&
        callee.getValue(1).hasOneUse())
      return true;

    return false;
  }

  static void moveBelowCallSeqStart(SelectionDAG curDAG, SDValue load,
                                    SDValue call, SDValue callSeqStart) {
    ArrayList<SDValue> ops = new ArrayList<>();
    SDValue chain = callSeqStart.getOperand(0);
    if (chain.getNode().equals(load.getNode()))
      ops.add(load.getOperand(0));
    else {
      Util.assertion(chain.getOpcode() == ISD.TokenFactor);
      for (int i = 0, e = chain.getNumOperands(); i < e; i++) {
        if (chain.getOperand(i).getNode().equals(load.getNode()))
          ops.add(load.getOperand(0));
        else
          ops.add(chain.getOperand(i));
      }

      SDValue newChain = curDAG.getNode(ISD.TokenFactor, new EVT(MVT.Other),
          ops);
      ops.clear();
      ops.add(newChain);
    }

    for (int i = 1, e = callSeqStart.getNumOperands(); i < e; i++)
      ops.add(callSeqStart.getOperand(i));
    curDAG.updateNodeOperands(callSeqStart, ops);
    curDAG.updateNodeOperands(load, call.getOperand(0),
        load.getOperand(1), load.getOperand(2));
    ops.clear();
    ops.add(new SDValue(load.getNode(), 1));
    for (int i = 1, e = call.getNode().getNumOperands(); i < e; i++)
      ops.add(call.getOperand(i));
    curDAG.updateNodeOperands(call, ops);
  }

  /**
   * Lower the FP_EXTEND and FP_ROUND operation to store-load.
   */
  private void preprocessForFPConvert() {
    for (int i = 0; i < curDAG.allNodes.size(); i++) {
      SDNode node = curDAG.allNodes.get(i);
      if (node.getOpcode() != ISD.FP_ROUND && node.getOpcode() != ISD.FP_EXTEND)
        continue;

      EVT srcVT = node.getOperand(0).getValueType();
      EVT dstVT = node.getValueType(0);
      boolean srcIsSSE = tli.isScalarFPTypeInSSEReg(srcVT);
      boolean dstIsSSE = tli.isScalarFPTypeInSSEReg(dstVT);
      if (srcIsSSE && dstIsSSE)
        continue;

      if (!srcIsSSE && !dstIsSSE) {
        // If this is an FPStack extension, it is a noop.
        if (node.getOpcode() == ISD.FP_EXTEND)
          continue;
        if (node.getConstantOperandVal(1) != 0)
          continue;
      }

      EVT memVT;
      if (node.getOpcode() == ISD.FP_ROUND)
        memVT = dstVT;
      else
        memVT = srcIsSSE ? srcVT : dstVT;

      SDValue memTmp = curDAG.createStackTemporary(memVT);
      SDValue store = curDAG.getTruncStore(curDAG.getEntryNode(), node.getOperand(0),
          memTmp, null, 0, memVT);
      SDValue result = curDAG.getExtLoad(EXTLOAD, dstVT, store, memTmp,
          null, 0, memVT);

      curDAG.replaceAllUsesOfValueWith(new SDValue(node, 0), result);
      curDAG.deleteNode(node);

      // delete the node from allNodes list.
      curDAG.allNodes.remove(i);
      --i;
    }
  }

  protected void emitSpecialCodeForMain(MachineBasicBlock mbb, MachineFrameInfo mfi) {
    TargetInstrInfo tii = tm.getInstrInfo();
    if (subtarget.isTargetCygMing()) {
      buildMI(mbb, new DebugLoc(), tii.get(X86GenInstrNames.CALLpcrel32)).addExternalSymbol("__main");
    }
  }

  protected void getAddressOperands(X86ISelAddressMode am, SDValue[] comp) {
    // comp[0] -- base
    // comp[1] -- scale
    // comp[2] -- index
    // comp[3] -- disp
    // comp[4] -- segment
    Util.assertion(comp.length == 5);
    comp[0] = am.baseType == FrameIndexBase ?
        curDAG.getTargetFrameIndex(am.base.frameIndex, new EVT(tli.getPointerTy())) :
        am.base.reg;
    comp[1] = getI8Imm(am.scale);
    comp[2] = am.indexReg;

    if (am.gv != null)
      comp[3] = curDAG.getTargetGlobalAddress(am.gv, new EVT(MVT.i32), am.disp,
          am.symbolFlags);
    else if (am.cp != null)
      comp[3] = curDAG.getTargetConstantPool(am.cp, new EVT(MVT.i32),
          am.align, am.disp, am.symbolFlags);
    else if (am.externalSym != null)
      comp[3] = curDAG.getTargetExternalSymbol(am.externalSym, new EVT(MVT.i32), am.symbolFlags);
    else if (am.jti != -1)
      comp[3] = curDAG.getTargetJumpTable(am.jti, new EVT(MVT.i32), am.symbolFlags);
    else
      comp[3] = curDAG.getTargetConstant(am.disp, new EVT(MVT.i32));

    if (am.segment.getNode() != null)
      comp[4] = am.segment;
    else
      comp[4] = curDAG.getRegister(0, new EVT(MVT.i32));
  }

  protected SDValue getI8Imm(int imm) {
    return curDAG.getTargetConstant(imm, new EVT(MVT.i8));
  }

  protected SDValue getI16Imm(int imm) {
    return curDAG.getTargetConstant(imm, new EVT(MVT.i16));
  }

  protected SDValue getI32Imm(int imm) {
    return curDAG.getTargetConstant(imm, new EVT(MVT.i32));
  }

  protected SDNode getGlobalBaseReg() {
    int baseReg = subtarget.getInstrInfo().getGlobalBaseReg(mf);
    return curDAG.getRegister(baseReg, new EVT(tli.getPointerTy())).getNode();
  }

  public X86TargetMachine getTargetMachine() {
    return (X86TargetMachine) super.getTargetMachine();
  }

  public SDNode select_DECLARE(SDValue n) {
    SDValue chain = n.getOperand(0);
    SDValue n1 = n.getOperand(1);
    SDValue n2 = n.getOperand(2);
    if (!(n1.getNode() instanceof FrameIndexSDNode)
        || !(n2.getNode() instanceof GlobalAddressSDNode)) {
      cannotYetSelect(n.getNode());
    }
    EVT pty = new EVT(tli.getPointerTy());
    int fi = ((FrameIndexSDNode) n1.getNode()).getFrameIndex();
    GlobalValue gv = ((GlobalAddressSDNode) n2.getNode()).getGlobalValue();
    SDValue tmp1 = curDAG.getTargetFrameIndex(fi, pty);
    SDValue tmp2 = curDAG.getTargetGlobalAddress(gv, pty, 0, 0);
    return curDAG.selectNodeTo(n.getNode(),
        X86GenInstrNames.DECLARE,
        new EVT(MVT.Other), tmp1, tmp2, chain);
  }

  @Override
  public boolean selectInlineAsmMemoryOperands(SDValue op,
                                               char constraintCode,
                                               ArrayList<SDValue> outOps) {
    SDValue[] comm = new SDValue[5];
    switch (constraintCode) {
      case 'm':   // memory
        if (!selectAddr(op.getNode(), op, comm))
          return true;
        break;
      case 'o':   // offsettable
      case 'v':   // not offsettable
      default:
        return true;
    }
    outOps.addAll(Arrays.asList(comm));
    return false;
  }
}
