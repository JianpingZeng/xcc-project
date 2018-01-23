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

public class InputArgList extends ArgList
{
    private ArrayList<String> argStrings;
    private ArrayList<Arg> actualArgs;

    public InputArgList(ArrayList<String> argStrings)
    {
        super(null);
        actualArgs = new ArrayList<>();
        setArg(actualArgs);
        this.argStrings = argStrings;
    }

    @Override
    public String getArgString(int index)
    {
        assert index >= 0 && index < argStrings.size();
        return argStrings.get(index);
    }

    public int getNumInputStrings()
    {
        return argStrings.size();
    }
}
