package utils.tablegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

import tools.Util;
import utils.tablegen.CodeGenIntrinsic.ModRefType;
import utils.tablegen.Init.BitsInit;
import utils.tablegen.Init.DagInit;
import utils.tablegen.Init.DefInit;
import utils.tablegen.Init.IntInit;
import utils.tablegen.RecTy.IntRecTy;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Objects;

import static backend.codegen.MVT.isVoid;
import static utils.tablegen.CodeGenTarget.getValueType;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public final class TreePattern
{
    private ArrayList<TreePatternNode> trees = new ArrayList<>();

    private Record theRecord;

    private ArrayList<String> args = new ArrayList<>();

    private CodeGenDAGPatterns cdp;

    private boolean isInputPattern;

    public TreePattern(Record theRec, Init.ListInit rawPat, boolean isInput,
            CodeGenDAGPatterns cdp) throws Exception
    {
        theRecord = theRec;
        this.cdp = cdp;
        isInputPattern = isInput;
        for (int i = 0, e = rawPat.getSize(); i != e; i++)
            trees.add(parseTreePattern((DagInit)rawPat.getElement(i)));
    }

    public TreePattern(Record theRec, DagInit pat, boolean isInput,
            CodeGenDAGPatterns cdp) throws Exception
    {
        theRecord = theRec;
        this.cdp = cdp;
        isInputPattern = isInput;
        trees.add(parseTreePattern(pat));
    }

    public TreePattern(Record theRec, TreePatternNode pat, boolean isInput,
            CodeGenDAGPatterns cdp)
    {
        theRecord = theRec;
        this.cdp = cdp;
        isInputPattern = isInput;
        trees.add(pat);
    }

    public ArrayList<TreePatternNode> getTrees()
    {
        return trees;
    }

    public int getNumTrees()
    {
        return trees.size();
    }

    public TreePatternNode getTree(int index)
    {
        return trees.get(index);
    }

    public TreePatternNode getOnlyTree()
    {
        Util.assertion(trees.size() == 1, "Doesn't have exactly one pattern!");
        return trees.get(0);
    }

    public Record getRecord()
    {
        return theRecord;
    }

    public int getNumArgs()
    {
        return args.size();
    }

    public String getArgName(int idx)
    {
        Util.assertion( idx >= 0 && idx < args.size());
        return args.get(idx);
    }

    public ArrayList<String> getArgList()
    {
        return args;
    }

    public CodeGenDAGPatterns getDAGPatterns()
    {
        return cdp;
    }

    public void inlinePatternFragments() throws Exception
    {
        for (int i = 0, e = trees.size(); i != e; i++)
            trees.set(i, trees.get(i).inlinePatternFragments(this));
    }

    /**
     * Infer/propagate as many types throughout the expression
     * patterns as possible.
     * @return Return true if all types are inferred, false
     * otherwise.  Throw an exception if a type contradiction is found.
     */
    public boolean inferAllTypes() throws Exception
    {
        boolean changed = true;
        while (changed)
        {
            changed = false;
            for (TreePatternNode node : trees)
                changed |= node.applyTypeConstraints(this, false);
        }
        boolean hasUnresolvedTypes = false;
        for (TreePatternNode node : trees)
            hasUnresolvedTypes |= node.containsUnresolvedType();

        return !hasUnresolvedTypes;
    }

    public void error(String msg) throws Exception
    {
        dump();
        throw new Exception("In " + theRecord.getName() + ": " + msg);
    }

    public void print(PrintStream os)
    {
        os.printf(getRecord().getName());
        if (!args.isEmpty())
        {
            os.printf("(%s", args.get(0));
            for (int i = 1; i != args.size(); i++)
                os.printf(", %s", args.get(i));

            os.printf(")");
        }
        os.printf(":");

        if (trees.size() > 1)
        {
            os.printf("[\n");
        }
        for (int i = 0, e = trees.size(); i != e; i++)
        {
            os.printf("\t");
            trees.get(i).print(os);
            os.println();
        }

        if (trees.size() > 1)
            os.println();
    }

    public void dump()
    {
        print(System.err);
    }

    private TreePatternNode parseTreePattern(DagInit dag) throws Exception
    {
        if (!(dag.getOperator() instanceof DefInit))
            error("Pattern has unexpected operator type!");

        DefInit opDef = (DefInit)dag.getOperator();

        Record operator = opDef.getDef();
        if (operator.isSubClassOf("ValueType"))
        {
            // If the operator is a ValueType, then this must be "type cast" of a leaf
            // node.
            if (dag.getNumArgs() != 1)
                error("Type cast only takes one operand!");

            Init arg = dag.getArg(0);
            TreePatternNode newNode;
            DefInit di = arg instanceof DefInit ? (DefInit)arg : null;
            if (di != null)
            {
                Record r = di.getDef();
                if (r.isSubClassOf("SDNode") || r.isSubClassOf("PatFrag"))
                {
                    dag.setArg(0, new DagInit(di, "", new ArrayList<>()));
                    return parseTreePattern(dag);
                }
                newNode = new TreePatternNode(di);
            }
            else if (arg instanceof DagInit)
            {
                DagInit dagI = (DagInit)arg;
                newNode = parseTreePattern(dagI);
            }
            else if (arg instanceof IntInit)
            {
                IntInit ii = (IntInit)arg;
                newNode = new TreePatternNode(ii);
                if (!dag.getArgName(0).isEmpty())
                    error("Constant int argument should not have a name!");
            }
            else if (arg instanceof BitsInit)
            {
                BitsInit bi = (BitsInit)arg;
                Init ii = bi.convertInitializerTo(new IntRecTy());
                if (ii == null || !(ii instanceof IntInit))
                    error("Bits value must be constants!");

                newNode = new TreePatternNode(ii);
                if (!dag.getArgName(0).isEmpty())
                    error("Constant int argument should not have a name!");
            }
            else
            {
                arg.dump();
                error("Unknown leaf value for tree pattern!");
                return null;
            }

            newNode.updateNodeType(getValueType(operator), this);
            if (newNode.getNumChildren() == 0)
                newNode.setName(dag.getArgName(0));

            return newNode;
        }

        // Verify that this is something that makes sense for an operator.
        if (!operator.isSubClassOf("PatFrag") &&
                !operator.isSubClassOf("SDNode") &&
                !operator.isSubClassOf("Instruction") &&
                !operator.isSubClassOf("SDNodeXForm") &&
                !operator.isSubClassOf("Intrinsic") &&
                !Objects.equals(operator.getName(), "set") &&
                !Objects.equals(operator.getName(), "implicit") &&
                !Objects.equals(operator.getName(), "parallel"))
        {
            error("Unrecognized node '" + operator.getName() + "'!");
        }

        if (isInputPattern && (operator.isSubClassOf("Instruction")
                || operator.isSubClassOf("SDNodeXForm")))
            error("Cannot use '" + operator.getName() + "' in an input pattern!");

        ArrayList<TreePatternNode> children = new ArrayList<>();

        for (int i = 0, e = dag.getNumArgs(); i != e; i++)
        {
            Init arg = dag.getArg(i);
            DagInit di = arg instanceof DagInit ? (DagInit)arg : null;
            if (di != null)
            {
                children.add(parseTreePattern(di));
                TreePatternNode lastNode = children.get(children.size() - 1);
                if (lastNode.getName().isEmpty())
                    lastNode.setName(dag.getArgName(i));
            }
            else if (arg instanceof DefInit)
            {
                DefInit def = (DefInit)arg;
                Record r = def.getDef();

                if (r.isSubClassOf("SDNode") || r.isSubClassOf("PatFrag"))
                {
                    dag.setArg(i, new DagInit(def, "", new ArrayList<>()));
                    --i;
                }
                else
                {
                    TreePatternNode node = new TreePatternNode(def);
                    node.setName(dag.getArgName(i));
                    children.add(node);

                    if (r.getName().equals("node"))
                    {
                        if (dag.getArgName(i).isEmpty())
                            error("'node' argument requires a name to match with operand list");
                        args.add(dag.getArgName(i));
                    }
                }
            }
            else if (arg instanceof IntInit)
            {
                IntInit ii = (IntInit)arg;
                TreePatternNode node = new TreePatternNode(ii);
                if (!dag.getArgName(i).isEmpty())
                    error("Constant int argument should not have a name!");

                children.add(node);
            }
            else if (arg instanceof BitsInit)
            {
                BitsInit bi = (BitsInit)arg;
                Init ii = bi.convertInitializerTo(new IntRecTy());
                if (ii == null || !(ii instanceof IntInit))
                    error("Bits value must be constants!");

                TreePatternNode node = new TreePatternNode(ii);
                if(!dag.getArgName(i).isEmpty())
                    error("Constant int argument should not have a name!");
                children.add(node);
            }
            else
            {
                System.err.print('"');
                arg.dump();
                System.err.print("\": ");
                error("Undefined leaf value for tree pattern!");
            }
        }

        if (operator.isSubClassOf("Intrinsic"))
        {
            CodeGenIntrinsic instrincis = getDAGPatterns().getIntrinsic(operator);
            int iid = getDAGPatterns().getIntrinsicID(operator) + 1;

            if (instrincis.is.retVTs.get(0) == isVoid)
                operator = getDAGPatterns().getIntrinsicVoidSDNode();
            else if (instrincis.modRef != ModRefType.NoMem)
                operator = getDAGPatterns().getIntrinsicWChainSDNode();
            else
            {
                // Otherwise, no chain.
                operator = getDAGPatterns().getIntrinsicWOChainSDNode();
            }
            TreePatternNode iidNode = new TreePatternNode(new IntInit(iid));
            children.add(0, iidNode);
        }

        TreePatternNode result = new TreePatternNode(operator, children);
        result.setName(dag.getName());

        return result;
    }
}
