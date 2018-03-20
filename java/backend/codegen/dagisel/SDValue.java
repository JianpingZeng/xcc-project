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

import backend.codegen.EVT;

/**
 * Unlike LLVM values, Selection DAG nodes may return multiple
 * /// values as the result of a computation.  Many nodes return multiple values,
 * /// from loads (which define a token and a return value) to ADDC (which returns
 * /// a result and a carry value), to calls (which may return an arbitrary number
 * /// of values).
 * ///
 * /// As such, each use of a SelectionDAG computation must indicate the node that
 * /// computes it as well as which return value to use from that node.  This pair
 * /// of information is represented with the SDValue value type.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public class SDValue implements Comparable<SDValue>
{
    private SDNode node;       // The node defining the value we are using.
    private int resNo;     // Which return value of the node we are using.

    public SDValue()
    {
    }

    public SDValue(SDNode node, int resno)
    {
    }

    /// get the index which selects a specific result in the SDNode
    public int getResNo()
    {
        return resNo;
    }

    /// get the SDNode which holds the desired result
    public SDNode getNode()
    {
        return node;
    }

    /// set the SDNode
    public void setNode(SDNode n)
    {
        node = n;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null) return false;
        if (this == obj) return true;
        if (getClass() != obj.getClass())
            return false;
        SDValue other = (SDValue)obj;
        return node.equals(other.node) && resNo == other.resNo;
    }

    @Override
    public int compareTo(SDValue o)
    {
        int res = node.compareTo(o.node);
        return (res < 0 || (res == 0 && resNo < o.resNo)) ? 1 : -1;
    }

    public SDValue getValue(int r)
    {
        return new SDValue(node, r);
    }

    // isOperandOf - Return true if this node is an operand of n.
    public boolean isOperandOf(SDNode n)
    {
    }

    /// getValueType - Return the ValueType of the referenced return value.
    ///
    public EVT getValueType()
    {
    }

    /// getValueSizeInBits - Returns the size of the value in bits.
    ///
    public int getValueSizeInBits()
    {
        return getValueType().getSizeInBits();
    }

    // Forwarding methods - These forward to the corresponding methods in SDNode.
    public int getOpcode()
    {
    }

    public int getNumOperands()
    {
    }

    public SDValue getOperand(int idx)
    {
    }

    public long getConstantOperandVal(int idx)
    {
    }

    public boolean isTargetOpcode()
    {
    }

    public boolean isMachineOpcode()
    {
    }

    public int getMachineOpcode()
    {
    }

    /// reachesChainWithoutSideEffects - Return true if this operand (which must
    /// be a chain) reaches the specified operand without crossing any
    /// side-effecting instructions.  In practice, this looks through token
    /// factors and non-volatile loads.  In order to remain efficient, this only
    /// looks a couple of nodes in, it does not do an exhaustive search.
    public boolean reachesChainWithoutSideEffects(SDValue dest)
    {
        return reachesChainWithoutSideEffects(dest, 2);
    }

    public boolean reachesChainWithoutSideEffects(SDValue Dest, int Depth)
    {
    }

    /**
     * Return true if there are no nodes using value resNo
     * of node.
     * @return
     */
    public boolean isUseEmpty()
    {
    }

    /// hasOneUse - Return true if there is exactly one node using value
    /// resNo of node.
    ///
    public boolean hasOneUse()
    {
    }
}
