package utils;


public final class AsmUtils {
    private AsmUtils() {}

    /**
     * 在对构造类型inxS存储空间分配的时候，用于计算对齐字节数。
     * @param n	
     * @param alignment
     * @return
     */
    static public long align(long n, long alignment) {
        return (n + alignment - 1) / alignment * alignment;
    }
    // #@@}
}
