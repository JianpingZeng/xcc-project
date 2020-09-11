/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 * This software is subjected to the protection of BSD 3.0 Licence.
 * For more details, please refers to the LICENSE file.
 */

package backend.codegen;

import backend.debug.CompileUnit;

public class DwarfDebug {
    private AsmPrinter printer;
    private MachineModuleInfo mmi;
    private CompileUnit firstCU;

    public int getOrCreateSourceID(String filename, String directory) {
        return 0;
    }
}
