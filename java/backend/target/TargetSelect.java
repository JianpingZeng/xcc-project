/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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

package backend.target;

import backend.pass.PassRegisterationUtility;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public class TargetSelect
{
    protected TargetSelect()
    {
        super();
    }

    public static TargetSelect create()
    {
        // TODO. 2017.8.12
        TargetSelect ts = null;
        try
        {
            String targetName = "X86";
            String className = "";
            if (targetName.equals("X86"))
            {
                className += "backend.target." +
                        targetName.toLowerCase() +
                        "." + targetName;
            }
            className += "TargetSelect";
            Class<?> klass = Class.forName(className);
            ts = (TargetSelect) klass.newInstance();
        }
        catch (IllegalAccessException | InstantiationException | ClassNotFoundException e)
        {
            e.printStackTrace();
        }
        return ts;
    }

    public void InitializeTargetInfo() {}

    public void LLVMInitializeTarget() {}

    /**
     * This method must be called to register all of passes.
     */
    public void registerAllPasses()
    {
        PassRegisterationUtility.registerPasses();
    }
}
