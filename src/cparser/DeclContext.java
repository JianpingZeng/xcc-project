package cparser;

import sema.Decl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static cparser.DeclKind.*;

/**
 * This is used only as base class of specific decl types that
 * can act as declaration contexts. These decls are (only the top classes
 * that directly derive from DeclContext are mentioned, not their subclasses):
 *
 * @see sema.Decl.TranslationUnitDecl
 * @see sema.Decl.FunctionDecl
 * @see sema.Decl.TagDecl
 * @see sema.Decl.EnumDecl
 * @see sema.Decl.RecordDecl
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public class DeclContext
{
    /**
     * A list contains all declaration in current declaration context.
     */
    private List<Decl> declInScope;
    /**
     * Indicates what kind this is.
     */
    private DeclKind kind;

    public DeclContext(DeclKind kind)
    {
        this.kind = kind;
        declInScope = new ArrayList<>(32);
    }

    public void addDecl(Decl decl)
    {
        declInScope.add(decl);
    }

    public void removeDecl(Decl decl)
    {
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
        return declInScope.contains(decl);
    }

    public boolean isTransparentContext()
    {
        if (this instanceof Decl)
            return ((Decl)this).getDeclKind() == EnumDecl;
        else
            return false;
    }

    public Iterator<Decl> iterator()
    {
        return declInScope.iterator();
    }

    public boolean isFunction()
    {
        return kind == FunctionDecl;
    }

    public boolean isRecord()
    {
        return kind == StructDecl;
    }
    /**
     * Determines if this context is file context.
     * @return
     */
    public boolean isFileContext()
    {
        return kind == DeclKind.CompilationUnitDecl;
    }

    /**
     * Gets the parent declaration context enclosing this.
     * @return
     */
    public DeclContext getParent()
    {
        return ((Decl)this).getDeclContext();
    }

    public boolean isEmpty()
    {
        return declInScope.isEmpty();
    }

    public List<Decl> getDeclsInContext()
    {
        return declInScope;
    }
}
