package ast;

import ast.Tree.Apply;
import ast.Tree.Assign;
import ast.Tree.Assignop;
import ast.Tree.Binary;
import ast.Tree.Block;
import ast.Tree.Break;
import ast.Tree.Case;
import ast.Tree.Conditional;
import ast.Tree.Continue;
import ast.Tree.DoLoop;
import ast.Tree.Erroneous;
import ast.Tree.Exec;
import ast.Tree.ForLoop;
import ast.Tree.Goto;
import ast.Tree.Ident;
import ast.Tree.If;
import ast.Tree.Import;
import ast.Tree.Indexed;
import ast.Tree.Labelled;
import ast.Tree.Literal;
import ast.Tree.MethodDef;
import ast.Tree.NewArray;
import ast.Tree.Parens;
import ast.Tree.Return;
import ast.Tree.Select;
import ast.Tree.Skip;
import ast.Tree.Switch;
import ast.Tree.TopLevel;
import ast.Tree.TypeArray;
import ast.Tree.TypeCast;
import ast.Tree.TypeIdent;
import ast.Tree.Unary;
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
public abstract class ASTVisitor {
	
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

	public void visitIf(If tree)
	{
		visitTree(tree);
	}
	
	public void visitSwitch(Switch tree)
	{
		visitTree(tree);
	}

	public void visitForLoop(ForLoop tree)
	{
		visitTree(tree);
	}

	public void visitBreak(Break tree)
	{
		visitTree(tree);
	}

	public void visitContinue(Continue tree)
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

	public void visitCase(Case tree)
	{
		visitTree(tree);
	}

	public void visitLabelled(Labelled tree)
	{
		visitTree(tree);
	}
	
	public void visitReturn(Return tree)
	{
		visitTree(tree);
	}

	public void visitSelect(Select tree)
	{
		visitTree(tree);
	}

	public void visitApply(Apply tree)
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

	public void visitConditional(Conditional tree)
	{
		visitTree(tree);
	}

	public void visitParens(Parens tree)
	{
		visitTree(tree);
	}

	public void visitAssignop(Assignop tree)
	{
		visitTree(tree);
	}

	public void visitUnary(Unary tree)
	{
		visitTree(tree);
	}

	public void visitBinary(Binary tree)
	{
		visitTree(tree);
	}

	public void visitTypeCast(TypeCast tree)
	{
		visitTree(tree);
	}

	public void visitIndexed(Indexed tree)
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

	public void visitIdent(Ident tree)
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
