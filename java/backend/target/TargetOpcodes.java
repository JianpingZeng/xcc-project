/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2015-2018, Jianping Zeng.
 * All rights reserved.
 *
 * Please refer the LICENSE for detail.
 */

package backend.target;
/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public interface TargetOpcodes {

  /**
   * Invariant opcodes: All instruction sets have these as their low opcodes.
   */
  int PHI = 0;
  int INLINEASM = 1;
  int DBG_LABEL = 2;
  int EH_LABEL = 3;
  int GC_LABEL = 4;
  int DECLARE = 5;
  /// EXTRACT_SUBREG - This instruction takes two operands: a register
  /// that has subregisters, and a subregister index. It returns the
  /// extracted subregister value. This is commonly used to implement
  /// truncation operations on target architectures which support it.
  int EXTRACT_SUBREG = 6;
  /// INSERT_SUBREG - This instruction takes three operands: a register
  /// that has subregisters, a register providing an insert value, and a
  /// subregister index. It returns the value of the first register with
  /// the value of the second register inserted. The first register is
  /// often defined by an IMPLICIT_DEF, as is commonly used to implement
  /// anyext operations on target architectures which support it.
  int INSERT_SUBREG = 7;
  /// IMPLICIT_DEF - This is the MachineInstr-level equivalent of undef.
  int IMPLICIT_DEF = 8;
  /// SUBREG_TO_REG - This instruction is similar to INSERT_SUBREG except
  /// that the first operand is an immediate integer constant. This constant
  /// is often zero, as is commonly used to implement zext operations on
  /// target architectures which support it, such as with x86-64 (with
  /// zext from i32 to i64 via implicit zero-extension).
  int SUBREG_TO_REG = 9;
  /// COPY_TO_REGCLASS - This instruction is a placeholder for a plain
  /// register-to-register copy into a specific register class. This is only
  /// used between instruction selection and MachineInstr creation, before
  /// public  registers have been created for all the instructions, and it's
  /// only needed in cases where the register classes implied by the
  /// instructions are insufficient. The actual MachineInstrs to perform
  /// the copy are emitted with the RISCVGenInstrInfo::copyRegToReg hook.
  int COPY_TO_REGCLASS = 10;
}
