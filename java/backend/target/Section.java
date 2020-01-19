package backend.target;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
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
 * @author Jianping Zeng
 * @version 0.4
 */
public class Section {
  private String name;
  private int flags;

  public Section(String name) {
    this(name, SectionFlags.Invalid);
  }

  public Section(String name, int flags) {
    this.name = name;
    this.flags = flags;
  }

  public boolean isNamed() {
    return (flags & SectionFlags.Named) != 0;
  }

  public int getEntitySize() {
    return (flags >> 24) & 0XFF;
  }

  public String getName() {
    return name;
  }

  public int getFlags() {
    return flags;
  }
}
