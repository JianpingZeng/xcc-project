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

import tools.Util;
import java.util.ArrayList;

import static tools.commandline.FormattingFlags.FormattingMask;
import static tools.commandline.FormattingFlags.NormalFormatting;
import static tools.commandline.MiscFlags.MiscMask;
import static tools.commandline.NumOccurrences.OccurrencesMask;
import static tools.commandline.OptionHidden.HiddenMask;
import static tools.commandline.ValueExpected.ValueMask;

/**
 *
 * @author Xlous.zeng
 * @version 0.1
 * indicates the type of value attached to this option if available.
 */
public abstract class Option<T>
{
    protected abstract boolean handleOccurrence(int pos, String optionName, String arg);

    protected ValueExpected getValueExpectedDefault()
    {
        return ValueExpected.ValueOptional;
    }

    /**
     * The number of times specified.
     */
    private int numOccurrences;
    /**
     * Flags for this option.
     */
    private int flags;

    /**
     * Position of the last occurrence.
     */
    private int position;
    /**
     * Greater than 0 for multi-values option.
     */
    private int additionalVals;

    /**
     * Singleed linked list of registered option.
     */
    private Option<?> nextRegistered;

    /**
     * The asmName of this option, like "help", "O".
     */
    public String optionName;
    /**
     * The text text for this option which would be printed out when user
     * type "-help" argument from command line.
     */
    public String helpStr;
    /**
     * It describes what the value of this option is.
     */
    public T value;

    public String valueStr;

    protected Parser<T> parser;

    protected Option(Parser<T> parser, int defaultFlags)
    {
        this.parser = parser;
        flags = defaultFlags | NormalFormatting.value;
        optionName = "";
        helpStr = "";
        value = null;
        Util.assertion(getNumOccurrencesFlag() != null,  "Not all default flags specified!");
    }

    public NumOccurrences getNumOccurrencesFlag()
    {
        int no = flags & OccurrencesMask.value;
        return NumOccurrences.getFromValue(no);
    }

    public ValueExpected getValueExpectedFlag()
    {
        int ve = flags & ValueMask.value;
        ValueExpected VE = ValueExpected.getFromValue(ve);
        return VE != null ? VE
                : getValueExpectedDefault();
    }

    public FormattingFlags getFormattingFlag()
    {
        int ff = flags & FormattingMask.value;
        return FormattingFlags.getFromValue(ff);
    }

    public OptionHidden getOptionHiddenFlag()
    {
        int oh = flags & HiddenMask.value;
        return OptionHidden.getFromValue(oh);
    }

    public int getPosition()
    {
        return position;
    }

    public int getNumAdditionalVals()
    {
        return additionalVals;
    }

    public void setOptionName(String name)
    {
        optionName = name;
    }

    public void setHelpStr(String helpStr)
    {
        this.helpStr = helpStr;
    }

    public void setValue(T val)
    {
        value = val;
    }

    public void setFlag(int flag, int flagMask)
    {
        flags &= ~flagMask;
        flags |= flag;
    }

    public void setFormattingFlag(FormattingFlags flag)
    {
        setFlag(flag.value, FormattingMask.value);
    }

    public void setValueExpectedFlag(ValueExpected flag)
    {
        setFlag(flag.value, ValueMask.value);
    }

    public void setNumOccurrencesFlag(NumOccurrences flag)
    {
        setFlag(flag.value, OccurrencesMask.value);
    }

    public void setMiscFlag(MiscFlags mf)
    {
        setFlag(mf.value, MiscMask.value);
    }

    public void setOptionHiddenFlag(OptionHidden hidden)
    {
        setFlag(hidden.value, HiddenMask.value);
    }

    public void setPosition(int pos)
    {
        this.position = pos;
    }

    public int getNumOccurrences()
    {
        return numOccurrences;
    }

    public boolean error(String message)
    {
        return error(message, null);
    }

    /**
     * Prints option asmName followed by message.
     * @param message
     * @param optName
     * @return Always return true.
     */
    public boolean error(String message, String optName)
    {
        if (optName == null) optName = optionName;
        if (optName.isEmpty())
            System.err.println(helpStr);
        else
            System.err.printf("xcc: for the - %s\n", optName);
        System.err.printf(" option: %s\n", message);
        return true;
    }

    protected void setNumAdditionalVals(int val)
    {
        additionalVals = val;
    }

    public abstract void setInitializer(T val);

    public boolean hasOptionName()
    {
        return !optionName.isEmpty();
    }

    /**
     * Returns the width of this option for printing.
     * @return
     */
    public abstract int getOptionWidth();

    public abstract void printOptionInfo(int globalWidth);

    public abstract void getExtraOptionNames(ArrayList<String> names);

    public void addArgument()
    {
        Util.assertion(nextRegistered == null, "Could not register option multiple");

        nextRegistered = CL.registeredOptionList;
        CL.registeredOptionList = this;
        CL.markOptionsChanged();
    }

    public Option<?> getNextRegisteredOption()
    {
        return nextRegistered;
    }

    public MiscFlags getMiscFlags()
    {
        return MiscFlags.getFromValue(flags & MiscMask.value);
    }

    public boolean addOccurrence(int pos, String argName, String value)
    {
        return addOccurrence(pos, argName, value, false);
    }
    /**
     * Wrapper around handleOccurrence that enforces Flags.
     * @return
     */
    public boolean addOccurrence(int pos, String argName, String value, boolean multiArg)
    {
        if (!multiArg)
            numOccurrences++;

        switch (getNumOccurrencesFlag())
        {
            case Optional:
                if (numOccurrences > 1)
                    return error("may only occur zero or one times!", argName);
                break;
            case Required:
                if (numOccurrences > 1)
                    return error("must occur exactly one time!", argName);
                // Fall throuth
            case OneOrMore:
            case ZeroOrMore:
            case ConsumeAfter:
                break;
            default:
                return error("bad num occurrences flag value!");
        }
        return handleOccurrence(pos, argName, value);
    }

    public String getValueStr()
    {
        return valueStr;
    }

    public void setValueStr(String valueStr)
    {
        this.valueStr = valueStr;
    }

    public Parser<T> getParser()
    {
        return parser;
    }
}
