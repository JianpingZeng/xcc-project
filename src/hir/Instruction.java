package hir;

import lir.ci.LIRKind;
import utils.Pair;
import utils.Util;

/**
 * This class is an abstract representation of Quadruple. In this class,
 * subclass of @ {@code Instruction} represents arithmetic and logical
 * operation, control flow operators,Phi assignment, function calling
 * conditional statement.
 *
 * @author Jianping Zeng
 * @version 1.0
 * @see BasicBlock
 * @see User
 * @see Value
 * @see Use
 */
public abstract class Instruction extends User
{
	/**
	 * Mainly for register allocation.
	 */
	public int id;

	public final Operator opcode;

	/**
	 * The ret of operation over this instruction, it is null if this instruction
	 * no return ret.
	 */
	//public Instruction ret;

	private BasicBlock bb;

	/**
	 * The name of this instruction.
	 */
	protected String instName;

	public BasicBlock getParent()
	{
		return bb;
	}

	public void setParent(BasicBlock bb)
	{
		this.bb = bb;
	}

	public Instruction(LIRKind kind, Operator opcode, String instName)
	{
		super(kind);
		this.id = -1;
		this.opcode = opcode;
		this.instName = instName;
	}

	/**
	 * Erases this instruction from it's parent basic block.
	 */
	public void eraseFromBasicBlock()
	{
		assert (this.bb
				== null) : "The basic block where the instruction reside to be erased!";
		bb.removeInst(this);
	}

	/**
	 * An interface for ValueVisitor invoking.
	 *
	 * @param visitor The instance of ValueVisitor.
	 */
	public abstract void accept(ValueVisitor visitor);

	/**
	 * Gets the text format of this Instruction.
	 *
	 * @return
	 */
	public String toString()
	{
		if (opcode != null)
			return opcode.opName;
		else
			return "";
	}

	/**
	 * For value number to determine whether this instruction is equivalent to
	 * that value.
	 *
	 * @param value Targeted instruction to be checked.
	 * @return return false by default.
	 */
	public boolean valueEqual(Instruction value)
	{
		return false;
	}

	/**
	 * For global or local inst numbering with initialization 0.
	 */
	public int valueNumber()
	{
		return 0;
	}

	/**
	 * Inserts an specified instruction into the instructions list after itself.
	 *
	 * @param inst An instruction to be inserted.
	 */
	public void insertAfter(Instruction inst)
	{
		int index = bb.lastIndexOf(inst);
		if (index >= 0 && index < bb.size())
			bb.addInst(inst, index + 1);
	}

	/**
	 * Inserts an instruction into the instructions list before this itself.
	 *
	 * @param inst An instruction to be inserted.
	 */
	public void insertBefore(Instruction inst)
	{
		int index = bb.lastIndexOf(inst);
		if (index >= 0 && index < bb.size())
			bb.addInst(inst, index);
	}

	/**
	 * Links the user value and its user.
	 *
	 * @param v The used {@code Value}.
	 * @param u The {@code Instruction} or other instance of {@code User}.
	 */
	void use(Value v, User u)
	{
		assert v != null && u != null;
		v.addUse(new Use(v, u));
	}

	/**
	 * Obtains the name of this instruction.
	 *
	 * @return
	 */
	public String getName()
	{
		return instName;
	}

	/**
	 * The abstract base class definition for unary operator.
	 */
	public static abstract class Op1 extends Instruction
	{
		/**
		 * The field represents first LIROperand of this instruction.
		 */
		public Value x;

		/**
		 * Constructs unary operation.
		 *
		 * @param kind   The inst kind of ret.
		 * @param opcode The operator code for this instruction.
		 * @param x      The sole LIROperand.
		 */
		public Op1(LIRKind kind, Operator opcode, Value x, String name)
		{
			super(kind, opcode, name);
			this.x = x;
			use(x, this);
		}

		@Override public int valueNumber()
		{
			return Util.hash1(opcode.index, x);
		}

	}

	/**
	 * This class just for binary operation definition.
	 *
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 */
	public static abstract class Op2 extends Instruction
	{
		/**
		 * The two field represents first or second LIROperand of this instruction
		 * respectively.
		 */
		public Value x, y;

		public Op2(LIRKind kind, Operator opcode, Value x, Value y, String name)
		{
			super(kind, opcode, name);
			this.x = x;
			this.y = y;

			assert x.kind
					== y.kind : "Cannot create binary operator with two operands of differing type!";
			use(x, this);
			use(y, this);
		}

		/**
		 * This method is used for attempting to swap the two operands of this
		 * binary instruction.
		 */
		public void swapOperands()
		{
			Value temp = x;
			x = y;
			y = temp;
		}

		@Override public int valueNumber()
		{
			return Util.hash2(opcode.index, x, y);
		}
	}

	public static class ArithmeticOp extends Op2
	{
		public ArithmeticOp(LIRKind kind, Operator opcode, Value x, Value y,
				String name)
		{
			super(kind, opcode, x, y, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitArithmeticOp(this);
		}
	}

	public static class LogicOp extends Op2
	{
		public LogicOp(LIRKind kind, Operator opcode, Value x, Value y,
				String name)
		{
			super(kind, opcode, x, y, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitLogicOp(this);
		}
	}

	public static class ShiftOp extends Op2
	{

		public ShiftOp(LIRKind kind, Operator opcode, Value x, Value y,
				String name)
		{
			super(kind, opcode, x, y, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitShiftOp(this);
		}
	}

	public static class Negate extends Op1
	{
		public Negate(LIRKind kind, Value x, String name)
		{
			super(kind, Operator.INeg, x, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitNegate(this);
		}
	}

	public static class Convert extends Op1
	{
		public Convert(LIRKind kind, Operator opcode, Value x, String name)
		{
			super(kind, opcode, x, name);
		}

		/**
		 * An interface for ValueVisitor invoking.
		 *
		 * @param visitor The instance of ValueVisitor.
		 */
		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitConvert(this);
		}
	}

	/**
	 * An abstract representation of branch instruction.
	 *
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 */
	public static abstract class Branch extends Instruction
	{
		/**
		 * Constructs a new branch instruction with specified inst type.
		 *
		 * @param kind
		 */
		public Branch(LIRKind kind, Operator opcode, String name)
		{
			super(kind, opcode, name);
		}
	}

	public static abstract class ConditionalBranch extends Branch
	{
		public BasicBlock trueTarget, falseTarget;

		/**
		 * Constructs a new conditional branch instruction with specified inst type.
		 *
		 * @param kind
		 */
		public ConditionalBranch(LIRKind kind, Operator opcode, String name)
		{
			super(kind, opcode, name);
		}
	}

	public static class IfOp extends ConditionalBranch
	{
		private Condition cond;
		/**
		 * The first LIROperand also that means left-most of this branch operation.
		 */
		Value x;
		/**
		 * The second LIROperand also that means right-most of this branch
		 * operation.
		 */
		Value y;

		IfOp(Value x, Value y, BasicBlock trueTarget, BasicBlock falseTarget,
				String name, Condition cond)
		{
			super(LIRKind.Illegal, Operator.Br, name);
			this.x = x;
			this.y = y;
			this.trueTarget = trueTarget;
			this.falseTarget = falseTarget;
			this.cond = cond;
		}

		/**
		 * Gets the first LIROperand.
		 *
		 * @return
		 */
		public Value x()
		{
			return x;
		}

		/**
		 * The second LIROperand.
		 *
		 * @return
		 */
		public Value y()
		{
			return y;
		}

		/**
		 * Gets the one of two direction corresponding to condition is true.
		 *
		 * @return
		 */
		public BasicBlock getTrueTarget()
		{
			return trueTarget;
		}

		/**
		 * Gets the one of two direction corresponding to condition is false.
		 *
		 * @return
		 */
		public BasicBlock getFalseTarget()
		{
			return falseTarget;
		}

		/**
		 * Return the corresponding targetAbstractLayer block determinated by condition.
		 * If the condition is true, the True TargetAbstractLayer is returned, otherwise
		 * return false targetAbstractLayer.
		 *
		 * @param istrue
		 * @return
		 */
		public BasicBlock successor(boolean istrue)
		{
			return istrue ? trueTarget : falseTarget;
		}

		/**
		 * An interface for ValueVisitor invoking.
		 *
		 * @param visitor The instance of ValueVisitor.
		 */
		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitIfOp(this);
		}

		/**
		 * Swaps the LIROperand and reverse the condition (e.g.<= --> >)
		 *
		 * @return
		 */
		public IfOp getMirror()
		{
			return new IfOp(y, x, trueTarget, falseTarget, instName,
					cond.mirror());
		}

		public Condition condition()
		{
			return cond;
		}
	}

	public static class Cmp extends Op2
	{
		private Condition cond;

		/**
		 * Creates a instance of different subclass served as different
		 * date type according to the date type.
		 *
		 * @param kind  The ret date type.
		 * @param left  The left LIROperand.
		 * @param right the right LIROperand.
		 * @param cond  The condition object.
		 * @return According comparison instruction.
		 */
		public Cmp(LIRKind kind, Value left, Value right, Condition cond,
				String name)
		{
			super(kind, Operator.Cmp, left, right, name);
			this.cond = cond;
		}

		/**
		 * An interface for ValueVisitor invoking.
		 *
		 * @param visitor The instance of ValueVisitor.
		 */
		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitCompare(this);
		}
		
		public Condition condition()
        {
	        return this.cond;
        }
	}

	/**
	 * The {@code Goto} instruction represents the end of a block that
	 * unconditional branches to another basic block.
	 *
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 */
	public static class Goto extends Branch
	{
		/**
		 * The jump targetAbstractLayer of this goto instruction.
		 */
		public BasicBlock target;

		/**
		 * Constructs a new {@code Goto} instruction with specified jump targetAbstractLayer.
		 *
		 * @param target The targetAbstractLayer block of this unconditional jump.
		 */
		public Goto(BasicBlock target, String name)
		{
			super(LIRKind.Illegal, Operator.Goto, name);
			this.target = target;
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitGoto(this);
		}
	}

	/**
	 * This {@code Return} class definition.
	 *
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 */
	public static class Return extends Instruction
	{
		Value ret;

		/**
		 * Constructs a new return instruction with return inst.
		 *
		 * @param retValue The return inst produce for this instruction, return
		 *                 void if ret is {@code null}.
		 */
		public Return(Value retValue, String name)
		{
			super(retValue == null ? LIRKind.Void : retValue.kind, Operator.Ret,
					name);
			this.ret = retValue;
			if (ret != null)
				use(ret, this);
		}

		/**
		 * Gets the instruction that produces the ret for the return.
		 *
		 * @return the instruction producing the ret
		 */
		public Value result()
		{
			return ret;
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitReturn(this);
		}
	}

	/**
	 * Method invocation instruction.
	 *
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 */
	public static class Invoke extends Instruction
	{
		/**
		 * The input arguments for function calling.
		 */
		public final Value[] arguments;
		/**
		 * The targetAbstractLayer of this method calling.
		 */
		public final Method target;

		public Value ret;

		/**
		 * Constructs a new method calling instruction.
		 *
		 * @param result The kind of return ret.
		 * @param args   The input arguments.
		 * @param target The called method.
		 */
		public Invoke(LIRKind result, Value[] args, Method target, String name)
		{
			super(result, Operator.Invoke, name);
			this.target = target;
			this.arguments = args;
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitInvoke(this);
		}

		@Override public String toString()
		{
			return null;
		}

		public int getNumsOfArgs()
		{
			return arguments.length;
		}

		public Value argumentAt(int i)
		{
			assert i >= 0 && i < arguments.length;
			return arguments[i];
		}
	}

	/**
	 * The {@code Phi} instruction represents the merging of data flow in the
	 * instruction graph. It refers to a join block and a variable.
	 *
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 */
	public static class Phi extends Instruction
	{
		/**
		 * The derived basic block from owned inputed parameter of this Phi
		 * assignment.
		 * <p>
		 * The parameters list of Phi assignment.
		 */
		private Pair<Value, BasicBlock>[] inputs;

		private int currIndex = 0;

		/**
		 * Constructs a new Phi-function instruction.
		 *
		 * @param kind   The kind of ret.
		 * @param args   The input arguments.
		 * @param blocks The one of which basic block array is corresponding to
		 *               an input argument.
		 */
		public Phi(LIRKind kind, Value[] args, BasicBlock[] blocks, String name)
		{
			super(kind, Operator.Phi, name);
			assert args.length == blocks.length;
			inputs = new Pair[args.length];
			for (int idx = 0; idx < args.length; idx++)
			{
				inputs[idx] = new Pair<>(args[idx], blocks[idx]);
				// make a chain between value definition and value use
				use(args[idx], this);
			}
		}

		public Phi(LIRKind kind, int length)
		{
			super(kind, Operator.Phi, "");
			this.inputs = new Pair[length];
		}

		public Phi(LIRKind kind, int length, String nameString)
		{
			super(kind, Operator.Phi, nameString);
			this.inputs = new Pair[length];
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitPhi(this);
		}

		/**
		 * Appends a pair that consists of both value and block into argument list.
		 *
		 * @param value The instruction that phi parameter to be inserted
		 * @param block The according block of corresponding phi parameter.
		 */
		public void addIncoming(Value value, BasicBlock block)
		{
			if (value != null && block != null && currIndex < inputs.length)
			{
				this.inputs[currIndex++] = new Pair<>(value, block);
			}
		}

		/**
		 * Gets the inputed parameter at given position.
		 *
		 * @param index The position where input parameter will be obtained.
		 * @return The input parameter at specified position.
		 */
		public Value getIncomingValue(int index)
		{
			assert index >= 0 && index
					< inputs.length : "The index is beyond out the num of list";
			return inputs[index].first;
		}

		/**
		 * Gets the input block at given position.
		 *
		 * @param index The position where input block will be obtained.
		 * @return The input block at specified position.
		 */
		public BasicBlock getBasicBlock(int index)
		{
			assert index >= 0 && index
					< inputs.length : "The index is beyond out the num of list";
			return inputs[index].second;
		}

		/**
		 * Updates the input argument at given position.
		 *
		 * @param index The index into argument to be updated.
		 * @param value
		 */
		public void setParameter(int index, Value value)
		{
			assert index >= 0 && index
					< inputs.length : "The index is beyond out the num of list";

			inputs[index].first = value;
		}

		/**
		 * Updates the input block at given position.
		 *
		 * @param index The index into block to be updated.
		 * @param block
		 */
		public void setBasicBlock(int index, BasicBlock block)
		{
			assert index >= 0 && index
					< inputs.length : "The index is beyond out the num of list";

			inputs[index].second = block;
		}

		@Override public String toString()
		{
			return null;
		}

		public int getBasicBlockIndex(BasicBlock basicBlock)
		{
			assert (basicBlock
					!= null) : "Phi.getBasicBlockIndex(<null>) is invalid";
			for (int idx = 0; idx < inputs.length; idx++)
				if (inputs[idx].second == basicBlock)
					return idx;
			return -1;
		}

		/**
		 * Obtains the numbers of incoming value of phi node.
		 *
		 * @return
		 */
		public int getNumberIncomingValues()
		{
			return currIndex;
		}

		/**
		 * Gets an array that contains all incoming basic blocks.
		 *
		 * @return
		 */
		public BasicBlock[] getAllBasicBlocks()
		{
			BasicBlock[] blocks = new BasicBlock[inputs.length];
			for (int idx = 0; idx < inputs.length; idx++)
				blocks[idx] = inputs[idx].second;
			return blocks;
		}

		/**
		 * Gets the name of this phi node.
		 *
		 * @return
		 */
		public String getName()
		{
			return instName;
		}
	}

	/**
	 * This class was served functionally as allocating memory at stack frame.
	 * <b>Note that </b>all of heap allocation is accomplished by invoking the
	 * C language library function, currently.
	 */
	public static class Alloca extends Instruction
	{
		/**
		 * The number of elements if allocating is used for array.
		 */
		private Value num;

		/**
		 * Creates a new {@linkplain Alloca} HIR that allocates memory for specified
		 * {@LIRKind kind} and the numbers of to be allocated element.
		 * @param kind  The data kind of allocated data which is instance of {@linkplain LIRKind}.
		 * @param num   The number of elements if allocating is used for array.
		 * @param name The name of this instruction for debugging.
		 */
		public Alloca(LIRKind kind, Value num, String name)
		{
			super(kind, Operator.Alloca, name);
			this.num = num;
		}

		/**
		 * Gets the instruction that produced the num argument.
		 */
		public Value length()
		{
			return num;
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitAlloca(this);
		}

		/**
		 * Gets the name of this alloated variable.
		 *
		 * @return
		 */
		public String getName()
		{
			return instName;
		}

		/**
		 * Determine whether this alloca instruction is promoted into
		 * register or not?
		 *
		 * @return Return true if it is pormotable.
		 */
		public boolean isAllocaPromoteable()
		{
			return true;
		}
	}

	/**
	 * An instruction for writing data into memory.
	 */
	public static class StoreInst extends Instruction
	{
		/**
		 * The inst being writed into destination variable.
		 */
		public Value value;
		/**
		 * The targetAbstractLayer of writing.
		 */
		public Alloca dest;

		/**
		 * Constructs a new store instruction.
		 *
		 * @param value The inst to being writed into memory.
		 * @param dest  The targetAbstractLayer memory address where inst stores.
		 */
		public StoreInst(Value value, Alloca dest, String name)
		{
			super(LIRKind.Illegal, Operator.Store, name);
			this.value = value;
			this.dest = dest;

			use(value, this);
			use(dest, this);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitStoreInst(this);
		}
	}

	/**
	 * An instruction for reading data from memory.
	 */
	public static class LoadInst extends Instruction
	{
		/**
		 * The source memory where desired inst reading.
		 */
		public Alloca from;

		public LoadInst(LIRKind kind, Alloca from, String name)
		{
			super(kind, Operator.Load, name);
			this.from = from;

			if (from != null)
				use(from, this);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitLoadInst(this);
		}
	}

	public static class SwitchInst extends Branch
	{
		private Pair<Value, BasicBlock>[] operands;
		private int currIdx = 0;
		private int lowKey, highKey;

		/**
		 * Constructs a new SwitchInst instruction with specified inst type.
		 *
		 * @param condV     the value of selector.
		 * @param defaultBB The default jump block when no other case match.
		 * @param numCases  The numbers of case value.
		 */
		public SwitchInst(Value condV, BasicBlock defaultBB, int numCases,
				String name)
		{
			super(LIRKind.Illegal, Operator.Switch, name);
			operands = new Pair[1 + numCases];
			operands[currIdx++] = new Pair<>(condV, defaultBB);
		}

		/**
		 * An interface for ValueVisitor invoking.
		 *
		 * @param visitor The instance of ValueVisitor.
		 */
		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitSwitch(this);
		}

		public void addCase(Value caseVal, BasicBlock targetBB)
		{
			operands[currIdx++] = new Pair<>(caseVal, targetBB);
		}

		/**
		 * Gets the default basic block where default case clause resides.
		 *
		 * @return The default basic block.
		 */
		public BasicBlock getDefaultBlock()
		{
			return this.operands[0].second;
		}

		public int numsOfCases()
		{
			return operands.length;
		}

		public Value[] getCaseValues()
		{
			Value[] vals = new Value[operands.length];
			for (int idx = 0; idx < vals.length; idx++)
				vals[idx] = operands[idx].first;
			return vals;
		}

		public BasicBlock targetAt(int idx)
		{
			assert idx >= 0 && idx < operands.length;
			return operands[idx].second;
		}

		public int getLowKey()
		{
			return lowKey;
		}

		public int getHighKey()
		{
			return highKey;
		}

		public void setLowKey(int lowKey)
		{
			this.lowKey = lowKey;
		}

		public void setHighKey(int highKey)
		{
			this.highKey = highKey;
		}
	}
}
