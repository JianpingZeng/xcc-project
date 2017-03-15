package jlang.basic;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2016, Xlous
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

import org.apache.commons.cli.*;

import java.util.ArrayList;
import java.util.Properties;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class CustomParser extends DefaultParser
{
    @Override
    public CommandLine parse(Options options, String[] arguments,
            Properties properties, boolean stopAtNonOption)
            throws ParseException
    {
        CommandLine cmdline = super.parse(options, arguments, properties, stopAtNonOption);
        // Takes the Options as the input, to check its legality.
        ArrayList<Option> opts = new ArrayList<>();
        for (OptionGroup grp :options.getOptionGroups())
        {
            opts.addAll(grp.getOptions());
        }

        opts.addAll(options.getOptions());
        for (Option opt : opts)
        {
            if (opt instanceof CustomOption)
            {
                CustomOption cusOpt = (CustomOption)opt;

                // skip those CustomOpition that has not validator assigned.
                if (cusOpt.validator == null)
                    continue;
                if(!cusOpt.validator.test(cusOpt))
                {
                    throw new ParseException("Invalid option value: " + cusOpt.toString());
                }
            }
        }
        return cmdline;
    }
}
