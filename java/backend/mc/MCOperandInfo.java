/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2015-2019, Jianping Zeng.
 * All rights reserved.
 *
 * Please refer the LICENSE for detail.
 */

package backend.mc;

import backend.target.TargetRegisterInfo;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class MCOperandInfo {
  public interface OperandFlags {
    int LookupPtrRegClass = 0;
    int Predicate = 1;
    int OptionalDef = 2;
  }

  public interface OperandConstraint {
    int EARLY_CLOBBER = 0;
    int TIED_TO = 1;
  }

  public int regClass;
  public int flags;
  public int constraints;

  public MCOperandInfo(int rc, int flags, int constraints) {
    regClass = rc;
    this.flags = flags;
    this.constraints = constraints;
  }

  public MCRegisterClass getRegisterClass(TargetRegisterInfo tri) {
    if (isLookupPtrRegClass())
      return tri.getPointerRegClass(regClass);

    return tri.getRegClass(regClass);
  }

  public boolean isLookupPtrRegClass() {
    return (flags & (1 << OperandFlags.LookupPtrRegClass)) != 0;
  }

  public boolean isPredicate() {
    return (flags & OperandFlags.Predicate) != 0;
  }

  public boolean isOptionalDef() {
    return (flags & OperandFlags.OptionalDef) != 0;
  }
}
