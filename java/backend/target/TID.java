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
    int NotDuplicate = 9;
    int DelaySlot = 10;
    int FoldableAsLoad = 11;
    int MayLoad = 12;
    int MayStore = 13;
    int UnmodeledSideEffects = 14;
    int Commutable = 15;
    int ConvertibleTo3Addr = 16;
    int UsesCustomDAGSchedInserter = 17;
    int Rematerializable = 18;
    int CheapAsAMove = 19;


    int M_NOP_FLAG = 1 << 0;
    int M_BRANCH_FLAG = 1 << 1;
    int M_CALL_FLAG = 1 << 2;
    int M_RET_FLAG = 1 << 3;
    int M_ARITH_FLAG = 1 << 4;
    int M_CC_FLAG = 1 << 6;
    int M_LOGICAL_FLAG = 1 << 6;
    int M_INT_FLAG = 1 << 7;
    int M_FLOAT_FLAG = 1 << 8;
    int M_CONDL_FLAG = 1 << 9;
    int M_LOAD_FLAG = 1 << 10;
    int M_PREFETCH_FLAG = 1 << 11;
    int M_STORE_FLAG = 1 << 12;
    int M_DUMMY_PHI_FLAG = 1 << 13;
    int M_PSEUDO_FLAG = 1 << 14;       // Pseudo instruction
    // 3-addr instructions which really work like 2-addr ones, eg. X86 add/sub
    int M_2_ADDR_FLAG = 1 << 15;
    // M_TERMINATOR_FLAG - Is this instruction part of the terminator for a basic
    // block?  Typically this is things like return and branch instructions.
    // Various passes use this to insert code into the bottom of a basic block, but
    // before control flow occurs.
    int M_TERMINATOR_FLAG = 1 << 16;
}
