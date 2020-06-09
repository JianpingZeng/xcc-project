package backend.target.arm;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 * This software is subjected to the protection of BSD 3.0 Licence.
 * For more details, please refers to the LICENSE file.
 */
import backend.codegen.*;

import static backend.target.arm.ARMGenRegisterNames.*;
import static backend.target.arm.ARMGenRegisterNames.R1;

class CC_ARM_APCS_Custom_f64 {
    static boolean apply(int valNo,
                         EVT valVT,
                         EVT locVT,
                         CCValAssign.LocInfo locInfo,
                         ArgFlagsTy argFlags,
                         CCState state) {
        if (!f64AssignAPCS(valNo, valVT, locVT, locInfo, state, true))
            return false;
        if (locVT.equals(new EVT(MVT.v2f64)) &&
                !f64AssignAPCS(valNo, valVT, locVT, locInfo, state, false))
            return false;
        return true;
    }

    private static boolean f64AssignAPCS(int valNo, EVT valVT, EVT locVT,
                                         CCValAssign.LocInfo locInfo,
                                         CCState state, boolean canFail) {
        int[] regList = new int[]{R0, R2, R1, R3};

        // get first register
        int reg = state.allocateReg(regList);
        if (reg != 0) {
            state.addLoc(CCValAssign.getCustomReg(valNo, valVT, reg, locVT, locInfo));
        } else {
            if (canFail) return false;
            // put whole thing into local stack slot.
            state.addLoc(CCValAssign.getCustomMem(valNo, valVT, state.allocateStack(8, 4),
                    locVT, locInfo));
            return true;
        }
        // get second register
        reg = state.allocateReg(regList);
        if (reg != 0) {
            state.addLoc(CCValAssign.getCustomReg(valNo, valVT, reg, locVT, locInfo));
        } else {
            // put second part into local stack slot.
            state.addLoc(CCValAssign.getCustomMem(valNo, valVT, state.allocateStack(4, 4),
                    locVT, locInfo));
        }
        return true;
    }
}
