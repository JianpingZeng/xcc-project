package jlang.type;

import jlang.sema.Decl;
import jlang.sema.Decl.TagDecl;
import jlang.support.PrintingPolicy;
import tools.Util;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public abstract class TagType extends Type {
  /**
   * Stores the tagDecl associated with this jlang.type. The decl may point to any
   * {@linkplain TagDecl} that declares the entity.
   */
  protected TagDecl decl;

  /**
   * Constructor with one parameter which represents the kind of jlang.type
   * for reason of comparison convenient.
   *
   * @param tc
   */
  public TagType(int tc, TagDecl decl, QualType canonical) {
    super(tc, canonical);
    this.decl = decl;
  }

  public TagDecl getDecl() {
    return decl;
  }

  /**
   * Determines if this jlang.type is being defined.
   *
   * @return
   */
  public boolean isBeingDefined() {
    return decl.isCompleteDefinition();
  }

  public void setBeingDefined() {
    decl.setBeingDefined();
  }

  @Override
  public String getAsStringInternal(String inner, PrintingPolicy policy) {
    if (policy.suppressTag)
      return inner;

    if (!inner.isEmpty())
      inner = ' ' + inner;
    String kind = policy.suppressTagKind ? null : getDecl().getKindName();
    String id;
    if (decl.getIdentifier() != null)
      id = decl.getIdentifier().getName();
    else if (decl.getTypedefAnonDecl() != null) {
      Decl.TypeDefDecl typedef = decl.getTypedefAnonDecl();
      kind = null;
      Util.assertion(typedef.getIdentifier() != null, "Typedef without identifier!");
      id = typedef.getIdentifier().getName();
    } else {
      id = "<anonymous>";
    }

    if (kind != null) {
      inner = kind + " " + id + inner;
    } else
      inner = id + inner;
    return inner;
  }

  public void setDecl(TagDecl decl) {
    this.decl = decl;
  }
}
