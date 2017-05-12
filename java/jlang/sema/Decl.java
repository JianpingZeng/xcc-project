package jlang.sema;

/**
 * @author Xlous.zeng
 * @version 0.1
 */

import jlang.ast.Tree;
import jlang.ast.Tree.Expr;
import jlang.ast.Tree.LabelledStmt;
import jlang.basic.Linkage;
import jlang.basic.SourceLocation;
import jlang.clex.IdentifierInfo;
import jlang.cparser.DeclContext;
import jlang.cparser.DeclKind;
import jlang.cparser.Declarator;
import jlang.type.QualType;
import jlang.type.RecordType;
import jlang.type.Type;

import java.util.ArrayList;
import java.util.List;

import static jlang.cparser.DeclKind.*;
import static jlang.type.Type.TagTypeKind.*;

/**
 * This class encapsulate information of declaration or definition, like
 * a variable, label, typedef, function, struct, enum, field, enum constant.
 */
public abstract class Decl extends DeclContext
{
    public enum StorageClass
    {
        SC_none,
        // for FunctionProto
        SC_static,
        SC_extern,

        // for variable
        SC_auto,
        SC_register
    }

    private SourceLocation location;
    private DeclKind kind;
    private boolean invalidDecl;
    private DeclContext context;
    private boolean isUsed;
    private IdentifierNamespace IDNS;

    private boolean implicit;

    Decl(DeclKind kind, DeclContext context, SourceLocation location)
    {
        super(kind);
        this.location = location;
        this.context = context;
        IDNS = getIdentifierNamespaceForKind(kind);
    }

    public DeclKind getDeclKind()
    {
        return kind;
    }

    public DeclContext getDeclContext()
    {
        return context;
    }

    public void setInvalidDecl(boolean invalidDecl)
    {
        this.invalidDecl = invalidDecl;
    }

    public boolean isInvalidDecl() { return invalidDecl; }

    public SourceLocation getLocation()
    {
        return location;
    }

    public boolean isUsed()
    {
        return isUsed;
    }

    public void setUsed()
    {
        isUsed = true;
    }

    public static IdentifierNamespace
        getIdentifierNamespaceForKind(DeclKind kind)
    {
        switch (kind)
        {
            case FunctionDecl:
            case EnumConstant:
            case VarDecl:
            case TypedefDecl:
                return IdentifierNamespace.IDNS_Ordinary;
            case StructDecl:
            case EnumDecl:
                return IdentifierNamespace.IDNS_Tag;
            case LabelDecl:
                return IdentifierNamespace.IDNS_Label;
            case FieldDecl:
                return IdentifierNamespace.IDNS_Member;
            default:
                return null;
        }
    }

    public IdentifierNamespace getIdentifierNamespace()
    {
        return IDNS;
    }

    public boolean isSameInIdentifierNameSpace(IdentifierNamespace ns)
    {
        return IDNS != ns;
    }

    public boolean hasTagIdentifierNamespace()
    {
        return isTagIdentifierNamespace(IDNS);
    }

    public static boolean isTagIdentifierNamespace(IdentifierNamespace ns)
    {
        return ns == IdentifierNamespace.IDNS_Tag;
    }

    public void setDeclContext(DeclContext dc)
    {
        context = dc;
    }

    public void setImplicit(boolean implicit)
    {
        this.implicit = implicit;
    }

    public boolean isImplicit()
    {
        return implicit;
    }

    public static class TranslationUnitDecl extends Decl
    {
        public TranslationUnitDecl(DeclContext context, SourceLocation location)
        {
            super(DeclKind.CompilationUnitDecl, context, location);
        }

        public TranslationUnitDecl(DeclContext context)
        {
            super(DeclKind.CompilationUnitDecl, context, SourceLocation.NOPOS);
        }
    }

    /**
     * This class represents a declaration with a getIdentifier, for example,
     * {@linkplain VarDecl}, {@linkplain LabelDecl} and {@linkplain TypeDecl}.
     */
    public static class NamedDecl extends Decl
    {
        IdentifierInfo name;

        NamedDecl(DeclKind kind, DeclContext context, 
                IdentifierInfo name, SourceLocation location)
        {
            super(kind, context, location);
            this.name = name;
        }

        public IdentifierInfo getDeclName()
        {
            return name;
        }

        public boolean hasLinkage()
        {
            if (this instanceof VarDecl)
            {
                VarDecl vd = (VarDecl)this;
                return vd.hasExternalStorage() || vd.isFileVarDecl();
            }

            return (this instanceof FunctionDecl);
        }

        public String getIdentifier()
        {
            return name.getName();
        }

        public Linkage getLinkage()
        {
            LinkageInfo info = getLVForDecl(this, LVFlags.createOnlyDeclLinkage());
            return info.getLinkage();
        }

        static LinkageInfo getLVForDecl(NamedDecl decl, LVFlags flags)
        {
            if (decl.getDeclContext().isFileContext())
            {
                if (decl instanceof VarDecl)
                {
                    Decl.VarDecl var = (Decl.VarDecl)decl;
                    // Explicitly declared static.
                    if (var.getStorageClass() == StorageClass.SC_static)
                        return LinkageInfo.internal();

                    if (var.getStorageClass() == StorageClass.SC_none)
                        return LinkageInfo.external();

                    if (var.getStorageClass() == StorageClass.SC_extern)
                        return LinkageInfo.external();
                }
                else if (decl instanceof FunctionDecl)
                {
                    Decl.FunctionDecl fnDecl = (Decl.FunctionDecl)decl;

                    // Explicitly declared static.
                    if (fnDecl.getStorageClass() == StorageClass.SC_static)
                        return LinkageInfo.internal();

                    return LinkageInfo.external();
                }
                else if (decl instanceof FieldDecl)
                {
                    //   - a data member of an anonymous union.
                    Decl.FieldDecl field = (Decl.FieldDecl)decl;
                    if (((RecordDecl)field.getDeclContext()).isAnonymousStructOrUnion())
                        return LinkageInfo.internal();
                }


                // Set up the defaults.

                // C99 6.2.2p5:
                //   If the declaration of an identifier for an object has file
                //   scope and no storage-class specifier, its linkage is
                //   external.
                LinkageInfo lv = new LinkageInfo();
                return lv;
            }
            if (decl.getDeclContext().isFunction())
            {
                if (decl instanceof VarDecl)
                {
                    Decl.VarDecl var = (Decl.VarDecl)decl;
                    if (var.getStorageClass() == StorageClass.SC_extern)
                        return LinkageInfo.uniqueExternal();

                    return new LinkageInfo();
                }
            }
            return LinkageInfo.none();
        }
    }

    /**
     * Represents a labelled statement in source code.
     */
    public static class LabelDecl extends NamedDecl
    {
        LabelledStmt stmt;
        LabelDecl(IdentifierInfo name, DeclContext context,
                LabelledStmt stmt, SourceLocation location)
        {
            super(DeclKind.LabelDecl, context, name, location);
            this.stmt = stmt;
        }

        public void setStmt(LabelledStmt stmt)
        {
            this.stmt = stmt;
        }
    }

    /**
     * Represents the declaration of a variable (in which case it is an lvalue)
     * or a function (in which case it is a function designator) or an enum constant.
     */
    public static class ValueDecl extends NamedDecl
    {
        private QualType declType;

        ValueDecl(DeclKind kind,
                DeclContext context,
                IdentifierInfo name,
                SourceLocation location,
                QualType type)
        {
            super(kind, context, name, location);
            this.declType = type;
        }

        public QualType getDeclType() { return declType; }
        public void setDeclType(QualType type) {declType = type; }
    }

    /**
     * Represents a ValueDecl that came out of a declarator.
     */
    public static abstract class DeclaratorDecl extends ValueDecl
    {
        DeclaratorDecl(DeclKind kind,
                DeclContext context,
                IdentifierInfo name,
                SourceLocation location,
                QualType type)
        {
            super(kind, context, name, location, type);
        }
    }

    /**
     * This class is created by {@linkplain Sema#actOnField(Scope, Decl,
     * SourceLocation, Declarator, Expr)}
     * to represents a member of a struct or union.
     */
    public static class FieldDecl extends DeclaratorDecl
    {
        /**
         * It indicates the initializer of member of a struct/union or bitfield
         * expression.
         */
        private Tree.Expr init;
        /**
         * Indicates the {@linkplain #init} is an initializer or a bitfield.
         */
        private boolean hasInit;

        /**
         * A cached index into this field in corresponding record decl.
         */
        private int cachedFieldIndex;

        FieldDecl(DeclContext context,
                IdentifierInfo name,
                SourceLocation location,
                QualType type,
                Expr init,
                boolean hasInit)
        {
            super(FieldDecl, context, name, location, type);
            this.init = init;
            this.hasInit = hasInit;
            assert !(init != null && hasInit):"got initializer for bitfield";
        }

        /**
         * Determines if this is a bitfield.
         * @return
         */
        public boolean isBitField() { return init != null && !hasInit; }

        public Tree.Expr getBitWidth() { return isBitField()? init: null; }

        public long getBitWidthValue()
        {
            assert isBitField() :"not a bitfield";
            Tree.Expr bitWidth = getBitWidth();
            return bitWidth.evaluateKnownConstInt().getZExtValue();
        }

        public RecordDecl getParent()
        {
            return (RecordDecl)getDeclContext();
        }

        public int getFieldIndex()
        {
            if (cachedFieldIndex != 0) return cachedFieldIndex;

            final RecordDecl rd = getParent();
            int index = 0;
            int i = 0, e = rd.getDeclCounts();
            while (true)
            {
                assert i!=e:"failed to find field in parent!";
                if (rd.getDeclAt(i).equals(this))
                    break;

                ++i;
                ++index;
            }

            cachedFieldIndex = index + 1;
            return index;
        }
        /**
         * Determines whether this bitfield is a unnamed bitfield.
         * @return
         */
        public boolean isUnamaedBitField()
        {
            return !hasInit && init != null && getDeclName() == null;
        }

        public boolean isAnonymousStructOrUnion()
        {
            if (!isImplicit() || getDeclName() != null)
                return false;

            if (getDeclType().isRecordType())
                return ((RecordType)getDeclType().getType()).getDecl().isAnonymousStructOrUnion();

            return false;
        }
    }

    public enum DefinitionKind
    {
        /**
         * This declaration is only a declaration.
         */
        DeclarationOnly,

        /**
         * This declaration is a tentative declaration.
         */
        TentativeDefinition,

        /**
         * This declaration is a definitely declaration.
         */
        Definition;
    }

    /**
     * An instance of this class is created to represent a variable
     * declaration or definition.
     */
    public static class VarDecl extends DeclaratorDecl
    {
        StorageClass sc;

        /**
         * The initializer for this variable.
         */
        Tree.Expr init;

        private boolean wasEvaluated;
        private APValue evaluatedValue;
        private boolean isEvaluating;

        public VarDecl(DeclKind kind,
                DeclContext context,
                IdentifierInfo name,
                SourceLocation location,
                QualType type, StorageClass sc)
        {
            super(kind, context, name, location, type);
            this.sc = sc;
            wasEvaluated = false;
            evaluatedValue = null;
            isEvaluating = false;
        }

        /**
         * Returns true if this variable just declared in function rathere than
         * parameter list. Note that this includes static variables
         * inside of functions. It also includes variables inside blocks
         *
         * void foo() { int x; static int y; extern int z; }
         * @return
         */
        public boolean isLocalVarDecl()
        {
            if (getDeclKind() != DeclKind.VarDecl)
                return false;
            if (getDeclContext() != null)
                return getDeclContext().isFunction();
            return false;
        }

        /**
         * Returns true if this variable declared in a function scope
         * but excludes the case that variable declared in blocks.
         * @return
         */
        public boolean isFunctionVarDecl()
        {
            if (getDeclKind() != DeclKind.VarDecl)
                return false;
            if (getDeclContext() != null)
                return getDeclContext().isFunction()
                        && getDeclKind() != DeclKind.BlockDecl;
            return false;
        }

        /**
         * Returns true if a variable with function scope
         * is a non-static local variable.
         */
        public boolean hasLocalStorage()
        {
            if (sc == StorageClass.SC_none)
                return !isFileVarDecl();
            return sc.ordinal() >= StorageClass.SC_auto.ordinal();
        }

        public boolean isStaticLocal()
        {
            return sc == StorageClass.SC_static && !isFileVarDecl();
        }

        public boolean isFileVarDecl()
        {
            if (getDeclKind() != DeclKind.VarDecl)
                return false;

            return getDeclContext().isFileContext();

        }

        public boolean hasGlobalStorage()
        {
            return !hasLocalStorage();
        }

        /**
         * Check whether this declaration is a definition. If this could be
         * a tentative definition (in C)
         * @return
         */
        public DefinitionKind isThisDeclarationADefinition()
        {
            // C99 6.7p5:
            //   A definition of an identifier is a declaration for that identifier that
            //   [...] causes storage to be reserved for that object.
            // Note: that applies for all non-file-scope objects.
            // C99 6.9.2p1:
            //   If the declaration of an identifier for an object has file scope and an
            //   initializer, the declaration is an external definition for the identifier
            if (hasInit())
                return DefinitionKind.Definition;

            if (sc == StorageClass.SC_extern)
                return DefinitionKind.DeclarationOnly;

            // C99 6.9.2p2:
            //   A declaration of an object that has file scope without an initializer,
            //   and without a storage class specifier or the scs 'static', constitutes
            //   a tentative definition.
            if (isFileVarDecl())
                return DefinitionKind.TentativeDefinition;

            // What's left is (in C, block-scope) declarations without initializers or
            // external storage. These are definitions.
            return DefinitionKind.Definition;
        }

        public boolean hasInit()
        {
            return init!=null;
        }

        public Tree.Expr getInit()
        {
            return init;
        }

        /**
         * Determines whether if a variable has extern qualified.
         * @return
         */
        public boolean hasExternalStorage()
        {
            return sc == StorageClass.SC_extern;
        }

        public void setInit(Tree.Expr init)
        {
            this.init = init;
        }

        public boolean isEvaluatingValue()
        {
            return isEvaluating;
        }

        public void setEvaluatingValue()
        {
            isEvaluating = true;
        }

        public void setEvaluatedValue(final  APValue val)
        {
            isEvaluating = false;
            wasEvaluated = true;
            evaluatedValue = val;
        }

        public APValue getEvaluatedValue()
        {
            if (wasEvaluated)
                return evaluatedValue;
            return null;
        }

        public StorageClass getStorageClass()
        {
            return sc;
        }

        /**
         * Return true for local variable declaration rather than file scope var.
         * @return
         */
        public boolean isBlockVarDecl()
        {
            if (getDeclKind() != VarDecl)
                return false;
            DeclContext dc = getDeclContext();
            if (dc != null)
                return dc.isFileContext();
            return false;
        }
    }

    /**
     * Represent a parameter to a function.
     */
    public static class ParamVarDecl extends VarDecl
    {
        public ParamVarDecl(
                DeclKind dk,
                DeclContext context,
                IdentifierInfo name,
                SourceLocation location,
                QualType type,
                StorageClass sc)
        {
            super(dk, context, name, location, type, sc);
        }

        public static ParamVarDecl create(
                DeclContext context,
                IdentifierInfo name,
                SourceLocation location,
                QualType type,
                StorageClass sc)
        {
            return new ParamVarDecl(ParamVar, context, name, location, type, sc);
        }

        public void setScopeInfo(int i, int protoTypeIndex)
        {

        }

        /**
         * Sets the function declaration that owns this
         * ParmVarDecl. Since ParmVarDecls are often created before the
         * FunctionDecls that own them, this routine is required to update
         * the DeclContext appropriately.
         */
        public void setOwningFunction(DeclContext fd) {setDeclContext(fd);}
    }

    public static class OriginalParamVarDecl extends ParamVarDecl
    {
        protected QualType originalType;

        public OriginalParamVarDecl(
                DeclContext dc,
                SourceLocation loc,
                IdentifierInfo id,
                QualType ty,
                QualType originalType,
                StorageClass sc)
        {
            super(OriginalParamVar, dc, id, loc, ty, sc);
            this.originalType = originalType;
        }

        public static OriginalParamVarDecl create(DeclContext dc,
                SourceLocation loc,
                IdentifierInfo id,
                QualType ty,
                QualType originalType,
                StorageClass sc)
        {
            return new OriginalParamVarDecl(dc, loc, id, ty, originalType, sc);
        }

        public QualType getOriginalType()
        {
            return originalType;
        }

        public void setOriginalType(QualType ty)
        {
            originalType = ty;
        }
    }

    /**
     * An instance of this class is created to represent a
     * function declaration or definition.
     * <p>
     * Since a given function can be declared several times in a program,
     * there may be several FunctionDecls that correspond to that
     * function. Only one of those FunctionDecls will be found when
     * traversing the list of declarations in the context of the
     * FunctionDecl (e.g., the translation unit); this FunctionDecl
     * contains all of the information known about the function. Other,
     * previous declarations of the function are available via the
     * getPreviousDeclaration() chain.
     * </p>
     */
    public static class FunctionDecl extends DeclaratorDecl
    {
        private StorageClass sc;
        private ArrayList<ParamVarDecl> paramInfo;
        private SourceLocation endLoc;

        private Tree.Stmt body;

        private boolean isInlineSpecified;
        private boolean hasImplicitReturnZero;
        private boolean hasPrototype;

        FunctionDecl(IdentifierInfo name,
                DeclContext context,
                SourceLocation location,
                QualType type,
                StorageClass sc,
                boolean isInline)
        {
            this(name, context, location, type, sc, isInline, false);
        }

        FunctionDecl(IdentifierInfo name,
                DeclContext context,
                SourceLocation location,
                QualType type,
                StorageClass sc,
                boolean isInline,
                boolean hasPrototype)
        {
            super(FunctionDecl, context, name, location, type);
            this.sc = sc;
            this.isInlineSpecified = isInline;
            paramInfo = new ArrayList<>();
            endLoc = SourceLocation.NOPOS;
            body = null;
            this.hasPrototype = hasPrototype;
        }

        public QualType getReturnType()
        {
            return getDeclType().getType().getFunctionType().getResultType();
        }

        public void setParams(ArrayList<ParamVarDecl> params)
        {
            paramInfo = paramInfo;
        }

        public void setRangeEnd(SourceLocation end)
        {
            endLoc = end;
        }

        public int getNumParams()
        {
            return paramInfo.size();
        }

        public ParamVarDecl getParamDecl(int idx)
        {
            assert idx>=0 && idx< paramInfo.size();
            return paramInfo.get(idx);
        }

        public ArrayList<ParamVarDecl> getParamInfo()
        {
            return paramInfo;
        }

        /**
         * Returns true if the function has body (definition).
         * @return
         */
        public boolean hasBody()
        {
            return body != null;
        }

        /**
         * Determines whether this function has definition body.
         * @return
         */
        public boolean isDefined()
        {
            // TODO: 17-5-9 finish redeclaration
            return hasBody();
        }

        public Tree.Stmt getBody()
        {
            return body;
        }

        public void setBody(Tree.Stmt b)
        {
            body = b;
        }

        /**
         * Determines whether this function is "main", which is the
         * entry point into an executable environment.
         * @return
         */
        public boolean isMain()
        {
            // TODO
            return false;
        }

        /**
         * Determines if this function is a global function.
         * @return
         */
        public boolean isGlobal()
        {
            return !(sc == StorageClass.SC_static);
        }

        public StorageClass getStorageClass()
        {
            return sc;
        }

        public boolean isInlineSpecified()
        {
            return isInlineSpecified;
        }

        public void setInlineSpecified(boolean val)
        {
            isInlineSpecified = val;
        }

        public boolean hasImplicitReturnZero()
        {
            return hasImplicitReturnZero;
        }

        public void setHasImplicitReturnZero(boolean b)
        {
            hasImplicitReturnZero = b;
        }

	    /**
         * Returns the compound statement attached to this function declaration.
         * @return
         */
        public Tree.CompoundStmt getCompoundBody()
        {
            Tree.Stmt s = getBody();
            if (s instanceof Tree.CompoundStmt)
                return (Tree.CompoundStmt)s;
            return null;
        }
    }
    /**
     * Represents a declaration of a type.
     */
    public static class TypeDecl extends NamedDecl
    {
        /**
         * This indicates the jlang.type object that represents this typeDecl.
         */
        private Type tyepForDecl;

        protected TypeDecl(DeclKind kind,
                DeclContext context,
                IdentifierInfo name,
                SourceLocation loc)
        {
            super(kind, context, name, loc);
        }

        public Type getTypeForDecl()
        {
            return tyepForDecl;
        }

        public void setTyepForDecl(Type t)
        {
            tyepForDecl = t;
        }
    }

    /**
     * Represents a declaration of a typedef getIdentifier.
     */
    public static class TypeDefDecl extends TypeDecl
    {
        private QualType underlyingType;

        public TypeDefDecl(
                DeclContext context,
                IdentifierInfo id,
                SourceLocation loc,
                QualType type)
        {
            super(TypedefDecl, context, id, loc);
            underlyingType = type;
        }

        public QualType getUnderlyingType() { return underlyingType;}
    }

    /**
     * Base class for declarations which introduce a typedef-getIdentifier.
     */
    public static class TypedefNameDecl extends TypeDecl
    {
        private QualType type;

        public TypedefNameDecl(
                DeclKind kind,
                DeclContext context,
                IdentifierInfo name,
                SourceLocation loc)
        {
            super(kind, context, name, loc);
        }

        public QualType getUnderlyingType()
        {
            return type;
        }
    }

    /**
     * Represents the declaration of a struct/union/enum.
     */
    public static class TagDecl extends TypeDecl
    {
        protected boolean isBeingDefined;
        protected boolean isCompleteDefinition;
        protected DeclContext declContext;
        protected Type.TagTypeKind tagTypeKind;

        /**
         * True if this tag is free standing, e.g. "struct X;".
         */
        protected boolean isFreeStanding;

        private SourceLocation tagKkeywordLoc;
        private SourceLocation rbraceLoc;
	    /**
         * If a TagDecl is anonymous and part of a typedef,
         * this points to the TypedefDecl. Used for mangling.
         */
        private TypeDefDecl typedefAnonDecl;

        public TagDecl(DeclKind kind,
                Type.TagTypeKind tagTypeKind,
                DeclContext context,
                IdentifierInfo name,
                SourceLocation loc,
                TagDecl prevDecl,
                SourceLocation tkl)
        {
            super(kind, context, name, loc);
            isBeingDefined = false;
            this.tagTypeKind = tagTypeKind;
            setPreviousDecl(prevDecl);
            tagKkeywordLoc = tkl;
        }


        public TagDecl(DeclKind kind,
                Type.TagTypeKind tagTypeKind,
                DeclContext context,
                IdentifierInfo name,
                SourceLocation loc,
                TagDecl prevDecl)
        {
            this(kind, tagTypeKind, context, name, loc, prevDecl, SourceLocation.NOPOS);;
        }

        private void setPreviousDecl(TagDecl prevDecl)
        {
            // TODO setPreviousDecl(TagDecl prevDecl)
        }

        public void startDefinition()
        {
            isBeingDefined = true;
        }

        public void setLexicalDeclaration(DeclContext curContext)
        {
            if (declContext == curContext)
                return;

            this.declContext = curContext;
        }
        public final boolean isBeingDefined() { return isBeingDefined; }

        public final boolean isCompleteDefinition()
        {
            return isCompleteDefinition;
        }

        /**
         * Sets the flag of {@linkplain this#isBeingDefined} for completing
         * the definition of this forward declaration.
         */
        void completeDefinition()
        {
            isBeingDefined = true;
        }

        public void setFreeStanding(boolean isFreeStanding)
        {
            this.isFreeStanding = isFreeStanding;
        }
        public boolean isFreeStanding()
        {
            return isFreeStanding;
        }

        public Type.TagTypeKind getTagKind()
        {
            return tagTypeKind;
        }

        public boolean isStruct()
        {
            return tagTypeKind == TTK_struct;
        }

        public boolean isUnion()
        {
            return tagTypeKind == TTK_union;
        }

        public boolean isEnum()
        {
            return tagTypeKind == TTK_enum;
        }

        public void setRBraceLoc(SourceLocation rbraceloc)
        {
            this.rbraceLoc = rbraceloc;
        }

        public String getKindName()
        {
            switch (tagTypeKind)
            {
                case TTK_enum: return "enum";
                case TTK_struct: return "struct";
                case TTK_union: return "union";
                default: return null;
            }
        }

        public TagDecl getDefinition()
        {
            if (isCompleteDefinition)
                return this;
            return null;
        }

        public TypeDefDecl getTypedefAnonDecl()
        {
            return typedefAnonDecl;
        }

        public void setTypedefAnonDecl(TypeDefDecl typedefAnonDecl)
        {
            this.typedefAnonDecl = typedefAnonDecl;
        }
    }

    public static class RecordDecl extends TagDecl
    {
        /**
         * This is true if this struct ends with a flexible
         * array member (e.g. int X[]) or if this union contains a struct that does.
         * If so, this cannot be contained in arrays or other structs as a member.
         */
        private boolean hasFlexibleArrayMember;
        /**
         * Whether this is the type of an anonymous struct or union.
         */
        private boolean anonymousStructOrUnion;
        /**
         * This is true if this struct has at least one
         * member containing an object
         */
        private boolean hasObjectMember;

        public RecordDecl(
                IdentifierInfo name,
                Type.TagTypeKind tagTypeKind,
                DeclContext context,
                SourceLocation loc,
                RecordDecl prevDecl)
        {
            super(StructDecl, tagTypeKind, context, name, loc, prevDecl);
            anonymousStructOrUnion = false;
        }

        void completeDefinition()
        {
            assert (!isCompleteDefinition):"Cannot redefine struct or union!";
            super.completeDefinition();
        }

        public void addDecl(FieldDecl fd)
        {
            addDecl(fd);
        }

        public void remove(FieldDecl fd)
        {
            removeDecl(fd);
        }

        public FieldDecl getDeclAt(int index)
        {
            List<Decl> list = getDeclsInContext();
            assert index>= 0&&index<list.size();
            return (FieldDecl) list.get(index);
        }

        public int getNumFields()
        {
            return getDeclsInContext().size();
        }

	    /**
         * Whether this is an anonymous struct or union.
         * To be an anonymous struct or union, it must have been
         * declared without a name and there must be no objects of this
         * type declared, e.g.,
         * <code>
         *   union { int i; float f; };
         * </code>
         * is an anonymous union but neither of the following are:
         * <code>
         *  union X { int i; float f; };
         *  union { int i; float f; } obj;
         * </code>
         * @return
         */
        public boolean isAnonymousStructOrUnion()
        {
            return anonymousStructOrUnion;
        }

        public boolean hasFlexibleArrayNumber()
        {
            return hasFlexibleArrayMember;
        }

        public void setHasFlexibleArrayMember(boolean val)
        {
            hasFlexibleArrayMember = val;
        }

        public boolean hasObjectMember()
        {
            return hasObjectMember;
        }

        public void setHasObjectMember(boolean v)
        {
            hasObjectMember = v;
        }
    }

    /**
     * Represents an enumeration declaration.
     * As an extension, we allows forward declared enums.
     */
    public static class EnumDecl extends TagDecl
    {
        /**
         * The integer jlang.type that values of this jlang.type should promote to.
         * In C, enumerators are generally of an integer tyep directly.
         */
        private QualType integerType;

        public EnumDecl(
                IdentifierInfo name,
                DeclContext context,
                SourceLocation loc,
                EnumDecl prevDecl)
        {
            super(EnumDecl, TTK_enum, context, name, loc, prevDecl);
        }

        /**
         * When instantiating an instance, the {@linkplain EnumDecl} corresponds to
         * a forward-declared enum.
         * <br>
         * This function is used to mark this declaration as has completed. It's
         * enumerator have already been added.
         * @param newType  Indicates that underlying jlang.type of this forward declaration.
         */
        public void completeDefinition(QualType newType)
        {
            assert !isCompleteDefinition():"Cannot refine enums!";
            integerType = newType;
            super.completeDefinition();
        }

        public QualType getIntegerType()
        {
            if (integerType == null)
                return new QualType();
            return integerType;
        }

        public void setIntegerType(QualType enumUnderlying)
        {
            integerType = enumUnderlying;
        }
    }

    /**
     * An instance of this object exists for each enum constant
     * that is defined.  For example, in "enum X {a,b}", each of a/b are
     * EnumConstantDecl's, X is an instance of EnumDecl, and the jlang.type of a/b is a
     * TagType for the X EnumDecl.
     */
    public static class EnumConstantDecl extends  ValueDecl
    {
        /**
         * An integeral constant expression.
         */
        private Tree.Stmt init;
        /**
         * The value.
         */
        private APSInt val;
        EnumConstantDecl(
                IdentifierInfo name,
                DeclContext context,
                SourceLocation location,
                QualType type,
                Tree.Stmt init)
        {
            this(name, context, location, type, init, null);
        }

        EnumConstantDecl(
                IdentifierInfo name,
                DeclContext context,
                SourceLocation location,
                QualType type,
                Tree.Stmt init,
                APSInt val)
        {
            super(DeclKind.EnumConstant, context, name, location, type);
            this.init = init;
            this.val = val;
        }

        public Expr getInitExpr() { return (Tree.Expr)init; }
        public APSInt getInitValue() { return val; }

        public void setInitExpr(Tree.Expr e)
        {
            init = e;
        }

        public void setInitValue(APSInt e)
        {
            val = e;
        }
    }
}