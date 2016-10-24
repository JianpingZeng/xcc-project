package ast;

/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2016, Xlous
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

import ast.Tree.*;

/**
 * This is an interface in which all visitor method should be implemented by
 * another concrete subclass for specified purpose.
 * <br>
 * There is a {@linkplain ASTVisitor} class implements all visitor method
 * If you don't want implement all visitor method by self, it is desired that
 * extends your class from {@linkplain ASTVisitor}.
 *
 * @see ASTVisitor
 * @author Xlous.zeng
 * @version 0.1
 */
public interface IASTVisitor<T>
{
    /**
     * Visits the root class represents the top level tree node.
     * @param that
     */
    T visitTree(Tree that);

    /**
     *  A visitor method for traverse the {@linkplain TopLevel}.
     * @param tree
     */
    T visitTopLevel(TopLevel tree);

    T visitErroneous(ErroneousTree erroneous);

    //=================Statement visitor method===============================//
    T visitBreakStmt(BreakStmt stmt);

    T visitCaseStmt(CaseStmt stmt);

    T visitCompoundStmt(CompoundStmt stmt);

    T visitContinueStmt(ContinueStmt stmt);

    T visitDeclStmt(DeclStmt stmt);

    T visitDefaultStmt(DefaultStmt stmt);

    T visitDoStmt(DoStmt stmt);

    T visitForStmt(ForStmt stmt);

    T visitGotoStmt(GotoStmt stmt);

    T visitIfStmt(IfStmt stmt);

    T visitLabelledStmt(LabelledStmt stmt);

    T visitNullStmt(NullStmt stmt);

    T visitReturnStmt(ReturnStmt stmt);

    T visitSelectStmt(SelectStmt stmt);

    T visitSwitchStmt(SwitchStmt stmt);

    T visitWhileStmt(WhileStmt stmt);

    //================Expression visitor method===============================//

    //================Bianry operaotr=========================================//
    T visitBinaryExpr(BinaryExpr expr);

    T visitCompoundAssignExpr(CompoundAssignExpr expr);

    T visitConditionalExpr(ConditionalExpr expr);

    // Unary operator.
    T visitUnaryExpr(UnaryExpr expr);

    T visitUnaryExprOrTypeTraitExpr(UnaryExprOrTypeTraitExpr expr);

    // postfix operator
    T visitImplicitCastExpr(ImplicitCastExpr expr);

    T visitExplicitCastExpr(ExplicitCastExpr expr);

    T visitParenListExpr(ParenListExpr expr);

    T visitArraySubscriptExpr(ArraySubscriptExpr expr);

    T visitMemberExpr(MemberExpr expr);

    T visitParenExpr(ParenExpr expr);

    T visitCallExpr(CallExpr expr);

    T visitInitListExpr(InitListExpr expr);

    // Primary expression.
    T visitDeclRefExpr(DeclRefExpr expr);

    T visitCharacterLiteral(CharacterLiteral literal);

    T visitCompoundLiteralExpr(CompoundLiteralExpr literal);

    T visitFloatLiteral(FloatLiteral literal);

    T visitStringLiteral(StringLiteral literal);

    T visitIntegerLiteral(IntegerLiteral literal);
}
