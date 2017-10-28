package jlang.diag;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Xlous
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

/**
 * @author xlous.zeng
 * @version 0.1
 */
public interface DiagnosticParseTag
{

	public static final int err_anon_type_definition = Diagnostic.DiagnosticParseKindsBegin;
	public static final int err_argument_required_after_attribute = err_anon_type_definition + 1;
	public static final int err_declaration_does_not_declare_param = err_argument_required_after_attribute + 1;
	public static final int err_destructor_class_name = err_declaration_does_not_declare_param + 1;
	public static final int err_dup_virtual = err_destructor_class_name + 1;
	public static final int err_enumerator_unnamed_no_def = err_dup_virtual + 1;
	public static final int err_expected_asm_operand = err_enumerator_unnamed_no_def + 1;
	public static final int err_expected_case_before_expression = err_expected_asm_operand + 1;
	public static final int err_expected_catch = err_expected_case_before_expression + 1;
	public static final int err_expected_class_before = err_expected_catch + 1;
	public static final int err_expected_class_name = err_expected_class_before + 1;
	public static final int err_expected_colon_after = err_expected_class_name + 1;
	public static final int err_expected_comma = err_expected_colon_after + 1;
	public static final int err_expected_comma_greater = err_expected_comma + 1;
	public static final int err_expected_equal_after_declarator = err_expected_comma_greater + 1;
	public static final int err_expected_equal_designator = err_expected_equal_after_declarator + 1;
	public static final int err_expected_expression = err_expected_equal_designator + 1;
	public static final int err_expected_external_declaration = err_expected_expression + 1;
	public static final int err_expected_field_designator = err_expected_external_declaration + 1;
	public static final int err_expected_fn_body = err_expected_field_designator + 1;
	public static final int err_expected_greater = err_expected_fn_body + 1;
	public static final int err_expected_ident = err_expected_greater + 1;
	public static final int err_expected_ident_in_using = err_expected_ident + 1;
	public static final int err_expected_ident_lbrace = err_expected_ident_in_using + 1;
	public static final int err_expected_ident_lparen = err_expected_ident_lbrace + 1;
	public static final int err_expected_lbrace = err_expected_ident_lparen + 1;
	public static final int err_expected_lbrace_in_compound_literal = err_expected_lbrace + 1;
	public static final int err_expected_lbrace_or_comma = err_expected_lbrace_in_compound_literal + 1;
	public static final int err_expected_less_after = err_expected_lbrace_or_comma + 1;
	public static final int err_expected_lparen = err_expected_less_after + 1;
	public static final int err_expected_lparen_after = err_expected_lparen + 1;
	public static final int err_expected_lparen_after_id = err_expected_lparen_after + 1;
	public static final int err_expected_lparen_after_type = err_expected_lparen_after_id + 1;
	public static final int err_expected_member_name_or_semi = err_expected_lparen_after_type + 1;
	public static final int err_expected_member_or_base_name = err_expected_member_name_or_semi + 1;
	public static final int err_expected_method_body = err_expected_member_or_base_name + 1;
	public static final int err_expected_qualified_after_typename = err_expected_method_body + 1;
	public static final int err_expected_rbrace = err_expected_qualified_after_typename + 1;
	public static final int err_expected_rparen = err_expected_rbrace + 1;
	public static final int err_expected_rsquare = err_expected_rparen + 1;
	public static final int err_expected_selector_for_method = err_expected_rsquare + 1;
	public static final int err_expected_semi_after = err_expected_selector_for_method + 1;
	public static final int err_expected_semi_after_attribute_list = err_expected_semi_after + 1;
	public static final int err_expected_semi_after_expr = err_expected_semi_after_attribute_list + 1;
	public static final int err_expected_semi_after_method_proto = err_expected_semi_after_expr + 1;
	public static final int err_expected_semi_after_namespace_name = err_expected_semi_after_method_proto + 1;
	public static final int err_expected_semi_after_static_assert = err_expected_semi_after_namespace_name + 1;
	public static final int err_expected_semi_after_stmt = err_expected_semi_after_static_assert + 1;
	public static final int err_expected_semi_decl_list = err_expected_semi_after_stmt + 1;
	public static final int err_expected_semi_declaration = err_expected_semi_decl_list + 1;
	public static final int err_expected_semi_for = err_expected_semi_declaration + 1;
	public static final int err_expected_statement = err_expected_semi_for + 1;
	public static final int err_expected_string_literal = err_expected_statement + 1;
	public static final int err_expected_template = err_expected_string_literal + 1;
	public static final int err_expected_type = err_expected_template + 1;
	public static final int err_expected_type_id_after = err_expected_type + 1;
	public static final int err_expected_type_name_after_typename = err_expected_type_id_after + 1;
	public static final int err_expected_unqualified_id = err_expected_type_name_after_typename + 1;
	public static final int err_expected_while = err_expected_unqualified_id + 1;
	public static final int err_explicit_instantiation_with_definition = err_expected_while + 1;
	public static final int err_friend_invalid_in_context = err_explicit_instantiation_with_definition + 1;
	public static final int err_friend_storage_spec = err_friend_invalid_in_context + 1;
	public static final int err_func_def_no_params = err_friend_storage_spec + 1;
	public static final int err_function_declared_typedef = err_func_def_no_params + 1;
	public static final int err_id_after_template_in_nested_name_spec = err_function_declared_typedef + 1;
	public static final int err_id_after_template_in_typename_spec = err_id_after_template_in_nested_name_spec + 1;
	public static final int err_illegal_decl_reference_to_reference = err_id_after_template_in_typename_spec + 1;
	public static final int err_invalid_complex_spec = err_illegal_decl_reference_to_reference + 1;
	public static final int err_invalid_decl_spec_combination = err_invalid_complex_spec + 1;
	public static final int err_invalid_long_spec = err_invalid_decl_spec_combination + 1;
	public static final int err_invalid_longlong_spec = err_invalid_long_spec + 1;
	public static final int err_invalid_reference_qualifier_application = err_invalid_longlong_spec + 1;
	public static final int err_invalid_short_spec = err_invalid_reference_qualifier_application + 1;
	public static final int err_invalid_sign_spec = err_invalid_short_spec + 1;
	public static final int err_invalid_token_after_toplevel_declarator = err_invalid_sign_spec + 1;
	public static final int err_label_end_of_compound_statement = err_invalid_token_after_toplevel_declarator + 1;
	public static final int err_less_after_template_name_in_nested_name_spec = err_label_end_of_compound_statement + 1;
	public static final int err_missing_catch_finally = err_less_after_template_name_in_nested_name_spec + 1;
	public static final int err_missing_class_definition = err_missing_catch_finally + 1;
	public static final int err_missing_id_definition = err_missing_class_definition + 1;
	public static final int err_missing_param = err_missing_id_definition + 1;
	public static final int err_missing_proto_definition = err_missing_param + 1;
	public static final int err_missing_sel_definition = err_missing_proto_definition + 1;
	public static final int err_multiple_template_declarators = err_missing_sel_definition + 1;
	public static final int err_no_matching_param = err_multiple_template_declarators + 1;
	public static final int err_objc_concat_string = err_no_matching_param + 1;
	public static final int err_objc_directive_only_in_protocol = err_objc_concat_string + 1;
	public static final int err_objc_expected_equal = err_objc_directive_only_in_protocol + 1;
	public static final int err_objc_expected_property_attr = err_objc_expected_equal + 1;
	public static final int err_objc_illegal_interface_qual = err_objc_expected_property_attr + 1;
	public static final int err_objc_illegal_visibility_spec = err_objc_illegal_interface_qual + 1;
	public static final int err_objc_missing_end = err_objc_illegal_visibility_spec + 1;
	public static final int err_objc_no_attributes_on_category = err_objc_missing_end + 1;
	public static final int err_objc_propertoes_require_objc2 = err_objc_no_attributes_on_category + 1;
	public static final int err_objc_property_bitfield = err_objc_propertoes_require_objc2 + 1;
	public static final int err_objc_property_requires_field_name = err_objc_property_bitfield + 1;
	public static final int err_objc_unexpected_attr = err_objc_property_requires_field_name + 1;
	public static final int err_operator_missing_type_specifier = err_objc_unexpected_attr + 1;
	public static final int err_parse_error = err_operator_missing_type_specifier + 1;
	public static final int err_parser_impl_limit_overflow = err_parse_error + 1;
	public static final int err_rvalue_reference = err_parser_impl_limit_overflow + 1;
	public static final int err_template_spec_syntax_non_template = err_rvalue_reference + 1;
	public static final int err_two_right_angle_brackets_need_space = err_template_spec_syntax_non_template + 1;
	public static final int err_typename_invalid_functionspec = err_two_right_angle_brackets_need_space + 1;
	public static final int err_typename_invalid_storageclass = err_typename_invalid_functionspec + 1;
	public static final int err_typename_refers_to_non_type_template = err_typename_invalid_storageclass + 1;
	public static final int err_typename_requires_specqual = err_typename_refers_to_non_type_template + 1;
	public static final int err_unexpected_at = err_typename_requires_specqual + 1;
	public static final int err_unexpected_namespace_attributes_alias = err_unexpected_at + 1;
	public static final int err_unexpected_template_spec_in_using = err_unexpected_namespace_attributes_alias + 1;
	public static final int err_unexpected_typedef_ident = err_unexpected_template_spec_in_using + 1;
	public static final int err_unknown_typename = err_unexpected_typedef_ident + 1;
	public static final int err_unspecified_vla_size_with_static = err_unknown_typename + 1;
	public static final int err_use_of_tag_name_without_tag = err_unspecified_vla_size_with_static + 1;
	public static final int err_using_namespace_in_class = err_use_of_tag_name_without_tag + 1;
	public static final int err_variadic_templates = err_using_namespace_in_class + 1;
	public static final int error_property_ivar_decl = err_variadic_templates + 1;
	public static final int ext_c99_compound_literal = error_property_ivar_decl + 1;
	public static final int ext_c99_variable_decl_in_for_loop = ext_c99_compound_literal + 1;
	public static final int ext_duplicate_declspec = ext_c99_variable_decl_in_for_loop + 1;
	public static final int ext_ellipsis_exception_spec = ext_duplicate_declspec + 1;
	public static final int ext_empty_source_file = ext_ellipsis_exception_spec + 1;
	public static final int ext_empty_struct_union_enum = ext_empty_source_file + 1;
	public static final int ext_enumerator_list_comma = ext_empty_struct_union_enum + 1;
	public static final int ext_expected_semi_decl_list = ext_enumerator_list_comma + 1;
	public static final int ext_extra_struct_semi = ext_expected_semi_decl_list + 1;
	public static final int ext_gnu_address_of_label = ext_extra_struct_semi + 1;
	public static final int ext_gnu_array_range = ext_gnu_address_of_label + 1;
	public static final int ext_gnu_case_range = ext_gnu_array_range + 1;
	public static final int ext_gnu_conditional_expr = ext_gnu_case_range + 1;
	public static final int ext_gnu_empty_initializer = ext_gnu_conditional_expr + 1;
	public static final int ext_gnu_indirect_goto = ext_gnu_empty_initializer + 1;
	public static final int ext_gnu_local_label = ext_gnu_indirect_goto + 1;
	public static final int ext_gnu_missing_equal_designator = ext_gnu_local_label + 1;
	public static final int ext_gnu_old_style_field_designator = ext_gnu_missing_equal_designator + 1;
	public static final int ext_gnu_statement_expr = ext_gnu_old_style_field_designator + 1;
	public static final int ext_ident_list_in_param = ext_gnu_statement_expr + 1;
	public static final int ext_integer_complex = ext_ident_list_in_param + 1;
	public static final int ext_plain_complex = ext_integer_complex + 1;
	public static final int ext_thread_before = ext_plain_complex + 1;
	public static final int ext_top_level_semi = ext_thread_before + 1;
	public static final int w_asm_qualifier_ignored = ext_top_level_semi + 1;
	public static final int warn_cxx0x_right_shift_in_template_arg = w_asm_qualifier_ignored + 1;
	public static final int warn_expected_implementation = warn_cxx0x_right_shift_in_template_arg + 1;
	public static final int warn_objc_protocol_qualifier_missing_id = warn_expected_implementation + 1;
	public static final int warn_parens_disambiguated_as_function_decl = warn_objc_protocol_qualifier_missing_id + 1;
	public static final int warn_pragma_expected_identifier = warn_parens_disambiguated_as_function_decl + 1;
	public static final int warn_pragma_expected_lparen = warn_pragma_expected_identifier + 1;
	public static final int warn_pragma_expected_rparen = warn_pragma_expected_lparen + 1;
	public static final int warn_pragma_extra_tokens_at_eol = warn_pragma_expected_rparen + 1;
	public static final int warn_pragma_pack_invalid_action = warn_pragma_extra_tokens_at_eol + 1;
	public static final int warn_pragma_pack_invalid_constant = warn_pragma_pack_invalid_action + 1;
	public static final int warn_pragma_pack_malformed = warn_pragma_pack_invalid_constant + 1;
	public static final int warn_pragma_unused_expected_punc = warn_pragma_pack_malformed + 1;
	public static final int warn_pragma_unused_expected_var = warn_pragma_unused_expected_punc + 1;

}