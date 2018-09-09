package jlang.diag;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this filename except in compliance with the License.
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

import jlang.basic.SourceManager;
import jlang.clex.StrData;
import jlang.support.FileID;
import jlang.support.MemoryBuffer;
import jlang.support.SourceLocation;
import tools.Pair;
import tools.Util;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public class FullSourceLoc extends SourceLocation {
  private SourceManager sourceMgr;

  public FullSourceLoc(SourceLocation loc, SourceManager sgr) {
    super(loc);
    sourceMgr = sgr;
  }

  public FullSourceLoc() {
    super();
  }

  public SourceManager getSourceMgr() {
    return sourceMgr;
  }

  public FileID getFileID() {
    Util.assertion(isValid());
    return sourceMgr.getFileID(this);
  }

  public FullSourceLoc getInstantiationLoc() {
    Util.assertion(isValid());
    return new FullSourceLoc(sourceMgr.getInstantiationLoc(this), sourceMgr);
  }

  public FullSourceLoc getSpellingLoc() {
    Util.assertion(isValid());
    return new FullSourceLoc(sourceMgr.getLiteralLoc(this), sourceMgr);
  }

  public int getInstantiationLineNumber() {
    Util.assertion(isValid());
    return sourceMgr.getInstantiationLineNumber(this);
  }

  public int getIntantiationColumnNumber() {
    Util.assertion(isValid());
    return sourceMgr.getInstantiationColumnNumber(this);
  }

  public int getSpellingLineNumber() {
    Util.assertion(isValid());
    return sourceMgr.getLiteralLineNumber(this);
  }

  public int getSpellingColumnNumber() {
    Util.assertion(isValid());
    return sourceMgr.getLiteralColumnNumber(this);
  }

  public boolean isInSystemHeader() {
    Util.assertion(isValid());
    return sourceMgr.isInSystemHeader(this);
  }

  public StrData getCharacterData() {
    Util.assertion(isValid());
    return sourceMgr.getCharacterData(this);
  }

  public MemoryBuffer getBuffer() {
    Util.assertion(isValid());
    return sourceMgr.getBuffer(sourceMgr.getFileID(this));
  }

  public char[] getBufferData() {
    Util.assertion(isValid());
    return getBuffer().getBuffer();
  }

  public Pair<FileID, Integer> getDecomposedLoc() {
    return sourceMgr.getDecomposedLoc(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null) return false;
    if (obj.getClass() != getClass())
      return false;
    FullSourceLoc loc = (FullSourceLoc) obj;
    return getRawEncoding() == loc.getRawEncoding() &&
        (sourceMgr == loc.sourceMgr || sourceMgr.equals(loc.sourceMgr));
  }

  @Override
  public int hashCode() {
    return getRawEncoding() << 11 + sourceMgr.hashCode();
  }
}
