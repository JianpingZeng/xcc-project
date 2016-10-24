package ast;

import ast.Tree.*;
import utils.Util;

/**
 * This class is common interface that client use as traveling the AST
 * using Visitor pattern. So that the semantic analysis and code generation 
 * can be implemented as same format. 
 * 
 * @author Xlous.zeng
 * @version 1.0
 */
public abstract class ASTVisitor<T> implements IASTVisitor<T>
{
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
