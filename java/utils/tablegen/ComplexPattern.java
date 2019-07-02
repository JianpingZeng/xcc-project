package utils.tablegen;
/*
 * Extremely C language Compiler
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

import tools.Error;

import java.util.ArrayList;

import static utils.tablegen.ComplexPattern.CPAttr.CPAttrParentAsRoot;
import static utils.tablegen.SDNP.*;

/**
 * ComplexPattern info, corresponding to the ComplexPattern
 * tablegen class in TargetSelectionDAG.td
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public final class ComplexPattern {
  /**
   * ComplexPattern attributes.
   */
  public interface CPAttr {
    int CPAttrParentAsRoot = 0;
  }

  private int ty;
  private int numOperands;
  private String selectFunc;
  private ArrayList<Record> rootNodes;
  /**
   * Node properties.
   */
  private int properties;
  /**
   * Pattern attributes.
   */
  private int attributes;

  public ComplexPattern() {
    super();
  }

  public ComplexPattern(Record r) {
    ty = CodeGenTarget.getValueType(r.getValueAsDef("Ty"));
    numOperands = (int) r.getValueAsInt("NumOperands");
    selectFunc = r.getValueAsString("SelectFunc");
    rootNodes = r.getValueAsListOfDefs("RootNodes");

    // Parse the properties.
    ArrayList<Record> propList = r.getValueAsListOfDefs("Properties");
    for (Record prop : propList) {
      switch (prop.getName()) {
        case "SDNPHasChain":
          properties |= 1 << SDNPHasChain;
          break;
        case "SDNPMayStore":
          properties |= 1 << SDNPMayStore;
          break;
        case "SDNPMayLoad":
          properties |= 1 << SDNPMayLoad;
          break;
        case "SDNPSideEffect":
          properties |= 1 << SDNPSideEffect;
          break;
        case "SDNPMemOperand":
          properties |= 1 << SDNPMemOperand;
          break;
        default: {
          Error.printFatalError(r.getLoc(),
              String.format("Unsupported SD Node property '%s' " +
                  "on ComplexPattern '%s'!\n", prop.getName(), r.getName()));
        }
      }
    }

    // Parse the attributes.
    attributes = 0;
    propList = r.getValueAsListOfDefs("Attributes");
    for (Record attr : propList) {
      if (attr.getName().equals("CPAttrParentAsRoot"))
        attributes |= 1 << CPAttrParentAsRoot;
      else {
        Error.printFatalError(String.format("Unsupported pattern attribute '%s' " +
            "on ComplexPattern '%s'!\n", attr.getName(), r.getName()));
      }
    }
  }

  public int getValueType() {
    return ty;
  }

  public int getNumOperands() {
    return numOperands;
  }

  public String getSelectFunc() {
    return selectFunc;
  }

  public ArrayList<Record> getRootNodes() {
    return rootNodes;
  }

  public boolean hasProperty(int prop) {
    return (properties & (1 << prop)) != 0;
  }

  public boolean hasAttribute(int attr) {
    return (attributes & (1 << attr)) != 0;
  }
}
