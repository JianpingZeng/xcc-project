package backend.type;
/*
 * Extremely C language CompilerInstance
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

/**
 * Definitions of all of the base types for the Type system.  Based on this
 * value, you can cast to a "DerivedType" subclass (see DerivedTypes.h)
 * Note: If you add an element to this, you need to add an element to the
 * Type::getPrimitiveType function, or else things will break!
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public interface LLVMTypeID
{
    int VoidTyID = 0;           //  0, 1: Basics...
    int FloatTyID = VoidTyID + 1;         // 10,11: Floating point types...
    int DoubleTyID = FloatTyID + 1;
    int FP128TyID = DoubleTyID + 1;
    int X86_FP80TyID = FP128TyID + 1;

    int TypeTyID = X86_FP80TyID + 1;
    int LabelTyID = TypeTyID + 1;         // 12   : Labels...
    int MetadataTyID = LabelTyID + 1;     //


    // Derived types... see DerivedTypes class...
    // Make sure FirstDerivedTyID stays up to date!!!
    int IntegerTyID = MetadataTyID + 1;          // Arbitrary bit width integers.
    int FunctionTyID = IntegerTyID + 1;       // Functions... Structs...
    int StructTyID = FunctionTyID + 1;
    int ArrayTyID = StructTyID + 1;          // Array... pointer...
    int PointerTyID = ArrayTyID + 1;
    int OpaqueTyID = PointerTyID + 1;
    //...

    int NumPrimitiveIDs = OpaqueTyID + 1;   // Must remain as last defined ID

    int LastPrimitiveTyID = LabelTyID;
    int FirstDerivedTyID = IntegerTyID;
}
