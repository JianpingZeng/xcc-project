package cfe.cparser;

import cfe.ast.Tree;
import cfe.clex.IdentifierInfo;
import cfe.cparser.DeclSpec.DeclaratorChunk;
import cfe.cparser.DeclSpec.DeclaratorChunk.FunctionTypeInfo;
import cfe.sema.Decl;
import cfe.sema.UnqualifiedId;
import cfe.sema.UnqualifiedId.DeclarationKind;
import cfe.support.SourceLocation;
import cfe.support.SourceRange;
import tools.OutRef;
import tools.Util;

import java.util.ArrayList;

/**
 * This class encapsulates some information about a declarator, including the
 * parsed jlang.type information and identifier. When the declarator is fully
 * formed, this is turned into the appropriate {@linkplain Decl} object.
 * <p>
 * <p>
 * Declarators come in two types: normal declarators and abstract declarators.
 * Abstract declarators are used when parsing jlang.type, and don't have
 * and identifier. Normal declarators do have ID.
 * </p>
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public class Declarator {
  public enum TheContext {
    /**
     * file scope declaraton.
     */
    FileContext,

    /**
     * Within a function prototype, like void (int a){} or K&R void (a) int a; {}.
     */
    FunctionProtoTypeContext,

    /**
     * K&R type definition list for formals.
     */
    KNRTypeListContext,

    /**
     * abstract declarator for types.
     */
    TypeNameContext,

    /**
     * struct/union field.
     */
    StructFieldContext,

    /**
     * declaration within a block in a function.
     */
    BlockContext,

    /**
     * declaration within the first part of for.
     */
    ForContext,
  }

  private DeclSpec ds;

  private UnqualifiedId name;

  private SourceRange range;

  /**
   * Where we are parsing this declarator.
   */
  private TheContext context;

  private boolean invalidType;
  /**
   * Is this a grouping declarator, set by
   * {@linkplain Parser#parseParenDeclarator(Declarator)} function.
   */
  private boolean groupingParens;
  /**
   * Is this Declarator for a function definition.
   */
  private boolean functionDefinition;

  /**
   * This list holds each type that the declarator includes as it is parsed.
   * This is pushed from the identifier out, which means that first element
   * will be the most closely bound to the identifier, and the last one will
   * be the least closely bound.
   */
  private ArrayList<DeclaratorChunk> declTypeInfos;

  /**
   * Attributes.
   */
  private AttributeList attrList;

  private Tree.Expr asmLabel;

  public Declarator(DeclSpec ds, TheContext context) {
    this.ds = ds;
    this.range = ds.getSourceRange();
    this.context = context;
    invalidType = ds.getTypeSpecType() == DeclSpec.TST.TST_error;
    functionDefinition = false;
    declTypeInfos = new ArrayList<>(8);
    name = new UnqualifiedId();
  }

  public DeclSpec getDeclSpec() {
    return ds;
  }

  public IdentifierInfo getIdentifier() {
    return name.getIdentifier();
  }

  public TheContext getContext() {
    return context;
  }

  public DeclarationKind getKind() {
    return name.getkind();
  }

  public boolean isProtoTypeContext() {
    return context == TheContext.FunctionProtoTypeContext;
  }

  public SourceRange getSourceRange() {
    return range;
  }

  public void setSourceRange(SourceRange range) {
    this.range = range;
  }

  public void setRangeEnd(SourceLocation loc) {
    range.setEnd(loc);
  }

  public void setRangeStart(SourceLocation loc) {
    range.setBegin(loc);
  }

  /**
   * Return true if the identifier is either optional or
   * not allowed.  This is true for typenames, prototypes,
   *
   * @return
   */
  public boolean mayOmitIdentifier() {
    switch (context) {
      case FileContext:
      case StructFieldContext:
      case BlockContext:
      case ForContext:
        return false;

      case TypeNameContext:
      case FunctionProtoTypeContext:
        return true;
      default:
        return false;
    }
  }

  /**
   * ReturnStmt true if the identifier is either optional or
   * required.  This is true for normal declarators and prototypes, but not
   * typenames.
   *
   * @return
   */
  public boolean mayHaveIdentifier() {
    switch (context) {
      case FileContext:
      case StructFieldContext:
      case BlockContext:
      case ForContext:
      case FunctionProtoTypeContext:
      case KNRTypeListContext:
        return true;

      case TypeNameContext:
        return false;
      default:
        return false;
    }
  }

  /**
   * Extend the declarator source range to include the
   * given declspec, unless its location is invalid. Adopts the range start if
   * the current range start is invalid.
   *
   * @param ds
   */
  public void extendWithDeclSpec(DeclSpec ds) {
    SourceRange sr = ds.getSourceRange();
    if (range.getBegin() == SourceLocation.NOPOS)
      range.setBegin(sr.getBegin());
    if (range.getEnd() == SourceLocation.NOPOS)
      range.setEnd(sr.getEnd());
  }

  public void setIdentifier(
      IdentifierInfo id,
      SourceLocation loc) {
    name.setIdentifier(id, loc);
  }

  public void setInvalidType(boolean val) {
    this.invalidType = val;
  }

  public void addTypeInfo(DeclaratorChunk chunk,
                          SourceLocation endLoc) {
    declTypeInfos.add(chunk);
    if (endLoc != SourceLocation.NOPOS)
      setRangeEnd(endLoc);
  }

  public int getNumTypeObjects() {
    return declTypeInfos.size();
  }

  public SourceLocation getIdentifierLoc() {
    return name.getStartLocation();
  }

  public boolean isInvalidType() {
    return invalidType || ds.getTypeSpecType() == DeclSpec.TST.TST_error;
  }

  /**
   * Whether this declarator has a name (for normal declarator) or not (
   * for abstract declarator).
   *
   * @return
   */
  public boolean hasName() {
    return name.isValid();
  }

  public boolean isFunctionDeclarator() {
    OutRef<Integer> index = new OutRef<>();
    return isFunctionDeclarator(index);
  }

  /**
   * This function returns true if the declarator is a function declarator
   * (looking through parenthesis).
   *
   * @return
   */
  public boolean isFunctionDeclarator(OutRef<Integer> index) {
    Util.assertion(index != null);

    for (int i = 0, size = declTypeInfos.size(); i < size; ++i) {
      switch (declTypeInfos.get(i).kind) {
        case Function:
          index.set(i);
          return true;
        case Paren:
          continue;
        case Pointer:
        case Array:
          return false;
      }
    }
    return false;
  }

  public void setFunctionDefinition(boolean val) {
    this.functionDefinition = val;
  }

  public void clear() {
    name.clear();
    range = ds.getSourceRange();
    declTypeInfos.clear();
  }

  /**
   * Returns true if we have passed the program point where the identifier
   * declared.
   *
   * @return
   */
  public boolean isPastIdentifier() {
    // Identifier is valid.
    return name.isValid();
  }

  /**
   * Retrieves the function jlang.type info object(looking through parentheses).
   *
   * @return
   */
  public FunctionTypeInfo getFunctionTypeInfo() {
    Util.assertion(isFunctionDeclarator(), "Not a function declarator!");
    OutRef<Integer> index = new OutRef<>();
    isFunctionDeclarator(index);
    return (FunctionTypeInfo) declTypeInfos.get(index.get()).typeInfo;
  }

  public DeclaratorChunk getTypeObject(int i) {
    Util.assertion(i >= 0 && i < declTypeInfos.size());
    return declTypeInfos.get(i);
  }

  /**
   * simply adds the attribute list to the Declarator.
   * These examples both add 3 attributes to "var":
   * short int var __attribute__((aligned(16),common,deprecated));
   * short int x, __attribute__((aligned(16)) var
   * __attribute__((common,deprecated));
   * <p>
   * Also extends the range of the declarator.
   *
   * @param attr
   * @param lastLoc
   */
  public void addAttributes(AttributeList attr, SourceLocation lastLoc) {
    if (attr == null)
      return;

    if (attrList != null)
      attr.addAttributeList(attrList);

    attrList = attr;
    if (lastLoc.isValid())
      setRangeEnd(lastLoc);
  }

  public boolean hasGroupingParens() {
    return groupingParens;
  }

  public void setGroupingParens(boolean groupingParens) {
    this.groupingParens = groupingParens;
  }

  public void setAsmLabel(Tree.Expr asmLabel) {
    this.asmLabel = asmLabel;
  }

  public Tree.Expr getAsmLabel() {
    return asmLabel;
  }

  public AttributeList getAttributes() {
    return attrList;
  }

  /**
   * do we contain any attributes?
   *
   * @return
   */
  public boolean hasAttributes() {
    if (attrList != null || getDeclSpec().getAttributes() != null)
      return true;
    for (int i = 0, e = getNumTypeObjects(); i != e; i++) {
      if (getTypeObject(i).getAttrs() != null)
        return true;
    }
    return false;
  }
}
