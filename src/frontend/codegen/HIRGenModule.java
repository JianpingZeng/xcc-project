package frontend.codegen;

import backend.hir.*;
import backend.intrinsic.Intrinsic;
import backend.lir.ci.LIRConstant;
import backend.lir.ci.LIRKind;
import backend.target.TargetData;
import backend.type.PointerType;
import backend.value.*;
import backend.value.Instruction.*;
import driver.Options;
import frontend.ast.StmtVisitor;
import frontend.ast.Tree;
import frontend.ast.Tree.*;
import frontend.comp.OpCodes;
import frontend.exception.JumpError;
import frontend.sema.ASTContext;
import frontend.sema.Decl;
import frontend.sema.Decl.FunctionDecl;
import frontend.sema.Decl.VarDecl;
import frontend.symbol.Symbol.OperatorSymbol;
import frontend.symbol.SymbolKinds;
import frontend.symbol.VarSymbol;
import frontend.type.FunctionType;
import frontend.type.QualType;
import frontend.type.Type;
import frontend.type.TypeClass;
import tools.Context;
import tools.Log;
import tools.Name;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static backend.value.GlobalValue.LinkageType.ExternalLinkage;
import static backend.value.GlobalValue.LinkageType.PrivateLinkage;

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
public class HIRGenModule
{
	private final static Context.Key AstToCfgKey = new Context.Key();
	private Log log;
	private Context context;
	private List<GlobalVariable> vars;
	private List<Function> functions;
	private Name.Table names;

    private TargetData theTargetData;

    private Function memCpyFn;
    private Function memMoveFn;
    private Function memSetFn;
    private ASTContext ctx;
    private Options options;
    private Module m;
    private CodeGenTypes cgTypes;

    private HashMap<String, Constant> constantStringMap;

	public HIRGenModule(ASTContext context, Options options, Module m)
    {
        ctx = context;
        this.vars = new ArrayList<>();
        this.functions = new ArrayList<>();
        this.options = options;
        this.m = m;
        cgTypes = new CodeGenTypes(this);
        constantStringMap = new HashMap<>();
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
     * This method is called by {@linkplain ModuleBuilder#handleTopLevelDecls(ArrayList)
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
            if (vd.isThisDeclarationADefinition() != Decl.DefinitionKind.Definition)
                return;

            emitGlobalVarDefinition(vd);
        }
    }

    private void emitGlobalVarDefinition(VarDecl vd)
    {
        // TODO
    }

    public Constant getAddrOfFunction(FunctionDecl fd)
    {
        return getAddrOfFunction(fd, null);
    }

    public Constant getAddrOfFunction(FunctionDecl fd, FunctionType ty)
    {
        // TODO
        return null;
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
        frontend.type.FunctionType fnType = fd.getDeclType().getFunctionType();
        backend.type.FunctionType ty = cgTypes.getFunctionType(fnType);

        // create a function instance
        Function fn = new Function(ty, ExternalLinkage, fd.getDeclName(), m);
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
                            Constant casted = ConstantExpr.getBitCast(c, type);
                            casted = ConstantExpr.getElementPtr(casted, offset, 1);
                            c = ConstantExpr.getBitCast(casted, c.getType());
                        }

                        if (destTy instanceof PointerType)
                        {
                            return ConstantExpr.getBitCast(c, destTy);
                        }
                        return ConstantExpr.getPtrToInt(c, destTy);
                    }
                    else
                    {
                        c = offset;
                        // Convert to the appropriate type; this could be an lvalue for
                        // an integer.
                        if (destTy instanceof PointerType)
                            return ConstantExpr.getIntToPtr(c, destTy);

                        if (c.getType() != destTy)
                            return ConstantExpr.getTrunc(c, destTy);

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
                        c = ConstantExpr.getZExt(c, boolTy);
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
            ConstantExpr.getZExt(c, boolTy);
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
        // Set the default prefix of a name wasn't present.
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
            HIRGenModule genModule,
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
}
