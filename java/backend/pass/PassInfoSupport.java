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

import java.util.HashMap;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class PassInfoSupport
{
    /**
     * Mapping from Class information to corresponding PassInfo instance.
     * Instantiated by class {@linkplain RegisterPass}.
     */
    public static HashMap<Class, PassInfo> passInfoMap = new HashMap<>();


    /**
     * This keeps track of which passes implement the interfaces
     * that are required by the current pass (to implement getAnalysisToUpDate()).
     */
    //public ArrayList<Pair<PassInfo, Pass>> analysisImpls;

    public static PassInfo getPassInfo(Class klass)
    {
        return lookupPassInfo(klass);
    }

    public static PassInfo lookupPassInfo(Class<?> analysisClass)
    {
        if (passInfoMap == null) return null;
        PassInfo res = passInfoMap.get(analysisClass);
        if (passInfoMap.containsKey(analysisClass))
            return res;
        return null;
    }
}
