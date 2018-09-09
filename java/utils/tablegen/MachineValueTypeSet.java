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
  private final static int Capability = Integer.MAX_VALUE;
  // sizeof(char) * sizeof(long) = 64.
  private final static int WordWidth = 64;
  private final static int NumWords = Capability/WordWidth;

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
    return ((words[vt.simpleVT/WordWidth] >>> (vt.simpleVT%WordWidth)) & 0x1) != 0;
  }

  /**
   * Set the element in the given index as true.
   * @param vt
   * @return  Return true if the given position has been setted as true.
   */
  public boolean insert(MVT vt) {
    boolean v = count(vt);
    words[vt.simpleVT/WordWidth] |= 1 <<(vt.simpleVT%WordWidth);
    return v;
  }
  public void erase(MVT vt) {
    words[vt.simpleVT/WordWidth] &= ~(1 <<(vt.simpleVT%WordWidth));
  }

  public int size() {
    return Capability;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (this == obj) return true;
    if (getClass() != obj.getClass()) return false;
    MachineValueTypeSet vt = (MachineValueTypeSet) obj;
    if (size() != vt.size()) return false;
    for (int i = 0,e = words.length; i < e; i++)
      if (words[i] != vt.words[i])
        return false;
    return true;
  }

  /**
   * Erases all MVT in this set if given predicate is satisfied.
   * @param pred
   * @return  Return true if there are MVT has been erased.
   */
  public boolean eraseIf(Predicate<MVT> pred) {
    boolean erased = false;
    for (Iterator<MVT> itr = iterator(); itr.hasNext(); ) {
      MVT vt = itr.next();
      if (!pred.test(vt))
        continue;
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

  @Override
  public Iterator<MVT> iterator() {
    return new MVTIterator(this);
  }

  @Override
  public void forEach(Consumer<? super MVT> action) {
    for (Iterator<MVT> itr = iterator(); itr.hasNext(); )
      action.accept(itr.next());
  }

  private static class MVTIterator implements Iterator<MVT> {
    private int pos;
    private MachineValueTypeSet typeSet;
    private int size;

    MVTIterator(MachineValueTypeSet typeSet) {
      pos = 0;
      this.typeSet = typeSet;
      size = typeSet.size();
    }
    @Override
    public boolean hasNext() {
      if (pos >= size) return false;
      return !typeSet.isEmpty();
    }

    @Override
    public MVT next() {
      Util.assertion(hasNext());
      for (; pos < size; ++pos) {
        if ((typeSet.words[pos/MachineValueTypeSet.WordWidth] &
            (1 << pos % MachineValueTypeSet.WordWidth)) != 0)
          return new MVT(pos);
      }
      return null;
    }
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder("[");
    int i  = 0, e = size();
    for (Iterator<MVT> itr = iterator(); itr.hasNext(); ){
      buf.append(ValueTypeByHwMode.getMVTName(itr.next()));
      if (i < e - 1)
        buf.append(", ");
    }
    buf.append('}');
    return buf.toString();
  }
}
