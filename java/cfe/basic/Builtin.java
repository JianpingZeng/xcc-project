package cfe.basic;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
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

import static cfe.basic.BuiltID.*;

// FIXME: this needs to be the full list supported by GCC.  Right now, I'm just
// adding stuff on demand.
//
// FIXME: This should really be a .td file, but that requires modifying tblgen.
// Perhaps tblgen should have plugins.

// The first value provided to the macro specifies the function name of the
// builtin, and results in a clang::builtin::BIXX enum value for XX.

// The second value provided to the macro specifies the type of the function
// (result value, then each argument) as follows:
//  v -> void
//  b -> boolean
//  c -> char
//  s -> short
//  i -> int
//  f -> float
//  d -> double
//  z -> size_t
//  F -> constant CFString
//  a -> __builtin_va_list
//  A -> "reference" to __builtin_va_list
//  V -> Vector, following num elements and a base type.
//  P -> FILE
//  J -> jmp_buf
//  SJ -> sigjmp_buf
//  . -> "...".  This may only occur at the end of the function list.
//
// Types maybe prefixed with the following modifiers:
//  L   -> long (e.g. Li for 'long int')
//  LL  -> long long
//  LLL -> __int128_t (e.g. LLLi)
//  S   -> signed
//  U   -> unsigned
//
// Types may be postfixed with the following modifiers:
// * -> pointer
// & -> reference
// C -> const

// The third value provided to the macro specifies information about attributes
// of the function.  These must be kept in sync with the predicates in the
// Builtin::Context class.  Currently we have:
//  n -> nothrow
//  r -> noreturn
//  c -> const
//  F -> this is a libc/libm function with a '__builtin_' prefix added.
//  f -> this is a libc/libm function without the '__builtin_' prefix. It can
//       be followed by ':headername:' to state which header this function
//       comes from.
//  p:N: -> this is a printf-like function whose Nth argument is the format
//          string.
//  P:N: -> similar to the p:N: attribute, but the function is like vprintf
//          in that it accepts its arguments as a va_list rather than
//          through an ellipsis
//  e -> const, but only when -fmath-errno=0
//  FIXME: gcc has nonnull

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public final class Builtin {
  enum BUILTIN {
    __BUILTIN_HUGE_VAL(BI__builtin_huge_val, "__builtin_huge_val", "d", "nc"),
    __BUILTIN_HUGE_VALF(BI__builtin_huge_valf, "__builtin_huge_valf", "f", "nc"),
    __BUILTIN_HUGE_VALL(BI__builtin_huge_vall, "__builtin_huge_vall", "Ld", "nc"),
    __BUILTIN_INF(BI__builtin_inf, "__builtin_inf", "d", "nc"),
    __BUILTIN_INFF(BI__builtin_inff, "__builtin_inff", "f", "nc"),
    __BUILTIN_INFL(BI__builtin_infl, "__builtin_infl", "Ld", "nc"),
    __BUILTIN_NAN(BI__builtin_nan, "__builtin_nan", "dcC*", "ncF"),
    __BUILTIN_NANF(BI__builtin_nanf, "__builtin_nanf", "fcC*", "ncF"),
    __BUILTIN_NANL(BI__builtin_nanl, "__builtin_nanl", "LdcC*", "ncF"),
    __BUILTIN_NANS(BI__builtin_nans, "__builtin_nans", "dcC*", "ncF"),
    __BUILTIN_NANSF(BI__builtin_nansf, "__builtin_nansf", "fcC*", "ncF"),
    __BUILTIN_NANSL(BI__builtin_nansl, "__builtin_nansl", "LdcC*", "ncF"),
    __BUILTIN_ABS(BI__builtin_abs, "__builtin_abs", "ii", "ncF"),
    __BUILTIN_FABS(BI__builtin_fabs, "__builtin_fabs", "dd", "ncF"),
    __BUILTIN_FABSF(BI__builtin_fabsf, "__builtin_fabsf", "ff", "ncF"),
    __BUILTIN_FABSL(BI__builtin_fabsl, "__builtin_fabsl", "LdLd", "ncF"),
    __BUILTIN_COPYSIGN(BI__builtin_copysign, "__builtin_copysign", "ddd", "ncF"),
    __BUILTIN_COPYSIGNF(BI__builtin_copysignf, "__builtin_copysignf", "fff", "ncF"),
    __BUILTIN_COPYSIGNL(BI__builtin_copysignl, "__builtin_copysignl", "LdLdLd", "ncF"),
    __BUILTIN_POWI(BI__builtin_powi, "__builtin_powi", "ddi", "nc"),
    __BUILTIN_POWIF(BI__builtin_powif, "__builtin_powif", "ffi", "nc"),
    __BUILTIN_POWIL(BI__builtin_powil, "__builtin_powil", "LdLdi", "nc"),
    __BUILTIN_ISGREATER(BI__builtin_isgreater, "__builtin_isgreater", "i.", "nc"),
    __BUILTIN_ISGREATEREQUAL(BI__builtin_isgreaterequal, "__builtin_isgreaterequal", "i.", "nc"),
    __BUILTIN_ISLESS(BI__builtin_isless, "__builtin_isless", "i.", "nc"),
    __BUILTIN_ISLESSEQUAL(BI__builtin_islessequal, "__builtin_islessequal", "i.", "nc"),
    __BUILTIN_ISLESSGREATER(BI__builtin_islessgreater, "__builtin_islessgreater", "i.", "nc"),
    __BUILTIN_ISUNORDERED(BI__builtin_isunordered, "__builtin_isunordered", "i.", "nc"),
    __BUILTIN_CLZ(BI__builtin_clz, "__builtin_clz", "iUi", "nc"),
    __BUILTIN_CLZL(BI__builtin_clzl, "__builtin_clzl", "iULi", "nc"),
    __BUILTIN_CLZLL(BI__builtin_clzll, "__builtin_clzll", "iULLi", "nc"),
    __BUILTIN_CTZ(BI__builtin_ctz, "__builtin_ctz", "iUi", "nc"),
    __BUILTIN_CTZL(BI__builtin_ctzl, "__builtin_ctzl", "iULi", "nc"),
    __BUILTIN_CTZLL(BI__builtin_ctzll, "__builtin_ctzll", "iULLi", "nc"),
    __BUILTIN_FFS(BI__builtin_ffs, "__builtin_ffs", "iUi", "nc"),
    __BUILTIN_FFSL(BI__builtin_ffsl, "__builtin_ffsl", "iULi", "nc"),
    __BUILTIN_FFSLL(BI__builtin_ffsll, "__builtin_ffsll", "iULLi", "nc"),
    __BUILTIN_PARITY(BI__builtin_parity, "__builtin_parity", "iUi", "nc"),
    __BUILTIN_PARITYL(BI__builtin_parityl, "__builtin_parityl", "iULi", "nc"),
    __BUILTIN_PARITYLL(BI__builtin_parityll, "__builtin_parityll", "iULLi", "nc"),
    __BUILTIN_POPCOUNT(BI__builtin_popcount, "__builtin_popcount", "iUi", "nc"),
    __BUILTIN_POPCOUNTL(BI__builtin_popcountl, "__builtin_popcountl", "iULi", "nc"),
    __BUILTIN_POPCOUNTLL(BI__builtin_popcountll, "__builtin_popcountll", "iULLi", "nc"),
    __BUILTIN_BSWAP32(BI__builtin_bswap32, "__builtin_bswap32", "UiUi", "nc"),
    __BUILTIN_BSWAP64(BI__builtin_bswap64, "__builtin_bswap64", "ULLiULLi", "nc"),
    __BUILTIN_CONSTANT_P(BI__builtin_constant_p, "__builtin_constant_p", "Us.", "nc"),
    __BUILTIN_CLASSIFY_TYPE(BI__builtin_classify_type, "__builtin_classify_type", "i.", "nc"),
    __BUILTIN___CFSTRINGMAKECONSTANTSTRING(BI__builtin___CFStringMakeConstantString, "__builtin___CFStringMakeConstantString", "FC*cC*", "nc"),
    __BUILTIN_VA_START(BI__builtin_va_start, "__builtin_va_start", "vA.", "n"),
    __BUILTIN_VA_END(BI__builtin_va_end, "__builtin_va_end", "vA", "n"),
    __BUILTIN_VA_COPY(BI__builtin_va_copy, "__builtin_va_copy", "vAA", "n"),
    __BUILTIN_STDARG_START(BI__builtin_stdarg_start, "__builtin_stdarg_start", "vA.", "n"),
    __BUILTIN_BCMP(BI__builtin_bcmp, "__builtin_bcmp", "iv*v*z", "n"),
    __BUILTIN_BCOPY(BI__builtin_bcopy, "__builtin_bcopy", "vv*v*z", "n"),
    __BUILTIN_BZERO(BI__builtin_bzero, "__builtin_bzero", "vv*z", "n"),
    __BUILTIN_MEMCMP(BI__builtin_memcmp, "__builtin_memcmp", "ivC*vC*z", "nF"),
    __BUILTIN_MEMCPY(BI__builtin_memcpy, "__builtin_memcpy", "v*v*vC*z", "nF"),
    __BUILTIN_MEMMOVE(BI__builtin_memmove, "__builtin_memmove", "v*v*vC*z", "nF"),
    __BUILTIN_MEMPCPY(BI__builtin_mempcpy, "__builtin_mempcpy", "v*v*vC*z", "nF"),
    __BUILTIN_MEMSET(BI__builtin_memset, "__builtin_memset", "v*v*iz", "nF"),
    __BUILTIN_STPCPY(BI__builtin_stpcpy, "__builtin_stpcpy", "c*c*cC*", "nF"),
    __BUILTIN_STPNCPY(BI__builtin_stpncpy, "__builtin_stpncpy", "c*c*cC*z", "nF"),
    __BUILTIN_STRCASECMP(BI__builtin_strcasecmp, "__builtin_strcasecmp", "icC*cC*", "nF"),
    __BUILTIN_STRCAT(BI__builtin_strcat, "__builtin_strcat", "c*c*cC*", "nF"),
    __BUILTIN_STRCHR(BI__builtin_strchr, "__builtin_strchr", "c*cC*i", "nF"),
    __BUILTIN_STRCMP(BI__builtin_strcmp, "__builtin_strcmp", "icC*cC*", "nF"),
    __BUILTIN_STRCPY(BI__builtin_strcpy, "__builtin_strcpy", "c*c*cC*", "nF"),
    __BUILTIN_STRCSPN(BI__builtin_strcspn, "__builtin_strcspn", "zcC*cC*", "nF"),
    __BUILTIN_STRDUP(BI__builtin_strdup, "__builtin_strdup", "c*cC*", "nF"),
    __BUILTIN_STRLEN(BI__builtin_strlen, "__builtin_strlen", "zcC*", "nF"),
    __BUILTIN_STRNCASECMP(BI__builtin_strncasecmp, "__builtin_strncasecmp", "icC*cC*z", "nF"),
    __BUILTIN_STRNCAT(BI__builtin_strncat, "__builtin_strncat", "c*c*cC*z", "nF"),
    __BUILTIN_STRNCMP(BI__builtin_strncmp, "__builtin_strncmp", "icC*cC*z", "nF"),
    __BUILTIN_STRNCPY(BI__builtin_strncpy, "__builtin_strncpy", "c*c*cC*z", "nF"),
    __BUILTIN_STRNDUP(BI__builtin_strndup, "__builtin_strndup", "c*cC*z", "nF"),
    __BUILTIN_STRPBRK(BI__builtin_strpbrk, "__builtin_strpbrk", "c*cC*cC*", "nF"),
    __BUILTIN_STRRCHR(BI__builtin_strrchr, "__builtin_strrchr", "c*cC*i", "nF"),
    __BUILTIN_STRSPN(BI__builtin_strspn, "__builtin_strspn", "zcC*cC*", "nF"),
    __BUILTIN_STRSTR(BI__builtin_strstr, "__builtin_strstr", "c*cC*cC*", "nF"),
    __BUILTIN_RETURN_ADDRESS(BI__builtin_return_address, "__builtin_return_address", "v*Ui", "n"),
    __BUILTIN_EXTRACT_RETURN_ADDR(BI__builtin_extract_return_addr, "__builtin_extract_return_addr", "v*v*", "n"),
    __BUILTIN_FRAME_ADDRESS(BI__builtin_frame_address, "__builtin_frame_address", "v*Ui", "n"),
    __BUILTIN_FLT_ROUNDS(BI__builtin_flt_rounds, "__builtin_flt_rounds", "i", "nc"),
    __BUILTIN_SETJMP(BI__builtin_setjmp, "__builtin_setjmp", "iv**", ""),
    __BUILTIN_LONGJMP(BI__builtin_longjmp, "__builtin_longjmp", "vv**i", ""),
    __BUILTIN_UNWIND_INIT(BI__builtin_unwind_init, "__builtin_unwind_init", "v", ""),
    __BUILTIN_OBJECT_SIZE(BI__builtin_object_size, "__builtin_object_size", "zv*i", "n"),
    __BUILTIN___MEMCPY_CHK(BI__builtin___memcpy_chk, "__builtin___memcpy_chk", "v*v*vC*zz", "nF"),
    __BUILTIN___MEMMOVE_CHK(BI__builtin___memmove_chk, "__builtin___memmove_chk", "v*v*vC*zz", "nF"),
    __BUILTIN___MEMPCPY_CHK(BI__builtin___mempcpy_chk, "__builtin___mempcpy_chk", "v*v*vC*zz", "nF"),
    __BUILTIN___MEMSET_CHK(BI__builtin___memset_chk, "__builtin___memset_chk", "v*v*izz", "nF"),
    __BUILTIN___STPCPY_CHK(BI__builtin___stpcpy_chk, "__builtin___stpcpy_chk", "c*c*cC*z", "nF"),
    __BUILTIN___STRCAT_CHK(BI__builtin___strcat_chk, "__builtin___strcat_chk", "c*c*cC*z", "nF"),
    __BUILTIN___STRCPY_CHK(BI__builtin___strcpy_chk, "__builtin___strcpy_chk", "c*c*cC*z", "nF"),
    __BUILTIN___STRNCAT_CHK(BI__builtin___strncat_chk, "__builtin___strncat_chk", "c*c*cC*zz", "nF"),
    __BUILTIN___STRNCPY_CHK(BI__builtin___strncpy_chk, "__builtin___strncpy_chk", "c*c*cC*zz", "nF"),
    __BUILTIN___SNPRINTF_CHK(BI__builtin___snprintf_chk, "__builtin___snprintf_chk", "ic*zizcC*.", "Fp:4:"),
    __BUILTIN___SPRINTF_CHK(BI__builtin___sprintf_chk, "__builtin___sprintf_chk", "ic*izcC*.", "Fp:3:"),
    __BUILTIN___VSNPRINTF_CHK(BI__builtin___vsnprintf_chk, "__builtin___vsnprintf_chk", "ic*zizcC*a", "FP:4:"),
    __BUILTIN___VSPRINTF_CHK(BI__builtin___vsprintf_chk, "__builtin___vsprintf_chk", "ic*izcC*a", "FP:3:"),
    __BUILTIN___FPRINTF_CHK(BI__builtin___fprintf_chk, "__builtin___fprintf_chk", "iP*icC*.", "Fp:2:"),
    __BUILTIN___PRINTF_CHK(BI__builtin___printf_chk, "__builtin___printf_chk", "iicC*.", "Fp:1:"),
    __BUILTIN___VFPRINTF_CHK(BI__builtin___vfprintf_chk, "__builtin___vfprintf_chk", "iP*icC*a", "FP:2:"),
    __BUILTIN___VPRINTF_CHK(BI__builtin___vprintf_chk, "__builtin___vprintf_chk", "iicC*a", "FP:1:"),
    __BUILTIN_EXPECT(BI__builtin_expect, "__builtin_expect", "iii", "nc"),
    __BUILTIN_PREFETCH(BI__builtin_prefetch, "__builtin_prefetch", "vvC*.", "nc"),
    __BUILTIN_TRAP(BI__builtin_trap, "__builtin_trap", "v", "n"),
    __BUILTIN_SHUFFLEVECTOR(BI__builtin_shufflevector, "__builtin_shufflevector", "v.", "nc"),
    __BUILTIN_ALLOCA(BI__builtin_alloca, "__builtin_alloca", "v*z", "n"),
    __SYNC_FETCH_AND_ADD(BI__sync_fetch_and_add, "__sync_fetch_and_add", "v.", ""),
    __SYNC_FETCH_AND_ADD_1(BI__sync_fetch_and_add_1, "__sync_fetch_and_add_1", "cc*c.", "n"),
    __SYNC_FETCH_AND_ADD_2(BI__sync_fetch_and_add_2, "__sync_fetch_and_add_2", "ss*s.", "n"),
    __SYNC_FETCH_AND_ADD_4(BI__sync_fetch_and_add_4, "__sync_fetch_and_add_4", "ii*i.", "n"),
    __SYNC_FETCH_AND_ADD_8(BI__sync_fetch_and_add_8, "__sync_fetch_and_add_8", "LLiLLi*LLi.", "n"),
    __SYNC_FETCH_AND_ADD_16(BI__sync_fetch_and_add_16, "__sync_fetch_and_add_16", "LLLiLLLi*LLLi.", "n"),
    __SYNC_FETCH_AND_SUB(BI__sync_fetch_and_sub, "__sync_fetch_and_sub", "v.", ""),
    __SYNC_FETCH_AND_SUB_1(BI__sync_fetch_and_sub_1, "__sync_fetch_and_sub_1", "cc*c.", "n"),
    __SYNC_FETCH_AND_SUB_2(BI__sync_fetch_and_sub_2, "__sync_fetch_and_sub_2", "ss*s.", "n"),
    __SYNC_FETCH_AND_SUB_4(BI__sync_fetch_and_sub_4, "__sync_fetch_and_sub_4", "ii*i.", "n"),
    __SYNC_FETCH_AND_SUB_8(BI__sync_fetch_and_sub_8, "__sync_fetch_and_sub_8", "LLiLLi*LLi.", "n"),
    __SYNC_FETCH_AND_SUB_16(BI__sync_fetch_and_sub_16, "__sync_fetch_and_sub_16", "LLLiLLLi*LLLi.", "n"),
    __SYNC_FETCH_AND_OR(BI__sync_fetch_and_or, "__sync_fetch_and_or", "v.", ""),
    __SYNC_FETCH_AND_OR_1(BI__sync_fetch_and_or_1, "__sync_fetch_and_or_1", "cc*c.", "n"),
    __SYNC_FETCH_AND_OR_2(BI__sync_fetch_and_or_2, "__sync_fetch_and_or_2", "ss*s.", "n"),
    __SYNC_FETCH_AND_OR_4(BI__sync_fetch_and_or_4, "__sync_fetch_and_or_4", "ii*i.", "n"),
    __SYNC_FETCH_AND_OR_8(BI__sync_fetch_and_or_8, "__sync_fetch_and_or_8", "LLiLLi*LLi.", "n"),
    __SYNC_FETCH_AND_OR_16(BI__sync_fetch_and_or_16, "__sync_fetch_and_or_16", "LLLiLLLi*LLLi.", "n"),
    __SYNC_FETCH_AND_AND(BI__sync_fetch_and_and, "__sync_fetch_and_and", "v.", ""),
    __SYNC_FETCH_AND_AND_1(BI__sync_fetch_and_and_1, "__sync_fetch_and_and_1", "cc*c.", "n"),
    __SYNC_FETCH_AND_AND_2(BI__sync_fetch_and_and_2, "__sync_fetch_and_and_2", "ss*s.", "n"),
    __SYNC_FETCH_AND_AND_4(BI__sync_fetch_and_and_4, "__sync_fetch_and_and_4", "ii*i.", "n"),
    __SYNC_FETCH_AND_AND_8(BI__sync_fetch_and_and_8, "__sync_fetch_and_and_8", "LLiLLi*LLi.", "n"),
    __SYNC_FETCH_AND_AND_16(BI__sync_fetch_and_and_16, "__sync_fetch_and_and_16", "LLLiLLLi*LLLi.", "n"),
    __SYNC_FETCH_AND_XOR(BI__sync_fetch_and_xor, "__sync_fetch_and_xor", "v.", ""),
    __SYNC_FETCH_AND_XOR_1(BI__sync_fetch_and_xor_1, "__sync_fetch_and_xor_1", "cc*c.", "n"),
    __SYNC_FETCH_AND_XOR_2(BI__sync_fetch_and_xor_2, "__sync_fetch_and_xor_2", "ss*s.", "n"),
    __SYNC_FETCH_AND_XOR_4(BI__sync_fetch_and_xor_4, "__sync_fetch_and_xor_4", "ii*i.", "n"),
    __SYNC_FETCH_AND_XOR_8(BI__sync_fetch_and_xor_8, "__sync_fetch_and_xor_8", "LLiLLi*LLi.", "n"),
    __SYNC_FETCH_AND_XOR_16(BI__sync_fetch_and_xor_16, "__sync_fetch_and_xor_16", "LLLiLLLi*LLLi.", "n"),
    __SYNC_FETCH_AND_NAND(BI__sync_fetch_and_nand, "__sync_fetch_and_nand", "v.", ""),
    __SYNC_FETCH_AND_NAND_1(BI__sync_fetch_and_nand_1, "__sync_fetch_and_nand_1", "cc*c.", "n"),
    __SYNC_FETCH_AND_NAND_2(BI__sync_fetch_and_nand_2, "__sync_fetch_and_nand_2", "ss*s.", "n"),
    __SYNC_FETCH_AND_NAND_4(BI__sync_fetch_and_nand_4, "__sync_fetch_and_nand_4", "ii*i.", "n"),
    __SYNC_FETCH_AND_NAND_8(BI__sync_fetch_and_nand_8, "__sync_fetch_and_nand_8", "LLiLLi*LLi.", "n"),
    __SYNC_FETCH_AND_NAND_16(BI__sync_fetch_and_nand_16, "__sync_fetch_and_nand_16", "LLLiLLLi*LLLi.", "n"),
    __SYNC_ADD_AND_FETCH(BI__sync_add_and_fetch, "__sync_add_and_fetch", "v.", ""),
    __SYNC_ADD_AND_FETCH_1(BI__sync_add_and_fetch_1, "__sync_add_and_fetch_1", "cc*c.", "n"),
    __SYNC_ADD_AND_FETCH_2(BI__sync_add_and_fetch_2, "__sync_add_and_fetch_2", "ss*s.", "n"),
    __SYNC_ADD_AND_FETCH_4(BI__sync_add_and_fetch_4, "__sync_add_and_fetch_4", "ii*i.", "n"),
    __SYNC_ADD_AND_FETCH_8(BI__sync_add_and_fetch_8, "__sync_add_and_fetch_8", "LLiLLi*LLi.", "n"),
    __SYNC_ADD_AND_FETCH_16(BI__sync_add_and_fetch_16, "__sync_add_and_fetch_16", "LLLiLLLi*LLLi.", "n"),
    __SYNC_SUB_AND_FETCH(BI__sync_sub_and_fetch, "__sync_sub_and_fetch", "v.", ""),
    __SYNC_SUB_AND_FETCH_1(BI__sync_sub_and_fetch_1, "__sync_sub_and_fetch_1", "cc*c.", "n"),
    __SYNC_SUB_AND_FETCH_2(BI__sync_sub_and_fetch_2, "__sync_sub_and_fetch_2", "ss*s.", "n"),
    __SYNC_SUB_AND_FETCH_4(BI__sync_sub_and_fetch_4, "__sync_sub_and_fetch_4", "ii*i.", "n"),
    __SYNC_SUB_AND_FETCH_8(BI__sync_sub_and_fetch_8, "__sync_sub_and_fetch_8", "LLiLLi*LLi.", "n"),
    __SYNC_SUB_AND_FETCH_16(BI__sync_sub_and_fetch_16, "__sync_sub_and_fetch_16", "LLLiLLLi*LLLi.", "n"),
    __SYNC_OR_AND_FETCH(BI__sync_or_and_fetch, "__sync_or_and_fetch", "v.", ""),
    __SYNC_OR_AND_FETCH_1(BI__sync_or_and_fetch_1, "__sync_or_and_fetch_1", "cc*c.", "n"),
    __SYNC_OR_AND_FETCH_2(BI__sync_or_and_fetch_2, "__sync_or_and_fetch_2", "ss*s.", "n"),
    __SYNC_OR_AND_FETCH_4(BI__sync_or_and_fetch_4, "__sync_or_and_fetch_4", "ii*i.", "n"),
    __SYNC_OR_AND_FETCH_8(BI__sync_or_and_fetch_8, "__sync_or_and_fetch_8", "LLiLLi*LLi.", "n"),
    __SYNC_OR_AND_FETCH_16(BI__sync_or_and_fetch_16, "__sync_or_and_fetch_16", "LLLiLLLi*LLLi.", "n"),
    __SYNC_AND_AND_FETCH(BI__sync_and_and_fetch, "__sync_and_and_fetch", "v.", ""),
    __SYNC_AND_AND_FETCH_1(BI__sync_and_and_fetch_1, "__sync_and_and_fetch_1", "cc*c.", "n"),
    __SYNC_AND_AND_FETCH_2(BI__sync_and_and_fetch_2, "__sync_and_and_fetch_2", "ss*s.", "n"),
    __SYNC_AND_AND_FETCH_4(BI__sync_and_and_fetch_4, "__sync_and_and_fetch_4", "ii*i.", "n"),
    __SYNC_AND_AND_FETCH_8(BI__sync_and_and_fetch_8, "__sync_and_and_fetch_8", "LLiLLi*LLi.", "n"),
    __SYNC_AND_AND_FETCH_16(BI__sync_and_and_fetch_16, "__sync_and_and_fetch_16", "LLLiLLLi*LLLi.", "n"),
    __SYNC_XOR_AND_FETCH(BI__sync_xor_and_fetch, "__sync_xor_and_fetch", "v.", ""),
    __SYNC_XOR_AND_FETCH_1(BI__sync_xor_and_fetch_1, "__sync_xor_and_fetch_1", "cc*c.", "n"),
    __SYNC_XOR_AND_FETCH_2(BI__sync_xor_and_fetch_2, "__sync_xor_and_fetch_2", "ss*s.", "n"),
    __SYNC_XOR_AND_FETCH_4(BI__sync_xor_and_fetch_4, "__sync_xor_and_fetch_4", "ii*i.", "n"),
    __SYNC_XOR_AND_FETCH_8(BI__sync_xor_and_fetch_8, "__sync_xor_and_fetch_8", "LLiLLi*LLi.", "n"),
    __SYNC_XOR_AND_FETCH_16(BI__sync_xor_and_fetch_16, "__sync_xor_and_fetch_16", "LLLiLLLi*LLLi.", "n"),
    __SYNC_NAND_AND_FETCH(BI__sync_nand_and_fetch, "__sync_nand_and_fetch", "v.", ""),
    __SYNC_NAND_AND_FETCH_1(BI__sync_nand_and_fetch_1, "__sync_nand_and_fetch_1", "cc*c.", "n"),
    __SYNC_NAND_AND_FETCH_2(BI__sync_nand_and_fetch_2, "__sync_nand_and_fetch_2", "ss*s.", "n"),
    __SYNC_NAND_AND_FETCH_4(BI__sync_nand_and_fetch_4, "__sync_nand_and_fetch_4", "ii*i.", "n"),
    __SYNC_NAND_AND_FETCH_8(BI__sync_nand_and_fetch_8, "__sync_nand_and_fetch_8", "LLiLLi*LLi.", "n"),
    __SYNC_NAND_AND_FETCH_16(BI__sync_nand_and_fetch_16, "__sync_nand_and_fetch_16", "LLLiLLLi*LLLi.", "n"),
    __SYNC_BOOL_COMPARE_AND_SWAP(BI__sync_bool_compare_and_swap, "__sync_bool_compare_and_swap", "v.", ""),
    __SYNC_BOOL_COMPARE_AND_SWAP_1(BI__sync_bool_compare_and_swap_1, "__sync_bool_compare_and_swap_1", "bc*cc.", "n"),
    __SYNC_BOOL_COMPARE_AND_SWAP_2(BI__sync_bool_compare_and_swap_2, "__sync_bool_compare_and_swap_2", "bs*ss.", "n"),
    __SYNC_BOOL_COMPARE_AND_SWAP_4(BI__sync_bool_compare_and_swap_4, "__sync_bool_compare_and_swap_4", "bi*ii.", "n"),
    __SYNC_BOOL_COMPARE_AND_SWAP_8(BI__sync_bool_compare_and_swap_8, "__sync_bool_compare_and_swap_8", "bLLi*LLi.", "n"),
    __SYNC_BOOL_COMPARE_AND_SWAP_16(BI__sync_bool_compare_and_swap_16, "__sync_bool_compare_and_swap_16", "bLLLi*LLLiLLLi.", "n"),
    __SYNC_VAL_COMPARE_AND_SWAP(BI__sync_val_compare_and_swap, "__sync_val_compare_and_swap", "v.", ""),
    __SYNC_VAL_COMPARE_AND_SWAP_1(BI__sync_val_compare_and_swap_1, "__sync_val_compare_and_swap_1", "cc*cc.", "n"),
    __SYNC_VAL_COMPARE_AND_SWAP_2(BI__sync_val_compare_and_swap_2, "__sync_val_compare_and_swap_2", "ss*ss.", "n"),
    __SYNC_VAL_COMPARE_AND_SWAP_4(BI__sync_val_compare_and_swap_4, "__sync_val_compare_and_swap_4", "ii*ii.", "n"),
    __SYNC_VAL_COMPARE_AND_SWAP_8(BI__sync_val_compare_and_swap_8, "__sync_val_compare_and_swap_8", "LLiLLi*LLi.", "n"),
    __SYNC_VAL_COMPARE_AND_SWAP_16(BI__sync_val_compare_and_swap_16, "__sync_val_compare_and_swap_16", "LLLiLLLi*LLLiLLLi.", "n"),
    __SYNC_LOCK_TEST_AND_SET(BI__sync_lock_test_and_set, "__sync_lock_test_and_set", "v.", ""),
    __SYNC_LOCK_TEST_AND_SET_1(BI__sync_lock_test_and_set_1, "__sync_lock_test_and_set_1", "cc*c.", "n"),
    __SYNC_LOCK_TEST_AND_SET_2(BI__sync_lock_test_and_set_2, "__sync_lock_test_and_set_2", "ss*s.", "n"),
    __SYNC_LOCK_TEST_AND_SET_4(BI__sync_lock_test_and_set_4, "__sync_lock_test_and_set_4", "ii*i.", "n"),
    __SYNC_LOCK_TEST_AND_SET_8(BI__sync_lock_test_and_set_8, "__sync_lock_test_and_set_8", "LLiLLi*LLi.", "n"),
    __SYNC_LOCK_TEST_AND_SET_16(BI__sync_lock_test_and_set_16, "__sync_lock_test_and_set_16", "LLLiLLLi*LLLi.", "n"),
    __SYNC_LOCK_RELEASE(BI__sync_lock_release, "__sync_lock_release", "v.", ""),
    __SYNC_LOCK_RELEASE_1(BI__sync_lock_release_1, "__sync_lock_release_1", "vc*.", "n"),
    __SYNC_LOCK_RELEASE_2(BI__sync_lock_release_2, "__sync_lock_release_2", "vs*.", "n"),
    __SYNC_LOCK_RELEASE_4(BI__sync_lock_release_4, "__sync_lock_release_4", "vi*.", "n"),
    __SYNC_LOCK_RELEASE_8(BI__sync_lock_release_8, "__sync_lock_release_8", "vLLi*.", "n"),
    __SYNC_LOCK_RELEASE_16(BI__sync_lock_release_16, "__sync_lock_release_16", "vLLLi*.", "n"),
    __SYNC_SYNCHRONIZE(BI__sync_synchronize, "__sync_synchronize", "v.", "n"),
    __BUILTIN_LLVM_MEMORY_BARRIER(BI__builtin_llvm_memory_barrier, "__builtin_llvm_memory_barrier", "vbbbbb", "n"),
    __SYNC_FETCH_AND_MIN(BI__sync_fetch_and_min, "__sync_fetch_and_min", "ii*i", "n"),
    __SYNC_FETCH_AND_MAX(BI__sync_fetch_and_max, "__sync_fetch_and_max", "ii*i", "n"),
    __SYNC_FETCH_AND_UMIN(BI__sync_fetch_and_umin, "__sync_fetch_and_umin", "UiUi*Ui", "n"),
    __SYNC_FETCH_AND_UMAX(BI__sync_fetch_and_umax, "__sync_fetch_and_umax", "UiUi*Ui", "n");

    int id;
    String name;
    String type;
    String attr;

    BUILTIN(int id, String name, String type, String attr) {
      this.id = id;
      this.name = name;
      this.type = type;
      this.attr = attr;
    }

  }

  enum LIBBUILTIN {
    CALLOC(BIcalloc, "calloc", "v*zz", "f", "stdlib.h"),
    EXIT(BIexit, "exit", "vi", "fr", "stdlib.h"),
    _EXIT(BI_Exit, "_Exit", "vi", "fr", "stdlib.h"),
    MALLOC(BImalloc, "malloc", "v*z", "f", "stdlib.h"),
    REALLOC(BIrealloc, "realloc", "v*v*z", "f", "stdlib.h"),
    MEMCPY(BImemcpy, "memcpy", "v*v*vC*z", "f", "string.h"),
    MEMMOVE(BImemmove, "memmove", "v*v*vC*z", "f", "string.h"),
    STRCPY(BIstrcpy, "strcpy", "c*c*cC*", "f", "string.h"),
    STRNCPY(BIstrncpy, "strncpy", "c*c*cC*z", "f", "string.h"),
    STRCAT(BIstrcat, "strcat", "c*c*cC*", "f", "string.h"),
    STRNCAT(BIstrncat, "strncat", "c*c*cC*z", "f", "string.h"),
    STRXFRM(BIstrxfrm, "strxfrm", "zc*cC*z", "f", "string.h"),
    MEMCHR(BImemchr, "memchr", "v*vC*iz", "f", "string.h"),
    STRCHR(BIstrchr, "strchr", "c*cC*i", "f", "string.h"),
    STRCSPN(BIstrcspn, "strcspn", "zcC*cC*", "f", "string.h"),
    STRPBRK(BIstrpbrk, "strpbrk", "c*cC*cC*", "f", "string.h"),
    STRRCHR(BIstrrchr, "strrchr", "c*cC*i", "f", "string.h"),
    STRSPN(BIstrspn, "strspn", "zcC*cC*", "f", "string.h"),
    STRSTR(BIstrstr, "strstr", "c*cC*cC*", "f", "string.h"),
    STRTOK(BIstrtok, "strtok", "c*c*cC*", "f", "string.h"),
    MEMSET(BImemset, "memset", "v*v*iz", "f", "string.h"),
    STRERROR(BIstrerror, "strerror", "c*i", "f", "string.h"),
    STRLEN(BIstrlen, "strlen", "zcC*", "f", "string.h"),
    PRINTF(BIprintf, "printf", "icC*.", "fp:0:", "stdio.h"),
    FPRINTF(BIfprintf, "fprintf", "iP*cC*.", "fp:1:", "stdio.h"),
    SNPRINTF(BIsnprintf, "snprintf", "ic*zcC*.", "fp:2:", "stdio.h"),
    SPRINTF(BIsprintf, "sprintf", "ic*cC*.", "fp:1:", "stdio.h"),
    VPRINTF(BIvprintf, "vprintf", "icC*a", "fP:0:", "stdio.h"),
    VFPRINTF(BIvfprintf, "vfprintf", "i.", "fP:1:", "stdio.h"),
    VSNPRINTF(BIvsnprintf, "vsnprintf", "ic*zcC*a", "fP:2:", "stdio.h"),
    VSPRINTF(BIvsprintf, "vsprintf", "ic*cC*a", "fP:1:", "stdio.h"),
    LONGJMP(BIlongjmp, "longjmp", "vJi", "fr", "setjmp.h"),
    ALLOCA(BIalloca, "alloca", "v*z", "f", "stdlib.h"),
    STPCPY(BIstpcpy, "stpcpy", "c*c*cC*", "f", "string.h"),
    STPNCPY(BIstpncpy, "stpncpy", "c*c*cC*z", "f", "string.h"),
    STRDUP(BIstrdup, "strdup", "c*cC*", "f", "string.h"),
    STRNDUP(BIstrndup, "strndup", "c*cC*z", "f", "string.h"),
    INDEX(BIindex, "index", "c*cC*i", "f", "strings.h"),
    RINDEX(BIrindex, "rindex", "c*cC*i", "f", "strings.h"),
    _EXIT_U(BI_exit, "_exit", "vi", "fr", "unistd.h"),
    _LONGJMP(BI_longjmp, "_longjmp", "vJi", "fr", "setjmp.h"),
    SIGLONGJMP(BIsiglongjmp, "siglongjmp", "vSJi", "fr", "setjmp.h"),
    OBJC_MSGSEND(BIobjc_msgSend, "objc_msgSend", "v*.", "f", "objc/message.h"),
    POW(BIpow, "pow", "ddd", "fe", "math.h"),
    POWL(BIpowl, "powl", "LdLdLd", "fe", "math.h"),
    POWF(BIpowf, "powf", "fff", "fe", "math.h"),
    SQRT(BIsqrt, "sqrt", "dd", "fe", "math.h"),
    SQRTL(BIsqrtl, "sqrtl", "LdLd", "fe", "math.h"),
    SQRTF(BIsqrtf, "sqrtf", "ff", "fe", "math.h"),
    SIN(BIsin, "sin", "dd", "fe", "math.h"),
    SINL(BIsinl, "sinl", "LdLd", "fe", "math.h"),
    SINF(BIsinf, "sinf", "ff", "fe", "math.h"),
    COS(BIcos, "cos", "dd", "fe", "math.h"),
    COSL(BIcosl, "cosl", "LdLd", "fe", "math.h"),
    COSF(BIcosf, "cosf", "ff", "fe", "math.h");


    int id;
    String name;
    String type;
    String attr;
    String header;

    LIBBUILTIN(int id, String name,
               String type, String attr,
               String header) {
      this.id = id;
      this.name = name;
      this.type = type;
      this.attr = attr;
      this.header = header;
    }
  }

}
