package backend.target;
/*
 * Extremely Compiler Collection
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

/**
 * An itinerary represents the scheduling
 * information for an instruction. This includes a set of stages
 * occupies by the instruction, and the pipeline cycle in which
 * operands are read and written.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public final class InstrItinerary {
  /**
   * The number of micro ops generated when this instruction is decoded.
   */
  public int numMicroOps;
  /**
   * Index of first stage in itinerary
   */
  public int firstStage;
  /**
   * Index of last + 1 stage in itinerary
   */
  public int lastStage;
  /**
   * Index of first operand rd/wr
   */
  public int firstOperandCycle;
  /**
   * Index of last + 1 operand rd/wr
   */
  public int lastOperandCycle;

  public InstrItinerary(int numMicroOps, int firstStg, int lastStg, int firstOpCycle, int lastOpCycle) {
    this.numMicroOps = numMicroOps;
    firstStage = firstStg;
    lastStage = lastStg;
    firstOperandCycle = firstOpCycle;
    lastOperandCycle = lastOpCycle;
  }

  public InstrItinerary() {
    this(0, 0, 0, 0, 0);
  }
}
