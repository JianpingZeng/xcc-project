package utils.tablegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
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

import tools.Pair;
import tools.Util;
import utils.tablegen.CodeGenHwModes.HwModeSelect;

import java.util.TreeSet;

import static utils.tablegen.CodeGenHwModes.DefaultMode;
import static utils.tablegen.CodeGenHwModes.getModeName;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class RegSizeInfoByHwMode extends InfoByHwMode<RegSizeInfo>
    implements Comparable<RegSizeInfoByHwMode> {

  public RegSizeInfoByHwMode() {
    super();
  }

  public RegSizeInfoByHwMode(Record r, CodeGenHwModes cgh) {
    HwModeSelect ms = cgh.getHwModeSelect(r);
    for (Pair<Integer, Record> itr : ms.items) {
      map.put(itr.first, new RegSizeInfo(itr.second, cgh));
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (this == obj) return true;
    if (getClass() != obj.getClass()) return false;
    RegSizeInfoByHwMode info = (RegSizeInfoByHwMode) obj;
    int m0 = map.firstKey();
    return get(m0).equals(info.get(m0));
  }

  @Override
  public int compareTo(RegSizeInfoByHwMode info) {
    int m0 = map.firstKey();
    return get(m0).compareTo(info.get(m0));
  }

  public boolean isSubClassOf(RegSizeInfoByHwMode info) {
    int m0 = map.firstKey();
    return get(m0).isSubClassOf(info.get(m0));
  }

  public boolean hasStricterSpillThan(RegSizeInfoByHwMode info) {
    int m0 = map.firstKey();
    RegSizeInfo info0 = get(m0), info1 = info.get(m0);
    return info0.compareTo(info1) > 0;
  }

  @Override
  public String toString() {
    TreeSet<Pair<Integer, RegSizeInfo>> pairs = new TreeSet<>(
        (o1, o2) ->
        {
          if (o1.first < o2.first) return -1;
          if (o1.first > o2.first) return 1;
          return o1.second.compareTo(o2.second);
        });
    map.forEach((mode, info) -> {
      pairs.add(Pair.get(mode, info));
    });
    StringBuilder buf = new StringBuilder("{");
    int i = 0, e = pairs.size();
    for (Pair<Integer, RegSizeInfo> itr : pairs) {
      buf.append("(").append(getModeName(itr.first))
          .append(":").append(itr.second.toString())
          .append(")");
      if (i != e - 1)
        buf.append(",");
    }
    buf.append('}');
    return buf.toString();
  }

  @Override
  public RegSizeInfo get(int mode) {
    if (!hasMode(mode)) {
      Util.assertion(hasDefault());
      map.put(mode, map.get(DefaultMode).clone());
    }
    return map.get(mode);
  }
}
