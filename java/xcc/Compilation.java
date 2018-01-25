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
        int res = Runtime.getRuntime().execute(sb.toString());
        if (res != 0)
        {

        }
        return res;
    }

    private int executeSingleJob(Job job)
    {
        if (j instanceof Command)
              return executeCommand((Command)j);
        else if (j instanceof JobList)
        {
            JobList jlist = (JobList)j;
            int res = 0;
            for (Job job : jlist.getJobs())
            {
                res |= executeSingleJob(job);
            }
            return res;
        }
        else
        {
            assert job instanceof PipedJob;
            assert false:"Currently PipedJob not supported!";
            return 0;
        }
    }

    public int executeJob()
    {
        res = 0;
        for (Job j : jobs)
        {
            res |= executeSingleJob(j);
        }
        return res;
    }

    public void addTempFile(String file)
    {
        assert file != null && !file.isEmpty():"No valid filename";
        tempFiles.add(file);
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
}
