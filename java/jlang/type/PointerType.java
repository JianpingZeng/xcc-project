package jlang.type;

import jlang.support.PrintingPolicy;

/**
 * @author xlous.zeng
 * @version 0.1
 */
public final class PointerType extends Type
{
    /**
     * The basic jlang.type of this pointer jlang.type.
     */
    private QualType pointeeType;

    public PointerType(QualType pointee, QualType canonical)
    {
        super(Pointer, canonical);
        pointeeType = pointee;
    }

    public QualType getPointeeType()
    {
        return pointeeType;
    }

    @Override
    public String getAsStringInternal(String inner, PrintingPolicy policy)
    {
        inner = "*" + inner;

        if (pointeeType.getType() instanceof ArrayType)
            inner = '(' + inner + ')';
        return pointeeType.getAsStringInternal(inner, policy);
    }
}
