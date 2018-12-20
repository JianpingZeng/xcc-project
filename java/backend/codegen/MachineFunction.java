package backend.codegen;

import backend.mc.MCAsmInfo;
import backend.mc.MCSymbol;
import backend.mc.MCRegisterClass;
import backend.target.TargetMachine;
import backend.target.TargetRegisterInfo;
import backend.target.TargetSubtarget;
import backend.value.BasicBlock;
import backend.value.Function;
import tools.Pair;
import tools.Util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class MachineFunction {
  private Function fn;
  private TargetMachine target;
  /**
   * A list containing all machine basic block.
   */
  private ArrayList<MachineBasicBlock> mbbNumber;
  /**
   * Used to keep track of stack frame information about target.
   */
  private MachineFrameInfo frameInfo;
  /**
   * Keeping track of diagMapping from SSA values to registers.
   */
  private MachineRegisterInfo regInfo;
  /**
   * Keep track of constants to be spilled into stack slot.
   */
  private MachineConstantPool constantPool;

  /**
   * This array keeps track of the defined machine operand with the spceified
   * machine register.
   */
  private MachineOperand[] phyRegDefUseList;

  /**
   * Used to keep track of target-specific per-machine function information for
   * the target implementation.
   */
  MachineFunctionInfo mfInfo;
  private int alignment;
  private MachineJumpTableInfo jumpTableInfo;
  private int functionNumber;

  public MachineFunction(Function fn, TargetMachine tm, int fnNumber) {
    this.fn = fn;
    target = tm;
    mbbNumber = new ArrayList<>();
    frameInfo = new MachineFrameInfo(tm.getFrameLowering(), tm.getRegisterInfo());
    regInfo = new MachineRegisterInfo(tm.getRegisterInfo());
    constantPool = new MachineConstantPool(tm.getTargetData());
    phyRegDefUseList = new MachineOperand[tm.getRegisterInfo().getNumRegs()];

    boolean isPIC = tm.getRelocationModel() == TargetMachine.RelocModel.PIC_;
    alignment = tm.getTargetLowering().getFunctionAlignment(fn);
    int entrySize = isPIC ? 4 : tm.getTargetData().getPointerSize();
    jumpTableInfo = new MachineJumpTableInfo(entrySize, alignment);
    functionNumber = fnNumber;

    // associate this machine function with HIR function.
    fn.setMachineFunc(this);
  }

  public Function getFunction() {
    return fn;
  }

  public TargetMachine getTarget() {
    return target;
  }

  public MachineBasicBlock getEntryBlock() {
    return mbbNumber.get(0);
  }

  public MachineFrameInfo getFrameInfo() {
    return frameInfo;
  }

  public MachineRegisterInfo getMachineRegisterInfo() {
    return regInfo;
  }

  public void clearSSARegMap() {
    regInfo.clear();
  }

  public MachineConstantPool getConstantPool() {
    return constantPool;
  }

  public ArrayList<MachineBasicBlock> getBasicBlocks() {
    return mbbNumber;
  }

  public void setBasicBlocks(List<MachineBasicBlock> mbbs) {
    mbbNumber.clear();
    if (mbbs == null || mbbs.isEmpty())
      return;
    mbbNumber.addAll(mbbs);
    for (int i = 0, e = mbbNumber.size(); i < e; i++)
      mbbNumber.get(i).setNumber(i);
  }

  public void erase(MachineBasicBlock mbb) {
    mbbNumber.remove(mbb);
  }

  /**
   * returns the number of allocated blocks ID.
   *
   * @return
   */
  public int getNumBlocks() {
    return mbbNumber.size();
  }

  public MachineBasicBlock getMBBAt(int blockNo) {
    Util.assertion(blockNo >= 0 && blockNo < mbbNumber.size());
    return mbbNumber.get(blockNo);
  }

  public MachineBasicBlock removeMBBAt(int blockNo) {
    Util.assertion(blockNo >= 0 && blockNo < mbbNumber.size());
    return mbbNumber.remove(blockNo);
  }

  public int getIndexOfMBB(MachineBasicBlock mbb) {
    Util.assertion(mbb != null);
    return mbbNumber.indexOf(mbb);
  }

  public boolean isEmpty() {
    return mbbNumber.isEmpty();
  }

  public void addMBBNumbering(MachineBasicBlock mbb) {
    mbbNumber.add(mbb);
    mbb.setNumber(mbbNumber.size() - 1);
  }

  /**
   * Performs a phase for re-numbering all of blocks in this function.
   */
  public void renumberBlocks() {
    if (isEmpty()) {
      mbbNumber.clear();
      return;
    }

    renumberBlocks(getEntryBlock());
  }

  private void renumberBlocks(MachineBasicBlock start) {
    int blockNo = 0;
    int i = mbbNumber.indexOf(start);
    if (i != 0)
      blockNo = mbbNumber.get(i - 1).getNumber() + 1;

    for (; i < mbbNumber.size(); i++, blockNo++) {
      mbbNumber.get(i).setNumber(blockNo);
    }

    Util.assertion(blockNo <= mbbNumber.size(), "Mismatch!");
    mbbNumber.ensureCapacity(blockNo);
  }

  public MachineFunctionInfo getInfo() {
    if (mfInfo == null)
      mfInfo = target.getTargetLowering().createMachineFunctionInfo(this);
    return mfInfo;
  }

  public int getAlignment() {
    return alignment;
  }

  /**
   * Set the alignment (log2, not bytes) of the function.
   * @param align
   */
  public void setAlignment(int align) {
    alignment = align;
  }

  public int size() {
    return getBasicBlocks().size();
  }

  public MachineBasicBlock createMachineBasicBlock(BasicBlock bb) {
    MachineBasicBlock mbb = new MachineBasicBlock(bb);
    mbb.setParent(this);
    return mbb;
  }

  public void print(PrintStream os) {
    print(os, new PrefixPrinter());
  }

  public void dump() {
    print(System.err);
  }

  public void print(PrintStream os, PrefixPrinter prefix) {
    os.printf("# Machine code for %s():%n", fn.getName());

    //Print frame information.
    frameInfo.print(this, os);

    // Print JumpTable information.
    jumpTableInfo.print(os);

    // Print Jump table information.
    // Print constant pool.
    constantPool.print(os);

    TargetRegisterInfo tri = target.getRegisterInfo();
    if (regInfo != null && !regInfo.isLiveInEmpty()) {
      os.printf("Live Ins:");
      for (Pair<Integer, Integer> entry : regInfo.getLiveIns()) {
        if (tri != null)
          os.printf(" %s", tri.getName(entry.first));
        else
          os.printf(" Reg #%d", entry.first);

        if (entry.second != 0)
          os.printf(" in VR#%d ", entry.second);
      }
      os.println();
    }

    if (regInfo != null && !regInfo.isLiveOutEmpty()) {
      os.printf("Live Outs:");
      for (Pair<Integer, Integer> entry : regInfo.getLiveIns()) {
        if (tri != null)
          os.printf(" %s", tri.getName(entry.first));
        else
          os.printf(" Reg #%d", entry.first);
      }
      os.println();
    }

    //
    for (MachineBasicBlock mbb : mbbNumber) {
      prefix.print(os, mbb);
      mbb.print(os, prefix);
    }
    os.printf("%n# End machine code for %s().%n%n", fn.getName());
  }

  /**
   * Add the specified physical register as a live-in values. Also create a
   * virtual register associated with this register.
   *
   * @param locReg
   * @param rc
   * @return
   */
  public int addLiveIn(int locReg, MCRegisterClass rc) {
    Util.assertion(rc.contains(locReg), "Not the current regclass!");
    int virReg = getMachineRegisterInfo().createVirtualRegister(rc);
    getMachineRegisterInfo().addLiveIn(locReg, virReg);
    return virReg;
  }

  public void deleteMachineInstr(MachineInstr mi) {
    mi.clearMemOperands();
  }

  public void insert(int insertPos, MachineBasicBlock mbb) {
    Util.assertion(insertPos >= 0 && insertPos < mbbNumber.size());
    mbbNumber.add(insertPos, mbb);
  }

  public MachineJumpTableInfo getJumpTableInfo() {
    return jumpTableInfo;
  }

  public void setJumpTableInfo(MachineJumpTableInfo jti) {
    this.jumpTableInfo = jti;
  }

  public TargetSubtarget getSubtarget() {
    return getTarget().getSubtarget();
  }

  public int getFunctionNumber() {
    return functionNumber;
  }

  public MCSymbol getJTISymbol(int jtiId, MCSymbol.MCContext outContext, boolean isLinkerPrivate) {
    Util.assertion(jumpTableInfo != null, "no jump tables");
    Util.assertion(jtiId < jumpTableInfo.getJumpTables().size());
    MCAsmInfo mai = getTarget().getMCAsmInfo();
    String prefix = isLinkerPrivate ?
        mai.getLinkerPrivateGlobalPrefix() :
        mai.getPrivateGlobalPrefix();
    String name = prefix + "JTI" + getFunctionNumber() + "_" + jtiId;
    return outContext.getOrCreateSymbol(name);
  }
}
