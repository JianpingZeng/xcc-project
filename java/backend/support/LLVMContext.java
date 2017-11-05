/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package backend.support;

import backend.type.IntegerType;
import backend.type.LLVMTypeID;
import backend.type.Type;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class LLVMContext
{
    public static final Type VoidTy = new Type(LLVMTypeID.VoidTyID);
    public static final Type LabelTy = new Type(LLVMTypeID.LabelTyID);
    public static final IntegerType Int1Ty = IntegerType.get(1);
    public static final IntegerType Int8Ty = IntegerType.get(8);
    public static final IntegerType Int16Ty = IntegerType.get(16);
    public static final IntegerType Int32Ty = IntegerType.get(32);
    public static final IntegerType Int64Ty = IntegerType.get(64);
    public static final Type FloatTy = new Type(LLVMTypeID.FloatTyID);
    public static final Type DoubleTy = new Type(LLVMTypeID.DoubleTyID);
    public static final Type FP128Ty = new Type(LLVMTypeID.FP128TyID);
    public static final Type X86_FP80Ty = new Type(LLVMTypeID.X86_FP80TyID);
}
