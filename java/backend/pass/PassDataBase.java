package backend.pass;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Xlous
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

import tools.Util;

import java.util.HashMap;

public final class PassDataBase
{
    /**
     * Mapping from Class information to corresponding PassInfo instance.
     * Instantiated by class {@linkplain RegisterPass}.
     */
    private static HashMap<Class<? extends Pass>, PassInfo> passInfoMap;

    private static HashMap<PassInfo, Class<? extends Pass>> registeredPasses;

	/**
	 * A registeration interface to client.
	 * @param pass
	 * @param pi
	 */
	public static void registerPass(Class<? extends Pass> pass, PassInfo pi)
	{
		if (registeredPasses == null)
			registeredPasses = new HashMap<>();
        if (passInfoMap == null)
            passInfoMap = new HashMap<>();
        if (Util.DEBUG)
        {
            if (!registeredPasses.containsKey(pi) && passInfoMap
                    .containsKey(pass))
            {
                pi.dump();
            }
        }
        else
        {
            assert !registeredPasses.containsKey(pi) && passInfoMap
                    .containsKey(pass) : "Pass already registered!";
        }
		registeredPasses.put(pi, pass);
		passInfoMap.put(pass, pi);
	}

    /**
     * This keeps track of which passes implement the interfaces
     * that are required by the current pass (to implement getAnalysisToUpDate()).
     */
    //public ArrayList<Pair<PassInfo, Pass>> analysisImpls;

    public static PassInfo getPassInfo(Class<? extends Pass> klass)
    {
        return lookupPassInfo(klass);
    }

    public static PassInfo lookupPassInfo(Class<? extends Pass> analysisClass)
    {
        if (passInfoMap == null) return null;
        PassInfo res = passInfoMap.get(analysisClass);
        if (passInfoMap.containsKey(analysisClass))
            return res;
        return null;
    }

    public void unregisterPass(PassInfo pi)
	{
		assert pi != null && registeredPasses != null :
				"Pass register factory is uninstantiated as yet!";
		assert registeredPasses.containsKey(pi) :
				"Pass registered but not in register factory!";
		passInfoMap.remove(registeredPasses.remove(pi));
		if (registeredPasses.isEmpty())
			registeredPasses = null;
		if (passInfoMap.isEmpty())
		    passInfoMap = null;
	}

	public static Pass getAnalysisOrNull(PassInfo passInfo)
	{
		if (passInfo == null)
			return null;
		return passInfo.createPass();
	}

	/**
	 * Return the analysis result which must be existed.
	 * @param pi
	 * @return
	 */
	public static Pass getAnalysis(PassInfo pi)
	{
		Pass res = getAnalysisOrNull(pi);
		assert res != null:"Pass has an incorrect pass used yet!";
		return res;
	}

	/**
	 * Return an analysis pass or null if it is not existed.
	 * @param passInfo
	 * @return
	 */
	public static Pass getAnalysisToUpdate(PassInfo passInfo)
	{
		return getAnalysisOrNull(passInfo);
	}
}
