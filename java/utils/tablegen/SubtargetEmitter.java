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

import backend.target.InstrItinerary;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.Util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;

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
    os.printf("\t// Sorted (by key) array of values for CPU features.\n"
        + "\tpublic static final SubtargetFeatureKV[] featureKV = {\n");

    for (int i = 0, e = featureList.size(); i != e; i++) {
      Record feature = featureList.get(i);

      String name = feature.getName();
      String commandLineName = feature.getValueAsString("Name");
      String desc = feature.getValueAsString("Desc");

      if (commandLineName.isEmpty())
        continue;

      // Emit as { "feature", "description", featureEnum, i1 | i2 | ... | in }
      os.printf("\t\tnew SubtargetFeatureKV(\"%s\", ", commandLineName);
      os.printf("\"%s\", ", desc);
      os.printf("%s, ", name);

      ArrayList<Record> impliesList = feature.getValueAsListOfDefs("Implies");
      if (impliesList.isEmpty())
        os.printf("0");
      else {
        for (int j = 0, sz = impliesList.size(); j != sz; j++) {
          os.printf(impliesList.get(j).getName());
          if (j < sz - 1) os.printf(" | ");
        }
      }

      os.print(")");

      if (i < e - 1) os.printf(",");

      os.println();
    }

    // End of feature table.
    os.printf("\t};\n");
  }

  /**
   * Emit data of all the subtarget processors.  Used by command line.
   *
   * @param os
   */
  private void cpuKeyValues(PrintStream os) {
    ArrayList<Record> processorList = records.getAllDerivedDefinition("Processor");

    processorList.sort(LessRecord);

    os.printf("\t// Sorted (by key) array of values for CPU subtype.\n" +
        "\tpublic static final SubtargetFeatureKV[] subTypeKV = {\n");

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
          os.printf(featureList.get(j).getName());
          if (j < sz - 1) os.printf(" | ");
        }
      }

      // The "0" is for the "implies" section of this data structure.
      os.printf(", 0)");

      if (i < e - 1) os.printf(",");
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
  private int formItineraryStageString(Record itinData, StringBuilder itinString) {
    ArrayList<Record> stageList = itinData.getValueAsListOfDefs("Stages");

    int n = stageList.size();
    int res = stageList.size();

    for (int i = 0; i < n; ) {
      Record stage = stageList.get(i);

      int cycles = (int) stage.getValueAsInt("Cycles");
      itinString.append("\t{").append(cycles).append(", ");

      ArrayList<Record> unitList = stage.getValueAsListOfDefs("Units");

      for (int j = 0, m = unitList.size(); i < m; i++) {
        itinString.append(unitList.get(j).getName());
        if (++j < m) itinString.append(" | ");
      }

      int timeInc = (int) stage.getValueAsInt("TimeInc");
      itinString.append(", ").append(timeInc);

      itinString.append(" }");
      if (++i < n) itinString.append(", ");
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

  private void emitStageAndOperandCycleData(PrintStream os,
                                            int nItinClasses,
                                            TObjectIntHashMap<String> itinClassesMap,
                                            ArrayList<ArrayList<InstrItinerary>> procList) {
    ArrayList<Record> procItinList = records.getAllDerivedDefinition("ProcessorItineraries");

    if (procItinList.size() < 2) return;

    StringBuilder stageTable = new StringBuilder("public static final InstrStage[] stages = {\n");
    ;
    stageTable.append("\tnew InstrStage(0, 0, 0),\t// No itinenary\n");

    String operandCycleTable = "public static final int[] operandCycles = {\n";
    operandCycleTable += " 0, // No itinerary\n";

    int stageCount = 1, operandCycleCount = 1;
    int itinStageEnum = 1, itinOperandCycleEnum = 1;

    TObjectIntHashMap<String> itinStageMap = new TObjectIntHashMap<>();
    TObjectIntHashMap<String> itinOperandCycleMap = new TObjectIntHashMap<>();
    for (int i = 0, e = procItinList.size(); i < e; i++) {
      Record proc = procItinList.get(i);

      String name = proc.getName();

      if (name.equals("NoItineraries")) continue;

      ArrayList<InstrItinerary> itinList = new ArrayList<>();

      // add nItinClasses dummmy element into itinList.
      for (int x = nItinClasses; x > 0; x--)
        itinList.add(null);

      ArrayList<Record> itinDataList = proc.getValueAsListOfDefs("IID");

      for (int j = 0, m = itinDataList.size(); i != m; i++) {
        Record itinData = itinDataList.get(j);

        String itinStageString;
        StringBuilder buf = new StringBuilder();
        int nstages = formItineraryStageString(itinData, buf);
        itinStageString = buf.toString();

        // Get string and operand cycle count
        buf = new StringBuilder();
        int nOperandCycles = formItineraryOperandCycleString(itinData, buf);
        String itinOperandCycleString = buf.toString();

        // Check to see if stage already exists and create if it doesn't
        int findStage = 0;
        if (nstages > 0) {
          findStage = itinStageMap.get(itinStageString);
          if (findStage == 0) {
            // Emit as { cycles, u1 | u2 | ... | un, timeinc }, // index
            stageTable.append(itinStageString).append(", // ")
                .append(itinStageEnum).append("\n");
            findStage = stageCount;
            itinStageMap.put(itinStageString, findStage);
            stageCount += nstages;
            itinStageEnum++;
          }
        }

        // Check to see if operand cycle already exists and create if it doesn't
        int findOperandCycle = 0;
        if (nOperandCycles > 0) {
          findOperandCycle = itinOperandCycleMap.get(itinOperandCycleString);
          if (findOperandCycle == 0) {
            // Emit as  cycle, // index
            operandCycleTable += itinOperandCycleString + ", // " + itinOperandCycleEnum + "\n";

            findOperandCycle = operandCycleCount;
            itinOperandCycleMap.put(itinOperandCycleString, operandCycleCount);

            operandCycleCount += nOperandCycles;
            itinOperandCycleEnum++;
          }
        }

        // Set up itinerary as location and location + stage count
        InstrItinerary itinerary = new InstrItinerary(findStage, findStage + nstages,
            findOperandCycle, findOperandCycle + nOperandCycles);

        // Locate where to inject into processor itinerary table
        String nameOfTheClass = itinData.getValueAsDef("TheClass").getName();
        int find = itinClassesMap.get(nameOfTheClass);

        // Inject - empty slots will be 0, 0
        itinList.set(find, itinerary);
      }

      // Add process itinerary to list
      procList.add(itinList);
    }

    stageTable.append("\tnew InstrStage(0, 0, 0)  // End itinerary\n");
    stageTable.append("};\n");

    operandCycleTable += " 0, // End of itinerary\n";
    operandCycleTable += "};\n";

    // Emit tables.
    os.printf(stageTable.toString());
    os.print(operandCycleTable);
  }

  private void emitProcessorData(PrintStream os, ArrayList<ArrayList<InstrItinerary>> procList) {
    ArrayList<Record> itins = records.getAllDerivedDefinition("ProcessItineraries");

    Iterator<ArrayList<InstrItinerary>> procListItr = procList.iterator();

    for (Record itin : itins) {
      String name = itin.getName();

      if (name.equals("NoItineraries")) continue;

      os.println();
      os.printf("\tpublic static InstrItinerary[] %s = {\n", name);

      ArrayList<InstrItinerary> itinList = procListItr.next();
      for (int j = 0, m = itinList.size(); j != m; j++) {
        InstrItinerary itinerary = itinList.get(j);

        if (itinerary.firstStage == 0)
          os.printf("new InstrItinerary(0, 0, 0)");
        else {
          os.printf("\tnew InstrItinerary(%d, %d, %d, %d)",
              itinerary.firstStage,
              itinerary.lastStage,
              itinerary.firstOperandCycle,
              itinerary.lastOperandCycle);
        }

        if (j < m - 1)
          os.printf(",");

        os.printf("\t// %d\n", j);
      }

      // End processor itinerary table
      os.printf("};\n");
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
        "public static final SubtargetInfoKV[] procItinKV = {\n");

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
    ArrayList<ArrayList<InstrItinerary>> procList = new ArrayList<>();

    // Enumerate all the itinerary classes
    int nitinCLasses = collectAllItinClasses(os, itinClassesMap);
    // Make sure the rest is worth the effort
    hasItrineraries = nitinCLasses != 1;

    if (hasItrineraries) {
      // Emit the stage data
      emitStageAndOperandCycleData(os, nitinCLasses, itinClassesMap, procList);
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

    os.printf("\t/**\n");
    os.printf("\t * Parses features string setting specified subtarget options.\n");
    os.println("\t */");
    os.println("\tpublic String parseSubtargetFeatures(String fs, String cpu) {");
    os.println("\t\tSubtargetFeatures features = new SubtargetFeatures(fs);");
    os.println("\t\tfeatures.setCPUIfNone(cpu);");
    os.println("\t\tlong bits = features.getBits(subTypeKV,featureKV);");

    for (int i = 0; i < features.size(); i++) {
      Record r = features.get(i);
      String instance = r.getName();
      String value = r.getValueAsString("Value");
      String attribute = r.getValueAsString("Attribute");

      if (value.equals("true") || value.equals("false")) {
        os.printf("\t\tif ((bits & %s) != 0)\n", instance);
        os.printf("\t\t\t%s = %s;\n", attribute, value);
      } else {
        os.printf("\t\tif ((bits & %s) != 0 && %s.compareTo(%s) < 0)\n",
            instance, attribute, value);
        os.printf("\t\t\t%s = %s;\n", attribute, value);
      }
    }

    if (hasItrineraries) {
      os.println();
      os.print("\t\tInstrItinerary itinerary = (InstrItinerary)" +
          "\t\tfeatures.getInfo(procItinKV);\n" +
          "\t\tInstrItins = instrItineraryData(stages, operandCycles, itinerary);\n");
    }

    os.printf("\t\treturn features.getCPU();\n\t}\n\n");
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

      os.printf("import backend.target.SubtargetFeatureKV;\n"
              + "import backend.target.SubtargetFeatures;\n\n"
              + "import static backend.target.%s.%sSubtarget.*;\n",
          targetName.toLowerCase(), targetName);

      String superClass = targetName + "Subtarget";
      os.printf("public final class %s extends %s {\n", className, superClass);

      enumeration(os, "FuncUnit", true);
      os.println();

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
      os.printf("public %s(%sTargetMachine tm, String tt,String cpu,\n" +
          "                         String fs, int stackAlignOverride, boolean is64Bit) {\n" +
          "\t\tsuper(tm, tt, cpu, fs, stackAlignOverride, is64Bit);\n" +
          "\t\tinitMCSubtargetInfo(tt, cpu, fs, featureKV, subTypeKV, null, null, null, null);\n" +
          "\t}", className, targetName);

      os.print("}");
    }
  }
}
