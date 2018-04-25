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

import backend.codegen.*;
import backend.target.*;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;

public abstract class ScheduleDAG
{
	public MachineBasicBlock mbb;
	public int insertPos;
	public TargetMachine tm;
	public TargetInstrInfo tii;
	public TargetRegisterInfo tri;
	public TargetLowering tli;
	public MachineFunction mf;
	public MachineRegisterInfo mri;
	public MachineConstantPool mcpl;
	public ArrayList<SUnit> sequence;

	public ArrayList<SUnit> sunits;
	public SUnit entrySU;
	public SUnit exitSU;

	public ScheduleDAG(MachineFunction mf)
	{}

	public abstract MachineBasicBlock emitSchedule();

	public void dumpSchedule()
	{}

	protected void run(MachineBasicBlock mbb, int insertPos)
	{}

	protected abstract void buildSchedGraph();

	protected abstract void computeLatency(SUnit su);

	protected void computeOperandLatency(SUnit def, 
		SUnit use, SDep dep)
	{}

	protected abstract void schedule();

	protected boolean forceUnitLatencies()
	{
		return false;
	}

	protected void emitNoop()
	{}

	protected void addMemOperand(MachineInstr mi, MachineMemOperand mmo)
	{}

	protected void emitPhysRegCopy(SUnit su, TObjectIntHashMap<SUnit> vrBaseMap)
	{}

	private void emitLiveCopy(MachineBasicBlock mbb, 
		int insertPos, 
		int virtReg, 
		int physReg, 
		TargetRegisterClass rc,
		TObjectIntHashMap<MachineInstr> copyRegMap)
	{}

	private void emitLiveInCopies(MachineBasicBlock mbb)
	{}
}