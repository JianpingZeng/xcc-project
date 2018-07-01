/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package jlang.codegen;

import tools.Util;
import backend.ir.HIRBuilder;
import backend.support.LLVMContext;
import backend.type.IntegerType;
import backend.type.PointerType;
import backend.type.Type;
import backend.value.ConstantInt;
import backend.value.Value;
import jlang.sema.ASTContext;
import jlang.sema.Decl;
import jlang.type.*;
import tools.Util;

/**
 * The X86_32 ABI information.
 * @author Xlous.zeng
 * @version 0.1
 */
public class X86_32ABIInfo implements ABIInfo
{
    private ASTContext context;
    private boolean isSmallStructInRegABI;

    public X86_32ABIInfo(ASTContext ctx)
    {
        context = ctx;
        isSmallStructInRegABI = false;
    }

    public static boolean isRegisterSize(long size)
    {
        return size == 8 || size == 16 || size == 32 || size == 64;
    }

    public static boolean shouldReturnTypeInRegister(QualType ty, ASTContext ctx)
    {
        long size = ctx.getTypeSize(ty);

        if (!isRegisterSize(size))
            return false;

        if (ty.isBuiltinType() || ty.isPointerType() || ty.isAnyComplexType())
            return true;

        // Array treated as record.
        ArrayType.ConstantArrayType cat = ctx.getAsConstantArrayType(ty);
        if (cat != null)
            return shouldReturnTypeInRegister(cat.getElementType(), ctx);

        RecordType rt = ty.getAsStructureType();
        if (rt == null)
            return false;

        Decl.RecordDecl rd = rt.getDecl();
        for (int i = 0, e = rd.getNumFields(); i != e; i++)
        {
            Decl.FieldDecl fd = rd.getDeclAt(i);
            // Skip the empty field.
            if (isEmptyField(ctx, fd))
                continue;

            if (!shouldReturnTypeInRegister(fd.getType(), ctx))
                return false;
        }

        return false;
    }

    private static boolean isEmptyRecord(ASTContext ctx, QualType type)
    {
        RecordType rt = type.getAsStructureType();
        if (rt == null)
            return false;
        Decl.RecordDecl decl = rt.getDecl();
        for (int i = 0, e = decl.getNumFields(); i != e; i++)
        {
            if (!isEmptyField(ctx, decl.getDeclAt(i)))
                return false;
        }
        return true;
    }

    private static boolean isEmptyField(ASTContext ctx, Decl.FieldDecl fd)
    {
        if (fd.isUnamaedBitField())
            return true;

        QualType ft = fd.getType();
        ArrayType.ConstantArrayType cat = ctx.getAsConstantArrayType(ft);
        while (cat != null)
        {
            ft = cat.getElementType();
        }
        return isEmptyRecord(ctx, ft);
    }

    private static jlang.type.Type isSingleElementStruct(QualType ty, ASTContext ctx)
    {
        RecordType rt = ty.getAsStructureType();
        if (rt == null)
            return null;

        Decl.RecordDecl rd = rt.getDecl();
        if (rd.hasFlexibleArrayNumber())
            return null;

        jlang.type.Type found = null;
        for (int i = 0, e = rd.getNumFields(); i != e; i++)
        {
            Decl.FieldDecl fd = rd.getDeclAt(i);
            QualType ft = fd.getType();

            // Ignore empty field.
            if (isEmptyField(ctx, fd))
                continue;

            if (found != null)
                return null;

            ArrayType.ConstantArrayType cat = ctx.getAsConstantArrayType(ft);
            while (cat != null)
            {
                if (cat.getSize().getZExtValue() != 1)
                    break;

                ft = cat.getElementType();
            }

            if (!CodeGenFunction.hasAggregateLLVMType(ft))
            {
                found = ft.getType();
            }
            else
            {
                found = isSingleElementStruct(ft, ctx);
                if (found == null)
                    return null;
            }
        }
        return found;
    }

    private static boolean typeContainSSEVector(Decl.RecordDecl decl, ASTContext ctx)
    {
        return false;
    }

    private static int getIndirectArgumentAlignment(QualType ty, ASTContext ctx)
    {
        int align = ctx.getTypeAlign(ty);
        if (align < 128)
            return 0;
        RecordType rt = ty.getAsStructureType();
        if (rt != null && typeContainSSEVector(rt.getDecl(), ctx))
        {
            return 16;
        }
        return 0;
    }

    private static boolean is32Or64BitBasicType(QualType ty, ASTContext ctx)
    {
        if (!ty.isBuiltinType() && !ty.isPointerType())
            return false;

        long size = ctx.getTypeSize(ty);
        return size == 32 || size == 64;
    }

    private static boolean areAllFields32Or64BitBasicType(Decl.RecordDecl decl, ASTContext ctx)
    {
        for (int i = 0, e = decl.getNumFields(); i != e; i++)
        {
            Decl.FieldDecl fd = decl.getDeclAt(i);
            if (!is32Or64BitBasicType(fd.getType(), ctx))
                return false;

            // Reject the bitfield.
            if (fd.isBitField())
                return false;
        }
        return true;
    }

    private ABIArgInfo classifyReturnType(QualType retType)
    {
        if (retType.isVoidType())
            return ABIArgInfo.getIgnore();
        else if (CodeGenFunction.hasAggregateLLVMType(retType))
        {
            // Structures with flexible arrays are always indirect.
            RecordType rt = retType.getAsStructureType();
            if (rt != null && rt.getDecl().hasFlexibleArrayNumber())
                return ABIArgInfo.getIndirect(0);

            if (!isSmallStructInRegABI && !retType.isAnyComplexType())
                return ABIArgInfo.getIndirect(0);

            // Classify "single element" structs as their element type.
            jlang.type.Type seltTy = isSingleElementStruct(retType, context);
            if (seltTy != null)
            {
                BuiltinType bt = seltTy.getAsBuiltinType();
                if (bt != null)
                {
                    if (bt.isIntegerType())
                    {
                        long size = context.getTypeSize(retType);
                        return ABIArgInfo.getCoerce(IntegerType.get((int) size));
                    }
                    else if (bt.getTypeClass() == TypeClass.Float)
                    {
                        Util.assertion(context.getTypeSize(seltTy) == context.getTypeSize(                                retType),  "Unexpect single element structure size!");

                        return ABIArgInfo.getCoerce(LLVMContext.FloatTy);
                    }
                    else if (bt.getTypeClass() == TypeClass.Double)
                    {
                        Util.assertion(context.getTypeSize(seltTy) == context.getTypeSize(                                retType),  "Unexpect single element structure size!");

                        return ABIArgInfo.getCoerce(LLVMContext.DoubleTy);
                    }
                }
                else if (seltTy.isPointerType())
                {
                    Type ptrTy = PointerType.getUnqual(LLVMContext.Int8Ty);
                    return ABIArgInfo.getCoerce(ptrTy);
                }
            }

            // Small structures which are register sized are generally returned
            // in a register.
            if (shouldReturnTypeInRegister(retType, context))
            {
                long size = context.getTypeSize(retType);
                return ABIArgInfo.getCoerce(IntegerType.get((int) size));
            }

            return ABIArgInfo.getIndirect(0);
        }
        else
        {
            return retType.isPromotableIntegerType() ? ABIArgInfo.getExtend() :
                    ABIArgInfo.getDirect();
        }
    }

    private ABIArgInfo classifyArgumentType(QualType argType)
    {
        if (CodeGenFunction.hasAggregateLLVMType(argType))
        {
            RecordType rt = argType.getAsStructureType();
            if (rt != null && rt.getDecl().hasFlexibleArrayNumber())
            {
                return ABIArgInfo.getIndirect(getIndirectArgumentAlignment(argType, context));
            }

            // Ignore empty structure.
            if (argType.isStructureType() && context.getTypeSize(argType) == 0)
            {
                return ABIArgInfo.getIgnore();
            }

            rt = argType.getAsStructureType();
            if (rt != null && context.getTypeSize(argType) < 4*32 &&
                    areAllFields32Or64BitBasicType(rt.getDecl(), context))
            {
                return ABIArgInfo.getExpand();
            }

            return ABIArgInfo.getIndirect(getIndirectArgumentAlignment(argType, context));
        }
        else
        {
            return argType.isPromotableIntegerType() ? ABIArgInfo.getExtend() :
                    ABIArgInfo.getDirect();
        }
    }

    @Override
    public void computeInfo(CodeGenTypes.CGFunctionInfo fi, ASTContext ctx)
    {
        fi.setReturnInfo(classifyReturnType(fi.getReturnType()));
        for (int i = 0, e = fi.getNumOfArgs(); i < e; i++)
            fi.setArgInfo(i, classifyArgumentType(fi.getArgInfoAt(i).type));
    }

    @Override
    public Value emitVAArg(Value vaListAddr, QualType ty, CodeGenFunction cgf)
    {
        backend.type.Type bp = PointerType.getUnqual(LLVMContext.Int8Ty);
        backend.type.Type bpp = PointerType.getUnqual(bp);

        HIRBuilder builder = cgf.builder;
        Value valistAddrAsBPP = builder.createBitCast(vaListAddr, bp, "ap");

        Value addr = builder.createLoad(valistAddrAsBPP, false, "ap.cur");
        Type pty = PointerType.getUnqual(cgf.convertType(ty));
        Value addrTyped = builder.createBitCast(addr, pty);

        long offset = Util.roundUp(cgf.getContext().getTypeSize(ty)/ 8, 4);
        Value nextAddr = builder.createGEP(addr, ConstantInt.get(LLVMContext.Int32Ty, offset), "ap.next");
        builder.createStore(nextAddr, valistAddrAsBPP);

        return addrTyped;
    }
}
