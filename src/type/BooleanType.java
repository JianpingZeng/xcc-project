package type;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class BooleanType extends PrimitiveType
{
    private byte value;

    public static final BooleanType True = new BooleanType((byte)1, "true");
    public static final BooleanType False = new BooleanType((byte)0, "false");
    /**
     * Constructor with one parameter which represents the kind of type
     * for reason of comparison convenient.
     *
     * @param value
     * @param name
     */
    private BooleanType(byte value, String name)
    {
        super(Bool, name);
        this.value = value;
    }

    public boolean isSignedType()
    {
        return false;
    }

    @Override
    public long getTypeSize()
    {
        return 1;
    }

    @Override
    public boolean isSameType(Type other)
    {
        if (!other.isBooleanType())
            return false;
        return equals(other);
    }

    @Override
    public boolean isCastableTo(Type target)
    {
        return false;
    }
}
