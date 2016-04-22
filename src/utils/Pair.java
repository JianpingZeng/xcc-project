/**
 * @(#)Pair.java	1.10 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package utils;

/**
 * A generic class for pairs.
 */
public class Pair<K, V> {
    public K first;
    public V second;

    public Pair(K first, V second) {
        super();
        this.first = first;
        this.second = second;
    }

    private static boolean equals(Object x, Object y) {
        return (x == null && y == null) || (x != null && x.equals(y));
    }

    public boolean equals(Object other) {
        return other instanceof Pair && equals(first, ((Pair) other).first) &&
                equals(second, ((Pair) other).second);
    }

    public int hashCode() {
        if (first == null)
            return second.hashCode() + 1;
        else if (second == null)
            return first.hashCode() + 2;
        else
            return first.hashCode() * second.hashCode();
    }
}
