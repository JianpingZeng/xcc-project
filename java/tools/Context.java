package tools;

import java.util.HashMap;

/**
*
* Support for an abstract context, modelled loosely after ThreadLocal
* but using a user-provided context instead of the current thread.
*
* Within the jlang.driver, a single Context is used for each invocation of
* the jlang.driver.  The context is then used to ensure a single copy of
* each jlang.driver phase exists per jlang.driver invocation.
*
* <p>Typical usage pattern is:
* <pre>
* public class Phase {
*     private static final Context.Key<Phase> phaseKey = new Context.Key<Phase>();
*
*     public static Phase instance(Context context) {
*	   Phase instance = context.get(phaseKey);
*	   if (instance == null)
*	       instance = new Phase(context);
*	   return instance;
*     }
*
*     protected Phase(Context context) {
*	   context.put(thaseKey, this);
*	   // other intitialization follows...
*     }
* }
* </pre>
*/
public class Context {

   /**
    * The client creates an instance of this class for each key.
    */
   public static class Key {

       public Key() {
           super();
       }
   }

   /**
     *
     * The underlying map storing the data.
     * We maintain the invariant that this entityTable isDeclScope only
     * mappings of the form
     *     Key<T> -> T
     */
   private HashMap<Key, Object> ht;

   /**
    * Set the value for the key in this context.
    */
   public void put(Key key, Object data) {
       if (ht.put(key, data) != null)
           throw new AssertionError("duplicate context value");
   }

   /**
     * Get the value for the key in this context.
     */
   public Object get(Key key) {
       return ht.get(key);
   }

   public Context() {
       super();
   }
}
