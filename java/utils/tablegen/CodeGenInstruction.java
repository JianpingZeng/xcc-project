package utils.tablegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng
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

import backend.codegen.MVT;
import tools.Error;
import tools.Pair;
import tools.Util;
import utils.tablegen.Init.DagInit;
import utils.tablegen.Init.DefInit;
import utils.tablegen.RecTy.DagRecTy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

import static utils.tablegen.Init.BinOpInit.BinaryOp.CONCAT;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public final class CodeGenInstruction {
  /**
   * The actual record defining this instruction.
   */
  Record theDef;
  /**
   * Contents of the 'namespace' field.
   */
  String namespace;
  /**
   * The format string used to emit a .s file for the
   * instruction.
   */
  String asmString;

  static class OperandInfo implements Cloneable {
    Record rec;

    String name;

    String printerMethodName;

    int miOperandNo;
    long miNumOperands;

    DagInit miOperandInfo;

    /**
     * Constraint information for this operand.
     */
    ArrayList<String> constraints;
    public ArrayList<Boolean> doNotEncode;

    OperandInfo(Record r, String name, String printerMethodName,
                int miOperandNo, long miNumOperands, DagInit operandInfo) {
      rec = r;
      this.name = name;
      this.printerMethodName = printerMethodName;
      this.miOperandNo = miOperandNo;
      this.miNumOperands = miNumOperands;
      this.miOperandInfo = operandInfo;
      constraints = new ArrayList<>();
      doNotEncode = new ArrayList<>();
    }

    @Override
    public OperandInfo clone() {
      OperandInfo res = new OperandInfo(rec, name, printerMethodName,
          miOperandNo, miNumOperands, miOperandInfo);
      res.constraints.addAll(constraints);
      res.doNotEncode.addAll(doNotEncode);
      return res;
    }
  }

  /**
   * Number of def operands declared.
   */
  int numDefs;

  ArrayList<OperandInfo> operandList;
  /**
   * The register implicit defined and used by this instruction
   */
  ArrayList<Record> implicitDefs;
  ArrayList<Record> implicitUses;

  // Various boolean values we track for the instruction.
  boolean isReturn;
  boolean isBranch;
  boolean isIndirectBranch;
  boolean isBarrier;
  boolean isCall;
  boolean canFoldAsLoad;
  boolean isPredicable;
  boolean mayLoad;
  boolean mayLoadUnset;
  boolean mayStore;
  boolean mayStoreUnset;
  boolean isTwoAddress;
  boolean isConvertibleToThreeAddress;
  boolean isCommutable;
  boolean isTerminator;
  boolean isReMaterializable;
  boolean hasDelaySlot;
  boolean usesCustomDAGSchedInserter;
  boolean isVariadic;
  boolean hasCtrlDep;
  boolean isNotDuplicable;
  boolean hasOptionalDef;
  boolean hasSideEffects;
  boolean mayHaveSideEffects;
  boolean haveSideEffectsUnset;
  boolean neverHasSideEffects;
  boolean isAsCheapAsAMove;
  boolean noResults;

  CodeGenInstruction(Record r, String asmStr) {
    theDef = r;
    namespace = r.getValueAsString("Namespace");
    asmString = asmStr;
    operandList = new ArrayList<>();

    isReturn = r.getValueAsBit("isReturn");
    isBranch = r.getValueAsBit("isBranch");
    isIndirectBranch = r.getValueAsBit("isIndirectBranch");
    isBarrier = r.getValueAsBit("isBarrier");
    isCall = r.getValueAsBit("isCall");
    canFoldAsLoad = r.getValueAsBit("canFoldAsLoad");
    mayLoad = r.getValueAsBit("mayLoad");
    mayStore = r.getValueAsBit("mayStore");
    isTwoAddress = r.getValueAsBit("isTwoAddress");
    isConvertibleToThreeAddress = r.getValueAsBit("isConvertibleToThreeAddress");
    isCommutable = r.getValueAsBit("isCommutable");
    isTerminator = r.getValueAsBit("isTerminator");
    isReMaterializable = r.getValueAsBit("isReMaterializable");
    hasDelaySlot = r.getValueAsBit("hasDelaySlot");
    usesCustomDAGSchedInserter = r
        .getValueAsBit("usesCustomDAGSchedInserter");
    hasCtrlDep = r.getValueAsBit("hasCtrlDep");
    isNotDuplicable = r.getValueAsBit("isNotDuplicable");
    hasSideEffects = r.getValueAsBit("hasSideEffects");
    mayHaveSideEffects = r.getValueAsBit("mayHaveSideEffects");
    neverHasSideEffects = r.getValueAsBit("neverHasSideEffects");
    isAsCheapAsAMove = r.getValueAsBit("isAsCheapAsAMove");
    hasOptionalDef = false; //r.getValueAsBit("hasOptionalDef");
    //noResults = r.getValueAsBit("noResults");
    isVariadic = false;

    implicitDefs = r.getValueAsListOfDefs("Defs");
    implicitUses = r.getValueAsListOfDefs("Uses");

    if ((mayHaveSideEffects ? 1 : 0) + (neverHasSideEffects ? 1 : 0) + (hasSideEffects ? 1 : 0) > 1)
      Error.printFatalError(r.getName() + ": multiple conflicting side effect flags-set!");

    DagInit di = r.getValueAsDag("OutOperandList");
    numDefs = di.getNumArgs();
    DagInit idi = r.getValueAsDag("InOperandList");
    di = (DagInit) ((new Init.BinOpInit(CONCAT, di, idi, new DagRecTy())).fold(r, null));

    int MIOperandNo = 0;
    HashSet<String> OperandNames = new HashSet<>();
    for (int i = 0, e = di.getNumArgs(); i != e; ++i) {
      if (!(di.getArg(i) instanceof DefInit))
        Error.printFatalError("Illegal operand for the '" + r.getName()
            + "' instruction!");

      DefInit Arg = (DefInit) (di.getArg(i));

      Record rec = Arg.getDef();
      String PrintMethod = "printOperand";
      long numOps = 1;
      DagInit miOpInfo = null;
      if (rec.isSubClassOf("Operand")) {
        PrintMethod = rec.getValueAsString("PrintMethod");
        //numOps = rec.getValueAsInt("NumMIOperands");
        miOpInfo = rec.getValueAsDag("MIOperandInfo");

        if (!(miOpInfo.getOperator() instanceof DefInit) ||
            !((DefInit) miOpInfo.getOperator()).getDef().getName().equals("ops")) {
          Error.printFatalError("Bad value for MIOperandInfo in operand '" +
              rec.getName() + "'\n");
        }

        int numArgs = miOpInfo.getNumArgs();
        if (numArgs != 0)
          numOps = numArgs;

        if (rec.isSubClassOf("PredicateOperand")) {
          isPredicable = true;
        } else if (rec.isSubClassOf("OptionalDefOperand"))
          hasOptionalDef = true;
      } else if (Objects.equals(rec.getName(), "variable_ops")) {
        isVariadic = true;
        continue;
      } else if (!rec.isSubClassOf("RegisterClass") &&
          !rec.getName().equals("ptr_rc") &&
          !rec.getName().equals("unknown"))
        Error.printFatalError("Unknown operand class '" + rec.getName()
            + "' in instruction '" + r.getName()
            + "' instruction!");

      // Check that the operand has a namespace and that it's unique.
      if (di.getArgName(i).isEmpty())
        Error.printFatalError(
            "In instruction '" + r.getName() + "', operand #" + i
                + " has no namespace!");
      if (!OperandNames.add(di.getArgName(i)))
        Error.printFatalError(
            "In instruction '" + r.getName() + "', operand #" + i
                + " has the same namespace as a previous operand!");

      operandList.add(new OperandInfo(rec, di.getArgName(i), PrintMethod,
          MIOperandNo, numOps, miOpInfo));
      MIOperandNo += numOps;
    }

    // Parse the constraints.
    parseConstraints(r.getValueAsString("Constraints"), this);

    if (isTwoAddress) {
      if (!operandList.get(1).constraints.get(0).isEmpty())
        Error.printFatalError(r.getName() + ": cannot use isTwoAddress property: instruction " +
            " already has constraint set!");
      operandList.get(1).constraints.set(0, "((0 << 16) | (1 << TIED_TO))");
    }

    for (int op = 0, e = operandList.size(); op != e; op++) {
      for (int j = 0, ee = (int) operandList.get(op).miNumOperands; j != ee; j++) {
        if (operandList.get(op).constraints.get(j).isEmpty())
          operandList.get(op).constraints.set(j, "0");
      }
    }

    String disableEncoding = r.getValueAsString("DisableEncoding");
    while (true) {
      String[] opNames = disableEncoding.split(",\t");

      if (opNames.length <= 0 || opNames[0].isEmpty())
        break;

      String opName = opNames[0];
      Pair<Integer, Integer> op = parseOperandName(opName, false);

      //if (op.second >= operandList.get(op.first).doNotEncode.size())
      operandList.get(op.first).doNotEncode.add(true);
    }
  }

  private static void parseConstraints(String constraints,
                                       CodeGenInstruction inst) {
    // Make sure the constraints list for each operand is large enough to hold
    // constraint info, even if none is present.
    for (int i = 0, e = inst.operandList.size(); i != e; i++) {
      for (int j = 0; j < inst.operandList.get(i).miNumOperands; j++)
        inst.operandList.get(i).constraints.add("");
    }

    if (constraints.isEmpty())
      return;

    String delims = ",";
    for (String sub : constraints.split(delims)) {
      if (!sub.isEmpty()) {
        // Make sure the constraints list for each operand is large enough to hold
        // constraint info, even if none is present.
        for (int i = 0, e = inst.operandList.size(); i != e; i++) {
          for (int j = 0; j < inst.operandList.get(i).miNumOperands; j++)
            inst.operandList.get(i).constraints.add("");
        }
      }
    }
  }

  private Pair<Integer, Integer> parseOperandName(String opName) {
    return parseOperandName(opName, true);
  }

  private Pair<Integer, Integer> parseOperandName(String op, boolean allowWholeOp) {
    if (op.isEmpty() || op.charAt(0) != '$')
      Error.printFatalError(theDef.getName() + ": Illegal operand namespace: '"
          + op + "'");

    String opName = op.substring(1);
    String subOpName = "";

    int dotIdx = opName.indexOf('.');
    if (dotIdx != -1) {
      subOpName = opName.substring(dotIdx + 1);
      if (subOpName.isEmpty())
        Error.printFatalError(theDef.getName() + ": illegal empty suboperand namespace in '" +
            op + "'");
      opName = opName.substring(0, dotIdx);
    }

    int opIdx = getOperandNamed(opName);

    if (subOpName.isEmpty()) {
      if (operandList.get(opIdx).miNumOperands > 1 && !allowWholeOp && subOpName.isEmpty())
        Error.printFatalError(theDef.getName() + ": Illegal to refer to " +
            " whole operand part of complex operand '" + op + "'");

      return Pair.get(opIdx, 0);
    }

    DagInit miOpInfo = operandList.get(opIdx).miOperandInfo;
    if (miOpInfo == null) {
      Error.printFatalError(theDef.getName() + ": unknown suboperand namespace in '" + op + "'");
    }

    for (int i = 0, e = miOpInfo.getNumArgs(); i != e; i++)
      if (miOpInfo.getArgName(i).equals(subOpName))
        return Pair.get(opIdx, i);

    Error.printFatalError(theDef.getName() + ": unknown suboperand namespace in '" +
        op + "'");
    return null;
  }

  /**
   * Return the index of the operand with the specified
   * non-empty namespace.  If the instruction does not have an operand with the
   * specified namespace, throw an exception.
   *
   * @param name
   * @return
   * @throws Exception
   */
  int getOperandNamed(String name) {
    Util.assertion(!name.isEmpty(), "Cannot search for operand with no namespace!");
    for (int i = 0, e = operandList.size(); i != e; ++i)
      if (operandList.get(i).name.equals(name))
        return i;
    Error.printFatalError("Instruction '" + theDef.getName()
        + "' does not have an operand named '$" + name + "'!");
    return -1;
  }

  public int hasOneImplicitDefWithKnownVT(CodeGenTarget target) {
    if (implicitDefs.isEmpty()) return MVT.Other;

    Record firstImplicitDef = implicitDefs.get(0);
    Util.assertion(firstImplicitDef.isSubClassOf("Register"));
    ArrayList<ValueTypeByHwMode> vts = target.getRegisterVTs(firstImplicitDef);
    if (vts.size() == 1 && vts.get(0).isSimple())
      return vts.get(0).getSimple().simpleVT;

    return MVT.Other;
  }
}
