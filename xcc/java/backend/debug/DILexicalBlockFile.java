/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 * This software is subjected to the protection of BSD 3.0 Licence.
 * For more details, please refers to the LICENSE file.
 */

package backend.debug;

import backend.value.MDNode;
import tools.Util;

/**
 * This is a wrapper for a lexical block with a filename change.
 */
public class DILexicalBlockFile extends DIScope {
    public DILexicalBlockFile(MDNode n) {
        super(n);
    }
    public DILexicalBlockFile() { super(null);}
    public DIScope getContext() { return getScope().getContext(); }
    public int getLineNumber() { return getScope().getLineNumber(); }
    public int getColumnNumber() { return getScope().getColumnNumber(); }
    public String getDirectory() {
        String dir = new DIFile(getDescriptorField(2).getDbgNode()).getDirectory();
        return dir != null && !dir.isEmpty() ? dir : getContext().getDirectory();
    }

    @Override
    public String getFilename() {
        String filename = new DIFile(getDescriptorField(2).getDbgNode()).getFilename();
        Util.assertion(filename != null && !filename.isEmpty(), "a file with empty filename?");
        return filename;
    }

    public DILexicalBlock getScope() {
        return new DILexicalBlock(getDescriptorField(1).getDbgNode());
    }
}
