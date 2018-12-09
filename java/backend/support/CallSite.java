package backend.support;

import backend.type.Type;
import backend.value.Function;
import backend.value.Instruction.CallInst;
import backend.value.Value;
import com.sun.javafx.binding.StringFormatter;
import tools.Util;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class CallSite {
  // Returns the operand number of the first argument
  private static final int ArgumentOffset = 1;

  private CallInst inst;

  public CallSite() {
    inst = null;
  }

  public CallSite(CallInst ii) {
    inst = ii;
  }

  public static CallSite get(Value v) {
    if (v instanceof CallInst)
      return new CallSite((CallInst) v);
    return new CallSite();
  }

  public Type getType() {
    return inst.getType();
  }

  public CallInst getInstruction() {
    return inst;
  }

  /**
   * Return the caller function for this call site
   *
   * @return
   */
  public Function getCaller() {
    return inst.getParent().getParent();
  }

  /**
   * Return the pointer to function that is being called.
   *
   * @return
   */
  public Value getCalledValue() {
    Util.assertion(inst != null, "Not a call instruction!");
    return inst.operand(0);
  }

  /**
   * Return the function being called if this is a direct
   * call, otherwise return null (if it's an indirect call).
   *
   * @return
   */
  public Function getCalledFunction() {
    Value v = getCalledValue();
    if (v instanceof Function)
      return (Function) v;
    return null;
  }

  public void setCalledFunction(Value v) {
    Util.assertion(inst != null, "Not a call inst");
    inst.setOperand(0, v, inst);
  }

  public Value getArgument(int idx) {
    Util.assertion(idx >= 0 && idx < inst.getNumsOfArgs(), StringFormatter.format("Argument #%d out of range!", idx).getValue());

    return inst.operand(ArgumentOffset + idx);
  }

  public void setArgument(int idx, Value newVal) {
    Util.assertion(inst != null, "Not a call inst");
    Util.assertion(idx + ArgumentOffset >= 1 && idx + 1 < inst.getNumsOfArgs(), "Argument # out of range!");

    inst.setOperand(idx + ArgumentOffset, newVal, inst);
  }

  public int getNumOfArguments() {
    return inst.getNumOfOperands() - ArgumentOffset;
  }

  public boolean doesNotAccessMemory() {
    return getInstruction().doesNotAccessMemory();
  }

  public void setDoesNotAccessMemory(boolean val) {
    getInstruction().setDoesNotAccessMemory(val);
  }

  public boolean onlyReadsMemory() {
    return getInstruction().onlyReadsMemory();
  }

  public void setOnlyReadsMemory(boolean val) {
    getInstruction().setOnlyReadsMemory(val);
  }

  public boolean doesNotReturn() {
    return getInstruction().doesNotReturn();
  }

  public void setDoesNotReturn(boolean val) {
    getInstruction().setDoesNotReturn(val);
  }

  public boolean doesNotThrow() {
    return getInstruction().doesNotThrow();
  }

  public void setDoesNotThrow(boolean val) {
    getInstruction().setDoesNotThrow(val);
  }

  public CallingConv getCallingConv() {
    return inst.getCallingConv();
  }

  public boolean paramHasAttr(int idx, int attr) {
    return inst.paramHasAttr(idx, attr);
  }

  public int paramAlignment(int idx) {
    return inst.getParamAlignment(idx);
  }

  public AttrList getAttributes() {
    return inst.getAttributes();
  }

  public void setAttributes(AttrList attrs) {
    getInstruction().setAttributes(attrs);
  }
}

