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
import backend.value.MDNode;

import java.io.PrintStream;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class DIDerivedType extends DIType {
  public DIDerivedType(MDNode dbgNode) {
    super(dbgNode, true, true);
  }
  public DIType getTypeDerivedFrom() { return (DIType) getDescriptorField(9); }

  public long getOriginalTypeSize() {
    int tag = getTag();
    switch (tag) {
      case Dwarf.DW_TAG_member:
      case Dwarf.DW_TAG_typedef:
      case Dwarf.DW_TAG_const_type:
      case Dwarf.DW_TAG_volatile_type:
      case Dwarf.DW_TAG_restrict_type:
        DIType baseType = getTypeDerivedFrom();
        if (!baseType.isValid())
          return getSizeInBits();
        if (baseType.isDerivedType())
          return new DIDerivedType(baseType.getDbgNode()).getOriginalTypeSize();
        else
          return baseType.getSizeInBits();
    }
    return getSizeInBits();
  }

  @Override
  public boolean verify() {
    return isDerivedType();
  }

  @Override
  public void print(PrintStream os) { super.print(os); }
}
