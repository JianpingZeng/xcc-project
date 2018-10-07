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

import backend.codegen.MVT;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.Error;
import tools.Util;
import utils.tablegen.Init.ListInit;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static utils.tablegen.CodeGenTarget.getValueType;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public class CallingConvEmitter extends TableGenBackend {
  private static class TopSortNode {
    String name;
    HashSet<TopSortNode> ins;
    HashSet<TopSortNode> outs;

    public TopSortNode(String name) {
      this.name = name;
      ins = new HashSet<>();
      outs = new HashSet<>();
    }

  }

  private RecordKeeper records;
  private int counter;

  public CallingConvEmitter(RecordKeeper r) {
    records = r;
  }

  /**
   * Outputs the calling convention.
   *
   * @param outputFile
   * @throws Exception
   */
  @Override
  public void run(String outputFile) throws FileNotFoundException {
    try (PrintStream os = outputFile.equals("-") ?
        System.out :
        new PrintStream(new FileOutputStream(outputFile))) {

      CodeGenTarget target = new CodeGenTarget(Record.records);
      String targetName = target.getName();
      String className = target.getName() + "GenCallingConv";

      os.printf("package backend.target.%s;\n\n", targetName.toLowerCase());
      os.printf("import backend.codegen.*;\n"
          + "import backend.codegen.CCValAssign.LocInfo;\n"
          + "import backend.support.CallingConv;\n" + "\n"
          + "import static backend.target.%s.%sGenRegisterNames.*;\n",
          targetName.toLowerCase(), targetName);

      emitSourceFileHeaderComment("Calling convetion Implementation Fragment", os);

      ArrayList<Record> ccs = records.getAllDerivedDefinition("CallingConv");

      os.printf("public class %s {\n", className);

      HashMap<String, Record> recNameToRec = new HashMap<>();
      for (Record cc : ccs) {
        recNameToRec.put(cc.getName(), cc);
      }
      ArrayList<String> dependency = topologicalSortOfAction(ccs);

      // Emit each calling convention description in full.
      for (String dept : dependency) {
        Util.assertion(recNameToRec.containsKey(dept), "No cc action have namespace " + dept);
        emitCallingConv(recNameToRec.get(dept), os);
      }
      os.println("}");
    }
  }

  private HashMap<String, TopSortNode> nameToNodeMap = new HashMap<>();

  private TopSortNode getOrCreate(String name) {
    if (nameToNodeMap.containsKey(name))
      return nameToNodeMap.get(name);
    TopSortNode node = new TopSortNode(name);
    nameToNodeMap.put(name, node);
    return node;
  }

  private TObjectIntHashMap<String> map = new TObjectIntHashMap<>();
  int cnt = 1;

  private int get(String name) {
    if (map.containsKey(name))
      return map.get(name);
    map.put(name, cnt++);
    return cnt - 1;
  }

  private void createTopGraph(Record cc, Record action) {
    TopSortNode node = getOrCreate(cc.getName());
    if (action.isSubClassOf("CCPredicateAction")) {
      createTopGraph(cc, action.getValueAsDef("SubAction"));
    } else {
      if (action.isSubClassOf("CCDelegateTo")) {
        // pretend its dependency action to the dependency set.
        Record r = action.getValueAsDef("CC");
        TopSortNode t = getOrCreate(r.getName());
        t.outs.add(node);
        node.ins.add(t);
      } else if (action.isSubClassOf("CCCustom")) {
        String actName = action.getValueAsString("FuncName");
        TopSortNode t = getOrCreate(actName);
        t.outs.add(node);
        node.ins.add(t);
      }
    }
  }

  private ArrayList<String> topologicalSortOfAction(ArrayList<Record> ccs)
      {
    for (Record cc : ccs) {
      ListInit ccActions = cc.getValueAsListInit("Actions");
      for (int i = 0, e = ccActions.getSize(); i != e; i++) {
        Record action = ccActions.getElementAsRecord(i);
        createTopGraph(cc, action);
      }
    }

    ArrayList<String> res = new ArrayList<>();
    while (!nameToNodeMap.isEmpty()) {
      TopSortNode root = findRoot(nameToNodeMap);
      nameToNodeMap.remove(root.name);
      if (TableGen.DEBUG) {
        if (root.outs.isEmpty()) {
          System.out.printf("%d\n", get(root.name));
        } else {
          for (TopSortNode out : root.outs) {
            out.ins.remove(root);
            System.out.printf("%d->%d\n", get(root.name), get(out.name));
          }
        }
      } else {
        for (TopSortNode out : root.outs) {
          out.ins.remove(root);
        }
      }

      res.add(root.name);
    }
    if (TableGen.DEBUG) {
      System.out.println("\n\nTopological result");
      res.forEach(name -> System.out.println(get(name)));

      System.out.println();
      res.forEach(name ->
      {
        System.out.println(name + " " + get(name));
      });
    }
    return res;
  }

  private TopSortNode findRoot(HashMap<String, TopSortNode> nameToNodeMap) {
    // Find the root node.
    for (TopSortNode node : nameToNodeMap.values()) {
      if (node.ins.isEmpty())
        return node;
    }
    Util.assertion(false, "No root node for topological sort");
    return null;
  }

  private void emitCallingConv(Record cc, PrintStream os) {
    ListInit ccActions = cc.getValueAsListInit("Actions");
    counter = 0;
    os.printf("\n\tpublic static CCAssignFn %s = (valNo, valVT, locVT, "
        + "locInfo, argFlags, state) ->\n", cc.getName());
    os.printf("\t{\n");

    // Emit all of the actions, in order.
    for (int i = 0, e = ccActions.getSize(); i != e; i++) {
      os.println();
      emitAction(ccActions.getElementAsRecord(i), 2, os);
    }
    os.printf("\n\treturn true;\t// CC didn't match.\n");
    os.println("\t};\n");
  }

  private void emitAction(Record action, int indent, PrintStream os)
      {
    String indentStr = Util.fixedLengthString(indent, ' ');
    if (action.isSubClassOf("CCPredicateAction")) {
      os.printf("%sif (", indentStr);

      if (action.isSubClassOf("CCIfType")) {
        ListInit vts = action.getValueAsListInit("VTs");
        for (int i = 0, e = vts.getSize(); i != e; i++) {
          Record vt = vts.getElementAsRecord(i);
          if (i != 0) os.printf(" ||\n%s%s", indentStr, indentStr);
          os.printf("locVT.equals(new EVT(%s))", MVT.getEnumName(getValueType(vt)));
        }
      } else if (action.isSubClassOf("CCIf"))
        os.printf(action.getValueAsString("Predicate"));
      else {
        action.dump();
        Error.printFatalError("Unknown CCPredicateAction!");
      }

      os.printf(") {\n");
      emitAction(action.getValueAsDef("SubAction"), indent + 2, os);
      os.printf("%s}\n", indentStr);
    } else {
      if (action.isSubClassOf("CCDelegateTo")) {
        Record cc = action.getValueAsDef("CC");
        os.printf("%sif (!%s.apply(valNo, valVT, locVT, locInfo, argFlags, state))\n",
            indentStr, cc.getName());
        os.printf("%s\treturn false;\n", indentStr);
      } else if (action.isSubClassOf("CCAssignToReg")) {
        ListInit regList = action.getValueAsListInit("RegList");
        if (regList.getSize() == 1) {
          os.printf("int reg = state.allocateReg(%s);\n",
              regList.getElementAsRecord(0).getName());

          os.printf("%sif (reg != 0) {\n", indentStr);
        } else {
          os.printf("%sint[] regList%d = {\n", indentStr, ++counter);
          os.printf(indentStr);
          for (int i = 0, e = regList.getSize(); i != e; i++) {
            if (i != 0) os.printf(", ");
            os.printf(regList.getElementAsRecord(i).getName());
          }
          os.println();
          os.printf("%s};\n", indentStr);
          os.printf("%sint reg = state.allocateReg(regList%d);\n",
              indentStr, counter);
          os.printf("%sif (reg != 0) {\n", indentStr);
        }
        os.printf("%s\tstate.addLoc(CCValAssign.getReg(valNo, valVT, reg, locVT, locInfo));\n", indentStr);
        os.printf("%s\treturn false;\n", indentStr);
        os.printf("%s}\n", indentStr);
      } else if (action.isSubClassOf("CCAssignToRegWithShadow")) {
        ListInit regList = action.getValueAsListInit("RegList");
        ListInit shadowRegList = action.getValueAsListInit("ShadowRegList");
        if (shadowRegList.getSize() > 0 && shadowRegList.getSize() != regList.getSize()) {
          Error.printFatalError("Invalid length of list of shadowed registers");
        }

        if (regList.getSize() == 1) {
          os.printf("%sint reg = state.allocateReg(%s, %s);\n",
              indentStr,
              regList.getElementAsRecord(0).getName(),
              shadowRegList.getElementAsRecord(0).getName());
          os.printf("%sif (reg != 0) {\n", indentStr);
        } else {
          int regListNumber = ++counter;
          int shadowRegListNumber = ++counter;

          os.printf("%sint[] regList%d = {\n", indentStr, regListNumber);
          os.printf(indentStr + "\t");
          for (int i = 0, e = regList.getSize(); i != e; i++) {
            if (i != 0) os.printf(", ");
            os.printf(regList.getElementAsRecord(i).getName());
          }

          os.println();
          os.printf("%s};\n", indentStr);

          os.printf("%sint[] regList%d = {\n", indentStr, shadowRegListNumber);
          os.printf(indentStr + '\t');
          for (int i = 0, e = shadowRegList.getSize(); i != e; i++) {
            if (i != 0) os.print(", ");
            os.printf(shadowRegList.getElementAsRecord(i).getName());
          }

          os.println();
          os.printf("%s};\n", indentStr);

          os.printf("%sint reg = state.allocateReg(regList%d, regList%d);\n",
              indentStr, regListNumber, shadowRegListNumber);
          os.printf("%sif (reg != 0) {\n", indentStr);
        }

        os.print(indentStr);
        os.printf("\tstate.addLoc(CCValAssign.getReg(valNo, valVT, reg, locVT, locInfo));\n");
        os.printf("%s\treturn false;\n", indentStr);
        os.printf("%s}\n", indentStr);
      } else if (action.isSubClassOf("CCAssignToStack")) {
        int size = (int) action.getValueAsInt("Size");
        int align = (int) action.getValueAsInt("Align");

        os.printf("%sint offset%d = state.allocateStack(", indentStr, ++counter);
        if (size != 0)
          os.printf("%d, ", size);
        else
          os.printf("\n%s\t(int)state.getTarget().getTargetData()" +
                  ".getTypeAllocSize(locVT.getTypeForEVT()), ",
              indentStr);

        if (align != 0)
          os.printf("%d", align);
        else
          os.printf("\n%s\tstate.getTarget().getTargetData()" +
                  ".getABITypeAlignment(locVT.getTypeForEVT())\n",
              indentStr);
        os.printf(");\n%s", indentStr);
        os.print(indentStr);
        os.printf("state.addLoc(CCValAssign.getMem(valNo, valVT, offset%d, locVT, locInfo));\n", counter);
        os.print(indentStr);
        os.println("return false;");
      } else if (action.isSubClassOf("CCPromoteToType")) {
        Record destTy = action.getValueAsDef("DestTy");
        os.printf("%slocVT = new EVT(%s);\n", indentStr, MVT.getEnumName(getValueType(destTy)));
        os.printf("%sif (argFlags.isSExt())\n", indentStr);
        os.printf("%s%slocInfo = LocInfo.SExt;\n", indentStr, indentStr);
        os.printf("%selse if (argFlags.isZExt())\n", indentStr);
        os.printf("%s%slocInfo = LocInfo.ZExt;\n", indentStr, indentStr);
        os.printf("%selse\n", indentStr);
        os.printf("%s%slocInfo = LocInfo.AExt;\n", indentStr, indentStr);
      } else if (action.isSubClassOf("CCBitConvertToType")) {
        Record destTy = action.getValueAsDef("DestTy");
        os.printf("%slocVT = new EVT(%s);\n", indentStr, MVT.getEnumName(getValueType(destTy)));
        os.printf("%slocInfo = LocInfo.BCvt;\n", indentStr);
      } else if (action.isSubClassOf("CCPassIndirect")) {
        Record destTy = action.getValueAsDef("DestTy");
        os.printf("%slocVT = new EVT(%s);\n", indentStr, MVT.getEnumName(getValueType(destTy)));
        os.printf("%slocInfo = LocInfo.Indirect;\n", indentStr);
      } else if (action.isSubClassOf("CCPassByVal")) {
        int size = (int) action.getValueAsInt("Size");
        int align = (int) action.getValueAsInt("Align");
        os.printf("%sstate.handleByVal(valNo, valVT, locVT, locInfo, %d, %d, argFlags);\n",
            indentStr, size, align);
        os.printf("%sreturn false;\n", indentStr);
      } else if (action.isSubClassOf("CCCustom")) {
        os.printf("%sif (%s.apply(valNo, valVT, locVT, locInfo, argFlags, state))\n",
            indentStr, action.getValueAsString("FuncName"));
        os.printf("%s%sreturn false;\n", indentStr, indentStr);
      } else {
        action.dump();
        Error.printFatalError("Undefined CCAction!");
      }
    }
  }
}
