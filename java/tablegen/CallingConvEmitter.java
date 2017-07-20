package utils.tablegen;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
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
import tools.Util;
import utils.tablegen.Init.ListInit;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import static utils.tablegen.CodeGenTarget.getValueType;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class CallingConvEmitter extends TableGenBackend
{
    private RecordKeeper records;
    private int counter;

    public CallingConvEmitter(RecordKeeper r)
    {
        records = r;
    }

    /**
     * Outputs the calling convention.
     * @param outputFile
     * @throws Exception
     */
    @Override
    public void run(String outputFile) throws Exception
    {
        try(PrintStream os = outputFile.equals("-") ?
                System.out :
                new PrintStream(new FileOutputStream(outputFile)))
        {
            emitSourceFileHeaderComment("Calling convetion Implementation Fragment", os);

            ArrayList<Record> ccs = records.getAllDerivedDefinition("CallingConv");
            CodeGenTarget target = new CodeGenTarget();
            String className = target.getName() + "GenCallingConv";
            os.printf("public class %s {\n", className);

            // Emit each calling convention description in full.
            for (Record cc : ccs)
            {
                emitCallingConv(cc, os);
            }
            os.println("}");
        }
    }

    private void emitCallingConv(Record cc, PrintStream os) throws Exception
    {
        ListInit ccActions = cc.getValueAsListInit("Actions");
        counter = 0;
        os.printf("\n\tpublic static bool %s(int valNo, EVT valVT,\n", cc.getName());
        os.printf(Util.fixedLengthString(cc.getName().length() + 13, ' '));
        os.printf("EVT locVT, CCValAssign.LocInfo locInfo,\n");
        os.printf(Util.fixedLengthString(cc.getName().length() + 13, ' '));
        os.printf("ArgFlagsTy argFlags, CCState state){\n");

        // Emit all of the actions, in order.
        for (int i = 0, e = ccActions.getSize(); i != e; i++)
        {
            os.println();
            emitAction(ccActions.getElementAsRecord(i), 2, os);
        }
        os.printf("\n\treturn true;\t// CC didn't match.\n");
        os.println("}\n");
    }

    private void emitAction(Record action, int indent, PrintStream os)
            throws Exception
    {
        String indentStr = Util.fixedLengthString(indent, ' ');
        if (action.isSubClassOf("CCPredicateAction"))
        {
            os.printf("%sif (", indentStr);

            if (action.isSubClassOf("CCIfType"))
            {
                ListInit vts = action.getValueAsListInit("VTs");
                for (int i = 0, e = vts.getSize(); i != e; i++)
                {
                    Record vt = vts.getElementAsRecord(i);
                    if (i != 0) os.printf(" ||\n%s%s", indentStr, indentStr);
                    os.printf("locVT == %s", MVT.getEnumName(getValueType(vt)));
                }
            }
            else if (action.isSubClassOf("CCIf"))
                os.printf(action.getValueAsString("Predicate"));
            else
            {
                action.dump();
                throw new Exception("Unknown CCPredicateAction!");
            }

            os.printf(") {\n");
            emitAction(action.getValueAsDef("SubAction"), indent + 2, os);
            os.printf("%s}\n", indentStr);
        }
        else
        {
            if (action.isSubClassOf("CCDelegateTo"))
            {
                Record cc = action.getValueAsDef("CC");
                os.printf("%sif (!%s(valNo, valVT, locVT, locInfo, argFlags, state))\n",
                        indentStr, cc.getName());
                os.printf("%s\treturn false;\n", indentStr);
            }
            else if (action.isSubClassOf("CCAssignToReg"))
            {
                ListInit regList = action.getValueAsListInit("RegList");
                if (regList.getSize() == 1)
                {
                    os.printf("%sint[] regList = {\n", indentStr);
                    os.printf(indentStr + ", ");
                    for (int i = 0, e = regList.getSize(); i != e; i++)
                    {
                        if (i != 0) os.printf(", ");
                        os.printf(regList.getElementAsRecord(i).getName());
                    }
                    os.println();
                    os.printf("%s};\n", indentStr);
                    os.printf("%sint reg = state.allocateReg(regList%d, %d));\n",
                            indentStr, counter, regList.getSize());
                    os.printf("%sif (reg != 0) {\n", indentStr);
                }
                os.printf("%s\tstate.addLoc(CCValAssign.getReg(valNo, valVT, reg, locVT, locInfo));\n", indentStr);
                os.printf("%s\treturn false;\n", indentStr);
                os.printf("%s}\n", indentStr);
            }
            else if (action.isSubClassOf("CCAssignToRegWithShadow"))
            {
                ListInit regList = action.getValueAsListInit("RegList");
                ListInit shadowRegList = action.getValueAsListInit("ShadowRegList");
                if (shadowRegList.getSize() > 0 && shadowRegList.getSize() != regList.getSize())
                {
                    throw new Exception("Invalid length of list of shadowed registers");
                }

                if (regList.getSize() == 1)
                {
                    os.printf("%sint reg = state.allocateReg(%s, %s);\n",
                            indentStr,
                            regList.getElementAsRecord(0).getName(),
                            shadowRegList.getElementAsRecord(0).getName());
                    os.printf("%sif (reg != 0) {\n", indentStr);
                }
                else
                {
                    int regListNumber = ++counter;
                    int shadowRegListNumber = ++counter;

                    os.printf("%sint[] regList%d = {\n", indentStr, regListNumber);
                    os.printf(indentStr + "\t");
                    for (int i = 0, e = regList.getSize(); i != e; i++)
                    {
                        if (i != 0) os.printf(", ");
                        os.printf(regList.getElementAsRecord(i).getName());
                    }

                    os.println();
                    os.printf("%s};\n", indentStr);

                    os.printf("%sint[] regList%d = {\n", indentStr, shadowRegListNumber);
                    os.printf(indentStr + '\t');
                    for (int i = 0, e = shadowRegList.getSize(); i != e; i++)
                    {
                        if (i != 0) os.print(", ");
                        os.printf(shadowRegList.getElementAsRecord(i).getName());
                    }

                    os.println();
                    os.printf("%s};\n", indentStr);

                    os.printf("%sint reg = state.allocateReg(regList%d, regList%d, %d);\n",
                            indentStr, regListNumber, shadowRegListNumber, regList.getSize());
                    os.printf("%sif (reg != 0) {\n", indentStr);
                }

                os.print(indentStr);
                os.printf("\tstate.addLoc(CCValAssign.getReg(valNo, valVT, reg, locVT, locInfo));\n");
                os.printf("%s\treturn false;\n", indentStr);
                os.printf("%s}\n", indentStr);
            }
            else if (action.isSubClassOf("CCAssignToStack"))
            {
                int size = (int) action.getValueAsInt("Size");
                int align = (int) action.getValueAsInt("Align");

                os.printf("%sint offset%d = state.allocateStack(", indentStr, ++counter);
                if (size != 0)
                    os.printf("%d, ", size);
                else
                    os.printf("\n%s\tstate.getTarget().getTargetData()" +
                            ".getTypeAllocSize(locVT.getTypeForEVT(state.getContext())), ",
                            indentStr);

                if (align != 0)
                    os.printf("%d, ", align);
                else
                    os.printf("\n%s\tstate.getTarget().getTargetData()" +
                                    ".getABITypeAlignment(locVT.getTypeForEVT(state.getContext()));\n",
                            indentStr);
                os.println();
                os.print(indentStr);
                os.printf("state.addLoc(CCValAssign.getMem(valNo, valVT, offset%d, locVT, locInfo));\n", counter);
                os.print(indentStr);
                os.println("return false;");
            }
            else if (action.isSubClassOf("CCPromoteToType"))
            {
                Record destTy = action.getValueAsDef("DestTy");
                os.printf("%slocVT = %s;\n", indentStr, MVT.getEnumName(getValueType(destTy)));
                os.printf("%sif (argFlags.isSExt())\n", indentStr);
                os.printf("%s%slocInfo = CCValueAssign.SExt;\n", indentStr, indentStr);
                os.printf("%selse if (argFlags.isZExt())\n", indentStr);
                os.printf("%s%slocInfo = CCValAssign.ZExt;\n", indentStr, indentStr);
                os.printf("%selse\n", indentStr);
                os.printf("%s%slocInfo = CCValAssign.AExt;\n", indentStr, indentStr);
            }
            else if (action.isSubClassOf("CCBitConvertToType"))
            {
                Record destTy = action.getValueAsDef("DestTy");
                os.printf("%slocVT = %s;\n", indentStr, MVT.getEnumName(getValueType(destTy)));
                os.printf("%slocInfo = CCValAssign.BCvt;\n", indentStr);
            }
            else if (action.isSubClassOf("CCPassIndirect"))
            {
                Record destTy = action.getValueAsDef("DestTy");
                os.printf("%slocVT = %s;\n", indentStr, MVT.getEnumName(getValueType(destTy)));
                os.printf("%slocInfo = CCValAssign.Indirect;\n", indentStr);
            }
            else if (action.isSubClassOf("CCPassByVal"))
            {
                int size = (int) action.getValueAsInt("Size");
                int align = (int)action.getValueAsInt("Align");
                os.printf("%sstate.handleByVal(valNo, valVT, locVT, locInfo, %d, %d, argFlags);\n",
                        indentStr, size, align);
                os.printf("%sreturn false;\n", indentStr);
            }
            else if (action.isSubClassOf("CCCustom"))
            {
                os.printf("%sif (%s(valNo, valVT, locVT, locInfo, argFlags, state))\n",
                        indentStr, action.getValueAsString("FuncName"));
                os.printf("%s%sreturn false;\n", indentStr, indentStr);
            }
            else
            {
                action.dump();
                throw new Exception("Unknown CCAction!");
            }
        }
    }
}
