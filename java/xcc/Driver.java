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

    public OptTable getOptTable()
    {
        return optTable;
    }

    private InputArgList parseArgList(String[] args)
    {
        InputArgList args = new InputArgList(args);
        for (int index = 0, sz = args.getInputStrings(); index < sz;)
        {
          if (args.getArgString(index).isEmpty())
          {
            ++index;
            continue;
          }

          int prev = args.getIndex();
          Arg arg = getOptTable().parseOneArg(args);
          int after = args.getIndex();
          assert after >= prev;
          if (arg == null)
          {
              // TODO
              diag().emit();
              continue;
          }
          if (arg.getOption().isNotSupported())
          {
              diag().emit();
              continue;
          }
          args.add(arg);
      }
        return args;
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
  
    /**
     * Constructs an Action for each Compilation phase as follow.
     */
    private Action constructAction(CompilationPhase phase, Action input)
    {
        if (phase == Preprocess)
        {
            return new PreprocessAction(input, TY_Preprocess);
        }
        else if (phase == Compile)
        {
            return new CompileAction(input, TY_Assemble);            
        }
        else if (phase == Assembly)
        {
            return new AssemblyAction(input, TY_Object);
        }
        else 
        {
            assert phase == Linking;
            assert false:"Linking should be handled in method buildActions!";
            return null;
        }
    }

    private void buildActions(Compilation c)
    {
        ArgList args = c.getArgs();
        ToolChain tc = c.getToolChain();
        ArrayList<Option> linkerInputs = new ArrayList<>();

        ArrayList<Pair<Arg, InputFileType>> inputs = new ArrayList<>();

        for (int i = 0, e = args.size(); i < e; i++)
        {
            Option opt = args.getOption(i);
            if (opt == null) continue;

            if (opt instanceof InputOption)
            {
                InputOption io = (InputOption)opt;

            }
            else if (opt.isLinkerInput())
            {
            }
            else if ()
        }

        // Compute the final compilatio phase.
        CompilationPhase finalPhase;
        if (args.hasArg(OPT__e_))
        {
            finalPhase = Preprocess;
        }
        else if (args.hasArg(OPT__c_))
        {
            finalPhase = Assembly;
        }
        else if (args.hasArg(OPT__S_))
        {
            if (args.hasArg(OPT__emit_llvm_))
              finalPhase = Compile;
            else 
              finalPhase = Assembly;
        }
        else 
          // Other cases which we always treat as linker input.
          finalPhase = Linking;

        for (Pair<Arg, InputFileType> entity : inputs)
        {
            InputFileType filetype = entity.second;

            int numSteps = computeCompilationSteps(filetype, 0);
            CompilationPhase initialPhase = computeInitialPhase(filetype);

            if (initialPhase > finalPhase)
            {
                diag(unused_file).emit();
                continue;
            }

            InputAction current = new InputAction(arg.getValue(), filetype);
            for (int i = 0; i < numSteps; i++)
            {
                if (i + initialPhase > finalPhase)
                  break;

                if(i + initialPhase === Linking)
                  linkerInputs.add(current);

                current = constructAction(i+initialPhase, current);
            }
            if (current != null)
              c.addAction(current);
        }
        if (!linkerInputs.isEmpty())
            c.addAction(new LinkerAction(linkerInputs, TY_Image));
    }

    private void buildJobs(Compilation c)
    {}

    public Compilation buildCompilation(String[] args)
    {
        InputArgList argList = parseArgList(args);
        host = getHostInfo(triple);

        Compilation c = new Compilation(this, host.selectToolChain(argList), argList);
      
        // Builds a sequence of Actions to be performed, like preprocess,
        // precompile, compile, assembly, linking etc.        
        buildActions(c);

        buildJobs(c);
        return c;
    }

    public int executeCompilation(Compilation c)
    {
        OutPutPrameterWrapper<Command> failureCmd = new OutPutPrameterWrapper<>();

        int res = c.executeCommands(failureCmd);
        if (res != 0)
        {
           clearTemporaryFiles();
        }

        if (res != 0)
        {
          diag().emit();
        }
        return res;
    }
}
