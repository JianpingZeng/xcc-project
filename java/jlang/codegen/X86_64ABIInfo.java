/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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

import backend.support.LLVMContext;
import backend.type.StructType;
import backend.type.Type;
import backend.type.VectorType;
import backend.value.Value;
import jlang.basic.TargetInfo;
import jlang.sema.ASTContext;
import jlang.sema.ASTRecordLayout;
import jlang.sema.Decl;
import jlang.type.*;
import tools.*;

import static jlang.codegen.CodeGenFunction.hasAggregateLLVMType;

/**
 * The X86_64 ABI information. Please visit the System V AMD64 Calling convention
 * for more details.
 * @author Jianping Zeng
 * @version 0.1
 */
public class X86_64ABIInfo implements ABIInfo
{
    /**
     * A class enumerates some static constant used for
     * illustrating how the argument was passed. Each kind
     * corresponding to concrete register or stack slot in X86_64
     * machine.
     */
    private interface Class
    {
        /**
         * Describes the argument can be fitted into general purpose register.
         */
        int INTEGER = 0;
        /**
         * This argument should passed by vector register.
         */
        int SSE = 1;
        /**
         * This argument fit into a vector register and can be
         * passed and returned in the upper bytes of it.
         */
        int SSEUP = 2;
        /**
         * This argument fit into the X86 floating stack register.
         */
        int X87 = 3;
        /**
         * * This argument fit into the upper bytes of X86 floating stack register.
         */
        int X87UP = 4;
        /**
         * Describes the class should be passed and returned via the
         * X87 FPU.
         */
        int COMPLEX_X87 = 5;
        /**
         * This class used to initialize the algorithms. It will be
         * used for padding and empty strucutres and unions.
         */
        int NO_CLASS = 6;
        /**
         * This class was used as describing the argument should be passed and
         * returned in memory via stack.
         */
        int MEMORY = 7;
    }
    private TargetInfo target;

    @Override
    public void computeInfo(CodeGenTypes.CGFunctionInfo fi, ASTContext ctx)
    {
        target = ctx.getTargetInfo();
        // set the return argument info.
        ABIArgInfo retInfo = classifyReturnType(fi.getReturnType(), ctx);
        fi.setReturnInfo(retInfo);

        // records the number of available registers.
        int freeIntRegisters = 6, freeSSERegisters = 8;

        // If the return value is indirect, then the hidden argument is consuming one
        // integer register.
        if (retInfo.isIndirect())
            --freeIntRegisters;

        // handle each argument for left to right.
        for (int i = 0, e = fi.getNumOfArgs(); i < e; i++)
        {
            OutRef<Integer> needsIntRegs = new OutRef<>(0);
            OutRef<Integer> needsSSERegs = new OutRef<>(0);
            ABIArgInfo info = classifyArgumentType(fi.getArgInfoAt(i).type,
                    ctx, needsIntRegs, needsSSERegs);

            if (freeIntRegisters >= needsIntRegs.get() &&
                    freeSSERegisters >= needsSSERegs.get())
            {
                freeIntRegisters -= needsIntRegs.get();
                freeSSERegisters -= needsSSERegs.get();
            }
            else
                info = getIndirectResult(fi.getArgInfoAt(i).type, ctx);
            fi.setArgInfo(i, info);
        }
    }

    private ABIArgInfo classifyArgumentType(QualType argType, ASTContext ctx,
                                            OutRef<Integer> needsIntRegs,
                                            OutRef<Integer> needsSSERegs)
    {
        Pair<Integer, Integer> resClass = classify(argType, 0, ctx);
        int hi = resClass.first, lo = resClass.second;
        Util.assertion(hi != Class.MEMORY || lo == Class.MEMORY, "unknown memory class");
        Util.assertion(hi != Class.SSEUP || lo == Class.SSE, "unknown sse class");

        backend.type.Type resType = null;
        switch (lo)
        {
            case Class.NO_CLASS:
                return ABIArgInfo.getIgnore();
            case Class.MEMORY:
            case Class.COMPLEX_X87:
            case Class.X87:
                return getIndirectResult(argType, ctx);
            case Class.INTEGER:
                needsIntRegs.set(needsIntRegs.get()+1);
                resType = LLVMContext.Int64Ty;
                break;
            case Class.SSE:
                needsSSERegs.set(needsSSERegs.get()+1);
                resType = LLVMContext.DoubleTy;
                break;
            case Class.SSEUP:
            case Class.X87UP:
                Util.assertion(false, "invalid classification for lo word!");
            default:
                Util.shouldNotReachHere("SEEUp and X87_UP should not be setted as high");
        }

        switch (hi)
        {
            case Class.MEMORY:
            case Class.X87:
            case Class.COMPLEX_X87:
                Util.shouldNotReachHere("invalid classification for hi word!");
                break;
            case Class.NO_CLASS:
                break;  // handled previously.
            case Class.INTEGER:
                resType = StructType.get(resType, LLVMContext.Int64Ty);
                needsIntRegs.set(needsIntRegs.get()+1);
                break;
            case Class.X87UP:
            case Class.SSE:
                resType = StructType.get(resType, LLVMContext.DoubleTy);
                needsSSERegs.set(needsSSERegs.get()+1);
                break;
            case Class.SSEUP:
                //Util.assertion(lo == Class.SSE, "Unexpected SSEUP classification");
                resType = VectorType.get(resType, 2);
                break;
        }
        Util.assertion(resType != null);
        return getCoerseResult(argType, resType, ctx);
    }

    @Override
    public Value emitVAArg(Value vaListAddr, QualType ty, CodeGenFunction cgf)
    {
        // Assume that va_list type is correct; should be pointer to LLVM type:
        // struct {
        //   i32 gp_offset;
        //   i32 fp_offset;
        //   i8* overflow_arg_area;
        //   i8* reg_save_area;
        // };
        Util.shouldNotReachHere("VAArg not implemented currently");
        return null;
    }

    /**
     * Gives a classification according the classification algorithm proposed in System V AMD64 ABI
     * (3.2.3p1).
     * @param type
     * @param offsetBase
     * @param ctx
     * @return
     */
    private Pair<Integer, Integer> classify(QualType type, int offsetBase, ASTContext ctx)
    {
        int[] res = {Class.NO_CLASS, Class.NO_CLASS};
        int current = offsetBase < 64 ? 1 : 0;
        res[current] = Class.MEMORY;

        BuiltinType bt = type.getAsBuiltinType();
        if (bt != null) {
            // handles primitive type.
            if (bt.getTypeClass() == TypeClass.Void) {
                res[current] = Class.NO_CLASS;
            }
            if (bt.getTypeClass() == TypeClass.Int128 ||
                    bt.getTypeClass() == TypeClass.UInt128) {
                res[1] = Class.INTEGER;
                res[0] = Class.INTEGER;
            } else if (bt.getTypeClass() >= TypeClass.Bool &&
                    bt.getTypeClass() <= TypeClass.LongLong)
                res[current] = Class.INTEGER;
            else if (bt.getTypeClass() == TypeClass.Float ||
                    bt.getTypeClass() == TypeClass.Double)
                res[current] = Class.SSE;
            else if (bt.getTypeClass() == TypeClass.LongDouble) {
                FltSemantics flt = target.getLongDoubleFormat();
                if (flt == APFloat.x87DoubleExtended) {
                    res[1] = Class.X87;
                    res[0] = Class.X87UP;
                } else if (flt == APFloat.IEEEdouble)
                    res[current] = Class.SSE;
                else if (flt == APFloat.IEEEquad) {
                    res[1] = Class.SSE;
                    res[0] = Class.SSEUP;
                } else
                    Util.shouldNotReachHere("Unknown long double FltSemantics");
            }
        }
        else if (type.isPointerType())
        {
            res[current] = Class.INTEGER;
        }
        else if (type.isEnumeralType())
        {
            return classify(type.getAsEnumType().getDecl().getIntegerType(), offsetBase, ctx);
        }
        else if (type.isComplexType())
        {
            Util.shouldNotReachHere("ComplexType isn't implemented!");
        }
        else if (type.isVectorType())
        {
            int size = (int) ctx.getTypeSize(type);
            if (size == 32)
            {
                // gcc passes all <4 x char>, <2 x short>, <1 x int>, <1 x
                // float> as integer.
                res[current] = Class.INTEGER;

                // If this type crosses an eightbyte boundary, it should be
                // split.
                int realOffset = offsetBase / 64;
                int imagOffset = (offsetBase + 31)/64;
                if (realOffset != imagOffset)
                {
                    res[0] = res[1];
                }
            }
            else if (size == 64)
            {
                // gcc passes <1 x double> in memory.
                if (type.isSpecifiedBuiltinType(TypeClass.Double))
                    res[current] = Class.MEMORY;

                // gcc passes <1 x long long> in as integer.
                else if (type.isSpecifiedBuiltinType(TypeClass.LongLong))
                    res[current] = Class.INTEGER;
                else
                    res[current] = Class.SSE;

                // If this type crosses an eightbyte boundary, it should be
                // split.
                if (offsetBase != 0 && offsetBase != 64)
                    res[0] = res[1];
            }
            else if (size == 128)
            {
                res[1] = Class.SSE;
                res[0] = Class.SSEUP;
            }
        }
        else if (type.isConstantArrayType())
        {
            ArrayType.ConstantArrayType cat = ctx.getAsConstantArrayType(type);
            QualType eleType = cat.getElementType();
            int size = (int) ctx.getTypeSize(cat);

            // 3.2.3p2, Rule 1, if the size of an object is larger than four eightbytes,
            // or it contains a unaligned fields, it has class MEMORY.
            if (size > 256 || (offsetBase % ctx.getTypeAlign(eleType)) != 0)
                return Pair.get(res[0], res[1]);

            // Otherwise implement simplified merge. We could be smarter about
            // this, but it isn't worth it and would be harder to verify.
            res[current] = Class.MEMORY;
            int num = (int) cat.getSize().getZExtValue();
            int eleSize = (int) ctx.getTypeSize(eleType);
            for (int i = 0; i < num; i++, offsetBase += eleSize)
            {
                // hi, lo
                Pair<Integer, Integer> field = classify(eleType, offsetBase, ctx);
                res[0] = merge(res[0], field.first);
                res[1] = merge(res[1], field.second);

                if (field.first == Class.MEMORY || field.second == Class.MEMORY)
                    break;
            }
            // The post merger clean up.
            postMerge(size, res);
            Util.assertion(res[0] != Class.SSEUP || res[1] == Class.SSE);
            return Pair.get(res[0], res[1]);
        }
        else if (type.isRecordType())
        {
            // 3.2.3p3
            RecordType rt = type.getAsRecordType();
            int size = (int) ctx.getTypeSize(rt);
            // it has a unaligned member.
            if (size > 256 || (offsetBase % ctx.getTypeAlign(rt)) != 0)
                return Pair.get(res[0], res[1]);

            // Otherwise implement simplified merge. We could be smarter about
            // this, but it isn't worth it and would be harder to verify.
            res[current] = Class.NO_CLASS;
            Decl.RecordDecl rd = rt.getDecl();
            ASTRecordLayout layout = ctx.getASTRecordLayout(rd);
            for (int i = 0, e = rd.getNumFields(); i < e; i++)
            {
                Decl.FieldDecl fd = rd.getDeclAt(i);
                offsetBase += layout.getFieldOffsetAt(i);
                boolean isBitField = fd.isBitField();
                QualType eleTy = fd.getType();
                if (!isBitField && (offsetBase % ctx.getTypeAlign(eleTy)) != 0)
                    return Pair.get(res[0], Class.MEMORY);

                int[] field = {Class.NO_CLASS, Class.NO_CLASS};
                if (isBitField)
                {
                    // Ignore padding bit-fields.
                    if (fd.isUnamaedBitField())
                        continue;
                    long offset = offsetBase + layout.getFieldOffsetAt(i);
                    long bitSize = fd.getBitWidth().evaluateAsInt(ctx).getZExtValue();
                    long eb_lo = offset / 64;
                    long eb_hi = (offset + bitSize - 1) / 64;
                    if (eb_lo != 0)
                    {
                        Util.assertion(eb_hi == eb_lo, "invalid classification, type > 16 bytes");
                        field[0] = Class.INTEGER;
                    }
                    else
                    {
                        field[1] = Class.INTEGER;
                        field[0] = eb_hi != 0 ? Class.INTEGER : Class.NO_CLASS;
                    }
                }
                else
                {
                    // hi, lo
                    Pair<Integer, Integer> temp = classify(eleTy, offsetBase, ctx);
                    res[0] = merge(res[0], temp.first);
                    res[1] = merge(res[1], temp.second);
                    if (res[1] == Class.MEMORY || res[0] == Class.MEMORY)
                        break;
                }
            }
            // The post merger clean up.
            postMerge(size, res);
            Util.assertion(res[0] != Class.SSEUP || res[1] == Class.SSE);
            return Pair.get(res[0], res[1]);
        }
        else
            Util.shouldNotReachHere("Unknown type!");
        return Pair.get(res[0], res[1]);
    }

    private void postMerge(int size, int[] res)
    {
        // AMD64-ABI 3.2.3p2: Rule 5. Then a post merger cleanup is done:
        //
        // (a) If one of the classes is Memory, the whole argument is passed in
        //     memory.
        //
        // (b) If X87UP is not preceded by X87, the whole argument is passed in
        //     memory.
        //
        // (c) If the size of the aggregate exceeds two eightbytes and the first
        //     eightbyte isn't SSE or any other eightbyte isn't SSEUP, the whole
        //     argument is passed in memory. NOTE: This is necessary to keep the
        //     ABI working for processors that don't support the __m256 type.
        //
        // (d) If SSEUP is not preceded by SSE or SSEUP, it is converted to SSE.
        //
        // Some of these are enforced by the merging logic.  Others can arise
        // only with unions; for example:
        //   union { _Complex double; unsigned; }
        //
        // Note that clauses (b) and (c) were added in 0.98.
        if (res[1] == Class.MEMORY)
            res[0] = Class.MEMORY;
        if (res[1] == Class.X87UP && res[0] != Class.X87 && honorRevision0_98())
            res[0] = Class.MEMORY;
        if (size > 128 && (res[0] != Class.SSE || res[1] != Class.SSEUP))
            res[0] = Class.MEMORY;
        if (res[1] == Class.SSEUP && res[0] != Class.SSE)
            res[1] = Class.SSE;
    }

    /**
     * The 0.98 ABI revision clarified a lot of ambiguities,
     * unfortunately in ways that were not always consistent with
     * certain previous compilers.  In particular, platforms which
     * required strict binary compatibility with older versions of GCC
     * may need to exempt themselves.
     * @return
     */
    private boolean honorRevision0_98()
    {
        return !target.getTriple().isDarwin();
    }

    /**
     * Merges the accumulate class with field class according 3.2.3p3 Rule 4.
     * @param accum
     * @param field
     * @return
     */
    private int merge(int accum, int field)
    {
        // System V AMD64 Convention:
        // (a). If both classes are equal, this is the resulting class.
        // (b). If one of the classes in NO_CLASS, the resulting is the other class.
        // (c). If one of the classes is MEMORY, the result is the MEMORY class.
        // (d). If one of the classes is INTEGER, the result is the INTEGER.
        // (e). If one of the classes is X87, X87UP, COMPLEX_X87 class, MEMORY is used as class.
        // (f). Otherwise, class SSE is used.
        if (accum == field || field == Class.NO_CLASS)
            return accum;
        if (accum == Class.NO_CLASS) return field;
        if (accum == Class.MEMORY || field == Class.MEMORY)
            return Class.MEMORY;
        if (accum == Class.INTEGER || field == Class.INTEGER)
            return Class.INTEGER;
        if (accum == Class.X87 || accum == Class.X87UP || accum == Class.COMPLEX_X87 ||
                field == Class.X87 || field == Class.X87UP || field == Class.COMPLEX_X87)
            return Class.MEMORY;
        return Class.SSE;
    }

    private ABIArgInfo classifyReturnType(QualType retType, ASTContext ctx)
    {
        Pair<Integer, Integer> resClass = classify(retType, 0, ctx);
        int hi = resClass.first, lo = resClass.second;
        Util.assertion(hi != Class.MEMORY || lo == Class.MEMORY, "unknown memory class");
        Util.assertion(hi != Class.SSEUP || lo == Class.SSE, "unknown sse class");

        backend.type.Type resType = null;
        switch (lo)
        {
            case Class.NO_CLASS:
                return ABIArgInfo.getIgnore();
            case Class.MEMORY:
                return getIndirectResult(retType, ctx);
            case Class.INTEGER:
                resType = LLVMContext.Int64Ty;
                break;
            case Class.SSE:
                resType = LLVMContext.DoubleTy;
                break;
            case Class.X87:
                resType = LLVMContext.X86_FP80Ty;
                break;
            case Class.COMPLEX_X87:
                resType = StructType.get(LLVMContext.X86_FP80Ty, LLVMContext.X86_FP80Ty);
                break;
            default:
                Util.shouldNotReachHere("SEEUp and X87_UP should not be setted as high");
        }

        switch (hi)
        {
            case Class.X87:
                Util.shouldNotReachHere();
                break;
            case Class.NO_CLASS:
            case Class.COMPLEX_X87:
                break;  // handled previously.
            case Class.INTEGER:
                resType = StructType.get(resType, LLVMContext.Int64Ty);
                break;
            case Class.SSE:
                resType = StructType.get(resType, LLVMContext.DoubleTy);
                break;
            case Class.SSEUP:
                resType = VectorType.get(resType, 2);
                break;
            case Class.X87UP:
                if (lo != Class.X87)
                    resType = StructType.get(resType, LLVMContext.DoubleTy);
                break;
        }
        Util.assertion(resType != null);
        return getCoerseResult(retType, resType, ctx);
    }

    private ABIArgInfo getCoerseResult(QualType retType, Type coerseTo, ASTContext ctx)
    {
        if (coerseTo.equals(LLVMContext.Int64Ty))
        {
            if (retType.isIntegralType() || retType.isPointerType())
                return retType.isPromotableIntegerType() ?
                        ABIArgInfo.getExtend() : ABIArgInfo.getDirect();
        }
        else if (coerseTo.equals(LLVMContext.DoubleTy))
        {
            QualType cty = ctx.getCanonicalType(retType);
            if (cty.equals(ctx.FloatTy) || cty.equals(ctx.DoubleTy))
                return ABIArgInfo.getDirect();
        }
        return ABIArgInfo.getCoerce(coerseTo);
    }

    /**
     * If this argument will be passed by indirect, let the compiler naturally select ABIArgInfo.
     * @param retType
     * @param ctx
     * @return
     */
    private ABIArgInfo getIndirectResult(QualType retType, ASTContext ctx)
    {
        if (!hasAggregateLLVMType(retType))
        {
            return retType.isPromotableIntegerType() ?
                    ABIArgInfo.getExtend() : ABIArgInfo.getDirect();
        }
        return ABIArgInfo.getIndirect(0);
    }
}
