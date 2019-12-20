package backend.codegen;

import backend.mc.MCSymbol;
import backend.target.TargetMachine;
import backend.target.TargetRegisterInfo;
import backend.value.*;
import tools.FormattedOutputStream;
import tools.Util;

import java.io.PrintStream;

import static backend.codegen.MachineOperand.MachineOperandType.*;

/**
 * Purpose:
 * Representation of each machine instruction operand.
 * This class is designed so that you can allocate a vector of operands
 * first and initialize each one later.
 * <p>
 * E.g, for this VM instruction:
 * ptr = alloca type, numElements
 * we generate 2 machine instructions on the SPARC:
 * <p>
 * mul Constant, Numelements -> Reg
 * add %sp, Reg -> Ptr
 * <p>
 * Each instruction has 3 operands, listed above.  Of those:
 * -	Reg, NumElements, and Ptr are of operand type MO_Register.
 * -	Constant is of operand type MO_SignExtendedImmed on the SPARC.
 * <p>
 * For the register operands, the virtual register type is as follows:
 * <p>
 * -  Reg will be of virtual register type MO_MInstrVirtualReg.  The field
 * MachineInstr* minstr will point to the instruction that computes reg.
 * <p>
 * -	%sp will be of virtual register type MO_MachineReg.
 * The field regNum identifies the machine register.
 * <p>
 * -	NumElements will be of virtual register type MO_VirtualReg.
 * The field Value value identifies the value.
 * <p>
 * -	Ptr will also be of virtual register type MO_VirtualReg.
 * Again, the field Value value identifies the value.
 *
 * @author Jianping Zeng
 * @version 0.4
 */

public abstract class MachineOperand {
  public interface RegState {
    int Define = 0x2;             /// This machine operand is only written by the instruction
    int Implicit = 0x4;
    int Kill = 0x8;
    int Dead = 0x10;
    int Undef = 0x20;
    int EarlyClobber = 0x40;
    int Debug = 0x80;
    int ImplicitDefine = Implicit | Define;
    int ImplicitKill = Implicit | Kill;
  }

  public enum MachineOperandType {
    MO_Register,        // register for *value
    MO_Immediate,
    MO_FPImmediate,
    MO_MachineBasicBlock,       // MachineBasicBlock reference
    MO_FrameIndex,              // Abstract Stack Frame Index
    MO_ConstantPoolIndex,       // Address of indexed Constant in Constant Pool
    MO_JumpTableIndex,
    MO_ExternalSymbol,          // Name of external global symbol
    MO_GlobalAddress,           // Address of a global value
    MO_BlockAddress,            // Address of a basic block
    MO_Metadata,                 // Metadata reference (for debug)
    MO_MCSymbol                  // MCSymbol reference (for debug/eh info)
  }

  // Bit fields of the flags variable used for different operand properties
  interface Kind {
    int DEFFLAG = 0x01;       // this is a def but not a use of the operand
    int DEFUSEFLAG = 0x02;       // this is both a def and a use
    int HIFLAG32 = 0x04;       // operand is %hi32(value_or_immedVal)
    int LOFLAG32 = 0x08;       // operand is %lo32(value_or_immedVal)
    int HIFLAG64 = 0x10;       // operand is %hi64(value_or_immedVal)
    int LOFLAG64 = 0x20;       // operand is %lo64(value_or_immedVal)
    int PCRELATIVE = 0x40;     // Operand is relative to PC, not a global address

    int USEDEFMASK = 0x03;
  }

  public abstract MachineOperand clone();

  public void substPhysReg(int reg, TargetRegisterInfo tri) {
    Util.assertion(TargetRegisterInfo.isPhysicalRegister(reg));
    if (getSubReg() != 0) {
      reg = tri.getSubReg(reg, getSubReg());
      setSubreg(0);
    }
    setReg(reg);
  }

  public void substVirtReg(int reg, int subIdx, TargetRegisterInfo tri) {
    /*Util.assertion(TargetRegisterInfo.isVirtualRegister(reg));
    if (subIdx != 0 && getSubReg() != 0)
      subIdx = tri.composeSubRegIndices(subIdx, getSubReg());
    setReg(reg);
    if (subIdx != 0)
      setSubreg(subIdx);*/
    Util.shouldNotReachHere();
  }

  static class RegOp {
    int regNo;
    MachineOperand prev;   // Access list for register.
    MachineOperand next;

    RegOp(int regNo) {
      this(regNo, null, null);
    }

    RegOp(int regNo, MachineOperand prev, MachineOperand next) {
      this.regNo = regNo;
      this.prev = prev;
      this.next = next;
    }

    public void clear() {
      regNo = 0;
      prev = null;
      next = null;
    }
  }

  private static class RegisterMO extends MachineOperand {
    /**
     * Virtual register for an SSA operand, including hidden operands
     * required for the generated machine code.
     */
    RegOp reg;
    int subReg;
    /**
     * This flag indicates if this machine operand is a definition register.
     */
    protected boolean isDef;
    /**
     * This flag indicates if this machine operand is a implicit operand.
     */
    protected boolean isImp;
    /**
     * This flag indicates if this machine operand is the last use of the specified register.
     */
    protected boolean isKill;
    /**
     * This flag indicates if this machine operand is definition of the register without
     * subsequent use.
     */
    protected boolean isDead;
    /**
     * This flag indicates if this machine operand is a register def/use of "undef",
     * for example, register defined by IMPLICIT_DEF. this is only valid on register.
     */
    protected boolean isUndef;

    /**
     * True if this MO_Register 'def' operand is written to
     * by the MachineInstr before all input registers are read.  This is used to
     * model the GCC inline asm '&' constraint modifier.
     */
    protected boolean isEarlyClobber;
    /**
     * Indicates if the machine operand is used in debug pseudo, not a real instruction.
     * Such users should be ignored during codegen.
     */
    protected boolean isDebug;
    /**
     * The machine instruction in which this machine operand is embedded.
     */
    RegisterMO(MachineInstr mi, int r, int subReg) {
      super(MO_Register, mi);
      reg = new RegOp(r);
      this.subReg = subReg;
    }

    RegisterMO(MachineInstr mi, int r) {
      this(mi, r, 0);
    }

    @Override
    public MachineOperand clone() {
      RegisterMO res = new RegisterMO(getParent(), reg.regNo, subReg);
      res.targetFlags = getTargetFlags();
      res.isDef = isDef;
      res.isImp = isImp;
      res.isKill = isKill;
      res.isDead = isDead;
      res.isUndef = isUndef;
      res.isEarlyClobber = isEarlyClobber;
      res.isDebug = isDebug;
      return res;
    }
  }

  private static class ImmediateMO extends MachineOperand {
    // ConstantVal for a non-address immediate.
    long immVal;        // Constant value for an explicit ant
    ImmediateMO(MachineInstr mi, long val) {
      super(MO_Immediate, mi);
      immVal = val;
    }

    @Override
    public MachineOperand clone() {
      ImmediateMO res = new ImmediateMO(getParent(), immVal);
      res.targetFlags = getTargetFlags();
      return res;
    }
  }

  private static class FPImmediateMO extends MachineOperand {
    // For constant FP.
    ConstantFP cfp;
    FPImmediateMO(MachineInstr mi, ConstantFP fp) {
      super(MO_FPImmediate, mi);
      cfp = fp;
    }

    @Override
    public MachineOperand clone() {
      FPImmediateMO res = new FPImmediateMO(getParent(), cfp);
      res.targetFlags = getTargetFlags();
      return res;
    }
  }

  private static class MachineBasicBlockMO extends MachineOperand {
    // For MO_MachineBasicBlock type
    MachineBasicBlock mbb;
    MachineBasicBlockMO(MachineInstr mi, MachineBasicBlock mbb) {
      super(MO_MachineBasicBlock, mi);
      this.mbb = mbb;
    }

    @Override
    public MachineOperand clone() {
      MachineBasicBlockMO res = new MachineBasicBlockMO(getParent(), mbb);
      res.targetFlags = getTargetFlags();
      return res;
    }
  }

  private static class IndexMO extends MachineOperand {
    /**
     * For MO_FrameIndex.
     */
    int index;
    /**
     * The offset from the object.
     */
    long offset;
    private IndexMO(MachineOperandType k, MachineInstr mi,
                    int index, long offset) {
      super(k, mi);
      this.index = index;
      this.offset = offset;
    }

    @Override
    public MachineOperand clone() {
      IndexMO res = new IndexMO(getType(), getParent(), index, offset);
      res.targetFlags = getTargetFlags();
      return res;
    }
  }

  private static class FrameIndexMO extends IndexMO {
    FrameIndexMO(MachineInstr mi, int idx, long offset) {
      super(MO_FrameIndex, mi, idx, offset);
    }
  }

  private static class ConstantPoolIndexMO extends IndexMO {
    ConstantPoolIndexMO(MachineInstr mi, int idx, long offset) {
      super(MO_ConstantPoolIndex, mi, idx, offset);
    }
  }

  private static class JumpTableIndexMO extends IndexMO {
    JumpTableIndexMO(MachineInstr mi, int idx, long offset) {
      super(MO_JumpTableIndex, mi, idx, offset);
    }
  }

  private static class ExternalSymbolMO extends MachineOperand {
    String symbolName;
    long offset;
    ExternalSymbolMO(MachineInstr mi, String sym, long offset) {
      super(MO_ExternalSymbol, mi);
      symbolName = sym;
      this.offset = offset;
    }

    @Override
    public MachineOperand clone() {
      ExternalSymbolMO res = new ExternalSymbolMO(getParent(), symbolName, offset);
      res.targetFlags = getTargetFlags();
      return res;
    }
  }

  private static class GlobalAddressMO extends MachineOperand {
    GlobalValue gv;
    long offset;
    // LLVM global for MO_GlobalAddress.
    GlobalAddressMO(MachineInstr mi, GlobalValue gv, long offset) {
      super(MO_GlobalAddress, mi);
      this.gv = gv;
      this.offset = offset;
    }

    @Override
    public MachineOperand clone() {
      GlobalAddressMO res = new GlobalAddressMO(getParent(), gv, offset);
      res.targetFlags = getTargetFlags();
      return res;
    }
  }

  private static class BlockAddresssMO extends MachineOperand {
    BlockAddress ba;
    long offset;
    BlockAddresssMO(MachineInstr mi, BlockAddress ba, long offset) {
      super(MO_BlockAddress, mi);
      this.ba = ba;
      this.offset = offset;
    }

    @Override
    public MachineOperand clone() {
      BlockAddresssMO res = new BlockAddresssMO(getParent(), ba, offset);
      res.targetFlags = getTargetFlags();
      return res;
    }
  }

  private static class MetadataMO extends MachineOperand {
    MDNode md;
    MetadataMO(MachineInstr mi, MDNode node) {
      super(MO_Metadata, mi);
      md = node;
    }

    @Override
    public MachineOperand clone() {
      MetadataMO res = new MetadataMO(getParent(), md);
      res.targetFlags = getTargetFlags();
      return res;
    }
  }

  private static class MCSymbolMO extends MachineOperand {
    MCSymbol sym;
    MCSymbolMO(MachineInstr mi, MCSymbol sym) {
      super(MO_MCSymbol, mi);
      this.sym = sym;
    }

    @Override
    public MachineOperand clone() {
      MCSymbolMO res = new MCSymbolMO(getParent(), sym);
      res.targetFlags = getTargetFlags();
      return res;
    }
  }

  /**
   * Which kind of this machine operand.
   */
  protected MachineOperandType opKind;
  /**
   * This is a set of target-specific flags.
   */
  protected int targetFlags;
  protected MachineInstr parentMI;

  // will be set for a value after reg allocation
  private MachineOperand(MachineOperandType k, MachineInstr mi) {
    opKind = k;
    parentMI = mi;
  }

  // Accessor methods.  Caller is responsible for checking the
  // operand type before invoking the corresponding accessor.
  //
  public MachineOperandType getType() {
    return opKind;
  }

  public int getTargetFlags() {
    return targetFlags;
  }

  public void setTargetFlags(int targetFlags) {
    this.targetFlags = targetFlags;
  }

  public void addTargetFlag(int f) {
    targetFlags |= f;
  }

  public MachineInstr getParent() {
    return parentMI;
  }

  public void setParent(MachineInstr parentMI) {
    this.parentMI = parentMI;
  }

  public void print(PrintStream os) {
    print(os, null);
  }

  public void print(PrintStream os, TargetMachine tm) {
    try (FormattedOutputStream out = new FormattedOutputStream(os)) {
      print(out, tm);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void print(FormattedOutputStream os, TargetMachine tm) {
    switch (getType()) {
      case MO_Register: {
        if (TargetRegisterInfo.isVirtualRegister(getReg())) {
          os.printf("%%reg%d", getReg());
        } else if (getReg() == 0) {
          os.print("%noreg");
        } else {
          // The used register is physical register.
          if (tm == null) {
            MachineInstr mi = getParent();
            if (mi != null) {
              MachineBasicBlock mbb = mi.getParent();
              if (mbb != null) {
                MachineFunction mf = mbb.getParent();
                if (mf != null)
                  tm = mf.getTarget();
              }
            }
          }
          if (tm != null)
            os.printf("%%%s", tm.getRegisterInfo().getName(getReg()));
          else
            os.printf("%%reg%d", getReg());
        }
        if (getSubReg() != 0)
          os.printf(":%d", getSubReg());

        if (isDef() || isKill() || isDead() || isImplicit() || isUndef()
            || isEarlyClobber()) {
          os.printf("<");
          boolean needComma = false;
          if (isImplicit()) {
            if (needComma)
              os.printf(",");
            os.printf(isDef() ? "imp-def" : "imp-use");
            needComma = true;
          } else if (isDef()) {
            if (needComma)
              os.printf(",");
            if (isEarlyClobber())
              os.printf("earlyclobber");
            os.printf("def");
            needComma = true;
          }
          if (isKill() || isDead() || isUndef()) {
            if (needComma)
              os.printf(",");
            if (isKill())
              os.printf("kill");
            if (isDead())
              os.printf("dead");
            if (isUndef()) {
              if (isKill() || isDead())
                os.printf(",");
              os.printf("undef");
            }
          }
          os.printf(">");
        }
        break;
      }
      case MO_Immediate:
        os.print(getImm());
        break;
      case MO_FPImmediate:
        if (getFPImm().getType().isFloatTy())
          os.print(getFPImm().getValueAPF().convertToFloat());
        else
          os.print(getFPImm().getValueAPF().convertToDouble());
        break;
      case MO_MachineBasicBlock:
        os.printf("<BB#%d>", getMBB().getNumber());
        break;
      case MO_FrameIndex:
        os.printf("<fi#%d>", getIndex());
        break;
      case MO_ConstantPoolIndex:
        os.printf("<cp#%d", getIndex());
        if (getOffset() != 0)
          os.printf("+%d", getOffset());
        os.printf(">");
        break;
      case MO_JumpTableIndex:
        os.printf("<ji#%d>", getIndex());
        break;
      case MO_GlobalAddress:
        os.printf("<ga:%s", getGlobal().getName());
        if (getOffset() != 0)
          os.printf("+%d", getOffset());
        os.print(">");
        break;
      case MO_ExternalSymbol:
        os.printf("<es:%s", getSymbolName());
        if (getOffset() != 0)
          os.printf("+%d", getOffset());
        os.print(">");
        break;
      default:
        Util.shouldNotReachHere("Unrecognized operand type");
        break;
    }
    int tf = getTargetFlags();
    if (tf != 0)
      os.printf("[TF=%d]", tf);
  }

  public boolean isRegister() {
    return opKind == MO_Register;
  }

  public boolean isMBB() {
    return opKind == MO_MachineBasicBlock;
  }

  public boolean isImm() {
    return opKind == MO_Immediate;
  }

  public boolean isFPImm() {
    return opKind == MO_FPImmediate;
  }

  public boolean isFrameIndex() {
    return opKind == MO_FrameIndex;
  }

  public boolean isConstantPoolIndex() {
    return opKind == MO_ConstantPoolIndex;
  }

  public boolean isGlobalAddress() {
    return opKind == MO_GlobalAddress;
  }

  public boolean isExternalSymbol() {
    return opKind == MO_ExternalSymbol;
  }

  public boolean isJumpTableIndex() {
    return opKind == MO_JumpTableIndex;
  }

  public int getReg() {
    Util.assertion(isRegister(), "This is not a register operand!");
    return ((RegisterMO)this).reg.regNo;
  }

  public RegOp getRegOp() {
    Util.assertion(isRegister(), "This is not a register operand!");
    return ((RegisterMO)this).reg;
  }

  public int getSubReg() {
    Util.assertion(isRegister(), "Wrong MachineOperand accessor");
    return ((RegisterMO)this).subReg;
  }

  private RegisterMO getAsRegOp() {
    Util.assertion(isRegister(), "Wrong MachineOperand accessor");
    return (RegisterMO)this;
  }
  public boolean isUse() {
    Util.assertion(isRegister(), "Wrong MachineOperand accessor");
    return !getAsRegOp().isDef;
  }

  public boolean isDef() {
    Util.assertion(isRegister(), "Wrong MachineOperand accessor");
    return getAsRegOp().isDef;
  }

  public boolean isImplicit() {
    Util.assertion(isRegister(), "Wrong MachineOperand accessor");
    return getAsRegOp().isImp;
  }

  public boolean isDead() {
    Util.assertion(isRegister(), "Wrong MachineOperand accessor");
    return getAsRegOp().isDead;
  }

  public boolean isKill() {
    Util.assertion(isRegister(), "Wrong MachineOperand accessor");
    return getAsRegOp().isKill;
  }

  public boolean isUndef() {
    Util.assertion(isRegister(), "Wrong MachineOperand accessor");
    return getAsRegOp().isUndef;
  }

  public boolean isEarlyClobber() {
    Util.assertion(isRegister(), "Wrong MachineOperand accessor");
    return getAsRegOp().isEarlyClobber;
  }

  public MachineOperand getNextOperandForReg() {
    Util.assertion(isRegister(), "Wrong MachineOperand accessor");
    return ((RegisterMO)this).reg.next;
  }

  public void setReg(int reg) {
    if (getReg() == reg)
      return;

    // Otherwise, we have to change the register.  If this operand is embedded
    // into a machine function, we need to update the old and new register's
    // use/def lists.
    MachineInstr mi = getParent();
    if (mi != null) {
      MachineBasicBlock mbb = mi.getParent();
      if (mbb != null) {
        MachineFunction mf = mbb.getParent();
        if (mf != null) {
          removeRegOperandFromRegInfo();
          ((RegisterMO)this).reg.regNo = reg;
          addRegOperandToRegInfo(mf.getMachineRegisterInfo());
          return;
        }
      }
    }

    // Otherwise, just change the register, no problem.  :)
    ((RegisterMO)this).reg.regNo = reg;
  }

  public void setSubreg(int subreg) {
    Util.assertion(isRegister(), "Wrong MachineOperand accessor");
    ((RegisterMO)this).subReg = subreg;
  }

  public void setIsUse(boolean val) {
    Util.assertion(isRegister(), "Wrong MachineOperand accessor");
    getAsRegOp().isDef = !val;
  }

  public void setIsDef(boolean val) {
    Util.assertion(isRegister(), "Wrong MachineOperand accessor");
    getAsRegOp().isDef = val;
  }

  public void setImplicit(boolean val) {
    Util.assertion(isRegister(), "Wrong MachineOperand accessor");
    getAsRegOp().isImp = val;
  }

  public void setIsKill(boolean val) {
    Util.assertion(isRegister(), "Wrong MachineOperand accessor");
    getAsRegOp().isKill = val;
  }

  public void setIsDead(boolean val) {
    Util.assertion(isRegister(), "Wrong MachineOperand accessor");
    getAsRegOp().isDead = val;
  }

  public void setIsUndef(boolean val) {
    Util.assertion(isRegister(), "Wrong MachineOperand accessor");
    getAsRegOp().isUndef = val;
  }

  public void setIsEarlyClobber(boolean val) {
    Util.assertion(isRegister(), "Wrong MachineOperand accessor");
    getAsRegOp().isEarlyClobber = val;
  }

  public long getImm() {
    Util.assertion(isImm(), "Wrong MachineOperand accessor");
    return ((ImmediateMO)this).immVal;
  }

  public ConstantFP getFPImm() {
    Util.assertion(isFPImm(), "Wrong MachineOperand accessor");
    return ((FPImmediateMO)this).cfp;
  }

  public MachineBasicBlock getMBB() {
    Util.assertion(isMBB(), "Can't get mbb in non-mbb operand!");
    return ((MachineBasicBlockMO)this).mbb;
  }

  public int getIndex() {
    Util.assertion(isFrameIndex() || isConstantPoolIndex() ||
        isJumpTableIndex(), "Wrong MachineOperand accessor");
    return ((IndexMO)this).index;
  }

  public long getOffset() {
    Util.assertion(isGlobalAddress() || isExternalSymbol() ||
        isConstantPoolIndex() || isJumpTableIndex(), "Wrong MachineOperand accessor");
    if (this instanceof IndexMO)
      return ((IndexMO)this).offset;
    else if (this instanceof ExternalSymbolMO)
      return ((ExternalSymbolMO)this).offset;
    else
      return ((GlobalAddressMO)this).offset;
  }

  public GlobalValue getGlobal() {
    Util.assertion(isGlobalAddress(), "Wrong MachineOperand accessor");
    return ((GlobalAddressMO)this).gv;
  }

  public String getSymbolName() {
    Util.assertion(isExternalSymbol(), "Wrong MachineOperand accessor");
    return ((ExternalSymbolMO)this).symbolName;
  }

  public void setImm(long imm) {
    Util.assertion(isImm(), "Wrong MachineOperand accessor");
    ((ImmediateMO)this).immVal = imm;
  }

  public void setOffset(long offset) {
    Util.assertion(isGlobalAddress() || isExternalSymbol() ||
        isConstantPoolIndex() || isJumpTableIndex(), "Wrong MachineOperand accessor");
    if (this instanceof IndexMO)
      ((IndexMO)this).offset = offset;
    else if (this instanceof ExternalSymbolMO)
      ((ExternalSymbolMO)this).offset = offset;
    else
      ((GlobalAddressMO)this).offset = offset;
  }

  public void setIndex(int idx) {
    Util.assertion(isFrameIndex() || isConstantPoolIndex() ||
        isJumpTableIndex(), "Wrong MachineOperand accessor");
    ((IndexMO)this).index = idx;
  }

  public void setMBB(MachineBasicBlock mbb) {
    Util.assertion(isMBB(), "Wrong MachineOperand accessor");
    ((MachineBasicBlockMO)this).mbb = mbb;
  }

  public BlockAddress getBlockAddress() {
    Util.assertion(isBlockAddress(), "Wrong MachineOperand accessor");
    return ((BlockAddresssMO)this).ba;
  }

  public boolean isBlockAddress() {
    return opKind == MO_BlockAddress;
  }
  /**
   * Return true if this operand is identical to the specified
   * operand. Note: This method ignores isDeclare and isDead properties.
   *
   * @param other
   * @return
   */
  public boolean isIdenticalTo(MachineOperand other) {
    if (getType() != other.getType()
        || getTargetFlags() != other.getTargetFlags())
      return false;

    switch (getType()) {
      default:
        Util.shouldNotReachHere("Unrecognized operand type");
        return false;
      case MO_Register:
        return getReg() == other.getReg() && isDef() == other.isDef() &&
            getSubReg() == other.getSubReg();
      case MO_Immediate:
        return getImm() == other.getImm();
      case MO_FPImmediate:
        return getFPImm().equals(other.getFPImm());
      case MO_MachineBasicBlock:
        return getMBB().equals(other.getMBB());
      case MO_FrameIndex:
      case MO_JumpTableIndex:
        return getIndex() == other.getIndex();
      case MO_ConstantPoolIndex:
        return getIndex() == other.getIndex() &&
            getOffset() == other.getOffset();
      case MO_GlobalAddress:
        return getGlobal() == other.getGlobal() &&
            getOffset() == other.getOffset();
      case MO_ExternalSymbol:
        return getSymbolName().equals(other.getSymbolName()) &&
            getOffset() == other.getOffset();
    }
  }

  /**
   * Replace this operand with a new immediate operand of
   * the specified value.  If an operand is known to be an immediate already,
   * the setImm method should be used.
   *
   * @param immVal
   */
  public void changeToImmediate(long immVal) {
    if (isRegister() && getParent() != null && getParent().getParent() != null
        && getParent().getParent().getParent() != null)
      removeRegOperandFromRegInfo();
    opKind = MO_Immediate;
    ((ImmediateMO)this).immVal = immVal;
  }

  public void removeRegOperandFromRegInfo() {
    // Unlink this reg operand from reg def-use linked list.
    Util.assertion(isOnRegUseList(), "Reg operand is not on a use list");
    MachineRegisterInfo regInfo = parentMI.getRegInfo();
    RegisterMO regMO = ((RegisterMO)this);
    MachineOperand head = regInfo.getRegUseDefListHead(regMO.reg.regNo);
    if (head.equals(this)) {
      if (((RegisterMO)head).reg.next != null) {
        ((RegisterMO)(((RegisterMO)head).reg.next)).reg.prev = ((RegisterMO)head).reg.prev;
        if (((RegisterMO)head).reg.prev != null)
          ((RegisterMO)((RegisterMO)head).reg.prev).reg.next = ((RegisterMO)head).reg.next;

        regInfo.updateRegUseDefListHead(getReg(), ((RegisterMO)head).reg.next);
        ((RegisterMO)head).reg.next = null;
      }
      else if (((RegisterMO)head).reg.prev != null) {
        regInfo.updateRegUseDefListHead(getReg(), ((RegisterMO)head).reg.prev);
        ((RegisterMO)((RegisterMO)head).reg.prev).reg.next = ((RegisterMO)head).reg.next;
        ((RegisterMO)head).reg.prev = null;
      }
      else
        regInfo.updateRegUseDefListHead(getReg(), null);

    } else {
      if (regMO.reg.prev != null) {
        Util.assertion(regMO.reg.prev.getReg() == getReg(), "Corrupt reg use/def chain!");

        ((RegisterMO)regMO.reg.prev).reg.next = regMO.reg.next;
      }
      if (regMO.reg.next != null) {
        Util.assertion(regMO.reg.next.getReg() == getReg(), "Corrupt reg use/def chain!");

        ((RegisterMO)regMO.reg.next).reg.prev = regMO.reg.prev;
      }
    }
    regMO.reg.prev = null;
    regMO.reg.next = null;
  }

  /**
   * Return true if this operand is on a register use/def list
   * or false if not.  This can only be called for register operands that are
   * part of a machine instruction.
   *
   * @return
   */
  public boolean isOnRegUseList() {
    Util.assertion(isRegister(), "Can only add reg operand to use lists");
    if (parentMI != null) {
      MachineRegisterInfo regInfo = parentMI.getRegInfo();
      MachineOperand head = regInfo.getRegUseDefListHead(((RegisterMO)this).reg.regNo);
      if (head != null)
        return true;
    }
    return false;
  }

  public MachineOperand changeToRegister(int reg,
                                         boolean isDef) {
    return changeToRegister(reg, isDef, false, false, false, false);
  }

  /**
   * Replace this operand with a new register operand of
   * the specified value.  If an operand is known to be an register already,
   * the setReg method should be used.
   *
   * @param reg
   * @param isDef
   * @param isImp
   * @param isKill
   * @param isDead
   * @param isUndef
   */
  public MachineOperand changeToRegister(int reg,
                                         boolean isDef,
                                         boolean isImp,
                                         boolean isKill,
                                         boolean isDead,
                                         boolean isUndef) {
    if (isRegister()) {
      Util.assertion(!isEarlyClobber());
      setReg(reg);
      return this;
    } else {
      RegisterMO res = new RegisterMO(getParent(), reg);
      res.isDef = isDef;
      res.isImp = isImp;
      res.isKill = isKill;
      res.isDead = isDead;
      res.isUndef = isUndef;
      res.isEarlyClobber = false;
      res.subReg = 0;

      MachineFunction mf;
      if (getParent() != null) {
        if (parentMI.getParent() != null)
          if ((mf = parentMI.getParent().getParent()) != null)
            res.addRegOperandToRegInfo(mf.getMachineRegisterInfo());
      }
      return res;
    }
  }

  /**
   * Add this register operand to the specified MachineRegisterInfo.
   * If it is null, then the next/prev fields should be explicitly
   * nulled out.
   *
   * @param regInfo
   */
  public void addRegOperandToRegInfo(MachineRegisterInfo regInfo) {
    // FIXME 2017/11/18, this method works inproperly.
    Util.assertion(isRegister(), "Can only add reg operand to use lists");

    // If the reginfo pointer is null, just explicitly null out or next/prev
    // pointers, to ensure they are not garbage.
    if (regInfo == null) {
      ((RegisterMO)this).reg.prev = null;
      ((RegisterMO)this).reg.next = null;
      return;
    }
    // Otherwise, add this operand to the head of the registers use/def list.
    MachineOperand head = regInfo.getRegUseDefListHead(getReg());
    if (head == null) {
      // If the head node is null, set current op as head node.
      ((RegisterMO)this).reg.prev = null;
      ((RegisterMO)this).reg.next = null;
      regInfo.updateRegUseDefListHead(getReg(), this);
      return;
    }

    if (isDef()) {
      if (!head.isDef()) {
        // insert the current node as head
        ((RegisterMO)this).reg.next = head;
        ((RegisterMO)this).reg.prev = null;
        ((RegisterMO)head).reg.prev = this;
        regInfo.updateRegUseDefListHead(getReg(), this);
        return;
      }
    }
    // Insert this machine operand into where immediately after head node.
    //  [    ] ---> [] -------->NULL
    //  [head] <--- []
    //  |    }      []
    //              ^ (prev)  ^  insert here (cur)
    MachineOperand cur = head, prev = head;
    while (cur != null) {
      prev = cur;
      cur = ((RegisterMO)cur).reg.next;
    }

    ((RegisterMO)prev).reg.next = this;
    ((RegisterMO)this).reg.prev = prev;
    ((RegisterMO)this).reg.next = null;
  }

  public static MachineOperand createImm(long val) {
    return new ImmediateMO(null, val);
  }

  public static MachineOperand createFPImm(ConstantFP fp) {
    return new FPImmediateMO(null, fp);
  }

  public static MachineOperand createReg(int reg,
                                         boolean isDef,
                                         boolean isImp) {
    return createReg(reg, isDef, isImp, false, false,
        false, false, 0);
  }

  public static MachineOperand createReg(int reg,
                                         boolean isDef,
                                         boolean isImp,
                                         boolean isKill,
                                         boolean isDead,
                                         boolean isUndef,
                                         boolean isEarlyClobber,
                                         int subreg) {

    MachineOperand op = new RegisterMO(null, reg, subreg);
    op.getAsRegOp().isDef = isDef;
    op.getAsRegOp().isImp = isImp;
    op.getAsRegOp().isKill = isKill;
    op.getAsRegOp().isDead = isDead;
    op.getAsRegOp().isUndef = isUndef;
    op.getAsRegOp().isEarlyClobber = isEarlyClobber;
    return op;
  }

  public static MachineOperand createMBB(MachineBasicBlock mbb, int targetFlags) {
    MachineOperand op = new MachineBasicBlockMO(null, mbb);
    op.setMBB(mbb);
    op.setTargetFlags(targetFlags);
    return op;
  }


  public static MachineOperand createFrameIndex(int idx) {
    return new FrameIndexMO(null, idx, 0);
  }

  public static MachineOperand createConstantPoolIndex(int idx,
                                                       long offset,
                                                       int targetFlags) {
    MachineOperand op = new ConstantPoolIndexMO(null, idx, offset);
    op.setTargetFlags(targetFlags);
    return op;
  }

  public static MachineOperand createJumpTableIndex(int idx, int targetFlags) {
    MachineOperand op = new JumpTableIndexMO(null, idx, 0);
    op.setTargetFlags(targetFlags);
    return op;
  }

  public static MachineOperand createGlobalAddress(GlobalValue gv,
                                                   long offset,
                                                   int targetFlags) {
    MachineOperand op = new GlobalAddressMO(null, gv, offset);
    op.setTargetFlags(targetFlags);
    return op;
  }

  public static MachineOperand createExternalSymbol(String symName,
                                                    long offset,
                                                    int targetFlags) {
    MachineOperand op = new ExternalSymbolMO(null, symName, offset);
    op.setTargetFlags(targetFlags);
    return op;
  }

  public static MachineOperand createBlockAddress(BlockAddress ba,
                                                  int targetFlags) {
    return new BlockAddresssMO(null, ba, targetFlags);
  }

  public static MachineOperand createMetadata(MDNode md) {
    return new MetadataMO(null, md);
  }

  public static MachineOperand createMCSymbol(MCSymbol sym) {
    return new MCSymbolMO(null, sym);
  }
}
