package lir;

import asm.Label;
import hir.BasicBlock;
import hir.Condition;
import hir.Method;
import hir.Operator;
import lir.alloc.LIRInsertionBuffer;
import lir.ci.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Jianping Zeng
 */
public class LIRList
{
	private List<LIRInstruction> operations;
	private final LIRGenerator generator;

	public LIRList(LIRGenerator lirGenerator)
	{
		this.generator = lirGenerator;
		operations = new ArrayList<>(16);
	}

	public void branchDesination(Label label)
	{
		append(new LIRLabel(label));
	}

	private void append(LIRInstruction inst)
	{
		operations.add(inst);
	}

	public void move(LIRValue src, LIRValue dest)
	{
		append(new LIROp1(LIROpcode.Move, src, dest, dest.kind));
	}
	public void unalignedMove(LIRValue src, LIRAddress dest)
	{
		append(new LIROp1(LIROpcode.Move, src, dest, src.kind,
				LIROp1.LIRMoveKind.Unaligned));
	}
	public void lrem(LIRValue left, LIRValue right, LIRValue res,
			LIRValue tmp)
	{
		append(new LIROp3(LIROpcode.Lrem, left, right, tmp, res));
	}

	public void ldiv(LIRValue left, LIRValue right, LIRValue res,
			LIRValue tmp)
	{
		append(new LIROp3(LIROpcode.Ldiv, left, right, tmp, res));
	}

	public void add(LIRValue left, LIRValue right, LIRValue result)
	{
		append(new LIROp2(LIROpcode.Add, left, right, result));
	}

	public void mul(LIRValue left, LIRValue right, LIRValue result)
	{
		append(new LIROp2(LIROpcode.Mul, left, right, result));
	}

	public void sub(LIRValue left, LIRValue right, LIRValue result)
	{
		append(new LIROp2(LIROpcode.Sub, left, right, result));
	}

	public void irem(LIRValue left, LIRValue right, LIRValue res,
			LIRValue tmp)
	{
		append(new LIROp3(LIROpcode.Irem, left, right, tmp, res));
	}

	public void idiv(LIRValue left, LIRValue right, LIRValue res,
			LIRValue tmp)
	{
		append(new LIROp3(LIROpcode.Idiv, left, right, tmp, res));
	}

	public void cmp(Condition opcode, LIRValue left, LIRValue right)
	{
		append(new LIROp2(LIROpcode.Cmp, opcode, left, right));
	}

	public void negate(LIRValue src, LIRValue dest)
	{
		LIRNegate op = new LIRNegate(src, dest);
		append(op);
	}

	public void shiftLeft(LIRValue value, int count, LIRValue dst)
	{
		shiftLeft(value, LIRConstant.forInt(count), dst, LIRValue.IllegalValue);
	}

	public void shiftLeft(LIRValue value, LIRValue count, LIRValue dst,
			LIRValue tmp)
	{
		append(new LIROp2(LIROpcode.Shl, value, count, dst, tmp));
	}

	public void shiftRight(LIRValue value, LIRValue count, LIRValue dst,
			LIRValue tmp)
	{
		append(new LIROp2(LIROpcode.Shr, value, count, dst, tmp));
	}

	public void unsignedShiftRight(LIRValue value, LIRValue count, LIRValue dst,
			LIRValue tmp)
	{
		append(new LIROp2(LIROpcode.Ushr, value, count, dst, tmp));
	}

	public void div(LIRValue left, LIRValue right, LIRValue res)
	{
		append(new LIROp2(LIROpcode.Div, left, right, res));
	}

	public void logicalAnd(LIRValue left, LIRValue right, LIRValue dst)
	{
		append(new LIROp2(LIROpcode.LogicAnd, left, right, dst));
	}

	public void logicalOr(LIRValue left, LIRValue right, LIRValue dst)
	{
		append(new LIROp2(LIROpcode.LogicOr, left, right, dst));
	}

	public void logicalXor(LIRValue left, LIRValue right, LIRValue dst)
	{
		append(new LIROp2(LIROpcode.LogicXor, left, right, dst));
	}

	public void fcmp2int(LIRValue left, LIRValue right, LIRValue dst)
	{
		append(new LIROp2(LIROpcode.Ucmpfd2i, left, right, dst));
	}
	public void lcmp2int(LIRValue left, LIRValue right, LIRValue dst)
	{
		append(new LIROp2(LIROpcode.Cmpl2i, left, right, dst));
	}

	public void jump(BasicBlock target)
	{
		append(new LIRBranch(Condition.TRUE, LIRKind.Illegal, target));
	}

	public void branch(Condition cond, LIRKind kind, BasicBlock trueTarget,
			BasicBlock falseTarget)
	{
		assert kind == LIRKind.Float || kind == LIRKind.Double :
				"fp comparisons only";
		append(new LIRBranch(cond, kind, trueTarget, falseTarget));
	}

	public void branch(Condition cond, LIRKind kind, BasicBlock trueTarget)
	{
		assert kind != LIRKind.Float && kind != LIRKind.Double :
				"no fp comparisons";
		append(new LIRBranch(cond, kind, trueTarget));
	}
	public void branch(Condition cond, Label lbl)
	{
		append(new LIRBranch(cond, lbl));
	}

	public void branchDestination(Label lbl)
	{
		append(new LIRLabel(lbl));
	}

	public List<LIRInstruction> instructionsList()
	{
		return Collections.unmodifiableList(operations);
	}

	public void alloca(StackFrame.StackBlock stackBlock, LIRValue dst)
	{
		append(new LIRStackAllocate(dst, stackBlock));
	}

	public void convert(Operator opcode, LIRValue left, LIRVariable result)
	{
		append(new LIRConvert(opcode, left, result));
	}

	public void move_wide(LIRValue src, LIRAddress dest)
	{
		move(src, dest);
	}

	public void callDirect(Method target, LIRVariable result, LIRValue[] args,
			LIRValue[] locations)
	{
		append(new LIRCall(LIROpcode.DirectCall, target, result,
				Arrays.asList(args), false,
				Arrays.asList(locations)));
	}

	public void returnOp(LIRValue result)
	{
		append(new LIROp1(LIROpcode.Return, result));
	}

	public void append(LIRInsertionBuffer buffer)
	{
		assert this == buffer.lirList() : "wrong lir list";
		int n = operations.size();

		if (buffer.numberOfOps() > 0) {
			// increase size of instructions list
			for (int i = 0; i < buffer.numberOfOps(); i++) {
				operations.add(null);
			}
			// insert ops from buffer into instructions list
			int opIndex = buffer.numberOfOps() - 1;
			int ipIndex = buffer.numberOfInsertionPoints() - 1;
			int fromIndex = n - 1;
			int toIndex = operations.size() - 1;
			for (; ipIndex >= 0; ipIndex--) {
				int index = buffer.indexAt(ipIndex);
				// make room after insertion point
				while (index < fromIndex) {
					operations.set(toIndex--, operations.get(fromIndex--));
				}
				// insert ops from buffer
				for (int i = buffer.countAt(ipIndex); i > 0; i--) {
					operations.set(toIndex--, buffer.opAt(opIndex--));
				}
			}
		}

		buffer.finish();
	}

	public void insertBefore(int i, LIRInstruction op)
	{
		operations.add(i, op);
	}

	public void phi(LIRValue[] args, BasicBlock[] blocks, LIRValue result)
	{
		append(new LIRPhi(LIROpcode.Phi, args, blocks, result));
	}
}
