package backend.codegen;
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

import backend.target.TargetRegisterClass;

/**
 * The CalleeSavedInfo class tracks the information need to locate where a
 * callee saved register in the current frame.
 *
 * @author Jianping Zeng
 * @version 0.1
 */
public class CalleeSavedInfo
{

    private int reg;
    private TargetRegisterClass regClass;
    private int frameIdx;

    public CalleeSavedInfo(int r, TargetRegisterClass rc)
    {
        this(r, rc, 0);
    }

    public CalleeSavedInfo(int r, TargetRegisterClass rc, int fi)
    {
        reg = r;
        regClass = rc;
        frameIdx = fi;
    }

    // Accessors.
    public int getReg()
    {
        return reg;
    }

    public TargetRegisterClass getRegisterClass()
    {
        return regClass;
    }

    public int getFrameIdx()
    {
        return frameIdx;
    }

    public void setFrameIdx(int FI)
    {
        frameIdx = FI;
    }
}
