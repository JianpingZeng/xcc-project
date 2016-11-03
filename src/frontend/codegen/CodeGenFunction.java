package frontend.codegen;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2016, Xlous
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

import backend.hir.BasicBlock;
import backend.hir.HIRBuilder;
import backend.hir.JumpDest;
import backend.type.FunctionType;
import backend.type.PointerType;
import backend.type.Type;
import backend.value.*;
import frontend.ast.Tree;
import frontend.sema.Decl;
import frontend.sema.Decl.FunctionDecl;
import frontend.sema.Decl.VarDecl;
import frontend.type.ArrayType;
import frontend.type.QualType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This class responsible for generating HIR code.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class CodeGenFunction
{
    private HIRGenModule generator;
    private FunctionDecl curFnDecl;
    private Function curFn;
    private QualType fnRetTy;
    private int id;

    /**
     * Unified return block.
     */
    private JumpDest returnBlock;

    /**
     * The temporary alloca to hold the return value.
     * This is null iff the function has no return value.
     */
    private Value returnValue;

    /**
     * This is an instruction before which we prefer to insert allocas.
     */
    private Instruction allocaInstPtr;

    private int nextCleanupDestIndex;

    HIRBuilder builder;

    /**
     * Indicates if code generation of this function has finished.
     */
    private boolean isFinished;

    /**
     * A hashmap for keeping track of local variable declaration in C.
     */
    private HashMap<Decl, Value> localVarMaps;

    /**
     * Keeps track of the Basic block for each C label.
     */
    private HashMap<Tree.LabelledStmt, BasicBlock> labelMap;

    private HashMap<Tree.Expr, Type> vlaSizeMap;

    public CodeGenFunction(HIRGenModule generator)
    {
        this.generator = generator;
        builder = new HIRBuilder();
        localVarMaps = new HashMap<>();
        labelMap = new HashMap<>();
        vlaSizeMap = new HashMap<>();
    }

    public void generateCode(FunctionDecl fd, Function fn)
    {
        QualType resTy = fd.getReturnType();

        ArrayList<VarDecl> functionArgList = new ArrayList<>(16);
        if (fd.getNumParams()>0)
        {
            for (int i= 0, e = fd.getNumParams(); i < e; i++)
                functionArgList.add(fd.getParamDecl(i));
        }

        // Emit the standard function prologue.
        startFunction(fd, resTy, fn, functionArgList);
        // Generates code for function body.
        emitFunctionBody();

        // emit standard function eliplogue.
        finishFunction();
    }

    private void startFunction(FunctionDecl fd, QualType resTy,
            Function fn,
            ArrayList<VarDecl> args)
    {
        curFn = fn;
        curFnDecl = fd;
        fnRetTy = resTy;

        assert fn.isDeclaration():"Function already has body.";

        BasicBlock entryBB = createBasicBlock("entry", curFn);

        // Create a marker to make it easy to insert allocas into the entryblock
        // later.  Don't create this with the builder, because we don't want it
        // folded.
        Value undef = Value.UndefValue.get(Type.Int32Ty);

        returnBlock = getJumpDestInCurrentScope("return");
        builder.setInsertPoint(entryBB);

        if (resTy.isVoidType())
            returnValue = null;
        else
        {
            returnValue = createIRTemp(resTy, "retval");
        }

        emitFunctionPrologue(curFn, curFn.getType(), args);
        // If any of the arguments have a variably modified type,
        // make sure to emit type size.
        for (VarDecl vd : args)
        {
            QualType ty = vd.getDeclType();
            // TODO handle variable size type introduced in C99.
        }
    }

    /**
     * Emits standard prologue code for function definition.
     * @param fn
     * @param args
     */
    private void emitFunctionPrologue(Function fn,
            FunctionType fnType,
            ArrayList<VarDecl> args)
    {
        if (curFnDecl.hasImplicitReturnZero())
        {
            QualType retType = curFnDecl.getReturnType().getUnQualifiedType();
            Type backendTy = generator.getCodeGenTypes().convertType(retType);
            Constant zero = Constant.getNullValue(backendTy);
            builder.createStore(zero, returnValue);
        }

        assert fn.getNumOfArgs() == args.size()
                :"Mismatch between function signature and argumens";

        // walking through variable declaration.
        int argNo = 1;
        // Emit allocs for param decls.  Give the HIR argument nodes names.
        Iterator<Argument> argItr = fn.getArgumentList().iterator();

        // obtains the type list of formal type enclosing in FunctionType.
        Iterator<CodeGenTypes.ArgTypeInfo> infoItr = fnType.getParamTypes().iterator();
        for (VarDecl vd : args)
        {
            QualType ty = vd.getDeclType();
            Value v = argItr.next();
            final CodeGenTypes.ArgTypeInfo ArgInfo = infoItr.next();

            assert !argItr.hasNext():"Argument mismatch!";

            // struct/union, array type.
            if (hasAggregateBackendType(ty))
            {
                // Create a temporary alloca to hold the argument; the rest of
                // codegen expects to access aggregates & complex values by
                // reference.
                Value ptr = createTempAlloc(convertTypeForMem(ty));
                builder.createStore(v, ptr);
                v = ptr;
            }
            else
            {
                // if argument type is compatible with parameter type.
                // issue conversion instruction.
                // v = emitScalarConversion(v, argument type, formal type);
                if (!ty.isCompatible(ArgInfo.frontendType))
                {
                    // This must be a promotion, for something like
                    // "void a(x) short x; {..."
                    v = emitScalarConversion(v, ty, ArgInfo.frontendType);
                }
            }
            emitParamDecl(vd, v);
        }
        assert !argItr.hasNext():"Argument mismatch!";
    }

    /**
     * Emits conversion instruction from the specified frontend type to another frotnend tyep.
     *
     * Both type must be no Aggregate type.
     * @param v
     * @param srcTy
     * @param destTy
     * @return
     */
    private Value emitScalarConversion(Value v, QualType srcTy, QualType destTy)
    {
        assert !hasAggregateBackendType(srcTy) && !hasAggregateBackendType(destTy)
                :"Invalid scalar expression to emit!";
        return new ScalarExprEmitter(this).emitScalarConversion(v, srcTy, destTy);
    }

    private void emitParamDecl(VarDecl param, Value v)
    {

    }

    private backend.type.Type convertTypeForMem(QualType ty)
    {
        return generator.getCodeGenTypes().convertTypeForMem(ty);
    }

    private boolean hasAggregateBackendType(QualType ty)
    {
        return !ty.isPointerType() && !ty.isRealType()
                && !ty.isVoidType() && !ty.isFunctionType();
    }

    private Value createIRTemp(QualType ty, String name)
    {
        Instruction.AllocaInst alloc = createTempAlloc(convertType(ty), name);
        return alloc;
    }

    /**
     * This creates a alloca and inserts it into the entry block.
     * @param ty
     * @param name
     * @return
     */
    private Instruction.AllocaInst createTempAlloc(Type ty, String name)
    {
        return new Instruction.AllocaInst(ty, null, name, allocaInstPtr);
    }

    private Instruction.AllocaInst createTempAlloc(Type ty)
    {
        return new Instruction.AllocaInst(ty, null, "temp", allocaInstPtr);
    }

    private Type convertType(QualType t)
    {
        return generator.getCodeGenTypes().convertType(t);
    }

    private JumpDest getJumpDestInCurrentScope(String name)
    {
        return getJumpDestInCurrentScope(createBasicBlock(name));
    }

    private JumpDest getJumpDestInCurrentScope(BasicBlock target)
    {
        return new JumpDest(target, nextCleanupDestIndex++);
    }

    /**
     * Emits code for the function body through visiting CompoundStmt of function.
     */
    private void emitFunctionBody()
    {
        assert curFnDecl.hasBody()
                :"Can not emit stmt code for function with no body.";
        emitStmt(curFnDecl.getBody());
    }

    private void emitStmt(Tree.Stmt stmt)
    {
        assert stmt!=null:"Null Statement!";

        // Check if we can handle this without bother to generate an
        // insert point.
        if (emitSimpleStmt(stmt))
            return;


    }

    private boolean emitSimpleStmt(Tree.Stmt s)
    {
        switch (s.getStmtClass())
        {
            default: return false;
            case Tree.Stmt.NullStmtClass:
            case Tree.Stmt.CompoundStmtClass:
                emitCompoundStmt((Tree.CompoundStmt)s);
                return true;
            case Tree.DeclStmtClass:
                emitDeclStmt((Tree.DeclStmt)s);
                return true;
            case Tree.GotoStmtClass:
                emitGotoStmt((Tree.GotoStmt)s);
                return true;
            case Tree.BreakStmtClass:
                emitBreakStmt((Tree.BreakStmt)s);
                return true;
            case Tree.ContinueStmtClass:
                emitContinueStmt((Tree.ContinueStmt)s);
                return true;
            case Tree.DefaultStmtClass:
                emitDefaultStmt((Tree.DefaultStmt)s);
                return true;
            case Tree.CaseStmtClass:
                emitCaseStmt((Tree.CaseStmt)s);
                return true;
        }
    }

    /**
     * Emits code for the compound statement, like {...}.
     * @param s
     */
    private void emitCompoundStmt(Tree.CompoundStmt s)
    {
        for (Iterator<Tree.Stmt> itr = s.iterator(); itr.hasNext();)
        {
            emitStmt(itr.next());
        }
    }

    private void emitDeclStmt(Tree.DeclStmt s)
    {
        for (Iterator<Decl> itr = s.iterator(); itr.hasNext();)
        {
            emitDecl(itr.next());
        }
    }

    private void emitDecl(Decl decl)
    {
        switch (decl.getDeclKind())
        {
            default:assert false:"Unknown decl type.";
                break;
            case ParamVar:
                assert false:"ParamDecls should not be handled in emitDecl().";
                break;
            case FunctionDecl: // void foo();
            case StructDecl:   // struct/union X;
            case EnumDecl:     // enum X;
            case EnumConstant: // enum ? {X =?,};
                // none of those decls required codegen support.
                return;
            case VarDecl:
            {
                VarDecl vd = (VarDecl)decl;
                assert vd.isBlockVarDecl()
                        :"Should not see file-scope variable declaration.";
                emitBlockVarDecl(vd);
                return;
            }
            case TypedefDecl: // typedef int x;
            {
                Decl.TypeDefDecl tf = (Decl.TypeDefDecl)decl;
                QualType ty = tf.getUnderlyingType();

                // TODO handle variable modified type, 2016.11.3.
                emitVLASize(ty);
                return;
            }
        }
    }

    /**
     * This method handles any variable function inside a function.
     * @param vd
     * @return
     */
    private void emitBlockVarDecl(VarDecl vd)
    {
        switch (vd.getStorageClass())
        {
            case SC_none:
            case SC_auto:
            case SC_register:
                emitLocalBlockVarDecl(vd);
                return;
            case SC_static:
                emitStaticBlockVarDecl(vd);
                return;
            case SC_extern:
                return;
        }
        assert false:"Unknown storage class.";
    }

    /**
     * <p>Emits code and set up an entry in LocalDeclMap for a variable declaration
     * with auto, register, or on storage class specifier.
     * </p>
     * <p>These turn into simple stack objects, or {@linkplain GlobalValue}
     * depending on target.
     * </p>
     * @param vd
     */
    private void emitLocalBlockVarDecl(VarDecl vd)
    {
        QualType ty = vd.getDeclType();
        backend.value.Value declPtr;
        if (ty.isConstantSizeType())
        {
            // A normal fixed sized variable becomes an alloca in the entry block.
            backend.type.Type lty = convertTypeForMem(ty);
            Instruction.AllocaInst alloca = createTempAlloc(lty);
            alloca.setName(vd.getDeclName());

            declPtr = alloca;
        }
        else
        {
            ensureInsertPoint();

            Type elemTy = convertTypeForMem(ty);
            Type elemPtrTy = PointerType.get(elemTy);

            Value vlaSize = emitVLASize(ty);

            // downcast the VLA size expression.
            vlaSize = builder.createIntCast(vlaSize, Type.Int32Ty,false, "");
            // allocate an array with variable size.
            Value vla = builder.createAlloca(Type.Int8Ty, vlaSize, "vla");

            // convert the pointer to array into regular pointer.
            declPtr = builder.creatBitCast(vla, elemPtrTy, "temp");
        }

        Value entry = localVarMaps.get(vd);
        assert entry == null:"Decl already exits in LocalVarMaps";

        entry = declPtr;

        // if this local var has initializer, emit it.
        Tree.Expr init = vd.getInit();

        // If we are at an unreachable point, we don't need to emit the initializer
        // unless it contains a label.
        if (!hasInsertPoint())
        {
            if (!containsLabel(init, false))
                init = null;
            else
                ensureInsertPoint();
        }

        if (init != null)
        {
            Value loc = declPtr;
            if (!hasAggregateBackendType(init.getType()))
            {
                Value v = emitScalarExpr(init);
                emitStoreOfScalar(v, loc, vd.getDeclType());
            }
            else if (init.getType().isComplexType())
            {
                // todo handle var declaration of typed complex type.
            }
            else
            {
                emitAggExpr(init, loc);
            }
        }
    }

    private Value emitVLASize(QualType type)
    {
        // todo handle variable sized type in the future. 2016.11.5.
        assert type.isVariablyModifiedType():
                "Must pass variably modified type to EmitVLASizes!";
        ensureInsertPoint();

        ArrayType.VariableArrayType vat = type.getAsVariableArrayType();
        if (vat != null)
        {
            Value sizeEntry = vlaSizeMap.get(vat.getSizeExpr());
            if (sizeEntry == null)
            {
                Type sizeTy = convertType(vat.getSizeExpr().getType());

                // get the element size.
                QualType elemTy = vat.getElemType();
                Value elemSize;
                if (elemTy.isVariableArrayType())
                    elemSize = emitVLASize(elemTy);
                else
                    elemSize = ConstantInt.get(sizeTy, elemTy.getTypeSize() / 8);

                Value numElements = emitScalarExpr(vat.getSizeExpr());
                numElements = builder.createIntCast(numElements, sizeTy, false, "tmp");
                sizeEntry = builder.createMul(elemSize, numElements, "");
            }
            return sizeEntry;
        }
        ArrayType at = type.getAsArrayType();
        if (at != null)
        {
            emitVLASize(at.getElemType());
            return null;
        }

        frontend.type.PointerType ptr = type.<frontend.type.PointerType>getAs();
        assert ptr != null: "unknown VM type!";
        emitVLASize(ptr.getPointeeType());
        return null;
    }

    private Value emitScalarExpr(Tree.Expr expr)
    {
        assert expr !=null && !hasAggregateBackendType(expr.getType())
                :"Invalid scalar expression to emit";
        return new ScalarExprEmitter(this).visit(expr);
    }

    /**
     * Emit the computation of the specified expression of aggregate
     * type.  The result is computed into {@code destPtr}.
     *
     * Note that if {@code destPtr} is null, the value of the aggregate
     * expression is not needed.
     * @param expr
     * @param destPtr
     */
    private void emitAggExpr(Tree.Expr expr, Value destPtr)
    {
        assert expr!=null && hasAggregateBackendType(expr.getType())
                :"Invalid aggregate expression to emit";
        if (destPtr == null)return;

        new AggExprEmitter(this, destPtr).visit(expr);
    }

    private void emitStoreOfScalar(Value val, Value addr/** boolean isVolatile*/, QualType ty)
    {
        if (ty.isBooleanType())
        {
            // Bool can have different representation in memory than in registers.
            Type srcTy = val.getType();
            PointerType destPtr = (PointerType)addr.getType();
            if (destPtr.getElemType() != srcTy)
            {
                Type memTy = PointerType.get(srcTy);
                addr = builder.createBitCast(addr, memTy, "storetmp");
            }
        }
        builder.createStore(val, addr);
    }

    private boolean containsLabel(Tree.Stmt s, boolean ignoreCaseStmts)
    {
        // Null statement, not a label.
        if (s == null) return false;

        // If this is a labelled statement, we want to emit code for it.
        // like this: if (0) {... foo: bar(); } goto foo;
        if (s instanceof Tree.LabelledStmt)
            return true;

        // If this is a case/default statement, and we haven't seen a switch, we have
        // to emit the code.
        if (s instanceof Tree.SwitchCase && !ignoreCaseStmts)
            return true;

        // If this is a switch statement, we want to ignore cases below it.
        if (s instanceof Tree.SwitchStmt)
            ignoreCaseStmts = true;

        if (s instanceof Tree.CompoundStmt)
        {
            Tree.CompoundStmt cs = (Tree.CompoundStmt)s;
            for (Tree.Stmt sub : cs.stats)
                if (containsLabel(sub, ignoreCaseStmts))
                    return true;
        }
        return false;
    }

    /**
     * Ensure the insert point has been defined as yet before emit IR.
     */
    private void ensureInsertPoint()
    {
        if (!hasInsertPoint())
            emitBlock(createBasicBlock());
    }

    private boolean hasInsertPoint()
    {
        return builder.getInsertBlock() != null;
    }

    private void emitBlock(BasicBlock bb)
    {
        // fall out of the current block if necessary.
        emitBranch(bb);

        if (isFinished && bb.isUseEmpty())
            return;
        curFn.getBasicBlockList().add(bb);
        builder.setInsertPoint(bb);
    }

    private void emitBranch(BasicBlock targetBB)
    {
        // Emit a branch instruction from the current block to the
        // target block if this is a real one. If this is just a fall-through
        // block after a terminator, don't emit it.
        BasicBlock curBB = builder.getInsertBlock();

        if (curBB == null || curBB.getTerminator() != null)
        {
            // If there is no insert point or the previous block is already
            // terminated, don't touch it.
        }
        else
        {
            // Otherwise, create a fall-through branch.
            builder.createBr(targetBB);
        }
        builder.clearInsertPoint();
    }

    /**
     * Emits code for static variable declared in block scope.
     * @param vd
     */
    private void emitStaticBlockVarDecl(VarDecl vd)
    {
        Value entry = localVarMaps.get(vd);
        assert entry == null:"Decl already exists in localdeclmap!";

        GlobalVariable gv = createStaticBlockVarDecl(vd, ".");

        // Store into localVarMaps before generating initializer to handle
        // circular reference.
        entry = gv;

        if (vd.getDeclType().isVariablyModifiedType())
            emitVLASize(vd.getDeclType());

        if (vd.hasInit())
        {
            Constant init = generator
                    .emitConstantExpr(vd.getInit(), vd.getDeclType(), this);

            // If constant emission failed, then this should be a C++ static
            // initializer.
            if (init == null)
            {
                // TODO generator.errorUnsupported(vd.getInit(), "constant l-value expression");
            }
            else
            {
                if (gv.getType() != init.getType())
                {
                    GlobalVariable oldGV = gv;

                    gv = new GlobalVariable(init.getType(),
                            oldGV.isConstant(),
                            init, "");

                    // Replace all uses of the old global with the new global
                    Constant newPtrForOldDecl =
                            ConstantExpr.getBitCast(gv, oldGV.getType());
                    oldGV.replaceAllUsesWith(newPtrForOldDecl);

                    // Erase the old global, since it is no longer used.
                    oldGV.eraseFromParent();
                }

                gv.setInitializer(init);
            }
        }
    }

    private GlobalVariable createStaticBlockVarDecl(VarDecl vd,
            String separator)
    {
        QualType ty = vd.getDeclType();
        assert ty.isConstantSizeType():"VLAs cann't be static";

        String contextName = "";
        if (curFnDecl != null)
            contextName = curFnDecl.getDeclName();
        else
            assert false:"Unknown context for block var decl";

        String name = contextName + "." + vd.getDeclName();
        Type lty = generator.getCodeGenTypes().convertTypeForMem(ty);
        return new GlobalVariable(lty,
                ty.isConstant(),
                generator.emitNullConstant(vd.getDeclType()),
                name);
    }
    private void emitGotoStmt(Tree.GotoStmt s)
    {

    }

    private void emitBreakStmt(Tree.BreakStmt s)
    {

    }

    private void finishFunction()
    {

    }

    private void emitContinueStmt(Tree.ContinueStmt s)
    {

    }

    private void emitDefaultStmt(Tree.DefaultStmt s)
    {

    }

    private void emitCaseStmt(Tree.CaseStmt s)
    {

    }

    private BasicBlock createBasicBlock(String name, Function parent)
    {
        return createBasicBlock(name, parent, null);
    }

    private BasicBlock createBasicBlock()
    {
        return createBasicBlock("", curFn, null);
    }

    private BasicBlock createBasicBlock(String name)
    {
        return createBasicBlock(name, curFn, null);
    }

    private BasicBlock createBasicBlock(String name, Function parent, BasicBlock before)
    {
        return BasicBlock.createBasicBlock(id++, name, parent, before);
    }
}
