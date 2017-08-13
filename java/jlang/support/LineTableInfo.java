package jlang.support;
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

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.Pair;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class LineTableInfo
{
    /// filenameIDs - This map is used to assign unique IDs to filenames in
    /// #line directives.  This allows us to unique the filenames that
    /// frequently reoccur and reference them with indices.  filenameIDs holds
    /// the mapping from string -> ID, and filenamesByID holds the mapping of ID
    /// to string.
    TObjectIntHashMap<String> filenameIDs;
    ArrayList<Pair<String, Integer>> filenamesByID;

    /// lineEntries - This is a map from FileIDs to a list of line entries (sorted
    /// by the offset they occur in the file.
    TIntObjectHashMap<ArrayList<LineEntry>> lineEntries;

    public LineTableInfo()
    {
        filenameIDs = new TObjectIntHashMap<>();
        filenamesByID = new ArrayList<>();
        lineEntries = new TIntObjectHashMap<>();
    }

    public void clear()
    {
        filenameIDs.clear();
        filenamesByID.clear();
        lineEntries.clear();
    }

    public int getLineTableFilenameID(String name)
    {
        int value = filenamesByID.size();
        if (filenameIDs.containsKey(name))
            filenameIDs.get(name);

        filenameIDs.put(name, value);
        filenamesByID.add(Pair.get(name, value));
        return filenamesByID.size() - 1;
    }

    public String getFilename(int ID)
    {
        assert ID < filenamesByID.size() : "Invalid filenameID";
        return filenamesByID.get(ID).first;
    }

    public int getNumFilenames()
    {
        return filenamesByID.size();
    }

    public void addLineNote(int fid, int offset, int lineNo, int filenameID)
    {
        ArrayList<LineEntry> entries = lineEntries.get(fid);

        assert entries.isEmpty() || entries.get(entries.size() - 1).fileOffset
                < offset : "Adding line entries out of order!";

        CharacteristicKind kind = CharacteristicKind.C_User;

        int includeOffset = 0;
        if (!entries.isEmpty())
        {
            if (filenameID == -1)
                filenameID = entries.get(entries.size() - 1).filenameID;

            kind = entries.get(entries.size() - 1).fileKind;
            includeOffset = entries.get(entries.size() - 1).includeOffset;
        }

        entries.add(LineEntry.get(offset, lineNo, filenameID, kind, includeOffset));
    }

    public void addLineNote(int fid, int offset, int lineNo, int filenameID,
            int entryExit, CharacteristicKind fileKind)
    {
        assert filenameID != -1 : "Unspecified filename should use other accessor";

        ArrayList<LineEntry> entries = lineEntries.get(fid);

        assert entries.isEmpty() || entries.get(entries.size() - 1).fileOffset
                < offset : "Adding line entries out of order!";

        int includeOffset = 0;
        if (entryExit == 0)
        {
            includeOffset = entries.isEmpty() ? 0 : entries.get(entries.size() - 1).includeOffset;
        }
        else if (entryExit == 1)
        {
            includeOffset = offset - 1;
        }
        else if (entryExit == 2)
        {
            assert !entries.isEmpty() && entries.get(entries.size() - 1).includeOffset != 0
                    : "PPDirectives should have caught case when popping empty include stack";

            includeOffset = 0;
            LineEntry prevEntry = findNearestLineEntry(fid, entries.get(entries.size() - 1).includeOffset);
            if (prevEntry != null)
                includeOffset = prevEntry.includeOffset;
        }
        entries.add(LineEntry.get(offset, lineNo, filenameID, fileKind, includeOffset));
    }

    /// findNearestLineEntry - Find the line entry nearest to fid that is before
    /// it.  If there is no line entry before offset in fid, return null.
    public LineEntry findNearestLineEntry(int fid, int offset)
    {
        ArrayList<LineEntry> entries = lineEntries.get(fid);
        assert !entries.isEmpty() : "No #line entries for this FID after all!";

        if (entries.get(entries.size() - 1).fileOffset <= offset)
            return entries.get(entries.size() - 1);

        // Do a binary search to find the maximal element that is still before Offset.
        entries.sort(Comparator.comparingInt(o -> o.fileOffset));

        int low = 0, high = entries.size();
        int mid;
        while (high - low > 0)
        {
            mid = (low + high)/2;
            if (offset >= entries.get(mid).fileOffset)
            {
                low = mid + 1;
            }
            else
                high = mid;
        }

        return entries.get(low - 1);
    }

    // Low-level access

    public TIntObjectHashMap<ArrayList<LineEntry>> getLineEntries()
    {
        return lineEntries;
    }

    /// \brief Add a new line entry that has already been encoded into
    /// the internal representation of the line table.
    public void addEntry(int fid, ArrayList<LineEntry> entries)
    {
        lineEntries.put(fid, entries);
    }
}
