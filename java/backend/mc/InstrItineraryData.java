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

/**
 * Itinerary data supplied by a subtarget to be used by a target.
 * @author Jianping Zeng.
 * @version 0.4
 */
public class InstrItineraryData {
  public InstrStage[] stages;
  public int[] operandCycles;
  public int[] forwardCycles;
  public InstrItinerary[] itineraries;
  /**
   * The max issued number of instructions per cycle. o indicates unknown.
   */
  public int issueWidth;

  public InstrItineraryData() { }
  public InstrItineraryData(InstrStage[] s, int[] oc, int[] fc,
                            InstrItinerary[] ii) {
    stages = s;
    operandCycles = oc;
    forwardCycles = fc;
    itineraries = ii;
    issueWidth = 0;
  }

  public boolean isEmpty() {
    return itineraries == null;
  }

  public InstrStage beginStage(int itinClassIndex) {
    int stageIdx = itineraries[itinClassIndex].firstStage;
    return stages[stageIdx];
  }

  public InstrStage endStage(int itinClassIndex) {
    int stageIdx = itineraries[itinClassIndex].lastStage;
    return stages[stageIdx];
  }


}
