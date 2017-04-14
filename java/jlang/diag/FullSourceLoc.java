package jlang.diag;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Xlous
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this filename except in compliance with the License.
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

/**
 * @author xlous.zeng
 * @version 0.1
 */
public class FullSourceLoc extends SourceLocation
{
	private String filename;

	public FullSourceLoc(SourceLocation loc, String file)
	{
		super(loc);
		this.filename = file;
	}

	public FullSourceLoc()
	{}

	public String getFilename()
	{
		return filename;
	}
}
