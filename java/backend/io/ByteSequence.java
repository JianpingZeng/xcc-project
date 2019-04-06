package backend.io;
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

import cfe.support.MemoryBuffer;

import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public interface ByteSequence {
  /**
   * Returns the length of this byte sequence.
   *
   * @return the number of <code>byte</code>s in this sequence
   * @since 0.4
   */
  int length();

  /**
   * Returns the <code>byte</code> value at the specified index. An index ranges from zero to
   * <tt>length() - 1</tt>. The first <code>char</code> value of the sequence is at index zero,
   * the next at index one, and so on, as for array indexing.
   *
   * @param index the index of the <code>byte</code> value to be returned
   * @return the specified <code>byte</code> value
   * @throws IndexOutOfBoundsException if the <tt>index</tt> argument is negative or not less than
   *             <tt>length()</tt>
   * @since 0.4
   */
  byte byteAt(int index);

  /**
   * Returns a <code>ByteSequence</code> that is a subsequence of this sequence. The subsequence
   * starts with the <code>byte</code> value at the specified index and ends with the
   * <code>byte</code> value at index <tt>end - 1</tt>. The length (in <code>byte</code>s) of the
   * returned sequence is <tt>end - start</tt>, so if <tt>start == end</tt> then an empty sequence
   * is returned.
   *
   * @param startIndex the start index, inclusive
   * @param endIndex the end index, exclusive
   * @return the specified subsequence
   * @throws IndexOutOfBoundsException if <tt>start</tt> or <tt>end</tt> are negative, if
   *             <tt>end</tt> is greater than <tt>length()</tt>, or if <tt>start</tt> is greater
   *             than <tt>end</tt>
   * @since 1.0
   */
  default ByteSequence subSequence(int startIndex, int endIndex) {
    int l = endIndex - startIndex;
    if (l < 0) {
      throw new IndexOutOfBoundsException(String.valueOf(l));
    }
    if (startIndex < 0) {
      throw new IndexOutOfBoundsException(String.valueOf(startIndex));
    }
    if (startIndex + l > length()) {
      throw new IndexOutOfBoundsException(String.valueOf(startIndex + l));
    }
    return new ByteSequence() {
      public int length() {
        return l;
      }

      public byte byteAt(int index) {
        return ByteSequence.this.byteAt(startIndex + index);
      }
    };
  }

  /**
   * Returns a <code>byte[]</code> containing the bytes in this sequence in the same order as this
   * sequence. The length of the byte array will be the length of this sequence. Creates a new
   * byte array with every invocation.
   *
   * @since 0.4
   */
  default byte[] toByteArray() {
    byte[] b = new byte[length()];
    for (int i = 0; i < b.length; i++) {
      b[i] = byteAt(i);
    }
    return b;
  }

  /**
   * Returns a stream of {@code int} zero-extending the {@code byte} values from this sequence.
   *
   * @return an IntStream of byte values from this sequence
   * @since 0.4
   */
  default IntStream bytes() {
    class ByteIterator implements PrimitiveIterator.OfInt {
      int cur = 0;

      public boolean hasNext() {
        return cur < length();
      }

      public int nextInt() {
        if (hasNext()) {
          return byteAt(cur++) & 0xFF;
        } else {
          throw new NoSuchElementException();
        }
      }

      @Override
      public void forEachRemaining(IntConsumer block) {
        for (; cur < length(); cur++) {
          block.accept(byteAt(cur) & 0xFF);
        }
      }
    }
    return StreamSupport.intStream(() -> Spliterators.spliterator(
        new ByteIterator(),
        length(),
        Spliterator.ORDERED),
        Spliterator.SUBSIZED | Spliterator.SIZED | Spliterator.ORDERED,
        false);
  }

  /**
   * Creates a <code>ByteSequence</code> from an existing <code>byte[]</code>. The byte array is
   * not defensively copied, therefore the given bytes must not mutate to ensure the contract of
   * an immutable ByteSequence.
   *
   * @since 0.4
   */
  static ByteSequence create(byte[] buffer) {
    return new ByteArraySequence(buffer, 0, buffer.length);
  }

  /**
   * This method is used to generate a byte sequence for the given memory buffer object.
   * @param buffer
   * @return If the input argument is null, return a null.
   */
  static ByteSequence create(MemoryBuffer buffer) {
    if (buffer == null)
      return null;
    return create(new String(buffer.getBuffer()).getBytes());
  }
}
