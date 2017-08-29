package jlang.sema;

import java.util.ArrayList;

import static jlang.sema.Scope.ScopeFlags.*;

/**
 * A scope is a transient data structure that is used while parsing the
 * program. It consists with resolving identifiers to the appropriate declaration.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public final class Scope
{
    public enum ScopeFlags
    {
        CompilationUnitScope(0),

        /**
         * FunctionProto scope.
         * This indicates that the scope corresponds to a function, which
         * means that labels are set here.
         */
        FnScope(0x1),
        /**
         * This is while, do, switch, for, which can have break statement
         * embedded into it.
         */
        BreakScope(0x2),
        /**
         * ContinueStmt scope.
         * This is while, do, for, which can have continue statement
         * embedded into it.
         */
        ContinueScope(0x4),

        /**
         * DeclScope.
         * This a scope that can contains a declaration.
         */
        DeclScope(0x8),

        /**
         * ControlScope.
         * This is a scope in a if/switch/while/for statement.
         */
        ControlScope(0x10),

        /**
         * The scope of struct/union definition.
         */
        ClassScope(0x20),

        /**
         * This is a scope that corresponds to a block statement.
         * BlockScope always have the FnScope, BreakScope, ContinueScope,
         * and DeclScope flags set as well.
         */
        BlockScope(0x40),

        /**
         * This is a scope that corresponds to switch statement.
         */
        SwitchScope(0x80),

        /**
         * This is a scope that corresponds to parameter list of function
         * declaration.
         */
        FunctionProtoTypeScope(0x100);

        public int value;

        ScopeFlags(int value)
        {
            this.value = value;
        }
    }

    /**
     * The outer scope of this scope.
     * This is null for the translation-unit scope.
     */
    private Scope outer;
    /**
     * The depth of this scope.
     * The translation-uit scope has depth 0
     */
    private int depth;
    /**
     * Indicates the kind of this scope.
     */
    private int flags;
    /**
     * This is the number of function prototype scopes enclosing this scope,
     * including this scope.
     */
    private int protoTypeDepth;

    /**
     * This is the number of parameters currently declared in this scope.
     */
    private int protoTypeIndex;
    /**
     * If this scope has a parent scope that is a function body, this
     * pointer is non-null and points to it.  This is used for label processing.
     */
    private Scope funcParent;

    /**
     * BreakParent/ContinueParent - This is a direct link to the immediately
     * preceding BreakParent/ContinueParent if this scope is not one, or null if
     * there is no containing break/continue scope.
     */
    private Scope breakParent, continueParent;

    /**
     * ControlParent - This is a direct link to the immediately
     * preceding ControlParent if this scope is not one, or null if
     * there is no containing control scope.
     */
    private Scope controlParent;

    /**
     * This is a direct link to the immediately containing
     * BlockScope if this scope is not one, or null if there is none.
     */
    private Scope blockParent;

    /**
     * The DeclContext with which this scope is associated.
     * For example, the entity of a struct scope is the struct itself,
     * the entity of a function scope is a function itself.
     */
    private IDeclContext entity;

    private ArrayList<Decl> declsInScope;

    public Scope(Scope outer, int flags)
    {
        this.outer = outer;
        this.flags = flags;

        if (outer != null)
        {
            depth = outer.depth + 1;
            funcParent = outer.funcParent;
            breakParent = outer.breakParent;
            continueParent = outer.continueParent;
            controlParent = outer.controlParent;
            blockParent = outer.blockParent;
        }

        if ((this.flags & FnScope.value) != 0)
            funcParent = this;
        if ((this.flags & BreakScope.value) != 0)
            breakParent = this;
        if ((this.flags & ContinueScope.value) != 0)
            continueParent = this;
        if ((this.flags & ControlScope.value) != 0)
            controlParent = this;
        if ((this.flags & BlockScope.value) != 0)
            blockParent = this;
        declsInScope = new ArrayList<>(32);
    }

    public int getFlags()
    {
        return flags;
    }

    public void setFlags(int f)
    {
        flags = f;
    }

    public boolean isBlockScope()
    {
        return (flags & BlockScope.value) != 0;
    }

    public Scope getParent()
    {
        return outer;
    }

    public Scope getFuncParent()
    {
        return funcParent;
    }

    /**
     * Returns the closest scope that a continue statement would be affected by.
     *
     * @return
     */
    public Scope getContinueParent()
    {
        if (continueParent != null && !continueParent.isBlockScope())
            return continueParent;
        return null;
    }

    public Scope getBreakParent()
    {
        if (breakParent != null && !breakParent.isBlockScope())
            return breakParent;
        return null;
    }

    public Scope getControlParent()
    {
        return controlParent;
    }

    public Scope getBlockParent()
    {
        return blockParent;
    }

    public boolean isSwitchScope()
    {
        for (Scope s = this; s != null; s = s.getParent())
        {
            if ((s.getFlags() & SwitchScope.value) != 0)
                return true;
            else if ((s.flags & (FnScope.value | ClassScope.value
                    | BlockScope.value)) != 0)
                return false;
        }
        return false;
    }

    public boolean isFunctionProtoTypeScope()
    {
        return (flags & ScopeFlags.FunctionProtoTypeScope.value) != 0;
    }

    public void addDecl(Decl decl)
    {
        declsInScope.add(decl);
    }

    public void removeDecl(Decl decl)
    {
        declsInScope.remove(decl);
    }

    public boolean declEmpty()
    {
        return declsInScope.isEmpty();
    }

    public ArrayList<Decl> getDeclInScope()
    {
        return declsInScope;
    }

    public boolean isDeclScope(Decl decl)
    {
        return declsInScope.contains(decl);
    }

    public IDeclContext getEntity()
    {
        return entity;
    }

    public void setEntity(IDeclContext dc)
    {
        this.entity = dc;
    }

    public int getFunctionProtoTypeDepth()
    {
        return protoTypeDepth;
    }

    public int getProtoTypeIndex()
    {
        return protoTypeIndex;
    }

    public boolean isStructScope()
    {
        return (getFlags() & ScopeFlags.ClassScope.value) != 0;
    }

    /**
     * Determines if this scope is struct/union scope.
     * @return  Return true if this scope is struct/union, otherwise return false.
     */
    public boolean isClassScope()
    {
        return (getFlags() & ScopeFlags.ClassScope.value) != 0;
    }
}
