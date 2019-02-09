/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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

package driver;

import tools.Util;
import driver.Job.Command;
import driver.Job.PipedJob;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class Compilation {
  private Driver driver;
  private ToolChain tc;
  private ArgList args;
  private ArrayList<String> tempFiles;
  private ArrayList<String> resultFiles;
  private Command failureCmd;
  private Job.JobList jobs;
  private ArrayList<Action> actions;

  public Compilation(Driver driver, ToolChain tc, ArgList args) {
    this.driver = driver;
    this.tc = tc;
    this.args = args;
    tempFiles = new ArrayList<>();
    resultFiles = new ArrayList<>();
    jobs = new Job.JobList();
    actions = new ArrayList<>();
  }

  /**
   * Eexecute a specified executable with some program arguments.
   */
  private int executeCommand(Command cmd) {
    String progPath = cmd.getExecutable();
    ArrayList<String> args = cmd.getArgs();
    StringBuilder sb = new StringBuilder(progPath);
    for (String arg : args) {
      sb.append(" ");
      sb.append(arg);
    }
    try {
      Process p = Runtime.getRuntime().exec(sb.toString());
      return p.waitFor();
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
      return -1;
    }
  }

  private int executeSingleJob(Job j) {
    if (j instanceof Command) {
      int t = executeCommand((Command) j);
      if (t != 0)
        failureCmd = (Command) j;
      return t;
    } else if (j instanceof Job.JobList) {
      Job.JobList jlist = (Job.JobList) j;
      int res = 0;
      for (Job job : jlist.getJobs()) {
        res = executeSingleJob(job);
        if (res != 0)
          return res;
      }
      return 0;
    } else {
      Util.assertion(j instanceof PipedJob);
      Util.assertion(false, "Currently PipedJob not supported!");
      return 0;
    }
  }

  public int executeJob() {
    for (Job j : jobs.getJobs()) {
      int t = executeSingleJob(j);
      if (t != 0)
        return t;
    }
    return 0;
  }

  public String addTempFile(String file) {
    Util.assertion(file != null && !file.isEmpty(), "No valid filename");
    Util.assertion(!tempFiles.contains(file), "Can't add a exist temporary file!");
    tempFiles.add(file);
    return file;
  }

  public void addJob(Job job) {
    if (jobs.contains(job))
      return;
    jobs.add(job);
  }

  public Job.JobList getJobs() {
    return jobs;
  }

  public void addAction(Action act) {
    actions.add(act);
  }

  public ArrayList<Action> getActions() {
    return actions;
  }

  public Driver getDriver() {
    return driver;
  }

  public ArgList getArgs() {
    return args;
  }

  public ToolChain getToolChain() {
    return tc;
  }

  public Command getFailureCommand() {
    return failureCmd;
  }

  public String addResultFile(String filename) {
    Util.assertion(!((filename == null || filename.isEmpty() || resultFiles.contains(filename))));

    resultFiles.add(filename);
    return filename;
  }

  public void clearTemporaryFiles() {
    for (String file : tempFiles) {
      Path p = Paths.get(file);
      if (Files.exists(p)) {
        try {
          Files.delete(p);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Prints out all of commands to be performed for finishing a series of compilation.
   *
   * @param os
   */
  public void printJobs(PrintStream os, Job job, boolean quote) {
    if (job instanceof Command) {
      Command c = (Command) job;
      os.printf(" \"%s\"", c.getExecutable());
      for (String arg : c.getArgs()) {
        if (quote)
          os.printf(" \"%s\"", arg);
        else
          os.printf(" %s", arg);
      }
      os.println();
    } else if (job instanceof PipedJob) {
      Util.assertion(false, "PipedJob is not supported currently!");
    } else {
      Job.JobList jl = (Job.JobList) job;
      for (Job j : jl)
        printJobs(os, j, quote);
    }
  }
}
