package backend.analysis;

import backend.value.*;
import backend.value.Operator;
import backend.type.Type;
import tools.APInt;
import tools.Pair;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;

import static backend.value.Instruction.CmpInst.Predicate.ICMP_ULT;

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
     * Computes the value * (value - 1) * ... (value - numSteps+1)
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
            return ScalarEvolution.getIntegerSCEV(1, ty);

        SCEV result = val;
        for(int i = 1; i < numSteps; i++)
            result = SCEVMulExpr.get(result, ScalarEvolution
		            .getMinusSCEV(val, ScalarEvolution
		            .getIntegerSCEV(i, ty)));

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
                    ScalarEvolution.getIntegerSCEV(divisor, ty));
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
	public SCEV getIterationNumberInRange(ConstantRange range, ScalarEvolution se)
	{
	    // Infinite loop.
	    if (range.isFullSet())
	        return SCEVCouldNotCompute.getInstance();

	    // If the start is a non-zero constant, shift the range to simplify things.
        if (getStart() instanceof SCEVConstant)
        {
            SCEVConstant sc = (SCEVConstant)getStart();
            if (!sc.getValue().isZero())
            {
                ArrayList<SCEV> newOps = new ArrayList<>();
                newOps.addAll(operands);
                newOps.set(0, ScalarEvolution.getIntegerSCEV(0, sc.getType()));

                SCEV shiftedRec = get(newOps, getLoop());
                if (shiftedRec instanceof SCEVAddRecExpr)
                {
                    return ((SCEVAddRecExpr)shiftedRec).getIterationNumberInRange(
                            range.subtract(sc.getValue().getValue()), se);
                }
                return SCEVCouldNotCompute.getInstance();
            }
        }

        // Checks if each binomial coefficient is a SCEVConstant, if not return
        // SCEVCouldNotCompute.
        for (SCEV sc : operands)
            if (!(sc instanceof SCEVConstant))
                return SCEVCouldNotCompute.getInstance();

        // Okay, at this point we known that all of cofficient is a SCEVConstant.
        // And the start element is zero.

        int bitwidth = se.getTypeSizeBits(getType());
        if (!range.contains(new APInt(bitwidth, 0)))
            return ScalarEvolution.getIntegerSCEV(0, getType());

        if (isAffine())
        {
            // If this expression is a affine then we can solve this problem by
            // equation Ax in range.
            APInt one = new APInt(bitwidth, 1);
            APInt first = ((SCEVConstant)getOperand(1)).getValue().getValue();
            APInt end = first.sge(one) ? (range.getUpper().sub(one)) : range.getLower();

            // compute the (end/first) =>(end + first - 1)/first.
            APInt exitItrNum = end.add(first).udiv(first);
            ConstantInt exitValue = ConstantInt.get(exitItrNum);

            // Evaluate at the exit value.  If we really did fall out of the valid
            // range, then we computed our trip count, otherwise wrap around or other
            // things must have happened.
            ConstantInt val = evaluateConstantChrecAtConstant(this, exitValue);
            if (range.contains(val.getValue()))
                return SCEVCouldNotCompute.getInstance();

            // Ensure that the previous value is in the range.
            assert range.contains(evaluateConstantChrecAtConstant(this,
                    ConstantInt.get(exitItrNum.sub(1))).getValue())
                    : "Linear scev computation is off in a bad way!";
            return SCEVConstant.get(exitValue);
        }
        else if (isQuadratic())
        {
            // If this is a quadratic AddRecExpr {L, +, M, N}, find out the roots
            // of quadratic equation to solve it.
            ArrayList<SCEV> newOps = new ArrayList<>();
            newOps.addAll(operands);
            newOps.set(0, ScalarEvolution.getNegativeSCEV(
                    SCEVConstant.get(ConstantInt.get(range.getLower()))));
            SCEV newAddRecExpr = SCEVAddRecExpr.get(newOps, loop);

            /// Next, solve the quadratic equation.
            Pair<SCEV, SCEV> roots = solveQuadraticEquation((SCEVAddRecExpr)newAddRecExpr);
            SCEV val1 = roots.first;
            if (val1 instanceof SCEVConstant)
            {
                SCEVConstant r1 = (SCEVConstant)val1;
                SCEVConstant r2 = (SCEVConstant)roots.second;
                // Pick up a smallest positive root value.
                Constant cmpRes = ConstantExpr.getICmp(ICMP_ULT, r1.getValue(), r2.getValue());
                if (cmpRes instanceof ConstantInt)
                {
                    ConstantInt root = (ConstantInt)cmpRes;
                    if (root.getZExtValue() == 0)
                    {
                        // r1 is the minimal root value.
                        SCEVConstant tmp = r1;
                        r1 = r2;
                        r2 = tmp;
                    }

                    ConstantInt r1Value = evaluateConstantChrecAtConstant(this, r1.getValue());
                    // If the r1 value is in the range, then the r1Value + 1 must
                    // out of the range.
                    if (range.contains(r1Value.getValue()))
                    {
                        /// The next iteration must be out of the range.
                        ConstantInt r1AddOne = ConstantInt.get(r1.getValue().getValue().increase());
                        r1Value = evaluateConstantChrecAtConstant(this, r1AddOne);
                        if (!range.contains(r1Value.getValue()))
                            return SCEVConstant.get(r1AddOne);

                        return SCEVCouldNotCompute.getInstance();
                    }

                    // If the r1Value is not in the range, so it is a good return
                    // value.
                    // But we must ensure that the r1Value-1 must in the range for
                    // sanity check.
                    ConstantInt r1SubOne = ConstantInt.get(r1Value.getValue().decrease());
                    r1Value = evaluateConstantChrecAtConstant(this, r1SubOne);
                    if(range.contains(r1Value.getValue()))
                        return r1;

                    return SCEVCouldNotCompute.getInstance();
                }
            }
        }
		return SCEVCouldNotCompute.getInstance();
	}

    /**
     * Computes the roots of the quadratic equation for the chrecs {L, +, M, + N},
     * it can represents as <pre>ax^2 + bx + c</pre>.
     * The return value either is the two roots (it may be the same value) or two
     * SCEVCouldNotCompute objects.
     * @param addRec
     * @return
     */
	public static Pair<SCEV, SCEV> solveQuadraticEquation(SCEVAddRecExpr addRec)
    {
        assert addRec.getNumOperands() ==3 :"The addrec is invalid!";
        SCEV c1 = addRec.getOperand(0);
        SCEV c2 = addRec.getOperand(1);
        SCEV c3 = addRec.getOperand(2);
        if (!(c1 instanceof SCEVConstant)
                || !(c2 instanceof SCEVConstant)
                || !(c3 instanceof SCEVConstant))
        {
            // We only compute the root when all coefficient are constant.
            SCEV temp = SCEVCouldNotCompute.getInstance();
            return new Pair<>(temp, temp);
        }

        SCEVConstant lc = (SCEVConstant)c1;
        SCEVConstant lm = (SCEVConstant)c2;
        SCEVConstant ln = (SCEVConstant)c3;

        int bitwidth = lc.getValue().getValue().getBitWidth();
        APInt l = lc.getValue().getValue();
        APInt m = lm.getValue().getValue();
        APInt n = ln.getValue().getValue();

        APInt two = new APInt(bitwidth,2);
        APInt four = new APInt(bitwidth, 4);

        // convert the chrec coefficient to the polynomial coefficient.
        // The b coefficient is m-n/2. So the equivalent equation is
        // ax^2 + bx + c.
        APInt c = l;
        APInt b = new APInt(m);
        b.subAssign(n.sdiv(two));

        // The a coefficient is n/2.
        APInt a = new APInt(n.sdiv(two));

        // Compute the b^2 - 4ac.
        APInt sqrtTerm = new APInt(b.mul(b));
        sqrtTerm.subAssign(a.mul(c).mul(four));

        APInt negativeB = b.negative();
        APInt towA = a.shl(1);
        if (towA.isMinValue())
        {
            // If the towA is the minimal value, we can not to compute it
            // for avoiding the loss of precision.
            SCEV temp = SCEVCouldNotCompute.getInstance();
            return new Pair<>(temp, temp);
        }

        APInt sqrtVal = new APInt(sqrtTerm.sqrt());

        ConstantInt r1 = ConstantInt.get(negativeB.sub(sqrtVal).sdiv(towA));
        ConstantInt r2 = ConstantInt.get(negativeB.add(sqrtVal).sdiv(towA));
        return Pair.get(SCEVConstant.get(r1), SCEVConstant.get(r2));
    }

	public static ConstantInt evaluateConstantChrecAtConstant(SCEVAddRecExpr addRec,
            ConstantInt itNum)
    {
        SCEV itrNumber = SCEVConstant.get(itNum);
        SCEV res = addRec.evaluateAtIteration(itrNumber);
        assert (res instanceof SCEVConstant)
                : "Evaluation of SCEV at constant didn't fold correctly!";
        return ((SCEVConstant)res).getValue();
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
	public boolean dominates(BasicBlock bb, DomTreeInfo dt)
	{
		// TODO: 17-6-18
		return false;
	}

	@Override
	public void print(PrintStream os)
	{

	}
}
