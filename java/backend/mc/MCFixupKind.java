/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2015-2018, Jianping Zeng.
 * All rights reserved.
 *
 * Please refer the LICENSE for detail.
 */

package backend.mc;
/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public interface MCFixupKind {
  /**
   * A one-byte fixup.
   */
  int FK_Data_1 = 0;
  /**
   * A two-byte fixup.
   */
  int FK_Data_2 = 1;
  /**
   * A four-byte fixup.
   */
  int FK_Data_4 = 2;
  /**
   * A eight-byte fixup.
   */
  int FK_Data_8 = 3;
  /**
   * A one-byte pc relative fixup.
   */
  int FK_PCRel_1 = 4;
  /**
   * A two-byte pc relative fixup.
   */
  int FK_PCRel_2 = 5;
  /**
   * A four-byte pc relative fixup.
   */
  int FK_PCRel_4 = 6;
  /**
   * A eight-byte pc relative fixup.
   */
  int FK_PCRel_8 = 7;

  int FirstTargetFixupKind = 128;

  /**
   * Limit range of target fixups, in case we want to pack more efficiently
   * later.
   */
  int MaxTargetFixupKind = (1 << 8);
}
