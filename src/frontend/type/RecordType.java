package frontend.type;

import frontend.sema.Decl;
import frontend.sema.Decl.RecordDecl;

/**
 * This class is intended to represent record frontend.type, like struct in C or class
 * in C++.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class RecordType extends TagType
{
    /**
     * Constructor with one parameter which represents the kind of frontend.type
     * for reason of comparison convenient.
     *
     * @param decl
     */
    public RecordType(RecordDecl decl)
    {
        super(Struct, decl);
    }

    protected void computeOffsets()
    {
        long offset = 0;
        long maxAlign = 1;
        /*
        for (Slot s : members)
        {
            offset = AsmUtils.align(offset, s.allocaSize());
            s.setOffset(offset);
            offset += s.allocaSize();
            maxAlign = Math.max(maxAlign, s.alignment());
        }
        cachedSize = AsmUtils.align(offset, maxAlign);
        cachedAlignment = maxAlign;
        */
    }

    /**
     * Returns the size of the specified frontend.type in bits.
     * </br>
     * This method doesn't work on incomplete types.
     *
     * @return
     */
    @Override
    public long getTypeSize()
    {
        return 0;
    }

    @Override
    public boolean isSameType(Type other)
    {
        /*
        if (!other.isRecordType())
            return false;
        RecordType rt = other.getRecordType();
        return compareMembersType(rt.members, IS_SAME_TYPE);
        */
        return true;
    }

    @Override
    public boolean isCastableTo(Type target)
    {
        /*
        if (!TargetData.isRecordType())
            return false;
        RecordType rt = TargetData.getRecordType();
        return compareMembersType(rt.members, IS_CASTABLE);
        */
        return true;
    }

    @Override
    public String toString()
    {
        return "struct " + decl.getDeclName();
    }

    public Decl.RecordDecl getDecl() { return (RecordDecl)decl; }
}
