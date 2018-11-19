package utils.tablegen;
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

import backend.codegen.MVT;
import tools.Util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class MachineValueTypeSet implements Iterable<MVT> {
  private final static int Capability = 1024;
  // sizeof(char) * sizeof(long) = 64.
  private final static int WordWidth = 64;
  private final static int NumWords = Capability / WordWidth;

  private long[] words;

  public MachineValueTypeSet() {
    words = new long[NumWords];
  }

  public void clear() {
    Arrays.fill(words, 0);
  }

  public boolean isEmpty() {
    for (long w : words) {
      if (w != 0) return false;
    }
    return true;
  }

  public boolean count(MVT vt) {
    return ((words[vt.simpleVT / WordWidth] >>> (vt.simpleVT % WordWidth)) & 0x1) != 0;
  }

  /**
   * Set the element in the given index as true.
   *
   * @param vt
   * @return Return true if the given position has been setted as true.
   */
  public boolean insert(MVT vt) {
    boolean v = count(vt);
    words[vt.simpleVT / WordWidth] |= 1L << (vt.simpleVT % WordWidth);
    return v;
  }

  public MachineValueTypeSet insert(MachineValueTypeSet set) {
    for (int i = 0; i < words.length; i++)
      words[i] |= set.words[i];
    return this;
  }

  public void erase(MVT vt) {
    words[vt.simpleVT / WordWidth] &= ~(1L << (vt.simpleVT % WordWidth));
  }

  public int size() {
    /// FIXME computing the number of seted bits
    int num = 0;
    for (long n : words) {
      if (n == 0) continue;
      while (n != 0) {
        ++num;
        n = n & (n - 1);
      }
    }
    return num;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (this == obj) return true;
    if (getClass() != obj.getClass()) return false;
    MachineValueTypeSet vt = (MachineValueTypeSet) obj;
    if (size() != vt.size()) return false;
    for (int i = 0, e = words.length; i < e; i++)
      if (words[i] != vt.words[i])
        return false;
    return true;
  }

  /**
   * Erases all MVT in this set if given predicate is satisfied.
   *
   * @param pred
   * @return Return true if there are MVT has been erased.
   */
  public boolean eraseIf(Predicate<MVT> pred) {
    boolean erased = false;
    for (Iterator<MVT> itr = iterator(); itr.hasNext(); ) {
      MVT vt = itr.next();
      boolean res = !pred.test(vt);
      if (res) continue;

      erased = true;
      erase(vt);
    }
    return erased;
  }

  public MachineValueTypeSet clone() {
    MachineValueTypeSet res = new MachineValueTypeSet();
    System.arraycopy(words, 0, res.words, 0, words.length);
    return res;
  }

  public boolean getBit(int index) {
    Util.assertion(index >= 0 && index < Capability);
    return (words[index / WordWidth] & (1L << (index % WordWidth))) != 0;
  }

  /***
   * FIXME, iterator is needed to be tested !!!!
   * @return
   */
  @Override
  public Iterator<MVT> iterator() {
    return new MVTIterator(this);
  }

  public void forEach(Consumer<? super MVT> action) {
    for (int idx = nextSetBit(0); idx != -1; idx = nextSetBit(idx + 1)) {
      MVT vt = new MVT(idx);
      action.accept(vt);
    }
  }

  public int nextSetBit(int startIndex) {
    if (startIndex < 0)
      return -1;

    for (int i = startIndex, e = Capability; i < e; ) {
      if (words[i / WordWidth] == 0) {
        i += ((i / WordWidth) + 1) * WordWidth;
        continue;
      }
      if (getBit(i)) return i;
      ++i;
    }
    return -1;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder("[");
    boolean firstOne = true;

    for (int idx = nextSetBit(0); idx >= 0; idx = nextSetBit(idx + 1)) {
      if (firstOne) firstOne = false;
      else buf.append(", ");
      buf.append(ValueTypeByHwMode.getMVTName(new MVT(idx)));
    }
    buf.append(']');
    return buf.toString();
  }

  @Override
  public int hashCode() {
    int res = 0;
    for (long n : words)
      res += n;
    return res;
  }

  public MVT getFirstSetBit() {
    for (int idx = nextSetBit(0); idx >= 0; idx = nextSetBit(idx + 1))
      return new MVT(idx);
    return null;
  }

  private static class MVTIterator implements Iterator<MVT> {
    private int pos;
    private final MachineValueTypeSet typeSet;
    private final int size;

    MVTIterator(MachineValueTypeSet typeSet) {
      pos = 0;
      this.typeSet = typeSet;
      size = Capability;
      pos = -1;
      for (pos = typeSet.nextSetBit(0); pos != -1; pos = typeSet.nextSetBit(pos + 1))
        if (typeSet.getBit(pos))
          break;
    }

    @Override
    public boolean hasNext() {
      return pos != -1;
    }

    @Override
    public MVT next() {
      MVT res = new MVT(pos);
      ++pos;
      for (pos = typeSet.nextSetBit(pos); pos != -1; pos = typeSet.nextSetBit(pos + 1))
        if (typeSet.getBit(pos))
          break;

      return res;
    }
  }
}
