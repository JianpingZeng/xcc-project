package backend.value;

import backend.codegen.MachineFunction;
import backend.support.AttrList;
import backend.support.Attribute;
import backend.support.CallingConv;
import backend.support.ValueSymbolTable;
import backend.type.FunctionType;
import backend.type.PointerType;
import backend.type.Type;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * This class is representation at the Module(high-level IR) of a function or method.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public class Function extends GlobalValue implements Iterable<BasicBlock>
{
    /**
     * For the function return value, it is null iff there is no return value
     * of this function.
     */
    public Instruction.AllocaInst ReturnValue;

    private ArrayList<Argument> argumentList;

    private LinkedList<BasicBlock> basicBlockList;

    private MachineFunction mf;
    private CallingConv cc;

    private ValueSymbolTable symTab;

    private AttrList attributeList;

    public Function(FunctionType ty,
            LinkageType linkage,
            String name,
            Module parentModule)
    {
        super(PointerType.getUnqual(ty), ValueKind.FunctionVal, linkage, name);
        argumentList = new ArrayList<>();

        if (parentModule != null)
            parentModule.addFunction(this);

        symTab = new ValueSymbolTable();
        for (int i = 0, e = ty.getNumParams(); i < e; i++)
        {
            Type t = ty.getParamType(i);
            assert !t.isVoidType() : "Can't have void typed argument!";
            Argument arg = new Argument(t);
            argumentList.add(arg);
            arg.setParent(this);
        }
        basicBlockList = new LinkedList<>();
    }

    public Type getReturnType()
    {
        return getFunctionType().getReturnType();
    }

    public FunctionType getFunctionType()
    {
        return (FunctionType) super.getType().getElementType();
    }

    public boolean isVarArg()
    {
        return getFunctionType().isVarArg();
    }

    /**
     * This method unlinks 'this' from the containing module
     * and deletes it.
     */
    @Override
    public void eraseFromParent()
    {
        getParent().getFunctionList().remove(this);
    }

    public Module getParent()
    {
        return parent;
    }

    public ArrayList<Argument> getArgumentList()
    {
        return argumentList;
    }

    /**
     * Gets the entry block of the CFG of this function.
     */
    public BasicBlock getEntryBlock()
    {
        return basicBlockList.getFirst();
    }

    public void appendBB(BasicBlock bb)
    {
        assert bb != null;
        basicBlockList.addLast(bb);
        bb.setParent(this);
    }

    /**
     * Returns an iterator over elements of jlang.type {@code T}.
     *
     * @return an Iterator.
     */
    @Override
    public Iterator<BasicBlock> iterator()
    {
        return basicBlockList.iterator();
    }

    public LinkedList<BasicBlock> getBasicBlockList()
    {
        return basicBlockList;
    }

    public boolean empty()
    {
        return basicBlockList.isEmpty();
    }

    public int getNumOfArgs()
    {
        return argumentList.size();
    }

    @Override
    public boolean isNullValue()
    {
        return false;
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
        return basicBlockList.isEmpty();
    }

    public MachineFunction getMachineFunc()
    {
        return mf;
    }

    public void setMachineFunc(MachineFunction newFunc)
    {
        mf = newFunc;
    }

    public int getIntrinsicID()
    {
        // TODO
        return 0;
    }

    public Argument argAt(int index)
    {
        assert index >= 0 && index < getNumOfArgs();
        return argumentList.get(index);
    }

    public CallingConv getCallingConv()
    {
        return cc;
    }

    public void setCallingConv(CallingConv cc)
    {
        this.cc = cc;
    }

    public ValueSymbolTable getValueSymbolTable()
    {
        return symTab;
    }

    public AttrList getAttributes()
    {
        return attributeList;
    }

    public void setAttributes(AttrList attrList)
    {
        this.attributeList = attrList;
    }

    public boolean hasFnAttr(int n)
    {
        return attributeList.paramHasAttr(0, n);
    }

    public void addFnAttr(int n)
    {
        addAttribute(0, n);
    }

    public void removeFnAttr(int n)
    {
        removeAttribute(0, n);
    }

    private void addAttribute(int index, int attr)
    {
        // TODO: 2017/11/27
    }

    private void removeAttribute(int index, int attr)
    {
        // TODO: 2017/11/27
    }

    public boolean paramHasAttr(int i, int attr)
    {
        return attributeList.paramHasAttr(i, attr);
    }

    public int getParamAlignment(int index)
    {
        return attributeList.getParamAlignment(index);
    }

    /// @brief Determine if the function does not access memory.
    public boolean doesNotAccessMemory()
    {
        return hasFnAttr(Attribute.ReadNone);
    }

    public void setDoesNotAccessMemory()
    {
        setDoesNotAccessMemory(true);
    }

    public void setDoesNotAccessMemory(boolean doesNotAccessMemory)
    {
        if (doesNotAccessMemory)
            addFnAttr(Attribute.ReadNone);
        else
            removeFnAttr(Attribute.ReadNone);
    }

    /// @brief Determine if the function does not access or only reads memory.
    public boolean onlyReadsMemory()
    {
        return doesNotAccessMemory() || hasFnAttr(Attribute.ReadOnly);
    }

    public void setOnlyReadsMemory()
    {
        setOnlyReadsMemory(true);
    }

    public void setOnlyReadsMemory(boolean OnlyReadsMemory)
    {
        if (OnlyReadsMemory)
            addFnAttr(Attribute.ReadOnly);
        else
            removeFnAttr(Attribute.ReadOnly | Attribute.ReadNone);
    }

    /// @brief Determine if the function cannot return.
    public boolean doesNotReturn()
    {
        return hasFnAttr(Attribute.NoReturn);
    }

    public void setDoesNotReturn()
    {
        setDoesNotReturn(true);
    }

    public void setDoesNotReturn(boolean DoesNotReturn)
    {
        if (DoesNotReturn)
            addFnAttr(Attribute.NoReturn);
        else
            removeFnAttr(Attribute.NoReturn);
    }

    /// @brief Determine if the function cannot unwind.
    public boolean doesNotThrow()
    {
        return hasFnAttr(Attribute.NoUnwind);
    }

    public void setDoesNotThrow()
    {
        setDoesNotThrow(true);
    }

    public void setDoesNotThrow(boolean DoesNotThrow)
    {
        if (DoesNotThrow)
            addFnAttr(Attribute.NoUnwind);
        else
            removeFnAttr(Attribute.NoUnwind);
    }

    /// @brief Determine if the function returns a structure through first
    /// pointer argument.
    public boolean hasStructRetAttr()
    {
        return paramHasAttr(1, Attribute.StructRet);
    }

    /// @brief Determine if the parameter does not alias other parameters.
    /// @param n The parameter to check. 1 is the first parameter, 0 is the return
    public boolean doesNotAlias(int n)
    {
        return paramHasAttr(n, Attribute.NoAlias);
    }

    public void setDoesNotAlias(int n)
    {
        setDoesNotAlias(n, true);
    }

    public void setDoesNotAlias(int n, boolean DoesNotAlias)
    {
        if (DoesNotAlias)
            addAttribute(n, Attribute.NoAlias);
        else
            removeAttribute(n, Attribute.NoAlias);
    }

    /// @brief Determine if the parameter can be captured.
    /// @param n The parameter to check. 1 is the first parameter, 0 is the return
    public boolean doesNotCapture(int n)
    {
        return paramHasAttr(n, Attribute.NoCapture);
    }

    public void setDoesNotCapture(int n)
    {
        setDoesNotCapture(n, true);
    }

    public void setDoesNotCapture(int n, boolean DoesNotCapture)
    {
        if (DoesNotCapture)
            addAttribute(n, Attribute.NoCapture);
        else
            removeAttribute(n, Attribute.NoCapture);
    }
}
