/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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

import backend.codegen.EVT;
import backend.codegen.MachineBasicBlock;
import backend.codegen.MachineFunction;
import backend.codegen.MachineRegisterInfo;
import backend.support.LLVMContext;
import backend.target.TargetData;
import backend.target.TargetInstrInfo;
import backend.target.TargetLowering;
import backend.type.ArrayType;
import backend.type.StructType;
import backend.type.Type;
import backend.value.*;
import backend.value.Instruction.AllocaInst;
import backend.value.Instruction.PhiNode;
import backend.value.Instruction.SwitchInst;
import gnu.trove.list.array.TLongArrayList;
import tools.APInt;
import tools.Util;

import java.util.ArrayList;
import java.util.HashMap;

import static backend.codegen.MachineInstrBuilder.buildMI;

/**
 * This contains information that is global to a function that is used when
 * lowering a region of the function.
 *
 * @author Jianping Zeng
 * @version 0.1
 */
public class FunctionLoweringInfo {
  public static class LiveOutInfo {
    public int numSignBits;
    public APInt knownOne, knownZero;

    public LiveOutInfo() {
      numSignBits = 0;
    }
  }


  public TargetLowering tli;
  public Function fn;
  public MachineFunction mf;
  public MachineRegisterInfo mri;

  /**
   * A mapping from LLVM basic block to their machine code entry.
   */
  public HashMap<BasicBlock, MachineBasicBlock> mbbmap;
  /**
   * Since we emit code for the function a basic block at a time, we must
   * remember which virtual registers hold the values for cross-basic-block
   * values.
   */
  public HashMap<Value, Integer> valueMap;

  /**
   * Keep track of frame indices for fixed sized allocas in the entry block.
   * This allows allocas to be efficiently referenced anywhere in the function.
   */
  public HashMap<AllocaInst, Integer> staticAllocaMap;

  /**
   * Information about live out vregs, indexed by their register number offset
   * by 'FirstVirtualRegister'.
   */
  public ArrayList<LiveOutInfo> liveOutRegInfo;

  public FunctionLoweringInfo(TargetLowering tli) {
    this.tli = tli;
    mbbmap = new HashMap<>();
    valueMap = new HashMap<>();

    staticAllocaMap = new HashMap<>();
    liveOutRegInfo = new ArrayList<>();
  }

  /**
   * Determines if the specified instruction is used by PHI node or outside
   * the basic block that defines it, or used by a switch instruction, which
   * may extend to multiples basic block.
   *
   * @param inst
   * @return
   */
  private static boolean isUsedOutsideOfDefiningBlock(Instruction inst) {
    if (inst instanceof PhiNode) return true;

    if (inst.isUseEmpty())
      return false;

    BasicBlock definingBB = inst.getParent();
    for (Use u : inst.getUseList()) {
      User user = u.getUser();
      if (user instanceof PhiNode || user instanceof SwitchInst)
        return true;
      if (user instanceof Instruction && ((Instruction) user)
          .getParent() != definingBB)
        return true;
    }
    return false;
  }

  /**
   * Checks whether the specified Argument is only used in entry block.
   *
   * @param arg
   * @return Return true if it is.
   */
  private static boolean isOnlyUsedInEntryBlock(Argument arg) {
    if (arg.isUseEmpty())
      return true;

    BasicBlock entryBB = arg.getParent().getEntryBlock();
    for (Use u : arg.getUseList()) {
      User user = u.getUser();
      if ((user instanceof Instruction &&
          ((Instruction) user).getParent() != entryBB) || user instanceof SwitchInst)
        return false;   // Not only used in entry block.
    }
    return true;
  }

  /**
   * Initiliaze this FunctionLoweringInfo with the given Function and its
   * associated MachineFunction.
   */
  public void set(Function fn, MachineFunction mf) {
    this.fn = fn;
    this.mf = mf;
    mri = mf.getMachineRegisterInfo();

    // Create virtual register for each argument that is not dead and is used
    // outside of the entry block.
    for (Argument arg : fn.getArgumentList()) {
      if (!isOnlyUsedInEntryBlock(arg))
        initializeRegForValue(arg);
    }

    // Initialize the mapping of values to registers.  This is only set up for
    // instruction values that are used outside of the block that defines
    // them.
    for (Instruction inst : fn.getEntryBlock()) {
      if (inst instanceof AllocaInst) {
        AllocaInst ai = (AllocaInst) inst;
        ConstantInt size = ai.getArraySize() instanceof ConstantInt
            ? (ConstantInt) ai.getArraySize() : null;
        if (size != null) {
          // Allocate an array with constant size.
          Type eltTy = ai.getAllocatedType();
          long tySize = tli.getTargetData().getTypeAllocSize(eltTy);

          int align = Math.max(tli.getTargetData().getPrefTypeAlignment(eltTy), ai.getAlignment());
          tySize *= size.getZExtValue();
          if (tySize == 0)
            tySize = 1;
          // Create a stack object for static sized array.
          staticAllocaMap.put(ai, mf.getFrameInfo().createStackObject(tySize, align));
        }
      }
    }

    for (BasicBlock bb : fn.getBasicBlockList()) {
      for (Instruction inst : bb) {
        if (isUsedOutsideOfDefiningBlock(inst)) {
          if (!(inst instanceof AllocaInst)
              || !staticAllocaMap.containsKey(inst))
            initializeRegForValue(inst);
        }
      }
    }

    // Create an initial MachineBasicBlock for each LLVM BasicBlock in F.  This
    // also creates the initial PHI MachineInstrs, though none of the input
    // operands are populated.
    TargetInstrInfo tii = tli.getTargetMachine().getInstrInfo();
    for (BasicBlock bb : fn.getBasicBlockList()) {
      MachineBasicBlock mbb = mf.createMachineBasicBlock(bb);
      mbbmap.put(bb, mbb);
      mf.addMBBNumbering(mbb);

      PhiNode pn = null;
      for (Instruction inst : bb) {
        if (!(inst instanceof PhiNode) || inst.isUseEmpty())
          continue;
        pn = (PhiNode) inst;
        Util.assertion(valueMap.containsKey(pn), "PhiNode must be assigned with a virtual register!");
        int vreg = valueMap.get(pn);
        ArrayList<EVT> vts = new ArrayList<>();
        computeValueVTs(tli, pn.getType(), vts);
        for (EVT vt : vts) {
          int num = tli.getNumRegisters(vt);
          for (int i = 0; i < num; i++)
            buildMI(mbb, tii.get(TargetInstrInfo.PHI), vreg + i);
          vreg += num;
        }
      }
    }
  }

  public int makeReg(EVT vt) {
    return mri.createVirtualRegister(tli.getRegClassFor(vt));
  }

  public boolean isExportedInst(Value v) {
    return valueMap.containsKey(v);
  }

  /**
   * Given an LLVM IR type, compute a sequence of
   * EVTs that represent all the individual underlying
   * non-aggregate types that comprise it.
   * <p>
   * If Offsets is non-null, it points to a vector to be filled in
   * with the in-memory offsets of each of the individual values.
   *
   * @param tli
   * @param ty
   * @param valueVTs
   * @param offsets
   * @param startingOffset
   */
  static void computeValueVTs(TargetLowering tli, Type ty, ArrayList<EVT> valueVTs,
                              TLongArrayList offsets, long startingOffset) {
    if (ty instanceof StructType) {
      StructType st = (StructType) ty;
      TargetData.StructLayout layout = tli.getTargetData().getStructLayout(st);
      for (int i = 0, e = st.getNumOfElements(); i < e; i++) {
        computeValueVTs(tli, st.getElementType(i), valueVTs, offsets,
            startingOffset + layout.getElementOffset(i));
        ;
      }
      return;
    }
    if (ty instanceof ArrayType) {
      ArrayType at = (ArrayType) ty;
      Type eltTy = at.getElementType();
      long eltSize = tli.getTargetData().getTypeAllocSize(eltTy);
      for (long i = 0, e = at.getNumElements(); i < e; i++) {
        computeValueVTs(tli, eltTy, valueVTs, offsets, startingOffset + i * eltSize);
        ;
      }
      return;
    }
    if (ty.equals(LLVMContext.VoidTy))
      return;
    // Non-aggragate type
    valueVTs.add(tli.getValueType(ty));
    if (offsets != null)
      offsets.add(startingOffset);
  }

  public static void computeValueVTs(TargetLowering tli, Type ty, ArrayList<EVT> valueVTs,
                                     TLongArrayList offset) {
    computeValueVTs(tli, ty, valueVTs, offset, 0);
  }

  public static void computeValueVTs(TargetLowering tli, Type ty, ArrayList<EVT> valueVTs) {
    computeValueVTs(tli, ty, valueVTs, null, 0);
  }

  public int createRegForValue(Value v) {
    ArrayList<EVT> valueVTs = new ArrayList<>();
    computeValueVTs(tli, v.getType(), valueVTs);

    int firstReg = 0;
    for (EVT valueVT : valueVTs) {
      EVT registerVT = tli.getRegisterType(valueVT);

      int numRegs = tli.getNumRegisters(valueVT);
      for (; numRegs != 0; --numRegs) {
        int r = makeReg(registerVT);
        if (firstReg == 0)
          firstReg = r;
      }
    }
    return firstReg;
  }

  public int initializeRegForValue(Value v) {
    Util.assertion(!valueMap.containsKey(v), "Already initialized this value register!");
    int r = createRegForValue(v);
    valueMap.put(v, r);
    return r;
  }

  /**
   * Clear out all the function-specific state. This returns this FunctionLoweringInfo
   * to an empty state, ready to be used for a different function.
   */
  public void clear() {
    mbbmap.clear();
    valueMap.clear();
    staticAllocaMap.clear();
    liveOutRegInfo.clear();
  }
}
