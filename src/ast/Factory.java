package ast; 

import java.util.List;

import utils.Name;
import ast.Tree.*;

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

   Labelled Labelled(Name label, Tree body);

   Switch Switch(Tree selector, List<Case> cases);

   Case Case(List<Tree> values, Tree caseBody);

   Conditional Conditional(Tree cond, Tree thenpart, Tree elsepart);

   If If(Tree cond, Tree thenpart, Tree elsepart);

   Exec Exec(Tree expr);

   Break Break();

   Continue Continue();

   Goto Goto(Name label);
   
   Return Return(Tree expr);     

   Apply Apply(Tree fn, List<Tree> args);

   Parens Parens(Tree expr);

   Assign Assign(Tree lhs, Tree rhs);

   Assignop Assignop(int opcode, Tree lhs, Tree rhs);

   Unary Unary(int opcode, Tree arg);

   Binary Binary(int opcode, Tree lhs, Tree rhs);

   TypeCast TypeCast(Tree expr, Tree type);

   Indexed Indexed(Tree indexed, Tree index);

   Ident Ident(Name idname);

   Literal Literal(int tag, Object value);

   TypeIdent TypeIdent(int typetag);

   TypeArray TypeArray(Tree elemtype);
   
   NewArray NewArray(Tree elemtype, List<Tree> dims, List<Tree> elems);

   Erroneous Erroneous();
}
