package jlang.cparser;

import tools.Position;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class Token implements Tag
{
    public final int tag;
    public final int loc;

    public Token(int tag, int loc)
    {
        this.tag = tag;
        this.loc = loc;
    }

    public static class IntLiteral extends Token
    {
        enum IntLong
        {
            IL_None,
            IL_Long,
            IL_Longlong,
        }

        private final char[] buf;
        private final int radix;
        private int len;
        private IntLong IL;
        private boolean isUnsigned;

        public IntLiteral(char[] value,
                int loc,
                int radix,
                IntLong IL,
                boolean isUnsigned)
        {
            super(INTLITERAL, loc);
            buf = value;
            this.radix = radix;
            len = value.length;
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

        public boolean isLong()
        {
            return IL == IntLong.IL_Long;
        }

        public String toString()
        {
            return buf.toString();
        }
    }

    public static class FLiteral extends Token
    {
        final float value;

        public FLiteral(float value, int loc)
        {
            super(FLOATLITERAL, loc);
            this.value = value;
        }
    }

    public static class DLiteral extends Token
    {
        final double value;

        public DLiteral(double value, int loc)
        {
            super(DOUBLELITERAL, loc);
            this.value = value;
        }
    }

    public static class StringLiteral extends Token
    {
        final String value;

        public StringLiteral(String value, int loc)
        {
            super(STRINGLITERAL, loc);
            this.value = value;
        }

        public String getValue() { return value; }
    }

    public static class CharLiteral extends Token
    {
        final char value;

        public CharLiteral(char value, int loc)
        {
            super(CHARLITERAL, loc);
            this.value = value;
        }

        public char getValue() { return value; }
    }

    public static class Ident extends Token
    {
        final String name;

        public Ident(String name, int loc)
        {
            super(IDENTIFIER, loc);
            this.name = name;
        }
        public String getName() { return name;}
    }

    public static class Keyword extends Token
    {
        public Keyword(int tag)
        {
            this(tag, Position.NOPOS);
        }
        public Keyword(int tag, int loc)
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
