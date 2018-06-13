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
/**
 * @author Xlous.zeng
 * @version 0.1
 */

import backend.codegen.EVT;

/**
 * Represents a use of a SDNode. This class holds an SDValue,
 * which records the SDNode being used and the result number, a
 * pointer to the SDNode using the value, and Next and Prev pointers,
 * which link together all the uses of an SDNode.
 */
public class SDUse implements Comparable<SDUse>
{
    /**
     * The value being used.
     */
    SDValue val;
    /**
     * The user of this value.
     */
    SDNode user;

    public SDUse() {}

    public SDValue get()
    {
        return val;
    }

    public SDNode getUser()
    {
        return user;
    }

    public SDNode getNode()
    {
        return val.getNode();
    }

    public int getResNo()
    {
        return val.getResNo();
    }

    /**
     * Determines the returned Value Type for the {@linkplain #val}.
     * @return
     */
    public EVT getValueType()
    {
        return val.getValueType();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
            return false;
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;
        SDUse use = (SDUse) obj;
        return use.val.equals(val) && use.user.equals(user);
    }

    @Override
    public int compareTo(SDUse o)
    {
        return val.compareTo(o.val);
    }

    public void setUser(SDNode p)
    {
        user = p;
    }

    /**
     * Remove this use from its existing use list, assign it the
     * given value, and add it to the new value's node's use list.
     * @param v
     */
    public void set(SDValue v)
    {
        if (val.getNode() != null)
            val.getNode().removeUse(this);
        val = v;
        if (val.getNode() != null)
            val.getNode().addUse(this);
    }

    /**
     * Like set, but only supports initializing a newly-allocated
     * SDUse with a non-null value.
     * @param v
     */
    public void setInitial(SDValue v)
    {
        val = v;
        if (v != null)
            v.getNode().addUse(this);
    }

    /**
     * Like set, but only sets the Node portion of the value,
     * leaving the ResNo portion unmodified.
     * @param n
     */
    public void setNode(SDNode n)
    {
        if (val.getNode() != null)
            val.getNode().removeUse(this);
        val.setNode(n);
        if (n != null)
            n.addUse(this);
    }
}
