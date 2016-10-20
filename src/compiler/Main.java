package compiler;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import utils.Context;
import utils.Log;

/**
 * This class is used as programmatic interface for handling command line
 * options.
 * 
 * @author zeng
 *
 */
public class Main
{
	/**
	 * The name of compiler instance, just for diagnostic.
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
	public static final String NAME = "scc";

	/**
	 * The array of recognization Option.
	 */
	private Option[] recognizedOptions = 
		{
	        new Option("-o", "<file>", "Place the output into <file>"),
	        new Option("-E", "",
	                "Preprocess only, no compile, assembly or link"),
	        new Option("-c", "", "Compile and assemble but no link"),
	        new Option("-S", "", "Compile only, no assembly or link"),
	        new Option("-O", "Specify the level of opt")
	        {
		        @Override
		        boolean matches(String arg)
		        {
			        return arg.startsWith("-O");
		        }

                @Override
		        boolean process(String option)
		        {
			        String type = option.substring(2);
			        // obtains level of opt.
			        if (!type.matches("^[0123s]|)$"))
			        {
				        error("unknown opt switch: " + option);
				        return true;
			        }
			        options.put(name, option);
			        return false;
		        }
            },
	        new Option("--debug-Parser", "Display the process of parser"),
	        new Option("--dump-ast", "Display abstract syntax tree"),
	        new Option("--dump-hir", "display higher level ir"),
	        new Option("--dump-lir", "display lower level ir"),
	        new Option("-g", "Generate debug infortion for output file"),
	        new Option("-h", "Display help information")
	        {
		        @Override
		        boolean matches(String arg)
		        {
			        return arg.equals("-h") || arg.equals("--help");
		        }

		        @Override
		        boolean process(String option)
		        {
			        printUsage();
			        return false;
		        }
	        },
	        new Option("-v", "Display the version information")
	        {
		        @Override
		        boolean process(String option)
		        {
			        version();
			        return false;
		        }
	        }
		};

	/**
	 * Construct a compiler instance with error output by default.
	 */
	public Main(String name)
	{
		this(name, new PrintWriter(System.err, true));
	}

	/**
	 * Construct a compiler instance with given ownerName and out stream.
	 */
	public Main(String name, PrintWriter out)
	{
		super();
		this.ownerName = name;
		this.out = out;
	}

	/**
	 * A table of all options that's passed to the JavaCompiler constructor.
	 */
	private Options options = null;

	/**
	 * The list of files to process
	 */
	List<String> filenames = null;

	/**
	 * Print a string that explains usage.
	 */
	private void printUsage()
	{
		out.println("Usage: scc <options> files...");
		for (int i = 0; i < recognizedOptions.length; i++)
		{
			recognizedOptions[i].help();
		}
		out.println();
		System.exit(0);
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
		List<SourceFile> files = new LinkedList<SourceFile>();
		int ac = 0;
		while (ac < args.length)
		{
			String arg = args[ac++];
			// compilation options
			if (arg.startsWith("-"))
			{
				int idx = 0;
				for (; idx < recognizedOptions.length; idx++)
					if (recognizedOptions[idx].matches(arg)) break;
				if (idx == recognizedOptions.length)
				{
					error("err.invalid.commandline.arg", arg);
					return null;
				}
				Option option = recognizedOptions[idx];
				if (option.hasArg())
				{
					if (ac == args.length)
					{
						error("err.argument.lack", arg);
						return null;
					}
					String operand = args[ac];
					ac++;
					if (option.process(arg, operand)) return null;
				}
				else
				{
					if (option.process(arg)) return null;
				}
			}		
			else
			{
				// source file
				files.add(new SourceFile(arg));
			}
		}
		return files;
	}

	/**
	 * Programmatic interface for main function.
	 * 
	 * @param args The command line parameters.
	 */
	@SuppressWarnings("finally")
	public int compile(String[] args)
	{
		Context context = new Context();
		options = Options.instance(context);
		this.filenames = new LinkedList<>();
		Compiler comp = null;
		try
		{
			if (args.length == 0)
			{
				printUsage();
				return EXIT_CMDERR;
			}
			List<SourceFile> filenames;

			// process command line arguments
			filenames = processArgs(args);
			if (filenames == null)
			{
				return EXIT_CMDERR;
			}
			else if (filenames.isEmpty())
			{
				if (options.get("-h") != null) return EXIT_OK;

				error("err.no.source.files");
				return EXIT_CMDERR;
			}

			context.put(utils.Log.outKey, out);
			comp = Compiler.make(context);
			if (comp == null) return EXIT_SYSERR;
			comp.compile(filenames);
			if (comp.errorCount() != 0) return EXIT_ERROR;
		}
		finally
		{
			if (comp != null)
			{
				comp.close();
				filenames = null;
				options = null;
			}
			return EXIT_OK;
		}
	}

	/**
	 * This class represents a single option derived from command line, when
	 * command line mode is used.
	 * 
	 * @author zeng
	 */
	private class Option
	{
		/**
		 * Option string.
		 */
		String name;

		/**
		 * Documentation key for arguments.
		 */
		String argsNameKey;

		/**
		 * Documentation key for description.
		 */
		String descrKey;

		Option(String name, String argsNameKey, String descrKey)
		{
			super();
			this.name = name;
			this.argsNameKey = argsNameKey;
			this.descrKey = descrKey;
		}

		Option(String name, String descrKey)
		{
			this(name, null, descrKey);
		}

		/**
		 * Does this option take an operand?
		 */
		boolean hasArg()
		{
			return argsNameKey != null;
		}

		/**
		 * Does argument string match option pattern?
		 * 
		 * @param arg The command line argument string.
		 */
		boolean matches(String arg)
		{
			return name.equals(arg);
		}

		/**
		 * Print a line of documentation describing this standard option.
		 */
		void help()
		{
			String s = "  " + helpSynopsis();
			out.print(s);
			for (int j = s.length(); j < 28; j++)
				out.print(" ");
			utils.Log.printLines(out, descrKey);
		}

		String helpSynopsis()
		{
			String s = name + " ";
			if (argsNameKey != null) s += argsNameKey;
			return s;
		}

		/**
		 * Process the option (with arg). ReturnInst true if error detected.
		 */
		boolean process(String option, String arg)
		{
			options.put(option, arg);
			return false;
		}

		/**
		 * Process the option (without arg). ReturnInst true if error detected.
		 */
		boolean process(String option)
		{
			return process(option, option);
		}
	}
}
