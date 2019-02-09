package cfe.codegen;

import backend.target.TargetData;
import backend.value.Module;
import cfe.diag.Diagnostic;
import cfe.sema.ASTContext;
import cfe.sema.Decl;
import cfe.support.CompileOptions;

import java.util.ArrayList;

/**
 * Extremely C Compiler. Jianping Zeng.
 */
public class CodeGeneratorImpl implements CodeGenerator {
  private Diagnostic diags;
  private CompileOptions compOpts;
  private Module m;
  private ASTContext ctx;
  private HIRModuleGenerator builder;
  private TargetData td;

  public CodeGeneratorImpl(Diagnostic diags, String moduleName, CompileOptions compOpts) {
    this.diags = diags;
    this.compOpts = compOpts;
    m = new Module(moduleName);
  }

  @Override
  public Module getModule() {
    return m;
  }

  @Override
  public void initialize(ASTContext context) {
    ctx = context;
    m.setTargetTriple(ctx.target.getTargetTriple());
    m.setDataLayout(ctx.target.getTargetDescription());
    td = new TargetData(ctx.target.getTargetDescription());
    builder = new HIRModuleGenerator(context, compOpts, m, td, diags);
  }

  @Override
  public void handleTopLevelDecls(ArrayList<Decl> decls) {
    // Make sure to emit all elements of a Decl.
    decls.forEach(builder::emitTopLevelDecl);
  }

  @Override
  public void handleTranslationUnit(ASTContext context) {
    if (diags.hasErrorOccurred()) {
      m = null;
      return;
    }
    if (builder != null)
      builder.release();
  }

  @Override
  public void handleTagDeclDefinition(Decl.TagDecl tag) {
    builder.updateCompletedType(tag);
  }

  @Override
  public void completeTentativeDefinition(Decl.VarDecl d) {
    if (diags.hasErrorOccurred())
      return;

    builder.emitTentativeDefinition(d);
  }

  public static CodeGenerator createLLVMCodeGen(
      Diagnostic diags,
      String moduleName,
      CompileOptions compOpts
  ) {
    return new CodeGeneratorImpl(diags, moduleName, compOpts);
  }
}
