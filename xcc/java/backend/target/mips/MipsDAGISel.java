package backend.target.mips;
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

import backend.codegen.EVT;
import backend.codegen.MVT;
import backend.codegen.dagisel.ISD;
import backend.codegen.dagisel.SDNode;
import backend.codegen.dagisel.SDNode.ConstantFPSDNode;
import backend.codegen.dagisel.SDNode.ConstantPoolSDNode;
import backend.codegen.dagisel.SDNode.FrameIndexSDNode;
import backend.codegen.dagisel.SDNode.GlobalAddressSDNode;
import backend.codegen.dagisel.SDValue;
import backend.codegen.dagisel.SelectionDAGISel;
import backend.debug.DebugLoc;
import backend.pass.FunctionPass;
import backend.target.TargetMachine;
import tools.Util;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class MipsDAGISel extends SelectionDAGISel {
  protected MipsSubtarget subtarget;

  protected MipsDAGISel(MipsTargetMachine tm, TargetMachine.CodeGenOpt optLevel) {
    super(tm, optLevel);
    subtarget = tm.getSubtarget();
  }

  public static FunctionPass createMipsDAGISel(MipsTargetMachine tm,
                                               TargetMachine.CodeGenOpt optLevel) {
    return new MipsGenDAGISel(tm, optLevel);
  }

  @Override
  public String getPassName() {
    return "MIPS DAG->DAG Pattern Instruction Selection";
  }

  @Override
  public SDNode select(SDNode node) {
    int opc = node.getOpcode();
    DebugLoc dl = node.getDebugLoc();

    // If we are going to select a machine node, just skip it.
    if (node.isMachineOpecode())
      return null;

    // Select some selection dag ops which are not generated tblegen.
    switch (opc) {
      default: break;
      case ISD.SUBE:
      case ISD.ADDE: {
        SDValue inFlag = node.getOperand(2), cmpLHS = new SDValue();
        int inFlagOPc = inFlag.getNode().getOpcode();
        Util.assertion(inFlagOPc == ISD.ADDC || inFlagOPc == ISD.ADDE ||
            inFlagOPc == ISD.SUBC || inFlagOPc == ISD.SUBE,
            "(ADD|SUB)E flag operand must come from (ADD|SUB)C/E insn");

        int mOp;
        if (opc == ISD.ADDE) {
          cmpLHS = inFlag.getValue(0);
          mOp = MipsGenInstrNames.ADDu;
        }
        else {
          cmpLHS = inFlag.getOperand(0);
          mOp = MipsGenInstrNames.SUBu;
        }

        SDValue[] ops = {cmpLHS, inFlag.getOperand(1)};
        SDValue lhs = node.getOperand(0), rhs = node.getOperand(1);

        EVT vt = lhs.getValueType();
        SDNode carry = curDAG.getMachineNode(MipsGenInstrNames.SLTu, dl, vt, ops);
        SDNode addCarry = curDAG.getMachineNode(MipsGenInstrNames.ADDu, dl, vt,
            new SDValue(carry, 0), rhs);
        return curDAG.selectNodeTo(node, mOp, vt, new EVT(MVT.Glue),
            lhs, new SDValue(addCarry, 0));
      }
      case ISD.SMUL_LOHI:
      case ISD.UMUL_LOHI: {
        // Mul with two results.
        Util.assertion(!node.getValueType(0).equals(new EVT(MVT.i64)),
            "64-bit multiplication with two results not handled!");
        SDValue op1 = node.getOperand(0), op2 = node.getOperand(1);

        int op = opc == ISD.UMUL_LOHI ? MipsGenInstrNames.MULTu : MipsGenInstrNames.MULT;
        SDNode mul = curDAG.getMachineNode(op, dl, new EVT(MVT.Glue), op1, op2);
        SDValue inFlag = new SDValue(mul, 0);
        SDNode lo = curDAG.getMachineNode(MipsGenInstrNames.MFLO, dl, new EVT(MVT.i32), new EVT(MVT.Glue), inFlag);
        inFlag = new SDValue(lo, 1);
        SDNode hi = curDAG.getMachineNode(MipsGenInstrNames.MFHI, dl, new EVT(MVT.i32), new EVT(MVT.Glue), inFlag);

        if (!new SDValue(node, 0).isUseEmpty()) {
          replaceUses(new SDValue(node, 0), new SDValue(lo, 0));
        }

        if (!new SDValue(node, 1).isUseEmpty()) {
          replaceUses(new SDValue(node, 1), new SDValue(hi, 0));
        }

        return null;
      }
      /// Special Muls
      case ISD.MUL: {
        // Mips32 has a 32-bit three operand mul instruction.
        if (subtarget.hasMips32() && node.getValueType(0).equals(new EVT(MVT.i32)))
          break;
      }

      case ISD.MULHS:
      case ISD.MULHU: {
        Util.assertion(opc == ISD.MUL || !node.getValueType(0).equals(new EVT(MVT.i64)),
            "64-bit MULH not handled!");
        EVT ty = node.getValueType(0);
        SDValue op0 = node.getOperand(0), op1 = node.getOperand(1);

        int mulOp = opc == ISD.MULHU ? MipsGenInstrNames.MULTu :
            (ty.equals(new EVT(MVT.i32)) ? MipsGenInstrNames.MULT : MipsGenInstrNames.DMULT);
        SDNode mulNode = curDAG.getMachineNode(mulOp, dl, new EVT(MVT.Glue), op0, op1);
        SDValue inFlag = new SDValue(mulNode, 0);
        if (opc == ISD.MUL) {
          int op = ty.equals(new EVT(MVT.i32)) ? MipsGenInstrNames.MFLO : MipsGenInstrNames.MFLO64;
          return curDAG.getMachineNode(op, dl, ty, inFlag);
        }
        else
          return curDAG.getMachineNode(MipsGenInstrNames.MFHI, dl, new EVT(MVT.i32), inFlag);
      }

      case ISD.GLOBAL_OFFSET_TABLE:
        return getGlobalBaseReg();

      case ISD.ConstantFP: {
        ConstantFPSDNode sdn = (ConstantFPSDNode) node;
        if (node.getValueType(0).equals(new EVT(MVT.f64)) && sdn.isExactlyValue(+0.0)) {
          SDValue zero = curDAG.getCopyFromReg(curDAG.getEntryNode(), dl, MipsGenRegisterNames.ZERO, new EVT(MVT.i32));
          return curDAG.getMachineNode(MipsGenInstrNames.BuildPairF64, dl, new EVT(MVT.f64), zero, zero);
        }
        break;
      }
      case MipsISD.ThreadPointer: {
        int srcReg = MipsGenRegisterNames.HWR29;
        int dstReg = MipsGenRegisterNames.V1;
        SDNode rdhwr = curDAG.getMachineNode(MipsGenInstrNames.RDHWR, dl,
            node.getValueType(0), curDAG.getRegister(srcReg, new EVT(MVT.i32)));
        SDValue chain = curDAG.getCopyToReg(curDAG.getEntryNode(), dl, dstReg,
            new SDValue(rdhwr, 0));
        SDValue resNode = curDAG.getCopyFromReg(chain, dl, dstReg, new EVT(MVT.i32));
        replaceUses(new SDValue(node, 0), resNode);
        return resNode.getNode();
      }
    }

    // Select the default instruction.
    return selectCommonCode(node);
  }

  private SDNode getGlobalBaseReg() {
    int globalBaseReg = subtarget.getInstrInfo().getGlobalBaseReg(mf);
    return curDAG.getRegister(globalBaseReg, new EVT(tli.getPointerTy())).getNode();
  }

  protected SDValue getI32Imm(int val) {
    return curDAG.getTargetConstant(val, new EVT(MVT.i32));
  }

  /**
   * This function is used to read the address expression and determine what kinds of
   * address mode could be suitable for it. The computed operand of address expression
   * is returned in the {@arg tmp} array.
   * @param addr
   * @param tmp
   * @return
   */
  protected boolean selectAddr(SDValue addr, SDValue[] tmp) {
    EVT valTy = addr.getValueType();
    int gpreg = valTy.equals(new EVT(MVT.i32)) ? MipsGenRegisterNames.GP : MipsGenRegisterNames.GP_64;

    // if Address is FI, get the TargetFrameIndex.
    if (addr.getNode() instanceof FrameIndexSDNode) {
      FrameIndexSDNode fin = (FrameIndexSDNode) addr.getNode();
      tmp[0] = curDAG.getTargetFrameIndex(fin.getFrameIndex(), valTy);
      tmp[1] = curDAG.getTargetConstant(0, valTy);
      return true;
    }

    // on PIC code Load GA
    if (tm.getRelocationModel() == TargetMachine.RelocModel.PIC_) {
      if (addr.getOpcode() == MipsISD.WrapperPIC) {
        tmp[0] = curDAG.getRegister(gpreg, valTy);
        tmp[1] = addr.getOperand(0);
        return true;
      }
    }
    else {
      if (addr.getOpcode() == ISD.TargetExternalSymbol ||
          addr.getOpcode() == ISD.TargetGlobalAddress) {
        return false;
      }
      else if (addr.getOpcode() == ISD.TargetGlobalTLSAddress) {
        tmp[0] = curDAG.getRegister(gpreg, valTy);
        tmp[1] = addr;
        return true;
      }
    }

    // Addresses of the form FI+const or FI|const
    if (curDAG.isBaseWithConstantOffset(addr)) {
      SDNode.ConstantSDNode cn = (SDNode.ConstantSDNode) addr.getOperand(1).getNode();
      if (Util.isInt16(cn.getSExtValue())) {
        if (addr.getOperand(0).getNode() instanceof FrameIndexSDNode) {
          FrameIndexSDNode fin = (FrameIndexSDNode) addr.getOperand(0).getNode();
          tmp[0] = curDAG.getTargetFrameIndex(fin.getFrameIndex(), valTy);
        }
        else
          tmp[0] = addr.getOperand(0);

        tmp[1] = curDAG.getTargetConstant(cn.getZExtValue(), valTy);
        return true;
      }
    }

    // Operand is a result from an ADD.
    if (addr.getOpcode() == ISD.ADD) {
      // When loading from constant pools, load the lower address part in
      // the instruction itself. Example, instead of:
      //  lui $2, %hi($CPI1_0)
      //  addiu $2, $2, %lo($CPI1_0)
      //  lwc1 $f0, 0($2)
      // Generate:
      //  lui $2, %hi($CPI1_0)
      //  lwc1 $f0, %lo($CPI1_0)($2)
      if ((addr.getOperand(0).getOpcode() == MipsISD.Hi ||
          addr.getOperand(0).getOpcode() == ISD.LOAD) &&
          addr.getOperand(1).getOpcode() == MipsISD.Lo) {
        SDValue loVal = addr.getOperand(1);
        if ((loVal.getOperand(0).getNode() instanceof ConstantPoolSDNode) ||
            (loVal.getOperand(0).getNode() instanceof GlobalAddressSDNode)) {
          tmp[0] = addr.getOperand(0);
          tmp[1] = addr.getOperand(1);
          return true;
        }
      }
    }

    tmp[0] = addr;
    tmp[1] = curDAG.getTargetConstant(0, valTy);
    return true;
  }
}
