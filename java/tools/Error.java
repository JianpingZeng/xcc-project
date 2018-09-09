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

package tools;

import jlang.support.MemoryBuffer;
import tools.SourceMgr.DiagKind;
import tools.SourceMgr.SMLoc;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public final class Error {
  public static SourceMgr sgr;

  public static void printMessage(SMLoc loc, String msg, DiagKind kind) {
    sgr.printMessage(loc, msg, "");
  }

  public static void printMessage(MemoryBuffer loc, String msg, DiagKind kind) {
    printMessage(SMLoc.get(loc), msg, kind);
  }

  public static void printNote(SMLoc loc, String msg) {
  }

  public static void printNote(MemoryBuffer loc, String msg) {
  }

  public static void printWarning(SMLoc loc, String msg) {
  }

  public static void printWarning(MemoryBuffer loc, String msg) {
  }

  public static void printError(SMLoc loc, String msg) {
  }

  public static void printError(MemoryBuffer loc, String msg) {
  }

  public static void printError(String msg) {
  }

  public static void printFatalError(String msg) {
  }

  public static void printFatalError(SMLoc loc, String msg) {
  }

  public static void printFatalError(MemoryBuffer loc, String msg) {
  }
}
