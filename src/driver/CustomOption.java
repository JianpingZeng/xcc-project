package driver;
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

import org.apache.commons.cli.Option;

import java.util.function.Predicate;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class CustomOption extends Option
{
    /**
     * This predicate is used for checking the correctess of the option value.
     */
    Predicate<CustomOption> validator;
    /**
     * Creates an Option using the specified parameters.
     * The option does not take an argument.
     *
     * @param opt         short representation of the option
     * @param description describes the function of the option
     * @throws IllegalArgumentException if there are any non valid
     *                                  Option characters in <code>opt</code>.
     */
    public CustomOption(String opt, String description)
            throws IllegalArgumentException
    {
        super(opt, description);
    }

    public CustomOption(String opt, String description,
            Predicate<CustomOption> validator)
    {
        this(opt, description);
        this.validator = validator;
    }

    /**
     * Creates an Option using the specified parameters.
     *
     * @param opt         short representation of the option
     * @param hasArg      specifies whether the Option takes an argument or not
     * @param description describes the function of the option
     * @throws IllegalArgumentException if there are any non valid
     *                                  Option characters in <code>opt</code>.
     */
    public CustomOption(String opt, boolean hasArg, String description)
            throws IllegalArgumentException
    {
        super(opt, hasArg, description);
    }

    public CustomOption(String opt, boolean hasArg, String description,
            Predicate<CustomOption> validator)
    {
        this(opt, hasArg, description);
        this.validator = validator;
    }

    /**
     * Creates an Option using the specified parameters.
     *
     * @param opt         short representation of the option
     * @param longOpt     the long representation of the option
     * @param hasArg      specifies whether the Option takes an argument or not
     * @param description describes the function of the option
     * @throws IllegalArgumentException if there are any non valid
     *                                  Option characters in <code>opt</code>.
     */
    public CustomOption(String opt, String longOpt, boolean hasArg,
            String description) throws IllegalArgumentException
    {
        super(opt, longOpt, hasArg, description);
    }

    public CustomOption(String opt, String longOpt, boolean hasArg,
            String description, Predicate<CustomOption> predicator)
    {
        this(opt, longOpt, hasArg, description);
        this.validator = validator;
    }
}
