package utils.tablegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
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

import backend.codegen.EVT;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;

import static backend.codegen.MVT.Other;
import static backend.codegen.MVT.iPTR;
import static utils.tablegen.CodeGenDAGPatterns.*;
import static utils.tablegen.CodeGenTarget.getValueType;
import static utils.tablegen.EEVT.*;
import static utils.tablegen.SDNodeInfo.SDTypeConstraint.constraintType.*;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class SDNodeInfo
{
    private Record theDef;
    private String enumName;
    private String sdClassName;
    private int properties;
    private int numResults;
    private int numOperands;
    private ArrayList<SDTypeConstraint> typeConstraints;

    public SDNodeInfo(Record r) throws Exception
    {
        theDef = r;
        enumName = r.getValueAsString("Opcode");
        sdClassName = r.getValueAsString("SDClass");
        Record typeProfile = r.getValueAsDef("TypeProfile");
        numResults = (int) typeProfile.getValueAsInt("NumResults");
        numOperands = (int)typeProfile.getValueAsInt("NumOperands");

        properties = 0;
        ArrayList<Record> propList = r.getValueAsListOfDefs("Properties");
        for (Record prop : propList)
        {
            switch (prop.getName())
            {
                case "SDNPCommutative":
                    properties |= 1 << SDNP.SDNPCommutative;
                    break;
                case "SDNPAssociative":
                    properties |= 1 << SDNP.SDNPAssociative;
                    break;
                case "SDNPHasChain":
                    properties |= 1 << SDNP.SDNPHasChain;
                    break;
                case "SDNPOutFlag":
                    properties |= 1 << SDNP.SDNPOutFlag;
                    break;
                case "SDNPInFlag":
                    properties |= 1 << SDNP.SDNPInFlag;
                    break;
                case "SDNPOptInFlag":
                    properties |= 1 << SDNP.SDNPOptInFlag;
                    break;
                case "SDNPMayStore":
                    properties |= 1 << SDNP.SDNPMayStore;
                    break;
                case "SDNPMayLoad":
                    properties |= 1 << SDNP.SDNPMayLoad;
                    break;
                case "SDNPSideEffect":
                    properties |= 1 << SDNP.SDNPSideEffect;
                    break;
                case "SDNPMemOperand":
                    properties |= 1 << SDNP.SDNPMemOperand;
                    break;
                default:
                    System.err.println("Undefined SD Node property '" + prop.getName()
                            + "' on node '" + r.getName() + "'!");
                    System.exit(1);
            }
        }

        // Parse the type constraints.
        typeConstraints = new ArrayList<>();
        ArrayList<Record> constrainsts = typeProfile.getValueAsListOfDefs("Constraints");
        for (Record con : constrainsts) 
        {
            typeConstraints.add(new SDTypeConstraint(con));
        }
    }

    public int getNumResults()
    {
        return numResults;
    }

    public int getNumOperands()
    {
        return numOperands;
    }

    public Record getRecord()
    {
        return theDef;
    }

    public String getEnumName()
    {
        return enumName;
    }

    public String getSDClassName()
    {
        return sdClassName;
    }

    public ArrayList<SDTypeConstraint> getTypeConstraints()
    {
        return typeConstraints;
    }

    /**
     * Return true if this node has the specified property.
     * @param prop
     * @return
     */
    public boolean hasProperty(int prop)
    {
        return (properties & (1 << prop)) != 0;
    }

    /**
     * Given a node in a pattern, apply the type
     * constraints for this node to the operands of the node.  This returns
     * true if it makes a change, false otherwise.  If a type contradiction is
     * found, throw an exception.
     * @param node
     * @param tp
     * @return
     */
    public boolean applyTypeConstraints(TreePatternNode node, TreePattern tp)
            throws Exception
    {
        boolean changed = false;
        for (SDTypeConstraint con : typeConstraints)
        {
            changed |= con.applyTypeConstraint(node, this, tp);
        }
        return changed;
    }

    public static final class SDTypeConstraint
    {
        public SDTypeConstraint(Record r) throws Exception
        {
            operandNo = (int) r.getValueAsInt("OperandNum");

            if (r.isSubClassOf("SDTCisVT")) 
            {
                constraintType = SDTCisVT;
                x = getValueType(r.getValueAsDef("VT"));
            }
            else if (r.isSubClassOf("SDTCisPtrTy"))
            {
                constraintType = SDTCisPtrTy;
            }
            else if (r.isSubClassOf("SDTCisInt"))
            {
                constraintType = SDTCisInt;
            }
            else if (r.isSubClassOf("SDTCisFP"))
            {
                constraintType = SDTCisFP;
            }
            else if (r.isSubClassOf("SDTCisVec"))
            {
                constraintType = SDTCisVec;
            }
            else if (r.isSubClassOf("SDTCisSameAs"))
            {
                constraintType = SDTCisSameAs;
                x = (int) r.getValueAsInt("OtherOperandNum");
            }
            else if (r.isSubClassOf("SDTCisVTSmallerThanOp"))
            {
                constraintType = SDTCisVTSmallerThanOp;
                x = (int) r.getValueAsInt("OtherOperandNum");
            }
            else if (r.isSubClassOf("SDTCisOpSmallerThanOp"))
            {
                constraintType = SDTCisOpSmallerThanOp;
                x = (int) r.getValueAsInt("BigOperandNum");
            }
            else if (r.isSubClassOf("SDTCisEltOfVec"))
            {
                constraintType = SDTCisEltOfVec;
                x = (int) r.getValueAsInt("OtherOpNum");
            }
            else
            {
                System.err.println("Unrecognized SDTypeConstraint '" + r.getName() +"'!");
                System.exit(1);
            }
        }

        public int operandNo;

        public enum constraintType
        {
            SDTCisVT, SDTCisPtrTy, SDTCisInt, SDTCisFP, SDTCisVec, SDTCisSameAs,
            SDTCisVTSmallerThanOp, SDTCisOpSmallerThanOp, SDTCisEltOfVec
        }
        public constraintType constraintType;

        /**
         * For represents the SDTCisVT_info, SDTCisSameAs_Info,
         * SDTCisVTSmallerThanOp_Info, SDTCisOpSmallerThanOp_Info,
         * SDTCisEltOfVec_Info.
         */
        public int x;

        /**
         * Given a node in a pattern, apply this type
         * constraint to the nodes operands.  This returns true if it makes a
         * change, false otherwise.  If a type contradiction is found, throw an
         * exception.
         * @param node
         * @param nodeInfo
         * @param pattern
         * @return
         */
        public boolean applyTypeConstraint(TreePatternNode node, SDNodeInfo nodeInfo,
                TreePattern pattern) throws Exception
        {
            int numResults = nodeInfo.getNumResults();
            assert  (numResults <= 1):"We only work which nodes with zero or result so far!";

            if (nodeInfo.getNumOperands() >= 0)
            {
                if (node.getNumChildren() != nodeInfo.getNumOperands())
                    pattern.error(node.getOperator().getName() + " node requires exactly "
                        + nodeInfo.getNumOperands() + " operands");
            }

            CodeGenTarget cgt = pattern.getDAGPatterns().getTarget();

            TreePatternNode nodeToApply = getOperandNum(operandNo, node, numResults);

            switch (constraintType)
            {
                default: assert false:"Unknown constraint yet!";
                case SDTCisVT:
                    return nodeToApply.updateNodeType(x, pattern);
                case SDTCisPtrTy:
                    return nodeToApply.updateNodeType(iPTR, pattern);
                case SDTCisInt:
                {
                    TIntArrayList intVTs = filterVTs(cgt.getLegalValueTypes(), isInteger);

                    if (intVTs.size() == 1)
                        return nodeToApply.updateNodeType(intVTs.get(0), pattern);
                    return nodeToApply.updateNodeType(isInt, pattern);
                }
                case SDTCisFP:
                {
                    TIntArrayList intVTs = filterVTs(cgt.getLegalValueTypes(), isFloatingPoint);

                    if (intVTs.size() == 1)
                        return nodeToApply.updateNodeType(intVTs.get(0), pattern);
                    return nodeToApply.updateNodeType(isFP, pattern);
                }
                case SDTCisVec:
                {

                    TIntArrayList intVTs = filterVTs(cgt.getLegalValueTypes(), isVector);

                    if (intVTs.size() == 1)
                        return nodeToApply.updateNodeType(intVTs.get(0), pattern);
                    return nodeToApply.updateNodeType(EEVT.isVec, pattern);
                }
                case SDTCisSameAs:
                {
                    TreePatternNode otherNode = getOperandNum(x, node, numResults);
                    return nodeToApply.updateNodeType(otherNode.getExtTypes(), pattern)
                            | otherNode.updateNodeType(nodeToApply.getExtTypes(), pattern);
                }
                case SDTCisVTSmallerThanOp:
                {
                    if (!nodeToApply.isLeaf() || !(nodeToApply.getLeafValue() instanceof Init.DefInit)
                        || !((Init.DefInit)nodeToApply.getLeafValue()).getDef().isSubClassOf("ValueType"))
                    {
                        pattern.error(node.getOperator().getName() + " expects a VT operand!");;
                    }

                    int vt = getValueType(((Init.DefInit)nodeToApply.getLeafValue()).getDef());
                    if (!isInteger.test(vt))
                        pattern.error(node.getOperator().getName() + " VT operand must be integera!");;

                    TreePatternNode otherNode = getOperandNum(x, node, numResults);

                    boolean changed = false;
                    changed |= otherNode.updateNodeType(isInt, pattern);

                    assert otherNode.getExtTypes().size() == 1:"Node has too many types!";
                    if (otherNode.hasTypeSet() && otherNode.getTypeNum(0) <= vt)
                        otherNode.updateNodeType(Other, pattern);

                    return false;
                }
                case SDTCisOpSmallerThanOp:
                {
                    TreePatternNode bigOperand = getOperandNum(x, node,
                            numResults);

                    boolean changed = false;
                    assert !(EEVT.isExtIntegerInVTs(nodeToApply.getExtTypes()) &&
                            EEVT.isExtFloatingPointInVTs(nodeToApply.getExtTypes())) &&
                            !((EEVT.isExtIntegerInVTs(bigOperand.getExtTypes()))
                            && EEVT.isExtFloatingPointInVTs(bigOperand.getExtTypes())) :
                            "SDTCisOpSmallerThanOp does not handle mixed int/fp types!";

                    if (isExtIntegerInVTs(nodeToApply.getExtTypes()))
                        changed |= bigOperand.updateNodeType(isInt, pattern);
                    else if (isExtFloatingPointInVTs(nodeToApply.getExtTypes()))
                        changed |= bigOperand.updateNodeType(isFP, pattern);
                    if (isExtIntegerInVTs(bigOperand.getExtTypes()))
                        changed |= nodeToApply.updateNodeType(isInt, pattern);
                    else if (isExtFloatingPointInVTs(bigOperand.getExtTypes()))
                        changed |= nodeToApply.updateNodeType(isFP, pattern);

                    TIntArrayList vts = cgt.getLegalValueTypes();
                    if (isExtIntegerInVTs(nodeToApply.getExtTypes()))
                        vts = filterVTs(vts, isInteger);
                    else if (isExtFloatingPointInVTs(nodeToApply.getExtTypes()))
                        vts = filterVTs(vts, isFloatingPoint);
                    else
                        vts.clear();

                    switch (vts.size())
                    {
                        default:
                        case 0: break;
                        case 1:
                            return nodeToApply.updateNodeType(Other, pattern);
                        case 2:
                            assert vts.get(0) < vts.get(1) :"Should be sorted";
                            changed |= nodeToApply.updateNodeType(vts.get(0), pattern);
                            changed |= bigOperand.updateNodeType(vts.get(1), pattern);
                            break;

                    }
                    return changed;
                }
                case SDTCisEltOfVec:
                {
                    TreePatternNode otherOperand = getOperandNum(x, node, numResults);
                    if (otherOperand.hasTypeSet())
                    {
                        if (!isVector.test(otherOperand.getTypeNum(0)))
                        {
                            pattern.error(node.getOperator().getName() + "VT operand must be a vector!");;
                        }
                        EVT ivt = new EVT(otherOperand.getTypeNum(0));
                        ivt = ivt.getVectorElementType();
                        return nodeToApply.updateNodeType(ivt.getSimpleVT().simpleVT, pattern);
                    }
                    return false;
                }
            }
        }

        /**
         * Return the node corresponding to operand {@code opNo} in tree
         * @{code node}, which has {@code numResults} results.
         * @param opNo
         * @param node
         * @param numResults
         * @return
         */
        public TreePatternNode getOperandNum(int opNo, TreePatternNode node, int numResults)
        {
            assert numResults <= 1:"We only work with nodes with zero or one result so far!";
            if (opNo >= (numResults  + node.getNumChildren()))
            {
                System.err.printf("Invalid operand number %d ", opNo);
                node.dump();
                System.err.println();
                System.exit(1);
            }

            if (opNo < numResults)
                return node;
            else
                return node.getChild(opNo - numResults);
        }
    }
}
