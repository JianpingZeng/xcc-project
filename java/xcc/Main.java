package xcc;

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
            loader.setPackageAssertionStatus(jlang.driver.Jlang.NAME, true);
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
		jlang.driver.Jlang compiler = new jlang.driver.Jlang();
        return compiler.compile(args);
    }
}
