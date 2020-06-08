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
  private Instruction inst;
  private boolean isInvoke;

  public CallSite() {
    inst = null;
  }

  public CallSite(CallInst ii) {
    inst = ii;
    isInvoke = false;
  }

  public CallSite(InvokeInst ii) {
    inst = ii;
    isInvoke = true;
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
    return isCall() ? getCallInst().getCalledValue() : getInvokeInst().getCalledValue();
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
    if (isCall())
      getCallInst().setCalledFunction(v);
    else
      getInvokeInst().setCalledFunction(v);
  }

  public Value getArgOperand(int idx) {
    return isCall() ? getCallInst().getArgOperand(idx) : getInvokeInst().getArgOperand(idx);
  }

  public void setArgOperand(int idx, Value newVal) {
    if (isCall())
      getCallInst().setArgOperand(idx, newVal);
    else
      getInvokeInst().setArgOperand(idx, newVal);
  }

  public int getNumOfOperands() {
    return isCall() ? getCallInst().getNumOfOperands() : getInvokeInst().getNumsOfArgs();
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

