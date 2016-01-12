package hir;

import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

public class ControlFlowGraph {

	/** The function that this control flow graph represents. */
	private DefinedFunction attachedFunction;
	
	/** The entry basic block of this control flow graph.*/
	private BasicBlock endNode;
	/** The exit basic block of this control flow graph.*/
	private BasicBlock startNode;
	
    /** Current number of basic blocks, used to generate unique id's. */
    private int bb_counter;
    /** Current number of quads, used to generate unique id's. */
    private int quad_counter;
    
    /**
     * Constructor that constructs an control flow graph.
     *  
     * @param method	the method that this graph represents.
     * @param numOfExits	the numbers of predecessor of end Tree.
     */
    public ControlFlowGraph(DefinedFunction method, int numOfExits) {
    
    	this.attachedFunction = method;
    	this.startNode = BasicBlock.createStartNode();
    	this.endNode = BasicBlock.createEndNode(numOfExits);
    	// number of basic block begin with one.
    	this.bb_counter = 1;
    	// number of quad begin with zero.
    	this.quad_counter = 0;
    }
    
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
    public DefinedFunction getMethod() {return this.attachedFunction; }
    
    /**
     * Create a new basic block in this control flow graph.  The new basic block
     * is given a new, unique id number.
     * 
     * @param numOfPredecessors  number of predecessor basic blocks that this
                                 basic block is expected to have.
     * @param numOfSuccessors  number of successor basic blocks that this
                               basic block is expected to have.
     * @param numOfInstructions  number of instructions that this basic block
                                 is expected to have.
     * @return  the newly created basic block.
     */
    public BasicBlock createBasicBlock(int numOfPredecessors, 
    		int numOfSuccessors, int numOfInstructions) {
        
    	return BasicBlock.createBasicBlock
    			(++bb_counter, numOfPredecessors, numOfSuccessors, numOfInstructions);
    }
    
    /** Use with care after renumbering basic blocks. */
    void updateBBcounter(int value) { bb_counter = value-1; }
    
    /**
     * Returns a maximum on the number of basic blocks in this control flow graph.
     * 
     * @return  a maximum on the number of basic blocks in this control flow graph.
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

    /** Returns a new id number for a quad. */
    public int getNewQuadID() { return ++quad_counter; }
    
    /** Returns the maximum id number for a quad. */
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
     * Returns a list of basic blocks in reverse post order, starting at the given basic block.
     * 
     * @param start_bb  basic block to start from.
     * @return  a list of basic blocks in reverse post order, starting at the given basic block.
     */
    public List<BasicBlock> reversePostOrder(BasicBlock start_bb) {
    	
        java.util.LinkedList<BasicBlock> result = new java.util.LinkedList<>();
        boolean[] visited = new boolean[bb_counter+1];
        reversePostOrder_helper(start_bb, visited, result, true);
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

	public void visitBasicBlocks(BasicBlockVisitor bbv) {
		
		List<BasicBlock> bblist = reversePostOrder(startNode);
		for (BasicBlock bb : bblist)
		{
			bbv.visitBasicBlock(bb);
		}
	}
	
	/**
	 * Computes a control flow graph according to a list of quad.
	 *  
	 * @param quads	A list of quads. 
	 * @return	A corresponding CFG returned.
	 */
	public static ControlFlowGraph computeCFG(List<Quad> quads) {
		
		return null;
	}
}
