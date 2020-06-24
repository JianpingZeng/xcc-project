package tools.commandline;
/*
 * Extremely C language Compiler.
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

import java.util.ArrayList;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class ValueClass<T> implements Modifier {
  public static class Entry<T> {
    public String enumName;
    public T enumVal;
    public String helpStr;

    public Entry(T enumVal,
                 String enumName,
                 String helpStr) {
      this.enumName = enumName;
      this.enumVal = enumVal;
      this.helpStr = helpStr;
    }
  }

  private ArrayList<Entry<T>> values;

  public ValueClass(Entry<T>... entries) {
    values = new ArrayList<>();
    for (Entry<T> e : entries)
      values.add(e);
  }

  @Override
  public void apply(Option<?> opt) {
    for (Entry<T> e : values) {
      ((Option<T>) opt).getParser().addLiteralOption(e.enumName, e.enumVal, e.helpStr);
    }
  }
}
