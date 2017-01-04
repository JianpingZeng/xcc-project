package backend.hir;

import backend.type.Type;
import backend.value.Function;
import backend.value.Instruction.CallInst;
import backend.value.Value;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class CallSite
{
	private CallInst inst;

	public CallSite() {inst = null;}
	public CallSite(CallInst ii) {inst = ii;}

	public static CallSite get(Value v)
	{
		if (v instanceof CallInst)
			return new CallSite((CallInst)v);
		return new CallSite();
	}

	public Type getType() { return inst.getType();}

	public CallInst getInstruction() {return inst;}

	/**
	 * Return the caller function for this call site
	 * @return
	 */
	public Function getCaller() {return inst.getParent().getParent();}

	/**
	 * Return the pointer to function that is being called.
	 * @return
	 */
	public Value getCallededValue()
	{
		assert inst != null:"Not a call instruction!";
		return inst.operand(0);
	}

	/**
	 * Return the function being called if this is a direct
	 * call, otherwise return null (if it's an indirect call).
	 * @return
	 */
	public Function getCalledFunction()
	{
		Value v = getCallededValue();
		if (v instanceof Function)
			return (Function)v;
		return null;
	}

	public void setCalledFunction(Value v)
	{
		assert inst!= null:"Not a call inst";
		inst.setOperand(0, v, inst);
	}

	public Value getArgument(int idx)
	{
		assert idx + ArgumentOffset >=1 && idx + 1 < inst.getNumsOfArgs()
				:"Argument # out of range!";
		return inst.operand(ArgumentOffset+idx);
	}

	public void setArgument(int idx, Value newVal)
	{
		assert inst!= null:"Not a call inst";
		assert idx + ArgumentOffset >=1 && idx + 1 < inst.getNumsOfArgs()
				:"Argument # out of range!";
		inst.setOperand(idx + ArgumentOffset, newVal, inst);
	}

	// Returns the operand number of the first argument
	private final int ArgumentOffset = 1;
}

