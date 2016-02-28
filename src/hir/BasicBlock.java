package hir;

import java.util.*;

/**
 * Represents a basic block in the quad intermediate representation. Basic
 * blocks are single-entry regions, but not necessarily single-exit regions. Due
 * to the fact that control flow may exit a basic block early due to runtime
 * exception.
 * <p>
 * Each basic block contains a serial of quads, a list of predecessors, a list
 * of successors. It also has an id id that is unique within its control
 * flow graph.
 * <p>
 * Note that you should never create directly a basic block using the
 * constructor of {@link BasicBlock}, you should create it via a
 * {@link ControlFlowGraph} so that id id is unique.
 *
 * @author Jianping Zeng < z121jping@hotmail.com >
 * @version 1.0
 * @see Instruction
 */
public class BasicBlock
{

	/**
	 * Unique id id for this basic block.
	 */
	private int idNumber;

	/**
	 * The numbering when performing linear scanning.
	 */
	private int linearScanNumber;
	/**
	 * The numbering when performing depth first traveling.
	 */
	private int depthFirstNumber;

	/**
	 * A list of quads.
	 */
	private final List<Instruction> instructions;

	/**
	 * A list of predecessors.
	 */
	private final List<BasicBlock> predecessors;

	/**
	 * A list of successors.
	 */
	private final List<BasicBlock> successors;

	/**
	 * The name of this block.
	 */
	public String bbName;

	/**
	 * A private constructor for entry node
	 */
	private BasicBlock(int id, List<BasicBlock> pres, String bbName)
	{
		this(id, pres, null, bbName);
	}

	private BasicBlock(int id, List<BasicBlock> pres, List<BasicBlock> succs,
			String bbName)
	{
		this.idNumber = id;
		this.instructions = new LinkedList<>();
		this.predecessors = pres;
		this.successors = succs;
		this.bbName = bbName;
	}

	/**
	 * Creates new entry node. Only to be called by ControlFlowGraph.
	 */
	static BasicBlock createStartNode(int id, String name)
	{
		return new BasicBlock(id, null, name);
	}

	/**
	 * Creates a new basic node for exit block without numbers of predecessors.
	 */
	static BasicBlock createEndNode(int id, String bbName)
	{
		return new BasicBlock(id, new ArrayList<>(), null, bbName);
	}

	/**
	 * Create new internal basic block. Only to be called by ControlFlowGraph.
	 */
	static BasicBlock createBasicBlock(int id, String bbName)
	{

		return new BasicBlock(id, new ArrayList<>(), new ArrayList<>(), bbName);
	}

	/**
	 * Returns true if this is the entry basic block.
	 *
	 * @return true if this is the entry basic block.
	 */
	public boolean isEntry()
	{
		return predecessors == null;
	}

	/**
	 * Returns true if this is the exit basic block.
	 *
	 * @return true if this is the exit basic block.
	 */
	public boolean isExit()
	{
		return successors == null;
	}

	/**
	 * Returns iterator over Quads in this basic block in forward order.
	 *
	 * @return Returns iterator over Quads in this basic block in forward order.
	 */
	public ListIterator<Instruction> iterator()
	{
		if (instructions == null)
			return Collections.<Instruction>emptyList().listIterator();
		else
			return instructions.listIterator();
	}

	/**
	 * Returns iterator over Quads in this basic block in forward order.
	 *
	 * @return Returns iterator over Quads in this basic block in forward order.
	 */
	public BackwardIterator<Instruction> backwardIterator()
	{
		if (instructions == null)
			return new BackwardIterator<Instruction>(
					Collections.<Instruction>emptyList().listIterator());
		else
			return new BackwardIterator<Instruction>(
					instructions.listIterator());
	}

	/**
	 * Visit all of the quads in this basic block in forward order with the
	 * given quad visitor.
	 *
	 * @param qv InstructionVisitor to visit the quads with.
	 * @see InstructionVisitor
	 */
	public void visitQuads(InstructionVisitor qv)
	{
		for (Instruction q : instructions)
		{
			q.accept(qv);
		}
	}

	/**
	 * Visit all of the quads in this basic block in backward order with the
	 * given quad visitor.
	 *
	 * @param qv InstructionVisitor to visit the quads with.
	 * @see InstructionVisitor
	 */
	public void backwardVisitQuads(InstructionVisitor qv)
	{
		for (Iterator<Instruction> i = backwardIterator(); i.hasNext(); )
		{
			Instruction q = i.next();
			q.accept(qv);
		}
	}

	/**
	 * Returns the id of quads in this basic block.
	 *
	 * @return the id of quads in this basic block.
	 */
	public int size()
	{
		if (instructions == null)
			return 0; // entry or exit block
		return instructions.size();
	}

	public Instruction getQuad(int i)
	{
		return instructions.get(i);
	}

	public Instruction getLastQuad()
	{
		if (size() == 0)
			return null;
		return instructions.get(instructions.size() - 1);
	}

	public int getQuadIndex(Instruction q)
	{
		return instructions == null ? -1 : instructions.indexOf(q);
	}

	public Instruction removeQuad(int i)
	{
		return instructions.remove(i);
	}

	public boolean removeQuad(Instruction q)
	{
		return instructions.remove(q);
	}

	public void removeAllQuads()
	{
		instructions.clear();
	}

	/**
	 * Add a quad to this basic block at the given location. Cannot add quads to
	 * the entry or exit basic blocks.
	 *
	 * @param index the index to add the quad
	 * @param q     quad to add
	 */
	public void addQuad(Instruction q, int index)
	{
		assert (instructions
				!= null) : "Cannot add instructions to entry/exit basic block";
		instructions.add(index, q);
	}

	/**
	 * Append a quad to the end of this basic block. Cannot add quads to the
	 * entry or exit basic blocks.
	 *
	 * @param q quad to add
	 */
	public void appendQuad(Instruction q)
	{
		assert (instructions
				!= null) : "Cannot add instructions to entry/exit basic block";
		instructions.add(q);
	}

	/**
	 * Replace the quad at position pos.
	 */
	public void replaceQuad(int pos, Instruction q)
	{
		assert (instructions
				!= null) : "Cannot add instructions to entry/exit basic block";
		instructions.set(pos, q);
	}

	/**
	 * Add a predecessor basic block to this basic block. Cannot add
	 * predecessors to the entry basic block.
	 *
	 * @param b basic block to add as a predecessor
	 */
	public void addPredecessor(BasicBlock b)
	{
		assert (predecessors
				!= null) : "Cannot add predecessor to entry basic block";
		predecessors.add(b);
	}

	/**
	 * Add a successor basic block to this basic block. Cannot add successors to
	 * the exit basic block.
	 *
	 * @param b basic block to add as a successor
	 */
	public void addSuccessor(BasicBlock b)
	{
		assert successors != null : "Cannot add successor to exit basic block";
		successors.add(b);
	}

	public boolean removePredecessor(BasicBlock bb)
	{
		assert predecessors
				!= null : "Cannot remove predecessor from entry basic block";
		return predecessors.remove(bb);
	}

	public void removePredecessor(int i)
	{
		assert predecessors
				!= null : "Cannot remove predecessor from entry basic block";
		predecessors.remove(i);
	}

	public boolean removePredecessors(Collection<BasicBlock> bb)
	{
		assert predecessors
				!= null : "Cannot remove predecessor from entry basic block";
		return predecessors.removeAll(bb);
	}

	public boolean removeSuccessor(BasicBlock bb)
	{
		assert successors
				!= null : "Cannot remove successor from exit basic block";
		return successors.remove(bb);
	}

	public void removeSuccessor(int i)
	{
		assert successors
				!= null : "Cannot remove successor from exit basic block";
		successors.remove(i);
	}

	public void removeAllPredecessors()
	{
		assert predecessors
				!= null : "Cannot remove predecessors from entry basic block";
		predecessors.clear();
	}

	public void removeAllSuccessors()
	{
		assert successors
				!= null : "Cannot remove successors from exit basic block";
		successors.clear();
	}

	public int getNumberOfSuccessors()
	{
		return successors == null ? 0 : successors.size();
	}

	public int getNumberOfPredecessors()
	{
		return predecessors == null ? 0 : predecessors.size();
	}

	/**
	 * Returns the fallthrough successor to this basic block, if it exists. If
	 * there is none, returns null.
	 *
	 * @return the fallthrough successor, or null if there is none.
	 */
	public BasicBlock getFallthroughSuccessor()
	{
		return successors == null ? null : successors.get(0);
	}

	/**
	 * Returns the fallthrough predecessor to this basic block, if it exists. If
	 * there is none, returns null.
	 *
	 * @return the fallthrough predecessor, or null if there is none.
	 */
	public BasicBlock getFallthroughPredecessor()
	{
		return predecessors == null ? null : predecessors.get(0);
	}

	/**
	 * Returns a list of the successors of this basic block.
	 *
	 * @return a list of the successors of this basic block.
	 */
	public List<BasicBlock> getSuccessors()
	{
		return successors == null ?
				Collections.<BasicBlock>emptyList() :
				successors;
	}

	/**
	 * Returns an list of the predecessors of this basic block.
	 *
	 * @return an iterator of the predecessors of this basic block.
	 */
	public List<BasicBlock> getPredecessors()
	{
		return predecessors == null ?
				Collections.<BasicBlock>emptyList() :
				predecessors;
	}

	public int getID()
	{
		return this.idNumber;
	}
}
