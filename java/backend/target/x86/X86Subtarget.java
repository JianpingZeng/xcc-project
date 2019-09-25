package backend.target.x86;
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

import backend.support.CallingConv;
import backend.support.Triple;
import backend.target.TargetMachine;
import backend.target.TargetSubtarget;
import backend.value.GlobalValue;
import tools.CPUInfoUtility;
import tools.Util;

import static backend.target.x86.X863DNowEnum.*;
import static backend.target.x86.X86GenSubtarget.*;
import static backend.target.x86.X86SSEEnum.*;
import static backend.target.x86.X86Subtarget.TargetType.*;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public abstract class X86Subtarget extends TargetSubtarget {
  public enum PICStyle {
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

  /**
   * Which PIC style to use
   */
  private PICStyle picStyle;

  /**
   * MMX, SSE1, SSE2, SSE3, SSSE3, SSE41, SSE42, or
   * none supported.
   */
  X86SSEEnum x86SSELevel;
  /**
   * 3DNow or 3DNow Athlon, or none supported.
   */
  X863DNowEnum x863DNowLevel;
  /**
   * Does this processor support conditional move instructions (available on pentium pro+)
   */
   boolean hasCMov;
  /**
   * True if the processor supports X86-64 instructions.
   */
  boolean hasX86_64;

  /**
   * True if the processor supports SSE4A instructions.
   */
  boolean hasSSE4A;

  /**
   * Target has AVX instructions
   */
  boolean hasAVX;

  boolean hasAVX2;

  /**
   * Target has 3-operand fused multiply-add
   */
  boolean hasFMA3;

  /**
   * Target has 4-operand fused multiply-add
   */
  boolean hasFMA4;

  /**
   * True if BT (bit test) of memory instructions are slow.
   */
  boolean isBTMemSlow;

  /**
   * Nonzero if this is a darwin platform: the numeric
   * version of the platform, e.g. 8 = 10.4 (Tiger), 9 = 10.5 (Leopard), etc.
   */
  private int darwinVers;

  /**
   * true if this is a "linux" platform.
   */
  private boolean isLinux;

  /**
   * The minimum alignment known to hold of the stack frame on
   * entry to the function and which must be maintained by every function.
   */
  private int stackAlignemnt;

  /**
   * Max. memset / memcpy size that is turned into rep/movs, rep/stos ops.
   */
  private int maxInlineSizeThreshold;

  /**
   * True if the processor supports 64-bit instructions and
   * pointer size is 64 bit.
   */
  protected boolean in64BitMode;

  boolean hasAES;
  boolean hasBMI;
  boolean hasBMI2;
  boolean hasCLMUL;
  boolean hasF16C;
  boolean hasFSGSBase;
  boolean hasLZCNT;
  boolean hasMOVBE;
  boolean asPOPCNT;
  boolean hasRDRAND;
  boolean hasVectorUAMem;
  boolean hasXOP;
  boolean hasPOPCNT;
  boolean hasCmpxchg16b;
  boolean isUAMemFast;

  public enum TargetType {
    isELF, isCygwin, isDarwin, isWindows, isMingw
  }

  private TargetType targetType;
  private boolean isLittleEndian;
  private X86TargetMachine tm;
  private Triple targetTriple;

  protected X86Subtarget(X86TargetMachine tm,
                         String tt,String cpu,
                         String fs,
                         int stackAlignOverride,
                         boolean is64Bit) {
    this.tm = tm;
    picStyle = PICStyle.None;
    x86SSELevel = NoMMXSSE;
    x863DNowLevel = NoThreeDNow;
    hasX86_64 = false;
    hasSSE4A = false;
    hasAVX = false;
    hasAVX2 = false;
    hasFMA3 = false;
    hasFMA4 = false;
    isBTMemSlow = false;
    darwinVers = 0;
    isLinux = false;
    // This is known good to Yonah, but I don't known about other.
    maxInlineSizeThreshold = 128;
    this.in64BitMode = is64Bit;
    // Default to ELF unless user explicitly specify.
    targetType = isELF;
    this.isLittleEndian = true;
    stackAlignemnt = 8;
    targetTriple = new Triple(tt);
    // default to hard float ABI.

    // determine default and user specified characteristics.
    if ((fs != null && !fs.isEmpty() || (cpu != null && !cpu.isEmpty()))) {
      // if feature string is not empty and null, parse features string.
      String cpuName = getCurrentX86CPU(isLittleEndian);
      // Make sure 64-bit features are available in 64-bit mode. (But make sure
      // SSE2 can be turned off explicitly.)
      String fullFS = fs;
      if (is64Bit) {
        // All X86-64 CPUs also have SSE2, however user might request no SSE via
        // -mattr, so don't force SSELevel here.
        if (fullFS != null && !fullFS.isEmpty())
          fullFS = "+64bit,+sse2," + fullFS;
        else
          fullFS = "+64bit,+sse2";
      }

      parseSubtargetFeatures(fullFS, cpuName);
    } else {
      // Otherwise, automatical detect CPU type and kind.
      autoDetectSubtargetFeatures();
      // make sure SSE2 is enabled, it is available in all of x86_64 CPU.
      if (is64Bit) {
        hasX86_64 = true;
        toggleFeature(Feature64Bit);
        hasCMov = true;
        toggleFeature(FeatureCMOV);
        if (x86SSELevel.compareTo(SSE2) < 0) {
          x86SSELevel = SSE2;
          toggleFeature(FeatureSSE1);
          toggleFeature(FeatureSSE2);
        }
      }
    }

    if (in64BitMode)
      toggleFeature(Mode64Bit);

    Util.Debug("Subtarget features: SSELevel " + x86SSELevel
        + ", 3DNowLevel " + x863DNowLevel + ", 64bit " + hasX86_64);
    Util.assertion(!is64Bit || hasX86_64, "64-bit code requested on a subtarget that doesn't support it!");

    if (tt.length() > 5) {
      int pos = -1;
      if ((pos = tt.indexOf("-darwin")) != -1) {
        targetType = isDarwin;
        // compute darwin version.
        if (Character.isDigit(tt.charAt(pos + 7)))
          darwinVers = tt.charAt(pos + 7) - '0';
        else {
          darwinVers = 8; // Minimum supported darwin is Tiger.
        }
      }
      if ((pos = tt.indexOf("-linux")) != -1) {
        targetType = isELF;
        isLinux = true;
      } else if ((pos = tt.indexOf("cygwin")) != -1) {
        targetType = isCygwin;
      } else if ((pos = tt.indexOf("mingw")) != -1) {
        targetType = isMingw;
      } else if ((pos = tt.indexOf("win32")) != -1) {
        targetType = isWindows;
      } else if ((pos = tt.indexOf("-cl")) != -1) {
        targetType = isDarwin;
        darwinVers = 9;
      }
    }

    // Stack alignment is 16 Bytes on Darwin (both 32 and 64 bit) and
    // for all 64 bit targets.
    // Stack alignment is 16 bytes on Darwin, FreeBSD, Linux and Solaris (both
    // 32 and 64 bit) and for all 64-bit targets.
    if (stackAlignOverride != 0)
      stackAlignemnt = stackAlignOverride;
    else if (isTargetDarwin() || isTargetLinux() ||
        isTargetFreeBSD() || isTargetSolaris() || is64Bit)
      stackAlignemnt = 16;
  }

  public X86TargetMachine getTargetMachine() {
    return tm;
  }

  public X86RegisterInfo getRegisterInfo() {
    if (regInfo == null)
      regInfo = new X86GenRegisterInfo(tm, getHwMode());
    return (X86RegisterInfo) regInfo;
  }

  public X86InstrInfo getInstrInfo() {
    if (instrInfo == null)
      instrInfo = new X86GenInstrInfo(tm);
    return (X86InstrInfo) instrInfo;
  }

  public int getStackAlignemnt() {
    return stackAlignemnt;
  }

  public int getMaxInlineSizeThreshold() {
    return maxInlineSizeThreshold;
  }

  /**
   * Return an array containing [family, model]
   * @param eax
   * @return
   */
  private static int[] detectFamilyModel(int eax) {
    // return [family, model].
    int family = (eax >> 8) & 0xf;
    int model = (eax >> 4) & 0xf;
    if (family == 6 || family == 0xf) {
      family += (eax >> 20) & 0xff;
    }
    model += ((eax >> 16) & 0xff) << 4;
    return new int[]{family, model};
  }

  private static String convertToString(int[] cpuName, int start, boolean isLittelEndian) {
    StringBuilder buf = new StringBuilder();
    for (int i = start; i < cpuName.length; i++) {
      int val = cpuName[i];
      if (isLittelEndian) {
        buf.append((char) (val & 0xff));
        buf.append((char) ((val >>> 8) & 0xff));
        buf.append((char) ((val >>> 16) & 0xff));
        buf.append((char) ((val >>> 24) & 0xff));
      } else {
        buf.append((char) ((val >>> 24) & 0xff));
        buf.append((char) ((val >>> 16) & 0xff));
        buf.append((char) ((val >>> 8) & 0xff));
        buf.append((char) (val & 0xff));
      }
    }
    if (Util.DEBUG)
      System.err.println(buf.toString());

    return buf.toString();
  }

  public void autoDetectSubtargetFeatures() {
    int[] u = new int[4];
    if (CPUInfoUtility.getCpuIDAndInfo(0, u))
      return;

    //Compute name of CPU.
    // swap u[2] with u[3]
    int t = u[2];
    u[2] = u[3];
    u[3] = t;
    String cpuName = convertToString(u, 1, isLittleEndian);
    int eax = u[0];
    int[] u2 = new int[4];
    u2[0] = eax;    // EAX, EBX, ecx, edx.
    CPUInfoUtility.getCpuIDAndInfo(0x1, u2);
    eax = u2[0];
    int ebx = u2[1], ecx = u2[2], edx = u2[3];
    if (((edx >> 15) & 1) != 0) {
      hasCMov = true;
      toggleFeature(FeatureCMOV);
    }
    if (((edx >> 23) & 0x1) != 0) {
      x86SSELevel = MMX;
      toggleFeature(FeatureMMX);
    }
    if (((edx >> 25) & 0x1) != 0) {
      x86SSELevel = SSE1;
      toggleFeature(FeatureSSE1);
    }
    if (((edx >> 26) & 0x1) != 0) {
      x86SSELevel = SSE2;
      toggleFeature(FeatureSSE2);
    }
    if ((ecx & 0x1) != 0) {
      x86SSELevel = SSE3;
      toggleFeature(FeatureSSE3);
    }
    if (((ecx >> 9) & 0x1) != 0) {
      x86SSELevel = SSSE3;
      toggleFeature(FeatureSSSE3);
    }
    if (((ecx >> 19) & 0x1) != 0) {
      x86SSELevel = SSE41;
      toggleFeature(FeatureSSE41);
    }
    if (((ecx >> 20) & 0x1) != 0) {
      x86SSELevel = SSE42;
      toggleFeature(FeatureSSE42);
    }
    boolean isIntel = cpuName.equals("GenuineIntel");
    boolean isAMD = cpuName.equals("AuthenticAMD");
    hasFMA3 = isIntel && ((ecx >> 12) & 0x1) != 0;
    hasAVX = ((ecx >> 28) & 0x1) != 0;
    if (isIntel && ((ecx >> 1) & 0x1) != 0) {
      hasCLMUL = true;
      toggleFeature(FeatureCLMUL);
    }
    if (isIntel && ((ecx >> 1) & 0x1) != 0) {
      hasCLMUL = true;
      toggleFeature(FeatureCLMUL);
    }
    if (isIntel && ((ecx >> 12) & 0x1) != 0) {
      hasFMA3 = true;
      toggleFeature(FeatureFMA3);
    }
    if (isIntel && ((ecx >> 22) & 0x1) != 0) {
      hasMOVBE = true;
      toggleFeature(FeatureMOVBE);
    }
    if (isIntel && ((ecx >> 23) & 0x1) != 0) {
      hasPOPCNT = true;
      toggleFeature(FeaturePOPCNT);
    }
    if (isIntel && ((ecx >> 25) & 0x1) != 0) {
      hasAES = true;
      toggleFeature(FeatureAES);
    }
    if (isIntel && ((ecx >> 29) & 0x1) != 0) {
      hasF16C = true;
      toggleFeature(FeatureF16C);
    }
    if (isIntel && ((ecx >> 30) & 0x1) != 0) {
      hasRDRAND = true;
      toggleFeature(FeatureRDRAND);
    }
    if (((ecx >> 13) & 0x1) != 0) {
      hasCmpxchg16b = true;
      toggleFeature(FeatureCMPXCHG16B);
    }

    if (isIntel || isAMD) {
      // Determine if bit test memory instructions are slow.
      int[] temp = detectFamilyModel(eax);
      int family = temp[0];
      int model = temp[1];

      if (isAMD || (family == 6 && model >= 13)) {
        isBTMemSlow = true;
        toggleFeature(FeatureSlowBTMem);
      }
      // If it's Nehalem, unaligned memory access is fast.
      // FIXME: Nehalem is family 6. Also include Westmere and later processors?
      if (family == 15 && model == 26) {
        isUAMemFast = true;
        toggleFeature(FeatureFastUAMem);
      }

      int maxExtLevel;
      u2 = new int[4];
      u2[0] = 0;    // EAX, EBX, ecx, edx.
      u2[1] = ebx;
      u2[2] = ecx;
      u2[3] = edx;
      CPUInfoUtility.getCpuIDAndInfo(0x80000000, u2);
      maxExtLevel = u2[0];

      if (maxExtLevel >= 0x80000001) {
        u2[0] = eax;    // EAX, EBX, ecx, edx.

        CPUInfoUtility.getCpuIDAndInfo(0x80000001, u2);
        eax = u2[0];
        ebx = u2[1];
        ecx = u2[2];
        edx = u2[3];

        if (((edx >> 29) & 0x1) != 0) {
          hasX86_64 = true;
          toggleFeature(Feature64Bit);
        }
        if (((ecx >> 5) & 0x1) != 0) {
          hasLZCNT = true;
          toggleFeature(FeatureLZCNT);
        }
        if (isAMD) {
          if (((ecx >> 6) & 0x1) != 0) {
            hasSSE4A = true;
            toggleFeature(FeatureSSE4A);
          }
          if (((ecx >> 11) & 0x1) != 0) {
            hasXOP = true;
            toggleFeature(FeatureXOP);
          }
          if (((ecx >> 16) & 0x1) != 0) {
            hasFMA4 = true;
            toggleFeature(FeatureFMA4);
          }
        }
      }
    }
  }

  public boolean is64Bit() {
    return in64BitMode;
  }

  public PICStyle getPICStyle() {
    return picStyle;
  }

  public void setPICStyle(PICStyle picStyle) {
    this.picStyle = picStyle;
  }

  public boolean hasMMX() {
    return x86SSELevel.ordinal() >= MMX.ordinal();
  }

  public boolean hasSSE1() {
    return x86SSELevel.ordinal() >= SSE1.ordinal();
  }

  public boolean hasSSE2() {
    return x86SSELevel.ordinal() >= SSE2.ordinal();
  }

  public boolean hasSSE3() {
    return x86SSELevel.ordinal() >= SSE3.ordinal();
  }

  public boolean hasSSE41() {
    return x86SSELevel.ordinal() >= SSE41.ordinal();
  }

  public boolean hasSSE42() {
    return x86SSELevel.ordinal() >= SSE42.ordinal();
  }

  public boolean hasSSE4A() {
    return hasSSE4A;
  }

  public boolean has3DNow() {
    return x863DNowLevel.ordinal() >= ThreeDNow.ordinal();
  }

  public boolean has3DNowA() {
    return x863DNowLevel.ordinal() >= ThreeDNowA.ordinal();
  }

  public boolean hasAVX() {
    return hasAVX;
  }

  public boolean hasFMA3() {
    return hasFMA3;
  }

  public boolean hasFMA4() {
    return hasFMA4;
  }

  public boolean isBIMemSlow() {
    return isBTMemSlow;
  }

  public boolean isTargetLinux() {
    return targetTriple.getOS() == Triple.OSType.Linux;
  }

  public boolean isTargetSolaris() {
    return targetTriple.getOS() == Triple.OSType.Solaris;
  }

  public boolean isTargetFreeBSD() {
    return targetTriple.getOS() == Triple.OSType.FreeBSD;
  }

  public boolean isTargetDarwin() {
    return targetType == isDarwin;
  }

  public boolean isTargetELF() {
    return targetType == isELF;
  }

  public boolean isTargetWindows() {
    return targetType == isWindows;
  }

  public boolean isTargetMingw() {
    return targetType == isMingw;
  }

  public boolean isTargetCygMing() {
    return targetType == isMingw || targetType == isCygwin;
  }

  public boolean isTargetCygwin() {
    return targetType == isCygwin;
  }

  public boolean isTargetWin64() {
    return in64BitMode && (targetType == isMingw || targetType == isWindows);
  }

  public void setIsLittleEndian(boolean isLittleEndian) {
    this.isLittleEndian = isLittleEndian;
  }

  public boolean isLittleEndian() {
    return isLittleEndian;
  }

  public String getDataLayout() {
    if (is64Bit())
      return "e-p:64:64-s:64-f64:64:64-i64:64:64-f80:128:128";
    else if (isTargetDarwin())
      return "e-p:32:32-f64:32:64-i64:32:64-f80:128:128";
    else
      return "e-p:32:32-f64:32:64-i64:32:64-f80:32:32";
  }

  public boolean isPICStyleSet() {
    return picStyle != PICStyle.None;
  }

  public boolean isPICStyleGOT() {
    return picStyle == PICStyle.GOT;
  }

  public boolean isPICStyleRIPRel() {
    return picStyle == PICStyle.RIPRel;
  }

  public boolean isPICStyleStubPIC() {
    return picStyle == PICStyle.StubPIC;
  }

  public boolean isPICStyleStubNoDynamic() {
    return picStyle == PICStyle.StubDynamicNoPIC;
  }

  public boolean isPICStyleStubAny() {
    return picStyle == PICStyle.StubDynamicNoPIC
        || picStyle == PICStyle.StubPIC;
  }

  /// getDarwinVers - Return the darwin version number, 8 = Tiger, 9 = Leopard,
  /// 10 = Snow Leopard, etc.
  public int getDarwinVers() {
    return darwinVers;
  }

  /// isLinux - Return true if the target is "Linux".
  public boolean isLinux() {
    return isLinux;
  }

  /// ClassifyGlobalReference - Classify a global variable reference for the
  /// current subtarget according to how we should reference it in a non-pcrel
  /// context.
  public int classifyGlobalReference(GlobalValue GV,
                                     TargetMachine tm) {
    // TODO: 17-8-5
    return 0;
  }

  /// IsLegalToCallImmediateAddr - Return true if the subtarget allows calls
  /// to immediate address.
  public boolean isLegalToCallImmediateAddr(TargetMachine tm) {
    // TODO: 17-8-5
    return false;
  }

  /**
   * This function returns the name of a function which has an interface
   * /// like the non-standard bzero function, if such a function exists on
   * /// the current subtarget and it is considered prefereable over
   * /// memset with zero passed as the second argument. Otherwise it
   * /// returns null.
   *
   * @return
   */
  public String getBZeroEntry() {
    if (getDarwinVers() >= 10)
      return "__bzero";

    return null;
  }

  /**
   * For targets where it is beneficial to
   * /// backschedule instructions that compute addresses, return a value
   * /// indicating the number of scheduling cycles of backscheduling that
   * /// should be attempted.
   *
   * @return
   */
  @Override
  public int getSpecialAddressLatency() {
    return 200;
  }

  private static String getCurrentX86CPU(boolean isLittleEndian) {
    int[] u = new int[4];  // {EAX, EBX, ecx, edx}
    if (CPUInfoUtility.getCpuIDAndInfo(0x1, u))
      return "generic";
    int[] res = detectFamilyModel(u[0]);
    int family = res[0];
    int model = res[1];

    CPUInfoUtility.getCpuIDAndInfo(0x80000001, u);
    boolean em64T = ((u[3] >> 29) & 0x1) != 0;
    boolean hasSSE3 = (u[2] & 0x01) != 0;

    int[] u2 = new int[4];
    u2[0] = u[0];   // EAX
    CPUInfoUtility.getCpuIDAndInfo(0, u2);
    u[0] = u2[0];   // update EAX

    // swap u[2] with u[3]
    int t = u2[2];
    u2[2] = u2[3];
    u2[3] = t;
    String cpuName = convertToString(u2, 1, isLittleEndian);
    if (cpuName.equals("GenuineIntel")) {
      switch (family) {
        case 3:
          return "i386";
        case 4:
          return "i486";
        case 5:
          switch (model) {
            case 4:
              return "pentium-mmx";
            default:
              return "pentium";
          }
        case 6:
          switch (model) {
            case 1:
              return "pentiumpro";
            case 3:
            case 5:
            case 6:
              return "pentium2";
            case 7:
            case 8:
            case 10:
            case 11:
              return "pentium3";
            case 9:
            case 13:
              return "pentium-m";
            case 14:
              return "yonah";
            case 15:
            case 22: // Celeron M 540
              return "core2";
            case 23: // 45nm: Penryn , Wolfdale, Yorkfield (XE)
              return "penryn";
            default:
              return "i686";
          }
        case 15: {
          switch (model) {
            case 3:
            case 4:
            case 6: // same as 4, but 65nm
              return (em64T) ? "nocona" : "prescott";
            case 26:
              return "corei7";
            case 28:
              return "atom";
            default:
              return (em64T) ? "x86-64" : "pentium4";
          }
        }
        default:
          return "generic";
      }
    } else if (cpuName.equals("AuthenticAMD")) {  // FIXME: this poorly matches the generated SubtargetFeatureKV table.  There
      // appears to be no way to generate the wide variety of AMD-specific targets
      // from the information returned from CPUID.
      switch (family) {
        case 4:
          return "i486";
        case 5:
          switch (model) {
            case 6:
            case 7:
              return "k6";
            case 8:
              return "k6-2";
            case 9:
            case 13:
              return "k6-3";
            default:
              return "pentium";
          }
        case 6:
          switch (model) {
            case 4:
              return "athlon-tbird";
            case 6:
            case 7:
            case 8:
              return "athlon-mp";
            case 10:
              return "athlon-xp";
            default:
              return "athlon";
          }
        case 15:
          if (hasSSE3) {
            return "k8-sse3";
          } else {
            switch (model) {
              case 1:
                return "opteron";
              case 5:
                return "athlon-fx"; // also opteron
              default:
                return "athlon64";
            }
          }
        case 16:
          return "amdfam10";
        default:
          return "generic";
      }
    } else {
      return "generic";
    }
  }

  public boolean isCallingConvWin64(CallingConv cc) {
    switch (cc) {
      case C:
      case Fast:
      case X86_FastCall:
      case X86_StdCall:
        return isTargetWin64();
      default:
        return false;
    }
  }

  /**
   * Classify a blockaddress reference for the
   * current subtarget according to how we should reference it in a non-pcrel context.
   * @return
   */
  public int classifyBlockAddressReference() {
    // 32 bit ELF target.
    if (isPICStyleGOT())
      return X86II.MO_GOTOFF;

    // darwin/32 in PIC mode
    if (isPICStyleStubPIC())
      return X86II.MO_PIC_BASE_OFFSET;
    // direct static reference to label.
    return X86II.MO_NO_FLAG;
  }
}
