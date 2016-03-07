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
public class BasicBlock implements Iterable<Instruction>
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
	private final LinkedList<Instruction> instructions;

	/**
	 * A set of predecessors.
	 */
	private final LinkedList<BasicBlock> predecessors;

	/**
	 * A  of successors.
	 */
	private final LinkedList<BasicBlock> successors;

	private ControlFlowGraph cfg;

	/**
	 * The name of this block.
	 */
	public String bbName;

	/**
	 * A private constructor for entry node
	 */
	private BasicBlock(int id, LinkedList<BasicBlock> pres,
			String bbName, ControlFlowGraph cfg)
	{
		this(id, pres, null, bbName, cfg);
	}

	private BasicBlock(int id, LinkedList<BasicBlock> pres, LinkedList<BasicBlock> succs,
			String bbName, ControlFlowGraph cfg)
	{
		this.idNumber = id;
		this.instructions = new LinkedList<>();
		this.predecessors = pres;
		this.successors = succs;
		this.bbName = bbName;
		this.cfg = cfg;
	}

	/**
	 * Creates new entry node. Only to be called by ControlFlowGraph.
	 */
	static BasicBlock createStartNode(int id, String name, ControlFlowGraph cfg)
	{
		return new BasicBlock(id, null, name, cfg);
	}

	/**
	 * Creates a new basic node for exit block without numbers of predecessors.
	 */
	static BasicBlock createEndNode(int id, String bbName, ControlFlowGraph cfg)
	{
		return new BasicBlock(id, new LinkedList<>(), null, bbName, cfg);
	}

	/**
	 * Create new internal basic block. Only to be called by ControlFlowGraph.
	 */
	static BasicBlock createBasicBlock(int id, String bbName, ControlFlowGraph cfg)
	{

		return new BasicBlock(id, new LinkedList<>(), new LinkedList<>(), bbName, cfg);
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
	 * Gets the index into instructions list. Return -1 if instruction no contains
	 * specified inst. Otherwise, return the index of first occurrence.
	 * @param inst
	 * @return
	 */
	public int indexOf(Instruction inst)
	{
		if (inst == null) return -1;
		return instructions.indexOf(inst);
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

	public Instruction getInst(int i)
	{
		return instructions.get(i);
	}

	public boolean removeInst(Instruction q)
	{
		return instructions.remove(q);
	}

	public void clear()
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
	public void addInst(Instruction q, int index)
	{
		assert (q != null) : "Cannot add null instruction to block";
		assert (index >= 0 && index < instructions.size()):
				"The index into insertion of gieven inst is bound out.";

		instructions.add(index, q);
	}

	/**
	 * Append a quad to the end of this basic block. Cannot add quads to the
	 * entry or exit basic blocks.
	 *
	 * @param q quad to add
	 */
	public void appendInst(Instruction q)
	{
		assert (q != null) : "Cannot add null instructions to block";
		instructions.add(q);
	}

	/**
	 * Add a predecessor basic block to this basic block. Cannot add
	 * predecessors to the entry basic block.
	 *
	 * @param b basic block to add as a predecessor
	 */
	public boolean addPred(BasicBlock b)
	{
		assert (b != null) : "Cannot add null block into predecessor list";
		if (predecessors.contains(b))
			return false;
		return predecessors.add(b);
	}

	/**
	 * Add a successor basic block to this basic block. Cannot add successors to
	 * the exit basic block.
	 *
	 * @param b basic block to add as a successor
	 */
	public boolean addSucc(BasicBlock b)
	{
		assert (b != null) : "Cannot add null block into successor list";
		if (successors.contains(b))
			return false;
		return successors.add(b);
	}

	public int getNumOfSuccs()
	{
		return successors == null ? 0 : successors.size();
	}

	public int getNumOfPreds()
	{
		return predecessors == null ? 0 : predecessors.size();
	}

	/**
	 * Returns a list of the successors of this basic block.
	 *
	 * @return a list of the successors of this basic block.
	 */
	public List<BasicBlock> getSuccs()
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
	public List<BasicBlock> getPreds()
	{
		return predecessors == null ?
				Collections.<BasicBlock>emptyList() :
				predecessors;
	}

	public int getID()
	{
		return this.idNumber;
	}

	public ControlFlowGraph getCFG() {return cfg;}

	public void setCFG(ControlFlowGraph cfg) {this.cfg = cfg;}

	public Instruction firstInst()
	{
		return instructions.get(0);
	}

	public  Instruction lastInst()
	{
		if (instructions.isEmpty())
			return null;
		return instructions.get(instructions.size() - 1);
	}

	/**
	 * Inserts a instruction into the position after the first inst of instructions
	 * list.
	 * @param inst
	 */
	public void insertAfterFirst(Instruction inst)
	{
		assert inst != null;

		if (instructions.isEmpty())
			instructions.addFirst(inst);
		else
		{
			Instruction first = instructions.getFirst();
			instructions.add(instructions.indexOf(first)+1, inst);
		}
	}
}
