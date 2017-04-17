package tools.commandline;
/*
 * Extremely C language Compiler.
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

import tools.OutParamWrapper;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class ParserChar extends Parser<Character>
{
    public boolean parse(Option<?> opt,
            String optName, String arg,
            OutParamWrapper<Character> val)
    {
        val.set(arg.charAt(0));
        return false;
    }

    @Override
    public String getValueName()
    {
        return "char";
    }

    @Override
    public <T1> void initialize(Option<T1> opt){}

    @Override
    public ValueExpected getValueExpectedFlagDefault()
    {
        return ValueExpected.ValueRequired;
    }
}
