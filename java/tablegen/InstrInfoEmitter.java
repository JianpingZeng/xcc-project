package utils.tablegen;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2017, Xlous
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

import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class InstrInfoEmitter extends TableGenBackend
{
    private RecordKeeper records;
    private CodeGenDAGPatterns cdp;
    private TObjectIntHashMap<String> itinClassMap;

    public InstrInfoEmitter(RecordKeeper records) throws Exception
    {
        this.records = records;
        cdp = new CodeGenDAGPatterns(records);
        itinClassMap = new TObjectIntHashMap<>();
    }

    @Override
    public void run(String outputFile)
    {
        gatherItinClasses();

        try(PrintStream os = outputFile.equals("-") ? System.out
                : new PrintStream(new FileOutputStream(outputFile)))
        {
            emitSourceFileHeaderComment("Target Instruction Descriptors", os);
            CodeGenTarget target = cdp.getTarget();
            Record instrInfo = target.getInstructionSet();
            ArrayList<CodeGenRegisterClass> rc = target.getRegisterClasses();

            String targetName = target.getName();
            String className = targetName + "InstrInfo";

            TObjectIntHashMap<ArrayList<Record>> emittedLists = new TObjectIntHashMap<>();
            int listNumber = 0;
            TObjectIntHashMap<ArrayList<Record>> emittedBarriers = new TObjectIntHashMap<>();
            int barriersNumber = 0;
            TObjectIntHashMap<Record> barriersMap = new TObjectIntHashMap<>();


            os.println("\n\nimport backend.target;\n");

            os.printf("public interface %s\n{\n", className);

            // Emit all of the instruction's implicit uses and defs.
            for (Map.Entry<String, CodeGenInstruction> pair : target.getInstructions().entrySet())
            {
                Record inst = pair.getValue().theDef;
                ArrayList<Record> uses = inst.getValueAsListOfDefs("Uses");
                if (!uses.isEmpty())
                {
                    if (!emittedLists.containsKey(uses))
                    {
                        printDefList(uses, ++listNumber, os);
                        emittedLists.put(uses, listNumber);
                    }
                }

                ArrayList<Record> defs = inst.getValueAsListOfDefs("Defs");
                if (!defs.isEmpty())
                {
                    ArrayList<Record> rcBarriers = new ArrayList<>();
                    detectRegisterClassBarriers(defs, rc, rcBarriers);
                    if (!rcBarriers.isEmpty())
                    {
                        if (!emittedBarriers.containsKey(rcBarriers))
                        {
                            printBarriers(rcBarriers, ++barriersNumber, os);
                            emittedBarriers.put(rcBarriers, barriersNumber);
                            barriersMap.put(inst, barriersNumber);
                        }
                    }

                    if (!emittedLists.containsKey(defs))
                    {
                        printDefList(defs, ++listNumber, os);
                        emittedLists.put(defs, listNumber);
                    }
                }
            }

            TObjectIntHashMap<ArrayList<String>> operandInfoIDs = new TObjectIntHashMap<>();

            // Emit all of the operand info records.
            emitOperandInfo(os, operandInfoIDs);

            // Emit allof the TargetInstrDesc records in theire ENUM order.
            os.printf("\n\tpublic static final TargetInstrDesc[] insts = {\n");
            ArrayList<CodeGenInstruction> numberedInstrs = new ArrayList<>();
            target.getInstructionsByEnumValue(numberedInstrs);

            // 351, 2
            for (int i = 0, e = numberedInstrs.size(); i != e; ++i)
            {
                //System.err.printf("%d %s\n", i, numberedInstrs.get(i).theDef.getName());
                emitRecord(numberedInstrs.get(i), i, instrInfo, emittedLists, barriersMap, operandInfoIDs, os);
            }
            os.printf("\t};\n");

            os.printf("}\n");
        }
        catch (FileNotFoundException e)
        {
            System.err.printf("File %s does not exist\n", outputFile);
            System.exit(1);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void emitRecord(CodeGenInstruction inst,
            int num, Record instrInfo,
            TObjectIntHashMap<ArrayList<Record>> emittedLists,
            TObjectIntHashMap<Record> barriersMap,
            TObjectIntHashMap<ArrayList<String>> opInfo,
            PrintStream os) throws Exception
    {
        int minOperands = 0;
        if (!inst.operandList.isEmpty())
        {
            int sz = inst.operandList.size();
            minOperands = inst.operandList.get(sz - 1).miOperandNo
                    + (int)inst.operandList.get(sz - 1).miNumOperands;
        }

        os.printf("\t\tnew TargetInstrDesc(%d, %d, %d, %d, \"%s\", 0",
                num, minOperands, inst.numDefs,
                getItinClassNumber(inst.theDef),
                inst.theDef.getName());

        // Emit all of the target independent flags.
        if(inst.isReturn) os.printf("|(1<<TID.Return)");
        if (inst.isBranch) os.printf("|(1<<TID.Branch)");
        if (inst.isIndirectBranch) os.printf("|(1<<TID.IndirectBranch)");
        if (inst.isBarrier) os.printf("|(1<<TID.Barrier)");
        if (inst.hasDelaySlot) os.printf("|(1<<TID.DelaySlot)");
        if (inst.isCall) os.printf("|(1<<TID.Call)");
        if (inst.canFoldAsLoad) os.printf("|(1<<TID.FoldableAsLoad)");
        if (inst.mayLoad) os.printf("|(1<<TID.MayLoad)");
        if (inst.mayStore) os.printf("|(1<<TID.MayStore)");
        if (inst.isPredicable) os.printf("|(1<<TID.Predicable)");
        if (inst.isConvertibleToThreeAddress) os.printf("|(1<<TID.ConvertibleTo3Addr)");
        if (inst.isCommutable) os.printf("|(1<<TID.Commutable)");
        if (inst.isTerminator) os.printf("|(1<<TID.Terminator)");
        if (inst.isReMaterializable) os.printf("|(1<<TID.Rematerializable)");
        if (inst.isNotDuplicable) os.printf("|(1<<TID.NotDuplicable)");
        if (inst.hasOptionalDef) os.printf("|(1<<TID.HasOptionalDef)");
        if (inst.usesCustomDAGSchedInserter)
            os.printf("|(1<<TID.UsesCustomDAGSchedInserter)");
        if (inst.isVariadic)
            os.printf("|(1<<TID.Variadic)");
        if (inst.hasSideEffects)
            os.printf("|(1<<TID.UnmodelSideEffects)");
        if (inst.isAsCheapAsAMove)
            os.printf("|(1<<TID.CheapAsAMove)");

        os.printf(", 0");

        // Emit all of the target-specific flags...
        Init.ListInit li = instrInfo.getValueAsListInit("TSFlagsFields");
        Init.ListInit shift = instrInfo.getValueAsListInit("TSFlagsShifts");
        if (li.getSize() != shift.getSize())
            throw new Exception("Lengths of " + instrInfo.getName()
                    + ":(TargetInfoFields, TargetInfoPositions) must be equal!");

        for (int i = 0, e = li.getSize(); i != e; i++)
        {
            emitShiftedValue(inst.theDef, (Init.StringInit)li.getElement(i),
                    (Init.IntInit)shift.getElement(i), os);
        }

        os.printf(", ");

        // Emit the implicit uses and defs list.
        ArrayList<Record> uses = inst.theDef.getValueAsListOfDefs("Uses");
        if (uses.isEmpty())
            os.printf("null, ");
        else
            os.printf("ImplicitList%d, ", emittedLists.get(uses));

        ArrayList<Record> defs = inst.theDef.getValueAsListOfDefs("Defs");
        if (defs.isEmpty())
            os.printf("null, ");
        else
            os.printf("ImplicitList%d, ", emittedLists.get(defs));

        if (!barriersMap.containsKey(inst.theDef))
            os.printf("null, ");
        else
            os.printf("Barriers%d, ", barriersMap.get(inst.theDef));

        // Emit the operand info.
        ArrayList<String> operandInfo = getOperandInfo(inst);
        if (operandInfo.isEmpty())
            os.printf("null");
        else
            os.printf("OperandInfo%d", opInfo.get(operandInfo));

        os.printf("),\t\t// Inst #%d = %s\n", num, inst.theDef.getName());
    }

    private void emitShiftedValue(Record r, Init.StringInit val,
            Init.IntInit shiftedInt, PrintStream os) throws Exception
    {
        if (val == null || shiftedInt == null)
            throw new Exception("Illegal value or shift amount in TargetInfo*!");

        RecordVal rv = r.getValue(val.getValue());
        int shift = (int) shiftedInt.getValue();

        if (rv == null || rv.getValue() == null)
        {
            switch (r.getName())
            {
                case "PHI":
                case "INLINEASM":
                case "DBG_LABEL":
                case "EH_LABEL":
                case "GC_LABEL":
                case "DECLARE":
                case "EXTRACT_SUBREG":
                case "INSERT_SUBREG":
                case "IMPLICIT_DEF":
                case "SUBREG_TO_REG":
                case "COPY_TO_REGCLASS":
                    return;
                default:
                    throw new Exception(r.getName() +  " doesn't have a field named '"
                        + val.getValue() + "'!");
            }
        }

        Init value = rv.getValue();
        if (value instanceof Init.BitInit)
        {
            Init.BitInit bi = (Init.BitInit)value;
            if (bi.getValue())
                os.printf("|(1<<%d)", shift);
            return;
        }
        else if (value instanceof Init.BitsInit)
        {
            Init.BitsInit bi = (Init.BitsInit)value;
            Init i = bi.convertInitializerTo(new RecTy.IntRecTy());
            if (i != null)
            {
                if (i instanceof Init.IntInit)
                {
                    Init.IntInit ii = (Init.IntInit)i;
                    if (ii.getValue() != 0)
                    {
                        if (shift != 0)
                            os.printf("|(%d << %d)", ii.getValue(), shift);
                        else
                            os.printf("|(%d)", ii.getValue());
                    }
                    return;
                }
            }
        }
        else if (value instanceof Init.IntInit)
        {
            Init.IntInit ii = (Init.IntInit)value;
            if (ii.getValue() != 0)
            {
                if (shift != 0)
                    os.printf("|(%d << %d)", ii.getValue(), shift);
                else
                    os.printf("|(%d)", ii.getValue());
            }
            return;
        }

        System.err.println("Unhandled initializer: " + val.toString());
        throw new Exception("In record '" + r.getName() + "' for TSFlag emission.");
    }

    private int getItinClassNumber(Record record) throws Exception
    {
        return itinClassMap.get(record.getValueAsDef("Itinerary"));
    }

    private void emitOperandInfo(PrintStream os,
            TObjectIntHashMap<ArrayList<String>> operandInfoIDs)
            throws Exception
    {
        int operandListNum = 0;
        operandInfoIDs.put(new ArrayList<>(), ++operandListNum);

        os.println();

        CodeGenTarget target = cdp.getTarget();
        for (Map.Entry<String, CodeGenInstruction> pair : target.getInstructions().entrySet())
        {
            ArrayList<String> operandInfo = getOperandInfo(pair.getValue());
            if (operandInfoIDs.containsKey(operandInfo))
                continue;

            operandInfoIDs.put(operandInfo, ++operandListNum);

            os.printf("\n\tpublic static final TargetOperandInfo[] operandInfo = {\n");
            int e = operandInfo.size();
            if (e > 0)
            {
                os.printf("\t\tnew TargetOperandInfo(%s)", operandInfo.get(0));
                for (int i = 1; i < e; i++)
                    os.printf(",\n\t\tnew TargetOperandInfo(%s)", operandInfo.get(i));
            }
            os.printf("\n\t};\n");
        }
    }

    private ArrayList<String> getOperandInfo(CodeGenInstruction instr)
            throws Exception
    {
        ArrayList<String> result = new ArrayList<>();

        for (int i = 0, e = instr.operandList.size(); i != e; i++)
        {
            ArrayList<CodeGenInstruction.OperandInfo> operandList = new ArrayList<>();

            Init.DagInit dag = instr.operandList.get(i).miOperandInfo;
            if (dag == null || dag.getNumArgs() == 0)
            {
                // Single, anonymous operand.
                operandList.add(instr.operandList.get(i));
            }
            else
            {
                for (int j = 0, ee = (int)instr.operandList.get(i).miNumOperands; j != ee; ++j)
                {
                    CodeGenInstruction.OperandInfo info = instr.operandList.get(i);
                    info.rec = ((Init.DefInit)dag.getArg(j)).getDef();
                    operandList.add(info);
                }
            }

            for (int j = 0, ee = operandList.size(); j != ee; ++j)
            {
                Record opr = operandList.get(j).rec;
                StringBuilder res = new StringBuilder();

                if (opr.isSubClassOf("RegisterClass"))
                    res.append(opr.getName()).append("RegClassID, ");
                else if (opr.isSubClassOf("PointerLikeRegClass"))
                    res.append(opr.getValueAsInt("RegClassKind"));
                else
                    res.append("null, ");

                res.append("null");

                if (opr.isSubClassOf("PointerLikeRegClass"))
                    res.append("|(1 << TOI.LookupPtrRegClass)");

                if (instr.operandList.get(i).rec.isSubClassOf("PredicateOperand"))
                    res.append("|(1<<TOI.Predicate)");

                if (instr.operandList.get(i).rec.isSubClassOf("OptionalDefOperand"))
                    res.append("|(1<<TOI.OptionalDef)");

                res.append(", ").append(instr.operandList.get(i).constraints.get(j));
                result.add(res.toString());
            }
        }

        return result;
    }

    private void detectRegisterClassBarriers(ArrayList<Record> defs,
            ArrayList<CodeGenRegisterClass> rcs, ArrayList<Record> rcBarriers)
    {
        HashSet<Record> defSet = new HashSet<>(defs);

        int numDefs = defs.size();

        for (CodeGenRegisterClass rc : rcs)
        {
            int numRegs = rc.elts.size();
            if (numRegs > numDefs)
                continue;

            boolean clobber = true;
            for (Record reg : rc.elts)
            {
                if (!defSet.contains(reg))
                {
                    clobber = false;
                    break;
                }
            }
            if (clobber)
                rcBarriers.add(rc.theDef);
        }
    }

    private static void printDefList(ArrayList<Record> uses, int num, PrintStream os)
    {
        os.printf("\tpublic static final int[] implicitList%d = { ", num);
        int e = uses.size();
        if (e > 0)
        {
            os.printf("%s", uses.get(0).getName());
            for (int i = 1 ; i != e; ++i)
            {
                os.printf(", %s", uses.get(i).getName());
            }
        }
        os.printf("};\n");
    }

    private static void printBarriers(ArrayList<Record> barriers, int num, PrintStream os)
    {
        os.printf("\tpublic static final TargetRegisterClass[] barriers = { ");
        int e = barriers.size();
        if (e > 0)
        {
            os.printf("%sRegClass", barriers.get(0).getName());
            for(int i = 1; i != e; ++i)
            {
                os.printf(", %sRegClass", barriers.get(i).getName());
            }
        }
        os.printf("};\n");
    }

    private void gatherItinClasses()
    {
        try
        {
            ArrayList<Record> defList = records.getAllDerivedDefinition("InstrItinClass");
            defList.sort(new Comparator<Record>()
            {
                @Override public int compare(Record o1, Record o2)
                {
                    return o1.getName().compareTo(o2.getName());
                }
            });

            for (int i = 0, e = defList.size(); i != e; ++i)
                itinClassMap.put(defList.get(i).getName(), i);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Emit the instruction enumeration to [TargetName]InstrNames.java.
     * @param outputFile
     */
    public void runEnums(String outputFile)
    {
        try(PrintStream os = outputFile.equals("-") ? System.out:
                new PrintStream(new FileOutputStream(outputFile)))
        {
            emitSourceFileHeaderComment("Target Instruction Enum Values", os);;

            CodeGenTarget target = cdp.getTarget();

            ArrayList<CodeGenInstruction> numberedInstrs = new ArrayList<>();
            target.getInstructionsByEnumValue(numberedInstrs);;
            String className = target.getName() + "InstrNames";

            os.printf("public interface %s\n{\n", className);

            for (int i = 0, e = numberedInstrs.size(); i != e; i++)
            {
                os.printf("\tint %s = %d;\n",
                        numberedInstrs.get(i).theDef.getName(), i);
            }

            os.printf("\tint INSTRUCTION_LIST_END = %d;\n", numberedInstrs.size());

            os.printf("\n}");
        }
        catch (FileNotFoundException e)
        {
            System.err.printf("File %s dose not exist", outputFile);
            System.exit(1);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}