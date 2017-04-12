package jlang.diag;

import static jlang.diag.Diagnostic.DiagnosticClass.*;
import static jlang.diag.Diagnostic.Mapping.*;

public enum DiagnosticLexKinds implements DiagnosticLexKindsTag 
{
	BACKSLASH_NEWLINE_SPACE(backslash_newline_space, CLASS_WARNING, MAP_WARNING, "backslash and newline separated by space"),
	CHARIZE_MICROSOFT_EXT(charize_microsoft_ext, CLASS_NOTE, MAP_IGNORE, "@# is a microsoft extension"),
	ERR__PRAGMA_MALFORMED(err__Pragma_malformed, CLASS_ERROR, MAP_ERROR, "_Pragma takes a parenthesized string literal"),
	ERR_DEFINED_MACRO_NAME(err_defined_macro_name, CLASS_ERROR, MAP_ERROR, "'defined' cannot be used as a macro name"),
	ERR_EMPTY_CHARACTER(err_empty_character, CLASS_ERROR, MAP_ERROR, "empty character constant"),
	ERR_EXPONENT_HAS_NO_DIGITS(err_exponent_has_no_digits, CLASS_ERROR, MAP_ERROR, "exponent has no digits"),
	ERR_FEATURE_CHECK_MALFORMED(err_feature_check_malformed, CLASS_ERROR, MAP_ERROR, "builtin feature check macro requires a parenthesized identifier"),
	ERR_HEX_ESCAPE_NO_DIGITS(err_hex_escape_no_digits, CLASS_ERROR, MAP_ERROR, "\\x used with no following hex digits"),
	ERR_HEXCONSTANT_REQUIRES_EXPONENT(err_hexconstant_requires_exponent, CLASS_ERROR, MAP_ERROR, "hexadecimal floating constants require an exponent"),
	ERR_INVALID_BINARY_DIGIT(err_invalid_binary_digit, CLASS_ERROR, MAP_ERROR, "invalid digit '%0' in binary constant"),
	ERR_INVALID_CHARACTER_TO_CHARIFY(err_invalid_character_to_charify, CLASS_ERROR, MAP_ERROR, "invalid argument to convert to character"),
	ERR_INVALID_DECIMAL_DIGIT(err_invalid_decimal_digit, CLASS_ERROR, MAP_ERROR, "invalid digit '%0' in decimal constant"),
	ERR_INVALID_OCTAL_DIGIT(err_invalid_octal_digit, CLASS_ERROR, MAP_ERROR, "invalid digit '%0' in octal constant"),
	ERR_INVALID_SUFFIX_FLOAT_CONSTANT(err_invalid_suffix_float_constant, CLASS_ERROR, MAP_ERROR, "invalid suffix '%0' on floating constant"),
	ERR_INVALID_SUFFIX_INTEGER_CONSTANT(err_invalid_suffix_integer_constant, CLASS_ERROR, MAP_ERROR, "invalid suffix '%0' on integer constant"),
	ERR_LEXER_ERROR(err_lexer_error, CLASS_ERROR, MAP_ERROR, "lexer error"),
	ERR_PASCAL_STRING_TOO_LONG(err_pascal_string_too_long, CLASS_ERROR, MAP_ERROR, "Pascal string is too long"),
	ERR_PASTE_AT_END(err_paste_at_end, CLASS_ERROR, MAP_ERROR, "'##' cannot appear at end of macro expansion"),
	ERR_PASTE_AT_START(err_paste_at_start, CLASS_ERROR, MAP_ERROR, "'##' cannot appear at start of macro expansion"),
	ERR_PP_BAD_PASTE(err_pp_bad_paste, CLASS_ERROR, MAP_ERROR, "pasting formed '%0', an invalid preprocessing token"),
	ERR_PP_COLON_WITHOUT_QUESTION(err_pp_colon_without_question, CLASS_ERROR, MAP_ERROR, "':' without preceding '?'"),
	ERR_PP_DEFINED_REQUIRES_IDENTIFIER(err_pp_defined_requires_identifier, CLASS_ERROR, MAP_ERROR, "operator 'defined' requires an identifier"),
	ERR_PP_DIVISION_BY_ZERO(err_pp_division_by_zero, CLASS_ERROR, MAP_ERROR, "division by zero in preprocessor expression"),
	ERR_PP_DUPLICATE_NAME_IN_ARG_LIST(err_pp_duplicate_name_in_arg_list, CLASS_ERROR, MAP_ERROR, "duplicate macro parameter name %0"),
	ERR_PP_EMPTY_FILENAME(err_pp_empty_filename, CLASS_ERROR, MAP_ERROR, "empty filename"),
	ERR_PP_ENDIF_WITHOUT_IF(err_pp_endif_without_if, CLASS_ERROR, MAP_ERROR, "#endif without #if"),
	ERR_PP_EXPECTED_COMMA_IN_ARG_LIST(err_pp_expected_comma_in_arg_list, CLASS_ERROR, MAP_ERROR, "expected comma in macro parameter list"),
	ERR_PP_EXPECTED_EOL(err_pp_expected_eol, CLASS_ERROR, MAP_ERROR, "expected end of line in preprocessor expression"),
	ERR_PP_EXPECTED_IDENT_IN_ARG_LIST(err_pp_expected_ident_in_arg_list, CLASS_ERROR, MAP_ERROR, "expected identifier in macro parameter list"),
	ERR_PP_EXPECTED_RPAREN(err_pp_expected_rparen, CLASS_ERROR, MAP_ERROR, "expected ')' in preprocessor expression"),
	ERR_PP_EXPECTED_VALUE_IN_EXPR(err_pp_expected_value_in_expr, CLASS_ERROR, MAP_ERROR, "expected value in expression"),
	ERR_PP_EXPECTS_FILENAME(err_pp_expects_filename, CLASS_ERROR, MAP_ERROR, "expected \"FILENAME\" or <FILENAME>"),
	ERR_PP_EXPR_BAD_TOKEN_BINOP(err_pp_expr_bad_token_binop, CLASS_ERROR, MAP_ERROR, "token is not a valid binary operator in a preprocessor subexpression"),
	ERR_PP_EXPR_BAD_TOKEN_START_EXPR(err_pp_expr_bad_token_start_expr, CLASS_ERROR, MAP_ERROR, "invalid token at start of a preprocessor expression"),
	ERR_PP_FILE_NOT_FOUND(err_pp_file_not_found, CLASS_ERROR, MAP_FATAL, "'%0' file not found"),
	ERR_PP_HASH_ERROR(err_pp_hash_error, CLASS_ERROR, MAP_ERROR, "#error%0"),
	ERR_PP_ILLEGAL_FLOATING_LITERAL(err_pp_illegal_floating_literal, CLASS_ERROR, MAP_ERROR, "floating point literal in preprocessor expression"),
	ERR_PP_INCLUDE_TOO_DEEP(err_pp_include_too_deep, CLASS_ERROR, MAP_ERROR, "#include nested too deeply"),
	ERR_PP_INVALID_DIRECTIVE(err_pp_invalid_directive, CLASS_ERROR, MAP_ERROR, "invalid preprocessing directive"),
	ERR_PP_INVALID_POISON(err_pp_invalid_poison, CLASS_ERROR, MAP_ERROR, "can only poison identifier tokens"),
	ERR_PP_INVALID_TOK_IN_ARG_LIST(err_pp_invalid_tok_in_arg_list, CLASS_ERROR, MAP_ERROR, "invalid token in macro parameter list"),
	ERR_PP_LINE_DIGIT_SEQUENCE(err_pp_line_digit_sequence, CLASS_ERROR, MAP_ERROR, "#line directive requires a simple digit sequence"),
	ERR_PP_LINE_INVALID_FILENAME(err_pp_line_invalid_filename, CLASS_ERROR, MAP_ERROR, "invalid filename for #line directive"),
	ERR_PP_LINE_REQUIRES_INTEGER(err_pp_line_requires_integer, CLASS_ERROR, MAP_ERROR, "#line directive requires a positive integer argument"),
	ERR_PP_LINEMARKER_INVALID_FILENAME(err_pp_linemarker_invalid_filename, CLASS_ERROR, MAP_ERROR, "invalid filename for line marker directive"),
	ERR_PP_LINEMARKER_INVALID_FLAG(err_pp_linemarker_invalid_flag, CLASS_ERROR, MAP_ERROR, "invalid flag line marker directive"),
	ERR_PP_LINEMARKER_INVALID_POP(err_pp_linemarker_invalid_pop, CLASS_ERROR, MAP_ERROR, "invalid line marker flag '2': cannot pop empty include stack"),
	ERR_PP_LINEMARKER_REQUIRES_INTEGER(err_pp_linemarker_requires_integer, CLASS_ERROR, MAP_ERROR, "line marker directive requires a positive integer argument"),
	ERR_PP_MACRO_NOT_IDENTIFIER(err_pp_macro_not_identifier, CLASS_ERROR, MAP_ERROR, "macro names must be identifiers"),
	ERR_PP_MALFORMED_IDENT(err_pp_malformed_ident, CLASS_ERROR, MAP_ERROR, "invalid #ident directive"),
	ERR_PP_MISSING_MACRO_NAME(err_pp_missing_macro_name, CLASS_ERROR, MAP_ERROR, "macro name missing"),
	ERR_PP_MISSING_RPAREN(err_pp_missing_rparen, CLASS_ERROR, MAP_ERROR, "missing ')' after 'defined'"),
	ERR_PP_MISSING_RPAREN_IN_MACRO_DEF(err_pp_missing_rparen_in_macro_def, CLASS_ERROR, MAP_ERROR, "missing ')' in macro parameter list"),
	ERR_PP_MISSING_VAL_BEFORE_OPERATOR(err_pp_missing_val_before_operator, CLASS_ERROR, MAP_ERROR, "missing value before operator"),
	ERR_PP_OPERATOR_USED_AS_MACRO_NAME(err_pp_operator_used_as_macro_name, CLASS_ERROR, MAP_ERROR, "C++ operator '%0' cannot be used as a macro name"),
	ERR_PP_REMAINDER_BY_ZERO(err_pp_remainder_by_zero, CLASS_ERROR, MAP_ERROR, "remainder by zero in preprocessor expression"),
	ERR_PP_STRINGIZE_NOT_PARAMETER(err_pp_stringize_not_parameter, CLASS_ERROR, MAP_ERROR, "'#' is not followed by a macro parameter"),
	ERR_PP_UNTERMINATED_CONDITIONAL(err_pp_unterminated_conditional, CLASS_ERROR, MAP_ERROR, "unterminated conditional directive"),
	ERR_PP_USED_POISONED_ID(err_pp_used_poisoned_id, CLASS_ERROR, MAP_ERROR, "attempt to use a poisoned identifier"),
	ERR_PRAGMA_COMMENT_MALFORMED(err_pragma_comment_malformed, CLASS_ERROR, MAP_ERROR, "pragma comment requires parenthesized identifier and optional string"),
	ERR_PRAGMA_COMMENT_UNKNOWN_KIND(err_pragma_comment_unknown_kind, CLASS_ERROR, MAP_ERROR, "unknown kind of pragma comment"),
	ERR_TOO_FEW_ARGS_IN_MACRO_INVOC(err_too_few_args_in_macro_invoc, CLASS_ERROR, MAP_ERROR, "too few arguments provided to function-like macro invocation"),
	ERR_TOO_MANY_ARGS_IN_MACRO_INVOC(err_too_many_args_in_macro_invoc, CLASS_ERROR, MAP_ERROR, "too many arguments provided to function-like macro invocation"),
	ERR_UCN_ESCAPE_INCOMPLETE(err_ucn_escape_incomplete, CLASS_ERROR, MAP_ERROR, "incomplete universal character name"),
	ERR_UCN_ESCAPE_INVALID(err_ucn_escape_invalid, CLASS_ERROR, MAP_ERROR, "invalid universal character"),
	ERR_UCN_ESCAPE_NO_DIGITS(err_ucn_escape_no_digits, CLASS_ERROR, MAP_ERROR, "\\u used with no following hex digits"),
	ERR_UCN_ESCAPE_TOO_BIG(err_ucn_escape_too_big, CLASS_ERROR, MAP_ERROR, "universal character name is too long"),
	ERR_UNTERM_MACRO_INVOC(err_unterm_macro_invoc, CLASS_ERROR, MAP_ERROR, "unterminated function-like macro invocation"),
	ERR_UNTERMINATED_BLOCK_COMMENT(err_unterminated_block_comment, CLASS_ERROR, MAP_ERROR, "unterminated /* comment"),
	ERR_UNTERMINATED_CHAR(err_unterminated_char, CLASS_ERROR, MAP_ERROR, "missing terminating ' character"),
	ERR_UNTERMINATED_STRING(err_unterminated_string, CLASS_ERROR, MAP_ERROR, "missing terminating '\"' character"),
	ESCAPED_NEWLINE_BLOCK_COMMENT_END(escaped_newline_block_comment_end, CLASS_WARNING, MAP_WARNING, "escaped newline between */ characters at block comment end"),
	EXT_BACKSLASH_NEWLINE_EOF(ext_backslash_newline_eof, CLASS_NOTE, MAP_IGNORE, "backslash-newline at end of file"),
	EXT_BCPL_COMMENT(ext_bcpl_comment, CLASS_NOTE, MAP_IGNORE, "// comments are not allowed in this language"),
	EXT_BINARY_LITERAL(ext_binary_literal, CLASS_NOTE, MAP_IGNORE, "binary integer literals are an extension"),
	EXT_C99_WHITESPACE_REQUIRED_AFTER_MACRO_NAME(ext_c99_whitespace_required_after_macro_name, CLASS_NOTE, MAP_WARNING, "ISO C99 requires whitespace after the macro name"),
	EXT_DOLLAR_IN_IDENTIFIER(ext_dollar_in_identifier, CLASS_NOTE, MAP_IGNORE, "'$' in identifier"),
	EXT_EMBEDDED_DIRECTIVE(ext_embedded_directive, CLASS_NOTE, MAP_IGNORE, "embedding a directive within macro arguments is not portable"),
	EXT_EMPTY_FNMACRO_ARG(ext_empty_fnmacro_arg, CLASS_NOTE, MAP_IGNORE, "empty macro arguments were standardized in C99"),
	EXT_FOUR_CHAR_CHARACTER_LITERAL(ext_four_char_character_literal, CLASS_NOTE, MAP_IGNORE, "multi-character character constant"),
	EXT_HEXCONSTANT_INVALID(ext_hexconstant_invalid, CLASS_NOTE, MAP_IGNORE, "hexadecimal floating constants are a C99 feature"),
	EXT_IMAGINARY_CONSTANT(ext_imaginary_constant, CLASS_NOTE, MAP_IGNORE, "imaginary constants are an extension"),
	EXT_MISSING_VARARGS_ARG(ext_missing_varargs_arg, CLASS_NOTE, MAP_IGNORE, "varargs argument missing, but tolerated as an extension"),
	EXT_MISSING_WHITESPACE_AFTER_MACRO_NAME(ext_missing_whitespace_after_macro_name, CLASS_NOTE, MAP_WARNING, "whitespace required after macro name"),
	EXT_MULTI_LINE_BCPL_COMMENT(ext_multi_line_bcpl_comment, CLASS_NOTE, MAP_IGNORE, "multi-line // comment"),
	EXT_MULTICHAR_CHARACTER_LITERAL(ext_multichar_character_literal, CLASS_NOTE, MAP_WARNING, "multi-character character constant"),
	EXT_NAMED_VARIADIC_MACRO(ext_named_variadic_macro, CLASS_NOTE, MAP_IGNORE, "named variadic macros are a GNU extension"),
	EXT_NO_NEWLINE_EOF(ext_no_newline_eof, CLASS_NOTE, MAP_IGNORE, "no newline at end of file"),
	EXT_NONSTANDARD_ESCAPE(ext_nonstandard_escape, CLASS_NOTE, MAP_IGNORE, "use of non-standard escape character '\\%0'"),
	EXT_PASTE_COMMA(ext_paste_comma, CLASS_NOTE, MAP_IGNORE, "Use of comma pasting extension is non-portable"),
	EXT_PP_BAD_VAARGS_USE(ext_pp_bad_vaargs_use, CLASS_NOTE, MAP_IGNORE, "__VA_ARGS__ can only appear in the expansion of a C99 variadic macro"),
	EXT_PP_BASE_FILE(ext_pp_base_file, CLASS_NOTE, MAP_IGNORE, "__BASE_FILE__ is a language extension"),
	EXT_PP_COMMA_EXPR(ext_pp_comma_expr, CLASS_NOTE, MAP_IGNORE, "comma operator in operand of #if"),
	EXT_PP_COUNTER(ext_pp_counter, CLASS_NOTE, MAP_IGNORE, "__COUNTER__ is a language extension"),
	EXT_PP_EXTRA_TOKENS_AT_EOL(ext_pp_extra_tokens_at_eol, CLASS_NOTE, MAP_WARNING, "extra tokens at end of #%0 directive"),
	EXT_PP_IDENT_DIRECTIVE(ext_pp_ident_directive, CLASS_NOTE, MAP_IGNORE, "#ident is a language extension"),
	EXT_PP_IMPORT_DIRECTIVE(ext_pp_import_directive, CLASS_NOTE, MAP_IGNORE, "#import is a language extension"),
	EXT_PP_INCLUDE_LEVEL(ext_pp_include_level, CLASS_NOTE, MAP_IGNORE, "__INCLUDE_LEVEL__ is a language extension"),
	EXT_PP_INCLUDE_NEXT_DIRECTIVE(ext_pp_include_next_directive, CLASS_NOTE, MAP_IGNORE, "#include_next is a language extension"),
	EXT_PP_LINE_TOO_BIG(ext_pp_line_too_big, CLASS_NOTE, MAP_IGNORE, "C requires #line number to be less than %0, allowed as extension"),
	EXT_PP_MACRO_REDEF(ext_pp_macro_redef, CLASS_NOTE, MAP_WARNING, "%0 macro redefined"),
	EXT_PP_TIMESTAMP(ext_pp_timestamp, CLASS_NOTE, MAP_IGNORE, "__TIMESTAMP__ is a language extension"),
	EXT_PP_WARNING_DIRECTIVE(ext_pp_warning_directive, CLASS_NOTE, MAP_IGNORE, "#warning is a language extension"),
	EXT_STDC_PRAGMA_IGNORED(ext_stdc_pragma_ignored, CLASS_NOTE, MAP_WARNING, "unknown pragma in STDC namespace"),
	EXT_STDC_PRAGMA_SYNTAX(ext_stdc_pragma_syntax, CLASS_NOTE, MAP_WARNING, "expected 'ON' or 'OFF' or 'DEFAULT' in pragma"),
	EXT_STDC_PRAGMA_SYNTAX_EOM(ext_stdc_pragma_syntax_eom, CLASS_NOTE, MAP_WARNING, "expected end of macro in STDC pragma"),
	EXT_TOKEN_USED(ext_token_used, CLASS_NOTE, MAP_IGNORE, "extension used"),
	EXT_UNKNOWN_ESCAPE(ext_unknown_escape, CLASS_NOTE, MAP_WARNING, "unknown escape sequence '\\%0'"),
	EXT_VARIADIC_MACRO(ext_variadic_macro, CLASS_NOTE, MAP_IGNORE, "variadic macros were introduced in C99"),
	NULL_IN_CHAR(null_in_char, CLASS_WARNING, MAP_WARNING, "null character(s) preserved in character literal"),
	NULL_IN_FILE(null_in_file, CLASS_WARNING, MAP_WARNING, "null character ignored"),
	NULL_IN_STRING(null_in_string, CLASS_WARNING, MAP_WARNING, "null character(s) preserved in string literal"),
	PP_ERR_ELIF_AFTER_ELSE(pp_err_elif_after_else, CLASS_ERROR, MAP_ERROR, "#elif after #else"),
	PP_ERR_ELIF_WITHOUT_IF(pp_err_elif_without_if, CLASS_ERROR, MAP_ERROR, "#elif without #if"),
	PP_ERR_ELSE_AFTER_ELSE(pp_err_else_after_else, CLASS_ERROR, MAP_ERROR, "#else after #else"),
	PP_ERR_ELSE_WITHOUT_IF(pp_err_else_without_if, CLASS_ERROR, MAP_ERROR, "#else without #if"),
	PP_HASH_WARNING(pp_hash_warning, CLASS_WARNING, MAP_WARNING, "#warning%0"),
	PP_INCLUDE_MACROS_OUT_OF_PREDEFINES(pp_include_macros_out_of_predefines, CLASS_ERROR, MAP_ERROR, "the #__include_macros directive is only for internal use by -imacros"),
	PP_INCLUDE_NEXT_ABSOLUTE_PATH(pp_include_next_absolute_path, CLASS_WARNING, MAP_WARNING, "#include_next with absolute path"),
	PP_INCLUDE_NEXT_IN_PRIMARY(pp_include_next_in_primary, CLASS_WARNING, MAP_WARNING, "#include_next in primary source file"),
	PP_INVALID_STRING_LITERAL(pp_invalid_string_literal, CLASS_WARNING, MAP_WARNING, "invalid string literal, ignoring final '\'"),
	PP_MACRO_NOT_USED(pp_macro_not_used, CLASS_WARNING, MAP_IGNORE, "macro is not used"),
	PP_OUT_OF_DATE_DEPENDENCY(pp_out_of_date_dependency, CLASS_WARNING, MAP_WARNING, "current file is older than dependency %0"),
	PP_POISONING_EXISTING_MACRO(pp_poisoning_existing_macro, CLASS_WARNING, MAP_WARNING, "poisoning existing macro"),
	PP_PRAGMA_ONCE_IN_MAIN_FILE(pp_pragma_once_in_main_file, CLASS_WARNING, MAP_WARNING, "#pragma once in main file"),
	PP_PRAGMA_SYSHEADER_IN_MAIN_FILE(pp_pragma_sysheader_in_main_file, CLASS_WARNING, MAP_WARNING, "#pragma system_header ignored in main file"),
	PP_REDEF_BUILTIN_MACRO(pp_redef_builtin_macro, CLASS_WARNING, MAP_WARNING, "redefining builtin macro"),
	PP_UNDEF_BUILTIN_MACRO(pp_undef_builtin_macro, CLASS_WARNING, MAP_WARNING, "undefining builtin macro"),
	TRIGRAPH_CONVERTED(trigraph_converted, CLASS_WARNING, MAP_WARNING, "trigraph converted to '%0' character"),
	TRIGRAPH_ENDS_BLOCK_COMMENT(trigraph_ends_block_comment, CLASS_WARNING, MAP_WARNING, "trigraph ends block comment"),
	TRIGRAPH_IGNORED(trigraph_ignored, CLASS_WARNING, MAP_WARNING, "trigraph ignored"),
	TRIGRAPH_IGNORED_BLOCK_COMMENT(trigraph_ignored_block_comment, CLASS_WARNING, MAP_WARNING, "ignored trigraph would end block comment"),
	WARN_CHAR_CONSTANT_TOO_LARGE(warn_char_constant_too_large, CLASS_WARNING, MAP_WARNING, "character constant too long for its type"),
	WARN_EXTRANEOUS_WIDE_CHAR_CONSTANT(warn_extraneous_wide_char_constant, CLASS_WARNING, MAP_WARNING, "extraneous characters in wide character constant ignored"),
	WARN_HEX_ESCAPE_TOO_LARGE(warn_hex_escape_too_large, CLASS_NOTE, MAP_WARNING, "hex escape sequence out of range"),
	WARN_MISSING_WHITESPACE_AFTER_MACRO_NAME(warn_missing_whitespace_after_macro_name, CLASS_WARNING, MAP_WARNING, "whitespace recommended after macro name"),
	WARN_NESTED_BLOCK_COMMENT(warn_nested_block_comment, CLASS_WARNING, MAP_WARNING, "'/*' within block comment"),
	WARN_OCTAL_ESCAPE_TOO_LARGE(warn_octal_escape_too_large, CLASS_NOTE, MAP_WARNING, "octal escape sequence out of range"),
	WARN_PP_CONVERT_LHS_TO_POSITIVE(warn_pp_convert_lhs_to_positive, CLASS_WARNING, MAP_WARNING, "left side of operator converted from negative value to unsigned: %0"),
	WARN_PP_CONVERT_RHS_TO_POSITIVE(warn_pp_convert_rhs_to_positive, CLASS_WARNING, MAP_WARNING, "right side of operator converted from negative value to unsigned: %0"),
	WARN_PP_EXPR_OVERFLOW(warn_pp_expr_overflow, CLASS_WARNING, MAP_WARNING, "integer overflow in preprocessor expression"),
	WARN_PP_LINE_DECIMAL(warn_pp_line_decimal, CLASS_WARNING, MAP_WARNING, "#line directive interprets number as decimal, not octal"),
	WARN_PP_UNDEF_IDENTIFIER(warn_pp_undef_identifier, CLASS_WARNING, MAP_IGNORE, "%0 is not defined, evaluates to 0"),
	WARN_PRAGMA_DIAGNOSTIC_CLANG_CANNOT_PPP(warn_pragma_diagnostic_clang_cannot_ppp, CLASS_NOTE, MAP_WARNING, "pragma diagnostic pop could not pop, no matching push"),
	WARN_PRAGMA_DIAGNOSTIC_CLANG_INVALID(warn_pragma_diagnostic_clang_invalid, CLASS_NOTE, MAP_WARNING, "pragma diagnostic expected 'error', 'warning', 'ignored', 'fatal' 'push', or 'pop'"),
	WARN_PRAGMA_DIAGNOSTIC_GCC_INVALID(warn_pragma_diagnostic_gcc_invalid, CLASS_NOTE, MAP_WARNING, "pragma diagnostic expected 'error', 'warning', 'ignored', or 'fatal'"),
	WARN_PRAGMA_DIAGNOSTIC_INVALID_OPTION(warn_pragma_diagnostic_invalid_option, CLASS_NOTE, MAP_WARNING, "pragma diagnostic expected option name (e.g. \"-Wundef\")"),
	WARN_PRAGMA_DIAGNOSTIC_INVALID_TOKEN(warn_pragma_diagnostic_invalid_token, CLASS_NOTE, MAP_WARNING, "unexpected token in pragma diagnostic"),
	WARN_PRAGMA_DIAGNOSTIC_UNKNOWN_WARNING(warn_pragma_diagnostic_unknown_warning, CLASS_NOTE, MAP_WARNING, "unknown warning group '%0', ignored"),
	WARN_PRAGMA_IGNORED(warn_pragma_ignored, CLASS_WARNING, MAP_IGNORE, "unknown pragma ignored"),
	WARN_STDC_FENV_ACCESS_NOT_SUPPORTED(warn_stdc_fenv_access_not_supported, CLASS_WARNING, MAP_WARNING, "pragma STDC FENV_ACCESS ON is not supported, ignoring pragma");

	public int diagID;
	public Diagnostic.DiagnosticClass diagClass;
	public Diagnostic.Mapping diagMapping;
	public boolean sfinae;
	public String optionGroup;
	public String text;

	DiagnosticLexKinds(int diagID, Diagnostic.DiagnosticClass diagClass,
			Diagnostic.Mapping diagMapping, String text)
	{
		this.diagID = diagID;
		this.diagClass = diagClass;
		this.diagMapping = diagMapping;
		this.text = text;
	}
}
