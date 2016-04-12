/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package lir.ci;

import sun.misc.Unsafe;

import static lir.ci.CiKind.Flags.*;

/**
 * Denotes the basic kinds of types in cc IR, including the all the primitive types,
 * for example, {@link CiKind#Int} for {@code int}.
 * A kind has a single character short name, a CC name, and a set of flags
 * further describing its behavior.
 */
public enum CiKind
{
	Boolean('z', "boolean", VAR_TYPE | RETURN_TYPE | PRIMITIVE),
	Byte('b', "byte", VAR_TYPE | RETURN_TYPE | PRIMITIVE),
	Short('s', "short", VAR_TYPE | RETURN_TYPE | PRIMITIVE),
	Char('c', "char", VAR_TYPE | RETURN_TYPE | PRIMITIVE),
	Int('i', "int", VAR_TYPE | RETURN_TYPE | PRIMITIVE),
	Float('f', "float", VAR_TYPE | RETURN_TYPE | PRIMITIVE),
	Long('l', "long", VAR_TYPE | RETURN_TYPE | PRIMITIVE),
	Double('d', "double", VAR_TYPE | RETURN_TYPE | PRIMITIVE),
	Object('a', "object", VAR_TYPE | RETURN_TYPE),
	Void('v', "void", RETURN_TYPE),
	Illegal('-', "illegal", 0);

	public static final CiKind[] VALUES = values();
	public static final CiKind[] JAVA_VALUES = new CiKind[] { CiKind.Boolean,
			CiKind.Byte, CiKind.Short, CiKind.Char, CiKind.Int, CiKind.Float,
			CiKind.Long, CiKind.Double };

	CiKind(char ch, String name, int flags)
	{
		this.typeChar = ch;
		this.cflatName = name;
		this.flags = flags;
	}

	public CiKind stackKind()
	{
		if (isInt())
		{
			return Int;
		}
		return this;
	}

	static class Flags
	{
		/**
		 * Can be type of a variable.
		 */
		public static final int VAR_TYPE = 0x0001;
		/**
		 * Can be result type of a method.
		 */
		public static final int RETURN_TYPE = 0x0002;
		/**
		 * Represents a C-flat primitive type.
		 */
		public static final int PRIMITIVE = 0x0004;
	}

	/**
	 * The flags for this kind.
	 */
	private final int flags;

	/**
	 * The name of the kind as a single character.
	 */
	public final char typeChar;

	/**
	 * The name of this kind which will also be it C-flat programming language name if
	 * it is {@linkplain #isPrimitive() primitive} or {@code void}.
	 */
	public final String cflatName;

	/**
	 * Checks whether this kind is valid as the type of a variable.
	 *
	 * @return {@code true} if this kind is valid as the type of a C-flat variable.
	 */
	public boolean isValidVariableType()
	{
		return (flags & VAR_TYPE) != 0;
	}

	/**
	 * Checks whether this kind is valid as the return type of a method.
	 *
	 * @return {@code true} if this kind is valid as the return type of a C-flat function.
	 */
	public boolean isValidReturnType()
	{
		return (flags & RETURN_TYPE) != 0;
	}

	/**
	 * Checks whether this type is a C-flat primitive type.
	 *
	 * @return {@code true} if this is {@link #Boolean}, {@link #Byte}, {@link #Char}, {@link #Short},
	 * {@link #Int}, {@link #Long}, {@link #Float} or {@link #Double}.
	 */
	public boolean isPrimitive()
	{
		return (flags & PRIMITIVE) != 0;
	}

	public static CiKind fromTypeString(String typeString)
	{
		assert typeString.length() > 0;
		final char first = typeString.charAt(0);
		return CiKind.fromPrimitiveOrVoidTypeChar(first);
	}

	/**
	 * Gets the kind from the character describing a primitive or void.
	 *
	 * @param ch the character
	 * @return the kind
	 */
	public static CiKind fromPrimitiveOrVoidTypeChar(char ch)
	{
		// Checkstyle: stop
		switch (ch)
		{
			case 'Z':
				return Boolean;
			case 'C':
				return Char;
			case 'F':
				return Float;
			case 'D':
				return Double;
			case 'B':
				return Byte;
			case 'S':
				return Short;
			case 'I':
				return Int;
			case 'L':
				return Long;
			case 'V':
				return Void;
		}
		// Checkstyle: resume
		throw new IllegalArgumentException(
				"unknown primitive or void type character: " + ch);
	}

	/**
	 * Checks whether this value type is integer.
	 *
	 * @return true if this type is integer.
	 */
	public final boolean isInt()
	{
		return this == CiKind.Int;
	}

	/**
	 * Checks whether this value type is void.
	 *
	 * @return {@code true} if this type is void
	 */
	public final boolean isVoid()
	{
		return this == CiKind.Void;
	}

	/**
	 * Checks whether this value type is long.
	 *
	 * @return {@code true} if this type is long
	 */
	public final boolean isLong()
	{
		return this == CiKind.Long;
	}

	/**
	 * Checks whether this value type is float.
	 *
	 * @return {@code true} if this type is float
	 */
	public final boolean isFloat()
	{
		return this == CiKind.Float;
	}

	/**
	 * Checks whether this value type is double.
	 *
	 * @return {@code true} if this type is double
	 */
	public final boolean isDouble()
	{
		return this == CiKind.Double;
	}

	/**
	 * Checks whether this value type is float or double.
	 *
	 * @return {@code true} if this type is float or double
	 */
	public final boolean isFloatOrDouble()
	{
		return this == CiKind.Double || this == CiKind.Float;
	}

	/**
	 * Checks whether this value type is an object type.
	 *
	 * @return {@code true} if this type is an object
	 */
	public final boolean isObject()
	{
		return this == CiKind.Object;
	}

	/**
	 * Converts this value type to a string.
	 */
	@Override public String toString()
	{
		return cflatName;
	}

	/**
	 * Marker interface for types that should be {@linkplain CiKind#format(Object) formatted}
	 * with their {@link Object#toString()} value.
	 */
	public interface FormatWithToString
	{
	}

	public final char signatureChar()
	{
		return Character.toUpperCase(typeChar);
	}

	public CiConstant readUnsafeConstant(Object value, long displacement)
	{
		Unsafe u = Unsafe.getUnsafe();
		switch (this)
		{
			case Boolean:
				return CiConstant.forBoolean(u.getBoolean(value, displacement));
			case Byte:
				return CiConstant.forByte(u.getByte(value, displacement));
			case Char:
				return CiConstant.forChar(u.getChar(value, displacement));
			case Short:
				return CiConstant.forShort(u.getShort(value, displacement));
			case Int:
				return CiConstant.forInt(u.getInt(value, displacement));
			case Long:
				return CiConstant.forLong(u.getLong(value, displacement));
			case Float:
				return CiConstant.forFloat(u.getFloat(value, displacement));
			case Double:
				return CiConstant.forDouble(u.getDouble(value, displacement));
			case Object:
				return CiConstant.forObject(u.getObject(value, displacement));
			default:
				assert false : "unexpected kind: " + this;
				return null;
		}
	}

	/**
	 * Gets a formatted string for a given value of this kind.
	 *
	 * @param value a value of this kind
	 * @return a formatted string for {@code value} based on this kind
	 */
	public String format(Object value)
	{
		if (isObject())
		{
			if (value == null)
			{
				return "null";
			}
			else
			{
				if (value instanceof String)
				{
					String s = (String) value;
					if (s.length() > 50)
					{
						return "\"" + s.substring(0, 30) + "...\"";
					}
					else
					{
						return " \"" + s + '"';
					}
				}
			}
		}
		return value.toString();
	}

}
