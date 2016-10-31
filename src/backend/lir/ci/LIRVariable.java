/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package backend.lir.ci;

/**
 * Represents a virtual variable that is yet to be bound to a machine location
 * (such as a {@linkplain LIRRegister register} or stack {@linkplain LIRAddress
 * address}) by a register allocator.
 */
public final class LIRVariable extends LIRValue
{
	private static final long serialVersionUID = 3894386843980093903L;

	/**
	 * The identifier of the variable. This is a non-zero index in a contiguous
	 * 0-based name space.
	 */
	public final int index;

	/**
	 * Creates a new variable.
	 *
	 * @param kind
	 * @param index
	 */
	private LIRVariable(LIRKind kind, int index)
	{
		super(kind);
		this.index = index;
	}

	private static LIRVariable[] generate(LIRKind kind, int count)
	{
		LIRVariable[] variables = new LIRVariable[count];
		for (int i = 0; i < count; i++)
		{
			variables[i] = new LIRVariable(kind, i);
		}
		return variables;
	}

	private static final int CACHE_PER_KIND_SIZE = 100;

	/**
	 * Cache of common variables.
	 */
	private static final LIRVariable[][] cache = new LIRVariable[LIRKind
			.values().length][];

	static
	{
		for (LIRKind kind : LIRKind.values())
		{
			cache[kind.ordinal()] = generate(kind, CACHE_PER_KIND_SIZE);
		}
	}

	/**
	 * Gets variable for a given kind and index or creates a new one if the it
	 * no exist at {@code #cache}.
	 * @param kind
	 * @param index
	 * @return the corresponding {@code LIRVariable}
	 */
	public static LIRVariable get(LIRKind kind, int index)
	{
		assert kind == kind.stackKind() : "Variables can be only created for stack kinds";
		assert index >= 0;
		LIRVariable[] cachedVars = cache[kind.ordinal()];
		if (index < cachedVars.length)
		{
			return cachedVars[index];
		}
		return new LIRVariable(kind, index);
	}

	@Override public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		if (obj instanceof LIRVariable)
		{
			LIRVariable var = (LIRVariable) obj;
			return kind == var.kind && index == var.index;
		}
		return false;
	}

	@Override public boolean equalsIgnoringKind(LIRValue o)
	{
		if (this == o)
		{
			return true;
		}
		if (o instanceof LIRVariable)
		{
			LIRVariable var = (LIRVariable) o;
			return index == var.index;
		}
		return false;
	}

	@Override public int hashCode()
	{
		return (index << 4) | kind.ordinal();
	}

	@Override public String name()
	{
		return "v" + index;
	}
}
