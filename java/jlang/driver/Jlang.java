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

import backend.hir.Module;
import backend.target.TargetMachine;
import jlang.ast.ASTConsumer;
import jlang.basic.*;
import jlang.basic.ProgramAction;
import jlang.codegen.BackendConsumer;
import jlang.cpp.Preprocessor;
import jlang.cpp.SourceLocation;
import jlang.diag.*;
import jlang.sema.Decl;
import jlang.sema.Sema;
import jlang.system.Process;
import org.apache.commons.cli.*;
import tools.commandline.CL;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

import static jlang.basic.BackendAction.Backend_EmitAssembly;
import static jlang.basic.BackendAction.Backend_EmitIr;
import static jlang.basic.CompileOptions.InliningMethod.NormalInlining;
import static jlang.basic.CompileOptions.InliningMethod.OnlyAlwaysInlining;
import static jlang.basic.InitHeaderSearch.IncludeDirGroup.*;
import static jlang.basic.InitHeaderSearch.IncludeDirGroup.System;
import static jlang.basic.ProgramAction.*;
import static jlang.driver.Jlang.LangStd.*;

/**
 * This class is used as programmatic interface for handling command line
 * options.
 * 
 * @author xlous.zeng
 *
 */
public class Jlang implements DiagnosticFrontendKindsTag
{
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

    /**
     * A list used for residing all of Option corresponding to legal command line
     * option and argument.
     * <p>
     * Note that: all the {@code Option} here are follow the style of GNU.
     */
    private static ArrayList<Option> allOptions = new ArrayList<>();

    static
    {
        for (ProgramAction action : ProgramAction.values())
        {
            allOptions.add(new CustomOption(action.getOptName(),
                    action.isHasArg(), action.getDesc(), action.getChecker()));
        }
    }

    /**
     * A table of all options that's passed to the JavaCompiler constructor.
     */
    private Options options = new Options();

    /**
     * The list of files to process
     */
    private List<String> filenames = new LinkedList<>();

    private CommandLine cmdline = null;

    private Function<Module, TargetMachine> targetMachineAllocator;

    private void printUsage(String msg)
    {
        java.lang.System.err.println(msg);
        new HelpFormatter().printHelp(NAME, options);
        java.lang.System.exit(EXIT_OK);
    }

    /**
     * Print a string that explains usage.
     */
    private void printUsage()
    {
        new HelpFormatter().printHelp(NAME, options);
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

    /**
     * Process command line arguments: store all command line options in
     * `options' table and return all source filenames.
     *
     * @param args An array of all of arguments.
     * @return
     */
    private List<String> processArgs(String[] args)
    {
        List<String> files = new LinkedList<>();

        try
        {
            CommandLineParser cmdParser = new DefaultParser();
            cmdline = cmdParser.parse(options, args);

            // add the left argument as the file to be parsed.
            files.addAll(cmdline.getArgList());
        }
        catch (ParseException ex)
        {
            printUsage(ex.getMessage());
        }
        return files;
    }

    private void parseAST(Preprocessor pp, ASTConsumer consumer)
    {
        Sema sema = new Sema(pp, consumer);
        jlang.cparser.Parser parser = sema.getParser();
        ArrayList<Decl> declsGroup = new ArrayList<>(16);

        while (!parser.parseTopLevel(declsGroup)) // Not end of file.
        {
            consumer.handleTopLevelDecls(declsGroup);
        }

        consumer.handleTranslationUnit();
    }

    private OutputStream computeOutFile(String infile,
            String extension, StringBuilder outPath)
    {
        boolean usestdout = false;
        String outfile = cmdline.getOptionValue(OutputFile.getOptName());
        OutputStream os = null;
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
                os = new FileOutputStream(file);
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
     * This method is called when processing each input file.
     * It responsible for creating an instance of {@linkplain Module}, containing
     * global Constant for global variable, {@linkplain backend.value.Function}.
     *
     * @param infile
     */
    private void processInputFile(Preprocessor pp,
            String infile,
            ProgramAction progAction)
    {
        ASTConsumer consumer;
        OutputStream os;
        StringBuilder outpath = new StringBuilder();
        switch (progAction)
        {
            default:
            case ParseSyntaxOnly:
            case ASTDump:
                assert false : "Unsupported currently.";
                return;
            case EmitIR:
            case GenerateAsmCode:
            {
                BackendAction act;
                if (progAction == EmitIR)
                {
                    act = Backend_EmitIr;
                    os = computeOutFile(infile, "ll", outpath);
                }
                else
                {
                    act = Backend_EmitAssembly;
                    os = computeOutFile(infile, "s", outpath);
                }

                CompileOptions compOpts = initializeCompileOptions();
                consumer = new BackendConsumer(act, compOpts,
                        infile, os, null,
                        targetMachineAllocator);
                consumer.initialize();
            }
        }

        try
        {
            InputStream is;
            if (!infile.equals("-"))
            {
                is = new FileInputStream(infile);
                pp.addInput(is, infile);
            }
            else
            {
                is = java.lang.System.in;
                pp.addInput(is, "<stdin>");
            }
        }
        catch (IOException e)
        {
            error(e.getMessage());
            java.lang.System.exit(0);
        }
        parseAST(pp, consumer);
    }

    private CompileOptions initializeCompileOptions()
    {
        CompileOptions compOpt = new CompileOptions();
        String optLevel = cmdline.getOptionValue(OptSpeed.getOptName());
        if (optLevel != null)
        {
            compOpt.optimizationLevel = Byte.parseByte(optLevel);
        }
        compOpt.optimizeSize = cmdline.hasOption(OptSize.getOptName());

        if (compOpt.optimizationLevel > 1)
            compOpt.inlining = NormalInlining;
        else
            compOpt.inlining = OnlyAlwaysInlining;

        compOpt.unrollLoops = compOpt.optimizationLevel > 1;
        compOpt.debugInfo = cmdline.hasOption(ProgramAction.
                GenerateDebugInfo.getOptName());
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
        // Sets the program action from command line option.
        if (cmdline.hasOption(ProgramAction.ASTDump.getOptName()))
            progAction = ProgramAction.ASTDump;
        if (cmdline.hasOption(ProgramAction.ParseSyntaxOnly.getOptName()))
            progAction = ProgramAction.ParseSyntaxOnly;
        if (cmdline.hasOption(ProgramAction.GenerateAsmCode.getOptName()))
            progAction = ProgramAction.GenerateAsmCode;
        return progAction;
    }

    private LangKind getLanguage(String filename)
    {
        int lastDotPos = filename.lastIndexOf('.');
        if (lastDotPos < 0)
        {
            return LangKind.Langkind_c;
        }

        String ext = filename.substring(lastDotPos + 1);
        // C header: .h
        // assembly no preprocessing: .s
        // assembly: .S
        switch (ext)
        {
            default:
            case "c":
                return LangKind.Langkind_c;
            case "S":
            case "s":
                return LangKind.Langkind_asm_cpp;
            case "i":
                return LangKind.Langkind_cpp;
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
            case Langkind_asm_cpp:
                langOption.asmPreprocessor = true;
                // fall through.
            case Langkind_cpp:
                noPreprocess = true;
                // fall through
            case Langkind_c:
                initializeOption(langOption);
                break;
        }
        langOption.setSymbolVisibility(LangOptions.VisibilityMode.valueOf(
                cmdline.getOptionValue("fvisibility", "Default")
        ));
    }

    enum LangStd
    {
        Lang_unpsecified,
        Lang_c89,
        Lang_c99,
        Lang_c11,
        Lang_gnu89,
        Lang_gnu99,
    }

    private void initializeLangStandard(LangOptions options,
            LangKind lk)
    {
        String std = cmdline.getOptionValue(ProgramAction.Std.getOptName());
        LangStd langStd = std != null? LangStd.valueOf(std) : Lang_unpsecified;
        // set the default language standard to c99.
        if (langStd == Lang_unpsecified)
        {
            switch (lk)
            {
                case Langkind_unspecified:
                    assert false : "unknown base language";
                case Langkind_c:
                case Langkind_asm_cpp:
                case Langkind_cpp:
                    langStd = LangStd.Lang_c99;
                    break;
            }
        }
        switch (langStd)
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
        options.gnuMode = langStd.ordinal() >= Lang_gnu89.ordinal()
                && langStd.ordinal() <= Lang_gnu99.ordinal();
        if (langStd == Lang_c89 || langStd == Lang_gnu89)
            options.implicitInt = true;

        // the trigraph mode is enabled just not in gnu mode or it is specified
        // in command line by user explicitly.
        options.trigraph = !options.gnuMode
                || cmdline.hasOption(Trigraph.getOptName());
        // Default to not accepting '$' in identifiers when preprocessing assembler,
        // but do accept when preprocessing C.
        options.dollarIdents = lk != LangKind.Langkind_asm_cpp
                // Explicit setting overrides default.
                || cmdline.hasOption(ProgramAction.DollarInIdents.getOptName());

        if (cmdline.hasOption(OptSize.getOptName())
                || cmdline.getOptionValue(OptSpeed.getOptName()) != null)
            options.optimize = true;
    }

    /**
     * Process the -I option and set them into the headerSearch object.
     * @param headerSearch
     */
    private void initializeIncludePaths(HeaderSearch headerSearch)
    {
        boolean v = cmdline.hasOption(Verbose.getOptName());
        String isysroot = cmdline.getOptionValue(Isysroot.getOptName(), "/");
        InitHeaderSearch init = new InitHeaderSearch(headerSearch, v, isysroot);

        // Handle the -I option.
        String[] idirs = cmdline.getOptionValues(I_dirs.getOptName());
        if (idirs != null && idirs.length > 0)
        {
            for (String dir : idirs)
            {
                init.addPath(dir, Angled, false);
            }
        }

        // Handle -iquote... options.
        String[] iquote_dirs = cmdline.getOptionValues(Iquote_dirs.getOptName());
        if (iquote_dirs != null && iquote_dirs.length > 0)
        {
            for (String dir : iquote_dirs)
            {
                init.addPath(dir, Quoted, false);
            }
        }

        // Handle -isystem... options.
        String[] isystem_dirs = cmdline.getOptionValues(Isystem.getOptName());
        if (isystem_dirs != null && isystem_dirs.length > 0)
        {
            for (String dir : isystem_dirs)
                init.addPath(dir, System, false);
        }

        // Add default environment path.
        init.addDefaultEnvVarPaths();

        if (!cmdline.hasOption(Nostdinc.getOptName()))
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
    private static String createTargetTriple(CommandLine cmdline)
    {
        // Initialize base triple.  If a -triple option has been specified, use
        // that triple.  Otherwise, default to the host triple.
        String triple = cmdline.getOptionValue(TRIPLE.getOptName());
        if (triple != null)
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
        String targetCPU = cmdline.getOptionValue(TARGET_CPU.getOptName(), "");
        target.getDefaultFeatures(targetCPU, features);

        String[] targetFeatures = cmdline.getOptionValues(TARGET_FEATURE.getOptName());
        if (targetFeatures == null || targetFeatures.length == 0)
            return;

        for (String name : targetFeatures)
        {
            char firstCh = name.charAt(0);
            if (firstCh != '-' && firstCh != '+')
            {
                java.lang.System.err.printf("error: xcc: invalid target features string: %s\n", name);
                java.lang.System.exit(EXIT_ERROR);
            }
            if (!target.setFeatureEnabled(features, name.substring(1), firstCh == '+'))
            {
                java.lang.System.err.printf("error: xcc: invalid target features string: %s\n", name.substring(1));
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
                        .addTaggedVal(inFile);
                return true;
            }
        }
        else
        {
            MemoryBuffer sb = MemoryBuffer.getSTDIN();

            if (sb == null)
            {
                pp.getDiagnostics().report(new FullSourceLoc(), err_fe_error_reading_stdin);
                return true;
            }
            sourceMgr.createMainFileIDForMemBuffer(sb);
            if (sourceMgr.getMainFileID().isInvalid())
            {
                pp.getDiagnostics().report(new FullSourceLoc(), err_fe_error_reading_stdin);
                return true;
            }
        }
        return false;
    }

	/**
	 * Programmatic interface for main function.
	 * 
	 * @param args The command line parameters.
	 */
	public int compile(String[] args)
	{
	    // Parse the command line argument.
        CL.parseCommandLineOptions(args);

        // load all options.
        allOptions.forEach(options::addOption);

        // process command line arguments
        List<String> filenames = processArgs(args);
        if (cmdline.hasOption(Verbose.getOptName()))
        {
            java.lang.System.err.println(NAME +  "version " + VERSION + "on X86 machine");
        }

        if (filenames.isEmpty())
        {
            filenames.add("-");
        }

        int messageLength;
        String val = cmdline.getOptionValue(F_MESSAGE_LENGTH.getOptName());
        messageLength = val != null ? Integer.parseInt(val) : Process.getStandardErrColumns();

        DiagnosticClient client = new TextDiagnosticClient(java.lang.System.err,
                true, true, true, true, true, true, messageLength, false);

        Diagnostic diag = new Diagnostic(client);

        // Get information about the target being compiled for.
        String triple = createTargetTriple(cmdline);
        TargetInfo target = TargetInfo.createTargetInfo(triple);
        if (target == null)
        {
            diag.report(new FullSourceLoc(), err_fe_unknown_triple).addTaggedVal(triple);
            return EXIT_ERROR;
        }

        // Allocate backend.target machine, default to using X86.
        targetMachineAllocator = TargetMachine::allocateIA32TargetMachine;

        // Parse the Program action.
        ProgramAction progAction = initializeProgAction();

        // Compute the feature set, unfortunately this effects the language!
        HashMap<String, Boolean> features = new HashMap<>();
        computeFeatureMap(target, features);


        SourceManager sourceManager = null;

        for (String inputFile : filenames)
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
            LangKind langkind = getLanguage(inputFile);
            LangOptions langOption = new LangOptions();
            initializeLangOptions(langOption, langkind);
            initializeLangStandard(langOption, langkind);

            // Handle -I option and set the include search.
            HeaderSearch headerSearch = new HeaderSearch();
            initializeIncludePaths(headerSearch);

            PreprocessorFactory ppFactory = new PreprocessorFactory(diag,
                    langOption, target, sourceManager, headerSearch, cmdline);
            Preprocessor pp = ppFactory.createAndInitPreprocessor();

            if (pp == null)
                continue;

            // Initialize the source manager with the given input file.
            if (initializeSourceManager(pp, inputFile))
                continue;

            processInputFile(pp, inputFile, progAction);
        }
        return EXIT_OK;
	}
}
