package lir.alloc;

import driver.Backend;
import exception.CiBailout;
import hir.BasicBlock;
import hir.Condition;
import hir.Method;
import lir.*;
import lir.alloc.Interval.RegisterPriority;
import lir.backend.RegisterConfig;
import lir.ci.*;
import utils.*;

import java.util.*;
import java.util.stream.Collectors;

import static lir.alloc.Interval.SpillState.StoreAtDefinition;
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

	final lir.linearScan.OperandPool operands;

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
				opID += 2;  // numbering the LIR instruction by 2 according to Christian Wimmer
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
	 * Creates a interval for the input operand of {@linkplain LIRInstruction}.
	 * Input operands uses LIRValues that calculated before the start of current
	 * operation, but the actual position is not known yet. So a range from the
	 * start of the current block to the position of LIRInstruction is added into
	 * range list. It may be shortened later when output operands are processed.
	 *
	 * @param operand
	 * @param from
	 * @param to
	 * @param priority
	 * @param kind
	 * @return
	 */
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

		// whether this operand must stay in memory and never reloaded into register
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
		// its length of range is seted with one
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
						addTemp(r.asValue(), opId, RegisterPriority.None,
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
				return RegisterPriority.ShouldHaveRegister;
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
	 * Eliminates moves from register to stack if the stack slot is known to be
	 * correct. This is a heuristic optimization strategy for taking positive effect
	 * on the quality of generated code.
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
						.spillDefinitionPos() :
						"positions are processed in reverse order when intervals are created";
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
	void changeSpillState(Interval interval, int spillPos)
	{
		switch (interval.spillState())
		{
			case NoSpillStore:
			{
				int defLoopDepth = blockForId(
						interval.spillDefinitionPos()).loopDepth;
				int spillLoopDepth = blockForId(spillPos).loopDepth;

				if (defLoopDepth < spillLoopDepth)
				{
					// the loop depth of the spilling position is higher then the loop depth
					// at the definition of the interval . move write to memory out of loop
					// by storing at definitin of the interval
					interval.setSpillState(
							Interval.SpillState.StoreAtDefinition);
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

	/**
	 * Creates a interval for the result operand of {@linkplain LIRInstruction}.
	 * Output operands of the {@linkplain LIRInstruction} shorten the first range
	 * of the interval of given operand. The definition overwrites any previous
	 * value of the operand, so the operand cnanot live immediately before this
	 * operation.
	 *
	 * @param operand  The result operand.
	 * @param defPos   The id of specified LIRInstruction that defines operand.
	 * @param priority For register spill.
	 * @param kind
	 */
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
				// dead value - make meaningless interval
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
	 * Determines the priority which with an instruction's input operand will be
	 * allocated a register.
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
				return RegisterPriority.ShouldHaveRegister;
			}
		}

		if (backend.targetMachine.arch.isX86())
		{
			// handle stack operands using conditional move
			// to avoid stop pipeline
			if (instr.opcode == LIROpcode.Cmove)
			{
				assert instr.result().isVariableOrRegister();
				return RegisterPriority.ShouldHaveRegister;
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
									.isVariableOrRegister() :
									"cannot mark second operand as stack if others are not in register";
							return RegisterPriority.ShouldHaveRegister;
						}
					}
					default:
						break;
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
									.isVariableOrRegister() :
									"cannot mark second operand as stack if others are not in register";
							return RegisterPriority.ShouldHaveRegister;
						}
					}
					default:
						break;
				}
			}
		}// end of x86

		// all other operands require a register
		return RegisterPriority.MustHaveRegister;
	}

	/**
	 * Optimizes moves related to incoming stack based arguments.
	 * The interval for the destination of such moves is assigned
	 * the stack slot (which is in the caller's frame) as its spill slot.
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
	 * The mostly frequently occurring instruction are moves from one virtual
	 * register to other. When two intervals are connected only by a move instruction
	 * , the interval for the move target stores the source of the move as its
	 * register hint. If possible, the target then gets the same register assigned
	 * as the source.
	 * @param instr
	 */
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
			// for intel architecture since i486
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
			default:
				break;
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

				// the entire range of the block is added--this is necessary if
				// the operand does not occur in any operation of the block.
				// if the operand is defined in the block, than the range would
				// be shortened to the definition position later.
				addUse(operand, blockFrom, blockTo + 2, RegisterPriority.None,
						LIRKind.Illegal);

				// add special use positions for loop-end blocks when the interval
				// is used anywhere inside loop. It's possible that the block was
				// part of a non-natural loop, so it might have an invalid loop
				// index.
				if (block
						.checkBlockFlags(BasicBlock.BlockFlag.LinearScanLoopEnd)
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

				// visits temporary operands equal to output operands
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
		// count the numbers of element is not null in intervals array
		int sortedLen = (int) Arrays.asList(intervals).stream().
				filter((i) -> { return i != null;}).count();

		Interval[] sortedList = new Interval[sortedLen];
		int sortedIdx = 0;
		int sortedFromMax = -1;
		List<Interval> temp = Arrays.asList(intervals).stream().
				filter((i) -> {return i != null;}).collect(Collectors.toList());

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

	private Interval addToList(Interval head, Interval prev, Interval tobe)
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
	 * destructs phi function by inserting move instruction.
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

	/**
	 * When an interval is split, a move instruction is inserted from the old to
	 * the new location at the split position.
	 */
	private void resolveDataFlow()
	{
		int numBlocks = blockCount();
		MoveResolver moveResolver = new MoveResolver(this);
		BitMap blockCompleted = new BitMap(numBlocks);
		BitMap alreadyResolved = new BitMap(numBlocks);

		int i;
		for (i = 0; i < numBlocks; i++)
		{
			BasicBlock block = blockAt(i);

			// check if block has only one predecessor and only one successor
			if (block.getNumOfPreds() == 1 && block.getNumOfSuccs() == 1)
			{
				List<LIRInstruction> instructions = block.getLIRBlock().lir()
						.instructionsList();
				assert instructions.get(0).opcode
						== LIROpcode.Label :
						"block must start with label";
				assert instructions.get(instructions.size() - 1).opcode
						== LIROpcode.Branch :
						"block with successors must end with branch";
				assert ((LIRBranch) instructions.get(instructions.size() - 1))
						.cond()
						== Condition.TRUE :
						"block with successor must end with unconditional branch";

				// check if block is empty (only label and branch)
				if (instructions.size() == 2)
				{
					BasicBlock pred = block.predAt(0);
					BasicBlock sux = block.succAt(0);

					// prevent optimization of two consecutive blocks
					if (!blockCompleted.get(pred.linearScanNumber)
							&& !blockCompleted.get(sux.linearScanNumber))
					{
						blockCompleted.set(block.linearScanNumber);

						// directly resolve between pred and sux (without looking
						// at the empty block between)
						resolveCollectMappings(pred, sux, moveResolver);
						if (moveResolver.hasMappings())
						{
							moveResolver.setInsertPosition(
									block.getLIRBlock().lir(), 0);
							moveResolver.resolveAndAppendMoves();
						}
					}
				}
			}
		}

		for (i = 0; i < numBlocks; i++)
		{
			if (!blockCompleted.get(i))
			{
				BasicBlock fromBlock = blockAt(i);
				alreadyResolved.setFrom(blockCompleted);

				int numSux = fromBlock.getNumOfSuccs();
				for (int s = 0; s < numSux; s++)
				{
					BasicBlock toBlock = fromBlock.succAt(s);

					// check for duplicate edges between the same blocks
					// (can happen with switch blocks)
					if (!alreadyResolved.get(toBlock.linearScanNumber))
					{
						alreadyResolved.set(toBlock.linearScanNumber);

						// collect all intervals that have been split between
						// fromBlock and toBlock
						resolveCollectMappings(fromBlock, toBlock,
								moveResolver);
						if (moveResolver.hasMappings())
						{
							resolveFindInsertPos(fromBlock, toBlock,
									moveResolver);
							moveResolver.resolveAndAppendMoves();
						}
					}
				}
			}
		}
	}

	Interval intervalAtBlockBegin(BasicBlock block, LIRValue operand)
	{
		assert operand.isVariable() : "register number out of bounds";
		assert intervalFor(operand) != null : "no interval found";

		return splitChildAtOpId(intervalFor(operand),
				block.firstLIRInstructionId(),
				LIRInstruction.OperandMode.Output);
	}

	Interval intervalAtBlockEnd(BasicBlock block, LIRValue operand)
	{
		assert operand.isVariable() : "register number out of bounds";
		assert intervalFor(operand) != null : "no interval found";

		return splitChildAtOpId(intervalFor(operand),
				block.lastLIRInstructionId() + 1,
				LIRInstruction.OperandMode.Output);
	}

	private void resolveCollectMappings(BasicBlock from, BasicBlock to,
			MoveResolver moveResolver)
	{
		assert moveResolver.checkEmpty();

		int numOperands = operands.size();
		BitMap liveAtEdge = to.getLIRBlock().livein;

		// visit all variables for which the liveAtEdge bit is set
		for (int operandNum = liveAtEdge.nextSetBit(0);
		     operandNum >= 0; operandNum = liveAtEdge
				.nextSetBit(operandNum + 1))
		{
			assert operandNum
					< numOperands : "live information set for not exisiting interval";
			assert from.getLIRBlock().liveout.get(operandNum) && to
					.getLIRBlock().livein
					.get(operandNum) : "interval not live at this edge";

			LIRValue liveOperand = operands.operandFor(operandNum);

			Interval toInterval = intervalAtBlockBegin(to, liveOperand);
			Interval fromInterval = intervalAtBlockEnd(from, liveOperand);

			if (fromInterval != toInterval && (fromInterval.location()
					!= toInterval.location()))
			{
				// need to insert move instruction
				moveResolver.addMapping(fromInterval, toInterval);
			}
		}
	}

	private void resolveFindInsertPos(BasicBlock from, BasicBlock to,
			MoveResolver moveResolver)
	{
		if (from.getNumOfSuccs() <= 1)
		{
			List<LIRInstruction> instructions = from.getLIRBlock().lir()
					.instructionsList();
			LIRInstruction instr = instructions.get(instructions.size() - 1);
			if (instr instanceof LIRBranch)
			{
				LIRBranch branch = (LIRBranch) instr;
				// insert moves before branch
				assert branch.cond()
						== Condition.TRUE :
						"block does not end with an unconditional jump";
				moveResolver.setInsertPosition(from.getLIRBlock().lir(),
						instructions.size() - 2);
			}
			else
			{
				moveResolver.setInsertPosition(from.getLIRBlock().lir(),
						instructions.size() - 1);
			}

		}
		else
		{
			moveResolver.setInsertPosition(to.getLIRBlock().lir(), 0);
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
			return i.isSplitParent() && i.spillState() == StoreAtDefinition;
		}
	};

	private void eliminateSpillMove()
	{
		// collect all intervals that must be stored after their definition.
		// the list is sorted by Interval.spillDefinitionPos
		Interval interval;
		interval = createUnhandledLists(mustStoreAtDefinition, null).first;

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
						instructions.set(j,
								null); // null-instructions are deleted by assignRegNum
					}

				}
				else
				{
					// insert move from register to stack just after the beginning
					// of the interval
					assert interval == Interval.EndMarker
							|| interval.spillDefinitionPos()
							>= opId : "invalid order";
					assert interval == Interval.EndMarker || (
							interval.isSplitParent() && interval.spillState()
									== StoreAtDefinition) : "invalid interval";

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

			// operands are not changed when an interval is split during allocation,
			// so search the right interval here
			interval = splitChildAtOpId(interval, opId, mode);
		}

		return interval.location();
	}

	// * Phase 6: resolve data flow
	// (insert moves at edges between blocks if intervals have been split)

	// wrapper for Interval.splitChildAtOpId that performs a bailout in product mode
	// instead of returning null
	Interval splitChildAtOpId(Interval interval, int opId,
			LIRInstruction.OperandMode mode)
	{
		Interval result = interval.getSplitChildAtOpId(opId, mode, this);

		if (result != null)
		{
			TTY.println(
					"Split child at pos " + opId + " of interval " + interval
							.toString() + " is " + result.toString());
			return result;
		}

		throw new CiBailout("LinearScan: interval is null");
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
	 * the main entry for linear scanning register allocation.
	 */
	public void allocate()
	{
		numberInstruction();
		computeLocalLiveSets();
		computeGlobalLiveSet();

		buildIntervalList();

		printIntervals("Before register allocation");
		sortIntervalListBeforeAllocation();

		allocateRegisters();

		resolveDataFlow();

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
