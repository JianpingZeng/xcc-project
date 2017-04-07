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

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class OtherType extends Type
{
    private static final OtherType FloatTy = new OtherType("float", Type.FloatTyID);
    private static final OtherType DoubleTy = new OtherType("double", Type.DoubleTyID);
    private static final OtherType LabelTy = new OtherType("label", Type.LabelTyID);

    private OtherType(String name, int primitiveID)
    {
        super(name, primitiveID);
    }

    public static OtherType getFloatType(int numBits)
    {
        assert numBits == 32 || numBits == 64;
        return numBits == 32 ? FloatTy: DoubleTy;
    }

    public static OtherType getVoidType() { return VoidTy; }

    public static OtherType getLabelType() { return LabelTy; }
}
