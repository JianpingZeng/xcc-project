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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class MemoryBuffer implements Cloneable
{
    private CharBuffer buffer;
    private int bufferStart;

    public MemoryBuffer(CharBuffer buffer)
    {
        this.buffer = buffer;
    }

    public CharBuffer getBuffer()
    {
        return buffer;
    }

    public int length()
    {
        return buffer.length();
    }

    public static MemoryBuffer getFile(Path path)
    {
        long sz = 0;
        try
        {
            sz = Files.size(path);
        }
        catch (IOException e)
        {
            System.err.println("Obtain the size of '"+ path.toString() + "' failed");
            System.exit(1);
        }
        if (sz >= 4 * 1024)
        {
            try (FileChannel channel = FileChannel.open(path))
            {
                ByteBuffer cb = channel.map(FileChannel.MapMode.READ_ONLY, 0, sz);
                return new MemoryBuffer(cb.asCharBuffer());
            }
            catch (IOException e) {}
        }

        try (BufferedReader reader = Files.newBufferedReader(path))
        {
            CharBuffer cb = CharBuffer.allocate((int)sz);
            int res = reader.read(cb);
            return new MemoryBuffer(cb);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public static MemoryBuffer getFile(String filename)
    {
        Path path = Paths.get(filename);
        if (!Files.exists(path))
        {
            System.err.println("File '"+ filename + "' is not exists");
            System.exit(1);
        }

        return getFile(path);
    }

    public static MemoryBuffer getSTDIN()
    {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in)))
        {
            int sz = System.in.available();
            char[] buf = null;
            if (sz <= 0)
            {
                return null;
            }

            buf = new char[sz];
            int offset = 0;
            do
            {
                sz = reader.read(buf, offset, sz);
                if (sz < 0)
                    break;

                offset += sz;

                char[] newArray = new char[buf.length * 2];
                System.arraycopy(buf, 0, newArray, 0, buf.length);
                buf = newArray;
            }while (true);

            CharBuffer cb = CharBuffer.wrap(buf, 0, offset);
            return new MemoryBuffer(cb);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public String getBufferName()
    {
        return "mapped buffer";
    }

    /**
     * Allocate a new MemoryBuffer of the specified size that
     * is completely initialized to zeros.  Note that the caller should
     * initialize the memory allocated by this method.  The memory is owned by
     * the MemoryBuffer object.
     * @param size
     * @param bufferName
     * @return
     */
    public static MemoryBuffer getNewMemBuffer(int size, String bufferName)
    {
        char[] buf = new char[size];
        return new MemoryBufferMem(buf, bufferName);
    }

    public int getBufferStart()
    {
        return bufferStart;
    }

    public void advance()
    {
        ++bufferStart;
    }

    /**
     * Obtains the current character indexed by {@linkplain #bufferStart}
     * from CharBuffer.
     * @return
     */
    public char getCurChar()
    {
        return buffer.charAt(bufferStart);
    }

    public char getCharAt(int i)
    {
        assert i>= getBufferStart() && i < buffer.length();
        return buffer.charAt(i);
    }

    public String getSubString(int lineStart, int lineEnd)
    {
        assert bufferStart <= lineStart && lineStart < lineEnd
                && lineEnd < buffer.length();
        StringBuilder sb = new StringBuilder();
        for (int i = lineStart; i < lineEnd; i++)
            sb.append(buffer.charAt(i));

        return sb.toString();
    }

    /**
     * Checks if the other MemoryBuffer is within the current
     * MemoryBuffer.
     * @param other
     * @return
     */
    public boolean contains(MemoryBuffer other)
    {
        return buffer.equals(other.getBuffer())
                && other.getBufferStart() >= bufferStart
                && other.getBufferStart() < buffer.length();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;

        if (getClass() != obj.getClass())
            return false;
        MemoryBuffer memBuf = (MemoryBuffer)obj;
        return bufferStart == memBuf.bufferStart && buffer.equals(memBuf.buffer);
    }

    @Override
    public MemoryBuffer clone()
    {
        try
        {
            return (MemoryBuffer)super.clone();
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public void setBufferStart(int bufferStart)
    {
        this.bufferStart = bufferStart;
    }

    static class MemoryBufferMem extends MemoryBuffer
    {
        private String fileID;

        public MemoryBufferMem(char[] buffer, String fid)
        {
            super(CharBuffer.wrap(buffer));
            fileID = fid;
        }

        @Override
        public String getBufferName()
        {
            return fileID;
        }
    }

    public static MemoryBuffer getMemBuffer(String buffer, String name)
    {
        return new MemoryBufferMem(buffer.toCharArray(), name);
    }
}
