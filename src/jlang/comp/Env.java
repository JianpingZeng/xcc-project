package jlang.comp;

import jlang.ast.*;
/**
 * A class for environments, instances of which are passed as
 *  arguments to tree visitors.  Environments refer to important ancestors
 *  of the subtree that's currently visited, such as the enclosing method,
 *  the enclosing class, or the enclosing toplevel node. They also contain
 *  a generic component, represented as a jlang.type parameter, to carry further
 *  information specific to individual passes.
 */
public class Env {

    /**
     * The next enclosing environment.
     */
    public Env next;

    /**
     * The environment enclosing the current class.
     */
    public Env outer;

    /**
     * The tree with which this environment is associated.
     */
    public Tree tree;

    /**
     * The enclosing toplevel tree.
     */
    public Tree.TopLevel toplevel;

    /**
     * The next enclosing method definition.
     */
    public Tree.MethodDef enclMethod;

    /**
     * A generic field for further information.
     */
    public Object info;

    /**
     * Create an outermost environment for a given (toplevel)tree,
     * with a given info field.
     */
    public Env(Tree tree, Object info) {
        super();
        this.next = null;
        this.outer = null;
        this.tree = tree;
        this.toplevel = null;      
        this.enclMethod = null;
        this.info = info;
    }

    /**
      * Duplicate this environment, updating with given tree and info,
      *  and copying all other fields.
      */
    public Env dup(Tree tree, Object info) {
        return dupto(new Env(tree, info));
    }

    /**
      * Duplicate this environment into a given Environment,
      *  using its tree and info, and copying all other fields.
      */
    public Env dupto(Env that) {
        that.next = this;
        that.outer = this.outer;
        that.toplevel = this.toplevel;
        that.enclMethod = this.enclMethod;
        return that;
    }

    /**
      * Duplicate this environment, updating with given tree,
      *  and copying all other fields.
      */
    public Env dup(Tree tree) {
        return dup(tree, this.info);
    }

    /**
      * ReturnInst closest enclosing environment which points to a tree with given tag.
      */
    public Env enclosing(int tag) {
        Env env1 = this;
        while (env1 != null && env1.tree.tag != tag)
            env1 = env1.next;
        return env1;
    }
}
