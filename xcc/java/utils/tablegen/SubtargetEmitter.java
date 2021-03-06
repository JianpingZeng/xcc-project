package utils.tablegen;
/*
 * Extremely Compiler Collection
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

import backend.target.InstrItinerary;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.Util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;

import static utils.tablegen.Record.LessRecord;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class SubtargetEmitter extends TableGenBackend {
  private RecordKeeper records;
  private String targetName;
  private boolean hasItrineraries;
  private CodeGenTarget target;

  private void enumeration(PrintStream os, String className, boolean isBits) {
    // Get all records of class and sort
    ArrayList<Record> defList = records.getAllDerivedDefinition(className);
    defList.sort(LessRecord);

    int i = 0;
    for (Record r : defList) {
      os.printf("\tpublic static final int %s", r.getName());

      if (isBits) os.printf(" = 1 << %d", i);
      else os.printf(" = %d", i);

      os.println(";");
      ++i;
    }
  }

  /**
   * Emit data of all the subtarget features.  Used by the
   * command line.
   *
   * @param os
   */
  private void featureKeValues(PrintStream os) {
    ArrayList<Record> featureList = records.getAllDerivedDefinition("SubtargetFeature");
    featureList.sort(LessRecord);

    // Begin feature table
    os.print("\t// Sorted (by key) array of values for CPU features.\n");
    os.printf("\tprivate static final SubtargetFeatureKV[] %sFeatureKV = {\n", targetName);

    for (int i = 0, e = featureList.size(); i != e; i++) {
      Record feature = featureList.get(i);

      String name = feature.getName();
      String commandLineName = feature.getValueAsString("Name");
      String desc = feature.getValueAsString("Desc");

      if (commandLineName == null || commandLineName.isEmpty())
        continue;

      // Emit as { "feature", "description", featureEnum, i1 | i2 | ... | in }
      os.printf("\t\tnew SubtargetFeatureKV(\"%s\", ", commandLineName);
      os.printf("\"%s\", ", desc);
      os.printf("%s, ", name);

      ArrayList<Record> impliesList = feature.getValueAsListOfDefs("Implies");
      if (impliesList.isEmpty())
        os.print("0");
      else {
        for (int j = 0, sz = impliesList.size(); j != sz; j++) {
          os.print(impliesList.get(j).getName());
          if (j < sz - 1) os.print(" | ");
        }
      }

      os.print(")");

      if (i < e - 1) os.print(",");

      os.println();
    }

    // End of feature table.
    os.print("\t};\n");
  }

  /**
   * Emit data of all the subtarget processors.  Used by command line.
   *
   * @param os
   */
  private void cpuKeyValues(PrintStream os) {
    ArrayList<Record> processorList = records.getAllDerivedDefinition("Processor");

    processorList.sort(LessRecord);

    os.println("\t// Sorted (by key) array of values for CPU subtype.\n");
    os.printf("\tprivate static final SubtargetFeatureKV[] %sSubTypeKV = {\n", targetName);

    // For each processor.
    for (int i = 0, e = processorList.size(); i != e; i++) {
      Record processor = processorList.get(i);

      String name = processor.getValueAsString("Name");
      ArrayList<Record> featureList = processor.getValueAsListOfDefs("Features");

      // Emit as { "cpu", "description", f1 | f2 | ... fn },
      os.printf("\t\tnew SubtargetFeatureKV(\"%s\", \"Select the %s processor\", ",
          name, name);
      if (featureList.isEmpty())
        os.print("0");
      else {
        for (int j = 0, sz = featureList.size(); j != sz; j++) {
          os.print(featureList.get(j).getName());
          if (j < sz - 1) os.print(" | ");
        }
      }

      // The "0" is for the "implies" section of this data structure.
      os.print(", 0)");

      if (i < e - 1) os.print(",");
      os.println();
    }

    // End processor table
    os.println("\t};");
  }

  /**
   * Gathers and enumerates all the itinerary classes.
   * Returns itinerary class count.
   *
   * @param os
   * @param itinClassesMap
   * @return
   */
  private int collectAllItinClasses(PrintStream os, TObjectIntHashMap<String> itinClassesMap) {
    ArrayList<Record> itinClassList = records.getAllDerivedDefinition("InstrItinClass");

    itinClassList.sort(LessRecord);

    // for each itinenary class.
    int n = itinClassList.size();
    for (int i = 0; i < n; i++) {
      Record itinClass = itinClassList.get(i);
      itinClassesMap.put(itinClass.getName(), i);
    }

    return n;
  }

  /**
   * Compose a string containing the stage
   * data initialization for the specified itinerary.  N is the number
   * of stages.
   *
   * @param itinData
   * @param itinString
   * @return
   */
  private int formItineraryStageString(String name, Record itinData, StringBuilder itinString) {
    ArrayList<Record> stageList = itinData.getValueAsListOfDefs("Stages");

    int n = stageList.size();
    int res = stageList.size();

    for (int i = 0; i < n; i++) {
      Record stage = stageList.get(i);

      int cycles = (int) stage.getValueAsInt("Cycles");
      itinString.append("\tnew InstrStage(").append(cycles).append(", ");

      ArrayList<Record> unitList = stage.getValueAsListOfDefs("Units");

      for (int j = 0, m = unitList.size(); j < m; j++) {
        itinString.append(name).append("FU.");
        itinString.append(unitList.get(j).getName());
        if (j < m - 1) itinString.append(" | ");
      }

      int timeInc = (int) stage.getValueAsInt("TimeInc");
      itinString.append(", ").append(timeInc);

      itinString.append(" )");
      if (i < n - 1) itinString.append(", ");
    }
    return res;
  }

  /**
   * Compose a string containing the
   * operand cycle initialization for the specified itinerary.  N is the
   * number of operands that has cycles specified.
   *
   * @param itinData
   * @param itinString
   * @return The number of operand cycles.
   */
  private int formItineraryOperandCycleString(Record itinData,
                                              StringBuilder itinString) {
    TIntArrayList operandCycleList = itinData.getValueAsListOfInts("OperandCycles");

    for (int i = 0, e = operandCycleList.size(); i != e; i++) {
      itinString.append(" ").append(operandCycleList.get(i));
      if (i < e - 1) itinString.append(", ");
    }
    return operandCycleList.size();
  }

  private void formItineraryBypassString(String name,
                                        Record itinData,
                                        StringBuilder itinString,
                                         int nOperandCycles) {
    ArrayList<Record> bypassList = itinData.getValueAsListOfDefs("Bypasses");
    int i = 0, n = bypassList.size();
    while (i < n) {
      itinString.append(name).append("Bypass.").append(bypassList.get(i).getName());
      if (++i < nOperandCycles) itinString.append(", ");
    }
    while(i < nOperandCycles) {
      itinString.append(" 0");
      if (++i < nOperandCycles)
        itinString.append(", ");
    }
  }

  private void emitStageAndOperandCycleData(PrintStream os,
                                            int nItinClasses,
                                            TObjectIntHashMap<String> itinClassesMap,
                                            ArrayList<Record> itinClassList,
                                            ArrayList<InstrItinerary[]> procList) {
    ArrayList<Record> procItinList = records.getAllDerivedDefinition("ProcessorItineraries");

    if (procItinList.size() < 2) return;

    // Print out the Function units table for the processor itineraries.
    procItinList.forEach(proc -> {
      ArrayList<Record> fu = proc.getValueAsListOfDefs("FU");
      if (!fu.isEmpty()) {
        String name = proc.getName();
        os.println();
        os.printf("\t// Functional units for itineraries \"%s\"\n", name);
        os.printf("\tinterface %sFU {\n", name);
        for (int j = 0, e = fu.size(); j < e; ++j) {
          os.printf("\t\tint %s = 1 << %d;\n", fu.get(j).getName(), j);
        }
        os.println("\t}");
      }

      // Print out the bypass information for the itineraries.
      ArrayList<Record> bp = proc.getValueAsListOfDefs("BP");
      if (!bp.isEmpty()) {
        String name = proc.getName();
        os.println();
        os.printf("\t// Pipeline forwarding pathes for itineraries \"%s\"\n", name);
        os.printf("\tinterface %sBypass {\n", name);
        os.println("\t\tint NoBypass = 0;");
        for (int j = 0, e = bp.size(); j < e; ++j) {
          os.printf("\t\tint %s = 1 << %d;\n", bp.get(j).getName(), j);
        }
        os.println("\t}");
      }
    });

    // Output the stage data table.
    StringBuilder stageTable = new StringBuilder(String.format("private static final InstrStage[] %sStages = {\n", targetName));
    stageTable.append("\tnew InstrStage(0, 0, 0),\t// No itinerary\n");

    StringBuilder operandCycleTable = new StringBuilder(String.format("private static final int[] %sOperandCycles = {\n", targetName));
    operandCycleTable.append(" 0, // No itinerary\n");

    // A table for recording bypass information.
    StringBuilder bypassTable = new StringBuilder();
    bypassTable.append("private static final int[] ").append(targetName);
    bypassTable.append("ForwardingPathes = {\n");
    bypassTable.append("  0, // No Itinerary\n");

    int stageCount = 1, operandCycleCount = 1;

    TObjectIntHashMap<String> itinStageMap = new TObjectIntHashMap<>();
    TObjectIntHashMap<String> itinOperandCycleMap = new TObjectIntHashMap<>();
    for (int i = 0, e = procItinList.size(); i < e; i++) {
      Record proc = procItinList.get(i);
      String name = proc.getName();

      // Skip the default itinerary.
      if (name.equals("NoItineraries")) continue;

      // Create and expand processor itinerary to cover all itinerary classes.
      InstrItinerary[] itinList = new InstrItinerary[nItinClasses];
      for (int x = nItinClasses; x > 0; x--)
        itinList[i] = new InstrItinerary();

      // Get the itinerary data list.
      ArrayList<Record> itinDataList = proc.getValueAsListOfDefs("IID");
      // Iterate over it.
      for (int j = 0, m = itinDataList.size(); j != m; j++) {
        Record itinData = itinDataList.get(j);

        // get the string and stage count.
        StringBuilder buf = new StringBuilder();
        int nstages = formItineraryStageString(name, itinData, buf);
        String itinStageString = buf.toString();

        // Get string and operand cycle count
        buf = new StringBuilder();
        int nOperandCycles = formItineraryOperandCycleString(itinData, buf);
        String itinOperandCycleString = buf.toString();


        // Get bypass string.
        buf = new StringBuilder();
        formItineraryBypassString(name, itinData, buf, nOperandCycles);
        String itinBypassString = buf.toString();

        // Check to see if stage already exists and create if it doesn't
        int findStage = 0;
        if (nstages > 0) {
          if (!itinStageMap.containsKey(itinStageString)) {
            // Emit as {cycles, u1|u2|...|un, timinc}, // indices.
            stageTable.append(itinStageString).append(", //").append(stageCount);
            if (nstages > 1)
              stageTable.append("-").append(stageCount + nstages - 1);
            stageTable.append("\n");
            // record the itin class number.
            findStage = stageCount;
            itinStageMap.put(itinStageString, findStage);
            stageCount += nstages;
          }
        }

        // Check to see if operand cycle already exists and create if it doesn't
        int findOperandCycle = 0;
        if (nOperandCycles > 0) {
          String itinOperandString = itinOperandCycleString + itinBypassString;
          if (!itinOperandCycleMap.containsKey(itinOperandString)) {
            // Emit as  cycle, // index
            operandCycleTable.append(itinOperandCycleString).append(", // ");
            String operandIdxComment = String.format("%d", operandCycleCount);
            if (nOperandCycles > 1)
              operandIdxComment += "-" + (operandCycleCount + nOperandCycles - 1);

            operandCycleTable.append(operandIdxComment).append('\n');
            // Record itin class number.
            findOperandCycle = operandCycleCount;
            itinOperandCycleMap.put(itinOperandCycleString, findOperandCycle);
            // Emit as bypass, // index.
            bypassTable.append(itinBypassString).append(", //")
                .append(operandIdxComment).append("\n");
            operandCycleCount += nOperandCycles;
          }
        }

        // Locate where to inject into processor itinerary table
        String nameOfTheClass = itinData.getValueAsDef("TheClass").getName();
        int find = itinClassesMap.get(nameOfTheClass);

        int numUops = (int) itinClassList.get(find).getValueAsInt("NumMicroOps");
        // Set up itinerary as location and location + stage count
        InstrItinerary itinerary = new InstrItinerary(numUops, findStage, findStage + nstages,
            findOperandCycle, findOperandCycle + nOperandCycles);

        // Inject - empty slots will be 0, 0
        itinList[find] = itinerary;
      }

      // Add process itinerary to list
      procList.add(itinList);
    }

    stageTable.append("\tnew InstrStage(0, 0, 0)  // End itinerary\n");
    stageTable.append("};\n");

    operandCycleTable.append(" 0, // End of itinerary\n");
    operandCycleTable.append("};\n");

    bypassTable.append("  0 // End itinerary\n");
    bypassTable.append("};\n");


    // Emit tables.
    os.print(stageTable.toString());
    os.print(operandCycleTable);
    os.print(bypassTable);
  }

  private void emitProcessorData(PrintStream os, ArrayList<InstrItinerary[]> procList) {
    ArrayList<Record> itins = records.getAllDerivedDefinition("ProcessorItineraries");

    Iterator<InstrItinerary[]> procListItr = procList.iterator();

    for (Record itin : itins) {
      String name = itin.getName();

      if (name.equals("NoItineraries")) continue;

      os.println();
      os.printf("\tprivate static InstrItinerary[] %s = {\n", name);

      InstrItinerary[] itinList = procListItr.next();
      for (int j = 0, m = itinList.length; j != m; j++) {
        InstrItinerary itinerary = itinList[j];

        if (itinerary == null || itinerary.firstStage == 0)
          os.print("\tnew InstrItinerary(1, 0, 0, 0, 0)");
        else {
          os.printf("\tnew InstrItinerary(%d, %d, %d, %d, %d)",
              itinerary.numMicroOps,
              itinerary.firstStage,
              itinerary.lastStage,
              itinerary.firstOperandCycle,
              itinerary.lastOperandCycle);
        }

        if (j < m - 1)
          os.print(",");

        os.printf("\t// %d\n", j);
      }

      // End processor itinerary table
      os.print("};\n");
    }
  }

  /**
   * generate cpu namespace to itinerary lookup table.
   *
   * @param os
   */
  private void emitProcessorLookup(PrintStream os) {
    ArrayList<Record> processorList = records.getAllDerivedDefinition("Processor");

    processorList.sort(LessRecord);

    os.println();
    os.printf("// Sorted (by key) array of itineraries for CPU subtype.\n" +
        "public static final SubtargetInfoKV[] %sProcItinKV = {\n", targetName);

    // For each processor
    for (int i = 0, n = processorList.size(); i != n; i++) {
      Record processor = processorList.get(i);

      String name = processor.getValueAsString("Name");
      String procItin = processor.getValueAsDef("ProcItin").getName();

      // Emit as { "cpu", procinit },
      os.printf("\tnew SubtargetInfoKV(\"%s\", %s)",
          name, procItin);

      // Depending on ''if more in the list'' emit comma
      if (i < n - 1) os.print(",");
      os.println();
    }
    os.println("};");
  }

  /**
   * Emits all stages and itineries, folding common patterns.
   *
   * @param os
   */
  private void emitData(PrintStream os) {
    TObjectIntHashMap<String> itinClassesMap = new TObjectIntHashMap<>();
    ArrayList<InstrItinerary[]> procList = new ArrayList<>();

    ArrayList<Record> itinClassList = records.getAllDerivedDefinition("InstrItinClass");

    // Enumerate all the itinerary classes
    int nitinCLasses = collectAllItinClasses(os, itinClassesMap);
    // Make sure the rest is worth the effort
    hasItrineraries = nitinCLasses > 1;

    if (hasItrineraries) {
      // Emit the stage data
      emitStageAndOperandCycleData(os, nitinCLasses, itinClassesMap, itinClassList, procList);
      // Emit the processor itinerary data
      emitProcessorData(os, procList);
      // Emit the processor lookup data
      emitProcessorLookup(os);
    }
  }

  /**
   * Produces a subtarget specific function for parsing
   * the subtarget features string.
   *
   * @param os
   */
  private void parseFeaturesFunction(PrintStream os) {
    ArrayList<Record> features = records.getAllDerivedDefinition("SubtargetFeature");
    features.sort(LessRecord);

    os.print("\t/**\n");
    os.println("\t * Parses features string setting specified subtarget options.");
    os.println("\t */");
    os.println("\tpublic void parseSubtargetFeatures(String fs, String cpu) {");
    os.println("\t\tlong bits = reInitMCSubtargetInfo(cpu, fs);");

    for (int i = 0; i < features.size(); i++) {
      Record r = features.get(i);
      String instance = r.getName();
      String value = r.getValueAsString("Value");
      String attribute = r.getValueAsString("Attribute");

      if (Objects.equals(value, "true") || Objects.equals(value, "false")) {
        os.printf("\t\tif ((bits & %s) != 0)\n", instance);
        os.printf("\t\t\tsubtarget.%s = %s;\n", attribute, value);
      } else {
        os.printf("\t\tif ((bits & %s) != 0 && subtarget.%s.compareTo(%s) < 0)\n",
            instance, attribute, value);
        os.printf("\t\t\tsubtarget.%s = %s;\n", attribute, value);
      }
    }
    os.print("\n\t}\n\n");
  }

  public SubtargetEmitter(RecordKeeper r) {
    records = r;
    hasItrineraries = false;
    target = new CodeGenTarget(records);
  }

  private void emitHwModeCheck(PrintStream os) {
    CodeGenHwModes cgh = target.getHwModes();
    Util.assertion(cgh.getNumNodeIds() > 0);
    if (cgh.getNumNodeIds() == 1)
      return;

    os.println("\t@Override");
    os.print("\tpublic int getHwMode() {\n");
    for (int m = 1, num = cgh.getNumNodeIds(); m < num; ++m) {
      CodeGenHwModes.HwMode hm = cgh.getMode(m);
      os.printf("\t\tif (checkFeatures(\"%s\")) return %d;\n", hm.features, m);
    }
    os.println("\t\treturn super.getHwMode();");
    os.println("\t}");
  }

  /**
   * Output the subtarget enumerations, returning true on failure.
   *
   * @param outputFile
   * @throws Exception
   */
  @Override
  public void run(String outputFile) throws FileNotFoundException {
    targetName = new CodeGenTarget(Record.records).getName();

    try (PrintStream os = outputFile.equals("-") ? System.out :
        new PrintStream(new FileOutputStream(outputFile))) {
      os.printf("package backend.target.%s;\n", targetName.toLowerCase());

      emitSourceFileHeaderComment("Subtarget Enumeration Source Fragment", os);
      String className = targetName + "GenSubtarget";

      os.println("import backend.mc.InstrStage;");
      os.println("import backend.mc.SubtargetInfoKV;");
      os.println("import backend.target.InstrItinerary;");
      os.println("import backend.target.SubtargetFeatureKV;");
      os.println("import backend.target.TargetSubtarget;");
      os.printf("import static backend.target.%s.%sSubtarget.*;", targetName.toLowerCase(), targetName);
      os.println();

      os.printf("public abstract class %s extends TargetSubtarget {\n", className);

      enumeration(os, "SubtargetFeature", true);
      os.println();

      featureKeValues(os);
      os.println();

      cpuKeyValues(os);
      os.println();

      emitData(os);
      os.println();

      parseFeaturesFunction(os);
      emitHwModeCheck(os);

      // Emit constructor method.
      os.printf("\tprotected backend.target.%s.%sSubtarget subtarget;\n", targetName.toLowerCase(), targetName);
      os.printf("\tpublic %s(String tt, String cpu, String fs) {\n", className);
      os.printf("\t\tinitMCSubtargetInfo(tt, cpu, fs, %sFeatureKV, %sSubTypeKV,", targetName, targetName);
      if (hasItrineraries) {
        os.printf("%sProcItinKV, %sStages, %sOperandCycles, %sForwardingPathes);\n",
                targetName, targetName, targetName, targetName);
      } else {
        os.println("null, null, null, null);");
      }
      os.println("\t}\n}");
    }
  }
}
