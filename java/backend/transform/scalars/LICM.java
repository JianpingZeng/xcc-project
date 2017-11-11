package backend.transform.scalars;

import backend.analysis.*;
import backend.analysis.AliasAnalysis.ModRefBehavior;
import backend.analysis.AliasSetTracker.AliasSet;
import backend.analysis.LoopInfo;
import backend.pass.RegisterPass;
import backend.support.LLVMContext;
import backend.value.*;
import backend.support.CallSite;
import backend.pass.AnalysisUsage;
import backend.pass.LPPassManager;
import backend.pass.LoopPass;
import backend.value.Instruction.*;
import backend.value.Value.UndefValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static backend.analysis.AliasAnalysis.ModRefBehavior.DoesNotAccessMemory;
import static backend.analysis.AliasAnalysis.ModRefBehavior.OnlyReadsMemory;
import static backend.transform.scalars.PromoteMemToReg.promoteMemToReg;

/** 
 * </p>
 * This class performs loop invariant code motion, attempting to remove
 * as much code from the body of a loop as possible. It does this by either
 * hoisting code into the pre-header block, or by sinking code to the exit 
 * block if it is safe. Currently, this class does not use alias analysis
 * so that the all backend.transform operated upon memory access are excluded.
 * </p>
 * 
 * <p>This pass expected to run after Loop Inversion
 * and {@linkplain LoopInfo pass}.
 * performed.
 * </p>
 * 
 * <p>This file is a member of Machine Independence Optimization</p>.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class LICM implements LoopPass
{
    /**
     * enable licm constant variables motion, default to false.
     */
    public static final boolean EnableLICMConstantMotion = false;
    /**
     * Disable memory promotion in LICM pass.
     */
    public static final boolean DisablePromotion = true;

    public static int numMoveLoads;
    public static int numMoveCalls;
    public static int numSunk;
    public static int numHoisted;

    /**
     * Current AliasAnalysis information.
     */
    private AliasAnalysis aa;
    /**
     * Current loopInfo.
     */
    private LoopInfo li;
    /**
     * Current dominator tree info.
     */
    private DomTreeInfo dt;
    /**
     * Current Dominator frontier info.
     */
    private DominatorFrontier df;
    /**
     * Set to true when we change anything.
     */
    private boolean changed;
    /**
     * the pre-header BB of the current loop.
     */
    private BasicBlock preheaderBB;
    /**
     * The current loop being processed.
     */
    private Loop curLoop;
    /**
     * AliasSet information for the current loop.
     */
    private AliasSetTracker curAST;

    private HashMap<Loop, AliasSetTracker> loopToAliasMap;

    private LICM()
    {
        loopToAliasMap = new HashMap<>();
    }

    static
    {
        new RegisterPass("licm", "Loop Invariant Code Motion", LICM.class);
    }

    public static LICM createLICMPass()
    {
        return new LICM();
    }

    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        au.addRequired(LoopSimplify.class);
        au.addRequired(LoopInfo.class);
        au.addRequired(DomTreeInfo.class);
        au.addRequired(DominatorFrontier.class);
        au.addRequired(AliasAnalysis.class);
    }

    /**
     * This transformation requires natural loop information and requires that
     * loop preheaders be inserted into the CFG.
     * @return
     */
    @Override
    public String getPassName()
    {
        return "Loop Invariant Code Motion";
    }

    /**
     * Moves the code outside of this loop.
     * @param loop
     * @return
     */
    @Override
    public boolean runOnLoop(Loop loop, LPPassManager ppm)
    {
        changed = false;
        li = getAnalysisToUpDate(LoopInfo.class);
        aa = getAnalysisToUpDate(AliasAnalysis.class);
        df = getAnalysisToUpDate(DominatorFrontier.class);
        dt = getAnalysisToUpDate(DomTreeInfo.class);

        curAST = new AliasSetTracker(aa);
        preheaderBB = loop.getLoopPreheader();

        // Collects alias information from sub loops.
        for (Loop subLoop: loop.getSubLoops())
        {
            assert loopToAliasMap.containsKey(subLoop);
            AliasSetTracker innerAST = loopToAliasMap.get(subLoop);

            // what if innerLoop was modified by other passes?
            curAST.add(innerAST);
        }

        curLoop = loop;

        // Loop over body of this loop, looking for calls, store, and load inst.
        // Since sub loops have already being incorporated into current AST, we
        // will skip blocks in subloop.
        for (BasicBlock bb : loop.getBlocks())
        {
            // Ignores the basic block in sub loop.
            if (li.getLoopFor(bb).equals(loop))
                curAST.add(bb);
        }

        // We want to visit all instructions in the order of DFS over dominator
        // tree of the loop, so that we are guaranted to see the definition before
        // variable use. This allow us to avoid expensively iteration.
        //
        if (loop.hasDedicatedExits())
            sinkRegion(dt.getNode(loop.getHeaderBlock()));
        if (preheaderBB != null)
            hoistRegion(dt.getNode(loop.getHeaderBlock()));

        curLoop = null;
        loopToAliasMap.put(loop, curAST);
        return changed;
    }

    private void sinkRegion(DomTreeNodeBase<BasicBlock> entryNode)
    {
        assert entryNode != null:"Null dominator tree node!";
        BasicBlock bb = entryNode.getBlock();

        // Terminates early, if this block is not contained in current loop.
        if (!curLoop.contains(bb)) return;

        // First, traverse the child node if there are children in reverse dfs
        // order.
        entryNode.getChildren().forEach(this::sinkRegion);

        // Only need to process the contentd of this block if it not in sub loop
        // (which is already processed before).
        if (inSubLoop(bb)) return;

        for (int i = bb.size() - 1; i >= 0;)
        {
            Instruction inst = bb.getInstAt(i--);

            // chose the instruction that is used outside the loop. In this case,
            // it does not matter if the operands of the instruction are not loop
            // invariant.
            if (isNotUsedInLoop(inst) && canHoistOrSinkInst(inst))
            {
                i++;
                sink(inst);
            }
        }
    }

    /**
     * Returns true if the only use of this inst is live in outside of the loop.
     * If it is true, we would sink this instruction to the exit blocks of the
     * loop.
     * @param inst
     * @return
     */
    private boolean isNotUsedInLoop(Instruction inst)
    {
        for (int i = 0, e = inst.getNumUses(); i < e; i++)
        {
            User u = inst.useAt(i).getUser();
            if (u instanceof Instruction)
            {
                Instruction user = (Instruction)u;
                PhiNode pn;
                if ((user instanceof PhiNode))
                {
                    pn = (PhiNode)user;
                    for (int j = 0, sz = pn.getNumberIncomingValues(); j < sz; j++)
                    {
                        if (pn.getIncomingValue(j).equals(inst))
                            if (curLoop.contains(pn.getIncomingBlock(j)))
                                return false;
                    }
                }
                else if (curLoop.contains(user.getParent()))
                    return false;
            }
        }
        return true;
    }

    /**
     * Return true if the Hoister and Sinker can handle this instruction.
     * @param inst
     * @return
     */
    private boolean canHoistOrSinkInst(Instruction inst)
    {
        if (inst instanceof LoadInst)
        {
            LoadInst loadInst = (LoadInst)inst;
            if (loadInst.isVolatile())
                return false;   // Don't hoist volatile load instruction.

            if (EnableLICMConstantMotion &&
                    aa.pointsToConstantMemory(loadInst.getPointerOperand()))
                return true;

            // Don't hoist the load instruction which have may-aliased stores in
            // loop.
            int size = 0;
            if (loadInst.getType().isSized())
                size = aa.getTypeStoreSize(loadInst.getType());
            return !pointerInvalidatedByLoop(loadInst.getPointerOperand(), size);
        }
        else if (inst instanceof CallInst)
        {
            CallInst ci = (CallInst)inst;
            ModRefBehavior behavior = aa.getModRefBehavior(new CallSite(ci));
            if (behavior == DoesNotAccessMemory)
                return true;
            if (behavior == OnlyReadsMemory)
            {
                // If this call only reads from memory and there are no writes to memory
                // in the loop, we can hoist or sink the call as appropriate.
                boolean foundMod = false;
                for (AliasSet as :curAST.getAliasSets())
                {
                    if (as.isForwardingAliasSet() && as.isMod())
                    {
                        foundMod = true;
                        break;
                    }
                }
                if (!foundMod) return true;
            }
            return false;

        }
        return (inst instanceof BinaryInstruction) || (inst instanceof CastInst)
                || (inst instanceof GetElementPtrInst)
                || (inst instanceof CmpInst);
    }

    /**
     * When an instruction is found to only be used outside of the loop,
     * this function moves it to the exit blocks and patches up SSA form as
     * needed. This method is guaranteed to remove the original instruction
     * from its position, and may either delete it or move it to outside of
     * the loop.
     * @param inst
     */
    private void sink(Instruction inst)
    {
        ArrayList<BasicBlock> exitBlocks = curLoop.getExitingBlocks();
        if (inst instanceof LoadInst)
            ++numMoveLoads;
        if (inst instanceof CallInst)
            ++numMoveCalls;
        ++numSunk;

        changed = true;

        if (exitBlocks.size() == 1)
        {
            if (!isExitBlockDominatedByBlockInLoop(exitBlocks.get(0), inst.getParent()))
            {
                // Instruction is not used, just delete it from basic block.
                curAST.deleteValue(inst);
                if (!inst.isUseEmpty())
                    inst.replaceAllUsesWith(UndefValue.get(inst.getType()));
                inst.eraseFromParent();
            }
            else
            {
                inst.eraseFromParent();

                int idx = exitBlocks.get(0).getFirstNonPhi();
                exitBlocks.get(0).insertBefore(inst, idx);
            }
        }
        else if (exitBlocks.isEmpty())
        {
            // the instruction actually is dead if there are no exit blocks.
            curAST.deleteValue(inst);
            if (!inst.isUseEmpty())
                inst.replaceAllUsesWith(UndefValue.get(inst.getType()));
            inst.eraseFromParent();
        }
        else
        {
            // Otherwise, if we have multiple exits, use the PromoteMemToReg function to
            // do all of the hard work of inserting PHI nodes as necessary.  We convert
            // the value into a stack object to get it to do this.

            // Firstly, we create a stack object to hold the value.
            AllocaInst ai = null;
            if (!inst.getType().equals(LLVMContext.VoidTy))
            {
                ai = new AllocaInst(inst.getType(), null, inst.getName(),
                        inst.getParent().getParent().getEntryBlock().getFirstInst());
                curAST.add(ai);
            }

            // Secondly, insert load instruction for each use of the instruction
            // which is outside of loop.
            while (!inst.isUseEmpty())
            {
                Use u = inst.getUseList().removeLast();
                Instruction user = (Instruction)u.getUser();

                if (user instanceof PhiNode)
                {
                    PhiNode pn = (PhiNode)user;
                    HashMap<BasicBlock, Value> insertBlocks = new HashMap<>();
                    for (int i = 0, e = pn.getNumberIncomingValues(); i < e; i++)
                    {
                        if (pn.getIncomingValue(i).equals(inst))
                        {
                            BasicBlock pred = pn.getIncomingBlock(i);
                            Value predVal = insertBlocks.get(pred);
                            if (!insertBlocks.containsKey(pred))
                            {
                                // insert a new load instruction right before the
                                // terminator in the predecessor block.
                                predVal = new LoadInst(ai, "", pred.getTerminator());
                                curAST.add((LoadInst)predVal);
                            }
                            pn.setIncomingValue(i, predVal);
                        }
                    }
                }
                else
                {
                    LoadInst l = new LoadInst(ai, "", user);
                    user.replaceUsesOfWith(inst, l);
                    curAST.add(l);
                }
            }

            // Thirdly, inserts a copy of the instruction in each exit block of
            // the loop that is dominated by the instruction, storing the result
            // into the memory location. Be careful not to insert the instruction
            // into any particular basic block more than once.
            HashSet<BasicBlock> insertedBlocks = new HashSet<>();
            BasicBlock instOriginBB = inst.getParent();

            for (BasicBlock exitBB : exitBlocks)
            {
                if (isExitBlockDominatedByBlockInLoop(exitBB, instOriginBB))
                {
                    // If we haven't already processed this exit block, do so now.
                    if (insertedBlocks.add(exitBB))
                    {
                        int insertPos = exitBB.getFirstNonPhi();
                        Instruction insertPtr = exitBB.getInstAt(insertPos);

                        Instruction newInst;
                        if (insertedBlocks.size() == 1)
                        {
                            inst.eraseFromParent();
                            exitBB.insertBefore(inst, insertPos);
                            newInst = inst;
                        }
                        else
                        {
                            newInst = inst.clone();
                            curAST.copyValue(inst, newInst);
                            if (!inst.getName().isEmpty())
                                newInst.setName(inst.getName()+".le");
                            exitBB.insertBefore(newInst, insertPos);
                        }

                        if (ai != null)
                            new Instruction.StoreInst(newInst, ai, "", insertPtr);
                    }
                }
            }

            // If the instruction doesn't dominate any exit blocks, it must be dead.
            if (insertedBlocks.isEmpty())
            {
                curAST.deleteValue(inst);
                inst.eraseFromParent();
            }

            // Finally, promote the fine value to SSA form.
            if (ai != null)
            {
                ArrayList<AllocaInst> allocas = new ArrayList<>();
                allocas.add(ai);
                promoteMemToReg(allocas, dt, df, curAST);
            }
        }
    }

    /**
     * This method checks to see if the specified exit block of the loop
     * is dominated by the specified block that is in the body of the loop.
     * We use these constraints to dramatically limit the amount of the
     * dominator tree that needs to be searched.
     * @param exitBB
     * @param bbInLoop
     * @return
     */
    private boolean isExitBlockDominatedByBlockInLoop(BasicBlock exitBB,
            BasicBlock bbInLoop)
    {
        return dt.dominates(bbInLoop, exitBB);
    }

    /**
     * Returns true if the specified basic block is contained in sub loop.
     * @param bb
     * @return
     */
    private boolean inSubLoop(BasicBlock bb)
    {
        assert curLoop.contains(bb);
        for (Loop innerLoop : curLoop.getSubLoops())
            if (innerLoop.contains(bb))
                return true;
        return false;
    }

    private boolean pointerInvalidatedByLoop(Value ptr, int size)
    {
        return curAST.getAliasSetForPointer(ptr, size, null).isMod();
    }

    private void hoistRegion(DomTreeNodeBase<BasicBlock> entryNode)
    {
        assert entryNode != null;
        BasicBlock bb = entryNode.getBlock();

        if (!curLoop.contains(bb)) return;

        // Only need to process the contents of this block if it is not part of a
        // subloop (which would already have been processed).
        if (!inSubLoop(bb))
        {
            for (int i = 0; i < bb.size();)
            {
                Instruction inst = bb.getInstAt(i++);
                if (isLoopInvariantInst(inst) && canHoistOrSinkInst(inst))
                {
                    hoist(inst);
                }
            }
        }

        entryNode.getChildren().forEach(this::hoistRegion);
    }

    private boolean isLoopInvariantInst(Instruction inst)
    {
        for (int i = 0, e = inst.getNumOfOperands(); i < e; i++)
            if (!curLoop.isLoopInVariant(inst.operand(i)))
                return false;
        return true;
    }

    private void hoist(Instruction inst)
    {
        inst.eraseFromParent();

        int idx = preheaderBB.getInstList().indexOf(preheaderBB.getTerminator());
        preheaderBB.insertBefore(inst, idx);

        if (inst instanceof LoadInst) ++numMoveLoads;
        if (inst instanceof CallInst) ++numMoveCalls;
        numHoisted++;
        changed = true;
    }
}
