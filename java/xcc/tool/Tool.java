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

package xcc.tool;

import xcc.*;

import java.util.ArrayList;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public abstract class Tool {
  /**
   * The tool name.
   */
  private String name;

  /**
   * The human readable name for this tool.
   */
  private String shortName;

  private ToolChain tc;

  public Tool(String name, String shortName, ToolChain tc) {
    this.name = name;
    this.shortName = shortName;
    this.tc = tc;
  }

  public String getName() {
    return name;
  }

  public String getShortName() {
    return shortName;
  }

  public ToolChain getToolChain() {
    return tc;
  }

  /**
   * Constructs a Command to be performed specified action, like compile,
   * assemble, or link with some inputs and produce an output.
   *
   * @param c
   * @param ja
   * @param output
   * @param inputs
   * @param args
   * @param linkerOutput
   * @return Return a command.
   */
  public abstract Job constructJob(
      Compilation c, Action.JobAction ja,
      InputInfo output,
      ArrayList<InputInfo> inputs, ArgList args,
      String linkerOutput);
}
