package jlang.ast;

import jlang.ast.Tree.TopLevel;
import tools.Convert;

import java.io.PrintWriter;
import java.util.List;

/**
 * <p>
 * This class provides a general scheme for descendant recursively traveling the
 * abstract syntax tree for C source file.
 * <p>
 * Prints out the C source file as a tree.
 *
 * @author xlous.zeng
 * @version 0.1
 */
public final class Pretty extends StmtVisitor
{
    public Pretty(PrintWriter out, boolean sourceOutput)
    {
        super();
        this.out = out;
        this.sourceOutput = sourceOutput;
    }

    /**
     * Set when we are producing source output. IfStmt we're not producing source
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
     * Enter a new precedence level. emit a `(' if new precedence level is less
     * than precedence level so far.
     *
     * @param contextPrec The precedence level in force so far.
     * @param ownPrec     The new precedence level.
     */
    void open(int contextPrec, int ownPrec)
    {
        if (ownPrec < contextPrec)
            out.print("(");
    }

    /**
     * Leave precedence level. emit a `)' if subLoops precedence level is less than
     * precedence level we revert to.
     *
     * @param contextPrec The precedence level we revert to.
     * @param ownPrec     The subLoops precedence level.
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

    }

    /**
     * Prints out statement.
     *
     * @param tree
     */
    private void printStat(Tree tree)
    {

    }

    /**
     * Prints out expression tree.
     * The current precedence level.
     *
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

    }

    /**
     * Prints out the storage class and qualifier of variable and method.
     */
    private void printFlag(long flags)
    {
        //print(TreeInfo.flagNames(flags));
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
     * @param sep   the seperator.
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
     * @param stats
     */
    private void printStats(Tree.Stmt[] stats)
    {
        for (Tree stat : stats)
        {
            align();
            printStat(stat);
            println();
        }
    }

    private void printBlock(Tree.CompoundStmt block)
    {
        println();
        print("{");
        indent();
        printStats(block.getBody());
        undent();
        align();
        print("}");

    }
}
