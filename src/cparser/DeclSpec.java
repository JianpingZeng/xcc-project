package cparser;

import ast.Tree.ExprStmt;
import cparser.Declarator.TheContext;
import sema.Decl;
import type.QualType;
import utils.OutParamWrapper;
import utils.Position;

import java.util.List;

import static cparser.DeclSpec.ParsedSpecifiers.*;
import static cparser.DeclSpec.SCS.*;
import static cparser.DeclSpec.TQ.*;
import static cparser.DeclSpec.TSC.*;
import static cparser.DeclSpec.TSW.*;
import static cparser.DeclSpec.TSS.*;
import static cparser.DeclSpec.TST.*;

/**
 * This class captures information about "declaration specifiers",
 * which encompasses storage-class-specifiers, type-specifiers,
 * type-qualifiers, and function-specifiers.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public class DeclSpec
{

    public static class DeclaratorChunk<T>
    {
        enum ChunkKind
        {
            Pointer,
            Array,
            Function,
            Paren
        }

        ChunkKind kind;
        /**
         * The place where this type was defined.
         */
        int loc;
        /**
         * IfStmt valid, the place where this chunk ends.
         */
        int endLoc;
        /**
         * This hold the reference to concrete TypeInfo, e.g. PointerTypeInfo,
         * FunctionTypeInfo, ArrayTypeInfo.
         */
        T typeInfo;

        public static class PointerTypeInfo
        {
            // the type qualifiers : const/volatile/rstrict
            int typeQuals;
            // the location of the const-qualifier, if any
            int constQualLoc;
            // the location of the volatile-qualifier, if any
            int volatileQualLoc;
            // the location of the restrict-qualifier, if any
            int restrictQualLoc;
        }

        public static class ArrayTypeInfo
        {
            // the type qualifiers : const/volatile/rstrict
            int typeQuals;
            // true if this dimension included the 'static' kwyword.
            boolean hasStatic;
            // true if this dimension was [*].
            boolean isStar;
            // This is the getTypeSize of the array, or null if [] or [*] was specified.
            ExprStmt numElts;
        }

        public static class FunctionTypeInfo
        {
            /**
             * This is true if the function had at least one typed argument.
             * IfStmt the function is () or (a, b, c), then it has no prototype,
             * and is treated as a K&R style function.
             */
            public boolean hasProtoType;

            /**
             * This is variable function parameters list.
             */
            public boolean isVariadic;

            /**
             * the type qualifiers : const/volatile/rstrict
             */
            public int typeQuals;

            /**
             * When isVariadic is true, the location of the ellipsis in the source.
             */
            public int ellipsisLoc;
            /**
             * The number of formal arguments provided for the declarator.
             */
            public int numArgs;

            public List<ParamInfo> argInfo;

            public QualType returnType;

            public boolean isKNRPrototype()
            {
                return !hasProtoType && numArgs != 0;
            }

            int getEllipsisLoc() { return ellipsisLoc; }
        }

        /**
         * ReturnStmt a declaratorChunk for a pointer.
         * @param typeQuals
         * @param loc
         * @param constQualLoc
         * @param volatileQualLoc
         * @param restrictQualLoc
         * @return
         */
        public static DeclaratorChunk
            getPointer(int typeQuals, int loc, int constQualLoc,
                    int volatileQualLoc, int restrictQualLoc)
        {
            DeclaratorChunk<PointerTypeInfo> res = new DeclaratorChunk<>();
            res.loc = loc;
            res.kind = ChunkKind.Pointer;
            res.typeInfo = new PointerTypeInfo();
            res.typeInfo.typeQuals = typeQuals;
            res.typeInfo.constQualLoc = constQualLoc;
            res.typeInfo.restrictQualLoc = restrictQualLoc;
            return res;
        }

        public static DeclaratorChunk
                getArray(int typeQuals,
                boolean isStatic,
                boolean isStar,
                ExprStmt numElts,
                int lBracketLoc,
                int rBracketLoc)
        {
            DeclaratorChunk<ArrayTypeInfo> res = new DeclaratorChunk<>();
            res.kind = ChunkKind.Array;
            res.loc = lBracketLoc;
            res.endLoc = rBracketLoc;
            res.typeInfo = new ArrayTypeInfo();
            res.typeInfo.typeQuals = typeQuals;
            res.typeInfo.hasStatic = isStatic;
            res.typeInfo.isStar = isStar;
            res.typeInfo.numElts = numElts;
            return res;
        }

        /**
         * ReturnStmt a DeclaratorChunk for a function.
         * "TheDeclarator" is the declarator that this will be added to
         * @param hasProto
         * @param isVariadic
         * @param ellipsisLoc
         * @param argInfo
         * @param typeQuals
         * @param rangeBegin
         * @param rangeEnd
         * @return
         */
        public static DeclaratorChunk
            getFunction(boolean hasProto,
                boolean isVariadic,
                int ellipsisLoc,
                List<ParamInfo> argInfo,
                int typeQuals,
                int rangeBegin,
                int rangeEnd)
        {
            DeclaratorChunk<FunctionTypeInfo> res = new DeclaratorChunk<>();
            res.kind = ChunkKind.Function;
            res.loc = rangeBegin;
            res.endLoc = rangeEnd;
            res.typeInfo = new FunctionTypeInfo();
            res.typeInfo.hasProtoType = hasProto;
            res.typeInfo.isVariadic = isVariadic;
            res.typeInfo.ellipsisLoc = ellipsisLoc;
            res.typeInfo.typeQuals = typeQuals;
            res.typeInfo.argInfo = argInfo;
            res.typeInfo.numArgs = argInfo.size();
            res.typeInfo.returnType = null;

            return res;
        }
    }

    public static class ParamInfo
    {
        public String name;
        public int loc;
        public Decl param;

        public ParamInfo(String ID, int loc, Decl param)
        {
            this.name = ID;
            this.loc = loc;
            this.param = param;
        }
    }

    static class FieldDeclarator
    {
        Declarator declarator;
        ExprStmt bitFieldSize;

        FieldDeclarator(DeclSpec ds)
        {
            declarator = new Declarator(ds, TheContext.StructFieldContext);
            bitFieldSize = null;
        }
    }

    public static class SourceRange
    {
        private int start;
        private int end;

        public SourceRange()
        {
            start = Position.NOPOS;
            end = Position.NOPOS;
        }

        public SourceRange(int start, int end)
        {
            assert start != Position.NOPOS;
            assert end != Position.NOPOS;
            this.start = start;
            this.end = end;
        }

        public int getStart() { return start; }
        public int getEnd() { return end; }

        public void setStart(int start)
        {
            assert start != Position.NOPOS;
            this.start = start;
        }
        public void setEnd(int end)
        {
            assert end != Position.NOPOS;
            this.end = end;
        }
        public boolean isValid()
        {
            return start != Position.NOPOS && end != Position.NOPOS;
        }
        public boolean isInvalid()
        {
            return !isValid();
        }
    }
    /**
     * Storage class specifiers.
     */
    public enum SCS
    {
        SCS_unspecified("unspecified"),
        SCS_typedef("typedef"),
        SCS_extern("extern"),
        SCS_static("static"),
        SCS_auto("auto"),
        SCS_register("register");

        String name;

        SCS(String name)
        {
            this.name = name;
        }
    }


    /**
     * Type specifier width, like short, long, long long.
     */
    public enum TSW
    {
        TSW_unspecified("unspecified"),
        TSW_short("short"),
        TSW_long("long"),
        TSW_longlong("long long");

        String name;

        TSW(String name)
        {
            this.name = name;
        }
    }

    /**
     * Type Specifier complex.
     */
    public enum TSC
    {
        TSC_unspecified("unspecified"),
        TSC_imaginary("_Imaginary"),
        TSC_complex("_Complex");

        String name;
        TSC(String name)
        {
            this.name = name;
        }
    }

    /**
     * Type Specifier Sign
     */
    public enum TSS
    {
        TSS_unspecified("unspecified"),
        TSS_signed("signed"),
        TSS_unsigned("unsigned");

        String name;
        TSS(String name)
        {
            this.name = name;
        }
    }

    /**
     * Type specifier type.
     */
    public enum TST
    {
        TST_unspecified("unspecified"),
        TST_void("void"),
        TST_char("char"),
        TST_int("int"),
        TST_float("float"),
        TST_double("double"),
        TST_bool("_Bool"),
        TST_enum("enum"),
        TST_union("union"),
        TST_struct("struct"),
        TST_typename("type-name"),
        TST_error("error");

        String name;
        TST(String name)
        {
            this.name = name;
        }
    }

    /**
     * Type Qualifier.
     */
    public enum TQ
    {
        TQ_unspecified("unspecified", 0),
        TQ_const("const", 1),
        TQ_restrict("restrict", 2),
        TQ_volatile("volatile", 4);

        String name;
        public int value;
        TQ(String name, int value)
        {
            this.name = name;
            this.value = value;
        }
    }

    static class ParsedSpecifiers
    {
        static final int PQ_none = 0;
        static final int PQ_StorageClassSpecifier = 1;
        static final int PQ_TypeSpeciifer = 2;
        static final int PQ_TypeQualifier = 4;
        static final int PQ_FunctionSpecifier = 8;
    }
    // storage-class-specifier
    private SCS storageClassSpec;
    // type-specifier
    private TSW typeSpecWidth;
    private TSC typeSpecComplex;
    private TSS typeSpecSign;
    private TST typeSpecType;

    // type-qualifier
    private int typeQualifier;

    // function-specifier
    private boolean inlineSpecifier;

    boolean typeSpecOwned;

    private Decl decl;

    /**
     * Following three variables are stored in a union in Clang.
     */
    private QualType typeRep;
    private Decl declRep;
    private ExprStmt exprRep;

    private int storageClassLoc = Position.NOPOS,
            TSWLoc = Position.NOPOS,
            TSTLoc = Position.NOPOS,
            TSTNameLoc = Position.NOPOS,
            TSSLoc = Position.NOPOS,
            TSCLoc = Position.NOPOS,
            TQ_constLoc = Position.NOPOS,
            TQ_restrictLoc = Position.NOPOS,
            TQ_volatileLoc = Position.NOPOS,
            ISLoc = Position.NOPOS;

    private SourceRange sourceRagne;

    public DeclSpec()
    {
        storageClassSpec = SCS.SCS_unspecified;
        typeSpecWidth  = TSW_unspecified;
        typeSpecComplex = TSC_unspecified;
        typeSpecSign = TSS_unspecified;
        typeSpecType = TST_unspecified;

        typeQualifier = TQ_unspecified.ordinal();
        inlineSpecifier = false;

        sourceRagne = new SourceRange();
    }

    public SourceRange getSourceRange()
    {
        return sourceRagne;
    }

    public void setRangeStart(int loc)
    {
        assert loc != Position.NOPOS;
        sourceRagne.setStart(loc);
    }

    public void setRangeEnd(int loc)
    {
        assert loc != Position.NOPOS;
        sourceRagne.setEnd(loc);
    }

    public int getRangeStart() { return sourceRagne.start; }
    public int getRangeEnd() { return sourceRagne.end; }

    public SCS getStorageClassSpec() { return storageClassSpec; }
    public void clearStorageClassSpec()
    {
        storageClassSpec = SCS.SCS_unspecified;
    }

    // type-specifier
    public TSW getTypeSpecWidth() { return typeSpecWidth; }
    public TSC getTypeSpecComplex() { return typeSpecComplex; }
    public TSS getTypeSpecSign() { return typeSpecSign; }
    public TST getTypeSpecType() { return typeSpecType; }

    public static String getSpecifierName(TST var)
    {
        return var.name;
    }

    public static String getSpecifierName(TQ var)
    {
        return var.name;
    }

    public static String getSpecifierName(TSS var)
    {
        return var.name;
    }

    public static String getSpecifierName(TSC var)
    {
        return var.name;
    }

    public static String getSpecifierName(TSW var)
    {
        return var.name;
    }

    public static String getSpecifierName(SCS var)
    {
        return var.name;
    }

    // type-qualifier
    public int getTypeQualifier(){ return typeQualifier; }

    public void clearTypeQualifier()
    {
        typeQualifier = 0;
    }

    public boolean isInlineSpecifier() { return inlineSpecifier; }

    public void clearFunctionSpecifier()
    {
        inlineSpecifier = false;
    }

    public int getParsedSpecifiers()
    {
        int res = 0;
        if (storageClassSpec != SCS_unspecified)
            res |= PQ_StorageClassSpecifier;
        if (typeQualifier != 0)
            res |= PQ_TypeQualifier;
        if (hasTypeSpecifier())
            res |= PQ_TypeSpeciifer;
        if (inlineSpecifier)
            res = PQ_FunctionSpecifier;

        return res;
    }

    public boolean hasTypeSpecifier()
    {
        return getTypeSpecType() != TST_unspecified
                || getTypeSpecWidth() != TSW_unspecified
                || getTypeSpecComplex() != TSC_unspecified
                || getTypeSpecSign() != TSS_unspecified;
    }

    public boolean isEmpty()
    {
        return getParsedSpecifiers() != PQ_none;
    }

    private <T> boolean badSpecifier(T newSCS, T prevSCS/*, String prevSpec, */)
    {
        //prevSpec = getSpecifierName(prevSCS);
        return true;
    }
    /**
     * Set the storage-class-specifier of the DeclSpec and return false if
     * there was no error. IfStmt an error occurs (for example, if we
     * tried to set "auto" on a spec with "extern" already set), they return true and
     * set PrevSpec and DiagID such that:
     * <pre>
     *     Diag(loc, diagID)<<PrevSpec;
     * </pre>
     * will yield a useful result.
     * @param val
     * @return
     */
    public boolean setStorageClassSpec(SCS val, int loc)
    {
        if (storageClassSpec != SCS_unspecified)
        {
            if (!(storageClassSpec == SCS_extern
            && val == SCS_extern))
            {
                return badSpecifier(val, storageClassSpec);
            }
        }
        storageClassSpec = val;
        storageClassLoc = loc;
        return false;
    }

    public boolean setTypeSpecWidth(TSW val, int loc)
    {
        if (typeSpecWidth == TSW_unspecified)
            TSWLoc = loc;

        else if (val != TSW_longlong || typeSpecWidth != TSW_long)
        {
            return badSpecifier(val, typeSpecWidth);
        }
        typeSpecWidth = val;
        return false;
    }

    public boolean setTypeSpecComplex(TSC val, int loc)
    {
        if (typeSpecComplex != TSC_unspecified)
            return badSpecifier(val, typeSpecComplex);
        typeSpecComplex = val;
        TSCLoc = loc;
        return false;
    }

    public boolean setTypeSpecSign(TSS val, int loc)
    {
        if (typeSpecSign != TSS_unspecified)
            return badSpecifier(val, typeSpecSign);
        typeSpecSign = val;
        TSSLoc = loc;
        return false;
    }

    public boolean setTypeSpecType(TST val, int loc,
            OutParamWrapper<String> prevDecl,
            OutParamWrapper<String> diag,
            QualType ty)
    {
        return setTypeSpecType(val, loc, loc, prevDecl, diag, ty);
    }

    public boolean setTypeSpecType(TST val,
            int tagKwLoc,
            int tagNameLoc,
            OutParamWrapper<String> prevDecl,
            OutParamWrapper<String> diag,
            Decl rep)
    {
        assert isDeclRep(val):"T does not store a decl";

        if (typeSpecType != TST_unspecified)
        {
            prevDecl.set(getSpecifierName(val));
            diag.set("cannot combine with previous '" + prevDecl
                    +"' declaration specifier");
            return true;
        }

        typeSpecType = val;
        declRep = rep;
        TSTLoc = tagKwLoc;
        TSTNameLoc = tagNameLoc;
        return false;
    }

    public boolean setTypeSpecType(TST val, int loc,
            OutParamWrapper<String> prevDecl,
            OutParamWrapper<String> diag)
    {
        assert !isDeclRep(val) && !isTypeRep(val)
                :"rep required for these type-spec kinds!";
        if (typeSpecType  != TST_unspecified)
        {
            prevDecl.set(getSpecifierName(typeSpecType));
            diag.set("cannot combine with previous '" + prevDecl
                    +"' declaration specifier");
            return true;
        }
        TSTLoc = loc;
        TSTNameLoc = loc;
        typeSpecType = val;
        return false;
    }

    public boolean setTypeSpecType(TST val, int tagKwLoc, int tagNameLoc,
            OutParamWrapper<String> prevDecl,
            OutParamWrapper<String> diag, QualType ty)
    {
        assert isTypeRep(val):"T does not store a type";
        assert ty != null:"no type provided!";

        if (typeSpecType != TST_unspecified)
        {
            prevDecl.set(getSpecifierName(typeSpecType));
            diag.set("cannot combine with previous '" + prevDecl
                    +"' declaration specifier");
            return true;
        }
        typeSpecType = val;
        typeRep = ty;
        TSTLoc = tagKwLoc;
        TSTNameLoc = tagNameLoc;
        return false;
    }

    public boolean setTypeSpecError()
    {
        typeSpecType = TST_error;
        TSTLoc = Position.NOPOS;
        return false;
    }

    public boolean setTypeQualifier(TQ val, int loc)
    {
        if ((typeQualifier & val.value) != 0)
            return badSpecifier(val, typeQualifier);

        typeQualifier |= val.value;
        switch (val)
        {
            default:
                break;
            case TQ_const:
                TQ_constLoc = loc;
                break;
            case TQ_restrict:
                TQ_restrictLoc = loc;
                break;
            case TQ_volatile:
                TQ_volatileLoc = loc;
                break;
        }
        return false;
    }

    public boolean setFunctionSpecInline(int loc)
    {
        inlineSpecifier = true;
        ISLoc = loc;
        return false;
    }

    /**
     * This function does final analysis of declaration-specifiers, rejecting
     * some cases which not can not conform C99 standard and issue some error
     * or warning diagnostic messages.
     */
    public void finish(Parser p)
    {
        // signed/unsigned are only valid with int/char/
        if (typeSpecSign != TSS_unspecified)
        {
            if (typeSpecType == TST_unspecified)
                typeSpecType = TST_int; // unsigned -> unsigned int, signed -> int.
            else if (typeSpecType != TST_int
                    && typeSpecType != TST_char)
            {
                p.syntaxError(TSSLoc, "'%s' cannot be signed or unsigned",
                        getSpecifierName(typeSpecType));
                typeSpecSign = TSS_unspecified;
            }
        }

        // Validate the width of the type.
        switch (typeSpecWidth)
        {
            case TSW_unspecified:break;
            case TSW_short: // short int
            case TSW_longlong:
                if (typeSpecType == TST_unspecified)
                    typeSpecType = TST_int; // short -> short int;
                else if (typeSpecType != TST_int)
                {
                    p.syntaxError(TSWLoc, typeSpecWidth == TSW_short
                                    ? "'short %s' is invalid":
                                    "'long long %s' is invalid",
                            getSpecifierName(typeSpecType));
                    typeSpecType = TST_int;
                    typeSpecOwned = false;
                }
                break;
            case TSW_long:
                    // long int, long double
                if (typeSpecType == TST_unspecified)
                    typeSpecType = TST_int; // long -> long int
                else if (typeSpecType != TST_int && typeSpecType != TST_double)
                {
                    p.syntaxError(TSWLoc, "'long long %s' is invalid",
                            getSpecifierName(typeSpecType));
                    typeSpecType = TST_int;
                    typeSpecOwned = false;
                }
                break;
        }

        // TODO: if the implementation does not implement _Complex or _Imaginary,
        // disallow their use.  Need information about the backend.
        if (typeSpecComplex != TSC_unspecified)
        {
            if (typeSpecType != TST_unspecified)
            {
                p.syntaxError(TSCLoc, "plain '_Complex' requires a type specifier; assuming '_Complex double'");
                typeSpecType = TST_double; // _Complex -> _Complex double.
            }
            else if (typeSpecType == TST_int || typeSpecType == TST_char)
            {
                // Note that this intentionally doesn't include _Complex _Bool
                p.syntaxError(TSTLoc, "complex integer types are a GNU extension");
            }
            else if (typeSpecType!=TST_float && typeSpecType != TST_double)
            {
                p.syntaxError(TSCLoc, "'_Complex %s' is invalid",
                        getSpecifierName(typeSpecType));
                typeSpecComplex = TSC_unspecified;
            }
        }

        assert !typeSpecOwned || isDeclRep(typeSpecType);

        // Okay, now we can infer the real type.
        // 'data definition has no type or storage class'?
    }

    public int getConstSpecLoc()
    {
        return TQ_constLoc;
    }

    public int getVolatileSpecLoc()
    {
        return TQ_volatileLoc;
    }
    public int getRestrictSpecLoc()
    {
        return TQ_restrictLoc;
    }

    public int getInlineSpecLoc()
    {
        return ISLoc;
    }

    public static boolean isTypeRep(TST t)
    {
        return t == TST_typename;
    }

    public static boolean isDeclRep(TST t)
    {
        return (t == TST_enum || t == TST_struct
                || t == TST_union);
    }
    public Decl getRepAsDecl()
    {
        assert isDeclRep(typeSpecType) :"DeclSpec does not stores a decl";
        return declRep;
    }
    public QualType getRepAsType()
    {
        assert isDeclRep(typeSpecType) :"DeclSpec does not stores a type";
        return typeRep;
    }

    public boolean isMissingDeclaratorOk()
    {
        TST tst = getTypeSpecType();
        return isDeclRep(tst) && getRepAsDecl()!= null
                && storageClassSpec != SCS_typedef;
    }

    public int getTypeSpecWidthLoc()
    {
        return TSWLoc;
    }

    public int getStorageClassSpecLoc()
    {
        return storageClassLoc;
    }

    public boolean isTypeSpecOwned()
    {
        return typeSpecOwned;
    }
}
