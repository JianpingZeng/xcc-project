package backend.analysis;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2017, Xlous
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

import backend.hir.BasicBlock;
import backend.hir.Operator;
import backend.hir.PredIterator;
import backend.pass.AnalysisUsage;
import backend.pass.FunctionPass;
import backend.target.TargetData;
import backend.type.PointerType;
import backend.type.Type;
import backend.value.*;
import backend.value.Instruction.*;
import backend.value.Instruction.CmpInst.Predicate;
import jlang.sema.APInt;
import tools.Util;

import java.util.*;

import static backend.analysis.ValueTracking.computeNumSignBits;
import static backend.transform.scalars.ConstantFolder.canConstantFoldCallTo;
import static backend.transform.scalars.ConstantFolder.constantFoldCall;

/**
 * This class is the main scalar evolution jlang.driver. Since client code (intentionally)
 * can't do much the SCEV objects directly, they must query this class for services.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class ScalarEvolution implements FunctionPass
{
    public static int maxBruteForceIteration = 100;
    public static int numTripCountsComputed;

    /**
     * The target information for the targeting.
     */
    private TargetData td;

    /**
     * The being analyzed function.
     */
    private Function f;
    /**
     * The loop information for the function being analyzed.
     */
    private LoopInfo li;
    /**
     * This SCEV is used to represent unknown trip count and thing.
     */
    private SCEV unknownValue;

    /**
     * This is a cache of the scalars we have analyzed as yet.
     */
    private HashMap<Value, SCEV> scalars;

    /**
     * Cache the iteration count of the loops for this function as they are
     * computed.
     */
    private HashMap<Loop, SCEV> iterationCount;

    /**
     * This map contains the entities for all of the PHI node to the constant.
     * This is reason for avoiding expensive pre-computation of there properties.
     * A instruction map to a null if we can not compute its exit value.
     */
    private HashMap<PhiNode, Constant> constantEvolutionLoopExitValue;

    public LoopInfo getLI()
    {
        return li;
    }
    @Override
    public String getPassName()
    {
        return "Scalar Evolution pass on Function";
    }

    @Override
    public boolean runOnFunction(Function f)
    {
        this.f = f;
        li = getAnalysisToUpDate(LoopInfo.class);
        td = getAnalysisToUpDate(TargetData.class);
        unknownValue = SCEVCouldNotCompute.getInstance();
        scalars = new HashMap<>();
        iterationCount = new HashMap<>();
        constantEvolutionLoopExitValue = new HashMap<>();
        return false;
    }

    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        au.addRequired(TargetData.class);
        au.addRequired(LoopInfo.class);
    }

    /**
     * Returns an existing SCEV if it exists, otherwise analyze the expression
     * and create a new one.
     *
     * @param val
     * @return
     */
    public SCEV getSCEV(Value val)
    {
        assert !val.getType().equals(Type.VoidTy) : "Cannot analyze void expression";
        if (scalars.containsKey(val))
            return scalars.get(val);
        SCEV newOne = createSCEV(val);
        scalars.put(val, newOne);
        return newOne;
    }

    /**
     * It is known that there is no SCEV for the specified expression.
     * Analyze the expression.
     * @param val
     * @return
     */
    private SCEV createSCEV(Value val)
    {
        if (val instanceof Instruction)
        {
            Instruction inst = (Instruction)val;
            switch (inst.getOpcode())
            {
                case Add:
                    return SCEVAddExpr.get(getSCEV(inst.operand(0)),
                            getSCEV(inst.operand(1)));
                case Mul:
                    return SCEVMulExpr.get(getSCEV(inst.operand(0)),
                            getSCEV(inst.operand(1)));
                case SDiv:
                    if(val.getType().isIntegral() && val.getType().isSigned())
                    {
                        return SCEVSDivExpr.get(getSCEV(inst.operand(0)),
                                getSCEV(inst.operand(1)));
                    }
                    break;
                case Sub:
                    return getMinusSCEV(getSCEV(inst.operand(0)),
                            getSCEV(inst.operand(1)));
                case Shl:
                    // turn shift left to the multiple operand.
                    if (inst.operand(1) instanceof ConstantInt)
                    {
                        Constant x = ConstantInt.get(val.getType(), 1);
                        ConstantInt ci = (ConstantInt)inst.operand(1);
                        x = ConstantExpr.getShl(x, ci);
                        return SCEVMulExpr.get(getSCEV(inst.operand(0)),
                                getSCEV(x));
                    }
                    break;
                case Phi:
                    return createNodeForPhi((PhiNode)inst);
                default:
                    break;
            }
        }
        return SCEVUnknown.get(val);
    }

    /**
     * We just handle loop phi node resides in loop header block.
     * @param pn
     * @return
     */
    private SCEV createNodeForPhi(PhiNode pn)
    {
        if (pn.getNumberIncomingValues() == 2)
        {
            Loop loop;
            if ((loop = li.getLoopFor(pn.getParent())) != null)
            {
                if (loop.getHeaderBlock().equals(pn.getParent()))
                {
                    // If it lives in the loop header, it has two incoming
                    // values, one from outside the loop, and one from inside.
                    int incomingEdge = loop.contains(pn.getIncomingBlock(0))?1:0;
                    int backEdge = incomingEdge^1;

                    SCEV symbolicName = SCEVUnknown.get(pn);
                    assert !scalars.containsKey(pn) :"Phi node has already processed!";
                    scalars.put(pn, symbolicName);

                    // Using this symbolic name for the PHI, analyze the value coming around
                    // the back-edge.

                    SCEV beval = getSCEV(pn.getIncomingValue(backEdge));

                    if (beval instanceof SCEVAddExpr)
                    {
                        SCEVAddExpr add = (SCEVAddExpr)beval;
                        int foundIndex = add.getNumOperands();

                        for (int i = 0, e = add.getNumOperands(); i < e; i++)
                        {
                            if (add.getOperand(i).equals(symbolicName))
                            {
                                if (foundIndex == e)
                                {
                                    foundIndex = i;
                                    break;
                                }
                            }
                        }

                        //
                        if (foundIndex != add.getNumOperands())
                        {
                            ArrayList<SCEV> ops = new ArrayList<>();
                            for (int i = 0, e = add.getNumOperands(); i < e; i++)
                            {
                                if (i != foundIndex)
                                    ops.add(add.getOperand(i));
                            }

                            SCEV accum = SCEVAddExpr.get(ops);

                            if (accum.isLoopInvariant(loop) ||
                                    ((accum instanceof SCEVAddRecExpr)
                                            && ((SCEVAddRecExpr)accum).getLoop().equals(loop)))
                            {
                                SCEV startVal = getSCEV(pn.getIncomingValue(incomingEdge));
                                SCEV phiSCEV = SCEVAddRecExpr.get(startVal, accum, loop);

                                replaceSymbolicValueWithConcrete(pn, symbolicName, phiSCEV);
                                return phiSCEV;
                            }
                        }
                    }
                    return symbolicName;
                }
            }
        }

        // If it is not a loop phi, wo reject to handle it as yet.
        return SCEVUnknown.get(pn);
    }

    /**
     * This method is called when the specified instruction is needed to
     * replace all reference to symbolic name with concrete value.
     * This is used for PHI resolution.
     * Note that all user of the specified instruction also would be replaced.
     * @param val
     * @param sym
     * @param con
     */
    private void replaceSymbolicValueWithConcrete(Instruction val, SCEV sym, SCEV con)
    {
        // If the specified instruction has not processed as yet, return.
        if (!scalars.containsKey(val)) return;

        SCEV handledVal = scalars.get(val);
        SCEV newVal = handledVal.replaceSymbolicValuesWithConcrete(sym, con);
        if (handledVal.equals(newVal))
            return;     // Exits early if there no change.

        // Updates the scalars map.
        scalars.put(val, newVal);

        // Any other instruction values that uses this instruction also
        // would be processed.
        val.getUseList().forEach(u->
                replaceSymbolicValueWithConcrete((Instruction)u.getUser(), sym, con)
        );
    }

    public boolean hasSCEV(Value val)
    {
        return scalars.containsKey(val);
    }

    public void setSCEV(Value val, SCEV s)
    {
        assert scalars.containsKey(val) : "This entry already existed!";
        scalars.put(val, s);
    }

    /**
     * Given an LLVM value and a loop, return a PHI node
     * in the loop that V is derived from.  We allow arbitrary operations along the
     * way, but the operands of an operation must either be constants or a value
     * derived from a constant PHI.  If this expression does not fit with these
     * constraints, return null.
     * @param val
     * @param loop
     * @return
     */
    private PhiNode getConstantEvolutingPhi(Value val, Loop loop)
    {
        // If this is not an instruction, or if this is an instruction outside of the
        // loop, it can't be derived from a loop PHI.
        if(!(val instanceof Instruction)
                || !loop.contains(((Instruction)val).getParent()))
            return null;
        Instruction inst = (Instruction)val;
        if (inst instanceof PhiNode)
        {
            PhiNode pn = (PhiNode)inst;
            if (loop.getHeaderBlock().equals(inst.getParent()))
                return pn;
            else
                return null;
        }

        // If we won't be able to constant fold this expression even if the operands
        // are constants, return early.
        if(!canConstantFold(inst)) return null;

        // Otherwise, we can evaluate this instruction if all of its operands are
        // constant or derived from a PHI node themselves.
        PhiNode phi = null;
        for (int i = 0, e = inst.getNumOfOperands(); i < e; i++)
        {
            Value opr = inst.operand(i);
            if (opr instanceof GlobalValue || opr instanceof Constant)
            {
                PhiNode p = getConstantEvolutingPhi(opr, loop);
                if (p == null)
                    return null;
                if (phi == null)
                    phi = p;
                else if (phi != p)
                    return null;    // Evolving from multiple different PHIs.
            }
        }
        // This is a expression evolving from a constant PHI!
        return phi;
    }

    private Constant evaluateExpression(Value val, Constant phiVal)
    {
        if (val instanceof PhiNode) return phiVal;
        if (val instanceof GlobalValue)
            return (GlobalValue)val;
        if (val instanceof Constant) return (Constant)val;

        Instruction inst = (Instruction)val;

        ArrayList<Constant> operands = new ArrayList<>();
        for (int i = 0, e = inst.getNumOfOperands(); i< e; i++)
        {
            operands.set(i, evaluateExpression(inst.operand(i), phiVal));
            if (operands.get(i) == null) return null;
        }

        return constantFold(inst, operands);
    }

    private static Constant constantFold(Instruction inst, ArrayList<Constant> operands)
    {
        if (inst instanceof Op2)
            return ConstantExpr.get(inst.getOpcode(), operands.get(0), operands.get(1));

        switch (inst.getOpcode())
        {
            case BitCast:
                return ConstantExpr.getBitCast(operands.get(0), inst.getType());
            case IntToPtr:
                return ConstantExpr.getIntToPtr(operands.get(0), inst.getType());
            case PtrToInt:
                return ConstantExpr.getPtrToInt(operands.get(0), inst.getType());
            case Trunc:
                return ConstantExpr.getTrunc(operands.get(0), inst.getType());
            case SExt:
                return ConstantExpr.getSExt(operands.get(0), inst.getType());
            case ZExt:
                return ConstantExpr.getZExt(operands.get(0), inst.getType());
            case Call:
                if (operands.get(0) instanceof Function)
                {
                    Function gv = (Function)operands.get(0);
                    operands.remove(0);
                    return constantFoldCall(gv, operands);
                }
                return null;

            case GetElementPtr:
                Constant base = operands.get(0);
                operands.remove(0);
                return ConstantExpr.getElementPtr(base, operands);
        }
        return null;
    }

    private static boolean canConstantFold(Instruction inst)
    {
        if (inst instanceof Op2 || inst instanceof CastInst
                || inst instanceof GetElementPtrInst)
            return true;
        Function f;
        if (inst instanceof CallInst)
            if ((f = ((CallInst)inst).getCalledFunction()) != null)
                return canConstantFoldCallTo(f);
        return false;
    }

    /**
     * If we know that the specified Phi is in the header of its containing
     * loop, we know the loop executes a constant number of times, and the
     * PHI node is just a recurrence involving constants, fold it.
     * @param pn
     * @param its
     * @param loop
     * @return
     */
    private Constant getConstantEvolutionLoopExitValue(PhiNode pn, APInt its, Loop loop)
    {
        if (constantEvolutionLoopExitValue.containsKey(pn))
            return constantEvolutionLoopExitValue.get(pn);

        if (its.sge(maxBruteForceIteration))
            return constantEvolutionLoopExitValue.put(pn, null);

        // Since the loop is canonicalized, the PHI node must have two entries.  One
        // entry must be a constant (coming in from outside of the loop), and the
        // second must be derived from the same PHI.
        int secondIsBackedeg = loop.contains(pn.getIncomingBlock(1)) ? 1:0;
        Constant startConst = (Constant) pn.getIncomingValue(secondIsBackedeg^1);
        if (startConst == null)
            return constantEvolutionLoopExitValue.put(pn, null);

        Value beValue = pn.getIncomingValue(secondIsBackedeg);
        PhiNode pn2 = getConstantEvolutingPhi(beValue, loop);
        if (!pn.equals(pn2))
            return constantEvolutionLoopExitValue.put(pn, null);

        // Execute the loop symbolically to determine the exit value.
        int iterationNum = 0;
        if (its.getBitWidth() > 32)
            return constantEvolutionLoopExitValue.put(pn, null);

        for (Constant phiVal = startConst; ; iterationNum++)
        {
            if (its.eq(iterationNum))
                return constantEvolutionLoopExitValue.put(pn, phiVal);

            Constant nextPhi = evaluateExpression(beValue, phiVal);
            if (nextPhi.equals(phiVal))
                return constantEvolutionLoopExitValue.put(pn, nextPhi);
            if (nextPhi == null)
                return null;
            phiVal = nextPhi;
        }
    }

    /**
     * Compute the value of the expression within the indicated loop
     * (which may be null to indicate in no loop). If the expression can
     * not be evaluated, return SCEVUnknown Value.
     * @param val
     * @param loop
     * @return
     */
    public SCEV getSCEVAtScope(SCEV val, Loop loop)
    {
        if (val instanceof SCEVConstant) return val;

        // If this instruction is evolves from a constant-evolving PHI, compute the
        // exit value from the loop without using SCEVs.
        SCEVUnknown su;
        Instruction inst;
        PhiNode pn;
        if (val instanceof SCEVUnknown)
        {
            su = (SCEVUnknown) val;
            if ((inst = (Instruction)(su.getValue()))!=null)
            {
                Loop li = this.li.getLoopFor(inst.getParent());
                if (li != null && li.getParentLoop() == loop)  // Looking for loop exit value.
                    if (inst instanceof PhiNode)
                    {
                        pn = (PhiNode) inst;
                        if (pn.getParent() == li.getHeaderBlock())
                        {
                            // Okay, there is no closed form solution for the PHI node.  Check
                            // to see if the loop that contains it has a known iteration count.
                            // If so, we may be able to force computation of the exit value.
                            SCEV iterationCount = getIterationCount(li);
                            SCEVConstant icc;
                            if (iterationCount instanceof SCEVConstant)
                            {
                                icc = (SCEVConstant)iterationCount;
                                // Okay, we know how many times the containing loop executes.  If
                                // this is a constant evolving PHI node, get the final value at
                                // the specified iteration number.
                                Constant rv = getConstantEvolutionLoopExitValue(
                                        pn, icc.getValue().getValue(), li);
                                if (rv != null)
                                    return SCEVUnknown.get(rv);
                            }
                        }
                    }

                // Okay, this is a some expression that we cannot symbolically evaluate
                // into a SCEV.  Check to see if it's possible to symbolically evaluate
                // the arguments into constants, and if see, try to constant propagate the
                // result.  This is particularly useful for computing loop exit values.
                if (canConstantFold(inst))
                {
                    ArrayList<Constant> operands = new ArrayList<>(inst.getNumOfOperands());

                    for (int i = 0, e = inst.getNumOfOperands(); i < e; ++i)
                    {
                        Value op = inst.operand(i);
                        if (op instanceof Constant)
                        {
                            operands.add((Constant)op);
                        }
                        else
                        {
                            SCEV opV = getSCEVAtScope(getSCEV(op), loop);
                            if (opV instanceof SCEVConstant)
                            {
                                SCEVConstant sc = (SCEVConstant)opV;
                                operands.add(ConstantExpr
                                        .getCast(Operator.BitCast,
                                                sc.getValue(), op.getType()));
                            }
                            else if (opV instanceof SCEVUnknown)
                            {
                                su = (SCEVUnknown)opV;
                                if (su.getValue() instanceof Constant)
                                    operands.add(ConstantExpr.getCast(
                                            Operator.BitCast,
                                            (Constant)su.getValue(),
                                            op.getType()));
                                else
                                    return val;
                            }
                            else
                            {
                                return val;
                            }
                        }
                    }
                    return SCEVUnknown.get(constantFold(inst, operands));
                }
            }

            // This is some other type of SCEVUnknown, just return it.
            return val;
        }

        if (val instanceof SCEVCommutativeExpr)
        {
            SCEVCommutativeExpr comm = (SCEVCommutativeExpr)val;
            // Avoid performing the lookup-up in the common case where the specified
            // expression has no loop-variant portions.
            for (int i = 0, e = comm.getNumOperands(); i != e; ++i) {
                SCEV opAtScope = getSCEVAtScope(comm.getOperand(i), loop);
                if (opAtScope != comm.getOperand(i)) {
                    if (opAtScope == unknownValue) return unknownValue;
                    // Okay, at least one of these operands is loop variant but might be
                    // foldable.  Build a new instance of the folded commutative expression.
                    ArrayList<SCEV> newOps = new ArrayList<>();
                    for (int j = 0; j < i; j++)
                        newOps.add(comm.getOperand(j));
                    newOps.add(opAtScope);

                    for (++i; i != e; ++i)
                    {
                        opAtScope = getSCEVAtScope(comm.getOperand(i), loop);
                        if (opAtScope == unknownValue) return unknownValue;
                        newOps.add(opAtScope);
                    }
                    if (comm instanceof SCEVAddExpr)
                        return SCEVAddExpr.get(newOps);
                    assert(comm instanceof SCEVMulExpr) : "Only know about add and mul!";
                    return SCEVMulExpr.get(newOps);
                }
            }
            // If we got here, all operands are loop invariant.
            return comm;
        }

        if (val instanceof SCEVSDivExpr)
        {
            SCEVSDivExpr div = (SCEVSDivExpr)val;
            SCEV LHS = getSCEVAtScope(div.getLHS(), loop);
            if (LHS == unknownValue) return LHS;
            SCEV RHS = getSCEVAtScope(div.getRHS(), loop);
            if (RHS == unknownValue) return RHS;
            if (LHS == div.getLHS() && RHS == div.getRHS())
                return div;   // must be loop invariant
            return SCEVSDivExpr.get(LHS, RHS);
        }

        // If this is a loop recurrence for a loop that does not contain loop, then we
        // are dealing with the final value computed by the loop.
        if (val instanceof SCEVAddRecExpr)
        {
            SCEVAddRecExpr addRec = (SCEVAddRecExpr)val;
            if (loop == null || !addRec.getLoop().contains(loop.getHeaderBlock()))
            {
                // To evaluate this recurrence, we need to know how many times the addRec
                // loop iterates.  Compute this now.
                SCEV IterationCount = getIterationCount(addRec.getLoop());
                if (IterationCount == unknownValue) return unknownValue;
                IterationCount = getTruncateOrZeroExtend(IterationCount,
                        addRec.getType());

                // If the value is affine, simplify the expression evaluation to just
                // Start + Step*IterationCount.
                if (addRec.isAffine())
                    return SCEVAddExpr.get(addRec.getStart(),
                            SCEVMulExpr.get(IterationCount,
                                    addRec.getOperand(1)));

                // Otherwise, evaluate it the hard way.
                return addRec.evaluateAtIteration(IterationCount);
            }
            return unknownValue;
        }
        return unknownValue;
    }

	/**
	 * This is a convenience function which does getSCEVAtScope(getSCEV(val), loop).
     * @param val
     * @param loop
     * @return
     */
    public SCEV getSCEVAtScope(Value val, Loop loop)
    {
        return getSCEVAtScope(getSCEV(val), loop);
    }

    /**
     * Return true if the specified loop has
     * an analyzable loop-invariant iteration count.
     * @param loop
     * @return
     */
    public boolean hasLoopInvariantIterationCount(Loop loop)
    {
        return !(getIterationCount(loop) instanceof SCEVCouldNotCompute);
    }

    /**
     * If the specified loop has a predictable iteration count, return it.
     * Note that it is not valid to call this method on a loop without a
     * loop-invariant iteration count.
     * @param loop
     * @return
     */
    public SCEV getIterationCount(Loop loop)
    {
        if(!iterationCount.containsKey(loop))
        {
            SCEV itCount = computeIterationCount(loop);
            iterationCount.put(loop, itCount);
            if (itCount != unknownValue)
            {
                assert itCount.isLoopInvariant(loop)
                        :"Computed tri count is not loop invariant!";
                ++numTripCountsComputed;
            }
        }
        return iterationCount.get(loop);
    }

    /**
     * Compute the number of times the specified loop will iterate.
     * @param loop
     * @return
     */
    private SCEV computeIterationCount(Loop loop)
    {
        // If the loop has a non-one exit block count, we can't analyze it.
        ArrayList<BasicBlock> exitBlocks = loop.getExitingBlocks();

        if (exitBlocks.size() != 1) return unknownValue;

        // Okay, there is one exit block.  Try to find the condition that causes the
        // loop to be exited.
        BasicBlock exitBlock = exitBlocks.get(0);

        BasicBlock exitingBlock = null;
        for (PredIterator<BasicBlock> pi = exitBlock.predIterator(); pi.hasNext();)
        {
            BasicBlock pred = pi.next();
            if (loop.contains(pred))
            {
                if (exitingBlock == null)
                    exitingBlock = pred;
                else
                    return unknownValue;   // More than one block exiting!
            }
        }
        assert exitingBlock != null : "No exits from loop, something is broken!";

        // Okay, we've computed the exiting block.  See what condition causes us to
        // exit.
        //
        // FIXME: we should be able to handle switch instructions (with a single exit)
        // FIXME: We should handle cast of int to bool as well
        TerminatorInst ti = exitingBlock.getTerminator();
        if (ti instanceof BranchInst)
        {
            BranchInst exitBr = (BranchInst)ti;
            assert exitBr.isConditional() : "If unconditional, it can't be in loop!";
            return computeIterationCountExhaustively(loop, exitBr.getCondition(),
                    exitBr.suxAt(0).equals(exitBlock));
        }
        return unknownValue;
    }

    /**
     * If the trip is known to execute a constant number of times
     * (the condition evolves only from constants), try to evaluate a few
     * iterations of the loop until we get the exit condition gets a value
     * of ExitWhen (true or false).  If we cannot evaluate the trip count
     * of the loop, return UnknownValue.
     * @param loop
     * @param condVal
     * @param exitOnTrue    Indicates whether branch to the exit block when
     *                      loop condition is true.
     * @return
     */
    private SCEV computeIterationCountExhaustively(Loop loop, Value condVal,
            boolean exitOnTrue)
    {
        PhiNode pn = getConstantEvolutingPhi(condVal, loop);
        if (pn == null) return unknownValue;

        int secondIsBackedge = loop.contains(pn.getIncomingBlock(1))?1:0;
        Constant startConst;
        Value incomingValue = pn.getIncomingValue(secondIsBackedge^1);
        if (incomingValue instanceof Constant)
            startConst = (Constant)incomingValue;
        else
            return unknownValue;

        Value beval = pn.getIncomingValue(secondIsBackedge);
        PhiNode pn2 = getConstantEvolutingPhi(beval, loop);
        if (!pn.equals(pn2)) return unknownValue;

        // Okay, we find a PHI node that defines the trip count of this loop.  Execute
        // the loop symbolically to determine when the condition gets a value of
        // "exitOnTrue".
        int iterationNum = 0;
        int maxIteration = maxBruteForceIteration;
        for (Constant phiVal = startConst; iterationNum < maxIteration; iterationNum++)
        {
            Constant res = evaluateExpression(condVal, phiVal);
            if (!(res instanceof ConstantInt))
                return unknownValue;
            ConstantInt cond = (ConstantInt)res;
            boolean isTrue = cond.equalsInt(1);
            if (isTrue == exitOnTrue)
            {
                constantEvolutionLoopExitValue.put(pn, phiVal);
                return SCEVConstant.get(ConstantInt.get(Type.Int32Ty, iterationNum));
            }

            // Compute the value of the PHI node for the next iteration.
            Constant nextPhi = evaluateExpression(beval, phiVal);
            if (nextPhi == null || nextPhi.equals(phiVal))
                return unknownValue;
            phiVal = nextPhi;
        }
        // Too many iterations were needed to evaluate.
        return unknownValue;
    }

    /**
     * This method should be called by the client before it removes an
     * instruction from the program, to make sure that no dangling references
     * are left around.
     * @param inst
     */
    public void removeInstructionFromRecords(Instruction inst)
    {
        scalars.remove(inst);
        if (inst instanceof PhiNode)
            constantEvolutionLoopExitValue.remove(inst);
    }

    public int getTypeSizeBits(Type ty)
    {
        assert isSCEVable(ty) :"Type is not SCEVable!";

        if (td != null)
            return (int)td.getTypeSizeInBits(ty);

        if (ty.isIntegral())
            return ty.getPrimitiveSizeInBits();

        assert (ty instanceof PointerType) :"isSCEVable permitted a non SCEVable type!";
        return 64;
    }

    public boolean isSCEVable(Type type)
    {
        return type.isIntegral() || (type instanceof PointerType);
    }

    /**
     * Return a SCEV corresponding to a conversion of the
     * input value to the specified type.  If the type must be extended, it is zero
     * extended.
     * @param value
     * @param ty
     * @return
     */
    public SCEV getTruncateOrZeroExtend(SCEV value, Type ty)
    {
        Type srcTy = value.getType();
        assert isSCEVable(srcTy);

        int diff = srcTy.getPrimitiveSize() - ty.getPrimitiveSize();
        return diff == 0?value: diff < 0 ?
                SCEVZeroExtendExpr.get(value, ty): SCEVTruncateExpr.get(value, ty);
    }

    public SCEV getIntegerSCEV(int val, Type ty)
    {
        Constant c;
        if (val == 0)
            c = Constant.getNullValue(ty);
        else if (ty.isFloatingPointType())
            c = ConstantFP.get(ty, val);
        else
        {
            assert ty.isIntegral():"Integral type is required.";
            c = ConstantInt.get(ty, val);
        }
        return SCEVUnknown.get(c);
    }

    /***
     * Returns a SCEV corresponding to -val = -1 * val.
     * @return
     */
    public SCEV getNegativeSCEV(SCEV val)
    {
        if (val instanceof SCEVConstant)
        {
            SCEVConstant vc = (SCEVConstant)val;
            return SCEVUnknown.get(ConstantExpr.getNeg(vc.getValue()));
        }
        return SCEVMulExpr.get(val, getIntegerSCEV(-1, val.getType()));
    }

    /**
     * Returns a SCEV corresponding to {@code lhs - rhs} that would
     * be converted to {@code lhs + (-rhs)}.
     * @param lhs
     * @param rhs
     * @return
     */
    public SCEV getMinusSCEV(SCEV lhs, SCEV rhs)
    {
        return SCEVAddExpr.get(lhs, getNegativeSCEV(rhs));
    }

	/**
     * Return a SCEV corresponding to ~V = -1-V.
     * @param val
     * @return
     */
    public SCEV getNotSCEV(SCEV val)
    {
        if (val instanceof SCEVConstant)
        {
            SCEVConstant sc = (SCEVConstant)val;
            return SCEVConstant.get((ConstantInt) ConstantExpr.getNot(sc.getValue()));
        }

        Type ty = val.getType();
        ty = getEffectiveSCEVType(ty);
        SCEV allOnes = SCEVConstant.get((ConstantInt) Constant.getAllOnesValue(ty));

        return getMinusSCEV(allOnes, val);
    }

    public Type getEffectiveSCEVType(Type srcTy)
    {
        assert isSCEVable(srcTy):"Type is not SCEVable!";
        if (srcTy.isIntegral())
            return srcTy;
        assert srcTy.isPointerType():"Unexpected non-pointer type!";
        if (td != null)
            return td.getIntPtrType();

        return Type.Int64Ty;
    }

	/**
     * Returns a SCEV object that performs conversion of converting
     * input value to the given destTy.
     * @param src
     * @param destTy
     * @return
     */
    public SCEV getTruncateOrNoop(SCEV src, Type destTy)
    {
        Type srcTy = src.getType();
        assert (srcTy.isIntegral() || srcTy.isPointerType())
                &&(destTy.isIntegral() || destTy.isPointerType())
                :"Cannot truncate or noop with non-integer type!";
        int destBits = getTypeSizeBits(destTy);
        int srcBits = getTypeSizeBits(srcTy);
        assert destBits <= srcBits;
        if (destBits == srcBits)
            return src;
        return getTruncateExpr(src, destTy);
    }

    private SCEV getTruncateExpr(SCEV val, Type destTy)
    {
        return null;
    }

    public SCEV getNoopOrAnyExtend(SCEV src, Type destTy)
    {
        Type srcTy = src.getType();
        assert (srcTy.isIntegral() || srcTy.isPointerType())
                &&(destTy.isIntegral() || destTy.isPointerType())
                :"Cannot extend or noop with non-integer type!";
        int destBits = getTypeSizeBits(destTy);
        int srcBits = getTypeSizeBits(srcTy);
        assert destBits >= srcBits;
        if (destBits == srcBits)
            return src;
        return getAnyExtendExpr(src, destTy);
    }

    public SCEV getNoopOrZeroExtend(SCEV s, Type ty)
    {
        Type srcTy = s.getType();
        assert (srcTy.isIntegral() || srcTy.isPointerType())
                && (ty.isIntegral() || ty.isPointerType())
                :"Cannot noop or zero extend with non-integer arguments!";
        assert getTypeSizeBits(srcTy) <= getTypeSizeBits(ty);
        if (getTypeSizeBits(srcTy) == getTypeSizeBits(ty))
            return s;
        return getZeroExtendExpr(s, ty);

    }

    private SCEV getZeroExtendExpr(SCEV val, Type ty)
    {
        return null;
    }

    private SCEV getSignExtendExpr(SCEV val, Type ty)
    {
        return null;
    }

    private SCEV getAnyExtendExpr(SCEV val, Type destTy)
    {
        return null;
    }

	/**
     * Test if the entry to the loop is protected by a conditional between
     * {@code lhs} and {@code rhs}. this is used for help avoid max expression
     * in loop trip counts, and to eliminate casts.
     * @param loop
     * @param pred
     * @param lhs
     * @param rhs
     * @return
     */
    public boolean isLoopGuardedByCond(Loop loop,
            Predicate pred,
            SCEV lhs,
            SCEV rhs)
    {
        if (loop == null) return false;

        BasicBlock predecessor = getLoopPredecessor(loop);
        BasicBlock predecessorDest = loop.getHeaderBlock();

        // Starting at the loop predecessor, climb up the predecessor chain, as long
        // as there are predecessors that can be found that have unique successors
        // leading to the original header.
        while (predecessor != null)
        {
            BranchInst loopEntryPredicate;
            TerminatorInst ti = predecessor.getTerminator();
            if (!(ti instanceof BranchInst) || ((loopEntryPredicate = (BranchInst)ti).isUnconditional()))
                continue;

            if (isImpliedCond(loopEntryPredicate.getCondition(), pred, lhs, rhs,
                    loopEntryPredicate.suxAt(0) != predecessorDest))
                return true;

            predecessorDest = predecessor;
            predecessor = getPredecessorWithUniqueSuccessorForBB(predecessor);
        }
        return false;
    }

    private BasicBlock getPredecessorWithUniqueSuccessorForBB(BasicBlock bb)
    {
        BasicBlock pred = bb.getSinglePredecessor();
        if (pred != null)
            return pred;

        // A loop's header is defined to be a block that dominates the loop.
        // If the header has a unique predecessor outside the loop, it must be
        // a block that has exactly one successor that can reach the loop.
        Loop l = li.getLoopFor(bb);
        if (l != null)
            return getLoopPredecessor(l);
        return null;
    }

	/**
	 * If the given loop's header has exactly one unique
     * predecessor outside the loop, return it. Otherwise return null.
     * @param loop
     * @return
     */
    private BasicBlock getLoopPredecessor(Loop loop)
    {
        BasicBlock header = loop.getHeaderBlock();
        BasicBlock pred = null;
        for (PredIterator itr = header.predIterator(); itr.hasNext();)
        {
            BasicBlock bb = itr.next();
            if (!(loop.contains(bb)))
            {
                if (pred != null && pred != bb) return null;
                pred = bb;
            }
        }
        return pred;
    }

	/**
	 * Test if the condition described by pred, lhs and rhs is true when
     * the given cond value is evaluated to true.
     * @param condValue
     * @param pred
     * @param lhs
     * @param rhs
     * @param inverse
     * @return
     */
    private boolean isImpliedCond(Value condValue, Predicate pred,
            SCEV lhs, SCEV rhs, boolean inverse)
    {
        // Recursively handle And and Or conditions.
        if (condValue instanceof Op2)
        {
            Op2 bo = (Op2)condValue;
            if (bo.getOpcode() == Operator.And)
            {
                if (!inverse)
                    return isImpliedCond(bo.operand(0), pred, lhs, rhs, inverse)
                    || isImpliedCond(bo.operand(1), pred, lhs, rhs, inverse);
            }
            else if (bo.getOpcode() == Operator.Or)
            {
                if (inverse)
                    return isImpliedCond(bo.operand(0), pred, lhs, rhs, inverse)
                            || isImpliedCond(bo.operand(1), pred, lhs, rhs, inverse);
            }
        }

        if (!(condValue instanceof ICmpInst)) return false;
        ICmpInst ici = (ICmpInst)condValue;

        if (getTypeSizeBits(lhs.getType()) < getTypeSizeBits(ici.operand(0).getType()))
            return false;

        Predicate foundPred;
        if (inverse)
            foundPred = ici.getInversePredicate();
        else
            foundPred = ici.getPredicate();

        SCEV foundLHS = getSCEV(ici.operand(0));
        SCEV foundRHS = getSCEV(ici.operand(1));

        // Balance the types. The case where FoundLHS' type is wider than
        // LHS' type is checked for above.
        if (getTypeSizeBits(lhs.getType()) >
                getTypeSizeBits(foundLHS.getType()))
        {
            if (CmpInst.isSigned(pred))
            {
                foundLHS = getSignExtendExpr(foundLHS, lhs.getType());
                foundRHS = getSignExtendExpr(foundRHS, rhs.getType());
            }
            else
            {
                foundLHS = getZeroExtendExpr(foundLHS, lhs.getType());
                foundRHS = getZeroExtendExpr(foundRHS, rhs.getType());
            }
        }

        if (lhs instanceof SCEVConstant)
        {
            SCEV temp = lhs;
            lhs = rhs;
            rhs = temp;
            pred = ICmpInst.getSwappedPredicate(pred);
        }

        if (rhs instanceof SCEVConstant)
        {
            SCEVConstant rc = (SCEVConstant)rhs;
            APInt ra = rc.getValue().getValue();
            switch (pred)
            {
                default:
                    Util.shouldNotReachHere("Unexpected ICmpInst::Predecaite value!");
                case ICMP_EQ:
                case ICMP_NE:
                    break;
                case ICMP_UGE:
                    if (ra.decrease().isMinValue())
                    {
                        pred = Predicate.ICMP_NE;
                        rhs = SCEVConstant.get(ra.decrease());
                        break;
                    }
                    if (ra.isMaxValue())
                    {
                        pred = Predicate.ICMP_EQ;
                        break;
                    }
                    if (ra.isMinValue()) return true;
                    break;
                case ICMP_ULE:
                    if (ra.increase().isMaxValue())
                    {
                        pred = Predicate.ICMP_NE;
                        rhs = SCEVConstant.get(ra.increase());
                        break;
                    }
                    if (ra.isMinValue())
                    {
                        pred = Predicate.ICMP_EQ;
                        break;
                    }
                    if (ra.isMaxValue()) return true;
                    break;
                case ICMP_SGE:
                    if (ra.decrease().isMinSignedValue())
                    {
                        pred = Predicate.ICMP_NE;
                        rhs = SCEVConstant.get(ra.decrease());
                        break;
                    }
                    if (ra.isMaxSignedValue())
                    {
                        pred = Predicate.ICMP_EQ;
                        break;
                    }
                    if (ra.isMinSignedValue()) return true;
                    break;
                case ICMP_SLE:
                    if (ra.increase().isMaxSignedValue())
                    {
                        pred = Predicate.ICMP_NE;
                        rhs = SCEVConstant.get(ra.increase());
                        break;
                    }
                    if (ra.isMinSignedValue())
                    {
                        pred = Predicate.ICMP_EQ;
                        break;
                    }
                    if (ra.isMaxSignedValue()) return true;
                    break;
                case ICMP_UGT:
                    if (ra.isMinValue())
                    {
                        pred = Predicate.ICMP_NE;
                        break;
                    }
                    if (ra.increase().isMaxValue())
                    {
                        pred = Predicate.ICMP_EQ;
                        rhs = SCEVConstant.get(ra.increase());
                        break;
                    }
                    if (ra.isMaxValue()) return false;
                    break;
                case ICMP_ULT:
                    if (ra.isMaxValue())
                    {
                        pred = Predicate.ICMP_NE;
                        break;
                    }
                    if (ra.decrease().isMinValue())
                    {
                        pred = Predicate.ICMP_EQ;
                        rhs = SCEVConstant.get(ra.decrease());
                        break;
                    }
                    if (ra.isMinValue()) return false;
                    break;
                case ICMP_SGT:
                    if (ra.isMinSignedValue())
                    {
                        pred = Predicate.ICMP_NE;
                        break;
                    }
                    if (ra.increase().isMaxSignedValue())
                    {
                        pred = Predicate.ICMP_EQ;
                        rhs = SCEVConstant.get(ra.increase());
                        break;
                    }
                    if (ra.isMaxSignedValue()) return false;
                    break;
                case ICMP_SLT:
                    if (ra.isMaxSignedValue())
                    {
                        pred = Predicate.ICMP_NE;
                        break;
                    }
                    if (ra.decrease().isMinSignedValue())
                    {
                        pred = Predicate.ICMP_EQ;
                        rhs = SCEVConstant.get(ra.decrease());
                        break;
                    }
                    if (ra.isMinSignedValue()) return false;
                    break;
            }
        }

        if (lhs == foundRHS || rhs == foundLHS)
        {
            if (rhs instanceof SCEVConstant)
            {
                SCEV temp = foundLHS;
                foundLHS = foundRHS;
                foundRHS = temp;
                foundPred = ICmpInst.getSwappedPredicate(foundPred);
            }
            else
            {
                SCEV temp = lhs;
                lhs = rhs;
                rhs = temp;
                pred = ICmpInst.getSwappedPredicate(pred);
            }
        }

        if (foundPred == pred)
            return isImpliedCondOperands(pred, lhs, rhs, foundLHS, foundRHS);

        if (ICmpInst.getSwappedPredicate(foundPred) == pred)
        {
            if (rhs instanceof SCEVConstant)
                return isImpliedCondOperands(pred, lhs, rhs, foundRHS, foundLHS);
            else
                return isImpliedCondOperands(ICmpInst.getSwappedPredicate(pred),
                        rhs, lhs, foundLHS, foundRHS);
        }

        if (foundPred == Predicate.ICMP_EQ)
        {
            if (ICmpInst.isTrueWhenEqual(pred))
                if (isImpliedCondOperands(pred, lhs, rhs, foundLHS, foundRHS))
                    return true;
        }

        if (pred == Predicate.ICMP_NE)
        {
            if (!ICmpInst.isTrueWhenEqual(foundPred))
                if (isImpliedCondOperands(foundPred, lhs, rhs, foundLHS, foundRHS))
                    return true;
        }
        // Otherwise, assume the worst.
        return false;
    }

	/**
	 * Test whether the condition described by Pred,
     * LHS, and RHS is true whenever the condition desribed by Pred, FoundLHS,
     * and FoundRHS is true.
     * @param pred
     * @param lhs
     * @param rhs
     * @param foundLHS
     * @param foundRHS
     * @return
     */
    private boolean isImpliedCondOperands(Predicate pred,
            SCEV lhs, SCEV rhs, SCEV foundLHS, SCEV foundRHS)
    {
        return isImpliedCondOperandsHelper(pred, lhs, rhs, foundLHS, foundRHS)  ||
                // ~x < ~y --> x > y
                isImpliedCondOperandsHelper(pred, lhs, rhs, getNotSCEV(foundRHS),
                getNotSCEV(foundLHS));
    }

    /*
	 * Test whether the condition described by Pred,
     * LHS, and RHS is true whenever the condition desribed by Pred, FoundLHS,
     * and FoundRHS is true.
     */
    private boolean isImpliedCondOperandsHelper(Predicate pred,
            SCEV lhs, SCEV rhs, SCEV foundLHS, SCEV foundRHS)
    {
        switch (pred)
        {
            default:
                Util.shouldNotReachHere("Unexpected Predicate value!");
                break;
            case ICMP_EQ:
            case ICMP_NE:
                if (hasSameValue(lhs, foundLHS) && hasSameValue(rhs, foundRHS))
                    return true;
                break;
            case ICMP_SLT:
            case ICMP_SLE:

        }
        return false;
    }

    public static boolean hasSameValue(SCEV s1, SCEV s2)
    {
        if (Objects.equals(s1, s2)) return true;

        if (s1 instanceof SCEVUnknown && s2 instanceof SCEVUnknown)
        {
            SCEVUnknown su1 = (SCEVUnknown)s1;
            SCEVUnknown su2 = (SCEVUnknown)s2;
            if (su1.getValue() instanceof Instruction
                    && su2.getValue() instanceof Instruction)
            {
                Instruction inst1 = (Instruction)su1.getValue();
                Instruction inst2 = (Instruction)su2.getValue();
                if (inst1.isIdenticalTo(inst2))
                    return true;
            }
        }
        // Otherwise assume they may have a different value.
        return false;
    }

    private boolean isKnownNegative(SCEV val)
    {
        return getSignedRange(val).getSignedMax().isNegative();
    }

    private boolean isKnownPositive(SCEV val)
    {
        return getSignedRange(val).getSignedMin().isStrictlyPositive();
    }

    private boolean isKnownNonNegative(SCEV val)
    {
        return !getSignedRange(val).getSignedMin().isNegative();
    }

    private boolean isKnownNonPositive(SCEV val)
    {
        return !getSignedRange(val).getSignedMax().isStrictlyPositive();
    }

    private boolean isKnownNonZero(SCEV val)
    {
        return isKnownNegative(val) || isKnownPositive(val);
    }

    private boolean isKnownPredicate(Predicate pred, SCEV lhs, SCEV rhs)
    {
        if (hasSameValue(lhs, rhs))
            return ICmpInst.isTrueWhenEqual(pred);

        switch (pred)
        {
            default:
                Util.shouldNotReachHere("Unexpected ICmpInst::Predicate value!");
                break;
            case ICMP_SGT:
            {
                pred = Predicate.ICMP_SLT;
                SCEV temp = lhs;
                lhs = rhs;
                rhs = temp;
                break;
            }
            case ICMP_SLT:
            {
                ConstantRange lhsRange = getSignedRange(lhs);
                ConstantRange rhsRange = getSignedRange(rhs);
                if (lhsRange.getSignedMax().slt(rhsRange.getSignedMin()))
                    return true;
                if (lhsRange.getSignedMin().sge(rhsRange.getSignedMax()))
                    return false;
                break;
            }
            case ICMP_SGE:
            {
                pred = Predicate.ICMP_SLE;
                SCEV temp = lhs;
                lhs = rhs;
                rhs = temp;
                break;
            }
            case ICMP_SLE:
            {
                ConstantRange lhsRange = getSignedRange(lhs);
                ConstantRange rhsRange = getSignedRange(rhs);
                if (lhsRange.getSignedMax().sle(rhsRange.getSignedMin()))
                    return true;
                if (lhsRange.getSignedMin().sgt(rhsRange.getSignedMax()))
                    return false;
                break;
            }
            case ICMP_UGT:
            {
                pred = Predicate.ICMP_ULT;
                SCEV temp = lhs;
                lhs = rhs;
                rhs = temp;
                break;
            }
            case ICMP_ULT:
            {
                ConstantRange lhsRange = getUnsignedRange(lhs);
                ConstantRange rhsRange = getUnsignedRange(rhs);
                if (lhsRange.getUnsignedMax().slt(rhsRange.getUnsignedMin()))
                    return true;
                if (lhsRange.getUnsignedMin().sge(rhsRange.getUnsignedMax()))
                    return false;
                break;
            }
            case ICMP_UGE:
            {
                pred = Predicate.ICMP_ULE;
                SCEV temp = lhs;
                lhs = rhs;
                rhs = temp;
                break;
            }
            case ICMP_ULE:
            {
                ConstantRange lhsRange = getUnsignedRange(lhs);
                ConstantRange rhsRange = getUnsignedRange(rhs);
                if (lhsRange.getUnsignedMax().ult(rhsRange.getUnsignedMin()))
                    return true;
                if (lhsRange.getUnsignedMin().uge(rhsRange.getUnsignedMax()))
                    return false;
                break;
            }
            case ICMP_NE:
            {
                if (getUnsignedRange(lhs).intersectWith(getUnsignedRange(rhs)).isEmptySet())
                    return true;
                if (getSignedRange(lhs).intersectWith(getSignedRange(rhs)).isEmptySet())
                    return true;

                SCEV diff = getMinusSCEV(lhs, rhs);
                if (isKnownNonZero(diff))
                    return true;
                break;
            }
            case ICMP_EQ:
                break;
        }
        return false;
    }

    private ConstantRange getUnsignedRange(SCEV s)
    {
        if (s instanceof SCEVConstant)
        {
            SCEVConstant c = (SCEVConstant)s;
            return new ConstantRange(c.getValue().getValue());
        }

        if (s instanceof SCEVAddExpr)
        {
            SCEVAddExpr add = (SCEVAddExpr)s;
            ConstantRange x = getUnsignedRange(add.getOperand(0));
            for (int i = 1, e = add.getNumOperands(); i < e; i++)
                x = x.add(getUnsignedRange(add.getOperand(i)));
            return x;
        }
        if (s instanceof SCEVMulExpr)
        {
            SCEVMulExpr mul = (SCEVMulExpr)s;
            ConstantRange x = getUnsignedRange(mul.getOperand(0));
            for (int i = 1, e = mul.getNumOperands(); i < e; i++)
                x = x.add(getUnsignedRange(mul.getOperand(i)));
            return x;
        }

        if (s instanceof SCEVSDivExpr)
        {
            SCEVSDivExpr div = (SCEVSDivExpr)s;
            ConstantRange x = getUnsignedRange(div.getLHS());
            ConstantRange y = getUnsignedRange(div.getRHS());
            return x.udiv(y);
        }

        ConstantRange fullset = new ConstantRange(getTypeSizeBits(s.getType()), true);
        if (s instanceof SCEVAddRecExpr)
        {
            SCEVAddRecExpr addRec = (SCEVAddRecExpr)s;
            SCEV t = getIterationCount(addRec.getLoop());
            if (!(t instanceof SCEVConstant))
                return fullset;

            SCEVConstant tripCount = (SCEVConstant)t;
            if (addRec.isAffine())
            {
                Type ty = addRec.getType();
                SCEV maxBECount = getMaxIterationCount(addRec.getLoop());
                if (getTypeSizeBits(maxBECount.getType())
                        <= getTypeSizeBits(ty))
                {
                    maxBECount = getNoopOrZeroExtend(maxBECount, ty);
                    SCEV start = addRec.getStart();
                    SCEV step = addRec.getStepRecurrence();
                    SCEV end = addRec.evaluateAtIteration(maxBECount);


                    // Check for overflow.
                    if (!step.isOne() && isKnownPredicate(Predicate.ICMP_ULT,
                            start, end) && !(step.isAllOnesValue()
                    && isKnownPredicate(Predicate.ICMP_UGT, start, end)))
                        return fullset;

                    ConstantRange startRange = getUnsignedRange(start);
                    ConstantRange endRange = getUnsignedRange(end);
                    APInt min = APInt.umin(startRange.getUnsignedMin(),
                            endRange.getUnsignedMin());
                    APInt max = APInt.umax(startRange.getUnsignedMax(),
                            endRange.getUnsignedMax());
                    if (min.isMinValue() && max.isMaxValue())
                        return fullset;
                    return new ConstantRange(min, max.increase());
                }
            }
        }

        if (s instanceof SCEVUnknown)
        {
            SCEVUnknown u = (SCEVUnknown)s;
            int bitwidth = getTypeSizeBits(u.getType());
            APInt mask = APInt.getAllOnesValue(bitwidth);
            APInt zeros = new APInt(bitwidth, 0), ones = new APInt(bitwidth, 0);
            ValueTracking.computeMaskedBits(u.getValue(), mask, zeros, ones, td);;

            APInt tmp = zeros.negative().increase();
            if (ones.eq(tmp))
                return fullset;
            return new ConstantRange(ones, tmp);
        }
        return fullset;
    }

    private ConstantRange getSignedRange(SCEV s)
    {
        if (s instanceof SCEVConstant)
        {
            SCEVConstant c = (SCEVConstant)s;
            return new ConstantRange(c.getValue().getValue());
        }

        if (s instanceof SCEVAddExpr)
        {
            SCEVAddExpr add = (SCEVAddExpr)s;
            ConstantRange x = getSignedRange(add.getOperand(0));
            for (int i = 1, e = add.getNumOperands(); i < e; i++)
                x = x.add(getSignedRange(add.getOperand(i)));
            return x;
        }
        if (s instanceof SCEVMulExpr)
        {
            SCEVMulExpr mul = (SCEVMulExpr)s;
            ConstantRange x = getSignedRange(mul.getOperand(0));
            for (int i = 1, e = mul.getNumOperands(); i < e; i++)
                x = x.add(getSignedRange(mul.getOperand(i)));
            return x;
        }

        if (s instanceof SCEVSDivExpr)
        {
            SCEVSDivExpr div = (SCEVSDivExpr)s;
            ConstantRange x = getSignedRange(div.getLHS());
            ConstantRange y = getSignedRange(div.getRHS());
            return x.udiv(y);
        }

        ConstantRange fullset = new ConstantRange(getTypeSizeBits(s.getType()), true);
        if (s instanceof SCEVAddRecExpr)
        {
            SCEVAddRecExpr addRec = (SCEVAddRecExpr)s;
            SCEV t = getIterationCount(addRec.getLoop());
            if (!(t instanceof SCEVConstant))
                return fullset;

            SCEVConstant tripCount = (SCEVConstant)t;
            if (addRec.isAffine())
            {
                Type ty = addRec.getType();
                SCEV maxBECount = getMaxIterationCount(addRec.getLoop());
                if (getTypeSizeBits(maxBECount.getType())
                        <= getTypeSizeBits(ty))
                {
                    maxBECount = getNoopOrZeroExtend(maxBECount, ty);
                    SCEV start = addRec.getStart();
                    SCEV step = addRec.getStepRecurrence();
                    SCEV end = addRec.evaluateAtIteration(maxBECount);


                    // Check for overflow.
                    if (!step.isOne() && isKnownPredicate(Predicate.ICMP_SLT,
                            start, end) && !(step.isAllOnesValue()
                            && isKnownPredicate(Predicate.ICMP_SGT, start, end)))
                        return fullset;

                    ConstantRange startRange = getSignedRange(start);
                    ConstantRange endRange = getSignedRange(end);
                    APInt min = APInt.smin(startRange.getSignedMin(),
                            endRange.getSignedMin());
                    APInt max = APInt.smax(startRange.getSignedMax(),
                            endRange.getSignedMax());
                    if (min.isMinSignedValue() && max.isMaxSignedValue())
                        return fullset;
                    return new ConstantRange(min, max.increase());
                }
            }
        }

        if (s instanceof SCEVUnknown)
        {
            SCEVUnknown u = (SCEVUnknown)s;
            int bitwidth = getTypeSizeBits(u.getType());
            int ns = computeNumSignBits(u.getValue(), td);
            if (ns == 1)
                return fullset;
            return new ConstantRange(APInt.getSignedMinValue(bitwidth).ashr(ns - 1),
                    APInt.getSignedMaxValue(bitwidth).ashr(ns - 1).increase());
        }
        return fullset;
    }

    public SCEV getMaxIterationCount(Loop loop)
    {
        return null;
    }

    public void forgetLoopBackendTakenCount(Loop loop)
    {
        iterationCount.remove(loop);

        LinkedList<Instruction> worklist = new LinkedList<>();
        pushLoopPhis(loop, worklist);

        HashSet<Instruction> visited = new HashSet<>();
        while (!worklist.isEmpty())
        {
            Instruction inst = worklist.removeLast();
            if (scalars.containsKey(inst))
            {
                scalars.remove(inst);
                if (inst instanceof PhiNode)
                    constantEvolutionLoopExitValue.remove(inst);
            }
            pushUseIntoStack(inst, worklist);
        }
    }

    private void pushLoopPhis(Loop loop, LinkedList<Instruction> worklist)
    {
        BasicBlock header = loop.getHeaderBlock();
        for (Instruction inst : header)
        {
            if (!(inst instanceof PhiNode))
                break;
            worklist.add(inst);
        }
    }

    private void pushUseIntoStack(Instruction inst,
            LinkedList<Instruction> worklist)
    {
        inst.getUseList().forEach(
                u -> worklist.add((Instruction) u.getUser()));
    }
}

