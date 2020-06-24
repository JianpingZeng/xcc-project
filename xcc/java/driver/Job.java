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

package driver;

import java.util.ArrayList;
import java.util.Iterator;

import static driver.Job.JobClass.*;

public class Job {
  enum JobClass {
    CommandClass,
    PipedJobClass,
    JobListClass
  }

  private JobClass kind;

  public Job(JobClass kind) {
    this.kind = kind;
  }

  public static class Command extends Job {
    private Action source;
    private String executable;
    private ArrayList<String> args;

    public Command(Action source, String executable, ArrayList<String> args) {
      super(CommandClass);
      this.source = source;
      this.executable = executable;
      this.args = args;
    }

    public String getExecutable() {
      return executable;
    }

    public ArrayList<String> getArgs() {
      return args;
    }

    public Action getSource() {
      return source;
    }
  }

  public static class PipedJob extends Job {
    public PipedJob() {
      super(PipedJobClass);
    }
  }

  public static class JobList extends Job implements Iterable<Job> {
    private ArrayList<Job> jobs;

    public JobList() {
      super(JobListClass);
      jobs = new ArrayList<>();
    }

    public ArrayList<Job> getJobs() {
      return jobs;
    }

    public void addJob(Job job) {
      if (jobs.contains(job)) return;
      jobs.add(job);
    }

    public int size() {
      return jobs.size();
    }

    public boolean contains(Job job) {
      return jobs.contains(job);
    }

    public void add(Job job) {
      jobs.add(job);
    }

    @Override
    public Iterator<Job> iterator() {
      return jobs.iterator();
    }
  }
}
