package jlang.codegen;

import backend.support.CallingConv;
import backend.support.LLVMContext;
import backend.value.Module;
import backend.value.Operator;
import backend.intrinsic.Intrinsic;
import backend.target.TargetData;
import backend.type.FunctionType;
import backend.type.PointerType;
import backend.type.Type;
import backend.value.*;
import backend.value.Instruction.CallInst;
import jlang.ast.Tree.Expr;
import jlang.ast.Tree.Stmt;
import jlang.ast.Tree.StringLiteral;
import jlang.support.CompileOptions;
import jlang.support.LangOptions;
import jlang.support.SourceLocation;
import jlang.diag.Diagnostic;
import jlang.sema.ASTContext;
import jlang.sema.Decl;
import jlang.sema.Decl.FunctionDecl;
import jlang.sema.Decl.VarDecl;
import jlang.type.QualType;

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
 * In this jlang.driver, all of source language constitute are converted into
 * SSA-based IR. So that entire optimizations rather no concerning about SSA are
 * associated. On the other hand, this jlang.driver is SSA-centeric.
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
 * construct up for loss of time and memory.
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
	private List<GlobalVariable> vars;
	private List<Function> functions;
    private Function memCpyFn;
    private Function memMoveFn;
    private Function memSetFn;
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

    private ASTContext ctx;
    private CompileOptions compileOptions;
    private TargetData td;
    private Diagnostic diags;

    private HashMap<String, Decl> deferredDecls = new HashMap<>();
    private ArrayList<Decl> deferredDeclToEmit = new ArrayList<>();
    private LangOptions langOptions;

    public HIRModuleGenerator(ASTContext context,
                              CompileOptions compOpts,
                              Module m,
                              TargetData td,
                              Diagnostic diags)
    {
        ctx = context;
        compileOptions = compOpts;
        this.m = m;
        this.td = td;
        this.diags = diags;
        vars = new ArrayList<>();
        functions = new ArrayList<>();
        cgTypes = new CodeGenTypes(this, td);
        constantStringMap = new HashMap<>();
        globalDeclMaps = new HashMap<>();
        langOptions = context.getLangOptions();
    }

    public CodeGenTypes getCodeGenTypes()
    {
        return cgTypes;
    }

    ASTContext getASTContext()
    {
        return ctx;
    }
    /**
     * Emits HIR code for a single top level declaration.
     * @param decl
     */
    public void emitTopLevelDecl(Decl decl)
    {
        // If an error has occurred, stop code generation, but continue
        // parsing and semantic analysis (to ensure all warnings and errors
        // are emitted).
        if (diags.hasErrorOccurred())
            return;

        switch(decl.getKind())
        {
            case FunctionDecl:
            case VarDecl:
                // handle function declaration.
                emitGlobal(decl);
                break;
            case TranslationUnitDecl:
            case FieldDecl:
            case ParamVarDecl:
            case EnumConstant:
            case LabelDecl:
                // skip it.
                break;
            default:
                assert decl instanceof Decl.TypeDecl:"Unsupported decl kind!";
                break;
        }
    }

    public void updateCompletedType(Decl.TagDecl tag)
    {

    }

    public void emitTentativeDefinition(VarDecl var)
    {

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
            if (!fd.isThisDeclarationADefinition())
                return;

            // Emit the LLVM code for this function declaration.
            emitGlobalFunctionDefinition(fd);
        }
        else
        {
            assert decl instanceof VarDecl;
            final VarDecl vd = (VarDecl)decl;
            assert vd.isFileVarDecl():"Cann't emit code for local variable!";


            // Defer code generation when possible if this is a static definition, inline
            // function etc.  These we only want to emit if they are used.
            if (mayDeferGeneration(vd))
            {
                String mangledName = getMangledName(vd);
                if (globalDeclMaps.containsKey(mangledName))
                {
                    deferredDeclToEmit.add(vd);
                }
                else
                {
                    // Otherwise, remember that we saw a deferred decl with this name.  The
                    // first use of the mangled name will cause it to move into
                    // DeferredDeclsToEmit.
                    deferredDecls.put(mangledName, vd);
                }
                return;
            }

            // Otherwise emit the definition.
            emitGlobalVarDefinition(vd);
        }
    }

    private boolean mayDeferGeneration(FunctionDecl fd)
    {
        return false;
    }

    private boolean mayDeferGeneration(VarDecl vd)
    {
       assert vd.isFileVarDecl() :"Invalid decl";
       return vd.getStorageClass() == Decl.StorageClass.SC_static;
    }

    private String getMangledName(Decl.NamedDecl decl)
    {
        assert decl.getIdentifier() != null:"Attempt to mangle unnamed decl";
        return decl.getNameAsString();
    }

    private void emitGlobalVarDefinition(VarDecl vd)
    {
        Constant init = null;
        QualType astTy = vd.getType();

        if (vd.getInit() == null)
        {
            // This a tentative definition. tentative definitions are
            // implicitly initialized with 0.
            // They should never have incomplete type.
            assert !astTy.getType().isIncompleteType():"Unexpected incomplete type!";
	        init = emitNullConstant(astTy);
        }
	    else
        {
	        init = emitConstantExpr(vd.getInit(), astTy, null);
	        if (init == null)
	        {
		        QualType t = vd.getInit().getType();
		        errorUnsupported(vd, "static initializer", false);
		        init = UndefValue.get(getCodeGenTypes().convertType(t));
	        }
        }

	    Type initType = init.getType();
	    Constant entry = getAddrOfGlobalVar(vd, initType);

	    // Strip off a bitcast if we got one back.
	    if (entry instanceof ConstantExpr)
	    {
		    ConstantExpr ce = (ConstantExpr)entry;
		    assert ce.getOpcode() == Operator.BitCast
				    || ce.getOpcode() == Operator.GetElementPtr;
		    entry = ce.operand(0);
	    }

	    // Entry is now a global variable.
	    GlobalVariable gv = (GlobalVariable)entry;

        // We have a definition after a declaration with the wrong type.
        // We must make a new GlobalVariable* and update everything that used OldGV
        // (a declaration or tentative definition) with the new GlobalVariable*
        // (which will be a definition).
        //
        // This happens if there is a prototype for a global (e.g.
        // "extern int x[];") and then a definition of a different type (e.g.
        // "int x[10];"). This also happens when an initializer has a different type
        // from the type of the global (this happens with unions).
        if (gv == null || !gv.getType().getElementType().equals(initType)
                || gv.getType().getAddressSpace() != astTy.getAddressSpace())
        {
            // Remove the old entry from GlobalDeclMap so that we'll create a new one.
            globalDeclMaps.remove(getMangledName(vd));

            gv = (GlobalVariable) getAddrOfGlobalVar(vd, initType);
            gv.setName(entry.getName());

            // Replace all uses of the old global with the new global
            Constant newPtrForOldDecl = ConstantExpr.getBitCast(gv, entry.getType());
            entry.replaceAllUsesWith(newPtrForOldDecl);

            // Erase the old global, since it is no longer used.
            ((GlobalValue)entry).eraseFromParent();;
        }

	    gv.setInitializer(init);

	    // if it is safe to mark the global constant, do that.
	    gv.setConstant(false);
	    if (vd.getType().isConstant(ctx))
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
        setCommonAttributes(vd, gv);
    }

    private LangOptions.VisibilityMode getDeclVisibilityMode(Decl d)
    {
        if (d instanceof VarDecl)
        {
            VarDecl vd = (VarDecl)d;
            if (vd.getStorageClass() == Decl.StorageClass.SC_PrivateExtern)
                return LangOptions.VisibilityMode.Hidden;
        }

        /*
        TODO complete VisibilityModeAttr for GlobalValue.
        if (const VisibilityAttr *attr = D->getAttr<VisibilityAttr>()) {
        switch (attr->getVisibility()) {
            default: assert(0 && "Unknown visibility!");
            case VisibilityAttr::DefaultVisibility:
                return LangOptions::Default;
            case VisibilityAttr::HiddenVisibility:
                return LangOptions::Hidden;
            case VisibilityAttr::ProtectedVisibility:
                return LangOptions::Protected;
            }
        }*/
        return langOptions.getSymbolVisibility();
    }

    private void setGlobalVisibility(Decl d, GlobalValue gv)
    {
        // Internal definitions always have default visibility.
        if (gv.hasLocalLinkage())
        {
            gv.setVisibility(GlobalValue.VisibilityTypes.DefaultVisibility);
            return;
        }

        switch (getDeclVisibilityMode(d))
        {
            default: assert false:"Unknown visibility!";

            case Default:
                gv.setVisibility(GlobalValue.VisibilityTypes.DefaultVisibility);
                return;
            case Hidden:
                gv.setVisibility(GlobalValue.VisibilityTypes.HiddenVisibility);
                return;
            case Protected:
                gv.setVisibility(GlobalValue.VisibilityTypes.ProtectedVisibility);
                return;
        }
    }

    /**
     * Set some common attributes for global variable or function.
     * @param d
     * @param gv
     */
    private void setCommonAttributes(Decl d, GlobalValue gv)
    {
        setGlobalVisibility(d, gv);
        // TODO: 2017/10/12  Set the UsedAtr, SectionAttr.
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
            ty = getCodeGenTypes().convertType(fd.getType());
        return getOrCreateFunction(fd.getNameAsString(), ty, fd);
    }

	/**
     * Create and return an backend FunctionProto with the specified type. If there
     * is something in the module with the specified getIdentifier, return it potentially
     * bitcasted to the right type.
     *
     * If D is non-null, it specifies a decl that correspond to this.  This is used
     * to set the attributes on the function when it is first created.
     * @param mangledName
     * @param type
     * @param fd
     * @return
     */
    private Constant getOrCreateFunction(String mangledName, Type type, FunctionDecl fd)
    {
        GlobalValue entry = globalDeclMaps.get(mangledName);
        if (entry != null)
        {
            if (entry.getType().getElementType().equals(type))
                return entry;

            // Make sure the result is of the correct type.
            Type ptrType = PointerType.getUnqual(type);
            return getBitCast(entry, ptrType);
        }

        // This is the first use or definition of a mangled name.  If there is a
        // deferred decl with this name, remember that we need to emit it at the end
        // of the file.
        if (deferredDecls.containsKey(mangledName))
        {
            // Move the potentially referenced deferred decl to deferredDeclsToEmit list.
            deferredDeclToEmit.add(fd);
            deferredDecls.remove(mangledName);
        }
        else
        {
            // If this is the first reference to a inline function.
            // queue up the deferred function body fo emission.
            if (fd.isThisDeclarationADefinition() && mayDeferGeneration(fd))
                deferredDeclToEmit.add(fd);
        }

        // This function doesn't have a complete type (for example, return
        // type is an incomplete struct). Use a fake type instead.
        boolean isIncompleteFunction = false;
        if (!(type instanceof FunctionType))
        {
            type = FunctionType.get(LLVMContext.VoidTy, new ArrayList<>(), false);
            isIncompleteFunction = true;
        }

        Function f = new Function((FunctionType)type, ExternalLinkage, "", getModule());
        if (fd != null)
        {
            setFunctionAttributes(fd, f, isIncompleteFunction);
        }

        f.setName(mangledName);
        globalDeclMaps.put(mangledName, f);
        return f;
    }

    private void setFunctionAttributes(FunctionDecl fd,
            Function f, boolean isIncompleteFunction)
    {
        if (!isIncompleteFunction)
            setLLVMFunctionAttributes(fd, getCodeGenTypes().getFunctionInfo(fd), f);
        // TODO set linkage according to Attribute, current just set it as ExternalLinkage.
        f.setLinkage(ExternalLinkage);
    }

    private void setLLVMFunctionAttributes(
            FunctionDecl fd,
            CodeGenTypes.CGFunctionInfo fi,
            Function f)
    {
        //FIXME Set the calling convention default to standard C. 2017/10/12
        f.setCallingConv(CallingConv.C);
    }

    public Constant getAddrOfGlobalVar(VarDecl vd)
    {
        return getAddrOfGlobalVar(vd, null);
    }

    /**
     * Return the backend.Constant for the address of the given global address. If ty is non
     * null and if the global var does not exist, then it will be greated with the specified
     * type instead of whatever the normal requested type would be.
     * @param vd
     * @param ty
     * @return
     */
    public Constant getAddrOfGlobalVar(VarDecl vd, backend.type.Type ty)
    {
        assert vd.hasGlobalStorage() :"Not a global variable";
        QualType astTy = vd.getType();
        if (ty == null)
            ty = getCodeGenTypes().convertTypeForMem(astTy);

        backend.type.PointerType pty = backend.type.PointerType.get(ty, astTy.getAddressSpace());
        return getOrCreateLLVMGlobal(getMangledName(vd), pty, vd);
    }

    /**
     * If the specified mangled mangledName is not in the module, create and return an backend GlobalVariable
     * with the specified type. If there is something in the module with the specified mangledName,
     * return it potentially bitcasted to the right type.
     *
     * @param mangledName
     * @param pty
     * @param vd
     * @return
     */
    private Constant getOrCreateLLVMGlobal(String mangledName, backend.type.PointerType pty, VarDecl vd)
    {
        if (globalDeclMaps.containsKey(mangledName))
        {
            GlobalValue entry = globalDeclMaps.get(mangledName);
            if (entry.getType().equals(pty))
                return entry;

            // Make sure the result is of the correct type.
            return ConstantExpr.getBitCast(entry, pty);
        }

        // This is the first use or definition of a mangled name.
        // If there is a defered decl with this name, remember that we need to emit
        // it at the end of file.
        if (deferredDecls.containsKey(mangledName))
        {
            deferredDeclToEmit.add(vd);
            deferredDecls.remove(mangledName);
        }

        GlobalVariable gv = new GlobalVariable(getModule(), pty.getElementType(),
                false, ExternalLinkage, null, mangledName, null,
                pty.getAddressSpace());

        gv.setName(mangledName);

        if (vd != null)
        {
            gv.setConstant(vd.getType().isConstant(ctx));
        }
        globalDeclMaps.put(mangledName, gv);
        return gv;
    }

    private static void replaceUsesOfNonProtoTypeWithRealFuncion(GlobalValue old,
                                                                 Function newFn)
    {
        if (!(old instanceof Function))
            return;

        Function oldFn = (Function)old;
        Type newRetTy = newFn.getReturnType();
        ArrayList<Value> argList = new ArrayList<>();
        for (int i = 0, e = oldFn.getNumUses(); i < e; i++)
        {
            User u = oldFn.useAt(i).getUser();
            if (!(u instanceof CallInst) || i != 0)
                continue;

            // If the return types don't match exactly, and if the call isn't dead, then
            // we can't transform this call.
            CallInst ci = (CallInst)u;
            if (!ci.getType().equals(newRetTy) && !ci.isUseEmpty())
                continue;


            // If the function was passed too few arguments, don't transform.  If extra
            // arguments were passed, we silently drop them.  If any of the types
            // mismatch, we don't transform.
            int argNo = 0;
            boolean dontTransform = false;
            for (int j = 0, sz = newFn.getNumOfArgs(); j < sz; j++, argNo++)
            {
                if (ci.getNumsOfArgs() == argNo ||
                        !ci.argumentAt(argNo).getType().equals(newFn.argAt(j).getType()))
                {
                    dontTransform = true;
                    break;
                }
            }

            if (dontTransform)
                continue;


            // Okay, we can transform this.  Create the new call instruction and copy
            // over the required information.
            for (int j = 0; j < argNo; j++)
                argList.add(ci.argumentAt(j));

            Value[] args = new Value[argNo];
            argList.toArray(args);
            CallInst newCI = new CallInst(args, newFn,"", ci);
            if (!newCI.getType().equals(LLVMContext.VoidTy))
                newCI.setName(ci.getName());

            newCI.setCallingConv(ci.getCallingConv());
            if(!ci.isUseEmpty())
                ci.replaceAllUsesWith(newCI);
            ci.eraseFromParent();
        }
    }

    /**
     * Emits code for global function definition.
     * @param fd
     */
    private void emitGlobalFunctionDefinition(FunctionDecl fd)
    {
        FunctionType ty = (FunctionType) getCodeGenTypes().
                convertType(fd.getType());

        if (fd.getType().isFunctionNoProtoType())
        {
            assert ty.isVarArg():"Didn't lower type as expected";
            ArrayList<Type> args = new ArrayList<>();
            for (int i = 0, e = ty.getNumParams(); i < e; i++)
                args.add(ty.getParamType(i));
            ty = FunctionType.get(ty.getReturnType(), args, false);
        }

        // Gets or creates the prototype for the function.
        Constant entry = getAddrOfFunction(fd, ty);

        // strip off a bitcast if we got back.
        if (entry instanceof ConstantExpr)
        {
            ConstantExpr ce = (ConstantExpr)entry;
            assert ce.getOpcode() == Operator.BitCast;
            entry = ce.operand(0);
        }

        if (!((GlobalValue)entry).getType().getElementType().equals(ty))
        {
            GlobalValue oldFn = (GlobalValue)entry;

            assert oldFn.isDeclaration() :"Should not replace non-declaration";

            globalDeclMaps.remove(getMangledName(fd));
            Function newFn = (Function)getAddrOfFunction(fd, ty);
            newFn.setName(oldFn.getName());

            if (fd.getType().isFunctionNoProtoType())
            {
                replaceUsesOfNonProtoTypeWithRealFuncion(oldFn, newFn);
                oldFn.removeDeadConstantUsers();
            }

            if (!entry.isUseEmpty())
            {
                Constant newPtrForOldDecl = ConstantExpr.getBitCast(newFn, entry.getType());
                entry.replaceAllUsesWith(newPtrForOldDecl);
            }

            oldFn.eraseFromParent();
            entry = newFn;
        }

        // create a function instance
        Function fn = (Function)entry;
        new CodeGenFunction(this).generateCode(fd, fn);

        globalDeclMaps.put(getMangledName(fd), fn);

        setFunctionDefinitionAttributes(fd, fn);
    }

    enum GVALinkage
    {
        GVA_Internal,
        GVA_C99Inline,
        GVA_CXXInline,
        GVA_StrongExternal
    }

    private static GVALinkage getLinkageForFunction(
            ASTContext context,
            FunctionDecl fd,
            LangOptions features)
    {
        GVALinkage external = GVALinkage.GVA_StrongExternal;
        // "static" function get internal linkage.
        if (fd.getStorageClass() == Decl.StorageClass.SC_static)
            return GVALinkage.GVA_Internal;

        if (!fd.isInlineSpecified())
            return external;

        // If the inline function explicitly has the GNU inline attribute on it, or if
        // this is C89 mode, we use to GNU semantics.
        if (!features.c99)
        {
            if (fd.getStorageClass() == Decl.StorageClass.SC_extern)
            {
                return GVALinkage.GVA_C99Inline;
            }
            // Normal inline is a strong symbol.
            return GVALinkage.GVA_StrongExternal;
        }
        else if (features.gnuMode)
        {
            // GCC in C99 mode seems to use a different decision-making process
            // for external inline.
            return GVALinkage.GVA_C99Inline;
        }

        assert features.c99:"Must be in c99 mode if not in C89";
        if (fd.isC99InlineDefinition())
            return GVALinkage.GVA_C99Inline;

        return GVALinkage.GVA_StrongExternal;
    }

    private void setFunctionDefinitionAttributes(FunctionDecl fd, GlobalValue gv)
    {
        GVALinkage linkage = getLinkageForFunction(ctx, fd, langOptions);
        if (linkage == GVALinkage.GVA_Internal)
        {
            gv.setLinkage(InteralLinkage);
        }
        else if (linkage == GVALinkage.GVA_C99Inline)
        {
            // In C99 mode, 'inline' functions are guaranteed to have a strong
            // definition somewhere else, so we can use available_externally linkage.
            gv.setLinkage(ExternalLinkage);
        }
        else
        {
            assert linkage == GVALinkage.GVA_StrongExternal;
            gv.setLinkage(ExternalLinkage);
        }

        setCommonAttributes(fd, gv);
    }

    public Module getModule() { return m;}

    /**
     * Generates a null constant according specified type. For example, emit 0 for
     * integral type, 0.0 for float pointing type.
     * @param type
     * @return
     */
	public Constant emitNullConstant(QualType type)
	{
	    return Constant.getNullValue(getCodeGenTypes().convertTypeForMem(type));
	}

	public Constant emitConstantExpr(Expr expr, QualType ty, CodeGenFunction cgf)
    {
        Expr.EvalResult result = new Expr.EvalResult();

        boolean success = expr.evaluate(result, ctx);

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
                    Constant offset = ConstantInt.get(LLVMContext.Int64Ty,
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
                                    LLVMContext.Int8Ty, ty.getAddressSpace());
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
                    if (c.getType() == LLVMContext.Int1Ty)
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
        if (c!=null && c.getType() == LLVMContext.Int1Ty)
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
        backend.type.Type intPtr = td.getIntPtrType();
        ArrayList<backend.type.Type> types = new ArrayList<>();
        types.add(intPtr);
        return (memCpyFn = getIntrinsic(Intrinsic.ID.memcpy, types));
    }

    public Function getMemMoveFn()
    {
        if (memMoveFn != null) return memMoveFn;
        backend.type.Type intPtr = td.getIntPtrType();
        ArrayList<backend.type.Type> types = new ArrayList<>();
        types.add(intPtr);
        return (memMoveFn = getIntrinsic(Intrinsic.ID.memmove, types));
    }

    public Function getMemSetFn()
    {
        if (memSetFn != null) return memSetFn;
        backend.type.Type intPtr = td.getIntPtrType();
        ArrayList<backend.type.Type> types = new ArrayList<>();
        types.add(intPtr);
        return (memSetFn = getIntrinsic(Intrinsic.ID.memset, types));
    }

    public TargetData getTargetData()
    {
        return td;
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
        // Set the default prefix of a getIdentifier wasn't present.
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
                c, globalName, null, 0);
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

    void errorUnsupported(Stmt stmt, String type)
    {
        errorUnsupported(stmt.getLocStart(), type, false);
    }

    private void errorUnsupported(SourceLocation loc, String type, boolean omitOnError)
    {
        if (omitOnError && diags.hasErrorOccurred())
            return;
        int diagID = diags.getCustomDiagID(Diagnostic.Level.Error,
                "cannot compile this %0 yet");
        diags.report(ctx.getFullLoc(loc), diagID).addTaggedVal(type).emit();
    }

    public void errorUnsupported(Decl d, String type)
    {
        errorUnsupported(d, type, false);
    }

	private void errorUnsupported(Decl d, String type, boolean omitOnError)
	{
	    errorUnsupported(d.getLocation(), type, omitOnError);
	}

	public void release() {}
}
