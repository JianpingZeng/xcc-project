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

package xcc;

import backend.support.Triple;
import jlang.diag.Diagnostic;
import xcc.Action.JobAction;

import static xcc.HostInfo.createLinuxHostInfo;
import static xcc.HostInfo.createUnknownHostInfo;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class Driver
{
    private String defaultBasename;
    private String defaultDirname;
    private String defaultImagename;
    private Diagnostic theDiags;
    private String triple;
    private HostInfo host;
    private OptTable optTable;

    public Driver(String basename, String dirname,
            String hostTriple, String imageName, Diagnostic diags)
    {
        defaultBasename = basename;
        defaultDirname = dirname;
        defaultImagename = imageName;
        theDiags = diags;
        triple = hostTriple;
        optTable = new OptTable();
    }

    private InputArgList parseArgList(String[] args)
    {
        return null;
    }

    private HostInfo getHostInfo(String tripleStr)
    {
        Triple defaultTriple = new Triple(tripleStr);
        if (defaultTriple.getArchName().equals("i686"))
            defaultTriple.setArchName("i386");
        else if (defaultTriple.getArchName().equals("amd64"))
            defaultTriple.setArchName("x86_64");
        else
            assert false:"Unknown architecture name!";
        switch (defaultTriple.getOS())
        {
            case Linux:
                return createLinuxHostInfo(this, defaultTriple);
            default:
                return createUnknownHostInfo(this, defaultTriple);
        }
    }

    public boolean useJlangAsCompiler(Compilation comp, JobAction ja, String archName)
    {
        return true;
    }

    public Compilation buildCompilation(String[] args)
    {
        InputArgList argList = parseArgList(args);
        host = getHostInfo(triple);

        return null;
    }

    public int executeCompilation(Compilation compilation)
    {
        return 0;
    }
}
