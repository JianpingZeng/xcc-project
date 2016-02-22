package hir; 

import java.util.List;
import type.Type;
import utils.Context;

/**
 * <p>
 * This class implements overrall container for the HIR(high-level IR) and directs
 * its construction, optimization and finalization.
 * </p>
 * <p>
 * This class consists of multiple {@link Variable} and/or {@link Method}, there
 * is an only control flow graph attached to every method declaration at the AST.
 * </p>
 * <p>
 * Further, a sorts of basic block has filled into CFG in the execution
 * order of program. At the any basic block, a large amount of quads are ordered 
 * in execution order.
 * </p>
 * @author Jianping Zeng <z1215jping@hotmail.com>
 * @version 2016年2月4日 下午4:59:48 
 */
public class HIR
{
	private static final Context.Key HIRKey = new Context.Key();
	/**
	 * A list of variable declaration.
	 */
	List<Variable> vars;
	/**
	 * A sorts of method declaration.
	 */
	List<Method> methods;
	
	/**
	 * An singleton method for instantiating an instance of this class.
	 * @param context	An context environment.
	 * @param vars	Variable declarations list.
	 * @param methods	Method declarations list
	 * @return	The instance of {@link HIR}
	 */
	public static HIR instance(Context context, List<Variable> vars, List<Method> methods)
	{
		HIR instance = (HIR)context.get(HIRKey);
		if (instance == null)
		{
			instance = new HIR(vars, methods);
			context.put(HIRKey, instance);
		}
		return instance;
	}
	
	/**
	 * Constructor.
	 * @param vars
	 * @param methods
	 */
	private HIR(List<Variable> vars, List<Method> methods)
	{
		this.vars = vars;
		this.methods = methods;
	}
	
	
	public static int typecode(Type targetType)
	{
		switch (targetType.tag)
        {
			case Type.BYTE:
				return BYTEcode;
			case Type.SHORT:
				return SHORTcode;
			case Type.CHAR:
				return CHARcode;
			case Type.BOOL:
				return INTcode;
			case Type.FLOAT:
				return FLOATcode;
			case Type.DOUBLE:
				return DOUBLEcode;
			case Type.LONG:
				return LONGcode;
			case Type.VOID:
				return VOIDcode;
			case Type.ARRAY:
				return OBJECTcode;
			default:
		      throw new AssertionError("typecode " + targetType.tag);
		}
	}
	public static int truncate(int typecode)
	{
		switch (typecode)
        {
			case BYTEcode:
			case SHORTcode:
			case CHARcode:
				return INTcode;

			default:
				return typecode;
		}
	}
    /**
     * Type codes.
     */
    public final static int INTcode = 0;

    /**
     * Type codes.
     */
    public final static int LONGcode = 1;

    /**
     * Type codes.
     */
    public final static int FLOATcode = 2;

    /**
     * Type codes.
     */
    public final static int DOUBLEcode = 3;

    /**
     * Type codes.
     */
    public final static int OBJECTcode = 4;

    /**
     * Type codes.
     */
    public final static int BYTEcode = 5;

    /**
     * Type codes.
     */
    public final static int CHARcode = 6;

    /**
     * Type codes.
     */
    public final static int SHORTcode = 7;

    /**
     * Type codes.
     */
    public final static int VOIDcode = 8;
	
}
