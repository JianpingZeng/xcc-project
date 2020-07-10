package utils.tablegen;
/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2020, Jianping Zeng
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

import tools.Error;
import tools.Util;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public final class CodeGenRegister {
  Record theDef;
  private boolean subRegsComplete;
  private HashMap<Record, CodeGenRegister> subRegs;
  private ArrayList<CodeGenRegister> superRegs;

  CodeGenRegister(Record rec) {
    theDef = rec;
    subRegs = new HashMap<>();
    subRegsComplete = false;
    superRegs = new ArrayList<>();
  }

  private void initializeSubRegs() {
    if (subRegsComplete) return;
    subRegsComplete = true;

    ArrayList<Record> subList = theDef.getValueAsListOfDefs("SubRegs");
    ArrayList<Record> indices = theDef.getValueAsListOfDefs("SubRegIndices");
    if (subList.size() != indices.size()) {
      Error.printError(theDef.getLoc(), String.format("Register %s SubRegIndices doesn't match SubRegs", theDef.getName()));
      return;
    }

    for (int i = 0, e = subList.size(); i < e; ++i) {
      Record sub = subList.get(i);
      CodeGenRegister subReg = CodeGenRegBank.getReg(sub);
      if (subRegs.containsKey(indices.get(i))) {
        Error.printError(theDef.getLoc(), String.format("SubRegIndex %s appears twice in Register %s", indices.get(i).getName(), getName()));
        return;
      }
      subRegs.put(indices.get(i), subReg);
    }

    for (int i = 0, e = subList.size(); i < e; ++i) {
      Record sub = subList.get(i);
      CodeGenRegister sr = CodeGenRegBank.getReg(sub);
      HashMap<Record, CodeGenRegister> map = sr.getSubRegs();

      sr.superRegs.add(this);
      map.forEach((key, value) -> {
        if (!value.equals(sr))
          value.superRegs.add(this);
      });
    }
  }

  String getName() {
    return theDef.getName();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (this == obj) return true;
    if (getClass() != obj.getClass()) return false;
    CodeGenRegister r = (CodeGenRegister) obj;
    return getName().equals(r.getName());
  }

  HashMap<Record, CodeGenRegister> getSubRegs() {
    if (!subRegsComplete)
      initializeSubRegs();

    Util.assertion(subRegsComplete);
    return subRegs;
  }
}
