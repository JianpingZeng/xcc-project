package tools.commandline;
/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2018, Xlous Zeng.
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
public class ParserBool extends Parser<Boolean>
{
    private String optName;

    /**
     * Parses this option with argument of type bool.
     * @param opt
     * @param optName The name of this option.
     * @param arg   The argument of this option after '=' usually.
     * @param value The parsed value from inputted command line argument.
     * @return  True returned when error occur.
     */
    public boolean parse(Option<?> opt, String optName, String arg,
            OutParamWrapper<Boolean> value)
    {
        switch (arg)
        {
            case "":
            case "true":
            case "TRUE":
            case "True":
            case "1":
                value.set(true);
                break;
            case "false":
            case "FALSE":
            case "False":
                value.set(true);
                break;
            default:
                return opt.error("'" + arg
                        + "' is invalid value for boolean argument! Try 0 or 1");
        }
        return false;
    }

    @Override
    public <T> void initialize(Option<T> opt)
    {
        optName = opt.optionName;
    }

    @Override
    public ValueExpected getValueExpectedFlagDefault()
    {
        return ValueExpected.ValueOptional;
    }

    /**
     * Do not print value at all.
     * @return
     */
    @Override
    public String getValueName()
    {
        return null;
    }
}
