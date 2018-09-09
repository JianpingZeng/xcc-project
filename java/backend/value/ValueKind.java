package backend.value;
/*
 * Extremely C language CompilerInstance
 * Copyright (c) 2015-2018; Xlous
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
 *
 * @author Jianping Zeng
 * @version 0.1
 */
public class ValueKind {
  public static int TypeVal = 0;
  public static int ArgumentVal = TypeVal + 1;              // This is an instance of Argument
  public static int BasicBlockVal = ArgumentVal + 1;            // This is an instance of BasicBlock
  public static int FunctionVal = BasicBlockVal + 2;              // This is an instance of FunctionProto
  public static int GlobalVariableVal = FunctionVal + 1;        // This is an instance of GlobalVariable
  public static int UndefValueVal = GlobalVariableVal + 1;            // This is an instance of UndefValue
  public static int ConstantExprVal = UndefValueVal + 1;          // This is an instance of ConstantExpr
  public static int ConstantAggregateZeroVal = ConstantExprVal + 1; // This is an instance of ConstantAggregateZero
  public static int ConstantIntVal = ConstantAggregateZeroVal + 1;           // This is an instance of ConstantInt
  public static int ConstantFPVal = ConstantIntVal + 1;            // This is an instance of ConstantFP
  public static int ConstantArrayVal = ConstantFPVal + 1;         // This is an instance of ConstantArray
  public static int ConstantStructVal = ConstantArrayVal + 1;        // This is an instance of ConstantStruct
  public static int ConstantVectorVal = ConstantStructVal + 1;
  public static int ConstantPointerNullVal = ConstantVectorVal + 1;   // This is an instance of ConstantPointerNull
  public static int MDNodeVal = ConstantPointerNullVal + 1;           // This is an instance of MDNode
  public static int MDStringVal = MDNodeVal + 1;                      // This is an instance of MDString
  public static int NamedMDNodeVal = MDStringVal + 1;                 // This is an instance of NamedMDNode
  public static int InlineAsmVal = NamedMDNodeVal + 1;                // This is an instance of InlineAsm
  public static int PseudoSourceValueVal = InlineAsmVal + 1;          // This is an instance of PseudoSourceValue
  public static int InstructionVal = PseudoSourceValueVal + 1;           // This is an instance of Instruction
  // Enum values starting at InstructionVal are used for Instructions;
  // don't add new values here!

  // Markers:
  public static int ConstantFirstVal = FunctionVal;
  public static int ConstantLastVal = ConstantPointerNullVal;
}
