package jlang.basic;
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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;

import static jlang.basic.InitHeaderSearch.IncludeDirGroup.*;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class InitHeaderSearch
{
    public enum IncludeDirGroup
    {
        /**
         * `#include ""` paths. Thing `gcc -iquote`.
         */
        Quoted,

        /**
         * < Paths for both `#include ""` and `#include &lt;&gt;`. (`-I`)
         */
        Angled,

        /**
         * Like Angled, but marks system directories.
         */
        System,

        /**
         * Like System, but searched after the system directories.
         */
        After,
    }
    private HeaderSearch headers;
    private boolean verbose;
    private String isysroot;
    private ArrayList<Path>[] includeGroup = (ArrayList<Path>[]) new ArrayList[4];

    public InitHeaderSearch(HeaderSearch headers,
            boolean verbose,
            String isysroot)
    {
        this.headers = headers;
        this.verbose = verbose;
        this.isysroot = isysroot;
        for (int i = 0; i < includeGroup.length; i++)
            includeGroup[i] = new ArrayList<>();
    }

    public void addPath(String path, IncludeDirGroup grp,
            boolean ignoreSysRoot)
    {
        assert !path.isEmpty() :"can't handle empty path here";

        // Compute the actual path, taking into consideration -isysroot.
        StringBuilder buf = new StringBuilder();
        // Handle isysroot.
        if (grp == IncludeDirGroup.System && !ignoreSysRoot)
        {
            if (isysroot.length() != 1 || isysroot.charAt(0) != '/')
                buf.append(isysroot);
        }

        buf.append(path);

        // Call the normalize method to remove the redundant entry in path.
        // e.g. converts the '/home/xlous/.././cary' to '/home/cary'.
        Path dir = Paths.get(buf.toString()).normalize();
        if (dir.toFile().exists())
        {
            includeGroup[grp.ordinal()].add(dir);
            return;
        }

        if (verbose)
            java.lang.System.err.println("ignoring nonexistent directory \""
            + buf.toString() +"\"");
    }

    public void addDefaultEnvVarPaths()
    {
        addEnvVarPaths("CPATH");
        addEnvVarPaths("C_INCLUDE_PATH");
    }

    private void addEnvVarPaths(String envName)
    {
        String at = java.lang.System.getenv(envName);
        // Terminating early if the env variable is null.
        if (at == null || at.isEmpty())
            return;
        int start = 0;
        int idx = at.indexOf(File.pathSeparator);
        while (idx >= 0)
        {
            if (idx == start)
                addPath(".", Angled, false);
            else
                addPath(at.substring(start, idx), Angled, false);
            start = idx + 1;
            idx = at.indexOf(File.pathSeparator, start);
        }
        if (start < at.length())
            addPath(at.substring(start), Angled, false);
        else
            addPath(".", Angled, false);
    }

    public void addDefaultSystemIncludePaths()
    {
        IncludeDirGroup grp = InitHeaderSearch.IncludeDirGroup.System;
        addPath("/usr/local/include", grp, false);
        addPath("/usr/include", grp, false);

        // add gnu compiler include header files.
        // Ubuntu 16.04 on X86_64 target, GCC 6.2.0
        addPath("/usr/lib/gcc/x86_64-linux-gnu/6/include", grp, false);
        addPath("/usr/lib/gcc/x86_64-linux-gnu/6/include-fixed", grp, false);
        addPath("/usr/include/x86_64-linux-gnu", grp, false);
    }

    private void removeDuplicates(ArrayList<Path> searchList)
    {
        if (searchList.isEmpty())
            return;
        HashSet<Path> seenDirs = new HashSet<>();
        for (int i = 0; i < searchList.size(); ++i)
        {
            Path cur = searchList.get(i);
            if (seenDirs.add(cur))
                continue;

            // the i-th path is duplicated which is should be removed.
            if (verbose)
            {
                java.lang.System.err.printf("ignoring duplicate directory \"%s\"\n",
                        cur.toString());
            }
            searchList.remove(i);
            --i;
        }
    }

    public void realize()
    {
        // Concatenate ANGLE+SYSTEM+AFTER chains together into SearchList.
        ArrayList<Path> searchList = new ArrayList<>();
        searchList.addAll(includeGroup[Angled.ordinal()]);
        searchList.addAll(includeGroup[IncludeDirGroup.System.ordinal()]);
        searchList.addAll(includeGroup[After.ordinal()]);

        removeDuplicates(searchList);
        removeDuplicates(includeGroup[Quoted.ordinal()]);

        // Prepend QUOTED list on the search list.
        searchList.addAll(0, includeGroup[Quoted.ordinal()]);

        headers.setSearchPaths(searchList, includeGroup[Quoted.ordinal()].size(),
                true);

        // If verbose, print the list of directories that will be used.
        if (verbose)
        {
            java.lang.System.err.println("#include \"...\" search starts here:");
            int quotedIdx = includeGroup[Quoted.ordinal()].size();
            for (int i = 0, e = searchList.size(); i < e; ++i)
            {
                if (i == quotedIdx)
                    java.lang.System.err.println("#include <...> search starts here:");
                Path cur = searchList.get(i);
                String name = cur.toFile().getName();
                java.lang.System.err.println(name);
            }
            java.lang.System.err.println("End of search list.");
        }
    }
}
