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

import cfe.support.LangOptions;
import tools.Pair;

import java.util.HashMap;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class IdentifierTable {
  static final int KEYALL = 1;
  static final int KEYC99 = 2;
  static final int KEYGNU = 4;
  static final int BOOLSUPPORT = 8;

  private IdentifierInfoLookup externalLookup;
  private HashMap<String, IdentifierInfo> hashtable;

  /**
   * Creates the identifier table, populate it with the given language infos
   * about the language keywords for the language specification.
   *
   * @param langOpts
   * @param externalLookup
   */
  public IdentifierTable(LangOptions langOpts, IdentifierInfoLookup externalLookup) {
    hashtable = new HashMap<>(8192);
    this.externalLookup = externalLookup;

    addKeywords(langOpts);
  }

  public void setExternalLookup(IdentifierInfoLookup externalLookup) {
    this.externalLookup = externalLookup;
  }

  /**
   * Returns the identifier token info for the specified named identifier.
   *
   * @param name
   * @return
   */
  public IdentifierInfo get(String name) {
    if (hashtable.containsKey(name))
      return hashtable.get(name);

    IdentifierInfo ii;
    if (externalLookup != null) {
      ii = externalLookup.get(name);
      if (ii != null) {
        // Cache in the hashmap.
        hashtable.put(name, ii);
        return ii;
      }
    }

    // Lookup failed, make a new IdentifierInfo.
    ii = new IdentifierInfo();
    hashtable.put(name, ii);

    // Make sure getName() knows how to find the IdentifierInfo contents.
    ii.setEntry(Pair.get(name, ii));
    return ii;
  }

  /**
   * Creates a new IdentifierInfo from the given string.
   * <br></br>
   * This is a lower-level version of get() that requires that this
   * identifier not be known previously and that does not consult an
   * external source for identifiers. In particular, external
   * identifier sources can use this routine to build IdentifierInfo
   * nodes and then introduce additional information about those
   * identifiers.
   *
   * @param name
   * @return
   */
  public IdentifierInfo createIdentifierInfo(String name) {
    if (hashtable.containsKey(name))
      return hashtable.get(name);

    IdentifierInfo ii = new IdentifierInfo();
    hashtable.put(name, ii);
    return ii;
  }

  public int size() {
    return hashtable.size();
  }

  public HashMap<String, IdentifierInfo> getHashtable() {
    return hashtable;
  }

  /// The C90/C99 flags are set to 0 if the token should be
  /// enabled in the specified language, set to 1 if it is an extension
  /// in the specified language, and set to 2 if disabled in the
  /// specified language.
  static void addKeyWord(String keyword, TokenKind kind, int flags,
                         LangOptions langOpts, IdentifierTable table) {
    int addResult = 0;
    if ((flags & KEYALL) != 0) addResult = 2;
    else if (langOpts.c99 && (flags & KEYC99) != 0) addResult = 2;
    else if (langOpts.bool && (flags & BOOLSUPPORT) != 0) addResult = 2;
    else if (langOpts.gnuMode && (flags & KEYGNU) != 0) addResult = 1;

    // This keyword is disabled.
    if (addResult == 0) return;

    IdentifierInfo info = table.get(keyword);
    info.setTokenID(kind);
    info.setIsExtensionToken(addResult == 1);
  }

  public void addKeywords(LangOptions langOpts) {
    for (int i = TokenKind.Void.ordinal(); i <= TokenKind.Typeof.ordinal(); i++) {
      TokenKind kind = TokenKind.values()[i];
      addKeyWord(kind.name, kind, kind.flags, langOpts, this);
    }

    // Add keyword kind to keywords table.
    for (KeywordAlias alias : KeywordAlias.values()) {
      addKeyWord(alias.name, alias.kind, alias.flag, langOpts, this);
    }
  }
}
