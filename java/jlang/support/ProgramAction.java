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
     * Print the AST node in terms of source code.
     */
    ASTPrint,

    /**
     * Just lex as fast as we can, no output.
     */
    RunPreprocessorOnly,

    /**
     * -E mode.
     */
    PrintPreprocessedInput,

    DumpTokens,

    /**
     * Parse with noop callbacks.
     */
    ParseNoop,

    /**
     * Indicates if letting the compiler emit assembly code in target machine.
     */
    EmitAssembly,
}
