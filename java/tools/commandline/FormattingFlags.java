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

/**
 * This controls special features that the option might have
 * that cause it to be parsed differently...
 * <p>
 * <b>Prefix</b> - This option allows arguments that are otherwise unrecognized to be
 * matched by options that are a prefix of the actual value.  This is useful for
 * cases like a linker, where options are typically of the form '-lfoo' or
 * '-L../../include' where -l or -L are the actual flags.  When prefix is
 * enabled, and used, the value for the flag comes from the suffix of the
 * argument.
 * </p>
 * <p>
 * <b>Grouping</b> - With this option enabled, multiple letter options are allowed to
 * bunch together with only a single hyphen for the whole group.  This allows
 * emulation of the behavior that ls uses for example: ls -la === ls -l -a
 * </p>
 * @author Xlous.zeng
 * @version 0.1
 */
public enum FormattingFlags
{
    NormalFormatting(0x000),     // Nothing special
    Positional(0x080),            // Is a positional argument, no '-' required
    Prefix(0x100),                // Can this option directly prefix its value?
    Grouping(0x180),              // Can this option group with other options?

    FormattingMask(0x180);

    public int value;
    FormattingFlags(int val)
    {
        value = val;
    }

    public static FormattingFlags getFromValue(int val)
    {
        for(FormattingFlags ff : values())
            if (ff.value == val)
                return ff;
        return null;
    }
}
