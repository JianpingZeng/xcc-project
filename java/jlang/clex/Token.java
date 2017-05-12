package jlang.clex;

import jlang.basic.SourceLocation;

import static jlang.clex.Token.TokenFlags.*;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class Token
{
    public static class StrData
    {
        /**
         * The entire char buffer.
         */
        public final char[] buffer;
        /**
         * The starting index of the {@linkplain #data} in
         * the {@linkplain #buffer}
         */
        public final int offset;

        public StrData(final char[] buffer, int offset)
        {
            this.buffer = buffer;
            this.offset = offset;
        }
    }

    private SourceLocation loc;
    /**
     *  This is a union of three different pointer types, which depends
     /// on what type of token this is:
     ///  Identifiers, keywords, etc:
     ///    This is an IdentifierInfo*, which contains the uniqued identifier
     ///    spelling.
     ///  Literals:  isLiteral() returns true.
     ///    This is a pointer to the start of the token in a text buffer, which
     ///    may be dirty (have trigraphs / escaped newlines).
     */
    private Object data;

    private TokenKind kind;
    private int flags;
    private int length;

    public interface TokenFlags
    {
        int StartOfLine   = 0x01,  // At start of line or only after whitespace.
            LeadingSpace  = 0x02,  // Whitespace exists before this token.
            DisableExpand = 0x04,  // This identifier may never be macro expanded.
            NeedsCleaning = 0x08;   // Contained an escaped newline or trigraph.
    }

    public TokenKind getKind()
    {
        return kind;
    }

    public void setKind(TokenKind kind)
    {
        this.kind = kind;
    }

    public boolean is(TokenKind k) {return kind == k;}

    public boolean isNot(TokenKind k) {return kind != k;}

    public boolean isLiteral()
    {
        return is(TokenKind.numeric_constant) || is(TokenKind.char_constant)
                || is(TokenKind.string_literal) || is(TokenKind.angle_string_literal);
    }

    public SourceLocation getLocation()
    {
        return loc;
    }

    public void setLocation(SourceLocation loc)
    {
        this.loc = loc;
    }

    public String getName()
    {
        return kind.name;
    }

    public void startToken()
    {
        kind = TokenKind.Unknown;
        flags = 0;
        data = null;
        loc = new SourceLocation();
    }

    public IdentifierInfo getIdentifierInfo()
    {
        if (isLiteral()) return null;
        return (IdentifierInfo)data;
    }

    public void setIdentifierInfo(IdentifierInfo ii)
    {
        data = ii;
    }

    public StrData getLiteralData()
    {
        assert isLiteral(): "Cannot get literal data of non-literal";
        return (StrData) data;
    }

    public void setLiteralData(char[] buffer, int offset)
    {
        assert isLiteral(): "Cannot get literal data of non-literal";
        data = new StrData(buffer, offset);
    }

    public void setFlag(int flag)
    {
        flags |= flag;
    }

    public void clearFlag(int flag)
    {
        flags &= ~flag;
    }

    public int getFlags()
    {
        return flags;
    }

    public void setFlagValue(int flag, boolean val)
    {
        if (val)
            setFlag(flag);
        else
            clearFlag(flag);
    }

    public boolean isAtStartOfLine()
    {
        return (flags & StartOfLine) != 0;
    }

    public boolean hasLeadingSpace()
    {
        return (flags & LeadingSpace) != 0;
    }

    public boolean isExpandingDisabled()
    {
        return (flags & DisableExpand) != 0;
    }

    public boolean needsCleaning()
    {
        return (flags & NeedsCleaning) != 0;
    }

    public void setLength(int length)
    {
        this.length = length;
    }

    public int getLength()
    {
        return length;
    }
}
