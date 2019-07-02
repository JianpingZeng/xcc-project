package cfe.support;
/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2019, Jianping Zeng.
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

/**
 * This is an opaque identifier used by SourceManager which refers to
 * a source file (MemoryBuffer) along with its #include path and #line data.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public class FileID implements Comparable<FileID> {
  /**
   * The unique ID for this File.
   * It is defined by {@linkplain cfe.basic.SourceManager} and incremented
   * automatically when encountering a new File.
   */
  private int id;

  /**
   * A flag indicates whether this FileID stems from built-in macro instead of
   * regulal C source file. It default to false, otherwise it is set by
   * {@linkplain #setIsBuiltin(boolean)}.
   */
  private boolean isBuiltIn;

  public FileID() {
    super();
    id = 0;
  }

  public FileID(int id) {
    this.id = id;
  }

  public boolean isInvalid() {
    return id == 0;
  }

  @Override
  public int compareTo(FileID o) {
    return id - o.id;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;

    if (obj == null) return false;

    if (getClass() != obj.getClass())
      return false;

    FileID other = (FileID) obj;
    return id == other.id;
  }

  public static FileID getSentinel() {
    return new FileID(~0);
  }

  @Override
  public int hashCode() {
    return id;
  }

  public int getID() {
    return id;
  }

  public boolean getIsBuiltin() {
    return isBuiltIn;
  }

  public void setIsBuiltin(boolean val) {
    isBuiltIn = val;
  }
}
