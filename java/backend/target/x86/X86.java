/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package backend.target.x86;

import backend.codegen.dagisel.SDNode;
import backend.codegen.dagisel.SDNode.ConstantFPSDNode;
import backend.codegen.dagisel.SDNode.ConstantSDNode;
import backend.codegen.dagisel.SDNode.ShuffleVectorSDNode;
import backend.codegen.dagisel.SDValue;
import backend.target.TargetMachine;

import static tools.Util.isInt32;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public interface X86
{
    // Enums for memory operand decoding.  Each memory operand is represented with
    // a 5 operand sequence in the form:
    //   [BaseReg, ScaleAmt, IndexReg, Disp, Segment]
    // These enums help decode this.

    int AddrBaseReg = 0;
    int AddrScaleAmt = 1;
    int AddrIndexReg = 2;
    int AddrDisp = 3;

    /// AddrSegmentReg - The operand # of the segment in the memory operand.
    int AddrSegmentReg = 4;

    /// AddrNumOperands - Total number of operands in a memory reference.
    int AddrNumOperands = 5;

    static boolean isPSHUFDMask(ShuffleVectorSDNode n)
    {
        return false;
    }

    static boolean isUNPCKHMask(ShuffleVectorSDNode n) {
        return false;
    }

    static boolean isUNPCKH_v_undef_Mask(ShuffleVectorSDNode n) {
        return false;
    }

    static boolean isUNPCKLMask(ShuffleVectorSDNode n) {
        return false;
    }

    static boolean isUNPCKL_v_undef_Mask(ShuffleVectorSDNode n) {
        return false;
    }

    static boolean isMOVDDUPMask(ShuffleVectorSDNode n) {
        return false;
    }

    static boolean isMOVHLPSMask(ShuffleVectorSDNode n) {
        return false;
    }

    static boolean isMOVHLPS_v_undef_Mask(ShuffleVectorSDNode n) {
        return false;
    }

    static boolean isMOVHPMask(ShuffleVectorSDNode n) {
        return false;
    }

    static boolean isMOVLMask(ShuffleVectorSDNode n) {
        return false;
    }

    static boolean isMOVLPMask(ShuffleVectorSDNode n) {
        return false;
    }

    static boolean isMOVSHDUPMask(ShuffleVectorSDNode n) {
        return false;
    }

    static boolean isMOVSLDUPMask(ShuffleVectorSDNode n) {
        return false;
    }

    static boolean isPSHUFHWMask(ShuffleVectorSDNode n) {
        return false;
    }

    static boolean isPSHUFLWMask(ShuffleVectorSDNode n) {
        return false;
    }

    static boolean isSHUFPMask(ShuffleVectorSDNode n) {
        return false;
    }

    static byte getShuffleSHUFImmediate(SDNode n) {
        return 0;
    }

    static byte getShufflePSHUFHWImmediate(SDNode n) {
        return 0;
    }

    static byte getShufflePSHUFLWImmediate(SDNode n) {
        return 0;
    }

    static boolean isZeroMode(SDValue elt)
    {
        SDNode n = elt.getNode();
        return (n instanceof ConstantSDNode) &&
                ((ConstantSDNode)n).getZExtValue() == 0 ||
                (n instanceof ConstantFPSDNode && ((ConstantFPSDNode)n).getValueAPF().isPosZero());
    }

    static boolean isOffsetSuitableForCodeModel(long offset,
            TargetMachine.CodeModel model)
    {
        return isOffsetSuitableForCodeModel(offset, model, true);
    }

    static boolean isOffsetSuitableForCodeModel(long offset,
            TargetMachine.CodeModel model, boolean hasSymbolicDisplacement)
    {
        if (!isInt32(offset))
            return false;

        if (!hasSymbolicDisplacement)
            return true;

        if (model != TargetMachine.CodeModel.Small && model != TargetMachine.CodeModel.Kernel)
            return false;

        if (model == TargetMachine.CodeModel.Small && offset < 16*1024*1024)
            return true;
        if (model == TargetMachine.CodeModel.Kernel && offset > 0)
            return true;

        return false;
    }
}
