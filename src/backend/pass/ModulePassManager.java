package backend.pass;
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

import backend.hir.Module;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class ModulePassManager extends PassManagerBase<Module, ModulePass>
{
    @Override
    public String getPMName()
    {
        return "Module pass manager!";
    }

    @Override
    public boolean runPass(ModulePass mp, Module m)
    {
        return mp.runOnModule(m);
    }

    public void addPass(FunctionPass pass, AnalysisUsage au)
    {

    }
}
