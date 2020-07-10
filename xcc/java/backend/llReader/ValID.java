package backend.llReader;
/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2020, Jianping Zeng.
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
import backend.value.MDNode;
import backend.value.MDString;
import tools.APFloat;
import tools.APSInt;
import tools.SourceMgr;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public final class ValID {
  /// ValID - Represents a reference of a definition of some sort with no type.
  /// There are several cases where we have to parse the value but where the
  /// type can depend on later context.  This may either be a numeric reference
  /// or a symbolic (%var) reference.  This is just a discriminated union.

  enum ValIDKind {
    t_LocalID, t_GlobalID,      // ID in UIntVal.
    t_LocalName, t_GlobalName,  // Name in StrVal.
    t_APSInt, t_APFloat,        // Value in APSIntVal/APFloatVal.
    t_Null, t_Undef, t_Zero,    // No value.
    t_EmptyArray,               // No value:  []
    t_Constant,                 // Value in ConstantVal.
    t_InlineAsm,                // Value in StrVal/StrVal2/UIntVal.
    t_MDNode,                   // Value in MDNodeVal.
    t_MDString,                 // Value in MDStringVal.
    t_ConstantStruct,           // Value in ConstantStructElts.
    t_PackedConstantStruct      // Value in ConstantStructElts.
  }

  ValIDKind kind;
  SourceMgr.SMLoc loc;
  int intVal;
  String strVal, strVal2;
  APSInt apsIntVal;
  APFloat apFloatVal;
  Constant constantVal;
  MDNode mdNodeVal;
  MDString mdStringVal;
  Constant[] constantStructElts;

  ValID() {
    apFloatVal = new APFloat(0.0f);
  }
}
