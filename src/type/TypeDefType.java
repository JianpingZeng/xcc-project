package type;

import sema.Decl;
import sema.Decl.TypedefNameDecl;
import utils.Name;
import utils.Position;

/**
 * This class was served as represents typedef declaration in C language.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class TypeDefType extends Type
{
    /**
     * The actual type of this typedef declaration.
     */
    private TypedefNameDecl decl;
    /**
     * Constructor with one parameter which represents the kind of type
     * for reason of comparison convenient.
     *
     * @param tag
     * @param d
     */
    public TypeDefType(int tag, TypedefNameDecl d)
    {
        super(tag);
        decl = d;
    }

    public String toString()
    {
        return decl.getDeclName();
    }

    /**
     * Returns the size of the specified type in bits.
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
     * Indicates if this type can be casted into target type.
     *
     * @param target
     * @return
     */
    @Override public boolean isCastableTo(Type target)
    {
        return false;
    }

    public TypedefNameDecl getDecl() { return decl;}
}
