package utils.tablegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
 * All rights reserved.

 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import backend.codegen.MVT;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.FormattedOutputStream;
import tools.Pair;
import tools.Util;
import tools.commandline.BooleanOpt;
import tools.commandline.Desc;
import tools.commandline.Initializer;
import tools.commandline.OptionNameApplicator;
import utils.tablegen.Matcher.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.TreeMap;

import static utils.tablegen.SDNP.SDNPHasChain;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class DAGISelMatcherEmitter {
  private static final int MAX_SIZE = 10900;

  private ArrayList<String> initializedMtds;
  private ArrayList<String> patternPredicates;
  private ArrayList<String> nodePredicates;
  private ArrayList<ComplexPattern> complexPatterns;
  private ArrayList<Record> nodeXForms;

  private TObjectIntHashMap<String> patternPredicateMap;
  private TObjectIntHashMap<String> nodePredicateMap;
  private TObjectIntHashMap<ComplexPattern> complexPatternMap;
  private TObjectIntHashMap<Record> nodeXFormMap;

  public static final BooleanOpt OmitComments =
      new BooleanOpt(OptionNameApplicator.optionName("omit-comments"),
          Desc.desc("Don't generate comments on matcher table"),
          Initializer.init(false));

  public static final int CommentIndent = 30;

  private String targetName;

  public DAGISelMatcherEmitter(String targetName) {
    initializedMtds = new ArrayList<>();
    patternPredicates = new ArrayList<>();
    nodePredicates = new ArrayList<>();
    complexPatterns = new ArrayList<>();
    nodeXForms = new ArrayList<>();
    patternPredicateMap = new TObjectIntHashMap<>();
    nodePredicateMap = new TObjectIntHashMap<>();
    complexPatternMap = new TObjectIntHashMap<>();
    nodeXFormMap = new TObjectIntHashMap<>();
    this.targetName = targetName;
  }

  private static int getVBRSize(int val) {
    if (val <= 127) return 1;

    int numBytes = 0;
    while (val >= 128) {
      val >>= 7;
      ++numBytes;
    }
    return numBytes + 1;
  }

  private int getPatternPredicate(String pred) {
    if (patternPredicateMap.containsKey(pred))
      return patternPredicateMap.get(pred);
    int res = patternPredicates.size();
    patternPredicateMap.put(pred, res);
    patternPredicates.add(pred);
    return res;
  }

  private int getNodePredicate(String pred) {
    if (nodePredicateMap.containsKey(pred))
      return nodePredicateMap.get(pred);

    int res = nodePredicates.size();
    nodePredicateMap.put(pred, res);
    nodePredicates.add(pred);
    return res;
  }

  private int getComplexPat(ComplexPattern cp) {
    if (complexPatternMap.containsKey(cp))
      return complexPatternMap.get(cp);

    int res = complexPatterns.size();
    complexPatternMap.put(cp, res);
    complexPatterns.add(cp);
    return res;
  }

  private int getNodeXFormID(Record rec) {
    if (nodeXFormMap.containsKey(rec))
      return nodeXFormMap.get(rec);

    int res = nodeXForms.size();
    nodeXFormMap.put(rec, res);
    nodeXForms.add(rec);
    return res;
  }

  private String getFullRegisterName(String reg) {
    return targetName + "GenRegisterNames." + reg;
  }

  private long emitMatcher(Matcher m,
                           int indent,
                           int currentIdx,
                           FormattedOutputStream os) {
    os.padToColumn(indent * 2);

    switch (m.getKind()) {
      case Scope: {
        Matcher.ScopeMatcher sm = (Matcher.ScopeMatcher) m;
        Util.assertion(sm.getNext() == null);
        int satrtIdx = currentIdx;

        for (int i = 0, e = sm.getNumChildren(); i < e; i++) {
          if (i == 0) {
            os.print("OPC_Scope, ");
            ++currentIdx;
          } else {
            if (!OmitComments.value) {
              os.printf("/*%d*/", currentIdx);
              os.padToColumn(indent * 2);
              os.print("/*Scope*/");
            } else
              os.padToColumn(indent * 2);
          }

          int childSize = 0;
          int vbrSize = 0;
          String tempBuf;
          do {
            vbrSize = getVBRSize(childSize);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            FormattedOutputStream fos = new FormattedOutputStream(baos);
            childSize = emitMatcherList(sm.getChild(i), indent + 1,
                currentIdx + vbrSize, fos);
            tempBuf = baos.toString();
          } while (getVBRSize(childSize) != vbrSize);

          Util.assertion(childSize != 0);
          currentIdx += emitVBRValue(childSize, os);
          if (!OmitComments.value) {
            os.printf("/*->%d*/", currentIdx + childSize);

            if (i == 0) {
              os.padToColumn(CommentIndent);
              os.printf("// %d children in Scope", sm.getNumChildren());
            }
          }

          os.println();
          os.print(tempBuf);
          currentIdx += childSize;
        }

        if (!OmitComments.value)
          os.printf("/*%d*/", currentIdx);
        os.padToColumn(indent * 2);
        os.printf("0, ");
        if (!OmitComments.value)
          os.print("/*End of Scope*/");
        os.println();
        return currentIdx - satrtIdx + 1;
      }
      case RecordNode:
        RecordMatcher rm = (RecordMatcher) m;
        os.print("OPC_RecordNode,");
        if (!OmitComments.value) {
          os.padToColumn(CommentIndent);
          os.printf("// #%d = %s", rm.getResultNo(), rm.getWhatFor());
        }
        os.println();
        return 1;
      case RecordChild:
        RecordChildMatcher rcm = (RecordChildMatcher) m;
        os.printf("OPC_RecordChild%d,", rcm.getChildNo());
        if (!OmitComments.value) {
          os.padToColumn(CommentIndent);
          os.printf("// #%d = %s", rcm.getResultNo(), rcm.getWhatFor());
        }
        os.println();
        return 1;
      case RecordMemRef:
        os.println("OPC_RecordMemRef,");
        return 1;
      case CaptureFlagInput:
        os.println("OPC_CaptureFlagInput,");
        return 1;
      case MoveChild:
        os.printf("OPC_MoveChild, %d,\n", ((MoveChildMatcher) m).getChildNo());
        return 2;
      case MoveParent:
        os.println("OPC_MoveParent,");
        return 1;
      case CheckSame:
        CheckSameMatcher csm = (CheckSameMatcher) m;
        os.printf("OPC_CheckSame,%d,\n", csm.getMatchNumber());
        return 2;
      case CheckPatternPredicate: {
        CheckPatternPredicateMatcher cpm = (CheckPatternPredicateMatcher) m;
        String pred = cpm.getPredicate();
        os.printf("OPC_CheckPatternPredicate, %d,", getPatternPredicate(pred));
        if (!OmitComments.value) {
          os.padToColumn(CommentIndent);
          os.printf("// %s\n", pred);
        }
        return 2;
      }
      case CheckPredicate: {
        String pred = ((CheckPredicateMatcher) m).getPredicateName();
        os.printf("OPC_CheckPredicate, %d,", getNodePredicate(pred));
        if (!OmitComments.value) {
          os.padToColumn(CommentIndent);
          os.printf("// %s", pred);
        }
        os.println();
        return 2;
      }
      case CheckOpcode:
        os.printf("OPC_CheckOpcode, %s, %n",
            ((CheckOpcodeMatcher) m).getOpcode().getEnumName());
        return 2;
      case SwitchOpcode:
      case SwitchType: {
        int startIdx = currentIdx;
        int numCases = 0;
        boolean isSwitchOpcode = false;
        if (m instanceof SwitchOpcodeMatcher) {
          os.printf("OPC_SwitchOpcode ");
          numCases = ((SwitchOpcodeMatcher) m).getNumCases();
          isSwitchOpcode = true;
        } else {
          os.print("OPC_SwitchType ");
          numCases = ((SwitchTypeMatcher) m).getNumCases();
        }

        if (!OmitComments.value)
          os.printf("/*%d*/", numCases);
        os.print(",");
        ++currentIdx;
        for (int i = 0; i < numCases; i++) {
          Matcher child;
          child = isSwitchOpcode ? ((SwitchOpcodeMatcher) m).getCaseMatcher(i) :
              ((SwitchTypeMatcher) m).getCaseMatcher(i);

          int childSize = 0;
          int vbrSize = 0;
          String tempBuf;
          do {
            vbrSize = getVBRSize(childSize);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            FormattedOutputStream fos = new FormattedOutputStream(baos);
            childSize = emitMatcherList(child, indent + 1,
                currentIdx + vbrSize, fos);
            tempBuf = baos.toString();
          } while (getVBRSize(childSize) != vbrSize);

          Util.assertion(childSize != 0);
          currentIdx += emitVBRValue(childSize, os);

          if (i != 0) {
            os.padToColumn(indent * 2);
            if (!OmitComments.value)
              os.printf(isSwitchOpcode ? "/*SwitchOpcode*/" : "/*SwitchType*/");
          }

          os.print(' ');
          if (isSwitchOpcode) {
            os.print(((SwitchOpcodeMatcher) m).getCaseOpcode(i).getEnumName());
          } else
            os.print(MVT.getEnumName(((SwitchTypeMatcher) m).getCaseType(i)));
          os.print(',');
          if (!OmitComments.value)
            os.printf("// ->%d", currentIdx + childSize + 1);
          os.println();
          ++currentIdx;
          os.print(tempBuf);
          currentIdx += childSize;
        }

        os.padToColumn(indent * 2);
        os.print("0, ");
        if (!OmitComments.value)
          os.print(isSwitchOpcode ? "// EndSwitchOpcode" : "// EndSwitchType");
        os.println();
        ++currentIdx;
        return currentIdx - startIdx;
      }
      case CheckType:
        os.printf("OPC_CheckType, %s,\n",
            MVT.getEnumName(((CheckTypeMatcher) m).getType()));
        return 2;

      case CheckChildType:
        CheckChildTypeMatcher ctm = (CheckChildTypeMatcher) m;
        os.printf("OPC_CheckChildType%dType, %s,\n",
            ctm.getChildNo(), MVT.getEnumName(ctm.getType()));
        return 2;
      case CheckInteger: {
        os.print("OPC_CheckInteger, ");
        long bytes = emitVBRValue(((CheckIntegerMatcher) m).getValue(), os);
        os.println();
        return bytes + 1;
      }
      case CheckCondCode:
        os.printf("OPC_CheckCondCode, CondCode.%s.ordinal(),\n",
            ((CheckCondCodeMatcher) m).getCondcodeName());
        return 2;
      case CheckValueType:
        os.printf("OPC_CheckValueType, MVT.%s,\n",
            ((CheckValueTypeMatcher) m).getTypeName());
        return 2;
      case CheckComplexPat: {
        CheckComplexPatMatcher cpm = (CheckComplexPatMatcher) m;
        ComplexPattern cp = cpm.getPatern();
        os.printf("OPC_CheckComplexPat, /*cp*/%d, /*#*/%d,", getComplexPat(cp),
            cpm.getMatchNumber());
        if (!OmitComments.value) {
          os.padToColumn(CommentIndent);
          os.printf("// %s", cp.getSelectFunc());
          os.printf(":$%s", cpm.getName());
          for (int i = 0, e = cp.getNumOperands(); i < e; i++)
            os.printf(" #%d", cpm.getFirstResult() + i);

          if (cp.hasProperty(SDNPHasChain))
            os.print(" + chain result");
        }
        os.println();
        return 3;
      }
      case CheckAndImm: {
        os.print("OPC_CheckAndImm,");
        long bytes = 1 + emitVBRValue(((CheckAndImmMatcher) m).getValue(), os);
        os.println();
        return bytes;
      }
      case CheckOrImm: {
        os.print("OPC_CheckOrImm,");
        long bytes = 1 + emitVBRValue(((CheckOrImmMatcher) m).getValue(), os);
        os.println();
        return bytes;
      }
      case CheckFoldableChainNode:
        os.println("OPC_CheckFoldableChainNode,");
        return 1;
      case EmitInteger: {
        EmitIntegerMatcher eim = (EmitIntegerMatcher) m;
        long val = eim.getValue();
        os.printf("OPC_EmitInteger, %s, ", MVT.getEnumName(eim.getVT()));
        long bytes = 2 + emitVBRValue(val, os);
        os.println();
        return bytes;
      }
      case EmitStringInteger: {
        EmitStringIntegerMatcher esm = (EmitStringIntegerMatcher) m;
        String val = esm.getValue();
        os.printf("OPC_EmitInteger, %s, %sGenRegisterInfo.%s,\n",
            MVT.getEnumName(esm.getVT()), targetName, val);
        return 3;
      }
      case EmitRegister: {
        EmitRegisterMatcher erm = (EmitRegisterMatcher) m;
        os.printf("OPC_EmitRegister, %s, ", MVT.getEnumName(erm.getVT()));
        Record r = erm.getRegister();
        if (r != null)
          os.printf("%s,\n", r.getName());
        else {
          os.printf("0 ");
          if (!OmitComments.value)
            os.print("/*zero_reg*/");
          os.println(",");
        }
        return 3;
      }
      case EmitConvertToTarget:
        os.printf("OPC_EmitConvertToTarget, %d,\n",
            ((EmitConvertToTargetMatcher) m).getSlot());
        return 2;
      case EmitMergeInputChains: {
        EmitMergeInputChainsMatcher emm = (EmitMergeInputChainsMatcher) m;
        os.printf("OPC_EmitMergeInputChains, %d, ", emm.getNumNodes());
        for (int i = 0, e = emm.getNumNodes(); i < e; i++)
          os.printf("%d, ", emm.getNode(i));
        os.println();
        return 2 + emm.getNumNodes();
      }
      case EmitCopyToReg: {
        EmitCopyToRegMatcher ecm = (EmitCopyToRegMatcher) m;
        os.printf("OPC_EmitCopyToReg, %d, %s,\n",
            ecm.getSrcSlot(), getFullRegisterName(ecm.getDstPhysReg().getName()));
        return 3;
      }
      case EmitNodeXForm: {
        EmitNodeXFromMatcher exm = (EmitNodeXFromMatcher) m;
        os.printf("OPC_EmitNodeXForm, %d, %d,",
            getNodeXFormID(exm.getNodeXForm()), exm.getSlot());
        if (!OmitComments.value) {
          os.padToColumn(CommentIndent);
          os.printf("// %s", exm.getNodeXForm().getName());
          os.println();
          return 3;
        }
      }
      case EmitNode:
      case MorphNodeTo: {
        EmitNodeMatcherCommon emc = (EmitNodeMatcherCommon) m;
        os.printf(emc instanceof EmitNodeMatcher ? "OPC_EmitNode" : "OPC_MorphNodeTo");
        os.printf(", ");
        emitTargetOpcode(emc.getOpcodeName(), os);
        os.print(", 0");

        if (emc.hasChain) os.print("|OPFL_Chain");
        if (emc.hasInFlag) os.print("|OPFL_FlagInput");
        if (emc.hasOutFlag) os.print("|OPFL_FlagOutput");
        if (emc.hasMemRefs) os.print("|OPFL_MemRefs");
        if (emc.getNumFixedArityOperands() != -1)
          os.printf("|OPFL_Variadic%d", emc.getNumFixedArityOperands());
        os.println(",");

        os.padToColumn(indent * 2 + 4);
        os.print(emc.getNumVTs());
        if (!OmitComments.value)
          os.print("/*#VTs*/");

        os.print(", ");
        for (int i = 0, e = emc.getNumVTs(); i < e; i++)
          os.printf("%s, ", MVT.getEnumName(emc.getVT(i)));
        os.print(emc.getNumOperands());
        if (!OmitComments.value)
          os.printf("/*#Ops*/");
        os.print(", ");
        int numOperandBytes = 0;
        for (int i = 0, e = emc.getNumOperands(); i < e; i++) {
          numOperandBytes += emitVBRValue(emc.getOperand(i), os);
        }

        if (!OmitComments.value) {
          if (emc instanceof EmitNodeMatcher) {
            EmitNodeMatcher enm = (EmitNodeMatcher) emc;
            int numResults = enm.getNumVTs();
            if (numResults != 0) {
              os.padToColumn(CommentIndent);
              os.printf("// Results = ");
              int first = enm.getFirstResultSlot();
              for (int i = 0; i < numResults; i++)
                os.printf("#%d ", first + i);
            }
          }
          os.println();
          if (emc instanceof MorphNodeToMatcher) {
            MorphNodeToMatcher mnm = (MorphNodeToMatcher) emc;
            os.padToColumn(indent * 2);
            os.print("// Src: ");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream tmpOS = new PrintStream(baos);
            mnm.getPattern().getSrcPattern().print(tmpOS);
            os.println(baos.toString());

            os.padToColumn(indent * 2);
            os.print("// Dst: ");
            baos = new ByteArrayOutputStream();
            tmpOS = new PrintStream(baos);
            mnm.getPattern().getDstPattern().print(tmpOS);
            os.println(baos.toString());
          }
        } else
          os.println();

        return 6 + emc.getNumVTs() + numOperandBytes;
      }
      case MarkFlagResults: {
        MarkFlagResultsMatcher mrm = (MarkFlagResultsMatcher) m;
        os.printf("OPC_MarkFlagResults, %d, ", mrm.getNumNodes());
        int numOperandBytes = 0;
        for (int i = 0, e = mrm.getNumNodes(); i < e; i++)
          numOperandBytes += emitVBRValue(mrm.getNode(i), os);

        os.println();
        return 2 + numOperandBytes;
      }
      case CompleteMatch: {
        CompleteMatchMatcher cmm = (CompleteMatchMatcher) m;
        os.printf("OPC_CompleteMatch, %d, ", cmm.getNumResults());
        int numResultBytes = 0;
        for (int i = 0, e = cmm.getNumResults(); i < e; i++)
          numResultBytes += emitVBRValue(cmm.getResult(i), os);
        os.println();
        if (!OmitComments.value) {
          os.padToColumn(indent * 2);
          os.print("// Src: ");

          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          PrintStream tmpOS = new PrintStream(baos);
          cmm.getPattern().getSrcPattern().print(tmpOS);
          os.println(baos.toString());

          os.padToColumn(indent * 2);
          os.print("// Dst: ");
          baos = new ByteArrayOutputStream();
          tmpOS = new PrintStream(baos);
          cmm.getPattern().getDstPattern().print(tmpOS);
          os.println(baos.toString());
        }
        return 2 + numResultBytes;
      }
    }
    Util.shouldNotReachHere("Unknown matcher operator!");
    return 0;
  }

  private void emitTargetOpcode(String opc, FormattedOutputStream os) {
    if (opc.startsWith(targetName))
      opc = targetName + "GenInstrNames" + opc.substring(targetName.length());

    os.printf("%s&0xFF, (%s>>>8)&0xFF", opc, opc);
  }

  private long emitVBRValue(long val, FormattedOutputStream os) {

    if (Long.compareUnsigned(val, 127) <= 0) {
      os.printf("%d, ", val);
      return 1;
    }

    long inVal = val;
    int numBytes = 0;
    while (Long.compareUnsigned(val, 128) >= 0) {
      os.printf("%d|128,", (byte) (val & 127));
      val >>>= 7;
      ++numBytes;
    }
    os.print(val);
    if (!OmitComments.value)
      os.printf("/*%s*/", Long.toUnsignedString(inVal));
    os.print(", ");
    return numBytes + 1;
  }

  private int emitMatcherList(Matcher theMatcher,
                              int indent,
                              int currentIdx,
                              FormattedOutputStream os) {
    int size = 0;
    while (theMatcher != null) {
      if (!OmitComments.value)
        os.printf("/*%d*/", currentIdx);
      long matcherSize = emitMatcher(theMatcher, indent, currentIdx, os);
      size += matcherSize;
      currentIdx += matcherSize;
      theMatcher = theMatcher.getNext();
    }
    return size;
  }

  private void buildHistogram(Matcher theMatcher,
                              int[] opcodeFreq) {
    for (; theMatcher != null; theMatcher = theMatcher.getNext()) {
      Util.assertion(theMatcher.getKind().ordinal() < opcodeFreq.length);

      opcodeFreq[theMatcher.getKind().ordinal()]++;
      if (theMatcher instanceof Matcher.ScopeMatcher) {
        Matcher.ScopeMatcher sm = (Matcher.ScopeMatcher) theMatcher;
        for (int i = 0, e = sm.getNumChildren(); i < e; i++)
          buildHistogram(sm.getChild(i), opcodeFreq);
      } else if (theMatcher instanceof SwitchOpcodeMatcher) {
        SwitchOpcodeMatcher som = (SwitchOpcodeMatcher) theMatcher;
        for (int i = 0, e = som.getNumCases(); i < e; i++)
          buildHistogram(som.getCaseMatcher(i), opcodeFreq);
      } else if (theMatcher instanceof SwitchTypeMatcher) {
        SwitchTypeMatcher stm = (SwitchTypeMatcher) theMatcher;
        for (int i = 0, e = stm.getNumCases(); i < e; i++)
          buildHistogram(stm.getCaseMatcher(i), opcodeFreq);
      }
    }
  }

  private void emitHistogram(Matcher theMatcher,
                             FormattedOutputStream os) {
    if (OmitComments.value)
      return;

    int[] opcodeFreg = new int[MatcherKind.values().length];
    buildHistogram(theMatcher, opcodeFreg);

    os.println("  // Opcode Histogram:");
    for (int opc = 0; opc < opcodeFreg.length; ++opc) {
      os.print("  // #");
      switch (MatcherKind.values()[opc]) {
        case Scope:
          os.print("OPC_Scope");
          break;
        case RecordNode:
          os.print("OPC_RecordNode");
          break;
        case RecordChild:
          os.print("OPC_RecordChild");
          break;
        case RecordMemRef:
          os.print("OPC_RecordMemRef");
          break;
        case CaptureFlagInput:
          os.print("OPC_CaptureFlagInput");
          break;
        case MoveChild:
          os.print("OPC_MoveChild");
          break;
        case MoveParent:
          os.print("OPC_MoveParent");
          break;
        case CheckSame:
          os.print("OPC_CheckSame");
          break;
        case CheckPatternPredicate:
          os.print("OPC_CheckPatternPredicate");
          break;
        case CheckPredicate:
          os.print("OPC_CheckPredicate");
          break;
        case CheckOpcode:
          os.print("OPC_CheckOpcode");
          break;
        case SwitchOpcode:
          os.print("OPC_SwitchOpcode");
          break;
        case CheckType:
          os.print("OPC_CheckType");
          break;
        case SwitchType:
          os.print("OPC_SwitchType");
          break;
        case CheckChildType:
          os.print("OPC_CheckChildType");
          break;
        case CheckInteger:
          os.print("OPC_CheckInteger");
          break;
        case CheckCondCode:
          os.print("OPC_CheckCondCode");
          break;
        case CheckValueType:
          os.print("OPC_CheckValueType");
          break;
        case CheckComplexPat:
          os.print("OPC_CheckComplexPat");
          break;
        case CheckAndImm:
          os.print("OPC_CheckAndImm");
          break;
        case CheckOrImm:
          os.print("OPC_CheckOrImm");
          break;
        case CheckFoldableChainNode:
          os.print("OPC_CheckFoldableChainNode");
          break;
        case EmitInteger:
          os.print("OPC_EmitInteger");
          break;
        case EmitStringInteger:
          os.print("OPC_EmitStringInteger");
          break;
        case EmitRegister:
          os.print("OPC_EmitRegister");
          break;
        case EmitConvertToTarget:
          os.print("OPC_EmitConvertToTarget");
          break;
        case EmitMergeInputChains:
          os.print("OPC_EmitMergeInputChains");
          break;
        case EmitCopyToReg:
          os.print("OPC_EmitCopyToReg");
          break;
        case EmitNode:
          os.print("OPC_EmitNode");
          break;
        case MorphNodeTo:
          os.print("OPC_MorphNodeTo");
          break;
        case EmitNodeXForm:
          os.print("OPC_EmitNodeXForm");
          break;
        case MarkFlagResults:
          os.print("OPC_MarkFlagResults");
          break;
        case CompleteMatch:
          os.print("OPC_CompleteMatch");
          break;
      }
      os.padToColumn(40);
      os.printf(" = %d%n", opcodeFreg[opc]);
    }
    os.println();
  }

  private void emitPredicateFunctions(CodeGenDAGPatterns cgp,
                                      FormattedOutputStream os) {
    // Emit pattern predicates.
    if (!patternPredicates.isEmpty()) {
      os.println("  public boolean checkPatternPredicate(int predNo) {");
      os.println("    switch(predNo) {");
      os.println("      default: Util.assertion(\"Invalid predicate in table\");");
      for (int i = 0, e = patternPredicates.size(); i < e; i++)
        os.printf("      case %d: return %s;%n", i, patternPredicates.get(i));
      os.println("    }");
      os.println("  }\n");
    }

    // Emit Node predicates.
    TreeMap<String, TreePattern> patByName = new TreeMap<>();
    cgp.getPatternFragments().forEach((rec, tp) -> {
      patByName.put(rec.getName(), tp);
    });

    if (!nodePredicates.isEmpty()) {
      os.println("  public boolean checkNodePredicate(SDNode node, int predNo) {");
      os.println("    switch(predNo) {");
      os.println("      default: Util.assertion(\"Invalid predicate in table\");");
      for (int i = 0, e = nodePredicates.size(); i < e; i++) {
        TreePattern tp = patByName.get(nodePredicates.get(i).substring("Predicate_".length()));
        Util.assertion(tp != null);
        String code = tp.getRecord().getValueAsCode("Predicate");
        Util.assertion(!code.isEmpty(), "No code in this predicate");
        os.printf("      case %d: { // %s%n", i, nodePredicates.get(i));
        String className;
        if (tp.getOnlyTree().isLeaf())
          className = "SDNode";
        else
          className = cgp.getSDNodeInfo(tp.getOnlyTree().getOperator()).getSDClassName();

        if (className.equals("SDNode"))
          os.println("        SDNode n = node;");
        else
          os.printf("        %s n = (%s)node;", className, className);
        os.printf("        %s\n", code);
        os.println("      }");
      }
      os.println("    }");
      os.println("  }\n");
    }

    // Emit CompletePattern matchers.
    if (!complexPatterns.isEmpty()) {
      os.println("  public boolean checkComplexPattern(SDNode root,");
      os.println("                                     SDValue n,");
      os.println("                                     int patternNo,");
      os.println("                                     ArrayList<SDValue> result) {");
      os.println("    switch(patternNo) {");
      os.println("      default: Util.assertion(\"Invalid pattern # in table?\");");
      for (int i = 0, e = complexPatterns.size(); i < e; i++) {
        ComplexPattern cp = complexPatterns.get(i);
        int numOps = cp.getNumOperands();
        if (cp.hasProperty(SDNPHasChain))
          ++numOps;

        os.printf("      case %d: {%n", i);
        os.printf("        SDValue[] tmp = new SDValue[%d];%n", numOps);
        os.printf("        boolean res = %s(root, n, tmp);\n", cp.getSelectFunc());
        os.println("        result.addAll(Arrays.asList(tmp));");
        os.println("        return res;");
        os.println("     }");
      }
      os.println("    }");
      os.println("  }");
    }

    if (!nodeXForms.isEmpty()) {
      os.println("  public SDValue runSDNodeXForm(SDValue v, int xformNo) {");
      os.println("    switch(xformNo) {");
      os.println("      default: Util.assertion(\"Invalid xform # in table?\");");
      for (int i = 0, e = nodeXForms.size(); i < e; i++) {
        Pair<Record, String> itr = cgp.getSDNodeTransform(nodeXForms.get(i));
        Record sdnode = itr.first;
        String code = itr.second;

        os.printf("      case %d: {\n", i);
        if (!OmitComments.value)
          os.printf("        // %s\n", nodeXForms.get(i).getName());
        String className = cgp.getSDNodeInfo(sdnode).getSDClassName();
        if (className.equals("SDNode"))
          os.println("        SDNode n = v.getNode();");
        else
          os.printf("        %s n = (%s)v.getNode();%n", className, className);
        os.printf("\t\t\t%s%n", code);
        os.println("      }");
      }
      os.println("    }");
      os.println("  }");
    }
  }

  private static int index = 0;

  private static void printMatcherTable(String table, long totalSize, PrintStream os) {
    os.printf("  private static void initTable%d(int[] matcherTable) {\n", index++);
    os.println("    int[] table = new int[]{");
    String completeMark = "OPC_CompleteMatch";
    String morphMark = "OPC_MorphNodeTo";
    for (int i = 0, size = table.length(); i < size; ) {
      int end;
      String endMark;
      int NumNewlineSkipped;
      if (size - i > completeMark.length() && table.substring(i, i + completeMark.length()).equals(completeMark)) {
        end = i + completeMark.length();
        endMark = completeMark;
        NumNewlineSkipped = 3;
      }
      else if (size - i > morphMark.length() && table.substring(i, i + morphMark.length()).equals(morphMark)) {
        end = i + morphMark.length();
        endMark = morphMark;
        NumNewlineSkipped = 4;
      }
      else {
        os.print(table.charAt(i));
        ++i;
        continue;
      }

      // separate the matcher for each complete match code piece.
      os.print(endMark);
      i = end;
      int numNewLines = 0;
      while (i < size && numNewLines < NumNewlineSkipped) {
        os.print(table.charAt(i));
        if (table.charAt(i) == '\n')
          ++numNewLines;
        ++i;
      }

      // If we can't see any OPC_CompleteMatch in the successive part, we have to
      // include this part into current function instead of separating it into other function
      boolean shouldSplit = false;
      for (int j = i; j < size - endMark.length(); j++) {
        if (table.substring(j, j + endMark.length()).equals(endMark)) {
          shouldSplit = true;
          break;
        }
      }

      if (!shouldSplit) {
        for (;i < size; i++)
          os.print(table.charAt(i));
      }

      // Create a new initializing sub-routines.
      os.println("    };");
      os.println("    System.arraycopy(table, 0, matcherTable, index, table.length);");
      os.println("    index += table.length;");
      os.println("  }");

      // Checks if it is needed to create a new initializing functions.
      if (i < size) {
        os.printf("  private static void initTable%d(int[] matcherTable) {\n", index++);
        os.println("    int[] table = new int[]{");
      }
    }
    os.printf("  // Matcher Table size is %d bytes\n", totalSize);
  }

  public static void emitMatcherTable(Matcher matcher,
                                      CodeGenDAGPatterns cdp,
                                      PrintStream os) {
    // emit the matcher table into separate different static methods
    // to statically initialize the matcher table invoked by constructor.
    String targetName = cdp.getTarget().getName();
    os.printf("package backend.target.%s;%n%n", targetName.toLowerCase());

    os.println("import backend.codegen.MVT;");
    os.println("import backend.codegen.dagisel.*;");
    os.println("import backend.target.TargetMachine;");
    os.println("import backend.codegen.dagisel.SDNode;");
    os.println("import backend.target.TargetMachine.CodeModel;");
    os.println("import backend.target.TargetMachine.RelocModel;");
    os.println("import backend.target.TargetOpcodes;");
    os.println("import backend.type.PointerType;");
    os.println("import backend.value.Value;");
    os.println("import tools.Util;");
    os.println();
    os.println("import static backend.codegen.dagisel.SDNode.*;");
    os.println();
    os.println("import java.util.ArrayList;");
    os.println("import java.util.Arrays;");
    os.println();

    String className = targetName + "GenDAGISel";
    os.printf("public final class %s extends %sDAGISel {%n",
        className, targetName);

    os.println("  private static int index = 0;");

    DAGISelMatcherEmitter emitter = new DAGISelMatcherEmitter(cdp.getTarget().getName());
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    FormattedOutputStream fos = new FormattedOutputStream(baos);
    int totalSize = emitter.emitMatcherList(matcher, 5, 0, fos);
    printMatcherTable(baos.toString(), totalSize, os);

    fos = new FormattedOutputStream(os);
    emitter.emitHistogram(matcher, fos);
    emitter.emitPredicateFunctions(cdp, fos);

    // emit constructor function.
    os.printf("\n  public %sGenDAGISel(%sTargetMachine tm, TargetMachine.CodeGenOpt optLevel) {%n",
        targetName, targetName);
    os.println("    super(tm, optLevel);");
    os.printf("    matcherTable = new int[%d];\n", totalSize);
    for (int i = 0; i < index; i++) {
      os.printf("    initTable%d(matcherTable);\n", i);
    }
    os.printf("    Util.assertion(index == %d, \"Inconsistency number of items in matcher table\");\n", totalSize);
    os.println("  }");
    os.println("}");
  }
}
