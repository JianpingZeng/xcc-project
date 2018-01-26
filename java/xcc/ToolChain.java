/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2017, Xlous zeng.
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

package xcc;

import backend.support.Triple;

import javax.tools.Tool;
import java.util.ArrayList;

public abstract class ToolChain
{
    private HostInfo host;
    private Triple triple;
    private ArrayList<String> filePaths;
    private ArrayList<String> programPaths;

    public ToolChain(HostInfo host, Triple triple)
    {
        this.host = host;
        this.triple = triple;
        filePaths = new ArrayList<>();
        programPaths = new ArrayList<>();
    }

    public String getTripleString()
    {
        return triple.getTriple();
    }

    public ArrayList<String> getFilePaths()
    {
        return filePaths;
    }

    public ArrayList<String> getProgramPaths()
    {
        return programPaths;
    }

    public Triple getTriple()
    {
        return triple;
    }

    public HostInfo getHost()
    {
        return host;
    }

    public String getArchName()
    {
        return triple.getArchName();
    }

    public String getPlatform()
    {
        return triple.getVendorName();
    }

    public String getOS()
    {
        return triple.getOSName();
    }

    public abstract Tool selectTool(Compilation c, Action.JobAction ja);
}
