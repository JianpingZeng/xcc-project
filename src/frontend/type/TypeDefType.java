package frontend.type;

import frontend.sema.Decl.TypeDefDecl;

/**
 * This class was served as represents typedef declaration in C language.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class TypeDefType extends Type
{
    /**
     * The actual frontend.type of this typedef declaration.
     */
    private TypeDefDecl decl;
    /**
     * Constructor with one parameter which represents the kind of frontend.type
     * for reason of comparison convenient.
     *
     * @param tag
     * @param d
     */
    public TypeDefType(int tag, TypeDefDecl d)
    {
        super(tag);
        decl = d;
    }

    public String toString()
    {
        return decl.getDeclName();
    }

    /**
     * Returns the size of the specified frontend.type in bits.
     * </br>
     * This method doesn't work on incomplete types.
     *
     * @return
     */
    @Override public long getTypeSize()
    {
        return 0;
    }

    @Override public boolean isSameType(Type other)
    {
        return false;
    }

    /**
     * Indicates if this frontend.type can be casted into target frontend.type.
     *
     * @param target
     * @return
     */
    @Override public boolean isCastableTo(Type target)
    {
        return false;
    }

    public TypeDefDecl getDecl() { return decl;}
}
