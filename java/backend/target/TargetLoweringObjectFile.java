/*
 * Extremely C language Compiler
 *   Copyright (c) 2015-2018, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.target;

import backend.mc.MCSection;
import backend.support.NameMangler;
import backend.value.GlobalValue;
import backend.value.GlobalVariable;
import tools.Util;

public class TargetLoweringObjectFile {

  /**
   * Given a mergable constant with the
   * specified size and relocation information, return a section that it
   * should be placed in.
   * @param kind
   * @return
   */
  public MCSection getSectionForConstant(SectionKind kind) {
    Util.assertion(kind != null);
    if (kind.isReadOnly() && ReadOnlySection != null)
      return ReadOnlySection;
    return DataSection;
  }

  public MCSection getStaticCtorSection() {
    return null;
  }

  public MCSection getStaticDtorSection() {
    return null;
  }

  public static SectionKind getKindForGlobal(GlobalValue gv, TargetMachine tm) {
    return null;
  }

  public MCSection sectionForGlobal(GlobalValue gv, SectionKind kind, NameMangler mangler, TargetMachine tm) {
    return null;
  }

  public MCSection sectionForGlobal(GlobalValue gv, NameMangler mangler, TargetMachine tm) {
    return null;
  }
}
