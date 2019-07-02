/*
 *  Extremely C language Compiler
 *    Copyright (c) 2015-2019, Jianping Zeng.
 *
 *  Licensed under the BSD License version 3. Please refer LICENSE for details.
 */

package backend.target.x86;

import backend.mc.MCAsmBackend;
import backend.target.Target;

public class X86AsmBackend extends MCAsmBackend {
  protected X86AsmBackend(Target t) {
    super(t);
  }
}
