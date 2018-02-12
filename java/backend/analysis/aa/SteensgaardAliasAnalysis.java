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

package backend.analysis.aa;

import backend.ir.AllocationInst;
import backend.pass.ModulePass;
import backend.support.CallSite;
import backend.utils.InstVisitor;
import backend.value.*;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;

/**
 * This file defines a class named "SteensGaardAliasAnalysis" in terms of several
 * papers as follows.
 * <ol>
 *  <li>"Points-to analysis in almost linear time."</li>
 *  <li>Lin, Sheng-Hsiu. Alias Analysis in LLVM. Lehigh University, 2015.</li>
 * </ol>
 * This is a trivial implementation about Steensgaard's paper. I would not to
 * performance some minor optimization, but I would to do in the future.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class SteensgaardAliasAnalysis extends AliasAnalysis implements
        ModulePass, InstVisitor<Void>
{
    /**
     * This class used for representing a node in constraint graph.
     * All nodes consists of a constraint graph.
     */
    private static class Node
    {
        /**
         * This points to it's representative node.
         * I uses a union-find set to operate on constraint.
         */
        int id;
        Node rep;
        Node pointsTo;
        Value value;

        Node(int id , Value val)
        {
            value = val;
            rep = this;
        }

        Node getRepresentativeNode()
        {
            if (rep == this) return this;
            return rep = rep.getRepresentativeNode();
        }

        boolean isRepresentative()
        {
            return rep == this;
        }

        void setRepresentative(Node rep)
        {
            if (this.rep != null)
                this.rep.setRepresentative(rep);
            this.rep = rep;
        }
    }

    /**
     * A maps from value to an integer.
     */
    private TObjectIntHashMap<Value> valueNodes;
    /**
     * A maps from memory object to an integer.
     * Note that We should discriminate the {@linkplain #valueNodes} and this.
     * The {@linkplain #valueNodes} is used for tracking unique id for any LLVM
     * value, for example, global variable, function formal parameter or local
     * variable. But this is only used for recording an unique number of memory
     * object, like global variable, any value allocated by AllocaInst or
     * MallocInst, that can be addressed(in other word, it could be taken address
     * by "&" operator in C-like).
     */
    private TObjectIntHashMap<Value> pointerNodes;
    /**
     * This map used for recording the unique number for function's return value.
     */
    private TObjectIntHashMap<Function> returnNodes;
    /**
     * This map used for recording the unique number for function's vararg value.
     */
    private TObjectIntHashMap<Function> varargNodes;

    private Node[] nodes;
    private Module m;

    SteensgaardAliasAnalysis()
    {
        valueNodes = new TObjectIntHashMap<>();
        pointerNodes = new TObjectIntHashMap<>();
        returnNodes = new TObjectIntHashMap<>();
        varargNodes = new TObjectIntHashMap<>();
    }

    /**
     * This method couldn't change the control flow graph of being analyzed LLVM
     * IR module. It will performs three steps to transform Alias problem into
     * a constraint-based method, and achieving the alias information by quering
     * those constraints information.
     * @param m
     * @return
     */
    @Override
    public boolean runOnModule(Module m)
    {
        this.m = m;
        identifyObjects();
        collectConstraints();
        return false;
    }

    private static final int NullObject = 0;
    private static final int NullPtr = 1;
    private static final int Universal = 2;
    private static final int NumSpecialValue = Universal + 1;

    private void identifyObjects()
    {
        int numObjects;
        numObjects = NumSpecialValue;
        // Handle Global variables.
        for(GlobalVariable gv : m.getGlobalVariableList())
        {
            valueNodes.put(gv, numObjects++);
            pointerNodes.put(gv, numObjects++);
        }

        // handle Functions.
        for (Function fn: m.getFunctionList())
        {
            // Because function can be treated as the target of function pointer,
            // so we need to keep track of the value id of function.
            valueNodes.put(fn, numObjects++);
            if (fn.getReturnType().isPointerType())
                returnNodes.put(fn, numObjects++);
            if (fn.getFunctionType().isVarArg())
                varargNodes.put(fn, numObjects++);

            // Walk through Function body in a order that doesn't care execution path
            // of program.
            for (BasicBlock bb : fn)
            {
                assert !bb.isEmpty():"Reaching here, there should not have any empty block!";
                for (Instruction inst : bb)
                {
                    if (!inst.getType().isPointerType()) continue;
                    // We just care about those instruction of type pointer.
                    valueNodes.put(inst, numObjects++);
                    if (inst instanceof AllocationInst)
                        pointerNodes.put(inst, numObjects++);
                }
            }
        }
        nodes = new Node[numObjects];
    }

    private Node getValueNode(Value val)
    {
        assert val != null && valueNodes.containsKey(val);
        int id = valueNodes.get(val);
        if (nodes[id] != null) return nodes[id];
        return nodes[id] = new Node(id, val);
    }

    private Node getNullPointerNode()
    {
        if (nodes[NullPtr] != null)
            return nodes[NullPtr];
        return nodes[NullPtr] = new Node(NullPtr, null);
    }

    private Node getNullObjecteNode()
    {
        if (nodes[NullObject] != null)
            return nodes[NullObject];
        return nodes[NullObject] = new Node(NullObject,null);
    }
    private Node getPointerNode(Value val)
    {
        assert val != null && pointerNodes.containsKey(val);
        int id = pointerNodes.get(val);
        if (nodes[id] != null) return nodes[id];
        return nodes[id] = new Node(id, val);
    }

    private Node getUniversalValueNode()
    {
        if (nodes[Universal] != null)
            return nodes[Universal];
        return nodes[Universal] = new Node(Universal, null);
    }

    private Node getValueNodeOfConstant(Constant c)
    {
        if (c instanceof ConstantPointerNull)
            return getNullPointerNode();
        else if (c instanceof GlobalVariable)
        {
            getValueNode(c);
        }
        else if (c instanceof ConstantExpr)
        {
            ConstantExpr ce = (ConstantExpr)c;
            switch (ce.getOpcode())
            {
                case GetElementPtr:
                    return getValueNodeOfConstant(ce.operand(0));
                case BitCast:
                    return getValueNodeOfConstant(ce.operand(0));
                case IntToPtr:
                    return getUniversalValueNode();
            }
        }
        assert false:"Unknown constant node!";
        return null;
    }

    private void addConstraintsOnGlobalVariable(Node dest, Constant c)
    {
        if (c.getType().isSingleValueType())
            dest.setRepresentative(getValueNodeOfConstant(c));
        else if (c.isNullValue())
            dest.setRepresentative(getNullObjecteNode());
        else if (!(c instanceof Value.UndefValue))
        {
            assert c instanceof ConstantArray || c instanceof ConstantStruct;
            for (int i = 0, e = c.getNumOfOperands(); i < e; i++)
                addConstraintsOnGlobalVariable(dest, c.operand(i));
        }
    }

    private void collectConstraints()
    {
        for (GlobalVariable gv : m.getGlobalVariableList())
        {
            // LLVM IR "@x = global i32 1, align 4" could be abstracted into following
            // constraint.
            Node ptrNode = getPointerNode(gv);
            getValueNode(gv).setRepresentative(ptrNode);
            if (gv.getInitializer() != null)
                addConstraintsOnGlobalVariable(ptrNode, gv.getInitializer());
        }
        // Walk through each function to collect constraints on each LLVM instruction.
        for (Function f : m.getFunctionList())
            visit(f);
    }

    @Override
    public AliasResult alias(Value ptr1, int size1, Value ptr2, int size2)
    {
        return null;
    }

    @Override
    public void getMustAliases(Value ptr, ArrayList<Value> retVals)
    {

    }

    @Override
    public boolean pointsToConstantMemory(Value ptr)
    {
        return false;
    }

    @Override
    public ModRefResult getModRefInfo(CallSite cs1, CallSite cs2)
    {
        return null;
    }

    @Override
    public boolean hasNoModRefInfoForCalls()
    {
        return false;
    }

    @Override
    public void deleteValue(Value val)
    {

    }

    @Override
    public void copyValue(Value from, Value to)
    {

    }

    @Override
    public String getPassName()
    {
        return "Steensgaard's style alias analysis";
    }

    @Override
    public Void visitRet(Instruction.ReturnInst inst)
    {
        return null;
    }

    @Override
    public Void visitBr(Instruction.BranchInst inst)
    {
        return null;
    }

    @Override
    public Void visitSwitch(Instruction.SwitchInst inst)
    {
        return null;
    }

    @Override
    public Void visitICmp(Instruction.ICmpInst inst)
    {
        return null;
    }

    @Override
    public Void visitFCmp(Instruction.FCmpInst inst)
    {
        return null;
    }

    @Override
    public Void visitCastInst(Instruction.CastInst inst)
    {
        return null;
    }

    @Override
    public Void visitAlloca(Instruction.AllocaInst inst)
    {
        return null;
    }

    @Override
    public Void visitLoad(Instruction.LoadInst inst)
    {
        return null;
    }

    @Override
    public Void visitStore(Instruction.StoreInst inst)
    {
        return null;
    }

    @Override
    public Void visitCall(Instruction.CallInst inst)
    {
        return null;
    }

    @Override
    public Void visitGetElementPtr(Instruction.GetElementPtrInst inst)
    {
        return null;
    }

    @Override
    public Void visitPhiNode(Instruction.PhiNode inst)
    {
        return null;
    }
}
