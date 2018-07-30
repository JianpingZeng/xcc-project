package jlang.codegen;

import backend.value.Module;
import jlang.ast.ASTConsumer;

/**
 * Extremely C Compiler. Jianping Zeng.
 */
public interface CodeGenerator extends ASTConsumer
{
    Module getModule();
}
