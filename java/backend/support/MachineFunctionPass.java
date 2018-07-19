/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package backend.support;

import backend.codegen.MachineFunction;
import backend.codegen.MachineFunctionAnalysis;
import backend.pass.Pass;
import tools.Util;
import backend.analysis.*;
import backend.pass.AnalysisResolver;
import backend.pass.AnalysisUsage;
import backend.pass.FunctionPass;
import backend.value.Function;

import java.io.PrintStream;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class MachineFunctionPass implements FunctionPass
{
	private AnalysisResolver resolver;

	@Override
	public void setAnalysisResolver(AnalysisResolver resolver)
	{
		this.resolver = resolver;
	}

	@Override
	public AnalysisResolver getAnalysisResolver()
	{
		return resolver;
	}

	/**
	 * This method must be overridded by concrete subclass for performing
	 * desired machine code transformation or analysis.
	 * @param mf
	 * @return
	 */
	public abstract boolean runOnMachineFunction(MachineFunction mf);

	/**
	 * This method will be passed by {@linkplain #runOnMachineFunction(MachineFunction)}
	 * @param f
	 * @return
	 */
	@Override
	public boolean runOnFunction(Function f)
	{
		MachineFunction mf = f.getMachineFunc();
		Util.assertion(mf != null, "Instruction selector did not be runned?");
		return runOnMachineFunction(mf);
	}

	/**
	 * Subclasses that override getAnalysisUsage
	 * must call this.
	 * @param au
	 */
	@Override
	public void getAnalysisUsage(AnalysisUsage au)
	{
		au.addRequired(MachineFunctionAnalysis.class);
		au.addPreserved(MachineFunctionAnalysis.class);

		// TODO 2017/11/19 au.addPreserved(AliasAnalysis.class);
		au.addPreserved(ScalarEvolution.class);
		au.addPreserved(IVUsers.class);
		// au.addPreserved(LoopDependenceAnalysis.class);
		// au.addPreserved(MemoryDependenceAnalysis.class);
		au.addPreserved(DomTree.class);
		au.addPreserved(DominanceFrontier.class);
		au.addPreserved(LoopInfo.class);

		FunctionPass.super.getAnalysisUsage(au);
	}

	@Override
	public Pass createPrinterPass(PrintStream os, String banner)
	{
		return PrintMachineFunctionPass.createMachineFunctionPrinterPass(os, banner);
	}
}
