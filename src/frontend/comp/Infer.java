package frontend.comp;

import frontend.type.Type;
import frontend.type.TypeClass;

/**
 * @author JianpingZeng
 * @version 1.0
 */
public class Infer implements TypeClass
{
    /**
     * A value for prototypes that admit any frontend.type, including polymorphic ones.
     */
    public static final Type anyPoly = new Type(None, null);
}
