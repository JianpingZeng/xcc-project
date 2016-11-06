package frontend.ast;

import frontend.ast.Tree.*;
import tools.Util;

/**
 * This class is common interface that client use as traveling the AST
 * using Visitor pattern. So that the semantic analysis and code generation
 * can be implemented as same format.
 *
 * @author Xlous.zeng
 * @version 1.0
 */
public abstract class StmtVisitor<T> implements IStmtVisitor<T>
{
	/**
	 * This method is the entry for visiting diversely statements.
	 * @param s
	 * @return
	 */
	public T visit(Stmt s)
	{
		// If we have a binary s, dispatch to the subcode of the binop.
		// A smart optimizer will fold this comparison into the switch stmt
		// below.
		if (s instanceof BinaryExpr)
		{
			BinaryExpr binOp = (BinaryExpr)s;
			switch (binOp.getOpcode())
			{
				default:assert false:"Unknown binary operator!";
				case BO_Mul: return visitBinMul(binOp);
				case BO_Div: return visitBinDiv(binOp);
				case BO_Rem: return visitBinRem(binOp);
				case BO_Add: return visitBinAdd(binOp);
				case BO_Sub: return visitBinSub(binOp);
				case BO_Shl: return visitBinShl(binOp);
				case BO_Shr: return visitBinShr(binOp);

				case BO_LT: return visitBinLT(binOp);
				case BO_GT: return visitBinGT(binOp);
				case BO_LE: return visitBinLE(binOp);
				case BO_GE: return visitBinGE(binOp);
				case BO_EQ: return visitBinEQ(binOp);
				case BO_NE: return visitBinNE(binOp);

				case BO_And: return visitBinAnd(binOp);
				case BO_Xor: return visitBinXor(binOp);
				case BO_Or: return visitBinOr(binOp);
				case BO_LAnd: return visitBinLAnd(binOp);
				case BO_LOr: return visitBinLOr(binOp);
				case BO_Assign: return visitBinAssign(binOp);
				case BO_MulAssign: return visitBinMulAssign(binOp);
				case BO_DivAssign: return visitBinDivAssign(binOp);
				case BO_RemAssign: return visitBinRemAssign(binOp);
				case BO_AddAssign: return visitBinAddAssign(binOp);
				case BO_SubAssign: return visitBinSubAssign(binOp);
				case BO_ShlAssign: return visitBinShlAssign(binOp);
				case BO_ShrAssign: return visitBinShrAssign(binOp);
				case BO_AndAssign: return visitBinAndAssign(binOp);
				case BO_OrAssign: return visitBinOrAssign(binOp);
				case BO_XorAssign: return visitBinXorAssign(binOp);
				case BO_Comma: return visitBinComma(binOp);
			}
		}
		else if (s instanceof UnaryExpr)
		{
			UnaryExpr unOp = (UnaryExpr)s;
			switch (unOp.getOpCode())
			{
				default: assert false:"Unknown unary operator!";
				case UO_PostInc: return visitUnaryPostInc(unOp);
				case UO_PostDec: return visitUnaryPostDec(unOp);
				case UO_PreInc: return visitUnaryPreInc(unOp);
				case UO_PreDec: return visitUnaryPreDec(unOp);
				case UO_AddrOf: return visitUnaryAddrOf(unOp);
				case UO_Deref: return visitUnaryDeref(unOp);
				case UO_Plus: return visitUnaryPlus(unOp);
				case UO_Minus: return visitUnaryMinus(unOp);
				case UO_Not: return visitUnaryNot(unOp);
				case UO_LNot: return visitUnaryLNot(unOp);
				case UO_Real: return visitUnaryReal(unOp);
				case UO_Imag: return visitUnaryImag(unOp);
			}
		}
		switch (s.getStmtClass())
		{
			case Tree.ArraySubscriptExprClass:
				return visitArraySubscriptExpr((ArraySubscriptExpr) s);
			case Tree.BreakStmtClass:
				return visitBreakStmt((BreakStmt)s);
			case Tree.CallExprClass:
				return visitCallExpr((CallExpr)s);
			case Tree.CaseStmtClass:
				return visitCaseStmt((CaseStmt)s);
			case Tree.CharacterLiteralClass:
				return visitCharacterLiteral((CharacterLiteral)s);
			case Tree.CompoundLiteralExprClass:
				return visitCompoundLiteralExpr((CompoundLiteralExpr)s);
			case Tree.CompoundStmtClass:
				return visitCompoundStmt((CompoundStmt)s);
			case Tree.CondtionalOperatorClass:
				return visitConditionalExpr((ConditionalExpr)s);
			case Tree.ContinueStmtClass:
				return visitContinueStmt((ContinueStmt)s);
			case Tree.DeclRefExprClass:
				return visitDeclRefExpr((DeclRefExpr)s);
			case Tree.DeclStmtClass:
				return visitDeclStmt((DeclStmt)s);
			case Tree.DefaultStmtClass:
				return visitDefaultStmt((DefaultStmt)s);
			case Tree.DoStmtClass:
				return visitDoStmt((DoStmt)s);
			case Tree.ExplicitCastClass:
				return visitExplicitCastExpr((ExplicitCastExpr)s);
			case Tree.FloatLiteralClass:
				return visitFloatLiteral((FloatLiteral)s);
			case Tree.ForStmtClass:
				return visitForStmt((ForStmt)s);
			case Tree.GotoStmtClass:
				return visitGotoStmt((GotoStmt)s);
			case Tree.IfStmtClass:
				return visitIfStmt((IfStmt)s);
			case Tree.ImplicitCastClass:
				return visitImplicitCastExpr((ImplicitCastExpr)s);
			case Tree.InitListExprClass:
				return visitInitListExpr((InitListExpr)s);
			case Tree.IntegerLiteralClass:
				return visitIntegerLiteral((IntegerLiteral)s);
			case Tree.LabelledStmtClass:
				return visitLabelledStmt((LabelledStmt)s);
			case Tree.MemberExprClass:
				return visitMemberExpr((MemberExpr)s);
			case Tree.NullStmtClass:
				return visitNullStmt((NullStmt)s);
			case Tree.ParenExprClass:
				return visitParenExpr((ParenExpr)s);
			case Tree.ParenListExprClass:
				return visitParenListExpr((ParenListExpr)s);
			case Tree.ReturnStmtClass:
				return visitReturnStmt((ReturnStmt)s);
			case Tree.SelectExprClass:
				return visitSelectStmt((SelectStmt)s);
			case Tree.StringLiteralClass:
				return visitStringLiteral((StringLiteral) s);
			case Tree.SwitchStmtClass:
				return visitSwitchStmt((SwitchStmt)s);
			case Tree.TopLevelClass:
				break;
			case Tree.UnaryExprOrTypeTraitClass:
				return visitUnaryExprOrTypeTraitExpr((UnaryExprOrTypeTraitExpr)s);
			case Tree.WhileStmtClass:
				return visitWhileStmt((WhileStmt)s);
		}
		return null;
	}

	public abstract T visitBinMul(BinaryExpr expr);

	public abstract T visitBinDiv(BinaryExpr expr);

	public abstract T visitBinRem(BinaryExpr expr);

	public abstract T visitBinAdd(BinaryExpr expr);

	public abstract T visitBinSub(BinaryExpr expr);

	public abstract T visitBinShl(BinaryExpr expr);

	public abstract T visitBinShr(BinaryExpr expr);

	public abstract T visitBinLT(BinaryExpr expr);

	public abstract T visitBinGT(BinaryExpr expr);

	public abstract T visitBinLE(BinaryExpr expr);

	public abstract T visitBinGE(BinaryExpr expr);

	public abstract T visitBinEQ(BinaryExpr expr);

	public abstract T visitBinNE(BinaryExpr expr);

	public abstract T visitBinAnd(BinaryExpr expr);

	public abstract T visitBinXor(BinaryExpr expr);

	public abstract T visitBinOr(BinaryExpr expr);

	public abstract T visitBinLAnd(BinaryExpr expr);

	public abstract T visitBinLOr(BinaryExpr expr);

	public abstract T visitBinAssign(BinaryExpr expr);

	public abstract T visitBinMulAssign(BinaryExpr expr);

	public abstract T visitBinDivAssign(BinaryExpr expr);

	public abstract T visitBinRemAssign(BinaryExpr expr);

	public abstract T visitBinAddAssign(BinaryExpr expr);

	public abstract T visitBinSubAssign(BinaryExpr expr);

	public abstract T visitBinShlAssign(BinaryExpr expr);

	public abstract T visitBinShrAssign(BinaryExpr expr);

	public abstract T visitBinAndAssign(BinaryExpr expr);

	public abstract T visitBinOrAssign(BinaryExpr expr);

	public abstract T visitBinXorAssign(BinaryExpr expr);

	public abstract T visitBinComma(BinaryExpr expr);

	public abstract T visitUnaryPostInc(UnaryExpr expr);

	public abstract T visitUnaryPostDec(UnaryExpr expr);

	public abstract T visitUnaryPreInc(UnaryExpr expr);

	public abstract T visitUnaryPreDec(UnaryExpr expr);

	public abstract T visitUnaryAddrOf(UnaryExpr expr);

	public abstract T visitUnaryDeref(UnaryExpr expr);

	public abstract T visitUnaryPlus(UnaryExpr expr);

	public abstract T visitUnaryMinus(UnaryExpr expr);

	public abstract T visitUnaryNot(UnaryExpr expr);

	public abstract T visitUnaryLNot(UnaryExpr expr);

	public abstract T visitUnaryReal(UnaryExpr expr);

	public abstract T visitUnaryImag(UnaryExpr expr);

	public T visitStmt(Stmt s)
	{
		return (T)new Object();
	}

	/**
	 * Visits the root class represents the top level tree node.
	 * @param that
	 */
    public T visitTree(Tree that)
    {
        Util.shouldNotReachHere("Should not reach here!");
        return (T)new Object();
    }

	/**
	 *  A visitor method for traverse the {@linkplain TopLevel}.
	 * @param tree
	 */
	public T visitTopLevel(TopLevel tree) { return visitTree(tree); }

	public T visitErroneous(ErroneousTree erroneous) { return visitTree(erroneous);}

	//=================Statement visitor method===============================//
	public T visitBreakStmt(BreakStmt stmt) {return visitTree(stmt);}

	public T visitCaseStmt(CaseStmt stmt) {return visitTree(stmt);}

	public T visitCompoundStmt(CompoundStmt stmt) {return visitTree(stmt);}

	public T visitContinueStmt(ContinueStmt stmt) {return visitTree(stmt);}

	public T visitDeclStmt(DeclStmt stmt) { return visitTree(stmt);}

	public T visitDefaultStmt(DefaultStmt stmt) {return visitTree(stmt);}

	public T visitDoStmt(DoStmt stmt) {return visitTree(stmt);}

	public T visitForStmt(ForStmt stmt) {return visitTree(stmt);}

	public T visitGotoStmt(GotoStmt stmt) {return visitTree(stmt);}

	public T visitIfStmt(IfStmt stmt) {return visitTree(stmt);}

	public T visitLabelledStmt(LabelledStmt stmt) {return visitTree(stmt);}

	public T visitNullStmt(NullStmt stmt) {return visitTree(stmt);}

	public T visitReturnStmt(ReturnStmt stmt) {return visitTree(stmt);}

    public T visitSelectStmt(SelectStmt stmt) {return visitTree(stmt);}

	public T visitSwitchStmt(SwitchStmt stmt) {return visitTree(stmt);}

	public T visitWhileStmt(WhileStmt stmt) {return visitTree(stmt);}

	//================Expression visitor method===============================//

    //================Bianry operaotr=========================================//
    public T visitBinaryExpr(BinaryExpr expr) {return visitTree(expr);}

    public T visitCompoundAssignExpr(CompoundAssignExpr expr){return visitTree(expr);}

    public T visitConditionalExpr(ConditionalExpr expr) {return visitTree(expr);}

    // Unary operator.
    public T visitUnaryExpr(UnaryExpr expr) {return visitTree(expr);}

    public T visitUnaryExprOrTypeTraitExpr(UnaryExprOrTypeTraitExpr expr) {return visitTree(expr);}

    // postfix operator
    public T visitImplicitCastExpr(ImplicitCastExpr expr) {return visitTree(expr);}

    public T visitExplicitCastExpr(ExplicitCastExpr expr) {return visitTree(expr);}

    public T visitParenListExpr(ParenListExpr expr) {return visitTree(expr);}

    public T visitArraySubscriptExpr(ArraySubscriptExpr expr) {return visitTree(expr);}

    public T visitMemberExpr(MemberExpr expr) {return visitTree(expr);}

    public T visitParenExpr(ParenExpr expr){return visitTree(expr);}

    public T visitCallExpr(CallExpr expr) {return visitTree(expr);}

    public T visitInitListExpr(InitListExpr expr){return visitTree(expr);}

    // Primary expression.
    public T visitDeclRefExpr(DeclRefExpr expr) {return visitTree(expr);}

    public T visitCharacterLiteral(CharacterLiteral literal){return visitTree(literal);}

    public T visitCompoundLiteralExpr(CompoundLiteralExpr literal) {return visitTree(literal);}

    public T visitFloatLiteral(FloatLiteral literal){return visitTree(literal);}

    public T visitStringLiteral(StringLiteral literal) {return visitTree(literal);}

    public T visitIntegerLiteral(IntegerLiteral literal) {return visitTree(literal);}

}
