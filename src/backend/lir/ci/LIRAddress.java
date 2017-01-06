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
 * Represents an address in targetAbstractLayer machine memory, specified via some combination
 * of a base register, an index register, a displacement and a scale. Note that
 * the base and index LIRRegisters may be {@link LIRVariable variable}, that is as yet
 * unassigned to targetAbstractLayer machine LIRRegisters.
 */
public final class LIRAddress extends LIRValue
{
	/**
	 *
	 */
	private static final long serialVersionUID = 1639045593259310943L;

	/**
	 * A sentinel value used as a place holder in an instruction stream for an
	 * address that will be patched.
	 */
	public static final LIRAddress Placeholder = new LIRAddress(LIRKind.Illegal,
			LIRRegister.None.asValue());

	/**
	 * Base register that defines the start of the address computation; always present.
	 */
	public final LIRValue base;
	/**
	 * Optional index register, the value of which (possibly scaled by {@link #scale})
	 * is added to {@link #base}. IfStmt not present, is denoted by {@link LIRValue#IllegalValue}.
	 */
	public final LIRValue index;
	/**
	 * Scaling factor for indexing, dependent on targetAbstractLayer LIROperand getArraySize.
	 */
	public final Scale scale;
	/**
	 * Optional additive displacement.
	 */
	public final int displacement;

	/**
	 * Creates a {@code LIRAddress} with given base register, no scaling and no
	 * displacement.
	 *
	 * @param kind the kind of the value being addressed
	 * @param base the base register
	 */
	public LIRAddress(LIRKind kind, LIRValue base)
	{
		this(kind, base, IllegalValue, Scale.Times1, 0);
	}

	/**
	 * Creates a {@code LIRAddress} with given base register, no scaling and a given
	 * displacement.
	 *
	 * @param kind         the kind of the value being addressed
	 * @param base         the base register
	 * @param displacement the displacement
	 */
	public LIRAddress(LIRKind kind, LIRValue base, int displacement)
	{
		this(kind, base, IllegalValue, Scale.Times1, displacement);
	}

	/**
	 * Creates a {@code LIRAddress} with given base and offset LIRRegisters, no scaling
	 * and no displacement.
	 *
	 * @param kind   the kind of the value being addressed
	 * @param base   the base register
	 * @param offset the offset register
	 */
	public LIRAddress(LIRKind kind, LIRValue base, LIRValue offset)
	{
		this(kind, base, offset, Scale.Times1, 0);
	}

	/**
	 * Creates a {@code LIRAddress} with given base and index LIRRegisters, scaling and
	 * displacement.
	 * This is the most general constructor..
	 *
	 * @param kind         the kind of the value being addressed
	 * @param base         the base register
	 * @param index        the index register
	 * @param scale        the scaling factor
	 * @param displacement the displacement
	 */
	public LIRAddress(LIRKind kind, LIRValue base, LIRValue index, Scale scale,
			int displacement)
	{
		super(kind);

		if (index.isConstant())
		{
			long longIndex = ((LIRConstant) index).asLong();
			long longDisp = displacement + longIndex * scale.value;
			if ((int) longIndex != longIndex || (int) longDisp != longDisp)
			{
				throw new Error(
						"integer overflow when computing constant displacement");
			}
			displacement = (int) longDisp;
			index = IllegalValue;
			scale = Scale.Times1;
		}
		assert base.isIllegal() || base.isVariableOrRegister();
		assert index.isIllegal() || index.isVariableOrRegister();

		this.base = base;
		this.index = index;
		this.scale = scale;
		this.displacement = displacement;
	}

	/**
	 * A scaling factor used in complex addressing modes such as those supported
	 * by IA32 platforms.
	 */
	public enum Scale
	{
		Times1(1, 0),
		Times2(2, 1),
		Times4(4, 2),
		Times8(8, 3);

		Scale(int value, int log2)
		{
			this.value = value;
			this.log2 = log2;
		}

		/**
		 * The value (or multiplier) of this scale.
		 */
		public final int value;

		/**
		 * The {@linkplain #value value} of this scale log 2.
		 */
		public final int log2;

		public static Scale fromInt(int scale)
		{
			// Checkstyle: stop
			switch (scale)
			{
				case 1:
					return Times1;
				case 2:
					return Times2;
				case 4:
					return Times4;
				case 8:
					return Times8;
				default:
					throw new IllegalArgumentException(String.valueOf(scale));
			}
			// Checkstyle: resume
		}

		public static Scale fromShift(int shift)
		{
			return fromInt(1 << shift);
		}
	}

	/**
	 * IfStmt the base register is a {@link LIRRegisterValue} returns the associated {@link LIRRegister}
	 * otherwise raises an jlang.exception..
	 *
	 * @return the base {@link LIRRegister}
	 * @throws Error if {@code base} is not a {@link LIRRegisterValue}
	 */
	public LIRRegister base()
	{
		return base.asRegister();
	}

	/**
	 * IfStmt the index register is a {@link LIRRegisterValue} returns the associated {@link LIRRegister}
	 * otherwise raises an jlang.exception..
	 *
	 * @return the base {@link LIRRegister}
	 * @throws Error if {@code index} is not a {@link LIRRegisterValue}
	 */
	public LIRRegister index()
	{
		return index.asRegister();
	}

	@Override
	public LIRAddress asAddress()
	{
		return this;
	}

	/**
	 * Encodes the possible addressing modes as a simple value.
	 */
	public enum Format
	{
		BASE,
		BASE_DISP,
		BASE_INDEX,
		BASE_INDEX_DISP,
		PLACEHOLDER
    }

	/**
	 * Returns the {@link Format encoded addressing mode} that this {@code LIRAddress} represents.
	 *
	 * @return the encoded addressing mode
	 */
	public Format format()
	{
		if (this == Placeholder)
		{
			return Format.PLACEHOLDER;
		}
		assert base.isLegal();
		if (index.isLegal())
		{
			if (displacement != 0)
			{
				return Format.BASE_INDEX_DISP;
			}
			else
			{
				return Format.BASE_INDEX;
			}
		}
		else
		{
			if (displacement != 0)
			{
				return Format.BASE_DISP;
			}
			else
			{
				return Format.BASE;
			}
		}
	}

	private static String s(LIRValue location)
	{
		if (location.isRegister())
		{
			return location.asRegister().name;
		}
		assert location.isVariable();
		return "v" + ((LIRVariable) location).index;
	}

	private static String signed(int i)
	{
		if (i >= 0)
		{
			return "+" + i;
		}
		return String.valueOf(i);
	}

	@Override public String name()
	{
		// Checkstyle: stop
		switch (format())
		{
			case BASE:
				return "[" + s(base) + "]";
			case BASE_DISP:
				return "[" + s(base) + signed(displacement) + "]";
			case BASE_INDEX:
				return "[" + s(base) + "+" + s(index) + "]";
			case BASE_INDEX_DISP:
				return "[" + s(base) + "+(" + s(index) + "*" + scale.value + ")"
						+ signed(displacement) + "]";
			case PLACEHOLDER:
				return "[<placeholder>]";
			default:
				throw new IllegalArgumentException(
						"unknown format: " + format());
		}
		// Checkstyle: resume
	}

	@Override public boolean equals(Object obj)
	{
		if (obj instanceof LIRAddress)
		{
			LIRAddress addr = (LIRAddress) obj;
			return kind == addr.kind && displacement == addr.displacement
					&& base.equals(addr.base) && scale == addr.scale && index
					.equals(addr.index);
		}
		return false;
	}

	@Override public boolean equalsIgnoringKind(LIRValue o)
	{
		if (o instanceof LIRAddress)
		{
			LIRAddress addr = (LIRAddress) o;
			return displacement == addr.displacement && base
					.equalsIgnoringKind(addr.base) && scale == addr.scale
					&& index.equalsIgnoringKind(addr.index);
		}
		return false;
	}

	@Override public int hashCode()
	{
		return (base.hashCode() << 4) | kind.ordinal();
	}
}
