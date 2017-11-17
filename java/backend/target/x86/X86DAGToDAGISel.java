package backend.target.x86;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
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

import backend.codegen.selectDAG.SelectionDAGISel;
import backend.target.TargetMachine.CodeGenOpt;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class X86DAGToDAGISel extends SelectionDAGISel
{
    private X86TargetLowering x86Lowering;

    private X86Subtarget subtaraget;

    private boolean optForSize;

    public X86DAGToDAGISel(X86TargetMachine tm, CodeGenOpt optLevel)
    {
        super(tm, optLevel);
        x86Lowering = tm.getTargetLowering();
        subtaraget = tm.getSubtarget();
        optForSize = false;
    }

    @Override
    public String getPassName()
    {
        return "X86 DAG->DAG Instruction Selection";
    }

    public static X86DAGToDAGISel createX86ISelDag(X86TargetMachine tm, CodeGenOpt level)
    {
        return new X86DAGToDAGISel(tm, level);
    }
}
