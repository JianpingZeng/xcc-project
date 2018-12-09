/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2015-2018, Jianping Zeng.
 * All rights reserved.
 *
 * Please refer the LICENSE for detail.
 */

package backend.mc;

import backend.target.InstrItinerary;
import backend.target.SubtargetFeatureKV;
import backend.target.SubtargetFeatures;
import tools.Util;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class MCSubtargetInfo {
  private String targetTriple;
  private SubtargetFeatureKV[] procFeatures;
  private SubtargetFeatureKV[] procDesc;
  private SubtargetInfoKV[] procItins;
  private InstrStage[] stages;
  private int[] operandCycles;
  private int[] forwardingPathes;
  private long featureBits;

  public void initMCSubtargetInfo(String tt, String cpu,
                                  String fs,
                                  SubtargetFeatureKV[] pf,
                                  SubtargetFeatureKV[] pd,
                                  SubtargetInfoKV[] pi,
                                  InstrStage[] is,
                                  int[] oc, int[] fp) {
    targetTriple = tt;
    procFeatures = pf;
    procDesc = pd;
    procItins = pi;
    stages = is;
    operandCycles = oc;
    forwardingPathes = fp;
    SubtargetFeatures features = new SubtargetFeatures(fs);
    featureBits = features.getBits(procDesc, procFeatures);
  }

  public String getTargetTriple() {
    return targetTriple;
  }

  public long getFeatureBits() {
    return featureBits;
  }

  /**
   * Change CPU and optionally supplemented with feature string, recompute and
   * return feature bits.
   * @param cpu
   * @param fs
   * @return
   */
  public long reInitMCSubtargetInfo(String cpu, String fs) {
    SubtargetFeatures features = new SubtargetFeatures(fs);
    featureBits = features.getBits(procDesc, procFeatures);
    return featureBits;
  }

  /**
   *
   * @param fb
   * @return
   */
  public long toggleFeature(long fb) {
    featureBits ^= fb;
    return featureBits;
  }

  /**
   * Toggle a feature and return the re-computed feature bits. This version
   * will also change all implied bits.
   * @param fs
   * @return
   */
  public long toggleFeature(String fs) {
    SubtargetFeatures features = new SubtargetFeatures("");
    featureBits = features.toggleFeature(featureBits, fs, procFeatures);
    return featureBits;
  }

  /**
   * Get scheduling itinerary of a CPU.
   * @param cpu
   * @return
   */
  public InstrItineraryData getInstrItineraryForCPU(String cpu) {
    Util.assertion(procItins != null, "Instruction itineraries information not available!");
    SubtargetInfoKV found = null;
    for (SubtargetInfoKV kv : procItins) {
      if (kv.key.equals(cpu)) {
        found = kv;
        break;
      }
    }

    if (found == null) {
      System.err.printf("'%s' is not a recognized processor for this target (ignoring processor)\n", cpu);
      return new InstrItineraryData();
    }
    return new InstrItineraryData(stages, operandCycles,
        forwardingPathes, (InstrItinerary[]) found.value);
  }
}
