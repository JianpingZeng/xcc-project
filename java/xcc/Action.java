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

import static xcc.Action.ActionClass.*;

public class Action
{
    public String getClassName()
    {
        return getKind().name();
    }

    public enum ActionClass
    {
        InputClass, BindArchClass, PreprocessJobClass, PrecompileJobClass, AnalyzeJobClass, CompileJobClass, AssembleJobClass, LinkJobClass, LipoJobClass;

        public static final ActionClass JobClassFirst = PreprocessJobClass;
        public static final ActionClass JobClassLast = LipoJobClass;
    }

    private ActionClass kind;
    private int outputType;

    private ArrayList<Action> inputs;
    public Action(ActionClass kind, int outputType)
    {
        this.kind = kind;
        this.outputType = outputType;
        inputs = new ArrayList<>();
    }

    public Action(ActionClass kind, Action input, int outputType)
    {
        this.kind = kind;
        this.outputType = outputType;
        inputs = new ArrayList<>();
        inputs.add(input);
    }

    public Action(ActionClass kind, ArrayList<Action> inputs, int outputType)
    {
        this.kind = kind;
        this.outputType = outputType;
        this.inputs = new ArrayList<>();
        this.inputs.addAll(inputs);
    }

    public int getOutputType()
    {
        return outputType;
    }

    public ActionClass getKind()
    {
        return kind;
    }

    public ArrayList<Action> getInputs()
    {
        return inputs;
    }

    public static class InputAction extends Action
    {
        private Arg input;
        public InputAction(Arg input, int outputType)
        {
            super(InputClass, outputType);
            this.input = input;
        }
    }

    public static class BindArchAction extends Action
    {
        private String archName;
        public BindArchAction(Action input, String archName)
        {
            super(BindArchClass, input, input.getOutputType());
            this.archName = archName;
        }
    }

    public static class JobAction extends Action
    {
        public JobAction(ActionClass kind, Action input, int type)
        {
            super(kind, input, type);
        }

        public JobAction (ActionClass kind, ArrayList<Action> inputs, int type)
        {
            super(kind, inputs, type);
        }
    }

    public static class PreprocessJobAction extends JobAction
    {
        public PreprocessJobAction(Action input, int outputType)
        {
            super(PreprocessJobClass, input, outputType);
        }
    }

    public static class PrecompileJobAction extends JobAction
    {
        public PrecompileJobAction(Action input, int outputType)
        {
            super(PrecompileJobClass, input, outputType);
        }
    }

    public static class CompileJobAction extends JobAction
    {
        public CompileJobAction(Action input, int type)
        {
            super(CompileJobClass, input, type);
        }
    }

    public static class AssembleJobAction extends JobAction
    {
        public AssembleJobAction(Action input, int type)
        {
            super(AssembleJobClass, input, type);
        }
    }

    public static class LinkJobAction extends JobAction
    {
        public LinkJobAction(ArrayList<Action> inputs, int type)
        {
            super(LinkJobClass, inputs, type);
        }
    }
}
