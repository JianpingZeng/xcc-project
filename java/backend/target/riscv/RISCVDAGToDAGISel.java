package backend.target.riscv;
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

import backend.codegen.dagisel.SDNode;
import backend.codegen.dagisel.SDNode.ConstantSDNode;
import backend.codegen.dagisel.SDValue;
import backend.codegen.dagisel.SelectionDAGISel;
import backend.target.TargetMachine;

import static tools.Util.assertion;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class RISCVDAGToDAGISel extends SelectionDAGISel {
  protected RISCVSubtarget subtarget;

  public RISCVDAGToDAGISel(RISCVTargetMachine tm, TargetMachine.CodeGenOpt optLevel) {
    super(tm, optLevel);
    subtarget = tm.getSubtarget();
  }

  @Override
  public void instructionSelect() {

  }
  public SDNode selectCode(SDValue n) {return null;}

  public boolean isOrEquivalentToAdd(SDNode n) { return false; }

  /**
   * Checks if specified val, a signed integer, has number of bits.
   *
   * @param csd
   * @param bits
   * @return
   */
  protected static boolean isInt(ConstantSDNode csd, int bits) {
    assertion(bits >= 1);
    long val = csd.getSExtValue();
    return bits >= 64 || (-(1 << (bits - 1)) <= val &&
        val <= (1 << (bits - 1)));
  }

  protected static boolean isInt8(ConstantSDNode val) {
    return isInt(val, 8);
  }

  protected static boolean isInt16(ConstantSDNode val) {
    return isInt(val, 16);
  }

  protected static boolean isInt32(ConstantSDNode val) {
    return isInt(val, 32);
  }

  protected static boolean isUInt(ConstantSDNode c, int bits) {
    assertion(bits >= 1);
    long val = c.getZExtValue();
    return Long.compareUnsigned(val, 0) >= 0 &&
        Long.compareUnsigned(val, (1 << bits) - 1) <= 0;
  }

  protected static boolean isUInt8(ConstantSDNode val) {
    return isUInt(val, 8);
  }

  protected static boolean isUInt16(ConstantSDNode val) {
    return isUInt(val, 16);
  }

  protected static boolean isUInt32(ConstantSDNode val) {
    return isUInt(val, 32);
  }

  protected static boolean isShiftedInt(ConstantSDNode c, int n, int s) {
    assertion(n > 0, "isShiftedInt with s = 0 doesn't make sense");
    assertion(s + n <= 64 && s >= 0 && n >= 0);
    long val = c.getSExtValue();
    return isInt(c, n + s) && (val % (1 << s)) == 0;
  }

  protected static long signExtend64(long val, int n) {
    assertion(n >= 0 && n <= 64);
    return (n << (64 - n)) >> (64 - n);
  }

  protected static int signExtend32(int val, int n) {
    assertion(n >= 0 && n <= 64);
    return (n << (32 - n)) >> (32 - n);
  }

  protected boolean selectAddrFI(SDValue root, SDValue op0, SDValue[] res) {
    return false;
  }
  public void replaceUses(SDValue[] froms, SDValue[] tos) {

  }
  public void replaceUses(SDValue from, SDValue to) {

  }
}
