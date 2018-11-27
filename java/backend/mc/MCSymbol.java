/*
 * Extremely C language Compiler
 *   Copyright (c) 2015-2018, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.mc;

import tools.Util;
import java.io.PrintStream;

/**
 * Instances of this class represent a symbol name in the MC file,
 * and MCSymbols are created and unique'd by the MCContext class.  MCSymbols
 * should only be constructed with valid names for the object file.
 *
 * If the symbol is defined/emitted into the current translation unit, the
 * Section member is set to indicate what section it lives in.  Otherwise, if
 * it is a reference to an external entity, it has a null section.
 */
public class MCSymbol {
  private static MCSection AbsolutePseduoSection = MCSection.getFakeSection();
  private String name;
  private MCSection section;
  private MCExpr value;
  private boolean isTemporary;

  private MCSymbol(String name,
                   boolean isTemp) {
    this.name = name;
    isTemporary = isTemp;
  }

  public String getName() {
    return name;
  }

  public boolean isTemporary() {
    return isTemporary;
  }

  public boolean isDefined() {
    return section != null;
  }

  public boolean isUndefined() {
    return !isDefined();
  }

  public boolean isAbsoluate() {
    return section == AbsolutePseduoSection;
  }
  public MCSection getSection() {
    Util.assertion(isDefined() && !isAbsoluate());
    return section;
  }

  public void setSection(MCSection section) {
    this.section = section;
  }

  public void setUndefined() {
    section = null;
  }

  public void setAbsolute() {
    section = AbsolutePseduoSection;
  }

  public boolean isVariable() {
    return value != null;
  }

  public MCExpr getValue() {
    return value;
  }

  public void setValue(MCExpr value) {
    this.value = value;
  }

  private static boolean isAccpatableChar(char ch) {
    return (ch >= 'a' && ch <= 'z') ||
        (ch >= 'A' && ch <= 'Z') ||
        ch == '_' || ch == '$' ||
        ch == '@';
  }

  private static boolean nameNeedsQuoting(String name) {
    Util.assertion(name != null && !name.isEmpty(), "Canot create an empty MCSymbol");
    for (int i = 0,e = name.length(); i < e; i++) {
      char ch = name.charAt(i);
      if (!isAccpatableChar(ch))
        return true;
    }
    return false;
  }

  public void print(PrintStream os) {
    if (!nameNeedsQuoting(getName())) {
      os.print(getName());
    }
    else {
      os.printf("\"%s\"", getName());
    }
  }
  public void dump() {
    print(System.err);
  }
}
