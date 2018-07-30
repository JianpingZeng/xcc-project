/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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

import backend.target.TargetLowering.TargetLoweringOpt;

import java.util.ArrayList;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public class DAGCombinerInfo
{
    private DAGCombiner combiner;
    private boolean beforeLegalize;
    private boolean beforeLegalizeOps;
    private boolean calledByLegalizer;
    public SelectionDAG dag;

    public DAGCombinerInfo(SelectionDAG dag,
            boolean bl, boolean blo, boolean cl,
            DAGCombiner dagCB)
    {
        this.dag = dag;
        this.beforeLegalize = bl;
        this.beforeLegalizeOps = blo;
        this.calledByLegalizer = cl;
        combiner = dagCB;
    }

    public boolean isBeforeLegalize()
    {
        return beforeLegalize;
    }

    public boolean isBeforeLegalizeOps()
    {
        return beforeLegalizeOps;
    }

    public boolean isCalledByLegalizer()
    {
        return calledByLegalizer;
    }

    public void addToWorkList(SDNode n)
    {
        combiner.addToWorkList(n);
    }
    public SDValue combineTo(SDNode n, ArrayList<SDValue> to)
    {
        return combineTo(n, to, true);
    }
    public SDValue combineTo(SDNode n, ArrayList<SDValue> to, boolean addTo)
    {
        return combiner.combineTo(n, to, addTo);
    }
    public SDValue combineTo(SDNode n, SDValue res)
    {
        return combineTo(n, res, true);
    }
    public SDValue combineTo(SDNode n, SDValue res, boolean addTo)
    {
        return combiner.combineTo(n, res, addTo);
    }
    public SDValue combineTo(SDNode n, SDValue res0, SDValue res1)
    {
        return combineTo(n, res0, res1, true);
    }
    public SDValue combineTo(SDNode n, SDValue res0, SDValue res1, boolean addTo)
    {
        return combiner.combineTo(n, res0, res1, addTo);
    }
    public void commitTargetLoweringOpt(TargetLoweringOpt tli)
    {
        combiner.commitTargetLoweringOpt(tli);
    }
}
