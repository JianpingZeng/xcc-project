package backend.analysis;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng
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

import backend.type.Type;
import backend.value.BasicBlock;
import backend.value.Loop;
import tools.Util;

import java.io.PrintStream;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public final class SCEVCouldNotCompute extends SCEV {
  private final static SCEVCouldNotCompute instance = new SCEVCouldNotCompute();

  private SCEVCouldNotCompute() {
    super(SCEVType.scCouldNotCompute);
  }

  public static SCEVCouldNotCompute getInstance() {
    return instance;
  }


  @Override
  public boolean isLoopInvariant(Loop loop) {
    Util.assertion(false, "Attempt to use a SCEVCouldNotCompute object!");
    return false;
  }

  @Override
  public boolean hasComputableLoopEvolution(Loop loop) {
    Util.assertion(false, "Attempt to use a SCEVCouldNotCompute object!");
    return false;
  }

  @Override
  public SCEV replaceSymbolicValuesWithConcrete(SCEV sym, SCEV concrete) {
    return this;
  }

  @Override
  public Type getType() {
    Util.assertion(false, "Attempt to use a SCEVCouldNotCompute object!");
    return null;
  }

  @Override
  public boolean dominates(BasicBlock bb, DomTree dt) {
    // TODO: 17-7-1
    return false;
  }

  @Override
  public void print(PrintStream os) {
    os.print("****CouldNotCompute****");
  }
}
