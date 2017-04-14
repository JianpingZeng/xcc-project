package jlang.system;
/*
 * Extremely C language Compiler.
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

import java.util.Objects;

import static jlang.system.Triple.ArchType.*;

/**
 * Triple - Helper class for working with target triples.
 *
 * Target triples are strings in the format of:
 *   ARCHITECTURE-VENDOR-OPERATING_SYSTEM
 * or
 *   ARCHITECTURE-VENDOR-OPERATING_SYSTEM-ENVIRONMENT
 *
 * This class is used for clients which want to support arbitrary
 * target triples, but also want to implement certain special
 * behavior for particular targets. This class isolates the mapping
 * from the components of the target triple to well known IDs.
 *
 * At its core the Triple class is designed to be a wrapper for a triple
 * string; it does not normally change or normalize the triple string, instead
 * it provides additional APIs to parse normalized parts out of the triple.
 *
 * One curiosity this implies is that for some odd triples the results of,
 * e.g., getOSName() can be very different from the result of getOS().  For
 * example, for 'i386-mingw32', getOS() will return MinGW32, but since
 * getOSName() is purely based on the string structure that will return the
 * empty string.
 *
 * Clients should generally avoid using getOSName() and related APIs unless
 * they are familiar with the triple format (this is particularly true when
 * rewriting a triple).
 * @author Xlous.zeng
 * @version 0.1
 */
public class Triple
{
    public enum ArchType
    {
        UnknownArch,

        x86,    // X86: i[3-9]86
        x86_64, // amd64, x86_64

        InvalidArch,
    }

    public enum VendorType
    {
        UnknownVendor,

        Apple,
        PC,
    }

    public enum OSType
    {
        UnknownOS,

        FreeBSD,
        Linux,
        Solaris,
    }

    private String data;
    /**
     * The parsed architecture type.
     */
    private ArchType arch;
    /**
     * The parsed vendor type.
     */
    private VendorType vendor;
    /**
     * The parsed os type.
     */
    private OSType os;

    private boolean isInitialized()
    {
        return arch != InvalidArch;
    }

    private void parse()
    {
        assert !isInitialized() :"Invalid parse call";

        String archName = getArchName();
        String vendorName = getVendorName();
        String osName = getOSName();

        if (archName.length() == 4 && archName.charAt(0) == 'i'
                && archName.charAt(2) == '8' && archName.charAt(3) == '6'
                && archName.charAt(1) - '3' < 6)
            arch = x86;
        else if (archName.equals("amd64") || archName.equals("x86_64"))
            arch = x86_64;
        else
            arch = UnknownArch;

        if (vendorName.equals("apple"))
            vendor = VendorType.Apple;
        else if (vendorName.equals("pc"))
            vendor = VendorType.PC;
        else
            vendor = VendorType.UnknownVendor;

        if (osName.startsWith("linux"))
            os = OSType.Linux;
        else if (osName.startsWith("freebsd"))
            os = OSType.FreeBSD;
        else
            os = OSType.UnknownOS;

        assert isInitialized() :"Failed to initialize!";
    }

    public Triple()
    {
        arch = InvalidArch;
    }

    public Triple(String tripleStr)
    {
        data = tripleStr;
        arch = InvalidArch;
    }

    public Triple(String archStr, String vendorStr, String osStr)
    {
        data = archStr;
        arch = InvalidArch;
        data += '-';
        data += vendorStr;
        data += '-';
        data += osStr;
    }

    public ArchType getArch()
    {
        if (!isInitialized())
            parse();
        return arch;
    }

    public VendorType getVendor()
    {
        if (!isInitialized())
            parse();
        return vendor;
    }

    public OSType getOS()
    {
        if (!isInitialized())
            parse();
        return os;
    }

    public boolean hasEnvironment()
    {
        return !Objects.equals(getEnvironmentName(), "");
    }

    public String getTriple()
    {
        return data;
    }

    /**
     * Get the architecture (first) component of the triple.
     * @return
     */
    public String getArchName()
    {
        return data.split("-")[0];
    }

    /**
     * Get the vendor (second) component of the triple.
     * @return
     */
    public String getVendorName()
    {
        return data.split("-")[1];
    }

    /**
     * Get the operating system (third) component of the triple.
     * @return
     */
    public String getOSName()
    {
        return data.split("-")[2];
    }

    /**
     * Get the optional environment (fourth) component of the triple, or "" if empty.
     * @return
     */
    public String getEnvironmentName()
    {
        String[] temps = data.split("-");
        if (temps.length > 3)
            return temps[3];
        return null;
    }

    public String getOSAndEnvironmentName()
    {
        String[] temps = data.split("-");
        if (temps.length > 3)
            return temps[2] + "-" + temps[3];
        return temps[2];
    }

    /**
     * Set the architecture (first) component of the triple to a known type.
     * @param arch
     */
    public void setArch(ArchType arch)
    {
        setArchName(getArchTypeName(arch));
    }

    /**
     * Set the vendor (second) component of the triple to a known type.
     * @param vendor
     */
    public void setVendor(VendorType vendor)
    {
        setVendorName(getVendorTypeName(vendor));
    }

    /**
     * Set the operating system (third) component of the triple to a known type.
     * @param os
     */
    public void setOS(OSType os)
    {
        setOSName(getOSTypeName(os));
    }

    /**
     * Set all components to the new triple {@code str}.
     * @param str
     */
    public void setTriple(String str)
    {
        data = str;
        arch = InvalidArch;
    }

    /**
     * Set the architecture (first) component of the triple by name.
     * @param str
     */
    public void setArchName(String str)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        sb.append("-");
        sb.append(getVendorName());
        sb.append("-");
        sb.append(getOSAndEnvironmentName());
        setTriple(sb.toString());
    }

    /**
     * Set the vendor (second) component of the triple
     /// by name.
     * @param str
     */
    public void setVendorName(String str)
    {
        setTriple(getArchName() + "-" + str + "-" + getOSAndEnvironmentName());
    }

    /**
     * Set the operating system (third) component of the
     /// triple by name.
     * @param str
     */
    public void setOSName(String str)
    {
        if (hasEnvironment())
            setTriple(getArchName() + "-" + getVendorName() + "-" + str
            + "-" + getEnvironmentName());
        else
            setTriple(getArchName() + "-" + getVendorName() + "-" + str);
    }

    /**
     * Set the optional environment (fourth)
     /// component of the triple by name.
     * @param str
     */
    public void setEnvironmentName(String str)
    {
        setTriple(getArchName() + "-" + getVendorName() + "-" + getOSName() + str);
    }

    /**
     * Set the operating system and optional
     /// environment components with a single string.
     * @param str
     */
    public void setOSAndEnvironmentName(String str)
    {
        setTriple(getArchName() + "-" + getVendorName() + "-" + str);
    }

    /// getArchTypeName - Get the canonical name for the \arg Kind
    /// architecture.
    public static String getArchTypeName(ArchType kind)
    {
        switch (kind)
        {
            default:
            case InvalidArch: return "<invalid>";
            case UnknownArch: return "unknown";
            case x86: return "i386";
            case x86_64: return "x86_64";
        }
    }

    /// getVendorTypeName - Get the canonical name for the \arg Kind
    /// vendor.
    public static String getVendorTypeName(VendorType kind)
    {
        switch (kind)
        {
            case UnknownVendor: return "unknown";
            case Apple: return "apple";
            case PC: return "pc";
            default:return "<invalid>";
        }
    }


    /// getOSTypeName - Get the canonical name for the \arg Kind vendor.
    public static String getOSTypeName(OSType Kind)
    {
        switch (Kind)
        {
            case Linux: return "linux";
            case UnknownOS: return "unknown";
            default: return "<inavlid>";
        }
    }

    /// getArchTypeForLLVMName - The canonical type for the given LLVM
    /// architecture name (e.g., "x86").
    public static ArchType getArchTypeForLLVMName(String str)
    {
        switch (str)
        {
            case "x86":
                return x86;
            case "x86_64":
                return x86_64;
            default:
                return UnknownArch;
        }
    }
}
