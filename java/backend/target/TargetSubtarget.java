package backend.target;
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

import backend.codegen.dagisel.SDep;
import backend.codegen.dagisel.SUnit;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public abstract class TargetSubtarget {

  protected TargetInstrInfo instrInfo;
  protected TargetRegisterInfo regInfo;
  protected SubtargetFeatureKV[] subTypeKV;
  protected SubtargetFeatureKV[] featureKV;

  public int getSpecialAddressLatency() {
    return 0;
  }

  public boolean isTargetDarwin() {
    return false;
  }

  public boolean isTargetELF() {
    return false;
  }

  public boolean isTargetCygMing() {
    return false;
  }

  public boolean isTargetWindows() {
    return false;
  }

  public boolean isTargetCOFF() {
    return isTargetCygMing() || isTargetWindows();
  }

  public void adjustSchedDependency(SUnit opSU, SUnit su, SDep dep) {
  }

  public int getHwMode() {
    return 0;
  }

  /**
   * Checks if the specified feature is enabled or not in current
   * hardware mode.
   *
   * @param fs Features.
   * @return
   */
  public boolean checkFeatures(String fs) {
    SubtargetFeatures testFS = new SubtargetFeatures(fs);
    SubtargetFeatures features = new SubtargetFeatures(fs);
    int featuresbits = features.getBits(subTypeKV, featureKV);
    int testBits = testFS.getBits(subTypeKV, featureKV);
    return (featuresbits & testBits) == featuresbits;
  }

  public TargetRegisterInfo getRegisterInfo() {
    return regInfo;
  }

  public TargetInstrInfo getInstrInfo() {
    return instrInfo;
  }

  public abstract String parseSubtargetFeatures(String fs, String cpu);

  public boolean is64Bit() {
    return false;
  }
}
