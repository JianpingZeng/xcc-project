package frontend.ast;

import java.util.List;

import tools.Name;
import frontend.ast.Tree.CallExpr;
import frontend.ast.Tree.Assign;
import frontend.ast.Tree.OpAssign;
import frontend.ast.Tree.CaseStmt;
import frontend.ast.Tree.DoStmt;
import frontend.ast.Tree.ErroneousTree;
import frontend.ast.Tree.Exec;
import frontend.ast.Tree.ForStmt;
import frontend.ast.Tree.GotoStmt;
import frontend.ast.Tree.Import;
import frontend.ast.Tree.ArraySubscriptExpr;
import frontend.ast.Tree.LabelledStmt;
import frontend.ast.Tree.Literal;
import frontend.ast.Tree.MethodDef;
import frontend.ast.Tree.NewArray;
import frontend.ast.Tree.ReturnStmt;
import frontend.ast.Tree.NullStmt;
import frontend.ast.Tree.SwitchStmt;
import frontend.ast.Tree.TopLevel;
import frontend.ast.Tree.TypeArray;
import frontend.ast.Tree.CastExpr;
import frontend.ast.Tree.TypeIdent;
import frontend.ast.Tree.VarDef;

/**
 * An interface for tree factories
 */
public interface Factory {

   TopLevel TopLevel(List<Tree> defs);

   Import Import(Tree qualid);

   MethodDef MethodDef(long flags, Name name, Tree restype,
           List<Tree> params, Tree body);

   VarDef VarDef(long flags, Name name, Tree vartype, Tree init);

   NullStmt Skip();

   Tree.CompoundStmt Block( List<Tree> stats);

   DoStmt DoLoop(Tree body, Tree cond);

   Tree.WhileStmt WhileLoop(Tree cond, Tree body);

   ForStmt ForLoop(List<Tree> init, Tree cond, List<Tree> step, Tree body);

   LabelledStmt Labelled(Name label, Tree body);

   SwitchStmt Switch(Tree selector, List<CaseStmt> cases);

   CaseStmt Case(List<Tree> values, Tree caseBody);

   Tree.ConditionalExpr Conditional(Tree cond, Tree thenpart, Tree elsepart);

   Tree.IfStmt If(Tree cond, Tree thenpart, Tree elsepart);

   Exec Exec(Tree expr);

   Tree.BreakStmt Break();

   Tree.ContinueStmt Continue();

   GotoStmt Goto(Name label);
   
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

   ErroneousTree Erroneous();
}
