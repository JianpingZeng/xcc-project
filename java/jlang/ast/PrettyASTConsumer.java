/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package jlang.ast;

import jlang.sema.ASTContext;
import jlang.sema.Decl;

import java.util.ArrayList;

/**
 * A empty action consumer for AST.
 *
 * @author Jianping Zeng
 * @version 0.1
 */
public class PrettyASTConsumer implements ASTConsumer {
  @Override
  public void initialize(ASTContext ctx) {
  }

  @Override
  public void handleTopLevelDecls(ArrayList<Decl> decls) {
  }

  @Override
  public void handleTranslationUnit(ASTContext context) {
  }
}
