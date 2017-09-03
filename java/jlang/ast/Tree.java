package jlang.ast;

import jlang.basic.SourceManager;
import tools.APFloat;
import tools.APInt;
import tools.APSInt;
import backend.value.BasicBlock;
import jlang.support.*;
import jlang.clex.IdentifierInfo;
import jlang.sema.*;
import jlang.sema.Decl.*;
import jlang.type.*;
import tools.OutParamWrapper;
import tools.Util;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

import static jlang.ast.CastKind.CK_Invalid;
import static jlang.ast.CastKind.CK_LValueToRValue;
import static jlang.ast.CastKind.CK_NoOp;
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
 * @author Xlous.zeng  
 * @version 1.0
 *
 */
public abstract class Tree
{
	/*
	 * TopLevel nodes, of jlang.type TopLevel, representing entire source files.
	 */
	public static final int TopLevelClass = 1;

	public static final int ImportStmtClass = TopLevelClass + 1;

	public static final int SelectExprClass = ImportStmtClass + 1;

	/**
	 * FunctionProto definitions, of jlang.type MethodDef.
	 */
	public static final int MethodDefStmtClass = SelectExprClass + 1;

	/**
	 * GlobalVariable definitions, of jlang.type VarDef.
	 */
	public static final int VarDefStmtClass = MethodDefStmtClass + 1;

	/**
	 * The no-op statement ";", of jlang.type NullStmt.
	 */
	public static final int NullStmtClass = VarDefStmtClass + 1;

    public static final int DeclStmtClass = NullStmtClass + 1;

	/**
	 * Blocks, of jlang.type CompoundStmt.
	 */
	public static final int CompoundStmtClass = DeclStmtClass + 1;

	/**
	 * Do-while loop statement, of jlang.type Doloop.
	 */
	public static final int DoStmtClass = CompoundStmtClass + 1;

	/**
	 * While loop statement, of jlang.type Whileloop.
	 */
	public static final int WhileStmtClass = DoStmtClass + 1;

	/**
	 * For-loop, of jlang.type Forloop.
	 */
	public static final int ForStmtClass = WhileStmtClass + 1;

	/**
	 * LabelStmt statement, of jlang.type LabelStmt.
	 */
	public static final int LabelledStmtClass = ForStmtClass + 1;

	/**
	 * SwitchStmt statement, of jlang.type SwitchStmt.
	 */
	public static final int SwitchStmtClass = LabelledStmtClass + 1;

	/**
	 * CaseStmt portions in switch statement, of jlang.type CaseStmt.
	 */
	public static final int CaseStmtClass = SwitchStmtClass + 1;

    public static final int DefaultStmtClass = CaseStmtClass + 1;

	/**
	 * ConditionalExpr expression, of jlang.type ConditionalExpr.
	 */
	public static final int ConditionalOperatorClass = DefaultStmtClass + 1;

	/**
	 * IfStmt statements, of jlang.type IfStmt.
	 */
	public static final int IfStmtClass = ConditionalOperatorClass + 1;

	/**
	 * Expression statements, of jlang.type Exec.
	 */
	public static final int ExprStmtClass = IfStmtClass + 1;

	/**
	 * BreakStmt statements, of jlang.type BreakStmt.
	 */
	public static final int BreakStmtClass = ExprStmtClass + 1;

	public static final int GotoStmtClass = BreakStmtClass + 1;

	/**
	 * ContinueStmt statements, of jlang.type ContinueStmt.
	 */
	public static final int ContinueStmtClass = GotoStmtClass + 1;

	/**
	 * ReturnInst statements, of jlang.type ReturnInst.
	 */
	public static final int ReturnStmtClass = ContinueStmtClass + 1;

	/**
	 * FunctionProto invocation expressions, of jlang.type CallExpr.
	 */
	public static final int CallExprClass = ReturnStmtClass + 1;

	/**
	 * Parenthesized subexpressions of jlang.type ParenExpr.
	 */
	public static final int ParenExprClass = CallExprClass + 1;

    public static final int ParenListExprClass = ParenExprClass + 1;


    public static final int InitListExprClass = ParenListExprClass + 1;

	/**
	 * Implicit jlang.type cast expressions.
	 */
	public static final int ImplicitCastClass = InitListExprClass + 1;

    public static final int ExplicitCastClass = ImplicitCastClass + 1;

	/**
	 * ArraySubscriptExpr array expression, of jlang.type ArraySubscriptExpr.
	 */
	public static final int ArraySubscriptExprClass = ExplicitCastClass + 1;

	/**
	 * Simple identifiers, of jlang.type DeclRefExpr.
	 */
	public static final int DeclRefExprClass = ArraySubscriptExprClass + 1;

    /**
     * Assignment expressions, of jlang.type Assign.
     */
    public static final int AssignExprOperatorClass = DeclRefExprClass + 1;

    /**
     * Assignment operators, of jlang.type OpAssign.
     */
    public static final int CompoundAssignOperatorClass = AssignExprOperatorClass
            + 1;

	/**
	 * UnaryExpr operators, of jlang.type UnaryExpr.
	 */
	public static final int UnaryOperatorClass = CompoundAssignOperatorClass + 1;

	public static final int UnaryExprOrTypeTraitClass = UnaryOperatorClass + 1;

	/**
	 * BinaryExpr operators, of jlang.type BinaryExpr.
	 */
	public static final int BinaryOperatorClass = UnaryExprOrTypeTraitClass + 1;

	public static final int MemberExprClass = BinaryOperatorClass + 1;

	public static final int CompoundLiteralExprClass = MemberExprClass + 1;

    public static final int IntegerLiteralClass = CompoundLiteralExprClass + 1;

    public static final int FloatLiteralClass = IntegerLiteralClass + 1;

    public static final int CharacterLiteralClass = FloatLiteralClass + 1;

    public static final int StringLiteralClass = CharacterLiteralClass + 1;

    public static final int SizeOfAlignOfExprClass = StringLiteralClass + 1;

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
	 * Everything in one source file is kept in a TopLevel structure.
	 * 
	 * @author Xlous.zeng  
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
	 * @author Xlous.zeng  
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
            assert isSingleDecl();
            return decls.get(0);
        }

        public ArrayList<Decl> getDeclGroup()
        {
            assert !isSingleDecl();
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
		public Stmt body;
		public Expr cond;
        public SourceLocation doLoc, whileLoc, rParenLoc;

		public DoStmt(Stmt body, Expr cond, 
				SourceLocation doLoc,
                SourceLocation whileLoc, 
				SourceLocation rParenLoc)
		{
			super(DoStmtClass);
			this.body = body;
			this.cond = cond;
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

        public Stmt getBody(){return body;}

        public void setBody(Stmt body)
        {
            this.body = body;
        }

        public Expr getCond(){return cond;}

        public void setCond(Expr cond)
        {
            this.cond = cond;
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
    }

	/**
	 * A for loop.
	 */
	public static class ForStmt extends Stmt
	{
		public Stmt init;
		public Expr cond;
		public Expr step;
		public Stmt body;
        public final SourceLocation lParenLoc, rParenLoc, forLoc;

		public ForStmt(SourceLocation forLoc,
				SourceLocation lParenLoc,
                Stmt init,
				Expr cond,
				Expr step,
				Stmt body,
				SourceLocation rParenLoc)
		{
			super(ForStmtClass);
			this.init = init;
			this.cond = cond;
			this.step = step;
			this.body = body;
            this.lParenLoc = lParenLoc;
            this.forLoc = forLoc;
            this.rParenLoc = rParenLoc;
		}

		public void accept(StmtVisitor v)
		{
			v.visitForStmt(this);
		}

        public Stmt getInit() {return init;}

        public void setInit(Stmt init)
        {
            this.init = init;
        }

        public Expr getCond() {return cond;}

        public void setCond(Expr cond)
        {
            this.cond = cond;
        }

        public Expr getStep() {return step;}

        public void setStep(Expr step)
        {
            this.step = step;
        }

        public Stmt getBody() {return body;}

        public void setBody(Stmt body)
        {
            this.body = body;
        }

        @Override
        public SourceRange getSourceRange()
        {
            return new SourceRange(forLoc, body.getLocEnd());
        }
    }

	/**
	 * A while loop
	 */
	public static class WhileStmt extends Stmt
	{
		public Expr cond;
		public Stmt body;
        public SourceLocation whileLoc;
		public WhileStmt(Expr cond, Stmt body, SourceLocation whileLoc)
		{
			super(WhileStmtClass);
			this.cond = cond;
			this.body = body;
            this.whileLoc = whileLoc;
		}

		public void accept(StmtVisitor v)
		{
			v.visitWhileStmt(this);
		}

        public Expr getCond(){return cond;}

        public void setCond(Expr cond)
        {
            this.cond = cond;
        }

        public Stmt getBody(){return body;}

        public void setBody(Stmt body)
        {
            this.body = body;
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
            return new SourceRange(whileLoc, body.getLocEnd());
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
            return label.getDeclName();
        }

        public String getName()
        {
            return label.getDeclName().getName();
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
    }

	/**
	 * An "if ( ) { } else { }" block
	 */
	public static class IfStmt extends Stmt
	{
		private Expr cond;
		private Stmt thenpart;
		private Stmt elsepart;
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
			this.cond = cond;
			this.thenpart = thenpart;
			this.elsepart = elsepart;
            this.ifLoc = ifLoc;
            this.elseLoc = elseLoc;
		}

		public void accept(StmtVisitor v){ v.visitIfStmt(this);}

		public Expr getCond()
		{
			return cond;
		}

        public Stmt getThenpart()
        {
            return thenpart;
        }

        public Stmt getElsepart()
        {
            return elsepart;
        }

        public Stmt getThenPart() {return thenpart;}

		public Stmt getElsePart() {return elsepart;}

		public SourceLocation getIfLoc() {return  ifLoc;}

        public SourceLocation getElseLoc()
        {
            return elseLoc;
        }

        @Override
        public SourceRange getSourceRange()
        {
            if (elsepart == null)
                return new SourceRange(ifLoc, elsepart.getLocEnd());
            else
                return new SourceRange(ifLoc, thenpart.getLocEnd());
        }
    }

	/**
	 * A "switch ( ) { }" construction.
	 */
	public static class SwitchStmt extends Stmt
	{
		private Expr cond;
        // This points to a linked list of case and default statements.
		private SwitchCase firstCase;
        private Stmt body;
        private SourceLocation switchLoc;
        /**
         * A flag which indicates whether all enum values are covered in current
         * switch statement by condition X - 'switch (X)'.
         */
        private boolean allEnumCasesCovered;

		public SwitchStmt(Expr cond, SourceLocation switchLoc)
		{
			super(SwitchStmtClass);
            this.cond = cond;
            this.switchLoc = switchLoc;
		}

		public void setBody(Stmt body)
        {
            this.body = body;
        }

        public Stmt getBody()
        {
            return body;
        }

        public Expr getCond()
        {
            return cond;
        }

        public void setCond(Expr cond)
        {
            this.cond = cond;
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
            assert sc.getNextSwitchCase() == null
                    : "case/default already added to a switch";
            sc.setNextSwitchCase(firstCase);
            firstCase = sc;
        }

        @Override
        public SourceRange getSourceRange()
        {
            return new SourceRange(switchLoc, body.getLocEnd());
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
        public Expr value;
        public Stmt subStmt;
        public final SourceLocation caseLoc;
        public final SourceLocation colonLoc;

        public CaseStmt(
		        Expr value,
		        Stmt caseBody,
		        SourceLocation caseLoc,
		        SourceLocation colonLoc)
        {
            super(CaseStmtClass);
            this.value = value;
            this.subStmt = caseBody;
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
            return subStmt;
        }

        public Expr getCondExpr()
        {
            return value;
        }

        public void setCondExpr(Expr val)
        {
            value = val;
        }

        @Override
        public SourceRange getSourceRange()
        {
            CaseStmt cs = this;
            while (cs.getSubStmt() instanceof CaseStmt)
                cs = (CaseStmt) cs.getSubStmt();

            return new SourceRange(caseLoc, cs.getSubStmt().getLocEnd());
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
            setType(type);
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
            assert type != null;
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
                    return et.getDecl().getPromotionType().isSignedIntegerType();
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
        public boolean isConstantInitializer()
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
                    return init.isConstantInitializer();
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
                        if (!expr.getInitAt(i).isConstantInitializer())
                            return false;
                    }
                    return true;
                }
                case ParenExprClass:
                    return ((ParenExpr)this).getSubExpr().isConstantInitializer();
                case UnaryOperatorClass:
                    break;
                case BinaryOperatorClass:
                    break;
                case ImplicitCastClass:
                case ExplicitCastClass:
                {
                    // Handle casts with a destination that's a struct or union; this
                    // deals with both the gcc no-op struct cast extension and the
                    // cast-to-union extension.
                    if (getType().isRecordType())
                    {
                        return ((CastExpr)this).getSubExpr().isConstantInitializer();
                    }

                    if (getType().isIntegerType()
                            && ((CastExpr)this).getSubExpr().getType().isIntegerType() )
                    {
                        return ((CastExpr)this).getSubExpr().isConstantInitializer();
                    }
                    break;
                }
            }
            return isEvaluatable();
        }

        /**
         * Call {@linkplain #evaluate(EvalResult, ASTContext)} to see if this expression can
         * be constant folded, but get rid of evaluation result.
         * @return
         */
        public boolean isEvaluatable()
        {
            OutParamWrapper<EvalResult> result = new OutParamWrapper<>();
            return evaluate(result.get(), null) && !result.get().hasSideEffects();
        }

	    /**
	     * Evaluates this expression and return a folded integer.
	     * @return
	     */
	    public APSInt evaluateKnownConstInt()
	    {
	        EvalResult res = new EvalResult();
	        boolean result = evaluate(res, null);
            assert result:"Cound not evaluate expression";
            assert res.val.isInt():"Expression did not be evaluated into integer.";
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
			    OutParamWrapper<APSInt> iceResult,
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
			    assert false:"ICE cannot be evaluated!";
		    assert !evalResult.hasSideEffects :"ICE with side effect!";
		    assert evalResult.val.isInt() :"ICE is not integer!";
		    iceResult.set(evalResult.val.getInt());
		    return true;
	    }

	    public boolean isIntegerConstantExpr(ASTContext ctx)
	    {
	    	OutParamWrapper<APSInt> x = new OutParamWrapper<>(new APSInt());
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
	    		OutParamWrapper<SourceLocation> loc,
			    OutParamWrapper<SourceRange> r1,
			    OutParamWrapper<SourceRange> r2)
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
            OutParamWrapper<APSInt> x = new OutParamWrapper<>(new APSInt());
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
		    assert result:"Could not evaluate expression";
		    assert evalResult.val.isInt() :"Expression did not evaluated to integer";
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
                assert val.isLValue();
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
	     *  - name, where name must be a variable
	     *  - e[i]
	     *  - (e), where e must be an lvalue
	     *  - e.name, where e must be an lvalue
	     *  - e->name
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
			    OutParamWrapper<SourceLocation> loc)
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
	    @Override public SourceRange getSourceRange()
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
            assert type.isIntegerType():"Illegal type in Integer literal.";
            assert value.getBitWidth() == ctx.getTypeSize(type)
                    :"Integer type is not the correct for constant.";
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
            super(CharacterLiteralClass, type, OK_Ordinary, EVK_RValue, null);
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
            super(ParenExprClass, l);
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
            assert idx>= 0&& idx< exprs.size();
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
	    @Override public SourceRange getSourceRange()
	    {
		    return new SourceRange(lParenLoc, rParenLoc);
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
            assert castKind != CK_Invalid :"creating cast with invalid cast kind.";
            this.expr = expr;
            this.castKind = castKind;
        }
        public CastKind getCastKind() { return castKind; }
        public Expr getSubExpr() { return expr;}
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
            subExprs = new Expr[2];
            subExprs[0] = indexed;
            subExprs[1] = index;
            this.rBracketLoc = rBracketLoc;
        }

        public ArraySubscriptExpr()
        {
            super(ArraySubscriptExprClass, SourceLocation.NOPOS);
        }

        public Expr getLHS()
        {
            return subExprs[0];
        }

        public void setLHS(Expr e)
        {
            subExprs[0] = e;
        }

        public Expr getRHS()
        {
            return subExprs[1];
        }

        public void setRHS(Expr e)
        {
            subExprs[1] = e;
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
    }

    /**
	 * Represents a function call (C99 6.5.2.2).
     * {@linkplain CallExpr} itself represents a normal function call,
     * e.g."f(x,2)",
	 */
	public static final class CallExpr extends Expr
	{
		/**
		 * The getIdentifier of callee method.
		 */
		public Expr fn;
		/**
		 * The formal parameters list.
		 */
		public ArrayList<Expr> args;

		SourceLocation rparenLoc;

		public CallExpr(Expr fn,
                ArrayList<Expr> args,
                QualType resultType,
                ExprValueKind vk,
                SourceLocation rparenLoc)
		{
			super(CallExprClass, resultType, OK_Ordinary, vk, rparenLoc);
			this.fn = fn;
			this.args = args;
			this.rparenLoc = rparenLoc;
		}

		public CallExpr()
        {
            super(CallExprClass, SourceLocation.NOPOS);
        }

        public Expr getCallee()
        {
            return fn;
        }

        public void setCallee(Expr fn)
        {
            this.fn = fn;
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
            return args;
        }

        public Expr getArgAt(int idx)
        {
            assert idx>= 0 && idx<args.size():"Arg access out of range!";
            return args.get(idx);
        }

        public void setArgAt(int idx, Expr e)
        {
            assert idx>= 0 && idx<args.size():"Arg access out of range!";
            args.set(idx, e);
        }

        public int getNumCommas()
        {
            return args != null && !args.isEmpty()
                    ? args.size() - 1 :0;
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
			return args.size();
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
				for (int i = numArgs, e = getNumArgs(); i < e; i++)
					args.remove(i);

				args.trimToSize();
				return;
			}
			// Otherwise, null out new args.
			for (int i = numArgs - getNumArgs(); i != 0; --i)
			{
				args.add(null);
			}
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
         * compound literal like "(int){4}".  This can be null if this is a
         * synthesized compound expression.
         */
        private SourceLocation lParenLoc;

        /**
         * This can be an incomplete array jlang.type, in
         * which case the actual expression jlang.type will be different.
         */
        private QualType ty;
        private Expr init;
        private boolean isFileScope;


        public CompoundLiteralExpr(SourceLocation lParenLoc,
                QualType type,
                ExprValueKind valuekind,
                Expr init,
                boolean fileScope)
        {
            super(CompoundLiteralExprClass, type, OK_Ordinary, valuekind, lParenLoc);
            this.lParenLoc = lParenLoc;
            ty = type;
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
	}

	public static final class UnaryExprOrTypeTraitExpr extends Expr
    {
        /**
         * true if operand is a jlang.type, false if it is an expression.
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
            v.visitUnaryExprOrTypeTraitExpr(this);
        }

	    /**
	     * Obtains a source range from the lexical start to the lexical end in
	     * source program.
	     *
	     * @return
	     */
	    @Override public SourceRange getSourceRange()
	    {
		    return new SourceRange(opLoc, rParenLoc);
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

            assert !isCompoundAssignmentOp()
                    : "Use ArithAssignBinaryOperator for compound assignments";
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

		public Expr getLHS() { return rhs; }
        public void setLHS(Expr e) { lhs = e;}
        public Expr getRHS() { return rhs; }
        public void setRHS(Expr e) { rhs = e;}

        public static final String getOpcodeStr(BinaryOperatorKind op)
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
	}

    /**
     * For compound assignments (e.g. +=), we keep
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

            assert isCompoundAssignmentOp():
                    "Only should be used for compound assignments";
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
        public Expr cond;
        public Expr truepart;
        public Expr falsepart;
        private SourceLocation qLoc, cLoc;
        public ConditionalExpr(Expr cond, SourceLocation qLoc,
                Expr lhs,
                SourceLocation cLoc,
                Expr rhs,
                QualType t,
                ExprValueKind vk)
        {
            super(ConditionalOperatorClass, t, OK_Ordinary, vk, cond.getExprLocation());
            this.cond = cond;
            truepart = lhs;
            falsepart = rhs;
            this.qLoc = qLoc;
            this.cLoc = cLoc;
        }

        public void accept(StmtVisitor v)
        {
            v.visitConditionalExpr(this);
        }

        public Expr getCond()
        {
            return cond;
        }

        public Expr getTrueExpr()
        {
            return truepart;
        }

        public Expr getFalseExpr()
        {
            return falsepart;
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
		    return new SourceRange(cond.getLocStart(), falsepart.getLocEnd());
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
        // TODO improve it in the future. 2016.10.15 xlous.zeng.
        private SourceLocation lBraceLoc, rBraceLoc;
        private ArrayList<Expr> initExprs;

        public InitListExpr(SourceLocation lBraceLoc,
		        SourceLocation rBraceLoc,
		        ArrayList<Expr> initList)
        {
            super(InitListExprClass, lBraceLoc);
            this.lBraceLoc = lBraceLoc;
            this.rBraceLoc = rBraceLoc;
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

        public Expr getInitAt(int i)
        {
            assert i >= 0 && i < getNumInits();
            return initExprs.get(i);
        }

	    /**
	     * Obtains a source range from the lexical start to the lexical end in
	     * source program.
	     *
	     * @return
	     */
	    @Override public SourceRange getSourceRange()
	    {
		    return new SourceRange(lBraceLoc, rBraceLoc);
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

		public SizeOfAlignOfExpr(
				QualType operand,
				QualType resultType,
				SourceLocation opLoc,
				SourceLocation rp)
		{
			super(SizeOfAlignOfExprClass, resultType, OK_Ordinary, EVK_RValue, opLoc);
			isType = true;
			this.operand = operand;
			this.opLoc = opLoc;
			rParenLoc = rp;
		}

		public SizeOfAlignOfExpr(
				Expr operand,
				QualType resultType,
				SourceLocation opLoc,
				SourceLocation rp)
		{
			super(SizeOfAlignOfExprClass, resultType, OK_Ordinary, EVK_RValue, opLoc);
			isType = false;
			this.operand = operand;
			this.opLoc = opLoc;
			rParenLoc = rp;
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
			assert isArgumentExpr():"calling getArgumentExpr on type!";
			return (Expr)operand;
		}

		public QualType getArgumentType()
		{
			assert isArgumentType():"calling getArgumentType on expr!";
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
	}
}
