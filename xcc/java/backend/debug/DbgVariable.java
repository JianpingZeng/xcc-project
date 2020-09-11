/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 * This software is subjected to the protection of BSD 3.0 Licence.
 * For more details, please refers to the LICENSE file.
 */

package backend.debug;

import backend.codegen.MachineInstr;
import tools.Util;

/**
 * This class is used to track local variable information.
 */
public class DbgVariable {
    private DIVariable var;
    private DIE theDIE;
    private int dotDebugLocOffset;
    /**
     * corresponding abstract variable.
     */
    private DbgVariable absVar;
    private MachineInstr mi;
    private int frameIndex;

    public DbgVariable(DIVariable var, DbgVariable absVar) {
        this.var = var;
        this.absVar = absVar;
        theDIE = null;
        dotDebugLocOffset = ~0;
        mi = null;
        frameIndex = ~0;
    }

    public DIVariable getVariable() { return var; }
    public void setDIE(DIE theDIE) { this.theDIE = theDIE; }
    public DIE getDIE() { return theDIE; }
    public void setDotDebugLocOffset(int dotDebugLocOffset) {
        this.dotDebugLocOffset = dotDebugLocOffset;
    }
    public int getDotDebugLocOffset() {
        return dotDebugLocOffset;
    }
    public String getName() { return var.getName(); }
    public DbgVariable getAbstractVariable() { return absVar; }
    public MachineInstr getMachineInstr() { return mi; }
    public void setMachineInstr(MachineInstr inst) { mi = inst; }
    public int getFrameIndex() { return frameIndex; }
    public void setFrameIndex(int fi) { this.frameIndex = fi; }
    public int getTag() {
        return (var.getTag() == Dwarf.DW_TAG_arg_variable) ?
            Dwarf.DW_TAG_formal_parameter : Dwarf.DW_TAG_variable;
    }
    public DIType getType() {
        DIType ty = var.getType();
        if (var.isBlockByrefVariable()) {
            DIType subType = ty;
            int tag = ty.getTag();
            if (tag == Dwarf.DW_TAG_pointer_type) {
                DIDerivedType dty = new DIDerivedType(ty.getDbgNode());
                subType = dty.getTypeDerivedFrom();
            }

            DICompositeType blockStruct = new DICompositeType(subType.getDbgNode());
            DIArray elements = blockStruct.getTypeArray();
            for (int i = 0, e = elements.getNumElements(); i < e; ++i) {
                DIDescriptor elt = elements.getElement(i);
                DIDerivedType dt = new DIDerivedType(elt.getDbgNode());
                if (getName().equals(dt.getName()))
                    return dt.getTypeDerivedFrom();
            }
            return ty;
        }
        return ty;
    }

    public boolean isArtificial() {
        if (var.isArtificial())
            return true;
        return var.getTag() == Dwarf.DW_TAG_arg_variable && getType().isArtifical();
    }

    public boolean variableHasComplexAddress() {
        Util.assertion(var.verify(), "Invalid complex DbgVariable!");
        return var.hasComplexAddress();
    }
    public boolean isBlockByrefVariable() {
        Util.assertion(var.verify(), "Invalid complex DbgVariable!");
        return var.isBlockByrefVariable();
    }
    public int getNumAddrElements() {
        return var.getNumAddrElement();
    }
    public long getAddrElement(int idx) {
        return var.getAddrElement(idx);
    }
}
