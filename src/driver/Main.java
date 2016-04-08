package driver;

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

	/**
	 * The array of recoginization Option. 
	 */
	private Option[] recognizedOptions = {
			new Option("--debug-Parser", "opt.debug.Parser"),
			
			new Option("-o", "opt.output.filename") {
                boolean matches(String s) {
                    return s.startsWith("-o");
                }
                boolean process(String option)
                {
                	String suboption = option.substring(2);
                	options.put("-o", suboption);
                	return false;
                }
			},
			new HiddenOption("sourcefile") {

                String s;

                boolean matches(String s) {
                    this.s = s;
                    return s.endsWith(".java");
                }

                boolean process(String option) {
                    filenames.add(s);
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
	void help()
	{

	}

	private void error(String msg, String arg)
	{
		Log.printLines(out, msg + ":" + arg); 
		help();		
	}

	private void error(String msg)
	{
		error(msg, "");
	}
	/**
      * Process command line arguments: store all command line options
      *  in `options' table and return all source filenames.
      *  
	 * @param args	An array of all of arguments.
	 * @return
	 */
	private List<String> processArgs(String[] args)
	{
		List<String> files = new LinkedList<String>();
		int ac = 0;
		while (ac < args.length)
		{
			String arg = args[ac++];
			int idx = 0;
			for (; idx < recognizedOptions.length; idx++)
				if (recognizedOptions[idx].matches(arg))
					break;
            if (idx == recognizedOptions.length) {
                error("err.invalid.commanline.arg", arg);
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
            	if (option.process(arg, operand))
            		return null;            	
            }
            else 
            {
                if (option.process(arg))
                    return null;
            }
            
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
		Context context = new Context();
		options = Options.instance(context);
		this.filenames = new LinkedList<>();
		CompileInterface comp = null;
		try
		{
			if (args.length == 0)
			{
				help();
				return EXIT_CMDERR;
			}
			List<String> filenames;
			
			// process command line arguments
			filenames = processArgs(args);
			if (filenames == null)
			{
				return EXIT_CMDERR;
			}
			else if (filenames.isEmpty())
			{
				if (options.get("-help") != null)
					return EXIT_OK;
				error("err.no.source.files");
				return EXIT_CMDERR;
			}
		
			context.put(utils.Log.outKey, out);
			comp = CompileInterface.make(context);
			if (comp == null)
				return EXIT_SYSERR;
			comp.compile(filenames);
            if (comp.errorCount() != 0)
                return EXIT_ERROR;
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
	 * This class represents a single option derived from command line,
	 * when command line mode is used.
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

	    Option(String name, String argsNameKey, String descrKey) {
	        super();
	        this.name = name;
	        this.argsNameKey = argsNameKey;
	        this.descrKey = descrKey;
	    }

	    Option(String name, String descrKey) {
	        this(name, null, descrKey);
	    }

	    /**
	      * Does this option take an operand?
	      */
	    boolean hasArg() {
	        return argsNameKey != null;
	    }

	    /**
	      * Does argument string match option pattern?
	      * 
	      *  @param arg        The command line argument string.
	      */
	    boolean matches(String arg) {
	        return name.equals(arg);
	    }
	    
	      /**
         * Print a line of documentation describing this standard option.
         */
       void help() {
           String s = "  " + helpSynopsis();
           out.print(s);
           for (int j = s.length(); j < 28; j++)
               out.print(" ");
           utils.Log.printLines(out, descrKey);
       }

       String helpSynopsis() {
           String s = name + " ";
           if (argsNameKey != null)
               s += argsNameKey;
           return s;
       }

       /**
         * Process the option (with arg). Return true if error detected.
         */
       boolean process(String option, String arg) {
           options.put(option, arg);
           return false;
       }

       /**
         * Process the option (without arg). Return true if error detected.
         */
       boolean process(String option) {
           return process(option, option);
       }
	}
    /**
     * A hidden (implementor) option
     */
   private class HiddenOption extends Option {

       HiddenOption(String name) {
           super(name, null, null);
       }

       void help() {
       }

       void xhelp() {
       }
   }
	
}
