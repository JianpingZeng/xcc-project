package backend.transform.scalars.instructionCombine;

/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Jianping Zeng.
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

import backend.utils.InstVisitor;
import backend.value.User;
import backend.value.Value;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class Combiner implements InstVisitor<Value>
{
    @Override
    public Value visitRet(User inst)
    {
        return null;
    }

    @Override
    public Value visitBr(User inst)
    {
        return null;
    }

    @Override
    public Value visitSwitch(User inst)
    {
        return null;
    }

    @Override
    public Value visitICmp(User inst)
    {
        return null;
    }

    @Override
    public Value visitFCmp(User inst)
    {
        return null;
    }

    @Override
    public Value visitLoad(User inst)
    {
        return null;
    }

    @Override
    public Value visitStore(User inst)
    {
        return null;
    }

    @Override
    public Value visitCall(User inst)
    {
        return null;
    }

    @Override
    public Value visitGetElementPtr(User inst)
    {
        return null;
    }

    @Override
    public Value visitPhiNode(User inst)
    {
        return null;
    }

    @Override
    public Value visitSelect(User inst)
    {
        return null;
    }
}
