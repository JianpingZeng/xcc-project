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

package backend.codegen;

/**
 * Machine pass node stored in registration list.
 * @author Xlous.zeng
 * @version 0.1
 */
public class MachinePassRegistryNode<T>
{
    private MachinePassRegistryNode next;
    private String name;    // the name of this pass to command line option.
    private String description;
    private T ctor;

    public MachinePassRegistryNode(String name, String desc,T ctor)
    {
        this.next = null;
        this.name = name;
        this.description = desc;
        this.ctor = ctor;
    }

    public MachinePassRegistryNode getNext()
    {
        return next;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public T getCtor()
    {
        return ctor;
    }

    public void setNext(MachinePassRegistryNode next)
    {
        this.next = next;
    }
}
