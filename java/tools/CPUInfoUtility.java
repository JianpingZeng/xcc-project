package tools;
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

import config.Config;
import java.io.File;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class CPUInfoUtility
{
    static
    {
        File lib = new File(Config.CPUInfoUtility_Path +"/" + System.mapLibraryName(Config.CPUInfoUtility_Name));
        System.load(lib.getAbsolutePath());
    }
    /**
     * This native method is used for getting CPU ID and information by calling
     * inline assembly in C language.
     * @param value The catogery to query
     * @param fourRegs  An array for holding returned four values, EAX, EBX, ECX,
     *                  EDX.
     * @return
     */
    public native static boolean getCpuIDAndInfo(int value, int[] fourRegs);
}
