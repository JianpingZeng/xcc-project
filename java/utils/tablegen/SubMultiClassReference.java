package utils.tablegen;
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

import tools.SourceMgr;

import java.util.ArrayList;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public final class SubMultiClassReference {
  public SourceMgr.SMLoc refLoc;
  public MultiClass mc;
  public ArrayList<Init> templateArgs;

  public SubMultiClassReference() {
    templateArgs = new ArrayList<>();
  }

  public boolean isInvalid() {
    return mc == null;
  }

  public void dump() {
    System.err.println("Multiclass:");

    mc.dump();
    System.err.println("Template args:");
    templateArgs.forEach(Init::dump);
  }
}
