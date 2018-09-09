package backend.target;
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

import backend.codegen.dagisel.SDep;
import backend.codegen.dagisel.SUnit;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public abstract class TargetSubtarget {
  public int getSpecialAddressLatency() {
    return 0;
  }

  public boolean isTargetDarwin() {
    return false;
  }

  public boolean isTargetELF() {
    return false;
  }

  public boolean isTargetCygMing() {
    return false;
  }

  public void adjustSchedDependency(SUnit opSU, SUnit su, SDep dep) {

  }
}
