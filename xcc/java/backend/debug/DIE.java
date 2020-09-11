/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 * This software is subjected to the protection of BSD 3.0 Licence.
 * For more details, please refers to the LICENSE file.
 */

package backend.debug;

import backend.debug.DIEValue.DIEInteger;
import tools.Util;

import java.io.PrintStream;
import java.util.ArrayList;

public class DIE {
    /**
     * Buffer for constructing abbreviation.
     */
    protected DIEAbbrev abbrev;
    /**
     * Offset in debug information section.
     */
    protected int offset;
    /**
     * Size of instance + children
     */
    protected int size;
    /**
     * Children DIEs.
     */
    protected ArrayList<DIE> children;
    /**
     * Parent of this DIE.
     */
    protected DIE parent;
    /**
     * Attributes values.
     */
    protected ArrayList<DIEValue> values;
    /**
     * for print()
     */
    protected int indentCount;

    public DIE(int tag) {
        abbrev = new DIEAbbrev(tag, Dwarf.DW_CHILDREN_no);
        offset = 0;
        size = 0;
        parent = new DIE(0);
        indentCount = 0;
    }

    public DIEAbbrev getAbbrev() { return abbrev; }
    public int getAbbrevNumber() { return abbrev.getNumber(); }
    public int getTag() { return abbrev.getTag(); }
    public int getNumber() { return abbrev.getNumber(); }
    public int getOffset() { return offset; }
    public int getSize() { return size; }
    public ArrayList<DIE> getChildren() { return children; }
    public ArrayList<DIEValue> getValues() { return values; }
    public DIE getParent() { return parent; }
    public void setTag(int t) { abbrev.setTag(t);}
    public void setOffset(int offset) { this.offset = offset; }
    public void setSize(int size) { this.size = size; }
    public void addValue(int attribute, int form, DIEValue value) {
        abbrev.addAttribute(attribute, form);
        values.add(value);
    }
    public int getSiblingOffset() { return offset + size; }
    public DIEValue addSiblingOffset() {
        DIEInteger di = new DIEInteger(0);
        values.add(0, di);
        abbrev.addFirstAttribute(Dwarf.DW_AT_sibling, Dwarf.DW_FORM_ref4);
        return di;
    }

    public void addChild(DIE child) {
        if (child.getParent() != null) {
            Util.assertion(child.getParent() == this, "unexpected DIE parent!");
            return;
        }
        abbrev.setChildrenFlag(Dwarf.DW_CHILDREN_yes);
        children.add(child);
        child.parent = this;
    }
    public void print(PrintStream os, int incIndent) {
        indentCount += incIndent;
        String indent = Util.fixedLengthString(indentCount, ' ');
        boolean isBlock = abbrev.getTag() == 0;
        if (!isBlock) {
            os.printf("%sDie: 0x%x, Offset: %d, Size: %d\n",
                    indent,
                    hashCode(),
                    offset,
                    size);
            os.printf("%s%s %s", indent, Dwarf.tagString(abbrev.getTag()),
                    Dwarf.childrenString(abbrev.getChildrenFlag()));
        } else {
            os.printf("Size: %d\n", size);
        }
    }
    public void print(PrintStream os) { print(os, 0); }
    public void dump() {
        print(System.err);
        System.err.println();
    }
}
