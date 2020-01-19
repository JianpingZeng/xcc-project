package backend.codegen;
/*
 * Extremely C language Compiler
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

import backend.debug.DebugLoc;
import backend.pass.AnalysisResolver;
import backend.pass.ImmutablePass;
import backend.value.MDNode;
import tools.Pair;

import java.util.ArrayList;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class MachineModuleInfo implements ImmutablePass {
  private AnalysisResolver resolver;
  private int nextLabelID;
  private boolean dgbInfoAvailable;
  private ArrayList<Pair<MDNode, Pair<Integer, DebugLoc>>> variableDbgInfo;

  @Override
  public void setAnalysisResolver(AnalysisResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public AnalysisResolver getAnalysisResolver() {
    return resolver;
  }

  @Override
  public String getPassName() {
    return "Machine Module Info Pass";
  }

  @Override
  public void initializePass() {

  }

  public int nextLabelID() {
    return 0;
  }

  public int getNextLabelID() {
    return nextLabelID;
  }

  public void addInvoke(MachineBasicBlock landingPad, int beginLabel,
                        int endLabel) {

  }

  public boolean hasDebugInfo() {
    return dgbInfoAvailable;
  }

  public void setDgbInfoAvailable(boolean ability) {
    dgbInfoAvailable = ability;
  }

  public void setVariableDgbInfo(MDNode node, int slot, DebugLoc dl) {
    variableDbgInfo.add(Pair.get(node, Pair.get(slot, dl)));
  }

  public ArrayList<Pair<MDNode, Pair<Integer, DebugLoc>>> getVariableDbgInfo() {
    return variableDbgInfo;
  }
}
