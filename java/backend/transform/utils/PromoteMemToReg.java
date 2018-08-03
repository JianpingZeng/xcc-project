/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package backend.transform.utils;

import backend.value.IntrinsicInst.DbgInfoIntrinsic;
import tools.Util;
import backend.analysis.aa.AliasSetTracker;
import backend.analysis.DomTree;
import backend.analysis.DominanceFrontier;
import backend.support.PrintModulePass;
import backend.type.PointerType;
import backend.utils.PredIterator;
import backend.utils.SuccIterator;
import backend.value.*;
import backend.value.Instruction.*;
import backend.value.Value.UndefValue;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.OutRef;
import tools.Pair;

import java.util.*;

import static backend.support.DepthFirstOrder.dfTraversal;

/**
 * This file promotes memory references to be register references.  It promotes
 * alloca instructions which only have loads and stores as usesList.  An alloca is
 * transformed by using iterated dominator frontiers to place PHI nodes, then
 * traversing the function in depth-first order to rewrite loads and stores as
 * appropriate.
 * <p>
 * The algorithm used here is based on:
 * <p>
 * Sreedhar and Gao. A linear time algorithm for placing phi-nodes.
 * In Proceedings of the 22nd ACM SIGPLAN-SIGACT Symposium on Principles of
 * Programming Languages
 * POPL '95. ACM, New York, NY, 62-73.
 * <p>
 * It has been modified to not explicitly use the DJ graph data structure and to
 * directly compute pruned SSA using per-variable liveness information.
 * <p>
 * Created by Jianping Zeng  on 2016/3/3.
 */
public final class PromoteMemToReg
{
	/**
	 * The list of {@code AllocaInst} to be promoted.
	 */
	private ArrayList<AllocaInst> allocas;

	/**
	 * The dominator tree information.
	 */
	private DomTree dt;
    /**
     * The computed dominator frontier information for a specified function.
     */
	private DominanceFrontier df;

	private AliasSetTracker ast;

	/**
	 * A reverse mapping of allocas.
	 */
	private TObjectIntHashMap<AllocaInst> allocaLookup;

	/**
	 * The phi node we are adding.
	 * <p>
	 * That map is used to simplify some PhiNode nodes as we
	 * iterate over it, so it should have deterministic iterators.
	 */
	private HashMap<Pair<BasicBlock, Integer>, PhiNode> newPhiNodes;
    /**
     * Mapping from the PhiNode to the index that indexed to the corresponding
     * Alloca instruction in list.
     */
	private TObjectIntHashMap<PhiNode> phiToAllocaMap;

    /**
     * If we are updating an AliasSetTracker, then for
     * each alloca that is of pointer type, we keep track of what to copyValue
     * to the inserted PHI nodes here.
     */
	private ArrayList<Value> pointerAllocaValues;

	/**
	 * The set of basic block the renamer has already visited, avoding duplicated
     * visiting of a basic block.
	 */
	private HashSet<BasicBlock> visitedBlocks;

    /**
     * Numbering the basic block contained in a function.
     */
	private TObjectIntHashMap<BasicBlock> bbNumbers;

	/**
	 * The numbers of dead alloca instruction to be removed from parent block.
	 */
	public static int numberDeadAlloca = 0;

	/**
	 * The numbers of promotable alloca instruction to be promoted.
	 */
	public static int numberPromotedAlloca = 0;

	/**
	 * The numbers of alloca instruction that just have only one stores(definition).
	 */
	public static int numberSingleStore = 0;

	/**
	 * The number of local alloca to be promoted successfully.
	 */
	public static int numberLocalPromoted = 0;

	public static int numberPhiInsert = 0;

    public static void promoteMemToReg(ArrayList<AllocaInst> allocas,
            DomTree dt,
            DominanceFrontier df)
    {
        promoteMemToReg(allocas, dt, df, null);
    }

	public static void promoteMemToReg(ArrayList<AllocaInst> allocas,
            DomTree dt,
            DominanceFrontier df,
			AliasSetTracker ast)
    {
        if (allocas.isEmpty()) return;
        new PromoteMemToReg(allocas, dt, df, ast).run();
    }

	public PromoteMemToReg(ArrayList<AllocaInst> allocas,
            DomTree dt,
            DominanceFrontier df,
            AliasSetTracker ast)
	{
		this.allocas = allocas;
		this.dt = dt;
		this.df = df;
		this.ast = ast;
		allocaLookup = new TObjectIntHashMap<>();
		newPhiNodes = new HashMap<>();
		visitedBlocks = new HashSet<>();
		bbNumbers = new TObjectIntHashMap<>();
		phiToAllocaMap = new TObjectIntHashMap<>();
		pointerAllocaValues = new ArrayList<>();
	}

    /**
     * Determine whether this alloca instruction is promoted into
     * register or not?
     *
     * @return Return true if there are only loads and stores to the alloca instruction.
     */
    public static boolean isAllocaPromotable(AllocaInst ai)
    {
        LoadInst li;
        StoreInst si;
        BitCastInst bc;
        for (Use u : ai.getUseList())
        {
            Instruction inst = (Instruction) u.getUser();
            if (inst instanceof LoadInst)
            {
                li = (LoadInst)inst;
                if (li.isVolatile())
                    return false;
            }
            else if (inst instanceof StoreInst)
            {
                si = (StoreInst)inst;
                if (si.operand(0).equals(ai))
                    return false;
                if (si.isVolatile())
                    return false;
            }
            else if (inst instanceof BitCastInst)
            {
                bc = (BitCastInst)inst;
                if (!bc.hasOneUses() || !(bc.useAt(0).getUser() instanceof DbgInfoIntrinsic))
                    return false;

                if (ai.hasOneUses())
                    return false;
            }
            else
                return false;
        }
        return true;
    }

	/**
	 * Running backend.transform algorithm to promote alloca onto register.
	 * <p>
	 * For simply, first we handle many trivial case as follow two situation.
	 * <br>
	 * <b>i.Only one stores(definition) to alloca:</b>
	 * Replaces all usesList to load instruction that load a value from a single
	 * alloca with the value loaded from alloca.
	 * Then simply removes this load and store.
	 * <br>
	 * <b>ii.All usesList to alloca are within a single basic block:</b>
	 * Removes useless loads and stores.
	 * </p>
	 * <p>
	 * <p>
	 * After, take advantage of DJ graph to place phi node with dominator
	 * frontier at same time, avoding inserts dead phi node.
	 * <br>
	 * The finally, performing rename algorithm as follow steps.
	 * <br>
	 * 1.Aads {@code UndefValue} into incoming VALUES of phi node.
	 * <br>
	 * 2.Looks up for all usesList(loads) to alloca, replace the all usesList to
	 * loads with the current active value of current alloca, then remove
	 * loads from basic block.
	 * <br>
	 * 3.Looks up for all definition(stores) to alloca, which modify the
	 * current active value of alloca, so that we updates the current active
	 * value of the targeted alloca of stores with the value used in store.
	 * </p>
	 */
	public void run()
	{
		Function f = df.getRoot().getParent();
		Util.assertion((f != null),  "The function of this Dominator Tree can not be null");

		AllocaInfo info = new AllocaInfo();
		LargeBlockInfo lbi = new LargeBlockInfo();

		// promotes every alloca instruction one by one.
		int i = 0;
		for (int allocaNum = 0; allocaNum < allocas.size(); allocaNum++)
		{
            AllocaInst ai = allocas.get(allocaNum);
			Util.assertion(isAllocaPromotable(ai),  "Can't promote non-promotable alloca");
			Util.assertion(ai.getParent().getParent().equals(f),
                    "All allocas should in the same method, which is same as DF!");

			// if it's use an intrinsic instruction, just remove it from
			// attached basic block.
			// However, it is not finished currently
			if (ai.usesList.isEmpty())
			{
				// if there no usesList of the alloca, just delete it
				ai.eraseFromParent();

				// remove the alloca out from alloca instructions list
				// because it is processed finished.
				allocaNum = removeFromAllocasList(allocaNum);
				++numberDeadAlloca;
				continue;
			}

			// Calculate the set of read and write-location(basic block) for
			// each alloca. This is analogous to finding the 'use' and
			// 'definition' of each variable.
			info.analyzeAlloca(ai);

			// if there is only a single store to this value, this is said
			// that there is only a single definition of this variable.
			// replace any loads of it that are directly dominated by the
			// definition with the value stored.
			if (info.definingBlocks.size() == 1)
			{
                // Finally, after the scan, check to see if the store is all
                // that is left.
                rewriteSingleStoreAlloca(ai, info, lbi);
				if (info.usingBlocks.isEmpty())
				{
				    info.onlyStore.eraseFromParent();
				    lbi.deleteValue(info.onlyStore);

				    if (ast != null) ast.deleteValue(ai);
				    ai.eraseFromParent();
				    lbi.deleteValue(ai);

					// the alloca instruction has been processed, remove it.
					allocaNum = removeFromAllocasList(allocaNum);
					++numberSingleStore;

					// Print out after each alloca promoted
                    if (Util.DEBUG)
                    {
                        System.err.println("# *** After " + (++i)
                                + "'th alloca promoted ***:");
                        new PrintModulePass(System.err).runOnModule(f.getParent());
                    }
					continue;
				}
			}

			// If the alloca is only read and written in one block.
			// just perform a linear scan sweep over the block to
			// eliminate it.
			if (info.onlyUsedOneBlock)
			{
				promoteSingleBlockAlloca(ai, info, lbi);

				if (info.usingBlocks.isEmpty())
                {
                    // Now that remove the dead stores and alloca.
                    while (!ai.isUseEmpty())
                    {
                        StoreInst si = (StoreInst)(ai.usesList.removeLast().getUser());
                        si.eraseFromParent();
                        lbi.deleteValue(si);
                    }

                    if (ast != null) ast.deleteValue(ai);
                    ai.eraseFromParent();
                    lbi.deleteValue(ai);

                    // the alloca instruction has been processed, remove it.
                    allocaNum = removeFromAllocasList(allocaNum);
                    numberLocalPromoted++;
                    continue;
                }
			}

			// If we haven't computed a numbering for the BB's in the function,
            // do so now.
			if (bbNumbers.isEmpty())
			{
				int id = 0;
				// with reverse post-order on function
                ArrayList<BasicBlock> reversePost = dfTraversal(f.getEntryBlock());
                for (BasicBlock bb : reversePost)
                {
                    bbNumbers.put(bb, id++);
                }
			}

            // If we have an AST to keep updated, remember some pointer value that is
            // stored into the alloca.
			if (ast != null)
			    pointerAllocaValues.set(allocaNum, info.allocaPointerVar);

			// Keeps the reverse diagMapping of the 'Allocas' array for the rename pass.
			allocaLookup.put(allocas.get(allocaNum), allocaNum);

			// Using standard SSA construction algorithm to promoting the alloca.
			// Determine which blocks need PHI nodes and see if we can optimize out
			// some work by avoiding insertion of dead phi nodes.
			determineInsertionPoint(ai, allocaNum, info);
		}// end of traveling alloca instruction list.

		// all of allocas must has been handled
		// just return.
		if (allocas.isEmpty())
			return;

		lbi.clear();

		ArrayList<Value> values = new ArrayList<>(allocas.size());
		for (int idx = 0; idx < allocas.size(); idx++)
			values.add(UndefValue.get(allocas.get(idx).getType()));

		// walk all basic block in the function performing
		// SSA construction algorithm and inserting the phi nodes
		// we marked as necessary.
		LinkedList<RenamePassData> renamePassWorkList = new LinkedList<>();
		renamePassWorkList
				.addLast(new RenamePassData(f.getEntryBlock(), null, values));

		do
		{
		    // Performs DFS walks on CFG
			RenamePassData rpd = new RenamePassData();
			rpd.swap(renamePassWorkList.removeLast());

			renamePass(rpd.BB, rpd.pred, rpd.values, renamePassWorkList);

		} while (!renamePassWorkList.isEmpty());

		visitedBlocks.clear();

		// Remove the allocas themselves from the function.
		for (AllocaInst ai : allocas)
		{
			// If there are any usesList of the alloca instructions left, they must be in
			// unreachable basic blocks that were not processed by walking the dominator
			// tree. Just delete the users now.
			if (!ai.usesList.isEmpty())
				ai.replaceAllUsesWith(UndefValue.get(ai.getType()));
			ai.eraseFromParent();
		}

		// Loop over all of the PHI nodes and see if there are any that we can
		// get rid of because they merge all of the same incoming VALUES.  This can
		// happen due to undef VALUES coming into the PHI nodes.  This process is
		// iterative, because eliminating one PHI node can cause others to be removed.
		boolean eliminatedAPHI = true;

		while (eliminatedAPHI)
		{
			eliminatedAPHI = false;

            Iterator<Map.Entry<Pair<BasicBlock, Integer>, PhiNode>> itr =
                    newPhiNodes.entrySet().iterator();
			while (itr.hasNext())
			{
                Map.Entry<Pair<BasicBlock, Integer>, PhiNode> entity = itr.next();
				PhiNode phiNode = entity.getValue();

				Value val;
				// if the phiNode merges one value and/or undefs, get the value
                if ((val = phiNode.hasConstantValue()) != null)
				{
				    if (!(val instanceof Instruction) ||
                            dt.strictDominates(((Instruction)val).getParent(), phiNode.getParent()))
                    {
                        if (ast != null && (phiNode.getType() instanceof PointerType))
                            ast.remove(phiNode);

                        phiNode.replaceAllUsesWith(val);
                        phiNode.eraseFromParent();
                        itr.remove();
                        eliminatedAPHI = true;
                    }
				}
			}
		}

		// At this point, the renamer has added entries to PHI nodes for all reachable
		// code.  Unfortunately, there may be unreachable blocks which the renamer
		// hasn't traversed.  If this is the case, the PHI nodes may not
		// have incoming VALUES for all predecessors.  Loop over all PHI nodes we have
		// created, inserting undef VALUES if they are missing any incoming VALUES.
		//
		for (Map.Entry<Pair<BasicBlock, Integer>, PhiNode> entity : newPhiNodes.entrySet())
		{
			PhiNode phiNode = entity.getValue();
			BasicBlock bb = phiNode.getParent();

			// We want to do this once per basic block.  As such, only process a block
			// when we find the PHI that is the first entry in the block.
			if (bb.getFirstInst() != phiNode)
				continue;

			// Only do work here if the phiNode node are missing incoming VALUES.
			if (phiNode.getNumberIncomingValues() == bb.getNumPredecessors())
				continue;

			LinkedList<BasicBlock> preds = new LinkedList<>();
			PredIterator<BasicBlock> predItr = bb.predIterator();
			while (predItr.hasNext())
			    preds.add(predItr.next());


			// loop through all parent which have entity in specified phiNode
			// and remove them from preds list.
			for (int idx = 0; idx < phiNode.getNumberIncomingValues(); idx++)
			{
				BasicBlock incomingBlock = phiNode.getIncomingBlock(idx);
				if (preds.contains(incomingBlock))
					preds.remove(incomingBlock);
			}

			int numBadPreds = phiNode.getNumberIncomingValues();
			Iterator<Instruction> it = bb.iterator();
			Value inst;

			while (it.hasNext() && ((inst = it.next()) instanceof PhiNode
					&& (phiNode = (PhiNode) inst).getNumberIncomingValues()
					== numBadPreds))
			{
				Value undef = UndefValue.get(phiNode.getType());
				for (BasicBlock pred : preds)
					phiNode.addIncoming(undef, pred);
			}
		}
		allocas.clear();
		newPhiNodes.clear();
	}

	/**
	 * <p>
	 * Recursively traverse the CFG of the function, renaming loads and
	 * stores to the alloca which we are promoting.
	 * <p>
	 * On the other hand, this rename algorithm is performed for promoting
	 * alloca onto register and removing related stores and loads to alloca
	 * promoted.
	 * </p>
	 * <p>
	 * Since the reference to alloca (variable) just isDeclScope stores and load.
	 * Stores is definition of alloca, and load is usesList to alloca.
	 * </p>
	 *
	 * @param curBB              The Basic CompoundStmt where all variable (alloca) will be renamed.
	 * @param pred            The predecessor of curBB.
	 * @param incomgingValues
	 * @param worklist        The list of basic blocks to be renamed.
	 */
	private void renamePass(BasicBlock curBB, BasicBlock pred,
            ArrayList<Value> incomgingValues, LinkedList<RenamePassData> worklist)
	{
		Value inst;
		HashSet<BasicBlock> visitedSuccs = new HashSet<>();

		while (true)
		{
			// determine whether any phi node already be in the block.
			if ((inst = curBB.getFirstInst()) instanceof PhiNode)
			{
				PhiNode phiNode = (PhiNode) inst;
				// to distinguish between phiNode node being inserted by this invocation
				// of mem2reg from those phiNode nodes that already existed in the Module
				// before mem2reg was run.
				if (phiToAllocaMap.containsKey(phiNode))
				{
					int newPhiNumOperands = phiNode.getNumberIncomingValues();

					int numEdges = 0;
                    for (SuccIterator itr = pred.succIterator(); itr.hasNext(); )
                        if (itr.next().equals(curBB))
                            ++numEdges;

					Util.assertion(numEdges > 0,  "Must be at least one edge form pred to curBB!");


					int idx = 1;
					do
					{
						int allocaNo = phiToAllocaMap.get(phiNode);

						// sets the undef value for phiNode node, it is reason that
						// handling loop.
						for (int j = 0; j < numEdges; ++j)
							phiNode.addIncoming(incomgingValues.get(allocaNo), pred);

						// the currently active variable for this block is now
						// the phiNode.
						incomgingValues.set(allocaNo, phiNode);

						// no more instruction
						if (idx >= curBB.size())
							break;
						inst = curBB.getInstAt(idx++);
						// the handling phiNode node has finished!
						if (!(inst instanceof PhiNode))
							break;
						phiNode = (PhiNode) inst;
					} while (phiNode.getNumberIncomingValues()
							== newPhiNumOperands);
				}
			}

			// don't revisit blocks
			if (!visitedBlocks.add(curBB))
				return;

			// handles subsequnce instruction at control flow graph.
			for (int i = 0, e = curBB.size(); i < e; i++)
			{
				inst = curBB.getInstAt(i);

				// Only load and store to alloca instruction will be handled,
				// because at our Module, the usesList of alloca just isDeclScope laods
				// and stores.
				if (inst instanceof LoadInst)
				{
					LoadInst li = (LoadInst) inst;
					if (!(li.getPointerOperand() instanceof AllocaInst))
						continue;

                    AllocaInst src = (AllocaInst) li.operand(0);

					if (!allocaLookup.containsKey(src))
						continue;
                    int index = allocaLookup.get(src);

					// gets the active value of current alloca with index.
					Value value = incomgingValues.get(index);
					// anything using the load now usesList the current value.
					li.replaceAllUsesWith(value);

					li.eraseFromParent();
					--i;
					--e;
					if (ast != null && li.getType() instanceof PointerType)
					    ast.deleteValue(li);
				}
				else if (inst instanceof StoreInst)
				{
					StoreInst si = (StoreInst) inst;
					if (!(si.getPointerOperand() instanceof AllocaInst))
						continue;

                    AllocaInst dest = (AllocaInst) si.operand(1);
                    if (!allocaLookup.containsKey(dest))
                        continue;

					int index = allocaLookup.get(dest);

					// what value were we writing?
					incomgingValues.set(index, si.operand(0));
					si.eraseFromParent();
					--i;
					--e;
				}
			}

			if (curBB.getNumSuccessors() == 0)
				return;

			visitedSuccs.clear();
			pred = curBB;
			SuccIterator itr = curBB.succIterator();
			curBB = itr.next();
			// recurse to successor
			while (itr.hasNext())
			{
				BasicBlock succ = itr.next();
				if (visitedSuccs.add(succ))
					worklist.addLast(new RenamePassData(succ, pred, incomgingValues));
			}
		}
	}

	/**
	 * Many allocas are only used within a single basic block.  If this is the
	 * case, avoid traversing the CFG and inserting a lot of potentially useless
	 * PHI nodes by just performing a single linear pass over the basic block
	 * using the AllocaInst.
	 *
	 * @param ai
	 * @param info
	 * @param lbi
	 */
	private void promoteSingleBlockAlloca(AllocaInst ai,
            AllocaInfo info,
			LargeBlockInfo lbi)
	{
		info.usingBlocks.clear();
		// sort the stores by their index, making it efficient to do lookup.
		TreeSet<Pair<Integer, Instruction>> storesByIndex =
                new TreeSet<>((o1, o2) ->
                {
                    if (o1.first < o2.first)
                        return -1;
                    else if (o1.first.equals(o2.first))
                        return 0;
                    else
                        return 1;
                });

		for (Use u : ai.usesList)
		{
			Instruction inst = (Instruction)u.getUser();
			if (inst instanceof StoreInst)
            {
                storesByIndex.add(new Pair<>(lbi.getIndexOfInstruction(inst), inst));
            }
		}

		// If there is no stores instruction to this alloca inst, just replace
        // all load with undef.
		if (storesByIndex.isEmpty())
        {
            for (Use u : ai.getUseList())
            {
                Instruction inst = (Instruction)u.getUser();
                if (inst instanceof LoadInst)
                {
                    LoadInst li = (LoadInst)inst;
                    inst.replaceAllUsesWith(UndefValue.get(li.getType()));
                    if (ast != null && (li.getType() instanceof PointerType))
                        ast.deleteValue(li);
                    li.eraseFromParent();
                    lbi.deleteValue(li);
                }
            }
            return;
        }

		// Walk all of the loads from this alloca, replacing them with the
		// nearest store above them, if any.
		for (Use use : ai.usesList)
		{
			User ui = use.getUser();
			if (!(ui instanceof LoadInst))
				continue;
			LoadInst li = (LoadInst) ui;
			int loadIndex = lbi.getIndexOfInstruction(li);

			Pair<Integer, Instruction> target = storesByIndex.floor(Pair.get(loadIndex, null));

            // If there is no store before this load, then we can't promote this load.
			if (target == null)
            {
                info.usingBlocks.add(li.getParent());
                continue;
            }

            // otherwise, there was store before load, the load just toke its value
            li.replaceAllUsesWith(target.second.operand(0));
            if (ast != null && (li.getType() instanceof PointerType))
                ast.deleteValue(li);

			// now, this load instruction is not useful
			li.eraseFromParent();
			lbi.deleteValue(li);
		}
	}

	/**
	 * Using standard SSA construction algorithm to promoting the alloca.
	 * Determine which blocks need PHI nodes and see if we can optimize out
	 * some work by avoiding insertion of dead phi nodes.
	 *
	 * @param ai        The alloca to be promoted.
	 * @param allocaNum The index of alloca into allocas list.
	 * @param info      The information relative to alloca.
	 */
	private void determineInsertionPoint(AllocaInst ai,
            int allocaNum,
			AllocaInfo info)
	{
		HashSet<BasicBlock> defBlocks = new HashSet<>();
		defBlocks.addAll(info.definingBlocks);

		HashSet<BasicBlock> liveInBlocks = new HashSet<>();
		computeLivenessBlocks(ai, info, defBlocks, liveInBlocks);

        int currentVersion = 0;
        HashSet<PhiNode> insertedPHINodes = new HashSet<>();
        while (!info.definingBlocks.isEmpty())
        {
            BasicBlock bb = info.definingBlocks.pollFirst();

            HashSet<BasicBlock> dfs = df.find(bb);
            if (dfs == null)
                continue;

            // Avoiding insert dead phi node if the alloca isn't live in
            // specified bb.
            dfs.removeIf(b->!liveInBlocks.contains(b));

            OutRef<Integer> res = new OutRef<>(currentVersion);
            for (BasicBlock b : dfs)
            {
                res.set(currentVersion);
                if (queuePhiNode(b, allocaNum, res, insertedPHINodes))
                {
	                info.definingBlocks.add(b);
                }
            }
        }
	}

	/**
	 * Queue a phi-node to be added to a basic block in dominator frontier
	 * for a specific alloca.
	 *
	 * @param bb       The dominator frontier block.
	 * @param allocaNo The number of alloca.
	 * @param version  The current version.
	 */
	private boolean queuePhiNode(BasicBlock bb,
            int allocaNo,
            OutRef<Integer> version,
            HashSet<PhiNode> insertedPHINodes)
	{
        // Look up the basic-block in question.
	    Pair<BasicBlock, Integer> pair = Pair.get(bb, allocaNo);

        // If the BB already has a phi node added for the i'th alloca then we're done!
	    if (newPhiNodes.containsKey(pair))
	        return false;

		PhiNode phiNode = newPhiNodes.get(pair);

		// if the specific parent already has a phiNode node added for the i-th alloca
		// and the we have done.
		if (phiNode != null)
			return false;

		AllocaInst ai = allocas.get(allocaNo);
		// create a phiNode node and add the phiNode-node into the basic block
		phiNode = new PhiNode(allocas.get(allocaNo).getAllocatedType(),
				bb.getNumPredecessors(),
				allocas.get(allocaNo).getName() + "." + version.get(),
                bb.getFirstInst());
		newPhiNodes.put(pair, phiNode);

		version.set(version.get()+1);
		//bb.insertBefore(phiNode, bb.getFirstNonPhi());
		++numberPhiInsert;

		phiToAllocaMap.put(phiNode, allocaNo);
		return true;
	}

	/**
	 * Determine the block where this alloca is live.
	 * <p>
	 * <p>
	 * Knowing that allows us to avoid inserting PhiNode node into blocks which
	 * don't lead to use(thus, the phi node inserted would be dead).
	 * </p>
	 *
	 * @param ai
	 * @param info
	 * @param defBlocks
	 * @param liveInBlocks
	 */
	private void computeLivenessBlocks(AllocaInst ai,
            AllocaInfo info,
            HashSet<BasicBlock> defBlocks,
            HashSet<BasicBlock> liveInBlocks)
	{
		// To determine liveness, we must iterate through the predecessors of blocks
		// where the def is live.  Blocks are added to the worklist if we need to
		// check their predecessors.  Start with all the using blocks.
		LinkedList<BasicBlock> liveBlockWorkList = new LinkedList<>();
		liveBlockWorkList.addAll(info.usingBlocks);

		// If any of the using blocks is also a definition block, check to see if the
		// definition occurs before or after the use.  If it happens before the use,
		// the value isn't really live-in.
		for (int idx = 0; idx < liveBlockWorkList.size(); ++idx)
		{
			BasicBlock BB = liveBlockWorkList.get(idx);
			if (!defBlocks.contains(BB))
				continue;

			for (Instruction inst : BB)
			{
				if (inst instanceof StoreInst)
				{
					StoreInst si = (StoreInst) inst;
					if (!si.operand(1).equals(ai))
						continue;

					// We found a store to the alloca before a load.  The alloca is not
					// actually live-in here.
					if (liveBlockWorkList.size() > 1)
					{
						liveBlockWorkList.set(idx, liveBlockWorkList.pollLast());

						--idx;
						break;
					}
				}
				if (inst instanceof LoadInst)
				{
					LoadInst li = (LoadInst) inst;
					if (!li.operand(0).equals(ai))
						continue;

					// Okay, we found a load before a store to the alloca.  It is actually
					// live into this block.
					break;
				}
			}
		}
		// Now that we have a set of blocks where the phi is live-in, recursively add
		// their predecessors until we find the full region the value is live.
		while (!liveBlockWorkList.isEmpty())
		{
			BasicBlock bb = liveBlockWorkList.pollLast();
			// if BB is already in the set, then it has already been processed.
			if (!liveInBlocks.add(bb))
				continue;

			// Since the value is live in the BB, so it is either defined in a
			// predesessor or live in it. Add the preds to the worklist unless
			// they are a defined block.
			PredIterator<BasicBlock> itr = bb.predIterator();
			while (itr.hasNext())
			{
				BasicBlock pred = itr.next();
				// exclude defined block
				if (defBlocks.contains(pred))
					continue;
				liveBlockWorkList.addLast(pred);
			}
		}
	}

	/**
	 * <p>
	 * Rewrites the loads of it that directly dominated by the a single store
	 * to this AllocaInst instruction with the value stored.
	 * </p>
	 * <p>
	 * When there is only a single store, we can use the domtree to trivially
	 * replace all of the dominated loads with the stored value. Do so, and return
	 * true if this has successfully promoted the alloca entirely. If this returns
	 * false there were some loads which were not dominated by the single store
	 * and thus must be phi-ed with undef. We fall back to the standard alloca
	 * </p>
	 *
	 * @param ai   The alloca instruction.
	 * @param info AllocaInst information analysis for rewriting.
	 * @param lbi  The large block information for backend.transform.
	 */
	private void rewriteSingleStoreAlloca(AllocaInst ai,
            AllocaInfo info,
			LargeBlockInfo lbi)
	{
		// the last definition of alloca
		StoreInst onlyStore = info.onlyStore;
		BasicBlock storeBB = onlyStore.getParent();
		boolean storingGlobalValue = !(onlyStore.operand(0) instanceof Instruction);

		// for dominance relation determination
		int storeIndex = -1;
		info.usingBlocks.clear();

		// handle loads to store by traveling over usesList list
		ArrayList<Use> tempList = new ArrayList<>();
		tempList.addAll(ai.usesList);
		for (Use u : tempList)
		{
			User user = u.getUser();
			if (!(user instanceof LoadInst))
			{
				Util.assertion((user instanceof StoreInst),  "Should only have store/load instruction.");
				continue;
			}

			LoadInst li = (LoadInst)user;
			// we just do than if the load dominated by store
			// otherwise, we use rest of the mem2reg machinery
			// to insert phi-node as appropriate.
            if (!storingGlobalValue)
            {
                if (li.getParent().equals(storeBB))
                {
                    // if the load and store are in a same block, compare the
                    // indices of the two instruction to see which one come first.
                    // If the load came before the store, don't handle it.
                    if (storeIndex == -1)
                        storeIndex = lbi.getIndexOfInstruction(onlyStore);
                    if (storeIndex > lbi.getIndexOfInstruction(li))
                    {
                        info.usingBlocks.add(li.getParent());
                        continue;
                    }
                }
                else if (!li.getParent().equals(storeBB)
                        && !dt.dominates(storeBB, li.getParent()))
                {
                    // if load and store are in different basic block,
                    // using dominance to check that their relationship.
                    // if the load doesn't dominated by store, just bail out.
                    info.usingBlocks.add(li.getParent());
                    continue;
                }
            }

			// At this point, knows that the loads is dominated by stores
			// So, we can saftly rewrite the load with the value stores to alloca.
			Value value = onlyStore.operand(0);
			if (value.equals(li))
				value = UndefValue.get(value.getType());
			li.replaceAllUsesWith(value);

			if (ast != null && (li.getType() instanceof PointerType))
			    ast.deleteValue(li);

			// remote it from it's basic block
			li.eraseFromParent();
			lbi.deleteValue(li);
		}// end of go through usesList
	}

	/**
	 * Removes the processed alloca instruction out from allocas list.
	 *
	 * @param allocaNum The index that indexed into the AllocaInst would be
     *                  deleted.
	 */
	private int removeFromAllocasList(int allocaNum)
	{
	    int lastIdx = allocas.size() - 1;
        allocas.set(allocaNum, allocas.get(lastIdx));
        allocas.remove(lastIdx);
        return allocaNum-1;
	}

	/**
	 * Packaged data used for rename pass.
	 */
	static final class RenamePassData
	{
		BasicBlock BB;
		BasicBlock pred;
		ArrayList<Value> values;

		public RenamePassData()
		{
			this.BB = null;
			this.pred = null;
			values = null;
		}

		public RenamePassData(BasicBlock BB, BasicBlock pred, ArrayList<Value> values)
		{
			this.BB = BB;
			this.pred = pred;
			this.values = values;
		}

		public void swap(RenamePassData other)
		{
			// swap BB
			BasicBlock otherBB = other.BB;
			other.BB = this.BB;
			this.BB = otherBB;

			// swap pred
			BasicBlock otherPred = other.pred;
			other.pred = this.pred;
			this.pred = otherPred;

			// swap VALUES
            ArrayList<Value> otherVal = this.values;
			this.values = other.values;
			other.values = otherVal;
		}
	}

	/**
	 * This keeps a per-parent relative ordering of load/store instructions
	 * in the block that directly load or store an alloca.
	 * <p>
	 * <p>
	 * This class is greatly important since it avoids scanning large
	 * basic blocks multiple times when promoting many allocas in the
	 * same block.
	 * </p>
	 */
	static class LargeBlockInfo
	{
		/**
		 * Keeps the index of instruction for each instruction that we tack.
		 */
		TObjectIntHashMap<Value> instNumbers = new TObjectIntHashMap<>();

		public boolean isIntertestingInstruction(Instruction inst)
		{
			return ((inst instanceof LoadInst) && (inst.operand(0) instanceof AllocaInst))
                    || ((inst instanceof StoreInst) && (inst.operand(1) instanceof AllocaInst));
		}

		/**
		 * Gets the index of specified instruction at instructions list.
		 *
		 * @param inst The inst to be evaluated.
		 * @return return the index of it if legal, otherwise return -1.
		 */
		public int getIndexOfInstruction(Instruction inst)
		{
			Util.assertion(isIntertestingInstruction(inst),  "Not a load/store to/from an alloca?");


			// if it already exit in instNumbers list
			if (instNumbers.containsKey(inst))
				return instNumbers.get(inst);

			// scan the entirely basic block to get the instruction.
			// This accumulates information for every instertesting
			// instruction in the block, in order to avoid repeating
			// scanning
			BasicBlock bb = inst.getParent();

			for (int no = 0, e = bb.getNumOfInsts(); no < e; no++)
			{
				Instruction it = bb.getInstAt(no);
				if (isIntertestingInstruction(it))
					instNumbers.put(it, no);
			}
			Util.assertion(instNumbers.contains(inst),  "No this instruction in current basic block.");

			return instNumbers.get(inst);
		}

		public void removeInstruction(Instruction inst)
		{
			instNumbers.remove(inst);
		}

		public void clear()
		{
			instNumbers.clear();
		}

		/**
		 * Erases the specified value from map.
		 *
		 * @param inst
		 */
		public void deleteValue(Instruction inst)
		{
			Util.assertion(inst					!= null,
                    "LargeBlockInformation.deleteValue(<null>) is invalid");
			instNumbers.remove(inst);
		}
	}

	/**
	 * A class for recording the uses  and definition information
	 * of {@code AllocaInst}instrcution.
	 */
	static class AllocaInfo
	{
		/**
		 * The list of block where there is a stores to alloca instruction
		 * (also, it is a definition to variable).
		 */
		LinkedList<BasicBlock> definingBlocks = new LinkedList<>();
		/**
		 * The list of blocks where there is a loads to alloca (also, it is a usesList
		 * to defined variable).
		 */
		LinkedList<BasicBlock> usingBlocks = new LinkedList<>();

		/**
		 * The last definition of this alloca among subsequnce of stores.
		 */
		StoreInst onlyStore;

		/**
		 * The only block when {@link #onlyUsedOneBlock} is {@code true}.
		 */
		BasicBlock onlyBlock;
		/**
		 * whether alloca used in only one block.
		 */
		boolean onlyUsedOneBlock;

		/**
		 * The pointer to allocated variable.
		 */
		Value allocaPointerVar;

		/**
		 * Clears all of occupied resourcee, so that it can be recycled
		 */
		void clear()
		{
			definingBlocks.clear();
			usingBlocks.clear();
			onlyStore = null;
			onlyBlock = null;
			onlyUsedOneBlock = true;
			allocaPointerVar = null;
		}

		/**
		 * Scan that usesList of the specified alloca, filling in the AllocaInfo
		 * used by the rest of the class to reason the usesList of this instruction.
		 *
		 * @param alloca An {@code AllocaInst} instruction to be analyzed.
		 */
		void analyzeAlloca(AllocaInst alloca)
		{
			clear();

			/*
			 * scaning the usesList of the alloca instruction, and keeping track of
			 * stores, and decide whether all of loads and stores to the alloca
			 * are within a same basic block.
			 */
			for (Use u : alloca.usesList)
			{
				Instruction inst = (Instruction) u.getUser();
				if (inst instanceof StoreInst)
				{
					// remember the basic block where store instruction define a
					// new value for alloca instruction.
                    StoreInst si = (StoreInst)inst;
					definingBlocks.add(inst.getParent());
					allocaPointerVar = si.operand(0);
					onlyStore = si;
				}
				else if (inst instanceof LoadInst)
				{
					// otherwise it must be a load instruction, keep track of
					// variable reads
					LoadInst li = (LoadInst) inst;
					usingBlocks.add(li.getParent());
					allocaPointerVar = li;
				}
				else
                {
                    Util.assertion(inst.hasOneUses(), "unexpected use for bitcast");
                    DbgInfoIntrinsic dbg = (DbgInfoIntrinsic)inst.useAt(0).getUser();
                    inst.eraseFromParent();
                    dbg.eraseFromParent();
                    continue;
                }

				if (onlyUsedOneBlock)
				{
					if (onlyBlock == null)
						onlyBlock = inst.getParent();
					else if (!onlyBlock.equals(inst.getParent()))
						onlyUsedOneBlock = false;
				}
			}
		}
	}
}
