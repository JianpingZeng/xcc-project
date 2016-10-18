package compiler;

import java.util.Map;
import java.util.HashMap;

/**
 * A enumerator for compilation mode.
 * @author zeng
 *
 */
enum CompilerMode {
	
	/**
	 * Different constructor for every instance.
	 */
    CheckSyntax ("--check-syntax"),
    DumpTokens ("--dump-tokens"),
    DumpAST ("--dump-ast"),
    DumpStmt ("--dump-stmt"),
    DumpExpr ("--dump-subExpr"),
    DumpSemantic ("--dump-semantic"),
    DumpReference ("--dump-reference"),
    DumpIR ("--dump-ir"),
    DumpAsm ("--dump-asm"),
    PrintAsm ("--print-asm"),
    Compile ("-c"),
    Assemble ("-S"),
    Link ("--link");

	/**
	 * A map that maps string represents compilation option into the instance of CompilerMode.
	 */
    static private Map<String, CompilerMode> modes;
    
    static {
        modes = new HashMap<String, CompilerMode>();
        modes.put("--check-syntax", CheckSyntax);
        modes.put("--dump-tokens", DumpTokens);
        modes.put("--dump-ast", DumpAST);
        modes.put("--dump-stmt", DumpStmt);
        modes.put("--dump-subExpr", DumpExpr);
        modes.put("--dump-semantic", DumpSemantic);
        modes.put("--dump-reference", DumpReference);
        modes.put("--dump-ir", DumpIR);
        modes.put("--dump-asm", DumpAsm);
        modes.put("--print-asm", PrintAsm);
        modes.put("-c", Compile);
        modes.put("-S", Assemble);
    }

    /**
     * Determines the given opt whether is legal or not.
     * @param opt	a specified option
     * @return	true returned if opt is legal ,otherwise false.
     */
    static public boolean isModeOption(String opt) {
        return modes.containsKey(opt);
    }

    /**
     * Gets the compilerMode according to a string represents a option.
     * @param opt	the given option
     * @return	a compilerMode corresponding to given option.
     * @Error if option specified is illegal.
     */
    static public CompilerMode fromOption(String opt) {
        CompilerMode m = modes.get(opt);
        if (m == null) {
            throw new Error("must not happen: unknown mode option: " + opt);
        }
        return m;
    }

    private final String option;

    CompilerMode(String option) {
        this.option = option;
    }

    public String toOption() {
        return option;
    }

    boolean requires(CompilerMode m) {
        return ordinal() >= m.ordinal();
    }
}
