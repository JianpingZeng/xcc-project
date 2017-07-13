package jlang.sema;
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

import jlang.ast.Tree.Expr;
import jlang.basic.*;
import jlang.basic.TargetInfo.IntType;
import jlang.clex.IdentifierTable;
import jlang.diag.FullSourceLoc;
import jlang.sema.Decl.*;
import jlang.support.*;
import jlang.type.*;
import jlang.type.ArrayType.*;
import tools.Pair;
import tools.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import static jlang.sema.ASTContext.FloatingRank.*;
import static jlang.type.ArrayType.ArraySizeModifier.Normal;
import static jlang.type.TypeClass.Bool;
import static jlang.type.TypeClass.Char;
import static jlang.type.TypeClass.ConstantArray;
import static jlang.type.TypeClass.Double;
import static jlang.type.TypeClass.Enum;
import static jlang.type.TypeClass.Float;
import static jlang.type.TypeClass.FunctionProto;
import static jlang.type.TypeClass.IncompleteArray;
import static jlang.type.TypeClass.Int;
import static jlang.type.TypeClass.LongDouble;
import static jlang.type.TypeClass.LongInteger;
import static jlang.type.TypeClass.LongLong;
import static jlang.type.TypeClass.Pointer;
import static jlang.type.TypeClass.Short;
import static jlang.type.TypeClass.Struct;
import static jlang.type.TypeClass.TypeDef;
import static jlang.type.TypeClass.Union;
import static jlang.type.TypeClass.UnsignedChar;
import static jlang.type.TypeClass.UnsignedInt;
import static jlang.type.TypeClass.UnsignedLong;
import static jlang.type.TypeClass.UnsignedLongLong;
import static jlang.type.TypeClass.UnsignedShort;
import static jlang.type.TypeClass.VariableArray;
import static jlang.type.TypeClass.Void;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class ASTContext
{
    private ArrayList<Type> types = new ArrayList<>();
    private TreeSet<ComplexType> complexTypes = new TreeSet<>();
    private TreeSet<PointerType> pointerTypes = new TreeSet<>();
    private TreeSet<ConstantArrayType> constantArrayTypes = new TreeSet<>();
    private TreeSet<IncompleteArrayType> incompleteArrayTypes = new TreeSet<>();
    private TreeSet<VariableArrayType> variableArrayTypes = new TreeSet<>();
    private TreeSet<FunctionNoProtoType> functionNoProtoTypes = new TreeSet<>();
    private TreeSet<FunctionProtoType> functionProtoTypes = new TreeSet<>();
    private HashMap<RecordDecl, ASTRecordLayout> astRecordLayoutMap = new HashMap<>();
    /**
     * A cache mapping from RecordDecls to ASTRecordLayouts.
     * This is lazily created.  This is intentionally not serialized.
     */
    private TreeSet<IncompleteArrayType> IncompleteArrayTypes = new TreeSet<>();
    private DeclContext translateUnitDecl;
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
	public final QualType FloatTy;
	public final QualType DoubleTy;
    public final QualType LongDoubleTy;
    public final QualType FloatComplexTy;
    public final QualType DoubleComplexTy;
    public final QualType LongDoubleComplexTy;

    public final QualType VoidPtrTy;

	public ASTContext(LangOptions opts, SourceManager sourceMgr,
			TargetInfo targetInfo, IdentifierTable identifierTable)
	{
		langOptions = opts;
		this.sourceMgr = sourceMgr;
		target = targetInfo;
		this.identifierTable = identifierTable;
        tuDel = TranslationUnitDecl.create(this);

        VoidTy = initBuiltinType(Void);
        BoolTy = initBuiltinType(Bool);
        CharTy = initBuiltinType(Char);
        SignedCharTy = initBuiltinType(Char);
        ShortTy = initBuiltinType(Short);
        IntTy = initBuiltinType(Int);
        LongTy = initBuiltinType(LongInteger);
        LongLongTy = initBuiltinType(LongLong);

        UnsignedCharTy = initBuiltinType(UnsignedChar);
        UnsignedShortTy = initBuiltinType(UnsignedShort);
        UnsignedIntTy = initBuiltinType(UnsignedInt);
        UnsignedLongTy = initBuiltinType(UnsignedLong);
        UnsignedLongLongTy = initBuiltinType(UnsignedLongLong);

        FloatTy = initBuiltinType(Float);
        DoubleTy = initBuiltinType(Double);
        LongDoubleTy = initBuiltinType(LongDouble);
        FloatComplexTy = getComplexType(FloatTy);
        DoubleComplexTy = getComplexType(DoubleTy);
        LongDoubleComplexTy = getComplexType(LongDoubleTy);

        VoidPtrTy = getPointerType(VoidTy);
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

    public QualType getComplexType(QualType eltType)
    {
        // TODO: 17-5-13
        return null;
    }

    /**
     * Returns the reference to the jlang.type for an array of the specified element jlang.type.
     * @param elemTy
     * @param size
     * @return
     */
    public QualType getConstantType(QualType elemTy, APInt size, ArraySizeModifier asm, int tq)
    {
        assert elemTy.isIncompleteType()
                || elemTy.isConstantSizeType():"Constant arrays of VLAs is illegal!";

        ConstantArrayType New = new ConstantArrayType(elemTy, size, asm, tq);
        return new QualType(New, 0);
    }

    public QualType getPointerType(QualType ty)
	{
        // TODO: 17-5-13
        return null;
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
		QualType newElemTy = getQualifiedType(aty.getElemType(), type.getQualifiers());

		if (aty instanceof ConstantArrayType)
		{
			ConstantArrayType cat = (ConstantArrayType)aty;
			return getConstantArrayType(newElemTy, cat.getSize());
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
		VariableArrayType vat = new VariableArrayType(elemTy,
                numElts, asm, eltTypeQuals, brackets);
		types.add(vat);
		return new QualType(vat);
	}

    /**
     * Return a reference to the type for
     * an array of the specified element type.
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
        arrSize.zextOrTrunc(target.getPointerWidth(0));
        ConstantArrayWithExprType arr = new ConstantArrayWithExprType(eltTy,
                arrSize, arraySizeExpr, asm, eltTypeQuals,brackets);
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
        arrSize.zextOrTrunc(target.getPointerWidth(0));

        ConstantArrayWithoutExprType arr = new ConstantArrayWithoutExprType(
                eltTy, arraySizeIn, asm, eltTypeQuals);
        types.add(arr);
        // TODO: 17-5-13
        return new QualType(arr);
    }

	public ConstantArrayType getConstantArrayType(
	        QualType elemTy,
            final APInt arraySize)
	{
		assert elemTy.isIncompleteType()||elemTy.isConstantSizeType()
				:"Constant array of VLAs is illegal";

		APInt size = new APInt(arraySize);
		size = size.zextOrTrunc(32);
		ConstantArrayType New = null;
		return New;

        // TODO: 17-5-13
    }

	public QualType getIncompleteArrayType(
			QualType elemTy,
			ArraySizeModifier sm,
            int eltTypeQuals)
	{
        // TODO: 17-5-13
        return null;
	}

    /**
     * Return a normal function type with a typed argument
     * list.
     * @param isVariadic Indicates whether the argument list includes '...'.
     * @return
     */
	public QualType getFunctionType(
	        QualType resultTy,
            ArrayList<QualType> argTys,
            boolean isVariadic,
            int typeQuals)
    {
        // TODO: 17-5-13
        return null;
    }

    /**
     * Return a K&R style C function type like 'int()'.
     * @param resultTy
     * @return
     */
    public QualType getFunctionNoProtoType(QualType resultTy)
    {
        // TODO: 17-5-13
        return null;
    }

	/**
	 * Returns the probably qualified result of specified array decaying into pointer
	 * jlang.type.
	 *
	 * @param ty
	 * @return
	 */
	public QualType getArrayDecayedType(QualType ty)
	{
	    final ArrayType arrayType = getAsArrayType(ty);
	    assert arrayType != null : "Not an array jlang.type!";

	    QualType ptrTy = getPointerType(arrayType.getElemType());

	    // int x[restrict 4]-> int *restrict;
	    // TODO unhandle jlang.type qualifier in array getNumOfSubLoop expression.
	    return ptrTy;
	}

	public LangOptions getLangOptions()
	{
		return langOptions;
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
        // TODO: 17-5-13
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
				if (ety.getDecl().getIntegerType().equals(rhs.getUnQualifiedType()))
					return rhs;
			}
			ety = rhs.getAsEnumType();
			if (ety != null)
			{
				if (ety.getDecl().getIntegerType().equals(lhs.getUnQualifiedType()))
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

				QualType lhsElem = getAsArrayType(lhs).getElemType();
				QualType rhsElem = getAsArrayType(rhs).getElemType();

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
					return new QualType(getConstantArrayType(resultType, lcat.getSize()));

				if (rcat != null)
					return new QualType(getConstantArrayType(resultType, rcat.getSize()));

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

			case UnsignedChar:
			case UnsignedShort:
			case UnsignedInt:
			case UnsignedLong:
			case Char:
			case Short:
			case LongInteger:
			case Float:
				return new QualType();
		}
		return new QualType();
	}


	public DeclContext getTranslateUnitDecl()
	{
		return translateUnitDecl;
	}

	public int getTypeAlign(QualType type)
	{
		return getTypeInfo(type).second;
	}

	public long getTypeSize(QualType type)
	{
		return getTypeInfo(type).first;
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
				case TypeClass.Char:
				case TypeClass.UnsignedChar:
				case TypeClass.Short:
				case TypeClass.UnsignedShort:
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
			QualType qt = et.getDecl().getIntegerType();
			if (qt.isNull())
				return false;

			return qt.getType().getTypeClass() == TypeClass.Int
					|| qt.getType().getTypeClass() == TypeClass.UnsignedInt;
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
			return ((EnumType) type.getType()).getDecl().getIntegerType();
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

		TypedefType newType = new TypedefType(TypeClass.TypeDef, decl, null);
		decl.setTypeForDecl(newType);
		return new QualType(newType);
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

			type = getAsArrayType(type).getElemType();
			qs.addCVQualifier(type.qualsFlag.mask);
		}
		return getQualifiedType(type, qs);
	}

	public QualType getBaseElementType(VariableArrayType vat)
	{
		QualType eltType = vat.getElemType();
		VariableArrayType eltVat = getAsVariableArrayType(eltType);
		if (eltVat != null)
			return getBaseElementType(eltVat);

		return eltType;
	}

	/**
	 * A generic method for casting a jlang.type instance to TargetData jlang.type.
	 * If casting failed, return null, otherwise, return the instance
	 * jlang.type required jlang.type.
	 * @param <T>
	 * @return
	 */
	public <T extends Type> T getAs(QualType type)
	{
		return convertInstanceOfObject(type, type.getClass());
	}

	public  <T extends Type> T convertInstanceOfObject(
			QualType o,
			Class<? extends QualType> clazz)
	{
		try
		{
			return (T)clazz.cast(o).getType();
		}
		catch (ClassCastException e)
		{
			return null;
		}
	}

	public boolean isSignedIntegerOrEnumerationType(QualType type)
	{
		if (type.isBuiltinType())
		{
			return type.getTypeClass() >= TypeClass.Char
					&& type.getTypeClass() <= TypeClass.LongInteger;
		}
		if (type.isEnumeralType())
		{
			EnumType et = (EnumType)type.getType();
			if (et.getDecl().isCompleteDefinition())
				return et.getDecl().getIntegerType().isSignedIntegerType();
		}
		return false;
	}

	public boolean isUnsignedIntegerOrEnumerationType(QualType type)
	{
		if (type.isBuiltinType())
		{
			return type.getTypeClass() >= TypeClass.Bool
					&& type.getTypeClass() <= TypeClass.UnsignedLong;
		}
		if (type.isEnumeralType())
		{
			EnumType et = (EnumType)type.getType();
			if (et.getDecl().isCompleteDefinition())
				return !et.getDecl().getIntegerType().isSignedIntegerType();
		}
		return false;
	}

	public int getIntWidth(QualType t)
	{
		if (t.isEnumeralType())
		{
			t = new QualType(t.getAsEnumType().getDecl().getIntegerType().getType());
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
				align = getTypeAlign(((ArrayType)t).getElemType());
				break;
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
					return getTypeInfo(((EnumType)tt).getDecl().getIntegerType() );

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
			return isConstant(getAsArrayType(type).getElemType());
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
		QualType newEltTy = at.getElemType().getWithAdditionalQualifiers(typeQuals);
		newEltTy = getCanonicalType(newEltTy);

		if (at instanceof ConstantArrayType)
		{
			ConstantArrayType cat = (ConstantArrayType )at;
			return new QualType(getConstantArrayType(newEltTy, cat.getSize()));
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
		ComplexType ct = ctx.<ComplexType>getAs(ty);
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
			T = et.getDecl().getIntegerType().getType();
		}

		switch (T.getTypeClass())
		{
			default: Util.shouldNotReachHere("getIntegerRank(): not a built-in integer");
			case TypeClass.Bool:
				return 1 + (getIntWidth(BoolTy) << 3);
			case TypeClass.Char:
			case TypeClass.UnsignedChar:
				return 2 + (getIntWidth(CharTy) << 3);
			case TypeClass.Short:
			case TypeClass.UnsignedShort:
				return 3 + (getIntWidth(ShortTy) << 3);
			case TypeClass.Int:
			case TypeClass.UnsignedInt:
				return 4 + (getIntWidth(IntTy) << 3);
			case TypeClass.LongInteger:
			case TypeClass.UnsignedLong:
				return 5 + (getIntWidth(LongTy) << 3);
			case TypeClass.LongLong:
			case TypeClass.UnsignedLongLong:
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
			type = (this.<EnumType>getAs(type)).getDecl().getIntegerType();
		}
		
		assert type.isBuiltinType() : "Unexpected signed integer type";
		switch (type.getTypeClass()) 
		{
			case TypeClass.Char:
				return UnsignedCharTy;
			case TypeClass.Short:
				return UnsignedShortTy;
			case TypeClass.Int:
				return UnsignedIntTy;
			case TypeClass.LongInteger:
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
		return getASTRecordLayout(d);
	}
}
