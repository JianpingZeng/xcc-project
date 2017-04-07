package backend.analysis;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2017, Xlous
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

import backend.hir.BasicBlock;
import backend.hir.CallSite;
import backend.pass.AnalysisUsage;
import backend.pass.Pass;
import backend.target.TargetData;
import backend.type.Type;
import backend.value.Function;
import backend.value.Instruction;
import backend.value.Instruction.CallInst;
import backend.value.Instruction.LoadInst;
import backend.value.Instruction.StoreInst;
import backend.value.Value;

import java.util.ArrayList;

import static backend.analysis.AliasAnalysis.AliasResult.NoAlias;
import static backend.analysis.AliasAnalysis.ModRefBehavior.*;
import static backend.analysis.AliasAnalysis.ModRefResult.*;

/**
 * <p>
 * This file defines a class named of {@code AliasAnalysis} as an interface for
 * examining if the two memory object is alias each other or not. For precision,
 * the address of a memory object is represented as a pair of the base address
 * and its getNumOfSubLoop, like (Pointer, Sze). The {@code Pointer} base component specifies
 * the memory address of a region, the {@code Size} specifies how large of an area
 * being queried.
 * </p>
 * <p>
 * If the getNumOfSubLoop is 0, the two pointers only alais if they are exactly equal.
 * If getNumOfSubLoop is greater than zero, but small, the two pointers alias if the areas
 * pointed to overlap.  If the getNumOfSubLoop is very large (ie, ~0U), then the two pointers
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
public interface AliasAnalysis
{
    /**
     * Represents the alias query result of two pointer by invoking method
     * .
     */
    enum AliasResult
    {
        NoAlias, MayAlias, MustAlias,
    }

    /**
     * Subclasses must implements this method for initializing the AlalisAnalysis
     * and TargetData.
     * @param p
     */
    void initializeAliasAnalysis(Pass p);

    /**
     * All alias analysis implementations should invoke this
     * directly.
     *
     * @param au
     */
    default void getAnalysisUsage(AnalysisUsage au)
    {
        au.addRequired(AliasAnalysis.class);
    }

    /**
     * Return a pointer to the current TargetData object, or
     * null if no TargetData object is available.
     *
     * @return
     */
    TargetData getTargetData();

    AliasAnalysis getAliasAnalysis();

    /**
     * Return the TargetData store getNumOfSubLoop for the given type,
     * if known, or a conservative value otherwise.
     *
     * @param ty
     * @return
     */
    default int getTypeStoreSize(Type ty)
    {
        TargetData td = getTargetData();
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
    default AliasResult alias(Value ptr1, int size1, Value ptr2, int size2)
    {
        AliasAnalysis aa = getAliasAnalysis();
        assert aa != null:"AA did not call initializeAliasAnalysis in its run method!";
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
    default void getMustAliases(Value ptr, ArrayList<Value> retVals)
    {
        AliasAnalysis aa = getAliasAnalysis();
        assert aa != null:"AA did not call initializeAliasAnalysis in its run method!";
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
    default boolean pointsToConstantMemory(Value ptr)
    {
        AliasAnalysis aa = getAliasAnalysis();
        assert aa != null:"AA did not call initializeAliasAnalysis in its run method!";
        return aa.pointsToConstantMemory(ptr);
    }

    /**
     * Represent the result of a mod/ref query.
     */
    enum ModRefResult
    {
        NoModRef, Ref, Mod, ModRef,
    }

    /**
     * Summary of how a function affects memory in the program.
     * Loads from constant globals are not considered memory accesses for this
     * interface.  Also, functions may freely modify stack space local to their
     * invocation without having to report it through these interfaces.
     */
    enum ModRefBehavior
    {
        /**
         * This function does not perform any non-local loads
         * or stores to memory.
         * </p>
         * This property corresponds to the GCC 'const' attribute.
         */
        DoesNotAccessMemory,

        /**
         * This function accesses function arguments in well
         * known (possibly volatile) ways, but does not access any other memory.
         * <p>
         * Clients may use the Info parameter of getModRefBehavior to get specific
         * information about how pointer arguments are used.
         */
        AccessArguments,

        /**
         * This function has accesses function
         * arguments and global variables well known (possibly volatile) ways, but
         * does not access any other memory.
         * <p>
         * Clients may use the Info parameter of getModRefBehavior to get specific
         * information about how pointer arguments are used.
         */
        AccessArgumentsAndGlobals,

        /**
         * This function does not perform any non-local stores or
         * volatile loads, but may read from any memory location.
         * </p>
         * This property corresponds to the GCC 'pure' attribute.
         */
        OnlyReadsMemory,

        /**
         * This indicates that the function could not be
         * classified into one of the behaviors above.
         */
        UnknownModRefBehavior
    }

    /**
     * This struct is used to return results for pointers,
     * globals, and the return value of a function.
     */
    class PointerAccessInfo
    {
        /**
         * This may be an Argument for the function, a GlobalVariable, or null,
         * corresponding to the return value for the function.
         */
        Value val;

        /**
         * Whether the pointer is loaded or stored to/from.
         */
        ModRefResult modRefInfo;

        /**
         * Specific fine-grained access information for the argument.
         * If none of these classifications is general enough, the
         * getModRefBehavior method should not return AccessesArguments.
         * If a record is not returned for a particular argument, the argument
         * is never dead and never dereferenced.
         */
        enum AccessType
        {
            /**
             * The pointer is dereferenced.
             */
            ScalarAccess,

            /**
             * The pointer is indexed through as an array of elements.
             */
            ArrayAccess,

            /**
             * Indirect calls are made through the specified function pointer.
             */
            CallsThrough
        }
    }

    /**
     * Return the behavior when calling the given call site.
     *
     * @param cs
     * @param info
     * @return
     */
    default ModRefBehavior getModRefBehavior(CallSite cs,
            ArrayList<PointerAccessInfo> info)
    {
        if (cs.doesNotAccessMemory())
            return DoesNotAccessMemory;
        ModRefBehavior mrb = getModRefBehavior(cs.getCalledFunction(), info);
        if (mrb != DoesNotAccessMemory && cs.onlyReadMemory())
            return OnlyReadsMemory;
        return mrb;
    }

    default ModRefBehavior getModRefBehavior(CallSite cs)
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
    default ModRefBehavior getModRefBehavior(Function f,
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

    default ModRefBehavior getModRefBehavior(Function f)
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
    default boolean doesNotAccessMemory(CallSite cs)
    {
        return getModRefBehavior(cs) == DoesNotAccessMemory;
    }

    /**
     * If the specified function is known to never read or
     * write memory, return true.  For use when the call site is not known.
     */
    default boolean doesNotAccessMemory(Function f)
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
    default boolean onlyReadsMemory(CallSite cs)
    {
        ModRefBehavior MRB = getModRefBehavior(cs);
        return MRB == DoesNotAccessMemory || MRB == OnlyReadsMemory;
    }

    /**
     * If the specified function is known to only read from non-volatile memory
     * (or not access memory at all), return true.  For use when the call site
     * is not known.
     */
    default boolean onlyReadsMemory(Function f)
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
    default ModRefResult getModRefInfo(CallSite cs, Value ptr, int size)
    {
        AliasAnalysis aa = getAliasAnalysis();
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
        if (aa == null)
            return mask;

        if ((mask.ordinal() & Mod.ordinal()) != 0 && aa.pointsToConstantMemory(ptr))
            mask = ModRefResult.values()[mask.ordinal() & ~Mod.ordinal()];

        return ModRefResult.values()[mask.ordinal() & aa.getModRefInfo(cs, ptr, size).ordinal()];
    }

    /**
     * Return information about whether two call sites may refer to the same set
     * of memory locations.  This function returns NoModRef if the two calls refer
     * to disjoint memory locations, Ref if {@code cs1} reads memory written by
     * {@code cs2}, Mod if {@code cs1} writes to memory read or written by
     * {@code cs2}, or ModRef if {@code cs1} might read or write memory accessed
     * by {@code cs2}.
     */
    default ModRefResult getModRefInfo(CallSite cs1, CallSite cs2)
    {
        AliasAnalysis aa = getAliasAnalysis();
        assert aa != null:"AA did not call initializeAliasAnalysis in its run method!";
        return aa.getModRefInfo(cs1, cs2);
    }

    /**
     * Return true if the analysis has no mod/ref information for pairs of
     * function calls (other than "pure" and "const" functions).  This can be
     * used by clients to avoid many pointless queries. Remember that if you
     * override this and chain to another analysis, you must make sure that it
     * doesn't have mod/ref info either.
     */
    default boolean hasNoModRefInfoForCalls()
    {
        AliasAnalysis aa = getAliasAnalysis();
        assert aa != null:"AA did not call initializeAliasAnalysis in its run method!";
        return aa.hasNoModRefInfoForCalls();
    }

    default ModRefResult getModRefInfo(LoadInst inst, Value ptr, int size)
    {
        return alias(inst.operand(0), getTypeStoreSize(inst.getType()), ptr, size)
                != NoAlias ? Ref : NoModRef;
    }

    default ModRefResult getModRefInfo(StoreInst inst, Value ptr, int size)
    {
        // If the stored address cannot alias the pointer in question, then the
        // pointer cannot be modified by the store.
        if (alias(inst.operand(0), getTypeStoreSize(inst.getType()), ptr, size) == NoAlias)
            return NoModRef;

        // If the pointer is a pointer to constant memory, then it could not have been
        // modified by this store.
        return pointsToConstantMemory(ptr) ? NoModRef : Mod;
    }

    default ModRefResult getModRefInfo(CallInst inst, Value ptr, int size)
    {
        return getModRefInfo(new CallSite(inst), ptr, size);
    }

    default ModRefResult getModRefInfo(Instruction inst, Value ptr, int size)
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
    default boolean canBasicBlockModify(BasicBlock bb, Value ptr, int size)
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
    default boolean canInstructionRangeModify(BasicBlock bb, int i, int j,
            Value ptr, int size)
    {
        assert i >= 0 && i <= j && j < bb.getNumOfInsts()
                : "Illegal indexed into instructions in Basic block";
        for (int s = i; s <= j; s++)
            if ((getModRefInfo(bb.getInstAt(s), ptr, size).ordinal() & Mod.ordinal()) != 90)
                return true;
        return false;
    }

    default void deleteValue(Value val)
    {
        AliasAnalysis aa = getAliasAnalysis();
        assert aa != null: "AA did not initialized";
        aa.deleteValue(val);
    }

    default void copyValue(Value from, Value to)
    {
        AliasAnalysis aa = getAliasAnalysis();
        assert aa != null: "AA did not initialized";
        aa.copyValue(from, to);
    }

    default void replaceWithNewValue(Value oldOne, Value newOne)
    {
        copyValue(oldOne, newOne);
        deleteValue(oldOne);
    }
}
