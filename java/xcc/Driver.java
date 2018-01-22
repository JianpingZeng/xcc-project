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

import jlang.diag.Diagnostic;
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

    public Driver(String basename, String dirname,
            String hostTriple, String imageName, Diagnostic diags)
    {
        defaultBasename = basename;
        defaultDirname = dirname;
        defaultImagename = imageName;
        theDiags = diags;
    }

    public Compilation buildComilation(String[] args)
    {
        return null;
    }

    public int executeCompilation(Compilation compilation)
    {
        return 0;
    }
}
