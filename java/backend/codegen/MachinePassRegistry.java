/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
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

package backend.codegen;

/**
 * Track the registration of machine passes.
 * @author Xlous.zeng
 * @version 0.1
 */
public class MachinePassRegistry
{
    /**
     * A list of registred machiene pass nodes
     */
    private MachinePassRegistryNode list;
    /**
     * The default constructor.
     */
    private MachinePassCtor defaultCtor;
    /**
     * The listener for registration.
     */
    private MachinePassRegistryListener listener;

    public MachinePassRegistryNode getList()
    {
        return list;
    }

    public MachinePassCtor getDefault()
    {
        return defaultCtor;
    }

    public void setDefaultCtor(MachinePassCtor defaultCtor)
    {
        this.defaultCtor = defaultCtor;
    }

    public void setList(MachinePassRegistryNode list)
    {
        this.list = list;
    }

    public void add(MachinePassRegistryNode node)
    {
        node.setNext(list);
        list = node;
        if (listener != null)
            listener.notifyAdd(node.getName(), node.getCtor(), node.getDescription());
    }

    public void remove(MachinePassRegistryNode node)
    {
        MachinePassRegistryNode prev = null;
        for (MachinePassRegistryNode ptr = list; ptr != null;)
        {
            if (ptr == node)
            {
                if (listener != null) listener.notifyRemove(node.getName());
                if (prev == null)
                {
                    // the deleted node is first one
                    list = list.getNext();
                }
                else
                {
                    prev.setNext(ptr.getNext());
                    ptr.setNext(null);
                }
                break;
            }
            prev = ptr;
            ptr = ptr.getNext();
        }
    }

    public void setListener(MachinePassRegistryListener listener)
    {
        this.listener = listener;
    }

    public MachinePassRegistryListener getListener()
    {
        return listener;
    }
}
