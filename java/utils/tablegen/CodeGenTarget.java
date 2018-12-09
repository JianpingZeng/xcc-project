package utils.tablegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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
import java.util.HashSet;
import java.util.TreeMap;
import java.util.TreeSet;

import static backend.codegen.MVT.Other;

/**
 * This class corresponds to the Target class in .td file.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public final class CodeGenTarget {
  private Record targetRec;
  private ArrayList<Record> calleeSavedRegisters;
  private int pointerType;
  private TreeMap<String, CodeGenInstruction> insts;
  private ArrayList<CodeGenRegister> registers;
  private ArrayList<CodeGenRegisterClass> registerClasses;
  private ArrayList<ValueTypeByHwMode> legalValueTypes;
  private CodeGenHwModes hwModes;
  private RecordKeeper keeper;

  public CodeGenTarget(RecordKeeper keeper) {
    pointerType = Other;
    legalValueTypes = new ArrayList<>();
    this.keeper = keeper;
    hwModes = new CodeGenHwModes(keeper);

    ArrayList<Record> targets = Record.records.getAllDerivedDefinition("Target");
    if (targets.isEmpty())
      Error.printFatalError("Error: No target defined!");
    if (targets.size() != 1)
      Error.printFatalError("Error: Multiple subclasses of Target defined!");

    targetRec = targets.get(0);

    // LLVM 1.0 introduced which is removed in LLVM 2.6.
    // calleeSavedRegisters = targetRec.getValueAsListOfDefs("CalleeSavedRegisters");
    // pointerType = getValueType(targetRec.getValueAsDef("PointerType"));

    readRegisters();

    // Read register classes description information from records.
    readRegisterClasses();
  }

  public CodeGenHwModes getHwModes() {
    return hwModes;
  }

  private void readRegisters() {
    ArrayList<Record> regs = Record.records.getAllDerivedDefinition("Register");
    regs.sort((r1, r2) -> {
      return r1.getName().compareTo(r2.getName());
    });

    if (regs.isEmpty())
      Error.printFatalError("No 'Register' subclasses defined!");
    registers = new ArrayList<>();
    regs.forEach(reg ->
    {
      try {
        registers.add(new CodeGenRegister(reg));
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  /**
   * rc1 is a sub-class of rc2 if it is a valid replacement for any
   * instruction operand where an rc2 register is required. It must satisfy
   * these conditions:
   * 1. All RC2 registers are also in RC.
   * 2. The RC2 spill size must not be smaller that the RC spill size.
   * 3.RC2 spill alignment must be compatible with RC.
   * <p>
   * Sub-classes are used to determine if a virtual register can be used
   * as an instruction operand, or if it must be copied first.
   *
   * @param rc1
   * @param rc2
   * @return
   */
  private boolean isSubRegClass(CodeGenRegisterClass rc1, CodeGenRegisterClass rc2) {
    return rc1.regInfos.isSubClassOf(rc2.regInfos) &&
        rc2.members.containsAll(rc1.members);
  }

  /**
   * For each register class, computing sub class for it.
   */
  private void computeSubClasses() {
    for (int i = 0, e = registerClasses.size(); i < e; i++) {
      CodeGenRegisterClass rc = registerClasses.get(i);
      // every reg class is the sub class of itself.
      rc.subClasses.add(rc);
      for (int j = i + 1; j < e; j++) {
        CodeGenRegisterClass rc2 = registerClasses.get(j);
        if (rc.subClasses.contains(rc2))
          continue;
        if (isSubRegClass(rc2, rc))
          rc.subClasses.add(rc2);
      }
    }
    for (CodeGenRegisterClass rc : registerClasses) {
      for (CodeGenRegisterClass subClass : rc.subClasses)
        subClass.superClasses.add(rc);
    }
  }

  private void readRegisterClasses() {
    ArrayList<Record> regClasses = Record.records.getAllDerivedDefinition("RegisterClass");
    if (regClasses.isEmpty())
      Error.printFatalError("No 'RegisterClass subclass defined!");
    registerClasses = new ArrayList<>();
    regClasses.forEach(regKls ->
        registerClasses.add(new CodeGenRegisterClass(regKls, getHwModes())));
    computeSubClasses();
  }

  private void readInstructions() {
    ArrayList<Record> instrs = Record.records.getAllDerivedDefinition("Instruction");
    if (instrs.size() <= 2)
      Error.printFatalError("No 'Instruction' subclasses defined!");

    String instFormatName = getAsmWriter().getValueAsString("InstFormatName");
    insts = new TreeMap<>();
    for (Record inst : instrs) {
      String asmStr = inst.getValueAsString(instFormatName);
      insts.put(inst.getName(), new CodeGenInstruction(inst, asmStr));
    }
  }

  public static int AsmWriterNum = 0;

  public Record getAsmWriter() {
    ArrayList<Record> li = targetRec.getValueAsListOfDefs("AssemblyWriters");
    if (AsmWriterNum >= li.size())
      Error.printFatalError("Target does not have an AsmWriter #" + AsmWriterNum + "!");
    return li.get(AsmWriterNum);
  }

  public Record getTargetRecord() {
    return targetRec;
  }

  public String getName() {
    return targetRec.getName();
  }

  public ArrayList<Record> getCalleeSavedRegisters() {
    return calleeSavedRegisters;
  }

  public int getPointerType() {
    return pointerType;
  }

  Record getInstructionSet() {
    return targetRec.getValueAsDef("InstructionSet");
  }

  public ArrayList<CodeGenRegisterClass> getRegisterClasses() {
    return registerClasses;
  }

  public ArrayList<CodeGenRegister> getRegisters() {
    return registers;
  }

  public TreeMap<String, CodeGenInstruction> getInstructions() {
    if (insts == null || insts.isEmpty())
      readInstructions();
    return insts;
  }

  /**
   * Return all of the insts defined by the target, ordered by their
   * enum value.
   *
   * @param numberedInstructions
   */
  public void getInstructionsByEnumValue(
      ArrayList<CodeGenInstruction> numberedInstructions) {
    String[] firstPriority = {
        "PHI",
        "INLINEASM",
        "DBG_LABEL",
        "EH_LABEL",
        "GC_LABEL",
        "DECLARE",
        "EXTRACT_SUBREG",
        "INSERT_SUBREG",
        "IMPLICIT_DEF",
        "SUBREG_TO_REG",
        "COPY_TO_REGCLASS"
    };
    TreeSet<String> names = new TreeSet<>();
    for (String instr : firstPriority) {
      if (!insts.containsKey(instr))
        Error.printFatalError(String.format("Could not find '%s' instruction", instr));
      numberedInstructions.add(insts.get(instr));
      names.add(instr);
    }

    // Print out the rest of the insts set.
    insts.entrySet().forEach(entry ->
    {
      CodeGenInstruction inst = entry.getValue();
      if (!names.contains(entry.getKey()))
        numberedInstructions.add(inst);
    });
  }

  public CodeGenInstruction getInstruction(String name) {
    insts = getInstructions();
    Util.assertion(insts.containsKey(name), "Not an instruction!");
    return insts.get(name);
  }

  /**
   * Return the MVT::SimpleValueType that the specified TableGen
   * record corresponds to.
   *
   * @param rec
   * @return
   * @throws Exception
   */
  public static int getValueType(Record rec) {
    return (int) rec.getValueAsInt("Value");
  }

  public void readLegalValueTypes() {
    ArrayList<CodeGenRegisterClass> rcs = getRegisterClasses();
    for (CodeGenRegisterClass rc : rcs) {
      for (int i = 0, e = rc.vts.size(); i != e; i++)
        legalValueTypes.add(rc.vts.get(i));
    }

    // Remove duplicates.
    HashSet<ValueTypeByHwMode> set = new HashSet<>();
    for (int i = 0; i != legalValueTypes.size(); i++)
      set.add(legalValueTypes.get(i));

    legalValueTypes.clear();
    legalValueTypes.addAll(set);
  }

  public ArrayList<ValueTypeByHwMode> getLegalValueTypes() {
    if (legalValueTypes.isEmpty()) readLegalValueTypes();

    return legalValueTypes;
  }

  public CodeGenRegisterClass getRegisterClass(Record r) {
    for (CodeGenRegisterClass rc : registerClasses)
      if (rc.theDef.equals(r))
        return rc;

    Util.assertion(false, "Didn't find the register class!");
    return null;
  }

  /**
   * Find the register class that contains the
   * specified physical register.  If the register is not in a register
   * class, return null. If the register is in multiple classes, and the
   * classes have a superset-subset relationship and the same set of
   * types, return the superclass.  Otherwise return null.
   *
   * @param r
   * @return
   */
  public CodeGenRegisterClass getRegisterClassForRegister(Record r) {
    ArrayList<CodeGenRegisterClass> rcs = getRegisterClasses();
    CodeGenRegisterClass foundRC = null;
    for (int i = 0, e = rcs.size(); i != e; ++i) {
      CodeGenRegisterClass rc = registerClasses.get(i);
      for (int ei = 0, ee = rc.members.size(); ei != ee; ++ei) {
        if (!rc.contains(r))
          continue;

        if (foundRC == null) {
          foundRC = rc;
          continue;
        }
        // If a register's classes have different types, return null.
        if (!rc.getValueTypes().equals(foundRC.getValueTypes()))
          return null;

        // Check to see if the previously found class that contains
        // the register is a subclass of the current class. If so,
        // prefer the superclass.
        if (rc.hasSubClass(foundRC)) {
          foundRC = rc;
          continue;
        }

        if (foundRC.hasSupClass(rc))
          continue;

        // Multiple classes, and neither is a superclass of the other.
        // Return null.
        return null;
      }
    }
    return foundRC;
  }

  /**
   * Find the union of all possible SimpleValueTypes for the
   * specified physical register.
   *
   * @param r
   * @return
   */
  public ArrayList<ValueTypeByHwMode> getRegisterVTs(Record r) {
    ArrayList<ValueTypeByHwMode> res = new ArrayList<>();
    for (CodeGenRegisterClass rc : registerClasses) {
      if (rc.contains(r)) {
        ArrayList<ValueTypeByHwMode> inVTs = rc.getValueTypes();
        res.addAll(inVTs);
      }
    }
    return res;
  }
}
