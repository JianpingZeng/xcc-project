package backend.pass;

import backend.support.Printable;

import java.io.PrintStream;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class PassInfo implements Printable
{
    /**
     * The asmName of pass.
     */
    private String passName;
    /**
     * Command line arguments for running this pass.
     */
    private String passArgument;
    /**
     * Class object for the Pass.
     */
    private Class<? extends Pass> klass;
    private boolean isAnalysisGroup;
    private boolean isAnalysis;
    private boolean isCFGOnlyPass;

    /**
     * Create a PassInfo instance that encanpsulates some information about how
     * to instance a pass, what name of Pass, and what is command line argument
     * corresponding to Pass.
     * @param name  The name of Pass.
     * @param arg   The command line argument to be printed out into console.
     * @param typeInfo
     * @param cfgOnly
     * @param isAnalysis
     */
    public PassInfo(String name,
            String arg,
            Class<? extends Pass> typeInfo,
            boolean cfgOnly,
            boolean isAnalysis)
    {
        passName = name;
        passArgument = arg;
        klass = typeInfo;
        this.isCFGOnlyPass = cfgOnly;
        this.isAnalysis = isAnalysis;
        isAnalysisGroup = true;
    }

    public String getPassName()
    {
        return passName;
    }

    public void setPassName(String passName)
    {
        this.passName = passName;
    }

    public String getPassArgument()
    {
        return passArgument;
    }

    public Class<? extends Pass> getKlass()
    {
        return klass;
    }

    public Pass createPass()
    {
        try
        {
            return klass.newInstance();
        }
        catch (IllegalAccessException | InstantiationException e)
        {
            e.printStackTrace();
        }
        assert false:"Can not create instance without default ctor!";
        return null;
    }

    public boolean isAnalysisGroup()
    {
        return isAnalysisGroup;
    }

    public boolean isAnalysis()
    {
        return isAnalysis;
    }

    public boolean isCFGOnlyPass()
    {
        return isCFGOnlyPass;
    }

    @Override
    public void print(PrintStream os)
    {
        os.printf("Pass: %s, %s\n", getPassName(), getPassArgument());
    }
}
