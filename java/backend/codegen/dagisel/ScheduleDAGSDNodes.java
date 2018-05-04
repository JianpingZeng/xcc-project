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
import backend.codegen.dagisel.SDNode.MemOperandSDNode;
import backend.codegen.fastISel.ISD;
import backend.target.TargetInstrDesc;
import backend.target.TargetInstrInfo;
import backend.target.TargetRegisterInfo;
import backend.target.TargetSubtarget;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.Util;

import java.util.LinkedList;
import java.util.Objects;

import static backend.codegen.dagisel.SDep.Kind.Data;
import static backend.codegen.dagisel.SDep.Kind.Order;
import static backend.target.TargetOperandInfo.OperandConstraint.TIED_TO;

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
public abstract class ScheduleDAGSDNodes extends ScheduleDAG
{
	public SelectionDAG dag;

	public ScheduleDAGSDNodes(MachineFunction mf)
	{
		super(mf);
	}

	public void run(SelectionDAG dag, MachineBasicBlock mbb, int insertPos)
	{
		this.dag = dag;
		super.run(mbb, insertPos);
	}

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
		if (node instanceof MemOperandSDNode) return true;
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
	{
		SUnit su = new SUnit(u.getNode(), -1);
		su.originNode = u.originNode;
		su.latency = u.latency;
		su.isTwoAddress = u.isTwoAddress;
		su.isCommutable = u.isCommutable;
		su.hasPhysRegDefs = u.hasPhysRegDefs;
		su.isCloned = true;
		return su;
	}

	public void buildSchedGraph()
	{
		buildSchedUnits();
		addSchedEdges();
	}

	public void computeLatency(SUnit su)
	{
		Util.shouldNotReachHere("Should not reach here!");
	}	

	public static int countResults(SDNode node)
	{
		int n = node.getNumValues();
		while (n != 0 && node.getValueType(n-1).getSimpleVT().simpleVT == MVT.Flag)
			--n;
		if (n > 0 && node.getValueType(n-1).getSimpleVT().simpleVT == MVT.Other)
			--n;
		return n;
	}

	public static int countOperands(SDNode node)
	{
		int n = computeMemOperandEnd(node);
		while (n > 0 && node.getOperand(n-1).getNode() instanceof MemOperandSDNode)
			--n;
		return n;
	}

	public static int computeMemOperandEnd(SDNode node)
	{
		int n = node.getNumOperands();
		while (n != 0 && node.getValueType(n-1).getSimpleVT().simpleVT == MVT.Flag)
			--n;
		if (n > 0 && node.getValueType(n-1).getSimpleVT().simpleVT == MVT.Other)
			--n;
		return n;
	}

	public void emitNode(SDNode node, boolean isClone, boolean hasClone, 
		TObjectIntHashMap<SDValue> vrBaseMap)
	{}

	public MachineBasicBlock emitSchedule()
	{
		TObjectIntHashMap<SDValue> vrBaseMap = new TObjectIntHashMap<>();
		TObjectIntHashMap<SUnit> copyVRBaseMap = new TObjectIntHashMap<>();
		for (SUnit su : sequence)
		{
			if (su == null)
			{
				emitNoop();
				continue;
			}

			if (su.getNode() == null)
			{
				emitPhysRegCopy(su, copyVRBaseMap);
				continue;
			}

			LinkedList<SDNode> flaggedNodes = new LinkedList<>();
			for (SDNode flag = su.getNode().getFlaggedNode(); flag != null; 
				flag = flag.getFlaggedNode())
				flaggedNodes.add(flag);

			while (!flaggedNodes.isEmpty())
			{
				SDNode sn = flaggedNodes.removeLast();
				emitNode(sn, !Objects.equals(su.originNode, su), su.isCloned, vrBaseMap);
			}
			emitNode(su.getNode(), !Objects.equals(su.originNode, su), su.isCloned, vrBaseMap);
		}
		return mbb;
	}

	@Override
	public abstract void schedule();


	public void dumpNode(SUnit su)
	{
		if (su.getNode() == null)
		{
			System.err.println("PHYS REG COPY!");
			return;
		}

		su.getNode().dump(dag);
		System.err.println();
		LinkedList<SDNode> flaggedNodes = new LinkedList<>();
		for (SDNode n = su.getNode().getFlaggedNode(); n != null; n = n.getFlaggedNode())
		{
			flaggedNodes.add(n);
		}
		while (!flaggedNodes.isEmpty())
		{
			System.err.print("    ");
			SDNode n = flaggedNodes.removeLast();
			n.dump(dag);
			System.err.println();			
		}
	}

	public String getGraphNodeLabel(SUnit su)
	{
		// TODO
		return "";
	}

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
	{
		return 0;
	}

	private int getDstOfOnlyCopyToRegUse(SDNode node, int resNo)
	{
		return 0;
	}

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
	{
		int numNodes = 0;
		for (SDNode node : dag.allNodes)
		{
			node.setNodeID(-1);
			++numNodes;
		}

		boolean unitLatencies = forceUnitLatencies();

		for (SDNode node : dag.allNodes)
		{
			if (isPassiveNode(node))
				continue;

			if (node.getNodeID() != -1)
				continue;
			SUnit nodeSUnit = newSUnit(node);

			SDNode n = node;
			while(n.getNumOperands() != 0 && 
				n.getOperand(n.getNumOperands()-1).getValueType().getSimpleVT()
				.simpleVT == MVT.Flag)
			{
				n = n.getOperand(n.getNumOperands()-1).getNode();
				assert n.getNodeID() == -1;
				n.setNodeID(nodeSUnit.nodeNum);
			}

			n = node;
			while (n.getValueType(n.getNumValues()-1).getSimpleVT().simpleVT == MVT.Flag)
			{
				SDValue flagVal = new SDValue(n, n.getNumValues()-1);
				boolean hasFlagUse = false;
				for (SDUse u : n.useList)
				{
					if (flagVal.isOperandOf(u.getUser()))
					{
						hasFlagUse = true;
						assert n.getNodeID() == -1;
						n.setNodeID(nodeSUnit.nodeNum);
						n = u.getUser();
						break;
					}				
				}
				if (!hasFlagUse) break;
			}
			nodeSUnit.setNode(n);
			assert n.getNodeID() == -1;
			n.setNodeID(nodeSUnit.nodeNum);

			if (unitLatencies)
				nodeSUnit.latency = 1;
			else 
				computeLatency(nodeSUnit);
		}
	}

	private void addSchedEdges()
	{
		TargetSubtarget ts = tm.getSubtarget();
		boolean unitLatencies = forceUnitLatencies();

		for (int i = 0, e = sunits.size(); i < e; i++)
		{
			SUnit su = sunits.get(i);
			SDNode node = su.getNode();

			if (node.isMachineOpecode())
			{
				int opc = node.getMachineOpcode();
				TargetInstrDesc tid = tii.get(opc);
				for (int j = 0; j < tid.getNumOperands(); j++)
				{
					if (tid.getOperandConstraint(j, TIED_TO) != -1)
					{
						su.isTwoAddress = true;
						break;
					}
				}
				if (tid.isCommutable())
					su.isCommutable = true;
			}
			for (SDNode n = su.getNode(); n != null; n = n.getFlaggedNode())
			{
				if (n.isMachineOpecode() && 
					tii.get(n.getMachineOpcode()).getImplicitDefs() != null)
				{
					su.hasPhysRegClobbers = true;
					int numUsed = countResults(n);
					while(numUsed != 0 && !n.hasAnyUseOfValue(numUsed-1))
						--numUsed;
					if (numUsed > tii.get(n.getMachineOpcode()).getNumDefs())
						su.hasPhysRegDefs = true;
				}

				for (int j = 0, sz = n.getNumOperands(); j < sz; j++)
				{
					SDNode opN = n.getOperand(j).getNode();
					if (isPassiveNode(opN)) continue;
					SUnit opSU = sunits.get(opN.getNodeID());
					assert opSU != null;
					if (Objects.equals(opSU, su)) continue;

					EVT opVT = n.getOperand(j).getValueType();
					assert !opVT.equals(new EVT(MVT.Flag));
					boolean isChain = opVT.equals(new EVT(MVT.Other));			

					int[] res = checkForPhysRegDependency(opN, n, i, tri, tii);
					int physReg = res[0];
					int cost = res[1];

					assert physReg == 0 || !isChain;
					if (cost >= 0)
						physReg = 0;

					SDep dep = new SDep(opSU, isChain ? Order:Data, 
						opSU.latency, physReg, false, false, false);
					if (!isChain && !unitLatencies)
					{
						computeOperandLatency(opSU, su, dep);
						ts.adjustSchedDependency(opSU, su, dep);
					}

					su.addPred(dep);
				}
			}
		}
	}

	static int[] checkForPhysRegDependency(SDNode opN, SDNode n, int i, TargetRegisterInfo tri, TargetInstrInfo tii)
	{
		return new int[0];
	}
}