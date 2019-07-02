package cfe.ast;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public interface StmtClass {
  /*
   * TopLevel nodes, of type TopLevel, representing entire source files.
   */
  int TopLevelClass = 1;

  int SelectExprClass = TopLevelClass + 1;

  /**
   * The no-op statement ";", of type NullStmt.
   */
  int NullStmtClass = SelectExprClass + 1;

  int DeclStmtClass = NullStmtClass + 1;

  /**
   * Blocks, of jlang.type CompoundStmt.
   */
  int CompoundStmtClass = DeclStmtClass + 1;

  /**
   * Do-while loop statement, of jlang.type Doloop.
   */
  int DoStmtClass = CompoundStmtClass + 1;

  /**
   * While loop statement, of jlang.type Whileloop.
   */
  int WhileStmtClass = DoStmtClass + 1;

  /**
   * For-loop, of jlang.type Forloop.
   */
  int ForStmtClass = WhileStmtClass + 1;

  /**
   * LabelStmt statement, of jlang.type LabelStmt.
   */
  int LabelledStmtClass = ForStmtClass + 1;

  /**
   * SwitchStmt statement, of jlang.type SwitchStmt.
   */
  int SwitchStmtClass = LabelledStmtClass + 1;

  /**
   * CaseStmt portions in switch statement, of jlang.type CaseStmt.
   */
  int CaseStmtClass = SwitchStmtClass + 1;

  int DefaultStmtClass = CaseStmtClass + 1;

  /**
   * ConditionalExpr expression, of jlang.type ConditionalExpr.
   */
  int ConditionalOperatorClass = DefaultStmtClass + 1;

  /**
   * IfStmt statements, of jlang.type IfStmt.
   */
  int IfStmtClass = ConditionalOperatorClass + 1;

  /**
   * Expression statements, of jlang.type Exec.
   */
  int ExprStmtClass = IfStmtClass + 1;

  /**
   * BreakStmt statements, of jlang.type BreakStmt.
   */
  int BreakStmtClass = ExprStmtClass + 1;

  int GotoStmtClass = BreakStmtClass + 1;

  /**
   * ContinueStmt statements, of jlang.type ContinueStmt.
   */
  int ContinueStmtClass = GotoStmtClass + 1;

  /**
   * ReturnInst statements, of jlang.type ReturnInst.
   */
  int ReturnStmtClass = ContinueStmtClass + 1;

  /**
   * FunctionProto invocation expressions, of jlang.type CallExpr.
   */
  int CallExprClass = ReturnStmtClass + 1;

  /**
   * Parenthesized subexpressions of jlang.type ParenExpr.
   */
  int ParenExprClass = CallExprClass + 1;

  int ParenListExprClass = ParenExprClass + 1;


  int InitListExprClass = ParenListExprClass + 1;

  /**
   * Implicit jlang.type cast expressions.
   */
  int ImplicitCastClass = InitListExprClass + 1;

  int ExplicitCastClass = ImplicitCastClass + 1;

  /**
   * ArraySubscriptExpr array expression, of jlang.type ArraySubscriptExpr.
   */
  int ArraySubscriptExprClass = ExplicitCastClass + 1;

  /**
   * Simple identifiers, of jlang.type DeclRefExpr.
   */
  int DeclRefExprClass = ArraySubscriptExprClass + 1;

  /**
   * Assignment expressions, of jlang.type Assign.
   */
  int AssignExprOperatorClass = DeclRefExprClass + 1;

  /**
   * Assignment operators, of jlang.type OpAssign.
   */
  int CompoundAssignOperatorClass = AssignExprOperatorClass
      + 1;

  /**
   * UnaryExpr operators, of jlang.type UnaryExpr.
   */
  int UnaryOperatorClass = CompoundAssignOperatorClass + 1;

  int UnaryExprOrTypeTraitClass = UnaryOperatorClass + 1;

  /**
   * BinaryExpr operators, of jlang.type BinaryExpr.
   */
  int BinaryOperatorClass = UnaryExprOrTypeTraitClass + 1;

  int MemberExprClass = BinaryOperatorClass + 1;

  int CompoundLiteralExprClass = MemberExprClass + 1;

  int IntegerLiteralClass = CompoundLiteralExprClass + 1;

  int FloatLiteralClass = IntegerLiteralClass + 1;

  int CharacterLiteralClass = FloatLiteralClass + 1;

  int StringLiteralClass = CharacterLiteralClass + 1;

  int SizeOfAlignOfExprClass = StringLiteralClass + 1;

  int StmtExprClass = SizeOfAlignOfExprClass + 1;

  int DesignatedInitExprClass = StmtExprClass + 1;

  int ImplicitValueInitExprClass = DesignatedInitExprClass + 1;

  /**
   * An array declares the dump name for each kind of Statement used in Tree.java
   */
  String[] StmtClassNames = {
      "",     // An dumper string for the erroreous Stmt.
      "TopLevel",
      "SelectExpr",
      "NullStmt",
      "DeclStmt",
      "CompoundStmt",
      "DoStmt",
      "WhileStmt",
      "ForStmt",
      "LabelledStmt",
      "SwitchStmt",
      "CaseStmt",
      "DefaultStmt",
      "ConditionalOperator",
      "IfStmt",
      "ExprStmt",
      "BreakStmt",
      "GotoStmt",
      "ContinueStmt",
      "ReturnStmt",
      "CallExpr",
      "ParenExpr",
      "ParenListExpr",
      "InitListExpr",
      "ImplicitCast",
      "ExplicitCast",
      "ArraySubscriptExpr",
      "DeclRefExpr",
      "AssignExprOperator",
      "CompoundAssignOperator",
      "UnaryOperator",
      "UnaryExprOrTypeTrait",
      "BinaryOperator",
      "MemberExpr",
      "CompoundLiteralExpr",
      "IntegerLiteral",
      "FloatLiteral",
      "CharacterLiteral",
      "StringLiteral",
      "SizeOfAlignOfExpr",
      "StmtExpr",
      "DesignatedInitExpr",
      "ImplicitValueInitExpr",
  };
}
