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

import gnu.trove.list.array.TIntArrayList;
import jlang.basic.SourceRange;
import jlang.type.QualType;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;

import static jlang.diag.Diagnostic.ExtensionHandling.Ext_Error;
import static jlang.diag.Diagnostic.ExtensionHandling.Ext_Ignore;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class Diagnostic
{
    public static final int DiagnosticCommonKindsBegin = 0;
    public static final int DiagnosticCommonKindsEnd = DiagnosticCommonKindsBegin + 120;
    public static final int DiagnosticLexKindsBegin = DiagnosticCommonKindsEnd;
    public static final int DiagnosticLexKindsEnd = DiagnosticLexKindsBegin + 300;
    public static final int DiagnosticParseKindsBegin = DiagnosticLexKindsEnd;

    public static final int DiagnosticParseKindsEnd = DiagnosticParseKindsBegin + 500;
    public static final int DiagnosticSemaKindsBegin = DiagnosticParseKindsEnd;
    public static final int DiagnosticSemaKindsEnd = DiagnosticSemaKindsBegin + 3500;

    public static final int DIAG_UPPER_LIMIT = DiagnosticSemaKindsEnd;


    static class StaticDiagInfoRec implements Comparable<StaticDiagInfoRec>
    {
        int diagID;
        Mapping mapping;
        DiagnosticClass diagClass;
        boolean sfinae;
        String description;
        String optionGroup;

        StaticDiagInfoRec(int diagID,
                Mapping mapping,
                DiagnosticClass diagClass,
                boolean sfinae,
                String description,
                String optionGroup)
        {
            this.diagID = diagID;
            this.mapping = mapping;
            this.sfinae = sfinae;
            this.description = description;
            this.optionGroup = optionGroup;
        }

        @Override
        public int compareTo(StaticDiagInfoRec o)
        {
            return diagID - o.diagID;
        }
    }

    private static final StaticDiagInfoRec[] staticDiagInfos;
    static
    {
        int commonLen = DiagnosticCommonKinds.values().length;
        int lexLen = DiagnosticLexKinds.values().length;
        int parseLen = DiagnosticParseKinds.values().length;
        int semaLen = DiagnosticSemaKinds.values().length;

        staticDiagInfos = new StaticDiagInfoRec[commonLen + lexLen + parseLen + semaLen];
        int idx = 0;
        for (DiagnosticCommonKinds kinds : DiagnosticCommonKinds.values())
        {
            staticDiagInfos[idx++] = new StaticDiagInfoRec(kinds.diagID,
                    kinds.diagMapping, kinds.diagClass,
                    kinds.sfinae, kinds.text, kinds.optionGroup);
        }

        for (DiagnosticLexKinds kinds : DiagnosticLexKinds.values())
        {
            staticDiagInfos[idx++] = new StaticDiagInfoRec(kinds.diagID,
                    kinds.diagMapping, kinds.diagClass,
                    kinds.sfinae, kinds.text, kinds.optionGroup);
        }

        for (DiagnosticParseKinds kinds : DiagnosticParseKinds.values())
        {
            staticDiagInfos[idx++] = new StaticDiagInfoRec(kinds.diagID,
                    kinds.diagMapping, kinds.diagClass,
                    kinds.sfinae, kinds.text, kinds.optionGroup);
        }

        for (DiagnosticSemaKinds kinds : DiagnosticSemaKinds.values())
        {
            staticDiagInfos[idx++] = new StaticDiagInfoRec(kinds.diagID,
                    kinds.diagMapping, kinds.diagClass,
                    kinds.sfinae, kinds.text, kinds.optionGroup);
        }
    }

    /**
     * Return the StaticDiagInfoRec entry for the specified DiagID,
     * or null if the ID is invalid.
     * @param diagID
     * @return
     */
    private static StaticDiagInfoRec getDiagInfo(int diagID)
    {
        StaticDiagInfoRec rec = new StaticDiagInfoRec(diagID, null, null, false, null, null);
        Arrays.sort(staticDiagInfos);
        int foundIndice = Arrays.binarySearch(staticDiagInfos, rec,
                Comparator.comparingInt(o -> o.diagID));
        StaticDiagInfoRec foundRec = staticDiagInfos[foundIndice];
        return foundIndice >= 0 && foundRec.diagID == diagID? foundRec : null;
    }

    // Diagnostic classes.
    public enum DiagnosticClass
    {
        CLASS_NOTE,
        CLASS_WARNING,
        CLASS_EXTENSION,
        CLASS_ERROR,
    }

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

        /**
         * Map this diagnostic to "warning", but make it immune to -Werror.  This
         * happens when you specify -Wno-error=foo.
         */
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

	public enum ArgumentKind
	{
		ak_std_string,      // std::string
		ak_c_string,        // String 
		ak_sint,            // int
		ak_uint,            // int
		ak_identifier,      // Identifier
		ak_qualtype,        // QualType
		ak_declarationname, // DeclarationName
		ak_nameddecl        // NamedDecl *
	}

	private int allExtensionsSilenced; // Used by __extension__
	/**
	 * Ignores all warnings.
	 */
	private boolean ignoreAllWarnings;
	/**
	 * Treat the warning as error.
	 */
	private boolean warningsAsErrors;
	/**
	 * Suppress warning in system headers.
	 */
	private boolean suppressSystemWarnings;

    /**
     * Is there error occured?
     */
	private boolean errorOccurred;

	private boolean fatalErrorOcurred;

    /**
     * Map extensions onto warnings or errors?
     */
	private ExtensionHandling extBehavior;

	/**
	 * This is the level of the last diagnostic emitted.  This is
	 * used to emit continuation diagnostics with the same level as the
	 * diagnostic that they follow.
	 */
	private Level lastDiagLevel;

	private int numDiagnostics;
	private int numErrors;

    /**
     * Information for uniquing and looking up custom diags.
     */
	private CustomDiagInfo customDiagInfo;

	/**
	 * This is a DiagnosticClient used for printing error or warning message
     * out in appropriate style.
	 */
	private DiagnosticClient client;

    /**
     * Mapping information for diagnostics.  Mapping info is
     * packed into four bits per diagnostic.  The low three bits are the mapping
     * (an instance of diag::Mapping), or zero if unset.  The high bit is set
     * when the mapping was established as a user mapping.  If the high bit is
     * clear, then the low bits are set to the default value, and should be
     * mapped with -pedantic, -Werror, etc.
     */
	private LinkedList<TIntArrayList> diagMappingsStack;

	private int curDiagID;

	private FullSourceLoc curDiagLoc;

	/**
	 * The maximum number of arguments we can hold. We currently
	 * only support up to 10 arguments (%0-%9).  A single diagnostic with more
	 * than that almost certainly has to be simplified anyway.
	 */
	public static final int maxArguments = 10;

	private int numDiagArgs;
	private int numDiagRanges;
	private int numFixItHints;
	private ArgumentKind[] diagArgumentsKind = new ArgumentKind[maxArguments];
	private Object[] diagArgumentsVal = new Object[maxArguments];
	private SourceRange[] diagRanges = new SourceRange[10];
	public static final int maxFixItHints = 3;

	private FixItHint[] fixItHints = new FixItHint[maxFixItHints];

	public Diagnostic(DiagnosticClient client)
	{
		this.client = client;
		allExtensionsSilenced = 0;
		ignoreAllWarnings = false;
		warningsAsErrors = false;
		suppressSystemWarnings = false;
		extBehavior = Ext_Ignore;
		errorOccurred = false;
		fatalErrorOcurred = false;
		numDiagArgs = 0;
        numErrors = 0;

        customDiagInfo = null;
		curDiagID = ~0;
        lastDiagLevel = Level.Ignored;

        diagMappingsStack = new LinkedList<>();
        TIntArrayList blankDiags = new TIntArrayList(DIAG_UPPER_LIMIT/2);
        diagMappingsStack.addLast(blankDiags);
	}

	enum SuppressKind
	{
		Suppress
	}

    public DiagnosticClient getClient()
    {
        return client;
    }

    public void setClient(DiagnosticClient client)
    {
        this.client = client;
    }

    /**
     * When set to true, any unmapped warnings are ignored.  If this and
     * warningsAsErrors are both set, then this one wins.
     * @param val
     */
    void setIgnoreAllWarnings(boolean val)
    {
        ignoreAllWarnings = val;
    }
    boolean getIgnoreAllWarnings()
    {
        return ignoreAllWarnings;
    }

    /**
     * When set to true, any warnings reported are issued as errors.
     * @param val
     */
    void setWarningsAsErrors(boolean val)
    {
        warningsAsErrors = val;
    }
    boolean getWarningsAsErrors()
    {
        return warningsAsErrors;
    }

    /**
     * When set to true mask warnings that come from system headers.
     * @param val
     */
    void setSuppressSystemWarnings(boolean val)
    {
        suppressSystemWarnings = val;
    }

    boolean getSuppressSystemWarnings()
    {
        return suppressSystemWarnings;
	}

    /**
     * This is a counter bumped when an __extension__
     * block is encountered.  When non-zero, all extension diagnostics are
     * entirely silenced, no matter how they are mapped.
     */
    public void incrementAllExtensionsSilenced()
    {
        ++allExtensionsSilenced;
    }
    public void decrementAllExtensionsSilenced()
    {
        --allExtensionsSilenced;
    }

    /**
     * This allows the client to specify that certain warnings are ignored.
     * Notes can never be mapped, errors can only be mapped to fatal, and
     * WARNINGs and EXTENSIONs can be mapped arbitrarily.
     * @param diag
     * @param Map
     */
    public void setDiagnosticMapping(int diag, Mapping Map)
    {
        assert diag < DIAG_UPPER_LIMIT : "Can only map builtin diagnostics";
        assert (isBuiltinWarningOrExtension(diag) || Map == Mapping.MAP_FATAL) :
                "Cannot map errors!";
        setDiagnosticMappingInternal(diag, Map, true);
    }

    /**
     * This controls whether otherwise-unmapped
     * extension diagnostics are mapped onto ignore/warning/error.  This
     * corresponds to the GCC -pedantic and -pedantic-errors option.
     * @param h
     */
    void setExtensionHandlingBehavior(ExtensionHandling h)
    {
        extBehavior = h;
    }

    public boolean hasErrorOccurred()
    {
        return errorOccurred;
    }

    public boolean hasFatalErrorOcurred()
    {
        return fatalErrorOcurred;
    }

    public int getNumErrors()
    {
        return numErrors;
    }

    public int getNumDiagnostics()
    {
        return numDiagnostics;
    }

    public int getID()
	{
		return curDiagID;
	}

    public FullSourceLoc getCurDiagLoc()
    {
        return curDiagLoc;
    }

    public int getNumDiagArgs()
	{
		return numDiagArgs;
	}

	public ArgumentKind getDiagArgKind(int index)
	{
		assert index >= 0 && index < diagArgumentsKind.length;
		return diagArgumentsKind[index];
	}

	public String getArgStdStr(int index)
	{
	    assert getDiagArgKind(index) == ArgumentKind.ak_std_string
                : "Invalid argument accessor!";
		return (String) diagArgumentsVal[index];
	}

	public int getArgSInt(int index)
    {
        assert getDiagArgKind(index) == ArgumentKind.ak_sint
                : "Invalid argument accessor!";
        return (Integer)diagArgumentsVal[index];
    }

    public int getArgUInt(int index)
    {
        assert getDiagArgKind(index) == ArgumentKind.ak_uint
                : "Invalid argument accessor!";
        return (Integer)diagArgumentsVal[index];
    }

    public String getArgIndentifier(int index)
    {
        assert getDiagArgKind(index) == ArgumentKind.ak_identifier
                :"invalid argument accessor!";
        return (String)diagArgumentsVal[index];
    }

    public Object getRawArg(int index)
    {
        assert getDiagArgKind(index) != ArgumentKind.ak_std_string
                :"invalid argument accessor!";
        return diagArgumentsVal[index];
    }

	public int getNumDiagRanges()
	{
		return numDiagRanges;
	}

	public SourceRange getRange(int index)
	{
		return diagRanges[index];
	}

	public int getNumFixItHints()
	{
		return numFixItHints;
	}

	public FixItHint getFixItHint(int index)
	{
		return fixItHints[index];
	}

    //===--------------------------------------------------------------------===//
    // Diagnostic classification and reporting interfaces.
    //
    public void pushMappings()
    {
        diagMappingsStack.addLast(diagMappingsStack.getLast());
    }

    public boolean popMappings()
    {
        if (diagMappingsStack.size() == 1)
            return false;
        diagMappingsStack.removeLast();
        return true;
    }

    /**
     * Return an ID for a diagnostic with the specified message
     * and level.  If this is the first request for this diagnosic, it is
     * registered and created, otherwise the existing ID is returned.
     * @param level
     * @param message
     * @return
     */
    public int getCustomDiagID(Level level, String message)
    {
        if (customDiagInfo == null)
            customDiagInfo = new CustomDiagInfo();
        return customDiagInfo.getOrCreateDiagID(level, message, this);
    }

    /**
     * Given a diagnostic ID, return a description of the issue.
     * @param diagID
     * @return
     */
    public static String getDescription(int diagID)
    {
        if (isDiagnosticLexKinds(diagID))
            return DiagnosticLexKinds.values()[diagID - DiagnosticCommonKindsBegin].text;
        else if (isDiagnosticParseKinds(diagID))
            return DiagnosticParseKinds.values()[diagID - DiagnosticParseKindsBegin].text;
        else if (isDiagnoticSemaKinds(diagID))
            return DiagnosticSemaKinds.values()[diagID - DiagnosticSemaKindsBegin].text;
        else
        {
            assert isDiagnosticCommonKinds(diagID);
            return DiagnosticCommonKinds.values()[diagID - DiagnosticCommonKindsBegin].text;
        }
    }
    
    /**
     * Return true if the unmapped diagnostic level of the specified diagnostic
     * ID is a Warning or Extension. This only works on builtin diagnostics,
     * not custom ones, and is not legal to call on NOTEs.
     * @param diagID
     * @return
     */
    public static boolean isBuiltinWarningOrExtension(int diagID)
    {
        return diagID < DIAG_UPPER_LIMIT &&
                getBuiltinDiagClass(diagID) != DiagnosticClass.CLASS_ERROR;
    }

    /**
     * Determine whether the given built-in diagnostic ID is a Note.
     * @param diagID
     * @return
     */
    public boolean isBuiltinNote(int diagID)
    {
        return diagID < DIAG_UPPER_LIMIT &&
                getBuiltinDiagClass(diagID) == DiagnosticClass.CLASS_NOTE;
    }

    /**
     * Determine whether the given built-in diagnostic ID is for an extension
     * of some sort.
     * @param diagID
     * @return
     */
    public static boolean isBuiltinExtensionDiag(int diagID)
    {
        return diagID < DIAG_UPPER_LIMIT
                && getBuiltinDiagClass(diagID) == DiagnosticClass.CLASS_EXTENSION;
    }

    /**
     * Return the lowest-level warning option that enables the specified
     * diagnostic.  If there is no -Wfoo flag that controls the diagnostic,
     * this returns null.
     * @param diagID
     * @return
     */
    public static String getWarningOptionForDiag(int diagID)
    {
        StaticDiagInfoRec info = getDiagInfo(diagID);
        if (info != null)
            return info.optionGroup;
        return null;
    }

    /**
     * Determines whether the given built-in diagnostic ID is
     * for an error that is suppressed if it occurs during C++ template
     * argument deduction.
     *
     * When an error is suppressed due to SFINAE, the template argument
     * deduction fails but no diagnostic is emitted. Certain classes of
     * errors, such as those errors that involve C++ access control,
     * are not SFINAE errors.
     * @param diagID
     * @return
     */
    public static boolean isBuiltinSFINAEDiag(int diagID)
    {
        StaticDiagInfoRec info = getDiagInfo(diagID);
        if (info != null)
            return info.sfinae;
        return false;
    }

    /**
     * Based on the way the client configured the Diagnostic
     * object, classify the specified diagnostic ID into a Level, consumable by
     * the DiagnosticClient.
     * @param diagID
     * @return
     */
    public Diagnostic.Level getDiagnosticLevel(int diagID)
    {
        // Handle custom diagnostics, which cannot be mapped.
        if (diagID >= DIAG_UPPER_LIMIT)
            return null; // customDiagInfo.getLevel(diagID);

        DiagnosticClass diagClass = getBuiltinDiagClass(diagID);
        assert diagClass != DiagnosticClass.CLASS_NOTE : "Cannot get diagnostic level of a note!";
        return getDiagnosticLevel(diagID, diagClass);
    }

    private static Mapping getDefaultDiagMapping(int diagID)
    {
        StaticDiagInfoRec info = getDiagInfo(diagID);
        if (info != null)
            return info.mapping;
        return Mapping.MAP_FATAL;
    }

    /**
     * Based on the way the client configured the Diagnostic
     * object, classify the specified diagnostic ID into a Level, consumable by
     * the DiagnosticClient.
     * @param diagID
     * @param diagClass
     * @return
     */
    private Diagnostic.Level getDiagnosticLevel(int diagID, DiagnosticClass diagClass)
    {
        // Specific non-error diagnostics may be mapped to various levels from ignored
        // to error.  Errors can only be mapped to fatal.
        Diagnostic.Level result = Level.Fatal;

        // Get the mapping information, if unset, compute it lazily.
        Mapping mappingInfo = getDiagnosticMappingInfo(diagID);
        if (mappingInfo.ordinal() == 0)
        {
            mappingInfo = getDefaultDiagMapping(diagID);
            setDiagnosticMappingInternal(diagID, mappingInfo, false);
        }

        switch (Mapping.values()[mappingInfo.ordinal() & 7]) 
        {
            default: assert false : "Unknown mapping!";
            case MAP_IGNORE:
                // Ignore this, unless this is an extension diagnostic and we're mapping
                // them onto warnings or errors.
                if (!isBuiltinExtensionDiag(diagID) ||  // Not an extension
                        extBehavior == Ext_Ignore ||        // Extensions ignored anyway
                        (mappingInfo.ordinal() & 8) != 0)             // User explicitly mapped it.
                    return Level.Ignored;
                result = Level.Warning;
                if (extBehavior == Ext_Error) 
                    result = Level.Error;
                break;
            case MAP_ERROR:
                result = Level.Error;
                break;
            case MAP_FATAL:
                result = Level.Fatal;
                break;
            case MAP_WARNING:
                // If warnings are globally mapped to ignore or error, do it.
                if (ignoreAllWarnings)
                    return Level.Ignored;

                result = Level.Warning;

                // If this is an extension diagnostic and we're in -pedantic-error mode, and
                // if the user didn't explicitly map it, upgrade to an error.
                if (extBehavior == Ext_Error && (mappingInfo.ordinal() & 8) == 0 &&
                        isBuiltinExtensionDiag(diagID))
                    result = Level.Error;

                if (warningsAsErrors)
                    result = Level.Error;
                break;

            case MAP_WARNING_NO_WERROR:
                // Diagnostics specified with -Wno-error=foo should be set to warnings, but
                // not be adjusted by -Werror or -pedantic-errors.
                result = Level.Warning;

                // If warnings are globally mapped to ignore or error, do it.
                if (ignoreAllWarnings)
                    return Level.Ignored;

                break;
        }

        // Okay, we're about to return this as a "diagnostic to emit" one last check:
        // if this is any sort of extension warning, and if we're in an __extension__
        // block, silence it.
        if (allExtensionsSilenced != 0 && isBuiltinExtensionDiag(diagID))
            return Level.Ignored;

        return result;
    }

    /**
     * Based on the way the client configured the Diagnostic object, classify
     * the specified diagnostic ID into a Level, consumable by the DiagnosticClient.
     * @param diagID
     * @return
     */
    public static DiagnosticClass getBuiltinDiagClass(int diagID)
    {
        StaticDiagInfoRec info = getDiagInfo(diagID);
        if (info != null)
            return info.diagClass;
        return null;
    }

    /**
     * Clear out the current diagnostic.
     */
    public void clear()
    {
        curDiagID = ~0;
    }

    /**
     * This is the method used to report a diagnostic that is finally fully formed.
     *
     * @return Returns true if the diagnostic was emitted, false if it was
     * suppressed.
     */
    private boolean processDiag()
    {
        Level diagLevel;
        int diagID = curDiagID;

        // True if this diagnostic should be produced even in a system header.
        boolean shouldEmitInSystemHeader;

        if (diagID >= DIAG_UPPER_LIMIT)
        {
            // Handle custom diagnostics, which cannot be mapped.
            diagLevel = customDiagInfo.getLevel(diagID);

            // Custom diagnostics always are emitted in system headers.
            shouldEmitInSystemHeader = true;
        }
        else
        {
            DiagnosticClass diagClass = getBuiltinDiagClass(diagID);
            if (diagClass == DiagnosticClass.CLASS_NOTE)
            {
                diagLevel = Level.Note;
                shouldEmitInSystemHeader = false;
            }
            else
            {
                shouldEmitInSystemHeader = diagClass == DiagnosticClass.CLASS_ERROR;
                diagLevel = getDiagnosticLevel(diagID, diagClass);
            }
        }

        if (diagLevel != Level.Note)
        {
            if (lastDiagLevel == Level.Fatal)
                fatalErrorOcurred = true;

            lastDiagLevel = diagLevel;
        }

        // If a fatal error has already been emitted, silence all subsequent
        // diagnostics.
        if (fatalErrorOcurred)
            return false;

        // If the client doesn't care about this message, don't issue it.  If this is
        // a note and the last real diagnostic was ignored, ignore it too.
        if (diagLevel == Level.Ignored || (diagLevel == Level.Note
                && lastDiagLevel == Level.Ignored))
            return false;

        // If this diagnostic is in a system header and is not a clang error, suppress
        // it.
        if (suppressSystemWarnings && !shouldEmitInSystemHeader &&
                getCurDiagLoc().isValid() &&
                (diagLevel != Level.Note || lastDiagLevel == Level.Ignored))
        {
            return false;
        }

        if (diagLevel.compareTo(Level.Error) >= 0)
        {
            errorOccurred = true;
            ++numErrors;
        }

        // Finally, report it.
        client.handleDiagnostic(diagLevel, this);
        if (client.includeInDiagnosticCounts())
            ++numDiagnostics;

        curDiagID= ~0;
        return true;
    }

    /**
     *  Issue the message to the client. {@code diagID} is a member of the
     *  kind.  This actually returns aninstance of DiagnosticBuilder which emits
     *  the diagnostics (through {@linkplain #processDiag()}). {@code loc}
     *  represents the source location associated with the diagnostic, which can
     *  be an invalid location if no position information is available.
     * @param loc
     * @param diagID
     * @return
     */
    public DiagnosticBuilder report(FullSourceLoc loc, int diagID)
    {
        assert curDiagID == ~0 : "Multiple diagnostics in flight at once!";
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

    /**
     * Return the mapping info currently set for the
     * specified builtin diagnostic.  This returns the high bit encoding, or zero
     * if the field is completely uninitialized.
     * @param diag
     * @return
     */
    private Mapping getDiagnosticMappingInfo(int diag)
    {
        TIntArrayList currentMappings = diagMappingsStack.getLast();
        return Mapping.values()[(currentMappings.get(diag/2) >> (((diag & 1)*4) & 15))];
    }

    private void setDiagnosticMappingInternal(
            int diagID,
            Mapping map,
            boolean isUser) 
    {
        int mapping = map.ordinal();
        if (isUser) mapping |= 8;  // Set the high bit for user mappings.
        int slot = diagMappingsStack.getLast().get(diagID/2);
        int shift = (diagID & 1)*4;
        slot &= ~(15 << shift);
        slot |= mapping << shift;
        diagMappingsStack.getLast().set(diagID/2, slot);
    }



    
	/**
	 * @author xlous.zeng
	 * @version 0.1
	 */
	public static class DiagnosticBuilder
	{
		private Diagnostic diagObj;
		private int numArgs, numRanges, numFixItHints;

		private DiagnosticBuilder(Diagnostic diag)
		{
			diagObj = diag;
		}

		public DiagnosticBuilder(SuppressKind kind)
		{
			super();
		}

		public DiagnosticBuilder(DiagnosticBuilder db)
		{
			this.diagObj = db.diagObj;
			this.numArgs = db.numArgs;
			this.numRanges = db.numRanges;
			this.numFixItHints = db.numFixItHints;
		}

		public boolean emit()
		{
			if (diagObj == null)return false;

			diagObj.numDiagArgs = numArgs;
			diagObj.numDiagRanges = numRanges;
			diagObj.numFixItHints = numFixItHints;

			// Process the diagnostic, sending the accumulated information to the
			// DiagnosticClient.
			boolean emitted = diagObj.processDiag();

			// Clear out the current diagnostic object.
			diagObj.clear();
			return emitted;
		}

		public DiagnosticBuilder addString(String str)
		{
			assert numArgs < maxArguments :"Too many arguments to diagnostics";
			if (diagObj != null)
			{
				diagObj.diagArgumentsKind[numArgs] = ArgumentKind.ak_c_string;
				diagObj.diagArgumentsVal[numArgs++] = str;
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
		public DiagnosticBuilder addFixItHint(FixItHint hint)
		{
			assert numFixItHints < maxFixItHints
					: "Too many arguments to diagnostics";
			if (diagObj != null)
			{
				diagObj.fixItHints[numFixItHints++] = hint;
			}
			return this;
		}
	}
}
