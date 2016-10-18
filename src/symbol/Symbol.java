package symbol;

import type.Type;
import type.TypeTags;
import utils.Name;

/**
 * This a internal root class that represents all of symbols in this c-flat
 * language. It isDeclScope subclasses for specific sorts of symbols, such as
 * variables, methods and operators and types. Each subclass is represented
 * as a static subLoops class inside Symbol.
 *
 * @author Xlous.zeng  
 * @version 2016年1月8日 上午10:48:39
 */
public class Symbol implements SymbolKinds, TypeTags
{

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
	 * @param kind The kind of this symbol
	 * @param name The instance of {@link Name} represents name of this.
	 * @param type The type of this symbol that is instance of {@link Type}
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
	public Symbol clone(Symbol newOwner)
	{
		throw new AssertionError();
	}

	/**
	 * A description of this symbol; overrides Object.
	 */
	public String toString()
	{
		return name.toString();
	}

	/**
	 * A description of the location of this symbol; used for error reporting.
	 */
	public String location()
	{
		if (name == null || name.len == 0)
			return "";
		else
			return " in " + name.toString();
	}

	/**
	 * A class for method symbols.
	 */
	public static class MethodSymbol extends Symbol
	{

		/**
		 * The flags, such as storage class or qualifier,
		 * which restricted by {@link TypeTags}.
		 */
		public long flags;

		/**
		 * Construct a method symbol, given its flags, name, type and owner.
		 */
		public MethodSymbol(long flags, Name name, Type type)
		{
			super(MTH, name, type);
			this.flags = flags;
		}

		/**
		 * Clone this symbol.
		 */
		public Symbol clone(Symbol newOwner)
		{
			MethodSymbol m = new MethodSymbol(flags, name, type);
			return m;
		}

		public String toString()
		{

			String s;
			s = "method " + name;
			if (type != null)
			{
				s += "(" + type.paramTypes().toString() + ")";
			}
			return s;
		}
	}

	/**
	 * A class for predefined operators.
	 */
	public static class OperatorSymbol extends MethodSymbol
	{

		/**
		 * The operator code represented by different semantic.
		 */
		public int opcode;

		public OperatorSymbol(long flags, Name name, Type type, int opcode)
		{
			super(flags, name, type);
			this.opcode = opcode;
		}
	}

	public static class TopLevelSymbol extends TypeSymbol
	{
		/**
		 * The top level scope corresponding to global compilation unit.
		 */
		public Scope topScope;

		/**
		 * The name of compiled source file.
		 */
		public Name sourcefile;

		public TopLevelSymbol(Name name)
		{
			super(name, null);
			this.topScope = null;
			this.sourcefile = null;
		}

	}

	public static class ErrorSymbol extends TypeSymbol
	{

		public ErrorSymbol(Name name)
		{
			super(name, null);
		}

	}
}
