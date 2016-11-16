package frontend.type;

import tools.Name;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class Slot
{
    /**
     * The getName of this slot.
     */
    private Name name;
    /**
     * The frontend.type of this slot.
     */
    private Type type;
    /**
     * The offset of this slot away from beginning address of struct node.
     */
    private long offset;

    /**
     * Constructs a instance of class Slot.
     * @param name
     * @param type
     * @param offset
     */
    public Slot(Name name, Type type, long offset)
    {
        this.name = name;
        this.type = type;
        this.offset = offset;
    }

    public Type getType() { return type; }
    public Name getName() { return name; }
    public long getOffset() { return offset; }
    public void setOffset(long newOffset) {offset = newOffset; }
    public long size() { return type.getTypeSize(); }
    public long allocaSize() { return type.allocSize(); }
    public long alignment() { return type.alignment(); }
}
