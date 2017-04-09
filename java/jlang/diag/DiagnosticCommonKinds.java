package jlang.diag;
import static jlang.diag.Diagnostic.Level.*;
import static jlang.diag.Diagnostic.Mapping.*;

public enum DiagnosticCommonKinds implements DiagnosticCommonKindsTag
{
	ERR_EXPECTED_COLON(err_expected_colon, Error, MAP_ERROR, "expected ':'"),
	ERR_EXPECTED_NAMESPACE_NAME(err_expected_namespace_name, Error, MAP_ERROR, "expected namespace name"),
	ERR_INVALID_STORAGE_CLASS_IN_FUNC_DECL(err_invalid_storage_class_in_func_decl, Error, MAP_ERROR, "invalid storage class specifier in function declarator"),
	ERR_NO_DECLARATORS(err_no_declarators, Error, MAP_ERROR, "declaration does not declare anything"),
	ERR_PARAM_REDEFINITION(err_param_redefinition, Error, MAP_ERROR, "redefinition of parameter %0"),
	ERR_PP_I_DASH_NOT_SUPPORTED(err_pp_I_dash_not_supported, Error, MAP_ERROR, "-I- not supported, please use -iquote instead"),
	EXT_LONGLONG(ext_longlong, Note, MAP_IGNORE, "'long long' is an extension when C99 mode is not enabled"),
	EXT_NO_DECLARATORS(ext_no_declarators, Note, MAP_WARNING, "declaration does not declare anything"),
	NOTE_ALSO_FOUND_DECL(note_also_found_decl, Note, MAP_FATAL, "also found"),
	NOTE_DUPLICATE_CASE_PREV(note_duplicate_case_prev, Note, MAP_FATAL, "previous case defined here"),
	NOTE_FORWARD_DECLARATION(note_forward_declaration, Note, MAP_FATAL, "forward declaration of %0"),
	NOTE_INVALID_SUBEXPR_IN_ICE(note_invalid_subexpr_in_ice, Note, MAP_FATAL, "subexpression not valid in an integer constant expression"),
	NOTE_MATCHING(note_matching, Note, MAP_FATAL, "to match this '%0'"),
	NOTE_PREVIOUS_DECLARATION(note_previous_declaration, Note, MAP_FATAL, "previous declaration is here"),
	NOTE_PREVIOUS_DEFINITION(note_previous_definition, Note, MAP_FATAL, "previous definition is here"),
	NOTE_PREVIOUS_IMPLICIT_DECLARATION(note_previous_implicit_declaration, Note, MAP_FATAL, "previous implicit declaration is here"),
	NOTE_PREVIOUS_USE(note_previous_use, Note, MAP_FATAL, "previous use is here"),
	NOTE_TYPE_BEING_DEFINED(note_type_being_defined, Note, MAP_FATAL, "definition of %0 is not complete until the closing '}'"),
	NOTE_USING_DECL(note_using_decl, Note, MAP_FATAL, "using"),
	WARN_INTEGER_TOO_LARGE(warn_integer_too_large, Warning, MAP_WARNING, "integer constant is too large for its type"),
	WARN_INTEGER_TOO_LARGE_FOR_SIGNED(warn_integer_too_large_for_signed, Warning, MAP_WARNING, "integer constant is so large that it is unsigned");

	public int diagID;
	public Diagnostic.Level diagClass;
	public Diagnostic.Mapping diagMapping;
	public String text;

	DiagnosticCommonKinds(int diagID, Diagnostic.Level diagClass,
			Diagnostic.Mapping diagMapping, String text)
	{
		this.diagID = diagID;
		this.diagClass = diagClass;
		this.diagMapping = diagMapping;
		this.text = text;
	}
}
