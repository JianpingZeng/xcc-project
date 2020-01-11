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

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public interface ARMBuildAttrs {
  // This is for the .cpu asm attr. It translates into one or more
  // AttrType (below) entries in the .ARM.attributes section in the ELF.
  int SEL_CPU = 0;

  // Rest correspond to ELF/.ARM.attributes
  int File                  = 1,
  Section                   = 2,
  Symbol                    = 3,
  CPU_raw_name              = 4,
  CPU_name                  = 5,
  CPU_arch                  = 6,
  CPU_arch_profile          = 7,
  ARM_ISA_use               = 8,
  THUMB_ISA_use             = 9,
  VFP_arch                  = 10,
  WMMX_arch                 = 11,
  Advanced_SIMD_arch        = 12,
  PCS_config                = 13,
  ABI_PCS_R9_use            = 14,
  ABI_PCS_RW_data           = 15,
  ABI_PCS_RO_data           = 16,
  ABI_PCS_GOT_use           = 17,
  ABI_PCS_wchar_t           = 18,
  ABI_FP_rounding           = 19,
  ABI_FP_denormal           = 20,
  ABI_FP_exceptions         = 21,
  ABI_FP_user_exceptions    = 22,
  ABI_FP_number_model       = 23,
  ABI_align8_needed         = 24,
  ABI_align8_preserved      = 25,
  ABI_enum_size             = 26,
  ABI_HardFP_use            = 27,
  ABI_VFP_args              = 28,
  ABI_WMMX_args             = 29,
  ABI_optimization_goals    = 30,
  ABI_FP_optimization_goals = 31,
  compatibility             = 32,
  CPU_unaligned_access      = 34,
  VFP_HP_extension          = 36,
  ABI_FP_16bit_format       = 38,
  MPextension_use           = 42, // was 70, 2.08 ABI
  DIV_use                   = 44,
  nodefaults                = 64,
  also_compatible_with      = 65,
  T2EE_use                  = 66,
  conformance               = 67,
  Virtualization_use        = 68,
  MPextension_use_old       = 70;

  // Magic numbers for .ARM.attributes
  int Format_Version  = 0x41;

  // Legal Values for CPU_arch, (=6), uleb128
  int  Pre_v4       = 0,
  v4       = 1,   // e.g. SA110
  v4T      = 2,   // e.g. ARM7TDMI
  v5T      = 3,   // e.g. ARM9TDMI
  v5TE     = 4,   // e.g. ARM946E_S
  v5TEJ    = 5,   // e.g. ARM926EJ_S
  v6       = 6,   // e.g. ARM1136J_S
  v6KZ     = 7,   // e.g. ARM1176JZ_S
  v6T2     = 8,   // e.g. ARM1156T2F_S
  v6K      = 9,   // e.g. ARM1136J_S
  v7       = 10,  // e.g. Cortex A8, Cortex M3
  v6_M     = 11,  // e.g. Cortex M1
  v6S_M    = 12,  // v6_M with the System extensions
  v7E_M    = 13;   // v7_M with DSP extensions

  // CPUArchProfile
  // (=7), uleb128
  int Not_Applicable = 0, // pre v7, or cross-profile code
  ApplicationProfile = (0x41), // 'A' (e.g. for Cortex A8)
  RealTimeProfile = (0x52), // 'R' (e.g. for Cortex R4)
  MicroControllerProfile = (0x4D), // 'M' (e.g. for Cortex M3)
  SystemProfile = (0x53); // 'S' Application or real-time profile

  // The following have a lot of common use cases

    //ARMISAUse (=8), uleb128  and THUMBISAUse (=9), uleb128
  int  Not_Allowed = 0,
  Allowed = 1,

  // FP_arch (=10), uleb128 (formerly Tag_VFP_arch = 10)
  AllowFPv2  = 2, // v2 FP ISA permitted (implies use of the v1 FP ISA)
  AllowFPv3A = 3, // v3 FP ISA permitted (implies use of the v2 FP ISA)
  AllowFPv3B = 4, // v3 FP ISA permitted, but only D0-D15, S0-S31
  AllowFPv4A = 5, // v4 FP ISA permitted (implies use of v3 FP ISA)
  AllowFPv4B = 6, // v4 FP ISA was permitted, but only D0-D15, S0-S31

  // Tag_WMMX_arch, (=11), uleb128
  AllowThumb32 = 2, // 32-bit Thumb (implies 16-bit instructions)

  // Tag_WMMX_arch, (=11), uleb128
  AllowWMMXv1 = 2,  // The user permitted this entity to use WMMX v2

  // Tag_ABI_FP_denormal, (=20), uleb128
  PreserveFPSign = 2, // sign when flushed-to-zero is preserved

  // Tag_ABI_FP_number_model, (=23), uleb128
  AllowRTABI = 2,  // numbers, infinities, and one quiet NaN (see [RTABI])
  AllowIEE754 = 3; // this code to use all the IEEE 754-defined FP encodings
}
