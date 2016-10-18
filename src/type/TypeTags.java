package type;

import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const;

/**
 * An interfaces for type tag VALUES, which distinguish between 
 * different sorts of types.
 * 
 * @author Xlous.zeng  
 * @version 1.0
 */
public interface TypeTags
{

    int VOID = 1;
    int BOOLEAN = VOID + 1;
    int UCHAR = BOOLEAN + 1;
    int USHORT = UCHAR + 1;
    int UINT = USHORT + 1;
    int ULONG = UINT + 1;

    int CHAR = ULONG + 1;
    int SHORT = CHAR + 1;
    int INT = SHORT + 1;
    int LONG = INT + 1;

    int REAL = LONG + 1;
    int COMPLEX = REAL + 1;
    int ENUMERATE = COMPLEX + 1;
    int POINTER = ENUMERATE + 1;
    int REFERENCE = POINTER + 1;
    int FUNCTION = REFERENCE + 1;
    int METHOD = FUNCTION + 1;
    int ConstantArray = METHOD + 1;
    int IncompleteArray = ConstantArray + 1;
    int VariableArray = IncompleteArray + 1;
    int STRUCT = VariableArray + 1;
    int UNION = STRUCT + 1;
    int ENUM = UNION + 1;
    int USER_DEF = ENUM + 1;
    /**
     * The tag of the missing type.
     */
    int NONE = USER_DEF + 1;
    
    /**
     * The tag of the error type.
     */
    int ERROR = NONE + 1;
}
