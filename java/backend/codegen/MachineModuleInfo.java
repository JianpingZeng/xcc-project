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
import cfe.type.ArrayType;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import tools.Pair;
import tools.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class MachineModuleInfo implements ImmutablePass {
  private AnalysisResolver resolver;
  private int nextLabelID;
  private boolean dgbInfoAvailable;
  private ArrayList<Pair<MDNode, Pair<Integer, DebugLoc>>> variableDbgInfo;
  private HashMap<MCSymbol, TLongArrayList> lpadToCallSiteMap;
  private MCSymbol.MCContext context;
  private ArrayList<Function> personalities;
  private long currentCallSite;
  private TObjectLongMap<MCSymbol> callSiteMap;
  private ArrayList<LandingPadInfo> landingPads;
  /**
   * List of c++ TypeInfo used in the current function.
   */
  private ArrayList<GlobalVariable> typeInfos;
  private boolean callsEHReturn;
  private boolean callsUnwindInit;

  public MachineModuleInfo(MCAsmInfo mai,
                           MCRegisterInfo mri) {
    nextLabelID = 0;
    dgbInfoAvailable = false;
    variableDbgInfo = new ArrayList<>();
    lpadToCallSiteMap = new HashMap<>();
    context = new MCSymbol.MCContext(mai, mri);
    personalities = new ArrayList<>();
    currentCallSite = 0;
    callSiteMap = new TObjectLongHashMap<>();
    landingPads = new ArrayList<>();
    typeInfos = new ArrayList<>();
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

  public void addCatchTypeInfo(MachineBasicBlock landingPad, ArrayList<GlobalVariable> tyInfos) {
    if (tyInfos == null || tyInfos.isEmpty()) return;
    LandingPadInfo lp = getOrCreateLandingPadInfo(landingPad);
    for (int i = tyInfos.size(); i != 0; --i)
      lp.typeIds.add(getTypeTDFor(tyInfos.get(i-1)));
  }

  public void addFilterTypeInfo(MachineBasicBlock mbb, ArrayList<GlobalVariable> filterList) {

  }

  /**
   * Provides the label of a try-catch landingpad block.
   * @param landingPad
   * @return
   */
  public MCSymbol addLandingPad(MachineBasicBlock landingPad) {
    MCSymbol landingPadLabel = context.createTemporarySymbol();
    LandingPadInfo lpi = getOrCreateLandingPadInfo(landingPad);
    lpi.landingPadLabel = landingPadLabel;
    return landingPadLabel;
  }

  /**
   * Find or create an {@linkplain LandingPadInfo} for the given landing pad block.
   * @param landingPad
   * @return
   */
  public LandingPadInfo getOrCreateLandingPadInfo(MachineBasicBlock landingPad) {
    for (LandingPadInfo info : landingPads)
      if (info.landingPadBlock == landingPad)
        return info;
    LandingPadInfo info = new LandingPadInfo(landingPad);
    landingPads.add(info);
    return info;
  }

  public void setCallSiteLandingPad(MCSymbol sym, TLongArrayList sites) {
    if (!lpadToCallSiteMap.containsKey(sym))
      lpadToCallSiteMap.put(sym, new TLongArrayList());
    if (sites == null || sites.isEmpty())
      return;
    lpadToCallSiteMap.get(sym).addAll(sites);
  }

  public long getCurrentCallSite() {
    return currentCallSite;
  }

  public void setCurrentCallSite(long val) {
    this.currentCallSite = val;
  }

  public void setCallSiteBeginLabel(MCSymbol beginLabel, long site) {
    callSiteMap.put(beginLabel, site);
  }

  public int getTypeTDFor(GlobalVariable gv) {
    for (int i = 0, e = typeInfos.size(); i < e; ++i) {
      GlobalVariable v = typeInfos.get(i);
        if (Objects.equals(v, gv)) return i+1;
    }
    typeInfos.add(gv);
    return typeInfos.size();
  }

  public void setCallsEHReturn(boolean b) { callsEHReturn = b; }
  public boolean getCallsEHReturn() { return callsEHReturn; }
  public void setCallsUnwindInit(boolean b) { callsUnwindInit = b; }
  public boolean getCallsUnwindInit() { return callsUnwindInit; }
}
