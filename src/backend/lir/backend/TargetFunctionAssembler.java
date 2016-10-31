package backend.lir.backend;

import backend.asm.AbstractAssembler;

/** 
 * @author Xlous.zeng
 * @version 0.1
 */
public class TargetFunctionAssembler
{
    public final AbstractAssembler asm;
    public final TargetFunction function;
    
    public TargetFunctionAssembler(AbstractAssembler asm)
    {
        this.asm = asm;
        function = new TargetFunction();
    }
    
    public void setFrameSize(int frameSize)
    {}
    
    public void finishTargetFunction()
    {
        
    }
}
