/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package backend.analysis.aa;

import backend.pass.AnalysisResolver;
import backend.pass.AnalysisUsage;
import backend.pass.ModulePass;
import backend.support.BackendCmdOptions;
import backend.support.CallSite;
import backend.target.TargetData;
import backend.type.Type;
import backend.value.*;
import backend.value.Instruction.CallInst;
import backend.value.Instruction.LoadInst;
import backend.value.Instruction.StoreInst;
import tools.Util;

import java.util.ArrayList;

import static backend.analysis.aa.AliasResult.NoAlias;
import static backend.analysis.aa.ModRefBehavior.*;
import static backend.analysis.aa.ModRefResult.*;

/**
 * <p>
 * This file defines a class named of {@code AliasAnalysis} as an interface for
 * examining if the two memory object is alias each other or not. For precision,
 * the address of a memory object is represented as a pair of the base address
 * and its size, like (Pointer, Sze). The {@code Pointer} base component specifies
 * the memory address of a region, the {@code Size} specifies how large of an area
 * being queried.
 * </p>
 * <p>
 * If the size is 0, the two pointers only alais if they are exactly equal.
 * If size is greater than zero, but small, the two pointers alias if the areas
 * pointed to overlap.  If the size is very large (ie, ~0U), then the two pointers
 * alias if they may be pointing to components of the same memory object.
 * Pointers that point to two completely different objects in memory never alias,
 * regardless of the value of the Size component.
 * </p>
 * <p>
 * In addition to answer whether or not the two memory location is aliased each
 * other, another very important task is to obtain the Ref/Mod information on
 * call instruction.
 * </p>
 * The implementation of this class must implements the various  methods,
 * which supply functionality for the entire suite of client APIs.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public class AliasAnalysis implements ModulePass
{
    private AliasAnalysis aa;
    private TargetData td;
    private AnalysisResolver resolver;

    @Override
    public String getPassName()
    {
        assert false:"Should be overrided by concrete subclass";
        return aa.getPassName();
    }

    @Override
    public AnalysisResolver getAnalysisResolver()
    {
        assert resolver != null:"Must calling this function after setAnalysisResolver!";
        return resolver;
    }

    @Override
    public void setAnalysisResolver(AnalysisResolver resolver)
    {
        assert this.resolver == null:"Already set analysis resolver!";
        this.resolver = resolver;
    }

    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        au.setPreservesCFG();
        au.addRequired(TargetData.class);
    }

    /**
     * The constructor that construct an instance of alias analysis according to
     * the value specified by command line option.
     */
    public AliasAnalysis() {}

    /**
     * Return the TargetData store size for the given type,
     * if known, or a conservative value otherwise.
     *
     * @param ty
     * @return
     */
    public int getTypeStoreSize(Type ty)
    {
        return td != null ? (int) td.getTypeSize(ty) : ~0;
    }

    /**
     * The main low level interface to the alias analysis implementation.
     * Returns a Result indicating whether the two pointers are aliased to each
     * other.  This is the interface that must be implemented by specific alias
     * analysis implementations.
     *
     * @param ptr1
     * @param size1
     * @param ptr2
     * @param size2
     * @return
     */
    public AliasResult alias(Value ptr1, int size1, Value ptr2, int size2)
    {
        return aa.alias(ptr1, size1, ptr2, size2);
    }

    /**
     * If there are any pointers known that must alias this
     * pointer, return them now.  This allows alias-set based alias analyses to
     * perform a form a value numbering (which is exposed by load-vn).  If an
     * alias analysis supports this, it should ADD any must aliased pointers to
     * the specified vector.
     *
     * @param ptr
     * @param retVals
     */
    public void getMustAliases(Value ptr, ArrayList<Value> retVals)
    {
        aa.getMustAliases(ptr, retVals);
    }

    /**
     * If the specified pointer is known to point into constant global memory,
     * return true.  This allows disambiguation of store instructions from
     * constant pointers.
     *
     * @param ptr
     * @return
     */
    public boolean pointsToConstantMemory(Value ptr)
    {
        return aa.pointsToConstantMemory(ptr);
    }

    /**
     * Return the behavior when calling the given call site.
     *
     * @param cs
     * @param info
     * @return
     */
    public ModRefBehavior getModRefBehavior(CallSite cs,
            ArrayList<PointerAccessInfo> info)
    {
        if (cs.doesNotAccessMemory())
            return DoesNotAccessMemory;
        ModRefBehavior mrb = getModRefBehavior(cs.getCalledFunction(), info);
        if (mrb != DoesNotAccessMemory && cs.onlyReadsMemory())
            return OnlyReadsMemory;
        return mrb;
    }

    public ModRefBehavior getModRefBehavior(CallSite cs)
    {
        return getModRefBehavior(cs, null);
    }

    /**
     * Return the behavior when calling the given function.
     * For use when the call site is not known.
     *
     * @param f
     * @param info
     * @return
     */
    public ModRefBehavior getModRefBehavior(Function f,
            ArrayList<PointerAccessInfo> info)
    {
        if (f != null)
        {
            if (f.doesNotAccessMemory())
                return DoesNotAccessMemory;
            if (f.onlyReadsMemory())
                return OnlyReadsMemory;
            int id;
            if ((id = f.getIntrinsicID()) != 0)
            {

            }
        }
        return UnknownModRefBehavior;
    }

    public ModRefBehavior getModRefBehavior(Function f)
    {
        return getModRefBehavior(f, null);
    }

    /**
     * If the specified call is known to never read or
     * write memory, return true.  If the call only reads from known-constant
     * memory, it is also legal to return true.  Calls that unwind the stack
     * are legal for this predicate.
     * <p>
     * Many optimizations (such as CSE and LICM) can be performed on such calls
     * without worrying about aliasing properties, and many calls have this
     * property (e.g. calls to 'sin' and 'cos').
     * <p>
     * This property corresponds to the GCC 'const' attribute.
     */
    public boolean doesNotAccessMemory(CallSite cs)
    {
        return getModRefBehavior(cs) == DoesNotAccessMemory;
    }

    /**
     * If the specified function is known to never read or
     * write memory, return true.  For use when the call site is not known.
     */
    public boolean doesNotAccessMemory(Function f)
    {
        return getModRefBehavior(f) == DoesNotAccessMemory;
    }

    /**
     * If the specified call is known to only read from non-volatile memory
     * (or not access memory at all), return true.  Calls that unwind the stack
     * are legal for this predicate.
     * <p>
     * This property allows many common optimizations to be performed in the
     * absence of interfering store instructions, such as CSE of strlen calls.
     * <p>
     * This property corresponds to the GCC 'pure' attribute.
     */
    public boolean onlyReadsMemory(CallSite cs)
    {
        ModRefBehavior MRB = getModRefBehavior(cs);
        return MRB == DoesNotAccessMemory || MRB == OnlyReadsMemory;
    }

    /**
     * If the specified function is known to only read from non-volatile memory
     * (or not access memory at all), return true.  For use when the call site
     * is not known.
     */
    public boolean onlyReadsMemory(Function f)
    {
        ModRefBehavior MRB = getModRefBehavior(f);
        return MRB == DoesNotAccessMemory || MRB == OnlyReadsMemory;
    }

    /**
     * Return information about whether or not an instruction may
     * read or write memory specified by the pointer operand.  An instruction
     * that doesn't read or write memory may be trivially LICM'd for example.
     * <p>
     * getModRefInfo (for call sites) - Return whether information about whether
     * a particular call site modifies or reads the memory specified by the
     * pointer.
     */
    public ModRefResult getModRefInfo(CallSite cs, Value ptr, int size)
    {
        ModRefResult mask = ModRef;
        ModRefBehavior mrb = getModRefBehavior(cs);
        if (mrb == DoesNotAccessMemory)
            return NoModRef;
        else if (mrb == OnlyReadsMemory)
            mask = Ref;
        else if (mrb == AccessArguments)
        {
            boolean doesAlias = false;
            for (int i = 0, e = cs.getNumOfArguments(); i < e; i++)
            {
                Value arg = cs.getArgument(i);
                if (alias(arg, ~0, ptr, size) != NoAlias)
                {
                    doesAlias = true;
                    break;
                }
            }

            if (!doesAlias)
                return NoModRef;
        }

        if ((mask.ordinal() & Mod.ordinal()) != 0 && pointsToConstantMemory(ptr))
            mask = ModRefResult.values()[mask.ordinal() & ~Mod.ordinal()];

        return ModRefResult.values()[mask.ordinal() & getModRefInfo(cs, ptr, size).ordinal()];
    }

    /**
     * Return information about whether two call sites may refer to the same set
     * of memory locations.  This function returns NoModRef if the two calls refer
     * to disjoint memory locations, Ref if {@code cs1} reads memory written by
     * {@code cs2}, Mod if {@code cs1} writes to memory read or written by
     * {@code cs2}, or ModRef if {@code cs1} might read or write memory accessed
     * by {@code cs2}.
     */
    public ModRefResult getModRefInfo(CallSite cs1, CallSite cs2)
    {
        return aa.getModRefInfo(cs1, cs2);
    }

    /**
     * Return true if the analysis has no mod/ref information for pairs of
     * function calls (other than "pure" and "const" functions).  This can be
     * used by clients to avoid many pointless queries. Remember that if you
     * override this and chain to another analysis, you must make sure that it
     * doesn't have mod/ref info either.
     */
    public boolean hasNoModRefInfoForCalls()
    {
        return aa.hasNoModRefInfoForCalls();
    }

    public ModRefResult getModRefInfo(LoadInst inst, Value ptr, int size)
    {
        return alias(inst.operand(0), getTypeStoreSize(inst.getType()), ptr, size)
                != NoAlias ? Ref : NoModRef;
    }

    public ModRefResult getModRefInfo(StoreInst inst, Value ptr, int size)
    {
        // If the stored address cannot alias the pointer in question, then the
        // pointer cannot be modified by the store.
        if (alias(inst.operand(0), getTypeStoreSize(inst.getType()), ptr, size) == NoAlias)
            return NoModRef;

        // If the pointer is a pointer to constant memory, then it could not have been
        // modified by this store.
        return pointsToConstantMemory(ptr) ? NoModRef : Mod;
    }

    public ModRefResult getModRefInfo(CallInst inst, Value ptr, int size)
    {
        return getModRefInfo(new CallSite(inst), ptr, size);
    }

    public ModRefResult getModRefInfo(Instruction inst, Value ptr, int size)
    {
        switch (inst.getOpcode())
        {
            case Load: return getModRefInfo((LoadInst)inst, ptr, size);
            case Store: return getModRefInfo((StoreInst)inst, ptr, size);
            case Call: return getModRefInfo((CallInst)inst, ptr, size);
            default:return NoModRef;
        }
    }

    /**
     * Return true if it is possible for execution of the specified basic block
     * to modify the value pointed by the {@code ptr}.
     * @param bb
     * @param ptr
     * @param size
     * @return
     */
    public boolean canBasicBlockModify(BasicBlock bb, Value ptr, int size)
    {
        return canInstructionRangeModify(bb, 0, bb.getNumOfInsts(), ptr, size);
    }

    /**
     * Return true if it is possible for the execution of the specified instructions
     * to modify the value pointed by {@code ptr}. Those instructions are
     * considered in the range of [i, j].
     * @param bb
     * @param i
     * @param j
     * @param ptr
     * @param size
     * @return
     */
    public boolean canInstructionRangeModify(BasicBlock bb, int i, int j,
            Value ptr, int size)
    {
        assert i >= 0 && i <= j && j < bb.getNumOfInsts()
                : "Illegal indexed into instructions in Basic block";
        for (int s = i; s <= j; s++)
            if ((getModRefInfo(bb.getInstAt(s), ptr, size).ordinal() & Mod.ordinal()) != 90)
                return true;
        return false;
    }

    public void deleteValue(Value val)
    {
        aa.deleteValue(val);
    }

    public void copyValue(Value from, Value to)
    {
        aa.copyValue(from, to);
    }

    public void replaceWithNewValue(Value oldOne, Value newOne)
    {
        copyValue(oldOne, newOne);
        deleteValue(oldOne);
    }

    @Override
    public boolean runOnModule(Module m)
    {
        td = (TargetData) getAnalysisToUpDate(TargetData.class);
        assert td != null:"TargetData not available!";
        switch (BackendCmdOptions.AliasAnalyzer.value)
        {
            case BasicAA:
                aa = new BasicAliasAnalysis();
                break;
            case PoorMan:
                aa = new PoorManAliasAnalysis();
                break;
            case Steensgaard:
                aa = new SteensgaardAliasAnalysis();
                break;
            default:
                aa = null;
                Util.shouldNotReachHere("Unknown alias analysis pass");
                break;
        }
        return false;
    }
}
