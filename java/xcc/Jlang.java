package xcc;

import config.Config;
import jlang.diag.Diagnostic;
import jlang.diag.DiagnosticClient;
import jlang.diag.DiagnosticInfo;

import java.io.PrintStream;

import static jlang.system.Process.getHostTriple;

public class Jlang
{
    private Jlang()
    {
        super();
    }

    /**
     * Command line interface.
     *
     * @param args The command line parameters.
     */
    public static void main(String[] args)
    {
        System.exit(new Jlang().compile(args));
    }

    private String executableDirname;
    private String executableBasename;

    /**
     * A diagnostic implementation for Jlang command.
     */
    private static class DriverDiagnosticClient implements DiagnosticClient
    {
        private String progName;
        private PrintStream os;

        public DriverDiagnosticClient(String progName, PrintStream os)
        {
            this.progName = progName;
            this.os = os;
        }

        @Override
        public void handleDiagnostic(Diagnostic.Level diagLevel,
                DiagnosticInfo info)
        {
            os.printf("%s:", progName);
            switch (diagLevel)
            {
                case Note:
                    os.print("note:");
                    break;
                case Warning:
                    os.print("warning:");
                    break;
                case Error:
                    os.print("error:");
                    break;
                case Fatal:
                    os.print("fatal:");
                    break;
            }
            StringBuilder msg = new StringBuilder();
            info.formatDiagnostic(msg);
            os.println(msg);
        }
    }

    private void getExecutableFile(String path)
    {
        assert !(path == null || path.isEmpty());
        int lastBlash = path.lastIndexOf('/');
        if (lastBlash < 0)
        {
            executableBasename = "";
            return;
        }
        executableDirname = path.substring(0, lastBlash);
        executableBasename = path.substring(lastBlash+1);
    }

    /**
      * Programmatic interface.
      * @param args   The command line parameters.
      */
    public int compile(String[] args)
    {
        assert !(args == null || args.length <= 0);
        String executableFilePath;
        if (args[0].equals("-launcher"))
        {
            executableFilePath = args[0];
            String[] temp = new String[args.length-1];
            System.arraycopy(args, 1, temp, 0, temp.length);
            args = temp;
        }
        else
            executableFilePath = Config.BinaryPath + "/jlang";

        // Compute the name of main program according to native launcher or
        // hardcoded.
        getExecutableFile(executableFilePath);
        DriverDiagnosticClient diagClient = new DriverDiagnosticClient(
                executableBasename, System.err);

        Diagnostic diags = new Diagnostic(diagClient);

        // Create a driver to run a sort of pipelines of compilation.
        Driver driver = new Driver(executableBasename, executableDirname,
                getHostTriple(), "a.out", diags);

        Compilation compilation = driver.buildComilation(args);
        return driver.executeCompilation(compilation);
    }
}
