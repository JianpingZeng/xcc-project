package frontend.comp;

import frontend.ast.Tree;

/**
 * Env<A> specialized as Env<AttrContext>
 */
public class AttrContextEnv extends Env {

    /**
     * Create an outermost environment for a given (toplevel)tree,
     *  with a given info field.
     */
    public AttrContextEnv(Tree tree, AttrContext info) {
        super(tree, info);
    }

    /*synthetic*/ public Env dup(Tree x0, Object x1) {
        return super.dup(x0, x1);
    }
}
