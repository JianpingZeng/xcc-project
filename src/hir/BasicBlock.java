package hir;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Represents a basic block in the quad intermediate representation. Basic
 * blocks are single-entry regions, but not necessarily single-exit regions. Due
 * to the fact that control flow may exit a basic block early due to runtime
 * exception.
 * 
 * Each basic block contains a serial of quads, a list of predecessors, a list
 * of successors. It also has an id id that is unique within its control
 * flow graph.
 * 
 * Note that you should never create directly a basic block using the
 * constructor of {@link BasicBlock}, you should create it via a
 * {@link ControlFlowGraph} so that id id is unique.
 * 
 * @author Jianping Zeng < z121jping@hotmail.com >
 * @see Quad
 * @version 1.0
 *
 */
public class BasicBlock {

	/** Unique id id for this basic block. */
	private int idNumber;

	/** A list of quads. */
	private final List<Quad> instructions;

	/** A list of predecessors. */
	private final List<BasicBlock> predecessors;

	/** A list of successors. */
	private final List<BasicBlock> successors;

	/** A private constructor for entry node */
	private BasicBlock() {

		this.idNumber = 0;
		this.instructions = null;
		this.predecessors = null;
		this.successors = new java.util.ArrayList<>();
	}

	/** A private constructor for exit node */
	private BasicBlock(int numOfExits) {

		this.idNumber = 1;
		this.instructions = null;
		this.predecessors = new java.util.ArrayList<>(numOfExits);
		this.successors = null;
	}

	/** Private constructor for internal nodes. */
	private BasicBlock(int id, int numOfPredecessors, int numOfSuccessors,
			int numOfInstructions) {
		this.idNumber = id;
		this.predecessors = new java.util.ArrayList<BasicBlock>(
				numOfPredecessors);
		this.successors = new java.util.ArrayList<BasicBlock>(numOfSuccessors);
		this.instructions = new java.util.ArrayList<Quad>(numOfInstructions);
	}

	/** Creates new entry node. Only to be called by ControlFlowGraph. */
	static BasicBlock createStartNode() {
		return new BasicBlock();
	}

	/** Creates a new basic node for exit block. */
	static BasicBlock createEndNode(int numberOfPredecessor) {

		return new BasicBlock(numberOfPredecessor);
	}

	/** Creates a new basic node for exit block without numbers of predecessors. */
	static BasicBlock createEndNode() {

		return new BasicBlock();
	}
	
	/**
	 * Create new internal basic block. Only to be called by ControlFlowGraph.
	 */
	static BasicBlock createBasicBlock(int id, int numOfPredecessors,
			int numOfSuccessors, int numOfInstructions) {

		return new BasicBlock(id, numOfPredecessors, numOfSuccessors,
				numOfInstructions);
	}
	
	/**
	 * Create new internal basic block. Only to be called by ControlFlowGraph.
	 */
	static BasicBlock createBasicBlock(int id) {

		return new BasicBlock(id);
	}
	

	/**
	 * Returns true if this is the entry basic block.
	 * 
	 * @return true if this is the entry basic block.
	 */
	public boolean isEntry() {
		return predecessors == null;
	}

	/**
	 * Returns true if this is the exit basic block.
	 * 
	 * @return true if this is the exit basic block.
	 */
	public boolean isExit() {
		return successors == null;
	}

	/**
	 * Returns iterator over Quads in this basic block in forward order.
	 * 
	 * @return Returns iterator over Quads in this basic block in forward order.
	 */
	public ListIterator<Quad> iterator() {
		if (instructions == null)
			return Collections.<Quad> emptyList().listIterator();
		else
			return instructions.listIterator();
	}

	/**
	 * Returns iterator over Quads in this basic block in forward order.
	 * 
	 * @return Returns iterator over Quads in this basic block in forward order.
	 */
	public BackwardIterator<Quad> backwardIterator() {
		if (instructions == null)
			return new BackwardIterator<Quad>(Collections.<Quad> emptyList()
					.listIterator());
		else
			return new BackwardIterator<Quad>(instructions.listIterator());
	}

	/**
	 * Visit all of the quads in this basic block in forward order with the
	 * given quad visitor.
	 * 
	 * @see QuadVisitor
	 * @param qv
	 *            QuadVisitor to visit the quads with.
	 */
	public void visitQuads(QuadVisitor qv) {
		for (Quad q : instructions) {
			q.accept(qv);
		}
	}

	/**
	 * Visit all of the quads in this basic block in backward order with the
	 * given quad visitor.
	 * 
	 * @see QuadVisitor
	 * @param qv
	 *            QuadVisitor to visit the quads with.
	 */
	public void backwardVisitQuads(QuadVisitor qv) {
		for (Iterator<Quad> i = backwardIterator(); i.hasNext();) {
			Quad q = i.next();
			q.accept(qv);
		}
	}

	/**
	 * Returns the id of quads in this basic block.
	 * 
	 * @return the id of quads in this basic block.
	 */
	public int size() {
		if (instructions == null)
			return 0; // entry or exit block
		return instructions.size();
	}

	public Quad getQuad(int i) {
		return instructions.get(i);
	}

	public Quad getLastQuad() {
		if (size() == 0)
			return null;
		return instructions.get(instructions.size() - 1);
	}

	public int getQuadIndex(Quad q) {
		return instructions == null ? -1 : instructions.indexOf(q);
	}

	public Quad removeQuad(int i) {
		return instructions.remove(i);
	}

	public boolean removeQuad(Quad q) {
		return instructions.remove(q);
	}

	public void removeAllQuads() {
		instructions.clear();
	}

	/**
	 * Add a quad to this basic block at the given location. Cannot add quads to
	 * the entry or exit basic blocks.
	 *
	 * @param index
	 *            the index to add the quad
	 * @param q
	 *            quad to add
	 */
	public void addQuad(Quad q) {
		assert (instructions == null) : "Cannot add instructions to entry/exit basic block";
		instructions.add(q);
	}

	/**
	 * Append a quad to the end of this basic block. Cannot add quads to the
	 * entry or exit basic blocks.
	 * 
	 * @param q
	 *            quad to add
	 */
	public void appendQuad(Quad q) {
		assert (instructions == null) : "Cannot add instructions to entry/exit basic block";
		instructions.add(q);
	}

	/**
	 * Replace the quad at position pos.
	 * */
	public void replaceQuad(int pos, Quad q) {
		assert (instructions == null) : "Cannot add instructions to entry/exit basic block";
		instructions.set(pos, q);
	}

	/**
	 * Add a predecessor basic block to this basic block. Cannot add
	 * predecessors to the entry basic block.
	 * 
	 * @param b
	 *            basic block to add as a predecessor
	 */
	public void addPredecessor(BasicBlock b) {
		assert (predecessors == null) : "Cannot add predecessor to entry basic block";
		predecessors.add(b);
	}

	/**
	 * Add a successor basic block to this basic block. Cannot add successors to
	 * the exit basic block.
	 * 
	 * @param b
	 *            basic block to add as a successor
	 */
	public void addSuccessor(BasicBlock b) {
		assert successors == null : "Cannot add successor to exit basic block";
		successors.add(b);
	}

	public boolean removePredecessor(BasicBlock bb) {
		assert predecessors == null : "Cannot remove predecessor from entry basic block";
		return predecessors.remove(bb);
	}

	public void removePredecessor(int i) {
		assert predecessors == null : "Cannot remove predecessor from entry basic block";
		predecessors.remove(i);
	}

	public boolean removePredecessors(Collection<BasicBlock> bb) {
		assert predecessors == null : "Cannot remove predecessor from entry basic block";
		return predecessors.removeAll(bb);
	}

	public boolean removeSuccessor(BasicBlock bb) {
		assert successors == null : "Cannot remove successor from exit basic block";
		return successors.remove(bb);
	}

	public void removeSuccessor(int i) {
		assert successors == null : "Cannot remove successor from exit basic block";
		successors.remove(i);
	}

	public void removeAllPredecessors() {
		assert predecessors == null : "Cannot remove predecessors from entry basic block";
		predecessors.clear();
	}

	public void removeAllSuccessors() {
		assert successors == null : "Cannot remove successors from exit basic block";
		successors.clear();
	}

	public int getNumberOfSuccessors() {
		return successors == null ? 0 : successors.size();
	}

	public int getNumberOfPredecessors() {
		return predecessors == null ? 0 : predecessors.size();
	}

	/**
	 * Returns the fallthrough successor to this basic block, if it exists. If
	 * there is none, returns null.
	 * 
	 * @return the fallthrough successor, or null if there is none.
	 */
	public BasicBlock getFallthroughSuccessor() {
		return successors == null ? null : successors.get(0);
	}

	/**
	 * Returns the fallthrough predecessor to this basic block, if it exists. If
	 * there is none, returns null.
	 * 
	 * @return the fallthrough predecessor, or null if there is none.
	 */
	public BasicBlock getFallthroughPredecessor() {
		return predecessors == null ? null : predecessors.get(0);
	}

	/**
	 * Returns a list of the successors of this basic block.
	 * 
	 * @return a list of the successors of this basic block.
	 */
	public List<BasicBlock> getSuccessors() {
		return successors == null ? Collections.<BasicBlock> emptyList()
				: successors;
	}

	/**
	 * Returns an list of the predecessors of this basic block.
	 * 
	 * @return an iterator of the predecessors of this basic block.
	 */
	public List<BasicBlock> getPredecessors() {
		return predecessors == null ? Collections.<BasicBlock> emptyList()
				: predecessors;
	}

	public int getID() {
		return this.idNumber;
	}
}
