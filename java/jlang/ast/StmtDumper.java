/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
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

import jlang.ast.Tree.DeclStmt;
import jlang.ast.Tree.Expr;
import jlang.ast.Tree.Stmt;
import jlang.basic.SourceManager;
import jlang.sema.Decl;
import jlang.sema.Decl.TypeDefDecl;
import jlang.sema.Decl.ValueDecl;
import jlang.sema.Decl.VarDecl;
import jlang.support.PresumedLoc;
import jlang.support.PrintingPolicy;
import jlang.support.SourceLocation;
import jlang.support.SourceRange;
import jlang.type.QualType;
import jlang.type.TypedefType;
import tools.TextUtils;

import java.io.PrintStream;
import java.util.ArrayList;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class StmtDumper extends StmtVisitor<Void>
{
    private SourceManager smg;
    private PrintStream os;
    private int indentLevel;

    private int maxDepth;
    private String lastLocFilename;
    private int lastLocLine;

    public StmtDumper(SourceManager sgr, PrintStream os, int maxDepth)
    {
        smg = sgr;
        this.os = os;
        indentLevel = 0;
        this.maxDepth = maxDepth;
        lastLocFilename = "";
        lastLocLine = ~0;
    }

    public void dumpSubTree(Tree s)
    {
        if (maxDepth == 0)
            return;

        ++indentLevel;
        if (s != null)
        {
            if (s instanceof DeclStmt)
            {
                visitDeclStmt((DeclStmt)s);
            }
            else
            {
                visit(s);

                // Print out children.
                for (int i = 0, e = s.getNumChildren(); i != e; i++)
                {
                    os.println();
                    dumpSubTree(s.getChildren(i));
                }
                os.printf(")");
            }
        }
        else
        {
            indent();
            os.print("<<NULL>>");
        }
        --indentLevel;
    }

    private void indent()
    {
        for (int i = indentLevel; i > 0; i--)
            os.print(' ');
    }

    private void dumpType(QualType ty)
    {
        os.printf("'%s'", ty.getAsString());

        if (!ty.isNull())
        {
            // If the type is directly a typedef, strip off typedefness
            // to given at least one level of concreteness.
            if (ty.getType() instanceof TypedefType)
            {
                TypedefType tdf = (TypedefType)ty.getType();
                QualType simplified = tdf.lookThroughTypedefs()
                        .getQualifiedType(ty.getCVRQualifiers());
                os.printf("'%s'", simplified.getAsString());
            }
        }
    }

    private void dumpStmt(Stmt node)
    {
        indent();
        os.printf("(%s %o)", node.getStmtClassName(), node.hashCode());
        dumpSourceRange(node);
    }

    private void dumpExpr(Expr node)
    {
        dumpStmt(node);
        os.print(' ');
        dumpType(node.getType());
    }

    private void dumpSourceRange(Stmt node)
    {
        if (node == null)
            return;

        SourceRange r = node.getSourceRange();
        os.print('<');
        dumpLocation(r.getBegin());
        if (!r.getEnd().equals(r.getBegin()))
        {
            os.print(", ");
            dumpLocation(r.getEnd());
        }
        os.print('>');
    }

    private void dumpLocation(SourceLocation loc)
    {
        SourceLocation spellingLoc = smg.getLiteralLoc(loc);
        if (!spellingLoc.isValid())
        {
            os.print("<invalid sloc>");
            return;
        }

        PresumedLoc ploc = smg.getPresumedLoc(spellingLoc);
        if (!ploc.getFilename().equals(lastLocFilename))
        {
            os.printf("%s:%d:%d", ploc.getFilename(), ploc.getLine(), ploc.getColumn());
            lastLocFilename = ploc.getFilename();
            lastLocLine = ploc.getLine();
        }
        else if (ploc.getLine() != lastLocLine)
        {
            os.printf("line:%d%d", ploc.getLine(), ploc.getColumn());
            lastLocLine = ploc.getLine();
        }
        else
        {
            os.printf("col:%d", ploc.getColumn());
        }
    }

    private void dumpDeclaration(Decl d)
    {
        if (d instanceof TypeDefDecl)
        {
            TypeDefDecl tdd = (TypeDefDecl)d;
            os.printf("\"typedef %s %s\"", tdd.getUnderlyingType().getAsString(),
                    tdd.getNameAsString());
        }
        else if (d instanceof ValueDecl)
        {
            ValueDecl vd = (ValueDecl)d;
            os.print('"');
            if (vd instanceof VarDecl)
            {
                VarDecl v = (VarDecl)vd;
                if (v.getStorageClass() != Decl.StorageClass.SC_none)
                    os.printf("%s ", VarDecl.getStorageClassSpecifierString(v.getStorageClass()));
            }

            String name = vd.getNameAsString();
            name = vd.getType().getAsStringInternal(name, new PrintingPolicy(vd.getASTContext().getLangOptions()));
            os.printf(name);

            // If this is a vardecl with an ininitializer emit it.
            if (vd instanceof VarDecl)
            {
                VarDecl v = (VarDecl)vd;
                Expr init = v.getInit();
                if (init != null)
                {
                    os.println(" =");
                    dumpSubTree(init);
                }
            }
            os.print('"');
        }
        else if (d instanceof Decl.TagDecl)
        {
            Decl.TagDecl td = (Decl.TagDecl)d;
            // print a free standing tag decl (e.g. "struct x;").
            String tagname = "";
            if (td.getIdentifier() != null)
                tagname = td.getIdentifier().getName();
            else
                tagname = "<anonymous>";
            os.printf("\"%s %s;\"", td.getKindName(), tagname);
        }
        else
        {
            assert false:"Unexpected decl";
        }
    }

    @Override
    public Void visitDeclStmt(DeclStmt stmt)
    {
        dumpStmt(stmt);
        os.println();

        ArrayList<Decl> decls = stmt.getDeclGroup();
        for (int i = 0, e = decls.size(); i != e; i++)
        {
            Decl d = decls.get(i);
            ++indentLevel;
            indent();
            os.printf("0x%o", d.hashCode());
            dumpDeclaration(d);
            if (i != e - 1)
            {
                os.println();
            }
            --indentLevel;
        }
        return null;
    }

    @Override
    public Void visitLabelledStmt(Tree.LabelStmt stmt)
    {
        dumpStmt(stmt);
        os.printf(" '%s'", stmt.getName());
        return null;
    }

    @Override
    public Void visitGotoStmt(Tree.GotoStmt stmt)
    {
        dumpStmt(stmt);
        os.printf(" '%s': 0x%o", stmt.getLabel().getNameAsString(), stmt.getLabel().hashCode());
        return null;
    }

    public void visitExpr(Tree.BinaryExpr expr)
    {
        dumpExpr(expr);
    }

    @Override
    public Void visitDeclRefExpr(Tree.DeclRefExpr expr)
    {
        dumpExpr(expr);
        os.print(' ');

        switch (expr.getDecl().getKind())
        {
            case FunctionDecl:
                os.print("FunctionDecl");
                break;
            case VarDecl:
                os.print("VarDecl");
                break;
            case ParamVarDecl:
                os.print("ParamVar");
                break;
            case EnumConstant:
                os.print("EnumConstant");
                break;
            case TypedefDecl:
                os.print("Typedef");
                break;
            case RecordDecl:
                os.print("Record");
                break;
            case EnumDecl:
                os.print("Enum");
                break;
            default:
                os.print("Decl");
                break;
        }
        os.printf("='%s' 0x%o", expr.getDecl().getNameAsString(),
                expr.getDecl().hashCode());
        return null;
    }

    @Override
    public Void visitCharacterLiteral(Tree.CharacterLiteral literal)
    {
        dumpExpr(literal);
        os.printf("%d", literal.getValue());
        return null;
    }

    @Override
    public Void visitIntegerLiteral(Tree.IntegerLiteral literal)
    {
        dumpExpr(literal);

        boolean isSigned = literal.getType().isSignedIntegerType();
        os.printf(" %s", literal.getValue().toString(10, isSigned));
        return null;
    }

    @Override
    public Void visitFloatLiteral(Tree.FloatingLiteral literal)
    {
        dumpExpr(literal);
        os.printf(" %f", literal.getValue().convertToDouble());
        return null;
    }

    @Override
    public Void visitStringLiteral(Tree.StringLiteral literal)
    {
        dumpExpr(literal);

        String str = literal.getStrData();
        for (int i = 0, e = literal.getByteLength(); i != e; i++)
        {
            char ch = str.charAt(i);
            switch (ch)
            {
                default:
                    if (TextUtils.isPrintable(ch))
                        os.print(ch);
                    else
                        os.printf("\\%03o", ch);
                    break;
                case '\\':
                    os.printf("\\\\");
                    break;
                case '"':
                    os.printf("\\\"");
                    break;
                case '\n':
                    os.printf("\\n");
                    break;
                case '\t':
                    os.printf("\\t");
                    break;
                case 7:
                    os.printf("\\a");
                    break;
                case '\b':
                    os.printf("\\b");
                    break;
            }
        }
        os.print('"');
        return null;
    }

    public Void visitUnaryExpr(Tree.UnaryExpr expr)
    {
        dumpExpr(expr);

        os.printf(" %s '%s'", expr.isPostifx()?"postfix":"prefix",
                Tree.UnaryExpr.getOpcodeStr(expr.getOpCode()));
        return null;
    }

    @Override
    public Void visitSizeofAlignofExpr(Tree.SizeOfAlignOfExpr expr)
    {
        dumpExpr(expr);

        os.printf(" sizeof ");
        if (expr.isArgumentType())
            dumpType(expr.getArgumentType());
        return null;
    }

    @Override
    public Void visitMemberExpr(Tree.MemberExpr expr)
    {
        dumpExpr(expr);

        os.printf("%s%s 0x%o", expr.isArrow()?"->":".",
                expr.getMemberDecl().getNameAsString(),
                expr.getMemberDecl().hashCode());
        return null;
    }

    public Void visitBinaryExpr(Tree.BinaryExpr expr)
    {
        dumpExpr(expr);
        os.printf(" '%s'", Tree.BinaryExpr.getOpcodeStr(expr.getOpcode()));
        return null;
    }

    @Override
    public Void visitCompoundAssignExpr(Tree.CompoundAssignExpr expr)
    {
        dumpExpr(expr);
        os.printf(" '%s' ComputeLHSTy=", Tree.BinaryExpr.getOpcodeStr(expr.getOpcode()));
        dumpType(expr.getComputationLHSType());
        os.printf(" ComputeResultTy=");
        dumpType(expr.getComputationResultType());
        return null;
    }
}
