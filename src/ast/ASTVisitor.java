package ast;

import ast.Tree.*;

/**
 * This class is common interface that client use as traveling the AST
 * using Visitor pattern. So that the semantic analysis and code generation 
 * can be implemented as same format. 
 * 
 * @author Xlous.zeng
 * @version 1.0
 */
public abstract class ASTVisitor
{
	/**
	 * Visits the root class represents the top level tree node.
	 * @param that
	 */
    public void visitTree(Tree that) { assert false;}

	/**
	 *  A visitor method for traverse the {@linkplain TopLevel}.
	 * @param tree
	 */
	public void visitTopLevel(TopLevel tree) { visitTree(tree); }
	
	public void visitErroneous(ErroneousTree erroneous) {visitTree(erroneous);}

	//=================Statement visitor method===============================//
	public void visitBreakStmt(BreakStmt stmt) {visitTree(stmt);}

	public void visitCaseStmt(CaseStmt stmt) {visitTree(stmt);}

	public void visitCompoundStmt(CompoundStmt stmt) {visitTree(stmt);}

	public void visitContinueStmt(ContinueStmt stmt){visitTree(stmt);}

	public void visitDeclStmt(DeclStmt stmt) {visitTree(stmt);}

	public void visitDefaultStmt(DefaultStmt stmt) {visitTree(stmt);}

	public void visitDoStmt(DoStmt stmt) {visitTree(stmt);}

	public void visitForStmt(ForStmt stmt) {visitTree(stmt);}

	public void visitGotoStmt(GotoStmt stmt) {visitTree(stmt);}

	public void visitIfStmt(IfStmt stmt) {visitTree(stmt);}

	public void visitLabelledStmt(LabelledStmt stmt) {visitTree(stmt);}

	public void visitNullStmt(NullStmt stmt) {visitTree(stmt);}

	public void visitReturnStmt(ReturnStmt stmt) {visitTree(stmt);}

    public void visitSelectStmt(SelectStmt stmt) {visitTree(stmt);}

	public void visitSwitchStmt(SwitchStmt stmt) {visitTree(stmt);}

	public void visitWhileStmt(WhileStmt stmt) {visitTree(stmt);}

	//================Expression visitor method===============================//

    //================Bianry operaotr=========================================//
    public void visitBinaryExpr(BinaryExpr expr) {visitTree(expr);}

    public void visitCompoundAssignExpr(CompoundAssignExpr expr){visitTree(expr);}

    public void visitConditionalExpr(ConditionalExpr expr) {visitTree(expr);}

    // Unary operator.
    public void visitUnaryExpr(UnaryExpr expr) {visitTree(expr);}

    public void visitUnaryExprOrTypeTraitExpr(UnaryExprOrTypeTraitExpr expr) {visitTree(expr);}

    // postfix operator
    public void visitImplicitCastExpr(ImplicitCastExpr expr) {visitTree(expr);}

    public void visitExplicitCastExpr(ExplicitCastExpr expr) {visitTree(expr);}

    public void visitParenListExpr(ParenListExpr expr) {visitTree(expr);}

    public void visitArraySubscriptExpr(ArraySubscriptExpr expr) {visitTree(expr);}

    public void visitMemberExpr(MemberExpr expr) {visitTree(expr);}

    public void visitParenExpr(ParenExpr expr){visitTree(expr);}

    public void visitCallExpr(CallExpr expr) {visitTree(expr);}

    public void visitInitListExpr(InitListExpr expr){visitTree(expr);}

    // Primary expression.
    public void visitDeclRefExpr(DeclRefExpr expr) {visitTree(expr);}

    public void visitCharacterLiteral(CharacterLiteral literal){visitTree(literal);}

    public void visitCompoundLiteralExpr(CompoundLiteralExpr literal) {visitTree(literal);}

    public void visitFloatLiteral(FloatLiteral literal){visitTree(literal);}

    public void visitStringLiteral(StringLiteral literal) {visitTree(literal);}

    public void visitIntegerLiteral(IntegerLiteral literal) {visitTree(literal);}

}
