package backend.codegen;
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

/**
 * This struct carries flags and a value for a
 * single outgoing (actual) argument or outgoing (from the perspective
 * of the caller) return value virtual register.
 * @author Xlous.zeng
 * @version 0.1
 */
public class OutputArg
{
    public ArgFlagsTy flags;
    public EVT vt;
    public boolean used;

    public OutputArg()
    {
        vt = new EVT(MVT.Other);
        used = false;
    }

    public OutputArg(ArgFlagsTy flags, EVT vt, boolean used)
    {
        this.flags = flags;
        this.vt = vt;
        this.used = used;
        assert vt.isSimple() :"OutputArg value type must be simple!";
    }
}
