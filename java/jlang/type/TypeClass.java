package jlang.type;

/**
 * An interfaces for jlang.type tc VALUES, which distinguish between
 * different sorts of types.
 * 
 * @author Xlous.zeng  
 * @version 1.0
 */
public interface TypeClass
{
    int BuiltinTypeBegin = 1;

    int Void = BuiltinTypeBegin;
    int Bool = Void + 1;        // This is bool and/or _Bool
    int UChar = Bool + 1;       // This is 'char' for targets where char is unsigned.
    int UShort = UChar + 1;
    int UInt = UShort + 1;
    int ULong = UInt + 1;
    int ULongLong = ULong + 1;
    int UInt128 = ULongLong + 1;      // __int128_t

    int Char_S = UInt128 + 1;   // This is 'char' for target where char is signed.
    int SChar = UInt128 + 1;    // This is explicitly qualified signed char.
    int Short = SChar + 1;      //
    int Int = Short + 1;
    int Long = Int + 1;
    int LongLong = Long + 1;
    int Int128 = LongLong + 1;      // // __uint128_t
    int Float = Int128 + 1;
    int Double = Float + 1;
    int LongDouble = Double + 1;
    int BuiltinTypeEnd = LongDouble + 1;

    int Complex = LongDouble + 1;
    int Enumerate = Complex + 1;
    int Pointer = Enumerate + 1;
    int Reference = Pointer + 1;
    int FunctionProto = Reference + 1;
    int FunctionNoProto = FunctionProto + 1;
    int Method = FunctionNoProto + 1;
    int ConstantArray = Method + 1;
    int ConstantArrayWithExpr = ConstantArray + 1;
    int ConstantArrayWithoutExpr = ConstantArrayWithExpr + 1;
    int IncompleteArray = ConstantArrayWithoutExpr + 1;
    int VariableArray = IncompleteArray + 1;
    int Struct = VariableArray + 1;
    int Union = Struct + 1;
    int Enum = Union + 1;
    int TypeDef = Enum + 1;
    /**
     * The tc of the missing jlang.type.
     */
    int None = TypeDef + 1;

    int Label = None + 1;
    
    /**
     * The tc of the error jlang.type.
     */
    int Error = None + 1;
}
