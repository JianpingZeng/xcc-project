package jlang.codegen;
/*
 * Xlous C language CompilerInstance
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

import backend.value.Module;
import backend.target.TargetData;
import backend.type.FunctionType;
import backend.type.*;
import backend.type.PointerType;
import backend.type.Type;
import tools.APFloat;
import tools.FltSemantics;
import jlang.basic.TargetInfo;
import jlang.sema.ASTContext;
import jlang.sema.Decl;
import jlang.sema.Decl.FieldDecl;
import jlang.sema.Decl.VarDecl;
import jlang.type.ArrayType;
import jlang.type.ArrayType.VariableArrayType;
import jlang.type.*;
import tools.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import static jlang.type.TypeClass.*;
import static jlang.type.TypeClass.Double;
import static jlang.type.TypeClass.Enum;
import static jlang.type.TypeClass.Float;
import static jlang.type.TypeClass.Short;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class CodeGenTypes
{
    public static class CGFunctionInfo
    {
        public static class ArgInfo
        {
            QualType type;
            ABIArgInfo info;
        }

        private ArgInfo[] args;

        public CGFunctionInfo(QualType resType, ArrayList<QualType> argTypes)
        {
            args = new ArgInfo[argTypes.size() + 1];
            args[0] = new ArgInfo();
            args[0].type = resType;

            for (int i = 0; i< argTypes.size(); i++)
            {
                args[i + 1] = new ArgInfo();
                args[i + 1].type = argTypes.get(i);
            }
        }

        public QualType getReturnType(){return args[0].type;}

        public ABIArgInfo getReturnInfo() {return args[0].info;}

        public int getNumOfArgs(){return args.length;}

        public ArgInfo getArgInfoAt(int idx)
        {
            assert idx>=0 && idx < args.length;
            return args[idx];
        }
    }

    public static class ArgTypeInfo
    {
        public QualType frontendType;
        public Type backendType;

        public ArgTypeInfo(QualType frontendType, Type backendType)
        {
            this.frontendType = frontendType;
            this.backendType = backendType;
        }
    }

    static class BitFieldInfo
    {
        BitFieldInfo(int fieldNo, int start, int size)
        {
            this.fieldNo = fieldNo;
            this.start = start;
            this.size = size;
        }

        int fieldNo;
        int start;
        int size;
    }
    private HIRModuleGenerator builder;

    /**
     * This is a cache diagMapping jlang type to backend type.
     */
    private HashMap<jlang.type.Type, backend.type.Type> typeCaches;

    /**
     * This set keeps track of records that we're currently
     * converting to an IR type.  For example, when converting:
     * struct A { struct B { int x; } } when processing 'x', the 'A' and 'B'
     * types will be in this set.
     */
    private HashSet<jlang.type.Type> recordBeingLaidOut;

    private HashSet<CGFunctionInfo> functionBeingProcessed;

    private LinkedList<Decl.RecordDecl> deferredRecords;

    /**
     * This contains the HIR type for any converted RecordDecl.
     */
    private HashMap<jlang.type.Type, StructType> recordDeclTypes;

    /**
     * True if we didn't layout a function due to a being inside a recursive struct conversion,
     * set this variable to true;
     */
    private boolean skipLayout = false;

    private HashMap<FieldDecl, Integer> fieldInfo;

    private HashMap<FieldDecl, BitFieldInfo> bitfields;

    private HashMap<jlang.type.Type, CGRecordLayout> cgRecordLayout;

    private ASTContext context;
    private TargetInfo target;
    private Module theModule;
    private TargetData targetData;
    private ABIInfo theABIInfo;

    private LinkedList<Pair<QualType, OpaqueType>> pointersToResolve;

    private HashMap<jlang.type.Type, backend.type.Type> tagDeclTypes;
    private HashMap<jlang.type.Type, backend.type.Type> functionTypes;

    public CodeGenTypes(HIRModuleGenerator moduleBuilder, TargetData td)
    {
        builder = moduleBuilder;
        typeCaches = new HashMap<>();
        recordBeingLaidOut = new HashSet<>();
        functionBeingProcessed = new HashSet<>();
        deferredRecords = new LinkedList<>();
        recordDeclTypes = new HashMap<>();
        cgRecordLayout = new HashMap<>();
        fieldInfo = new HashMap<>();
        bitfields = new HashMap<>();
        context = moduleBuilder.getASTContext();
        target = context.target;
        theModule = moduleBuilder.getModule();
        targetData = td;
        pointersToResolve = new LinkedList<>();
        tagDeclTypes = new HashMap<>();
        functionTypes = new HashMap<>();
    }

    public backend.type.FunctionType getFunctionType(CGFunctionInfo fi, boolean isVaridic)
    {
        boolean inserted = functionBeingProcessed.contains(fi);
        assert inserted:"recursively process function.";

        ArrayList<Type> argTypes = new ArrayList<>();
        backend.type.Type restType = null;
        ABIArgInfo retAI = fi.getReturnInfo();
        switch (retAI.getKind())
        {
            case Direct:
            {
                restType = retAI.getType();
                break;
            }
            case Indirect:
            {
                restType = Type.VoidTy;
                QualType ret = fi.getReturnType();
                Type ty = convertType(ret);
                argTypes.add(PointerType.get(ty, ret.getAddressSpace()));
                break;
            }
            case Ignore:
            {
                restType = Type.VoidTy;
                break;
            }
        }

        for (int i = 0, e = fi.getNumOfArgs(); i<e; i++)
        {
            CGFunctionInfo.ArgInfo argInfo = fi.getArgInfoAt(i);
            ABIArgInfo argAI = argInfo.info;
            switch (argAI.getKind())
            {
                case Ignore:
                    break;
                case Indirect:
                {
                    // Indirect argument always on stack.
                    Type ty = convertTypeForMem(argInfo.type);
                    argTypes.add(PointerType.get(ty, argInfo.type.getAddressSpace()));
                    break;
                }
                case Direct:
                {
                    Type argType = argAI.getType();

                    // If the type is aggregate type, flatten it.
                    if (argType instanceof StructType)
                    {
                        StructType st = (StructType)argType;
                        for (int j = 0, size = st.getNumOfElements(); j<e;i++)
                            argTypes.add(st.getElementType(j));
                    }
                    else
                    {
                        argTypes.add(argType);
                    }
                    break;
                }
            }
        }
        boolean erased = functionBeingProcessed.remove(fi);
        assert erased:"Not in set?";
        return FunctionType.get(restType, argTypes, isVaridic);
    }

    /**
     * Convert type {@code t} into a backend.type. This differs from {@linkplain
     * #convertType(QualType)} in that it is used to convert to the memory representation
     * for a type. For example, the scalar representation for _Bool is i1, but it's memory
     * representation is usually i8 or i32, depending on the target.
     * @param t
     * @return
     */
    public backend.type.Type convertTypeForMem(QualType t)
    {
        backend.type.Type res = convertType(t);

        // If this is a non-bool type, don't map it.
        if (!res.equals(Type.Int1Ty))
            return res;

        return backend.type.IntegerType.get((int)builder.getASTContext().getTypeSize(t));
    }

    private backend.type.Type getTypeForFormat(FltSemantics flt)
    {
        if (flt.equals(APFloat.IEEEsingle))
            return Type.FloatTy;
        if (flt.equals(APFloat.IEEEdouble))
            return Type.DoubleTy;
        if (flt.equals(APFloat.IEEEquad))
            return Type.FP128Ty;
        if (flt.equals(APFloat.x87DoubleExtended))
            return Type.X86_FP80Ty;
        assert false:"Unknown float format!";
        return null;
    }

    /**
     * Code to verify a given function type is complete, i.e. the return type
     * and all of the argument types are complete.
     * @param fnType
     * @return
     */
    public static TagType verifyFunctionTypeComplete(jlang.type.FunctionType fnType)
    {
        if (fnType.getResultType().getType() instanceof jlang.type.TagType)
        {
            TagType tt = (TagType)fnType.getResultType().getType();
            if(!tt.getDecl().isCompleteDefinition())
                return tt;
        }
        if (fnType instanceof FunctionProtoType)
        {
            FunctionProtoType fpt = (FunctionProtoType)fnType;
            for (int i = 0; i < fpt.getNumArgs(); i++)
            {
                if (fpt.getArgType(i).getType() instanceof jlang.type.TagType)
                {
                    TagType tt = (TagType)fpt.getArgType(i).getType();
                    if (!tt.getDecl().isCompleteDefinition())
                        return tt;
                }
            }
        }
        return null;
    }

    /**
     * Laid out the tagged decl type like struct or union or enum.
     * @param td
     * @return
     */
    private backend.type.Type convertTagDeclType(Decl.TagDecl td)
    {
        jlang.type.Type key = context.getTagDeclType(td).getType();
        if (tagDeclTypes.containsKey(key))
            return tagDeclTypes.get(key);

        if (!td.isCompleteDefinition())
        {
            backend.type.Type res = OpaqueType.get();
            tagDeclTypes.put(key, res);
            return res;
        }

        // If this is enum decl, just treat it as integral type.
        if (td.isEnum())
        {
            return convertTypeRecursive(((Decl.EnumDecl)td).getPromotionType());
        }

        OpaqueType placeHolderType = OpaqueType.get();
        tagDeclTypes.put(key, placeHolderType);

        Decl.RecordDecl rd = (Decl.RecordDecl)td;

        CGRecordLayout layout = CGRecordLayoutBuilder.computeLayout(this, rd);

        cgRecordLayout.put(key, layout);
        backend.type.Type resType = layout.getLLVMType();
        placeHolderType.refineAbstractTypeTo(resType);
        return placeHolderType;
    }

    private backend.type.Type convertNewType(QualType t)
    {
        jlang.type.Type ty = context.getCanonicalType(t).getType();
        switch (ty.getTypeClass())
        {
            // Builtin type.
            case Void:
                // LLVM void type can only be used as the result of a function call.
                // just map to the same as char.
                return backend.type.IntegerType.get(8);
            case Bool:
                return Type.Int1Ty;
            case Char_U:
            case UShort:
            case UInt:
            case ULong:
            case ULongLong:
            case SChar:
            case Short:
            case Int:
            case Long:
            case LongLong:
                return backend.type.IntegerType.get((int) context.getTypeSize(t));
            case Float:
            case Double:
            case LongDouble:
                return getTypeForFormat(context.getFloatTypeSemantics(t));

            case Complex:
            {
                // TODO 9/26
                assert false:"ComplexType is not supported";
                break;
            }
            case Pointer:
            {
                jlang.type.PointerType pt = ty.getAsPointerType();
                QualType qualType = pt.getPointeeType();
                OpaqueType pointeeType = OpaqueType.get();
                pointersToResolve.add(Pair.get(qualType, pointeeType));
                return backend.type.PointerType.get(pointeeType, qualType.getAddressSpace());
            }
            case VariableArray:
            {
                VariableArrayType a = (VariableArrayType)ty;
                assert a.getIndexTypeQuals() == 0 : "FIXME: we only handle trivial array!";

                // VLAs resolve to the innermost element type; this matches
                // the return of alloca, and there isn't any obviously better choice.
                return convertTypeForMemRecursive(a.getElementType());
            }
            case IncompleteArray:
            {
                ArrayType.IncompleteArrayType a = (ArrayType.IncompleteArrayType) ty;
                assert a.getIndexTypeQuals() == 0
                        : "FIXME: we only handle trivial array!";

                // int X[] -> [0 x int], unless the element type is not sized.  If it is
                // unsized (e.g. an incomplete struct) just use [0 x i8].
                Type eltTy = convertTypeForMemRecursive(a.getElementType());
                if (!eltTy.isSized())
                {
                    eltTy = Type.Int8Ty;
                }
                return backend.type.ArrayType.get(eltTy, 0);
            }
            case ConstantArray:
            {
                ArrayType.ConstantArrayType a = (ArrayType.ConstantArrayType)ty;
                backend.type.Type eltTy = convertTypeForMemRecursive(a.getElementType());

                // Lower arrays of undefined struct type to arrays of i8 just to have a
                // concrete type.
                if (!eltTy.isSized())
                {
                    skipLayout = true;
                    eltTy = backend.type.Type.Int8Ty;
                }
                return backend.type.ArrayType.get(eltTy, a.getSize().getZExtValue());
            }
            case FunctionNoProto:
            case FunctionProto:
            {
                jlang.type.FunctionType fnType = (jlang.type.FunctionType) ty;
                // First, check whether we can build the full function type.  If the
                // function type depends on an incomplete type (e.g. a struct or enum), we
                // cannot lower the function type.
                TagType tt = verifyFunctionTypeComplete(fnType);
                if (tt != null)
                {
                    convertTagDeclType(tt.getDecl());

                    Type resultType = OpaqueType.get();
                    functionTypes.put(ty, resultType);
                    return resultType;
                }
                if (ty instanceof FunctionProtoType)
                {
                    FunctionProtoType fpt = (FunctionProtoType)ty;
                    return getFunctionType(getFunctionInfo(fpt), fpt.isVariadic());
                }
                FunctionNoProtoType fnpt = (FunctionNoProtoType)ty;
                return getFunctionType(getFunctionInfo(fnpt), true);
            }
            case Struct:
            case Union:
            case Enum:
            {
                Decl.TagDecl td = ((jlang.type.TagType)ty).getDecl();
                backend.type.Type res = convertTagDeclType(td);

                StringBuilder typeName = new StringBuilder(td.getKindName());
                typeName.append(".");
                if (td.getIdentifier() != null)
                    typeName.append(td.getNameAsString());
                else if (t.getType() instanceof TypedefType)
                {
                    TypedefType tdf = (TypedefType)t.getType();
                    typeName.append(tdf.getDecl().getNameAsString());
                }
                else
                    typeName.append("anon");

                theModule.addTypeName(typeName.toString(), res);
                return res;
            }
        }

        return OpaqueType.get();
    }

    private backend.type.Type convertTypeRecursive(QualType ty)
    {
        ty = context.getCanonicalType(ty);

        if (typeCaches.containsKey(ty.getType()))
            return typeCaches.get(ty.getType());

        Type resultType = convertNewType(ty);
        typeCaches.put(ty.getType(), resultType);
        return resultType;
    }

    public backend.type.Type convertTypeForMemRecursive(QualType ty)
    {
        backend.type.Type resType = convertTypeRecursive(ty);
        if (resType.equals(Type.Int1Ty))
            return IntegerType.get((int) context.getTypeSize(ty));

        return resType;
    }

    /**
     * Converts the specified type to its Backend type.
     *
     * @param type
     * @return
     */
    public backend.type.Type convertType(QualType type)
    {
        backend.type.Type result = convertTypeRecursive(type);

        while (!pointersToResolve.isEmpty())
        {
            Pair<QualType, backend.type.OpaqueType> p = pointersToResolve.pop();
            backend.type.Type llvmTy = convertTypeForMemRecursive(p.first);
            p.second.refineAbstractTypeTo(llvmTy);
        }

        return result;
    }

    /**
     * Lay out a tagged decl type like struct/union type.
     * @param decl
     * @return
     */
    private backend.type.StructType convertRecordDeclType(Decl.RecordDecl decl)
    {
        jlang.type.Type key = decl.getTypeForDecl();
        StructType entry = recordDeclTypes.get(key);

        if (entry == null)
        {
            // create a place holder type.
            entry = StructType.get();
            // TODO addRecordTypeName(decl, entry, "");
        }

        if (!decl.isCompleteDefinition())
            return entry;

        // if it is unsafe to convert record type to backend type, defer it!
        if (!isSafeToConvert(decl))
        {
            deferredRecords.add(decl);
            return entry;
        }

        // Okay, this is a definition of a type.  Compile the implementation now.
        boolean insertResult = recordBeingLaidOut.add(key);
        assert insertResult:"Recursively compiling a struct?";

        CGRecordLayout layout = computeRecordLayout(decl, entry);
        cgRecordLayout.put(key, layout);

        // We're done laying out this struct.
        boolean eraseResult = recordBeingLaidOut.remove(key);
        assert eraseResult:"struct not in RecordsBeingLaidOut set?";

        if (skipLayout)
            typeCaches.clear();

        // If we're done converting the outer-most record, then convert any deferred
        // structs as well.
        if (recordBeingLaidOut.isEmpty())
            while (!deferredRecords.isEmpty())
                convertRecordDeclType(deferredRecords.removeLast());

        return entry;
    }

    /**
     * Return true if it is safe to convert the specified record
     * decl to IR and lay it out, false if doing so would cause us to get into a
     * recursive compilation mess.
     * @param rd
     * @return
     */
    public boolean isSafeToConvert(Decl.RecordDecl rd)
    {
        if (noRecordBeingLaidOut()) return true;

        return isSafeToConvert(rd, new ArrayList<>());
    }

    public boolean isSafeToConvert(Decl.RecordDecl rd,
            ArrayList<Decl.RecordDecl> alreadyChecked)
    {
        if (!alreadyChecked.add(rd)) return true;

        jlang.type.Type key = rd.getTypeForDecl();

        // If this type is already laid out, converting it is a noop.
        if (isRecordLayoutComplete(key)) return true;

        // If this type is currently being laid out, we can't recursively compile it.
        if (isRecordBeingLaidOut(key))
            return false;

        // If this type would require laying out members that are currently being laid
        // out, don't do it.
        for (int i = 0, e = rd.getDeclCounts(); i < e; i++)
        {
            FieldDecl d = rd.getDeclAt(i);
            if (!isSafeToConvert(d.getType(), alreadyChecked))
                return false;
        }

        // If there are no problems, lets do it.
        return true;
    }

    private boolean isSafeToConvert(QualType t, ArrayList<Decl.RecordDecl> alreadyChecked)
    {
        // If this is a record, check it.
        if (t.getType() instanceof RecordType)
            return isSafeToConvert(((RecordType)t.getType()).getDecl(), alreadyChecked );

        // If this is an array, check the elements, which are embedded inline.
        if (t.getType() instanceof jlang.type.ArrayType)
        {
            jlang.type.ArrayType at = (jlang.type.ArrayType)t.getType();
            return isSafeToConvert(at.getElementType(), alreadyChecked);
        }

        // Otherwise, there is no concern about transforming this.  We only care about
        // things that are contained by-value in a structure that can have another
        // structure as a member.
        return true;
    }

    private boolean isRecordLayoutComplete(jlang.type.Type rd)
    {
        return recordDeclTypes.containsKey(rd);
    }

    private boolean isRecordBeingLaidOut(jlang.type.Type ty)
    {
        return recordBeingLaidOut.contains(ty);
    }

    public boolean noRecordBeingLaidOut() { return recordBeingLaidOut.isEmpty();}

    /**
     * Verify if a given function type is complete, i.e. return type and all
     * argument type are complete.
     * @param fnType
     * @return
     */
    private boolean isFuncTypeConvertible(jlang.type.FunctionType fnType)
    {
        if (!isFuncTypeArgumentConvitable(fnType.getResultType()))
            return false;

        if (fnType instanceof FunctionProtoType)
        {
            FunctionProtoType fpt = (FunctionProtoType)fnType;
            for (int i = 0, e = fpt.getNumArgs(); i < e; i++)
            {
                if (!isFuncTypeArgumentConvitable(fpt.getArgType(i)))
                    return false;
            }
            return true;
        }

        return false;
    }

    private boolean isFuncTypeArgumentConvitable(QualType ty)
    {
        // If this isn't a tagged type, we can convert it!
        if (!(ty.getType() instanceof TagType))
        {
            return false;
        }

        TagType tt = (TagType)ty.getType();

        // If it's a tagged type used by-value, but is just a forward decl, we can't
        // convert it.  Note that getDefinition()==0 is not the same as !isCompleteDefinition.
        if (tt.getDecl() == null)
            return false;

        if (!(tt instanceof RecordType))
            return true;

        // Otherwise, we have to be careful.  If it is a struct that we're in the
        // process of expanding, then we can't convert the function type.  That's ok
        // though because we must be in a pointer context under the struct, so we can
        // just convert it to a dummy type.
        Decl.RecordDecl rd = ((RecordType)tt).getDecl();
        return isSafeToConvert(rd);
    }

    private CGRecordLayout computeRecordLayout(Decl.RecordDecl rd, StructType entry)
    {
        // TODO creates the layout of record type, 2016.11.1
        return null;
    }

    public CGFunctionInfo getFunctionInfo(FunctionProtoType fpt)
    {
        ArrayList<QualType> argTypes = new ArrayList<>();
        for (int i = 0, e = fpt.getNumArgs(); i < e; i++)
            argTypes.add(fpt.getArgType(i));

        return getFunctionInfo2(fpt.getResultType(), argTypes);
    }

    public CGFunctionInfo getFunctionInfo(FunctionNoProtoType fnpt)
    {
        return getFunctionInfo2(fnpt.getResultType(), new ArrayList<QualType>());
    }

    public CGFunctionInfo getFunctionInfo(QualType resultType,
            ArrayList<Pair<VarDecl, QualType>> callArgs)
    {
        ArrayList<QualType> argTypes = new ArrayList<>();
        for (Pair<VarDecl, QualType> itr : callArgs)
            argTypes.add(itr.second);

        return getFunctionInfo2(resultType, argTypes);
    }

    public CGFunctionInfo getFunctionInfo2(QualType resType, ArrayList<QualType> argTypes)
    {
        CGFunctionInfo fi = new CGFunctionInfo(resType, argTypes);
        // TODO: 17-6-11  Create FoldingSet.
        // Compute ABI information.
        return fi;
    }

    public CGFunctionInfo getFunctionInfo3(QualType resultType,
            ArrayList<Pair<RValue, QualType>> callArgs)
    {
        ArrayList<QualType> argTypes = new ArrayList<>();
        for (Pair<RValue, QualType> itr : callArgs)
            argTypes.add(itr.second);

        return getFunctionInfo2(resultType, argTypes);
    }

    public int getFieldNo(FieldDecl field)
    {
        assert field.isBitField():"Don't use getFieldNo on non-bitfield.";
        assert fieldInfo.containsKey(field):"Unable to find field no!";

        return fieldInfo.get(field);
    }

    public void addFieldInfo(FieldDecl fd, int no)
    {
        fieldInfo.put(fd, no);
    }

    public BitFieldInfo getBitFieldInfo(FieldDecl field)
    {
        assert bitfields.containsKey(field):"Unable to find field no!";

        return bitfields.get(field);
    }

    public void addBitFieldInfo(FieldDecl field, int fieldNo, int start, int size)
    {
        bitfields.put(field, new BitFieldInfo(fieldNo, start, size));
    }

    public ASTContext getContext()
    {
        return context;
    }

    public TargetData getTargetData()
    {
        return targetData;
    }
}
