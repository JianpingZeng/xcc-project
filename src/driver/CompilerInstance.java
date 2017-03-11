package driver;

import backend.hir.Module;
import backend.target.TargetMachine;
import driver.SourceFile.FileType;
import jlang.ast.ASTConsumer;
import jlang.codegen.BackendConsumer;
import jlang.sema.Decl;
import jlang.sema.Sema;
import org.apache.commons.cli.CommandLine;
import tools.Context;
import tools.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static driver.BackendAction.Backend_EmitAssembly;
import static driver.BackendAction.Backend_EmitIr;
import static driver.CompileOptions.InliningMethod.NormalInlining;
import static driver.CompileOptions.InliningMethod.OnlyAlwaysInlining;
import static driver.ProgramAction.EmitIR;
import static driver.SourceFile.FileType.AsmSource;
import static driver.SourceFile.FileType.IRSource;

public class CompilerInstance
{
    private static final Context.Key compilerKey = new Context.Key();

    /**
     * The log to be used for error reporting.
     */
    private Log log;

    private Context ctx = null;

	/**
	 * The program action which this compiler instance will takes.
     * Default to ParseASTOnly.
     */
    private ProgramAction progAction;

	/**
     * A functional interface used to create a backend.target machine specified by command
     * line argument, default to X86.
     */
    private Function<Module, TargetMachine> targetMachineAllocator;
    private CommandLine cmdLine;

    private CompilerInstance(CommandLine cmdLine)
    {
        super();
        ctx.put(compilerKey, this);
        log = Log.instance(ctx);
        this.cmdLine = cmdLine;

        // Set the program action as ParseSyntaxOnly by default.
        progAction = ProgramAction.ParseSyntaxOnly;
    }

    public static CompilerInstance construct(CommandLine cmdline)
    {
        return new CompilerInstance(cmdline);
    }

    /**
     * The id of errors reported so far.
     */
    public int errorCount()
    {
        return log.nerrors;
    }

    /**
     * This is the main entry point for compiling the specified list of source files.
     * @param filenames A list of sources file to be processed.
     */
    public void compile(List<SourceFile> filenames)
    {
        long msec = System.currentTimeMillis();

        // Allocate backend.target machine, default to using X86.
        targetMachineAllocator = TargetMachine::allocateIA32TargetMachine;

        // Sets the program action from command line option.
        if (cmdLine.hasOption(ProgramAction.ASTDump.getOptName()))
            progAction = ProgramAction.ASTDump;
        if (cmdLine.hasOption(ProgramAction.ParseSyntaxOnly.getOptName()))
            progAction = ProgramAction.ParseSyntaxOnly;
        if (cmdLine.hasOption(ProgramAction.GenerateAsmCode.getOptName()))
            progAction = ProgramAction.GenerateAsmCode;

        for (SourceFile name : filenames)
        {
            processInputFile(name);
        }

        int errCount = errorCount();
        if (errCount >= 1)
            printCount("error", errCount);

        if (cmdLine.hasOption(ProgramAction.Verbose.getOptName()))
        {
            printVerbose("total",Long.toString(System.currentTimeMillis() - msec));
        }
        
        // close logger stream.
        close();
    }

    /**
     * This method is called when processing each input file.
     * It responsible for creating an instance of {@linkplain Module}, containing
     * global Constant for global variable, {@linkplain backend.value.Function}.
     *
     * @param filename
     */
    private void processInputFile(SourceFile filename)
    {
        // TODO: 1.identify language options according to file kind.
        // TODO: 2.initialize language standard according command line
        // TODO: and input file kind.
        // TODO: 3. creating a preprocessor from langOptions and LangStandard.

        ASTConsumer consumer;
        FileOutputStream os;
        try (FileInputStream reader = new FileInputStream(filename.path()))
        {
            switch (progAction)
            {
                default:
                case ParseSyntaxOnly:
                case ASTDump:
                    assert false:"Unsupported currently.";
                    return;
                case EmitIR:
                case GenerateAsmCode:
                {
                    BackendAction act;
                    if(progAction == EmitIR)
                    {
                        act = Backend_EmitIr;
                        os = computeOutFile(filename, IRSource);
                    }
                    else
                    {
                        act = Backend_EmitAssembly;
                        os = computeOutFile(filename, AsmSource);
                    }

                    CompileOptions compOpts = initializeCompileOptions();
                    consumer = new BackendConsumer(act,
                            compOpts,
                            filename.getCurrentName(),
                            os, ctx, targetMachineAllocator);
                    consumer.initialize();
                }
            }

            parseAST(reader, consumer);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void parseAST(FileInputStream in, ASTConsumer consumer)
    {
        Sema sema = new Sema(in, consumer);

        jlang.cparser.Parser parser = sema.getParser();
        ArrayList<Decl> declsGroup = new ArrayList<>(16);

        while (!parser.parseTopLevel(declsGroup)) // Not end of file.
        {
            consumer.handleTopLevelDecls(declsGroup);
        }

        consumer.handleTranslationUnit();
    }

    private FileOutputStream computeOutFile(SourceFile infile,
            FileType kind)
    {
        String outfile = infile.replaceExtension(kind);
        try
        {
            return new FileOutputStream(outfile);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    private CompileOptions initializeCompileOptions()
    {
        CompileOptions compOpt = new CompileOptions();
        String optLevel = cmdLine.getOptionValue(ProgramAction.OptSpeed.getOptName());
        if(optLevel != null)
        {
            compOpt.optimizationLevel = Byte.parseByte(optLevel);
        }
        compOpt.optimizeSize = cmdLine.hasOption(ProgramAction.OptSize.getOptName());

        if (compOpt.optimizationLevel > 1)
            compOpt.inlining = NormalInlining;
        else
            compOpt.inlining = OnlyAlwaysInlining;

        compOpt.unrollLoops = compOpt.optimizationLevel>1;
        compOpt.debugInfo = cmdLine.hasOption(ProgramAction.
                GenerateDebugInfo.getOptName());
        return compOpt;
    }

    /**
     * Prints numbers of errors and warnings.
     *
     * @param key The key massage to be reported.
     * @param cnt The count of errors and warnings.
     */
    private void printCount(String key, int cnt)
    {
        if (cnt != 0)
        {
            Log.printLines(
                    log.errWriter,
                    Log.getLocalizedString("count." + key,
                            Integer.toString(cnt)));
            log.flush();
        }
    }

    private void printVerbose(String key, String msg)
    {
        Log.printLines(log.noticeWriter, Log.getLocalizedString("verbose." + key, msg));
    }

    /**
     * Close the driver, flushing the logs
     */
    public void close()
    {
        log.flush();
    }
}
