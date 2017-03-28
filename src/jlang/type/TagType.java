package jlang.type;

import jlang.sema.Decl;
import jlang.sema.Decl.TagDecl;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class TagType extends Type
{
    /**
     * Stores the tagDecl associated with this jlang.type. The decl may point to any
     * {@linkplain TagDecl} that declares the entity.
     */
    protected TagDecl decl;
    /**
     * Constructor with one parameter which represents the kind of jlang.type
     * for reason of comparison convenient.
     *
     * @param tag
     */
    public TagType(int tag,TagDecl decl)
    {
        super(tag);
        this.decl = decl;
    }

    public Decl.TagDecl getDecl() { return decl; }

    /**
     * Determines if this jlang.type is being defined.
     * @return
     */
    public boolean isBeingDefined() { return decl.isBeingDefined(); }
}
