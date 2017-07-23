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

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static backend.codegen.MVT.Other;

/**
 * This class corresponds to the Target class in .td file.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class CodeGenTarget
{
    private Record targetRec;
    private ArrayList<Record> calleeSavedRegisters;
    private int pointerType;


    private HashMap<String, CodeGenInstruction> insts;
    private ArrayList<CodeGenRegister> registers;
    private ArrayList<CodeGenRegisterClass> registerClasses;
    private TIntArrayList legalValueTypes;

    public CodeGenTarget() throws Exception
    {
        pointerType = Other;
        legalValueTypes = new TIntArrayList();

        ArrayList<Record> targets = Record.records.getAllDerivedDefinition("Target");
        if (targets.isEmpty())
            throw new Exception("Error: No target defined!");
        if (targets.size() != 1)
            throw new Exception("Error: Multiple subclasses of Target defined!");

        targetRec = targets.get(0);

        // LLVM 1.0 introduced which is removed in LLVM 2.6.
        // calleeSavedRegisters = targetRec.getValueAsListOfDefs("CalleeSavedRegisters");
        // pointerType = getValueType(targetRec.getValueAsDef("PointerType"));

        readRegisters();

        // Read register classes description information from records.
        readRegisterClasses();

        // Read the instruction description information from records.
        // readInstructions();
    }

    private void readRegisters() throws Exception
    {
        ArrayList<Record> regs = Record.records.getAllDerivedDefinition("Register");
        if (regs.isEmpty())
            throw new Exception("No 'Register' subclasses defined!");
        registers = new ArrayList<>();
        regs.forEach(reg->
        {
            try
            {
                registers.add(new CodeGenRegister(reg));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        });
    }

    private void readRegisterClasses() throws Exception
    {
        ArrayList<Record> regClasses = Record.records.getAllDerivedDefinition("RegisterClass");
        if (regClasses.isEmpty())
            throw new Exception("No 'RegisterClass subclass defined!");
        registerClasses = new ArrayList<>();
        regClasses.forEach(regKls ->
        {
            try
            {
                registerClasses.add(new CodeGenRegisterClass(regKls));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        });
    }

    private void readInstructions() throws Exception
    {
        ArrayList<Record> instrs = Record.records.getAllDerivedDefinition("Instruction");
        if (instrs.size() <= 2)
            throw new Exception("No 'Instruction' subclasses defined!");

        String instFormatName = getAsmWriter().getValueAsString("InstFormatName");
        insts = new HashMap<>();
        for (Record inst : instrs)
        {
            String asmStr = inst.getValueAsString(instFormatName);
            insts.put(inst.getName(), new CodeGenInstruction(inst, asmStr));

        }
    }

    public static int AsmWriterNum = 0;

    public Record getAsmWriter() throws Exception
    {
        ArrayList<Record> li = targetRec.getValueAsListOfDefs("AssemblyWriters");
        if (AsmWriterNum >= li.size())
            throw  new Exception("Target does not have an AsmWriter #" + AsmWriterNum + "!");
        return li.get(AsmWriterNum);
    }

    public Record getTargetRecord()
    {
        return targetRec;
    }

    public String getName()
    {
        return targetRec.getName();
    }

    public ArrayList<Record> getCalleeSavedRegisters()
    {
        return calleeSavedRegisters;
    }

    public int getPointerType()
    {
        return pointerType;
    }

    Record getInstructionSet() throws Exception
    {
        return targetRec.getValueAsDef("InstructionSet");
    }

    public ArrayList<CodeGenRegisterClass> getRegisterClasses()
    {
        return registerClasses;
    }

    public ArrayList<CodeGenRegister> getRegisters()
    {
        return registers;
    }

    public HashMap<String, CodeGenInstruction> getInstructions()
            throws Exception
    {
        if (insts == null || insts.isEmpty())
            readInstructions();
        return insts;
    }

    /**
     * Return all of the X86Insts defined by the target, ordered by their
     * enum value.
     * @param numberedInstructions
     */
    public void getInstructionsByEnumValue(
            ArrayList<CodeGenInstruction> numberedInstructions)
            throws Exception
    {
        if (!insts.containsKey("PHI"))
            throw new Exception("Could not find 'PHI' instruction");
        CodeGenInstruction phi = insts.get("PHI");

        if (!insts.containsKey("INLINEASM"))
            throw new Exception("Could not find 'INLINEASM instruction'");

        CodeGenInstruction inlineAsm = insts.get("INLINEASM");

        // Print out the rest of the X86Insts set.
        numberedInstructions.add(phi);
        numberedInstructions.add(inlineAsm);
        insts.entrySet().forEach(entry->
        {
            CodeGenInstruction inst =  entry.getValue();
            if (inst != phi && inst != inlineAsm)
                numberedInstructions.add(inst);
        });
    }

    public CodeGenInstruction getInstruction(String name) throws Exception
    {
        insts = getInstructions();
        assert insts.containsKey(name):"Not an instruction!";
        return insts.get(name);
    }

    /**
     * Return the MVT::SimpleValueType that the specified TableGen
     * record corresponds to.
     * @param rec
     * @return
     * @throws Exception
     */
    public static int getValueType(Record rec) throws Exception
    {
        return (int) rec.getValueAsInt("Value");
    }

    public void readLegalValueTypes()
    {
        ArrayList<CodeGenRegisterClass> rcs = getRegisterClasses();
        for (CodeGenRegisterClass rc : rcs)
        {
            for (int i = 0, e = rc.vts.size(); i != e; i++)
                legalValueTypes.add(rc.vts.get(i));
        }

        // Remove duplicates.
        HashSet<Integer> set = new HashSet<>();
        for (int i = 0; i != legalValueTypes.size(); i++)
            set.add(legalValueTypes.get(i));

        legalValueTypes.clear();
        legalValueTypes.addAll(set);
    }

    public TIntArrayList getLegalValueTypes()
    {
        if (legalValueTypes.isEmpty()) readLegalValueTypes();

        return legalValueTypes;
    }

    public CodeGenRegisterClass getRegisterClass(Record r)
    {
        for (CodeGenRegisterClass rc : registerClasses)
            if (rc.theDef.equals(r))
                return rc;

        assert false:"Didn't find the register class!";
        return null;
    }

    /**
     * Find the register class that contains the
     * specified physical register.  If the register is not in a register
     * class, return null. If the register is in multiple classes, and the
     * classes have a superset-subset relationship and the same set of
     * types, return the superclass.  Otherwise return null.
     * @param r
     * @return
     */
    public CodeGenRegisterClass getRegisterClassForRegister(Record r)
    {
        ArrayList<CodeGenRegisterClass> rcs = getRegisterClasses();
        CodeGenRegisterClass foundRC = null;
        for (int i = 0, e = rcs.size(); i != e; ++i)
        {
            CodeGenRegisterClass rc = registerClasses.get(i);
            for (int ei = 0, ee = rc.elts.size(); ei != ee; ++ei)
            {
                if (r != rc.elts.get(ei))
                    continue;

                // If a register's classes have different types, return null.
                if (foundRC != null && !rc.getValueTypes().equals(foundRC.getValueTypes()))
                    return null;

                // If this is the first class that contains the register,
                // make a note of it and go on to the next class.
                if (foundRC == null)
                {
                    foundRC = rc;
                    break;
                }

                ArrayList<Record> elements = new ArrayList<>();
                elements.addAll(rc.elts);

                ArrayList<Record> foundElements = new ArrayList<>();
                foundElements.addAll(foundRC.elts);

                // Check to see if the previously found class that contains
                // the register is a subclass of the current class. If so,
                // prefer the superclass.
                if(elements.containsAll(foundElements))
                {
                    foundRC = rc;
                    break;
                }

                // Check to see if the previously found class that contains
                // the register is a superclass of the current class. If so,
                // prefer the superclass.
                if (foundElements.containsAll(elements))
                    break;

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
     * @param r
     * @return
     */
    public TIntArrayList getRegisterVTs(Record r)
    {
        TIntArrayList res = new TIntArrayList();
        for (CodeGenRegisterClass rc : registerClasses)
        {
            for (Record elt : rc.elts)
            {
                if (r.equals(elt))
                {
                    TIntArrayList inVTs = rc.getValueTypes();
                    res.addAll(CodeGenDAGPatterns.convertVTs(inVTs));
                }
            }
        }
        return res;
    }
}
