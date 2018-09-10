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

import backend.codegen.MVT;
import gnu.trove.iterator.TIntObjectIterator;
import tools.Error;
import tools.Util;
import utils.tablegen.Init.DefInit;

import java.util.ArrayList;

import static utils.tablegen.SDNodeInfo.SDTypeConstraint.constraintType.*;
import static utils.tablegen.ValueTypeByHwMode.getValueTypeByHwMode;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public final class SDNodeInfo {
  private Record theDef;
  private String enumName;
  private String sdClassName;
  private int properties;
  private int numResults;
  private int numOperands;
  private ArrayList<SDTypeConstraint> typeConstraints;
  private CodeGenHwModes hwModes;

  public SDNodeInfo(Record r, CodeGenHwModes hwModes) {
    theDef = r;
    enumName = r.getValueAsString("Opcode");
    sdClassName = r.getValueAsString("SDClass");
    Record typeProfile = r.getValueAsDef("TypeProfile");
    numResults = (int) typeProfile.getValueAsInt("NumResults");
    numOperands = (int) typeProfile.getValueAsInt("NumOperands");
    this.hwModes = hwModes;

    properties = 0;
    ArrayList<Record> propList = r.getValueAsListOfDefs("Properties");
    for (Record prop : propList) {
      switch (prop.getName()) {
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
          Error.printError("Undefined SD Node property '" + prop.getName()
              + "' on node '" + r.getName() + "'!");
          System.exit(1);
      }
    }

    // Parse the type constraints.
    typeConstraints = new ArrayList<>();
    ArrayList<Record> constrainsts = typeProfile.getValueAsListOfDefs("Constraints");
    for (Record con : constrainsts) {
      typeConstraints.add(new SDTypeConstraint(con, hwModes));
    }
  }

  public int getNumResults() {
    return numResults;
  }

  public int getNumOperands() {
    return numOperands;
  }

  public Record getRecord() {
    return theDef;
  }

  public String getEnumName() {
    return enumName;
  }

  public String getSDClassName() {
    return sdClassName;
  }

  public ArrayList<SDTypeConstraint> getTypeConstraints() {
    return typeConstraints;
  }

  /**
   * Return true if this node has the specified property.
   *
   * @param prop
   * @return
   */
  public boolean hasProperty(int prop) {
    return (properties & (1 << prop)) != 0;
  }

  /**
   * Given a node in a pattern, apply the type
   * constraints for this node to the operands of the node.  This returns
   * true if it makes a change, false otherwise.  If a type contradiction is
   * found, throw an exception.
   *
   * @param node
   * @param tp
   * @return
   */
  public boolean applyTypeConstraints(TreePatternNode node, TreePattern tp)
      {
    boolean changed = false;
    for (SDTypeConstraint con : typeConstraints) {
      changed |= con.applyTypeConstraint(node, this, tp);
    }
    return changed;
  }

  public static final class SDTypeConstraint {
    public SDTypeConstraint(Record r, CodeGenHwModes hwModes) {
      operandNo = (int) r.getValueAsInt("OperandNum");

      if (r.isSubClassOf("SDTCisVT")) {
        constraintType = SDTCisVT;
        vvt = getValueTypeByHwMode(r.getValueAsDef("VT"), hwModes);
        for (TIntObjectIterator<MVT> itr = vvt.iterator(); itr.hasNext(); ) {
          if (itr.value().simpleVT == MVT.isVoid)
            Error.printFatalError(r.getLoc(), "Can't use 'Void' as type to SDTCisVT");
        }
      } else if (r.isSubClassOf("SDTCisPtrTy")) {
        constraintType = SDTCisPtrTy;
      } else if (r.isSubClassOf("SDTCisInt")) {
        constraintType = SDTCisInt;
      } else if (r.isSubClassOf("SDTCisFP")) {
        constraintType = SDTCisFP;
      } else if (r.isSubClassOf("SDTCisVec")) {
        constraintType = SDTCisVec;
      } else if (r.isSubClassOf("SDTCisSameAs")) {
        constraintType = SDTCisSameAs;
        x = (int) r.getValueAsInt("OtherOperandNum");
      } else if (r.isSubClassOf("SDTCisVTSmallerThanOp")) {
        constraintType = SDTCisVTSmallerThanOp;
        x = (int) r.getValueAsInt("OtherOperandNum");
      } else if (r.isSubClassOf("SDTCisOpSmallerThanOp")) {
        constraintType = SDTCisOpSmallerThanOp;
        x = (int) r.getValueAsInt("BigOperandNum");
      } else if (r.isSubClassOf("SDTCisEltOfVec")) {
        constraintType = SDTCisEltOfVec;
        x = (int) r.getValueAsInt("OtherOpNum");
      } else {
        Error.printFatalError("Unrecognized SDTypeConstraint '" + r.getName() + "'!");
      }
    }

    public int operandNo;

    public enum constraintType {
      SDTCisVT,
      SDTCisPtrTy,
      SDTCisInt,
      SDTCisFP,
      SDTCisVec,
      SDTCisSameAs,
      SDTCisVTSmallerThanOp,
      SDTCisOpSmallerThanOp,
      SDTCisEltOfVec
    }

    public constraintType constraintType;

    /**
     * For represents the SDTCisVT_info, SDTCisSameAs_Info,
     * SDTCisVTSmallerThanOp_Info, SDTCisOpSmallerThanOp_Info,
     * SDTCisEltOfVec_Info.
     */
    public int x;
    /**
     * The VT for SDTCisVT and SDTCVecEltisVT.
     */
    public ValueTypeByHwMode vvt;

    /**
     * Given a node in a pattern, apply this type constraint to the nodes operands.
     * This returns true if it makes a change, false otherwise.
     * If a type contradiction is found, issue an error.
     *
     * @param node
     * @param nodeInfo
     * @param tp
     * @return
     */
    public boolean applyTypeConstraint(TreePatternNode node,
                                       SDNodeInfo nodeInfo,
                                       TreePattern tp) {
      int numResults = nodeInfo.getNumResults();
      Util.assertion((numResults <= 1),
          "We only work which nodes with zero or result so far!");

      if (nodeInfo.getNumOperands() >= 0) {
        if (node.getNumChildren() != nodeInfo.getNumOperands())
          tp.error(node.getOperator().getName() +
              " node requires exactly "
              + nodeInfo.getNumOperands() + " operands");
      }

      TypeInfer infer = tp.getTypeInfer();
      CodeGenTarget cgt = tp.getDAGPatterns().getTarget();
      int[] tempResNo = new int[1];
      TreePatternNode nodeToApply = getOperandNum(operandNo, node, numResults, tempResNo);
      int resNo = tempResNo[0]; // The result number being referenced.

      switch (constraintType) {
        default:
          Util.assertion("Unknown constraint yet!");
        case SDTCisVT:
          return nodeToApply.updateNodeType(resNo, vvt, tp);
        case SDTCisPtrTy:
          // Require it to be one of the legal FP VTs.
          return nodeToApply.updateNodeType(resNo, MVT.iPTR, tp);
        case SDTCisInt: {
          // Require it to be one of the legal integer VTs.
          return infer.enforceInteger(nodeToApply.getExtType(resNo));
        }
        case SDTCisFP: {
          // Require it to be one of the legal floating point VTs.
          return infer.enforceFloatingPoint(nodeToApply.getExtType(resNo));
        }
        case SDTCisVec: {
          // Require it to be one of the legal vector VTs.
          return infer.enforceVector(nodeToApply.getExtType(resNo));
        }
        case SDTCisSameAs: {
          // Require it to be one of the legal floating point VTs.
          int[] oResNo = new int[1];
          TreePatternNode otherNode = getOperandNum(x, node, numResults, oResNo);
          return nodeToApply.updateNodeType(resNo, otherNode.getExtType(oResNo[0]), tp) |
              otherNode.updateNodeType(oResNo[0], nodeToApply.getExtType(resNo), tp);
        }
        case SDTCisVTSmallerThanOp: {
          if (!nodeToApply.isLeaf() || !(nodeToApply.getLeafValue() instanceof DefInit)
              || !((DefInit) nodeToApply.getLeafValue()).getDef().isSubClassOf("ValueType")) {
            tp.error(node.getOperator().getName() + " expects a VT operand!");
          }

          DefInit di = ((DefInit) nodeToApply.getLeafValue());
          vvt = getValueTypeByHwMode(di.getDef(), cgt.getHwModes());
          TypeSetByHwMode set = new TypeSetByHwMode(vvt);
          int[] oResNo = new int[1];
          TreePatternNode otherNode = getOperandNum(x, node, numResults, oResNo);
          return infer.enforceSmallerThan(set, otherNode.getExtType(oResNo[0]));
        }
        case SDTCisOpSmallerThanOp: {
          int[] bResNo = new int[1];
          TreePatternNode bigOp = getOperandNum(x, node, numResults, bResNo);
          return infer.enforceSmallerThan(nodeToApply.getExtType(resNo),
              bigOp.getExtType(bResNo[0]));
        }
        case SDTCisEltOfVec: {
          int[] vResNo = new int[1];
          TreePatternNode bigVecOp = getOperandNum(x, node, numResults, vResNo);
          return infer.enforceVectorSubVectorTypesIs(bigVecOp.getExtType(vResNo[0]),
              nodeToApply.getExtType(resNo));
        }
      }
    }

    /**
     * Return the node corresponding to operand {@code opNo} in tree
     *
     * @param opNo
     * @param node
     * @param numResults
     * @return
     * @{code node}, which has {@code numResults} results.
     */
    public TreePatternNode getOperandNum(int opNo,
                                         TreePatternNode node,
                                         int numResults,
                                         int[] resNo) {
      Util.assertion(numResults <= 1, "We only work with nodes with zero or one result so far!");
      if (opNo >= (numResults + node.getNumChildren())) {
        System.err.printf("Invalid operand number %d ", opNo);
        node.dump();
        System.err.println();
        System.exit(1);
      }

      if (opNo < numResults) {
        resNo[0] = opNo;
        return node;
      }
      else
        return node.getChild(opNo - numResults);
    }
  }
}
