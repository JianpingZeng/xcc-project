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

import jlang.sema.ASTContext;
import jlang.sema.Decl;
import jlang.sema.IDeclContext;
import jlang.support.PrintingPolicy;
import jlang.type.FunctionProtoType;
import jlang.type.FunctionType;
import jlang.type.QualType;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import static jlang.sema.Decl.VarDecl.getStorageClassSpecifierString;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class DeclPrinter extends DeclVisitor
{
    private PrintStream out;
    private ASTContext context;
    private PrintingPolicy policy;
    private int indentation;

    private PrintStream indent()
    {
        return out;
    }

    private void processDeclGroup(ArrayList<Decl> decls)
    {
    }

    public DeclPrinter(PrintStream os,
            ASTContext ctx,
            PrintingPolicy printingPolicy)
    {
        this(os, ctx, printingPolicy, 0);
    }

    public DeclPrinter(PrintStream os,
            ASTContext ctx,
            PrintingPolicy printingPolicy,
            int indent)
    {
        out = os;
        context = ctx;
        policy = printingPolicy;
        indentation = indent;
    }

    public void visitDeclContext(IDeclContext dc)
    {
        visitDeclContext(dc, true);
    }

    public void visitDeclContext(IDeclContext dc, boolean indent)
    {

    }

    public void visitTranslationUnitDecl(Decl.TranslationUnitDecl decl)
    {
        visitDeclContext(decl, false);
    }

    public void visitTypedefDecl(Decl.TypeDefDecl decl)
    {
        String name = decl.getNameAsString();
        name = decl.getUnderlyingType().getAsStringInternal(name, policy);
        if (!policy.suppressSpecifiers)
            out.print("typedef ");
        out.print(name);
    }

    public void visitEnumDecl(Decl.EnumDecl decl)
    {
        out.printf("enum %s{\n", decl.getNameAsString());
        visitDeclContext(decl);
        indent().print("}");
    }

    public void visitRecordDecl(Decl.RecordDecl decl)
    {
        out.printf("struct");
        if (decl.getIdentifier() != null)
        {
            out.printf(" %s", decl.getNameAsString());
        }
        if (decl.isBeingDefined())
        {
            out.print(" {\n");
            visitDeclContext(decl);
            indent().print("}");
        }
    }

    public void visitEnumConstantDecl(Decl.EnumConstantDecl decl)
    {
        out.print(decl.getNameAsString());
        Tree.Expr e = decl.getInitExpr();
        if (e != null)
        {
            out.printf(" = ");
            e.printPretty(out, context, null, policy, indentation);
        }
    }

    public void visitFunctionDecl(Decl.FunctionDecl decl)
    {
        if(!policy.suppressSpecifiers)
        {
            switch (decl.getStorageClass())
            {
                case SC_none:
                    break;
                case SC_extern:
                    out.print("extern ");
                    break;
                case SC_static:
                    out.print("static ");
                    break;
            }
            if (decl.isInlineSpecified())
                out.print("inline ");
        }

        PrintingPolicy subPolicy = new PrintingPolicy(policy);
        subPolicy.suppressSpecifiers = false;
        String proto = decl.getNameAsString();
        if (decl.getType().getType() instanceof FunctionType)
        {
            FunctionType ft = decl.getType().getAsFunctionType();

            FunctionProtoType fpt = null;
            if (decl.hasWrittenPrototype())
                fpt = ft.getAsFunctionProtoType();

            proto += "(";

            if (fpt != null)
            {

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream os = new PrintStream(baos);
                DeclPrinter paramPrinter = new DeclPrinter(os, context, subPolicy, indentation);
                for (int i = 0, e = decl.getNumParams(); i != e; i++)
                {
                    if (i != 0)
                        os.print(", ");
                    paramPrinter.visitParamVarDecl(decl.getParamDecl(i));
                }

                if (fpt.isVariadic())
                {
                    if(decl.getNumParams() > 0)
                        os.print(", ...");
                    else
                        os.print("...");
                }
                proto += baos.toString();
            }
            else if (decl.isThisDeclarationADefinition() && !decl.hasPrototype())
            {
                for (int i = 0, e = decl.getNumParams(); i != e; i++)
                {
                    if (i != 0)
                        proto += ", ";
                    proto += decl.getParamDecl(i).getNameAsString();
                }
            }

            proto += ")";
            proto = ft.getResultType().getAsStringInternal(proto, policy);
        }
        else
        {
            proto = decl.getType().getAsStringInternal(proto, policy);
        }

        out.print(proto);
        if (decl.isPure())
            out.print(" = 0");
        else if (decl.isThisDeclarationADefinition())
        {
            if (!decl.hasPrototype() && decl.getNumParams() != 0)
            {
                out.println();
                DeclPrinter paramPrinter = new DeclPrinter(out, context, subPolicy, indentation);
                indentation += paramPrinter.indentation;
                for (int i = 0, e = decl.getNumParams(); i != e; i++)
                {
                    indent();
                    paramPrinter.visitParamVarDecl(decl.getParamDecl(i));
                    out.println(";");
                }
                indentation -= paramPrinter.indentation;
            }
            else
                out.print(" ");

            decl.getBody().printPretty(out, context, null, subPolicy, indentation);;
            out.println();
        }
    }

    public void visitFieldDecl(Decl.FieldDecl decl)
    {
        if (!policy.suppressSpecifiers)
            out.print("mutale ");

        String name = decl.getNameAsString();
        name = decl.getType().getAsStringInternal(name, policy);
        out.print(name);

        if (decl.isBitField())
        {
            out.print(" : ");
            decl.getBitWidth().printPretty(out, context, null, policy, indentation);
        }
    }

    public void visitVarDecl(Decl.VarDecl decl)
    {
        if (!policy.suppressSpecifiers && decl.getStorageClass() != Decl.StorageClass.SC_none)
        {
            out.print(getStorageClassSpecifierString(decl.getStorageClass()));
        }

        String name = decl.getNameAsString();
        QualType t = decl.getType();
        if (decl instanceof Decl.OriginalParamVarDecl)
        {
            t = ((Decl.OriginalParamVarDecl)decl).getOriginalType();
        }
        name = t.getAsStringInternal(name, policy);
        out.print(name);

        if(decl.getInit() != null)
        {
            out.print(" = ");
            decl.getInit().printPretty(out, context, null, policy, indentation);
        }
    }

    public void visitParamVarDecl(Decl.ParamVarDecl decl)
    {
        visitVarDecl(decl);
    }

    public void visitLabelDecl(Decl.LabelDecl decl)
    {
        out.print(decl.getNameAsString() + ":\n");
        PrintingPolicy subPolicy = new PrintingPolicy(policy);
        indentation += subPolicy.indentation;
        StmtPrinter subPrinter = new StmtPrinter(out, context, null, subPolicy, indentation);
        subPrinter.visit(decl.getStmt());
        indentation -= subPolicy.indentation;
    }

    public void visitFileScopeAsmDecl(Decl.FileScopeAsmDecl decl)
    {
        out.print("__asm (");
        decl.getAsmString().printPretty(out, context, null, policy, indentation);
        out.print(")");
    }
}
