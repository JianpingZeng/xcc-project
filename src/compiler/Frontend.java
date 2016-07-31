package compiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import parser.ParseException;
import parser.Parser;
import utils.Context;
import utils.Log;
import utils.Name;
import utils.Position;
import ast.Pretty;
import ast.Tree;
import ast.TreeMaker;
import comp.Attr;
import comp.Enter;
import comp.Env;
import comp.Todo;

/**
 * @author Xlous.zeng
 */
public final class Frontend
{
	private final boolean debugParser;

	private Context context;
	private final Name.Table names;
	private final Log log;
	private final TreeMaker make;
	private Enter enter;
	private Todo<Env> todo;
	private Options opt;
	public Frontend(Context context)
	{
		opt = Options.instance(context);		
		debugParser = opt.isDebugParser();		
		this.context = context;
		this.names = Name.Table.instance(context);
		this.log = Log.instance(context);
		this.make = TreeMaker.instance(context);
		this.enter = Enter.instance(context);
		this.todo = Todo.instance(context);
	}
	/**
	 * Parses the single c-flat source file by given filename. So that returns a
	 * TopLevel {@link Tree.TopLevel}
	 *
	 * @param file the file name of source file.
	 * @return The TopLevel.
	 */
	public Tree doParseAttribute(String file)
	{
		// step 1: parse given inputed source file
		Tree root = parse(file, openSourcefile(file));

		// step 2: attributes the generated AST
		if(errorCount() == 0)
		{
			if (debugParser)
			{
				Pretty p = new Pretty(new PrintWriter(System.out), false);

				// display abstract syntax tree for any source file
				root.accept(p);
			}
			// syntax fatal checking and symbol entering
			enter.main(root);

			Attr attr = Attr.instance(context);
			for (Env env : todo)
			{
				// the unattributed syntax tree
				Tree unattributed = env.tree;
				/*if (verbose)
					printVerbose("checking.attribution",
							env.enclMethod.sym.toString());
				*/
				Name prev = log.useSource(env.toplevel.sourceFile);
				attr.attriMethod(unattributed.pos, env.enclMethod.sym);				
				log.useSource(prev);
			}
		}
		return root;
	}

	public Tree[] doParseAttribute(List<SourceFile> files)
	{
		Tree[] treeList = new Tree[files.size()];
		int idx = 0;
		for (SourceFile file : files) {
			String name = file.getCurrentName();
			treeList[idx++] = parse(name, openSourcefile(name));
		}

		// step 2: attributes the generated AST
		if(errorCount() == 0)
		{
			if (debugParser)
			{
				Pretty p = new Pretty(new PrintWriter(System.out), false);

				// display abstract syntax tree for any source file
				for (idx = 0; idx < treeList.length; idx++)
					treeList[idx].accept(p);
			}
			// syntax fatal checking and symbol entering
			enter.main(Arrays.asList(treeList));

			Attr attr = Attr.instance(context);
			for (Env env : todo)
			{
				// the unattributed syntax tree
				Tree unattributed = env.tree;
				/*if (verbose)
					printVerbose("checking.attribution",
							env.enclMethod.sym.toString());
				*/
				Name prev = log.useSource(env.toplevel.sourceFile);
				attr.attriMethod(unattributed.pos, env.enclMethod.sym);
				log.useSource(prev);
			}
		}
		return treeList;
	}

	public Tree.TopLevel parse(String filename, InputStream input)
	{
		Name prev = log.useSource(names.fromString(filename));
		Tree.TopLevel tree = make.TopLevel(Tree.emptyList);
		long msec = System.currentTimeMillis();
		if (input != null)
		{
			if (debugParser) printVerbose("parsing started", filename);
			try
			{
				Parser parser = new Parser(input, true, context);
				tree = parser.compilationUnit();
				if (debugParser)
					printVerbose("parsing.done",
							Long.toString(System.currentTimeMillis() - msec));

			}
			catch (ParseException e)
			{
				log.error(Position.NOPOS, "error.parsing.file", filename);
			}
		}
		log.useSource(prev);
		tree.sourceFile = names.fromString(filename);
		return tree;
	}

	/**
	 * The numbers of errors reported so far.
	 */
	public int errorCount()
	{
		return log.nerrors;
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
	 * Prints debugging information in human readability style.
	 * @param key
	 * @param msg
	 */
	void printVerbose(String key, String msg)
	{
		Log.printLines(log.noticeWriter,
				Log.getLocalizedString("verbose." + key, msg));
	}
}