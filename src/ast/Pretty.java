package ast;

import java.io.PrintWriter;
import java.util.List;

import type.Type;
import type.TypeTags;
import utils.Convert;
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
 * This class provides a general scheme for descendant recursively traveling the
 * abstract syntax tree for c-flat source file.
 * 
 * Prints out the c-flat source file as a tree.
 * 
 * @author zeng
 */
public class Pretty extends ASTVisitor
{

	public Pretty(PrintWriter out, boolean sourceOutput)
	{
		super();
		this.out = out;
		this.sourceOutput = sourceOutput;
	}

	/**
	 * Set when we are producing source output. If we're not producing source
	 * output, we can sometimes give more detail in the output even though that
	 * detail would not be valid java soruce.
	 */
	private final boolean sourceOutput;

	/**
	 * The output stream on which trees are printed.
	 */
	PrintWriter out;

	/**
	 * Indentation width (can be reassigned from outside).
	 */
	public int width = 4;

	/**
	 * The current left margin.
	 */
	int lmargin = 0;

	/**
	 * The current precedence level.
	 */
	private int prec;

	/**
	 * Align code to be indented to left margin.
	 */
	void align()
	{
		for (int i = 0; i < lmargin; i++)
			out.print(" ");
	}

	/**
	 * Increase left margin by indentation width.
	 */
	void indent()
	{
		lmargin = lmargin + width;
	}

	/**
	 * Decrease left margin by indentation width.
	 */
	void undent()
	{
		lmargin = lmargin - width;
	}

	/**
	 * Enter a new precedence level. Emit a `(' if new precedence level is less
	 * than precedence level so far.
	 * 
	 * @param contextPrec The precedence level in force so far.
	 * @param ownPrec The new precedence level.
	 */
	void open(int contextPrec, int ownPrec)
	{
		if (ownPrec < contextPrec)
			out.print("(");
	}

	/**
	 * Leave precedence level. Emit a `)' if subLoops precedence level is less than
	 * precedence level we revert to.
	 * 
	 * @param contextPrec The precedence level we revert to.
	 * @param ownPrec The subLoops precedence level.
	 */
	void close(int contextPrec, int ownPrec)
	{
		if (ownPrec < contextPrec)
			out.print(")");
	}

	/**
	 * Print string, replacing all non-ascii character with unicode escapes.
	 */
	public void print(String s)
	{
		out.print(Convert.escapeUnicode(s));
	}

	/**
	 * Print new line.
	 */
	public void println()
	{
		out.println();
	}

	/**
	 * Prints out a compilation unit consisting of serial of import clauses,
	 * followed by variable definition, or method definition.
	 * 
	 * @param tree The TopLevel tree.
	 */
	private void printUnit(TopLevel tree)
	{
		for (Tree t : tree.defs)
		{
			printStat(t);
			println();
		}
	}

	/**
	 * Prints out statement.
	 * 
	 * @param tree
	 */
	private void printStat(Tree tree)
	{
		printExpr(tree, TreeInfo.notExpression);
	}

	/**
	 * Prints out expression tree.
	 * The current precedence level.
	 * @param tree
	 * @param prec
	 */
	private void printExpr(Tree tree, int prec)
	{
		int prevPrec = this.prec;
		try
		{
			this.prec = prec;
			if (tree == null)
				print("/*missing*/");
			else
				tree.accept(this);
		}
		finally
		{
			this.prec = prevPrec;
		}
	}

	/**
	 * Derived visitor method: print expression tree at minimum precedence level
	 * for expression.
	 */
	private void printExpr(Tree tree)
	{
		printExpr(tree, TreeInfo.noPrec);
	}

	/**
	 * Prints out the storage class and qualifier of variable and method.
	 */
	private void printFlag(long flags)
	{
		print(TreeInfo.flagNames(flags));
		if ((flags & Flags.StandardFlags) != 0)
			print(" ");
	}

	/**
	 * Derived visitor method: print list of expression trees, separated by
	 * commas.
	 */
	public void printExprs(List<Tree> trees)
	{
		printExprs(trees, ", ");
	}

	/**
	 * Derived visitor method: print list of expression trees, seperated by sep
	 * 
	 * @param trees A list of expression trees.
	 * @param sep the seperator.
	 */
	private void printExprs(List<Tree> trees, String sep)
	{
		if (!trees.isEmpty())
		{
			int idx = 0;
			// prints out the first expression
			printExpr(trees.get(idx));

			for (idx = 1; idx < trees.size(); idx++)
			{
				print(sep + " ");
				printExpr(trees.get(idx));
			}
		}
	}

	/**
	 * Prints out a list of statements, each on seperate line.
	 * 
	 * @param list
	 */
	private void printStats(List<? extends Tree> stats)
	{
		for (Tree stat : stats)
		{
			align();
			printStat(stat);
			println();
		}
	}

	private void printBlock(Block block)
	{
		println();
		print("{");
		indent();
		printStats(block.stats);
		undent();
		align();
		print("}");

	}

	/**
	 * Visits toplevel.
	 * 
	 * @param tree The TopLevel tree.
	 */
	@Override
	public void visitTopLevel(TopLevel tree)
	{
		printUnit(tree);
	}

	/**
	 * Visits import clauses.
	 */
	@Override
	public void visitImport(Import tree)
	{
		print("import");
		printExpr(tree.qualid);
		print(";");
		println();
	}

	/**
	 * Visits method definition clauses.
	 */
	@Override
	public void visitMethodDef(MethodDef tree)
	{
		println();
		align();
		printFlag(tree.flags);
		printExpr(tree.rettype);
		print(" " + tree.name);
		printExprs(tree.params);

		if (tree.body != null)
		{
			print(" ");
			printStat(tree.body);
		}
		else
		{
			print(";");
		}

	}

	@Override
	public void visitVarDef(VarDef tree)
	{
		println();
		align();
		printFlag(tree.flags);
		printExpr(tree.varType);
		print(" " + tree.name);

		if (tree.init != null)
		{
			print(" = ");
			printExpr(tree.init);
		}
		if (prec == TreeInfo.notExpression)
			print(";");
	}

	/**
	 * Skip.
	 */
	@Override
	public void visitSkip(Skip tree)
	{
		print(";");
	}

	/**
	 * Derived visits block statement.
	 */
	@Override
	public void visitBlock(Block tree)
	{
		printBlock(tree);
	}

	@Override
	public void visitIf(If tree)
	{
		print("if ");
		if (tree.cond.tag == Tree.PARENS)
		{
			printExpr(tree.cond);
		}
		else
		{
			print("(");
			printExpr(tree.cond);
			print(")");
		}
		print(" ");
		printStat(tree.thenpart);
		if (tree.elsepart != null)
		{
			print(" else ");
			printStat(tree.elsepart);
		}
	}

	@Override
	public void visitSwitch(Switch tree)
	{
		print("switch ");
		if (tree.selector.tag == Tree.PARENS)
		{
			printExpr(tree.selector);
		}
		else
		{
			print("(");
			printExpr(tree.selector);
			print(")");
		}
		println();
		align();
		print("{");
		println();
		printStats(tree.cases);
		align();
		print("}");
	}

	@Override
	public void visitCase(Case tree)
	{
		if (tree.values == null)
		{
			print("default");
		}
		else
		{
			print("case ");
			printExprs(tree.values);
		}
		print(": ");
		println();
		indent();
		printStat(tree.caseBody);
		undent();
		align();
	}

	@Override
	public void visitForLoop(ForLoop tree)
	{
		print("for (");
		if (!tree.init.isEmpty())
		{
			// variable definition
			Tree first = tree.init.get(0);
			if (first.tag == Tree.VARDEF)
			{
				printExpr(first);
				for (int idx = 1; idx < tree.init.size(); idx++)
				{
					VarDef vdef = (VarDef) tree.init.get(idx);
					print(", " + vdef.name + " = ");
					printExpr(vdef.init);
				}
				// expressions
			}
			else
			{
				printExprs(tree.init);
			}
		}
		print("; ");
		if (tree.cond != null)
			printExpr(tree.cond);
		print("; ");
		printExprs(tree.step);
		print(") ");
		printStat(tree.body);

	}

	@Override
	public void visitBreak(Break tree)
	{
		print("break;");
	}

	@Override
	public void visitContinue(Continue tree)
	{
		print("continue;");

	}

	@Override
	public void visitGoto(Goto tree)
	{
		print("goto");
		if (tree.label != null)
			print(" " + tree.label);
		print(";");
	}

	@Override
	public void visitDoLoop(DoLoop tree)
	{
		print("do");
        printStat(tree.body);
        align();
        print(" while ");
        if (tree.cond.tag == Tree.PARENS) {
            printExpr(tree.cond);
        } else {
            print("(");
            printExpr(tree.cond);
            print(")");
        }
        print(";");

	}

	@Override
	public void visitWhileLoop(WhileLoop tree)
	{
        print("while ");
        if (tree.cond.tag == Tree.PARENS) {
            printExpr(tree.cond);
        } else {
            print("(");
            printExpr(tree.cond);
            print(")");
        }
        print(" ");
        printStat(tree.body);

	}

	@Override
	public void visitLabelled(Labelled tree)
	{
        print(tree.label + ": ");
        printStat(tree.body);

	}

	@Override
	public void visitReturn(Return tree)
	{
		print("return");
		if (tree.expr != null) {
			print(" ");
			printExpr(tree.expr);
		}
			
		print(";");

	}

	/**
	@Override
	public void visitSelect(Select tree)
	{
        printExpr(tree.selected, TreeInfo.postfixPrec);
        print("." + tree.name);

	}
	*/
	@Override
	public void visitApply(Apply tree)
	{
        printExpr(tree.meth);
        print("(");
        printExprs(tree.args);
        print(")");

	}

	@Override
	public void visitAssign(Assign tree)
	{
        open(prec, TreeInfo.assignPrec);
        printExpr(tree.lhs, TreeInfo.assignPrec + 1);
        print(" = ");
        printExpr(tree.rhs, TreeInfo.assignPrec);
        close(prec, TreeInfo.assignPrec);

	}

	@Override
	public void visitExec(Exec tree)
	{
        printExpr(tree.expr);
        if (prec == TreeInfo.notExpression)
            print(";");
	}

	@Override
	public void visitConditional(Conditional tree)
	{
        open(prec, TreeInfo.condPrec);
        printExpr(tree.cond, TreeInfo.condPrec);
        print(" ? ");
        printExpr(tree.truepart, TreeInfo.condPrec);
        print(" : ");
        printExpr(tree.falsepart, TreeInfo.condPrec);
        close(prec, TreeInfo.condPrec);
	}

	@Override
	public void visitParens(Parens tree)
	{
		print(")");
		printExpr(tree.expr);
		print(")");
	}


    public String operatorName(int tag) {
        switch (tag) {
        case Tree.POS:
            return "+";

        case Tree.NEG:
            return "-";

        case Tree.NOT:
            return "!";

        case Tree.COMPL:
            return "~";

        case Tree.PREINC:
            return "++";

        case Tree.PREDEC:
            return "--";

        case Tree.POSTINC:
            return "++";

        case Tree.POSTDEC:
            return "--";

        case Tree.OR:
            return "||";

        case Tree.AND:
            return "&&";

        case Tree.EQ:
            return "==";

        case Tree.NE:
            return "!=";

        case Tree.LT:
            return "<";

        case Tree.GT:
            return ">";

        case Tree.LE:
            return "<=";

        case Tree.GE:
            return ">=";

        case Tree.BITOR:
            return "|";

        case Tree.BITXOR:
            return "^";

        case Tree.BITAND:
            return "&";

        case Tree.SL:
            return "<<";

        case Tree.SR:
            return ">>";

        case Tree.PLUS:
            return "+";

        case Tree.MINUS:
            return "-";

        case Tree.MUL:
            return "*";

        case Tree.DIV:
            return "/";

        case Tree.MOD:
            return "%";

        default:
            throw new Error();

        }
    }

    public void visitAssignop(Assignop tree) {
        open(prec, TreeInfo.assignopPrec);
        printExpr(tree.lhs, TreeInfo.assignopPrec + 1);
        print(" " + operatorName(tree.tag - Tree.ASGOffset) + "= ");
        printExpr(tree.rhs, TreeInfo.assignopPrec);
        close(prec, TreeInfo.assignopPrec);
    }

	@Override
	public void visitUnary(Unary tree)
	{
		int ownprec = TreeInfo.opPrec(tree.tag);
		String opname = operatorName(tree.tag).toString();
		open(prec, ownprec);
		// prefix operator
		if (tree.tag <= Tree.PREDEC)
		{
			print(opname);
			printExpr(tree.arg, ownprec);
		}
		// suffix operator
		else
		{
			printExpr(tree.arg, ownprec);
			print(opname);
		}
		close(prec, ownprec);

	}

	@Override
	public void visitBinary(Binary tree)
	{
        int ownprec = TreeInfo.opPrec(tree.tag);
        String opname = operatorName(tree.tag).toString();
        open(prec, ownprec);
        printExpr(tree.lhs, ownprec);
        print(" " + opname + " ");
        printExpr(tree.rhs, ownprec + 1);
        close(prec, ownprec);

	}

	@Override
	public void visitTypeCast(TypeCast tree)
	{
        open(prec, TreeInfo.prefixPrec);
        print("(");
        printExpr(tree.clazz);
        print(")");
        printExpr(tree.expr, TreeInfo.prefixPrec);
        close(prec, TreeInfo.prefixPrec);

	}

	@Override
	public void visitIndexed(Indexed tree)
	{
        printExpr(tree.indexed, TreeInfo.postfixPrec);
        print("[");
        printExpr(tree.index);
        print("]");

	}

	@Override
	public void visitTypeArray(TypeArray tree)
	{
        printExpr(tree.elemtype);
        print("[]");

	}

	@Override
	public void visitTypeIdent(TypeIdent tree)
	{
        switch (tree.typetag) {
        case TypeTags.BYTE:
            print("byte");
            break;

        case TypeTags.CHAR:
            print("char");
            break;

        case TypeTags.SHORT:
            print("short");
            break;

        case TypeTags.INT:
            print("int");
            break;

        case TypeTags.LONG:
            print("long");
            break;

        case TypeTags.FLOAT:
            print("float");
            break;

        case TypeTags.DOUBLE:
            print("double");
            break;

        case TypeTags.BOOL:
            print("bool");
            break;

        case TypeTags.VOID:
            print("void");
            break;
        default:
            print("error");
            break;

        }

	}

	@Override
	public void visitLiteral(Literal tree)
	{
        switch (tree.typetag) {
        case Type.INT:
            print(tree.value.toString());
            break;

        case Type.LONG:
            print(tree.value.toString() + "L");
            break;

        case Type.FLOAT:
            print(tree.value.toString() + "F");
            break;

        case Type.DOUBLE:
            print(tree.value.toString());
            break;

        case Type.CHAR:
            print("\'" + Convert.quote(
                    String.valueOf((char)((Number) tree.value).intValue())) + "\'");
            break;
        default:
            print(tree.value.toString());
        }

	}

	@Override
	public void visitIdent(Ident tree)
	{
        print(tree.name.toString());

	}

	@Override
	public void visitNewArray(NewArray tree)
	{
        if (tree.elemtype != null) {
            print("new ");
            int n = 0;
            Tree elemtype = tree.elemtype;
            while (elemtype.tag == Tree.ARRAYTYPE) {
                n++;
                elemtype = ((TypeArray) elemtype).elemtype;
            }
            printExpr(elemtype);
            for (Tree l : tree.dims) {
                print("[");
                printExpr(l);
                print("]");
            }
            for (int i = 0; i < n; i++) {
                print("[]");
            }
            if (tree.elems != null) {
                print("[]");
            }
        }
        if (tree.elems != null) {
            print("{");
            printExprs(tree.elems);
            print("}");
        }
	}

	@Override
	public void visitErroneous(Erroneous erroneous)
	{
        print("(ERROR)");

	}
	
    public void visitTree(Tree tree) {
        print("(UNKNOWN: " + tree + ")");
        println();
    }

}
