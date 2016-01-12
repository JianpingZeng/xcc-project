package symbol;

import type.Type;
import type.TypeTags;
import utils.Name;
import utils.Position;

/**
 * This a internal root class that represents all of symbols in this c-flat
 * language. It contains subclasses for specific sorts of symbols, such as
 * variables, methods and operators and types. Each subclass is represented as a
 * static inner class inside Symbol.
 * 
 * @author Jianping Zeng <z1215jping@hotmail.com>
 * @version 2016年1月8日 上午10:48:39
 */
public class Symbol implements SymbolKinds, TypeTags {

	/**
	 * The kind of this symbol.
	 * 
	 * @see SymbolKinds
	 */
	public int kind;

	/**
	 * The name of this symbol in uft8 representation.
	 */
	public Name name;

	/**
	 * The type of this symbol.
	 */
	public Type type;

	/**
	 * The constructor that constructs a new symbol with given kind, name, type
	 * and owner.
	 * 
	 * @param kind
	 * @param name
	 * @param type
	 * @param owner
	 */
	public Symbol(int kind, Name name, Type type)
	{
		super();
		this.kind = kind;
		this.type = type;	
		this.name = name;
	}

	/**
	 * Clone this symbol with new owner. Legal only for fields and methods.
	 */
	public Symbol clone(Symbol newOwner) {
		throw new AssertionError();
	}

	/**
	 * A description of this symbol; overrides Object.
	 */
	public String toString() {
		return name.toString();
	}

	/**
	 * A description of the location of this symbol; used for error reporting.
	 *
	 * XXX 06/09/99 iris This method appears to be redundant and should probably
	 * be unified with javaLocation();
	 */
	public String location() {
		if (name == null || name.len == 0)
			return "";
		else
			return " in " + name.toString();
	}

	/**
	 * A class for type symbols. Type variables are represented by instances of
	 * this class, classes and packages by instances of subclasses.
	 */
	public static class TypeSymbol extends Symbol {

		public TypeSymbol(Name name, Type type) {
			super(TYP, name, type);
		}

		public String toString() {
			return "type variable " + name;
		}

	}

	public static class CompositeTypeSymbol extends TypeSymbol 
	{
		public Scope members_field;
		public Name fullname;
		
		public CompositeTypeSymbol(Name name, Type type)
		{
			super(name, type);
			this.kind = COMPOSITE;
			this.members_field = null;
			this.fullname = name;
			
		}
	}
	
	/**
	 * A class for variable symbols
	 */
	public static class VarSymbol extends Symbol {

		public long flags;
		/**
		 * The variable's declaration position.
		 */
		public int pos = Position.NOPOS;

		/**
		 * The variable's address. Used for different purposes during flow
		 * analysis, translation and code generation. Flow analysis: If this is
		 * a blank final or local variable, its sequence number. Translation: If
		 * this is a private field, its access number. Code generation: If this
		 * is a local variable, its logical slot number.
		 */
		public int adr = -1;

		/**
		 * The variable's constant value, if this is a constant. Before the
		 * constant value is evaluated, it points to an initalizer environment.
		 */
		public Object constValue;

		/**
		 * Construct a variable symbol, given its flags, name, type and owner.
		 */
		public VarSymbol(long flags, Name name, Type type) 
		{
			super(VAR, name, type);
			this.flags = flags;
		}

		/**
		 * Clone this symbol.
		 */
		public Symbol clone() {
			VarSymbol v = new VarSymbol(flags, name, type);
			v.pos = pos;
			v.adr = adr;
			v.constValue = constValue;
			return v;
		}

		public String toString() {
			return "variable " + name;
		}

	}

	/**
	 * A class for method symbols.
	 */
	public static class MethodSymbol extends Symbol {

		public long flags;
		/**
		 * Construct a method symbol, given its flags, name, type and owner.
		 */
		public MethodSymbol(long flags, Name name, Type type) {
			super(MTH, name, type);
			this.flags = flags;
		}

		/**
		 * Clone this symbol.
		 */
		public Symbol clone(Symbol newOwner) {
			MethodSymbol m = new MethodSymbol(flags, name, type);
			return m;
		}

		public String toString() {

			String s;
			s = "method " + name;
			if (type != null) {
				s += "(" + type.paramTypes().toString() + ")";
			}
			return s;

		}

	}
	

	/**
	 * A class for predefined operators.
	 */
	public static class OperatorSymbol extends MethodSymbol {
		public int opcode;

		public OperatorSymbol(long flags, Name name, Type type, int opcode) 
		{
			super(flags, name, type);
			this.opcode = opcode;
		}
	}
}
