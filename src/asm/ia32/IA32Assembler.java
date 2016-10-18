package asm.ia32;

import lir.backend.RegisterConfig;
import lir.backend.MachineInfo;
import lir.ci.LIRKind;
import lir.ci.LIRRegister;
import asm.AbstractAssembler;

/**
 * This class implements an assembler that able to encode x86 instructions.
 * @author Xlous.zeng
 * @version 0.1
 */
public class IA32Assembler extends AbstractAssembler
{
	/**
	 * The x86 condition codes used for conditional jumps/moves.
	 */
	public enum ConditionFlag
	{
		zero(0x4), 
		notZero(0x5),
		equal(0x4),
		notEqual(0x5), 
		less(0xc), 
		lessEqual(0xe), 
		greater(0xf), 
		greaterEqual(0xd), 
		below(0x2), 
		belowEqual(0x6), 
		above(0x7), 
		aboveEqual(0x3), 
		overflow(0x0), 
		noOverflow(0x1), 
		carrySet(0x2), 
		carryClear(0x3), 
		negative(0x8), 
		positive(0x9), 
		parity(0xa), 
		noParity(0xb);

		public final int value;

		public static final ConditionFlag[] VALUES = values();
	
		private static final ConditionFlag[] intToConditionFlag = new ConditionFlag[16];

		static
		{
			for (ConditionFlag flag : ConditionFlag.values())
			{
				intToConditionFlag[flag.value] = flag;
			}
		}

		private ConditionFlag(int value)
		{
			this.value = value;
		}

		public ConditionFlag negation()
		{
			return intToConditionFlag[this.value ^ 0x1];
		}
	}

	/**
	 * The kind for pointers and raw registers. Since we know we are 32 bit
	 * here, we can hard code it.
	 */
	private static final LIRKind Word = LIRKind.Int;

	/**
	 * The frame register to which {@linkplain LIRRegister#Frame} or
	 * {@linkplain LIRRegister#CallerFrame} was bound.
	 */
	private final LIRRegister frameRegister;

	/**
	 * Constructs a instance of assembler for x86 platform.
	 * @param target	The target machine for which this assembler was constructed.
	 * @param config	The register configuration.
	 */
	public IA32Assembler(MachineInfo target, RegisterConfig config)
	{
		super(target);
		this.frameRegister = config == null ? null : config.getFrameRegister();
	}
	
	private static int encode(LIRRegister r)
	{
		assert r.encoding >= 0 && r.encoding < 16 
				: "encoding out of range: " + r.encoding;
		return r.encoding & 0x7;
	}
	
	

	@Override
	protected void patchJumpTarget(int branch, int target)
	{
		
	}

}
