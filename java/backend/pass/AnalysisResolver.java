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

import backend.passManaging.PMDataManager;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class AnalysisResolver
{
    private PMDataManager pm;
    public AnalysisResolver(PMDataManager pmd)
    {
        pm = pmd;
    }

    public PMDataManager getPMDataManager()
    {
        return pm;
    }

    /**
     * Return the analysis result or null if it doesn't exist.
     * @param passInfo
     * @return
     */
    public Pass getAnalysisIfAvailable(PassInfo passInfo, boolean dir)
    {
        return pm.findAnalysisPass(passInfo, dir);
    }
}
