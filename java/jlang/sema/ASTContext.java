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

import jlang.ast.Tree;
import jlang.basic.LangOptions;
import jlang.basic.TargetInfo;
import jlang.basic.TargetInfo.IntType;
import jlang.cparser.DeclContext;
import jlang.sema.Decl.*;
import jlang.type.*;
import jlang.type.ArrayType.ConstantArrayType;
import tools.Pair;
import tools.Util;

import java.util.LinkedList;

import static jlang.sema.ASTContext.FloatingRank.DoubleRank;
import static jlang.sema.ASTContext.FloatingRank.FloatRank;
import static jlang.sema.ASTContext.FloatingRank.LongDoubleRank;
import static jlang.type.TypeClass.*;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class ASTContext
{
	public final QualType VoidTy = new QualType(VoidType.New());
	public final QualType BoolTy = new QualType(new IntegerType(1, false));
	public final QualType CharTy = new QualType(new IntegerType(1, true));
	public final QualType SignedCharTy = new QualType(new IntegerType(1, true));
	public final QualType UnsignedCharTy = new QualType(new IntegerType(1, false));
	public final QualType ShortTy = new QualType(new IntegerType(2, true));
	public final QualType UnsignedShortTy = new QualType(
	        new IntegerType(2, false));
	public final QualType IntTy = new QualType(new IntegerType(4, true));
	public final QualType UnsignedIntTy = new QualType(
	        new IntegerType(4, false));
	public final QualType LongTy = new QualType(new IntegerType(4, true));
	public final QualType UnsignedLongTy = new QualType(new IntegerType(4, false));
	public final QualType LongLongTy = new QualType(new IntegerType(8, true));
	public final QualType UnsignedLongLongTy = new QualType(new IntegerType(8, false));
	public final QualType FloatTy = new QualType(new RealType(4, "float"));
	public final QualType DoubleTy = new QualType(new RealType(8, "double"));

	public LangOptions langOptions;
	public TargetInfo target;
	public LinkedList<Type> types = new LinkedList<>();
	private DeclContext translateUnitDecl;

	public QualType getPointerType(Type ty)
	{
	    PointerType New = new PointerType(ty);
	    return new QualType(New, 0);
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

	public ArrayType.IncompleteArrayType getAsInompleteArrayType(QualType type)
	{
		ArrayType res = getAsArrayType(type);
		return (res instanceof ArrayType.IncompleteArrayType)
				? (ArrayType.IncompleteArrayType)res : null;
	}

	public ArrayType.VariableArrayType getAsVariableArrayType(QualType type)
	{
		ArrayType res = getAsArrayType(type);
		return (res instanceof ArrayType.VariableArrayType)
				? (ArrayType.VariableArrayType)res : null;
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

		if (aty instanceof ArrayType.IncompleteArrayType)
		{
			ArrayType.IncompleteArrayType icat = (ArrayType.IncompleteArrayType)aty;
			return getIncompleteArrayType(newElemTy);
		}

		ArrayType.VariableArrayType vat = (ArrayType.VariableArrayType)aty;
		return getVariableArrayType(newElemTy, vat.getSizeExpr());
	}

	public ArrayType.VariableArrayType getVariableArrayType(QualType elemTy, Tree.Expr sizeExpr)
	{
		return new ArrayType.VariableArrayType(elemTy, sizeExpr);
	}

	public ConstantArrayType getConstantArrayType(QualType elemTy, final APInt arraySize)
	{
		assert elemTy.isIncompleteType()||elemTy.isConstantSizeType()
				:"Constant array of VLAs is illegal";

		APInt size = new APInt(arraySize);
		size = size.zextOrTrunc(32);
		ConstantArrayType New = new ConstantArrayType(elemTy, size);
		return New;
	}

	public ArrayType.IncompleteArrayType getIncompleteArrayType(QualType elemTy)
	{
		return new ArrayType.IncompleteArrayType(elemTy);
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
		decl.setTyepForDecl(new TypeDefType(TypeClass.TypeDef, decl, cannonical));
		types.addLast(decl.getTypeForDecl());
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
				decl.setTyepForDecl(prevDecl.getTypeForDecl());
			else
				decl.setTyepForDecl(new RecordType(record));
		}
		else if (decl instanceof EnumDecl)
		{
			EnumDecl enumDecl = (EnumDecl)decl;
			if (prevDecl != null)
				decl.setTyepForDecl(prevDecl.getTypeForDecl());
			else
				decl.setTyepForDecl(new EnumType(enumDecl));
		}
		else
		{
			assert false:"TypeDecl without a type?";
		}
		if (prevDecl == null)
			types.addLast(decl.getTypeForDecl());
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
			EnumType ety = lhs.getEnumType();
			if (ety != null)
			{
				if (ety.getDecl().getIntegerType().equals(rhs.getUnQualifiedType()))
					return rhs;
			}
			ety = rhs.getEnumType();
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
			case Function:
				Util.shouldNotReachHere("Types are eliminated abo e");

			case Pointer:
			{
				// Merge two pointer types, while trying to preserve typedef info.
				QualType lhsPointee = getPointerType(lhs).getPointee();
				QualType rhsPointee = getPointerType(rhs).getPointee();

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

				ArrayType.VariableArrayType lvat = getAsVariableArrayType(lhs);
				ArrayType.VariableArrayType rvat = getAsVariableArrayType(rhs);
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

				return new QualType(getIncompleteArrayType(resultType));
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
		if (type.getType() instanceof PrimitiveType)
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
		if (type.isEnumType())
		{
			EnumType et = type.getEnumType();
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
		if (type.isSignedType())
			return IntTy;
		long promotableSize = type.getTypeSize();
		long intSize = IntTy.getTypeSize();
		assert !type.isSignedType() && promotableSize <= intSize;

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

		TypeDefType newType = new TypeDefType(TypeClass.TypeDef, decl, null);
		decl.setTyepForDecl(newType);
		return new QualType(newType);
	}

	public static QualType getRecordType(RecordDecl record)
	{
		if (record.getTypeForDecl() != null)
			return new QualType(record.getTypeForDecl());

		RecordType newType = new RecordType(record);
		record.setTyepForDecl(newType);
		return new QualType(newType);
	}

	public static QualType getEnumType(Decl.EnumDecl decl)
	{
		if (decl.getTypeForDecl() != null)
			return new QualType(decl.getTypeForDecl());

		EnumType newType = new EnumType(decl);
		decl.setTyepForDecl(newType);
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

	public QualType getBaseElementType(ArrayType.VariableArrayType vat)
	{
		QualType eltType = vat.getElemType();
		ArrayType.VariableArrayType eltVat = getAsVariableArrayType(eltType);
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
			Object o,
			Class<? extends Type> clazz)
	{
		try
		{
			return (T) clazz.cast(o);
		}
		catch (ClassCastException e)
		{
			return null;
		}
	}

	public boolean isSignedIntegerOrEnumerationType(QualType type)
	{
		if (type.isPrimitiveType())
		{
			return type.getTypeKind() >= TypeClass.Char
					&& type.getTypeKind() <= TypeClass.LongInteger;
		}
		if (type.isEnumType())
		{
			EnumType et = (EnumType)type.getType();
			if (et.getDecl().isCompleteDefinition())
				return et.getDecl().getIntegerType().isSignedType();
		}
		return false;
	}

	public boolean isUnsignedIntegerOrEnumerationType(QualType type)
	{
		if (type.isPrimitiveType())
		{
			return type.getTypeKind() >= TypeClass.Bool
					&& type.getTypeKind() <= TypeClass.UnsignedLong;
		}
		if (type.isEnumType())
		{
			EnumType et = (EnumType)type.getType();
			if (et.getDecl().isCompleteDefinition())
				return !et.getDecl().getIntegerType().isSignedType();
		}
		return false;
	}

	public static int getIntWidth(QualType t)
	{
		if (t.isEnumType())
		{
			t = new QualType(t.getEnumType().getIntegerType());
		}
		if (t.isBooleanType())
			return 1;
		return (int)t.getTypeSize();
	}

	/**
	 * Return the ABI-specified alignment of a jlang.type, in bytes.
	 * This method does not work on incomplete types.
	 * @param t
	 * @return
	 */
	public int getTypeAlignInBytes(QualType t)
	{
		return toByteUnitFromBits(t.alignment());
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
			case Function:
				// GCC extension: alignof(function) = 32 bits
				width = 0;
				align = 32;

			case IncompleteArray:
			case VariableArray:
				width = 0;
				align = (int)((ArrayType)t).getElemType().alignment();
				break;
			case ConstantArray:
			{
				ConstantArrayType cat = ((ConstantArrayType)t);
				Pair<Long, Integer> res = getTypeInfo(cat.getEnumType());
				width = res.first * cat.getSize().getZExtValue();
				align = res.second;
				width = Util.roundUp(width, align);
				break;
			}
			case Pointer:
			{
				int as = ((PointerType)t).getPointerType().getTypeSizeInBytes();
				width = as;
				align = as;
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
				TypeDefDecl typedef = ((TypeDefType)t).getDecl();
				Pair<Long, Integer> info = getTypeInfo(typedef.getUnderlyingType().getType());

				align = info.second;
				width = info.first;
				break;
			}
		}

		assert Util.isPowerOf2(align):"Alignment must be power of 2!";
		return new Pair<>(width, align);
	}

	public Pair<Integer, Integer> getTypeInfoInBytes(QualType t)
	{
		return getTypeInfoInBytes(t.getType());
	}

	public long toBits(long num)
	{
		return num<<3;
	}

	public int toByteUnitFromBits(long bits)
	{
		return (int)bits>>3;
	}

	/**
	 * C99 6.7.5p3.
	 * Return true for variable length array types and types that contain
	 * variable array types in their declarator.
	 * @return
	 */
	public boolean isVariablyModifiedType(QualType type)
	{
		// A VLA is a variably modified type.
		if (type.isVariableArrayType())
			return true;

		jlang.type.Type ty = type.getArrayElementTypeNoQuals();
		if (ty !=null)
			return ty.isVariablyModifiedType();

		PointerType pt = getAs(type);
		if (pt != null)
			return pt.getPointeeType().isVariablyModifiedType();

		FunctionType ft = type.getFunctionType();
		if (ft != null)
			return ft.getResultType().isVariablyModifiedType();
		return false;
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

		if (at instanceof ArrayType.IncompleteArrayType)
		{
			ArrayType.IncompleteArrayType iat = (ArrayType.IncompleteArrayType)at;
			return new QualType(getIncompleteArrayType(newEltTy));
		}

		assert at instanceof ArrayType.VariableArrayType;

		ArrayType.VariableArrayType vat = (ArrayType.VariableArrayType)at;
		return new QualType(getVariableArrayType(newEltTy, vat.getSizeExpr()));
	}

	public boolean isFunctionPointerType(QualType type)
	{
		PointerType t = this.<PointerType>getAs(type);
		if ( t != null)
			return t.getPointee().isFunctionType();
		return false;
	}

	public boolean isUnionType(QualType type)
	{
		return type.isUnionType();
	}

	/**
	 * Checks whether the unqualified type of t1 is as same as t2.
	 * @param t1
	 * @param t2
	 * @return
	 */
	public static boolean hasSameUnqualifiedType(QualType t1, QualType t2)
	{
		t1 = t1.getCanonicalTypeInternal();
		t2 = t2.getCanonicalTypeInternal();
		return t1.getUnQualifiedType() == t2.getUnQualifiedType();
	}

	/**
	 *
	 * @param typeClass
	 * @return
	 */
	public boolean isSpecifiedBuiltinType(QualType type, int typeClass)
	{
		return type.getTypeClass() == typeClass && type.isBuiltinType();
	}

	public QualType getLValueExprType(QualType type)
	{
		// See C99 6.3.2.1p2.
		return !type.isRecordType() ? type.getUnQualifiedType() : type;
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

		boolean lhsUnsigned = (!lhsC.isSignedType() && lhsC.isIntegerType());
		boolean rhsUnsigned = (!rhsC.isSignedType() && rhsC.isIntegerType());

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
		assert type.isSignedType() && type.isIntegerType() : "Unexpected type";

		// For enums, we return the unsigned version of the base type.
		if (type.isEnumType())
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

	public QualType getComplexType(QualType ty)
	{
		// TODO: 2017/4/9
		return null;
	}
}
