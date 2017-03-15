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
import jlang.cparser.*;
import jlang.cparser.Parser;
import jlang.cpp.Preprocessor;
import jlang.diag.Diagnostics;
import jlang.sema.Decl;
import jlang.sema.Sema;
import org.apache.commons.cli.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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
public class Jlang
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

    private static final String VERSION = "0.1";
    private static final String NAME = "xcc";

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
        jlang.cparser.Parser parser = Parser.instance(pp, sema);
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

    private static LangKind getLanguage(String filename)
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

    private static void initializeOption(LangOption options)
    {
        // do nothing.
    }

    private static void initializeLangOptions(LangOption langOption,
            LangKind lk,
            CommandLine cmdline)
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
        langOption.setSymbolVisibility(LangOption.VisibilityMode.valueOf(
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

    private static void initializeLangStandard(LangOption options,
            LangKind lk, CommandLine cmdline)
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
     * @param cmdline
     */
    private static void initializeIncludePaths(HeaderSearch headerSearch,
            CommandLine cmdline)
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

    private static void initializePreprocessorInitOptions(
            PreprocessorInitOptions initOpts, CommandLine cmdline)
    {
        // Add macro from command line.
        String[] defines = cmdline.getOptionValues(D_macros.getOptName());
        if (defines != null && defines.length > 0)
        {
            for (String m : defines)
             initOpts.addMacroDef(m);
        }
        defines = cmdline.getOptionValues(U_macros.getOptName());
        if (defines != null && defines.length > 0)
        {
            for (String u : defines)
                initOpts.addMacroUndef(u);
        }
    }

    /**
     * Append a #define line to buf for macro {@code m}.
     * @param buf
     * @param m
     */
    private static void defineBuiltinMacro(StringBuilder buf, String m)
    {
        String cmd = "#define ";
        buf.append(cmd);
        int eqPos = m.indexOf("=");
        if (eqPos >= 0)
        {
            // Turn 'X=Y' -> 'X Y'
            buf.append(m.substring(0, eqPos)).append(" ");
            int end = m.indexOf("\n\r", eqPos + 1);
            if (end >= 0)
            {
                java.lang.System.err.printf("warning: macro '%s' contains" + " embedded newline, text after the newline is ignored",
                        m.substring(eqPos + 1));
            }
            else
            {
                end = m.length();
            }
            buf.append(m.substring(eqPos+1, end));
        }
        else
        {
            // Push "macroname 1".
            buf.append(m).append(" ").append("1");
        }
        buf.append(File.separator);
    }

    private static void initializePredefinedMacros(LangOption langOptions,
            StringBuilder buf)
    {
        defineBuiltinMacro(buf, "__xcc__=1"); // XCC version.
        if (langOptions.asmPreprocessor)
            defineBuiltinMacro(buf, "__ASSEMBLER__=1");

        if (langOptions.c99)
            defineBuiltinMacro(buf, "__STDC_VERSION__=199901L");
        else if (!langOptions.gnuMode && langOptions.trigraph)
            defineBuiltinMacro(buf, "__STDC_VERSION__=199409L");

        if (!langOptions.gnuMode)
            defineBuiltinMacro(buf, "__STRICT_ANSI=1");

        if (langOptions.optimize)
            defineBuiltinMacro(buf, "__OPTIMIZE__=1");
        if (langOptions.optimizeSize)
            defineBuiltinMacro(buf, "__OPTIMIZE_SIZE__=1");

        // TODO Initialize target-specific preprocessor defines.
    }

    /**
     * Append a #undef line to Buf for Macro.
     * Macro should be of the form XXX and we emit "#undef XXX".
     * @param buf
     * @param name
     */
    private static void undefineBuiltinMacro(StringBuilder buf,
            String name)
    {
        buf.append("#undef ").append(name).append(File.separator);
    }

    private static boolean initializePreprocessor(Preprocessor pp,
            PreprocessorInitOptions initOpts)
    {
        StringBuilder predefinedBuffer = new StringBuilder();
        String lineDirective = "# 1 \"<built-in>\" 3\n";
        predefinedBuffer.append(lineDirective);

        // Install things like __GNUC__, etc.
        initializePredefinedMacros(pp.getLangOption(), predefinedBuffer);

        // Add on the predefines from the driver.  Wrap in a #line directive to report
        // that they come from the command line.
        lineDirective = "# 1 \"<command line>\" 1\n";
        predefinedBuffer.append(lineDirective);

        // Process #define's and #undef's in the order they are given.
        initOpts.getMacros().forEach(pair->
        {
            if (pair.second)
                undefineBuiltinMacro(predefinedBuffer, pair.first);
            else
                defineBuiltinMacro(predefinedBuffer, pair.first);
        });

        // Append a line terminator into predefinedBuffer.
        predefinedBuffer.append(File.separator);
        pp.setPredefines(predefinedBuffer.toString());
        // Once we are reaching this, done!
        return false;
    }

    /**
     * A factory method to create and initialize an object of {@linkplain Preprocessor}.
     * from several arguments.
     * @param diag
     * @param langOptions
     * @param headerSearch
     * @return
     */
    private static Preprocessor createAndInitPreprocessor(Diagnostics diag,
            LangOption langOptions,
            HeaderSearch headerSearch,
            CommandLine cmdline)
    {
        Preprocessor pp = new Preprocessor(diag, langOptions, headerSearch);
        PreprocessorInitOptions initOptions = new PreprocessorInitOptions();
        initializePreprocessorInitOptions(initOptions, cmdline);

        return initializePreprocessor(pp, initOptions)? null: pp;
    }

	/**
	 * Programmatic interface for main function.
	 * 
	 * @param args The command line parameters.
	 */
	public int compile(String[] args)
	{
        if (args.length == 0)
        {
            printUsage();
            return EXIT_CMDERR;
        }

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

        // TODO Initialize a Diagnostic client instance.
        Diagnostics diag = new Diagnostics();

        // TODO Obtains the target triple information.

        // Allocate backend.target machine, default to using X86.
        targetMachineAllocator = TargetMachine::allocateIA32TargetMachine;

        // Parse the Program action.
        ProgramAction progAction = initializeProgAction();

        for (String inputFile : filenames)
        {
            // Walk through all of source files, initialize LangOption and Language
            // Standard, and compile option.
            // Instance a Preprocessor.
            LangKind langkind = getLanguage(inputFile);
            LangOption langOption = new LangOption();
            initializeLangOptions(langOption, langkind, cmdline);
            initializeLangStandard(langOption, langkind, cmdline);

            // Handle -I option and set the include search.
            HeaderSearch headerSearch = new HeaderSearch();
            initializeIncludePaths(headerSearch, cmdline);

            Preprocessor pp = createAndInitPreprocessor(diag, langOption,
                    headerSearch, cmdline);

            if (pp == null)
                continue;
            processInputFile(pp, inputFile, progAction);
        }
        return EXIT_OK;
	}
}
