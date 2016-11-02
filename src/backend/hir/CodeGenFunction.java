package backend.hir;
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

import backend.hir.CodeGenTypes.ArgTypeInfo;
import backend.type.FunctionType;
import backend.type.Type;
import backend.value.*;
import frontend.sema.Decl.FunctionDecl;
import frontend.sema.Decl.VarDecl;
import frontend.type.QualType;

import java.util.ArrayList;
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

    public CodeGenFunction(HIRGenModule generator)
    {
        this.generator = generator;
        builder = new HIRBuilder();
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
        emitFunctionBody(functionArgList);

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
    private void emitFunctionPrologue(Function fn, FunctionType fnType, ArrayList<VarDecl> args)
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
        Iterator<ArgTypeInfo> infoItr = fnType.getParamTypes().iterator();
        for (VarDecl vd : args)
        {
            QualType ty = vd.getDeclType();
            Value v = argItr.next();
            final ArgTypeInfo ArgInfo = infoItr.next();

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

    private void emitFunctionBody(ArrayList<VarDecl> args)
    {

    }

    private void finishFunction()
    {

    }

    private BasicBlock createBasicBlock(String name, Function parent)
    {
        return createBasicBlock(name, parent, null);
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
