package backend.value;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
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

import backend.support.LLVMContext;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class NamedMDNode extends MetadataBase
{
    private Module parent;
    private ArrayList<MetadataBase> node;

    public NamedMDNode(String name, MetadataBase[] elts, Module m)
    {
        super(LLVMContext.MetadataTy, ValueKind.NamedMDNodeVal);
        parent = m;
        node = new ArrayList<>();
        for (MetadataBase n : elts)
            node.add(n);
    }

    public NamedMDNode(String name, List<MetadataBase> elts, Module m)
    {
        super(LLVMContext.MetadataTy, ValueKind.NamedMDNodeVal);
        parent = m;
        node = new ArrayList<>();
        node.addAll(elts);
    }

    public static NamedMDNode create(String name, ArrayList<MetadataBase> elts,
            Module m)
    {
        return new NamedMDNode(name, elts, m);
    }
}
