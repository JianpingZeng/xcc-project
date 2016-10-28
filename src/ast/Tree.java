package ast;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import cparser.DeclSpec.SourceRange;
import sema.*;
import hir.BasicBlock;
import sema.Decl.*;
import symbol.Symbol;
import symbol.Symbol.MethodSymbol;
import symbol.VarSymbol;
import type.*;
import utils.Name;
import utils.OutParamWrapper;
import utils.Position;
import utils.Util;

import static ast.CastKind.CK_Invalid;
import static ast.Tree.ExprValueKind.EVK_RValue;
import static sema.BinaryOperatorKind.*;
import static sema.UnaryOperatorKind.*;
import static type.Type.IntTy;
import static type.Type.UnsignedIntTy;

/**
 * Root class for abstract syntax tree nodes. It provides definitions for
 * specific tree nodes as subclasses. There are 40 such subclasses.
 * 
 * Each subclass is highly standardized. It generally isDeclScope only tree fields
 * for the syntactic sub-components of the node. Some classes that represents
 * identifier usesList or definitions also define a Symbol field that denotes the
 * represented identifier. Classes for non-local jumps also carry the jump
 * targetAbstractLayer as a field. The root class {@link Tree} itself defines fields for the
 * tree's type and position. No other fields are kept in a tree node; instead
 * parameters are passed to methods accessing the node.
 * 
 * The only method defined in subclasses is 'visit' which applies a given
 * visitor to the tree. The actual tree processing is done by visitor classes in
 * other package. The abstract class {@link ASTVisitor} is the abstract root
 * class for visiting different tree.
 * 
 * @author Xlous.zeng  
 * @version 1.0
 *
 */
abstract public class Tree
{
	/*
	 * TopLevel nodes, of type TopLevel, representing entire source files.
	 */
	public static final int TopLevelClass = 1;

	public static final int ImportStmtClass = TopLevelClass + 1;

	public static final int SelectExprClass = ImportStmtClass + 1;

	/**
	 * Function definitions, of type MethodDef.
	 */
	public static final int MethodDefStmtClass = SelectExprClass + 1;

	/**
	 * Variable definitions, of type VarDef.
	 */
	public static final int VarDefStmtClass = MethodDefStmtClass + 1;

	/**
	 * The no-op statement ";", of type NullStmt.
	 */
	public static final int NullStmtClass = VarDefStmtClass + 1;

    public static final int DeclStmtClass = NullStmtClass + 1;

	/**
	 * Blocks, of type CompoundStmt.
	 */
	public static final int CompoundStmtClass = DeclStmtClass + 1;

	/**
	 * Do-while loop statement, of type Doloop.
	 */
	public static final int DoStmtClass = CompoundStmtClass + 1;

	/**
	 * While loop statement, of type Whileloop.
	 */
	public static final int WhileStmtClass = DoStmtClass + 1;

	/**
	 * For-loop, of type Forloop.
	 */
	public static final int ForStmtClass = WhileStmtClass + 1;

	/**
	 * LabelledStmt statement, of type LabelledStmt.
	 */
	public static final int LabelledStmtClass = ForStmtClass + 1;

	/**
	 * SwitchStmt statement, of type SwitchStmt.
	 */
	public static final int SwitchStmtClass = LabelledStmtClass + 1;

	/**
	 * CaseStmt portions in switch statement, of type CaseStmt.
	 */
	public static final int CaseStmtClass = SwitchStmtClass + 1;

    public static final int DefaultStmtClass = CaseStmtClass + 1;

	/**
	 * ConditionalExpr expression, of type ConditionalExpr.
	 */
	public static final int CondtionalOperatorClass = CaseStmtClass + 1;

	/**
	 * IfStmt statements, of type IfStmt.
	 */
	public static final int IfStmtClass = CondtionalOperatorClass + 1;

	/**
	 * Expression statements, of type Exec.
	 */
	public static final int ExprStmtClass = IfStmtClass + 1;

	/**
	 * BreakStmt statements, of type BreakStmt.
	 */
	public static final int BreakStmtClass = ExprStmtClass + 1;

	public static final int GotoStmtClass = BreakStmtClass + 1;

	/**
	 * ContinueStmt statements, of type ContinueStmt.
	 */
	public static final int ContinueStmtClass = BreakStmtClass + 1;

	/**
	 * ReturnInst statements, of type ReturnInst.
	 */
	public static final int ReturnStmtClass = ContinueStmtClass + 1;

	/**
	 * Function invocation expressions, of type CallExpr.
	 */
	public static final int CallExprClass = ReturnStmtClass + 1;

	/**
	 * Parenthesized subexpressions of type ParenExpr.
	 */
	public static final int ParenExprClass = CallExprClass + 1;

    public static final int ParenListExprClass = ParenExprClass + 1;


    public static final int InitListExprClass = ParenListExprClass + 1;

	/**
	 * Implicit type cast expressions.
	 */
	public static final int ImplicitCastClass = InitListExprClass + 1;

    public static final int ExplicitCastClass = ImplicitCastClass + 1;

	/**
	 * ArraySubscriptExpr array expression, of type ArraySubscriptExpr.
	 */
	public static final int ArraySubscriptExprClass = ExplicitCastClass + 1;

	/**
	 * Simple identifiers, of type DeclRefExpr.
	 */
	public static final int DeclRefExprClass = ArraySubscriptExprClass + 1;

    /**
     * Assignment expressions, of type Assign.
     */
    public static final int AssignExprOperatorClass = DeclRefExprClass + 1;

    /**
     * Assignment operators, of type OpAssign.
     */
    public static final int CompoundAssignOperatorClass = AssignExprOperatorClass
            + 1;

	/**
	 * UnaryExpr operators, of type UnaryExpr.
	 */
	public static final int UnaryOperatorClass = CompoundAssignOperatorClass + 1;

	public static final int UnaryExprOrTypeTraitClass = UnaryOperatorClass + 1;

	/**
	 * BinaryExpr operators, of type BinaryExpr.
	 */
	public static final int BinaryOperatorClass = UnaryExprOrTypeTraitClass + 1;

	public static final int MemberExprClass = BinaryOperatorClass + 1;

	public static final int CompoundLiteralExprClass = MemberExprClass + 1;

    public static final int IntegerLiteralClass = DeclRefExprClass + 1;

    public static final int FloatLiteralClass = IntegerLiteralClass + 1;

    public static final int CharacterLiteralClass = FloatLiteralClass + 1;

    public static final int StringLiteralClass = CharacterLiteralClass + 1;

	/**
	 * Error Trees, of type ErroneousTree.
	 */
	public static final int ErroneousStmtClass = StringLiteralClass + 1;

	public static List<Tree> emptyList = new LinkedList<>();

	/**
	 * The encoded position of current tree.
	 */
	public int pos;

	/**
	 * The tag that represents the kind of this tree.
	 */
	public int tag;

	/**
	 * Constructor. Initialize tree with given tag.
	 * 
	 * @param tag
	 */
	public Tree(int tag)
	{
		this.tag = tag;
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

	/**
	 * Set the position and return this tree.
	 * 
	 * @param pos a given position.
	 * @return
	 */
	public Tree setPos(int pos)
	{
		this.pos = pos;
		return this;
	}

	public int getStmtClass()
    {
        return tag;
    }

	/**
	 * Visit this tree with a given visitor.
	 */
	public abstract void accept(ASTVisitor v);

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
		 * @param sourceFile the name of source file.
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
		public void accept(ASTVisitor v)
		{
			v.visitTopLevel(this);
		}
	}

	/**
	 * @author Xlous.zeng  
	 * @version 0.1
	 */
	public static class NullStmt extends Stmt
	{
	    final int loc;
		public NullStmt(int loc)
		{
			super(NullStmtClass);
            this.loc = loc;
		}

		public void accept(ASTVisitor v)
		{
			v.visitNullStmt(this);
		}
	}

	/**
	 * @author Xlous.zeng  
	 * @version 0.1
	 */
	public static class MethodDef extends Tree
	{
		public long flags;
		public Name name;
		public Tree rettype;
		public List<Tree> params;
		public Tree body;
		public MethodSymbol sym;

		public MethodDef(Long flags, Name name, Tree rettype, 
				List<Tree> params, Tree body, MethodSymbol sym)
		{
			super(MethodDefStmtClass);
			this.flags = flags;
			this.name = name;
			this.rettype = rettype;
			this.params = params;
			this.body = body;
			this.sym = sym;
		}

		public void accept(ASTVisitor v)
		{

		}
	}

	/**
	 * @author Xlous.zeng  
	 * @version 0.1
	 */
	public static class VarDef extends Tree
	{
		/**
		 * Is static or const?
		 */
		public long flags;
		public Name name;
		public Tree varType;
		public Tree init;
		public VarSymbol sym;

		public VarDef(long flags, Name name, Tree varType,
				Tree init, VarSymbol sym)
		{
			super(VarDefStmtClass);
			this.flags = flags;
			this.name = name;
			this.varType = varType;
			this.init = init;
			this.sym = sym;
		}

		public void accept(ASTVisitor v)
		{
			//v.visitVarDef(this);
		}
	}

	public abstract static class Stmt extends Tree
    {
        Stmt(int tag)
        {
            super(tag);
        }
    }

    /**
     * This
     */
    public static class DeclStmt extends Stmt implements Iterable<Decl>
    {
        final ArrayList<Decl> decls;
        final int declStart, declEnd;
	    public DeclStmt(ArrayList<Decl> decls, int declStart, int declEnd)
	    {
		    super(DeclStmtClass);
            this.decls = decls;
            this.declStart = declStart;
            this.declEnd = declEnd;
	    }

        public void accept(ASTVisitor v)
        {
            v.visitDeclStmt(this);
        }

        public Iterator<Decl> iterator()
        {
            return decls.iterator();
        }
    }

	/**
	 * A statement block.
	 *
	 */
	public static class CompoundStmt extends Stmt
	{
		public List<Stmt> stats;
		public int endpos = Position.NOPOS;

		public CompoundStmt(List<Stmt> stats, int loc)
		{
			super(CompoundStmtClass);
			this.stats = stats;
            this.endpos = loc;
		}

		public void accept(ASTVisitor v)
		{
			v.visitCompoundStmt(this);
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
		 * name of field to select
		 */
		public Name name;
		/**
		 * symbol of the selected class
		 */
		public Symbol sym;

		public SelectStmt(Tree selected, Name name, Symbol sym)
		{
			super(SelectExprClass);
			this.selected = selected;
			this.name = name;
			this.sym = sym;
		}

		public void accept(ASTVisitor v)
		{
			v.visitSelectStmt(this);
		}
	}
	/**
	 * A do loop
	 */
	public static class DoStmt extends Stmt
	{
		public Stmt body;
		public Expr cond;
        public int doLoc, whileLoc, rParenLoc;

		public DoStmt(Stmt body, Expr cond, int doLoc,
                int whileLoc, int rParenLoc)
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

		public void accept(ASTVisitor v)
		{
			v.visitDoStmt(this);
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
        public final int lParenLoc, rParenLoc, forLoc;

		public ForStmt(int forLoc,
                int lParenLoc,
                Stmt init, Expr cond, Expr step, Stmt body,
                int rParenLoc)
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

		public void accept(ASTVisitor v)
		{
			v.visitForStmt(this);
		}
	}

	/**
	 * A while loop
	 */
	public static class WhileStmt extends Stmt
	{
		public Expr cond;
		public Stmt body;
        public int whileLoc;
		public WhileStmt(Expr cond, Stmt body, int whileLoc)
		{
			super(WhileStmtClass);
			this.cond = cond;
			this.body = body;
            this.whileLoc = whileLoc;
		}

		public void accept(ASTVisitor v)
		{
			v.visitWhileStmt(this);
		}
	}

	/**
	 * A labelled expression or statement.
	 */
	public static class LabelledStmt extends Stmt
	{
		public LabelDecl label;
		public Stmt body;
        public final int loc;
		/** The corresponding basic block of this label.*/
		public BasicBlock corrBB;

		public LabelledStmt(LabelDecl label, Stmt body, int loc)
		{
			super(LabelledStmtClass);
			this.label = label;
			this.body = body;
            this.loc = loc;
		}

		public void accept(ASTVisitor v)
		{
			v.visitLabelledStmt(this);
		}
	}

	/**
	 * An "if ( ) { } else { }" block
	 */
	public static class IfStmt extends Stmt
	{
		public Expr cond;
		public Stmt thenpart;
		public Stmt elsepart;
        public final int ifLoc;
		public IfStmt(Expr cond, Stmt thenpart, Stmt elsepart, int ifLoc)
		{
			super(IfStmtClass);
			this.cond = cond;
			this.thenpart = thenpart;
			this.elsepart = elsepart;
            this.ifLoc = ifLoc;
		}

		public void accept(ASTVisitor v)
		{
			v.visitIfStmt(this);
		}
	}

	/**
	 * A "switch ( ) { }" construction.
	 */
	public static class SwitchStmt extends Stmt
	{
		private Expr cond;
		private CaseStmt firstCase;
        private Stmt body;
        private int switchLoc;
        /**
         * A flag which indicates whether all enum values are covered in current
         * switch statement by condition X - 'switch (X)'.
         */
        private boolean allEnumCasesCovered;

		public SwitchStmt(Expr cond, int switchLoc)
		{
			super(SwitchStmtClass);
            this.cond = cond;
            this.switchLoc = switchLoc;
		}

		public void setBody(Stmt body)
        {
            this.body = body;
        }

        public Expr getCond()
        {
            return cond;
        }

        public int getSwitchLoc()
        {
            return switchLoc;
        }

		public void accept(ASTVisitor v)
		{
			v.visitSwitchStmt(this);
		}

        public CaseStmt getSwitchCaseList()
        {
            return firstCase;
        }

        public void setAllEnumCasesCovered()
        {
            allEnumCasesCovered = true;
        }

        public boolean getAllEnumCasesCovered()
        {
            return allEnumCasesCovered;
        }
    }

    public static abstract class SwitchCase extends Stmt
    {
        public SwitchCase(int tag)
        {
            super(tag);
        }
        public abstract SwitchCase getNextCaseStmt();

        public abstract int getCaseLoc();
        public abstract int getColonLoc();
    }
	/**
	 * A "case  :" of a switch.
	 */
	public static class CaseStmt extends SwitchCase
    {
        public Expr value;
        public Stmt subStmt;
        public final int caseLoc;
        public final int colonLoc;

        public CaseStmt(Expr value, Stmt caseBody, int caseLoc, int colonLoc)
        {
            super(CaseStmtClass);
            this.value = value;
            this.subStmt = caseBody;
            this.caseLoc = caseLoc;
            this.colonLoc = colonLoc;
        }

        public int getCaseLoc()
        {
            return caseLoc;
        }

        public int getColonLoc()
        {
            return colonLoc;
        }

		public void accept(ASTVisitor v)
		{
			v.visitCaseStmt(this);
		}

        @Override
        public SwitchCase getNextCaseStmt()
        {
            if (subStmt.tag == CaseStmtClass)
                return (CaseStmt)subStmt;
            else
                return null;
        }

        public Expr getCondExpr()
        {
            return value;
        }

        public void setCondExpr(Expr val)
        {
            value = val;
        }
    }

    public static class DefaultStmt extends SwitchCase
    {
        public final int defaultLoc;
        public final int colonLoc;
        public Stmt subStmt;
        public DefaultStmt(int defaultLoc, int colonLoc, Stmt subStmt)
        {
            super(DefaultStmtClass);
            this.defaultLoc =defaultLoc;
            this.colonLoc = colonLoc;
        }

        @Override
        public SwitchCase getNextCaseStmt()
        {
            if (subStmt.tag == CaseStmtClass)
                return (DefaultStmt)subStmt;
            else
                return null;
        }

        @Override
        public int getCaseLoc()
        {
            return colonLoc;
        }

        public int getColonLoc()
        {
            return defaultLoc;
        }

        @Override
        public void accept(ASTVisitor v)
        {
            v.visitDefaultStmt(this);
        }
    }

	/**
	 * A break from a loop or switch.
	 */
	public static class BreakStmt extends Stmt
	{
		public final int breakLoc;

		public BreakStmt(int breakLoc)
		{
			super(BreakStmtClass);
			this.breakLoc = breakLoc;
		}

		public void accept(ASTVisitor v)
		{
			v.visitBreakStmt(this);
		}
	}

	public static class GotoStmt extends Stmt
	{
		public LabelDecl label;
		public final int gotoLoc, labelLoc;

		public GotoStmt(LabelDecl label, int gotoLoc, int labelLoc)
		{
			super(GotoStmtClass);
			this.label = label;
			this.gotoLoc = gotoLoc;
            this.labelLoc = labelLoc;
		}
		public void accept(ASTVisitor v)
		{
			v.visitGotoStmt(this);
		}
	}

	/**
	 * A continue of a loop.
	 */
	public static class ContinueStmt extends Stmt
	{
		public final int continueLoc;

		public ContinueStmt(int continueLoc)
		{
			super(ContinueStmtClass);
			this.continueLoc = continueLoc;
		}

		public void accept(ASTVisitor v)
		{
			v.visitContinueStmt(this);
		}
	}

	/**
	 * A return statement.
	 */
	public static class ReturnStmt extends Stmt
	{
		public Expr expr;
        public final int returnloc;
		public ReturnStmt(int returnloc, Expr expr)
		{
			super(ReturnStmtClass);
            this.returnloc = returnloc;
			this.expr = expr;
		}

        public ReturnStmt(int returnloc)
        {
            this(returnloc, null);
        }

		public void accept(ASTVisitor v)
		{
			v.visitReturnStmt(this);
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
     * used on any scenerio where a {@linkplain Stmt} rquired.
     */
	public abstract static class Expr extends Stmt
    {
        private QualType type;
        private ExprValueKind valuekind;
        private int loc;

        public Expr(int tag, QualType type, ExprValueKind valuekind, int loc)
        {
            super(tag);
            this.valuekind = valuekind;
            setType(type);
            this.loc = loc;
        }

        public Expr(int tag, int loc)
        {
            this(tag, null, null, loc);
        }
        public int getLocation()
        {
            return loc;
        }
        public void setType(QualType type)
        {
            assert type != null;
            this.type = type;
        }
        public QualType getType() { return type; }

        /**
         * Whether this is a promotable bitfield reference according to C99 6.3.1.1p2.
         * or {@code null} if no promotion occurs.
         * @return
         */
        public QualType isPromotableBitField()
        {
            FieldDecl field = getBitField();
            if (field == null)
                return new QualType();
            QualType t = field.getDeclType();
            long bitWidth = field.getBitWidthValue();
            long intSize = IntTy.getType().getTypeSize();

            // GCC extension compatibility: if the bit-field getTypeSize is less than or equal
            // to the getTypeSize of int, it gets promoted no matter what its type is.
            // For instance, unsigned long bf : 4 gets promoted to signed int.
            if (bitWidth < intSize)
                return IntTy;
            if (bitWidth == intSize)
                return t.getType().isSignedType() ? IntTy : UnsignedIntTy;

            // Types bigger than int are not subject to promotions, and therefore act
            // like the base type.
            return new QualType();
        }

        public FieldDecl getBitField()
        {
            Expr e = ignoreParens();
            return null;
        }

        public Expr ignoreParens()
        {
            Expr e = this;
            while (true)
            {
                if (e.tag == ParenExprClass)
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

        public long getIntWidth()
        {
            QualType t = null;
            if (type.getType().isEnumType())
                t = type.getType().getEnumType().getDecl().getIntegerType();
            if (type.getType().isBooleanType())
                return 1;
            // for the primitive type, just use the standard type size.
            return t.getTypeSize();
        }

        public boolean isSignedIntegerOrEnumeration()
        {
            if (type.getType().isPrimitiveType())
            {
                return type.getType().getTypeKind() >= TypeClass.Char
                        && type.getType().getTypeKind() <= TypeClass.LongInteger;
            }

            if (type.getType().isEnumType())
            {
                EnumType et = type.getType().getEnumType();
                if (et.getDecl().isCompleteDefinition())
                    return et.getDecl().getIntegerType().getType().isSignedType();
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
        public boolean evaluate(EvalResult result)
        {
            return ExprEvaluatorBase.evaluate(result, this);
        }

        public boolean isLValue()
        {
            return valuekind == ExprValueKind.EVK_LValue;
        }

        public boolean isRValue()
        {
            return valuekind == EVK_RValue;
        }

        public ExprValueKind getValuekind()
        {
            return valuekind;
        }

        public Expr ignoreParensImpCasts()
        {
            Expr e = this;
            while (true)
            {
                if (e.tag == ParenExprClass)
                {
                    e = ((ParenExpr)e).subExpr;
                    continue;
                }
                if (e.tag == ImplicitCastClass)
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
                if (e.tag == ParenExprClass)
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
            switch (tag)
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
                    ast.Tree.InitListExpr expr = (Tree.InitListExpr)this;
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
         * Call {@linkplain #evaluate(EvalResult)} to see if this expression can
         * be constant folded, but get rid of evaluation result.
         * @return
         */
        public boolean isEvaluatable()
        {
            OutParamWrapper<EvalResult> result = new OutParamWrapper<>();
            return evaluate(result.get()) && !result.get().hasSideEffects();
        }

	    /**
	     * Evaluates this expression and return a folded integer.
	     * @return
	     */
	    public APSInt evaluateKnownConstInt()
	    {
	        EvalResult res = new EvalResult();
	        boolean result = evaluate(res);
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

            private String diag;
            private final Expr diagExpr;
            private int diagLoc;

            public EvalResult()
            {
                diagExpr = null;
                diagLoc = Position.NOPOS;
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
    }

    //************* Primary expression ******************************//

    /**
     * [C99 6.5.1p2]
     * An reference to a declared variable name(in which case it is a lvalue) or
     * function name (in which case it is a function designator).
     *
     */
    public static class DeclRefExpr extends Expr
    {
        private String name;

        /**
         * The declaration that we are referencing.
         */
        private ValueDecl d;
        /**
         * The location of the declaration name itself.
         */
        private int location;

        public DeclRefExpr(String name, ValueDecl d,
                QualType ty,
                ExprValueKind valueKind,
                int loc)
        {
            super(DeclRefExprClass, ty, valueKind, loc);
            this.name = name;
            this.d = d;
            location = loc;
        }

        @Override
        public void accept(ASTVisitor v)
        {
            v.visitDeclRefExpr(this);
        }

        public ValueDecl getDecl()
        {
            return d;
        }

        @Override
        public int getLocation()
        {
            return location;
        }

        public void setLocation(int location)
        {
            this.location = location;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }
    }

    public static class IntegerLiteral extends Expr
    {
        public final APInt val;
        public IntegerLiteral(
                final APInt value,
                QualType type,
                int loc)
        {
            super(IntegerLiteralClass, type, EVK_RValue, loc);
            assert type.isIntegerType():"Illegal type in Integer literal.";
            assert value.getBitWidth() == type.getTypeSize()
                    :"Integer type is not the correct size for constant.";
            val = value;
        }

        public APInt getValue() { return val;}

        @Override
        public void accept(ASTVisitor v)
        {
            v.visitIntegerLiteral(this);
        }
    }

    public static class FloatLiteral extends Expr
    {
        private BigDecimal val;
        public FloatLiteral(QualType type,
                ExprValueKind valuekind,
                int loc)
        {
            super(FloatLiteralClass, type, valuekind, loc);
        }

        public FloatLiteral(int loc)
        {
            super(FloatLiteralClass, loc);
        }

        public BigDecimal getValue()
        {
            return val;
        }

        @Override
        public void accept(ASTVisitor v)
        {
            v.visitFloatLiteral(this);
        }
    }

    public static class CharacterLiteral extends Expr
    {
        private char val;
        public CharacterLiteral(
                char val,
                QualType type,
                ExprValueKind valuekind,
                int loc)
        {
            super(CharacterLiteralClass, type, valuekind, loc);
            this.val = val;
        }

        public CharacterLiteral(int loc)
        {
            super(CharacterLiteralClass, loc);
        }

        public char getValue()
        {
            return val;
        }

        @Override
        public void accept(ASTVisitor v)
        {
            v.visitCharacterLiteral(this);
        }
    }

    public static class StringLiteral extends Expr
    {
        public StringLiteral(
                QualType type,
                ExprValueKind valuekind,
                int loc)
        {
            super(CharacterLiteralClass, type, valuekind, loc);
        }

        public StringLiteral(int loc)
        {
            super(CharacterLiteralClass, loc);
        }

        @Override
        public void accept(ASTVisitor v)
        {
            v.visitStringLiteral(this);
        }
    }

    /**
     * A parenthesized subexpression, e.g. (1).
     * This AST node is only formed if full location information was requested.
     */
    public static class ParenExpr extends Expr
    {
        public Expr subExpr;
        public int lParenLoc, rParenLoc;

        public ParenExpr(Expr expr, int l, int r)
        {
            super(ParenExprClass, l);
            this.subExpr = expr;
            lParenLoc = l;
            rParenLoc = r;
        }

        public SourceRange getSourceRange()
        {
            return new SourceRange(lParenLoc, rParenLoc);
        }
        public void accept(ASTVisitor v)
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
        private int lParenLoc, rParenLoc;

        public ParenListExpr(
                int lParenLoc,
                ArrayList<Expr> exprs,
                int rParenLoc,
                QualType type)
        {
            super(ParenListExprClass, type, EVK_RValue, lParenLoc);
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

        public int getExprLoc()
        {
            return Position.NOPOS;
        }

        @Override
        public void accept(ASTVisitor v)
        {
            v.visitParenListExpr(this);
        }
    }

    //===============================================================//
    //                 Postfix operators                             //
    //===============================================================//

    /**
     * A type cast.
     */
    public static abstract class CastExpr extends Expr
    {
        protected Expr expr;
        protected CastKind castKind;

        public CastExpr(int tag, QualType ty,
                ExprValueKind valueKind,
                Expr expr,
                final CastKind castKind,
                int loc)
        {
            super(tag, ty, valueKind, loc);
            assert castKind != CK_Invalid :"creating cast with invalid cast kind.";
            this.expr = expr;
            this.castKind = castKind;
        }
        public CastKind getCastKind() { return castKind; }
        public Expr getSubExpr() { return expr;}
    }

    /**
     * Allows us to explicitly represent implicit type conversions, which have
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
                int loc)
        {
            super(ImplicitCastClass, ty, valueKind, expr, castKind, loc);
        }

        /**
         * ReturnStmt the expression after ignoring all of implicitly type cast operation.
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
        public void accept(ASTVisitor v)
        {
            v.visitImplicitCastExpr(this);
        }
    }

    /**
     * An explicit cast in C (C99 6.5.4).
     */
    public static final class ExplicitCastExpr extends CastExpr
    {
        private int lParenLoc;
        private int rParenLoc;

        public ExplicitCastExpr(QualType ty, ExprValueKind valueKind,
                Expr expr, CastKind castKind,
                int lParenLoc, int rParenLoc)
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
        public void accept(ASTVisitor v)
        {
            v.visitExplicitCastExpr(this);
        }
    }

    /**
     * An array selection [C99 6.5.2.1]
     */
    public static class ArraySubscriptExpr extends Expr
    {
        private Expr[] subExprs;
        private int rBracketLoc;

        public ArraySubscriptExpr(Expr indexed, Expr
                index,
                QualType t,
                ExprValueKind valueKind,
                int rBracketLoc)
        {
            super(ArraySubscriptExprClass, t, valueKind, rBracketLoc);
            subExprs = new Expr[2];
            subExprs[0] = indexed;
            subExprs[1] = index;
            this.rBracketLoc = rBracketLoc;
        }

        public ArraySubscriptExpr()
        {
            super(ArraySubscriptExprClass, Position.NOPOS);
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
            return new SourceRange(getLHS().getLocation(), rBracketLoc);
        }

        public int getRBracketLoc()
        {
            return rBracketLoc;
        }

        public int getExprLoc()
        {
            return getBase().getLocation();
        }

        @Override
        public void accept(ASTVisitor v)
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
		 * The name of callee method.
		 */
		public Expr fn;
		/**
		 * The formal parameters list.
		 */
		public ArrayList<Expr> args;

		public CallExpr(Expr fn,
                ArrayList<Expr> args,
                QualType resultType,
                ExprValueKind vk,
                int rparenLoc)
		{
			super(CallExprClass, resultType, vk, rparenLoc);
			this.fn = fn;
			this.args = args;
		}

		public CallExpr()
        {
            super(CallExprClass, Position.NOPOS);
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

            // If we are calling a dereference, look at the pointer instead.
            /**
            if (cee.tag == BinaryOperatorClass)
            {
                BinaryExpr be = (BinaryExpr)cee;
                // Not to deal with '->' or '.' operator, since it just works
                // in C++.
            }
             */
            if (cee.tag == UnaryOperatorClass)
            {
                UnaryExpr ue = (UnaryExpr)cee;
                if (ue.getOpCode() == UO_Deref)
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
            PointerType ty = calleeType.getPointerType();
            if ( ty != null)
            {
                calleeType = ty.getPointeeType();
            }

            final FunctionType fnType = calleeType.getFunctionType();
            return fnType.getReturnType();
        }
        @Override
		public void accept(ASTVisitor v)
		{
			v.visitCallExpr(this);
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
         * This is the decl being referenced by the field/member name.
         * In X.F, this is the decl referenced by F.
         */
        private ValueDecl memberDecl;

        /**
         * This is the location of the member name.
         */
        private int memberLoc;

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
                int loc)
        {
            super(MemberExprClass, type, valuekind, loc);
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

        public int getMemberLoc()
        {
            return memberLoc;
        }

        public void setMemberLoc(int memberLoc)
        {
            this.memberLoc = memberLoc;
        }

        public int getExprLoc() { return memberLoc;}

        @Override
        public void accept(ASTVisitor v)
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
        private int lParenLoc;

        /**
         * This can be an incomplete array type, in
         * which case the actual expression type will be different.
         */
        private QualType ty;
        private Expr init;
        private boolean isFileScope;


        public CompoundLiteralExpr(int lParenLoc,
                QualType type,
                ExprValueKind valuekind,
                Expr init,
                boolean fileScope)
        {
            super(CompoundLiteralExprClass, type, valuekind, lParenLoc);
            this.lParenLoc = lParenLoc;
            ty = type;
            isFileScope = fileScope;
        }

        public CompoundLiteralExpr()
        {
            super(CompoundLiteralExprClass, Position.NOPOS);
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

        public int getLParenLoc()
        {
            return lParenLoc;
        }

        public void setLParenLoc(int lParenLoc)
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
        public void accept(ASTVisitor v)
        {
            v.visitCompoundLiteralExpr(this);
        }
    }

	/**
	 * This represents the unary-expression (except sizeof and alignof).
	 */
	public static final class UnaryExpr extends Expr
	{
		private Expr subExpr;
		private UnaryOperatorKind opcode;
        private ExprValueKind evk;

		public UnaryExpr(Expr subExpr,
                UnaryOperatorKind opcode,
                QualType type,
                ExprValueKind evk,
                int loc)
		{
			super(UnaryOperatorClass, loc);
			this.subExpr = subExpr;
            this.opcode = opcode;
            this.evk = evk;
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

		public void accept(ASTVisitor v)
		{
			v.visitUnaryExpr(this);
		}
	}

	public static final class UnaryExprOrTypeTraitExpr extends Expr
    {
        /**
         * true if operand is a type, false if it is an expression.
         */
        private boolean isType;
        private UnaryExprOrTypeTrait kind;
        private QualType ty;
        private Expr ex;
        private int opLoc, rParenLoc;

        public UnaryExprOrTypeTraitExpr(UnaryExprOrTypeTrait kind,
                QualType resultType,
                int opLoc, int rp)
        {
            super(UnaryExprOrTypeTraitClass, resultType, EVK_RValue, opLoc);
            this.kind = kind;
            isType = true;
            this.opLoc = opLoc;
            this.rParenLoc = rp;
            ty = resultType;
        }

        public UnaryExprOrTypeTraitExpr(UnaryExprOrTypeTrait kind,
                Expr e,
                QualType resultType,
                int opLoc, int rp)
        {
            super(UnaryExprOrTypeTraitClass, resultType, EVK_RValue, opLoc);
            this.kind = kind;
            isType = true;
            this.opLoc = opLoc;
            this.rParenLoc = rp;
            ex = e;
        }

        @Override
        public void accept(ASTVisitor v)
        {
            v.visitUnaryExprOrTypeTraitExpr(this);
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
        private int oploc;

		public BinaryExpr(Expr lhs,
                Expr rhs,
                BinaryOperatorKind op,
                ExprValueKind vk,
                QualType resultTy,
                int oploc)
		{
			super(BinaryOperatorClass, resultTy, vk, oploc);
			this.lhs = lhs;
			this.rhs = rhs;
			opcode = op;
            this.oploc = oploc;

            assert !isCompoundAssignmentOp()
                    : "Use ArithAssignBinaryOperator for compound assignments";
        }

        public BinaryExpr(int kind)
        {
            super(kind, Position.NOPOS);
        }

        @Override
		public void accept(ASTVisitor v)
		{
			v.visitBinaryExpr(this);
		}

		public int getOperatorLoc() { return oploc; }

		public void setOperatorLoc(int loc) { oploc = loc;}

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
    }

    /**
     * For compound assignments (e.g. +=), we keep
     * track of the type the operation is performed in.  Due to the semantics of
     * these operators, the operands are promoted, the arithmetic performed, an
     * implicit conversion back to the result type done, then the assignment takes
     * place.  This captures the intermediate type which the computation is done
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
                int oploc)
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
         * The two computation types are the type the LHS is converted
         * to for the computation and the type of the result; the two are
         * distinct in a few cases (specifically, int+=ptr and ptr-=ptr).
         * @return
         */
        public QualType getComputationLHSType() { return computationLHSType; }
        public void setComputationLHSType(QualType T) { computationLHSType = T; }

        public QualType getComputationResultType() { return computationResultType; }
        public void setComputationResultType(QualType T) { computationResultType = T; }

        @Override
        public void accept(ASTVisitor v)
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
        private int qLoc, cLoc;
        public ConditionalExpr(Expr cond, int qLoc,
                Expr lhs,
                int cLoc,
                Expr rhs,
                QualType t,
                ExprValueKind vk)
        {
            super(CondtionalOperatorClass, t, vk, cond.getLocation());
            this.cond = cond;
            truepart = lhs;
            falsepart = rhs;
            this.qLoc = qLoc;
            this.cLoc = cLoc;
        }

        public void accept(ASTVisitor v)
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
     * placeholder type "void". This initializer list is called the
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
        private int lBraceLoc, rBraceLoc;
        private ArrayList<Expr> initExprs;

        public InitListExpr(int lBraceLoc, int rBraceLoc, ArrayList<Expr> initList)
        {
            super(InitListExprClass, lBraceLoc);
            this.lBraceLoc = lBraceLoc;
            this.rBraceLoc = rBraceLoc;
        }
        @Override
        public void accept(ASTVisitor v)
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
    }

	public static class ErroneousTree extends Tree
	{

		public ErroneousTree()
		{
			super(ErroneousStmtClass);
		}

		public void accept(ASTVisitor v)
		{
			v.visitErroneous(this);
		}
	}
}
