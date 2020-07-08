package tools;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 * This software is subjected to the protection of BSD 3.0 Licence.
 * For more details, please refers to the LICENSE file.
 */

import java.io.*;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class MemoryBuffer implements Cloneable {
    private final byte[] buffer;
    private int bufferStart;
    private String filename;
    private boolean isRegular;

    private MemoryBuffer(byte[] buffer) {
        this.buffer = new byte[buffer.length];
        System.arraycopy(buffer, 0, this.buffer, 0, buffer.length);
    }

    public String getFilename() { return filename; }

    public void setFilename(String filename) { this.filename = filename; }

    public byte[] getCharBuffer() { return buffer; }

    public int length() { return buffer.length; }

    /**
     * Determines whether this memory buffer is read from regular file (either stdin nor memory).
     *
     * @return
     */
    public boolean isRegular() {
        return isRegular;
    }

    public void setRegular(boolean regular) {
        isRegular = regular;
    }

    public int getBufferStart() {
        return bufferStart;
    }

    public void advance() {
        ++bufferStart;
    }

    /**
     * Obtains the current character indexed by {@linkplain #bufferStart}
     * from CharBuffer.
     *
     * @return
     */
    public byte getCurChar() {
        return buffer[bufferStart];
    }

    public byte getCharAt(int i) {
        int len = length();
        if (i >= getBufferStart() && i < len)
            return buffer[i];
        return 0;
    }

    public String getSubString(int lineStart, int lineEnd) {
        Util.assertion(0 <= lineStart && lineStart <= lineEnd && lineEnd < buffer.length);
        if (lineEnd == lineStart) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = lineStart; i < lineEnd; i++)
            sb.append((char)buffer[i]);
        return sb.toString();
    }

    /**
     * Checks if the other MemoryBuffer is within the current
     * MemoryBuffer.
     *
     * @param other
     * @return
     */
    public boolean contains(MemoryBuffer other) {
        boolean b1 = Arrays.equals(buffer, other.getCharBuffer()),
                b2 = other.getBufferStart() >= bufferStart,
                b3 = other.getBufferStart() <= buffer.length;   // must be less and equal.
        return b1 && b2 && b3;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;

        if (getClass() != obj.getClass())
            return false;
        MemoryBuffer memBuf = (MemoryBuffer) obj;
        return bufferStart == memBuf.bufferStart && Arrays
                .equals(buffer, memBuf.buffer);
    }

    @Override
    public MemoryBuffer clone() {
        try {
            return (MemoryBuffer) super.clone();
        } catch (Exception e) {
            return null;
        }
    }

    public void setBufferStart(int bufferStart) {
        this.bufferStart = bufferStart;
    }

    private static MemoryBuffer read(String filename) {
        int sz = 1024;
        try (BufferedInputStream reader = new BufferedInputStream(filename.equals("-") ?
                System.in : new FileInputStream(filename))) {
            byte[] buf = new byte[sz];
            int offset = 0;
            do {
                sz = reader.read(buf, offset, sz);
                if (sz < 0)
                    break;

                offset += sz;

                byte[] newArray = new byte[buf.length + sz];
                System.arraycopy(buf, 0, newArray, 0, buf.length);
                buf = newArray;
            } while (true);

            // Allocate a redundant one space to reside the '\0' which
            // indicates EOF.
            byte[] newBuf = new byte[offset];
            System.arraycopy(buf, 0, newBuf, 0, offset);
            MemoryBuffer buffer = new MemoryBuffer(newBuf);
            buffer.setRegular(!filename.equals("-"));
            buffer.setFilename(filename);
            return buffer;
        } catch (Exception e) {
            throw new CompilerException(String.format("error when reading input file '%s'\n", filename));
        }
    }

    public static MemoryBuffer getFile(String filename) {
        Path path = Paths.get(filename);
        if (!Files.exists(path)) {
            System.err.println("File '" + filename + "' is not exists");
            System.exit(1);
        }
        return read(filename);
    }

    public static MemoryBuffer getSTDIN() { return read("-"); }

    public static MemoryBuffer getFileOrSTDIN(String filename) {
        if (!filename.equals("-") && !filename.isEmpty())
            return getFile(filename);
        MemoryBuffer m = getSTDIN();
        if (m != null) return m;

        // If stdin was empty, M is null.  Cons up an empty memory buffer now.
        return MemoryBuffer.getMemBuffer("", "<stdin>");
    }

    public String getBufferIdentifier() {
        return getFilename();
    }

    static class MemoryBufferMem extends MemoryBuffer {
        public MemoryBufferMem(byte[] buffer, String fid) {
            super(buffer);
            setFilename(fid);
        }

        /**
         * Determines whether this memory buffer is read from regular file (either stdin nor memory).
         *
         * @return
         */
        public boolean isRegular() { return false; }
    }

    public static MemoryBuffer getMemBuffer(String buffer, String name) {
        byte[] temp = buffer.getBytes(StandardCharsets.US_ASCII);
        return new MemoryBuffer.MemoryBufferMem(temp, name);
    }

    public static MemoryBuffer getMemBuffer(byte[] buffer, String name) {
        return new MemoryBuffer.MemoryBufferMem(buffer, name);
    }
}
