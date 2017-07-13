package backend.pass;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Xlous
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

import backend.value.Loop;
import backend.analysis.LoopInfo;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public interface LoopPass extends Pass
{
    @Override
    default void getAnalysisUsage(AnalysisUsage au)
    {
        au.addRequired(LoopInfo.class);
    }

    boolean runOnLoop(Loop loop, LPPassManager ppm);

    default boolean doInitialization(Loop loop, LPPassManager ppm)
    {
        return false;
    }

    default boolean doFinalization() { return false; }
}
