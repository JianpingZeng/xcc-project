package backend.transform;

import backend.analysis.AliasSetTracker;
import backend.analysis.DomTreeInfo;
import backend.analysis.DomTreeNodeBase;
import backend.hir.BasicBlock;
import backend.analysis.DomTree;
import backend.analysis.DomTree.DomTreeNode;
import backend.value.*;
import backend.value.Instruction.AllocaInst;
import backend.value.Instruction.LoadInst;
import backend.value.Instruction.PhiNode;
import backend.value.Instruction.StoreInst;
import backend.value.Value.UndefValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeSet;

import gnu.trove.map.hash.TObjectIntHashMap;
import tools.Pair;

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
public final class PromoteMem2Reg
{
	/**
	 * The list of {@code AllocaInst} to be promoted.
	 */
	private List<AllocaInst> allocas;

	private DomTreeInfo dt;

	/**
	 * A reverse mapping of allocas
	 */
	private HashMap<AllocaInst, Integer> allocaLookup;

	/**
	 * The phi node we are adding.
	 * <p>
	 * That map is used to simplify some PhiNode nodes as we
	 * iterate over it, so it should have deterministic iterators.
	 */
	private HashMap<Pair, PhiNode> newPhiNodes;

	private TObjectIntHashMap<PhiNode> phiToAllocaMap;

	/**
	 * The set of basic block the renamaer has already visited.
	 */
	private HashSet<BasicBlock> visitedBlocks;

	private TObjectIntHashMap<BasicBlock> bbNumbers;

	/**
	 * The numbers of dead alloca instruction to be removed from parent block.
	 */
	private int numberDeadAlloca = 0;

	/**
	 * The numbers of promotable alloca instruction to be promoted.
	 */
	private int numberPromotedAlloca = 0;

	/**
	 * The numbers of alloca instruction that just have only one stores(definition).
	 */
	private int numberSingleStore = 0;

	/**
	 * The number of local alloca to be promoted successfully.
	 */
	private int numberLocalPromoted = 0;

	private int numberPhiInsert = 0;

	/**
	 * Maps the DomTreeNode to it's level at dominatro tree.
	 */
	private TObjectIntHashMap<DomTreeNodeBase<BasicBlock>> domLevels;

	public static void promoteMemToReg(ArrayList<AllocaInst> allocas,
            DomTreeInfo dt,
            DominatorFrontier df, AliasSetTracker ast)
    {
        if (allocas.isEmpty()) return;
        new PromoteMem2Reg(allocas, dt, df, ast);
    }

	public PromoteMem2Reg(){}

	public PromoteMem2Reg(ArrayList<AllocaInst> allocas,
            DomTreeInfo dt,
            DominatorFrontier df,
            AliasSetTracker ast)
	{
		this.allocas = allocas;
		this.dt = dt;
		allocaLookup = new HashMap<>();
		newPhiNodes = new HashMap<>();
		visitedBlocks = new HashSet<>();
		domLevels = new TObjectIntHashMap<>();
		bbNumbers = new TObjectIntHashMap<>();
		phiToAllocaMap = new TObjectIntHashMap<>();
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
	@Override
	public boolean runOnFunction(Function f)
	{
		Function m = this.dt.getRootNode().getBlock().getCFG().getMethod();
		assert (m != null) : "The method of this Dominator Tree is null";
		AllocaInfo info = new AllocaInfo();
		LargeBlockInfo LBI = new LargeBlockInfo();

		// promotes every alloca instruction one by one.
		for (AllocaInst AI : allocas)
		{
			assert AI
					.isAllocaPromotable() : "Cann't promote non-promotable alloca";
			assert AI.getParent().getCFG().getMethod()
					!= m : "All allocas should in the same method, which is same as DF!";

			// if it's use isDeclScope intrinsic instruction, just remove it from
			// attached basic block.
			// However, it is not finished currently
			if (AI.usesList.isEmpty())
			{
				// if there no usesList of the alloca, just delete it
				AI.eraseFromBasicBlock();

				// remove the alloca out from alloca instructions list
				// because it is processed finished.
				removeFromAllocasList(AI);
				++numberDeadAlloca;
				continue;
			}

			// Calculate the set of read and write-location(basic block) for
			// each alloca. This is analogous to finding the 'use' and
			// 'definition' of each variable.
			info.analyzeAlloca(AI);

			// if there is only a single store to this value, this is said
			// that there is only a single definition of this variable.
			// replace any loads of it that are directly dominated by the
			// definition with the value stored.
			if (info.definingBlocks.size() == 1)
			{
				if (rewriteSingleStoreAlloca(AI, info, LBI, dt))
				{
					// the alloca instruction has been processed, remove it
					removeFromAllocasList(AI);
					++numberSingleStore;
					continue;
				}
			}

			// IfStmt the alloca is only read and written in one block.
			// just perform a linear scan sweep over the block to
			// eliminate it.
			if (info.onlyUsedOneBlock)
			{
				promoteSingleBlockAlloca(AI, info, LBI);

				// the alloca instruction has been processed, remove it
				removeFromAllocasList(AI);
				continue;
			}

			// if we haven't computed dominator tree level, just do it
			// with width-first traverse.
			if (domLevels.isEmpty())
			{
				LinkedList<DomTree.DomTreeNode> worklist = new LinkedList<>();

				DomTree.DomTreeNode root = dt.getRootNode();
				domLevels.put(root, 0);
				worklist.addLast(root);

				while (!worklist.isEmpty())
				{
					DomTree.DomTreeNode node = worklist.removeLast();
					// the next level
					int childLevel = domLevels.get(node) + 1;
					for (DomTree.DomTreeNode dom : node)
					{
						domLevels.put(dom, childLevel);
						worklist.addLast(dom);
					}
				}
			}

			// IfStmt we haven't computed a numbering for the BB's in the function, do so
			// now.
			if (bbNumbers.isEmpty())
			{
				int id = 0;
				for (BasicBlock BB : m)
					bbNumbers.put(BB, Integer.valueOf(id));
			}

			// IfStmt we haven't computed a numbering for the BB's in the function,
			// do so now.

			int allocaNum = allocas.indexOf(AI);
			// Keeps the reverse mapping of the 'Allocas' array for the rename pass.
			allocaLookup.put(AI, allocaNum);

			// Using standard SSA construction algorithm to promoting the alloca.
			// Determine which blocks need PHI nodes and see if we can optimize out
			// some work by avoiding insertion of dead phi nodes.
			determineInsertionPoint(AI, allocaNum, info);
		}// end of traveling alloca instruction list.

		// all of allocas must has been handled
		// just return.
		if (allocas.isEmpty())
			return;

		LBI.clear();

		Value[] values = new Value[allocas.size()];
		for (int idx = 0; idx < allocas.size(); idx++)
			values[idx] = UndefValue.get(allocas.get(idx).kind);

		// wolk all basic block in the function performing
		// SSA construction algorithm and inserting the phi nodes
		// we marked as necessary.
		LinkedList<RenamePassData> renamePassWorkList = new LinkedList<>();
		renamePassWorkList
				.addLast(new RenamePassData(m.getEntryBlock(), null, values));

		do
		{
			RenamePassData rpd = new RenamePassData();
			rpd.swap(renamePassWorkList.removeLast());

			renamePass(rpd.BB, rpd.pred, rpd.values, renamePassWorkList);

		} while (!renamePassWorkList.isEmpty());

		visitedBlocks.clear();

		// Remove the allocas themselves from the function.
		for (AllocaInst AI : allocas)
		{
			// IfStmt there are any usesList of the alloca instructions left, they must be in
			// unreachable basic blocks that were not processed by walking the dominator
			// tree. Just delete the users now.
			if (!AI.usesList.isEmpty())
				AI.replaceAllUsesWith(UndefValue.get(AI.kind));
			AI.eraseFromBasicBlock();
		}

		// Loop over all of the PHI nodes and see if there are any that we can
		// get rid of because they merge all of the same incoming VALUES.  This can
		// happen due to undef VALUES coming into the PHI nodes.  This process is
		// iterative, because eliminating one PHI node can cause others to be removed.
		boolean eliminatedAPHI = true;

		while (eliminatedAPHI)
		{
			eliminatedAPHI = false;
			for (Map.Entry<Pair, PhiNode> entity : newPhiNodes.entrySet())
			{
				PhiNode phiNode = entity.getValue();

				Instruction V;
				// if the phiNode merges one value and/or undefs, get the value
				if ((V = simplifyInstruction(phiNode, dt)) != null)
				{
					phiNode.replaceAllUsesWith(V);
					phiNode.eraseFromBasicBlock();
					newPhiNodes.remove(entity);
					eliminatedAPHI = true;
					continue;
				}
			}
		}// end of while eliminatedPhi

		// At this point, the renamer has added entries to PHI nodes for all reachable
		// code.  Unfortunately, there may be unreachable blocks which the renamer
		// hasn't traversed.  IfStmt this is the case, the PHI nodes may not
		// have incoming VALUES for all predecessors.  Loop over all PHI nodes we have
		// created, inserting undef VALUES if they are missing any incoming VALUES.
		//
		for (Map.Entry<Pair, PhiNode> entity : newPhiNodes.entrySet())
		{
			PhiNode phiNode = entity.getValue();
			BasicBlock BB = phiNode.getParent();

			// We want to do this once per basic block.  As such, only process a block
			// when we find the PHI that is the first entry in the block.
			if (BB.getFirstInst() != phiNode)
				continue;

			// Only do work here if the phiNode node are missing incoming VALUES.
			if (phiNode.getNumberIncomingValues() == BB.getNumOfPreds())
				continue;

			List<BasicBlock> preds = new LinkedList<>();
			preds.addAll(BB.getPreds());

			// loop through all BB which have entity in specified phiNode
			// and remove them from preds list.
			for (int idx = 0; idx < phiNode.getNumberIncomingValues(); idx++)
			{
				BasicBlock incomingBlock = phiNode.getIncomingBlock(idx);
				if (preds.contains(incomingBlock))
					preds.remove(incomingBlock);
			}

			int numBadPreds = phiNode.getNumberIncomingValues();
			Iterator<Value> it = BB.iterator();
			Value inst;

			while (it.hasNext() && ((inst = it.next()) instanceof PhiNode
					&& (phiNode = (PhiNode) inst).getNumberIncomingValues()
					== numBadPreds))
			{
				Value undef = UndefValue.get(phiNode.kind);
				for (BasicBlock pred : preds)
					phiNode.addIncoming(undef, pred);
			}
		}// end of

		newPhiNodes.clear();
	}

	/**
	 * See if we can compute a simplified version of phiNode instruction.
	 * IfStmt not, this return null.
	 *
	 * @param phiNode
	 * @param DT
	 * @return
	 */
	private Instruction simplifyInstruction(PhiNode phiNode, DomTree DT)
	{
		return null;
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
	 * @param BB              The Basic CompoundStmt where all variable (alloca) will be renamed.
	 * @param pred            The predecessor of BB.
	 * @param incomgingValues
	 * @param worklist        The list of basic blocks to be renamed.
	 */
	private void renamePass(BasicBlock BB, BasicBlock pred,
                            Value[] incomgingValues, LinkedList<RenamePassData> worklist)
	{
		Value inst;
		HashSet<BasicBlock> visitedSuccs = new HashSet<>();

		while (true)
		{
			// determine whether any phi node already be in the block.
			if ((inst = BB.getFirstInst()) instanceof PhiNode)
			{
				PhiNode phiNode = (PhiNode) inst;
				// to distinguish between phiNode node being inserted by this invocation
				// of mem2reg from those phiNode nodes that already existed in the Module
				// before mem2reg was run.
				if (phiToAllocaMap.containsKey(phiNode))
				{
					int newPhiNumOperands = phiNode.getNumberIncomingValues();
					int numEdges = Collections.frequency(pred.getSuccs(), BB);
					assert numEdges
							> 0 : "Must be at least one edge form pred to BB!";

					int idx = 1;
					do
					{
						int allocaNo = phiToAllocaMap.get(phiNode);

						// sets the undef value for phiNode node, it is reason that
						// handling loop.
						for (int j = 0; j < numEdges; ++j)
							phiNode.addIncoming(incomgingValues[allocaNo], pred);

						// the currently active variable for this block is now
						// the phiNode.
						incomgingValues[allocaNo] = phiNode;

						// no more instruction
						if (idx >= BB.size())
							break;
						inst = BB.getInstAt(idx++);
						// the handling phiNode node has finished!
						if (!(inst instanceof PhiNode))
							break;
						phiNode = (PhiNode) inst;
					} while (phiNode.getNumberIncomingValues()
							== newPhiNumOperands);
				}
			}

			// don't revisit blocks
			if (!visitedBlocks.add(BB))
				return;

			// handles subsequnce instruction at control flow graph.
			Iterator<Value> it = BB.iterator();
			while (it.hasNext())
			{
				inst = it.next();

				// Only load and store to alloca instruction will be handled,
				// because at our Module, the usesList of alloca just isDeclScope laods
				// and stores.
				if (inst instanceof LoadInst)
				{
					LoadInst LI = (LoadInst) inst;
					AllocaInst src = LI.from;
					if (src == null)
						continue;

					Integer index = allocaLookup.get(src);
					if (index == null)
						continue;

					// gets the active value of current alloca with index.
					Value value = incomgingValues[index];
					// anything using the load now usesList the current value.
					LI.replaceAllUsesWith(value);

					LI.eraseFromBasicBlock();
				}
				else if (inst instanceof StoreInst)
				{
					StoreInst SI = (StoreInst) inst;
					AllocaInst dest = SI.dest;
					if (dest == null)
						continue;

					Integer index = allocaLookup.get(dest);
					if (index == null)
						continue;
					// what value were we writing?
					incomgingValues[index] = SI.value;

					SI.eraseFromBasicBlock();
				}
			}

			if (BB.getSuccs().isEmpty())
				return;

			visitedSuccs.clear();
			pred = BB;
			BB = BB.getSuccs().get(0);
			// recurse to successor
			for (int idx = 1; idx < BB.getSuccs().size(); idx++)
			{
				BasicBlock succ = BB.getSuccs().get(idx);
				if (visitedSuccs.add(succ))
					worklist.addLast(
							new RenamePassData(succ, pred, incomgingValues));
			}
		}
	}

	/**
	 * Many allocas are only used within a single basic block.  IfStmt this is the
	 * case, avoid traversing the CFG and inserting a lot of potentially useless
	 * PHI nodes by just performing a single linear pass over the basic block
	 * using the AllocaInst.
	 *
	 * @param AI
	 * @param info
	 * @param LBI
	 */
	private void promoteSingleBlockAlloca(AllocaInst AI, AllocaInfo info,
			LargeBlockInfo LBI)
	{
		// sort the stores by their index, making it efficient to do lookup.
		TreeSet<Pair> storesByIndex = new TreeSet<>(new Comparator<Pair>()
		{
			@Override public int compare(Pair o1, Pair o2)
			{
				if ((Integer) o1.first < (Integer) o2.second)
					return -1;
				else if (o1.first == o2.second)
					return 0;
				else
					return 1;
			}
		});
		for (Use inst : AI.usesList)
		{
			User user = inst.getUser();
			if (user instanceof StoreInst)
				storesByIndex.add(new Pair(
						LBI.getIndexOfInstruction((Instruction) user), user));
		}
		// Walk all of the loads from this alloca, replacing them with the
		// nearest store above them, if any.
		for (Use use : AI.usesList)
		{
			User UI = use.getUser();
			if (!(UI instanceof LoadInst))
				continue;
			LoadInst LI = (LoadInst) UI;
			int loadIndex = LBI.getIndexOfInstruction(LI);

			Pair target = storesByIndex.floor(new Pair(loadIndex, null));
			// if there is no stores before load, this load take undef value.
			if (target == null)
				LI.replaceAllUsesWith(UndefValue.get(LI.kind));
				// otherwise, there was store before load, the load just toke its value
			else
				LI.replaceAllUsesWith(((StoreInst) target.second).value);

			// now, this load instruction is not useful
			LI.eraseFromBasicBlock();
			LBI.deleteValue(LI);
		}

		// 鍘婚櫎鏃犵敤鐨剆tore鍜宎lloca鎸囦护锛屽洜涓轰粠alloca涓娇鐢╨oad鎸囦护鍔犺浇杩涚殑鍊煎凡缁忕洿鎺�
		//浼犻�佸埌浜唋oad鐨勪娇鐢ㄥ锛岄偅涔堣store鎸囦护灏变笉闇�瑕佷簡銆�
		for (Use u : AI.usesList)
		{
			User inst = u.getUser();
			if (inst instanceof StoreInst)
			{
				((StoreInst) inst).eraseFromBasicBlock();
				LBI.deleteValue((Instruction) inst);
			}
		}

		// 姝ゆ椂锛屼篃鍙互鍒犻櫎璇lloca鎸囦护
		AI.eraseFromBasicBlock();
		LBI.deleteValue(AI);

		++numberLocalPromoted;
	}

	/**
	 * Using standard SSA construction algorithm to promoting the alloca.
	 * Determine which blocks need PHI nodes and see if we can optimize out
	 * some work by avoiding insertion of dead phi nodes.
	 *
	 * @param AI        The alloca to be promoted.
	 * @param allocaNum The index of alloca into allocas list.
	 * @param info      The information relative to alloca.
	 */
	private void determineInsertionPoint(AllocaInst AI, int allocaNum,
			AllocaInfo info)
	{
		// 璇ュ嚱鏁扮殑鐩殑灏辨槸鑾峰彇AI鎸囦护鐨勬敮閰嶈竟鐣岄泦锛岀劧鍚庢斁缃甈hi鍑芥暟銆�
		// 浠庝笅寰�涓婇亶鍘嗘敮閰嶆爲
		HashSet<BasicBlock> defBlocks = new HashSet<>();
		defBlocks.addAll(info.definingBlocks);

		// 鍒ゆ柇璇ュ�煎湪鍝竴涓熀鏈潡涓槸娲昏穬鐨�
		HashSet<BasicBlock> liveInBlocks = new HashSet<>();
		computeLifenessBlocks(AI, info, defBlocks, liveInBlocks);

		// 浣跨敤涓�涓紭鍏堢骇闃熷垪锛屾寜鐓у湪鏀厤鏍戜腑鐨勫眰娆★紝瓒婃繁鐨勭粨鐐规斁鍦ㄥ墠闈�
		PriorityQueue<Pair<DomTreeNode, Integer>> PQ = new PriorityQueue<>(32,
				new Comparator<Pair<DomTreeNode, Integer>>()
				{
					@Override public int compare(Pair<DomTreeNode, Integer> o1,
							Pair<DomTreeNode, Integer> o2)
					{
						return -1;
					}
				});

		DomTree.DomTreeNode node;
		for (BasicBlock BB : defBlocks)
			if ((node = dt.getTreeNodeForBlock(BB)) != null)
				PQ.add(new Pair<>(node, domLevels.get(node)));

		// 瀛樺偍璇lloca鐨勬敮閰嶈竟鐣岄泦
		ArrayList<Pair<Integer, BasicBlock>> DFBlocks = new ArrayList<>(32);

		LinkedList<DomTreeNode> worklist = new LinkedList<>();
		HashSet<DomTreeNode> visited = new HashSet<>(32);

		// 浠庡湪鏀厤鏍戜腑鏈�搴曞眰鐨勫畾涔夊潡寮�濮嬪悜涓婁竴涓竴涓殑閬嶅巻锛�
		// 鍦ㄦ瘡涓熀鏈潡鐨勬敮閰嶈竟鐣屼腑鏀惧叆Phi缁撶偣銆�
		while (!PQ.isEmpty())
		{
			Pair<DomTreeNode, Integer> rootPair = PQ.poll();
			DomTreeNode rootNode = rootPair.first;
			int rootLevel = rootPair.second;

			worklist.clear();
			worklist.addLast(rootNode);

			while (!worklist.isEmpty())
			{
				DomTreeNode Node = worklist.removeLast();
				BasicBlock BB = Node.getBlock();

				for (BasicBlock succ : BB.getSuccs())
				{
					DomTreeNode succNode = dt.getTreeNodeForBlock(succ);

					// 璺宠繃鎵�鏈塀B鍧楁墍鏀厤鐨勭殑鍧�
					if (succNode.getIDom() == Node)
						continue;

					int succLevel = domLevels.get(succNode);
					if (succLevel > rootLevel)
						continue;

					// skip the visisted dom tree node
					if (!visited.add(succNode))
						continue;

					// skip the block where alloca is not live in
					BasicBlock succBlock = succNode.getBlock();
					if (!liveInBlocks.contains(succBlock))
						continue;

					DFBlocks.add(
							new Pair<>(bbNumbers.get(succBlock), succBlock));
					if (!defBlocks.contains(succBlock))
						PQ.offer(new Pair<>(succNode, succLevel));
				}// end for successor

				for (DomTreeNode domNode : Node)
					if (!visited.contains(domNode))
						worklist.addLast(domNode);
			}
		}

		// 鎸夌収缂栧彿浠庝笅鍒板ぇ鐨勬柟寮忎緷娆℃帓搴�
		if (DFBlocks.size() > 1)
		{
			Collections
					.sort(DFBlocks, new Comparator<Pair<Integer, BasicBlock>>()
					{
						@Override
						public int compare(Pair<Integer, BasicBlock> o1,
								Pair<Integer, BasicBlock> o2)
						{
							if (o1.first < o2.first)
								return -1;
							else if (o1.first == o2.first)
								return 0;
							else
								return 1;
						}
					});
		}
		// 鎻掑叆phi鍑芥暟
		int currentVersion = 0;
		for (int idx = 0, e = DFBlocks.size(); idx != e; ++idx)
		{
			queuePhiNode(DFBlocks.get(idx).second, allocaNum, currentVersion);
		}
	}

	/**
	 * Queue a phi-node to be added to a basic block in dominator frontier
	 * for a specific alloca.
	 *
	 * @param BB       The dominator frontier block.
	 * @param allocaNo The number of alloca.
	 * @param Version  The current version.
	 */
	private boolean queuePhiNode(BasicBlock BB, int allocaNo, int Version)
	{
		PhiNode phiNode = newPhiNodes.get(new Pair(bbNumbers.get(BB), allocaNo));

		// if the specific BB already has a phiNode node added for the i-th alloca
		// and the we have done.
		if (phiNode != null)
			return false;

		AllocaInst AI = allocas.get(allocaNo);
		// create a phiNode node and add the phiNode-node into the basic block
		phiNode = new PhiNode(AI.kind, BB.getNumOfPreds(),
				AI.name() + "." + (Version++));
		BB.insertAfterFirst(phiNode);
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
	 * @param AI
	 * @param info
	 * @param defBlocks
	 * @param liveInBlocks
	 */
	private void computeLifenessBlocks(AllocaInst AI, AllocaInfo info,
                                       HashSet<BasicBlock> defBlocks, HashSet<BasicBlock> liveInBlocks)
	{
		// To determine liveness, we must iterate through the predecessors of blocks
		// where the def is live.  Blocks are added to the worklist if we need to
		// check their predecessors.  Start with all the using blocks.
		LinkedList<BasicBlock> liveBlockWorkList = new LinkedList<>();
		liveBlockWorkList.addAll(info.usingBlocks);

		// IfStmt any of the using blocks is also a definition block, check to see if the
		// definition occurs before or after the use.  IfStmt it happens before the use,
		// the value isn't really live-in.
		for (int idx = 0; idx < liveBlockWorkList.size(); ++idx)
		{
			BasicBlock BB = liveBlockWorkList.get(idx);
			if (!defBlocks.contains(BB))
				continue;

			for (Value inst : BB)
			{
				if (inst instanceof StoreInst)
				{
					if (((StoreInst) inst).dest != AI)
						continue;

					// We found a store to the alloca before a load.  The alloca is not
					// actually live-in here.
					liveBlockWorkList.set(idx, liveBlockWorkList.pollLast());
					--idx;
					break;
				}
				if (inst instanceof LoadInst)
				{
					LoadInst LI = (LoadInst) inst;
					if (LI.from != AI)
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
			BasicBlock BB = liveBlockWorkList.pollLast();
			// if BB is already in the set, then it has already been processed.
			if (!liveInBlocks.add(BB))
				continue;

			// Since the value is live in the BB, so it is either defined in a
			// predesessor or live in it. Add the preds to the worklist unless
			// they are a defined block.
			for (BasicBlock pred : BB.getPreds())
			{
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
	 * true if this has successfully promoted the alloca entirely. IfStmt this returns
	 * false there were some loads which were not dominated by the single store
	 * and thus must be phi-ed with undef. We fall back to the standard alloca
	 * </p>
	 *
	 * @param AI   The alloca instruction.
	 * @param info AllocaInst information analysis for rewriting.
	 * @param LBI  The large block information for backend.transform.
	 * @param DT   The dominator tree for calculating dominator frontier.
	 * @return return true if rewriting successfully.
	 */
	private boolean rewriteSingleStoreAlloca(AllocaInst AI, AllocaInfo info,
			LargeBlockInfo LBI, DomTree DT)
	{
		// the last definition of alloca
		StoreInst onlyStore = info.onlyStore;
		BasicBlock storeBB = onlyStore.getParent();
		// for dominatance relation determination
		int storeIndex = -1;
		info.usingBlocks.clear();

		// handle loads to store by traveling over usesList list
		for (Use u : AI.usesList)
		{
			User UI = u.getUser();
			if (!(UI instanceof LoadInst))
			{
				assert (UI instanceof StoreInst) : "Should only have store/load instruction.";
				continue;
			}
			// 姝ゅ锛屾垜浠病蹇呰澶勭悊浣跨敤鍏ㄥ眬鍙橀噺鍒濆鍖栫殑alloca鎸囦护锛屽洜涓鸿繖鏍风殑
			// 璇濓紝浠绘剰鐨刲oad閮芥槸鍙梥tore鏀厤鐨勩��
			// 浣嗘槸锛岀洰鍓嶆殏鏃舵湭瀹炵幇鍏ㄥ眬鍙橀噺銆傘�傘��
			LoadInst LI = (LoadInst) UI;
			// we just do than if the load dominated by store
			// otherwise, we use rest of the mem2reg machinery
			// to insert phi-node as appropriate.
			if (LI.getParent() == storeBB)
			{
				// if the load and store are in a same block, compare the
				// indices of the two instrcution to see which one come first.
				// IfStmt the load came before the store, don't handle it.
				if (storeIndex < 0)
					storeIndex = LBI.getIndexOfInstruction(onlyStore);
				if (storeIndex > LBI.getIndexOfInstruction(LI))
				{
					info.usingBlocks.add(LI.getParent());
					continue;
				}
			}
			else if (LI.getParent() != storeBB && !DT
					.dominates(storeBB, LI.getParent()))
			{
				// if load and store are in different basic block,
				// using dominatence to check that their relationships.
				// if the load doesn't dominated by store, just bail out.
				info.usingBlocks.add(LI.getParent());
				continue;
			}

			// At this point, knows that the loads is dominated by stores
			// So, we can safty rewrite the load with the value stores to alloca.
			Value value = onlyStore.value;
			if (value == LI)
				value = UndefValue.get(value.kind);
			LI.replaceAllUsesWith(value);

			// remote it from it's basic block
			LI.eraseFromBasicBlock();
			LBI.deleteValue(LI);
		}// end of go through usesList

		// Finally, after the scan, check to see whether there are stores is left
		// if not, we will have to fall back to the rem
		if (!info.usingBlocks.isEmpty())
			return false;

		// Removes the now dead stores and alloca.
		info.onlyStore.eraseFromBasicBlock();
		LBI.deleteValue(info.onlyStore);

		AI.eraseFromBasicBlock();
		LBI.deleteValue(AI);
		return true;
	}

	/**
	 * Removes the processed alloca instruction out from allocas list.
	 *
	 * @param inst The alloca instruction to be removed.
	 */
	private void removeFromAllocasList(Instruction inst)
	{
		this.allocas.remove(inst);
	}

	@Override
	public String getPassName()
	{
		return "Promotes memory variable to register";
	}

	/**
	 * Packaged data used for rename pass.
	 */
	static final class RenamePassData
	{
		BasicBlock BB;
		BasicBlock pred;
		Value[] values;

		public RenamePassData()
		{
			this.BB = null;
			this.pred = null;
			values = null;
		}

		public RenamePassData(BasicBlock BB, BasicBlock pred, Value[] values)
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
			Value[] otherVal = this.values;
			this.values = other.values;
			other.values = otherVal;
		}
	}

	/**
	 * This keeps a per-bb relative ordering of load/store instructions
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
		Map<Value, Integer> instNumbers;

		public boolean isIntertestingInstruction(Value inst)
		{
			return (inst instanceof LoadInst) || (inst instanceof StoreInst);
		}

		/**
		 * Gets the index of specified instruction at instructions list.
		 *
		 * @param inst The inst to be evaluated.
		 * @return return the index of it if legal, otherwise return -1.
		 */
		public int getIndexOfInstruction(Instruction inst)
		{
			assert isIntertestingInstruction(
					inst) : "Not a load/store to/from an alloca?";

			// if it already exit in instNumbers list
			if (instNumbers.containsKey(inst))
				return instNumbers.get(inst).intValue();

			// scan the entirely basic block to get the instruction.
			// This accumulates information for every instertesting
			// instruction in the block, in order to avoid repeating
			// scanning
			BasicBlock BB = inst.getParent();
			int no = 0;
			for (Value it : BB)
			{
				if (isIntertestingInstruction(it))
					instNumbers.put(it, no++);
			}

			Integer it = instNumbers.get(inst);
			assert it != null : "No this instruction in current basic block.";
			return it.intValue();
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
			assert inst
					!= null : "LargeBlockInformation.deleteValue(<null>) is invalid";
			instNumbers.remove(inst);
		}
	}

	/**
	 * A class for recording the usesList and definition information
	 * of {@code AllocaInst}instrcution.
	 */
	static class AllocaInfo
	{
		/**
		 * The list of block where there is a stores to alloca instruction
		 * (also, it is a definition to variable).
		 */
		ArrayList<BasicBlock> definingBlocks = new ArrayList<>(32);
		/**
		 * The list of blocks where there is a loads to alloca (also, it is a usesList
		 * to defined variable).
		 */
		ArrayList<BasicBlock> usingBlocks = new ArrayList<>(32);

		/**
		 * The last definition of this alloca among subsequnce of stores.
		 */
		StoreInst onlyStore;

		/**
		 * The only block when {@link hir.PromoteMem2Reg.AllocaInfo#onlyUsedOneBlock}
		 * is {@code true}.
		 */
		BasicBlock onlyBlock;
		/**
		 * whether alloca used in only one block.
		 */
		boolean onlyUsedOneBlock;
		/**
		 * The pointer to allocated variable.
		 */
		Instruction allocaPointerVar;

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
					definingBlocks.add(inst.getParent());
					allocaPointerVar = ((StoreInst) inst).dest;
					onlyStore = (StoreInst) inst;
				}
				else
				{
					// otherwise it must be a load instruction, keep track of
					// variable reads
					LoadInst LI = (LoadInst) inst;
					usingBlocks.add(LI.getParent());
					allocaPointerVar = LI.from;
				}

				if (onlyUsedOneBlock)
				{
					if (onlyBlock == null)
						onlyBlock = inst.getParent();
					else if (onlyBlock != inst.getParent())
						onlyUsedOneBlock = false;
				}
			}

		}
	}
}
