package lir.linearScan;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
import lir.LIRInstruction;
import lir.ci.LIRAddress;
import lir.ci.LIRKind;
import lir.ci.LIRRegisterValue;
import lir.ci.LIRValue;
import lir.ci.StackSlot;

/**
 * * Represents an interval in the {@linkplain LinearScan linear scanning register allocator}.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public final class Interval
{
	/**
	 * Sentinel interval to denote the end of an interval list.
	 */
	public static Interval EndMarker = new Interval(LIRValue.IllegalValue, -1) ;

	/**
	 * Constant used in opt of spilling of an interval.
	 */
	enum SpillState
	{
		/**
		 * Starting state of calculation: no definition found yet.
		 */
		NoDefinitionFound,
		/**
		 * One definition has already been found. Two consecutive definitions
		 * are treated as one  (e.g. a consecutive move and add because of two-
		 * operand LIR form). The position of this definition is given by
		 * {@link Interval#spillDefinitionPos()}.
		 */
		NoSpillStore,

		/**
		 * One spill move has already been inserted.
		 */
		OneSpillStore,

		/**
		 * The interval should be stored immediately after its definition to prevent
		 * multiple redundant stores.
		 */
		StoreAtDefinition,

		/**
		 * The interval starts in memory (e.g. method parameter), so a store is
		 * never necessary.
		 */
		StartInMemory,

		/**
		 * The interval has more than one definition (e.g. resulting from phi moves),
		 * so stores
		 * to memory are not optimized.
		 */
		NoOptimization
	}
	
	/**
	 * A set of interval lists ,one per {@linkplain RegisterBinding binding} type.
	 */
	static final class RegisterBindingLists
	{
		/**
		 * List of intervals whose binding is currently {@linkplain Interval.RegisterBinding#Fixed Fixed}.
		 */
		public Interval fixed;
		/**
		 * List of intervals whose binding is currently {@link Interval.RegisterBinding#Any}.
		 */
		public Interval any;

		public RegisterBindingLists(Interval fixed, Interval any)
		{
			this.fixed = fixed;
			this.any = any;
		}

		/**
		 * Gets the list for a specified binding.
		 *
		 * @param binding specifies the list to be returned
		 * @return the list of intervals whose binding is {@code binding}
		 */
		public Interval get(RegisterBinding binding)
		{
			if (binding == RegisterBinding.Any)
			{
				return any;
			}
			assert binding == RegisterBinding.Fixed;
			return fixed;
		}

		/**
		 * Sets the list for a specified binding.
		 *
		 * @param binding specifies the list to be replaced
		 * @param list       list of intervals whose binding is {@code binding}
		 */
		public void set(RegisterBinding binding, Interval list)
		{
			assert list != null;
			if (binding == RegisterBinding.Any)
			{
				any = list;
			}
			else
			{
				assert binding == RegisterBinding.Fixed;
				fixed = list;
			}
		}

		/**
		 * Adds an interval to a list sorted by {@linkplain Interval#currentFrom()
		 * current from} positions.
		 *
		 * @param binding  specifies the list to be updated
		 * @param interval the interval to add
		 */
		public void addToListSortedByCurrentFromPositions(
				RegisterBinding binding, Interval interval)
		{
			Interval list = get(binding);
			Interval prev = null;
			Interval cur = list;

			// a insertion sort algorithm was adopted
			while (interval.currentFrom() >= cur.currentFrom())
			{
				prev = cur;
				cur = cur.next;
			}

			if (prev == null)
			{
				// add to head of list
				list = interval;
			}
			else
			{
				// add before 'cur'
				prev.next = interval;
			}

			// extracts common expression out of if-else statement
			interval.next = cur;
			set(binding, list);
		}

		/**
		 * Adds an interval to a list sorted by {@linkplain Interval#from() start}
		 * positions and {@linkplain Interval#firstUsage(RegisterPriority) first
		 * usage} positions.
		 *
		 * @param binding  specifies the list to be updated
		 * @param interval the interval to add
		 */
		/*
		public void addToListSortedByStartAndUsePositions(
				RegisterBinding binding, Interval interval)
		{
			// get he interval list corresponding to binding
			Interval list = get(binding);
			Interval prev = null;
			Interval cur = list;
			while (cur.from() < interval.from() || (
					cur.from() == interval.from()
							&& cur.firstUsage(RegisterPriority.None) < interval
							.firstUsage(RegisterPriority.None)))
			{
				prev = cur;
				cur = cur.next;
			}
			if (prev == null)
			{
				list = interval;
			}
			else
			{
				prev.next = interval;
			}
			interval.next = cur;
			set(binding, list);
		}
		*/

		/**
		 * Removes an interval from a list corresponding to given binding.
		 *
		 * @param binding  specifies the list to be updated
		 * @param i the interval to remove
		 */
		public void remove(RegisterBinding binding, Interval i)
		{
			Interval list = get(binding);
			Interval prev = null;
			Interval cur = list;
			while (cur != i)
			{
				assert cur != null && cur != Interval.EndMarker :
						"interval has not been found in list: " + i;
				prev = cur;
				cur = cur.next;
			}
			if (prev == null)
			{
				set(binding, cur.next);
			}
			else
			{
				prev.next = cur.next;
			}
			// unlink the cur with list
			cur.next = null;
		}
	}


	/**
	 * Constants denoting whether an interval is bound to a specific register.
	 * This models platform dependencies on register usage for certain instructions.
	 */
	enum RegisterBinding
	{
		Fixed,

		Any;

		public static final RegisterBinding[] VALUES = values();
	}

	/**
	 * Constants denoting the state of an interval on linear scan may be with respect
	 * to the {@linkplain Interval#from() start} {@code position} of the interval
	 * being processed.
	 */
	enum State
	{
		/**
		 * An interval that starts after {@code position}.
		 */
		Unhandled,

		/**
		 * An interval that {@linkplain Interval#covers covers} {@code position}
		 * and has a physical register assigned.
		 */
		Active,
		/**
		 * An interval that starts before and ends after {@code position} but does
		 * not {@linkplain #covers} covers} it due to a lifetime hole.
		 */
		Inactive,
		/**
		 * An interval that ends before {@code position} or and spilled to memory.
		 */
		Handled;
	}

	/**
	 * The {@linkplain lir.ci.LIRRegisterValue register} or
	 * {@linkplain lir.ci.LIRVariable variable} for register allocation.
	 */
	public final LIRValue operand;

	/**
	 * The {@linkplain OperandPool#operandNumber(LIRValue) operand number} for
	 * this interval's {@linkplain #operand operand}.
	 */
	public final int operandNumber;

	/**
	 * The {@linkplain LIRRegisterValue register}, {@linkplain StackSlot spill slot}
	 * or {@linkplain LIRAddress memory address} assigned to this interval.
	 */
	private LIRValue location;

	/**
	 * The stack slot to which all splits of this interval are spilled if necessary.
	 */
	private StackSlot spillSlot;

	/**
	 * For spill move opt.
	 */
	private SpillState spillState;
	
	/**
	 * The kind of this interval. Only valid if this is a variable.
	 */
	private LIRKind kind;

	/**
	 * The head of the list of ranges describing this interval. This list is sorted
	 * by {@linkplain LIRInstruction#id instruction ids}. Because the lifetime
	 * interval of virtual register is represented as multiple range rather than
	 * only one. Further, every instance of Range consists of a pair of two integer,
	 * the first part is meant to the number of definition instruction, and second
	 * one is the number of nearest usage.
	 */
	private Range first;

	/**
	 * Iterator used to traverse the ranges of an interval.
	 */
	private Range current;

	/**
	 * Link to next interval in a sorted list of intervals that ends with
	 * {@link #EndMarker}.
	 */
	Interval next;

	State state;
	
	int spillDefinitionPos;

	public Interval(LIRValue operand, int operandNumber)
	{
		assert operand != null : "can not assign interval for null";
		this.operand = operand;
		this.operandNumber = operandNumber;
		if (operand.isRegister())
		{
			location = operand;
		}
		else
		{
			assert operand.isIllegal() || operand.isVariable();
		}
		kind = LIRKind.Illegal;
		first = Range.EndMarker;
		current = Range.EndMarker;
		next = EndMarker;
	}

	int currentFrom()
	{
		return current.from;
	}

	int currentTo()
	{
		return current.to;
	}

	StackSlot spillSlot()
	{
		return spillSlot;
	}

	void setSpillSlot(StackSlot slot)
	{
		assert this.spillSlot != null
				:"connot overwrite existing spill slot";
		this.spillSlot = slot;
	}

	// test intersection
	boolean intersects(Interval i)
	{
		return first.intersects(i.first);
	}

	int intersectsAt(Interval i)
	{
		return first.intersectsAt(i.first);
	}

	/**
	 * Assigns a stack or register to this interval.
	 * @param location
	 */
	void assignLocation(LIRValue location)
	{
		if (location.isRegister())
		{
			assert this.location == null :
					"cannot re-assign location for " + this;
			if (location.kind == LIRKind.Illegal && kind != LIRKind.Illegal)
			{
				location = location.asRegister().asValue(kind);
			}
		}
		else
		{
			assert this.location == null || this.location.isRegister() :
					"cannot re-assign location for " + this;
			assert location.isStackSlot();
			assert location.kind != LIRKind.Illegal;
			assert location.kind == this.kind;
		}
		this.location = location;
	}

	/**
	 * Obtains the assigned location, which is stack memory or register.
	 * @return
	 */
	public LIRValue location() {return location;}

	public LIRKind kind()
	{
		assert !location.isRegister() : "can not access type for fixed interval";
		return kind;
	}

	void setKind(LIRKind kind)
	{
		assert operand.isRegister() || this.kind == LIRKind.Illegal
				|| this.kind == kind : "overwriting existing type";
		assert kind == kind.stackKind() || kind
				== LIRKind.Short : "these kinds should have int type registers";
		this.kind = kind;
	}

	public Range first()
	{
		return first;
	}

	int from()
	{
		return first.from;
	}

	int to()
	{
		return  calTo();
	}

	private int calTo()
	{
		assert first != Range.EndMarker : "interval has no range";
		Range cur = first;
		while (cur.next != Range.EndMarker)
		{
			cur = cur.next;
		}
		return cur.to;
	}

	/**
	 * returns true if the opId is inside the interval.
	 * @param opId
	 * @param mode
	 * @return
	 */
	boolean covers(int opId, LIRInstruction.OperandMode mode)
	{
		Range cur = first;
		while (cur != Range.EndMarker && cur.to < opId)
		{
			cur = cur.next;
		}
		if (cur != Range.EndMarker)
		{
			assert cur.to != cur.next.from : "can not separately";
			if (mode == LIRInstruction.OperandMode.Output)
			{
				return opId >= cur.from && opId < opId;
			}
			else
			{
				return opId >= cur.from && opId <= opId;
			}
		}
		return false;
	}

	/**
	 * returns true if the interval has any hole between
	 * <pre>
	 *      [holeFrom, holeTo]
	 * </pre>
	 * (even if the hole has only the length 1).
	 * @param holeFrom
	 * @param holeTo
	 * @return
	 */
	boolean hasHoleBetween(int holeFrom, int holeTo)
	{
		assert holeFrom < holeTo;
		assert from()<= holeFrom && holeTo <= to() : "out of range";

		Range cur = first;
		while (cur != Range.EndMarker)
		{
			assert cur.to < cur.next.from : "no space between ranges";
			// hole-range starts before this range . hole
			if (holeFrom < cur.from)
			{
				return true;
			}
			else
			{
				// hole-range completely inside this range . no hole
				if (holeTo <= cur.to)
				{
					return false;
				}
				// overlapping. hole
				else if ( holeFrom <= cur.to)
				{
					return true;
				}
			}
			cur = cur.next;
		}
		return false;
	}

	void addRange(int from, int to)
	{
		assert from < to;
		assert first == Range.EndMarker || to < first.next.from;

		if (to >= first.from)
		{
			assert first != Range.EndMarker;
			first.from = Math.min(from, first.from);
			first.to = Math.max(to, first.to);
		}
		else
		{
			first = new Range(from, to, first);
		}
	}

	@Override
	public String toString()
	{
		String from = "?";
		String to = "?";
		if (first != null && first != Range.EndMarker)
		{
			from = String.valueOf(from());
			to = String.valueOf(to());
		}
		String loc = this.location == null ? "" : "@" + location.name();
		return operandNumber + ":" + operand +
				(location.isRegister() ? "" : loc)
				+ "[" + from + ", " + to + "]";
	}

	/**
	 * Gets a single line string for logging the details of this interval to a
	 * log stream.
	 *
	 * @param allocator the register allocator context
	 */
	public String logString(LinearScan allocator)
	{
		StringBuilder buf = new StringBuilder(100);
		buf.append(operandNumber).append(':').append(operand).append(' ');
		if (!operand.isRegister())
		{
			if (location != null)
			{
				buf.append("location{").append(location).append("} ");
			}
		}

		buf.append("} ranges{");

		// print ranges
		Range cur = first;
		while (cur != Range.EndMarker)
		{
			if (cur != first)
			{
				buf.append(", ");
			}
			buf.append(cur);
			cur = cur.next;
			assert cur != null : "range list not closed with range sentinel";
		}
		buf.append("}");
		return buf.toString();
	}
	/**
	 * Advances to the next usage range.
	 */
	void nextRange()
    {
		assert this != EndMarker : "not allowed on sentinel";
		current = current.next;
    }
	/**
	 * Checks if current rage approach the sentinel.
	 * @return 
	 */
	boolean currentAtEnd()
	{
		return current == Range.EndMarker;
	}
	/**
	 * reset the first range of current interval.
	 */
	void rewindRange()
	{
		current = first;	
	}
	
	int currentIntersectsAt(Interval it)
	{
		return current.intersectsAt(it.current);
	}
	
	boolean currentIntersects(Interval it)
	{
		return current.intersects(it.current);
	}
	
	/**
	 * for spill opt.
	 * @return
	 */
	public SpillState spillState()
	{
		return this.spillState;
	}
	
	void setSpillState(SpillState state)
	{
		assert state.ordinal() >= spillState()
				.ordinal() : "state cannot decrease";
		this.spillState = state;
	}
	
	/**
	 * returns true if this interval has a shadow copy on the stack that is always correct.
	 * @return
	 */
	boolean alwaysInMemory()
	{
		return spillState == SpillState.StoreAtDefinition
				|| spillState == SpillState.StartInMemory;
	}
	

	int spillDefinitionPos()
	{
		return spillDefinitionPos;
	}

	void setSpillDefinitionPos(int pos)
	{
		assert spillDefinitionPos() == -1 : "cannot set the position twice";
		spillDefinitionPos = pos;
	}
}
