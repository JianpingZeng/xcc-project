package cfe.codegen;
/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2020, Jianping Zeng.
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

import backend.codegen.MachineCodeEmitter;
import backend.codegen.RegAllocLocal;
import backend.codegen.RegisterRegAlloc;
import backend.codegen.dagisel.RegisterScheduler;
import backend.codegen.dagisel.ScheduleDAGFast;
import backend.pass.Pass;
import backend.pass.PassCreator;
import backend.passManaging.FunctionPassManager;
import backend.passManaging.PassManager;
import backend.support.LLVMContext;
import backend.support.PrintModulePass;
import backend.target.SubtargetFeatures;
import backend.target.Target;
import backend.target.TargetData;
import backend.target.TargetMachine;
import backend.target.TargetMachine.CodeGenFileType;
import backend.target.TargetMachine.CodeGenOpt;
import backend.value.BasicBlock;
import backend.value.Module;
import cfe.ast.ASTConsumer;
import cfe.diag.Diagnostic;
import cfe.sema.ASTContext;
import cfe.sema.Decl;
import cfe.support.BackendAction;
import cfe.support.CompileOptions;
import cfe.support.LangOptions;
import tools.OutRef;
import tools.Util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.function.Function;

import static backend.support.ErrorHandling.reportFatalError;
import static backend.target.TargetMachine.CodeGenFileType.CGFT_AssemblyFile;
import static backend.target.TargetMachine.CodeGenFileType.CGFT_ObjectFile;
import static backend.target.TargetMachine.CodeGenOpt.*;
import static cfe.support.BackendAction.Backend_EmitAssembly;
import static cfe.support.BackendAction.Backend_EmitNothing;

/**
 * <p>
 * This class is responsible for transforming external declaration in a
 * translation unit, like global variable declaration and function definition
 * , into implementation corresponding to Higher Level Representation.
 * </p>
 * <p>
 * For function definition, each statement contained in it would be lowered into
 * HIR instruction contained in {@linkplain BasicBlock}.
 * For the same methodology, global variable declaration would be viewed as
 * Constant value in HIR.
 * </p>
 * <p>
 * As the consequence, a {@linkplain Module} would be obtained which holds many
 * element, for example, Functions in HIR perspective, global Constants.
 * </p>
 *
 * @author Jianping Zeng
 * @version 0.4
 * @see ASTConsumer
 * @see ASTContext
 */
public class BackendConsumer implements ASTConsumer {
  private CompileOptions compileOptions;
  private BackendAction action;
  private Diagnostic diags;
  private LangOptions langOpts;
  private Module theModule;
  private CodeGenerator gen;
  private PrintStream asmOutStream;
  private TargetData theTargetData;
  //private TargetMachine tm;
  private ASTContext context;

  private FunctionPassManager perFunctionPasses;
  private PassManager perModulePasses;
  private FunctionPassManager perCodeGenPasses;

  public BackendConsumer(
      BackendAction act,
      Diagnostic diags,
      LangOptions langOpts,
      CompileOptions opts,
      String moduleName,
      PrintStream os,
      Function<Module, TargetMachine> targetMachineAllocator,
      LLVMContext ctx) {
    action = act;
    this.diags = diags;
    this.langOpts = langOpts;
    compileOptions = opts;
    gen = CodeGeneratorImpl.createLLVMCodeGen(diags, moduleName, compileOptions, ctx);
    asmOutStream = os;
    //tm = targetMachineAllocator.apply(theModule);
  }

  public static ASTConsumer createBackendConsumer(
      BackendAction act,
      Diagnostic diags,
      LangOptions langOpts,
      CompileOptions compOpts,
      String moduleID,
      PrintStream os,
      Function<Module, TargetMachine> targetMachine,
      LLVMContext ctx) {
    return new BackendConsumer(act, diags, langOpts, compOpts, moduleID, os, targetMachine, ctx);
  }

  /**
   * This method is invoked for initializing this ASTConsumer.
   */
  @Override
  public void initialize(ASTContext ctx) {
    context = ctx;

    gen.initialize(ctx);

    theModule = gen.getModule();
    theTargetData = new TargetData(ctx.target.getTargetDescription());
  }

  /**
   * Handle the specified top level declaration.
   * This method is called by {@linkplain Compiler} to process every top-level
   * decl.
   * <b>Note that</b> decls is a list that chained multiple Declaration, like
   * <code>'int a, b'</code>, there are two declarator chained.
   *
   * @param decls
   */
  @Override
  public void handleTopLevelDecls(ArrayList<Decl> decls) {
    // Make sure that emits all elements for each decl.
    gen.handleTopLevelDecls(decls);
  }

  /**
   * This method is called when the parsing file for entire translation unit
   * was parsed.
   */
  @Override
  public void handleTranslationUnit(ASTContext context) {
    gen.handleTranslationUnit(context);

    // Emits assembly code or ir code for backend.target.
    emitAssembly();

    // force to close and flush output stream.
    if (asmOutStream != null) {
      asmOutStream.close();
    }
  }

  @Override
  public void handleTagDeclDefinition(Decl.TagDecl tag) {
    gen.handleTagDeclDefinition(tag);
  }

  @Override
  public void completeTentativeDefinition(Decl.VarDecl d) {
    gen.completeTentativeDefinition(d);
  }

  /**
   * Handle to interactive with backend to generate actual machine code
   * or assembly code.
   */
  private void emitAssembly() {
    // Silently ignore generating code, if backend.target data or module is null.
    if (theTargetData == null || theModule == null)
      return;

    Util.assertion(theModule == gen.getModule(),
        "Unexpected module change when IR generation");
    // creates some necessary pass for code generation and optimization.
    createPass();
    OutRef<String> error = new OutRef<>("");
    if (!addEmitPasses(error)) {
      reportFatalError("UNKNOWN: " + error.get());
    }

    // Run passes. For now we do all passes at once.
    if (perFunctionPasses != null) {
      for (backend.value.Function f : theModule.getFunctionList())
        if (!f.isDeclaration())
          perFunctionPasses.run(f);
    }

    if (perModulePasses != null) {
      perModulePasses.run(theModule);
    }

    // performing a serial of code gen procedures, like instruction selection,
    // register allocation, and instruction scheduling etc.
    if (perCodeGenPasses != null) {
      // Performs initialization works before operating on Function.
      perCodeGenPasses.doInitialization();
      for (backend.value.Function f : theModule.getFunctionList())
        if (!f.isDeclaration())
          perCodeGenPasses.run(f);

      // Finalize!
      perCodeGenPasses.doFinalization();
    }
  }

  private FunctionPassManager getPerFunctionPasses() {
    if (perFunctionPasses == null) {
      perFunctionPasses = new FunctionPassManager(theModule);
      perFunctionPasses.add(new TargetData(theTargetData));
    }
    return perFunctionPasses;
  }

  private PassManager getPerModulePasses() {
    if (perModulePasses == null) {
      perModulePasses = new PassManager();
      perModulePasses.add(new TargetData(theTargetData));
    }
    return perModulePasses;
  }

  private FunctionPassManager getCodeGenPasses() {
    if (perCodeGenPasses == null) {
      perCodeGenPasses = new FunctionPassManager(theModule);
      perCodeGenPasses.add(new TargetData(theTargetData));
    }
    return perCodeGenPasses;
  }

  private void createPass() {
    // The optimization is not needed if optimization level is -O0.
    if (compileOptions.optimizationLevel > 0)
      PassCreator.createStandardFunctionPasses(getPerFunctionPasses(),
          compileOptions.optimizationLevel);

    Pass inliningPass = null;
    switch (compileOptions.inlining) {
      default:
      case NoInlining:
        break;
      case NormalInlining: {
        inliningPass = PassCreator.createAlwaysInliningPass();
        break;
      }
      case OnlyAlwaysInlining: {
        // inline small function.
        int threshold = (compileOptions.optimizeSize ||
            compileOptions.optimizationLevel < 3) ? 50 : 200;
        inliningPass = PassCreator.createFunctionInliningPass(threshold);
        break;
      }
    }

    // creates a module pass manager.
    PassManager pm = getPerModulePasses();
    PassCreator.createStandardModulePasses(pm,
        compileOptions.optimizationLevel,
        compileOptions.optimizeSize,
        compileOptions.unrollLoops,
        inliningPass);
  }

  private boolean addEmitPasses(OutRef<String> error) {
    if (action == Backend_EmitNothing)
      return true;

    switch (action) {
      case Backend_EmitAssembly:
      case Backend_EmitObj: {
        boolean fast = compileOptions.optimizationLevel < 1;
        FunctionPassManager pm = getCodeGenPasses();

        CodeGenOpt optLevel = Default;
        switch (compileOptions.optimizationLevel) {
          default:
            break;
          case 0:
            optLevel = None;
            break;
          case 3:
            optLevel = Aggressive;
            break;
        }

        String triple = theModule.getTargetTriple();
        Target theTarget = Target.TargetRegistry.lookupTarget(triple, error);
        if (theTarget == null) {
          error.set("Unable to get target machine: " + error.get());
          return false;
        }

        String featureStr = "";
        if (!compileOptions.CPU.isEmpty() || !compileOptions.features.isEmpty()) {
          SubtargetFeatures features = new SubtargetFeatures();
          features.setCPU(compileOptions.CPU);
          for (String str : compileOptions.features) {
            features.addFeature(str);
          }
          featureStr = features.getString();
        }

        TargetMachine tm = theTarget.createTargetMachine(triple, compileOptions.CPU, featureStr,
            TargetMachine.RelocModel.Default, TargetMachine.CodeModel.Default);
        tm.setAsmVerbosityDefault(true);

        // Set the default Register Allocator.
        RegisterRegAlloc.setDefault(RegAllocLocal::createLocalRegAllocator);

        // Set the default instruction scheduler.
        RegisterScheduler.setDefault(ScheduleDAGFast::createFastDAGScheduler);

        MachineCodeEmitter mce = null;
        CodeGenFileType cft = action == Backend_EmitAssembly ? CGFT_AssemblyFile : CGFT_ObjectFile;
        if (tm.addPassesToEmitFile(pm, asmOutStream, cft, optLevel)) {
          reportFatalError("Unsupport to generate this kind of file!");
        }
        return true;
      }
      case Backend_EmitLL: {
        getPerModulePasses().add(new PrintModulePass(asmOutStream));
        return true;
      }
      default: {
        error.set("Unsupported to emit object file yet!");
        return false;
      }
    }
  }

  public Module getModule() {
    return theModule;
  }
}
