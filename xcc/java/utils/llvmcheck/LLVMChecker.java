package utils.llvmcheck;
/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2018, Jianping Zeng.
 * All rights reserved.
 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import backend.support.AssemblerAnnotationWriter;
import backend.support.LLVMContext;
import backend.target.TargetSelect;
import backend.value.Function;
import backend.value.Module;
import backend.value.Value;
import tools.FormattedOutputStream;
import tools.OutRef;
import tools.SMDiagnostic;
import tools.commandline.*;

import static tools.commandline.Desc.desc;
import static tools.commandline.FormattingFlags.Positional;
import static tools.commandline.Initializer.init;
import static tools.commandline.ValueDesc.valueDesc;

/**
 * This class is used to check whether the LLReader and LLWriter works properly by
 * checking equality between original LLVM file and dumped LLVM file.
 * @author Jianping Zeng.
 * @version 0.4
 */
public class LLVMChecker {
  private static final StringOpt InputFilename =
      new StringOpt(new FormattingFlagsApplicator(Positional),
          desc("<input LLVM IR file>"),
          init("-"),
          valueDesc("filename"));

  private static final BooleanOpt ShowAnnotations =
      new BooleanOpt(
          OptionNameApplicator.optionName("show-annotations"),
          desc("Add informational comments to the .ll file"),
          init(false));

  private static class CommentWriter implements AssemblerAnnotationWriter {
    @Override
    public void emitFunctionAnnot(Function f, FormattedOutputStream os) {
      os.printf("; [#uses=%d]\n", f.getNumUses());
    }

    @Override
    public void printInfoComment(Value v, FormattedOutputStream os) {
      if (v.getType().isVoidType()) return;
      os.padToColumn(50);
      os.printf("; [#uses=%d]]", v.getNumUses());
    }
  }

  public static void main(String[] args) {
    // Initialize Target machine
    // Initialize Target machine
    TargetSelect.InitializeAllTargetInfo();
    TargetSelect.InitializeAllTarget();

    CL.parseCommandLineOptions(args, "The Compiler for LLVM IR");


    OutRef<SMDiagnostic> diag = new OutRef<>();
    Module theModule = backend.llReader.Parser.parseAssemblyFile(InputFilename.value, diag, LLVMContext.getGlobalContext());
    if (theModule == null) {
      diag.get().print("llc", System.err);
      System.exit(0);
    }

    AssemblerAnnotationWriter annotator = null;
    if (ShowAnnotations.value)
      annotator = new CommentWriter();
    theModule.print(new FormattedOutputStream(System.err), annotator);
    System.exit(0);
  }
}
