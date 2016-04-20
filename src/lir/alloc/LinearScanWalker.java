package lir.alloc;

public final class LinearScanWalker extends IntervalWalker
{
	public LinearScanWalker(LinearScan allocator,
			Interval unhandledFixed,
			Interval unhandledAny)
	{
		super(allocator, unhandledFixed, unhandledAny);
	}

	public void walk()
	{

	}

	public void finishAllocation()
	{

	}
}