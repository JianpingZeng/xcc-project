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

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TObjectIntHashMap;
import jlang.clex.StrData;
import jlang.support.*;
import tools.Pair;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;

import static jlang.support.CharacteristicKind.C_System;
import static jlang.support.CharacteristicKind.C_User;

/**
 * <p>
 * This file handles loading and caching of source files into
 * memory.  This object owns the MemoryBuffer objects for all of the loaded
 * files and assigns unique FileID's for each unique #include chain.
 * </p>
 * <p>
 * The SourceManager can be queried for information about SourceLocation
 * objects, turning them into either spelling or instantiation locations.
 * Spelling locations represent where the bytes corresponding to a token came
 * from and instantiation locations represent where the location is in the
 * user's view.  In the case of a macro expansion, for example, the spelling
 * location indicates where the expanded token came from and the instantiation
 * location specifies where it was expanded.
 * </p>
 * @author Xlous.zeng
 * @version 0.1
 */
public class SourceManager
{
    private HashMap<Path, ContentCache> fileInfo;

    private ArrayList<ContentCache> memBufferInfo;

    private ArrayList<SLocEntry> slocEntryTable;

    private int nextOffset;

    private ArrayList<Boolean> slocEntryLoaded;

    private FileID lastFileIDLookup;

    private LineTableInfo lineTable;

    private FileID lastLineNoFileIDQuery;
    private ContentCache lastLineNoContentCache;
    private int lastLineNoFilePos;
    private int lastLineNoResult;

    /**
     * The file ID for the main source file of the translation unit.
     */
    private FileID mainFileID;
    
    // Cache results for the isBeforeInTranslationUnit method.
    private FileID lastLFIDForBeforeTUCheck;
    private FileID lastRFIDForBeforeTUCheck;
    private boolean lastResForBeforeTUCheck;

    public SourceManager()
    {
        fileInfo = new HashMap<>();
        memBufferInfo = new ArrayList<>();
        slocEntryTable = new ArrayList<>();
        nextOffset = 0;
        slocEntryLoaded = new ArrayList<>();
        lastFileIDLookup = new FileID();
        lineTable = new LineTableInfo();
        lastLineNoFileIDQuery = new FileID();
        lastLineNoContentCache = new ContentCache();
        lastLineNoFilePos = 0;
        lastLineNoResult = 0;
        mainFileID = new FileID();
        lastLFIDForBeforeTUCheck = new FileID();
        lastRFIDForBeforeTUCheck = new FileID();
        lastResForBeforeTUCheck = false;

        clearIDTables();
    }

    public void clearIDTables()
    {
        mainFileID = new FileID();
        slocEntryTable.clear();
        lastLineNoFileIDQuery = new FileID();
        lastLineNoContentCache = null;
        lastFileIDLookup = new FileID();

        if (lineTable != null)
            lineTable.clear();

        nextOffset = 0;
        createInstantiationLoc(new SourceLocation(), new SourceLocation(),
                new SourceLocation(), 1,0, 0);
    }

    public SourceLocation createInstantiationLoc(
            SourceLocation SpellingLoc,
            SourceLocation ILocStart,
            SourceLocation ILocEnd,
            int tokLength,
            int preallocationID,
            int offset)
    {
        InstantiationInfo ii = InstantiationInfo.get(ILocStart, ILocEnd, SpellingLoc);

        if (preallocationID != 0)
        {
            assert preallocationID < slocEntryLoaded.size():"Preallocated ID out of range";
            assert slocEntryTable.get(preallocationID) == null :
                    "Source location entry already loaded";
            assert offset != 0 :"Preallocated source location cannot have zero offset";

            slocEntryTable.set(preallocationID, SLocEntry.get(offset, ii));
            slocEntryLoaded.set(preallocationID, true);
            return SourceLocation.getMacroLoc(offset);
        }

        slocEntryTable.add(SLocEntry.get(nextOffset, ii));
        assert nextOffset + tokLength + 1 >nextOffset :
                " Ran out of source locations!";

        nextOffset += tokLength + 1;
        return SourceLocation.getMacroLoc(nextOffset - (tokLength + 1));
    }

    public FileID getMainFileID()
    {
        return mainFileID;
    }

    public FileID createMainFileID(Path sourceFile, SourceLocation includePos)
    {
        assert mainFileID.isInvalid() :"MainFileID already setted";
        mainFileID = createFileID(sourceFile, includePos, C_User, 0, 0);;
        return mainFileID;
    }

    public FileID createFileID(
            Path sourceFile,
            SourceLocation includePos,
            CharacteristicKind character,
            int preallocatedID,
            int offset)
    {
        ContentCache cache = getOrCreateContentCache(sourceFile);
        if (cache == null) return new FileID(); // Error openning file
        return createFileID(cache, includePos, character, preallocatedID, offset);
    }

    public FileID createFileIDForMemBuffer(
            MemoryBuffer buffer)
    {
        return createFileIDForMemBuffer(buffer, 0, 0);
    }

    public FileID createFileIDForMemBuffer(
            MemoryBuffer buffer,
            int preallocatedID,
            int offset)
    {
        return createFileID(createMemBufferContentCache(buffer),
                new SourceLocation(), C_User, preallocatedID, offset);
    }

    public FileID createMainFileIDForMemBuffer(MemoryBuffer buffer)
    {
        assert mainFileID.isInvalid();
        mainFileID = createFileIDForMemBuffer(buffer, 0, 0);
        return mainFileID;
    }

    public SLocEntry getSLocEntry(FileID fid)
    {
        assert fid.getID() < slocEntryTable.size();
        return slocEntryTable.get(fid.getID());
    }

    public MemoryBuffer getBuffer(FileID fid)
    {
        SLocEntry entry = getSLocEntry(fid);
        assert entry.isFile(): "Cann't get buffer for instantiation id";
        return entry.getFile().getContentCache().getBuffer();
    }

    public Path getFileEntryForID(FileID fid)
    {
        SLocEntry entry = getSLocEntry(fid);
        assert entry.isFile(): "Cann't get buffer for instantiation id";
        return entry.getFile().getContentCache().fileEntry;
    }

    public FileID getFileID(SourceLocation literalLoc)
    {
        int slocOffset = literalLoc.getOffset();

        if (isOffsetInFileID(lastFileIDLookup, slocOffset))
            return lastFileIDLookup;

        return getFileIDSlow(slocOffset);
    }

    public SourceLocation getLocForStartOfFile(FileID fid)
    {
        assert fid.getID() < slocEntryTable.size():"FileID out of range";
        assert getSLocEntry(fid).isFile():"FileID is not a file";
        int fileOffset = getSLocEntry(fid).getOffset();
        return SourceLocation.getFileLoc(fileOffset);
    }

    public SourceLocation getInstantiationLoc(SourceLocation loc)
    {
        if (loc.isFileID()) return loc;
        return getInstantiationLocSlow(loc);
    }

    public SourceRange getImmediateInstantiationRange(SourceLocation loc)
    {
        assert loc.isMacroID() :"Not an instantiation identLoc!";
        InstantiationInfo ii = getSLocEntry(getFileID(loc)).getInstantiation();
        return ii.getInstantiationLocRange();
    }

    public SourceRange getInstantiationRange(SourceLocation loc)
    {
        if (loc.isFileID()) return new SourceRange(loc, loc);

        SourceRange res = getImmediateInstantiationRange(loc);

        while (!res.getBegin().isFileID())
            res.setBegin(getImmediateInstantiationRange(res.getBegin()).getBegin());
        while (!res.getEnd().isFileID())
            res.setEnd(getImmediateInstantiationRange(res.getEnd()).getEnd());

        return res;
    }

    public SourceLocation getLiteralLoc(SourceLocation loc)
    {
        if(loc.isFileID()) return loc;
        return getLiteralLocSlowCase(loc);
    }

    public SourceLocation getImmediateLiteralLoc(SourceLocation loc)
    {
        if (loc.isFileID()) return loc;

        Pair<FileID, Integer> locInfo = getDecomposedLoc(loc);
        loc = getSLocEntry(locInfo.first).getInstantiation().getLiteralLoc();
        return loc.getFileLocWithOffset(locInfo.second);
    }

    public Pair<FileID, Integer> getDecomposedLoc(SourceLocation loc)
    {
        FileID fid = getFileID(loc);
        return Pair.get(fid, loc.getOffset() - getSLocEntry(fid).getOffset());
    }

    public Pair<FileID, Integer> getDecomposedInstantiationLoc(SourceLocation loc)
    {
        FileID fid = getFileID(loc);
        SLocEntry e = getSLocEntry(fid);

        int offset = loc.getOffset() - e.getOffset();
        if (loc.isFileID())
            return Pair.get(fid, offset);

        return getDecomposedInstantiationLocSlowCase(e, offset);
    }

    public Pair<FileID, Integer>  getDecomposedLiteralLoc(SourceLocation loc)
    {
        FileID fid = getFileID(loc);
        SLocEntry e = getSLocEntry(fid);

        int offset = loc.getOffset() - e.getOffset();
        if (loc.isFileID())
            return Pair.get(fid, offset);

        return getDecomposedLiteralLocSlowCase(e, offset);
    }

    public int getFileOffset(SourceLocation literalLoc)
    {
        return getDecomposedLiteralLoc(literalLoc).second;
    }

    /**
     * Compute the starting position for the given {@code identLoc} in input buffer.
     * @param loc
     * @return
     */
    public StrData getCharacterData(SourceLocation loc)
    {
        Pair<FileID, Integer> locInfo = getDecomposedLiteralLoc(loc);

        return new StrData(getSLocEntry(locInfo.first).getFile().getContentCache().
                getBuffer().getBuffer(), locInfo.second);
    }

    public int getColumnNumber(FileID fid, int filePos)
    {
        char[] buf = getBuffer(fid).getBuffer();

        int lineStart = filePos;
        while (lineStart != 0 && buf[lineStart] != '\n' && buf[lineStart]!= '\r')
            --lineStart;
        return filePos - lineStart + 1;
    }

    public int getLiteralColumnNumber(SourceLocation loc)
    {
        if (!loc.isValid()) return 0;

        Pair<FileID, Integer> locInfo = getDecomposedLiteralLoc(loc);
        return getColumnNumber(locInfo.first, locInfo.second);
    }

    public int getInstantiationColumnNumber(SourceLocation loc)
    {
        if (!loc.isValid()) return 0;

        Pair<FileID, Integer> locInfo = getDecomposedInstantiationLoc(loc);
        return getColumnNumber(locInfo.first, locInfo.second);
    }

    /**
     * Given a SourceLocation, return the spelling line number
     * for the position indicated.  This requires building and caching a table of
     * line offsets for the MemoryBuffer, so this is not cheap: use only when
     * about to emit a diagnostic.
     * @param file
     */
    public static void computeLineNumbers(ContentCache file)
    {
        MemoryBuffer buffer = file.getBuffer();
        char[] cb = buffer.getBuffer();
        TIntArrayList lineOffsets = new TIntArrayList();

        lineOffsets.add(0);

        int offs = 0;
        while (true)
        {
            int i = 0;
            while (cb[i] != '\n' && cb[i] != '\r' && cb[i] != '\0')
            {
                ++i;
            }

            offs += i;

            if (cb[i] == '\n' || cb[i] == '\r')
            {
                // If this is \n\r or \r\n, skip both characters.
                if ((cb[i+1] =='\n' || cb[i+1] == '\r')
                        && cb[i] != cb[i+1])
                {
                    ++offs;
                    ++i;
                }
                ++offs;
                ++i;
                lineOffsets.add(offs);
            }
            else
            {
                // Otherwise, this is a null.  If end of file, exit.
                if (i == cb.length) break;

                // Otherwise, skip the null.
                ++offs;
                ++i;
            }
        }

        file.sourceLineCache = new TIntArrayList(lineOffsets);
    }

    public int getLineNumber(FileID fid, int filePos)
    {
        ContentCache ccache;
        if (lastLineNoFileIDQuery == fid)
            ccache = lastLineNoContentCache;
        else
            ccache = getSLocEntry(fid).getFile().getContentCache();

        if (ccache.sourceLineCache == null)
            computeLineNumbers(ccache);

        TIntArrayList sourceLineCache = ccache.sourceLineCache;
        int queriedFilePos = filePos + 1;

        int start = 0;
        int end = sourceLineCache.size();

        if (lastLineNoFileIDQuery == fid)
        {
            if (queriedFilePos >= lastLineNoFilePos)
            {
                start = lastLineNoResult - 1;

                if (start + 5 < sourceLineCache.size())
                {
                    if (sourceLineCache.get(start + 5) > queriedFilePos)
                    {
                        end = start + 5;
                    }
                }
                else if (start + 10 < sourceLineCache.size())
                {
                    if (sourceLineCache.get(start + 10) > queriedFilePos)
                    {
                        end = start + 10;
                    }
                    else if (sourceLineCache.get(start + 20) > queriedFilePos)
                    {
                        end = start + 20;
                    }
                }
            }
            else
            {
                if (lastLineNoResult < ccache.sourceLineCache.size())
                    end = lastLineNoResult + 1;
            }
        }

        int pos = end;
        for (int i = start; i < end; ++i)
        {
            if (sourceLineCache.get(i) >= queriedFilePos)
            {
                pos = i;
                break;
            }
        }

        int lineNo = pos;

        lastLineNoFileIDQuery = fid;
        lastLineNoContentCache = ccache;
        lastLineNoFilePos = queriedFilePos;
        lastLineNoResult = lineNo;
        return lineNo;
    }

    public int getInstantiationLineNumber(SourceLocation loc)
    {
        if (!loc.isValid()) return 0;

        Pair<FileID, Integer> locInfo = getDecomposedInstantiationLoc(loc);
        return getLineNumber(locInfo.first, locInfo.second);
    }

    public int getLiteralLineNumber(SourceLocation loc)
    {
        if (!loc.isValid()) return 0;

        Pair<FileID, Integer> locInfo = getDecomposedLiteralLoc(loc);
        return getLineNumber(locInfo.first, locInfo.second);
    }

    public String getBufferName(SourceLocation loc)
    {
        if (!loc.isValid()) return "<invalid identLoc>";
        return getBuffer(getFileID(loc)).getBufferName();
    }

    public CharacteristicKind getFileCharacteristicKind(SourceLocation loc)
    {
        assert loc.isValid() : "Can't get file characteristic of invalid identLoc!";

        Pair<FileID, Integer> locInfo = getDecomposedInstantiationLoc(loc);

        FileInfo fi = getSLocEntry(locInfo.first).getFile();

        if (!fi.hasLineDirectives())
            return fi.getFileCharacteristic();

        assert lineTable != null: "Can't have linetable entries without a LineTable!";

        LineEntry entry = lineTable.findNearestLineEntry(locInfo.first.getID(), locInfo.second);;

        if (entry == null)
            return fi.getFileCharacteristic();

        return entry.fileKind;
    }

    public PresumedLoc getPresumedLoc(SourceLocation loc)
    {
        if (!loc.isValid()) return new PresumedLoc();

        Pair<FileID, Integer> locInfo = getDecomposedInstantiationLoc(loc);

        FileInfo fi = getSLocEntry(locInfo.first).getFile();
        ContentCache cache = fi.getContentCache();

        String filename = cache.fileEntry != null?
                cache.fileEntry.toFile().getName() : cache.getBuffer().getBufferName();

        int lineNo = getLineNumber(locInfo.first, locInfo.second);
        int colNo = getColumnNumber(locInfo.first, locInfo.second);
        SourceLocation includeLoc = fi.getIncludeLoc();

        if (fi.hasLineDirectives())
        {
            assert lineTable != null;

            LineEntry entry = lineTable.findNearestLineEntry(locInfo.first.getID(), locInfo.second);

            if (entry != null)
            {
                if (entry.filenameID != -1)
                {
                    filename = lineTable.getFilename(entry.filenameID);
                }

                int markerLineNo = getLineNumber(locInfo.first, entry.fileOffset);

                lineNo = entry.lineNo + (lineNo - markerLineNo - 1);

                if (entry.includeOffset != 0)
                {
                    includeLoc = getLocForStartOfFile(locInfo.first);
                    includeLoc = includeLoc.getFileLocWithOffset(entry.includeOffset);
                }
            }
        }
        return new PresumedLoc(filename, lineNo, colNo, includeLoc);
    }

    public boolean isFromSameFile(SourceLocation loc1, SourceLocation loc2)
    {
        return getFileID(loc1).equals(getFileID(loc2));
    }

    public boolean isFromMainFile(SourceLocation loc)
    {
        return getFileID(loc).equals(mainFileID);
    }

    public boolean isInSystemHeader(SourceLocation loc)
    {
        return getFileCharacteristicKind(loc) != C_User;
    }

    /**
     * Return an unique id for the specified filename.
     * @param filename
     * @return
     */
    public int getLineTableFilenameID(String filename)
    {
        if (lineTable == null)
            lineTable = new LineTableInfo();
        return lineTable.getLineTableFilenameID(filename);
    }

    public void addLineNote(SourceLocation loc, int lineNo, int filenameID)
    {
        Pair<FileID, Integer> locInfo = getDecomposedInstantiationLoc(loc);
        FileInfo fi = getSLocEntry(locInfo.first).getFile();

        fi.setLineDirectives(true);

        if (lineTable == null)
            lineTable = new LineTableInfo();
        lineTable.addLineNote(locInfo.first.getID(), locInfo.second, lineNo, filenameID);
    }

    /**
     * Add a GNU line marker to the line table.
     * @param loc
     * @param lineNo
     * @param filenameID
     * @param isFileEntry
     * @param isFileExit
     * @param isSystemHeader
     */
    public void addLineNote(SourceLocation loc, int lineNo, int filenameID,
            boolean isFileEntry, boolean isFileExit,
            boolean isSystemHeader)
    {
        if (filenameID == -1)
        {
            assert !isFileEntry && !isFileExit && !isSystemHeader
                    : "Can't set flags without setting the filename!";
            addLineNote(loc, lineNo, filenameID);
            return;
        }

        Pair<FileID, Integer> locInfo = getDecomposedInstantiationLoc(loc);
        FileInfo fi = getSLocEntry(locInfo.first).getFile();

        fi.setLineDirectives(true);

        if (lineTable == null)
            lineTable = new LineTableInfo();

        CharacteristicKind fileKind;
        if (isSystemHeader)
            fileKind = C_System;
        else
            fileKind = C_User;

        int entryExit = 0;
        if (isFileEntry)
            entryExit = 1;
        else if (isFileExit)
            entryExit = 2;

        lineTable.addLineNote(locInfo.first.getID(), locInfo.second,
                lineNo, filenameID,
                entryExit, fileKind);
    }

    public boolean hasLineTable() {return lineTable != null; }

    public LineTableInfo getLineTable()
    {
        if (lineTable == null)
            lineTable = new LineTableInfo();
        return lineTable;
    }

    /**
     * Get the source location for the given file:line:col triplet.
     *
     * If the source file is included multiple times, the source location will
     * be based upon the first inclusion.
     * @param sourcefile
     * @param line
     * @param col
     * @return
     */
    public SourceLocation getLocation(Path sourcefile, int line, int col)
    {
        assert sourcefile != null : "Null source file!";
        assert line != 0 && col != 0 : "Line and column should start from 1!";

        if (!fileInfo.containsKey(sourcefile))
            return new SourceLocation();

        ContentCache content = fileInfo.get(sourcefile);

        // If this is the first use of line information for this buffer, compute the
        /// SourceLineCache for it on demand.
        if (content.sourceLineCache == null)
            computeLineNumbers(content);

        if (line < content.sourceLineCache.size())
            return new SourceLocation();

        int filePos = content.sourceLineCache.get(line - 1);
        int end = content.getBuffer().length();
        char[] buf = content.getBuffer().getBuffer();
        int i = filePos;

        // Check that the given column is valid.
        while (i < end - 1 && i < col-1 && buf[i] != '\n' && buf[i] != '\r')
            ++i;

        if (i < col-1)
            return new SourceLocation();

        return getLocForStartOfFile(content.firstFID).
                getFileLocWithOffset(filePos +col - 1);
    }

    public boolean isBeforeTranslationUnit(SourceLocation lhs, SourceLocation rhs)
    {
        assert lhs.isValid() && rhs.isValid(): "Passed invalid source location!";
        if (rhs.equals(lhs))
            return false;

        Pair<FileID, Integer> loffs = getDecomposedLoc(lhs);
        Pair<FileID, Integer> roffs = getDecomposedLoc(rhs);

        if (loffs.first.equals(roffs.first))
            return loffs.second < roffs.second;

        // If we are comparing a source location with multiple locations in the same
        // file, we get a big win by caching the result.

        if (lastLFIDForBeforeTUCheck.equals(loffs.first) &&
                lastRFIDForBeforeTUCheck.equals(roffs.first))
            return lastResForBeforeTUCheck;

        lastLFIDForBeforeTUCheck = loffs.first;
        lastRFIDForBeforeTUCheck = roffs.first;

        TObjectIntHashMap<FileID> roffsMap = new TObjectIntHashMap<>();
        roffsMap.put(roffs.first, roffs.second);


        while (true)
        {
            SourceLocation upperLoc;
            SLocEntry entry = getSLocEntry(roffs.first);
            if (entry.isInstantiation())
                upperLoc = entry.getInstantiation().getInstantiationLocStart();
            else
                upperLoc = entry.getFile().getIncludeLoc();

            if (!upperLoc.isValid())
                break;

            roffs = getDecomposedLoc(upperLoc);

            if (loffs.first.equals(roffs.first))
                return lastResForBeforeTUCheck = loffs.second < roffs.second;

            roffsMap.put(roffs.first, roffs.second);
        }

        // We didn't find a common ancestor. Now traverse the stack of the left
        // location, checking against the stack map of the right location.

        while (true) 
        {
            SourceLocation UpperLoc;
            SLocEntry Entry = getSLocEntry(loffs.first);
            if (Entry.isInstantiation())
                UpperLoc = Entry.getInstantiation().getInstantiationLocStart();
            else
                UpperLoc = Entry.getFile().getIncludeLoc();

            if (!UpperLoc.isValid())
                break; // We reached the top.

            loffs = getDecomposedLoc(UpperLoc);

            if (roffsMap.containsKey(loffs.first))
                return lastResForBeforeTUCheck = loffs.second < roffsMap.get(loffs.first);
        }

        // No common ancestor.
        // Now we are getting into murky waters. Most probably this is because one
        // location is in the predefines buffer.

        Path lentry = getSLocEntry(loffs.first).getFile()
                .getContentCache().fileEntry;
        Path rentry = getSLocEntry(roffs.first).getFile()
                .getContentCache().fileEntry;

        // If the locations are in two memory buffers we give up, we can't answer
        // which one should be considered first.
        // FIXME: Should there be a way to "include" memory buffers in the translation
        // unit ?
        assert (lentry != null || rentry != null) : "Locations in memory buffers.";

        // Consider the memory buffer as coming before the file in the translation
        // unit.
        if (lentry == null)
            return lastResForBeforeTUCheck = true;
        else
        {
            assert rentry == null : "Locations in not #included files ?";
            return lastResForBeforeTUCheck = false;
        }
    }

    public HashMap<Path, ContentCache> getFileInfo()
    {
        return fileInfo;
    }

    public ArrayList<SLocEntry> getSlocEntryTable()
    {
        return slocEntryTable;
    }

    public int getNextOffset()
    {
        return nextOffset;
    }

    public void preallocatedSLocEntries(int numSLocEntries, int nextOffset)
    {
        this.nextOffset = nextOffset;
        slocEntryLoaded = new ArrayList<>(numSLocEntries + 1);
        slocEntryLoaded.add(true);
        slocEntryTable = new ArrayList<>(slocEntryTable.size() + numSLocEntries);
    }

    public void clearPreallocatedSLocEntries()
    {
        slocEntryLoaded.clear();
        slocEntryTable.clear();
    }

    /**
     * Return true if the specified FileID contains the
     * specified SourceLocation offset.  This is a very hot method.
     * @param fid
     * @param slocOffset
     * @return
     */
    private boolean isOffsetInFileID(FileID fid, int slocOffset)
    {
        SLocEntry entry = getSLocEntry(fid);

        if (slocOffset < entry.getOffset())
            return false;

        if (fid.getID() + 1 == slocEntryTable.size())
            return true;

        return slocOffset < getSLocEntry(new FileID(fid.getID() + 1)).getOffset();
    }

    private FileID createFileID(
            ContentCache cache,
            SourceLocation includePos,
            CharacteristicKind fileCharacter,
            int preallocatedID,
            int offset)
    {
        if (preallocatedID != 0)
        {
            assert preallocatedID < slocEntryLoaded.size();
            assert slocEntryLoaded.get(preallocatedID) == null;
            assert offset != 0;
            slocEntryTable.set(preallocatedID, SLocEntry.get(offset,
                    FileInfo.get(includePos, cache, fileCharacter)));
            slocEntryLoaded.set(preallocatedID, true);
            FileID fid = new FileID(preallocatedID);
            if (cache.firstFID.isInvalid())
                cache.firstFID = fid;
            return lastFileIDLookup = fid;
        }

        slocEntryTable.add(SLocEntry.get(nextOffset, FileInfo.get(includePos,
                cache, fileCharacter)));
        long filesize = cache.getSize();
        assert nextOffset + filesize + 1 > nextOffset;
        nextOffset += filesize + 1;

        FileID fid = new FileID(slocEntryTable.size() - 1);
        if (cache.firstFID.isInvalid())
            cache.firstFID = fid;
        return lastFileIDLookup = fid;
    }

    private ContentCache getOrCreateContentCache(Path file)
    {
        assert file!= null:"Didn't specify a file entry to use?";

        if (fileInfo.containsKey(file))
            return fileInfo.get(file);

        ContentCache entry = new ContentCache(file);
        fileInfo.put(file, entry);
        return entry;
    }

    private ContentCache createMemBufferContentCache(MemoryBuffer buffer)
    {
        ContentCache file = new ContentCache();
        memBufferInfo.add(file);
        file.setBuffer(buffer);
        return file;
    }

    /**
     * Return the FileID for a SourceLocation.  This is a very hot
     * method that is used for all SourceManager queries that start with a
     * SourceLocation object.  It is responsible for finding the entry in
     * SLocEntryTable which contains the specified location.
     * @param slocOffset
     * @return
     */
    private FileID getFileIDSlow(int slocOffset)
    {
        assert slocOffset != 0 : "Invalid FileID";

        int i = 0;
        if (slocEntryTable.get(lastFileIDLookup.getID()).getOffset() < slocOffset)
        {
            i = slocEntryTable.size();
        }
        else
        {
            i = lastFileIDLookup.getID();
        }

        int numProbes = 0;
        while (true)
        {
            --i;
            if (slocEntryTable.get(i).getOffset() <= slocOffset)
            {
                FileID res = new FileID(i);

                if (!slocEntryTable.get(i).isInstantiation())
                    lastFileIDLookup = res;
                return res;
            }
            if (++numProbes == 8)
                break;
        }

        int greaterIndex = i;

        int lessIndex = 0;
        numProbes = 0;

        // Do binary search on SLocEntryTable, since it is sorted in the
        // increasing the offset of each SLocEntry.
        while (true)
        {
            int middleIndex = (greaterIndex - lessIndex) / 2;
            int middleOffset = getSLocEntry(new FileID(middleIndex)).getOffset();

            ++numProbes;

            // If the slocOffset is before middleOffset, searching in left hafl
            if (slocOffset <= middleOffset)
            {
                greaterIndex = middleIndex;
                continue;
            }

            if (isOffsetInFileID(new FileID(middleIndex), slocOffset))
            {
                FileID res = new FileID(middleIndex);

                if (!slocEntryTable.get(i).isInstantiation())
                    lastFileIDLookup = res;

                return res;
            }

            lessIndex = middleIndex;
        }
    }

    private SourceLocation getInstantiationLocSlow(SourceLocation loc)
    {
        do
        {
            Pair<FileID, Integer> locInfo = getDecomposedLoc(loc);
            loc = getSLocEntry(locInfo.first).getInstantiation().getInstantiationLocStart();
            loc = loc.getFileLocWithOffset(locInfo.second);
        }while (!loc.isFileID());

        return loc;
    }

    private SourceLocation getLiteralLocSlowCase(SourceLocation loc)
    {
        do
        {
            Pair<FileID, Integer> locInfo = getDecomposedLoc(loc);
            loc = getSLocEntry(locInfo.first).getInstantiation().getLiteralLoc();
            loc = loc.getFileLocWithOffset(locInfo.second);
        }while (!loc.isFileID());

        return loc;
    }

    private Pair<FileID, Integer> getDecomposedInstantiationLocSlowCase(SLocEntry entry, int offset)
    {
        FileID fid;
        SourceLocation loc;
        do
        {
            loc = entry.getInstantiation().getInstantiationLocStart();
            fid = getFileID(loc);
            entry = getSLocEntry(fid);
            offset += loc.getOffset() - entry.getOffset();
        }while (!loc.isFileID());

        return Pair.get(fid, offset);
    }

    private Pair<FileID, Integer> getDecomposedLiteralLocSlowCase(SLocEntry entry, int offset)
    {
        FileID fid;
        SourceLocation loc;
        do
        {
            loc = entry.getInstantiation().getLiteralLoc();
            fid = getFileID(loc);
            entry = getSLocEntry(fid);
            offset += loc.getOffset() - entry.getOffset();
        }while (!loc.isFileID());

        return Pair.get(fid, offset);
    }

    public char[] getBufferData(FileID fileID)
    {
        return getBuffer(fileID).getBuffer();
    }
}
