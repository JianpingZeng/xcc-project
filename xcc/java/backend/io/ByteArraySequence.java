package backend.io;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
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

import java.util.Arrays;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class ByteArraySequence implements ByteSequence {
  private final byte[] buffer;
  private final int start;
  private final int length;

  /** Cache the hash code for the byte array sequences. */
  private int hash; // Default to 0

  ByteArraySequence(byte[] buffer, int start, int length) {
    assert buffer.length >= start + length;
    assert start >= 0;
    assert length >= 0;
    this.buffer = buffer;
    this.start = start;
    this.length = length;
  }

  public int length() {
    return length;
  }

  public byte byteAt(int index) {
    int resolvedIndex = start + index;
    if (resolvedIndex >= start + length) {
      throw new IndexOutOfBoundsException(String.valueOf(index));
    }
    return buffer[resolvedIndex];
  }

  public byte[] toByteArray() {
    return Arrays.copyOfRange(buffer, start, start + length);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (obj instanceof ByteArraySequence) {
      ByteArraySequence other = ((ByteArraySequence) obj);
      if (buffer == other.buffer) {
        return start == other.start && length == other.length;
      }
      if (length != other.length) {
        return false;
      }
      int thisHash = this.hash;
      int otherHash = other.hash;
      if (thisHash != 0 && otherHash != 0 && thisHash != otherHash) {
        // hash was already computed and hash is not equal
        return false;
      }
      int otherStart = other.start;
      for (int i = 0; i < length; i++) {
        if (buffer[start + i] != other.buffer[otherStart + i]) {
          return false;
        }
      }
      return true;
    } else if (obj instanceof ByteSequence) {
      ByteSequence other = ((ByteSequence) obj);
      if (length != other.length()) {
        return false;
      }
      for (int i = 0; i < length; i++) {
        if (buffer[start + i] != other.byteAt(i)) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h = hash;
    if (h == 0 && length > 0) {
      int end = start + length;
      h = 1;
      int i = start;
      for (; i + 3 < end; i += 4) {
        int h0 = buffer[i + 0] & 0xff << 0;
        int h1 = buffer[i + 1] & 0xff << 8;
        int h2 = buffer[i + 2] & 0xff << 16;
        int h3 = buffer[i + 3] & 0xff << 24;
        h = 31 * h + (h0 | h1 | h2 | h3);
      }
      for (; i < end; i++) {
        h = 31 * h + buffer[i];
      }
      hash = h;
    }
    return h;
  }

  public ByteSequence subSequence(int startIndex, int endIndex) {
    int l = endIndex - startIndex;
    if (l < 0) {
      throw new IndexOutOfBoundsException(String.valueOf(l));
    }
    final int realStartIndex = start + startIndex;
    if (realStartIndex < 0) {
      throw new IndexOutOfBoundsException(String.valueOf(startIndex));
    }
    if (endIndex > length()) {
      throw new IndexOutOfBoundsException(String.valueOf(realStartIndex + l));
    }
    return new ByteArraySequence(buffer, realStartIndex, l);
  }
}
