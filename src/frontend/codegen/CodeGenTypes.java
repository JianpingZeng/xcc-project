package frontend.codegen;
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

import backend.type.*;
import backend.type.FunctionType;
import backend.type.IntegerType;
import backend.type.PointerType;
import backend.type.Type;
import frontend.sema.Decl;
import frontend.sema.Decl.FieldDecl;
import frontend.sema.Decl.VarDecl;
import frontend.type.*;
import frontend.type.ArrayType;
import frontend.type.ArrayType.VariableArrayType;
import tools.Pair;
import tools.Util;

import java.util.*;

import static frontend.type.TypeClass.*;
import static frontend.type.TypeClass.Enum;
import static frontend.type.TypeClass.Short;

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
     * This is a cache mapping frontend type to backend type.
     */
    private HashMap<frontend.type.Type, backend.type.Type> typeCaches;

    /**
     * This set keeps track of records that we're currently
     * converting to an IR type.  For example, when converting:
     * struct A { struct B { int x; } } when processing 'x', the 'A' and 'B'
     * types will be in this set.
     */
    private HashSet<frontend.type.Type> recordBeingLaidOut;

    private HashSet<CGFunctionInfo> functionBeingProcessed;

    private LinkedList<Decl.RecordDecl> deferredRecords;

    /**
     * This contains the HIR type for any converted RecordDecl.
     */
    private HashMap<frontend.type.Type, StructType> recordDeclTypes;

    /**
     * True if we didn't layout a function due to a being inside a recursive struct conversion,
     * set this variable to true;
     */
    private boolean skipLayout = false;

    private HashMap<FieldDecl, Integer> fieldInfo;

    private HashMap<FieldDecl, BitFieldInfo> bitfields;

    private HashMap<frontend.type.Type, CGRecordLayout> cgRecordLayout;
    public CodeGenTypes(HIRModuleGenerator module)
    {
        builder = module;
        typeCaches = new HashMap<>();
        recordBeingLaidOut = new HashSet<>();
        functionBeingProcessed = new HashSet<>();
        deferredRecords = new LinkedList<>();
        recordDeclTypes = new HashMap<>();
        cgRecordLayout = new HashMap<>();
        fieldInfo = new HashMap<>();
        bitfields = new HashMap<>();
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
                argTypes.add(PointerType.get(ty));
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
                    argTypes.add(PointerType.get(ty));
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
     * Converts the specified type to its Backend type.
     *
     * @param type
     * @return
     */
    public backend.type.Type convertType(QualType type)
    {
        final frontend.type.Type ty = type.getType();
        // RecordTypes are cached and processed specially.
        if (ty instanceof RecordType)
        {
            RecordType rt = (RecordType) ty;
            return convertRecordDeclType(rt.getDecl());
        }

        // See if type is cached.
        backend.type.Type found = typeCaches.get(ty);
        if (found != null)
            return found;

        // If we don't caches for frontend type, compute it.
        backend.type.Type resultType = null;
        switch (ty.getTypeClass())
        {
            case Struct:
            case Union:
                Util.shouldNotReachHere("Record type have be handled above!");
                break;
            case Bool:
            case Char:
            case Short:
            case Int:
            case LongInteger:
            case UnsignedChar:
            case UnsignedShort:
            case UnsignedInt:
            case UnsignedLong:
                resultType = IntegerType.get((int) ty.getTypeSize());
                break;
            case Real:
            {
                if (((RealType) ty).isSinglePoint())
                    resultType = backend.type.Type.FloatTy;
                else
                    resultType = backend.type.Type.DoubleTy;
                break;
            }

            case Complex:
            {
                // TODO
                break;
            }
            case Pointer:
            {
                frontend.type.PointerType pt = ty.getPointerType();
                QualType qualType = pt.getPointeeType();
                backend.type.Type pointeeType = convertTypeForMem(qualType);
                if (pointeeType.isVoidType())
                    pointeeType = backend.type.Type.Int8Ty;
                // TODO introduce address space for specified TargetData.
                resultType = backend.type.PointerType.get(pointeeType);
                break;
            }
            case VariableArray:
            {
                VariableArrayType a = (VariableArrayType) ty;
                assert a.getIndexTypeCVRQualifiers() == 0 : "FIXME: we only handle trivial array!";

                // VLAs resolve to the innermost element type; this matches
                // the return of alloca, and there isn't any obviously better choice.
                resultType = convertTypeForMem(a.getElemType());
                break;
            }
            case IncompleteArray:
            {
                ArrayType.IncompleteArrayType a = (ArrayType.IncompleteArrayType) ty;
                assert a.getIndexTypeCVRQualifiers()
                        == 0 : "FIXME: we only handle trivial array!";

                // int X[] -> [0 x int], unless the element type is not sized.  If it is
                // unsized (e.g. an incomplete struct) just use [0 x i8].
                resultType = convertTypeForMem(a.getElemType());
                if (!resultType.isSized())
                {
                    skipLayout = true;
                    resultType = backend.type.Type.Int8Ty;
                }
                resultType = backend.type.ArrayType.get(resultType, 0);
                break;
            }
            case ConstantArray:
            {
                ArrayType.ConstantArrayType a = (ArrayType.ConstantArrayType) ty;
                backend.type.Type eltTy = convertTypeForMem(a.getElemType());

                // Lower arrays of undefined struct type to arrays of i8 just to have a
                // concrete type.
                if (!eltTy.isSized())
                {
                    skipLayout = true;
                    eltTy = backend.type.Type.Int8Ty;
                }
                resultType = backend.type.ArrayType.get(eltTy, a.getSize().getZExtValue());
                break;
            }
            case Function:
            {
                frontend.type.FunctionType fnType = (frontend.type.FunctionType) ty;
                // First, check whether we can build the full function type.  If the
                // function type depends on an incomplete type (e.g. a struct or enum), we
                // cannot lower the function type.
                if (!isFuncTypeConvertible(fnType))
                {
                    // This function's type depends on an incomplete tag type.
                    // Return a placeholder type.
                    resultType = backend.type.StructType.get();
                    skipLayout = true;
                    break;
                }

                // While we're converting the argument types for a function, we don't want
                // to recursively convert any pointed-to structs.  Converting directly-used
                // structs is ok though.
                if (!recordBeingLaidOut.add(ty))
                {
                    resultType = StructType.get();
                    skipLayout = true;
                    break;
                }
                CGFunctionInfo fi = getFunctionInfo2(new QualType(fnType), null);
                boolean isVaridic = fnType.isVarArgs();

                // The function type can be built; call the appropriate routines to
                // build it.
                if (functionBeingProcessed.contains(fi))
                {
                    resultType = StructType.get();
                    skipLayout = true;
                }
                else
                {
                    // Otherwise, happy to go.
                    resultType = getFunctionType(fi, isVaridic);
                }

                recordBeingLaidOut.remove(ty);

                if (skipLayout)
                    typeCaches.clear();

                if (recordBeingLaidOut.isEmpty())
                    while(!deferredRecords.isEmpty())
                        convertRecordDeclType(deferredRecords.removeLast());
                break;
            }

            case Enum:
            {
                Decl.EnumDecl ed = ((frontend.type.EnumType)ty).getDecl();
                if (ed.isCompleteDefinition())
                {
                    return convertType(ed.getIntegerType());
                }

                // Return a placeholder 'i32' type.  This can be changed later when the
                // type is defined (see updateCompletedType), but is likely to be the
                // "right" answer.
                resultType = backend.type.Type.Int32Ty;
                break;
            }
        }
        assert resultType!=null:"Don't convert type!";
        typeCaches.put(ty, resultType);
        return resultType;
    }

    /**
     * Lay out a tagged decl type like struct/union type.
     * @param decl
     * @return
     */
    private backend.type.StructType convertRecordDeclType(Decl.RecordDecl decl)
    {
        frontend.type.Type key = decl.getTypeForDecl();
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

        frontend.type.Type key = rd.getTypeForDecl();

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
            if (!isSafeToConvert(d.getDeclType(), alreadyChecked))
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
        if (t.getType() instanceof frontend.type.ArrayType)
        {
            frontend.type.ArrayType at = (frontend.type.ArrayType)t.getType();
            return isSafeToConvert(at.getElemType(), alreadyChecked);
        }

        // Otherwise, there is no concern about transforming this.  We only care about
        // things that are contained by-value in a structure that can have another
        // structure as a member.
        return true;
    }

    private boolean isRecordLayoutComplete(frontend.type.Type rd)
    {
        return recordDeclTypes.containsKey(rd);
    }

    private boolean isRecordBeingLaidOut(frontend.type.Type ty)
    {
        return recordBeingLaidOut.contains(ty);
    }

    public boolean noRecordBeingLaidOut() { return recordBeingLaidOut.isEmpty();}

    /**
     * Convert type T into a backend.type.Type.  This differs from
     * ConvertType in that it is used to convert to the memory representation for
     * a type.  For example, the scalar representation for _Bool is i1, but the
     * memory representation is usually i8 or i32, depending on the TargetData.
     * @param qualType
     * @return
     */
    public backend.type.Type convertTypeForMem(QualType qualType)
    {
        backend.type.Type r = convertType(qualType);

        // If this is a non-bool type, don't map it.
        if (!r.isIntegerType())
            return r;

        // Otherwise, return an integer of the TargetData-specified size.
        return IntegerType.get((int)qualType.getTypeSize());
    }

    /**
     * Verify if a given function type is complete, i.e. return type and all
     * argument type are complete.
     * @param fnType
     * @return
     */
    private boolean isFuncTypeConvertible(frontend.type.FunctionType fnType)
    {
        if (!isFuncTypeArgumentConvitable(fnType.getReturnType()))
            return false;

        for (frontend.type.QualType t : fnType.getParamTypes())
            if (!isFuncTypeArgumentConvitable(t))
                return false;

        return true;
    }

    private boolean isFuncTypeArgumentConvitable(QualType ty)
    {
        // If this isn't a tagged type, we can convert it!
        TagType tt = ty.<TagType>getAs();
        if (tt == null) return true;

        // If it's a tagged type used by-value, but is just a forward decl, we can't
        // convert it.  Note that getDefinition()==0 is not the same as !isDefinition.
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
    { fieldInfo.put(fd, no);}

    public BitFieldInfo getBitFieldInfo(FieldDecl field)
    {
        assert bitfields.containsKey(field):"Unable to find field no!";

        return bitfields.get(field);
    }

    public void addBitFieldInfo(FieldDecl field, int fieldNo, int start, int size)
    {
        bitfields.put(field, new BitFieldInfo(fieldNo, start, size));
    }
}
