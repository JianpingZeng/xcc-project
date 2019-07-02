package utils.tablegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng
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

import tools.Util;
import utils.tablegen.Init.UnsetInit;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public final class RecordVal {
  private String name;
  private RecTy ty;
  private int prefix;
  private Init value;

  public RecordVal(String name, RecTy ty, int prefix) {
    this.name = name;
    this.ty = ty;
    this.prefix = prefix;
    value = ty.convertValue(UnsetInit.getInstance());
    Util.assertion(value != null, "Cannot create unset value for current type!");
  }

  public String getName() {
    return name;
  }

  public int getPrefix() {
    return prefix;
  }

  public RecTy getType() {
    return ty;
  }

  public Init getValue() {
    return value;
  }

  public boolean setValue(Init val) {
    if (val != null) {
      value = val.convertInitializerTo(ty);
      return value == null;
    }

    value = null;
    return false;
  }

  public void dump() {
    print(System.err, true);
  }

  public void print(PrintStream os) {
    print(os, true);
  }

  public void print(PrintStream os, boolean printSem) {
    if (getPrefix() != 0) os.print("field ");
    getType().print(os);
    os.printf(" %s", getName());
    if (getValue() != null) {
      os.print(" = ");
      getValue().print(os);
    }
    if (printSem) os.println(";");
  }

  @Override
  public String toString() {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    print(new PrintStream(os));
    return os.toString();
  }

  @Override
  public RecordVal clone() {
    RecordVal rv = (RecordVal) new RecordVal(name, ty, prefix);
    rv.value = value.clone();
    return rv;
  }
}
