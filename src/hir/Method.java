package hir; 

import com.sun.org.apache.xerces.internal.parsers.SecurityConfiguration;

import ast.Tree.MethodDef;

/** 
 * @author Jianping Zeng <z1215jping@hotmail.com>
 * @version 2016年2月2日 下午9:10:07 
 */
public class Method
{
	/**
	 * An control flow graph corresponding to method compound 
	 * of this method declaration. 
	 */
	public ControlFlowGraph cfg;
	/** 
	 * Keeps with all property including return type, method name 
	 * and  parameter list. 
	 */
	public MethodDef m;
	
	public Method(MethodDef m)
    {
		this.m = m;
    }
}
