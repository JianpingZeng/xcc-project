/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
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

package backend.codegen;

/**
 * This file define a pass that takes responsibility for assginming virtual register
 * to physical register by Partitioned Boolean Qruadran Programming method.
 * <p>
 * It maps the register allocation into a PBQP problem and solve it by reduction
 * algorithm. Then map the solution of PBQP back to register assignment.
 * If you want to known more information about PBQP register allocation in details,
 * following two papers are useful to understand the working flow of PBQP algorithm:
 * <ol>
 *  <li>Eckstein, Erik, and E. Eckstein. "Register allocation for irregular architectures.
 * "Joint Conference on Languages, Compilers and TOOLS for Embedded Systems: Software
 * and Compilers for Embedded Systems ACM, 2002:139-148.
 *  </li>
 *  <li>Lang, Hames, and B. Scholz. "Nearly Optimal Register Allocation with PBQP.
 * "Joint Modular Languages Conference Springer Berlin Heidelberg, 2006:346-361.
 *  </li>
 * </ol>
 * </p>
 * @author Xlous.zeng
 * @version 0.1
 */
public final class RegAllocPBQP extends MachineFunctionPass
{
    public static RegAllocPBQP createPBQPRegisterAllocator()
    {
        return new RegAllocPBQP();
    }

    @Override
    public boolean runOnMachineFunction(MachineFunction mf)
    {
        return false;
    }

    @Override
    public String getPassName()
    {
        return "PBQP register allocator Pass";
    }
}
