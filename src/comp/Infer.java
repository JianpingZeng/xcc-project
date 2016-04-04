package comp; 

import type.TypeTags;
import type.Type;

/**
 * @author JianpingZeng
 * @version 1.0
 */
public class Infer implements TypeTags
{
    /**
     * A value for prototypes that admit any type, including polymorphic ones.
     */
    public static final Type anyPoly = new Type(NONE, null);
}
