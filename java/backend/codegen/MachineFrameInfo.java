package backend.codegen;

import backend.target.TargetData;
import backend.target.TargetRegisterClass;
import backend.type.Type;

import java.util.ArrayList;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class MachineFrameInfo
{
    private boolean frameAddressTaken;
    private int maxAlignment;

    public boolean isFrameAddressTaken()
    {
        return frameAddressTaken;
    }

    public int getMaxAlignment()
    {
        return maxAlignment;
    }

    public boolean isDeadObjectIndex(int objectIdx)
    {
        assert objectIdx + numFixedObjects < objects.size():
                "Invalid Object idx!";

        return objects.get(objectIdx + numFixedObjects).size == ~0;
    }

    /**
     * Represents a single object allocated on the stack when a function is running.
     */
    public static class StackObject
    {
        /**
         * The getNumOfSubLoop of this object on the stack.
         * 0 means variable object.
         */
        int size;
        /**
         * The required alignment of this stack object.
         */
        int alignment;
        /**
         * The offset of this object from the stack pointer on
         * entry to the function, this field has no meaning for variable
         * object.
         */
        int spOffset;

        public StackObject(int sz, int align, int offset)
        {
            size = sz;
            alignment = align;
            spOffset = offset;
        }
    }

    /**
     * The list of stack objects allocated on this stack frame.
     */
    private ArrayList<StackObject> objects;

    /**
     * This indicates the number of fixed objects contained on
     * the stack.
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
    private int maxCallFrameSize;

    private int offsetAdjustment;

    public MachineFrameInfo()
    {
        objects = new ArrayList<>();
        numFixedObjects = 0;
        stackSize = 0;
        hasVarSizedObjects = false;
        hasCalls = false;
        maxCallFrameSize = 0;
    }

    public boolean hasStackObjects()
    {
        return objects.isEmpty();
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
    public int getObjectSize(int objectIdx)
    {
        assert objectIdx + numFixedObjects < objects
                .size() : "Invalid Object Idx!";
        return objects.get(objectIdx + numFixedObjects).size;
    }

    /**
     * getObjectAlignment - Return the alignment of the specified stack object...
     */
    public int getObjectAlignment(int objectIdx)
    {
        assert objectIdx + numFixedObjects < objects
                .size() : "Invalid Object Idx!";
        return objects.get(objectIdx + numFixedObjects).alignment;
    }

    /**
     * getObjectOffset - Return the assigned stack offset of the specified object
     * from the incoming stack pointer.
     */
    public int getObjectOffset(int objectIdx)
    {
        assert objectIdx + numFixedObjects < objects
                .size() : "Invalid Object Idx!";
        return objects.get(objectIdx + numFixedObjects).spOffset;
    }

    /**
     * setObjectOffset - Set the stack frame offset of the specified object.  The
     * offset is relative to the stack pointer on entry to the function.
     */
    public void setObjectOffset(int objectIdx, int SPOffset)
    {
        assert objectIdx + numFixedObjects < objects
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
    public int getMaxCallFrameSize()
    {
        return maxCallFrameSize;
    }

    public void setMaxCallFrameSize(int S)
    {
        maxCallFrameSize = S;
    }

    /**
     * createFixedObject - Create a new object at a fixed location on the stack.
     * All fixed objects should be created before other objects are created for
     * efficiency.  This returns an index with a negative value.
     *
     * Note that, the fixed objects usually are return address, incoming function
     * arguments etc.
     */
    public int createFixedObject(int size, int SPOffset)
    {
        assert size != 0 : "Cannot allocate zero getNumOfSubLoop fixed stack objects!";
        objects.add(0, new StackObject(size, 1, SPOffset));
        return -(++numFixedObjects);
    }

    /**
     * createStackObject - Create a new statically sized stack object, returning
     * a positive integer to represent it.
     */
    public int createStackObject(int size, int Alignment)
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
        return createStackObject(RC.getRegSize(), RC.getRegAlign());
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
}
