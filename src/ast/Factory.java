package ast; 

import java.util.List;

import utils.Name;
import ast.Tree.CallExpr;
import ast.Tree.Assign;
import ast.Tree.OpAssign;
import ast.Tree.Block;
import ast.Tree.CaseStmt;
import ast.Tree.DoLoop;
import ast.Tree.Erroneous;
import ast.Tree.Exec;
import ast.Tree.ForLoop;
import ast.Tree.Goto;
import ast.Tree.Import;
import ast.Tree.ArraySubscriptExpr;
import ast.Tree.LabelledStmt;
import ast.Tree.Literal;
import ast.Tree.MethodDef;
import ast.Tree.NewArray;
import ast.Tree.ReturnStmt;
import ast.Tree.Skip;
import ast.Tree.SwitchStmt;
import ast.Tree.TopLevel;
import ast.Tree.TypeArray;
import ast.Tree.CastExpr;
import ast.Tree.TypeIdent;
import ast.Tree.VarDef;
import ast.Tree.WhileLoop;

/**
 * An interface for tree factories
 */
public interface Factory {

   TopLevel TopLevel(List<Tree> defs);

   Import Import(Tree qualid);

   MethodDef MethodDef(long flags, Name name, Tree restype,
           List<Tree> params, Tree body);

   VarDef VarDef(long flags, Name name, Tree vartype, Tree init);

   Skip Skip();

   Block Block( List<Tree> stats);

   DoLoop DoLoop(Tree body, Tree cond);

   WhileLoop WhileLoop(Tree cond, Tree body);

   ForLoop ForLoop(List<Tree> init, Tree cond, List<Tree> step, Tree body);

   LabelledStmt Labelled(Name label, Tree body);

   SwitchStmt Switch(Tree selector, List<CaseStmt> cases);

   CaseStmt Case(List<Tree> values, Tree caseBody);

   Tree.ConditionalExpr Conditional(Tree cond, Tree thenpart, Tree elsepart);

   Tree.IfStmt If(Tree cond, Tree thenpart, Tree elsepart);

   Exec Exec(Tree expr);

   Tree.BreakStmt Break();

   Tree.ContinueStmt Continue();

   Goto Goto(Name label);
   
   ReturnStmt Return(Tree expr);

   CallExpr Apply(Tree fn, List<Tree> args);

   Tree.ParenExpr Parens(Tree expr);

   Assign Assign(Tree lhs, Tree rhs);

   OpAssign Assignop(int opcode, Tree lhs, Tree rhs);

   Tree.UnaryExpr Unary(int opcode, Tree arg);

   Tree.BinaryExpr Binary(int opcode, Tree lhs, Tree rhs);

   CastExpr TypeCast(Tree expr, Tree type);

   ArraySubscriptExpr Indexed(Tree indexed, Tree index);

   Tree.DeclRefExpr Ident(Name idname);

   Literal Literal(int tag, Object value);

   TypeIdent TypeIdent(int typetag);

   TypeArray TypeArray(Tree elemtype);
   
   NewArray NewArray(Tree elemtype, List<Tree> dims, List<Tree> elems);

   Erroneous Erroneous();
}
