package jlang.basic;
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

import jlang.clex.ExternalIdentifierLookup;
import jlang.clex.IdentifierInfo;
import jlang.support.CharacteristicKind;
import tools.OutRef;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class HeaderSearch
{
    /**
     * The preprocessor keeps track of this information for each
     * file that is #included.
     */
    public static class HeaderFileInfo
    {
        /**
         * True if this is a #pragma once file.
         */
        boolean isPragmaOnce;
        /**
         * Keep track of whether this is a system header, and if so,
         * whether it is C++ clean or not.  This can be set by the include paths or
         * by #pragma gcc system_header.  This is an instance of
         * {@linkplain CharacteristicKind}
         */
        CharacteristicKind dirInfo;
        /**
         * This is the number of times the file has been included
         * already.
         */
        short numIncludes;
        /**
         * If this file has a #ifndef XXX (or equivalent) guard
         * that protects the entire contents of the file, this is the identifier
         * for the macro that controls whether or not it has any effect.
         */
        IdentifierInfo controllingMacro;

        /**
         * This ID number will be non-zero when there is a controlling
         * macro whose IdentifierInfo may not yet have been loaded from
         * external storage.
         */
        int controllingMacroID;

        public HeaderFileInfo()
        {
            isPragmaOnce = false;
            dirInfo = CharacteristicKind.C_User;
            numIncludes = 0;
            controllingMacro = null;
            controllingMacroID = 0;
        }

        public IdentifierInfo getControllingMacro(ExternalIdentifierLookup external)
        {
            if (controllingMacro != null)
                return controllingMacro;

            if (controllingMacroID == 0 || external == null)
                return null;

            controllingMacro = external.getIdentifier(controllingMacroID);
            return controllingMacro;
        }
    }

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

    /**
     * This contains all of the preprocessor-specific data about files
     * that are included.
     */
    HashMap<Path, HeaderFileInfo> fileInfo;

    /// Entity used to resolve the identifier IDs of controlling
    /// macros into IdentifierInfo pointers, as needed.
    ExternalIdentifierLookup externalLookup;

    // Various statistics we track for performance analysis.
    int numIncluded;
    int numMultiIncludeFileOptzn;

    public HeaderSearch()
    {
        searchDirs = new ArrayList<>();
        fileInfo = new HashMap<>();
    }

    public void setSearchPaths(ArrayList<Path> searchList,
            int systemDirIdx,
            boolean noCurDirSearch)
    {
        searchDirs = searchList;
        this.systemDirIdx = systemDirIdx;
        this.noCurDirSearch = noCurDirSearch;
    }

    public void clearFileInfo()
    {
        fileInfo.clear();
    }

    public void setExternalLookup(ExternalIdentifierLookup externalLookup)
    {
        this.externalLookup = externalLookup;
    }

    public Path lookupFile(
            String filename,
            boolean isAngled,
            Path fromDir,
            OutRef<Path> curDir,
            Path curFileEntry)
    {
        // If 'Filename' is absolute, check to see if it exists and no searching.
        if (Paths.get(filename).isAbsolute())
        {
            curDir.set(null);

            // If this was an #include_next "/absolute/file", fail.
            if (fromDir != null) return null;

            // Otherwise, just return the file.
            return Paths.get(filename);
        }

        // Check if this header file is relative to the current file.
        // like #include "X.h"      // X.h file is relative to the current
        // including file.
        if (curFileEntry != null && !isAngled && !noCurDirSearch)
        {
            String temp = curFileEntry.normalize().getParent().toString();
            temp += File.separator;
            temp += filename;

            Path fe = Paths.get(temp);
            if (Files.exists(fe))
            {

                CharacteristicKind kind = getFileInfo(curFileEntry).dirInfo;
                getFileInfo(fe).dirInfo = kind;
                return fe;
            }
        }

        curDir.set(null);

        // If this is a system #include, ignore the user #include locs.
        int i = isAngled ? systemDirIdx : 0;
        if (fromDir != null)
            while (searchDirs.get(i) != fromDir) ++i;

        // Check each directory in sequence to see if it contains this file.
        for (; i < searchDirs.size(); ++i)
        {
            Path fe = searchDirs.get(i);
            Path tempDir = Paths.get(fe.toString() + File.separator + filename);
            if (!Files.exists(tempDir))
                continue;

            curDir.set(fe);

            // This file is a system header or C++ unfriendly if the dir is.
            getFileInfo(fe).dirInfo = getFileInfo(tempDir).dirInfo;
            return tempDir;
        }

        // Otherwise, didn't find it, just return null instead.
        return null;
    }

    /**
     * Mark the specified file as a target of of a
     * #include, #include_next. Return false if #including
     * the file will have no effect or true if we should include it.
     * @param file
     * @param isPragmaOnce
     * @return
     */
    public boolean shouldEnterIncludeFile(Path file, boolean isPragmaOnce)
    {
        ++numIncluded;

        HeaderFileInfo fileInfo = getFileInfo(file);

        if (isPragmaOnce)
        {
            fileInfo.isPragmaOnce = true;

            if (fileInfo.numIncludes != 0)
                return false;
        }
        else
        {
            if (fileInfo.isPragmaOnce)
                return false;
        }

        IdentifierInfo controllingMacro = fileInfo.getControllingMacro(externalLookup);
        if (controllingMacro != null)
        {
            if (controllingMacro.isHasMacroDefinition())
            {
                ++numMultiIncludeFileOptzn;
                return false;
            }
        }

        ++fileInfo.numIncludes;
        return true;
    }

    public CharacteristicKind getFileDirFlavor(Path file)
    {
        return getFileInfo(file).dirInfo;
    }

    /**
     * Mark the specified file as a "once only" file, e.g.
     * due to #pragma once.
     * @param file
     */
    public void markFileIncludeOnce(Path file)
    {
        getFileInfo(file).isPragmaOnce = true;
    }

    public void markFileSystemHeader(Path file)
    {
        getFileInfo(file).dirInfo = CharacteristicKind.C_System;
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

    public void incrementIncludeCount(Path entry)
    {
        ++getFileInfo(entry).numIncludes;
    }

    private HeaderFileInfo getFileInfo(Path entry)
    {
        if (fileInfo.containsKey(entry))
            return fileInfo.get(entry);
        HeaderFileInfo info = new HeaderFileInfo();
        fileInfo.put(entry, info);
        return info;
    }

    public void setFileControllingMacro(Path fileEntry, IdentifierInfo controllingMacro)
    {
        getFileInfo(fileEntry).controllingMacro = controllingMacro;
    }

    public HashMap<Path, HeaderFileInfo> getHeaderFileInfos()
    {
        return fileInfo;
    }
}
