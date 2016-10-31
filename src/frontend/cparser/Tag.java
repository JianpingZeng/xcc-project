package frontend.cparser;

/**
 * An interface that defines codes for C language source tokens
 * returned from lexical analysis.
 *  
 * @author Xlous.zeng  
 * @version 0.1
 */
public interface Tag {
    int EOF = 0;
    int ERROR = EOF + 1;
    int IDENTIFIER = ERROR + 1;
 
    // frontend.type specifier
    int VOID = IDENTIFIER + 1;
    int BOOL = VOID + 1;
    int CHAR = BOOL + 1;
    int SHORT = CHAR + 1;
    int INT = SHORT + 1;
    int LONG = INT + 1;
    int LONGLONG = LONG + 1;
    int FLOAT = LONGLONG + 1;
    int DOUBLE = FLOAT + 1;
    int UNSIGNED = DOUBLE + 1;
    int SIGNED = UNSIGNED + 1;
    int STRUCT = SIGNED + 1;
    int UNION = STRUCT + 1;
    int ENUM = UNION + 1;
    int COMPLEX= ENUM + 1;
    int ANN_TYPENAME = COMPLEX + 1;

    // storage class specifier
    int STATIC = ANN_TYPENAME + 1;
    int REGISTER = STATIC + 1;
    int EXTERN = REGISTER + 1;
    int TYPEDEF = EXTERN + 1;
    int AUTO = TYPEDEF + 1;
    
    // frontend.type qualifier
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
    
    // literal
    int INTLITERAL = WHILE + 1;
    int LONGLITERAL = INTLITERAL + 1;
    int FLOATLITERAL = LONGLITERAL + 1;
    int DOUBLELITERAL = FLOATLITERAL + 1;
    int CHARLITERAL = DOUBLELITERAL + 1;
    int STRINGLITERAL = CHARLITERAL + 1;

    
    // operator and seperator
    int LPAREN = STRINGLITERAL + 1;
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
    int BANG = LT + 1;
    int TILDE = BANG + 1;
    int QUES = TILDE + 1;
    int COLON = QUES + 1;
    int EQEQ = COLON + 1;
    int LTEQ = EQEQ + 1;
    int GTEQ = LTEQ + 1;
    int BANGEQ = GTEQ + 1;
    int AMPAMP = BANGEQ + 1;
    int BARBAR = AMPAMP + 1;
    int PLUSPLUS = BARBAR + 1;
    int SUBSUB = PLUSPLUS + 1;
    int PLUS = SUBSUB + 1;
    int SUB = PLUS + 1;
    int SUBGT = SUB + 1;
    int STAR = SUBGT+ 1;
    int SLASH = STAR + 1;
    int AMP = SLASH + 1;
    int BAR = AMP + 1;
    int CARET = BAR + 1;
    int PERCENT = CARET + 1;
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
    
    int TokenCount = ELLIPSIS + 1;
}

