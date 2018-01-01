/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
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
}
