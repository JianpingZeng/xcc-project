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

import backend.support.Triple;
import tools.Util;

import java.util.TreeMap;

import static driver.OptionID.OPT__m32;

public abstract class HostInfo {
  private Driver driver;
  private Triple triple;
  private TreeMap<String, ToolChain> toolChains;

  public HostInfo(Driver driver, Triple defaultTriple) {
    this.driver = driver;
    this.triple = defaultTriple;
    toolChains = new TreeMap<>();
  }

  public String getArchName() {
    return triple.getArchName();
  }

  public TreeMap<String, ToolChain> getToolChains() {
    return toolChains;
  }

  public Triple getTriple() {
    return triple;
  }

  public abstract ToolChain getToolChain(ArgList argList, String archName);

  public static HostInfo createLinuxHostInfo(Driver driver, Triple defaultTriple) {
    return new LinuxHostInfo(driver, defaultTriple);
  }

  public static HostInfo createUnknownHostInfo(Driver driver, Triple defaultTriple) {
    return new LinuxHostInfo(driver, defaultTriple);
  }

  public Driver getDriver() {
    return driver;
  }

  public static class LinuxHostInfo extends HostInfo {

    public LinuxHostInfo(Driver driver, Triple defaultTriple) {
      super(driver, defaultTriple);
    }

    @Override
    public ToolChain getToolChain(ArgList argList, String archName) {
      Util.assertion(archName != null);
      archName = getArchName();
      // Automatically handle some instances of -m32/-m64 we know about.
      Arg arg = argList.getLastArg(OPT__m32, OptionID.OPT__m64);
      if (arg != null) {
        archName = arg.getOption().getID() == OPT__m32 ? "i386" : "x86_64";
      }

      TreeMap<String, ToolChain> toolChains = getToolChains();
      if (toolChains.containsKey(archName))
        return toolChains.get(archName);
      else {
        Triple triple = new Triple(getTriple().getTriple());
        triple.setArchName(archName);
        ToolChain tc = new LinuxToolChain(this, triple);
        toolChains.put(archName, tc);
        return tc;
      }
    }
  }

  public static class UnknownHostInfo extends HostInfo {

    public UnknownHostInfo(Driver driver, Triple defaultTriple) {
      super(driver, defaultTriple);
    }

    @Override
    public ToolChain getToolChain(ArgList argList, String archName) {
      Util.assertion(archName != null);
      archName = getArchName();
      // Automatically handle some instances of -m32/-m64 we know about.
      Arg arg = argList.getLastArg(OPT__m32, OptionID.OPT__m64);
      if (arg != null) {
        archName = arg.getOption().getID() == OPT__m32 ? "i386" : "x86_64";
      }

      TreeMap<String, ToolChain> toolChains = getToolChains();
      if (toolChains.containsKey(archName))
        return toolChains.get(archName);
      else {
        Triple triple = new Triple(getTriple().getTriple());
        triple.setArchName(archName);
        ToolChain tc = new GenericGCCToolChain(this, triple);
        toolChains.put(archName, tc);
        return tc;
      }
    }
  }
}
