package lir.alloc;

import driver.Backend;
import exception.CiBailout;
import hir.BasicBlock;
import hir.Method;
import lir.*;
import lir.alloc.Interval.RegisterPriority;
import lir.backend.RegisterConfig;
import lir.ci.*;
import utils.BitMap;
import utils.BitMap2D;
import utils.TTY;

import java.util.Arrays;
import java.util.List;

import static utils.Util.isEven;
import static utils.Util.isOdd;

/**
 * An implementation of the linear scan register allocator algorithm described
 * in <a href="http://doi.acm.org/10.1145/1064979.1064998">"Optimized Interval
 * Splitting in a Linear Scan Register Allocator"</a> by Christian Wimmer and
 * Hanspeter Moessenboeck.
 */
public final class LinearScan
{
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

	static final IntervalPredicate IS_PRECOLORED_INTERVAL = new IntervalPredicate()
	{
		@Override boolean apply(Interval i)
		{
			return i.operand.isRegister();
		}
	};

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
	private StackSlot allocateSpillSlot(LIRKind kind)
	{
		StackSlot slot;
		// it means that the number of occupied stack slot of
		// specified kind is 2
		if (numOfSpillSlots(kind) == 2)
		{
			if (isOdd(maxSpills))
			{
				// alignment of double slot values
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

	private void assignSpillSlot(Interval i)
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
	private int blockCount()
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
	private BasicBlock blockAt(int index)
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
	private LIRInstruction instructionForId(int opID)
	{
		assert isEven(opID) : "opID not even";

		assert opID >= 0 && opID < opIdToInstructionMap.length;
		LIRInstruction instr = opIdToInstructionMap[opIDToIndex(opID)];
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
		int numInstrs = 0;
		for (int i = 0; i < numBlocks; i++)
		{
			numInstrs += blockAt(i).getLIRBlock().lir().instructionsList()
					.size();
		}

		opIdToInstructionMap = new LIRInstruction[numInstrs];
		opIdToBlockMap = new BasicBlock[numBlocks];

		int opID = 0, index = 0;
		for (int i = 0; i < numBlocks; i++)
		{
			BasicBlock block = blockAt(i);
			block.setFirstLIRInstruction(opID);
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
				opID += 2;  // numbering the LIR instruction by 2 according to Christian Wimmer
			}
			block.setLastLIRInstructionId(opID - 2);
		}
		assert index == numInstrs : "must match";
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
				}// end of handling operand

				// iterate temp operands of specified instruction
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

	private Interval addUse(LIRValue operand, int from, int to,
			RegisterPriority priority, LIRKind kind)
	{
		if (!isProcessed(operand))
		{
			return null;
		}
		if (kind == null)
		{
			kind = operand.kind.stackKind();
		}
		Interval interval = intervalFor(operand);

		// yet there no interval assigned to this operand
		if (interval == null)
		{
			interval = createInterval(operand);
		}
		if (kind != LIRKind.Illegal)
		{
			interval.setKind(kind);
		}
		if (operand.isVariable() && gen.operands
				.mustStayInMemory((LIRVariable) operand))
		{
			interval.addRange(from, maxSpills);
		}
		else
		{
			interval.addRange(from, to);
		}
		interval.addUsePos(to, priority);
		return interval;
	}

	private boolean isIntervalInLoop(int interval, int loopIndex)
	{
		return intervalInLoop.at(interval, loopIndex);
	}

	/**
	 * adds temporary lifetime interval for caller-saved register when the flag of
	 * instruction's {@linkplain LIRInstruction#hasCall hasCall} is seted.
	 *
	 * @param operand
	 * @param tempPos
	 * @param priority
	 * @param kind
	 */
	private void addTemp(LIRValue operand, int tempPos,
			RegisterPriority priority, LIRKind kind)
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
		interval.addRange(tempPos, tempPos + 1);
		interval.addUsePos(tempPos, priority);
	}

	/**
	 * Phase 5: actual register allocation.
	 *
	 * @param op
	 */
	private void pdAddTemps(LIRInstruction op)
	{
		// TODO Platform dependent!
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
						addTemp(r.asValue(), opId, RegisterPriority.None,
								LIRKind.Illegal);
					}
				}
				break;
			}
		}
	}

	/**
	 * Gets the highest instruction id allocated by this object.
	 */
	private int maxOpId()
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
	private BasicBlock blockForId(int opid)
	{
		assert opIdToBlockMap.length > 0 && opid >= 0
				&& opid <= maxOpId() + 1 : "opID out of range";
		return opIdToBlockMap[opIDToIndex(opid)];
	}

	/**
	 * Determines the register priority for an instruction's output/result operand.
	 */
	private RegisterPriority registerPriorityOfOutputOperand(
			LIRInstruction instr, LIRValue operand)
	{
		if (instr.opcode == LIROpcode.Move)
		{
			LIROp1 move = (LIROp1) instr;
			LIRValue res = move.result();
			boolean resultInMemory = res.isVariable() && operands
					.mustStartInMemory((LIRVariable) res);

			if (resultInMemory)
			{
				// Begin of an interval with mustStartInMemory set
				// This interval will always get a stack slot first, so return noUse.
				return RegisterPriority.None;
			}
			else if (move.operand().isStackSlot())
			{
				// function argument (condition must be equal to handleFunctionArgument)
				return RegisterPriority.None;
			}
			else if (move.operand().isVariableOrRegister() && move.result()
					.isVariableOrRegister())
			{
				// move from register to register
				return RegisterPriority.ShouldHavaRegister;
			}
		}

		if (operand.isVariable() && operands
				.mustStartInMemory((LIRVariable) operand))
		{
			// result is a stack-slot, so prevent immediate reloading
			return RegisterPriority.None;
		}

		// all other operands require a register
		return RegisterPriority.MustHaveRegister;
	}

	/**
	 * Eliminates moves from register to stack if the stack slot is known to be correct.
	 */
	private void changeSpillDefinitionPos(Interval interval, int defPos)
	{
		assert interval
				.isSplitParent() : "can only be called for split parents";
		switch (interval.spillState())
		{
			case NoDefinitionFound:
				assert interval.spillDefinitionPos()
						== -1 : "must no be set before";
				interval.setSpillDefinitionPos(defPos);
				interval.setSpillState(Interval.SpillState.NoSpillStore);
				break;
			case NoSpillStore:
				assert defPos <= interval
						.spillDefinitionPos() : "positions are processed in reverse order when intervals are created";
				if (defPos < interval.spillDefinitionPos() - 2)
				{
					interval.setSpillState(Interval.SpillState.NoOptimization);
				}
				else
				{
					// two consecutive definitions (because of two-operand LIR form)
					assert blockForId(defPos) == blockForId(
							interval.spillDefinitionPos()) : "block must be equal";
				}
				break;
			case NoOptimization:
				// nothing to do
				break;
			default:
				throw new CiBailout("other states are allowed at this point");
		}
	}

	/**
	 * called during register allocation
	 */
	private void changeSpillState(Interval interval, int spillPos)
	{
		switch (interval.spillState())
		{
			case NoSpillStore:
			{
				int defLoopDepth = blockForId(interval.spillDefinitionPos())
						.loopDepth;
				int spillLoopDepth = blockForId(spillPos).loopDepth;

				if (defLoopDepth < spillLoopDepth)
				{
					// the loop depth of the spilling position is higher then the loop depth
					// at the definition of the interval . move write to memory out of loop
					// by storing at definitin of the interval
					interval.setSpillState(Interval.SpillState.StoreAtDefinition);
				}
				else
				{
					// the interval is currently spilled only once, so for now there is no
					// reason to store the interval at the definition
					interval.setSpillState(Interval.SpillState.OneSpillStore);
				}
				break;
			}

			case OneSpillStore:
			{
				// the interval is spilled more then once, so it is better to store it to
				// memory at the definition
				interval.setSpillState(Interval.SpillState.StoreAtDefinition);
				break;
			}

			case StoreAtDefinition:
			case StartInMemory:
			case NoOptimization:
			case NoDefinitionFound:
				// nothing to do
				break;

			default:
				throw new CiBailout("other states not allowed at this time");
		}
	}

	private void addDef(LIRValue operand, int defPos, RegisterPriority priority,
			LIRKind kind)
	{
		if (!isProcessed(operand))
			return;

		TTY.println(" def %s defPos %d (%s)", operand, defPos, priority.name());
		Interval interval = intervalFor(operand);
		if (interval != null)
		{
			if (kind != LIRKind.Illegal)
			{
				interval.setKind(kind);
			}

			Range r = interval.first();
			if (r.from <= defPos)
			{
				// update the starting point (when a range is first created for a
				// use , its start is the beginning of the current block until
				// a def in encountered.)
				r.from = defPos;
				interval.addUsePos(defPos, priority);
			}
			else
			{
				// dead value - make vacuous interval
				interval.addRange(defPos, defPos + 1);
				interval.addUsePos(defPos, priority);
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
			interval.addUsePos(defPos, priority);

			TTY.println("Warning: def of operand %s at %d occurs without use",
					operand, defPos);
		}

		changeSpillDefinitionPos(interval, defPos);
		if (priority == RegisterPriority.None && interval.spillState().ordinal()
				<= Interval.SpillState.StartInMemory.ordinal())
		{
			// detection of function parameters and round fp-results
			interval.setSpillState(Interval.SpillState.StartInMemory);
		}
	}

	/**
	 * Determines the priority which with an instruction's input operand will be allocated a register.
	 */
	private RegisterPriority registerPriorityOfInputOperand(
			LIRInstruction instr, LIRValue operand)
	{
		if (instr.opcode == LIROpcode.Move)
		{
			LIROp1 move = (LIROp1) instr;
			LIRValue res = move.result();
			boolean resultInMemory = res.isVariable() && operands
					.mustStartInMemory((LIRVariable) res);

			if (resultInMemory)
			{
				// Move to an interval with mustStartInMemory set.
				// To avoid moves from stack to stack (not allowed) force the
				// input operand to a register
				return RegisterPriority.MustHaveRegister;
			}
			else if (move.operand().isVariableOrRegister() && move.result()
					.isVariableOrRegister())
			{
				// move from register to register
				return RegisterPriority.ShouldHavaRegister;
			}
		}

		if (backend.targetMachine.arch.isX86())
		{
			// handle stack operands using conditional move
			// to avoid stop pipeline
			if (instr.opcode == LIROpcode.Cmove)
			{
				assert instr.result().isVariableOrRegister();
				return RegisterPriority.ShouldHavaRegister;
			}

			// optimizations for second input operand of arithmetic operations on Interl
			// this operand is allowed to be on the stack in some cases
			LIRKind kind = operand.kind.stackKind();
			if (kind == LIRKind.Float || kind == LIRKind.Double)
			{
				// SSE float instruction (LIRKind.Double only supported with SSE2)
				switch (instr.opcode)
				{
					case Cmp:
					case Add:
					case Sub:
					case Mul:
					case Div:
					{
						LIROp2 op2 = (LIROp2) instr;
						if (op2.operand1() != op2.operand2()
								&& op2.operand2() == operand)
						{
							assert (op2.result().isVariableOrRegister()
									|| instr.opcode == LIROpcode.Cmp) && op2
									.operand1()
									.isVariableOrRegister() : "cannot mark second operand as stack if others are not in register";
							return RegisterPriority.ShouldHavaRegister;
						}
					}
				}
			}
			else if (kind == LIRKind.Long)
			{
				// integer instruction (note that: integer operand always be in register)
				switch (instr.opcode)
				{
					case Cmp:
					case Add:
					case Sub:
					case LogicAnd:
					case LogicOr:
					case LogicXor:
					{
						LIROp2 op2 = (LIROp2) instr;
						if (op2.operand1() != op2.operand2()
								&& op2.operand2() == operand)
						{
							assert (op2.result().isVariableOrRegister()
									|| instr.opcode == LIROpcode.Cmp) && op2
									.operand1()
									.isVariableOrRegister() : "cannot mark second operand as stack if others are not in register";
							return RegisterPriority.ShouldHavaRegister;
						}
					}
				}
			}
		}// end of x86

		// all other operands require a register
		return RegisterPriority.MustHaveRegister;
	}

	/**
	 * Optimizes moves related to incoming stack based arguments.
	 * The interval for the destination of such moves is assigned
	 * the stack slot (which is in the caller's frame) as its
	 * spill slot.
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

	private void addRegisterHints(LIRInstruction instr)
	{
		switch (instr.opcode)
		{
			case Move:
			case Convert:
			{
				LIROp1 move = (LIROp1) instr;
				LIRValue moveFrom = move.operand();
				LIRValue moveTo = move.result();

				if (moveTo.isVariableOrRegister() && moveFrom
						.isVariableOrRegister())
				{
					Interval from = intervalFor(moveFrom);
					Interval to = intervalFor(moveTo);
					if (from != null && to != null)
					{
						to.setLocationHint(from);
						TTY.println(
								"operation at opId %d: added hint from interval %d to %d",
								move.id, from.operandNumber, to.operandNumber);
					}
				}
				break;
			}
			case Cmove:
			{
				LIROp2 cmove = (LIROp2) instr;
				LIRValue moveFrom = cmove.operand1();
				LIRValue moveTo = cmove.result();

				if (moveTo.isVariableOrRegister() && moveFrom
						.isVariableOrRegister())
				{
					Interval from = intervalFor(moveFrom);
					Interval to = intervalFor(moveTo);
					if (from != null && to != null)
					{
						to.setLocationHint(from);
						TTY.println(
								"operation at opId %d: added hint from interval %d to %d",
								cmove.id, from.operandNumber, to.operandNumber);
					}
				}
				break;
			}
		}
	}

	/**
	 * Builds interval list for every virtual register or so called variable.
	 */
	private void buildIntervalList()
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
				assert liveOut.get(operandNum) : "this operand must live out";
				LIRValue operand = operands.operandFor(operandNum);

				// print debug information here in the future

				addUse(operand, blockFrom, blockTo + 2, RegisterPriority.None,
						LIRKind.Illegal);

				// add special use positions for loop-end blocks when the interval
				// is used anywhere inside loop. It's possible that the block was
				// part of a non-natural loop, so it might have an invalid loop
				// index.
				if (block.checkBlockFlags(BasicBlock.BlockFlag.LinearScanLoopEnd)
						&& block.loopIndex != -1 && isIntervalInLoop(operandNum,
						block.loopIndex))
				{
					intervalFor(operand).addUsePos(blockTo + 1,
							RegisterPriority.LiveAtLoopEnd);
				}
			}
			// iterate all instructions of the block in reverse order.
			// skip the first instruction because it is always a label
			// definitions of intervals are processed before uses
			assert !instructions.get(0)
					.hasOperands() : "the first instruction of block must be label";
			for (int j = instructions.size() - 1; j >= 1; j--)
			{
				LIRInstruction opr = instructions.get(j);
				final int opID = opr.id;

				// add temp range for each register if operand destroy caller-saved registers
				if (opr.hasCall)
				{
					for (LIRRegister r : callerRegisters)
					{
						if (attributes(r).isAllocatable)
							addTemp(r.asValue(), opID, RegisterPriority.None,
									LIRKind.Illegal);
					}
				}

				// add any platform dependent temps that can not implemented in x86
				pdAddTemps(opr);

				// visit definition (output and temp operands)
				int n = opr.operandCount(LIRInstruction.OperandMode.Output);
				for (int k = 0; k < n; k++)
				{
					LIRValue operand = opr
							.operandAt(LIRInstruction.OperandMode.Output, k);
					assert operand.isVariableOrRegister();
					addDef(operand, opID,
							registerPriorityOfOutputOperand(opr, operand),
							operand.kind.stackKind());
				}

				// visits temporary operands
				n = opr.operandCount(LIRInstruction.OperandMode.Temp);
				for (int k = 0; k < n; k++)
				{
					LIRValue tmp = opr
							.operandAt(LIRInstruction.OperandMode.Temp, k);
					assert tmp.isVariableOrRegister();
					addTemp(tmp, opID, RegisterPriority.MustHaveRegister,
							tmp.kind.stackKind());
				}

				// visits uses (input operands)
				n = opr.operandCount(LIRInstruction.OperandMode.Input);
				for (int k = 0; k < n; k++)
				{
					LIRValue input = opr
							.operandAt(LIRInstruction.OperandMode.Input, k);
					assert input.isVariableOrRegister();
					RegisterPriority priority = registerPriorityOfInputOperand(
							opr, input);
					Interval interval = addUse(input, blockFrom, opID, priority,
							null);
					if (interval != null)
					{
						Range first = interval.first();
						if (first.to == opID)
							first.to++;
					}
				}

				// handles function arguments
				handleFunctionArguments(opr);
				addRegisterHints(opr);
			}// end of instruction iteration

			// make sure that no spil store optimization is applied for phi
			// instructions

		} // end of block iteration

		// add the range [0, 1] to all fixed intervals
		// the register allocator need not handler unhandled fixed intervals
		for (Interval interval : intervals)
		{
			if (interval != null && interval.operand.isRegister())
			{
				interval.addRange(0, 1);
			}
		}
	}

	private void sortIntervalListBeforeAllocation()
	{

	}

	private void allocateRegisters()
	{

	}

	private void resolveDataFlow()
	{

	}

	private void sortIntervalListAfterAllocation()
	{

	}

	private void eliminateSpillMove()
	{

	}

	private void assignLocations()
	{

	}

	/**
	 * the main entry for linear scanning register allocation.
	 */
	public void allocate()
	{
		numberInstruction();
		computeLocalLiveSets();
		computeGlobalLiveSet();

		buildIntervalList();
		sortIntervalListBeforeAllocation();

		allocateRegisters();

		resolveDataFlow();
		// fill in number of spill spot into stack frame
		frameMap.finalizeFrame(maxSpills);

		sortIntervalListAfterAllocation();

		// make local optimization
		eliminateSpillMove();
		assignLocations();

		EdgeMoveOptimizer.optimize(m.linearScanOrder());
	}

}
