package backend.target.x86;
/*
 * Xlous C language Compiler
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

import backend.target.TargetMachine;
import backend.target.TargetSubtarget;
import backend.value.GlobalValue;
import tools.Util;

import static backend.target.x86.X86Subtarget.TargetType.*;
import static backend.target.x86.X86Subtarget.X863DNowEnum.NoThreeDNow;
import static backend.target.x86.X86Subtarget.X863DNowEnum.ThreeDNow;
import static backend.target.x86.X86Subtarget.X863DNowEnum.ThreeDNowA;
import static backend.target.x86.X86Subtarget.X86SSEEnum.*;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class X86Subtarget extends TargetSubtarget
{
    public enum PICStyle
    {
        /**
         * sed on i386-darwin in -fPIC mode.
         */
        StubPIC,

        /**
         * Used on i386-darwin in -mdynamic-no-pic mode.
         */
        StubDynamicNoPIC,

        /**
         * Used on many 32-bit unices in -fPIC mode.
         */
        GOT,

        /**
         * Used on X86-64 when not in -static mode.
         */
        RIPRel,

        /**
         * Set when in -static mode (not PIC or DynamicNoPIC mode).
         */
        None
    }

    protected enum X86SSEEnum
    {
        NoMMXSSE, MMX, SSE1, SSE2, SSE3, SSE41, SSE42
    }

    protected enum X863DNowEnum
    {
        NoThreeDNow, ThreeDNow, ThreeDNowA
    }

    /**
     * Which PIC style to use
     */
    protected PICStyle picStyle;

    /**
     * MMX, SSE1, SSE2, SSE3, SSSE3, SSE41, SSE42, or
     * none supported.
     */
    protected X86SSEEnum x86SSELevel;
    /**
     * 3DNow or 3DNow Athlon, or none supported.
     */
    protected X863DNowEnum x863DNowLevel;

    /**
     * True if the processor supports X86-64 instructions.
     */
    protected boolean hasX86_64;

    /**
     * True if the processor supports SSE4A instructions.
     */
    protected boolean hasSSE4A;

    /**
     * Target has AVX instructions
     */
    protected boolean hasAVX;

    /**
     * Target has 3-operand fused multiply-add
     */
    protected boolean hasFMA3;

    /**
     * Target has 4-operand fused multiply-add
     */
    protected boolean hasFMA4;

    /**
     * True if BT (bit test) of memory instructions are slow.
     */
    protected boolean isBTMemSlow;

    /**
     * Nonzero if this is a darwin platform: the numeric
     * version of the platform, e.g. 8 = 10.4 (Tiger), 9 = 10.5 (Leopard), etc.
     */
    protected int darwinVers;

    /**
     * true if this is a "linux" platform.
     */
    protected boolean isLinux;

    /**
     * The minimum alignment known to hold of the stack frame on
     * entry to the function and which must be maintained by every function.
     */
    protected int stackAlignemnt;

    /**
     * Max. memset / memcpy size that is turned into rep/movs, rep/stos ops.
     */
    protected int maxInlineSizeThreshold;

    /**
     * True if the processor supports 64-bit instructions and
     * pointer size is 64 bit.
     */
    protected boolean is64Bit;

    public enum TargetType
    {
        isELF, isCygwin, isDarwin, isWindows, isMingw
    }

    public TargetType targetType;

    protected X86Subtarget(String tt, String fs, boolean is64bit)
    {
        picStyle = PICStyle.None;
        x86SSELevel = NoMMXSSE;
        x863DNowLevel = NoThreeDNow;
        hasX86_64 = false;
        hasSSE4A = false;
        hasAVX = false;
        hasFMA3 = false;
        hasFMA4 = false;
        isBTMemSlow = false;
        darwinVers = 0;
        isLinux = false;
        stackAlignemnt = 8;
        // This is known good to Yonah, but I don't known about other.
        maxInlineSizeThreshold = 128;
        this.is64Bit = is64bit;
        // Default to ELF unless user explicitly specify.
        targetType = isELF;

        // default to hard float ABI.

        // determine default and user specified characteristics.
        if (fs != null && !fs.isEmpty())
        {
            // if feature string is not empty and null, parse features string.
            String cpu = getCurrentX86CPU();
            parseSubtargetFeatures(fs, cpu);
            // All X86-64 CPUs also have SSE2, however user might request no SSE via
            // -mattr, so don't force SSELevel here.
        }
        else
        {
            // Otherwise, automatical detect CPU type and kind.
            autoDetectSubtargetFeatures();
            // make sure SSE2 is enabled, it is available in all of x86_64 CPU.
            if (is64bit && x86SSELevel.compareTo(SSE2) < 0)
                x86SSELevel = SSE2;
        }

        // If requesting codegen for X86-64, make sure that 64-bit features
        // are enabled.
        if (is64bit)
            hasX86_64 = true;

        Util.DEBUG("Subtarget features: SSELevel " + x86SSELevel
                + ", 3DNowLevel " + x863DNowLevel + ", 64bit " + hasX86_64);
        assert !is64bit || hasX86_64 :"64-bit code requested on a subtarget that doesn't support it!";
        if (tt.length() > 5)
        {
            int pos = -1;
            if ((pos = tt.indexOf("-darwin")) != -1)
            {
                targetType = isDarwin;
                // compute darwin version.
                if (Character.isDigit(tt.charAt(pos+7)))
                    darwinVers = tt.charAt(pos+7) - '0';
                else
                {
                    darwinVers = 8; // Minimum supported darwin is Tiger.
                }
            }
            if ((pos = tt.indexOf("-linux")) != -1)
            {
                targetType = isELF;
                isLinux = true;
            }
            else if ((pos = tt.indexOf("cygwin")) != -1)
            {
                targetType = isCygwin;
            }
            else if ((pos = tt.indexOf("mingw")) != -1)
            {
                targetType = isMingw;
            }
            else if ((pos = tt.indexOf("win32")) != -1)
            {
                targetType = isWindows;
            }
            else if ((pos = tt.indexOf("-cl")) != -1)
            {
                targetType = isDarwin;
                darwinVers = 9;
            }
        }

        // Stack alignment is 16 Bytes on Darwin (both 32 and 64 bit) and
        // for all 64 bit targets.
        if (targetType == isDarwin || is64bit)
            stackAlignemnt = 16;
    }

    /**
     * Create a X86Subtarget instance with specified target triple, features string,
     * and predicate.
     * @param tt    Target triple
     * @param fs    Features string
     * @param is64bit   Flag to indicates whether current platform is 64 bit or not.
     * @return
     */
    public static X86Subtarget createX86Subtarget(String tt, String fs, boolean is64bit)
    {
        return new X86GenSubtarget(tt, fs, is64bit);
    }

    public int getStackAlignemnt()
    {
        return stackAlignemnt;
    }

    public int getMaxInlineSizeThreshold()
    {
        return maxInlineSizeThreshold;
    }

    public String parseSubtargetFeatures(String fs, String cpu)
    {
        return "";
    }

    public void autoDetectSubtargetFeatures()
    {
    }

    public boolean is64Bit()
    {
        return is64Bit;
    }

    public PICStyle getPICStyle()
    {
        return picStyle;
    }

    public void setPICStyle(PICStyle picStyle)
    {
        this.picStyle = picStyle;
    }

    public boolean hasMMX()
    {
        return x86SSELevel.ordinal() >= MMX.ordinal();
    }

    public boolean hasSSE1()
    {
        return x86SSELevel.ordinal() >= SSE1.ordinal();
    }

    public boolean hasSSE2()
    {
        return x86SSELevel.ordinal() >= SSE2.ordinal();
    }

    public boolean hasSSE3()
    {
        return x86SSELevel.ordinal() >= SSE3.ordinal();
    }

    public boolean hasSSE41()
    {
        return x86SSELevel.ordinal() >= SSE41.ordinal();
    }

    public boolean hasSSE42()
    {
        return x86SSELevel.ordinal() >= SSE42.ordinal();
    }

    public boolean hasSSE4A()
    {
        return hasSSE4A;
    }

    public boolean has3DNow()
    {
        return x863DNowLevel.ordinal() >= ThreeDNow.ordinal();
    }

    public boolean has3DNowA()
    {
        return x863DNowLevel.ordinal() >= ThreeDNowA.ordinal();
    }

    public boolean hasAVX()
    {
        return hasAVX;
    }

    public boolean hasFMA3()
    {
        return hasFMA3;
    }

    public boolean hasFMA4()
    {
        return hasFMA4;
    }

    public boolean isBIMemSlow()
    {
        return isBTMemSlow;
    }

    public boolean isTargetDarwin()
    {
        return targetType == isDarwin;
    }

    public boolean isTargetELF()
    {
        return targetType == isELF;
    }

    public boolean isTargetWindows()
    {
        return targetType == isWindows;
    }

    public boolean isTargetMingw()
    {
        return targetType == isMingw;
    }

    public boolean isTargetCygMing()
    {
        return targetType == isMingw || targetType == isCygwin;
    }

    public boolean isTargetCygwin()
    {
        return targetType == isCygwin;
    }

    public boolean isTargetWin64()
    {
        return is64Bit && (targetType == isMingw || targetType == isWindows);
    }

    public String getDataLayout()
    {
        if (is64Bit())
            return "e-p:64:64:64-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:128:128-a0:0:64-s0:64:64-f80:128:128";
        else if (isTargetDarwin())
            return  "e-p:32:32-f64:32:64-i64:32:64-f80:128:128";
        else
            return  "e-p:32:32:32-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:32:64-f32:32:32-f64:32:64-v64:64:64-v128:128:128-a0:0:64-f80:32:32";
    }

    public boolean isPICStyleSet()
    {
        return picStyle != PICStyle.None;
    }

    public boolean isPICStyleGOT()
    {
        return picStyle == PICStyle.GOT;
    }

    public boolean isPICStyleRIPRel()
    {
        return picStyle == PICStyle.RIPRel;
    }

    public boolean isPICStyleStubPIC()
    {
        return picStyle == PICStyle.StubPIC;
    }

    public boolean isPICStyleStubNoDynamic()
    {
        return picStyle == PICStyle.StubDynamicNoPIC;
    }

    public boolean isPICStyleStubAny()
    {
        return picStyle == PICStyle.StubDynamicNoPIC
                || picStyle == PICStyle.StubPIC;
    }

    /// getDarwinVers - Return the darwin version number, 8 = Tiger, 9 = Leopard,
    /// 10 = Snow Leopard, etc.
    public int getDarwinVers()
    {
        return darwinVers;
    }

    /// isLinux - Return true if the target is "Linux".
    public boolean isLinux()
    {
        return isLinux;
    }

    /// ClassifyGlobalReference - Classify a global variable reference for the
    /// current subtarget according to how we should reference it in a non-pcrel
    /// context.
    public int classifyGlobalReference(GlobalValue GV,
                                        TargetMachine tm)
    {
        // TODO: 17-8-5
        return 0;
    }

    /// IsLegalToCallImmediateAddr - Return true if the subtarget allows calls
    /// to immediate address.
    public boolean isLegalToCallImmediateAddr(TargetMachine tm)
    {
        // TODO: 17-8-5
        return false;
    }

    /// This function returns the asmName of a function which has an interface
    /// like the non-standard bzero function, if such a function exists on
    /// the current subtarget and it is considered prefereable over
    /// memset with zero passed as the second argument. Otherwise it
    /// returns null.
    public String getBZeroEntry()
    {
        // TODO: 17-8-5
        return null;
    }

    /// getSpecialAddressLatency - For targets where it is beneficial to
    /// backschedule instructions that compute addresses, return a value
    /// indicating the number of scheduling cycles of backscheduling that
    /// should be attempted.
    public int getSpecialAddressLatency()
    {
        // TODO: 17-8-5
        return 0;
    }

    private static String getCurrentX86CPU()
    {
        // TODO: 2017/11/17
        return null;
    }
}
