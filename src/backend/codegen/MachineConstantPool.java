package backend.codegen;

import backend.value.Constant;
import backend.value.ConstantFP;

import java.util.ArrayList;

/**
 * Keeps track of information for constant spilled into stack frame.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class MachineConstantPool
{
    private ArrayList<Constant> constantPool;

    /**
     * Creates a new entry in constant pool and returns it's index
     * or a existing one if there is existing.
     * @param c
     * @return
     */
    public int getConstantPoolIndex(Constant c)
    {
        for (int i = 0, e = constantPool.size(); i<e; i++)
            if (constantPool.get(i) == c)
                return i;

        constantPool.add(c);
        return constantPool.size()-1;
    }

    public ArrayList<Constant> getConstantPool() {return constantPool;}
}
