package backend.value;

import backend.type.Type;
import tools.FltSemantics;
import tools.Util;

import java.util.ArrayList;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public abstract class User extends Value {
  /**
   * Mainly for register allocation.
   */
  public int id;

  /**
   * This array with element of jlang.type Value represents all operands.
   */
  protected Use[] operandList;
  protected int numOps;

  public User(Type ty, int valueKind) {
    super(ty, valueKind);
    id = -1;
    operandList = null;
    numOps = 0;
  }

  protected void reserve(int numOperands) {
    Util.assertion(numOperands > 0);
    numOps = numOperands;
    if (operandList == null)
      operandList = new Use[numOperands];
  }

  /**
   * Obtains a reference to the operand at index position.
   *
   * @param index the position indexed to TargetData element.
   * @return the index-th operand.
   */
  public Value operand(int index) {
    Util.assertion(index >= 0 && index < getNumOfOperands(), "index out of range");
    return operandList[index] != null ? operandList[index].getValue() : null;
  }

  public void setOperand(int index, Value val, User user) {
    Util.assertion(index >= 0 && index < getNumOfOperands(), "index out of range");
    setOperand(index, new Use(val, user));
  }

  /**
   * set element at specified position with {@code use}
   *
   * @param index
   * @param use
   */
  public void setOperand(int index, Use use) {
    Util.assertion(use != null, "can't set operand as null");
    Util.assertion(operandList != null, "should initialize operands list before update operand");
    Util.assertion(index >= 0 && index < getNumOfOperands(), "index out of range");
    /*if (index >= getNumOfOperands()) {
      // allocate extra 10 elements.
      int oldNumOps = numOps;
      numOps = index+10;
      Use[] newArray = new Use[numOps];
      System.arraycopy(operandList, 0, newArray, 0, oldNumOps);
      operandList = newArray;
    }*/
    FltSemantics fltSemantics = null;
    if (use.getValue() instanceof ConstantFP)
      fltSemantics = ((ConstantFP)use.getValue()).getValueAPF().getSemantics();

    operandList[index] = use;
    use = operandList[index];
    if (fltSemantics != null)
      ((ConstantFP)use.getValue()).getValueAPF().setSemantics(fltSemantics);
  }

  public void setOperand(int index, Value opVal) {
    Util.assertion(index >= 0 && index < getNumOfOperands(), "index out of range");
    operandList[index].setValue(opVal);
  }

  public Use getOperand(int index) {
    Util.assertion(index >= 0 && index < getNumOfOperands(), "index out of range");
    return operandList[index];
  }

  public void removeOperand(int index) {
    Util.assertion(index >= 0 && index < getNumOfOperands(), "index out of range");
    for (int i = index+1; i < getNumOfOperands() - 1; i++)
      operandList[i] = operandList[i+1];
    numOps -= 1;
  }

  /**
   * obtains the number of reservedOperands of this instruction.
   *
   * @return
   */
  public int getNumOfOperands() {
    return numOps;
  }

  /**
   * This method is in charge of dropping all objects that this user refers to.
   */
  public void dropAllReferences() {
    usesList.clear();
  }

  /**
   * Replace all references to the {@code from} with reference to the {@code to}.
   *
   * @param from
   * @param to
   */
  public void replaceUsesOfWith(Value from, Value to) {
    if (from == to) return;
    Util.assertion(!(this instanceof Constant) || (this instanceof GlobalValue),
        "Can't call User.replaceUsesOfWith() on a constant");
    for (int i = 0, e = getNumOfOperands(); i < e; i++) {
      if (operand(i) == from) {
        setOperand(i, to, this);
      }
    }
  }
}
