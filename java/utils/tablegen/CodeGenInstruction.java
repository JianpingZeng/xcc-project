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

import utils.tablegen.Init.DagInit;
import utils.tablegen.Init.DefInit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class CodeGenInstruction
{
    /**
     * The actual record defining this instruction.
     */
    Record theDef;
    /**
     * Contents of the 'name' field.
     */
    String name;
    /**
     * The format string used to emit a .s file for the
     * instruction.
     */
    String asmString;

    static class OperandInfo
    {
        Record rec;

        String name;

        String printerMethodName;

        int miOperandNo;
        int miNumOperands;

        DagInit miOperandInfo;

        OperandInfo(Record r, String name, String printerMethodName,
                int miOperandNo, int miNumOperands, DagInit operandInfo)
        {
            rec = r;
            this.name = name;
            this.printerMethodName = printerMethodName;
            this.miOperandNo = miOperandNo;
            this.miNumOperands = miNumOperands;
            this.miOperandInfo = operandInfo;
        }
    }

    ArrayList<OperandInfo> operandList;

    // Various boolean values we track for the instruction.
    boolean isReturn;
    boolean isBranch;
    boolean isBarrier;
    boolean isCall;
    boolean isLoad;
    boolean isStore;
    boolean isTwoAddress;
    boolean isConvertibleToThreeAddress;
    boolean isCommutable;
    boolean isTerminator;
    boolean hasDelaySlot;
    boolean usesCustomDAGSchedInserter;
    boolean hasVariableNumberOfOperands;
    boolean hasCtrlDep;
    boolean noResults;

    CodeGenInstruction(Record r, String asmStr) throws Exception
    {
        theDef = r;
        name = r.getValueAsString("name");
        isReturn = r.getValueAsBit("isReturn");
        isReturn = r.getValueAsBit("isReturn");
        isBranch = r.getValueAsBit("isBranch");
        isBarrier = r.getValueAsBit("isBarrier");
        isCall = r.getValueAsBit("isCall");
        isLoad = r.getValueAsBit("isLoad");
        isStore = r.getValueAsBit("isStore");
        isTwoAddress = r.getValueAsBit("isTwoAddress");
        isConvertibleToThreeAddress = r
                .getValueAsBit("isConvertibleToThreeAddress");
        isCommutable = r.getValueAsBit("isCommutable");
        isTerminator = r.getValueAsBit("isTerminator");
        hasDelaySlot = r.getValueAsBit("hasDelaySlot");
        usesCustomDAGSchedInserter = r
                .getValueAsBit("usesCustomDAGSchedInserter");
        hasCtrlDep = r.getValueAsBit("hasCtrlDep");
        noResults = r.getValueAsBit("noResults");
        hasVariableNumberOfOperands = false;

        DagInit di = r.getValueAsDag("OperandList");

        int MIOperandNo = 0;
        HashSet<String> OperandNames = new HashSet<>();
        for (int i = 0, e = di.getNumArgs(); i != e; ++i)
        {
            if (!(di.getArg(i) instanceof DefInit))
                throw new Exception("Illegal operand for the '" + r.getName()
                        + "' instruction!");

            DefInit Arg = (DefInit) (di.getArg(i));

            Record Rec = Arg.getDef();
            String PrintMethod = "printOperand";
            int NumOps = 1;
            DagInit MIOpInfo = null;
            if (Rec.isSubClassOf("Operand"))
            {
                PrintMethod = Rec.getValueAsString("PrintMethod");
                NumOps = Rec.getValueAsInt("NumMIOperands");
                MIOpInfo = Rec.getValueAsDag("MIOperandInfo");
            }
            else if (Objects.equals(Rec.getName(), "variable_ops"))
            {
                hasVariableNumberOfOperands = true;
                continue;
            }
            else if (!Rec.isSubClassOf("RegisterClass"))
                throw new Exception("Unknown operand class '" + Rec.getName()
                        + "' in instruction '" + r.getName()
                        + "' instruction!");

            // Check that the operand has a name and that it's unique.
            if (di.getArgName(i).isEmpty())
                throw new Exception(
                        "In instruction '" + r.getName() + "', operand #" + i
                                + " has no name!");
            if (!OperandNames.add(di.getArgName(i)))
                throw new Exception(
                        "In instruction '" + r.getName() + "', operand #" + i
                                + " has the same name as a previous operand!");

            operandList.add(new OperandInfo(Rec, di.getArgName(i), PrintMethod,
                    MIOperandNo, NumOps, MIOpInfo));
            MIOperandNo += NumOps;
        }
    }

    /**
     * Return the index of the operand with the specified
     * non-empty name.  If the instruction does not have an operand with the
     * specified name, throw an exception.
     * @param name
     * @return
     * @throws Exception
     */
    int getOperandNamed(String name) throws Exception
    {
        assert !name.isEmpty() : "Cannot search for operand with no name!";
        for (int i = 0, e = operandList.size(); i != e; ++i)
            if (operandList.get(i).name.equals(name))
                return i;
        throw new Exception("Instruction '" + theDef.getName()
                + "' does not have an operand named '$" + name + "'!");
    }

}
