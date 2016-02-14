package hir;

import comp.OpCodes;
import hir.Operand.TargetOperand;
import hir.Operand.RegisterOperand;
import hir.Quad.IntIfCmp;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import exception.JumpError;
import exception.SemanticError;
import symbol.Symbol.OperatorSymbol;
import symbol.Symbol.VarSymbol;
import type.Type;
import type.TypeTags;
import utils.Context;
import utils.Log;
import utils.Name;
import ast.ASTVisitor;
import ast.Tree;
import ast.Tree.Ident;
import ast.TreeMaker;
import ast.Tree.Apply;
import ast.Tree.Binary;
import ast.Tree.Block;
import ast.Tree.Break;
import ast.Tree.Case;
import ast.Tree.Conditional;
import ast.Tree.Continue;
import ast.Tree.DoLoop;
import ast.Tree.Exec;
import ast.Tree.ForLoop;
import ast.Tree.Goto;
import ast.Tree.If;
import ast.Tree.Indexed;
import ast.Tree.Labelled;
import ast.Tree.MethodDef;
import ast.Tree.Parens;
import ast.Tree.Return;
import ast.Tree.Skip;
import ast.Tree.Switch;
import ast.Tree.TopLevel;
import ast.Tree.TypeCast;
import ast.Tree.Unary;
import ast.Tree.VarDef;
import ast.Tree.WhileLoop;
import ast.TreeInfo;

/**
 * <p>
 * This class just for converting abstract syntax tree into SSA-based
 * Intermediate Representation. At firstly, it constructs control flow graph
 * over AST. Afterwards, filling quad instruction into basic block over flow
 * graph.</p>
 * <p>
 * In this compiler, all of source language constitute are converted into
 * SSA-based IR. So that entire optimizations rather no concerning about SSA are
 * associated. On the other hand, this compiler is SSA-centeric.
 * </p>
 * <p>
 * The method this class taken is derived from Matthias
 * Braun(matthias.braun@kit.edu)' literature,Simple and Efficient Construction
 * of Static Single Assignment Form. The approach proposed by Braun is an
 * efficient for directly constructing SSA from abstract syntax tree or
 * bytecode.
 * </p>
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
	private TreeMaker maker;
	private Name.Table names;

	public ASTToQuad(Context context)
	{
		this.context = context;
		context.put(ASTToQuadKey, this);
		this.maker = TreeMaker.instance(context);
		this.names = Name.Table.instance(context);
		this.log = Log.instance(context);
		this.vars = new ArrayList<Variable>();
		this.methods = new ArrayList<>();
	}

	/**
	 * A translation method for converting from abstract syntax tree into IR.
	 *
	 * @return HIR An instance of IR in SSA form representing single compilation
	 *         file.
	 */
	public HIR translate(TopLevel tree)
	{
		tree.accept(this);
		return HIR.instance(context, vars, methods);
	}

	/**
	 * A programmatic interface for compiling an single method declaration.
	 *
	 * @param tree
	 * @return
	 */
	public HIR traslate(MethodDef tree)
	{
		return null;
	}

	/**
	 * Appends a quad instruction into current basic block.
	 *
	 * @param inst The quad to be appended.
	 */
	private void appendInst(Quad inst)
	{
		currentBlock.addQuad(inst);
	}

	private int nerrs = 0;

	/**
	 * Checks constant for string variable and report if it is a string that is
	 * too large.
	 *
	 * @param pos The position to error reporting.
	 * @param constValue The constant value of string.
	 */
	private void checkStringConstant(int pos, Object constValue)
	{
		if (nerrs != 0
		        || constValue == null
		        || !(constValue instanceof String)
		        || ((String) constValue).length() > ControlFlowGraph.MAX_STRING_LENGTH)
		{
			log.error(pos, "too.large.string");
			nerrs++;
		}
	}

	/**
	 * The result of expression.
	 */
	private Operand exprResult = null;

	/**
	 * Generates IR for expression.
	 *
	 * @param expr
	 * @param type
	 */
	private Operand genExpr(Tree expr)
	{
		expr.accept(this);
		return exprResult;
	}

	/**
	 * generates IR sequence for expression list.
	 *
	 * @param exprs
	 */
	private void genExprList(List<Tree> exprs)
	{
		if (exprs == null || exprs.isEmpty()) return;
		for (Tree expr : exprs)
			genExpr(expr);
	}

	/**
	 * Generates a jump instruction to be inserted into instructions list of a
	 * basic block.
	 *
	 * @param target The target of jump inst
	 */
	private void genJump(BasicBlock target)
	{
		Quad.Goto goto_ = new Quad.Goto(instID++, target);
		goto_.refs++;
		appendInst(goto_);

		currentBlock.addSuccessor(target);
		target.addPredecessor(currentBlock);
	}

	/**
	 * Links the given block into cfg.
	 *
	 * @param bb The given basic block to be insert into cfg.
	 */
	private void startBasicBlock(BasicBlock bb)
	{
		currentBlock.addSuccessor(bb);
		bb.addPredecessor(currentBlock);
		// updates current block
		currentBlock = bb;
	}

	/**
	 * Not the expr.
	 *
	 * @param expr
	 * @return
	 */
	private Tree Not(Tree expr)
	{
		Binary bin = null;
		Unary unary = null;
		int[] rops = { Tree.NE, Tree.EQ, Tree.GE, Tree.GT, Tree.LE, Tree.LT };
		switch (expr.tag)
		{
			case Tree.OR:
				bin = (Binary) expr;
				bin.tag = Tree.AND;
				bin.lhs = Not(bin.lhs);
				bin.rhs = Not(bin.rhs);
				return bin;
			case Tree.AND:
				bin = (Binary) expr;
				bin.tag = Tree.OR;
				bin.lhs = Not(bin.lhs);
				bin.rhs = Not(bin.rhs);
				return bin;
			case Tree.EQ:
			case Tree.NE:
			case Tree.LT:
			case Tree.LE:
			case Tree.GE:
			case Tree.GT:
				bin = (Binary) expr;
				bin.tag = rops[bin.tag - Tree.EQ];
				return bin;
			case Tree.NOT:
				unary = (Unary) expr;
				return unary.arg;
			default:
				unary = this.maker.Unary(Tree.NOT, expr);
				return constantFold(unary);
		}
	}

	private Tree constantFold(Unary tree)
	{
		if (tree != null)
		{
			switch (tree.arg.tag)
			{
				case Tree.IDENT:
					Tree.Ident ident = (Tree.Ident) tree.arg;
					if (ident.name.toString() == "true")
						return maker.Ident(Name.fromString(names, "false"));
					else if (ident.name.toString() == "false")
						return maker.Ident(Name.fromString(names, "true"));
					else
					{
						log.error(tree.pos, "not.bool.constant.at.condition");
						return tree;
					}
				default:
					log.error(tree.pos, "not.bool.constant.at.condition");
					return tree;
			}
		}
		return tree;
	}

	/**
	 * Translates conditional branch.
	 *
	 * @param expr
	 * @param trueBB the target block when condition is true.
	 * @param falseBB the target block when condition is false.
	 */
	private void translateBranch(Tree expr, BasicBlock trueBB,
	        BasicBlock falseBB)
	{
		BasicBlock remainderBB;
		Binary bin;
		Operand src1, src2;
		IntIfCmp inst;
		IntIfCmp[] insts = { new Quad.IfCmp_EQ(instID++, null, null, null),
		        new Quad.IfCmp_NEQ(instID++, null, null, null),
		        new Quad.IfCmp_LT(instID++, null, null, null),
		        new Quad.IfCmp_LE(instID++, null, null, null),
		        new Quad.IfCmp_GT(instID++, null, null, null),
		        new Quad.IfCmp_GE(instID++, null, null, null) };
		switch (expr.tag)
		{
			case Tree.AND:
				remainderBB = currentCFG.createBasicBlock();
				bin = (Binary) expr;
				translateBranch(Not(TreeInfo.skipParens(bin.lhs)), falseBB,
				        remainderBB);
				startBasicBlock(remainderBB);
				// translates right hand side
				translateBranch(TreeInfo.skipParens(bin.rhs), trueBB, falseBB);
				break;
			case Tree.OR:
				remainderBB = currentCFG.createBasicBlock();
				bin = (Binary) expr;
				translateBranch(TreeInfo.skipParens(bin.lhs), trueBB,
				        remainderBB);
				startBasicBlock(remainderBB);
				// translates right hand side
				translateBranch(TreeInfo.skipParens(bin.rhs), trueBB, falseBB);
				break;
			case Tree.EQ:
			case Tree.NE:
			case Tree.LT:
			case Tree.LE:
			case Tree.GT:
			case Tree.GE:
				inst = insts[expr.tag - Tree.EQ];
				bin = (Binary) expr;
				src1 = genExpr(bin.lhs);
				src2 = genExpr(bin.rhs);
				inst.operand1 = src1;
				inst.operand2 = src2;
				inst.target = new Operand.TargetOperand(trueBB);
				inst.refs++;
				currentBlock.addSuccessor(trueBB);
				trueBB.addPredecessor(currentBlock);

				// appends quad into current block
				appendInst(inst);
			case Tree.NOT:
				Tree inner_tree = ((Unary) expr).arg;
				src1 = genExpr(inner_tree);
				if (inner_tree.type.tag < Type.INT)
				{
					src1 = genCast(src1, inner_tree.type, Type.INTType);
				}
				inst = new Quad.IfCmp_EQ(instID++, src1,
				        new Operand.IConstOperand(0), new TargetOperand(trueBB));
				inst.refs++;
				appendInst(inst);
				currentBlock.addSuccessor(trueBB);
				trueBB.addPredecessor(currentBlock);
				break;
			case Tree.IDENT:
				if (((Ident) expr).name.toString() == "true")
					genJump(trueBB);
				else if (((Ident) expr).name.toString() == "false")
					genJump(falseBB);
				else
					log.error(expr.pos, "not.boolean.type.at.condition");
				break;
			default:
				break;
		}
	}

	/**
	 * Doing type cast.
	 * @param src	The source operand.
	 * @param sType	The source type of operand
	 * @param dType	The target type that operand will be casted.
	 * @return
	 */
	private Operand genCast(Operand src, Type sType, Type dType)
	{
		// the original type
		int srccode = HIR.typecode(sType);
		// the target type
		int targetcode = HIR.typecode(dType);
		if (srccode == targetcode)
			return src;
		else
		{
			// the return value of truncate method at least INTcode
			int typecode1 = HIR.truncate(srccode);
			int targetcode2 = HIR.truncate(targetcode);
			//
			if (typecode1 != targetcode2)
			{

			}
			if (targetcode != targetcode2)
			{

			}
		}
	}

	/**
	 * Creates a temp register with given type.
	 */
	private RegisterOperand createTemp(Type t)
	{
		return this.rf.makeTempRegOp(t);
	}

	/**
	 * Translating the top level tree that the compilation unit. All of
	 * definitions will be handled rather than import statement and global
	 * variable declaration.
	 */
	@Override
	public void visitTopLevel(TopLevel tree)
	{
		for (Tree t : tree.defs)
		{
			// firstly, donn't handle import clause and global variable
			// definition
			if (t.tag == Tree.METHODDEF) 
				t.accept(this);
		}
	}

	/**
	 * Saved current cfg of transforming method.
	 */
	private ControlFlowGraph currentCFG = null;
	private BasicBlock currentBlock = null;
	/**
	 * The id for instruction of cfg.
	 */
	private int instID = 0;

	/**
	 * Two stack for parsing the target of break and continue statement
	 * respectively.
	 */
	private LinkedList<BasicBlock> continueStack, breakStack;

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

			this.continueStack = new LinkedList<>();
			this.breakStack = new LinkedList<>();

			currentCFG.entry().addSuccessor(currentBlock);
			currentBlock.addPredecessor(currentCFG.entry());
			// places actual parameter onto entry block
			for (Tree t : tree.params)
			{
				RegisterOperand dest = rf.makeRegOp(((VarDef) t).sym);

				// a IR placeholder for formal parameter at current cfg
				genAssign(dest, null, t.type);

			}
			// translate method body
			tree.body.accept(this);

			// appends exit block into the successor list of current block.
			currentBlock.addSuccessor(currentCFG.exit());
			// appends current block into the predecessor list of exit block.
			currentCFG.exit().addPredecessor(currentBlock);
			
			this.methods.add(m);
		}
	}

	/**
	 * Appends a instruction into quad list of current block.
	 *
	 * @param dest a register that the destination of this inst.
	 * @param src the source operand of this inst.
	 * @param t the type of data to be stored in dest.
	 */
	private void genAssign(RegisterOperand dest, Operand src, Type t)
	{
		Quad.Assign inst = Quad.Assign.create(instID++, dest, src, t);
		inst.refs++;
		appendInst(inst);
	}

	@Override
	public void visitVarDef(VarDef tree)
	{
		VarSymbol v = tree.sym;
		// create a new local register for variable definition
		RegisterOperand newLocal = this.rf.makeRegOp(v);
		Operand src = null;
		if (tree.init != null)
		{
			checkStringConstant(tree.init.pos, v.constValue);
			src = genExpr(tree.init);
		}
		genAssign(newLocal, src, v.type);
	}

	/**
	 * Translates program block surrounding with a pair of braces.
	 */
	@Override
	public void visitBlock(Block tree)
	{
		for (Tree t : tree.stats)
			t.accept(this);
	}

	/**
	 * Translates expression statement.
	 */
	@Override
	public void visitExec(Exec tree)
	{
		if (tree.expr != null) tree.expr.accept(this);
	}

	/**
	 * Translates if statement.
	 * <p>
	 * if (expr) statement is translated into follow presentation if !expr goto
	 * 
	 * <pre>
	 * nextBB trueBB: ..... nextBB: .....
	 * </pre>
	 * <p>
	 * if (expr) stmt1 else stmt2 is translated into follow presentation
	 * 
	 * <pre>
	 * if !expr goto falseBB
	 * trueBB:
	 * 		.....
	 * falseBB:
	 * 		.....
	 * nextBB:
	 * 		.....
	 * </pre>
	 */
	@Override
	public void visitIf(If tree)
	{
		BasicBlock nextBB = currentCFG.createBasicBlock();
		BasicBlock trueBB = currentCFG.createBasicBlock();
		if (tree.elsepart == null)
		{
			translateBranch(Not(tree.cond), nextBB, trueBB);
			startBasicBlock(trueBB);
			tree.thenpart.accept(this);
		}
		else
		{
			BasicBlock falseBB = currentCFG.createBasicBlock();

			translateBranch(Not(TreeInfo.skipParens(tree.cond)), falseBB,
			        trueBB);

			startBasicBlock(trueBB);
			tree.thenpart.accept(this);
			genJump(nextBB);

			startBasicBlock(falseBB);
			tree.elsepart.accept(this);
		}
		// inserts next basic block into cfg.
		startBasicBlock(nextBB);
	}

	private void pushContinue(BasicBlock target)
	{
		this.continueStack.addLast(target);
	}

	private void popContinue()
	{
		if (continueStack.isEmpty())
		    throw new Error("Unmatched push or pop for continue stack.");
		this.continueStack.removeLast();
	}

	private void pushBreak(BasicBlock target)
	{
		this.breakStack.addLast(target);
	}

	private void popBreak()
	{
		if (breakStack.isEmpty())
		    throw new Error("Unmatched push or pop for break stack.");
		breakStack.removeLast();
	}

	private BasicBlock getCurrrentBreakTarget()
	{
		if (breakStack.isEmpty())
		{
			throw new JumpError(
			        "can not break from no loop or switch statement.");
		}
		return breakStack.getLast();
	}

	private BasicBlock getCurrentContinueTarget()
	{
		if (continueStack.isEmpty())
		{
			throw new JumpError("can not continue from no loop statement.");
		}
		return continueStack.getLast();
	}

	/**
	 * Translates while loop statement into IR. while (expr) stmt is translated
	 * into:
	 * 
	 * <pre>
	 * headerBB:
	 * if (!expr) goto nextBB
	 * loopBB:
	 *     stmt
	 * goto headerBB
	 * 
	 * nextBB:
	 *     ...
	 * </pre>
	 */
	@Override
	public void visitWhileLoop(WhileLoop tree)
	{
		BasicBlock headerBB, loopBB, nextBB;
		headerBB = currentCFG.createBasicBlock();
		loopBB = currentCFG.createBasicBlock();
		nextBB = currentCFG.createBasicBlock();

		// add the target of break and continue into stack
		pushBreak(nextBB);
		pushContinue(headerBB);

		// translates condition
		startBasicBlock(headerBB);
		translateBranch(TreeInfo.skipParens(tree.cond), loopBB, nextBB);

		// translates loop body
		startBasicBlock(loopBB);
		tree.body.accept(this);

		// generates jump instruction from current block to headerB
		// this jump may be removed by optimized int the future
		genJump(headerBB);

		popBreak();
		popContinue();

		startBasicBlock(nextBB);
	}

	/**
	 * Translates do while loop into IRs as follow:
	 * 
	 * <pre>
	 * loopBB:
	 *     stmt
	 * condBB:
	 *     if (expr) goto loopBB
	 * nextBB:
	 *     ...
	 * </pre>
	 */
	@Override
	public void visitDoLoop(DoLoop tree)
	{
		BasicBlock loopBB, condBB, nextBB;
		loopBB = currentCFG.createBasicBlock();
		condBB = currentCFG.createBasicBlock();
		nextBB = currentCFG.createBasicBlock();

		pushBreak(nextBB);
		pushContinue(condBB);

		// starts loopBB
		startBasicBlock(loopBB);
		tree.body.accept(this);

		// translates condtion
		startBasicBlock(condBB);
		translateBranch(tree.cond, loopBB, nextBB);

		popBreak();
		popContinue();

		startBasicBlock(nextBB);
	}

	/**
	 * <pre>
	 * Original statement like:
	 * for (initExpr; condExpr; stepExpr)
	 * {
	 * 		stmts;
	 * }
	 * Translates for loop into IRs as follow:
	 * initExpr
	 * condBB:
	 *     if condExpr goto loopBB
	 * loopBB:
	 *     stmts
	 *     stepExpr
	 * 	   goto condBB
	 * nextBB:
	 * </pre>
	 */
	@Override
	public void visitForLoop(ForLoop tree)
	{
		BasicBlock nextBB, condBB, loopBB;
		nextBB = currentCFG.createBasicBlock();
		condBB = currentCFG.createBasicBlock();
		loopBB = currentCFG.createBasicBlock();

		pushBreak(nextBB);
		pushContinue(condBB);

		// starts initial BB
		genExprList(tree.init);

		// starts conditional BB
		startBasicBlock(condBB);
		translateBranch(tree.cond, loopBB, nextBB);

		// starts loopBB
		startBasicBlock(loopBB);
		tree.body.accept(this);
		genExprList(tree.step);

		// generates jump for jumping to condition BB
		genJump(condBB);

		popBreak();
		popContinue();

		// nextBB is here
		startBasicBlock(nextBB);
	}

	/**
	 * Translates goto statement into IR.
	 */
	@Override
	public void visitGoto(Goto tree)
	{
		/* if the block corresponding to target of this stmt is null, creating a
		 * block to be associated with it. */
		if (tree.target.corrBB == null)
		    tree.target.corrBB = currentCFG.createBasicBlock();
		genJump(tree.target.corrBB);

		// starts a new basic block
		startBasicBlock(currentCFG.createBasicBlock());
	}

	/**
	 * Translates labelled statement.
	 */
	@Override
	public void visitLabelled(Labelled tree)
	{
		/* if the block corresponding to labelled statement is null, creating a
		 * block to be associated with it. */
		if (tree.corrBB == null) tree.corrBB = currentCFG.createBasicBlock();
		startBasicBlock(tree.corrBB);
		tree.body.accept(this);
	}

	/**
	 * Translates break statement into IRs. A break statement terminates the
	 * execution of associated switch or loop.
	 * <p>
	 * 
	 * <pre>
	 * It is translated into:
	 * 		goto the nextBB of switch or loop.
	 * nextBB:
	 * </pre>
	 */
	@Override
	public void visitBreak(Break tree)
	{
		try
		{
			genJump(getCurrrentBreakTarget());
		}
		catch (Exception e)
		{
			log.error(tree.pos, "A.break.not.in.loop.or.switch");
		}
	}

	/**
	 * Translates continue statement into IRs. A continue statement terminates
	 * the execution of associated switch or loop.
	 * <p>
	 * 
	 * <pre>
	 * It is translated into:
	 * 		goto the nextBB of switch or loop.
	 * nextBB:
	 * </pre>
	 */
	@Override
	public void visitContinue(Continue tree)
	{
		try
		{
			genJump(getCurrentContinueTarget());
		}
		catch (Exception e)
		{
			log.error(tree.pos, "A.continue.not.in.loop");
		}
	}

	@Override
	public void visitReturn(Return tree)
	{
		if (tree.expr != null)
		{
			Operand res = genExpr(tree.expr);
			Quad.Return inst = new Quad.Return(instID++, res);
			inst.refs++;
			appendInst(inst);
		}
		// goto the exit of current method.
		genJump(currentCFG.exit());
		startBasicBlock(currentCFG.createBasicBlock());
	}

	/**
	 * Currently, switch is not supported.
	 *
	 * @param tree
	 */
	@Override
	public void visitSwitch(Switch tree)
	{

	}

	/**
	 * Currently, Case statement is not supported.
	 */
	@Override
	public void visitCase(Case tree)
	{

	}

	/**
	 * Just ignores.
	 */
	@Override
	public void visitSkip(Skip tree)
	{

	}

	@Override
	public void visitApply(Apply tree)
	{
		List<RegisterOperand> args = new ArrayList<>();
		// translates actual parameter list
		for (Tree para : tree.args)
			args.add(0, (RegisterOperand) genExpr(para));
		Operand.MethodOperand m = new Operand.MethodOperand(new Method(
		        (MethodDef) tree.meth));
		Operand.ParamListOperand params = new Operand.ParamListOperand(args);
		Quad.Invoke inst = new Quad.Invoke(instID++, null, m, params);
		inst.refs++;
		appendInst(inst);
	}

	@Override
	public void visitParens(Parens tree)
	{
		TreeInfo.skipParens(tree).accept(this);
	}

	/**
	 * <p>
	 * Translates conditional statement like
	 * </p>
	 * 
	 * <pre>
	 * (relation expr) ? expr : expr;
	 * </pre>
	 */
	@Override
	public void visitConditional(Conditional tree)
	{
		// a temporary register where result stores
		RegisterOperand t = createTemp(tree.cond.type);
		Operand t1, t2;
		BasicBlock trueBB, falseBB, nextBB;
		trueBB = currentCFG.createBasicBlock();
		falseBB = currentCFG.createBasicBlock();
		nextBB = currentCFG.createBasicBlock();

		translateBranch(tree.cond, trueBB, falseBB);

		// handles true portion
		startBasicBlock(trueBB);
		t1 = genExpr(tree.truepart);
		if (t1 != null)
		{
			genAssign(t, t1, t1.getType());
		}
		genJump(nextBB);

		// handles the false part
		startBasicBlock(falseBB);
		t2 = genExpr(tree.falsepart);
		if (t2 != null) genAssign(t, t2, t2.getType());

		// starts next basic block
		startBasicBlock(nextBB);

		// sets the result of this expression
		this.exprResult = t;
	}

	/**
	 * <p>
	 * Translates assignment statement into IRs, as depicted follow:
	 * </p>
	 *
	 * @param tree The assignment to be translated.
	 */
	@Override
	public void visitAssign(Tree.Assign tree)
	{
		RegisterOperand dest = (RegisterOperand) genExpr(tree.lhs);
		Operand src = genExpr(tree.rhs);
		// generates move instruction
		genAssign(dest, src, tree.lhs.type);
	}

	/**
	 * Translates assignment with operator.
	 *
	 * @param tree The tree to be transformed.
	 */
	@Override
	public void visitAssignop(Tree.Assignop tree)
	{
		OperatorSymbol operator = (OperatorSymbol) tree.operator;
		if (operator.opcode == OpCodes.string_add)
		{
			/* currently, string concat with add operation is not supported it
			 * may be taken into consideration in the future. */
		}
		else
		{
			RegisterOperand res = (RegisterOperand) genExpr(tree.lhs);
			Operand rhs = genExpr(tree.rhs);
			this.exprResult = transformAssignOp(tree.pos, tree.tag, res,
			        rhs);
		}
	}

	/**
	 * An auxiliary method for translates assignment with op into IRs.
	 * @param pos
	 * @param operator
	 * @param dest
	 * @param src
	 * @return
	 */
	private RegisterOperand transformAssignOp(int pos, int op,
	        RegisterOperand dest, Operand src)
	{
		genAssign(dest, genBin(dest.getType(), op, dest, src, pos), dest.getType());
		return dest;
	}

	/**
	 * Generates an add instruction and inserts into basic block.
	 * @param ty	The data type of result.
	 * @param lhs	The left hand side of it.
	 * @param rhs	The right hand side of it.
	 * @param res	The result of this instruction.
	 */
	private void genADD(Type ty, Operand lhs, Operand rhs, RegisterOperand res)
	{
		if (ty.isIntLike())
		{
			Quad inst = new Quad.ADD_I(instID++, res, lhs, res);
			inst.refs++;
			appendInst(inst);
		}
		else if(ty.equals(Type.LONGType))
		{
			Quad inst = new Quad.ADD_L(instID++, res, lhs, res);
			inst.refs++;
			appendInst(inst);
		}
		else if (ty.equals(Type.FLOATType))
		{
			Quad inst = new Quad.ADD_F(instID++, res, lhs, res);
			inst.refs++;
			appendInst(inst);
		}
		else if (ty.equals(Type.DOUBLEType)) 
		{
			Quad inst = new Quad.ADD_D(instID++, res, lhs, res);
			inst.refs++;
			appendInst(inst);
		}
		else 
			throw new SemanticError("Invalid data type in the add IR.");
	}
	/**
	 * Generates an sub instruction and inserts into basic block.
	 * @param ty	The data type of result.
	 * @param lhs	The left hand side of it.
	 * @param rhs	The right hand side of it.
	 * @param res	The result of this instruction.
	 */
	private void genSUB(Type ty, Operand lhs, Operand rhs, RegisterOperand res)
	{
		if (ty.isIntLike())
		{
			Quad inst = new Quad.SUB_I(instID++, res, lhs, res);
			inst.refs++;
			appendInst(inst);
		}
		else if(ty.equals(Type.LONGType))
		{
			Quad inst = new Quad.SUB_L(instID++, res, lhs, res);
			inst.refs++;
			appendInst(inst);
		}
		else if (ty.equals(Type.FLOATType))
		{
			Quad inst = new Quad.SUB_F(instID++, res, lhs, res);
			inst.refs++;
			appendInst(inst);
		}
		else if (ty.equals(Type.DOUBLEType)) 
		{
			Quad inst = new Quad.SUB_D(instID++, res, lhs, res);
			inst.refs++;
			appendInst(inst);
		}
		else 
			throw new SemanticError("Invalid data type in the sub IR.");
	}
	/**
	 * Generates an mul instruction and inserts into basic block.
	 * @param ty	The data type of result.
	 * @param lhs	The left hand side of it.
	 * @param rhs	The right hand side of it.
	 * @param res	The result of this instruction.
	 */
	private void genMUL(Type ty, Operand lhs, Operand rhs, RegisterOperand res)
	{
		if (ty.isIntLike())
		{
			Quad inst = new Quad.MUL_I(instID++, res, lhs, res);
			inst.refs++;
			appendInst(inst);
		}
		else if(ty.equals(Type.LONGType))
		{
			Quad inst = new Quad.MUL_L(instID++, res, lhs, res);
			inst.refs++;
			appendInst(inst);
		}
		else if (ty.equals(Type.FLOATType))
		{
			Quad inst = new Quad.MUL_F(instID++, res, lhs, res);
			inst.refs++;
			appendInst(inst);
		}
		else if (ty.equals(Type.DOUBLEType)) 
		{
			Quad inst = new Quad.MUL_D(instID++, res, lhs, res);
			inst.refs++;
			appendInst(inst);
		}
		else 
			throw new SemanticError("Invalid data type in the MUL IR.");
	}
	/**
	 * Generates an div instruction and inserts into basic block.
	 * @param ty	The data type of result.
	 * @param lhs	The left hand side of it.
	 * @param rhs	The right hand side of it.
	 * @param res	The result of this instruction.
	 */
	private void genDIV(Type ty, Operand lhs, Operand rhs, RegisterOperand res)
	{
		if (ty.isIntLike())
		{
			Quad inst = new Quad.DIV_I(instID++, res, lhs, res);
			inst.refs++;
			appendInst(inst);
		}
		else if(ty.equals(Type.LONGType))
		{
			Quad inst = new Quad.DIV_L(instID++, res, lhs, res);
			inst.refs++;
			appendInst(inst);
		}
		else if (ty.equals(Type.FLOATType))
		{
			Quad inst = new Quad.DIV_F(instID++, res, lhs, res);
			inst.refs++;
			appendInst(inst);
		}
		else if (ty.equals(Type.DOUBLEType)) 
		{
			Quad inst = new Quad.DIV_D(instID++, res, lhs, res);
			inst.refs++;
			appendInst(inst);
		}
		else 
			throw new SemanticError("Invalid data type in the DIV IR.");
	}
	/**
	 * Generates an mod instruction and inserts into basic block.
	 * @param ty	The data type of result.
	 * @param lhs	The left hand side of it.
	 * @param rhs	The right hand side of it.
	 * @param res	The result of this instruction.
	 */
	private void genMOD(Type ty, Operand lhs, Operand rhs, RegisterOperand res)
	{
		if (ty.isIntLike())
		{
			Quad inst = new Quad.MOD_I(instID++, res, lhs, res);
			inst.refs++;
			appendInst(inst);
		}
		else if(ty.equals(Type.LONGType))
		{
			Quad inst = new Quad.MOD_L(instID++, res, lhs, res);
			inst.refs++;
			appendInst(inst);
		}
		else if (ty.equals(Type.FLOATType))
		{
			Quad inst = new Quad.MOD_F(instID++, res, lhs, res);
			inst.refs++;
			appendInst(inst);
		}
		else if (ty.equals(Type.DOUBLEType)) 
		{
			Quad inst = new Quad.MOD_D(instID++, res, lhs, res);
			inst.refs++;
			appendInst(inst);
		}
		else 
			throw new SemanticError("Invalid data type in the MOD IR.");
	}
	/**
	 * Generates an bit-and instruction and inserts into basic block.
	 * @param ty	The data type of result.
	 * @param lhs	The left hand side of it.
	 * @param rhs	The right hand side of it.
	 * @param res	The result of this instruction.
	 */
	private void genBITAND(Type ty, Operand lhs, Operand rhs, RegisterOperand res)
	{
		if (ty.isIntLike())
		{
			Quad inst = new Quad.AND_I(instID++, res, lhs, res);
			inst.refs++;
			appendInst(inst);
		}
		else if(ty.equals(Type.LONGType))
		{
			Quad inst = new Quad.AND_L(instID++, res, lhs, res);
			inst.refs++;
			appendInst(inst);
		}
		else 
			throw new SemanticError("Invalid data type in the BIT-AND IR.");
	}
	
	/**
	 * Generates an bit-and instruction and inserts into basic block.
	 * @param ty	The data type of result.
	 * @param lhs	The left hand side of it.
	 * @param rhs	The right hand side of it.
	 * @param res	The result of this instruction.
	 */
	private void genBITOR(Type ty, Operand lhs, Operand rhs, RegisterOperand res)
	{
		if (ty.isIntLike())
		{
			Quad inst = new Quad.OR_I(instID++, res, lhs, res);
			inst.refs++;
			appendInst(inst);
		}
		else if(ty.equals(Type.LONGType))
		{
			Quad inst = new Quad.OR_L(instID++, res, lhs, res);
			inst.refs++;
			appendInst(inst);
		}
		else 
			throw new SemanticError("Invalid data type in the BIT-OR IR.");
	}
	/**
	 * Generates an bit-and instruction and inserts into basic block.
	 * @param ty	The data type of result.
	 * @param lhs	The left hand side of it.
	 * @param rhs	The right hand side of it.
	 * @param res	The result of this instruction.
	 */
	private void genBITXOR(Type ty, Operand lhs, Operand rhs, RegisterOperand res)
	{
		if (ty.isIntLike())
		{
			Quad inst = new Quad.XOR_I(instID++, res, lhs, res);
			inst.refs++;
			appendInst(inst);
		}
		else if(ty.equals(Type.LONGType))
		{
			Quad inst = new Quad.XOR_L(instID++, res, lhs, res);
			inst.refs++;
			appendInst(inst);
		}
		else 
			throw new SemanticError("Invalid data type in the BIT-XOR IR.");
	}
	/**
	 * Generates an sheft left instruction and inserts into basic block.
	 * @param ty	The data type of result.
	 * @param lhs	The left hand side of it.
	 * @param rhs	The right hand side of it.
	 * @param res	The result of this instruction.
	 */
	private void genSHL(Type ty, Operand lhs, Operand rhs, RegisterOperand res)
	{
		if (ty.isIntLike())
		{
			Quad inst = new Quad.SHL_I(instID++, res, lhs, res);
			inst.refs++;
			appendInst(inst);
		}
		else if(ty.equals(Type.LONGType))
		{
			Quad inst = new Quad.SHL_L(instID++, res, lhs, res);
			inst.refs++;
			appendInst(inst);
		}
		else 
			throw new SemanticError("Invalid data type in the SHL IR.");
	}
	/**
	 * Generates an sheft rigth instruction and inserts into basic block.
	 * @param ty	The data type of result.
	 * @param lhs	The left hand side of it.
	 * @param rhs	The right hand side of it.
	 * @param res	The result of this instruction.
	 */
	private void genSHR(Type ty, Operand lhs, Operand rhs, RegisterOperand res)
	{
		if (ty.isIntLike())
		{
			Quad inst = new Quad.SHR_I(instID++, res, lhs, res);
			inst.refs++;
			appendInst(inst);
		}
		else if(ty.equals(Type.LONGType))
		{
			Quad inst = new Quad.SHR_L(instID++, res, lhs, res);
			inst.refs++;
			appendInst(inst);
		}
		else 
			throw new SemanticError("Invalid data type in the BIT-AND IR.");
	}
	/**
	 * translates binary operation expression into IRs.
	 * @param op	The operator code.
	 * @param lhs	The left hand side of it.
	 * @param rhs	The right hand side of it.
	 * @return	The result of this operation.
	 */
	private RegisterOperand genBin(Type ty, int op, Operand lhs, Operand rhs, int pos)
	{
		RegisterOperand t = createTemp(ty);
		switch (op)
        {
			case Tree.PLUS:				
				genADD(ty, lhs, rhs, t);
				break;
			case Tree.MINUS:
				genSUB(ty, lhs, rhs, t);
				break;
			case Tree.MUL:
				genMUL(ty, lhs, rhs, t);
				break;
			case Tree.DIV:
				genDIV(ty, lhs, rhs, t);
				break;
			case Tree.MOD:
				genMOD(ty, lhs, rhs, t);
				break;
			case Tree.BITAND:
				genBITAND(ty, lhs, rhs, t);
				break;
			case Tree.BITOR:
				genBITOR(ty, lhs, rhs, t);
				break;
			case Tree.BITXOR:
				genBITXOR(ty, lhs, rhs, t);
				break;
			case Tree.SL:
				genSHL(ty, lhs, rhs, t);
				break;
			case Tree.SR:
				genSHR(ty, lhs, rhs, t);
				break;
			default:
				t = null;
				log.error(pos, "Invalid binary operation at abstract syntax tree");
		}
		return t;
	}
	/**
	 * Generate intermedicate code to calculate a branch expression's value.
	 * <pre> e.g. int a, b; a = a > b;
	 * Introduces a new temporary t to holds the value of a > b.
	 *     if a > b goto trueBB
	 * falseBB:
	 *     t = 0;
	 *     goto nextBB;
	 * trueBB:
	 *     t = 1;
	 * nextBB:
	 *     ...
	 * </pre>
	 */
	private RegisterOperand translateBranchExpression(Tree expr)
	{
		BasicBlock nextBB, trueBB, falseBB;
		RegisterOperand t = createTemp(expr.type);
		nextBB = currentCFG.createBasicBlock();
		trueBB = currentCFG.createBasicBlock();
		falseBB = currentCFG.createBasicBlock();
		
		// do it
		translateBranch(expr, trueBB, falseBB);
		startBasicBlock(falseBB);
		genAssign(t, new Operand.IConstOperand(0), expr.type);
		genJump(nextBB);
		
		startBasicBlock(trueBB);
		genAssign(t, new Operand.IConstOperand(1), expr.type);
		
		// next bb is upcoming
		startBasicBlock(nextBB);
		return t;
	}
	
	@Override
	public void visitBinary(Binary tree)
	{
		if (tree.tag == Tree.OR || tree.tag == Tree.AND ||
				( tree.tag >= Tree.EQ && tree.tag <= Tree.GE))
		{
			this.exprResult = translateBranchExpression(tree);
		}
		Operand lhs = genExpr(tree.lhs);
		Operand rhs = genExpr(tree.rhs);

		this.exprResult = genBin(tree.type, tree.tag, lhs, rhs, tree.pos);
	}
	
	private RegisterOperand translateIncrement(Unary expr)
	{
		Operand res = genExpr(expr);
		try
        {
	        Operand temp = res.clone();
			switch (expr.tag)
	        {
				case Tree.PREDEC:
					res = genBin(res.getType(), Tree.MINUS, res, new Operand.IConstOperand(1), expr.pos);
					this.exprResult = res;
					return (RegisterOperand) res;
				case Tree.PREINC:
					res = genBin(res.getType(), Tree.PLUS, res, new Operand.IConstOperand(1), expr.pos);
					this.exprResult = res;
					return (RegisterOperand) res;
					
				case Tree.POSTDEC:
					res = genBin(res.getType(), Tree.MINUS, res, new Operand.IConstOperand(1), expr.pos);
					this.exprResult = temp;
					return (RegisterOperand) temp;
				case Tree.POSTINC:
					res = genBin(res.getType(), Tree.PLUS, res, new Operand.IConstOperand(1), expr.pos);
					this.exprResult = temp;
					return (RegisterOperand) temp;
				default:
					return null;
			}
        }
        catch (CloneNotSupportedException e)
        {
	        e.printStackTrace();
	        return null;
        }
	}
	/**
	 * Translates unary operation expression.
	 * @param tree	The expression to be translated.
	 */
	@Override
	public void visitUnary(Unary tree)
	{
		// firstly, prefix operation is handled
		if (tree.tag == Tree.NOT)
		{
			this.exprResult = translateBranchExpression(tree);
			return;
		}
		if (tree.tag >= Tree.PREINC && tree.tag <= Tree.PREDEC)			
		{
			this.exprResult = translateIncrement(tree);
			return;
		}
		Operand operand1 = genExpr(tree.arg);
		switch (tree.tag)
        {
			case Tree.TYPECAST:
				this.exprResult = genCast(operand1, tree.type, tree.arg.type);
				break;
			case Tree.NEG:
				
				break;
			case Tree.COMPL:
				
				break;
			case Tree.INDEXED:
				this.exprResult = genExpr(tree.arg);
				break;
			case Tree.APPLY:
				this.exprResult = genExpr(tree.arg);
				break;
			case Tree.POSTDEC:
			case Tree.POSTINC:
				this.exprResult = translateIncrement(tree);
				break;
			default:
				this.exprResult = null;
				break;
		}		
	}
	/**
	 * Translates array index access expression into IRs.
	 * @param tree
	 */
	@Override
	public void visitIndexed(Indexed tree)
	{
	
	}
	/**
	 * Translates type cast expression.
	 * @param tree
	 */
	@Override
	public void visitTypeCast(TypeCast tree)
	{
		Operand res = genExpr(tree.expr);
		this.exprResult = genCast(res, res.getType(), tree.clazz.type);		
	}
	
	@Override
	public void visitLiteral(Tree.Literal tree)
	{
		if (tree.typetag == TypeTags.INT)
		{
			this.exprResult = new Operand.IConstOperand(
			        ((Integer) tree.value).intValue());
		}
		else if (tree.typetag == TypeTags.LONG)
		{
			this.exprResult = new Operand.LConstOperand(
			        ((Long) tree.value).longValue());
		}
		else if (tree.typetag == TypeTags.FLOAT)
		{
			this.exprResult = new Operand.FConstOperand(
			        ((Float) tree.value).floatValue());
		}
		else if (tree.typetag == TypeTags.DOUBLE)
		{
			this.exprResult = new Operand.DConstOperand(
			        ((Double) tree.value).doubleValue());
		}
	}

	/**
	 * Translates a erroneous abstract syntax tree.
	 *
	 * @param erroneous The erroneous tree.
	 */
	@Override
	public void visitErroneous(Tree.Erroneous erroneous)
	{
		log.error(erroneous.pos, "A.errorous.tree");
		this.exprResult = null;
	}
}
