package jlang.codegen;

import backend.value.Module;
import jlang.ast.ASTConsumer;

/**
 * Extremely C Compiler. Xlous zeng.
 */
public interface CodeGenerator extends ASTConsumer
{
    Module getModule();
}
