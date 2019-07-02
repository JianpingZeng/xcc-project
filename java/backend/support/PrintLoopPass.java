/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
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

import backend.pass.*;
import backend.value.BasicBlock;
import backend.value.Loop;

import java.io.PrintStream;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class PrintLoopPass implements LoopPass {
  private AnalysisResolver resolver;
  private PrintStream os;
  private String banner;

  public PrintLoopPass(PrintStream os, String banner) {
    this.os = os;
    this.banner = banner;
  }

  @Override
  public void getAnalysisUsage(AnalysisUsage au) {
    au.setPreservedAll();
  }

  @Override
  public boolean runOnLoop(Loop loop, LPPassManager ppm) {
    if (loop.isEmpty())
      return false;

    for (BasicBlock bb : loop.blocks) {
      if (bb != null)
        bb.print(os);
      else
        os.print("Printing <null> block");
    }
    return false;
  }

  public String getPassName() {
    return "Print Loop Pass";
  }

  @Override
  public AnalysisResolver getAnalysisResolver() {
    return resolver;
  }

  @Override
  public void setAnalysisResolver(AnalysisResolver resolver) {
    this.resolver = resolver;
  }

  public static Pass createPrintLoopPass(PrintStream os, String banner) {
    return new PrintLoopPass(os, banner);
  }
}
