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

import backend.analysis.aa.AliasAnalysis;
import backend.target.TargetLowering.TargetLoweringOpt;
import backend.target.TargetMachine;

import java.util.ArrayList;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class DAGCombiner
{
    private SelectionDAG dag;
    private CombineLevel level;
    private TargetMachine.CodeGenOpt optLevel;
    private boolean legalOprations;
    private boolean legalTypes;
    private ArrayList<SDNode> workList;
    private AliasAnalysis aa;


    public DAGCombiner(SelectionDAG dag, AliasAnalysis aa,
            TargetMachine.CodeGenOpt optLevel)
    {
        this.dag = dag;
        this.aa = aa;
        this.optLevel = optLevel;
        workList = new ArrayList<>();
    }

    public void run(CombineLevel level)
    {
        this.level = level;
    }

    public void addToWorkList(SDNode n)
    {
        removeFromWorkList(n);
        workList.add(n);
    }

    public void removeFromWorkList(SDNode n)
    {
        workList.remove(n);
    }

    public SDValue combineTo(SDNode n, ArrayList<SDValue> to, boolean addTo)
    {
        return null;
    }

    public SDValue combineTo(SDNode n, SDValue res, boolean addTo)
    {
        ArrayList<SDValue> vals = new ArrayList<>();
        vals.add(res);
        return combineTo(n, vals, addTo);
    }

    public SDValue combineTo(SDNode n, SDValue res0, SDValue res1, boolean addTo)
    {
        ArrayList<SDValue> vals = new ArrayList<>();
        vals.add(res0);
        vals.add(res1);
        return combineTo(n, vals, addTo);
    }

    public static class WorklistRemover implements DAGUpdateListener
    {
        private DAGCombiner combiner;
        public WorklistRemover(DAGCombiner cmb)
        {
            combiner = cmb;
        }
        @Override
        public void nodeDeleted(SDNode node, SDNode e)
        {
            combiner.removeFromWorkList(node);
        }

        @Override
        public void nodeUpdated(SDNode node)
        {
            // ignore updates.
        }
    }

    public void commitTargetLoweringOpt(TargetLoweringOpt tlo)
    {
        WorklistRemover remover = new WorklistRemover(this);
        dag.replaceAllUsesOfValueWith(tlo.oldVal, tlo.newVal, remover);

        addToWorkList(tlo.newVal.getNode());
        addUsersToWorklist(tlo.newVal.getNode());

        if (tlo.oldVal.getNode().isUseEmpty())
        {
            removeFromWorkList(tlo.oldVal.getNode());

            for (int i = 0, e = tlo.oldVal.getNode().getNumOperands(); i < e; i++)
            {
                if (tlo.oldVal.getNode().getOperand(i).getNode().hasOneUse())
                    addToWorkList(tlo.oldVal.getNode().getOperand(i).getNode());
            }
            dag.deleteNode(tlo.oldVal.getNode());
        }
    }

    private void addUsersToWorklist(SDNode node)
    {
        for (SDUse u : node.useList)
        {
            addToWorkList(u.getNode());
        }
    }
}
