package utils.tablegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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

import tools.OutRef;

import static utils.tablegen.SDNP.SDNPHasChain;
import static utils.tablegen.SDNP.SDNPOutFlag;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
class DAGISelMatcherOpt {

  static Matcher optimizeMatcher(Matcher theMatcher,
                                 CodeGenDAGPatterns cgp) {
    OutRef<Matcher> matcherRef = new OutRef<>(theMatcher);
    contractNodes(matcherRef, cgp);
    sinkPatternPredicates(matcherRef);
    return matcherRef.get();
  }

  /**
   * Pattern predicates can be checked at any level of
   * the matching tree.  The generator dumps them at the top level of the pattern
   * though, which prevents factoring from being able to see past them.  This
   * optimization sinks them as far down into the pattern as possible.
   *
   * Conceptually, we'd like to sink these predicates all the way to the last
   * matcher predicate in the series.  However, it turns out that some
   * ComplexPatterns have side effects on the graph, so we really don't want to
   * run a the complex pattern if the pattern predicate will fail.  For this
   * reason, we refuse to sink the pattern predicate past a ComplexPattern.
   * @param matcherRef
   */
  private static void sinkPatternPredicates(OutRef<Matcher> matcherRef) {
    Matcher n = matcherRef.get();
    if (n == null) return;

    // walk down all members of a scope node.
    if (n instanceof Matcher.ScopeMatcher) {
      Matcher.ScopeMatcher scope = (Matcher.ScopeMatcher) n;
      for (int i = 0, e = scope.getNumChildren(); i < e; i++) {
        OutRef<Matcher> child = new OutRef<>(scope.getChild(i));
        sinkPatternPredicates(child);
        scope.resetChild(i, child.get());
      }
      return;
    }

    if (!(n instanceof Matcher.CheckPatternPredicateMatcher)) {
      OutRef<Matcher> ref = new OutRef<>(n.getNext());
      sinkPatternPredicates(ref);
      if (ref.get() != n.getNext())
        n.setNext(ref.get());
      return;
    }

    Matcher.CheckPatternPredicateMatcher cppm = (Matcher.CheckPatternPredicateMatcher) n;
    if (!cppm.getNext().isSafeToRecorderWithPatternPredicate())
      return;

    matcherRef.set(cppm.getNext());
    n = matcherRef.get();
    while (n.getNext().isSafeToRecorderWithPatternPredicate())
      n = n.getNext();

    cppm.setNext(n.getNext());
    n.setNext(cppm);
  }

  /**
   * Turn multiple matcher node patterns like 'MoveChild+Record'
   * into single compound nodes like RecordChild.
   * @param matcherRef
   * @param cgp
   */
  private static void contractNodes(OutRef<Matcher> matcherRef,
                                    CodeGenDAGPatterns cgp) {
    Matcher n = matcherRef.get();
    if (n == null)
      return;

    // if we have a scope node, walk down all child node.
    if (n instanceof Matcher.ScopeMatcher) {
      Matcher.ScopeMatcher scope = (Matcher.ScopeMatcher) n;
      for (int i = 0, e = scope.getNumChildren(); i < e; i++) {
        OutRef<Matcher> child = new OutRef<>(scope.getChild(i));
        contractNodes(child, cgp);
        scope.resetChild(i, child.get());
      }
      return;
    }

    // If we found a movechild node with a node that comes in a 'foochild' form,
    // transform it.
    /*if (n instanceof Matcher.MoveChildMatcher) {
      Matcher.MoveChildMatcher mc = (Matcher.MoveChildMatcher) n;
      Matcher newNode = null;
      if (mc.getNext() instanceof Matcher.RecordMatcher) {
        Matcher.RecordMatcher rm = (Matcher.RecordMatcher) mc.getNext();
        newNode = new Matcher.RecordChildMatcher(mc.getChildNo(), rm.getWhatFor(),
            rm.getResultNo());
      }
      if (mc.getNext() instanceof Matcher.CheckTypeMatcher) {
        Matcher.CheckTypeMatcher cm = (Matcher.CheckTypeMatcher) mc.getNext();
        newNode = new Matcher.CheckTypeMatcher(mc.getChildNo(), cm.getType());
      }
      if (newNode != null) {
        // insert the new node
        newNode.setNext(n);
        matcherRef.set(newNode);
        mc.setNext(mc.getNext().getNext());
        contractNodes(matcherRef, cgp);
        return;
      }
    }*/

    // Zap movechild -> moveparent
    if (n instanceof Matcher.MoveChildMatcher &&
        n.getNext() instanceof Matcher.MoveParentMatcher) {
      matcherRef.set(n.getNext().getNext());
      contractNodes(matcherRef, cgp);
      return;
    }

    // Turn EmitNode->MarkFlagResults->CompleteMatch into
    // MarkFlagResults->EmitNode->CompleteMatch when we can to encourage
    // MorphNodeTo formation.  This is safe because MarkFlagResults never refers
    // to the root of the pattern.
    if (n instanceof Matcher.EmitNodeMatcher &&
        n.getNext() instanceof Matcher.MarkFlagResultsMatcher &&
        n.getNext().getNext() instanceof Matcher.CompleteMatchMatcher) {
      Matcher emitNode = matcherRef.get();
      Matcher mfr = emitNode.getNext();
      Matcher tail = mfr.getNext();

      // relink them.
      matcherRef.set(mfr);
      emitNode.setNext(tail);
      mfr.setNext(emitNode);
      contractNodes(matcherRef,cgp);
      return;
    }

    // Turn EmitNode->CompleteMatch into MorphNodeTo if we can.
    if (n instanceof Matcher.EmitNodeMatcher &&
        n.getNext() instanceof Matcher.CompleteMatchMatcher) {
      Matcher.EmitNodeMatcher emitNode = (Matcher.EmitNodeMatcher) n;
      Matcher.CompleteMatchMatcher cmm = (Matcher.CompleteMatchMatcher) n.getNext();
      int rootResultFirst = emitNode.getFirstResultSlot();
      boolean resultMatch = true;
      for (int i = 0, e = cmm.getNumResults(); i < e; i++)
        if (cmm.getResult(i) != rootResultFirst)
          resultMatch = false;

      // If the selected node defines a subset of the flag/chain results, we
      // can't use MorphNodeTo.  For example, we can't use MorphNodeTo if the
      // matched pattern has a chain but the root node doesn't.
      PatternToMatch pattern = cmm.getPattern();
      if (!emitNode.isHasChain() &&
          pattern.getSrcPattern().hasProperty(SDNPHasChain, cgp))
        resultMatch = false;

      // If the matched node has a flag and the output root doesn't, we can't
      // use MorphNodeTo.
      //
      // NOTE: Strictly speaking, we don't have to check for the flag here
      // because the code in the pattern generator doesn't handle it right.  We
      // do it anyway for thoroughness.
      if (!emitNode.isHasOutFlag() &&
          pattern.getSrcPattern().hasProperty(SDNPOutFlag, cgp))
        resultMatch = false;

      if (resultMatch) {
        int[] vts = emitNode.getVtList();
        int[] operands = emitNode.getOperands();
        matcherRef.set(new Matcher.MorphNodeToMatcher(emitNode.getOpcodeName(),
            vts, operands, emitNode.isHasChain(),
            emitNode.isHasInFlag(),
            emitNode.isHasOutFlag(),
            emitNode.isHasMemRefs(),
            emitNode.getNumFixedArityOperands(),
            pattern));

        matcherRef.get().setNext(cmm.getNext());
        return;
      }
    }

    OutRef<Matcher> nextPtr = new OutRef<>(n.getNext());
    contractNodes(nextPtr, cgp);
    if (n.getNext() != nextPtr.get()) {
      n.setNext(nextPtr.get());
    }

    // If we have a CheckType/CheckChildType/Record node followed by a
    // CheckOpcode, invert the two nodes.  We prefer to do structural checks
    // before type checks, as this opens opportunities for factoring on targets
    // like X86 where many operations are valid on multiple types.
    if ((n instanceof Matcher.CheckTypeMatcher ||
        n instanceof Matcher.CheckChildTypeMatcher ||
        n instanceof Matcher.RecordMatcher) &&
        n.getNext() instanceof Matcher.CheckOpcodeMatcher) {
      Matcher checkType = n;
      Matcher checkOpcode = checkType.getNext();
      Matcher tail = checkOpcode.getNext();

      matcherRef.set(checkOpcode);
      checkType.setNext(tail);
      checkOpcode.setNext(checkType);
      contractNodes(matcherRef, cgp);
    }
  }
}
