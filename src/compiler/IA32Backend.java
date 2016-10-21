package compiler; 

import asm.AbstractAssembler;
import asm.ia32.IA32Assembler;
import lir.LIRAssembler;
import lir.LIRGenerator;
import lir.backend.Architecture;
import lir.backend.MachineInfo;
import lir.backend.RegisterConfig;
import lir.backend.TargetFunctionAssembler;
import lir.backend.ia32.IA32LIRAssembler;
import utils.Context;

/** 
 * @author Xlous.zeng
 * @version 0.1
 */
public final class IA32Backend extends Backend 
{

    public IA32Backend(Context context, MachineInfo machineInfo,
            RegisterConfig registerConfig)
    {
        super(context, machineInfo, registerConfig);
    }

    @Override
    public LIRAssembler newLIRAssember(Backend backend,
            TargetFunctionAssembler tasm)
    {
        return new IA32LIRAssembler(backend, tasm);
    }
    
    @Override
    public LIRGenerator newLIRGenerator(Architecture arch, Backend backend)
    {
        return LIRGenerator.create(arch, backend);
    }

    @Override
    public AbstractAssembler newAssember(RegisterConfig registerConfig)
    {
        return new IA32Assembler(machineInfo, registerConfig);
    }
    
}
