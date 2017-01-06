package backend.transform;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2016, Xlous
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http:*www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

import backend.analysis.DomTreeInfo;
import backend.analysis.Loop;
import backend.analysis.LoopInfo;
import backend.analysis.ScalarEvolution;
import backend.pass.AnalysisUsage;
import backend.pass.LoopPass;

/**
 * This transformation analyzes and transforms the induction variables (and
 * computations derived from them) into simpler forms suitable for subsequent
 * analysis and transformation.
 *
 * This transformation make the following changes to each loop with an
 * identifiable induction variable:
 *   1. All loops are transformed to have a SINGLE canonical induction variable
 *      which starts at zero and steps by one.
 *   2. The canonical induction variable is guaranteed to be the first PHI node
 *      in the loop header block.
 *   3. Any pointer arithmetic recurrences are raised to use array subscripts.
 *
 * If the trip count of a loop is computable, this pass also makes the following
 * changes:
 *   1. The exit condition for the loop is canonicalized to compare the
 *      induction value against the exit value.  This turns loops like:
 *        'for (i = 7; i*i < 1000; ++i)' into 'for (i = 0; i != 25; ++i)'
 *   2. Any use outside of the loop of an expression derived from the indvar
 *      is changed to compute the derived value outside of the loop, eliminating
 *      the dependence on the exit value of the induction variable.  If the only
 *      purpose of the loop is to compute the exit value of some derived
 *      expression, this transformation will make the loop dead.
 *
 * This transformation should be followed by strength reduction after all of the
 * desired loop transformations have been performed.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class IndVarSimplify extends LoopPass
{
    private LoopInfo li;
    private ScalarEvolution se;
    private boolean changed = false;

    @Override
    public boolean runOnLoop(Loop loop)
    {
        li = getAnalysisToUpDate(LoopInfo.class);
        se = getAnalysisToUpDate(ScalarEvolution.class);

        return changed;
    }

    @Override
    public String getPassName()
    {
        return "Induction variable simplification pass";
    }

    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        au.addRequired(LoopSimplify.class);
        au.addRequired(DomTreeInfo.class);
        au.addRequired(LoopInfo.class);
        au.addRequired(ScalarEvolution.class);
    }
}
