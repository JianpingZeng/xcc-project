package frontend.type;

/**
 * An interfaces for frontend.type tag VALUES, which distinguish between
 * different sorts of types.
 * 
 * @author Xlous.zeng  
 * @version 1.0
 */
public interface TypeClass
{

    int Void = 1;
    int Bool = Void + 1;
    int UnsignedChar = Bool + 1;
    int UnsignedShort = UnsignedChar + 1;
    int UnsignedInt = UnsignedShort + 1;
    int UnsignedLong = UnsignedInt + 1;

    int Char = UnsignedLong + 1;
    int Short = Char + 1;
    int Int = Short + 1;
    int LongInteger = Int + 1;

    int Real = LongInteger + 1;
    int Complex = Real + 1;
    int Enumerate = Complex + 1;
    int Pointer = Enumerate + 1;
    int Reference = Pointer + 1;
    int Function = Reference + 1;
    int Method = Function + 1;
    int ConstantArray = Method + 1;
    int IncompleteArray = ConstantArray + 1;
    int VariableArray = IncompleteArray + 1;
    int Struct = VariableArray + 1;
    int Union = Struct + 1;
    int Enum = Union + 1;
    int TypeDef = Enum + 1;
    /**
     * The tag of the missing frontend.type.
     */
    int None = TypeDef + 1;

    int Label = None + 1;
    
    /**
     * The tag of the error frontend.type.
     */
    int Error = None + 1;
}
