package driver;

import ast.Tree;
import hir.HIR;
import lir.backend.TargetMachine;
import lir.backend.amd64.AMD64;
import lir.backend.amd64.AMD64RegisterConfig;
import lir.backend.x86.X86;
import utils.Context;
import utils.Log;
import utils.Name;
import java.util.List;

public class CompileInterface
{
	private static final Context.Key compilerKey = new Context.Key();

	/**
	 * The log to be used for error reporting.
	 */
	private Log log;

	/**
	 * The name table.
	 */
	private Name.Table names;

	private Context context;

	private boolean verbose = false;

	/**
	 * A flag that marks whether output targetAbstractLayer file.
	 */
	private boolean outputResult = false;

	private CompileInterface(Context context)
	{
		super();
		context.put(compilerKey, this);
		this.context = context;
		this.log = Log.instance(context);
		Options options = Options.instance(context);
		outputResult = options.get("-o") != null;
		verbose = options.get("-verbose") != null;
	}

	public static CompileInterface make(Context context)
	{
		return new CompileInterface(context);
	}

	public void compile(List<String> filenames)
	{
		long msec = System.currentTimeMillis();
		Options opt = Options.instance(context);

		// machine specific not to do now

		Backend backend = new Backend(opt, X86.target(),
				AMD64RegisterConfig.newInstance());
		Frontend frontend = new Frontend(opt, context);
		Optimizer optimizer = new Optimizer(context);

		try
		{
			Tree[] trees = frontend.doParseAttribute(filenames);

			// performs high level IR generation and HIR optimization
			HIR[] hirs = optimizer.emitHIR(trees);

			// emits machine instruction for HIR instance of any TopLevel instance
			backend.emitMachineInst(hirs);

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
	}

	/**
	 * The numbers of errors reported so far.
	 */
	public int errorCount()
	{
		return log.nerrors;
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

	/**
	 * Prints debugging information in human readability style.
	 *
	 * @param key
	 * @param msg
	 */
	void printVerbose(String key, String msg)
	{
		Log.printLines(log.noticeWriter,
				Log.getLocalizedString("verbose." + key, msg));
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
