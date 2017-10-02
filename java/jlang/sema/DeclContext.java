package jlang.sema;

import jlang.clex.IdentifierInfo;
import jlang.cparser.DeclKind;
import jlang.sema.Decl.NamedDecl;
import jlang.type.TagType;
import jlang.type.Type;

import java.util.ArrayList;
import java.util.List;

import static jlang.cparser.DeclKind.*;

/**
 * <p>
 * 该类实现一个接口IDeclContext，由接口IDeclContext定义声明作用域所需要的函数，然后
 * TranslattionUnitDecl, FunctionDecl, TagDecl, EnumDecl, RecordDecl等实现这个接口，
 * 并在这些类里面拥有一个DeclContext对象，所有的IDeclContext接口转发给这个对象实现。
 * </p>
 * <p>
 * 同样的实现Redeclarable也一样.
 * </p>
 * <p>
 * This is used only as base class of specific decl types that
 * can act as declaration contexts. These decls are (only the top classes
 * that directly derive from DeclContext are mentioned, not their subclasses):
 * </p>
 * @see jlang.sema.Decl.TranslationUnitDecl
 * @see jlang.sema.Decl.FunctionDecl
 * @see jlang.sema.Decl.TagDecl
 * @see jlang.sema.Decl.EnumDecl
 * @see jlang.sema.Decl.RecordDecl
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public class DeclContext implements IDeclContext
{
    /**
     * A list contains all declaration in current declaration context.
     */
    private List<Decl> declInScope;
    /**
     * Indicates what kind this is.
     */
    private DeclKind kind;

    private Decl decl;

    /**
     * A lookup data structure for performing declaration searching
     * according to given {@linkplain IdentifierInfo}. Note that,
     * this lookup table is lazy and don't be built until some client
     * asks for it.
     */
    private StoredDeclsMap lookupTable;

    public DeclContext(DeclKind kind, Decl d)
    {
        this.kind = kind;
        decl = d;
        assert decl instanceof IDeclContext :"Only IDeclContext allow to used here!";
        declInScope = new ArrayList<>(32);
    }

    public void addDecl(Decl decl)
    {
        addHiddenDecl(decl);

        if (decl instanceof NamedDecl)
        {
            NamedDecl nd = (NamedDecl)decl;
            nd.getDeclContext().makeDeclVisibleInContext(nd);
        }
    }

    public void addHiddenDecl(Decl d)
    {
        if (!declInScope.contains(d))
            declInScope.add(d);
    }

    public void removeDecl(Decl decl)
    {
        assert declInScope.contains(decl) :
                "Can not calling this on element not contained in decls";
        declInScope.remove(decl);
    }

    /**
     * isDeclInContext method return true if given {@code decl} is declared in
     * the current DeclContext.
     * @param decl
     * @return
     */
    public boolean isDeclInContext(Decl decl)
    {
        for (Decl d : declInScope)
        {
            if (d.getDeclKindName().equals(decl.getDeclKindName()))
                return true;
        }
        return false;
    }

    public boolean isTransparentContext()
    {
        if (decl != null)
            return decl.getKind() == EnumDecl;
        else
            return false;
    }

    public boolean isFunction()
    {
        return kind == FunctionDecl;
    }

    public boolean isRecord()
    {
        return kind == RecordDecl;
    }
    /**
     * Determines if this context is file context.
     * @return
     */
    public boolean isFileContext()
    {
        return kind == DeclKind.TranslationUnitDecl;
    }

    @Override
    public boolean isTranslationUnit()
    {
        return kind == TranslationUnitDecl;
    }

    @Override
    public DeclKind getKind()
    {
        return kind;
    }

    @Override
    public String getDeclKindName()
    {
        return kind.declKindName;
    }

    @Override
    public ASTContext getParentASTContext()
    {
        if (decl != null)
            return decl.getASTContext();
        return null;
    }

    /**
     * Gets the parent declaration context enclosing this.
     * @return
     */
    public IDeclContext getParent()
    {
        return decl.getDeclContext();
    }

    public boolean isEmpty()
    {
        return declInScope.isEmpty();
    }

    public List<Decl> getDeclsInContext()
    {
        return declInScope;
    }

    @Override
    public Decl getDeclAt(int idx)
    {
        assert idx >= 0 && idx < declInScope.size();
        return declInScope.get(idx);
    }

    public int getDeclCounts()
    {
        return declInScope.size();
    }

    /**
     * Build the lookup data structure with all of the
     * declarations in DCtx (and any other contexts linked to it or
     * transparent contexts nested within it).
     * @param dc
     */
    private void buildLookup(IDeclContext dc)
    {
        for (int i = 0,e = dc.getDeclCounts(); i < e; ++i)
        {
            Decl d = dc.getDeclAt(i);
            if (d instanceof NamedDecl)
            {
                // Insert this declaration into the lookup structure, but only
                // if it's semantically in its decl context.  During non-lazy
                // lookup building, this is implicitly enforced by addDecl.
                NamedDecl nd = (NamedDecl)d;
                if (d.getDeclContext() == dc)
                    makeDeclVisibleInContextImpl(nd);

                // If this declaration is itself a transparent declaration context,
                // add its members (recursively).
                if(d instanceof IDeclContext)
                {
                    IDeclContext innerCtx = (IDeclContext)d;
                    if (innerCtx.isTransparentContext())
                        buildLookup(innerCtx.getPrimaryContext());
                }
            }
        }
    }

    @Override
    public NamedDecl[] lookup(IdentifierInfo name)
    {
        IDeclContext primaryCtx = getPrimaryContext();
        if (primaryCtx != decl)
            return primaryCtx.lookup(name);

        /// If there is no lookup data structure, build one now by walking
        /// all of the linked DeclContexts (in declaration order!) and
        /// inserting their values.
        if (lookupTable == null)
        {
            buildLookup((IDeclContext)decl);
            if (lookupTable == null)
                return new NamedDecl[0];
        }

        if (!lookupTable.containsKey(name))
            return new NamedDecl[0];
        return lookupTable.get(name).getLookupResult(getParentASTContext());
    }

    private void makeDeclVisibleInContextImpl(NamedDecl d)
    {
        // Skip the unnamed declaration.
        if (d.getDeclName() == null)
            return;

        if (lookupTable == null)
            lookupTable = new StoredDeclsMap();

        StoredDeclsList declNameEntries;
        if (lookupTable.containsKey(d.getDeclName()))
            declNameEntries = lookupTable.get(d.getDeclName());
        else
        {
            // Insert a new decl list for this declName.
            declNameEntries = new StoredDeclsList();
            lookupTable.put(d.getDeclName(), declNameEntries);
        }

        // If the d is the first decl of same decl asmName,
        // we just set the only value by calling the method setOnlyValue.
        if (declNameEntries.isNull())
        {
            declNameEntries.setOnlyValue(d);
            return;
        }
        // Otherwise, the d is the redeclaration of the same decl asmName.
        // check to see if there is already a decl for which declarationReplaces
        // returns true.  If there is one, just replace it and return.
        if(declNameEntries.handleRedeclaration(getParentASTContext(), d))
            return;

        // Put this declaration into the appropriate slot.
        declNameEntries.addSubsequentialDecl(d);
    }

    @Override
    public void makeDeclVisibleInContext(NamedDecl d)
    {
        if (lookupTable != null)
        {
            makeDeclVisibleInContextImpl(d);
        }

        // If we already have a lookup data structure, perform the insertion
        // into it. Otherwise, be lazy and don't build that structure until
        // someone asks for it.
        if(lookupTable != null)
            makeDeclVisibleInContextImpl(d);

        // If we are in a transparent context, insert into our parent
        // context too. This operation is recursive.
        if (isTransparentContext())
            getParent().makeDeclVisibleInContext(d);
    }

    public IDeclContext getLookupContext()
    {
        IDeclContext ctx = this;
        while (ctx.isTransparentContext())
            ctx = ctx.getParent();
        return ctx;
    }

    @Override
    public IDeclContext getPrimaryContext()
    {
        switch (kind)
        {
            case TranslationUnitDecl:
                return this;
            case RecordDecl:
            case EnumDecl:
            {
                Type t = ((Decl.TagDecl)decl).getTypeForDecl();
                if (t instanceof TagType)
                {
                    TagType tt = (TagType)t;
                    if (tt.isBeingDefined() || (tt.getDecl() != null &&
                        tt.getDecl().isCompleteDefinition()))
                        return tt.getDecl();
                }
                return this;
            }
            case FunctionDecl:
                return this;
            default:
                assert false :"Unknown DeclContext kind";
                return null;
        }
    }
}
