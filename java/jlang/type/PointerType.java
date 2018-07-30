package jlang.type;

import jlang.support.PrintingPolicy;
import tools.FoldingSetNode;
import tools.FoldingSetNodeID;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public final class PointerType extends Type implements FoldingSetNode
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

    @Override
    public void profile(FoldingSetNodeID id)
    {
        id.addInteger(pointeeType.hashCode());
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
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;
        PointerType pt = (PointerType)obj;
        return pt.hashCode() == hashCode();
    }
}
