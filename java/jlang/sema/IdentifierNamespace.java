package jlang.sema;
/*
 * Extremely C language CompilerInstance
 * Copyright (c) 2015-2018, Xlous
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
 * The different namespaces in which declarations may appear.
 * According to C99 6.2.3, there are four namespaces, labels, tags, members and
 * ordinary identifiers.
 *
 * @author Jianping Zeng
 * @version 0.1
 */
public enum IdentifierNamespace {
  /**
   * Labels, declared with 'x:' and reference with 'goto x'.
   */
  IDNS_Label,

  /**
   * Tags, declared with 'struct/union/enum foo;' and reference with 'struct foo'
   * All tags are also same jlang.type.
   */
  IDNS_Tag,

  /**
   * Members, all identifiers declared as members of any one struct or union.
   * Every struct and union introduces its own name space of this kind.
   */
  IDNS_Member,

  /**
   * Ordinary names.  In C, everything that's not a label, tc,
   * or member ends up here.
   */
  IDNS_Ordinary,
}
