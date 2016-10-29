package hir;

import static hir.Operator.Flags.*;
import lir.ci.LIRKind;
/**
 * This file defines a enumerator that isDeclScope all of operators which represents
 * as a integer in Module instruction.
 *
 * @author xlous.zeng
 */
public enum Operator
{
	None("illegal", -1, 0),
	// terminators operation.
	Ret("ret", 0, 0),
	Br("br", Ret.index + 1, 0),
	Goto("goto", Br.index + 1, 0),
	Invoke("invoke", Goto.index + 1, 0),
	Switch("switch", Invoke.index + 1, 0),

	// binary operator

	// addictive
	IAdd("iadd", Switch.index + 1, COMMUTATIVE | ASSOCIATIVE),
	LAdd("ladd", IAdd.index + 1, COMMUTATIVE | ASSOCIATIVE),
	FAdd("fadd", LAdd.index + 1, COMMUTATIVE | ASSOCIATIVE),
	DAdd("dadd", FAdd.index + 1, COMMUTATIVE | ASSOCIATIVE),

	// subtractive
	ISub("isub", DAdd.index + 1, ASSOCIATIVE),
	LSub("lsub", ISub.index + 1, ASSOCIATIVE),
	FSub("fsub", LSub.index + 1, ASSOCIATIVE),
	DSub("dsub", FSub.index + 1, ASSOCIATIVE),

	// multiple
	IMul("imul", DSub.index + 1, COMMUTATIVE | ASSOCIATIVE),
	LMul("lmul", IMul.index + 1, COMMUTATIVE | ASSOCIATIVE),
	FMul("fmul", LMul.index + 1, COMMUTATIVE | ASSOCIATIVE),
	DMul("dmul", FMul.index + 1, COMMUTATIVE | ASSOCIATIVE),

	// division
	IDiv("idiv", DMul.index + 1, ASSOCIATIVE),
	LDiv("ldiv", IDiv.index + 1, ASSOCIATIVE),
	FDiv("fdiv", LDiv.index + 1, ASSOCIATIVE),
	DDiv("ddiv", FDiv.index + 1, ASSOCIATIVE),

    // mod operation
    IMod("imod", DDiv.index + 1, ASSOCIATIVE),
    LMod("lmod", IMod.index + 1, ASSOCIATIVE),

	// bit-operation
	IAnd("iand", LMod.index + 1, ASSOCIATIVE | COMMUTATIVE),
	LAnd("land", IAnd.index + 1, ASSOCIATIVE | COMMUTATIVE),

	IOr("ior", LAnd.index + 1, ASSOCIATIVE | COMMUTATIVE),
	LOr("lor", IOr.index + 1, ASSOCIATIVE | COMMUTATIVE),

	IXor("ixor", LOr.index + 1, ASSOCIATIVE | COMMUTATIVE),
	LXor("lxor", IXor.index + 1, ASSOCIATIVE | COMMUTATIVE),

    // comparison operation
    SetEQ("SetEQ", LXor.index + 1, 0),
    SetNE("SetNE", SetEQ.index + 1, 0),
    SetLE("SetLE", SetNE.index + 1, 0),
    SetGE("SetGE", SetLE.index + 1, 0),
    SetLT("SetLT", SetGE.index + 1, 0),
    SetGT("SetGT", SetLT.index + 1, 0),

	// negative operation
	INeg("ineg", SetLT.index + 1, 0),
	LNeg("lneg", INeg.index + 1, 0),
	FNeg("fneg", LNeg.index + 1, 0),
	DNeg("dneg", FNeg.index + 1, 0),

	// shift operation
	IShr("ishr", DNeg.index + 1, 0),
	IUShr("iushr", IShr.index + 1, 0),
	LShr("lshr", IUShr.index + 1, 0),
	LUShr("lushr", LShr.index + 1, 0),

	IShl("ishl", LUShr.index + 1, 0),
	LShl("lshl", IShl.index  + 1, 0),

	// memory operation
	Alloca("alloca", LShl.index + 1, 0),
	Store("store", Alloca.index + 1, 0),
	Load("load", Store.index + 1, 0),

	// converts operation
	I2L("i2l", Load.index + 1, 0),
	I2F("i2f", I2L.index + 1, 0),
	I2D("i2d", I2F.index + 1, 0),
	L2I("l2i", I2D.index + 1, 0),
	L2F("l2f", L2I.index + 1, 0),
	L2D("l2d", L2F.index + 1, 0),
	F2I("f2i", L2D.index + 1, 0),
	F2L("f2l", F2I.index + 1, 0),
	F2D("f2d", F2L.index + 1, 0),
	D2I("d2i", F2D.index + 1, 0),
	D2L("d2l", D2I.index + 1, 0),
	D2F("d2f", D2L.index + 1, 0),
	I2B("i2b", D2F.index + 1, 0),
	I2C("i2c", I2B.index + 1, 0),
	I2S("i2s", I2C.index + 1, 0),

	// other operation
	Phi("phi", I2S.index + 1, 0);

	public final String opName;
	public final int index;
	private int flags;

	Operator(String name, int index, int flag)
	{
		this.opName = name;
		this.index = index;
		this.flags |= flag;
	}

	static class Flags
	{
		public static final int COMMUTATIVE = 0x1;
		public static final int ASSOCIATIVE = 0x2;
	}

	/**
	 * Determines if this operator is commutative.
	 * @return
	 */
	public boolean isCommutative()
	{
		return ((this.flags & 0xff) & COMMUTATIVE) != 0;
	}
	/**
	 * Check if this operator is an additive operation. ReturnInst true if
	 * condition is satisfied, otherwise return false.
	 * @return
	 */
	public boolean isAdd()
	{
		return this.index >= IAdd.index && this.index <= DAdd.index;
	}
    /**
     * Determines whether this operator is subtraction operator.
     * @return
     */
	public boolean isSub()
	{
		return index >= ISub.index && index <= DSub.index;
	}
	/**
	 * Determines whether this operator is Multiple operator.
	 * @return
	 */
	public boolean isMul()
    {
	    return index >= IMul.index && index<= DMul.index;
    }	
	
	public static Operator getMulByKind(LIRKind kind)
	{
		switch (kind)
        {
            case Int:
                return IMul;
            case Float:
                return FMul;
            case Long:
                return LMul;
            case Double:
                return DMul;
            default:
                return None;
        }
	}
	
	public static Operator getAddByKind(LIRKind kind)
	{
	    switch (kind)
        {
            case Double:
                return DAdd;
            case Float:
                return FAdd;
            case Long:
                return LAdd;
            case Int:
                return IAdd;
            default:
                return None;
        }
	}
}
