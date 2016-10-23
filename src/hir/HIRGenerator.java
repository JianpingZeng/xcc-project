package hir;

import ast.ASTVisitor;
import ast.Tree;
import ast.Tree.*;
import ast.Tree.GotoStmt;
import ast.Tree.ReturnStmt;
import hir.Value.Constant;
import lir.ci.LIRConstant;
import lir.ci.LIRKind;
import comp.OpCodes;
import exception.JumpError;
import hir.Instruction.*;
import symbol.Symbol.OperatorSymbol;
import symbol.SymbolKinds;
import symbol.VarSymbol;
import type.Type;
import type.TypeClass;
import utils.Context;
import utils.Log;
import utils.Name;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

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
 * <p>
 * When we performs that translating normal IR into IR in SSA form, all of global
 * variables will be ignored since the benefit of performing Memory-SSA can not
 * make up for loss of time and memory.
 *
 * Instead of a virtual register will be used for handling the global variable,
 * all of operations performed over global variable will takes effect on virtual
 * register.
 * </p>
 *
 * @author Xlous.zeng 
 * @version 1.0
 */
public class HIRGenerator extends ASTVisitor
{
	private final static Context.Key AstToCfgKey = new Context.Key();
	private Log log;
	private Context context;
	private List<Variable> vars;
	private List<Function> functions;
	private Name.Table names;

	public HIRGenerator(Context context)
	{
		this.context = context;
		context.put(AstToCfgKey, this);
		this.names = Name.Table.instance(context);
		this.log = Log.instance(context);
		this.vars = new ArrayList<>();
		this.functions = new ArrayList<>();
	}

	/**
	 * A translation method for converting from abstract syntax tree into IR.
	 *
	 * @return Module An instance of IR in SSA form representing single compilation
	 * file.
	 */
	public Module translate(Tree tree)
	{
		tree.accept(this);
		return Module.instance(context, vars, functions);
	}

	/**
	 * A programmatic interface for compiling an single method declaration.
	 *
	 * @param tree
	 * @return
	 */
	public Module traslate(MethodDef tree)
	{
		tree.accept(this);
		return Module.instance(context, vars, functions);
	}

	/**
	 * Appends a quad instruction into current basic block.
	 *
	 * @param inst The quad to be appended.
	 */
	private void appendInst(Value inst)
	{
		if (inst instanceof Instruction)
			currentBlock.appendInst((Instruction) inst);
	}

	private int nerrs = 0;

	/**
	 * Checks constant for string variable and report if it is a string that is
	 * too large.
	 *
	 * @param pos        The position to error reporting.
	 * @param constValue The constant inst of string.
	 */
	private void checkStringConstant(int pos, Object constValue)
	{
		if (nerrs != 0 || constValue == null || !(constValue instanceof String)
				|| ((String) constValue).length()
				> ControlFlowGraph.MAX_STRING_LENGTH)
		{
			log.error(pos, "too.large.string");
			nerrs++;
		}
	}

	/**
	 * The ret of expression.
	 */
	private Value exprResult = null;

	/**
	 * Generates IR for expression.
	 *
	 * @param expr
	 */
	private Value emitExpr(Tree expr)
	{
		expr.accept(this);
		return exprResult;
	}

	/**
	 * generates IR sequence for expression list.
	 *
	 * @param exprs
	 */
	private void emitExprList(List<Tree> exprs)
	{
		if (exprs == null || exprs.isEmpty())
			return;
		exprs.forEach(this::emitExpr);
	}

	/**
	 * Generates a jump instruction to be inserted into instructions list of a
	 * basic block.
	 *
	 * @param target The targetAbstractLayer of jump inst
	 */
	private void emitJump(BasicBlock target)
	{
		Instruction.Goto goto_ = new Instruction.Goto(target,
				Operator.Goto.opName);
		appendInst(goto_);

		currentBlock.addSucc(target);
		target.addPred(currentBlock);
	}

	/**
	 * Links the given block into cfg.
	 *
	 * @param currBB The curent being used basic block.
	 * @param nextBB The given basic block to be insert into cfg.
	 */
	private void startBasicBlock(BasicBlock currBB, BasicBlock nextBB)
	{
		nextBB.setCFG(currentCFG);
		currBB.addSucc(nextBB);
		nextBB.addPred(currBB);
		// updates current block
		currBB = nextBB;
	}

	/**
	 * Links the given block into cfg.
	 *
	 * @param nextBB The given basic block to be insert into cfg.
	 */
	private void startBasicBlock(BasicBlock nextBB)
	{
		nextBB.setCFG(currentCFG);
		currentBlock.addSucc(nextBB);
		nextBB.addPred(currentBlock);
		// updates current block
		currentBlock = nextBB;
	}

	private boolean constantFoldOnBool(Tree tree)
	{
		if (tree instanceof Literal
				&& ((Literal) tree).typetag == TypeClass.BOOL)
		{
			Boolean val = (Boolean) ((Literal) tree).value;
			// true
            // false
            return val;
		}
		return false;
	}

	/**
	 * Translates conditional branch.
	 * <p>
	 * <p>
	 * Emit a branch on a boolean condition (e.g. for an if statement) to
	 * the specified blocks. Based the condition, this might try to simplify
	 * the codegen of the conditional based on the branches.
	 * </p>
	 *
	 * @param expr    The relative expression.
	 * @param trueBB  the targetAbstractLayer block when condition is true.
	 * @param falseBB the targetAbstractLayer block when condition is false.
	 */
	private void translateBranchOnBool(Tree expr, BasicBlock trueBB,
			BasicBlock falseBB)
	{
		BasicBlock remainderBB;
		BinaryExpr bin;

		if ((expr.tag >= Tree.OR && expr.tag <= Tree.AND) || (
				expr.tag >= Tree.NE && expr.tag <= Tree.GE))
		{
			bin = (BinaryExpr) expr;
			if (bin.tag == Tree.AND)
			{
				// if we have "true && X", or "false && X", to simpify the code
				if (constantFoldOnBool(bin.lhs))
				{
					// br(true && X) => br(X)
					translateBranchOnBool(bin.rhs, trueBB, falseBB);
					return;
				}

				// if we have " X && true", or " X && false", to simpify the code
				if (constantFoldOnBool(bin.rhs))
				{
					// br(X && true) => br(X)
					translateBranchOnBool(bin.lhs, trueBB, falseBB);
					return;
				}

				remainderBB = currentCFG.createBasicBlock("and.lhs.true");
				translateBranchOnBool(TreeInfo.skipParens(bin.lhs), remainderBB,
						falseBB);
				startBasicBlock(remainderBB);
				// translates right hand side
				translateBranchOnBool(TreeInfo.skipParens(bin.rhs), trueBB,
						falseBB);
				return;
			}
			else if (bin.tag == Tree.OR)
			{
				// if we have "true || X", or "false || X", to simpify the code
				if (!constantFoldOnBool(bin.lhs))
				{
					// br(false || X) => br(X)
					translateBranchOnBool(bin.rhs, trueBB, falseBB);
					return;
				}

				// if we have " X || true", or " X || false", to simpify the code
				if (!constantFoldOnBool(bin.rhs))
				{
					// br(X || false) => br(X)
					translateBranchOnBool(bin.lhs, trueBB, falseBB);
					return;
				}

				remainderBB = currentCFG.createBasicBlock("or.lhs.false");
				translateBranchOnBool(TreeInfo.skipParens(bin.lhs), trueBB,
						remainderBB);
				startBasicBlock(remainderBB);
				// translates right hand side
				translateBranchOnBool(TreeInfo.skipParens(bin.rhs), trueBB,
						falseBB);
				return;
			}
			else
			{
				Condition[] conds = {
						Condition.EQ,
						Condition.NE,
						Condition.LT,
						Condition.LE,
						Condition.GT,
						Condition.GE };

				Condition cond = conds[expr.tag - Tree.EQ];
				Value x = emitExpr(bin.lhs);
				Value y = emitExpr(bin.rhs);

				currentBlock.addSucc(trueBB);
				currentBlock.addSucc(falseBB);
				trueBB.addPred(currentBlock);
				falseBB.addPred(currentBlock);

				// appends quad into current block
				appendInst(new IfOp(x, y, trueBB, falseBB, "IfOp", cond));
			}
		}
		else
		{
			switch (expr.tag)
			{
				case Tree.NOT:
					Tree inner_tree = TreeInfo.skipParens(((UnaryExpr) expr).arg);

					// !true or !false
					if (constantFoldOnBool(inner_tree))
					{
						// !true => false
						emitJump(falseBB);
					}
					else
					{
						// !false => true
						emitJump(trueBB);
					}
					translateBranchOnBool(inner_tree, falseBB, trueBB);
					return;
				case Tree.LITERAL:
					if (((Literal) expr).typetag == TypeClass.BOOL)
					{
						Boolean val = (Boolean) ((Literal) expr).value;
						if (val)
							emitJump(trueBB);
						else
							emitJump(falseBB);
					}
					else
						log.error(expr.pos, "not.boolean.type.at.condition");
					return;
				default:
					break;
			}
		}
	}

	private void emitIfCmpNE(Value lhs, Value rhs, BasicBlock trueBB,
			BasicBlock falseBB)
	{
		IfOp inst = new IfOp(lhs, rhs, trueBB, falseBB,
				"IfOp", Condition.NE);
		appendInst(inst);
		currentBlock.addSucc(trueBB);
		currentBlock.addSucc(falseBB);

		trueBB.addPred(currentBlock);
		falseBB.addPred(currentBlock);
	}

	private void emitIfCmpEQ(Value lhs, Value rhs, BasicBlock trueBB,
			BasicBlock falseBB)
	{
		IfOp inst = new IfOp(lhs, rhs, trueBB, falseBB,
				"IfOp", Condition.EQ);
		appendInst(inst);
		currentBlock.addSucc(trueBB);
		currentBlock.addSucc(falseBB);

		trueBB.addPred(currentBlock);
		falseBB.addPred(currentBlock);
	}

	private void emitIfCmpLE(Value lhs, Value rhs, BasicBlock trueBB,
			BasicBlock falseBB)
	{
		IfOp inst = new IfOp(lhs, rhs, trueBB, falseBB,
				"IfOp", Condition.LE);
		appendInst(inst);
		currentBlock.addSucc(trueBB);
		currentBlock.addSucc(falseBB);

		trueBB.addPred(currentBlock);
		falseBB.addPred(currentBlock);
	}

	private void emitIfCmpLT(Value lhs, Value rhs, BasicBlock trueBB,
			BasicBlock falseBB)
	{
		IfOp inst = new IfOp(lhs, rhs, trueBB, falseBB,
				"IfOp", Condition.LT);
		appendInst(inst);
		currentBlock.addSucc(trueBB);
		currentBlock.addSucc(falseBB);

		trueBB.addPred(currentBlock);
		falseBB.addPred(currentBlock);
	}

	private void emitIfCmpGT(Value lhs, Value rhs, BasicBlock trueBB,
			BasicBlock falseBB)
	{
		IfOp inst = new IfOp(lhs, rhs, trueBB, falseBB,
				"IfOp", Condition.GT);
		appendInst(inst);
		currentBlock.addSucc(trueBB);
		currentBlock.addSucc(falseBB);

		trueBB.addPred(currentBlock);
		falseBB.addPred(currentBlock);
	}

	private void emitIfCmpGE(Value lhs, Value rhs, BasicBlock trueBB,
			BasicBlock falseBB)
	{
		IfOp inst = new IfOp(lhs, rhs, trueBB, falseBB,
				"IfOp", Condition.GE);
		appendInst(inst);
		currentBlock.addSucc(trueBB);
		currentBlock.addSucc(falseBB);

		trueBB.addPred(currentBlock);
		falseBB.addPred(currentBlock);
	}

	/**
	 * %%%%%%<h2>This method is not implemented, currently.</h2> %%%%%%%
	 * Doing type cast.
	 *
	 * @param src   The source LIROperand.
	 * @param sType The source type of LIROperand
	 * @param dType The targetAbstractLayer type into that LIROperand will be casted.
	 * @return
	 */
	private Value emitCast(Value src, Type sType, Type dType)
	{
		/*
		// the original type
		int srccode = Module.typecode(sType);
		// the targetAbstractLayer type
		int targetcode = Module.typecode(dType);
		if (srccode == targetcode)
			return src;
		else
		{
			// the return inst of truncate method at least INTcode
			int typecode1 = Module.truncate(srccode);
			int targetcode2 = Module.truncate(targetcode);
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

	 private Local createTemp(Type t)
	 {
	 return new Local(type2Kind(t), tempNameGenerator.next());
	 }
	 */

	/**
	 * Translating the top level tree that the compilation unit. All of
	 * definitions will be handled rather than import statement and global
	 * variable declaration.
	 */
	@Override public void visitTopLevel(TopLevel tree)
	{
		for (Tree t : tree.defs)
		{
			// firstly, donn't handle import clause and global variable
			// definition
			if (t.tag == Tree.MethodDefStmtClass)
				t.accept(this);
		}
	}

	/**
	 * Saved current cfg of transforming method.
	 */
	private ControlFlowGraph currentCFG = null;
	private BasicBlock currentBlock = null;

	/**
	 * Two stack for parsing the targetAbstractLayer of break and continue statement
	 * respectively.
	 */
	private LinkedList<BasicBlock> continueStack, breakStack;

	private TempNameGenerator tempNameGenerator = TempNameGenerator.instance();

	/**
	 * Translates method definition into intermediate representation(IR).
	 */
	@Override public void visitMethodDef(MethodDef tree)
	{
		if (tree.body != null)
		{
			// initialize some variable for emitting Module of function.
			Function m = enterFunctionInit(tree);

			// initialize return value if this function have return
			if (m.signature().returnKind() != LIRKind.Void)
				emitReturnValue(m);

			// places actual parameter onto entry block
			for (Tree t : tree.params)
			{
				emitAllocaForVarDecl((VarDef) t);
			}
			// translate method body
			tree.body.accept(this);

			functionExit();

			// SSA construction and memory promotion.
			new EnterSSA(m);
			this.functions.add(m);
		}
	}

	private Function enterFunctionInit(MethodDef tree)
	{
		Function m = new Function(tree);
		currentCFG = new ControlFlowGraph(m);
		m.cfg = currentCFG;
		this.functions.add(m);

		// sets the current block with entry of a cfg with increment number id.
		this.currentBlock = currentCFG.createStartNode();

		this.continueStack = new LinkedList<>();
		this.breakStack = new LinkedList<>();

		// a generator for temporary variable.
		tempNameGenerator.init();

		currentCFG.entry().addSucc(currentBlock);
		currentBlock.addPred(currentCFG.entry());
		return m;
	}

	private void functionExit()
	{
		BasicBlock exit = currentCFG.createEndNode();
		// appends exit block into the successor list of current block.
		currentBlock.addSucc(exit);
		// appends current block into the predecessor list of exit block.
		exit.addPred(currentBlock);
	}

	/**
	 * This map that associates variable's name with it's allocation instruction
	 * for its alloca instruction according to name.
	 */
	private HashMap<Name, AllocaInst> NameValues = new HashMap<>();

	private AllocaInst lastAllocaInst = null;

	/**
	 * Allocate a local variable at execution stack of current function with
	 * instruction {@code AllocaInst}, all alloca instruction of a cfg are stores
	 * at the entry block of current control flow graph.
	 *
	 * @param kind The kind of data value to be allocated.
	 * @param var  The tree node that represents a local variable.
	 * @return An alloca instruction.
	 */
	private AllocaInst createEnterBlockAlloca(LIRKind kind, VarDef var)
	{
		AllocaInst inst = createEnterBlockAlloca(kind, var.name);
		// associate its local with variable symbol
		var.sym.varInst = inst;

		return inst;
	}

	/**
	 * Allocates memory for return vairable of specified method.
	 *
	 * @param m The handled method.
	 */
	private void emitReturnValue(Function m)
	{
		Name returnName = Name.fromString(names, "%retvalue");
		m.ReturnValue = createEnterBlockAlloca(m.signature().returnKind(),
				returnName);
	}

	private AllocaInst createEnterBlockAlloca(LIRKind kind, Name var)
	{
		BasicBlock entry = currentCFG.entry();
		AllocaInst inst = new AllocaInst(kind, Constant.forInt(1), Operator.Alloca.opName);

		// associate its local with variable symbol
		NameValues.put(var, inst);

		int index = entry.indexOf(lastAllocaInst);
		if (index < 0 || index == entry.size() - 1)
			entry.appendInst(inst);
		else
			entry.insertAt(inst, index + 1);

		inst.setParent(entry);

		lastAllocaInst = inst;
		return inst;
	}

	/**
	 * generates memory {@code AllocaInst} instruction and initial store for variable
	 * declaration.
	 *
	 * @param var The variable to be allocated.
	 */
	private void emitAllocaForVarDecl(VarDef var)
	{
		LIRKind varKind = type2Kind(var.type);

		// allocates stack slot for local variable definition
		AllocaInst inst = createEnterBlockAlloca(varKind, var);

		Value initValue;
		if (var.init != null)
		{
			checkStringConstant(var.init.pos, var.sym.constValue);
			// generates IR for initial expression.
			initValue = emitExpr(var.init);
		}
		// setting default inst for different inst type
		else
		{
			initValue = new Constant(LIRConstant.defaultValue(varKind));
		}
		// generats store instruction that stores initial inst into specified local
		emitStore(initValue, inst);
	}

	/**
	 * Converts {@code Type} into {@code LIRKind}.
	 *
	 * @param ty
	 * @return
	 */
	static LIRKind type2Kind(Type ty)
	{
		if (ty.isIntLike())
			return LIRKind.Int;
		else if (ty == Type.LONGType)
			return LIRKind.Long;
		else if (ty == Type.FLOATType)
			return LIRKind.Float;
		else if (ty == Type.DOUBLEType)
			return LIRKind.Double;
		else if (ty == Type.VOIDType)
			return LIRKind.Void;
		else
			return LIRKind.Illegal;
	}

	/**
	 * Allocates memory at the stack and initializing for variable declaration.
	 *
	 * @param tree Variable definition or declaration.
	 */
	@Override public void visitVarDef(VarDef tree)
	{
		emitAllocaForVarDecl(tree);
	}

	/**
	 * Translates program block surrounding with a pair of braces.
	 */
	@Override public void visitBlock(CompoundStmt tree)
	{
		for (Tree t : tree.stats)
			t.accept(this);
	}

	/**
	 * Translates expression statement.
	 */
	@Override public void visitExec(Exec tree)
	{
		if (tree.expr != null)
			tree.expr.accept(this);
	}

	/**
	 * Translates if statement.
	 * <p>
	 * if (subExpr) statement is translated into follow presentation if !subExpr goto
	 * <p>
	 * <pre>
	 * nextBB trueBB: ..... nextBB: .....
	 * </pre>
	 * <p>
	 * if (subExpr) stmt1 else stmt2 is translated into follow presentation
	 * <p>
	 * <pre>
	 * if !subExpr goto falseBB
	 * trueBB:
	 * 		.....
	 * falseBB:
	 * 		.....
	 * nextBB:
	 * 		.....
	 * </pre>
	 */
	@Override public void visitIf(IfStmt tree)
	{
		BasicBlock nextBB = currentCFG.createBasicBlock("if.end");
		BasicBlock trueBB = currentCFG.createBasicBlock( "if.true");
		if (tree.elsepart == null)
		{
			translateBranchOnBool(tree.cond, nextBB, trueBB);
			startBasicBlock(trueBB);
			tree.thenpart.accept(this);
		}
		else
		{
			BasicBlock falseBB = currentCFG.createBasicBlock("if.false");

			translateBranchOnBool(TreeInfo.skipParens(tree.cond), falseBB,
					trueBB);

			startBasicBlock(trueBB);
			tree.thenpart.accept(this);
			emitJump(nextBB);

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
	 * Translates while loop statement into IR. while (subExpr) stmt is translated
	 * into:
	 * <p>
	 * <pre>
	 * headerBB:
	 * if (!subExpr) goto nextBB
	 * loopBB:
	 *     stmt
	 * goto headerBB
	 *
	 * nextBB:
	 *     ...
	 * </pre>
	 */
	@Override public void visitWhileLoop(WhileStmt tree)
	{
		BasicBlock headerBB, loopBB, nextBB;
		headerBB = currentCFG.createBasicBlock("while.cond");
		loopBB = currentCFG.createBasicBlock("while.body");
		nextBB = currentCFG.createBasicBlock("while.exit");

		// add the targetAbstractLayer of break and continue into stack
		pushBreak(nextBB);
		pushContinue(headerBB);

		// translates condition
		startBasicBlock(headerBB);
		translateBranchOnBool(TreeInfo.skipParens(tree.cond), loopBB, nextBB);

		// translates loop body
		startBasicBlock(loopBB);
		tree.body.accept(this);

		// generates jump instruction from current block to headerB
		// this jump may be removed by optimized int the future
		emitJump(headerBB);

		popBreak();
		popContinue();

		startBasicBlock(nextBB);
	}

	/**
	 * Translates do while loop into IRs as follow:
	 * <p>
	 * <pre>
	 * loopBB:
	 *     stmt
	 * condBB:
	 *     if (subExpr) goto loopBB
	 * nextBB:
	 *     ...
	 * </pre>
	 */
	@Override public void visitDoLoop(DoStmt tree)
	{
		BasicBlock loopBB, condBB, nextBB;
		loopBB = currentCFG.createBasicBlock("do.body");
		condBB = currentCFG.createBasicBlock("do.cond");
		nextBB = currentCFG.createBasicBlock("do.exit");

		pushBreak(nextBB);
		pushContinue(condBB);

		// starts loopBB
		startBasicBlock(loopBB);
		tree.body.accept(this);

		// translates condtion
		startBasicBlock(condBB);
		translateBranchOnBool(tree.cond, loopBB, nextBB);

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
	@Override public void visitForLoop(ForStmt tree)
	{
		BasicBlock nextBB, condBB, loopBB;
		nextBB = currentCFG.createBasicBlock("for.exit");
		condBB = currentCFG.createBasicBlock("for.cond");
		loopBB = currentCFG.createBasicBlock("for.body");

		pushBreak(nextBB);
		pushContinue(condBB);

		// starts initial BB
		emitExprList(tree.init);

		// starts conditional BB
		startBasicBlock(condBB);
		translateBranchOnBool(tree.cond, loopBB, nextBB);

		// starts loopBB
		startBasicBlock(loopBB);
		tree.body.accept(this);
		emitExprList(tree.step);

		// generates jump for jumping to condition BB
		emitJump(condBB);

		popBreak();
		popContinue();

		// nextBB is here
		startBasicBlock(nextBB);
	}

	/**
	 * Translates goto statement into IR.
	 */
	@Override public void visitGoto(GotoStmt tree)
	{
		/* if the block corresponding to targetAbstractLayer of this stmt is null, creating a
		 * block to be associated with it. */
		if (tree.target.corrBB == null)
			tree.target.corrBB = currentCFG
					.createBasicBlock(tree.label.toString());
		emitJump(tree.target.corrBB);

		// starts a new basic block
		startBasicBlock(currentCFG.createBasicBlock(""));
	}

	/**
	 * Translates labelled statement.
	 */
	@Override public void visitLabelled(LabelledStmt tree)
	{
		/* if the block corresponding to labelled statement is null, creating a
		 * block to be associated with it. */
		if (tree.corrBB == null)
			tree.corrBB = currentCFG
					.createBasicBlock(tree.label.toString());
		startBasicBlock(tree.corrBB);
		tree.body.accept(this);
	}

	/**
	 * Translates break statement into IRs. A break statement terminates the
	 * execution of associated switch or loop.
	 * <p>
	 * <p>
	 * <pre>
	 * It is translated into:
	 * 		goto the nextBB of switch or loop.
	 * nextBB:
	 * </pre>
	 */
	@Override public void visitBreak(BreakStmt tree)
	{
		try
		{
			emitJump(getCurrrentBreakTarget());
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
	 * <p>
	 * <pre>
	 * It is translated into:
	 * 		goto the nextBB of switch or loop.
	 * nextBB:
	 * </pre>
	 */
	@Override public void visitContinue(ContinueStmt tree)
	{
		try
		{
			emitJump(getCurrentContinueTarget());
		}
		catch (Exception e)
		{
			log.error(tree.pos, "A.continue.not.in.loop");
		}
	}

	/**
	 * Translates return statement into IR.
	 *
	 * @param tree The return tree node.
	 */
	@Override public void visitReturn(ReturnStmt tree)
	{
		ReturnInst inst;
		if (tree.expr != null)
		{
			// emit the ret value even if unused, in order to the side effect.
			Value res = emitExpr(tree.expr);

			// stores the return value to specified memory.
			emitStore(res, currentCFG.getMethod().ReturnValue);
			inst = new ReturnInst(res, Operator.Ret.opName);
		}
		else
			inst = new ReturnInst(null, Operator.Ret.opName);

		appendInst(inst);
		// goto the exit of current method.
		emitJump(currentCFG.exit());
		startBasicBlock(currentCFG.createBasicBlock(""));
	}

	// the generated switch statement currently for nested switch.
	private SwitchInst switchInst;

	/**
	 * Currently, switch is not supported.
	 *
	 * @param tree The {@code SwitchStmt} expression node.
	 */
	@Override public void visitSwitch(SwitchStmt tree)
	{
		// handle nested switch statements.
		SwitchInst savedSwitchInst = this.switchInst;

		// the exit block of this switch statement.
		BasicBlock switchExit = currentCFG.createBasicBlock("sw.epilog");

		// At actually, the constant folding opt should be taken.
		// Yet it not implements that for I am lazy....~ ~.
		Value condV = emitExpr(tree.selector);

		// create a block to holds default case statement so that explicit case
		// ranges test can have a place to jump to on failure.
		BasicBlock defaultBlock = currentCFG
				.createBasicBlock("sw.default");

		// keeps the getArraySize of jump list of switch statement.
		int reserved = 0;
		for (CaseStmt ca : tree.cases)
			reserved += ca.values.size();

		this.switchInst = new SwitchInst(condV, defaultBlock, reserved,
				Operator.Switch.opName);

		// All break statement jump to exit block.
		pushBreak(switchExit);
		int idx = 0;
		HashMap<CaseStmt, BasicBlock> caseBlocks = new HashMap<>();
		// Especially handle default case at current
		for (CaseStmt clause : tree.cases)
		{
			// the default case for specially handling.
			if (clause.values == null)
			{
				// hanle default case clause
				startBasicBlock(currentBlock, defaultBlock);
				caseBlocks.put(clause, defaultBlock);

				emitDefaultCase(clause);
			}
			else
			{
				String postfix = idx > 0 ? String.valueOf(idx) : "";
				++idx;
				BasicBlock caseBB = currentCFG
						.createBasicBlock("sw.bb" + postfix);

				caseBlocks.put(clause, caseBB);

				startBasicBlock(currentBlock, caseBB);
				clause.accept(this);
			}
		}
		popBreak();

		handleSubsequnceCases(tree, caseBlocks);

		// Ifa default case was never emitted.
		if (defaultBlock.getCFG() == null)
		{
			// just emits default block so that there are a place to jump
			// when all cases are mismatch
			defaultBlock = switchExit;
			startBasicBlock(defaultBlock);
		}
		else
			startBasicBlock(switchExit);

		// restores saved switch instruction.
		this.switchInst = savedSwitchInst;
	}

	/**
	 * Builds pred-succ link between previous case clause and next clause,
	 * if current case clause no isDeclScope break statement as the last.
	 *
	 * @param tree       The switch statement.
	 * @param caseBlocks The map that maps case clause to correspondint block.
	 */
	private void handleSubsequnceCases(SwitchStmt tree,
			HashMap<CaseStmt, BasicBlock> caseBlocks)
	{
		for (int idx = 0; idx < tree.cases.size() - 1; idx++)
		{
			CaseStmt clause = tree.cases.get(idx);
			Tree lastStmt = null;
			CompoundStmt caseBlock = (CompoundStmt)clause.subStmt;
				
			lastStmt = caseBlock.stats.get(caseBlock.stats.size() - 1);	

			// the last statement of case clause is a break.
			// So that we should associate the basic block attached to
			// switch with the the predecessor of current case clause.
			if ((lastStmt != null) && !(lastStmt instanceof BreakStmt))
			{
				BasicBlock cur = caseBlocks.get(clause);
				BasicBlock next = caseBlocks.get(tree.cases.get(idx + 1));
				cur.addSucc(next);
				next.addPred(cur);
			}
		}
	}

	/**
	 * Emits Module for default case statement.
	 *
	 * @param tree The default case statement.
	 */
	private void emitDefaultCase(CaseStmt tree)
	{
		BasicBlock defaultBlock = this.switchInst.getDefaultBlock();
		assert defaultBlock
				!= null : "emitDefaultCase: default block already defined?";
		tree.subStmt.accept(this);
	}

	/**
	 * Currently, CaseStmt statement is not supported.
	 */
	@Override public void visitCase(CaseStmt tree)
	{
		for (Tree expr : tree.values)
		{
			Value RVal = emitExpr(expr);
			this.switchInst.addCase(RVal, currentBlock);
		}

		// branch to default or switch exit block.
		// when a break statement occures.
		tree.subStmt.accept(this);
	}

	/**
	 * Just ignores.
	 */
	@Override public void visitSkip(NullStmt tree)
	{
		// skip it.
	}

	/**
	 * Generates Module for function call.
	 *
	 * @param m    The targeted method.
	 * @param args The arguments list passed to callee.
	 * @return ReturnInst null if return type is void, otherwise, return value.
	 */
	private Value emitCall(Function m, Value[] args)
	{
		LIRKind ret = returnKind(m);
		InvokeInst inst = new InvokeInst(ret, args, m, Operator.Invoke.opName);

		appendInst(inst);
		inst.setParent(currentBlock);
		// return null if it's return type is void
		return ret == LIRKind.Void ? null : inst;
	}

	/**
	 * Handling function invocation expression.
	 *
	 * @param tree The invocation expression.
	 */
	@Override public void visitApply(CallExpr tree)
	{
		Value[] args = new Value[tree.args.size()];
		// translates actual parameter list
		int idx = 0;
		for (Tree para : tree.args)
			args[idx++] = emitExpr(para);

		Function m = (new Function((MethodDef) tree.fn));

		// emiterates calling expression
		this.exprResult = emitCall(m, args);
	}

	/**
	 * Gets the return kind of specified method.
	 *
	 * @param target The targeted method.
	 * @return The return kind.
	 */
	private LIRKind returnKind(Function target)
	{
		return target.signature().returnKind();
	}

	@Override public void visitParens(ParenExpr tree)
	{
		TreeInfo.skipParens(tree).accept(this);
	}

	/**
	 * Geneates phi node and inserts it into current block.
	 *
	 * @param kind   The ret kind.
	 * @param values The parameter array to be passed into PhiNode node.
	 * @param blocks The corresponding block array.
	 * @return A complete {@code PhiNode} instruction.
	 */
	private PhiNode emitPhi(LIRKind kind, Value[] values, BasicBlock[] blocks)
	{
		PhiNode phiNode = new PhiNode(kind, values, blocks, Operator.Phi.opName);
		appendInst(phiNode);
		phiNode.setParent(currentBlock);

		return phiNode;
	}

	/**
	 * <p>
	 * Translates conditional statement like
	 * </p>
	 * <p>
	 * <pre>
	 * (relation subExpr) ? subExpr : subExpr;
	 * </pre>
	 */
	@Override public void visitConditional(ConditionalExpr tree)
	{
		Value t1, t2;
		BasicBlock trueBB, falseBB, nextBB;
		trueBB = currentCFG.createBasicBlock("cond.true");
		falseBB = currentCFG.createBasicBlock("cond.false");
		nextBB = currentCFG.createBasicBlock("cond.end");

		translateBranchOnBool(tree.cond, trueBB, falseBB);

		// handles true portion
		startBasicBlock(trueBB);
		t1 = emitExpr(tree.truepart);
		if (t1 == null)
		{
			this.exprResult = null;
			return;
		}
		emitJump(nextBB);

		// handles the false part
		startBasicBlock(falseBB);
		t2 = emitExpr(tree.falsepart);
		if (t2 == null)
		{
			this.exprResult = null;
			return;
		}
		emitJump(nextBB);

		// starts next basic block
		startBasicBlock(nextBB);

		// sets the ret of this expression
		this.exprResult = emitPhi(t1.kind, new Value[] { t1, t2 },
				new BasicBlock[] { trueBB, falseBB });
	}

	/**
	 * Generates move instrcution.
	 *
	 * @param value The source of move, including all of instruction.
	 * @param dest  The targetAbstractLayer of move, which is variable, occasionally.
	 */
	private void emitStore(Value value, AllocaInst dest)
	{
		StoreInst inst = new StoreInst(value, dest, Operator.Store.opName);

		appendInst(inst);
		inst.setParent(currentBlock);
	}

	/**
	 * <p>
	 * Translates assignment statement into IRs, as depicted follow:
	 * </p>
	 *
	 * @param tree The assignment to be translated.
	 */
	@Override public void visitAssign(Tree.Assign tree)
	{
		Value rhs = emitExpr(tree.rhs);

		Value lhs = emitExpr(tree.lhs);
		if (rhs == null || lhs == null)
		{
			this.exprResult = null;
			return;
		}
		if (!(lhs instanceof AllocaInst) || lhs.name == null)
		{
			log.error(tree.pos, "destination of '=' must be a variable");
			this.exprResult = null;
			return;
		}
		AllocaInst alloca = NameValues.get(lhs.name);
		if (alloca == null)
		{
			log.error(tree.pos, "Unkonw variable name");
			this.exprResult = null;
			return;
		}
		// generates move instruction
		emitStore(rhs, (AllocaInst) lhs);
	}

	/**
	 * Translates assignment with operator.
	 *
	 * @param tree The tree to be transformed.
	 */
	@Override public void visitAssignop(OpAssign tree)
	{
		OperatorSymbol operator = (OperatorSymbol) tree.operator;
		if (operator.opcode == OpCodes.string_add)
		{
			/* currently, string concat with add operation is not supported it
			 * may be taken into consideration in the future. */
		}
		else
		{
			Value rhs = emitExpr(tree.rhs);

			Value lhs = emitExpr(tree.lhs);
			if (rhs == null || lhs == null)
			{
				this.exprResult = null;
				return;
			}
			if (!(lhs instanceof AllocaInst) || lhs.name == null)
			{
				log.error(tree.pos, "destination of '=' must be a variable");
				this.exprResult = null;
				return;
			}
			AllocaInst alloca = NameValues.get(lhs.name);
			if (alloca == null)
			{
				log.error(tree.pos, "Unkonw variable name");
				this.exprResult = null;
				return;
			}

			this.exprResult = transformAssignOp(tree.lhs.type, tree.pos,
					tree.tag, rhs, (AllocaInst) lhs);
		}
	}

	/**
	 * An auxiliary method for translates assignment with op into IRs.
	 *
	 * @param pos  The position of this expression.
	 * @param op   The opcode.
	 * @param src  The source.
	 * @param dest The destination.
	 * @return Result of this instruction.
	 */
	private Value transformAssignOp(Type ty, int pos, int op, Value src,
			AllocaInst dest)
	{
		emitStore(emitBin(ty, pos, op, dest, src), dest);
		return dest;
	}

	/**
	 * Generates an add instruction and inserts into basic block.
	 *
	 * @param ty  The data type of ret.
	 * @param pos The position to error report.
	 * @param lhs The left hand side of it.
	 * @param rhs The right hand side of it.
	 */
	private Value emitADD(Type ty, int pos, Value lhs, Value rhs)
	{
		Operator opcode = Operator.None;
		if (ty.isIntLike())
		{
			opcode = Operator.IAdd;
		}
		else if (ty.equals(Type.LONGType))
		{
			opcode = Operator.LAdd;
		}
		else if (ty.equals(Type.FLOATType))
		{
			opcode = Operator.FAdd;
		}
		else if (ty.equals(Type.DOUBLEType))
		{
			opcode = Operator.DAdd;
		}
		else
		{
			log.error(pos, "Invalid data type in the add IR.");
		}
		Value inst = new ArithmeticOp(type2Kind(ty), opcode, lhs, rhs,"");
		appendInst(inst);
		return inst;
	}

	/**
	 * Generates an sub instruction and inserts into basic block.
	 *
	 * @param ty  The data type of ret.
	 * @param pos The position to error report.
	 * @param lhs The left hand side of it.
	 * @param rhs The right hand side of it.
	 */
	private Value emitSUB(Type ty, int pos, Value lhs, Value rhs)
	{
		Operator opcode = Operator.None;
		if (ty.isIntLike())
		{
			opcode = Operator.ISub;
		}
		else if (ty.equals(Type.LONGType))
		{
			opcode = Operator.LSub;
		}
		else if (ty.equals(Type.FLOATType))
		{
			opcode = Operator.FSub;
		}
		else if (ty.equals(Type.DOUBLEType))
		{
			opcode = Operator.DSub;
		}
		else
		{
			log.error(pos, "Invalid data type in the subtract IR.");
		}
		Value inst = new ArithmeticOp(type2Kind(ty), opcode, lhs, rhs,"");
		appendInst(inst);
		return inst;
	}

	/**
	 * Generates an mul instruction and inserts into basic block.
	 *
	 * @param ty  The data type of ret.
	 * @param pos The position to error report.
	 * @param lhs The left hand side of it.
	 * @param rhs The right hand side of it.
	 */
	private Value emitMUL(Type ty, int pos, Value lhs, Value rhs)
	{
		Operator opcode = Operator.None;
		if (ty.isIntLike())
		{
			opcode = Operator.IMul;
		}
		else if (ty.equals(Type.LONGType))
		{
			opcode = Operator.LMul;
		}
		else if (ty.equals(Type.FLOATType))
		{
			opcode = Operator.FMul;
		}
		else if (ty.equals(Type.DOUBLEType))
		{
			opcode = Operator.DMul;
		}
		else
		{
			log.error(pos, "Invalid data type in the multiple IR.");
		}
		Value inst = new ArithmeticOp(type2Kind(ty), opcode, lhs, rhs,"");
		appendInst(inst);
		return inst;
	}

	/**
	 * Generates an div instruction and inserts into basic block.
	 *
	 * @param ty  The data type of ret.
	 * @param pos The position to error report.
	 * @param lhs The left hand side of it.
	 * @param rhs The right hand side of it.
	 */
	private Value emitDIV(Type ty, int pos, Value lhs, Value rhs)
	{
		Operator opcode = Operator.None;
		if (ty.isIntLike())
		{
			opcode = Operator.IDiv;
		}
		else if (ty.equals(Type.LONGType))
		{
			opcode = Operator.LDiv;
		}
		else if (ty.equals(Type.FLOATType))
		{
			opcode = Operator.FDiv;
		}
		else if (ty.equals(Type.DOUBLEType))
		{
			opcode = Operator.DDiv;
		}
		else
		{
			log.error(pos, "Invalid data type in the division IR.");
		}
		Value inst = new ArithmeticOp(type2Kind(ty), opcode, lhs, rhs,"");
		appendInst(inst);
		return inst;
	}

	/**
	 * Generates an mod instruction and inserts into basic block.
	 *
	 * @param ty  The data type of ret.
	 * @param pos The position to error report.
	 * @param lhs The left hand side of it.
	 * @param rhs The right hand side of it.
	 */
	private Value emitMOD(Type ty, int pos, Value lhs, Value rhs)
	{
		Operator opcode = Operator.None;
		if (ty.isIntLike())
		{
			opcode = Operator.IMod;
		}
		else if (ty.equals(Type.LONGType))
		{
			opcode = Operator.LMod;
		}
		else
		{
			log.error(pos, "Invalid data type in the mod IR.");
		}
		Value inst = new ArithmeticOp(type2Kind(ty), opcode, lhs, rhs,"");
		appendInst(inst);
		return inst;
	}

	/**
	 * Generates an bit-and instruction and inserts into basic block.
	 *
	 * @param ty  The data type of ret.
	 * @param pos The position to error report.
	 * @param lhs The left hand side of it.
	 * @param rhs The right hand side of it.
	 */
	private Value emitBITAND(Type ty, int pos, Value lhs, Value rhs)
	{
		Operator opcode = Operator.None;
		if (ty.isIntLike())
		{
			opcode = Operator.IAnd;
		}
		else if (ty.equals(Type.LONGType))
		{
			opcode = Operator.LAnd;
		}
		else
		{
			log.error(pos, "Invalid data type in the bitwise and IR.");
		}
		Value inst = new LogicOp(type2Kind(ty), opcode, lhs, rhs,"");
		appendInst(inst);
		return inst;
	}

	/**
	 * Generates an bit-and instruction and inserts into basic block.
	 *
	 * @param ty  The data type of ret.
	 * @param pos The position to error report.
	 * @param lhs The left hand side of it.
	 * @param rhs The right hand side of it.
	 */
	private Value emitBITOR(Type ty, int pos, Value lhs, Value rhs)
	{
		Operator opcode = Operator.None;
		if (ty.isIntLike())
		{
			opcode = Operator.IOr;
		}
		else if (ty.equals(Type.LONGType))
		{
			opcode = Operator.LOr;
		}
		else
		{
			log.error(pos, "Invalid data type in the bitwise or IR.");
		}
		Value inst = new LogicOp(type2Kind(ty), opcode, lhs, rhs,"");
		appendInst(inst);
		return inst;
	}

	/**
	 * Generates an bit-and instruction and inserts into basic block.
	 *
	 * @param ty  The data type of ret.
	 * @param pos The position to error report.
	 * @param lhs The left hand side of it.
	 * @param rhs The right hand side of it.
	 */
	private Value emitBITXOR(Type ty, int pos, Value lhs, Value rhs)
	{
		Operator opcode = Operator.None;
		if (ty.isIntLike())
		{
			opcode = Operator.IXor;
		}
		else if (ty.equals(Type.LONGType))
		{
			opcode = Operator.LXor;
		}
		else
		{
			log.error(pos, "Invalid data type in the bitwise xor IR.");
		}
		Value inst = new LogicOp(type2Kind(ty), opcode, lhs, rhs,"");
		appendInst(inst);
		return inst;
	}

	/**
	 * Generates an sheft left instruction and inserts into basic block.
	 *
	 * @param ty  The data type of ret.
	 * @param pos The position to error report.
	 * @param lhs The left hand side of it.
	 * @param rhs The right hand side of it.
	 */
	private Value emitSHL(Type ty, int pos, Value lhs, Value rhs)
	{
		Operator opcode = Operator.None;
		if (ty.isIntLike())
		{
			opcode = Operator.IShl;
		}
		else if (ty.equals(Type.LONGType))
		{
			opcode = Operator.LShl;
		}
		else
		{
			log.error(pos, "Invalid data type in the shift left IR.");
		}
		Value inst = new ShiftOp(type2Kind(ty), opcode, lhs, rhs,"");
		appendInst(inst);
		return inst;
	}

	/**
	 * Generates an sheft rigth instruction and inserts into basic block.
	 *
	 * @param ty  The data type of ret.
	 * @param pos The position to error report.
	 * @param lhs The left hand side of it.
	 * @param rhs The right hand side of it.
	 */
	private Value emitSHR(Type ty, int pos, Value lhs, Value rhs)
	{
		Operator opcode = Operator.None;
		if (ty.isIntLike())
		{
			opcode = Operator.IShr;
		}
		else if (ty.equals(Type.LONGType))
		{
			opcode = Operator.LShr;
		}
		else
		{
			log.error(pos, "Invalid data type in the shift right IR.");
		}
		Value inst = new ShiftOp(type2Kind(ty), opcode, lhs, rhs,"");
		appendInst(inst);
		return inst;
	}

	/**
	 * translates binary operation expression into IRs.
	 *
	 * @param pos The position to error report.
	 * @param op  The operator code.
	 * @param lhs The left hand side of it.
	 * @param rhs The right hand side of it.
	 * @return The ret of this operation.
	 */
	private Value emitBin(Type ty, int pos, int op, Value lhs, Value rhs)
	{
		Value res = null;
		switch (op)
		{
			case Tree.PLUS:
				res = emitADD(ty, pos, lhs, rhs);
				break;
			case Tree.MINUS:
				res = emitSUB(ty, pos, lhs, rhs);
				break;
			case Tree.MUL:
				res = emitMUL(ty, pos, lhs, rhs);
				break;
			case Tree.DIV:
				res = emitDIV(ty, pos, lhs, rhs);
				break;
			case Tree.MOD:
				res = emitMOD(ty, pos, lhs, rhs);
				break;
			case Tree.BITAND:
				res = emitBITAND(ty, pos, lhs, rhs);
				break;
			case Tree.BITOR:
				res = emitBITOR(ty, pos, lhs, rhs);
				break;
			case Tree.BITXOR:
				res = emitBITXOR(ty, pos, lhs, rhs);
				break;
			case Tree.SL:
				res = emitSHL(ty, pos, lhs, rhs);
				break;
			case Tree.SR:
				res = emitSHR(ty, pos, lhs, rhs);
				break;
			default:
				log.error(pos,
						"Invalid binary operation at abstract syntax tree");
		}
		return res;
	}

	/**
	 * Generate intermedicate code to calculate a branch expression's inst.
	 * <pre> e.g. int a, b; a = a > b;
	 * Introduces a new temporary t to holds the inst of a > b.
	 * %1 = load %a;
	 * %2 = load %b;
	 * %tmp = ICmp gt %1, %2;
	 * store %tmp, %a;
	 * </pre>
	 */
	private Value translateRelative(BinaryExpr tree)
	{
		Value rhs = emitExpr(tree.rhs);
		Value lhs = emitExpr(tree.lhs);
		Value result = null;
		Condition[] conds = {
				Condition.EQ,
				Condition.NE,
				Condition.LT,
				Condition.LE,
				Condition.GT,
				Condition.GE
		};
		if (tree.tag < Tree.EQ || tree.tag > Tree.GE)
		{
			log.error(tree.pos,
					"Invalid comparison operation at abstract syntax tree");
			return null;
		}
		return new Cmp(type2Kind(tree.type), lhs,rhs, conds[tree.tag - Tree.EQ],"");
	}

	/**
	 * Generate intermedicate code to calculate a logical expression's inst.
	 * <pre> e.g. int a, b; a = a && b;
	 * Introduces a new temporary t to holds the inst of a > b.
	 * <p>
	 * </pre>
	 */
	private Value translateLogicalExpression(BinaryExpr expr)
	{
		BasicBlock nextBB, rhsBB;
		Value rhsResult = null;
		nextBB = currentCFG.createBasicBlock("");
		rhsBB = currentCFG.createBasicBlock("");

		// saves the entry for current context.
		BasicBlock entry = currentBlock;

		// firstly, translating left hand LIROperand
		Value lhs = emitExpr(expr.lhs);
		if (lhs == null)
		{
			return null;
		}
		Constant zero = new Constant(
				LIRConstant.defaultValue(lhs.kind));
		Constant one = new Constant(LIRConstant.getOne(lhs.kind));

		switch (expr.tag)
		{
			case Tree.AND:
				rhsBB.bbName = "and.rhs";
				nextBB.bbName = "and.end";
				// branch
				appendInst(new IfOp(lhs, zero, rhsBB, nextBB,
						"IfGT", Condition.GT));

				// translate right hand side
				startBasicBlock(rhsBB);
				Value rhs = emitExpr(expr.rhs);
				if (rhs == null)
					return null;

				rhsResult = new Cmp(type2Kind(expr.type), lhs, zero, Condition.GT,"GT");

				appendInst(rhsResult);

				Instruction.Goto go = new Instruction.Goto(nextBB,
						Operator.Goto.opName);
				appendInst(go);

				startBasicBlock(nextBB);
				// phiNode
				PhiNode phiNode = new PhiNode(lhs.kind, 2);
				phiNode.addIncoming(zero, entry);
				phiNode.addIncoming(rhsResult, rhsBB);
				return phiNode;

			case Tree.OR:
				rhsBB.bbName = "or.rhs";
				nextBB.bbName = "or.end";

				// branch
				appendInst(new IfOp(lhs, zero, nextBB, rhsBB,
						"IfGT", Condition.GT));

				// translate right hand side
				startBasicBlock(rhsBB);
				rhs = emitExpr(expr.rhs);
				if (rhs == null)
					return null;

				rhsResult = new Cmp(type2Kind(expr.type), rhs, zero, Condition.GT,"GT");

				appendInst(rhsResult);

				go = new Instruction.Goto(nextBB, Operator.Goto.opName);

				appendInst(go);

				startBasicBlock(nextBB);
				// phiNode
				phiNode = new PhiNode(lhs.kind, 2);
				phiNode.addIncoming(one, entry);
				phiNode.addIncoming(rhsResult, rhsBB);
				return phiNode;
			default:
				return null;
		}
	}

	@Override public void visitBinary(BinaryExpr tree)
	{
		if (tree.tag == Tree.OR || tree.tag == Tree.AND)
		{
			this.exprResult = translateLogicalExpression(tree);
			return;
		}
		if (tree.tag >= Tree.EQ && tree.tag <= Tree.GE)
		{
			this.exprResult = translateRelative(tree);
			return;
		}

		Value rhs = emitExpr(tree.rhs);
		Value lhs = emitExpr(tree.lhs);

		this.exprResult = emitBin(tree.type, tree.pos, tree.tag, lhs, rhs);
	}

	private Value translateIncrement(UnaryExpr expr)
	{
		Value res = emitExpr(expr);
		if (res == null || res.name == null)
		{
			log.error(expr.pos, "The left hand of '=' must be a left-value");
			return null;
		}
		Value temp = res.clone();
		Value ret = null;
		Value incre = null;
		switch (expr.tag)
		{
			case Tree.PREDEC:
				incre = emitBin(expr.type, expr.pos, Tree.MINUS, res,
						new Constant(LIRConstant.INT_1));
				ret = incre;
				break;
			case Tree.PREINC:
				incre = emitBin(expr.type, expr.pos, Tree.PLUS, res,
						new Constant(LIRConstant.INT_1));
				ret = incre;
				break;
			case Tree.POSTDEC:
				incre = emitBin(expr.type, expr.pos, Tree.MINUS, res,
						new Constant(LIRConstant.INT_1));
				ret = temp;
				break;
			case Tree.POSTINC:
				incre = emitBin(expr.type, expr.pos, Tree.PLUS, res,
						new Constant(LIRConstant.INT_1));
				ret = temp;
				break;
			default:
				return null;
		}
		AllocaInst addr = NameValues.get(res.name);
		if (addr == null)
		{
			log.error(expr.pos, "Unknow variable name " + res.name.toString());
			return null;
		}
		// store decrement ret into targetAbstractLayer address
		emitStore(incre, addr);
		return ret;
	}

	private Value translateNotExpression(UnaryExpr expr)
	{
		Value res = emitExpr(expr.arg);
		if (res == null)
		{
			return null;
		}
		Constant zero = new Constant(
				LIRConstant.defaultValue(res.kind));
		Cmp cmp = new Cmp(type2Kind(expr.arg.type), res, zero, Condition.LE,"LE");
		appendInst(cmp);
		return cmp;
	}

	/**
	 * Translates unary operation expression.
	 *
	 * @param tree The expression to be translated.
	 */
	@Override public void visitUnary(UnaryExpr tree)
	{
		// firstly, prefix operation is handled
		if (tree.tag == Tree.NOT)
		{
			this.exprResult = translateNotExpression(tree);
			return;
		}
		if (tree.tag >= Tree.PREINC && tree.tag <= Tree.PREDEC)
		{
			this.exprResult = translateIncrement(tree);
			return;
		}
		Value operand1 = emitExpr(tree.arg);
		switch (tree.tag)
		{
			case Tree.ImplicitCast:
				this.exprResult = emitCast(operand1, tree.type, tree.arg.type);
				break;
			case Tree.NEG:

				break;
			case Tree.COMPL:

				break;
			case Tree.ArraySubscriptExprClass:
				this.exprResult = emitExpr(tree.arg);
				break;
			case Tree.CallExprClass:
				this.exprResult = emitExpr(tree.arg);
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
	 *
	 * @param tree
	 */
	@Override public void visitIndexed(ArraySubscriptExpr tree)
	{

	}

	/**
	 * Translates type cast expression.
	 *
	 * @param tree
	 */
	@Override public void visitTypeCast(CastExpr tree)
	{
		Value res = emitExpr(tree.expr);
		this.exprResult = emitCast(res, tree.expr.type, tree.clazz.type);
	}

	@Override public void visitLiteral(Literal tree)
	{
		if (tree.typetag == TypeClass.Int)
		{
			this.exprResult = new Constant(new LIRConstant(LIRKind.Int,
					((Integer) tree.value).intValue()));
		}
		else if (tree.typetag == TypeClass.LongInteger)
		{
			this.exprResult = new Constant(new LIRConstant(LIRKind.Long,
					((Long) tree.value).longValue()));
		}
		else if (tree.typetag == TypeClass.FLOAT)
		{
			this.exprResult = new Constant(new LIRConstant(LIRKind.Float,
					((Float) tree.value).longValue()));
		}
		else if (tree.typetag == TypeClass.DOUBLE)
		{
			this.exprResult = new Constant(new LIRConstant(LIRKind.Double,
					((Double) tree.value).longValue()));
		}
		else if (tree.typetag == TypeClass.BOOL)
		{
			this.exprResult = ((Boolean) tree.value).equals("true") ?
					Constant.forInt(1) :
					Constant.forInt(0);
		}
	}

	/**
	 * Generates loading instruction for local or global instruction that loads
	 * a variable into a temporary virtual variable.
	 *
	 * @param src The source instruction that will be loaded into targetAbstractLayer.
	 * @return ReturnInst the {@code LoadInst} that load value from src
	 */
	private Value emitLoadInstruction(AllocaInst src)
	{
		LoadInst inst = new LoadInst(src.kind, src, Operator.Load.opName);

		appendInst(inst);
		return inst;
	}

	/**
	 * Translates variable reference.
	 *
	 * @param tree The variable identifier.
	 */
	@Override public void visitIdent(DeclRefExpr tree)
	{
		// parses variable
		if (tree.sym.kind == SymbolKinds.VAR)
		{
			VarSymbol sym = (VarSymbol) tree.sym;
			if (sym.varInst == null)
			{
				log.error(tree.pos, "Unkown variable name");
				this.exprResult = null;
				return;
			}
			this.exprResult = emitLoadInstruction(sym.varInst);
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
	@Override public void visitErroneous(ErroneousTree erroneous)
	{
		log.error(erroneous.pos, "A.errorous.tree");
		this.exprResult = null;
	}

	/**
	 * A Class served as generator for yielding the name of a temporary variable.
	 * <p>
	 * All temporary name generated by this class is described as follow:
	 * $1, $2, $3.
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
		 *
		 * @return The concrete instance of this class.
		 */
		public static TempNameGenerator instance()
		{
			if (instance == null)
				instance = new TempNameGenerator();
			return instance;
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
		 *
		 * @return The next index.
		 */
		public String next()
		{
			String postfix = "$" + idx;
			idx++;
			return postfix;
		}
	}
}
