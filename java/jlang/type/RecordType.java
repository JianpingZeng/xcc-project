package jlang.type;

import jlang.sema.Decl;
import jlang.sema.Decl.RecordDecl;

/**
 * This class is intended to represent record jlang.type, like struct in C or class
 * in C++.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class RecordType extends TagType
{
    /**
     * Constructor with one parameter which represents the kind of jlang.type
     * for reason of comparison convenient.
     *
     * @param decl
     */
    public RecordType(RecordDecl decl)
    {
        super(Struct, decl, new QualType());
    }

    @Override
    public String toString()
    {
        return "struct " + decl.getDeclName();
    }

    public Decl.RecordDecl getDecl()
    {
        return (RecordDecl)decl;
    }

    // FIXME: This predicate is a helper to QualType/Type. It needs to
    // recursively check all fields for const-ness. If any field is declared
    // const, it needs to return false.
    public boolean hasConstFields()
    {
        return false;
    }

    /**
     * RecordType needs to check when it is created that all fields are in
     // the same address space, and return that.
     */
    public int getAddressSpace()
    {
        return 0;
    }
}
