/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 * This software is subjected to the protection of BSD 3.0 Licence.
 * For more details, please refers to the LICENSE file.
 */

package backend.debug;

import backend.codegen.*;
import backend.debug.DIEValue.DIEBlock;
import backend.debug.DIEValue.DIEEntry;
import backend.debug.DIEValue.DIEInteger;
import backend.mc.MCSymbol;
import backend.target.TargetFrameLowering;
import backend.target.TargetRegisterInfo;
import backend.type.PointerType;
import backend.type.StructType;
import backend.value.*;
import tools.APFloat;
import tools.APInt;
import tools.OutRef;
import tools.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * This dwarf writer support class manages information associative with a source file.
 */
public class CompileUnit {
    /**
     * File identifier for source file.
     */
    private int id;
    /**
     * Compile unit debug information entry.
     */
    private DIE cuDIE;
    private AsmPrinter printer;
    private DwarfDebug dd;

    private DIE indexTyDie;
    private HashMap<MDNode, DIE> mdNodeToDIEMap;
    private HashMap<MDNode, DIEEntry> mdNodeToDIEEntryMap;
    private TreeMap<String, DIE> globals;
    private TreeMap<String, DIE> globalTypes;
    private ArrayList<DIEBlock> dieBlocks;
    private HashMap<DIE, MDNode> containingTypeMap;
    private DIEInteger dieIntegerOne;
    public CompileUnit(int i, DIE d, AsmPrinter ap, DwarfDebug dd) {
        this.id = i;
        cuDIE = d;
        printer = ap;
        this.dd = dd;
        indexTyDie = null;
        mdNodeToDIEMap = new HashMap<>();
        mdNodeToDIEEntryMap = new HashMap<>();
        globals = new TreeMap<>();
        globalTypes = new TreeMap<>();
        dieBlocks = new ArrayList<>();
        containingTypeMap = new HashMap<>();
        dieIntegerOne = new DIEInteger(1);
    }

    public DIEEntry createDIEEntry(DIE entry) {
        return new DIEEntry(entry);
    }

    public void addUInt(DIE die, int attribute,
                        int form, long integer) {
        if (form == 0) form = DIEInteger.bestForm(false, integer);
        DIEValue value = integer == 1 ? dieIntegerOne : new DIEInteger(integer);
        die.addValue(attribute, form, value);
    }
    public void addSInt(DIE die, int attribute,
                        int form, long integer) {
        if (form == 0) form = DIEInteger.bestForm(true, integer);
        DIEValue value = integer == 1 ? dieIntegerOne : new DIEInteger(integer);
        die.addValue(attribute, form, value);
    }
    public void addString(DIE die, int attribute,
                          int form, String string) {
        DIEValue value = new DIEValue.DIEString(string);
        die.addValue(attribute, form, value);
    }

    public void addDelta(DIE die, int attribute,
                         int form, MCSymbol hi, MCSymbol lo) {
        DIEValue value = new DIEValue.DIEDelta(hi, lo);
        die.addValue(attribute, form, value);
    }

    public void addDIE(DIE buffer) { this.cuDIE.addChild(buffer); }

    public void addDIEEntry(DIE die, int attribute,
                            int form, DIE entry) {
        die.addValue(attribute, form, createDIEEntry(entry));
    }

    public void addBlock(DIE die, int attribute,
                         int form, DIEBlock block) {
        block.computeSize(printer);
        dieBlocks.add(block);
        die.addValue(attribute, block.bestForm(), block);
    }

    public DIE getIndexTyDie() { return indexTyDie; }

    public void setIndexTyDie(DIE indexTyDie) { this.indexTyDie = indexTyDie; }

    public void addSourceLine(DIE die, DIVariable v) {
        if (!v.verify()) return;

        int line = v.getLineNumber();
        if (line == 0) return;
        int fileID = dd.getOrCreateSourceID(v.getContext().getFilename(),
                v.getContext().getDirectory());
        Util.assertion(fileID != 0, "invalid file id");
        addUInt(die, Dwarf.DW_AT_decl_file, 0, fileID);
        addUInt(die, Dwarf.DW_AT_decl_line, 0, line);
    }

    public void addSourceLine(DIE die, DIGlobalVariable g) {
        if (!g.verify()) return;

        int line = g.getLineNumber();
        if (line == 0) return;
        int fileID = dd.getOrCreateSourceID(g.getFilename(),
                g.getDirectory());
        Util.assertion(fileID != 0, "invalid file id");
        addUInt(die, Dwarf.DW_AT_decl_file, 0, fileID);
        addUInt(die, Dwarf.DW_AT_decl_line, 0, line);
    }

    public void addSourceLine(DIE die, DISubprogram subprogram) {
        if (!subprogram.verify()) return;

        int line = subprogram.getLineNumber();
        if (line == 0) return;
        if (!subprogram.getContext().verify()) return;

        int fileID = dd.getOrCreateSourceID(subprogram.getFilename(),
                subprogram.getDirectory());
        Util.assertion(fileID != 0, "invalid file id");
        addUInt(die, Dwarf.DW_AT_decl_file, 0, fileID);
        addUInt(die, Dwarf.DW_AT_decl_line, 0, line);
    }

    public void addSourceLine(DIE die, DIType type) {
        if (!type.verify()) return;

        int line = type.getLineNumber();
        if (line == 0 || !type.getContext().verify()) return;
        int fileID = dd.getOrCreateSourceID(type.getFilename(),
                type.getDirectory());
        Util.assertion(fileID != 0, "invalid file id");
        addUInt(die, Dwarf.DW_AT_decl_file, 0, fileID);
        addUInt(die, Dwarf.DW_AT_decl_line, 0, line);
    }

    public void addSourceLine(DIE die, DINameSpace ns) {
        if (!ns.verify()) return;

        int line = ns.getLineNumber();
        if (line == 0) return;
        int fileID = dd.getOrCreateSourceID(ns.getFilename(),
                ns.getDirectory());
        Util.assertion(fileID != 0, "invalid file id");
        addUInt(die, Dwarf.DW_AT_decl_file, 0, fileID);
        addUInt(die, Dwarf.DW_AT_decl_line, 0, line);
    }

    public void addVariableAddress(DbgVariable dv, DIE die,
                                   MachineLocation location) {
        if (dv.variableHasComplexAddress())
            addComplexAddress(dv, die, Dwarf.DW_AT_location, location);
        else if (dv.isBlockByrefVariable())
            addBlockByrefAddress(dv, die, Dwarf.DW_AT_location, location);
        else
            addAddress(die, Dwarf.DW_AT_location, location);
    }
    public void addComplexAddress(DbgVariable dv, DIE die,
                                  int attribute,
                                  MachineLocation location) {
        DIEBlock block = new DIEBlock();
        int n = dv.getNumAddrElements();
        int i = 0;
        if (location.isReg()) {
            if (n >= 2 && dv.getAddrElement(0) == DIBuilder.ComplexAddrKind.OpPlus.value) {
                addRegisterOffset(block, location.getReg(), dv.getAddrElement(1));
                i = 2;
            } else
                addRegisterOp(block, location.getReg());
        }
    }
    public void addRegisterOffset(DIE die, int reg, long offset) {
        TargetRegisterInfo tri = printer.tm.getSubtarget().getRegisterInfo();
        int dwReg = tri.getDwarfRegNum(reg, false);
        if (reg == tri.getFrameRegister(printer.mf))
            addUInt(die, 0, Dwarf.DW_FORM_data1, Dwarf.DW_OP_fbreg);
        else if (dwReg < 32)
            addUInt(die, 0, Dwarf.DW_FORM_data1, Dwarf.DW_OP_breg0 + dwReg);
        else {
            addUInt(die, 0, Dwarf.DW_FORM_data1, Dwarf.DW_OP_bregx);
            addUInt(die, 0, Dwarf.DW_FORM_udata, dwReg);
        }
        addSInt(die, 0, Dwarf.DW_FORM_sdata, offset);
    }

    public void addRegisterOp(DIE die, int reg) {
        TargetRegisterInfo tri = printer.tm.getSubtarget().getRegisterInfo();
        int dwReg = tri.getDwarfRegNum(reg, false);
        if (dwReg < 32)
            addUInt(die, 0, Dwarf.DW_FORM_data1, Dwarf.DW_OP_reg0 + dwReg);
        else {
            addUInt(die, 0, Dwarf.DW_FORM_data1, Dwarf.DW_OP_regx);
            addUInt(die, 0, Dwarf.DW_FORM_udata, dwReg);
        }
    }

    public void addAddress(DIE die, int attribute,
                           MachineLocation location) {
        DIEBlock block = new DIEBlock();
        if (location.isReg())
            addRegisterOp(block, location.getReg());
        else
            addRegisterOffset(block, location.getReg(), location.getOffset());

        addBlock(die, attribute, 0, block);
    }

    public void addBlockByrefAddress(DbgVariable dv, DIE die,
                                     int attribute,
                                     MachineLocation location) {
        DIType ty = dv.getType();
        DIType tmpTy = ty;
        int tag = ty.getTag();
        boolean isPointer = false;
        String varName = dv.getName();
        if (tag == Dwarf.DW_TAG_pointer_type) {
            DIDerivedType dty = new DIDerivedType(ty.getDbgNode());
            tmpTy = dty.getTypeDerivedFrom();
            isPointer = true;
        }

        DICompositeType blockStruct = new DICompositeType(tmpTy.getDbgNode());
        DIArray fields = blockStruct.getTypeArray();
        DIDescriptor varField = new DIDescriptor();
        DIDescriptor forwardingField = new DIDescriptor();
        for (int i = 0, e = fields.getNumElements(); i < e; ++i) {
            DIDescriptor elt = fields.getElement(i);
            DIDerivedType dt = new DIDerivedType(elt.getDbgNode());
            String fieldName = dt.getName();
            if (fieldName.equals("__forwarding"))
                forwardingField = elt;
            else if (fieldName.equals(varName))
                varField = elt;
        }
        long forwardingFieldOffset = new DIDerivedType(forwardingField.getDbgNode()).getOffsetInBits() >>> 3;
        long varFieldOffset = new DIDerivedType(varField.getDbgNode()).getOffsetInBits() >>> 3;
        TargetRegisterInfo ri = printer.tm.getSubtarget().getRegisterInfo();
        int reg = ri.getDwarfRegNum(location.getReg(), false);
        DIEBlock block = new DIEBlock();
        if (location.isReg()) {
            if (reg < 32)
                addUInt(block, 0, Dwarf.DW_FORM_data1, Dwarf.DW_OP_reg0 + reg);
            else {
                addUInt(block, 0, Dwarf.DW_FORM_data1, Dwarf.DW_OP_regx);
                addUInt(block, 0, Dwarf.DW_FORM_udata, reg);
            }
        } else {
            if (reg < 32)
                addUInt(block, 0, Dwarf.DW_FORM_data1, Dwarf.DW_OP_breg0 + reg);
            else {
                addUInt(block, 0, Dwarf.DW_FORM_data1, Dwarf.DW_OP_bregx);
                addUInt(block, 0, Dwarf.DW_FORM_udata, reg);
            }
            addUInt(block, 0, Dwarf.DW_FORM_sdata, location.getOffset());
        }

        if (isPointer)
            addUInt(block, 0, Dwarf.DW_FORM_data1, Dwarf.DW_OP_deref);
        if (forwardingFieldOffset > 0) {
            addUInt(block, 0, Dwarf.DW_FORM_data1, Dwarf.DW_OP_plus_uconst);
            addUInt(block, 0, Dwarf.DW_FORM_udata, forwardingFieldOffset);
        }

        addUInt(block, 0, Dwarf.DW_FORM_data1, Dwarf.DW_OP_deref);
        if (varFieldOffset > 0) {
            addUInt(block, 0, Dwarf.DW_FORM_data1, Dwarf.DW_OP_plus_uconst);
            addUInt(block, 0, Dwarf.DW_FORM_udata, varFieldOffset);
        }
        addBlock(die, attribute, 0, block);
    }

    public DIE getDIE(MDNode n) { return mdNodeToDIEMap.getOrDefault(n, null); }
    public void insertDIE(MDNode n, DIE die) {
        mdNodeToDIEMap.put(n, die);
    }
    public DIEEntry getDIEEntry(MDNode n) {
        return mdNodeToDIEEntryMap.getOrDefault(n, null);
    }
    public void insertDIEEntry(MDNode n, DIEEntry e) {
        mdNodeToDIEEntryMap.put(n, e);
    }
    public DIEBlock getDIEBlock() { return new DIEBlock(); }

    public DIE getOrCreateTypeDIE(MDNode tyNode) {
        DIType ty = new DIType(tyNode);
        if (!ty.verify()) return null;
        DIE tyDIE = getDIE(ty.getDbgNode());
        if (tyDIE != null) return tyDIE;

        // create an new type.
        tyDIE = new DIE(Dwarf.DW_TAG_base_type);
        insertDIE(ty.getDbgNode(), tyDIE);
        if (ty.isBasicType())
            constructTypeDIE(tyDIE, new DIBasicType(ty.getDbgNode()));
        else if (ty.isCompositeType())
            constructTypeDIE(tyDIE, new DICompositeType(ty.getDbgNode()));
        else {
            Util.assertion(ty.isDerivedType(), "Unknown kind of DIType!");
            constructTypeDIE(tyDIE, new DIDerivedType(ty.getDbgNode()));
        }
        addToContextOwner(tyDIE, ty.getContext());
        return tyDIE;
    }
    public void constructTypeDIE(DIE buffer, DIBasicType bty) {
        String name = bty.getName();
        if (name != null && !name.isEmpty())
            addString(buffer, Dwarf.DW_AT_name, Dwarf.DW_FORM_string, name);

        if (bty.getTag() == Dwarf.DW_TAG_unspecified_type) {
            buffer.setTag(Dwarf.DW_TAG_unspecified_type);
            return;
        }

        buffer.setTag(Dwarf.DW_TAG_base_type);
        addUInt(buffer, Dwarf.DW_AT_encoding, Dwarf.DW_FORM_data1, bty.getEncoding());
        long size = bty.getSizeInBits() >>> 3;
        addUInt(buffer, Dwarf.DW_AT_byte_size, 0, size);
    }

    public void constructTypeDIE(DIE buffer, DICompositeType cty) {
        String name = cty.getName();
        long size = cty.getSizeInBits() >>> 3;
        int tag = cty.getTag();
        buffer.setTag(tag);

        switch (tag) {
            case Dwarf.DW_TAG_vector_type:
            case Dwarf.DW_TAG_array_type:
                constructArrayTypeDIE(buffer, cty);
                break;
            case Dwarf.DW_TAG_enumeration_type: {
                DIArray elements = cty.getTypeArray();
                for (int i = 0, e = elements.getNumElements(); i < e; ++i) {
                    DIE eltDIE = null;
                    DIDescriptor enum_ = new DIDescriptor(elements.getElement(i).getDbgNode());
                    if (enum_.isEnumerator()) {
                        eltDIE = constructEnumTypeDIE(new DIEnumerator(enum_.getDbgNode()));
                        buffer.addChild(eltDIE);
                    }
                }
                break;
            }
            case Dwarf.DW_TAG_subroutine_type: {
                // return type
                DIArray elements = cty.getTypeArray();
                DIDescriptor rty = elements.getElement(0);
                addType(buffer, new DIType(rty.getDbgNode()));

                boolean isPrototyped  = true;
                // add arguments.
                for (int i = 1, e = elements.getNumElements(); i < e; ++i) {
                    DIDescriptor ty = elements.getElement(i);
                    if (ty.isUnspecifiedParameter()) {
                        DIE arg = new DIE(Dwarf.DW_TAG_unspecified_parameters);
                        buffer.addChild(arg);
                        isPrototyped = false;
                    } else {
                        DIE arg = new DIE(Dwarf.DW_TAG_formal_parameter);
                        addType(arg, new DIType(ty.getDbgNode()));
                        buffer.addChild(arg);
                    }
                }
                // add prototype flag
                if (isPrototyped)
                    addUInt(buffer, Dwarf.DW_AT_prototyped, Dwarf.DW_FORM_flag, 1);
                break;
            }
            case Dwarf.DW_TAG_structure_type:
            case Dwarf.DW_TAG_union_type:
            case Dwarf.DW_TAG_class_type: {
                // Add elements to structure type.
                DIArray elements = cty.getTypeArray();
                // a forward struct declared type may not have elements availabe.
                int n = elements.getNumElements();
                if (n == 0) break;
                for (int i = 0; i < n; ++i) {
                    DIDescriptor elt = elements.getElement(i);
                    DIE eltDIE = null;
                    if (elt.isSubprogram()) {
                        DISubprogram subprogram = new DISubprogram(elt.getDbgNode());
                        eltDIE = getOrCreateSubprogramDIE(new DISubprogram(elt.getDbgNode()));
                        if (subprogram.isProtected()) {
                            addUInt(eltDIE, Dwarf.DW_AT_accessibility, Dwarf.DW_FORM_flag,
                                    Dwarf.DW_ACCESS_protected);
                        } else if (subprogram.isPrivate())
                            addUInt(eltDIE, Dwarf.DW_AT_accessibility, Dwarf.DW_FORM_flag,
                                    Dwarf.DW_ACCESS_private);
                        else
                            addUInt(eltDIE, Dwarf.DW_AT_accessibility, Dwarf.DW_FORM_flag,
                                    Dwarf.DW_ACCESS_public);
                        if (subprogram.isExplicit())
                            addUInt(eltDIE, Dwarf.DW_AT_explicit, Dwarf.DW_FORM_flag, 1);
                    } else if (elt.isVariable()) {
                        DIVariable var = new DIVariable(elt.getDbgNode());
                        eltDIE = new DIE(Dwarf.DW_TAG_variable);
                        addString(eltDIE, Dwarf.DW_AT_name, Dwarf.DW_FORM_string, var.getName());
                        addType(eltDIE, var.getType());
                        addUInt(eltDIE, Dwarf.DW_AT_declaration, Dwarf.DW_FORM_flag, 1);
                        addUInt(eltDIE, Dwarf.DW_AT_external, Dwarf.DW_FORM_flag, 1);
                        addSourceLine(eltDIE, var);
                    } else if (elt.isDerivedType())
                        eltDIE = createMemberDIE(new DIDerivedType(elt.getDbgNode()));
                    else
                        continue;
                    buffer.addChild(eltDIE);
                }

                if (cty.isAppleBlockExtension())
                    addUInt(buffer, Dwarf.DW_AT_APPLE_block, Dwarf.DW_FORM_flag, 1);

                int rlang = cty.getRuntimeLang();
                if (rlang != 0)
                    addUInt(buffer, Dwarf.DW_AT_APPLE_runtime_class, Dwarf.DW_FORM_data1, rlang);

                DICompositeType containingType = cty.getContainingType();
                if (new DIDescriptor(containingType).isCompositeType()) {
                    addDIEEntry(buffer, Dwarf.DW_AT_containing_type,
                            Dwarf.DW_FORM_ref4, getOrCreateTypeDIE(containingType.getDbgNode()));
                }
                else {
                    addToContextOwner(buffer, cty.getContext());
                }
                if (cty.isObjcClassComplete())
                    addUInt(buffer, Dwarf.DW_AT_APPLE_objc_complete_type, Dwarf.DW_FORM_flag, 1);

                if (tag == Dwarf.DW_TAG_class_type)
                    addTemplateParams(buffer, cty.getTemplateParams());
                break;
            }
            default: break;
        }
        // Add name if not anonymous or intermediate type.
        if (name != null && !name.isEmpty())
            addString(buffer, Dwarf.DW_AT_name, Dwarf.DW_FORM_string, name);

        if (tag == Dwarf.DW_TAG_enumeration_type || tag == Dwarf.DW_TAG_class_type ||
                tag == Dwarf.DW_TAG_structure_type || tag == Dwarf.DW_TAG_union_type) {
            if (size != 0)
                addUInt(buffer, Dwarf.DW_AT_byte_size, 0, size);
            else {
                if (cty.isForwardDecl())
                    addUInt(buffer, Dwarf.DW_AT_declaration, Dwarf.DW_FORM_flag, 1);
                else
                    addUInt(buffer, Dwarf.DW_AT_byte_size, 0, 0);
            }
            // add source line information
            if (!cty.isForwardDecl())
                addSourceLine(buffer, cty);
        }
    }

    private DIE createMemberDIE(DIDerivedType dt) {
        DIE memberDIE = new DIE(dt.getTag());
        String name = dt.getName();
        if (name != null && !name.isEmpty())
            addString(memberDIE, Dwarf.DW_AT_name, Dwarf.DW_FORM_string, name);

        addType(memberDIE, dt.getTypeDerivedFrom());
        addSourceLine(memberDIE, dt);
        DIEBlock memLocationDIE = new DIEBlock();
        addUInt(memLocationDIE, 0, Dwarf.DW_FORM_data1, Dwarf.DW_OP_plus_uconst);

        long size = dt.getSizeInBits();
        long fieldSize = dt.getOriginalTypeSize();
        if (size != fieldSize) {
            // handle bitfield.
            addUInt(memberDIE, Dwarf.DW_AT_byte_size, 0, dt.getOriginalTypeSize()>>>3);
            addUInt(memberDIE, Dwarf.DW_AT_bit_size, 0, dt.getSizeInBits());
            long offset = dt.getOffsetInBits();
            long alignMask = -dt.getAlignInBits();
            long hiMark = (offset + fieldSize)&alignMask;
            long fieldOffset = hiMark - fieldSize;
            offset -= fieldOffset;

            if (printer.getTargetData().isLittleEndian())
                offset = fieldSize - (offset + size);

            addUInt(memberDIE, Dwarf.DW_AT_bit_offset, 0, offset);
            addUInt(memLocationDIE, 0, Dwarf.DW_FORM_udata, fieldOffset >>> 3);
        } else {
            // this is not a bitfield.
            addUInt(memLocationDIE, 0, Dwarf.DW_FORM_udata, dt.getOffsetInBits() >>> 3);
            // for c++, virtual base classes are not at fixed offset. Use following
            // expression to extract appropriate offset from vtable.
            // baseAddr = objAddr + *(*objAddr - offset)
            if (dt.getTag() == Dwarf.DW_TAG_inheritance && dt.isVirtual()) {
                DIEBlock vbaseLocatioinDIE = new DIEBlock();
                addUInt(vbaseLocatioinDIE, 0, Dwarf.DW_FORM_data1, Dwarf.DW_OP_dup);
                addUInt(vbaseLocatioinDIE, 0, Dwarf.DW_FORM_data1, Dwarf.DW_OP_deref);
                addUInt(vbaseLocatioinDIE, 0, Dwarf.DW_FORM_data1, Dwarf.DW_OP_constu);
                addUInt(vbaseLocatioinDIE, 0, Dwarf.DW_FORM_udata, dt.getOffsetInBits());
                addUInt(vbaseLocatioinDIE, 0, Dwarf.DW_FORM_data1, Dwarf.DW_OP_minus);
                addUInt(vbaseLocatioinDIE, 0, Dwarf.DW_FORM_data1, Dwarf.DW_OP_deref);
                addUInt(vbaseLocatioinDIE, 0, Dwarf.DW_FORM_data1, Dwarf.DW_OP_plus);
                addBlock(memberDIE, Dwarf.DW_AT_data_member_location, 0, vbaseLocatioinDIE);
            } else
                addBlock(memberDIE, Dwarf.DW_AT_data_member_location, 0, memLocationDIE);

            if (dt.isProtected())
                addUInt(memberDIE, Dwarf.DW_AT_accessibility, Dwarf.DW_FORM_flag,
                        Dwarf.DW_ACCESS_protected);
            else if (dt.isPrivate())
                addUInt(memberDIE, Dwarf.DW_AT_accessibility, Dwarf.DW_FORM_flag,
                        Dwarf.DW_ACCESS_private);
            else
                addUInt(memberDIE, Dwarf.DW_AT_accessibility, Dwarf.DW_FORM_flag,
                        Dwarf.DW_ACCESS_public);

            // objective-c properties.
            String propertyName = dt.getObjCPropertyName();
            if (propertyName != null && !propertyName.isEmpty()) {
                addString(memberDIE, Dwarf.DW_AT_APPLE_property_name,
                        Dwarf.DW_FORM_string, propertyName);
                String getterName = dt.getObjCPropertyGetterName();
                if (getterName != null && !getterName.isEmpty())
                    addString(memberDIE, Dwarf.DW_AT_APPLE_property_getter,
                            Dwarf.DW_FORM_string, getterName);
                String setterName = dt.getObjCPropertySetterName();
                if (setterName != null && !setterName.isEmpty())
                    addString(memberDIE, Dwarf.DW_AT_APPLE_property_setter,
                            Dwarf.DW_FORM_string, getterName);
                int propertyAttributes = 0;
                if (dt.isReadOnlyObjCProperty())
                    propertyAttributes |= Dwarf.DW_APPLE_PROPERTY_readonly;
                if (dt.isReadWriteObjCProperty())
                    propertyAttributes |= Dwarf.DW_APPLE_PROPERTY_readwrite;
                if (dt.isAssignObjCProperty())
                    propertyAttributes |= Dwarf.DW_APPLE_PROPERTY_assign;
                if (dt.isRetainObjCProperty())
                    propertyAttributes |= Dwarf.DW_APPLE_PROPERTY_retain;
                if (dt.isCopyObjCProperty())
                    propertyAttributes |= Dwarf.DW_APPLE_PROPERTY_copy;
                if (dt.isNonAtomicObjCProperty())
                    propertyAttributes |= Dwarf.DW_APPLE_PROPERTY_nonatomic;
                if (propertyAttributes != 0)
                    addUInt(memberDIE, Dwarf.DW_AT_APPLE_property_attribute, 0, propertyAttributes);
            }
        }
        return memberDIE;
    }

    public DIE constructEnumTypeDIE(DIEnumerator ety) {
        DIE enumerator = new DIE(Dwarf.DW_TAG_enumerator);
        String name = ety.getName();
        addString(enumerator, Dwarf.DW_AT_name, Dwarf.DW_FORM_string, name);
        long value = ety.getEnumValue();
        addSInt(enumerator, Dwarf.DW_AT_const_value, Dwarf.DW_FORM_sdata, value);
        return enumerator;
    }

    /**
     * If special LLVM prefix that is used to inform the asm printer to not emit
     * usual prefix before the symbol name is used then return linkage name after
     * skipping this special LLVM prefix.
     * @param linkageName
     * @return
     */
    private static String getRealLinkageName(String linkageName) {
        if (linkageName.startsWith("\1"))
            return linkageName.substring(1);
        return linkageName;
    }

    private DIE getOrCreateSubprogramDIE(DISubprogram sp) {
        DIE spDIE = getDIE(sp.getDbgNode());
        if (spDIE != null) return spDIE;

        spDIE = new DIE(Dwarf.DW_TAG_subprogram);
        insertDIE(sp.getDbgNode(), spDIE);

        // ad to context owner
        addToContextOwner(spDIE, sp.getContext());

        // add function template parameters.
        addTemplateParams(spDIE, sp.getTemplateParams());
        String linkageName = sp.getLinkageName();
        if (linkageName != null && !linkageName.isEmpty())
            addString(spDIE, Dwarf.DW_AT_MIPS_linkage_name,
                    Dwarf.DW_FORM_string, getRealLinkageName(linkageName));

        // If this DIE is going to refer declaration info using AT_specification
        // then there is no need to add other attributes.
        if (sp.getFunctionDeclaration().isSubrange())
            return spDIE;
        if (!sp.getName().isEmpty())
            addString(spDIE, Dwarf.DW_AT_name,
                    Dwarf.DW_FORM_string, sp.getName());
        addSourceLine(spDIE, sp);
        if (sp.isPrototyped())
            addUInt(spDIE, Dwarf.DW_AT_prototyped, Dwarf.DW_FORM_flag, 1);

        // add return type
        DICompositeType spTy = sp.getType();
        DIArray args = spTy.getTypeArray();
        int spTag = spTy.getTag();
        if (args.getNumElements() == 0 || spTag != Dwarf.DW_TAG_subroutine_type)
            addType(spDIE, spTy);
        else
            addType(spDIE, new DIType(args.getElement(0).getDbgNode()));

        int vk = sp.getVirtuality();
        if (vk != 0) {
            addUInt(spDIE, Dwarf.DW_AT_virtuality, Dwarf.DW_FORM_flag, vk);
            DIEBlock block = getDIEBlock();
            addUInt(block, 0, Dwarf.DW_FORM_data1, Dwarf.DW_OP_constu);
            addUInt(block, 0, Dwarf.DW_FORM_udata, sp.getVirtuality());
            addBlock(spDIE, Dwarf.DW_AT_vtable_elem_location, 0, block);
            containingTypeMap.put(spDIE, sp.getContainingType().getDbgNode());
        }

        if (!sp.isDefinition()) {
            addUInt(spDIE, Dwarf.DW_AT_declaration, Dwarf.DW_FORM_flag, 1);

            DICompositeType spty = sp.getType();
            args = spty.getTypeArray();
            spTag = spty.getTag();
            if(spTag == Dwarf.DW_TAG_subroutine_type) {
                for (int i = 1, n = args.getNumElements(); i < n; ++i) {
                    DIE arg = new DIE(Dwarf.DW_TAG_formal_parameter);
                    DIType aty = new DIType(args.getElement(i).getDbgNode());
                    addType(arg, aty);
                    if (aty.isArtifical())
                        addUInt(arg, Dwarf.DW_AT_artificial, Dwarf.DW_FORM_flag, 1);
                    spDIE.addChild(arg);
                }
            }
        }

        if (sp.isArtificial())
            addUInt(spDIE, Dwarf.DW_AT_artificial, Dwarf.DW_FORM_flag, 1);
        if (!sp.isLocalToUnit())
            addUInt(spDIE, Dwarf.DW_AT_external, Dwarf.DW_FORM_flag, 1);
        if (sp.isOptimized())
            addUInt(spDIE, Dwarf.DW_AT_APPLE_optimized, Dwarf.DW_FORM_flag, 1);
        int isa = printer.getISAEncoding();
        if (isa != 0)
            addUInt(spDIE, Dwarf.DW_AT_APPLE_isa, Dwarf.DW_FORM_flag, isa);
        return spDIE;
    }

    private void addTemplateParams(DIE buffer, DIArray tparams) {
        // add template parameters.
        for (int i = 0, e = tparams.getNumElements(); i < e; ++i) {
            DIDescriptor elt = tparams.getElement(i);
            if (elt.isTemplateTypeParameter()) {
                buffer.addChild(getOrCreateTemplateTypeParameterDIE(
                        new DITemplateTypeParameter(elt.getDbgNode())));
            } else if (elt.isTemplateValueParameter())
                buffer.addChild(getOrCreateTemplateValueParameterDIE(
                        new DITemplateValueParameter(elt.getDbgNode())));
        }
    }

    private DIE getOrCreateTemplateTypeParameterDIE(DITemplateTypeParameter tp) {
        DIE paramDIE = getDIE(tp.getDbgNode());
        if (paramDIE != null) return paramDIE;

        paramDIE = new DIE(Dwarf.DW_TAG_template_type_parameter);
        addType(paramDIE, tp.getType());
        addString(paramDIE, Dwarf.DW_AT_name, Dwarf.DW_FORM_string, tp.getName());
        return paramDIE;
    }
    private DIE getOrCreateTemplateValueParameterDIE(DITemplateValueParameter tvp) {
        DIE paramDIE = getDIE(tvp.getDbgNode());
        if (paramDIE != null) return paramDIE;

        paramDIE = new DIE(Dwarf.DW_TAG_template_value_parameter);
        addType(paramDIE, tvp.getType());
        if (tvp.getName() != null && !tvp.getName().isEmpty())
            addString(paramDIE, Dwarf.DW_AT_name, Dwarf.DW_FORM_string, tvp.getName());
        addUInt(paramDIE, Dwarf.DW_AT_const_value, Dwarf.DW_FORM_udata, tvp.getValue());
        return paramDIE;
    }

    public void constructArrayTypeDIE(DIE buffer, DICompositeType cty) {
        buffer.setTag(Dwarf.DW_TAG_array_type);
        if (cty.getTag() == Dwarf.DW_TAG_vector_type)
            addUInt(buffer, Dwarf.DW_AT_GNU_vector, Dwarf.DW_FORM_flag, 1);
        // emit derived type
        addType(buffer, cty.getTypeDerivedFrom());
        DIArray elements = cty.getTypeArray();
        DIE idxTy = getIndexTyDie();
        if (idxTy == null) {
            idxTy = new DIE(Dwarf.DW_TAG_base_type);
            addUInt(idxTy, Dwarf.DW_AT_byte_size,0 , 4);
            addUInt(idxTy, Dwarf.DW_AT_encoding, Dwarf.DW_FORM_data1, Dwarf.DW_ATE_signed);
            addDIE(idxTy);
            setIndexTyDie(idxTy);
        }
        // add subranges to array type.
        for (int i = 0, n = elements.getNumElements(); i < n; ++i) {
            DIDescriptor elt = elements.getElement(i);
            if (elt.getTag() == Dwarf.DW_TAG_subrange_type);
            constructSubrangeDIE(buffer, new DISubrange(elt.getDbgNode()), idxTy);
        }
    }

    private void constructSubrangeDIE(DIE buffer, DISubrange sr, DIE indexTy) {
        DIE subrange = new DIE(Dwarf.DW_TAG_subrange_type);
        addDIEEntry(subrange, Dwarf.DW_AT_type, Dwarf.DW_FORM_ref4, indexTy);
        long lo = sr.getLo(), hi = sr.getHi();
        if (lo > hi) {
            buffer.addChild(subrange);
            return;
        }
        if (lo != 0)
            addSInt(subrange, Dwarf.DW_AT_lower_bound, 0, lo);
        addSInt(subrange, Dwarf.DW_AT_upper_bound, 0, hi);
        buffer.addChild(subrange);
    }

    public void constructTypeDIE(DIE buffer, DIDerivedType dty) {
        String name = dty.getName();
        long size = dty.getSizeInBits() >>> 3;
        int tag = dty.getTag();
        if (tag == Dwarf.DW_TAG_inheritance)
            tag = Dwarf.DW_TAG_reference_type;

        buffer.setTag(tag);
        DIType fromTy = dty.getTypeDerivedFrom();
        addType(buffer, fromTy);

        if (name != null && !name.isEmpty())
            addString(buffer, Dwarf.DW_AT_name, Dwarf.DW_FORM_string, name);

        if (size != 0)
            addUInt(buffer, Dwarf.DW_AT_byte_size, 0, size);
        if (!dty.isForwardDecl())
            addSourceLine(buffer, dty);
    }

    public void addType(DIE die, DIType ty) {
        if (!ty.verify()) return;

        DIEEntry entry = getDIEEntry(ty.getDbgNode());
        if (entry != null) {
            die.addValue(Dwarf.DW_AT_type, Dwarf.DW_FORM_ref4, entry);
            return;
        }

        DIE buffer = getOrCreateTypeDIE(ty.dbgNode);
        entry = createDIEEntry(buffer);
        insertDIEEntry(ty.getDbgNode(), entry);
        die.addValue(Dwarf.DW_AT_type, Dwarf.DW_FORM_ref4, entry);
        addGlobalType(ty);
    }
    public void addGlobalType(DIType ty) {
        DIDescriptor context = ty.getContext();
        if (ty.isCompositeType() && !ty.getName().isEmpty() && !ty.isForwardDecl() &&
                (context == null || context.isCompileUnit() || context.isFile() ||
                        context.isNameSpace())) {
            DIEEntry entry = getDIEEntry(ty.getDbgNode());
            if (entry != null)
                globalTypes.put(ty.getName(), entry.getEntry());
        }
    }

    public void addToContextOwner(DIE die, DIDescriptor context) {
        if (context.isType()) {
            DIE contextDIE = getOrCreateTypeDIE(context.getDbgNode());
            contextDIE.addChild(die);
        }
    }

    public DIE getOrCreateNamespace(DINameSpace ns) {
        DIE die = getDIE(ns.dbgNode);
        if (die != null) return die;
        die = new DIE(Dwarf.DW_TAG_namespace);
        insertDIE(ns.getDbgNode(), die);
        if (ns.getName() != null && !ns.getName().isEmpty())
            addString(die, Dwarf.DW_AT_name, Dwarf.DW_FORM_string, ns.getName());
        addSourceLine(die, ns);
        addToContextOwner(die, ns.getContext());
        return die;
    }

    /**
     * Return constant expression if value is a GEP to access merged global constant.
     * e.g., i8* getelementptr ({i8, i8, i8, i8}* @_mergedGlobals, i32 0, i32 0)
     * @param val
     * @return
     */
    private static ConstantExpr getMergedGlobalExpr(Value val) {
        if (!(val instanceof ConstantExpr)) return null;
        ConstantExpr ce = (ConstantExpr) val;
        if (ce.getNumOfOperands() != 3 || ce.getOpcode() != Operator.GetElementPtr)
            return null;

        // first operand points to the global struct.
        Value ptr = ce.operand(0);
        if (!(ptr instanceof GlobalValue) || !(((PointerType)ptr.getType()).getElementType() instanceof StructType))
            return null;

        // second is zero
        Value op1 = ce.operand(1);
        if (!(op1 instanceof ConstantInt) || !((ConstantInt)op1).isZero())
            return null;

        // third one is offset.
        Value op2 = ce.operand(1);
        if (!(op2 instanceof ConstantInt))
            return null;
        return ce;
    }

    /**
     * Create a debug information entry DIE for global variable.
     * @param n
     */
    public void createGlobalVariableDIE(MDNode n) {
        if (getDIE(n) != null) return;
        ConstantExpr ce;
        DIGlobalVariable gv = new DIGlobalVariable(n);
        if (!gv.verify()) return;
        DIE varDIE = new DIE(gv.getTag());
        // add to map.
        insertDIE(n, varDIE);
        // add name
        addString(varDIE, Dwarf.DW_AT_name, Dwarf.DW_FORM_string, gv.getDisplayName());
        String linkageName = gv.getLinkageName();
        boolean isGlobalVariable = gv.getGlobal() != null;
        if (linkageName != null && !linkageName.isEmpty() && isGlobalVariable)
            addString(varDIE, Dwarf.DW_AT_MIPS_linkage_name,
                    Dwarf.DW_FORM_string, getRealLinkageName(linkageName));
        // add type
        DIType gty = gv.getType();
        addType(varDIE, gty);

        // add scoping information.
        if (!gv.isLocalToUnit()) {
            addUInt(varDIE, Dwarf.DW_AT_external, Dwarf.DW_FORM_flag, 1);
            // expose as global
            addGlobal(gv.getName(), varDIE);
        }
        // add line number
        addSourceLine(varDIE, gv);
        // add to context owner
        DIDescriptor gvCtx = gv.getContext();
        addToContextOwner(varDIE, gvCtx);
        // add location.
        if (isGlobalVariable) {
            DIEBlock block = new DIEBlock();
            addUInt(block, 0, Dwarf.DW_FORM_data1, Dwarf.DW_OP_addr);
            addLabel(block, 0, Dwarf.DW_FORM_udata, printer.mangler.getSymbol(gv.getGlobal()));
            if (gv.isDefinition() &&
                !gvCtx.isCompileUnit() &&
                !gvCtx.isFile() &&
                !isSubprogramContext(gvCtx.getDbgNode())) {
                // create specification DIE
                DIE variableSpecDIE = new DIE(Dwarf.DW_TAG_variable);
                addDIEEntry(variableSpecDIE, Dwarf.DW_AT_specification, Dwarf.DW_FORM_ref4, varDIE);
                addBlock(variableSpecDIE, Dwarf.DW_AT_location, 0, block);
                addUInt(varDIE, Dwarf.DW_AT_declaration, Dwarf.DW_FORM_flag, 1);
                addDIE(variableSpecDIE);
            } else
                addBlock(varDIE, Dwarf.DW_AT_location, 0, block);
        } else if (gv.getConstant() instanceof ConstantInt) {
            ConstantInt ci = (ConstantInt) gv.getConstant();
            addConstantValue(varDIE, ci, gty.isUnsignedDIType());
        } else if ((ce = getMergedGlobalExpr(n.operand(11))) != null) {
            // gv is a merged global.
            DIEBlock block = new DIEBlock();
            // pointer
            Value ptr = ce.operand(0);
            addUInt(block, 0, Dwarf.DW_FORM_data1, Dwarf.DW_OP_addr);
            addLabel(block, 0, Dwarf.DW_FORM_udata, printer.mangler.getSymbol((GlobalValue) ptr));
            addUInt(block, 0, Dwarf.DW_FORM_data1, Dwarf.DW_OP_constu);
            ArrayList<Value> idx = new ArrayList<>();
            for (int i = 1, e = ce.getNumOfOperands(); i < e; ++i)
                idx.add(ce.operand(i));
            addUInt(block, 0, Dwarf.DW_FORM_udata,
                    printer.getTargetData().getIndexedOffset(ptr.getType(), idx));
            addUInt(block, 0, Dwarf.DW_FORM_data1, Dwarf.DW_OP_plus);
            addBlock(varDIE, Dwarf.DW_AT_location, 0, block);
        }
    }

    public boolean addConstantValue(DIE die, ConstantInt ci,
                                 boolean isUnsigned) {
        int bitwidth = ci.getBitsWidth();
        if (bitwidth <= 64) {
            int form = 0;
            switch (bitwidth) {
                case 8: form = Dwarf.DW_FORM_data1; break;
                case 16: form = Dwarf.DW_FORM_data2; break;
                case 32: form = Dwarf.DW_FORM_data4; break;
                case 64: form = Dwarf.DW_FORM_data8; break;
                default:
                    form = isUnsigned ? Dwarf.DW_FORM_udata : Dwarf.DW_FORM_sdata;
            }
            if (isUnsigned)
                addUInt(die, Dwarf.DW_AT_const_value, form, ci.getZExtValue());
            else
                addSInt(die, Dwarf.DW_AT_const_value, form, ci.getSExtValue());
            return true;
        }
        DIEBlock block = new DIEBlock();
        APInt val = ci.getValue();
        long[] ptr = val.getRawData();
        int numBytes = val.getBitWidth()/8;
        boolean isLittleEndian = printer.getTargetData().isLittleEndian();
        for (int i = 0; i < numBytes; ++i) {
            byte c;
            if (isLittleEndian)
                c = (byte)(ptr[i/8] >>> (8*(i&7)));
            else
                c = (byte)(ptr[(numBytes - i - 1)/8] >>> (8*((numBytes - 1 - i)&7)));
            addUInt(block, 0, Dwarf.DW_FORM_data1, c);
        }
        addBlock(die, Dwarf.DW_AT_const_value, 0, block);
        return true;
    }

    private static boolean isTypeSigned(DIType ty, OutRef<Integer> sizeInBits) {
        if (ty.isDerivedType())
            return isTypeSigned(new DIDerivedType(ty.getDbgNode()).getTypeDerivedFrom(), sizeInBits);
        if (ty.isBasicType()) {
            DIBasicType bty = new DIBasicType(ty.getDbgNode());
            if (bty.getEncoding() == Dwarf.DW_ATE_signed ||
                    bty.getEncoding() == Dwarf.DW_ATE_signed_char) {
                sizeInBits.set((int) ty.getSizeInBits());
                return true;
            }
        }
        return false;
    }

    public boolean addConstantValue(DIE die, MachineOperand mo, DIType ty) {
        Util.assertion(mo.isImm(), "Invalid machine operand");
        DIEBlock block = new DIEBlock();
        OutRef<Integer> sizeInBits = new OutRef<>(-1);
        boolean signedConstant = isTypeSigned(ty, sizeInBits);
        int form = signedConstant ? Dwarf.DW_FORM_sdata :Dwarf.DW_FORM_udata;
        switch (sizeInBits.get()) {
            case 8: form = Dwarf.DW_FORM_data1; break;
            case 16: form = Dwarf.DW_FORM_data2; break;
            case 32: form = Dwarf.DW_FORM_data4; break;
            case 64: form = Dwarf.DW_FORM_data8; break;
            default: break;
        }
        if (signedConstant)
            addSInt(block, 0, form, mo.getImm());
        else
            addUInt(block, 0, form, mo.getImm());

        addBlock(die, Dwarf.DW_AT_const_value, 0, block);
        return true;
    }

    public boolean isSubprogramContext(MDNode context) {
        if (context == null) return false;
        DIDescriptor d = new DIDescriptor(context);
        if (d.isSubprogram()) return true;
        if (d.isType())
            return isSubprogramContext(new DIType(context).getContext().getDbgNode());
        return false;
    }

    public void addLabel(DIE die, int attribute,
                         int form, MCSymbol label) {
        DIEValue value = new DIEValue.DIELabel(label);
        die.addValue(attribute, form, value);
    }

    public void addGlobal(String name , DIE die) { globals.put(name, die); }

    public void addPubTypes(DISubprogram sp) {
        DICompositeType compTy = sp.getType();
        int spTag = compTy.getTag();
        if (spTag != Dwarf.DW_TAG_subroutine_type)
            return;
        DIArray args = compTy.getTypeArray();
        for (int i = 0, e = args.getNumElements(); i < e; ++i) {
            DIType aty = new DIType(args.getElement(i).getDbgNode());
            if (!aty.verify()) continue;
            addGlobalType(aty);
        }
    }
    public void constructContainingTypeDIEs() {
        for (Map.Entry<DIE, MDNode> entry : containingTypeMap.entrySet()) {
            DIE spDIE = entry.getKey();
            MDNode n = entry.getValue();
            if (n == null)continue;;
            DIE ndie = getDIE(n);
            if (ndie == null) continue;
            addDIEEntry(spDIE, Dwarf.DW_AT_containing_type, Dwarf.DW_FORM_ref4, ndie);
        }
    }

    /**
     * Construct a DIE for the given DbgVariable.
     * @param dv
     * @param isScopeAbstract
     * @return
     */
    public DIE constructVariableDIE(DbgVariable dv, boolean isScopeAbstract) {
        String name = dv.getName();
        if (name == null || name.isEmpty()) return null;
        int tag = dv.getTag();
        DIE variableDIE = new DIE(tag);
        DbgVariable absVar = dv.getAbstractVariable();
        DIE absDIE = absVar != null ? absVar.getDIE() : null;
        if (absDIE != null)
            addDIEEntry(variableDIE, Dwarf.DW_AT_abstract_origin, Dwarf.DW_FORM_ref4, absDIE);
        else {
            addString(variableDIE, Dwarf.DW_AT_name, Dwarf.DW_FORM_string, name);
            addSourceLine(variableDIE, dv.getVariable());
            addType(variableDIE, dv.getType());
        }
        if (dv.isArtificial()) {
            addUInt(variableDIE, Dwarf.DW_AT_artificial, Dwarf.DW_FORM_flag, 1);
        }

        if (isScopeAbstract) {
            dv.setDIE(variableDIE);
            return variableDIE;
        }

        // add variable address
        int offset = dv.getDotDebugLocOffset();
        if (offset != ~0) {
            addLabel(variableDIE, Dwarf.DW_AT_location, Dwarf.DW_FORM_data4,
                    printer.getTempSymbol("debug_loc", offset));
            dv.setDIE(variableDIE);
            return variableDIE;
        }

        // check if variable is described by a DBG_VALUE instruction.
        MachineInstr dvInst = dv.getMachineInstr();
        if (dvInst != null) {
            boolean updated = false;
            if (dvInst.getNumOperands() == 3) {
                if (dvInst.getOperand(0).isRegister()) {
                    MachineOperand regOp = dvInst.getOperand(0);
                    TargetRegisterInfo tri = printer.tm.getSubtarget().getRegisterInfo();
                    if (dvInst.getOperand(1).isImm() &&
                            tri.getFrameRegister(printer.mf) == regOp.getReg()) {
                        OutRef<Integer> frameReg = new OutRef<>(0);
                        TargetFrameLowering tfl = printer.tm.getSubtarget().getFrameLowering();
                        offset = tfl.getFrameIndexReference(printer.mf,
                                (int) dvInst.getOperand(1).getImm(),
                                frameReg);
                        MachineLocation location = new MachineLocation(frameReg.get(), offset);
                        addVariableAddress(dv, variableDIE, location);
                    } else if (regOp.getReg() != 0)
                        addVariableAddress(dv, variableDIE, new MachineLocation(regOp.getReg()));
                    updated = true;
                } else if (dvInst.getOperand(0).isImm()) {
                    updated = addConstantValue(variableDIE, dvInst.getOperand(0), dv.getType());
                } else if (dvInst.getOperand(0).isFPImm()) {
                    updated = addConstantFPValue(variableDIE, dvInst.getOperand(0));
                }
            } else {
                addVariableAddress(dv, variableDIE, printer.getDebugValueLocation(dvInst));
                updated = true;
            }
            if (!updated) {
                // if variableDIE is not updated then DBG_VALUE instruction doesn't contain valid info
                return null;
            }
            dv.setDIE(variableDIE);
            return variableDIE;
        } else {
            // else use frame index.
            int fi = dv.getFrameIndex();
            if (fi != ~0) {
                OutRef<Integer> frameReg = new OutRef<>(0);
                TargetFrameLowering tfl = printer.mf.getSubtarget().getFrameLowering();
                offset = tfl.getFrameIndexReference(printer.mf, fi, frameReg);
                MachineLocation location = new MachineLocation(frameReg.get(), offset);
                addVariableAddress(dv, variableDIE, location);
            }
        }
        dv.setDIE(variableDIE);
        return variableDIE;
    }

    /**
     * Add FP constant entry into variable DIE.
     * @param die
     * @param mo
     * @return
     */
    public boolean addConstantFPValue(DIE die, MachineOperand mo) {
        Util.assertion(mo.isFPImm(), "Invalid machine operand");
        DIEBlock block = new DIEBlock();
        APFloat fpImm = mo.getFPImm().getValueAPF();
        APInt fltVal = fpImm.bitcastToAPInt();
        byte[] fltPtr = fltVal.getRawByteData();

        int numBytes = fltVal.getBitWidth()/8;
        boolean littleEndian = printer.getTargetData().isLittleEndian();
        int incr = littleEndian ? 1 : -1;
        int start = littleEndian ? 0 : numBytes - 1;
        int stop = littleEndian ? numBytes : -1;

        // output the constant to DWARF one byte at a time.
        for (; start != stop; start += incr) {
            addUInt(block, 0, Dwarf.DW_FORM_data1, fltPtr[start]);
        }
        addBlock(die, Dwarf.DW_AT_const_value, 0, block);
        return true;
    }
}
