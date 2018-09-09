/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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

package jlang.sema;

import jlang.clex.IdentifierInfo;
import jlang.support.SourceLocation;
import jlang.support.SourceRange;

/**
 * Represents a C unqualified id that has been parsed.
 *
 * @author Jianping Zeng
 * @version 0.1
 */
public class UnqualifiedId {
  public enum DeclarationKind {
    /**
     * An abstract declarator.
     */
    DK_Abstract,

    /**
     * A normal declarator.
     */
    DK_Normal,
  }

  private IdentifierInfo identifier;
  private DeclarationKind kind;
  private SourceLocation startLocation;
  private SourceLocation endLocation;

  public UnqualifiedId() {
    identifier = null;
    kind = null;
    startLocation = new SourceLocation();
    endLocation = new SourceLocation();
  }

  public UnqualifiedId(UnqualifiedId other) {
    identifier = other.identifier;
    kind = other.kind;
    startLocation = new SourceLocation(other.startLocation);
    endLocation = new SourceLocation(other.endLocation);
  }

  /**
   * Set this to invalid state.
   */
  public void clear() {
    identifier = null;
    kind = null;
    startLocation = new SourceLocation();
    endLocation = new SourceLocation();
  }

  public boolean isValid() {
    return startLocation.isValid();
  }

  public boolean isInvalid() {
    return !isValid();
  }

  public void setIdentifier(IdentifierInfo id, SourceLocation loc) {
    identifier = id;
    startLocation = loc;
    if (id != null)
      kind = DeclarationKind.DK_Normal;
    else
      kind = DeclarationKind.DK_Abstract;
  }

  public IdentifierInfo getIdentifier() {
    return identifier;
  }

  public SourceRange getSourceRange() {
    return new SourceRange(startLocation, endLocation);
  }

  public SourceLocation getStartLocation() {
    return startLocation;
  }

  public SourceLocation getEndLocation() {
    return endLocation;
  }

  public DeclarationKind getkind() {
    return kind;
  }
}
