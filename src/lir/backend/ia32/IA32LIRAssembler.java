package lir.backend.ia32; 

import compiler.Backend;
import lir.LIRAssembler;
import lir.backend.TargetFunctionAssembler;

/** 
 * @author Xlous.zeng
 * @version 0.1
 */
public final class IA32LIRAssembler extends LIRAssembler
{
    public IA32LIRAssembler(Backend backend, TargetFunctionAssembler tasm)
    {
        super(backend, tasm);
    }
}
