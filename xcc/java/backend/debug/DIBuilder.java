/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 * This software is subjected to the protection of BSD 3.0 Licence.
 * For more details, please refers to the LICENSE file.
 */

package backend.debug;

public class DIBuilder {
    public enum ComplexAddrKind {
        OpPlus(1),
        OpDeref(2);
        public final int value;
        ComplexAddrKind(int val) { value = val; }
    }
}
