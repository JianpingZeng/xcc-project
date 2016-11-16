package frontend.comp;

import java.util.Arrays;
import java.util.List;

import frontend.symbol.Scope;
import frontend.symbol.Scope.Entry;
import frontend.symbol.Symbol;
import frontend.symbol.SymbolKinds;
import frontend.type.Type;
import frontend.type.TypeClass;
import tools.Context;
import tools.Log;
import tools.Name;
import frontend.ast.Flags;

/**
 * A helpful utility class for resovling method getName and frontend.type.
 *
 * @author JianpingZeng
 * @version 1.0
 */
public class Resolve implements TypeClass, SymbolKinds, Flags
{
	private TreeInfo treeinfo;
	private static final Context.Key resolveKey = new Context.Key();

	private Name.Table names;
	private Log log;
	private Symtab syms;
	/**
	 * Error kinds that complement the constants in Kinds
	 */
	static final int AMBIGUOUS = 256;
    static final int ABSENT_VAR = 257;
    static final int WRONG_MTHS = 258;
    static final int WRONG_MTH = 259;
	static final int ABSENT_MTH = 260;
	
	public static Resolve instance(Context context)
	{
		Resolve instance = (Resolve) context.get(resolveKey);
		if (instance == null) instance = new Resolve(context);
		return instance;
	}

	public Resolve(Context context)
	{
		context.put(resolveKey, this);
		this.treeinfo = TreeInfo.instance(context);
		this.names = Name.Table.instance(context);
		this.log = Log.instance(context);
		this.syms = Symtab.instance(context);
        varNotFound = new ResolveError(ABSENT_VAR, syms.errSymbol, "variable not found");
        wrongMethod = new ResolveError(WRONG_MTH, syms.errSymbol, "method not found");
        wrongMethods = new ResolveError(WRONG_MTHS, syms.errSymbol, "wrong methods");
        methodNotFound = new ResolveError(ABSENT_MTH, syms.errSymbol, "method not found");
	}

	/**
	 * A localized string describing a given kind.
	 */
	static String kindName(int kind)
	{
		switch (kind)
		{

			case TYP:
				return Log.getLocalizedString("kindname.class");

			case VAR:
				return Log.getLocalizedString("kindname.variable");

			case VAL:
				return Log.getLocalizedString("kindname.value");

			case MTH:
				return Log.getLocalizedString("kindname.method");

			default:
				return Log.getLocalizedString("kindname.default",
				        Integer.toString(kind));

		}
	}

	/**
	 * A localized string describing a given set of kinds.
	 */
	static String kindNames(int kind)
	{
		String[] s = new String[4];
		int i = 0;
		if ((kind & VAL) != 0)
		    s[i++] = ((kind & VAL) == VAR) ? Log
		            .getLocalizedString("kindname.variable") : Log
		            .getLocalizedString("kindname.value");
		if ((kind & MTH) != 0)
		    s[i++] = Log.getLocalizedString("kindname.method");
		if ((kind & TYP) != 0)
		    s[i++] = Log.getLocalizedString("kindname.class");
		String names = "";
		for (int j = 0; j < i - 1; j++)
			names = names + s[j] + ", ";
		if (i >= 1)
			names = names + s[i - 1];
		else
			names = Log.getLocalizedString("kindname.default",
			        Integer.toString(kind));
		return names;
	}

	/**
	 * Resolve operator.
	 * 
	 * @param pos The position to use for error reporting.
	 * @param optag The tag of the operation tree.
	 * @param env The environment current at the operation.
	 * @param argtypes The types of the operands.
	 */
	Symbol resolveOperator(int pos, int optag, Env env, List<Type> argtypes)
	{
		// obtains the getName of this operator
		Name name = treeinfo.operatorName(optag);
		
		return access(findMethod(env, name, argtypes), pos, name, argtypes);
	}

	/**
	 * IfStmt `sym' is a bad frontend.symbol: report error and return errSymbol else pass
	 * through unchanged, additional arguments duplicate what has been used in
	 * trying to find the frontend.symbol (--> flyweight pattern). This improves
	 * performance since we expect misses to happen frequently.
	 *
	 * @param sym The frontend.symbol that was found, or a ResolveError.
	 * @param pos The position to use for error reporting.
	 * @param site The original frontend.type from where the selection took place.
	 * @param name The frontend.symbol's getName.
	 * @param argtypes The invocation's value parameters, if we looked for a
	 *            method.
	 */
	Symbol access(Symbol sym, int pos, Name name, List<Type> argtypes)
	{
		if (sym.kind >= AMBIGUOUS)
		{
			if (!Type.isErroneous(argtypes))
			    ((ResolveError) sym).report(log, pos, name, argtypes);
			do
			{
				sym = ((ResolveError) sym).sym;
			} while (sym.kind >= AMBIGUOUS);

			if (sym == syms.errSymbol)
			    sym = new Type.ErrorType(name, syms.noSymbol).tsym;
		}
		return sym;
	}

	/**
	 * Find best qualified method matching given getName, frontend.type and value
	 * parameters.
	 * 
	 * @param env The current environment.
	 * @param site The original frontend.type from where the selection takes place.
	 * @param name The method's getName.
	 * @param argtypes The method's value parameters.
	 */
	Symbol findMethod(Env env, Name name, List<Type> argtypes)
	{
		Symbol bestSoFar = methodNotFound;
		Scope.Entry e = ((AttrContext) env.info).scope.lookup(name);
		for (; e.scope != null; e = e.next())
		{
			if (e.sym.kind == MTH)
			{
				bestSoFar = e.sym;
			}
		}
		return bestSoFar;
	}

	/**
	 * Resolve unary operator.
	 * 
	 * @param pos The position to use for error reporting.
	 * @param optag The tag of the operation tree.
	 * @param env The environment current at the operation.
	 * @param argtype The frontend.type of the LIROperand.
	 */
	public Symbol resolveUnaryOperator(int pos, int optag, Env env, Type argtype)
	{
		// obtains the getName of this operator
		Name name = treeinfo.operatorName(optag);
		List<Type> argtypes =  Arrays.asList(argtype);
		return access(findOperator(env, name, argtypes), pos, name, argtypes);		
	}
	
	/**
	 * Resolve unary operator.
	 * 
	 * @param pos The position to use for error reporting.
	 * @param optag The tag of the operation tree.
	 * @param env The environment current at the operation.
	 * @param left The frontend.type of the first LIROperand.
	 * @param right	The frontend.type of the second LIROperand.
	 */
	public Symbol resolveBinaryOperator(int pos, int optag, Env env, Type left, Type right)
	{
		// obtains the getName of this operator
		Name name = treeinfo.operatorName(optag);
		List<Type> argtypes =  Arrays.asList(left, right);
		return access(findOperator(env, name, argtypes), pos, name, argtypes);
		
	}
	
	public Symbol findOperator(Env env, Name name, List<Type> argtypes)
	{
		int numbers = argtypes.size(); 
		Symbol bestSoFar = syms.errSymbol;
		if (numbers <= 0 || numbers > 2)
		{
			return bestSoFar;
		}
		else if(numbers == 1)
		{
			Entry e = syms.predefTopLevelSymbol.topScope.lookup(name);
			for (; e.scope != null; e = e.next())
			{
				if (e.sym.kind == MTH)
					bestSoFar = selectBest(argtypes, e.sym, bestSoFar);
			}
				
		}
		return bestSoFar;
	}
	
	private Symbol selectBest(List<Type> argtypes, Symbol sym, Symbol bestSoFar)
	{
		if (sym.kind == ERR)
			return bestSoFar;
		assert sym.kind < AMBIGUOUS;		

		boolean res1 = false, res2 = false;
		if ( isSameType(bestSoFar.type.paramTypes(), sym.type.paramTypes())
						&& 
						(res2 = isSubType(argtypes, bestSoFar.type.paramTypes()))) 
			return new AmbiguityError(sym, bestSoFar);
		res1 = isSubType(argtypes, sym.type.paramTypes());
		
		if (res1 || res2)
		{
			bestSoFar = isSubType(sym.type.paramTypes(), bestSoFar.type.paramTypes())
					? sym : bestSoFar;
		}		
		
		return bestSoFar;
	}
	
	/**
	 * Is every element of src list as same as frontend.type of one from dest list?
	 * return true if succeed, otherwise return false.
	 * 
	 * @param src
	 * @param dest
	 * @return
	 */
	private boolean isSameType(List<Type> src, List<Type> dest)
	{
		if (src.size() != dest.size())
			return false;
		boolean result = true;
		for (int idx = 0; idx < src.size(); idx++)
		{
			if ( !src.get(idx).isSameType(dest.get(idx)))
			{result = false; break;}
		}
		return result;
	}
	
	/**
	 * Is every element of src list a subtype of frontend.type of one from dest list?
	 * return true if succeed, otherwise return false.
	 * 
	 * @param src
	 * @param dest
	 * @return
	 */
	private boolean isSubType(List<Type> src, List<Type> dest)
	{
		if (src.size() != dest.size())
			return false;
		boolean result = true;
		for (int idx = 0; idx < src.size(); idx++)
		{
			if ( !src.get(idx).isSubType(dest.get(idx)))
			{result = false; break;}
		}
		return result;
	}
	
	
    /**
     * error symbols, which are returned when resolution fails
     */
    final ResolveError varNotFound;
    final ResolveError wrongMethod;
    final ResolveError wrongMethods;
    final ResolveError methodNotFound;
	
    /**
     * A localized string describing the kind of a missing frontend.symbol, given an
     *  error kind.
     */
   static String absentKindName(int kind) {
       switch (kind) {
       case ABSENT_VAR:
           return Log.getLocalizedString("kindname.variable");

       case WRONG_MTHS:
       case WRONG_MTH:
       case ABSENT_MTH:
           return Log.getLocalizedString("kindname.method");

       default:
           return Log.getLocalizedString("kindname.identifier");

       }
   }
   
   /**
    * An environment is "static" if its static level is greater than
    *  the one of its outerLoop environment
    */
   static boolean isStatic(Env env) {
       return ((AttrContext) env.info).staticLevel >
               ((AttrContext) env.outer.info).staticLevel;
   }

	/**
	 * Root class for resolve errors. Instances of this class indicate
	 * "Symbol not found". Instances of subclass indicate other errors.
	 */
	private static class ResolveError extends Symbol implements TypeClass
	{

		ResolveError(int kind, Symbol sym, String debugName)
		{
			super(kind, null, null);
			this.debugName = debugName;
			this.sym = sym;
		}

		/**
		 * The getName of the kind of error, for debugging only.
		 */
		final String debugName;

		/**
		 * The frontend.symbol that was determined by resolution, or errSymbol if none
		 * was found.
		 */
		final Symbol sym;

		/**
		 * The frontend.symbol that was a close mismatch, or null if none was found.
		 * wrongSym is currently set if a simgle method with the correct getName,
		 * but the wrong parameters was found.
		 */
		Symbol wrongSym;

		/**
		 * Print the (debug only) getName of the kind of error.
		 */
		public String toString()
		{
			return debugName + " wrongSym=" + wrongSym;
		}

		/**
		 * Update wrongSym and return this
		 */
		ResolveError setWrongSym(Symbol sym)
		{
			this.wrongSym = sym;
			return this;
		}

		/**
		 * Report error.
		 * @param log The error log to be used for error reporting.
		 * @param pos The position to be used for error reporting.
		 * @param name The getName of the frontend.symbol to be resolved.
		 * @param argtypes The invocation's value parameters, if we looked for a
		 *            method.
		 */
		void report(Log log, int pos, Name name, List<Type> argtypes)
		{
			if (name != name.table.error)
			{
				String kindname = absentKindName(kind);
				String idname = name.toString();
				String args = "";
				if (kind >= WRONG_MTHS && kind <= ABSENT_MTH)
				{
					if (isOperator(name))
					{
						log.error(pos, "operator.cant.be.applied",
						        name.toString(), Type.toStringList(argtypes));
						return;
					}
					args = "(" + Type.toStringList(argtypes) + ")";
				}
				else
					log.error(pos, "cannot.resolve", kindname, idname, args);
			}
		}

		/**
		 * A getName designates an operator if it consists of a non-empty sequence
		 * of operator symbols +-~!/*%&|^<>=
		 */
		boolean isOperator(Name name)
		{
			int i = 0;
			while (i < name.len && "+-~!*/%&|^<>=".indexOf(name.byteAt(i)) >= 0)
				i++;
			return i > 0 && i == name.len;
		}
	}

	 /**
     * Resolve error class indicating an ambiguous reference.
     */
   static class AmbiguityError extends ResolveError {
       Symbol sym1;
       Symbol sym2;

       AmbiguityError(Symbol sym1, Symbol sym2) {
           super(AMBIGUOUS, sym1, "ambiguity error");
           this.sym1 = sym1;
           this.sym2 = sym2;
       }

       /**
         * Report error.
         *  @param log       The error log to be used for error reporting.
         *  @param pos       The position to be used for error reporting.
         *  @param name      The getName of the frontend.symbol to be resolved.
         *  @param argtypes  The invocation's value parameters,
         *                   if we looked for a method.
         */
       void report(Log log, int pos, Name name, List argtypes) {
           AmbiguityError pair = this;
           while (true) {
               if (pair.sym1.kind == AMBIGUOUS)
                   pair = (AmbiguityError) pair.sym1;
               else if (pair.sym2.kind == AMBIGUOUS)
                   pair = (AmbiguityError) pair.sym2;
               else
                   break;
           }
           Name sname = pair.sym1.name;
           log.error(pos, "ref.ambiguous", sname.toString(),
                   kindName(pair.sym1.kind), pair.sym1.toString(),
                   kindName(pair.sym2.kind),
                   pair.sym2.toString());
       }
   }
}
