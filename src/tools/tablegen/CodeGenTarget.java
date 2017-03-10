package tools.tablegen;
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

import backend.codegen.MVT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * This class corresponds to the Target class in .td file.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class CodeGenTarget
{
    private Record targetRec;
    private ArrayList<Record> calleeSavedRegisters;
    private MVT.ValueType pointerType;


    private HashMap<String, CodeGenInstruction> instructions;
    private ArrayList<CodeGenRegister> registers;
    private ArrayList<CodeGenRegisterClass> registerClasses;
    private ArrayList<MVT.ValueType> legalValueType;

    public CodeGenTarget() throws Exception
    {
        pointerType = MVT.ValueType.Other;

        try
        {
            ArrayList<Record> targets = Record.records.getAllDerivedDefinition("Target");
            targetRec = targets.get(0);

            calleeSavedRegisters = targetRec.getValueAsListOfDefs("CalleeSavedRegisters");
            pointerType = getValueType(targetRec.getValueAsDef("PointerType"));

            readRegisters();
        }
        catch (Exception e)
        {
            throw new Exception("ERROR: Multiple subclasses of Target defined");
        }

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
        ArrayList<Record> insts = Record.records.getAllDerivedDefinition("Instruction");
        if (insts.size() <= 2)
            throw new Exception("No 'Instruction' subclasses defined!");

        String instFormatName = getAsmWriter().getValueAsString("InstFormatName");
        instructions = new HashMap<>();
        insts.forEach(inst->
        {
            try
            {
                String asmStr = inst.getValueAsString(instFormatName);
                instructions.put(inst.getName(), new CodeGenInstruction(inst, asmStr));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        });
    }

    public static int AsmWriterNum = 0;

    public Record getAsmWriter() throws Exception
    {
        ArrayList<Record> li = targetRec.getValueAsListOfDefs("AssemblyWriters");
        if (AsmWriterNum >= li.size())
            throw  new Exception("Target does not have an AsmWriter #" + AsmWriterNum + "!");
        return li.get(AsmWriterNum);
    }

    static MVT.ValueType getValueType(Record rec) throws Exception
    {
        return MVT.ValueType.values()[rec.getValueAsInt("Value")];
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

    public MVT.ValueType getPointerType()
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
    {
        return instructions;
    }

    /**
     * Return all of the instructions defined by the target, ordered by their
     * enum value.
     * @param numberedInstructions
     */
    public void getInstructionsByEnumValue(
            LinkedList<CodeGenInstruction> numberedInstructions)
            throws Exception
    {
        if (!instructions.containsKey("PHI"))
            throw new Exception("Could not find 'PHI' instruction");
        CodeGenInstruction phi = instructions.get("PHI");

        if (instructions.containsKey("INLINEASM"))
            throw new Exception("Could not find 'INLINEASM instruction'");

        CodeGenInstruction inlineAsm = instructions.get("INLINEASM");

        // Print out the rest of the instructions set.
        numberedInstructions.add(phi);
        numberedInstructions.add(inlineAsm);
        instructions.entrySet().forEach(entry->
        {
            CodeGenInstruction inst =  entry.getValue();
            if (inst != phi && inst != inlineAsm)
                numberedInstructions.add(inst);
        });
    }
}
