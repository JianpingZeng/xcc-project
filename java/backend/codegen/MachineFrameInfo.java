package backend.codegen;

import backend.target.TargetData;
import backend.target.TargetFrameInfo;
import backend.target.TargetRegisterClass;
import backend.type.Type;

import java.io.PrintStream;
import java.util.ArrayList;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class MachineFrameInfo
{
    /**
     * Represents a single object allocated on the stack when a function is running.
     */
    public static class StackObject
    {
        /**
         * The getNumOfSubLoop of this object on the stack.
         * 0 means variable object.
         */
        public long size;
        /**
         * The required alignment of this stack object.
         */
        public int alignment;

        public boolean isImmutable;
        /**
         * The offset of this object from the stack pointer on
         * entry to the function, this field has no meaning for variable
         * object.
         */
        public long spOffset;

        public StackObject(long sz, int align, int offset, boolean im)
        {
            size = sz;
            alignment = align;
            spOffset = offset;
            isImmutable = im;
        }

        public StackObject(long sz, int align, int offset)
        {
            this(sz, align, offset, false);
        }
    }

    /**
     * The list of stack objects allocated on this stack frame.
     * <pre>
     * The layout of objects is illustrated as follows:
     * |                      |                            |
     * |<-Fixed stack object->|<--Local variable object--->|
     * |For incoming argument |                            |
     * ^                      ^                            ^
     * |                      |                            |
     * -numFixedObjects       0                       top of stack
     * </pre>
     */
    private ArrayList<StackObject> objects;

    /**
     * This indicates the number of fixed objects contained on
     * the stack.</br>
     * In the another world, it means the number of incoming arguments for
     * current function.
     */
    private int numFixedObjects;

    /**
     * Keep tracks of if any variable sized objects have been alocated.
     */
    private boolean hasVarSizedObjects;
    /**
     * THe prologue/epilogue code inserter calculates the final stack offsts
     * for all fixed sized objects, updating the object list above.
     * It then upates stackSize to contain the number of bytes that
     * need to be allocated on entry to the fucntion.
     */
    private int stackSize;

    /**
     * Set to true if there are any function call.
     */
    private boolean hasCalls;

    /**
     * This contains the getNumOfSubLoop of largest call frame if the target
     * uses frame setup/destroy instruction.  This information is important for frame pointer
     * elimination.  If is only valid during and after prolog/epilog code
     * insertion.
     */
    private long maxCallFrameSize;

    private int offsetAdjustment;

    private boolean frameAddressTaken;

    private int maxAlignment;

    private ArrayList<CalleeSavedInfo> csInfo;

    private boolean csIValid;

    private TargetFrameInfo tfi;

    public MachineFrameInfo(TargetFrameInfo tfi)
    {
        csInfo = new ArrayList<>();
        this.tfi = tfi;
        objects = new ArrayList<>();
        numFixedObjects = 0;
        stackSize = 0;
        hasVarSizedObjects = false;
        hasCalls = false;
        maxCallFrameSize = 0;
    }

    public boolean isFrameAddressTaken()
    {
        return frameAddressTaken;
    }

    public int getMaxAlignment()
    {
        return maxAlignment;
    }

    public void setMaxAlignment(int maxAlignment)
    {
        this.maxAlignment = maxAlignment;
    }

    public boolean isDeadObjectIndex(int objectIdx)
    {
        assert objectIdx + numFixedObjects < objects
                .size() : "Invalid Object idx!";

        return objects.get(objectIdx + numFixedObjects).size == ~0;
    }

    /**
     * Returns true if the specified index corresponds to a
     * fixed stack object.
     *
     * @param idx
     * @return
     */
    public boolean isFixedObjectIndex(int idx)
    {
        return idx < 0 && (idx >= -numFixedObjects);
    }

    public boolean isImmutableObjectIndex(int objectIdx)
    {
        assert (objectIdx + numFixedObjects) < objects
                .size() : "Invalid object idx!";
        return objects.get(objectIdx + numFixedObjects).isImmutable;
    }

    public ArrayList<CalleeSavedInfo> getCalleeSavedInfo()
    {
        return csInfo;
    }

    public boolean hasStackObjects()
    {
        return !objects.isEmpty();
    }

    public boolean hasVarSizedObjects()
    {
        return hasVarSizedObjects;
    }

    /**
     * getObjectIndexBegin - Return the minimum frame object index...
     */
    public int getObjectIndexBegin()
    {
        return -numFixedObjects;
    }

    /**
     * getObjectIndexEnd - Return one past the maximum frame object index...
     */
    public int getObjectIndexEnd()
    {
        return objects.size() - numFixedObjects;
    }

    /**
     * getObjectSize - Return the getNumOfSubLoop of the specified object
     */
    public long getObjectSize(int objectIdx)
    {
        assert objectIdx + numFixedObjects >= 0 &&
                objectIdx + numFixedObjects < objects
                .size() : "Invalid Object Idx!";
        return objects.get(objectIdx + numFixedObjects).size;
    }

    /**
     * getObjectAlignment - Return the alignment of the specified stack object...
     */
    public int getObjectAlignment(int objectIdx)
    {
        assert objectIdx + numFixedObjects >= 0 &&
                objectIdx + numFixedObjects < objects
                        .size() : "Invalid Object Idx!";
        return objects.get(objectIdx + numFixedObjects).alignment;
    }

    /**
     * getObjectOffset - Return the assigned stack offset of the specified object
     * from the incoming stack pointer.
     */
    public int getObjectOffset(int objectIdx)
    {
        assert objectIdx + numFixedObjects >= 0 &&
                objectIdx + numFixedObjects < objects
                        .size() : "Invalid Object Idx!";
        return (int) objects.get(objectIdx + numFixedObjects).spOffset;
    }

    /**
     * setObjectOffset - Set the stack frame offset of the specified object.  The
     * offset is relative to the stack pointer on entry to the function.
     */
    public void setObjectOffset(int objectIdx, long SPOffset)
    {
        assert objectIdx + numFixedObjects >= 0 &&
                objectIdx + numFixedObjects < objects
                        .size() : "Invalid Object Idx!";
        objects.get(objectIdx + numFixedObjects).spOffset = SPOffset;
    }

    /**
     * getStackSize - Return the number of bytes that must be allocated to hold
     * all of the fixed getNumOfSubLoop frame objects.  This is only valid after
     * Prolog/Epilog code insertion has finalized the stack frame layout.
     */
    public int getStackSize()
    {
        return stackSize;
    }

    /**
     * setStackSize - Set the getNumOfSubLoop of the stack...
     */
    public void setStackSize(int size)
    {
        stackSize = size;
    }

    /**
     * hasCalls - Return true if the current function has no function calls.
     * This is only valid during or after prolog/epilog code emission.
     */
    public boolean hasCalls()
    {
        return hasCalls;
    }

    public void setHasCalls(boolean V)
    {
        hasCalls = V;
    }

    /**
     * getMaxCallFrameSize - Return the maximum getNumOfSubLoop of a call frame that must be
     * allocated for an outgoing function call.  This is only available if
     * CallFrameSetup/Destroy pseudo instructions are used by the target, and
     * then only during or after prolog/epilog code insertion.
     */
    public long getMaxCallFrameSize()
    {
        return maxCallFrameSize;
    }

    public void setMaxCallFrameSize(long S)
    {
        maxCallFrameSize = S;
    }

    /**
     * createFixedObject - Create a new object at a fixed location on the stack.
     * All fixed objects should be created before other objects are created for
     * efficiency.  This returns an index with a negative value.
     * <p>
     * Note that, the fixed objects usually are return address, incoming function
     * arguments etc.
     */
    public int createFixedObject(int size, int offset)
    {
        return createFixedObject(size, offset, true);
    }

    public int createFixedObject(int size, int offset, boolean isImmutable)
    {
        assert size != 0 : "Cannot allocate zero getNumOfSubLoop fixed stack objects!";
        objects.add(0, new StackObject(size, 1, offset, isImmutable));
        return -(++numFixedObjects);
    }

    /**
     * createStackObject - Create a new statically sized stack object, returning
     * a positive integer to represent it.
     */
    public int createStackObject(long size, int Alignment)
    {
        assert size != 0 : "Cannot allocate zero getNumOfSubLoop stack objects!";
        objects.add(new StackObject(size, Alignment, -1));
        return objects.size() - numFixedObjects - 1;
    }

    /**
     * createStackObject - Create a stack object for a value of the specified
     * Backend type or register class.
     */
    public int createStackObject(Type type, TargetData td)
    {
        return createStackObject((int) td.getTypeSize(type),
                td.getTypeAlign(type));
    }

    public int createStackObject(TargetRegisterClass RC)
    {
        return createStackObject(RC.getRegSize(), RC.getAlignment());
    }

    /**
     * createVariableSizedObject - Notify the MachineFrameInfo object that a
     * variable sized object has been created.  This must be created whenever a
     * variable sized object is created, whether or not the index returned is
     * actually used.
     */
    public int createVariableSizedObject()
    {
        hasVarSizedObjects = true;
        objects.add(new StackObject(0, 1, -1));
        return objects.size() - numFixedObjects - 1;
    }

    public int getOffsetAdjustment()
    {
        return offsetAdjustment;
    }

    public void setOffsetAdjustment(int offset)
    {
        offsetAdjustment = offset;
    }

    public void setCalleeSavedInfo(ArrayList<CalleeSavedInfo> csInfo)
    {
        this.csInfo = csInfo;
    }

    public void setCalleeSavedInfoValid(boolean b)
    {
        csIValid = b;
    }

    public void print(MachineFunction mf, PrintStream os)
    {
        TargetFrameInfo tfi = mf.getTarget().getFrameInfo();
        int valueOffset = tfi != null ? tfi.getLocalAreaOffset():0;

        for (int i = 0, e = objects.size(); i < e; i++)
        {
            StackObject obj = objects.get(i);
            os.printf(" <fi#%d>: ", i - numFixedObjects);
            if (obj.size == ~0L)
            {
                os.printf("dead%n");
                continue;
            }
            if (obj.size == 0)
                os.printf("variable dead");
            else
            {
                os.printf("size is %d byte%s,", obj.size, obj.size != 1 ? "s":"");
            }
            os.printf(" alignent is %d byte%s", obj.alignment, obj.alignment != 1 ?"s,":",");
            if (i < numFixedObjects)
                os.printf(" fixed");
            if (i < numFixedObjects || obj.spOffset != -1)
            {
                long offset = obj.spOffset - valueOffset;
                os.printf(" at location [SP");
                if (offset > 0)
                    os.printf("+%d", offset);
                else if (offset < 0)
                    os.print(offset);
                os.print("]");
            }
            os.println();
        }

        if (hasVarSizedObjects())
            os.println(" Stack frame contains variable sized objects");
    }

    public void dump(MachineFunction mf)
    {
        print(mf, System.err);
    }
}
