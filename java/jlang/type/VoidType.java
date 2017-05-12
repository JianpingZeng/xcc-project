package jlang.type;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class VoidType extends Type
{
    /**
     * Constructor with one parameter which represents the kind of jlang.type
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

    public String toString()
    {
        return "void";
    }
}
