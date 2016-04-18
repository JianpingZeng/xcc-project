package lir.alloc;

import driver.Backend;
import hir.BasicBlock;
import hir.Method;
import lir.LIRGenerator;
import lir.LIRInstruction;
import lir.StackFrame;
import lir.ci.*;
import utils.BitSet2D;

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
	BitSet2D intervalInLoop;

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
				assert unusedSpillSlot == null :
						"wasting a spill slot";
				unusedSpillSlot = StackSlot.get(kind, maxSpills);
				maxSpills++;
			}
			slot = StackSlot.get(kind, maxSpills);
			maxSpills += 2;
		}
		else if(unusedSpillSlot != null)
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
		assert sortedBlocks.length == m.linearScanOrder().size() :
				"invalid cached block sorted list";
		return sortedBlocks.length;
	}

	/**
	 * obtains the basic block at given position where indexable to.
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
			numInstrs += blockAt(i).getMachineBlock().lir().instructionsList().size();
		}

		opIdToInstructionMap = new LIRInstruction[numInstrs];
		opIdToBlockMap = new BasicBlock[numBlocks];

		int opID = 0, index = 0;
		for(int i = 0; i < numBlocks; i++)
		{
			BasicBlock block = blockAt(i);
			block.setFirstLIRInstruction(opID);
			List<LIRInstruction> instructions = block.getMachineBlock().lir().instructionsList();
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
	 * Gets the size of the {@link lir.MachineBlock#livein} and {@link lir.MachineBlock#liveout}
	 * sets for a basic block. These sets do not include any operands allocated
	 * as a result of creating {@linkplain #createDerivedInterval(Interval) derived
	 * intervals}.
	*/
	private int liveSetSize()
	{
		return firstDerivedIntervalIndex == -1 ?operands.size()
				: firstDerivedIntervalIndex;
	}

	private int numLoops()
	{
		return m.numLoops();
	}

	/**
	 * Computes local live sets (i.e. {@link lir.MachineBlock#livegen} and
	 * {@link lir.MachineBlock#livekill}) separately for each block.
	 */
	private void computeLocalLiveSets()
	{
		int numBlocks = blockCount();
		int liveSize = liveSetSize();

		BitSet2D localIntervalInLoop = new BitSet2D(operands.size(), numLoops());

	}

	private void computeGlobalLiveSet()
	{

	}
	private void buildIntervalList()
	{

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
