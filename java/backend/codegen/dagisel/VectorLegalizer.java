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

package backend.codegen.dagisel;

import backend.target.TargetLowering;

import java.util.HashMap;

/**
 * This file implements the {@linkplain SelectionDAG#legalizeVectors} method.
 * <br></br>
 * The vector legalizer looks for vector operations which might needs to be
 * scalarized and legalizes them. This is a separate step from legalize because
 * scalarizing can introduce illegal types. For instance, suppose we have an
 * ISD.SDIV of type v2i64 on x86-32. The type is legal(for example, addition
 * on v2i64 is legal), but ISD.SDIV is illegal, so we have to unroll the operation,
 * which introduces nodes with the illegal type i64 which must be expanded.
 * Similarly, suppose we have an ISD.SRA of type v16i8 on PowerPC; the operation
 * must be unrolled, which introduces nodes with the illegal type i8 which must be
 * promoted.
 * <br></br>
 * This does not legalize vector manipulation like ISD.BUILD_VECTOR, or operation
 * that happen to take a vector which are custom-lowered; the legalization for
 * such operations never produces nodes with illegal types, so it's fine to
 * put off legalizing them until {@linkplain SelectionDAG#legalize} method runs.
 * @author Xlous.zeng
 * @version 0.1
 */
public class VectorLegalizer
{
    private SelectionDAG dag;
    private TargetLowering tli;
    private boolean changed;

    private HashMap<SDValue, SDValue> legalizeNodes;

    private void addLegalizedOperand(SDValue from, SDValue to)
    {}

    private SDValue legalizeOp(SDValue op)
    {}
    private SDValue translateLegalizeResults(SDValue op, SDValue result)
    {}
    private SDValue unrollVectorOp(SDValue op)
    {}
    private SDValue unrollVSETCC(SDValue op)
    {}
    private SDValue expandFNEG(SDValue op)
    {}
    private SDValue promoteVectorOp(SDValue op)
    {}

    public boolean run()
    {}

    public VectorLegalizer(SelectionDAG dag)
    {
        this.dag = dag;
        tli = dag.getTargetLoweringInfo();
        changed = false;
    }
}
