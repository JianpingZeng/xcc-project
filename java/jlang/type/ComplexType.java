package jlang.type;

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

    public boolean isSignedType(){return false;}

    public String toString()
    {
        StringBuilder buffer = new StringBuilder("");
        return buffer.toString();
    }

    public QualType getElementType()
    {
        return elementType;
    }
}
