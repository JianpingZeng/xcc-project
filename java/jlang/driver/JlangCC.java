package jlang.driver;
/*
 * Extremely C language Compiler
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

import backend.support.ErrorHandling;
import backend.target.TargetMachine;
import backend.target.TargetSelect;
import backend.value.Module;
import jlang.ast.ASTConsumer;
import jlang.ast.PrettyASTConsumer;
import jlang.basic.HeaderSearch;
import jlang.basic.InitHeaderSearch;
import jlang.basic.SourceManager;
import jlang.basic.TargetInfo;
import jlang.clex.*;
import jlang.diag.*;
import jlang.sema.ASTContext;
import jlang.sema.Decl;
import jlang.sema.Sema;
import jlang.support.*;
import jlang.support.LangOptions.VisibilityMode;
import jlang.system.Process;
import tools.OutParamWrapper;
import tools.Pair;
import tools.commandline.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

import static jlang.basic.InitHeaderSearch.IncludeDirGroup.*;
import static jlang.basic.InitHeaderSearch.IncludeDirGroup.System;
import static jlang.codegen.BackendConsumer.createBackendConsumer;
import static jlang.driver.JlangCC.LangStds.*;
import static jlang.support.BackendAction.Backend_EmitAssembly;
import static jlang.support.BackendAction.Backend_EmitIR;
import static jlang.support.CompileOptions.InliningMethod.NormalInlining;
import static jlang.support.CompileOptions.InliningMethod.OnlyAlwaysInlining;
import static jlang.support.LangKind.*;
import static jlang.support.ProgramAction.*;
import static tools.commandline.Desc.desc;
import static tools.commandline.FormattingFlags.Positional;
import static tools.commandline.Initializer.init;
import static tools.commandline.MiscFlags.CommaSeparated;
import static tools.commandline.OptionNameApplicator.optionName;
import static tools.commandline.ValueDesc.valueDesc;

/**
 * This class is used as programmatic interface for handling command line
 * options.
 * 
 * @author xlous.zeng
 *
 */
public class JlangCC implements DiagnosticFrontendKindsTag
{
    public static BooleanOpt Verbose = new BooleanOpt(
            new OptionNameApplicator("v"),
            desc("Enable verbose output"),
            init(false));

    public static StringOpt OutputFile =
            new StringOpt(new OptionNameApplicator("o"),
            valueDesc("path"),
            desc("Specify the output file"),
            init(""));

    public static Opt<ProgramAction> ProgAction
             = new Opt<ProgramAction>(new Parser<>(),
            desc("Choose output type:"),
            new NumOccurrencesApplicator(NumOccurrences.ZeroOrMore),
            init(ParseSyntaxOnly),
            new ValueClass<>(
                new ValueClass.Entry<>(PrintPreprocessedInput, "E",
                    "Run preprocessor, emit preprocessed file"),
                new ValueClass.Entry<>(ParseSyntaxOnly, "fsyntax-only",
                    "Run parser and perform semantic analysis"),
                new ValueClass.Entry<>(ASTDump, "ast-dump",
                        "Build ASTs and then debug dump them"),
                new ValueClass.Entry<>(GenerateAsmCode, "S",
                        "Emit native assembly code"),
                new ValueClass.Entry<>(EmitLLVM, "emit-llvm",
                        "Build ASTs then convert to LLVM, emit .ll file"))
            );

    public static BooleanOpt NoShowColumn =
            new BooleanOpt(new OptionNameApplicator("fno-show-column"),
            desc("Do not include column number on diagnostics"),
            init(false));

    public static BooleanOpt NoShowLocation =
            new BooleanOpt(new OptionNameApplicator("f-no-show-source-location"),
            desc("Do not include source location information with diagnostics"),
            init(false));;

    public static BooleanOpt NoCaretDiagnostics =
            new BooleanOpt(new OptionNameApplicator("fno-caret-diagnostics"),
            desc("Do not include source line and caret with diagnostics"),
            init(false));

    public static BooleanOpt NoDiagnosticsFixIt =
            new BooleanOpt(new OptionNameApplicator("fno-diagnostics-fixit-info"),
            desc("Do not include fixit information in diagnostics"),
            init(false));;

    public static BooleanOpt PrintSourceRangeInfo =
            new BooleanOpt(new OptionNameApplicator("fdiagnostics-print-source-range-info"),
            desc("Print source range spans in numeric form"),
            init(false));

    public static BooleanOpt PrintDiagnosticOption =
            new BooleanOpt(new OptionNameApplicator("fdiagnostics-show-option"),
            desc("Print diagnostic asmName with mappable diagnostics"),
            init(false));

    public static IntOpt MessageLength =
            new IntOpt(new OptionNameApplicator("fmessage-length"),
            desc("Format message diagnostics so that they fit " +
                    "within N columns or fewer, when possible."),
            valueDesc("N"),
            init(80));

    public static BooleanOpt NoColorDiagnostic =
            new BooleanOpt(new OptionNameApplicator("fno-color-diagnostics"),
            desc("Don't use colors when showing diagnostics " +
                    "(automatically turned off if output is not a " +
                    "terminal)."),
            init(false));

    public static Opt<LangKind> BaseLang =
            new Opt<LangKind>(new Parser<>(),
            new OptionNameApplicator("x"),
            desc("Base language to compile"),
            init(langkind_unspecified),
            new ValueClass<>(
                    new ValueClass.Entry<>(langkind_c, "c", "C"),
                    new ValueClass.Entry<>(langkind_cpp, "clex-output", "Preprocessed C"),
                    new ValueClass.Entry<>(langkind_asm_cpp, "assembler-with-clex",
                            "Preprocessed asm")));

    public static Opt<VisibilityMode> SymbolVisibility =
            new Opt<VisibilityMode>(new Parser<>(),
            new OptionNameApplicator("fvisibility"),
            desc("Set the default symbol visibility:"),
            init(VisibilityMode.Default),
            new ValueClass<>(new ValueClass.Entry<>(VisibilityMode.Default,
                    "default", "Use default symbol visibility"),
                    new ValueClass.Entry<>(VisibilityMode.Hidden,
                            "hidden", "Use hidden symbol visibility"),
                    new ValueClass.Entry<>(VisibilityMode.Protected,
                            "protected", "Use protected symbol visibility")));

    public static Opt<LangStds> LangStd =
            new Opt<LangStds>(new Parser<>(),
            new OptionNameApplicator("std"),
            desc("Language standard to compile for"),
            init(Lang_unpsecified),
            new ValueClass<>(
                new ValueClass.Entry<>(Lang_c89, "c89","ISO C 1990"),
                new ValueClass.Entry<>(Lang_c99, "c99","ISO C 1999"),
                new ValueClass.Entry<>(Lang_c11, "c11", "ISO C 2011"),
                new ValueClass.Entry<>(Lang_gnu89, "gnu89",
                        "ISO C 1990 with GNU extensions"),
                new ValueClass.Entry<>(Lang_gnu99, "gnu99",
                        "ISO C 1999 with GNU extensions (default for C)")));

    public static BooleanOpt Trigraphs = new BooleanOpt(
            new OptionNameApplicator("trigraphs"),
            desc("Process trigraph sequences"),
            init(false));

    public static BooleanOpt DollarsInIdents = new BooleanOpt(
            new OptionNameApplicator("fdollars-in-identifiers"),
            desc("Allow '$' in identifiers"),
            init(false));

    public static BooleanOpt OptSize = new BooleanOpt(
            new OptionNameApplicator("Os"),
            desc("Optimize for size"),
            init(false));

    public static StringOpt MainFileName =
            new StringOpt(new OptionNameApplicator("main-file-asmName"),
            desc("JlangCC file asmName to use for debug info"),
            init(""));

    public static class OptLevelParser extends ParserUInt
    {
        public boolean parse(Option<?> O, String ArgName,
                String Arg, OutParamWrapper<Integer> Val)
        {
            if (super.parse(O, ArgName, Arg, Val))
                return true;
            if (Val.get() > 3)
                return O.error("'" + Arg + "' invalid optimization level!");
            return false;
        }
    }

    public static UIntOpt OptLevel = new UIntOpt(
            new OptLevelParser(),
            new OptionNameApplicator("O"),
            new FormattingFlagsApplicator(FormattingFlags.Prefix),
            desc("Optimization level"),
            init(0));

    public static StringOpt TargetTriple = new StringOpt(
            new OptionNameApplicator("triple"),
            desc("Specify target triple (e.g. x86_64-unknown-linux-gnu)"),
            init(""));

    //===----------------------------------------------------------------------===//
    // Preprocessor Initialization
    //===----------------------------------------------------------------------===//
    public static ListOpt<String> D_Macros = new ListOpt<String>(
            new ParserString(),
            new OptionNameApplicator("D"),
            valueDesc("macro"),
            desc("Predefine the specified macro"),
            new FormattingFlagsApplicator(FormattingFlags.Prefix));

    public static ListOpt<String> U_macros = new ListOpt<String>(
            new ParserString(),
            new OptionNameApplicator("U"),
            new FormattingFlagsApplicator(FormattingFlags.Prefix),
            valueDesc("macro"),
            desc("Undefine the specified macro"));

    public static ListOpt<String> ImplicitInclude = new ListOpt<String>(
            new ParserString(),
            new OptionNameApplicator("include"),
            valueDesc("file"),
            desc("Include file before parsing"));

    public static ListOpt<String> I_dirs = new ListOpt<String>(
            new ParserString(),
            new OptionNameApplicator("I"),
            desc("Add directory to include search path"),
            valueDesc("directory"),
            new FormattingFlagsApplicator(FormattingFlags.Prefix));

    public static ListOpt<String> Iquote_dirs = new ListOpt<String>(
            new ParserString(),
            new OptionNameApplicator("iquote"),
            valueDesc("directory"),
            new FormattingFlagsApplicator(FormattingFlags.Prefix),
            desc("Add directory to QUOTE include search path"));

    public static ListOpt<String> Isystem_dirs = new ListOpt<String>(
            new ParserString(),
            new OptionNameApplicator("isystem"),
            valueDesc("directory"),
            new FormattingFlagsApplicator(FormattingFlags.Prefix),
            desc("Add directory to SYSTEM include search path"));

    public static BooleanOpt GenerateDebugInfo = new BooleanOpt(
            new OptionNameApplicator("g"),
            desc("Generate source level debug information"),
            init(false));

    public StringOpt TargetCPU = new StringOpt(
            new OptionNameApplicator("mcpu"),
            desc("Target a specific cpu type (-mcpu=help for details)"),
            init(""));

    public ListOpt<String> TargetFeatures = new ListOpt<String>(
            new ParserString(),
            new MiscFlagsApplicator(CommaSeparated),
            new OptionNameApplicator("target-feature"),
            desc("Target specific attributes"),
            valueDesc("+a1,+a2,-a3,..."));

    public static ListOpt<String> InputFilenames = new ListOpt<String>(
            new ParserString(),
            new FormattingFlagsApplicator(Positional),
            desc("<input files>"));

    public static StringOpt Isysroot = new StringOpt(
        new OptionNameApplicator("isysroot"),
            valueDesc("dir"),
            init("/"),
            desc("Set the system root directory (usually /)"),
            init(""));

    public static BooleanOpt nostdinc = new BooleanOpt(
            new OptionNameApplicator("nostdinc"),
            desc("Disable standard #include directories"),
            init(false));

    //===----------------------------------------------------------------------===//
    // Preprocessing (-E mode) Options
    //===----------------------------------------------------------------------===//
    public static final BooleanOpt DisableLineMarker =
            new BooleanOpt(optionName("P"),
            desc("Disable linemarker output in -E mode"),
            init(false));
    public static final BooleanOpt EnableCommentOutput =
            new BooleanOpt(optionName("C"),
            desc("Enable comment output in -E mode"),
            init(false));
    public static final BooleanOpt EnableMacroCommentOutput =
            new BooleanOpt(optionName("CC"),
            desc("Enable comment output in -E mode, even from macro expansions"),
            init(false));

    public static final BooleanOpt DumpMacros =
            new BooleanOpt(optionName("dM"),
            desc("Print macro definitions in -E mode instead of normal output"),
            init(false));

    public static final BooleanOpt DumpDefines =
            new BooleanOpt(optionName("dD"),
            desc("Print macro definitions in -E mode in addition to normal output"),
            init(false));

    /**
     * Result codes.
     */
    private static final int EXIT_OK = 0;

    /**
     * Result codes.
     */
    private static final int EXIT_ERROR = 1;

    /**
     * Result codes.
     */
    private static final int EXIT_CMDERR = 2;

    /**
     * Result codes.
     */
    private static final int EXIT_SYSERR = 3;

    /**
     * Result codes.
     */
    private static final int EXIT_ABNORMAL = 4;

    public static final String VERSION = "0.1";
    public static final String NAME = "xcc";

    private Function<Module, TargetMachine> targetMachineAllocator;

    private void printUsage(String msg)
    {
        java.lang.System.err.println(msg);
        java.lang.System.exit(EXIT_OK);
    }

    /**
     * Print a string that explains usage.
     */
    private void printUsage()
    {
        java.lang.System.exit(EXIT_OK);
    }

    private void version()
    {
        java.lang.System.err.printf("%s version %s\n", NAME, VERSION);
    }

    private void error(String msg, String arg)
    {
        java.lang.System.err.println(msg + ":" + arg);
        printUsage();
    }

    private void error(String msg)
    {
        error(msg, "");
    }

    private void parseAST(Preprocessor pp, ASTConsumer consumer, ASTContext ctx)
    {
        Sema sema = new Sema(pp, ctx, consumer);

        jlang.cparser.Parser parser = new jlang.cparser.Parser(pp, sema);
        pp.enterMainSourceFile();

        // Initialize the parser.
        parser.initialize();
        consumer.initialize(ctx);

        ArrayList<Decl> declsGroup = new ArrayList<>(16);

        while (!parser.parseTopLevel(declsGroup)) // Not end of file.
        {
            consumer.handleTopLevelDecls(declsGroup);
        }

        consumer.handleTranslationUnit();
    }

    private PrintStream computeOutFile(
            String infile,
            String extension,
            boolean binary,
            StringBuilder outPath)
    {
        boolean usestdout = false;
        String outfile = OutputFile.value;
        PrintStream os = null;
        String outputFile = "";
        if (outfile == null || outfile.length() <= 0 || infile.equals("-"))
        {
            usestdout = true;
        }
        else if (!outfile.isEmpty())
        {
            outputFile = outfile;
        }
        else if (extension != null)
        {
            int dotPos = infile.lastIndexOf(".");
            Path path;
            if (dotPos >= 0)
                infile = infile.substring(0, dotPos + 1);
            outputFile = infile + extension;
        }
        else
        {
            usestdout = true;
        }

        if (usestdout)
        {
            os = java.lang.System.out;
        }
        else
        {
            try
            {
                Path path = Files.createFile(Paths.get(outputFile));
                File file = path.toFile();
                outPath.append(file.getAbsolutePath());
                os = new PrintStream(new FileOutputStream(file));
            }
            catch (IOException e)
            {
                error(e.getMessage());
                java.lang.System.exit(-1);
            }
        }
        return os;
    }

    /**
     * Print a macro definition in a form that will be properly accepted back
     * as a definition.
     * @param ii
     * @param mi
     * @param pp
     * @param os
     */
    public static void printMacroDefinition(IdentifierInfo ii, MacroInfo mi,
            Preprocessor pp, PrintStream os)
    {
        os.printf("#define %s", ii.getName());

        if (mi.isFunctionLike())
        {
            os.print("(");
            if (mi.getNumArgs() <= 0);
            else if (mi.getNumArgs() == 1)
                os.print(mi.getArgAt(0).getName());
            else
            {
                os.print(mi.getArgAt(0).getName());
                for (int i = 1, e = mi.getNumArgs(); i != e; i++)
                    os.printf(",%s", mi.getArgAt(i).getName());
            }

            if (mi.isVariadic())
            {
                if (mi.getNumArgs() != 0)
                    os.print(",");
                os.print("...");
            }
            os.print(")");
        }

        // GCC always emits a space, even if the macro body is empty.  However, do not
        // want to emit two spaces if the first token has a leading space.
        if (mi.getNumTokens() == 0 || !mi.getReplacementToken(0).hasLeadingSpace())
            os.print(' ');

        for (Token tok : mi.getReplacementTokens())
        {
            if (tok.hasLeadingSpace())
                os.print(' ');

            String str = pp.getSpelling(tok);
            os.print(str);
        }
    }

    private void printMacros(Preprocessor pp, PrintStream os)
    {
        pp.enterMainSourceFile();
        Token tok = new Token();
        do
        {
            pp.lex(tok);
        }while (tok.isNot(TokenKind.eof));

        ArrayList<Pair<IdentifierInfo, MacroInfo>> macroInfosByID = new ArrayList<>();
        pp.getMacros().forEach((key, val)->
        {
            macroInfosByID.add(Pair.get(key, val));
        });

        // Sorts in the order of alphabetic name.
        macroInfosByID.sort(Comparator.comparing(o -> o.first.getName()));
        for (Pair<IdentifierInfo, MacroInfo> pair : macroInfosByID)
        {
            MacroInfo mi = pair.second;
            // Ignore computed macros like __LINE__
            if (mi.isBuiltinMacro()) continue;

            printMacroDefinition(pair.first, mi, pp, os);
            os.println();
        }
    }

    /**
     * This implements -E mode.
     * @param pp
     * @param os
     * @param enableCommentOutput
     * @param enableMacroCommentOutput
     * @param disableLineMarkers
     * @param dumpDefines
     */
    private void printPreprocessedInput(
            Preprocessor pp,
            PrintStream os,
            boolean enableCommentOutput,
            boolean enableMacroCommentOutput,
            boolean disableLineMarkers,
            boolean dumpDefines)
    {
        // Inform the preprocessor whether we want it to retain comments or not, due
        // to -C or -CC.
        pp.setCommentRetentionState(enableCommentOutput, enableMacroCommentOutput);

        PrintPPOutputPPCallbacks callbacks = new PrintPPOutputPPCallbacks(
                pp, os, disableLineMarkers, dumpDefines);
        pp.addPragmaHandler(null, new UnknownPragmaHandler("#pragma", callbacks));
        pp.addPragmaHandler("GCC", new UnknownPragmaHandler("#pragma GCC", callbacks));

        // After we have configured the preprocessor, enter the main file.
        pp.enterMainSourceFile();

        // Consume all of the tokens that come from the predefines buffer.  Those
        // should not be emitted into the output and are guaranteed to be at the
        // start.
        SourceManager sgr = pp.getSourceManager();
        Token tok = new Token();
        do
        {
            pp.lex(tok);
        }while (tok.isNot(TokenKind.eof) && tok.getLocation().isFileID()
                && sgr.getPresumedLoc(tok.getLocation()).getFilename().equals("<built-in>"));

        // Read all the preprocessed tokens, printing them out to the stream.
        printPreprocessedTokens(pp, tok, callbacks, os);
        os.println();
    }

    private static void printPreprocessedTokens(Preprocessor pp, Token tok,
            PrintPPOutputPPCallbacks callbacks, PrintStream os)
    {
        Token prevTok = new Token();
        while (true)
        {
            if (tok.isAtStartOfLine() && callbacks.handleFirstTokenOnLine(tok))
            {// done.
            }
            else if (tok.hasLeadingSpace() ||
                    // If we haven't emitted a token on this line yet, PrevTok isn't
                    // useful to look at and no concatenation could happen anyway.
                ( callbacks.hasEmittedTokensOnThisLine() &&
                    // Don't print "-" next to "-", it would form "--".
                callbacks.avoidConcat(prevTok, tok)))
            {
                os.print(' ');
            }
            IdentifierInfo ii = tok.getIdentifierInfo();
            if (ii != null)
            {
                os.print(ii.getName());
            }
            else if (tok.isLiteral() && !tok.needsCleaning() && tok.getLiteralData() != null)
            {
                int offset = tok.getLiteralData().offset;
                int len = tok.getLength();
                String str = String.valueOf(Arrays.copyOfRange(tok.getLiteralData()
                        .buffer, offset, offset + len));
                os.print(str);
            }
            else if (tok.getLength() < 256)
            {
                String tokStr = pp.getSpelling(tok);
                os.printf(tokStr);

                if (tok.getKind() == TokenKind.Comment)
                    callbacks.handleNewLinesInToken(tokStr);
            }
            else
            {
                String s = pp.getSpelling(tok);
                os.print(s);

                if (tok.getKind() == TokenKind.Comment)
                    callbacks.handleNewLinesInToken(s);
            }
            callbacks.setEmittedTokensOnThisLine();

            if (tok.is(TokenKind.eof))
                break;

            prevTok = tok.clone();
            pp.lex(tok);
        }
    }

    /**
     * This method is called when processing each input file.
     * It responsible for creating an instance of {@linkplain Module}, containing
     * global Constant for global variable, {@linkplain backend.value.Function}.
     *
     * @param infile
     */
    private void processInputFile(
            Preprocessor pp,
            String infile,
            ProgramAction progAction,
            HashMap<String, Boolean> features)
    {
        ASTConsumer consumer = null;
        PrintStream os = null;
        StringBuilder outpath = new StringBuilder();
        boolean clearSourceMgr = false;
        switch (progAction)
        {
            case RunPreprocessorOnly:
                break;
            case ParseSyntaxOnly:
                consumer = new PrettyASTConsumer();
                break;
            case ASTDump:
                assert false : "Unsupported currently.";
                return;
            case DumpTokens:
                Token tok = new Token();
                // Start preprocessing the specified input file.
                pp.enterMainSourceFile();
                do
                {
                    pp.lex(tok);
                    pp.dumpToken(tok, true);
                    java.lang.System.err.println();
                }while (tok.isNot(TokenKind.eof));
                clearSourceMgr = true;
                break;
            case PrintPreprocessedInput:
                os = computeOutFile(infile, null, true, outpath);
                break;
            case EmitLLVM:
            case GenerateAsmCode:
            {
                BackendAction act;
                if (progAction == EmitLLVM)
                {
                    act = Backend_EmitIR;
                    os = computeOutFile(infile, "ll", true, outpath);
                }
                else
                {
                    act = Backend_EmitAssembly;
                    os = computeOutFile(infile, "s", true, outpath);
                }

                CompileOptions compOpts = initializeCompileOptions(features);
                consumer = createBackendConsumer(act,
                        pp.getDiagnostics(),
                        pp.getLangOptions(),
                        compOpts,
                        infile, os,
                        targetMachineAllocator);
                break;
            }
        }

        ASTContext astCtx = new ASTContext(
                pp.getLangOptions(),
                pp.getSourceManager(),
                pp.getTargetInfo(),
                pp.getIdentifierTable());

        // If we have an ASTConsumer, run the parser with it.
        if (consumer != null)
            parseAST(pp, consumer, astCtx);

        // Just lex as fast as we can, no output.
        if (progAction == RunPreprocessorOnly)
        {
            Token tok = new Token();
            // Start preprocessing the specified input file.
            pp.enterMainSourceFile();
            do
            {
                pp.lex(tok);
            }while (tok.isNot(TokenKind.eof));
            clearSourceMgr = true;
        }
        else if (progAction == PrintPreprocessedInput)
        {
            if (DumpMacros.value)
                printMacros(pp, os);
            else
                printPreprocessedInput(pp, os, EnableCommentOutput.value,
                        EnableMacroCommentOutput.value,
                        DisableLineMarker.value, DumpDefines.value);
            clearSourceMgr = true;
        }

        // For a multi-file compilation, some things are ok with nuking the source
        // manager tables, other require stable fileid/macroid's across multiple
        // files.
        if (clearSourceMgr)
            pp.getSourceManager().clearIDTables();

        os = null;
        // Always delete the output stream because we don't want to leak file
        // handles.
        if (pp.getDiagnostics().getNumErrors() > 0 && outpath.length() > 0)
        {
            // If we had errors, try to erase the output file.
            File outFile = new File(outpath.toString());
            if (outFile.exists())
                outFile.delete();
        }
    }

    private CompileOptions initializeCompileOptions(HashMap<String, Boolean> features)
    {
        CompileOptions compOpt = new CompileOptions();
        compOpt.optimizeSize = OptSize.value;
        compOpt.debugInfo = GenerateDebugInfo.value;
        if (OptSize.value)
        {
            // -Os implies -O2
            compOpt.optimizationLevel = 2;
        }
        else
        {
            compOpt.optimizationLevel = OptLevel.value.byteValue();
        }

        // We must always run at least the always inlining pass.
        if (compOpt.optimizationLevel > 1)
            compOpt.inlining = NormalInlining;
        else
            compOpt.inlining = OnlyAlwaysInlining;

        // Can not unroll loops when enable optimize code size.
        compOpt.unrollLoops = compOpt.optimizationLevel > 1 && !OptSize.value;
        compOpt.CPU = TargetCPU.value;
        compOpt.features.clear();
        for (Map.Entry<String, Boolean> entry : features.entrySet())
        {
            String name = entry.getValue()?"+":"-";
            name += entry.getKey();
            compOpt.features.add(name);
        }
        return compOpt;
    }

    /**
     * Initialize the kind of program action to be performed by Compiler instance
     * , according to command line option. Note that the action performed by compiler
     * is setted to {@code ParseSyntaxOnly} by default if the absence of command line
     * option.
     *
     * @return
     */
    private ProgramAction initializeProgAction()
    {
        ProgramAction progAction = ProgramAction.ParseSyntaxOnly;
        if (ProgAction.value != null)
            progAction = ProgAction.value;

        return progAction;
    }

    private LangKind getLanguage(String filename)
    {
        int lastDotPos = filename.lastIndexOf('.');
        if (lastDotPos < 0)
        {
            return langkind_c;
        }

        String ext = filename.substring(lastDotPos + 1);
        // C header: .h
        // assembly no preprocessing: .s
        // assembly: .S
        switch (ext)
        {
            default:
            case "c":
                return langkind_c;
            case "S":
            case "s":
                return langkind_asm_cpp;
            case "i":
                return langkind_cpp;
        }
    }

    private void initializeOption(LangOptions options)
    {
        // do nothing.
    }

    private void initializeLangOptions(LangOptions langOption,
            LangKind lk)
    {
        boolean noPreprocess = false;
        switch (lk)
        {
            default:assert false:"Unknown language kind!";
            case langkind_asm_cpp:
                langOption.asmPreprocessor = true;
                // fall through.
            case langkind_cpp:
                noPreprocess = true;
                // fall through
            case langkind_c:
                initializeOption(langOption);
                break;
        }
        langOption.setSymbolVisibility(SymbolVisibility.value);
    }

    enum LangStds
    {
        Lang_unpsecified,
        Lang_c89,
        Lang_c99,
        Lang_c11,
        Lang_gnu89,
        Lang_gnu99,
    }

    private void initializeLangStandard(
            LangOptions options,
            LangKind lk,
            TargetInfo target,
            HashMap<String, Boolean> features)
    {

        // Allow the target to set the default the language options.
        target.getDefaultLangOptions(options);

        // Pass the map of target features to the target for validateion
        // and processing.
        target.handleTargetFeatures(features);

        // set the default language standard to c99.
        if (LangStd.value == Lang_unpsecified)
        {
            switch (lk)
            {
                case langkind_unspecified:
                    assert false : "unknown base language";
                case langkind_c:
                case langkind_asm_cpp:
                case langkind_cpp:
                    LangStd.setValue(LangStds.Lang_c99);
                    break;
            }
        }
        switch (LangStd.value)
        {
            default:assert false:"Unknown language standard!";
            case Lang_gnu99:
            case Lang_c99:
                options.c99 = true;
                options.hexFloats = true;
                // fall through.
            case Lang_gnu89:
                options.bcplComment = true;
                // fall through.
            case Lang_c89:
                // nothing.
                break;
        }

        // Check to see if we are in gnu mode now.
        options.gnuMode = LangStd.value.ordinal() >= Lang_gnu89.ordinal()
                && LangStd.value.ordinal() <= Lang_gnu99.ordinal();

        if (LangStd.value == Lang_c89 || LangStd.value == Lang_gnu89)
            options.implicitInt = true;

        // the trigraph mode is enabled just not in gnu mode or it is specified
        // in command line by user explicitly.
        options.trigraph = !options.gnuMode;
        if (Trigraphs.getPosition() != 0)
            options.trigraph = Trigraphs.value;

        // Default to not accepting '$' in identifiers when preprocessing assembler,
        // but do accept when preprocessing C.
        options.dollarIdents = lk != langkind_asm_cpp;
                // Explicit setting overrides default.
        if (DollarsInIdents.getPosition() != 0)
            options.dollarIdents = DollarsInIdents.value;

        options.optimizeSize = false;

        // -Os implies -O2
        if (OptSize.value || OptLevel.value !=0)
            options.optimize = true;

        options.noInline = !OptSize.value && OptLevel.value == 0;

        if (MainFileName.getPosition() != 0)
            options.setMainFileName(MainFileName.value);
    }

    /**
     * Process the -I option and set them into the headerSearch object.
     * @param headerSearch
     */
    private void initializeIncludePaths(HeaderSearch headerSearch)
    {
        InitHeaderSearch init = new InitHeaderSearch(headerSearch, Verbose.value, Isysroot.value);

        // Handle the -I option.
        if (!I_dirs.isEmpty())
        {
            for (String dir : I_dirs)
            {
                init.addPath(dir, Angled, false);
            }
        }

        // Handle -iquote... options.
        if (!Iquote_dirs.isEmpty())
        {
            for (String dir : Iquote_dirs)
            {
                init.addPath(dir, Quoted, false);
            }
        }

        // Handle -isystem... options.
        if (!Isystem_dirs.isEmpty())
        {
            for (String dir : Isystem_dirs)
                init.addPath(dir, System, false);
        }

        // Add default environment path.
        init.addDefaultEnvVarPaths();

        if (!nostdinc.value)
            init.addDefaultSystemIncludePaths();

        // Now that we have collected all of the include paths, merge them all
        // together and tell the preprocessor about them.
        init.realize();
    }

    /**
     * Process the various options that may affects the target triple and build a
     * final aggregate string that we are compiling for.
     * @return
     */
    private static String createTargetTriple()
    {
        // Initialize base triple.  If a -triple option has been specified, use
        // that triple.  Otherwise, default to the host triple.
        String triple = TargetTriple.value;
        if (triple == null || triple.isEmpty())
            triple = Process.getHostTriple();

        return triple;
    }

    /**
     * Recompute the target feature list to only be the list of things that are
     * enabled, based on the target cpu and feature list.
     * @param target
     * @param features
     */
    private void computeFeatureMap(TargetInfo target, HashMap<String, Boolean> features)
    {
        assert features.isEmpty() :"Invalid map";

        // Initialze the feature map based on the target.
        String targetCPU = TargetCPU.value;
        target.getDefaultFeatures(targetCPU, features);

        if (TargetFeatures.isEmpty())
            return;

        for (int i = 0, e = TargetFeatures.size(); i != e; i++)
        {
            String name = TargetFeatures.get(i);
            char firstCh = name.charAt(0);
            if (firstCh != '-' && firstCh != '+')
            {
                java.lang.System.err.printf("error: xcc: invalid target features string: %s\n", name);
                java.lang.System.exit(EXIT_ERROR);
            }
            if (!target.setFeatureEnabled(features, name.substring(1), firstCh == '+'))
            {
                java.lang.System.err.printf("error: xcc: invalid target features string: %s\n",
                        name.substring(1));
                java.lang.System.exit(EXIT_ERROR);
            }
        }
    }

    //===----------------------------------------------------------------------===//
    // SourceManager initialization.
    //===----------------------------------------------------------------------===//
    private boolean initializeSourceManager(Preprocessor pp, String inFile)
    {
        SourceManager sourceMgr = pp.getSourceManager();
        if(!Objects.equals(inFile, "-"))
        {
            Path file = Paths.get(inFile);
            if (file != null && Files.exists(file))
                sourceMgr.createMainFileID(file, new SourceLocation());
            if (sourceMgr.getMainFileID().isInvalid())
            {
                pp.getDiagnostics().report(new FullSourceLoc(), err_fe_error_reading)
                        .addTaggedVal(inFile).emit();
                return true;
            }
        }
        else
        {
            MemoryBuffer sb = MemoryBuffer.getSTDIN();

            if (sb == null)
            {
                pp.getDiagnostics().report(new FullSourceLoc(),
                        err_fe_error_reading_stdin).emit();
                return true;
            }
            sourceMgr.createMainFileIDForMemBuffer(sb);
            if (sourceMgr.getMainFileID().isInvalid())
            {
                pp.getDiagnostics().report(new FullSourceLoc(),
                        err_fe_error_reading_stdin).emit();
                return true;
            }
        }
        return false;
    }

    /***
     * Defines a Error handler for LLVM backend.
     */
    private ErrorHandling.LLVMErrorHandler errorHandler = new ErrorHandling.LLVMErrorHandler()
    {
        @Override
        public void apply(Diagnostic diag, String msg)
        {
            diag.report(new FullSourceLoc(), err_fe_error_backend).
                    addTaggedVal(msg).emit();
        }
    };

	/**
	 * Programmatic interface for main function.
	 * 
	 * @param args The command line parameters.
	 */
	public int compile(String[] args) throws Exception
    {
        // Initialize Target machine
        TargetSelect ts = TargetSelect.create();
        ts.InitializeTargetInfo();
        ts.LLVMInitializeTarget();

	    // Parse the command line argument.
        CL.parseCommandLineOptions(args, "Extremely C Compiler: https://github.com/JianpingZeng/xcc");
        if (Verbose.value)
        {
            java.lang.System.err.println(NAME +  "version " + VERSION + "on X86 machine");
        }
        if (InputFilenames.isEmpty())
        {
            InputFilenames.add("-");
        }

        int messageLength;
        if (MessageLength.getNumOccurrences() == 0)
            MessageLength.setValue(Process.getStandardErrColumns());

        if (!NoColorDiagnostic.value)
        {
            NoColorDiagnostic.setValue(Process.getStandardErrHasColors());
        }

        DiagnosticClient diagClient = new TextDiagnosticPrinter(
                java.lang.System.err,
                !NoShowColumn.value,
                !NoCaretDiagnostics.value,
                !NoShowLocation.value,
                PrintSourceRangeInfo.value,
                PrintDiagnosticOption.value,
                !NoDiagnosticsFixIt.value,
                MessageLength.value,
                !NoColorDiagnostic.value);

        Diagnostic diag = new Diagnostic(diagClient);

        // Install LLVM error handler, so that any LLVM backend diagnostics
        // go through our error handler.
        ErrorHandling.installLLVMErrorHandler(errorHandler, diag);

        // Get information about the target being compiled for.
        String triple = createTargetTriple();
        TargetInfo target = TargetInfo.createTargetInfo(triple);
        if (target == null)
        {
            diag.report(new FullSourceLoc(), err_fe_unknown_triple).
                    addTaggedVal(triple).emit();
            return EXIT_ERROR;
        }

        // Allocate backend.target machine, default to using X86.
        // targetMachineAllocator = X86TargetMachine::allocateIA32TargetMachine;

        // Parse the Program action.
        ProgramAction progAction = initializeProgAction();

        // Compute the feature set, unfortunately this effects the language!
        HashMap<String, Boolean> features = new HashMap<>();
        computeFeatureMap(target, features);

        SourceManager sourceManager = null;

        for (String inputFile : InputFilenames)
        {
            if (sourceManager == null)
            {
                sourceManager = new SourceManager();
            }
            else
            {
                sourceManager.clearIDTables();
            }

            // Walk through all of source files, initialize LangOptions and Language
            // Standard, and compile option.
            // Instance a Preprocessor.

            // Initialize the language options, inferring file types
            // from input filenames.
            LangOptions langOption = new LangOptions();
            diagClient.setLangOptions(langOption);

            LangKind langkind = getLanguage(inputFile);
            initializeLangOptions(langOption, langkind);
            initializeLangStandard(langOption, langkind, target, features);

            // Handle -I option and set the include search.
            HeaderSearch headerSearch = new HeaderSearch();
            initializeIncludePaths(headerSearch);

            PreprocessorFactory ppFactory = new PreprocessorFactory(diag,
                    langOption, target, sourceManager, headerSearch);
            Preprocessor pp = ppFactory.createAndInitPreprocessor();

            if (pp == null)
                continue;

            // Initialize the source manager with the given input file.
            if (initializeSourceManager(pp, inputFile))
                continue;

            processInputFile(pp, inputFile, progAction, features);
        }

        return EXIT_OK;
	}
}