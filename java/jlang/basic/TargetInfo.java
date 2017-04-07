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
 * @author xlous.zeng
 * @version 0.1
 */
public class TargetInfo
{
	private String triple;

	protected boolean TLSSupported;
	protected int pointerWidth, pointerAlign;
	protected int charWidth, charAlign;
	protected int shortWidth, shortAlign;
	protected int intWidth, intAlign;
	protected int floatWidth, floatAlign;
	protected int doubleWidth, doubleAlign;
	protected int longDoubleWidth, longDoubleAlign;
	protected int longWidth, longAlign;
	protected int longlongWidth, longlongAlign;
	protected String descriptionString;

	public String getTriple()
	{
		return triple;
	}

	public void setTriple(String triple)
	{
		this.triple = triple;
	}

	public boolean isTLSSupported() {return  TLSSupported;}
	public int getPointerWidth() {return pointerWidth; }

	public int getPointerAlign()
	{
		return pointerAlign;
	}

	public int getCharWidth()
	{
		return charWidth;
	}

	public int getCharAlign()
	{
		return charAlign;
	}

	public int getShortWidth()
	{
		return shortWidth;
	}

	public int getShortAlign()
	{
		return shortAlign;
	}

	public int getIntAlign()
	{
		return intAlign;
	}

	public int getIntWidth()
	{
		return intWidth;
	}

	public int getFloatAlign()
	{
		return floatAlign;
	}

	public int getFloatWidth()
	{
		return floatWidth;
	}

	public int getDoubleAlign()
	{
		return doubleAlign;
	}

	public int getDoubleWidth()
	{
		return doubleWidth;
	}

	public int getLongWidth()
	{
		return longWidth;
	}

	public int getLongAlign()
	{
		return longAlign;
	}

	public int getLongDoubleAlign()
	{
		return longDoubleAlign;
	}

	public int getLongDoubleWidth()
	{
		return longDoubleWidth;
	}

	public int getLonglongAlign()
	{
		return longlongAlign;
	}

	public int getLonglongWidth()
	{
		return longlongWidth;
	}

	public String getDescriptionString()
	{
		return descriptionString;
	}
}
