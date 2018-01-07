package backend.target;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
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
 * These values represent a non-pipelined step in
 * the execution of an instruction.  Cycles represents the number of
 * discrete time slots needed to complete the stage.  Units represent
 * the choice of functional units that can be used to complete the
 * stage.  Eg. IntUnit1, IntUnit2. NextCycles indicates how many
 * cycles should elapse from the start of this stage to the start of
 * the next stage in the itinerary. A value of -1 indicates that the
 * next stage should start immediately after the current one.
 * For example:
 *
 *   { 1, x, -1 }
 *      indicates that the stage occupies FU x for 1 cycle and that
 *      the next stage starts immediately after this one.
 *
 *   { 2, x|y, 1 }
 *      indicates that the stage occupies either FU x or FU y for 2
 *      consecuative cycles and that the next stage starts one cycle
 *      after this stage starts. That is, the stage requirements
 *      overlap in time.
 *
 *   { 1, x, 0 }
 *      indicates that the stage occupies FU x for 1 cycle and that
 *      the next stage starts in this same cycle. This can be used to
 *      indicate that the instruction requires multiple stages at the
 *      same time.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public final class InstrStage
{
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

    public InstrStage(int c, int u, int next)
    {
        cycles = c;
        units = u;
        nextCycles = next;
    }

    public int getCycles()
    {
        return cycles;
    }

    public int getUnits()
    {
        return units;
    }

    public int getNextCycles()
    {
        return nextCycles >= 0 ? nextCycles : cycles;
    }
}
