/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 * This software is subjected to the protection of BSD 3.0 Licence.
 * For more details, please refers to the LICENSE file.
 */

package backend.debug;

import tools.FoldingSetNode;
import tools.FoldingSetNodeID;

public class DIEAbbrevData implements FoldingSetNode {
    private int attribute;
    private int form;
    public DIEAbbrevData(int a, int f) {
        attribute = a;
        form = f;
    }
    public int getAttribute() { return attribute; }
    public int getForm() { return form; }


    @Override
    public void profile(FoldingSetNodeID id) {
        id.addInteger(attribute);
        id.addInteger(form);
    }
}
