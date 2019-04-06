package backend.bitcode;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
 * All rights reserved.
 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import backend.io.ByteSequence;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class BitcodeUtil {
  /**
   * Return true if the given bytes are the magic bytes for an LLVM IR bitcode wrapper.
   * @param buffer
   * @return
   */
  public static boolean isBitcodeWrapper(ByteSequence buffer) {
    return buffer.length() >= 4 &&
        buffer.byteAt(0) == (byte)0xDE &&
        buffer.byteAt(1) == (byte)0xC0 &&
        buffer.byteAt(2) == (byte)0x17 &&
        buffer.byteAt(3) == (byte)0x0B;
  }

  /**
   * Return true if the given bytes are the magic bytes for an LLVM IR bitcode.
   * @param buffer
   * @return
   */
  public static boolean isRawBitcode (ByteSequence buffer) {
    return buffer.length() >= 4 &&
        buffer.byteAt(0) == 'B' &&
        buffer.byteAt(1) == 'C' &&
        buffer.byteAt(2) == (byte)0xc0 &&
        buffer.byteAt(3) == (byte)0xde;
  }

  public static boolean isBitcode(ByteSequence buffer) {
    return isBitcodeWrapper(buffer) || isRawBitcode(buffer);
  }

  private static final int
      KnownHeaderSize = 4*4,  // Size of header we read.
      OffsetField = 2*4,      // Offset in bytes to Offset field.
      SizeField = 3*4;         // Offset in bytes to Size field.
  public static ByteSequence skipBitcodeWrapperHeader(ByteSequence buffer) {
    if (buffer.length() < KnownHeaderSize)
      return null;

    int offset = buffer.byteAt(OffsetField) |
        (buffer.byteAt(OffsetField+1) << 8) |
        (buffer.byteAt(OffsetField+2) << 16) |
        (buffer.byteAt(OffsetField+3) << 24);
    int size = (buffer.byteAt(SizeField) |
        (buffer.byteAt(SizeField+1)>>8) |
        (buffer.byteAt(SizeField+2)>>16)|
        (buffer.byteAt(SizeField+3)>>24));

    // Verify that Offset+Size fits in the file.
    if (offset + size > buffer.length())
      return null;
    return new ByteSequence() {
      @Override
      public int length() {
        return size;
      }

      @Override
      public byte byteAt(int index) {
        return buffer.byteAt(index+offset);
      }
    };
  }
}
