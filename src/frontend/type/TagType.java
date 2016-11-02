package frontend.type;

import frontend.sema.Decl;
import frontend.sema.Decl.TagDecl;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class TagType extends Type
{
    /**
     * Stores the tagDecl associated with this frontend.type. The decl may point to any
     * {@linkplain TagDecl} that declares the entity.
     */
    protected TagDecl decl;
    /**
     * Constructor with one parameter which represents the kind of frontend.type
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
     * Determines if this frontend.type is being defined.
     * @return
     */
    public boolean isDefined() { return decl.isBeingDefined(); }
}
