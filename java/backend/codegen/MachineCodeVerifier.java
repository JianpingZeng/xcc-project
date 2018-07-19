package backend.codegen;

import backend.support.MachineFunctionPass;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class MachineCodeVerifier extends MachineFunctionPass
{
    public static MachineCodeVerifier createMachineVerifierPass(boolean allowDoubleDefs)
    {
        return new MachineCodeVerifier(allowDoubleDefs);
    }

    private MachineCodeVerifier(boolean allowDoubleDefs)
    {

    }

    @Override
    public boolean runOnMachineFunction(MachineFunction mf)
    {
        return false;
    }

    @Override
    public String getPassName()
    {
        return null;
    }


}
