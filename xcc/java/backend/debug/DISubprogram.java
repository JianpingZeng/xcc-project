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

import backend.value.Function;
import backend.value.MDNode;
import backend.value.Value;
import tools.Util;

import java.io.PrintStream;

import static backend.debug.Dwarf.LLVMDebugVersion8;

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

  public DIScope getContext() { return new DIScope(getDescriptorField(2).getDbgNode()); }
  public String getName() { return getStringField(3); }
  public String getDisplayName() { return getStringField(4); }
  public String getLinkageName() { return getStringField(5); }
  public DICompileUnit getCompileUnit() {
    if (getVersion() == Dwarf.LLVMDebugVersion7)
      return new DICompileUnit(getDescriptorField(6).getDbgNode());
    DIFile di = new DIFile(getDescriptorField(6).getDbgNode());
    return di.getCompileUnit();
  }
  public int getLineNumber() { return getUnsignedField(7); }
  public DICompositeType getType() { return new DICompositeType(getDescriptorField(8).getDbgNode()); }
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
  public DIArray getTemplateParams() { return new DIArray(getDescriptorField(17).getDbgNode()); }
  public DISubprogram getFunctionDeclaration() { return new DISubprogram(getDescriptorField(18).getDbgNode()); }
  public MDNode getVariableNodes() {
    if (dbgNode == null || dbgNode.getNumOfOperands() <= 19)
      return null;
    Value val = dbgNode.operand(19);
    MDNode n = val instanceof MDNode ? (MDNode)val : null;
    if (n != null)
      return n.operand(0) instanceof MDNode ? (MDNode) n.operand(0) : null;
    return null;
  }
  public DIArray getVariables() {
    if (dbgNode == null || dbgNode.getNumOfOperands() <= 19)
      return null;
    Value val = dbgNode.operand(19);
    MDNode n = val instanceof MDNode ? (MDNode)val : null;
    if (n != null && n.operand(0) instanceof MDNode)
      return new DIArray(((MDNode)n.operand(0)));
    return new DIArray();
  }

  public boolean isLocalToUnit() { return getUnsignedField(9) != 0; }
  public boolean isDefinition() { return getUnsignedField(10) != 0; }
  public int getVirtuality() { return getUnsignedField(11); }
  public int getVirtualIndex() { return getUnsignedField(12); }
  public DICompositeType getContainingType() { return new DICompositeType(getDescriptorField(13).getDbgNode()); }
  public boolean isArtificial() { return getUnsignedField(14) != 0; }
  public boolean isOptimized() {
    Util.assertion(dbgNode != null, "Invalid subprogram descriptor!");
    if (dbgNode.getNumOfOperands() == 16)
      return getUnsignedField(15) != 0;
    return false;
  }

  public boolean isPrivate() {
    if (getVersion() <= LLVMDebugVersion8)
      return false;
    return (getUnsignedField(14) & FlagPrivate) != 0;
  }

  public boolean isProtected() {
    if (getVersion() <= LLVMDebugVersion8)
      return false;
    return (getUnsignedField(14) & FlagProtected) != 0;
  }

  public boolean isExplicit() {
    if (getVersion() <= LLVMDebugVersion8)
      return false;
    return (getUnsignedField(14) & FlagExplicit) != 0;
  }
  public boolean isPrototyped() {
    if (getVersion() <= LLVMDebugVersion8)
      return false;
    return (getUnsignedField(14) & FlagPrototyped) != 0;
  }

  @Override
  public String getFilename() {
    if (getVersion() == Dwarf.LLVMDebugVersion7)
      return getCompileUnit().getFilename();

    DIFile f = new DIFile(getDescriptorField(6).getDbgNode());
    return f.getFilename();
  }

  @Override
  public String getDirectory() {
    if (getVersion() == Dwarf.LLVMDebugVersion7)
      return getCompileUnit().getDirectory();

    DIFile f = new DIFile(getDescriptorField(6).getDbgNode());
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
