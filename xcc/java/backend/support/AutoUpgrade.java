package backend.support;
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

import backend.intrinsic.Intrinsic;
import backend.ir.IRBuilder;
import backend.type.*;
import backend.utils.SuccIterator;
import backend.value.*;
import backend.value.Instruction.*;
import tools.OutRef;
import tools.Pair;
import tools.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class AutoUpgrade {

  public static boolean upgradeGlobalVariable(GlobalVariable gv) {
    switch (gv.getName()) {
      case ".llvm.eh.catch.all.value":
        gv.setName("llvm.eh.catch.all.value");
        return true;
    }
    return false;
  }

  public static boolean upgradeIntrinsicFunction(Function f,
                                                  OutRef<Function> newFn) {
    newFn.set(null);
    boolean upgraded = upgradeInstrinsicFunction1(f, newFn);
    if (newFn.get() != null)
      f = newFn.get();

    Intrinsic.ID id = f.getIntrinsicID();
    if (id != Intrinsic.ID.not_intrinsic)
      f.setAttributes(Intrinsic.getAttributes(id));

    return upgraded;
  }

  private static boolean upgradeInstrinsicFunction1(Function f,
                                                    OutRef<Function> newFn) {
    Util.assertion(f != null, "Illegal to upgrade a on-existent function.");

    String name = f.getName();
    // quickly eliminate it, if it's not a candidate.
    if (name.length() <= 8 || !name.startsWith("llvm."))
      return false;
    // strip the leading 5 letters - 'llvm.'
    name = name.substring(5);
    FunctionType fty = f.getFunctionType();
    Module m = f.getParent();
    switch (name.charAt(0)) {
      default: break;
      case 'a':
        if (name.startsWith("atomic.cmp.swap") ||
                name.startsWith("atomic.swap") ||
                name.startsWith("atomic.load.add") ||
                name.startsWith("atomic.load.sub") ||
                name.startsWith("atomic.load.and") ||
                name.startsWith("atomic.load.nand") ||
                name.startsWith("atomic.load.or") ||
                name.startsWith("atomic.load.xor") ||
                name.startsWith("atomic.load.max") ||
                name.startsWith("atomic.load.min") ||
                name.startsWith("atomic.load.umax") ||
                name.startsWith("atomic.load.umin"))
          return true;
        break;
      case 'i':
        if (name.equals("init.trampoline")) {
          // new llvm.init.trampoline returns nothing.
          if (fty.getReturnType().isVoidType())
            break;
          Util.assertion(fty.getNumParams() == 1, "old init.trampoline must has only on parameter");
          String nameTemp = f.getName();
          f.setName("");
          newFn.set((Function) m.getOrInsertFunction(nameTemp,
                  Type.getVoidTy(m.getContext()),
                  fty.getParamType(0),
                  fty.getParamType(1),
                  fty.getParamType(2), null));
          return true;
        }
        break;
      case 'm':
        if (name.equals("memory.barrier"))
          return true;
        break;
      case 'p':
        if (name.equals("prefetch")) {
          if (fty.getNumParams() == 4) break;
          Util.assertion(fty.getNumParams() == 3, "old prefetch must take 3 parameters");
          String nameTemp = f.getName();
          f.setName("");
          newFn.set((Function)m.getOrInsertFunction(nameTemp,
                  fty.getReturnType(),
                  fty.getParamType(0),
                  fty.getParamType(1),
                  fty.getParamType(2),
                  fty.getParamType(2), null));
          return true;
        }
        break;
      case 'x':
        String newFnName = null;
        if (name.equals("x86.sse43.crc32.8"))
          newFnName = "llvm.x86.sse42.crc32.32.8";
        else if (name.equals("x86.sse42.crc32.16"))
          newFnName = "llvm.x86.sse42.crc32.32.16";
        else if (name.equals("x86.sse42.crc32.32"))
          newFnName = "llvm.x86.sse42.crc32.32.32";
        else if (name.equals("x86.sse42.crc64.8"))
          newFnName = "llvm.x86.sse42.crc32.64.8";
        else if (name.equals("x86.sse42.crc64.64"))
          newFnName = "llvm.x86.sse42.crc32.64.64";
        if (newFnName != null) {
          f.setName(newFnName);
          newFn.set(f);
          return true;
        }

        // Calls to these instructions are transformed into unaligned loads.
        if (name.equals("x86.sse.loadu.ps") ||
                name.equals("x86.sse2.loadu.dq") ||
                name.equals("x86.sse2.loadu.pd"))
          return true;

        // Calls to these instructions are transformed into nontemporal stores.
        if (name.equals("x86.sse.movnt.ps") ||
                name.equals("x86.sse2.movnt.dq") ||
                name.equals("x86.sse2.movnt.pd") ||
                name.equals("x86.sse2.movnt.i"))
          return true;
        break;
    }
    return false;
  }

  public static void upgradeExceptionHandling(Module m) {
    Function ehException = m.getFunction("llvm.eh.exception");
    Function ehSelector = m.getFunction("llvm.eh.selector");
    if (ehException == null || ehSelector == null)
      return;

    LLVMContext ctx = m.getContext();
    Type exnTy = PointerType.getUnqual(Type.getInt8Ty(ctx));
    Type selTy = Type.getInt32Ty(ctx);
    Type lpadSlotTy = StructType.get(ctx, exnTy, selTy);

    HashMap<InvokeInst, tools.Pair<Value, Value>> invokeToIntrinsicsMap = new HashMap<>();
    for (Function f : m) {
      for (BasicBlock bb : f) {
        TerminatorInst ti = bb.getTerminator();
        if (!(ti instanceof InvokeInst))
          continue;

        InvokeInst ii = (InvokeInst) ti;
        BasicBlock unwindBB = ii.getUnwindDest();
        // ignore the landing pad block which already has a landingpad instruction.
        if (unwindBB.isLandingPad()) continue;

        HashSet<BasicBlock> visited = new HashSet<>();
        OutRef<CallInst> exn = new OutRef<>();
        OutRef<CallInst> sel = new OutRef<>();
        findExnAndSelIntrinsics(unwindBB, exn, sel, visited);
        Util.assertion(exn.get() != null && sel.get() != null,
                "can't find eh.exception and eh.selector calls!");
        invokeToIntrinsicsMap.put(ii, Pair.get(exn.get(), sel.get()));
      }
    }

    HashMap<Function, Pair<Value, Value>> fnToLPadSlotMap = new HashMap<>();
    HashSet<Instruction> deadInsts = new HashSet<>();
    for (Map.Entry<InvokeInst, Pair<Value, Value>> entry : invokeToIntrinsicsMap.entrySet()) {
      InvokeInst invoke = entry.getKey();
      BasicBlock unwindDest = invoke.getUnwindDest();
      Function fn = unwindDest.getParent();
      CallInst exn = (CallInst) entry.getValue().first;
      CallInst sel = (CallInst) entry.getValue().second;

      Value exnSlot, selSlot;
      if (!fnToLPadSlotMap.containsKey(fn)) {
        BasicBlock entryBB = fn.getEntryBlock();
        exnSlot = new AllocaInst(exnTy, "exn", entryBB.getTerminator());
        selSlot = new AllocaInst(selTy, "sel", entryBB.getTerminator());
        fnToLPadSlotMap.put(fn, Pair.get(exnSlot, selSlot));
      } else {
        exnSlot = fnToLPadSlotMap.get(fn).first;
        selSlot = fnToLPadSlotMap.get(fn).second;
      }

      if (unwindDest.getSinglePredecessor() == null) {
        // the unwind block has multiple predecessors, create an
        // new unwind block and set it as the new predecessor of old one.
        BasicBlock newBB = BasicBlock.createBasicBlock(ctx, "new.lpad",
                unwindDest.getParent());
        new BranchInst(unwindDest, newBB);
        invoke.setUnwindDest(newBB);

        // fix up those phis in the original unwind block.
        for (int i = 0, e = unwindDest.getNumOfInsts(); i < e && unwindDest.getInstAt(i) instanceof PhiNode; ++i) {
          PhiNode pn = (PhiNode) unwindDest.getInstAt(i);
          int idx = pn.getBasicBlockIndex(invoke.getParent());
          if (idx == -1) continue;
          pn.setIncomingBlock(idx, newBB);
        }
      }

      IRBuilder builder = new IRBuilder(ctx);
      builder.setInsertPoint(unwindDest, unwindDest.getInstAt(unwindDest.getFirstNonPhi()));
      Value persFn = sel.getArgOperand(1);

      ArrayList<Value> clauses = new ArrayList<>();
      boolean isCleanup = transferClausesToHandingPadInst(sel, clauses);
      LandingPadInst lpi = builder.createLandingPad(lpadSlotTy, persFn, clauses.size(), "");
      lpi.setCleanup(isCleanup);
      clauses.forEach(lpi::addClause);

      Value lpExn = builder.createExtractValue(lpi, "", 0);
      Value lpSel = builder.createExtractValue(lpi, "", 1);
      builder.createStore(lpExn, exnSlot);
      builder.createStore(lpSel, selSlot);

      deadInsts.add(exn);
      deadInsts.add(sel);
    }

    // Replace the old intrinsic calls with the values from the landingpad
    // instruction(s). These values were stored in allocas for us to use here.
    for (Map.Entry<InvokeInst, Pair<Value, Value>> entry : invokeToIntrinsicsMap.entrySet()) {
      CallInst exn = (CallInst) entry.getValue().first;
      CallInst sel = (CallInst) entry.getValue().second;
      BasicBlock parent = exn.getParent();

      Pair<Value, Value> exnSelSlots = fnToLPadSlotMap.get(parent.getParent());
      IRBuilder builder = new IRBuilder(ctx);
      builder.setInsertPoint(parent, exn);
      LoadInst lpExn = builder.createLoad(exnSelSlots.first, false, "exn.load");
      LoadInst lpSel = builder.createLoad(exnSelSlots.second, false, "sel.load");
      exn.replaceAllUsesWith(lpExn);
      sel.replaceAllUsesWith(lpSel);
    }

    // remove dead instructions.
    deadInsts.forEach(Instruction::eraseFromParent);

    // Replace calls to "llvm.eh.resume" with the 'resume' instruction. Load the
    // exception and selector values from the stored place.
    Function ehResume = m.getFunction("llvm.eh.resume");
    if (ehResume == null) return;

    ArrayList<Use> uses = new ArrayList<>(ehResume.getUseList());
    for (Use u : uses) {
      CallInst resume = (CallInst) u.getUser();
      BasicBlock bb = resume.getParent();

      IRBuilder builder = new IRBuilder(ctx);
      builder.setInsertPoint(bb, resume);

      Value lpadVal = builder.createInsertValue(Value.UndefValue.get(lpadSlotTy),
              resume.getArgOperand(0), "lpad.val", 0);
      lpadVal = builder.createInsertValue(lpadVal, resume.getArgOperand(1),
              "lpad.val", 1);
      builder.createResume(lpadVal);

      // remove all instructions after the resmue.
      int idx = resume.getIndexToBB();
      int size = bb.size();
      ArrayList<Instruction> dead = new ArrayList<>();
      while (idx < size) {
        dead.add(bb.getInstAt(idx++));
      }
      dead.forEach(Instruction::eraseFromParent);
    }
  }

  private static boolean transferClausesToHandingPadInst(CallInst ehSel, ArrayList<Value> clauses) {
    LLVMContext ctx = ehSel.getContext();
    int n = ehSel.getNumOfOperands();
    boolean isCleanup = false;

    for (int i = n - 1; i > 1; --i) {
      Value arg = ehSel.getArgOperand(i);
      if (arg instanceof ConstantInt) {
        ConstantInt ci = (ConstantInt) arg;
        long filterLength = ci.getZExtValue();
        long firstCatch = i + filterLength + (filterLength == 0 ? 1 : 0);
        Util.assertion(firstCatch <= n, "invalid filter length!");

        if (firstCatch < n) {
          for (long j = firstCatch; j < n; ++j) {
            Value val = ehSel.getArgOperand((int) j);
            if (!val.hasName() || !val.getName().equals("llvm.eh.catch.all.value"))
              clauses.add(ehSel.getArgOperand((int) j));
            else {
              GlobalVariable gv = (GlobalVariable) val;
              clauses.add(gv.getInitializer());
            }
          }
        }

        if (filterLength == 0) {
          // cleanup
          isCleanup = true;
        } else {
          // filter.
          ArrayList<Constant> tyInfo = new ArrayList<>();
          for (int j = i + 1; j < firstCatch; ++j)
            tyInfo.add((Constant) ehSel.getArgOperand(j));
          ArrayType aty = ArrayType.get(!tyInfo.isEmpty() ?
                          tyInfo.get(0).getType() :
                          PointerType.getUnqual(Type.getInt8Ty(ctx)),
                  tyInfo.size());
          clauses.add(ConstantArray.get(aty, tyInfo));
        }
        n = i;
      }
    }

    if (n > 2) {
      for (int j = 2; j < n; ++j) {
        Value val = ehSel.getArgOperand(j);
        if (!val.hasName() || !val.getName().equals("llvm.eh.catch.all.value"))
          clauses.add(ehSel.getArgOperand(j));
        else {
          GlobalVariable gv = (GlobalVariable) ehSel.getArgOperand(j);
          clauses.add(gv.getInitializer());
        }
      }
    }
    return isCleanup;
  }

  private static void findExnAndSelIntrinsics(BasicBlock bb,
                                              OutRef<CallInst> exn,
                                              OutRef<CallInst> sel,
                                              HashSet<BasicBlock> visited) {
    if (!visited.add(bb)) return;

    for (Instruction inst : bb) {
      if (inst instanceof CallInst) {
        CallInst ci = (CallInst) inst;
        switch (ci.getCalledFunction().getIntrinsicID()) {
          default: break;
          case eh_exception:
            Util.assertion(exn.get() == null, "found more than one eh.exception call");
            exn.set(ci);
            break;
          case eh_selector:
            Util.assertion(sel.get() == null, "found more than one eh.selector call!");
            sel.set(ci);
            break;
        }
        if (exn.get() != null && sel.get() != null)
          return;
      }
    }

    if (exn.get() != null && sel.get() != null) return;

    for (SuccIterator itr = bb.succIterator(); itr.hasNext();) {
      BasicBlock nextBB = itr.next();
      findExnAndSelIntrinsics(nextBB, exn, sel, visited);
      if (exn.get() != null && sel.get() != null) return;
    }
  }

  /**
   * This function strips all debug information intrinsics, except for llvm.dbg.declare.
   * If an llvm.dbg.declare intrinsic is invalid, the all uses of that will be be striped.
   * @param m
   */
  public static void checkDebugInfoIntrinsics(Module m) {
    // TODO July/06/2020
  }

  public static void upgradeIntrinsicCall(CallInst ci, Function func) {

  }
}
