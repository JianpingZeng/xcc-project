package backend.debug;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
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

import backend.value.Function;
import backend.value.MDNode;
import tools.Util;

import static backend.support.Dwarf.LLVMDebugVersion7;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class DIVariable extends DIDescriptor {
  public DIVariable(MDNode n) {
    super(n);
  }

  public DIScope getContext() { return new DIScope(getDescriptorField(1).getDbgNode()); }
  public String getName() { return getStringField(2); }
  public DICompileUnit getCompileUnit() {
    if (getVersion() == LLVMDebugVersion7)
      return new DICompileUnit(getDescriptorField(3).getDbgNode());
    DIFile f = new DIFile(getDescriptorField(3).getDbgNode());
    return f.getCompileUnit();
  }
  public int getLineNumber() { return getUnsignedField(4); }
  public DIType getType() { return new DIType(getDescriptorField(5).getDbgNode()); }
  public boolean verify() {
    if (dbgNode == null) return false;
    if (!getContext().verify()) return false;
    if (!getCompileUnit().verify()) return false;
    DIType ty = getType();
    return ty.verify();
  }
  public boolean hasComplexAddress() {
    return getNumAddrElement() > 0;
  }

  public int getNumAddrElement() {
    return dbgNode.getNumOfOperands() - 6;
  }

  public long getAddrElement(int idx) {
    return getInt64Field(idx + 6);
  }

  /**
   * Return true if the variable was declared as
   * a "__block" variable (Apple Blocks).
   * @return
   */
  public boolean isBlockByrefVariable() {
    return getType().isBlockByrefStruct();
  }

  public boolean isInlinedFnArgument(Function function) {
    Util.assertion(function != null, "Invalid function");
    if (!getContext().isSubprogram())
      return false;
    // This variable is not inlined function argument if its scope
    // does not describe current function.
    return !new DISubprogram(getContext().getDbgNode()).describes(function);
  }
}
