package backend.passManaging;
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

import backend.pass.*;
import backend.support.BackendCmdOptions;
import backend.support.PrintModulePass;
import backend.value.Module;
import tools.Util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static backend.passManaging.PMDataManager.PassDebugLevel.Arguments;
import static backend.passManaging.PMDataManager.PassDebugLevel.Structures;
import static backend.passManaging.PMTopLevelManager.TopLevelPassManagerType.TLM_Pass;
import static backend.support.BackendCmdOptions.shouldPrintAfterPass;
import static backend.support.BackendCmdOptions.shouldPrintBeforePass;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class PassManagerImpl extends PMDataManager implements
    PMTopLevelManager,
    Pass {
  private TopLevelPassManagerType tpmt;

  private HashMap<Pass, Pass> lastUsers;
  private HashMap<Pass, HashSet<Pass>> inversedLastUser;
  private ArrayList<ImmutablePass> immutablePasses;
  private HashMap<Pass, AnalysisUsage> anUsageMap;
  private PMStack activeStack;
  private ArrayList<PMDataManager> passManagers;

  private AnalysisResolver resolver;

  @Override
  public void setAnalysisResolver(AnalysisResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public AnalysisResolver getAnalysisResolver() {
    return resolver;
  }

  public PassManagerImpl(int depth) {
    super(depth);
    initTopLevelManager();
  }

  @Override
  public Pass createPrinterPass(PrintStream os, String banner) {
    return PrintModulePass.createPrintModulePass(os);
  }

  private void initTopLevelManager() {
    lastUsers = new HashMap<>();
    inversedLastUser = new HashMap<>();
    immutablePasses = new ArrayList<>();
    anUsageMap = new HashMap<>();
    activeStack = new PMStack();
    passManagers = new ArrayList<>();

    MPPassManager mp = new MPPassManager();
    mp.setTopLevelManager(this);
    activeStack.push(mp);
    addPassManager(mp);
    tpmt = TLM_Pass;
  }

  /**
   * Execute all of the passes scheduled for execution.  Keep track of
   * whether any of the passes modifies the module, and if so, return true.
   *
   * @param m
   * @return
   */
  public boolean run(Module m) {
    dumpArguments();
    dumpPasses();
    initializeAllAnalysisInfo();
    boolean changed = false;
    for (int i = 0, e = getNumContainedManagers(); i != e; i++)
      changed |= getContainedManager(i).runOnModule(m);
    return changed;
  }

  /**
   * Add a pass to the queue of passes scheduled to be run.
   *
   * @param p
   */
  @Override
  public void add(Pass p) {
    schedulePass(p);
  }

  @Override
  public String getPassName() {
    return "Pass Manager Impl";
  }

  @Override
  public void getAnalysisUsage(AnalysisUsage au) {
    au.setPreservedAll();
  }

  @Override
  public PMDataManager getAsPMDataManager() {
    return this;
  }

  @Override
  public Pass getAsPass() {
    return this;
  }

  public MPPassManager getContainedManager(int index) {
    Util.assertion(index >= 0 && index < passManagers.size());
    return (MPPassManager) passManagers.get(index);
  }

  @Override
  public int getNumContainedManagers() {
    return passManagers.size();
  }

  @Override
  public void schedulePass(Pass p) {
    // Given pass a chance to prepare the stage.
    p.preparePassManager(activeStack);

    PassInfo pi = p.getPassInfo();
    if (pi != null && pi.isAnalysis() && findAnalysisPass(pi) != null)
      return;

    AnalysisUsage au = findAnalysisUsage(p);
    boolean checkAnalysis = true;
    while (checkAnalysis) {
      checkAnalysis = false;
      HashSet<PassInfo> requiredSet = au.getRequired();
      for (PassInfo pInfo : requiredSet) {
        Pass analysisPass = findAnalysisPass(pInfo);
        if (analysisPass == null) {
          analysisPass = pInfo.createPass();
          if (p.getPotentialPassManagerType() ==
              analysisPass.getPotentialPassManagerType()) {
            // schedule analysis pass that is managed by the same pass manager.
            schedulePass(analysisPass);
          } else if (p.getPotentialPassManagerType()
              .compareTo(analysisPass.getPotentialPassManagerType()) > 0) {
            // schedule analysis pass that is managed by a new manager.
            schedulePass(analysisPass);
            // recheck analysis passes to ensure that
            // required analysises are already checked are
            // still available.
            checkAnalysis = true;
          } else {
            // don't schedule this analysis.
          }
        }
      }
    }

    ImmutablePass ip = p.getAsImmutablePass();
    if (ip != null) {
      // p is a immutable pass and it will be managed by this
      // top level manager. Set up analysis resolver to connect them.
      AnalysisResolver ar = new AnalysisResolver(this);
      p.setAnalysisResolver(ar);
      initializeAnalysisImpl(p);
      addImmutablePass(ip);
      recordAvailableAnalysis(ip);
      return;
    }

    if (pi != null && !pi.isAnalysis() && shouldPrintBeforePass(pi)) {
      Pass pp = p.createPrinterPass(System.err,
          String.format("*** IR Dump Before %s ***", p.getPassName()));
      pp.assignPassManager(activeStack, getTopLevelPassManagerType());
    }

    // Now all required passes are available.
    p.assignPassManager(activeStack, getTopLevelPassManagerType());

    if (pi != null && !pi.isAnalysis() && shouldPrintAfterPass(pi)) {
      Pass pp = p.createPrinterPass(System.err,
          String.format("*** IR Dump After %s ***", p.getPassName()));
      pp.assignPassManager(activeStack, getTopLevelPassManagerType());
    }
  }

  @Override
  public PassManagerType getTopLevelPassManagerType() {
    return PassManagerType.PMT_ModulePassManager;
  }

  @Override
  public void setLastUser(ArrayList<Pass> analysisPasses, Pass p) {
    for (Pass anaPass : analysisPasses) {
      lastUsers.put(anaPass, p);
      if (p.equals(anaPass))
        continue;

      for (Map.Entry<Pass, Pass> entry : lastUsers.entrySet()) {
        if (entry.getValue().equals(anaPass)) {
          lastUsers.put(entry.getKey(), p);
        }
      }
    }
  }

  @Override
  public void collectLastUses(ArrayList<Pass> lastUsers, Pass p) {
    if (!inversedLastUser.containsKey(p))
      return;

    HashSet<Pass> lu = inversedLastUser.get(p);
    lastUsers.addAll(lu);
  }

  @Override
  public Pass findAnalysisPass(PassInfo pi) {
    Pass p = null;
    // check pass manager.
    for (PMDataManager pm : passManagers) {
      p = pm.findAnalysisPass(pi, false);
    }

    for (ImmutablePass ip : immutablePasses) {
      if (ip.getPassInfo().equals(pi))
        p = ip;
    }
    return p;
  }

  public AnalysisUsage findAnalysisUsage(Pass p) {
    AnalysisUsage au = null;
    if (anUsageMap.containsKey(p))
      au = anUsageMap.get(p);
    else {
      au = new AnalysisUsage();
      p.getAnalysisUsage(au);
      anUsageMap.put(p, au);
    }
    return au;
  }

  public void addImmutablePass(ImmutablePass p) {
    p.initializePass();
    immutablePasses.add(p);
  }

  public ArrayList<ImmutablePass> getImmutablePasses() {
    return immutablePasses;
  }

  public void addPassManager(PMDataManager pm) {
    passManagers.add(pm);
  }

  public void dumpPasses() {
    if (BackendCmdOptions.PassDebugging.value.compareTo(Structures) < 0)
      return;

    // print out the immutable passes.
    immutablePasses.forEach(im -> {
      im.dumpPassStructures(0);
    });

    // Every class that derives from PMDataManager also derives
    // from Pass.
    passManagers.forEach(pm ->
    {
      ((Pass) pm).dumpPassStructures(1);
    });
  }

  public void dumpArguments() {
    if (BackendCmdOptions.PassDebugging.value.compareTo(Arguments) < 0)
      return;

    System.err.print("Pass Arguments: ");
    passManagers.forEach(PMDataManager::dumpPassArguments);
    System.err.println();
  }

  public void initializeAllAnalysisInfo() {
    for (PMDataManager pm : passManagers) {
      pm.initializeAnalysisInfo();
    }

    for (Map.Entry<Pass, Pass> entry : lastUsers.entrySet()) {
      if (inversedLastUser.containsKey(entry.getValue())) {
        HashSet<Pass> l = inversedLastUser.get(entry.getValue());
        l.add(entry.getKey());
      } else {
        HashSet<Pass> l = new HashSet<>();
        l.add(entry.getKey());
        inversedLastUser.put(entry.getValue(), l);
      }
    }
  }

  public PMStack getActiveStack() {
    return activeStack;
  }
}
