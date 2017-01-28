package backend.analysis;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2016, Xlous
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
import jlang.sema.APInt;

import java.util.ArrayList;
import java.util.HashMap;

import static backend.transform.ConstantFolder.canConstantFoldCallTo;
import static backend.transform.ConstantFolder.constantFoldCall;

/**
 * This class is the main scalar evolution driver. Since client code (intentionally)
 * can't do much the SCEV objects directly, they must query this class for services.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class ScalarEvolution extends FunctionPass
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
            // Avoid performing the look-up in the common case where the specified
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
        ArrayList<BasicBlock> exitBlocks = loop.getExitBlocks();

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
}
