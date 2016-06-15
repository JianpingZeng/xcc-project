package lir.alloc;

import driver.Backend;
import lir.alloc.Interval.RegisterBinding;
import lir.alloc.Interval.RegisterBindingLists;
import lir.ci.LIRRegister;
import lir.ci.LIRValue;
import lir.ci.LIRVariable;
import utils.TTY;

import java.net.InterfaceAddress;
import java.util.ArrayList;
import java.util.List;

import static lir.alloc.Interval.EndMarker;
import static lir.alloc.Interval.State.Active;
import static lir.alloc.Interval.State.Handled;
import static lir.alloc.Interval.State.Inactive;

/**
 * @author Xlous.zeng
 */
public class SimpleWalker
{
	LinearScan allocator;
	Backend backend;
	RegisterBindingLists unhandledList;
	RegisterBindingLists activeList;
	RegisterBindingLists inactiveList;

	/**
	 * The current handling interval.
	 */
	Interval current = null;
	/*
	 * The starting position of the first range of current interval.
	 */
	int currentPos = -1;
	/**
	 * The instance of {@link MoveResolver} for inserting move operation when
	 * spliting interval.
	 */
	MoveResolver moveResolver;

	List<Interval>[] spillIntervals;
	int[] usePos;
	int[] blockPos;

	LIRRegister[] availableRegs;

	/**
	 * A switch for selecting any interval or fixed interval.
	 */
	RegisterBinding currentBinding;

	/**
	 * Creates a simple linear scan walker without complicate aoptimization.
	 * @param allocator
	 * @param unhandledFixed
	 * @param unhandledAny
	 */
	public SimpleWalker(LinearScan allocator, Interval unhandledFixed,
			Interval unhandledAny)
	{
		this.allocator = allocator;
		this.backend = allocator.backend;
		unhandledList = new RegisterBindingLists(unhandledFixed, unhandledAny);
		activeList = new RegisterBindingLists(EndMarker, EndMarker);
		inactiveList = new RegisterBindingLists(EndMarker, EndMarker);
		moveResolver = new MoveResolver(allocator);
		availableRegs = allocator.registers;
		int numRegs = allocator.registers.length;
		spillIntervals = new List[numRegs];
		for (int i = 0; i < numRegs; i++)
		{
			spillIntervals[i] = new ArrayList<>(8);
		}
		usePos = new int[numRegs];
		blockPos = new int[numRegs];

		// obtains the first interval to being handled
		nextInterval();
	}

	private void nextInterval()
	{
		RegisterBinding binding = null;
		Interval any = unhandledList.any;
		Interval fixed = unhandledList.fixed;

		if (any != Interval.EndMarker)
		{
			// intervals may start at same position . prefer fixed interval
			binding =
					fixed != Interval.EndMarker && fixed.from() <= any.from() ?
							RegisterBinding.Fixed :
							RegisterBinding.Any;

			assert binding == RegisterBinding.Fixed && fixed.from() <= any
					.from()
					|| binding == RegisterBinding.Any && any.from() <= fixed
					.from() : "wrong interval!!!";
			assert any == Interval.EndMarker || fixed == Interval.EndMarker
					|| any.from() != fixed.from() || binding
					== RegisterBinding.Fixed :
					"if fixed and any-Interval start at same position, "
							+ "fixed must be processed first";

		}
		else if (fixed != Interval.EndMarker)
		{
			binding = RegisterBinding.Fixed;
		}
		else
		{
			current = null;
			return;
		}

		currentBinding = binding;
		current = unhandledList.get(binding);
		unhandledList.set(binding, current.next);
		current.next = Interval.EndMarker;
		current.rewindRange();
	}

	/**
	 * Walks through all intervals.
	 */
	public void walk()
	{
		walkTo(Integer.MAX_VALUE);
	}

	private void walkTo(Interval.State state, int toOpID)
	{
		assert state == Active || state == Inactive : "wrong state";
		// walk through fixed and any register
		for (RegisterBinding binding : RegisterBinding.VALUES)
		{
			Interval prevprev = null;
			Interval prev = state == Active ? activeList.get(binding) :
					inactiveList.get(binding);
			Interval next = prev;
			while (next.from() <= toOpID)
			{
				Interval cur= next;
				next = cur.next;
				boolean rangeHasRanged = false;
				while (cur.currentTo() <= toOpID)
				{
					cur.nextRange();
					rangeHasRanged = true;
				}

				rangeHasRanged = rangeHasRanged || state == Inactive && cur.currentFrom() <= toOpID;
				if (rangeHasRanged)
				{
					if (prevprev == null)
					{
						// remove cur from list
						if (state == Active)
							activeList.set(binding, next);
						else
							inactiveList.set(binding, next);
					}
					else
					{
						prevprev.next = next;
					}
					prev = next;
					if (cur.currentAtEnd())
					{
						// move to handled state
						cur.state = Handled;
						intervalMoved(cur, binding, state, Handled);
					}
					else if (cur.currentFrom() <= toOpID)
					{
						activeList.addToListSortedByCurrentFromPositions(binding, cur);
						cur.state = Active;
						if (prev == cur)
						{
							assert state == Active;
							prevprev = prev;
							prev = cur.next;
						}
						intervalMoved(cur, binding, state, Active);
					}
					else
					{
						inactiveList.addToListSortedByCurrentFromPositions(binding, cur);
						cur.state = Inactive;
						if (prev == cur)
						{
							assert state == Inactive;
							prevprev = prev;
							prev = cur.next;
						}
						intervalMoved(cur, binding, state, Inactive);
					}
				}
				else
				{
					prevprev = prev;
					prev = cur.next;
				}
			}
		}
	}

	public void walkTo(int toOpId)
	{
		assert currentPos <= toOpId;
		while (current != null)
		{
			boolean isActive = current.from() <= toOpId;
			int opId = isActive ? current.from() : toOpId;

			// for debug information display
			if (!TTY.isSuppressed())
			{
				if (currentPos < opId)
				{
					TTY.println();
					TTY.println("walkTo(%d) *", opId);
				}
			}

			// set currentPosition prior to call of walkTo
			currentPos = opId;

			// checks for intervals in active that are expired or inactive
			walkTo(Active, opId);
			// checks for intervals in inactive that are expired or active
			walkTo(Inactive, opId);

			if (isActive)
			{
				current.state = Active;
				if (activateCurrent())
				{
					activeList.addToListSortedByCurrentFromPositions(
							currentBinding, current);

					intervalMoved(current, currentBinding,
							Interval.State.Unhandled, Active);
				}

				nextInterval();
			}
			else
			{
				return;
			}
		}
	}

	private void intervalMoved(Interval interval,
			RegisterBinding kind,
			Interval.State from, Interval.State to)
	{
		// intervalMoved() is called whenever an interval moves from one interval
		// list to another. In the implementation of this method it is prohibited
		// to move the interval to any list.
		if (!TTY.isSuppressed())
		{
			TTY.print(from.toString() + " to " + to.toString());
			TTY.fillTo(23);
			TTY.out().println(interval.logString(allocator));
		}
	}

	/**
	 * Allocates a physical register or memory stack slot to current.
	 * @return
	 */
	private boolean activateCurrent()
	{
		Interval interval = current;
		boolean result = true;

		LIRValue operand = interval.operand;
		// activating an interval that has assigned stack slot, split it
		// at first usage position.
		// Note that: this is called for handling function parameter
		if (interval.location() != null && interval.location().isStackSlot())
		{
			TTY.println("      interval has spill slot assigned (method parameter) "
					+ ". split it before first use");
			splitStackInterval(interval);
			result = false;
		}
		else
		{
			if (operand.isVariable() && allocator.operands.mustStartInMemory((LIRVariable)operand))
			{
				assert interval.location() == null : "register already assigned";
				allocator.assignSpillSlot(interval);

				// activating an interval that must start in a stack slot but may
				// get a register later used for lirRoundfp: rounding is done by
				// store to stack and reload later
				if (!allocator.operands.mustStayInMemory((LIRVariable)operand))
				{
					TTY.println("      interval must start in stack slot . split it before first use");
					splitStackInterval(interval);
				}
				result = false;
			}
			// interval has not assigned register, which is normal case for most interval
			else if (interval.location() == null)
			{
				TTY.println("      normal allocation of register");
				
			}
		}

		return result;
	}

	private void splitStackInterval(Interval interval)
	{

	}
}
