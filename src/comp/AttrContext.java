package comp; 

import symbol.Scope;

/** 
 * Contains information specific to the attribute and enter
 * passes, to be used in place of the generic field in 
 * environments.
 * 
 * @author Jianping Zeng <z1215jping@hotmail.com>
 * @version 2016年1月15日 下午1:12:03 
 */
public class AttrContext
{
	public AttrContext()
	{
		super();
	}
	
    /**
     * The scope of local symbols.
     */
   public Scope scope = null;   
   
   /**
    * The numbers of enclosing static modifiers. 
    */
   public int staticLevel = 0;
   
   /**
    * Duplicate this context, replacing scope field and copying all others.
    */
   public AttrContext dup(Scope scope) {
       AttrContext info = new AttrContext();
       info.scope = scope;
       info.staticLevel = staticLevel;
       return info;
   }

   /**
     * Duplicate this context, copying all fields.
     */
   public AttrContext dup() {
       return dup(scope);
   }
}
