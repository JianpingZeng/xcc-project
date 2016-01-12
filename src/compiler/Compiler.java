package compiler;

import java.util.*;
import java.io.*;

import parser.ParseException;
import parser.Parser;
import utils.*;
import ast.*;
import ast.Tree.*;

public class Compiler
{
	private static final Context.Key compilerKey = new Context.Key();

	/**
	 * The log to be used for error reporting.
	 */
	private Log log;

	/**
	 * The tree factory module.
	 */
	private TreeMaker make;

	/**
	 * The name table.
	 */
	private Name.Table names;

	private Context context;

	/**
	 * A flag that marks whether debug parer.
	 */
	private boolean debugParser = false;
	
	/**
	 * A flag that marks whether output target file.
	 */
	@SuppressWarnings("unused")
	private boolean outputResult = false;	
	
	public Compiler(Context context)
	{
		super();
		context.put(compilerKey, this);
		this.context = context;
		this.names = Name.Table.instance(context);
		this.log = Log.instance(context);
		this.make = TreeMaker.instance(context);
        Options options = Options.instance(context);
        this.debugParser = options.get("--debug-Parser") != null;
        this.outputResult = options.get("-o") != null;
	}

	public static Compiler make(Context context)
	{
		return new Compiler(context);
	}


    /**
     * The number of errors reported so far.
     */
    public int errorCount() {
        return log.nerrors;
    }
	
	public void compile(List<String> filenames)
	{
        long msec = System.currentTimeMillis();
        try
        {
        	List<Tree> trees = new LinkedList<Tree>();
        	// make a abstract syntax tree for any source file, and add it into trees.
        	for (String file : filenames)
        		trees.add(parse(file));        		
        	if (errorCount() == 0 && debugParser)
        	{       
        		Pretty p = new Pretty(new PrintWriter( System.out), false);
        		// display abstract syntax tree for any source file
        		for (Tree t : trees)
        			t.accept(p);
        		
        		// display total spent time.            	
        		printVerbose("total", Long.toString(System.currentTimeMillis() - msec));
        	}        	        	
        }
        finally
        {
        	
        }        
	}
	
	private void printVerbose(String key, String msg)
	{
		Log.printLines(log.noticeWriter, Log.getLocalizedString("verbose." + key, msg));
	}
	
	private InputStream openSourcefile(String filename)
	{
		try {
			File f = new File(filename);
			return new FileInputStream(f);
		} 
		catch (IOException e)
		{
            log.error(Position.NOPOS, "cannot.read.file", filename);
            return null;
		}
	}
	
	/**
	 * Parses the single c-flat source file by given filename.
	 * So that returns a TopLevel {@link TopLevel}
	 * 
	 * @param filename	the file name of source file.
	 * @return	The TopLevel.
	 */
	public TopLevel parse(String filename)
	{
		return parse(filename, openSourcefile(filename));
	}
	
	public TopLevel parse(String filename, InputStream input)
	{
		Name prev = log.useSource(names.fromString(filename));
		TopLevel tree = make.TopLevel(Tree.emptyList);
		long msec = System.currentTimeMillis();
		if (input != null)
		{
			if (debugParser)
				printVerbose("parsing started", filename);
			try {
				Parser parser = new Parser(input, true, context);
				tree = parser.compilationUnit();
				if (debugParser)
					printVerbose("parsing.done", Long.toString(System.currentTimeMillis() - msec));
				
			}
			catch (ParseException e) {
				log.error(Position.NOPOS, "error.parsing.file", filename);
			}
		}
		log.useSource(prev);
		tree.sourceFile = names.fromString(filename);
		return tree;
	}
	
    /**
     * Close the compiler, flushing the logs
     */
   public void close() {
       log.flush();
       names.dispose();
   }	
}
