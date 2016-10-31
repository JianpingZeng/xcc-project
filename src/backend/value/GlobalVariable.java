package backend.value;

import backend.lir.ci.LIRConstant;

/**
 * @author Xlous.zeng  
 */
public class GlobalVariable extends GlobalValue
{
    /**
     * Constructs a new instruction representing the specified constant.
     *
     * @param value
     */
    public GlobalVariable(LIRConstant value)
    {
        super(value);
    }
}
