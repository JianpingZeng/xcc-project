package frontend.comp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import frontend.exception.CompletionFailure;
import frontend.symbol.Scope;
import frontend.symbol.Symbol;
import frontend.symbol.Symbol.MethodSymbol;
import frontend.symbol.Symbol.TopLevelSymbol;
import frontend.symbol.VarSymbol;
import frontend.symbol.SymbolKinds;
import frontend.type.FunctionType;
import frontend.type.Type;
import frontend.type.TypeClass;
import tools.Context;
import tools.Log;
import tools.Name;
import frontend.ast.*;
import frontend.ast.Tree.*;

/**
 * <p>This class enters symbols for all encountered definition into the frontend.symbol
 * table. The pass consists of two phases, orgnized as follows:
 *
 * <p>In the first stage, all global variable definition and function signature
 * (except that function body) are entered into their enclosing scope.
 * <p>In the second stage, all of function body to be completed desending recursively
 * down.
 *
 * @author JianpingZeng
 * @version 1.0
 */
public class Enter extends StmtVisitor implements TypeClass, SymbolKinds, Flags
{

	private static final Context.Key enterKey = new Context.Key();
	private Log log;
	private Name.Table names;
	private Check check;
	private Attr attr;
	
	/**
	 * Where all of unattributed method in the stage of scope entering stores.
	 *
	 * <p>The {@code enter} class serves as entering all symbols, such as funct
	 * ion declaration or variable definition etc, which consists of two stages,
	 * first entering all function header except function body into scope, then
	 * all of function body could be attributed after in second stage.
	 */
	private Todo<Env> todo;

	/**
	 * The current context environment.
	 */
	private Env env = null;
	
	/**
	 * Maps method frontend.symbol to env attached to it.
	 * <p>This map is used for memorizing the method frontend.symbol and corresponding
	 * context environment, so that the uses of function can be proceded before
	 * function declaration or definition.
	 */
	HashMap<MethodSymbol, Env> methodEnvs;

	boolean completionEnabled = true;

	LinkedList<MethodSymbol> uncompleted;
	/**
	 * A singleton method for constructing merely instance of this class.
	 * 
	 * @param context
	 * @return
	 */
	public static Enter instance(Context context)
	{
		Enter instance = (Enter) context.get(enterKey);
		if (instance == null)
		{
			instance = new Enter(context);
		}
		return instance;
	}
	/**
	 * Constructs a new instance of {@code Enter}.
	 * @param context   A instance of {@code Context}.
	 */
	public Enter(Context context)
	{
		super();
		context.put(enterKey, this);
		names = Name.Table.instance(context);
		log = Log.instance(context);
		check = Check.instance(context);
		attr = Attr.instance(context);
		todo = Todo.instance(context);
		uncompleted = new LinkedList<>();
	}

	public void main(List<Tree> trees)
	{
		complete(trees, null);
	}

	public void main(Tree tree)
	{
		complete(tree, null);
	}

	/**
	 * A entry method for entering sorts of symbols into corresponding scope.
	 * Every scope is attached to adaptable frontend.symbol.
	 *
	 * @param tree
	 */
	private void complete(Tree tree, TopLevelSymbol symbol)
	{
		LinkedList<MethodSymbol> prevUncompleted = uncompleted;
		try
		{
			globalMemberEnter(tree, null);
			while (!uncompleted.isEmpty())
			{
				MethodSymbol methodSymbol = uncompleted.removeFirst();
				if (symbol == null || prevUncompleted == null)
					todo.add(methodEnvs.get(methodSymbol));
				else
					prevUncompleted.addLast(methodSymbol);
			}
		}
		catch (CompletionFailure e)
		{
			e.printStackTrace();
		}
		finally
		{
			uncompleted = prevUncompleted;
		}
	}

	/**
	 * A entry method for entering sorts of symbols into corresponding scope.
	 * Every scope is attached to adaptable frontend.symbol.
	 * 
	 * @param trees
	 */
	private void complete(List<Tree> trees, TopLevelSymbol symbol)
	{
		LinkedList<MethodSymbol> prevUncompleted = uncompleted;
		try
        {
	        globalMemberEnter(trees, null);
	        while (!uncompleted.isEmpty())
	        {
		        MethodSymbol methodSymbol = uncompleted.removeFirst();
		        if (symbol == null || prevUncompleted == null)
			        todo.add(methodEnvs.get(methodSymbol));
		        else
		            prevUncompleted.addLast(methodSymbol);
	        }
        }
        catch (CompletionFailure e)
        {
        	e.printStackTrace();
        }
		finally
		{
			uncompleted = prevUncompleted;
		}
	}

	/**
	 * Enters global element,like global variable or function declaration into
	 * enclosing scope.
	 * @param trees
	 * @param env
	 */
	private void globalMemberEnter(List<Tree> trees, Env env)
	{
		for (Tree tree : trees)
			globalMemberEnter(tree, env);
	}

	private Type globalMemberEnter(Tree tree, Env env)
	{
		Env preEnv = this.env;
		try
		{
			this.env = env;
			tree.accept(this);
			return result;
		}
		catch (CompletionFailure ex)
		{
			return check.completionError(tree.pos, ex);
		}
		finally
		{
			this.env = preEnv;
		}
	}

	/**
	 * Entering into a new scope for global symbols or local symbols.
	 * 
	 * @param env	the parent scope of that new local scope.
	 * 
	 * @return	a new local scope.
	 */
	private Scope enterScope(Env env)
	{
		// save current scope
		return (env.tree.tag == Tree.TopLevelClass) ?
				((TopLevel)env.tree).topScope : 
					((AttrContext)env.info).scope;
	}
	
	/**
	 * Checks the unique frontend.symbol in the given scope.
	 * 
	 * @param pos	The position of frontend.symbol to be error reported.
	 * @param sym		The frontend.symbol.
	 * @param encScope	The given scope
	 * @return
	 */
	private boolean checkUnique(int pos, Symbol sym, Scope encScope)
	{
		Scope.Entry e = encScope.lookup(sym.name);		
		// look up the frontend.symbol whose getName as same as given frontend.symbol at current scope
		for (; e.scope == encScope; e = e.next())
		{
			if (sym != e.sym && sym.kind == e.sym.kind 
					&& sym.name != names.error)
			{
				duplicateError(pos, e.sym);
				return false;
			}				
		}
		return true;
	}
	
	/**
	 * Reports a duplicate frontend.symbol error at given postion of source file.
	 * 
	 * @param pos	The position to be error reported.
	 * @param sym	The frontend.symbol to be error reported.
	 */
	private void duplicateError(int pos, Symbol sym)
	{
		if (! sym.type.isErronuous()) 
		{
			log.error(pos, "already.defined", sym.toString(), sym.location());
		}
	}
	
	
	/**
	 * Constructs a method signature according to return frontend.type,
	 * parameters list of method.
	 * 
	 * @param params	The parameters list of this method.
	 * @param rettype	The return value' frontend.type.
	 * @param env		The method's local context environment.
	 * @return 
	 */
	private Type signature(java.util.List<Tree> params, Tree rettype, Env env) 
	{
		java.util.List<Type> argbuf = new ArrayList<>();
		for (Tree t : params)
			argbuf.add(attr.attribType(t, env));
		Type resType = rettype == null ? new Type(Void, null)
			: attr.attribType(rettype, env);
		Type mtype = new FunctionType(resType, argbuf, null);
		return mtype;
	} 
	
	
	/**
	 * Enters the frontend.symbol of variable definition into current scope used by
	 * current context environment. The scope that this variable definition
	 * will be entered in is derived from current context environment rather
	 * than other method.
	 * 
	 * @param def	variable definition tree node.
	 */
	private void enterVarDef(VarDef def)
	{
		// firstly, the current used environment be acquired
		// secondly, corresponding scope also gotten 
		Scope encScope = enterScope(env);
		
		VarSymbol v = new VarSymbol(0, def.name, def.varType.type);
		v.flags = check.checkFlags(def.pos, def.flags, v);
		def.sym = v;
		// this variable definition has initializer
		if (def.init != null) 
		{
			v.flags |= HASINIT;
			if ((v.flags & CONST) != 0)
				v.constValue = null;			
		}
		// check variable definition unique
		if (checkUnique(def.pos, v, encScope))
		{
			encScope.enter(v);
		}
		v.pos = def.pos;
	}
	
	/**
	 * Enters the frontend.symbol of method definition into current scope used by
	 * current context environment. The scope which this method definition
	 * will be entered in is derived from current context environment rather
	 * than other method.
	 * 
	 * @param def	method definition tree node.
	 */
	private void enterMethodDef(MethodDef def)
	{
		// entering into the top level scope
		Scope encScope = enterScope(env);
		MethodSymbol m = new MethodSymbol(0, def.name, null);
		m.flags = check.checkFlags(def.pos, def.flags, m);
		def.sym = m;
		
		Env localEnv = methodEnv(def, env);
		// for second phase
		methodEnvs.put(m, localEnv);
		
		m.type = signature(def.params, def.rettype, localEnv);
		((AttrContext)localEnv.info).scope.leave();
		
		if (checkUnique(def.pos, m, encScope))
		{
			encScope.enter(m);
		}
		uncompleted.addLast(m);
	}
	
	/**
	 * Enter a fresh environment for method body.
	 * 
	 * @param tree	The method definition to be attributed
	 * @param topEnv	The environment current outside of method definition
	 * @return
	 */
	public Env methodEnv(MethodDef tree, Env topEnv)
	{
		Env localEnv = env.dup(tree, 
				((AttrContext)topEnv.info).dup(
						((AttrContext)topEnv.info).scope.dupUnshared()));
		// given tree attached into local envrinment for method body
		localEnv.tree = tree;
		if((tree.flags & STATIC) != 0)
			((AttrContext)localEnv.info).staticLevel++;
		return localEnv;
	}
	
	/**
	 * Create a fresh environment for a variable's initializer.
	 * if the variable is a global, the owner of the environment's
	 * scope is the variable itself, otherwise the owner is the 
	 * method enclosing the variable definition.
	 * 
	 * @param tree	The variable definition.
	 * @param env	The environment current outside of variable definition.
	 * @return
	 */
	public Env initEnv(VarDef tree, Env env)
	{
		Env localenv = env.dupto(new AttrContextEnv(tree, 
				((AttrContext)env.info).dup()));
		if ((tree.flags & STATIC) != 0)
			((AttrContext)env.info).staticLevel++;
		return localenv;
	}
	
    Type result = null;
	
    /**
     * Creates a fresh environment for TopLevel tree.
     * @param tree  The instance of {@code TopLevel}.
     * @return  A fresh context environment.
     */
    private Env topLevelEnv(TopLevel tree)
    {
    	Env localEnv = new Env(tree, new AttrContext());
    	localEnv.toplevel  = tree;
	    tree.topScope = new Scope(tree.compilation);
    	((AttrContext)localEnv.info).scope = tree.topScope;
    	return localEnv;
    }
    
	/**
	 * Entering global variable definition and method definition into
	 * corresponding scope.
	 */
	@Override
	public void visitTopLevel(TopLevel tree)
	{
		Name prev = log.useSource(tree.sourceFile);

		// initialize a top level context envrionment
		globalMemberEnter(tree.defs, topLevelEnv(tree));
		log.useSource(prev);
		result = null;
	}
	
	/**
	 * Visits variable definitions.
	 */
	@Override
	public void visitVarDef(VarDef tree)
	{
	    enterVarDef(tree);
	}
	
	/**
	 * Visits method definitions.
	 */
	@Override
	public void visitMethodDef(MethodDef tree)
	{	
		enterMethodDef(tree);
	}

	/**
	 * This method just for overriding method derived from super class.
	 */
	@Override
	public void visitTree(Tree that)
	{
		result = null;
	}

	@Override
    public void visitImport(Import tree)
    {

    }
}
