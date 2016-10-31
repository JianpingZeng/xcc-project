package tools;

public final class AsmUtils
{
    /**
     * 在对构造类型inxS存储空间分配的时候，用于计算对齐字节数。
     * 返回大于等于n的最小的alignment倍数.
     * @param n
     * @param alignment
     * @return
     */
    public static long align(long n, long alignment)
    {
        return (n + alignment - 1) / alignment * alignment;
    }
}
