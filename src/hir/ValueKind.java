package hir;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2016; Xlous
 *
 * Licensed under the Apache License; Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing; software
 * distributed under the License is distributed on an "AS IS" BASIS;
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND; either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

/**
 * An enumeration for keeping track of the concrete subclass.
 * @author Xlous.zeng
 * @version 0.1
 */
public class ValueKind
{
    public static int ArgumentVal = 0;              // This is an instance of Argument
    public static int BasicBlockVal = 1;            // This is an instance of BasicBlock
    public static int FunctionVal = 2;              // This is an instance of Function
    public static int GlobalAliasVal = 3;           // This is an instance of GlobalAlias
    public static int GlobalVariableVal = 4;        // This is an instance of GlobalVariable
    public static int UndefValueVal = 5;            // This is an instance of UndefValue
    public static int BlockAddressVal = 6;          // This is an instance of BlockAddress
    public static int ConstantExprVal = 7;          // This is an instance of ConstantExpr
    public static int ConstantAggregateZeroVal = 8; // This is an instance of ConstantAggregateZero
    public static int ConstantIntVal = 9;           // This is an instance of ConstantInt
    public static int ConstantFPVal = 10;            // This is an instance of ConstantFP
    public static int ConstantArrayVal = 11;         // This is an instance of ConstantArray
    public static int ConstantStructVal = 12;        // This is an instance of ConstantStruct
    public static int ConstantVectorVal = 13;        // This is an instance of ConstantVector
    public static int ConstantPointerNullVal = 14;   // This is an instance of ConstantPointerNull
    public static int MDNodeVal = 15;                // This is an instance of MDNode
    public static int MDStringVal = 16;              // This is an instance of MDString
    public static int InlineAsmVal = 17;             // This is an instance of InlineAsm
    public static int PseudoSourceValueVal = 18;     // This is an instance of PseudoSourceValue
    public static int FixedStackPseudoSourceValueVal = 19; // This is an instance of FixedStackPseudoSourceValue
    public static int InstructionVal = 20;           // This is an instance of Instruction
    // Enum values starting at InstructionVal are used for Instructions;
    // don't add new values here!

    // Markers:
    public static int ConstantFirstVal = FunctionVal;
    public static int ConstantLastVal  = ConstantPointerNullVal;
}
