package cfe.clex;
/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2019, Jianping Zeng.
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
 * An abstract class used to resolve numerical identifier
 * references (meaningful only to some external source) into IdentifierInfo.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public interface ExternalIdentifierLookup {
  /**
   * Return the identifier associated with the given ID number.
   * The ID 0 is associated with the NULL identifier.
   *
   * @param id
   * @return
   */
  IdentifierInfo getIdentifier(int id);
}
