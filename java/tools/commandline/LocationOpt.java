/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package tools.commandline;

import tools.Util;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class LocationOpt<T> extends Opt<T> {
  private LocationClass<T> lc;

  public LocationOpt(Parser<T> parser, Modifier... mods) {
    super(parser, mods);
  }

  private void check() {
    Util.assertion(lc != null, "LocationClassApplicator::apply not called" + " for a command line option with external storage");

  }

  @Override
  public void setValue(T val) {
    check();
    lc.setLocation(val);
  }

  public void setLocation(LocationClass<T> loc) {
    lc = loc;
  }
}
