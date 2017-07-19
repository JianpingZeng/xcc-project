package backend.codegen;

import backend.target.TargetData;
import backend.value.Constant;

import java.util.ArrayList;

/**
 * Keeps track of information for constant spilled into stack frame.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class MachineConstantPool
{
    private ArrayList<MachineConstantPoolEntry> constantPool;
    private TargetData td;
    private int poolAlignment;

    public MachineConstantPool(TargetData td)
    {
        this.td = td;
        poolAlignment = 1;
    }
    /**
     * Creates a new entry in constant pool and returns it's index
     * or a existing one if there is existing.
     * @param c
     * @return
     */
    public int getConstantPoolIndex(Constant c, int align)
    {
        assert align != 0 :"Alignment must be specified.";
        if (align > poolAlignment)
            poolAlignment = align;

        for (int i = 0, e = constantPool.size(); i<e; i++)
        {
            MachineConstantPoolEntry cc = constantPool.get(i);
            if (cc == c && (cc.getAlignment()
                    &))
                return i;
        }
        constantPool.add(c);
        return constantPool.size()-1;
    }

    public ArrayList<Constant> getConstantPool() {return constantPool;}

    public int getContantPoolAlignment()
    {
        return poolAlignment;
    }

    public boolean isEmpty() {return constantPool.isEmpty();}
}
