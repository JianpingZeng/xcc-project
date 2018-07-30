package jlang.type;

import jlang.support.PrintingPolicy;
import tools.FoldingSetNode;
import tools.FoldingSetNodeID;

public class VectorType extends Type implements FoldingSetNode
{
    private QualType eleTypes;
    private int numElts;

    public VectorType(QualType eltType, int numElts, QualType canTy)
    {
        super(TypeClass.Vector, canTy);
        this.eleTypes = eltType;
        this.numElts = numElts;
    }

    public int getNumOfElements()
    {
        return numElts;
    }

    public QualType getElementTypes()
    {
        return eleTypes;
    }

    @Override
    public String getAsStringInternal(String inner, PrintingPolicy policy)
    {
        inner += " __attribute__((__vector_size__(" +
                numElts + " * sizeof(" + eleTypes.getAsString() + "))))";
        return eleTypes.getAsStringInternal(inner, policy);
    }

    @Override
    public void profile(FoldingSetNodeID id)
    {
        profile(id, getElementTypes(), getNumOfElements(), getTypeClass());
    }

    static void profile(FoldingSetNodeID id, QualType eleType, int numElts,
                        int typeClass)
    {
        id.addInteger(eleType.hashCode());
        id.addInteger(numElts);
        id.addInteger(typeClass);
    }
}
