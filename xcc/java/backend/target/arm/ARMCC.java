package backend.target.arm;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
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

import tools.Util;

/**
 * Enums corresponding to ARM condition codes.
 * @author Jianping Zeng.
 * @version 0.4
 */
public class ARMCC {
  // The CondCodes constants map directly to the 4-bit encoding of the
  // condition field for predicated instructions.
  enum CondCodes { // Meaning (integer)          Meaning (floating-point)
    EQ,            // Equal                      Equal
    NE,            // Not equal                  Not equal, or unordered
    HS,            // Carry set                  >, ==, or unordered
    LO,            // Carry clear                Less than
    MI,            // Minus, negative            Less than
    PL,            // Plus, positive or zero     >, ==, or unordered
    VS,            // Overflow                   Unordered
    VC,            // No overflow                Not unordered
    HI,            // Unsigned higher            Greater than, or unordered
    LS,            // Unsigned lower or same     Less than or equal
    GE,            // Greater than or equal      Greater than or equal
    LT,            // Less than                  Less than, or unordered
    GT,            // Greater than               Greater than
    LE,            // Less than or equal         <, ==, or unordered
    AL             // Always (unconditional)     Always (unconditional)
  }

  static CondCodes getOppositeCondition(CondCodes cc) {
    switch (cc) {
      default:
        Util.shouldNotReachHere("Unknown condition code");
        return null;
      case EQ: return CondCodes.NE;
      case NE: return CondCodes.EQ;
      case HS: return CondCodes.LO;
      case LO: return CondCodes.HS;
      case MI: return CondCodes.PL;
      case PL: return CondCodes.MI;
      case VS: return CondCodes.VC;
      case VC: return CondCodes.VS;
      case HI: return CondCodes.LS;
      case LS: return CondCodes.HI;
      case GE: return CondCodes.LT;
      case LT: return CondCodes.GE;
      case GT: return CondCodes.LE;
      case LE: return CondCodes.GT;
    }
  }

  static CondCodes getCondCodes(int cc) {
    return CondCodes.values()[cc];
  }

  static String ARMCondCodeToString(CondCodes cc) {
    switch (cc) {
      default: Util.shouldNotReachHere("Unknown condition code");
      case EQ:  return "eq";
      case NE:  return "ne";
      case HS:  return "hs";
      case LO:  return "lo";
      case MI:  return "mi";
      case PL:  return "pl";
      case VS:  return "vs";
      case VC:  return "vc";
      case HI:  return "hi";
      case LS:  return "ls";
      case GE:  return "ge";
      case LT:  return "lt";
      case GT:  return "gt";
      case LE:  return "le";
      case AL:  return "al";
    }
  }
}
