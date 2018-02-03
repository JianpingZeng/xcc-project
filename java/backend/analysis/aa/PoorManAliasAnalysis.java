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

package backend.analysis.aa;

import backend.pass.AnalysisResolver;
import backend.support.CallSite;
import backend.value.Value;

import java.util.ArrayList;

/**
 * This file defines a class which implements a well known pointer analysis
 * algorithm named of "Poor's man pointer analysis".
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public final class PoorManAliasAnalysis extends AliasAnalysis
{
    @Override
    public AliasResult alias(Value ptr1, int size1, Value ptr2, int size2)
    {
        return null;
    }

    @Override
    public void getMustAliases(Value ptr, ArrayList<Value> retVals)
    {

    }

    @Override
    public boolean pointsToConstantMemory(Value ptr)
    {
        return false;
    }

    @Override
    public ModRefResult getModRefInfo(CallSite cs1, CallSite cs2)
    {
        return null;
    }

    @Override
    public boolean hasNoModRefInfoForCalls()
    {
        return false;
    }

    @Override
    public void deleteValue(Value val)
    {

    }

    @Override
    public void copyValue(Value from, Value to)
    {

    }

    @Override
    public String getPassName()
    {
        return "Poor's man alias analysis Pass";
    }

    @Override
    public AnalysisResolver getAnalysisResolver()
    {
        return null;
    }

    @Override
    public void setAnalysisResolver(AnalysisResolver resolver)
    {

    }
}
