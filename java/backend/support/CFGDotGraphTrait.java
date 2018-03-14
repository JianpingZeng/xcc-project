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

package backend.support;

import backend.value.BasicBlock;
import backend.value.Function;
import backend.value.Instruction;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.BitMap;
import tools.BitSet2D;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.LinkedList;

import static backend.support.AssemblyWriter.writeAsOperand;
import static backend.support.GraphWriter.escapeString;


public class CFGDotGraphTrait extends DefaultDotGraphTrait<BasicBlock>
{
    private final Function fn;
    private String funcName;
    CFGDotGraphTrait(Function func)
    {
        assert func !=null:"Function can't be null!";
        fn = func;
        funcName = func.getName();
    }

    @Override
    public String getGraphName()
    {
        return "CFG for '" + funcName + "' function";
    }

    @Override
    public String getNodeLabel(BasicBlock node, boolean shortName)
    {
        if (shortName && !node.getName().isEmpty())
            return node.getName()+":";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        if (shortName)
        {
            writeAsOperand(ps, node, false, null);
            ps.close();
            return baos.toString();
        }
        if (node.getName().isEmpty())
        {
            writeAsOperand(new PrintStream(baos), node, false, null);
            ps.print(":");
        }
        node.print(ps);
        StringBuilder sb = new StringBuilder(baos.toString());
        if (sb.charAt(0) == '\n')
            sb.deleteCharAt(0);
        for (int i = 0; i< sb.length(); i++)
        {
            // converts '\n' to '\l' for justify.
            if (sb.charAt(i) == '\n')
            {
                sb.setCharAt(i, '\\');
                sb.insert(i+1, 'l');
                ++i;
            }
            else if (sb.charAt(i) == ';')
            {
                // delete all of comments.
                int endIdx = sb.indexOf("\n", i+1);
                if (endIdx != -1)
                {
                    sb.delete(i, endIdx);
                    --i;
                }
            }
        }
        return sb.toString();
    }

    private PrintStream os;

    @Override
    public void writeNodes(GraphWriter writer)
    {
        this.os = writer.getOut();
        BasicBlock entryBB = fn.getEntryBlock();
        assert entryBB != null:"No entry block for Function?";
        LinkedList<BasicBlock> stack = new LinkedList<>();
        stack.addLast(entryBB);
        ArrayList<BasicBlock> list = DepthFirstOrder.dfs(entryBB);
        TObjectIntHashMap<BasicBlock> visited = new TObjectIntHashMap<>();
        for (BasicBlock bb : list)
            visited.put(bb, 0);

        while (!stack.isEmpty())
        {
            BasicBlock curBB = stack.pollFirst();
            if (visited.get(curBB) != 0)
                continue;

            visited.put(curBB, 1);  // record it already be visited for avoiding dead loop.

            String nodeAttrites = getNodeAttributes(curBB);
            os.printf("\tNode0x%x [shape=record,", curBB.hashCode());
            if (!nodeAttrites.isEmpty())
                os.printf("%s,", nodeAttrites);
            os.print("label=\"{");

            if (!renderGraphFromBottomUp())
            {
                os.printf("%s", escapeString(getNodeLabel(curBB, writer.shortName)));
                if (hasNodeAddressLabel(curBB))
                    os.printf("|0x%x", curBB.hashCode());
            }
            // output nodes.
            int numSuccs = curBB.getNumSuccessors();
            if (numSuccs > 0)
            {
                if (!renderGraphFromBottomUp())
                    os.print("|");
                os.print("{");
                if (numSuccs > 64)
                {
                    os.print("|<s64>truncated...");
                }
                else
                {
                    for (int i = 0; i < numSuccs; i++)
                    {
                        if (i != 0)
                            os.print("|");
                        os.printf("<s%d>%s", i, getEdgeSourceLabel(curBB, curBB.suxAt(i)));
                    }
                }
                os.print("}");
            }
            os.println("}\"];");

            // Output edges.
            int i = 0;
            for (;i < numSuccs && i < 64; i++)
            {
                BasicBlock succ = curBB.suxAt(i);
                writeEdge(curBB, i, curBB.suxAt(i));
                // queue the successor blocks into list.
                stack.addLast(succ);
            }
            while(i < numSuccs)
            {
                BasicBlock succ = curBB.suxAt(i++);
                writeEdge(curBB, 64, succ);
                // queue the successor blocks into list.
                stack.addLast(succ);
            }
        }
    }

    /**
     * Output an edge from a simple node into the graph
     * @param source
     * @param edgeIdx
     * @param dest
     */
    private void writeEdge(BasicBlock source, int edgeIdx, BasicBlock dest)
    {
        if (edgeIdx >= 64) return;  // eliminating truncated parts.

        os.printf("\tNode0x%x", source.hashCode());
        if (edgeIdx >= 0)
            os.printf(":s%d", edgeIdx);
        os.printf(" -> Node0x%x", dest.hashCode());

        String attr = getEdgeAttributes(source, dest);
        if (!attr.isEmpty())
            os.printf("[%s]", attr);
        os.println(";");
    }

    @Override
    public void writeFooter(GraphWriter writer)
    {
        os.println("}");
    }

    @Override
    public String getEdgeSourceLabel(BasicBlock from, BasicBlock to)
    {
        Instruction.TerminatorInst ti = from.getTerminator();
        if (ti != null && ti instanceof Instruction.BranchInst)
        {
            Instruction.BranchInst br = (Instruction.BranchInst)ti;
            if (br.isConditional())
                return to == from.suxAt(0) ?"T":"F";
        }
        return "";
    }
}
