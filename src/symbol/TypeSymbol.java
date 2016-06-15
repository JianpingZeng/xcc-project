package symbol;

import type.Type;
import utils.Name;

/**
 * A class for type symbols. Type symbol are represented by instances of
 * this class, classes and packages by instances of subclasses in the future.
 *
 * @author Xlous.zeng
 */
public class TypeSymbol extends Symbol
{

	public TypeSymbol(Name name, Type type)
	{
		super(TYP, name, type);
	}

	public String toString()
	{
		return "type symbol " + name;
	}

	/**
	 * A subclass of superclass TypeSymbol for specified sorts of CompositeType.
	 * In the future, there all two different subclass inherited from this.
	 */
	public static class CompositeTypeSymbol extends TypeSymbol
	{
		/**
		 * The scope of members of this Composite Type.
		 */
		public Scope members_field;

		/**
		 * Full name.
		 */
		public Name fullname;

		public CompositeTypeSymbol(Name name, Type type)
		{
			super(name, type);
			this.kind = COMPOSITE;
			this.members_field = null;
			this.fullname = name;

		}
	}
}