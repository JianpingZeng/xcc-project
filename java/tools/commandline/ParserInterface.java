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

import java.util.ArrayList;

import static tools.commandline.ValueExpected.ValueRequired;

/**
 * This file defines  an interface for providing some useful methods with Command
 * line parser.
 * @author Xlous.zeng
 * @version 0.1
 */
public interface ParserInterface<T>
{
    boolean parse(Option<?> opt,
            String optName, String arg,
            OutParamWrapper<T> val);

    default String getValueStr(Option<?> opt, String defaultMsg)
    {
        if (opt.value == null)
            return defaultMsg;
        return String.valueOf(opt.value);
    }
    /**
     * Determines the width of the option tag for printing.
     * @param opt
     * @return
     */
    default int getOptionWidth(Option<?> opt)
    {
        int len = opt.optionName.length();
        String valName = getValueName();
        if (valName != null)
            len += getValueStr(opt, valName).length();
        return len + 6;
    }

    /**
     * Print out information of the given {@code opt}.
     * @param opt
     * @param globalWidth
     */
    default void printOptionInfo(Option<?> opt, int globalWidth)
    {
        System.out.printf("  -%s", opt.optionName);

        String valueName = getValueName();
        if (valueName != null)
            System.out.printf("=<%s>", getValueStr(opt, valueName));

        System.out.printf("%s - %s\n",
                String.format("%1$" + (globalWidth - getOptionWidth(opt)) + "s", ' '),
                opt.helpStr);
    }

    <T> void initialize(Option<T> opt);

    default void getExtraOptionNames(ArrayList<String> optionNames)
    {}

    default ValueExpected getValueExpectedFlagDefault()
    {
        return ValueRequired;
    }

    /**
     * Overloaded in subclass to provide a better default value.
     * @return
     */
    default String getValueName()
    {
        return "value";
    }
}
