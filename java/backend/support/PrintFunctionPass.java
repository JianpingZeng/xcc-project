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
import backend.pass.FunctionPass;
import backend.pass.Pass;
import backend.value.Function;

import java.io.PrintStream;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public class PrintFunctionPass implements FunctionPass {
  private AnalysisResolver resolver;
  private PrintStream os;
  private String banner;

  public PrintFunctionPass(PrintStream os, String banner) {
    this.os = os;
    this.banner = banner;
  }

  @Override
  public void getAnalysisUsage(AnalysisUsage au) {
    au.setPreservedAll();
  }

  @Override
  public boolean runOnFunction(Function f) {
    if (BackendCmdOptions.isFunctionInPrintList(f.getName())) {
      os.print(banner);
      f.print(os);
    }
    return false;
  }

  @Override
  public String getPassName() {
    return "Print Function Pass";
  }

  @Override
  public AnalysisResolver getAnalysisResolver() {
    return resolver;
  }

  @Override
  public void setAnalysisResolver(AnalysisResolver resolver) {
    this.resolver = resolver;
  }

  public static Pass createPrintFunctionPass(PrintStream os, String banner) {
    return new PrintFunctionPass(os, banner);
  }
}
