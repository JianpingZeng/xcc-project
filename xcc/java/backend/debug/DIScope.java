package backend.debug;
/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2018, Jianping Zeng.
 * All rights reserved.
 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import backend.support.Dwarf;
import backend.value.MDNode;
import tools.Util;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class DIScope extends DIDescriptor {
  public DIScope(MDNode n) { super(n); }
  public boolean isSubprogram() {
    return dbgNode != null && getTag() == Dwarf.DW_TAG_subprogram;
  }
  public String getFilename() {
    if (dbgNode == null) return "";
    if (isLexicalBlock())
      return new DILexicalBlock(dbgNode).getFilename();
    if (isSubprogram())
      return new DISubprogram(dbgNode).getFilename();
    if (isCompileUnit())
      return new DICompileUnit(dbgNode).getFilename();
    if (isNameSpace())
      return new DINameSpace(dbgNode).getFilename();
    if (isType())
      return new DIType(dbgNode).getFilename();
    if (isFile())
      return new DIFile(dbgNode).getFilename();
    Util.assertion("Invalid DIScope");
    return "";
  }
  public String getDirectory() {
    if (dbgNode == null) return "";
    if (isLexicalBlock())
      return new DILexicalBlock(dbgNode).getDirectory();
    if (isSubprogram())
      return new DISubprogram(dbgNode).getDirectory();
    if (isCompileUnit())
      return new DICompileUnit(dbgNode).getDirectory();
    if (isNameSpace())
      return new DINameSpace(dbgNode).getDirectory();
    if (isType())
      return new DIType(dbgNode).getDirectory();
    if (isFile())
      return new DIFile(dbgNode).getDirectory();
    Util.assertion("Invalid DIScope");
    return "";
  }
}
