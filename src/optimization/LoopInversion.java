package optimization; 
/** 
 * <p>
 * This pass was designed to reduce the number of branch instruction by
 * a simply transformation: a {@code while} loop is converted to a 
 * {@code do-while} loop wrapped by an {@code if} statement as shown follow:
 * </p>
 * <pre>
 * void pre_inversion()          void post_inversion()
 * {                             {
 * 		while (condition)         	if (condition)
 * 		{                           {
 * 			// loop body               	do{
 * 		}                         		// loop body   
 * }                                   	}while(condition)
 * 1.a).before loop inversion       } 
 *    							}	
 *    							1.a).after inversion
 * </pre>                                
 * @author Xlous.zeng
 * @version 0.1
 */
public class LoopInversion
{

}
