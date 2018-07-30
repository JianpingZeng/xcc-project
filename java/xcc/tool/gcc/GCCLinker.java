/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2017, Jianping Zeng.
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

package xcc.tool.gcc;

import backend.support.Triple;
import xcc.*;
import xcc.tool.Tool;

import java.util.ArrayList;

import static xcc.OptionID.*;

public class GCCLinker extends Tool
{
    public GCCLinker(ToolChain tc)
    {
        super("linux::link", "link", tc);
    }

    @Override
    public Job constructJob(Compilation c, Action.JobAction ja,
            InputInfo output, ArrayList<InputInfo> inputs, ArgList args,
            String linkerOutput)
    {
        LinuxToolChain toolChain = (LinuxToolChain)getToolChain();
        Driver driver = toolChain.getDriver();

        ArrayList<String> cmdArgs = new ArrayList<>();
        if (args.hasArg(OPT__S))
            cmdArgs.add("-s");

        cmdArgs.addAll(toolChain.getExtraOpts());

        if (!args.hasArg(OPT__static))
            cmdArgs.add("--eh-frame-hdr");

        cmdArgs.add("-m");
        if (toolChain.getArch() == Triple.ArchType.x86)
            cmdArgs.add("elf_i386");
        else if (toolChain.getArch() == Triple.ArchType.x86_64)
            cmdArgs.add("elf_x86_64");

        if (args.hasArg(OPT__static))
            cmdArgs.add("-static");
        else if (args.hasArg(OPT__shared))
            cmdArgs.add("-shared");

        if (!args.hasArg(OPT__static) && !args.hasArg(OPT__shared))
        {
            if (toolChain.getArch() == Triple.ArchType.x86)
                cmdArgs.add("/lib/ld-linux-so.2");
            else
                cmdArgs.add("/lib64/ld-linux-x86-64-so.2");
        }
        cmdArgs.add("-o");
        cmdArgs.add(output.getFilename());

        if (!args.hasArg(OPT__nostdlib) && !args.hasArg(OPT__nostartfiles))
        {
            String crt1 = null;
            if (!args.hasArg(OPT__shared))
            {
                if (args.hasArg(OPT__pie))
                    crt1= "Scrt1.o";
                else
                    crt1 = "crt1.o";
            }
            if (crt1 != null)
            {
                cmdArgs.add(toolChain.getFilePath(crt1));
            }
            cmdArgs.add(toolChain.getFilePath("crti.o"));

            String crtBegin = null;
            if (args.hasArg(OPT__static))
                crtBegin = "crtbeginT.o";
            else if (args.hasArg(OPT__shared) || args.hasArg(OPT__pie))
                crtBegin = "crtbeginS.o";
            else
                crtBegin = "crtbegin.o";
            cmdArgs.add(toolChain.getFilePath(crtBegin));
        }

        args.addAllArgs(cmdArgs, OPT__L);

        ArrayList<String> paths = toolChain.getFilePaths();
        for (String path : paths)
            cmdArgs.add("-L" + path);

        addLinkerInputs(inputs, args, cmdArgs);

        if (!args.hasArg(OPT__nostdlib))
        {
            if (args.hasArg(OPT__static))
                cmdArgs.add("--start=group");

            cmdArgs.add("-lgcc");

            if (!args.hasArg(OPT__static))
            {
                cmdArgs.add("--as-needed");
                cmdArgs.add("-lgcc_s");
                cmdArgs.add("--no-as-needed");
            }

            if (args.hasArg(OPT__static))
                cmdArgs.add("-lgcc_eh");
            if (args.hasArg(OPT__pthread) || args.hasArg(OPT__pthreads))
                cmdArgs.add("-lpthread");

            cmdArgs.add("-lc");

            if (args.hasArg(OPT__static))
                cmdArgs.add("--end-group");
            else
            {
                cmdArgs.add("-lgcc");
                cmdArgs.add("--as-needed");
                cmdArgs.add("-lgcc_s");
                cmdArgs.add("--no-as-needed");
            }

            if (!args.hasArg(OPT__nostartfiles))
            {
                String crtEnd = null;
                if (args.hasArg(OPT__shared) || args.hasArg(OPT__pie))
                    crtEnd = "crtendS.o";
                else
                    crtEnd = "crtend.o";

                cmdArgs.add(toolChain.getFilePath(crtEnd));
                cmdArgs.add(toolChain.getFilePath("crtn.o"));
            }
        }

        return new Job.Command(ja, toolChain.getLinker(), cmdArgs);
    }

    private static void addLinkerInputs(ArrayList<InputInfo> inputs,
            ArgList args, ArrayList<String> cmdArgs)
    {
        args.addAllArgValues(cmdArgs, OPT__Zlinker_input);

        for (InputInfo ii : inputs)
        {
            if (ii.isFilename())
            {
                cmdArgs.add(ii.getFilename());
                continue;
            }

            Arg arg = ii.getInputArg();
            arg.renderAsInput(args, cmdArgs);
        }
    }
}
