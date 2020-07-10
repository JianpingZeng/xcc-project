package tools.commandline;
/*
 * Extremely Compiler Collection.
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

import tools.OutRef;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class ParserDouble extends Parser<Double> {
  public boolean parse(Option<?> opt,
                       String optName, String arg,
                       OutRef<Double> val) {
    try {
      val.set(Double.parseDouble(arg));
    } catch (NumberFormatException ex) {
      return opt.error("'" + arg + "' value invalid for double argument");
    }
    return false;
  }

  @Override
  public String getValueName() {
    return "number";
  }

  @Override
  public <T1> void initialize(Option<T1> opt) {
  }

  @Override
  public ValueExpected getValueExpectedFlagDefault() {
    return ValueExpected.ValueRequired;
  }
}
