/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package xcc;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class InputInfo
{
    public enum InputInfoClass
    {
        Nothing,
        Filename,
        InputArg,
        Pipe
    }

    /**
     * The input data, could be filename, inputArg or pipe.
     */
    private Object data;
    private InputInfoClass kind;
    private int outputType;
    private String baseInput;

    public InputInfo(int outputType, String baseInput)
    {
        this.kind = InputInfoClass.Nothing;
        this.outputType = outputType;
        this.baseInput = baseInput;
    }

    public InputInfo(String filename, int outputType, String baseInput)
    {
        data = filename;
        this.kind = InputInfoClass.Filename;
        this.outputType = outputType;
        this.baseInput = baseInput;
    }

    public InputInfo(Arg arg, int outputType, String baseInput)
    {
        data = arg;
        kind = InputInfoClass.InputArg;
        this.outputType = outputType;
        this.baseInput = baseInput;
    }

    public InputInfo(Job.PipedJob pj, int outputType, String baseInput)
    {
        data = pj;
        kind = InputInfoClass.InputArg;
        this.outputType = outputType;
        this.baseInput = baseInput;
    }

    public boolean isNothing()
    {
        return kind == InputInfoClass.Nothing;
    }

    public boolean isFilename()
    {
        return kind == InputInfoClass.Filename;
    }

    public boolean isInputArg()
    {
        return kind == InputInfoClass.InputArg;
    }

    public boolean isPipe()
    {
        return kind == InputInfoClass.Pipe;
    }

    public int getOutputType()
    {
        return outputType;
    }

    public String getBaseInput()
    {
        return baseInput;
    }

    public String getFilename()
    {
        assert isFilename();
        return (String)data;
    }

    public Arg getInputArg()
    {
        assert isInputArg();
        return (Arg)data;
    }

    public Job.PipedJob getPipe()
    {
        assert isPipe();
        return (Job.PipedJob)data;
    }

    public String toString()
    {
        switch (kind)
        {
            case Pipe:
                return "(pipe)";
            case Filename:
                return "\"" + getFilename() + "\"";
            case InputArg:
                return "(input arg)";
            default:
                return "(nothing)";
        }
    }
}
