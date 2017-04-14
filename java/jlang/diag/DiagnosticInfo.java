package jlang.diag;
/*
 * Extremely C language Compiler.
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

import jlang.diag.Diagnostic.ArgumentKind;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class DiagnosticInfo
{
    private final Diagnostic diagObj;

    public DiagnosticInfo(Diagnostic diagObj)
    {
        this.diagObj = diagObj;
    }

    public Diagnostic getDiagObj()
    {
        return diagObj;
    }

    public int getID()
    {
        return diagObj.getID();
    }

    public FullSourceLoc getLocation()
    {
        return diagObj.getCurDiagLoc();
    }

    public int getNumArgs()
    {
        return diagObj.getNumDiagArgs();
    }

    /**
     * Obtain the kind qualified by {@linkplain ArgumentKind} of the specified
     * argument in the specified position.
     * @param index
     * @return
     */
    public ArgumentKind getArgKind(int index)
    {
        assert index >= 0 && index < getNumArgs() :"Argument index out of range!";
        return diagObj.getDiagArgKind(index);
    }

    public String getArgStdStr(int index)
    {
        return diagObj.getArgStdStr(index);
    }

    public int getArgSInt(int index)
    {
        return diagObj.getArgSInt(index);
    }
}
