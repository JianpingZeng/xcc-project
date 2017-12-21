package backend.codegen;
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
import backend.pass.RegisterPass;
import backend.target.TargetMachine;
import backend.value.Function;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class MachineFunctionAnalysis implements FunctionPass
{
    static
    {
        new RegisterPass("machine-function-analysis",
                "Machine Function Analysis", MachineFunctionAnalysis.class,
                false, true);
    }
    private TargetMachine tm;
    private TargetMachine.CodeGenOpt optLevel;
    private MachineFunction mf;
    private AnalysisResolver resolver;

    @Override
    public void setAnalysisResolver(AnalysisResolver resolver)
    {
        this.resolver = resolver;
    }

    @Override
    public AnalysisResolver getAnalysisResolver()
    {
        return resolver;
    }

    public MachineFunctionAnalysis(
            TargetMachine tm)
    {
        this(tm, TargetMachine.CodeGenOpt.Default);
    }

    public MachineFunctionAnalysis(
            TargetMachine tm,
            TargetMachine.CodeGenOpt level)
    {
        this.tm = tm;
        optLevel = level;
        mf = null;
    }

    @Override
    public String getPassName()
    {
        return "Generates MachineFunction for each LLVM function";
    }

    @Override
    public boolean runOnFunction(Function f)
    {
        assert mf == null:"MachineFunctionAnalysis already initialized!";
        mf = new MachineFunction(f, tm);
        return false;
    }
}
