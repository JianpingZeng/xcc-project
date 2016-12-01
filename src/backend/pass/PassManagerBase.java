package backend.pass;

import tools.Pair;

import java.util.*;

import static backend.pass.PassInfoSupport.getPassInfo;

/**
 * An abstract interface to allow code to add
 * passes to a pass manager without having to hard-code what
 * kind of pass manager it is.
 *
 * @T This generic type parameter represents the type of entity on which different
 * Pass will operates.
 * @PassType Represents the type of pass.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class PassManagerBase<T, PassType extends Pass>
		implements AnalysisResolver
{
	protected ArrayList<ModulePass> passesList;

	protected ArrayList<ImmutablePass> immutablePasses;

	// As the passes are being run, this map contains the
	// analyses that are available to the current pass for use.  This is accessed
	// through the getAnalysisToUpDate() function in this class and in Pass.
	protected LinkedHashMap<PassInfo, Pass> currentAnalysis;
	/**
	 * Keeps track with the las user of the specified pass.
	 * mapping from usage to user.
	 */
	protected HashMap<Pass, Pass> lastUseOf;

	protected PassManagerBase()
	{
		passesList = new ArrayList<>();
		currentAnalysis = new LinkedHashMap<>();
		lastUseOf = new HashMap<>();
		immutablePasses = new ArrayList<>();
	}
	/**
	 * Add a pass tot he queue of passes to be run.
	 * These passes ownership of the pass to the PassManager.
	 * @param p
	 */
	public void add(PassType p)
	{
		//assert (p instanceof ModulePass) :"Not a module pass?";

		AnalysisUsage au = new AnalysisUsage();
		p.getAnalysisUsage(au);

		addRequiredPass(au);

		p.addToPassManager(this, au);
	}

	protected void addRequiredPass(AnalysisUsage au)
	{
		for (PassInfo pi : au.getRequired())
		{
			Pass ps = getAnalysisOrNull(pi);
			if (ps == null)
			{
				Pass ap = pi.createPass();
				if (ap instanceof ImmutablePass)
					add((ImmutablePass)ap);
				else
				{
					try
					{
						PassType ins = (PassType)ap;
						add(ins);
					}
					catch (Exception ex)
					{
						assert false:"Wrong kind of pass for this PassManager!";
					}
				}
			}
		}
	}

	public void add(ImmutablePass imp)
	{
		AnalysisUsage au = new AnalysisUsage();
		imp.getAnalysisUsage(au);

		addRequiredPass(au);
		addPass(imp, au);
	}

	public void addPass(ModulePass p, AnalysisUsage au)
	{
		passesList.add(p);
		// inform other pass manager (include ourselves) that there
		// analyses as being used by this pass.
		for (PassInfo pi : au.getRequired())
			markPassUsed(pi, p);

		makeCurrentAvailable(p);
		lastUseOf.put(p, p);
	}

	protected void makeCurrentAvailable(Pass p)
	{
		PassInfo pi = getPassInfo(p.getClass());
		if (pi != null)
		{
			currentAnalysis.put(pi, p);
		}
	}

	@Override
	public Pass getAnalysisOrNull(PassInfo pi)
	{
		if (currentAnalysis.containsKey(pi))
			return currentAnalysis.get(pi);
		if (immutablePasses.isEmpty())
			return null;

		Pass immutablePass = getImmutablePassOrNull(pi);
		return immutablePass;
	}

	@Override
	public void addPass(ImmutablePass p, AnalysisUsage au)
	{
		immutablePasses.add(p);
		ArrayList<Pair<PassInfo, Pass>> analysisImpls = p.getAnalysisImpls();
		analysisImpls.clear();
		analysisImpls.ensureCapacity(au.getRequired().size());

		for (PassInfo pi : au.getRequired())
		{
			Pass imp = getAnalysisOrNull(pi);
			if (imp == null)
			{
				System.err.println("Analysis '" + imp.getPassName()
						+ "' used but not avaliable!");
			}
			analysisImpls.add(new Pair<>(pi, imp));
		}
		// perform some initialization pass.
		p.initializePass();
	}

	@Override
	public void markPassUsed(PassInfo pi, Pass user)
	{
		if (currentAnalysis.containsKey(pi))
		{
			Pass requiredPass = currentAnalysis.get(pi);
			lastUseOf.put(requiredPass, user);

			// continue to load the required analysis by subpass.
			AnalysisUsage au = new AnalysisUsage();
			requiredPass.getAnalysisUsage(au);

			for (PassInfo info : au.getRequired())
				markPassUsed(info, user);
		}
		else
		{
			assert getAnalysisOrNull(pi) != null
					: "Pass avaliable not found! Perhaps"
					+ "this is a module pass but requiring a function pass";
		}
	}

	protected Pass getImmutablePassOrNull(PassInfo pi)
	{
		for (ImmutablePass im : immutablePasses)
			if (getPassInfo(im.getClass()) == pi)
				return im;

		return null;
	}

	public boolean runPass(PassType mp, T m)
	{
		assert false :"Must be implemented by ModulePassManager";
		return false;
	}

	public abstract String getPMName();

	public boolean run(T m)
	{
		boolean changed = false;
		currentAnalysis.clear();

		// add all immutablePass into the current analysis set.
		for (ImmutablePass immutablePass : immutablePasses)
		{
			PassInfo pi = getPassInfo(immutablePass.getClass());
			if (pi != null)
				currentAnalysis.put(pi, immutablePass);
		}

		// run all of the passes.
		for (Pass pass : passesList)
		{
			AnalysisUsage au = new AnalysisUsage();
			pass.getAnalysisUsage(au);

			ArrayList<Pair<PassInfo, Pass>> analysisImpls = pass.getAnalysisImpls();
			analysisImpls.clear();
			analysisImpls.ensureCapacity(au.getRequired().size());

			for (PassInfo info : au.getRequired())
			{
				Pass impl = getAnalysisOrNull(info);
				if (impl == null)
				{
					System.err.println("Analysis: '" + info.getPassName()
							+ "' used but not available!");
					assert false :"Analysis used but not available!";
				}
				analysisImpls.add(new Pair<>(info, impl));
			}

			// run the sub pass.
			boolean chg = runPass((PassType) pass, m);

			// erase all analysis not in the preserved set.
			if (!au.getPreservedAll())
			{
				HashSet<PassInfo> preserved = au.getPreserved();
				for (Iterator<PassInfo> itr = currentAnalysis.keySet().iterator(); itr.hasNext();)
				{
					// Erase the analysis pass which is not existed in preserved set.
					PassInfo info = itr.next();
					if (!preserved.contains(info))
						currentAnalysis.remove(info);
				}
			}

			// add current pass to the set of passes.
			PassInfo pi = pass.getPassInfo();
		}
		return changed;
	}
}
