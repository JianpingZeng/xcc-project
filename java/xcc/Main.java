package xcc;

import java.io.PrintWriter;

public class Main
{
    public Main()
    {
        super();
    }
    static
    {
        ClassLoader loader = Main.class.getClassLoader();
        if (loader != null)
            loader.setPackageAssertionStatus(jlang.driver.Main.NAME, true);
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
		jlang.driver.Main compiler = new jlang.driver.Main(jlang.driver.Main.NAME);
        return compiler.compile(args);
    }

    /**
      * Programmatic interface.
      * @param args   The command line parameters.
      * @param out    Where the jlang.driver's output is directed.
      */
    public static int compile(String[] args, PrintWriter out) {
    	jlang.driver.Main compiler =
                new jlang.driver.Main(jlang.driver.Main.NAME, out);
        return compiler.compile(args);
    }
}
