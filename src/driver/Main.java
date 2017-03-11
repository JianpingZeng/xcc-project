package driver;

import org.apache.commons.cli.*;
import tools.Log;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * This class is used as programmatic interface for handling command line
 * options.
 * 
 * @author xlous.zeng
 *
 */
public class Main
{
	/**
	 * The getName of driver instance, just for diagnostic.
	 */
	String ownerName;

	/**
	 * The writer to use for diagnostic output.
	 */
	PrintWriter out;

	/**
	 * Result codes.
	 */
	static final int EXIT_OK = 0;

	/**
	 * Result codes.
	 */
	static final int EXIT_ERROR = 1;

	/**
	 * Result codes.
	 */
	static final int EXIT_CMDERR = 2;

	/**
	 * Result codes.
	 */
	static final int EXIT_SYSERR = 3;

	/**
	 * Result codes.
	 */
	static final int EXIT_ABNORMAL = 4;

	public static final String VERSION = "0.1";
	public static final String NAME = "xcc";

    /**
     * A list used for residing all of Option corresponding to legal command line
     * option and argument.
     *
     * Note that: all the {@code Option} here are follow the style of GNU.
     */
    private static ArrayList<Option> allOptions = new ArrayList<>();
    static
    {
        for (ProgramAction action : ProgramAction.values())
        {
            allOptions.add(new Option(action.getOptName(),
                    action.isHasArg(), action.getDesc()));
        }
    }
	/**
	 * Construct a driver instance with error output by default.
	 */
	public Main(String name)
	{
		this(name, new PrintWriter(System.err, true));
	}

	/**
	 * Construct a driver instance with given ownerName and out stream.
	 */
	public Main(String name, PrintWriter out)
	{
		this.ownerName = name;
		this.out = out;
	}

	/**
	 * A table of all options that's passed to the JavaCompiler constructor.
	 */
	private Options options = new Options();

	/**
	 * The list of files to process
	 */
	List<String> filenames = new LinkedList<>();

	CommandLine cmdline = null;

	private void printUsage(String msg)
    {
        System.err.println(msg);
        new HelpFormatter().printHelp(NAME, options);
        System.exit(EXIT_ERROR);
    }

	/**
	 * Print a string that explains usage.
	 */
	private void printUsage()
	{
        new HelpFormatter().printHelp(NAME, options);
        System.exit(EXIT_ERROR);
	}

	private void version()
	{
		out.printf("%s version %s\n", NAME, VERSION);
		System.exit(0);
	}

	private void error(String msg, String arg)
	{
		Log.printLines(out, msg + ":" + arg);
		printUsage();
	}

	private void error(String msg)
	{
		error(msg, "");
	}

	/**
	 * Process command line arguments: store all command line options in
	 * `options' table and return all source filenames.
	 * 
	 * @param args An array of all of arguments.
	 * @return
	 */
	private List<SourceFile> processArgs(String[] args)
	{
		List<SourceFile> files = new LinkedList<>();

        try
        {
            CommandLineParser cmdParser = new DefaultParser();
            cmdline = cmdParser.parse(options, args);

            // add the left argument as the file to be parsed.
            cmdline.getArgList().forEach(arg->files.add(new SourceFile(arg)));
        }
        catch (ParseException ex)
        {
            printUsage(ex.getMessage());
        }
		return files;
	}

	/**
	 * Programmatic interface for main function.
	 * 
	 * @param args The command line parameters.
	 */
	public int compile(String[] args)
	{
        if (args.length == 0)
        {
            printUsage();
            return EXIT_CMDERR;
        }

        // load all options.
        allOptions.forEach(options::addOption);

        // process command line arguments
        List<SourceFile> filenames = processArgs(args);
        if (filenames.isEmpty())
        {
            error("err.no.source.files");
            return EXIT_CMDERR;
        }

        CompilerInstance comp = CompilerInstance.construct(cmdline);
        if (comp == null)
            return EXIT_SYSERR;
        comp.compile(filenames);
        if (comp.errorCount() != 0)
            return EXIT_ERROR;

        return EXIT_OK;
	}
}
