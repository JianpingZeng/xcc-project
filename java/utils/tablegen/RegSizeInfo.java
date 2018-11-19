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

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class RegSizeInfo implements Comparable<RegSizeInfo>, Cloneable {
  public long regSize;
  public long spillSize;
  public long spillAlignment;

  public RegSizeInfo(Record r, CodeGenHwModes cgh) {
    regSize = r.getValueAsInt("RegSize");
    spillSize = r.getValueAsInt("SpillSize");
    spillAlignment = r.getValueAsInt("SpillAlignment");
  }

  public RegSizeInfo() {
  }

  @Override
  public int compareTo(RegSizeInfo o) {
    if (regSize < o.regSize)
      return -1;
    if (regSize > o.regSize)
      return 1;
    if (spillSize < o.spillSize)
      return -1;
    if (spillSize > o.spillSize)
      return 1;
    if (spillAlignment < o.spillAlignment)
      return -1;
    return spillAlignment == o.spillAlignment ? 0 : 1;
  }

  @Override
  public String toString() {
    return String.format("[R=%d, S=%d, A=%d]]", regSize, spillSize, spillAlignment);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (obj == this) return true;
    if (getClass() != obj.getClass())
      return false;
    RegSizeInfo info = (RegSizeInfo) obj;
    return regSize == info.regSize &&
        spillSize == info.spillSize &&
        spillAlignment == info.spillAlignment;
  }

  public boolean isSubClassOf(RegSizeInfo info) {
    return regSize <= info.regSize &&
        spillAlignment != 0 && info.spillAlignment % spillAlignment == 0 &&
        spillSize <= info.spillSize;
  }

  @Override
  public RegSizeInfo clone() {
    RegSizeInfo res = new RegSizeInfo();
    res.regSize = regSize;
    res.spillSize = spillSize;
    res.spillAlignment = spillAlignment;
    return res;
  }
}
