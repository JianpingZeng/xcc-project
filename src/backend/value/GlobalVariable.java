package backend.value;

import backend.hir.Module;
import backend.type.PointerType;
import backend.type.Type;

/**
 * @author Xlous.zeng  
 */
public class GlobalVariable extends GlobalValue
{
    /**
     * Is this a global constant.
     */
    private boolean isConstantGlobal;
    /**
     * Constructs a new instruction representing the specified constant.
     *
     */
    public GlobalVariable(Module m,
            Type ty,
            boolean isConstant,
            LinkageType linkage,
            Constant init,
            String name)
    {
        super(PointerType.get(ty),
                ValueKind.GlobalVariableVal,
                linkage,
                name);
        isConstantGlobal = isConstant;
        if (init != null)
        {
            reserve(1);
            assert init.getType() == ty:"Initializer should be the same type as the GlobalVariable!";
            setOperand(0, new Use(init, this));
        }
        if (m != null)
            m.getGlobalVariableList().add(this);
    }

    /**
     * This method unlinks 'this' from the containing module
     * and deletes it.
     */
    @Override
    public void eraseFromParent()
    {
        parent.getGlobalVariableList().remove(this);
    }

    @Override
    public boolean isNullValue(){return false;}

    public void setInitializer(Constant init)
    {
        setOperand(0, new Use(init, this));
    }
}
