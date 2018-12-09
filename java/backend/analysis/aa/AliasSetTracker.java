/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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

import backend.support.CallSite;
import backend.value.BasicBlock;
import backend.value.Instruction;
import backend.value.Instruction.CallInst;
import backend.value.Instruction.LoadInst;
import backend.value.Instruction.StoreInst;
import backend.value.Value;
import tools.OutRef;
import tools.Pair;
import tools.Util;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import static backend.analysis.aa.AliasResult.NoAlias;
import static backend.analysis.aa.AliasSetTracker.AliasSet.AccessType.*;
import static backend.analysis.aa.AliasSetTracker.AliasSet.AliasType.MayAlias;
import static backend.analysis.aa.AliasSetTracker.AliasSet.AliasType.MustAlias;

/**
 * These interface are used to classify a collection of pointer references
 * into a maximal number of disjoint sets.  Each {@linkplain AliasSet}
 * object constructed by the {@linkplain AliasSetTracker} object refers
 * to memory disjoint from the other sets.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public final class AliasSetTracker {
  private AliasAnalysis aa;
  private ArrayList<AliasSet> aliasSets;
  private HashMap<Pair<Value, AliasSetTracker>, AliasSet.PointerRec> pointerMap;

  public AliasSetTracker(AliasAnalysis aa) {
    this.aa = aa;
    aliasSets = new ArrayList<>();
    pointerMap = new HashMap<>();
  }

  public AliasAnalysis getAliasAnalysis() {
    return aa;
  }

  public ArrayList<AliasSet> getAliasSets() {
    return aliasSets;
  }

  /**
   * add methods - These methods are used to add different types of
   * instructions to the alias sets.  Adding a new instruction can result in
   * one of three actions happening:
   *
   *   1. If the instruction doesn't alias any other sets, create a new set.
   *   2. If the instruction aliases exactly one set, add it to the set
   *   3. If the instruction aliases multiple sets, merge the sets, and add
   *      the instruction to the result.
   *
   * These methods return true if inserting the instruction resulted in the
   * addition of a new alias set (i.e., the pointer did not alias anything).
   */

  /**
   * Add a memory location into a AliasSet as appropriate.
   *
   * @param ptr
   * @param size
   * @return
   */
  public boolean add(Value ptr, int size) {
    OutRef<Boolean> res = new OutRef<>(false);
    addPointer(ptr, size, NoModRef, res);
    return res.get();
  }

  public boolean add(LoadInst li) {
    OutRef<Boolean> res = new OutRef<>(false);
    AliasSet as = addPointer(li.getPointerOperand(), aa.getTypeStoreSize(li.getType()),
        Refs, res);
    if (li.isVolatile()) as.setVolatile(true);

    return res.get();
  }

  public boolean add(StoreInst st) {
    OutRef<Boolean> res = new OutRef<>(false);
    Value val = st.operand(0);
    AliasSet as = addPointer(st.operand(1), aa.getTypeStoreSize(val.getType()),
        Mods, res);
    if (st.isVolatile()) as.setVolatile(true);

    return res.get();
  }

  public boolean add(CallInst ci) {
    return add(new CallSite(ci));
  }

  public boolean add(CallSite cs) {
    if (aa.doesNotAccessMemory(cs))
      return true;
    AliasSet as = findAliasSetForCallSite(cs);
    if (as == null) {
      as = new AliasSet();
      as.addCallSite(cs, aa);
      aliasSets.add(as);
      return true;
    } else {
      as.addCallSite(cs, aa);
      return false;
    }
  }

  public boolean add(Instruction inst) {
    switch (inst.getOpcode()) {
      case Load:
        add((LoadInst) inst);
      case Store:
        add((StoreInst) inst);
      case Call:
        add((CallInst) inst);
      default:
        return true;
    }
  }

  public boolean add(BasicBlock bb) {
    boolean res = false;
    for (Instruction inst : bb)
      res |= add(inst);
    return res;
  }

  /**
   * Add alias relation from another AST.
   *
   * @param ast
   * @return
   */
  public boolean add(AliasSetTracker ast) {
    Util.assertion(aa.equals(ast.aa), "Merging AliasSetTracker objects with different Alias Analysis!");


    for (AliasSet set : ast.aliasSets) {
      // ignore forwarding alias set.
      if (set.forward == null) {
        // add each callSite in AST.
        for (CallSite cs : set.callSites)
          add(cs);

        OutRef<Boolean> res = new OutRef<>(false);
        for (AliasSet.PointerRec rec : set.pointerRecs) {
          AliasSet newAS = addPointer(rec.getPointer(), rec.getSize(), set.accessTy, res);
          if (set.isVolatile) newAS.setVolatile(true);
        }
      }
    }
    return false;
  }

  /*
   * remove methods - These methods are used to remove all entries that might
   * be aliased by the specified instruction.  These methods return true if any
   * alias sets were eliminated.
   */

  /**
   * Remove the specified (potentially non-empty) alias set from the
   * tracker.
   *
   * @param as
   */
  public void remove(AliasSet as) {
    as.callSites.clear();
    while (!as.isEmpty()) {
      for (int i = 0; i < as.pointerRecs.size(); i++) {
        AliasSet.PointerRec rec = as.pointerRecs.get(i);
        rec.eraseFromList();
        pointerMap.remove(new Pair<>(rec.getPointer(), this));
      }
    }
    as.removeFromTracker(this);
  }

  public boolean remove(Value ptr, int size) {
    AliasSet as = findAliasSetForPointer(ptr, size);
    if (as == null) return false;
    remove(as);
    return true;
  }

  public boolean remove(LoadInst li) {
    return remove(li.getPointerOperand(), aa.getTypeStoreSize(li.getType()));
  }

  public boolean remove(StoreInst st) {
    Value val = st.operand(0);
    return remove(st.getPointerOperand(), aa.getTypeStoreSize(val.getType()));
  }

  public boolean remove(CallInst ci) {
    return remove(CallSite.get(ci));
  }

  public boolean remove(CallSite cs) {
    if (aa.doesNotAccessMemory(cs))
      return false;

    AliasSet as = findAliasSetForCallSite(cs);
    if (as == null) return false;
    remove(as);
    return true;
  }

  public boolean remove(Instruction inst) {
    if (inst instanceof LoadInst)
      return remove((LoadInst) inst);
    else if (inst instanceof StoreInst)
      return remove((StoreInst) inst);
    else if (inst instanceof CallInst)
      return remove((CallInst) inst);
    return true;
  }

  public boolean remove(BasicBlock bb) {
    boolean eliminated = false;
    for (Instruction inst : bb)
      eliminated |= remove(inst);
    return eliminated;
  }

  public void clear() {
    aliasSets.clear();
    pointerMap.clear();
  }

  /**
   * Return the alias set that the specified pointer lives in.
   *
   * @param ptr
   * @param size
   * @param newSet
   * @return
   */
  public AliasSet getAliasSetForPointer(Value ptr, int size,
                                        OutRef<Boolean> newSet) {
    AliasSet.PointerRec entry = getEntryFor(ptr);
    AliasSet as;
    if (entry.hasAliasSet()) {
      entry.updateSize(size);
      return entry.getAliasSet(this).getForwardedTarget(this);
    } else if ((as = findAliasSetForPointer(ptr, size)) != null) {
      as.addPointer(this, entry, size);
      return as;
    } else {
      if (newSet != null) newSet.set(true);
      AliasSet set = new AliasSet();
      set.addPointer(this, entry, size);
      aliasSets.add(set);
      return set;
    }
  }

  public AliasSet getAliasSetForPointerIfExists(Value ptr, int size) {
    return findAliasSetForPointer(ptr, size);
  }

  /**
   * Return true if the specified location is represented by
   * this alias set, false otherwise.  This does not modify the AST object or
   * alias sets.
   *
   * @param ptr
   * @param size
   * @return
   */
  public boolean containsPointer(Value ptr, int size) {
    for (AliasSet as : aliasSets) {
      if (as.forward == null && as.aliasesPointer(ptr, size, aa))
        return true;
    }
    return false;
  }

  /**
   * his method is used to remove a pointer value from
   * the AliasSetTracker entirely.  It should be used when an instruction is
   * deleted from the program to update the AST.  If you don't use this, you
   * would have dangling pointers to deleted instructions.
   *
   * @param val
   */
  public void deleteValue(Value val) {
    // notify the alias analysis module this value is removed.
    aa.deleteValue(val);

    CallSite cs = CallInst.get(val);
    AliasSet as;
    if (cs.getInstruction() != null) {
      if (!aa.doesNotAccessMemory(cs))
        if ((as = findAliasSetForCallSite(cs)) != null)
          as.removeCallSite(cs);
    }

    Pair<Value, AliasSetTracker> pair = new Pair<>(val, this);
    if (!pointerMap.containsKey(pair))
      return;

    AliasSet.PointerRec ptrEntry = pointerMap.get(pair);
    as = ptrEntry.getAliasSet(this);

    // delete this pointerRec from its list.
    ptrEntry.eraseFromList();
    pointerMap.remove(pair, ptrEntry);
  }

  /**
   * This method should be used whenever a preexisting value in the
   * program is copied or cloned, introducing a new value.  Note that it is ok for
   * clients that use this method to introduce the same value multiple times: if
   * the tracker already knows about a value, it will ignore the request.
   *
   * @param from
   * @param to
   */
  public void copyValue(Value from, Value to) {
    // notify the alias analysis module that this value is copied.
    aa.copyValue(from, to);

    Pair<Value, AliasSetTracker> pair = new Pair<>(from, this);
    if (!pointerMap.containsKey(pair))
      return;
    AliasSet.PointerRec fromPtrEentry = pointerMap.get(pair);
    Util.assertion(fromPtrEentry.hasAliasSet(), "Dead entry!");

    AliasSet.PointerRec toEntry = getEntryFor(to);
    // already in alias set tracker.
    if (toEntry.hasAliasSet()) return;

    AliasSet as = fromPtrEentry.getAliasSet(this);
    as.addPointer(this, toEntry, fromPtrEentry.getSize(), true);
  }

  private void removeAliasSet(AliasSet as) {
    if (as.forward != null) {
      as.forward = null;
    }
    aliasSets.remove(as);
  }

  private AliasSet.PointerRec getEntryFor(Value val) {
    AliasSet.PointerRec entry;
    if (!pointerMap.containsKey(new Pair<>(val, this)))
      entry = new AliasSet.PointerRec(val);
    else
      entry = pointerMap.get(val);
    return entry;
  }

  private AliasSet addPointer(Value ptr, int size, int accessTy,
                              OutRef<Boolean> newSet) {
    newSet.set(false);
    AliasSet as = getAliasSetForPointer(ptr, size, newSet);
    as.accessTy |= accessTy;
    return as;
  }

  /**
   * Given a pointer with getNumOfSubLoop, finds a alias set which this pointer lives in.
   * If there are multiply such alias set which alias the specified pointer, then
   * merge them together and return the unified set.
   *
   * @param ptr
   * @param size
   * @return
   */
  private AliasSet findAliasSetForPointer(Value ptr, int size) {
    AliasSet foundSet = null;
    for (AliasSet as : aliasSets) {
      if (as.forward == null && as.aliasesPointer(ptr, size, aa)) {
        if (foundSet == null)
          foundSet = as;
        else
          foundSet.mergeSetIn(as, this);
      }
    }
    return foundSet;
  }

  private AliasSet findAliasSetForCallSite(CallSite cs) {
    AliasSet foundAS = null;
    for (AliasSet as : aliasSets) {
      if (as.forward == null && as.aliasesCallSite(cs, aa)) {
        if (foundAS == null)
          foundAS = as;
        else
          foundAS.mergeSetIn(as, this);
      }
    }
    return foundAS;
  }

  public void print(OutputStream os) {
    // TODO: 17-10-8
  }

  public static final class AliasSet {
    public static class PointerRec {
      Value val;
      AliasSet as;
      int size;

      public PointerRec(Value val) {
        this.val = val;
      }

      public Value getPointer() {
        return val;
      }

      public int getSize() {
        return size;
      }

      public boolean hasAliasSet() {
        return as != null;
      }

      public void updateSize(int newSize) {
        if (newSize > size)
          size = newSize;
      }

      public AliasSet getAliasSet(AliasSetTracker ast) {
        Util.assertion(as != null, "No AliasSet yet!");
        if (as.forward != null) {
          as = as.getForwardedTarget(ast);
        }
        return as;
      }

      public void setAliasSet(AliasSet as) {
        Util.assertion(this.as == null, "Already have an alias set");
        this.as = as;
      }

      public void eraseFromList() {
        as.pointerRecs.remove(this);
      }
    }

    AliasSet forward;
    /**
     * All pointer records in this alias set.
     */
    ArrayList<PointerRec> pointerRecs = new ArrayList<>();

    /**
     * all call sites in this alias set.
     */
    ArrayList<CallSite> callSites = new ArrayList<>();

    /**
     * Keep track of whether this alias set merely refers to the locations of
     * memory, whether it modifies the content of memory, or does both.
     */
    public interface AccessType {
      int NoModRef = 0;
      int Refs = 1;
      int Mods = 2;
      int ModRef = 3;
    }

    int accessTy;

    public interface AliasType {
      int MustAlias = 0;
      int MayAlias = 1;
    }

    int aliasTy;

    /**
     * True if this alais set contains a volatile load or store.
     */
    boolean isVolatile;

    public boolean isRef() {
      return (accessTy & Refs) != 0;
    }

    public boolean isMod() {
      return (accessTy & Mods) != 0;
    }

    public boolean isMustAlias() {
      return aliasTy == MustAlias;
    }

    public boolean isMayAlias() {
      return aliasTy == MayAlias;
    }

    public boolean isVolatile() {
      return isVolatile;
    }

    public void setVolatile(boolean val) {
      isVolatile = val;
    }

    /**
     * Return true if this alias set should be ignored as
     * part of the AliasSetTracker object.
     *
     * @return
     */
    public boolean isForwardingAliasSet() {
      return forward != null;
    }

    /**
     * Merge the specified alias set into this alias set.
     *
     * @param as
     * @param ast
     */
    public void mergeSetIn(AliasSet as, AliasSetTracker ast) {

    }

    private AliasSet() {
      accessTy = NoModRef;
      aliasTy = MustAlias;
      isVolatile = false;
    }

    private PointerRec getSomePointer() {
      return pointerRecs.isEmpty() ? null : pointerRecs.get(0);
    }

    /**
     * Return the real alias set this represents.  If this has been merged
     * with another set and is forwarding, return the ultimate destination set.
     * This also implements the union-find collapsing as well.
     *
     * @param ast
     * @return
     */
    private AliasSet getForwardedTarget(AliasSetTracker ast) {
      if (forward == null)
        return this;

      AliasSet dest = forward.getForwardedTarget(ast);
      if (dest != forward) {
        forward = dest;
      }
      return dest;
    }

    private void removeFromTracker(AliasSetTracker ast) {
      Util.assertion(ast != null);
      ast.getAliasSets().remove(this);
    }

    private void addPointer(AliasSetTracker ast, PointerRec entry, int size,
                            boolean knownMustAlias) {
      Util.assertion(!entry.hasAliasSet(), "Entry already in set!");

      if (isMustAlias() && !knownMustAlias) {
        PointerRec p = getSomePointer();
        if (p != null) {
          AliasAnalysis aa = ast.getAliasAnalysis();
          AliasResult result = aa.alias(p.getPointer(),
              p.getSize(), entry.getPointer(), entry.getSize());
          if (result == AliasResult.MayAlias)
            aliasTy = MayAlias;
          else
            p.updateSize(size);
          Util.assertion(result != NoAlias, "Cannot be part of must set!");

        }
      }

      entry.setAliasSet(this);
      entry.updateSize(size);

      // Add it the end of the alias set.
      pointerRecs.add(entry);
    }

    private void addPointer(AliasSetTracker ast, PointerRec entry, int size) {
      addPointer(ast, entry, size, false);
    }

    private void addCallSite(CallSite cs, AliasAnalysis aa) {
      callSites.add(cs);
      ModRefBehavior behavior = aa.getModRefBehavior(cs);
      if (behavior == ModRefBehavior.DoesNotAccessMemory)
        return;
      else if (behavior == ModRefBehavior.OnlyReadsMemory) {
        aliasTy = MayAlias;
        accessTy |= Refs;
        return;
      }

      aliasTy = MayAlias;
      accessTy = ModRef;
    }

    private void removeCallSite(CallSite cs) {
      for (int i = 0; i < callSites.size(); i++) {
        if (callSites.get(i).getInstruction().equals(cs.getInstruction())) {
          callSites.set(i, callSites.get(callSites.size() - 1));
          callSites.remove(callSites.size() - 1);
        }
      }
    }

    /**
     * Return true if the specified pointer may or must alias one of the elements
     * in this set.
     *
     * @param ptr
     * @param size
     * @param aa
     * @return
     */
    private boolean aliasesPointer(Value ptr, int size, AliasAnalysis aa) {
      if (aliasTy == MustAlias) {
        Util.assertion(callSites.isEmpty(), "Illegal must alias set");

        PointerRec somePtr = getSomePointer();
        Util.assertion(somePtr != null, "Empty must alias set?");
        return aa.alias(somePtr.getPointer(),
            somePtr.getSize(), ptr, size) != NoAlias;
      }

      // If this is a may-alias set, we have to check if any pointers in this
      // alias set is alias the given ptr or not?
      for (PointerRec rec : pointerRecs)
        if (aa.alias(rec.getPointer(), rec.getSize(), ptr, size) != NoAlias)
          return true;

      // Check all call sites.
      if (!callSites.isEmpty()) {
        if (aa.hasNoModRefInfoForCalls())
          return true;

        for (CallSite cs : callSites)
          if (aa.getModRefInfo(cs, ptr, size) != ModRefResult.NoModRef)
            return true;
      }
      return false;
    }

    /**
     * Return true if the specified CallSite may or must alias with the one of
     * elements within this set.
     *
     * @param cs
     * @param aa
     * @return
     */
    private boolean aliasesCallSite(CallSite cs, AliasAnalysis aa) {
      if (aa.doesNotAccessMemory(cs))
        return false;

      // Check all call sites.
      if (aa.hasNoModRefInfoForCalls())
        return true;

      for (CallSite CS : callSites)
        if (aa.getModRefInfo(CS, cs) != ModRefResult.NoModRef
            || aa.getModRefInfo(cs, CS) != ModRefResult.NoModRef)
          return true;

      for (PointerRec rec : pointerRecs)
        if (aa.getModRefInfo(cs, rec.getPointer(), rec.getSize())
            != ModRefResult.NoModRef)
          return true;

      return false;
    }

    public boolean isEmpty() {
      return pointerRecs.isEmpty();
    }

    public void print(OutputStream os) {
      try (PrintWriter pw = new PrintWriter(os)) {
        pw.print(" AliasSet[" + this + "]");
        pw.print((isMustAlias() ? "must" : "may") + "alias, ");
        switch (accessTy) {
          case NoModRef:
            pw.print("No access");
          case Refs:
            pw.print("Ref      ");
          case Mods:
            pw.print("Mod      ");
          case ModRef:
            pw.print("Mod/Ref  ");
          default:
            Util.shouldNotReachHere("Bad value for AccessTy");
        }
        if (isVolatile) pw.print("[volatile]");
        if (forward != null)
          pw.print(" forwarding to " + forward);

        if (!isEmpty()) {
          pw.print("Pointers: ");
          for (int i = 0, e = pointerRecs.size(); i < e; i++) {
            PointerRec rec = pointerRecs.get(i);
            if (i != 0)
              pw.print(", ");
            pw.print("(" + rec.getPointer());
            pw.print(", " + rec.getSize() + ")");
          }
        }

        if (!callSites.isEmpty()) {
          pw.println();
          pw.print(callSites.size() + " call sites: ");
          for (int i = 0, e = callSites.size(); i < e; i++) {
            if (i != 0)
              pw.print(", ");
            pw.print(callSites.get(i).getCalledValue());
          }
        }
        pw.println();
      }
    }
  }
}
