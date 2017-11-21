package backend.target;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
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
 * TargetInstrDesc flags - These should be considered private to the
 * implementation of the TargetInstrDesc class.  Clients should use the
 * predicate methods on TargetInstrDesc, not use these directly.  These
 * all correspond to bitfields in the TargetInstrDesc::Flags field.
 * @author Xlous.zeng
 * @version 0.1
 */
public interface TID
{
    int Variadic = 0;
    int hasOptionalDef = 1;
    int Return = 2;
    int Call = 3;
    int Barrier = 4;
    int Terminator = 5;
    int Branch = 6;
    int IndirectBranch = 7;
    int Predicable = 8;
    int NotDuplicable = 9;
    int DelaySlot = 10;
    int FoldableAsLoad = 11;
    int MayLoad = 12;
    int MayStore = 13;
    int UnmodelSideEffects = 14;
    int Commutable = 15;
    int ConvertibleTo3Addr = 16;
    int UsesCustomDAGSchedInserter = 17;
    int Rematerializable = 18;
    int CheapAsAMove = 19;
}
