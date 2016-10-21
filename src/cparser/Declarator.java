package cparser;

import cparser.DeclSpec.DeclaratorChunk.FunctionTypeInfo;
import cparser.DeclSpec.SourceRange;
import type.IntegerType;
import utils.OutParamWrapper;
import utils.Position;
import java.util.ArrayList;

/**
 * This class encapsulates some information about a declarator, including the
 * parsed type information and identifier. When the declarator is fully formed,
 * this is turned into the appropriate {@linkplain sema.Decl} object.
 *
 * <p>
 *  Declarators come in two types: normal declarators and abstract declarators.
 *  Abstract declarators are used when parsing type, and don't have and identifier.
 *  Normal declarators do have ID.
 * @author Xlous.zeng
 * @version 0.1
 */
public class Declarator
{
    enum TheContext
    {
        FileContext,
        FunctionProtoTypeContext,
        TypeNameContext,        // abstract declarator for types.
        StructFieldContext,     // struct/union field.
        BlockContext,           // declaration within a block in a function.
        ForContext,             //  declaration within the first part of for.
    }

    private DeclSpec ds;
    private String name;
    private SourceRange range;

    /**
     * Where we are parsing this declarator.
     */
    private TheContext context;

    private boolean invalidType;
    /**
     * Is this Declarator for a function definition.
     */
    private boolean functionDefinition;
    private Token.Ident id;
    private int IdLoc;

    private ArrayList<DeclSpec.DeclaratorChunk> declTypeInfos;

    Declarator(DeclSpec ds, TheContext context)
    {
        this.ds = ds;
        this.range = ds.getSourceRange();
        this.context = context;
        invalidType = ds.getTypeSpecType() == DeclSpec.TST.TST_error;
        functionDefinition = false;
        declTypeInfos = new ArrayList<>(8);
    }

    public DeclSpec getDeclSpec() { return ds; }

    public String getName() { return name; }

    public TheContext getContext() { return context; }

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

    public void setRangeEnd(int loc)
    {
        assert loc != Position.NOPOS;
        range.setEnd(loc);
    }

    public void setRangeStart(int loc)
    {
        assert loc != Position.NOPOS;
        range.setStart(loc);
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
        if (range.getStart() == Position.NOPOS)
            range.setStart(sr.getStart());
        if (range.getEnd() == Position.NOPOS)
            range.setEnd(sr.getEnd());
    }

    public void setIdentifier(Token.Ident id, int IdLoc)
    {
        this.id = id;
        this.IdLoc =IdLoc;
    }

    public void setInvalidType(boolean val)
    {
        this.invalidType = val;
    }

    public void addTypeInfo(DeclSpec.DeclaratorChunk chunk,
            int endLoc)
    {
        declTypeInfos.add(chunk);
        if (endLoc != Position.NOPOS)
            setRangeEnd(endLoc);
    }

    public int getNumTypeObjects()
    {
        return declTypeInfos.size();
    }

    public int getIdentifierLoc()
    {
        return range.getStart();
    }

    public boolean isInvalidType()
    {
        return invalidType || ds.getTypeSpecType() == DeclSpec.TST.TST_error;
    }

    public boolean hasName()
    {
        return name != null;
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
        // name is valid.
        return name != null;
    }

    /**
     * Retrieves the function type info object(looking through parentheses).
     * @return
     */
    public FunctionTypeInfo getFunctionTypeInfo()
    {
        assert isFunctionDeclarator():"Not a function declarator!";
        OutParamWrapper<Integer> index = new OutParamWrapper<>();
        isFunctionDeclarator(index);
        return (FunctionTypeInfo) declTypeInfos.get(index.get()).typeInfo;
    }

}
