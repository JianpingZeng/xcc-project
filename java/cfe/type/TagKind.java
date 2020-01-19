package cfe.type;
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

import cfe.cparser.DeclSpec;
import tools.Util;

/**
 * The kind of a tc jlang.type.
 */
public enum TagKind {
  TTK_struct, TTK_union, TTK_enum;

  public static TagKind getTagTypeKindForTypeSpec(DeclSpec.TST tagType) {
    switch (tagType) {
      case TST_struct:
        return TTK_struct;
      case TST_union:
        return TTK_union;
      case TST_enum:
        return TTK_enum;
      default:
        Util.shouldNotReachHere("Type specifier is not a tc jlang.type kind");
        return TTK_union;
    }
  }
}
