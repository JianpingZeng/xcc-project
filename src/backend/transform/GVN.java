package backend.transform;

import backend.pass.FunctionPass;
import backend.value.Function;

/**
 * An internal class for global value numbering which is desired when performing
 * global common subexpression elimination.
 * 
 * @author xlous.zeng
 * @version 0.1
 */
public final class GVN extends FunctionPass
{
    @Override
    public String getPassName()
    {
        return "Global value numbering";
    }

    @Override
    public boolean runOnFunction(Function f)
    {
        return false;
    }
}
