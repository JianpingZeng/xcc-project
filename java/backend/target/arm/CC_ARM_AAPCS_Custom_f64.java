package backend.target.arm;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 * This software is subjected to the protection of BSD 3.0 Licence.
 * For more details, please refers to the LICENSE file.
 */
import backend.codegen.*;
import tools.Util;

import static backend.target.arm.ARMGenRegisterNames.*;

class CC_ARM_AAPCS_Custom_f64 {
    static boolean apply(int valNo,
                         EVT valVT,
                         EVT locVT,
                         CCValAssign.LocInfo locInfo,
                         ArgFlagsTy argFlags,
                         CCState state) {
        if (!f64AssignAAPCS(valNo, valVT, locVT, locInfo, state, true))
            return false;
        if (locVT.equals(new EVT(MVT.v2f64)) &&
                !f64AssignAAPCS(valNo, valVT, locVT, locInfo, state, false))
            return false;
        return true;
    }

    private static boolean f64AssignAAPCS(int valNo, EVT valVT, EVT locVT,
                                          CCValAssign.LocInfo locInfo,
                                          CCState state, boolean canFail) {
        int[] hiRegList = new int[] {R0, R2};
        int[] loRegList = new int[] {R1, R3};
        int[] shadowRegList = new int[] {R0, R1};

        int reg = state.allocateReg(hiRegList, shadowRegList);
        if (reg == 0) {
            if (canFail) return false;
            // for the second half of a v2f64, allocate it to stack.
            state.addLoc(CCValAssign.getCustomMem(valNo, valVT, state.allocateStack(8,8),
                    locVT, locInfo));
            return true;
        }
        int i = 0;
        for (;i < 2; ++i)
            if (hiRegList[i] == reg)
                break;

        int t = state.allocateReg(loRegList[i]);
        Util.assertion(t == loRegList[i], "couldn't allocate register");
        state.addLoc(CCValAssign.getReg(valNo, valVT, reg, locVT, locInfo));
        state.addLoc(CCValAssign.getReg(valNo, valVT, loRegList[i], locVT, locInfo));
        return true;
    }
}
