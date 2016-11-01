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

import backend.type.Type;
import backend.value.Constant;
import backend.value.Function;
import backend.value.Instruction;
import backend.value.Value;
import frontend.sema.Decl.FunctionDecl;
import frontend.sema.Decl.VarDecl;
import frontend.type.QualType;
import java.util.ArrayList;

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

    private HIRBuilder builder;

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

        emitFunctionPrologue(curFn, args);
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
    private void emitFunctionPrologue(Function fn, ArrayList<VarDecl> args)
    {
        if (curFnDecl.hasImplicitReturnZero())
        {
            QualType retType = curFnDecl.getReturnType().getUnQualifiedType();
            Type backendTy = generator.getCodeGenTypes().convertType(retType);
            Constant zero = Constant.getNullValue(backendTy);
            builder.createStore(zero, returnValue);
        }
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
