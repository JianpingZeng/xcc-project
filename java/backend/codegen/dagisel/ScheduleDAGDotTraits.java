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

import backend.support.DefaultDotGraphTrait;
import backend.support.GraphWriter;
import tools.Util;

import java.io.PrintStream;
import java.util.ArrayList;

import static tools.Util.escapeString;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public class ScheduleDAGDotTraits extends DefaultDotGraphTrait<SUnit>
{
    private ScheduleDAG dag;
    private PrintStream os;
    private boolean shortName;

    public ScheduleDAGDotTraits(ScheduleDAG dag, boolean shortName)
    {
        this.dag = dag;
        this.shortName = shortName;
    }

    @Override
    public String getGraphName()
    {
        return dag.getMachineFunction().getFunction().getName();
    }

    @Override
    public boolean renderGraphFromBottomUp()
    {
        return true;
    }

    @Override
    public boolean hasNodeAddressLabel(SUnit node)
    {
        return true;
    }

    @Override
    public String getEdgeAttributes(SUnit from, Object to)
    {
        SDep sd = (SDep)to;
        if (sd.isArtificial())
            return "color=cyan,style=dashed";
        if (sd.isCtrl())
            return "color=blue,style=dashed";
        return "";
    }

    @Override
    public String getNodeLabel(SUnit node, boolean shortName)
    {
        return dag.getGraphNodeLabel(node);
    }

    @Override
    public String getNodeAttributes(SUnit node)
    {
        return "shape=Mrecord";
    }

    @Override
    public void addCustomGraphFeatures()
    {
        dag.addCustomGraphFeatures(this);
    }

    @Override
    public void writeFooter(GraphWriter writer)
    {
        os.println("}");
    }

    public void emitEdge(SUnit src, int srcNodePort,
            SUnit dest, int destNodePort,
            String attr)
    {
        if (srcNodePort > 64) return;
        if (destNodePort > 64)
            destNodePort = 64;

        os.printf("\tNode0x%x", src == null?0:src.hashCode());
        if (srcNodePort >= 0)
            os.printf(":s%d", srcNodePort);
        os.printf(" -> Node0x%x", dest == null?0:dest.hashCode());
        if (destNodePort >= 0)
        {
            if (hasEdgeDestLabels())
                os.printf(":d%d", destNodePort);
            else
                os.printf(":s%d", destNodePort);
        }
        if (!attr.isEmpty())
        {
            os.printf("[%s]", attr);
        }
        os.println(";");
    }

    @Override
    public void writeNodes(GraphWriter writer)
    {
        this.os = writer.getOut();
        for (SUnit su : dag.sunits)
            writeNode(su);
    }

    private void writeNode(SUnit su)
    {
        String nodeAttrs = getNodeAttributes(su);
        os.printf("\tNode0x%x [shape=record,", su.hashCode());
        if (!nodeAttrs.isEmpty())
        {
            os.print(nodeAttrs);
            os.print(",");
        }
        os.print("label=\"{");
        if (!renderGraphFromBottomUp())
        {
            os.print(getNodeLabel(su, shortName));
            if (hasNodeAddressLabel(su))
                os.printf("|%x", su.hashCode());
        }

        if (su.preds.size() > 0)
        {
            if (!renderGraphFromBottomUp())
                os.print("|");
            os.print("{");
            int i = 0, e = su.preds.size();
            for (; i < e && i < 64; i++)
            {
                if (i!=0) os.print("|");
                os.printf("<s%d>%s", i, getEdgeSourceLabel(su, su.preds.get(i)));
            }

            if (i != e)
                os.print("|<s64>truncated...");
            os.print("}");
            if (renderGraphFromBottomUp())
                os.print("|");
        }

        if (renderGraphFromBottomUp())
        {
            os.print(escapeString(getNodeLabel(su, shortName)));
            if (hasNodeAddressLabel(su))
                os.printf("|0x%x", su.hashCode());
        }

        if (hasEdgeDestLabels())
        {
            os.print("|{");

            int i = 0, e = getNumEdgeDestLabels(su);
            for (; i < e && i != 64; i++)
            {
                if (i != 0)
                    os.print("|");
                os.printf("<d%d>%s", i, getEdgeDestLabel(su, i));
            }

            if (i != e)
                os.print("|<d64>truncated...");
            os.print("}");
        }
        os.println("}\"];");    // finishing printing the "node" line.

        int i = 0, e = su.preds.size();
        for (; i < e && i < 64; i++)
            writeEdge(su, i, su.preds.get(i));
        for (; i < e; i++)
            writeEdge(su, 64, su.preds.get(i));
    }

    /**
     * Output an edge from a simple node into the graph
     * @param source
     * @param edgeIdx
     * @param dest
     */
    private void writeEdge(SUnit source, int edgeIdx, SDep dest)
    {
        if (edgeIdx >= 64) return;  // eliminating truncated parts.

        SUnit target = dest.getSUnit();
        if (target != null)
        {
            int destPort = -1;
            if (edgeTargetEdgeSource(source, dest))
            {
                destPort = getEdgeTarget(source, dest);
            }
            emitEdge(source, edgeIdx, target, destPort, getEdgeAttributes(source, dest));
        }
    }

    @Override
    public void emitSimpleNode(Object id, String attr, String label,
            int numEdgeSources, ArrayList<String> edgeSourceLabels)
    {
        os.printf("\tNode0x%d[ ", id == null?0:id.hashCode());
        if (!attr.isEmpty())
            os.printf("%s,", attr);
        os.printf(" label=\"");
        if (numEdgeSources > 0)
            os.print("{");
        os.print(Util.escapeString(label));
        if (numEdgeSources > 0)
        {
            os.print("|{");
            for (int i = 0; i < numEdgeSources; i++)
            {
                if (i != 0) os.print("|");
                os.printf("<s%d>", i);
                if (edgeSourceLabels != null)
                    os.printf(edgeSourceLabels.get(i));
            }
            os.print("}}");
        }
        os.println("\"];");
    }
}
