/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2020, Jianping Zeng.
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
import backend.debug.DebugLoc;
import backend.intrinsic.Intrinsic;
import backend.mc.MCSymbol;
import backend.target.TargetInstrInfo;
import backend.target.TargetIntrinsicInfo;
import backend.target.TargetLowering;
import backend.type.Type;
import backend.value.*;
import gnu.trove.list.array.TIntArrayList;
import tools.*;

import java.io.PrintStream;
import java.util.*;

import static backend.codegen.dagisel.MemIndexedMode.UNINDEXED;
import static backend.support.AssemblyWriter.writeAsOperand;
import static backend.target.TargetRegisterInfo.isPhysicalRegister;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class SDNode implements Comparable<SDNode>, FoldingSetNode {
  protected int opcode;
  /**
   * This field used for keeping track of old opcode after marking this SDNode as DELETED_NODE.
   */
  protected int oldOpcode;
  protected int sublassData;
  protected int nodeID;
  protected SDUse[] operandList;
  protected EVT[] valueList;
  protected ArrayList<SDUse> useList;
  private static HashMap<EVT, TreeSet<EVT>> evts = new HashMap<>();
  private static EVT[] vts = new EVT[MVT.LAST_VALUETYPE];
  private boolean hasDebugValue;
  private DebugLoc debugLoc;

  private static EVT getValueTypeList(EVT vt) {
    if (vt.isExtended()) {
      evts.put(vt, new TreeSet<>());
      return vt;
    } else {
      vts[vt.getSimpleVT().simpleVT] = vt;
      return vt;
    }
  }

  public ArrayList<SDUse> getUseList() {
    return useList;
  }

  public void clearUseList() {
    useList.clear();
  }

  public int getOpcode() {
    return opcode;
  }

  public void setOpcode(int newOpc) {
    opcode = newOpc;
  }

  public boolean isTargetOpcode() {
    return opcode >= ISD.BUILTIN_OP_END;
  }

  public boolean isMachineOpecode() {
    return opcode < 0;
  }

  public int getMachineOpcode() {
    Util.assertion(isMachineOpecode());
    return ~opcode;
  }

  public boolean isUseEmpty() {
    if (useList == null || useList.isEmpty()) return true;
    for (int i = 0, e = getUseSize(); i < e; ++i) {
      if (!getUse(i).getUser().isDeleted())
        return false;
    }
    return true;
  }

  public boolean hasOneUse() {
    return !isUseEmpty() && useList.size() == 1;
  }

  public int getUseSize() {
    return useList == null ? 0 : useList.size();
  }

  public SDUse getUse(int idx) {
    Util.assertion(idx >= 0 && idx < getUseSize(), "index to use list out of range!");
    return useList.get(idx);
  }

  public int getNodeID() {
    return nodeID;
  }

  public void setNodeID(int id) {
    nodeID = id;
  }

  public boolean hasNumUsesOfValue(int numOfUses, int value) {
    Util.assertion(value < getNumValues(), "Illegal value!");
    for (SDUse u : useList) {
      if (u.getResNo() == value) {
        if (numOfUses == 0)
          return false;
        --numOfUses;
      }
    }
    return numOfUses == 0;
  }

  public boolean hasAnyUseOfValue(int value) {
    Util.assertion(value < getNumValues(), "Illegal value!");
    for (SDUse u : useList) {
      if (u.getResNo() == value) {
        return true;
      }
    }
    return false;
  }

  public boolean isOnlyUserOf(SDNode node) {
    for (SDUse u : useList) {
      if (!u.getUser().equals(node))
        return false;
    }
    return true;
  }

  public boolean isOperandOf(SDNode node) {
    for (int i = 0, e = node.getNumOperands(); i < e; i++) {
      if (node.getOperand(i).getNode().equals(this))
        return true;
    }
    return false;
  }

  private static void collectsPreds(SDNode root, HashSet<SDNode> visited) {
    if (root == null || !visited.add(root)) return;

    for (int i = 0, e = root.getNumOperands(); i < e; i++)
      collectsPreds(root.getOperand(i).getNode(), visited);
  }

  /**
   * Checks if this is predecessor of the specified node. The predecessor means it either is an
   * operand of the given node or it can be reached traversed recursively from the sub tree rooted at node.
   *
   * @param node
   * @return
   */
  public boolean isPredecessorOf(SDNode node) {
    HashSet<SDNode> visited = new HashSet<>();
    collectsPreds(node, visited);
    return visited.contains(this);
  }

  public static FltSemantics EVTToAPFloatSemantics(EVT vt) {
    switch (vt.getSimpleVT().simpleVT) {
      case MVT.f32:
        return APFloat.IEEEsingle;
      case MVT.f64:
        return APFloat.IEEEdouble;
      case MVT.f80:
        return APFloat.x87DoubleExtended;
      case MVT.f128:
        return APFloat.IEEEquad;
      default:
        Util.shouldNotReachHere("Unknown FP format!");
        return null;
    }
  }

  /**
   * Return this is a normal load SDNode.
   *
   * @return
   */
  public boolean isNormalLoad() {
    LoadSDNode ld = this instanceof LoadSDNode ? (LoadSDNode) this : null;
    return (ld != null && ld.getExtensionType() == LoadExtType.NON_EXTLOAD &&
        ld.getAddressingMode() == UNINDEXED);
  }

  public boolean isNONExtLoad() {
    LoadSDNode ld = this instanceof LoadSDNode ? (LoadSDNode) this : null;
    return ld != null && ld.getExtensionType() == LoadExtType.NON_EXTLOAD;
  }

  public boolean isExtLoad() {
    LoadSDNode ld = this instanceof LoadSDNode ? (LoadSDNode) this : null;
    return ld != null && ld.getExtensionType() == LoadExtType.EXTLOAD;
  }

  public boolean isSEXTLoad() {
    LoadSDNode ld = this instanceof LoadSDNode ? (LoadSDNode) this : null;
    return ld != null && ld.getExtensionType() == LoadExtType.SEXTLOAD;
  }

  public boolean isZEXTLoad() {
    LoadSDNode ld = this instanceof LoadSDNode ? (LoadSDNode) this : null;
    return ld != null && ld.getExtensionType() == LoadExtType.ZEXTLOAD;
  }

  public boolean isUNINDEXEDLoad() {
    LoadSDNode ld = this instanceof LoadSDNode ? (LoadSDNode) this : null;
    return ld != null && ld.getAddressingMode() == UNINDEXED;
  }

  public boolean isNormalStore() {
    StoreSDNode st = this instanceof StoreSDNode ? (StoreSDNode) this : null;
    return st != null && st.getAddressingMode() == UNINDEXED &&
        !st.isTruncatingStore();
  }

  public boolean isNONTRUNCStore() {
    StoreSDNode st = this instanceof StoreSDNode ? (StoreSDNode) this : null;
    return st != null && !st.isTruncatingStore();
  }

  public boolean isTRUNCStore() {
    StoreSDNode st = this instanceof StoreSDNode ? (StoreSDNode) this : null;
    return st != null && st.isTruncatingStore();
  }

  public boolean isUNINDEXEDStore() {
    StoreSDNode st = this instanceof StoreSDNode ? (StoreSDNode) this : null;
    return st != null && st.getAddressingMode() == UNINDEXED;
  }

  public int getNumOperands() {
    return operandList == null ? 0 : operandList.length;
  }

  public long getConstantOperandVal(int num) {
    Util.assertion(num >= 0 && num < getNumOperands());
    SDValue op = getOperand(num);
    Util.assertion(op.getNode() instanceof ConstantSDNode);
    return ((ConstantSDNode) op.getNode()).getZExtValue();
  }

  public SDValue getOperand(int num) {
    Util.assertion(num < getNumOperands() && num >= 0);
    return operandList[num].val;
  }

  public SDUse[] getOperandList() {
    return operandList;
  }

  public SDNode getFlaggedNode() {
    if (getNumOperands() != 0 && getOperand(getNumOperands() - 1).
        getValueType().getSimpleVT().simpleVT == MVT.Glue) {
      return getOperand(getNumOperands() - 1).getNode();
    }
    return null;
  }

  public SDNode getFlaggedMachineNode() {
    SDNode res = this;
    while (!res.isMachineOpecode()) {
      SDNode n = res.getFlaggedNode();
      if (n == null)
        break;
      res = n;
    }
    return res;
  }

  public int getNumValues() {
    return valueList.length;
  }

  public EVT getValueType(int resNo) {
    Util.assertion(resNo < getNumValues() && resNo >= 0, "resNo out of range");
    return valueList[resNo];
  }

  public int getValueSizeInBits(int resNo) {
    return getValueType(resNo).getSizeInBits();
  }

  public SDVTList getValueList() {
    return new SDVTList(valueList);
  }

  public String getOperationName() {
    return getOperationName(null);
  }

  public String getOperationName(SelectionDAG dag) {
    switch (getOpcode()) {
      default:
        if (isMachineOpecode()) {
          if (dag != null) {
            TargetInstrInfo tii = dag.getTarget().getSubtarget().getInstrInfo();
            if (tii != null)
              if (getMachineOpcode() < tii.getNumOperands())
                return tii.get(getMachineOpcode()).getName();
          }
          return "<<Unknown Machine Node>>";
        }
        if (dag != null) {
          TargetLowering tli = dag.getTarget().getSubtarget().getTargetLowering();
          String name = tli.getTargetNodeName(getOpcode());
          if (name != null) return name;
          return "<<Unknown Target Node>>";
        }
        return "<<Unknown Node>>";

      case ISD.DELETED_NODE:
        return "<<Deleted Node!>>";

      case ISD.PREFETCH:
        return "Prefetch";
      case ISD.MEMBARRIER:
        return "MemBarrier";
      case ISD.ATOMIC_CMP_SWAP:
        return "AtomicCmpSwap";
      case ISD.ATOMIC_SWAP:
        return "AtomicSwap";
      case ISD.ATOMIC_LOAD_ADD:
        return "AtomicLoadAdd";
      case ISD.ATOMIC_LOAD_SUB:
        return "AtomicLoadSub";
      case ISD.ATOMIC_LOAD_AND:
        return "AtomicLoadAnd";
      case ISD.ATOMIC_LOAD_OR:
        return "AtomicLoadOr";
      case ISD.ATOMIC_LOAD_XOR:
        return "AtomicLoadXor";
      case ISD.ATOMIC_LOAD_NAND:
        return "AtomicLoadNand";
      case ISD.ATOMIC_LOAD_MIN:
        return "AtomicLoadMin";
      case ISD.ATOMIC_LOAD_MAX:
        return "AtomicLoadMax";
      case ISD.ATOMIC_LOAD_UMIN:
        return "AtomicLoadUMin";
      case ISD.ATOMIC_LOAD_UMAX:
        return "AtomicLoadUMax";
      case ISD.PCMARKER:
        return "PCMarker";
      case ISD.READCYCLECOUNTER:
        return "ReadCycleCounter";
      case ISD.SRCVALUE:
        return "SrcValue";
      case ISD.MEMOPERAND:
        return "MemOperand";
      case ISD.EntryToken:
        return "EntryToken";
      case ISD.TokenFactor:
        return "TokenFactor";
      case ISD.AssertSext:
        return "AssertSext";
      case ISD.AssertZext:
        return "AssertZext";

      case ISD.BasicBlock:
        return "BasicBlock";
      case ISD.VALUETYPE:
        return "ValueType";
      case ISD.Register:
        return "Register";

      case ISD.Constant:
        return "Constant";
      case ISD.ConstantFP:
        return "ConstantFP";
      case ISD.GlobalAddress:
        return "GlobalAddress";
      case ISD.GlobalTLSAddress:
        return "GlobalTLSAddress";
      case ISD.FrameIndex:
        return "FrameIndex";
      case ISD.JumpTable:
        return "JumpTable";
      case ISD.GLOBAL_OFFSET_TABLE:
        return "GLOBAL_OFFSET_TABLE";
      case ISD.RETURNADDR:
        return "RETURNADDR";
      case ISD.FRAMEADDR:
        return "FRAMEADDR";
      case ISD.FRAME_TO_ARGS_OFFSET:
        return "FRAME_TO_ARGS_OFFSET";
      case ISD.EXCEPTIONADDR:
        return "EXCEPTIONADDR";
      case ISD.LSDAADDR:
        return "LSDAADDR";
      case ISD.EHSELECTION:
        return "EHSELECTION";
      case ISD.EH_RETURN:
        return "EH_RETURN";
      case ISD.ConstantPool:
        return "ConstantPool";
      case ISD.ExternalSymbol:
        return "ExternalSymbol";
      case ISD.BlockAddress:
        return "BlockAddress";

      case ISD.INTRINSIC_WO_CHAIN:
      case ISD.INTRINSIC_VOID:
      case ISD.INTRINSIC_W_CHAIN: {
        int opNo = opcode == ISD.INTRINSIC_WO_CHAIN ? 0 : 1;
        int iid = (int)((ConstantSDNode) getOperand(opNo).getNode()).getZExtValue();
        if (iid < Intrinsic.ID.num_intrinsics.ordinal())
          return Intrinsic.getName(iid);
        else if (dag.getTarget().getIntrinsinsicInfo() != null) {
          TargetIntrinsicInfo tii = dag.getTarget().getIntrinsinsicInfo();
          return tii.getName(iid);
        }
        Util.assertion("Invalid Intrinsic ID!");
      }

      case ISD.BUILD_VECTOR:
        return "BUILD_VECTOR";
      case ISD.TargetConstant:
        return "TargetConstant";
      case ISD.TargetConstantFP:
        return "TargetConstantFP";
      case ISD.TargetGlobalAddress:
        return "TargetGlobalAddress";
      case ISD.TargetGlobalTLSAddress:
        return "TargetGlobalTLSAddress";
      case ISD.TargetFrameIndex:
        return "TargetFrameIndex";
      case ISD.TargetJumpTable:
        return "TargetJumpTable";
      case ISD.TargetConstantPool:
        return "TargetConstantPool";
      case ISD.TargetExternalSymbol:
        return "TargetExternalSymbol";
      case ISD.TargetBlockAddress:
        return "TargetBlockAddress";
      case ISD.CopyToReg:
        return "CopyToReg";
      case ISD.CopyFromReg:
        return "CopyFromReg";
      case ISD.UNDEF:
        return "undef";
      case ISD.MERGE_VALUES:
        return "merge_values";
      case ISD.INLINEASM:
        return "inlineasm";
      case ISD.DBG_LABEL:
        return "dbg_label";
      case ISD.EH_LABEL:
        return "eh_label";
      case ISD.DECLARE:
        return "declare";
      case ISD.HANDLENODE:
        return "handlenode";

      // Unary operators
      case ISD.FABS:
        return "fabs";
      case ISD.FNEG:
        return "fneg";
      case ISD.FSQRT:
        return "fsqrt";
      case ISD.FSIN:
        return "fsin";
      case ISD.FCOS:
        return "fcos";
      case ISD.FPOWI:
        return "fpowi";
      case ISD.FPOW:
        return "fpow";
      case ISD.FTRUNC:
        return "ftrunc";
      case ISD.FFLOOR:
        return "ffloor";
      case ISD.FCEIL:
        return "fceil";
      case ISD.FRINT:
        return "frint";
      case ISD.FNEARBYINT:
        return "fnearbyint";

      // Binary operators
      case ISD.ADD:
        return "add";
      case ISD.SUB:
        return "sub";
      case ISD.MUL:
        return "mul";
      case ISD.MULHU:
        return "mulhu";
      case ISD.MULHS:
        return "mulhs";
      case ISD.SDIV:
        return "sdiv";
      case ISD.UDIV:
        return "udiv";
      case ISD.SREM:
        return "srem";
      case ISD.UREM:
        return "urem";
      case ISD.SMUL_LOHI:
        return "smul_lohi";
      case ISD.UMUL_LOHI:
        return "umul_lohi";
      case ISD.SDIVREM:
        return "sdivrem";
      case ISD.UDIVREM:
        return "udivrem";
      case ISD.AND:
        return "and";
      case ISD.OR:
        return "or";
      case ISD.XOR:
        return "xor";
      case ISD.SHL:
        return "shl";
      case ISD.SRA:
        return "sra";
      case ISD.SRL:
        return "srl";
      case ISD.ROTL:
        return "rotl";
      case ISD.ROTR:
        return "rotr";
      case ISD.FADD:
        return "fadd";
      case ISD.FSUB:
        return "fsub";
      case ISD.FMUL:
        return "fmul";
      case ISD.FDIV:
        return "fdiv";
      case ISD.FREM:
        return "frem";
      case ISD.FCOPYSIGN:
        return "fcopysign";
      case ISD.FGETSIGN:
        return "fgetsign";

      case ISD.SETCC:
        return "setcc";
      case ISD.VSETCC:
        return "vsetcc";
      case ISD.SELECT:
        return "select";
      case ISD.SELECT_CC:
        return "select_cc";
      case ISD.INSERT_VECTOR_ELT:
        return "insert_vector_elt";
      case ISD.EXTRACT_VECTOR_ELT:
        return "extract_vector_elt";
      case ISD.CONCAT_VECTORS:
        return "concat_vectors";
      case ISD.EXTRACT_SUBVECTOR:
        return "extract_subvector";
      case ISD.SCALAR_TO_VECTOR:
        return "scalar_to_vector";
      case ISD.VECTOR_SHUFFLE:
        return "vector_shuffle";
      case ISD.CARRY_FALSE:
        return "carry_false";
      case ISD.ADDC:
        return "addc";
      case ISD.ADDE:
        return "adde";
      case ISD.SADDO:
        return "saddo";
      case ISD.UADDO:
        return "uaddo";
      case ISD.SSUBO:
        return "ssubo";
      case ISD.USUBO:
        return "usubo";
      case ISD.SMULO:
        return "smulo";
      case ISD.UMULO:
        return "umulo";
      case ISD.SUBC:
        return "subc";
      case ISD.SUBE:
        return "sube";
      case ISD.SHL_PARTS:
        return "shl_parts";
      case ISD.SRA_PARTS:
        return "sra_parts";
      case ISD.SRL_PARTS:
        return "srl_parts";

      // Conversion operators.
      case ISD.SIGN_EXTEND:
        return "sign_extend";
      case ISD.ZERO_EXTEND:
        return "zero_extend";
      case ISD.ANY_EXTEND:
        return "any_extend";
      case ISD.SIGN_EXTEND_INREG:
        return "sign_extend_inreg";
      case ISD.TRUNCATE:
        return "truncate";
      case ISD.FP_ROUND:
        return "fp_round";
      case ISD.FLT_ROUNDS_:
        return "flt_rounds";
      case ISD.FP_ROUND_INREG:
        return "fp_round_inreg";
      case ISD.FP_EXTEND:
        return "fp_extend";

      case ISD.SINT_TO_FP:
        return "sint_to_fp";
      case ISD.UINT_TO_FP:
        return "uint_to_fp";
      case ISD.FP_TO_SINT:
        return "fp_to_sint";
      case ISD.FP_TO_UINT:
        return "fp_to_uint";
      case ISD.BIT_CONVERT:
        return "bit_convert";

      case ISD.CONVERT_RNDSAT: {
        Util.assertion(false, "Not supported!");
      }

      // Control flow instructions
      case ISD.BR:
        return "br";
      case ISD.BRIND:
        return "brind";
      case ISD.BR_JT:
        return "br_jt";
      case ISD.BRCOND:
        return "brcond";
      case ISD.BR_CC:
        return "br_cc";
      case ISD.CALLSEQ_START:
        return "callseq_start";
      case ISD.CALLSEQ_END:
        return "callseq_end";

      // Other operators
      case ISD.LOAD:
        return "load";
      case ISD.STORE:
        return "store";
      case ISD.VAARG:
        return "vaarg";
      case ISD.VACOPY:
        return "vacopy";
      case ISD.VAEND:
        return "vaend";
      case ISD.VASTART:
        return "vastart";
      case ISD.DYNAMIC_STACKALLOC:
        return "dynamic_stackalloc";
      case ISD.EXTRACT_ELEMENT:
        return "extract_element";
      case ISD.BUILD_PAIR:
        return "build_pair";
      case ISD.STACKSAVE:
        return "stacksave";
      case ISD.STACKRESTORE:
        return "stackrestore";
      case ISD.TRAP:
        return "trap";

      // Bit manipulation
      case ISD.BSWAP:
        return "bswap";
      case ISD.CTPOP:
        return "ctpop";
      case ISD.CTTZ:
        return "cttz";
      case ISD.CTLZ:
        return "ctlz";

      // Debug info
      case ISD.DBG_STOPPOINT:
        return "dbg_stoppoint";
      case ISD.DEBUG_LOC:
        return "debug_loc";

      // Trampolines
      case ISD.TRAMPOLINE:
        return "trampoline";

      case ISD.CONDCODE:
        switch (((CondCodeSDNode) this).getCondition()) {
          default:
            Util.shouldNotReachHere("Unknown setcc condition!");
          case SETOEQ:
            return "setoeq";
          case SETOGT:
            return "setogt";
          case SETOGE:
            return "setoge";
          case SETOLT:
            return "setolt";
          case SETOLE:
            return "setole";
          case SETONE:
            return "setone";

          case SETO:
            return "seto";
          case SETUO:
            return "setuo";
          case SETUEQ:
            return "setue";
          case SETUGT:
            return "setugt";
          case SETUGE:
            return "setuge";
          case SETULT:
            return "setult";
          case SETULE:
            return "setule";
          case SETUNE:
            return "setune";

          case SETEQ:
            return "seteq";
          case SETGT:
            return "setgt";
          case SETGE:
            return "setge";
          case SETLT:
            return "setlt";
          case SETLE:
            return "setle";
          case SETNE:
            return "setne";
        }
    }
  }

  public static String getIndexedModeName(MemIndexedMode am) {
    switch (am) {
      default:
        return "";
      case PRE_DEC:
        return "<pre-inc>";
      case POST_DEC:
        return "<post-inc>";
      case PRE_INC:
        return "<pre-inc>";
      case POST_INC:
        return "<post-inc>";
    }
  }

  public void printTypes(PrintStream os) {
    printTypes(os, null);
  }

  public void printTypes(PrintStream os, SelectionDAG dag) {
    os.printf("0x%x: ", this.hashCode());
    for (int i = 0, e = getNumValues(); i < e; i++) {
      if (i != 0) os.print(",");
      if (getValueType(i).equals(new EVT(MVT.Other)))
        os.print("ch");
      else
        os.print(getValueType(i).getEVTString());
    }
    os.printf(" = %s", getOperationName(dag));
  }

  public void printDetails(PrintStream os, SelectionDAG dag) {
    if (this instanceof MachineSDNode) {
      MachineSDNode memSD = (MachineSDNode) this;
      if (memSD.memRefs != null) {
        os.print("<Mem:");
        int i = 0;
        for (MachineMemOperand mmo : memSD.memRefs) {
          mmo.print(os);
          if (i < memSD.memRefs.length - 1)
            os.print(" ");
        }
        os.print(">");
      }
    }
    else if (isTargetOpcode() && getOpcode() == ISD.VECTOR_SHUFFLE) {
      ShuffleVectorSDNode svn = (ShuffleVectorSDNode) this;
      os.print("<");
      for (int i = 0, e = valueList[0].getVectorNumElements(); i < e; i++) {
        int idx = svn.getMaskElt(i);
        if (i != 0) os.print(",");
        if (idx < 0)
          os.print("u");
        else
          os.print(idx);
      }
      os.print(">");
    }
    else if (this instanceof ConstantSDNode) {
      os.print("<");
      ((ConstantSDNode) this).getAPIntValue().print(os);
      os.print(">");
    } else if (this instanceof ConstantFPSDNode) {
      ConstantFPSDNode fpn = (ConstantFPSDNode) this;
      if (fpn.getValueAPF().getSemantics() == APFloat.IEEEsingle)
        os.printf("<%f>", fpn.getValueAPF().convertToFloat());
      else if (fpn.getValueAPF().getSemantics() == APFloat.IEEEdouble)
        os.printf("<%f>", fpn.getValueAPF().convertToDouble());
      else {
        os.print("<APFloat(");
        fpn.getValueAPF().bitcastToAPInt().print(os);
        os.print(")>");
      }
    } else if (this instanceof GlobalAddressSDNode) {
      GlobalAddressSDNode gan = (GlobalAddressSDNode) this;
      long offset = gan.getOffset();
      os.print("<");
      writeAsOperand(os, gan.getGlobalValue(), true, null);
      os.print(">");
      String prefix = offset > 0 ? " + " : " ";
      os.printf("%s%d", prefix, offset);
      int tf = gan.getTargetFlags();
      if (tf != 0)
        os.printf("[TF=%d]", tf);
    } else if (this instanceof FrameIndexSDNode) {
      FrameIndexSDNode fi = (FrameIndexSDNode) this;
      os.printf("<%d>", fi.getFrameIndex());
    } else if (this instanceof JumpTableSDNode) {
      JumpTableSDNode jt = (JumpTableSDNode) this;
      os.printf("<%d>", jt.getJumpTableIndex());
    } else if (this instanceof ConstantPoolSDNode) {
      ConstantPoolSDNode cp = (ConstantPoolSDNode) this;
      int offset = cp.getOffset();
      if (cp.isMachineConstantPoolValue()) {
        os.print("<");
        cp.getMachineConstantPoolValue().print(os);
        os.print(">");
      } else {
        os.print("<");
        cp.getConstantValue().print(os);
        os.print(">");
      }
      String prefix = offset > 0 ? "+" : " ";
      os.printf("%s%s", prefix, offset);
      int tf = cp.getTargetFlags();
      if (tf != 0)
        os.printf(" [TF=%d]", tf);
    } else if (this instanceof BasicBlockSDNode) {
      BasicBlockSDNode bbn = (BasicBlockSDNode) this;
      os.print("<");
      Value lbb = bbn.getBasicBlock().getBasicBlock();
      if (lbb != null)
        os.printf("%s ", lbb.getName());
      os.printf("%x>", bbn.getBasicBlock().hashCode());
    } else if (this instanceof RegisterSDNode) {
      RegisterSDNode rsn = (RegisterSDNode) this;
      if (dag != null && rsn.getReg() != 0 &&
          isPhysicalRegister(rsn.getReg())) {
        os.printf(" %s", dag.getTarget().getSubtarget().getRegisterInfo().getName(rsn.getReg()));
      } else {
        os.printf(" #%d", rsn.getReg());
      }
    } else if (this instanceof ExternalSymbolSDNode) {
      ExternalSymbolSDNode esn = (ExternalSymbolSDNode) this;
      os.printf("'%s'", esn.getExtSymol());
      int tf = esn.getTargetFlags();
      if (tf != 0)
        os.printf("[TF=%d]", tf);
    } else if (this instanceof SrcValueSDNode) {
      SrcValueSDNode ssn = (SrcValueSDNode) this;
      if (ssn.getValue() != null)
        os.printf("<0x%x>", ssn.getValue().hashCode());
      else
        os.print("<null>");
    } else if (this instanceof MemOperandSDNode) {
      MemOperandSDNode mon = (MemOperandSDNode) this;
      if (mon.getMachineMemOperand().getValue() != null) {
        os.printf("<0x%x:%d>", mon.getMachineMemOperand().getValue().hashCode(),
            mon.getMachineMemOperand().getOffset());
      } else {
        os.printf("<null:%d>", mon.getMachineMemOperand().getOffset());
      }
    } else if (this instanceof VTSDNode) {
      os.printf(":%s", ((VTSDNode) this).getVT().getEVTString());
    } else if (this instanceof LoadSDNode) {
      LoadSDNode ld = (LoadSDNode) this;
      Value srcValue = ld.getSrcValue();
      int srcOffset = ld.getSrcValueOffset();
      os.print(" <");
      if (srcValue != null)
        os.printf("0x%x", srcValue.hashCode());
      else
        os.print("null");
      os.printf(":%d>", srcOffset);

      boolean doExt = true;
      switch (ld.getExtensionType()) {
        default:
          doExt = false;
          break;
        case EXTLOAD:
          os.print(" <anyext ");
          break;
        case SEXTLOAD:
          os.print(" <sext ");
          break;
        case ZEXTLOAD:
          os.print(" <zext ");
          break;
      }
      if (doExt)
        os.printf("%s>", ld.getMemoryVT().getEVTString());

      String am = getIndexedModeName(ld.getAddressingMode());
      os.printf(" %s", am);
      if (ld.isVolatile())
        os.print(" <volatile>");
      os.printf(" alignment=%d", ld.getAlignment());
    } else if (this instanceof StoreSDNode) {
      StoreSDNode st = (StoreSDNode) this;
      Value srcValue = st.getSrcValue();
      int srcOffset = st.getSrcValueOffset();
      os.print(" <");
      if (srcValue != null)
        os.printf("0x%x", srcValue.hashCode());
      else
        os.print("null");
      os.printf(":%d>", srcOffset);

      if (st.isTruncatingStore())
        os.printf(" <trunc %s>", st.getMemoryVT().getEVTString());

      String am = getIndexedModeName(st.getAddressingMode());
      os.printf(" %s", am);
      if (st.isVolatile())
        os.print(" <volatile>");
      os.printf(" alignment=%d", st.getAlignment());
    } else if (this instanceof AtomicSDNode) {
      AtomicSDNode at = (AtomicSDNode) this;
      Value srcValue = at.getSrcValue();
      int srcOffset = at.getSrcValueOffset();
      os.print(" <");
      if (srcValue != null)
        os.printf("0x%x", srcValue.hashCode());
      else
        os.print("null");
      os.printf(":%d>", srcOffset);

      if (at.isVolatile())
        os.print(" <volatile>");
      os.printf(" alignment=%d", at.getAlignment());
    }
    else if (this instanceof BlockAddressSDNode) {
      BlockAddressSDNode ba = (BlockAddressSDNode) this;
      os.print("<");
      writeAsOperand(os, ba.getBlockAddress().getFunction(), false, null);
      os.print(",");
      writeAsOperand(os, ba.getBlockAddress().getBasicBlock(), false, null);
      os.print(">");
      int tf = ba.getTargetFlags();
      if (tf != 0)
        os.printf("[TF=%d]", tf);
    }
  }

  public void print(PrintStream os) {
    print(os, null);
  }

  public void print(PrintStream os, SelectionDAG dag) {
    printTypes(os, dag);
    os.print(" ");
    for (int i = 0, e = getNumOperands(); i < e; i++) {
      if (i != 0) os.print(", ");
      if (getOperand(i).getNode() == null)
        os.print("0x0");
      else
        os.printf("0x%x", getOperand(i).getNode().hashCode());
      int resNo = getOperand(i).getResNo();
      if (resNo != 0)
        os.printf(":%d", resNo);
    }
    printDetails(os, dag);
    os.println();
  }

  public void printr(PrintStream os, SelectionDAG dag) {
  }

  public void printr(PrintStream os) {
    printr(os, null);
  }

  public void dump() {
    dump(null);
  }

  public void dumpr() {
  }

  public void dump(SelectionDAG dag) {
    print(System.err, dag);
  }

  public void addUse(SDUse use) {
    Util.assertion(use != null);
    if (useList == null)
      useList = new ArrayList<>();
    useList.add(use);
  }

  public void removeUse(SDUse use) {
    Util.assertion(use != null);
    useList.remove(use);
  }

  public int compareTo(SDNode node) {
    return 0;
  }

  @Override
  public void profile(FoldingSetNodeID id) {
    SelectionDAG.addNodeToID(id, this);
  }

  public boolean isDeleted() {
    return getOpcode() == ISD.DELETED_NODE;
  }

  public void markDeleted() {
    oldOpcode = opcode;
    setOpcode(ISD.DELETED_NODE);
  }

  public void setHasDebugValue(boolean b) {
    hasDebugValue = b;
  }

  public boolean getHasDebugValue() { return hasDebugValue; }

  public DebugLoc getDebugLoc() {
    return debugLoc;
  }

  public static class SDVTList {
    EVT[] vts;
    int numVTs;

    public SDVTList() {
    }

    public SDVTList(EVT[] vts) {
      this.vts = vts;
      this.numVTs = vts.length;
    }
  }

  protected static SDVTList getSDVTList(EVT vt) {
    SDVTList list = new SDVTList();
    list.vts = new EVT[1];
    list.vts[0] = getValueTypeList(vt);
    list.numVTs = 1;
    return list;
  }

  protected SDNode(int opc, DebugLoc dl, SDVTList vts, ArrayList<SDValue> ops) {
    this.opcode = opc;
    this.debugLoc = dl;
    sublassData = 0;
    nodeID = -1;
    operandList = new SDUse[ops.size()];
    valueList = vts.vts;
    for (int i = 0; i < ops.size(); i++) {
      operandList[i] = new SDUse();
      operandList[i].setUser(this);
      operandList[i].setInitial(ops.get(i));
    }
    useList = new ArrayList<>();
  }

  protected SDNode(int opc, DebugLoc dl, SDVTList vts, SDValue[] ops) {
    Util.assertion(ops != null);
    this.opcode = opc;
    debugLoc = dl;
    sublassData = 0;
    nodeID = -1;
    operandList = new SDUse[ops.length];
    valueList = vts.vts;
    for (int i = 0; i < ops.length; i++) {
      operandList[i] = new SDUse();
      operandList[i].setUser(this);
      operandList[i].setInitial(ops[i]);
    }
    useList = new ArrayList<>();
  }

  protected SDNode(int opc,  DebugLoc dl, SDVTList list) {
    opcode = opc;
    debugLoc = dl;
    sublassData = 0;
    nodeID = -1;
    operandList = null;
    valueList = list.vts;
    useList = new ArrayList<>();
  }

  protected void initOperands(SDValue op0) {
    operandList = new SDUse[1];
    operandList[0] = new SDUse();
    operandList[0].setUser(this);
    operandList[0].setInitial(op0);
  }

  protected void initOperands(SDValue op0, SDValue op1) {
    operandList = new SDUse[2];
    operandList[0] = new SDUse();
    operandList[1] = new SDUse();
    operandList[0].setUser(this);
    operandList[0].setInitial(op0);
    operandList[1].setUser(this);
    operandList[1].setInitial(op1);
  }

  protected void initOperands(SDValue op0, SDValue op1, SDValue op2) {
    operandList = new SDUse[3];
    operandList[0] = new SDUse();
    operandList[1] = new SDUse();
    operandList[2] = new SDUse();

    operandList[0].setUser(this);
    operandList[0].setInitial(op0);
    operandList[1].setUser(this);
    operandList[1].setInitial(op1);
    operandList[2].setUser(this);
    operandList[2].setInitial(op2);
  }

  protected void initOperands(SDValue op0, SDValue op1, SDValue op2, SDValue op3) {
    operandList = new SDUse[4];
    operandList[0] = new SDUse();
    operandList[1] = new SDUse();
    operandList[2] = new SDUse();
    operandList[3] = new SDUse();

    operandList[0].setUser(this);
    operandList[0].setInitial(op0);
    operandList[1].setUser(this);
    operandList[1].setInitial(op1);
    operandList[2].setUser(this);
    operandList[2].setInitial(op2);
    operandList[3].setUser(this);
    operandList[3].setInitial(op3);
  }

  protected void initOperands(ArrayList<SDValue> vals) {
    Util.assertion(vals != null && vals.size() > 0, "Illegal values for initialization!");
    operandList = new SDUse[vals.size()];
    for (int i = 0; i < operandList.length; i++) {
      operandList[i] = new SDUse();
      operandList[i].setUser(this);
      operandList[i].setInitial(vals.get(i));
    }
  }

  void initOperands(SDValue... vals) {
    if (vals == null || vals.length <= 0)
      return;
    operandList = new SDUse[vals.length];
    for (int i = 0; i < operandList.length; i++) {
      operandList[i] = new SDUse();
      operandList[i].setUser(this);
      operandList[i].setInitial(vals[i]);
    }
  }

  public void dropOperands() {
    if (operandList != null && operandList.length > 0) {
      for (SDUse op : operandList) {
        if (op.get().getNode() != null)
          op.get().getNode().removeUse(op);
      }
      operandList = null;
    }
  }

  public static class UnarySDNode extends SDNode {
    public UnarySDNode(int opc, DebugLoc dl, SDVTList vts, SDValue op) {
      super(opc, dl, vts);
      initOperands(op);
    }
  }

  public static class BinarySDNode extends SDNode {
    public BinarySDNode(int opc, DebugLoc dl, SDVTList vts, SDValue op0, SDValue op1) {
      super(opc, dl, vts);
      initOperands(op0, op1);
    }
  }

  public static class TernarySDNode extends SDNode {
    public TernarySDNode(int opc, DebugLoc dl, SDVTList vts, SDValue op0, SDValue op1, SDValue op2) {
      super(opc, dl, vts);
      initOperands(op0, op1, op2);
    }
  }

  private static int encodeMemSDNodeFlags(int convType, MemIndexedMode mode,
                                          boolean isVolatile, int alignment) {
    Util.assertion((convType & 3) == convType, "convType may not require more than 2 bits!");
    Util.assertion((mode.ordinal() & 7) == mode.ordinal(), "mode may not require more than 3 bits!");
    return convType | (mode.ordinal() << 2) |
        ((isVolatile ? 1 : 0) << 5) |
        ((Util.log2(alignment) + 1) << 6);
  }

  /**
   * SDNode for memory operation.
   */
  public static class MemSDNode extends SDNode {
    private EVT memoryVT;
    private MachineMemOperand mmo;

    public MemSDNode(int opc, DebugLoc dl, SDVTList vts, EVT memVT, MachineMemOperand mmo) {
      super(opc, dl, vts);
      this.memoryVT = memVT;
      this.mmo = mmo;
      sublassData = encodeMemSDNodeFlags(0, UNINDEXED, mmo.isVolatile(), mmo.getAlignment());
      Util.assertion(Util.isPowerOf2(mmo.getAlignment()));
      Util.assertion(getAlignment() == mmo.getAlignment());
      Util.assertion(isVolatile() == mmo.isVolatile());
    }

    public MemSDNode(int opc, DebugLoc dl, SDVTList vts, SDValue[] ops, EVT memVT,
                     MachineMemOperand mmo) {
      super(opc, dl, vts, ops);
      this.memoryVT = memVT;
      this.mmo = mmo;
      sublassData = encodeMemSDNodeFlags(0, UNINDEXED, mmo.isVolatile(), mmo.getAlignment());
      Util.assertion(Util.isPowerOf2(mmo.getAlignment()));
      Util.assertion(getAlignment() == mmo.getAlignment());
      Util.assertion(isVolatile() == mmo.isVolatile());
    }

    public int getAlignment() {
      return (1 << (sublassData >> 6)) >> 1;
    }

    public boolean isVolatile() {
      return ((sublassData >> 5) & 0x1) != 0;
    }

    public boolean isNonTemporal() { Util.shouldNotReachHere(); return false; }
    public int getRawSubclassData() {
      return sublassData;
    }

    public Value getSrcValue() {
      return mmo.getValue();
    }

    public int getSrcValueOffset() {
      return (int) mmo.getOffset();
    }

    public EVT getMemoryVT() {
      return memoryVT;
    }

    public SDValue getChain() {
      return getOperand(0);
    }

    public SDValue getBasePtr() {
      return getOperand(getOpcode() == ISD.STORE ? 2 : 1);
    }

    public MachineMemOperand getMemOperand() { return mmo; }
  }

  public static class AtomicSDNode extends MemSDNode {

    public AtomicSDNode(int opc,
                        DebugLoc dl,
                        SDVTList vts,
                        EVT memVT,
                        SDValue chain,
                        SDValue ptr,
                        SDValue cmp,
                        SDValue swp,
                        MachineMemOperand mmo) {
      super(opc, dl, vts, memVT, mmo);
      initOperands(chain, ptr, cmp, swp);
    }

    public AtomicSDNode(int opc,
                        DebugLoc dl,
                        SDVTList vts,
                        EVT memVT,
                        SDValue chain,
                        SDValue ptr,
                        SDValue val,
                        MachineMemOperand mmo) {
      super(opc, dl, vts, memVT, mmo);
      initOperands(chain, ptr, val);
    }

    @Override
    public SDValue getBasePtr() {
      return getOperand(1);
    }

    public SDValue getVal() {
      return getOperand(2);
    }

    public boolean isCompareAndSwap() {
      return getOpcode() == ISD.ATOMIC_CMP_SWAP;
    }
  }

  public static class MemIntrinsicSDNode extends MemSDNode {

    public MemIntrinsicSDNode(int opc, DebugLoc dl, SDVTList vts, SDValue[] ops,
                              EVT memVT, MachineMemOperand mmo) {
      super(opc, dl, vts, ops, memVT, mmo);
    }
  }

  public static class ConstantSDNode extends SDNode {
    private ConstantInt value;

    public ConstantSDNode(boolean isTarget, ConstantInt val, EVT vt) {
      super(isTarget ? ISD.TargetConstant : ISD.Constant, new DebugLoc(), getSDVTList(vt));
      value = val;
    }

    public ConstantInt getConstantIntValue() {
      return value;
    }

    public APInt getAPIntValue() {
      return value.getValue();
    }

    public long getZExtValue() {
      return value.getZExtValue();
    }

    public long getSExtValue() {
      return value.getSExtValue();
    }

    public boolean isNullValue() {
      return value.isNullValue();
    }

    public boolean isAllOnesValue() {
      return value.isAllOnesValue();
    }
  }

  public static class ConstantFPSDNode extends SDNode {
    private ConstantFP value;

    public ConstantFPSDNode(boolean isTarget, ConstantFP val, EVT vt) {
      super(isTarget ? ISD.TargetConstantFP : ISD.ConstantFP, new DebugLoc(), getSDVTList(vt));
      value = val;
    }

    public APFloat getValueAPF() {
      return value.getValueAPF();
    }

    public ConstantFP getConstantFPValue() {
      return value;
    }

    public boolean isExactlyValue(double v) {
      APFloat tmp = new APFloat(v);
      OutRef<Boolean> ignored = new OutRef<>(false);
      tmp.convert(value.getValueAPF().getSemantics(),
          APFloat.RoundingMode.rmNearestTiesToEven, ignored);
      return isExactlyValue(tmp);
    }

    public boolean isExactlyValue(APFloat v) {
      return getValueAPF().bitwiseIsEqual(v);
    }

    public boolean isValueValidForType(EVT vt, APFloat val) {
      Util.assertion(vt.isFloatingPoint(), "Can only convert between FP types!");
      APFloat val2 = new APFloat(val);
      OutRef<Boolean> ignored = new OutRef<>(false);
      val2.convert(EVTToAPFloatSemantics(vt), APFloat.RoundingMode.rmNearestTiesToEven,
          ignored);
      return !ignored.get();
    }
  }

  public static class GlobalAddressSDNode extends SDNode {
    private GlobalValue gv;
    private long offset;
    private int tsFlags;

    public GlobalAddressSDNode(int opc, DebugLoc dl, EVT vt, GlobalValue gv, long off, int targetFlags) {
      super(opc, dl, getSDVTList(vt));
      this.gv = gv;
      this.offset = off;
      this.tsFlags = targetFlags;
    }

    public GlobalValue getGlobalValue() {
      return gv;
    }

    public long getOffset() {
      return offset;
    }

    public int getTargetFlags() {
      return tsFlags;
    }
  }

  public static class FrameIndexSDNode extends SDNode {
    private int frameIndex;

    public FrameIndexSDNode(int fi, EVT vt, boolean isTarget) {
      super(isTarget ? ISD.TargetFrameIndex : ISD.FrameIndex, new DebugLoc(), getSDVTList(vt));
      this.frameIndex = fi;
    }

    public int getFrameIndex() {
      return frameIndex;
    }

    public void setFrameIndex(int frameIndex) {
      this.frameIndex = frameIndex;
    }
  }

  public static class JumpTableSDNode extends SDNode {
    private int jumpTableIndex;
    private int targetFlags;

    public JumpTableSDNode(int jti, EVT vt, boolean isTarget, int tf) {
      super(isTarget ? ISD.TargetJumpTable : ISD.JumpTable, new DebugLoc(), getSDVTList(vt));
      jumpTableIndex = jti;
      targetFlags = tf;
    }

    public int getJumpTableIndex() {
      return jumpTableIndex;
    }

    public void setJumpTableIndex(int jumpTableIndex) {
      this.jumpTableIndex = jumpTableIndex;
    }

    public int getTargetFlags() {
      return targetFlags;
    }
  }

  public static class ConstantPoolSDNode extends SDNode {
    private Object val;
    private int offset;
    private int align;
    private int targetFlags;

    public ConstantPoolSDNode(boolean isTarget, Constant cnt, EVT vt,
                              int off, int align, int targetFlags) {
      super(isTarget ? ISD.TargetConstantPool : ISD.ConstantPool, new DebugLoc(), getSDVTList(vt));
      val = cnt;
      offset = off;
      this.align = align;
      this.targetFlags = targetFlags;
    }

    public ConstantPoolSDNode(boolean isTarget, MachineConstantPoolValue machPoolVal,
                              EVT vt, int off, int align, int targetFlags) {
      super(isTarget ? ISD.TargetConstantPool : ISD.ConstantPool, new DebugLoc(), getSDVTList(vt));
      val = machPoolVal;
      offset = off;
      this.align = align;
      this.targetFlags = targetFlags;
    }

    public boolean isMachineConstantPoolValue() {
      return val instanceof MachineConstantPoolValue;
    }

    public Constant getConstantValue() {
      Util.assertion(!isMachineConstantPoolValue());
      return (Constant) val;
    }

    public MachineConstantPoolValue getMachineConstantPoolValue() {
      Util.assertion(isMachineConstantPoolValue());
      return (MachineConstantPoolValue) val;
    }

    public int getOffset() {
      return offset;
    }

    public int getAlignment() {
      return align;
    }

    public int getTargetFlags() {
      return targetFlags;
    }

    public Type getType() {
      if (isMachineConstantPoolValue())
        return getMachineConstantPoolValue().getType();
      return getConstantValue().getType();
    }
  }

  public static class BasicBlockSDNode extends SDNode {
    private MachineBasicBlock bb;

    public BasicBlockSDNode(MachineBasicBlock bb) {
      super(ISD.BasicBlock, new DebugLoc(), getSDVTList(new EVT(MVT.Other)));
      this.bb = bb;
    }

    public MachineBasicBlock getBasicBlock() {
      return bb;
    }
  }

  public static class MemOperandSDNode extends SDNode {
    private MachineMemOperand mmo;

    public MemOperandSDNode(MachineMemOperand mmo) {
      super(ISD.MEMOPERAND, new DebugLoc(), getSDVTList(new EVT(MVT.Other)));
      this.mmo = mmo;
    }

    public MachineMemOperand getMachineMemOperand() {
      return mmo;
    }
  }

  public static class RegisterSDNode extends SDNode {
    private int reg;

    public RegisterSDNode(EVT vt, int reg) {
      super(ISD.Register, new DebugLoc(), getSDVTList(vt));
      this.reg = reg;
    }

    public int getReg() {
      return reg;
    }
  }

  public static class EHLabelSDNode extends SDNode {
    private MCSymbol label;
    public EHLabelSDNode(DebugLoc dl, SDValue ch, MCSymbol l) {
      super(ISD.EH_LABEL, dl, getSDVTList(new EVT(MVT.Other)));
      initOperands(ch);
      this.label = l;
    }

    public MCSymbol getLabel() { return label; }
  }

  public static class BlockAddressSDNode extends SDNode {
    private BlockAddress ba;
    private int targetFlags;

    protected BlockAddressSDNode(int opc, EVT vt, BlockAddress ba, int ts) {
      super(opc, new DebugLoc(), getSDVTList(vt));
      this.ba = ba;
      targetFlags = ts;
    }

    public BlockAddress getBlockAddress() { return ba; }
    public int getTargetFlags() { return targetFlags; }
  }

  public static class ExternalSymbolSDNode extends SDNode {
    private String extSymol;
    private int targetFlags;

    public ExternalSymbolSDNode(boolean isTarget, EVT vt, String sym, int flags) {
      super(isTarget ? ISD.TargetExternalSymbol : ISD.ExternalSymbol, new DebugLoc(), getSDVTList(vt));
      this.extSymol = sym;
      this.targetFlags = flags;
    }

    public String getExtSymol() {
      return extSymol;
    }

    public int getTargetFlags() {
      return targetFlags;
    }
  }

  public static class CondCodeSDNode extends SDNode {
    private CondCode condition;

    public CondCodeSDNode(CondCode cc) {
      super(ISD.CONDCODE, new DebugLoc(), getSDVTList(new EVT(MVT.Other)));
      condition = cc;
    }

    public CondCode getCondition() {
      return condition;
    }
  }

  public static class VTSDNode extends SDNode {
    private EVT vt;

    public VTSDNode(EVT vt) {
      super(ISD.VALUETYPE, new DebugLoc(), getSDVTList(new EVT(MVT.Other)));
      this.vt = vt;
    }

    public EVT getVT() { return vt; }
  }

  public static class LSBaseSDNode extends MemSDNode {
    public LSBaseSDNode(int nodeTy, DebugLoc dl, SDValue[] operands, SDVTList vts,
                        MemIndexedMode mode, EVT memVT, MachineMemOperand mmo) {
      super(nodeTy, dl, vts, memVT, mmo);
      Util.assertion(mmo.getAlignment() != 0,
              "Loads and Stores should have non-zero alignment!");
      sublassData |= mode.ordinal() << 2;
      Util.assertion(getAddressingMode() == mode);
      initOperands(operands);
      Util.assertion(getOffset().getOpcode() == ISD.UNDEF || isIndexed(),
              "Only indexed loads and stores have a non-undef offset operand!");
    }

    public SDValue getOffset() {
      return getOperand(getOpcode() == ISD.LOAD ? 2 : 3);
    }

    public MemIndexedMode getAddressingMode() {
      return MemIndexedMode.values()[(sublassData >> 2) & 7];
    }

    public boolean isIndexed() {
      return getAddressingMode() != UNINDEXED;
    }

    public boolean isUnindexed() {
      return !isIndexed();
    }
  }

  public static class LoadSDNode extends LSBaseSDNode {
    public LoadSDNode(SDValue[] chainPtrOff, DebugLoc dl,
                      SDVTList vts, MemIndexedMode mode,
                      LoadExtType ety,
                      EVT memvt, MachineMemOperand mmo) {
      super(ISD.LOAD, dl, chainPtrOff, vts, mode, memvt, mmo);
      sublassData |= ety.ordinal();
      Util.assertion(getExtensionType() == ety, "LoadExtType encoding error!");
    }

    public LoadExtType getExtensionType() {
      return LoadExtType.values()[sublassData & 3];
    }

    @Override
    public SDValue getBasePtr() {
      return getOperand(1);
    }

    @Override
    public SDValue getOffset() {
      return getOperand(2);
    }
  }

  public static class StoreSDNode extends LSBaseSDNode {
    public StoreSDNode(SDValue[] chainPtrOff,
                       DebugLoc dl, SDVTList vts,
                       MemIndexedMode mode,
                       boolean isTrunc, EVT vt,
                       MachineMemOperand mmo) {
      super(ISD.STORE, dl, chainPtrOff, vts, mode, vt, mmo);
      sublassData |= isTrunc ? 1 : 0;
      Util.assertion(isTruncatingStore() == isTrunc);
    }

    public boolean isTruncatingStore() {
      return (sublassData & 1) != 0;
    }

    public SDValue getValue() {
      return getOperand(1);
    }

    @Override
    public SDValue getBasePtr() {
      return getOperand(2);
    }

    @Override
    public SDValue getOffset() {
      return getOperand(3);
    }
  }

  public static class HandleSDNode extends SDNode {
    public HandleSDNode(SDValue x) {
      super(ISD.HANDLENODE, new DebugLoc(), getSDVTList(new EVT(MVT.Other)));
      initOperands(x);
    }

    public SDValue getValue() {
      return getOperand(0);
    }
  }

  public static class CvtRndSatSDNode extends SDNode {
    private CvtCode code;

    public CvtRndSatSDNode(EVT vt, DebugLoc dl, CvtCode code, SDValue[] ops) {
      super(ISD.CONVERT_RNDSAT, dl, getSDVTList(vt), ops);
      this.code = code;
    }

    public CvtCode getCvtCode() {
      return code;
    }
  }

  public static class ShuffleVectorSDNode extends SDNode {
    private int[] mask;

    public ShuffleVectorSDNode(SDVTList vts,
                               DebugLoc dl,
                               ArrayList<SDValue> ops,
                               int[] mask) {
      super(ISD.VECTOR_SHUFFLE, dl, vts, ops);
      this.mask = mask;
    }

    public ShuffleVectorSDNode(SDVTList vts,
                               DebugLoc dl,
                               SDValue op0,
                               SDValue op1,
                               int[] mask) {
      super(ISD.VECTOR_SHUFFLE, dl, vts, new SDValue[] {op0, op1});
      this.mask = mask;
    }

    public void getMask(TIntArrayList m) {
      Util.assertion(m != null);
      EVT vt = getValueType(0);
      m.clear();
      for (int i = 0, e = vt.getVectorNumElements(); i < e; i++)
        m.add(mask[i]);
    }

    public int getMaskElt(int idx) {
      Util.assertion(idx < getValueType(0).getVectorNumElements());
      return mask[idx];
    }

    public boolean isSplat() {
      return isSplatMask(mask, getValueType(0));
    }

    public static boolean isSplatMask(int[] mask, EVT vt) {
      int i = 0, e = vt.getVectorNumElements();
      for (; i < e && mask[i] < 0; i++) ;

      Util.assertion(i != e);
      for (int idx = mask[i]; i < e; i++)
        if (mask[i] >= 0 && mask[i] != idx)
          return false;
      return true;
    }

    public int getSplatIndex() {
      Util.assertion(isSplat());
      return mask[0];
    }
  }

  public static class SrcValueSDNode extends SDNode {
    private Value val;

    public SrcValueSDNode(Value val) {
      super(ISD.SRCVALUE, new DebugLoc(), getSDVTList(new EVT(MVT.Other)));
      this.val = val;
    }

    public Value getValue() {
      return val;
    }
  }

  /**
   * An SDNode that represents everything that will be needed
   * to construct a MachineInstr. These nodes are created during the
   * instruction selection proper phase.
   */
  public static class MachineSDNode extends SDNode {
    private MachineMemOperand[] memRefs;

    public MachineSDNode(int opc, DebugLoc dl, SDVTList vts) {
      super(opc, dl, vts);
    }

    public void setMemRefs(MachineMemOperand[] memRefs) {
      this.memRefs = memRefs;
    }
  }
}
