package type;

/**
 * An interfaces for type tag VALUES, which distinguish between 
 * different sorts of types.
 * 
 * @author Jianping Zeng <z1215jping@hotmail.com>
 * @version 1.0
 */
public interface TypeTags {

    /**
     * The tag of the basic type `byte'.
     */
    int BYTE = 1;

    /**
     * The tag of the basic type `char'.
     */
    int CHAR = 2;

    /**
     * The tag of the basic type `short'.
     */
    int SHORT = 3;

    /**
     * The tag of the basic type `int'.
     */
    int INT = 4;

    /**
     * The tag of the basic type `long'.
     */
    int LONG = 5;

    /**
     * The tag of the basic type `float'.
     */
    int FLOAT = 6;

    /**
     * The tag of the basic type `double'.
     */
    int DOUBLE = 7;

    //int STRING = 8;
    
    /**
     * The tag of the basic type `boolean'.
     */
    int BOOL = 9;

    /**
     * The tag of the type `void'.
     */
    int VOID = 10;    

    /**
     * The tag of the type `array'.
     */
    int ARRAY = 11;
    
    /**
     * The tag of the type `struct'.
     */
    int STRUCT = 12;

    /**
     * The tag of the type `union'.
     */
    int UNION = 13;
    
    /**
     * The tag of the type `enum'.
     */
    int ENUM = 14;    

    /**
     * The tag of the type `method'.
     */
    int FUNCTION = 15;

    /**
     * The tag of the missing type.
     */
    int NONE = 16;
    
    /**
     * The tag of the error type.
     */
    int ERROR = 17;

    /**
     * The tag of the unknowed type.
     */
    int UNKNOWN = 18;
    
    /**
     * The tag of the pointer type.
     */
    int POINTER = 19;
    
    /**
     * The id of type tags.
     */
    int TypeTagCount = POINTER;
    
    /**
     * The maximum tag of a basic type.
     */
    int lastBaseTag = BOOL;
    
}
