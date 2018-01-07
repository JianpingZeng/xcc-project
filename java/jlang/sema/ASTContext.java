package jlang.sema;
/*
 * Extremely C language CompilerInstance
 * Copyright (c) 2015-2018, Xlous
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

import jlang.ast.Attr;
import jlang.ast.Tree.Expr;
import jlang.basic.Context;
import jlang.basic.SourceManager;
import jlang.basic.TargetInfo;
import jlang.basic.TargetInfo.IntType;
import jlang.clex.IdentifierTable;
import jlang.diag.FullSourceLoc;
import jlang.sema.Decl.*;
import jlang.support.LangOptions;
import jlang.support.PrintingPolicy;
import jlang.support.SourceLocation;
import jlang.support.SourceRange;
import jlang.type.*;
import jlang.type.ArrayType.*;
import tools.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import static jlang.sema.ASTContext.FloatingRank.*;
import static jlang.sema.ASTContext.GetBuiltinTypeError.*;
import static jlang.type.ArrayType.ArraySizeModifier.Normal;
import static jlang.type.TypeClass.Bool;
import static jlang.type.TypeClass.Char_S;
import static jlang.type.TypeClass.Char_U;
import static jlang.type.TypeClass.ConstantArray;
import static jlang.type.TypeClass.ConstantArrayWithExpr;
import static jlang.type.TypeClass.ConstantArrayWithoutExpr;
import static jlang.type.TypeClass.Double;
import static jlang.type.TypeClass.Enum;
import static jlang.type.TypeClass.Float;
import static jlang.type.TypeClass.FunctionProto;
import static jlang.type.TypeClass.IncompleteArray;
import static jlang.type.TypeClass.Int;
import static jlang.type.TypeClass.Int128;
import static jlang.type.TypeClass.Long;
import static jlang.type.TypeClass.LongDouble;
import static jlang.type.TypeClass.LongLong;
import static jlang.type.TypeClass.Pointer;
import static jlang.type.TypeClass.SChar;
import static jlang.type.TypeClass.Short;
import static jlang.type.TypeClass.Struct;
import static jlang.type.TypeClass.TypeDef;
import static jlang.type.TypeClass.UChar;
import static jlang.type.TypeClass.UInt;
import static jlang.type.TypeClass.UInt128;
import static jlang.type.TypeClass.ULong;
import static jlang.type.TypeClass.ULongLong;
import static jlang.type.TypeClass.UShort;
import static jlang.type.TypeClass.Union;
import static jlang.type.TypeClass.VariableArray;
import static jlang.type.TypeClass.Void;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class ASTContext
{
    private ArrayList<Type> types = new ArrayList<>();
    private TreeMap<Integer, ComplexType> complexTypes = new TreeMap<>();
    private TreeMap<Integer, PointerType> pointerTypes = new TreeMap<>();
    private TreeMap<Integer, ConstantArrayType> constantArrayTypes = new TreeMap<>();
    private TreeMap<Integer, IncompleteArrayType> incompleteArrayTypes = new TreeMap<>();
    private ArrayList<VariableArrayType> variableArrayTypes = new ArrayList<>();
    private TreeMap<Integer, FunctionNoProtoType> functionNoProtoTypes = new TreeMap<>();
    private TreeMap<Integer, FunctionProtoType> functionProtoTypes = new TreeMap<>();

	/**
	 * A cache diagMapping from RecordDecls to ASTRecordLayouts.
	 * This is lazily created.  This is intentionally not serialized.
	 */
	private TreeMap<Integer, IncompleteArrayType> IncompleteArrayTypes = new TreeMap<>();

    private HashMap<RecordDecl, ASTRecordLayout> astRecordLayoutMap = new HashMap<>();

    private SourceManager sourceMgr;
    private TranslationUnitDecl tuDel;

    private LangOptions langOptions;
    public TargetInfo target;
    public IdentifierTable identifierTable;

    // Built-in types.
    public final QualType VoidTy;
	public final QualType BoolTy;
	public final QualType CharTy;
	public final QualType SignedCharTy;
	public final QualType UnsignedCharTy;
	public final QualType ShortTy;
	public final QualType UnsignedShortTy;
	public final QualType IntTy;
	public final QualType UnsignedIntTy;
	public final QualType LongTy;
	public final QualType UnsignedLongTy;
	public final QualType LongLongTy;
	public final QualType UnsignedLongLongTy;
	public final QualType Int128Ty;
	public final QualType UnsignedInt128Ty;
	public final QualType FloatTy;
	public final QualType DoubleTy;
    public final QualType LongDoubleTy;
    public final QualType FloatComplexTy;
    public final QualType DoubleComplexTy;
    public final QualType LongDoubleComplexTy;

    public final QualType VoidPtrTy;

    /**
     * Source ranges for all of the comments in the source file,
     * sorted in order of appearance in the translation unit.
     */
    public ArrayList<SourceRange> comments;

    public PrintingPolicy printingPolicy;

    public Context builtinInfo;

	/**
	 * Built-in va list type.
	 * This is initially null and set by Sema::LazilyCreateBuiltin when
	 * a builtin that takes a valist is encountered.
	 */
	public QualType builtinVaListType;

	/**
	 * The type for C FILE.
	 */
	private TypeDecl fileDecl;

	/**
	 * The type for the c jmp_buf type.
	 */
	private TypeDecl jmp_bufDecl;
	/**
	 * The type for the C sigjmp_buf type.
	 */
	private TypeDecl sigjmp_buf_Decl;

	private HashMap<Decl, ArrayList<Attr>> declAttrs;

	public ASTContext(LangOptions opts, SourceManager sourceMgr,
			TargetInfo targetInfo, IdentifierTable identifierTable,
		    Context builtinInfo)
	{
		langOptions = opts;
		this.sourceMgr = sourceMgr;
		target = targetInfo;
		this.identifierTable = identifierTable;
		this.builtinInfo = builtinInfo;
        tuDel = TranslationUnitDecl.create(this);

        VoidTy = initBuiltinType(Void);
        BoolTy = initBuiltinType(Bool);
        CharTy = initBuiltinType(SChar);
        SignedCharTy = initBuiltinType(SChar);
        ShortTy = initBuiltinType(Short);
        IntTy = initBuiltinType(Int);
        LongTy = initBuiltinType(Long);
        LongLongTy = initBuiltinType(LongLong);

        UnsignedCharTy = initBuiltinType(Char_U);
        UnsignedShortTy = initBuiltinType(UShort);
        UnsignedIntTy = initBuiltinType(UInt);
        UnsignedLongTy = initBuiltinType(ULong);
        UnsignedLongLongTy = initBuiltinType(ULongLong);
        Int128Ty = initBuiltinType(Int128);
        UnsignedInt128Ty = initBuiltinType(UInt128);

        FloatTy = initBuiltinType(Float);
        DoubleTy = initBuiltinType(Double);
        LongDoubleTy = initBuiltinType(LongDouble);
        FloatComplexTy = getComplexType(FloatTy);
        DoubleComplexTy = getComplexType(DoubleTy);
        LongDoubleComplexTy = getComplexType(LongDoubleTy);

        VoidPtrTy = getPointerType(VoidTy);
        comments = new ArrayList<>();
        printingPolicy = new PrintingPolicy(langOptions);

        builtinVaListType = new QualType();

		declAttrs = new HashMap<>();
	}

    public SourceManager getSourceManager()
    {
        return sourceMgr;
    }

    public void setSourceManager(SourceManager sourceMgr)
    {
        this.sourceMgr = sourceMgr;
    }

    public APSInt makeIntValue(long value, QualType type)
    {
        APSInt res = new APSInt(getIntWidth(type), !type.isSignedIntegerType());
        res.assign(value);
        return res;
    }

    private QualType initBuiltinType(int tc)
    {
        QualType res = new QualType(new BuiltinType(tc));
        types.add(res.getType());
        return res;
    }

    public QualType getComplexType(QualType ty)
    {
	    FoldingSetNodeID id = new FoldingSetNodeID();
	    ty.profile(id);
	    int hashcode = id.computeHash();
        if (complexTypes.containsKey(hashcode))
            return new QualType(complexTypes.get(hashcode));

        // If the pointee type isn't canonical, this won't be a canonical type either,
        // so fill in the canonical type field.
        QualType canonical = new QualType();
        if (!ty.isCanonical())
        {
            canonical = getPointerType(getCanonicalType(ty));
        }
        ComplexType newTy = new ComplexType(ty, canonical);
        types.add(newTy);
        complexTypes.put(hashcode, newTy);
        return new QualType(newTy);
    }

	public QualType getPointerType(QualType ty)
	{
		FoldingSetNodeID id = new FoldingSetNodeID();
		ty.profile(id);
		int hashcode = id.computeHash();

		if (pointerTypes.containsKey(hashcode))
		    return new QualType(pointerTypes.get(hashcode));

        // If the pointee type isn't canonical, this won't be a canonical type either,
        // so fill in the canonical type field.
        QualType canonical = new QualType();
        if (!ty.isCanonical())
        {
            canonical = getPointerType(getCanonicalType(ty));
        }
        PointerType newPtr = new PointerType(ty, canonical);
        types.add(newPtr);
        pointerTypes.put(hashcode, newPtr);
        return new QualType(newPtr);
	}

	public QualType getConstantArrayType(
	        QualType eltTy,
            APInt arraySizeIn,
            ArraySizeModifier asm,
            int eltTypeQuals)
    {
        assert eltTy.isConstantSizeType() && !eltTy.isIncompleteType():
                "Constant array of VLAs is illegal";

        // Convert the array size into a canonical width matching the pointer size for
        // the target.
        APInt arrSize = new APInt(arraySizeIn);
        arrSize.zextOrTrunc(target.getPointerWidth(eltTy.getAddressSpace()));

        FoldingSetNodeID id = new FoldingSetNodeID();
        ConstantArrayType.profile(id, eltTy, arrSize, asm, eltTypeQuals);

        int hash = id.computeHash();
        if (constantArrayTypes.containsKey(hash))
            return new QualType(constantArrayTypes.get(hash));

        QualType canonical = new QualType();
        if (!eltTy.isCanonical())
        {
            canonical = getConstantArrayType(getCanonicalType(eltTy), arrSize,
                    asm, eltTypeQuals);
        }

        ConstantArrayType cat = new ConstantArrayType(eltTy, canonical,
                arrSize, asm, eltTypeQuals);
        constantArrayTypes.put(hash, cat);
        types.add(cat);
        return new QualType(cat);
    }

	/**
	 * Return a reference to the type for an array of the specified element type.
	 * @param eltTy
	 * @param arraySizeIn
	 * @param arraySizeExpr
	 * @param asm
	 * @param eltTypeQuals
	 * @param brackets
	 * @return
	 */
	public QualType getConstantArrayWithExprType(
			QualType eltTy,
			APInt arraySizeIn,
			Expr arraySizeExpr,
			ArraySizeModifier asm,
			int eltTypeQuals,
			SourceRange brackets)
	{
        APInt arrSize = new APInt(arraySizeIn);
        // Truncate or extending the array size to fit target machine.
        arrSize.zextOrTrunc(target.getPointerWidth(eltTy.getAddressSpace()));
        QualType can = getConstantArrayType(getCanonicalType(eltTy), arrSize,
                asm, eltTypeQuals);

		ConstantArrayWithExprType arr = new ConstantArrayWithExprType(eltTy,
                can, arrSize, arraySizeExpr, asm, eltTypeQuals,brackets);

		types.add(arr);
		return new QualType(arr);
	}

	/**
	 * Return a reference to the type for an array of the specified element type.
	 * @param eltTy
	 * @param arraySizeIn
	 * @param asm
	 * @param eltTypeQuals
	 * @return
	 */
	public QualType getConstantArrayWithoutExprType(
			QualType eltTy,
			APInt arraySizeIn,
			ArraySizeModifier asm,
			int eltTypeQuals)
	{
		APInt arrSize = new APInt(arraySizeIn);

        // Truncate or extending the array size to fit target machine.
		arrSize.zextOrTrunc(target.getPointerWidth(eltTy.getAddressSpace()));

		QualType can = getConstantArrayType(getCanonicalType(eltTy),
                arraySizeIn, asm, eltTypeQuals);

		ConstantArrayWithoutExprType arr = new ConstantArrayWithoutExprType(
				eltTy, can, arraySizeIn, asm, eltTypeQuals);
		types.add(arr);
		return new QualType(arr);
	}

    /**
     * Returns a reference to the type for a variable array of the specified
     * element type.
     * @param elemTy
     * @param numElts
     * @return
     */
    public QualType getVariableArrayType(
            QualType elemTy,
            Expr numElts,
            ArraySizeModifier asm,
            int eltTypeQuals,
            SourceRange brackets)
    {
        VariableArrayType vat = new VariableArrayType(elemTy, new QualType(),
                numElts, asm, eltTypeQuals, brackets);
        variableArrayTypes.add(vat);
        types.add(vat);
        return new QualType(vat);
    }

	public QualType getIncompleteArrayType(
			QualType eltTy,
			ArraySizeModifier sm,
			int eltTypeQuals)
	{
        int id = eltTy.hashCode();
        if (incompleteArrayTypes.containsKey(id))
            return new QualType(incompleteArrayTypes.get(id));

        // If the pointee type isn't canonical, this won't be a canonical type either,
        // so fill in the canonical type field.
        QualType canonical = new QualType();
        if (!eltTy.isCanonical())
        {
            canonical = getPointerType(getCanonicalType(eltTy));
        }
        IncompleteArrayType newIP = new IncompleteArrayType(eltTy, canonical,
                sm, eltTypeQuals);

        types.add(newIP);
        incompleteArrayTypes.put(id, newIP);
        return new QualType(newIP);
	}

	/**
	 * Return a normal function type with a typed argument list.
	 * @param isVariadic Indicates whether the argument list includes '...'.
	 * @return
	 */
	public QualType getFunctionType(
			QualType resultTy,
			ArrayList<QualType> argTys,
			boolean isVariadic,
			int typeQuals,
            boolean noReturn)
	{
        FoldingSetNodeID id = new FoldingSetNodeID();
        FunctionProtoType.profile(id, resultTy, argTys, isVariadic, noReturn);
        int hash = id.computeHash();
        if (functionProtoTypes.containsKey(hash))
            return new QualType(functionProtoTypes.get(hash));

        boolean isCanonical = resultTy.isCanonical();
        for (int i = 0,e = argTys.size(); i != e && isCanonical; i++)
            if (!argTys.get(i).isCanonical())
                isCanonical = false;

        QualType can = new QualType();
        if (!isCanonical)
        {
            ArrayList<QualType> canonicalArgs = new ArrayList<>();
            for (QualType arg : argTys)
                canonicalArgs.add(getCanonicalType(arg));

            can = getFunctionType(getCanonicalType(resultTy), canonicalArgs,
                    isVariadic, typeQuals, noReturn);
        }

        FunctionProtoType ftp = new FunctionProtoType(resultTy, argTys, isVariadic,
                can, noReturn);
        types.add(ftp);
        functionProtoTypes.put(hash, ftp);
		return new QualType(ftp);
	}

    /**
     * The variant of {@linkplain #getFunctionType(QualType, ArrayList, boolean, int, boolean)}
     * with noReturn default to {@code false}.
     * @param resultTy
     * @param argTys
     * @param isVariadic
     * @param typeQuals
     * @return
     */
    public QualType getFunctionType(
            QualType resultTy,
            ArrayList<QualType> argTys,
            boolean isVariadic,
            int typeQuals)
    {
        return getFunctionType(resultTy, argTys, isVariadic, typeQuals, false);
    }

	/**
	 * Return a K&R style C function type like 'int()'.
	 * @param resultTy
	 * @return
	 */
	public QualType getFunctionNoProtoType(QualType resultTy, boolean noReturn)
	{
        FoldingSetNodeID id = new FoldingSetNodeID();
        FunctionNoProtoType.profile(id, resultTy, noReturn);
        int hash = id.computeHash();
        if (functionNoProtoTypes.containsKey(hash))
            return new QualType(functionNoProtoTypes.get(hash));

        QualType can = new QualType();
        if (!resultTy.isCanonical())
        {
            can = getFunctionNoProtoType(getCanonicalType(resultTy), noReturn);
        }

        FunctionNoProtoType ftp = new FunctionNoProtoType(resultTy, can, noReturn);
        types.add(ftp);
        functionNoProtoTypes.put(hash, ftp);
        return new QualType(ftp);
	}

    /**
     * The variant of {@linkplain #getFunctionNoProtoType(QualType, boolean)}
     * with noReturn default to {@code false}.
     * @param resultTy
     * @return
     */
    public QualType getFunctionNoProtoType(QualType resultTy)
    {
        return getFunctionNoProtoType(resultTy, false);
    }

	/**
	 * Return the properly qualified result of decaying the
     * specified array type to a pointer.  This operation is non-trivial when
     * handling typedefs etc.  The canonical type of "T" must be an array type,
     * this returns a pointer to a properly qualified element of the array.
	 * @param ty
	 * @return
	 */
	public QualType getArrayDecayedType(QualType ty)
	{
        // Get the element type with 'getAsArrayType' so that we don't lose any
        // typedefs in the element type of the array.  This also handles propagation
        // of type qualifiers from the array type into the element type if present
        // (C99 6.7.3p8).
		ArrayType arrayType = getAsArrayType(ty);
		assert arrayType != null : "Not an array jlang.type!";

		QualType ptrTy = getPointerType(arrayType.getElementType());

		// int x[restrict 4]-> int *restrict;
        return ptrTy.getQualifiedType(arrayType.getIndexTypeQuals());
	}

	/**
	 * Return the unique reference to the type for the specified
	 * TagDecl (struct/union/enum) decl.
	 * @param decl
	 * @return
	 */
	public QualType getTagDeclType(TagDecl decl)
	{
		assert decl != null;
		return getTypeDeclType(decl, null);
	}

	/**
	 * Return the unique reference to the type for the
	 * specified typename decl.
	 * @param decl
	 * @return
	 */
	public QualType getTypedefType(TypeDefDecl decl)
	{
		if (decl.getTypeForDecl() != null)
			return new QualType(decl.getTypeForDecl());

		QualType cannonical = decl.getUnderlyingType();
		decl.setTypeForDecl(new TypedefType(TypeClass.TypeDef, decl, cannonical));
		types.add(decl.getTypeForDecl());
		return new QualType(decl.getTypeForDecl());
	}

	/**
	 * Return the unique reference to the type for the
	 * specified type declaration.
	 * @param decl
	 * @param prevDecl
	 * @return
	 */
	public QualType getTypeDeclType(TypeDecl decl, TypeDecl prevDecl)
	{
		assert decl != null:"Passed null for decl param";
		if (decl.getTypeForDecl() != null)
			return new QualType(decl.getTypeForDecl());

		if (decl instanceof TypeDefDecl)
		{
			TypeDefDecl typedef = (TypeDefDecl)decl;
			return getTypedefType(typedef);
		}
		if (decl instanceof RecordDecl)
		{
			RecordDecl record = (RecordDecl)decl;
			if (prevDecl != null)
				decl.setTypeForDecl(prevDecl.getTypeForDecl());
			else
				decl.setTypeForDecl(new RecordType(record));
		}
		else if (decl instanceof EnumDecl)
		{
			EnumDecl enumDecl = (EnumDecl)decl;
			if (prevDecl != null)
				decl.setTypeForDecl(prevDecl.getTypeForDecl());
			else
				decl.setTypeForDecl(new EnumType(enumDecl));
		}
		else
		{
			assert false:"TypeDecl without a type?";
		}
		if (prevDecl == null)
			types.add(decl.getTypeForDecl());
		return new QualType(decl.getTypeForDecl());
	}

	public QualType getQualifiedType(QualType t, QualType.Qualifier qs)
	{
	    return new QualType(t.getType(), qs.mask);
	}

	public ConstantArrayType getAsConstantArrayType(QualType type)
	{
		ArrayType res = getAsArrayType(type);
		return (res instanceof ConstantArrayType)
				? (ConstantArrayType)res : null;
	}

	public IncompleteArrayType getAsInompleteArrayType(QualType type)
	{
		ArrayType res = getAsArrayType(type);
		return (res instanceof IncompleteArrayType)
				? (IncompleteArrayType)res : null;
	}

	public VariableArrayType getAsVariableArrayType(QualType type)
	{
		ArrayType res = getAsArrayType(type);
		return (res instanceof VariableArrayType)
				? (VariableArrayType)res : null;
	}

	public ArrayType getAsArrayType(QualType type)
	{
		// Handle the non-qualified case efficiently.
		if (!type.hasQualifiers())
		{
			if (type.getType() instanceof ArrayType)
				return (ArrayType)type.getType();
		}

		if (!(type.getType() instanceof ArrayType))
			return null;
		ArrayType aty = (ArrayType)type.getType();
		// Otherwise, we have an array and we have qualifiers on it.
		// Push qualifiers into the array element type and return a new array jlang.type.
		QualType newElemTy = getQualifiedType(aty.getElementType(), type.getQualifiers());

		if (aty instanceof ConstantArrayType)
		{
			ConstantArrayType cat = (ConstantArrayType)aty;
			return (ArrayType) getConstantArrayType(newElemTy, cat.getSize(),
                    cat.getSizeModifier(), cat.getIndexTypeQuals()).getType();
		}

		if (aty instanceof IncompleteArrayType)
		{
			IncompleteArrayType icat = (IncompleteArrayType)aty;
			return (ArrayType) getIncompleteArrayType(newElemTy, 
                    icat.getSizeModifier(), icat.getIndexTypeQuals()).getType();
		}

		VariableArrayType vat = (VariableArrayType)aty;
		return (ArrayType) getVariableArrayType(newElemTy, vat.getSizeExpr(), 
                vat.getSizeModifier(), vat.getIndexTypeQuals(), vat.getBrackets())
                .getType();
	}

	public LangOptions getLangOptions()
	{
		return langOptions;
	}


	/**
	 * Indicates if this type can be wrapped with other jlang.type.
	 *
	 * @return
	 */
	public boolean isCompatible(QualType first, QualType second)
	{
		return !mergeType(first, second, false).isNull();
	}

	public QualType mergeType(QualType lhs, QualType rhs)
	{
		return mergeType(lhs, rhs, false);
	}

	public QualType mergeType(QualType lhs, QualType rhs, boolean unqualified)
	{
		if (unqualified)
		{
			lhs = lhs.getUnQualifiedType();
			rhs = rhs.getUnQualifiedType();
		}

		// If two types are identical. they are compatible.
		if (lhs.equals(rhs))
			return lhs;

		// If the qualifiers are different, the types aren't compatible...
		QualType.Qualifier lQuals = lhs.qualsFlag;
		QualType.Qualifier rQuals = rhs.qualsFlag;

		// If the jlang.type qualifiers are different, we get a mismatch.
		if (lQuals.equals(rQuals))
		{
			return new QualType();
		}

		// Get the point, qualifiers are equal.
		int lhsClass = lhs.getTypeClass();
		int rhsClass = rhs.getTypeClass();


		if (lhsClass == VariableArray)
			lhsClass = TypeClass.ConstantArray;
		if (rhsClass == VariableArray)
			rhsClass = ConstantArray;

		// If the jlang.type classes don't match.
		if (lhsClass != rhsClass)
		{
			// C99 6.7.2.2p4: Each enumerated jlang.type shall be compatible with char,
			// a signed integer jlang.type, or an unsigned integer jlang.type.
			// Compatibility is based on the underlying jlang.type, not the promotion
			// jlang.type.
			EnumType ety = lhs.getAsEnumType();
			if (ety != null)
			{
				if (ety.getDecl().getPromotionType().equals(rhs.getUnQualifiedType()))
					return rhs;
			}
			ety = rhs.getAsEnumType();
			if (ety != null)
			{
				if (ety.getDecl().getPromotionType().equals(lhs.getUnQualifiedType()))
					return rhs;
			}

			return new QualType();
		}

		switch (lhsClass)
		{
			case VariableArray:
			case FunctionProto:
				Util.shouldNotReachHere("types are eliminated abo e");

			case Pointer:
			{
				// Merge two pointer types, while trying to preserve typedef info.
				QualType lhsPointee = getPointerType(lhs).getPointeeType();
				QualType rhsPointee = getPointerType(rhs).getPointeeType();

				if (unqualified)
				{
					lhsPointee = lhsPointee.getUnQualifiedType();
					rhsPointee = rhsPointee.getUnQualifiedType();
				}
				QualType resultType = mergeType(lhsPointee, rhsPointee, unqualified);
				if (resultType.isNull()) return new QualType();
				if (lhsPointee.equals(resultType))
					return lhs;
				if (rhsPointee.equals(resultType))
					return rhs;
				return getPointerType(resultType);
			}
			case ConstantArray:
			{
				final ConstantArrayType lcat = getAsConstantArrayType(lhs);
				final ConstantArrayType rcat = getAsConstantArrayType(rhs);
				if (lcat != null && rcat != null && rcat.getSize().ne(lcat.getSize()))
					return new QualType();

				QualType lhsElem = getAsArrayType(lhs).getElementType();
				QualType rhsElem = getAsArrayType(rhs).getElementType();

				if (unqualified)
				{
					rhsElem = rhsElem.getUnQualifiedType();
					lhsElem = lhsElem.getUnQualifiedType();
				}

				QualType resultType  = mergeType(lhsElem, rhsElem, unqualified);
				if (resultType.isNull()) return new QualType();

				if (lcat != null && lhsElem.equals(resultType))
					return lhs;

				if (rcat!= null && rhsElem.equals(resultType))
					return rhs;

				if (lcat != null)
					return getConstantArrayType(resultType, lcat.getSize(),
                            lcat.getSizeModifier(), lcat.getIndexTypeQuals());

				if (rcat != null)
					return getConstantArrayType(resultType, rcat.getSize(),
                            rcat.getSizeModifier(), rcat.getIndexTypeQuals());

				VariableArrayType lvat = getAsVariableArrayType(lhs);
				VariableArrayType rvat = getAsVariableArrayType(rhs);
				if (lvat != null && lhsElem.equals(resultType))
					return lhs;

				if (rvat != null && rhsElem.equals(resultType))
					return rhs;

				if (lvat != null)
				{
					// FIXME: This isn't correct! But tricky to implement because
					// the array's getNumOfSubLoop has to be the getNumOfSubLoop of LHS, but the jlang.type
					// has to be different.
					return lhs;
				}
				if (rcat != null)
				{
					// FIXME: This isn't correct! But tricky to implement because
					// the array's getNumOfSubLoop has to be the getNumOfSubLoop of LHS, but the jlang.type
					// has to be different.
					return rhs;
				}

				if (lhsElem.equals(resultType)) return lhs;
				if (rhsElem.equals(resultType)) return rhs;

				return getIncompleteArrayType(resultType, Normal, 0);
			}

			case TypeClass.Struct:
			case TypeClass.Union:
			case TypeClass.Enum:

			case Char_U:
			case UShort:
			case UInt:
			case ULong:
			case SChar:
			case Short:
			case Long:
			case Float:
				return new QualType();
		}
		return new QualType();
	}


	public TranslationUnitDecl getTranslateUnitDecl()
	{
		return tuDel;
	}

	public int getTypeAlign(QualType type)
	{
		return getTypeInfo(type).second;
	}

	public long getTypeSize(QualType type)
	{
		return getTypeInfo(type).first;
	}

	public long getTypeSize(Type ty)
	{
		return getTypeInfo(ty).first;
	}

	public int getTypeAlign(Type ty)
	{
		return getTypeInfo(ty).second;
	}

	/**
	 * Return the unique type for "ptrdiff_t" (ref?)
	 * defined in <stddef.h>. Pointer - pointer requires this (C99 6.5.6p9).
	 * @return
	 */
	public QualType getPointerDiffType()
	{
		return getFromTargetType(target.getPtrDiffType(0));
	}

	public QualType getFromTargetType(IntType type)
	{
		switch (type)
		{
			case NoInt: return new QualType();
			case SignedShort: return ShortTy;
			case UnsignedShort: return UnsignedShortTy;
			case SignedInt: return IntTy;
			case UnsignedInt: return UnsignedIntTy;
			case SignedLong: return LongTy;
			case UnsignedLong: return UnsignedLongTy;
			case SignedLongLong: return LongLongTy;
			case UnsignedLongLong: return UnsignedLongLongTy;
		}

		assert false : "Unhandled IntType value";
		return new QualType();
	}


	public boolean isPromotableIntegerType(QualType type)
	{
		if (type.getType() instanceof BuiltinType)
		{
			switch (type.getTypeClass())
			{
				case TypeClass.Bool:
                case TypeClass.Char_S:
                case TypeClass.Char_U:
				case TypeClass.SChar:
                case TypeClass.UChar:
				case TypeClass.Short:
				case TypeClass.UShort:
					return true;
				default:
					return false;
			}
		}

		// Enumerated types are promotable to their compatible integer types
		// (C99 6.3.1.1)
		if (type.isEnumeralType())
		{
			EnumType et = type.getAsEnumType();
			QualType qt = et.getDecl().getPromotionType();
			if (qt.isNull())
				return false;

			BuiltinType bt = qt.getAsBuiltinType();
			return bt.getTypeClass() == Int || bt.getTypeClass() == UInt;
		}
		return false;
	}

	/**
	 * Returns the jlang.type that promotable jlang.type promote to.
	 * <br>
	 * C99 6.3.1.1p2, assuming that promotable is promotable to integer.
	 *
	 * @return
	 */
	public QualType getPromotedIntegerType(QualType type)
	{
		assert !type.isNull() : "promotable can not be null!";
		assert isPromotableIntegerType(type);

		if (type.getType() instanceof EnumType)
			return ((EnumType) type.getType()).getDecl().getPromotionType();
		if (type.isSignedIntegerType())
			return IntTy;
		long promotableSize = getTypeSize(type);
		long intSize = getTypeSize(IntTy);
		assert !type.isSignedIntegerType() && promotableSize <= intSize;

		return promotableSize != intSize ? IntTy : UnsignedIntTy;
	}

	/**
	 * Returns the reference to the jlang.type for the specified jlang.type declaration.
	 * @param decl
	 * @return
	 */
	public QualType getTypeDeclType(Decl.TypeDecl decl)
	{
		assert decl != null:"Passed null for decl param";
		if (decl.getTypeForDecl() != null)
			return new QualType(decl.getTypeForDecl());

		return getTypeDeclTypeSlow(decl);
	}

	public QualType getTypeDeclTypeSlow(Decl.TypeDecl decl)
	{
		assert decl!= null:"Passed null for decl param";
		assert decl.getTypeForDecl() ==null:"TypeForDecl present in slow case";

		if (decl instanceof TypeDefDecl)
		{
			return getTypeDefType((TypeDefDecl) decl);
		}
		if (decl instanceof RecordDecl)
		{
			RecordDecl record = (RecordDecl)decl;
			return getRecordType(record);
		}
		if (decl instanceof Decl.EnumDecl)
		{
			Decl.EnumDecl d = (Decl.EnumDecl)decl;
			return getEnumType(d);
		}
		else
		{
			Util.shouldNotReachHere("TypeDecl without a jlang.type?");
		}
		return new QualType(decl.getTypeForDecl());
	}

	public QualType getTypeDefType(TypeDefDecl decl)
	{
		if (decl.getTypeForDecl() != null)
			return new QualType(decl.getTypeForDecl());

		QualType canonical = getCanonicalType(decl.getUnderlyingType());
		TypedefType ty = new TypedefType(TypeClass.TypeDef, decl, canonical);
		decl.setTypeForDecl(ty);

		decl.setTypeForDecl(ty);
		return new QualType(ty);
	}

	public static QualType getRecordType(RecordDecl record)
	{
		if (record.getTypeForDecl() != null)
			return new QualType(record.getTypeForDecl());

		RecordType newType = new RecordType(record);
		record.setTypeForDecl(newType);
		return new QualType(newType);
	}

	public static QualType getEnumType(Decl.EnumDecl decl)
	{
		if (decl.getTypeForDecl() != null)
			return new QualType(decl.getTypeForDecl());

		EnumType newType = new EnumType(decl);
		decl.setTypeForDecl(newType);
		return new QualType(newType);
	}

	public boolean isIncompleteOrObjectType(QualType type)
	{
		return !type.isFunctionType();
	}

	public QualType getBaseElementType(QualType type)
	{
		QualType.Qualifier qs = new QualType.Qualifier();
		while (true)
		{
			if (!type.isConstantArrayType())
				break;

			type = getAsArrayType(type).getElementType();
			qs.addCVQualifier(type.qualsFlag.mask);
		}
		return getQualifiedType(type, qs);
	}

	public QualType getBaseElementType(VariableArrayType vat)
	{
		QualType eltType = vat.getElementType();
		VariableArrayType eltVat = getAsVariableArrayType(eltType);
		if (eltVat != null)
			return getBaseElementType(eltVat);

		return eltType;
	}

	/**
	 * A generic method for casting a QualType instance to jlang.type.Type.
	 * If casting failed, return null, otherwise, return the instance
	 * type as required result.
	 * @return
	 */
	public <T extends Type> T getAs(QualType type, Class<T> clazz)
	{
	    if (clazz.isAssignableFrom(type.getType().getClass()))
        {
            return clazz.cast(type.getType());
        }
        return null;
	}

	public boolean isSignedIntegerOrEnumerationType(QualType type)
	{
		if (type.isBuiltinType())
		{
			return type.getTypeClass() >= TypeClass.SChar
					&& type.getTypeClass() <= TypeClass.Long;
		}
		if (type.isEnumeralType())
		{
			EnumType et = (EnumType)type.getType();
			if (et.getDecl().isCompleteDefinition())
				return et.getDecl().getPromotionType().isSignedIntegerType();
		}
		return false;
	}

	public boolean isUnsignedIntegerOrEnumerationType(QualType type)
	{
		if (type.isBuiltinType())
		{
			return type.getTypeClass() >= TypeClass.Bool
					&& type.getTypeClass() <= TypeClass.ULong;
		}
		if (type.isEnumeralType())
		{
			EnumType et = (EnumType)type.getType();
			if (et.getDecl().isCompleteDefinition())
				return !et.getDecl().getPromotionType().isSignedIntegerType();
		}
		return false;
	}

	public int getIntWidth(QualType t)
	{
		if (t.isEnumeralType())
		{
			t = new QualType(t.getAsEnumType().getDecl().getPromotionType().getType());
		}
		if (t.isBooleanType())
			return 1;
		return (int) getTypeSize(t);
	}

	public Pair<Integer, Integer> getTypeInfoInBytes(Type t)
	{
		Pair<Long, Integer> res = getTypeInfo(t);
		return new Pair<>(toByteUnitFromBits(res.first),
				toByteUnitFromBits(res.second));
	}

	public Pair<Long, Integer> getTypeInfo(QualType t)
	{
		return getTypeInfo(t.getType());
	}

	public Pair<Long, Integer> getTypeInfo(Type t)
	{
		long width = 0;
		int align = 8;
		switch (t.getTypeClass())
		{
			case FunctionProto:
				// GCC extension: alignof(function) = 32 bits
				width = 0;
				align = 32;

			case IncompleteArray:
			case VariableArray:
				width = 0;
				align = getTypeAlign(((ArrayType)t).getElementType());
				break;
			case ConstantArrayWithExpr:
			case ConstantArrayWithoutExpr:
			case ConstantArray:
			{
				ConstantArrayType cat = ((ConstantArrayType)t);
				Pair<Long, Integer> res = getTypeInfo(cat.getAsEnumType());
				width = res.first * cat.getSize().getZExtValue();
				align = res.second;
				width = Util.roundUp(width, align);
				break;
			}
			case Pointer:
			{
				int as = ((PointerType)t).getPointeeType().getAddressSpace();

				width = target.getPointerWidth(as);
				align = target.getPointerAlign(as);
				break;
			}

			case Struct:
			case Union:
			case Enum:
			{
				TagType tt = (TagType)t;
				if (tt.getDecl().isInvalidDecl())
				{
					width = 8;
					align = 8;
					break;
				}
				if (tt instanceof EnumType)
					return getTypeInfo(((EnumType)tt).getDecl().getPromotionType() );

				RecordType rt = (RecordType)tt;
				ASTRecordLayout layout = ASTRecordLayout.getRecordLayout(this, rt.getDecl());
				width = layout.getSize();
				align = (int)layout.getAlignment();
				break;
			}
			case TypeDef:
			{
				TypeDefDecl typedef = ((TypedefType)t).getDecl();
				Pair<Long, Integer> info = getTypeInfo(typedef.getUnderlyingType().getType());

				align = info.second;
				width = info.first;
				break;
			}
			case Bool:
				width = target.getBoolWidth();
				align = target.getBoolAlign();
				break;
			case Char_U:
			case Char_S:
			case UChar:
			case SChar:
				width = target.getCharWidth();
				align = target.getCharAlign();
				break;
			case UShort:
			case Short:
				width = target.getShortWidth();
				align = target.getShortAlign();
				break;
			case UInt:
			case Int:
				width = target.getIntWidth();
				align = target.getIntAlign();
				break;
			case ULong:
			case Long:
				width = target.getLongWidth();
				align = target.getLongAlign();
				break;
			case LongLong:
			case ULongLong:
				width = target.getLongLongWidth();
				align = target.getLonglongAlign();
				break;
			case UInt128:
			case Int128:
				width = 128;
				align = 128;
				break;
			case Float:
				width = target.getFloatWidth();
				align = target.getFloatAlign();
				break;
			case Double:
				width = target.getDoubleWidth();
				align = target.getDoubleAlign();
				break;
			case LongDouble:
				width = target.getLongDoubleWidth();
				align = target.getLongDoubleAlign();
				break;
		}

		assert Util.isPowerOf2(align):"Alignment must be power of 2!";
		return new Pair<>(width, align);
	}

	public int toByteUnitFromBits(long bits)
	{
		return (int)bits>>3;
	}

	public boolean isConstant(QualType type)
	{
		if (type.isConstQualifed())
			return true;

		if (type.getType().isArrayType())
		{
			return isConstant(getAsArrayType(type).getElementType());
		}
		return false;
	}

	/**
	 * Return the canonical (structural) type corresponding to the specified
	 * potentially non-canonical type. The non-canonical version of a type may
	 * have many "decorated" versions of types. Decorators can include typedefs.
	 * The returned type is guaranteed to be free of any of these, allowing two
	 * canonical types to compared for exact equality with a simple pointer
	 * comparison.
	 * @param type
	 * @return
	 */
	public QualType getCanonicalType(QualType type)
	{
		QualType canType = type.getType().getCanonicalTypeInternal();

		// If the result has type qualifiers, make sure to clear it.
		int typeQuals = type.getCVRQualifiers() | canType.getCVRQualifiers();
		if (typeQuals == 0)
			return canType;

		// If the type qualifiers are on an array type, get the canonical type of the
		// array with the qualifiers applied to the element type.
		if (!(canType.getType() instanceof ArrayType))
		{
			return canType.getQualifiedType(typeQuals);
		}

		// Get the canonical version of the element with the extra qualifiers on it.
		// This can recursively sink qualifiers through multiple levels of arrays.'
		ArrayType at = (ArrayType)canType.getType();
		QualType newEltTy = at.getElementType().getWithAdditionalQualifiers(typeQuals);
		newEltTy = getCanonicalType(newEltTy);

		if (at instanceof ConstantArrayType)
		{
			ConstantArrayType cat = (ConstantArrayType )at;
			return getConstantArrayType(newEltTy, cat.getSize(),
                    cat.getSizeModifier(), cat.getIndexTypeQuals());
		}

		if (at instanceof IncompleteArrayType)
		{
			IncompleteArrayType iat = (IncompleteArrayType)at;
			return getIncompleteArrayType(newEltTy,
                    iat.getSizeModifier(), iat.getIndexTypeQuals());
		}

		assert at instanceof VariableArrayType;

		VariableArrayType vat = (VariableArrayType)at;
		return getVariableArrayType(newEltTy, vat.getSizeExpr(),
                vat.getSizeModifier(), vat.getIndexTypeQuals(),
                vat.getBrackets());
	}

	/**
	 * Checks whether the unqualified type of t1 is as same as t2.
	 * @param t1
	 * @param t2
	 * @return
	 */
	public boolean hasSameUnqualifiedType(QualType t1, QualType t2)
	{
		t1 = t1.getType().getCanonicalTypeInternal();
		t2 = t2.getType().getCanonicalTypeInternal();
		return t1.getUnQualifiedType() == t2.getUnQualifiedType();
	}

	/**
	 *
	 * @param typeClass
	 * @return
	 */
	public boolean isSpecifiedBuiltinType(QualType type, int typeClass)
	{
		return type.isSpecifiedBuiltinType(typeClass);
	}

	public QualType getLValueExprType(QualType type)
	{
		// See C99 6.3.2.1p2.
		return !type.isRecordType() ? type.getUnQualifiedType() : type;
	}

	public FullSourceLoc getFullLoc(SourceLocation location)
	{
		return new FullSourceLoc(location, sourceMgr);
	}

	/**
	 * Return the unique type for "size_t" (C99 7.17), the result
	 * of the sizeof operator (C99 6.5.3.4p4). The value is target dependent and
	 * needs to agree with the definition in <stddef.h>.
	 * @return
	 */
	public QualType getSizeType()
	{
		return getFromTargetType(target.getSizeType());
	}

	//===----------------------------------------------------------------------===//
	//                          Builtin Type Computation
	//===----------------------------------------------------------------------===//
	private static QualType decodeTypeFromStr(String str,
			OutParamWrapper<Integer> idx,
			ASTContext context,
			OutParamWrapper<GetBuiltinTypeError> error)
	{
		return decodeTypeFromStr(str, idx, context, error, true);
	}

	/// DecodeTypeFromStr - This decodes one type descriptor from Str, advancing the
	/// pointer over the consumed characters.  This returns the resultant type.
	private static QualType decodeTypeFromStr(String str,
			OutParamWrapper<Integer> idx, ASTContext context,
			OutParamWrapper<GetBuiltinTypeError> error,
			boolean allowTypeModifiers)
	{
		int howLong = 0;
		boolean signed = false, unsigned = false;
		boolean done = false;

		// Read prefixed modifiers, like 'U', 'S', 'L'.
		while (!done)
		{
			switch (str.charAt(idx.get()))
			{
				default:
					done = true;
					idx.set(idx.get() - 1);
					break;
				case 'S':
					assert !unsigned : "Can not use both 'S' and 'U' modifiers";
					assert !signed : "Can not use 'S' modifiers multiple times";
					signed = true;
					break;
				case 'U':
					assert !unsigned : "Can not use 'U' modifiers multiple times";
					assert !signed : "Can not use both 'S' and 'U' modifiers";
					unsigned = true;
					break;
				case 'L':
					assert howLong <= 2 : "Can not have LLLL modifier";
					++howLong;
					break;
			}
			idx.set(idx.get() + 1);
		}

		// Step2, read the middle type specifier.
		QualType type;
		switch (str.charAt(idx.get()))
		{
			default:
				assert false : "Unknown builtin type letter!";
			case 'v':
				assert howLong == 0 && !signed
						&& !unsigned : "Bad modifiers used with 'v'";
				type = context.VoidTy;
				break;
			case 'f':
				assert howLong == 0 && !signed
						&& !unsigned : "Bad modifiers used with 'f'";
				type = context.FloatTy;
				break;
			case 'd':
				assert howLong < 2 && !unsigned
						&& !signed : "Bad modifiers used with 'd;";
				if (howLong != 0)
					type = context.LongDoubleTy;
				else
					type = context.DoubleTy;
				break;
			case 's':
				assert howLong == 0 : "Bad modifiers with 's'";
				if (unsigned)
					type = context.UnsignedShortTy;
				else
					type = context.ShortTy;
				break;
			case 'i':
			{
				switch (howLong)
				{
					case 3:
						type = unsigned ? context.UnsignedInt128Ty :
								context.Int128Ty;
						break;
					case 2:
						type = unsigned ? context.UnsignedLongLongTy :
								context.LongLongTy;
						break;
					case 1:
						type = unsigned ? context.UnsignedLongTy :
								context.LongTy;
						break;
					default:
						type = unsigned ? context.UnsignedIntTy : context.IntTy;
						break;
				}
				break;
			}
			case 'c':
			{
				assert howLong == 0 : "Bad modifiers with 'c'";
				if (signed)
					type = context.SignedCharTy;
				else if (unsigned)
					type = context.UnsignedCharTy;
				else
					type = context.CharTy;
				break;
			}
			case 'b':   // boolean
				assert howLong == 0 : "Bad modifiers with 'b'";
				type = context.BoolTy;
				break;
			case 'z':
				// size_t
				assert howLong == 0 : "Bad modifiers with 'z'";
				type = context.getSizeType();
				break;
			case 'a':
				type = context.getBuiltinVaListType();
				assert !type.isNull() : "builtin va list type not initialized";
				break;
			case 'P':
				type = context.getFILEType();
				if (type.isNull())
				{
					error.set(GE_Missing_stdio);
					return new QualType();
				}
				break;
			case 'J':
			{
				if (signed)
					type = context.getSigjmp_buf_Type();
				else
					type = context.getJmp_bufType();
				if (type.isNull())
				{
					error.set(GE_Missing_setjmp);
					return new QualType();
				}
				break;
			}
		}
		idx.set(idx.get() + 1);

		if (!allowTypeModifiers)
		{
			return type;
		}

		// Step3, read the postfixed modifiers.
		done = false;
		while (!done)
		{
			switch (str.charAt(idx.get()))
			{
				default:
					done = true;
					idx.set(idx.get() - 1);
					break;
				case '*':
					type = context.getPointerType(type);
					break;
				case 'C':
					type = type.getQualifiedType(QualType.CONST_QUALIFIER);
					break;
			}
			idx.set(idx.get() + 1);
		}
		return type;
	}

	/**
	 * Return the type for the specified builtin.
	 * @param builtid
	 * @return
	 */
	public Pair<QualType, GetBuiltinTypeError> getBuiltinType(int builtid)
	{
		String typeStr = builtinInfo.getTypeString(builtid);
		ArrayList<QualType> argTypes = new ArrayList<>();

		//GetBuiltinTypeError error = GE_None;
		OutParamWrapper<Integer> idx = new OutParamWrapper<>(0);
		OutParamWrapper<GetBuiltinTypeError> error = new OutParamWrapper<>(GE_None);
		QualType resType = decodeTypeFromStr(typeStr, idx, this, error);
		if (error.get() != GE_None)
			return Pair.get(new QualType(), error.get());

		while (idx.get() < typeStr.length() && typeStr.charAt(idx.get()) != '.')
		{
			QualType ty = decodeTypeFromStr(typeStr, idx, this, error);
			if (error.get() != GE_None)
				return Pair.get(new QualType(), error.get());

			// Do array-> pointer array. The builtin should use the decayed type.
			if (ty.isArrayType())
			{
				ty = getArrayDecayedType(ty);
			}
			argTypes.add(ty);
		}

		assert idx.get() + 1 == typeStr.length() || typeStr.charAt(idx.get()) != '.'
				:"'.' should only occur at end of builtin type list";

		if (argTypes.isEmpty() && typeStr.charAt(idx.get()) == '.')
		{
			return Pair.get(getFunctionNoProtoType(resType), error.get());
		}
		return Pair.get(getFunctionType(resType, argTypes,
				typeStr.charAt(idx.get()) == '.', 0),
				error.get());
	}

	public void setFILEDecl(TypeDecl fd)
	{
		fileDecl = fd;
	}

	public QualType getFILEType()
	{
		if (fileDecl != null)
			return getTypeDeclType(fileDecl);
		return new QualType();
	}

	public TypeDecl getJmp_bufDecl()
	{
		return jmp_bufDecl;
	}

	public QualType getJmp_bufType()
	{
		if (jmp_bufDecl != null)
			return getTypeDeclType(jmp_bufDecl);
		return new QualType();
	}

	public void setJmp_bufDecl(TypeDecl decl)
	{
		this.jmp_bufDecl = decl;
	}

	public TypeDecl getSigjmp_buf_Decl()
	{
		return sigjmp_buf_Decl;
	}

	public QualType getSigjmp_buf_Type()
	{
		if (sigjmp_buf_Decl != null)
			return getTypeDeclType(sigjmp_buf_Decl);
		return new QualType();
	}

	public void setSigjmp_buf_Decl(TypeDecl decl)
	{
		this.sigjmp_buf_Decl = decl;
	}

    enum FloatingRank
	{
		FloatRank, DoubleRank, LongDoubleRank
	}
	/**
	 * Return a relative rank for floating point types.
	 * This routine will assert if passed a built-in type that isn't a float.
	 * @param ty
	 * @return
	 */
	static FloatingRank getFloatingRank(QualType ty, ASTContext ctx)
	{
		ComplexType ct = ctx.getAs(ty, ComplexType.class);
		if (ct != null)
			return getFloatingRank(ct.getElementType(), ctx);

		assert ty.isBuiltinType(): "getFloatingRank(): not a floating type";
		switch (ty.getTypeClass())
		{
			default: Util.shouldNotReachHere("getFloatingRank(): not a floating type");
			case TypeClass.Float:      return FloatRank;
			case TypeClass.Double:     return DoubleRank;
			case TypeClass.LongDouble: return LongDoubleRank;
		}
	}

	public int getFloatingTypeOrder(QualType lhs, QualType rhs)
	{
		FloatingRank rank1 = getFloatingRank(lhs, this);
		FloatingRank rank2 = getFloatingRank(rhs, this);

		return rank1.compareTo(rank2);
	}

	/**
	 * Return an integer conversion rank (C99 6.3.1.1p1). This
	 /// routine will assert if passed a built-in type that isn't an integer or enum,
	 /// or if it is not canonicalized.
	 */
	public int getIntegerRank(Type T) 
	{
		//assert(T.isCanonicalUnqualified() && "T should be canonicalized");
		if (T instanceof EnumType)
		{
			EnumType et = (EnumType)T;
			T = et.getDecl().getPromotionType().getType();
		}

		switch (T.getTypeClass())
		{
			default: Util.shouldNotReachHere("getIntegerRank(): not a built-in integer");
			case TypeClass.Bool:
				return 1 + (getIntWidth(BoolTy) << 3);
			case TypeClass.SChar:
			case TypeClass.Char_U:
				return 2 + (getIntWidth(CharTy) << 3);
			case TypeClass.Short:
			case TypeClass.UShort:
				return 3 + (getIntWidth(ShortTy) << 3);
			case TypeClass.Int:
			case TypeClass.UInt:
				return 4 + (getIntWidth(IntTy) << 3);
			case TypeClass.Long:
			case TypeClass.ULong:
				return 5 + (getIntWidth(LongTy) << 3);
			case TypeClass.LongLong:
			case TypeClass.ULongLong:
				return 6 + (getIntWidth(LongLongTy) << 3);
		}
	}

	public int getIntegerTypeOrder(QualType lhs, QualType rhs)
	{
		Type lhsC = getCanonicalType(lhs).getType();
		Type rhsC = getCanonicalType(rhs).getType();

		if (lhsC.equals(rhsC))
			return 0;

		boolean lhsUnsigned = !lhsC.isSignedIntegerType() && lhsC.isIntegerType();
		boolean rhsUnsigned = !rhsC.isSignedIntegerType() && rhsC.isIntegerType();

		int lhsRank = getIntegerRank(lhsC);
		int rhsRank = getIntegerRank(rhsC);

		if (lhsUnsigned = rhsUnsigned)
		{
			if (lhsRank == rhsRank)
				return 0;
			return lhsRank > rhsRank ? 1 : -1;
		}

		// Otherwise, the LHS is signed and the RHS is unsigned or visa versa.
		if (lhsUnsigned)
		{
			// If the unsigned [LHS] type is larger, return it.
			if (lhsRank >= rhsRank)
				return 1;

			// If the signed type can represent all values of the unsigned type, it
			// wins.  Because we are dealing with 2's complement and types that are
			// powers of two larger than each other, this is always safe.
			return -1;
		}

		// If the unsigned [RHS] type is larger, return it.
		if (rhsRank >= lhsRank)
			return -1;

		// If the signed type can represent all values of the unsigned type, it
		// wins.  Because we are dealing with 2's complement and types that are
		// powers of two larger than each other, this is always safe.
		return 1;
	}

	public QualType getCorrespondingUnsignedType(QualType type)
	{
		assert type.isSignedIntegerType() && type.isIntegerType() : "Unexpected type";

		// For enums, we return the unsigned version of the base type.
		if (type.isEnumeralType())
		{
			type = (getAs(type, EnumType.class)).getDecl().getPromotionType();
		}
		
		assert type.isBuiltinType() : "Unexpected signed integer type";
		switch (type.getTypeClass()) 
		{
			case TypeClass.SChar:
				return UnsignedCharTy;
			case TypeClass.Short:
				return UnsignedShortTy;
			case TypeClass.Int:
				return UnsignedIntTy;
			case TypeClass.Long:
				return UnsignedLongTy;
			case TypeClass.LongLong:
				return UnsignedLongLongTy;
			default:
				Util.shouldNotReachHere("Unexpected signed integer type");
		}
		return null;
	}

    /*
     * Checks if this the kind of this jlang.type is same as ty2.
     */
    public boolean isSameType(QualType ty1, QualType ty2)
	{
		return getCanonicalType(ty1) == getCanonicalType(ty2);
	}

    /**
     * Return the APFloat 'semantics' for the specified
     * scalar floating point type.
     * @param ty
     * @return
     */
	public FltSemantics getFloatTypeSemantics(QualType ty)
	{
	    assert ty.isBuiltinType():"Not a floating point type!";
        BuiltinType pty = ty.getAsBuiltinType();
	    switch (pty.getTypeClass())
        {
            default: assert false :"Not a floating point type!";
            case Float: return target.getFloatFormat();
            case Double:    return target.getDoubleFormat();
            case LongDouble:    return target.getLongDoubleFormat();
        }
	}

	public ASTRecordLayout getASTRecordLayout(RecordDecl d)
	{
        d = d.getDefinition();
        if (astRecordLayoutMap.containsKey(d))
            return astRecordLayoutMap.get(d);
        ASTRecordLayout layout = ASTRecordLayout.getRecordLayout(this, d);
        astRecordLayoutMap.put(d, layout);
        return layout;
	}
	/**
	 * Whether this is a promotable bitfield reference according to C99 6.3.1.1p2.
	 * or {@code null} if no promotion occurs.
	 * @return  Return the type this bit-field will promote to, or NULL if no
	 *           promotion occurs.
	 */
	public QualType isPromotableBitField(Expr e)
	{
		FieldDecl field = e.getBitField();
		if (field == null)
			return new QualType();

		QualType t = field.getType();
		APSInt bitWidthAP = field.getBitWidth().evaluateAsInt(this);
		long bitWidth = bitWidthAP.getZExtValue();
		long intSize = getTypeSize(IntTy);

		// GCC extension compatibility: if the bit-field size is less than or equal
		// to the size of int, it gets promoted no matter what its type is.
		// For instance, unsigned long bf : 4 gets promoted to signed int.
		if (bitWidth < intSize)
			return IntTy;
		if (bitWidth == intSize)
			return t.isSignedIntegerType() ? IntTy : UnsignedIntTy;

		// Types bigger than int are not subject to promotions, and therefore act
		// like the base type.
		return new QualType();
	}

    /**
     * For two qualified types to be compatible,
     * both shall have the identically qualified version of a compatible type.
     * C99 6.2.7p1: Two types have compatible types if their types are the
     * same. See 6.7.[2,3,5] for additional rules.
     * @param lhs
     * @param rhs
     * @return
     */
	public boolean typesAreCompatible(QualType lhs, QualType rhs)
	{
		return !mergeType(lhs, rhs).isNull();
	}

	public QualType getBuiltinVaListType()
	{
		return builtinVaListType;
	}

	public void setBuiltinVaListType(QualType builtinVaListType)
	{
		this.builtinVaListType = builtinVaListType;
	}

	public enum GetBuiltinTypeError
    {
	    /**
	     * No error.
	     */
    	GE_None,

	    /**
	     * Missing a type from stdio.h
	     */
	    GE_Missing_stdio,

	    /**
	     * Missing type from setjmp.h
	     */
	    GE_Missing_setjmp
    }

	public ArrayList<Attr> getDeclAttrs(Decl decl)
	{
		if (declAttrs.containsKey(decl))
		{
			return declAttrs.get(decl);
		}
		else
		{
			ArrayList<Attr> alist = new ArrayList<>();
			declAttrs.put(decl, alist);
			return alist;
		}
	}

	public ArrayList<Attr> eraseDeclAttrs(Decl d)
	{
		if (declAttrs.containsKey(d))
		{
			return declAttrs.remove(d);
		}
		return null;
	}

	public void putDeclAttrs(Decl d, ArrayList<Attr> alist)
	{
		assert !declAttrs.containsKey(d);
		declAttrs.put(d, alist);
	}
}
