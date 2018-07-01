package jlang.driver;
/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2018, Xlous Zeng.
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

import tools.Util;
import jlang.basic.HeaderSearch;
import jlang.basic.SourceManager;
import jlang.basic.TargetInfo;
import jlang.clex.Preprocessor;
import jlang.diag.Diagnostic;
import jlang.support.LangOptions;
import jlang.support.PreprocessorInitOptions;
import tools.APFloat;
import tools.FltSemantics;

import java.math.BigInteger;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
class PreprocessorFactory
{
    private Diagnostic diag;
    private LangOptions opts;
    private TargetInfo target;
    private HeaderSearch headerInfo;
    private SourceManager sourceMgr;

    PreprocessorFactory(Diagnostic diag, LangOptions opts,
            TargetInfo target,
            SourceManager sourceMgr,
            HeaderSearch headerInfo)
    {
        this.diag = diag;
        this.opts = opts;
        this.target = target;
        this.headerInfo = headerInfo;
        this.sourceMgr = sourceMgr;
    }

    /**
     * A factory method to create and initialize an object of {@linkplain Preprocessor}.
     * from several arguments.
     *
     * @return
     */
    public Preprocessor createAndInitPreprocessor()
    {
        Preprocessor pp = new Preprocessor(diag, opts, target, sourceMgr, headerInfo);
        PreprocessorInitOptions initOptions = new PreprocessorInitOptions();
        initializePreprocessorInitOptions(initOptions);

        return initializePreprocessor(pp, initOptions) ? null : pp;
    }

    private void initializePreprocessorInitOptions(
            PreprocessorInitOptions initOpts)
    {
        // Add macro from command line.
        if (!JlangCC.D_Macros.isEmpty())
        {
            for (String m : JlangCC.D_Macros)
                initOpts.addMacroDef(m);
        }

        if (!JlangCC.U_macros.isEmpty())
        {
            for (String u : JlangCC.U_macros)
                initOpts.addMacroUndef(u);
        }
    }

    private boolean initializePreprocessor(Preprocessor pp,
            PreprocessorInitOptions initOpts)
    {
        StringBuilder predefinedBuffer = new StringBuilder();
        String lineDirective = "# 1 \"<built-in>\" 3\n";
        predefinedBuffer.append(lineDirective);

        // Install things like __GNUC__, etc.
        initializePredefinedMacros(predefinedBuffer);

        // Add on the predefines from the driver.  Wrap in a #line directive to report
        // that they come from the command line.
        lineDirective = "# 1 \"<command line>\" 1\n";
        predefinedBuffer.append(lineDirective);

        // Process #define's and #undef's in the order they are given.
        initOpts.getMacros().forEach(pair ->
        {
            if (pair.second)
                undefineBuiltinMacro(predefinedBuffer, pair.first);
            else
                defineBuiltinMacro(predefinedBuffer, pair.first);
        });

        // Append a line terminator into predefinedBuffer.
        predefinedBuffer.append("\n");

        pp.setPredefines(predefinedBuffer.toString());
        // Once we are reaching this, done!
        return false;
    }

    private void initializePredefinedMacros(StringBuilder buf)
    {
        defineBuiltinMacro(buf, "__xcc__=1"); // XCC version.
        defineBuiltinMacro(buf, "__llvm__=1"); // LLVM backend.
        defineBuiltinMacro(buf, "NULL=(void*)0");

        // Currently isClaim to compatible with GCC 4.2.1-5621
        defineBuiltinMacro(buf, "__GNUC_MINOR__=2");
        defineBuiltinMacro(buf, "__GNUC_PATCHLEVEL__=1");
        defineBuiltinMacro(buf, "__GNUC__=4");
        defineBuiltinMacro(buf, "__VERSION__=\"4.2.1 Compatible XCC Compiler\"");
        if (opts.asmPreprocessor)
            defineBuiltinMacro(buf, "__ASSEMBLER__=1");

        defineBuiltinMacro(buf, "__STDC__=1");
        if (opts.c99)
            defineBuiltinMacro(buf, "__STDC_VERSION__=199901L");
        else if (!opts.gnuMode && opts.trigraph)
            defineBuiltinMacro(buf, "__STDC_VERSION__=199409L");

        if (!opts.gnuMode)
            defineBuiltinMacro(buf, "__STRICT_ANSI=1");

        if (opts.optimize)
            defineBuiltinMacro(buf, "__OPTIMIZE__=1");
        if (opts.optimizeSize)
            defineBuiltinMacro(buf, "__OPTIMIZE_SIZE__=1");

        // Initialize target-specific preprocessor defines.
        Util.assertion(target.getCharWidth() == 8, "Only support 8 bit char so far");
        defineBuiltinMacro(buf, "__CHAR_BIT__=8");

        int intMaxWidth;
        String intMaxSuffix;
        if (target.getIntMaxType() == TargetInfo.IntType.SignedLongLong)
        {
            intMaxWidth = target.getLongLongWidth();
            intMaxSuffix = "LL";
        }
        else if (target.getIntMaxType() == TargetInfo.IntType.SignedLong)
        {
            intMaxWidth = target.getLongWidth();
            intMaxSuffix = "L";
        }
        else
        {
            Util.assertion( target.getIntMaxType() == TargetInfo.IntType.SignedInt);
            intMaxWidth = target.getIntWidth();
            intMaxSuffix = "";
        }

        defineTypeSize("__SCHAR_MAX__", target.getCharWidth(), "", true, buf);
        defineTypeSize("__SHRT_MAX__", target.getShortWidth(), "", true, buf);
        defineTypeSize("__INT_MAX__", target.getIntWidth(), "", true, buf);
        defineTypeSize("__LONG_MAX__", target.getLongWidth(), "L", true, buf);
        defineTypeSize("__LONG_LONG_MAX__", target.getLongLongWidth(), "LL", true, buf);
        defineTypeSize("__INTMAX_MAX__", intMaxWidth, intMaxSuffix, true, buf);

        defineType("__INTMAX_TYPE__", target.getIntMaxType(), buf);
        defineType("__UINTMAX_TYPE__", target.getUIntMaxType(), buf);
        defineType("__PTRDIFF_TYPE__", target.getPtrDiffType(0), buf);
        defineType("__INTPTR_TYPE__", target.getIntPtrType(), buf);
        defineType("__SIZE_TYPE__", target.getSizeType(), buf);

        defineFloatMacros(buf, "FLT", target.getFloatFormat());
        defineFloatMacros(buf, "DBL", target.getDoubleFormat());
        defineFloatMacros(buf, "LDBL", target.getLongDoubleFormat());

        defineBuiltinMacro(buf, "__POINTER_WIDTH__=" + target.getPointerWidth(0));

        // Define fixed-sized integer types for stdint.h
        Util.assertion(target.getCharWidth() == 8,  "unsupported target types");
        Util.assertion(target.getShortWidth() == 16,  "unsupported target types");
        defineBuiltinMacro(buf, "__INT8_TYPE__=char");
        defineBuiltinMacro(buf, "__INT16_TYPE__=short");

        if (target.getIntWidth() == 32)
            defineBuiltinMacro(buf, "__INT32_TYPE__=int");
        else
            {
            Util.assertion(target.getLongLongWidth() == 32,  "unsupported target types");
            defineBuiltinMacro(buf, "__INT32_TYPE__=long long");
        }

        // 16-bit targets doesn't necessarily have a 64-bit type.
        if (target.getLongLongWidth() == 64)
            defineType("__INT64_TYPE__", target.getInt64Type(), buf);

        // Add __builtin_va_list typedef.
        {
            String laList = target.getVAListDeclaration();
            buf.append(laList);
            buf.append('\n');
        }
        String  Prefix = target.getUserLabelPrefix();
        if (Prefix != null)
        {
            defineBuiltinMacro(buf, "__USER_LABEL_PREFIX__=" + Prefix);
        }
        
        // Build configuration options.  
        // FIXME: these should be controlled by command line options or something.
        defineBuiltinMacro(buf, "__FINITE_MATH_ONLY__=0");

        // Macros to control C99 numerics and <float.h>
        defineBuiltinMacro(buf, "__FLT_EVAL_METHOD__=0");
        defineBuiltinMacro(buf, "__FLT_RADIX__=2");

        int x = (Integer) pickFP(target.getLongDoubleFormat(), -1/*FIXME*/, 17, 21, 36);
        defineBuiltinMacro(buf, "__DECIMAL_DIG__=" + x);

        // Get other target #defines.
        target.getTargetDefines(opts, buf);

    }

    private void defineTypeSize(String macroName, int typeWidth, String valSuffix,
            boolean isSigned, StringBuilder buf)
    {
        BigInteger maxVal;
        if (isSigned)
        {
            BigInteger one = BigInteger.ONE;
            maxVal = one.shiftLeft(typeWidth - 1).subtract(one);
        }
        else
        {
            BigInteger zero = BigInteger.ZERO;
            maxVal = zero.not().shiftRight(64 - typeWidth);
        }

        defineBuiltinMacro(buf, macroName + "=" + maxVal.toString(10) + valSuffix);
    }

    private void defineType(String macroName, TargetInfo.IntType ty, StringBuilder buf)
    {
        defineBuiltinMacro(buf, macroName + "=" + TargetInfo.getTypeName(ty));
    }

    private void defineFloatMacros(StringBuilder buf, String prefix, FltSemantics fltSem)
    {
        String denormMin, epsilon, max, min;
        denormMin = (String) pickFP(fltSem, "1.40129846e-45F",
                "4.9406564584124654e-324",
                "3.64519953188247460253e-4951L",
                "6.47517511943802511092443895822764655e-4966L");

        int digits = (Integer) pickFP(fltSem, 6, 15, 18, 33);

        epsilon = (String)pickFP(fltSem, "1.19209290e-7F", "2.2204460492503131e-16",
                "1.08420217248550443401e-19L",
                "1.92592994438723585305597794258492732e-34L");

        int hasInifinity = 1, hasQuietNaN = 1;
        int mantissaDigits = (Integer) pickFP(fltSem, 24, 53, 64, 113);
        int min10Exp = (Integer) pickFP(fltSem, -37, -307, -4931, -4931);
        int max10Exp = (Integer) pickFP(fltSem, 38, 308, 4932, 4932);
        int minExp = (Integer) pickFP(fltSem, -125, -1021, -16381, -16381);
        int maxExp = (Integer) pickFP(fltSem, 128, 1024, 16384, 16384);
        min = (String)pickFP(fltSem, "1.17549435e-38F", "2.2250738585072014e-308",
                "3.36210314311209350626e-4932L",
                "3.36210314311209350626267781732175260e-4932L");
        max = (String) pickFP(fltSem, "3.40282347e+38F", "1.7976931348623157e+308",
                "1.18973149535723176502e+4932L",
                "1.18973149535723176508575932662800702e+4932L");

        defineBuiltinMacro(buf, String.format("__%s_DENORM_MIN__=%s", prefix, denormMin));
        defineBuiltinMacro(buf, String.format("__%s_DIG__=%d", prefix, digits));
        defineBuiltinMacro(buf, String.format("__%s_EPSILON__=%s", prefix, epsilon));
        defineBuiltinMacro(buf, String.format("__%s_HAS_INFINITY__=%d", prefix, hasInifinity));
        defineBuiltinMacro(buf, String.format("__%s_HAS_QUIET_NAN__=%d", prefix, hasQuietNaN));
        defineBuiltinMacro(buf, String.format("__%s_MANT_DIG__=%d", prefix, mantissaDigits));
        defineBuiltinMacro(buf, String.format("__%s_MAX_10_EXP__=%d", prefix, max10Exp));
        defineBuiltinMacro(buf, String.format("__%s_MAX_EXP__=%d", prefix, maxExp));
        defineBuiltinMacro(buf, String.format("__%s_MAX__=%s", prefix, max));
        defineBuiltinMacro(buf, String.format("__%s_MIN_10_EXP__=(%d)", prefix, min10Exp));
        defineBuiltinMacro(buf, String.format("__%s_MIN_EXP__=(%d)", prefix, minExp));
        defineBuiltinMacro(buf, String.format("__%s_MIN__=%s", prefix, min));
        defineBuiltinMacro(buf, String.format("__%s_HAS_DENORM__=1", prefix));
    }

    private Object pickFP(FltSemantics sem, Object ieeeSingleVal,
            Object ieeeDoubleVal, Object x87DoubleExtendedVal,
            Object ieeeQuadVal)
    {
        if (sem == APFloat.IEEEsingle)
            return ieeeSingleVal;
        if (sem == APFloat.IEEEdouble)
            return ieeeDoubleVal;
        if (sem == APFloat.x87DoubleExtended)
            return x87DoubleExtendedVal;
        Util.assertion( sem == APFloat.IEEEquad);
        return ieeeQuadVal;
    }

    /**
     * Append a #define line to buf for macro {@code m}.
     *
     * @param buf
     * @param m
     */
    private void defineBuiltinMacro(StringBuilder buf, String m)
    {
        String cmd = "#define ";
        buf.append(cmd);
        int eqPos = m.indexOf("=");
        if (eqPos >= 0)
        {
            // Turn 'X=Y' -> 'X Y'
            buf.append(m.substring(0, eqPos)).append(" ");
            int end = m.indexOf("\n\r", eqPos + 1);
            if (end >= 0)
            {
                System.err.printf("warning: macro '%s' contains"
                                + " embedded newline, text after the newline is ignored",
                        m.substring(eqPos + 1));
            }
            else
            {
                end = m.length();
            }
            buf.append(m.substring(eqPos + 1, end));
        }
        else
        {
            // Push "macroname 1".
            buf.append(m).append(" ").append("1");
        }
        buf.append('\n');
    }

    /**
     * Append a #undef line to buf for Macro.
     * Macro should be of the form XXX and we emit "#undef XXX".
     *
     * @param buf
     * @param name
     */
    private void undefineBuiltinMacro(StringBuilder buf, String name)
    {
        buf.append("#undef ").append(name).append('\n');
    }
}
