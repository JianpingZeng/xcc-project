/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package backend.codegen;
/**
 * This enum defines all of the runtime library calls that backend can emit.
 * @author Xlous.zeng
 * @version 0.1
 */
public enum RTLIB
{
    // Integer
    SHL_I16,
    SHL_I32,
    SHL_I64,
    SHL_I128,
    SRL_I16,
    SRL_I32,
    SRL_I64,
    SRL_I128,
    SRA_I16,
    SRA_I32,
    SRA_I64,
    SRA_I128,
    MUL_I16,
    MUL_I32,
    MUL_I64,
    MUL_I128,
    SDIV_I16,
    SDIV_I32,
    SDIV_I64,
    SDIV_I128,
    UDIV_I16,
    UDIV_I32,
    UDIV_I64,
    UDIV_I128,
    SREM_I16,
    SREM_I32,
    SREM_I64,
    SREM_I128,
    UREM_I16,
    UREM_I32,
    UREM_I64,
    UREM_I128,
    NEG_I32,
    NEG_I64,

    // FLOATING POINT
    ADD_F32,
    ADD_F64,
    ADD_F80,
    ADD_PPCF128,
    SUB_F32,
    SUB_F64,
    SUB_F80,
    SUB_PPCF128,
    MUL_F32,
    MUL_F64,
    MUL_F80,
    MUL_PPCF128,
    DIV_F32,
    DIV_F64,
    DIV_F80,
    DIV_PPCF128,
    REM_F32,
    REM_F64,
    REM_F80,
    REM_PPCF128,
    POWI_F32,
    POWI_F64,
    POWI_F80,
    POWI_PPCF128,
    SQRT_F32,
    SQRT_F64,
    SQRT_F80,
    SQRT_PPCF128,
    LOG_F32,
    LOG_F64,
    LOG_F80,
    LOG_PPCF128,
    LOG2_F32,
    LOG2_F64,
    LOG2_F80,
    LOG2_PPCF128,
    LOG10_F32,
    LOG10_F64,
    LOG10_F80,
    LOG10_PPCF128,
    EXP_F32,
    EXP_F64,
    EXP_F80,
    EXP_PPCF128,
    EXP2_F32,
    EXP2_F64,
    EXP2_F80,
    EXP2_PPCF128,
    SIN_F32,
    SIN_F64,
    SIN_F80,
    SIN_PPCF128,
    COS_F32,
    COS_F64,
    COS_F80,
    COS_PPCF128,
    POW_F32,
    POW_F64,
    POW_F80,
    POW_PPCF128,
    CEIL_F32,
    CEIL_F64,
    CEIL_F80,
    CEIL_PPCF128,
    TRUNC_F32,
    TRUNC_F64,
    TRUNC_F80,
    TRUNC_PPCF128,
    RINT_F32,
    RINT_F64,
    RINT_F80,
    RINT_PPCF128,
    NEARBYINT_F32,
    NEARBYINT_F64,
    NEARBYINT_F80,
    NEARBYINT_PPCF128,
    FLOOR_F32,
    FLOOR_F64,
    FLOOR_F80,
    FLOOR_PPCF128,

    // CONVERSION
    FPEXT_F32_F64,
    FPROUND_F64_F32,
    FPROUND_F80_F32,
    FPROUND_PPCF128_F32,
    FPROUND_F80_F64,
    FPROUND_PPCF128_F64,
    FPTOSINT_F32_I8,
    FPTOSINT_F32_I16,
    FPTOSINT_F32_I32,
    FPTOSINT_F32_I64,
    FPTOSINT_F32_I128,
    FPTOSINT_F64_I32,
    FPTOSINT_F64_I64,
    FPTOSINT_F64_I128,
    FPTOSINT_F80_I32,
    FPTOSINT_F80_I64,
    FPTOSINT_F80_I128,
    FPTOSINT_PPCF128_I32,
    FPTOSINT_PPCF128_I64,
    FPTOSINT_PPCF128_I128,
    FPTOUINT_F32_I8,
    FPTOUINT_F32_I16,
    FPTOUINT_F32_I32,
    FPTOUINT_F32_I64,
    FPTOUINT_F32_I128,
    FPTOUINT_F64_I32,
    FPTOUINT_F64_I64,
    FPTOUINT_F64_I128,
    FPTOUINT_F80_I32,
    FPTOUINT_F80_I64,
    FPTOUINT_F80_I128,
    FPTOUINT_PPCF128_I32,
    FPTOUINT_PPCF128_I64,
    FPTOUINT_PPCF128_I128,
    SINTTOFP_I32_F32,
    SINTTOFP_I32_F64,
    SINTTOFP_I32_F80,
    SINTTOFP_I32_PPCF128,
    SINTTOFP_I64_F32,
    SINTTOFP_I64_F64,
    SINTTOFP_I64_F80,
    SINTTOFP_I64_PPCF128,
    SINTTOFP_I128_F32,
    SINTTOFP_I128_F64,
    SINTTOFP_I128_F80,
    SINTTOFP_I128_PPCF128,
    UINTTOFP_I32_F32,
    UINTTOFP_I32_F64,
    UINTTOFP_I32_F80,
    UINTTOFP_I32_PPCF128,
    UINTTOFP_I64_F32,
    UINTTOFP_I64_F64,
    UINTTOFP_I64_F80,
    UINTTOFP_I64_PPCF128,
    UINTTOFP_I128_F32,
    UINTTOFP_I128_F64,
    UINTTOFP_I128_F80,
    UINTTOFP_I128_PPCF128,

    // COMPARISON
    OEQ_F32,
    OEQ_F64,
    UNE_F32,
    UNE_F64,
    OGE_F32,
    OGE_F64,
    OLT_F32,
    OLT_F64,
    OLE_F32,
    OLE_F64,
    OGT_F32,
    OGT_F64,
    UO_F32,
    UO_F64,
    O_F32,
    O_F64,

    // MEMORY
    MEMCPY,
    MEMSET,
    MEMMOVE,

    // EXCEPTION HANDLING
    UNWIND_RESUME,

    UNKNOWN_LIBCALL
}
