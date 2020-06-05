package backend.codegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
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

import backend.debug.DebugLoc;
import backend.mc.MCAsmInfo;
import backend.mc.MCRegisterInfo;
import backend.mc.MCSymbol;
import backend.pass.AnalysisResolver;
import backend.pass.ImmutablePass;
import backend.value.Function;
import backend.value.GlobalVariable;
import backend.value.MDNode;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.Pair;
import tools.Util;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class MachineModuleInfo implements ImmutablePass {
  private AnalysisResolver resolver;
  private int nextLabelID;
  private boolean dgbInfoAvailable;
  private ArrayList<Pair<MDNode, Pair<Integer, DebugLoc>>> variableDbgInfo;
  private HashMap<MCSymbol, TIntArrayList> lpadToCallSiteMap;
  private MCSymbol.MCContext context;
  private ArrayList<Function> personalities;
  private int currentCallSite;
  private TObjectIntHashMap<MCSymbol> callSiteMap;

  public MachineModuleInfo(MCAsmInfo mai,
                           MCRegisterInfo mri) {
    nextLabelID = 0;
    dgbInfoAvailable = false;
    variableDbgInfo = new ArrayList<>();
    lpadToCallSiteMap = new HashMap<>();
    context = new MCSymbol.MCContext(mai, mri);
    personalities = new ArrayList<>();
    currentCallSite = 0;
    callSiteMap = new TObjectIntHashMap<>();
  }

  public MachineModuleInfo() {
    Util.shouldNotReachHere("This pass should be explicitly called in LLVMTargetMachine");
  }

  public MCSymbol.MCContext getContext() { return context; }

  @Override
  public void setAnalysisResolver(AnalysisResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public AnalysisResolver getAnalysisResolver() {
    return resolver;
  }

  @Override
  public String getPassName() {
    return "Machine Module Info Pass";
  }

  @Override
  public void initializePass() {

  }

  public int nextLabelID() {
    return 0;
  }

  public int getNextLabelID() {
    return nextLabelID;
  }

  public void addInvoke(MachineBasicBlock landingPad,
                        MCSymbol beginLabel,
                        MCSymbol endLabel) {

  }

  public boolean hasDebugInfo() {
    return dgbInfoAvailable;
  }

  public void setDgbInfoAvailable(boolean ability) {
    dgbInfoAvailable = ability;
  }

  public void setVariableDgbInfo(MDNode node, int slot, DebugLoc dl) {
    variableDbgInfo.add(Pair.get(node, Pair.get(slot, dl)));
  }

  public ArrayList<Pair<MDNode, Pair<Integer, DebugLoc>>> getVariableDbgInfo() {
    return variableDbgInfo;
  }

  public void addPersonality(MachineBasicBlock mbb, Function stripPointerCasts) {

  }

  public void addCleanup(MachineBasicBlock mbb) {

  }

  public void addCatchTypeInfo(MachineBasicBlock mbb, GlobalVariable tyInfo) {
    ArrayList<GlobalVariable> list = new ArrayList<>();
    list.add(tyInfo);
    addCatchTypeInfo(mbb, list);
  }

  public void addCatchTypeInfo(MachineBasicBlock mbb, ArrayList<GlobalVariable> tyInfos) {

  }

  public void addFilterTypeInfo(MachineBasicBlock mbb, ArrayList<GlobalVariable> filterList) {

  }

  public MCSymbol addLandingPad(MachineBasicBlock mbb) {
    return null;
  }

  public void setCallSiteLandingPad(MCSymbol sym, TIntArrayList sites) {
    if (!lpadToCallSiteMap.containsKey(sym))
      lpadToCallSiteMap.put(sym, new TIntArrayList());
    lpadToCallSiteMap.get(sym).addAll(sites);
  }

  public int getCurrentCallSite() {
    return currentCallSite;
  }

  public void setCurrentCallSite(int val) {
    this.currentCallSite = val;
  }

  public void setCallSiteBeginLabel(MCSymbol beginLabel, int site) {
    callSiteMap.put(beginLabel, site);
  }
}
