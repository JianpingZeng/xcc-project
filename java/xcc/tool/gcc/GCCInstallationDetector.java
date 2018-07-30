/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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

package xcc.tool.gcc;

import backend.support.Triple;
import xcc.Driver;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public class GCCInstallationDetector
{
    private boolean isValid;
    private String gccTriple;
    private String gccInstallPath;
    private String gccParentLibPath;
    private GCCVersion version;

    public GCCInstallationDetector(Driver driver)
    {
        isValid = false;
        gccTriple = driver.getHostTriple();

        Triple.ArchType arch = new Triple(gccTriple).getArch();
        ArrayList<String> candidateLibDirs = new ArrayList<>();
        ArrayList<String> candidateTriples = new ArrayList<>();
        collectLibDirsAndTriple(arch, candidateLibDirs, candidateTriples);

        ArrayList<String> prefixes = new ArrayList<>();
        prefixes.add("/usr");
        for (String prefix : driver.getInstalledDir())
            prefixes.add(prefix + "/..");

        version = GCCVersion.parse("0.0.0");
        for (int i = 0, e = prefixes.size(); i < e; i++)
        {
            if (!Files.exists(Paths.get(prefixes.get(i))))
                continue;

            for (int j = 0, sz = candidateLibDirs.size(); j < sz; j++)
            {
                String libDir = prefixes.get(i) + candidateLibDirs.get(j);
                if (!Files.exists(Paths.get(libDir)))
                    continue;
                for (int k = 0, ke = candidateTriples.size(); k < ke; k++)
                {
                    scanLibDirForGCCTriple(arch, libDir, candidateTriples.get(k));
                }
            }
        }
    }

    public boolean isValid()
    {
        return isValid;
    }

    public String getGccTriple()
    {
        return gccTriple;
    }

    public String getGccInstallPath()
    {
        return gccInstallPath;
    }

    public String getGccParentLibPath()
    {
        return gccParentLibPath;
    }

    public GCCVersion getVersion()
    {
        return version;
    }

    private static void collectLibDirsAndTriple(Triple.ArchType arch,
            ArrayList<String> libDirs, ArrayList<String> triples)
    {
        if (arch == Triple.ArchType.x86_64)
        {
            String[] X86_64_LibDirs = { "/lib64", "/lib" };
            String[] X86_64Triples = {
                    "x86_64-linux-gnu",
                    "x86_64-unknown-linux-gnu",
                    "x86_64-pc-linux-gnu",
                    "x86_64-redhat-linux6E",
                    "x86_64-redhat-linux",
                    "x86_64-suse-linux",
                    "x86_64-manbo-linux-gnu",
                    "x86_64-linux-gnu",
                    "x86_64-slackware-linux"
                };
            libDirs.addAll(Arrays.asList(X86_64_LibDirs));
            triples.addAll(Arrays.asList(X86_64Triples));
        }
        else if (arch == Triple.ArchType.x86)
        {

            String[] X86LibDirs = { "/lib32", "/lib" };
            String[] X86Triples = {
            "i686-linux-gnu",
                    "i686-pc-linux-gnu",
                    "i486-linux-gnu",
                    "i386-linux-gnu",
                    "i686-redhat-linux",
                    "i586-redhat-linux",
                    "i386-redhat-linux",
                    "i586-suse-linux",
                    "i486-slackware-linux"
                };

            libDirs.addAll(Arrays.asList(X86LibDirs));
            triples.addAll(Arrays.asList(X86Triples));
        }
    }

    private void scanLibDirForGCCTriple(Triple.ArchType arch,
            String libDir,
            String candidateTriple)
    {
        String[] suffixes = {"/gcc/" + candidateTriple,
        "/" + candidateTriple + "/gcc/" + candidateTriple,
        "i386-linux-gnu/gcc/" + candidateTriple};

        String[] installSufixes = {
                "/../../..",
                "/../../../..",
                "/../../../.."};
        int numSuffixes = arch != Triple.ArchType.x86 ? suffixes.length - 1:suffixes.length;
        for (int i = 0; i < numSuffixes; i++)
        {
            String suffix = suffixes[i];
            if (!Files.exists(Paths.get(libDir + suffix)))
                continue;

            String[] entries = new File(libDir + suffix).list();
            if (entries == null) continue;

            for (String versionText : entries)
            {
                GCCVersion candidateVersion = GCCVersion.parse(versionText);
                GCCVersion minVersion = new GCCVersion("4.1.1", 4, 1, 1, "");
                if (candidateVersion.compareTo(minVersion) < 0)
                    continue;
                if (candidateVersion.compareTo(version) <= 0)
                    continue;

                if (!Files.exists(Paths.get(libDir, suffix, versionText, "/crtbegin.o")))
                    continue;

                version = candidateVersion;
                gccTriple = candidateTriple;
                gccInstallPath = libDir + suffix + "/" + versionText;
                gccParentLibPath = gccInstallPath + installSufixes[i];
                isValid = true;
            }
        }
    }
}
