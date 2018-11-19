/**
 * @(#)LayoutCharacters.java 1.11 03/01/23
 * <p>
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package tools;

/**
 * An interface containing layout character constants used in Java
 * programs.
 */
public interface LayoutCharacters {

  /**
   * Tabulator column increment.
   */
  int TabInc = 8;

  /**
   * Tabulator character.
   */
  byte TAB = 8;

  /**
   * Line feed character.
   */
  byte LF = 10;

  /**
   * Form feed character.
   */
  byte FF = 12;

  /**
   * Carriage return character.
   */
  byte CR = 13;

  /**
   * End of input character.  Used as a sentinel to denote the
   * character one beyond the last defined character in a
   * source file.
   */
  byte EOI = 26;
}
