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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class HeaderSearch
{
    /**
     * #include search path information.
     * </p>
     * Requests for #include "x" search the
     * directory of the #including file first, then each directory in SearchDirs
     * consequetively. Requests for <x> search the current dir first, then each
     * directory in SearchDirs, starting at SystemDirIdx, consequetively.  If
     * NoCurDirSearch is true, then the check for the file in the current
     * directory is suppressed.
     * <pre>
     *     |quoted include| System include |
     *                   ^
     *               systemDirIdx
     * </pre>
     */
    ArrayList<Path> searchDirs;
    /**
     * The starting index into {@code searchDirs}.
     */
    int systemDirIdx;
    boolean noCurDirSearch;

    public void setSearchPaths(ArrayList<Path> searchList,
            int systemDirIdx,
            boolean noCurDirSearch)
    {
        searchDirs = searchList;
        this.systemDirIdx = systemDirIdx;
        this.noCurDirSearch = noCurDirSearch;
    }

    public ArrayList<String> getQuotedPaths()
    {
        ArrayList<String> res = new ArrayList<>();
        List<String> temps = (searchDirs.stream().map(dir->dir.toFile().getAbsolutePath()).collect(
                Collectors.toList()));
        for (int i = 0; i < systemDirIdx; i++)
            res.add(temps.get(i));
        return res;
    }

    public ArrayList<String> getSystemPaths()
    {
        ArrayList<String> res = new ArrayList<>();
        List<String> temps = (searchDirs.stream().map(dir->dir.toFile().getAbsolutePath()).collect(
                Collectors.toList()));
        for (int i = systemDirIdx; i < temps.size(); i++)
            res.add(temps.get(i));
        return res;
    }
}
