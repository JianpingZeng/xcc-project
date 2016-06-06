package type;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import symbol.TypeSymbol;
import utils.Name;

/**
 * The abstract root class of various type. It provides different definitions
 * for it's concrete subclass.
 * 
 * @author Jianping Zeng <z1215jping@hotmail.com>
 * @version 1.0
 */
public class Type implements TypeTags
{
	/**
	 * Constant type: no type at all.
	 */
	public static final Type noType = new Type(NONE, null);
	public static final Type CHARType = new Type(TypeTags.CHAR, null);
	public static final Type BYTEType = new Type(TypeTags.BYTE, null);
	public static final Type SHORTType = new Type(TypeTags.SHORT, null);
	public static final Type INTType = new Type(TypeTags.INT, null);
	public static final Type LONGType = new Type(TypeTags.LONG, null);
	public static final Type FLOATType = new Type(TypeTags.FLOAT, null);
	public static final Type DOUBLEType = new Type(TypeTags.DOUBLE, null);
	public static final Type VOIDType = new Type(TypeTags.VOID, null);

	public int tag;

	public TypeSymbol tsym;

	/**
	 * The constant of this type, null if this type does not have a constant
	 * value attribute. Constant VALUES can be set only for base type(numbers,
	 * boolean, string).
	 */
	public Object constValue = null;

	public Type(int tag, TypeSymbol tsym)
	{
		super();
		this.tag = tag;
		this.tsym = tsym;
	}

	/**
	 * Define a constant type with same kind of this type and given constant
	 * value.
	 * 
	 * @param constValue
	 * @return
	 */
	public Type constType(Object constValue)
	{
		assert tag <= BOOL;
		Type t = new Type(tag, tsym);
		t.constValue = constValue;
		return t;
	}

	/**
	 * If this is a constant type, returns its realistic type. Otherwise, return
	 * the type itself.
	 * 
	 * @return
	 */
	public Type basetype()
	{
		if (constValue == null)
			return this;
		else
			return tsym.type;
	}

	/**
	 * Converts to string.
	 */
	public String toString()
	{
		String s = (tsym == null || tsym.name == null) ? "null" : tsym.name
		        .toString();
		return s;
	}

	/**
	 * The constant value of this type, converted to string. Note that: Type has
	 * a non-null constValue field.
	 * 
	 * @return
	 */
	public String stringValue()
	{
		if (tag == BOOL)
			return ((Integer) constValue).intValue() == 0 ? "false" : "true";
		else if (tag == CHAR)
			return String.valueOf((char) ((Integer) constValue).intValue());
		else
			return constValue.toString();
	}

	/**
	 * Is this constant type whose value is false ?
	 * 
	 * @return
	 */
	public boolean isFalse()
	{
		return tag == BOOL && constValue != null
		        && ((Integer) constValue).intValue() == 0;
	}

	/**
	 * Is this constant type whose value is true ?
	 * 
	 * @return
	 */
	public boolean isTrue()
	{
		return tag == BOOL && constValue != null
		        && ((Integer) constValue).intValue() != 0;
	}

	/**
	 * This method just for array type.
	 * 
	 * @return
	 */
	public Type elemType()
	{
		return null;
	}

	public Type returnType()
	{
		return this;
	}

	/**
	 * An empty list of types.
	 */
	public static final List<Type> emptyList = new LinkedList<>();

	public List<Type> paramTypes()
	{
		return emptyList;
	}

	/**
	 * This method just for array type.
	 * 
	 * @return
	 */
	public int dimensions()
	{
		return 0;
	}

	public boolean isErronuous()
	{
		return false;
	}

	public static boolean isErroneous(List<Type> ts)
	{
		boolean result = false;
		for (Type t : ts)
			if (t.isErronuous()) 
			{
				result = true; 
				break;
			}
		return result;
	}

	/**
	 * Is this type equivalent ot other.
	 */
	public boolean isSameType(Type other)
	{
		if (this == other) return true;
		switch (this.tag)
		{
			case BYTE:
			case CHAR:
			case INT:
			case LONG:
			case FLOAT:
			case DOUBLE:
			case BOOL:
			case VOID:
			case NONE:
				return this.tag == other.tag;
			default:
				throw new AssertionError("isSameType " + this.tag);
		}
	}

	/**
	 * Estimates the equality of two type list.
	 * 
	 * @param first
	 * @param second
	 * @return
	 */
	public boolean isSameTypes(List<Type> first, List<Type> second)
	{
		Iterator<Type> it1, it2;
		if (first.size() != second.size()) return false;

		for (it1 = first.iterator(), it2 = second.iterator(); it1.hasNext()
		        && it2.hasNext();)
		{
			if (!it1.next().isSameType(it2.next())) break;
		}
		return !it1.hasNext() || !it2.hasNext();
	}

	/**
	 * Is this type a sub-type of that type? (not defined for Method)
	 * 
	 * @param that
	 * @return
	 */
	public boolean isSubType(Type that)
	{
		if (this == that) return true;
		switch (this.tag)
		{
			case BYTE:

			case CHAR:
				return (this.tag == that.tag || this.tag + 2 <= that.tag
				        && that.tag <= DOUBLE);

			case SHORT:

			case INT:

			case LONG:

			case FLOAT:

			case DOUBLE:
				return this.tag <= that.tag && that.tag <= DOUBLE;

			case BOOL:

			case VOID:
				return this.tag == that.tag;

			default:
				throw new AssertionError("isSubType " + this.tag);
		}
	}

	/**
	 * Is this type a super type of that type?
	 * 
	 * @param that
	 * @return
	 */
	public boolean isSuperType(Type that)
	{
		return that.isSubType(this);
	}

	/**
	 * Is this type assignable to that type?
	 * 
	 * @param that
	 * @return
	 */
	public boolean isAssignable(Type that)
	{
		if (this.tag <= INT && this.constValue != null)
		{
			int value = ((Number) this.constValue).intValue();
			switch (that.tag)
			{
				case BYTE:
					if (this.tag != CHAR && value >= Byte.MIN_VALUE
					        && value <= Byte.MAX_VALUE) return true;
					break;
				case CHAR:
					if (value >= Character.MIN_VALUE
					        && value <= Character.MAX_VALUE) return true;
					break;
				case SHORT:
					if (Short.MIN_VALUE <= value && value <= Short.MAX_VALUE)
					    return true;
					break;

				case INT:
					return true;
			}
		}
		return this.isSubType(that);
	}

	/**
	 * If this type is castable to that type, return the result of the cast,
	 * otherwise null returned.
	 * 
	 * @param that
	 * @return
	 */
	public boolean isCastable(Type that)
	{
		if (that.tag == ERROR) return true;
		switch (this.tag)
		{
			case BYTE:

			case CHAR:

			case SHORT:

			case INT:

			case LONG:

			case FLOAT:

			case DOUBLE:
				return that.tag <= DOUBLE;

			case BOOL:
				return that.tag == BOOL;

			case VOID:
				return false;

			default:
				throw new AssertionError();
		}
	}

	/**
	 * 判断该类型是否是有符号类型
	 * 
	 * @return 如果是，则返回true;否则返回false
	 * @exception Error
	 */
	public boolean isSigned()
	{
		throw new Error("#isSigned for non-integer type");
	}

	/**
	 * The source code witch this type represented. A list 
	 * will always be represented as a comma-seperated listing
	 * of the elements in that list. 
	 * 
	 * @param argtypes
	 * @return
	 */
	public static String toStringList(List<Type> argtypes)
    {
		if (argtypes.isEmpty())
			return "";
		else 
		{
			StringBuilder builder = new StringBuilder();
			builder.append(argtypes.get(0).toString());
			for (int idx = 1; idx < argtypes.size(); idx++)
			{
				builder.append(',');
				builder.append(argtypes.get(idx));
			}
			return builder.toString();
		}
		
    }

	/**
	 * Is a array type?
	 * @return
	 */
    public boolean isArrayType()
    {
		return tag == ARRAY;
    }
    /**
     * Is primitive type, such as byte, char, short, int, long etc?
     * @return
     */
    public boolean isPrimitiveType()
    {
    	return tag >= BYTE && tag <= DOUBLE;
    }
    
    /**
     * Is assignable to integer?
     */
    public boolean isIntLike()
    {
    	return tag >= BYTE && tag <= LONG;
    }
    
	
	public static class ErrorType extends Type
	{
		public Name name;

		public ErrorType(TypeSymbol errSymbol)
		{
			super(ERROR, errSymbol);
		}

		public ErrorType(Name name, TypeSymbol errSymbol)
		{
			this(errSymbol);
			this.name = name;
		}

		public Type constType(Object constValue)
		{
			return this;
		}

		public Type basetype()
		{
			return this;
		}

		/**
		 * This method just for array type.
		 * 
		 * @return
		 */
		public Type elemType()
		{
			return this;
		}

		public Type returnType()
		{
			return this;
		}

		public boolean isErroneous()
		{
			return true;
		}

		public boolean isSameType(Type that)
		{
			return true;
		}

		public boolean isCastable(Type that)
		{
			return true;
		}

		public boolean hasSameArgs(Type that)
		{
			return false;
		}

		public boolean isAssignable(Type that)
		{
			return true;
		}

		public boolean isSubType(Type that)
		{
			return true;
		}

		public boolean isSuperType(Type that)
		{
			return true;
		}
	}
}
