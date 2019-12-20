package backend.codegen;

import backend.debug.DebugLoc;
import backend.mc.MCInstrDesc;
import backend.mc.MCOperandInfo;
import backend.mc.MCRegisterClass;
import backend.target.TargetInstrInfo;
import backend.target.TargetMachine;
import backend.target.TargetOpcode;
import backend.target.TargetRegisterInfo;
import backend.value.Value;
import tools.FormattedOutputStream;
import tools.OutRef;
import tools.Util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static backend.mc.MCOperandInfo.OperandConstraint.TIED_TO;
import static backend.target.TargetOpcode.INLINEASM;
import static backend.target.TargetRegisterInfo.isPhysicalRegister;

/**
 * Representation of each machine instruction.
 * <p>
 * <p>MachineOpCode must be an enum, defined separately for each target.
 * E.g., It is defined in .
 * </p>
 * <p>There are 2 kinds of operands:
 * <ol>
 * <li>Explicit operands of the machine instruction in vector operands[].
 * And the more important is that the format of MI is compatible with Intel
 * assembly, where dest operand is in the leftmost as follows:
 * op dest, op0, op1;
 * op dest op0.
 * </li>
 * <li>"Implicit operands" are values implicitly used or defined by the
 * machine instruction, such as arguments to a CALL, return value of
 * a CALL (if any), and return value of a RETURN.</li>
 * </ol>
 * </p>
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public class MachineInstr implements Cloneable {
  private MCInstrDesc tid;

  private int opCode;              // the opcode
  private int opCodeFlags;         // flags modifying instrn behavior

  /**
   * |<-----Explicit operands--------->|-----Implicit operands------->|
   */
  private ArrayList<MachineOperand> operands; // the operands
  private int numImplicitOps;             // number of implicit operands

  private MachineBasicBlock parent;

  private ArrayList<MachineMemOperand> memOperands = new ArrayList<>();

  /**
   * The value records the sum of implicitOps and real operands.
   * Note that it is different with operands.size() which reflect current
   * number of operands been added into operands list, but totalOperands means
   * finally number after complete operands add.
   */
  private int totalOperands;

  private DebugLoc debugLoc;

  /**
   * Return true if it's illegal to add a new operand.
   *
   * @return
   */
  private boolean operandsComplete() {
    int numOperands = tid.getNumOperands();
    if (!tid.isVariadic() && getNumOperands() - numImplicitOps >= numOperands)
      return true;
    return false;
  }

  public MachineInstr(MCInstrDesc tid, DebugLoc dl) {
    this(tid, dl, false);
  }

  public MachineInstr(MCInstrDesc tid, DebugLoc dl, boolean noImp) {
    this.tid = tid;
    debugLoc = dl;
    if (!noImp && tid.getImplicitDefs() != null) {
      numImplicitOps += tid.getImplicitDefs().length;
    }
    if (!noImp && tid.getImplicitUses() != null) {
      numImplicitOps += tid.getImplicitUses().length;
    }
    totalOperands = numImplicitOps + tid.getNumOperands();
    operands = new ArrayList<>();

    if (!noImp)
      addImplicitDefUseOperands();
  }

  public DebugLoc getDebugLoc() { return debugLoc; }


  public void addImplicitDefUseOperands() {
    if (tid.implicitDefs != null) {
      for (int i = 0, e = tid.implicitDefs.length; i < e; i++)
        addOperand(MachineOperand
            .createReg(tid.getImplicitDefs()[i], true, true));
    }
    if (tid.implicitUses != null) {
      for (int i = 0; i < tid.implicitUses.length; i++)
        addOperand(MachineOperand
            .createReg(tid.implicitUses[i], false, true));
    }
  }

  /**
   * Add the specified operand to the instruction.  If it is an
   * implicit operand, it is added to the end of the operand list.  If it is
   * an explicit operand it is added at the end of the explicit operand list
   * (before the first implicit operand).
   *
   * @param mo
   */
  public void addOperand(MachineOperand mo) {
    boolean isImpReg = mo.isRegister() && mo.isImplicit();
    Util.assertion(isImpReg || !operandsComplete(), "Try to add an operand to a machine instr that is already done!");

    MachineRegisterInfo regInfo = getRegInfo();
    // If we are adding the operand to the end of the list, our job is simpler.
    // This is true most of the time, so this is a reasonable optimization.
    if (isImpReg || numImplicitOps == 0) {
      if (operands.isEmpty() || operands.size() < totalOperands) {
        mo.setParent(this);
        operands.add(mo);
        if (mo.isRegister())
          mo.addRegOperandToRegInfo(regInfo);
        return;
      }
    }
    // Otherwise, we have to insert a real operand before any implicit ones.
    int opNo = operands.size() - numImplicitOps;
    if (regInfo == null) {
      mo.setParent(this);
      operands.add(opNo, mo);

      if (mo.isRegister())
        mo.addRegOperandToRegInfo(null);
    } else if (operands.size() < totalOperands) {
      for (int i = opNo; i < operands.size(); i++) {
        Util.assertion(operands.get(i).isRegister(), "Should only be an implicit register!");

        operands.get(i).removeRegOperandFromRegInfo();
      }

      operands.add(opNo, mo);
      mo.setParent(this);
      if (mo.isRegister())
        mo.addRegOperandToRegInfo(regInfo);

      // re-add all the implicit ops.
      for (int i = opNo + 1; i < operands.size(); i++) {
        Util.assertion(operands.get(i).isRegister(), "Should only be an implicit register!");

        operands.get(i).addRegOperandToRegInfo(regInfo);
      }
    } else {
      removeRegOperandsFromUseList();
      operands.add(opNo, mo);
      mo.setParent(this);
      addRegOperandsToUseLists(regInfo);
    }
  }

  public void removeOperand(int opNo) {
    Util.assertion(opNo >= 0 && opNo < operands.size());

    int size = operands.size();
    if (opNo == size - 1) {
      if (operands.get(size - 1).isRegister() && operands.get(size - 1).isOnRegUseList())
        operands.get(size - 1).removeRegOperandFromRegInfo();

      operands.remove(size - 1);
      return;
    }

    MachineRegisterInfo regInfo = getRegInfo();
    if (regInfo != null) {
      for (int i = opNo, e = operands.size(); i != e; i++) {
        if (operands.get(i).isRegister())
          operands.get(i).removeRegOperandFromRegInfo();
      }
    }
    operands.remove(opNo);

    if (regInfo != null) {
      for (int i = opNo, e = operands.size(); i != e; i++) {
        if (operands.get(i).isRegister())
          operands.get(i).addRegOperandToRegInfo(regInfo);
      }
    }
  }

  public MachineRegisterInfo getRegInfo() {
    return parent == null ? null :
        parent.getParent().getMachineRegisterInfo();
  }

  public int getOpcode() {
    return tid.opCode;
  }

  public int getNumOperands() {
    return operands.size();
  }

  public MachineOperand getOperand(int index) {
    Util.assertion(index >= 0 && index < getNumOperands(),
        String.format("%d out of bound %d", index, getNumOperands()));
    return operands.get(index);
  }

  public void setOperand(int index, MachineOperand mo) {
    Util.assertion(index >= 0 && index < getNumOperands(),
        String.format("%d out of bound %d", index, getNumOperands()));
    if (mo == operands.get(index))
      return;

    operands.set(index, mo);
  }

  public int getExplicitOperands() {
    int numOperands = tid.getNumOperands();
    if (!tid.isVariadic())
      return numOperands;

    for (int i = numOperands, e = getNumOperands(); i != e; i++) {
      MachineOperand mo = getOperand(i);
      if (!mo.isRegister() || !mo.isImplicit())
        numOperands++;
    }
    return numOperands;
  }

  public boolean isIdenticalTo(MachineInstr other) {
    if (other.getOpcode() != getOpcode() || other.getNumOperands() != getNumOperands())
      return false;

    for (int i = 0, e = getNumOperands(); i != e; i++)
      if (!getOperand(i).isIdenticalTo(other.getOperand(i)))
        return false;
    return true;
  }

  public void removeFromParent() {
    Util.assertion(getParent() != null);
    for (int i = 0, e = getNumOperands(); i < e; i++) {
      MachineOperand mo = getOperand(i);
      if (mo.isRegister() && mo.getReg() > 0) {
        mo.removeRegOperandFromRegInfo();
      }
    }
    parent.remove(this);
  }

  public boolean isLabel() {
    int op = getOpcode();
    return op == TargetOpcode.PROLOG_LABEL || op == TargetOpcode.EH_LABEL
        || op == TargetOpcode.GC_LABEL;
  }

  public boolean isGCLabel() {
    return getOpcode() == TargetOpcode.GC_LABEL;
  }

  public boolean isEHLabel() {
    return getOpcode() == TargetOpcode.EH_LABEL;
  }

  public boolean isPrologLabel() {
    return getOpcode() == TargetOpcode.PROLOG_LABEL;
  }

  public boolean isPHI() {
    return getOpcode() == TargetOpcode.PHI;
  }

  public boolean isReturn() {
    return getDesc().isReturn();
  }

  public boolean isCall() {
    return getDesc().isCall();
  }

  public boolean isImplicitDef() {
    return getOpcode() == TargetOpcode.IMPLICIT_DEF;
  }

  public boolean isInlineAsm() {
    return getOpcode() == TargetOpcode.INLINEASM;
  }

  public boolean isInsertSubreg() {
    return getOpcode() == TargetOpcode.INSERT_SUBREG;
  }

  public boolean isSubregToReg() {
    return getOpcode() == TargetOpcode.SUBREG_TO_REG;
  }

  public boolean readsRegister(int reg, TargetRegisterInfo tri) {
    return findRegisterUseOperandIdx(reg, false, tri) != -1;
  }

  public boolean killsRegister(int reg) {
    return killsRegister(reg, null);
  }

  public boolean killsRegister(int reg, TargetRegisterInfo tri) {
    return findRegisterUseOperandIdx(reg, true, tri) != -1;
  }

  public boolean modifiedRegister(int reg, TargetRegisterInfo tri) {
    return findRegisterDefOperand(reg, false, tri) != null;
  }

  public boolean registerDefIsDead(int reg, TargetRegisterInfo tri) {
    return findRegisterDefOperand(reg, true, tri) != null;
  }

  public boolean modifiersRegister(int reg) {
    return modifiersRegister(reg, null);
  }

  public boolean modifiersRegister(int reg, TargetRegisterInfo tri) {
    return findRegisterDefOperand(reg, false, tri) != null;
  }

  /**
   * Returns the MachineOperand that is a use of
   * the specific register or -1 if it is not found. It further tightening
   * the search criteria to a use that kills the register if isDeclare is true.
   *
   * @param reg
   * @param isKill
   * @param tri
   * @return
   */
  public int findRegisterUseOperandIdx(int reg,
                                       boolean isKill,
                                       TargetRegisterInfo tri) {
    for (int i = 0, e = getNumOperands(); i != e; i++) {
      MachineOperand mo = getOperand(i);
      if (!mo.isRegister() || !mo.isUse())
        continue;

      int moreg = mo.getReg();
      if (moreg == 0)
        continue;
      if (moreg == reg ||
          (tri != null &&
              isPhysicalRegister(moreg) &&
              isPhysicalRegister(reg) &&
              tri.isSubRegister(moreg, reg))) {
        if (!isKill || mo.isKill())
          return i;
      }
    }
    return -1;
  }

  public MachineOperand findRegisterUseOperand(int reg, boolean isKill, TargetRegisterInfo tri) {
    int idx = findRegisterUseOperandIdx(reg, isKill, tri);
    return idx == -1 ? null : getOperand(idx);

  }

  /**
   * Returns the MachineOperand that is a def of the specific register or -1
   * if it is not found. It further tightening
   * the search criteria to a use that kills the register if isDead is true.
   *
   * @param reg
   * @param isDead
   * @param tri
   * @return
   */
  public MachineOperand findRegisterDefOperand(
      int reg,
      boolean isDead,
      TargetRegisterInfo tri) {
    for (int i = 0, e = getNumOperands(); i != e; i++) {
      MachineOperand mo = getOperand(i);
      if (!mo.isRegister() || !mo.isDef())
        continue;

      int moreg = mo.getReg();
      if (moreg == 0)
        continue;
      if (moreg == reg || (tri != null && isPhysicalRegister(moreg))
          && isPhysicalRegister(reg) && tri.isSubRegister(moreg, reg)) {
        if (!isDead || mo.isDead())
          return mo;
      }
    }
    return null;
  }

  /**
   * Find the index of the first operand in the
   * operand list that is used to represent the predicate. It returns -1 if
   * none is found.
   *
   * @return
   */
  public int findFirstPredOperandIdx() {
    MCInstrDesc tid = getDesc();
    if (tid.isPredicable()) {
      for (int i = 0, e = getNumOperands(); i != e; i++)
        if (tid.opInfo[i].isPredicate())
          return i;
    }
    return -1;
  }

  /**
   * Given the index of a register def operand,
   * check if the register def is tied to a source operand, due to either
   * two-address elimination or inline assembly constraints. Returns the
   * first tied use operand index by reference is useOpIdx is not null.
   *
   * @param defOpIdx
   * @param useOpIdx
   * @return
   */
  public boolean isRegTiedToUseOperand(int defOpIdx, OutRef<Integer> useOpIdx) {
    if (getOpcode() == INLINEASM) {
      Util.assertion(false, "Unsupported inline asm!");
      return false;
    }

    Util.assertion(getOperand(defOpIdx).isDef(), "defOpIdx is not a def!");
    MCInstrDesc TID = getDesc();
    for (int i = 0, e = TID.getNumOperands(); i != e; ++i) {
      MachineOperand MO = getOperand(i);
      if (MO.isRegister() && MO.isUse()
          && TID.getOperandConstraint(i, TIED_TO) == (int) defOpIdx) {
        if (useOpIdx != null)
          useOpIdx.set(i);
        return true;
      }
    }
    return false;
  }

  public boolean isRegTiedToDefOperand(int useOpIdx) {
    return isRegTiedToDefOperand(useOpIdx, null);
  }

  /**
   * Return true if the operand of the specified index
   * is a register use and it is tied to an def operand. It also returns the def
   * operand index by reference.
   *
   * @param useOpIdx
   * @param defOpIdx
   * @return
   */
  public boolean isRegTiedToDefOperand(int useOpIdx, OutRef<Integer> defOpIdx) {
    if (getOpcode() == INLINEASM) {
      Util.assertion(false, "Unsupported inline asm!");
      return false;
    }

    MCInstrDesc tid = getDesc();
    if (useOpIdx >= tid.getNumOperands())
      return false;
    MachineOperand mo = getOperand(useOpIdx);
    if (!mo.isRegister() || !mo.isUse())
      return false;
    int defIdx = tid.getOperandConstraint(useOpIdx, TIED_TO);
    if (defIdx == -1)
      return false;
    if (defOpIdx != null)
      defOpIdx.set(defIdx);
    return true;
  }

  /**
   * Copies kill / dead operand properties from MI.
   *
   * @param mi
   */
  public void copyKillDeadInfo(MachineInstr mi) {
    for (int i = 0, e = mi.getNumOperands(); i != e; i++) {
      MachineOperand mo = mi.getOperand(i);
      if (!mo.isRegister() || (!mo.isKill() && !mo.isDead()))
        continue;
      for (int j = 0, ee = getNumOperands(); j != ee; j++) {
        MachineOperand mop = getOperand(j);
        if (!mop.isIdenticalTo(mo))
          continue;
        if (mo.isKill())
          mop.setIsKill(true);
        else
          mo.setIsDead(true);
        break;
      }
    }
  }

  /**
   * Copies predicate operand(s) from MI.
   *
   * @param mi
   */
  public void copyPredicates(MachineInstr mi) {
    MCInstrDesc tid = getDesc();
    if (!tid.isPredicable())
      return;
    for (int i = 0; i < getNumOperands(); i++) {
      if (tid.opInfo[i].isPredicate())
        addOperand(mi.getOperand(i));
    }
  }

  /**
   * We have determined MI kills a register. Look for the
   * operand that uses it and mark it as IsKill. If addIfNotFound is true,
   * add a implicit operand if it's not found. Returns true if the operand
   * exists / is added.
   *
   * @param incomingReg
   * @param regInfo*
   * @return Return true if the operand exists/ is killed.
   */
  public boolean addRegisterKilled(int incomingReg,
                                   TargetRegisterInfo regInfo) {
    return addRegisterKilled(incomingReg, regInfo, false);
  }

  /**
   * We have determined MI kills a register. Look for the
   * operand that uses it and mark it as IsKill. If addIfNotFound is true,
   * add a implicit operand if it's not found. Returns true if the operand
   * exists / is added.
   *
   * @param incomingReg
   * @param regInfo
   * @param addIfNotFound
   * @return Return true if the operand exists/ is killed.
   */
  public boolean addRegisterKilled(int incomingReg,
                                   TargetRegisterInfo regInfo,
                                   boolean addIfNotFound) {
    boolean isPhysReg = isPhysicalRegister(incomingReg);
    int[] alias = isPhysReg ? regInfo.getAliasSet(incomingReg) : null;
    boolean hasAliases = isPhysReg && alias != null && alias.length > 0;
    boolean found = false;
    LinkedList<Integer> deadOps = new LinkedList<>();
    for (int i = 0, e = getNumOperands(); i != e; ++i) {
      MachineOperand mo = getOperand(i);
      if (!mo.isRegister() || !mo.isUse() ||
          mo.isUndef() || mo.getReg() <= 0)
        continue;
      int reg = mo.getReg();
      if (reg == incomingReg) {
        if (!found) {
          if (mo.isKill())
            // The register is already marked kill.
            return true;
          if (isPhysReg && isRegTiedToDefOperand(i))
            // Two-address uses of physregs must not be marked kill.
            return true;
          mo.setIsKill(true);
          found = true;
        }
      } else if (hasAliases && mo.isKill() && isPhysicalRegister(reg)) {
        // A super-register kill already exists.
        if (regInfo.isSuperRegister(incomingReg, reg))
          return true;
        if (regInfo.isSubRegister(incomingReg, reg))
          deadOps.add(i);
      }
    }

    // Trim unneeded kill operands.
    while (!deadOps.isEmpty()) {
      int opIdx = deadOps.removeLast();
      if (getOperand(opIdx).isImplicit())
        removeOperand(opIdx);
      else
        getOperand(opIdx).setIsKill(false);
    }

    // If not found, this means an alias of one of the operands is killed. Add a
    // new implicit operand if required.
    if (!found && addIfNotFound) {
      addOperand(MachineOperand
          .createReg(incomingReg, false /*isDef*/, true  /*isImp*/,
              true/*isDeclare*/, false, false, false, 0));
      return true;
    }
    return found;
  }

  public boolean addRegisterDead(int IncomingReg, TargetRegisterInfo RegInfo) {
    return addRegisterDead(IncomingReg, RegInfo, false);
  }

  /// addRegisterDead - We have determined MI defined a register without a use.
  /// Look for the operand that defines it and mark it as IsDead. If
  /// addIfNotFound is true, add a implicit operand if it's not found. Returns
  /// true if the operand exists / is added.
  public boolean addRegisterDead(
      int incomingReg,
      TargetRegisterInfo regInfo,
      boolean addIfNotFound) {
    boolean isPhyReg = isPhysicalRegister(incomingReg);
    int[] alias = isPhyReg ? regInfo.getAliasSet(incomingReg) : null;
    boolean hasAlias = isPhyReg && alias != null && alias.length > 0;
    boolean found = false;

    LinkedList<Integer> deadOps = new LinkedList<>();
    for (int i = 0, e = getNumOperands(); i != e; i++) {
      MachineOperand mo = getOperand(i);
      if (!mo.isRegister() || !mo.isDef())
        continue;
      int reg = mo.getReg();
      if (reg == 0)
        continue;

      if (reg == incomingReg) {
        if (!found) {
          if (mo.isDead())
            return true;
          mo.setIsDead(true);
          found = true;
        }
      } else if (hasAlias && mo.isDead() &&
          isPhysicalRegister(reg)) {
        if (regInfo.isSuperRegister(incomingReg, reg)) {
          return true;
        }
        if (regInfo.getSubRegisters(incomingReg) != null &&
            regInfo.getSuperRegisters(reg) != null &&
            regInfo.isSubRegister(incomingReg, reg)) {
          deadOps.add(i);
        }
      }
    }

    while (!deadOps.isEmpty()) {
      int opIdx = deadOps.removeLast();
      if (getOperand(opIdx).isImplicit())
        removeOperand(opIdx);
      else
        getOperand(opIdx).setIsDead(false);
    }

    if (found || !addIfNotFound)
      return found;

    addOperand(MachineOperand.createReg(incomingReg,
        true  /*IsDef*/,
        true  /*IsImp*/,
        false /*IsKill*/,
        true  /*IsDead*/,
        false, false, 0));
    return true;
  }

  /// isSafeToMove - Return true if it is safe to move this instruction. If
  /// SawStore is set to true, it means that there is a store (or call) between
  /// the instruction's location and its intended destination.
  public boolean isSafeToMove(TargetInstrInfo tii,
                              OutRef<Boolean> sawStore) {
    if (tid.mayStore() || tid.isCall()) {
      sawStore.set(true);
      return false;
    }

    if (tid.isTerminator() || tid.hasUnmodeledSideEffects())
      return false;

    if (tid.mayLoad() && !tii.isInvariantLoad(this))
      return !sawStore.get() && !hasVolatileMemoryRef();

    return true;
  }

  /// isSafeToReMat - Return true if it's safe to rematerialize the specified
  /// instruction which defined the specified register instead of copying it.
  public boolean isSafeToReMat(TargetInstrInfo tii, int dstReg) {
    OutRef<Boolean> sawStore = new OutRef<>(false);

    if (!getDesc().isRematerializable() ||
        !tii.isTriviallyReMaterializable(this) ||
        !isSafeToMove(tii, sawStore)) {
      return false;
    }

    for (int i = 0, e = getNumOperands(); i != e; i++) {
      MachineOperand mo = getOperand(i);
      if (!mo.isRegister())
        continue;

      if (mo.isUse())
        return false;
      else if (!mo.isDead() && mo.getReg() != dstReg)
        return false;
    }
    return true;
  }

  /**
   * Return true if this instruction may have a
   * volatile memory reference, or if the information describing the
   * memory reference is not available. Return false if it is known to
   * have no volatile memory references.
   *
   * @return
   */
  public boolean hasVolatileMemoryRef() {
    if (!tid.mayStore() &&
        !tid.mayLoad() &&
        !tid.isCall() &&
        !tid.hasUnmodeledSideEffects())
      return false;

    if (memOperands.isEmpty())
      return true;

    for (MachineMemOperand mmo : memOperands) {
      if (mmo.isVolatile())
        return true;
    }
    return false;
  }

  public void print(FormattedOutputStream os, TargetMachine tm) {
    int startOp = 0;
    TargetRegisterInfo tri = tm.getRegisterInfo();
    MachineRegisterInfo mri = null;
    if (getParent() != null && getParent().getParent() != null)
      mri = getParent().getParent().getMachineRegisterInfo();

    // Saves all virtual registers. We will print all register class for each virtual register.
    HashMap<MCRegisterClass, ArrayList<Integer>> virtRegs = new HashMap<>();
    while (getNumOperands() != 0 && getOperand(startOp).isRegister() &&
        getOperand(startOp).isDef() && !getOperand(startOp).isImplicit()) {

      if (startOp != 0)
        os.print(", ");
      getOperand(startOp).print(os, tm);
      int reg = getOperand(startOp).getReg();
      if (reg != 0 && TargetRegisterInfo.isVirtualRegister(reg) && mri != null) {
        MCRegisterClass rc = mri.getRegClass(reg);
        if (!virtRegs.containsKey(rc))
          virtRegs.put(rc, new ArrayList<>());

        virtRegs.get(rc).add(reg);
      }

      ++startOp;
    }

    if (startOp != 0)
      os.print(" = ");

    // print the opcode name.
    os.printf(getDesc().getName());

    // we have to print the first six operands instead of all operands in the sake of space.
    for (int i = startOp, e = getNumOperands(); i != e; i++) {
      MachineOperand mo = getOperand(i);
      if (mo.isRegister() && TargetRegisterInfo.isVirtualRegister(mo.getReg()) && mri != null) {
        int reg = mo.getReg();
        MCRegisterClass rc = mri.getRegClass(reg);
        if (!virtRegs.containsKey(rc))
          virtRegs.put(rc, new ArrayList<>());

        virtRegs.get(rc).add(reg);
      }

      if (i != startOp)
        os.print(",");
      os.print(" ");

      if (i < getDesc().numOperands) {
        MCOperandInfo mcoi = getDesc().opInfo[i];
        if (mcoi.isPredicate())
          os.print("pred:");
        if (mcoi.isOptionalDef())
          os.print("opt:");
      }
      if (i == 6 && isCall()) {
        os.print("...");
        break;
      }
      getOperand(i).print(os, tm);
    }

    if (!memOperands.isEmpty()) {
      os.print(", mem:");
      for (MachineMemOperand mmo : memOperands) {
        Value v = mmo.getValue();

        Util.assertion(mmo.isLoad() || mmo.isStore(), "SV has to be a load, store or both");

        if (mmo.isVolatile())
          os.print("Volatile ");
        if (mmo.isLoad())
          os.printf("LD");
        if (mmo.isStore())
          os.printf("ST");

        os.printf("(%d,%d) [", mmo.getSize(), mmo.getAlignment());
        if (v == null) {
          os.print("<unknown>");
        } else if (!v.getName().isEmpty()) {
          os.print(v.getName());
        } else
          v.print(os);

        os.printf(" + %d]", mmo.getOffset());
      }
    }

    // print the regclass for each virtual registers used and defined.
    if (!virtRegs.isEmpty()) {
      os.print(";");
      for (Map.Entry<MCRegisterClass, ArrayList<Integer>> entry : virtRegs.entrySet()) {
        MCRegisterClass rc = entry.getKey();
        os.printf(" %s:", rc.getName());
        for (int i = 0, e = entry.getValue().size(); i < e; ++i) {
          int virtReg = entry.getValue().get(i);
          TargetRegisterInfo.printReg(os, virtReg);
          if (i != e - 1)
            os.print(',');
        }
      }
    }
    os.println();
  }

  public void print(PrintStream os, TargetMachine tm) {
    try {
      FormattedOutputStream out = new FormattedOutputStream(os);
      print(out, tm);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void dump() {
    print(System.err, null);
  }

  public void setDesc(MCInstrDesc tid) {
    this.tid = tid;
  }

  /**
   * Unlink all of the register operands in
   * this instruction from their respective use lists.  This requires that the
   * operands already be on their use lists
   */
  private void removeRegOperandsFromUseList() {
    for (int i = 0, e = operands.size(); i != e; i++) {
      if (operands.get(i).isRegister())
        operands.get(i).removeRegOperandFromRegInfo();
    }
  }

  /**
   * Add all of the register operands in
   * this instruction from their respective use lists.  This requires that the
   * operands not be on their use lists yet.
   *
   * @param regInfo
   */
  public void addRegOperandsToUseLists(MachineRegisterInfo regInfo) {
    for (int i = 0, e = operands.size(); i != e; i++) {
      if (operands.get(i).isRegister())
        operands.get(i).addRegOperandToRegInfo(regInfo);
    }
  }

  /**
   * Remove all register operands of the current machine instruction from the
   * corresponding use list.
   */
  public void removeRegOperandsFromUseLists() {
    for (MachineOperand mo : operands) {
      if (mo.isRegister())
        mo.removeRegOperandFromRegInfo();
    }
  }

  public MachineBasicBlock getParent() {
    return parent;
  }

  public void setParent(MachineBasicBlock mbb) {
    parent = mbb;
  }

  public MCInstrDesc getDesc() {
    return tid;
  }

  public void addMemOperand(MachineMemOperand mmo) {
    memOperands.add(mmo);
  }

  public int getIndexOf(MachineOperand op) {
    return operands.indexOf(op);
  }

  @Override
  public MachineInstr clone() {
    MachineInstr res = new MachineInstr(tid, getDebugLoc());
    res.numImplicitOps = 0;
    res.parent = null;

    // Add operands
    for (int i = 0; i != getNumOperands(); i++)
      res.addOperand(getOperand(i).clone());

    res.numImplicitOps = numImplicitOps;

    // Add memory operands.
    for (MachineMemOperand mmo : memOperands)
      res.addMemOperand(mmo);
    return res;
  }

  public boolean hasOneMemOperand() {
    return !memOperands.isEmpty();
  }

  public MachineMemOperand getMemOperand(int index) {
    Util.assertion(index >= 0 && index < memOperands.size());
    return memOperands.get(index);
  }

  public ArrayList<MachineMemOperand> getMemOperands() {
    return memOperands;
  }

  public void setMemOperands(ArrayList<MachineMemOperand> memOperands) {
    this.memOperands = new ArrayList<>();
    this.memOperands.addAll(memOperands);
  }

  public void setMachineOperandReg(int idx, int reg) {
    Util.assertion(idx >= 0 && idx < getNumOperands());
    //Util.assertion( isPhysicalRegister(reg));
    Util.assertion(getOperand(idx).isRegister());
    getOperand(idx).setReg(reg);
  }

  public int getIndexInMBB() {
    return getParent().getInsts().indexOf(this);
  }

  public String getName() {
    return getDesc().getName();
  }

  @Override
  public String toString() {
    return getName();
  }

  public void clearMemOperands() {
    memOperands.clear();
  }

  public void substituteRegister(int fromReg,
                                 int toReg,
                                 int subIdx,
                                 TargetRegisterInfo tri) {
    if (TargetRegisterInfo.isPhysicalRegister(toReg)) {
      if (subIdx != 0)
        toReg = tri.getSubReg(toReg, subIdx);
      for (int i = 0, e = getNumOperands(); i< e; ++i) {
        MachineOperand mo = getOperand(i);
        if (mo.isRegister() && mo.getReg() == fromReg)
          mo.substPhysReg(toReg, tri);
      }
    }
    else {
      for (int i = 0, e = getNumOperands(); i < e; ++i) {
        MachineOperand mo = getOperand(i);
        if (mo.isRegister() && mo.getReg() == fromReg)
          mo.substVirtReg(toReg, subIdx, tri);
      }
    }
  }
}
