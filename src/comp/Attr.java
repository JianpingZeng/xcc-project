package comp;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import symbol.Scope;
import symbol.Symbol;
import symbol.Symbol.MethodSymbol;
import symbol.Symbol.OperatorSymbol;
import symbol.SymbolKinds;
import symbol.VarSymbol;
import type.ConstantArrayType;
import type.FunctionType;
import type.Type;
import type.TypeClass;
import utils.Context;
import utils.Log;
import utils.Name;
import utils.Position;
import ast.ASTVisitor;
import ast.Flags;
import ast.Tree;
import ast.Tree.CallExpr;
import ast.Tree.Assign;
import ast.Tree.OpAssign;
import ast.Tree.CompoundStmt;
import ast.Tree.CaseStmt;
import ast.Tree.DoStmt;
import ast.Tree.ErroneousTree;
import ast.Tree.Exec;
import ast.Tree.GotoStmt;
import ast.Tree.IfStmt;
import ast.Tree.Import;
import ast.Tree.ArraySubscriptExpr;
import ast.Tree.LabelledStmt;
import ast.Tree.Literal;
import ast.Tree.MethodDef;
import ast.Tree.NewArray;
import ast.Tree.ReturnStmt;
import ast.Tree.NullStmt;
import ast.Tree.SwitchStmt;
import ast.Tree.TopLevel;
import ast.Tree.TypeArray;
import ast.Tree.CastExpr;
import ast.Tree.TypeIdent;
import ast.Tree.UnaryExpr;
import ast.Tree.VarDef;
import exception.CompletionFailure;

/**
 * This is main context-dependent analysis phase in compiler internal. It
 * encompasses type checking and constant folding as sub task. Some subtask
 * involve auxiliary classes.
 * <p>
 *
 * @author JianpingZeng
 * @version 1.0
 * @see Check
 * @see ConstFold
 * @see Enter
 */
public class Attr extends ASTVisitor implements SymbolKinds, TypeClass, Flags
{
	private static final Context.Key attrKey = new Context.Key();

	/**
	 * Current expected proto-type
	 */
	private Type pt;

	/**
	 * current expected proto-kind
	 */
	private int pkind;

	private Env env;

	private Type prept;

	private int prepkind;

	private Env preenv;
	private Log log;

	@SuppressWarnings("unused") private Name.Table names;

	private Check check;
	private Context context;
	private Enter enter;
	private ConstFold cfold;
	private Resolve rs;
	private Symtab syms;
	private TreeInfo treeinfo;

	public static Attr instance(Context context)
	{
		Attr instance = (Attr) context.get(attrKey);
		if (instance == null)
			instance = new Attr(context);
		return instance;
	}

	public Attr(Context context)
	{
		super();
		this.context = context;
		this.context.put(attrKey, this);
		this.log = Log.instance(context);
		this.names = Name.Table.instance(context);
		this.enter = Enter.instance(context);
		this.cfold = ConstFold.instance(context);
		this.rs = Resolve.instance(context);
		this.syms = Symtab.instance(context);
		this.treeinfo = TreeInfo.instance(context);
		methTemplateSupply = new ArrayDeque<>(10);
	}

	/**
	 * Main method: attributes method definition associated with given method
	 * symbol. Reporting completion failure error at given position.
	 *
	 * @param pos   The position encoded to be error report.
	 * @param sym   The method symbol.
	 */
	public void attriMethod(int pos, MethodSymbol sym)
	{
		try
		{
			attriMethod(sym);
		}
		catch (CompletionFailure ex)
		{
			check.completionError(pos, ex);
		}
	}

	public void attriMethod(MethodSymbol sym)
	{
		if (sym.type.tag == Error)
			return;

		// no finished !!!!!! in the future
		// check.checkNonCyclic(Position.NOPOS, sym.type);
		if ((sym.flags & UNATTRIBUTED) != 0)
		{
			sym.flags &= ~UNATTRIBUTED;
			Env env = enter.methodEnvs.remove(sym);
			Name prev = log.useSource(env.toplevel.sourceFile);
			try
			{
				attribMethodBody(env, sym);
			}
			finally
			{
				log.useSource(prev);
			}
		}
	}

	private void attribMethodBody(Env env, MethodSymbol m)
	{
		MethodDef tree = (MethodDef) env.tree;
		tree.type = m.type;
		attribStat(tree.body, env);
	}

	/**
	 * Attribute a type tree.
	 *
	 * @param tree
	 * @return
	 */
	public Type attribType(Tree tree, Env env)
	{
		return attribTree(tree, env, TYP, Type.noType);
	}

	/**
	 * Derived visitor method: attribute an expression tree.
	 */
	Type attribExpr(Tree tree, Env env, Type pt)
	{
		return attribTree(tree, env, VAL, pt);
	}

	/**
	 * Derived visitor method: attribute an expression tree with no constraints
	 * on the computed type.
	 */
	Type attribExpr(Tree tree, Env env)
	{
		return attribTree(tree, env, VAL, Type.noType);
	}

	/**
	 * Derived visitor method: attribute a statement or definition tree.
	 */
	Type attribStat(Tree tree, Env env)
	{
		return attribTree(tree, env, NIL, Type.noType);
	}

	/**
	 * Attribute a list of expressions, returning a list of types.
	 */
	List<Type> attribExprs(List<Tree> trees, Env env, Type pt)
	{
		List<Type> ts = new ArrayList<Type>();
		for (Tree tree : trees)
			ts.add(attribExpr(tree, env, pt));
		return ts;
	}

	/**
	 * Attribute a list of statements, returning nothing.
	 */
	void attribStats(List<Tree> trees, Env env)
	{
		for (Tree tree : trees)
			attribStat(tree, env);
	}

	/**
	 * Attribute the arguments in a method call, returning a list of types.
	 */
	List<Type> attribArgs(List<Tree> trees, Env env)
	{
		List<Type> argtypes = new ArrayList<Type>();
		for (Tree tree : trees)
		{
			argtypes.add(check.checkNonVoid(tree.pos,
					attribTree(tree, env, VAL, Infer.anyPoly)));
		}
		return argtypes;
	}

	private Type result = null;

	/**
	 * Attributes a tree, catching possible completion failure exception. ReturnInst
	 * the tree's type.
	 *
	 * @param tree      The tree to be visited
	 * @param env       The environment visitor argument.
	 * @param protoKind The proto-kind of visitor argument.
	 * @param protoType The proto-type of visitor argument.
	 * @return
	 */
	public Type attribTree(Tree tree, Env env, int protoKind, Type protoType)
	{
		this.prepkind = this.pkind;
		this.prept = this.pt;
		this.preenv = env;
		try
		{
			this.pkind = protoKind;
			this.pt = protoType;
			tree.accept(this);
			return result;
		}
		catch (CompletionFailure e)
		{
			tree.type = new Type.ErrorType(syms.errSymbol);
			return check.completionError(tree.pos, e);
		}
		finally
		{
			this.pt = this.prept;
			this.pkind = this.prepkind;
			this.env = this.preenv;
		}
	}

	/**
	 * Evaluates a const variable's initializer, unless this has already been
	 * finished, and set variable's constant value, if the initializer is
	 * constant.
	 */
	private void evaluateInit(VarSymbol v)
	{
		AttrContextEnv evalEnv = (AttrContextEnv) v.constValue;
		v.constValue = null;
		Type itype = attribExpr(((VarDef) evalEnv.info).init, evalEnv, v.type);
		if (itype.constValue != null)
		{
			v.constValue = v.constValue = cfold
					.coerce(itype, v.type).constValue;
		}
	}

	/**
	 * Optimization: To avoid allocating a new methodtype for every attribution
	 * of an CallExpr node, we use a reservoir.
	 */
	ArrayDeque<FunctionType> methTemplateSupply;

	/**
	 * Obtain an otherwise unused method type with given argument types. Take it
	 * from the reservoir if non-empty, or create a new one.
	 */
	Type newMethTemplate(List<Type> argtypes)
	{
		if (methTemplateSupply.isEmpty())
			methTemplateSupply.add(new FunctionType(null, null, null));

		// obtains the first element cached
		FunctionType mt = methTemplateSupply.poll();
		mt.paramTypes = argtypes;
		return mt;
	}

	/**
	 * Checks kind and type of given tree against proto-kind and proto-type. IfStmt
	 * checks succeeds, stores type in tree and return it. Otherwise fails,
	 * store errType in tree and return it.
	 * <p>
	 * No checks performed if the proto-type is a method type. It is not
	 * neccesary in this case since we know that kind and type are correct.
	 *
	 * @param tree       The tree whose kind and type is checked
	 * @param owntype    The computed type of this tree.
	 * @param ownkind    The computed kind of this tree.
	 * @param expectKind The expected kind of this tree.
	 * @param expectType The expected type of this tree.
	 * @return
	 */
	private Type check(Tree tree, Type owntype, int ownkind, int expectKind,
			Type expectType)
	{
		if (owntype.tag != Error && expectType.tag != Function)
		{
			if ((ownkind & ~expectKind) == 0)
			{
				owntype = check.checkType(tree.pos, owntype, expectType);
			}
			else
			{
				log.error(tree.pos, "unexpected.type",
						Resolve.kindNames(expectKind),
						Resolve.kindName(ownkind));
				owntype = new Type.ErrorType(syms.errSymbol);
			}
		}
		tree.type = owntype;
		return owntype;
	}

	/**
	 * Determine type of identifier or select expression and check that (1) the
	 * referenced symbol is not deprecated (2) the symbol's type is safe (@see
	 * checkSafe) (3) if symbol is a variable, check that its type and kind are
	 * compatible with the prototype and protokind. (4) if symbol is an instance
	 * field of a raw type, which is being assigned to, issue an unchecked
	 * warning if its type changes under erasure. (5) if symbol is an instance
	 * method of a raw type, issue an unchecked warning if its argument types
	 * change under erasure. IfStmt checks succeed: IfStmt symbol is a constant, return
	 * its constant type else if symbol is a method, return its getReturnValue type
	 * otherwise return its type. Otherwise return errType.
	 *
	 * @param tree  The syntax tree representing the identifier
	 * @param sym   The symbol representing the identifier.
	 * @param env   The current environment.
	 * @param pkind The set of expected kinds.
	 * @param pt    The expected type.
	 */
	Type checkId(Tree tree, Symbol sym, Env env, int pkind, Type pt)
	{
		Type owntype;
		switch (sym.kind)
		{
			case VAR:
				VarSymbol v = (VarSymbol) sym;
				owntype = sym.type;
				if (v.constValue != null)
					owntype = owntype.constType(v.constValue);
				break;

			case ERR:
				owntype = sym.type;
				break;

			default:
				throw new AssertionError(
						"unexpected kind: " + sym.kind + " in tree " + tree);

		}

		return check(tree, owntype, sym.kind, pkind, pt);
	}

	/**
	 * Check that variable is initialized and evaluate the variable's
	 * initializer, if not yet done. Also check that variable is not referenced
	 * before it is defined.
	 *
	 * @param tree The tree making up the variable reference.
	 * @param env  The current environment.
	 * @param v    The variable's symbol.
	 */
	private void checkInit(Tree.DeclRefExpr tree, Env env, VarSymbol v)
	{
		if (v.pos > tree.pos && canOwnInitializer(v)
				&& ((v.flags & STATIC) != 0) == Resolve.isStatic(env) && (
				env.tree.tag != Tree.AssignExprOperator
						|| TreeInfo.skipParens(((Assign) env.tree).lhs)
						!= tree))
			log.error(tree.pos, "illegal.forward.ref");
		evaluateInit(v);
	}

	/**
	 * Can the given symbol be the owner of code which forms part if class
	 * initialization? This is the case if the symbol is a type or field, or if
	 * the symbol is the synthetic method. owning a block.
	 */
	private boolean canOwnInitializer(Symbol sym)
	{
		return (sym.kind & VAR) != 0;
	}

	@Override public void visitTopLevel(TopLevel tree)
	{

	}

	@Override public void visitImport(Import tree)
	{

	}

	@Override public void visitMethodDef(MethodDef tree)
	{
		Env localEnv = enter.methodEnv(tree, env);
		MethodSymbol m = tree.sym;
		// check override when it is desired in the future
		for (Tree var : tree.params)
			attribStat(var, localEnv);
		if (tree.body != null)
		{
			attribStat(tree.body, localEnv);
		}
		((AttrContext) localEnv.info).scope.leave();
		result = tree.type = m.type;
	}

	@Override public void visitVarDef(VarDef tree)
	{
		VarSymbol v = tree.sym;
		if (tree.init != null)
		{
			v.pos = Position.MAXPOS;
			if ((tree.flags & CONST) != 0)
				evaluateInit(v);
			else
			{
				Type itype = attribExpr(tree.init,
						enter.initEnv(tree, env), v.type);
			}
			v.pos = tree.pos;
		}
		result = tree.type = v.type;
	}

	@Override public void visitSkip(NullStmt tree)
	{
		result = null;
	}

	@Override public void visitBlock(CompoundStmt tree)
	{
		Env localEnv = env.dup(tree, ((AttrContext) env.info)
				.dup(((AttrContext) env.info).scope.dup()));

		attribStats(tree.stats, localEnv);
		((AttrContext) localEnv.info).scope.leave();
		result = null;
	}

	@Override public void visitIf(IfStmt tree)
	{
		attribExpr(tree.cond, env, new Type(BOOL, null));
		attribExpr(tree.thenpart, env, pt);
		if (tree.elsepart != null)
		{
			attribExpr(tree.elsepart, env, pt);
		}
		result = null;
	}

	@Override public void visitSwitch(SwitchStmt tree)
	{
		Type seltype = attribExpr(tree.selector, env, syms.intType);
		Env switchEnv = env.dup(tree, ((AttrContext) env.info)
				.dup(((AttrContext) env.info).scope.dup()));

		Set labels = new HashSet();
		boolean hasDefault = false;
		for (CaseStmt cc : tree.cases)
		{
			Env caseEnv = switchEnv.dup(cc, ((AttrContext) env.info)
					.dup(((AttrContext) switchEnv.info).scope.dup()));

			if (cc.values != null)
			{
				Type pattype = attribExpr(cc.subStmt, caseEnv, syms.intType);
				if (pattype.tag == Error)
				{
					if (pattype.constValue == null)
						log.error(cc.pos, "const.expression.required");
					else if (labels.contains(pattype.constValue))
						log.error(cc.pos, "duplicate.case.label");
					else
					{
						check.checkType(cc.pos, pattype, seltype);
						labels.add(pattype.constValue);
					}
				}

			}
			else if (hasDefault)
			{
				log.error(cc.pos, "duplicate.default.label");
			}
			else
			{
				hasDefault = true;
			}
			attribStat(cc.subStmt, caseEnv);
			((AttrContext) caseEnv.info).scope.leave();
			addVars((CompoundStmt) cc.subStmt, ((AttrContext) switchEnv.info).scope);
		}
		((AttrContext) switchEnv.info).scope.leave();
		result = null;
	}

	/**
	 * Add any variable defined in subStmt into switch scope.
	 *
	 * @param caseBody
	 * @param switchScope
	 */
	private void addVars(CompoundStmt caseBody, Scope switchScope)
	{
		for (Tree tree : caseBody.stats)
		{
			if (tree.tag == Tree.VarDefStmtClass)
				switchScope.enter(((VarDef) tree).sym);
		}
	}

	private final Type boolType = new Type(BOOL, null);

	@Override public void visitForLoop(Tree.ForStmt tree)
	{
		Env loopEnv = env.dup(env.tree, ((AttrContext) env.info)
				.dup(((AttrContext) env.info).scope.dup()));

		if (tree.init != null && !tree.init.isEmpty())
			attribStats(tree.init, loopEnv);
		if (tree.cond != null)
			attribExpr(tree.cond, loopEnv, boolType);
		if (tree.step != null && !tree.step.isEmpty())
			attribStats(tree.step, loopEnv);
		((AttrContext) loopEnv.info).scope.leave();
		result = null;
	}

	@Override public void visitBreak(Tree.BreakStmt tree)
	{
		tree.target = findJumpTarget(tree.pos, tree.tag, null, env);
		result = null;
	}

	@Override public void visitContinue(Tree.ContinueStmt tree)
	{
		tree.target = findJumpTarget(tree.pos, tree.tag, null, env);
		result = null;
	}

	/**
	 * Returns the jump targetAbstractLayer of break, goto and continue statement, if exists,
	 * report error if not. Note that the targetAbstractLayer of a goto or break , continue
	 * is the non-labelled statement tree referred to by the label.
	 *
	 * @param pos
	 * @param tag
	 * @param env
	 * @return
	 */
	private LabelledStmt findJumpTarget(int pos, int tag, Name label, Env env)
	{
		Env env1 = env;
		LOOP:
		while (env1 != null)
		{
			switch (env1.tree.tag)
			{
				case Tree.LabelledStmtClass:
					Tree.LabelledStmt labelled = (LabelledStmt) env1.tree;
					if (label == labelled.label)
					{
						return labelled;
					}
					break;
				case Tree.DoStmtClass:
				case Tree.WhileStmtClass:
				case Tree.ForStmtClass:
					if (label == null)
						return (Tree.LabelledStmt) env1.tree;
					break;
				case Tree.SwitchStmtClass:
					if (label == null && tag == Tree.BreakStmtClass)
						return (LabelledStmt) env1.tree;
					break;
				case Tree.MethodDefStmtClass:
					break LOOP;
				default:
					break;
			}
			env1 = env1.next;
		}
		if (label != null)
			log.error(pos, "undef.label", label.toString());
		else if (tag == Tree.ContinueStmtClass)
			log.error(pos, "continue.outside.loop");
		else
			log.error(pos, "break.outside.loop");
		return null;
	}

	@Override public void visitGoto(GotoStmt tree)
	{
		tree.target = findJumpTarget(tree.pos, tree.tag, tree.label, env);
		result = null;
	}

	@Override public void visitDoLoop(DoStmt tree)
	{
		attribStat(tree.body, env.dup(tree));
		attribExpr(tree.cond, env, boolType);
		result = null;

	}

	@Override public void visitWhileLoop(Tree.WhileStmt tree)
	{
		attribExpr(tree.cond, env, boolType);
		attribStat(tree.body, env.dup(tree));
		result = null;
	}

	@Override public void visitCase(CaseStmt tree)
	{
		visitTree(tree);
	}

	@Override public void visitLabelled(LabelledStmt tree)
	{
		Env env1 = env;
		while (env1 != null && env1.tree.tag != Tree.MethodDefStmtClass)
		{
			if (env1.tree.tag == Tree.LabelledStmtClass
					&& ((LabelledStmt) env1.tree).label == tree.label)
			{
				log.error(tree.pos, "label.already.in.use",
						tree.label.toString());
				break;
			}
			env1 = env1.next;
		}
		attribStat(tree.body, env.dup(tree));
		result = null;
	}

	@Override public void visitReturn(ReturnStmt tree)
	{
		if (env.enclMethod == null)
			log.error(tree.pos, "return.outside.method.definition");
		else
		{
			Symbol m = env.enclMethod.sym;
			if (m.type.returnType().tag == Void)
			{
				if (tree.expr != null)
					log.error(tree.expr.pos,
							"cannot.return.value.from.void.method");
			}
			else if (tree.expr == null)
			{
				log.error(tree.pos, "missing.return.value");
			}
			else
			{
				attribExpr(tree.expr, env, m.type.returnType());
			}
		}
		result = null;
	}

	@Override public void visitSelect(Tree.SelectStmt tree)
	{
		visitTree(tree);
	}

	/**
	 * Visitor method for method invocation.
	 *
	 * @param tree
	 */
	@Override public void visitApply(CallExpr tree)
	{
		Env localEnv = env;
		Name calleeName = TreeInfo.name(tree.fn);
		List<Type> argtypes = attribArgs(tree.args, localEnv);
		Type mpt = newMethTemplate(argtypes);
		Type mtype = attribExpr(tree.fn, localEnv, mpt);
		// restores used FunctionType instance
		this.methTemplateSupply.add((FunctionType) mtype);

		result = check(tree, mtype.returnType(), VAL, pkind, pt);
	}

	@Override public void visitAssign(Assign tree)
	{
		Type owntype = attribTree(tree.lhs, env.dup(tree), VAR, pt);
		attribExpr(tree.rhs, env, owntype);
		result = check(tree, owntype, VAL, pkind, pt);
	}

	@Override public void visitAssignop(OpAssign tree)
	{
		List<Type> argtypes = Arrays.asList(attribTree(tree.lhs, env, VAR, Type.noType),
                attribExpr(tree.rhs, env));

		Symbol operator = rs
				.resolveOperator(tree.pos, tree.tag - Tree.ASGOffset, env,
						argtypes);
		tree.operator = (OperatorSymbol) operator;
		Type owntype = argtypes.get(0);
		if (operator.kind == MTH)
		{
			if (owntype.tag <= DOUBLE)
				check.checkCastable(tree.rhs.pos, operator.type.returnType(),
						owntype);
			else
				check.checkType(tree.rhs.pos, operator.type.returnType(),
						owntype);
		}
		result = check(tree, owntype, VAL, pkind, pt);
	}

	@Override public void visitExec(Exec tree)
	{
		attribExpr(tree.expr, env);
		result = null;

	}

	@Override public void visitConditional(Tree.ConditionalExpr tree)
	{
		attribExpr(tree.cond, env, syms.boolType);
		attribExpr(tree.truepart, env, pt);
		attribExpr(tree.falsepart, env, pt);
		result = check(tree,
				condType(tree.pos, tree.cond.type, tree.truepart.type,
						tree.falsepart.type), VAL, pkind, pt);
	}

	/**
	 * Compute the type of a conditional expression, after
	 * checking that it exists. See Spec 15.25.
	 *
	 * @param pos      The source position to be used for error diagnostics.
	 * @param condtype The type of the expression's condition.
	 * @param thentype The type of the expression's then-part.
	 * @param elsetype The type of the expression's else-part.
	 */
	private Type condType(int pos, Type condtype, Type thentype, Type elsetype)
	{
		Type ctype = condType1(pos, condtype, thentype, elsetype);
		return ((condtype.constValue != null) && (thentype.constValue != null)
				&&
				(elsetype.constValue != null)) ?
				cfold.coerce((((Number) condtype.constValue).intValue() != 0) ?
								thentype :
								elsetype, ctype) :
				ctype;
	}

	/**
	 * Compute the type of a conditional expression, after
	 * checking that it exists.  Does not take into
	 * account the special case where condition and both arms are constants.
	 *
	 * @param pos      The source position to be used for error diagnostics.
	 * @param condtype The type of the expression's condition.
	 * @param thentype The type of the expression's then-part.
	 * @param elsetype The type of the expression's else-part.
	 */
	private Type condType1(int pos, Type condtype, Type thentype, Type elsetype)
	{
		if (thentype.tag < Int && elsetype.tag == Int &&
				elsetype.isAssignable(thentype))
			return thentype.basetype();
		else if (elsetype.tag < Int && thentype.tag == Int &&
				thentype.isAssignable(elsetype))
			return elsetype.basetype();
		else if (thentype.tag <= DOUBLE && elsetype.tag <= DOUBLE)
		{
			for (int i = BYTE; i <= DOUBLE; i++)
			{
				Type candidate = syms.typeOfTag[i];
				if (thentype.isSubType(candidate) && elsetype.isSubType(candidate))
					return candidate;
			}
		}
		if (thentype.isSubType(elsetype))
			return elsetype.basetype();
		else if (elsetype.isSubType(thentype))
			return thentype.basetype();
		else
		{
			log.error(pos, "neither.conditional.subtype", thentype.toString(),
					elsetype.toString());
			return thentype.basetype();
		}
	}

	@Override public void visitParens(Tree.ParenExpr tree)
	{
		attribExpr(tree.subExpr, env);
		result = null;
	}

	@Override public void visitUnary(UnaryExpr tree)
	{
		Type argtype = (Tree.PREINC <= tree.tag && tree.tag <= Tree.POSTDEC) ?
				attribTree(tree.arg, env, VAR, Type.noType) :
				check.checkNonVoid(tree.arg.pos, attribExpr(tree.arg, env));
		Symbol operator = rs
				.resolveUnaryOperator(tree.pos, tree.tag - Tree.ASGOffset, env,
						argtype);
		tree.operator = (OperatorSymbol)operator;

		Type ownType = syms.errType;
		if (operator.kind == MTH)
		{
			ownType = operator.type.returnType();
			int opc = ((OperatorSymbol) operator).opcode;
			if (argtype.constValue != null)
			{
				Type ctype = cfold.fold1(opc, argtype);
			}
		}
		result = check(tree, ownType, VAL, pkind, pt);
	}

	@Override public void visitBinary(Tree.BinaryExpr tree)
	{
		Type left = check.checkNonVoid(tree.lhs.pos, attribExpr(tree.lhs, env));
		Type right = check
				.checkNonVoid(tree.lhs.pos, attribExpr(tree.rhs, env));

		Symbol operator = rs
				.resolveBinaryOperator(tree.pos, tree.tag, env, left, right);
		tree.operator = (OperatorSymbol)operator;

		Type owntype = syms.errType;
		if (operator.kind == MTH)
		{
			owntype = operator.type.returnType();
			int opc = ((OperatorSymbol) operator).opcode;
			// a illegal bianry operator
			if (tree.tag < Tree.OR || tree.tag > Tree.MOD)
			{
				log.error(tree.lhs.pos, "operator.cant.be.applied",
						treeinfo.operatorName(tree.tag).toString(),
						left.toString() + "," + right.toString());
			}
			// process constant folding
			if (left.constValue != null && right.constValue != null)
			{
				Type ctype = cfold.fold2(opc, left, right);
				if (ctype != null)
				{
					owntype = cfold.coerce(ctype, owntype);
				}
			}
		}
		result = check(tree, owntype, VAL, pkind, pt);

	}

	@Override public void visitTypeCast(CastExpr tree)
	{
		Type clazztype = attribType(tree.clazz, env);
		Type exprtype = attribExpr(tree.expr, env);
		Type owntype = check.checkCastable(tree.expr.pos, exprtype, clazztype);
		if (exprtype.constValue != null)
		{
			owntype = cfold.coerce(exprtype, clazztype);
		}
		result = check(tree, owntype, VAL, pkind, pt);
	}

	@Override public void visitIndexed(ArraySubscriptExpr tree)
	{
		Type owntype = syms.errType;
		Type atype = attribExpr(tree.indexed, env);
		attribExpr(tree.index, env, syms.intType);
		if (atype.tag == ConstantArray)
			owntype = atype.elemType();
		else if (atype.tag != Error)
			log.error(tree.pos, "array.require.but.not.found",
					atype.toString());
		result = check(tree, owntype, VAR, pkind, pt);
	}

	@Override public void visitTypeArray(TypeArray tree)
	{
		Type etype = attribType(tree.elemtype, env);
		result = check(tree, new ConstantArrayType(etype, syms.arrayClass), TYP, pkind,
				pt);

	}

	@Override public void visitTypeIdent(TypeIdent tree)
	{
		result = check(tree, syms.typeOfTag[tree.typetag], TYP, pkind, pt);
	}

	@Override public void visitLiteral(Literal tree)
	{
		result = check(tree, litType(tree.typetag).constType(tree.value), VAL,
				pkind, pt);
	}

	/**
	 * ReturnInst the type of a literal with given type tag.
	 */
	Type litType(int tag)
	{
		return syms.typeOfTag[tag];
	}

	/**
	 * Cannot resolve used identifier at moment
	 *
	 * @param tree
	 */
	@Override public void visitIdent(Tree.DeclRefExpr tree)
	{
		result = null;
	}

	@Override public void visitNewArray(NewArray tree)
	{
		Type owntype = syms.errType;
		Type elemtype;
		if (tree.elemtype != null)
		{
			elemtype = attribType(tree.elemtype, env);
			owntype = elemtype;
			for (Tree l : tree.dims)
			{
				attribExpr(l, env, syms.intType);
				owntype = new ConstantArrayType(owntype, syms.arrayClass);
			}
		}
		else
		{
			if (pt.tag == ConstantArray)
			{
				elemtype = pt.elemType();
			}
			else
			{
				if (pt.tag != Error)
				{
					log.error(tree.pos, "illegal.initializer.for.type",
							pt.toString());
				}
				elemtype = syms.errType;
			}
		}
		if (tree.elems != null)
		{
			attribExprs(tree.elems, env, elemtype);
			owntype = new ConstantArrayType(elemtype, syms.arrayClass);
		}
		result = check(tree, owntype, VAL, pkind, pt);
	}

	@Override public void visitErroneous(ErroneousTree erroneous)
	{
		result = erroneous.type = syms.errType;
	}

}
