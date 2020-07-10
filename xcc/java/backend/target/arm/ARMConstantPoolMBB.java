package backend.target.arm;
/*
 * Extremely Compiler Collection
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

import backend.codegen.MachineBasicBlock;
import backend.codegen.MachineConstantPool;
import backend.codegen.MachineConstantPoolEntry;
import backend.support.LLVMContext;
import tools.FoldingSetNodeID;

import java.io.PrintStream;
import java.util.ArrayList;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class ARMConstantPoolMBB extends ARMConstantPoolValue {
  private MachineBasicBlock mbb;

  private ARMConstantPoolMBB(LLVMContext ctx, MachineBasicBlock mbb,
                             int id, int pcAdj, ARMCP.ARMCPModifier modifier,
                             boolean addCurrentAddress) {
    super(ctx, id, ARMCP.ARMCPKind.CPMachineBasicBlock, pcAdj, modifier, addCurrentAddress);
    this.mbb = mbb;
  }

  public static ARMConstantPoolMBB create(LLVMContext ctx,
                                          MachineBasicBlock mbb,
                                          int id, int pcAdj) {
    return new ARMConstantPoolMBB(ctx, mbb, id, pcAdj, ARMCP.ARMCPModifier.no_modifier, false);
  }

  public MachineBasicBlock getMBB() { return mbb;}

  @Override
  public int getExistingMachineCPValue(MachineConstantPool pool, int align) {
    int alignMask = align - 1;
    ArrayList<MachineConstantPoolEntry> constants = pool.getConstants();
    for (int i = 0, e = constants.size(); i != e; ++i) {
      if (constants.get(i).isMachineConstantPoolEntry() &&
          (constants.get(i).getAlignment() & alignMask) == 0) {
        ARMConstantPoolValue cpv = (ARMConstantPoolValue) constants.get(i).getValueAsCPV();
        if (!(cpv instanceof ARMConstantPoolMBB)) continue;

        ARMConstantPoolMBB apmMBB = (ARMConstantPoolMBB) cpv;
        if (apmMBB.mbb == mbb && equals(apmMBB))
          return i;
      }
    }
    return -1;
  }

  @Override
  public void addSelectionDAGCSEId(FoldingSetNodeID id) {
    id.addInteger(mbb.hashCode());
    super.addSelectionDAGCSEId(id);
  }

  @Override
  public boolean hasSameValue(ARMConstantPoolValue val) {
    return val instanceof ARMConstantPoolMBB && ((ARMConstantPoolMBB) val).mbb == mbb &&
        super.hasSameValue(val);
  }

  @Override
  public void print(PrintStream os) {
    super.print(os);
  }
}
