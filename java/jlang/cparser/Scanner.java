package jlang.cparser;

import jlang.cpp.Tag;
import jlang.cpp.Token;
import jlang.cpp.Token.IntLiteral.IntLiteralKinds;
import jlang.cpp.Preprocessor;
import jlang.basic.SourceLocation;
import jlang.diag.DiagnosticLexKindsTag;
import jlang.diag.Diagnostic;
import jlang.diag.FullSourceLoc;
import tools.LayoutCharacters;

import java.util.LinkedList;

/**
 * The lexical analyzer maps an input stream consisting of ASCII characters into
 * a token sequence.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public class Scanner implements Tag, LayoutCharacters, DiagnosticLexKindsTag
{
    /**
     * The token, set by lex().
     */
    Token token;

    /**
     * The keyword table.
     */
    private final Keywords keywords;

    /**
     * specify the line number of this token
     */
    private int line;
    /**
     * specify the column number of this token
     */
    private int col;
    /**
     * The cached character read from input stream.
     */
    private char buf[];
    /**
     * the number of character in {@#linkplain buf} available currently.
     */
    private int buflen = 0;

    private int bp;

    private Diagnostic diags;

    private Preprocessor pp;

	/**
     * Those variable keep tracks of the look ahead tokens.
     */
    private final int Token_Ahead_Number = 2;
    private LinkedList<Token> tokenAheads = new LinkedList<>();

    public Scanner(Preprocessor pp)
    {
        this.keywords = Keywords.instance();
        diags = pp.getDiagnostics();
        this.pp = pp;
        int bufsize = 10000;
        buf = new char[bufsize];
        try (CPPReader reader = new CPPReader(pp))
        {
            while (true)
            {
                int nread = reader.read(buf, buflen, buf.length - buflen);
                if (nread < 0)
                    nread = 0;
                buflen = buflen + nread;
                if (buflen < buf.length)
                    break;
                char[] newbuf = new char[buflen << 1];
                System.arraycopy(buf, 0, newbuf, 0, buflen);
                buf = newbuf;
            }
        }
        catch (Exception e)
        {
            lexError(err_lexer_error).addTaggedVal(e.getMessage());
            buf = new char[1];
            buflen = 0;
        }
        buf[buflen] = EOI;
        line = 1;
        col = 0;
        bp = -1;
        scanChar();
        lex();
    }

    /**
     * The current character.
     */
    private char ch;

    /**
     * Skips the whitespace and reads next character.
     */
    private void scanChar()
    {
        bp++;
        ch = buf[bp];
        switch (ch)
        {
            case '\r':
                col = 0;
                line++;
                break;

            case '\n':
                if (bp == 0 || buf[bp - 1] != '\r')
                {
                    col = 0;
                    line++;
                }
                break;

            case '\t':
                col = (col / TabInc * TabInc) + TabInc;
                break;

            default:
                col++;
                break;
        }
    }

    /**
     * Retrieves the next token from token stream and consuming it.
     */
    void lex()
    {
        readAheadTokens();
        token = tokenAheads.removeFirst();
    }

	/**
     * This peeks the ahead one token and returns it without consuming it.
     * @return
     */
    Token nextToken()
    {
        readAheadTokens();
        return tokenAheads.getFirst();
    }

    private void readAheadTokens()
    {
        while (tokenAheads.size() < Token_Ahead_Number
                && tokenAheads.getLast().tag != EOF)
        {
            nextToken2();
        }
    }

    private void nextToken2()
    {
        sp = 0;
        while (true)
        {
            switch (ch)
            {
                case ' ':
                case '\t':
                case FF:
                case CR:
                case LF:
                    scanChar();
                    break;

                case ':':
                    makeIdent(":");
                    return;
                case '+':
                    scanChar();
                    if (ch == '+')
                    {
                        makeIdent("++");
                        return;
                    }
                    if (ch == '=')
                    {
                        makeIdent("+=");
                        return;
                    }
                    makeIdent("+");
                    return;
                case '*':
                    scanChar();
                    if (ch == '=')
                    {
                        makeIdent("*=");
                        return;
                    }
                    makeIdent(String.valueOf(ch));
                    return;
                case '=':
                    scanChar();
                    if (ch == '=')
                    {
                        makeIdent("==");
                        return;
                    }
                    makeIdent(String.valueOf(ch));
                    return;
                case '!':
                    scanChar();
                    if (ch == '=')
                    {
                        makeIdent("!=");
                        return;
                    }
                    makeIdent(String.valueOf(ch));
                    return;
                case '&':
                    scanChar();
                    if (ch == '=')
                    {
                        makeIdent("&=");
                        return;
                    }
                    if (ch == '&')
                    {
                        makeIdent("&&");
                        return;
                    }
                    makeIdent("&");
                    return;
                case '|':
                    scanChar();
                    if (ch == '=')
                    {
                        makeIdent("|=");
                        return;
                    }
                    if (ch == '|')
                    {
                        makeIdent("||");
                        return;
                    }
                    makeIdent("|");
                    return;
                case '^':
                    scanChar();
                    if (ch == '=')
                    {
                        makeIdent("^=");
                        return;
                    }
                    makeIdent("^");
                    return;
                case '\"':
                    readString();
                    return;
                case '\'':
                    scanChar();
                    if (ch == '\'')
                        lexError(err_empty_character);
                    else
                    {
                        if (ch == CR || ch == LF)
                            lexError(backslash_newline_space);
                        readChar();
                    }
                    return;
                case '/':
                    scanChar();
                    if (ch == '=')
                    {
                        makeIdent("/=");
                        return;
                    }
                    // for block comment
                    if (ch == '*')
                    {
                        skipComment();
                        break;
                    }
                    // for single line comment
                    if (ch == '/')
                    {
                        do
                        {
                            scanChar();
                        } while (ch != CR && ch != LF && bp < buflen);
                        break;
                    }
                    makeIdent("/");
                    return;

                // scan identifier
                case 'A':
                case 'B':
                case 'C':
                case 'D':
                case 'E':
                case 'F':
                case 'G':
                case 'H':
                case 'I':
                case 'J':
                case 'K':
                case 'M':
                case 'N':
                case 'O':
                case 'P':
                case 'Q':
                case 'R':
                case 'S':
                case 'T':
                case 'V':
                case 'W':
                case 'X':
                case 'Y':
                case 'Z':
                case 'a':
                case 'b':
                case 'c':
                case 'd':
                case 'e':
                case 'f':
                case 'g':
                case 'h':
                case 'i':
                case 'j':
                case 'k':
                case 'l':
                case 'm':
                case 'n':
                case 'o':
                case 'p':
                case 'q':
                case 'r':
                case 's':
                case 't':
                case 'v':
                case 'w':
                case 'x':
                case 'y':
                case 'z':
                //case '$':
                case '_':
                    scanIdent();
                    return;
                case '0':
                    scanChar();
                    if (ch == 'x' || ch == 'X')
                    {
                        scanChar();
                        if (digit(16) < 0)
                            lexError(ext_hexconstant_invalid);
                        scanNumber(16);
                    }
                    else
                    {
                        putChar('0');
                        scanNumber(8);
                    }
                    return;
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    scanNumber(10);
                    return;
                case '.':
                    if (isDigit(buf[bp + 1]))
                    {
                        putChar('.');
                        scanFractionAndSuffix();
                        return;
                    }
                    scanChar();
                    if (ch == '.')
                    {
                        scanChar();
                        if (ch == '.')
                        {
                            makeIdent("...");
                            return;
                        }
                        makeIdent("..");
                        return;
                    }
                    makeIdent(".");
                    return;

                case '(':
                case ')':
                case ',':
                case ';':
                case '[':
                case ']':
                case '{':
                case '}':
                case '?':
                case '~':
                    makeIdent(String.valueOf(ch));
                    return;

                case '-':
                    scanChar();
                    if (ch == '-')
                    {
                        makeIdent("--");
                        return;
                    }
                    if (ch == '>')
                    {
                        makeIdent("->");
                        return;
                    }
                    if (ch == '=')
                    {
                        makeIdent("-=");
                        return;
                    }
                    makeIdent("-");
                    return;

                case '<':
                    scanChar();
                    if (ch == '<')
                    {
                        scanChar();
                        if (ch == '=')
                        {
                            makeIdent("<<=");
                            return;
                        }
                        makeIdent("<<");
                        return;
                    }
                    if (ch == '=')
                    {
                        makeIdent("<=");
                        return;
                    }
                    makeIdent("<");
                    return;

                case '>':
                    scanChar();
                    if (ch == '=')
                    {
                        makeIdent(">=");
                        return;
                    }
                    if (ch == '>')
                    {
                        scanChar();
                        if (ch == '=')
                        {
                            makeIdent(">>=");
                            return;
                        }
                        makeIdent(">>");
                        return;
                    }
                    makeIdent(">");
                    return;

                case '%':
                    scanChar();
                    if (ch == '=')
                    {
                        makeIdent("%=");
                        return;
                    }
                    makeIdent("%");
                    return;

                default:
                    if (bp == buflen || (ch == EOI && bp + 1 == buflen))
                    {
                        tokenAheads.addLast(new Token(EOI, new SourceLocation(line, col)));
                    }
                    else
                    {
                        lexError(err_invalid_character_to_charify).addTaggedVal(ch);
                        scanChar();
                    }
                    return;
            }
        }
    }

    /**
     * A character buffer for literals, including integer and string.
     */
    private char[] sbuf = new char[128];

    private int sp = 0;

    /**
     * Append a character to sbuf.
     */
    private void putChar(char ch)
    {
        if (sp == sbuf.length)
        {
            char[] newsbuf = new char[sbuf.length * 2];
            System.arraycopy(sbuf, 0, newsbuf, 0, sbuf.length);
            sbuf = newsbuf;
        }
        sbuf[sp++] = ch;
    }

    /**
     * Read fractional part of floating point number.
     */
    private void scanFraction()
    {
        while (digit(10) >= 0)
        {
            putChar(ch);
            scanChar();
        }
        int sp1 = sp;
        if (ch == 'e' || ch == 'E')
        {
            putChar(ch);
            scanChar();
            if (ch == '+' || ch == '-')
            {
                putChar(ch);
                scanChar();
            }
            if ('0' <= ch && ch <= '9')
            {
                do
                {
                    putChar(ch);
                    scanChar();
                } while ('0' <= ch && ch <= '9');
                return;
            }
            lexError(err_invalid_suffix_float_constant);
            sp = sp1;
        }
    }

    /**
     * Read fractional part and 'd' or 'f' suffix of floating point number.
     */
    private void scanFractionAndSuffix()
    {
        scanFraction();
        if (ch == 'f' || ch == 'F')
        {
            putChar(ch);
            scanChar();
            Float val = Float.parseFloat(String.valueOf(sbuf, 0, sp));
            tokenAheads.addLast(new FLiteral(val, new SourceLocation(line, col)));
        }
        else
        {
            if (ch == 'd' || ch == 'D')
            {
                putChar(ch);
                scanChar();
            }
            Float val = Float.parseFloat(String.valueOf(sbuf, 0, sp));
            tokenAheads.addLast(new FLiteral(val, new SourceLocation(line, col)));
        }
    }

    /**
     * Skips block comment.
     */
    private void skipComment()
    {
        while (bp < buflen)
        {
            switch (ch)
            {
                case '*':
                    scanChar();
                    if (ch == '/')
                        return;
                    break;

                default:
                    scanChar();
                    break;
            }
        }
    }

    /**
     * Convert an ASCII digit from its base (8, 10, or 16) to its value.
     */
    private int digit(int base)
    {
        char c = ch;
        int result = Character.digit(c, base);
        if (result >= 0 && c > 127)
        {
            lexError(err_invalid_octal_digit);
            ch = "0123456789abcdef".charAt(0);
        }
        return result;
    }

    private void makeIdent(String s)
    {
        Token tok;
        if (keywords.isKeyword(s))
            tok = keywords.getByName(s);
        else
            tok = new Ident(s, new SourceLocation(line, col));
        tokenAheads.addLast(tok);
    }

    /**
     * estimates whether the ch is a digit or not.
     */
    private boolean isDigit(char ch)
    {
        return ch >= '0' && ch <= '9';
    }

    /**
     * Scan the identifier.
     */
    private void scanIdent()
    {
        putChar(ch);
        while (true)
        {
            scanChar();
            if (isDigit(ch) || (ch >= 'a' && ch <= 'z') || (ch >= 'A'
                    && ch <= 'Z') || ch == '_')
            {
                putChar(ch);
            }
            else
                break;
        }
        makeIdent(String.valueOf(sbuf, 0, sp));
    }

    private void scanNumber(int base)
    {
        int digitRadix = base<= 10? 10: 16;
        while(digit(digitRadix) >= 0)
        {
            putChar(ch);
            scanChar();
        }

        if (base<= 10 && ch == '.')
        {
            putChar(ch);
            scanChar();
            scanFractionAndSuffix();
        }
        else if (base <= 10 && (ch == 'e' || ch == 'E'||ch == 'f' || ch == 'F'
        || ch == 'D' || ch == 'd'))
        {
            scanFractionAndSuffix();
        }
        else if (ch == 'L'||ch == 'l')
        {
            // current number constant is long suffixed.
            scanChar();
            boolean isUnsigned = false;
            if (ch == 'U' || ch == 'u')
            {
                scanChar();
                isUnsigned = true;
            }
            char[] temp = new char[sp];
            System.arraycopy(sbuf, 0, temp, 0, sp);
            tokenAheads.addLast(new IntLiteral(temp, new SourceLocation(line, col),
                    base, IntLiteralKinds.IL_Long, isUnsigned));
        }
        else if (ch == 'U' || ch == 'u')
        {
            // number constant is suffixed with unsigned.
            scanChar();
            IntLiteralKinds il = IntLiteralKinds.IL_Int;
            if (ch == 'L' || ch == 'l')
            {
                scanChar();
                il = IntLiteralKinds.IL_Long;
            }
            char[] temp = new char[sp];
            System.arraycopy(sbuf, 0, temp, 0, sp);
            tokenAheads.addLast(new IntLiteral(temp, new SourceLocation(line, col),
                    base, il, false));
        }
    }

    /**
     * Reads a string.
     */
    private void readString()
    {
        while (true)
        {
            scanChar();
            if (ch == EOF)
                lexError(err_unterminated_string);
            if (ch == '\"')
                break;
            putChar(ch);
        }
        tokenAheads.addLast(new StringLiteral(String.valueOf(sbuf, 0, sp),
                new SourceLocation(line, col)));
    }

    /**
     * Reads next character.
     */
    private void readChar()
    {
        scanChar();
        if (ch != '\'')
        {
            lexError(err_unterminated_char);
        }
        else
        {
            tokenAheads.addLast(new CharLiteral(ch, new SourceLocation(line, col)));
        }
    }

    /**
     * Report an error at the current token position.
     */
    private Diagnostic.DiagnosticBuilder lexError(int diagID)
    {
        return diags.report(new FullSourceLoc(new SourceLocation(line, col),
                pp.getInputFile()), diagID);
    }
}
