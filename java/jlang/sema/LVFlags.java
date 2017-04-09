package jlang.sema;
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

/**
 * Flags controlling the computation of linkage and visibility.
 * @author xlous.zeng
 * @version 0.1
 */
public class LVFlags
{
	public boolean considerGlobalVisibility;
	public boolean considerVisibilityAttributes;
	public boolean considerTemplateParameterTypes;

	public LVFlags()
	{

		considerGlobalVisibility = true;

		considerVisibilityAttributes = true;

		considerTemplateParameterTypes = true;
	}

	/// \brief Returns a set of flags that is only useful for computing the
	/// linkage, not the visibility, of a declaration.
	public static LVFlags createOnlyDeclLinkage()
	{
		LVFlags F = new LVFlags();
		F.considerGlobalVisibility = false;
		F.considerVisibilityAttributes = false;
		F.considerTemplateParameterTypes = false;
		return F;
	}

	/// Returns a set of flags, otherwise based on these, which ignores
	/// off all sources of visibility except template arguments.
	public LVFlags onlyTemplateVisibility()
	{
		LVFlags F = this;
		F.considerGlobalVisibility = false;
		F.considerVisibilityAttributes = false;
		F.considerTemplateParameterTypes = false;
		return F;
	}
}
