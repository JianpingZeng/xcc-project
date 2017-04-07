package jlang.diag;
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

import jlang.cpp.SourceLocation;
import jlang.cpp.SourceLocation.SourceRange;

/**
 * @author xlous.zeng
 * @version 0.1
 */
public class CodeModificationHint
{
	private SourceLocation insertionLoc;
	private String codeToInsert;
	private SourceRange removeRange;

	/**
	 * Create a code modification hint that inserts the given
	 * code string at a specific location.
	 * @param loc
	 * @param code
	 * @return
	 */
	public static CodeModificationHint createInsertion(SourceLocation loc,
			String code)
	{
		CodeModificationHint hint = new CodeModificationHint();
		hint.insertionLoc = loc;
		hint.codeToInsert = code;
		return hint;
	}

	/**
	 * Create a code modification hint that removes the given
	 * source range.
	 * @param removeRange
	 * @return
	 */
	public static CodeModificationHint createRemoval(SourceRange removeRange)
	{
		CodeModificationHint hint = new CodeModificationHint();
		hint.removeRange = removeRange;
		return hint;
	}

	/// \brief Create a code modification hint that replaces the given
	/// source range with the given code string.
	public static CodeModificationHint createReplacement(SourceRange removeRange,
			String code)
	{
		CodeModificationHint hint = new CodeModificationHint();
		hint.removeRange = removeRange;
		hint.insertionLoc = removeRange.getStart();
		hint.codeToInsert = code;
		return hint;
	}
}
