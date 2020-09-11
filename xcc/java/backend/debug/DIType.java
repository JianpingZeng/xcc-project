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

import backend.value.MDNode;

import java.io.PrintStream;
import java.util.Objects;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class DIType extends DIScope {
  // a subroutine type. e.g. "this" in c++.
  protected DIType(MDNode dbgNode, boolean b1, boolean b2) {
    super(dbgNode);
  }

  public DIType(MDNode n) { this(n ,true, true); }
  public DIType() { this(null); }
  public DIScope getContext() { return new DIScope(getDescriptorField(1).getDbgNode()); }
  public String getName() { return getStringField(2); }
  public DICompileUnit getCompileUnit() {
    if (getVersion() == Dwarf.LLVMDebugVersion7)
      return new DICompileUnit(getDescriptorField(3).getDbgNode());
    DIFile f = new DIFile(getDescriptorField(3).getDbgNode());
    return f.getCompileUnit();
  }
  public int getLineNumber() { return getUnsignedField(4); }
  public long getSizeInBits() { return getInt64Field(5); }
  public long getAlignInBits() { return getInt64Field(6); }
  public long getOffsetInBits() { return getInt64Field(7); }
  public int getFlags() { return getUnsignedField(8); }
  public boolean isPrivate() {
    return (getFlags() & FlagPrivate) != 0;
  }
  public boolean isProtected() {
    return (getFlags() & FlagProtected) != 0;
  }
  public boolean isForwardDecl() {
    return (getFlags() & FlagFwdDecl) != 0;
  }
  public boolean isAppleBlockExtension() {
    return (getFlags() & FlagAppleBlock) != 0;
  }
  public boolean isBlockByrefStruct() {
    return (getFlags() & FlagBlockByrefStruct) != 0;
  }
  public boolean isVirtual() {
    return (getFlags() & FlagVirtual) != 0;
  }
  public boolean isArtifical() {
    return (getFlags() & FlagArtificial) != 0;
  }
  public boolean isValid() {
    return dbgNode != null && (isBasicType() || isDerivedType() || isCompositeType());
  }
  @Override
  public String getFilename() { return getCompileUnit().getFilename(); }

  @Override
  public String getDirectory() { return getCompileUnit().getDirectory(); }
  public void replaceAllUsesWith(DIDescriptor di) {
    if (dbgNode == null) return;
    if (!Objects.equals(dbgNode, di.getDbgNode())) {
      dbgNode.replaceAllUsesWith(di.getDbgNode());
    }
  }

  public boolean isObjcClassComplete() {
    return (getFlags() & FlagObjcClassComplete) != 0;
  }

  @Override
  public void print(PrintStream os) { super.print(os); }

  public boolean isUnsignedDIType() {
    DIDerivedType dty = new DIDerivedType(dbgNode);
    if (dty.verify())
      return dty.getTypeDerivedFrom().isUnsignedDIType();

    DIBasicType bty = new DIBasicType(dbgNode);
    if (bty.verify()) {
      int encoding = bty.getEncoding();
      if (encoding == Dwarf.DW_ATE_unsigned ||
              encoding == Dwarf.DW_ATE_unsigned_char)
        return true;
    }
    return false;
  }
}
