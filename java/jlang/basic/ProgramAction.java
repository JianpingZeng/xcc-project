package jlang.basic;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public enum ProgramAction
{
    /**
     * Specify the output file name.
     */
	OutputFile,

    /**
     * Specify whether just make preprocessing.
     */
    PrintPreprocessedInput,

    /**
     * Specify whether just the compilation process be enabled.
     */
    CompilationOnly,
    /**
     * Indicates if letting the compiler emit assembly code in target machine.
     */
    GenerateAsmCode,

    EmitLLVM,
	/**
	 * The default action taken by compiler.
	 */
	ParseSyntaxOnly,

    DebugParserFlag,

	/**
	 * Parse AST and dump them.
	 */
	ASTDump,
}
