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

import backend.value.Constant;
import backend.value.GlobalVariable;
import backend.value.MDNode;
import tools.Util;

import java.io.PrintStream;

import static backend.debug.Dwarf.*;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class DIGlobalVariable extends DIDescriptor {
    public DIGlobalVariable() { super();}
    public DIGlobalVariable(MDNode n) { super(n);}
    public DIScope getContext() { return new DIScope(getDescriptorField(2).getDbgNode()); }
    public String getName() { return getStringField(3); }
    public String getDisplayName() { return getStringField(4); }
    public String getLinkageName() { return getStringField(5); }
    public DICompileUnit getCompileUnit() {
        Util.assertion(getVersion() <= LLVMDebugVersion10, "Invalid getCompileUnit!");
        if (getVersion() == LLVMDebugVersion7)
            return new DICompileUnit(getDescriptorField(6).getDbgNode());
        DIFile f = new DIFile(getDescriptorField(6).getDbgNode());
        return f.getCompileUnit();
    }
    public String getFilename() {
        if (getVersion() <= LLVMDebugVersion10)
            getContext().getFilename();
        return new DIFile(getDescriptorField(6).getDbgNode()).getFilename();
    }

    public String getDirectory() {
        if (getVersion() <= LLVMDebugVersion10)
            getContext().getDirectory();
        return new DIFile(getDescriptorField(6).getDbgNode()).getDirectory();
    }

    public int getLineNumber() { return getUnsignedField(7); }
    public DIType getType() { return new DIType(getDescriptorField(8).getDbgNode()); }
    public boolean isLocalToUnit() { return getUnsignedField(9) != 0; }
    public boolean isDefinition() { return getUnsignedField(10) != 0; }
    public GlobalVariable getGlobal() { return getGlobalVariableField(11); }
    public Constant getConstant() { return getConstantField(11); }
    public boolean verify() {
        if (dbgNode == null) return false;
        if (getDisplayName() == null || getDisplayName().isEmpty())
            return false;
        if (getContext() != null && !getContext().verify())
            return false;
        DIType ty = getType();
        if (!ty.verify()) return false;
        if (getGlobal() == null && getConstant() == null)
            return false;
        return true;
    }

    @Override
    public void print(PrintStream os) {
        os.print(" [");
        String name = getName();
        if (name != null && !name.isEmpty())
            os.printf(" [%s] ", name);

        int tag = getTag();
        os.printf(" [%s] ", Dwarf.tagString(tag));
        os.printf(" [%d] ", getLineNumber());
        if (isLexicalBlock()) os.print(" [local] ");
        if (isDefinition()) os.print(" [def] ");
        if (isGlobalVariable())
            new DIGlobalVariable(dbgNode).print(os);

        os.println("]");
    }
}
