package backend.pass;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class PassInfo
{
    /**
     * The name of pass.
     */
    private String passName;
    /**
     * Command line arguments for running this pass.
     */
    private Object[] passArgument;
    /**
     * Class object for the Pass.
     */
    private Class<Pass> klass;

    public PassInfo(String name, Object[] arg, Class<Pass> typeInfo)
    {
        passName = name;
        passArgument = arg;
        klass = typeInfo;
    }

    public String getPassName()
    {
        return passName;
    }

    public void setPassName(String passName)
    {
        this.passName = passName;
    }

    public Object[] getPassArgument()
    {
        return passArgument;
    }

    public Class<Pass> getKlass()
    {
        return klass;
    }

    public Pass createPass()
    {
        try
        {
            return klass.newInstance();
        }
        catch (InstantiationException e)
        {
            e.printStackTrace();
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
        assert false:"Can not create instance without default ctor!";
        return null;
    }
}
