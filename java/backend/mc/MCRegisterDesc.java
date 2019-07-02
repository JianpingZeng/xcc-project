/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2015-2019, Jianping Zeng.
 * All rights reserved.
 *
 * Please refer the LICENSE for detail.
 */

package backend.mc;
/**
 * This record contains all of the information known about a
 * particular register.  The subRegs field (if not null) is an array of
 * registers that are sub-register of the specified register. The superRegs
 * field (if not null) is an array of registers that are super-register of
 * the specified register. This is needed for architectures like X86 which
 * have AL alias AX alias EAX. Registers that this does not apply to simply
 * should set this to null.
 */
public final class MCRegisterDesc {
  /**
   * Assembly language getIdentifier for the register.
   */
  public String asmName;

  public String name;

  public int[] aliasSet;

  /**
   * Register Alias Set, described above
   */
  public int[] subRegs;

  public int[] superRegs;

  public MCRegisterDesc(String asmName,
                        String name,
                        int[] as,
                        int[] SubRegs,
                        int[] SuperRegs) {
    this.asmName = asmName;
    this.name = name;
    aliasSet = as;
    subRegs = SubRegs;
    superRegs = SuperRegs;
  }
}
