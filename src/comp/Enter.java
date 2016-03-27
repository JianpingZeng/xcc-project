package comp;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import exception.CompletionFailure;
import symbol.Scope;
import symbol.Symbol;
import symbol.Symbol.MethodSymbol;
import symbol.Symbol.VarSymbol;
import symbol.SymbolKinds;
import type.Type;
import type.TypeTags;
import utils.Context;
import utils.Log;
import utils.Name;
import ast.*;
import ast.Tree.*;

/**
 * This class enters symbols for all encountered definition into the symbol
 * table.
 * 
 * It operates over tree with descending recursively down for top level variable
 * definition, type definition and method definition.
 * 
 * 
 * @author Jianping Zeng <z1215jping@hotmail.com>
 * @version 2016年1月14日
 */
public class Enter extends ASTVisitor implements TypeTags, SymbolKinds, Flags
{

	private static final Context.Key enterKey = new Context.Key();
	private Log log;
	private Name.Table names;
	private Check check;
	private Attr attr;
	
	/**
	 * Where all of unattributed method in the stage of scope entering stores 
	 */
	private Todo<Env> todo;

	/**
	 * The current context environment.
	 */
	private Env env = null;
	
	/**
	 * Maps method symbol to env attached to it.
	 */
	HashMap<MethodSymbol, Env> methodEnvs;
	
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

	public Enter(Context context)
	{
		super();
		context.put(enterKey, this);
		names = Name.Table.instance(context);
		log = Log.instance(context);
		check = Check.instance(context);
		attr = Attr.instance(context);
		todo = Todo.instance(context);
	}

	/**
	 * A entry for entering sorts of symbols into corresponding scope. Every
	 * scope is attached to adaptable symbol.
	 * 
	 * @param trees
	 */
	public void complete(List<Tree> trees)
	{
		try
        {
			for (Tree toplevel : trees)
				toplevel.accept(this);
			
			for(java.util.Map.Entry<MethodSymbol, Env> entity 
					: methodEnvs.entrySet())
				todo.add(entity.getValue());			
        }
        catch (CompletionFailure e)
        {
        	e.printStackTrace();
        }
	}
	
	/**
	 * Entering into a new scope for global symbols or local symbols.
	 * 
	 * @param env	the parent scope of that new local scope.
	 * 
	 * @return	a new local scope.
	 */
	private symbol.Scope enterScope(Env env)
	{
		// save current scope
		return (env.tree.tag == Tree.TOPLEVEL) ? 
				((TopLevel)env.tree).topScope : 
					((AttrContext)env.info).scope;
	}
	
	/**
	 * Checks the unique symbol in the given scope.
	 * 
	 * @param pos	The position of symbol to be error reported.
	 * @param sym		The symbol.
	 * @param encScope	The given scope
	 * @return
	 */
	private boolean checkUnique(int pos, Symbol sym, Scope encScope)
	{
		Scope.Entry e = encScope.lookup(sym.name);		
		// look up the symbol whose name as same as given symbol at current scope
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
	 * Reports a duplicate symbol error at given postion of source file.
	 * 
	 * @param pos	The position to be error reported.
	 * @param sym	The symbol to be error reported. 
	 */
	private void duplicateError(int pos, Symbol sym)
	{
		if (! sym.type.isErronuous()) 
		{
			log.error(pos, "already.defined", sym.toString(), sym.location());
		}
	}
	
	
	/**
	 * Constructs a method signature according to return type, 
	 * parameters list of method.
	 * 
	 * @param params	The parameters list of this method.
	 * @param rettype	The return value' type.
	 * @param env		The method's local context environment.
	 * @return 
	 */
	private Type signature(java.util.List<Tree> params, Tree rettype, Env env) 
	{
		java.util.List<Type> argbuf = new ArrayList<>();
		for (Tree t : params)
			argbuf.add(attr.attribType(t, env));
		Type resType = rettype == null ? new Type(VOID, null) 
			: attr.attribType(rettype, env);
		Type mtype = new type.MethodType(resType, argbuf, null);
		return mtype;
	} 
	
	
	/**
	 * Enters the symbol of variable definition into current scope used by 
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
		symbol.Scope encScope = enterScope(env);
		
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
	 * Enters the symbol of method definition into current scope used by 
	 * current context environment. The scope which this method definition
	 * will be entered in is derived from current context environment rather
	 * than other method.
	 * 
	 * @param def	method definition tree node.
	 */
	private void enterMethodDef(MethodDef def)
	{
		// entering into the top level scope
		symbol.Scope encScope = enterScope(env);
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
     * @param tree
     * @return
     */
    Env topLevelEnv(TopLevel tree)
    {
    	Env localEnv = new Env(tree, new AttrContext());
    	localEnv.toplevel  = tree;
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
		// initialize a top level context envrionment
		this.env = topLevelEnv(tree);
		for (Tree def : tree.defs)
		{
			def.accept(this); 
		}
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
	}

	@Override
    public void visitImport(Import tree)
    {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void visitSkip(Skip tree)
    {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void visitBlock(Block tree)
    {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void visitIf(If tree)
    {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void visitSwitch(Switch tree)
    {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void visitForLoop(ForLoop tree)
    {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void visitBreak(Break tree)
    {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void visitContinue(Continue tree)
    {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void visitGoto(Goto tree)
    {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void visitDoLoop(DoLoop tree)
    {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void visitWhileLoop(WhileLoop tree)
    {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void visitCase(Case tree)
    {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void visitLabelled(Labelled tree)
    {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void visitReturn(Return tree)
    {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void visitSelect(Select tree)
    {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void visitApply(Apply tree)
    {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void visitAssign(Assign tree)
    {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void visitExec(Exec tree)
    {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void visitConditional(Conditional tree)
    {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void visitParens(Parens tree)
    {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void visitAssignop(Assignop tree)
    {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void visitUnary(Unary tree)
    {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void visitBinary(Binary tree)
    {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void visitTypeCast(TypeCast tree)
    {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void visitIndexed(Indexed tree)
    {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void visitTypeArray(TypeArray tree)
    {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void visitTypeIdent(TypeIdent tree)
    {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void visitLiteral(Literal tree)
    {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void visitIdent(Ident tree)
    {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void visitNewArray(NewArray tree)
    {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void visitErroneous(Erroneous erroneous)
    {
	    // TODO Auto-generated method stub
	    
    }
}
