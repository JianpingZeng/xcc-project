package lir.ci;

/**
 * Represents a compiler spill slot or an outgoing stack-based argument in a method's frame
 * or an incoming stack-based argument in a method's {@linkplain #inCallerFrame() caller's frame}.
 */
public final class StackSlot extends CiValue
{
	/**
	 *
	 */
	private static final long serialVersionUID = 2463192016899841921L;

	/**
	 * @see StackSlot#index()
	 */
	private final int index;

	/**
	 * Gets a {@link StackSlot} instance representing a stack slot in the current frame
	 * at a given index holding a value of a given kind.
	 *
	 * @param kind  the kind of the value stored in the stack slot
	 * @param index the index of the stack slot
	 */
	public static StackSlot get(CiKind kind, int index)
	{
		return get(kind, index, false);
	}

	/**
	 * Gets a {@link StackSlot} instance representing a stack slot at a given index
	 * holding a value of a given kind.
	 *
	 * @param kind          the kind of the value stored in the stack slot
	 * @param index         the index of the stack slot
	 * @param inCallerFrame specifies if the slot is in the current frame or in the caller's frame
	 */
	public static StackSlot get(CiKind kind, int index, boolean inCallerFrame)
	{

		StackSlot[][] cache = inCallerFrame ? CALLER_FRAME_CACHE : CACHE;
		StackSlot[] slots = cache[kind.ordinal()];
		StackSlot slot;
		if (index < slots.length)
		{
			slot = slots[index];
		}
		else
		{
			slot = new StackSlot(kind, inCallerFrame ? -(index + 1) : index);
		}
		assert slot.inCallerFrame() == inCallerFrame;
		return slot;
	}

	/**
	 * Private constructor to enforce use of {@link #get(CiKind, int)} so that the
	 * shared instance {@linkplain #CACHE cache} is used.
	 */
	private StackSlot(CiKind kind, int index)
	{
		super(kind);
		this.index = index;
	}

	/**
	 * Gets the index of this stack slot. If this is a spill slot or outgoing stack argument to a call,
	 * then the return value is relative to the stack pointer. Otherwise this is an incoming stack
	 * argument and the return value is relative to the frame pointer.
	 *
	 * @return the index of this slot
	 * @see #inCallerFrame()
	 */
	public int index()
	{
		return index < 0 ? -(index + 1) : index;
	}

	public int rawIndex()
	{
		return index;
	}

	@Override public int hashCode()
	{
		return kind.ordinal() + index;
	}

	@Override public boolean equals(Object o)
	{
		if (o == this)
		{
			return true;
		}
		if (o instanceof StackSlot)
		{
			StackSlot l = (StackSlot) o;
			return l.kind == kind && l.index == index;
		}
		return false;
	}

	@Override public boolean equalsIgnoringKind(CiValue o)
	{
		if (o == this)
		{
			return true;
		}
		if (o instanceof StackSlot)
		{
			StackSlot l = (StackSlot) o;
			return l.index == index;
		}
		return false;
	}

	@Override public String name()
	{
		return (inCallerFrame() ? "caller-stack" : "stack:") + index();
	}

	/**
	 * Determines if this is a stack slot in the caller's frame.
	 */
	public boolean inCallerFrame()
	{
		return index < 0;
	}

	/**
	 * Gets this stack slot used to pass an argument from the perspective of a caller.
	 */
	public StackSlot asOutArg()
	{
		if (inCallerFrame())
		{
			return get(kind, index(), false);
		}
		return this;
	}

	/**
	 * Gets this stack slot used to pass an argument from the perspective of a callee.
	 */
	public StackSlot asInArg()
	{
		if (!inCallerFrame())
		{
			return get(kind, index(), true);
		}
		return this;
	}

	/**
	 * Default size of the cache to generate per kind.
	 */
	private static final int CACHE_PER_KIND_SIZE = 100;

	private static final int CALLER_FRAME_CACHE_PER_KIND_SIZE = 10;

	/**
	 * A cache of {@linkplain #inCallerFrame() non-caller-frame} stack slots.
	 */
	private static final StackSlot[][] CACHE = makeCache(CACHE_PER_KIND_SIZE,
			false);

	/**
	 * A cache of {@linkplain #inCallerFrame() caller-frame} stack slots.
	 */
	private static final StackSlot[][] CALLER_FRAME_CACHE = makeCache(
			CALLER_FRAME_CACHE_PER_KIND_SIZE, true);

	private static StackSlot[][] makeCache(int cachePerKindSize,
			boolean inCallerFrame)
	{
		StackSlot[][] cache = new StackSlot[CiKind.VALUES.length][];
		cache[CiKind.Illegal.ordinal()] = makeCacheForKind(CiKind.Illegal,
				cachePerKindSize, inCallerFrame);
		cache[CiKind.Int.ordinal()] = makeCacheForKind(CiKind.Int,
				cachePerKindSize, inCallerFrame);
		cache[CiKind.Long.ordinal()] = makeCacheForKind(CiKind.Long,
				cachePerKindSize, inCallerFrame);
		cache[CiKind.Float.ordinal()] = makeCacheForKind(CiKind.Float,
				cachePerKindSize, inCallerFrame);
		cache[CiKind.Double.ordinal()] = makeCacheForKind(CiKind.Double,
				cachePerKindSize, inCallerFrame);
		cache[CiKind.Object.ordinal()] = makeCacheForKind(CiKind.Object,
				cachePerKindSize, inCallerFrame);
		return cache;
	}

	/**
	 * Creates an array of {@code StackSlot} objects for a given {@link CiKind}.
	 * The {@link #index} values range from {@code 0} to {@code count - 1}.
	 *
	 * @param kind  the {@code CiKind} of the stack slot
	 * @param count the size of the array to create
	 * @return the generated {@code StackSlot} array
	 */
	private static StackSlot[] makeCacheForKind(CiKind kind, int count,
			boolean inCallerFrame)
	{
		StackSlot[] slots = new StackSlot[count];
		for (int i = 0; i < count; ++i)
		{
			slots[i] = new StackSlot(kind, inCallerFrame ? -(i + 1) : i);
		}
		return slots;
	}
}
