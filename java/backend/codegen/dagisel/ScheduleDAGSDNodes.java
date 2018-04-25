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

import backend.codegen.MachineBasicBlock;
import backend.codegen.MachineFunction;
import backend.codegen.MachineInstr;
import backend.codegen.fastISel.ISD;
import backend.target.TargetInstrDesc;
import gnu.trove.map.hash.TObjectIntHashMap;

/**
 * A ScheduleDAG for scheduling SDNode-based DAGs.
 * 
 * Edges between SUnits are initially based on edges in the SelectionDAG,
 * and additional edges can be added by the schedulers as heuristics.
 * SDNodes such as Constants, Registers, and a few others that are not
 * interesting to schedulers are not allocated SUnits.
 *
 * SDNodes with MVT::Flag operands are grouped along with the flagged
 * nodes into a single SUnit so that they are scheduled together.
 *
 * SDNode-based scheduling graphs do not use SDep::Anti or SDep::Output
 * edges.  Physical register dependence information is not carried in
 * the DAG and must be handled explicitly by schedulers.
 */
public class ScheduleDAGSDNodes extends ScheduleDAG
{
	public SelectionDAG dag;

	public ScheduleDAGSDNodes(MachineFunction mf)
	{
		super(mf);
	}

	void run(SelectionDAG dag, MachineBasicBlock mbb, int insertPos)
	{}

	private static boolean isPassiveNode(SDNode node)
	{
		if (node instanceof SDNode.ConstantSDNode) return true;
		if (node instanceof SDNode.ConstantFPSDNode) return true;
		if (node instanceof SDNode.RegisterSDNode) return true;
		if (node instanceof SDNode.GlobalAddressSDNode) return true;
		if (node instanceof SDNode.BasicBlockSDNode) return true;
		if (node instanceof SDNode.FrameIndexSDNode) return true;
		if (node instanceof SDNode.ConstantPoolSDNode) return true;
		if (node instanceof SDNode.JumpTableSDNode) return true;
		if (node instanceof SDNode.ExternalSymbolSDNode) return true;
		if (node instanceof SDNode.MemOperandSDNode) return true;
		if (node.getOpcode() == ISD.EntryToken) return true;
		return false;
	}

	public SUnit newSUnit(SDNode n)
	{
		SUnit su = new SUnit(n, sunits.size());
		su.originNode = su;
		sunits.add(su);
		return su;
	}

	public SUnit clone(SUnit u)
	{}

	public void buildSchedGraph()
	{}

	public void computeLatency(SUnit su)
	{}	

	public static int countResults(SDNode node)
	{}

	public static int countOperands(SDNode node)
	{}

	public static int computeMemOperandEnd(SDNode node)
	{}

	public void emitNode(SDNode node, boolean isClone, boolean hasClone, 
		TObjectIntHashMap<SDValue> vrBaseMap)
	{}

	public MachineBasicBlock emitSchedule()
	{}

	@Override
	public void schedule()
	{
	}

	public void dumpNode(SUnit su)
	{}

	private void emitSubregNode(SDNode node, 
		TObjectIntHashMap<SDValue> vrBaseMap)
	{}

	private void emitCopyToRegClassNode(SDNode node, 
		TObjectIntHashMap<SDValue> vrBaseMap)
	{}

	/**
	 * Return the virtual register corresponding to the 
	 * specified SDValue.
	 */
	private int getVR(SDValue op, TObjectIntHashMap<SDValue> vrBaseMap)
	{}

	private int getDstOfOnlyCopyToRegUse(SDNode node, int resNo)
	{}

	private void addOperand(MachineInstr mi,
		SDValue op, 
		int iiOpNum, 
		TargetInstrDesc tid,
		TObjectIntHashMap<SDValue> vrBaseMap)
	{}

	private void addRegisterOperand(MachineInstr mi, 
		SDValue op, 
		int iiOpNum, 
		TargetInstrDesc tid, 
		TObjectIntHashMap<SDValue> vrBaseMap)
	{}

	private void emitCopyFromReg(SDNode node, 
		int resNo, 
		boolean isClone, 
		boolean isCloned, 
		int srcReg, 
		TObjectIntHashMap<SDValue> vrBaseMap)
	{}

	private void createVritualRegisters(SDNode node, 
		MachineInstr mi, 
		TargetInstrDesc tid, 
		boolean isClone, 
		boolean isCloned, 
		TObjectIntHashMap<SDValue> vrBaseMap)
	{}

	private void buildSchedUnits()
	{}

	private void addSchedEdges()
	{}
}