package lir.linearScan;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
import compiler.*;
import exception.CiBailout;
import hir.BasicBlock;
import hir.Condition;
import hir.Method;
import lir.*;
import lir.backend.RegisterConfig;
import lir.ci.*;
import utils.*;
import java.util.*;
import java.util.stream.Collectors;
import static utils.Util.isEven;
import static utils.Util.isOdd;

/**
 * An implementation of the linear scan register allocator algorithm described
 * in <a href=http://www.christianwimmer.at/Publications/Wimmer10a/Wimmer10a.pdf>
 *     "Linear Scan Register Allocation on SSA Form"</a> by Christian Wimmer and
 *  Michael Franz.
 */
public final class LinearScan
{
	/**
	 * Determines if an {@link LIRInstruction} destroys all caller saved registers.
	 *
	 * @param opId an instruction {@linkplain LIRInstruction#id id}
	 * @return {@code true} if the instruction denoted by {@code id} destroys all
	 * caller saved registers.
	 */
	boolean hasCall(int opId)
	{
		assert isEven(opId) : "opId not even";
		return instructionForId(opId).hasCall;
	}

	abstract static class IntervalPredicate
	{
		abstract boolean apply(Interval i);
	}

	final Backend backend;
	final Method m;
	final LIRGenerator gen;
	final StackFrame frameMap;
	final RegisterAttributes[] registerAttributes;
	final LIRRegister[] registers;

	private static final int INITIAL_SPLIT_INTERVALS_CAPACITY = 32;

	/**
	 * List of blocks in linear-scan order. This is only correct as long as the
	 * CFG does not change.
	 */
	final BasicBlock[] sortedBlocks;

	final OperandPool operands;

	/**
	 * Number of stack slots used for intervals allocated to memory.
	 */
	int maxSpills;

	/**
	 * Unused spill slot for a single-word value because of alignment of a
	 * double-word value.
	 */
	StackSlot unusedSpillSlot;

	/**
	 * Map from {@linkplain #operandNumber(LIRValue) operand numbers} to intervals.
	 */
	Interval[] intervals;

	/**
	 * The number of valid entries in {@link #intervals}.
	 */
	int intervalsSize;

	/**
	 * The index of the first entry in {@link #intervals} for a {@linkplain
	 * #createDerivedInterval(Interval) derived interval}.
	 */
	int firstDerivedIntervalIndex = -1;

	/**
	 * Intervals sorted by {@link Interval#from()}.
	 */
	Interval[] sortedIntervals;

	/**
	 * Map from an instruction {@linkplain LIRInstruction#id id} to the instruction.
	 * Entries should be retrieved with {@link #instructionForId(int)} as the id is
	 * not simply an index into this array.
	 */
	LIRInstruction[] opIdToInstructionMap;

	/**
	 * Map from an instruction {@linkplain LIRInstruction#id id} to the {@linkplain
	 * BasicBlock block} containing the instruction. Entries should be retrieved with
	 * {@link #blockForId(int)} as the id is not simply an index into this array.
	 */
	BasicBlock[] opIdToBlockMap;

	/**
	 * Bit set for each variable that is contained in each loop.
	 */
	BitMap2D intervalInLoop;

	public LinearScan(Backend backend, Method m, LIRGenerator gen,
			StackFrame frameMap)
	{
		this.backend = backend;
		this.m = m;
		this.gen = gen;
		this.frameMap = frameMap;
		this.maxSpills = frameMap.initialSpillSlot();
		this.unusedSpillSlot = null;
		this.sortedBlocks = m.linearScanOrder()
				.toArray(new BasicBlock[m.linearScanOrder().size()]);
		LIRRegister[] allocatableRegisters = backend.registerConfig
				.getAllocatableRegisters();
		this.registers = new LIRRegister[
				LIRRegister.maxRegisterNumber(allocatableRegisters) + 1];
		for (LIRRegister reg : allocatableRegisters)
		{
			registers[reg.number] = reg;
		}
		this.registerAttributes = backend.registerConfig.getAttributesMap();
		this.operands = gen.operands;
	}

	/**
	 * Converts an operand (variable or register) to an index in a flat address
	 * space covering all the {@linkplain lir.ci.LIRVariable variables} and
	 * {@linkplain lir.ci.LIRRegisterValue registers} being processed by allocator.
	 *
	 * @param opr
	 * @return
	 */
	int operandNumber(LIRValue opr)
	{
		return operands.operandNumber(opr);
	}

	/**
	 * To check if the operand corresponding to interval has a fixed register.
	 */
	static final IntervalPredicate IS_PRECOLORED_INTERVAL = new IntervalPredicate()
	{
		@Override boolean apply(Interval i)
		{
			return i.operand.isRegister();
		}
	};

	/**
	 * To check if the operand corresponding to interval is a virtual register.
	 */
	static final IntervalPredicate IS_VARIABLE_INTERVAL = new IntervalPredicate()
	{
		@Override boolean apply(Interval i)
		{
			return i.operand.isVariable();
		}
	};

	/**
	 * Gets an object describing the attributes of a given register according to
	 * this register configuration.
	 */
	RegisterAttributes attributes(LIRRegister reg)
	{
		return registerAttributes[reg.number];
	}

	/**
	 * Allocates a stack slot at current activate frame for specified kind.
	 *
	 * @param kind
	 * @return
	 */
	StackSlot allocateSpillSlot(LIRKind kind)
	{
		StackSlot slot;
		// it means that the number of occupied stack slot for
		// specified kind is 2
		if (numOfSpillSlots(kind) == 2)
		{
			if (isOdd(maxSpills))
			{
				// alignment of double slot VALUES
				assert unusedSpillSlot == null : "wasting a spill slot";
				unusedSpillSlot = StackSlot.get(kind, maxSpills);
				maxSpills++;
			}
			slot = StackSlot.get(kind, maxSpills);
			maxSpills += 2;
		}
		else if (unusedSpillSlot != null)
		{
			// reuse hole that was the result of a preceded double word alignment
			// stack slot
			slot = unusedSpillSlot;
			unusedSpillSlot = null;
		}
		else
		{
			// it means that the value for allocation occupy just one stack slot
			slot = StackSlot.get(kind, maxSpills);
			maxSpills++;
		}
		return slot;
	}

	public int numOfSpillSlots(LIRKind kind)
	{
		return backend.targetMachine.spillSlots(kind);
	}

	/**
	 * assigns a spill stack slot to specified interval.
	 * @param i The interval instance being assigned.
	 */
	void assignSpillSlot(Interval i)
	{
		if (i.spillSlot() != null)
		{
			i.assignLocation(i.spillSlot());
		}
		else
		{
			StackSlot slot = allocateSpillSlot(i.kind());
			i.setSpillSlot(slot);
			i.assignLocation(slot);
		}
	}

	/**
	 * Creates a new instance of {@code Interval} for specified {@code LIRValue}.
	 *
	 * @param opr
	 * @return
	 */
	Interval createInterval(LIRValue opr)
	{
		assert isProcessed(opr);
		assert opr.isLegal();
		int oprNum = operandNumber(opr);
		Interval interval = new Interval(opr, oprNum);
		assert oprNum < intervalsSize;
		assert intervals[oprNum] == null;
		intervals[oprNum] = interval;
		return interval;
	}

	Interval createDerivedInterval(Interval source)
	{
		if (firstDerivedIntervalIndex == -1)
		{
			firstDerivedIntervalIndex = intervalsSize;
		}
		if (intervalsSize == intervals.length)
		{
			intervals = Arrays.copyOf(intervals, intervals.length * 2);
		}
		intervalsSize++;
		Interval interval = createInterval(operands.newVariable(source.kind()));
		assert intervals[intervalsSize - 1] == interval;
		return interval;
	}

	/**
	 * obtains the numbers of blocks sorted in linear scan order.
	 */
	int blockCount()
	{
		assert sortedBlocks.length == m.linearScanOrder()
				.size() : "invalid cached block sorted list";
		return sortedBlocks.length;
	}

	/**
	 * obtains the basic block at given position where indexable to.
	 *
	 * @param index
	 * @return
	 */
	BasicBlock blockAt(int index)
	{
		assert index >= 0 && index < sortedBlocks.length;
		return sortedBlocks[index];
	}

	static int opIDToIndex(int opID)
	{
		return opID >> 1;
	}

	/**
	 * Retrieves the {@link LIRInstruction} based on its {@linkplain LIRInstruction#id id}.
	 *
	 * @param opID an instruction {@linkplain LIRInstruction#id id}
	 * @return the instruction whose {@linkplain LIRInstruction#id} {@code == id}
	 */
	LIRInstruction instructionForId(int opID)
	{
		assert isEven(opID) : "opID not even";

		assert opID >= 0 && opID < opIdToInstructionMap.length;
		// opID must devided by 2
		LIRInstruction instr = opIdToInstructionMap[opID >> 1];
		assert instr.id == opID;
		return instr;
	}

	/**
	 * Numbers the all of instructions by flatten CFG.
	 */
	private void numberInstruction()
	{
		// assign ID to LIR nodes and build a mapping, lirOps, from ID to
		// LIRInstruction
		int numBlocks = blockCount();

		// the numbers of instructions
		int allInstrs = 0;
		for (int i = 0; i < numBlocks; i++)
		{
			allInstrs += blockAt(i).getLIRBlock().lir().instructionsList()
					.size();
		}

		opIdToInstructionMap = new LIRInstruction[allInstrs];
		opIdToBlockMap = new BasicBlock[numBlocks];

		// the number of LIR instruction is beginning from 0, and increasing by 2
		int opID = 0, index = 0;
		for (int i = 0; i < numBlocks; i++)
		{
			BasicBlock block = blockAt(i);

			// sets the id for first instruction
			block.setFirstLIRInstructionId(opID);
			List<LIRInstruction> instructions = block.getLIRBlock().lir()
					.instructionsList();

			int numInsts = instructions.size();
			for (int j = 0; j < numInsts; j++)
			{
				LIRInstruction op = instructions.get(j);
				op.id = opID;
				opIdToInstructionMap[index] = op;
				opIdToBlockMap[index] = block;
				assert instructionForId(opID) == op : "must match";

				index++;
				// numbering the LIR instruction by 2 according to Christian Wimmer
				opID += 2;
			}

			block.setLastLIRInstructionId(opID - 2);
		}
		assert index == allInstrs : "must match";
		assert (index << 1) == opID : "must match" + (index << 1);
	}

	/**
	 * Gets the size of the {@link LIRBlock#livein} and {@link LIRBlock#liveout}
	 * sets for a basic block. These sets do not include any operands allocated
	 * as a result of creating {@linkplain #createDerivedInterval(Interval) derived
	 * intervals}.
	 */
	private int liveSetSize()
	{
		return firstDerivedIntervalIndex == -1 ?
				operands.size() :
				firstDerivedIntervalIndex;
	}

	/**
	 * Gets the number of loop in specified method being compiled.
	 * @return
	 */
	private int numLoops()
	{
		return m.numLoops();
	}

	/**
	 * Computes local live sets (i.e. {@link LIRBlock#livegen} and
	 * {@link LIRBlock#livekill}) separately for each block.
	 */
	private void computeLocalLiveSets()
	{
		int numBlocks = blockCount();
		int liveSize = liveSetSize();

		BitMap2D localIntervalInLoop = new BitMap2D(operands.size(),
				numLoops());

		// iterate over all basic block
		for (int i = 0; i < numBlocks; i++)
		{
			BasicBlock block = blockAt(i);
			final BitMap livegen = new BitMap(liveSize);
			final BitMap livekill = new BitMap(liveSize);

			List<LIRInstruction> instructions = block.getLIRBlock().lir()
					.instructionsList();
			int numList = instructions.size();

			// iterate over all instructions of the block.
			// skip the first because it is always a label
			assert instructions.get(0)
					.hasOperands() : "first operation must always label";
			for (int j = 1; j < numList; j++)
			{
				final LIRInstruction opr = instructions.get(j);

				// iterate input operands of instruction
				int n = opr.operandCount(LIRInstruction.OperandMode.Input);
				for (int k = 0; k < n; k++)
				{
					LIRValue operand = opr
							.operandAt(LIRInstruction.OperandMode.Input, k);

					if (operand.isVariable())
					{
						int operandNum = operandNumber(operand);
						if (!livekill.get(operandNum))
						{
							livegen.set(operandNum);
						}
						// within a loop
						if (block.loopIndex >= 0)
						{
							localIntervalInLoop
									.setBit(operandNum, block.loopIndex);
						}
					}
				}// end of handling input operand

				// iterate temp operands of specified instruction as same as Output
				// operand
				n = opr.operandCount(LIRInstruction.OperandMode.Temp);
				for (int k = 0; k < n; k++)
				{
					LIRValue tmp = opr
							.operandAt(LIRInstruction.OperandMode.Temp, k);
					if (tmp.isVariable())
					{
						int varNum = operandNumber(tmp);
						livekill.set(varNum);
						if (block.loopIndex >= 0)
						{
							localIntervalInLoop.setBit(varNum, block.loopIndex);
						}
					}
				}

				// iterates output operand of specified instruction
				n = opr.operandCount(LIRInstruction.OperandMode.Output);
				for (int k = 0; k < n; k++)
				{
					LIRValue outputOpr = opr
							.operandAt(LIRInstruction.OperandMode.Output, k);
					if (outputOpr.isVariable())
					{
						int varNum = operandNumber(outputOpr);
						livekill.set(varNum);
					}
				}
			}// end of instruction iteration

			LIRBlock lirBlock = block.getLIRBlock();
			lirBlock.livegen = livegen;
			lirBlock.livekill = livekill;

			// allocates two new bit vector to livein and liveout set of BB
			lirBlock.livein = new BitMap(liveSize);
			lirBlock.liveout = new BitMap(liveSize);
		}// end of block iteration

		intervalInLoop = localIntervalInLoop;
	}

	/**
	 * Computes {@link LIRBlock#liveout LiveOut set} and {@link LIRBlock#livein LiveIn set}
	 * for every basic block seperately by performing backward data flow analysis.
	 */
	private void computeGlobalLiveSet()
	{
		int numBlocks = blockCount();
		boolean changedOccured;
		boolean changedOccuredInBlock;
		int iterationCount = 0;
		BitMap liveOut = new BitMap(liveSetSize());

		// perform a backward dataflow analysis to compute liveOut
		// and liveIn for each block. The loop is executed until a fixpoint
		// is reached (no changes in an iteration).
		do
		{
			changedOccured = false;
			// iterate all blocks in reverse order
			for (int i = numBlocks - 1; i >= 0; i--)
			{
				BasicBlock block = blockAt(i);
				LIRBlock lirBlock = block.getLIRBlock();

				changedOccuredInBlock = false;

				// liveOut[B] is the union of liveIn[sux]
				// , for successors sux of B.
				int numSuxs = block.getNumOfSuccs();
				if (numSuxs > 0)
				{
					BasicBlock firstSux = block.succAt(0);
					liveOut.setFrom(firstSux.getLIRBlock().livein);
					for (int j = 1; j < numSuxs; j++)
					{
						BitMap suxLiveIn = block.succAt(j).getLIRBlock().livein;
						assert liveOut.size() == suxLiveIn.size();
						liveOut.or(suxLiveIn);
					}
				}
				else
				{
					// the exit block for function
					liveOut.clear();
				}
				// update the liveOut of specified LIRBlock with liveOut
				// if and only if those are not same
				if (!lirBlock.liveout.isSame(liveOut))
				{
					// a change occured, swap the old and new live out
					// sets to aovid copying
					BitMap temp = lirBlock.liveout;
					lirBlock.liveout = liveOut;
					liveOut = temp;

					changedOccured = true;
					changedOccuredInBlock = true;
				}

				if (iterationCount == 0 || changedOccuredInBlock)
				{
					BitMap liveIn = lirBlock.livein;
					liveIn.setFrom(lirBlock.liveout);
					liveIn.diff(lirBlock.livekill);
					liveIn.or(lirBlock.livegen);
				}
			}
			iterationCount++;

			if (changedOccured && iterationCount > 50)
			{
				new CiBailout("too many iteration in computesGlobalLiveSet");
			}
		} while (changedOccured);
	}

	private boolean isProcessed(LIRValue operand)
	{
		return !operand.isRegister() || attributes(
				operand.asRegister()).isAllocatable;
	}

	private Interval intervalFor(LIRValue operand)
	{
		int operandNumber = operandNumber(operand);
		assert operandNumber < intervalsSize;
		return intervals[operandNumber];
	}

	/**
	 * adds temporary lifetime interval for caller-saved register when the flag of
	 * instruction's {@linkplain LIRInstruction#hasCall hasCall} is set.
	 *
	 * @param operand
	 * @param tempPos
	 * @param kind
	 */
	private void addTemp(LIRValue operand, int tempPos, LIRKind kind)
	{
		if (!isProcessed(operand))
			return;
		Interval interval = intervalFor(operand);
		if (interval == null)
		{
			interval = createInterval(operand);
		}
		if (kind != LIRKind.Illegal)
		{
			interval.setKind(kind);
		}
		// its length of range is seted with one
		interval.addRange(tempPos, tempPos + 1);
	}

	/**
	 * Phase 5: actual register allocation.
	 *
	 * @param op
	 */
	private void pdAddTemps(LIRInstruction op)
	{
		// currently, only x86 platform was considered.
		assert backend.targetMachine.arch.isX86();

		switch (op.opcode)
		{
			case Tan:
			case Sin:
			case Cos:
			{
				// The slow path for these functions may need to save and
				// restore all live registers but we don't want to save and
				// restore everything all the time, so mark the xmms as being
				// killed. If the slow path were explicit or we could propagate
				// live register masks down to the assembly we could do better
				// but we don't have any easy way to do that right now. We
				// could also consider not killing all xmm registers if we
				// assume that slow paths are uncommon but it's not clear that
				// would be a good idea.

				TTY.println("killing XMMs for trig");

				int opId = op.id;

				for (LIRRegister r : backend.registerConfig
						.getCallerSaveRegisters())
				{
					if (r.isFpu())
					{
						addTemp(r.asValue(), opId,
								LIRKind.Illegal);
					}
				}
				break;
			}
			default:
				break;
		}
	}

	/**
	 * Gets the highest instruction id allocated by this object.
	 */
	int maxOpId()
	{
		assert opIdToInstructionMap.length > 0 : "no operations";
		return (opIdToInstructionMap.length - 1) << 1;
	}

	/**
	 * Gets the block containing a given instruction.
	 *
	 * @param opid an instruction {@linkplain LIRInstruction#id id}
	 * @return the block containing the instruction denoted by {@code opId}
	 */
	BasicBlock blockForId(int opid)
	{
		assert opIdToBlockMap.length > 0 && opid >= 0
				&& opid <= maxOpId() + 1 : "opID out of range";
		return opIdToBlockMap[opid >> 1];
	}

	/**
	 * Creates a interval for the result operand of {@linkplain LIRInstruction}.
	 * Output operands of the {@linkplain LIRInstruction} shorten the first range
	 * of the interval of given operand. The definition overwrites any previous
	 * value of the operand, so the operand cnanot live immediately before this
	 * operation.
	 *
	 * @param operand  The result operand.
	 * @param defPos   The id of specified LIRInstruction that defines operand.
	 * @param kind
	 */
	private void addDef(LIRValue operand, int defPos,
			LIRKind kind)
	{
		if (!isProcessed(operand))
			return;

		TTY.println(" def %s defPos %d", operand, defPos);
		Interval interval = intervalFor(operand);
		if (interval != null)
		{
			if (kind != LIRKind.Illegal)
			{
				interval.setKind(kind);
			}

			Range r = interval.first();
			// narrow the range of this interval 
			if ( defPos > r.from)
			{
				// update the starting point (when a range is first created for a
				// use , its start is the beginning of the current block until
				// a def in encountered.)
				r.from = defPos;
			}
			else
			{
				// dead value - make meaningless interval
				interval.addRange(defPos, defPos + 1);
				TTY.println(
						"Warning: def of operand %s at %d occurs without use",
						operand, defPos);
			}
		}
		else
		{
			// dead value - make vacuous interval
			interval = createInterval(operand);
			if (kind != LIRKind.Illegal)
				interval.setKind(kind);

			interval.addRange(defPos, defPos + 1);

			TTY.println("Warning: def of operand %s at %d occurs without use",
					operand, defPos);
		}
	}
	/**
	 * Creates a interval for purpose of usage.
	 * @param operand	The operand typed of {@linkplain LIRValue} which is used.
	 * @param from	The starting position of using range.
	 * @param to	The ending position of using range.
	 * @param kind	The kind of operand.
	 * @return	
	 */
	Interval addUse(LIRValue operand, int from, int to, LIRKind kind)
	{
		if (!isProcessed(operand))
			return null;

		if (kind == null)
			kind = operand.kind.stackKind();

		Interval interval = intervalFor(operand);
		if (interval == null)
		{
			interval = createInterval(operand);
		}
		if (kind != LIRKind.Illegal)
		{
			interval.setKind(kind);
		}
		if (operand.isVariable())
		{
			interval.addRange(from, maxOpId());
		}
		else
		{
			interval.addRange(from, to);
		}
		return interval;
	}

	/**
	 * Optimizes moves related to incoming stack based arguments.
	 * The interval for the destination of such moves is assigned
	 * the same stack slot with incoming argument (which is in the caller's frame) 
	 * when its spill slot.
	 */
	private void handleFunctionArguments(LIRInstruction instr)
	{
		if (instr.opcode == LIROpcode.Move)
		{
			LIROp1 move = (LIROp1) instr;

			if (move.operand().isStackSlot())
			{
				StackSlot slot = (StackSlot) move.operand();
				Interval interval = intervalFor(move.result());
				StackSlot copySlot = slot;
				if (slot.kind == LIRKind.Object)
				{
					copySlot = allocateSpillSlot(slot.kind);
				}
				interval.setSpillSlot(copySlot);
				interval.assignLocation(copySlot);
			}
		}
	}

	/**
	 * Builds interval list for every virtual register, called variable.
	 */
	private void buildIntervals()
	{
		intervalsSize = operands.size();
		intervals = new Interval[intervalsSize
				+ INITIAL_SPLIT_INTERVALS_CAPACITY];

		// create a list with all caller-saved register (e.g.eax, ecx, edx for x86 cpu)
		RegisterConfig registerConfig = backend.registerConfig;
		LIRRegister[] callerRegisters = registerConfig.getCallerSaveRegisters();

		// iterate over all blocks in reverse order
		for (int i = blockCount() - 1; i >= 0; i--)
		{
			BasicBlock block = blockAt(i);
			List<LIRInstruction> instructions = block.getLIRBlock().lir()
					.instructionsList();

			final int blockFrom = block.firstLIRInstructionId();
			int blockTo = block.lastLIRInstructionId();

			assert blockFrom == instructions.get(0).id;
			assert blockTo == instructions.get(instructions.size() - 1).id;

			// update intervals for operands live at the end of this block
			BitMap liveOut = block.getLIRBlock().liveout;

			for (int operandNum = liveOut.nextSetBit(0);
			     operandNum >= 0; operandNum = liveOut
					.nextSetBit(operandNum + 1))
			{
				LIRValue operand =  operands.operandFor(operandNum);
				addUse(operand, blockFrom, blockTo + 2, LIRKind.Illegal);
			}

			// iterate over all instructions of the block in reverse order.
			// skip the first instruction because it is always a label 
			// definitions of intervals are processed before uses
			assert !instructions.get(0)
					.hasOperands() : "the first instruction of block must be label";

			for (int j = instructions.size() - 1; j >= 1; j--)
			{
				LIRInstruction opr = instructions.get(j);
				final int opID = opr.id;

				// add temp range for each register if operand destroy 
				// caller-saved registers
				if (opr.hasCall)
				{
					for (LIRRegister r : callerRegisters)
					{
						if (attributes(r).isAllocatable)
							addTemp(r.asValue(), opID,
									LIRKind.Illegal);
					}
				}

				// add any platform dependent temps that can not implemented in x86
				pdAddTemps(opr);

				int k = 0;
				
				// visit definition (output and temp operands)
				int n = opr.operandCount(LIRInstruction.OperandMode.Output);
				for (k = 0; k < n; k++)
				{
					LIRValue operand = opr
							.operandAt(LIRInstruction.OperandMode.Output, k);
					assert operand.isVariableOrRegister();
					addDef(operand, opID, operand.kind.stackKind());
				}

				// visits temporary operands equal to output operands
				n = opr.operandCount(LIRInstruction.OperandMode.Temp);
				for (k = 0; k < n; k++)
				{
					LIRValue tmp = opr
							.operandAt(LIRInstruction.OperandMode.Temp, k);
					assert tmp.isVariableOrRegister();
					addTemp(tmp, opID,
							tmp.kind.stackKind());
				}

				// visits uses (input operands)
				n = opr.operandCount(LIRInstruction.OperandMode.Input);
				for (k = 0; k < n; k++)
				{
					LIRValue input = opr
							.operandAt(LIRInstruction.OperandMode.Input, k);
					assert input.isVariableOrRegister();

					Interval interval = addUse(input, blockFrom, opID, null);
					if (interval != null)
					{
						Range first = interval.first();
						if (first.to == opID)
							first.to++;
					}
				}

				// handles function arguments
				handleFunctionArguments(opr);
			}// end of instruction iteration

		} // end of block iteration

		// add the range [0, 1] to all fixed intervals
		// the register allocator need not handle unhandled fixed intervals
		for (Interval interval : intervals)
		{
			if (interval != null && interval.operand.isRegister())
			{
				interval.addRange(0, 1);
			}
		}
	}

	/**
	 * Sorts the interval list in increasing the start position of interval.
	 */
	private void sortIntervalListBeforeAllocation()
	{		
		List<Interval> temp = Arrays.asList(intervals).stream().
				filter((i) -> {return i != null;}).collect(Collectors.toList());
		
		// count the numbers of element is not null in intervals array
		int sortedLen = temp.size();

		Interval[] sortedList = new Interval[sortedLen];
		int sortedIdx = 0;
		int sortedFromMax = -1;	

		for (Interval i : temp)
		{
			int from = i.from();
			if (sortedFromMax <= from)
			{
				sortedList[sortedIdx] = i;
				sortedFromMax = i.from();
			}
			else
			{
				// when the start position of current interval is less than the
				// last element in array sortedList[0...sortedIdx]
				// so this interval (denoting by i) must be sorted in manually
				// using insertion sort algorithm
				int j;
				for (j = sortedIdx - 1;
				     j >= 0 && from < sortedList[j].from(); j--)
				{
					sortedList[j + 1] = sortedList[j];
				}
				sortedList[j + 1] = i;
			}
			sortedIdx++;
		}
		this.sortedIntervals = sortedList;
	}

	private boolean isSorted(Interval[] intervals)
	{
		boolean isSorted = true;
		for (int i = 1; i < intervals.length; i++)
		{
			if (intervals[i].from() < intervals[i - 1].from())
			{
				isSorted = false;
				break;
			}
		}
		return isSorted;
	}

	/**
	 * Adds a interval into interval list.
	 * @param head	The head of interval list.
	 * @param prev	The previous elements before the element being inserted.
	 * @param tobe	The interval to be inserted.
	 * @return	The head of new interval list after insertion.
	 */
	private Interval addToList(
			Interval head, Interval prev, Interval tobe)
	{
		Interval newFirst = head;
		if (prev != null)
		{
			prev.next = tobe;
		}
		else
		{
			newFirst = tobe;
		}
		return newFirst;
	}

	private Pair<Interval, Interval> createUnhandledLists(
			IntervalPredicate isList1, IntervalPredicate isList2)
	{
		assert isSorted(sortedIntervals) : "intervals list must be sorted";

		// for fixed register list
		Interval list1 = Interval.EndMarker;
		// for any register list
		Interval list2 = Interval.EndMarker;
		Interval list1Prev = null, list2Prev = null;

		// selects all of non-null interval
		List<Interval> temp = Arrays.asList(sortedIntervals).stream()
				.filter((x) -> {return x != null;})
				.collect(Collectors.toList());

		for (Interval i : temp)
		{
			if (isList1.apply(i))
			{
				list1 = addToList(list1, list1Prev, i);
				list1Prev = i;
			}
			else
			{
				assert isList2.apply(i);

				list2 = addToList(list2, list2Prev, i);
				list2Prev = i;
			}
		}

		// append end marker for fixed list and any list
		if (list1Prev != null)
		{
			list1Prev.next = Interval.EndMarker;
		}
		if (list2Prev != null)
		{
			list2Prev.next = Interval.EndMarker;
		}

		assert list1Prev == null || list1Prev.next
				== Interval.EndMarker : "linear list ends must with sentinal";
		assert list2Prev == null || list2Prev.next
				== Interval.EndMarker : "linear list ends must with sentinal";
		return new Pair<>(list1, list2);
	}

	private void allocateRegisters()
	{
		Interval precoloredIntervals;
		Interval notPrecoloredIntervals;

		// creates the list for unhandled interval
		Pair<Interval, Interval> result = createUnhandledLists(
				IS_PRECOLORED_INTERVAL, IS_VARIABLE_INTERVAL);

		// fixed register
		precoloredIntervals = result.first;
		// virtual register waiting to be colored
		notPrecoloredIntervals = result.second;

		// allocate cpu register no fpu
		LinearScanWalker walker = new LinearScanWalker(this,
				precoloredIntervals, notPrecoloredIntervals);

		walker.walk();
		walker.finishAllocation();

		// allocates gpu register as follows if current processor is not supported
		// with SSE/SSE2 instruction set, currently, however, it no supported.
	}


	/**
	 * Destruct phi assignment by inserting move instruction.
	 */
	private void phiDestruction()
	{
		MoveResolver moveResolver = new MoveResolver(this);

		int numBlocks = blockCount();
		BitMap alreadyResolved = new BitMap(numBlocks);

		for (int i = 0; i < numBlocks; i++)
		{
			BasicBlock fromBlock = blockAt(i);
			for (BasicBlock toBlock : fromBlock.getSuccs())
			{
				if (!alreadyResolved.get(toBlock.linearScanNumber))
				{
					alreadyResolved.get(toBlock.linearScanNumber);

					final BitMap liveIn = toBlock.getLIRBlock().livein;

					// filter out non phi instruction and no live in the begin of toBlock
					List<LIRInstruction> allPhis = toBlock.getLIRBlock().lir()
							.instructionsList().
							stream().filter(inst ->
							(inst instanceof LIRPhi)
							&& liveIn.get(operandNumber(inst.result()))
							).collect(Collectors.toList());

					for (LIRInstruction inst : allPhis)
					{
						int num = operandNumber(inst.result());
						assert num>= 0 && num < intervalsSize;

						Interval resultIt = intervals[num];
						LIRValue opd = ((LIRPhi)inst).incomingValueAt(i);
						Interval oprIt = intervals[operandNumber(opd)];

						if (resultIt.location() != oprIt.location())
						{
							// collect all intervals that have been split between
							// fromBlock and toBlock
							// insert new instruction before instruction at position index
							setInsertPosOfPhiResolution(fromBlock, moveResolver);
							moveResolver.addMapping(resultIt, oprIt);

							if (moveResolver.hasMappings())
							{
								moveResolver.resolveAndAppendMoves();
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Sets the insert position when resolving phi destructing.
	 * @param pred  The predecessor where the incoming argument of phi comes.
	 * @param moveResolver  A instance of {@linkplain MoveResolver}.
	 */
	private void setInsertPosOfPhiResolution(BasicBlock pred,
			MoveResolver moveResolver)
	{
		List<LIRInstruction> instructions = pred.getLIRBlock().lir()
				.instructionsList();
		LIRInstruction instr = instructions.get(instructions.size() - 1);
		if (instr instanceof LIRBranch)
		{
			LIRBranch branch = (LIRBranch) instr;
			// insert moves before branch
			assert branch.cond()
					== Condition.TRUE :
					"block does not end with an unconditional jump";
			moveResolver.setInsertPosition(pred.getLIRBlock().lir(),
					instructions.size() - 2);
		}
		else
		{
			moveResolver.setInsertPosition(pred.getLIRBlock().lir(),
					instructions.size() - 1);
		}
	}

	private static final Comparator<Interval> INTERVAL_COMPARATOR
			= new Comparator<Interval>()
	{

		public int compare(Interval a, Interval b)
		{
			if (a != null)
			{
				if (b != null)
				{
					return a.from() - b.from();
				}
				else
				{
					return -1;
				}
			}
			else
			{
				if (b != null)
				{
					return 1;
				}
				else
				{
					return 0;
				}
			}
		}
	};

	private void sortIntervalListAfterAllocation()
	{
		if (firstDerivedIntervalIndex == -1)
		{
			// no intervals have been added during allocation, so sorted list
			// is already up to date
			return;
		}

		Interval[] oldList = sortedIntervals;
		Interval[] newList = Arrays
				.copyOfRange(intervals, firstDerivedIntervalIndex,
						intervalsSize);
		int oldLen = oldList.length;
		int newLen = newList.length;

		// conventional sort-algorithm for new intervals
		Arrays.sort(newList, INTERVAL_COMPARATOR);

		// merge old and new list (both already sorted) into one combined list
		Interval[] combinedList = new Interval[oldLen + newLen];
		int oldIdx = 0;
		int newIdx = 0;

		while (oldIdx + newIdx < combinedList.length)
		{
			if (newIdx >= newLen || (oldIdx < oldLen
					&& oldList[oldIdx].from() <= newList[newIdx].from()))
			{
				combinedList[oldIdx + newIdx] = oldList[oldIdx];
				oldIdx++;
			}
			else
			{
				combinedList[oldIdx + newIdx] = newList[newIdx];
				newIdx++;
			}
		}

		sortedIntervals = combinedList;
	}
	private static final IntervalPredicate mustStoreAtDefinition = new IntervalPredicate()
	{
		@Override public boolean apply(Interval i)
		{
			return i.spillState() == Interval.SpillState.StoreAtDefinition;
		}
	};

	private void eliminateSpillMove()
	{
		// collect all intervals that must be stored after their definition.
		// the list is sorted by Interval.spillDefinitionPos
		Interval interval = createUnhandledLists(mustStoreAtDefinition, null).first;

		LIRInsertionBuffer insertionBuffer = new LIRInsertionBuffer();
		int numBlocks = blockCount();
		for (int i = 0; i < numBlocks; i++)
		{
			BasicBlock block = blockAt(i);
			List<LIRInstruction> instructions = block.getLIRBlock().lir()
					.instructionsList();
			int numInst = instructions.size();
			boolean hasNew = false;

			// iterate all instructions of the block. skip the first because it
			// is always a label
			for (int j = 1; j < numInst; j++)
			{
				LIRInstruction op = instructions.get(j);
				int opId = op.id;

				if (opId == -1)
				{
					LIRValue resultOperand = op.result();
					// remove move from register to stack if the stack slot is
					// guaranteed to be correct.
					// only moves that have been inserted by LinearScan can be
					// removed.
					assert op.opcode
							== LIROpcode.Move : "only moves can have a opId of -1";
					assert resultOperand
							.isVariable() : "LinearScan inserts only moves to variables";

					Interval curInterval = intervalFor(resultOperand);

					if (!curInterval.location().isRegister() && curInterval
							.alwaysInMemory())
					{
						// move target is a stack slot that is always correct,
						// so eliminate instruction
						
						instructions.set(j,	null); // null-instructions are deleted by assignRegNum
					}
				}
				else
				{
					// insert move from register to stack just after the beginning
					// of the interval
					assert interval == Interval.EndMarker
							|| interval.spillDefinitionPos()
							>= opId : "invalid order";
					assert interval == Interval.EndMarker || 
							(interval.spillState() == Interval.SpillState.StoreAtDefinition) : "invalid interval";

					while (interval != Interval.EndMarker
							&& interval.spillDefinitionPos() == opId)
					{
						if (!hasNew)
						{
							// prepare insertion buffer (appended when all instructions
							// of the block are processed)
							insertionBuffer.init(block.getLIRBlock().lir());
							hasNew = true;
						}

						LIRValue fromLocation = interval.location();
						LIRValue toLocation = canonicalSpillOpr(interval);

						assert fromLocation.isRegister() :
								"from operand must be a register but is: "
										+ fromLocation + " toLocation="
										+ toLocation + " spillState=" + interval
										.spillState();
						assert toLocation
								.isStackSlot() : "to operand must be a stack slot";

						insertionBuffer.move(j, fromLocation, toLocation);

						interval = interval.next;
					}
				}
			} // end of instruction iteration

			if (hasNew)
			{
				block.getLIRBlock().lir().append(insertionBuffer);
			}
		} // end of block iteration

		assert interval == Interval.EndMarker : "missed an interval";
	}

	private LIRValue canonicalSpillOpr(Interval interval)
	{
		assert interval.spillSlot() != null : "canonical spill slot not set";
		return interval.spillSlot();
	}

	/**
	 * Assigns the allocated location for an LIR instruction operand back into
	 * the instruction.
	 *
	 * @param operand an LIR instruction operand
	 * @param opId    the id of the LIR instruction using {@code operand}
	 * @param mode    the usage mode for {@code operand} by the instruction
	 * @return the location assigned for the operand
	 */
	private LIRValue colorLirOperand(LIRVariable operand, int opId,
			LIRInstruction.OperandMode mode)
	{
		Interval interval = intervalFor(operand);
		assert interval != null : "interval must exist";

		if (opId != -1)
		{
			{
				BasicBlock block = blockForId(opId);
				if (block.getNumOfSuccs() <= 1 && opId == block
						.lastLIRInstructionId())
				{
					// check if spill moves could have been appended at the end
					// of this block, but
					// before the branch instruction. So the split child information
					// for this branch would
					// be incorrect.
					LIRInstruction instr = block.getLIRBlock().lir()
							.instructionsList()
							.get(block.getLIRBlock().lir().instructionsList()
									.size() - 1);
					if (instr instanceof LIRBranch)
					{
						LIRBranch branch = (LIRBranch) instr;
						if (block.getLIRBlock().liveout
								.get(operandNumber(operand)))
						{
							assert branch.cond()
									== Condition.TRUE :
									"block does not end with an unconditional jump";
							throw new CiBailout(
									"can't get split child for the last branch of a block because the information would be incorrect (moves are inserted before the branch in resolveDataFlow)");
						}
					}
				}
			}
		}

		return interval.location();
	}

	private void assignLocations(List<LIRInstruction> instructions,
			IntervalWalker iw)
	{
		int numInst = instructions.size();
		boolean hasDead = false;

		for (int j = 0; j < numInst; j++)
		{
			LIRInstruction op = instructions.get(j);
			if (op == null)
			{ // this can happen when spill-moves are removed in eliminateSpillMoves
				hasDead = true;
				continue;
			}

			// iterate all modes of the visitor and process all virtual operands
			for (LIRInstruction.OperandMode mode : LIRInstruction.OPERAND_MODES)
			{
				int n = op.operandCount(mode);
				for (int k = 0; k < n; k++)
				{
					LIRValue operand = op.operandAt(mode, k);
					if (operand.isVariable())
					{
						op.setOperandAt(mode, k,
								colorLirOperand((LIRVariable) operand, op.id,
										mode));
					}
				}
			}

			// make sure we haven't made the op invalid.
			assert op.verify();

			// remove useless moves
			if (op.opcode == LIROpcode.Move)
			{
				LIRValue src = op.operand(0);
				LIRValue dst = op.result();
				if (dst == src || src.equals(dst))
				{
					instructions.set(j, null);
					hasDead = true;
				}
			}
		}

		if (hasDead)
		{
			// iterate all instructions of the block and remove all null-VALUES.

			// the insertPoint records the index into null value
			int insertPoint = 0;
			for (int j = 0; j < numInst; j++)
			{
				LIRInstruction op = instructions.get(j);
				if (op != null)
				{
					if (insertPoint != j)
					{
						instructions.set(insertPoint, op);
					}
					insertPoint++;
				}
			}
			Util.truncate(instructions, insertPoint);
		}
	}

	static final IntervalPredicate IS_OOP_INTERVAL = new IntervalPredicate()
	{
		@Override public boolean apply(Interval i)
		{
			return !i.operand.isRegister() && i.kind() == LIRKind.Object;
		}
	};

	IntervalWalker initComputeOopMaps()
	{
		// setup lists of potential oops for walking
		Interval oopIntervals;
		Interval nonOopIntervals;

		oopIntervals = createUnhandledLists(IS_OOP_INTERVAL, null).first;

		// intervals that have no oops inside need not to be processed.
		// to ensure a walking until the last instruction id, add a dummy interval
		// with a high operation id
		nonOopIntervals = new Interval(LIRValue.IllegalValue, -1);
		nonOopIntervals.addRange(Integer.MAX_VALUE - 2, Integer.MAX_VALUE - 1);

		return new IntervalWalker(this, oopIntervals, nonOopIntervals);
	}

	private void assignLocations()
	{
		IntervalWalker iw = initComputeOopMaps();
		for (BasicBlock block : sortedBlocks)
		{
			assignLocations(block.getLIRBlock().lir().instructionsList(), iw);
		}
	}

	private void printIntervals(String label)
	{
		int i;
		TTY.println();
		TTY.println(label);

		for (Interval interval : intervals)
		{
			if (interval != null)
			{
				TTY.out().println(interval.logString(this));
			}
		}

		TTY.println();
		TTY.println("--- Basic Blocks ---");
		for (i = 0; i < blockCount(); i++)
		{
			BasicBlock block = blockAt(i);
			TTY.print("B%d [%d, %d, %d, %d] ", block.getID(),
					block.getLIRBlock().firstLIRInstructionID, block.getID(),
					block.getLIRBlock().lastLIRInstructionID, block.loopIndex,
					block.loopDepth);
		}
		TTY.println();
		TTY.println();
	}

	public boolean isBlockBegin(int opID)
	{
		return opID == 0 || blockForId(opID) != blockForId(opID - 1);
	}

	public void copyRegisterFlags(Interval from, Interval to)
	{
		if (operands.mustBeByteRegister(from.operand))
		{
			operands.setMustBeByteRegister((LIRVariable) to.operand);
		}
		// Note: do not copy the mustStartInMemory flag because it is not necessary for child
		// intervals (only the very beginning of the interval must be in memory)
	}

	/**
	 * the entry for performing linear scanning register allocation.
	 */
	public void allocate()
	{
		numberInstruction();

		computeLocalLiveSets();
		computeGlobalLiveSet();

		buildIntervals();

		printIntervals("Before register allocation");
		sortIntervalListBeforeAllocation();

		allocateRegisters();

		// phi function destruction before finishing allocation
		phiDestruction();

		// fill in number of spill spot into stack frame
		frameMap.finalizeFrame(maxSpills);

		sortIntervalListAfterAllocation();

		// make local optimization
		eliminateSpillMove();
		assignLocations();

		EdgeMoveOptimizer.optimize(m.linearScanOrder());
	}
}
