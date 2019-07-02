package backend.support;
/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2019, Jianping Zeng.
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

import backend.analysis.CallGraphNode;
import backend.pass.AnalysisUsage;
import backend.pass.CallGraphSCCPass;
import backend.value.Function;

import java.io.PrintStream;
import java.util.ArrayList;

import static backend.support.BackendCmdOptions.isFunctionInPrintList;

public class PrintCallGraphPass extends CallGraphSCCPass {
  private PrintStream out;
  private String ban;

  public PrintCallGraphPass(String banner, PrintStream os) {
    out = os;
    ban = banner;
  }

  @Override
  public void getAnalysisUsage(AnalysisUsage au) {
    au.setPreservedAll();
    super.getAnalysisUsage(au);
  }

  @Override
  public boolean runOnSCC(ArrayList<CallGraphNode> nodes) {
    out.print(ban);
    for (CallGraphNode n : nodes) {
      Function f = n.getFunction();
      if (f != null) {
        if (isFunctionInPrintList(f.getName()))
          f.print(out);
      } else
        out.println("\nPrint <null> Function");
    }
    return false;
  }

  @Override
  public String getPassName() {
    return "Printing Call Graph Pass";
  }
}
