/*
 *  Extremely C language Compiler
 *    Copyright (c) 2015-2018, Jianping Zeng.
 *
 *  Licensed under the BSD License version 3. Please refer LICENSE for details.
 */

package backend.mc;

import backend.target.SectionKind;

import java.io.PrintStream;

public class MCSectionCOFF extends MCSection {
  private String name;
  private boolean isDirective;

  private MCSectionCOFF(String name, boolean isDirective, SectionKind k) {
    super(k);
    this.name = name;
    this.isDirective = isDirective;
  }

  public MCSectionCOFF create(String name, boolean isDirective, SectionKind k) {
    return new MCSectionCOFF(name, isDirective, k);
  }

  public String getName() {
    return name;
  }

  public boolean isDirective() {
    return isDirective;
  }

  @Override
  public void printSwitchToSection(MCAsmInfo mai, PrintStream os) {
    if (isDirective) {
      os.printf("%s\n", getName());
      return;
    }
    os.printf("\t.section\t%s,\"", getName());
    if (getKind().isText())
      os.print('x');
    if (getKind().isWriteable())
      os.print('w');
    os.println("\"");
  }
}
