package jlang.codegen;

import backend.hir.Module;
import jlang.ast.ASTConsumer;
import jlang.basic.CompileOptions;
import jlang.diag.Diagnostic;

/**
 * Extremely C Compiler. Xlous zeng.
 */
public interface CodeGenerator extends ASTConsumer
{
    Module getModule();
}
