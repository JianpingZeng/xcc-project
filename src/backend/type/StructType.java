package backend.type;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2016, Xlous
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

import backend.value.Constant;
import backend.value.ConstantInt;
import backend.value.Value;
import tools.TypeMap;

import java.util.ArrayList;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class StructType extends CompositeType
{

    static class StructValType
    {
        ArrayList<Type> elemTypes;

        StructValType(ArrayList<Type> args)
        {
            elemTypes = new ArrayList<>(args.size());
            for (Type t : args)
                elemTypes.add(t);
        }
    }
    /**
     * An array of struct member type.
     */
    private ArrayList<Type> elementTypes;
    private static TypeMap<StructValType, StructType> structTypes;

    /**
     * A place holder type.
     */
    private static StructType PlaceHolderType = new StructType(null);
    static
    {
        structTypes = new TypeMap<>();
    }

    protected StructType(ArrayList<Type> memberTypes)
    {
        super(StructTyID);
        elementTypes = new ArrayList<>(memberTypes.size());
        for (Type t : memberTypes)
            elementTypes.add(t);
    }

    public static StructType get(ArrayList<Type> memberTypes)
    {
        StructValType svt = new StructValType(memberTypes);
        StructType st = structTypes.get(svt);
        if (st != null)
            return st;
        structTypes.put(svt, st);
        return st;
    }

    public static StructType get()
    {
        return PlaceHolderType;
    }

    public ArrayList<Type> getElementTypes() { return elementTypes;}

    @Override
    public Type getTypeAtIndex(Value v)
    {
        assert v instanceof Constant;
        assert v.getType() == Type.Int32Ty;
        int idx = (int)((ConstantInt)v).getZExtValue();
        assert idx < elementTypes.size();
        assert indexValid(v);
        return elementTypes.get(idx);
    }

    @Override
    public boolean indexValid(Value v)
    {
        if (!(v instanceof Constant)) return false;
        if (v.getType() != Type.Int32Ty) return false;
        int idx = (int)((ConstantInt)v).getZExtValue();

        return idx < elementTypes.size();
    }

    @Override
    public Type getIndexType() {return Type.Int32Ty;}

    public int getNumOfElements() { return elementTypes.size();}

    public Type getElementType(int idx){return elementTypes.get(idx);}
}
