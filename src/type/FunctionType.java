package type;

import java.util.List;

/**
 * C99 6.7.5.3 - Function Declarators.
 *
 * @author xlous.zeng
 * @version 0.1
 */
public class FunctionType extends Type
{
    /**
     * ReturnStmt type.
     */
    private QualType returnType;
    /**
     * Parameter type list.
     */
    private List<Type> paramTypes;
    /**
     * Indicates whether it is variable parameters list.
     */
    private boolean isVarArgs;

    /**
     * Constructor with one parameter which represents the kind of type
     * for reason of comparison convenient.
     * @param returnType indicates what type would be returned.
     * @param paramTypes indicates the parameters type list would be passed into
     *                   function body.
     * @param isVarArgs indicates if it is variable parameter list.
     */
    public FunctionType(QualType returnType, List<Type> paramTypes, boolean isVarArgs)
    {
        super(Function);
        this.returnType = returnType;
        this.paramTypes = paramTypes;
        this.isVarArgs = isVarArgs;
    }
    @Override
    public boolean isCallable()
    {
        return true;
    }

    public long getTypeSize()
    {
        throw new Error("FunctionType#getArraySize called");
    }

    /**
     * A private function for comparing two type list.
     * @param list1
     * @param list2
     * @return
     */
    private boolean isSameType(List<Type> list1, List<Type> list2)
    {
        if (list1.size() != list2.size())
            return false;
        for (int i = 0; i< list1.size(); ++i)
        {
            if (!list1.get(i).isSameType(list2.get(i)))
                return false;
        }
        return true;
    }

    @Override
    public boolean isSameType(Type other)
    {
        if (!other.isFunctionType())
            return false;
        FunctionType ft = other.getFunctionType();
        return returnType.isSameType(ft.returnType)
                && isSameType(paramTypes, ft.paramTypes);
    }

    @Override
    public boolean isCastableTo(Type target)
    {
        return target.isFunctionType();
    }

    public QualType getReturnType()
    {
        return returnType;
    }

    public List<Type> getParamTypes()
    {
        return paramTypes;
    }

    public boolean isVarArgs()
    {
        return isVarArgs;
    }

    public boolean acceptsArgc(int numArgs)
    {
        if (isVarArgs)
        {
            return numArgs>= paramTypes.size();
        }
        else
        {
            assert !isVarArgs : "The isVarArgs must be false.";
            return numArgs == paramTypes.size();
        }
    }

    @Override
    public long alignment()
    {
        throw new Error("FunctionType#alignment called");
    }

    public String toString()
    {
        String sep = "";
        StringBuffer buf = new StringBuffer();
        buf.append(returnType.toString());
        buf.append("(");
        for (Type t : paramTypes)
        {
            buf.append(sep);
            buf.append(t.toString());
            sep = ", ";
        }
        buf.append(")");
        return buf.toString();
    }

    public int getNumParams() { return paramTypes.size(); }

    public Type getParamType(int i)
    {
        assert i>=0 && i<paramTypes.size();
        return paramTypes.get(i);
    }
}
