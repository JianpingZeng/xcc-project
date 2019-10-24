package backend.support;

/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng
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
public enum CallingConv {
  /**
   * The default llvm calling convention, compatible with C.  This
   * convention is the only calling convention that supports varargs calls.
   * As with typical C calling conventions, the callee/caller have to
   * tolerate certain amounts of prototype mismatch.
   */
  C(0),

  /***
   * Generic LLVM calling conventions.  None of these calling conventions
   * support varargs calls, and all assume that the caller and callee
   * prototype exactly match.
   * Fast - This calling convention attempts to make calls as fast as
   * possible (e.g. by passing things in registers).
   */
  Fast(8),

  /**
   * This calling convention attempts to make code in the caller as
   * efficient as possible under the assumption that the call is not commonly
   * executed.  As such, these calls often preserve all registers so that the
   * call does not break any live ranges in the caller side.
   */
  Cold(9),

  /**
   * stdcall is the calling conventions mostly used by the
   * Win32 API. It is basically the same as the C convention with the
   * difference in that the callee is responsible for popping the arguments
   * from the stack.
   */
  X86_StdCall(64),

  /**
   * 'fast' analog of X86_StdCall. Passes first two arguments
   * in ECX:EDX registers, others - via stack. Callee is responsible for
   * stack cleaning.
   */
  X86_FastCall(65),

  /**
   * ARM Procedure Calling Standard calling convention (obsolete,
   * but still used on some targets).
   */
  ARM_APCS(66),

  /**
   * ARM Architecture Procedure Calling Standard calling
   * convention (aka EABI). Soft float variant.
   */
  ARM_AAPCS(67),

  /**
   * Same as ARM_AAPCS, but uses hard floating point ABI.
   */
  ARM_AAPCS_VFP(68);

  public final int enumValue;
  CallingConv(int id) {
    enumValue = id;
  }
}
