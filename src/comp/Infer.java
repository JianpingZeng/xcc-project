package comp; 

import type.TypeTags;
import type.Type;

/** 
 * @author Jianping Zeng <z1215jping@hotmail.com>
 * @version 2016年1月15日 下午7:55:23 
 */
public class Infer implements TypeTags
{
    /**
     * A value for prototypes that admit any type, including polymorphic ones.
     */
    public static final Type anyPoly = new Type(NONE, null);
}
