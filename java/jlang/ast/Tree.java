package jlang.ast;

import tools.Util;
import backend.value.BasicBlock;
import jlang.basic.SourceManager;
import jlang.clex.IdentifierInfo;
import jlang.sema.*;
import jlang.sema.BinaryOperatorKind;
import jlang.sema.Decl.*;
import jlang.support.PrintingPolicy;
import jlang.support.SourceLocation;
import jlang.support.SourceRange;
import jlang.type.*;
import tools.*;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static jlang.ast.CastKind.*;
import static jlang.ast.Tree.Expr.IsLvalueResult.*;
import static jlang.ast.Tree.Expr.IsModifiableLvalueResult.*;
import static jlang.ast.Tree.ExprObjectKind.OK_Ordinary;
import static jlang.ast.Tree.ExprValueKind.EVK_RValue;
import static jlang.sema.BinaryOperatorKind.*;
import static jlang.sema.UnaryOperatorKind.*;

/**
 * Root class for abstract syntax tree nodes. It provides definitions for
 * specific tree nodes as subclasses. There are 40 such subclasses.
 * 
 * Each subclass is highly standardized. It generally isDeclScope only tree fields
 * for the syntactic sub-components of the node. Some classes that represents
 * identifier usesList or definitions also define a Symbol field that denotes the
 * represented identifier. Classes for non-local jumps also carry the jump
 * targetAbstractLayer as a field. The root class {@link Tree} itself defines fields for the
 * tree's jlang.type and position. No other fields are kept in a tree node; instead
 * parameters are passed to methods accessing the node.
 * 
 * The only method defined in subclasses is 'visit' which applies a given
 * visitor to the tree. The actual tree processing is done by visitor classes in
 * other package. The abstract class {@link StmtVisitor} is the abstract root
 * class for visiting different tree.
 * 
 * @author Jianping Zeng
 * @version 1.0
 *
 */
public abstract class Tree implements StmtClass
{
    /**
	 * A further classification of the kind of object referenced by an
	 * l-value or x-value.
	 */
	public enum ExprObjectKind
	{
		/// An ordinary object is located at an address in memory.
		OK_Ordinary,

		/// A bitfield object is a bitfield on a C record.
		OK_BitField,
	}

	/**
	 * The tc that represents the kind of this tree.
	 */
	public int stmtClass;

	/**
	 * Constructor. Initialize tree with given tc.
	 *
	 * @param stmtClass
	 */
	public Tree(int stmtClass)
	{
		this.stmtClass = stmtClass;
	}

	/**
	 * Converts a tree to a pretty printed string.
	 */
	@Override
	public String toString()
	{
		StringWriter s = new StringWriter();
		this.accept(new Pretty(new PrintWriter(s), false));
		return s.toString();
	}

	public int getStmtClass()
    {
        return stmtClass;
    }

	/**
	 * Visit this tree with a given visitor.
	 */
	public abstract void accept(StmtVisitor v);

	public void dumpPretty(ASTContext ctx)
	{
		printPretty(System.err, ctx, null, new PrintingPolicy(ctx.getLangOptions()));
	}

	public void printPretty(PrintStream os, ASTContext ctx,
			PrinterHelper helper,
			PrintingPolicy policy)
	{
		printPretty(os, ctx, helper, policy, 0);
	}

	public void printPretty(PrintStream os, ASTContext ctx,
			PrinterHelper helper,
			PrintingPolicy policy,
			int indentation)
	{
	    if (policy.dump)
        {
            dump(ctx.getSourceManager());
            return;
        }

        StmtPrinter printer = new StmtPrinter(os, ctx, helper, policy, indentation);
	    printer.visit(this);
    }

    /**
     * This does a local dump of the specified AST fragment It dumps
     * the specified node and a few nodes underneath it, but not the whole
     * subtree. This is useful in a debugger.
     * @param sourceMgr
     */
    public void dump(SourceManager sourceMgr)
    {
        StmtDumper dumper = new StmtDumper(sourceMgr, System.err, 4);;
        dumper.dumpSubTree(this);
        System.err.println();
    }

    /**
     * This does a local dump of the specified AST fragment It dumps
     * the specified node and a few nodes underneath it, but not the whole
     * subtree. This is useful in a debugger.
     */
    public void dump()
    {
        StmtDumper dumper = new StmtDumper(null, System.err, 4);
        dumper.dumpSubTree(this);
        System.err.println();
    }

	/**
	 * Gets the number of children stmt enclosed in this stmt.
	 * @return
	 */
	public int getNumChildren()
    {
    	return 0;
    }

	/**
	 * Obtains the children stmt in the specified position.
	 * @param index
	 * @return
	 */
	public Tree getChildren(int index)
    {
    	return null;
    }

    public String getStmtClassName()
    {
    	/*
    	for (int i = 0, e = StmtClassNames.length; i != e; i++)
    		System.out.printf("%d, %s\n", i, StmtClassNames[i]);
		*/
        return StmtClassNames[stmtClass];
    }

	/**
	 * Everything in one source file is kept in a TopLevel structure.
	 *
	 * @author Jianping Zeng
	 * @version 0.1
	 */
	public static class TopLevel extends Tree
	{
		/**
		 * Represents all declarations in a compilation unit file.
		 */
		private ArrayList<DeclStmt> decls;
		private String sourceFile;

		/**
		 * Constructs TopLevel tree node that represents a source file.
		 *
		 * @param decls all of definitions in a source file.
		 * @param sourceFile the getIdentifier of source file.
		 */
		public TopLevel(ArrayList<DeclStmt> decls, String sourceFile)
		{
			super(TopLevelClass);
			this.decls = decls;
			this.sourceFile = sourceFile;
		}

		public String getSourceFile() { return sourceFile; }
		public void setSourceFile(String file) {sourceFile = file;}

		public ArrayList<DeclStmt> getDecls() {return decls;}
		public void setDecls(ArrayList<DeclStmt> decls) {this.decls = decls;}

        @Override
		public void accept(StmtVisitor v)
		{
		}

		/**
		 * Gets the number of children stmt enclosed in this stmt.
		 * @return
		 */
		@Override
		public int getNumChildren()
		{
			return decls.size();
		}

		/**
		 * Obtains the children stmt in the specified position.
		 * @param index
		 * @return
		 */
		@Override
		public Tree getChildren(int index)
		{
			Util.assertion( index >= 0 && index < decls.size());
			return decls.get(index);
		}
	}

	public abstract static class Stmt extends Tree
	{
		Stmt(int tag)
		{
			super(tag);
		}

		public abstract SourceRange getSourceRange();

		public SourceLocation getLocStart()
		{
			return getSourceRange().getBegin();
		}

		public SourceLocation getLocEnd()
		{
			return getSourceRange().getEnd();
		}
	}
	/**
     * This is the null statement ";": C99 6.8.3p3.
	 * @author Jianping Zeng
	 * @version 0.1
	 */
	public static class NullStmt extends Stmt
	{
	    private SourceLocation semiLoc;
		public NullStmt(SourceLocation semiLoc)
		{
			super(NullStmtClass);
            this.semiLoc = semiLoc;
		}

		public void accept(StmtVisitor v)
		{
			v.visitNullStmt(this);
		}

		@Override
		public SourceRange getSourceRange()
		{
			return new SourceRange(semiLoc, semiLoc);
		}

        public SourceLocation getSemiLoc()
        {
            return semiLoc;
        }

        public void setSemiLoc(SourceLocation semiLoc)
        {
            this.semiLoc = semiLoc;
        }
    }

    /**
     * Adaptor class for mixing declarations with statements and
     * expressions. For example, CompoundStmt mixes statements, expressions
     * and declarations (variables, types). Another example is ForStmt, where
     * the first statement can be an expression or a declaration.
     */
    public static class DeclStmt extends Stmt implements Iterable<Decl>
    {
        private ArrayList<Decl> decls;
        private SourceLocation declStart, declEnd;
	    public DeclStmt(
			    ArrayList<Decl> decls,
			    SourceLocation declStart,
			    SourceLocation declEnd)
	    {
		    super(DeclStmtClass);
            this.decls = decls;
            this.declStart = declStart;
            this.declEnd = declEnd;
	    }

	    public boolean isSingleDecl()
        {
            return decls.size() == 1;
        }

        public Decl getSingleDecl()
        {
            Util.assertion( isSingleDecl());
            return decls.get(0);
        }

        public ArrayList<Decl> getDeclGroup()
        {
            return decls;
        }

        public void accept(StmtVisitor v)
        {
            v.visitDeclStmt(this);
        }

        public Iterator<Decl> iterator()
        {
            return decls.iterator();
        }

        public SourceLocation getStartLoc()
        {
            return declStart;
        }

        public void setStartLoc(SourceLocation loc)
        {
            declStart = loc;
        }

        public SourceLocation getEndLoc()
        {
            return declEnd;
        }

        public void setEndLoc(SourceLocation loc)
        {
            declEnd = loc;
        }

        @Override
        public SourceRange getSourceRange()
        {
            return new SourceRange(declStart, declEnd);
        }
    }

	/**
	 * A statement block.
	 *
	 */
	public static class CompoundStmt extends Stmt
	{
		private Stmt[] stats;
		private SourceLocation rBraceLoc;
		private SourceLocation lBraceLoc;

		public CompoundStmt(
		        List<Stmt> stats,
				SourceLocation l,
				SourceLocation r)
		{
			super(CompoundStmtClass);
			this.stats = new Stmt[stats.size()];
			stats.toArray(this.stats);
            this.rBraceLoc = r;
			this.lBraceLoc = l;
		}

		public void accept(StmtVisitor v)
		{
			v.visitCompoundStmt(this);
		}

        public Stmt[] getBody()
        {
            return stats;
        }

        public boolean bodyEmpty()
        {
            return stats.length == 0;
        }
        public SourceLocation getRBraceLoc()
		{
			return rBraceLoc;
		}

        @Override
        public SourceRange getSourceRange()
        {
            return new SourceRange(lBraceLoc, rBraceLoc);
        }

        public void setBody(Stmt[] body)
        {
            if (body != null)
            {
                stats = new Stmt[body.length];
                System.arraycopy(body, 0, stats, 0, body.length);
            }
            else
                stats = null;
        }

        public SourceLocation getLBraceLoc()
		{
			return lBraceLoc;
		}
        /**
         * Gets the number of children stmt enclosed in this stmt.
         * @return
         */
        @Override
        public int getNumChildren()
        {
            return getBody().length;
        }

        /**
         * Obtains the children stmt in the specified position.
         * @param index
         * @return
         */
        @Override
        public Tree getChildren(int index)
        {
            Util.assertion( index >= 0 && index < getNumChildren());
            return stats[index];
        }
    }

	/**
	 * Selects through packages or class for future.
	 */
	public static class SelectStmt extends Stmt
	{
		/**
		 * selected Tree hierarchie
		 */
		public Tree selected;
		/**
		 * getIdentifier of field to select
		 */
		public IdentifierInfo name;

		public SelectStmt(Tree selected, IdentifierInfo name)
		{
			super(SelectExprClass);
			this.selected = selected;
			this.name = name;
		}

		public void accept(StmtVisitor v)
		{
			v.visitSelectStmt(this);
		}

        @Override
        public SourceRange getSourceRange()
        {
            return null;
        }
    }
	/**
	 * A do loop
	 */
	public static class DoStmt extends Stmt
	{
	    private static final int BODY = 0;
	    private static final int COND = 1;
	    private static final int END_EXPR = 2;

	    private Stmt[] subExprs;
        public SourceLocation doLoc, whileLoc, rParenLoc;

		public DoStmt(Stmt body, Expr cond,
				SourceLocation doLoc,
                SourceLocation whileLoc,
				SourceLocation rParenLoc)
		{
			super(DoStmtClass);
			subExprs = new Stmt[END_EXPR];
			subExprs[BODY] = body;
			subExprs[COND] = cond;
            this.doLoc = doLoc;
            this.whileLoc = whileLoc;
            this.rParenLoc = rParenLoc;
		}

		public SourceRange getSourceRange()
        {
            return new SourceRange(doLoc, rParenLoc);
        }

		public void accept(StmtVisitor v)
		{
			v.visitDoStmt(this);
		}

        public Stmt getBody()
        {
            return subExprs[BODY];
        }

        public void setBody(Stmt body)
        {
            subExprs[BODY] = body;
        }

        public Expr getCond()
        {
            return (Expr) subExprs[COND];
        }

        public void setCond(Expr cond)
        {
            subExprs[COND] = cond;
        }

        public SourceLocation getDoLoc()
        {
            return doLoc;
        }

        public void setDoLoc(SourceLocation doLoc)
        {
            this.doLoc = doLoc;
        }

        public SourceLocation getWhileLoc()
        {
            return whileLoc;
        }

        public void setWhileLoc(SourceLocation whileLoc)
        {
            this.whileLoc = whileLoc;
        }

        public SourceLocation getRParenLoc()
        {
            return rParenLoc;
        }

        public void setRParenLoc(SourceLocation rParenLoc)
        {
            this.rParenLoc = rParenLoc;
        }

        /**
         * Gets the number of children stmt enclosed in this stmt.
         * @return
         */
        @Override
        public int getNumChildren()
        {
            return subExprs.length;
        }

        /**
         * Obtains the children stmt in the specified position.
         * @param index
         * @return
         */
        public Tree getChildren(int index)
        {
            Util.assertion( index >= 0 && index < getNumChildren());
            return subExprs[index];
        }
    }

	/**
	 * A for loop.
	 */
	public static class ForStmt extends Stmt
    {
        private final static int INIT = 0;
        private final static int COND = 1;
        private final static int STEP = 2;
        private final static int BODY = 3;
        private final static int END_EXPR = 4;

        private Stmt[] subExprs;
        public final SourceLocation lParenLoc, rParenLoc, forLoc;

        public ForStmt(SourceLocation forLoc, SourceLocation lParenLoc,
                Stmt init, Expr cond, Expr step, Stmt body, SourceLocation rParenLoc)
        {
            super(ForStmtClass);
            subExprs = new Stmt[END_EXPR];
            subExprs[INIT] = init;
            subExprs[COND] = cond;
            subExprs[STEP] = step;
            subExprs[BODY] = body;
            this.lParenLoc = lParenLoc;
            this.forLoc = forLoc;
            this.rParenLoc = rParenLoc;
        }

        public void accept(StmtVisitor v)
        {
        }

        public Stmt getInit()
        {
            return subExprs[INIT];
        }

        public void setInit(Stmt init)
        {
            subExprs[INIT] = init;
        }

        public Expr getCond()
        {
            return (Expr) subExprs[COND];
        }

        public void setCond(Expr cond)
        {
            subExprs[COND] = cond;
        }

        public Expr getStep()
        {
            return (Expr) subExprs[STEP];
        }

        public void setStep(Expr step)
        {
            subExprs[STEP] = step;
        }

        public Stmt getBody()
        {
            return subExprs[BODY];
        }

        public void setBody(Stmt body)
        {
            subExprs[BODY] = body;
        }

        @Override public SourceRange getSourceRange()
        {
            return new SourceRange(forLoc, getBody().getLocEnd());
        }

        /**
         * Gets the number of children stmt enclosed in this stmt.
         *
         * @return
         */
        @Override public int getNumChildren()
        {
            return subExprs.length;
        }

        /**
         * Obtains the children stmt in the specified position.
         *
         * @param index
         * @return
         */
        public Tree getChildren(int index)
        {
            Util.assertion( index >= 0 && index < getNumChildren());
            return subExprs[index];
        }
    }
	/**
	 * A while loop
	 */
	public static class WhileStmt extends Stmt
	{
	    private final static int COND = 0;
	    private final static int BODY = 1;
	    private final static int END_EXPR = 2;
	    private Stmt[] subExprs;

        public SourceLocation whileLoc;
		public WhileStmt(Expr cond, Stmt body, SourceLocation whileLoc)
		{
			super(WhileStmtClass);
			subExprs = new Stmt[END_EXPR];
			subExprs[COND] = cond;
			subExprs[BODY] = body;
            this.whileLoc = whileLoc;
		}

		public void accept(StmtVisitor v)
		{
			v.visitWhileStmt(this);
		}

        public Expr getCond()
        {
            return (Expr) subExprs[COND];
        }

        public void setCond(Expr cond)
        {
            subExprs[COND] = cond;
        }

        public Stmt getBody()
        {
            return subExprs[BODY];
        }

        public void setBody(Stmt body)
        {
            subExprs[BODY] = body;
        }

        public SourceLocation getWhileLoc()
        {
            return whileLoc;
        }

        public void setWhileLoc(SourceLocation whileLoc)
        {
            this.whileLoc = whileLoc;
        }

        @Override
        public SourceRange getSourceRange()
        {
            return new SourceRange(whileLoc, getBody().getLocEnd());
        }

        /**
         * Gets the number of children stmt enclosed in this stmt.
         * @return
         */
        @Override
        public int getNumChildren()
        {
            return subExprs.length;
        }

        /**
         * Obtains the children stmt in the specified position.
         * @param index
         * @return
         */
        public Tree getChildren(int index)
        {
            Util.assertion( index >=0 && index < getNumChildren());
            return subExprs[index];
        }
    }

	/**
	 * A labelled expression or statement.
	 */
	public static class LabelStmt extends Stmt
	{
		public LabelDecl label;
		public Stmt body;
        public final SourceLocation identLoc;
		/** The corresponding basic block of this label.*/
		public BasicBlock corrBB;

		public LabelStmt(LabelDecl label, Stmt body, SourceLocation loc)
		{
			super(LabelledStmtClass);
			this.label = label;
			this.body = body;
            this.identLoc = loc;
		}

		public void accept(StmtVisitor v)
		{
			v.visitLabelledStmt(this);
		}

        public SourceLocation getIdentLoc()
        {
            return identLoc;
        }

        IdentifierInfo getID()
        {
            return label.getIdentifier();
        }

        public String getName()
        {
            return label.getIdentifier().getName();
        }

        public Stmt getSubStmt()
        {
            return body;
        }

        @Override
        public SourceRange getSourceRange()
        {
            return new SourceRange(identLoc, body.getLocEnd());
        }

        /**
         * Gets the number of children stmt enclosed in this stmt.
         * @return
         */
        @Override
        public int getNumChildren()
        {
            return 1;
        }

        /**
         * Obtains the children stmt in the specified position.
         * @param index
         * @return
         */
        @Override
        public Tree getChildren(int index)
        {
            Util.assertion( index >= 0 && index < getNumChildren());
            return body;
        }
    }

	/**
	 * An "if ( ) { } else { }" block
	 */
	public static class IfStmt extends Stmt
	{
        private static final int COND = 0;
        private static final int THEN = 1;
        private static final int ELSE = 2;
        private static final int END_EXPR = 3;

        private Stmt[] subStmts;
        private SourceLocation ifLoc;
        private SourceLocation elseLoc;

        public IfStmt(
                Expr cond,
                Stmt thenpart,
                Stmt elsepart,
                SourceLocation ifLoc)
        {
            this(cond, thenpart, elsepart, ifLoc, new SourceLocation());
        }

        public IfStmt(
				Expr cond,
				Stmt thenpart,
				Stmt elsepart,
				SourceLocation ifLoc,
                SourceLocation elseLoc)
		{
			super(IfStmtClass);
			subStmts = new Stmt[END_EXPR];
			subStmts[COND] = cond;
			subStmts[THEN] = thenpart;
			subStmts[ELSE] = elsepart;
            this.ifLoc = ifLoc;
            this.elseLoc = elseLoc;
		}

		public void accept(StmtVisitor v){ v.visitIfStmt(this);}

		public Expr getCond()
		{
			return (Expr) subStmts[COND];
		}

        public Stmt getThenPart()
        {
            return subStmts[THEN];
        }

		public Stmt getElsePart()
        {
            return subStmts[ELSE];
        }

		public SourceLocation getIfLoc()
        {
            return  ifLoc;
        }

        public SourceLocation getElseLoc()
        {
            return elseLoc;
        }

        public void setCond(Expr val)
        {
            subStmts[COND] = val;
        }

        public void setThenPart(Stmt then)
        {
            subStmts[THEN] = then;
        }

        public void setElsePart(Stmt then)
        {
            subStmts[ELSE] = then;
        }

        @Override
        public SourceRange getSourceRange()
        {
            if (getElsePart() != null)
                return new SourceRange(ifLoc, getElsePart().getLocEnd());
            else
                return new SourceRange(ifLoc, getThenPart().getLocEnd());
        }

        /**
         * Gets the number of children stmt enclosed in this stmt.
         * @return
         */
        public int getNumChildren()
        {
            return subStmts.length;
        }

        /**
         * Obtains the children stmt in the specified position.
         * @param index
         * @return
         */
        public Tree getChildren(int index)
        {
            Util.assertion( index >=0 && index < getNumChildren());
            return subStmts[index];
        }
    }

	/**
	 * A "switch ( ) { }" construction.
	 */
	public static class SwitchStmt extends Stmt
	{
	    private final static int COND = 0;
	    private final static int BODY = 1;
	    private final static int END_EXPR = 2;

	    private Stmt[] subExprs;

        // This points to a linked list of case and default statements.
		private SwitchCase firstCase;

        private SourceLocation switchLoc;
        /**
         * A flag which indicates whether all enum values are covered in current
         * switch statement by condition X - 'switch (X)'.
         */
        private boolean allEnumCasesCovered;

		public SwitchStmt(Expr cond, SourceLocation switchLoc)
		{
			super(SwitchStmtClass);
			subExprs = new Stmt[END_EXPR];
			subExprs[COND] = cond;
            this.switchLoc = switchLoc;
		}

		public void setBody(Stmt body)
        {
            subExprs[BODY] = body;
        }

        public Stmt getBody()
        {
            return subExprs[BODY];
        }

        public Expr getCond()
        {
            return (Expr) subExprs[COND];
        }

        public void setCond(Expr cond)
        {
            subExprs[COND] = cond;
        }

        public SourceLocation getSwitchLoc()
        {
            return switchLoc;
        }

		public void accept(StmtVisitor v)
		{
			v.visitSwitchStmt(this);
		}

        public SwitchCase getSwitchCaseList()
        {
            return firstCase;
        }

        public void setSwitchCaseList(SwitchCase cs)
        {
            firstCase = cs;
        }

        public void setAllEnumCasesCovered()
        {
            allEnumCasesCovered = true;
        }

        public boolean getAllEnumCasesCovered()
        {
            return allEnumCasesCovered;
        }

        public void addSwitchCase(SwitchCase sc)
        {
            Util.assertion(sc.getNextSwitchCase() == null,  "case/default already added to a switch");

            sc.setNextSwitchCase(firstCase);
            firstCase = sc;
        }

        @Override
        public SourceRange getSourceRange()
        {
            return new SourceRange(switchLoc, subExprs[BODY].getLocEnd());
        }

        /**
         * Gets the number of children stmt enclosed in this stmt.
         * @return
         */
        public int getNumChildren()
        {
            return subExprs.length;
        }

        /**
         * Obtains the children stmt in the specified position.
         * @param index
         * @return
         */
        public Tree getChildren(int index)
        {
            Util.assertion( index >=0 && index < getNumChildren());
            return subExprs[index];
        }
    }

    public static abstract class SwitchCase extends Stmt
    {
        protected SwitchCase nextSwitchCase;

        public SwitchCase(int tag)
        {
            super(tag);
        }

        public SwitchCase getNextSwitchCase()
        {
            return nextSwitchCase;
        }

        public void setNextSwitchCase(SwitchCase nextSwitchCase)
        {
            this.nextSwitchCase = nextSwitchCase;
        }

        public abstract SourceLocation getCaseLoc();
        public abstract SourceLocation getColonLoc();
        public abstract Stmt getSubStmt();

        @Override
        public SourceRange getSourceRange()
        {
            return new SourceRange();
        }
    }
	/**
	 * A "case  :" of a switch.
	 */
	public static class CaseStmt extends SwitchCase
    {
        private static final int LHS = 0;
        private static final int SUBSTMT = 1;
        private static final int END_EXPR = 2;

        private Stmt[] subExprs;

        public final SourceLocation caseLoc;
        public final SourceLocation colonLoc;

        public CaseStmt(
		        Expr lhs,
		        Stmt caseBody,
		        SourceLocation caseLoc,
		        SourceLocation colonLoc)
        {
            super(CaseStmtClass);
            subExprs = new Stmt[END_EXPR];
            subExprs[LHS] = lhs;
            subExprs[SUBSTMT] = caseBody;
            this.caseLoc = caseLoc;
            this.colonLoc = colonLoc;
        }

        public SourceLocation getCaseLoc()
        {
            return caseLoc;
        }

        public SourceLocation getColonLoc()
        {
            return colonLoc;
        }

		public void accept(StmtVisitor v)
		{
			v.visitCaseStmt(this);
		}

        public Stmt getSubStmt()
        {
            return subExprs[SUBSTMT];
        }

        public Expr getCondExpr()
        {
            return (Expr)subExprs[LHS];
        }

        public void setCondExpr(Expr val)
        {
            subExprs[LHS] = val;
        }

        public void setSubStmt(Stmt stmt)
        {
            subExprs[SUBSTMT] = stmt;
        }

        @Override
        public SourceRange getSourceRange()
        {
            CaseStmt cs = this;
            while (cs.getSubStmt() instanceof CaseStmt)
                cs = (CaseStmt) cs.getSubStmt();

            return new SourceRange(caseLoc, cs.getSubStmt().getLocEnd());
        }

        /**
         * Gets the number of children stmt enclosed in this stmt.
         * @return
         */
        @Override
        public int getNumChildren()
        {
            return END_EXPR;
        }

        /**
         * Obtains the children stmt in the specified position.
         * @param index
         * @return
         */
        @Override
        public Tree getChildren(int index)
        {
            Util.assertion( index >= 0 && index < getNumChildren());
            return subExprs[index];
        }
    }

    public static class DefaultStmt extends SwitchCase
    {
        public final SourceLocation defaultLoc;
        public final SourceLocation colonLoc;
        public Stmt subStmt;
        public DefaultStmt(
		        SourceLocation defaultLoc,
		        SourceLocation colonLoc,
		        Stmt subStmt)
        {
            super(DefaultStmtClass);
            this.defaultLoc =defaultLoc;
            this.colonLoc = colonLoc;
            this.subStmt = subStmt;
        }

        @Override
        public SourceLocation getCaseLoc()
        {
            return colonLoc;
        }

        public SourceLocation getColonLoc()
        {
            return defaultLoc;
        }

        @Override
        public void accept(StmtVisitor v)
        {
            v.visitDefaultStmt(this);
        }

        public Stmt getSubStmt()
        {
            return subStmt;
        }

        @Override
        public SourceRange getSourceRange()
        {
            return new SourceRange(defaultLoc, subStmt.getLocEnd());
        }

        /**
         * Gets the number of children stmt enclosed in this stmt.
         * @return
         */
        @Override
        public int getNumChildren()
        {
            return 1;
        }

        /**
         * Obtains the children stmt in the specified position.
         * @param index
         * @return
         */
        @Override
        public Tree getChildren(int index)
        {
            Util.assertion( index >= 0 && index < getNumChildren());
            return subStmt;
        }
    }

	/**
	 * A break from a loop or switch.
	 */
	public static class BreakStmt extends Stmt
	{
		private SourceLocation breakLoc;

		public BreakStmt(SourceLocation breakLoc)
		{
			super(BreakStmtClass);
			this.breakLoc = breakLoc;
		}

		public void accept(StmtVisitor v)
		{
			v.visitBreakStmt(this);
		}

        public SourceLocation getBreakLoc()
        {
            return breakLoc;
        }

        public void setBreakLoc(SourceLocation breakLoc)
        {
            this.breakLoc = breakLoc;
        }

        @Override
        public SourceRange getSourceRange()
        {
            return new SourceRange(breakLoc);
        }

        /**
         * Gets the number of children stmt enclosed in this stmt.
         * @return
         */
        @Override
        public int getNumChildren()
        {
            return 0;
        }

        /**
         * Obtains the children stmt in the specified position.
         * @param index
         * @return
         */
        @Override
        public Tree getChildren(int index)
        {
            Util.assertion( index >= 0 && index < getNumChildren());
            return null;
        }
    }

	public static class GotoStmt extends Stmt
	{
		private LabelDecl label;
		private SourceLocation gotoLoc, labelLoc;

		public GotoStmt(LabelDecl label,
				SourceLocation gotoLoc,
				SourceLocation labelLoc)
		{
			super(GotoStmtClass);
			this.label = label;
			this.gotoLoc = gotoLoc;
            this.labelLoc = labelLoc;
		}

		public void accept(StmtVisitor v)
		{
			v.visitGotoStmt(this);
		}

        public LabelDecl getLabel()
        {
            return label;
        }

        public void setLabel(LabelDecl label)
        {
            this.label = label;
        }

        public SourceLocation getGotoLoc()
        {
            return gotoLoc;
        }

        public void setGotoLoc(SourceLocation gotoLoc)
        {
            this.gotoLoc = gotoLoc;
        }

        public SourceLocation getLabelLoc()
        {
            return labelLoc;
        }

        public void setLabelLoc(SourceLocation labelLoc)
        {
            this.labelLoc = labelLoc;
        }

        @Override
        public SourceRange getSourceRange()
        {
            return new SourceRange(gotoLoc, labelLoc);
        }
    }

	/**
	 * A continue of a loop.
	 */
	public static class ContinueStmt extends Stmt
	{
		private SourceLocation continueLoc;

		public ContinueStmt(SourceLocation continueLoc)
		{
			super(ContinueStmtClass);
			this.continueLoc = continueLoc;
		}

		public void accept(StmtVisitor v)
		{
			v.visitContinueStmt(this);
		}

        public SourceLocation getContinueLoc()
        {
            return continueLoc;
        }

        public void setContinueLoc(SourceLocation continueLoc)
        {
            this.continueLoc = continueLoc;
        }

        @Override public SourceRange getSourceRange()
        {
            return new SourceRange(continueLoc);
        }
        /**
         * Gets the number of children stmt enclosed in this stmt.
         * @return
         */
        @Override
        public int getNumChildren()
        {
            return 0;
        }

        /**
         * Obtains the children stmt in the specified position.
         * @param index
         * @return
         */
        @Override
        public Tree getChildren(int index)
        {
            Util.assertion( index >= 0 && index < getNumChildren());
            return null;
        }
    }

	/**
	 * A return statement.
	 */
	public static class ReturnStmt extends Stmt
	{
        private SourceLocation returnloc;
        private Expr retValue;

        public ReturnStmt(SourceLocation returnloc, Expr retValExpr)
		{
			super(ReturnStmtClass);
            this.returnloc = returnloc;
			retValue = retValExpr;
		}

        public ReturnStmt(SourceLocation returnloc)
        {
	        this(returnloc, null);
        }

		public void accept(StmtVisitor v)
		{
			v.visitReturnStmt(this);
		}

        public Expr getRetValue()
        {
	        return retValue;
        }

        public void setRetValue(Expr retValue)
        {
            this.retValue = retValue;
        }

        public SourceLocation getReturnLoc()
		{
			return returnloc;
		}

        public void setReturnloc(SourceLocation returnloc)
        {
            this.returnloc = returnloc;
        }

        @Override
        public SourceRange getSourceRange()
        {
            if (retValue != null)
                return new SourceRange(returnloc, retValue.getLocEnd());
            else
                return new SourceRange(returnloc);
        }

        /**
         * Gets the number of children stmt enclosed in this stmt.
         * @return
         */
        public int getNumChildren()
        {
            return retValue != null ? 1 : 0;
        }

        /**
         * Obtains the children stmt in the specified position.
         * @param index
         * @return
         */
        public Tree getChildren(int index)
        {
            Util.assertion( index >=0 && index < getNumChildren());
            return retValue;
        }
    }

    /**
     * Represents the kind of object an expression reference to.
     */
	public enum ExprValueKind
    {
        /**Represents the right value which is temporary located in memory.*/
        EVK_RValue,
        /**
         * Represents the left value which the address-of(&) operator could be
         * applied to.
         */
        EVK_LValue
    }

    /**
     * This class represents an expression. Note that{@linkplain, Expr} is the
     * subclass of {@linkplain Stmt}. This allows an expression to be transparently
     * used on any scenerio where a {@linkplain Stmt} required.
     */
	public abstract static class Expr extends Stmt
    {
        private QualType type;
        private ExprValueKind valuekind;
	    private ExprObjectKind ok;
        private SourceLocation loc;

        public Expr(int stmtClass,
		        QualType type,
		        ExprObjectKind ok,
		        ExprValueKind valuekind,
		        SourceLocation loc)
        {
            super(stmtClass);
            this.valuekind = valuekind;
	        this.ok = ok;
            this.type = type;
            this.loc = loc;
        }

        public Expr(int stmtClass, SourceLocation loc)
        {
            this(stmtClass, null, null, null, loc);
        }

        public SourceLocation getExprLocation()
        {
            return loc;
        }

        public void setType(QualType type)
        {
            Util.assertion( type != null);
            this.type = type;
        }

        public QualType getType() { return type; }

        public FieldDecl getBitField()
        {
            Expr e = ignoreParens();
            while (e instanceof ImplicitCastExpr)
            {
            	ImplicitCastExpr ice = (ImplicitCastExpr)e;
            	if (ice.getCastKind() == CK_LValueToRValue ||
			            (ice.getValueKind() != EVK_RValue && ice.getCastKind()
					            == CK_NoOp))
	            {
	            	e = ice.getSubExpr().ignoreParens();
	            }
	            else
	            	break;
            }

            if (e instanceof MemberExpr)
            {
            	MemberExpr memExpr = (MemberExpr)e;
            	if (memExpr.getMemberDecl() instanceof FieldDecl)
	            {
	            	if (memExpr.getMemberDecl() instanceof FieldDecl)
		            {
			            FieldDecl fd = (FieldDecl)memExpr.getMemberDecl();
			            if (fd.isBitField())
			            	return fd;
		            }
	            }
            }

            if (e instanceof DeclRefExpr)
            {
            	DeclRefExpr ref = (DeclRefExpr)e;
            	if (ref.getDecl() instanceof FieldDecl)
	            {
	            	FieldDecl field = (FieldDecl)ref.getDecl();
	            	if (field.isBitField())
	            		return field;
	            }
            }

            if (e instanceof BinaryExpr)
            {
            	BinaryExpr be = (BinaryExpr)e;
            	if (be.isAssignmentOp() && be.getLHS() != null)
	            {
	            	return be.getLHS().getBitField();
	            }
	            if (be.getOpcode() == BO_Comma && be.getRHS() != null)
	            	return be.getRHS().getBitField();
            }

            return null;
        }

        public Expr ignoreParens()
        {
            Expr e = this;
            while (true)
            {
                if (e instanceof ParenExpr)
                {
                    e = ((ParenExpr)e).subExpr;
                    continue;
                }
                return e;
            }
        }

        public void setValueKind(ExprValueKind valueKind)
        {
            this.valuekind = valueKind;
        }

        public boolean isSignedIntegerOrEnumeration()
        {
            if (type.isBuiltinType())
            {
                return type.getTypeClass() >= TypeClass.SChar
                        && type.getTypeClass() <= TypeClass.Long;
            }

            if (type.isEnumeralType())
            {
                EnumType et = type.getAsEnumType();
                if (et.getDecl().isCompleteDefinition())
                    return et.getDecl().getIntegerType().isSignedIntegerType();
            }
            return false;
        }

        /**
         * Returns the folding result if this is a constant which we can fold.
         * </br>
         * If this result returned is not {@code null}, then the constant folding
         * result will be returned.
         * @return
         */
        public boolean evaluate(EvalResult result, ASTContext ctx)
        {
            return ExprEvaluatorBase.evaluate(result, this, ctx);
        }

        public boolean isLValue()
        {
            return valuekind == ExprValueKind.EVK_LValue;
        }

        public boolean isRValue()
        {
            return valuekind == EVK_RValue;
        }

	    public boolean isGLValue()
	    {
		    return getValueKind() != EVK_RValue;
	    }

        public ExprValueKind getValueKind()
        {
            return valuekind;
        }

        public Expr ignoreParensImpCasts()
        {
            Expr e = this;
            while (true)
            {
                if (e.stmtClass == ParenExprClass)
                {
                    e = ((ParenExpr)e).subExpr;
                    continue;
                }
                if (e.stmtClass == ImplicitCastClass)
                {
                    e = ((ImplicitCastExpr)e).getSubExpr();
                    continue;
                }
                return e;
            }
        }

        public Expr ignoreParenCasts()
        {
            Expr e = this;
            while (true)
            {
                if (e.stmtClass == ParenExprClass)
                {
                    e = ((ParenExpr)e).subExpr;
                    continue;
                }
                if (e instanceof CastExpr)
                {
                    e = ((CastExpr)e).getSubExpr();
                    continue;
                }
                return e;
            }
        }

        /**
         * This method is attempting whether an expression is an initializer
         * which can be evaluated at compile-time as a constant.
         * @return
         */
        public boolean isConstantInitializer(ASTContext context)
        {
            switch (stmtClass)
            {
                default:break;
                case StringLiteralClass:
                    return true;
                case CompoundLiteralExprClass:
                {
                    // This handles gcc's extension that allows global initializers like
                    // "struct x {int x;} x = (struct x) {};".
                    // FIXME: This accepts other cases it shouldn't!
                    Expr init = ((CompoundLiteralExpr)this).getInitializer();
                    return init.isConstantInitializer(context);
                }
                case InitListExprClass:
                {
                    // FIXME: This doesn't deal with fields with reference types correctly.
                    // FIXME: This incorrectly allows pointers cast to integers to be assigned
                    // to bitfields.
                    jlang.ast.Tree.InitListExpr expr = (Tree.InitListExpr)this;
                    int numInits = expr.getNumInits();
                    for (int i = 0; i < numInits; i++)
                    {
                        if (!expr.getInitAt(i).isConstantInitializer(context))
                            return false;
                    }
                    return true;
                }
                case ImplicitValueInitExprClass:
                    return true;
                case ParenExprClass:
                    return ((ParenExpr)this).getSubExpr().isConstantInitializer(context);
                case ImplicitCastClass:
                case ExplicitCastClass:
                {
                    // Handle casts with a destination that's a struct or union; this
                    // deals with both the gcc no-op struct cast extension and the
                    // cast-to-union extension.
                    if (getType().isRecordType())
                    {
                        return ((CastExpr)this).getSubExpr().isConstantInitializer(context);
                    }

                    if (getType().isIntegerType()
                            && ((CastExpr)this).getSubExpr().getType().isIntegerType() )
                    {
                        return ((CastExpr)this).getSubExpr().isConstantInitializer(context);
                    }
                    break;
                }
            }
            return isEvaluatable(context);
        }

        /**
         * Call {@linkplain #evaluate(EvalResult, ASTContext)} to see if this expression can
         * be constant folded, but get rid of evaluation result.
         * @return
         */
        public boolean isEvaluatable(ASTContext context)
        {
            EvalResult result = new EvalResult();
            return evaluate(result, context) && !result.hasSideEffects();
        }

	    /**
	     * Evaluates this expression and return a folded integer.
	     * @return
	     */
	    public APSInt evaluateKnownConstInt()
	    {
	        EvalResult res = new EvalResult();
	        boolean result = evaluate(res, null);
            Util.assertion(result, "Cound not evaluate expression");
            Util.assertion(res.val.isInt(), "Expression did not be evaluated into integer.");
		    return res.val.getInt();
	    }

	    public boolean hasSideEffects()
        {
            // TODO 2016.10.28
            return false;
        }

	    /**
	     * Obtains a source range from the lexical start to the lexical end in
	     * source program.
	     * @return
	     */
	    public abstract SourceRange getSourceRange();

	    public SourceLocation getLocStart()
	    {
		    return getSourceRange().getBegin();
	    }

	    public SourceLocation getLocEnd()
	    {
		    return getSourceRange().getEnd();
	    }

	    public boolean isIntegerConstantExpr(
			    OutRef<APSInt> iceResult,
			    ASTContext ctx)
	    {
		    ICEDiag d = checkICE(this, ctx);
		    if (d.val != 0)
		    {
			    if (loc != null) loc = d.loc;
			    return false;
		    }
		    EvalResult evalResult = new EvalResult();
		    if (!evaluate(evalResult, ctx))
			    Util.assertion(false, "ICE cannot be evaluated!");
		    Util.assertion(!evalResult.hasSideEffects, "ICE with side effect!");
		    Util.assertion(evalResult.val.isInt(), "ICE is not integer!");
		    iceResult.set(evalResult.val.getInt());
		    return true;
	    }

	    public boolean isIntegerConstantExpr(ASTContext ctx)
	    {
	    	OutRef<APSInt> x = new OutRef<>(new APSInt());
	    	return isIntegerConstantExpr(x, ctx);
	    }

	    public ExprObjectKind getObjectKind()
	    {
		    return ok;
	    }

	    public void setObjectKind(ExprObjectKind okKind)
	    {
		    ok = okKind;
	    }

        /**
         * Return true if this immediate expression should
         * be warned about if the result is unused.  If so, fill in Loc and Ranges
         * with location to warn on and the source range[s] to report with the
         * warning.
         * @param loc
         * @param r1
         * @param r2
         * @return
         */
	    public boolean isUnusedResultAWarning(
	    		OutRef<SourceLocation> loc,
			    OutRef<SourceRange> r1,
			    OutRef<SourceRange> r2)
	    {
	        switch (getStmtClass())
            {
                default:
                    loc.set(getExprLocation());
                    r1.set(getSourceRange());
                    return true;
                case ParenExprClass:
                    return ((ParenExpr)this).getSubExpr().
                            isUnusedResultAWarning(loc, r1, r2);
                case UnaryOperatorClass:
                {
                    UnaryExpr ue = (UnaryExpr)this;
                    switch (ue.getOpCode())
                    {
                        case UO_PostInc:
                        case UO_PostDec:
                        case UO_PreInc:
                        case UO_PreDec:
                            return false;   // No warning.
                        case UO_Deref:
                            // Dereference a volatile pointer is a side-effect.
                            if (getType().isVolatileQualified())
                                return false;   // No warning.
                            break;
                        case UO_Real:
                        case UO_Imag:
                            if (ue.getSubExpr().getType().isVolatileQualified())
                                return false;
                            break;
                    }
                    loc.set(ue.getExprLocation());
                    r1.set(ue.getSubExpr().getSourceRange());
                    return true;
                }
                case BinaryOperatorClass:
                {
                    BinaryExpr be = (BinaryExpr)this;
                    if (be.getOpcode() == BO_Comma)
                        return be.getRHS().isUnusedResultAWarning(loc, r1, r2)
                                || be.getLHS().isUnusedResultAWarning(loc, r1, r2);

                    if (be.isAssignmentOp())
                        return false;

                    loc.set(be.getOperatorLoc());
                    r1.set(be.getLHS().getSourceRange());
                    r2.set(be.getRHS().getSourceRange());
                    return true;
                }
                case CompoundAssignOperatorClass:
                    return false;
                case ConditionalOperatorClass:
                {
                    ConditionalExpr expr = (ConditionalExpr)this;
                    if (expr.getTrueExpr() != null &&
                            expr.getTrueExpr().isUnusedResultAWarning(loc, r1, r2))
                        return true;
                    return expr.getFalseExpr().isUnusedResultAWarning(loc, r1, r2);
                }
                case MemberExprClass:
                {
                    // If the base pointer or element is to a volatile pointer/field, accessing
                    // it is a side effect.
                    if (getType().isVolatileQualified())
                        return false;
                    MemberExpr expr = (MemberExpr)this;
                    loc.set(expr.getMemberLoc());
                    r1.set(new SourceRange(loc.get(), loc.get()));
                    r2.set(expr.getBase().getSourceRange());
                    return true;
                }
                case ArraySubscriptExprClass:
                {
                    // If the base pointer or element is to a volatile pointer/field, accessing
                    // it is a side effect.
                    if (getType().isVolatileQualified())
                        return false;

                    ArraySubscriptExpr expr = (ArraySubscriptExpr)this;
                    loc.set(expr.getRBracketLoc());
                    r1.set(expr.getLHS().getSourceRange());
                    r2.set(expr.getRHS().getSourceRange());
                    return true;
                }
                case CallExprClass:
                {
                    CallExpr ce = (CallExpr)this;
                    Expr calleeExpr = ce.getCallee().ignoreParenCasts();
                    if (calleeExpr instanceof DeclRefExpr)
                    {
                        DeclRefExpr calleeDE = (DeclRefExpr)calleeExpr;

                        // If the callee has attribute pure, const, or warn_unused_result, warn
                        // about it. void foo() { strlen("bar"); } should warn.
                        if (calleeDE.getDecl() instanceof FunctionDecl)
                        {
                            // TODO: 17-5-9 check function attributes
                        }
                    }
                    return false;
                }

                case ExplicitCastClass:
                    if (getType().isVoidType())
                        return false;

                    ExplicitCastExpr ce = (ExplicitCastExpr)this;
                    loc.set(ce.getlParenLoc());
                    r1.set(ce.getSourceRange());
                    return true;
                case ImplicitCastClass:
                    return ((ImplicitCastExpr)this).getSubExpr().isUnusedResultAWarning(loc, r1, r2);
            }
	    }

	    /**
	     * C99 6.3.2.3p3. Determines if this expression is null pointer constant.
	     * @return  Return true if this is either an
	     * integer constant expression with the value zero, or if this is one that is
	     * cast to void*.
	     */
	    public boolean isNullPointerConstant(ASTContext context)
	    {
			if (this instanceof ExplicitCastExpr)
			{
				ExplicitCastExpr ee = (ExplicitCastExpr)this;
				// The case of (void*)0;
				QualType ty = ee.getType();
				if (ty.isPointerType() && ty.getPointeeType().isVoidType() &&
						ty.getPointeeType().getCVRQualifiers() == 0)
				{
					return ee.getSubExpr().isNullPointerConstant(context);
				}
			}
			if (this instanceof ImplicitCastExpr)
            {
                ImplicitCastExpr ice = (ImplicitCastExpr)this;
                return ice.getSubExpr().isNullPointerConstant(context);
            }
            if (this instanceof ParenExpr)
            {
                return ((ParenExpr)this).getSubExpr().isNullPointerConstant(context);
            }

	    	// The type of expression must be integral.
	    	if (!getType().isIntegerType())
	    		return false;

            // If we have an integer constant expression, we need to *evaluate* it and
            // test for the value 0.
            OutRef<APSInt> x = new OutRef<>(new APSInt());
            return isIntegerConstantExpr(x, context) && x.get().eq(0);
	    }

        public static class ICEDiag
	    {
		    public int val;
		    public SourceLocation loc;

		    public ICEDiag(int v, SourceLocation l)
		    {
			    val = v;
			    loc = l;
		    }

		    public ICEDiag() {super();}
	    }

	    public static ICEDiag noDiag() {return new ICEDiag(); }

	    static ICEDiag checkEvalInICE(Expr e, ASTContext ctx)
	    {
		    EvalResult evalResult = new EvalResult();
		    if (!e.evaluate(evalResult, ctx) || evalResult.hasSideEffects ||
				    !evalResult.val.isInt())
		    {
			    return new ICEDiag(2, e.getLocStart());
		    }
		    return noDiag();
	    }

	    static ICEDiag checkICE(Expr e, ASTContext ctx)
	    {
		    if (!e.getType().isIntegerType())
		    {
		        return new ICEDiag(2, e.getLocStart());
	        }

		    switch (e.getStmtClass())
		    {
			    default:
				    return new ICEDiag(2, e.getLocStart());
			    case ParenExprClass:
				    return checkICE(((ParenExpr) e).getSubExpr(), ctx);
			    case IntegerLiteralClass:
			    case CharacterLiteralClass:
			        return noDiag();
			    case CallExprClass:
			    {
				    CallExpr ce = (CallExpr) e;
				    return new ICEDiag(2, e.getLocStart());
			    }
			    case DeclRefExprClass:
				    if ((((DeclRefExpr) e)
						    .getDecl()) instanceof EnumConstantDecl)
					    return noDiag();
				    return new ICEDiag(2, e.getLocStart());
			    case UnaryOperatorClass:
			    {
				    UnaryExpr exp = (UnaryExpr) e;
				    switch (exp.getOpCode())
				    {
					    default:
						    return new ICEDiag(2, e.getLocStart());
					    case UO_LNot:
					    case UO_Plus:
					    case UO_Minus:
					    case UO_Not:
					    case UO_Real:
					    case UO_Imag:
						    return checkICE(exp.getSubExpr(), ctx);
				    }
			    }
			    case BinaryOperatorClass:
			    {
				    BinaryExpr exp = (BinaryExpr) e;
				    switch (exp.getOpcode())
				    {
					    default:
						    return new ICEDiag(2, e.getLocStart());
					    case BO_Mul:
					    case BO_Div:
					    case BO_Rem:
					    case BO_Add:
					    case BO_Sub:
					    case BO_Shl:
					    case BO_Shr:
					    case BO_LT:
					    case BO_GT:
					    case BO_LE:
					    case BO_GE:
					    case BO_EQ:
					    case BO_NE:
					    case BO_And:
					    case BO_Xor:
					    case BO_Or:
					    case BO_Comma:
					    {
						    ICEDiag lhsResult = checkICE(exp.getLHS(), ctx);
						    ICEDiag rhsResult = checkICE(exp.getRHS(), ctx);
						    if (exp.getOpcode() == BO_Div
								    || exp.getOpcode() == BO_Rem)
						    {
							    // Evaluate gives an error for undefined Div/Rem, so make sure
							    // we don't evaluate one.
							    if (lhsResult.val != 2 && rhsResult.val != 2)
							    {
								    APSInt REval = exp.getRHS().evaluateAsInt(ctx);
								    if (REval == null)
									    return new ICEDiag(1, e.getLocStart());
								    if (REval.isSigned() && REval
										    .isAllOnesValue())
								    {
									    APSInt LEval = exp.getRHS().evaluateAsInt(ctx);
									    if (LEval.isMinSignedValue())
										    return new ICEDiag(1,
												    e.getLocStart());
								    }
							    }
						    }
						    if (exp.getOpcode() == BO_Comma)
						    {
							    if (ctx.getLangOptions().c99)
							    {
								    // C99 6.6p3 introduces a strange edge case: comma can be in an ICE
								    // if it isn't evaluated.
								    if (lhsResult.val == 0
										    && rhsResult.val == 0)
									    return new ICEDiag(1, e.getLocStart());
							    }
							    else
							    {
								    // In both C89 and C++, commas in ICEs are illegal.
								    return new ICEDiag(2, e.getLocStart());
							    }
						    }
						    if (lhsResult.val >= rhsResult.val)
							    return lhsResult;
						    return rhsResult;
					    }
					    case BO_LAnd:
					    case BO_LOr:
					    {
						    ICEDiag lhsResult = checkICE(exp.getLHS(), ctx);
						    ICEDiag rhsResult = checkICE(exp.getRHS(), ctx);
						    if (lhsResult.val == 0 && rhsResult.val == 1)
						    {
							    // Rare case where the RHS has a comma "side-effect"; we need
							    // to actually check the condition to see whether the side
							    // with the comma is evaluated.
							    if ((exp.getOpcode() == BO_LAnd) != (
									    exp.getLHS().evaluateAsInt(ctx).eq(0)))
								    return rhsResult;
							    return noDiag();
						    }

						    if (lhsResult.val >= rhsResult.val)
							    return lhsResult;
						    return rhsResult;
					    }
				    }
			    }
			    case ImplicitCastClass:
			    {
				    Expr subExpr = ((CastExpr) e).getSubExpr();
				    if (subExpr.getType().isIntegerType())
					    return checkICE(subExpr, ctx);
				    if (subExpr.ignoreParens() instanceof FloatingLiteral)
					    return noDiag();
				    return new ICEDiag(2, e.getLocStart());
			    }
				case ConditionalOperatorClass:
			    {
				    ConditionalExpr exp = (ConditionalExpr) e;
				    // If the condition (ignoring parens) is a __builtin_constant_p call,
				    // then only the true side is actually considered in an integer constant
				    // expression, and it is fully evaluated.  This is an important GNU
				    // extension.  See GCC PR38377 for discussion.
				    if (exp.getCond().ignoreParens() instanceof CallExpr)
				    {
					    CallExpr callCE = (CallExpr) exp.getCond()
							    .ignoreParens();
				    /*
				    if (callCE -> isBuiltinCall(ctx) == Builtin::BI__builtin_constant_p)
				    {
					    EvalResult EVResult = new EvalResult();
					    if (!e.evaluate(EVResult) || EVResult.hasSideEffects
							    ||
							    !EVResult.value.isInt())
					    {
						    return ICEDiag(2, e -> getLocStart());
					    }
					    return noDiag();
				    }*/
				    }
				    ICEDiag condResult = checkICE(exp.getCond(), ctx);
				    ICEDiag trueResult = checkICE(exp.getTrueExpr(), ctx);
				    ICEDiag falseResult = checkICE(exp.getFalseExpr(), ctx);
				    if (condResult.val == 2)
					    return condResult;
				    if (trueResult.val == 2)
					    return trueResult;
				    if (falseResult.val == 2)
					    return falseResult;
				    if (condResult.val == 1)
					    return condResult;
				    if (trueResult.val == 0 && falseResult.val == 0)
					    return noDiag();
				    // Rare case where the diagnostics depend on which side is evaluated
				    // Note that if we get here, condResult is 0, and at least one of
				    // trueResult and falseResult is non-zero.
				    if (exp.getCond().evaluateAsInt(ctx).eq(0))
				    {
					    return falseResult;
				    }
				    return trueResult;
			    }
		    }
	    }

	    public APSInt evaluateAsInt(ASTContext ctx)
	    {
		    EvalResult evalResult = new EvalResult();
		    boolean result = evaluate(evalResult, ctx);
		    Util.assertion(result, "Could not evaluate expression");
		    Util.assertion(evalResult.val.isInt(), "Expression did not evaluated to integer");
		    return evalResult.val.getInt();
	    }

	    /**
         * This class contains detailed information about an evaluation expression.
         */
        public static class EvalResult
        {
            /**
             * This is a value the expression can be folded to.
             */
            public APValue val;
            /**
             * Whether the evaludated expression has side effect.
             * for example, (f() && 0) can be folded, but it still has side effect.
             */
            private boolean hasSideEffects;

            public int diag;
            public final Expr diagExpr;
            public SourceLocation diagLoc;

            public EvalResult()
            {
                diagExpr = null;
                diagLoc = SourceLocation.NOPOS;
            }

            public boolean hasSideEffects()
            {
                return hasSideEffects;
            }

            public APValue getValue()
            {
                return val;
            }

            public boolean isGlobalLValue()
            {
                Util.assertion( val.isLValue());
                return ExprEvaluatorBase.isGlobalLValue(val.getLValueBase());
            }
        }

	    /**
	     * C99 6.3.2.1: an lvalue that does not have array type,
	     * does not have an incomplete type, does not have a const-qualified type,
	     * and if it is a structure or union, does not have any member (including,
	     * recursively, any member or element of all contained aggregates or unions)
	     * with a const-qualified type.
	     */
	    public enum IsModifiableLvalueResult
	    {
		    MLV_Valid,
		    MLV_NotObjectType,
		    MLV_IncompleteVoidType,
		    MLV_InvalidExpression,
		    MLV_LValueCast,           // Specialized form of MLV_InvalidExpression.
		    MLV_IncompleteType,
		    MLV_ConstQualified,
		    MLV_ArrayType,
	    }

	    /**
	     * C99 6.3.2.1: an lvalue is an expression with an object type or an
	     * incomplete type other than void. Nonarray expressions that can be lvalues:
	     *  - nameOrField, where nameOrField must be a variable
	     *  - e[i]
	     *  - (e), where e must be an lvalue
	     *  - e.nameOrField, where e must be an lvalue
	     *  - e->nameOrField
	     *  - *e, the type of e cannot be a function type
	     *  - string-constant
	     *  - (__real__ e) and (__imag__ e) where e is an lvalue  [GNU extension]
	     */
	    public enum IsLvalueResult
	    {
		    LV_Valid,
		    LV_NotObjectType,
		    LV_IncompleteVoidType,
		    LV_InvalidExpression,
	    }

	    /**
	     * Determine whether the given declaration can be
	     * an lvalue. This is a helper routine for isLvalue.
	     * @param refDecl
	     * @param ctx
	     * @return
	     */
	    private boolean declCanBeLvalue(NamedDecl refDecl, ASTContext ctx)
	    {
		    return refDecl instanceof VarDecl || refDecl instanceof FieldDecl;
	    }

	    /**
	     * Check whether the expression can be sanely treated like an l-value
	     * @param ctx
	     * @return
	     */
	    private IsLvalueResult isLvalueInternal(ASTContext ctx)
	    {
		    switch (getStmtClass())
		    {
			    // C99 6.5.1p4
			    case StringLiteralClass:
				    return LV_Valid;
			    case ArraySubscriptExprClass:
				    // C99 6.5.3p4 (e1[e2] == (*((e1)+(e2))))
				    return LV_Valid;
			    case DeclRefExprClass:
				    NamedDecl refedDecl = ((DeclRefExpr)this).getDecl();
				    if (declCanBeLvalue(refedDecl, ctx))
					    return LV_Valid;
				    break;
			    case MemberExprClass:
				    MemberExpr m = (MemberExpr)this;
				    // C99 6.5.2.3p4
				    return m.isArrow() ? LV_Valid : m.getBase().isLvalue(ctx);
			    case UnaryOperatorClass:
				    UnaryExpr ue = (UnaryExpr)this;
				    if (ue.getOpCode() == UnaryOperatorKind.UO_Deref)
					    return LV_Valid;
				    break;
			    case ImplicitCastClass:
				    return LV_InvalidExpression;
			    case ParenExprClass:
				    return ((ParenExpr)this).getSubExpr().isLvalue(ctx);
			    case BinaryOperatorClass:
			    case CompoundAssignOperatorClass:
				    return LV_InvalidExpression;
			    case CallExprClass:
				    break;
			    case CompoundLiteralExprClass:
				    // C99 6.5.2.5p5
				    return LV_Valid;
			    case ConditionalOperatorClass:
				    return LV_InvalidExpression;
		    }

		    return LV_InvalidExpression;
	    }

	    /**
	     * C99 6.3.2.1: an lvalue is an expression with an object type or an
	     * incomplete type other than void. Nonarray expressions that can be lvalues:
	     *  - asmName, where asmName must be a variable
	     *  - e[i]
	     *  - (e), where e must be an lvalue
	     *  - e.asmName, where e must be an lvalue
	     *  - e->asmName
	     *  - *e, the type of e cannot be a function type
	     *  - string-constant
	     *  - (__real__ e) and (__imag__ e) where e is an lvalue  [GNU extension]
	     * @param ctx
	     * @return
	     */
	    private IsLvalueResult isLvalue(ASTContext ctx)
	    {
			IsLvalueResult res = isLvalueInternal(ctx);

		    if (res != LV_Valid)
			    return res;

		    // first, check the type (C99 6.3.2.1). Expressions with function
		    // type in C are not lvalues
		    if (type.isFunctionType())
			    return LV_NotObjectType;

		    // Allow qualified void which is an incomplete type other than void (yuck).
		    if (type.isVoidType() && ctx.getCanonicalType(type).getCVRQualifiers() == 0)
			    return LV_IncompleteVoidType;

		    return LV_Valid;
	    }

	    public IsModifiableLvalueResult isModifiableLvalue(
			    ASTContext context,
			    OutRef<SourceLocation> loc)
	    {
			IsLvalueResult lvalResult = isLvalue(context);

		    switch (lvalResult)
		    {
			    case LV_Valid:
			        break;
			    case LV_NotObjectType:
			        return MLV_NotObjectType;
			    case LV_IncompleteVoidType:
			        return MLV_IncompleteVoidType;
			    case LV_InvalidExpression:
				    // If the top level is a C-style cast, and the subexpression is a valid
				    // lvalue, then this is probably a use of the old-school "cast as lvalue"
				    // GCC extension.  We don't support it, but we want to produce good
				    // diagnostics when it happens so that the user knows why.
				    if (ignoreParens() instanceof ExplicitCastExpr)
				    {
					    ExplicitCastExpr ce = (ExplicitCastExpr)ignoreParens();
					    if (ce.getSubExpr().isLvalue(context) == LV_Valid)
					    {
						    if (loc != null)
							    loc.set(ce.lParenLoc);
						    return MLV_LValueCast;
					    }
				    }
			        return MLV_InvalidExpression;
		    }

		    QualType ct = context.getCanonicalType(getType());

		    if (ct.isConstQualifed())
			    return MLV_ConstQualified;
		    if (ct.isArrayType())
			    return MLV_ArrayType;
		    if (ct.isIncompleteType())
			    return MLV_IncompleteType;
		    RecordType rt = context.getAs(ct, RecordType.class);
		    if (rt != null)
		    {
			    if (rt.hasConstFields())
				    return MLV_ConstQualified;
		    }

		    return MLV_Valid;
	    }
    }

    //************* Primary expression ******************************//

    /**
     * [C99 6.5.1p2]
     * An reference to a declared variable getIdentifier(in which case it is a lvalue) or
     * function getIdentifier (in which case it is a function designator).
     *
     */
    public static class DeclRefExpr extends Expr
    {
        private IdentifierInfo name;

        /**
         * The declaration that we are referencing.
         */
        private NamedDecl d;
        /**
         * The location of the declaration getIdentifier itself.
         */
        private SourceLocation location;

        public DeclRefExpr(
        		IdentifierInfo name,
		        NamedDecl d,
                QualType ty,
		        ExprObjectKind ok,
                ExprValueKind valueKind,
                SourceLocation loc)
        {
            super(DeclRefExprClass, ty, ok, valueKind, loc);
            this.name = name;
            this.d = d;
            location = loc;
        }

        @Override
        public void accept(StmtVisitor v)
        {
            v.visitDeclRefExpr(this);
        }

        public NamedDecl getDecl()
        {
            return d;
        }

        @Override
        public SourceLocation getExprLocation()
        {
            return location;
        }

        public void setLocation(SourceLocation location)
        {
            this.location = location;
        }

        public IdentifierInfo getName()
        {
            return name;
        }

        public void setName(IdentifierInfo name)
        {
            this.name = name;
        }
	    /**
	     * Obtains a source range from the lexical start to the lexical end in
	     * source program.
	     *
	     * @return
	     */
	    @Override
        public SourceRange getSourceRange()
	    {
		    return new SourceRange(getExprLocation());
	    }
    }

    public static class IntegerLiteral extends Expr
    {
        public final APInt val;
        public IntegerLiteral(
                ASTContext ctx,
                final APInt value,
                QualType type,
                SourceLocation loc)
        {
            super(IntegerLiteralClass, type, OK_Ordinary, EVK_RValue, loc);
            Util.assertion(type.isIntegerType(), "Illegal type in Integer literal.");
            Util.assertion(value.getBitWidth() == ctx.getTypeSize(type), "Integer type is not the correct for constant.");

            val = value;
        }

        public APInt getValue() { return val;}

        @Override
        public void accept(StmtVisitor v)
        {
            v.visitIntegerLiteral(this);
        }

	    /**
	     * Obtains a source range from the lexical start to the lexical end in
	     * source program.
	     *
	     * @return
	     */
	    @Override public SourceRange getSourceRange()
	    {
		    return new SourceRange(getExprLocation());
	    }
    }

    public static class FloatingLiteral extends Expr
    {
        private APFloat value;
        private boolean isExact;
        public FloatingLiteral(
		        APFloat value,
		        boolean isExact,
        		QualType type,
                SourceLocation loc)
        {
            super(FloatLiteralClass, type, OK_Ordinary, EVK_RValue, loc);
	        this.value = value;
	        this.isExact = isExact;
        }

        public APFloat getValue()
        {
            return value;
        }

	    public void setValue(APFloat value)
	    {
		    this.value = value;
	    }

	    public boolean isExact()
	    {
		    return isExact;
	    }

	    public void setExact(boolean exact)
	    {
		    isExact = exact;
	    }

	    @Override
        public void accept(StmtVisitor v)
        {
            v.visitFloatLiteral(this);
        }

	    /**
	     * Obtains a source range from the lexical start to the lexical end in
	     * source program.
	     *
	     * @return
	     */
	    @Override
	    public SourceRange getSourceRange()
	    {
		    return new SourceRange(getExprLocation());
	    }
    }

    public static class CharacterLiteral extends Expr
    {
        private int val;
        private boolean isWide;

        public CharacterLiteral(
                int val,
                boolean isWide,
                QualType type,
                SourceLocation loc)
        {
            super(CharacterLiteralClass, type, OK_Ordinary, EVK_RValue, loc);
            this.val = val;
        }

        public int getValue()
        {
            return val;
        }

        @Override
        public void accept(StmtVisitor v)
        {
            v.visitCharacterLiteral(this);
        }
	    /**
	     * Obtains a source range from the lexical start to the lexical end in
	     * source program.
	     *
	     * @return
	     */
	    @Override public SourceRange getSourceRange()
	    {
		    return new SourceRange(getExprLocation());
	    }
    }

    public static class StringLiteral extends Expr
    {
	    private String strData;
		private boolean isWide;
		private int numConcatenated;
		private SourceLocation[] tokLocs;

        public StringLiteral(QualType type)
        {
            super(StringLiteralClass, type, OK_Ordinary, EVK_RValue, null);
        }

        @Override
        public void accept(StmtVisitor v)
        {
            v.visitStringLiteral(this);
        }

	    public String getStrData() {return strData;}
	    public int getByteLength() {return strData.length();}

	    public void setStrData(String newStrData){this.strData = newStrData;}

	    /**
	     * Obtains a source range from the lexical start to the lexical end in
	     * source program.
	     *
	     * @return
	     */
	    @Override
	    public SourceRange getSourceRange()
	    {
		    return new SourceRange(getExprLocation());
	    }

	    public static StringLiteral create(
	    		String strData,
			    boolean isWide,
			    QualType strTy,
			    ArrayList<SourceLocation> stringLocs)
	    {
	    	StringLiteral lit = new StringLiteral(strTy);
	    	lit.strData = strData;
	    	lit.isWide = isWide;
	    	lit.tokLocs = new SourceLocation[stringLocs.size()];
	    	stringLocs.toArray(lit.tokLocs);
		    return lit;
	    }
    }

    /**
     * A parenthesized subexpression, e.g. (1).
     * This AST node is only formed if full location information was requested.
     */
    public static class ParenExpr extends Expr
    {
        public Expr subExpr;
        public SourceLocation lParenLoc, rParenLoc;

        public ParenExpr(Expr expr, SourceLocation l, SourceLocation r)
        {
            super(ParenExprClass, expr.getType(), OK_Ordinary, EVK_RValue, l);
            this.subExpr = expr;
            lParenLoc = l;
            rParenLoc = r;
        }

		@Override
        public SourceRange getSourceRange()
        {
            return new SourceRange(lParenLoc, rParenLoc);
        }
        public void accept(StmtVisitor v)
        {
            v.visitParenExpr(this);
        }

        public Expr getSubExpr()
        {
            return subExpr;
        }
        /**
         * Gets the number of children stmt enclosed in this stmt.
         * @return
         */
        @Override
        public int getNumChildren()
        {
            return 1;
        }

        /**
         * Obtains the children stmt in the specified position.
         * @param index
         * @return
         */
        @Override
        public Tree getChildren(int index)
        {
            Util.assertion( index >= 0 && index < getNumChildren());
            return subExpr;
        }
    }

    public static class ParenListExpr extends Expr
    {
        private ArrayList<Expr> exprs;
        private SourceLocation lParenLoc, rParenLoc;

        public ParenListExpr(
                SourceLocation lParenLoc,
                ArrayList<Expr> exprs,
                SourceLocation rParenLoc,
                QualType type)
        {
            super(ParenListExprClass, type, OK_Ordinary, EVK_RValue, lParenLoc);
            this.lParenLoc = lParenLoc;
            this.exprs = exprs;
            this.rParenLoc = rParenLoc;
        }

        public Expr getExpr(int idx)
        {
            Util.assertion( idx>= 0&& idx< exprs.size());
            return exprs.get(idx);
        }

        public int getNumExprs()
        {
            return exprs.size();
        }

        public SourceLocation getExprLoc()
        {
            return rParenLoc;
        }

        @Override
        public void accept(StmtVisitor v)
        {
            v.visitParenListExpr(this);
        }

	    /**
	     * Obtains a source range from the lexical start to the lexical end in
	     * source program.
	     *
	     * @return
	     */
	    @Override
        public SourceRange getSourceRange()
	    {
		    return new SourceRange(lParenLoc, rParenLoc);
	    }

        /**
         * Gets the number of children stmt enclosed in this stmt.
         * @return
         */
        @Override
        public int getNumChildren()
        {
            return exprs.size();
        }

        /**
         * Obtains the children stmt in the specified position.
         * @param index
         * @return
         */
        @Override
        public Tree getChildren(int index)
        {
            Util.assertion( index >= 0 && index < getNumChildren());
            return exprs.get(index);
        }
    }

    //===============================================================//
    //                 Postfix operators                             //
    //===============================================================//

    /**
     * A jlang.type cast.
     */
    public static abstract class CastExpr extends Expr
    {
        protected Expr expr;
        protected CastKind castKind;

        public CastExpr(int tag, QualType ty,
                ExprValueKind valueKind,
                Expr expr,
                final CastKind castKind,
                SourceLocation loc)
        {
            super(tag, ty, OK_Ordinary, valueKind, loc);
            Util.assertion(castKind != CK_Invalid, "creating cast with invalid cast kind.");
            this.expr = expr;
            this.castKind = castKind;
        }

        public CastKind getCastKind() { return castKind; }
        public Expr getSubExpr() { return expr;}

        /**
         * Gets the number of children stmt enclosed in this stmt.
         * @return
         */
        @Override
        public int getNumChildren()
        {
            return 1;
        }

        /**
         * Obtains the children stmt in the specified position.
         * @param index
         * @return
         */
        @Override
        public Tree getChildren(int index)
        {
            Util.assertion( index >= 0 && index < getNumChildren());
            return expr;
        }
    }

    /**
     * Allows us to explicitly represent implicit jlang.type conversions, which have
     * no direct representation in the original source code.
     * </br>
     * For example:
     * <pre>
     * converting T[]->T*, void f()->void (*f)(), float->double,
     * short->int, etc.
     * </pre>
     *
     * In C, implicit casts always produce rvalues
     */
    public static final class ImplicitCastExpr extends CastExpr
    {
        public ImplicitCastExpr(QualType ty,
                ExprValueKind valueKind,
                Expr expr,
                CastKind castKind,
                SourceLocation loc)
        {
            super(ImplicitCastClass, ty, valueKind, expr, castKind, loc);
        }

        /**
         * ReturnStmt the expression after ignoring all of implicitly jlang.type cast operation.
         * @return
         */
        public Expr ignoreImplicitCast()
        {
            Expr e = this;
            while (e instanceof ImplicitCastExpr)
                e = getSubExpr();
            return e;
        }

        /**
         * Visit this tree with a given visitor.
         *
         * @param v
         */
        @Override
        public void accept(StmtVisitor v)
        {
            v.visitImplicitCastExpr(this);
        }

	    /**
	     * Obtains a source range from the lexical start to the lexical end in
	     * source program.
	     *
	     * @return
	     */
	    @Override public SourceRange getSourceRange()
	    {
		    return getSubExpr().getSourceRange();
	    }
    }

    /**
     * An explicit cast in C (C99 6.5.4).
     */
    public static final class ExplicitCastExpr extends CastExpr
    {
        private SourceLocation lParenLoc;
        private SourceLocation rParenLoc;

	    public ExplicitCastExpr(QualType ty,
			    Expr expr, CastKind castKind,
			    SourceLocation lParenLoc,
			    SourceLocation rParenLoc)
	    {
		    this(ty, EVK_RValue, expr, castKind, lParenLoc, rParenLoc);
	    }

        public ExplicitCastExpr(QualType ty, ExprValueKind valueKind,
                Expr expr, CastKind castKind,
                SourceLocation lParenLoc,
		        SourceLocation rParenLoc)
        {
            super(ExplicitCastClass, ty, valueKind, expr, castKind, lParenLoc);
            this.lParenLoc = lParenLoc;
            this.rParenLoc = rParenLoc;
        }

        /**
         * Visit this tree with a given visitor.
         *
         * @param v
         */
        @Override
        public void accept(StmtVisitor v)
        {
            v.visitExplicitCastExpr(this);
        }

	    /**
	     * Obtains a source range from the lexical start to the lexical end in
	     * source program.
	     *
	     * @return
	     */
	    @Override
        public SourceRange getSourceRange()
	    {
		    return new SourceRange(lParenLoc, rParenLoc);
	    }

        public SourceLocation getlParenLoc()
        {
            return lParenLoc;
        }

        public SourceLocation getrParenLoc()
        {
            return rParenLoc;
        }
    }

    /**
     * An array selection [C99 6.5.2.1]
     */
    public static class ArraySubscriptExpr extends Expr
    {
        private final static int LHS = 0;
        private final static int RHS = 1;
        private final static int END_EXPR = 2;

        private Expr[] subExprs;
        private SourceLocation rBracketLoc;

        public ArraySubscriptExpr(
		        Expr indexed,
		        Expr index,
                QualType t,
                ExprValueKind valueKind,
                SourceLocation rBracketLoc)
        {
            super(ArraySubscriptExprClass, t, OK_Ordinary, valueKind, rBracketLoc);
            subExprs = new Expr[END_EXPR];
            subExprs[LHS] = indexed;
            subExprs[RHS] = index;
            this.rBracketLoc = rBracketLoc;
        }

        public ArraySubscriptExpr()
        {
            super(ArraySubscriptExprClass, SourceLocation.NOPOS);
        }

        public Expr getLHS()
        {
            return subExprs[LHS];
        }

        public void setLHS(Expr e)
        {
            subExprs[LHS] = e;
        }

        public Expr getRHS()
        {
            return subExprs[RHS];
        }

        public void setRHS(Expr e)
        {
            subExprs[RHS] = e;
        }

        public Expr getBase()
        {
            return getRHS().getType().isIntegerType() ? getLHS() : getRHS();
        }

        public Expr getIdx()
        {
            return getRHS().getType().isIntegerType() ? getRHS() : getLHS();
        }

        public SourceRange getSourceRange()
        {
            return new SourceRange(getLHS().getExprLocation(), rBracketLoc);
        }

        public SourceLocation getRBracketLoc()
        {
            return rBracketLoc;
        }

        public SourceLocation getExprLoc()
        {
            return getBase().getExprLocation();
        }

        @Override
        public void accept(StmtVisitor v)
        {
            v.visitArraySubscriptExpr(this);
        }

        /**
         * Gets the number of children stmt enclosed in this stmt.
         * @return
         */
        @Override
        public int getNumChildren()
        {
            return subExprs.length;
        }

        /**
         * Obtains the children stmt in the specified position.
         * @param index
         * @return
         */
        @Override
        public Tree getChildren(int index)
        {
            Util.assertion( index >= 0 && index < getNumChildren());
            return subExprs[index];
        }
    }

    /**
	 * Represents a function call (C99 6.5.2.2).
     * {@linkplain CallExpr} itself represents a normal function call,
     * e.g."f(x,2)",
	 */
	public static final class CallExpr extends Expr
	{
	    private static final int Fn = 0;
	    private static final int PREARGS_START = 1;

	    private Stmt[] subExprs;

		SourceLocation rparenLoc;

		public CallExpr(Expr fn,
                ArrayList<Expr> args,
                QualType resultType,
                ExprValueKind vk,
                SourceLocation rparenLoc)
		{
			super(CallExprClass, resultType, OK_Ordinary, vk, rparenLoc);
			subExprs = new Stmt[args.size() + 1];
			subExprs[Fn] = fn;
			for (int i = 0, e = args.size(); i != e; i++)
			    subExprs[i+1] = args.get(i);

			this.rparenLoc = rparenLoc;
		}

		public CallExpr()
        {
            super(CallExprClass, SourceLocation.NOPOS);
        }

        public Expr getCallee()
        {
            return (Expr)subExprs[Fn];
        }

        public void setCallee(Expr fn)
        {
            subExprs[Fn] = fn;
        }

        public Decl getCalleeDecl()
        {
            Expr cee = getCallee().ignoreParensImpCasts();

            // If we are calling a dereference, lookup at the pointer instead.
            /**
            if (cee.tc == BinaryOperatorClass)
            {
                BinaryExpr be = (BinaryExpr)cee;
                // Not to deal with '->' or '.' operator, since it just works
                // in C++.
            }
             */
            if (cee.stmtClass == UnaryOperatorClass)
            {
                UnaryExpr ue = (UnaryExpr)cee;
                if (ue.getOpCode() == UnaryOperatorKind.UO_Deref)
                    cee = ue.getSubExpr().ignoreParenCasts();
            }
            if (cee instanceof  MemberExpr)
                return ((MemberExpr)cee).getMemberDecl();

            return null;
        }

        public FunctionDecl getDirectCallee()
        {
            Decl res = getCalleeDecl();
            if (res instanceof FunctionDecl)
                return (FunctionDecl)res;
            return null;
        }

        public ArrayList<Expr> getArgs()
        {
            ArrayList<Expr> res = new ArrayList<>();
            for (int i = PREARGS_START; i < subExprs.length; i++)
                res.add((Expr) subExprs[i]);
            return res;
        }

        public Expr getArgAt(int idx)
        {
            Util.assertion(idx>= 0 && idx< subExprs.length - PREARGS_START, "Arg access out of range!");
            return (Expr) subExprs[idx + 1];
        }

        public void setArgAt(int idx, Expr e)
        {
            Util.assertion(idx>= 0 && idx< subExprs.length - 1, "Arg access out of range!");
            subExprs[idx + 1] = e;
        }

        public int getNumCommas()
        {
            return subExprs != null && subExprs.length > 1 ? subExprs.length - 1:0;
        }

        public QualType getCallReturnType()
        {
            QualType calleeType = getCallee().getType();
            PointerType ty = calleeType.getAsPointerType();
            if ( ty != null)
            {
                calleeType = ty.getPointeeType();
            }

            final FunctionType fnType = calleeType.getAsFunctionType();
            return fnType.getResultType();
        }

        @Override
		public void accept(StmtVisitor v)
		{
			v.visitCallExpr(this);
		}

		@Override
		public SourceRange getSourceRange()
		{
			return new SourceRange(getCallee().getExprLocation(), rparenLoc);
		}

		public int getNumArgs()
		{
			return subExprs.length - PREARGS_START;
		}

		/**
		 * This changes the number of arguments present in this call.
		 * Any orphaned expressions are deleted by this, and any new operands are set
		 * to null.
		 * @param numArgs
		 */
		public void setNumArgs( int numArgs)
		{
			if (numArgs == getNumArgs())
				return;

			if (numArgs < getNumArgs())
			{
				Stmt[] tmp = new Stmt[numArgs+PREARGS_START];
				tmp[Fn] = subExprs[Fn];
				System.arraycopy(subExprs, 1, tmp, 1, numArgs);
				subExprs = tmp;
				return;
			}
            // Otherwise, null out new args.
			Stmt[] tmp = new Stmt[numArgs];
			System.arraycopy(subExprs, 0, tmp, 0, subExprs.length);
			subExprs = tmp;
		}

        /**
         * Gets the number of children stmt enclosed in this stmt.
         * @return
         */
        @Override
        public int getNumChildren()
        {
            return subExprs.length;
        }

        /**
         * Obtains the children stmt in the specified position.
         * @param index
         * @return
         */
        @Override
        public Tree getChildren(int index)
        {
            Util.assertion( index >= 0 && index < getNumChildren());
            return subExprs[index];
        }
	}

    /**
     * Struct and Union Members. X->a and X.a [C99 6.5.2.3];
     */
	public static class MemberExpr extends Expr
    {
        /**
         * The expression for the base pointer. In X.F, this is {@code X}.
         */
        private Expr base;

        /**
         * This is the decl being referenced by the field/member getIdentifier.
         * In X.F, this is the decl referenced by F.
         */
        private ValueDecl memberDecl;

        /**
         * This is the location of the member asmName.
         */
        private SourceLocation memberLoc;

        /**
         * True if this is "X->F", false if this is "X.F".
         */
        private boolean isArrow;

        public MemberExpr(
                Expr base,
                boolean isArrow,
                ValueDecl memberDecl,
                QualType type,
                ExprValueKind valuekind,
                SourceLocation loc,
		        ExprObjectKind ok)
        {
            super(MemberExprClass, type, ok, valuekind, loc);
            this.base = base;
            this.isArrow = isArrow;
            this.memberDecl = memberDecl;
            memberLoc = loc;
        }

        public Expr getBase() { return base;}
        public void setBase(Expr e) { base = e; }

        /**
         * Retrieve the member declaration to which this expression refers.
         *
         * The returned declaration will be a {@linkplain FieldDecl}.
         * @return
         */
        public ValueDecl getMemberDecl() { return memberDecl; }
        public void setMemberDecl(ValueDecl d) { memberDecl = d; }

        public boolean isArrow()
        {
            return isArrow;
        }

        public void setArrow(boolean arrow)
        {
            isArrow = arrow;
        }

        public SourceLocation getMemberLoc()
        {
            return memberLoc;
        }

        public void setMemberLoc(SourceLocation memberLoc)
        {
            this.memberLoc = memberLoc;
        }

		@Override
        public SourceLocation getExprLocation() { return memberLoc;}

	    /**
	     * Obtains a source range from the lexical start to the lexical end in
	     * source program.
	     *
	     * @return
	     */
	    @Override
	    public SourceRange getSourceRange()
	    {
		    return new SourceRange(base.getExprLocation(), memberLoc);
	    }

	    @Override
        public void accept(StmtVisitor v)
        {
            v.visitMemberExpr(this);
        }
    }

    public static class CompoundLiteralExpr extends Expr
    {
        /**
         * LParenLoc - If non-null, this is the location of the left paren in a
         * subStmt literal like "(int){4}".  This can be null if this is a
         * synthesized subStmt expression.
         */
        private SourceLocation lParenLoc;

        /**
         * This can be an incomplete array jlang.type, in
         * which case the actual expression jlang.type will be different.
         */
        private QualType ty;
        private Expr init;
        private boolean isFileScope;


        public CompoundLiteralExpr(
                SourceLocation lParenLoc,
                QualType type,
                Expr init,
                boolean fileScope)
        {
            super(CompoundLiteralExprClass, type, OK_Ordinary, EVK_RValue, lParenLoc);
            this.lParenLoc = lParenLoc;
            ty = type;
            this.init = init;
            isFileScope = fileScope;
        }

        public CompoundLiteralExpr()
        {
            super(CompoundLiteralExprClass, SourceLocation.NOPOS);
        }

        public Expr getInitializer()
        {
            return init;
        }

        public void setInitializer(Expr init)
        {
            this.init = init;
        }

        public boolean isFileScope()
        {
            return isFileScope;
        }

        public void setFileScope(boolean fileScope)
        {
            isFileScope = fileScope;
        }

        public SourceLocation getLParenLoc()
        {
            return lParenLoc;
        }

        public void setLParenLoc(SourceLocation lParenLoc)
        {
            this.lParenLoc = lParenLoc;
        }

        public QualType getTy()
        {
            return ty;
        }

        public void setTy(QualType ty)
        {
            this.ty = ty;
        }

        @Override
        public void accept(StmtVisitor v)
        {
            v.visitCompoundLiteralExpr(this);
        }

	    /**
	     * Obtains a source range from the lexical start to the lexical end in
	     * source program.
	     *
	     * @return
	     */
	    @Override public SourceRange getSourceRange()
	    {
		    if (init == null)
			    return new SourceRange();
		    if (!lParenLoc.isValid())
			    return init.getSourceRange();
		    return new SourceRange(lParenLoc, init.getLocEnd());
	    }

        /**
         * Gets the number of children stmt enclosed in this stmt.
         * @return
         */
        @Override
        public int getNumChildren()
        {
            return 1;
        }

        /**
         * Obtains the children stmt in the specified position.
         * @param index
         * @return
         */
        @Override
        public Tree getChildren(int index)
        {
            Util.assertion( index >= 0 && index < getNumChildren());
            return init;
        }
    }

	/**
	 * This represents the unary-expression (except sizeof and alignof).
	 */
	public static final class UnaryExpr extends Expr
	{
		private Expr subExpr;
		private UnaryOperatorKind opcode;
		private SourceLocation loc;
		public UnaryExpr(Expr subExpr,
                UnaryOperatorKind opcode,
                QualType type,
                ExprValueKind evk,
                SourceLocation loc)
		{
			super(UnaryOperatorClass, type, OK_Ordinary, evk, loc);
			this.subExpr = subExpr;
            this.opcode = opcode;
			this.loc = loc;
		}

		public UnaryOperatorKind getOpCode()
        {
            return opcode;
        }

        public Expr getSubExpr()
        {
            return subExpr;
        }

        public static boolean isPostfix(UnaryOperatorKind op)
        {
            return op == UO_PostInc || op == UO_PostDec;
        }

        public static boolean isPrefix(UnaryOperatorKind op)
        {
            return op == UO_PreInc || op == UO_PreDec;
        }

        public boolean isPrefix()
        {
            return isPrefix(opcode);
        }

        public boolean isPostifx()
        {
            return isPostfix(opcode);
        }

        public boolean isIncrementOp()
        {
            return opcode == UO_PreInc || opcode == UO_PostInc;
        }

        public boolean isIncrementDecrementOp()
        {
            return opcode.ordinal() <= UO_PreDec.ordinal();
        }

        public static boolean isArithmeticOp(UnaryOperatorKind op)
        {
            return op.ordinal()>=UO_Plus.ordinal()
                    && op.ordinal()<= UO_LNot.ordinal();
        }

        public static final String getOpcodeStr(UnaryOperatorKind op)
        {
            switch (op)
            {
                default:
                    Util.shouldNotReachHere("Unkown unary operator");
                case UO_PostInc: return "++";
                case UO_PostDec: return "--";
                case UO_PreInc: return "++";
                case UO_PreDec: return "--";
                case UO_AddrOf: return "&";
                case UO_Deref: return "*";
                case UO_Plus: return "+";
                case UO_Minus: return "-";
                case UO_Not: return "!";
                case UO_Real: return "__real";
                case UO_Imag: return "--imag";
            }
        }

		public void accept(StmtVisitor v)
		{
			v.visitUnaryExpr(this);
		}

		/**
		 * Obtains a source range from the lexical start to the lexical end in
		 * source program.
		 *
		 * @return
		 */
		@Override public SourceRange getSourceRange()
		{
			if (isPostifx())
				return new SourceRange(subExpr.getLocStart(), loc);
			else
				return new SourceRange(loc, subExpr.getLocEnd());
		}

		public SourceLocation getOperatorLoc()
		{
			return loc;
		}

		public void setOperatorLoc(SourceLocation opLoc)
		{
			loc = opLoc;
		}

        /**
         * Gets the number of children stmt enclosed in this stmt.
         * @return
         */
        @Override
        public int getNumChildren()
        {
            return 1;
        }

        /**
         * Obtains the children stmt in the specified position.
         * @param index
         * @return
         */
        @Override
        public Tree getChildren(int index)
        {
            Util.assertion( index >= 0 && index < getNumChildren());
            return subExpr;
        }
	}

    /**
     * Expression with either a type or (unevaluated)
     * expression operand.  Used for sizeof/alignof (C99 6.5.3.4) and
     * vec_step (OpenCL 1.1 6.11.12).
     */
	public static final class UnaryExprOrTypeTraitExpr extends Expr
    {
        /**
         * true if operand is a type, false if it is an expression.
         */
        private boolean isType;
        private UnaryExprOrTypeTrait kind;
        private QualType ty;
        private Expr ex;
        private SourceLocation opLoc, rParenLoc;

        public UnaryExprOrTypeTraitExpr(UnaryExprOrTypeTrait kind,
                QualType resultType,
                SourceLocation opLoc,
		        SourceLocation rp)
        {
            super(UnaryExprOrTypeTraitClass, resultType, OK_Ordinary, EVK_RValue, opLoc);
            this.kind = kind;
            isType = true;
            this.opLoc = opLoc;
            this.rParenLoc = rp;
            ty = resultType;
        }

        public UnaryExprOrTypeTraitExpr(UnaryExprOrTypeTrait kind,
                Expr e,
                QualType resultType,
                SourceLocation opLoc,
		        SourceLocation rp)
        {
            super(UnaryExprOrTypeTraitClass, resultType, OK_Ordinary, EVK_RValue, opLoc);
            this.kind = kind;
            isType = true;
            this.opLoc = opLoc;
            this.rParenLoc = rp;
            ex = e;
        }

        @Override
        public void accept(StmtVisitor v)
        {
        }

	    /**
	     * Obtains a source range from the lexical start to the lexical end in
	     * source program.
	     *
	     * @return
	     */
	    @Override
        public SourceRange getSourceRange()
	    {
		    return new SourceRange(opLoc, rParenLoc);
	    }

	    public boolean isArgumentType()
        {
            return isType;
        }

        QualType getArgumentType()
        {
            return ty;
        }

        /**
         * Gets the number of children stmt enclosed in this stmt.
         * @return
         */
        @Override
        public int getNumChildren()
        {
            Util.assertion(false, "Should not reaching here");
            return 0;
        }

        /**
         * Obtains the children stmt in the specified position.
         * @param index
         * @return
         */
        @Override
        public Tree getChildren(int index)
        {
            Util.assertion(false, "Should not reaching here");
            return null;
        }
    }

	/**
	 * A binary operation expression such as "x+y" or "x<=y".
     * <br>
     * This expression node kind describes a binary operation,
     * such as "x+y" for integer values 'x' and 'y". The operands
     * will already have been converted to appropriate types (e.g.
     * by performing of promotion and conversion).
	 */
	public static class BinaryExpr extends Expr
	{
		public Expr lhs;
		public Expr rhs;
		public BinaryOperatorKind opcode;
        private SourceLocation oploc;

		public BinaryExpr(Expr lhs,
                Expr rhs,
                BinaryOperatorKind op,
                ExprValueKind vk,
                QualType resultTy,
                SourceLocation oploc)
		{
			super(BinaryOperatorClass, resultTy, OK_Ordinary, vk, oploc);
			this.lhs = lhs;
			this.rhs = rhs;
			opcode = op;
            this.oploc = oploc;
        }

        public BinaryExpr(int kind)
        {
            super(kind, SourceLocation.NOPOS);
        }

        @Override
		public void accept(StmtVisitor v)
		{
			v.visitBinaryExpr(this);
		}

		public SourceLocation getOperatorLoc() { return oploc; }

		public void setOperatorLoc(SourceLocation loc) { oploc = loc;}

		public BinaryOperatorKind getOpcode() { return opcode;}

		public Expr getLHS() { return lhs; }
        public void setLHS(Expr e) { lhs = e;}
        public Expr getRHS() { return rhs; }
        public void setRHS(Expr e) { rhs = e;}

        public static String getOpcodeStr(BinaryOperatorKind op)
        {
            if (op.ordinal() >= BO_Mul.ordinal() && op.ordinal()<= BO_Comma.ordinal())
                return op.toString();
            else
            {
                Util.shouldNotReachHere("Invalid binary operator.");
                return null;
            }
        }

        public String getOpcodeStr()
        {
            return getOpcodeStr(opcode);
        }

        public boolean isMultiplicativeOp()
        {
            return opcode.ordinal() >= BO_Mul.ordinal()
                    && opcode.ordinal() <= BO_Rem.ordinal();
        }

        public boolean isAdditiveOp()
        {
            return opcode == BO_Add
                   || opcode == BO_Sub;
        }

        public boolean isShiftOp()
        {
            return opcode == BO_Shl || opcode == BO_Shr;
        }

        public boolean isBitwiseOp()
        {
            return opcode.ordinal()>= BO_And.ordinal()
                    && opcode.ordinal()<= BO_Or.ordinal();
        }

        public boolean isRelationalOp()
        {
            return opcode.ordinal()>=BO_LT.ordinal()
                    && opcode.ordinal()<=BO_GE.ordinal();
        }

        public boolean isEqualityOp()
        {
            return opcode == BO_EQ || opcode == BO_NE;
        }

        public boolean isComparisonOp()
        {
            return opcode.ordinal() >= BO_LT.ordinal()
                    && opcode.ordinal()<= BO_NE.ordinal();
        }

        public boolean isLogicalOp()
        {
            return opcode.ordinal() >= BO_LAnd.ordinal()
                    && opcode.ordinal() <= BO_LOr.ordinal();
        }

        public boolean isAssignmentOp()
        {
            return opcode.ordinal() >= BO_Assign.ordinal()
                    && opcode.ordinal() <= BO_OrAssign.ordinal();
        }

        public boolean isCompoundAssignmentOp()
        {
            return opcode.ordinal()> BO_Assign.ordinal()
                    && opcode.ordinal() <= BO_OrAssign.ordinal();
        }

        public boolean isShiftAssignOp()
        {
            return opcode == BO_ShlAssign || opcode == BO_ShrAssign;
        }

		/**
		 * Obtains a source range from the lexical start to the lexical end in
		 * source program.
		 *
		 * @return
		 */
		@Override public SourceRange getSourceRange()
		{
			return new SourceRange(lhs.getLocStart(), rhs.getLocEnd());
		}

        /**
         * Gets the number of children stmt enclosed in this stmt.
         * @return
         */
        @Override
        public int getNumChildren()
        {
            return 2;
        }

        /**
         * Obtains the children stmt in the specified position.
         * @param index
         * @return
         */
        @Override
        public Tree getChildren(int index)
        {
            Util.assertion( index >= 0 && index < getNumChildren());
            return index == 0 ? lhs : rhs;
        }
	}

    /**
     * For subStmt assignments (e.g. +=), we keep
     * track of the jlang.type the operation is performed in.  Due to the semantics of
     * these operators, the operands are promoted, the arithmetic performed, an
     * implicit conversion back to the result jlang.type done, then the assignment takes
     * place.  This captures the intermediate jlang.type which the computation is done
     * in.
     */
    public static class CompoundAssignExpr extends BinaryExpr
    {
        private QualType computationLHSType;
        private QualType computationResultType;

        public CompoundAssignExpr(Expr lhs, Expr rhs,
                BinaryOperatorKind op,
                ExprValueKind vk,
                QualType resultTy,
                QualType compLHSType,
                QualType compResultType,
                SourceLocation oploc)
        {
            super(lhs, rhs, op, vk, resultTy, oploc);
            computationLHSType = compLHSType;
            computationResultType = compResultType;

            Util.assertion(isCompoundAssignmentOp(),                     "Only should be used for subStmt assignments");

        }

        public CompoundAssignExpr()
        {
            super(BinaryOperatorClass);
        }

        /**
         * The two computation types are the jlang.type the LHS is converted
         * to for the computation and the jlang.type of the result; the two are
         * distinct in a few cases (specifically, int+=ptr and ptr-=ptr).
         * @return
         */
        public QualType getComputationLHSType() { return computationLHSType; }
        public void setComputationLHSType(QualType T) { computationLHSType = T; }

        public QualType getComputationResultType() { return computationResultType; }
        public void setComputationResultType(QualType T) { computationResultType = T; }

        @Override
        public void accept(StmtVisitor v)
        {
            v.visitCompoundAssignExpr(this);
        }
    }

    /**
     * A ( ) ? ( ) : ( ) conditional expression
     */
    public static class ConditionalExpr extends Expr
    {
        private final static int COND = 0;
        private final static int TRUE = 1;
        private final static int FALSE = 2;
        private final static int END_EXPR = 3;
        private Stmt[] subExprs;

        private SourceLocation qLoc, cLoc;
        public ConditionalExpr(Expr cond, SourceLocation qLoc,
                Expr lhs,
                SourceLocation cLoc,
                Expr rhs,
                QualType t,
                ExprValueKind vk)
        {
            super(ConditionalOperatorClass, t, OK_Ordinary, vk, cond.getExprLocation());
            subExprs = new Stmt[END_EXPR];
            subExprs[COND] = cond;
            subExprs[TRUE] = lhs;
            subExprs[FALSE] = rhs;
            this.qLoc = qLoc;
            this.cLoc = cLoc;
        }

        public void accept(StmtVisitor v)
        {
            v.visitConditionalExpr(this);
        }

        public Expr getCond()
        {
            return (Expr)subExprs[COND];
        }

        public Expr getTrueExpr()
        {
            return (Expr) subExprs[TRUE];
        }

        public Expr getFalseExpr()
        {
            return (Expr)subExprs[FALSE];
        }

	    /**
	     * Obtains a source range from the lexical start to the lexical end in
	     * source program.
	     *
	     * @return
	     */
	    @Override
	    public SourceRange getSourceRange()
	    {
		    return new SourceRange(getCond().getLocStart(), getFalseExpr().getLocEnd());
	    }
        /**
         * Gets the number of children stmt enclosed in this stmt.
         * @return
         */
        @Override
        public int getNumChildren()
        {
            return subExprs.length;
        }

        /**
         * Obtains the children stmt in the specified position.
         * @param index
         * @return
         */
        @Override
        public Tree getChildren(int index)
        {
            Util.assertion( index >= 0 && index < getNumChildren());
            return subExprs[index];
        }
    }

    /**
     * Describes an C or C++ initializer list.
     * <br>
     * InitListExprClass describes an initializer list, which can be used to
     * initialize objects of different types, including
     * struct/class/union types, arrays, and vectors. For example:
     * <pre>
     * struct foo x = { 1, { 2, 3 } };
     * </pre>
     * <br>
     * Prior to semantic analysis, an initializer list will represent the
     * initializer list as written by the user, but will have the
     * placeholder jlang.type "void". This initializer list is called the
     * syntactic form of the initializer, and may contain C99 designated
     * initializers (represented as DesignatedInitExprs), initializations
     * of subobject members without explicit braces, and so on. Clients
     * interested in the original syntax of the initializer list should
     * use the syntactic form of the initializer list.
     * <br>
     * After semantic analysis, the initializer list will represent the
     * semantic form of the initializer, where the initializations of all
     * subobjects are made explicit with nested InitListExprClass nodes and
     * C99 designators have been eliminated by placing the designated
     * initializations into the subobject they initialize. Additionally,
     * any "holes" in the initialization, where no initializer has been
     * specified for a particular subobject, will be replaced with
     * implicitly-generated ImplicitValueInitExpr expressions that
     * value-initialize the subobjects. Note, however, that the
     * initializer lists may still have fewer initializers than there are
     * elements to initialize within the object.
     * <br>
     * Given the semantic form of the initializer list, one can retrieve
     * the original syntactic form of that initializer list (if it
     * exists) using getSyntacticForm(). Since many initializer lists
     * have the same syntactic and semantic forms, getSyntacticForm() may
     * return NULL, indicating that the current initializer list also
     * serves as its syntactic form.
     */
    public static class InitListExpr extends Expr
    {
        // TODO improve it in the future. 2016.10.15 Jianping Zeng.
        private SourceLocation lBraceLoc, rBraceLoc;
        private ArrayList<Expr> initExprs;

        /**
         * Contains the initializer list that describes the syntactic form
         * written in the source code.
         */
        private InitListExpr syntacticForm;

        private FieldDecl unionFieldInit;
        private boolean hadArrayRangeDesignator;

        public InitListExpr(SourceLocation lBraceLoc,
		        SourceLocation rBraceLoc,
		        ArrayList<Expr> initList)
        {
            super(InitListExprClass, lBraceLoc);
            this.lBraceLoc = lBraceLoc;
            this.rBraceLoc = rBraceLoc;
            this.initExprs = initList;
        }
        @Override
        public void accept(StmtVisitor v)
        {
            v.visitInitListExpr(this);
        }

        public int getNumInits()
        {
            return initExprs.size();
        }

        public Expr getInitAt(int index)
        {
            Util.assertion(index >= 0 && index < getNumInits(),  "Initializer access out of range!");

            return initExprs.get(index);
        }

        public void setInitAt(int index, Expr init)
        {
            Util.assertion(index >= 0 && index < getNumInits(),  "Initializer access out of range!");

            initExprs.set(index, init);
        }

	    /**
	     * Obtains a source range from the lexical start to the lexical end in
	     * source program.
	     *
	     * @return
	     */
	    @Override
        public SourceRange getSourceRange()
	    {
		    return new SourceRange(lBraceLoc, rBraceLoc);
	    }

        public void reserveInits(long numElements)
        {
            if (initExprs == null)
                initExprs = new ArrayList<>();

            if (numElements > initExprs.size())
            {
                for (long i = numElements - initExprs.size(); i > 0; --i)
                {
                    initExprs.add(null);
                }
            }
        }

        public void resizeInits(long numElts)
        {
            if (initExprs == null)
                initExprs = new ArrayList<>();

            initExprs.clear();
            for (;numElts >= 0; --numElts)
            {
                initExprs.add(null);
            }
        }

        public Expr updateInit(int index, Expr init)
        {
            if (index >= initExprs.size())
            {
                for (int i = index - initExprs.size(); i > 0; i--)
                    initExprs.add(null);

                initExprs.add(init);
                return null;
            }

            Expr prev = initExprs.get(index);
            initExprs.set(index, init);
            return prev;
        }

        public void setSyntacticForm(InitListExpr init)
        {
            syntacticForm = init;
        }

        public InitListExpr getSyntacticForm()
        {
            return syntacticForm;
        }

        public boolean isExplicit()
        {
            return lBraceLoc.isValid() && rBraceLoc.isValid();
        }

        public void setInitializedFieldInUnion(FieldDecl fd)
        {
            unionFieldInit = fd;
        }

        /**
         * If this initializes a union, specifies which field in the
         * union to initialize.
         * </br>
         * Typically, this field is the first named field within the
         * union. However, a designated initializer can specify the
         * initialization of a different field within the union.
         * @return
         */
        public FieldDecl getInitializedFieldInUnion()
        {
            return unionFieldInit;
        }

        public void sawArrayRangeDesignator()
        {
            hadArrayRangeDesignator = true;
        }

        public void setRBraceLoc(SourceLocation loc)
        {
            rBraceLoc = loc;
        }

        public SourceLocation getRBraceLoc()
        {
            return rBraceLoc;
        }

        public SourceLocation getLBraceLoc()
        {
            return lBraceLoc;
        }

        public void setLBraceLoc(SourceLocation loc)
        {
            lBraceLoc = loc;
        }

        /**
         * Gets the number of children stmt enclosed in this stmt.
         * @return
         */
        @Override
        public int getNumChildren()
        {
            return initExprs.size();
        }

        /**
         * Obtains the children stmt in the specified position.
         * @param index
         * @return
         */
        @Override
        public Tree getChildren(int index)
        {
            Util.assertion( index >= 0 && index < getNumChildren());
            return initExprs.get(index);
        }
    }

    /**
     * Represents an implicitly-generated value initialization of
     * an object of a given type.
     * <p>
     * Implicit value initializations occur within semantic initializer
     * list expressions (InitListExpr) as placeholders for subobject
     * initializations not explicitly specified by the user.
     * </p>
     */
    public static class ImplicitValueInitExpr extends Expr
    {
        public ImplicitValueInitExpr(QualType type)
        {
            super(ImplicitValueInitExprClass, type,
                    OK_Ordinary, EVK_RValue, new SourceLocation());
        }

        @Override
        public void accept(StmtVisitor v)
        {
        }

        @Override
        public SourceRange getSourceRange()
        {
            return new SourceRange();
        }
    }

	/**
	 * [C99 6.5.3.4] - This is for sizeof/alignof, both of
	 * types and expressions.
	 */
	public static class SizeOfAlignOfExpr extends Expr
	{
		/**
		 * True if operand is a type, false if an expression.
		 */
		private boolean isType;
		private Object operand;

		private SourceLocation opLoc;
		private SourceLocation rParenLoc;

		// true if sizeof, false if alignof.
		private boolean isSizeof;

		public SizeOfAlignOfExpr(
				boolean isSizeof,
				QualType operand,
				QualType resultType,
				SourceLocation opLoc,
				SourceLocation rp)
		{
			super(SizeOfAlignOfExprClass, resultType, OK_Ordinary, EVK_RValue, opLoc);
			this.isSizeof = isSizeof;
			isType = true;
			this.operand = operand;
			this.opLoc = opLoc;
			rParenLoc = rp;
		}

		public SizeOfAlignOfExpr(boolean isSizeof,
				Expr subExpr,
				QualType resultType,
				SourceLocation opLoc,
				SourceLocation rp)
		{
			super(SizeOfAlignOfExprClass, resultType, OK_Ordinary, EVK_RValue, opLoc);
			this.isSizeof = isSizeof;
			isType = false;
			this.operand = subExpr;
			this.opLoc = opLoc;
			this.rParenLoc = rp;
		}

		@Override
		public void accept(StmtVisitor v)
		{
		}

		@Override
		public SourceRange getSourceRange()
		{
			return new SourceRange(opLoc, rParenLoc);
		}

		public boolean isArgumentType()
		{
			return isType;
		}

		public boolean isArgumentExpr()
		{
			return !isArgumentType();
		}

		public Expr getArgumentExpr()
		{
			Util.assertion(isArgumentExpr(), "calling getArgumentExpr on type!");
			return (Expr)operand;
		}

		public QualType getArgumentType()
		{
			Util.assertion(isArgumentType(), "calling getArgumentType on expr!");
			return (QualType)operand;
		}

		public void setArgumentType(QualType ty)
		{
			operand = ty;
			isType = true;
		}

		public void setArgumentExpr(Expr e)
		{
			operand = e;
			isType = false;
		}

		public SourceLocation getOperatorLoc()
		{
			return opLoc;
		}

		public SourceLocation getRParenLoc()
		{
			return rParenLoc;
		}

        /**
         * Gets the number of children stmt enclosed in this stmt.
         * @return
         */
        @Override
        public int getNumChildren()
        {
            if (isArgumentType())
            {
                Util.assertion(false, "Should not reaching here");
                return 0;
            }
            return 1;
        }

        /**
         * Obtains the children stmt in the specified position.
         * @param index
         * @return
         */
        @Override
        public Tree getChildren(int index)
        {
            Util.assertion( index >= 0 && index < getNumChildren());
            if (isArgumentType())
            {
                Util.assertion(false, "Should not reaching here");
                return null;
            }
            return getArgumentExpr();
        }

		public boolean isSizeof()
		{
			return isSizeof;
		}

		public QualType getTypeOfArgument()
		{
			return isArgumentType() ? getArgumentType() : getArgumentExpr().getType();
		}
	}

	/**
	 * This is the GNU Statement Expression extension: ({int X=4; X;}).
	 * The StmtExpr contains a single CompoundStmt node, which it evaluates and
	 * takes the value of the last subexpression.
	 */
	public static class StmtExpr extends Expr
	{
		private SourceLocation lParenLoc, rParenLoc;
		private Stmt subStmt;

		public StmtExpr(
		        Stmt compound,
		        QualType type,
				SourceLocation lp,
				SourceLocation rp)
		{
			super(StmtExprClass, type, OK_Ordinary, EVK_RValue, lp);
			this.subStmt = compound;
		}

		@Override
		public void accept(StmtVisitor v)
		{
		}

		@Override
		public SourceRange getSourceRange()
		{
			return new SourceRange(lParenLoc, rParenLoc);
		}

        public Stmt getSubStmt()
        {
            return subStmt;
        }

        public void setSubStmt(Stmt subStmt)
        {
            this.subStmt = subStmt;
        }

        public SourceLocation getLParenLoc()
        {
            return lParenLoc;
        }

        public void setLParenLoc(SourceLocation loc)
        {
            lParenLoc = loc;
        }

        public SourceLocation getRParenLoc()
        {
            return rParenLoc;
        }

        public void setRParenLoc(SourceLocation loc)
        {
            rParenLoc = loc;
        }

        /**
         * Gets the number of children stmt enclosed in this stmt.
         * @return
         */
        @Override
        public int getNumChildren()
        {
            return 1;
        }

        /**
         * Obtains the children stmt in the specified position.
         * @param index
         * @return
         */
        @Override
        public Tree getChildren(int index)
        {
            Util.assertion( index >= 0 && index < getNumChildren());
            return subStmt;
        }
    }

	/**
	 * Represents a C99 designated initializer expression.
	 * <p>
	 * A designated initializer expression (C99 6.7.8) contains one or
	 * more designators (which can be field designators, array
	 * designators, or GNU array-range designators) followed by an
	 * expression that initializes the field or element(s) that the
	 * designators refer to. For example, given:
	 * </p>
	 * <pre>
	 * struct point {
	 *   double x;
	 *   double y;
	 * };
	 * struct point ptarray[10] = { [2].y = 1.0, [2].x = 2.0, [0].x = 1.0 };
	 * </pre>
	 *
	 * The InitListExpr contains three DesignatedInitExprs, the first of
	 * which covers <code>[2].y=1.0</code>. This DesignatedInitExprClass will have two
	 * designators, one array designator for <code>[2]</code> followed by one field
	 * designator for <code>.y</code>. The initalization expression will be 1.0.
	 */
	public static class DesignatedInitExpr extends Expr
    {
        public void expandDesignator(int desigIdx,
                ArrayList<Designator> replacements)
        {
            if (replacements.isEmpty())
                return;

            if (replacements.size() == 1)
            {
                designators[desigIdx] = replacements.get(0);
            }

            Designator[] newDesignators = new Designator[designators.length + replacements.size() - 1];
            System.arraycopy(designators, 0, newDesignators, 0, designators.length - 1);
            Designator[] t = new Designator[replacements.size()];
            replacements.toArray(t);
            System.arraycopy(t, 0, newDesignators, designators.length, t.length);
            designators = newDesignators;
        }

        /**
	     * A field designator, e.g., ".x".
	     */
    	public static class FieldDesignator
	    {
		    /**
		     * Refers to the field that is being initialized. The low bit
		     * of this field determines whether this is actually a pointer
		     * to an IdentifierInfo (if 1) or a FieldDecl (if 0). When
		     * initially constructed, a field designator will store an
		     * IdentifierInfo*. After semantic analysis has resolved that
		     * name, the field designator will instead store a FieldDecl.
		     */
			Object nameOrField;
			int dotLoc;
			int fieldLoc;
	    }

	    /**
	     * A array designator, e.g., "[2]".
	     */
	    public static class ArrayDesignator
	    {
	    	int index;
	    	int lBracketLoc;
	    	int rBracketLoc;
	    }

	    /**
	     * A GNU extension of array range designator. e.g., "[0 ... 2]".
	     */
	    public static class ArrayRangeDesignator
	    {
		    int startIdx;
		    int endIdx;
		    int lBracketLoc;
		    int ellipsisLoc;
		    int rBracketLoc;
	    }

    	public enum DesignatorKind
	    {
	    	FieldDesignator,
		    ArrayDesignator,
		    ArrayRangeDesignator
	    }

    	public static class Designator
	    {
            private DesignatorKind kind;

            private Object desig;

		    /**
		     * Create a field designator and initialize its property according
		     * several arguments passed into this constructor.
		     * @param fieldName
		     * @param dotLoc
		     * @param fieldLoc
		     */
            public Designator(
            		IdentifierInfo fieldName,
		            SourceLocation dotLoc,
		            SourceLocation fieldLoc)
            {
				FieldDesignator d = new FieldDesignator();
				d.dotLoc = dotLoc.getRawEncoding();
				d.fieldLoc = fieldLoc.getRawEncoding();
				d.nameOrField = fieldName;
				desig = d;
				kind = DesignatorKind.FieldDesignator;
            }

		    /**
		     * Creates an array designator with specified arguments.
		     * @param index
		     * @param lBracketLoc
		     * @param rBracketLoc
		     */
		    public Designator(int index,
		            SourceLocation lBracketLoc,
		            SourceLocation rBracketLoc)
            {
            	ArrayDesignator d = new ArrayDesignator();
            	d.index = index;
            	d.lBracketLoc = lBracketLoc.getRawEncoding();
            	d.rBracketLoc = rBracketLoc.getRawEncoding();
            	desig = d;
            	kind = DesignatorKind.ArrayDesignator;
            }

            public Designator(int beginIdx,
		            SourceLocation lBracketLoc,
		            SourceLocation ellipsisLoc,
		            SourceLocation rBracketLoc)
            {
            	ArrayRangeDesignator d = new ArrayRangeDesignator();
            	d.startIdx = beginIdx;
            	d.endIdx = beginIdx+1;
            	d.lBracketLoc = lBracketLoc.getRawEncoding();
            	d.ellipsisLoc = ellipsisLoc.getRawEncoding();
            	d.rBracketLoc = rBracketLoc.getRawEncoding();
            	kind = DesignatorKind.ArrayRangeDesignator;
            }

            public boolean isFieldDesignator()
            {
            	return kind == DesignatorKind.FieldDesignator;
            }

            public boolean isArrayDesignator()
            {
            	return kind == DesignatorKind.ArrayDesignator;
            }

            public boolean isArrayRangeDesignator()
            {
            	return kind == DesignatorKind.ArrayRangeDesignator;
            }

            public IdentifierInfo getFieldName()
            {
	            Util.assertion(isFieldDesignator(), "Only valid on a field designator");

            	Object obj = ((FieldDesignator)desig).nameOrField;
            	if (obj instanceof IdentifierInfo)
            		return (IdentifierInfo)obj;
            	else
            		return getField().getIdentifier();
            }

            public FieldDecl getField()
            {
            	Util.assertion(isFieldDesignator(), "Only valid on a field designator");
	            Object obj = ((FieldDesignator)desig).nameOrField;
	            if (obj instanceof FieldDecl)
		            return (FieldDecl)obj;
	            else
		            return null;
            }

            public void setField(FieldDecl fd)
            {
	            Util.assertion(isFieldDesignator(), "Only valid on a field designator");
	            FieldDesignator d = ((FieldDesignator)desig);
	            d.nameOrField = fd;
            }

            public SourceLocation getDotLoc()
            {
	            Util.assertion(isFieldDesignator(), "Only valid on a field designator");
	            FieldDesignator d = ((FieldDesignator)desig);
	            return SourceLocation.getFromRawEncoding(d.dotLoc);
            }

            public SourceLocation getFieldLoc()
            {
	            Util.assertion(isFieldDesignator(), "Only valid on a field designator");
	            FieldDesignator d = ((FieldDesignator)desig);
	            return SourceLocation.getFromRawEncoding(d.fieldLoc);
            }

		    public SourceLocation getLBracketLoc()
		    {
			    Util.assertion( isArrayDesignator() || isArrayRangeDesignator());
			    if (isArrayDesignator())
			    {
			    	return SourceLocation.getFromRawEncoding(((ArrayDesignator)desig).lBracketLoc);
			    }
			    else
			    {
				    return SourceLocation.getFromRawEncoding(((ArrayRangeDesignator)desig).lBracketLoc);
			    }
		    }

		    public SourceLocation getRBracketLoc()
		    {
			    Util.assertion( isArrayDesignator() || isArrayRangeDesignator());
			    if (isArrayDesignator())
			    {
				    return SourceLocation.getFromRawEncoding(((ArrayDesignator)desig).rBracketLoc);
			    }
			    else
			    {
				    return SourceLocation.getFromRawEncoding(((ArrayRangeDesignator)desig).rBracketLoc);
			    }
		    }

		    public SourceLocation getEllipsisLoc()
		    {
			    Util.assertion( isArrayRangeDesignator());
			    return SourceLocation.getFromRawEncoding(((ArrayRangeDesignator)desig).ellipsisLoc);
		    }

		    public int getFirstExprIndex()
		    {
		    	Util.assertion( isArrayDesignator() || isArrayRangeDesignator());
			    if (isArrayDesignator())
			    {
				    return ((ArrayDesignator)desig).index;
			    }
			    else
			    {
				    return ((ArrayRangeDesignator)desig).startIdx;
			    }
		    }

		    public SourceLocation getStartLocation()
		    {
		    	if (isFieldDesignator())
		    		return getDotLoc().isValid() ? getDotLoc() : getFieldLoc();
		    	else
		    		return getLBracketLoc();
		    }
	    }

	    private Designator[] designators;
        /**
         * The initialization expression or array or array range designator.
         */
	    private Expr[] subExprs;

	    private SourceLocation equalOrColonLoc;

	    private boolean isGnuSyntax;

		public static DesignatedInitExpr create(
				ASTContext ctx,
                ArrayList<Designator> designator,
				ArrayList<Expr> indexExprs,
				SourceLocation equalOrColonLoc,
				boolean gnuStyle,
				Expr init)
		{
			return new DesignatedInitExpr(ctx.VoidTy, designator,
					indexExprs, equalOrColonLoc, gnuStyle, init);
		}

	    public DesignatedInitExpr(
	    		QualType type,
	    		ArrayList<Designator> designator,
	    		ArrayList<Expr> indexExprs,
	    		SourceLocation equalOrColonLoc,
	    		boolean gnuStyle,
	    		Expr init)
	    {
		    super(DesignatedInitExprClass, type,
                    OK_Ordinary, EVK_RValue,
                    designator.get(0).getStartLocation());
		    designators = new Designator[designator.size()];
		    subExprs = new Expr[indexExprs.size() + 1];
		    this.equalOrColonLoc = equalOrColonLoc;
		    this.isGnuSyntax = gnuStyle;

		    int indexIdx = 0;

		    // The first one in sub expr array is the initialization expression.
		    for (int i = 0, e = designator.size(); i != e; i++)
            {
                designators[i] = designator.get(i);

                if (designators[i].isArrayDesignator())
                {
                    int idx = designators[i].getFirstExprIndex();
                    subExprs[indexIdx++] = indexExprs.get(idx);
                }
                else if (designators[i].isArrayRangeDesignator())
                {
                    int idx = designators[i].getFirstExprIndex();
                    subExprs[indexIdx++] = indexExprs.get(idx);
                    subExprs[indexIdx++] = indexExprs.get(idx+1);
                }
            }

		    subExprs[indexIdx++] = init;
            Util.assertion(indexIdx == subExprs.length,  "Wrong number of index expressions!");

	    }

	    @Override
        public void accept(StmtVisitor v)
	    {
	    }

	    public Expr getInit()
        {
            return subExprs[0];
        }

        public void setInit(Expr e)
        {
            subExprs[0] = e;
        }

	    @Override
        public SourceRange getSourceRange()
	    {
            SourceLocation startLoc;
		    Designator firstDesig = designators[0];
            startLoc = firstDesig.getStartLocation();
            return new SourceRange(startLoc, getInit().getSourceRange().getEnd());
	    }

	    public Expr getSubExpr(int idx)
        {
            Util.assertion( idx >= 0 && idx < subExprs.length);
            return subExprs[idx];
        }

        public void setSubExpr(int idx, Expr e)
        {
            Util.assertion( idx >= 0 && idx < subExprs.length);
            subExprs[idx] = e;
        }

        public int getNumSubExprs()
        {
            return subExprs.length;
        }

        public boolean usesGNUSyntax()
        {
            return isGnuSyntax;
        }

        public void setGnuSyntax(boolean gnuSyntax)
        {
            isGnuSyntax = gnuSyntax;
        }

        public SourceLocation getEqualOrColonLoc()
        {
            return equalOrColonLoc;
        }

        public void setEqualOrColonLoc(SourceLocation loc)
        {
            equalOrColonLoc = loc;
        }

        public Designator[] getDesignators()
        {
            return designators;
        }

        public Designator getDesignator(int idx)
        {
            Util.assertion( idx >= 0 && idx < designators.length);
            return designators[idx];
        }

        public void setDesignator(int idx, Designator d)
        {
            Util.assertion( idx >= 0 && idx < designators.length);
            designators[idx] = d;
        }

        public int getSize()
        {
            return designators.length;
        }

        public Expr getArrayIndex(Designator d)
        {
            if (d.isArrayDesignator())
                return subExprs[d.getFirstExprIndex()];
            return null;
        }

        public Expr getArrayRangeStart(Designator d)
        {
            if (d.isArrayRangeDesignator())
                return subExprs[d.getFirstExprIndex()];
            return null;
        }

        public Expr getArrayRangeEnd(Designator d)
        {
            if (d.isArrayRangeDesignator())
                return subExprs[d.getFirstExprIndex()+1];
            return null;
        }

        /**
         * Gets the number of children stmt enclosed in this stmt.
         * @return
         */
        @Override
        public int getNumChildren()
        {
            return subExprs.length;
        }

        /**
         * Obtains the children stmt in the specified position.
         * @param index
         * @return
         */
        @Override
        public Tree getChildren(int index)
        {
            Util.assertion( index >= 0 && index < getNumChildren());
            return subExprs[index];
        }
    }
}
