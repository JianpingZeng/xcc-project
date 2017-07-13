package jlang.cparser;

import jlang.cparser.DeclSpec.DeclaratorChunk;
import jlang.cparser.DeclSpec.DeclaratorChunk.FunctionTypeInfo;
import jlang.support.SourceLocation;
import jlang.support.SourceRange;
import jlang.clex.IdentifierInfo;
import jlang.sema.Decl;
import tools.OutParamWrapper;

import java.util.ArrayList;

/**
 * This class encapsulates some information about a declarator, including the
 * parsed jlang.type information and identifier. When the declarator is fully
 * formed, this is turned into the appropriate {@linkplain Decl} object.
 *
 * <p>
 *  Declarators come in two types: normal declarators and abstract declarators.
 *  Abstract declarators are used when parsing jlang.type, and don't have
 *  and identifier. Normal declarators do have ID.
 *  </p>
 * @author Xlous.zeng
 * @version 0.1
 */
public class Declarator
{
    public enum TheContext
    {
        FileContext,                // file scope declaraton.
        FunctionProtoTypeContext, // Within a function prototype.
        TypeNameContext,           // abstract declarator for types.
        StructFieldContext,        // struct/union field.
        BlockContext,               // declaration within a block in a function.
        ForContext,                 //  declaration within the first part of for.
    }

    public enum DeclarationKind
    {
        /**
         * An abstract declarator.
         */
        DK_Abstract,

        /**
         * A normal declarator.
         */
        DK_Normal,
    }

    private DeclSpec ds;
    private IdentifierInfo name;
    private SourceLocation identifierLoc;
    private SourceRange range;

    /**
     * Where we are parsing this declarator.
     */
    private TheContext context;
    /**
     * What kind of declarator this is .
     */
    private DeclarationKind kind;

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

    public Declarator(DeclSpec ds, TheContext context)
    {
        this.ds = ds;
        this.range = ds.getSourceRange();
        this.context = context;
        invalidType = ds.getTypeSpecType() == DeclSpec.TST.TST_error;
        functionDefinition = false;
        declTypeInfos = new ArrayList<>(8);
    }

    public DeclSpec getDeclSpec() { return ds; }

    public IdentifierInfo getIdentifier() { return name; }

    public TheContext getContext() { return context; }

    public DeclarationKind getKind() {return kind;}

    public boolean isProtoTypeContext()
    {
        return context == TheContext.FunctionProtoTypeContext;
    }

    public SourceRange getSourceRange()
    {
        return range;
    }

    public void setSourceRange(SourceRange range)
    {
        this.range = range;
    }

    public void setRangeEnd(SourceLocation loc)
    {
        range.setEnd(loc);
    }

    public void setRangeStart(SourceLocation loc)
    {
        range.setBegin(loc);
    }
    /**
     * Return true if the identifier is either optional or
     * not allowed.  This is true for typenames, prototypes,
     * @return
     */
    public boolean mayOmitIdentifier()
    {
        switch (context)
        {
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
     * @return
     */
    public boolean mayHaveIdentifier()
    {
        switch (context)
        {
            case FileContext:
            case StructFieldContext:
            case BlockContext:
            case ForContext:
            case FunctionProtoTypeContext:
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
     * @param ds
     */
    public void extendWithDeclSpec(DeclSpec ds)
    {
        SourceRange sr = ds.getSourceRange();
        if (range.getBegin() == SourceLocation.NOPOS)
            range.setBegin(sr.getBegin());
        if (range.getEnd() == SourceLocation.NOPOS)
            range.setEnd(sr.getEnd());
    }

    public void setIdentifier(String id,
            SourceLocation IdLoc)
    {
        this.identifierLoc =IdLoc;
    }

    public void setInvalidType(boolean val)
    {
        this.invalidType = val;
    }

    public void addTypeInfo(DeclaratorChunk chunk,
            SourceLocation endLoc)
    {
        declTypeInfos.add(chunk);
        if (endLoc != SourceLocation.NOPOS)
            setRangeEnd(endLoc);
    }

    public int getNumTypeObjects()
    {
        return declTypeInfos.size();
    }

    public SourceLocation getIdentifierLoc()
    {
        return identifierLoc;
    }

    public void setIdentifierLoc(IdentifierInfo id, SourceLocation loc)
    {
        name = id;
        identifierLoc = loc;
        if (id != null)
            kind = DeclarationKind.DK_Normal;
        else
            kind = DeclarationKind.DK_Abstract;
        setRangeEnd(loc);
    }

    public boolean isInvalidType()
    {
        return invalidType || ds.getTypeSpecType() == DeclSpec.TST.TST_error;
    }

    /**
     * Whether this declarator has a name (for normal declarator) or not.
     * @return
     */
    public boolean hasName()
    {
        return kind !=DeclarationKind.DK_Abstract;
    }

    public boolean isFunctionDeclarator()
    {
        OutParamWrapper<Integer> index = new OutParamWrapper<>();
        return isFunctionDeclarator(index);
    }

    /**
     * This function returns true if the declarator is a function declarator
     * (looking through parenthesis).
     * @return
     */
    public boolean isFunctionDeclarator(OutParamWrapper<Integer> index)
    {
        assert index != null;

        for (int size = declTypeInfos.size(), i = 0;
             i < size; ++i)
        {
            switch (declTypeInfos.get(i).kind)
            {
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

    public void setFunctionDefinition(boolean val)
    {
        this.functionDefinition = val;
    }

    public void clear()
    {
        name = null;
        range = ds.getSourceRange();
        declTypeInfos.clear();
    }

    /**
     * Returns true if we have passed the program point where the identifier
     * declared.
     * @return
     */
    public boolean isPastIdentifier()
    {
        // getIdentifier is valid.
        return name != null;
    }

    /**
     * Retrieves the function jlang.type info object(looking through parentheses).
     * @return
     */
    public FunctionTypeInfo getFunctionTypeInfo()
    {
        assert isFunctionDeclarator():"Not a function declarator!";
        OutParamWrapper<Integer> index = new OutParamWrapper<>();
        isFunctionDeclarator(index);
        return (FunctionTypeInfo) declTypeInfos.get(index.get()).typeInfo;
    }

    public DeclaratorChunk getTypeObject(int i)
    {
        assert i >= 0 && i < declTypeInfos.size();
        return declTypeInfos.get(i);
    }

}
