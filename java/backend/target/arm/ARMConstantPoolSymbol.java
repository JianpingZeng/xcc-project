package backend.target.arm;
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

import backend.codegen.MachineConstantPool;
import backend.codegen.MachineConstantPoolEntry;
import backend.support.LLVMContext;
import backend.type.Type;
import tools.FoldingSetNodeID;

import java.io.PrintStream;
import java.util.ArrayList;

/**
 * For external symbol.
 * @author Jianping Zeng.
 * @version 0.4
 */
public class ARMConstantPoolSymbol extends ARMConstantPoolValue {
  private String symbol;

  private ARMConstantPoolSymbol(LLVMContext ctx, String s,
                                int id, int pcAdj, ARMCP.ARMCPModifier modifier,
                                boolean addCurrentAddress) {
    super(Type.getInt32Ty(ctx), id, ARMCP.ARMCPKind.CPExtSymbol, pcAdj, modifier, addCurrentAddress);
    symbol = s;
  }

  public String getSymbol() { return symbol; }

  public static ARMConstantPoolSymbol create(LLVMContext ctx, String sym,
                                             int armPCLabelIndex, int pcAdj) {
    return new ARMConstantPoolSymbol(ctx, sym, armPCLabelIndex, pcAdj, ARMCP.ARMCPModifier.no_modifier, false);
  }

  @Override
  public int getExistingMachineCPValue(MachineConstantPool pool, int align) {
    int alignMask = align - 1;
    ArrayList<MachineConstantPoolEntry> constants = pool.getConstants();
    for (int i = 0, e = constants.size(); i < e; ++i) {
      if (constants.get(i).isMachineConstantPoolEntry() &&
          (constants.get(i).getAlignment() & alignMask) == 0) {
        ARMConstantPoolValue cpv = (ARMConstantPoolValue) constants.get(i).val;
        if (!(cpv instanceof ARMConstantPoolSymbol))
          continue;
        ARMConstantPoolSymbol apc = (ARMConstantPoolSymbol) cpv;
        if (apc.symbol.equals(symbol) && equals(apc))
          return i;
      }
    }
    return -1;
  }

  @Override
  public void addSelectionDAGCSEId(FoldingSetNodeID id) {
    id.addString(symbol);
    super.addSelectionDAGCSEId(id);
  }

  @Override
  public boolean hasSameValue(ARMConstantPoolValue val) {
    if (val instanceof ARMConstantPoolSymbol) {
      ARMConstantPoolSymbol apv = (ARMConstantPoolSymbol) val;
      return apv.symbol.equals(symbol) && super.hasSameValue(apv);
    }
    return false;
  }

  @Override
  public void print(PrintStream os) {
    os.print(symbol);
    super.print(os);
  }
}
