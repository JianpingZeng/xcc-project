package backend.target;
/*
 * Extremely C language Compiler
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

/**
 * MCInstrDesc flags - These should be considered private to the
 * implementation of the MCInstrDesc class.  Clients should use the
 * predicate methods on MCInstrDesc, not use these directly.  These
 * all correspond to bitfields in the MCInstrDesc::Flags field.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public interface TID {
  int Return = 0;
  int Branch = 1;
  int IndirectBranch = 2;
  int Compare = 3;
  int MoveImm = 4;
  int Bitcast = 5;
  int Barrier = 6;
  int Call = 7;
  int FoldAsLoad = 8;
  int Predicable = 9;
  int MayLoad = 10;
  int MayStore = 11;
  int ConvertibleToThreeAddress = 12;
  int Commutable = 13;
  int Terminator = 14;
  int ReMaterializable = 15;
  int DelaySlot = 16;
  int UsesCustomInserter = 17;
  int PostISelHook = 18;
  int CtrlDep = 19;
  int NotDuplicable = 20;
  int SideEffects = 21;
  int CheapAsAMove = 22;
  int ExtraSrcRegAllocReq = 23; // Sources have special regalloc requirement?
  int ExtraDefRegAllocReq = 24;
  int Pseudo = 25;
  int OptionalDef = 26;
  int Variadic = 27;
  int CodeGenOnly = 28;
}
