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

    /**
     * The operand of the segment in the memory operand.
     */
    int AddrSegmentReg = 4;

    /**
     * Total number of operands in a memory reference.
     */
    int AddrNumOperands = 5;

    /**
     * Return true if the specified VECTOR_SHUFFLE operand specifies a shuffle
     * of elements that is suitable for input to PSHUFD.
     * @param n
     * @return
     */
    static boolean isPSHUFDMask(ShuffleVectorSDNode n)
    {
        // TODO: 18-6-7
        return false;
    }

    /**
     * Return true if the specified VECTOR_SHUFFLE operand
     * specifies a shuffle of elements that is suitable for input to PSHUFD.
     * @param n
     * @return
     */
    static boolean isPSHUFHWMask(ShuffleVectorSDNode n)
    {
        // TODO: 18-6-7
        return false;
    }

    /**
     * Return true if the specified VECTOR_SHUFFLE operand
     * specifies a shuffle of elements that is suitable for input to PSHUFD.
     * @param n
     * @return
     */
    static boolean isPSHUFLWMask(ShuffleVectorSDNode n)
    {
        // TODO: 18-6-7
        return false;
    }

    /**
     * Return true if the specified VECTOR_SHUFFLE operand
     * specifies a shuffle of elements that is suitable for input to SHUFP*.
     * @param n
     * @return
     */
    static boolean isSHUFPMask(ShuffleVectorSDNode n)
    {
        // TODO: 18-6-7
        return false;
    }

    /**
     * Return true if the specified VECTOR_SHUFFLE operand
     * specifies a shuffle of elements that is suitable for input to MOVHLPS.
     * @param n
     * @return
     */
    static boolean isMOVHLPSMask(ShuffleVectorSDNode n)
    {
        // TODO: 18-6-7
        return false;
    }

    /**
     * Special case of isMOVHLPSMask for canonical form
     * of vector_shuffle v, v, <2, 3, 2, 3>, i.e. vector_shuffle v, undef,
     * <2, 3, 2, 3>
     * @param n
     * @return
     */
    static boolean isMOVHLPS_v_undef_Mask(ShuffleVectorSDNode n)
    {
        // TODO: 18-6-7
        return false;
    }

    /**
     * Return true if the specified VECTOR_SHUFFLE operand
     * specifies a shuffle of elements that is suitable for MOVLP{S|D}.
     * @param n
     * @return
     */
    static boolean isMOVLPMask(ShuffleVectorSDNode n)
    {
        // TODO: 18-6-7
        return false;
    }

    /**
     * Return true if the specified VECTOR_SHUFFLE operand
     * specifies a shuffle of elements that is suitable for MOVHP{S|D}.
     * as well as MOVLHPS.
     * @param n
     * @return
     */
    static boolean isMOVHPMask(ShuffleVectorSDNode n)
    {
        // TODO: 18-6-7
        return false;
    }

    static boolean isUNPCKLMask(ShuffleVectorSDNode n)
    {
        return isUNPCKLMask(n, false);
    }
    /**
     * Return true if the specified VECTOR_SHUFFLE operand
     * specifies a shuffle of elements that is suitable for input to UNPCKL.
     * @param n
     * @param v2IsSplat
     * @return
     */
    static boolean isUNPCKLMask(ShuffleVectorSDNode n, boolean v2IsSplat)
    {
        // TODO: 18-6-7
        return false;
    }

    static boolean isUNPCKHMask(ShuffleVectorSDNode n)
    {
        return isUNPCKHMask(n, false);
    }

    /**
     * Return true if the specified VECTOR_SHUFFLE operand
     * specifies a shuffle of elements that is suitable for input to UNPCKH.
     * @param n
     * @param v2IsSplat
     * @return
     */
    static boolean isUNPCKHMask(ShuffleVectorSDNode n, boolean v2IsSplat)
    {
        // TODO: 18-6-7
        return false;
    }

    /**
     * Special case of isUNPCKLMask for canonical form
     * of vector_shuffle v, v, <0, 4, 1, 5>, i.e. vector_shuffle v, undef,
     * <0, 0, 1, 1>
     * @param n
     * @return
     */
    static boolean isUNPCKL_v_undef_Mask(ShuffleVectorSDNode n)
    {
        // TODO: 18-6-7
        return false;
    }

    /**
     * Special case of isUNPCKHMask for canonical form
     * of vector_shuffle v, v, <2, 6, 3, 7>, i.e. vector_shuffle v, undef,
     * <2, 2, 3, 3>
     * @param n
     * @return
     */
    static boolean isUNPCKH_v_undef_Mask(ShuffleVectorSDNode n)
    {
        // TODO: 18-6-7
        return false;
    }

    /**
     * Return true if the specified VECTOR_SHUFFLE operand
     * specifies a shuffle of elements that is suitable for input to MOVSS,
     * MOVSD, and MOVD, i.e. setting the lowest element.
     * @param n
     * @return
     */
    static boolean isMOVLMask(ShuffleVectorSDNode n)
    {
        // TODO: 18-6-7
        return false;
    }

    /**
     * Return true if the specified VECTOR_SHUFFLE operand
     * specifies a shuffle of elements that is suitable for input to MOVSHDUP.
     * @param n
     * @return
     */
    static boolean isMOVSHDUPMask(ShuffleVectorSDNode n)
    {
        // TODO: 18-6-7
        return false;
    }

    /**
     * Return true if the specified VECTOR_SHUFFLE operand
     * specifies a shuffle of elements that is suitable for input to MOVSLDUP.
     * @param n
     * @return
     */
    static boolean isMOVSLDUPMask(ShuffleVectorSDNode n)
    {
        // TODO: 18-6-7
        return false;
    }

    /**
     * Return true if the specified VECTOR_SHUFFLE operand
     * specifies a shuffle of elements that is suitable for input to MOVDDUP.
     * @param n
     * @return
     */
    static boolean isMOVDDUPMask(ShuffleVectorSDNode n)
    {
        // TODO: 18-6-7
        return false;
    }

    /**
     * Return the appropriate immediate to shuffle
     * the specified isShuffleMask VECTOR_SHUFFLE mask with PSHUF* and SHUFP*
     * instructions.
     * @param n
     * @return
     */
    static int getShuffleSHUFImmediate(SDNode n)
    {
        // TODO: 18-6-7
        return 0;
    }

    /**
     * Return the appropriate immediate to shuffle
     * the specified isShuffleMask VECTOR_SHUFFLE mask with PSHUFHW
     * instructions.
     * @param n
     * @return
     */
    static int getShufflePSHUFHWImmediate(SDNode n)
    {
        // TODO: 18-6-7
        return 0;
    }

    /**
     * Return the appropriate immediate to shuffle
     * the specified isShuffleMask VECTOR_SHUFFLE mask with PSHUFLW
     * instructions.
     * @param n
     * @return
     */
    static int getShufflePSHUFLWImmediate(SDNode n)
    {
        // TODO: 18-6-7
        return 0;
    }

    static boolean isZeroMode(SDValue elt)
    {
        SDNode n = elt.getNode();
        return (n instanceof ConstantSDNode)
                && ((ConstantSDNode) n).getZExtValue() == 0 ||
                (n instanceof ConstantFPSDNode && ((ConstantFPSDNode) n)
                .getValueAPF().isPosZero());
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

        if (model == TargetMachine.CodeModel.Small && offset < 16 * 1024 * 1024)
            return true;
        if (model == TargetMachine.CodeModel.Kernel && offset > 0)
            return true;

        return false;
    }
}
