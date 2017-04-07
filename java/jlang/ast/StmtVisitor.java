package jlang.ast;

import jlang.ast.Tree.*;

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
				case BO_MulAssign: return visitBinMulAssign((CompoundAssignExpr) binOp);
				case BO_DivAssign: return visitBinDivAssign((CompoundAssignExpr) binOp);
				case BO_RemAssign: return visitBinRemAssign((CompoundAssignExpr) binOp);
				case BO_AddAssign: return visitBinAddAssign((CompoundAssignExpr) binOp);
				case BO_SubAssign: return visitBinSubAssign((CompoundAssignExpr) binOp);
				case BO_ShlAssign: return visitBinShlAssign((CompoundAssignExpr) binOp);
				case BO_ShrAssign: return visitBinShrAssign((CompoundAssignExpr) binOp);
				case BO_AndAssign: return visitBinAndAssign((CompoundAssignExpr) binOp);
				case BO_OrAssign: return visitBinOrAssign((CompoundAssignExpr) binOp);
				case BO_XorAssign: return visitBinXorAssign((CompoundAssignExpr) binOp);
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
			case Tree.ConditionalOperatorClass:
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

	//=========================================================================//
	// If the subclass doesn't implement binary operator methods, fall back to
	// visitBinaryExpr().
	//=========================================================================//
	public T visitBinMul(BinaryExpr expr){return visitBinaryExpr(expr);}

	public T visitBinDiv(BinaryExpr expr){ return visitBinaryExpr(expr);}

	public T visitBinRem(BinaryExpr expr){ return visitBinaryExpr(expr);}

	public T visitBinAdd(BinaryExpr expr){ return visitBinaryExpr(expr);}

	public T visitBinSub(BinaryExpr expr){ return visitBinaryExpr(expr);}

	public T visitBinShl(BinaryExpr expr){ return visitBinaryExpr(expr);}

	public T visitBinShr(BinaryExpr expr){ return visitBinaryExpr(expr);}

	public T visitBinLT(BinaryExpr expr){ return visitBinaryExpr(expr);}

	public T visitBinGT(BinaryExpr expr){ return visitBinaryExpr(expr);}

	public T visitBinLE(BinaryExpr expr){ return visitBinaryExpr(expr);}

	public T visitBinGE(BinaryExpr expr){ return visitBinaryExpr(expr);}

	public T visitBinEQ(BinaryExpr expr){ return visitBinaryExpr(expr);}

	public T visitBinNE(BinaryExpr expr){ return visitBinaryExpr(expr);}

	public T visitBinAnd(BinaryExpr expr){ return visitBinaryExpr(expr);}

	public T visitBinXor(BinaryExpr expr){ return visitBinaryExpr(expr);}

	public T visitBinOr(BinaryExpr expr){ return visitBinaryExpr(expr);}

	public T visitBinLAnd(BinaryExpr expr){ return visitBinaryExpr(expr);}

	public T visitBinLOr(BinaryExpr expr){ return visitBinaryExpr(expr);}

	public T visitBinAssign(BinaryExpr expr){ return visitBinaryExpr(expr);}

	public T visitBinComma(BinaryExpr expr){ return visitBinaryExpr(expr);}


	//=========================================================================//
	// If the subclass does not implements compound assignment operator, just
	// fall back to method visitCompoundAssignExpr().
	//=========================================================================//
	public T visitBinMulAssign(CompoundAssignExpr expr){ return visitCompoundAssignExpr(expr);}

	public T visitBinDivAssign(CompoundAssignExpr expr){ return visitCompoundAssignExpr(expr);}

	public T visitBinRemAssign(CompoundAssignExpr expr){ return visitCompoundAssignExpr(expr);}

	public T visitBinAddAssign(CompoundAssignExpr expr){ return visitCompoundAssignExpr(expr);}

	public T visitBinSubAssign(CompoundAssignExpr expr){ return visitCompoundAssignExpr(expr);}

	public T visitBinShlAssign(CompoundAssignExpr expr){ return visitCompoundAssignExpr(expr);}

	public T visitBinShrAssign(CompoundAssignExpr expr){ return visitCompoundAssignExpr(expr);}

	public T visitBinAndAssign(CompoundAssignExpr expr){ return visitCompoundAssignExpr(expr);}

	public T visitBinOrAssign(CompoundAssignExpr expr){ return visitCompoundAssignExpr(expr);}

	public T visitBinXorAssign(CompoundAssignExpr expr){ return visitCompoundAssignExpr(expr);}

	//=========================================================================//
	// If the subclass does not implements unary operator visitor method, just
	// fall back to method visitUnaryExpr().
	//=========================================================================//
	public T visitUnaryPostInc(UnaryExpr expr){ return visitUnaryExpr(expr);}

	public T visitUnaryPostDec(UnaryExpr expr){ return visitUnaryExpr(expr);}

	public T visitUnaryPreInc(UnaryExpr expr){ return visitUnaryExpr(expr);}

	public T visitUnaryPreDec(UnaryExpr expr){ return visitUnaryExpr(expr);}

	public T visitUnaryAddrOf(UnaryExpr expr){ return visitUnaryExpr(expr);}

	public T visitUnaryDeref(UnaryExpr expr){ return visitUnaryExpr(expr);}

	public T visitUnaryPlus(UnaryExpr expr){ return visitUnaryExpr(expr);}

	public T visitUnaryMinus(UnaryExpr expr){ return visitUnaryExpr(expr);}

	public T visitUnaryNot(UnaryExpr expr){ return visitUnaryExpr(expr);}

	public T visitUnaryLNot(UnaryExpr expr){ return visitUnaryExpr(expr);}

	public T visitUnaryReal(UnaryExpr expr){ return visitUnaryExpr(expr);}

	public T visitUnaryImag(UnaryExpr expr){ return visitUnaryExpr(expr);}

	public T visitStmt(Stmt s)
	{
		return (T)new Object();
	}

	//=================Statement visitor method===============================//
	public T visitBreakStmt(BreakStmt stmt) {return visitStmt(stmt);}

	public T visitCaseStmt(CaseStmt stmt) {return visitStmt(stmt);}

	public T visitCompoundStmt(CompoundStmt stmt) {return visitStmt(stmt);}

	public T visitContinueStmt(ContinueStmt stmt) {return visitStmt(stmt);}

	public T visitDeclStmt(DeclStmt stmt) { return visitStmt(stmt);}

	public T visitDefaultStmt(DefaultStmt stmt) {return visitStmt(stmt);}

	public T visitDoStmt(DoStmt stmt) {return visitStmt(stmt);}

	public T visitForStmt(ForStmt stmt) {return visitStmt(stmt);}

	public T visitGotoStmt(GotoStmt stmt) {return visitStmt(stmt);}

	public T visitIfStmt(IfStmt stmt) {return visitStmt(stmt);}

	public T visitLabelledStmt(LabelledStmt stmt) {return visitStmt(stmt);}

	public T visitNullStmt(NullStmt stmt) {return visitStmt(stmt);}

	public T visitReturnStmt(ReturnStmt stmt) {return visitStmt(stmt);}

    public T visitSelectStmt(SelectStmt stmt) {return visitStmt(stmt);}

	public T visitSwitchStmt(SwitchStmt stmt) {return visitStmt(stmt);}

	public T visitWhileStmt(WhileStmt stmt) {return visitStmt(stmt);}

	//================Expression visitor method===============================//

    //================Bianry operaotr=========================================//
    public T visitBinaryExpr(BinaryExpr expr) {return visitStmt(expr);}

    public T visitCompoundAssignExpr(CompoundAssignExpr expr){return visitStmt(expr);}

    public T visitConditionalExpr(ConditionalExpr expr) {return visitStmt(expr);}

    // Unary operator.
    public T visitUnaryExpr(UnaryExpr expr) {return visitStmt(expr);}

    public T visitUnaryExprOrTypeTraitExpr(UnaryExprOrTypeTraitExpr expr) {return visitStmt(expr);}

    // postfix operator
    public T visitImplicitCastExpr(ImplicitCastExpr expr) {return visitStmt(expr);}

    public T visitExplicitCastExpr(ExplicitCastExpr expr) {return visitStmt(expr);}

    public T visitParenListExpr(ParenListExpr expr) {return visitStmt(expr);}

    public T visitArraySubscriptExpr(ArraySubscriptExpr expr) {return visitStmt(expr);}

    public T visitMemberExpr(MemberExpr expr) {return visitStmt(expr);}

    public T visitParenExpr(ParenExpr expr){return visitStmt(expr);}

    public T visitCallExpr(CallExpr expr) {return visitStmt(expr);}

    public T visitInitListExpr(InitListExpr expr){return visitStmt(expr);}

    // Primary expression.
    public T visitDeclRefExpr(DeclRefExpr expr) {return visitStmt(expr);}

    public T visitCharacterLiteral(CharacterLiteral literal){return visitStmt(literal);}

    public T visitCompoundLiteralExpr(CompoundLiteralExpr literal) {return visitStmt(literal);}

    public T visitFloatLiteral(FloatLiteral literal){return visitStmt(literal);}

    public T visitStringLiteral(StringLiteral literal) {return visitStmt(literal);}

    public T visitIntegerLiteral(IntegerLiteral literal) {return visitStmt(literal);}

}
