package backend.lir;

/**
 * @author Xlous.zeng
 */
public class CodeBlob
{
	private String name;
	/**
	 * total getTypeSize of CodeBlob in bytes.
	 */
	private int size;
	/**
	 * getTypeSize of header (depends on subclass).
	 */
	private int headerSize;
	/**
	 * getTypeSize of relocation.
	 */
	private int relocationSize;
	/**
	 * offset to where content region begin.
	 */
	private int contentOffset;
	/**
	 * offset to where instruction region begin.
	 */
	private int codeOffset;
	/**
	 * instruction offset in [0...frameCompleteOffset] have not finished setting
	 * up their frame. Beware of pc's in that ranges. There is similar range(s)
	 * on returns which we don't detect.
	 */
	private int frameCompleteOffset;

	/**
	 * offset to where datga region begins.
	 */
	private int dataOffset;

	/**
	 * getTypeSize of stack frame.
	 */
	private int frameSize;
}
