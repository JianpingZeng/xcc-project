/*package backend.utils;

import tools.Util;
import backend.analysis.DomTree;
import backend.hir.CriticalEdgeFinder;
import backend.hir.Statistics;
import backend.value.BasicBlock;
import backend.value.Function;
import backend.value.Instruction;

import java.util.*;

public class ControlFlowGraph
{

	public static final int MAX_STRING_LENGTH = 65535;

	/**
	 * The function that this control flow graph represents.
	 *
	private Function attachedFunction;

	/**
	 * The entry basic block of this control flow graph.
	 *
	private BasicBlock startNode;
	/**
	 * The exit basic block of this control flow graph.
	 *
	private BasicBlock endNode;
	/**
	 * Current id of quads, used to generate unique id's.
	 *
	private int quad_counter;

	public int bbCounter = 0;

	/**
	 * The id for instruction of cfg.
	 *
	private int instID = 0;

	private final int START_ID = -1;
	private final int END_ID = 0;
	public Statistics stats;

	/**
	 * The linear-scan ordered list of blocks.
	 *
	private List<BasicBlock> orderedBlocks;
	/**
	 * Constructor that constructs an control flow graph.
	 *
	 * @param function the function that this graph represents.
	 *
	public ControlFlowGraph(Function function)
	{

		this.attachedFunction = function;
		// id of basic block begin with one.
		stats.blockCount = 1;
		// id of quad begin with zero.
		this.quad_counter = 0;

		this.stats = new Statistics();
	}

	public BasicBlock createStartNode()
	{
		this.startNode = BasicBlock.createStartNode(START_ID, "entry", this);
		return startNode;
	}

	public BasicBlock createEndNode()
	{
		endNode = BasicBlock.createEndNode(END_ID, "exit", this);
		return endNode;
	}

	/**
	 * Get the new id of current instruction.
	 *
	public int getInstID()
	{
		return this.instID++;
	}

	/**
	 * Returns the entry node.
	 *
	 * @return the entry node.
	 *
	public BasicBlock entry()
	{
		return startNode;
	}

	/**
	 * Returns the exit node.
	 *
	 * @return the exit node.
	 *
	public BasicBlock exit()
	{
		return endNode;
	}

	/**
	 * Returns the method this graph represents.
	 *
	 * @return the attached function.
	 *
	public Function getMethod()
	{
		return this.attachedFunction;
	}

	/**
	 * Create a new basic block in this control flow graph.  The new basic block
	 * is given a new, unique id id.
	 *
	 * @param bbName The getIdentifier of the basic block to be constructed.
	 * @return the newly created basic block.
	 *
	public BasicBlock createBasicBlock(String bbName)
	{

		return BasicBlock.createBasicBlock(bbCounter++, bbName, this);
	}

	/**
	 * Use with care after renumbering basic blocks.
	 *
	void updateBBcounter(int value)
	{
		stats.blockCount = value;
	}

	/**
	 * Returns a maximum on the id of basic blocks in this control flow graph.
	 *
	 * @return a maximum on the id of basic blocks in this control flow graph.
	 *
	public int getNumberOfBasicBlocks()
	{
		return stats.blockCount + 1;
	}

	public int getNumberOfQuads()
	{
		int total = 0;
		ListIterator<BasicBlock> i = reversePostOrderIterator();
		while (i.hasNext())
		{
			BasicBlock bb = i.next();
			total += bb.size();
		}
		return total;
	}

	/**
	 * Returns a new id id for a quad.
	 *
	public int getNewQuadID()
	{
		return ++quad_counter;
	}

	/**
	 * Returns the maximum id id for a quad.
	 *
	public int getMaxQuadID()
	{
		return quad_counter;
	}

	/**
	 * Returns an iteration of the basic blocks in this graph in reverse post order.
	 *
	 * @return an iteration of the basic blocks in this graph in reverse post order.
	 *
	public ListIterator<BasicBlock> reversePostOrderIterator()
	{
		return reversePostOrderIterator(startNode);
	}

	/**
	 * Returns an iteration of the basic blocks in this graph reachable from the given
	 * basic block in reverse post order, starting from the given basic block.
	 *
	 * @param start_bb basic block to start reverse post order from.
	 * @return an iteration of the basic blocks in this graph reachable from the
	 * given basic block in reverse post order.
	 *
	public ListIterator<BasicBlock> reversePostOrderIterator(
			BasicBlock start_bb)
	{
		return reversePostOrder(start_bb).listIterator();
	}

	/**
	 * Returns a list of basic blocks in reverse post order, starting at the
	 * given basic block.
	 *
	 * @param start_bb basic block to start from.
	 * @return a list of basic blocks in reverse post order, starting at the
	 * given basic block.
	 *
	public List<BasicBlock> reversePostOrder(BasicBlock start_bb)
	{

		java.util.LinkedList<BasicBlock> result = new java.util.LinkedList<>();
		boolean[] visited = new boolean[stats.blockCount + 1];
		reversePostOrder_helper(start_bb, visited, result, true);
		return Collections.unmodifiableList(result);
	}

	/**
	 * Returns a list of basic blocks in reverse post order, starting at the
	 * entry block.
	 *
	 * @return a list of basic blocks in reverse post order, starting at
	 * the entry.
	 *
	public List<BasicBlock> reversePostOrder()
	{

		java.util.LinkedList<BasicBlock> result = new java.util.LinkedList<>();
		boolean[] visited = new boolean[stats.blockCount + 1];
		reversePostOrder_helper(startNode, visited, result, true);
		return Collections.unmodifiableList(result);
	}
	/**
	 * Returns a list of basic blocks in post order, starting at the
	 * entry block.
	 *
	 * @return a list of basic blocks in post order, starting at
	 * the entry.
	 *
	public List<BasicBlock> postOrder()
	{
		java.util.LinkedList<BasicBlock> result = new java.util.LinkedList<>();
		boolean[] visited = new boolean[stats.blockCount + 1];

		postOrderHelper(startNode, visited, result, true);
		return result;
	}

	private void postOrderHelper(BasicBlock entry,
                                 boolean[] visited, LinkedList<BasicBlock> result, boolean direction)
	{
		if (visited[entry.getID()])
			return;

		LinkedList<BasicBlock> worklist = new LinkedList<>();
		worklist.addLast(entry);

		while (!worklist.isEmpty())
		{
			BasicBlock curr = worklist.getLast();

			List<BasicBlock> succs = direction ? curr.getSuccs() : curr.getPreds();
			if (!succs.isEmpty())
			{
				Collections.reverse(succs);
				for (BasicBlock bb : succs)
					worklist.addLast(bb);
			}
			else
			{
				visited[curr.getID()] = true;
				result.add(curr);
				worklist.removeLast();
			}
		}
	}

	/**
	 * Helper function to compute reverse post order.
	 *
	 * @param b         the start node.
	 * @param visited   a array that records visiting flag.
	 * @param result    reverse post order of all of basic node in this graph.
	 * @param direction whether forward or backward.
	 *

	private void reversePostOrder_helper(BasicBlock b, boolean[] visited,
                                         java.util.LinkedList<BasicBlock> result, boolean direction)
	{

		 There is a more understandable method that recursively traverse the CFG
		 as follows. This code commented here just for human reading due to stack
		 depth limitation when recursively calling to itself.

		 List<BasicBlock> bbs = direction ? b.getSuccs() : b.getPreds();
		 for (BasicBlock b2 : bbs)
		 {
		 reversePostOrder_helper(b2, visited, getReturnValue, direction);
		 }
		 getReturnValue.addFirst(b);


		if (visited[b.getID()])
			return;

		LinkedList<BasicBlock> worklist = new LinkedList<>();
		worklist.addLast(b);
		while (!worklist.isEmpty())
		{
			BasicBlock curr = worklist.removeLast();
			visited[curr.getID()] = true;
			result.add(curr);

			List<BasicBlock> succs = direction ? curr.getSuccs() : curr.getPreds();
			Collections.reverse(succs);
			for (BasicBlock bb : succs)
				worklist.addLast(bb);
		}
	}

	/**
	 * Computes the linear scan order for current CFG and return it.
	 * @return
	 *
	public List<BasicBlock> linearScanOrder(DomTree DT)
	{
		if (orderedBlocks == null)
		{
			CriticalEdgeFinder finder = new CriticalEdgeFinder(this);

			// #Step 1: iterate over reverse post order to find critical edge
			List<BasicBlock> reversePosts = reversePostOrder();

			for (BasicBlock block : reversePosts)
			{
				finder.apply(block);
			}

			// #Step 2: performs split over finded critical edges list.
			finder.splitCriticalEdges();

			// #Step3: computes linear scan order.
			ComputeLinearScanOrder com = new ComputeLinearScanOrder(stats.blockCount, startNode, DT);
			orderedBlocks = com.linearScanOrder();
			stats.loopCount = com.numLoops();
		}
		return orderedBlocks;
	}

	/**
	 * Creates and inserts a new basic block into the middle between {@code from}
	 * and {@code to}.
	 * @param from the source block of edge.
	 * @param to the successor block of edge.
	 * @return the new block inserted.
	 
	public BasicBlock splitEdge(BasicBlock from, BasicBlock to)
	{
		BasicBlock newSucc = createBasicBlock("CriticalEdge");

		// creates a new goto instruction that jumps to {@code to}.
		newSucc.appendInst(new Instruction.Goto(to, "critialEdgeGoto"));

		// set the CriticalEdgeSplit flag
		newSucc.setBlockFlags(BasicBlock.BlockFlag.CriticalEdgeSplit);

		int index = from.getSuccs().indexOf(to);
		Util.assertion(  index >= 0);

		from.getSuccs().set(index, newSucc);

		index = to.getPreds().indexOf(from);
		Util.assertion( index >= 0);

		to.getPreds().set(index, newSucc);
		newSucc.addPred(from);

		Iterator<BasicBlock> itr = to.getPreds().iterator();
		while (itr.hasNext())
		{
			if (itr.next() == from)
			{
				itr.remove();
				newSucc.addPred(from);
			}
		}

		return newSucc;
	}
}
*/