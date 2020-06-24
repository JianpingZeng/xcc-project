/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2017, Jianping Zeng.
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

package driver.tool.gcc;

import backend.support.Triple;
import driver.*;
import driver.tool.Tool;

import java.util.ArrayList;

import static driver.OptionID.OPT__Wa_COMMA;
import static driver.OptionID.OPT__Xassembler;

public class GCCAssembler extends Tool {
  public GCCAssembler(ToolChain tc) {
    super("linux::Assemble", "assemble", tc);
  }

  @Override
  public Job constructJob(Compilation c, Action.JobAction ja,
                          InputInfo output, ArrayList<InputInfo> inputs, ArgList args,
                          String linkerOutput) {
    ArrayList<String> cmdArgs = new ArrayList<>();
    LinuxToolChain tc = (LinuxToolChain) getToolChain();

    if (getToolChain().getArch() == Triple.ArchType.x86) {
      cmdArgs.add("--32");
    } else if (getToolChain().getArch() == Triple.ArchType.x86_64) {
      cmdArgs.add("--64");
    }

    args.addAllArgValues(cmdArgs, OPT__Wa_COMMA, OPT__Xassembler);
    cmdArgs.add("-o");
    cmdArgs.add(output.getFilename());

    for (InputInfo ii : inputs) {
      cmdArgs.add(ii.getFilename());
    }

    return new Job.Command(ja, tc.getAssembler(), cmdArgs);
  }
}
