package utils; 

/**
 * A class that encodes and decodes source code positions. Source code
 *  positions are internally represented as integers that contain
 *  both column and line id information.
 */
public class Position {

    public Position() {
        super();
    }

    /**
      * Source file positions are integers in the format:
      *  line-id << LINESHIFT + column-id
      *  NOPOS represents an undefined position.
      */
    public static final int LINESHIFT = 10;
    public static final int COLUMNMASK = (1 << LINESHIFT) - 1;
    public static final int NOPOS = 0;
    public static final int FIRSTPOS = (1 << LINESHIFT) + 1;
    public static final int MAXPOS = Integer.MAX_VALUE;

    /**
     * The line id of the given position.
     */
    public static int line(int pos) {
        return pos >>> LINESHIFT;
    }

    /**
      * The column id of the given position.
      */
    public static int column(int pos) {
        return pos & COLUMNMASK;
    }

    /**
      * Form a position from a line id and a column id.
      */
    public static int make(int line, int col) {
        return (line << LINESHIFT) + col;
    }
}

