package backend.lir.alloc;

import tools.TTY;
import driver.*;

/**
 * @author Xlous.zeng
 */
public class IntervalWalker
{

	protected final Backend backend;
	protected final LinearScan allocator;

	/**
	 * Sorted list of intervals, not live before the current position.
	 */
	Interval.RegisterBindingLists unhandledLists;

	/**
	 * Sorted list of intervals assigned with physic register and intersects with
	 * current handling interval.
	 */
	Interval.RegisterBindingLists activeLists;

	/**
	 * Sorted list of intervals assigned with a physical register, but in a life
	 * time hole at the current position, this list just for optimal allocation
	 * through making use of the hole of interval.
	 */
	Interval.RegisterBindingLists inactiveLists;

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
	protected Interval.RegisterBinding currentBinding;

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
	 * @param unhandledFixed the list of unhandled {@linkplain Interval.RegisterBinding#Fixed
	 *                          fixed} intervals
	 * @param unhandledAny   the list of unhandled {@linkplain Interval.RegisterBinding#Any
	 *                          non-fixed} intervals
	 */
	IntervalWalker(LinearScan allocator, Interval unhandledFixed,
			Interval unhandledAny)
	{
		this.backend = allocator.backend;
		this.allocator = allocator;

		unhandledLists = new Interval.RegisterBindingLists(unhandledFixed, unhandledAny);

		activeLists = new Interval.RegisterBindingLists(Interval.EndMarker,
				Interval.EndMarker);
		inactiveLists = new Interval.RegisterBindingLists(Interval.EndMarker,
				Interval.EndMarker);
		currentPosition = -1;
		current = null;
		nextInterval();
	}

	void removeFromList(Interval interval)
	{
		if (interval.state == Interval.State.Active)
		{
			activeLists.remove(Interval.RegisterBinding.Any, interval);
		}
		else
		{
			assert interval.state == Interval.State.Inactive : "invalid state";
			inactiveLists.remove(Interval.RegisterBinding.Any, interval);
		}
	}

	void walkTo(Interval.State state, int from)
	{
		assert state == Interval.State.Active || state == Interval.State.Inactive : "wrong state";

		// handles precolored and unprecolored interval in Active or Inactive list
		for (Interval.RegisterBinding binding : Interval.RegisterBinding.VALUES)
		{
			Interval prevprev = null;
			Interval prev = (state == Interval.State.Active) ?
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
				rangeHasChanged = rangeHasChanged || (state == Interval.State.Inactive
						&& cur.currentFrom() <= from);

				if (rangeHasChanged)
				{
					// Step#1: remove cur from list
					if (prevprev == null)
					{
						if (state == Interval.State.Active)
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
						cur.state = Interval.State.Handled;
						intervalMoved(cur, binding, state, Interval.State.Handled);
					}
					else if (cur.currentFrom() <= from)
					{
						// sort into active list
						activeLists
								.addToListSortedByCurrentFromPositions(binding,
										cur);
						cur.state = Interval.State.Active;
						if (prev == cur)
						{
							assert state == Interval.State.Active : "check";
							prevprev = prev;
							prev = cur.next;
						}
						intervalMoved(cur, binding, state, Interval.State.Active);
					}
					else
					{
						// sort into inactive list
						inactiveLists
								.addToListSortedByCurrentFromPositions(binding,
										cur);
						cur.state = Interval.State.Inactive;
						if (prev == cur)
						{
							assert state == Interval.State.Inactive : "check";
							prevprev = prev;
							prev = cur.next;
						}
						intervalMoved(cur, binding, state, Interval.State.Inactive);
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
		Interval.RegisterBinding binding;

		Interval any = unhandledLists.any;
		Interval fixed = unhandledLists.fixed;

		// first, choose the fixed list or any list according to whether the starting
		// position of the first fixed interval is before
		if (any != Interval.EndMarker)
		{
			// intervals may start at same position . prefer fixed interval
			binding =
					fixed != Interval.EndMarker && fixed.from() <= any.from() ?
							Interval.RegisterBinding.Fixed :
							Interval.RegisterBinding.Any;

			assert binding == Interval.RegisterBinding.Fixed && fixed.from() <= any
					.from()
					|| binding == Interval.RegisterBinding.Any && any.from() <= fixed
					.from() : "wrong interval!!!";

			assert any == Interval.EndMarker || fixed == Interval.EndMarker
					|| any.from() != fixed.from() || binding
					== Interval.RegisterBinding.Fixed :
					"if fixed and any-Interval start at same position, fixed must be processed first";

		}
		else if (fixed != Interval.EndMarker)
		{
			binding = Interval.RegisterBinding.Fixed;
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
			walkTo(Interval.State.Active, opId);
			// checks for intervals in inactive that are expired or active
			walkTo(Interval.State.Inactive, opId);

			if (isActive)
			{
				current.state = Interval.State.Active;

				// activating current that just means assigning the register to
				// current interval
				if (activateCurrent())
				{
					// add current interval into the active list
					activeLists.addToListSortedByCurrentFromPositions(
							currentBinding, current);
					intervalMoved(current, currentBinding,
							Interval.State.Unhandled, Interval.State.Active);
				}

				nextInterval();
			}
			else
			{
				return;
			}
		}
	}

	private void intervalMoved(Interval interval, Interval.RegisterBinding kind,
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
}


