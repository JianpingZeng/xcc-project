package lir;

import asm.Label;
import hir.BasicBlock;
import hir.Condition;
import hir.Method;
import hir.Operator;
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
	public void lrem(LIRValue dividend, LIRValue divisor, LIRValue resultReg,
			LIRRegisterValue ldivTmp)
	{

	}

	public void ldiv(LIRValue dividend, LIRValue divisor, LIRValue resultReg,
			LIRRegisterValue ldivTmp)
	{

	}

	public void add(LIRValue leftOp, LIRValue right, LIRValue result)
	{

	}

	public void mul(LIRValue leftOp, LIRValue right, LIRValue result)
	{

	}

	public void sub(LIRValue leftOp, LIRValue right, LIRValue result)
	{

	}

	public void irem(LIRValue dividend, LIRValue divisor, LIRValue resultReg,
			LIRRegisterValue tmp)
	{

	}

	public void idiv(LIRValue dividend, LIRValue divisor, LIRValue resultReg,
			LIRRegisterValue tmp)
	{

	}

	public void cmp(Condition opcode, LIRValue left, LIRValue right)
	{

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
}
