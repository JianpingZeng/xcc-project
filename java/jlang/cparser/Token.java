package jlang.cparser;

import jlang.cpp.SourceLocation;
import jlang.sema.APInt;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class Token implements Tag
{
    public final int tag;
    public final SourceLocation loc;

    public Token(int tag, SourceLocation loc)
    {
        this.tag = tag;
        this.loc = loc;
    }

    public SourceLocation getLocation()
    {
        return loc;
    }

    public String getIdentifierInfo()
    {
        return null;
    }

    public static class IntLiteral extends Token
    {
        public boolean getIntegerValue(APInt resultVal)
        {
            return false;
        }

        enum IntLiteralKinds
        {
            IL_Int,
            IL_Long,
            IL_Longlong,
        }

        private final char[] buf;
        private final int radix;
        private IntLiteralKinds IL;
        private boolean isUnsigned;

        public IntLiteral(char[] value,
                SourceLocation loc,
                int radix,
                IntLiteralKinds IL,
                boolean isUnsigned)
        {
            super(INTLITERAL, loc);
            buf = value;
            this.radix = radix;
            this.IL = IL;
            this.isUnsigned = isUnsigned;
        }

        public boolean isUnsigned()
        {
            return isUnsigned;
        }

        public int getRadix()
        {
            return radix;
        }

        public boolean isInt()
        {
            return IL == IntLiteralKinds.IL_Int;
        }

        public boolean isLong()
        {
            return IL == IntLiteralKinds.IL_Long;
        }

        public boolean isLongLong()
        {
            return IL == IntLiteralKinds.IL_Longlong;
        }

        public String toString()
        {
            return String.valueOf(buf);
        }
    }

    public static class FLiteral extends Token
    {
        final float value;

        public FLiteral(float value, SourceLocation loc)
        {
            super(FLOATLITERAL, loc);
            this.value = value;
        }

        public float getValue()
        {
            return value;
        }
    }

    public static class DLiteral extends Token
    {
        final double value;

        public DLiteral(double value, SourceLocation loc)
        {
            super(DOUBLELITERAL, loc);
            this.value = value;
        }

        public double getValue()
        {
            return value;
        }
    }

    public static class StringLiteral extends Token
    {
        final String value;

        public StringLiteral(String value, SourceLocation loc)
        {
            super(STRINGLITERAL, loc);
            this.value = value;
        }

        public String getValue() { return value; }
    }

    public static class CharLiteral extends Token
    {
        final char value;

        public CharLiteral(char value, SourceLocation loc)
        {
            super(CHARLITERAL, loc);
            this.value = value;
        }

        public char getValue() { return value; }
    }

    public static class Ident extends Token
    {
        final String name;

        public Ident(String name, SourceLocation loc)
        {
            super(IDENTIFIER, loc);
            this.name = name;
        }
        public String getName() { return name;}

        public String getIdentifierInfo()
        {
            return name;
        }
    }

    public static class Keyword extends Token
    {
        public Keyword(int tag)
        {
            this(tag, new SourceLocation());
        }
        public Keyword(int tag, SourceLocation loc)
        {
            super(tag, loc);
        }
    }

    private static String[] names = {"eof", "error", "identifier",
                                    "void", "_Bool", "char", "short",
                                    "int", "long", "long long", "float",
                                    "double", "unsigned", "signed",
                                    "struct", "union", "_Complex",
                                    "typename", "static", "register",
                                    "extern", "typedef", "auto", "const",
                                    "volatile", "restrict", "inline",
                                    "break", "case", "continue", "default",
                                    "do", "else", "for", "goto", "if",
                                    "return", "sizeof", "switch", "while",
                                    "for", "integer literal", "long literal",
                                    "float literal", "double literal",
                                    "char literal", "string literal",
                                    "(", ")", "{", "}", "[", "]", ";",
                                    ",", ".", "=", ">", "<", "!", "~",
                                    "?", ":", "==", "<=", ">=", "!=",
                                    "&&", "||", "++", "--", "+", "-",
                                    "->", "*", "/", "&", "|", "^", "%",
                                    "<<", ">>", "+=", "-=", "*=", "/=",
                                    "&=", "|=", "^=", "%=", "<<=", ">>=",
                                    "..."};
    public static String getTokenName(int tag)
    {
        assert tag>= EOF && tag < TokenCount:"invalid token tag " + tag;
        return names[tag];
    }
}
