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

import java.util.HashMap;

public final class PassDataBase
{
	private static  HashMap<PassInfo, Pass> registeredPasses = new HashMap<>();

	/**
	 * A registeration interface to client.
	 * @param pass
	 * @param name
	 */
	public static void registerPass(Class<?> pass, String name)
	{
	}

	public static Pass getAnalysisOrNull(PassInfo passInfo)
	{
		if (passInfo == null)
			return null;
		return registeredPasses.getOrDefault(passInfo, null);
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
