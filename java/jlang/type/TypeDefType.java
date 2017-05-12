package jlang.type;

import jlang.sema.Decl.TypeDefDecl;

/**
 * This class was served as represents typedef declaration in C language.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class TypeDefType extends Type
{
    /**
     * The actual jlang.type of this typedef declaration.
     */
    private TypeDefDecl decl;
    /**
     * Constructor with one parameter which represents the kind of jlang.type
     * for reason of comparison convenient.
     *
     * @param tag
     * @param d
     */
    public TypeDefType(int tag, TypeDefDecl d, QualType canonicalType)
    {
        super(tag, canonicalType);
        decl = d;
    }

    public String toString()
    {
        return decl.getDeclName().getName();
    }

    public TypeDefDecl getDecl() { return decl;}
}
