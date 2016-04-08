package driver;

import ast.Tree;
import hir.HIR;
import hir.HIRGenerator;
import utils.Context;

/**
 * @author Jianping Zeng
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
	 * Emits HIR(High level Intermediate Representation) for speicifed TopLevel
	 * tree.
	 * @param trees a list of {@code TopLevel} instance to be HIRifed.
	 * @return  a array of Emitted HIR instance.
	 */

	public HIR[] emitHIR(Tree[] trees)
	{
		HIR[] hirs = new HIR[trees.length];
		int idx = 0;
		for (Tree tree : trees)
			hirs[idx++]  = hirGenerator.translate(tree);
		return hirs;
	}

	/**
	 * Emits HIR(High level Intermediate Representation) for speicifed TopLevel
	 * tree.
	 * @param tree  the {@code TopLevel} instance to be HIRifed.
	 * @return  emitted HIR instance.
	 */

	public HIR emitHIR(Tree tree)
	{
		return hirGenerator.translate(tree);
	}
}
