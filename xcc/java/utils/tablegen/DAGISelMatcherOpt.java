package utils.tablegen;
/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2020, Jianping Zeng.
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
import gnu.trove.map.hash.TIntIntHashMap;
import tools.OutRef;
import tools.Pair;
import tools.Util;

import java.util.ArrayList;
import java.util.HashSet;

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
    factorNodes(matcherRef);
    return matcherRef.get();
  }

  /**
   * Pattern predicates can be checked at any level of
   * the matching tree.  The generator dumps them at the top level of the pattern
   * though, which prevents factoring from being able to see past them.  This
   * optimization sinks them as far down into the pattern as possible.
   * <p>
   * Conceptually, we'd like to sink these predicates all the way to the last
   * matcher predicate in the series.  However, it turns out that some
   * ComplexPatterns have side effects on the graph, so we really don't want to
   * run a the complex pattern if the pattern predicate will fail.  For this
   * reason, we refuse to sink the pattern predicate past a ComplexPattern.
   *
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
   *
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
      contractNodes(matcherRef, cgp);
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

  /**
   * Turn the matchers like this:
   * <pre>
   * Scope
   *   OPC_CheckType i32
   *     ABC
   *   OPC_CheckType i32
   *     XYZ
   * into
   *   OPC_CheckType i32
   *     Scope
   *       ABC
   *       XZY
   * </pre>
   *
   * @param matcherRef
   */
  private static void factorNodes(OutRef<Matcher> matcherRef) {
    Matcher n = matcherRef.get();
    if (n == null) return;

    if (!(n instanceof Matcher.ScopeMatcher)) {
      OutRef<Matcher> nextRef = new OutRef<>(n.getNext());
      factorNodes(nextRef);
      if (nextRef.get() != n.getNext())
        n.setNext(nextRef.get());
      return;
    }

    Matcher.ScopeMatcher scope = (Matcher.ScopeMatcher) n;
    ArrayList<Matcher> optionsToMatch = new ArrayList<>();
    for (int i = 0, e = scope.getNumChildren(); i < e; i++) {
      OutRef<Matcher> child = new OutRef<>(scope.getChild(i));
      factorNodes(child);
      if (child.get() != null)
        optionsToMatch.add(child.get());
    }

    ArrayList<Matcher> newOptionsToMatch = new ArrayList<>();
    for (int optionIdx = 0, e = optionsToMatch.size(); optionIdx < e; ) {
      Matcher option = optionsToMatch.get(optionIdx);

      if (optionIdx == e) {
        newOptionsToMatch.add(option);
        continue;
      }

      ArrayList<Matcher> equalMatchers = new ArrayList<>();
      equalMatchers.add(option);

      while (optionIdx < e && optionsToMatch.get(optionIdx).isEqual(option))
        equalMatchers.add(optionsToMatch.get(optionIdx++));

      int scan = optionIdx;
      while (true) {
        if (scan == e) break;

        Matcher scanMatcher = optionsToMatch.get(scan);

        if (option.isEqual(scanMatcher)) {
          equalMatchers.add(scanMatcher);
          optionsToMatch.remove(scan);
          --e;
          continue;
        }

        if (option.isContradictoryImpl(scanMatcher)) {
          ++scan;
          continue;
        }

        if (option.isSimplePredicateOrRecordNode()) {
          Matcher m2 = findNodeWthKind(scanMatcher, option.getKind());
          if (m2 != null && m2 != scanMatcher &&
              m2.canMoveBefore(scanMatcher) &&
              (m2.isEqual(option) || m2.isContradictoryImpl(option))) {
            Matcher matcherWithoutM2 = scanMatcher.unlinkNode(m2);
            m2.setNext(matcherWithoutM2);
            optionsToMatch.set(scan, m2);
            continue;
          }
        }

        // otherwise, we don't know how to handle this entry, we have to hail.
        break;
      }

      if (scan != e &&
          // don't print it's obvious nothing extra could be merged anyway.
          scan + 1 != e) {
        if (Util.DEBUG) {
          System.err.println("Couldn't merge this:");
          option.print(System.err, 4);
          System.err.println("into this:");
          optionsToMatch.get(scan).print(System.err, 4);
          if (scan + 1 < e)
            optionsToMatch.get(scan + 1).printOne(System.err);
          if (scan + 2 < e)
            optionsToMatch.get(scan + 2).printOne(System.err);
          System.err.println();
        }
      }

      if (equalMatchers.size() == 1) {
        newOptionsToMatch.add(equalMatchers.get(0));
        continue;
      }

      Matcher shared = option;
      option = option.getNext();
      equalMatchers.set(0, option);

      for (int i = 1, sz = equalMatchers.size(); i < sz; i++) {
        Matcher tmp = equalMatchers.get(i).getNext();
        equalMatchers.set(i, tmp);
      }

      shared.setNext(new Matcher.ScopeMatcher(equalMatchers));

      OutRef<Matcher> nextRef = new OutRef<>(shared.getNext());
      if (nextRef.get() != shared.getNext())
        shared.setNext(nextRef.get());

      newOptionsToMatch.add(shared);
    }

    if (newOptionsToMatch.size() == 1) {
      matcherRef.set(newOptionsToMatch.get(0));
      return;
    }

    if (newOptionsToMatch.isEmpty()) {
      matcherRef.set(null);
      return;
    }

    boolean allOpcodeChecks = true, allTypeChecks = true;
    for (int i = 0, e = newOptionsToMatch.size(); i < e; i++) {
      if (allOpcodeChecks && !(newOptionsToMatch.get(i) instanceof Matcher.CheckOpcodeMatcher))
        allOpcodeChecks = false;

      if (allTypeChecks) {
        Matcher tmp = findNodeWthKind(newOptionsToMatch.get(i), MatcherKind.CheckType);
        if (!(tmp instanceof Matcher.CheckTypeMatcher) ||
            ((Matcher.CheckTypeMatcher) tmp).getType() == MVT.iPTR ||
            !tmp.canMoveBefore(newOptionsToMatch.get(i))) {
          allTypeChecks = false;
        }
      }
    }

    if (allOpcodeChecks) {
      HashSet<String> opcodes = new HashSet<>();
      ArrayList<Pair<SDNodeInfo, Matcher>> cases = new ArrayList<>();
      newOptionsToMatch.forEach(m-> {
        Matcher.CheckOpcodeMatcher com = (Matcher.CheckOpcodeMatcher) m;
        Util.assertion(opcodes.add(com.getOpcode().getEnumName()),
            "Duplicate opcodes not factored?");

        cases.add(Pair.get(com.getOpcode(), com.getNext()));
      });

      matcherRef.set(new Matcher.SwitchOpcodeMatcher(cases));
      return;
    }

    if (allTypeChecks) {
      TIntIntHashMap typeEntry = new TIntIntHashMap();
      ArrayList<Pair<Integer, Matcher>> cases = new ArrayList<>();
      for (Matcher tmp : newOptionsToMatch) {
        Matcher.CheckTypeMatcher ctm = (Matcher.CheckTypeMatcher) findNodeWthKind(tmp, MatcherKind.CheckType);
        Matcher matcherWithoutCTM = tmp.unlinkNode(ctm);
        int ctmTy = ctm.getType();

        if (typeEntry.containsKey(ctmTy)) {
          int entry = typeEntry.get(ctmTy);
          Matcher prevMatcher = cases.get(entry-1).second;
          if (prevMatcher instanceof Matcher.ScopeMatcher) {
            Matcher.ScopeMatcher sm = (Matcher.ScopeMatcher) prevMatcher;
            sm.setNumChildren(sm.getNumChildren()+1);
            sm.resetChild(sm.getNumChildren()-1, matcherWithoutCTM);
            continue;
          }

          cases.get(entry-1).second = new Matcher.ScopeMatcher(new Matcher[] {prevMatcher, matcherWithoutCTM});
          continue;
        }

        typeEntry.put(ctmTy, cases.size() + 1);
        cases.add(Pair.get(ctmTy, matcherWithoutCTM));
      }

      if (cases.size() != 1) {
        matcherRef.set(new Matcher.SwitchTypeMatcher(cases));
      }
      else {
        matcherRef.set(new Matcher.CheckTypeMatcher(cases.get(0).first, 0));
        matcherRef.get().setNext(cases.get(0).second);
      }
      return;
    }

    scope.setNumChildren(newOptionsToMatch.size());
    for (int i = 0, e = newOptionsToMatch.size(); i < e; i++)
      scope.resetChild(i, newOptionsToMatch.get(i));



  }

  /**
   * Scan a series of matchers looking for a matcher with a
   * specified kind.  Return null if we didn't find one otherwise return the
   * matcher.
   * @param matcher
   * @param kind
   * @return
   */
  private static Matcher findNodeWthKind(Matcher matcher, MatcherKind kind) {
    for (; matcher != null; matcher = matcher.getNext())
      if (matcher.getKind() == kind)
        return matcher;
    return null;
  }
}
