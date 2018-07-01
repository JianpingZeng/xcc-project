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

import tools.Util;
import backend.support.Triple;
import xcc.Action.ActionClass;
import xcc.HostInfo.LinuxHostInfo;
import xcc.tool.Tool;
import xcc.tool.gcc.GCCAssembler;
import xcc.tool.gcc.GCCInstallationDetector;
import xcc.tool.gcc.GCCLinker;
import xcc.tool.jlang.JlangTool;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class LinuxToolChain extends ToolChain
{
    private ArrayList<String> extraOpts;
    private HashMap<ActionClass, Tool> tools;
    private GCCInstallationDetector gccInstallation;

    public LinuxToolChain(LinuxHostInfo linuxHostInfo, Triple triple)
    {
        super(linuxHostInfo, triple);
        extraOpts = new ArrayList<>();
        tools = new HashMap<>();
        gccInstallation = new GCCInstallationDetector(getDriver());

        getProgramPaths().addAll(getDriver().getInstalledDir());
        if (getDriver().getDir() != getDriver().getInstalledDir())
            getProgramPaths().addAll(getDriver().getDir());

        ArrayList<String> programPaths = getProgramPaths();
        addPathIfExists(gccInstallation.getGccInstallPath() + "/bin", programPaths);
        addPathIfExists("/usr/bin", programPaths);
        addPathIfExists("/usr/local/bin", programPaths);
        addPathIfExists("/bin", programPaths);
        addPathIfExists("/sbin", programPaths);

        extraOpts.add("-z");
        boolean is32Bit = getArch() == Triple.ArchType.x86;

        String suffix32 = getArch() == Triple.ArchType.x86_64? "/32":"";
        String suffix64 = getArch() == Triple.ArchType.x86_64? "":"/64";
        String suffix = is32Bit? suffix32: suffix64;
        String multilib = is32Bit?"lib32":"lib64";
        String multiArchTriple = getMultiArchTriple(triple);
        ArrayList<String> paths = getFilePaths();
        if (gccInstallation.isValid())
        {
            String libPath = gccInstallation.getGccParentLibPath();
            String gccTriple = gccInstallation.getGccTriple();

            addPathIfExists(gccInstallation.getGccInstallPath() + suffix, paths);
            addPathIfExists(libPath + "/../" + gccTriple + "/lib/../" + multilib,
                    paths);
            addPathIfExists( libPath+ "/" + multiArchTriple, paths);
            addPathIfExists(libPath + "/../" + multilib, paths);
        }
        addPathIfExists("/lib/" + multiArchTriple, paths);
        addPathIfExists("/lib../" + multiArchTriple, paths);
        addPathIfExists("/usr/lib/" + multiArchTriple, paths);
        addPathIfExists("/usr/lib../" + multiArchTriple, paths);

        if (gccInstallation.isValid())
            addPathIfExists("/usr/lib/" + gccInstallation.getGccTriple()
                            + "../../" + multilib, paths);

        if (gccInstallation.isValid())
        {
            String libPath = gccInstallation.getGccParentLibPath();
            String gccTriple = gccInstallation.getGccTriple();
            if (!suffix.isEmpty())
                addPathIfExists(gccInstallation.getGccInstallPath(), paths);

            addPathIfExists(libPath + "/../" + gccTriple + "/lib", paths);
            addPathIfExists(libPath + "/" + multiArchTriple, paths);
            addPathIfExists(libPath, paths);

        }
        addPathIfExists("/lib/" + multiArchTriple, paths);
        addPathIfExists("/lib", paths);
        addPathIfExists("/usr/lib/" + multiArchTriple, paths);
        addPathIfExists("/usr/lib", paths);
    }

    private void addPathIfExists(String path, ArrayList<String> paths)
    {
        if (Files.exists(Paths.get(path)))
            paths.add(path);
    }

    private String getMultiArchTriple(Triple triple)
    {
        switch (triple.getArch())
        {
            case x86:
                if (Files.exists(Paths.get("/lib/i686-linux-gnu")))
                    return "i686-linux-gnu";
                if (Files.exists(Paths.get("/lib/i386-linux-gnu")))
                    return "i386-linux-gnu";
                return triple.getTriple();
            case x86_64:
                if (Files.exists(Paths.get("/lib/x86_64-linux-gnu")))
                    return "x86_64-linux-gnu";
                if (Files.exists(Paths.get("/lib/x86_64-pc-linux-gnu")))
                    return "x86_64-pc-linux-gnu";
                if (Files.exists(Paths.get("/lib/x86_64-unknown-linux-gnu")))
                    return "x86_64-unknown-linux-gnu";
                return triple.getTriple();
            default:
                return triple.getTriple();
        }
    }

    @Override
    public Tool selectTool(Compilation c, Action.JobAction ja)
    {
        ActionClass key;
        if (c.getDriver().useJlangAsCompiler(c, ja, getArchName()))
            key = ActionClass.CompileJobClass;
        else
            key = ja.getKind();

        if (tools.containsKey(key))
            return tools.get(key);

        Tool tool = null;
        switch (key)
        {
            case InputClass:
            case BindArchClass:
            case PreprocessJobClass:
            case PrecompileJobClass:
            default:
                Util.assertion(false, "Invalid tool kind!");
                break;
            case CompileJobClass:
                tool = new JlangTool(this);
                break;
            case AssembleJobClass:
                tool = new GCCAssembler(this);
                break;
            case LinkJobClass:
                tool = new GCCLinker(this);
                break;
        }
        tools.put(key, tool);
        return tool;
    }

    @Override
    public String getLinker()
    {
        return getProgramPath("ld");
    }

    @Override
    public String getAssembler()
    {
        return getProgramPath("as");
    }

    @Override
    public String getForcedPicModel()
    {
        return null;
    }

    @Override
    public String getDefaultRelocationModel()
    {
        return "static";
    }

    @Override
    public void addSystemIncludeDir(ArrayList<String> cmdStrings)
    {
        cmdStrings.add("-I");
        cmdStrings.add(gccInstallation.getGccInstallPath() + "/include");
    }

    public ArrayList<String> getExtraOpts()
    {
        return extraOpts;
    }
}
