package frontend.symbol;

/**
 * Internal frontend.symbol kinds, which distinguish between elements of
 *  different subclasses of Symbol. Symbol kinds are organized so they can be
 *  or'ed to sets.
 *  
 * @author Xlous.zeng  
 * @version 2016年1月8日 上午10:50:01 
 */
public interface SymbolKinds {

    /**
     * The empty set of kinds.
     */
    int NIL = 0;

    /**
     * The kind of frontend.type symbols (frontend.type variables).
     */
    int TYP = 1;

    /**
     * The kind of variable symbols.
     */
    int VAR = 2;

    /**
     * The kind of VALUES (variables or non-variable expressions), includes VAR.
     */
    int VAL = 4 | VAR;

    /**
     * The kind of methods.
     */
    int MTH = 8;
    
    /**
     * The kind of composite frontend.type.
     */
    int COMPOSITE = 16;

    /**
     * The error kind, which includes all other kinds.
     */
    int ERR = 31;

    /**
     * The set of all kinds.
     */
    int AllKinds = ERR;
}

