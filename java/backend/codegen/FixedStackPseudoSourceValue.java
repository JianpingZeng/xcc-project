package backend.codegen;

import java.io.PrintStream;

/**
 * A specialized PseudoSourceValue for holding FixedStack values, which must
 * include a frame index.
 * @author Xlous.zeng
 * @version 0.1
 */
public class FixedStackPseudoSourceValue extends PseudoSourceValue
{
    private int frameIndex;
    public FixedStackPseudoSourceValue(int fi)
    {
        frameIndex = fi;
    }

    @Override
    public boolean isConstant()
    {
        return super.isConstant();
    }

    @Override
    public void print(PrintStream os)
    {
        super.print(os);
    }
}
