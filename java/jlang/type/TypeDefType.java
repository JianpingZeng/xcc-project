package jlang.type;

import jlang.sema.Decl.TypeDefDecl;

/**
 * This class was served as represents typedef declaration in C language.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class TypeDefType extends Type
{
    /**
     * The actual jlang.type of this typedef declaration.
     */
    private TypeDefDecl decl;
    /**
     * Constructor with one parameter which represents the kind of jlang.type
     * for reason of comparison convenient.
     *
     * @param tag
     * @param d
     */
    public TypeDefType(int tag, TypeDefDecl d, QualType canonicalType)
    {
        super(tag, canonicalType);
        decl = d;
    }

    public String toString()
    {
        return decl.getDeclName();
    }

    /**
     * Returns the getNumOfSubLoop of the specified jlang.type in bits.
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
        return false;
    }

    /**
     * Indicates if this jlang.type can be casted into TargetData jlang.type.
     *
     * @param target
     * @return
     */
    @Override
    public boolean isCastableTo(Type target)
    {
        return false;
    }

    public TypeDefDecl getDecl() { return decl;}
}
