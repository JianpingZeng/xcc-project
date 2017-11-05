package jlang.type;

import jlang.support.PrintingPolicy;
import jlang.sema.Decl.TypeDefDecl;

/**
 * This class was served as represents typedef declaration in C language.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class TypedefType extends Type
{
    /**
     * The actual jlang.type of this typedef declaration.
     */
    private TypeDefDecl decl;
    /**
     * Constructor with one parameter which represents the kind of jlang.type
     * for reason of comparison convenient.
     *
     * @param tc
     * @param d
     */
    public TypedefType(int tc, TypeDefDecl d, QualType canonicalType)
    {
        super(tc, canonicalType);
        decl = d;
        assert !(canonicalType.getType() instanceof TypedefType):"Invalid canonical type!";
    }

    public String toString()
    {
        return decl.getIdentifier().getName();
    }

    public TypeDefDecl getDecl() { return decl;}

    /**
     * Return the ultimate type this typedef corresponds to
     * potentially looking through *all* consecutive typedefs.  This returns the
     * sum of the type qualifiers, so if you have:
     * <pre>
     *   typedef const int A;
     *   typedef volatile A B;
     * </pre>
     * looking through the typedefs for B will give you "const volatile A".
     * @return
     */
    public QualType lookThroughTypedefs()
    {
        QualType firstType = getDecl().getUnderlyingType();
        if (! (firstType.getType() instanceof TypedefType))
            return firstType;

        // Otherwise, do the fully general loop.
        int typeQuals = 0;
        TypedefType tdt = this;
        while (true)
        {
            QualType curType = tdt.getDecl().getUnderlyingType();

            typeQuals |= curType.getCVRQualifiers();
            if (!(curType.getType() instanceof TypedefType))
                return new QualType(curType.getType(), typeQuals);
            tdt = (TypedefType)curType.getType();
        }
    }

    @Override
    public String getAsStringInternal(String inner, PrintingPolicy policy)
    {
        if (!inner.isEmpty())
            inner = " " + inner;

        return getDecl().getIdentifier().getName() + inner;
    }
}
