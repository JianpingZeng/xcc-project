package utils; 

/**
 * A class that encodes and decodes source code positions. Source code
 *  positions are internally represented as integers that contain
 *  both column and line number information.
 */
public class Position {

    public Position() {
        super();
    }

    /**
      * Source file positions are integers in the format:
      *  line-number << LINESHIFT + column-number
      *  NOPOS represents an undefined position.
      */
    public static final int LINESHIFT = 10;
    public static final int COLUMNMASK = (1 << LINESHIFT) - 1;
    public static final int NOPOS = 0;
    public static final int FIRSTPOS = (1 << LINESHIFT) + 1;
    public static final int MAXPOS = Integer.MAX_VALUE;

    /**
     * The line number of the given position.
     */
    public static int line(int pos) {
        return pos >>> LINESHIFT;
    }

    /**
      * The column number of the given position.
      */
    public static int column(int pos) {
        return pos & COLUMNMASK;
    }

    /**
      * Form a position from a line number and a column number.
      */
    public static int make(int line, int col) {
        return (line << LINESHIFT) + col;
    }
}

