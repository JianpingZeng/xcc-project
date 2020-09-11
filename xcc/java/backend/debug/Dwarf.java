/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 * This software is subjected to the protection of BSD 3.0 Licence.
 * For more details, please refers to the LICENSE file.
 */

package backend.debug;
/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class Dwarf {
  public static final int LLVMDebugVersion = (11 << 16);         // Current version of debug information.
  public static final int LLVMDebugVersion10 = (10 << 16);        // Constant for version 10.
  public static final int LLVMDebugVersion9 = (9 << 16);        // Constant for version 9.
  public static final int LLVMDebugVersion8 = (8 << 16);        // Constant for version 8.
  public static final int LLVMDebugVersion7 = (7 << 16);        // Constant for version 7.
  public static final int LLVMDebugVersion6 = (6 << 16);        // Constant for version 6.
  public static final int LLVMDebugVersion5 = (5 << 16);        // Constant for version 5.
  public static final int LLVMDebugVersion4 = (4 << 16);        // Constant for version 4.
  public static final int LLVMDebugVersionMask = 0xffff0000;     // Mask for version number.


  //===----------------------------------------------------------------------===//
  // Dwarf constants as gleaned from the DWARF Debugging Information Format V.3
  // reference manual http://dwarf.freestandards.org .
  //

  // Do not mix the following two enumerations sets.  DW_TAG_invalid changes the
  // enumeration base type.

  // llvm mock tags
  public static final int DW_TAG_invalid = ~0;                 // Tag for invalid results.
  public static final int DW_TAG_anchor = 0;                    // Tag for descriptor anchors.
  public static final int DW_TAG_auto_variable = 0x100;         // Tag for local (auto) variables.
  public static final int DW_TAG_arg_variable = 0x101;          // Tag for argument variables.
  public static final int DW_TAG_return_variable = 0x102;       // Tag for return variables.
  public static final int DW_TAG_vector_type = 0x103;           // Tag for vector types.
  public static final int DW_TAG_user_base = 0x1000;            // Recommended base for user tags.
  public static final int DW_CIE_VERSION = 1;                   // Common frame information version.
  public static final int DW_CIE_ID       = 0xffffffff;          // Common frame information mark.

  public static final int DWARF_VERSION = 2,

  // Tags
  DW_TAG_array_type = 0x01,
  DW_TAG_class_type = 0x02,
  DW_TAG_entry_point = 0x03,
  DW_TAG_enumeration_type = 0x04,
  DW_TAG_formal_parameter = 0x05,
  DW_TAG_imported_declaration = 0x08,
  DW_TAG_label = 0x0a,
  DW_TAG_lexical_block = 0x0b,
  DW_TAG_member = 0x0d,
  DW_TAG_pointer_type = 0x0f,
  DW_TAG_reference_type = 0x10,
  DW_TAG_compile_unit = 0x11,
  DW_TAG_string_type = 0x12,
  DW_TAG_structure_type = 0x13,
  DW_TAG_subroutine_type = 0x15,
  DW_TAG_typedef = 0x16,
  DW_TAG_union_type = 0x17,
  DW_TAG_unspecified_parameters = 0x18,
  DW_TAG_variant = 0x19,
  DW_TAG_common_block = 0x1a,
  DW_TAG_common_inclusion = 0x1b,
  DW_TAG_inheritance = 0x1c,
  DW_TAG_inlined_subroutine = 0x1d,
  DW_TAG_module = 0x1e,
  DW_TAG_ptr_to_member_type = 0x1f,
  DW_TAG_set_type = 0x20,
  DW_TAG_subrange_type = 0x21,
  DW_TAG_with_stmt = 0x22,
  DW_TAG_access_declaration = 0x23,
  DW_TAG_base_type = 0x24,
  DW_TAG_catch_block = 0x25,
  DW_TAG_const_type = 0x26,
  DW_TAG_constant = 0x27,
  DW_TAG_enumerator = 0x28,
  DW_TAG_file_type = 0x29,
  DW_TAG_friend = 0x2a,
  DW_TAG_namelist = 0x2b,
  DW_TAG_namelist_item = 0x2c,
  DW_TAG_packed_type = 0x2d,
  DW_TAG_subprogram = 0x2e,
  DW_TAG_template_type_parameter = 0x2f,
  DW_TAG_template_value_parameter = 0x30,
  DW_TAG_thrown_type = 0x31,
  DW_TAG_try_block = 0x32,
  DW_TAG_variant_part = 0x33,
  DW_TAG_variable = 0x34,
  DW_TAG_volatile_type = 0x35,
  DW_TAG_dwarf_procedure = 0x36,
  DW_TAG_restrict_type = 0x37,
  DW_TAG_interface_type = 0x38,
  DW_TAG_namespace = 0x39,
  DW_TAG_imported_module = 0x3a,
  DW_TAG_unspecified_type = 0x3b,
  DW_TAG_partial_unit = 0x3c,
  DW_TAG_imported_unit = 0x3d,
  DW_TAG_condition = 0x3f,
  DW_TAG_shared_type = 0x40,
  DW_TAG_lo_user = 0x4080,
  DW_TAG_hi_user = 0xffff,

  // Children flag
  DW_CHILDREN_no = 0x00,
  DW_CHILDREN_yes = 0x01,

  // Attributes
  DW_AT_sibling = 0x01,
  DW_AT_location = 0x02,
  DW_AT_name = 0x03,
  DW_AT_ordering = 0x09,
  DW_AT_byte_size = 0x0b,
  DW_AT_bit_offset = 0x0c,
  DW_AT_bit_size = 0x0d,
  DW_AT_stmt_list = 0x10,
  DW_AT_low_pc = 0x11,
  DW_AT_high_pc = 0x12,
  DW_AT_language = 0x13,
  DW_AT_discr = 0x15,
  DW_AT_discr_value = 0x16,
  DW_AT_visibility = 0x17,
  DW_AT_import = 0x18,
  DW_AT_string_length = 0x19,
  DW_AT_common_reference = 0x1a,
  DW_AT_comp_dir = 0x1b,
  DW_AT_const_value = 0x1c,
  DW_AT_containing_type = 0x1d,
  DW_AT_default_value = 0x1e,
  DW_AT_inline = 0x20,
  DW_AT_is_optional = 0x21,
  DW_AT_lower_bound = 0x22,
  DW_AT_producer = 0x25,
  DW_AT_prototyped = 0x27,
  DW_AT_return_addr = 0x2a,
  DW_AT_start_scope = 0x2c,
  DW_AT_bit_stride = 0x2e,
  DW_AT_upper_bound = 0x2f,
  DW_AT_abstract_origin = 0x31,
  DW_AT_accessibility = 0x32,
  DW_AT_address_class = 0x33,
  DW_AT_artificial = 0x34,
  DW_AT_base_types = 0x35,
  DW_AT_calling_convention = 0x36,
  DW_AT_count = 0x37,
  DW_AT_data_member_location = 0x38,
  DW_AT_decl_column = 0x39,
  DW_AT_decl_file = 0x3a,
  DW_AT_decl_line = 0x3b,
  DW_AT_declaration = 0x3c,
  DW_AT_discr_list = 0x3d,
  DW_AT_encoding = 0x3e,
  DW_AT_external = 0x3f,
  DW_AT_frame_base = 0x40,
  DW_AT_friend = 0x41,
  DW_AT_identifier_case = 0x42,
  DW_AT_macro_info = 0x43,
  DW_AT_namelist_item = 0x44,
  DW_AT_priority = 0x45,
  DW_AT_segment = 0x46,
  DW_AT_specification = 0x47,
  DW_AT_static_link = 0x48,
  DW_AT_type = 0x49,
  DW_AT_use_location = 0x4a,
  DW_AT_variable_parameter = 0x4b,
  DW_AT_virtuality = 0x4c,
  DW_AT_vtable_elem_location = 0x4d,
  DW_AT_allocated = 0x4e,
  DW_AT_associated = 0x4f,
  DW_AT_data_location = 0x50,
  DW_AT_byte_stride = 0x51,
  DW_AT_entry_pc = 0x52,
  DW_AT_use_UTF8 = 0x53,
  DW_AT_extension = 0x54,
  DW_AT_ranges = 0x55,
  DW_AT_trampoline = 0x56,
  DW_AT_call_column = 0x57,
  DW_AT_call_file = 0x58,
  DW_AT_call_line = 0x59,
  DW_AT_description = 0x5a,
  DW_AT_binary_scale = 0x5b,
  DW_AT_decimal_scale = 0x5c,
  DW_AT_small = 0x5d,
  DW_AT_decimal_sign = 0x5e,
  DW_AT_digit_count = 0x5f,
  DW_AT_picture_string = 0x60,
  DW_AT_mutable = 0x61,
  DW_AT_threads_scaled = 0x62,
  DW_AT_explicit = 0x63,
  DW_AT_object_pointer = 0x64,
  DW_AT_endianity = 0x65,
  DW_AT_elemental = 0x66,
  DW_AT_pure = 0x67,
  DW_AT_recursive = 0x68,
  DW_AT_signature = 0x69,
  DW_AT_main_subprogram = 0x6a,
  DW_AT_data_bit_offset = 0x6b,
  DW_AT_const_expr = 0x6c,
  DW_AT_enum_class = 0x6d,
  DW_AT_linkage_name = 0x6e,
  DW_AT_MIPS_linkage_name = 0x2007,
  DW_AT_sf_names   = 0x2101,
  DW_AT_src_info = 0x2102,
  DW_AT_mac_info = 0x2103,
  DW_AT_src_coords = 0x2104,
  DW_AT_body_begin = 0x2105,
  DW_AT_body_end = 0x2106,
  DW_AT_GNU_vector = 0x2107,
  DW_AT_GNU_template_name = 0x2110,
  DW_AT_MIPS_assumed_size = 0x2011,
  DW_AT_lo_user = 0x2000,
  DW_AT_hi_user = 0x3fff,

  // Apple extensions.
  DW_AT_APPLE_optimized = 0x3fe1,
  DW_AT_APPLE_flags = 0x3fe2,
  DW_AT_APPLE_isa = 0x3fe3,
  DW_AT_APPLE_block = 0x3fe4,
  DW_AT_APPLE_major_runtime_vers = 0x3fe5,
  DW_AT_APPLE_runtime_class = 0x3fe6,
  DW_AT_APPLE_omit_frame_ptr = 0x3fe7,
  DW_AT_APPLE_property_name = 0x3fe8,
  DW_AT_APPLE_property_getter = 0x3fe9,
  DW_AT_APPLE_property_setter = 0x3fea,
  DW_AT_APPLE_property_attribute = 0x3feb,
  DW_AT_APPLE_objc_complete_type = 0x3fec,

  // Attribute form encodings
  DW_FORM_addr = 0x01,
  DW_FORM_block2 = 0x03,
  DW_FORM_block4 = 0x04,
  DW_FORM_data2 = 0x05,
  DW_FORM_data4 = 0x06,
  DW_FORM_data8 = 0x07,
  DW_FORM_string = 0x08,
  DW_FORM_block = 0x09,
  DW_FORM_block1 = 0x0a,
  DW_FORM_data1 = 0x0b,
  DW_FORM_flag = 0x0c,
  DW_FORM_sdata = 0x0d,
  DW_FORM_strp = 0x0e,
  DW_FORM_udata = 0x0f,
  DW_FORM_ref_addr = 0x10,
  DW_FORM_ref1 = 0x11,
  DW_FORM_ref2 = 0x12,
  DW_FORM_ref4 = 0x13,
  DW_FORM_ref8 = 0x14,
  DW_FORM_ref_udata = 0x15,
  DW_FORM_indirect = 0x16,
  DW_FORM_sec_offset = 0x17,
  DW_FORM_exprloc = 0x18,
  DW_FORM_flag_present = 0x19,
  DW_FORM_ref_sig8 = 0x20,

  // Operation encodings
  DW_OP_addr = 0x03,
  DW_OP_deref = 0x06,
  DW_OP_const1u = 0x08,
  DW_OP_const1s = 0x09,
  DW_OP_const2u = 0x0a,
  DW_OP_const2s = 0x0b,
  DW_OP_const4u = 0x0c,
  DW_OP_const4s = 0x0d,
  DW_OP_const8u = 0x0e,
  DW_OP_const8s = 0x0f,
  DW_OP_constu = 0x10,
  DW_OP_consts = 0x11,
  DW_OP_dup = 0x12,
  DW_OP_drop = 0x13,
  DW_OP_over = 0x14,
  DW_OP_pick = 0x15,
  DW_OP_swap = 0x16,
  DW_OP_rot = 0x17,
  DW_OP_xderef = 0x18,
  DW_OP_abs = 0x19,
  DW_OP_and = 0x1a,
  DW_OP_div = 0x1b,
  DW_OP_minus = 0x1c,
  DW_OP_mod = 0x1d,
  DW_OP_mul = 0x1e,
  DW_OP_neg = 0x1f,
  DW_OP_not = 0x20,
  DW_OP_or = 0x21,
  DW_OP_plus = 0x22,
  DW_OP_plus_uconst = 0x23,
  DW_OP_shl = 0x24,
  DW_OP_shr = 0x25,
  DW_OP_shra = 0x26,
  DW_OP_xor = 0x27,
  DW_OP_skip = 0x2f,
  DW_OP_bra = 0x28,
  DW_OP_eq = 0x29,
  DW_OP_ge = 0x2a,
  DW_OP_gt = 0x2b,
  DW_OP_le = 0x2c,
  DW_OP_lt = 0x2d,
  DW_OP_ne = 0x2e,
  DW_OP_lit0 = 0x30,
  DW_OP_lit1 = 0x31,
  DW_OP_lit2 = 0x32,
  DW_OP_lit3 = 0x33,
  DW_OP_lit4 = 0x34,
  DW_OP_lit5 = 0x35,
  DW_OP_lit6 = 0x36,
  DW_OP_lit7 = 0x37,
  DW_OP_lit8 = 0x38,
  DW_OP_lit9 = 0x39,
  DW_OP_lit10 = 0x3a,
  DW_OP_lit11 = 0x3b,
  DW_OP_lit12 = 0x3c,
  DW_OP_lit13 = 0x3d,
  DW_OP_lit14 = 0x3e,
  DW_OP_lit15 = 0x3f,
  DW_OP_lit16 = 0x40,
  DW_OP_lit17 = 0x41,
  DW_OP_lit18 = 0x42,
  DW_OP_lit19 = 0x43,
  DW_OP_lit20 = 0x44,
  DW_OP_lit21 = 0x45,
  DW_OP_lit22 = 0x46,
  DW_OP_lit23 = 0x47,
  DW_OP_lit24 = 0x48,
  DW_OP_lit25 = 0x49,
  DW_OP_lit26 = 0x4a,
  DW_OP_lit27 = 0x4b,
  DW_OP_lit28 = 0x4c,
  DW_OP_lit29 = 0x4d,
  DW_OP_lit30 = 0x4e,
  DW_OP_lit31 = 0x4f,
  DW_OP_reg0 = 0x50,
  DW_OP_reg1 = 0x51,
  DW_OP_reg2 = 0x52,
  DW_OP_reg3 = 0x53,
  DW_OP_reg4 = 0x54,
  DW_OP_reg5 = 0x55,
  DW_OP_reg6 = 0x56,
  DW_OP_reg7 = 0x57,
  DW_OP_reg8 = 0x58,
  DW_OP_reg9 = 0x59,
  DW_OP_reg10 = 0x5a,
  DW_OP_reg11 = 0x5b,
  DW_OP_reg12 = 0x5c,
  DW_OP_reg13 = 0x5d,
  DW_OP_reg14 = 0x5e,
  DW_OP_reg15 = 0x5f,
  DW_OP_reg16 = 0x60,
  DW_OP_reg17 = 0x61,
  DW_OP_reg18 = 0x62,
  DW_OP_reg19 = 0x63,
  DW_OP_reg20 = 0x64,
  DW_OP_reg21 = 0x65,
  DW_OP_reg22 = 0x66,
  DW_OP_reg23 = 0x67,
  DW_OP_reg24 = 0x68,
  DW_OP_reg25 = 0x69,
  DW_OP_reg26 = 0x6a,
  DW_OP_reg27 = 0x6b,
  DW_OP_reg28 = 0x6c,
  DW_OP_reg29 = 0x6d,
  DW_OP_reg30 = 0x6e,
  DW_OP_reg31 = 0x6f,
  DW_OP_breg0 = 0x70,
  DW_OP_breg1 = 0x71,
  DW_OP_breg2 = 0x72,
  DW_OP_breg3 = 0x73,
  DW_OP_breg4 = 0x74,
  DW_OP_breg5 = 0x75,
  DW_OP_breg6 = 0x76,
  DW_OP_breg7 = 0x77,
  DW_OP_breg8 = 0x78,
  DW_OP_breg9 = 0x79,
  DW_OP_breg10 = 0x7a,
  DW_OP_breg11 = 0x7b,
  DW_OP_breg12 = 0x7c,
  DW_OP_breg13 = 0x7d,
  DW_OP_breg14 = 0x7e,
  DW_OP_breg15 = 0x7f,
  DW_OP_breg16 = 0x80,
  DW_OP_breg17 = 0x81,
  DW_OP_breg18 = 0x82,
  DW_OP_breg19 = 0x83,
  DW_OP_breg20 = 0x84,
  DW_OP_breg21 = 0x85,
  DW_OP_breg22 = 0x86,
  DW_OP_breg23 = 0x87,
  DW_OP_breg24 = 0x88,
  DW_OP_breg25 = 0x89,
  DW_OP_breg26 = 0x8a,
  DW_OP_breg27 = 0x8b,
  DW_OP_breg28 = 0x8c,
  DW_OP_breg29 = 0x8d,
  DW_OP_breg30 = 0x8e,
  DW_OP_breg31 = 0x8f,
  DW_OP_regx = 0x90,
  DW_OP_fbreg = 0x91,
  DW_OP_bregx = 0x92,
  DW_OP_piece = 0x93,
  DW_OP_deref_size = 0x94,
  DW_OP_xderef_size = 0x95,
  DW_OP_nop = 0x96,
  DW_OP_push_object_address = 0x97,
  DW_OP_call2 = 0x98,
  DW_OP_call4 = 0x99,
  DW_OP_call_ref = 0x9a,
  DW_OP_form_tls_address = 0x9b,
  DW_OP_call_frame_cfa = 0x9c,
  DW_OP_bit_piece = 0x9d,
  DW_OP_implicit_value = 0x9e,
  DW_OP_stack_value = 0x9f,
  DW_OP_lo_user = 0xe0,
  DW_OP_hi_user = 0xff,

  // Encoding attribute values
  DW_ATE_address = 0x01,
  DW_ATE_boolean = 0x02,
  DW_ATE_complex_float = 0x03,
  DW_ATE_float = 0x04,
  DW_ATE_signed = 0x05,
  DW_ATE_signed_char = 0x06,
  DW_ATE_unsigned = 0x07,
  DW_ATE_unsigned_char = 0x08,
  DW_ATE_imaginary_float = 0x09,
  DW_ATE_packed_decimal = 0x0a,
  DW_ATE_numeric_string = 0x0b,
  DW_ATE_edited = 0x0c,
  DW_ATE_signed_fixed = 0x0d,
  DW_ATE_unsigned_fixed = 0x0e,
  DW_ATE_decimal_float = 0x0f,
  DW_ATE_UTF = 0x10,
  DW_ATE_lo_user = 0x80,
  DW_ATE_hi_user = 0xff,

  // Decimal sign attribute values
  DW_DS_unsigned = 0x01,
  DW_DS_leading_overpunch = 0x02,
  DW_DS_trailing_overpunch = 0x03,
  DW_DS_leading_separate = 0x04,
  DW_DS_trailing_separate = 0x05,

  // Endianity attribute values
  DW_END_default = 0x00,
  DW_END_big = 0x01,
  DW_END_little = 0x02,
  DW_END_lo_user = 0x40,
  DW_END_hi_user = 0xff,

  // Accessibility codes
  DW_ACCESS_public = 0x01,
  DW_ACCESS_protected = 0x02,
  DW_ACCESS_private = 0x03,

  // Visibility codes
  DW_VIS_local = 0x01,
  DW_VIS_exported = 0x02,
  DW_VIS_qualified = 0x03,

  // Virtuality codes
  DW_VIRTUALITY_none = 0x00,
  DW_VIRTUALITY_virtual = 0x01,
  DW_VIRTUALITY_pure_virtual = 0x02,

  // Language names
  DW_LANG_C89 = 0x0001,
  DW_LANG_C = 0x0002,
  DW_LANG_Ada83 = 0x0003,
  DW_LANG_C_plus_plus = 0x0004,
  DW_LANG_Cobol74 = 0x0005,
  DW_LANG_Cobol85 = 0x0006,
  DW_LANG_Fortran77 = 0x0007,
  DW_LANG_Fortran90 = 0x0008,
  DW_LANG_Pascal83 = 0x0009,
  DW_LANG_Modula2 = 0x000a,
  DW_LANG_Java = 0x000b,
  DW_LANG_C99 = 0x000c,
  DW_LANG_Ada95 = 0x000d,
  DW_LANG_Fortran95 = 0x000e,
  DW_LANG_PLI = 0x000f,
  DW_LANG_ObjC = 0x0010,
  DW_LANG_ObjC_plus_plus = 0x0011,
  DW_LANG_UPC = 0x0012,
  DW_LANG_D = 0x0013,
  DW_LANG_lo_user = 0x8000,
  DW_LANG_hi_user = 0xffff,

  // Identifier case codes
  DW_ID_case_sensitive = 0x00,
  DW_ID_up_case = 0x01,
  DW_ID_down_case = 0x02,
  DW_ID_case_insensitive = 0x03,

  // Calling convention codes
  DW_CC_normal = 0x01,
  DW_CC_program = 0x02,
  DW_CC_nocall = 0x03,
  DW_CC_lo_user = 0x40,
  DW_CC_hi_user = 0xff,

  // Inline codes
  DW_INL_not_inlined = 0x00,
  DW_INL_inlined = 0x01,
  DW_INL_declared_not_inlined = 0x02,
  DW_INL_declared_inlined = 0x03,

  // Array ordering
  DW_ORD_row_major = 0x00,
  DW_ORD_col_major = 0x01,

  // Discriminant descriptor values
  DW_DSC_label = 0x00,
  DW_DSC_range = 0x01,

  // Line Number Standard Opcode Encodings
  DW_LNS_copy = 0x01,
  DW_LNS_advance_pc = 0x02,
  DW_LNS_advance_line = 0x03,
  DW_LNS_set_file = 0x04,
  DW_LNS_set_column = 0x05,
  DW_LNS_negate_stmt = 0x06,
  DW_LNS_set_basic_block = 0x07,
  DW_LNS_const_add_pc = 0x08,
  DW_LNS_fixed_advance_pc = 0x09,
  DW_LNS_set_prologue_end = 0x0a,
  DW_LNS_set_epilogue_begin = 0x0b,
  DW_LNS_set_isa = 0x0c,

  // Line Number Extended Opcode Encodings
  DW_LNE_end_sequence = 0x01,
  DW_LNE_set_address = 0x02,
  DW_LNE_define_file = 0x03,
  DW_LNE_set_discriminator = 0x04,
  DW_LNE_lo_user = 0x80,
  DW_LNE_hi_user = 0xff,

  // Macinfo Type Encodings
  DW_MACINFO_define = 0x01,
  DW_MACINFO_undef = 0x02,
  DW_MACINFO_start_file = 0x03,
  DW_MACINFO_end_file = 0x04,
  DW_MACINFO_vendor_ext = 0xff,

  // Call frame instruction encodings
  DW_CFA_extended = 0x00,
  DW_CFA_nop = 0x00,
  DW_CFA_advance_loc = 0x40,
  DW_CFA_offset = 0x80,
  DW_CFA_restore = 0xc0,
  DW_CFA_set_loc = 0x01,
  DW_CFA_advance_loc1 = 0x02,
  DW_CFA_advance_loc2 = 0x03,
  DW_CFA_advance_loc4 = 0x04,
  DW_CFA_offset_extended = 0x05,
  DW_CFA_restore_extended = 0x06,
  DW_CFA_undefined = 0x07,
  DW_CFA_same_value = 0x08,
  DW_CFA_register = 0x09,
  DW_CFA_remember_state = 0x0a,
  DW_CFA_restore_state = 0x0b,
  DW_CFA_def_cfa = 0x0c,
  DW_CFA_def_cfa_register = 0x0d,
  DW_CFA_def_cfa_offset = 0x0e,
  DW_CFA_def_cfa_expression = 0x0f,
  DW_CFA_expression = 0x10,
  DW_CFA_offset_extended_sf = 0x11,
  DW_CFA_def_cfa_sf = 0x12,
  DW_CFA_def_cfa_offset_sf = 0x13,
  DW_CFA_val_offset = 0x14,
  DW_CFA_val_offset_sf = 0x15,
  DW_CFA_val_expression = 0x16,
  DW_CFA_lo_user = 0x1c,
  DW_CFA_hi_user = 0x3f,

  DW_EH_PE_absptr = 0x00,
  DW_EH_PE_omit = 0xff,
  DW_EH_PE_uleb128 = 0x01,
  DW_EH_PE_udata2 = 0x02,
  DW_EH_PE_udata4 = 0x03,
  DW_EH_PE_udata8 = 0x04,
  DW_EH_PE_sleb128 = 0x09,
  DW_EH_PE_sdata2 = 0x0A,
  DW_EH_PE_sdata4 = 0x0B,
  DW_EH_PE_sdata8 = 0x0C,
  DW_EH_PE_signed = 0x08,
  DW_EH_PE_pcrel = 0x10,
  DW_EH_PE_textrel = 0x20,
  DW_EH_PE_datarel = 0x30,
  DW_EH_PE_funcrel = 0x40,
  DW_EH_PE_aligned = 0x50,
  DW_EH_PE_indirect = 0x80,

  DW_APPLE_PROPERTY_readonly = 0x01,
  DW_APPLE_PROPERTY_readwrite = 0x02,
  DW_APPLE_PROPERTY_assign = 0x04,
  DW_APPLE_PROPERTY_retain = 0x08,
  DW_APPLE_PROPERTY_copy = 0x10,
  DW_APPLE_PROPERTY_nonatomic = 0x20;

  public static String tagString(int tag) {
    switch (tag) {
      case DW_TAG_array_type:                return "DW_TAG_array_type";
      case DW_TAG_class_type:                return "DW_TAG_class_type";
      case DW_TAG_entry_point:               return "DW_TAG_entry_point";
      case DW_TAG_enumeration_type:          return "DW_TAG_enumeration_type";
      case DW_TAG_formal_parameter:          return "DW_TAG_formal_parameter";
      case DW_TAG_imported_declaration:      return "DW_TAG_imported_declaration";
      case DW_TAG_label:                     return "DW_TAG_label";
      case DW_TAG_lexical_block:             return "DW_TAG_lexical_block";
      case DW_TAG_member:                    return "DW_TAG_member";
      case DW_TAG_pointer_type:              return "DW_TAG_pointer_type";
      case DW_TAG_reference_type:            return "DW_TAG_reference_type";
      case DW_TAG_compile_unit:              return "DW_TAG_compile_unit";
      case DW_TAG_string_type:               return "DW_TAG_string_type";
      case DW_TAG_structure_type:            return "DW_TAG_structure_type";
      case DW_TAG_subroutine_type:           return "DW_TAG_subroutine_type";
      case DW_TAG_typedef:                   return "DW_TAG_typedef";
      case DW_TAG_union_type:                return "DW_TAG_union_type";
      case DW_TAG_unspecified_parameters:    return "DW_TAG_unspecified_parameters";
      case DW_TAG_variant:                   return "DW_TAG_variant";
      case DW_TAG_common_block:              return "DW_TAG_common_block";
      case DW_TAG_common_inclusion:          return "DW_TAG_common_inclusion";
      case DW_TAG_inheritance:               return "DW_TAG_inheritance";
      case DW_TAG_inlined_subroutine:        return "DW_TAG_inlined_subroutine";
      case DW_TAG_module:                    return "DW_TAG_module";
      case DW_TAG_ptr_to_member_type:        return "DW_TAG_ptr_to_member_type";
      case DW_TAG_set_type:                  return "DW_TAG_set_type";
      case DW_TAG_subrange_type:             return "DW_TAG_subrange_type";
      case DW_TAG_with_stmt:                 return "DW_TAG_with_stmt";
      case DW_TAG_access_declaration:        return "DW_TAG_access_declaration";
      case DW_TAG_base_type:                 return "DW_TAG_base_type";
      case DW_TAG_catch_block:               return "DW_TAG_catch_block";
      case DW_TAG_const_type:                return "DW_TAG_const_type";
      case DW_TAG_constant:                  return "DW_TAG_constant";
      case DW_TAG_enumerator:                return "DW_TAG_enumerator";
      case DW_TAG_file_type:                 return "DW_TAG_file_type";
      case DW_TAG_friend:                    return "DW_TAG_friend";
      case DW_TAG_namelist:                  return "DW_TAG_namelist";
      case DW_TAG_namelist_item:             return "DW_TAG_namelist_item";
      case DW_TAG_packed_type:               return "DW_TAG_packed_type";
      case DW_TAG_subprogram:                return "DW_TAG_subprogram";
      case DW_TAG_template_type_parameter:  return "DW_TAG_template_type_parameter";
      case DW_TAG_template_value_parameter:return "DW_TAG_template_value_parameter";
      case DW_TAG_thrown_type:               return "DW_TAG_thrown_type";
      case DW_TAG_try_block:                 return "DW_TAG_try_block";
      case DW_TAG_variant_part:              return "DW_TAG_variant_part";
      case DW_TAG_variable:                  return "DW_TAG_variable";
      case DW_TAG_volatile_type:             return "DW_TAG_volatile_type";
      case DW_TAG_dwarf_procedure:           return "DW_TAG_dwarf_procedure";
      case DW_TAG_restrict_type:             return "DW_TAG_restrict_type";
      case DW_TAG_interface_type:            return "DW_TAG_interface_type";
      case DW_TAG_namespace:                 return "DW_TAG_namespace";
      case DW_TAG_imported_module:           return "DW_TAG_imported_module";
      case DW_TAG_unspecified_type:          return "DW_TAG_unspecified_type";
      case DW_TAG_partial_unit:              return "DW_TAG_partial_unit";
      case DW_TAG_imported_unit:             return "DW_TAG_imported_unit";
      case DW_TAG_condition:                 return "DW_TAG_condition";
      case DW_TAG_shared_type:               return "DW_TAG_shared_type";
      case DW_TAG_lo_user:                   return "DW_TAG_lo_user";
      case DW_TAG_hi_user:                   return "DW_TAG_hi_user";
    }
    return null;
  }

  public static String childrenString(int children) {
    switch (children) {
      case DW_CHILDREN_no: return "DW_CHILDREN_no";
      case DW_CHILDREN_yes: return "DW_CHILDREN_yes";
    }
    return null;
  }
  public static String attributeString(int attribute) {
    switch (attribute) {
      case DW_AT_sibling:                    return "DW_AT_sibling";
      case DW_AT_location:                   return "DW_AT_location";
      case DW_AT_name:                       return "DW_AT_name";
      case DW_AT_ordering:                   return "DW_AT_ordering";
      case DW_AT_byte_size:                  return "DW_AT_byte_size";
      case DW_AT_bit_offset:                 return "DW_AT_bit_offset";
      case DW_AT_bit_size:                   return "DW_AT_bit_size";
      case DW_AT_stmt_list:                  return "DW_AT_stmt_list";
      case DW_AT_low_pc:                     return "DW_AT_low_pc";
      case DW_AT_high_pc:                    return "DW_AT_high_pc";
      case DW_AT_language:                   return "DW_AT_language";
      case DW_AT_discr:                      return "DW_AT_discr";
      case DW_AT_discr_value:                return "DW_AT_discr_value";
      case DW_AT_visibility:                 return "DW_AT_visibility";
      case DW_AT_import:                     return "DW_AT_import";
      case DW_AT_string_length:              return "DW_AT_string_length";
      case DW_AT_common_reference:           return "DW_AT_common_reference";
      case DW_AT_comp_dir:                   return "DW_AT_comp_dir";
      case DW_AT_const_value:                return "DW_AT_const_value";
      case DW_AT_containing_type:            return "DW_AT_containing_type";
      case DW_AT_default_value:              return "DW_AT_default_value";
      case DW_AT_inline:                     return "DW_AT_inline";
      case DW_AT_is_optional:                return "DW_AT_is_optional";
      case DW_AT_lower_bound:                return "DW_AT_lower_bound";
      case DW_AT_producer:                   return "DW_AT_producer";
      case DW_AT_prototyped:                 return "DW_AT_prototyped";
      case DW_AT_return_addr:                return "DW_AT_return_addr";
      case DW_AT_start_scope:                return "DW_AT_start_scope";
      case DW_AT_bit_stride:                 return "DW_AT_bit_stride";
      case DW_AT_upper_bound:                return "DW_AT_upper_bound";
      case DW_AT_abstract_origin:            return "DW_AT_abstract_origin";
      case DW_AT_accessibility:              return "DW_AT_accessibility";
      case DW_AT_address_class:              return "DW_AT_address_class";
      case DW_AT_artificial:                 return "DW_AT_artificial";
      case DW_AT_base_types:                 return "DW_AT_base_types";
      case DW_AT_calling_convention:         return "DW_AT_calling_convention";
      case DW_AT_count:                      return "DW_AT_count";
      case DW_AT_data_member_location:       return "DW_AT_data_member_location";
      case DW_AT_decl_column:                return "DW_AT_decl_column";
      case DW_AT_decl_file:                  return "DW_AT_decl_file";
      case DW_AT_decl_line:                  return "DW_AT_decl_line";
      case DW_AT_declaration:                return "DW_AT_declaration";
      case DW_AT_discr_list:                 return "DW_AT_discr_list";
      case DW_AT_encoding:                   return "DW_AT_encoding";
      case DW_AT_external:                   return "DW_AT_external";
      case DW_AT_frame_base:                 return "DW_AT_frame_base";
      case DW_AT_friend:                     return "DW_AT_friend";
      case DW_AT_identifier_case:            return "DW_AT_identifier_case";
      case DW_AT_macro_info:                 return "DW_AT_macro_info";
      case DW_AT_namelist_item:              return "DW_AT_namelist_item";
      case DW_AT_priority:                   return "DW_AT_priority";
      case DW_AT_segment:                    return "DW_AT_segment";
      case DW_AT_specification:              return "DW_AT_specification";
      case DW_AT_static_link:                return "DW_AT_static_link";
      case DW_AT_type:                       return "DW_AT_type";
      case DW_AT_use_location:               return "DW_AT_use_location";
      case DW_AT_variable_parameter:         return "DW_AT_variable_parameter";
      case DW_AT_virtuality:                 return "DW_AT_virtuality";
      case DW_AT_vtable_elem_location:       return "DW_AT_vtable_elem_location";
      case DW_AT_allocated:                  return "DW_AT_allocated";
      case DW_AT_associated:                 return "DW_AT_associated";
      case DW_AT_data_location:              return "DW_AT_data_location";
      case DW_AT_byte_stride:                return "DW_AT_byte_stride";
      case DW_AT_entry_pc:                   return "DW_AT_entry_pc";
      case DW_AT_use_UTF8:                   return "DW_AT_use_UTF8";
      case DW_AT_extension:                  return "DW_AT_extension";
      case DW_AT_ranges:                     return "DW_AT_ranges";
      case DW_AT_trampoline:                 return "DW_AT_trampoline";
      case DW_AT_call_column:                return "DW_AT_call_column";
      case DW_AT_call_file:                  return "DW_AT_call_file";
      case DW_AT_call_line:                  return "DW_AT_call_line";
      case DW_AT_description:                return "DW_AT_description";
      case DW_AT_binary_scale:               return "DW_AT_binary_scale";
      case DW_AT_decimal_scale:              return "DW_AT_decimal_scale";
      case DW_AT_small:                      return "DW_AT_small";
      case DW_AT_decimal_sign:               return "DW_AT_decimal_sign";
      case DW_AT_digit_count:                return "DW_AT_digit_count";
      case DW_AT_picture_string:             return "DW_AT_picture_string";
      case DW_AT_mutable:                    return "DW_AT_mutable";
      case DW_AT_threads_scaled:             return "DW_AT_threads_scaled";
      case DW_AT_explicit:                   return "DW_AT_explicit";
      case DW_AT_object_pointer:             return "DW_AT_object_pointer";
      case DW_AT_endianity:                  return "DW_AT_endianity";
      case DW_AT_elemental:                  return "DW_AT_elemental";
      case DW_AT_pure:                       return "DW_AT_pure";
      case DW_AT_recursive:                  return "DW_AT_recursive";
      case DW_AT_signature:                  return "DW_AT_signature";
      case DW_AT_main_subprogram:            return "DW_AT_main_subprogram";
      case DW_AT_data_bit_offset:            return "DW_AT_data_bit_offset";
      case DW_AT_const_expr:                 return "DW_AT_const_expr";
      case DW_AT_enum_class:                 return "DW_AT_enum_class";
      case DW_AT_linkage_name:               return "DW_AT_linkage_name";
      case DW_AT_MIPS_linkage_name:          return "DW_AT_MIPS_linkage_name";
      case DW_AT_sf_names:                   return "DW_AT_sf_names";
      case DW_AT_src_info:                   return "DW_AT_src_info";
      case DW_AT_mac_info:                   return "DW_AT_mac_info";
      case DW_AT_src_coords:                 return "DW_AT_src_coords";
      case DW_AT_body_begin:                 return "DW_AT_body_begin";
      case DW_AT_body_end:                   return "DW_AT_body_end";
      case DW_AT_GNU_vector:                 return "DW_AT_GNU_vector";
      case DW_AT_GNU_template_name:          return "DW_AT_GNU_template_name";
      case DW_AT_MIPS_assumed_size:          return "DW_AT_MIPS_assumed_size";
      case DW_AT_lo_user:                    return "DW_AT_lo_user";
      case DW_AT_hi_user:                    return "DW_AT_hi_user";
      case DW_AT_APPLE_optimized:            return "DW_AT_APPLE_optimized";
      case DW_AT_APPLE_flags:                return "DW_AT_APPLE_flags";
      case DW_AT_APPLE_isa:                  return "DW_AT_APPLE_isa";
      case DW_AT_APPLE_block:                return "DW_AT_APPLE_block";
      case DW_AT_APPLE_major_runtime_vers:   return "DW_AT_APPLE_major_runtime_vers";
      case DW_AT_APPLE_runtime_class:        return "DW_AT_APPLE_runtime_class";
      case DW_AT_APPLE_omit_frame_ptr:       return "DW_AT_APPLE_omit_frame_ptr";
      case DW_AT_APPLE_property_name:        return "DW_AT_APPLE_property_name";
      case DW_AT_APPLE_property_getter:      return "DW_AT_APPLE_property_getter";
      case DW_AT_APPLE_property_setter:      return "DW_AT_APPLE_property_setter";
      case DW_AT_APPLE_property_attribute:   return "DW_AT_APPLE_property_attribute";
      case DW_AT_APPLE_objc_complete_type:   return "DW_AT_APPLE_objc_complete_type";
    }
    return null;
  }

  public static String formEncodingString(int encoding) {
    switch (encoding) {
      case DW_FORM_addr:                     return "DW_FORM_addr";
      case DW_FORM_block2:                   return "DW_FORM_block2";
      case DW_FORM_block4:                   return "DW_FORM_block4";
      case DW_FORM_data2:                    return "DW_FORM_data2";
      case DW_FORM_data4:                    return "DW_FORM_data4";
      case DW_FORM_data8:                    return "DW_FORM_data8";
      case DW_FORM_string:                   return "DW_FORM_string";
      case DW_FORM_block:                    return "DW_FORM_block";
      case DW_FORM_block1:                   return "DW_FORM_block1";
      case DW_FORM_data1:                    return "DW_FORM_data1";
      case DW_FORM_flag:                     return "DW_FORM_flag";
      case DW_FORM_sdata:                    return "DW_FORM_sdata";
      case DW_FORM_strp:                     return "DW_FORM_strp";
      case DW_FORM_udata:                    return "DW_FORM_udata";
      case DW_FORM_ref_addr:                 return "DW_FORM_ref_addr";
      case DW_FORM_ref1:                     return "DW_FORM_ref1";
      case DW_FORM_ref2:                     return "DW_FORM_ref2";
      case DW_FORM_ref4:                     return "DW_FORM_ref4";
      case DW_FORM_ref8:                     return "DW_FORM_ref8";
      case DW_FORM_ref_udata:                return "DW_FORM_ref_udata";
      case DW_FORM_indirect:                 return "DW_FORM_indirect";
      case DW_FORM_sec_offset:               return "DW_FORM_sec_offset";
      case DW_FORM_exprloc:                  return "DW_FORM_exprloc";
      case DW_FORM_flag_present:             return "DW_FORM_flag_present";
      case DW_FORM_ref_sig8:                 return "DW_FORM_ref_sig8";
    }
    return null;
  }

  /**
   * Return the string for the specified operation encoding.
   * @param encoding
   * @return
   */
  public static String operationEncodingString(int encoding) {
    switch (encoding) {
      case DW_OP_addr:                       return "DW_OP_addr";
      case DW_OP_deref:                      return "DW_OP_deref";
      case DW_OP_const1u:                    return "DW_OP_const1u";
      case DW_OP_const1s:                    return "DW_OP_const1s";
      case DW_OP_const2u:                    return "DW_OP_const2u";
      case DW_OP_const2s:                    return "DW_OP_const2s";
      case DW_OP_const4u:                    return "DW_OP_const4u";
      case DW_OP_const4s:                    return "DW_OP_const4s";
      case DW_OP_const8u:                    return "DW_OP_const8u";
      case DW_OP_const8s:                    return "DW_OP_const8s";
      case DW_OP_constu:                     return "DW_OP_constu";
      case DW_OP_consts:                     return "DW_OP_consts";
      case DW_OP_dup:                        return "DW_OP_dup";
      case DW_OP_drop:                       return "DW_OP_drop";
      case DW_OP_over:                       return "DW_OP_over";
      case DW_OP_pick:                       return "DW_OP_pick";
      case DW_OP_swap:                       return "DW_OP_swap";
      case DW_OP_rot:                        return "DW_OP_rot";
      case DW_OP_xderef:                     return "DW_OP_xderef";
      case DW_OP_abs:                        return "DW_OP_abs";
      case DW_OP_and:                        return "DW_OP_and";
      case DW_OP_div:                        return "DW_OP_div";
      case DW_OP_minus:                      return "DW_OP_minus";
      case DW_OP_mod:                        return "DW_OP_mod";
      case DW_OP_mul:                        return "DW_OP_mul";
      case DW_OP_neg:                        return "DW_OP_neg";
      case DW_OP_not:                        return "DW_OP_not";
      case DW_OP_or:                         return "DW_OP_or";
      case DW_OP_plus:                       return "DW_OP_plus";
      case DW_OP_plus_uconst:                return "DW_OP_plus_uconst";
      case DW_OP_shl:                        return "DW_OP_shl";
      case DW_OP_shr:                        return "DW_OP_shr";
      case DW_OP_shra:                       return "DW_OP_shra";
      case DW_OP_xor:                        return "DW_OP_xor";
      case DW_OP_skip:                       return "DW_OP_skip";
      case DW_OP_bra:                        return "DW_OP_bra";
      case DW_OP_eq:                         return "DW_OP_eq";
      case DW_OP_ge:                         return "DW_OP_ge";
      case DW_OP_gt:                         return "DW_OP_gt";
      case DW_OP_le:                         return "DW_OP_le";
      case DW_OP_lt:                         return "DW_OP_lt";
      case DW_OP_ne:                         return "DW_OP_ne";
      case DW_OP_lit0:                       return "DW_OP_lit0";
      case DW_OP_lit1:                       return "DW_OP_lit1";
      case DW_OP_lit2:                       return "DW_OP_lit2";
      case DW_OP_lit3:                       return "DW_OP_lit3";
      case DW_OP_lit4:                       return "DW_OP_lit4";
      case DW_OP_lit5:                       return "DW_OP_lit5";
      case DW_OP_lit6:                       return "DW_OP_lit6";
      case DW_OP_lit7:                       return "DW_OP_lit7";
      case DW_OP_lit8:                       return "DW_OP_lit8";
      case DW_OP_lit9:                       return "DW_OP_lit9";
      case DW_OP_lit10:                      return "DW_OP_lit10";
      case DW_OP_lit11:                      return "DW_OP_lit11";
      case DW_OP_lit12:                      return "DW_OP_lit12";
      case DW_OP_lit13:                      return "DW_OP_lit13";
      case DW_OP_lit14:                      return "DW_OP_lit14";
      case DW_OP_lit15:                      return "DW_OP_lit15";
      case DW_OP_lit16:                      return "DW_OP_lit16";
      case DW_OP_lit17:                      return "DW_OP_lit17";
      case DW_OP_lit18:                      return "DW_OP_lit18";
      case DW_OP_lit19:                      return "DW_OP_lit19";
      case DW_OP_lit20:                      return "DW_OP_lit20";
      case DW_OP_lit21:                      return "DW_OP_lit21";
      case DW_OP_lit22:                      return "DW_OP_lit22";
      case DW_OP_lit23:                      return "DW_OP_lit23";
      case DW_OP_lit24:                      return "DW_OP_lit24";
      case DW_OP_lit25:                      return "DW_OP_lit25";
      case DW_OP_lit26:                      return "DW_OP_lit26";
      case DW_OP_lit27:                      return "DW_OP_lit27";
      case DW_OP_lit28:                      return "DW_OP_lit28";
      case DW_OP_lit29:                      return "DW_OP_lit29";
      case DW_OP_lit30:                      return "DW_OP_lit30";
      case DW_OP_lit31:                      return "DW_OP_lit31";
      case DW_OP_reg0:                       return "DW_OP_reg0";
      case DW_OP_reg1:                       return "DW_OP_reg1";
      case DW_OP_reg2:                       return "DW_OP_reg2";
      case DW_OP_reg3:                       return "DW_OP_reg3";
      case DW_OP_reg4:                       return "DW_OP_reg4";
      case DW_OP_reg5:                       return "DW_OP_reg5";
      case DW_OP_reg6:                       return "DW_OP_reg6";
      case DW_OP_reg7:                       return "DW_OP_reg7";
      case DW_OP_reg8:                       return "DW_OP_reg8";
      case DW_OP_reg9:                       return "DW_OP_reg9";
      case DW_OP_reg10:                      return "DW_OP_reg10";
      case DW_OP_reg11:                      return "DW_OP_reg11";
      case DW_OP_reg12:                      return "DW_OP_reg12";
      case DW_OP_reg13:                      return "DW_OP_reg13";
      case DW_OP_reg14:                      return "DW_OP_reg14";
      case DW_OP_reg15:                      return "DW_OP_reg15";
      case DW_OP_reg16:                      return "DW_OP_reg16";
      case DW_OP_reg17:                      return "DW_OP_reg17";
      case DW_OP_reg18:                      return "DW_OP_reg18";
      case DW_OP_reg19:                      return "DW_OP_reg19";
      case DW_OP_reg20:                      return "DW_OP_reg20";
      case DW_OP_reg21:                      return "DW_OP_reg21";
      case DW_OP_reg22:                      return "DW_OP_reg22";
      case DW_OP_reg23:                      return "DW_OP_reg23";
      case DW_OP_reg24:                      return "DW_OP_reg24";
      case DW_OP_reg25:                      return "DW_OP_reg25";
      case DW_OP_reg26:                      return "DW_OP_reg26";
      case DW_OP_reg27:                      return "DW_OP_reg27";
      case DW_OP_reg28:                      return "DW_OP_reg28";
      case DW_OP_reg29:                      return "DW_OP_reg29";
      case DW_OP_reg30:                      return "DW_OP_reg30";
      case DW_OP_reg31:                      return "DW_OP_reg31";
      case DW_OP_breg0:                      return "DW_OP_breg0";
      case DW_OP_breg1:                      return "DW_OP_breg1";
      case DW_OP_breg2:                      return "DW_OP_breg2";
      case DW_OP_breg3:                      return "DW_OP_breg3";
      case DW_OP_breg4:                      return "DW_OP_breg4";
      case DW_OP_breg5:                      return "DW_OP_breg5";
      case DW_OP_breg6:                      return "DW_OP_breg6";
      case DW_OP_breg7:                      return "DW_OP_breg7";
      case DW_OP_breg8:                      return "DW_OP_breg8";
      case DW_OP_breg9:                      return "DW_OP_breg9";
      case DW_OP_breg10:                     return "DW_OP_breg10";
      case DW_OP_breg11:                     return "DW_OP_breg11";
      case DW_OP_breg12:                     return "DW_OP_breg12";
      case DW_OP_breg13:                     return "DW_OP_breg13";
      case DW_OP_breg14:                     return "DW_OP_breg14";
      case DW_OP_breg15:                     return "DW_OP_breg15";
      case DW_OP_breg16:                     return "DW_OP_breg16";
      case DW_OP_breg17:                     return "DW_OP_breg17";
      case DW_OP_breg18:                     return "DW_OP_breg18";
      case DW_OP_breg19:                     return "DW_OP_breg19";
      case DW_OP_breg20:                     return "DW_OP_breg20";
      case DW_OP_breg21:                     return "DW_OP_breg21";
      case DW_OP_breg22:                     return "DW_OP_breg22";
      case DW_OP_breg23:                     return "DW_OP_breg23";
      case DW_OP_breg24:                     return "DW_OP_breg24";
      case DW_OP_breg25:                     return "DW_OP_breg25";
      case DW_OP_breg26:                     return "DW_OP_breg26";
      case DW_OP_breg27:                     return "DW_OP_breg27";
      case DW_OP_breg28:                     return "DW_OP_breg28";
      case DW_OP_breg29:                     return "DW_OP_breg29";
      case DW_OP_breg30:                     return "DW_OP_breg30";
      case DW_OP_breg31:                     return "DW_OP_breg31";
      case DW_OP_regx:                       return "DW_OP_regx";
      case DW_OP_fbreg:                      return "DW_OP_fbreg";
      case DW_OP_bregx:                      return "DW_OP_bregx";
      case DW_OP_piece:                      return "DW_OP_piece";
      case DW_OP_deref_size:                 return "DW_OP_deref_size";
      case DW_OP_xderef_size:                return "DW_OP_xderef_size";
      case DW_OP_nop:                        return "DW_OP_nop";
      case DW_OP_push_object_address:        return "DW_OP_push_object_address";
      case DW_OP_call2:                      return "DW_OP_call2";
      case DW_OP_call4:                      return "DW_OP_call4";
      case DW_OP_call_ref:                   return "DW_OP_call_ref";
      case DW_OP_form_tls_address:           return "DW_OP_form_tls_address";
      case DW_OP_call_frame_cfa:             return "DW_OP_call_frame_cfa";
      case DW_OP_bit_piece:                  return "DW_OP_bit_piece";
      case DW_OP_implicit_value:             return "DW_OP_implicit_value";
      case DW_OP_stack_value:                return "DW_OP_stack_value";
      case DW_OP_lo_user:                    return "DW_OP_lo_user";
      case DW_OP_hi_user:                    return "DW_OP_hi_user";
    }
    return null;
  }

  /**
   * Return the string for the specified attribute encoding.
   * @param encoding
   * @return
   */
  public static String attributeEncodingString(int encoding) {
    switch (encoding) {
      case DW_ATE_address:                   return "DW_ATE_address";
      case DW_ATE_boolean:                   return "DW_ATE_boolean";
      case DW_ATE_complex_float:             return "DW_ATE_complex_float";
      case DW_ATE_float:                     return "DW_ATE_float";
      case DW_ATE_signed:                    return "DW_ATE_signed";
      case DW_ATE_signed_char:               return "DW_ATE_signed_char";
      case DW_ATE_unsigned:                  return "DW_ATE_unsigned";
      case DW_ATE_unsigned_char:             return "DW_ATE_unsigned_char";
      case DW_ATE_imaginary_float:           return "DW_ATE_imaginary_float";
      case DW_ATE_UTF:                       return "DW_ATE_UTF";
      case DW_ATE_packed_decimal:            return "DW_ATE_packed_decimal";
      case DW_ATE_numeric_string:            return "DW_ATE_numeric_string";
      case DW_ATE_edited:                    return "DW_ATE_edited";
      case DW_ATE_signed_fixed:              return "DW_ATE_signed_fixed";
      case DW_ATE_unsigned_fixed:            return "DW_ATE_unsigned_fixed";
      case DW_ATE_decimal_float:             return "DW_ATE_decimal_float";
      case DW_ATE_lo_user:                   return "DW_ATE_lo_user";
      case DW_ATE_hi_user:                   return "DW_ATE_hi_user";
    }
    return null;
  }

  /**
   * Return the string for the specified decimal sign attribute.
   * @param sign
   * @return
   */
  public static String decimalSignString(int sign) {
    switch (sign) {
      case DW_DS_unsigned:                   return "DW_DS_unsigned";
      case DW_DS_leading_overpunch:          return "DW_DS_leading_overpunch";
      case DW_DS_trailing_overpunch:         return "DW_DS_trailing_overpunch";
      case DW_DS_leading_separate:           return "DW_DS_leading_separate";
      case DW_DS_trailing_separate:          return "DW_DS_trailing_separate";
    }
    return null;
  }

  /**
   * Return the string for the specified endianity.
   * @param endian
   * @return
   */
  public static String endianityString(int endian) {
    switch (endian) {
      case DW_END_default:                   return "DW_END_default";
      case DW_END_big:                       return "DW_END_big";
      case DW_END_little:                    return "DW_END_little";
      case DW_END_lo_user:                   return "DW_END_lo_user";
      case DW_END_hi_user:                   return "DW_END_hi_user";
    }
    return null;
  }

  /**
   * Return the string for the specified accessibility.
   * @param access
   * @return
   */
  public static String accessibilityString(int access) {
    switch (access) {
      // Accessibility codes
      case DW_ACCESS_public:                 return "DW_ACCESS_public";
      case DW_ACCESS_protected:              return "DW_ACCESS_protected";
      case DW_ACCESS_private:                return "DW_ACCESS_private";
    }
    return null;
  }

  /**
   * Return the string for the specified visibility.
   * @param visibility
   * @return
   */
  public static String visibilityString(int visibility) {
    switch (visibility) {
      case DW_VIS_local:                     return "DW_VIS_local";
      case DW_VIS_exported:                  return "DW_VIS_exported";
      case DW_VIS_qualified:                 return "DW_VIS_qualified";
    }
    return null;
  }

  /**
   * Return the string for the specified virtuality.
   * @param virtuality
   * @return
   */
  public static String virtualityString(int virtuality) {
    switch (virtuality) {
      case DW_VIRTUALITY_none:               return "DW_VIRTUALITY_none";
      case DW_VIRTUALITY_virtual:            return "DW_VIRTUALITY_virtual";
      case DW_VIRTUALITY_pure_virtual:       return "DW_VIRTUALITY_pure_virtual";
    }
    return null;
  }

  /**
   * Return the string for the specified language.
   * @param language
   * @return
   */
  public static String languageString(int language) {
    switch (language) {
      case DW_LANG_C89:                      return "DW_LANG_C89";
      case DW_LANG_C:                        return "DW_LANG_C";
      case DW_LANG_Ada83:                    return "DW_LANG_Ada83";
      case DW_LANG_C_plus_plus:              return "DW_LANG_C_plus_plus";
      case DW_LANG_Cobol74:                  return "DW_LANG_Cobol74";
      case DW_LANG_Cobol85:                  return "DW_LANG_Cobol85";
      case DW_LANG_Fortran77:                return "DW_LANG_Fortran77";
      case DW_LANG_Fortran90:                return "DW_LANG_Fortran90";
      case DW_LANG_Pascal83:                 return "DW_LANG_Pascal83";
      case DW_LANG_Modula2:                  return "DW_LANG_Modula2";
      case DW_LANG_Java:                     return "DW_LANG_Java";
      case DW_LANG_C99:                      return "DW_LANG_C99";
      case DW_LANG_Ada95:                    return "DW_LANG_Ada95";
      case DW_LANG_Fortran95:                return "DW_LANG_Fortran95";
      case DW_LANG_PLI:                      return "DW_LANG_PLI";
      case DW_LANG_ObjC:                     return "DW_LANG_ObjC";
      case DW_LANG_ObjC_plus_plus:           return "DW_LANG_ObjC_plus_plus";
      case DW_LANG_UPC:                      return "DW_LANG_UPC";
      case DW_LANG_D:                        return "DW_LANG_D";
      case DW_LANG_lo_user:                  return "DW_LANG_lo_user";
      case DW_LANG_hi_user:                  return "DW_LANG_hi_user";
    }
    return null;
  }

  /**
   * Return the string for the specified identifier case.
   * @param _case
   * @return
   */
  public static String caseString(int _case) {
    switch (_case) {
      case DW_ID_case_sensitive:             return "DW_ID_case_sensitive";
      case DW_ID_up_case:                    return "DW_ID_up_case";
      case DW_ID_down_case:                  return "DW_ID_down_case";
      case DW_ID_case_insensitive:           return "DW_ID_case_insensitive";
    }
    return null;
  }

  /**
   * Return the string for the specified calling convention.
   * @param convention
   * @return
   */
  public static String conventionString(int convention) {
    switch (convention) {
      case DW_CC_normal:                     return "DW_CC_normal";
      case DW_CC_program:                    return "DW_CC_program";
      case DW_CC_nocall:                     return "DW_CC_nocall";
      case DW_CC_lo_user:                    return "DW_CC_lo_user";
      case DW_CC_hi_user:                    return "DW_CC_hi_user";
    }
    return null;
  }

  /**
   * Return the string for the specified inline code.
   * @param code
   * @return
   */
  public static String inlineCodeString(int code) {
    switch (code) {
      case DW_INL_not_inlined:               return "DW_INL_not_inlined";
      case DW_INL_inlined:                   return "DW_INL_inlined";
      case DW_INL_declared_not_inlined:      return "DW_INL_declared_not_inlined";
      case DW_INL_declared_inlined:          return "DW_INL_declared_inlined";
    }
    return null;
  }

  /**
   * Return the string for the specified array order
   * @param order
   * @return
   */
  public static String arrayOrderString(int order) {
    switch (order) {
      case DW_ORD_row_major:                 return "DW_ORD_row_major";
      case DW_ORD_col_major:                 return "DW_ORD_col_major";
    }
    return null;
  }

  /**
   * Return the string for the specified discriminant descriptor.
   * @param discriminant
   * @return
   */
  public static String discriminantString(int discriminant) {
    switch (discriminant) {
      case DW_DSC_label:                     return "DW_DSC_label";
      case DW_DSC_range:                     return "DW_DSC_range";
    }
    return null;
  }

  /**
   * Return the string for the specified line number standard.
   * @param standard
   * @return
   */
  public static String lnStandardString(int standard) {
    switch (standard) {
      case DW_LNS_copy:                      return "DW_LNS_copy";
      case DW_LNS_advance_pc:                return "DW_LNS_advance_pc";
      case DW_LNS_advance_line:              return "DW_LNS_advance_line";
      case DW_LNS_set_file:                  return "DW_LNS_set_file";
      case DW_LNS_set_column:                return "DW_LNS_set_column";
      case DW_LNS_negate_stmt:               return "DW_LNS_negate_stmt";
      case DW_LNS_set_basic_block:           return "DW_LNS_set_basic_block";
      case DW_LNS_const_add_pc:              return "DW_LNS_const_add_pc";
      case DW_LNS_fixed_advance_pc:          return "DW_LNS_fixed_advance_pc";
      case DW_LNS_set_prologue_end:          return "DW_LNS_set_prologue_end";
      case DW_LNS_set_epilogue_begin:        return "DW_LNS_set_epilogue_begin";
      case DW_LNS_set_isa:                   return "DW_LNS_set_isa";
    }
    return null;
  }

  /**
   * Return the string for the specified line number extended
   * opcode encodings.
   * @param encoding
   * @return
   */
  public static String lnExtendedString(int encoding) {
    switch (encoding) {
      // Line Number Extended Opcode Encodings
      case DW_LNE_end_sequence:              return "DW_LNE_end_sequence";
      case DW_LNE_set_address:               return "DW_LNE_set_address";
      case DW_LNE_define_file:               return "DW_LNE_define_file";
      case DW_LNE_set_discriminator:         return "DW_LNE_set_discriminator";
      case DW_LNE_lo_user:                   return "DW_LNE_lo_user";
      case DW_LNE_hi_user:                   return "DW_LNE_hi_user";
    }
    return null;
  }

  /**
   * Return the string for the specified macinfo type encodings.
   * @param encoding
   * @return
   */
  public static String macinfoString(int encoding) {
    switch (encoding) {
      // Macinfo Type Encodings
      case DW_MACINFO_define:                return "DW_MACINFO_define";
      case DW_MACINFO_undef:                 return "DW_MACINFO_undef";
      case DW_MACINFO_start_file:            return "DW_MACINFO_start_file";
      case DW_MACINFO_end_file:              return "DW_MACINFO_end_file";
      case DW_MACINFO_vendor_ext:            return "DW_MACINFO_vendor_ext";
    }
    return null;
  }

  /**
   * Return the string for the specified call frame instruction encodings.
   * @param encoding
   * @return
   */
  public static String callFrameString(int encoding) {
    switch (encoding) {
      case DW_CFA_advance_loc:               return "DW_CFA_advance_loc";
      case DW_CFA_offset:                    return "DW_CFA_offset";
      case DW_CFA_restore:                   return "DW_CFA_restore";
      case DW_CFA_set_loc:                   return "DW_CFA_set_loc";
      case DW_CFA_advance_loc1:              return "DW_CFA_advance_loc1";
      case DW_CFA_advance_loc2:              return "DW_CFA_advance_loc2";
      case DW_CFA_advance_loc4:              return "DW_CFA_advance_loc4";
      case DW_CFA_offset_extended:           return "DW_CFA_offset_extended";
      case DW_CFA_restore_extended:          return "DW_CFA_restore_extended";
      case DW_CFA_undefined:                 return "DW_CFA_undefined";
      case DW_CFA_same_value:                return "DW_CFA_same_value";
      case DW_CFA_register:                  return "DW_CFA_register";
      case DW_CFA_remember_state:            return "DW_CFA_remember_state";
      case DW_CFA_restore_state:             return "DW_CFA_restore_state";
      case DW_CFA_def_cfa:                   return "DW_CFA_def_cfa";
      case DW_CFA_def_cfa_register:          return "DW_CFA_def_cfa_register";
      case DW_CFA_def_cfa_offset:            return "DW_CFA_def_cfa_offset";
      case DW_CFA_def_cfa_expression:        return "DW_CFA_def_cfa_expression";
      case DW_CFA_expression:                return "DW_CFA_expression";
      case DW_CFA_offset_extended_sf:        return "DW_CFA_offset_extended_sf";
      case DW_CFA_def_cfa_sf:                return "DW_CFA_def_cfa_sf";
      case DW_CFA_def_cfa_offset_sf:         return "DW_CFA_def_cfa_offset_sf";
      case DW_CFA_val_offset:                return "DW_CFA_val_offset";
      case DW_CFA_val_offset_sf:             return "DW_CFA_val_offset_sf";
      case DW_CFA_val_expression:            return "DW_CFA_val_expression";
      case DW_CFA_lo_user:                   return "DW_CFA_lo_user";
      case DW_CFA_hi_user:                   return "DW_CFA_hi_user";
    }
    return null;
  }
}
