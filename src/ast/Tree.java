package ast;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import symbol.Scope;
import symbol.Symbol;
import symbol.Symbol.OperatorSymbol;
import symbol.Symbol.MethodSymbol;
import symbol.Symbol.VarSymbol;
import type.Type;
import utils.Name;
import utils.Position;

/**
 * Root class for abstract syntax tree nodes. It provides definitions for
 * specific tree nodes as subclasses. There are 40 such subclasses.
 * 
 * Each subclass is highly standardized. It generally contains only tree fields
 * for the syntactic subcomponents of the node. Some classes that represents
 * identifier uses or definitions also define a Symbol field that denotes the
 * represented identifier. Classes for non-local jumps also carry the jump
 * target as a field. The root class {@link Tree} itself defines fields for the
 * tree's type and position. No other fields are kept in a tree node; instead
 * parameters are passed to methods accessing the node.
 * 
 * The only method defined in subclasses is 'visit' which applies a given
 * visitor to the tree. The actual tree processing is done by visitor classes in
 * other package. The abstract class {@link ASTVisitor} is the abstract root
 * class for visiting different tree.
 * 
 * @author Jianping Zeng <z1215jping@hotmail.com>
 * @version 1.0
 *
 */
abstract public class Tree
{

	/*
	 * TopLevel nodes, of type TopLevel, representing entire source files.
	 */
	public static final int TOPLEVEL = 1;

	public static final int IMPORT = TOPLEVEL + 1;

	public static final int SELECT = IMPORT + 1;

	/**
	 * Method definitions, of type MethodDef.
	 */
	public static final int METHODDEF = SELECT + 1;

	/**
	 * Variable definitions, of type VarDef.
	 */
	public static final int VARDEF = METHODDEF + 1;

	/**
	 * The no-op statement ";", of type Skip.
	 */
	public static final int SKIP = VARDEF + 1;

	/**
	 * Blocks, of type Block.
	 */
	public static final int BLOCK = SKIP + 1;

	/**
	 * Do-while loop statement, of type Doloop.
	 */
	public static final int DOLOOP = BLOCK + 1;

	/**
	 * While loop statement, of type Whileloop.
	 */
	public static final int WHILELOOP = DOLOOP + 1;

	/**
	 * For-loop, of type Forloop.
	 */
	public static final int FORLOOP = WHILELOOP + 1;

	/**
	 * Labelled statement, of type Labelled.
	 */
	public static final int LABELLED = FORLOOP + 1;

	/**
	 * Switch statement, of type Switch.
	 */
	public static final int SWITCH = LABELLED + 1;

	/**
	 * Case portions in switch statement, of type Case.
	 */
	public static final int CASE = SWITCH + 1;

	/**
	 * Conditional expression, of type Conditional.
	 */
	public static final int CONDEXPR = CASE + 1;

	/**
	 * If statements, of type If.
	 */
	public static final int IF = CONDEXPR + 1;

	/**
	 * Expression statements, of type Exec.
	 */
	public static final int EXEC = IF + 1;

	/**
	 * Break statements, of type Break.
	 */
	public static final int BREAK = EXEC + 1;

	public static final int GOTO = BREAK + 1;

	/**
	 * Continue statements, of type Continue.
	 */
	public static final int CONTINUE = BREAK + 1;

	/**
	 * Return statements, of type Return.
	 */
	public static final int RETURN = CONTINUE + 1;

	/**
	 * Method invocation expressions, of type Apply.
	 */
	public static final int APPLY = RETURN + 1;

	/**
	 * Parenthesized subexpressions of type Parens.
	 */
	public static final int PARENS = APPLY + 1;

	/**
	 * Assignment expressions, of type Assign.
	 */
	public static final int ASSIGN = PARENS + 1;

	/**
	 * Type cast expressions, of type Typecast.
	 */
	public static final int TYPECAST = ASSIGN + 1;

	/**
	 * Indexed array expression, of type Indexed.
	 */
	public static final int INDEXED = TYPECAST + 1;

	/**
	 * Simple identifiers, of type Ident.
	 */
	public static final int IDENT = INDEXED + 1;

	/**
	 * Literals, of type Literal.
	 */
	public static final int LITERAL = IDENT + 1;

	/**
	 * Basic type identifiers, of type TypeIdent.
	 */
	public static final int TYPEIDENT = LITERAL + 1;

	/**
	 * Array types ,of type TypeArray.
	 */
	public static final int ARRAYTYPE = TYPEIDENT + 1;

	public static final int NEWARRAY = ARRAYTYPE + 1;

	/**
	 * Error Trees, of type Erroneous.
	 */
	public static final int ERRONEOUS = ARRAYTYPE + 1;

	/**
	 * Unary operators, of type Unary.
	 */
	public static final int POS = ERRONEOUS + 1;
	public static final int NEG = POS + 1;
	public static final int NOT = NEG + 1;
	/** 按位取反 */
	public static final int COMPL = NOT + 1;
	public static final int PREINC = COMPL + 1;
	public static final int PREDEC = PREINC + 1;
	public static final int POSTINC = PREDEC + 1;
	public static final int POSTDEC = POSTINC + 1;

	/**
	 * Binary operators, of type Binary.
	 */
	public static final int OR = POSTDEC + 1;
	public static final int AND = OR + 1;
	public static final int BITOR = AND + 1;
	public static final int BITXOR = BITOR + 1;
	public static final int BITAND = BITXOR + 1;
	public static final int EQ = BITAND + 1;
	public static final int NE = EQ + 1;
	public static final int LT = NE + 1;
	public static final int LE = LT + 1;
	public static final int GT = LE + 1;
	public static final int GE = GT + 1;
	public static final int SL = GE + 1;
	public static final int SR = SL + 1;
	public static final int PLUS = SR + 1;
	public static final int MINUS = PLUS + 1;
	public static final int MUL = MINUS + 1;
	public static final int DIV = MUL + 1;
	public static final int MOD = DIV + 1;
	
	/**
	 * Assignment operators, of type Assignop.
	 */
	public static final int BITOR_ASG = MOD + 1;
	public static final int BITXOR_ASG = BITOR_ASG + 1;
	public static final int BITAND_ASG = BITXOR_ASG + 1;
	public static final int SL_ASG = BITAND_ASG + 1;
	public static final int SR_ASG = SL_ASG + 1;
	public static final int PLUS_ASG = SR_ASG + 1;
	public static final int MINUS_ASG = PLUS_ASG + 1;
	public static final int MUL_ASG = MINUS_ASG + 1;
	public static final int DIV_ASG = MUL_ASG + 1;
	public static final int MOD_ASG = DIV_ASG + 1;

	/**
	 * The offset between assignment operators and normal binary operators.
	 */
	public static final int ASGOffset = BITOR_ASG - BITOR;

	public static List<Tree> emptyList = new LinkedList<>();

	/**
	 * The encoded position of current tree.
	 */
	public int pos;

	/**
	 * The type of this tree that is ASTs type.
	 */
	public Type type;

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

	/**
	 * Set the type of this tree and return this tree.
	 * 
	 * @param type
	 * @return
	 */
	public Tree setType(Type type)
	{
		this.type = type;
		return this;
	}

	/**
	 * Visit this tree with a given visitor.
	 */
	public void accept(ASTVisitor v)
	{
		v.visitTree(this);
	}

	/**
	 * Everything in one source file is kept in a TopLevel structure.
	 * 
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 * @version 2016年1月9日 下午4:05:29
	 */
	public static class TopLevel extends Tree
	{
		public List<Tree> defs;
		public Name sourceFile;
		public Scope topScope = null;
		public HashMap<Object, Object> endPositions = null;

		/**
		 * Constructs TopLevel tree node that represents a source file.
		 * 
		 * @param defs all of definitions in a source file.
		 * @param sourceFile the name of source file.
		 * @param topScope the corresponding scope of this source file.
		 */
		public TopLevel(List<Tree> defs, Name sourceFile, Scope topScope)
		{
			super(TOPLEVEL);		
			this.defs = defs;
			this.sourceFile = sourceFile;
			this.topScope = topScope;
		}

		public void accept(ASTVisitor v)
		{
			v.visitTopLevel(this);
		}
	}

	/**
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 * @version 2016年1月9日 下午4:29:09
	 */
	public static class Import extends Tree
	{
		public Tree qualid;

		public Import(Tree qualid)
		{
			super(IMPORT);
			this.qualid = qualid;
		}

		public void accept(ASTVisitor v)
		{
			v.visitImport(this);
		}
	}

	/**
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 * @version 2016年1月9日 下午4:40:56
	 */
	public static class Skip extends Tree
	{

		public Skip()
		{
			super(SKIP);
		}

		public void accept(ASTVisitor v)
		{
			v.visitSkip(this);
		}
	}

	/**
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 * @version 2016年1月9日 下午4:32:56
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
			super(METHODDEF);
			this.flags = flags;
			this.name = name;
			this.rettype = rettype;
			this.params = params;
			this.body = body;
			this.sym = sym;
		}

		public void accept(ASTVisitor v)
		{
			v.visitMethodDef(this);
		}
	}

	/**
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 * @version 2016年1月9日 下午4:38:11
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

		public VarDef(long flags, Name name, Tree varType, Tree init, VarSymbol sym)
		{
			super(VARDEF);
			this.flags = flags;
			this.name = name;
			this.varType = varType;
			this.init = init;
			this.sym = sym;
		}

		public void accept(ASTVisitor v)
		{
			v.visitVarDef(this);
		}
	}

	/**
	 * A statement block.
	 * 
	 * @param stats statements
	 * @param flags modifier
	 */
	public static class Block extends Tree
	{

		public List<Tree> stats;
		public int endpos = Position.NOPOS;

		public Block(List<Tree> stats)
		{
			super(BLOCK);
			this.stats = stats;
		}

		public void accept(ASTVisitor v)
		{
			v.visitBlock(this);
		}
	}

	
	/**
	 * Selects through packages or class for future.
	 * 
	 * @param selected selected Tree hierarchie
	 * @param selector name of field to select thru
	 * @param sym symbol of the selected class
	 */	
	public static class Select extends Tree
	{
		public Tree selected;
		public Name name;
		public Symbol sym;

		public Select(Tree selected, Name name, Symbol sym)
		{
			super(SELECT);
			this.selected = selected;
			this.name = name;
			this.sym = sym;
		}

		public void accept(ASTVisitor v)
		{
			v.visitSelect(this);
		}
	}
	/**
	 * A ( ) ? ( ) : ( ) conditional expression
	 */
	public static class Conditional extends Tree
	{
		public Tree cond;
		public Tree truepart;
		public Tree falsepart;

		public Conditional(Tree cond, Tree truepart, Tree falsepart)
		{
			super(CONDEXPR);
			this.cond = cond;
			this.truepart = truepart;
			this.falsepart = falsepart;
		}

		public void accept(ASTVisitor v)
		{
			v.visitConditional(this);
		}
	}

	/**
	 * A do loop
	 */
	public static class DoLoop extends Tree
	{
		public Tree body;
		public Tree cond;

		public DoLoop(Tree body, Tree cond)
		{
			super(DOLOOP);
			this.body = body;
			this.cond = cond;
		}

		public void accept(ASTVisitor v)
		{
			v.visitDoLoop(this);
		}
	}

	/**
	 * A for loop.
	 */
	public static class ForLoop extends Tree
	{
		public List<Tree> init;
		public Tree cond;
		public List<Tree> step;
		public Tree body;

		public ForLoop(List<Tree> init, Tree cond, List<Tree> step, Tree body)
		{
			super(FORLOOP);
			this.init = init;
			this.cond = cond;
			this.step = step;
			this.body = body;
		}

		public void accept(ASTVisitor v)
		{
			v.visitForLoop(this);
		}
	}

	/**
	 * A while loop
	 */
	public static class WhileLoop extends Tree
	{
		public Tree cond;
		public Tree body;

		public WhileLoop(Tree cond, Tree body)
		{
			super(WHILELOOP);
			this.cond = cond;
			this.body = body;
		}

		public void accept(ASTVisitor v)
		{
			v.visitWhileLoop(this);
		}
	}

	/**
	 * A labelled expression or statement.
	 */
	public static class Labelled extends Tree
	{
		public Name label;
		public Tree body;
		/** The corresponding basic block of this label.*/
		public hir.BasicBlock corrBB;

		public Labelled(Name label, Tree body)
		{
			super(LABELLED);
			this.label = label;
			this.body = body;
		}

		public void accept(ASTVisitor v)
		{
			v.visitLabelled(this);
		}
	}

	/**
	 * An "if ( ) { } else { }" block
	 */
	public static class If extends Tree
	{
		public Tree cond;
		public Tree thenpart;
		public Tree elsepart;

		public If(Tree cond, Tree thenpart, Tree elsepart)
		{
			super(IF);
			this.cond = cond;
			this.thenpart = thenpart;
			this.elsepart = elsepart;
		}

		public void accept(ASTVisitor v)
		{
			v.visitIf(this);
		}
	}

	/**
	 * an expression statement
	 * 
	 * @param expr expression structure
	 */
	public static class Exec extends Tree
	{
		public Tree expr;

		public Exec(Tree expr)
		{
			super(EXEC);
			this.expr = expr;
		}

		public void accept(ASTVisitor v)
		{
			v.visitExec(this);
		}
	}

	/**
	 * A "switch ( ) { }" construction.
	 */
	public static class Switch extends Tree
	{
		public Tree selector;
		public List<Tree> cases;

		public Switch(Tree selector, List<Tree> cases)
		{
			super(SWITCH);
			this.selector = selector;
			this.cases = cases;
		}

		public void accept(ASTVisitor v)
		{
			v.visitSwitch(this);
		}
	}

	/**
	 * A "case  :" of a switch.
	 */
	public static class Case extends Tree
	{
		public Tree pat;
		public List<Tree> stats;

		public Case(Tree pat, List<Tree> stats)
		{
			super(CASE);
			this.pat = pat;
			this.stats = stats;
		}

		public void accept(ASTVisitor v)
		{
			v.visitCase(this);
		}
	}

	/**
	 * A break from a loop or switch.
	 */
	public static class Break extends Tree
	{
		public Tree target;

		public Break(Tree target)
		{
			super(BREAK);
			this.target = target;
		}

		public void accept(ASTVisitor v)
		{
			v.visitBreak(this);
		}
	}

	public static class Goto extends Tree
	{
		public Name label;
		public Labelled target;

		public Goto(Name label, Labelled target)
		{
			super(GOTO);
			this.label = label;
			this.target = target;
		}
		public void accept(ASTVisitor v)
		{
			v.visitGoto(this);
		}
	}

	/**
	 * A continue of a loop.
	 */
	public static class Continue extends Tree
	{
		public Tree target;

		public Continue(Tree target)
		{
			super(CONTINUE);
			this.target = target;
		}

		public void accept(ASTVisitor v)
		{
			v.visitContinue(this);
		}
	}

	/**
	 * A return statement.
	 */
	public static class Return extends Tree
	{
		public Tree expr;

		public Return(Tree expr)
		{
			super(RETURN);
			this.expr = expr;
		}

		public void accept(ASTVisitor v)
		{
			v.visitReturn(this);
		}
	}

	/**
	 * A method invocation
	 */
	public static class Apply extends Tree
	{
		/**
		 * The name of callee method.
		 */
		public Tree meth;
		/**
		 * The formal parameters list.
		 */
		public List<Tree> args;

		public Apply(Tree meth, List<Tree> args)
		{
			super(APPLY);
			this.meth = meth;
			this.args = args;
		}

		public void accept(ASTVisitor v)
		{
			v.visitApply(this);
		}
	}

	/**
	 * A parenthesized subexpression ( ... )
	 */
	public static class Parens extends Tree
	{
		public Tree expr;

		public Parens(Tree expr)
		{
			super(PARENS);
			this.expr = expr;
		}

		public void accept(ASTVisitor v)
		{
			v.visitParens(this);
		}
	}

	/**
	 * A assignment with "=".
	 */
	public static class Assign extends Tree
	{
		public Tree lhs;
		public Tree rhs;

		public Assign(Tree lhs, Tree rhs)
		{
			super(ASSIGN);
			this.lhs = lhs;
			this.rhs = rhs;
		}

		public void accept(ASTVisitor v)
		{
			v.visitAssign(this);
		}
	}

	/**
	 * An assignment with "+=", "|=" ...
	 */
	public static class Assignop extends Tree
	{
		public Tree lhs;
		public Tree rhs;
		public OperatorSymbol operator;

		public Assignop(int opcode, Tree lhs, Tree rhs, OperatorSymbol operator)
		{
			super(opcode);
			this.lhs = lhs;
			this.rhs = rhs;
			this.operator = operator;
		}

		public void accept(ASTVisitor v)
		{
			v.visitAssignop(this);
		}
	}

	/**
	 * A unary operation.
	 */
	public static class Unary extends Tree
	{
		public Tree arg;
		public OperatorSymbol operator;

		public Unary(int opcode, Tree arg, OperatorSymbol operator)
		{
			super(opcode);
			this.arg = arg;
			this.operator = operator;
		}

		public void accept(ASTVisitor v)
		{
			v.visitUnary(this);
		}
	}

	/**
	 * A binary operation.
	 */
	public static class Binary extends Tree
	{
		public Tree lhs;
		public Tree rhs;
		public symbol.Symbol.OperatorSymbol operator;

		public Binary(int opcode, Tree lhs, Tree rhs, OperatorSymbol operator)
		{
			super(opcode);
			this.lhs = lhs;
			this.rhs = rhs;
			this.operator = operator;
		}

		public void accept(ASTVisitor v)
		{
			v.visitBinary(this);
		}
	}

	/**
	 * A type cast.
	 */
	public static class TypeCast extends Tree
	{
		public Tree clazz;
		public Tree expr;

		public TypeCast(Tree clazz, Tree expr)
		{
			super(TYPECAST);
			this.clazz = clazz;
			this.expr = expr;
		}

		public void accept(ASTVisitor v)
		{
			v.visitTypeCast(this);
		}
	}

	/**
	 * An array selection
	 */
	public static class Indexed extends Tree
	{
		public Tree indexed;
		public Tree index;

		public Indexed(Tree indexed, Tree index)
		{
			super(INDEXED);
			this.indexed = indexed;
			this.index = index;
		}

		public void accept(ASTVisitor v)
		{
			v.visitIndexed(this);
		}
	}

	/**
	 * An identifier.
	 * 
	 * @param idname the name
	 * @param sym the symbol
	 */
	public static class Ident extends Tree
	{
		public Name name;
		public Symbol sym;

		public Ident(Name name, Symbol sym)
		{
			super(IDENT);
			this.name = name;
			this.sym = sym;
		}

		public void accept(ASTVisitor v)
		{
			v.visitIdent(this);
		}
	}

	/**
	 * A constant value given literally.
	 * 
	 * @param value value representation
	 */
	public static class Literal extends Tree
	{
		public int typetag;
		public Object value;

		public Literal(int typetag, Object value)
		{
			super(LITERAL);
			this.typetag = typetag;
			this.value = value;
		}

		public void accept(ASTVisitor v)
		{
			v.visitLiteral(this);
		}
	}

	/**
	 * Identifies a basic type.
	 */
	public static class TypeIdent extends Tree
	{
		public int typetag;

		/**
		 * Constructs a new Type identifier with given {@code TypeTags} typetag.
		 * @param typetag
		 */
		public TypeIdent(int typetag)
		{
			super(TYPEIDENT);
			this.typetag = typetag;
		}

		public void accept(ASTVisitor v)
		{
			v.visitTypeIdent(this);
		}
	}

	/**
	 * An array type, A[]
	 */
	public static class TypeArray extends Tree
	{
		public Tree elemtype;
		public int dimensions;

		public TypeArray(Tree elemtype)
		{
			super(ARRAYTYPE);
			this.elemtype = elemtype;
		}

		public void accept(ASTVisitor v)
		{
			v.visitTypeArray(this);
		}
	}

	/**
	 * A new[...] operation.
	 */
	public static class NewArray extends Tree
	{
		public Tree elemtype;
		public List<Tree> dims;
		public List<Tree> elems;

		public NewArray(Tree elemtype, List<Tree> dims, List<Tree> elems)
		{
			super(NEWARRAY);
			this.elemtype = elemtype;
			this.dims = dims;
			this.elems = elems;
		}

		public void accept(ASTVisitor v)
		{
			v.visitNewArray(this);
		}
	}

	public static class Erroneous extends Tree
	{

		public Erroneous()
		{
			super(ERRONEOUS);
		}

		public void accept(ASTVisitor v)
		{
			v.visitErroneous(this);
		}
	}

}
