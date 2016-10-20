package lir;

/**
 * This enum defines a various of opcode for LIR instruction.
 * @author Xlous.zeng
 */
public enum LIROpcode
{
	BeginOp0,
	Label,
	Here,
	Info,
	UncommonTrap,
	Alloca,
	Breakpoint,
	Pause,
	Membar,
	Branch,
	CondFloatBranch,
	EndOp0,

	BeginOp1,
	Return,
	Lea,
	Neg,
	TableSwitch,
	Move,
	Prefetchr,
	Prefetchw,
	Convert,
	Lsb,
	Msb,
	EndOp1,

	BeginOp2,
	Cmp,
	Cmpl2i,
	Ucmpfd2i,
	Cmpfd2i,
	Cmove,
	Add,
	Sub,
	Mul,
	Div,
	Rem,
	Sqrt,
	Abs,
	Sin,
	Cos,
	Tan,
	Log,
	Log10,
	LogicAnd,
	LogicOr,
	LogicXor,
	Shl,
	Shr,
	Ushr,
	Unwind,
	CompareTo,
	IfBit,
	EndOp2,

	BeginOp3,
	Idiv,
	Irem,
	Iudiv,
	Iurem,
	Ldiv,
	Lrem,
	Ludiv,
	Lurem,
	EndOp3,

	DirectCall,
	IndirectCall,

	Phi
}
