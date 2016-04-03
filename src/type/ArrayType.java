package type;

import symbol.Symbol.TypeSymbol;


/**
 * This class represents the array type that extends from super class {@link Type}
 * 
 * @author JianpingZeng
 * @version 1.0
 */
public class ArrayType extends Type {
	
	/**
	 * The type of element stored at this array.
	 */
    private Type elemtype;
    
    /**
     * The lenght of this array.
     */
    private long length;   

    /**
     * Constructs a instance with given element type and TypeSymbol.
     * 
     */
    public ArrayType(Type elemType, TypeSymbol arrayClass) {
        super(ARRAY, arrayClass);
    	this.elemtype = elemType;
    }

    public long length() {
        return length;
    }

    public Type elemType()
    {
    	return elemtype;
    }
    
    public boolean equals(Object that) 
    {
    	return this == that || (that instanceof ArrayType
    			 && this.elemtype.equals(((ArrayType) that).elemtype)
    			 && this.length == ((ArrayType)that).length);
    }
    
    public int hashCode()
    {
    	return (int) ((ARRAY << 11) + (this.length << 5) + elemtype.hashCode());
    }
    
    public int dimensions()
    {
    	int result = 0;
    	for (Type t = this; t.tag == ARRAY; t = t.elemType())
    		result++;
    	return result;
    }

    /**
     * Is this type a same type as type other?
     */
    public boolean isSameType(Type other) {
        // length is not important
    	if (this == other)
    		return true;
    
        return ( other.tag == ARRAY && elemtype.isSameType(other.elemType()));
    }

    public boolean isSubType(Type that) {
        if (this == that)
            return true;
       
        if (that.tag == ARRAY) {
            if (this.elemtype.tag <= lastBaseTag)
                return this.elemtype.isSameType(that.elemType());
            else
                return this.elemtype.isSubType(that.elemType());
        } else {
            return false;
        }
    }

    public boolean isCastable(Type that) {
        return that.tag == ERROR ||
                that.tag == ARRAY && (this.elemType().tag <= lastBaseTag ?
                this.elemType().tag == that.elemType().tag :
                this.elemType().isCastable(that.elemType()));
    }   

    public String toString() {
        if (length < 0) {
            return elemtype.toString() + "[]";
        }
        else {
            return elemtype.toString() + "[" + length + "]";
        }
    }
}
