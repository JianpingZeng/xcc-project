package jlang.type;

import jlang.basic.PrintingPolicy;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class ComplexType extends Type
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
}
