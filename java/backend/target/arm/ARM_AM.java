package backend.target.arm;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
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

import backend.codegen.dagisel.ISD;
import tools.APFloat;
import tools.APInt;
import tools.OutRef;
import tools.Util;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class ARM_AM {
  /**
   * Given a 32-bit immediate, if it is something that can fit
   * into an shifter_operand immediate operand, return the 12-bit encoding for
   * it.  If not, return -1.
   * @param arg
   * @return
   */
  public static int getSOImmVal(int arg) {
    if ((arg & ~255) == 0) return arg;

    int rotAmt = getSOImmValRotate(arg);
    if ((rotr32(~255, rotAmt) & arg) != 0)
      return -1;

    return rotl32(arg, rotAmt) | ((rotAmt >>> 1) << 8);
  }

  public static int getT2SOImmVal(int arg) {
    int splat = getT2SOImmValSplatVal(arg);
    if (splat != -1)
      return splat;

    return getT2SOImmValRotateVal(arg);
  }

  private static int getT2SOImmValRotateVal(int val) {
    int rotAmt = Util.countLeadingZeros32(val);
    if (rotAmt >= 24)
      return -1;

    if ((rotr32(0xff000000, rotAmt) & val) == val)
      return (rotr32(val, 24 - rotAmt) & 0x7f) | ((rotAmt + 8) << 7);
    return -1;
  }

  /**
   * Return the 12-bit encoded representation
   * if the specified value can be obtained by splatting the low 8 bits
   * into every other byte or every byte of a 32-bit value. i.e.,
   * <pre>
   *     00000000 00000000 00000000 abcdefgh    control = 0
   *     00000000 abcdefgh 00000000 abcdefgh    control = 1
   *     abcdefgh 00000000 abcdefgh 00000000    control = 2
   *     abcdefgh abcdefgh abcdefgh abcdefgh    control = 3
   * </pre>
   * Return -1 if none of the above apply. See ARM Reference Manual A6.3.2.
   * @param val
   * @return
   */
  private static int getT2SOImmValSplatVal(int val) {
    int u, vs, imm;
    if ((val & 0xffffff00) == 0)
      return val;

    vs = (val & 0xff) == 0 ? val >>> 8 : val;
    imm = vs & 0xff;
    u = imm | (imm << 16);
    if (vs == u)
      return ((vs == val ? 1 : 2) << 8) | imm;

    if (vs == (u | (u << 8)))
      return (3 << 8) | imm;
    return -1;
  }

  /**
   * Try to handle Imm with an immediate shifter operand,
   * computing the rotate amount to use.  If this immediate value cannot be
   * handled with a single shifter-op, determine a good rotate amount that will
   * take a maximal chunk of bits out of the immediate.
   * @param val
   * @return
   */
  static int getSOImmValRotate(int val) {
    if ((val & ~255) == 0) return 0;

    int tz = Util.countTrailingZeros(val);
    int rotAmt = tz & ~1;

    if ((rotr32(val, rotAmt) & ~255) == 0)
      return (32 - rotAmt) & 21;

    if ((val & 63) != 0) {
      int tz2 = Util.countTrailingZeros(val & ~63);
      int rotAmt2 = tz2 & ~1;
      if ((rotr32(val, rotAmt2) & ~255) == 0)
        return (32 - rotAmt2)&31;
    }
    return (32 - rotAmt)&31;
  }

  public static long decodeNEONModImm(long modImm, OutRef<Integer> eltBits) {
    int opCmode = getNEONModImmOpCmode((int) modImm);
    int imm8 = getNEONModImmVal((int) modImm);
    long val = 0;

    if (opCmode == 0xe) {
      // 8 bit vector element.
      val = imm8;
      eltBits.set(8);
    }
    else if ((opCmode & 0xc) == 0x8) {
      // 16 bit vector element.
      int byteNum = (opCmode & 0x6) >>> 1;
      val = imm8 << (8 * byteNum);
      eltBits.set(16);
    }
    else if ((opCmode & 0x8) == 0) {
      // 32-bit vector elements, zero with one byte set
      int byteNum = (opCmode & 0x6) >>> 1;
      val = imm8 << (8 * byteNum);
      eltBits.set(32);
    }
    else if ((opCmode & 0xe) == 0xc) {
      // 32-bit vector elements, one byte with low bits set
      int byteNum = (opCmode & 0x1) + 1;
      val = (imm8 << (8 * byteNum) | (0xffff >>> (8*(2-byteNum))));
      eltBits.set(32);
    }
    else if (opCmode == 0x1e) {
      // 64-bit vector elements
      for (int byteNum = 0; byteNum < 8; ++byteNum) {
        if (((modImm >>> byteNum) & 1) != 0)
          val |= 0xffL << (8 * byteNum);
      }
      eltBits.set(64);
    }
    return val;
  }

  public static boolean isThumbImmShiftedVal(int val) {
    val = (~255 << getThumbImmValShift(val) & val);
    return val == 0;
  }

  /**
   * Rotate a 32-bit int value right by a specified # bits.
   * @param val
   * @param amt
   * @return
   */
  static int rotr32(int val, int amt) {
    Util.assertion(amt < 32, "Invalid rotate amount");
    return (val >>> amt) | (val << ((32-amt)&31));
  }

  /**
   * Rotate a 32-bit int value left by a specified # bits.
   * @param Val
   * @param Amt
   * @return
   */
  static int rotl32(int Val, int Amt) {
    Util.assertion(Amt < 32, "Invalid rotate amount");
    return (Val << Amt) | (Val >>> ((32-Amt)&31));
  }
  
  public static boolean isSOImmTwoPartVal(int val) {
    val = rotr32(~255, getSOImmValRotate(val) & val);
    if (val == 0)
      return false;

    val = rotr32(~255, getSOImmValRotate(val) & val);
    return val == 0;
  }

  public static int getThumbImmNonShiftedVal(int val) {
    return val >>> getThumbImmValShift(val);
  }

  public static int getThumbImmValShift(int imm) {
    if ((imm & ~255) == 0) return 0;
    return Util.countTrailingZeros(imm);
  }

  static ShiftOpc getSORegShOp(long imm) {
    return ShiftOpc.values()[(int) imm];
  }

  static long getSORegShOpc(ShiftOpc shOp, long imm) {
    return shOp.ordinal() | (imm << 3);
  }

  static int getSORegOffset(long imm) {
    return (int) (imm >> 3);
  }

  static int getAM2IdxMode(long am2Opc) {
    return (int) (am2Opc >>> 16);
  }

  public enum ShiftOpc {
    no_shift,
    asr,
    lsl,
    lsr,
    ror,
    rrx
  }

  enum AddrOpc {
    sub,
    add
  };

  //===--------------------------------------------------------------------===//
  // Addressing Mode #2
  //===--------------------------------------------------------------------===//
  //
  // This is used for most simple load/store instructions.
  //
  // addrmode2 := reg +/- reg shop imm
  // addrmode2 := reg +/- imm12
  //
  // The first operand is always a Reg.  The second operand is a reg if in
  // reg/reg form, otherwise it's reg#0.  The third field encodes the operation
  // in bit 12, the immediate in bits 0-11, and the shift op in 13-15. The
  // fourth operand 16-17 encodes the index mode.
  //
  // If this addressing mode is a frame index (before prolog/epilog insertion
  // and code rewriting), this operand will have the form:  FI#, reg0, <offs>
  // with no shift amount for the frame offset.

  static int getAM2Opc(AddrOpc opc, int imm12, ShiftOpc so) {
    return getAM2Opc(opc, imm12, so, 0);
  }

  static int getAM2Opc(AddrOpc opc, int imm12, ShiftOpc so, int idxMode) {
    Util.assertion(imm12 < (1 << 12), "Imm too large!");
    boolean isSub = opc == AddrOpc.sub;
    return imm12 | ((isSub ? 1 : 0) << 12) | (so.ordinal() << 13) | (idxMode << 16) ;
  }

  static  int getAM2Offset(int am2Opc) {
    return am2Opc & ((1 << 12)-1);
  }

  static  AddrOpc getAM2Op(int am2Opc) {
    return ((am2Opc >> 12) & 1) != 0 ? AddrOpc.sub : AddrOpc.add;
  }

  static  ShiftOpc getAM2ShiftOpc(int am2Opc) {
    return ShiftOpc.values()[((am2Opc >> 13) & 7)];
  }

  static  int getAM2IdxMode(int am2Opc) {
    return (am2Opc >> 16);
  }

  static String getAddrOpcStr(AddrOpc Op) {
    return Op == AddrOpc.sub ? "-" : "";
  }

  static String getShiftOpcStr(ShiftOpc Op) {
    switch (Op) {
      default: Util.shouldNotReachHere("Unknown shift opc!");
      case asr: return "asr";
      case lsl: return "lsl";
      case lsr: return "lsr";
      case ror: return "ror";
      case rrx: return "rrx";
    }
  }

  static ShiftOpc getShiftOpcForNode(int opc) {
    switch (opc) {
      default: return ShiftOpc.no_shift;
      case ISD.SHL: return ShiftOpc.lsl;
      case ISD.SRL: return ShiftOpc.lsr;
      case ISD.SRA: return ShiftOpc.asr;
      case ISD.ROTR: return ShiftOpc.ror;
    }
  }

  static int getShiftOpcEncoding(ShiftOpc Op) {
    switch (Op) {
      default: Util.shouldNotReachHere("Unknown shift opc!");
      case asr: return 2;
      case lsl: return 0;
      case lsr: return 1;
      case ror: return 3;
    }
  }

  enum AMSubMode {
    bad_am_submode,
    ia,
    ib,
    da,
    db
  }

  //===--------------------------------------------------------------------===//
  // Addressing Mode #3
  //===--------------------------------------------------------------------===//
  //
  // This is used for sign-extending loads, and load/store-pair instructions.
  //
  // addrmode3 := reg +/- reg
  // addrmode3 := reg +/- imm8
  //
  // The first operand is always a Reg.  The second operand is a reg if in
  // reg/reg form, otherwise it's reg#0.  The third field encodes the operation
  // in bit 8, the immediate in bits 0-7. The fourth operand 9-10 encodes the
  // index mode.

  /// getAM3Opc - This function encodes the addrmode3 opc field.
  static int getAM3Opc(AddrOpc Opc, int Offset) {
    return getAM3Opc(Opc, Offset, 0);
  }

  static int getAM3Opc(AddrOpc Opc, int Offset, int IdxMode) {
    boolean isSub = Opc == AddrOpc.sub;
    return ((isSub ? 1 : 0) << 8) | Offset | (IdxMode << 9);
  }
  static int getAM3Offset(int AM3Opc) {
    return AM3Opc & 0xFF;
  }
  static AddrOpc getAM3Op(int AM3Opc) {
    return ((AM3Opc >> 8) & 1) != 0 ? AddrOpc.sub : AddrOpc.add;
  }
  static int getAM3IdxMode(int AM3Opc) {
    return (AM3Opc >> 9);
  }

  //===--------------------------------------------------------------------===//
  // Addressing Mode #4
  //===--------------------------------------------------------------------===//
  //
  // This is used for load / store multiple instructions.
  //
  // addrmode4 := reg, <mode>
  //
  // The four modes are:
  //    IA - Increment after
  //    IB - Increment before
  //    DA - Decrement after
  //    DB - Decrement before
  // For VFP instructions, only the IA and DB modes are valid.

  static AMSubMode getAM4SubMode(int Mode) {
    return AMSubMode.values()[Mode & 0x7];
  }

  static int getAM4ModeImm(AMSubMode SubMode) {
    return SubMode.ordinal();
  }

  //===--------------------------------------------------------------------===//
  // Addressing Mode #5
  //===--------------------------------------------------------------------===//
  //
  // This is used for coprocessor instructions, such as FP load/stores.
  //
  // addrmode5 := reg +/- imm8*4
  //
  // The first operand is always a Reg.  The second operand encodes the
  // operation in bit 8 and the immediate in bits 0-7.

  /// getAM5Opc - This function encodes the addrmode5 opc field.
  static int getAM5Opc(AddrOpc Opc, int Offset) {
    boolean isSub = Opc == AddrOpc.sub;
    return ((isSub ? 1 : 0) << 8) | Offset;
  }
  static int getAM5Offset(int AM5Opc) {
    return AM5Opc & 0xFF;
  }
  static AddrOpc getAM5Op(int AM5Opc) {
    return ((AM5Opc >> 8) & 1) != 0 ? AddrOpc.sub : AddrOpc.add;
  }

  //===--------------------------------------------------------------------===//
  // Addressing Mode #6
  //===--------------------------------------------------------------------===//
  //
  // This is used for NEON load / store instructions.
  //
  // addrmode6 := reg with optional alignment
  //
  // This is stored in two operands [regaddr, align].  The first is the
  // address register.  The second operand is the value of the alignment
  // specifier in bytes or zero if no explicit alignment.
  // Valid alignments depend on the specific instruction.

  //===--------------------------------------------------------------------===//
  // NEON Modified Immediates
  //===--------------------------------------------------------------------===//
  //
  // Several NEON instructions (e.g., VMOV) take a "modified immediate"
  // vector operand, where a small immediate encoded in the instruction
  // specifies a full NEON vector value.  These modified immediates are
  // represented here as encoded integers.  The low 8 bits hold the immediate
  // value; bit 12 holds the "Op" field of the instruction, and bits 11-8 hold
  // the "Cmode" field of the instruction.  The interfaces below treat the
  // Op and Cmode values as a single 5-bit value.

  static int createNEONModImm(int OpCmode, int Val) {
    return (OpCmode << 8) | Val;
  }
  static int getNEONModImmOpCmode(int ModImm) {
    return (ModImm >> 8) & 0x1f;
  }
  static int getNEONModImmVal(int ModImm) {
    return ModImm & 0xff;
  }

  /// decodeNEONModImm - Decode a NEON modified immediate value into the
  /// element value and the element size in bits.  (If the element size is
  /// smaller than the vector, it is splatted into all the elements.)
  static long decodeNEONModImm(int ModImm, int EltBits) {
    int OpCmode = getNEONModImmOpCmode(ModImm);
    int Imm8 = getNEONModImmVal(ModImm);
    long Val = 0;

    if (OpCmode == 0xe) {
      // 8-bit vector elements
      Val = Imm8;
      EltBits = 8;
    } else if ((OpCmode & 0xc) == 0x8) {
      // 16-bit vector elements
      int ByteNum = (OpCmode & 0x6) >> 1;
      Val = Imm8 << (8 * ByteNum);
      EltBits = 16;
    } else if ((OpCmode & 0x8) == 0) {
      // 32-bit vector elements, zero with one byte set
      int ByteNum = (OpCmode & 0x6) >> 1;
      Val = Imm8 << (8 * ByteNum);
      EltBits = 32;
    } else if ((OpCmode & 0xe) == 0xc) {
      // 32-bit vector elements, one byte with low bits set
      int ByteNum = 1 + (OpCmode & 0x1);
      Val = (Imm8 << (8 * ByteNum)) | (0xffff >> (8 * (2 - ByteNum)));
      EltBits = 32;
    } else if (OpCmode == 0x1e) {
      // 64-bit vector elements
      for (int ByteNum = 0; ByteNum < 8; ++ByteNum) {
        if (((ModImm >> ByteNum) & 1) != 0)
          Val |= (long)0xff << (8 * ByteNum);
      }
      EltBits = 64;
    } else {
      Util.shouldNotReachHere("Unsupported NEON immediate");
    }
    return Val;
  }

  AMSubMode getLoadStoreMultipleSubMode(int Opcode) {
    switch (Opcode) {
      default: Util.shouldNotReachHere("Unhandled opcode!");
      case ARMGenInstrNames.LDMIA_RET:
      case ARMGenInstrNames.LDMIA:
      case ARMGenInstrNames.LDMIA_UPD:
      case ARMGenInstrNames.STMIA:
      case ARMGenInstrNames.STMIA_UPD:
      case ARMGenInstrNames.t2LDMIA_RET:
      case ARMGenInstrNames.t2LDMIA:
      case ARMGenInstrNames.t2LDMIA_UPD:
      case ARMGenInstrNames.t2STMIA:
      case ARMGenInstrNames.t2STMIA_UPD:
      case ARMGenInstrNames.VLDMSIA:
      case ARMGenInstrNames.VLDMSIA_UPD:
      case ARMGenInstrNames.VSTMSIA:
      case ARMGenInstrNames.VSTMSIA_UPD:
      case ARMGenInstrNames.VLDMDIA:
      case ARMGenInstrNames.VLDMDIA_UPD:
      case ARMGenInstrNames.VSTMDIA:
      case ARMGenInstrNames.VSTMDIA_UPD:
        return AMSubMode.ia;

      case ARMGenInstrNames.LDMDA:
      case ARMGenInstrNames.LDMDA_UPD:
      case ARMGenInstrNames.STMDA:
      case ARMGenInstrNames.STMDA_UPD:
        return AMSubMode.da;

      case ARMGenInstrNames.LDMDB:
      case ARMGenInstrNames.LDMDB_UPD:
      case ARMGenInstrNames.STMDB:
      case ARMGenInstrNames.STMDB_UPD:
      case ARMGenInstrNames.t2LDMDB:
      case ARMGenInstrNames.t2LDMDB_UPD:
      case ARMGenInstrNames.t2STMDB:
      case ARMGenInstrNames.t2STMDB_UPD:
      case ARMGenInstrNames.VLDMSDB_UPD:
      case ARMGenInstrNames.VSTMSDB_UPD:
      case ARMGenInstrNames.VLDMDDB_UPD:
      case ARMGenInstrNames.VSTMDDB_UPD:
        return AMSubMode.db;

      case ARMGenInstrNames.LDMIB:
      case ARMGenInstrNames.LDMIB_UPD:
      case ARMGenInstrNames.STMIB:
      case ARMGenInstrNames.STMIB_UPD:
        return AMSubMode.ib;
    }
  }

  //===--------------------------------------------------------------------===//
  // Floating-point Immediates
  //
  static float getFPImmFloat(int Imm) {
    // We expect an 8-bit binary encoding of a floating-point number here.
    int Sign = (Imm >> 7) & 0x1;
    int Exp = (Imm >> 4) & 0x7;
    int Mantissa = Imm & 0xf;

    //   8-bit FP    iEEEE Float Encoding
    //   abcd efgh   aBbbbbbc defgh000 00000000 00000000
    //
    // where B = NOT(b);
    int I;
    I = 0;
    I |= Sign << 31;
    I |= ((Exp & 0x4) != 0 ? 0 : 1) << 30;
    I |= ((Exp & 0x4) != 0 ? 0x1f : 0) << 25;
    I |= (Exp & 0x3) << 23;
    I |= Mantissa << 19;
    return Util.bitsToFloat(I);
  }

  /// getFP32Imm - Return an 8-bit floating-point version of the 32-bit
  /// floating-point value. If the value cannot be represented as an 8-bit
  /// floating-point value, then return -1.
  static long getFP32Imm(APInt Imm) {
    long Sign = Imm.lshr(31).getZExtValue() & 1;
    long Exp = (Imm.lshr(23).getSExtValue() & 0xff) - 127;  // -126 to 127
    long Mantissa = Imm.getZExtValue() & 0x7fffff;  // 23 bits

    // We can handle 4 bits of mantissa.
    // mantissa = (16+UInt(e:f:g:h))/16.
    if ((Mantissa & 0x7ffff) != 0)
      return -1;
    Mantissa >>= 19;
    if ((Mantissa & 0xf) != Mantissa)
      return -1;

    // We can handle 3 bits of exponent: exp == UInt(NOT(b):c:d)-3
    if (Exp < -3 || Exp > 4)
      return -1;
    Exp = ((Exp+3) & 0x7) ^ 4;

    return ((int)Sign << 7) | (Exp << 4) | Mantissa;
  }

  static int getFP32Imm(APFloat FPImm) {
    return (int)getFP32Imm(FPImm.bitcastToAPInt());
  }

  /// getFP64Imm - Return an 8-bit floating-point version of the 64-bit
  /// floating-point value. If the value cannot be represented as an 8-bit
  /// floating-point value, then return -1.
  static long getFP64Imm(APInt Imm) {
    long Sign = Imm.lshr(63).getZExtValue() & 1;
    long Exp = (Imm.lshr(52).getSExtValue() & 0x7ff) - 1023; // -1022 to 1023
    long Mantissa = Imm.getZExtValue() & 0xfffffffffffffL;

    // We can handle 4 bits of mantissa.
    // mantissa = (16+UInt(e:f:g:h))/16.
    if ((Mantissa & 0xffffffffffffL) != 0)
    return -1;
    Mantissa >>= 48;
    if ((Mantissa & 0xf) != Mantissa)
      return -1;

    // We can handle 3 bits of exponent: exp == UInt(NOT(b):c:d)-3
    if (Exp < -3 || Exp > 4)
      return -1;
    Exp = ((Exp+3) & 0x7) ^ 4;

    return ((int)Sign << 7) | (Exp << 4) | Mantissa;
  }

  static int getFP64Imm(APFloat FPImm) {
    return (int)getFP64Imm(FPImm.bitcastToAPInt());
  }
}
