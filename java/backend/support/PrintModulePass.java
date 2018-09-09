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

package backend.support;

import backend.pass.AnalysisResolver;
import backend.pass.AnalysisUsage;
import backend.pass.ModulePass;
import backend.value.Module;
import tools.FormattedOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public class PrintModulePass implements ModulePass {
  private FormattedOutputStream os;

  private AnalysisResolver resolver;

  @Override
  public void getAnalysisUsage(AnalysisUsage au) {
    au.setPreservedAll();
  }

  @Override
  public void setAnalysisResolver(AnalysisResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public AnalysisResolver getAnalysisResolver() {
    return resolver;
  }

  public PrintModulePass(OutputStream out) {
    super();
    os = new FormattedOutputStream(out);
  }

  @Override
  public boolean runOnModule(Module m) {
    try {
      m.print(os);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public String getPassName() {
    return "Print module into text file";
  }

  public static PrintModulePass createPrintModulePass(PrintStream os) {
    return new PrintModulePass(os);
  }
}
