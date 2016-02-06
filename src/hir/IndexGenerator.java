package hir; 
/** 
 * A index generator for unique id.
 * 
 * @author Jianping Zeng <z1215jping@hotmail.com>
 * @version 2015年12月23日 下午8:01:01 
 */
public class IndexGenerator {

	private int sequence = 0;
	
	/** Gets the next id id. */
	public int nextIndex() {return sequence++;}
}
