package jlang.support;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public enum ProgramAction
{
    /**
     * Specify the output file asmName.
     */
	OutputFile,

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

    /**
     * Just lex as fast as we can, no output.
     */
    RunPreprocessorOnly,

    /**
     * -E mode.
     */
    PrintPreprocessedInput,

    DumpTokens,
}
