package jlang.ast;

import java.util.List;

import tools.Name;
import jlang.ast.Tree.CallExpr;
import jlang.ast.Tree.Assign;
import jlang.ast.Tree.OpAssign;
import jlang.ast.Tree.CaseStmt;
import jlang.ast.Tree.DoStmt;
import jlang.ast.Tree.ErroneousTree;
import jlang.ast.Tree.Exec;
import jlang.ast.Tree.ForStmt;
import jlang.ast.Tree.GotoStmt;
import jlang.ast.Tree.Import;
import jlang.ast.Tree.ArraySubscriptExpr;
import jlang.ast.Tree.LabelledStmt;
import jlang.ast.Tree.Literal;
import jlang.ast.Tree.MethodDef;
import jlang.ast.Tree.NewArray;
import jlang.ast.Tree.ReturnStmt;
import jlang.ast.Tree.NullStmt;
import jlang.ast.Tree.SwitchStmt;
import jlang.ast.Tree.TopLevel;
import jlang.ast.Tree.TypeArray;
import jlang.ast.Tree.CastExpr;
import jlang.ast.Tree.TypeIdent;
import jlang.ast.Tree.VarDef;

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
