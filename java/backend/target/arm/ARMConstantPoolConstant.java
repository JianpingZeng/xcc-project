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
import backend.type.Type;
import backend.value.BlockAddress;
import backend.value.Constant;
import backend.value.GlobalValue;
import tools.FoldingSetNodeID;

import java.io.PrintStream;
import java.util.ArrayList;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class ARMConstantPoolConstant extends ARMConstantPoolValue {
  private Constant cval;
  private ARMConstantPoolConstant(Constant c,
                                  Type ty, int id, ARMCP.ARMCPKind kind,
                                  int pcAdj, ARMCP.ARMCPModifier modifier,
                                  boolean addCurrentAddress) {
    super(ty, id, kind, pcAdj, modifier, addCurrentAddress);
    cval = c;
  }

  private ARMConstantPoolConstant(Constant c,
                                  int id, ARMCP.ARMCPKind kind,
                                  int pcAdj, ARMCP.ARMCPModifier modifier,
                                  boolean addCurrentAddress) {
    super(c.getType(), id, kind, pcAdj, modifier, addCurrentAddress);
    cval = c;
  }

  public static ARMConstantPoolConstant create(Constant c, int id) {
    return new ARMConstantPoolConstant(c, id, ARMCP.ARMCPKind.CPValue, 0, ARMCP.ARMCPModifier.no_modifier, false);
  }
  public static ARMConstantPoolConstant create(GlobalValue gv, ARMCP.ARMCPModifier modifier) {
    return new ARMConstantPoolConstant(gv, Type.getInt32Ty(gv.getContext()), 0, ARMCP.ARMCPKind.CPValue, 0, modifier, false);
  }
  public static ARMConstantPoolConstant create(Constant c, int id, ARMCP.ARMCPKind kind, int pcAddr) {
    return new ARMConstantPoolConstant(c, id, kind, pcAddr, ARMCP.ARMCPModifier.no_modifier, false);
  }
  public static ARMConstantPoolConstant create(Constant c, int id, ARMCP.ARMCPKind kind, int cpAddr,
                                               ARMCP.ARMCPModifier modifier, boolean addCurrentAddress) {
    return new ARMConstantPoolConstant(c, id, kind, cpAddr, modifier, addCurrentAddress);
  }

  public GlobalValue getGlobalValue() { return (GlobalValue) cval;}
  public BlockAddress getBlockAddress() { return (BlockAddress) cval;};

  @Override
  public int getExistingMachineCPValue(MachineConstantPool pool, int align) {
    int alignMask = align - 1;
    ArrayList<MachineConstantPoolEntry> constants = pool.getConstants();
    for (int i = 0, e = constants.size(); i < e; ++i) {
      if (constants.get(i).isMachineConstantPoolEntry() &&
          (constants.get(i).getAlignment() & alignMask) == 0) {
        ARMConstantPoolValue cpv = (ARMConstantPoolValue) constants.get(i).val;
        if (!(cpv instanceof ARMConstantPoolConstant))
          continue;
        ARMConstantPoolConstant apc = (ARMConstantPoolConstant) cpv;
        if (apc.cval.equals(cval) && equals(apc))
          return i;
      }
    }
    return -1;
  }

  @Override
  public boolean hasSameValue(ARMConstantPoolValue val) {
    if (val instanceof ARMConstantPoolConstant) {
      ARMConstantPoolConstant apv = (ARMConstantPoolConstant) val;
      return apv.cval.equals(cval) && super.hasSameValue(apv);
    }
    return false;
  }

  @Override
  public void addSelectionDAGCSEId(FoldingSetNodeID id) {
    id.addInteger(cval.hashCode());
    super.addSelectionDAGCSEId(id);
  }

  @Override
  public void print(PrintStream os) {
    os.print(cval.getName());
    super.print(os);
  }
}
