package xcc;

import jlang.driver.JlangCC;

public class Jlang
{
    public Jlang()
    {
        super();
    }
    static
    {
        ClassLoader loader = Jlang.class.getClassLoader();
        if (loader != null)
            loader.setPackageAssertionStatus(JlangCC.NAME, true);
    }

    /**
      * Command line interface.
      * @param args   The command line parameters.
      */
    public static void main(String[] args)
    {
        System.exit(compile(args));
    }
    
    /**
      * Programmatic interface.
      * @param args   The command line parameters.
      */
    public static int compile(String[] args)
    {
		JlangCC compiler = new JlangCC();
        try
        {
            return compiler.compile(args);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return -1;
    }
}
