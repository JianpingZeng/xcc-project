package utils.tablegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng
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
import utils.tablegen.Init.DefInit;

import java.util.ArrayList;
import java.util.HashSet;

import static utils.tablegen.CodeGenHwModes.DefaultMode;
import static utils.tablegen.ValueTypeByHwMode.getValueTypeByHwMode;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public final class CodeGenRegisterClass {
  Record theDef;
  ArrayList<CodeGenRegister> members;
  ArrayList<ValueTypeByHwMode> vts;
  RegSizeInfoByHwMode regInfos;
  String methodBodies;
  long copyCost;
  ArrayList<Record> subRegClasses;

  HashSet<CodeGenRegisterClass> subClasses;
  HashSet<CodeGenRegisterClass> superClasses;

  public String getName() {
    return theDef.getName();
  }

  public ArrayList<ValueTypeByHwMode> getValueTypes() {
    return vts;
  }

  public int getNumValueTypes() {
    return vts.size();
  }

  public ValueTypeByHwMode getValueTypeAt(int idx) {
    Util.assertion(idx >= 0 && idx < vts.size());
    return vts.get(idx);
  }

  private static int anonCnter = 0;

  public CodeGenRegisterClass(Record r, CodeGenHwModes cgh) {
    members = new ArrayList<>();
    vts = new ArrayList<>();
    subRegClasses = new ArrayList<>();
    theDef = r;
    subClasses = new HashSet<>();
    superClasses = new HashSet<>();

    // Rename the anonymous register class.
    if (r.getName().length() > 9 && r.getName().charAt(9) == '.')
      r.setName("AnonRegClass_" + (anonCnter++));

    ArrayList<Record> typeList = r.getValueAsListOfDefs("RegTypes");
    for (Record ty : typeList) {
      if (!ty.isSubClassOf("ValueType"))
        Error.printFatalError("RegTypes list member '" + ty.getName()
            + "' does not derive from the ValueType class!");
      vts.add(getValueTypeByHwMode(ty, cgh));
    }

    Util.assertion(!vts.isEmpty());

    ArrayList<Record> regList = r.getValueAsListOfDefs("MemberList");
    for (Record reg : regList) {
      if (!reg.isSubClassOf("Register"))
        Error.printFatalError("Register Class member '" + reg.getName() +
            "' does not derive from the Register class!");
      members.add(new CodeGenRegister(reg));
    }

    // Obtains the information about SubRegisterClassList.
    ArrayList<Record> subRegClassList = r.getValueAsListOfDefs("SubRegClassList");

    for (Record subReg : subRegClassList) {
      if (!subReg.isSubClassOf("RegisterClass"))
        Error.printFatalError("Register class member '" + subReg.getName()
            + "' doest not derive from the RegisterClass class!");

      subRegClasses.add(subReg);
    }

    // Allow targets to override the size and alignment in bits of
    // the RegisterClass.
    RecordVal regInfoRec = r.getValue("RegInfos");
    regInfos = new RegSizeInfoByHwMode();

    if (regInfoRec != null && regInfoRec.getValue() instanceof DefInit)
      regInfos = new RegSizeInfoByHwMode(((DefInit) regInfoRec.getValue()).getDef(), cgh);

    long size = r.getValueAsInt("Size");
    Util.assertion(size != 0 || (regInfos != null &&
            regInfos.hasDefault()) || vts.get(0).isSimple(),
        "Impossible to determine the register size");

    // add a register info by default mode.
    if (!regInfos.hasDefault()) {
      RegSizeInfo ri = new RegSizeInfo();
      ri.regSize = ri.spillSize = size != 0 ? size :
          vts.get(0).getSimple().getSizeInBits();
      ri.spillAlignment = r.getValueAsInt("Alignment");
      regInfos.map.put(DefaultMode, ri);
    }
    copyCost = r.getValueAsInt("CopyCost");
    methodBodies = r.getValueAsCode("MethodBodies");
  }

  public boolean contains(Record r) {
    return members.contains(new CodeGenRegister(r));
  }

  public boolean hasSubClass(CodeGenRegisterClass rc) {
    return subClasses.contains(rc);
  }

  public boolean hasSupClass(CodeGenRegisterClass rc) {
    return superClasses.contains(rc);
  }
}
