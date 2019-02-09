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

package cfe.ast;


import cfe.sema.ASTContext;
import cfe.sema.Decl;
import cfe.support.PrintingPolicy;
import cfe.type.TypeClass;
import tools.TextUtils;
import tools.Util;

import java.io.PrintStream;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class StmtPrinter extends StmtVisitor<Void> {
  private PrintStream os;
  private ASTContext context;
  private PrinterHelper helper;
  private PrintingPolicy policy;
  private int indentation;

  public StmtPrinter(PrintStream out,
                     ASTContext ctx,
                     PrinterHelper helper,
                     PrintingPolicy policy,
                     int indentation) {
    this.os = out;
    this.context = ctx;
    this.helper = helper;
    this.policy = policy;
    this.indentation = indentation;
  }

  private PrintStream indent() {
    return indent(0);
  }

  private PrintStream indent(int delta) {
    os.print(Util.fixedLengthString(indentation - delta, ' '));
    return os;
  }

  public void printStmt(Tree.Stmt s) {
    printStmt(s, policy.indentation);
  }

  public void printStmt(Tree.Stmt s, int subIndent) {
    indentation += subIndent;
    if (s != null && s instanceof Tree.Expr) {
      indent();
      visit(s);
      os.println(";");
    } else if (s != null) {
      visit(s);
    } else {
      indent().println("<<NULL STATEMENT>>");
    }
    indentation -= subIndent;
  }

  /**
   * Print a compound stmt without indenting the {, and
   * with no newline after the }.
   *
   * @param cs
   */
  public void printRawCompoundStmt(Tree.CompoundStmt cs) {
    os.print("{\n");
    for (Tree.Stmt s : cs.getBody()) {
      printStmt(s);
    }
    indent().print("}");
  }

  public void printRawDecl(Decl d) {
    d.print(os, policy, indentation);
  }

  public void printRawDeclStmt(Tree.DeclStmt s) {
    Decl.printGroup(s.getDeclGroup(), os, policy, indentation);
  }

  public void printRawIfStmt(Tree.IfStmt s) {
    os.print("if (");
    printExpr(s.getCond());
    os.print(")");

    Tree.Stmt thenStmt = s.getThenPart();
    if (thenStmt instanceof Tree.CompoundStmt) {
      Tree.CompoundStmt cs = (Tree.CompoundStmt) thenStmt;
      os.print(" ");
      printRawCompoundStmt(cs);
      os.print(s.getElsePart() != null ? " " : "\n");
    } else {
      os.println();
      printStmt(s.getThenPart());
      if (s.getElsePart() != null)
        indent();
    }

    Tree.Stmt elsePart = s.getElsePart();
    if (elsePart != null) {
      os.print("else");

      if (elsePart instanceof Tree.CompoundStmt) {
        Tree.CompoundStmt cs = (Tree.CompoundStmt) elsePart;
        os.print(" ");
        printRawCompoundStmt(cs);
        os.println();
      } else if (elsePart instanceof Tree.IfStmt) {
        os.print(" ");
        printRawIfStmt((Tree.IfStmt) elsePart);
      } else {
        os.println();
        printStmt(elsePart);
      }
    }
  }

  public void printCallArgs(Tree.CallExpr ce) {
    if (ce.getNumArgs() <= 0)
      return;

    printExpr(ce.getArgAt(0));

    for (int i = 1, e = ce.getNumArgs(); i != e; i++) {
      os.print(", ");
      printExpr(ce.getArgAt(i));
    }
  }

  public void printExpr(Tree.Expr e) {
    if (e != null)
      visit(e);
    else
      os.print("<null expr>");
  }


  @Override
  public Void visitStmt(Tree.Stmt s) {
    os.println("<<Error: unknown stmt>>");
    return null;
  }

  //=================Statement visitor method===============================//
  public Void visitBreakStmt(Tree.BreakStmt stmt) {
    os.println("break;");
    return null;
  }

  public Void visitCaseStmt(Tree.CaseStmt stmt) {
    indent(-2).print("case ");
    printExpr(stmt.getCondExpr());
    os.println(":");
    printStmt(stmt.getSubStmt());
    return null;
  }

  public Void visitCompoundStmt(Tree.CompoundStmt stmt) {
    indent();
    printRawCompoundStmt(stmt);
    os.println();
    return null;
  }

  public Void visitContinueStmt(Tree.ContinueStmt stmt) {
    indent();
    os.println("continue;");
    return null;
  }

  public Void visitDeclStmt(Tree.DeclStmt stmt) {
    indent();
    printRawDeclStmt(stmt);
    os.println(";");
    return null;
  }

  public Void visitDefaultStmt(Tree.DefaultStmt stmt) {
    indent(-1).println("default:");
    printStmt(stmt.getSubStmt(), 0);
    return null;
  }

  public Void visitDoStmt(Tree.DoStmt stmt) {
    indent().print("do ");
    Tree.Stmt body = stmt.getBody();
    if (body instanceof Tree.CompoundStmt)
      printRawCompoundStmt((Tree.CompoundStmt) body);
    else {
      indent();
      printStmt(stmt.getBody());
    }
    os.print("while(");
    printExpr(stmt.getCond());
    os.println(");");
    return null;
  }

  public Void visitForStmt(Tree.ForStmt stmt) {
    indent().print("for(");
    printStmt(stmt.getInit(), 0);
    os.print("; ");
    printExpr(stmt.getCond());
    os.print("; ");
    printExpr(stmt.getStep());
    os.print(";");

    Tree.Stmt body = stmt.getBody();
    if (body instanceof Tree.CompoundStmt)
      printRawCompoundStmt((Tree.CompoundStmt) body);
    else {
      indent();
      printStmt(stmt.getBody());
    }
    return null;
  }

  public Void visitGotoStmt(Tree.GotoStmt stmt) {
    indent().print("goto ");
    os.print(stmt.getLabel().getStmt().getName());
    os.println();
    return null;
  }

  public Void visitIfStmt(Tree.IfStmt stmt) {
    indent();
    os.print("if (");
    printExpr(stmt.getCond());
    os.print(")");
    printRawIfStmt(stmt);
    return null;
  }

  public Void visitLabelledStmt(Tree.LabelStmt stmt) {
    indent(-1).println(stmt.getName() + ":");
    printStmt(stmt.getSubStmt(), 0);
    return null;
  }

  public Void visitNullStmt(Tree.NullStmt stmt) {
    indent();
    os.println(";");
    return null;
  }

  public Void visitReturnStmt(Tree.ReturnStmt stmt) {
    indent().print("return");
    Tree.Expr e = stmt.getRetValue();
    if (e == null)
      os.println(";");
    else {
      os.print(" ");
      printExpr(e);
      os.println();
    }
    return null;
  }

  public Void visitSelectStmt(Tree.SelectStmt stmt) {
    Util.assertion(false, "Cannot reaching here!");
    return null;
  }

  public Void visitSwitchStmt(Tree.SwitchStmt stmt) {
    indent().print("switch (");
    printExpr(stmt.getCond());
    os.print(")");
    Tree.Stmt body = stmt.getBody();
    if (body instanceof Tree.CompoundStmt) {
      os.print(" ");
      printRawCompoundStmt((Tree.CompoundStmt) body);
      os.println();
    } else {
      os.println();
      printStmt(body);
    }
    return null;
  }

  public Void visitWhileStmt(Tree.WhileStmt stmt) {
    indent().print("while (");
    printExpr(stmt.getCond());
    os.print(")");
    Tree.Stmt body = stmt.getBody();
    if (body instanceof Tree.CompoundStmt) {
      os.print(" ");
      printRawCompoundStmt((Tree.CompoundStmt) body);
      os.println();
    } else {
      os.println();
      printStmt(body);
    }
    return null;
  }

  //================Expression visitor method===============================//

  //================Bianry operator=========================================//
  public Void visitBinaryExpr(Tree.BinaryExpr expr) {
    printExpr(expr.getLHS());
    os.printf(" %s ", expr.getOpcodeStr());
    printExpr(expr.getRHS());
    return null;
  }

  public Void visitCompoundAssignExpr(Tree.CompoundAssignExpr expr) {
    printExpr(expr.getLHS());
    os.printf(" %s ", expr.getOpcodeStr());
    printExpr(expr.getRHS());
    return null;
  }

  public Void visitConditionalExpr(Tree.ConditionalExpr expr) {
    printExpr(expr.getCond());
    os.print(" ? ");
    printExpr(expr.getTrueExpr());
    os.print(" : ");
    printExpr(expr.getFalseExpr());
    return null;
  }

  // Unary operator.
  public Void visitUnaryExpr(Tree.UnaryExpr expr) {
    return visitStmt(expr);
  }

  public Void visitSizeofAlignofExpr(Tree.UnaryExprOrTypeTraitExpr expr) {
    Util.assertion(false, "Should not handle TypeTraitExpr");
    return null;
  }

  // postfix operator
  public Void visitImplicitCastExpr(Tree.ImplicitCastExpr expr) {
    printExpr(expr.getSubExpr());
    return null;
  }

  public Void visitExplicitCastExpr(Tree.ExplicitCastExpr expr) {
    os.print("(");
    os.print(expr.getType().getAsStringInternal("", policy));
    os.print(")");
    printExpr(expr.getSubExpr());
    return null;
  }

  public Void visitParenListExpr(Tree.ParenListExpr expr) {
    os.print("(");
    for (int i = 0, e = expr.getNumExprs(); i != e; i++) {
      if (i + 1 != e)
        os.print(", ");
      printExpr(expr.getExpr(i));
    }
    os.print(")");
    return null;
  }

  public Void visitArraySubscriptExpr(Tree.ArraySubscriptExpr expr) {
    printExpr(expr.getBase());
    os.print("[");
    printExpr(expr.getIdx());
    os.print("]");
    return null;
  }

  public Void visitMemberExpr(Tree.MemberExpr expr) {
    printExpr(expr.getBase());
    os.print(expr.isArrow() ? "->" : ".");
    printRawDecl(expr.getMemberDecl());
    return null;
  }

  public Void visitParenExpr(Tree.ParenExpr expr) {
    os.print("(");
    printExpr(expr.getSubExpr());
    os.print(")");
    return null;
  }

  public Void visitCallExpr(Tree.CallExpr expr) {
    printExpr(expr.getCallee());
    os.print("(");
    printCallArgs(expr);
    os.print(")");
    return null;
  }

  public Void visitInitListExpr(Tree.InitListExpr expr) {
    if (expr.getSyntacticForm() != null) {
      visit(expr.getSyntacticForm());
      return null;
    }

    os.print("{");
    if (expr.getNumInits() <= 0)
      return null;
    Tree.Expr e = expr.getInitAt(0);
    if (e == null)
      os.print("0");
    else
      printExpr(expr.getInitAt(0));
    for (int i = 1, sz = expr.getNumInits(); i != sz; i++) {
      os.print(", ");
      e = expr.getInitAt(i);
      if (e == null)
        os.print("0");
      else
        printExpr(expr.getInitAt(i));
    }
    os.print("}");
    return null;
  }

  public Void visitDesignatedInitExpr(Tree.DesignatedInitExpr expr) {
    for (Tree.DesignatedInitExpr.Designator d : expr.getDesignators()) {
      if (d.isFieldDesignator()) {
        if (!d.getDotLoc().isValid()) {
          os.print(d.getFieldName().getName() + ":");
        } else {
          os.print("." + d.getFieldName().getName());
        }
      } else {
        os.print("[");
        if (d.isArrayDesignator()) {
          printExpr(expr.getArrayIndex(d));
        } else {
          printExpr(expr.getArrayRangeStart(d));
          os.print(" ... ");
          printExpr(expr.getArrayRangeEnd(d));
        }
        os.print("]");
      }
    }
    os.print(" = ");
    printExpr(expr.getInit());
    return null;
  }

  public Void visitImplicitValueExpr(Tree.ImplicitValueInitExpr expr) {
    os.print("(");
    os.print(expr.getType().getAsStringInternal("", policy));
    os.print(")");
    if (expr.getType().isRecordType())
      os.print("{}");
    else
      os.print(0);
    return null;
  }

  // Primary expression.
  public Void visitDeclRefExpr(Tree.DeclRefExpr expr) {
    os.print(expr.getName().getName());
    return null;
  }

  public Void visitCharacterLiteral(Tree.CharacterLiteral literal) {
    int val = literal.getValue();

    switch (val) {
      case '\\':
        os.print("\\\\");
        break;
      case '\'':
        os.print("'\\''");
        break;
      case 7: // '\a'
        os.print("\\a");
        break;
      case '\b':
        os.print("\\b");
        break;
      case '\f':
        os.print("\\f");
        break;
      case '\n':
        os.print("\\n");
        break;
      case '\r':
        os.print("\\r");
        break;
      case '\t':
        os.print("\\t");
        break;
      case 11: // '\v'
        os.print("\\v");
        break;
      default:
        if (val < 256 && TextUtils.isPrintable(val)) {
          os.print("'" + (char) val + "'");
        } else if (val < 256) {
          os.print("'\\");
          os.printf("%x", val);
          os.print("'");
        } else {
          os.print(val);
        }
    }
    return null;
  }

  public Void visitCompoundLiteralExpr(Tree.CompoundLiteralExpr literal) {
    os.print("(" + literal.getType().getAsStringInternal("", policy) + ")");
    printExpr(literal.getInitializer());
    return null;
  }

  public Void visitFloatLiteral(Tree.FloatingLiteral literal) {
    os.print(literal.getValue().toString());
    return null;
  }

  public Void visitStringLiteral(Tree.StringLiteral literal) {
    os.print("'");
    String data = literal.getStrData();
    for (int i = 0, e = data.length(); i != e; i++) {
      char ch = data.charAt(i);
      switch (ch) {
        default: {
          if (TextUtils.isPrintable(ch)) {
            os.print(ch);
          } else {
            os.print("\\");
            os.print((char) ('0' + ((ch >> 6) & 7)));
            os.print((char) ('0' + ((ch >> 3) & 7)));
            os.print((char) ('0' + ((ch) & 7)));
          }
          break;
        }
        case '\\':
          os.print("\\\\");
          break;
        case '"':
          os.print("\\\"");
          break;
        case '\n':
          os.print("\\n");
          break;
        case '\t':
          os.print("\\t");
          break;
        case 7: // '\a'
          os.print("\\a");
          break;
        case '\b':
          os.print("\\b");
          break;
      }
    }
    os.print("'");
    return null;
  }

  public Void visitIntegerLiteral(Tree.IntegerLiteral literal) {
    boolean isSigned = literal.getType().isSignedIntegerType();
    literal.getValue().print(os, isSigned);
    switch (literal.getType().getTypeClass()) {
      case TypeClass.Int:
        break;
      case TypeClass.UInt:
        os.printf("U");
        break;
      case TypeClass.Long:
        os.print("L");
        break;
      case TypeClass.ULong:
        os.print("UL");
        break;
      case TypeClass.LongLong:
        os.print("LL");
        break;
      case TypeClass.ULongLong:
        os.print("ULL");
        break;
    }
    return null;
  }
}
