package hir; 

import hir.Operand.RHS;
import hir.Operand.RegisterOperand;
import hir.RegisterFactory.Register;

import java.util.ArrayList;
import java.util.List;

import symbol.Symbol.VarSymbol;
import type.Type;
import utils.Context;
import utils.Log;
import ast.ASTVisitor;
import ast.Tree;
import ast.Tree.Block;
import ast.Tree.MethodDef;
import ast.Tree.TopLevel;
import ast.Tree.VarDef;

/** 
 * This class just for converting abstract syntax tree into SSA-based
 * Intermediate Representation. At firstly, it constructs control flow graph
 * over AST. Afterwards, filling quad instruction into basic block over flow 
 * graph. 
 * 
 * In this compiler, all of source language constitute are converted into SSA-based
 * IR. So that entire optimizations rather no concerning about SSA are associated.
 * On the other hand, this compiler is SSA-centeric.
 * 
 * The method this class taken is derived from Matthias Braun(matthias.braun@kit.edu)' 
 * literature,Simple and Efficient Construction of Static Single Assignment Form.
 * The approach proposed by Braun is an efficient for directly constructing SSA from 
 * abstract syntax tree or bytecode.
 * 
 * @author Jianping Zeng <z1215jping@hotmail.com>
 * @version 2016年2月3日 下午12:08:05 
 */
public class ASTToQuad extends ASTVisitor
{
	private final static Context.Key ASTToQuadKey = new Context.Key();
	private Log log;
	private Context context;
	private List<Variable> vars;
	private List<Method> methods;
	private RegisterFactory rf;
	
	public ASTToQuad(Context context)
    {
		this.context = context;
		context.put(ASTToQuadKey, this);
		this.log = Log.instance(context);
		this.vars = new ArrayList<Variable>();
		this.methods = new ArrayList<>();
    }
	
	/**
	 * A traslatation method for converting from abstract syntax tree into IR
	 * in SSA form.
	 * @return HIR	An instance of IR in SSA form representing single compilation file. 
	 */
	public HIR translate(TopLevel tree)
	{
		tree.accept(this);
		return HIR.instance(context, vars, methods);
	}
	
	/**
	 * A programmatic interface for compiling an single method declaration.
	 * @param tree
	 * @return
	 */
	public HIR traslate(MethodDef tree)
	{
		return null;
	}
	

	/**
	 * Appends a quad instruction into current basic block.
	 * @param inst	The quad to be appended.
	 */
	private void appendInst(Quad inst)
	{
		currentBlock.addQuad(inst);
	}
	
	private int nerrs = 0;
	
	/**
	 * Checks constant for string variable and report if it is a string that 
	 * is too large.
	 * @param pos	The position to error reporting.
	 * @param constValue	The constant value of string.
	 */
	private void checkStringConstant(int pos, Object constValue)
	{
		if (nerrs != 0 || constValue == null || !(constValue instanceof String)
				|| ((String)constValue).length() > ControlFlowGraph.MAX_STRING_LENGTH)
		{
			log.error(pos, "too.large.string");
			nerrs++;
		}
	}
	/** The result of expression. */
	private RHS exprResult = null;
	/**
	 * Generates IR for expression.
	 * @param expr	
	 * @param type
	 */
	private RHS genExpr(Tree expr)
	{
		expr.accept(this);
		return exprResult;
	}	
	/**
	 * Translating the top level tree that the compilation unit.
	 * All of definitions will be handled rather than import statement
	 * and global variable declaration.  
	 */
	@Override
	public void visitTopLevel(TopLevel tree)
	{	
	    for (Tree t : tree.defs)
	    {
	    	// firstly, donn't handle import clause and global variable definition
	    	if (t.tag != Tree.IMPORT && t.tag != Tree.VARDEF)
	    		t.accept(this);
	    }
	}

	
	/** Saved current cfg of transforming method. */
	private ControlFlowGraph currentCFG = null;
	private BasicBlock currentBlock = null;
	/** The id for instruction of cfg.*/
	private int instID = 0;
	/**
	 * Translates method definition into intermediate representation(IR).
	 */
	@Override
	public void visitMethodDef(MethodDef tree)
	{
		if (tree.body != null)
		{
    		// creating a new register factory with every method.
    		this.rf = new RegisterFactory(context); 
    		Method m = new Method(tree);		
    		currentCFG = new ControlFlowGraph(m);
    		m.cfg = currentCFG;
    		this.methods.add(m);
    		this.instID = 0;
    		// sets the current block with entry of a cfg.
    		this.currentBlock = currentCFG.createBasicBlock();
    		
    		currentCFG.entry().addSuccessor(currentBlock);
    		currentBlock.addPredecessor(currentCFG.entry());
    		// places actual parameter onto entry block
    		for (Tree t : tree.params)
    		{
    			Register dest = rf.makeReg(((VarDef)t).sym);
    			// a IR placeholder for formal parameter at current cfg
    			appendInst(Quad.Assign.create(this.instID++, dest, null, t.type));
    		}
    		// translate method body
    		tree.body.accept(this);
    		
    		// appends exit block into the successor list of current block.
    		currentBlock.addSuccessor(currentCFG.exit());
    		// appends current block into the predecessor list of exit block.
    		currentCFG.exit().addPredecessor(currentBlock);
		}
	}

	@Override
	public void visitVarDef(VarDef tree)
	{
		VarSymbol v = tree.sym;
		// create a new local register for variable definition
		Register newLocal = this.rf.makeReg(v);
		RHS src = null;
		if (tree.init != null)
		{
			checkStringConstant(tree.init.pos, v.constValue);
			src = genExpr(tree.init);
		}
		currentBlock.addQuad(Quad.Assign.create(instID++, newLocal, src, v.type));
	}	
	/**
	 * Translates program block surrounding with a pair of braces.
	 */
	@Override
	public void visitBlock(Block tree)
	{
		tree.stats
	}
}
