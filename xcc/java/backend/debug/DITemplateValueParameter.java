/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 * This software is subjected to the protection of BSD 3.0 Licence.
 * For more details, please refers to the LICENSE file.
 */

package backend.debug;

import backend.value.MDNode;

public class DITemplateValueParameter extends DIDescriptor {
    public DITemplateValueParameter() { super();}
    public DITemplateValueParameter(MDNode n) { super(n);}
    public DIScope getContext() { return new DIScope(getDescriptorField(1).getDbgNode()); }
    public String getName() { return getStringField(2); }
    public DIType getType() { return new DIType(getDescriptorField(3).getDbgNode()); }
    public long getValue() { return getInt64Field(4); }
    public String getFilename() { return new DIFile(getDescriptorField(5).getDbgNode()).getFilename(); }
    public String getDirectory() {
        return new DIFile(getDescriptorField(5).getDbgNode()).getDirectory();
    }
    public int getLineNumber() { return getUnsignedField(6); }
    public int getColumnNumber() { return getUnsignedField(7);}
}
