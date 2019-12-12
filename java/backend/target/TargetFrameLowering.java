package backend.target;

import backend.codegen.MachineBasicBlock;
import backend.codegen.MachineFrameInfo;
import backend.codegen.MachineFunction;
import tools.Pair;

import static backend.target.TargetOptions.DisableFPEliMLeaf;
import static backend.target.TargetOptions.DisableFPElim;

/**
 * This class defines an interface used for obtaining stack frame layout
 * information about the specified target machine.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public abstract class TargetFrameLowering {
  /**
   * This returns true if frame pointer elimination optimization should be
   * turned off for the given machine function.
   * @param mf
   * @return
   */
  protected static boolean disableFramePointerElim(MachineFunction mf) {
    if (DisableFPElim.value && !DisableFPEliMLeaf.value) {
      MachineFrameInfo mfi = mf.getFrameInfo();
      return mfi.hasCalls();
    }
    return DisableFPElim.value;
  }

  public enum StackDirection {
    /**
     * Adding to the stack increasing the stack address.
     */
    StackGrowUp,
    /**
     * Adding to the stack decreasing the stack address.
     */
    StackGrowDown
  }

  /**
   * The direction of stack growth, downward or upward?.
   */
  private StackDirection direction;
  /**
   * The alignemnt size of function.
   */
  private int stackAlignment;
  /**
   * The offset to the local area is the offset from the stack pointer on
   * function entry to the first location where function data (local variables,
   * spill locations) can be stored.
   * <p>
   * It is negative if stack grow downward. Otherwise it is positive.
   * </p>
   */
  private int localAreaOffset;

  public TargetFrameLowering(StackDirection dir, int stackAlign, int lao) {
    direction = dir;
    stackAlignment = stackAlign;
    localAreaOffset = lao;
  }

  public StackDirection getStackGrowDirection() {
    return direction;
  }

  public int getStackAlignment() {
    return stackAlignment;
  }

  public int getLocalAreaOffset() {
    return localAreaOffset;
  }

  /**
   * This method used for aligning stack frame depending on the specified target.
   *
   * @param unalignOffset
   * @param growUp
   * @param align
   */
  public void adjustAlignment(int unalignOffset, boolean growUp, int align) {

  }

  /**
   * This method returns a pointer to an array of pairs, that contains an entry
   * for each callee saved register that must be spilled to a particular stack
   * location if it is spilled.
   * <p>
   * Each entry in this array contains a &lt;register, offset&gt; pair, indicating
   * the fixed offset from the incoming stack pointer that each register
   * should be spilled at. If a register is not listed here, the code generator
   * is allowed to spill it anywhere it choose.
   *
   * @return
   */
  public Pair<Integer, Integer>[] getCalleeSavedSpillSlots() {
    return null;
  }

  /**
   * This method insert prologue code into the function.
   */
  public abstract void emitPrologue(MachineFunction mf);

  /**
   * This method insert epilogue code into the function.
   */
  public abstract void emitEpilogue(MachineFunction mf,
                                    MachineBasicBlock mbb);

  public abstract boolean hasFP(MachineFunction mf);

  public boolean hasReservedCallFrame(MachineFunction mf) {
    return !hasFP(mf);
  }
}
