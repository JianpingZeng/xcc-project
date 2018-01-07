package utils.tablegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

import utils.tablegen.CodeGenIntrinsic.ModRefType;
import utils.tablegen.Init.DefInit;

import static utils.tablegen.SDNP.SDNPMayLoad;
import static utils.tablegen.SDNP.SDNPMayStore;
import static utils.tablegen.SDNP.SDNPSideEffect;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class InstAnalyzer
{
    CodeGenDAGPatterns cdp;
    boolean mayStore;
    boolean mayLoad;
    boolean hasSideEffect;

    public InstAnalyzer(CodeGenDAGPatterns cdp,
            boolean mayStore,
            boolean mayLoad,
            boolean hasSideEffect)
    {
        this.cdp = cdp;
        this.mayStore = mayStore;
        this.mayLoad = mayLoad;
        this.hasSideEffect = hasSideEffect;
    }

    /**
     * Analyze the specified instruction, returning true if the instruction
     * had a pattern.
     * @param instDef    The instruction definition that we would infer flags
     *                  for.
     * @return          Return true if have pattern, otherwise return false.
     */
    public boolean analyze(Record instDef)
    {
        TreePattern pattern = cdp.getInstruction(instDef).getPattern();
        if (pattern == null)
        {
            // No pattern, just terminate early.
            hasSideEffect = true;       // conservative consideration.
            return false;
        }

        analyzeNode(pattern.getTree(0));
        return true;
    }

    private void analyzeNode(TreePatternNode node)
    {
        if (node.isLeaf())
        {
            DefInit def = node.getLeafValue() instanceof DefInit ?
                    (DefInit)node.getLeafValue() : null;
            if (def != null)
            {
                Record leafRec = def.getDef();
                // Handle ComplexPattern leaves.
                if (leafRec.isSubClassOf("ComplexPattern"))
                {
                    ComplexPattern cp = cdp.getComplexPattern(leafRec);
                    if (cp.hasProperty(SDNPMayStore))
                        mayStore = true;
                    if (cp.hasProperty(SDNPMayLoad))
                        mayLoad = true;
                    if (cp.hasProperty(SDNPSideEffect))
                        hasSideEffect = true;
                }
            }
            return;
        }

        // Analyze childen.
        for (int i = 0, e = node.getNumChildren(); i < e; i++)
        {
            analyzeNode(node.getChild(i));
        }

        // Ignore set nodes which are not SDNodes.
        if (node.getOperator().getName().equals("set"))
        {
            return;
        }

        // get information about the SDNode for the operator.
        SDNodeInfo opInfo = cdp.getSDNodeInfo(node.getOperator());

        // Notice of properties of the node.
        if (opInfo.hasProperty(SDNPMayStore)) mayStore = true;
        if (opInfo.hasProperty(SDNPMayLoad)) mayLoad = true;
        if (opInfo.hasProperty(SDNPSideEffect)) hasSideEffect = true;

        CodeGenIntrinsic intrisic = node.getIntrinsicInfo(cdp);
        if (intrisic != null)
        {
            // If this is an intrinsic, analyze it.
            if (intrisic.modRef.compareTo(ModRefType.ReadArgMem)>=0)
                mayLoad = true;
            if (intrisic.modRef.compareTo(ModRefType.WriteArgMem) >= 0)
                mayStore = true;
            if (intrisic.modRef.compareTo(ModRefType.WriteMem) >= 0)
                hasSideEffect = true;
        }
    }
}
