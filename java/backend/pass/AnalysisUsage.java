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

import java.util.HashSet;

import static backend.pass.PassDataBase.getPassInfo;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class AnalysisUsage
{
    private HashSet<PassInfo> required, preserved;
    private boolean preservedAll;
    public AnalysisUsage()
    {
        required = new HashSet<>();
        preserved = new HashSet<>();
    }

    public AnalysisUsage addRequired(Class<? extends Pass> reqPass)
    {
        PassInfo info = getPassInfo(reqPass);
        assert info != null:"Pass is not registered!";
        return addRequiredPassInfo(info);
    }

    public AnalysisUsage addRequiredPassInfo(PassInfo info)
    {
        if (!required.contains(info))
            required.add(info);
        return this;
    }

    public AnalysisUsage addPreserved(Class<? extends Pass> prePass)
    {
        PassInfo info = getPassInfo(prePass);
        assert info != null :"Pass is not registered";
        return addPreservedPassInfo(info);
    }

    public AnalysisUsage addPreservedPassInfo(PassInfo info)
    {
        if (!preserved.contains(info))
            preserved.add(info);
        return this;
    }

    public void setPreservedAll() {preservedAll = true;}
    public boolean getPreservedAll() {return preservedAll;}

    public HashSet<PassInfo> getRequired() {return required;}

    public HashSet<PassInfo> getPreserved() {return preserved;}
}
