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

import backend.codegen.MachineInstr;
import backend.target.TargetRegisterClass;

import java.io.PrintStream;
import java.util.ArrayList;

public class SUnit
{
	private SDNode node;
	private MachineInstr instr;

	public SUnit originNode;

	public ArrayList<SDep> preds;
	public ArrayList<SDep> succs;

	public int nodeNum;
	public int nodeQueueId;
	public int latency;
	public int numPredsLeft;
	public int numSuccsLeft;
	public boolean isTwoAddress;
	public boolean isCommutable;
	public boolean hasPhysRegDefs;
	public boolean hasPhysRegClobbers;
	public boolean isPending;
	public boolean isAvailable;
	public boolean isScheduled;
	public boolean isScheduleHigh;
	public boolean isCloned;

	private boolean isDepthCurrent;
	private boolean isHeightCurrent;
	private int depth;
	private int height;

	public TargetRegisterClass copyDstRC, copySrcRC;

	/**
	 * Constructor for DAGNode based Scheduler(pre-ra).
	 */
	public SUnit(SDNode node, int nodeNum)
	{
		this.node = node;
		this.nodeNum = nodeNum;
		preds = new ArrayList<>();
		succs = new ArrayList<>();
	}

	/**
	 * Constructor for MachineInstr based Scheduler(post-ra).
	 */
	public SUnit(MachineInstr instr, int nodeNum)
	{
		this.instr = instr;
		this.nodeNum = nodeNum;
		preds = new ArrayList<>();
		succs = new ArrayList<>();
	}

	/**
	 * Constructor for placeholder SUnit.
	 */
	public SUnit()
	{}

	public void setNode(SDNode n)
	{
		assert instr == null;
		node = n;
	}

	public SDNode getNode()
	{
		assert instr == null;
		return node;	
	}

	public void setInstr(MachineInstr mi)
	{
		assert node == null;
		instr = mi;
	}

	public MachineInstr getInstr()
	{
		assert node == null;
		return instr;	
	}

	public void addPred(SDep d)
	{}

	public void removePred(SDep d)
	{}

	public void addSucc(SDep d)
	{}

	public int getDepth()
	{
		if (!isDepthCurrent) 
			return computeDepth();
		return depth;
	}

	public int getHeight()
	{
		if (!isHeightCurrent)
			return computeHeight();
		return height;
	}

	public void setDepthToAtLeast(int newDepth)
	{

	}

	public void setHeightToAtLeast(int newHeight)
	{}

	public void setDepthDirty()
	{}

	public void setHeightDirty()
	{}

	public boolean isPred(SUnit u)
	{
		return false;
	}

	public boolean isSucc(SUnit u)
	{
		return false;
	}

	public void dump(ScheduleDAG dag)
	{

	}

	public void dumpAll(ScheduleDAG dag)
	{}

	public void print(PrintStream os, ScheduleDAG dag)
	{}

	private int computeHeight()
	{
		return 0;
	}

	private int computeDepth()
	{
		return 0;
	}
}