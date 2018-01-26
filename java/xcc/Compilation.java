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

import xcc.Job.Command;
import xcc.Job.PipedJob;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class Compilation
{
    private Driver driver;
    private ToolChain tc;
    private ArgList args;
    private ArrayList<String> tempFiles;
    private ArrayList<String> resultFiles;
    private Command failureCmd;
    private ArrayList<Job> jobs;
    private ArrayList<Action> actions;

    public Compilation(Driver driver, ToolChain tc, ArgList args)
    {
        this.driver = driver;
        this.tc = tc;
        this.args = args;
        tempFiles = new ArrayList();
        resultFiles = new ArrayList();
        jobs = new ArrayList<>();
        actions = new ArrayList<>();
    }

    /**
     * Eexecute a specified executable with some program arguments.
     */
    private int executeCommand(Command cmd)
    {
        String progPath = cmd.getExecutable();
        ArrayList<String> args = cmd.getArgs();
        StringBuilder sb = new StringBuilder(progPath);
        for (String arg : args)
        {
            sb.append(" ");
            sb.append(arg);
        }
        try
        {
            Process p = Runtime.getRuntime().exec(sb.toString());
            int res = p.waitFor();
            return res;
        }
        catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
            return -1;
        }
    }

    private int executeSingleJob(Job j)
    {
        if (j instanceof Command)
              return executeCommand((Command)j);
        else if (j instanceof Job.JobList)
        {
            Job.JobList jlist = (Job.JobList)j;
            int res = 0;
            for (Job job : jlist.getJobs())
            {
                res |= executeSingleJob(job);
            }
            return res;
        }
        else
        {
            assert j instanceof PipedJob;
            assert false:"Currently PipedJob not supported!";
            return 0;
        }
    }

    public int executeJob()
    {
        int res = 0;
        for (Job j : jobs)
        {
            res |= executeSingleJob(j);
        }
        return res;
    }

    public String addTempFile(String file)
    {
        assert file != null && !file.isEmpty():"No valid filename";
        assert !tempFiles.contains(file):"Can't add a exist temporary file!";
        tempFiles.add(file);
        return file;
    }

    public void addJob(Job job)
    {
        if (jobs.contains(job))
          return;
        jobs.add(job);
    }

    public ArrayList<Job> getJobs()
    {
        return jobs;
    }

    public void addAction(Action act)
    {
        actions.add(act);
    }
    
    public ArrayList<Action> getActions()
    {
        return actions;
    }

    public Driver getDriver()
    {
        return driver;
    }

    public ArgList getArgs()
    {
        return args;
    }

    public ToolChain getToolChain()
    {
        return tc;
    }

    public Command getFailureCommand()
    {
        return failureCmd;
    }

    public String addResultFile(String filename)
    {
        assert !((filename == null || filename.isEmpty()
                || resultFiles.contains(filename)));
        resultFiles.add(filename);
        return filename;
    }

    public void clearTemporaryFiles()
    {
        for (String file : tempFiles)
        {
            Path p = Paths.get(file);
            if (Files.exists(p))
            {
                try
                {
                    Files.delete(p);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
}
