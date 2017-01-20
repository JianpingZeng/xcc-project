package backend.analysis;

import backend.type.Type;
import backend.value.ConstantExpr;

import java.io.PrintStream;

/**
 * This class represent an analyzed expression in the program.  These
 * are reference counted opaque objects that the client is not allowed to
 * do much with directly.
 */
public abstract class SCEV
{
    /**
     * These should ordered in terms of increasing complexity to make the folder
     * easily.
     */
    enum SCEVType
    {
        scConstant, scTruncate, scZeroExtend, scAddExpr, scMulExpr,
        scSDivExpr, scAddRecExpr, scUnknown, scCouldNotCompute
    }
    SCEVType scevType;

    protected SCEV(SCEVType scevType)
    {
        this.scevType = scevType;
    }

    /***
     * Returns a SCEV corresponding to -val = -1 * val.
     * @return
     */
    public static SCEV getNegativeSCEV(SCEV val)
    {
        if (val instanceof SCEVConstant)
        {
            SCEVConstant vc = (SCEVConstant)val;
            return SCEVUnknown.get(ConstantExpr.getNeg(vc.getValue()));
        }
        return SCEVMulExpr.get(val, SCEVUnknown.getIntegerSCEV(-1, val.getType()));
    }

    public static SCEV getMinusSCEV(SCEV lhs, SCEV rhs)
    {
        return null;
    }

    public final SCEVType getSCEVType()
    {
        return scevType;
    }

    public final boolean isZero()
    {
        if (this instanceof SCEVConstant)
            ((SCEVConstant)this).getValue().isZero();
        return false;
    }

    public final boolean isOne()
    {
        if (this instanceof SCEVConstant)
            ((SCEVConstant)this).getValue().isOne();
        return false;
    }

    public final boolean isAllOnesValue()
    {
        if (this instanceof SCEVConstant)
            ((SCEVConstant)this).getValue().isAllOnesValue();
        return false;
    }

	/**
     * Returns true if the value is invariant in the specified loop.
     * @param loop
     * @return
     */
    public abstract boolean isLoopInvariant(Loop loop);

	/**
     * Checks to see whether this SCEV changes value in a known method in the
     * specified loop. This properly being true implies that the value is variant
     * in the loop and that we can emit an expression to compute the value of
     * the expression at any particular loop iteration.
     * @param loop
     * @return
     */
    public abstract boolean hasComputableLoopEvolution(Loop loop);

	/**
	 * If this SCEV internally references the symbolic value {@code sym},
     * construct and return a new SCEV that produces the same value, but
     * which uses the concrete value {@code concrete} instead of the
     * symbolic value. If this SCEV does not use the symbolic value,
     * it returns itself.
     * @param sym
     * @param concrete
     * @return
     */
    public abstract SCEV replaceSymbolicValuesWithConcrete(SCEV sym, SCEV concrete);

	/**
     * Returns the LLVM type of this SCEV value.
     * @return
     */
    public abstract Type getType();

    public abstract void print(PrintStream os);

    public void dump()
    {
        print(System.err);
        System.err.println();
    }
}
