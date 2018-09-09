package jlang.basic;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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

import tools.FoldingSetNodeID;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public final class Info {
  private String name, type, attributes, headerName;
  private boolean suppressed;

  public Info(String name, String type,
              String attr, String header,
              boolean suppressed) {
    this.name = name;
    this.type = type;
    attributes = attr;
    headerName = header;
    this.suppressed = suppressed;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getAttributes() {
    return attributes;
  }

  public void setAttributes(String attributes) {
    this.attributes = attributes;
  }

  public String getHeaderName() {
    return headerName;
  }

  public void setHeaderName(String headerName) {
    this.headerName = headerName;
  }

  public boolean isSuppressed() {
    return suppressed;
  }

  public void setSuppressed(boolean suppressed) {
    this.suppressed = suppressed;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null)
      return false;
    if (this == obj)
      return true;
    if (getClass() != obj.getClass())
      return false;

    Info rhs = (Info) obj;
    return name.equals(rhs.name) &&
        type.equals(rhs.type) &&
        attributes.equals(rhs.attributes) &&
        headerName.equals(rhs.headerName) &&
        suppressed == rhs.suppressed;
  }

  @Override
  public int hashCode() {
    FoldingSetNodeID id = new FoldingSetNodeID();
    id.addString(name);
    id.addString(type);
    id.addString(attributes);
    id.addString(headerName);
    id.addBoolean(suppressed);
    return id.computeHash();
  }
}
