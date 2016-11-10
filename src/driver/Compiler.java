package driver;

import frontend.codegen.ModuleBuilder;
import backend.hir.Module;
import backend.lir.backend.ia32.IA32;
import backend.lir.backend.ia32.IA32RegisterConfig;
import frontend.ast.ASTConsumer;
import frontend.sema.ASTContext;
import frontend.sema.Decl;
import frontend.sema.Sema;
import tools.Context;
import tools.Log;
import tools.Position;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Compiler
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

    private Sema sema;
    private ASTContext context;
    private ASTConsumer consumer;
    private Options options;

    public Compiler(Context ctx)
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
    }

    public static Compiler make(Context context)
    {
        return new Compiler(context);
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
        /*
        long msec = System.currentTimeMillis();

        for (SourceFile name : filenames)
        {
            parseAST(name);
        }

        Tree[] trees=null;// = frontend.doParseAttribute(filenames);

        // performs high level IR generation and Module backend.opt
        Module[] hirLists = new Module[trees.length];
        for (int i = 0; i < trees.length; i++)
            hirLists[i++] = (new HIRModuleGenerator(ctx).translate(trees[i]));

        // performs high level IR generation and Module backend.opt
        optimizer.runOnModules(hirLists);

        // emits machine instruction for Module of any TopLevel instance
        backend.emitMachineInst(hirLists);

        if (verbose)
        {
            printVerbose("total",
                    Long.toString(System.currentTimeMillis() - msec));
        }
        int errCount = errorCount();
        if (errCount == 1)
            printCount("error", errCount);
        else
            printCount("error.plural", errCount);
        */
    }

    /**
     * This method is called when processing each input file.
     * It responsible for creating an instance of {@linkplain Module}, containing
     * global Constant for global variable, {@linkplain backend.value.Function}.
     *
     * @param filename
     */
    private void parseAST(SourceFile filename)
    {
        try (FileInputStream reader = new FileInputStream(filename.path()))
        {
            consumer = new ModuleBuilder(filename.getCurrentName(), options);
            consumer.initialize(context);

            createSema(reader);
            frontend.cparser.Parser parser = sema.getParser();
            ArrayList<Decl> declsGroup = new ArrayList<>(16);
            ASTConsumer consumer = sema.getASTConsumer();

            while (!parser.parseTopLevel(declsGroup)) // Not end of file.
            {
                consumer.handleTopLevelDecls(declsGroup);
            }

            consumer.handleTranslationUnit(sema.getASTContext());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void createSema(FileInputStream in)
    {
        sema = new Sema(in, getASTConsumer(), getASTConext());
    }

    private ASTConsumer getASTConsumer()
    {
        assert consumer !=null:"Compiler instance must have an ASTConsumer!";
        return consumer;
    }

    private ASTContext getASTConext()
    {
        assert context !=null:"Compiler instance must have ASTContext!";
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
        Log.printLines(log.noticeWriter,
                Log.getLocalizedString("verbose." + key, msg));
    }

    private InputStream openSourcefile(String filename)
    {
        try
        {
            File f = new File(filename);
            return new FileInputStream(f);
        }
        catch (IOException e)
        {
            log.error(Position.NOPOS, "cannot.read.file", filename);
            return null;
        }
    }
    /**
     * Close the driver, flushing the logs
     */
    public void close()
    {
        log.flush();
    }
}
