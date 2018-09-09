package backend.codegen;

import backend.target.TargetData;
import backend.value.Constant;
import tools.Util;

import java.io.PrintStream;
import java.util.ArrayList;

/**
 * Keeps track of information for constant spilled into stack frame.
 *
 * @author Jianping Zeng
 * @version 0.1
 */
public final class MachineConstantPool {
  private ArrayList<MachineConstantPoolEntry> constants;
  private TargetData td;
  private int poolAlignment;

  public MachineConstantPool(TargetData td) {
    constants = new ArrayList<>();
    this.td = td;
    poolAlignment = 1;
  }

  /**
   * Creates a new entry in constant pool and returns it's index
   * or a existing one if there is existing.
   *
   * @param c
   * @return
   */
  public int getConstantPoolIndex(Constant c, int align) {
    Util.assertion(align != 0, "Alignment must be specified.");
    if (align > poolAlignment)
      poolAlignment = align;

    for (int i = 0, e = constants.size(); i < e; i++) {
      MachineConstantPoolEntry cc = constants.get(i);
      if (cc.val.equals(c) && (cc.getAlignment() & (align - 1)) == 0)
        return i;
    }
    constants.add(new MachineConstantPoolEntry(c, align));
    return constants.size() - 1;
  }

  /**
   * Creates a new entry in constant pool and returns it's index
   * or a existing one if there is existing.
   *
   * @param val
   * @return
   */
  public int getConstantPoolIndex(MachineConstantPoolValue val, int align) {
    Util.assertion(align != 0, "Alignment must be specified.");
    if (align > poolAlignment)
      poolAlignment = align;

    int idx = val.getExistingMachineCPValue(this, align);
    if (idx != -1)
      return idx;

    constants.add(new MachineConstantPoolEntry(val, align));
    return constants.size() - 1;
  }

  public ArrayList<MachineConstantPoolEntry> getConstants() {
    return constants;
  }

  public int getContantPoolAlignment() {
    return poolAlignment;
  }

  public boolean isEmpty() {
    return constants.isEmpty();
  }

  public void dump() {
    print(System.err);
  }

  public void print(PrintStream os) {
    if (constants == null || constants.isEmpty())
      return;

    for (int i = 0, e = constants.size(); i < e; i++) {
      os.printf(" <cp#%d> is", i);
      if (constants.get(i).isMachineConstantPoolEntry())
        constants.get(i).getValueAsCPV().print(os);
      else {
        constants.get(i).getValueAsConstant().print(os);
      }
      os.printf(" , alignment=%d\n", constants.get(i).getAlignment());
    }
  }
}
