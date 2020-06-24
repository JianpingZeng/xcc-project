package cfe.support;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng
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
 * Describes the different kinds of linkage.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public enum Linkage {
  /**
   * No linkage, which means that the entity is unique and
   * can only be referred to from within its scope.
   */
  NoLinkage,

  /**
   * Internal linkage, which indicates that the entity can
   * be referred to from within the translation unit (but not other
   * translation units).
   */
  InternalLinkage,

  /**
   * External linkage within a unique namespace. From the
   * langauge perspective, these entities have external
   * linkage. However, since they reside in an anonymous namespace,
   * their names are unique to this translation unit, which is
   * equivalent to having internal linkage from the code-generation
   * point of view.
   */
  UniqueExternalLinkage,

  /**
   * External linkage, which indicates that the entity can
   * be referred to from other translation units.
   */
  ExternalLinkage;


  /**
   * Determine whether the given linkage is semantically external.
   *
   * @param L
   * @return
   */
  public static boolean isExternalLinkage(Linkage L) {
    return L == UniqueExternalLinkage || L == ExternalLinkage;
  }

  /**
   * Compute the minimum linkage given two linages.
   *
   * @param L1
   * @param L2
   * @return
   */
  public static Linkage minLinkage(Linkage L1, Linkage L2) {
    return L1.ordinal() < L2.ordinal() ? L1 : L2;
  }
}
