package backend.lir.linearScan;

import hir.BasicBlock;
import hir.Condition;

import java.util.ArrayList;
import java.util.List;

import backend.lir.LIRInstruction;
import backend.lir.LIROp1;
import backend.lir.LIROpcode;
import backend.lir.ci.LIRBranch;

/**
 * This class is served as eliminating moves operation between predecessor and
 * successor, while the last move instruction of all predecessors are same, removing
 * those and inserting those into the head of current block.
 *
 * @author Xlous.zeng
 */
public final class EdgeMoveOptimizer
{
	/**
	 * Optimizes moves on block edges.
	 *
	 * @param blockList a list of blocks whose moves should be optimized
	 */
	public static void optimize(List<BasicBlock> blockList)
	{
		EdgeMoveOptimizer optimizer = new EdgeMoveOptimizer();

		// ignore the first block in the list (so, index 0 is not processed)
		for (int i = blockList.size() - 1; i >= 1; i--)
		{
			BasicBlock block = blockList.get(i);

			if (block.getNumOfPreds() > 1)
			{
				optimizer.optimizeMovesAtBlockEnd(block);
			}
			if (block.getNumOfSuccs() == 2)
			{
				optimizer.optimizeMovesAtBlockBegin(block);
			}
		}
	}

	private final List<List<LIRInstruction>> edgeInstructionSeqences;

	private EdgeMoveOptimizer()
	{
		edgeInstructionSeqences = new ArrayList<>(4);
	}

	/**
	 * Determines if two operations are both {@linkplain LIROpcode#Move moves}
	 * that have the same {@linkplain LIROp1#operand() source} and
	 * {@linkplain LIROp1#result() destination} reservedOperands.
	 *
	 * @param op1 the first instruction to compare
	 * @param op2 the second instruction to compare
	 * @return {@opcode true} if {@opcode op1} and {@opcode op2} are the same by
	 *                          the above algorithm
	 */
	private boolean same(LIRInstruction op1, LIRInstruction op2)
	{
		assert op1 != null;
		assert op2 != null;

		if (op1.opcode == LIROpcode.Move && op2.opcode == LIROpcode.Move)
		{
			assert op1 instanceof LIROp1 : "move must be LIROp1";
			assert op2 instanceof LIROp1 : "move must be LIROp1";
			LIROp1 move1 = (LIROp1) op1;
			LIROp1 move2 = (LIROp1) op2;
			if (move1.operand()
					.equals(move2.operand()) && move1.result()
					.equals(move2.result()))
			{
				// these moves are exactly equal and can be optimized
				return true;
			}
		}
		return false;
	}

	/**
	 * Moves the longest {@linkplain #same common} subsequence at the end all
	 * predecessors of {@opcode block} to the start of {@opcode block}.
	 */
	private void optimizeMovesAtBlockEnd(BasicBlock block)
	{
		if (block.isPredecessor(block))
		{
			// currently we can't handle this correctly.
			return;
		}

		// clear all internal data structures
		edgeInstructionSeqences.clear();

		int numPreds = block.getNumOfPreds();
		assert numPreds > 1 : "do not call otherwise";

		// setup a list with the LIR instructions of all predecessors
		for (int i = 0; i < numPreds; i++)
		{
			BasicBlock pred = block.predAt(i);
			List<LIRInstruction> predInstructions = pred.getLIRBlock().lir()
					.instructionsList();

			if (pred.getNumOfSuccs() != 1)
			{
				// this can happen with switch-statements where multiple edges
				// are between the same blocks.
				return;
			}

			assert pred.succAt(0) == block : "invalid control flow";
			assert predInstructions.get(predInstructions.size() - 1).opcode
					== LIROpcode.Branch : "block with successor must end with branch";
			assert predInstructions.get(predInstructions.size()
					- 1) instanceof LIRBranch : "branch must be LIROpBranch";
			assert ((LIRBranch) predInstructions
					.get(predInstructions.size() - 1)).cond()
					== Condition.TRUE : "block must end with unconditional branch";

			// ignore the unconditional branch at the end of the block
			List<LIRInstruction> seq = predInstructions
					.subList(0, predInstructions.size() - 1);
			edgeInstructionSeqences.add(seq);
		}

		// process lir-instructions while all predecessors end with the same
		// instruction
		while (true)
		{
			if (edgeInstructionSeqences.isEmpty())
				return;

			List<LIRInstruction> seq = edgeInstructionSeqences.get(0);
			if (seq.isEmpty())
			{
				return;
			}

			LIRInstruction op = last(seq);
			for (int i = 1; i < numPreds; ++i)
			{
				List<LIRInstruction> otherSeq = edgeInstructionSeqences.get(i);
				if (otherSeq.isEmpty() || !same(op, last(otherSeq)))
				{
					return;
				}
			}

			// insert the instruction at the beginning of the current block
			block.getLIRBlock().lir().insertBefore(1, op);

			// delete the instruction at the end of all predecessors
			for (int i = 0; i < numPreds; i++)
			{
				seq = edgeInstructionSeqences.get(i);
				removeLast(seq);
			}
		}
	}

	/**
	 * Moves the longest {@linkplain #same common} subsequence at the start of all
	 * successors of {@opcode block} to the end of {@opcode block} just prior to the
	 * branch instruction ending {@opcode block}.
	 */
	private void optimizeMovesAtBlockBegin(BasicBlock block)
	{
		edgeInstructionSeqences.clear();
		int numSux = block.getNumOfSuccs();

		List<LIRInstruction> instructions =
				block.getLIRBlock().lir().instructionsList();

		assert numSux == 2 : "method should not be called otherwise";
		assert instructions.get(instructions.size() - 1).opcode
				== LIROpcode.Branch : "block with successor must end with branch";
		assert instructions.get(instructions.size()
				- 1) instanceof LIRBranch : "branch must be LIROpBranch";
		assert ((LIRBranch) instructions.get(instructions.size() - 1)).cond()
				== Condition.TRUE : "block must end with unconditional branch";


		LIRInstruction branch = instructions.get(instructions.size() - 2);
		if ((branch.opcode != LIROpcode.Branch
				&& branch.opcode != LIROpcode.CondFloatBranch))
		{
			// not a valid case for backend.opt
			// currently, only blocks that end with two branches (conditional
			// branch followed by unconditional branch) are optimized
			return;
		}

		// now it is guaranteed that the block ends with two branch instructions.
		// the instructions are inserted at the end of the block before these
		// two branches
		int insertIdx = instructions.size() - 2;
		
		{
			for (int i = insertIdx - 1; i >= 0; i--)
			{
				LIRInstruction op = instructions.get(i);
				if ((op.opcode == LIROpcode.Branch
						|| op.opcode == LIROpcode.CondFloatBranch)
						&& ((LIRBranch) op).block() != null)
				{
					throw new Error(
							"block with two successors can have only two branch instructions");
				}
			}
		}

		// setup a list with the lir-instructions of all successors
		for (int i = 0; i < numSux; i++)
		{
			BasicBlock sux = block.succAt(i);
			List<LIRInstruction> suxInstructions =
					sux.getLIRBlock().lir().instructionsList();

			assert suxInstructions.get(0).opcode
					== LIROpcode.Label : "block must start with label";

			if (sux.getNumOfPreds() != 1)
			{
				// this can happen with switch-statements where multiple edges
				// are between the same blocks.
				return;
			}
			assert sux.predAt(0) == block : "invalid control flow";

			// ignore the label at the beginning of the block
			List<LIRInstruction> seq = suxInstructions
					.subList(1, suxInstructions.size());
			edgeInstructionSeqences.add(seq);
		}

		// process LIR instructions while all successors begin with the same
		// instruction
		while (true)
		{
			List<LIRInstruction> seq = edgeInstructionSeqences.get(0);
			if (seq.isEmpty())
			{
				return;
			}

			LIRInstruction op = first(seq);
			for (int i = 1; i < numSux; i++)
			{
				List<LIRInstruction> otherSeq = edgeInstructionSeqences.get(i);
				if (otherSeq.isEmpty() || !same(op, first(otherSeq)))
				{
					// these instructions are different and cannot be optimized .
					// no further backend.opt possible
					return;
				}
			}

			// insert instruction at end of current block
			block.getLIRBlock().lir().insertBefore(insertIdx, op);
			insertIdx++;

			// delete the instructions at the beginning of all successors
			for (int i = 0; i < numSux; i++)
			{
				seq = edgeInstructionSeqences.get(i);
				removeFirst(seq);
			}
		}
	}

	/**
	 * Gets the first element from a LIR instruction sequence.
	 */
	private static LIRInstruction first(List<LIRInstruction> seq)
	{
		return seq.get(0);
	}

	/**
	 * Gets the last element from a LIR instruction sequence.
	 */
	private static LIRInstruction last(List<LIRInstruction> seq)
	{
		return seq.get(seq.size() - 1);
	}

	/**
	 * Removes the first element from a LIR instruction sequence.
	 */
	private static void removeFirst(List<LIRInstruction> seq)
	{
		seq.remove(0);
	}

	/**
	 * Removes the last element from a LIR instruction sequence.
	 */
	private static void removeLast(List<LIRInstruction> seq)
	{
		seq.remove(seq.size() - 1);
	}
}
