package driver;

import driver.SourceFile.FileType;
import jlang.codegen.BackendConsumer;
import backend.hir.Module;
import backend.lir.backend.ia32.IA32;
import backend.lir.backend.ia32.IA32RegisterConfig;
import jlang.ast.ASTConsumer;
import jlang.sema.ASTContext;
import jlang.sema.Decl;
import jlang.sema.Sema;
import backend.target.TargetMachine;
import tools.Context;
import tools.Log;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static driver.BackendAction.Backend_EmitAssembly;
import static driver.BackendAction.Backend_EmitHir;
import static driver.CompileOptions.InliningMethod.NormalInlining;
import static driver.CompileOptions.InliningMethod.OnlyAlwaysInlining;
import static driver.ProgramAction.EmitHIR;
import static driver.SourceFile.FileType.AsmSource;
import static driver.SourceFile.FileType.HirSource;

public class CompilerInstance
{
    private static final Context.Key compilerKey = new Context.Key();

    /**
     * The log to be used for error reporting.
     */
    private Log log;

    private Context ctx;
    /**
     * A flag that marks whether debug parer.
     */
    private boolean debugParser = false;

    private boolean verbose = false;

    /**
     * A flag that marks whether output TargetData file.
     */
    @SuppressWarnings("unused")
    private boolean outputResult = false;

    private Backend backend;
    private Optimizer optimizer;

    private ASTContext context;
    private Options options;

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

    public CompilerInstance(Context ctx)
    {
        super();
        ctx.put(compilerKey, this);
        this.ctx = ctx;
        this.log = Log.instance(ctx);
        options = Options.instance(ctx);

        // TODO: to specify machine specification with command line option.
        Backend backend = new IA32Backend(ctx, IA32.target(),
                IA32RegisterConfig.newInstance());
        Optimizer optimizer = new Optimizer(ctx);

        this.debugParser = options.get("--debug-Parser") != null;
        this.outputResult = options.get("-o") != null;
        context = new ASTContext();
        progAction = ProgramAction.ParseSyntaxOnly;
    }

    public static CompilerInstance make(Context context)
    {
        return new CompilerInstance(context);
    }

    /**
     * The id of errors reported so far.
     */
    public int errorCount()
    {
        return log.nerrors;
    }

    public void compile(List<SourceFile> filenames)
    {
        long msec = System.currentTimeMillis();

        // Allocate backend.target machine, default to using X86.
        targetMachineAllocator = TargetMachine::allocateIA32TargetMachine;

        // Sets the program action from command line option.
        if (options.isDumpAst())
            progAction = ProgramAction.ASTDump;
        if (options.isParseASTOnly())
            progAction = ProgramAction.ParseSyntaxOnly;
        if (options.isEmitAssembly())
            progAction = ProgramAction.EmitAssembly;

        for (SourceFile name : filenames)
        {
            processInputFile(name);
        }
        if (verbose)
        {
            printVerbose("total",Long.toString(System.currentTimeMillis() - msec));
        }
        int errCount = errorCount();
        if (errCount == 1)
            printCount("error", errCount);
        else
            printCount("error.plural", errCount);

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
                case EmitHIR:
                case EmitAssembly:
                {
                    BackendAction act;
                    if(progAction == EmitHIR)
                    {
                        act = Backend_EmitHir;
                        os = computeOutFile(filename, HirSource);
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
        if(options.optLevel() != null)
        {
            compOpt.optimizationLevel = Byte.parseByte(options.optLevel());
        }

        if (compOpt.optimizationLevel > 1)
            compOpt.inlining = NormalInlining;
        else
            compOpt.inlining = OnlyAlwaysInlining;

        compOpt.unrollLoops = compOpt.optimizationLevel>1;
        compOpt.debugInfo = options.enableDebug();
        return compOpt;
    }

    private ASTContext getASTConext()
    {
        assert context !=null:"CompilerInstance instance must have ASTContext!";
        return context;
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
    public void close(){log.flush();}
}
