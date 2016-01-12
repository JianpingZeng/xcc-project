package ast; 

/**
 * storage and qualified flags for c-flat method and variable.
 * 
 * @author Jianping Zeng <z1215jping@hotmail.com>
 */
public interface Flags {
	int STATIC = 1 << 0;
	int CONST = 1 << 1;
	
    int StandardFlags = 1 << CONST - 1;
	/**
	 * A flag marks method parameter.
	 */
	int PARAMETER = 1 << 13;
}