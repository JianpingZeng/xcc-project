package backend.analysis;

import backend.target.TargetData;
import backend.value.Value;
import jlang.support.APInt;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class ValueTracking
{

    public static void computeMaskedBits(Value val, APInt mask,
            APInt knownZero, APInt knownOne, TargetData td)
    {
        computeMaskedBits(val, mask, knownZero, knownOne, td, 0);
    }

    public static void computeMaskedBits(Value val, APInt mask,
            APInt knownZero, APInt knownOne, TargetData td, int depth)
    {

    }

    public static int computeNumSignBits(Value val, TargetData td)
    {
        return computeNumSignBits(val, td, 0);
    }

    public static int computeNumSignBits(Value val, TargetData td, int depth)
    {
        return 0;
    }
}
