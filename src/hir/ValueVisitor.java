package hir;

import hir.Instruction.*;

/**
 * A Value Visitor for Instruction or Value using Visitor pattern.
 * @author Jianping Zeng
 * 
 */
public abstract class ValueVisitor
{
	/**
	 * Visits {@code Instruction} with visitor pattern.
	 * @param inst  The instruction to be visited.
	 */
	public void visitInstruction(Instruction inst) {}

	/**
	 * Visits {@code ADD_I} with visitor pattern.
	 * @param inst  The ADD_I to be visited.
	 */
	public void visitADD_I(ADD_I inst)
	{

	}

	/**
	 * Visits {@code SUB_I} with visitor pattern.
	 * @param inst  The SUB_I to be visited.
	 */
	public void visitSUB_I(SUB_I inst){}

	/**
	 * Visits {@code MUL_I} with visitor pattern.
	 * @param inst  The MUL_I to be visited.
	 */
	public void visitMUL_I(MUL_I inst)
	{

	}

	/**
	 * Visits {@code DIV_I} with visitor pattern.
	 * @param inst  The DIV_I to be visited.
	 */
	public void visitDIV_I(DIV_I inst){}

	/**
	 * Visits {@code MOD_I} with visitor pattern.
	 * @param inst  The MOD_I to be visited.
	 */
	public void visitMOD_I(MOD_I inst){}

	/**
	 * Visits {@code AND_I} with visitor pattern.
	 * @param inst  The AND_I to be visited.
	 */
	public void visitAND_I(AND_I inst){}

	/**
	 * Visits {@code OR_I} with visitor pattern.
	 * @param inst  The OR_I to be visited.
	 */
	public void visitOR_I(OR_I inst){}
	/**
	 * Visits {@code XOR_I} with visitor pattern.
	 * @param inst  The XOR_I to be visited.
	 */
	public void visitXOR_I(XOR_I inst)
	{
		
	}
	/**
	 * Visits {@code ShiftOp} with visitor pattern.
	 * @param inst  The ShiftOp to be visited.
	 */
	public void visitShiftOp(ShiftOp inst)
	{
		
	}

	/**
	 * Visits {@code ADD_L} with visitor pattern.
	 * @param inst  The ADD_L to be visited.
	 */
	public void visitADD_L(ADD_L inst)
	{
		
	}

	/**
	 * Visits {@code SUB_I} with visitor pattern.
	 * @param inst  The SUB_I to be visited.
	 */
	public void visitSUB_L(SUB_L inst)
	{
		
	}

	/**
	 * Visits {@code MUL_L} with visitor pattern.
	 * @param inst  The MUL_L to be visited.
	 */
	public void visitMUL_L(MUL_L inst)
	{
		
	}

	/**
	 * Visits {@code DIV_L} with visitor pattern.
	 * @param inst  The DIV_L to be visited.
	 */
	public void visitDIV_L(DIV_L inst)
	{
		
	}

	/**
	 * Visits {@code MOD_L} with visitor pattern.
	 * @param inst  The MOD_L to be visited.
	 */
	public void visitMOD_L(MOD_L inst)
	{
		
	}

	/**
	 * Visits {@code AND_L} with visitor pattern.
	 * @param inst  The AND_L to be visited.
	 */
	public void visitAND_L(AND_L inst)
	{
		
	}

	/**
	 * Visits {@code OR_L} with visitor pattern.
	 * @param inst  The OR_L to be visited.
	 */
	public void visitOR_L(OR_L inst)
	{
		
	}
	/**
	 * Visits {@code XOR_L} with visitor pattern.
	 * @param inst  The XOR_L to be visited.
	 */
	public void visitXOR_L(XOR_L inst)
	{
		
	}

	/**
	 * Visits {@code ADD_F} with visitor pattern.
	 * @param inst  The ADD_F to be visited.
	 */
	public void visitADD_F(ADD_F inst)
	{
		
	}

	/**
	 * Visits {@code SUB_F} with visitor pattern.
	 * @param inst  The SUB_F to be visited.
	 */
	public void visitSUB_F(SUB_F inst)
	{
		
	}

	/**
	 * Visits {@code MUL_F} with visitor pattern.
	 * @param inst  The MUL_F to be visited.
	 */
	public void visitMUL_F(MUL_F inst)
	{
		
	}

	/**
	 * Visits {@code DIV_F} with visitor pattern.
	 * @param inst  The DIV_F to be visited.
	 */
	public void visitDIV_F(DIV_F inst)
	{
		
	}

	/**
	 * Visits {@code ADD_D} with visitor pattern.
	 * @param inst  The ADD_D to be visited.
	 */
	public void visitADD_D(ADD_D inst)
	{
		
	}

	/**
	 * Visits {@code SUB_D} with visitor pattern.
	 * @param inst  The SUB_D to be visited.
	 */
	public void visitSUB_D(SUB_D inst)
	{
		
	}

	/**
	 * Visits {@code MUL_D} with visitor pattern.
	 * @param inst  The MUL_D to be visited.
	 */
	public void visitMUL_D(MUL_D inst)
	{
		
	}

	/**
	 * Visits {@code DIV_D} with visitor pattern.
	 * @param inst  The DIV_D to be visited.
	 */
	public void visitDIV_D(DIV_D inst)
	{
		
	}

	/**
	 * Visits {@code NEG_I} with vistor pattern.
	 * @param inst  The inst to be visited.
	 */
	public void visitNEG_I(NEG_I inst)
	{
		
	}

	/**
	 * Visits {@code NEG_L} with vistor pattern.
	 * @param inst  The inst to be visited.
	 */
	public void visitNEG_L(NEG_L inst)
	{
		
	}

	/**
	 * Visits {@code NEG_F} with vistor pattern.
	 * @param inst  The inst to be visited.
	 */
	public void visitNEG_F(NEG_F inst)
	{
		
	}

	/**
	 * Visits {@code NEG_D} with vistor pattern.
	 * @param inst  The inst to be visited.
	 */
	public void visitNEG_D(NEG_D inst)
	{
		
	}

	public void visitINT_2LONG(INT_2LONG inst)
	{
		
	}
	public void visitINT_2FLOAT(INT_2FLOAT inst)
	{
		
	}
	public void visitINT_2DOUBLE(INT_2DOUBLE inst)
	{
		
	}

	public void visitLONG_2INT(LONG_2INT inst)
	{
		
	}

	public void visitLONG_2FLOAT(LONG_2FLOAT inst)
	{
		
	}

	public void visitLONG_2DOUBLE(LONG_2DOUBLE inst)
	{
		
	}

	public void visitFLOAT_2INT(FLOAT_2INT inst)
	{
		
	}

	public void visitFLOAT_2LONG(FLOAT_2LONG inst)
	{
		
	}

	public void visitFLOAT_2DOUBLE(FLOAT_2DOUBLE inst)
	{
		
	}

	public void visitDOUBLE_2INT(DOUBLE_2INT inst)
	{
		
	}

	public void visitDOUBLE_2LONG(DOUBLE_2LONG inst)
	{
		
	}

	public void visitDOUBLE_2FLOAT(DOUBLE_2FLOAT inst)
	{
		
	}

	public void visitINT_2BYTE(INT_2BYTE inst)
	{
		
	}

	public void visitINT_2CHAR(INT_2CHAR inst)
	{
		
	}

	public void visitINT_2SHORT(INT_2SHORT inst)
	{
		
	}

	public void visitBR(BR inst) {}

	public void visitICmp(ICmp inst){}

	public void visitLCmp(LCmp inst){}

	public void visitFCmp(FCmp inst){}

	public void visitDCmp(DCmp inst){}

	public void visitIfCmp_LT(IfCmp_LT inst)
	{
		
	}

	public void visitIfCmp_LE(IfCmp_LE inst)
	{
		
	}

	public void visitIfCmp_GT(IfCmp_GT inst)
	{
		
	}

	public void visitIfCmp_GE(IfCmp_GE inst)
	{
		
	}

	public void visitIfCmp_EQ(IfCmp_EQ inst)
	{
		
	}

	public void visitIfCmp_NEQ(IfCmp_NEQ inst)
	{
		
	}

	public void visitGoto(Goto inst)
	{
		
	}

	public void visitReturn(Return inst)
	{
		
	}

	public void visitInvoke(Invoke inst)
	{
		
	}

	public void visitPhi(Phi inst)
	{
		
	}

	public void visitAlloca(Alloca inst)
	{
		
	}

	public void visitStoreInst(StoreInst inst)
	{
		
	}

	public void visitLoadInst(LoadInst inst)
	{
		
	}

	/**
	 * Go through the value {@code Value}. Usually, this method is not used
	 * instead of calling to the visitor to it's subclass, like {@code Constant}.
	 * @param val   The instance of {@code Value} to be visited.
	 */
	public void visitValue(Value val)
	{
	}

	/**
	 * Visits a constant that is an instance of concrete subclass of super class
	 * {@code Value}.
	 * @param Const A constant to be visited.
	 */
	public void visitConstant(Value.Constant Const)
	{

	}

	public void visitUndef(Value.UndefValue undef)
	{

	}
}
