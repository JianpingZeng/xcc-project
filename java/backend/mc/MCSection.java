/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */
package backend.mc;

import backend.target.SectionKind;
import java.io.PrintStream;

/**
 * Instances of this class represent a uniqued identifier for
 * a section in the current translation unit. The MCContext class
 * uniques and creates these.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public class MCSection {
  private SectionKind kind;
  private static MCSection FakeSection;
  public static MCSection getFakeSection() {
    if (FakeSection == null)
      FakeSection = new MCSection(null);
    return FakeSection;
  }

  protected MCSection(SectionKind k) { kind = k; }

  public SectionKind getKind() {
    return kind;
  }

  public void printSwitchToSection(MCAsmInfo mai,
                                   PrintStream os) {}
}
