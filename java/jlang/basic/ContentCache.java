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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class ContentCache
{
    /**
     * The actual buffer containing the characters from the input
     * file.  This is owned by the ContentCache object.
     */
    private MemoryBuffer buffer;

    public Path fileEntry;

    public TIntArrayList sourceLineCache;

    /**
     * First FileID that was created for this ContentCache.
     * Represents the first source inclusion of the file associated with this
     * ContentCache.
     */
    public FileID firstFID;

    public MemoryBuffer getBuffer()
    {
        if (buffer == null && fileEntry != null)
        {
            buffer = MemoryBuffer.getFile(fileEntry);
        }
        return buffer;
    }

    public long getSize()
    {
        try
        {
            return fileEntry != null? Files.size(fileEntry) : buffer.length();
        }
        catch (IOException e)
        {
            return 0;
        }
    }

    public void setBuffer(MemoryBuffer buffer)
    {
        assert this.buffer == null :"MemoryBuffer already set!";
        this.buffer = buffer;
    }

    public ContentCache()
    {
        this(null);
    }

    public ContentCache(Path entry)
    {
        this.fileEntry = entry;
    }
}
