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

import backend.support.Dwarf;
import backend.value.*;

import java.io.PrintStream;

import static backend.support.Dwarf.*;

/**
 * A thin wrapper around the MDNode to access encoded debug info.
 * @author Jianping Zeng.
 * @version 0.4
 */
public class DIDescriptor {
  protected MDNode dbgNode;
  protected String getStringField(int elt) {
    if (dbgNode == null) return "";

    if (elt < dbgNode.getNumOperands()) {
      if (dbgNode.getOperand(elt) instanceof MDString)
        return ((MDString)dbgNode.getOperand(elt)).getString();
    }
    return "";
  }
  int getUnsignedField(int elt) {
    return (int)getInt64Field(elt);
  }
  long getInt64Field(int elt) {
    if (dbgNode == null) return 0;

    if (elt < dbgNode.getNumOperands()) {
      if (dbgNode.getOperand(elt) instanceof ConstantInt)
        return ((ConstantInt)dbgNode.getOperand(elt)).getZExtValue();
    }
    return 0;
  }
  protected DIDescriptor getDescriptorField(int elt) {
    if (dbgNode == null) return new DIDescriptor();

    if (elt < dbgNode.getNumOperands()) {
      if (dbgNode.getOperand(elt) instanceof MDNode)
        return new DIDescriptor((MDNode)dbgNode.getOperand(elt));
    }
    return new DIDescriptor();
  }
  protected GlobalVariable getGlobalVariableField(int elt) {
    if (dbgNode == null) return null;

    if (elt < dbgNode.getNumOperands()) {
      if (dbgNode.getOperand(elt) instanceof GlobalVariable)
        return ((GlobalVariable)dbgNode.getOperand(elt));
    }
    return null;
  }
  protected Constant getConstantField(int elt) {
    if (dbgNode == null) return null;

    if (elt < dbgNode.getNumOperands()) {
      if (dbgNode.getOperand(elt) instanceof Constant)
        return ((Constant)dbgNode.getOperand(elt));
    }
    return null;
  }
  protected Function getFunctionField(int elt) {
    if (dbgNode == null) return null;

    if (elt < dbgNode.getNumOperands()) {
      if (dbgNode.getOperand(elt) instanceof Function)
        return ((Function)dbgNode.getOperand(elt));
    }
    return null;
  }

  public DIDescriptor() {dbgNode = null;}
  public DIDescriptor(MDNode n) { dbgNode = n; }
  public DIDescriptor(DIFile f) { dbgNode = f.dbgNode; }
  public DIDescriptor(DISubprogram f) { dbgNode = f.dbgNode; }
  public DIDescriptor(DILexicalBlock f) { dbgNode = f.dbgNode; }
  public DIDescriptor(DIVariable f) { dbgNode = f.dbgNode; }
  public DIDescriptor(DIType f) { dbgNode = f.getDbgNode(); }

  public boolean verify() { return dbgNode != null; }

  public MDNode getDbgNode() { return dbgNode; }
  public int getVersion() {
    return getUnsignedField(0) & Dwarf.LLVMDebugVersionMask;
  }
  public int getTag() {
    return getUnsignedField(0) & ~Dwarf.LLVMDebugVersionMask;
  }

  public void print(PrintStream os) {}

  public void dump() {
    print(System.err);
    System.err.println();
  }

  public boolean isDerivedType() {
    if (dbgNode == null) return false;
    switch (getTag()) {
      case DW_TAG_typedef:
      case DW_TAG_pointer_type:
      case DW_TAG_reference_type:
      case DW_TAG_const_type:
      case DW_TAG_volatile_type:
      case DW_TAG_restrict_type:
      case DW_TAG_member:
      case DW_TAG_inheritance:
      case DW_TAG_friend:
        return true;
      default:
        // CompositeTypes are currently modelled as DerivedTypes.
        return isCompositeType();
    }
  }
  public boolean isCompositeType() {
    if (dbgNode == null) return false;
    switch (getTag()) {
      case DW_TAG_array_type:
      case DW_TAG_structure_type:
      case DW_TAG_union_type:
      case DW_TAG_enumeration_type:
      case DW_TAG_vector_type:
      case DW_TAG_subroutine_type:
      case DW_TAG_class_type:
        return true;
      default:
        return false;
    }
  }
  public boolean isBasicType() {
    return dbgNode != null && getTag() == Dwarf.DW_TAG_base_type;
  }
  public boolean isVariable() {
    if (dbgNode == null) return false;
    switch (getTag()) {
      case DW_TAG_auto_variable:
      case DW_TAG_arg_variable:
      case DW_TAG_return_variable:
        return true;
      default:
        return false;
    }
  }
  public boolean isSubprogram() {
    return dbgNode != null && getTag() == Dwarf.DW_TAG_subprogram;
  }
  public boolean isGlobalVariable() {
    return dbgNode != null && (getTag() == Dwarf.DW_TAG_variable ||
        getTag() == Dwarf.DW_TAG_constant);
  }
  public boolean isScope() {
    if (dbgNode == null) return false;
    switch (getTag()) {
      case DW_TAG_compile_unit:
      case DW_TAG_lexical_block:
      case DW_TAG_subprogram:
      case DW_TAG_namespace:
        return true;
      default:
        break;
    }
    return false;
  }
  public boolean isFile() {
    return dbgNode != null && getTag() == Dwarf.DW_TAG_file_type;
  }
  public boolean isCompileUnit() {
    return dbgNode != null && getTag() == Dwarf.DW_TAG_compile_unit;
  }
  public boolean isNameSpace() {
    return dbgNode != null && getTag() == Dwarf.DW_TAG_namespace;
  }
  public boolean isLexicalBlock() {
    return dbgNode != null && getTag() == Dwarf.DW_TAG_lexical_block;
  }
  public boolean isSubrange() {
    return dbgNode != null && getTag() == Dwarf.DW_TAG_subrange_type;
  }
  public boolean isEnumerator() {
    return dbgNode != null && getTag() == Dwarf.DW_TAG_enumerator;
  }
  public boolean isType() {
    return isBasicType() || isCompositeType() || isDerivedType();
  }
  public boolean isGlobal() {
    return isGlobalVariable();
  }
}
