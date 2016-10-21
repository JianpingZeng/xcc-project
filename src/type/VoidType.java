package type;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class VoidType extends Type
{
    /**
     * Constructor with one parameter which represents the kind of type
     * for reason of comparison convenient.
     */
    private VoidType()
    {
        super(Void);
    }

    private static VoidType instance = null;
    public static VoidType  New()
    {
        if (instance == null)
            instance = new VoidType();
        return instance;
    }

    @Override
    public long getTypeSize()
    {
        return 1;
    }

    @Override
    public boolean isSameType(Type other)
    {
        return tag == other.tag;
    }

    @Override
    public boolean isCastableTo(Type target)
    {
        return target.isVoidType();
    }

    public String toString()
    {
        return "void";
    }
}
