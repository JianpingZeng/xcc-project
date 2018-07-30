/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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

import tools.Util;
import backend.codegen.*;
import backend.support.DefaultDotGraphTrait;
import backend.support.GraphWriter;
import backend.target.*;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.Util;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static backend.support.GraphWriter.writeGraph;

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
	{
		tm = mf.getTarget();
		tii = tm.getInstrInfo();
		tri = tm.getRegisterInfo();
		tli = tm.getTargetLowering();
		this.mf = mf;
		mri = mf.getMachineRegisterInfo();
		mcpl = mf.getConstantPool();
		sequence = new ArrayList<>();
		sunits = new ArrayList<>();
		entrySU = new SUnit();
		exitSU = new SUnit();
	}

	public MachineFunction getMachineFunction()
	{
		return mf;
	}

	public abstract MachineBasicBlock emitSchedule();

	public void dumpSchedule()
	{
		sequence.forEach(seq->
		{
			if (seq!= null)
				seq.dump(this);
			else 
				System.err.println("**** NOOP ****");
		});
	}

	public abstract void dumpNode(SUnit su);

	public abstract String getGraphNodeLabel(SUnit su);

	protected void run(SelectionDAG dag, MachineBasicBlock mbb, int insertPos)
	{
		this.mbb = mbb;
		this.insertPos = insertPos;
		sunits.clear();
		sequence.clear();
		entrySU = new SUnit();
		exitSU = new SUnit();
		schedule();

		if (Util.DEBUG)
		{
			System.err.println("*** Final Schedule ***");
			dumpSchedule();
			System.err.println();
		}
	}

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
	{
		tii.insertNoop(mbb, insertPos++);
	}

	protected void addMemOperand(MachineInstr mi, MachineMemOperand mmo)
	{
		mi.addMemOperand(mmo);
	}

	protected void emitPhysRegCopy(SUnit su, TObjectIntHashMap<SUnit> vrBaseMap)
	{
		for (SDep d : su.preds)
		{
			if (d.isCtrl()) continue;
			if (d.getSUnit().copyDstRC != null)
			{
				Util.assertion(vrBaseMap.containsKey(d.getSUnit()), "Node emitted out of order!");
				int reg = 0;
				for (SDep s : su.succs)
				{
					if (s.getReg() != 0)
					{
						reg = s.getReg();
						break;
					}
				}
				tii.copyRegToReg(mbb, insertPos++, reg, vrBaseMap.get(d.getSUnit()), 
					su.copyDstRC, su.copySrcRC);
			}
			else 
			{
				Util.assertion(d.getReg() != 0, "Unknown physical register!");
				int vrBase = mri.createVirtualRegister(su.copyDstRC);
				Util.assertion( !vrBaseMap.containsKey(su));
				vrBaseMap.put(su, vrBase);
				tii.copyRegToReg(mbb, insertPos++, vrBase, d.getReg(), 
					su.copyDstRC, su.copySrcRC);
			}
			break;
		}
	}

	public void viewGraph()
	{
		viewGraph("");
	}

	public void viewGraph(String title)
	{
		String funcName = getMachineFunction().getFunction().getName();
		String filename = "dag." + funcName + ".dot";
		DefaultDotGraphTrait trait = DefaultDotGraphTrait.createScheduleDAGTrait(this, false);
		GraphWriter.viewGraph(title, filename, trait);
	}

	public void addCustomGraphFeatures(ScheduleDAGDotTraits graphWriter) {}
}