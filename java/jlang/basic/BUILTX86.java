package jlang.basic;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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

import static jlang.basic.BuiltIDX86.*;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public enum BUILTX86 {
  __BUILTIN_IA32_EMMS(BI__builtin_ia32_emms, "__builtin_ia32_emms", "v", ""),
  __BUILTIN_IA32_COMIEQ(BI__builtin_ia32_comieq, "__builtin_ia32_comieq", "iV4fV4f", ""),
  __BUILTIN_IA32_COMILT(BI__builtin_ia32_comilt, "__builtin_ia32_comilt", "iV4fV4f", ""),
  __BUILTIN_IA32_COMILE(BI__builtin_ia32_comile, "__builtin_ia32_comile", "iV4fV4f", ""),
  __BUILTIN_IA32_COMIGT(BI__builtin_ia32_comigt, "__builtin_ia32_comigt", "iV4fV4f", ""),
  __BUILTIN_IA32_COMIGE(BI__builtin_ia32_comige, "__builtin_ia32_comige", "iV4fV4f", ""),
  __BUILTIN_IA32_COMINEQ(BI__builtin_ia32_comineq, "__builtin_ia32_comineq", "iV4fV4f", ""),
  __BUILTIN_IA32_UCOMIEQ(BI__builtin_ia32_ucomieq, "__builtin_ia32_ucomieq", "iV4fV4f", ""),
  __BUILTIN_IA32_UCOMILT(BI__builtin_ia32_ucomilt, "__builtin_ia32_ucomilt", "iV4fV4f", ""),
  __BUILTIN_IA32_UCOMILE(BI__builtin_ia32_ucomile, "__builtin_ia32_ucomile", "iV4fV4f", ""),
  __BUILTIN_IA32_UCOMIGT(BI__builtin_ia32_ucomigt, "__builtin_ia32_ucomigt", "iV4fV4f", ""),
  __BUILTIN_IA32_UCOMIGE(BI__builtin_ia32_ucomige, "__builtin_ia32_ucomige", "iV4fV4f", ""),
  __BUILTIN_IA32_UCOMINEQ(BI__builtin_ia32_ucomineq, "__builtin_ia32_ucomineq", "iV4fV4f", ""),
  __BUILTIN_IA32_COMISDEQ(BI__builtin_ia32_comisdeq, "__builtin_ia32_comisdeq", "iV2dV2d", ""),
  __BUILTIN_IA32_COMISDLT(BI__builtin_ia32_comisdlt, "__builtin_ia32_comisdlt", "iV2dV2d", ""),
  __BUILTIN_IA32_COMISDLE(BI__builtin_ia32_comisdle, "__builtin_ia32_comisdle", "iV2dV2d", ""),
  __BUILTIN_IA32_COMISDGT(BI__builtin_ia32_comisdgt, "__builtin_ia32_comisdgt", "iV2dV2d", ""),
  __BUILTIN_IA32_COMISDGE(BI__builtin_ia32_comisdge, "__builtin_ia32_comisdge", "iV2dV2d", ""),
  __BUILTIN_IA32_COMISDNEQ(BI__builtin_ia32_comisdneq, "__builtin_ia32_comisdneq", "iV2dV2d", ""),
  __BUILTIN_IA32_UCOMISDEQ(BI__builtin_ia32_ucomisdeq, "__builtin_ia32_ucomisdeq", "iV2dV2d", ""),
  __BUILTIN_IA32_UCOMISDLT(BI__builtin_ia32_ucomisdlt, "__builtin_ia32_ucomisdlt", "iV2dV2d", ""),
  __BUILTIN_IA32_UCOMISDLE(BI__builtin_ia32_ucomisdle, "__builtin_ia32_ucomisdle", "iV2dV2d", ""),
  __BUILTIN_IA32_UCOMISDGT(BI__builtin_ia32_ucomisdgt, "__builtin_ia32_ucomisdgt", "iV2dV2d", ""),
  __BUILTIN_IA32_UCOMISDGE(BI__builtin_ia32_ucomisdge, "__builtin_ia32_ucomisdge", "iV2dV2d", ""),
  __BUILTIN_IA32_UCOMISDNEQ(BI__builtin_ia32_ucomisdneq, "__builtin_ia32_ucomisdneq", "iV2dV2d", ""),
  __BUILTIN_IA32_CMPPS(BI__builtin_ia32_cmpps, "__builtin_ia32_cmpps", "V4fV4fV4fc", ""),
  __BUILTIN_IA32_CMPSS(BI__builtin_ia32_cmpss, "__builtin_ia32_cmpss", "V4fV4fV4fc", ""),
  __BUILTIN_IA32_MINPS(BI__builtin_ia32_minps, "__builtin_ia32_minps", "V4fV4fV4f", ""),
  __BUILTIN_IA32_MAXPS(BI__builtin_ia32_maxps, "__builtin_ia32_maxps", "V4fV4fV4f", ""),
  __BUILTIN_IA32_MINSS(BI__builtin_ia32_minss, "__builtin_ia32_minss", "V4fV4fV4f", ""),
  __BUILTIN_IA32_MAXSS(BI__builtin_ia32_maxss, "__builtin_ia32_maxss", "V4fV4fV4f", ""),
  __BUILTIN_IA32_PADDSB(BI__builtin_ia32_paddsb, "__builtin_ia32_paddsb", "V8cV8cV8c", ""),
  __BUILTIN_IA32_PADDSW(BI__builtin_ia32_paddsw, "__builtin_ia32_paddsw", "V4sV4sV4s", ""),
  __BUILTIN_IA32_PSUBSB(BI__builtin_ia32_psubsb, "__builtin_ia32_psubsb", "V8cV8cV8c", ""),
  __BUILTIN_IA32_PSUBSW(BI__builtin_ia32_psubsw, "__builtin_ia32_psubsw", "V4sV4sV4s", ""),
  __BUILTIN_IA32_PADDUSB(BI__builtin_ia32_paddusb, "__builtin_ia32_paddusb", "V8cV8cV8c", ""),
  __BUILTIN_IA32_PADDUSW(BI__builtin_ia32_paddusw, "__builtin_ia32_paddusw", "V4sV4sV4s", ""),
  __BUILTIN_IA32_PSUBUSB(BI__builtin_ia32_psubusb, "__builtin_ia32_psubusb", "V8cV8cV8c", ""),
  __BUILTIN_IA32_PSUBUSW(BI__builtin_ia32_psubusw, "__builtin_ia32_psubusw", "V4sV4sV4s", ""),
  __BUILTIN_IA32_PMULHW(BI__builtin_ia32_pmulhw, "__builtin_ia32_pmulhw", "V4sV4sV4s", ""),
  __BUILTIN_IA32_PMULHUW(BI__builtin_ia32_pmulhuw, "__builtin_ia32_pmulhuw", "V4sV4sV4s", ""),
  __BUILTIN_IA32_PAVGB(BI__builtin_ia32_pavgb, "__builtin_ia32_pavgb", "V8cV8cV8c", ""),
  __BUILTIN_IA32_PAVGW(BI__builtin_ia32_pavgw, "__builtin_ia32_pavgw", "V4sV4sV4s", ""),
  __BUILTIN_IA32_PCMPEQB(BI__builtin_ia32_pcmpeqb, "__builtin_ia32_pcmpeqb", "V8cV8cV8c", ""),
  __BUILTIN_IA32_PCMPEQW(BI__builtin_ia32_pcmpeqw, "__builtin_ia32_pcmpeqw", "V4sV4sV4s", ""),
  __BUILTIN_IA32_PCMPEQD(BI__builtin_ia32_pcmpeqd, "__builtin_ia32_pcmpeqd", "V2iV2iV2i", ""),
  __BUILTIN_IA32_PCMPGTB(BI__builtin_ia32_pcmpgtb, "__builtin_ia32_pcmpgtb", "V8cV8cV8c", ""),
  __BUILTIN_IA32_PCMPGTW(BI__builtin_ia32_pcmpgtw, "__builtin_ia32_pcmpgtw", "V4sV4sV4s", ""),
  __BUILTIN_IA32_PCMPGTD(BI__builtin_ia32_pcmpgtd, "__builtin_ia32_pcmpgtd", "V2iV2iV2i", ""),
  __BUILTIN_IA32_PMAXUB(BI__builtin_ia32_pmaxub, "__builtin_ia32_pmaxub", "V8cV8cV8c", ""),
  __BUILTIN_IA32_PMAXSW(BI__builtin_ia32_pmaxsw, "__builtin_ia32_pmaxsw", "V4sV4sV4s", ""),
  __BUILTIN_IA32_PMINUB(BI__builtin_ia32_pminub, "__builtin_ia32_pminub", "V8cV8cV8c", ""),
  __BUILTIN_IA32_PMINSW(BI__builtin_ia32_pminsw, "__builtin_ia32_pminsw", "V4sV4sV4s", ""),
  __BUILTIN_IA32_PUNPCKHBW(BI__builtin_ia32_punpckhbw, "__builtin_ia32_punpckhbw", "V8cV8cV8c", ""),
  __BUILTIN_IA32_PUNPCKHWD(BI__builtin_ia32_punpckhwd, "__builtin_ia32_punpckhwd", "V4sV4sV4s", ""),
  __BUILTIN_IA32_PUNPCKHDQ(BI__builtin_ia32_punpckhdq, "__builtin_ia32_punpckhdq", "V2iV2iV2i", ""),
  __BUILTIN_IA32_PUNPCKLBW(BI__builtin_ia32_punpcklbw, "__builtin_ia32_punpcklbw", "V8cV8cV8c", ""),
  __BUILTIN_IA32_PUNPCKLWD(BI__builtin_ia32_punpcklwd, "__builtin_ia32_punpcklwd", "V4sV4sV4s", ""),
  __BUILTIN_IA32_PUNPCKLDQ(BI__builtin_ia32_punpckldq, "__builtin_ia32_punpckldq", "V2iV2iV2i", ""),
  __BUILTIN_IA32_CMPPD(BI__builtin_ia32_cmppd, "__builtin_ia32_cmppd", "V2dV2dV2dc", ""),
  __BUILTIN_IA32_CMPSD(BI__builtin_ia32_cmpsd, "__builtin_ia32_cmpsd", "V2dV2dV2dc", ""),
  __BUILTIN_IA32_MINPD(BI__builtin_ia32_minpd, "__builtin_ia32_minpd", "V2dV2dV2d", ""),
  __BUILTIN_IA32_MAXPD(BI__builtin_ia32_maxpd, "__builtin_ia32_maxpd", "V2dV2dV2d", ""),
  __BUILTIN_IA32_MINSD(BI__builtin_ia32_minsd, "__builtin_ia32_minsd", "V2dV2dV2d", ""),
  __BUILTIN_IA32_MAXSD(BI__builtin_ia32_maxsd, "__builtin_ia32_maxsd", "V2dV2dV2d", ""),
  __BUILTIN_IA32_PADDSB128(BI__builtin_ia32_paddsb128, "__builtin_ia32_paddsb128", "V16cV16cV16c", ""),
  __BUILTIN_IA32_PADDSW128(BI__builtin_ia32_paddsw128, "__builtin_ia32_paddsw128", "V8sV8sV8s", ""),
  __BUILTIN_IA32_PSUBSB128(BI__builtin_ia32_psubsb128, "__builtin_ia32_psubsb128", "V16cV16cV16c", ""),
  __BUILTIN_IA32_PSUBSW128(BI__builtin_ia32_psubsw128, "__builtin_ia32_psubsw128", "V8sV8sV8s", ""),
  __BUILTIN_IA32_PADDUSB128(BI__builtin_ia32_paddusb128, "__builtin_ia32_paddusb128", "V16cV16cV16c", ""),
  __BUILTIN_IA32_PADDUSW128(BI__builtin_ia32_paddusw128, "__builtin_ia32_paddusw128", "V8sV8sV8s", ""),
  __BUILTIN_IA32_PSUBUSB128(BI__builtin_ia32_psubusb128, "__builtin_ia32_psubusb128", "V16cV16cV16c", ""),
  __BUILTIN_IA32_PSUBUSW128(BI__builtin_ia32_psubusw128, "__builtin_ia32_psubusw128", "V8sV8sV8s", ""),
  __BUILTIN_IA32_PMULLW128(BI__builtin_ia32_pmullw128, "__builtin_ia32_pmullw128", "V8sV8sV8s", ""),
  __BUILTIN_IA32_PMULHW128(BI__builtin_ia32_pmulhw128, "__builtin_ia32_pmulhw128", "V8sV8sV8s", ""),
  __BUILTIN_IA32_PAVGB128(BI__builtin_ia32_pavgb128, "__builtin_ia32_pavgb128", "V16cV16cV16c", ""),
  __BUILTIN_IA32_PAVGW128(BI__builtin_ia32_pavgw128, "__builtin_ia32_pavgw128", "V8sV8sV8s", ""),
  __BUILTIN_IA32_PCMPEQB128(BI__builtin_ia32_pcmpeqb128, "__builtin_ia32_pcmpeqb128", "V16cV16cV16c", ""),
  __BUILTIN_IA32_PCMPEQW128(BI__builtin_ia32_pcmpeqw128, "__builtin_ia32_pcmpeqw128", "V8sV8sV8s", ""),
  __BUILTIN_IA32_PCMPEQD128(BI__builtin_ia32_pcmpeqd128, "__builtin_ia32_pcmpeqd128", "V4iV4iV4i", ""),
  __BUILTIN_IA32_PCMPGTB128(BI__builtin_ia32_pcmpgtb128, "__builtin_ia32_pcmpgtb128", "V16cV16cV16c", ""),
  __BUILTIN_IA32_PCMPGTW128(BI__builtin_ia32_pcmpgtw128, "__builtin_ia32_pcmpgtw128", "V8sV8sV8s", ""),
  __BUILTIN_IA32_PCMPGTD128(BI__builtin_ia32_pcmpgtd128, "__builtin_ia32_pcmpgtd128", "V4iV4iV4i", ""),
  __BUILTIN_IA32_PMAXUB128(BI__builtin_ia32_pmaxub128, "__builtin_ia32_pmaxub128", "V16cV16cV16c", ""),
  __BUILTIN_IA32_PMAXSW128(BI__builtin_ia32_pmaxsw128, "__builtin_ia32_pmaxsw128", "V8sV8sV8s", ""),
  __BUILTIN_IA32_PMINUB128(BI__builtin_ia32_pminub128, "__builtin_ia32_pminub128", "V16cV16cV16c", ""),
  __BUILTIN_IA32_PMINSW128(BI__builtin_ia32_pminsw128, "__builtin_ia32_pminsw128", "V8sV8sV8s", ""),
  __BUILTIN_IA32_PACKSSWB128(BI__builtin_ia32_packsswb128, "__builtin_ia32_packsswb128", "V8sV8sV8s", ""),
  __BUILTIN_IA32_PACKSSDW128(BI__builtin_ia32_packssdw128, "__builtin_ia32_packssdw128", "V4iV4iV4i", ""),
  __BUILTIN_IA32_PACKUSWB128(BI__builtin_ia32_packuswb128, "__builtin_ia32_packuswb128", "V8sV8sV8s", ""),
  __BUILTIN_IA32_PMULHUW128(BI__builtin_ia32_pmulhuw128, "__builtin_ia32_pmulhuw128", "V8sV8sV8s", ""),
  __BUILTIN_IA32_ADDSUBPS(BI__builtin_ia32_addsubps, "__builtin_ia32_addsubps", "V4fV4fV4f", ""),
  __BUILTIN_IA32_ADDSUBPD(BI__builtin_ia32_addsubpd, "__builtin_ia32_addsubpd", "V2dV2dV2d", ""),
  __BUILTIN_IA32_HADDPS(BI__builtin_ia32_haddps, "__builtin_ia32_haddps", "V4fV4fV4f", ""),
  __BUILTIN_IA32_HADDPD(BI__builtin_ia32_haddpd, "__builtin_ia32_haddpd", "V2dV2dV2d", ""),
  __BUILTIN_IA32_HSUBPS(BI__builtin_ia32_hsubps, "__builtin_ia32_hsubps", "V4fV4fV4f", ""),
  __BUILTIN_IA32_HSUBPD(BI__builtin_ia32_hsubpd, "__builtin_ia32_hsubpd", "V2dV2dV2d", ""),
  __BUILTIN_IA32_PHADDW128(BI__builtin_ia32_phaddw128, "__builtin_ia32_phaddw128", "V8sV8sV8s", ""),
  __BUILTIN_IA32_PHADDW(BI__builtin_ia32_phaddw, "__builtin_ia32_phaddw", "V4sV4sV4s", ""),
  __BUILTIN_IA32_PHADDD128(BI__builtin_ia32_phaddd128, "__builtin_ia32_phaddd128", "V4iV4iV4i", ""),
  __BUILTIN_IA32_PHADDD(BI__builtin_ia32_phaddd, "__builtin_ia32_phaddd", "V2iV2iV2i", ""),
  __BUILTIN_IA32_PHADDSW128(BI__builtin_ia32_phaddsw128, "__builtin_ia32_phaddsw128", "V8sV8sV8s", ""),
  __BUILTIN_IA32_PHADDSW(BI__builtin_ia32_phaddsw, "__builtin_ia32_phaddsw", "V4sV4sV4s", ""),
  __BUILTIN_IA32_PHSUBW128(BI__builtin_ia32_phsubw128, "__builtin_ia32_phsubw128", "V8sV8sV8s", ""),
  __BUILTIN_IA32_PHSUBW(BI__builtin_ia32_phsubw, "__builtin_ia32_phsubw", "V4sV4sV4s", ""),
  __BUILTIN_IA32_PHSUBD128(BI__builtin_ia32_phsubd128, "__builtin_ia32_phsubd128", "V4iV4iV4i", ""),
  __BUILTIN_IA32_PHSUBD(BI__builtin_ia32_phsubd, "__builtin_ia32_phsubd", "V2iV2iV2i", ""),
  __BUILTIN_IA32_PHSUBSW128(BI__builtin_ia32_phsubsw128, "__builtin_ia32_phsubsw128", "V8sV8sV8s", ""),
  __BUILTIN_IA32_PHSUBSW(BI__builtin_ia32_phsubsw, "__builtin_ia32_phsubsw", "V4sV4sV4s", ""),
  __BUILTIN_IA32_PMADDUBSW128(BI__builtin_ia32_pmaddubsw128, "__builtin_ia32_pmaddubsw128", "V16cV16cV16c", ""),
  __BUILTIN_IA32_PMADDUBSW(BI__builtin_ia32_pmaddubsw, "__builtin_ia32_pmaddubsw", "V8cV8cV8c", ""),
  __BUILTIN_IA32_PMULHRSW128(BI__builtin_ia32_pmulhrsw128, "__builtin_ia32_pmulhrsw128", "V8sV8sV8s", ""),
  __BUILTIN_IA32_PMULHRSW(BI__builtin_ia32_pmulhrsw, "__builtin_ia32_pmulhrsw", "V4sV4sV4s", ""),
  __BUILTIN_IA32_PSHUFB128(BI__builtin_ia32_pshufb128, "__builtin_ia32_pshufb128", "V16cV16cV16c", ""),
  __BUILTIN_IA32_PSHUFB(BI__builtin_ia32_pshufb, "__builtin_ia32_pshufb", "V8cV8cV8c", ""),
  __BUILTIN_IA32_PSIGNB128(BI__builtin_ia32_psignb128, "__builtin_ia32_psignb128", "V16cV16cV16c", ""),
  __BUILTIN_IA32_PSIGNB(BI__builtin_ia32_psignb, "__builtin_ia32_psignb", "V8cV8cV8c", ""),
  __BUILTIN_IA32_PSIGNW128(BI__builtin_ia32_psignw128, "__builtin_ia32_psignw128", "V8sV8sV8s", ""),
  __BUILTIN_IA32_PSIGNW(BI__builtin_ia32_psignw, "__builtin_ia32_psignw", "V4sV4sV4s", ""),
  __BUILTIN_IA32_PSIGND128(BI__builtin_ia32_psignd128, "__builtin_ia32_psignd128", "V4iV4iV4i", ""),
  __BUILTIN_IA32_PSIGND(BI__builtin_ia32_psignd, "__builtin_ia32_psignd", "V2iV2iV2i", ""),
  __BUILTIN_IA32_PABSB128(BI__builtin_ia32_pabsb128, "__builtin_ia32_pabsb128", "V16cV16c", ""),
  __BUILTIN_IA32_PABSB(BI__builtin_ia32_pabsb, "__builtin_ia32_pabsb", "V8cV8c", ""),
  __BUILTIN_IA32_PABSW128(BI__builtin_ia32_pabsw128, "__builtin_ia32_pabsw128", "V8sV8s", ""),
  __BUILTIN_IA32_PABSW(BI__builtin_ia32_pabsw, "__builtin_ia32_pabsw", "V4sV4s", ""),
  __BUILTIN_IA32_PABSD128(BI__builtin_ia32_pabsd128, "__builtin_ia32_pabsd128", "V4iV4i", ""),
  __BUILTIN_IA32_PABSD(BI__builtin_ia32_pabsd, "__builtin_ia32_pabsd", "V2iV2i", ""),
  __BUILTIN_IA32_PSLLW(BI__builtin_ia32_psllw, "__builtin_ia32_psllw", "V4sV4sV1LLi", ""),
  __BUILTIN_IA32_PSLLD(BI__builtin_ia32_pslld, "__builtin_ia32_pslld", "V2iV2iV1LLi", ""),
  __BUILTIN_IA32_PSLLQ(BI__builtin_ia32_psllq, "__builtin_ia32_psllq", "V1LLiV1LLiV1LLi", ""),
  __BUILTIN_IA32_PSRLW(BI__builtin_ia32_psrlw, "__builtin_ia32_psrlw", "V4sV4sV1LLi", ""),
  __BUILTIN_IA32_PSRLD(BI__builtin_ia32_psrld, "__builtin_ia32_psrld", "V2iV2iV1LLi", ""),
  __BUILTIN_IA32_PSRLQ(BI__builtin_ia32_psrlq, "__builtin_ia32_psrlq", "V1LLiV1LLiV1LLi", ""),
  __BUILTIN_IA32_PSRAW(BI__builtin_ia32_psraw, "__builtin_ia32_psraw", "V4sV4sV1LLi", ""),
  __BUILTIN_IA32_PSRAD(BI__builtin_ia32_psrad, "__builtin_ia32_psrad", "V2iV2iV1LLi", ""),
  __BUILTIN_IA32_PMADDWD(BI__builtin_ia32_pmaddwd, "__builtin_ia32_pmaddwd", "V2iV4sV4s", ""),
  __BUILTIN_IA32_PACKSSWB(BI__builtin_ia32_packsswb, "__builtin_ia32_packsswb", "V8cV4sV4s", ""),
  __BUILTIN_IA32_PACKSSDW(BI__builtin_ia32_packssdw, "__builtin_ia32_packssdw", "V4sV2iV2i", ""),
  __BUILTIN_IA32_PACKUSWB(BI__builtin_ia32_packuswb, "__builtin_ia32_packuswb", "V8cV4sV4s", ""),
  __BUILTIN_IA32_LDMXCSR(BI__builtin_ia32_ldmxcsr, "__builtin_ia32_ldmxcsr", "vUi", ""),
  __BUILTIN_IA32_STMXCSR(BI__builtin_ia32_stmxcsr, "__builtin_ia32_stmxcsr", "Ui", ""),
  __BUILTIN_IA32_CVTPI2PS(BI__builtin_ia32_cvtpi2ps, "__builtin_ia32_cvtpi2ps", "V4fV4fV2i", ""),
  __BUILTIN_IA32_CVTPS2PI(BI__builtin_ia32_cvtps2pi, "__builtin_ia32_cvtps2pi", "V2iV4f", ""),
  __BUILTIN_IA32_CVTSS2SI(BI__builtin_ia32_cvtss2si, "__builtin_ia32_cvtss2si", "iV4f", ""),
  __BUILTIN_IA32_CVTSS2SI64(BI__builtin_ia32_cvtss2si64, "__builtin_ia32_cvtss2si64", "LLiV4f", ""),
  __BUILTIN_IA32_CVTTPS2PI(BI__builtin_ia32_cvttps2pi, "__builtin_ia32_cvttps2pi", "V2iV4f", ""),
  __BUILTIN_IA32_MASKMOVQ(BI__builtin_ia32_maskmovq, "__builtin_ia32_maskmovq", "vV8cV8cc*", ""),
  __BUILTIN_IA32_LOADUPS(BI__builtin_ia32_loadups, "__builtin_ia32_loadups", "V4ffC*", ""),
  __BUILTIN_IA32_STOREUPS(BI__builtin_ia32_storeups, "__builtin_ia32_storeups", "vf*V4f", ""),
  __BUILTIN_IA32_STOREHPS(BI__builtin_ia32_storehps, "__builtin_ia32_storehps", "vV2i*V4f", ""),
  __BUILTIN_IA32_STORELPS(BI__builtin_ia32_storelps, "__builtin_ia32_storelps", "vV2i*V4f", ""),
  __BUILTIN_IA32_MOVMSKPS(BI__builtin_ia32_movmskps, "__builtin_ia32_movmskps", "iV4f", ""),
  __BUILTIN_IA32_PMOVMSKB(BI__builtin_ia32_pmovmskb, "__builtin_ia32_pmovmskb", "iV8c", ""),
  __BUILTIN_IA32_MOVNTPS(BI__builtin_ia32_movntps, "__builtin_ia32_movntps", "vf*V4f", ""),
  __BUILTIN_IA32_MOVNTQ(BI__builtin_ia32_movntq, "__builtin_ia32_movntq", "vV1LLi*V1LLi", ""),
  __BUILTIN_IA32_SFENCE(BI__builtin_ia32_sfence, "__builtin_ia32_sfence", "v", ""),
  __BUILTIN_IA32_PSADBW(BI__builtin_ia32_psadbw, "__builtin_ia32_psadbw", "V4sV8cV8c", ""),
  __BUILTIN_IA32_RCPPS(BI__builtin_ia32_rcpps, "__builtin_ia32_rcpps", "V4fV4f", ""),
  __BUILTIN_IA32_RCPSS(BI__builtin_ia32_rcpss, "__builtin_ia32_rcpss", "V4fV4f", ""),
  __BUILTIN_IA32_RSQRTPS(BI__builtin_ia32_rsqrtps, "__builtin_ia32_rsqrtps", "V4fV4f", ""),
  __BUILTIN_IA32_RSQRTSS(BI__builtin_ia32_rsqrtss, "__builtin_ia32_rsqrtss", "V4fV4f", ""),
  __BUILTIN_IA32_SQRTPS(BI__builtin_ia32_sqrtps, "__builtin_ia32_sqrtps", "V4fV4f", ""),
  __BUILTIN_IA32_SQRTSS(BI__builtin_ia32_sqrtss, "__builtin_ia32_sqrtss", "V4fV4f", ""),
  __BUILTIN_IA32_MASKMOVDQU(BI__builtin_ia32_maskmovdqu, "__builtin_ia32_maskmovdqu", "vV16cV16cc*", ""),
  __BUILTIN_IA32_LOADUPD(BI__builtin_ia32_loadupd, "__builtin_ia32_loadupd", "V2ddC*", ""),
  __BUILTIN_IA32_STOREUPD(BI__builtin_ia32_storeupd, "__builtin_ia32_storeupd", "vd*V2d", ""),
  __BUILTIN_IA32_MOVMSKPD(BI__builtin_ia32_movmskpd, "__builtin_ia32_movmskpd", "iV2d", ""),
  __BUILTIN_IA32_PMOVMSKB128(BI__builtin_ia32_pmovmskb128, "__builtin_ia32_pmovmskb128", "iV16c", ""),
  __BUILTIN_IA32_MOVNTI(BI__builtin_ia32_movnti, "__builtin_ia32_movnti", "vi*i", ""),
  __BUILTIN_IA32_MOVNTPD(BI__builtin_ia32_movntpd, "__builtin_ia32_movntpd", "vd*V2d", ""),
  __BUILTIN_IA32_MOVNTDQ(BI__builtin_ia32_movntdq, "__builtin_ia32_movntdq", "vV2LLi*V2LLi", ""),
  __BUILTIN_IA32_PSADBW128(BI__builtin_ia32_psadbw128, "__builtin_ia32_psadbw128", "V2LLiV16cV16c", ""),
  __BUILTIN_IA32_SQRTPD(BI__builtin_ia32_sqrtpd, "__builtin_ia32_sqrtpd", "V2dV2d", ""),
  __BUILTIN_IA32_SQRTSD(BI__builtin_ia32_sqrtsd, "__builtin_ia32_sqrtsd", "V2dV2d", ""),
  __BUILTIN_IA32_CVTDQ2PD(BI__builtin_ia32_cvtdq2pd, "__builtin_ia32_cvtdq2pd", "V2dV4i", ""),
  __BUILTIN_IA32_CVTDQ2PS(BI__builtin_ia32_cvtdq2ps, "__builtin_ia32_cvtdq2ps", "V4fV4i", ""),
  __BUILTIN_IA32_CVTPD2DQ(BI__builtin_ia32_cvtpd2dq, "__builtin_ia32_cvtpd2dq", "V2LLiV2d", ""),
  __BUILTIN_IA32_CVTPD2PI(BI__builtin_ia32_cvtpd2pi, "__builtin_ia32_cvtpd2pi", "V2iV2d", ""),
  __BUILTIN_IA32_CVTPD2PS(BI__builtin_ia32_cvtpd2ps, "__builtin_ia32_cvtpd2ps", "V4fV2d", ""),
  __BUILTIN_IA32_CVTTPD2DQ(BI__builtin_ia32_cvttpd2dq, "__builtin_ia32_cvttpd2dq", "V4iV2d", ""),
  __BUILTIN_IA32_CVTTPD2PI(BI__builtin_ia32_cvttpd2pi, "__builtin_ia32_cvttpd2pi", "V2iV2d", ""),
  __BUILTIN_IA32_CVTPI2PD(BI__builtin_ia32_cvtpi2pd, "__builtin_ia32_cvtpi2pd", "V2dV2i", ""),
  __BUILTIN_IA32_CVTSD2SI(BI__builtin_ia32_cvtsd2si, "__builtin_ia32_cvtsd2si", "iV2d", ""),
  __BUILTIN_IA32_CVTSD2SI64(BI__builtin_ia32_cvtsd2si64, "__builtin_ia32_cvtsd2si64", "LLiV2d", ""),
  __BUILTIN_IA32_CVTPS2DQ(BI__builtin_ia32_cvtps2dq, "__builtin_ia32_cvtps2dq", "V4iV4f", ""),
  __BUILTIN_IA32_CVTPS2PD(BI__builtin_ia32_cvtps2pd, "__builtin_ia32_cvtps2pd", "V2dV4f", ""),
  __BUILTIN_IA32_CVTTPS2DQ(BI__builtin_ia32_cvttps2dq, "__builtin_ia32_cvttps2dq", "V4iV4f", ""),
  __BUILTIN_IA32_CLFLUSH(BI__builtin_ia32_clflush, "__builtin_ia32_clflush", "vvC*", ""),
  __BUILTIN_IA32_LFENCE(BI__builtin_ia32_lfence, "__builtin_ia32_lfence", "v", ""),
  __BUILTIN_IA32_MFENCE(BI__builtin_ia32_mfence, "__builtin_ia32_mfence", "v", ""),
  __BUILTIN_IA32_LOADDQU(BI__builtin_ia32_loaddqu, "__builtin_ia32_loaddqu", "V16ccC*", ""),
  __BUILTIN_IA32_STOREDQU(BI__builtin_ia32_storedqu, "__builtin_ia32_storedqu", "vc*V16c", ""),
  __BUILTIN_IA32_PSLLWI(BI__builtin_ia32_psllwi, "__builtin_ia32_psllwi", "V4sV4si", ""),
  __BUILTIN_IA32_PSLLDI(BI__builtin_ia32_pslldi, "__builtin_ia32_pslldi", "V2iV2ii", ""),
  __BUILTIN_IA32_PSLLQI(BI__builtin_ia32_psllqi, "__builtin_ia32_psllqi", "V1LLiV1LLii", ""),
  __BUILTIN_IA32_PSRAWI(BI__builtin_ia32_psrawi, "__builtin_ia32_psrawi", "V4sV4si", ""),
  __BUILTIN_IA32_PSRADI(BI__builtin_ia32_psradi, "__builtin_ia32_psradi", "V2iV2ii", ""),
  __BUILTIN_IA32_PSRLWI(BI__builtin_ia32_psrlwi, "__builtin_ia32_psrlwi", "V4sV4si", ""),
  __BUILTIN_IA32_PSRLDI(BI__builtin_ia32_psrldi, "__builtin_ia32_psrldi", "V2iV2ii", ""),
  __BUILTIN_IA32_PSRLQI(BI__builtin_ia32_psrlqi, "__builtin_ia32_psrlqi", "V1LLiV1LLii", ""),
  __BUILTIN_IA32_PMULUDQ(BI__builtin_ia32_pmuludq, "__builtin_ia32_pmuludq", "V1LLiV2iV2i", ""),
  __BUILTIN_IA32_PMULUDQ128(BI__builtin_ia32_pmuludq128, "__builtin_ia32_pmuludq128", "V2LLiV4iV4i", ""),
  __BUILTIN_IA32_PSRAW128(BI__builtin_ia32_psraw128, "__builtin_ia32_psraw128", "V8sV8sV8s", ""),
  __BUILTIN_IA32_PSRAD128(BI__builtin_ia32_psrad128, "__builtin_ia32_psrad128", "V4iV4iV4i", ""),
  __BUILTIN_IA32_PSRLW128(BI__builtin_ia32_psrlw128, "__builtin_ia32_psrlw128", "V8sV8sV8s", ""),
  __BUILTIN_IA32_PSRLD128(BI__builtin_ia32_psrld128, "__builtin_ia32_psrld128", "V4iV4iV4i", ""),
  __BUILTIN_IA32_PSLLDQI128(BI__builtin_ia32_pslldqi128, "__builtin_ia32_pslldqi128", "V2LLiV2LLii", ""),
  __BUILTIN_IA32_PSRLDQI128(BI__builtin_ia32_psrldqi128, "__builtin_ia32_psrldqi128", "V2LLiV2LLii", ""),
  __BUILTIN_IA32_PSRLQ128(BI__builtin_ia32_psrlq128, "__builtin_ia32_psrlq128", "V2LLiV2LLiV2LLi", ""),
  __BUILTIN_IA32_PSLLW128(BI__builtin_ia32_psllw128, "__builtin_ia32_psllw128", "V8sV8sV8s", ""),
  __BUILTIN_IA32_PSLLD128(BI__builtin_ia32_pslld128, "__builtin_ia32_pslld128", "V4iV4iV4i", ""),
  __BUILTIN_IA32_PSLLQ128(BI__builtin_ia32_psllq128, "__builtin_ia32_psllq128", "V2LLiV2LLiV2LLi", ""),
  __BUILTIN_IA32_PSLLWI128(BI__builtin_ia32_psllwi128, "__builtin_ia32_psllwi128", "V8sV8si", ""),
  __BUILTIN_IA32_PSLLDI128(BI__builtin_ia32_pslldi128, "__builtin_ia32_pslldi128", "V4iV4ii", ""),
  __BUILTIN_IA32_PSLLQI128(BI__builtin_ia32_psllqi128, "__builtin_ia32_psllqi128", "V2LLiV2LLii", ""),
  __BUILTIN_IA32_PSRLWI128(BI__builtin_ia32_psrlwi128, "__builtin_ia32_psrlwi128", "V8sV8si", ""),
  __BUILTIN_IA32_PSRLDI128(BI__builtin_ia32_psrldi128, "__builtin_ia32_psrldi128", "V4iV4ii", ""),
  __BUILTIN_IA32_PSRLQI128(BI__builtin_ia32_psrlqi128, "__builtin_ia32_psrlqi128", "V2LLiV2LLii", ""),
  __BUILTIN_IA32_PSRAWI128(BI__builtin_ia32_psrawi128, "__builtin_ia32_psrawi128", "V8sV8si", ""),
  __BUILTIN_IA32_PSRADI128(BI__builtin_ia32_psradi128, "__builtin_ia32_psradi128", "V4iV4ii", ""),
  __BUILTIN_IA32_PMADDWD128(BI__builtin_ia32_pmaddwd128, "__builtin_ia32_pmaddwd128", "V8sV8sV8s", ""),
  __BUILTIN_IA32_MONITOR(BI__builtin_ia32_monitor, "__builtin_ia32_monitor", "vv*UiUi", ""),
  __BUILTIN_IA32_MWAIT(BI__builtin_ia32_mwait, "__builtin_ia32_mwait", "vUiUi", ""),
  __BUILTIN_IA32_LDDQU(BI__builtin_ia32_lddqu, "__builtin_ia32_lddqu", "V16ccC*", ""),
  __BUILTIN_IA32_PALIGNR128(BI__builtin_ia32_palignr128, "__builtin_ia32_palignr128", "V2LLiV2LLiV2LLii", ""),
  __BUILTIN_IA32_PALIGNR(BI__builtin_ia32_palignr, "__builtin_ia32_palignr", "V1LLiV1LLiV1LLis", ""),
  __BUILTIN_IA32_INSERTPS128(BI__builtin_ia32_insertps128, "__builtin_ia32_insertps128", "V4fV4fV4fi", ""),
  __BUILTIN_IA32_STORELV4SI(BI__builtin_ia32_storelv4si, "__builtin_ia32_storelv4si", "vV2i*V2LLi", ""),
  __BUILTIN_IA32_PBLENDVB128(BI__builtin_ia32_pblendvb128, "__builtin_ia32_pblendvb128", "V16cV16cV16cV16c", ""),
  __BUILTIN_IA32_PBLENDW128(BI__builtin_ia32_pblendw128, "__builtin_ia32_pblendw128", "V8sV8sV8si", ""),
  __BUILTIN_IA32_BLENDPD(BI__builtin_ia32_blendpd, "__builtin_ia32_blendpd", "V2dV2dV2di", ""),
  __BUILTIN_IA32_BLENDPS(BI__builtin_ia32_blendps, "__builtin_ia32_blendps", "V4fV4fV4fi", ""),
  __BUILTIN_IA32_BLENDVPD(BI__builtin_ia32_blendvpd, "__builtin_ia32_blendvpd", "V2dV2dV2dV2d", ""),
  __BUILTIN_IA32_BLENDVPS(BI__builtin_ia32_blendvps, "__builtin_ia32_blendvps", "V4fV4fV4fV4f", ""),
  __BUILTIN_IA32_PACKUSDW128(BI__builtin_ia32_packusdw128, "__builtin_ia32_packusdw128", "V8sV4iV4i", ""),
  __BUILTIN_IA32_PMAXSB128(BI__builtin_ia32_pmaxsb128, "__builtin_ia32_pmaxsb128", "V16cV16cV16c", ""),
  __BUILTIN_IA32_PMAXSD128(BI__builtin_ia32_pmaxsd128, "__builtin_ia32_pmaxsd128", "V4iV4iV4i", ""),
  __BUILTIN_IA32_PMAXUD128(BI__builtin_ia32_pmaxud128, "__builtin_ia32_pmaxud128", "V4iV4iV4i", ""),
  __BUILTIN_IA32_PMAXUW128(BI__builtin_ia32_pmaxuw128, "__builtin_ia32_pmaxuw128", "V8sV8sV8s", ""),
  __BUILTIN_IA32_PMINSB128(BI__builtin_ia32_pminsb128, "__builtin_ia32_pminsb128", "V16cV16cV16c", ""),
  __BUILTIN_IA32_PMINSD128(BI__builtin_ia32_pminsd128, "__builtin_ia32_pminsd128", "V4iV4iV4i", ""),
  __BUILTIN_IA32_PMINUD128(BI__builtin_ia32_pminud128, "__builtin_ia32_pminud128", "V4iV4iV4i", ""),
  __BUILTIN_IA32_PMINUW128(BI__builtin_ia32_pminuw128, "__builtin_ia32_pminuw128", "V8sV8sV8s", ""),
  __BUILTIN_IA32_PMOVSXBD128(BI__builtin_ia32_pmovsxbd128, "__builtin_ia32_pmovsxbd128", "V4iV16c", ""),
  __BUILTIN_IA32_PMOVSXBQ128(BI__builtin_ia32_pmovsxbq128, "__builtin_ia32_pmovsxbq128", "V2LLiV16c", ""),
  __BUILTIN_IA32_PMOVSXBW128(BI__builtin_ia32_pmovsxbw128, "__builtin_ia32_pmovsxbw128", "V8sV16c", ""),
  __BUILTIN_IA32_PMOVSXDQ128(BI__builtin_ia32_pmovsxdq128, "__builtin_ia32_pmovsxdq128", "V2LLiV4i", ""),
  __BUILTIN_IA32_PMOVSXWD128(BI__builtin_ia32_pmovsxwd128, "__builtin_ia32_pmovsxwd128", "V4iV8s", ""),
  __BUILTIN_IA32_PMOVSXWQ128(BI__builtin_ia32_pmovsxwq128, "__builtin_ia32_pmovsxwq128", "V2LLiV8s", ""),
  __BUILTIN_IA32_PMOVZXBD128(BI__builtin_ia32_pmovzxbd128, "__builtin_ia32_pmovzxbd128", "V4iV16c", ""),
  __BUILTIN_IA32_PMOVZXBQ128(BI__builtin_ia32_pmovzxbq128, "__builtin_ia32_pmovzxbq128", "V2LLiV16c", ""),
  __BUILTIN_IA32_PMOVZXBW128(BI__builtin_ia32_pmovzxbw128, "__builtin_ia32_pmovzxbw128", "V8sV16c", ""),
  __BUILTIN_IA32_PMOVZXDQ128(BI__builtin_ia32_pmovzxdq128, "__builtin_ia32_pmovzxdq128", "V2LLiV4i", ""),
  __BUILTIN_IA32_PMOVZXWD128(BI__builtin_ia32_pmovzxwd128, "__builtin_ia32_pmovzxwd128", "V4iV8s", ""),
  __BUILTIN_IA32_PMOVZXWQ128(BI__builtin_ia32_pmovzxwq128, "__builtin_ia32_pmovzxwq128", "V2LLiV8s", ""),
  __BUILTIN_IA32_PMULDQ128(BI__builtin_ia32_pmuldq128, "__builtin_ia32_pmuldq128", "V2LLiV4iV4i", ""),
  __BUILTIN_IA32_PMULLD128(BI__builtin_ia32_pmulld128, "__builtin_ia32_pmulld128", "V4iV4iV4i", ""),
  __BUILTIN_IA32_ROUNDPS(BI__builtin_ia32_roundps, "__builtin_ia32_roundps", "V4fV4fi", ""),
  __BUILTIN_IA32_ROUNDSS(BI__builtin_ia32_roundss, "__builtin_ia32_roundss", "V4fV4fi", ""),
  __BUILTIN_IA32_ROUNDSD(BI__builtin_ia32_roundsd, "__builtin_ia32_roundsd", "V2dV2di", ""),
  __BUILTIN_IA32_ROUNDPD(BI__builtin_ia32_roundpd, "__builtin_ia32_roundpd", "V2dV2di", "");


  int id;
  String name;
  String type;
  String attr;

  BUILTX86(int id, String name, String type, String attr) {
    this.id = id;
    this.name = name;
    this.type = type;
    this.attr = attr;
  }
}
