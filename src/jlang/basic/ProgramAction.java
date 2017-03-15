package jlang.basic;

import java.util.function.Predicate;

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
    OptSpeed("O", true, "Specify the level of backend.transform",
            (opt)->
            {
                String val = opt.getValue();
                try
                {
                    int lev = Integer.parseInt(val);
                    if (lev < 0 || lev > 3)
                        return false;
                    return true;
                }
                catch (NumberFormatException ex)
                {
                    return false;
                }
            }),

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
            "Build ASTs and then debug dump them"),

	DefaultVisibility("fvisibility", true, "Set the default symbol visibility",
            opt->
            {
                String val = opt.getValue();
               return val.equals("Default") || val.equals("Hidden")
                       || val.equals("Protected");
            }),

	Std("std", true, "Language standard to compile for",
            opt->
            {
                String val = opt.getValue();
                return val.equals("c89") || val.equals("c90") ||
                        val.equals("c99") || val.equals("c11")
                        || val.equals("gnu89") || val.equals("gnu99");
            }),

	Trigraph("trigraphs", false, "Process trigraph sequences"),

	DollarInIdents("fdollars-in-identifiers", false, "Allow '$' in identifier"),

	Isysroot("isysroot", true, "Set the system root directory (usually '/')"),

	I_dirs("I", true, "Add include to the include search path"),

	Iquote_dirs("iquote", true, "Add directory to QUOTE include search path"),

	Isystem("isystem", true, "Add directory to SYSTEM include search path"),

	Nostdinc("nostdinc", false, "Disable standard #include directories"),

    // Preprocessor initialization.
	D_macros("D", true, "Predefine the specified macros"),

	U_macros("U", true, "Undefine the specified macros");

	private String optName;
	private boolean hasArg;
	private String desc;
	private Predicate<CustomOption> checker;

	ProgramAction(String opt, boolean hasArg, String desc)
	{
		this(opt, hasArg, desc, null);
	}

    ProgramAction(String opt, boolean hasArg, String desc,
            Predicate<CustomOption> checker)
    {
        optName = opt;
        this.hasArg = hasArg;
        this.desc = desc;
        this.checker = checker;
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

    public Predicate<CustomOption> getChecker()
    {
        return checker;
    }
}
