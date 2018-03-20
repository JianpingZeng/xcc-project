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
 * /// which records the SDNode being used and the result number, a
 * /// pointer to the SDNode using the value, and Next and Prev pointers,
 * /// which link together all the uses of an SDNode.
 * ///
 */
public class SDUse implements Comparable<SDUse>
{
    /// val - The value being used.
    SDValue val;
    /// User - The user of this value.
    SDNode User;

    SDUse()
    {
    }

    /// If implicit conversion to SDValue doesn't work, the get() method returns
    /// the SDValue.
    SDValue get()
    {
        return val;
    }

    /// getUser - This returns the SDNode that contains this Use.
    SDNode getUser()
    {
        return User;
    }

    /// getNode - Convenience function for get().getNode().
    SDNode getNode()
    {
        return val.getNode();
    }

    /// getResNo - Convenience function for get().getResNo().
    int getResNo()
    {
        return val.getResNo();
    }

    /// getValueType - Convenience function for get().getValueType().
    EVT getValueType()
    {
        return val.getValueType();
    }

    /// operator== - Convenience function for get().operator==

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
        return use.val.equals(val);
    }

    @Override
    public int compareTo(SDUse o)
    {
        return val.compareTo(o.val);
    }

    void setUser(SDNode p)
    {
        User = p;
    }

    /// set - Remove this use from its existing use list, assign it the
    /// given value, and add it to the new value's node's use list.
    void set(SDValue V)
    {
    }

    /// setInitial - like set, but only supports initializing a newly-allocated
    /// SDUse with a non-null value.
    void setInitial(SDValue V)
    {
    }

    /// setNode - like set, but only sets the Node portion of the value,
    /// leaving the ResNo portion unmodified.
    void setNode(SDNode N)
    {
    }

    public int getSize()
    {
        assert false;
        return 0;
    }

    public SDValue getAt(int num)
    {
        assert false;
        return null;
    }
}
