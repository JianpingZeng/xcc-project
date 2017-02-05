package backend.transform;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2017, Xlous
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

import backend.analysis.*;
import backend.pass.AnalysisUsage;
import backend.pass.LoopPass;
import backend.pass.Pass;
import backend.pass.RegisterPass;

/**
 * <p>
 * This transformation analyzes and transforms the induction variables (and
 * computations derived from them) into simpler forms suitable for subsequent
 * analysis and transformation.
 * </p>
 * <p>
 * This transformation make the following changes to each loop with an
 * identifiable induction variable:
 *  <ol>
 *      <li>
 *      All loops are transformed to have a SINGLE canonical induction variable
 *      which starts at zero and steps by one.
 *      </li>
 *      <li>The canonical induction variable is guaranteed to be the first PHI node
 *      in the loop header block.
 *      </li>
 *      <li>
 *       Any pointer arithmetic recurrences are raised to use array subscripts.
 *      </li>
 *   </ol>
 * </p>
 * If the trip count of a loop is computable, this pass also makes the following
 * changes:
 * <ol>
 *     <li>
 *         The exit condition for the loop is canonicalized to compare the
 *      induction value against the exit value.  This turns loops like:
 *        <pre>for (i = 7; i*i < 1000; ++i)</pre>
 *        into
 *        <pre>for (i = 0; i != 25; ++i)</pre>
 *     </li>
 *     <li>
 *      Any use outside of the loop of an expression derived from the indvar
 *      is changed to compute the derived value outside of the loop, eliminating
 *      the dependence on the exit value of the induction variable.  If the only
 *      purpose of the loop is to compute the exit value of some derived
 *      expression, this transformation will make the loop dead.
 *     </li>
 * </ol>
 *
 * <p>
 * This transformation should be followed by strength reduction after all of the
 * desired loop transformations have been performed.
 * </p>
 * @author Xlous.zeng
 * @version 0.1
 */
public final class IndVarSimplify extends LoopPass
{
    private LoopInfo li;
    private ScalarEvolution se;
    private boolean changed = false;

    public static final RegisterPass X =
            new RegisterPass("Canonicalize Induction Variables", IndVarSimplify.class);

    public static Pass createIndVarSimplifyPass()
    {
        return new IndVarSimplify();
    }

	/**
     * Sealed with private accessibility.
     */
    private IndVarSimplify()
    {
        super();
    }

    @Override
    public boolean runOnLoop(Loop loop)
    {
        // If the LoopSimplify form is not available, just return early.
        // A LoopSimplify form must having a preheader, a latch block and
        // dedicated exit blocks, which is required for moving induction
        // variable.
        if (!loop.isLoopSimplifyForm())
            return false;

        li = getAnalysisToUpDate(LoopInfo.class);
        se = getAnalysisToUpDate(ScalarEvolution.class);

        // Firstly, transforms all sub loops nested in current loop processed.
        loop.getSubLoops().forEach(this::runOnLoop);

        // Checks to see if this loop has a computable loop-invariant exit expression.
        // If so, this means that we can compute the final value of any expression
        // that recurrent in the loop, and substitute the exit values from the loop
        // into any instruction outside of the loop that use the final value of the
        // current value.

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
        au.addRequired(DomTreeInfo.class);
        au.addRequired(LoopInfo.class);
        au.addRequired(ScalarEvolution.class);
        au.addRequired(LoopSimplify.class);
        au.addRequired(LCSSA.class);
        au.addRequired(IVUsers.class);
        au.addPreserved(ScalarEvolution.class);
        au.addPreserved(LoopSimplify.class);
        au.addPreserved(LCSSA.class);
        au.addPreserved(IVUsers.class);
    }
}
