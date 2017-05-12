package jlang.type;

/**
 * An interfaces for jlang.type tag VALUES, which distinguish between
 * different sorts of types.
 * 
 * @author Xlous.zeng  
 * @version 1.0
 */
public interface TypeClass
{
    int BuiltinTypeBegin = 1;

    int Void = BuiltinTypeBegin;
    int Bool = Void + 1;
    int UnsignedChar = Bool + 1;
    int UnsignedShort = UnsignedChar + 1;
    int UnsignedInt = UnsignedShort + 1;
    int UnsignedLong = UnsignedInt + 1;
    int UnsignedLongLong = UnsignedLong + 1;

    int Char = UnsignedLongLong + 1;
    int Short = Char + 1;
    int Int = Short + 1;
    int LongInteger = Int + 1;
    int LongLong = LongInteger + 1;
    int Float = LongLong + 1;
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
    int IncompleteArray = ConstantArray + 1;
    int VariableArray = IncompleteArray + 1;
    int Struct = VariableArray + 1;
    int Union = Struct + 1;
    int Enum = Union + 1;
    int TypeDef = Enum + 1;
    /**
     * The tag of the missing jlang.type.
     */
    int None = TypeDef + 1;

    int Label = None + 1;
    
    /**
     * The tag of the error jlang.type.
     */
    int Error = None + 1;
}
