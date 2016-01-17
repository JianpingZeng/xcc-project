package comp;

import sun.awt.datatransfer.ToolkitThreadBlockedHandler;
import sun.misc.Queue;
import utils.Context;

/**
 * A queue of all as yet unattributed method. There two phases involved as
 * followed when we take check on c-flat source file.
 * 
 * In the first stage, attributes global variable definition and method
 * definition itself but no method body. just attributes the return type, name,
 * parameters list of a method and check unique method'name. Then, all of
 * uncomplete method will be added into {@link Todo} list attributed by second
 * phase
 * 
 * In the second stage, attributes method body, including defined statements and
 * local variable of method local scope.
 * 
 * The reason that handling a compilation unit through two phases is that
 * considers circle reference of different method, for example:
 * 
 * void A(){... B(); ...} void B(){...A();....}
 * 
 * @author Jianping Zeng <z1215jping@hotmail.com>
 * @version 2016年1月16日 下午2:04:35
 */
public class Todo<E> extends java.util.ArrayDeque<E>
{
	/**
	 * The context key for the todo list.
	 */
	private static final Context.Key totoKey = new Context.Key();

	/**
	 * A sington method.
	 * @param context
	 * @return
	 */
	public static <E> Todo<E> instance(Context context)
	{
		Todo<E> instance = (Todo<E>)context.get(totoKey);
		if (instance == null)
			instance = new Todo<>(context);
		return instance;
	}
	private Context context  = null;
	public Todo(Context context)
	{
		this.context = context;
		context.put(totoKey, this);		
	}
}
