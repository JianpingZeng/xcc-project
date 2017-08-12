package jlang.codegen;

import backend.value.Module;
import backend.target.TargetData;
import jlang.support.CompileOptions;
import jlang.diag.Diagnostic;
import jlang.sema.ASTContext;
import jlang.sema.Decl;

import java.util.ArrayList;

/**
 * Extremely C Compiler. Xlous zeng.
 */
public class CodeGeneratorImpl implements CodeGenerator
{
    private Diagnostic diags;
    private CompileOptions compOpts;
    private Module m;
    private ASTContext ctx;
    private HIRModuleGenerator builder;
    private TargetData td;

    public CodeGeneratorImpl(Diagnostic diags, String moduleName, CompileOptions compOpts)
    {
        this.diags = diags;
        this.compOpts = compOpts;
        m = new Module(moduleName);
    }

    @Override
    public Module getModule()
    {
        return m;
    }

    @Override
    public void initialize(ASTContext context)
    {
        ctx = context;
        m.setTargetTriple(ctx.target.getTargetTriple());
        m.setDataLayout(ctx.target.getTargetDescription());
        td = new TargetData(ctx.target.getTargetDescription());
        builder = new HIRModuleGenerator(context, compOpts, m, td, diags);
    }

    @Override
    public void handleTopLevelDecls(ArrayList<Decl> decls)
    {
        // Make sure to emit all elements of a Decl.
        decls.forEach(builder::emitTopLevelDecl);
    }

    @Override
    public void handleTranslationUnit()
    {
        if (diags.hasErrorOccurred())
        {
            m = null;
            return;
        }
        if (builder != null)
            builder.release();
    }

    @Override
    public void handleTagDeclDefinition(Decl.TagDecl tag)
    {
        builder.updateCompletedType(tag);
    }

    @Override
    public void completeTentativeDefinition(Decl.VarDecl d)
    {
        if (diags.hasErrorOccurred())
            return;

        builder.emitTentativeDefinition(d);
    }

    public static CodeGenerator createLLVMCodeGen(
            Diagnostic diags,
            String moduleName,
            CompileOptions compOpts
    )
    {
        return new CodeGeneratorImpl(diags, moduleName, compOpts);
    }
}
