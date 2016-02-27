package hir;

import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

public class ControlFlowGraph {

	public static final int MAX_STRING_LENGTH = 65535;
	
	/** The function that this control flow graph represents. */
	private Method attachedMethod;
	
	/** The entry basic block of this control flow graph.*/
	private BasicBlock startNode;
	/** The exit basic block of this control flow graph.*/
	private BasicBlock endNode;
	
    /** Current id of basic blocks, used to generate unique id's. */
    private int bb_counter;
    /** Current id of quads, used to generate unique id's. */
    private int quad_counter;
    
	/**
	 * The id for instruction of cfg.
	 */
	private int instID = 0;
    
    /**
     * Constructor that constructs an control flow graph.
     *  
     * @param method	the method that this graph represents.
     */
    public ControlFlowGraph(Method method) {
    
    	this.attachedMethod = method;
    	this.startNode = BasicBlock.createStartNode();
    	this.endNode = BasicBlock.createEndNode();
    	// id of basic block begin with one.
    	this.bb_counter = 1;
    	// id of quad begin with zero.
    	this.quad_counter = 0;
    }
    /** Get the new id of current instruction.*/
    public int getInstID(){return this.instID++;}
    
    /**
     * Returns the entry node.
     * 
     * @return  the entry node.
     */
    public BasicBlock entry() { return startNode; }
    
    /**
     * Returns the exit node.
     * 
     * @return  the exit node.
     */
    public BasicBlock exit() { return endNode; }
    
    /**
     * Returns the method this graph represents.
     * 
     * @return	the attached function.
     */
    public Method getMethod() {return this.attachedMethod; }
    
    /**
     * Create a new basic block in this control flow graph.  The new basic block
     * is given a new, unique id id.
     * 
     * @param numOfPredecessors  id of predecessor basic blocks that this
                                 basic block is expected to have.
     * @param numOfSuccessors  id of successor basic blocks that this
                               basic block is expected to have.
     * @param numOfInstructions  id of instructions that this basic block
                                 is expected to have.
     * @return  the newly created basic block.
     */
    public BasicBlock createBasicBlock(int numOfPredecessors, 
    		int numOfSuccessors, int numOfInstructions) {
        
    	return BasicBlock.createBasicBlock
    			(++bb_counter, numOfPredecessors, numOfSuccessors, numOfInstructions);
    }
    
    /**
     * Create a new basic block in this control flow graph.  The new basic block
     * is given a new, unique id id.
     * 
     * @return  the newly created basic block.
     */
    public BasicBlock createBasicBlock() {
        
    	return BasicBlock.createBasicBlock(++bb_counter);
    }
    
    /** Use with care after renumbering basic blocks. */
    void updateBBcounter(int value) { bb_counter = value-1; }
    
    /**
     * Returns a maximum on the id of basic blocks in this control flow graph.
     * 
     * @return  a maximum on the id of basic blocks in this control flow graph.
     */
    public int getNumberOfBasicBlocks() { return bb_counter+1; }
    
    public int getNumberOfQuads() {
        int total = 0;
        ListIterator<BasicBlock> i = reversePostOrderIterator();
        while (i.hasNext()) {
            BasicBlock bb = i.next();
            total += bb.size();
        }
        return total;
    }

    /** Returns a new id id for a quad. */
    public int getNewQuadID() { return ++quad_counter; }
    
    /** Returns the maximum id id for a quad. */
    public int getMaxQuadID() { return quad_counter; }
    
    
    /**
     * Returns an iteration of the basic blocks in this graph in reverse post order.
     * 
     * @return  an iteration of the basic blocks in this graph in reverse post order.
     */
    public ListIterator<BasicBlock> reversePostOrderIterator() {
        return reversePostOrderIterator(startNode);
    }
    
    /**
     * Returns an iteration of the basic blocks in this graph reachable from the given
     * basic block in reverse post order, starting from the given basic block.
     * 
     * @param start_bb  basic block to start reverse post order from.
     * @return  an iteration of the basic blocks in this graph reachable from the given basic block in reverse post order.
     */
    public ListIterator<BasicBlock> reversePostOrderIterator(BasicBlock start_bb) {
        return reversePostOrder(start_bb).listIterator();
    }
    
    /**
     * Returns a list of basic blocks in reverse post order, starting at the
     * given basic block.
     * 
     * @param start_bb  basic block to start from.
     * @return  a list of basic blocks in reverse post order, starting at the
     * given basic block.
     */
    public List<BasicBlock> reversePostOrder(BasicBlock start_bb) {
    	
        java.util.LinkedList<BasicBlock> result = new java.util.LinkedList<>();
        boolean[] visited = new boolean[bb_counter+1];
        reversePostOrder_helper(start_bb, visited, result, true);
        return Collections.unmodifiableList(result);
    }

	/**
	 * Returns a list of basic blocks in reverse post order, starting at the
	 * entry block.
	 *
	 * @return  a list of basic blocks in reverse post order, starting at
	 * the entry.
	 */
	public List<BasicBlock> reversePostOrder() {

		java.util.LinkedList<BasicBlock> result = new java.util.LinkedList<>();
		boolean[] visited = new boolean[bb_counter+1];
		reversePostOrder_helper(startNode, visited, result, true);
		return Collections.unmodifiableList(result);
	}

    /**
     * Helper function to compute reverse post order.
     *
     * @param b		the start node.
     * @param visited	a array that records visiting flag.
     * @param result	reverse post order of all of basic node in this graph.
     * @param direction		whether forward or backward.
     */

    private void reversePostOrder_helper(BasicBlock b, boolean[] visited, java.util.LinkedList<BasicBlock> result, boolean direction) {
        if (visited[b.getID()]) return;
        visited[b.getID()] = true;
        List<BasicBlock> bbs = direction ? b.getSuccessors() : b.getPredecessors();
        for (BasicBlock b2 : bbs) {
            reversePostOrder_helper(b2, visited, result, direction);
        }
        result.addFirst(b);
    }

    /**
     * Visits all of basic block in order that reverse post order with specified
     * {@code BasicBlockVisitor} bbv.
     * @param bbv   The instance of BasicBlockVisitor.
     */
	public void visitBasicBlocks(BasicBlockVisitor bbv) {
		
		List<BasicBlock> bblist = reversePostOrder(startNode);
		for (BasicBlock bb : bblist)
		{
			bbv.visitBasicBlock(bb);
		}
	}
}
