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

import static jlang.diag.Diagnostic.DiagnosticClass.*;
import static jlang.diag.Diagnostic.Mapping.*;

/**
 * @author xlous.zeng
 * @version 0.1
 */
public enum DiagnosticParseKinds implements DiagnosticParseTag
{
	ERR_ANON_TYPE_DEFINITION(err_anon_type_definition, CLASS_ERROR, MAP_ERROR,
			"declaration of anonymous %0 must be a definition"),
	ERR_ARGUMENT_REQUIRED_AFTER_ATTRIBUTE(err_argument_required_after_attribute,
			CLASS_ERROR, MAP_ERROR, "argument required after attribute"),
	ERR_DECLARATION_DOES_NOT_DECLARE_PARAM(
			err_declaration_does_not_declare_param, CLASS_ERROR, MAP_ERROR,
			"declaration does not declare a parameter"),
	ERR_DESTRUCTOR_CLASS_NAME(err_destructor_class_name, CLASS_ERROR, MAP_ERROR,
			"expected the class name after '~' to name a destructor"),
	ERR_DUP_VIRTUAL(err_dup_virtual, CLASS_ERROR, MAP_ERROR,
			"duplicate 'virtual' in base specifier"),
	ERR_ENUMERATOR_UNNAMED_NO_DEF(err_enumerator_unnamed_no_def, CLASS_ERROR, MAP_ERROR, ""),
	ERR_EXPECTED_ASM_OPERAND(err_expected_asm_operand, CLASS_ERROR, MAP_ERROR,
			"expected string literal or '[' for asm operand"),
	ERR_EXPECTED_CASE_BEFORE_EXPRESSION(err_expected_case_before_expression, CLASS_ERROR, MAP_ERROR, "expected 'case' keyword before expression"),
	ERR_EXPECTED_CATCH(err_expected_catch, CLASS_ERROR, MAP_ERROR, "expected catch"),
	ERR_EXPECTED_CLASS_BEFORE(err_expected_class_before, CLASS_ERROR, MAP_ERROR,
			"expected 'class' before '%0'"),
	ERR_EXPECTED_CLASS_NAME(err_expected_class_name, CLASS_ERROR, MAP_ERROR,
			"expected class name"),
	ERR_EXPECTED_COLON_AFTER(err_expected_colon_after, CLASS_ERROR, MAP_ERROR,
			"expected ':' after %0"),
	ERR_EXPECTED_COMMA(err_expected_comma, CLASS_ERROR, MAP_ERROR, "expected ','"),
	ERR_EXPECTED_COMMA_GREATER(err_expected_comma_greater, CLASS_ERROR, MAP_ERROR,
			"expected ',' or '>' in template-parameter-list"),
	ERR_EXPECTED_EQUAL_AFTER_DECLARATOR(err_expected_equal_after_declarator,
			CLASS_ERROR, MAP_ERROR, "expected '=' after declarator"),
	ERR_EXPECTED_EQUAL_DESIGNATOR(err_expected_equal_designator, CLASS_ERROR,
			MAP_ERROR, "expected '=' or another designator"),
	ERR_EXPECTED_EXPRESSION(err_expected_expression, CLASS_ERROR, MAP_ERROR,
			"expected expression"),
	ERR_EXPECTED_EXTERNAL_DECLARATION(err_expected_external_declaration, CLASS_ERROR,
			MAP_ERROR, "expected external declaration"),
	ERR_EXPECTED_FIELD_DESIGNATOR(err_expected_field_designator, CLASS_ERROR,
			MAP_ERROR, "expected a field designator, such as '.field = 4'"),
	ERR_EXPECTED_FN_BODY(err_expected_fn_body, CLASS_ERROR, MAP_ERROR,
			"expected function body after function declarator"),
	ERR_EXPECTED_GREATER(err_expected_greater, CLASS_ERROR, MAP_ERROR,
			"expected '>'"),
	ERR_EXPECTED_IDENT(err_expected_ident, CLASS_ERROR, MAP_ERROR,
			"expected identifier"),
	ERR_EXPECTED_IDENT_IN_USING(err_expected_ident_in_using, CLASS_ERROR, MAP_ERROR,
			"expected an identifier in using directive"),
	ERR_EXPECTED_IDENT_LBRACE(err_expected_ident_lbrace, CLASS_ERROR, MAP_ERROR,
			"expected identifier or '{'"),
	ERR_EXPECTED_IDENT_LPAREN(err_expected_ident_lparen, CLASS_ERROR, MAP_ERROR,
			"expected identifier or '('"),
	ERR_EXPECTED_LBRACE(err_expected_lbrace, CLASS_ERROR, MAP_ERROR, "expected '{'"),
	ERR_EXPECTED_LBRACE_IN_COMPOUND_LITERAL(
			err_expected_lbrace_in_compound_literal, CLASS_ERROR, MAP_ERROR,
			"expected '{' in compound literal"),
	ERR_EXPECTED_LBRACE_OR_COMMA(err_expected_lbrace_or_comma, CLASS_ERROR, MAP_ERROR,
			"expected '{' or ','"),
	ERR_EXPECTED_LESS_AFTER(err_expected_less_after, CLASS_ERROR, MAP_ERROR,
			"expected '<' after '%0'"),
	ERR_EXPECTED_LPAREN(err_expected_lparen, CLASS_ERROR, MAP_ERROR, "expected '('"),
	ERR_EXPECTED_LPAREN_AFTER(err_expected_lparen_after, CLASS_ERROR, MAP_ERROR,
			"expected '(' after '%0'"),
	ERR_EXPECTED_LPAREN_AFTER_ID(err_expected_lparen_after_id, CLASS_ERROR, MAP_ERROR,
			"expected '(' after %0"),
	ERR_EXPECTED_LPAREN_AFTER_TYPE(err_expected_lparen_after_type, CLASS_ERROR,
			MAP_ERROR,
			"expected '(' for function-style cast or type construction"),
	ERR_EXPECTED_MEMBER_NAME_OR_SEMI(err_expected_member_name_or_semi, CLASS_ERROR,
			MAP_ERROR,
			"expected member name or ';' after declaration specifiers"),
	ERR_EXPECTED_MEMBER_OR_BASE_NAME(err_expected_member_or_base_name, CLASS_ERROR,
			MAP_ERROR, "expected class member or base class name"),
	ERR_EXPECTED_METHOD_BODY(err_expected_method_body, CLASS_ERROR, MAP_ERROR,
			"expected method body"),
	ERR_EXPECTED_QUALIFIED_AFTER_TYPENAME(err_expected_qualified_after_typename,
			CLASS_ERROR, MAP_ERROR, "expected a qualified name after 'typename'"),
	ERR_EXPECTED_RBRACE(err_expected_rbrace, CLASS_ERROR, MAP_ERROR, "expected '}'"),
	ERR_EXPECTED_RPAREN(err_expected_rparen, CLASS_ERROR, MAP_ERROR, "expected ')'"),
	ERR_EXPECTED_RSQUARE(err_expected_rsquare, CLASS_ERROR, MAP_ERROR,
			"expected ']'"),
	ERR_EXPECTED_SELECTOR_FOR_METHOD(err_expected_selector_for_method, CLASS_ERROR,
			MAP_ERROR, "expected selector for Objective-C method"),
	ERR_EXPECTED_SEMI_AFTER(err_expected_semi_after, CLASS_ERROR, MAP_ERROR,
			"expected ';' after %0"),
	ERR_EXPECTED_SEMI_AFTER_ATTRIBUTE_LIST(
			err_expected_semi_after_attribute_list, CLASS_ERROR, MAP_ERROR,
			"expected ';' after attribute list"),
	ERR_EXPECTED_SEMI_AFTER_EXPR(err_expected_semi_after_expr, CLASS_ERROR, MAP_ERROR,
			"expected ';' after expression"),
	ERR_EXPECTED_SEMI_AFTER_METHOD_PROTO(err_expected_semi_after_method_proto,
			CLASS_ERROR, MAP_ERROR, "expected ';' after method prototype"),
	ERR_EXPECTED_SEMI_AFTER_NAMESPACE_NAME(
			err_expected_semi_after_namespace_name, CLASS_ERROR, MAP_ERROR,
			"expected ';' after namespace name"),
	ERR_EXPECTED_SEMI_AFTER_STATIC_ASSERT(err_expected_semi_after_static_assert,
			CLASS_ERROR, MAP_ERROR, "expected ';' after static_assert"),
	ERR_EXPECTED_SEMI_AFTER_STMT(err_expected_semi_after_stmt, CLASS_ERROR, MAP_ERROR,
			"expected ';' after %0 statement"),
	ERR_EXPECTED_SEMI_DECL_LIST(err_expected_semi_decl_list, CLASS_ERROR, MAP_ERROR,
			"expected ';' at end of declaration list"),
	ERR_EXPECTED_SEMI_DECLARATION(err_expected_semi_declaration, CLASS_ERROR,
			MAP_ERROR, "expected ';' at end of declaration"),
	ERR_EXPECTED_SEMI_FOR(err_expected_semi_for, CLASS_ERROR, MAP_ERROR,
			"expected ';' in 'for' statement specifier"),
	ERR_EXPECTED_STATEMENT(err_expected_statement, CLASS_ERROR, MAP_ERROR,
			"expected statement"),
	ERR_EXPECTED_STRING_LITERAL(err_expected_string_literal, CLASS_ERROR, MAP_ERROR,
			"expected string literal"),
	ERR_EXPECTED_TEMPLATE(err_expected_template, CLASS_ERROR, MAP_ERROR,
			"expected template"),
	ERR_EXPECTED_TYPE(err_expected_type, CLASS_ERROR, MAP_ERROR, "expected a type"),
	ERR_EXPECTED_TYPE_ID_AFTER(err_expected_type_id_after, CLASS_ERROR, MAP_ERROR,
			"expected type-id after '%0'"),
	ERR_EXPECTED_TYPE_NAME_AFTER_TYPENAME(err_expected_type_name_after_typename,
			CLASS_ERROR, MAP_ERROR,
			"expected an identifier or template-id after '::'"),
	ERR_EXPECTED_UNQUALIFIED_ID(err_expected_unqualified_id, CLASS_ERROR, MAP_ERROR,
			"expected unqualified-id"),
	ERR_EXPECTED_WHILE(err_expected_while, CLASS_ERROR, MAP_ERROR,
			"expected 'while' in do/while loop"),
	ERR_EXPLICIT_INSTANTIATION_WITH_DEFINITION(
			err_explicit_instantiation_with_definition, CLASS_ERROR, MAP_ERROR,
			"explicit template instantiation cannot have a definition; if this definition is meant to be an explicit specialization, add '<>' after the 'template' keyword"),
	ERR_FRIEND_INVALID_IN_CONTEXT(err_friend_invalid_in_context, CLASS_ERROR,
			MAP_ERROR, "'friend' used outside of class"),
	ERR_FRIEND_STORAGE_SPEC(err_friend_storage_spec, CLASS_ERROR, MAP_ERROR,
			"'%0' is invalid in friend declarations"),
	ERR_FUNC_DEF_NO_PARAMS(err_func_def_no_params, CLASS_ERROR, MAP_ERROR,
			"function definition does not declare parameters"),
	ERR_FUNCTION_DECLARED_TYPEDEF(err_function_declared_typedef, CLASS_ERROR,
			MAP_ERROR, "function definition declared 'typedef'"),
	ERR_ID_AFTER_TEMPLATE_IN_NESTED_NAME_SPEC(
			err_id_after_template_in_nested_name_spec, CLASS_ERROR, MAP_ERROR,
			"expected template name after 'template' keyword in nested name specifier"),
	ERR_ID_AFTER_TEMPLATE_IN_TYPENAME_SPEC(
			err_id_after_template_in_typename_spec, CLASS_ERROR, MAP_ERROR,
			"expected template name after 'template' keyword in typename specifier"),
	ERR_ILLEGAL_DECL_REFERENCE_TO_REFERENCE(
			err_illegal_decl_reference_to_reference, CLASS_ERROR, MAP_ERROR,
			"%0 declared as a reference to a reference"),
	ERR_INVALID_COMPLEX_SPEC(err_invalid_complex_spec, CLASS_ERROR, MAP_ERROR,
			"'_Complex %0' is invalid"),
	ERR_INVALID_DECL_SPEC_COMBINATION(err_invalid_decl_spec_combination, CLASS_ERROR,
			MAP_ERROR,
			"cannot combine with previous '%0' declaration specifier"),
	ERR_INVALID_LONG_SPEC(err_invalid_long_spec, CLASS_ERROR, MAP_ERROR,
			"'long %0' is invalid"),
	ERR_INVALID_LONGLONG_SPEC(err_invalid_longlong_spec, CLASS_ERROR, MAP_ERROR,
			"'long long %0' is invalid"),
	ERR_INVALID_REFERENCE_QUALIFIER_APPLICATION(
			err_invalid_reference_qualifier_application, CLASS_ERROR, MAP_ERROR,
			"'%0' qualifier may not be applied to a reference"),
	ERR_INVALID_SHORT_SPEC(err_invalid_short_spec, CLASS_ERROR, MAP_ERROR,
			"'short %0' is invalid"),
	ERR_INVALID_SIGN_SPEC(err_invalid_sign_spec, CLASS_ERROR, MAP_ERROR,
			"'%0' cannot be signed or unsigned"),
	ERR_INVALID_TOKEN_AFTER_TOPLEVEL_DECLARATOR(
			err_invalid_token_after_toplevel_declarator, CLASS_ERROR, MAP_ERROR,
			"invalid token after top level declarator"),
	ERR_LABEL_END_OF_COMPOUND_STATEMENT(err_label_end_of_compound_statement,
			CLASS_ERROR, MAP_ERROR,
			"label at end of compound statement: expected statement"),
	ERR_LESS_AFTER_TEMPLATE_NAME_IN_NESTED_NAME_SPEC(
			err_less_after_template_name_in_nested_name_spec, CLASS_ERROR, MAP_ERROR,
			"expected '<' after 'template %0' in nested name specifier"),
	ERR_MISSING_CATCH_FINALLY(err_missing_catch_finally, CLASS_ERROR, MAP_ERROR,
			"@try statement without a @catch and @finally clause"),
	ERR_MISSING_CLASS_DEFINITION(err_missing_class_definition, CLASS_ERROR, MAP_ERROR,
			"cannot find definition of 'Class'"),
	ERR_MISSING_ID_DEFINITION(err_missing_id_definition, CLASS_ERROR, MAP_ERROR,
			"cannot find definition of 'id'"),
	ERR_MISSING_PARAM(err_missing_param, CLASS_ERROR, MAP_ERROR,
			"expected parameter declarator"),
	ERR_MISSING_PROTO_DEFINITION(err_missing_proto_definition, CLASS_ERROR, MAP_ERROR,
			"cannot find definition of 'Protocol'"),
	ERR_MISSING_SEL_DEFINITION(err_missing_sel_definition, CLASS_ERROR, MAP_ERROR,
			"cannot find definition of 'SEL'"),
	ERR_MULTIPLE_TEMPLATE_DECLARATORS(err_multiple_template_declarators, CLASS_ERROR,
			MAP_ERROR,
			"%select{|a template declaration|an explicit template specialization|an explicit template instantiation}0 can only %select{|declare|declare|instantiate}0 a single entity"),
	ERR_NO_MATCHING_PARAM(err_no_matching_param, CLASS_ERROR, MAP_ERROR,
			"parameter named %0 is missing"),
	ERR_OBJC_CONCAT_STRING(err_objc_concat_string, CLASS_ERROR, MAP_ERROR,
			"unexpected token after Objective-C string"),
	ERR_OBJC_DIRECTIVE_ONLY_IN_PROTOCOL(err_objc_directive_only_in_protocol,
			CLASS_ERROR, MAP_ERROR,
			"directive may only be specified in protocols only"),
	ERR_OBJC_EXPECTED_EQUAL(err_objc_expected_equal, CLASS_ERROR, MAP_ERROR,
			"setter/getter expects '=' followed by name"),
	ERR_OBJC_EXPECTED_PROPERTY_ATTR(err_objc_expected_property_attr, CLASS_ERROR,
			MAP_ERROR, "unknown property attribute %0"),
	ERR_OBJC_ILLEGAL_INTERFACE_QUAL(err_objc_illegal_interface_qual, CLASS_ERROR,
			MAP_ERROR, "illegal interface qualifier"),
	ERR_OBJC_ILLEGAL_VISIBILITY_SPEC(err_objc_illegal_visibility_spec, CLASS_ERROR,
			MAP_ERROR, "illegal visibility specification"),
	ERR_OBJC_MISSING_END(err_objc_missing_end, CLASS_ERROR, MAP_ERROR,
			"missing @end"),
	ERR_OBJC_NO_ATTRIBUTES_ON_CATEGORY(err_objc_no_attributes_on_category,
			CLASS_ERROR, MAP_ERROR, "attributes may not be specified on a category"),
	ERR_OBJC_PROPERTOES_REQUIRE_OBJC2(err_objc_propertoes_require_objc2, CLASS_ERROR,
			MAP_ERROR, "properties are an Objective-C 2 feature"),
	ERR_OBJC_PROPERTY_BITFIELD(err_objc_property_bitfield, CLASS_ERROR, MAP_ERROR,
			"property name cannot be a bitfield"),
	ERR_OBJC_PROPERTY_REQUIRES_FIELD_NAME(err_objc_property_requires_field_name,
			CLASS_ERROR, MAP_ERROR, "property requires fields to be named"),
	ERR_OBJC_UNEXPECTED_ATTR(err_objc_unexpected_attr, CLASS_ERROR, MAP_ERROR,
			"prefix attribute must be followed by an interface or protocol"),
	ERR_OPERATOR_MISSING_TYPE_SPECIFIER(err_operator_missing_type_specifier,
			CLASS_ERROR, MAP_ERROR, "missing type specifier after 'operator'"),
	ERR_PARSE_ERROR(err_parse_error, CLASS_ERROR, MAP_ERROR, "parse error"),
	ERR_RVALUE_REFERENCE(err_rvalue_reference, CLASS_ERROR, MAP_ERROR,
			"rvalue references are only allowed in C++0x"),
	ERR_TEMPLATE_SPEC_SYNTAX_NON_TEMPLATE(err_template_spec_syntax_non_template,
			CLASS_ERROR, MAP_ERROR,
			"identifier followed by '<' indicates a class template specialization but %0 %select{does not refer to a template|refers to a function template|<unused>|refers to a template template parameter}1"),
	ERR_TWO_RIGHT_ANGLE_BRACKETS_NEED_SPACE(
			err_two_right_angle_brackets_need_space, CLASS_ERROR, MAP_ERROR,
			"a space is required between consecutive right angle brackets (use '> >')"),
	ERR_TYPENAME_INVALID_FUNCTIONSPEC(err_typename_invalid_functionspec, CLASS_ERROR,
			MAP_ERROR,
			"type name does not allow function specifier to be specified"),
	ERR_TYPENAME_INVALID_STORAGECLASS(err_typename_invalid_storageclass, CLASS_ERROR,
			MAP_ERROR,
			"type name does not allow storage class to be specified"),
	ERR_TYPENAME_REFERS_TO_NON_TYPE_TEMPLATE(
			err_typename_refers_to_non_type_template, CLASS_ERROR, MAP_ERROR,
			"typename specifier refers to a non-template"),
	ERR_TYPENAME_REQUIRES_SPECQUAL(err_typename_requires_specqual, CLASS_ERROR,
			MAP_ERROR, "type name requires a specifier or qualifier"),
	ERR_UNEXPECTED_AT(err_unexpected_at, CLASS_ERROR, MAP_ERROR,
			"unexpected '@' in program"),
	ERR_UNEXPECTED_NAMESPACE_ATTRIBUTES_ALIAS(
			err_unexpected_namespace_attributes_alias, CLASS_ERROR, MAP_ERROR,
			"attributes can not be specified on namespace alias"),
	ERR_UNEXPECTED_TEMPLATE_SPEC_IN_USING(err_unexpected_template_spec_in_using,
			CLASS_ERROR, MAP_ERROR,
			"use of template specialization in using directive not allowed"),
	ERR_UNEXPECTED_TYPEDEF_IDENT(err_unexpected_typedef_ident, CLASS_ERROR, MAP_ERROR,
			"unexpected type name %0: expected identifier"),
	ERR_UNKNOWN_TYPENAME(err_unknown_typename, CLASS_ERROR, MAP_ERROR,
			"unknown type name %0"),
	ERR_UNSPECIFIED_VLA_SIZE_WITH_STATIC(err_unspecified_vla_size_with_static,
			CLASS_ERROR, MAP_ERROR,
			"'static' may not be used with an unspecified variable length array size"),
	ERR_USE_OF_TAG_NAME_WITHOUT_TAG(err_use_of_tag_name_without_tag, CLASS_ERROR,
			MAP_ERROR, "use of tagged type %0 without '%1' stmtClass"),
	ERR_USING_NAMESPACE_IN_CLASS(err_using_namespace_in_class, CLASS_ERROR, MAP_ERROR,
			"'using namespace' in class not allowed"),
	ERR_VARIADIC_TEMPLATES(err_variadic_templates, CLASS_ERROR, MAP_ERROR,
			"variadic templates are only allowed in C++0x"),
	ERROR_PROPERTY_IVAR_DECL(error_property_ivar_decl, CLASS_ERROR, MAP_ERROR,
			"property synthesize requires specification of an ivar"),
	EXT_C99_COMPOUND_LITERAL(ext_c99_compound_literal, CLASS_NOTE, MAP_IGNORE,
			"compound literals are a C99-specific feature"),
	EXT_C99_VARIABLE_DECL_IN_FOR_LOOP(ext_c99_variable_decl_in_for_loop, CLASS_NOTE,
			MAP_IGNORE,
			"variable declaration in for loop is a C99-specific feature"),
	EXT_DUPLICATE_DECLSPEC(ext_duplicate_declspec, CLASS_NOTE, MAP_IGNORE,
			"duplicate '%0' declaration specifier"),
	EXT_ELLIPSIS_EXCEPTION_SPEC(ext_ellipsis_exception_spec, CLASS_NOTE, MAP_IGNORE,
			"exception specification of '...' is a Microsoft extension"),
	EXT_EMPTY_SOURCE_FILE(ext_empty_source_file, CLASS_NOTE, MAP_IGNORE,
			"ISO C forbids an empty source file"),
	EXT_EMPTY_STRUCT_UNION_ENUM(ext_empty_struct_union_enum, CLASS_NOTE, MAP_IGNORE,
			"use of empty %0 extension"),
	EXT_ENUMERATOR_LIST_COMMA(ext_enumerator_list_comma, CLASS_NOTE, MAP_IGNORE,
			"commas at the end of enumerator lists are a %select{C99|C++0x}0-specific feature"),
	EXT_EXPECTED_SEMI_DECL_LIST(ext_expected_semi_decl_list, CLASS_NOTE, MAP_IGNORE,
			"expected ';' at end of declaration list"),
	EXT_EXTRA_STRUCT_SEMI(ext_extra_struct_semi, CLASS_NOTE, MAP_IGNORE,
			"extra ';' inside a struct or union"),
	EXT_GNU_ADDRESS_OF_LABEL(ext_gnu_address_of_label, CLASS_NOTE, MAP_IGNORE,
			"use of GNU address-of-label extension"),
	EXT_GNU_ARRAY_RANGE(ext_gnu_array_range, CLASS_NOTE, MAP_IGNORE,
			"use of GNU array range extension"),
	EXT_GNU_CASE_RANGE(ext_gnu_case_range, CLASS_NOTE, MAP_IGNORE,
			"use of GNU case range extension"),
	EXT_GNU_CONDITIONAL_EXPR(ext_gnu_conditional_expr, CLASS_NOTE, MAP_IGNORE,
			"use of GNU ?: expression extension, eliding middle term"),
	EXT_GNU_EMPTY_INITIALIZER(ext_gnu_empty_initializer, CLASS_NOTE, MAP_IGNORE,
			"use of GNU empty initializer extension"),
	EXT_GNU_INDIRECT_GOTO(ext_gnu_indirect_goto, CLASS_NOTE, MAP_IGNORE,
			"use of GNU indirect-goto extension"),
	EXT_GNU_MISSING_EQUAL_DESIGNATOR(ext_gnu_missing_equal_designator, CLASS_NOTE,
			MAP_WARNING, "use of GNU 'missing =' extension in designator"),
	EXT_GNU_OLD_STYLE_FIELD_DESIGNATOR(ext_gnu_old_style_field_designator, CLASS_NOTE,
			MAP_WARNING, "use of GNU old-style field designator extension"),
	EXT_GNU_STATEMENT_EXPR(ext_gnu_statement_expr, CLASS_NOTE, MAP_IGNORE,
			"use of GNU statement expression extension"),
	EXT_IDENT_LIST_IN_PARAM(ext_ident_list_in_param, CLASS_NOTE, MAP_IGNORE,
			"type-less parameter names in function declaration"),
	EXT_INTEGER_COMPLEX(ext_integer_complex, CLASS_NOTE, MAP_IGNORE,
			"complex integer types are an extension"),
	EXT_PLAIN_COMPLEX(ext_plain_complex, CLASS_NOTE, MAP_WARNING,
			"plain '_Complex' requires a type specifier; assuming '_Complex double'"),
	EXT_THREAD_BEFORE(ext_thread_before, CLASS_NOTE, MAP_IGNORE,
			"'__thread' before 'static'"),
	EXT_TOP_LEVEL_SEMI(ext_top_level_semi, CLASS_NOTE, MAP_IGNORE,
			"extra ';' outside of a function"),
	W_ASM_QUALIFIER_IGNORED(w_asm_qualifier_ignored, CLASS_WARNING, MAP_WARNING,
			"ignored %0 qualifier on asm"),
	WARN_CXX0X_RIGHT_SHIFT_IN_TEMPLATE_ARG(
			warn_cxx0x_right_shift_in_template_arg, CLASS_WARNING, MAP_WARNING,
			"use of right-shift operator ('>>') in template argument will require parentheses in C++0x"),
	WARN_EXPECTED_IMPLEMENTATION(warn_expected_implementation, CLASS_WARNING,
			MAP_WARNING, "@end must appear in an @implementation context"),
	WARN_OBJC_PROTOCOL_QUALIFIER_MISSING_ID(
			warn_objc_protocol_qualifier_missing_id, CLASS_WARNING, MAP_WARNING,
			"protocol qualifiers without 'id' is archaic"),
	WARN_PARENS_DISAMBIGUATED_AS_FUNCTION_DECL(
			warn_parens_disambiguated_as_function_decl, CLASS_WARNING, MAP_WARNING,
			"parentheses were disambiguated as a function declarator"),
	WARN_PRAGMA_EXPECTED_IDENTIFIER(warn_pragma_expected_identifier, CLASS_WARNING,
			MAP_WARNING, "expected identifier in '#pragma %0' - ignored"),
	WARN_PRAGMA_EXPECTED_LPAREN(warn_pragma_expected_lparen, CLASS_WARNING,
			MAP_WARNING, "missing '(' after '#pragma %0' - ignoring"),
	WARN_PRAGMA_EXPECTED_RPAREN(warn_pragma_expected_rparen, CLASS_WARNING,
			MAP_WARNING, "missing ')' after '#pragma %0' - ignoring"),
	WARN_PRAGMA_EXTRA_TOKENS_AT_EOL(warn_pragma_extra_tokens_at_eol, CLASS_WARNING,
			MAP_WARNING, "extra tokens at end of '#pragma %0' - ignored"),
	WARN_PRAGMA_PACK_INVALID_ACTION(warn_pragma_pack_invalid_action, CLASS_WARNING,
			MAP_WARNING, "unknown action for '#pragma pack' - ignored"),
	WARN_PRAGMA_PACK_INVALID_CONSTANT(warn_pragma_pack_invalid_constant,
			CLASS_WARNING, MAP_WARNING,
			"invalid constant for '#pragma pack', expected %0 - ignored"),
	WARN_PRAGMA_PACK_MALFORMED(warn_pragma_pack_malformed, CLASS_WARNING, MAP_WARNING,
			"expected integer or identifier in '#pragma pack' - ignored"),
	WARN_PRAGMA_UNUSED_EXPECTED_PUNC(warn_pragma_unused_expected_punc, CLASS_WARNING,
			MAP_WARNING, "expected ')' or ',' in '#pragma unused'"),
	WARN_PRAGMA_UNUSED_EXPECTED_VAR(warn_pragma_unused_expected_var, CLASS_WARNING,
			MAP_WARNING,
			"expected '#pragma unused' argument to be a variable name");

	public int diagID;
	public Diagnostic.DiagnosticClass diagClass;
	public Diagnostic.Mapping diagMapping;
	public boolean sfinae;
    public String optionGroup;
	public String text;

	DiagnosticParseKinds(int diagID,
			Diagnostic.DiagnosticClass diagClass,
			Diagnostic.Mapping diagMapping, 
			String text)
	{
		this.diagID = diagID;
		this.diagClass = diagClass;
		this.diagMapping = diagMapping;
		this.text = text;
	}

}
