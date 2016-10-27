package sema;

/**
 * @author Xlous.zeng
 * @version 0.1
 */

import ast.Tree;
import ast.Tree.Expr;
import ast.Tree.LabelledStmt;
import cparser.DeclContext;
import cparser.DeclKind;
import cparser.Declarator;
import type.QualType;
import type.Type;
import utils.Position;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static cparser.DeclKind.*;
import static type.Type.TagTypeKind.TTK_enum;
import static type.Type.TagTypeKind.TTK_struct;
import static type.Type.TagTypeKind.TTK_union;

/**
 * This class encapsulate information of declaration or definition, like
 * a variable, typedef, function, struct.
 */
public abstract class Decl extends DeclContext
{

    public enum StorageClass
    {
        SC_none,
        // for Function
        SC_static,
        SC_extern,

        // for variable
        SC_auto,
        SC_register
    }

    private int location;
    private DeclKind kind;
    private boolean invalidDecl;
    private DeclContext context;
    private boolean isUsed;
    private IdentifierNamespace IDNS;

    Decl(DeclKind kind, DeclContext context, int location)
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

    public int getLocation()
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

    public static class TranslationUnitDecl extends Decl
    {
        public TranslationUnitDecl(DeclContext context, int location)
        {
            super(DeclKind.CompilationUnitDecl, context, location);
        }

        public TranslationUnitDecl(DeclContext context)
        {
            super(DeclKind.CompilationUnitDecl, context, Position.NOPOS);
        }
    }

    public static class NamedDecl extends Decl
    {
        String name;

        NamedDecl(DeclKind kind, DeclContext context, String name, int location)
        {
            super(kind, context, location);
            this.name = name;
        }

        public String getDeclName()
        {
            return name;
        }
    }
    public static class LabelDecl extends NamedDecl
    {
        LabelledStmt stmt;
        LabelDecl(String name, DeclContext context, LabelledStmt stmt, int location)
        {
            super(DeclKind.LabelDecl, context, name, location);
            this.stmt = stmt;
        }

        public void setStmt(LabelledStmt stmt)
        {
            this.stmt = stmt;
        }
    }

    public static class ValueDecl extends NamedDecl
    {
        private QualType declType;

        ValueDecl(DeclKind kind,
                DeclContext context,
                String name,
                int location,
                QualType type)
        {
            super(kind, context, name, location);
            this.declType = type;
        }

        public QualType getDeclType() { return declType; }
        public void setDeclType(QualType type) {declType = type; }
    }

    public static class DeclaratorDecl extends ValueDecl
    {
        DeclaratorDecl(DeclKind kind,
                DeclContext context,
                String name,
                int location,
                QualType type)
        {
            super(kind, context, name, location, type);
        }
    }

    /**
     * This class is created by {@linkplain Sema#actOnField(Scope, Decl,
     * int, Declarator, Expr)}
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

        FieldDecl(DeclContext context, String name, int location,
                QualType type, Expr init, boolean hasInit)
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

        public int getBitWidthValue()
        {
            assert isBitField() :"not a bitfield";
            Tree.Expr bitWidth = getBitWidth();
            return bitWidth.evaluateKnownConstInt();
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
                String name,
                int location,
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

        public boolean isFunctionVarDecl()
        {
            if (getDeclKind() != DeclKind.VarDecl)
                return false;
            if (getDeclContext() != null)
                return getDeclContext().isFunction() && ;
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
    }

    public static class ParamVarDecl extends VarDecl
    {
        ParamVarDecl(DeclKind kind,
                DeclContext context,
                String name,
                int location,
                QualType type,
                StorageClass sc)
        {
            super(kind, context, name, location, type, sc);
        }

        public void setScopeInfo(int i, int protoTypeIndex)
        {

        }

        /// Sets the function declaration that owns this
        /// ParmVarDecl. Since ParmVarDecls are often created before the
        /// FunctionDecls that own them, this routine is required to update
        /// the DeclContext appropriately.
        public void setOwningFunction(DeclContext fd)
        {
            setDeclContext(fd);
        }
    }

    public static class FunctionDecl extends DeclaratorDecl
    {
        private StorageClass sc;
        private ArrayList<ParamVarDecl> paramInfo;
        private int endLoc;

        private Tree.Stmt body;

        private boolean isInlineSpecified;

        FunctionDecl(String name,
                DeclContext context,
                int location,
                QualType type,
                StorageClass sc,
                boolean isInlineSpecified)
        {
            super(FunctionDecl, context, name, location, type);
            this.sc = sc;
            this.isInlineSpecified = isInlineSpecified;
            paramInfo = new ArrayList<>();
            endLoc = Position.NOPOS;
            body = null;
        }

        public QualType getReturnType()
        {
            return getDeclType().getType().getFunctionType().getReturnType()
        }

        public void setParams(ArrayList<ParamVarDecl> params)
        {
            paramInfo = paramInfo;
        }

        public void setRangeEnd(int end)
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
    }
    /**
     * Represents a declaration of a type.
     */
    public static class TypeDecl extends NamedDecl
    {
        /**
         * This indicates the type object that represents this typeDecl.
         */
        private Type tyepForDecl;

        protected TypeDecl(DeclKind kind,
                DeclContext context,
                String name,
                int loc)
        {
            super(kind, context, name,loc);
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
     * Represents a declaration of a typedef name.
     */
    public static class TypeDefDecl extends TypeDecl
    {
        public TypeDefDecl(DeclKind kind,
                DeclContext context,
                String name,
                int loc)
        {
            super(kind, context, name, loc);
        }
    }

    public static class TypedefNameDecl extends TypeDefDecl
    {
        private QualType type;

        public TypedefNameDecl(DeclKind kind, DeclContext context, String name,
                int loc)
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
    public static class TagDecl extends TypeDefDecl
    {
        protected boolean isBeingDefined;
        protected boolean isCompleteDefinition;
        protected DeclContext declContext;
        protected Type.TagTypeKind tagTypeKind;

        /**
         * True if this tag is free standing, e.g. "struct X;".
         */
        protected boolean isFreeStanding;

        public TagDecl(DeclKind kind,
                Type.TagTypeKind tagTypeKind,
                DeclContext context,
                String name,
                int loc,
                TagDecl prevDecl)
        {
            super(kind, context, name, loc, );
            isBeingDefined = false;
            this.tagTypeKind = tagTypeKind;
            setPreviousDecl(prevDecl);
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
    }

    public static class RecordDecl extends TagDecl
    {
        private boolean annoymousStructOrUnion;

        public RecordDecl(String name,
                Type.TagTypeKind tagTypeKind,
                DeclContext context,
                int loc,
                RecordDecl prevDecl)
        {
            super(StructDecl, tagTypeKind, context, name, loc, prevDecl);
            annoymousStructOrUnion = false;
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
    }

    /**
     * Represents an enumeration declaration.
     * As an extension, we allows forward declared enums.
     */
    public static class EnumDecl extends TagDecl
    {
        /**
         * The integer type that values of this type should promote to.
         * In C, enumerators are generally of an integer tyep directly.
         */
        private QualType integerType;

        public EnumDecl(String name,
                DeclContext context,
                int loc,
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
         * @param newType  Indicates that underlying type of this forward declaration.
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
     * EnumConstantDecl's, X is an instance of EnumDecl, and the type of a/b is a
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
        EnumConstantDecl( String name,
                DeclContext context,
                int location,
                QualType type,
                Tree.Stmt init)
        {
            super(DeclKind.EnumConstant, context, name, location, type);
            this.init = init;
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