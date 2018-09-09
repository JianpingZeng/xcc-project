package jlang.clex;
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

import jlang.support.CharacteristicKind;
import jlang.support.SourceLocation;

/**
 * This interface provides a way to observe the actions of the
 * preprocessor as it does its thing.  Clients can define their hooks here to
 * implement preprocessor level tool.
 *
 * @author Jianping Zeng
 * @version 0.1
 */
public interface PPCallBack {
  enum FileChangeReason {
    EnterFile,
    ExitFile,
    SystemHeaderPragma,
    RenameFile
  }

  /**
   * This callback is invoked whenever a source file is
   * /// entered or exited.  The SourceLocation indicates the new location, and
   * /// EnteringFile indicates whether this is because we are entering a new
   * /// #include'd file (when true) or whether we're exiting one because we ran
   * /// off the end (when false).
   *
   * @param loc
   * @param reason
   * @param kind
   */
  default void fileChanged(SourceLocation loc,
                           FileChangeReason reason,
                           CharacteristicKind kind) {
  }

  default void ident(SourceLocation loc, String str) {
  }

  default void pragmaComment(SourceLocation loc, IdentifierInfo kind, String str) {
  }

  default void macroExpands(Token id, MacroInfo mi) {
  }

  default void macroDefined(IdentifierInfo ii, MacroInfo mi) {
  }

  default void macroUndefined(IdentifierInfo ii, MacroInfo mi) {
  }


  class PPChainedCallBack implements PPCallBack {
    private PPCallBack first, second;

    public PPChainedCallBack(PPCallBack first, PPCallBack second) {
      this.first = first;
      this.second = second;
    }

    @Override
    public void fileChanged(SourceLocation loc, FileChangeReason reason,
                            CharacteristicKind kind) {
      first.fileChanged(loc, reason, kind);
      second.fileChanged(loc, reason, kind);
    }

    @Override
    public void pragmaComment(SourceLocation loc, IdentifierInfo kind,
                              String str) {
      first.pragmaComment(loc, kind, str);
      second.pragmaComment(loc, kind, str);
    }

    @Override
    public void macroExpands(Token id, MacroInfo mi) {
      first.macroExpands(id, mi);
      second.macroExpands(id, mi);
    }

    @Override
    public void macroDefined(IdentifierInfo ii, MacroInfo mi) {
      first.macroDefined(ii, mi);
      second.macroDefined(ii, mi);
    }

    @Override
    public void macroUndefined(IdentifierInfo ii, MacroInfo mi) {
      first.macroUndefined(ii, mi);
      second.macroUndefined(ii, mi);
    }
  }
}
