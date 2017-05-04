package jlang.basic;
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
 * This class represents a character granular source range.
 * The underlying SourceRange can either specify the starting/ending character
 * of the range, or it can specify the start or the range and the start of the
 * last token of the range (a "token range").  In the token range case, the
 * size of the last token must be measured to determine the actual end of the
 * range.
 * @author xlous.zeng
 * @version 0.1
 */
public final class CharSourceRange 
{
	private SourceRange range;
	private boolean isTokenRange;
	
	public CharSourceRange()
	{
		super();
	}

	public CharSourceRange(SourceRange r)
	{
		this(r, true);
	}
	
	public CharSourceRange(SourceRange r, boolean itr)
	{
		super();
		range = r;
		isTokenRange = itr;
	}

	public static CharSourceRange getTokenRange(SourceRange r)
	{
		CharSourceRange result = new CharSourceRange();
		result.range = r;
		result.isTokenRange = true;
		return result;
	}

	public static CharSourceRange getCharRange(SourceRange r) {
		CharSourceRange result = new CharSourceRange();
		result.range = r;
		result.isTokenRange = false;
		return result;
	}

	public static CharSourceRange getTokenRange(SourceLocation begin, SourceLocation end)
	{
		return getTokenRange(new SourceRange(begin, end));
	}
	
	public static CharSourceRange getCharRange(SourceLocation begin, SourceLocation end) {
		return getCharRange(new SourceRange(begin, end));
	}

	/**
	 * Return true if the end of this range specifies the start of
	 * the last token.  Return false if the end of this range specifies the last
	 * character in the range.
	 * @return
	 */
	public boolean isTokenRange() 
	{
		return isTokenRange;
	}

	public SourceLocation getBegin() 
	{
		return range.getBegin();
	}
	
	public SourceLocation getEnd() 
	{
		return range.getEnd();
	}
	
	public SourceRange getAsRange() 
	{
		return range;
	}

	public void setBegin(SourceLocation b) 
	{ 
		range.setBegin(b);
	}
	
	public void setEnd(SourceLocation e) 
	{
		range.setEnd(e);
	}
	
	public boolean isValid() 
	{
		return range.isValid();
	}
	public boolean isInvalid() 
	{
		return !isValid(); 
	}
}

