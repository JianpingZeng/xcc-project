package backend.codegen;

import backend.support.FormattedOutputStream;
import backend.support.LLVMContext;
import backend.value.Value;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.PrintStream;

import static backend.value.ValueKind.PseudoSourceValueVal;

/**
 * Special value supplied for machine level alias
 * analysis. It indicates that the a memory access references the functions
 * stack frame (e.g., a spill slot), below the stack frame (e.g., argument
 * space), or constant pool.
 * @author Xlous.zeng
 * @version 0.1
 */
public class PseudoSourceValue extends Value
{
    public PseudoSourceValue()
    {
        super(LLVMContext.Int8Ty, PseudoSourceValueVal);
    }

    @Override
    public void dump()
    {
        print(System.err);
        System.err.println();
    }

    @Override
    public void print(PrintStream os)
    {
        FormattedOutputStream out = new FormattedOutputStream(os);
        print(out);
    }
    public static final String PSVNames[] =
            {
                "Stack",
                "GOT",
                "JumpTable",
                "ConstantPool"
            };

    @Override
    public void print(FormattedOutputStream os)
    {
        os.printf("XX");
    }

    @Override
    public boolean isConstant()
    {
        return super.isConstant();
    }

    private static TIntObjectHashMap<PseudoSourceValue> fsValueMap = new TIntObjectHashMap<>();

    /**
     * A pseudo source value referencing a fixed stack frame entry,
     * e.g., a spill slot.
     * @param frameIndex
     * @return
     */
    public static PseudoSourceValue getFixedStack(int frameIndex)
    {
        PseudoSourceValue val;
        if (fsValueMap.containsKey(frameIndex))
        {
            val = fsValueMap.get(frameIndex);
        }
        else
        {
            val = new FixedStackPseudoSourceValue(frameIndex);
            fsValueMap.put(frameIndex, val);
        }
        return val;
    }

    /**
     * A source value referencing the area below the stack frame of a function,
     * e.g., the argument space.
     * @return
     */
    public static PseudoSourceValue getStack()
    {
        if (!fsValueMap.containsKey(0))
        {
            fsValueMap.put(0, new PseudoSourceValue());
        }
        return fsValueMap.get(0);
    }

    /**
     * A source value referencing the global offset table (or something the
     * like).
     * @return
     */
    public static PseudoSourceValue getGOT()
    {
        if (!fsValueMap.containsKey(1))
        {
            fsValueMap.put(0, new PseudoSourceValue());
        }
        return fsValueMap.get(1);
    }

    /**
     * A reference to Constant pool.
     * @return
     */
    public static PseudoSourceValue getConstantPool()
    {
        if (!fsValueMap.containsKey(2))
        {
            fsValueMap.put(0, new PseudoSourceValue());
        }
        return fsValueMap.get(2);
    }

    /**
     * A reference to Jump table.
     * @return
     */
    public static PseudoSourceValue getJumpTable()
    {
        if (!fsValueMap.containsKey(3))
        {
            fsValueMap.put(0, new PseudoSourceValue());
        }
        return fsValueMap.get(3);
    }

}
