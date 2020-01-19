/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 *
 * Please refer the LICENSE for detail.
 */

package backend.mc;
/**
 * These values represent a non-pipelined step in
 * the execution of an instruction.  Cycles represents the number of
 * discrete time slots needed to complete the stage.  Units represent
 * the choice of functional units that can be used to complete the
 * stage.  Eg. IntUnit1, IntUnit2. NextCycles indicates how many
 * cycles should elapse from the start of this stage to the start of
 * the next stage in the itinerary. A value of -1 indicates that the
 * next stage should start immediately after the current one.
 * For example:
 * <p>
 * { 1, x, -1 }
 * indicates that the stage occupies FU x for 1 cycle and that
 * the next stage starts immediately after this one.
 * <p>
 * { 2, x|y, 1 }
 * indicates that the stage occupies either FU x or FU y for 2
 * consecuative cycles and that the next stage starts one cycle
 * after this stage starts. That is, the stage requirements
 * overlap in time.
 * <p>
 * { 1, x, 0 }
 * indicates that the stage occupies FU x for 1 cycle and that
 * the next stage starts in this same cycle. This can be used to
 * indicate that the instruction requires multiple stages at the
 * same time.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public final class InstrStage {
  /**
   * Length of stage in machine cycles
   */
  private int cycles;
  /**
   * Choice of functional units.
   */
  private int units;
  /**
   * Number of machine cycles to next stage.
   */
  private int nextCycles;

  public InstrStage(int c, int u, int next) {
    cycles = c;
    units = u;
    nextCycles = next;
  }

  public int getCycles() {
    return cycles;
  }

  public int getUnits() {
    return units;
  }

  public int getNextCycles() {
    return nextCycles >= 0 ? nextCycles : cycles;
  }
}
