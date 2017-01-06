package jlang.cparser;

import jlang.cparser.Token.Keyword;

import java.util.HashMap;
import java.util.Map.Entry;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class Keywords implements Tag
{

    private static Keywords instance = null;

    private static HashMap<String, Keyword> keywordTables = new HashMap<>();

    /**
     * A instance method for singleton pattern.
     *
     * @return
     */
    public static Keywords instance()
    {
        if (instance == null)
        {
            instance = new Keywords();
        }
        return instance;
    }

    private Keywords()
    {
        enterKeyword("void", VOID);
        enterKeyword("char", CHAR);
        enterKeyword("_Bool", BOOL);
        enterKeyword("short", SHORT);
        enterKeyword("int", INT);
        enterKeyword("long", LONG);
        enterKeyword("float", FLOAT);
        enterKeyword("double", DOUBLE);
        enterKeyword("unsigned", UNSIGNED);
        enterKeyword("unsigned", UNSIGNED);
        enterKeyword("struct", STRUCT);
        enterKeyword("union", UNION);
        enterKeyword("enum", ENUM);
        enterKeyword("_Complex", COMPLEX);

        enterKeyword("static", STATIC);
        enterKeyword("register", REGISTER);
        enterKeyword("extern", EXTERN);
        enterKeyword("typedef", TYPEDEF);

        enterKeyword("const", CONST);
        enterKeyword("volatile", VOLATILE);
        enterKeyword("restrict", RESTRICT);

        enterKeyword("inline", INLINE);

        enterKeyword("break", BREAK);
        enterKeyword("case", CASE);
        enterKeyword("continue", CONTINUE);
        enterKeyword("default", DEFAULT);
        enterKeyword("do", DO);
        enterKeyword("else", ELSE);
        enterKeyword("for", FOR);
        enterKeyword("goto", GOTO);
        enterKeyword("if", IF);
        enterKeyword("return", RETURN);
        enterKeyword("sizeof", SIZEOF);
        enterKeyword("switch", SWITCH);
        enterKeyword("while", WHILE);

        enterKeyword("(", LPAREN);
        enterKeyword(")", RPAREN);
        enterKeyword("{", LBRACE);
        enterKeyword("}", RBRACE);
        enterKeyword("[", LBRACKET);
        enterKeyword("]", RBRACKET);
        enterKeyword(";", SEMI);
        enterKeyword(",", COMMA);
        enterKeyword(".", DOT);

        enterKeyword("+", PLUS);
        enterKeyword("-", SUB);
        enterKeyword("!", BANG);
        enterKeyword("%", PERCENT);
        enterKeyword("^", CARET);
        enterKeyword("&", AMP);
        enterKeyword("*", STAR);
        enterKeyword("|", BAR);
        enterKeyword("~", TILDE);
        enterKeyword("/", SLASH);
        enterKeyword(">", GT);
        enterKeyword("<", LT);
        enterKeyword("?", QUES);
        enterKeyword(":", COLON);
        enterKeyword("=", EQ);
        enterKeyword("++", PLUSPLUS);
        enterKeyword("--", SUBSUB);
        enterKeyword("==", EQEQ);
        enterKeyword("<=", LTEQ);
        enterKeyword(">=", GTEQ);
        enterKeyword("!=", BANGEQ);
        enterKeyword("<<", LTLT);
        enterKeyword(">>", GTGT);
        enterKeyword("+=", PLUSEQ);
        enterKeyword("-=", SUBEQ);
        enterKeyword("->", SUBGT);
        enterKeyword("*=", STAREQ);
        enterKeyword("/=", SLASHEQ);
        enterKeyword("&=", AMPEQ);
        enterKeyword("|=", BAREQ);
        enterKeyword("^=", CARETEQ);
        enterKeyword("%=", PERCENTEQ);
        enterKeyword("<<=", LTLTEQ);
        enterKeyword(">>=", GTGTEQ);
        enterKeyword("||", BARBAR);
        enterKeyword("&&", AMPAMP);

        enterKeyword("...", ELLIPSIS);
    }

    public boolean isKeyword(String s)
    {
        return keywordTables.get(s) != null;
    }

    public boolean isKeyword(Token t)
    {
        return t instanceof Keyword;
    }

    public Token getByName(String name)
    {
        return keywordTables.get(name);
    }

    private void enterKeyword(String s, int token)
    {
        if (Keywords.keywordTables.get(s) == null)
            keywordTables.put(s, new Keyword(token));
    }

    public String token2string(int token)
    {
        String res = "";
        for (Entry<String, Keyword> entity : keywordTables.entrySet())
        {
            if (entity.getValue().tag == token)
            {
                res = entity.getKey();
                break;
            }
        }
        return res;
    }
}
