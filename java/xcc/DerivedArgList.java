/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2017, Xlous zeng.
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

package xcc;

import java.util.ArrayList;

public class DerivedArgList extends ArgList
{
    private InputArgList baseArgs;
    private ArrayList<Arg> actualArgs;
    private boolean onlyProxy;

    public DerivedArgList(InputArgList args, boolean onlyProxy)
    {
        super(onlyProxy ? args.getArgs():null);
        actualArgs = new ArrayList<>();
        if (!onlyProxy)
            setArg(actualArgs);
        baseArgs = args;
        this.onlyProxy = onlyProxy;
    }

    @Override
    public String getArgString(int index)
    {
        return null;
    }
}
