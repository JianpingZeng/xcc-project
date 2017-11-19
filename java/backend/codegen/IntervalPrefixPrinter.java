package backend.codegen;

/**
 * @author Xlous.zeng
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
