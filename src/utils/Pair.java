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
public class Pair {
    public final Object fst;
    public final Object snd;

    public Pair(Object fst, Object snd) {
        super();
        this.fst = fst;
        this.snd = snd;
    }

    private static boolean equals(Object x, Object y) {
        return (x == null && y == null) || (x != null && x.equals(y));
    }

    public boolean equals(Object other) {
        return other instanceof Pair && equals(fst, ((Pair) other).fst) &&
                equals(snd, ((Pair) other).snd);
    }

    public int hashCode() {
        if (fst == null)
            return snd.hashCode() + 1;
        else if (snd == null)
            return fst.hashCode() + 2;
        else
            return fst.hashCode() * snd.hashCode();
    }
}
