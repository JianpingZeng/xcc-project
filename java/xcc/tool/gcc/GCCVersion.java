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

import tools.Util;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public class GCCVersion implements Comparable<GCCVersion>
{
    public String text;
    public int major;
    public int minor;
    public int patch;
    public String patchSuffix;

    public GCCVersion(String text, int major, int minor, int patch, String suffix)
    {
        this.text = text;
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.patchSuffix = suffix;
    }

    public static GCCVersion parse(String versionText)
    {
        String[] parts = versionText.split("\\.");
        GCCVersion badVersion = new GCCVersion(versionText, -1, -1, -1, "");
        GCCVersion goodVersion = new GCCVersion(versionText, -1, -1, -1, "");
        if (parts.length <= 0) return badVersion;

        try
        {
            if ((goodVersion.major = Integer.parseInt(parts[0])) < 0)
                return badVersion;

            if (parts.length > 1 && (goodVersion.minor = Integer.parseInt(parts[1])) < 0)
                return badVersion;
            //   4.4
            //   4.4.0
            //   4.4.x
            //   4.4.2-rc4
            //   4.4.x-patched
            String patchText = goodVersion.patchSuffix = parts.length > 2 ?parts[2]: "";
            if (!patchText.isEmpty())
            {
                int endNumber = Util.findFirstNonOf(patchText, "12345677890");
                if ((goodVersion.patch = Integer.parseInt(patchText.substring(0, endNumber))) < 0)
                    return badVersion;
                if (endNumber < patchText.length())
                    goodVersion.patchSuffix = patchText.substring(endNumber);
            }
            return goodVersion;
        }
        catch (NumberFormatException e)
        {
            return badVersion;
        }
    }


    @Override
    public int compareTo(GCCVersion rhs)
    {
        if (major < rhs.major) return -1;
        if (major > rhs.major) return 1;
        if (minor < rhs.minor) return -1;
        if (minor > rhs.minor) return 1;

        if (rhs.patch == -1) return -1;
        if (patch == -1) return 1;
        if (patch > rhs.patch) return 1;

        return rhs.patchSuffix.isEmpty() ? 1 : -1;
    }
}
