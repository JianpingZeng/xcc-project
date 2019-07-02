/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2015-2019, Jianping Zeng.
 * All rights reserved.
 *
 * Please refer the LICENSE for detail.
 */

package backend.mc;
/**
 * @author Jianping Zeng
 * @version 0.4
 */
public final class SubtargetInfoKV implements Comparable<SubtargetInfoKV> {
  public String key;
  public Object value;

  public SubtargetInfoKV(String k, Object v) {
    key = k;
    value = v;
  }

  @Override
  public int compareTo(SubtargetInfoKV o) {
    return key.compareTo(o.key);
  }
}
