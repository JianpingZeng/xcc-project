package jlang.cparser;

import jlang.cparser.Token.*;
import jlang.cparser.Token.IntLiteral.IntLong;
import jlang.cpp.CPPReader;
import jlang.cpp.Preprocessor;
import tools.LayoutCharacters;
import tools.Position;

/**
 * The lexical analyzer maps an input stream consisting of ASCII characters into
 * a token sequence.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public class Scanner implements Tag, LayoutCharacters
{
    /**
     * The token, set by nextToken().
     */
    Token token;

    /**
     * The token's position. pos = line << Position.LINESHIFT + col. Line and
     * column numbers start at 1.
     */
    private int pos;

    /**
     * The last character position of the token.
     */
    private int endPos;

    /**
     * The last character position of the previous token.
     */
    int prevEndPos;

    /**
     * The position where a lexical error occurred;
     */
    int errPos = Position.NOPOS;

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

    public Scanner(Preprocessor pp)
    {
        this.keywords = Keywords.instance();

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
            lexError("io.exception:", e.getMessage());
            buf = new char[1];
            buflen = 0;
        }
        buf[buflen] = EOI;
        line = 1;
        col = 0;
        bp = -1;
        scanChar();
        nextToken();
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
     * Read the next token.
     */
    void nextToken()
    {
        try
        {
            prevEndPos = endPos;
            sp = 0;
            while (true)
            {
                pos = (line << Position.LINESHIFT) + col;
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
                        makeIdent(Character.toString(ch));
                        return;
                    case '=':
                        scanChar();
                        if (ch == '=')
                        {
                            makeIdent("==");
                            return;
                        }
                        makeIdent(Character.toString(ch));
                        return;
                    case '!':
                        scanChar();
                        if (ch == '=')
                        {
                            makeIdent("!=");
                            return;
                        }
                        makeIdent(Character.toString(ch));
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
                            lexError("empty.char.list");
                        else
                        {
                            if (ch == CR || ch == LF)
                                lexError(pos, "illegal.line.end.in.char.lit");
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
                    case '$':
                    case '_':
                        scanIdent();
                        return;
                    case '0':
                        scanChar();
                        if (ch == 'x' || ch == 'X')
                        {
                            scanChar();
                            if (digit(16) < 0)
                                lexError("invalid.hex.number");
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
                        makeIdent(Character.toString(ch));
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
                            nextToken();
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
                        if (bp == buflen || ch == EOI && bp + 1 == buflen)
                        {
                            token = new Token(EOI, pos);
                        }
                        else
                        {
                            lexError("iilegal.char", Character.toString(ch));
                            scanChar();
                        }
                        return;
                }
            }

        }
        finally
        {
            endPos = (line << Position.LINESHIFT) + col - 1;
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
            lexError("malformed.fp.lit");
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
            token = new FLiteral(Float.parseFloat(sbuf.toString()), pos);
        }
        else
        {
            if (ch == 'd' || ch == 'D')
            {
                putChar(ch);
                scanChar();
            }
            token = new DLiteral(Double.parseDouble(sbuf.toString()), pos);
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
            lexWarning(pos + 1, "illegal.nonascii.digit");
            ch = "0123456789abcdef".charAt(result);
        }
        return result;
    }

    private void makeIdent(String s)
    {

        if (keywords.isKeyword(s))
            this.token = keywords.getByName(s);
        else
            token = new Ident(s, pos);
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
        StringBuilder sb = new StringBuilder(ch);
        while (true)
        {
            scanChar();
            if (isDigit(ch) || (ch >= 'a' && ch <= 'z') || (ch >= 'A'
                    && ch <= 'Z') || ch == '_' || ch == '$')
            {
                sb.append(ch);
            }
            else
                break;
        }
        makeIdent(sb.toString());

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
            token = new IntLiteral(temp, pos, base, IntLong.IL_Long, isUnsigned);
        }
        else if (ch == 'U' || ch == 'u')
        {
            // number constant is suffixed with unsigned.
            scanChar();
            IntLong il = IntLong.IL_None;
            if (ch == 'L' || ch == 'l')
            {
                scanChar();
                il = IntLong.IL_Long;
            }
            char[] temp = new char[sp];
            System.arraycopy(sbuf, 0, temp, 0, sp);
            token = new IntLiteral(temp, pos, base, il, false);
        }
    }

    /**
     * Reads a string.
     */
    private void readString()
    {
        StringBuilder sb = new StringBuilder();
        while (true)
        {
            scanChar();
            if (ch == EOF)
                lexError(pos, "unterminated string");
            if (ch == '\"')
                break;
            if (ch != '\\')
            {
                sb.append(ch);
                continue;
            }
            sb.append(ch);
        }
        token = new StringLiteral(sbuf.toString(), pos);
        return;
    }

    /**
     * Reads next character.
     */
    private void readChar()
    {
        char tmp = ch;
        scanChar();
        if (ch != '\'')
        {
            lexError(pos, "unclosed.char.literal");
        }
        else
        {
            token = new CharLiteral(ch, pos);
        }
    }

    private void lexError(String key, String arg)
    {
        lexError(pos, key, arg);
    }

    /**
     * Report an error at the given token position.
     */
    private void lexError(int pos, String key)
    {
        lexError(pos, key, null);
    }

    /**
     * Report an error at the current token position.
     */
    private void lexError(String key)
    {
        lexError(pos, key, null);
    }

    /**
     * Report an error at the given position using the provided argument.
     */
    private void lexError(int pos, String msg, String arg)
    {
        int line = pos >> Position.LINESHIFT;
        int col = pos & Position.COLUMNMASK;
        System.err.println(
                "At line " + line + "col " + col + ", a error occured:" + msg
                        + ", " + arg);
        token = new Token(ERROR, pos);
        errPos = pos;
    }

    /**
     * Report an warning at the given position using the provided argument.
     */
    private void lexWarning(int pos, String msg)
    {
        int line = pos >> Position.LINESHIFT;
        int col = pos & Position.COLUMNMASK;
        System.err.println(
                "At line " + line + "col " + col + ", a warning occured:"
                        + msg);
    }
}
