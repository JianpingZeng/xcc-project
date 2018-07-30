package backend.value;

import tools.Util;
import backend.codegen.MachineFunction;
import backend.intrinsic.Intrinsic;
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
import java.util.List;

/**
 * This class is representation at the Module(high-level IR) of a function or method.
 *
 * @author Jianping Zeng
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
            Util.assertion(!t.isVoidType(),  "Can't have void typed argument!");
            Argument arg = new Argument(t);
            argumentList.add(arg);
            arg.setParent(this);
        }
        basicBlockList = new LinkedList<>();
        setCallingConv(CallingConv.C);
        setVisibility(VisibilityTypes.DefaultVisibility);
        attributeList = new AttrList(new ArrayList<>());
    }

    /**
     * Returns the number of basic blocks in this function.
     * @return
     */
    public int size()
    {
        return basicBlockList == null ? 0 : basicBlockList.size();
    }

    /**
     * Return the basic block at the position specified by given index.
     * @param index
     * @return
     */
    public BasicBlock getBlockAt(int index)
    {
        Util.assertion(index >= 0 && index < size());
        return basicBlockList.get(index);
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
        return basicBlockList == null || basicBlockList.isEmpty() ?
                null : basicBlockList.getFirst();
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

    /**
     * Add Basic block into this function's block list.
     * @param bb
     */
    public void addBasicBlock(BasicBlock bb)
    {
        Util.assertion(bb != null, "Can't add a null block!");
        if (basicBlockList.contains(bb))
            return;

        basicBlockList.add(bb);
        bb.setParent(this);
        symTab.createValueName(bb.getName(), bb);
    }

    public void addBasicBlockBefore(BasicBlock beforePos, BasicBlock bb)
    {
        Util.assertion( beforePos != null && basicBlockList.contains(beforePos));
        Util.assertion( bb != null && !basicBlockList.contains(bb));
        int idx = basicBlockList.indexOf(beforePos);
        basicBlockList.add(idx, bb);
        bb.setParent(this);
        symTab.createValueName(bb.getName(), bb);
    }

    public void addBasicBlockAfter(BasicBlock afterPos, BasicBlock bb)
    {
        Util.assertion( afterPos != null && basicBlockList.contains(afterPos));
        Util.assertion( bb != null && !basicBlockList.contains(bb));
        int idx = basicBlockList.indexOf(afterPos);
        basicBlockList.add(idx+1, bb);
        bb.setParent(this);
        symTab.createValueName(bb.getName(), bb);
    }

    public LinkedList<BasicBlock> getBasicBlockList()
    {
        return basicBlockList;
    }

    public void setBasicBlockList(List<BasicBlock> bbs)
    {
        basicBlockList.clear();
        if (bbs == null || bbs.isEmpty())
        {
            return;
        }
        basicBlockList.addAll(bbs);
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

    public Intrinsic.ID getIntrinsicID()
    {
        String name = getName();
        if (name == null) return Intrinsic.ID.not_intrinsic;
        int len = name.length();
        if (len < 5 || name.charAt(4) != '.' ||
                !name.startsWith("llvm"))
            return Intrinsic.ID.not_intrinsic;

        for (Intrinsic.ID id : Intrinsic.ID.values())
            if (name.startsWith(id.name))
                return id;
        return Intrinsic.ID.not_intrinsic;
    }

    public boolean isIntrinsicID()
    {
        return getIntrinsicID() != Intrinsic.ID.not_intrinsic;
    }

    public Argument argAt(int index)
    {
        Util.assertion( index >= 0 && index < getNumOfArgs());
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
