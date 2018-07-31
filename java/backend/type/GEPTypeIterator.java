package backend.type;

import backend.value.Instruction;
import backend.value.Value;
import tools.Util;

public class GEPTypeIterator
{
    private Type curTy;
    private Instruction.GetElementPtrInst gep;
    private int index;
    public GEPTypeIterator(Instruction.GetElementPtrInst gep)
    {
        curTy = gep.getPointerOperandType();
        this.gep = gep;
        index = 1;
    }

    public boolean hasNext()
    {
        return curTy != null;
    }

    public void next()
    {
        if (curTy instanceof CompositeType)
        {
            CompositeType ct = (CompositeType) curTy;
            curTy = ct.getTypeAtIndex(getOperand());
        }
        else
            curTy = null;
        ++index;
    }

    public Type getCurType()
    {
        return curTy;
    }

    public Type getIndexedType()
    {
        Util.assertion(curTy instanceof CompositeType);
        CompositeType ct = (CompositeType) curTy;
        return ct.getTypeAtIndex(getOperand());
    }

    public Value getOperand()
    {
        return gep.operand(index);
    }
}
