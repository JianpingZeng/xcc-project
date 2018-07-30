package backend.codegen;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public class IntervalPrefixPrinter extends PrefixPrinter
{
    private LiveInterval liveInterval;

    public IntervalPrefixPrinter(LiveInterval interval)
    {
        liveInterval = interval;
    }
}
