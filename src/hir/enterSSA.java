package hir;

import hir.HIRGenerator.TempNameGenerator;
import jdk.internal.org.objectweb.asm.tree.analysis.Value;

import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

/**
 * <p>
 * This class served as an auxiliary to tansform traditional typeed 3-address
 * instruction into IR in SSA form.
 * </p>
 * <p>
 * The method this class taken is derived from Matthias
 * Braun(matthias.braun@kit.edu)' literature,Simple and Efficient Construction
 * of Static Single Assignment Form.
 * </p>
 * Created by Jianping Zeng<z1215jping@hotmail.com> on 2016/2/26.
 */
public class EnterSSA
{
	private HashMap<BasicBlock, ValueMap> valueMaps;
	/**
	 * The size of valeuMaps by default.
	 */
	private final int defaultSize = 10;
	private ControlFlowGraph cfg;

	private GlobalValueNumber gvn;

	/**
	 * For numbering at value numbering.
	 */
	private volatile int id = 0;
	/**
	 * The list of basic block in order that first insertion first visited.
	 */
	private List<BasicBlock> preorderBlocks;

	private TempNameGenerator indexer;

	public EnterSSA(ControlFlowGraph cfg, TempNameGenerator indexer)
	{
		this.cfg = cfg;
		this.indexer = indexer;
		this.valueMaps = new HashMap<>(defaultSize);
		preorderBlocks = cfg.reversePostOrder();
		gvn = new GlobalValueNumber();
		enter();
	}

	private void enter()
	{
		// performs algorithm iterating over list of blocks in reverse postorder
		for (BasicBlock currBB : preorderBlocks)
		{
			this.valueMaps.put(currBB, new ValueMap());
			// begin transforming
			gvn.tranform(currBB);
		}
	}

	/**
	 * An internal class for global value numbering for transforming traditional
	 * instruction into SSA form.
	 */
	private class GlobalValueNumber extends InstructionVisitor
	{
		private BasicBlock currentBlock;

		/**
		 * Constructor.
		 *
		 * @param block The current block.
		 */
		public void tranform(BasicBlock block)
		{
			this.currentBlock = block;

			for (ListIterator<Instruction> it = block.iterator(); it
					.hasNext(); )
			{
				Instruction cur = it.next();
				cur.accept(this);
			}
		}

		private void writeVariable(Instruction inst, BasicBlock block,
				int value)
		{

		}

		private Instruction readVariable(Instruction inst, BasicBlock block)
		{
			// looks up for inst at current block's valueMap
			ValueMap currentMap = valueMaps.get(block);
			currentMap.findInsert(inst);

			return null;
		}

		@Override
		public void visitInstruction(Instruction inst)
		{
			assert false : "It should not reach here.";
		}

		@Override
		public void visitADD_I(Instruction.ADD_I inst)
		{
			Instruction l = readVariable(inst.x, currentBlock);
			Instruction r = readVariable(inst.y, currentBlock);

		}

		@Override
		public void visitSUB_I(Instruction.SUB_I inst)
		{
			visitInstruction(inst);
		}

		@Override
		public void visitMUL_I(Instruction.MUL_I inst)
		{
			visitInstruction(inst);
		}

		@Override
		public void visitDIV_I(Instruction.DIV_I inst)
		{
			visitInstruction(inst);
		}

		@Override
		public void visitMOD_I(Instruction.MOD_I inst)
		{
			visitInstruction(inst);
		}

		@Override
		public void visitAND_I(Instruction.AND_I inst)
		{
			visitInstruction(inst);
		}

		@Override
		public void visitOR_I(Instruction.OR_I inst)
		{
			visitInstruction(inst);
		}

		@Override
		public void visitXOR_I(Instruction.XOR_I inst)
		{
			visitInstruction(inst);
		}

		@Override
		public void visitSHL_I(Instruction.SHL_I inst)
		{
			visitInstruction(inst);
		}

		@Override
		public void visitSHR_I(Instruction.SHR_I inst)
		{
			visitInstruction(inst);
		}

		@Override
		public void visitUSHR_I(Instruction.USHR_I inst)
		{
			visitInstruction(inst);
		}

		@Override
		public void visitADD_L(Instruction.ADD_L inst)
		{
			visitInstruction(inst);
		}

		@Override
		public void visitSUB_L(Instruction.SUB_L inst)
		{
			visitInstruction(inst);
		}

		@Override
		public void visitMUL_L(Instruction.MUL_L inst)
		{
			visitInstruction(inst);
		}

		@Override
		public void visitDIV_L(Instruction.DIV_L inst)
		{
			visitInstruction(inst);
		}

		@Override
		public void visitMOD_L(Instruction.MOD_L inst)
		{
			visitInstruction(inst);
		}

		@Override
		public void visitAND_L(Instruction.AND_L inst)
		{
			visitInstruction(inst);
		}

		@Override
		public void visitOR_L(Instruction.OR_L inst)
		{
			visitInstruction(inst);
		}

		@Override
		public void visitXOR_L(Instruction.XOR_L inst)
		{
			visitInstruction(inst);
		}

		@Override
		public void visitSHL_L(Instruction.SHL_L inst)
		{
			visitInstruction(inst);
		}

		@Override
		public void visitSHR_L(Instruction.SHR_L inst)
		{
			visitInstruction(inst);
		}

		@Override
		public void visitUSHR_L(Instruction.USHR_L inst)
		{
			visitInstruction(inst);
		}
		@Override
		public void visitADD_F(Instruction.ADD_F inst)
		{
			visitInstruction(inst);
		}

		@Override
		public void visitSUB_F(Instruction.SUB_F inst)
		{
			visitInstruction(inst);
		}

		@Override
		public void visitMUL_F(Instruction.MUL_F inst)
		{
			visitInstruction(inst);
		}

		@Override
		public void visitDIV_F(Instruction.DIV_F inst)
		{
			visitInstruction(inst);
		}

		@Override
		public void visitMOD_F(Instruction.MOD_F inst)
		{
			visitInstruction(inst);
		}

		@Override
		public void visitADD_D(Instruction.ADD_D inst)
		{
			visitInstruction(inst);
		}

		@Override
		public void visitSUB_D(Instruction.SUB_D inst)
		{
			visitInstruction(inst);
		}

		@Override
		public void visitMUL_D(Instruction.MUL_D inst)
		{
			visitInstruction(inst);
		}

		@Override
		public void visitDIV_D(Instruction.DIV_D inst)
		{
			visitInstruction(inst);
		}

		@Override
		public void visitMOD_D(Instruction.MOD_D inst)
		{
			visitInstruction(inst);
		}

		@Override
		public void visitNEG_I(Instruction.NEG_I inst)
		{
			visitInstruction(inst);
		}

		@Override
		public void visitNEG_L(Instruction.NEG_L inst)
		{
			visitInstruction(inst);
		}

		@Override
		public void visitNEG_F(Instruction.NEG_F inst)
		{
			visitInstruction(inst);
		}

		@Override
		public void visitNEG_D(Instruction.NEG_D inst)
		{
			visitInstruction(inst);
		}
		@Override
		public void visitINT_2LONG(Instruction.INT_2LONG inst)
		{
			visitInstruction(inst);
		}
		@Override
		public void visitINT_2FLOAT(Instruction.INT_2FLOAT inst)
		{
			visitInstruction(inst);
		}
		@Override
		public void visitINT_2DOUBLE(Instruction.INT_2DOUBLE inst)
		{
			visitInstruction(inst);
		}
		@Override
		public void visitLONG_2INT(Instruction.LONG_2INT inst)
		{
			visitInstruction(inst);
		}
		@Override
		public void visitLONG_2FLOAT(Instruction.LONG_2FLOAT inst)
		{
			visitInstruction(inst);
		}
		@Override
		public void visitLONG_2DOUBLE(Instruction.LONG_2DOUBLE inst)
		{
			visitInstruction(inst);
		}
		@Override
		public void visitFLOAT_2INT(Instruction.FLOAT_2INT inst)
		{
			visitInstruction(inst);
		}
		@Override
		public void visitFLOAT_2LONG(Instruction.FLOAT_2LONG inst)
		{
			visitInstruction(inst);
		}
		@Override
		public void visitFLOAT_2DOUBLE(Instruction.FLOAT_2DOUBLE inst)
		{
			visitInstruction(inst);
		}
		@Override
		public void visitDOUBLE_2INT(Instruction.DOUBLE_2INT inst)
		{
			visitInstruction(inst);
		}
		@Override
		public void visitDOUBLE_2LONG(Instruction.DOUBLE_2LONG inst)
		{
			visitInstruction(inst);
		}
		@Override
		public void visitDOUBLE_2FLOAT(Instruction.DOUBLE_2FLOAT inst)
		{
			visitInstruction(inst);
		}
		@Override
		public void visitINT_2BYTE(Instruction.INT_2BYTE inst)
		{
			visitInstruction(inst);
		}
		@Override
		public void visitINT_2CHAR(Instruction.INT_2CHAR inst)
		{
			visitInstruction(inst);
		}
		@Override
		public void visitINT_2SHORT(Instruction.INT_2SHORT inst)
		{
			visitInstruction(inst);
		}
		@Override
		public void visitIfCmp_LT(Instruction.IfCmp_LT inst)
		{
			visitInstruction(inst);
		}
		@Override
		public void visitIfCmp_LE(Instruction.IfCmp_LE inst)
		{
			visitInstruction(inst);
		}
		@Override
		public void visitIfCmp_GT(Instruction.IfCmp_GT inst)
		{
			visitInstruction(inst);
		}
		@Override
		public void visitIfCmp_GE(Instruction.IfCmp_GE inst)
		{
			visitInstruction(inst);
		}
		@Override
		public void visitIfCmp_EQ(Instruction.IfCmp_EQ inst)
		{
			visitInstruction(inst);
		}
		@Override
		public void visitIfCmp_NEQ(Instruction.IfCmp_NEQ inst)
		{
			visitInstruction(inst);
		}
		@Override
		public void visitGoto(Instruction.Goto inst)
		{
			visitInstruction(inst);
		}

		@Override
		public void visitReturn(Instruction.Return inst)
		{
			visitInstruction(inst);
		}

		@Override
		public void visitInvoke(Instruction.Invoke inst)
		{
			visitInstruction(inst);
		}
		@Override
		public void visitPhi(Instruction.Phi inst)
		{
			visitInstruction(inst);
		}
		@Override
		public void visitConstant(Instruction.Constant inst)
		{
			visitInstruction(inst);
		}
		@Override
		public void visitAlloca(Instruction.Alloca inst)
		{
			visitInstruction(inst);
		}
		@Override
		public void visitLocal(Instruction.Local inst)
		{
			visitInstruction(inst);
		}
		@Override
		public void visitStoreInst(Instruction.StoreInst inst)
		{
			visitInstruction(inst);
		}
		@Override
		public void visitLoadInst(Instruction.LoadInst inst)
		{
			Instruction from = readVariable(inst.from, currentBlock);
			from.number = id++;

		}
	}


}
