package hir;

import hir.Quad.*;

/**
 * A Quad Visitor for Quad using Visitor pattern.
 * @author Jianping Zeng
 * 
 */
public interface QuadVisitor {
	
	void visitIAdd(IAdd iAdd);

	void visitISub(ISub iSub);

	void visitIMul(IMul iMul);

	void visitIDiv(IDiv iDiv);

	void visitIMod(IMod iMod);

	void visitLShift(LShift lShift);

	void visitRShift(RShift rShift);

	void visitBitAnd(BitAnd bitAnd);

	void visitBitOR(BitOR bitOR);

	void visitBitXOR(BitXOR bitXOR);

	void visitAssign(Assign assign);

	void visitIMinus(IMinus iMinus);

	void visitIBitNot(IBitNot iBitNot);

	void visitIfLT(IfLT ifLT);

	void visitIfCmpLT(IfCmpLT ifCmpLT);

	void visitIfCmpLE(IfCmpLE ifCmpLE);

	void visitIfLE(IfLE ifLE);

	void visitIfCmpEQ(IfCmpEQ ifCmpEQ);

	void visitIfEQ(IfEQ ifEQ);

	void visitIfCmpGE(IfCmpGE ifCmpGE);

	void visitIfGE(IfGE ifGE);

	void visitIfCmpNE(IfCmpNE ifCmpNE);

	void visitIfNE(IfNE ifNE);

	void visitGoto(Goto goto1);

	void visitRet(Ret ret);

	void visitLRet(LRet lRet);

	void visitIRet(IRet iRet);

	void visitLabel(Label label);

	void visitLRShift(LRShift lrShift);

	void visitIfCmpGT(IfCmpGT ifCmpGT);

	void visitIfGT(IfGT ifGT);

}
