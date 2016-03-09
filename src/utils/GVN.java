package utils; 

import hir.BasicBlock;
import java.util.HashMap;

/** 
 * This a auxiliary class for global value numbering of transforming from
 * abstract syntax tree into IR in SSA form according to Matthias Braun's 
 * literature, which keeps a map that associates basic block {@link BasicBlock}
 * with another map that associates definition with it's sole value numbering. 
 * @author Jianping Zeng <z1215jping@hotmail.com>
 * @version 2016年2月5日 下午2:52:53 
 */
/*
public class GVN
{
	private HashMap<BasicBlock, ValueNum> defTable;
	
	public GVN()
    {
		this.defTable = new HashMap<>();		
    }
	
	public long readVariable(BasicBlock block, String variable)
	{
		ValueNum num = defTable.get(block);
		Long number = null;
		if (num != null && (number = num.numbering.get(variable)) != null)
			return number.longValue();
		// global value numbering
		return readVariableRecursive(block, variable);
	}
	
	private long readVariableRecursive(BasicBlock block, String variable)
	{
		
	}
	
	private static class ValueNum
	{
		public HashMap<String, Long> numbering;
		
	}
}
*/
