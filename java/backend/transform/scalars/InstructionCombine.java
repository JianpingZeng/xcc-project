package backend.transform.scalars;
/*
 * Extremely C language Compiler
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

import backend.pass.AnalysisResolver;
import backend.pass.FunctionPass;
import backend.value.Function;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class InstructionCombine implements FunctionPass
{
    private AnalysisResolver resolver;

    @Override
    public boolean runOnFunction(Function f)
    {
        return false;
    }

    @Override
    public String getPassName()
    {
        return "Instruction Combiner";
    }

    @Override
    public AnalysisResolver getAnalysisResolver()
    {
        return resolver;
    }

    @Override
    public void setAnalysisResolver(AnalysisResolver resolver)
    {
        this.resolver = resolver;
    }

    public static FunctionPass createInstructionCombinePass()
    {
        return new InstructionCombine();
    }
}
