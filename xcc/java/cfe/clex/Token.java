package cfe.clex;

import cfe.support.SourceLocation;
import tools.Util;

import static cfe.clex.Token.TokenFlags.*;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class Token implements Cloneable {
  private SourceLocation loc;
  /**
   * This is a union of three different pointer types, which depends
   * /// on what type of token this is:
   * ///  Identifiers, keywords, etc:
   * ///    This is an IdentifierInfo*, which contains the uniqued identifier
   * ///    spelling.
   * ///  Literals:  isLiteral() returns true.
   * ///    This is a pointer to the start of the token in a text buffer, which
   * ///    may be dirty (have trigraphs / escaped newlines).
   */
  private Object data;

  private TokenKind kind;
  private int flags;
  private int length;

  public interface TokenFlags {
    int StartOfLine = 0x01,  // At start of line or only after whitespace.
        LeadingSpace = 0x02,  // Whitespace exists before this token.
        DisableExpand = 0x04,  // This identifier may never be macro expanded.
        NeedsCleaning = 0x08;   // Contained an escaped newline or trigraph.
  }

  public TokenKind getKind() {
    return kind;
  }

  public void setKind(TokenKind kind) {
    this.kind = kind;
  }

  public boolean is(TokenKind k) {
    return kind == k;
  }

  public boolean isNot(TokenKind k) {
    return kind != k;
  }

  public boolean isLiteral() {
    return is(TokenKind.numeric_constant) || is(TokenKind.char_constant)
        || is(TokenKind.string_literal) || is(TokenKind.angle_string_literal);
  }

  public SourceLocation getLocation() {
    return loc;
  }

  public void setLocation(SourceLocation loc) {
    this.loc = loc;
  }

  public String getKindName() {
    return kind.name;
  }

  public void startToken() {
    kind = TokenKind.Unknown;
    flags = 0;
    data = null;
    loc = new SourceLocation();
  }

  public IdentifierInfo getIdentifierInfo() {
    if (isLiteral()) return null;
    return (IdentifierInfo) data;
  }

  public void setIdentifierInfo(IdentifierInfo ii) {
    data = ii;
  }

  public StrData getLiteralData() {
    Util.assertion(isLiteral(), "Cannot get literal data of non-literal");
    return (StrData) data;
  }

  public void setLiteralData(char[] buffer, int offset) {
    Util.assertion(isLiteral(), "Cannot get literal data of non-literal");
    data = new StrData(buffer, offset);
  }

  public void setLiteralData(StrData data) {
    Util.assertion(isLiteral(), "Cannot get literal data of non-literal");
    this.data = data;
  }

  public void setFlag(int flag) {
    flags |= flag;
  }

  public void clearFlag(int flag) {
    flags &= ~flag;
  }

  public int getFlags() {
    return flags;
  }

  public void setFlag(int flag, boolean val) {
    if (val)
      setFlag(flag);
    else
      clearFlag(flag);
  }

  public boolean isAtStartOfLine() {
    return (flags & StartOfLine) != 0;
  }

  public boolean hasLeadingSpace() {
    return (flags & LeadingSpace) != 0;
  }

  public boolean isExpandingDisabled() {
    return (flags & DisableExpand) != 0;
  }

  public boolean needsCleaning() {
    return (flags & NeedsCleaning) != 0;
  }

  public void setLength(int length) {
    this.length = length;
  }

  public int getLength() {
    return length;
  }

  public Token clone() {
    Token res = new Token();
    res.loc = loc;
    res.data = data;
    res.kind = kind;
    res.flags = flags;
    res.length = length;
    return res;
  }

  public static String getTokenSimpleSpelling(TokenKind kind) {
    switch (kind) {
      case l_bracket:
        return "[";
      case r_bracket:
        return "]";
      case l_paren:
        return "(";
      case r_paren:
        return ")";
      case l_brace:
        return "{";
      case r_brace:
        return "}";
      case dot:
        return ".";
      case ellipsis:
        return "...";
      case amp:
        return "&";
      case ampamp:
        return "&&";
      case ampequal:
        return "&=";
      case star:
        return "*";
      case starequal:
        return "*=";
      case plus:
        return "+";
      case plusplus:
        return "++";
      case plusequal:
        return "+=";
      case sub:
        return "-";
      case arrow:
        return "->";
      case subsub:
        return "--";
      case subequal:
        return "-=";
      case tilde:
        return "~";
      case bang:
        return "!";
      case bangequal:
        return "!=";
      case slash:
        return "/";
      case slashequal:
        return "/=";
      case percent:
        return "%";
      case percentequal:
        return "%=";
      case less:
        return "<";
      case lessless:
        return "<<";
      case lessequal:
        return "<=";
      case lesslessequal:
        return "<<=";
      case greater:
        return ">";
      case greatergreater:
        return ">>";
      case greaterequal:
        return ">=";
      case greatergreaterequal:
        return ">>=";
      case caret:
        return "^";
      case caretequal:
        return "^=";
      case bar:
        return "|";
      case barbar:
        return "||";
      case barequal:
        return "|=";
      case question:
        return "?";
      case colon:
        return ":";
      case semi:
        return ";";
      case equal:
        return "=";
      case equalequal:
        return "==";
      case comma:
        return ",";
      case hash:
        return "#";
      case hashhash:
        return "##";
      default:
        break;
    }
    return null;
  }

  /**
   * Fill the fields of this with other's fields.
   *
   * @param other
   */
  public void copyFrom(Token other) {
    if (other == null)
      return;

    loc = other.loc;
    data = other.data;
    kind = other.kind;
    flags = other.flags;
    length = other.length;
  }
}
