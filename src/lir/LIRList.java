package lir;

import asm.Label;
import hir.BasicBlock;
import hir.Condition;
import hir.Operator;
import lir.ci.*;

import java.util.ArrayList;
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

	public void move(CiValue src, CiValue dest)
	{
		append(new LIROp1(LIROpcode.Move, src, dest, dest.kind));
	}

	public void lrem(CiValue dividend, CiValue divisor, CiValue resultReg,
			CiRegisterValue ldivTmp)
	{

	}

	public void ldiv(CiValue dividend, CiValue divisor, CiValue resultReg,
			CiRegisterValue ldivTmp)
	{

	}

	public void add(CiValue leftOp, CiValue right, CiValue result)
	{

	}

	public void mul(CiValue leftOp, CiValue right, CiValue result)
	{

	}

	public void sub(CiValue leftOp, CiValue right, CiValue result)
	{

	}

	public void irem(CiValue dividend, CiValue divisor, CiValue resultReg,
			CiRegisterValue tmp)
	{

	}

	public void idiv(CiValue dividend, CiValue divisor, CiValue resultReg,
			CiRegisterValue tmp)
	{

	}

	public void cmp(Condition opcode, CiValue left, CiValue right)
	{

	}

	public void negate(CiValue src, CiValue dest)
	{
		LIRNegate op = new LIRNegate(src, dest);
		append(op);
	}

	public void shiftLeft(CiValue value, int count, CiValue dst)
	{
		shiftLeft(value, CiConstant.forInt(count), dst, CiValue.IllegalValue);
	}

	public void shiftLeft(CiValue value, CiValue count, CiValue dst,
			CiValue tmp)
	{
		append(new LIROp2(LIROpcode.Shl, value, count, dst, tmp));
	}

	public void shiftRight(CiValue value, CiValue count, CiValue dst,
			CiValue tmp)
	{
		append(new LIROp2(LIROpcode.Shr, value, count, dst, tmp));
	}

	public void unsignedShiftRight(CiValue value, CiValue count, CiValue dst,
			CiValue tmp)
	{
		append(new LIROp2(LIROpcode.Ushr, value, count, dst, tmp));
	}



	public void div(CiValue left, CiValue right, CiValue res)
	{
		append(new LIROp2(LIROpcode.Div, left, right, res));
	}

	public void logicalAnd(CiValue left, CiValue right, CiValue dst)
	{
		append(new LIROp2(LIROpcode.LogicAnd, left, right, dst));
	}

	public void logicalOr(CiValue left, CiValue right, CiValue dst)
	{
		append(new LIROp2(LIROpcode.LogicOr, left, right, dst));
	}

	public void logicalXor(CiValue left, CiValue right, CiValue dst)
	{
		append(new LIROp2(LIROpcode.LogicXor, left, right, dst));
	}

	public void fcmp2int(CiValue left, CiValue right, CiValue dst)
	{
		append(new LIROp2(LIROpcode.Ucmpfd2i, left, right, dst));
	}
	public void lcmp2int(CiValue left, CiValue right, CiValue dst)
	{
		append(new LIROp2(LIROpcode.Cmpl2i, left, right, dst));
	}

	public void jump(BasicBlock target)
	{
		append(new LIRBranch(Condition.TRUE, CiKind.Illegal, target));
	}

	public void branch(Condition cond, CiKind kind, BasicBlock trueTarget,
			BasicBlock falseTarget)
	{
		assert kind == CiKind.Float || kind == CiKind.Double :
				"fp comparisons only";
		append(new LIRBranch(cond, kind, trueTarget, falseTarget));
	}

	public void branch(Condition cond, CiKind kind, BasicBlock trueTarget)
	{
		assert kind != CiKind.Float && kind != CiKind.Double :
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

	public void alloca(FrameMap.StackBlock stackBlock, CiValue dst)
	{
		append(new LIRStackAllocate(dst, stackBlock));
	}

	public void convert(Operator opcode, CiValue left, CiVariable result)
	{
		append(new LIRConvert(opcode, left, result));
	}
}
