package backend.passManaging;
/*
 * Xlous C language Compiler
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

import backend.pass.AnalysisUsage;
import backend.pass.ImmutablePass;
import backend.pass.Pass;

import java.util.ArrayList;

/**
 * The interface to PMTopLevelManager.
 * @author Xlous.zeng
 * @version 0.1
 */
public interface IPMTopLevelManager
{
    void schedulePass(Pass p);
    
    void addTopLevelPass(Pass p);

    void setLastUser(ArrayList<Pass> analysisPasses, Pass p);

    void collectLastUses(ArrayList<Pass> lastUsers, Pass p);
    
    Pass findAnalysisPass();

    AnalysisUsage findAnalysisUsage(Pass p);

    void addImmutablePass(ImmutablePass p);

    ArrayList<ImmutablePass> getImmutablePasses();

    void addPassManager(PMDataManager pm);
    
    void dumpPasses();

    void dumpArguments();
    
    void initializeAllAnalysisInfo();
}
