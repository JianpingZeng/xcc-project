package jlang.sema;
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

import jlang.support.CharSourceRange;
import jlang.support.SourceRange;
import jlang.diag.FixItHint;
import jlang.diag.Diagnostic;
import jlang.diag.Diagnostic.ArgumentKind;
import jlang.type.QualType;

/**
 * @author xlous.zeng
 * @version 0.1
 */
public final class PartialDiagnostic
{
	static class Storage
	{
		Storage()
		{
			diagArgumentsKind = new ArgumentKind[Diagnostic.maxArguments];
			diagArgumentsStr = new String[Diagnostic.maxArguments];
			diagArgumentsVal = new Object[Diagnostic.maxArguments];
			diagRanges = new CharSourceRange[10];
			fixItHints = new FixItHint[Diagnostic.maxFixItHints];
		}

		/**
		 * This contains the number of entries in Arguments.
		 */
		int numDiagArgs;

		/**
		 * This is the number of ranges in the diagRanges array.
		 */
		int numDiagRanges;

		/**
		 * The number of code modifications hints in the
		 * fixItHints array.
		 */
		int numFixItHints;

		/**
		 * This is an array of ArgumentKind::ArgumentKind enum
		 * values, with one for each argument.  This specifies whether the argument
		 * is in diagArgumentsStr or in DiagArguments.
		 */
		ArgumentKind[] diagArgumentsKind;

		/**
		 * The values for the various substitution positions.
		 * This is used when the argument is not an String. The specific value
		 * is mangled into an intptr_t and the interpretation depends on exactly
		 * what sort of argument kind it is.
		 */
		Object[] diagArgumentsVal;

		/**
		 * The values for the various substitution positions that have
		 * string arguments.
		 */
		String[] diagArgumentsStr;

		/**
		 * The list of ranges added to this diagnostic.  It currently
		 * only support 10 ranges, could easily be extended if needed.
		 */
		CharSourceRange[] diagRanges;

		/**
		 * If valid, provides a hint with some code
		 * to insert, remove, or modify at a particular position.
		 */
		FixItHint[] fixItHints;
	}

	// NOTE: Sema assumes that PartialDiagnostic is location-invariant
	// in the sense that its bits can be safely memcpy'ed and destructed
	// in the new location.

	/**
	 * The diagnostic ID.
	 */
	private int diagID;

	/**
	 * Storage for args and ranges.
	 */
	private Storage diagStorage;

	/**
	 * Retrieve storage for this particular diagnostic.
	 * @return
	 */
	public Storage getStorage()
	{
		if (diagStorage != null)
			return diagStorage;
		diagStorage = new Storage();
		return diagStorage;
	}

	public PartialDiagnostic addSourceRange(SourceRange r)
	{
		return addSourceRange(new CharSourceRange(r));
	}

	public PartialDiagnostic addSourceRange(CharSourceRange r)
	{
		if (diagStorage == null)
			diagStorage = getStorage();

		assert diagStorage.numDiagRanges < diagStorage.diagRanges.length :
				"Too many arguments to diagnostic!";
		diagStorage.diagRanges[diagStorage.numDiagRanges++] = r;
		return this;
	}

	public void addFixItHint(FixItHint hint)
	{
		if (hint.isNull())
			return;

		if (diagStorage == null)
			diagStorage = getStorage();
		assert diagStorage.numFixItHints < Diagnostic.maxFixItHints :
				"Too many code modification hints!";
		if (diagStorage.numFixItHints >= Diagnostic.maxFixItHints)
			return;  // Don't crash in release builds
		diagStorage.fixItHints[diagStorage.numFixItHints++] = hint;
	}

	public PartialDiagnostic(int diagID)
	{
		this.diagID = diagID;
	}

	public PartialDiagnostic(Diagnostic Other)
	{
		this.diagID = Other.getID();
		// Copy arguments.
		for (int I = 0, N = Other.getNumDiagArgs(); I != N; ++I)
		{
			if (Other.getDiagArgKind(I) == ArgumentKind.ak_std_string)
				addString(Other.getArgStdStr(I));
			else
				addTaggedVal(Other.getRawArg(I), Other.getDiagArgKind(I));
		}

		// Copy source ranges.
		for (int I = 0, N = Other.getNumDiagRanges(); I != N; ++I)
			addSourceRange(new CharSourceRange(Other.getRange(I)));

		// Copy fix-its.
		for (int I = 0, N = Other.getNumFixItHints(); I != N; ++I)
			addFixItHint(Other.getFixItHint(I));
	}


	public int getDiagID() { return diagID; }

	public PartialDiagnostic addTaggedVal(Object V, ArgumentKind Kind)
	{
		if (diagStorage == null)
			diagStorage = getStorage();
		assert diagStorage.numDiagArgs < Diagnostic.maxArguments
				: "Too many arguments to diagnostic!";
		diagStorage.diagArgumentsVal[diagStorage.numDiagArgs] = V;
		diagStorage.diagArgumentsKind[diagStorage.numDiagArgs++] = Kind;
		return this;
	}

	public PartialDiagnostic addString(String V)
	{
		if (diagStorage == null)
			diagStorage = getStorage();

		assert diagStorage.numDiagArgs < Diagnostic.maxArguments
				:"Too many arguments to diagnostic!";
		diagStorage.diagArgumentsKind[diagStorage.numDiagArgs] = ArgumentKind.ak_std_string;
		diagStorage.diagArgumentsStr[diagStorage.numDiagArgs++] = V;
		return this;
	}

	public void emit(Diagnostic.DiagnosticBuilder db)
	{
		if (diagStorage == null)
			return;
	
		// Add all arguments.
		for (int i = 0, e = diagStorage.numDiagArgs; i != e; ++i)
		{
			if (diagStorage.diagArgumentsKind[i] == ArgumentKind.ak_std_string)
				db.addString(diagStorage.diagArgumentsStr[i]);
			else
				db.addTaggedVal(diagStorage.diagArgumentsVal[i], diagStorage.diagArgumentsKind[i]);
		}
	
		// Add all ranges.
		for (int i = 0, e = diagStorage.numDiagRanges; i != e; ++i)
				db.addSourceRange(diagStorage.diagRanges[i].getAsRange());
	
		// Add all fix-its.
		for (int i = 0, e = diagStorage.numFixItHints; i != e; ++i)
				db.addFixItHint(diagStorage.fixItHints[i]);
	}

	/**
	 * Clear out this partial diagnostic, giving it a new diagnostic ID
	 * and removing all of its arguments, ranges, and fix-it hints.
	 */
	public void reset()
	{
		reset(0);
	}

	public void reset(int diagID)
	{
		this.diagID = diagID;
	}

	public boolean hasStorage()
	{
		return diagStorage != null;
	}

	public PartialDiagnostic addTaggedVal(boolean val)
	{
		addTaggedVal(val ?1:0, ArgumentKind.ak_sint);
		return this;
	}

	public PartialDiagnostic addTaggedVal(int iVal)
	{
		addTaggedVal(iVal, ArgumentKind.ak_sint);
		return this;
	}

	public PartialDiagnostic addTaggedVal(String str)
	{
		addString(str);
		return this;
	}

	public PartialDiagnostic addTaggedVal(SourceRange range)
	{
		addSourceRange(CharSourceRange.getTokenRange(range));
		return this;
	}

	public PartialDiagnostic addTaggedVal(QualType type)
	{
		addTaggedVal(type, ArgumentKind.ak_qualtype);
		return this;
	}
}
