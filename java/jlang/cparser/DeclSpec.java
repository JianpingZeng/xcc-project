package jlang.cparser;

import tools.Util;
import jlang.ast.Tree;
import jlang.ast.Tree.Expr;
import jlang.basic.SourceManager;
import jlang.support.SourceRange;
import jlang.cparser.Declarator.TheContext;
import jlang.clex.IdentifierInfo;
import jlang.clex.Preprocessor;
import jlang.support.SourceLocation;
import jlang.diag.*;
import jlang.sema.Decl;
import jlang.type.QualType;
import tools.OutRef;

import java.util.List;

import static jlang.cparser.DeclSpec.ParsedSpecifiers.*;
import static jlang.cparser.DeclSpec.SCS.*;
import static jlang.cparser.DeclSpec.TQ.*;
import static jlang.cparser.DeclSpec.TSC.*;
import static jlang.cparser.DeclSpec.TSW.*;
import static jlang.cparser.DeclSpec.TSS.*;
import static jlang.cparser.DeclSpec.TST.*;

/**
 * This class captures information about "declaration specifiers",
 * which encompasses storage-class-specifiers, jlang.type-specifiers,
 * jlang.type-qualifiers, and function-specifiers.
 *
 * @author Jianping Zeng
 * @version 0.1
 */
public class DeclSpec implements DiagnosticSemaTag, DiagnosticParseTag
{
    public static class DeclaratorChunk<T>
    {
        public enum ChunkKind
        {
            Pointer,
            Array,
            Function,
            Paren
        }

        ChunkKind kind;
        /**
         * The place where this jlang.type was defined.
         */
        SourceLocation loc;
        /**
         * IfStmt valid, the place where this chunk ends.
         */
        SourceLocation endLoc;
        /**
         * This hold the reference to concrete TypeInfo, e.g. PointerTypeInfo,
         * FunctionTypeInfo, ArrayTypeInfo.
         */
        public T typeInfo;

        public static class PointerTypeInfo
        {
            // the type qualifiers : const/volatile/rstrict
            public int typeQuals;
            // the location of the const-qualifier, if any
            public SourceLocation constQualLoc;
            // the location of the volatile-qualifier, if any
            public SourceLocation volatileQualLoc;
            // the location of the restrict-qualifier, if any
            public SourceLocation restrictQualLoc;
            public AttributeList attrList;
        }

        public static class ArrayTypeInfo
        {
            // the jlang.type qualifiers : const/volatile/rstrict
            public int typeQuals;
            // true if this dimension included the 'static' kwyword.
            public boolean hasStatic;
            // true if this dimension was [*].
            public boolean isStar;
            // This is the getTypeSize of the array, or null if [] or [*] was specified.
            public Tree.Expr numElts;
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
             * the jlang.type qualifiers : const/volatile/rstrict
             */
            public int typeQuals;

            /**
             * When isVariadic is true, the location of the ellipsis in the source.
             */
            public SourceLocation ellipsisLoc;
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

            SourceLocation getEllipsisLoc() { return ellipsisLoc; }
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
            getPointer(int typeQuals,
                SourceLocation loc,
                SourceLocation constQualLoc,
                SourceLocation volatileQualLoc,
                SourceLocation restrictQualLoc,
                AttributeList alist)
        {
            DeclaratorChunk<PointerTypeInfo> res = new DeclaratorChunk<>();
            res.loc = loc;
            res.kind = ChunkKind.Pointer;
            res.typeInfo = new PointerTypeInfo();
            res.typeInfo.typeQuals = typeQuals;
            res.typeInfo.constQualLoc = constQualLoc;
            res.typeInfo.volatileQualLoc = volatileQualLoc;
            res.typeInfo.restrictQualLoc = restrictQualLoc;
            res.typeInfo.attrList = alist;
            return res;
        }

        public static DeclaratorChunk
                getArray(int typeQuals,
                boolean isStatic,
                boolean isStar,
                Tree.Expr numElts,
                SourceLocation lBracketLoc,
                SourceLocation rBracketLoc)
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
                SourceLocation ellipsisLoc,
                List<ParamInfo> argInfo,
                int typeQuals,
                SourceLocation rangeBegin,
                SourceLocation rangeEnd)
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
            res.typeInfo.numArgs = argInfo != null ? argInfo.size(): 0;
            res.typeInfo.returnType = null;

            return res;
        }

        public ChunkKind getKind()
        {
            return kind;
        }

        public SourceLocation getLocation()
        {
            return loc;
        }

        public SourceLocation getEndLocation()
        {
            return endLoc;
        }

        public AttributeList getAttrs()
        {
            switch (kind)
            {
                default:
                    Util.assertion(false, "unknown declarator kind");
                case Pointer:
                    return ((PointerTypeInfo)typeInfo).attrList;
                case Array:
                case Function:
                    return null;
            }
        }
    }

    public static class ParamInfo
    {
        public IdentifierInfo ident;
        public SourceLocation identLoc;
        public Decl param;

        public ParamInfo(IdentifierInfo ID, SourceLocation identLoc, Decl param)
        {
            this.ident = ID;
            this.identLoc = identLoc;
            this.param = param;
        }
    }

    static class FieldDeclarator
    {
        Declarator declarator;
        Tree.Expr bitFieldSize;

        FieldDeclarator(DeclSpec ds)
        {
            declarator = new Declarator(ds, TheContext.StructFieldContext);
            bitFieldSize = null;
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
     * Type specifier jlang.type.
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
        TST_typename("jlang.type-getIdentifier"),
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
    // jlang.type-specifier
    private TSW typeSpecWidth;
    private TSC typeSpecComplex;
    private TSS typeSpecSign;
    private TST typeSpecType;

    // jlang.type-qualifier
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
    private Expr exprRep;

    /**
     * Attributes list.
     */
    private AttributeList attrList;

    private SourceLocation storageClassLoc = SourceLocation.NOPOS,
            TSWLoc = SourceLocation.NOPOS,
            TSTLoc = SourceLocation.NOPOS,
            TSTNameLoc = SourceLocation.NOPOS,
            TSSLoc = SourceLocation.NOPOS,
            TSCLoc = SourceLocation.NOPOS,
            TQ_constLoc = SourceLocation.NOPOS,
            TQ_restrictLoc = SourceLocation.NOPOS,
            TQ_volatileLoc = SourceLocation.NOPOS,
            ISLoc = SourceLocation.NOPOS;

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

    public void setRangeStart(SourceLocation loc)
    {
        sourceRagne.setBegin(loc);
    }

    public void setRangeEnd(SourceLocation loc)
    {
        sourceRagne.setEnd(loc);
    }

    public SourceLocation getRangeStart()
    {
        return sourceRagne.getBegin();
    }
    public SourceLocation getRangeEnd()
    {
        return sourceRagne.getEnd();
    }

    public SCS getStorageClassSpec() { return storageClassSpec; }
    public void clearStorageClassSpec()
    {
        storageClassSpec = SCS.SCS_unspecified;
    }

    // jlang.type-specifier
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

    // jlang.type-qualifier
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
        return getParsedSpecifiers() == PQ_none;
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
     *     Diag(identLoc, diagID)<<PrevSpec;
     * </pre>
     * will yield a useful result.
     * @param val
     * @return
     */
    public boolean setStorageClassSpec(SCS val, SourceLocation loc)
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

    public boolean setTypeSpecWidth(TSW val, SourceLocation loc)
    {
        if (typeSpecWidth == TSW_unspecified)
            TSWLoc = loc;

        // Turn long long into long and combines proceeding long into 'long long'
        else if (val != TSW_longlong || typeSpecWidth != TSW_long)
        {
            return badSpecifier(val, typeSpecWidth);
        }
        typeSpecWidth = val;
        return false;
    }

    public boolean setTypeSpecComplex(TSC val, SourceLocation loc)
    {
        if (typeSpecComplex != TSC_unspecified)
            return badSpecifier(val, typeSpecComplex);
        typeSpecComplex = val;
        TSCLoc = loc;
        return false;
    }

    public boolean setTypeSpecSign(TSS val, SourceLocation loc)
    {
        if (typeSpecSign != TSS_unspecified)
            return badSpecifier(val, typeSpecSign);
        typeSpecSign = val;
        TSSLoc = loc;
        return false;
    }

    public boolean setTypeSpecType(TST val,
            SourceLocation loc,
            OutRef<String> prevDecl,
            OutRef<Integer> diag,
            QualType ty)
    {
        return setTypeSpecType(val, loc, loc, prevDecl, diag, ty);
    }

    public boolean setTypeSpecType(TST val,
            SourceLocation tagKwLoc,
            SourceLocation tagNameLoc,
            OutRef<String> prevDecl,
            OutRef<Integer> diag,
            Decl rep)
    {
        Util.assertion(isDeclRep(val), "T does not store a decl");

        if (typeSpecType != TST_unspecified)
        {
            prevDecl.set(getSpecifierName(val));
            diag.set(err_invalid_decl_spec_combination);
            return true;
        }

        typeSpecType = val;
        declRep = rep;
        TSTLoc = tagKwLoc;
        TSTNameLoc = tagNameLoc;
        return false;
    }

    public boolean setTypeSpecType(TST val,
            SourceLocation loc,
            OutRef<String> prevDecl,
            OutRef<Integer> diagID)
    {
        Util.assertion(!isDeclRep(val) && !isTypeRep(val), "rep required for these jlang.type-spec kinds!");

        if (typeSpecType  != TST_unspecified)
        {
            prevDecl.set(getSpecifierName(typeSpecType));
            diagID.set(err_invalid_decl_spec_combination);
            return true;
        }
        TSTLoc = loc;
        TSTNameLoc = loc;
        typeSpecType = val;
        return false;
    }

    public boolean setTypeSpecType(TST val,
            SourceLocation tagKwLoc,
            SourceLocation tagNameLoc,
            OutRef<String> prevDecl,
            OutRef<Integer> diag,
            QualType ty)
    {
        Util.assertion(isTypeRep(val), "T does not store a jlang.type");
        Util.assertion(ty != null, "no jlang.type provided!");

        if (typeSpecType != TST_unspecified)
        {
            prevDecl.set(getSpecifierName(typeSpecType));
            diag.set(err_invalid_decl_spec_combination);
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
        TSTLoc = SourceLocation.NOPOS;
        return false;
    }

    public boolean setTypeQualifier(TQ val, SourceLocation loc)
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

    public boolean setFunctionSpecInline(SourceLocation loc)
    {
        inlineSpecifier = true;
        ISLoc = loc;
        return false;
    }

    static Diagnostic.DiagnosticBuilder diag(
            Diagnostic diags,
            SourceLocation loc,
            SourceManager mgr,
            int diagID)
    {
        return diags.report(new FullSourceLoc(loc, mgr), diagID);
    }

    /**
     * This function does final analysis of declaration-specifiers, rejecting
     * some cases which not can not conform C99 standard and issue some error
     * or warning diagnostic messages.
     */
    public void finish(Diagnostic diags, Preprocessor pp)
    {
        SourceManager sourceMgr = pp.getSourceManager();

        // signed/unsigned are only valid with int/char/
        if (typeSpecSign != TSS_unspecified)
        {
            if (typeSpecType == TST_unspecified)
                typeSpecType = TST_int; // unsigned -> unsigned int, signed -> int.
            else if (typeSpecType != TST_int
                    && typeSpecType != TST_char)
            {
                diag(diags, TSSLoc, sourceMgr, err_invalid_sign_spec)
                    .addTaggedVal(getSpecifierName(typeSpecType));
                // signed double -> double.
                // signed float -> float.
                typeSpecSign = TSS_unspecified;
            }
        }

        // Validate the width of the type.
        switch (typeSpecWidth)
        {
            case TSW_unspecified:
                break;
            case TSW_short: // short int
            case TSW_longlong:
            {
                if (typeSpecType == TST_unspecified)
                    typeSpecType = TST_int; // short -> short int;
                else if (typeSpecType != TST_int)
                {
                    diag(diags, TSWLoc, sourceMgr, typeSpecWidth == TSW_short ?
                            err_invalid_short_spec :
                            err_invalid_longlong_spec).
                            addTaggedVal(getSpecifierName(typeSpecType));
                    typeSpecType = TST_int;
                    typeSpecOwned = false;
                }
                break;
            }
            case TSW_long:
            {
                // long int, long double
                if (typeSpecType == TST_unspecified)
                    typeSpecType = TST_int; // long -> long int
                else if (typeSpecType != TST_int && typeSpecType != TST_double)
                {
                    diag(diags, TSWLoc, sourceMgr, err_invalid_long_spec).addTaggedVal(getSpecifierName(typeSpecType));
                    typeSpecType = TST_int;
                    typeSpecOwned = false;
                }
                break;
            }
        }

        // TODO: if the implementation does not implement _Complex or _Imaginary,
        // disallow their use.  Need information about the backend.
        if (typeSpecComplex != TSC_unspecified)
        {
            if (typeSpecType != TST_unspecified)
            {
                diag(diags, TSCLoc, sourceMgr, ext_plain_complex)
                .addFixItHint(FixItHint.createInsertion(
                   TSCLoc, " double"));
                typeSpecType = TST_double; // _Complex -> _Complex double.
            }
            else if (typeSpecType == TST_int || typeSpecType == TST_char)
            {
                // Note that this intentionally doesn't include _Complex _Bool
                diag(diags, TSTLoc, sourceMgr, ext_integer_complex);
            }
            else if (typeSpecType!=TST_float && typeSpecType != TST_double)
            {
                diag(diags, TSCLoc, sourceMgr, err_invalid_complex_spec)
                .addTaggedVal(getSpecifierName(typeSpecType));
                typeSpecComplex = TSC_unspecified;
            }
        }

        Util.assertion( !typeSpecOwned || isDeclRep(typeSpecType));

        // Okay, now we can infer the real jlang.type.
        // 'data definition has no jlang.type or storage class'?
    }

    public SourceLocation getConstSpecLoc()
    {
        return TQ_constLoc;
    }

    public SourceLocation getVolatileSpecLoc()
    {
        return TQ_volatileLoc;
    }
    public SourceLocation getRestrictSpecLoc()
    {
        return TQ_restrictLoc;
    }

    public SourceLocation getInlineSpecLoc()
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
        Util.assertion(isDeclRep(typeSpecType), "DeclSpec does not stores a decl");
        return declRep;
    }
    public QualType getRepAsType()
    {
        Util.assertion(isTypeRep(typeSpecType), "DeclSpec does not stores a jlang.type");
        return typeRep;
    }

    public boolean isMissingDeclaratorOk()
    {
        TST tst = getTypeSpecType();
        return isDeclRep(tst) && getRepAsDecl()!= null
                && storageClassSpec != SCS_typedef;
    }

    public SourceLocation getTypeSpecWidthLoc()
    {
        return TSWLoc;
    }

    public SourceLocation getStorageClassSpecLoc()
    {
        return storageClassLoc;
    }

    public boolean isTypeSpecOwned()
    {
        return typeSpecOwned;
    }

    /**
     * contatenates two attribute lists.
     /// The GCC attribute syntax allows for the following:
     ///
     /// short __attribute__(( unused, deprecated ))
     /// int __attribute__(( may_alias, aligned(16) )) var;
     ///
     /// This declares 4 attributes using 2 lists. The following syntax is
     /// also allowed and equivalent to the previous declaration.
     ///
     /// short __attribute__((unused)) __attribute__((deprecated))
     /// int __attribute__((may_alias)) __attribute__((aligned(16))) var;
     * @param alist
     */
    public void addAttributes(AttributeList alist)
    {
        if (alist == null)
            return;
        if (attrList != null)
            alist.addAttributeList(attrList);
        attrList = alist;
    }

    public AttributeList getAttributes()
    {
        return attrList;
    }
}
