package frontend.codegen;

import backend.type.Type;

import java.awt.image.DirectColorModel;

import static frontend.codegen.ABIArgInfo.Kind.Direct;
import static frontend.codegen.ABIArgInfo.Kind.Ignore;
import static frontend.codegen.ABIArgInfo.Kind.Indirect;

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

		/**
		 * Pass the argument indirectly by a hidden pointer.
		 */
		Indirect,

		/**
		 * Ignore the passed in argument (treat as void).
		 * Useful for void type and empty struct type.
		 */
		Ignore;

		public static final Kind FirstKind = Direct;
		public static final Kind LastKind = Ignore;
	}

	private Kind kind;
	private Type typeData;

	public ABIArgInfo()
	{
		kind = Direct;
		typeData = null;
	}

	private ABIArgInfo(Kind kind, Type ty)
	{
		this.kind = kind;
		typeData = ty;
	}
	public static ABIArgInfo getDirect()
	{
		return new ABIArgInfo(Direct, null);
	}

	public static ABIArgInfo getIgnore()
	{
		return new ABIArgInfo(Ignore, null);
	}

	public static ABIArgInfo getIndirect()
	{
		return new ABIArgInfo(Indirect, null);
	}

	public Kind getKind() {return kind;}
	public boolean isDirect() {return kind == Direct;}
	public boolean isIndirect() {return kind == Indirect;}
	public boolean isIgnore() {return kind == Ignore;}
}
