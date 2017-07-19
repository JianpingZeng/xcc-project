package backend.target;

/**
 * This class defines an interface used for obtaining stack frame layout
 * information about the specified target machine.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public class TargetFrameInfo
{
    public enum StackDirection
	{
		/**
		 * Adding to the stack increasing the stack address.
		 */
		StackGrowUp,
		/**
		 * Adding to the stack decreasing the stack address.
		 */
		StackGrowDown
	}

	private StackDirection direction;
	private int stackAlignment;
	private int localAreaOffset;

	public TargetFrameInfo(StackDirection dir, int stackAlign, int lao)
	{
		direction = dir;
		stackAlignment = stackAlign;
		localAreaOffset = lao;
	}

	public StackDirection getStackGrowDirection() {return direction;}

	public int getStackAlignment() {return stackAlignment;}

	public int getLocalAreaOffset() {return localAreaOffset;}

	/**
	 * This method used for aligning stack frame depending on the specified target.
	 * @param unalignOffset
	 * @param growUp
	 * @param align
	 */
	public void adjustAlignment(int unalignOffset, boolean growUp, int align){}
}
