package driver;

import config.Config;
import cfe.diag.Diagnostic;
import cfe.diag.DiagnosticClient;
import cfe.diag.DiagnosticInfo;
import tools.Util;

import java.io.PrintStream;

import static cfe.system.Process.getHostTriple;

public class XCC {
  private XCC() {
    super();
  }

  /**
   * Command line interface.
   *
   * @param args The command line parameters.
   */
  public static void main(String[] args) {
    System.exit(new XCC().compile(args));
  }

  private String executableDirname;
  private String executableBasename;

  /**
   * A diagnostic implementation for XCCTool command.
   */
  private static class DriverDiagnosticClient implements DiagnosticClient {
    private String progName;
    private PrintStream os;

    public DriverDiagnosticClient(String progName, PrintStream os) {
      this.progName = progName;
      this.os = os;
    }

    @Override
    public void handleDiagnostic(Diagnostic.Level diagLevel,
                                 DiagnosticInfo info) {
      os.printf("%s:", progName);
      switch (diagLevel) {
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

  private void getExecutableFile(String path) {
    Util.assertion(!(path == null || path.isEmpty()));
    int lastBlash = path.lastIndexOf('/');
    if (lastBlash < 0) {
      executableBasename = "";
      return;
    }
    executableDirname = path.substring(0, lastBlash);
    executableBasename = path.substring(lastBlash + 1);
  }

  /**
   * Programmatic interface.
   *
   * @param args The command line parameters.
   */
  public int compile(String[] args) {
    Util.assertion(!(args == null || args.length <= 0));

    String executableFilePath;
    if (args[0].equals("launcher")) {
      executableFilePath = args[1];
      String[] temp = new String[args.length - 2];
      System.arraycopy(args, 2, temp, 0, temp.length);
      args = temp;
    } else
      executableFilePath = Config.BinaryPath + "/xcc";

    // Compute the name of main program according to native launcher or
    // hardcoded.
    getExecutableFile(executableFilePath);
    DriverDiagnosticClient diagClient = new DriverDiagnosticClient(
        executableBasename, System.err);

    Diagnostic diags = new Diagnostic(diagClient);
    boolean cccPrintPhases = false;
    int startOfArgs = 0;
    switch (args[0]) {
      case "-ccc-print-phases":
        cccPrintPhases = true;
        startOfArgs = 1;
        break;
      default:
        break;
    }
    String[] temp = new String[args.length - startOfArgs];
    System.arraycopy(args, startOfArgs, temp, 0, temp.length);
    args = temp;
    // create a driver to run a sort of pipelines of compilation.
    Driver driver = new Driver(executableBasename, executableDirname,
        getHostTriple(), "a.out", diags, cccPrintPhases);

    Compilation compilation = driver.buildCompilation(args);
    return driver.executeCompilation(compilation);
  }
}