package backend.lir.linearScan;

/**
 * @author Xlous.zeng
 * @version 0.1
 */

import static backend.lir.ci.LIRRegister.RegisterFlag.Byte;
import static backend.lir.ci.LIRRegister.RegisterFlag.CPU;
import static backend.lir.ci.LIRRegister.RegisterFlag.FPU;
import static backend.lir.linearScan.Interval.RegisterBinding.Any;
import static backend.lir.linearScan.Interval.RegisterBinding.Fixed;
import static tools.Util.isOdd;
import hir.BasicBlock;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import backend.lir.LIRInstruction;
import backend.lir.LIROp1;
import backend.lir.LIROpcode;
import backend.lir.ci.LIRKind;
import backend.lir.ci.LIRRegister;
import backend.lir.ci.LIRValue;
import backend.lir.ci.LIRVariable;
import backend.lir.linearScan.Interval.RegisterBinding;
import tools.TTY;

/**
 * This file defines a class that takes a important role in implementing register
 * allocation walking through all of intervals.
 */
public final class LinearScanWalker extends IntervalWalker
{
	MoveResolver moveResolver;

	// for those interval spilled out onto stack slot
	List<Interval>[] spillIntervals;

	/** freeUntilPos*/
	int[] usePos;

	/**
	 * blockPos[reg] stores a hard limit to each register where the register cannot be
	 * freed by spilling. This position is setted with {@linkplain Integer#MAX_VALUE}
	 * by the fixed active and inactives that model the operations requiring reservedOperands
	 * in fixed registers.
	 */
	int[] blockPos;

	/**he available register list when allocating*/
	private LIRRegister[] availableRegs;

	/**
	 * Creates a new interval walker in linear scan order.
	 *
	 * @param allocator      the register allocator context.
	 * @param unhandledFixed the list of unhandled {@linkplain RegisterBinding#Fixed
	 *                          fixed} intervals.
	 * @param unhandledAny   the list of unhandled {@linkplain RegisterBinding#Any
	 *                          non-fixed} intervals.
	 */
	public LinearScanWalker(
			LinearScan allocator, Interval unhandledFixed,
			Interval unhandledAny)
	{
		super(allocator, unhandledFixed, unhandledAny);
		this.moveResolver = new MoveResolver(allocator);

		int numRegs = allocator.registers.length;
		spillIntervals = new List[numRegs];
		for (int i = 0; i < numRegs; i++)
		{
			spillIntervals[i] = new ArrayList<>(8);
		}
		usePos = new int[numRegs];
		blockPos = new int[numRegs];

	}

	/**
	 * Finishes the linear scan register allocation, makes destructing of phi function
	 * and resolving data flow.
	 */
	public void finishAllocation()
	{
		// must be called when all intervals are allocated.
		moveResolver.resolveAndAppendMoves();
	}
	
	boolean isMove(LIRInstruction op, Interval from, Interval to)
	{
		if (op.opcode != LIROpcode.Move)
		{
			return false;
		}
		assert op instanceof LIROp1 : "move must be LIROp1";

		LIRValue input = ((LIROp1) op).operand();
		LIRValue result = op.result();
		return input.isVariable() && result.isVariable()
				&& input == from.operand && result == to.operand;
	}
	/**
	 * Obtains the allocatable register for this interval in terms of its data frontend.type,
	 * for example, byte, float-gpu register, normal-cpu register. 
	 * @param interval
	 */
	void initVarsForAlloc(Interval interval)
	{
		EnumMap<LIRRegister.RegisterFlag, LIRRegister[]> categorizedRegs =
				allocator.backend.registerConfig
				.getCategorizedAllocatableRegisters();

		if (allocator.operands.mustBeByteRegister(interval.operand))
		{
			assert interval.kind() != LIRKind.Float
					&& interval.kind() != LIRKind.Double : "cpu regs only";
			availableRegs = categorizedRegs.get(Byte);
		}
		else if (interval.kind() == LIRKind.Float
				|| interval.kind() == LIRKind.Double)
		{
			availableRegs = categorizedRegs.get(FPU);
		}
		else
		{
			availableRegs = categorizedRegs.get(CPU);
		}
	}
	/** Checks if it is possible to allocate register for given interval.*/
	boolean noAllocationPossible(Interval interval)
	{
		// specially handling for x86
		if (backend.machineInfo.arch.isX86())
		{
			// fast calculation of intervals that can never get a register
			// because the the next instruction is a call that blocks all registers
			// Note: this does not work if callee-saved registers are available
			// (e.g. on Sparc)

			// check if this interval is the getReturnValue of a split operation
			// (an interval got a register until this position)
			int pos = interval.from();
			if (isOdd(pos))
			{
				// the current instruction is a call that blocks all registers
				if (pos < allocator.maxOpId() && allocator.hasCall(pos + 1)
						&& interval.to() > pos + 1)
				{
					TTY.println("      free register cannot be available because "
							+ "all registers blocked by following call");

					// safety check that there is really no register available
					assert !allocFreeRegister(
							interval) : "found a register for this interval";
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Initialize the freeUntilPos with {@linkplain Integer#MAX_VALUE} for
	 * available register.
	 */
	void initUsePosLists(boolean onlyProcessUsePos)
	{
		for (LIRRegister register : availableRegs)
		{
			int i = register.number;
			usePos[i] = Integer.MAX_VALUE;

			if (!onlyProcessUsePos)
			{
				blockPos[i] = Integer.MAX_VALUE;
				spillIntervals[i].clear();
			}
		}
	}

	void excludeFromUse(Interval i)
	{
		LIRValue location = i.location();
		int i1 = location.asRegister().number;

		// validates the register number
		if (i1 >= availableRegs[0].number && i1 <= availableRegs[
				availableRegs.length - 1].number)
		{
			usePos[i1] = 0;
		}
	}

	void setUsePos(Interval interval, int usePos, boolean onlyProcessUsePos)
	{
		if (usePos != -1)
		{
			assert usePos != 0 : "must use excludeFromUse to set usePos to 0";
			int i = interval.location().asRegister().number;
			if (i >= availableRegs[0].number && i <= availableRegs[
					availableRegs.length - 1].number)
			{
				// choose the lower value
				if (usePos < this.usePos[i])
				{
					this.usePos[i] = usePos;
				}
				if (!onlyProcessUsePos)
				{
					spillIntervals[i].add(interval);
				}
			}
		}
	}

	void setBlockPos(Interval i, int blockPos)
	{
		if (blockPos != -1)
		{
			int reg = i.location().asRegister().number;
			if (reg >= availableRegs[0].number && reg <= availableRegs[
					availableRegs.length - 1].number)
			{
				if (blockPos <= this.blockPos[reg])
				{
					this.blockPos[reg] = blockPos;
				}
				if (blockPos <= usePos[reg])
				{
					usePos[reg] = blockPos;
				}
			}
		}
	}

	/**
	 * Set the usage position of any interval in active list to 0.
	 */
	private void computeUsePosForActive()
	{
		for (RegisterBinding binding : RegisterBinding.VALUES)
		{
			Interval interval = activeLists.get(binding);
			while (interval != Interval.EndMarker)
			{
				assert interval.location().isRegister()
						: "active interval must have a register assigned";

				// exclude the active interval, which means set the usePos[interval] = 0
				excludeFromUse(interval);
				interval = interval.next;
			}
		}
	}

	void freeCollectInactiveFixed(Interval current)
	{
		Interval interval = inactiveLists.get(Fixed);
		while (interval != Interval.EndMarker)
		{
			if (current.to() <= interval.currentFrom())
			{
				assert interval.currentIntersectsAt(current)
						== -1 : "must not intersect";
				setUsePos(interval, interval.currentFrom(), true);
			}
			else
			{
				setUsePos(interval, interval.currentIntersectsAt(current),
						true);
			}
			interval = interval.next;
		}
	}

	void freeCollectInactiveAny(Interval current)
	{
		Interval interval = inactiveLists.get(Any);
		while (interval != Interval.EndMarker)
		{
			// they are inactive and thus have a lifetime hole at the current
			// interval, so they don't intersect with the current, witch is guaranteed
			// by SSA form properties.
			if (current.to() <= interval.currentFrom())
			{
				assert interval.currentIntersectsAt(current)
						== -1 : "must not intersect";
				setUsePos(interval, interval.currentFrom(), true);
			}
			else
			{
				setUsePos(interval, interval.currentIntersectsAt(current), true);
				interval = interval.next;
			}
		}
	}

	void freeCollectUnhandled(RegisterBinding kind, Interval current)
	{
		Interval interval = unhandledLists.get(kind);
		while (interval != Interval.EndMarker)
		{
			setUsePos(interval, interval.intersectsAt(current), true);
			if (kind == Fixed && current.to() <= interval.from())
			{
				setUsePos(interval, interval.from(), true);
			}
			interval = interval.next;
		}
	}

	void spillExcludeActiveFixed()
	{
		Interval interval = activeLists.get(Fixed);
		while (interval != Interval.EndMarker)
		{
			excludeFromUse(interval);
			interval = interval.next;
		}
	}

	void spillBlockUnhandledFixed(Interval current)
	{
		Interval interval = unhandledLists.get(Fixed);
		while (interval != Interval.EndMarker)
		{
			setBlockPos(interval, interval.intersectsAt(current));
			interval = interval.next;
		}
	}

	void spillBlockInactiveFixed(Interval current)
	{
		Interval interval = inactiveLists.get(Fixed);
		while (interval != Interval.EndMarker)
		{
			if (current.to() > interval.currentFrom())
			{
				setBlockPos(interval, interval.currentIntersectsAt(current));
			}
			else
			{
				assert interval.currentIntersectsAt(current)
						== -1 : "invalid backend.opt: intervals intersect";
			}

			interval = interval.next;
		}
	}

	void spillCollectActiveAny()
	{
		Interval interval = activeLists.get(Any);
		while (interval != Interval.EndMarker)
		{
			setUsePos(interval, interval.to(), false);
			interval = interval.next;
		}
	}

	void spillCollectInactiveAny(Interval current)
	{
		Interval interval = inactiveLists.get(Any);
		while (interval != Interval.EndMarker)
		{
			// when current is the getReturnValue of an interval split, then intersect check is needed
			// for SSA form
			if (interval.currentIntersects(current))
			{
				setUsePos(interval,	interval.to(), false);
			}
			interval = interval.next;
		}
	}

	/**
	 * <ol>
	 *     <li>Inserts move instruction was inserted in split position, when an interval is split
	 *         within a basic block.
	 *     </li>
 *         <li>IfStmt the location of interval at the end of predecessor and begin of successor
	 *         differ, appropriate move instruction are inserted.
	 *     </li>
	 * </ol>
	 * @param opId The op id of first LIRInstruction of this interval.
	 * @param srcIt
	 * @param dstIt
     */
	void insertMove(int opId, Interval srcIt, Interval dstIt)
	{
		// output all moves here. When source and target are equal, the move is
		// optimized away later in assignRegNums

		// the second instruction
		opId = (opId + 1) & ~1;
		BasicBlock opBlock = allocator.blockForId(opId);
		assert opId > 0 && allocator.blockForId(opId - 2)
				== opBlock : "cannot insert move at block boundary";

		// calculate index of instruction inside instruction list of current block
		// the minimal index (for a block with no spill moves) can be calculated
		// because the numbering of instructions is known. When the block already
		// isDeclScope spill moves, the index must be increased until the correct
		// index is reached.

		List<LIRInstruction> list = opBlock.getLIRBlock().lir()
				.instructionsList();
		int index = (opId - list.get(0).id) >> 1;
		assert list.get(index).id <= opId : "error in calculation";

		while (list.get(index).id != opId)
		{
			index++;
			assert 0 <= index && index < list.size() : "index out of bounds";
		}
		assert 1 <= index && index < list.size() : "index out of bounds";
		assert list.get(index).id == opId : "error in calculation";

		// insert new instruction before instruction at position index
		moveResolver.moveInsertPosition(opBlock.getLIRBlock().lir(), index - 1);
		moveResolver.addMapping(srcIt, dstIt);
	}

	/**
	 * Allocates a free register for this interval.
	 * @param interval The interval to been assigned register.
	 * @return  Returns true if allocating of register successfully, otherwise, 
	 * false would be returned.
	 */
	private boolean allocFreeRegister(Interval interval)
	{
		TTY.println("trying to find free register for " + interval
				.logString(allocator));

		// initialize the usePos which represented as freeUntilPos in Wimmer's paper
		initUsePosLists(true);

		// compute usePos array for any interval in active list
		computeUsePosForActive();

		// compute usePos array for any interval in inactive list
		freeCollectInactiveFixed(interval);
		freeCollectInactiveAny(interval);

		// freeCollectUnhandled(fixedKind, cur);
		assert unhandledLists.get(Fixed) == Interval.EndMarker :
				"must not have unhandled fixed intervals"
						+ " because all fixed intervals have a use at position 0";

		// usePos isDeclScope the start of the next interval that has this register assigned
		// (either as a fixed register or a normal allocated register in the past)
		// only intervals overlapping with cur are processed, non-overlapping
		// intervals can be ignored safely
		{
			TTY.println("      state of registers:");
			for (LIRRegister register : availableRegs)
			{
				int i = register.number;
				TTY.println("      reg %d: usePos: %d", register.number, usePos[i]);
			}
		}

		assert interval.location() == null
				: "interval" + interval + "must not be assigned with a register";

		// the register must be free at least until this position
		int regNeededUntil = interval.from() + 1;
		int intervalTo = interval.to();

		boolean needSplit = false;

		LIRRegister reg = null;

		// the reason for as minimum as possible: avoiding waste
		LIRRegister minFullReg = null;
		// the reason for as maximum as possible: lead to more part of current
		// interval could been assigned with a register.
		LIRRegister maxPartialReg = null;

		// walk through all of available register, to find a register witch can entirely
		// or partially holds wrap the current interval
		for (int i = 0; i < availableRegs.length; ++i)
		{
			LIRRegister availableReg = availableRegs[i];
			int number = availableReg.number;	
			
			// this register is free for the full interval
			if (usePos[number] >= intervalTo)
			{
				if (minFullReg == null ||
						usePos[number] < usePos[minFullReg.number])
				{
					minFullReg = availableReg;
				}
			}
			else if (usePos[number] > regNeededUntil)
			{
				// this register is at least free until regNeededUntil
				if (maxPartialReg == null || 
						usePos[number] > usePos[maxPartialReg.number])
				{
					maxPartialReg = availableReg;
				}
			}
		}
		if (minFullReg != null)
		{
			reg = minFullReg;
		}
		// if no entirely available register, then spilling of current interval is unavoidable
		else if (maxPartialReg != null)
		{
			needSplit = true;
			reg = maxPartialReg;
		}
		else
		{
			return false;
		}

		if (needSplit)
		{
			interval.assignLocation(interval.spillSlot());
			TTY.println("selected stack slot");
		}
		else 
		{
			interval.assignLocation(reg.asValue(interval.kind()));
			TTY.println("selected register %d", reg.number);
		}

		// only return true if interval is completely assigned
		return true;
	}
	
	/**
	 * Assigns a physical register or stack slot to an interval.
	 *
	 * @return	return false if assignment failed, otherwise return false.
	 */
	boolean activateCurrent()
	{
		Interval interval = current;
		boolean result = true;

		LIRValue operand = interval.operand;
		
		// if the interval already be allocated in stack slot,
		// just return false.
		if (interval.location() != null && interval.location().isStackSlot())
		{	
			result = false;
		}
		else
		{
			// because it must stayed in memory, so a stack slot would 
			// be assigned to this interval.
			if (operand.isVariable() && allocator.operands.
					mustStartInMemory((LIRVariable) operand))
			{
				assert interval.location()
						== null : "register already assigned";
				allocator.assignSpillSlot(interval);
				
				result = false;
			}
			else if (interval.location() == null)
			{	
				/** Obtains the available register to be allocated,
				 * denoting by availableRegister.
				 */
				initVarsForAlloc(interval);

				// attempts to allocate a free register to this interval.
				allocFreeRegister(interval);

				// spilled intervals need not be move to active list
				if (!interval.location().isRegister())
				{
					result = false;
				}
			}
		}

		// when true, the interval is moved to active list
		return result;
	}
}