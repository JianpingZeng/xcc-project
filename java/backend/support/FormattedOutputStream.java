/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2017, Xlous zeng.
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

package backend.support;

import tools.Util;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * This class is served as output stream in custom format. the purpose of creating
 * this class rather than Java Standard Output Stream is to add extra format control
 * on output content which is absence in stdout, like appending padding whitespace
 * on the end of line for beauty.
 */
public final class FormattedOutputStream implements AutoCloseable
{
    private OutputStream os;
    private byte[] buffer;
    private int curPos;

    public FormattedOutputStream(OutputStream out)
    {
        os = out;
        // allocate a buffer with size of 1KB.
        // it would be resize as appropriate.
        buffer = new byte[1024];
        curPos = 0;
    }

    public void print(String str)
    {
        print(str.getBytes(StandardCharsets.UTF_8));
    }

    public void print(char ch)
    {
        print(String.valueOf(ch));
    }

    public void print(int ch)
    {
        print(String.valueOf(ch));
    }

    public void print(short ch)
    {
        print(String.valueOf(ch));
    }

    public void print(long ch)
    {
        print(String.valueOf(ch));
    }

    public void print(float ch)
    {
        print(String.valueOf(ch));
    }

    public void print(double ch)
    {
        print(String.valueOf(ch));
    }

    public void print(byte[] chs)
    {
        if (curPos + chs.length >= buffer.length)
        {
            int len = curPos + chs.length;
            len = len > buffer.length << 1 ? len + buffer.length : buffer.length << 1;
            byte[] temp = new byte[len];
            System.arraycopy(buffer, 0, temp, 0, curPos);
            buffer = temp;
        }
        System.arraycopy(chs, 0, buffer, curPos, chs.length);
        curPos += chs.length;
        try
        {
            os.write(chs);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void print(boolean ch)
    {
        print(ch ? 1: 0);
    }

    public void printf(String format, Object... args)
    {
        if (format == null || format.isEmpty())
            return;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos))
        {
            ps.printf(format, args);
            print(baos.toString());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void flush() throws IOException
    {
        os.write(buffer, 0, curPos);
    }

    public void close() throws IOException
    {
        flush();
        os.close();
    }

    public void println()
    {
        printf("%n");
    }

    public void println(int val)
    {
        printf("%d%n", val);
    }

    public void println(short val)
    {
        printf("%d%n", val);
    }

    public void println(char val)
    {
        printf("%c%n", val);
    }

    public void println(long val)
    {
        printf("%d%n", val);
    }

    public void println(byte val)
    {
        printf("%d%n", val);
    }

    public void println(float val)
    {
        printf("%f%n", val);
    }

    public void println(double val)
    {
        printf("%f%n", val);
    }

    public void println(String string)
    {
        printf("%s%n", string);
    }

    /**
     * Figure out what columns where currently output stream position.
     * @return
     */
    private int computeColumns()
    {
        int start = curPos - 1;
        while (start >= 0)
        {
            if (buffer[start] == '\n')
                return curPos - start-1;
            --start;
        }
        return curPos;
    }
    /**
     * Align the output to some column number.
     * @param newCols
     */
    public void padToColumn(int newCols)
    {
        int curCol = computeColumns();
        int num = newCols - curCol;
        if (num <= 1) num = 1;
        print(Util.fixedLengthString(num, ' '));
    }
}
