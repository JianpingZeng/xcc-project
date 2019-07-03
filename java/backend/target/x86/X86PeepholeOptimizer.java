package backend.target.x86;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng
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

import backend.codegen.MachineBasicBlock;
import backend.codegen.MachineFunction;
import backend.codegen.MachineInstr;
import backend.pass.RegisterPass;
import backend.support.MachineFunctionPass;
import backend.target.TargetInstrInfo;
import tools.Util;

import static backend.codegen.MachineInstrBuilder.buildMI;
import static backend.target.x86.X86GenInstrNames.*;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class X86PeepholeOptimizer extends MachineFunctionPass {
  @Override
  public String getPassName() {
    return "X86 Peephole optimization pass";
  }

  /**
   * This method must be overridded by concrete subclass for performing
   * desired machine code transformation or analysis.
   *
   * @param mf
   * @return
   */
  @Override
  public boolean runOnMachineFunction(MachineFunction mf) {
    boolean changed = false;
    tii = mf.getTarget().getInstrInfo();
    for (MachineBasicBlock mbb : mf.getBasicBlocks()) {
      for (int i = 0; i < mbb.size(); ) {
        i = optimizeInst(mbb, i);
        changed |= res;
      }
    }
    return changed;
  }

  private boolean res = false;
  private TargetInstrInfo tii;

  private int optimizeInst(MachineBasicBlock mbb, int idx) {
    MachineInstr curMI = mbb.getInstAt(idx);
    MachineInstr next = idx == mbb.size() - 1 ? null : mbb.getInstAt(idx + 1);
    switch (curMI.getOpcode()) {
      case MOV8rr:
      case MOV16rr:
      case MOV32rr:
        // destroy X=X copy.
        if (curMI.getOperand(0).getReg()
            == curMI.getOperand(1).getReg()) {
          mbb.erase(idx);
          res = true;
          return idx;
        }
        res = false;
        return idx + 1;
      case ADD16ri:
      case ADD32ri:
      case SUB16ri:
      case SUB32ri:
      case IMUL16rri:
      case IMUL32rri:
      case AND16ri:
      case AND32ri:
      case OR16ri:
      case OR32ri:
      case XOR16ri:
      case XOR32ri:
        Util.assertion(curMI.getNumOperands() == 3, "There should have 3 opernds!");
        if (curMI.getOperand(2).isImm()) {
          long val = curMI.getOperand(2).getImm();
          // If the value is the same when signed extended from 8 bits
          if (val == (byte) val) {
            int opcode;
            switch (curMI.getOpcode()) {
              default:
                Util.assertion(false, "Undefined opcode value!");
              case ADD16ri:
                opcode = ADD16ri8;
                break;
              case ADD32ri:
                opcode = ADD32ri8;
                break;
              case SUB16ri:
                opcode = SUB16ri8;
                break;
              case SUB32ri:
                opcode = SUB32ri8;
                break;
              case IMUL16rri:
                opcode = IMUL16rri8;
                break;
              case IMUL32rri:
                opcode = IMUL32rri8;
                break;
              case AND16ri:
                opcode = AND16ri8;
                break;
              case AND32ri:
                opcode = AND32ri8;
                break;
              case OR16ri:
                opcode = OR16ri8;
                break;
              case OR32ri:
                opcode = OR32ri8;
                break;
              case XOR16ri:
                opcode = XOR32ri8;
                break;
              case XOR32ri:
                opcode = XOR32ri8;
                break;
            }

            int r0 = curMI.getOperand(0).getReg();
            int r1 = curMI.getOperand(1).getReg();
            mbb.replace(idx, buildMI(tii.get(opcode), r0)
                .addReg(r1)
                .addImm(val).getMInstr());
            res = true;
            return idx + 1;
          }
        }
        res = false;
        return idx + 1;
      case BSWAP32r: {
        // Change bswap EAX, bswap EAX into nothing.
        if (next.getOpcode() == BSWAP32r
            && curMI.getOperand(0).getReg() ==
            next.getOperand(0).getReg()) {
          mbb.erase(idx);
          res = true;
          return idx;
        }
        res = false;
        return idx + 1;
      }
      default:
        res = false;
        return idx + 1;
    }
  }

  public static X86PeepholeOptimizer createX86PeepholeOptimizer() {
    return new X86PeepholeOptimizer();
  }

  /**
   * Register X86 peephole optimization pass.
   */
  static {
    new RegisterPass("peephole", "X86 peephole optimizer",
        X86PeepholeOptimizer.class);
  }
}
