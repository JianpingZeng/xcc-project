/*
 *  Extremely C language Compiler
 *    Copyright (c) 2015-2018, Jianping Zeng.
 *
 *  Licensed under the BSD License version 3. Please refer LICENSE for details.
 */

package backend.mc;

import java.io.PrintStream;

/**
 * Instances of this class represent a label name in the MC file,
 * and MCLabel are created and unique'd by the MCContext class.  MCLabel
 * should only be constructed for valid instances in the object file.
 */
public class MCLabel {
  private int instance;

  MCLabel(int instance) {
    this.instance = instance;
  }

  public int getInstance() {
    return instance;
  }

  public int incInstance() { return ++instance; }

  public void print(PrintStream os) {
    os.printf("\"%d\"", instance);
  }
  public void dump() { print(System.err);}
}
