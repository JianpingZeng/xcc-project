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

import backend.analysis.DomTree; /**
 * @author Xlous.zeng
 * @version 0.1
 */
public class DefaultDotGraphTrait<T>
{
    public static DefaultDotGraphTrait createDotTrait(DomTree dt, String funcName)
    {
        return new DomTreeDotGraphTrait(dt, funcName);
    }

    public String getGraphName()
    {
        return "";
    }

    public boolean renderGraphFromBottomUp()
    {
        return false;
    }

    public String getGraphProperties()
    {
        return "";
    }

    public String getNodeLabel(T node, boolean shortName)
    {
        return "";
    }

    public boolean hasNodeAddressLabel(T node)
    {
        return false;
    }

    public String getNodeAttributes(T node)
    {
        return "";
    }

    public String getEdgeAttributes(T from, T to)
    {
        return "";
    }

    public String getEdgeSourceLabel(T from, T to)
    {
        return "";
    }

    public boolean edgeTargetEdgeSource(T from, T to)
    {
        return false;
    }

    public void getEdgeTarget(T node)
    {
        // TODO: 17-12-4
    }

    public boolean hasEdgeDestLabels()
    {
        return false;
    }

    public int getNumEdgeDestLabels(T node)
    {
        return 0;
    }

    public String getEdgeDestLabel(T node, int i)
    {
        return "";
    }

    public void addCustomGraphFeatures()
    {
    }

    public void writeFooter(GraphWriter writer)
    {

    }

    public void writeNodes(GraphWriter writer)
    {

    }
}
