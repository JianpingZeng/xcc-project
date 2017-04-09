package jlang.diag;

import static jlang.diag.DiagnosticSemaTag.DiagnosticSemaKindsEnd;

public interface DiagnosticCommonKindsTag
{
	public static final int DiagnosticCommonKindsBegin = DiagnosticSemaKindsEnd;

	public static final int err_expected_colon = DiagnosticCommonKindsBegin;
	public static final int err_expected_namespace_name = err_expected_colon + 1;
	public static final int err_invalid_storage_class_in_func_decl = err_expected_namespace_name + 1;
	public static final int err_no_declarators = err_invalid_storage_class_in_func_decl + 1;
	public static final int err_param_redefinition = err_no_declarators + 1;
	public static final int err_pp_I_dash_not_supported = err_param_redefinition + 1;
	public static final int ext_longlong = err_pp_I_dash_not_supported + 1;
	public static final int ext_no_declarators = ext_longlong + 1;
	public static final int note_also_found_decl = ext_no_declarators + 1;
	public static final int note_duplicate_case_prev = note_also_found_decl + 1;
	public static final int note_forward_declaration = note_duplicate_case_prev + 1;
	public static final int note_invalid_subexpr_in_ice = note_forward_declaration + 1;
	public static final int note_matching = note_invalid_subexpr_in_ice + 1;
	public static final int note_previous_declaration = note_matching + 1;
	public static final int note_previous_definition = note_previous_declaration + 1;
	public static final int note_previous_implicit_declaration = note_previous_definition + 1;
	public static final int note_previous_use = note_previous_implicit_declaration + 1;
	public static final int note_type_being_defined = note_previous_use + 1;
	public static final int note_using_decl = note_type_being_defined + 1;
	public static final int warn_integer_too_large = note_using_decl + 1;
	public static final int warn_integer_too_large_for_signed = warn_integer_too_large + 1;

	public static final int DiagnosticCommonKindsEnd = warn_integer_too_large_for_signed + 1;
}