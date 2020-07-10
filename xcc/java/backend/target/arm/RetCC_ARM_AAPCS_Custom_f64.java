package backend.target.arm;
/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 * This software is subjected to the protection of BSD 3.0 Licence.
 * For more details, please refers to the LICENSE file.
 */
import backend.codegen.*;

class RetCC_ARM_AAPCS_Custom_f64 {
    static boolean apply(int valNo,
                         EVT valVT,
                         EVT locVT,
                         CCValAssign.LocInfo locInfo,
                         ArgFlagsTy argFlags,
                         CCState state) {
        return RetCC_ARM_APCS_Custom_f64.apply(valNo, valVT, locVT, locInfo, argFlags, state);
    }
}
