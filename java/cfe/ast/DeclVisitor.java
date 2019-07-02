/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
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

package cfe.ast;

import cfe.sema.Decl;
import cfe.sema.Decl.*;
import tools.Util;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class DeclVisitor {
  public void visit(Decl decl) {
    switch (decl.getKind()) {
      case TranslationUnitDecl:
        visitTranslationUnitDecl((TranslationUnitDecl) decl);
        return;
      case FunctionDecl:
        visitFunctionDecl((FunctionDecl) decl);
        return;
      case RecordDecl:
        visitRecordDecl((RecordDecl) decl);
        return;
      case EnumDecl:
        visitEnumDecl((EnumDecl) decl);
        return;
      case TypedefDecl:
        visitTypedefDecl((TypeDefDecl) decl);
        return;
      case EnumConstant:
        visitEnumConstantDecl((EnumConstantDecl) decl);
        return;
      case LabelDecl:
        visitLabelDecl((LabelDecl) decl);
        return;
      case FieldDecl:
        visitFieldDecl((FieldDecl) decl);
        return;
      case VarDecl:
        visitVarDecl((VarDecl) decl);
        return;
      case ParamVarDecl:
        visitParamVarDecl((ParamVarDecl) decl);
        return;
      case OriginalParamVar:
        Util.assertion(false, "Should not reaching here!");
        return;
      case FileScopeAsmDecl:
        visitFileScopeAsmDecl((FileScopeAsmDecl) decl);
        break;
    }
  }

  public void visitTranslationUnitDecl(TranslationUnitDecl decl) {
  }

  public void visitTypedefDecl(TypeDefDecl decl) {
  }

  public void visitEnumDecl(EnumDecl decl) {
  }

  public void visitRecordDecl(RecordDecl decl) {
  }

  public void visitEnumConstantDecl(EnumConstantDecl decl) {
  }

  public void visitFunctionDecl(FunctionDecl decl) {
  }

  public void visitFieldDecl(FieldDecl decl) {
  }

  public void visitVarDecl(VarDecl decl) {
  }

  public void visitParamVarDecl(ParamVarDecl decl) {
  }

  public void visitLabelDecl(LabelDecl decl) {
  }

  public void visitFileScopeAsmDecl(FileScopeAsmDecl decl) {
  }
}
