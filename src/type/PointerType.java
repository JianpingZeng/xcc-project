package type;

import symbol.TypeSymbol;

/**
 *
 * @author JianpingZeng
 *
 */
public class PointerType extends Type 
{
    protected long size;
    /**
     * The basic type of this pointer type.
     */
    protected Type baseType;

    public PointerType(long size, Type baseType, TypeSymbol pointerClass) {
        super(POINTER, pointerClass);
    	this.size = size;
        this.baseType = baseType;
    } 

    public long size() {
        return size;
    }

    public Type baseType() {
        return baseType;
    }

    public boolean equals(Object other) {
        if (this == other) return true;
        if ( !(other instanceof PointerType)) return false;
            	
        return baseType.equals(((PointerType)other).baseType);
       
    }

    public int hashCode()
    {
    	return (int) (size << 5 + baseType.hashCode());
    }
    
    public boolean isSameType(Type other) 
    {
    	if (this == other) return true;
    	        
        return  other.tag == POINTER && isSameType(((PointerType)other).baseType);
    }

    public boolean isCastableTo(Type other) {
        return  other.tag == POINTER || other.tag == INT;
    }

    public String toString() {
        return baseType.toString() + "*";
    }
}
