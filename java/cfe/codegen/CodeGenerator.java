package cfe.codegen;

import backend.value.Module;
import cfe.ast.ASTConsumer;

/**
 * Extremely C Compiler. Jianping Zeng.
 */
public interface CodeGenerator extends ASTConsumer {
  Module getModule();
}
