package xcc;

import jlang.driver.JlangCC;

public class Jlang
{
    private Jlang()
    {
        super();
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
