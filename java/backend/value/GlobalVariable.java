package backend.value;

import backend.type.PointerType;
import backend.type.Type;
import tools.Util;

/**
 * @author Jianping Zeng
 */
public class GlobalVariable extends GlobalValue {
  /**
   * Is this a global constant.
   */
  private boolean isConstantGlobal;
  private boolean isThreadLocal;

  /**
   * Constructs a new instruction representing the specified constant.
   */
  public GlobalVariable(Module m,
                        Type ty,
                        boolean isConstant,
                        LinkageType linkage,
                        Constant init,
                        String name,
                        GlobalVariable before,
                        int addressSpace) {
    super(PointerType.get(ty, addressSpace),
        ValueKind.GlobalVariableVal,
        linkage,
        name);
    isConstantGlobal = isConstant;
    if (init != null) {
      reserve(1);
      Util.assertion(init.getType() == ty, "Initializer should be the same type as the GlobalVariable!");
      setOperand(0, new Use(init, this));
    }
    if (before != null) {
      int beforeIdx = before.getParent().getGlobalVariableList().indexOf(before);
      before.getParent().addGlobalVariable(beforeIdx + 1, this);
    } else {
      m.addGlobalVariable(this);
    }
  }

  /**
   * This method unlinks 'this' from the containing module
   * and deletes it.
   */
  @Override
  public void eraseFromParent() {
    parent.getGlobalVariableList().remove(this);
  }

  @Override
  public boolean isNullValue() {
    return false;
  }

  @Override
  public void replaceUsesOfWithOnConstant(Value from, Value to, Use u) {
    Util.assertion(getNumOfOperands() == 1, "Attempt to replace uses of Constants on a GVar with no initializer");
    Util.assertion(operand(0).equals(from), "Attempt to replace wrong constant initializer in GVar");
    Util.assertion(to instanceof Constant, "Attempt to replace GVar initializer with non-constant");
    setOperand(0, to);
  }

  public void setInitializer(Constant init) {
    if (operandList == null)
      reserve(1);
    setOperand(0, new Use(init, this));
  }

  public boolean isConstant() {
    return isConstantGlobal;
  }

  public void setConstant(boolean c) {
    isConstantGlobal = c;
  }

  public boolean hasInitializer() {
    return !isDeclaration();
  }

  @Override
  public GlobalVariable clone() {
    return (GlobalVariable) super.clone();
  }

  public boolean isThreadLocal() {
    return isThreadLocal;
  }

  public Constant getInitializer() {
    Util.assertion(hasInitializer());
    return (Constant) operand(0);
  }

  public void setThreadLocal(boolean threadLocal) {
    this.isThreadLocal = threadLocal;
  }

  public boolean hasDefinitiveInitializer() {
    return hasInitializer() &&
        !mayBeOverridden();
  }

  public boolean mayBeOverridden() {
    LinkageType link = getLinkage();
    return link == LinkageType.CommonLinkage;
  }
}
