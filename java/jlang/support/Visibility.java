package jlang.support;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous
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
 * @author Jianping Zeng
 * @version 0.1
 */
public enum Visibility
{
	/// Objects with "hidden" visibility are not seen by the dynamic
	/// linker.
	HiddenVisibility,

	/// Objects with "protected" visibility are seen by the dynamic
	/// linker but always dynamically resolve to an object within this
	/// shared object.
	ProtectedVisibility,

	/// Objects with "default" visibility are seen by the dynamic linker
	/// and act like normal objects.
	DefaultVisibility;

	public static Visibility minVisibility(Visibility L, Visibility R)
	{
		return L.ordinal() < R.ordinal() ? L : R;
	}
}
