/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2015-2018, Jianping Zeng.
 * All rights reserved.
 *
 * Please refer the LICENSE for detail.
 */

package backend.mc;

import tools.Util;

/**
 * Encode information on a single operation to perform on a byte sequence
 * (e.g. an encoded instruction) which requires assemble, or run-time patching.
 *
 * Fixups are used any time the target instruction encoder needs to represent
 * some value in an instruction which is not yet concrete. The encoder will
 * encode the instruction assuming the value is 0, and emit a fixup which
 * communicates to the assembler backend how it should rewrite the encoded
 * value.
 *
 * During the process of relaxation, the assembler will apply fixups as symbolic
 * values become concrete. When relaxation is complete, any remaining fixups
 * become relocations in the object file (or errors, if the fixup cann't be encoded
 * on the target).
 * @author Jianping Zeng.
 * @version 0.4
 */
public class MCFixup implements MCFixupKind {
  /**
   * The value to put into the fixup location. The exact interpretation of the
   * expression is target dependent, usually it will be one of the operands to
   * an instruction or an assembler directive.
   */
  private MCExpr value;
  /**
   * The byte index of start of the relocation inside the encoded instruction.
   */
  private int offset;
  /**
   * The target dependent kind of fixup item this is. The kind is used to
   * determine how the operand values should be encoded into the instruction.
   */
  private int kind;

  private MCFixup() {}
  public static MCFixup create(int offset, MCExpr value, int kind) {
    Util.assertion(kind < MaxTargetFixupKind, "Kind out of range!");
    MCFixup fi = new MCFixup();
    fi.value = value;
    fi.offset = offset;
    fi.kind = kind;
    return fi;
  }

  public int getKind() {
    return kind;
  }

  public int getOffset() {
    return offset;
  }

  public MCExpr getValue() {
    return value;
  }

  /**
   * Return the generic fixup kind for a value with the given size.
   * It is an error to pass an unsupported size.
   * @param size
   * @param isPCRel
   * @return
   */
  public static int getKindForSize(int size, boolean isPCRel) {
    switch (size) {
      default:
        Util.assertion("Invalid generic fixup size");
        return -1;
      case 1: return isPCRel ? FK_PCRel_1 : FK_Data_1;
      case 2: return isPCRel ? FK_PCRel_2 : FK_Data_2;
      case 4: return isPCRel ? FK_PCRel_4 : FK_Data_4;
      case 8: return isPCRel ? FK_PCRel_8 : FK_Data_8;
    }
  }
}
