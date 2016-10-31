package backend.hir;
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

import backend.value.Instruction.TerminatorInst;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class SuccIterator implements Iterator<BasicBlock>
{
    private BasicBlock curBB;
    private int idx;
    private TerminatorInst endInst;

    SuccIterator(BasicBlock BB)
    {
        curBB = BB;
        endInst = curBB.getTerminator();
    }

    /**
     * Returns {@code true} if the iteration has more elements.
     * (In other words, returns {@code true} if {@link #next} would
     * return an element rather than throwing an frontend.exception.)
     *
     * @return {@code true} if the iteration has more elements
     */
    @Override
    public boolean hasNext()
    {
        return idx<endInst.getNumOfSuccessors();
    }

    /**
     * Returns the next element in the iteration.
     *
     * @return the next element in the iteration
     * @throws NoSuchElementException if the iteration has no more elements
     */
    @Override
    public BasicBlock next()
    {
        if (idx>= endInst.getNumOfSuccessors())
            throw new NoSuchElementException();

        return ((TerminatorInst)endInst.useAt(idx).getUser()).suxAt(idx);
    }
}
