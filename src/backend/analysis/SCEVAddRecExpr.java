package backend.analysis;

import backend.hir.Operator;
import backend.type.Type;
import backend.value.Constant;
import backend.value.ConstantExpr;
import backend.value.ConstantInt;
import jlang.sema.APInt;
import tools.Pair;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This node represents a polynomial recurrence on the trip count of the specified loop.
 * </p>
 * Note that all operands of an {@code AddRec} are required to be loop invariant.
 * @author Xlous.zeng
 * @version 0.1
 */
public class SCEVAddRecExpr extends SCEV
{
	private ArrayList<SCEV> operands;
	private Loop loop;

	private SCEVAddRecExpr(ArrayList<SCEV> operands, Loop loop)
	{
		super(SCEVType.scAddRecExpr);
		this.operands = operands;
		this.loop = loop;
		operands.forEach(op->
		{
			assert op.isLoopInvariant(loop)
					: "Operands of AddRec expression must be Loop Invariant";
		});
	}

	private static HashMap<Pair<Loop, ArrayList<SCEV>>, SCEVAddRecExpr>
			scevAddRecExpr = new HashMap<>();

	public static SCEV get(SCEV start, SCEV step, Loop loop)
	{
		ArrayList<SCEV> operands = new ArrayList<>();
		operands.add(start);
		if (step instanceof SCEVAddRecExpr)
		{
			SCEVAddRecExpr addRecExpr = (SCEVAddRecExpr)step;
			if (addRecExpr.getLoop().equals(loop))
			{
				operands.addAll(addRecExpr.getOperands());
				return get(operands, loop);
			}
		}
		operands.add(step);
		return get(operands, loop);
	}

	public static SCEV get(ArrayList<SCEV> operands, Loop loop)
	{
		if (operands.size() == 1) return operands.get(0);
		SCEV last = operands.get(operands.size() - 1);
		if (last instanceof SCEVConstant)
		{
			// {X, +, 0} => X.
			SCEVConstant stepC = (SCEVConstant)last;
			if (stepC.getValue().isNullValue())
			{
				operands.remove(operands.size() - 1);
				return get(operands, loop);
			}
		}
		Pair<Loop, ArrayList<SCEV>> key = new Pair<>(loop, operands);
		if (scevAddRecExpr.containsKey(key))
			return scevAddRecExpr.get(key);

		SCEVAddRecExpr result = new SCEVAddRecExpr(operands, loop);
		scevAddRecExpr.put(key, result);
		return result;
	}

	public ArrayList<SCEV> getOperands()
	{
		return operands;
	}

	public SCEV getStart()
	{
		return operands.get(0);
	}

	public Loop getLoop()
	{
		return loop;
	}

	public int getNumOperands()
	{
		return operands.size();
	}

	/**
	 * This method constructs and returns the SCEV value that indicates
	 * how much this expression steps by.
	 * If this is a polynomial value of degree N, it returns a chrec of degree
	 * N-1.
	 * @return
	 */
	public SCEV getStepRecurrence()
	{
		if (isAffine()) return operands.get(1);
		return get(new ArrayList<>(operands.subList(1, operands.size()))
				, loop);
	}

	/**
	 * Return true if this is an affine AddRec (i.e., it represents
	 * an expressions A+B*x where A and B are loop invariant values.
	 * @return
	 */
	public boolean isAffine()
	{
		return getNumOperands() == 2;
	}

	/**
	 * Return true if this is an quadratic AddRec (i.e., it
	 * represents an expressions A+B*x+C*x^2 where A, B and C are loop
	 * invariant values.  This corresponds to an addrec of the form {L,+,M,+,N}
	 * @return
	 */
	public boolean isQuadratic()
	{
		return getNumOperands() == 3;
	}

    /**
     * Computes the val * (val - 1) * ... (val - numSteps+1)
     * @param val
     * @param numSteps
     * @return
     */
	private SCEV partialFact(SCEV val, int numSteps)
    {
        Type ty = val.getType();
        // We should make folding operation on the common cases.
        if (val instanceof SCEVConstant)
        {
            SCEVConstant cons = (SCEVConstant)val;
            APInt init = cons.getValue().getValue();
            APInt result = init;
            for (; numSteps != 0; numSteps--)
                result.mulAssign(result.sub(new APInt(32, numSteps-1)));
            Constant res = ConstantInt.get(result);
            return SCEVUnknown.get(ConstantExpr.getCast(Operator.BitCast, res, ty));
        }

        if (numSteps == 0)
            return SCEVUnknown.getIntegerSCEV(1, ty);

        SCEV result = val;
        for(int i = 1; i < numSteps; i++)
            result = SCEVMulExpr.get(result, SCEV.getMinusSCEV(val, SCEVUnknown.getIntegerSCEV(i, ty)));

        return result;
    }

    public SCEV getOperand(int index)
    {
        assert index >= 0 && index < getNumOperands();
        return operands.get(index);
    }

	/**
	 * Returns the value of this add recurrence at the specified iteration number.
     * We evaluate this recurrence by multiplying each element in the chain by
     * binomial coefficient corresponding to it. In other words, we compute the
     * {A, +, B, +, C, D} with following equation:
     * <pre>
     *     A*choose(it, 0) + B*choose(it, 1) + C*choose(it, 2) + D*choose(it, 3).
     * </pre>
	 * @param it
	 * @return
	 */
	public SCEV evaluateAtIteration(SCEV it)
	{
	    SCEV result = getStart();
	    Type ty = it.getType();
	    int divisor = 1;
	    for(int i = 1, e = getNumOperands(); i < e; i++)
        {
            SCEV bc = partialFact(it, i);
            SCEV val = SCEVSDivExpr.get(SCEVMulExpr.get(bc, operands.get(i)),
                    SCEVUnknown.getIntegerSCEV(divisor, ty));
            result = SCEVAddExpr.get(result, val);
        }
		return result;
	}

    /**
     * Return the number of iterations of this loop that produce values in the
     * specified constant range.  Another way of looking at this is that it
     * returns the first iteration number where the value is not in the
     * condition, thus computing the exit count.  If the iteration count can't
     * be computed, an instance of SCEVCouldNotCompute is returned.
     * @param range
     * @return
     */
	public SCEV getIterationNumberInRange(ConstantRange range)
	{
		return null;
	}

	public SCEV replaceSymbolicValuesWithConcrete(SCEV sym, SCEV concrete)
	{
	    for(int i = 0, e = getNumOperands(); i < e; i++)
        {
            SCEV h = operands.get(i).replaceSymbolicValuesWithConcrete(sym, concrete);
            if (!h.equals(operands.get(i)))
            {
                ArrayList<SCEV> newOps = new ArrayList<>();
                for (int j = 0; j < i; j++)
                    newOps.add(operands.get(j));
                newOps.add(h);
                for (int j = i+1; j < e;j++)
                    newOps.add(operands.get(j).replaceSymbolicValuesWithConcrete(sym, concrete));

                return get(newOps, loop);
            }
        }
		return this;
	}

	/**
	 * Returns true if the value is invariant in the specified queryLoop.
	 *
	 * This recurrence is an invariant only and only if the queryLoop does
	 * not contain the loop and the start SCEV is loop invariant in terms of
	 * the queryLoop.
	 *
	 * @param queryLoop
	 * @return
	 */
	@Override
	public boolean isLoopInvariant(Loop queryLoop)
	{
		return !queryLoop.contains(loop) &&
				getStart().isLoopInvariant(queryLoop);
	}

	/**
	 * Checks to see whether this SCEV changes value in a known method in the
	 * specified loop. This properly being true implies that the value is variant
	 * in the loop and that we can emit an expression to compute the value of
	 * the expression at any particular loop iteration.
	 *
	 * @param loop
	 * @return
	 */
	@Override
	public boolean hasComputableLoopEvolution(Loop loop)
	{
		return this.loop.equals(loop);
	}

	/**
	 * Returns the LLVM type of this SCEV value.
	 *
	 * @return
	 */
	@Override
	public Type getType()
	{
		return getStart().getType();
	}

	@Override
	public void print(PrintStream os)
	{

	}
}
