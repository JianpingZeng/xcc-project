package lir.alloc;

import exception.CiBailout;
import hir.BasicBlock;
import lir.LIRInstruction;
import lir.LIROp1;
import lir.LIROpcode;
import lir.alloc.Interval.RegisterBinding;
import lir.ci.*;
import utils.TTY;

import java.util.*;

import static lir.LIRInstruction.OperandMode.Input;
import static lir.LIRInstruction.OperandMode.Output;
import static lir.alloc.Interval.RegisterBinding.Any;
import static lir.alloc.Interval.RegisterBinding.Fixed;
import static lir.alloc.Interval.RegisterPriority.*;
import static lir.alloc.Interval.SpillState.NoOptimization;
import static lir.alloc.Interval.State.*;
import static lir.ci.LIRRegister.RegisterFlag.Byte;
import static lir.ci.LIRRegister.RegisterFlag.*;
import static utils.Util.isOdd;

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
	public LinearScanWalker(LinearScan allocator, Interval unhandledFixed,
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

	private int findOptimalSplitPos(BasicBlock minBlock, BasicBlock maxBlock,
                                    int maxSplitPos)
	{
		int fromBlockNr = minBlock.linearScanNumber;
		int toBlockNr = maxBlock.linearScanNumber;

		assert 0 <= fromBlockNr && fromBlockNr < allocator
				.blockCount() : "out of range";
		assert 0 <= toBlockNr && toBlockNr < allocator
				.blockCount() : "out of range";
		assert fromBlockNr < toBlockNr : "must cross block boundary";

		// Try to split at end of maxBlock. IfStmt this would be after
		// maxSplitPos, then use the begin of maxBlock
		int optimalSplitPos = maxBlock.lastLIRInstructionId() + 2;
		if (optimalSplitPos > maxSplitPos)
		{
			optimalSplitPos = maxBlock.firstLIRInstructionId();
		}

		int minLoopDepth = maxBlock.loopDepth;
		for (int i = toBlockNr - 1; i >= fromBlockNr; i--)
		{
			BasicBlock cur = allocator.blockAt(i);

			if (cur.loopDepth < minLoopDepth)
			{
				// block with lower loop-depth found . split at the end of this block
				minLoopDepth = cur.loopDepth;
				optimalSplitPos = cur.lastLIRInstructionId() + 2;
			}
		}
		assert optimalSplitPos > allocator.maxOpId() || allocator.isBlockBegin(
				optimalSplitPos) : "algorithm must move split pos to block boundary";

		return optimalSplitPos;
	}

	/**
	 * finds out a optimal split position between minSplitPos and maxSplitPos
	 * according to following two rules:
	 * <ol>
     *  <li>Move split position out of loops.</li>
	 *  <li>Move split position to block boundaries.</li>
 *     </ol>
	 * @param interval
	 * @param minSplitPos
	 * @param maxSplitPos
	 * @param doLoopOptimization
	 * @return
	 */
	private int findOptimalSplitPos(Interval interval, int minSplitPos,
			int maxSplitPos, boolean doLoopOptimization)
	{
		int optimalSplitPos = -1;
		if (minSplitPos == maxSplitPos)
		{
			// trivial case, no opt of split position possible
			optimalSplitPos = minSplitPos;
		}
		else
		{
			assert minSplitPos < maxSplitPos : "must be true then";
			assert minSplitPos > 0 : "cannot access minSplitPos - 1 otherwise";

			// reason for using minSplitPos - 1: when the minimal split pos is
			// exactly at the beginning of a block, then minSplitPos is also a
			// possible split position. Use the block before as minBlock, because
			// then minBlock.lastLIRInstructionId() + 2 == minSplitPos
			BasicBlock minBlock = allocator.blockForId(minSplitPos - 1);

			// reason for using maxSplitPos - 1: otherwise there would be an
			// assert on failure when an interval ends at the end of the last block
			// of the method (in this case, maxSplitPos == allocator().maxLOpId()
			// + 2, and there is no block at this opId)
			BasicBlock maxBlock = allocator.blockForId(maxSplitPos - 1);

			assert minBlock.linearScanNumber
					<= maxBlock.linearScanNumber : "invalid order";
			if (minBlock == maxBlock)
			{
				// split position cannot be moved to block boundary,
				// so split as late as possible
				optimalSplitPos = maxSplitPos;
			}
			else
			{
				if (interval.hasHoleBetween(maxSplitPos - 1, maxSplitPos)
						&& !allocator.isBlockBegin(maxSplitPos))
				{
					// Do not move split position if the interval has a hole before
					// maxSplitPos. Intervals resulting from PhiNode-Functions have
					// more than one definition (marked as mustHaveRegister) with
					// a hole before each definition. When the register is needed
					// for the second definition : an earlier reloading is unnecessary.
					optimalSplitPos = maxSplitPos;

				}
				else
				{
					// search optimal block boundary between minSplitPos and maxSplitPos

					if (doLoopOptimization)
					{
						// Loop opt: if a loop-end marker is found between
						// min- and max-position : then split before this loop
						int loopEndPos = interval.nextUsageExact(
								Interval.RegisterPriority.LiveAtLoopEnd,
								minBlock.lastLIRInstructionId() + 2);

						assert loopEndPos > minSplitPos : "invalid order";
						if (loopEndPos < maxSplitPos)
						{
							// loop-end marker found between min- and max-position
							// if it is not the end marker for the same loop as the
							// min-position : then move the max-position to this loop block.
							// Desired getReturnValue: uses tagged as shouldHaveRegister
							// inside a loop cause a reloading of the interval
							// (normally, only mustHaveRegister causes a reloading)
							BasicBlock loopBlock = allocator
									.blockForId(loopEndPos);

							assert loopBlock
									!= minBlock :
									"loopBlock and minBlock must be "
									+ "different because block boundary is needed between";

							optimalSplitPos = findOptimalSplitPos(minBlock,
									loopBlock,
									loopBlock.lastLIRInstructionId() + 2);
							if (optimalSplitPos
									== loopBlock.lastLIRInstructionId() + 2)
							{
								optimalSplitPos = -1;
							}
						}
					}

					if (optimalSplitPos == -1)
					{
						// not calculated by loop opt
						optimalSplitPos = findOptimalSplitPos(minBlock,
								maxBlock, maxSplitPos);
					}
				}
			}
		}

		return optimalSplitPos;
	}

	/**
	 * Split an interval at the optimal position between minSplitPos and
	 * maxSplitPos into two portions:
	 * <br>(1) the left part has already a location assigned.
	 * <br>(2) the right part is sorted into to the unhanded list.
	 *
	 * @param interval
	 * @param minSplitPos
	 * @param maxSplitPos
	 */
	private void splitBeforeUsage(Interval interval, int minSplitPos,
			int maxSplitPos)
	{
		assert interval.from() < minSplitPos
				&& minSplitPos <= maxSplitPos
				&& maxSplitPos <= interval.to();

		assert currentPosition < minSplitPos;

		// to finds out a optimal split position
		int optimalSplitPos = findOptimalSplitPos(interval, minSplitPos,
				maxSplitPos, true);

		assert minSplitPos <= optimalSplitPos
				&& optimalSplitPos <= maxSplitPos : "out of range";
		assert optimalSplitPos <= interval
				.to() : "cannot split after end of interval";
		assert optimalSplitPos > interval
				.from() : "cannot split at start of interval";

		if (optimalSplitPos == interval.to()
				&& interval.nextUsage(MustHaveRegister, minSplitPos)
				== Integer.MAX_VALUE)
		{
			// the split position would be just before the end of the interval
			// . no split at all necessary
			return;
		}

		// must calculate this before the actual split is performed and before
		// split position is moved to odd opId.

		// when the split position at the block begin or no hole, the insert of
		// move is desired.
		boolean moveNecessary =
				allocator.isBlockBegin(optimalSplitPos)
				|| !interval.hasHoleBetween(optimalSplitPos - 1, optimalSplitPos);

		if (!allocator.isBlockBegin(optimalSplitPos))
		{
			// move position before actual instruction (odd opId)
			optimalSplitPos = (optimalSplitPos - 1) | 1;
		}

		assert allocator.isBlockBegin(optimalSplitPos) ||
				(optimalSplitPos % 2 == 1)
				: "split pos must be odd when not on block boundary";
		assert !allocator.isBlockBegin(optimalSplitPos) ||
				(optimalSplitPos % 2 == 0)
				: "split pos must be even on block boundary";

		// the right part to be added into unhandled list
		Interval splitPart = interval.split(optimalSplitPos, allocator);

		allocator.copyRegisterFlags(interval, splitPart);
		splitPart.setInsertMoveWhenActivated(moveNecessary);

		assert splitPart.from() >= current
				.currentFrom() :
				"cannot append new interval before current walk position";
		unhandledLists.addToListSortedByStartAndUsePositions(Any, splitPart);

		TTY.println(
				"      split interval in two parts (insertMoveWhenActivated: %b)",
				moveNecessary);

		TTY.print("      ");
		TTY.println(interval.logString(allocator));
		TTY.print("      ");
		TTY.println(splitPart.logString(allocator));
	}

	/**
	 * A little heuristic strategy was adopted just for optimizing interval has
	 * been assigned to {@linkplain lir.ci.StackSlot StackSlot}
	 * @param interval
	 */
	private void splitStackInterval(Interval interval)
	{
		int minSplitPos = currentPosition + 1;
		int maxSplitPos = Math
				.min(interval.firstUsage(ShouldHaveRegister), interval.to());

		splitBeforeUsage(interval, minSplitPos, maxSplitPos);
	}

	boolean isMove(LIRInstruction op, Interval from, Interval to)
	{
		if (op.opcode != LIROpcode.Move)
		{
			return false;
		}
		assert op instanceof LIROp1 : "move must be LIROp1";

		LIRValue input = ((LIROp1) op).operand();
		LIRValue result = ((LIROp1) op).result();
		return input.isVariable() && result.isVariable()
				&& input == from.operand && result == to.operand;
	}

	/**
	 * opt (especially for phi functions of nested loops):
	 * assign same spill slot to non-intersecting intervals using Register Hints.
	 * @param interval
	 */
	private void combineSpilledIntervals(Interval interval)
	{
		if (interval.isSplitChild())
		{
			// opt is only suitable for split parents
			return;
		}

		Interval registerHint = interval.locationHint(false, allocator);
		if (registerHint == null)
		{
			// cur is not the target of a move : otherwise registerHint would be set
			return;
		}
		assert registerHint
				.isSplitParent() : "register hint must be split parent";

		if (interval.spillState() != NoOptimization
				|| registerHint.spillState() != NoOptimization)
		{
			// combining the stack slots for intervals where spill move
			// opt is applied is not benefitial and would cause problems
			return;
		}

		int beginPos = interval.from();
		int endPos = interval.to();
		if (endPos > allocator.maxOpId() || isOdd(beginPos) || isOdd(endPos))
		{
			// safety checking that lirOpWithId is allowed
			return;
		}

		if (!isMove(allocator.instructionForId(beginPos), registerHint,
				interval) || !isMove(allocator.instructionForId(endPos),
				interval, registerHint))
		{
			// cur and registerHint are not connected with two moves
			return;
		}

		Interval beginHint = registerHint
				.getSplitChildAtOpId(beginPos, Input, allocator);

		Interval endHint = registerHint
				.getSplitChildAtOpId(endPos, Output, allocator);
		if (beginHint == endHint || beginHint.to() != beginPos
				|| endHint.from() != endPos)
		{
			// registerHint must be split : otherwise the re-writing of use
			// positions does not work
			return;
		}

		assert beginHint.location() != null : "must have register assigned";
		assert endHint.location() == null : "must not have register assigned";
		assert interval.firstUsage(MustHaveRegister)
				== beginPos : "must have use position at begin of interval because of move";
		assert endHint.firstUsage(MustHaveRegister)
				== endPos : "must have use position at begin of interval because of move";

		if (beginHint.location().isRegister())
		{
			// registerHint is not spilled at beginPos : so it would not be
			// benefitial to immediately spill cur
			return;
		}
		assert registerHint.spillSlot()
				!= null : "must be set when part of interval was spilled";

		// modify intervals such that cur gets the same stack slot as registerHint
		// delete use positions to prevent the intervals to get a register at beginning
		interval.setSpillSlot(registerHint.spillSlot());
		interval.removeFirstUsePos();
		endHint.removeFirstUsePos();
	}

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
						&& interval.to() > pos + 1 &&
						interval.nextUsage(ShouldHaveRegister, pos) > pos + 1)
				{
					TTY.println("      free register cannot be available because all registers blocked by following call");

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
						== -1 : "invalid opt: intervals intersect";
			}

			interval = interval.next;
		}
	}

	void spillCollectActiveAny()
	{
		Interval interval = activeLists.get(Any);
		while (interval != Interval.EndMarker)
		{
			setUsePos(interval,
					Math.min(interval.nextUsage(LiveAtLoopEnd, currentPosition),
							interval.to()), false);
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
			if (interval.splitParent() != null
					&& interval.currentIntersects(current))
			{
				setUsePos(interval, Math.min(
						interval.nextUsage(LiveAtLoopEnd, currentPosition),
						interval.to()), false);
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
	 * Splits the specified interval when only partial register is available,
	 * which can greatly improves the quality of generating code in terms of CISC cpu,
	 * like x86.
	 * @param interval
	 * @param registerAvailableUntil
     */
	void splitWhenPartialRegisterAvailable(Interval interval,
			int registerAvailableUntil)
	{
		int minSplitPos = Math.max(interval.previousUsage(ShouldHaveRegister,
						registerAvailableUntil), interval.from() + 1);
		splitBeforeUsage(interval, minSplitPos, registerAvailableUntil);
	}

	/**
	 * Allocates a free register for this interval.
	 * @param interval The interval to been assigned register.
	 * @return  Returns true if allocating successfully, otherwise, false would
	 * be returned.
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
		// invervals can be ignored safely
		{
			TTY.println("      state of registers:");
			for (LIRRegister register : availableRegs)
			{
				int i = register.number;
				TTY.println("      reg %d: usePos: %d", register.number,
						usePos[i]);
			}
		}

		// if the register hint is not null, then we use it
		LIRRegister hint = null;
		Interval locationHint = interval.locationHint(true, allocator);
		if (locationHint != null && locationHint.location() != null
				&& locationHint.location().isRegister())
		{
			hint = locationHint.location().asRegister();

			{
				TTY.println("      hint register %d from interval %s",
						hint.number, locationHint.logString(allocator));
			}
		}

		assert interval.location() == null
				: "interval" + interval + "must not be assigned with a register";

		// the register must be free at least until this position
		int regNeededUntil = interval.from() + 1;
		int intervalTo = interval.to();

		boolean needSplit = false;
		int splitPos = -1;

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
				// in this condition, we takes advantage of register hints to
				// optimizes the allocation
				if (minFullReg == null || availableReg == hint ||
						(usePos[number] < usePos[minFullReg.number]
								&& minFullReg != hint))
				{
					minFullReg = availableReg;
				}
			}
			else if (usePos[number] > regNeededUntil)
			{
				// this register is at least free until regNeededUntil
				if (maxPartialReg == null || availableReg == hint || (
						usePos[number] > usePos[maxPartialReg.number]
								&& maxPartialReg != hint))
				{
					maxPartialReg = availableReg;
				}
			}
		}
		if (minFullReg != null)
		{
			reg = minFullReg;
		}
		// if no entirely available register, then splitting of current interval is unavoidable
		else if (maxPartialReg != null)
		{
			needSplit = true;
			reg = maxPartialReg;
		}
		else
		{
			return false;
		}

		// the split position
		splitPos = usePos[reg.number];
		interval.assignLocation(reg.asValue(interval.kind()));

		{
			TTY.println("selected register %d", reg.number);
		}

		assert splitPos > 0 : "invalid splitPos";
		if (needSplit)
		{
			// register not available for full interval, so split it
			splitWhenPartialRegisterAvailable(interval, splitPos);
		}

		// only return true if interval is completely assigned
		return true;
	}

	/**
	 * split an interval at the optimal position between minSplitPos and
	 * maxSplitPos in two parts:
	 * <ol>
	 *     <li>the left part has already a location assigned.</li>
	 *     <li>the right part is always on the stack and therefore ignored in
	 *     further processing.</li>
	 * </ol>
	 * @param interval
	 */
	void splitForSpilling(Interval interval)
	{
		// calculate allowed range of splitting position
		int maxSplitPos = currentPosition;
		int minSplitPos = Math
				.max(interval.previousUsage(ShouldHaveRegister, maxSplitPos)
						+ 1, interval.from());

		{
			TTY.print("----- splitting and spilling interval: ");
			TTY.println(interval.logString(allocator));
			TTY.println("      between %d and %d", minSplitPos, maxSplitPos);
		}

		assert interval.state
				== Active : "why spill interval that is not active?";
		assert interval.from()
				<= minSplitPos : "cannot split before start of interval";
		assert minSplitPos <= maxSplitPos : "invalid order";
		assert maxSplitPos < interval
				.to() : "cannot split at end end of interval";
		assert currentPosition < interval
				.to() : "interval must not end before current position";

		if (minSplitPos == interval.from())
		{
			// the whole interval is never used, so spill it entirely to memory
			{
				TTY.println(
						"      spilling entire interval because split pos is at "
								+ "beginning of interval");
				TTY.println(
						"      use positions: " + interval.usePosList().size());
			}
			assert interval.firstUsage(ShouldHaveRegister)
					> currentPosition : "interval must not have use position "
					+ "before currentPosition";

			allocator.assignSpillSlot(interval);
			allocator.changeSpillState(interval, minSplitPos);

			// Also kick parent intervals out of register to memory when they have no use
			// position. This avoids short interval in register surrounded by intervals in
			// memory . avoid useless moves from memory to register and back
			Interval parent = interval;
			while (parent != null && parent.isSplitChild())
			{
				parent = parent.getSplitChildBeforeOpId(parent.from());

				if (parent.location().isRegister())
				{
					if (parent.firstUsage(ShouldHaveRegister)
							== Integer.MAX_VALUE)
					{
						// parent is never used, so kick it out of its assigned register
						{
							TTY.println(
									"      kicking out interval %d out of its register"
											+ " because it is never used",
									parent.operandNumber);
						}
						allocator.assignSpillSlot(parent);
					}
					else
					{
						// do not go further back because the register is actually used by the interval
						parent = null;
					}
				}
			}

		}
		else
		{
			// search optimal split pos, split interval and spill only the right hand part
			int optimalSplitPos = findOptimalSplitPos(interval, minSplitPos,
					maxSplitPos, false);

			assert minSplitPos <= optimalSplitPos
					&& optimalSplitPos <= maxSplitPos : "out of range";
			assert optimalSplitPos < interval
					.to() : "cannot split at end of interval";
			assert optimalSplitPos >= interval
					.from() : "cannot split before start of interval";

			if (!allocator.isBlockBegin(optimalSplitPos))
			{
				// move position before actual instruction (odd opId)
				optimalSplitPos = (optimalSplitPos - 1) | 1;
			}

			{
				TTY.println("      splitting at position %d", optimalSplitPos);
			}
			assert allocator.isBlockBegin(optimalSplitPos) || (
					optimalSplitPos % 2
							== 1) : "split pos must be odd when not on block boundary";
			assert !allocator.isBlockBegin(optimalSplitPos) || (
					optimalSplitPos % 2
							== 0) : "split pos must be even on block boundary";

			Interval spilledPart = interval.split(optimalSplitPos, allocator);
			allocator.assignSpillSlot(spilledPart);
			allocator.changeSpillState(spilledPart, optimalSplitPos);

			if (!allocator.isBlockBegin(optimalSplitPos))
			{
				{
					TTY.println("      inserting move from interval %d to %d",
							interval.operandNumber, spilledPart.operandNumber);
				}
				insertMove(optimalSplitPos, interval, spilledPart);
			}

			// the currentSplitChild is needed later when moves are inserted for reloading
			assert spilledPart.currentSplitChild()
					== interval : "overwriting wrong currentSplitChild";
			spilledPart.makeCurrentSplitChild();

			{
				TTY.println("      split interval in two parts");
				TTY.print("      ");
				TTY.println(interval.logString(allocator));
				TTY.print("      ");
				TTY.println(spilledPart.logString(allocator));
			}
		}
	}

	/**
	 * Split and spill specified interval into stack slot, or the interval in active list.
	 * @param interval
	 */
	void splitAndSpillInterval(Interval interval)
	{
		assert interval.state == Active
				|| interval.state == Inactive : "other states not allowed";

		int currentPos = currentPosition;
		if (interval.state == Inactive)
		{
			// the interval is currently inactive, so no spill slot is needed for
			// now. when the split part is activated, the interval has a new chance
			// to get a register, so in the best case no stack slot is necessary
			assert interval.hasHoleBetween(currentPos - 1,
					currentPos + 1) : "interval can not be inactive otherwise";
			splitBeforeUsage(interval, currentPos + 1, currentPos + 1);

		}
		else
		{
			// search the position where the interval must have a register and split
			// at the optimal position before.
			// The new created part is added to the unhandled list and will get a register
			// when it is activated
			int minSplitPos = currentPos + 1;
			int maxSplitPos = Math
					.min(interval.nextUsage(MustHaveRegister, minSplitPos),
							interval.to());

			splitBeforeUsage(interval, minSplitPos, maxSplitPos);

			assert interval.nextUsage(MustHaveRegister, currentPos)
					== Integer.MAX_VALUE : "the remaining part is spilled to stack "
					+ "and therefore has no register";
			splitForSpilling(interval);
		}
	}

	/**
	 * Split an Interval and spill it to memory so that cur can be placed in a
	 * register.
	 * @param interval	The current interval being partial or entirely assigned
	 *                     to a register or stack slot.
	 */
	void allocateBlockedReg(Interval interval)
	{
		{
			TTY.println(
					"need to split and spill to get register for " + interval
							.logString(allocator));
		}

		// collect current usage of registers
		initUsePosLists(false);

		// calculate usage position for register
		spillExcludeActiveFixed();

		StringBuilder sb = new StringBuilder();
		sb.append("must not have unhandled fixed intervals ");
		sb.append("because all fixed intervals have a use at position 0");
		assert unhandledLists.get(Fixed) == Interval.EndMarker : sb.toString();

		spillBlockInactiveFixed(interval);

		//
		spillCollectActiveAny();
		spillCollectInactiveAny(interval);

		// the register must be free at least until this position
		int firstUsage = interval.firstUsage(MustHaveRegister);

		// a little heuristic opt
		int regNeededUntil = Math.min(firstUsage, interval.from() + 1);
		int intervalTo = interval.to();
		assert regNeededUntil > 0
				&& regNeededUntil < Integer.MAX_VALUE : "interval has no use";

		LIRRegister reg = null;
		LIRRegister ignore = interval.location() != null && interval.location()
				.isRegister() ? interval.location().asRegister() : null;

		// to find the last most(also the maximum use position) used physical register.
		for (LIRRegister availableReg : availableRegs)
		{
			int number = availableReg.number;
			if (availableReg == ignore)
			{
				// this register must be ignored
			}
			else if (usePos[number] > regNeededUntil)
			{
				if (reg == null || (usePos[number] > usePos[reg.number]))
				{
					reg = availableReg;
				}
			}
		}

		if (reg == null || firstUsage >= usePos[reg.number])
		{
			// all other intervals are used before current
			// so it is best to spill current
			// the first use of cur is later than the spilling position -> spill cur

			TTY.println(
					"able to spill current interval. firstUsage(register): %d, usePos: %d",
					firstUsage, reg == null ? 0 : usePos[reg.number]);

			if (firstUsage <= interval.from() + 1)
			{
				assert false :
						"cannot spill interval that is used in first instruction "
								+ "(possible reason: no register found) firstUsage="
								+ firstUsage + ", interval.from()=" + interval
								.from();
				// assign a reasonable register and do a bailout in product mode
				// to avoid errors
				allocator.assignSpillSlot(interval);
				throw new CiBailout("LinearScan: no register found");
			}

			splitAndSpillInterval(interval);
			return;
		}

		// usually, splitting is not needed.
		boolean needSplit = blockPos[reg.number] <= intervalTo;

		int splitPos = blockPos[reg.number];

		{
			TTY.println("decided to use register %d", reg.number);
		}

		assert splitPos > 0 : "invalid splitPos";
		assert needSplit || splitPos > interval
				.from() : "splitting interval at from";

		interval.assignLocation(reg.asValue(interval.kind()));
		if (needSplit)
		{
			// register not available for full interval :  so split it
			splitWhenPartialRegisterAvailable(interval, splitPos);
		}

		// perform splitting and spilling for all affected intervals
		splitAndSpillIntersectingIntervals(reg);
	}

	/**
	 * First, splits the specified interval whose value resides in specified register at
	 * a optimal position by invoking {@linkplain #splitAndSpillInterval(Interval)}.
	 * </br>
	 * Second, spills the first part of interval and appends the split child interval into
	 * the last of unhandled list.
	 * @param reg
     */
	void splitAndSpillIntersectingIntervals(LIRRegister reg)
	{
		assert reg != null : "no register assigned";

		for (int i = 0; i < spillIntervals[reg.number].size(); i++)
		{
			Interval interval = spillIntervals[reg.number].get(i);
			removeFromList(interval);
			splitAndSpillInterval(interval);
		}
	}


	/**
	 * Allocates a physical register or memory location to an interval.
	 *
	 * @return
	 */
	boolean activateCurrent()
	{
		Interval interval = current;
		boolean result = true;

		LIRValue operand = interval.operand;
		// if the interval already be allocated in stack slot,
		// split it at first use position
		if (interval.location() != null && interval.location().isStackSlot())
		{
			splitStackInterval(interval);
			result = false;
		}
		else
		{
			// heuristic strategy adopted for optimally splitting the interval
			if (operand.isVariable() && allocator.operands.
					mustStartInMemory((LIRVariable) operand))
			{
				assert interval.location()
						== null : "register already assigned";
				allocator.assignSpillSlot(interval);

				if (!allocator.operands.mustStayInMemory((LIRVariable) operand))
				{
					splitStackInterval(interval);
				}
				result = false;
			}
			else if (interval.location() == null)
			{
				/**
				 * interval has not assigned register or stack slot.
				 * this is the normal case for most intervals
				 */
				combineSpilledIntervals(interval);

				/** Obtains the available register to be allocated,
				 * denoting by availableRegister.
				 */
				initVarsForAlloc(interval);

				if (noAllocationPossible(interval) || !allocFreeRegister(
						interval))
				{
					// no empty register available
					// split and spill another interval so that this
					// interval gets a register
					allocateBlockedReg(interval);
				}

				// spilled intervals need not be move to active list
				if (!interval.location().isRegister())
				{
					result = false;
				}
			}
		}

		// load spilled value that become active from stack slot to register
		if (interval.insertMoveWhenActivated())
		{
			assert interval.isSplitChild();
			assert interval.currentSplitChild() != null;
			assert interval.currentSplitChild().operand
					!= operand : "can not insert move between same register";

			insertMove(interval.from(), interval.currentSplitChild(), interval);
		}

		interval.makeCurrentSplitChild();

		// when true, the interval is moved to active list
		return result;
	}
}