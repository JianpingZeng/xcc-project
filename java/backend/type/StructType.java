package backend.type;
/*
 * Xlous C language CompilerInstance
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
        private boolean packed;
        StructValType(ArrayList<Type> args, boolean packed)
        {
            elemTypes = new ArrayList<>(args.size());
            elemTypes.addAll(args);
            this.packed = packed;
        }
    }
    /**
     * An array of struct member type.
     */
    private ArrayList<Type> elementTypes;

    private static TypeMap<StructValType, StructType> structTypes;

    private boolean packed;

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
        this(memberTypes, false);
    }

    private boolean isValidElementType(backend.type.Type elemTy)
    {
        return  !(elemTy.equals(Type.VoidTy) || elemTy.equals(Type.LabelTy));
    }

    protected StructType(ArrayList<Type> memberTypes, boolean packed)
    {
        super(StructTyID);
        elementTypes = new ArrayList<>(memberTypes.size());

        this.packed = packed;
        isAbstract = false;
        for(int i = 0, e = memberTypes.size(); i < e; i++)
        {
            assert memberTypes.get(i) != null :"<null> type for structure type!";
            assert isValidElementType(memberTypes.get(i)) :"Invalid type for structure element!";
            isAbstract |= memberTypes.get(i).isAbstract();
            this.elementTypes.add(memberTypes.get(i));
        }
        setAbstract(isAbstract);
    }

    public static StructType get(ArrayList<Type> memberTypes, boolean packed)
    {
        StructValType svt = new StructValType(memberTypes, packed);
        StructType st = structTypes.get(svt);
        if (st != null)
            return st;

        st = new StructType(memberTypes, packed);
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

    public boolean isPacked()
    {
        return packed;
    }
}
