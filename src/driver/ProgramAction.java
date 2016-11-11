package driver;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public enum ProgramAction
{
	/**
	 * The default action taken by compiler.
	 */
	ParseSyntaxOnly("fsyntax-only", "Run parser and perform semantic analysis"),
	/**
	 * Emits a .s file.
	 */
	EmitAssembly("S", "Emit native assembly code"),
	/**
	 * Emits hir code file.
	 */
	EmitHIR("emit-hir", "Build ASTs then convert to HIR, emit .ir file"),
	/**
	 * Parse AST and dump them.
	 */
	ASTDump("ast-dump", "Build ASTs and then debug dump them");

	private String option;
	private String desc;

	ProgramAction(String opt, String desc)
	{
		option = opt;
		this.desc = desc;
	}

	public String getDescript() {return desc;}

	public String getOption() {return option;}
}
