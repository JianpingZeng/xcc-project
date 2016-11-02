package frontend.type;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class ComplexType extends Type
{
    private double real;
    private double img;
    private String name;
    /**
     * Constructor with one parameter which represents the kind of frontend.type
     * for reason of comparison convenient.
     *
     * @param
     */
    public ComplexType(double real, double img, String name)
    {
        super(Complex);
        this.real = real;
        this.img = img;
        this.name = name;
    }

    public boolean isSignedType()
    {
        return false;
    }

    @Override
    public long getTypeSize()
    {
        return 0;
    }

    @Override
    public boolean isSameType(Type other)
    {
        if (!other.isComplexType())
            return false;
        return equals(other.getComplexTye());
    }

    @Override
    public boolean isCastableTo(Type target)
    {
        return false;
    }

    public String toString()
    {
        StringBuilder buffer = new StringBuilder(name);
        buffer.append("@(");
        buffer.append(real);
        buffer.append(",");
        buffer.append(img);
        buffer.append("i)");
        return buffer.toString();
    }
}
