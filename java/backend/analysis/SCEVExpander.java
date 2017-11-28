package backend.analysis;

import backend.hir.HIRBuilder;
import backend.support.BasicBlockUtil;
import backend.type.PointerType;
import backend.type.Type;
import backend.value.*;
import backend.value.Instruction.CastInst;
import backend.value.Instruction.PhiNode;
import tools.APInt;
import tools.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * <p>
 * This class uses the analyzed SCEV information to rewrite expressions
 * in canonical form.
 * </p>
 * <p>
 * The client should creates an instance of this class when rewriting is
 * needed.
 * </p>
 * @author Xlous.zeng
 * @version 0.1
 */
public final class SCEVExpander implements SCEVVisitor<Value>
{
	private ScalarEvolution se;
	private HashMap<Pair<SCEV, Instruction>, Value> insertedExpressions;
	private HashSet<Value> insertedValues;
	private HIRBuilder builder;

	public SCEVExpander(ScalarEvolution se)
	{
		this.se = se;
		insertedExpressions = new HashMap<>();
		insertedValues = new HashSet<>();
		builder = new HIRBuilder();
	}

	public void clear()
	{
		insertedValues.clear();
		insertedExpressions.clear();
	}

	/**
	 * <p>
	 * Creates a canonical induction variable in the specified loop and of
	 * the specified type (inserting one if there is none).
	 * </p>
	 * <p>
	 *  A canonical induction variable starts with zero and steps by one on
	 *  each iteration.
	 * </p>
	 * @param loop
	 * @param ty
	 * @return
	 */
	public Value getOrCreateCanonicalInductionVariable(Loop loop, Type ty)
	{
		assert ty.isIntegral():"Can only insert integer induction variable";
		SCEV s = SCEVAddRecExpr.get(se.getIntegerSCEV(0, ty),
				se.getIntegerSCEV(1, ty), loop);
		BasicBlock saveInsertBB = builder.getInsertBlock();
		Instruction saveInsertPos = builder.getInsertPoint();
		Value val = expandCodeFor(s, ty, loop.getHeaderBlock().getFirstInst());
		if (saveInsertBB != null)
			builder.setInsertPoint(saveInsertBB, saveInsertPos);
		return val;
	}

	/**
	 * Inserts code to directly compute the specified SCEV expression
	 * into the program. The inserted code is inserted into the specified
	 * block.
	 * @param s     The SCEV expression to be computed.
	 * @param ty    The type of computed code.
	 * @param ip    The position where computed code inserted into.
	 * @return
	 */
	public Value expandCodeFor(SCEV s, Type ty, Instruction ip)
	{
		builder.setInsertPoint(ip.getParent(), ip);
		return expandCodeFor(s, ty);
	}

	private Value expandCodeFor(SCEV s, Type ty)
	{
		Value val = expand(s);
		if (ty != null)
		{
			assert se.getTypeSizeBits(ty) == se.getTypeSizeBits(s.getType());
			val = insertNoopCastOfTo(val, ty);
		}
		return val;
	}

	private Value insertBinOp(Operator binOp, Value lhs, Value rhs)
	{
		if (lhs instanceof Constant)
		{
			if(rhs instanceof Constant)
				return ConstantExpr.get(binOp, (Constant)lhs, (Constant)rhs);
		}

		// Do a quick scan to see if we have this binop nearby.
		// If so, reuse it.
		int scanLimit = 6;
		Instruction ip = builder.getInsertPoint();
		BasicBlock bb = builder.getInsertBlock();
		// Scanning starts from the last instruction before the insertion point.
		int idx = bb.indexOf(ip);
		if (idx > 0)
		{
			idx--;
			for (; scanLimit != 0;)
			{
				Instruction inst = bb.getInstAt(idx);
				if (inst.getOpcode() == binOp
						&& inst.operand(0).equals(lhs)
						&& inst.operand(1).equals(rhs))
					return inst;
				if (idx == 0) break;
				idx--;
				scanLimit--;
			}
		}
		Value bo = builder.createBinOp(binOp, lhs, rhs, "tmp");
		insertedValues.add(bo);
		return bo;
	}

	/**
	 * Insert a cast of V to the specified type, which must be possible
	 * with a noop cast, doing what we can to share the casts.
	 * @param val
	 * @param ty
	 * @return
	 */
	private Value insertNoopCastOfTo(Value val, Type ty)
	{
		Operator castOp = CastInst.getCastOpcode(val, false, ty, false);
		assert (castOp == Operator.BitCast
				|| castOp == Operator.PtrToInt
				|| castOp == Operator.IntToPtr)
				:"insertNoopCastOfTo cannot performs non-noop cast!";
		assert se.getTypeSizeBits(val.getType()) == se.getTypeSizeBits(ty)
				:"insertNoopCastOfTo cannot performs inequal type getNumOfSubLoop!";

		if (castOp == Operator.BitCast && val.getType() == ty)
			return val;

		if ((castOp == Operator.PtrToInt || castOp == Operator.IntToPtr)
				&& se.getTypeSizeBits(ty) == se.getTypeSizeBits(val.getType()))
		{
			if (val instanceof CastInst)
			{
				CastInst ci = (CastInst)val;
				if ((ci.getOpcode() == Operator.PtrToInt ||
				ci.getOpcode() == Operator.IntToPtr)&&
						se.getTypeSizeBits(ci.getType()) ==
						se.getTypeSizeBits(ci.operand(0).getType()))
				{
					return ci.operand(0);
				}
			}
			if (val instanceof ConstantExpr)
			{
				ConstantExpr ce = (ConstantExpr)val;
				if ((ce.getOpcode() == Operator.PtrToInt
						|| ce.getOpcode() == Operator.IntToPtr)
						&& se.getTypeSizeBits(ce.getType())
						== se.getTypeSizeBits(ce.operand(0).getType()))
					return ce.operand(0);
			}
		}

		if (val instanceof Constant)
			return ConstantExpr.getCast(castOp, (Constant)val, ty);

		if (val instanceof Argument)
		{
			// Check to see if there is already a cast.
			Argument a = (Argument)val;
			for (Use u : a.getUseList())
			{
				if (u.getUser().getType() == ty)
				{
					if (u.getUser() instanceof CastInst)
					{
						CastInst ci = (CastInst)u.getUser();
						if (ci.getOpcode() == castOp)
						{
							// If the cast isn't the first instruction of
							// the function, move it.
							if (ci.getIndexToBB() != 0)
							{
								Instruction newCI = CastInst.create(castOp, val, ty,
										"", a.getParent().getEntryBlock().getFirstInst());
								newCI.setName(ci.getName());
								ci.replaceAllUsesWith(newCI);
								return newCI;
							}
							return ci;
						}
					}
				}
			}
			Instruction inst = CastInst.create(castOp, val, ty, val.getName(),
					a.getParent().getEntryBlock().getFirstInst());
			insertedValues.add(inst);
			return inst;
		}

		Instruction inst = (Instruction)val;

		// Check to see if there is already a cast.
		// If there is , use it.
		for (Use u : inst.getUseList())
		{
			if (u.getUser().getType() == ty)
			{
				if (u.getUser() instanceof CastInst)
				{
					CastInst ci = (CastInst)u.getUser();
					if (ci.getOpcode() == castOp)
					{
						int itr = inst.getIndexToBB();
						BasicBlock bb = inst.getParent();
						itr++;
						while (bb.getInstAt(itr) instanceof PhiNode)
							itr++;
						if (itr == ci.getIndexToBB())
						{
							Instruction newCI = CastInst.create(castOp, val, ty,
									"", bb.getInstAt(itr));
							newCI.setName(ci.getName());
							ci.replaceAllUsesWith(newCI);
							return newCI;
						}
						return ci;
					}
				}
			}
		}
		int itr = inst.getIndexToBB();
		BasicBlock bb = inst.getParent();
		itr++;
		while (bb.getInstAt(itr) instanceof PhiNode)
			itr++;
		Instruction newCI = CastInst.create(castOp, val, ty,
				val.getName(), bb.getInstAt(itr));
		insertedValues.add(newCI);
		return newCI;
	}

	/**
	 * Expand a SCEVAddExpr with a pointer type into a GEP
	 * instead of using ptrtoint+arithmetic+inttoptr.
	 * @param ops
	 * @param pty
	 * @param ty
	 * @param val
	 * @return
	 */
	private Value expandAddToGEP(ArrayList<SCEV> ops, PointerType pty,
			Type ty, Value val)
	{
		return null;
	}

	private Value expand(SCEV s)
	{
		// Compute an insert point for this SCEV object.
		// Hoist the code as far out in the loop nest as possible.
		Instruction insertPtr = builder.getInsertPoint();
		for(Loop l = se.getLI().getLoopFor(builder.getInsertBlock());;
				l = l.getParentLoop())
		{
			if (s.isLoopInvariant(l))
			{
				if (l == null) break;
				BasicBlock preheader = l.getLoopPreheader();
				if (preheader != null)
					insertPtr = preheader.getTerminator();
			}
			else
			{
				// If this SCEV object is a computable at this level
				// insert it into the position after first non-phi instruction
				// So that it is guaranteed to dominate any user inside the loop.
				if (l != null && s.hasComputableLoopEvolution(l))
				{
					BasicBlock bb = l.getHeaderBlock();
					insertPtr = bb.getInstAt(bb.getFirstNonPhi());
				}
				while (isInsertedInstruction(insertPtr))
					insertPtr = BasicBlockUtil.next(insertPtr);
				break;
			}
		}

		// Check to see if we already expanded this in here.
		Pair<SCEV, Instruction> key = Pair.get(s, insertPtr);
		if (insertedExpressions.containsKey(key))
			return insertedExpressions.get(key);

		BasicBlock saveInsertBB = builder.getInsertBlock();
		Instruction saveInsertPtr = builder.getInsertPoint();

		builder.setInsertPoint(insertPtr.getParent(), insertPtr);
		Value val = visit(s);
		insertedExpressions.put(key, val);

		builder.setInsertPoint(saveInsertBB, saveInsertPtr);
		return val;
	}

	private boolean isInsertedInstruction(Instruction inst)
	{
		return insertedValues.contains(inst);
	}

	@Override
	public Value visitConstant(SCEVConstant s)
	{
		return s.getValue();
	}

	@Override
	public Value visitAddExpr(SCEVAddExpr s)
	{
		Type ty = se.getEffectiveSCEVType(s.getType());
		Value val = expand(s.getOperand(s.getNumOperands()-1));
		// Turn things like ptrtoint+arithmetic+inttoptr into GEP. See the
		// comments on expandAddToGEP for details.
		if (val.getType().isPointerType())
		{
			PointerType pty = (PointerType)val.getType();
			return expandAddToGEP(s.getOperands(), pty, ty,val);
		}

		val = insertNoopCastOfTo(val, ty);
		for (int i = s.getNumOperands() - 2; i >= 0; i--)
		{
			Value v = expandCodeFor(s.getOperand(i), ty);
			val = insertBinOp(Operator.Add, val, v);
		}
		return val;
	}

	@Override
	public Value visitMulExpr(SCEVMulExpr s)
	{
		Type ty = se.getEffectiveSCEVType(s.getType());
		int firstOp = 0;
		if (s.getOperand(0) instanceof SCEVConstant)
		{
			SCEVConstant sc = (SCEVConstant)s.getOperand(0);
			if (sc.getValue().isAllOnesValue())
				firstOp = 1;
		}

		int i = s.getNumOperands() - 2;
		Value val = expandCodeFor(s.getOperand(i+1), ty);
		for (; i >= firstOp; i--)
		{
			Value rhs = expandCodeFor(s.getOperand(i), ty);
			val = insertBinOp(Operator.Mul, val, rhs);
		}
		if (firstOp == 1)
			val = insertBinOp(Operator.Sub, Constant.getNullValue(ty), val);
		return val;
	}

	@Override
	public Value visitSDivExpr(SCEVSDivExpr s)
	{
		Type ty = se.getEffectiveSCEVType(s.getType());
		Value lhs = expandCodeFor(s.getLHS(), ty);
		if (s.getRHS() instanceof SCEVConstant)
		{
			SCEVConstant sc = (SCEVConstant)s.getRHS();
			APInt rhs = sc.getValue().getValue();
			if (rhs.isPowerOf2())
				return insertBinOp(Operator.LShr, lhs,
						ConstantInt.get(ty, rhs.logBase2()));
		}
		Value rhs = expandCodeFor(s.getRHS(), ty);
		return insertBinOp(Operator.SDiv, lhs, rhs);
	}

	@Override
	public Value visitAddRecExpr(SCEVAddRecExpr s)
	{
		Type ty = s.getType();
		Loop loop = s.getLoop();

		assert ty.isIntegral():"Cannot expand fp recurrence expression!";

		PhiNode canonicalIV = null;
		PhiNode pn = loop.getCanonicalInductionVariable();
		if (pn != null)
		{
			if (se.isSCEVable(pn.getType()) &&
					se.getEffectiveSCEVType(pn.getType()).isPointerType()
					&& se.getTypeSizeBits(pn.getType()) == se.getTypeSizeBits(ty))
				canonicalIV = pn;
		}

		// {X, +, F}=> X + {0, +, F}.
		if (!(s.getStart() instanceof SCEVConstant)
				|| !((SCEVConstant)s.getStart()).getValue().isNullValue())
		{
			Value start = expandCodeFor(s.getStart(), ty);
			ArrayList<SCEV> newOps = new ArrayList<>(s.getOperands());
			newOps.set(0, se.getIntegerSCEV(0, ty));
			Value rest = expandCodeFor(SCEVAddRecExpr.get(newOps, loop), ty);
			return builder.createAdd(rest, start, "tmp");
		}

		// {0, +, 1} => insert a canonical induction variable into the loop.
		if (s.isAffine() && s.getOperand(1).equals(se.getIntegerSCEV(1, ty)))
		{
			// If there's a canonical IV, just use it.
			if (canonicalIV != null)
			{
				assert ty == se.getEffectiveSCEVType(canonicalIV.getType())
						:"IVs with types different from the canonical IV"
						+ "should already have been handled!";
				return canonicalIV;
			}

			// Create and insert the PHI node for the induction variable in the
			// specified loop.
			BasicBlock header = loop.getHeaderBlock();
			BasicBlock preheader = loop.getLoopPreheader();

			pn = new PhiNode(ty, 0, "indvar", header.getFirstInst());
			insertedValues.add(pn);
			pn.addIncoming(Constant.getNullValue(ty), preheader);

			int numPreds = header.getNumPredecessors();
			int predItr = 0;
			assert numPreds > 0:"Loop with zero preds";
			if (!loop.contains(header.predAt(predItr))) predItr++;
			assert predItr!=0 && loop.contains(header.predAt(predItr))
					:"No backedge in loop!";

			Constant one = ConstantInt.get(ty, 1);
			Instruction add = Instruction.BinaryOps
                    .createAdd(pn, one, "indvar.next",
					header.predAt(predItr).getTerminator());

			insertedValues.add(add);
			predItr = 0;
			if (header.predAt(predItr) == preheader)
				++predItr;
			pn.addIncoming(add, header.predAt(predItr));
			return pn;
		}

		// {0,+,F} => {0,+,1} *F.
		Value val = canonicalIV != null? canonicalIV:
				getOrCreateCanonicalInductionVariable(loop, ty);

		// If this is a simple linear addrec, emit it now as a special case.
		if (s.isAffine())
		{
			return expand(se.getTruncateOrNoop(SCEVMulExpr
					.get(SCEVUnknown.get(val),
							se.getNoopOrAnyExtend(s.getOperand(1),
									val.getType())), ty));
		}

		SCEV ih = SCEVUnknown.get(val);
		SCEV newS = s;
		SCEV ext = se.getNoopOrAnyExtend(s, val.getType());
		if (ext instanceof SCEVAddRecExpr)
			newS = ext;

		SCEV v =  ((SCEVAddRecExpr)newS).evaluateAtIteration(ih);
		SCEV t = se.getTruncateOrNoop(v, ty);
		return expand(t);
	}

	@Override
	public Value visitUnknown(SCEVUnknown s)
	{
		return s.getValue();
	}
}
