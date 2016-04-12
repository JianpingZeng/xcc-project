package comp;

import exception.CompletionFailure;
import ast.Flags;
import ast.TreeInfo;
import symbol.Symbol;
import symbol.SymbolKinds;
import type.Type;
import type.TypeTags;
import utils.Context;
import utils.Log;
import utils.Name;

/**
 * This is a helpful auxiliary class for type check.
 *
 * @author JianpingZeng
 * @version 1.0
 */
public class Check implements SymbolKinds, TypeTags, Flags
{
	private static final Context.Key checkKey = new Context.Key();
	private Name.Table names;
	private Log log;
	private Symtab syms;

	public static Check instance(Context context)
	{
		Check instance = (Check) context.get(checkKey);
		if (instance == null) instance = new Check(context);
		return instance;
	}

	public Check(Context context)
	{
		context.put(checkKey, this);
		this.log = Log.instance(context);
		this.names = Name.Table.instance(context);
		this.syms = Symtab.instance(context);

	}

	/**
	 * Checks that given storage class or qualifier are legal for given symbol
	 * and return flags together with any implicit flags for that symbol.
	 * 
	 * @param pos The position to be used for error reporting.
	 * @param flags The set of storage class and qualifier.
	 * @param sym The defined symbol.
	 * @return
	 */
	public long checkFlags(int pos, long flags, Symbol sym)
	{
		long mask = 0;
		long implicit = 0;
		switch (sym.kind)
		{
			case VAR:
				mask = VarFlags;
				break;
			case MTH:
				mask = MethodFlags;
				implicit = UNATTRIBUTED;
				break;
			default:
				throw new AssertionError();
		}
		long illegal = flags & StandardFlags & ~mask;
		if (illegal != 0)
		{
			log.error(pos, "qualifier-specifier.not.allowed.here",
			        TreeInfo.flagNames(illegal));
		}
		return flags & (mask | ~StandardFlags) | implicit;
	}

	/**
	 * Checks that type is different from 'void'
	 * 
	 * @param pos The position to be error reporting.
	 * @param t The type instance.
	 * @return
	 */
	public Type checkNonVoid(int pos, Type t)
	{
		if (t.tag == VOID)
		{
			log.error(pos, "void.not.allowed.here");
			return syms.errType;
		}
		else
		{
			return t;
		}
	}

	/**
	 * Report a type error.
	 * @param pos Position to be used for error reporting.
	 * @param problem A string describing the error.
	 * @param found The type that was found.
	 * @param req The type that was required.
	 */
	Type typeError(int pos, String problem, Type found, Type req)
	{
		log.error(pos, "prob.found.req", problem, found.toString(),
		        req.toString());
		return syms.errType;
	}

	/**
	 * Check that a given type is assignable to a given proto-type. If it is,
	 * return the type, otherwise return errType.
	 * @param pos Position to be used for error reporting.
	 * @param found The type that was found.
	 * @param req The type that was required.
	 */
	Type checkType(int pos, Type found, Type req)
	{
		if (req.tag == ERROR)
		{
			return req;
		}
		else if (req.tag == NONE)
		{
			return found;
		}
		else if (found.isAssignable(req))
		{
			return found;
		}
		else
		{
			String problem;
			if (found.tag <= DOUBLE && req.tag <= DOUBLE)
				problem = log.getLocalizedString("possible.loss.of.precision");
			else
				problem = log.getLocalizedString("incompatible.types");
			return typeError(pos, problem, found, req);
		}
	}
	
	public void checkNonCyclic(int pos, Type type)
	{
		
	}

	/**
	 * Report a failure to complete a class.
	 * @param pos Position to be used for error reporting.
	 * @param ex The failure to report.
	 */
	public Type completionError(int pos, CompletionFailure ex)
	{
		log.error(pos, "cant.access", ex.sym.toString(), ex.errmsg);
		return syms.errType;
	}

    /**
     * Check that a given type can be cast to a given targetAbstractLayer type.
     *  Return the result of the cast.
     *  @param pos        Position to be used for error reporting.
     *  @param found      The type that is being cast.
     *  @param req        The targetAbstractLayer type of the cast.
     * @return 
     */
	public Type checkCastable(int pos, Type found, Type req)
    {
        if (found.isCastable(req)) {
            checkCompatible(pos, found, req);
            return req;
        } else {
            return typeError(pos, Log.getLocalizedString("inconvertible.types"),
                    found, req);
        }
    }
	
    /**
     * Check that (arrays of) interfaces do not each define a method
     *  with same name and arguments but different return types.
     *  If either argument type is not an (array of) interface type, do
     *  nothing.
     *  @param pos          Position to be used for error reporting.
     *  @param t1           The first argument type.
     *  @param t2           The second argument type.
     */
   public boolean checkCompatible(int pos, Type t1, Type t2) {
       if (t1.tag == ARRAY && t2.tag == ARRAY) {
           checkCompatible(pos, t1.elemType(), t2.elemType());
       }
       return true;
   }
}
