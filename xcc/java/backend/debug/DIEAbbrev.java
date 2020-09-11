/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 * This software is subjected to the protection of BSD 3.0 Licence.
 * For more details, please refers to the LICENSE file.
 */

package backend.debug;

import backend.codegen.AsmPrinter;
import tools.FoldingSetNode;
import tools.FoldingSetNodeID;

import java.io.PrintStream;
import java.util.ArrayList;

public class DIEAbbrev implements FoldingSetNode {
    /**
     * Dwarf tag code.
     */
    protected int tag;
    /**
     * Unique number for node.
     */
    protected int number;
    /**
     * Dwarf children flag.
     */
    protected int childrenFlag;

    /**
     * Raw data bytes for abbreviation.
     */
    protected ArrayList<DIEAbbrevData> data;

    public DIEAbbrev(int t, int c) {
        tag = t;
        childrenFlag = c;
        data = new ArrayList<>();
    }

    public int getTag() { return tag; }
    public int getNumber() { return number; }
    public int getChildrenFlag() { return childrenFlag; }
    public ArrayList<DIEAbbrevData> getData() { return data; }
    public void setTag(int tag) { this.tag = tag; }
    public void setNumber(int number) { this.number = number; }
    public void setChildrenFlag(int childrenFlag) { this.childrenFlag = childrenFlag; }
    public void addAttribute(int attribute, int form) {
        data.add(new DIEAbbrevData(attribute, form));
    }
    public void addFirstAttribute(int attribute, int form) {
        data.add(0, new DIEAbbrevData(attribute, form));
    }
    public void emit(AsmPrinter printer) {
        // emit its dwarf tag type
        printer.emitULEB128(tag, Dwarf.tagString(tag));
        printer.emitULEB128(childrenFlag, Dwarf.childrenString(childrenFlag));
        // for each attribute description
        for (DIEAbbrevData attrData : data) {
            // emit attribute type
            printer.emitULEB128(attrData.getAttribute(), Dwarf.attributeString(attrData.getAttribute()));
            // emit form type
            printer.emitULEB128(attrData.getForm(), Dwarf.formEncodingString(attrData.getForm()));
        }
        // mark end of abbreviation
        printer.emitULEB128(0, "EOM(1)");
        printer.emitULEB128(0, "EOM(2)");
    }
    public void print(PrintStream os) {
        os.printf("Abbreviation @%d %s %s\n",
                hashCode(),
                Dwarf.tagString(tag),
                Dwarf.childrenString(childrenFlag));
        data.forEach(e-> {
            os.printf(" %s %s\n", Dwarf.attributeString(e.getAttribute()),
                    Dwarf.formEncodingString(e.getForm()));
        });
    }
    public void dump() { print(System.err); System.err.println();}

    @Override
    public void profile(FoldingSetNodeID id) {
        id.addInteger(tag);
        id.addInteger(childrenFlag);
        data.forEach(e->e.profile(id));
    }
}
