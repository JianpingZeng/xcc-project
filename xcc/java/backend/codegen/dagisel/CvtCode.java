/*
 * Extremely Compiler Collection
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

package backend.codegen.dagisel;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public enum CvtCode {
  CVT_FF,     // Float from Float
  CVT_FS,     // Float from Signed
  CVT_FU,     // Float from Unsigned
  CVT_SF,     // Signed from Float
  CVT_UF,     // Unsigned from Float
  CVT_SS,     // Signed from Signed
  CVT_SU,     // Signed from Unsigned
  CVT_US,     // Unsigned from Signed
  CVT_UU,     // Unsigned from Unsigned
  CVT_INVALID // Marker - Invalid opcode
}
