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
 * Definitions of all of the base types for the Type system.  Based on this
 * value, you can cast to a "DerivedType" subclass (see DerivedTypes.h)
 * Note: If you add an element to this, you need to add an element to the
 * Type::getPrimitiveType function, or else things will break!
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public interface PrimitiveID
{
    int VoidTyID = 0;           //  0, 1: Basics...
    int Int1TyID = VoidTyID + 1;
    int Int8TyID = Int1TyID + 1;          //  2, 3: 8 bit types...
    int Int16TyID = Int8TyID + 1;         //  4, 5: 16 bit types...

    int Int32TyID = Int16TyID + 1;           //  6, 7: 32 bit types...
    int Int64TyID = Int32TyID + 1;          //  8, 9: 64 bit types...

    int FloatTyID = Int64TyID + 1;         // 10,11: Floating point types...
    int DoubleTyID = FloatTyID + 1;

    int TypeTyID = DoubleTyID + 1;
    int LabelTyID = TypeTyID + 1;         // 12   : Labels...

    // Derived types... see DerivedTypes class...
    // Make sure FirstDerivedTyID stays up to date!!!
    int FunctionTyID = LabelTyID + 1;       // Functions... Structs...
    int StructTyID = FunctionTyID + 1;
    int ArrayTyID = StructTyID + 1;          // Array... pointer...
    int PointerTyID = ArrayTyID + 1;
    //...

    int NumPrimitiveIDs = PointerTyID + 1;   // Must remain as last defined ID

    int LastPrimitiveTyID = LabelTyID;
    int FirstDerivedTyID = FunctionTyID;
}
