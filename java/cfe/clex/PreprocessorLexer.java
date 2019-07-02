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

import cfe.support.FileID;
import cfe.support.SourceLocation;
import tools.Util;

import java.nio.file.Path;
import java.util.Stack;

import static cfe.diag.DiagnosticLexKindsTag.err_pp_expects_filename;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public abstract class PreprocessorLexer {
  /**
   * Preprocessor object controlling lexing.
   */
  protected Preprocessor pp;

  /**
   * The SourceManager FileID corresponding to the file being lexed.
   */
  protected FileID fid;

  /**
   * This is true when parsing #XXX.  This turns
   * '\n' into a tok::eom token.
   */
  protected boolean parsingPreprocessorDirective;

  /**
   * True after #include: this turns <xx> into a tok::angle_string_literal token.
   */
  protected boolean parsingFilename;

  /**
   * True if in raw mode:  This flag disables interpretation of
   * /// tokens and is a far faster mode to lex in than non-raw-mode.  This flag:
   * ///  1. If EOF of the current lexer is found, the include stack isn't popped.
   * ///  2. Identifier information is not looked up for identifier tokens.  As an
   * ///     effect of this, implicit macro expansion is naturally disabled.
   * ///  3. "#" tokens at the start of a line are treated as normal tokens, not
   * ///     implicitly transformed by the lexer.
   * ///  4. All diagnostic messages are disabled.
   * ///  5. No callbacks are made into the preprocessor.
   * ///
   * /// Note that in raw mode that the PP pointer may be null.
   */
  protected boolean lexingRawMode;

  /**
   * This is a state machine that detects the #ifndef-wrapping a file
   * /// idiom for the multiple-include optimization.
   */
  protected MultipleIncludeOpt miOpt;

  /**
   * Information about the set of #if/#ifdef/#ifndef blocks
   * /// we are currently in
   */
  protected Stack<PPConditionalInfo> conditionalStack;

  protected PreprocessorLexer(Preprocessor pp, FileID fid) {
    this.pp = pp;
    this.fid = fid;
    parsingFilename = false;
    parsingPreprocessorDirective = false;
    lexingRawMode = false;
    miOpt = new MultipleIncludeOpt();
    conditionalStack = new Stack<>();
  }

  protected PreprocessorLexer() {
    pp = null;
    parsingPreprocessorDirective = false;
    parsingFilename = false;
    lexingRawMode = false;
    conditionalStack = new Stack<>();
    miOpt = new MultipleIncludeOpt();
  }

  protected abstract void indirectLex(Token result);

  /**
   * Return the source location for the next observable
   * location.
   *
   * @return
   */
  protected abstract SourceLocation getSourceLocation();

  protected void pushConditionalLevel(SourceLocation directiveStart,
                                      boolean wasSkipping, boolean foundNonSkip, boolean foundElse) {
    PPConditionalInfo ci = new PPConditionalInfo();
    ci.ifLoc = directiveStart;
    ci.wasSkipping = wasSkipping;
    ci.foundNonSkip = foundNonSkip;
    ci.foundElse = foundElse;
    conditionalStack.push(ci);
  }

  protected void pushConditionalLevel(PPConditionalInfo ci) {
    conditionalStack.push(ci);
  }

  protected PPConditionalInfo popConditionalLevel() {
    if (conditionalStack.isEmpty())
      return null;
    return conditionalStack.pop();
  }

  protected PPConditionalInfo peekConditionalLevel() {
    Util.assertion(!conditionalStack.isEmpty(), "No conditionals active!");
    return conditionalStack.peek();
  }

  protected int getConditionalStackDepth() {
    return conditionalStack.size();
  }

  /**
   * After the preprocessor has parsed a #include, lex and
   * /// (potentially) macro expand the filename.  If the sequence parsed is not
   * /// lexically legal, emit a diagnostic and return a result EOM token.
   *
   * @return
   */
  public void lexIncludeFilename(Token result) {
    Util.assertion(parsingPreprocessorDirective && !parsingFilename, "Must be in a preprocessing directive!");


    parsingFilename = true;

    indirectLex(result);
    parsingFilename = false;

    if (result.is(TokenKind.Eod))
      pp.diag(result.getLocation(), err_pp_expects_filename).emit();
  }

  public boolean isLexingRawMode() {
    return lexingRawMode;
  }

  public Preprocessor getPP() {
    return pp;
  }

  public FileID getFileID() {
    Util.assertion(pp != null, "PreprocessorLexer::getFileID() should only be used with a Preprocessor");

    return fid;
  }

  /**
   * Return the FileEntry corresponding to this FileID.  Like
   * /// getFileID(), this only works for lexers with attached preprocessors.
   *
   * @return
   */
  public Path getFileEntry() {
    return pp.getSourceManager().getFileEntryForID(getFileID());
  }
}
