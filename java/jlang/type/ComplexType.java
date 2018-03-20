package jlang.type;

import jlang.support.PrintingPolicy;
import tools.FoldingSetNode;
import tools.FoldingSetNodeID;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class ComplexType extends Type implements FoldingSetNode
{
    private QualType elementType;

    /**
     * Constructor with one parameter which represents the kind of jlang.type
     * for reason of comparison convenient.
     *
     * @param
     */
    public ComplexType(QualType eltType, QualType canonicalPtr)
    {
        super(Complex, canonicalPtr);
        elementType = eltType;
    }

    public QualType getElementType()
    {
        return elementType;
    }

    @Override
    public String getAsStringInternal(String inner, PrintingPolicy policy)
    {
        return "_Complex" + elementType.getAsStringInternal(inner, policy);
    }

    @Override
    public void profile(FoldingSetNodeID id)
    {
        id.addInteger(elementType.hashCode());
    }

    @Override
    public int hashCode()
    {
        FoldingSetNodeID id = new FoldingSetNodeID();
        profile(id);
        return id.computeHash();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (getClass() != obj.getClass())
            return false;
        ComplexType ty = (ComplexType)obj;
        return ty.hashCode() == hashCode();
    }
}
