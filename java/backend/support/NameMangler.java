package backend.support;
/*
 * Extremely C language Compiler
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

import backend.type.Type;
import backend.value.*;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.Util;

import java.util.HashMap;
import java.util.HashSet;

/**
 * This file defines a class which responsible for make asmName mangling for global
 * linkage entity of a module.
 *
 * @author Jianping Zeng
 * @version 0.1
 */
public class NameMangler {
  /**
   * This string is added to each symbol that is emitted, unless the
   * symbol is marked as not needed this prefix.
   */
  private String prefix;

  private String privatePrefix;
  private String linkerPrivatePrefix;

  /**
   * If this set, the target accepts global names in input.
   * e.g. "foo bar" is a legal asmName. This syntax is used instead
   * of escape the space character. By default, this is false.
   */
  private boolean useQuotes;
  /**
   * Memorize the asmName that we assign a value.
   */
  private HashMap<Value, String> memo;

  private int count;

  private TObjectIntHashMap<Type> typeMap;
  private int typeCount;
  /**
   * Keep track of which global value is mangled in the current module.
   */
  private HashSet<GlobalValue> mangledGlobals;

  private int acceptableChars[];
  private static int globalID = 0;

  public NameMangler(Module m) {
    this(m, "", "", "");
  }

  public NameMangler(Module m, String globalPrefix,
                     String privateGlobalPrefix,
                     String linkerPrivateGlobalPrefix) {
    this.prefix = globalPrefix;
    this.privatePrefix = privateGlobalPrefix;
    this.linkerPrivatePrefix = linkerPrivateGlobalPrefix;
    useQuotes = false;
    memo = new HashMap<>();
    count = 0;
    typeMap = new TObjectIntHashMap<>();
    typeCount = 0;
    mangledGlobals = new HashSet<>();
    acceptableChars = new int[256 / 32];

    // Letters and numbers are acceptable.
    for (char x = 'a'; x <= 'z'; x++)
      markCharAcceptable(x);

    for (char x = 'A'; x <= 'Z'; x++)
      markCharAcceptable(x);

    for (char x = '0'; x <= '9'; x++)
      markCharAcceptable(x);

    // These chars are acceptable.
    markCharAcceptable('_');
    markCharAcceptable('$');
    markCharAcceptable('.');

    HashMap<String, GlobalValue> names = new HashMap<>();
    for (Function fn : m.getFunctionList())
      insertName(fn, names);

    for (GlobalVariable gv : m.getGlobalVariableList())
      insertName(gv, names);
  }

  private void insertName(GlobalValue gv, HashMap<String, GlobalValue> names) {
    if (!gv.hasName()) return;

    // Figure out if this is already used.
    GlobalValue existingValue = names.get(gv.getName());
    if (existingValue == null)
      existingValue = gv;
    else {
      // If GV is external but the existing one is static, mangle the existing one
      if (gv.hasExternalLinkage() && !existingValue.hasExternalLinkage()) {
        mangledGlobals.add(existingValue);
        existingValue = gv;
      } else if (gv.hasExternalLinkage() && existingValue.hasExternalLinkage()
          && gv.isExternal() && existingValue.isExternal()) {
        // If the two globals both have external inkage, and are both external,
        // don't mangle either of them, we just have some silly type mismatch.
      } else {
        // otherwise, mangle gv.
        mangledGlobals.add(gv);
      }
    }
  }

  private int getTypeID(Type ty) {
    int e = typeMap.get(ty);
    if (e == 0) e = ++typeCount;
    return e;
  }

  enum ManglerPrefixTy {
    /**
     * Emit default string before each symbol.
     */
    Default,
    /**
     * Emit "private" prefix before each symbol.
     */
    Private,
    /**
     * Emit "linker private" prefix before each symbol.
     */
    LinkerPrivate
  }

  public String getMangledName(GlobalValue gv, String suffix, boolean forceprivate) {
    Util.assertion(!(gv instanceof Function) || !((Function) gv).isIntrinsicID(),
        "Intrinsic function shouldn't be mangled!");
    ManglerPrefixTy prefixTy =
        (gv.hasPrivateLinkage() || forceprivate) ? ManglerPrefixTy.Private
            : gv.hasLinkerPrivateLinkage() ? ManglerPrefixTy.LinkerPrivate :
            ManglerPrefixTy.Default;
    if (gv.hasName())
      return makeNameProper(gv.getName() + suffix, prefixTy);

    int typeUniqueID = getTypeID(gv.getType());
    String name = "__unnamed_" + typeUniqueID + "_" + globalID++;
    return makeNameProper(name + suffix, prefixTy);
  }

  public String getMangledName(GlobalValue gv) {
    return getMangledName(gv, "", false);
  }

  private static char hexDigit(int v) {
    return v < 10 ? Character.forDigit(v, 10) : (char) (v - 10 + (int) 'A');
  }

  private String mangleLetter(char digit) {
    return "_" + hexDigit(digit >> 4) + hexDigit(digit & 15) + "_";
  }

  private String makeNameProper(String origin, ManglerPrefixTy prefixTy) {
    Util.assertion(!origin.isEmpty(), "Can't mangle an empty string");

    String result = "";
    if (!useQuotes) {
      int i = 0;
      boolean needsPrefix = true;
      if (origin.charAt(i) == '1') {
        needsPrefix = false;
        ++i;
      }
      // Mangle the first letter specially, don't allow numbers.
      if (origin.charAt(i) >= '0' && origin.charAt(i) <= '9')
        result += mangleLetter(origin.charAt(i++));

      for (; i < origin.length(); i++) {
        char ch = origin.charAt(i);
        if (!isCharAcceptable(ch))
          result += mangleLetter(ch);
        else
          result += ch;
      }
      if (needsPrefix) {
        result = prefix + result;
        if (prefixTy == ManglerPrefixTy.Private)
          result = privatePrefix + result;
        else if (prefixTy == ManglerPrefixTy.LinkerPrivate)
          result = linkerPrivatePrefix + result;
      }
      return result;
    }
    boolean needsPrefix = true;
    boolean needsQuotes = false;
    int i = 0;
    if (origin.charAt(i) == '1') {
      needsPrefix = false;
      ++i;
    }

    if (origin.charAt(i) >= '0' && origin.charAt(i) <= '9')
      needsQuotes = true;
    // Do an initial scan of the string, checking to see if we need quotes or
    // to escape a '"' or not.
    if (!needsQuotes) {
      for (; i < origin.length(); i++) {
        char ch = origin.charAt(i);
        if (!isCharAcceptable(ch)) {
          needsQuotes = true;
          break;
        }
      }
    }

    if (!needsQuotes) {
      if (!needsPrefix)
        return origin.substring(1);

      result = prefix + origin;
      if (prefixTy == ManglerPrefixTy.Private)
        result = privatePrefix + result;
      else if (prefixTy == ManglerPrefixTy.LinkerPrivate)
        result = linkerPrivatePrefix + result;

      return result;
    }

    if (needsPrefix)
      result = origin.substring(0, i);

    // Otherwise, construct the string in expensive way.
    for (; i < origin.length(); i++) {
      char ch = origin.charAt(i);
      if (ch == '"')
        result += "_QQ_";
      else if (ch == '\n')
        result += "_NL_";
      else
        result += ch;
    }
    if (needsPrefix) {
      result = prefix + result;
      if (prefixTy == ManglerPrefixTy.Private)
        result = privatePrefix + result;
      else if (prefixTy == ManglerPrefixTy.LinkerPrivate)
        result = linkerPrivatePrefix + result;
    }
    result = '"' + result + '"';
    return result;
  }

  private void markCharAcceptable(char x) {
    acceptableChars[x / 32] |= 1 << (x & 31);
  }

  private void markCharUnacceptale(char x) {
    acceptableChars[x / 32] &= ~(1 << (x & 31));
  }

  private boolean isCharAcceptable(char ch) {
    return (acceptableChars[ch / 32] & (1 << (ch & 31))) != 0;
  }

  public String makeNameProperly(String name) {
    return null;
  }

  public void setUseQuotes(boolean val) {
    useQuotes = val;
  }
}
