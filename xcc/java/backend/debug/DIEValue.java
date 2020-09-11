/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 * This software is subjected to the protection of BSD 3.0 Licence.
 * For more details, please refers to the LICENSE file.
 */

package backend.debug;

import backend.codegen.AsmPrinter;
import backend.mc.MCAsmInfo;
import backend.mc.MCSymbol;
import tools.Util;

import java.io.PrintStream;

public interface DIEValue {
    enum DIEValueType {
        isInteger,
        isString,
        isLabel,
        isSectionOffset,
        isDelta,
        isEntry,
        isBlock
    }

    DIEValueType getType();
    void emitValue(AsmPrinter printer, int form);
    int sizeOf(AsmPrinter printer, int form);
    void print(PrintStream os);
    default void dump() {
        print(System.err);
        System.err.println();
    }

    class DIEInteger implements DIEValue {
        private long integer;

        public DIEInteger(long i) {
            integer = i;
        }
        @Override
        public DIEValueType getType() { return DIEValueType.isInteger; }

        @Override
        public void emitValue(AsmPrinter printer, int form) {
            int size = ~0;
            switch (form) {
                case Dwarf.DW_FORM_flag:
                case Dwarf.DW_FORM_ref1:
                case Dwarf.DW_FORM_data1: size = 1; break;
                case Dwarf.DW_FORM_ref2:
                case Dwarf.DW_FORM_data2: size = 2; break;
                case Dwarf.DW_FORM_ref4:
                case Dwarf.DW_FORM_data4: size = 4; break;
                case Dwarf.DW_FORM_ref8:
                case Dwarf.DW_FORM_data8: size = 8; break;
                case Dwarf.DW_FORM_udata: printer.emitULEB128(integer); return;
                case Dwarf.DW_FORM_sdata: printer.emitSLEB128(integer); return;
                case Dwarf.DW_FORM_addr: size = printer.getTargetData().getPointerSize(); break;
                default:
                    Util.shouldNotReachHere("DIE Value form not supported yet!");
            }
            printer.outStreamer.emitIntValue(integer,size, 0);
        }

        @Override
        public int sizeOf(AsmPrinter printer, int form) {
            switch (form) {
                case Dwarf.DW_FORM_flag:  // Fall thru
                case Dwarf.DW_FORM_ref1:  // Fall thru
                case Dwarf.DW_FORM_data1: return 1;
                case Dwarf.DW_FORM_ref2:  // Fall thru
                case Dwarf.DW_FORM_data2: return 2;
                case Dwarf.DW_FORM_ref4:  // Fall thru
                case Dwarf.DW_FORM_data4: return 4;
                case Dwarf.DW_FORM_ref8:  // Fall thru
                case Dwarf.DW_FORM_data8: return 8;
                case Dwarf.DW_FORM_udata: return MCAsmInfo.getSLEB128Size(integer);
                case Dwarf.DW_FORM_sdata: return MCAsmInfo.getULEB128Size(integer);
                case Dwarf.DW_FORM_addr:  return printer.getTargetData().getPointerSize();
                default: Util.shouldNotReachHere("DIE Value form not supported yet"); break;
            }
            return 0;
        }

        @Override
        public void print(PrintStream os) {
            os.printf("Int: %d 0x%x", integer, integer);
        }

        /**
         * Choose the best form for the integer.
         * @param isSigned
         * @param i
         * @return
         */
        public static int bestForm(boolean isSigned, long i) {
            if(isSigned) {
                if ((byte)i == (int)i) return Dwarf.DW_FORM_data1;
                if ((short)i == (int)i) return Dwarf.DW_FORM_data2;
                if ((int)i == (int)i) return Dwarf.DW_FORM_data4;
            } else {
                if (Byte.toUnsignedLong((byte)(i&0xff)) == i)
                    return Dwarf.DW_FORM_data1;
                if (Short.toUnsignedLong((short)(i&0xffff)) == i)
                    return Dwarf.DW_FORM_data2;
                if (Integer.toUnsignedLong((int)i) == i)
                    return Dwarf.DW_FORM_data4;
            }
            return Dwarf.DW_FORM_data8;
        }
    }

    class DIEString implements DIEValue {
        private String str;
        public DIEString(String s) { str = s; }
        @Override
        public DIEValueType getType() { return DIEValueType.isString; }

        @Override
        public void emitValue(AsmPrinter printer, int form) {
            printer.outStreamer.emitBytes(str, 0);
            printer.outStreamer.emitIntValue(0, 1, 0);
        }

        @Override
        public int sizeOf(AsmPrinter printer, int form) {
            return str.length() + 1; // extra '0' in the end of string.
        }

        @Override
        public void print(PrintStream os) {
            os.printf("Str: \"%s\"", str);
        }
    }

    class DIELabel implements DIEValue {
        private MCSymbol label;
        public DIELabel(MCSymbol label) { this.label = label; }
        @Override
        public DIEValueType getType() { return DIEValueType.isLabel; }

        public MCSymbol getValue() { return label; }
        @Override
        public void emitValue(AsmPrinter printer, int form) {
            printer.outStreamer.emitSymbolValue(label, sizeOf(printer, form));
        }

        @Override
        public int sizeOf(AsmPrinter printer, int form) {
            if (form == Dwarf.DW_FORM_data4) return 4;
            return printer.getTargetData().getPointerSize();
        }

        @Override
        public void print(PrintStream os) {
            os.printf("Lbl: %s", label.getName());
        }
    }

    class DIEDelta implements DIEValue {
        private MCSymbol labelHI;
        private MCSymbol labelLO;
        public DIEDelta(MCSymbol hi, MCSymbol lo) {
            labelHI = hi;
            labelLO = lo;
        }
        @Override
        public DIEValueType getType() { return DIEValueType.isDelta; }

        @Override
        public void emitValue(AsmPrinter printer, int form) {
            printer.emitLabelDifference(labelHI, labelLO, sizeOf(printer, form));
        }

        @Override
        public int sizeOf(AsmPrinter printer, int form) {
            if (form == Dwarf.DW_FORM_data4) return 4;
            return printer.getTargetData().getPointerSize();
        }

        @Override
        public void print(PrintStream os) {
            os.printf("Del: %s-%s", labelHI.getName(), labelLO.getName());
        }
    }

    class DIEEntry implements DIEValue {
        private DIE entry;
        public DIEEntry(DIE e) { entry = e; }

        public DIE getEntry() { return entry; }

        @Override
        public DIEValueType getType() { return DIEValueType.isEntry; }

        @Override
        public void emitValue(AsmPrinter printer, int form) {
            printer.emitInt32(entry.getOffset());
        }

        @Override
        public int sizeOf(AsmPrinter printer, int form) {
            return 4;
        }

        @Override
        public void print(PrintStream os) {
            os.printf("Die: 0x%x", entry.hashCode());
        }
    }

    class DIEBlock extends DIE implements DIEValue {
        private int size;
        public DIEBlock() {
            super(0);
            size = 0;
        }

        public int computeSize(AsmPrinter printer) {
            if (size == 0) {
                int i = 0;
                for (DIEValue val : values) {
                    size += val.sizeOf(printer, abbrev.getData().get(i++).getForm());
                }
            }
            return size;
        }
        public int bestForm() {
            if (Byte.toUnsignedLong((byte)(size&0xff)) == size)
                return Dwarf.DW_FORM_block1;
            if (Short.toUnsignedLong((short)(size&0xffff)) == size)
                return Dwarf.DW_FORM_block2;
            if (Integer.toUnsignedLong((int)(size)) == size)
                return Dwarf.DW_FORM_block4;
            return Dwarf.DW_FORM_block;
        }
        @Override
        public DIEValueType getType() { return DIEValueType.isBlock; }

        @Override
        public void emitValue(AsmPrinter printer, int form) {
            switch (form) {
                default:
                    Util.shouldNotReachHere("Improper form for DIEBlock");
                case Dwarf.DW_FORM_block1:
                    printer.emitInt8(size);
                    break;
                case Dwarf.DW_FORM_block2:
                    printer.emitInt16(size);
                    break;
                case Dwarf.DW_FORM_block4:
                    printer.emitInt32(size);
                    break;
                case Dwarf.DW_FORM_block:
                    printer.emitULEB128(size);
                    break;
            }
            int i = 0;
            for (DIEValue val : values)
                val.emitValue(printer, abbrev.getData().get(i++).getForm());
        }

        @Override
        public int sizeOf(AsmPrinter printer, int form) {
            switch (form) {
                default:
                    Util.shouldNotReachHere("Improper form for DIEBlock");
                case Dwarf.DW_FORM_block1: return size + 1;
                case Dwarf.DW_FORM_block2: return size + 2;
                case Dwarf.DW_FORM_block4: return size + 4;
                case Dwarf.DW_FORM_block:  return size + MCAsmInfo.getULEB128Size(size);
            }
        }

        @Override
        public void print(PrintStream os) {
            os.print("Blk: ");
            super.print(os, 5);
        }
    }
}
