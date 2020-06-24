package cfe.type;

import cfe.support.LangOptions;
import cfe.support.PrintingPolicy;
import cfe.type.ArrayType.ConstantArrayType;
import cfe.type.ArrayType.IncompleteArrayType;
import cfe.type.ArrayType.VariableArrayType;
import cfe.type.QualType.ScalarTypeKind;
import tools.Util;

import static cfe.type.QualType.ScalarTypeKind.*;

/**
 * <p>
 * The abstract root class of various jlang.type. It provides different definitions
 * for it's concrete subclass.
 * </p>
 * A central concept with types is that each type always has a canonical type.
 * A canonical type is the type with any typedef names stripped out of it or the
 * types it references.  For example, consider:
 * <pre>
 *  typedef int  foo;
 *  typedef foo* bar;
 *  'int *'    'foo *'    'bar'
 * </pre>
 * <br></br>
 * There will be a Type object created for 'int'.  Since int is canonical, its
 * canonicaltype pointer points to itself.  There is also a Type for 'foo' (a
 * TypedefType).  Its CanonicalType pointer points to the 'int' Type.  Next
 * there is a PointerType that represents 'int*', which, like 'int', is
 * canonical.  Finally, there is a PointerType type for 'foo*' whose canonical
 * type is 'int*', and there is a TypedefType for 'bar', whose canonical type
 * is also 'int*'.
 * <p>
 * Non-canonical types are useful for emitting diagnostics, without losing
 * information about typedefs being used.  Canonical types are useful for type
 * comparisons (they allow by-pointer equality tests) and useful for reasoning
 * about whether something has a particular form (e.g. is a function type),
 * because they implicitly, recursively, strip all typedefs out of a type.
 * </p>
 * Types, once created, are immutable.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public abstract class Type implements TypeClass {
  /**
   * The type class of this type.
   */
  protected int tc;
  /**
   * The canonical type which the original type has been removed typedef,
   * modifiers after.
   */
  private QualType canonicalType;

  /**
   * Constructor with one parameter which represents the kind of jlang.type
   * for reason of comparison convenient.
   *
   * @param tc
   */
  public Type(int tc) {
    this(tc, new QualType());
  }

  public Type(int typeClass, QualType canonical) {
    tc = typeClass;
    canonicalType = canonical.isNull() ? new QualType(this, 0) : canonical;
  }

  public int getTypeClass() {
    return tc;
  }

  public boolean isCanonical() {
    return canonicalType.getType() == this;
  }

  /**
   * <p>
   * Determines whether the type describes an object in memory.
   * </p>
   * <p>
   * Types are partitioned into 3 broad categories (C99 6.2.5p1):
   * object types, function types, and incomplete types.
   * </p>
   *
   * @return
   */
  public boolean isObjectType() {
    Type ty = canonicalType.getType();
    return !(ty instanceof FunctionType
        || ty instanceof IncompleteArrayType
        || isVoidType());
  }

  /**
   * Return true if this is an incomplete type.
   * /// A type that can describe objects, but which lacks information needed to
   * /// determine its size (e.g. void, or a fwd declared struct). Clients of this
   * /// routine will need to determine if the size is actually required.
   *
   * @return
   */
  public boolean isIncompleteType() {
    switch (canonicalType.getTypeClass()) {
      default:
        return false;
      case Void:
        return true;
      case Struct:
      case Enum:
        // A tagged type (struct/union/enum/class) is incomplete if the decl is a
        // forward declaration, but not a full definition (C99 6.2.5p22).
        return !((TagType) canonicalType.getType()).getDecl().isCompleteDefinition();
      case IncompleteArray:
        return true;
    }
  }

  /**
   * Return true if this is an incomplete or object
   * type, in other words, not a function type.
   *
   * @return
   */
  public boolean isIncompleteOrObjectType() {
    return !isFunctionType();
  }

  /**
   * (C99 6.7.5.2p2) - Return true for variable array
   * types that have a non-constant expression. This does not include "[]".
   *
   * @return
   */
  public boolean isVariablyModifiedType() {
    if (isVariableArrayType())
      return true;

    Type t = getArrayElementTypeNoTypeQual();
    if (t != null)
      return t.isVariablyModifiedType();

    // A pointer can point to a variably modified type.
    if (this instanceof PointerType)
      return getPointeeType().isVariablyModifiedType();

    // A function can return a variably modified type
    // This one isn't completely obvious, but it follows from the
    // definition in C99 6.7.5p3. Because of this rule, it's
    // illegal to declare a function returning a variably modified type.
    FunctionType ft = getAsFunctionType();
    if (ft != null)
      return ft.getResultType().isVariablyModifiedType();
    return false;
  }

  /**
   * If this is an array type, return the
   * /// element type of the array, potentially with type qualifiers missing.
   * /// This method should never be used when type qualifiers are meaningful.
   *
   * @return
   */
  public Type getArrayElementTypeNoTypeQual() {
    if (this instanceof ArrayType)
      return ((ArrayType) this).getElementType().getType();

    if (!(canonicalType.getType() instanceof ArrayType)) {
      Type t = canonicalType.getUnQualifiedType().getType();
      if (t instanceof ArrayType) {
        return ((ArrayType) t).getElementType().getType();
      }
      return null;
    }

    // If this is a typedef for an array type, strip the typedef off without
    // losing all typedef information.
    return ((ArrayType) (getDesugaredType().getType())).getElementType().getType();
  }

  public QualType getDesugaredType() {
    if (this instanceof TypedefType)
      return ((TypedefType) this).lookThroughTypedefs().getDesugaredType();
    return new QualType(this);
  }

  public boolean isBuiltinType() {
    return this instanceof BuiltinType &&
        tc >= BuiltinTypeBegin && tc < BuiltinTypeEnd;
  }

  /**
   * Helper methods to distinguish type categories. All type predicates
   * /// operate on the canonical type, ignoring typedefs and qualifiers.
   *
   * @param tk
   * @return
   */
  public boolean isSpecifiedBuiltinType(int tk) {
    Util.assertion(tk >= BuiltinTypeBegin && tk < BuiltinTypeEnd);
    return tk == tc;
  }

  /**
   * // C99 6.2.5p17 (int, char, bool, enum)
   *
   * @return
   */
  public boolean isIntegerType() {
    if (canonicalType.getType() instanceof BuiltinType)
      return tc >= Bool && tc <= Int128;
    if (canonicalType.getType() instanceof TagType) {
      // Incomplete enum types are not treated as integer types.
      TagType tt = (TagType) canonicalType.getType();
      if (tt.getDecl().isEnum() && tt.getDecl().isCompleteDefinition())
        return true;
    }
    return false;
  }

  public boolean isEnumeralType() {
    if (canonicalType.getType() instanceof TagType) {
      return ((TagType) canonicalType.getType()).getDecl().isEnum();
    }
    return false;
  }

  public boolean isStructureType() {
    if (canonicalType.getType() instanceof RecordType)
      return ((RecordType) canonicalType.getType()).getDecl().isStruct();
    return false;
  }

  public boolean isUnionType() {
    if (canonicalType.getType() instanceof RecordType)
      return ((RecordType) canonicalType.getType()).getDecl().isUnion();
    return false;
  }

  public boolean isBooleanType() {
    return canonicalType.getType() instanceof BuiltinType && tc == Bool;
  }

  public boolean isCharType() {
    return canonicalType.getType() instanceof BuiltinType
        && (tc == SChar || tc == Char_U);
  }

  public boolean isIntegralType() {
    return isIntegerType();
  }

  /**
   * // C99 6.2.5p10 (float, double, long double)
   *
   * @return
   */
  public boolean isRealFloatingType() {
    return canonicalType.getType() instanceof BuiltinType
        && tc >= Float && tc <= LongDouble;
  }

  public boolean isComplexType() {
    if (canonicalType.getType() instanceof ComplexType)
      return ((ComplexType) canonicalType.getType()).getElementType().isFloatingType();
    return false;
  }

  public boolean isComplexIntegerType() {
    // Check for GCC complex integer extension.
    if (canonicalType.getType() instanceof ComplexType)
      return ((ComplexType) canonicalType.getType()).getElementType().isIntegerType();
    return false;
  }

  public boolean isAnyComplexType() {
    return canonicalType.getUnQualifiedType().getType() instanceof ComplexType;
  }

  public boolean isFloatingType() {
    if (canonicalType.getType() instanceof BuiltinType)
      return tc >= Float && tc <= LongDouble;
    return isComplexType();
  }

  /**
   * C99 6.2.5p17 (real floating + integer)
   *
   * @return
   */
  public boolean isRealType() {
    return isFloatingType() || isIntegerType();
  }

  /**
   * C99 6.2.5p18 (integer + floating)
   *
   * @return
   */
  public boolean isArithmeticType() {
    return isRealType() || canonicalType.getType() instanceof ComplexType;
  }

  public boolean isVoidType() {
    return canonicalType.getType() instanceof BuiltinType
        && tc == Void;
  }

  public boolean isDerivedType() {
    switch (canonicalType.getTypeClass()) {
      case Pointer:
      case VariableArray:
      case ConstantArray:
      case ConstantArrayWithExpr:
      case ConstantArrayWithoutExpr:
      case IncompleteArray:
      case FunctionProto:
      case FunctionNoProto:
      case Struct:
        return true;
      default:
        return false;
    }
  }

  public boolean isScalarType() {
    if (canonicalType.getType() instanceof BuiltinType)
      return tc != Void;
    if (canonicalType.getType() instanceof TagType) {
      TagType tt = (TagType) canonicalType.getType();
      if (tt.getDecl().isCompleteDefinition() && tt.getDecl().isEnum())
        return true;
    }
    return canonicalType.getType() instanceof PointerType;
  }

  public boolean isAggregateType() {
    Type t = canonicalType.getType();
    return t instanceof RecordType || t instanceof ArrayType;
  }

  public boolean isFunctionType() {
    return canonicalType.getUnQualifiedType().getType() instanceof FunctionType;
  }

  public boolean isFunctionNoProtoType() {
    return getAsFunctionNoProtoType() != null;
  }

  public boolean isFunctionProtoType() {
    return getAsFunctionProtoType() != null;
  }

  public boolean isPointerType() {
    return canonicalType.getUnQualifiedType().getType() instanceof PointerType;
  }

  public boolean isVoidPointerType() {
    PointerType pt = getAsPointerType();
    if (pt != null)
      return pt.getPointeeType().isVoidType();
    return false;
  }

  public boolean isFunctionPointerType() {
    PointerType pt = getAsPointerType();
    if (pt != null)
      return pt.getPointeeType().isFunctionType();
    return false;
  }

  public boolean isArrayType() {
    return canonicalType.getUnQualifiedType().getType() instanceof ArrayType;
  }

  public boolean isConstantArrayType() {
    return canonicalType.getUnQualifiedType().getType() instanceof ConstantArrayType;
  }

  public boolean isIncompleteArrayType() {
    return canonicalType.getUnQualifiedType().getType() instanceof IncompleteArrayType;
  }

  public boolean isVariableArrayType() {
    return canonicalType.getUnQualifiedType().getType() instanceof VariableArrayType;
  }

  public boolean isRecordType() {
    return canonicalType.getUnQualifiedType().getType() instanceof RecordType;
  }

  public boolean isVectorType() {
    return canonicalType.getUnQualifiedType().getType() instanceof VectorType;
  }

  // Type Checking Functions: Check to see if this type is structurally the
  // specified type, ignoring typedefs and qualifiers, and return a pointer to
  // the best type we can.
  public BuiltinType getAsBuiltinType() {
    // If this is directly a builtin type, return it.
    if (this instanceof BuiltinType)
      return (BuiltinType) this;

    QualType ty = getDesugaredType();
    if (ty.getType().canonicalType.getType() instanceof BuiltinType)
      return (BuiltinType) ty.getType().canonicalType.getType();
    return null;
  }

  public FunctionType getAsFunctionType() {
    // If this is directly a builtin type, return it.
    if (this instanceof FunctionType)
      return (FunctionType) this;

    if (!(canonicalType.getType() instanceof FunctionType)) {
      if (canonicalType.getUnQualifiedType().getType() instanceof FunctionType)
        return canonicalType.getUnQualifiedType().getAsFunctionType();
      return null;
    }

    QualType ty = getDesugaredType();
    return (FunctionType) ty.getType();
  }

  public FunctionNoProtoType getAsFunctionNoProtoType() {
    FunctionType ft = getAsFunctionType();
    return ft != null && ft instanceof FunctionNoProtoType ?
        (FunctionNoProtoType) ft : null;
  }

  public FunctionProtoType getAsFunctionProtoType() {
    FunctionType ft = getAsFunctionType();
    return ft != null && ft instanceof FunctionProtoType ?
        (FunctionProtoType) ft : null;
  }

  public RecordType getAsStructureType() {
    if (this instanceof RecordType)
      return (RecordType) this;

    if (canonicalType.getType() instanceof RecordType) {
      RecordType rt = (RecordType) canonicalType.getType();
      if (!rt.getDecl().isStruct())
        return null;

      Type ty = getDesugaredType().getType();
      return ty instanceof RecordType ? (RecordType) ty : null;
    }

    // Look through type qualifiers.
    if (canonicalType.getUnQualifiedType().getType() instanceof RecordType)
      return canonicalType.getUnQualifiedType().getType().getAsStructureType();
    return null;
  }

  public RecordType getAsUnionType() {
    if (this instanceof RecordType)
      return (RecordType) this;

    if (canonicalType.getType() instanceof RecordType) {
      RecordType rt = (RecordType) canonicalType.getType();
      if (!rt.getDecl().isUnion())
        return null;

      Type ty = getDesugaredType().getType();
      return ty instanceof RecordType ? (RecordType) ty : null;
    }

    // Look through type qualifiers.
    if (canonicalType.getUnQualifiedType().getType() instanceof RecordType)
      return canonicalType.getUnQualifiedType().getType().getAsUnionType();
    return null;
  }

  public EnumType getAsEnumType() {
    Type t = canonicalType.getUnQualifiedType().getType();
    return t instanceof EnumType ? (EnumType) t : null;
  }

  public TypedefType getAsTypedefType() {
    return this instanceof TypedefType ? (TypedefType) this : null;
  }

  public ComplexType getAsComplexType() {
    if (this instanceof ComplexType)
      return (ComplexType) this;

    if (!(canonicalType.getType() instanceof ComplexType)) {
      return null;
    }

    // Look through type qualifiers.
    Type ty = getDesugaredType().getType();
    return ty instanceof ComplexType ? (ComplexType) ty : null;
  }

  public ComplexType getAsComplexIntegerType() {
    if (this instanceof ComplexType) {
      ComplexType ct = (ComplexType) this;
      if (ct.getElementType().isIntegerType())
        return ct;
      return null;
    }

    if (!(canonicalType.getType() instanceof ComplexType)) {
      return null;
    }

    // If this is a typedef for a complex type, strip the typedef off without
    // losing all typedef information.
    Type ty = getDesugaredType().getType();
    return ty instanceof ComplexType ? (ComplexType) ty : null;
  }

  public PointerType getAsPointerType() {
    if (this instanceof PointerType)
      return (PointerType) this;

    if (!(canonicalType.getType() instanceof PointerType)) {
      return null;
    }

    // If this is a typedef for a complex type, strip the typedef off without
    // losing all typedef information.
    Type ty = getDesugaredType().getType();
    return ty instanceof PointerType ? (PointerType) ty : null;
  }

  public RecordType getAsRecordType() {
    if (this instanceof RecordType)
      return (RecordType) this;

    if (!(canonicalType.getType() instanceof RecordType))
      return null;

    Type ty = getDesugaredType().getType();
    return ty instanceof RecordType ? (RecordType) ty : null;
  }

  public TagType getAsTagType() {
    if (this instanceof TagType)
      return (TagType) this;

    if (!(canonicalType.getType() instanceof TagType))
      return null;

    Type ty = getDesugaredType().getType();
    return ty instanceof TagType ? (TagType) ty : null;
  }

  public QualType getPointeeType() {
    PointerType pt = getAsPointerType();
    if (pt != null)
      return pt.getPointeeType();
    return new QualType();
  }

  /**
   * More type predicates useful for type checking/promotion.
   *
   * @return
   */
  public boolean isPromotableIntegerType() {
    BuiltinType bt = getAsBuiltinType();
    if (bt != null && tc >= Bool && tc <= UShort) {
      return true;
    }
    return false;
  }

  /**
   * Return true if this is an integer type that is
   * /// signed, according to C99 6.2.5p4 [char, signed char, short, int, long..],
   * /// an enum decl which has a signed representation
   *
   * @return
   */
  public boolean isSignedIntegerType() {
    if (isBuiltinType() && tc >= TypeClass.SChar && tc <= LongLong)
      return true;
    EnumType et = getAsEnumType();
    if (et != null)
      return et.getDecl().getIntegerType().isSignedIntegerType();
    ;

    return false;
  }

  public boolean isUnsignedIntegerType() {
    return (isBuiltinType() && tc >= TypeClass.Bool &&
        tc <= TypeClass.ULongLong);
  }

  public boolean isConstantSizeType() {
    Util.assertion(!isIncompleteType(), "This is not make sense for incomplete type");
    return !(canonicalType.getType() instanceof VariableArrayType);
  }

  public void dump() {
    String str = "identifier";
    LangOptions opts = new LangOptions();
    System.err.println(getAsStringInternal(str, new PrintingPolicy(opts)));
  }

  public abstract String getAsStringInternal(String inner, PrintingPolicy policy);

  public boolean isSpecifierType() {
    // Note that this intentionally does not use the canonical type.
    switch (getTypeClass()) {
      case Struct:
      case Enum:
      case TypeDef:
      case Complex:
        return true;
      default:
        if (tc >= BuiltinTypeBegin && tc < BuiltinTypeEnd)
          return true;
        return false;
    }
  }

  public QualType getCanonicalTypeInternal() {
    return canonicalType;
  }

  public boolean isIntegralOrEnumerationType() {
    return isIntegerType() || isEnumeralType();
  }

  public ScalarTypeKind getScalarTypeKind() {
    Util.assertion(isScalarType());

    switch (getTypeClass()) {
      case Pointer:
        return ScalarTypeKind.STK_CPointer;
      case Bool:
        return STK_Bool;
    }
    if (isIntegerType())
      return STK_Integral;
    if (isFloatingType())
      return STK_Floating;
    if (isEnumeralType()) {
      Util.assertion(getAsEnumType().getDecl().isCompleteDefinition());
      return STK_Integral;
    }
    if (isComplexType()) {
      ComplexType ct = getAsComplexType();
      if (ct.getElementType().isFloatingType())
        return STK_FloatingComplex;
      return STK_IntegralComplex;
    }

    Util.shouldNotReachHere("Unknown scalar type");
    return null;
  }
}
