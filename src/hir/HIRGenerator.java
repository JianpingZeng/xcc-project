package hir;

import hir.Operand.*;
import hir.Quad.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.xml.transform.Templates;

import type.Type;
import utils.ErrorHandler;
import entity.DefinedFunction;
import entity.DefinedVariable;
import entity.LocalScope;
import exception.JumpError;
import exception.SemanticError;
import exception.SemanticException;
import ast.*;

public class HIRGenerator implements ASTVisitor {

	private ErrorHandler errorHandler = null;

	/**
	 * Constructor.
	 * 
	 * @param errorHandler
	 */
	public HIRGenerator(ErrorHandler errorHandler) {

		this.errorHandler = errorHandler;
	}

	/**
	 * Generates linear IR for all of defined function.
	 * 
	 * @param ast
	 *            A ast instantiation.
	 * @return A HIR.
	 */
	public HIR generate(AST ast) {
		for (DefinedFunction fun : ast.definedFunctions()) {
			List<Quad> quads = compileFunctionBody(fun);
			fun.setCFG(ControlFlowGraph.computeCFG(quads));
		}
		return ASTToHIR.convert(ast);
	}

	LinkedList<Quad.Label> breakStack = null;
	LinkedList<Quad.Label> continueStack = null;
	LinkedList<LocalScope> scopeStack = null;

	List<Quad> quads = null;

	/** For the id number of quad in basic block. */
	IndexGenerator indexer = new IndexGenerator();

	/** Compiles a function body and translates into a list of Quad. */
	private List<Quad> compileFunctionBody(DefinedFunction function) {

		// re-initialize per this method be called.
		quads = new ArrayList<>();
		breakStack = new LinkedList<Quad.Label>();
		continueStack = new LinkedList<Quad.Label>();
		scopeStack = new LinkedList<>();
		labeledMap = new HashMap<>();
		transformStmt(function.body());
		return quads;
	}

	/** Transforms a statement into a list of quads. */
	private void transformStmt(StmtNode stmt) {
		stmt.accept(this);
	}

	/**
	 * A interface method that transforming statement into IR.
	 * 
	 * @param node
	 */
	private void transformStmt(ExprNode node) {
		node.accept(this);
	}

	private int exprNestLevel = 0;

	/**
	 * Interface that transforms expression into IR.
	 * 
	 * @param node
	 * @return
	 */
	private Operand transformExpr(ExprNode node) {
		exprNestLevel++;
		node.accept(this);
		exprNestLevel--;
		return result;
	}

	/**
	 * Emitting a labeled instruction into quads list.
	 * 
	 * @param label
	 *            A targeted label.
	 */
	private void label(Quad.Label label) {
		quads.add(label);
	}

	@Override
	public void visit(BlockNode node) {

		// add a local scope represents function body into scopeStack.
		scopeStack.add(node.scope());
		for (StmtNode stmt : node.stmts()) {
			transformStmt(stmt);
		}

		scopeStack.removeLast();
	}

	@Override
	public void visit(ExprStmtNode node) {

		node.expr().accept(this);
	}

	LinkedList<CondItem> condStack = new LinkedList<>();

	private CondItem genCond(ExprNode node) {

		node.accept(this);
		return condStack.isEmpty() ? null : condStack.removeLast();
	}

	LinkedList<NextInstItem> nextStack = new LinkedList<>();

	private NextInstItem genNextInstChain(StmtNode node) {

		node.accept(this);
		return nextStack.isEmpty() ? null : nextStack.removeLast();
	}


	public void visit(IfNode node) {

		// translates condition of if statement.
		CondItem cond = genCond(node.cond());

		if (node.elseBody() == null) {
			Quad.Label thenLabel = new Quad.Label(indexer.nextIndex());
			quads.add(thenLabel);

			NextInstItem next = genNextInstChain(node.thenBody());

			Quad.Label endLabel = new Quad.Label(indexer.nextIndex());
			quads.add(endLabel);

			if (cond != null) {
				resovleCondChain(cond.trueLists, thenLabel);
				resovleCondChain(cond.falseLists, endLabel);
			}
			if (next != null) {
				next.mergeNextList(cond.falseLists);
				nextStack.addLast(next);
			}

		} else {
			Quad.Label thenLabel = new Quad.Label(indexer.nextIndex());
			Quad.Label elseLabel = new Quad.Label(indexer.nextIndex());
			Quad.Label endLabel = new Quad.Label(indexer.nextIndex());

			// for then part.
			quads.add(thenLabel);

			NextInstItem end1 = genNextInstChain(node.thenBody());
			// infer that it is endThen
			quads.add(new Quad.Goto(indexer.nextIndex(), endLabel));

			// for else part
			quads.add(elseLabel);

			NextInstItem end2 = genNextInstChain(node.elseBody());

			quads.add(endLabel);

			if (cond != null) {
				resovleCondChain(cond.trueLists, thenLabel);
				resovleCondChain(cond.falseLists, elseLabel);
			}
			if (end1 != null) {
				resovleCondChain(end1.nextLists, endLabel);
			}
			if (end1 != null && end2 != null) {
				end1.mergeNextList(end2.nextLists);
				nextStack.addLast(end1);
			}
		}
	}

	@Override
	public void visit(SwitchNode node) {
		// a hashmap used to establish a jump entityTable.
		HashMap<Integer, Label> jumpTable = new HashMap<>();
		ArrayList<Label> labelList = new ArrayList<>();	
		
		
		// translates the expression of switch statement.
		Operand res = transformExpr(node.cond());
		
		for (CaseNode c : node.cases()) {
						
		}
	}

	@Override
	public void visit(CaseNode node) {
		error(node, "Single case clauses is illegal.");
	}

	@Override
	public void visit(WhileNode node) {
		
		Quad.Label beginLabel = new Quad.Label(indexer.nextIndex());
		Quad.Label bodyLabel = new Quad.Label(indexer.nextIndex());
		Quad.Label endLabel = new Quad.Label(indexer.nextIndex());
				
		quads.add(beginLabel);
		
		breakStack.addLast(endLabel);
		continueStack.add(beginLabel);
		// translates condition of if statement.
		CondItem cond = genCond(node.cond());
			
		quads.add(bodyLabel);
		
		node.body().accept(this);
		
		quads.add(new Quad.Goto(indexer.nextIndex(), beginLabel));		

		quads.add(endLabel);
		
		breakStack.removeLast();
		continueStack.removeLast();
		
		if (cond != null)
		{
			resovleCondChain(cond.trueLists, bodyLabel);
			resovleCondChain(cond.falseLists, endLabel);
		}
	}


	public void visit(DoWhileNode node) {
		
		Quad.Label beginLabel = new Quad.Label(indexer.nextIndex());
		Quad.Label condLabel = new Quad.Label(indexer.nextIndex());
		Quad.Label endLabel = new Quad.Label(indexer.nextIndex());
		
		quads.add(beginLabel);
		
		breakStack.addLast(endLabel);
		continueStack.addLast(condLabel);
		
		node.body().accept(this);
		quads.add(condLabel);
		
		breakStack.removeLast();
		continueStack.removeLast();
		
		// translates condition of if statement.
		CondItem cond = genCond(node.cond());		
		quads.add(endLabel);
		
		if (cond != null)
		{
			resovleCondChain(cond.trueLists, beginLabel);
			resovleCondChain(cond.falseLists, endLabel);
		}
	}

	
	public void visit(ForNode node) {
		
		Quad.Label condLabel = new Quad.Label(indexer.nextIndex());
		Quad.Label bodyLabel = new Quad.Label(indexer.nextIndex());
		Quad.Label endLabel = new Quad.Label(indexer.nextIndex());
		
		transformStmt(node.init());
		quads.add(condLabel);
		
		// translates condition of if statement.
		CondItem cond = genCond(node.cond());
		
		quads.add(bodyLabel);
		
		breakStack.addLast(endLabel);
		continueStack.addLast(condLabel);
		
		transformStmt(node.body());
		transformStmt(node.incr());
		quads.add(new Quad.Goto(indexer.nextIndex(), condLabel));
		
		breakStack.removeLast();
		continueStack.removeLast();
		
		quads.add(endLabel);
		
		if (cond != null){
			resovleCondChain(cond.falseLists, endLabel);
			resovleCondChain(cond.trueLists, bodyLabel);
		}
	}

	/**
	 * Gets the target of current break.	
	 * @return
	 */
	private Quad.Label currentBreakTarget()
	{
		if (breakStack.isEmpty())
		{
			throw new JumpError("break from out of loop");
		}
		return breakStack.getLast();
	}
	
	/**
	 * Gets the target of current continue statement.	
	 * @return
	 */
	private Quad.Label currentContinueTarget()
	{
		if (continueStack.isEmpty())
		{
			throw new JumpError("continue from out of loop");
		}
		return continueStack.getLast();
	}

	public void visit(BreakNode node) {
		
		try {
			
			quads.add(new Quad.Goto(indexer.nextIndex(), currentBreakTarget()));
		}
		catch (JumpError err)
		{
			error(node, err.getMessage());
		}
	}


	public void visit(ContinueNode node) {

		try {
			
			quads.add(new Quad.Goto(indexer.nextIndex(), currentContinueTarget()));
		}
		catch (JumpError err)
		{
			error(node, err.getMessage());
		}
	}


	public void visit(GotoNode node) {

		Quad.Label target = labeledMap.get(node.target());
		if (target != null)
			quads.add(new Quad.Goto(indexer.nextIndex(), target));
		else 
		{
			error(node, "A goto statement without target.");
		}
	}

	// records all of label statement at current function.
	HashMap<String, Quad.Label> labeledMap; 

	public void visit(LabelNode node) {
		
		Quad.Label target = new Quad.Label(indexer.nextIndex());
		labeledMap.put(node.name() , target);
		quads.add(target);
	}

	@Override
	public void visit(ReturnNode node) {
		
		if (node.expr() != null)
		{
			Operand res = transformExpr(node.expr());
			switch (res.t.size()) {
			case 1:
			case 2:
			case 4:
				quads.add(new Quad.IRet(indexer.nextIndex(), res));
				break;
			case 8:
				quads.add(new Quad.LRet(indexer.nextIndex(), res));
				break;
			default:
				error(node, "Returned value overflow of return statement.");
				break;
			}			
		}
		else 
		{
			quads.add(new Quad.Ret(indexer.nextIndex()));			
		}
	}

	private boolean isStatement()
	{
		return (exprNestLevel == 0);
	}

	public void visit(AssignNode node) {
		Position lloc = node.lhs().location();
		Position rloc = node.rhs().location();
		if (isStatement())
		{
			// Evaluate RHS before LHS.
			Operand rhs = transformExpr(node.rhs());
			Operand lhs = transformExpr(node.lhs());		
			quads.add(new Quad.Assign(indexer.nextIndex(), rhs, lhs));
		}
		else
		{
			// lhs = rhs -> tmp = rhs, lhs = tmp, tmp
			DefinedVariable tmp = scopeStack.getLast().allocateTmp(node.rhs().type());
			Operand t1 = new Operand.Var(tmp.name(), HIRType(tmp.type()));
			
			Operand rhs = transformExpr(node.rhs());
			quads.add(new Quad.Assign(indexer.nextIndex(), rhs, t1));
			Operand lhs = transformExpr(node.lhs());
			quads.add(new Quad.Assign(indexer.nextIndex(), t1, lhs));
			this.result = t1;			
		}
		
	}


	public void visit(OpAssignNode node) {

		// Evaluate RHS before LHS.
		Operand rhs = transformExpr(node.rhs());
		Operand lhs = transformExpr(node.lhs());
		String op = node.operator();
		
		try {
			this.result = transformOpAssign(op, node.type(), lhs, rhs);
		}catch (SemanticException ex) 
		{
			error(node,ex.getMessage());
		}
	}

	private Operand transformOpAssign(String op, Type t, Operand lhs, Operand rhs) 
			throws SemanticException {
		if (lhs.isVar()) {
			// cont(lhs += rhs) -> lhs = lhs + rhs; cont(lhs)
		 	Operand res = performBinOp(op, t, lhs, rhs);		 	
			quads.add(new Quad.Assign(indexer.nextIndex(), res, lhs));
			return isStatement() ? null : lhs;
		}
		else {
			return null;
		}			
	}
	
	@Override
	public void visit(CondExprNode node) {

		Quad.Label thenLabel = new Quad.Label(indexer.nextIndex());
		Quad.Label elseLabel = new Quad.Label(indexer.nextIndex());
		Quad.Label endLabel = new Quad.Label(indexer.nextIndex());

		DefinedVariable tmp = scopeStack.getLast().allocateTmp(node.type());
		Operand t1 = new Operand.Var(tmp.name(), HIRType(node.type()));
		
		// translates condition of if statement.
		CondItem c = genCond(node.cond());

		// for then part.
		quads.add(thenLabel);
		t1 = transformExpr(node.thenExpr());			
		quads.add(new Quad.Goto(indexer.nextIndex(), endLabel));
		
		// for else part
		quads.add(elseLabel);

		t1 = transformExpr(node.elseExpr());

		quads.add(endLabel);

		if (c != null) {
			resovleCondChain(c.trueLists, thenLabel);
			resovleCondChain(c.falseLists, elseLabel);
		}
	}

	/** Backpatching the given chains using target label. */

	private void resovleCondChain(LinkedList<Branch> chains, Quad.Label target) {

		for (Branch inst : chains) {
			inst.target = target;
		}
	}

	public void visit(LogicalOrNode node) {

		Quad.Label trueLabel = new Quad.Label(indexer.nextIndex());
		// translates left expression
		node.left().accept(this);

		// the label infers that left is true.
		label(trueLabel);
		CondItem c1 = condStack.removeLast();

		// backpatching for trueChian of left expr.
		if (c1 != null)
			resovleCondChain(c1.trueLists, trueLabel);

		// translates right expr.
		node.right().accept(this);

		// gets the CondItem generating for right expression
		CondItem c2 = condStack.removeLast();

		// sets the truelist and falselist
		// B.trueList = B2.trueList
		// B.falseList = B1.falseList || B2.falseList
		if (c2 != null) {
			c1.setFalseJumps(c2.falseLists);
			c1.mergeTrueLists(c2.trueLists);
			condStack.addLast(c1);
		}
		// for GC ?
		c2 = null;
	}

	public void visit(LogicalAndNode node) {

		Quad.Label trueLabel = new Quad.Label(indexer.nextIndex());
		// translates left expression
		node.left().accept(this);

		// the label infers that left is true.
		label(trueLabel);
		CondItem c1 = condStack.removeLast();

		// backpatching for trueChian of left expr.
		if (c1 != null)
			resovleCondChain(c1.trueLists, trueLabel);

		// translates right expr.
		node.right().accept(this);

		// gets the CondItem generating for right expression
		CondItem c2 = condStack.removeLast();

		// sets the truelist and falselist
		// B.trueList = B2.trueList
		// B.falseList = B1.falseList || B2.falseList
		if (c2 != null) {
			c1.setTrueJumps(c2.trueLists);
			c1.mergeFalseLists(c2.falseLists);
			condStack.addLast(c1);
		}
		// for GC ?
		c2 = null;
	}

	private Operand result;

	/**
	 * Visits binary operator node.
	 * 
	 * @param node
	 * @return
	 */
	public void visit(BinaryOpNode node) {
		Operand right = transformExpr(node.right());
		Operand left = transformExpr(node.left());

		Type t = node.type();
		Type r = node.right().type();
		Type l = node.left().type();

		// two pointer subtract
		String op = node.operator();
		if (isPointerDiff(op, l, r)) {

			// ptr - ptr -> (ptr - ptr) / ptrBaseSize
			DefinedVariable var = scopeStack.getLast().allocateTmp(t);
			Operand temp = new Var(var.name(), HIRType(var.type()));

			// adds a sub instruction into quads list.
			quads.add(new ISub(indexer.nextIndex(), left, right, temp));

			// the difference divide into size of base type,
			// and then result is offset of two pointer in storage size.
			quads.add(new IDiv(indexer.nextIndex(), temp, ptrBaseSize(t), temp));
			result = temp;
		} else if (isPointerArithmetic(op, l)) {

			// ptr + int -> ptr + (int * ptrBaseSize)
			DefinedVariable var = scopeStack.getLast().allocateTmp(t);
			Operand t1 = new Var(var.name(), HIRType(var.type()));
			quads.add(new IMul(indexer.nextIndex(), right, ptrBaseSize(t), t1));

			DefinedVariable var2 = scopeStack.getLast().allocateTmp(t);
			Operand t2 = new Var(var2.name(), HIRType(var2.type()));
			quads.add(new IAdd(indexer.nextIndex(), left, t1, t2));
			result = t2;
		} else if (isPointerArithmetic(op, r)) {
			// int + ptr -> (int * ptrBaseSize) + ptr

			DefinedVariable var = scopeStack.getLast().allocateTmp(t);
			Operand t1 = new Var(var.name(), HIRType(var.type()));
			quads.add(new IMul(indexer.nextIndex(), left, ptrBaseSize(t), t1));

			DefinedVariable var2 = scopeStack.getLast().allocateTmp(t);
			Operand t2 = new Var(var2.name(), HIRType(var2.type()));
			quads.add(new IAdd(indexer.nextIndex(), t1, right, t2));
			result = t2;
		}

		// handles relation operator.
		else if (op == ">" || op == ">=" || op == "<" || op == "<="
				|| op == "==" || op == "!=" || op == "<>") {

			performRelOp(op, left, right);
		} else {
			// like int + int
			try {
				result = performBinOp(node.operator(), node.type(), left, right);
			} catch (SemanticException e) {
				e.printStackTrace();
			}
		}
	}

	private void performRelOp(String op, Operand left, Operand right) {

		CondItem c = new CondItem();
		Branch cmp = null;
		switch (op) {
		case "<":
			cmp = new Quad.IfCmpLT(indexer.nextIndex(), left, right, null);
			break;
		case "<=":
			cmp = new Quad.IfCmpLE(indexer.nextIndex(), left, right, null);
			break;
		case ">":
			cmp = new Quad.IfCmpGT(indexer.nextIndex(), left, right, null);
			break;
		case ">=":
			cmp = new Quad.IfCmpGE(indexer.nextIndex(), left, right, null);
			break;
		case "==":
			cmp = new Quad.IfCmpEQ(indexer.nextIndex(), left, right, null);
			break;
		case "!=":
		case "<>":
			cmp = new Quad.IfCmpNE(indexer.nextIndex(), left, right, null);
			break;
		default:
			throw new SemanticError("Ilegal relational operatior.");
		}
		Branch GOTO = new Quad.Goto(indexer.nextIndex(), null);
		if (cmp != null)
			c.trueLists.add(cmp);
		c.falseLists.add(GOTO);

		condStack.addLast(c);
	}

	private Operand performBinOp(String op, Type t, Operand left, Operand right)
			throws SemanticException {

		DefinedVariable var = scopeStack.getLast().allocateTmp(t);
		Operand t1 = new Var(var.name(), HIRType(var.type()));
		switch (op) {
		case "+":
			quads.add(new IAdd(indexer.nextIndex(), left, right, t1));
			break;
		case "-":
			quads.add(new ISub(indexer.nextIndex(), left, right, t1));
			break;
		case "*":
			quads.add(new IMul(indexer.nextIndex(), left, right, t1));
			break;
		case "/":
			quads.add(new IDiv(indexer.nextIndex(), left, right, t1));
			break;

		case "%":
			quads.add(new IMod(indexer.nextIndex(), left, right, t1));
			break;
		case "&":
			quads.add(new BitAnd(indexer.nextIndex(), left, right, t1));
			break;
		case "|":
			quads.add(new BitOR(indexer.nextIndex(), left, right, t1));
			break;
		case "^":
			quads.add(new BitXOR(indexer.nextIndex(), left, right, t1));
			break;

		case "<<":
			quads.add(new LShift(indexer.nextIndex(), left, right, t1));
			break;
		case ">>":
			quads.add(new RShift(indexer.nextIndex(), left, right, t1));
			break;
		default:
			throw new SemanticError("Illegal operator");
		}

		return t1;
	}

	public void visit(UnaryOpNode node) {

		if (node.operator().equals("+")) {
			// +expr -> expr
			result = transformExpr(node.expr());
		} else {
			result = performUnary(node.operator(), node.expr());
		}
	}

	private Operand performUnary(String op, ExprNode expr) {

		DefinedVariable var = scopeStack.getLast().allocateTmp(expr.type());
		Var t1 = new Var(var.name(), HIRType(var.type()));
		result = transformExpr(expr);

		switch (op) {
		case "-":
			quads.add(new Quad.IMinus(indexer.nextIndex(), result, t1));
			break;
		case "~":
			quads.add(new Quad.IBitNot(indexer.nextIndex(), result, t1));
			break;
		case "!":
			CondItem c = condStack.getLast();
			LinkedList<Branch> temp = c.falseLists;
			c.falseLists = c.trueLists;
			c.trueLists = temp;
			t1 = null;
			break;
		default:
			break;
		}
		return t1;
	}

	@Override
	public void visit(PrefixOpNode node) {

	}

	@Override
	public void visit(SuffixOpNode node) {

	}

	@Override
	public void visit(ArefNode node) {

	}

	@Override
	public void visit(MemberNode node) {

	}

	@Override
	public void visit(PtrMemberNode node) {

	}

	@Override
	public void visit(FuncallNode node) {

	}

	@Override
	public void visit(DereferenceNode node) {

	}

	@Override
	public void visit(AddressNode node) {

	}

	@Override
	public void visit(CastNode node) {

	}

	@Override
	public void visit(SizeofExprNode node) {

	}

	@Override
	public void visit(SizeofTypeNode node) {

	}

	@Override
	public void visit(VariableNode node) {

	}

	@Override
	public void visit(IntegerLiteralNode node) {

	}

	@Override
	public void visit(StringLiteralNode node) {

	}

	//
	// Utilities
	//

	private boolean isPointerDiff(String op, Type l, Type r) {
		return op == "-" && l.isPointer() && r.isPointer();
	}

	private hir.type.Type HIRType(Type t) {

		if (t.isVoid())
			return int_t();
		return hir.type.Type.get(t.size());
	}

	private hir.type.Type int_t() {
		return hir.type.Type.get(4);
	}

	private Operand ptrBaseSize(Type t) {
		return new IConstOperand((int) t.baseType().size(), ptrdiff_t());
	}

	/**
	 * Obtains a pointer type with width Long integer.
	 * 
	 * @return
	 */
	private hir.type.Type ptrdiff_t() {
		return hir.type.Type.get(8);
	}

	private boolean isPointerArithmetic(String op, Type operandType) {
		switch (op) {
		case "+":
		case "-":
			return operandType.isPointer();
		default:
			return false;
		}
	}

	private void error(Tree n, String msg)
	{
		errorHandler.error(n.location(), msg);
	}
	
	/**
	 * An item represents a conditional or unconditional jump.
	 */
	class CondItem {

		/**
		 * A chain consists of all jumps that can be taken if the condition
		 * evaluates to true.
		 */
		LinkedList<Branch> trueLists;

		/**
		 * A chain consists of all jumps that can be taken if the condition
		 * evaluates to false.
		 */
		LinkedList<Branch> falseLists;

		public CondItem() {
			this.trueLists = new LinkedList<Branch>();
			this.falseLists = new LinkedList<Branch>();
		}

		/** Sets the true chains. */
		public void setTrueJumps(LinkedList<Branch> jumpsList) {
			this.trueLists = jumpsList;
		}

		/** Sets the false chains. */
		public void setFalseJumps(LinkedList<Branch> jumpsList) {
			this.falseLists = jumpsList;
		}

		/** Merges a specified jump list into this trueChains. */
		public void mergeTrueLists(LinkedList<Branch> lists) {

			this.trueLists.addAll(lists);
		}

		/** Merges a specified jump list into this falseChains. */
		public void mergeFalseLists(LinkedList<Branch> lists) {

			this.falseLists.addAll(lists);
		}
	}

	class NextInstItem {

		/**
		 * A chain consists of all jumps to next instruction.
		 */
		LinkedList<Branch> nextLists;

		public NextInstItem() {
			this.nextLists = new LinkedList<>();
		}

		/** Sets the next instructions chain. */
		public void setNextList(LinkedList<Branch> jumpLists) {

			this.nextLists = jumpLists;
		}

		/** Merges a specified jump list into this nextChains. */
		public void mergeNextList(LinkedList<Branch> jumpLists) {

			this.nextLists.addAll(jumpLists);
		}
	}
}
