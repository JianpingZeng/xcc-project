package driver;

import ast.Tree;
import hir.Module;
import hir.HIRGenerator;
import utils.Context;

/**
 * @author Xlous.zeng
 */
public final class Optimizer
{
	final Context context;
	final HIRGenerator hirGenerator;
	public Optimizer(Context context)
	{
		this.context = context;
		hirGenerator = new HIRGenerator(context);
	}

	/**
	 * Emits Module(High level Intermediate Representation) for speicifed TopLevel
	 * tree.
	 * @param trees a list of {@code TopLevel} instance to be HIRifed.
	 * @return  a array of Emitted Module instance.
	 */

	public Module[] emitHIR(Tree[] trees)
	{
		Module[] hirs = new Module[trees.length];
		int idx = 0;
		for (Tree tree : trees)
			hirs[idx++]  = hirGenerator.translate(tree);
		return hirs;
	}

	/**
	 * Emits Module(High level Intermediate Representation) for speicifed TopLevel
	 * tree.
	 * @param tree  the {@code TopLevel} instance to be HIRifed.
	 * @return  emitted Module instance.
	 */

	public Module emitHIR(Tree tree)
	{
		return hirGenerator.translate(tree);
	}
}
