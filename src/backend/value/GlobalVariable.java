package backend.value;

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
    public GlobalVariable(Type ty,
            boolean isConstant,
            Constant init,
            String name)
    {
        super(PointerType.get(ty), ValueKind.GlobalVariableVal,
           name);
        isConstantGlobal = isConstant;
        if (init != null)
        {
            reserve(1);
            assert init.getType() == ty:"Initializer should be the same type as the GlobalVariable!";
            setOperand(0, init);
        }
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
    public boolean isNullValue()
    {
        return false;
    }

    public void setInitializer(Constant init)
    {
        setOperand(0, init);
    }
}
