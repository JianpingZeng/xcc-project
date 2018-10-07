package jlang.sema;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng
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

import static jlang.diag.Diagnostic.DiagnosticBuilder;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public class SemaDiagnosticBuilder extends DiagnosticBuilder {
  private Sema sema;
  private int diagID;

  public SemaDiagnosticBuilder(DiagnosticBuilder db, Sema sema, int diagID) {
    super(db);
    this.sema = sema;
    this.diagID = diagID;
  }
}
