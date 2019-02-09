package cfe.support;
/*
 * Extremely C language Compiler.
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
 * Each InstantiationInfo encodes the Instantiation
 * location - where the token was ultimately instantiated, and the
 * SpellingLoc - where the actual character data for the token came from.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public class InstantiationInfo {
  /**
   * The position of source file Where the spelling for the token can be found.
   */
  private int literalLoc;

  /**
   * In a macro expansion, these indicate the start and end of the
   * instantiation.  In object-like macros, these will be the same.
   * In a function-like macro instantiation, the start will be the
   * identifier and the end will be the ')'.
   */
  private int instantiationLocStart, instantiationLocEnd;

  public SourceLocation getLiteralLoc() {
    return SourceLocation.getFromRawEncoding(literalLoc);
  }

  public SourceLocation getInstantiationLocStart() {
    return SourceLocation.getFromRawEncoding(instantiationLocStart);
  }

  public SourceLocation getInstantiationLocEnd() {
    return SourceLocation.getFromRawEncoding(instantiationLocEnd);
  }

  public SourceRange getInstantiationLocRange() {
    return new SourceRange(getInstantiationLocStart(),
        getInstantiationLocEnd());
  }

  /**
   * Return a InstantiationInfo for an expansion.
   *
   * @param start
   * @param end
   * @param literalLoc
   * @return
   */
  public static InstantiationInfo get(SourceLocation start, SourceLocation end,
                                      SourceLocation literalLoc) {
    InstantiationInfo X = new InstantiationInfo();
    X.literalLoc = literalLoc.getRawEncoding();
    X.instantiationLocStart = start.getRawEncoding();
    X.instantiationLocEnd = end.getRawEncoding();
    return X;

  }
}
