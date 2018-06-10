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
import backend.codegen.dagisel.SDNode.LoadSDNode;
import backend.codegen.fastISel.ISD;

/**
 * Unlike LLVM values, Selection DAG nodes may return multiple
 * values as the result of a computation.  Many nodes return multiple values,
 * from loads (which define a token and a return value) to ADDC (which returns
 * a result and a carry value), to calls (which may return an arbitrary number
 * of values).
 *
 * As such, each use of a SelectionDAG computation must indicate the node that
 * computes it as well as which return value to use from that node.  This pair
 * of information is represented with the SDValue value type.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public class SDValue implements Comparable<SDValue>, Cloneable
{
    /**
     * The node defining the value we are using.
     */
    private SDNode node;
    /**
     * Which return value of the node we are using.
     */
    private int resNo;

    public SDValue()
    {
    }

    public SDValue(SDNode node, int resno)
    {
        this.node = node;
        resNo = resno;
    }

    /**
     * get the index which selects a specific result in the SDNode
     * @return
     */
    public int getResNo()
    {
        return resNo;
    }

    /**
     * get the SDNode which holds the desired result
     * @return
     */
    public SDNode getNode()
    {
        return node;
    }

    /**
     * set the SDNode
     * @param n
     */
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
    public int hashCode()
    {
        return node.hashCode() << 7 + resNo;
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

    /**
     * Return true if this node is an operand of n.
     * @param n
     * @return
     */
    public boolean isOperandOf(SDNode n)
    {
        for (int i = n.getNumOperands() - 1; i >= 0; i--)
        {
            if (n.getOperand(i).equals(this))
                return true;
        }

        return false;
    }

    /**
     * Return the ValueType of the referenced return value.
     * @return
     */
    public EVT getValueType()
    {
        return node.getValueType(resNo);
    }

    /**
     * Returns the size of the value in bits.
     * @return
     */
    public int getValueSizeInBits()
    {
        return getValueType().getSizeInBits();
    }

    /**
     * These forward to the corresponding methods in SDNode.
     * @return
     */
    public int getOpcode()
    {
        return node.getOpcode();
    }

    public int getNumOperands()
    {
        return node.getNumOperands();
    }

    public SDValue getOperand(int idx)
    {
        assert idx >= 0 && idx < getNumOperands();
        return node.getOperand(idx);
    }

    public long getConstantOperandVal(int idx)
    {
        assert idx >= 0 && idx < getNumOperands();
        return node.getConstantOperandVal(idx);
    }

    public boolean isTargetOpcode()
    {
        return node.isTargetOpcode();
    }

    public boolean isMachineOpcode()
    {
        return node.isMachineOpecode();
    }

    public int getMachineOpcode()
    {
        assert isMachineOpcode():"Can't calling this method on non-machine opcode!";
        return node.getMachineOpcode();
    }

    /**
     * Return true if this operand (which must
     * be a chain) reaches the specified operand without crossing any
     * side-effecting instructions.  In practice, this looks through token
     * factors and non-volatile loads.  In order to remain efficient, this only
     * looks a couple of nodes in, it does not do an exhaustive search.
     * @param dest
     * @return
     */
    public boolean reachesChainWithoutSideEffects(SDValue dest)
    {
        return reachesChainWithoutSideEffects(dest, 2);
    }

    public boolean reachesChainWithoutSideEffects(SDValue dest, int depth)
    {
        if (this == dest) return true;
        if (depth == 0) return false;

        if (getOpcode() == ISD.TokenFactor)
        {
            for (int i = 0, e = getNumOperands(); i < e; i++)
            {
                if (getOperand(i).reachesChainWithoutSideEffects(dest, depth-1))
                    return true;
            }
            return false;
        }

        LoadSDNode load = node instanceof LoadSDNode ? (LoadSDNode)node: null;
        if (load != null)
        {
            if (!load.isVolatile())
                return load.getChain().reachesChainWithoutSideEffects(dest, depth-1);
        }
        return false;
    }

    /**
     * Return true if there are no nodes using value resNo
     * of node.
     * @return
     */
    public boolean isUseEmpty()
    {
        return !node.hasAnyUseOfValue(resNo);
    }

    /**
     * Return true if there is exactly one node using value
     * resNo of node.
     * @return
     */
    public boolean hasOneUse()
    {
        return node.hasNumUsesOfValue(1, resNo);
    }

    @Override
    public SDValue clone()
    {
        SDValue res = new SDValue();
        res.node = node;
        res.resNo = resNo;
        return res;
    }
}
