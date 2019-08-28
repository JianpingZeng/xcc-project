/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
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

package backend.target;

import backend.pass.PassRegisterationUtility;
import backend.target.x86.X86TargetInfo;
import config.Targets;
import tools.Util;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class TargetSelect {
  static{
    /**
     * This static method block must be called to register all of passes.
     */
    PassRegisterationUtility.registerPasses();
  }

  public static void InitializeAllTargetInfo() {
    for (String t : Targets.TargetNames) {
      switch (t) {
        case "X86":
          X86TargetInfo.InitializeX86TargetInfo();
          break;
        default:
          Util.shouldNotReachHere(String.format("Unknown target name '%s'!", t));
      }
    }
  }

  public static void InitializeAllTarget() {
    for (String t : Targets.TargetNames) {
      switch (t) {
        case "X86":
          X86TargetInfo.InitiliazeX86Target();
          break;
        default:
          Util.shouldNotReachHere(String.format("Unknown target name '%s'!", t));
      }
    }
  }
}
