package backend.mc;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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

import backend.codegen.AsmPrinter;
import backend.target.TargetAsmInfo;
import tools.FormattedOutputStream;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public class MCStreamer
{
    public static MCStreamer createAsmStreamer(
            MCContext context,
            FormattedOutputStream os,
            TargetAsmInfo tai,
            AsmPrinter printer)
    {
        return new MCStreamer();
    }
}
