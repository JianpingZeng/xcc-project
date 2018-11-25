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

package backend.codegen.dagisel;

import java.util.ArrayList;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public class ISelUpdater implements DAGUpdateListener {
  private int iselPos;
  private ArrayList<SDNode> allNodes;

  public ISelUpdater(int pos, ArrayList<SDNode> nodes) {
    iselPos = pos;
    allNodes = nodes;
  }

  @Override
  public void nodeDeleted(SDNode node, SDNode e) {
    if (allNodes.get(iselPos-1) == node)
      ++iselPos;
  }

  @Override
  public void nodeUpdated(SDNode node) {
  }

  public int getISelPos() {
    return iselPos;
  }
}
