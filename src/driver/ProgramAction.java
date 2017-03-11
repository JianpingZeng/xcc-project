package driver;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public enum ProgramAction
{
    /**
     * Specify the output file name.
     */
	OutputFile("o", true, "Specify output file"),

    /**
     * Specify whether just make preprocessing.
     */
    Preprocessed("E", false,
            "Preprocess only, no compile, assembly or link"),

    /**
     * Specify whether just the compilation process be enabled.
     */
    CompilationOnly("c", false,
            "Compile and assemble but no link"),
    /**
     * Indicates if letting the compiler emit assembly code in target machine.
     */
    GenerateAsmCode("S", false, "Emit native assembly code"),

    /**
     * Specify the optimize level for speed.
     */
    OptSpeed("O", true,
            "Specify the level of backend.transform"),

    /**
     * Specify the optimization level just for decreasing the generated code size
     */
    OptSize("Os", false,
            "Specify the level of backend.transform"),

    /**
     * Specified whether to generate source code debug information.
     */
    GenerateDebugInfo("g", false,
            "Generate source level debug information"),

    /**
     * Print out the verbose information to stdout.
     */
    Verbose("v",false,
            "Enable verbose output"),

    /**
     * Display all option information in details.
     */
    Help("help", false,
            "Print out help information"),

	/**
	 * The default action taken by compiler.
	 */
	ParseSyntaxOnly("fsyntax-only", false,
            "Run parser and perform semantic analysis"),

    DebugParserFlag("fdebug-Parser", false,
            "Display the process of jlang.parser"),

	/**
	 * Emits hir code file.
	 */
	EmitIR("femit-hir", false,
            "Build ASTs then convert to HIR, emit .ir file"),
	/**
	 * Parse AST and dump them.
	 */
	ASTDump("fast-dump", false,
            "Build ASTs and then debug dump them");

	private String optName;
	private boolean hasArg;
	private String desc;

	ProgramAction(String opt, boolean hasArg, String desc)
	{
		optName = opt;
		this.hasArg = hasArg;
		this.desc = desc;
	}

	public String getDesc()
    {
        return desc;
    }

	public String getOptName()
    {
        return optName;
	}

    public boolean isHasArg()
    {
        return hasArg;
    }
}
