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

package backend.codegen.dagisel;

import backend.codegen.EVT;
import backend.codegen.MachineBasicBlock;
import backend.codegen.MachineFunction;
import backend.codegen.MachineRegisterInfo;
import backend.target.*;
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
import java.util.HashSet;

import static backend.codegen.MachineInstrBuilder.buildMI;

/**
 * This contains information that is global to a function that is used when
 * lowering a region of the function.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public class FunctionLoweringInfo {
  public static class LiveOutInfo {
    public int numSignBits;
    public boolean isValid;
    public APInt knownOne, knownZero;

    public LiveOutInfo() {
      numSignBits = 0;
      isValid = true;
      knownOne = new APInt(1, 0);
      knownZero = new APInt(1, 0);
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
  public HashMap<Integer, LiveOutInfo> liveOutRegInfo;

  /**
   * The set of basic blocks visited thus used for instruction selection.
   */
  public HashSet<BasicBlock> visitedBBs;

  public FunctionLoweringInfo(TargetLowering tli) {
    this.tli = tli;
    mbbmap = new HashMap<>();
    valueMap = new HashMap<>();

    staticAllocaMap = new HashMap<>();
    liveOutRegInfo = new HashMap<>();
    visitedBBs = new HashSet<>();
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

    // create virtual register for each argument that is not dead and is used
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
          // create a stack object for static sized array.
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

    // create an initial MachineBasicBlock for each LLVM BasicBlock in F.  This
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
            buildMI(mbb, tii.get(TargetOpcodes.PHI), vreg + i);
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
      }
      return;
    }
    if (ty instanceof ArrayType) {
      ArrayType at = (ArrayType) ty;
      Type eltTy = at.getElementType();
      long eltSize = tli.getTargetData().getTypeAllocSize(eltTy);
      for (long i = 0, e = at.getNumElements(); i < e; i++) {
        computeValueVTs(tli, eltTy, valueVTs, offsets, startingOffset + i * eltSize);
      }
      return;
    }
    if (ty.isVoidType())
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
      EVT registerVT = tli.getRegisterType(v.getContext(), valueVT);

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

  /**
   * Gets LiveOutInfo for a register, returning NULL if the
   * register is a PHI destination and the PHI's LiveOutInfo is not valid.
   * @param reg
   * @return
   */
  public LiveOutInfo getLiveOutInfo(int reg) {
    if (!liveOutRegInfo.containsKey(reg) || liveOutRegInfo.get(reg) == null)
      return null;
    LiveOutInfo loi = liveOutRegInfo.get(reg);
    if (!loi.isValid)
      return null;
    return loi;
  }

  /**
   * Gets LiveOutInfo for a register, returning NULL if the
   * register is a PHI destination and the PHI's LiveOutInfo is not valid. If
   * the register's LiveOutInfo is for a smaller bit width, it is extended to
   * the larger bit width by zero extension. The bit width must be no smaller
   * than the LiveOutInfo's existing bit width.
   * @param reg
   * @param bitWidth
   * @return
   */
  public LiveOutInfo getLiveOutRegInfo(int reg, int bitWidth) {
    if (!liveOutRegInfo.containsKey(reg) || liveOutRegInfo.get(reg) == null)
      return null;

    LiveOutInfo loi = liveOutRegInfo.get(reg);
    if (!loi.isValid)
      return null;

    if (bitWidth > loi.knownZero.getBitWidth()) {
      loi.numSignBits = 1;
      loi.knownZero = loi.knownZero.zextOrTrunc(bitWidth);
      loi.knownOne = loi.knownOne.zext(bitWidth);
    }
    return loi;
  }

  /**
   * Compute LiveOutInfo for a PHI's destination register based on the LiveOutInfo of its operands.
   * @param pn
   */
  public void computePHILiveOutRegInfo(PhiNode pn) {
    if (pn == null || !pn.getType().isIntegerTy() || pn.getType().isVectorTy())
      return;

    Type ty = pn.getType();
    ArrayList<EVT> valueVTs = new ArrayList<>();
    computeValueVTs(tli, ty, valueVTs);
    Util.assertion(valueVTs.size() == 1, "PHIs with non-vector integer types should have a single VT.");
    EVT intVT = valueVTs.get(0);

    if (tli.getNumRegisters(intVT) != 1)
      return;

    intVT = tli.getTypeToTransformTo(ty.getContext(), intVT);
    int bitWidth = intVT.getSizeInBits();
    int destReg = valueMap.get(pn);
    if (!TargetRegisterInfo.isVirtualRegister(destReg))
      return;

    LiveOutInfo destLOI = new LiveOutInfo();
    liveOutRegInfo.put(destReg, destLOI);
    Value v = pn.getIncomingValue(0);
    if (v instanceof Value.UndefValue || v instanceof ConstantExpr) {
      destLOI.numSignBits = 1;
      destLOI.knownZero = new APInt(bitWidth, 0);
      destLOI.knownOne = new APInt(bitWidth, 0);
      return;
    }

    if (v instanceof ConstantInt) {
      ConstantInt ci = (ConstantInt) v;
      APInt val = ci.getValue().zextOrTrunc(bitWidth);
      destLOI.numSignBits = val.getNumSignBits();
      destLOI.knownZero = val.not();
      destLOI.knownOne = val;
    }
    else {
      Util.assertion(valueMap.containsKey(v), "V should have been placed in ValueMap when its CopyToReg node was created!");
      int srcReg = valueMap.get(v);
      if (!TargetRegisterInfo.isVirtualRegister(srcReg)) {
        destLOI.isValid = false;
        return;
      }
      LiveOutInfo srcLOT = getLiveOutRegInfo(srcReg, bitWidth);
      if (srcLOT == null) {
        destLOI.isValid = false;
        return;
      }
      destLOI = srcLOT;
      liveOutRegInfo.put(destReg, destLOI);
    }

    Util.assertion(destLOI.knownZero.getBitWidth() == bitWidth &&
        destLOI.knownOne.getBitWidth() == bitWidth, "Masks should have the same bit width as the type.");
    for (int i = 1, e = pn.getNumberIncomingValues(); i < e; i++) {
      v = pn.getIncomingValue(i);

      if (v instanceof Value.UndefValue || v instanceof ConstantExpr) {
        destLOI.numSignBits = 1;
        destLOI.knownZero = new APInt(bitWidth, 0);
        destLOI.knownOne = new APInt(bitWidth, 0);
        return;
      }

      if (v instanceof ConstantInt) {
        ConstantInt ci = (ConstantInt) v;
        APInt val = ci.getValue().zextOrTrunc(bitWidth);
        destLOI.numSignBits = Math.min(destLOI.numSignBits, val.getNumSignBits());
        destLOI.knownZero.andAssign(val.not());
        destLOI.knownOne.andAssign(val);
        continue;
      }

      Util.assertion(valueMap.containsKey(v), "V should have been placed in ValueMap when its CopyToReg node was created!");
      int srcReg = valueMap.get(v);
      if (!TargetRegisterInfo.isVirtualRegister(srcReg)) {
        destLOI.isValid = false;
        return;
      }
      LiveOutInfo srcLOT = getLiveOutRegInfo(srcReg, bitWidth);
      if (srcLOT == null) {
        destLOI.isValid = false;
        return;
      }
      destLOI.numSignBits = Math.min(destLOI.numSignBits, srcLOT.numSignBits);
      destLOI.knownZero.andAssign(srcLOT.knownZero);
      destLOI.knownOne.andAssign(srcLOT.knownOne);
    }
  }

  public void invalidatePHILiveOutRegInfo(PhiNode pn) {
    if (!valueMap.containsKey(pn)) return;

    int reg = valueMap.get(pn);
    LiveOutInfo loi;
    if (!liveOutRegInfo.containsKey(reg)) {
      loi = new LiveOutInfo();
      liveOutRegInfo.put(reg, loi);
    }
    else
      loi = liveOutRegInfo.get(reg);
    loi.isValid = false;
  }

  /**
   * Adds LiveOutInfo for a register.
   * @param destReg
   * @param numSignBits
   * @param knownZero
   * @param knownOne
   */
  public void addLiveOutRegInfo(int destReg, int numSignBits, APInt knownZero, APInt knownOne) {
    if (numSignBits == 1 && knownZero.eq(0) && knownOne.eq(0))
      return;

    LiveOutInfo loi;
    if (liveOutRegInfo.containsKey(destReg))
      loi = liveOutRegInfo.get(destReg);
    else {
      loi = new LiveOutInfo();
      liveOutRegInfo.put(destReg, loi);
    }
    loi.numSignBits = numSignBits;
    loi.knownOne = knownOne;
    loi.knownZero = knownZero;
  }
}
