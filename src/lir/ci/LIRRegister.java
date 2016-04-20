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

import lir.backend.Architecture;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;

/**
 * Represents a register relative to target machine.
 */
public final class LIRRegister implements Comparable<LIRRegister>, Serializable
{
	private static final long serialVersionUID = -8716409387825941222L;

	/**
	 * Invalid register.
	 */
	public static final LIRRegister None = new LIRRegister(-1, -1, 0, "noreg");

	/**
	 * Frame pointer of the current method. All spill slots and outgoing stack-based arguments
	 * are addressed relative to this register.
	 */
	public static final LIRRegister Frame = new LIRRegister(-2, -2, 0, "framereg",
			RegisterFlag.CPU);

	public static final LIRRegister CallerFrame = new LIRRegister(-3, -3, 0,
			"callerframereg", RegisterFlag.CPU);

	/**
	 * The identifier for this register that is unique across all the LIRRegisters
	 * in a {@link Architecture}. A valid register has {@code number > 0}.
	 */
	public final int number;

	/**
	 * The mnemonic of this register.
	 */
	public final String name;

	/**
	 * The actual encoding in a targetAbstractLayer machine instruction for this register,
	 * which may or may not be the same as {@link #number}.
	 */
	public final int encoding;

	/**
	 * The length of the stack slot used to spill the value of this register.
	 */
	public final int spillSlotSize;

	/**
	 * The set of {@link RegisterFlag} values associated with this register.
	 */
	private final int flags;

	/**
	 * An array of {@link LIRRegisterValue} objects, for this register, with one entry
	 * per {@link LIRKind}, indexed by {@link LIRKind#ordinal}.
	 */
	private final LIRRegisterValue[] values;

	/**
	 * Attributes that characterize a register in a useful way.
	 */
	public enum RegisterFlag
	{
		/**
		 * Denotes an integral (i.e. non floating point) register.
		 */
		CPU,

		/**
		 * Denotes a register whose lowest order byte can be addressed separately.
		 */
		Byte,

		/**
		 * Denotes a floating point register.
		 */
		FPU;

		public final int mask = 1 << (ordinal() + 1);
	}

	/**
	 * Creates a {@code LIRRegister} instance.
	 *
	 * @param number        unique identifier for the register
	 * @param encoding      the targetAbstractLayer machine encoding for the register
	 * @param spillSlotSize the length of the stack slot used to spill the value of the register
	 * @param name          the mnemonic name for the register
	 * @param flags         the set of {@link RegisterFlag} values for the register
	 */
	public LIRRegister(int number, int encoding, int spillSlotSize, String name,
			RegisterFlag... flags)
	{
		this.number = number;
		this.name = name;
		this.spillSlotSize = spillSlotSize;
		this.flags = createMask(flags);
		this.encoding = encoding;

		values = new LIRRegisterValue[LIRKind.VALUES.length];
		for (LIRKind kind : LIRKind.VALUES)
		{
			values[kind.ordinal()] = new LIRRegisterValue(kind, this);
		}
	}

	private int createMask(RegisterFlag... flags)
	{
		int result = 0;
		for (RegisterFlag f : flags)
		{
			result |= f.mask;
		}
		return result;
	}

	public boolean isSet(RegisterFlag f)
	{
		return (flags & f.mask) != 0;
	}

	/**
	 * Gets this register as a {@linkplain LIRRegisterValue value} with a specified kind.
	 *
	 * @param kind the specified kind
	 * @return the {@link LIRRegisterValue}
	 */
	public LIRRegisterValue asValue(LIRKind kind)
	{
		return values[kind.ordinal()];
	}

	/**
	 * Gets this register as a {@linkplain LIRRegisterValue value} with no particular kind.
	 *
	 * @return a {@link LIRRegisterValue} with {@link LIRKind#Illegal} kind.
	 */
	public LIRRegisterValue asValue()
	{
		return asValue(LIRKind.Illegal);
	}

	/**
	 * Determines if this is a valid register.
	 *
	 * @return {@code true} iff this register is valid
	 */
	public boolean isValid()
	{
		return number >= 0;
	}

	/**
	 * Determines if this a floating point register.
	 */
	public boolean isFpu()
	{
		return isSet(RegisterFlag.FPU);
	}

	/**
	 * Determines if this a general purpose register.
	 */
	public boolean isCpu()
	{
		return isSet(RegisterFlag.CPU);
	}

	/**
	 * Determines if this register has the {@link RegisterFlag#Byte} attribute set.
	 *
	 * @return {@code true} iff this register has the {@link RegisterFlag#Byte} attribute set.
	 */
	public boolean isByte()
	{
		return isSet(RegisterFlag.Byte);
	}

	/**
	 * Categorizes a set of LIRRegisters by {@link RegisterFlag}.
	 *
	 * @param LIRRegisters a list of LIRRegisters to be categorized
	 * @return a map from each {@link RegisterFlag} constant to the list of LIRRegisters for which the flag is
	 * {@linkplain #isSet(RegisterFlag) set}
	 */
	public static EnumMap<RegisterFlag, LIRRegister[]> categorize(
			LIRRegister[] LIRRegisters)
	{
		EnumMap<RegisterFlag, LIRRegister[]> result = new EnumMap<RegisterFlag, LIRRegister[]>(
				RegisterFlag.class);
		for (RegisterFlag flag : RegisterFlag.values())
		{
			ArrayList<LIRRegister> list = new ArrayList<LIRRegister>();
			for (LIRRegister r : LIRRegisters)
			{
				if (r.isSet(flag))
				{
					list.add(r);
				}
			}
			result.put(flag, list.toArray(new LIRRegister[list.size()]));
		}
		return result;
	}

	/**
	 * Gets the maximum register {@linkplain #number number} in a given set of LIRRegisters.
	 *
	 * @param LIRRegisters the set of LIRRegisters to process
	 * @return the maximum register number for any register in {@code LIRRegisters}
	 */
	public static int maxRegisterNumber(LIRRegister[] LIRRegisters)
	{
		int max = Integer.MIN_VALUE;
		for (LIRRegister r : LIRRegisters)
		{
			if (r.number > max)
			{
				max = r.number;
			}
		}
		return max;
	}

	/**
	 * Gets the maximum register {@linkplain #encoding encoding} in a given set of LIRRegisters.
	 *
	 * @param LIRRegisters the set of LIRRegisters to process
	 * @return the maximum register encoding for any register in {@code LIRRegisters}
	 */
	public static int maxRegisterEncoding(LIRRegister[] LIRRegisters)
	{
		int max = Integer.MIN_VALUE;
		for (LIRRegister r : LIRRegisters)
		{
			if (r.encoding > max)
			{
				max = r.encoding;
			}
		}
		return max;
	}

	@Override public String toString()
	{
		return name;
	}

	@Override public int compareTo(LIRRegister o)
	{
		if (number < o.number)
		{
			return -1;
		}
		if (number > o.number)
		{
			return 1;
		}
		return 0;
	}

}
