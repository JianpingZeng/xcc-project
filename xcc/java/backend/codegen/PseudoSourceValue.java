package backend.codegen;

import backend.support.LLVMContext;
import backend.type.Type;
import backend.value.Value;
import gnu.trove.map.hash.TIntObjectHashMap;
import tools.FormattedOutputStream;
import tools.Util;

import java.io.PrintStream;

import static backend.value.ValueKind.PseudoSourceValueVal;

/**
 * Special value supplied for machine level alias
 * analysis. It indicates that the a memory access references the functions
 * stack frame (e.g., a spill slot), below the stack frame (e.g., argument
 * space), or constant pool.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public class PseudoSourceValue extends Value {
  private static TIntObjectHashMap<PseudoSourceValue> fsValueMap = new TIntObjectHashMap<>();

  public PseudoSourceValue() {
    super(Type.getInt8Ty(LLVMContext.getGlobalContext()), PseudoSourceValueVal);
  }

  @Override
  public void dump() {
    print(System.err);
    System.err.println();
  }

  @Override
  public void print(PrintStream os) {
    FormattedOutputStream out = new FormattedOutputStream(os);
    print(out);
  }

  public static final String PSVNames[] =
      {
          "Stack",
          "GOT",
          "JumpTable",
          "ConstantPool"
      };

  private static final PseudoSourceValue[] PSValues =
      {
          new PseudoSourceValue(),
          new PseudoSourceValue(),
          new PseudoSourceValue(),
          new PseudoSourceValue()
      };

  @Override
  public void print(FormattedOutputStream os) {
    if (equals(getStack()))
      os.print(PSVNames[0]);
    else if (equals(getGOT()))
      os.print(PSVNames[1]);
    else if (equals(getJumpTable()))
      os.print(PSVNames[2]);
    else if (equals(getConstantPool()))
      os.print(PSVNames[3]);
    else
      Util.assertion(false, "Invalid PseudoSourceValue");

  }

  public boolean isConstant(MachineFrameInfo mfi) {
    if (this == getStack())
      return false;
    if (this == getGOT() || this == getConstantPool() ||
        this == getJumpTable())
      return true;
    Util.shouldNotReachHere("Unknown PseudoSourceValue");
    return false;
  }

  /**
   * A pseudo source value referencing a fixed stack frame entry,
   * e.g., a spill slot.
   *
   * @param frameIndex
   * @return
   */
  public static PseudoSourceValue getFixedStack(int frameIndex) {
    PseudoSourceValue val;
    if (fsValueMap.containsKey(frameIndex)) {
      val = fsValueMap.get(frameIndex);
    } else {
      val = new FixedStackPseudoSourceValue(frameIndex);
      fsValueMap.put(frameIndex, val);
    }
    return val;
  }

  /**
   * A source value referencing the area below the stack frame of a function,
   * e.g., the argument space.
   *
   * @return
   */
  public static PseudoSourceValue getStack() {
    return PSValues[0];
  }

  /**
   * A source value referencing the global offset table (or something the
   * like).
   *
   * @return
   */
  public static PseudoSourceValue getGOT() {
    return PSValues[1];
  }

  /**
   * A reference to Constant pool.
   *
   * @return
   */
  public static PseudoSourceValue getConstantPool() {
    return PSValues[2];
  }

  /**
   * A reference to Jump table.
   *
   * @return
   */
  public static PseudoSourceValue getJumpTable() {
    return PSValues[3];
  }

}
