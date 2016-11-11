package target;

/**
 * Primary interface to complete machine description for the target machine.
 * Our goal is that all target-specific information should accessible through
 * this interface.
 * @see TargetData
 * @author Xlous.zeng
 * @version 0.1
 */
public class TargetMachine
{
	/**
	 * The target name.
	 */
	private String name;
	/**
	 * Calculate type size and alignment.
	 */
	private TargetData dataLayout;

	/**
	 * Can only called by subclass.
	 */
	protected TargetMachine(String name,
			boolean littleEndian,
			int ptrSize, int ptrAlign,
			int doubleAlign, int floatAlign,
			int longAlign, int intAlign,
			int shortAlign, int byteAlign)
	{
		this.name = name;
	}

	protected TargetMachine(String name)
	{
		this(name, false, 8, 8, 8, 4, 8, 4, 2, 1);
	}
}
