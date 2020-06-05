package backend.support;

import backend.type.Type;
import backend.value.Function;
import backend.value.Instruction;
import backend.value.Instruction.CallInst;
import backend.value.Instruction.InvokeInst;
import backend.value.Value;
import tools.Util;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class CallSite {
  // Returns the operand number of the first argument
  private static int ArgumentOffset;

  private Instruction inst;
  private boolean isInvoke;

  public CallSite() {
    inst = null;
  }

  public CallSite(CallInst ii) {
    inst = ii;
    isInvoke = false;
    ArgumentOffset = 1;
  }

  public CallSite(InvokeInst ii) {
    inst = ii;
    isInvoke = true;
    ArgumentOffset = 3;
  }

  public static CallSite get(Value v) {
    if (v instanceof CallInst)
      return new CallSite((CallInst) v);
    else if (v instanceof InvokeInst)
      return new CallSite((InvokeInst) v);
    return new CallSite();
  }

  public Type getType() {
    return inst.getType();
  }

  public Instruction getInstruction() {
    return inst;
  }

  public boolean isInvoke() { return isInvoke; }
  public boolean isCall() { return !isInvoke; }

  public CallInst getCallInst() {
    Util.assertion(isCall());
    return (CallInst) inst;
  }

  public InvokeInst getInvokeInst() {
    Util.assertion(isInvoke());
    return (InvokeInst) inst;
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
    int nums = isCall() ? getCallInst().getNumsOfArgs() : getInvokeInst().getNumOfOperands();
    Util.assertion(idx >= 0 && idx < nums,
        String.format("Argument #%d out of range!", idx));
    return inst.operand(ArgumentOffset + idx);
  }

  public void setArgument(int idx, Value newVal) {
    Util.assertion(inst != null, "Not a call inst");
    int nums = isCall() ? getCallInst().getNumsOfArgs() : getInvokeInst().getNumsOfArgs();
    Util.assertion(idx >= 0 && idx + ArgumentOffset < nums, "Argument # out of range!");
    inst.setOperand(idx + ArgumentOffset, newVal, inst);
  }

  public int getNumOfArguments() {
    return inst.getNumOfOperands() - ArgumentOffset;
  }

  public boolean doesNotAccessMemory() {
    if (isCall())
      return getCallInst().doesNotAccessMemory();
    else
      return getInvokeInst().doesNotAccessMemory();
  }

  public void setDoesNotAccessMemory(boolean val) {
    if (isCall())
      getCallInst().setDoesNotAccessMemory(val);
    else
      getInvokeInst().setDoesNotAccessMemory(val);
  }

  public boolean onlyReadsMemory() {
    if (isCall())
      return getCallInst().onlyReadsMemory();
    else
      return getInvokeInst().onlyReadsMemory();
  }

  public void setOnlyReadsMemory(boolean val) {
    if (isCall())
      getCallInst().setOnlyReadsMemory(val);
    else
      getInvokeInst().setOnlyReadsMemory(val);
  }

  public boolean doesNotReturn() {
    if (isCall())
      return getCallInst().doesNotReturn();
    else
      return getCallInst().doesNotReturn();
  }

  public void setDoesNotReturn(boolean val) {
    if (isCall())
      getCallInst().setDoesNotReturn(val);
    else
      getInvokeInst().setDoesNotReturn(val);
  }

  public boolean doesNotThrow() {
    if (isCall())
      return getCallInst().doesNotThrow();
    else
      return getInvokeInst().doesNotThrow();
  }

  public void setDoesNotThrow(boolean val) {
    if (isCall())
      getCallInst().setDoesNotThrow(val);
    else
      getInvokeInst().setDoesNotThrow(val);
  }

  public CallingConv getCallingConv() {
    if (isCall())
      return getCallInst().getCallingConv();
    else
      return getInvokeInst().getCallingConv();
  }

  public boolean paramHasAttr(int idx, int attr) {
    if (isCall())
      return getCallInst().paramHasAttr(idx, attr);
    else
      return getInvokeInst().paramHasAttr(idx, attr);
  }

  public int paramAlignment(int idx) {
    if (isCall())
      return getCallInst().getParamAlignment(idx);
    else
      return getInvokeInst().getParamAlignment(idx);
  }

  public AttrList getAttributes() {
    if (isCall())
      return getCallInst().getAttributes();
    else
      return getInvokeInst().getAttributes();
  }

  public void setAttributes(AttrList attrs) {
    if (isCall())
      getCallInst().setAttributes(attrs);
    else
      getInvokeInst().setAttributes(attrs);
  }

  public void setCallingConv(CallingConv cc) {
    if (isCall())
      getCallInst().setCallingConv(cc);
    else
      getInvokeInst().setCallingConv(cc);
  }

  public boolean isTailCall() {
    return isCall() && getCallInst().isTailCall();
  }

  public void setTailCall(boolean b) {
    if (isCall())
      getCallInst().setTailCall(b);
  }
}

