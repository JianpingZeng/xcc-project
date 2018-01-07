package utils.tablegen;
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

import java.util.ArrayList;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class DAGInstruction
{
    private TreePattern pattern;
    private ArrayList<Record> results;
    private ArrayList<Record> operands;
    private ArrayList<Record> impResults;
    private ArrayList<Record> impOperands;
    private TreePatternNode resultPattern;

    public DAGInstruction(TreePattern tp,
            ArrayList<Record> results,
            ArrayList<Record> operands,
            ArrayList<Record> impResults,
            ArrayList<Record> impOperands)
    {
        pattern = tp;
        this.results = results;
        this.operands = operands;
        this.impResults = impResults;
        this.impOperands = impOperands;
    }

    public TreePattern getPattern()
    {
        return pattern;
    }

    public int getNumResults()
    {
        return results.size();
    }

    public int getNumOperands()
    {
        return operands.size();
    }

    public int getNumImpResults()
    {
        return impResults.size();
    }

    public int getNumImpOperands()
    {
        return impOperands.size();
    }

    public ArrayList<Record> getImpResults()
    {
        return impResults;
    }

    public void setResultPattern(TreePatternNode resultPattern)
    {
        this.resultPattern = resultPattern;
    }

    public TreePatternNode getResultPattern()
    {
        return resultPattern;
    }

    public Record getResult(int idx)
    {
        assert idx >= 0 && idx < results.size();
        return results.get(idx);
    }

    public Record getOperand(int idx)
    {
        assert idx >= 0 && idx < operands.size();
        return operands.get(idx);
    }

    public Record getImpResult(int idx)
    {
        assert idx >= 0 && idx < impResults.size();
        return impResults.get(idx);
    }

    public Record getImpOperand(int idx)
    {
        assert idx >= 0 && idx < impOperands.size();
        return impOperands.get(idx);
    }
}
