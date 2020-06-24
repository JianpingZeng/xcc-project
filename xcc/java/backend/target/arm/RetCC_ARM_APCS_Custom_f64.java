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
import static backend.target.arm.ARMGenRegisterNames.R3;

class RetCC_ARM_APCS_Custom_f64 {
    static boolean apply(int valNo,
                         EVT valVT,
                         EVT locVT,
                         CCValAssign.LocInfo locInfo,
                         ArgFlagsTy argFlags,
                         CCState state) {
        if (!f64RetAssign(valNo, valVT, locVT, locInfo, state))
            return false;
        if (locVT.equals(new EVT(MVT.v2f64)) && !f64RetAssign(valNo, valVT, locVT, locInfo, state))
            return false;
        return true;
    }

    private static boolean f64RetAssign(int valNo,
                                        EVT valVT,
                                        EVT locVT,
                                        CCValAssign.LocInfo locInfo,
                                        CCState state) {
        int[] hiRegList = new int[]{R0, R2};
        int[] lowRegList = new int[]{R1, R3};
        int reg = state.allocateReg(hiRegList, lowRegList);
        // fail to handle it.
        if (reg == 0) return false;
        int i = 0;
        for (; i < 2; ++i)
            if (hiRegList[i] == reg)
                break;
        state.addLoc(CCValAssign.getCustomReg(valNo, valVT, reg, locVT, locInfo));
        state.addLoc(CCValAssign.getCustomReg(valNo, valVT, lowRegList[i], locVT, locInfo));
        return true;
    }
}
