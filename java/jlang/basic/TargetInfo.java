package jlang.basic;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous
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
import tools.FltSemantics;
import jlang.support.LangOptions;
import backend.support.Triple;

import java.util.HashMap;

import static tools.APFloat.*;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public abstract class TargetInfo
{
    /**
     * Return a target info object for the specified target triple string.
     * Currently only support Linux on x86_32 and x86_64 target.
     * @param triStr
     * @return
     */
    public static TargetInfo createTargetInfo(String triStr)
    {
        Triple triple = new Triple(triStr);
        Triple.OSType os = triple.getOS();

        switch (triple.getArch())
        {
            default:
                return null;
            case x86:
                switch (os)
                {
                    case Darwin:
                        return new DarwinI386TargetInfo(triStr);
                    case Linux:
                        return new LinuxX86_32TargetInfo(triStr);
                    default:
                        return new X86_32TargetInfo(triStr);
                }
            case x86_64:
            {
                switch (os)
                {
                    case Darwin:
                        return new DarwinX86_64TargetInfo(triStr);
                    case Linux:
                        return new LinuxX86_64TargetInfo(triStr);
                    default:
                        return new X86_64TargetInfo(triStr);
                }
            }
        }
    }

    public FltSemantics getFloatFormat()
    {
        return floatFormat;
    }

    public FltSemantics getDoubleFormat()
    {
        return doubleFormat;
    }

    public FltSemantics getLongDoubleFormat()
    {
        return longDoubleFormat;
    }

    public static final class GCCRegAlias
    {
        String[] Aliases;
        String Register;

        GCCRegAlias(String[] aliases, String reg)
        {
            Aliases = aliases;
            Register = reg;
        }
    }

    public static final String[] GCCRegNames = { "ax", "dx", "cx", "bx", "si",
            "di", "bp", "sp", "st", "st(1)", "st(2)", "st(3)", "st(4)", "st(5)",
            "st(6)", "st(7)", "argp", "flags", "fspr", "dirflag", "frame",
            "xmm0", "xmm1", "xmm2", "xmm3", "xmm4", "xmm5", "xmm6", "xmm7",
            "mm0", "mm1", "mm2", "mm3", "mm4", "mm5", "mm6", "mm7", "r8", "r9",
            "r10", "r11", "r12", "r13", "r14", "r15", "xmm8", "xmm9", "xmm10",
            "xmm11", "xmm12", "xmm13", "xmm14", "xmm15" };

    public static final GCCRegAlias[] GCCRegAliases = {
            new GCCRegAlias(new String[] { "al", "ah", "eax", "rax" }, "ax"),
            new GCCRegAlias(new String[] { "bl", "bh", "ebx", "rbx" }, "bx"),
            new GCCRegAlias(new String[] { "cl", "ch", "ecx", "rcx" }, "cx"),
            new GCCRegAlias(new String[] { "dl", "dh", "edx", "rdx" }, "dx"),
            new GCCRegAlias(new String[] { "esi", "rsi" }, "si"),
            new GCCRegAlias(new String[] { "edi", "rdi" }, "di"),
            new GCCRegAlias(new String[] { "esp", "rsp" }, "sp"),
            new GCCRegAlias(new String[] { "ebp", "rbp" }, "bp"), };

    protected static void define(StringBuilder buf, String macro)
    {
        define(buf, macro, "1");
    }

    protected static void define(StringBuilder buf, String macro, String val)
    {
        String def = "#define ";
        buf.append(def);
        buf.append(macro);
        buf.append(' ');
        buf.append(val);
        buf.append('\n');
    }

    /**
     * Define a macro asmName and standard variants.  For example if
     * MacroName is "unix", then this will define "__unix", "__unix__", and "unix"
     * when in GNU mode.
     *
     * @param buf
     * @param macroName
     * @param langOpts
     */
    private static void defineStd(StringBuilder buf, String macroName,
            LangOptions langOpts)
    {
        Util.assertion(macroName.charAt(0) != '_',  "identifier should be in user space");
        // If we are in gnu mode (e.g. -std=gnu99 but not -std=c99)
        // define the raw identifier in the user's namespace.
        if (langOpts.gnuMode)
            define(buf, macroName, "1");

        // Define __unix.
        StringBuilder tempsStr = new StringBuilder();
        tempsStr.append("__");
        tempsStr.append(macroName);
        define(buf, tempsStr.toString(), "1");

        // Define __unix__
        tempsStr.append("__");
        define(buf, tempsStr.toString(), "1");
    }

    public enum IntType
    {
        NoInt, SignedShort, UnsignedShort, SignedInt, UnsignedInt, SignedLong, UnsignedLong, SignedLongLong, UnsignedLongLong
    }

    private String triple;

    protected boolean TLSSupported;
    protected int pointerWidth, pointerAlign;
    protected int charWidth, charAlign;
    protected int shortWidth, shortAlign;
    protected int intWidth, intAlign;
    protected int floatWidth, floatAlign;
    protected int doubleWidth, doubleAlign;
    protected int longDoubleWidth, longDoubleAlign;
    protected int longWidth, longAlign;
    protected int longlongWidth, longlongAlign;
    protected int intMaxTWidth;
    protected String descriptionString;

    protected IntType sizeType, IntMaxType, UIntMaxType, PtrDiffType, intPtrType, WCharType, Char16Type, Char32Type, int64Type;

    protected String userLabelPrefix;
    protected FltSemantics floatFormat, doubleFormat, longDoubleFormat;
    protected char regParmMax, sseRegParmMax;

    public TargetInfo(String triple)
    {
        this.triple = triple;
        TLSSupported = true;
        pointerWidth = pointerAlign = 32;
        charWidth = charAlign = 8;
        shortWidth = shortAlign = 16;
        intWidth = intAlign = 32;
        longWidth = longAlign = 32;
        longlongWidth = longlongAlign = 64;
        floatWidth = floatAlign = 32;
        doubleWidth = doubleAlign = 64;
        longDoubleWidth = longDoubleAlign = 64;
        intMaxTWidth = 64;
        sizeType = IntType.UnsignedLong;
        PtrDiffType = IntType.SignedLong;
        IntMaxType = IntType.SignedLongLong;
        UIntMaxType = IntType.UnsignedLongLong;
        intPtrType = IntType.SignedLong;
        int64Type = IntType.SignedLongLong;
        floatFormat = IEEEsingle;
        doubleFormat = IEEEdouble;
        longDoubleFormat = IEEEdouble;
        descriptionString = "E-p:32:32:32-i1:8:8-i8:8:8-i16:16:16-i32:32:32-"
                + "i64:64:64-f32:32:32-f64:64:64";
        userLabelPrefix = "_";
    }

    public Triple getTriple()
    {
        return new Triple(triple);
    }

    public void setTriple(String triple)
    {
        this.triple = triple;
    }

    public boolean isTLSSupported()
    {
        return TLSSupported;
    }

    public IntType getSizeType()
    {
        return sizeType;
    }

    public IntType getIntMaxType()
    {
        return IntMaxType;
    }

    public IntType getUIntMaxType()
    {
        return UIntMaxType;
    }

    public IntType getIntPtrType()
    {
        return intPtrType;
    }

    public IntType getWCharType()
    {
        return WCharType;
    }

    public IntType getChar16Type()
    {
        return Char16Type;
    }

    public IntType getChar32Type()
    {
        return Char32Type;
    }

    public IntType getInt64Type()
    {
        return int64Type;
    }

    public int getBoolWidth()
    {
        return 8;
    }

    public int getBoolAlign()
    {
        return 8;
    }

    public int getCharWidth()
    {
        return charWidth;
    }

    public int getCharAlign()
    {
        return charAlign;
    }

    public int getShortWidth()
    {
        return shortWidth;
    }

    public int getShortAlign()
    {
        return shortAlign;
    }

    public int getIntAlign()
    {
        return intAlign;
    }

    public int getIntWidth()
    {
        return intWidth;
    }

    public int getFloatAlign()
    {
        return floatAlign;
    }

    public int getFloatWidth()
    {
        return floatWidth;
    }

    public int getDoubleAlign()
    {
        return doubleAlign;
    }

    public int getDoubleWidth()
    {
        return doubleWidth;
    }

    public int getLongWidth()
    {
        return longWidth;
    }

    public int getLongAlign()
    {
        return longAlign;
    }

    public int getLongDoubleAlign()
    {
        return longDoubleAlign;
    }

    public int getLongDoubleWidth()
    {
        return longDoubleWidth;
    }

    public int getLonglongAlign()
    {
        return longlongAlign;
    }

    public int getLongLongWidth()
    {
        return longlongWidth;
    }

    public String getDescriptionString()
    {
        return descriptionString;
    }

    public int getIntMaxWidth()
    {
        return intMaxTWidth;
    }

    /**
     * This returns the default value of the __USER_LABEL_PREFIX__ macro, which is the prefix given to user symbols by
     * default.  On most platforms this is "_", but it is "" on some, and "." on
     * others.
     *
     * @return
     */
    public String getUserLabelPrefix()
    {
        return userLabelPrefix;
    }

    /**
     * Return the user string for the specified integer type enum.
     * For example, SignedShort -&gt; "short".
     *
     * @param t
     * @return
     */
    public static String getTypeName(IntType t)
    {
        switch (t)
        {
            case SignedShort:
                return "short";
            case UnsignedShort:
                return "unsigned short";
            case SignedInt:
                return "int";
            case UnsignedInt:
                return "unsigned int";
            case SignedLong:
                return "long int";
            case UnsignedLong:
                return "unsigned long int";
            case SignedLongLong:
                return "long long int";
            case UnsignedLongLong:
                return "unsigned long long int";
            default:
                Util.assertion(false,  "not an integer type!");
        }
        return null;
    }

    public abstract String getVAListDeclaration();

    /**
     * Returns whether the passed in string
     * is a valid register asmName according to GCC. This is used by Sema for
     * inline asm statements.
     *
     * @param name
     * @return
     */
    public boolean isValidGCCRegisterName(String name)
    {
        return true;
    }

    /**
     * Returns the "normalized" GCC register asmName.
     * For example, on x86 it will return "ax" when "eax" is passed in.
     *
     * @param name
     * @return
     */
    public String getNormalizedGCCRegisterName(String name)
    {
        // TODO: 17-4-14
        return null;
    }

    public String convertConstraint(char constraint)
    {
        return String.valueOf(constraint);
    }

    public abstract String getClobbers();

    public abstract String getTargetPrefix();

    public String getTargetTriple()
    {
        return triple;
    }

    public String getTargetDescription()
    {
        return descriptionString;
    }

    /**
     * Allow the target to specify default settings for
     * various language options.  These may be overridden by command line
     * options.
     *
     * @param langOpts
     */
    public void getDefaultLangOptions(LangOptions langOpts)
    {

    }

    /**
     * Return a set of the X86-specific #defines that are not tied to a specific subtarget.
     *
     * @param opts
     * @param defs
     */
    public abstract void getTargetDefines(LangOptions opts, StringBuilder defs);

    public boolean setFeatureEnabled(HashMap<String, Boolean> features,
            String name, boolean enabled)
    {
        return false;
    }

    public void getDefaultFeatures(String cpu, HashMap<String, Boolean> features)
    {
    }

    /**
     * Performs the initialization of SSELevel according to the user configuration
     * set of features.
     *
     * @param features
     */
    public void handleTargetFeatures(HashMap<String, Boolean> features)
    {

    }

    public char getRegParmMax()
    {
        return regParmMax;
    }

    public int getPointerWidth(int addrSpace)
    {
        return pointerWidth;
    }

    public int getPointerAlign(int addrSpace)
    {
        return pointerAlign;
    }

    public IntType getPtrDiffType(int addrSpace)
    {
        return PtrDiffType;
    }

    protected String[] getGCCRegNames()
    {
        return GCCRegNames;
    }

    protected GCCRegAlias[] getRCCAlias()
    {
        return GCCRegAliases;
    }

    public Info[] getTargetBuiltins()
    {
        return null;
    }

    // X86 target abstract base class; x86-32 and x86-64 are very close, so
    // most of the implementation can be shared.
    public static abstract class X86TargetInfo extends TargetInfo
    {
        private static final Info[] BuiltinInfoX86 = new Info[BUILTX86.values().length];

        static
        {
            int i = 0;
            for (BUILTX86 item : BUILTX86.values())
                BuiltinInfoX86[i++] = new Info(item.name, item.type,
                        item.attr, null, false);
        }
        enum X86SSEEnum
        {
            NoMMXSSE, MMX, SSE1, SSE2, SSE3, SSSE3, SSE41, SSE42
        }

        X86SSEEnum sseLevel;

        public X86TargetInfo(String triple)
        {
            super(triple);
            sseLevel = X86SSEEnum.NoMMXSSE;
            longDoubleFormat = x87DoubleExtended;
        }

        @Override
        public Info[] getTargetBuiltins()
        {
            return BuiltinInfoX86;
        }

        @Override
        public String getTargetPrefix()
        {
            return "x86";
        }

        @Override
        public String[] getGCCRegNames()
        {
            return GCCRegNames;
        }

        @Override
        public GCCRegAlias[] getRCCAlias()
        {
            return GCCRegAliases;
        }

        @Override
        public String convertConstraint(char constraint)
        {
            switch (constraint)
            {
                case 'a':
                    return "{ax}";
                case 'b':
                    return "{bx}";
                case 'c':
                    return "{cx}";
                case 'd':
                    return "{dx}";
                case 'S':
                    return "{si}";
                case 'D':
                    return "{di}";
                case 't': // the top of floating point stack.
                    return "{st}";
                case 'u':
                    // second from top of floating point stack.
                    return "{st(1)}";
                default:
                    return String.valueOf(constraint);
            }
        }

        @Override
        public String getClobbers()
        {
            return "~{dirflag},~{fpsr},~{flags}";
        }

        /**
         * Return a set of the X86-specific #defines that are not tied to a specific subtarget.
         *
         * @param opts
         * @param defs
         */
        @Override
        public void getTargetDefines(LangOptions opts, StringBuilder defs)
        {
            if (pointerWidth == 64)
            {
                define(defs, "__amd64__", "1");
                define(defs, "__amd64", "1");
                define(defs, "x86_64__", "1");
                define(defs, "x86_64", "1");
            }
            else
            {
                TargetInfo.defineStd(defs, "i386", opts);
            }

            // Target properties.
            TargetInfo.define(defs, "__LITTLE_ENDIAN", "1");

            // SubTarget options.
            TargetInfo.define(defs, "__nocona", "1");
            TargetInfo.define(defs, "__nocona__", "1");
            TargetInfo.define(defs, "__tune_nocona__", "1");
            TargetInfo.define(defs, "__REGISTER_PREFIX__", "");

            // Define __NO_MATH_INLINES on linux/x86 so that we don't get inline
            // functions in glibc header files that use FP Stack inline asm which the
            // backend can't deal with (PR879).
            TargetInfo.define(defs, "_NO_MATH_INLINES", "1");

            // Each case falls through to the previous one here.
            switch (sseLevel)
            {
                case SSE42:
                    TargetInfo.define(defs, "__SSE4_2__", "1");
                case SSE41:
                    TargetInfo.define(defs, "__SSE4_1__", "1");
                case SSE3:
                    TargetInfo.define(defs, "__SSE3__", "1");
                case SSE2:
                    TargetInfo.define(defs, "__SSE2__", "1");
                    TargetInfo.define(defs, "__SSE2_MATH__", "1");
                case SSE1:
                    TargetInfo.define(defs, "__SSE__", "1");
                    TargetInfo.define(defs, "__SSE_MATH__", "1");
                case MMX:
                    TargetInfo.define(defs, "__MMX__", "1");
                case NoMMXSSE:
                    break;
            }
        }

        @Override
        public boolean setFeatureEnabled(HashMap<String, Boolean> features,
                String name, boolean enabled)
        {
            if (!features.containsKey(name) && !name.equals("sse4"))
                return false;

            switch (name)
            {
                case "mmx":
                    features.put("mmx", enabled);
                    break;
                case "sse":
                    features.put("mmx", enabled);
                    features.put("sse", enabled);
                    break;
                case "sse2":
                    features.put("mmx", enabled);
                    features.put("sse", enabled);
                    features.put("sse2", enabled);
                    break;
                case "sse3":
                    features.put("mmx", enabled);
                    features.put("sse", enabled);
                    features.put("sse2", enabled);
                    features.put("sse3", enabled);
                    break;
                case "sse4":
                    features.put("mmx", enabled);
                    features.put("sse", enabled);
                    features.put("sse2", enabled);
                    features.put("sse3", enabled);
                    features.put("sse4", enabled);
                    break;
                case "3dnow":
                    features.put("3dnow", enabled);
                    break;
                case "3dnowa":
                    features.put("3dnow", enabled);
                    features.put("3dnowa", enabled);
                    break;
            }
            return true;
        }

        @Override
        public void getDefaultFeatures(String cpu, HashMap<String, Boolean> features)
        {
            features.put("mmx", false);
            features.put("sse", false);
            features.put("sse2", false);
            features.put("sse3", false);
            features.put("sse4", false);
            features.put("3dnow", false);
            features.put("3dnowa", false);

            // X86_64 always has SSE2.
            if (pointerWidth == 64)
            {
                features.put("sse2", true);
                features.put("sse", true);
                features.put("mmx", true);
            }

            if (cpu.equals("generic") || cpu.equals("i386") || cpu.equals("i486")
                    || cpu.equals("i586") || cpu.equals("pentium") || cpu.equals("i686")
                    || cpu.equals("pentiumpro"))
                ;
            else if (cpu.equals("pentium-mmx") || cpu.equals("pentium2"))
                setFeatureEnabled(features, "mmx", true);
            else if (cpu.equals("pentium3"))
                setFeatureEnabled(features, "sse", true);
            else if (cpu.equals("pentium-m") || cpu.equals("x86_64") || cpu.equals("pentuim4"))
                setFeatureEnabled(features, "sse2", true);
            else if (cpu.equals("yanah") || cpu.equals("prescott") || cpu.equals("nocona"))
                setFeatureEnabled(features, "sse3", true);
            else if (cpu.equals("core2"))
                setFeatureEnabled(features, "sse3", true);
            else if (cpu.equals("penryn"))
            {
                setFeatureEnabled(features, "sse4", true);
                features.put("sse42", false);
            }
            else if (cpu.equals("atom"))
                setFeatureEnabled(features, "sse3", true);
            else if (cpu.equals("corei7"))
                setFeatureEnabled(features, "sse4", true);
            else if (cpu.equals("k6") || cpu.equals("winchip-c6"))
                setFeatureEnabled(features, "mmx", true);
            else if (cpu.equals("k6-2") || cpu.equals("k6-3") || cpu.equals("athlon")
                    || cpu.equals("athlon-tbird") || cpu
                    .equals("winchip2") || cpu.equals("c3"))
            {
                setFeatureEnabled(features, "mmx", true);
                setFeatureEnabled(features, "3dnow", true);
            }
            else if (cpu.equals("athlon-4") || cpu.equals("athlon-xp") || cpu.equals("athlon-mp"))
            {
                setFeatureEnabled(features, "sse", true);
                setFeatureEnabled(features, "3dnowa", true);
            }
            else if (cpu.equals("k8") || cpu.equals("opteron") || cpu.equals("athlon64")
                    || cpu.equals("athlon-fx"))
            {
                setFeatureEnabled(features, "sse2", true);
                setFeatureEnabled(features, "3dnowa", true);
            }
            else if (cpu.equals("c3-2"))
            {
                setFeatureEnabled(features, "sse", true);
            }
        }

        /**
         * Performs the initialization of SSELevel according to the user configuration
         * set of features.
         *
         * @param features
         */
        @Override
        public void handleTargetFeatures(HashMap<String, Boolean> features)
        {
            if (features.containsKey("sse42"))
                sseLevel = X86SSEEnum.SSE42;
            else if (features.containsKey("sse41"))
                sseLevel = X86SSEEnum.SSE41;
            else if (features.containsKey("sse3"))
                sseLevel = X86SSEEnum.SSE3;
            else if (features.containsKey("sse2"))
                sseLevel = X86SSEEnum.SSE2;
            else if (features.containsKey("sse"))
                sseLevel = X86SSEEnum.SSE1;
            else if (features.containsKey("mmx"))
                sseLevel = X86SSEEnum.MMX;
        }
    }

    public static class X86_32TargetInfo extends X86TargetInfo
    {
        public X86_32TargetInfo(String triple)
        {
            super(triple);
            doubleAlign = longlongAlign = 32;
            longDoubleWidth = 96;
            longDoubleAlign = 32;
            descriptionString =
                    "e-p:32:32:32-i1:8:8-i8:8:8-i16:16:16-i32:32:32-"
                            + "i64:32:64-f32:32:32-f64:32:64-v64:64:64-v128:128:128-"
                            + "a0:0:64-f80:32:32";

            sizeType = IntType.UnsignedInt;
            PtrDiffType = IntType.SignedInt;
            intPtrType = IntType.SignedInt;
            regParmMax = 3;
        }

        @Override
        public String getVAListDeclaration()
        {
            return "typedef char* __builtin_va_list;";
        }
    }

    // x86-64 generic target
    public static class X86_64TargetInfo extends X86TargetInfo
    {
        public X86_64TargetInfo(String triple)
        {
            super(triple);
            longWidth = longAlign = pointerWidth = pointerAlign = 64;
            longDoubleWidth = 128;
            longDoubleAlign = 128;
            IntMaxType = IntType.SignedLong;
            UIntMaxType = IntType.UnsignedLong;
            int64Type = IntType.SignedLong;
            regParmMax = 6;

            descriptionString =
                    "e-p:64:64:64-i1:8:8-i8:8:8-i16:16:16-i32:32:32-"
                            + "i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:128:128-"
                            + "a0:0:64-s0:64:64-f80:128:128";
        }

        @Override
        public String getVAListDeclaration()
        {
            return "typedef struct __va_list_tag {" +
                    "  unsigned gp_offset;" +
                    "  unsigned fp_offset;" +
                    "  void* overflow_arg_area;" +
                    "  void* reg_save_area;" +
                    "} __va_list_tag;" +
                    "typedef __va_list_tag __builtin_va_list[1];";
        }
    }

    //===----------------------------------------------------------------------===//
    // Defines specific to certain operating systems.
    //===----------------------------------------------------------------------===//

    public interface OSTargetInfo
    {
        /**
         * Defines some useful macros for specific operating system.
         * @param langOpts
         * @param triple
         * @param defs
         */
        void getOSDefines(LangOptions langOpts, String triple, StringBuilder defs);
    }

    public interface LinuxTargetInfo extends OSTargetInfo
    {
        default void getOSDefines(LangOptions langOpts, String triple, StringBuilder defs)
        {
            // Linux defines; list based off of gcc output
            defineStd(defs, "unix", langOpts);
            defineStd(defs, "linux", langOpts);
            define(defs, "__gnu_linux__", "1");
            define(defs, "__ELF__", "1");
        }
    }


    public static final class LinuxX86_32TargetInfo
            extends X86_32TargetInfo implements LinuxTargetInfo
    {
        public LinuxX86_32TargetInfo(String triple)
        {
            super(triple);
            this.userLabelPrefix = "";
        }

        @Override
        public void getTargetDefines(LangOptions opts, StringBuilder defs)
        {
            super.getTargetDefines(opts, defs);
            getOSDefines(opts, getTargetTriple(), defs);
        }
    }

    public static final class LinuxX86_64TargetInfo extends X86_64TargetInfo
            implements LinuxTargetInfo
    {
        public LinuxX86_64TargetInfo(String triple)
        {
            super(triple);
            this.userLabelPrefix = "";
        }

        @Override
        public void getTargetDefines(LangOptions opts, StringBuilder defs)
        {
            super.getTargetDefines(opts, defs);
            getOSDefines(opts, getTargetTriple(), defs);
        }
    }
}
