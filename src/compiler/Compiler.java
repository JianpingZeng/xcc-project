package compiler;

import java.util.*;
import java.io.*;

import lir.backend.x86.X86;
import lir.backend.x86.X86RegisterConfig;
import comp.Enter;
import comp.Env;
import comp.Todo;
import hir.Module;
import hir.HIRGenerator;
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

	private Enter enter;
	private Todo<Env> todo;

	/**
	 * A flag that marks whether debug parer.
	 */
	private boolean debugParser = false;
	
	private boolean verbose = false;
	/**
	 * optimization level.
	 */
	private String opt_level;

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
		this.enter = Enter.instance(context);
		this.todo = Todo.instance(context);
		Options options = Options.instance(context);
		
		this.debugParser = options.get("--debug-Parser") != null;
		this.outputResult = options.get("-o") != null;
		this.opt_level = options.get("-O");
		
	}

	public static Compiler make(Context context)
	{
		return new Compiler(context);
	}

	/**
	 * The id of errors reported so far.
	 */
	public int errorCount()
	{
		return log.nerrors;
	}

	public void compile(List<SourceFile> filenames)
	{
		long msec = System.currentTimeMillis();
		Options opt = Options.instance(context);

		// machine specific not to do now

		Backend backend = new Backend(context, X86.target(),
				X86RegisterConfig.newInstance());
		Frontend frontend = new Frontend(context);
		Optimizer optimizer = new Optimizer(context);

		try
		{
			Tree[] trees = frontend.doParseAttribute(filenames);

			// performs high level IR generation and Module optimization
			Module[] hirLists = new Module[trees.length]; 
			int i = 0;
			for (Tree t : trees)
				hirLists[i++] = (new HIRGenerator(context).translate(t));
			
			// performs high level IR generation and Module optimization
			optimizer.runOnModules(hirLists);

			// emits machine instruction for Module instance of any TopLevel instance
			backend.emitMachineInst(hirLists);

			if (verbose)
			{
				printVerbose("total", Long.
						toString(System.currentTimeMillis() - msec));
			}
			int errCount = errorCount();
			if (errCount == 1)				
				printCount("error", errCount);
			else
				printCount("error.plural", errCount);
		}
		catch (Error ex)
		{
		}
		finally
		{

		}
	}
	/**
	 * Prints numbers of errors and warnings.
	 *
	 * @param key The key massage to be reported.
	 * @param cnt The count of errors and warnings.
	 */
	private void printCount(String key, int cnt)
	{
		if (cnt != 0)
		{
			Log.printLines(log.errWriter, Log.getLocalizedString("count." + key,
					Integer.toString(cnt)));
			log.flush();
		}
	}
	private void printVerbose(String key, String msg)
	{
		Log.printLines(log.noticeWriter,
		        Log.getLocalizedString("verbose." + key, msg));
	}

	private InputStream openSourcefile(String filename)
	{
		try
		{
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
	 * Parses the single c-flat source file by given filename. So that returns a
	 * TopLevel {@link TopLevel}
	 * 
	 * @param filename the file name of source file.
	 * @return The TopLevel.
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
			if (debugParser) printVerbose("parsing started", filename);
			try
			{
				Parser parser = new Parser(input, true, context);
				tree = parser.compilationUnit();
				if (debugParser)
				    printVerbose("parsing.done",
				            Long.toString(System.currentTimeMillis() - msec));

			}
			catch (ParseException e)
			{
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
	public void close()
	{
		log.flush();
		names.dispose();
	}
}
