package utils.tablegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the namespace of the <organization> nor the
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
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.Error;
import tools.Pair;
import tools.Util;
import utils.tablegen.Init.IntInit;

import java.util.ArrayList;

import static utils.tablegen.Matcher.*;
import static utils.tablegen.SDNP.*;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class DAGISelMatcherGen {
  private PatternToMatch pattern;
  private CodeGenDAGPatterns cgp;
  private TreePatternNode patWithNoTypes;
  private TObjectIntHashMap<String> variableMap;
  private int nextRecordedOperandNo;
  private TIntArrayList matchedChainNodes;
  private TIntArrayList matchedFlagResultNodes;
  private ArrayList<Pair<TreePatternNode, Integer>> matchedComplexPatterns;
  private ArrayList<Pair<Record, Integer>> physRegInputs;
  private Matcher theMatcher;
  private Matcher curPredicate;

  public DAGISelMatcherGen(PatternToMatch pattern, CodeGenDAGPatterns cgp) {
    this.pattern = pattern;
    this.cgp = cgp;
    nextRecordedOperandNo = 0;
    theMatcher = null;
    curPredicate = null;
    variableMap = new TObjectIntHashMap<>();
    matchedChainNodes = new TIntArrayList();
    matchedFlagResultNodes = new TIntArrayList();
    matchedComplexPatterns = new ArrayList<>();
    physRegInputs = new ArrayList<>();

    patWithNoTypes = pattern.getSrcPattern().clone();
    patWithNoTypes.removeTypes();
    inferPossibleTypes();
  }

  static int getRegisterValueType(Record r, CodeGenTarget target) {
    boolean foundRC = false;
    int vt = MVT.Other;
    ArrayList<CodeGenRegisterClass> rcs = target.getRegisterClasses();
    for (CodeGenRegisterClass rc : rcs) {
      if (rc.contains(r)) {
        if (!foundRC) {
          foundRC = true;
          ValueTypeByHwMode vvt = rc.getValueTypeAt(0);
          if (vvt.isSimple())
            vt = vvt.getSimple().simpleVT;
          else
            continue;
        }
      }
    }
    return vt;
  }

  public static Matcher convertPatternToMatcher(PatternToMatch tp,
                                                int variant,
                                                CodeGenDAGPatterns cdp) {
    DAGISelMatcherGen gen = new DAGISelMatcherGen(tp, cdp);
    // generate the code for the matcher.
    if (gen.emitMatcherCode(variant))
      return null;

    gen.emitResultCode();
    return gen.getMatcher();
  }

  private void inferPossibleTypes() {
    inferPossibleTypes(false);
  }

  private void inferPossibleTypes(boolean forceMode) {
    TreePattern tp = cgp.getPatternFragments().values().iterator().next();
    tp.getTypeInfer().codeGen = true;
    tp.getTypeInfer().forceMode = forceMode;
    boolean madeChange = true;
    while (madeChange)
      // set the ignore reg constraints as true.
      madeChange = patWithNoTypes.applyTypeConstraints(tp, true);
  }

  /**
   * Generate the code that matches the predicate of this pattern for
   * the specified variant. If the variant is invalid this returns
   * true and does not generate code. if ti is valid, it returns false.
   *
   * @param variant
   * @return
   */
  public boolean emitMatcherCode(int variant) {
    // If the root of the pattern is a ComplexPattern and if it is specified to
    // match some number of root opcodes, these are considered to be our variants.
    // Depending on which variant we're generating code for, emit the root opcode
    // check.
    ComplexPattern cp = pattern.getSrcPattern().getComplexPatternInfo(cgp);
    if (cp != null) {
      ArrayList<Record> opNodes = cp.getRootNodes();
      Util.assertion(!opNodes.isEmpty(), "Complex pattern must specify what it can match");
      if (variant >= opNodes.size()) return true;

      addMatcher(new CheckOpcodeMatcher(cgp.getSDNodeInfo(opNodes.get(variant))));
    } else if (variant != 0) return true;

    // emit the matcher for the pattern structure and types.
    emitMatchCode(pattern.getSrcPattern(), patWithNoTypes, pattern.forceMode);

    // if the pattern has a predicate on it (e.g. only enabled when a substarget
    // feature is around, do the check)
    if (!pattern.getPredicateCheck().isEmpty())
      addMatcher(new CheckPatternPredicateMatcher(pattern.getPredicateCheck()));

    // Now that we've completed the structural type match, emit any ComplexPattern
    // checks (e.g. addrmode matches). We emit this after the structural match
    // because they are generally more expensive to evaluate and more difficult to
    // factor.
    for (Pair<TreePatternNode, Integer> itr : matchedComplexPatterns) {
      TreePatternNode n = itr.first;
      itr.second = nextRecordedOperandNo;

      Util.assertion(variableMap.containsKey(n.getName()));
      int recNodeEntry = variableMap.get(n.getName());
      Util.assertion(recNodeEntry != 0, "Complex Pattern should have a namespace and slot");
      --recNodeEntry;

      cp = cgp.getComplexPattern(((Init.DefInit) n.getLeafValue()).getDef());
      addMatcher(new CheckComplexPatMatcher(cp, recNodeEntry, n.getName(),
          nextRecordedOperandNo));

      nextRecordedOperandNo += cp.getNumOperands();
      if (cp.hasProperty(SDNPHasChain)) {
        ++nextRecordedOperandNo;
        Util.assertion(nextRecordedOperandNo > 1,
            "Should have recorded input/result chains at least!");
        matchedChainNodes.add(nextRecordedOperandNo - 1);
      }
    }
    return false;
  }

  public void emitResultCode() {
    // Patterns that match nodes with (potentially multiple) chain inputs have to
    // merge them together into a token factor.  This informs the generated code
    // what all the chained nodes are.
    if (!matchedChainNodes.isEmpty())
      addMatcher(new EmitMergeInputChainsMatcher(matchedChainNodes.toArray()));

    // codegen the root of the result pattern, capturing the result values.
    TIntArrayList ops = new TIntArrayList();
    emitResultOperand(pattern.getDstPattern(), ops);

    // If the matched pattern covers nodes which define a flag result, emit a node
    // that tells the matcher about them so that it can update their results.
    if (!matchedFlagResultNodes.isEmpty())
      addMatcher(new MarkFlagResultsMatcher(matchedFlagResultNodes.toArray()));

    addMatcher(new CompleteMatchMatcher(ops.toArray(), pattern));
  }

  public Matcher getMatcher() {
    return theMatcher;
  }

  public Matcher getCurPredicate() {
    return curPredicate;
  }

  private void addMatcher(Matcher newMode) {
    if (curPredicate != null)
      curPredicate.setNext(newMode);
    else
      theMatcher = newMode;
    curPredicate = newMode;
  }

  private void emitMatchCode(TreePatternNode n, TreePatternNode nodeNoTypes,
                             boolean forceMode) {
    TIntArrayList resultsToTypeCheck = new TIntArrayList();
    for (int i = 0, e = nodeNoTypes.getNumTypes(); i < e; i++) {
      if (nodeNoTypes.getExtType(i).equals(n.getExtType(i)))
        continue;

      inferPossibleTypes(forceMode);
      resultsToTypeCheck.add(i);
    }

    if (!n.getName().isEmpty()) {
      if (!variableMap.containsKey(n.getName())) {
        addMatcher(new RecordMatcher("$" + n.getName(), nextRecordedOperandNo));
        variableMap.put(n.getName(), ++nextRecordedOperandNo);
      } else {
        addMatcher(new CheckSameMatcher(variableMap.get(n.getName()) - 1));
        return;
      }
    }

    if (n.isLeaf())
      emitLeafMatchNode(n);
    else
      emitOperatorMatchNode(n, nodeNoTypes, forceMode);

    // If there are node predicates for this node, generate their checks.
    n.getPredicateFns().forEach(pred -> addMatcher(new CheckPredicateMatcher(pred)));
    resultsToTypeCheck.forEach(vt -> {
      addMatcher(new CheckTypeMatcher(n.getSimpleType(vt), vt));
      return true;
    });
  }

  private void emitLeafMatchNode(TreePatternNode n) {
    Util.assertion(n.isLeaf(), "Not a leaf?");
    if (n.getLeafValue() instanceof IntInit) {
      IntInit ii = (IntInit) n.getLeafValue();
      if (n == pattern.getSrcPattern()) {
        SDNodeInfo info = cgp.getSDNodeInfo(cgp.getSDNodeNamed("imm"));
        addMatcher(new CheckOpcodeMatcher(info));
      }
      addMatcher(new CheckIntegerMatcher(ii.getValue()));
      return;
    }

    if (!(n.getLeafValue() instanceof Init.DefInit)) {
      System.err.print("Unknown leaf kind: ");
      n.dump();
      System.err.println();
      System.exit(-1);
    }

    Init.DefInit di = (Init.DefInit) n.getLeafValue();
    Record def = di.getDef();
    if (def.isSubClassOf("RegisterClass") ||
        def.isSubClassOf("PointerLikeRegClass") ||
        def.getName().equals("srcvalue"))
      return;

    if (def.isSubClassOf("Register")) {
      addMatcher(new RecordMatcher(String.format("physreg input %s",
          def.getName()), nextRecordedOperandNo));
      physRegInputs.add(Pair.get(def, nextRecordedOperandNo++));
      return;
    }

    if (def.isSubClassOf("ValueType")) {
      addMatcher(new CheckValueTypeMatcher(def.getName()));
      return;
    }
    if (def.isSubClassOf("CondCode")) {
      addMatcher(new CheckCondCodeMatcher(def.getName()));
      return;
    }

    if (def.isSubClassOf("ComplexPattern")) {
      if (n.getName().isEmpty()) {
        System.err.print("We expect complex pattern has a name: ");
        n.dump();
        System.err.println();
        System.exit(-1);
      }

      matchedComplexPatterns.add(Pair.get(n, 0));
      return;
    }

    System.err.print("Unknown leaf kind: ");
    n.dump();
    System.err.println();
    System.exit(-1);
  }

  private void emitOperatorMatchNode(TreePatternNode n,
                                     TreePatternNode nodeNoTypes,
                                     boolean forceMode) {
    Util.assertion(!n.isLeaf(), "Not an operator!");
    SDNodeInfo cInfo = cgp.getSDNodeInfo(n.getOperator());

    // If this is an 'and R, 1234' where the operation is AND/OR and the RHS is
    // a constant without a predicate fn that has more that one bit set, handle
    // this as a special case.  This is usually for targets that have special
    // handling of certain large constants (e.g. alpha with it's 8/16/32-bit
    // handling stuff).  Using these instructions is often far more efficient
    // than materializing the constant.  Unfortunately, both the instcombiner
    // and the dag combiner can often infer that bits are dead, and thus drop
    // them from the mask in the dag.  For example, it might turn 'AND X, 255'
    // into 'AND X, 254' if it knows the low bit is set.  Emit code that checks
    // to handle this.
    if ((n.getOperator().getName().equals("and") ||
        n.getOperator().getName().equals("or")) &&
        n.getChild(1).isLeaf() && n.getChild(1).getPredicateFns().isEmpty() &&
        n.getPredicateFns().isEmpty()) {
      if (n.getChild(1).getLeafValue() instanceof IntInit) {
        IntInit ii = (IntInit) n.getChild(1).getLeafValue();
        if (!Util.isPowerOf2(ii.getValue())) {
          if (n == pattern.getSrcPattern())
            addMatcher(new CheckOpcodeMatcher(cInfo));

          if (n.getOperator().getName().equals("and"))
            addMatcher(new CheckAndImmMatcher(ii.getValue()));
          else
            addMatcher(new CheckOrImmMatcher(ii.getValue()));

          addMatcher(new MoveChildMatcher(0));
          emitMatchCode(n.getChild(0), nodeNoTypes.getChild(0), forceMode);
          addMatcher(new MoveParentMatcher());
          return;
        }
      }
    }

    addMatcher(new CheckOpcodeMatcher(cInfo));

    // Tell the interpreter to capture the memory reference if the node
    // is a load or store.
    if (n.hasProperty(SDNPMemOperand, cgp))
      addMatcher(new RecordMemRefMatcher());

    int opNo = 0;
    if (n.hasProperty(SDNPHasChain, cgp)) {
      addMatcher(new RecordMatcher("'" + n.getOperator().getName() + "' chained node",
          nextRecordedOperandNo));
      matchedChainNodes.add(nextRecordedOperandNo++);

      opNo = 1;

      TreePatternNode root = pattern.getSrcPattern();
      if (n != root) {
        boolean needCheck = !root.hasChild(n);

        if (!needCheck) {
          SDNodeInfo info = cgp.getSDNodeInfo(root.getOperator());
          needCheck = root.getOperator().equals(cgp.getIntrinsicVoidSDNode()) ||
              root.getOperator().equals(cgp.getIntrinsicWChainSDNode()) ||
              root.getOperator().equals(cgp.getIntrinsicWOChainSDNode()) ||
              info.getNumOperands() > 1 ||
              info.hasProperty(SDNPHasChain) ||
              info.hasProperty(SDNPInFlag) ||
              info.hasProperty(SDNPOptInFlag);
        }
        if (needCheck)
          addMatcher(new CheckFoldableChainNodeMatcher());
      }
    }

    if (n.hasProperty(SDNPOutFlag, cgp) && n != pattern.getSrcPattern()) {
      addMatcher(new RecordMatcher(String.format("\'%s\' flag output node",
          n.getOperator().getName()), nextRecordedOperandNo));
      matchedFlagResultNodes.add(nextRecordedOperandNo++);
    }

    if (n.hasProperty(SDNPOptInFlag, cgp) ||
        n.hasProperty(SDNPInFlag, cgp)) {
      addMatcher(new CaptureFlagInputMatcher());
    }

    for (int i = 0, e = n.getNumChildren(); i < e; i++, ++opNo) {
      addMatcher(new MoveChildMatcher(opNo));
      emitMatchCode(n.getChild(i), nodeNoTypes.getChild(i), forceMode);
      addMatcher(new MoveParentMatcher());
    }
  }

  private int getNamedArgumentSlot(String name) {
    Util.assertion(variableMap.containsKey(name),
        "Variable referenced but not defined and not caught earlier!");
    return variableMap.get(name) - 1;
  }

  private void emitResultOperand(TreePatternNode n, TIntArrayList resultOps) {
    if (!n.getName().isEmpty())
      emitResultOfNamedOperand(n, resultOps);
    else if (n.isLeaf())
      emitResultLeafAsOperand(n, resultOps);
    else {
      Record rec = n.getOperator();
      if (rec.isSubClassOf("Instruction"))
        emitResultInstructionAsOperand(n, resultOps);
      else if (rec.isSubClassOf("SDNodeXForm"))
        emitResultSDNodeXFormAsOperand(n, resultOps);
      else {
        System.err.printf("Unknown result node to emit code for : ");
        n.dump();
        System.err.println();
        System.exit(-1);
        Error.printFatalError("Uknown node in result pattern!");
      }
    }
  }

  private void emitResultOfNamedOperand(TreePatternNode n, TIntArrayList resultOps) {
    Util.assertion(!n.getName().isEmpty(), "Operand not named!");
    ComplexPattern cp = n.getComplexPatternInfo(cgp);
    if (cp != null) {
      int slotNo = 0;
      for (Pair<TreePatternNode, Integer> itr : matchedComplexPatterns) {
        if (itr.first.getName().equals(n.getName())) {
          slotNo = itr.second;
          break;
        }
      }
      Util.assertion(slotNo != 0, "didn't get a slot number assigned");
      for (int i = 0, e = cp.getNumOperands(); i < e; i++)
        resultOps.add(slotNo + i);
      return;
    }

    int slotNo = getNamedArgumentSlot(n.getName());
    if (!n.isLeaf()) {
      String operatorName = n.getOperator().getName();
      if (operatorName.equals("imm") || operatorName.equals("fpimm")) {
        addMatcher(new EmitConvertToTargetMatcher(slotNo));
        resultOps.add(nextRecordedOperandNo++);
        return;
      }
    }
    resultOps.add(slotNo);
  }

  private void emitResultLeafAsOperand(TreePatternNode n, TIntArrayList resultOps) {
    Util.assertion(n.isLeaf(), "Must be a leaf");
    if (n.getLeafValue() instanceof IntInit) {
      IntInit ii = (IntInit) n.getLeafValue();
      addMatcher(new EmitIntegerMatcher(ii.getValue(), n.getSimpleType(0)));
      resultOps.add(nextRecordedOperandNo++);
      return;
    }

    if (n.getLeafValue() instanceof Init.DefInit) {
      Init.DefInit di = (Init.DefInit) n.getLeafValue();
      Record rec = di.getDef();
      if (rec.isSubClassOf("Register")) {
        addMatcher(new EmitRegisterMatcher(rec, n.getSimpleType(0)));
        resultOps.add(nextRecordedOperandNo++);
        return;
      }

      if (rec.getName().equals("zero_reg")) {
        addMatcher(new EmitRegisterMatcher(null, n.getSimpleType(0)));
        resultOps.add(nextRecordedOperandNo++);
        return;
      }

      if (rec.isSubClassOf("RegisterOperand"))
        rec = rec.getValueAsDef("RegClass");
      if (rec.isSubClassOf("RegisterClass")) {
        String value = rec.getName() + "RegClassID";
        addMatcher(new EmitStringIntegerMatcher(value, MVT.i32));
        resultOps.add(nextRecordedOperandNo++);
        return;
      }

      if (rec.isSubClassOf("SubRegIndex")) {
        String value = rec.getName();
        addMatcher(new EmitStringIntegerMatcher(value, MVT.i32));
        resultOps.add(nextRecordedOperandNo++);
        return;
      }
    }
    System.err.println("unhandled leaf node: ");
    n.dump();
  }

  private void emitResultInstructionAsOperand(TreePatternNode n, TIntArrayList resultOps) {
    Record op = n.getOperator();
    CodeGenTarget cgt = cgp.getTarget();
    CodeGenInstruction ii = cgt.getInstruction(op.getName());
    DAGInstruction inst = cgp.getInstruction(op);

    boolean isRoot = n == pattern.getDstPattern();
    boolean treeHasInFlag = false, treeHasOutFlag = false;
    if (isRoot) {
      TreePatternNode srcPat = pattern.getSrcPattern();
      treeHasInFlag = srcPat.hasProperty(SDNPOptInFlag, cgp) ||
          srcPat.hasProperty(SDNPInFlag, cgp);

      treeHasOutFlag = srcPat.hasProperty(SDNPOutFlag, cgp);
    }

    // NumResults - This is the number of results produced by the instruction in
    // the "outs" list.
    int numResults = inst.getNumResults();
    TIntArrayList instOps = new TIntArrayList();

    // Loop over all of the operands of the instruction pattern, emitting code
    // to fill them all in.  The node 'N' usually has number children equal to
    // the number of input operands of the instruction.  However, in cases
    // where there are predicate operands for an instruction, we need to fill
    // in the 'execute always' values.  Match up the node operands to the
    // instruction operands to do this.
    int childNo = 0;
    for (int instOpNo = numResults, e = ii.operandList.size();
         instOpNo < e; ++instOpNo) {
      Record operandNode = ii.operandList.get(instOpNo).rec;

      // This is a predicate or optional def operand; emit the
      // 'default ops' operands.
      if ((operandNode.isSubClassOf("PredicateOperand") ||
          operandNode.isSubClassOf("OptionalDefOperand")) &&
          !cgp.getDefaultOperand(operandNode).defaultOps.isEmpty()) {
        DAGDefaultOperand defaultOp =
            cgp.getDefaultOperand(ii.operandList.get(instOpNo).rec);
        for (int i = 0, sz = defaultOp.defaultOps.size(); i < sz; i++)
          emitResultOperand(defaultOp.defaultOps.get(i), instOps);
        continue;
      }

      // Otherwise this is a normal operand or a predicate operand without
      // 'execute always'; emit it.
      emitResultOperand(n.getChild(childNo++), instOps);
    }

    if (ii.isVariadic) {
      for (int i = childNo, e = n.getNumChildren(); i < e; i++)
        emitResultOperand(n.getChild(i), instOps);
    }

    if (isRoot && !physRegInputs.isEmpty()) {
      for (int i = 0, e = physRegInputs.size(); i < e; i++)
        addMatcher(new EmitCopyToRegMatcher(physRegInputs.get(i).second,
            physRegInputs.get(i).first));

      treeHasInFlag = true;
    }

    TIntArrayList resultVTs = new TIntArrayList();
    for (int i = 0, e = n.getNumTypes(); i < e; i++)
      resultVTs.add(n.getSimpleType(i));

    // If this is the root instruction of a pattern that has physical registers in
    // its result pattern, add output VTs for them.  For example, X86 has:
    //   (set AL, (mul ...))
    // This also handles implicit results like:
    //   (implicit EFLAGS)
    if (isRoot && !pattern.getDstRegs().isEmpty()) {
      Record handledReg = null;
      if (ii.hasOneImplicitDefWithKnownVT(cgt) != MVT.Other)
        handledReg = ii.implicitDefs.get(0);

      for (Record reg : pattern.getDstRegs()) {
        if (!reg.isSubClassOf("Register") || reg == handledReg) continue;
        resultVTs.add(getRegisterValueType(reg, cgt));
      }
    }

    int numFixedArityOperands = -1;
    if (isRoot && pattern.getSrcPattern().hasProperty(SDNPVariadic, cgp))
      numFixedArityOperands = pattern.getSrcPattern().getNumChildren();

    // If this is the root node and any of the nodes matched nodes in the input
    // pattern have MemRefs in them, have the interpreter collect them and plop
    // them onto this node.
    //
    // FIXME3: This is actively incorrect for result patterns where the root of
    // the pattern is not the memory reference and is also incorrect when the
    // result pattern has multiple memory-referencing instructions.  For example,
    // in the X86 backend, this pattern causes the memrefs to get attached to the
    // CVTSS2SDrr instead of the MOVSSrm:
    //
    //  def : Pat<(extloadf32 addr:$src),
    //            (CVTSS2SDrr (MOVSSrm addr:$src))>;
    //
    boolean nodeHasMemRefs = isRoot && pattern.getSrcPattern().hasProperty(SDNPMemOperand, cgp);

    boolean nodeHasChain = false;
    if (pattern.getSrcPattern().hasProperty(SDNPHasChain, cgp)) {
      nodeHasChain = isRoot;

      if (ii.hasCtrlDep || ii.mayLoad || ii.mayStore || ii.canFoldAsLoad ||
          ii.hasSideEffects)
        nodeHasChain = true;
    }
    addMatcher(new EmitNodeMatcher(ii.namespace + "." + ii.theDef.getName(),
        resultVTs.toArray(), instOps.toArray(),
        nodeHasChain, treeHasInFlag,
        treeHasOutFlag,
        nodeHasMemRefs, numFixedArityOperands,
        nextRecordedOperandNo));

    // the non-chain and non-flag results of the newly emitted node get recorded.
    for (int i = 0, e = resultVTs.size(); i < e; i++) {
      int vt = resultVTs.get(i);
      if (vt == MVT.Other || vt == MVT.Glue) break;
      resultOps.add(nextRecordedOperandNo++);
    }
  }

  private void emitResultSDNodeXFormAsOperand(TreePatternNode n, TIntArrayList resultOps) {
    Util.assertion(n.getOperator().isSubClassOf("SDNodeXForm"), "Not SDNodeXForm");
    TIntArrayList inputOps = new TIntArrayList();
    Util.assertion(n.getNumChildren() == 1);
    emitResultOperand(n.getChild(0), inputOps);

    Util.assertion(inputOps.size() == 1);
    addMatcher(new EmitNodeXFromMatcher(inputOps.get(0), n.getOperator()));
    resultOps.add(nextRecordedOperandNo++);
  }
}

