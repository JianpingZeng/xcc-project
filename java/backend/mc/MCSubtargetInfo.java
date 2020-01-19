/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 *
 * Please refer the LICENSE for detail.
 */

package backend.mc;

import backend.support.Triple;
import backend.target.InstrItinerary;
import backend.target.SubtargetFeatureKV;
import backend.target.SubtargetFeatures;
import tools.Util;

import java.util.Arrays;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class MCSubtargetInfo {
  protected Triple targetTriple;
  private SubtargetFeatureKV[] procFeatures;
  private SubtargetFeatureKV[] procDesc;
  private SubtargetInfoKV[] procItins;
  protected InstrStage[] stages;
  private int[] operandCycles;
  private int[] forwardingPathes;
  private long featureBits;

  protected void initMCSubtargetInfo(String tt,
                                     String cpu,
                                     String fs,
                                     SubtargetFeatureKV[] pf,
                                     SubtargetFeatureKV[] pd,
                                     SubtargetInfoKV[] pi,
                                     InstrStage[] is, /* Instruction pipeline stage array. */
                                     int[] oc, /* OperandCycles array. */
                                     int[] fp  /* ForwardingPathes array. */) {
    targetTriple = new Triple(tt);
    procFeatures = pf;
    procDesc = pd;
    procItins = pi;
    stages = is;
    operandCycles = oc;
    forwardingPathes = fp;
    SubtargetFeatures features = new SubtargetFeatures(fs);
    Arrays.sort(procDesc);
    Arrays.sort(procFeatures);
    featureBits = features.getBits(cpu, procDesc, procFeatures);
  }

  public Triple getTargetTriple() {
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
    featureBits = features.getBits(cpu, procDesc, procFeatures);
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
