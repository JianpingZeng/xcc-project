package backend.codegen;

import backend.mc.MCSymbol;
import backend.target.TargetRegisterInfo;
import backend.value.BasicBlock;
import gnu.trove.list.array.TIntArrayList;
import tools.FormattedOutputStream;
import tools.Util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;

import static backend.support.AssemblyWriter.writeAsOperand;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class MachineBasicBlock {
  private LinkedList<MachineInstr> insts;
  private final BasicBlock bb;
  private TIntArrayList liveIns;

  /**
   * Indicates the number of this machine block in the machine function.
   */
  private int number;

  private ArrayList<MachineBasicBlock> predecessors;
  private ArrayList<MachineBasicBlock> successors;

  private MachineFunction parent;
  private int alignment;
  /**
   * Indicate that this basic block is reached via exception handler.
   */
  private boolean isLandingPad;
  /**
   * Indicate this basic block is the target of indirect branch.
   */
  private boolean addressTaken;

  public MachineBasicBlock(final BasicBlock bb) {
    insts = new LinkedList<>();
    this.bb = bb;
    number = -1;
    predecessors = new ArrayList<>();
    successors = new ArrayList<>();
    liveIns = new TIntArrayList();
    addressTaken = bb.isHasAddrTaken();
  }

  public boolean hasAddressTaken() {
    return addressTaken;
  }

  public boolean isLandingPad() {
    return isLandingPad;
  }

  public BasicBlock getBasicBlock() {
    return bb;
  }

  public int size() {
    return insts.size();
  }

  public boolean isEmpty() {
    return insts.isEmpty();
  }

  public LinkedList<MachineInstr> getInsts() {
    return insts;
  }

  public void addLast(MachineInstr instr) {
    insts.addLast(instr);
    addNodeToList(instr);
  }

  public MachineInstr getInstAt(int index) {
    return insts.get(index);
  }

  public int getIndexOf(MachineInstr mi) {
    return mi != null ? insts.indexOf(mi) : -1;
  }

  private void addNodeToList(MachineInstr instr) {
    Util.assertion(instr.getParent() == null, "machine instruction already have parent!");
    instr.setParent(this);

    // Add the instruction's register operands to their corresponding
    // use/def lists.
    instr.addRegOperandsToUseLists(parent.getMachineRegisterInfo());
  }

  public void insert(int itr, MachineInstr instr) {
    Util.assertion(itr >= 0 && itr <= size());
    if (itr == size())
      insts.add(instr);
    else
      insts.add(itr, instr);
    addNodeToList(instr);
  }

  public void insert(MachineInstr itr, MachineInstr instr) {
    int idx = insts.indexOf(itr);
    Util.assertion(idx >= 0 && idx < size());
    insert(idx, instr);
  }

  public void erase(int idx) {
    if (idx >= 0 && idx < size())
      insts.remove(idx);
  }

  public void erase(int start, int end) {
    for (int i = start; i < end; i++)
      insts.remove(i);
  }

  public void replace(int idx, MachineInstr newInstr) {
    Util.assertion(idx >= 0 && idx < size(), "idx out of range!");
    insts.remove(idx);
    insts.add(idx, newInstr);
  }

  public MachineInstr front() {
    return insts.getFirst();
  }

  public MachineInstr back() {
    return insts.getLast();
  }

  public ArrayList<MachineBasicBlock> getPredecessors() {
    return predecessors;
  }

  public ArrayList<MachineBasicBlock> getSuccessors() {
    return successors;
  }

  public void addSuccessor(MachineBasicBlock succ) {
    Util.assertion(succ != null, "Can not add a null succ into succ list");
    successors.add(succ);
    succ.addPredecessor(this);
  }

  public void removeSuccessor(MachineBasicBlock succ) {
    Util.assertion(successors.contains(succ), "The succ to be removed not contained in succ list");

    successors.remove(succ);
    succ.removePredecessor(this);
  }

  public void removeSuccessor(int idx) {
    Util.assertion(idx >= 0 && idx < getNumSuccessors());
    suxAt(idx).removePredecessor(this);
    successors.remove(idx);
  }

  public void addPredecessor(MachineBasicBlock pred) {
    Util.assertion(pred != null, "Can not add a null pred");
    predecessors.add(pred);
  }

  public void removePredecessor(MachineBasicBlock pred) {
    Util.assertion(predecessors.contains(pred), "The pred to be removed not contained in pred list");

    predecessors.remove(pred);
  }

  public void removePredecessor(int idx) {
    Util.assertion(idx >= 0 && idx < getNumPredecessors());
    predecessors.remove(idx);
  }

  public boolean isSuccessor(MachineBasicBlock mbb) {
    return successors.contains(mbb);
  }

  public void replaceSuccessor(MachineBasicBlock oldOne, MachineBasicBlock newOne) {
    if (!successors.contains(oldOne))
      return;
    int idx = successors.indexOf(oldOne);
    successors.set(idx, newOne);
  }

  public boolean isPredEmpty() {
    return predecessors.isEmpty();
  }

  public boolean succIsEmpty() {
    return successors.isEmpty();
  }

  public int getNumPredecessors() {
    return predecessors.size();
  }

  public int getNumSuccessors() {
    return successors.size();
  }

  public MachineBasicBlock predAt(int idx) {
    Util.assertion(idx >= 0 && idx < getNumPredecessors());
    return predecessors.get(idx);
  }

  public MachineBasicBlock suxAt(int idx) {
    Util.assertion(idx >= 0 && idx < getNumSuccessors());
    return successors.get(idx);
  }

  /**
   * Obtains the number of this machine block.
   *
   * @return
   */
  public int getNumber() {
    return number;
  }

  public void setNumber(int number) {
    this.number = number;
  }

  public void eraseFromParent() {
    Util.assertion(getParent() != null);
    getParent().erase(this);
  }

  public MachineFunction getParent() {
    return parent;
  }

  public void setParent(MachineFunction parent) {
    this.parent = parent;
  }

  public Iterator<MachineBasicBlock> predIterator() {
    return predecessors.iterator();
  }

  public Iterator<MachineBasicBlock> succIterator() {
    return successors.iterator();
  }

  public int getFirstTerminator() {
    int i = 0;
    int size = size();
    while (i < size && !getInstAt(i).getDesc().isTerminator()) ++i;
    return i;
  }

  /**
   * Remove the specified MachineInstr and remove the index to the position where
   * it located.
   *
   * @param miToDelete
   * @return
   */
  public int remove(MachineInstr miToDelete) {
    int index = insts.indexOf(miToDelete);
    insts.remove(miToDelete);
    miToDelete.setParent(null);
    return index;
  }

  public void remove(int indexToDel) {
    Util.assertion(indexToDel >= 0 && indexToDel < size());
    remove(getInstAt(indexToDel));
  }

  /**
   * Return true if the specified MBB will be emitted
   * immediately after this block, such that if this block exits by
   * falling through, control will transfer to the specified MBB. Note
   * that MBB need not be a successor at all, for example if this block
   * ends with an unconditional branch to some other block.
   * @param mbb
   * @return
   */
  public boolean isLayoutSuccessor(MachineBasicBlock mbb) {
    if (parent != null && mbb.getParent() != null && parent == mbb.getParent()) {
      ArrayList<MachineBasicBlock> mbbs = parent.getBasicBlocks();
      for (int i = 0, e = mbbs.size(); i < e; i++) {
        if (mbbs.get(i) == this) {
          if (i == e - 1) return false;
          return mbbs.get(i + 1) == mbb;
        }
      }
    }
    return false;
  }

  public void removeInstrAt(int indexToDel) {
    Util.assertion(indexToDel >= 0 && indexToDel < size());
    insts.remove(indexToDel);
  }

  public void addLiveIn(int reg) {
    liveIns.add(reg);
  }

  public TIntArrayList getLiveIns() {
    return liveIns;
  }

  public MachineInstr getLastInst() {
    Util.assertion(!insts.isEmpty());
    return insts.getLast();
  }

  public MachineInstr getFirstInst() {
    return insts.getFirst();
  }

  /**
   * Return true if this basic block has
   * exactly one predecessor and the control transfer mechanism between
   * the predecessor and this block is a fall-through.
   *
   * @return
   */
  public boolean isOnlyReachableByFallThrough() {
    if (isPredEmpty())
      return false;

    // If there is not exactly one predecessor, it can not be a fall through.
    if (getNumPredecessors() != 1)
      return false;

    // The predecessor has to be immediately before this block.
    MachineBasicBlock pred = predAt(0);
    if (!pred.isLayoutSuccessor(this))
      return false;

    // If the block is completely empty, then it definitely does fall through.
    if (pred.isEmpty())
      return true;

    // Otherwise, check the last instruction.
    MachineInstr lastInst = pred.getLastInst();
    return !lastInst.getDesc().isBarrier();
  }

  public int getAlignment() {
    return alignment;
  }

  public void setAlignment(int align) {
    alignment = align;
  }

  public void dump() {
    print(System.err);
  }

  public void print(PrintStream os) {
    print(os, new PrefixPrinter());
  }

  public void print(PrintStream os, PrefixPrinter prefix) {
    FormattedOutputStream out = new FormattedOutputStream(os);
    print(out, prefix);
  }

  public void print(FormattedOutputStream os, PrefixPrinter prefix) {
    MachineFunction mf = getParent();
    if (mf == null) {
      os.println("Can't print out MachineBasicBlock because parent MachineFunction is null");
      return;
    }

    BasicBlock bb = getBasicBlock();
    os.println();
    os.printf("BB#%d: ", getNumber());
    String comma = "";
    if (bb != null) {
      os.printf(",derived from LLVM BB ");
      writeAsOperand(os, bb, false, null);
      comma = ", ";
    }

    if (isLandingPad()) {
      os.printf("%sEH LANDING PAD", comma);
      comma = ", ";
    }
    if (hasAddressTaken()) {
      os.printf("%sADDRESS TAKEN", comma);
      comma = ", ";
    }

    TargetRegisterInfo tri = getParent().getTarget().getRegisterInfo();
    if (!liveIns.isEmpty()) {
      os.printf("    Live Ins:");
      for (int i = 0, e = liveIns.size(); i < e; i++)
        outputReg(os, liveIns.get(i), tri);
    }

    os.println();

    // Print the preds of this block according to the CFGs.
    if (predecessors != null && !predecessors.isEmpty()) {
      os.printf("    Predecessors according to CFG:");
      for (MachineBasicBlock pred : predecessors)
        os.printf(" BB#%d", pred.getNumber());
      os.println();
    }

    // Print each machine instruction.
    for (MachineInstr mi : insts) {
      prefix.print(os, mi).printf("\t");
      mi.print(os, getParent().getTarget());
    }

    // Print the sucessors of this block according to CFG
    if (!succIsEmpty()) {
      os.printf("    Successors according to CFG:");
      Iterator<MachineBasicBlock> itr = succIterator();
      while (itr.hasNext()) {
        MachineBasicBlock succ = itr.next();
        os.printf(" BB#%d", succ.getNumber());
      }
      os.println();
    }
  }

  private static void outputReg(FormattedOutputStream os, int reg) {
    outputReg(os, reg, null);
  }

  private static void outputReg(FormattedOutputStream os, int reg, TargetRegisterInfo tri) {
    if (reg == 0 || TargetRegisterInfo.isPhysicalRegister(reg)) {
      if (tri != null)
        os.printf(" %%%s", tri.getName(reg));
      else
        os.printf(" %%reg(%d)", reg);
    } else {
      os.printf(" %%reg%d", reg);
    }
  }

  public String getName() {
    BasicBlock bb = getBasicBlock();
    if (bb != null)
      return bb.getName();

    return "(null)";
  }

  public void transferSuccessor(MachineBasicBlock fromMBB) {
    if (Objects.equals(this, fromMBB))
      return;

    fromMBB.getSuccessors().forEach(this::addSuccessor);
    if (!fromMBB.getSuccessors().isEmpty())
      fromMBB.getSuccessors().clear();
  }

  public void transferSuccessorsAndUpdatePHIs(MachineBasicBlock fromMBB) {
    if (this == fromMBB)
      return;

    while (!fromMBB.succIsEmpty()) {
      MachineBasicBlock succ = fromMBB.suxAt(0);
      addSuccessor(succ);
      fromMBB.removeSuccessor(0);

      // fix up any PHI nodes in the successor.
      for (int i = 0, e = succ.size(); i < e && succ.getInstAt(i).isPHI(); i++) {
        MachineInstr mi = succ.getInstAt(i);
        for (int j = 2, sz = mi.getNumOperands(); j <= sz; j += 2) {
          if (mi.getOperand(j).getMBB().equals(fromMBB))
            mi.getOperand(j).setMBB(this);
        }
      }
    }
  }

  public MCSymbol getSymbol(MCSymbol.MCContext ctx) {
    MachineFunction mf = getParent();
    String name = mf.getTarget().getMCAsmInfo().getPrivateGlobalPrefix() +
        mf.getFunctionNumber() + "_" + getNumber();
    return ctx.getOrCreateSymbol(name);
  }

  public void splice(int insertAfter, MachineBasicBlock fromMBB, int start, int end) {
    Util.assertion(start <= end && start >= 0, "illegal instruction range in from MBB!");
    Util.assertion((size() == 0 && insertAfter == 0) ||
        (insertAfter >= 0 && insertAfter < size()), "illegal insertion position!" );

    if (fromMBB == null || start == end) return;
    Util.assertion(fromMBB != this, "can't splice the same block!");

    ArrayList<MachineInstr> toDelete = new ArrayList<>();
    for (int i = start; i < end; i++)
      toDelete.add(fromMBB.getInstAt(i));

    toDelete.forEach(MachineInstr::removeFromParent);
    int i = insertAfter;
    if (!isEmpty()) ++i;

    for (MachineInstr mi : toDelete) {
      insert(i++, mi);
    }
  }
}
