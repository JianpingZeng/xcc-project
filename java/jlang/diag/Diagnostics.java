package jlang.diag;
/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2017, Xlous Zeng.
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
import jlang.type.QualType;

import static jlang.diag.DiagnosticCommonKindsTag.DiagnosticCommonKindsBegin;
import static jlang.diag.DiagnosticCommonKindsTag.DiagnosticCommonKindsEnd;
import static jlang.diag.DiagnosticLexKindsTag.DiagnosticLexKindsBegin;
import static jlang.diag.DiagnosticLexKindsTag.DiagnosticLexKindsEnd;
import static jlang.diag.DiagnosticParseTag.DiagnosticParseKindsBegin;
import static jlang.diag.DiagnosticParseTag.DiagnosticParseKindsEnd;
import static jlang.diag.DiagnosticSemaTag.DiagnosticSemaKindsBegin;
import static jlang.diag.DiagnosticSemaTag.DiagnosticSemaKindsEnd;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class Diagnostics
{
	/**
	 * The level of the diagnostics.
	 */
	public enum Level
	{
		Ignored, Note, Warning, Error, Fatal
	}

	public enum Mapping
	{
		MAP_IGNORE,     //< Map this diagnostic to nothing, ignore it.
		MAP_WARNING,     //< Map this diagnostic to a warning.
		MAP_ERROR,     //< Map this diagnostic to an error.
		MAP_FATAL,     //< Map this diagnostic to a fatal error.

		/// Map this diagnostic to "warning", but make it immune to -Werror.  This
		/// happens when you specify -Wno-error=foo.
		MAP_WARNING_NO_WERROR
	}

	/**
	 * How do we handle otherwise-unmapped extension?  This
	 * is controlled by -pedantic and -pedantic-errors.
	 */
	enum ExtensionHandling
	{
		Ext_Ignore, Ext_Warn, Ext_Error
	}

	enum ArgumentKind
	{
		ak_std_string,      // std::string
		ak_c_string,        // const char *
		ak_sint,            // int
		ak_uint,            // unsigned
		ak_identifier,      // Identifier
		ak_qualtype,        // QualType
		ak_declarationname, // DeclarationName
		ak_nameddecl        // NamedDecl *
	}

	/**
	 * Ignores all warnings.
	 */
	private boolean ignoreAllWarings;
	/**
	 * Treat the warning as error.
	 */
	private boolean warningsAsErrors;
	/**
	 * Suppress warning in system headers.
	 */
	private boolean suppressSystemWarnings;

	private boolean errorOccurred;
	private boolean fatalErrorOcurred;

	/**
	 * This is the level of the last diagnostic emitted.  This is
	 * used to emit continuation diagnostics with the same level as the
	 * diagnostic that they follow.
	 */
	private Level lastDiagLevel;

	private int numDiagnostics;
	private int numErrors;

	private DiagnosticClient client;

	private int curDiagID;

	private SourceLocation curDiagLoc;

	/**
	 * The maximum number of arguments we can hold. We currently
	 * only support up to 10 arguments (%0-%9).  A single diagnostic with more
	 * than that almost certainly has to be simplified anyway.
	 */
	private final int maxArguments = 10;

	private int numDiagArgs;
	private int numDiagRanges;
	private int numCodeModificationHints;
	private ArgumentKind[] diagArgumentsKind = new ArgumentKind[maxArguments];
	private String[] diagArgumentsStr = new String[maxArguments];
	private Object[] diagArgumentsVal = new Object[maxArguments];
	private SourceRange[] diagRanges = new SourceRange[10];
	private final int maxCodeModificationHints = 3;

	private CodeModificationHint[] codeModificationHints =
			new CodeModificationHint[maxCodeModificationHints];

	public Diagnostics(DiagnosticClient client)
	{
		this.client = client;
		lastDiagLevel = Level.Ignored;
		curDiagID = ~0;
	}

	enum SuppressKind
	{
		Suppress
	}

	/**
	 * @author xlous.zeng
	 * @version 0.1
	 */
	public final class DiagnosticBuilder
	{
		private Diagnostics diagObj;
		private int numArgs, numRanges, numCodeModificationHints;

		private DiagnosticBuilder(Diagnostics diag)
		{
			diagObj = diag;
		}

		public DiagnosticBuilder(SuppressKind kind)
		{
			super();
		}

		public boolean emit()
		{
			if (diagObj == null)return false;

			diagObj.numDiagArgs = numArgs;
			diagObj.numDiagRanges = numRanges;
			diagObj.numCodeModificationHints = numCodeModificationHints;

			// Process the diagnostic, sending the accumulated information to the
			// DiagnosticClient.
			boolean emitted = diagObj.processDiag();

			// Clear out the current diagnostic object.
			diagObj.clear();

			diagObj = null;
			return emitted;
		}

		public DiagnosticBuilder addString(String str)
		{
			assert numArgs < maxArguments :"Too many arguments to diagnostics";
			if (diagObj != null)
			{
				diagObj.diagArgumentsKind[numArgs] = ArgumentKind.ak_c_string;
				diagObj.diagArgumentsStr[numArgs++] = str;
			}
			return this;
		}

		public DiagnosticBuilder addTaggedVal(Object val, ArgumentKind kind)
		{
			assert numArgs < maxArguments :"Too many arguments to diagnostics";
			if (diagObj != null)
			{
				diagObj.diagArgumentsKind[numArgs] = kind;
				diagObj.diagArgumentsVal[numArgs++] = val;
			}
			return this;
		}

		public DiagnosticBuilder addTaggedVal(int i)
		{
			return addTaggedVal(i, ArgumentKind.ak_sint);
		}

		public DiagnosticBuilder addTaggedVal(boolean b)
		{
			return addTaggedVal(b, ArgumentKind.ak_sint);
		}

		public DiagnosticBuilder addTaggedVal(String identifier)
		{
			return addTaggedVal(identifier, ArgumentKind.ak_identifier);
		}

		public DiagnosticBuilder addTaggedVal(QualType type)
		{
			return addTaggedVal(type, ArgumentKind.ak_qualtype);
		}

		public DiagnosticBuilder addSourceRange(SourceRange range)
		{
			assert numRanges < diagObj.diagRanges.length
					:"Too many arguments to diagnostics";
			if (diagObj != null)
			{
				diagObj.diagRanges[numRanges++] = range;
			}
			return this;
		}
		public DiagnosticBuilder addCodeModificationHint(CodeModificationHint hint)
		{
			assert numCodeModificationHints < maxCodeModificationHints
					: "Too many arguments to diagnostics";
			if (diagObj != null)
			{
				diagObj.codeModificationHints[numCodeModificationHints++] = hint;
			}
			return this;
		}
	}

	private void clear()
	{

	}

	private boolean processDiag()
	{
		return false;
	}

	public DiagnosticBuilder report(FullSourceLoc loc, int diagID)
	{
		assert curDiagID == ~0 :"Multiple diagnostics in flight at once!";
		curDiagLoc = loc;
		curDiagID = diagID;
		return new DiagnosticBuilder(this);
	}

	public static boolean isDiagnosticLexKinds(int diagID)
	{
		return diagID >= DiagnosticLexKindsBegin
				&& diagID < DiagnosticLexKindsEnd;
	}

	public static boolean isDiagnosticParseKinds(int diagID)
	{
		return diagID >= DiagnosticParseKindsBegin
				&& diagID < DiagnosticParseKindsEnd;
	}

	public static boolean isDiagnoticSemaKinds(int diagID)
	{
		return diagID >= DiagnosticSemaKindsBegin
				&& diagID < DiagnosticSemaKindsEnd;
	}

	public static boolean isDiagnosticCommonKinds(int diagID)
	{
		return diagID >= DiagnosticCommonKindsBegin
				&& diagID < DiagnosticCommonKindsEnd;
	}

	public static Level getDiagnosticLevel(int diagID)
	{
		if (isDiagnosticLexKinds(diagID))
			return DiagnosticLexKinds.values()[diagID - DiagnosticCommonKindsBegin].diagClass;
		else if (isDiagnosticParseKinds(diagID))
			return DiagnosticParseKinds.values()[diagID - DiagnosticParseKindsBegin].diagClass;
		else if (isDiagnoticSemaKinds(diagID))
			return DiagnosticSemaKinds.values()[diagID - DiagnosticSemaKindsBegin].diagClass;
		else
		{
			assert isDiagnosticCommonKinds(diagID);
			return DiagnosticCommonKinds.values()[diagID - DiagnosticCommonKindsBegin].diagClass;
		}
	}
}
