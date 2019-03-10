/*
 * Extremely C language Compiler
 *   Copyright (c) 2015-2018, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.support;

public class LLVMDwarfConstants {

  public static final int LLVMDebugVersion = (7 << 16),         // Current version of debug information.
  LLVMDebugVersion6 = (6 << 16),        // Constant for version 6.
  LLVMDebugVersion5 = (5 << 16),        // Constant for version 5.
  LLVMDebugVersion4 = (4 << 16),        // Constant for version 4.
  LLVMDebugVersionMask = 0xffff0000,     // Mask for version number.

  // llvm mock tags
  DW_TAG_invalid = ~0,                 // Tag for invalid results.

  DW_TAG_anchor = 0,                    // Tag for descriptor anchors.
  DW_TAG_auto_variable = 0x100,         // Tag for local (auto) variables.
  DW_TAG_arg_variable = 0x101,          // Tag for argument variables.
  DW_TAG_return_variable = 0x102,       // Tag for return variables.

  DW_TAG_vector_type = 0x103,           // Tag for vector types.

  DW_TAG_user_base = 0x1000,            // Recommended base for user tags.

  DW_CIE_VERSION = 1,                   // Common frame information version.
  DW_CIE_ID       = 0xffffffff;          // Common frame information mark.
}
