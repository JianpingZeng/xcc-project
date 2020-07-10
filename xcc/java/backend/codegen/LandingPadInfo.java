/*
 *  Extremely Compiler Collection
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 * This software is subjected to the protection of BSD 3.0 Licence. For more
 * details,lease refers to the LICENSE file.
 */

package backend.codegen;

import backend.mc.MCSymbol;
import backend.value.Function;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;

/**
 * This is a plain class for encapsulating landing pad information
 * of current function.
 */
public class LandingPadInfo {
    MachineBasicBlock landingPadBlock;
    ArrayList<MCSymbol> beginLabels;
    ArrayList<MCSymbol> endLabels;
    MCSymbol landingPadLabel;
    Function personality;
    TIntArrayList typeIds;
    LandingPadInfo(MachineBasicBlock pad) {
        landingPadBlock = pad;
        beginLabels = new ArrayList<>();
        endLabels = new ArrayList<>();
        landingPadLabel = null;
        personality = null;
        typeIds = new TIntArrayList();
    }
}
