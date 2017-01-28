package backend.analysis;

import backend.hir.BasicBlock;
import backend.intrinsic.Intrinsic;
import backend.pass.LoopPass;
import backend.value.Instruction;
import backend.value.Instruction.PhiNode;
import tools.OutParamWrapper;

import java.io.ByteArrayInputStream;
import java.util.*;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class IVUsers extends LoopPass
{
	private Loop loop;
	private LoopInfo li;
	private DomTreeInfo dt;
	private ScalarEvolution se;
	private HashSet<Instruction> processed = new HashSet<>();

	/**
	 * A list of all tracked IV users of induction variable expressions
	 * we are interested in.
	 */
	public ArrayList<IVUsersOfOneStride> ivUsers;
	/**
	 * A mapping from strides to the uses in {@linkplain #ivUsers}.
	 */
	public HashMap<SCEV, IVUsersOfOneStride> ivUsesByStride;

	@Override
	public boolean runOnLoop(Loop loop)
	{
		this.loop = loop;
		li = getAnalysisToUpDate(LoopInfo.class);
		dt = getAnalysisToUpDate(DomTreeInfo.class);
		se = getAnalysisToUpDate(ScalarEvolution.class);
		BasicBlock header = loop.getHeaderBlock();
		assert header != null;

		// Find all of uses of induction variables in this loop, and
		// categorize them by stride. Start by finding all Phi nodes
		// in the header block of this loop. If they are induction
		// variables, inspect there uses.

		// Note that, the induction variable in SSA form is represented by
		// Phi node.
		for (Instruction inst : header)
		{
			if (!(inst instanceof PhiNode))
				break;
			addUsersIfInteresting(inst);
		}
		return false;
	}

	/**
	 * Inspect the specified instruction whether is a SCEV variable, recursively
	 * add its user to the {@linkplain #ivUsesByStride} map and return true.
	 * Otherwise, return false.
	 * @param inst
	 *
	 */
	private boolean addUsersIfInteresting(Instruction inst)
	{
		// Avoiding that instruction of typed FP.
		if (!se.isSCEVable(inst.getType()))
			return false;

		// avoiding handle the too bigger integral type.
		if (se.getTypeSizeBits(inst.getType()) > 64)
			return false;

		// Has been processed yet.
		if (!processed.add(inst))
			return true;

		// get the scev for this instruction.
		SCEV ise = se.getSCEV(inst);
		if (ise instanceof SCEVCouldNotCompute) return false;

		Loop useLoop = li.getLoopFor(inst.getParent());
		SCEV start = se.getIntegerSCEV(0, ise.getType());
		SCEV stride = start;
		OutParamWrapper<SCEV> startOut = new OutParamWrapper<>(start);
		OutParamWrapper<SCEV> strideOut = new OutParamWrapper<>(stride);
		if (!getSCEVStartAndStride(ise, loop, useLoop, startOut, strideOut, se, dt))
			return false;

		// Updates the returned result.
		start = startOut.get();
		stride = strideOut.get();
		return false;
	}

	/**
	 * Computes the start and stride for this instruction, returning false
	 * if the expression is not a start/stride pair, or true if it is.
	 * The stride must be a loop invariant expression, but the start may be
	 * a mixture of loop invariant and loop variant. However, the start cannot
	 * contain an AddRec from a different loop, unless that loop is an outer
	 * loop of the current loop.
	 * @param sh
	 * @param loop
	 * @param useLoop
	 * @param start
	 * @param stride
	 * @param se
	 * @param dt
	 * @return
	 */
	private static boolean getSCEVStartAndStride(SCEV sh, Loop loop,
			Loop useLoop, OutParamWrapper<SCEV> start,
			OutParamWrapper<SCEV> stride,
			ScalarEvolution se, DomTreeInfo dt)
	{
		SCEV theAddRec = start.get();

		// If the outer level is an AddExpr, the operands are all starts values
		// except for a nested AddRecExpr.
		if (sh instanceof SCEVAddExpr)
		{
			SCEVAddExpr ae = (SCEVAddExpr)sh;
			for (int i = 0, e = ae.getNumOperands(); i < e; i++)
			{
				SCEV op = ae.getOperand(i);
				if (op instanceof SCEVAddRecExpr)
				{
					SCEVAddRecExpr addRec = (SCEVAddRecExpr)op;
					if (addRec.getLoop().equals(loop))
						theAddRec = SCEVAddExpr.get(addRec, theAddRec);
					else
						// Nested IV of some sort?
						return false;
				}
				else
				{
					start.set(SCEVAddExpr.get(start.get(), op));
				}
			}
		}
		else if (sh instanceof SCEVAddRecExpr)
			theAddRec = sh;
		else
			return false;   // Non analyzable.


		if (!(theAddRec instanceof SCEVAddRecExpr)
				|| ((SCEVAddRecExpr) theAddRec).getLoop() != loop)
			return false;

		SCEVAddRecExpr addRec = (SCEVAddRecExpr)theAddRec;

		// Use the getSCEVAtScope to attempt to simplify other loops.
		SCEV addRecStart = addRec.getStart();
		addRecStart = se.getSCEVAtScope(addRecStart, useLoop);
		SCEV addRecStride = addRec.getStepRecurrence();

		if (containsAddRecFromDifferentLoop(addRecStart, loop))
			return false;

		start.set(SCEVAddExpr.get(start.get(), addRecStart));

		// If stride is an instruction, make sure it dominates the loop preheader.
		// Otherwise we could end up with a use before def situation.
		if (!(addRecStride instanceof SCEVConstant))
		{
			BasicBlock preheader = loop.getLoopPreheader();
			if (!addRecStride.dominates(preheader, dt))
				return false;
		}

		stride.set(addRecStride);
		return true;
	}

	/**
	 * Determine whether expression {@code s} involves a subexpression that is
	 * an AddRec from a loop other than L.  An outer loop of L is OK, but not
	 * an inner loop nor a disjoint loop.
	 * @param s
	 * @param loop
	 * @return
	 */
	private static boolean containsAddRecFromDifferentLoop(SCEV s, Loop loop)
	{
		if (s instanceof SCEVConstant)
			return false;

		if (s instanceof SCEVCommutativeExpr)
		{
			SCEVCommutativeExpr commExpr = (SCEVCommutativeExpr)s;
			for (int i = 0, e = commExpr.getNumOperands(); i < e; i++)
			{
				if (containsAddRecFromDifferentLoop(commExpr.getOperand(i), loop))
					return true;
			}
			return false;
		}

		if (s instanceof SCEVAddRecExpr)
		{
			SCEVAddRecExpr ae = (SCEVAddRecExpr)s;
			Loop newLoop = ae.getLoop();
			if (newLoop != null)
			{
				if (newLoop == loop)
					return false;

				// If the newLoop is an outer loop of loop, this is OK.
				if (!newLoop.contains(loop))
					return false;
			}
			return true;
		}

		if (s instanceof SCEVSDivExpr)
		{
			SCEVSDivExpr divExpr = (SCEVSDivExpr)s;
			return containsAddRecFromDifferentLoop(divExpr.getLHS(), loop)
					|| containsAddRecFromDifferentLoop(divExpr.getRHS(), loop);
		}

		return false;
	}

	@Override
	public String getPassName()
	{
		return "Induction variable users function pass";
	}
}
