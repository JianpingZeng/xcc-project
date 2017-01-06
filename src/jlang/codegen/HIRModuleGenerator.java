package jlang.codegen;

import backend.hir.*;
import backend.intrinsic.Intrinsic;
import backend.target.TargetData;
import backend.type.FunctionType;
import backend.type.PointerType;
import backend.type.Type;
import backend.value.*;
import driver.Options;
import jlang.ast.Tree.*;
import jlang.sema.Decl;
import jlang.sema.Decl.FunctionDecl;
import jlang.sema.Decl.VarDecl;
import jlang.type.QualType;
import tools.Context;
import tools.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static backend.value.ConstantExpr.*;
import static backend.value.GlobalValue.LinkageType.*;

/**
 * <p>
 * This class just for converting abstract syntax tree into SSA-based
 * Intermediate Representation. At firstly, it constructs control flow graph
 * over AST. Afterwards, filling quad instruction into basic block over flow
 * graph.</p>
 * <p>
 * In this driver, all of source language constitute are converted into
 * SSA-based IR. So that entire optimizations rather no concerning about SSA are
 * associated. On the other hand, this driver is SSA-centeric.
 * </p>
 * <p>
 * The method this class taken is derived from Matthias
 * Braun(matthias.braun@kit.edu)' literature,Simple and Efficient Construction
 * of Static Single Assignment Form. The approach proposed by Braun is an
 * efficient for directly constructing SSA from abstract syntax tree or
 * bytecode.
 * </p>
 * <p>
 * When we performs that translating normal IR into IR in SSA form, all of global
 * variables will be ignored since the benefit of performing Memory-SSA can not
 * make up for loss of time and memory.
 *
 * Instead of a virtual register will be used for handling the global variable,
 * all of operations performed over global variable will takes effect on virtual
 * register.
 * </p>
 *
 * @author Xlous.zeng
 * @version 1.0
 */
public class HIRModuleGenerator
{
	private final static Context.Key AstToCfgKey = new Context.Key();
	private List<GlobalVariable> vars;
	private List<Function> functions;

    private TargetData theTargetData;

    private Function memCpyFn;
    private Function memMoveFn;
    private Function memSetFn;
    private Context ctx;
    private Options options;
	private Log logger;
    private Module m;
    private CodeGenTypes cgTypes;

    private HashMap<String, Constant> constantStringMap;

	/**
	 * Mapping of decl names (represented as unique
     * character pointers from either the identifier table or the set
     * of mangled names) to global variables we have already
     * emitted. Note that the entries in this map are the actual
     * globals and therefore may not be of the same type as the decl,
     * they should be bitcasted on retrieval.
     */
    private HashMap<String, GlobalValue> globalDeclMaps;

	public HIRModuleGenerator(Context context, Module m)
    {
		logger = Log.instance(context);
        vars = new ArrayList<>();
        functions = new ArrayList<>();
        options = Options.instance(context);
        this.m = m;
        cgTypes = new CodeGenTypes(this);
        constantStringMap = new HashMap<>();
        globalDeclMaps = new HashMap<>();
    }

    public CodeGenTypes getCodeGenTypes() { return cgTypes;}

    /**
     * Emits HIR code for a single top level declaration.
     * @param decl
     */
    public void emitTopLevelDecl(Decl decl)
    {
        // TODO if there are error occurred, stop code generation. 2016.10.29
        switch(decl.getDeclKind())
        {
            case FunctionDecl:
                // handle function declaration.
                emitGlobal(decl);
                break;
            case VarDecl:
                emitGlobal(decl);
                break;
            case CompilationUnitDecl:
            case BlockDecl:
            case FieldDecl:
            case ParamVar:
            case EnumConstant:
            case LabelDecl:
                // skip it.
                break;
            default:
                assert decl instanceof Decl.TypeDecl:"Unsupported decl kind!";
        }
    }

    /**
     * This method is called by {@linkplain BackendConsumer#handleTopLevelDecls(ArrayList)
     * handleTopLevelDecls}
     * to emit HIR code in procedures as follow:
     * <ol>
     *     <li>ensures that given {@code decl} either is {@linkplain VarDecl} or
     *     {@linkplain FunctionDecl}.</li>
     *     <li>calls {@linkplain #emitGlobal(Decl)} method to emite HIR code.</li>
     * </ol>
     * @param decl
     */
    private void emitGlobal(Decl decl)
    {
        if (decl instanceof FunctionDecl)
        {
            FunctionDecl fd = (FunctionDecl)decl;
            // skip function declaration.
            if (!fd.hasBody())
                return;

            emitGlobalFunctionDefinition(fd);
        }
        else
        {
            assert decl instanceof VarDecl;
            final VarDecl vd = (VarDecl)decl;
            assert vd.isFileVarDecl():"Cann't emit code for local variable!";

            // defer code generation if this variable is just declaration.
            if (vd.isThisDeclarationADefinition() != Decl.DefinitionKind.Definition)
                return;

            emitGlobalVarDefinition(vd);
        }
    }

    private void emitGlobalVarDefinition(VarDecl vd)
    {
        Constant init = null;
        QualType type = vd.getDeclType();

        if (vd.getInit() == null)
        {
            // This a tentative definition. tentative definitions are
            // implicitly initialized with 0.
            // They should never have incomplete type.
            assert type.getType().isIncompleteType():"Unexpected incomplete type!";
	        init = emitNullConstant(type);
        }
	    else
        {
	        init = emitConstantExpr(vd.getInit(), type, null);
	        if (init == null)
	        {
		        QualType t = vd.getInit().getType();
		        errorUnsupported(vd.getInit(), "static initializer");
		        init = UndefValue.get(getCodeGenTypes().convertType(t));
	        }
        }

	    Type initType = init.getType();
	    Constant entry = getAddrOfGlobalVar(vd, initType);
	    // Strip off a bitcast if we got one back.
	    if (entry instanceof ConstantExpr)
	    {
		    ConstantExpr ce = (ConstantExpr)entry;
		    assert ce.getOpCode() == Operator.BitCast
				    || ce.getOpCode() == Operator.GetElementPtr;
		    entry = ce.operand(0);
	    }

	    // Entry is now a global variable.
	    GlobalVariable gv = (GlobalVariable)entry;
	    gv.setInitializer(init);

	    // if it is safe to mark the global constant, do that.
	    gv.setConstant(false);
	    if (vd.getDeclType().isConstant())
		    gv.setConstant(true);

	    // set the appropriate linkage type.
	    if (vd.getStorageClass() == Decl.StorageClass.SC_static)
		    gv.setLinkage(InteralLinkage);
	    else if (!vd.hasExternalStorage() && !vd.hasInit())
	    {
		    gv.setLinkage(CommonLinkage);
		    gv.setConstant(false);
	    }
	    else
		    gv.setLinkage(ExternalLinkage);
    }

    public Constant getAddrOfFunction(FunctionDecl fd)
    {
        return getAddrOfFunction(fd, null);
    }

	/**
     * Return the address of the given function.  If Ty is
     * non-null, then this function will use the specified type if it has to
     * create it (this occurs when we see a definition of the function).
     * @param fd
     * @param ty
     * @return
     */
    public Constant getAddrOfFunction(FunctionDecl fd, Type ty)
    {
        if (ty == null)
            ty = getCodeGenTypes().convertType(fd.getDeclType());
        return getOrCreateFunction(fd.getDeclName(), ty, fd);
    }

	/**
     * Create and return an backend Function with the specified type. If there
     * is something in the module with the specified getName, return it potentially
     * bitcasted to the right type.
     *
     * If D is non-null, it specifies a decl that correspond to this.  This is used
     * to set the attributes on the function when it is first created.
     * @param name
     * @param type
     * @param fd
     * @return
     */
    private Constant getOrCreateFunction(String name, Type type, FunctionDecl fd)
    {
        GlobalValue entry = globalDeclMaps.get(name);
        if (entry != null)
        {
            if (entry.getType().getElemType() == type)
                return entry;

            // Make sure the result is of the correct type.
            Type ptrType = PointerType.get(type);
            return getBitCast(entry, ptrType);
        }

        // This function doesn't have a complete type(for example, return
        // type is an incomplete struct). Use a fake type instead.
        boolean isIncompleteFunction = false;
        if (!(type instanceof FunctionType))
        {
            type = FunctionType.get(Type.VoidTy, new ArrayList<>(), false);
            isIncompleteFunction = true;
        }

        Function f = new Function((FunctionType)type, ExternalLinkage, "", getModule());

        f.setName(name);
        entry = f;
        return entry;
    }

    public Constant getAddrOfGlobalVar(VarDecl vd)
    {
        return getAddrOfGlobalVar(vd, null);
    }

    public Constant getAddrOfGlobalVar(VarDecl vd, backend.type.Type ty)
    {
        // TODO
        return null;
    }
    /**
     * Emits code for global function definition.
     * @param fd
     */
    private void emitGlobalFunctionDefinition(FunctionDecl fd)
    {
        FunctionType ty = (FunctionType) getCodeGenTypes().
                convertType(fd.getDeclType());

        // Gets or creats the prototype for the function.
        Constant entry = getAddrOfFunction(fd, ty);

        // strip off a bitcast if we got back.
        if (entry instanceof ConstantExpr)
        {
            ConstantExpr ce = (ConstantExpr)entry;
            assert ce.getOpCode() == Operator.BitCast;
            entry = ce.operand(0);
        }

        // create a function instance
        Function fn = (Function)entry;
        new CodeGenFunction(this).generateCode(fd, fn);
    }

    public Module getModule() { return m;}

	public Constant emitNullConstant(QualType type)
	{
		return Constant.getNullValue(getCodeGenTypes().convertType(type));
	}

	public Constant emitConstantExpr(Expr expr, QualType ty, CodeGenFunction cgf)
    {
        Expr.EvalResult result = new Expr.EvalResult();

        boolean success = expr.evaluate(result);

        if (success)
        {
            assert !result.hasSideEffects():"Constant expr should not have any side effects!";
            switch (result.getValue().getKind())
            {
                case Uninitialized:
                    assert false:"Constant expressions should be initialized.";
                    return null;
                case LValue:
                {
                    backend.type.Type destTy = getCodeGenTypes().convertType(ty);
                    Constant offset = ConstantInt.get(backend.type.Type.Int64Ty,
                            result.getValue().getLValueOffset());
                    Constant c;
                    Expr LVBase = result.getValue().getLValueBase();
                    if (LVBase != null)
                    {
                        c = new ConstExprEmitter(this, cgf).emitLValue(LVBase);

                        // apply offset if necessary.
                        if (!offset.isNullValue())
                        {
                            backend.type.Type type = PointerType.get(
                                    backend.type.Type.Int8Ty);
                            Constant casted = getBitCast(c, type);
                            casted = getElementPtr(casted, offset, 1);
                            c = getBitCast(casted, c.getType());
                        }

                        if (destTy instanceof PointerType)
                        {
                            return getBitCast(c, destTy);
                        }
                        return getPtrToInt(c, destTy);
                    }
                    else
                    {
                        c = offset;
                        // Convert to the appropriate type; this could be an lvalue for
                        // an integer.
                        if (destTy instanceof PointerType)
                            return getIntToPtr(c, destTy);

                        if (c.getType() != destTy)
                            return getTrunc(c, destTy);

                        return c;
                    }
                }

                case Int:
                {
                    Constant c = ConstantInt.get(result.getValue().getInt());
                    if (c.getType() == backend.type.Type.Int1Ty)
                    {
                        backend.type.Type boolTy =
                                getCodeGenTypes().convertTypeForMem(expr.getType());
                        c = getZExt(c, boolTy);
                    }
                    return c;
                }
                case ComplexInt:
                {
                    Constant complex[] = new Constant[2];
                    complex[0] = ConstantInt.get(result.getValue().getComplexIntReal());
                    complex[1] = ConstantInt.get(result.getValue().getComplexIntImag());
                    return ConstantStruct.get(complex);
                }
                case ComplexFloat:
                {
                    Constant complex[] = new Constant[2];
                    complex[0] = ConstantFP.get(result.getValue().getComplexIntReal());
                    complex[1] = ConstantFP.get(result.getValue().getComplexIntImag());
                    return ConstantStruct.get(complex);
                }
                case Float:
                {
                    return ConstantFP.get(result.getValue().getFloat());
                }
            }
        }

        Constant c = new ConstExprEmitter(this, cgf).visit(expr);
        if (c!=null && c.getType() == backend.type.Type.Int1Ty)
        {
            backend.type.Type boolTy = getCodeGenTypes().convertTypeForMem(expr.getType());
            getZExt(c, boolTy);
        }
        return c;

    }

	public boolean returnTypeUseSret(CodeGenTypes.CGFunctionInfo callInfo)
	{
		return false;
	}

    public Function getIntrinsic(Intrinsic.ID id, ArrayList<backend.type.Type> types)
    {
        return Intrinsic.getDeclaration(getModule(), id, types);
    }

    public Function getMemCpyFn()
    {
        if (memCpyFn != null) return memCpyFn;
        backend.type.Type intPtr = theTargetData.getIntPtrType();
        ArrayList<backend.type.Type> types = new ArrayList<>();
        types.add(intPtr);
        return (memCpyFn = getIntrinsic(Intrinsic.ID.memcpy, types));
    }

    public Function getMemMoveFn()
    {
        if (memMoveFn != null) return memMoveFn;
        backend.type.Type intPtr = theTargetData.getIntPtrType();
        ArrayList<backend.type.Type> types = new ArrayList<>();
        types.add(intPtr);
        return (memMoveFn = getIntrinsic(Intrinsic.ID.memmove, types));
    }

    public Function getMemSetFn()
    {
        if (memSetFn != null) return memSetFn;
        backend.type.Type intPtr = theTargetData.getIntPtrType();
        ArrayList<backend.type.Type> types = new ArrayList<>();
        types.add(intPtr);
        return (memSetFn = getIntrinsic(Intrinsic.ID.memset, types));
    }

    public TargetData getTargetData()
    {
        return theTargetData;
    }

	/**
     * Return the appropriate bytes for a string literal, properly padded
     * to match the literal type.
     * @param s
     * @return
     */
    public String getStringForStringLiteral(StringLiteral s)
    {
        String strData = s.getStrData();
        strData += '\0';
        s.setStrData(strData);
        return strData;
    }

	/**
	 * Returns a pointer to a character array contianing the the literal.
     * @param str
     * @return
     */
    public Constant getAddrOfConstantString(String str, String globalName)
    {
        // Set the default prefix of a getName wasn't present.
        if (globalName == null)
            globalName = ".str";

        Constant c = constantStringMap.get(str);
        if (c!=null)
            return c;
        // Create a global variable for this.
        c = generateStringLiteral(str, true, this, globalName);
        constantStringMap.put(str, c);
        return c;
    }

	/**
     * Create a storage for string literal.
     * @param str
     * @param constant
     * @param genModule
     * @param globalName
     * @return
     */
    public static Constant generateStringLiteral(String str,
            boolean constant,
            HIRModuleGenerator genModule,
            String globalName)
    {
        // Create a constant for this string literal.
        Constant c = ConstantArray.get(str, false);
        // Create a global variable for this string.
        return new GlobalVariable(genModule.getModule(),
                c.getType(), constant,
                PrivateLinkage,
                c, globalName);
    }

	/**
	 * Return a pointer to a constant array for the given string literal.
     * @param expr
     * @return
     */
    public Constant getAddrOfConstantStringFromLiteral(StringLiteral expr)
    {
        return getAddrOfConstantString(getStringForStringLiteral(expr), null);
    }

	public void errorUnsupported(Stmt s, String msg)
	{
		logger.error(s.getLoc(), msg);
	}

	public void release() {}
}
