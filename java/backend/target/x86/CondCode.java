package backend.target.x86;
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

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public interface CondCode {
  int COND_A = 0;
  int COND_AE = 1;
  int COND_B = 2;
  int COND_BE = 3;
  int COND_E = 4;
  int COND_G = 5;
  int COND_GE = 6;
  int COND_L = 7;
  int COND_LE = 8;
  int COND_NE = 9;
  int COND_NO = 10;
  int COND_NP = 11;
  int COND_NS = 12;
  int COND_O = 13;
  int COND_P = 14;
  int COND_S = 15;

  // Artificial condition codes. These are used by AnalyzeBranch
  // to indicate a block terminated with two conditional branches to
  // the same location. This occurs in code using FCMP_OEQ or FCMP_UNE,
  // which can't be represented on x86 with a single condition. These
  // are never used in MachineInstrs.
  int COND_NE_OR_P = 16;
  int COND_NP_OR_E = 17;

  int COND_INVALID = 18;
}
