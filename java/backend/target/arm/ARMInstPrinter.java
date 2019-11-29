package backend.target.arm;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
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

import backend.mc.*;
import tools.OutRef;
import tools.Util;

import java.io.PrintStream;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public abstract class ARMInstPrinter extends MCInstPrinter {
  public ARMInstPrinter(PrintStream os, MCAsmInfo mai) {
    super(os, mai);
  }

  public static MCInstPrinter createARMInstPrinter(PrintStream os, MCAsmInfo mai) {
    return new ARMGenInstPrinter(os, mai);
  }

  protected void printOperand(MCInst mi, int opIdx) {
    MCOperand op = mi.getOperand(opIdx);
    if (op.isReg())
      os.printf("%s", getRegisterName(op.getReg()));
    else if (op.isImm()) {
      os.printf("#%d", op.getImm());
      if (commentStream != null && (op.getImm() > 255 || op.getImm() < -256))
        commentStream.printf("imm = 0x%%%x\n", op.getImm());
    } else {
      Util.assertion(op.isExpr(), "unknown operand kind in printOperand");
      MCConstantExpr branchTarget = op.getExpr() instanceof MCConstantExpr ?
          (MCConstantExpr) op.getExpr() : null;
      OutRef<Long> address = new OutRef<>(0L);
      if (branchTarget != null && branchTarget.evaluateAsAbsolute(address)) {
        os.printf("0x%x", address.get());
      }
      else {
        op.getExpr().print(os);
      }
    }
  }

  protected void printMemRegImm(MCInst mi, int opIdx) {
    printOperand(mi, opIdx+1);
    os.print(',');
    printOperand(mi, opIdx);
  }
}
