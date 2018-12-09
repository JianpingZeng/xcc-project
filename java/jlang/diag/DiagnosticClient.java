package jlang.diag;
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

import jlang.support.LangOptions;

/**
 * This is an abstract interface, which should be implemented by client of
 * front-end, which formats and prints fully processed diagnostics.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public interface DiagnosticClient {
  /**
   * This is set by clients of diagnostics when they know the
   * language parameters of the diagnostics that may be sent through.  Note
   * that this can change over time if a DiagClient has multiple languages sent
   * through it.  It may also be set to null (e.g. when processing command line
   * options).
   *
   * @param langOptions
   */
  default void setLangOptions(LangOptions langOptions) {
  }

  /**
   * This method (whose default implementation returns true) indicates whether
   * the diagnostics handled by this DiagnosticClient should be included in
   * the number of diagnostics reported by Diagnostic.
   *
   * @return
   */
  default boolean includeInDiagnosticCounts() {
    return true;
  }

  /**
   * Handle this diagnostic, reporting it or capturing it to a log as needed.
   *
   * @param diagLevel
   * @param info
   */
  void handleDiagnostic(Diagnostic.Level diagLevel, DiagnosticInfo info);
}
