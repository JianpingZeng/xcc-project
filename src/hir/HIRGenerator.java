package hir;

import ci.CiConstant;
import ci.CiKind;
import comp.OpCodes;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import exception.JumpError;
import exception.SemanticError;
import hir.Instruction.*;
import symbol.Symbol;
import symbol.Symbol.OperatorSymbol;
import symbol.SymbolKinds;
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
 * <p>
 * 	在转换成为SSA形式的IR时，将所有的全局变量看作放在内存中，当在函数内部使用时，使用一个虚拟临时寄存器T1存储它。
 * 	当函数返回之时，对于已经修改的变量，写回到变量中。
 * </p>
 * @author Jianping Zeng<z1215jping@hotmail.com>
 * @version 2016年2月3日 下午12:08:05
 */
public class HIRGenerator extends ASTVisitor
{
	private final static Context.Key ASTToQuadKey = new Context.Key();
	private Log log;
	private Context context;
	private List<Variable> vars;
	private List<Method> methods;
	private TreeMaker maker;
	private Name.Table names;

	public HIRGenerator(Context context)
	{
		this.context = context;
		context.put(ASTToQuadKey, this);
		this.maker = TreeMaker.instance(context);
		this.names = Name.Table.instance(context);
		this.log = Log.instance(context);
		this.vars = new ArrayList<>();
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
		tree.accept(this);
		return HIR.instance(context, vars, methods);
	}

	/**
	 * Appends a quad instruction into current basic block.
	 *
	 * @param inst The quad to be appended.
	 */
	private void appendInst(Instruction inst)
	{
		currentBlock.appendQuad(inst);
	}

	private int nerrs = 0;

	/**
	 * Checks constant for string variable and report if it is a string that is
	 * too large.
	 *
	 * @param pos The position to error reporting.
	 * @param constValue The constant inst of string.
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
	private Instruction exprResult = null;

	/**
	 * Generates IR for expression.
	 *
	 * @param expr
	 */
	private Instruction genExpr(Tree expr)
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
		exprs.forEach(this::genExpr);
	}

	/**
	 * Generates a jump instruction to be inserted into instructions list of a
	 * basic block.
	 *
	 * @param target The target of jump inst
	 */
	private void genJump(BasicBlock target)
	{
		Instruction.Goto goto_ = new Instruction.Goto(target);
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
		Binary bin;
		Unary unary;
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
					if (ident.name.toString().equals("true"))
					{
						return maker.Ident(Name.fromString(names, "false"));
					}
					else if (ident.name.toString().equals("false"))
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
		return null;
	}

	/**
	 * Translates conditional branch.
	 *
	 * @param expr  The relative expression.
	 * @param trueBB the target block when condition is true.
	 * @param falseBB the target block when condition is false.
	 */
	private void translateBranch(Tree expr, BasicBlock trueBB,
	        BasicBlock falseBB)
	{
		BasicBlock remainderBB;
		Binary bin;
		Instruction src1;
		IntCmp inst;
		IntCmp[] insts = { new Instruction.IfCmp_EQ(null, null, null, null),
		        new Instruction.IfCmp_NEQ(null, null, null, null),
		        new Instruction.IfCmp_LT(null, null, null, null),
		        new Instruction.IfCmp_LE(null, null, null, null),
		        new Instruction.IfCmp_GT(null, null, null, null),
		        new Instruction.IfCmp_GE(null, null, null, null)
		};
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
				inst.x = genExpr(bin.lhs);
				inst.y = genExpr(bin.rhs);
				inst.trueTarget = trueBB;
				inst.falseTarget = falseBB;
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
				inst = new IfCmp_EQ(src1, Constant.forInt(0), trueBB, falseBB);
				inst.refs++;
				appendInst(inst);
				currentBlock.addSuccessor(trueBB);
				trueBB.addPredecessor(currentBlock);
				break;
			case Tree.IDENT:
				if (((Ident) expr).name.toString().equals("true"))
					genJump(trueBB);
				else if (((Ident) expr).name.toString().equals("false"))
					genJump(falseBB);
				else
					log.error(expr.pos, "not.boolean.type.at.condition");
				break;
			default:
				break;
		}
	}

	/**
	 * %%%%%%<h2>This method is not implemented, currently.</h2> %%%%%%%
	 * Doing type cast.
	 * @param src	The source operand.
	 * @param sType	The source type of operand
	 * @param dType	The target type that operand will be casted.
	 * @return
	 */
	private Instruction genCast(Instruction src, Type sType, Type dType)
	{
		/*
		// the original type
		int srccode = HIR.typecode(sType);
		// the target type
		int targetcode = HIR.typecode(dType);
		if (srccode == targetcode)
			return src;
		else
		{
			// the return inst of truncate method at least INTcode
			int typecode1 = HIR.truncate(srccode);
			int targetcode2 = HIR.truncate(targetcode);
			//
			if (typecode1 != targetcode2)
			{

			}
			if (targetcode != targetcode2)
			{

			}
			// 
			
		}
		*/
		return null;
	}

	/**
	 * Creates a temp variable with given type.
	 */
	private Local createTemp(Type t)
	{
		return new Local(type2Kind(t), tempNameGenerator.next());
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
	 * Two stack for parsing the target of break and continue statement
	 * respectively.
	 */
	private LinkedList<BasicBlock> continueStack, breakStack;

	private TempNameGenerator tempNameGenerator =
			TempNameGenerator.instance();

	/**
	 * Translates method definition into intermediate representation(IR).
	 */
	@Override
	public void visitMethodDef(MethodDef tree)
	{
		if (tree.body != null)
		{
			Method m = new Method(tree);
			currentCFG = new ControlFlowGraph(m);
			m.cfg = currentCFG;
			this.methods.add(m);

			// sets the current block with entry of a cfg with increment number id.
			this.currentBlock = currentCFG.createBasicBlock();

			this.continueStack = new LinkedList<>();
			this.breakStack = new LinkedList<>();

			// a generator for temporary variable.
			tempNameGenerator.init();

			currentCFG.entry().addSuccessor(currentBlock);
			currentBlock.addPredecessor(currentCFG.entry());
			// places actual parameter onto entry block
			for (Tree t : tree.params)
			{
				genAllocaForVarDecl((VarDef) t);
			}
			// translate method body
			tree.body.accept(this);

			// appends exit block into the successor list of current block.
			currentBlock.addSuccessor(currentCFG.exit());
			// appends current block into the predecessor list of exit block.
			currentCFG.exit().addPredecessor(currentBlock);
			new EnterSSA(currentCFG, tempNameGenerator);
			this.methods.add(m);
		}
	}

	/**
	 * generates memory {@code Alloca} instruction and initial store for variable
	 * declaration.
	 * @param var   The variable to be allocated.
	 */
	private void genAllocaForVarDecl(VarDef var)
	{
		CiKind varKind = type2Kind(var.type);

		// creates a new local
		Local local = new Local(varKind, var.name.toString());

		Alloca inst = new Alloca(varKind, local);
		inst.refs++;
		appendInst(inst);
		local.memAddr = inst;

		// associte its local with variable symbol
		var.sym.varInst = local;

		Instruction initValue;
		if (var.init != null)
		{
			checkStringConstant(var.init.pos, var.sym.constValue);
			// generates IR for initial expression.
			initValue = genExpr(var.init);
		}
		// setting default inst for different inst type
		else
		{
			initValue = new Constant(CiConstant.defaultValue(varKind));
		}
		// generats store instruction that stores initial inst into specified local
		Instruction store = new StoreInst(initValue, local);
		store.refs++;
		appendInst(store);
	}

	/**
	 * Converts {@code Type} into {@code CiKind}.
	 * @param ty
	 * @return
	 */
	static CiKind type2Kind(Type ty)
	{
		if (ty.isIntLike())
			return CiKind.Int;
		else if (ty == Type.LONGType)
			return CiKind.Long;
		else if (ty == Type.FLOATType)
			return CiKind.Float;
		else if (ty == Type.DOUBLEType)
			return CiKind.Double;
		else if (ty == Type.VOIDType)
			return CiKind.Void;
		else
			return CiKind.Illegal;
	}

	/**
	 * Allocates memory at the stack and initializing for variable declaration.
	 * @param tree  Variable definition or declaration.
	 */
	@Override
	public void visitVarDef(VarDef tree)
	{
		genAllocaForVarDecl(tree);
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

	/**
	 * Translates return statement into IR.
	 * @param tree  The return tree node.
	 */
	@Override
	public void visitReturn(Return tree)
	{
		Instruction.Return inst;
		if (tree.expr != null)
		{
			Instruction res = genExpr(tree.expr);
			inst = new Instruction.Return(res);
		}
		else
			inst = new Instruction.Return(null);

		inst.refs++;
		appendInst(inst);
		// goto the exit of current method.
		genJump(currentCFG.exit());
		startBasicBlock(currentCFG.createBasicBlock());
	}

	/**
	 * Currently, switch is not supported.
	 *
	 * @param tree The {@code Switch} expression node.
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

	/**
	 * Handling function invocation expression.
	 * @param tree  The invocation expression.
	 */
	@Override
	public void visitApply(Apply tree)
	{
		Instruction[] args = new Instruction[tree.args.size()];
		// translates actual parameter list
		int idx = 0;
		for (Tree para : tree.args)
			args[idx++] = genExpr(para);

		Method m = (new Method((MethodDef) tree.meth));
		// creates a temporally local variable for storing return value.
		Local ret = createTemp(m.signature().returnType());

		Invoke inst = new Invoke(returnKind(m), args, m, ret);
		inst.refs++;
		appendInst(inst);
		// sets the result of this expression
		this.exprResult = inst;
	}

	/**
	 * Gets the return kind of specified method.
	 * @param target    The targeted method.
	 * @return  The return kind.
	 */
	private CiKind returnKind(Method target)
	{
		return target.signature().returnKind();
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
		Local t = createTemp(tree.cond.type);
		Instruction t1, t2;
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
			genStore(t1, t);
		}
		genJump(nextBB);

		// handles the false part
		startBasicBlock(falseBB);
		t2 = genExpr(tree.falsepart);
		if (t2 != null) genStore(t2, t);

		// starts next basic block
		startBasicBlock(nextBB);

		// sets the result of this expression
		this.exprResult = t;
	}

	/**
	 * Generates move instrcution.
	 * @param src   The source of move, including all of instruction.
	 * @param dest  The target of move, which is variable, occasionally.
	 */
	private void genStore(Instruction src, Instruction dest)
	{
		StoreInst inst = new StoreInst(src.result, dest.result);
		inst.refs++;
		appendInst(inst);
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
		Instruction lhs = genExpr(tree.lhs);
		Instruction rhs = genExpr(tree.rhs);
		// generates move instruction
		genStore(rhs, lhs);
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
			Instruction lhs = genExpr(tree.lhs);
			Instruction rhs = genExpr(tree.rhs);
			this.exprResult = transformAssignOp(tree.lhs.type, tree.pos, tree.tag,
					lhs.result,
			        rhs.result);
		}
	}

	/**
	 * An auxiliary method for translates assignment with op into IRs.
	 * @param pos   The position of this expression.
	 * @param op    The opcode.
	 * @param dest  The destination.
	 * @param src   The source.
	 * @return  Result of this instruction.
	 */
	private Instruction transformAssignOp(Type ty, int pos, int op,
	        Instruction dest, Instruction src)
	{
		genStore(genBin(ty, op, dest, src, pos), dest);
		return dest;
	}

	/**
	 * Generates an add instruction and inserts into basic block.
	 * @param ty	The data type of result.
	 * @param lhs	The left hand side of it.
	 * @param rhs	The right hand side of it.
	 */
	private Instruction genADD(Type ty, Instruction lhs, Instruction rhs)
	{
		Instruction inst = null;

		// for storing result of this operation.
		Instruction result = createTemp(ty);
		if (ty.isIntLike())
		{
			inst = new Instruction.ADD_I(type2Kind(ty), lhs, rhs, result);
			inst.refs++;
			appendInst(inst);

		}
		else if(ty.equals(Type.LONGType))
		{
			inst = new Instruction.ADD_L(type2Kind(ty), lhs, rhs, result);
			inst.refs++;
			appendInst(inst);
		}
		else if (ty.equals(Type.FLOATType))
		{
			inst = new Instruction.ADD_F(type2Kind(ty), lhs, rhs, result);
			inst.refs++;
			appendInst(inst);
		}
		else if (ty.equals(Type.DOUBLEType)) 
		{
			inst = new Instruction.ADD_D(type2Kind(ty), lhs, rhs, result);
			inst.refs++;
			appendInst(inst);
		}
		else 
			throw new SemanticError("Invalid data type in the add IR.");
		return inst;
	}
	/**
	 * Generates an sub instruction and inserts into basic block.
	 * @param ty	The data type of result.
	 * @param lhs	The left hand side of it.
	 * @param rhs	The right hand side of it.
	 */
	private Instruction genSUB(Type ty, Instruction lhs, Instruction rhs)
	{
		Instruction inst = null;
		// for storing result of this operation.
		Instruction result = createTemp(ty);
		if (ty.isIntLike())
		{
			inst = new Instruction.SUB_I(type2Kind(ty), lhs, rhs, result);
			inst.refs++;
			appendInst(inst);
		}
		else if(ty.equals(Type.LONGType))
		{
			inst = new Instruction.SUB_L(type2Kind(ty), lhs, rhs, result);
			inst.refs++;
			appendInst(inst);
		}
		else if (ty.equals(Type.FLOATType))
		{
			inst = new Instruction.SUB_F(type2Kind(ty), lhs, rhs, result);
			inst.refs++;
			appendInst(inst);
		}
		else if (ty.equals(Type.DOUBLEType)) 
		{
			inst = new Instruction.SUB_D(type2Kind(ty), lhs, rhs, result);
			inst.refs++;
			appendInst(inst);
		}
		else 
			throw new SemanticError("Invalid data type in the sub IR.");
		return inst;
	}
	/**
	 * Generates an mul instruction and inserts into basic block.
	 * @param ty	The data type of result.
	 * @param lhs	The left hand side of it.
	 * @param rhs	The right hand side of it.
	 */
	private Instruction genMUL(Type ty, Instruction lhs, Instruction rhs)
	{
		Instruction inst = null;

		// for storing result of this operation.
		Instruction result = createTemp(ty);
		if (ty.isIntLike())
		{
			inst = new Instruction.MUL_I(type2Kind(ty), lhs, rhs, result);
			inst.refs++;
			appendInst(inst);
		}
		else if(ty.equals(Type.LONGType))
		{
			inst = new Instruction.MUL_L(type2Kind(ty), lhs, rhs, result);
			inst.refs++;
			appendInst(inst);
		}
		else if (ty.equals(Type.FLOATType))
		{
			inst = new Instruction.MUL_F(type2Kind(ty), lhs, rhs, result);
			inst.refs++;
			appendInst(inst);
		}
		else if (ty.equals(Type.DOUBLEType)) 
		{
			inst = new Instruction.MUL_D(type2Kind(ty), lhs, rhs, result);
			inst.refs++;
			appendInst(inst);
		}
		else 
			throw new SemanticError("Invalid data type in the MUL IR.");
		return inst;
	}
	/**
	 * Generates an div instruction and inserts into basic block.
	 * @param ty	The data type of result.
	 * @param lhs	The left hand side of it.
	 * @param rhs	The right hand side of it.
	 */
	private Instruction genDIV(Type ty, Instruction lhs, Instruction rhs)
	{
		Instruction inst = null;

		// for storing result of this operation.
		Instruction result = createTemp(ty);
		if (ty.isIntLike())
		{
			inst = new Instruction.DIV_I(type2Kind(ty), lhs, rhs, result);
			inst.refs++;
			appendInst(inst);
		}
		else if(ty.equals(Type.LONGType))
		{
			inst = new Instruction.DIV_L(type2Kind(ty), lhs, rhs, result);
			inst.refs++;
			appendInst(inst);
		}
		else if (ty.equals(Type.FLOATType))
		{
			inst = new Instruction.DIV_F(type2Kind(ty), lhs, rhs, result);
			inst.refs++;
			appendInst(inst);
		}
		else if (ty.equals(Type.DOUBLEType)) 
		{
			inst = new Instruction.DIV_D(type2Kind(ty), lhs, rhs, result);
			inst.refs++;
			appendInst(inst);
		}
		else 
			throw new SemanticError("Invalid data type in the DIV IR.");
		return inst;
	}
	/**
	 * Generates an mod instruction and inserts into basic block.
	 * @param ty	The data type of result.
	 * @param lhs	The left hand side of it.
	 * @param rhs	The right hand side of it.
	 */
	private Instruction genMOD(Type ty, Instruction lhs, Instruction rhs)
	{
		Instruction inst = null;

		// for storing result of this operation.
		Instruction result = createTemp(ty);
		if (ty.isIntLike())
		{
			inst = new Instruction.MOD_I(type2Kind(ty), lhs, rhs, result);
			inst.refs++;
			appendInst(inst);
		}
		else if(ty.equals(Type.LONGType))
		{
			inst = new Instruction.MOD_L(type2Kind(ty), lhs, rhs, result);
			inst.refs++;
			appendInst(inst);
		}
		else if (ty.equals(Type.FLOATType))
		{
			inst = new Instruction.MOD_F(type2Kind(ty), lhs, rhs, result);
			inst.refs++;
			appendInst(inst);
		}
		else if (ty.equals(Type.DOUBLEType)) 
		{
			inst = new Instruction.MOD_D(type2Kind(ty), lhs, rhs, result);
			inst.refs++;
			appendInst(inst);
		}
		else 
			throw new SemanticError("Invalid data type in the MOD IR.");
		return inst;
	}
	/**
	 * Generates an bit-and instruction and inserts into basic block.
	 * @param ty	The data type of result.
	 * @param lhs	The left hand side of it.
	 * @param rhs	The right hand side of it.
	 */
	private Instruction genBITAND(Type ty, Instruction lhs, Instruction rhs)
	{
		Instruction inst = null;

		// for storing result of this operation.
		Instruction result = createTemp(ty);
		if (ty.isIntLike())
		{
			inst = new Instruction.AND_I(type2Kind(ty), lhs, rhs, result);
			inst.refs++;
			appendInst(inst);
		}
		else if(ty.equals(Type.LONGType))
		{
			inst = new Instruction.AND_L(type2Kind(ty), lhs, rhs, result);
			inst.refs++;
			appendInst(inst);
		}
		else 
			throw new SemanticError("Invalid data type in the BIT-AND IR.");
		return inst;
	}
	
	/**
	 * Generates an bit-and instruction and inserts into basic block.
	 * @param ty	The data type of result.
	 * @param lhs	The left hand side of it.
	 * @param rhs	The right hand side of it.
	 */
	private Instruction genBITOR(Type ty, Instruction lhs, Instruction rhs)
	{
		Instruction inst = null;

		// for storing result of this operation.
		Instruction result = createTemp(ty);
		if (ty.isIntLike())
		{
			inst = new Instruction.OR_I(type2Kind(ty), lhs, rhs, result);
			inst.refs++;
			appendInst(inst);
		}
		else if(ty.equals(Type.LONGType))
		{
			inst = new Instruction.OR_L(type2Kind(ty), lhs, rhs, result);
			inst.refs++;
			appendInst(inst);
		}
		else 
			throw new SemanticError("Invalid data type in the BIT-OR IR.");
		return inst;
	}
	/**
	 * Generates an bit-and instruction and inserts into basic block.
	 * @param ty	The data type of result.
	 * @param lhs	The left hand side of it.
	 * @param rhs	The right hand side of it.
	 */
	private Instruction genBITXOR(Type ty, Instruction lhs, Instruction rhs)
	{
		Instruction inst = null;

		// for storing result of this operation.
		Instruction result = createTemp(ty);
		if (ty.isIntLike())
		{
			inst = new Instruction.XOR_I(type2Kind(ty), lhs, rhs, result);
			inst.refs++;
			appendInst(inst);
		}
		else if(ty.equals(Type.LONGType))
		{
			inst = new Instruction.XOR_L(type2Kind(ty), lhs, rhs, result);
			inst.refs++;
			appendInst(inst);
		}
		else 
			throw new SemanticError("Invalid data type in the BIT-XOR IR.");
		return inst;
	}
	/**
	 * Generates an sheft left instruction and inserts into basic block.
	 * @param ty	The data type of result.
	 * @param lhs	The left hand side of it.
	 * @param rhs	The right hand side of it.
	 */
	private Instruction genSHL(Type ty, Instruction lhs, Instruction rhs)
	{
		Instruction inst = null;

		// for storing result of this operation.
		Instruction result = createTemp(ty);
		if (ty.isIntLike())
		{
			inst = new Instruction.SHL_I(type2Kind(ty), lhs, rhs, result);
			inst.refs++;
			appendInst(inst);
		}
		else if(ty.equals(Type.LONGType))
		{
			inst = new Instruction.SHL_L(type2Kind(ty), lhs, rhs, result);
			inst.refs++;
			appendInst(inst);
		}
		else 
			throw new SemanticError("Invalid data type in the SHL IR.");
		return inst;
	}
	/**
	 * Generates an sheft rigth instruction and inserts into basic block.
	 * @param ty	The data type of result.
	 * @param lhs	The left hand side of it.
	 * @param rhs	The right hand side of it.
	 */
	private Instruction genSHR(Type ty, Instruction lhs, Instruction rhs)
	{
		Instruction inst = null;

		// for storing result of this operation.
		Instruction result = createTemp(ty);
		if (ty.isIntLike())
		{
			inst = new Instruction.SHR_I(type2Kind(ty), lhs, rhs, result);
			inst.refs++;
			appendInst(inst);
		}
		else if(ty.equals(Type.LONGType))
		{
			inst = new Instruction.SHR_L(type2Kind(ty), lhs, rhs, result);
			inst.refs++;
			appendInst(inst);
		}
		else 
			throw new SemanticError("Invalid data type in the BIT-AND IR.");
		return inst;
	}
	/**
	 * translates binary operation expression into IRs.
	 * @param op	The operator code.
	 * @param lhs	The left hand side of it.
	 * @param rhs	The right hand side of it.
	 * @return	The result of this operation.
	 */
	private Instruction genBin(Type ty, int op, Instruction lhs, Instruction rhs, int pos)
	{
		Instruction res = null;
		switch (op)
        {
			case Tree.PLUS:				
				res = genADD(ty, lhs, rhs);
				break;
			case Tree.MINUS:
				res = genSUB(ty, lhs, rhs);
				break;
			case Tree.MUL:
				res = genMUL(ty, lhs, rhs);
				break;
			case Tree.DIV:
				res = genDIV(ty, lhs, rhs);
				break;
			case Tree.MOD:
				res = genMOD(ty, lhs, rhs);
				break;
			case Tree.BITAND:
				res = genBITAND(ty, lhs, rhs);
				break;
			case Tree.BITOR:
				res = genBITOR(ty, lhs, rhs);
				break;
			case Tree.BITXOR:
				res = genBITXOR(ty, lhs, rhs);
				break;
			case Tree.SL:
				res = genSHL(ty, lhs, rhs);
				break;
			case Tree.SR:
				res = genSHR(ty, lhs, rhs);
				break;
			default:
				log.error(pos, "Invalid binary operation at abstract syntax tree");
		}
		return res;
	}
	/**
	 * Generate intermedicate code to calculate a branch expression's inst.
	 * <pre> e.g. int a, b; a = a > b;
	 * Introduces a new temporary t to holds the inst of a > b.
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
	private Instruction translateBranchExpression(Tree expr)
	{
		BasicBlock nextBB, trueBB, falseBB;
		Local t = createTemp(expr.type);
		nextBB = currentCFG.createBasicBlock();
		trueBB = currentCFG.createBasicBlock();
		falseBB = currentCFG.createBasicBlock();
		
		// do it
		translateBranch(expr, trueBB, falseBB);
		startBasicBlock(falseBB);
		genStore(new Instruction.Constant(CiConstant.INT_0), t);
		genJump(nextBB);
		
		startBasicBlock(trueBB);
		genStore(new Instruction.Constant(CiConstant.INT_1), t);
		
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
		Instruction lhs = genExpr(tree.lhs);
		Instruction rhs = genExpr(tree.rhs);

		this.exprResult = genBin(tree.type, tree.tag, lhs.result, rhs.result, tree.pos);
	}
	
	private Instruction translateIncrement(Unary expr)
	{
		Instruction res = genExpr(expr).result;
		try
        {
	        Instruction temp = res.clone();
			switch (expr.tag)
	        {
				case Tree.PREDEC:
					res = genBin(expr.type, Tree.MINUS, res, new Constant(CiConstant.INT_1), expr.pos);
					this.exprResult = res;
					return res;
				case Tree.PREINC:
					res = genBin(expr.type, Tree.PLUS, res, new Constant(CiConstant.INT_1), expr.pos);
					this.exprResult = res;
					return res;
					
				case Tree.POSTDEC:
					res = genBin(expr.type, Tree.MINUS, res, new Constant(CiConstant.INT_1), expr.pos);
					this.exprResult = temp;
					return temp;
				case Tree.POSTINC:
					res = genBin(expr.type, Tree.PLUS, res, new Constant(CiConstant.INT_1), expr.pos);
					this.exprResult = temp;
					return temp;
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
		Instruction operand1 = genExpr(tree.arg).result;
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
		Instruction res = genExpr(tree.expr);
		this.exprResult = genCast(res, tree.expr.type, tree.clazz.type);
	}
	
	@Override
	public void visitLiteral(Tree.Literal tree)
	{
		if (tree.typetag == TypeTags.INT)
		{
			this.exprResult = new Constant(new CiConstant(CiKind.Int, ((Integer)tree.value).intValue()));
		}
		else if (tree.typetag == TypeTags.LONG)
		{
			this.exprResult = new Constant(new CiConstant(CiKind.Long, ((Long)tree.value).longValue()));
		}
		else if (tree.typetag == TypeTags.FLOAT)
		{
			this.exprResult = new Constant(new CiConstant(CiKind.Float, ((Float)tree.value).longValue()));
		}
		else if (tree.typetag == TypeTags.DOUBLE)
		{
			this.exprResult = new Constant(new CiConstant(CiKind.Double, ((Double)tree.value).longValue()));
		}
		else if (tree.typetag == TypeTags.BOOL)
		{
			this.exprResult = ((Boolean)tree.value).equals("true")
					? Constant.forInt(1) : Constant.forInt(0);
		}
	}

	/**
	 * Generates loading instruction for local or global instruction that loads
	 * a variable into a temporary virtual variable.
	 * @param src   The source instruction that will be loaded into target.
	 * @param target    A temporary virtual variable.
	 */
	private void genLoadInstruction(Instruction src, Local target)
	{
		LoadInst inst = new LoadInst(target.kind, src, target);
		inst.refs++;
		appendInst(inst);
	}

	/**
	 * Translates variable reference.
	 * @param tree  The variable identifier.
	 */
	@Override
	public void visitIdent(Ident tree)
	{
		// parses variable
		if (tree.sym.kind == SymbolKinds.VAR)
		{
			Symbol.VarSymbol sym = (Symbol.VarSymbol)tree.sym;

			Local t = createTemp(sym.type);
			genLoadInstruction(sym.varInst, t);
		}
		// for handling aggregate type, like array and struct
		if (tree.sym.kind == SymbolKinds.COMPOSITE)
		{

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

	/**
	 * A Class served as generator for yielding the name of a temporary variable.
	 * <p>
	 *     All temporary name generated by this class is described as follow:
	 *     $1, $2, $3.
	 * </p>
	 */
	static class TempNameGenerator
	{
		private int idx;
		private static TempNameGenerator instance = null;
		/**
		 * Constructor that initialize the index with {@code 0}.
		 */
		private TempNameGenerator()
		{
			idx = 0;
		}

		/**
		 * Singleton pattern.
 		 * @return  The concrete instance of this class.
		 */
		public static TempNameGenerator instance()
		{
			if (instance == null)
				instance = new TempNameGenerator();
			return  instance;
		}

		/**
		 * Intialize the {@TempNameGenerator##idx} with zero.
		 */
		public void init()
		{
			idx = 0;
		}

		/**
		 * Gets the next index as string.
		 * @return  The next index.
		 */
		public String next()
		{
			String postfix = "$" + idx;
			idx++;
			return postfix;
		}
	}
}
