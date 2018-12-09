package jlang.clex;

/**
 * An interface that defines codes for C language source tokens
 * returned from lexical analysis.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public interface Tag {
  // Not a token.
  int UNKNOWN = 0;
  // End of file.
  int EOF = UNKNOWN + 1;
  // End of preprocessing directive (end of line inside a
  // directive).
  int EOD = EOF + 1;

  // The end of macro
  int EOM = EOD + 1;

  // C99 6.4.9: Comments.
  int COMMENT = EOM + 1;  // Comment (only in -E -C[C] mode)

  // C99 6.4.2: Identifiers.
  int IDENTIFIER = COMMENT + 1;   // abcde123

  // C99 6.4.4.1: Integer Constants
  // C99 6.4.4.2: Floating Constants
  int NUMERIC_CONSTANT = IDENTIFIER + 1; // 0x123

  // C99 6.4.4: Character Constants
  int CHAR_CONSTANT = NUMERIC_CONSTANT + 1;

  // C99 6.4.5: String Literals.
  int STRING_LITERAL = CHAR_CONSTANT + 1; // "foo"

  int ANGLE_STRING_LITERAL = STRING_LITERAL + 1; // <foo>

  // specifier
  int VOID = STRING_LITERAL + 1;
  int BOOL = VOID + 1;
  int CHAR = BOOL + 1;
  int SHORT = CHAR + 1;
  int INT = SHORT + 1;
  int LONG = INT + 1;
  int FLOAT = LONG + 1;
  int DOUBLE = FLOAT + 1;
  int FALSE = DOUBLE + 1;
  int UNSIGNED = FALSE + 1;
  int SIGNED = UNSIGNED + 1;
  int STRUCT = SIGNED + 1;
  int TRUE = STRUCT + 1;
  int UNION = TRUE + 1;
  int ENUM = UNION + 1;
  int COMPLEX = ENUM + 1;
  int IMAGINARY = COMPLEX + 1;

  // storage class specifier
  int STATIC = IMAGINARY + 1;
  int REGISTER = STATIC + 1;
  int EXTERN = REGISTER + 1;
  int TYPEDEF = EXTERN + 1;
  int AUTO = TYPEDEF + 1;

  // qualifier
  int CONST = AUTO + 1;
  int VOLATILE = CONST + 1;
  int RESTRICT = VOLATILE + 1;

  // function-specifier
  int INLINE = RESTRICT + 1;

  // statement
  int BREAK = INLINE + 1;
  int CASE = BREAK + 1;
  int CONTINUE = CASE + 1;
  int DEFAULT = CONTINUE + 1;
  int DO = DEFAULT + 1;
  int ELSE = DO + 1;
  int FOR = ELSE + 1;
  int GOTO = FOR + 1;
  int IF = GOTO + 1;
  int RETURN = IF + 1;
  int SIZEOF = RETURN + 1;
  int SWITCH = SIZEOF + 1;
  int WHILE = SWITCH + 1;


  // C99 6.4.6: Punctuators.
  int LPAREN = WHILE + 1;
  int RPAREN = LPAREN + 1;
  int LBRACE = RPAREN + 1;
  int RBRACE = LBRACE + 1;
  int LBRACKET = RBRACE + 1;
  int RBRACKET = LBRACKET + 1;
  int SEMI = RBRACKET + 1;
  int COMMA = SEMI + 1;

  int DOT = COMMA + 1;
  int EQ = DOT + 1;
  int GT = EQ + 1;
  int LT = GT + 1;
  int BANG = LT + 1;          // '!'
  int TILDE = BANG + 1;       // '~'
  int QUES = TILDE + 1;       // '?'
  int COLON = QUES + 1;       // ':'
  int EQEQ = COLON + 1;
  int LTEQ = EQEQ + 1;
  int GTEQ = LTEQ + 1;
  int BANGEQ = GTEQ + 1;      // '!='
  int AMPAMP = BANGEQ + 1;    // '&&'
  int BARBAR = AMPAMP + 1;    // '||'
  int PLUSPLUS = BARBAR + 1;
  int SUBSUB = PLUSPLUS + 1;
  int PLUS = SUBSUB + 1;
  int SUB = PLUS + 1;
  int SUBGT = SUB + 1;
  int STAR = SUBGT + 1;        // '*'
  int SLASH = STAR + 1;       // '/'
  int AMP = SLASH + 1;        // '&'
  int BAR = AMP + 1;          // '|'
  int CARET = BAR + 1;        // '^'
  int PERCENT = CARET + 1;    // '%'
  int LTLT = PERCENT + 1;
  int GTGT = LTLT + 1;
  int PLUSEQ = GTGT + 1;
  int SUBEQ = PLUSEQ + 1;
  int STAREQ = SUBEQ + 1;
  int SLASHEQ = STAREQ + 1;
  int AMPEQ = SLASHEQ + 1;
  int BAREQ = AMPEQ + 1;
  int CARETEQ = BAREQ + 1;
  int PERCENTEQ = CARETEQ + 1;
  int LTLTEQ = PERCENTEQ + 1;
  int GTGTEQ = LTLTEQ + 1;

  // for variable argument
  int ELLIPSIS = GTGTEQ + 1;

  int HASH = ELLIPSIS + 1;
  int HASHHASH = HASH + 1;

  // GNU Extensions (inside impl-reserved namespace)
  int ASM = HASHHASH + 1;
  int _DECIMAL32 = ASM + 1;
  int _DECIMAL64 = _DECIMAL32 + 1;
  int _DECIMAL128 = _DECIMAL64 + 1;
  int __ALIGNOF = _DECIMAL128 + 1;
  int __ATTRIBUTE = __ALIGNOF + 1;
  int __BUILTIN_CHOOSE_EXPR = __ATTRIBUTE + 1;
  int __BUILTIN_OFFSETOF = __BUILTIN_CHOOSE_EXPR + 1;
  int __BUILTIN_TYPES_COMPATIBLE_P = __BUILTIN_OFFSETOF + 1;
  int __BUILTIN_VA_ARG = __BUILTIN_TYPES_COMPATIBLE_P + 1;
  int __EXTENSION__ = __BUILTIN_VA_ARG + 1;
  int __IMAG = __EXTENSION__ + 1;
  int __LABEL__ = __IMAG + 1;
  int __REAL__ = __LABEL__ + 1;
  int __THREAD = __REAL__ + 1;
  int __FUNCTION__ = __THREAD + 1;
  int __PREITY_FUNCTION = __FUNCTION__ + 1;

  // GNU Extensions (outside impl-reserved namespace)
  int TYPEOF = __PREITY_FUNCTION + 1;
}

