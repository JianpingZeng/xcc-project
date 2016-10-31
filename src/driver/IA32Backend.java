package driver;

import backend.asm.AbstractAssembler;
import backend.asm.ia32.IA32Assembler;
import backend.lir.LIRAssembler;
import backend.lir.LIRGenerator;
import backend.lir.backend.Architecture;
import backend.lir.backend.MachineInfo;
import backend.lir.backend.RegisterConfig;
import backend.lir.backend.TargetFunctionAssembler;
import backend.lir.backend.ia32.IA32LIRAssembler;
import tools.Context;

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
