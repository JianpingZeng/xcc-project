/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package backend.support;

import backend.pass.AnalysisResolver;
import backend.pass.BasicBlockPass;
import backend.pass.Pass;
import backend.value.BasicBlock;

import java.io.PrintStream;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class PrintBasicBlockPass implements BasicBlockPass
{
    private AnalysisResolver resolver;
    private PrintStream os;
    private String banner;

    public PrintBasicBlockPass(PrintStream os, String banner)
    {
        this.os = os;
        this.banner = banner;
    }

    @Override
    public boolean runOnBasicBlock(BasicBlock block)
    {
        os.print(banner);
        block.print(os);
        return false;
    }

    @Override
    public String getPassName()
    {
        return "Print Basic Block Pass";
    }

    @Override
    public AnalysisResolver getAnalysisResolver()
    {
        return resolver;
    }

    @Override
    public void setAnalysisResolver(AnalysisResolver resolver)
    {
        this.resolver = resolver;
    }

    public static Pass createPrintBasicBlockPass(PrintStream os, String banner)
    {
        return new PrintBasicBlockPass(os, banner);
    }
}
