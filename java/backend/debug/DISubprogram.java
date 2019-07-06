package backend.debug;
/*
 * Extremely C language Compiler
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
import backend.value.Function;
import backend.value.MDNode;
import tools.Util;

import java.io.PrintStream;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class DISubprogram extends DIScope {
  public DISubprogram(MDNode n) { super(n);}

  public boolean describes(Function function) {
    Util.assertion(function != null, "Invalid function!");
    if (function == getFunction())
      return true;

    String name = getLinkageName();
    return false;
  }

  public DIScope getContext() { return (DIScope) getDescriptorField(2); }
  public String getName() { return getStringField(3); }
  public String getDisplayName() { return getStringField(4); }
  public String getLinkageName() { return getStringField(5); }
  public DICompileUnit getCompileUnit() {
    if (getVersion() == Dwarf.LLVMDebugVersion7)
      return (DICompileUnit) getDescriptorField(6);
    DIFile di = (DIFile) getDescriptorField(6);
    return di.getCompileUnit();
  }
  public int getLineNumber() { return getUnsignedField(7); }
  public DICompositeType getType() { return (DICompositeType) getDescriptorField(8); }
  public String getReturnTypeName() {
    DICompositeType dct = getType();
    if (dct.verify()) {
      DIArray da = dct.getTypeArray();
      DIType t = new DIType(da.getElement(0).getDbgNode());
      return t.getName();
    }
    DIType t = getType();
    return t.getName();
  }

  public Function getFunction() {
    return getFunctionField(16);
  }

  public int isLocalToUnit() { return getUnsignedField(9); }
  public int isDefinition() { return getUnsignedField(10); }
  public int getVirtuality() { return getUnsignedField(11); }
  public int getVirtualIndex() { return getUnsignedField(12); }
  public DICompositeType getContainingType() { return (DICompositeType) getDescriptorField(13); }
  public int isArtificial() { return getUnsignedField(14); }
  public int isOptimized() {
    Util.assertion(dbgNode != null, "Invalid subprogram descriptor!");
    if (dbgNode.getNumOperands() == 16)
      return getUnsignedField(15);
    return 0;
  }

  @Override
  public String getFilename() {
    if (getVersion() == Dwarf.LLVMDebugVersion7)
      return getCompileUnit().getFilename();

    DIFile f = (DIFile) getDescriptorField(6);
    return f.getFilename();
  }

  @Override
  public String getDirectory() {
    if (getVersion() == Dwarf.LLVMDebugVersion7)
      return getCompileUnit().getDirectory();

    DIFile f = (DIFile) getDescriptorField(6);
    return f.getDirectory();
  }

  @Override
  public boolean verify() {
    if (dbgNode == null) return false;
    if (!getContext().verify()) return false;
    DICompileUnit cu = getCompileUnit();
    if (!cu.verify()) return false;
    DICompositeType ty = getType();
    if (!ty.verify()) return false;
    return true;
  }

  @Override
  public void print(PrintStream os) {  super.print(os); }
}
