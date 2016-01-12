/**
 * @(#)LayoutCharacters.java	1.11 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package utils;

/**
 * An interface containing layout character constants used in Java
 *  programs.
 */
public interface LayoutCharacters {

    /**
     * Tabulator column increment.
     */
    static final int TabInc = 8;

    /**
     * Tabulator character.
     */
    static final byte TAB = 8;

    /**
     * Line feed character.
     */
    static final byte LF = 10;

    /**
     * Form feed character.
     */
    static final byte FF = 12;

    /**
     * Carriage return character.
     */
    static final byte CR = 13;

    /**
     * End of input character.  Used as a sentinel to denote the
     *  character one beyond the last defined character in a
     *  source file.
     */
    static final byte EOI = 26;
}
