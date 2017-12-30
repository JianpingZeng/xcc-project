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
    private boolean isThreadLocal;

    /**
     * Constructs a new instruction representing the specified constant.
     *
     */
    public GlobalVariable(Module m,
            Type ty,
            boolean isConstant,
            LinkageType linkage,
            Constant init,
            String name,
            GlobalVariable before,
            int addressSpace)
    {
        super(PointerType.get(ty, addressSpace),
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
        if (before != null)
        {
            int beforeIdx = before.getParent().getGlobalVariableList().indexOf(before);
            before.getParent().getGlobalVariableList().add(beforeIdx+1, this);
        }
        else
        {
            m.addGlobalVariable(this);
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
    public boolean isNullValue(){return false;}

    @Override
    public void replaceUsesOfWithOnConstant(Value from, Value to, Use u)
    {
        assert getNumOfOperands() == 1:
                "Attempt to replace uses of Constants on a GVar with no initializer";
        assert operand(0).equals(from):
                "Attempt to replace wrong constant initializer in GVar";
        assert to instanceof Constant:
                "Attempt to replace GVar initializer with non-constant";
        setOperand(0, (Constant)to);
    }

    /**
     * Return true if the primary definition of this global value is
     * outside of the current translation unit.
     *
     * @return
     */
    @Override
    public boolean isExternal()
    {
        return operand(0) == null;
    }

    public void setInitializer(Constant init)
    {
        setOperand(0, new Use(init, this));
    }
    public boolean isConstant() {return isConstantGlobal;}
    public void setConstant(boolean c) {isConstantGlobal = c;}

    public boolean hasInitializer()
    {
        return !isExternal();
    }

    @Override
    public GlobalVariable clone()
    {
        return (GlobalVariable) super.clone();
    }

    public boolean isThreadLocal()
    {
        return isThreadLocal;
    }

    public Constant getInitializer()
    {
        assert hasInitializer();
        return (Constant) operand(0);
    }

    public void setThreadLocal(boolean threadLocal)
    {
        this.isThreadLocal = threadLocal;
    }
}
