package jlang.basic;
/*
 * Extremely C language Compiler
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
 * @author Xlous.zeng
 * @version 0.1
 */
public interface BuiltID
{
    /**
     * This is not a built-in.
     */
    int NotBuiltin = 0;
    int BI__builtin_huge_val = 1;
    int BI__builtin_huge_valf = 2;
    int BI__builtin_huge_vall = 3;
    int BI__builtin_inf = 4;
    int BI__builtin_inff = 5;
    int BI__builtin_infl = 6;
    int BI__builtin_nan = 7;
    int BI__builtin_nanf = 8;
    int BI__builtin_nanl = 9;
    int BI__builtin_nans = 10;
    int BI__builtin_nansf = 11;
    int BI__builtin_nansl = 12;
    int BI__builtin_abs = 13;
    int BI__builtin_fabs = 14;
    int BI__builtin_fabsf = 15;
    int BI__builtin_fabsl = 16;
    int BI__builtin_copysign = 17;
    int BI__builtin_copysignf = 18;
    int BI__builtin_copysignl = 19;
    int BI__builtin_powi = 20;
    int BI__builtin_powif = 21;
    int BI__builtin_powil = 22;
    int BI__builtin_isgreater = 23;
    int BI__builtin_isgreaterequal = 24;
    int BI__builtin_isless = 25;
    int BI__builtin_islessequal = 26;
    int BI__builtin_islessgreater = 27;
    int BI__builtin_isunordered = 28;
    int BI__builtin_clz = 29;
    int BI__builtin_clzl = 30;
    int BI__builtin_clzll = 31;
    int BI__builtin_ctz = 32;
    int BI__builtin_ctzl = 33;
    int BI__builtin_ctzll = 34;
    int BI__builtin_ffs = 35;
    int BI__builtin_ffsl = 36;
    int BI__builtin_ffsll = 37;
    int BI__builtin_parity = 38;
    int BI__builtin_parityl = 39;
    int BI__builtin_parityll = 40;
    int BI__builtin_popcount = 41;
    int BI__builtin_popcountl = 42;
    int BI__builtin_popcountll = 43;
    int BI__builtin_bswap32 = 44;
    int BI__builtin_bswap64 = 45;
    int BI__builtin_constant_p = 46;
    int BI__builtin_classify_type = 47;
    int BI__builtin___CFStringMakeConstantString = 48;
    int BI__builtin_va_start = 49;
    int BI__builtin_va_end = 50;
    int BI__builtin_va_copy = 51;
    int BI__builtin_stdarg_start = 52;
    int BI__builtin_bcmp = 53;
    int BI__builtin_bcopy = 54;
    int BI__builtin_bzero = 55;
    int BI__builtin_memcmp = 56;
    int BI__builtin_memcpy = 57;
    int BI__builtin_memmove = 58;
    int BI__builtin_mempcpy = 59;
    int BI__builtin_memset = 60;
    int BI__builtin_stpcpy = 61;
    int BI__builtin_stpncpy = 62;
    int BI__builtin_strcasecmp = 63;
    int BI__builtin_strcat = 64;
    int BI__builtin_strchr = 65;
    int BI__builtin_strcmp = 66;
    int BI__builtin_strcpy = 67;
    int BI__builtin_strcspn = 68;
    int BI__builtin_strdup = 69;
    int BI__builtin_strlen = 70;
    int BI__builtin_strncasecmp = 71;
    int BI__builtin_strncat = 72;
    int BI__builtin_strncmp = 73;
    int BI__builtin_strncpy = 74;
    int BI__builtin_strndup = 75;
    int BI__builtin_strpbrk = 76;
    int BI__builtin_strrchr = 77;
    int BI__builtin_strspn = 78;
    int BI__builtin_strstr = 79;
    int BI__builtin_return_address = 80;
    int BI__builtin_extract_return_addr = 81;
    int BI__builtin_frame_address = 82;
    int BI__builtin_flt_rounds = 83;
    int BI__builtin_setjmp = 84;
    int BI__builtin_longjmp = 85;
    int BI__builtin_unwind_init = 86;
    int BI__builtin_object_size = 87;
    int BI__builtin___memcpy_chk = 88;
    int BI__builtin___memmove_chk = 89;
    int BI__builtin___mempcpy_chk = 90;
    int BI__builtin___memset_chk = 91;
    int BI__builtin___stpcpy_chk = 92;
    int BI__builtin___strcat_chk = 93;
    int BI__builtin___strcpy_chk = 94;
    int BI__builtin___strncat_chk = 95;
    int BI__builtin___strncpy_chk = 96;
    int BI__builtin___snprintf_chk = 97;
    int BI__builtin___sprintf_chk = 98;
    int BI__builtin___vsnprintf_chk = 99;
    int BI__builtin___vsprintf_chk = 100;
    int BI__builtin___fprintf_chk = 101;
    int BI__builtin___printf_chk = 102;
    int BI__builtin___vfprintf_chk = 103;
    int BI__builtin___vprintf_chk = 104;
    int BI__builtin_expect = 105;
    int BI__builtin_prefetch = 106;
    int BI__builtin_trap = 107;
    int BI__builtin_shufflevector = 108;
    int BI__builtin_alloca = 109;
    int BI__sync_fetch_and_add = 110;
    int BI__sync_fetch_and_add_1 = 111;
    int BI__sync_fetch_and_add_2 = 112;
    int BI__sync_fetch_and_add_4 = 113;
    int BI__sync_fetch_and_add_8 = 114;
    int BI__sync_fetch_and_add_16 = 115;
    int BI__sync_fetch_and_sub = 116;
    int BI__sync_fetch_and_sub_1 = 117;
    int BI__sync_fetch_and_sub_2 = 118;
    int BI__sync_fetch_and_sub_4 = 119;
    int BI__sync_fetch_and_sub_8 = 120;
    int BI__sync_fetch_and_sub_16 = 121;
    int BI__sync_fetch_and_or = 122;
    int BI__sync_fetch_and_or_1 = 123;
    int BI__sync_fetch_and_or_2 = 124;
    int BI__sync_fetch_and_or_4 = 125;
    int BI__sync_fetch_and_or_8 = 126;
    int BI__sync_fetch_and_or_16 = 127;
    int BI__sync_fetch_and_and = 128;
    int BI__sync_fetch_and_and_1 = 129;
    int BI__sync_fetch_and_and_2 = 130;
    int BI__sync_fetch_and_and_4 = 131;
    int BI__sync_fetch_and_and_8 = 132;
    int BI__sync_fetch_and_and_16 = 133;
    int BI__sync_fetch_and_xor = 134;
    int BI__sync_fetch_and_xor_1 = 135;
    int BI__sync_fetch_and_xor_2 = 136;
    int BI__sync_fetch_and_xor_4 = 137;
    int BI__sync_fetch_and_xor_8 = 138;
    int BI__sync_fetch_and_xor_16 = 139;
    int BI__sync_fetch_and_nand = 140;
    int BI__sync_fetch_and_nand_1 = 141;
    int BI__sync_fetch_and_nand_2 = 142;
    int BI__sync_fetch_and_nand_4 = 143;
    int BI__sync_fetch_and_nand_8 = 144;
    int BI__sync_fetch_and_nand_16 = 145;
    int BI__sync_add_and_fetch = 146;
    int BI__sync_add_and_fetch_1 = 147;
    int BI__sync_add_and_fetch_2 = 148;
    int BI__sync_add_and_fetch_4 = 149;
    int BI__sync_add_and_fetch_8 = 150;
    int BI__sync_add_and_fetch_16 = 151;
    int BI__sync_sub_and_fetch = 152;
    int BI__sync_sub_and_fetch_1 = 153;
    int BI__sync_sub_and_fetch_2 = 154;
    int BI__sync_sub_and_fetch_4 = 155;
    int BI__sync_sub_and_fetch_8 = 156;
    int BI__sync_sub_and_fetch_16 = 157;
    int BI__sync_or_and_fetch = 158;
    int BI__sync_or_and_fetch_1 = 159;
    int BI__sync_or_and_fetch_2 = 160;
    int BI__sync_or_and_fetch_4 = 161;
    int BI__sync_or_and_fetch_8 = 162;
    int BI__sync_or_and_fetch_16 = 163;
    int BI__sync_and_and_fetch = 164;
    int BI__sync_and_and_fetch_1 = 165;
    int BI__sync_and_and_fetch_2 = 166;
    int BI__sync_and_and_fetch_4 = 167;
    int BI__sync_and_and_fetch_8 = 168;
    int BI__sync_and_and_fetch_16 = 169;
    int BI__sync_xor_and_fetch = 170;
    int BI__sync_xor_and_fetch_1 = 171;
    int BI__sync_xor_and_fetch_2 = 172;
    int BI__sync_xor_and_fetch_4 = 173;
    int BI__sync_xor_and_fetch_8 = 174;
    int BI__sync_xor_and_fetch_16 = 175;
    int BI__sync_nand_and_fetch = 176;
    int BI__sync_nand_and_fetch_1 = 177;
    int BI__sync_nand_and_fetch_2 = 178;
    int BI__sync_nand_and_fetch_4 = 179;
    int BI__sync_nand_and_fetch_8 = 180;
    int BI__sync_nand_and_fetch_16 = 181;
    int BI__sync_bool_compare_and_swap = 182;
    int BI__sync_bool_compare_and_swap_1 = 183;
    int BI__sync_bool_compare_and_swap_2 = 184;
    int BI__sync_bool_compare_and_swap_4 = 185;
    int BI__sync_bool_compare_and_swap_8 = 186;
    int BI__sync_bool_compare_and_swap_16 = 187;
    int BI__sync_val_compare_and_swap = 188;
    int BI__sync_val_compare_and_swap_1 = 189;
    int BI__sync_val_compare_and_swap_2 = 190;
    int BI__sync_val_compare_and_swap_4 = 191;
    int BI__sync_val_compare_and_swap_8 = 192;
    int BI__sync_val_compare_and_swap_16 = 193;
    int BI__sync_lock_test_and_set = 194;
    int BI__sync_lock_test_and_set_1 = 195;
    int BI__sync_lock_test_and_set_2 = 196;
    int BI__sync_lock_test_and_set_4 = 197;
    int BI__sync_lock_test_and_set_8 = 198;
    int BI__sync_lock_test_and_set_16 = 199;
    int BI__sync_lock_release = 200;
    int BI__sync_lock_release_1 = 201;
    int BI__sync_lock_release_2 = 202;
    int BI__sync_lock_release_4 = 203;
    int BI__sync_lock_release_8 = 204;
    int BI__sync_lock_release_16 = 205;
    int BI__sync_synchronize = 206;
    int BI__builtin_llvm_memory_barrier = 207;
    int BI__sync_fetch_and_min = 208;
    int BI__sync_fetch_and_max = 209;
    int BI__sync_fetch_and_umin = 210;
    int BI__sync_fetch_and_umax = 211;
    int BIcalloc = 212;
    int BIexit = 213;
    int BI_Exit = 214;
    int BImalloc = 215;
    int BIrealloc = 216;
    int BImemcpy = 217;
    int BImemmove = 218;
    int BIstrcpy = 219;
    int BIstrncpy = 220;
    int BIstrcat = 221;
    int BIstrncat = 222;
    int BIstrxfrm = 223;
    int BImemchr = 224;
    int BIstrchr = 225;
    int BIstrcspn = 226;
    int BIstrpbrk = 227;
    int BIstrrchr = 228;
    int BIstrspn = 229;
    int BIstrstr = 230;
    int BIstrtok = 231;
    int BImemset = 232;
    int BIstrerror = 233;
    int BIstrlen = 234;
    int BIprintf = 235;
    int BIfprintf = 236;
    int BIsnprintf = 237;
    int BIsprintf = 238;
    int BIvprintf = 239;
    int BIvfprintf = 240;
    int BIvsnprintf = 241;
    int BIvsprintf = 242;
    int BIlongjmp = 243;
    int BIalloca = 244;
    int BIstpcpy = 245;
    int BIstpncpy = 246;
    int BIstrdup = 247;
    int BIstrndup = 248;
    int BIindex = 249;
    int BIrindex = 250;
    int BI_exit = 251;
    int BI_longjmp = 252;
    int BIsiglongjmp = 253;
    int BIobjc_msgSend = 254;
    int BIpow = 255;
    int BIpowl = 256;
    int BIpowf = 257;
    int BIsqrt = 258;
    int BIsqrtl = 259;
    int BIsqrtf = 260;
    int BIsin = 261;
    int BIsinl = 262;
    int BIsinf = 263;
    int BIcos = 264;
    int BIcosl = 265;
    int BIcosf = 266;
    /**
     * All target-specific built-in starts from this id.
     */
    int FirstTSBuiltin = 267;
}
