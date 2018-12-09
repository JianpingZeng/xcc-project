package backend.analysis;

import backend.value.Instruction;
import backend.value.Value;

/**
 * Keeps track of one use of a strided induction variable, where the stride
 * is stored externally.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public final class IVStrideUses {
  /**
   * A pointer to the IVUsersOfOneStride that owns this IVStrideUse.
   */
  private IVUsersOfOneStride parent;
  /**
   * The offset to add to the base induction expression.
   */
  private SCEV offset;
  /**
   * The user instruction that uses the {@linkplain #operandValToReplace}.
   */
  private Instruction user;
  /**
   * The Value of the operandValToReplace in the user instruction
   * that this IVStrideUse is representing.
   */
  private Value operandValToReplace;
  /**
   * True indicates that this should use post-incremented version of
   * this IV, not pre-incremented.
   */
  private boolean isUseOfPostIncrementedValue;

  public IVStrideUses(IVUsersOfOneStride parent, SCEV offset,
                      Instruction user, Value operand) {
    this.parent = parent;
    this.offset = offset;
    this.user = user;
    this.operandValToReplace = operand;
  }

  public IVUsersOfOneStride getParent() {
    return parent;
  }

  public SCEV getOffset() {
    return offset;
  }

  public void setOffset(SCEV offset) {
    this.offset = offset;
  }

  public Instruction getUser() {
    return user;
  }

  public void setUser(Instruction user) {
    this.user = user;
  }

  public Value getOperandValToReplace() {
    return operandValToReplace;
  }

  public void setOperandValToReplace(Value operandValToReplace) {
    this.operandValToReplace = operandValToReplace;
  }

  public boolean isUseOfPostIncrementedValue() {
    return this.isUseOfPostIncrementedValue;
  }

  public void setUseOfPostIncrementedValue(boolean useOfPostIncrementedValue) {
    this.isUseOfPostIncrementedValue = useOfPostIncrementedValue;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (this == obj) return true;

    if (getClass() != obj.getClass())
      return false;
    IVStrideUses ins = (IVStrideUses) obj;
    return parent.equals(ins.parent) && offset.equals(ins.offset)
        && user.equals(ins.user) && operandValToReplace
        .equals(ins.operandValToReplace);
  }

  @Override
  public int hashCode() {
    return (parent.hashCode() << 23) ^ (offset.hashCode() << 17)
        ^ (user.hashCode() << 11) ^ (operandValToReplace.hashCode() << 5);
  }
}
