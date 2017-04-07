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

import backend.codegen.MVT;

import java.util.ArrayList;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class CodeGenRegisterClass
{
    Record theDef;
    ArrayList<Record> elts;
    ArrayList<MVT.ValueType> vts;
    int spillSize, spillAlignment;
    String methodBodies;

    public String getName() {return  theDef.getName(); }

    public ArrayList<MVT.ValueType> getValueTypes()
    {
        return vts;
    }

    public int getNumValueTypes()
    {
        return vts.size();
    }

    public MVT.ValueType getValueTypeAt(int idx)
    {
        assert idx >= 0 && idx < vts.size();
        return vts.get(idx);
    }

    private static int anonCnter = 0;

    public CodeGenRegisterClass(Record r) throws Exception
    {
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

        assert !vts.isEmpty();

        ArrayList<Record> regList = r.getValueAsListOfDefs("MemberList");
        for (Record reg : regList)
        {
            if (!reg.isSubClassOf("Register"))
                throw new Exception("Register Class member '" + reg.getName() +
                        "' does not derive from the Register class!");
            elts.add(reg);
        }

        // Allow targets to override the getNumOfSubLoop in bits of the RegisterClass.
        int size = r.getValueAsInt("Size");
        spillSize = size != 0 ? size : MVT.getSizeInBits(vts.get(0));
        spillAlignment = r.getValueAsInt("Alignment");
        methodBodies = r.getValueAsCode("MethodBodies");
    }

    private static MVT.ValueType getValueType(Record rec, CodeGenTarget cgt)
            throws Exception
    {
        MVT.ValueType vt = MVT.ValueType.values()[rec.getValueAsInt("Value")];
        if (vt == MVT.ValueType.isPtr)
        {
            assert cgt!= null:"Use a pointer type in a place that isn't supported as yet!";;
            vt = cgt.getPointerType();
        }
        return vt;
    }
}
