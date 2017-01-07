package driver;

import tools.Context;

import java.util.HashMap;

/**
 * A table of all command-line options. IfStmt an option has an argument, the option
 * getName is mapped to the argument. IfStmt a set option has no argument, it is mapped
 * to itself.
 */
public class Options extends HashMap<String, String>
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 4537436982708186456L;
	/**
	 * The context key for the options.
	 */
	private static final Context.Key optionsKey = new Context.Key();

	/**
	 * Get the Options instance for this context.
	 */
	public static Options instance(Context context)
	{
		Options instance = (Options) context.get(optionsKey);
		if (instance == null) instance = new Options(context);
		return instance;
	}

	protected Options(Context context)
	{
		super();
		context.put(optionsKey, this);
	}

	public String remove(String key)
	{
		return super.remove(key);
	}

	public String put(String key, String msg)
	{
		return super.put(key, msg);
	}

	public String get(String key)
	{
		return super.get(key);
	}
	/**
	 * Checks if enable debug jlang.parser.
	 * @return if enable to debug jlang.parser, return true, otherwise return false.
	 */
	public boolean isDebugParser()
	{
		return get("-debug-Parser") != null;
	}
	/** 
	 * Checks if dump jlang.ast.
	 * @return return true when enable to dump jlang.ast, otherwise false returned.
	 */
	public boolean isDumpAst()
	{
		return get("-ast-dump") != null;
	}
	/** 
	 * Checks if dump hir(Higher Immediate Representation). 
	 * @return return true when enable to dump hir, otherwise false returned.
	 */
	public boolean isDumpHIR()
	{
		return get("--hir-dump") != null;
	}
	/** 
	 * Checks if turns on debugging information in preferred format for TargetData.
	 * @return return true when debugging is enabled, otherwise, return false.
	 */
	public boolean enableDebug()
	{
		return get("-g") != null;
	}
	/** 
	 * Checks if enable preprocessing. 
	 * @return return true when pre-processing is enabled.
	 */
	public boolean enablePreprocess()
	{
		return get("-E") != null;
	}
	/** 
	 * Checks if just working through compilation rather than no code emission. 
	 */
	public boolean enableCompiled()
	{
		return get("-C") != null;
	}
	
	public boolean enableAssembled()
	{
		return get("-S") != null;
	}
	/**
	 * Specify the getName of generated output file.
	 * @return
	 */
	public String outputFile()
	{
		return get("-o");
	}
	/**
	 * Obtains the specified level of backend.transform.
	 * @return
	 */
	public String optLevel()
	{
		return get("-O");				
	}
	/**
	 * Determines if display help information.
	 * @return
	 */
	public boolean displayHelp()
	{
		return get("-h") != null;
	}
	/**
	 * Checks if display version information onto screen.
	 * @return
	 */
	public boolean displayVersion()
	{
		return get("-v") != null;
	}

	public boolean isParseASTOnly()
	{
		return get("fsyntax-only") != null;
	}

	public boolean isEmitAssembly()
    {
	    return get("S")!=null;
	}
}
