package backend.lir.linearScan;

import driver.*;
import backend.lir.linearScan.Interval.RegisterBinding;
import backend.lir.linearScan.Interval.RegisterBindingLists;
import tools.TTY;
import static backend.lir.linearScan.Interval.State;
import static backend.lir.linearScan.Interval.State.*;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class IntervalWalker
{

	protected final Backend backend;
	protected final LinearScan allocator;

	/**
	 * Sorted list of intervals, not live before the current position.
	 */
	RegisterBindingLists unhandledLists;

	/**
	 * Sorted list of intervals assigned with physic register and intersects with
	 * current handling interval.
	 */
	RegisterBindingLists activeLists;

	/**
	 * Sorted list of intervals assigned with a physical register, but in a life
	 * time hole at the current position, this list just for optimal allocation
	 * through making use of the hole of interval.
	 */
	RegisterBindingLists inactiveLists;

	/**
	 * The current interval (taken from the unhandled list) being processed.
	 */
	protected Interval current;

	/**
	 * The current position (the starting position of {@linkplain #current Current
	 * handling Interval}).
	 */
	protected int currentPosition;

	/**
	 * The binding of the current interval being processed used for selecting which
	 * Interval list, either precolored interval or un precolored interval.
	 */
	protected RegisterBinding currentBinding;

	/**
	 * Processes the {@linkplain #current} interval in an attempt to allocate a physical
	 * register to it and thus allow it to be moved to a list of {@linkplain #activeLists
	 * active} intervals.
	 *
	 * @return {@code true} if a register was allocated to the {@linkplain #current}
	 * interval
	 */
	boolean activateCurrent()
	{
		return true;
	}

	void walkBefore(int lirOpId)
	{
		walkTo(lirOpId - 1);
	}

	void walk()
	{
		walkTo(Integer.MAX_VALUE);
	}

	/**
	 * Creates a new interval walker.
	 *
	 * @param allocator      the register allocator context
	 * @param unhandledFixed the list of unhandled {@linkplain RegisterBinding#Fixed
	 *                          fixed} intervals
	 * @param unhandledAny   the list of unhandled {@linkplain RegisterBinding#Any
	 *                          non-fixed} intervals
	 */
	IntervalWalker(LinearScan allocator, Interval unhandledFixed,
			Interval unhandledAny)
	{
		this.backend = allocator.backend;
		this.allocator = allocator;

		unhandledLists = new RegisterBindingLists(unhandledFixed, unhandledAny);

		activeLists = new RegisterBindingLists(Interval.EndMarker,
				Interval.EndMarker);
		inactiveLists = new RegisterBindingLists(Interval.EndMarker,
				Interval.EndMarker);
		currentPosition = -1;
		current = null;
		nextInterval();
	}

	void removeFromList(Interval interval)
	{
		if (interval.state == Active)
		{
			activeLists.remove(RegisterBinding.Any, interval);
		}
		else
		{
			assert interval.state == Inactive : "invalid state";
			inactiveLists.remove(RegisterBinding.Any, interval);
		}
	}

	void walkTo(State state, int from)
	{
		assert state == Active || state == Inactive : "wrong state";

		// handles precolored and unprecolored interval in Active or Inactive list
		for (RegisterBinding binding : RegisterBinding.VALUES)
		{
			Interval prevprev = null;
			Interval prev = (state == Active) ?
					activeLists.get(binding) :
					inactiveLists.get(binding);

			Interval next = prev;
			while (next.currentFrom() <= from)
			{
				Interval cur = next;
				next = cur.next;

				boolean rangeHasChanged = false;
				while (cur.currentTo() <= from)
				{
					cur.nextRange();
					rangeHasChanged = true;
				}

				// also handle move from inactive list to active list
				rangeHasChanged = rangeHasChanged || (state == Inactive
						&& cur.currentFrom() <= from);

				if (rangeHasChanged)
				{
					// Step#1: remove cur from list
					if (prevprev == null)
					{
						if (state == Active)
						{
							activeLists.set(binding, next);
						}
						else
						{
							inactiveLists.set(binding, next);
						}
					}
					else
					{
						prevprev.next = next;
					}
					prev = next;
					// Step#2: take insertion that append the cur into handled list
					if (cur.currentAtEnd())
					{
						// move to handled state (not maintained as a list)
						cur.state = Handled;
						intervalMoved(cur, binding, state, Handled);
					}
					else if (cur.currentFrom() <= from)
					{
						// sort into active list
						activeLists
								.addToListSortedByCurrentFromPositions(binding,
										cur);
						cur.state = Active;
						if (prev == cur)
						{
							assert state == Active : "check";
							prevprev = prev;
							prev = cur.next;
						}
						intervalMoved(cur, binding, state, Active);
					}
					else
					{
						// sort into inactive list
						inactiveLists
								.addToListSortedByCurrentFromPositions(binding,
										cur);
						cur.state = Inactive;
						if (prev == cur)
						{
							assert state == Inactive : "check";
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

	/**
	 * Removes the first interval from {@linkplain #unhandledLists UnhandledLists}
	 * for register allocation.
	 */
	void nextInterval()
	{
		RegisterBinding binding;

		Interval any = unhandledLists.any;
		Interval fixed = unhandledLists.fixed;

		// first, choose the fixed list or any list according to whether the starting
		// position of the first fixed interval is before
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
					"if fixed and any-Interval start at same position, fixed must be processed first";

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
		current = unhandledLists.get(binding);

		// after removing the first Interval which sources from fixed list or any list
		// according to the binding
		unhandledLists.set(binding, current.next);
		current.next = Interval.EndMarker;

		// reset the first range of current interval
		current.rewindRange();
	}

	/**
	 * Walks through all intervals.
	 *
	 * @param toOpId
	 */
	void walkTo(int toOpId)
	{
		assert currentPosition <= toOpId : "can not walk backwards";
		while (current != null)
		{
			boolean isActive = current.from() <= toOpId;
			int opId = isActive ? current.from() : toOpId;

			// for debugging information display
			if (!TTY.isSuppressed())
			{
				if (currentPosition < opId)
				{
					TTY.println();
					TTY.println("walkTo(%d) *", opId);
				}
			}

			// set currentPosition prior to call of walkTo
			currentPosition = opId;

			// checks for intervals in active that are expired or inactive
			walkTo(Active, opId);
			// checks for intervals in inactive that are expired or active
			walkTo(Inactive, opId);

			if (isActive)
			{
				current.state = Active;

				// activating current that just means assigning the register to
				// current interval
				if (activateCurrent())
				{
					// add current interval into the active list
					activeLists.addToListSortedByCurrentFromPositions(
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

	private void intervalMoved(Interval interval, RegisterBinding kind,
			State from, State to)
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
}


