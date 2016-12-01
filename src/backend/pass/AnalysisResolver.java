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

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public interface AnalysisResolver
{
    Pass getAnalysisOrNull(PassInfo passInfo);

    void addPass(ImmutablePass ip, AnalysisUsage au);

    /**
     * Return the analysis result which must be existed.
     * @param pi
     * @return
     */
    default Pass getAnalysis(PassInfo pi)
    {
        Pass res = getAnalysisOrNull(pi);
        assert res != null:"Pass has an incorrent pass used yet!";
        return res;
    }

    /**
     * Return an analysis pass or null if it is not existed.
     * @param passInfo
     * @return
     */
    default Pass getAnalysisToUpdate(PassInfo passInfo)
    {
        return getAnalysisOrNull(passInfo);
    }

    void markPassUsed(PassInfo passInfo, Pass user);
}
