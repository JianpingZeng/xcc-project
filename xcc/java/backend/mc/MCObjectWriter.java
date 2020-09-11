/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 * This software is subjected to the protection of BSD 3.0 Licence.
 * For more details, please refers to the LICENSE file.
 */

package backend.mc;

public class MCObjectWriter {
    public static void encodeSLEB128(long value, StringBuilder os) {
        boolean more;
        do {
            int tmp = (int) (value & 0x7f);
            value >>= 7;
            more = !((value == 0 && (tmp & 0x40) == 0) ||
                    (value == -1 && (tmp & 0x40) != 0));
            if (more)
                tmp |= 0x80;
            os.append((byte)tmp);
        }while (more);
    }

    public static void encodeULEB128(long value, StringBuilder os) {
        do {
            int tmp = (int) (value & 0x7f);
            value >>>= 7;
            if (value != 0)
                tmp |= 0x80;
            os.append((byte)tmp);
        }while (value != 0);
    }
}
