package backend.analysis;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2016, Xlous
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

import backend.pass.AnalysisUsage;
import backend.pass.FunctionPass;
import backend.value.Constant;
import backend.value.Function;
import backend.value.Instruction.PhiNode;
import backend.value.Value;

import java.util.HashMap;

/**
 * This class is the main scalar evolution driver. Since client code (intentionally)
 * can't do much the SCEV objects directly, they must query this class for services.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class ScalarEvolution extends FunctionPass
{
    /**
     * This class represent an analyzed expression in the program.  These
     * are reference counted opaque objects that the client is not allowed to
     * do much with directly.
     */
    static class SCEV
    {
        int scevType;
        int refCount;

        void addRef()
        {
            refCount++;
        }

        void dropRef()
        {
            refCount--;
        }
    }

    static class ScalarEvolutionImpl
    {
        /**
         * The being analyzed function.
         */
        Function f;
        /**
         * The loop information for the function being analyzed.
         */
        LoopInfo li;
        /**
         * This SCEV is used to represent unknown trip count and thing.
         */
        SCEV unknownValue;

        /**
         * This is a cache of the scalars we have analyzed as yet.
         */
        HashMap<Value, SCEV> scalars;

        /**
         * Cache the iteration count of the loops for this function as they are
         * computed.
         */
        HashMap<Loop, SCEV> iterationCount;

        /**
         * This map contains the entities for all of the PHI node to the constant.
         * This is reason for avoiding expensive pre-computation of there properties.
         * A instruction map to a null if we can not compute its exit value.
         */
        HashMap<PhiNode, Constant> constantEvolutionLoopExitValue;

        public ScalarEvolutionImpl(Function f, LoopInfo li)
        {
            this.f = f;
            this.li = li;
        }

        public SCEV getSCEV(Value val)
        {
            return null;
        }

        public boolean hasSCEV(Value val)
        {
            return scalars.containsKey(val);
        }

        public void setSCEV(Value val, SCEV s)
        {
            assert scalars.containsKey(val) : "This entry already existed!";
            scalars.put(val, s);
        }
    }

    private ScalarEvolutionImpl impl;

    @Override
    public String getPassName()
    {
        return "Scalar Evolution pass on Function";
    }

    @Override
    public boolean runOnFunction(Function f)
    {
        impl = new ScalarEvolutionImpl(f, getAnalysisToUpDate(LoopInfo.class));
        return false;
    }

    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        au.addRequired(LoopInfo.class);
    }

    public SCEV getSCEV(Value val)
    {
        return impl.getSCEV(val);
    }

    public boolean hasSCEV(Value val)
    {
        return impl.hasSCEV(val);
    }

    public void setSCEV(Value val, SCEV s)
    {
        impl.setSCEV(val, s);
    }
}
