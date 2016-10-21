package ast;

import ast.Tree.Assign;
import ast.Tree.OpAssign;
import ast.Tree.BinaryExpr;
import ast.Tree.Block;
import ast.Tree.BreakStmt;
import ast.Tree.CaseStmt;
import ast.Tree.DoLoop;
import ast.Tree.Erroneous;
import ast.Tree.ForLoop;
import ast.Tree.Goto;
import ast.Tree.IfStmt;
import ast.Tree.Literal;
import ast.Tree.MethodDef;
import ast.Tree.NewArray;
import ast.Tree.ParenExpr;
import ast.Tree.ReturnStmt;
import ast.Tree.Select;
import ast.Tree.Skip;
import ast.Tree.TopLevel;
import ast.Tree.TypeArray;
import ast.Tree.CastExpr;
import ast.Tree.TypeIdent;
import ast.Tree.UnaryExpr;
import ast.Tree.VarDef;
import ast.Tree.WhileLoop;

/**
 * This class is common interface that client use as traveling the AST
 * using Visitor pattern. So that the semantic analysis and code generation 
 * can be implemented as same format. 
 * 
 * @author Xlous.zeng
 * @version 1.0
 */
public abstract class AstVisitor
{
	
	/**
	 * Visits the root class represents the top level tree node.
	 * @param that
	 */
    public void visitTree(Tree that) {
        assert false;
    }
    
	public void visitTopLevel(TopLevel tree)
	{
		visitTree(tree);
	}

	public void visitImport(Import tree)
	{
		visitTree(tree);
	}

	public void visitMethodDef(MethodDef tree)
	{
		visitTree(tree);
	}

	public void visitVarDef(VarDef tree)
	{
		visitTree(tree);
	}
	
	public void visitSkip(Skip tree) 
	{
		visitTree(tree);
	}
	
	public void visitBlock(Block tree)
	{
		visitTree(tree);
	}

	public void visitIf(IfStmt tree)
	{
		visitTree(tree);
	}
	
	public void visitSwitch(Tree.SwitchStmt tree)
	{
		visitTree(tree);
	}

	public void visitForLoop(ForLoop tree)
	{
		visitTree(tree);
	}

	public void visitBreak(BreakStmt tree)
	{
		visitTree(tree);
	}

	public void visitContinue(Tree.ContinueStmt tree)
	{
		visitTree(tree);
	}
	
	public void visitGoto(Goto tree)
	{
		visitTree(tree);
	}

	public void visitDoLoop(DoLoop tree)
	{
		visitTree(tree);
	}
	
	public void visitWhileLoop(WhileLoop tree)
	{
		visitTree(tree);
	}

	public void visitCase(CaseStmt tree)
	{
		visitTree(tree);
	}

	public void visitLabelled(Tree.LabelledStmt tree)
	{
		visitTree(tree);
	}
	
	public void visitReturn(ReturnStmt tree)
	{
		visitTree(tree);
	}

	public void visitSelect(Select tree)
	{
		visitTree(tree);
	}

	public void visitApply(Tree.CallExpr tree)
	{
		visitTree(tree);
	}

	public void visitAssign(Assign tree)
	{
		visitTree(tree);
	}

	public void visitExec(Exec tree)
	{
		visitTree(tree);
	}

	public void visitConditional(Tree.ConditionalExpr tree)
	{
		visitTree(tree);
	}

	public void visitParens(ParenExpr tree)
	{
		visitTree(tree);
	}

	public void visitAssignop(OpAssign tree)
	{
		visitTree(tree);
	}

	public void visitUnary(UnaryExpr tree)
	{
		visitTree(tree);
	}

	public void visitBinary(BinaryExpr tree)
	{
		visitTree(tree);
	}

	public void visitTypeCast(CastExpr tree)
	{
		visitTree(tree);
	}

	public void visitIndexed(Tree.ArraySubscriptExpr tree)
	{
		visitTree(tree);
	}	

	public void visitTypeArray(TypeArray tree)
	{
		visitTree(tree);
	}

	public void visitTypeIdent(TypeIdent tree)
	{
		visitTree(tree);
	}

	public void visitLiteral(Literal tree)
	{
		visitTree(tree);
	}

	public void visitIdent(Tree.DeclRefExpr tree)
	{
		visitTree(tree);
	}

	public void visitNewArray(NewArray tree)
	{
		visitTree(tree);
	}
	
	public void visitErroneous(Erroneous erroneous)
	{
		visitTree(erroneous);
	}
}
