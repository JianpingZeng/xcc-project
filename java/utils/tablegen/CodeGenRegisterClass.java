package utils.tablegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous
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

import tools.Util;
import backend.codegen.MVT;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public final class CodeGenRegisterClass
{
    Record theDef;
    ArrayList<Record> elts;
    TIntArrayList vts;
    long spillSize, spillAlignment;
    String methodBodies;
    long copyCost;
    ArrayList<Record> subRegClasses;

    public String getName()
    {
        return  theDef.getName();
    }

    public TIntArrayList getValueTypes()
    {
        return vts;
    }

    public int getNumValueTypes()
    {
        return vts.size();
    }

    public int getValueTypeAt(int idx)
    {
        Util.assertion( idx >= 0 && idx < vts.size());
        return vts.get(idx);
    }

    private static int anonCnter = 0;

    public CodeGenRegisterClass(Record r) throws Exception
    {
        elts = new ArrayList<>();
        vts = new TIntArrayList();
        subRegClasses = new ArrayList<>();

        theDef = r;

        // Rename the anonymous register class.
        if (r.getName().length() > 9 && r.getName().charAt(9)=='.')
            r.setName("AnonRegClass_"+(anonCnter++));

        ArrayList<Record> typeList = r.getValueAsListOfDefs("RegTypes");
        for (Record ty : typeList)
        {
            if (!ty.isSubClassOf("ValueType"))
                throw new Exception("RegTypes list member '" + ty.getName()
                        + "' does not derive from the ValueType class!");
            vts.add(getValueType(ty, null));
        }

        Util.assertion( !vts.isEmpty());

        ArrayList<Record> regList = r.getValueAsListOfDefs("MemberList");
        for (Record reg : regList)
        {
            if (!reg.isSubClassOf("Register"))
                throw new Exception("Register Class member '" + reg.getName() +
                        "' does not derive from the Register class!");
            elts.add(reg);
        }

        // Obtains the information about SubRegisterClassList.
        ArrayList<Record> subRegClassList = r.getValueAsListOfDefs("SubRegClassList");

        for (Record subReg : subRegClassList)
        {
            if (!subReg.isSubClassOf("RegisterClass"))
                throw new Exception("Register class member '" + subReg.getName()
                + "' doest not derive from the RegisterClass class!");

            subRegClasses.add(subReg);
        }

        // Allow targets to override the getNumOfSubLoop in bits of the RegisterClass.
        long size = r.getValueAsInt("Size");
        spillSize = size != 0 ? size : new MVT(vts.get(0)).getSizeInBits();
        spillAlignment = r.getValueAsInt("Alignment");
        copyCost = r.getValueAsInt("CopyCost");
        methodBodies = r.getValueAsCode("MethodBodies");
    }

    private static int getValueType(Record rec, CodeGenTarget cgt)
            throws Exception
    {
        long val = rec.getValueAsInt("Value");
        return (int)val;
        /**
        if (vt == MVT.SimpleValueType.isPtr)
        {
            Util.assertion(cgt!= null, "Use a pointer type in a place that isn't supported as yet!");;
            vt = cgt.getPointerType();
        }
         */
    }
}
