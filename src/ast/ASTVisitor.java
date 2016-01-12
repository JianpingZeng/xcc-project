package ast;

import ast.Tree.*;

/**
 * This class is common interface that client use as traveling the AST
 * using Visitor pattern. So that the semantic analysis and code generation 
 * can be implemented as same format. 
 * 
 * @author Jianping Zeng
 * @version 1.0
 */
abstract public class ASTVisitor {
	
	/**
	 * Visits the root class represents the top level tree node.
	 * @param that
	 */
    public void visitTree(Tree that) {
        assert false;
    }
    
	abstract public void visitTopLevel(TopLevel tree);

	abstract public void visitImport(Import tree);

	abstract public void visitMethodDef(MethodDef tree);

	abstract public void visitVarDef(VarDef tree);

	abstract public void visitSkip(Skip tree) ;
	
	public abstract void visitBlock(Block tree);

	abstract public void visitIf(If tree);
	
	abstract public void visitSwitch(Switch tree);

	abstract public void visitForLoop(ForLoop tree);

	abstract public void visitBreak(Break tree);

	abstract public void visitContinue(Continue tree);
	
	abstract public void visitGoto(Goto tree);

	abstract public void visitDoLoop(DoLoop tree);

	abstract public void visitWhileLoop(WhileLoop tree);

	abstract public void visitCase(Case tree);

	abstract public void visitLabelled(Labelled tree);
	
	abstract public void visitReturn(Return tree);

	abstract public void visitSelect(Select tree);

	abstract public void visitApply(Apply tree);

	abstract public void visitAssign(Assign tree);

	abstract public void visitExec(Exec tree);

	abstract public void visitConditional(Conditional tree);

	abstract public void visitParens(Parens tree);

	abstract public void visitAssignop(Assignop tree);

	abstract public void visitUnary(Unary tree);

	abstract public void visitBinary(Binary tree);

	abstract public void visitTypeCast(TypeCast tree);

	abstract public void visitIndexed(Indexed tree);

	abstract public void visitTypeArray(TypeArray tree);

	abstract public void visitTypeIdent(TypeIdent tree);

	abstract public void visitLiteral(Literal tree);

	abstract public void visitIdent(Ident tree);

	abstract public void visitNewArray(NewArray tree);   
	
	abstract public void visitErroneous(Erroneous erroneous);
}
