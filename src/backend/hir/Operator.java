package backend.hir;

import backend.lir.ci.LIRKind;

import static backend.hir.Operator.Flags.ASSOCIATIVE;
import static backend.hir.Operator.Flags.COMMUTATIVE;
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
	Call("call", Goto.index + 1, 0),
	Switch("switch", Call.index + 1, 0),

	// binary operator

	// add
	Add("add", Switch.index + 1, COMMUTATIVE | ASSOCIATIVE),
    FAdd("fadd", Add.index + 1, COMMUTATIVE | ASSOCIATIVE),

	// subtractive
	Sub("isub", FAdd.index + 1, ASSOCIATIVE),
	FSub("fsub", Sub.index + 1, ASSOCIATIVE),

	// multiple
	Mul("imul", FSub.index + 1, COMMUTATIVE | ASSOCIATIVE),
	FMul("fmul", Mul.index + 1, COMMUTATIVE | ASSOCIATIVE),

	// division
	UDiv("idiv", FMul.index + 1, ASSOCIATIVE),
    SDiv("sdiv", UDiv.index + 1, ASSOCIATIVE),
	FDiv("fdiv", UDiv.index + 1, ASSOCIATIVE),

    // mod operation
    URem("URem", FDiv.index + 1, ASSOCIATIVE),
    SRem("SRem", URem.index + 1, ASSOCIATIVE),
    FRem("FRem", SRem.index + 1, ASSOCIATIVE),

	// bit-operation
	And("and", SRem.index + 1, ASSOCIATIVE | COMMUTATIVE),

	Or("or", And.index + 1, ASSOCIATIVE | COMMUTATIVE),

	Xor("xor", Or.index + 1, ASSOCIATIVE | COMMUTATIVE),

    // comparison operation
    SetEQ("SetEQ", Xor.index + 1, 0),
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
	Shl("ishl", DNeg.index + 1, 0),
	LShr("lshr", Shl.index + 1, 0),
	AShr("lushr", LShr.index + 1, 0),

	// memory operation
	Alloca("alloca", AShr.index + 1, 0),
	Store("store", Alloca.index + 1, 0),
	Load("load", Store.index + 1, 0),

	// converts operation
	//truncate integers.
	Trunc("trunc", Load.index + 1, 0),
	// zero extend integers.
	ZExt("ZExt", Trunc.index + 1, 0),
	// Sign extend integers.
	SExt("SExt", ZExt.index + 1, 0),
	// floatint-pint to unsigned integer.
	FPToUI("FPToUI", SExt.index + 1, 0),
	// floating point to signed integer.
	FPToSI("FPToSI", FPToUI.index + 1, 0),
	// unsigned integer to floating-point.
	UIToFP("UIToFP", FPToSI.index + 1, 0),
	// signed integer to floating-point.
	SIToFP("SIToFP", UIToFP.index + 1, 0),
	// floating point truncate.
	FPTrunc("f2l", SIToFP.index + 1, 0),
	// float point extend.
	FPExt("FPExt", FPTrunc.index + 1, 0),
	// pointer to integer.
	PtrToInt("PtrToInt", FPExt.index + 1, 0),
	// Integer to pointer.
	IntToPtr("IntToPtr", PtrToInt.index + 1, 0),
	// type cast.
	BitCast("BitCast", IntToPtr.index + 1, 0),

	I2B("i2b", BitCast.index + 1, 0),
	I2C("i2c", I2B.index + 1, 0),
	I2S("i2s", I2C.index + 1, 0),

	// other operation
	Phi("phi", I2S.index + 1, 0),
	ICmp("ICmp", Phi.index + 1, 0),
	FCmp("FCmp",ICmp.index + 1, 0),
	GetElementPtr("GetElementPtr", FCmp.index + 1, 0);

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
		return this.index >= Add.index && this.index <= FAdd.index;
	}
    /**
     * Determines whether this operator is sub operator.
     * @return
     */
	public boolean isSub()
	{
		return index >= Sub.index && index <= FSub.index;
	}
	/**
	 * Determines whether this operator is Multiple operator.
	 * @return
	 */
	public boolean isMul()
    {
	    return index >= Mul.index && index<= FMul.index;
    }	
	
	public static Operator getMulByKind(LIRKind kind)
	{
		switch (kind)
        {
            case Int:
            case Long:
                return Mul;
            case Float:
            case Double:
                return FMul;
            default:
                return None;
        }
	}
	
	public static Operator getAddByKind(LIRKind kind)
	{
	    switch (kind)
        {
            case Double:
            case Float:
                return FAdd;
            case Long:
            case Int:
                return Add;
            default:
                return None;
        }
	}

	public boolean isComparison()
    {
        return index>=Trunc.index && index<=BitCast.index;
    }
}
