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

package backend.codegen.dagisel;

import backend.codegen.MachineFunction;
import backend.target.TargetMachine.CodeGenOpt;
import backend.target.TargetRegisterClass;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.LinkedList;

public class ScheduleDAGFast extends ScheduleDAGSDNodes
{
	private LinkedList<SUnit> availableQueue;

	private int numLiveRegs;
	private ArrayList<SUnit> liveRegDefs;
	private TIntArrayList liveRegCycles;

	public ScheduleDAGFast(MachineFunction mf)
	{
		super(mf);
	}

	public void schedule()
	{}

	public void addPred(SUnit su, SDep d)
	{
		su.addPred(d);
	}

	public void removePred(SUnit su, SDep d)
	{
		su.removePred(d);
	}

	private void releasePred(SUnit su, SDep predEdge)
	{}

	private void releasePredecessors(SUnit su, int curCycle)
	{}

	private void scheduleNotBottomUp(SUnit su, int cycle)
	{}

	private SUnit copyAndMoveSuccessors(SUnit su)
	{
		return null;
	}

	private void insertCopiesAndMoveSuccs(SUnit su, 
		int reg, 
		TargetRegisterClass dstRC,
		TargetRegisterClass srcRC, 
		ArrayList<SUnit> copies)
	{}

	private boolean delayForLiveRegsBottemUp(SUnit su, 
		TIntArrayList lregs)
	{
		return false;
	}

	private void listScheduleBottomUp()
	{}

	protected boolean forceUnitLatencies()
	{
		return true;
	}

	/**
	 * A static factory method used for creating an ScheduleFast pass.
	 */
	public static ScheduleDAGSDNodes createFastDAGScheduler(SelectionDAGISel isel, CodeGenOpt level)
	{
		return new ScheduleDAGFast(isel.mf);
	}
}