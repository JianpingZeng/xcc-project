package backend.codegen.dagisel;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
 * All rights reserved.
 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public interface BuiltinOpcodes {
  byte OPC_Scope = 0;
  byte OPC_RecordNode = 1;
  byte OPC_RecordChild0 = 2;
  byte OPC_RecordChild1 = 3;
  byte OPC_RecordChild2 = 4;
  byte OPC_RecordChild3 = 5;
  byte OPC_RecordChild4 = 6;
  byte OPC_RecordChild5 = 7;
  byte OPC_RecordChild6 = 8;
  byte OPC_RecordChild7 = 9;
  byte OPC_RecordMemRef = 10;
  byte OPC_CaptureFlagInput = 11;
  byte OPC_MoveChild = 12;
  byte OPC_MoveParent = 13;
  byte OPC_CheckSame = 14;
  byte OPC_CheckPatternPredicate = 15;
  byte OPC_CheckPredicate = 16;
  byte OPC_CheckOpcode = 17;
  byte OPC_SwitchOpcode = 18;
  byte OPC_CheckType = 19;
  byte OPC_SwitchType = 20;
  byte OPC_CheckChild0Type = 21;
  byte OPC_CheckChild1Type = 22;
  byte OPC_CheckChild2Type = 23;
  byte OPC_CheckChild3Type = 24;
  byte OPC_CheckChild4Type = 25;
  byte OPC_CheckChild5Type = 26;
  byte OPC_CheckChild6Type = 27;
  byte OPC_CheckChild7Type = 28;
  byte OPC_CheckInteger = 29;
  byte OPC_CheckCondCode = 30;
  byte OPC_CheckValueType = 31;
  byte OPC_CheckComplexPat = 32;
  byte OPC_CheckAndImm = 33;
  byte OPC_CheckOrImm = 34;
  byte OPC_CheckFoldableChainNode = 35;

  byte OPC_EmitInteger = 36;
  byte OPC_EmitStringInteger = 37;
  byte OPC_EmitRegister = 38;
  byte OPC_EmitConvertToTarget = 39;
  byte OPC_EmitMergeInputChains = 40;
  byte OPC_EmitCopyToReg = 41;
  byte OPC_EmitNodeXForm = 42;
  byte OPC_EmitNode = 43;
  byte OPC_MorphNodeTo = 44;
  byte OPC_MarkFlagResults = 45;
  byte OPC_CompleteMatch = 46;
};
