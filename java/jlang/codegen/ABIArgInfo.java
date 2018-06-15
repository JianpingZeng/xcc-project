package jlang.codegen;

import backend.type.Type;

import static jlang.codegen.ABIArgInfo.Kind.*;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class ABIArgInfo
{
    public enum Kind
	{
		/**
		 * Pass the argument directly using the normal converted backend type.
		 * Complex and array type are passed using first class aggregate.
		 */
		Direct,

		Extend,

		/**
		 * Pass the argument indirectly by a hidden pointer.
		 */
		Indirect,

		/**
		 * Ignore the passed in argument (treat as void).
		 * Useful for void type and empty struct type.
		 */
		Ignore,

		Coerce,

		Expand;

		public static final Kind FirstKind = Direct;
		public static final Kind LastKind = Expand;
	}

	private Kind kind;
	private Type typeData;
	private int intData;

	public ABIArgInfo()
	{
		kind = Direct;
		typeData = null;
	}

	private ABIArgInfo(Kind kind, Type ty, int id)
	{
		this.kind = kind;
		typeData = ty;
		intData = id;
	}
	public static ABIArgInfo getDirect()
	{
		return new ABIArgInfo(Direct, null, 0);
	}

	public static ABIArgInfo getExtend()
	{
		return new ABIArgInfo(Extend, null, 0);
	}

	public static ABIArgInfo getIgnore()
	{
		return new ABIArgInfo(Ignore, null, 0);
	}

	public static ABIArgInfo getIndirect(int alignment)
	{
		return new ABIArgInfo(Indirect, null, alignment);
	}

	public static ABIArgInfo getExpand()
	{
		return new ABIArgInfo(Expand, null, 0);
	}

	public static ABIArgInfo getCoerce(Type ty)
	{
		return new ABIArgInfo(Coerce, ty, 0);
	}

    public Kind getKind()
    {
        return kind;
    }

    public boolean isDirect()
    {
        return kind == Direct;
    }

	public boolean isIndirect()
    {
        return kind == Indirect;
    }

	public boolean isIgnore()
    {
        return kind == Ignore;
    }

	public Type getType()
    {
        return typeData;
    }

    public Type getCoerceType()
    {
        assert kind == Coerce :"Invalid kind!";
        return typeData;
    }
	public int getIndirectAlign()
	{
		assert kind == Indirect:"Invalid accessor!";
		return intData;
	}
}
