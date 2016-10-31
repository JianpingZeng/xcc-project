package backend.lir.backend.ia32;

import driver.Backend;
import backend.lir.LIRAssembler;
import backend.lir.backend.TargetFunctionAssembler;

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
