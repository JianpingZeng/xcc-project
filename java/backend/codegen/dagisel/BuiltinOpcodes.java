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
  int OPC_Scope = 0;
  int OPC_RecordNode = 1;
  int OPC_RecordChild0 = 2; int OPC_RecordChild1 = 3; int OPC_RecordChild2 = 4; int OPC_RecordChild3 = 5;
  int OPC_RecordChild4 = 6; int OPC_RecordChild5 = 7; int OPC_RecordChild6 = 8; int OPC_RecordChild7 = 9;
  int OPC_RecordMemRef = 10;
  int OPC_CaptureFlagInput = 11;
  int OPC_MoveChild = 12;
  int OPC_MoveParent = 13;
  int OPC_CheckSame = 14;
  int OPC_CheckPatternPredicate = 15;
  int OPC_CheckPredicate = 16;
  int OPC_CheckOpcode = 17;
  int OPC_SwitchOpcode = 18;
  int OPC_CheckType = 19;
  int OPC_SwitchType = 20;
  int OPC_CheckChild0Type = 21; int OPC_CheckChild1Type = 22; int OPC_CheckChild2Type = 23;
  int OPC_CheckChild3Type = 24; int OPC_CheckChild4Type = 25; int OPC_CheckChild5Type = 26;
  int OPC_CheckChild6Type = 27; int OPC_CheckChild7Type = 28;
  int OPC_CheckInteger = 29;
  int OPC_CheckCondCode = 30;
  int OPC_CheckValueType = 31;
  int OPC_CheckComplexPat = 32;
  int OPC_CheckAndImm = 33; int OPC_CheckOrImm = 34;
  int OPC_CheckFoldableChainNode = 35;

  int OPC_EmitInteger = 36;
  int OPC_EmitRegister = 37;
  int OPC_EmitConvertToTarget = 38;
  int OPC_EmitMergeInputChains = 39;
  int OPC_EmitCopyToReg = 40;
  int OPC_EmitNodeXForm = 41;
  int OPC_EmitNode = 42;
  int OPC_MorphNodeTo = 43;
  int OPC_MarkFlagResults = 44;
  int OPC_CompleteMatch = 45;
};
