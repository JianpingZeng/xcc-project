package ast; 

import java.util.ArrayList;
import java.util.List;

import symbol.Symbol;
import symbol.Symbol.MethodSymbol;
import symbol.Symbol.VarSymbol;
import symbol.SymbolKinds;
import type.*;
import utils.*;
import ast.Tree.*;


/**
 * Factory class for trees
 */
public class TreeMaker implements Factory, SymbolKinds, TypeTags {

    /**
     * The context key for the tree factory.
     */
    private static final Context.Key treeMakerKey = new Context.Key();

    /**
     * Get the TreeMaker instance.
     */
    public static TreeMaker instance(Context context) {
        TreeMaker instance = (TreeMaker) context.get(treeMakerKey);
        if (instance == null)
            instance = new TreeMaker(context);
        return instance;
    }

    /**
      * The position at which subsequent trees will be created.
      */
    public int pos = Position.NOPOS;

    /**
     * The toplevel tree to which created trees belong.
     */
    public TopLevel toplevel;

    /**
     * The current name table.
     */
    private Name.Table names;

    /**
     * Create a tree maker with null toplevel and NOPOS as initial position.
     */
    private TreeMaker(Context context) {
        super();
        context.put(treeMakerKey, this);
        this.pos = Position.NOPOS;
        this.toplevel = null;
        this.names = Name.Table.instance(context);
    }

    /**
      * Create a tree maker with null toplevel and NOPOS as initial position.
      */
    public TreeMaker(TopLevel toplevel) {
        super();
        this.pos = Position.FIRSTPOS;
        this.toplevel = toplevel;
        this.names = toplevel.sourceFile.table;
    }

    /**
      * Reassign current position.
      */
    public TreeMaker at(int pos) {
        this.pos = pos;
        return this;
    }

    /**
      * Create given tree node at current position.
      */
    public TopLevel TopLevel(List<Tree> defs) {
        TopLevel tree = new TopLevel(defs, null, null);
        tree.pos = pos;
        return tree;
    }

    public Import Import(Tree qualid) {
        Import tree = new Import(qualid);
        tree.pos = pos;
        return tree;
    }


    public MethodDef MethodDef(long flags, Name name, Tree restype,
            List<Tree> params, Tree body) {
        MethodDef tree = new MethodDef(flags, name, restype, params, body, null);
        tree.pos = pos;
        return tree;
    }

    public VarDef VarDef(long flags, Name name, Tree vartype, Tree init) {
        VarDef tree = new VarDef(flags, name, vartype, init, null);
        tree.pos = pos;
        return tree;
    }
    
    public NewArray NewArray(Tree elemtype, List<Tree> dims, List<Tree> elems) {
        NewArray tree = new NewArray(elemtype, dims, elems);
        tree.pos = pos;
        return tree;
    }

    public Skip Skip() {
        Skip tree = new Skip();
        tree.pos = pos;
        return tree;
    }

    public Block Block(List<Tree> stats) {
        Block tree = new Block(stats);
        tree.pos = pos;
        return tree;
    }

    public DoLoop DoLoop(Tree body, Tree cond) {
        DoLoop tree = new DoLoop(body, cond);
        tree.pos = pos;
        return tree;
    }

    public WhileLoop WhileLoop(Tree cond, Tree body) {
        WhileLoop tree = new WhileLoop(cond, body);
        tree.pos = pos;
        return tree;
    }

    public ForLoop ForLoop(List<Tree> init, Tree cond, List<Tree> step, Tree body) {
        ForLoop tree = new ForLoop(init, cond, step, body);
        tree.pos = pos;
        return tree;
    }

    public Labelled Labelled(Name label, Tree body) {
        Labelled tree = new Labelled(label, body);
        tree.pos = pos;
        return tree;
    }

    public Switch Switch(Tree selector, List<Case> cases) {
        Switch tree = new Switch(selector, cases);
        tree.pos = pos;
        return tree;
    }

    public Case Case(List<Tree> values, List<Tree> stats) {
        Case tree = new Case(values, stats);
        tree.pos = pos;
        return tree;
    }

    public Conditional Conditional(Tree cond, Tree thenpart, Tree elsepart) {
        Conditional tree = new Conditional(cond, thenpart, elsepart);
        tree.pos = pos;
        return tree;
    }

    public If If(Tree cond, Tree thenpart, Tree elsepart) {
        If tree = new If(cond, thenpart, elsepart);
        tree.pos = pos;
        return tree;
    }

    public Exec Exec(Tree expr) {
        Exec tree = new Exec(expr);
        tree.pos = pos;
        return tree;
    }

    public Break Break() {
        Break tree = new Break(null);
        tree.pos = pos;
        return tree;
    }

    public Continue Continue() {
        Continue tree = new Continue(null);
        tree.pos = pos;
        return tree;
    }

    public Goto Goto(Name label)
    {
    	Goto go = new Goto(label, null);
    	go.pos = pos;
    	return go;
    }
    
    public Return Return(Tree expr) {
        Return tree = new Return(expr);
        tree.pos = pos;
        return tree;
    }

    public Apply Apply(Tree fn, List<Tree> args) {
        Apply tree = new Apply(fn, args);
        tree.pos = pos;
        return tree;
    }

    public Parens Parens(Tree expr) {
        Parens tree = new Parens(expr);
        tree.pos = pos;
        return tree;
    }

    public Assign Assign(Tree lhs, Tree rhs) {
        Assign tree = new Assign(lhs, rhs);
        tree.pos = pos;
        return tree;
    }

    public Assignop Assignop(int opcode, Tree lhs, Tree rhs) {
        Assignop tree = new Assignop(opcode, lhs, rhs, null);
        tree.pos = pos;
        return tree;
    }

    public Unary Unary(int opcode, Tree arg) {
        Unary tree = new Unary(opcode, arg, null);
        tree.pos = pos;
        return tree;
    }

    public Binary Binary(int opcode, Tree lhs, Tree rhs) {
        Binary tree = new Binary(opcode, lhs, rhs, null);
        tree.pos = pos;
        return tree;
    }

    public TypeCast TypeCast(Tree clazz, Tree expr) {
        TypeCast tree = new TypeCast(clazz, expr);
        tree.pos = pos;
        return tree;
    }

    public Indexed Indexed(Tree indexed, Tree index) {
        Indexed tree = new Indexed(indexed, index);
        tree.pos = pos;
        return tree;
    }

    public Select Select(Tree selected, Name selector) {
        Select tree = new Select(selected, selector, null);
        tree.pos = pos;
        return tree;
    }

    public Ident Ident(Name name) {
        Ident tree = new Ident(name, null);
        tree.pos = pos;
        return tree;
    }

    public Literal Literal(int tag, Object value) {
        Literal tree = new Literal(tag, value);
        tree.pos = pos;
        return tree;
    }

    public TypeIdent TypeIdent(int typetag) {
        TypeIdent tree = new TypeIdent(typetag);
        tree.pos = pos;
        return tree;
    }

    public TypeArray TypeArray(Tree elemtype) {
        TypeArray tree = new TypeArray(elemtype);
        tree.pos = pos;
        return tree;
    }   

    public Erroneous Erroneous() {
        Erroneous tree = new Erroneous();
        tree.pos = pos;
        return tree;
    }

    /**
      * Create an identifier from a symbol.
      */
    public Tree Ident(Symbol sym) {
        return new Ident(sym.name, sym).setPos(pos).setType(sym.type);
    }

    /**
      * Create a selection node from a qualifier tree and a symbol.
      *  @param base   The qualifier tree.
      */
    public Tree Select(Tree base, Symbol sym) {
        return new Select(base, sym.name, sym).setPos(pos).setType(sym.type);
    }

    /**
      * Create an identifier that refers to the variable declared in given variable
      *  declaration.
      */
    public Tree Ident(VarDef param) {
        return Ident(param.sym);
    }

    /**
      * Create a list of identifiers referring to the variables declared
      *  in given list of variable declarations.
      */
    public List<Ident> Idents(List<VarDef> params) {
        List<Ident> ids = new ArrayList<Tree.Ident>();
        for (VarDef def : params)
        	ids.add((ast.Tree.Ident) Ident(def));
        return ids;
    }



    /**
      * Create a method invocation from a method tree and a list of argument trees.
      */
    public Tree App(Tree meth, List<Tree> args) {
        return Apply(meth, args).setType(meth.type.returnType());
    }

    /**
      * Create a tree representing given type.
      */
    public Tree Type(type.Type t) {
        if (t == null)
            return null;
        Tree tp;
        switch (t.tag) {
        case BYTE:

        case CHAR:

        case SHORT:

        case INT:

        case LONG:

        case FLOAT:

        case DOUBLE:

        case BOOL:

        case VOID:
            tp = TypeIdent(t.tag);
            break;

        case ARRAY:
            tp = TypeArray(Type(t.elemType()));
            break;

        case ERROR:
            tp = TypeIdent(ERROR);
            break;

        default:
            throw new AssertionError("unexpected type: " + t);

        }
        return tp.setType(t);
    }

    /**
      * Create a list of trees representing given list of types.
      */
    public List<Tree> Types(List<type.Type> ts) {
        List<Tree> types = new ArrayList<>();
        for (type.Type t : ts)
        	types.add(Type(t));
        return types;
    }

    /**
      * Create a variable definition from a variable symbol and an initializer
      *  expression.
      */
    public VarDef VarDef(symbol.Symbol.VarSymbol v, Tree init) {
        return (VarDef) new VarDef(v.flags, v.name, Type(v.type), init,
                v).setPos(pos).setType(v.type);
    }

    /**
      * Create a method definition from a method symbol and a method body.
      */
    public MethodDef MethodDef(symbol.Symbol.MethodSymbol m, Block body) {
        return MethodDef(m, m.type, body);
    }

    /**
      * Create a method definition from a method symbol, method type
      *  and a method body.
      */
    public MethodDef MethodDef(MethodSymbol m, type.Type mtype, Block body) {
        return (MethodDef) new MethodDef(m.flags, m.name,
                Type(mtype.returnType()), Params(mtype.paramTypes(), m), 
                body, m).setPos(pos).setType(mtype);
    }

    /**
      * Create a value parameter tree from its name, type, and owner.
      */
    public VarDef Param(Name name, Type argtype, Symbol owner) {
        return VarDef(new VarSymbol(0, name, argtype), null);
    }

    /**
      * Create a a list of value parameter trees x0, ..., xn from a list of
      *  their types and an their owner.
      */
    public List<Tree> Params(List<type.Type> argtypes, Symbol owner) {
        List<Tree> params = new ArrayList<>();
        int i = 0;
        for (type.Type t : argtypes)
        	params.add(Param(paramName(i++), t, owner));        
        return params;
    }

    /**
      * Wrap a method invocation in an expression statement or return statement,
      *  depending on whether the method invocation expression's type is void.
      */
    public Tree Call(Tree apply) {
        return apply.type.tag == VOID ? (Tree) Exec(apply) : (Tree) Return(apply);
    }

    /**
      * Construct an assignment from a variable symbol and a right hand side.
      */
    public Tree Assignment(Symbol v, Tree rhs) {
        return Exec(Assign(Ident(v), rhs).setType(v.type));
    }

    /**
      * The name of synthetic parameter id `i'.
      */
    public Name paramName(int i) {
        return names.fromString("x" + i);
    }

    /**
      * The name of synthetic type parameter id `i'.
      */
    public Name typaramName(int i) {
        return names.fromString("A" + i);
    }
}