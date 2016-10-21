package type;

import sema.Decl.TagDecl;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class TagType extends Type
{
    /**
     * Stores the tagDecl associated with this type. The decl may point to any
     * {@linkplain TagDecl} that declares the entity.
     */
    protected TagDecl decl;
    /**
     * Constructor with one parameter which represents the kind of type
     * for reason of comparison convenient.
     *
     * @param tag
     */
    public TagType(int tag,TagDecl decl)
    {
        super(tag);
        this.decl = decl;
    }

    public TagDecl getDecl() { return decl; }

    /**
     * Determines if this type is being defined.
     * @return
     */
    public boolean isDefined() { return decl.isBeingDefined(); }
}
